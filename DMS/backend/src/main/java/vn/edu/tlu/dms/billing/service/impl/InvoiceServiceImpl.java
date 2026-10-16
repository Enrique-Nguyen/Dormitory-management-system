package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.billing.api.InvoiceDtos;
import vn.edu.tlu.dms.billing.domain.Invoice;
import vn.edu.tlu.dms.billing.repository.InvoiceItemRepository;
import vn.edu.tlu.dms.billing.repository.InvoiceRepository;
import vn.edu.tlu.dms.billing.service.InvoiceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.InvoiceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.InvoiceType;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.PageQuery;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/** Hóa đơn và dòng tiền không có API xóa/sửa tùy ý; thanh toán là một hành động có khóa. */
@Service
@Transactional(readOnly = true)
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoices;

    private final InvoiceItemRepository items;

    private final CurrentUser currentUser;

    private final ScopeGuard scopeGuard;

    private final AccountReferenceService accounts;

    private final JdbcTemplate jdbc;

    private final StatusHistoryService historyService;

    public InvoiceServiceImpl(
            InvoiceRepository invoices,
            InvoiceItemRepository items,
            CurrentUser currentUser,
            ScopeGuard scopeGuard,
            AccountReferenceService accounts,
            JdbcTemplate jdbc,
            StatusHistoryService historyService) {
        this.invoices = invoices;
        this.items = items;
        this.currentUser = currentUser;
        this.scopeGuard = scopeGuard;
        this.accounts = accounts;
        this.jdbc = jdbc;
        this.historyService = historyService;
    }

    @Override
    public PageResponse<InvoiceDtos.Response> searchByPage(SearchRequest input, boolean mine) {
        scopeGuard.requirePermission(Permission.INVOICE_READ);
        scopeGuard.requireRole(mine ? new String[] {Role.STUDENT} : new String[] {Role.STAFF});
        if (input.type() != null
                && !Set.of(InvoiceType.ROOM_FEE, InvoiceType.UTILITY).contains(input.type())) {
            throw BusinessException.invalid("type", "Loại hóa đơn không hợp lệ");
        }
        List<String> statuses = input.statuses();
        boolean filterStatuses = statuses != null && !statuses.isEmpty();
        if (filterStatuses
                && statuses.stream()
                        .anyMatch(
                                status ->
                                        status == null
                                                || !Set.of(InvoiceStatus.UNPAID, InvoiceStatus.PAID)
                                                        .contains(status))) {
            throw BusinessException.invalid("status", "Trạng thái hóa đơn không hợp lệ");
        }
        var requested =
                PageQuery.of(
                        input.page(),
                        input.size(),
                        input.sort(),
                        Set.of(
                                "id",
                                "code",
                                "type",
                                "studentId",
                                "semesterId",
                                "roomId",
                                "billingMonth",
                                "totalAmount",
                                "dueDate",
                                "status",
                                "issuedAt",
                                "paidAt"),
                        "issuedAt,desc");
        // Native SQL dùng tên cột thật; mọi thuộc tính đã qua whitelist trước khi đổi tên.
        Sort sqlSort =
                Sort.by(
                        requested.getSort().stream()
                                .map(
                                        order ->
                                                new Sort.Order(
                                                        order.getDirection(),
                                                        order.getProperty()
                                                                .replaceAll(
                                                                        "([a-z0-9])([A-Z])",
                                                                        "$1_$2")
                                                                .toLowerCase(
                                                                        java.util.Locale.ROOT)))
                                .toList());
        var pageable = PageRequest.of(requested.getPageNumber(), requested.getPageSize(), sqlSort);
        var actor = currentUser.get();
        return PageResponse.from(
                invoices.searchAccessibleInvoices(
                                actor.role(),
                                actor.id(),
                                actor.unitId(),
                                actor.crossUnitAccess(),
                                mine,
                                input.type(),
                                filterStatuses,
                                filterStatuses ? statuses : List.of(InvoiceStatus.UNPAID),
                                LikePattern.contains(input.q().trim()),
                                pageable)
                        .map(invoice -> toInvoiceResponse(invoice, false)));
    }

    @Override
    public InvoiceDtos.Response getInvoiceById(Long id) {
        scopeGuard.requirePermission(Permission.INVOICE_READ);
        scopeGuard.requireRole(Role.STUDENT, Role.STAFF);
        return toInvoiceResponse(findAccessibleInvoice(id), true);
    }

    /**
     * Khóa hóa đơn trước khi kiểm tra UNPAID; hai request xác nhận cùng lúc chỉ một request thành
     * công. Không nhận totalAmount/status từ client vì số tiền thuộc dữ liệu đã phát hành.
     */
    @Transactional
    @Override
    public InvoiceDtos.Response confirmInvoicePayment(Long id, InvoiceDtos.Payment input) {
        scopeGuard.requirePermission(Permission.INVOICE_UPDATE);
        scopeGuard.requireRole(Role.STAFF);
        jdbc.query(
                "SELECT id FROM invoice WHERE id = ? FOR UPDATE",
                (row, index) -> row.getLong(1),
                id);
        Invoice invoice = findAccessibleInvoice(id);
        // Quyền tổng hợp chỉ mở rộng quyền đọc; Staff ghi cần đúng đơn vị của hóa đơn (kỳ/phòng của
        // nó), còn Admin được ghi mọi đơn vị.
        if (!currentUser.get().isAdmin()
                && !invoices.isInStaffUnit(id, currentUser.get().unitId())) {
            throw BusinessException.notFound();
        }
        if (!invoice.getStatus().equals(InvoiceStatus.UNPAID)) {
            throw BusinessException.conflict(ErrorCode.INVOICE_ALREADY_PAID);
        }
        if (input.payerUserId() != null) {
            accounts.user(input.payerUserId());
        }
        if (input.payerUserId() == null
                && (input.payerName() == null || input.payerName().isBlank())) {
            throw BusinessException.invalid("payerName", "Cần người nộp hoặc họ tên người nộp");
        }
        invoice.setPayerUserId(input.payerUserId());
        invoice.setPayerName(input.payerName() == null ? null : input.payerName().trim());
        invoice.setPaymentMethod(input.paymentMethod());
        invoice.setPaidAt(OffsetDateTime.now());
        invoice.setConfirmedBy(currentUser.get().id());
        invoice.setStatus(InvoiceStatus.PAID);
        invoices.saveAndFlush(invoice);
        historyService.record(
                EntityType.INVOICE,
                id,
                InvoiceStatus.UNPAID,
                InvoiceStatus.PAID,
                currentUser.get().id(),
                null);
        return toInvoiceResponse(invoice, true);
    }

    private Invoice findAccessibleInvoice(Long id) {
        var actor = currentUser.get();
        return invoices.findAccessibleInvoice(
                        id, actor.role(), actor.id(), actor.unitId(), actor.crossUnitAccess())
                .orElseThrow(BusinessException::notFound);
    }

    private InvoiceDtos.Response toInvoiceResponse(Invoice invoice, boolean detail) {
        List<InvoiceDtos.Item> itemResponses =
                detail
                        ? items
                                .findAll(
                                        (root, query, builder) ->
                                                builder.equal(
                                                        root.get("invoiceId"), invoice.getId()))
                                .stream()
                                .map(
                                        item ->
                                                new InvoiceDtos.Item(
                                                        item.getId(),
                                                        item.getItemType(),
                                                        item.getQuantity(),
                                                        item.getUnitPrice(),
                                                        item.getAmount(),
                                                        item.getNote()))
                                .toList()
                        : null;
        // ADMIN được ghi mọi đơn vị; STAFF tổng hợp chỉ mở rộng đọc, còn xác nhận tiền vẫn đúng đơn
        // vị.
        List<String> actions = List.of();
        if (detail
                && scopeGuard.hasPermission(Permission.INVOICE_UPDATE)
                && invoice.getStatus().equals(InvoiceStatus.UNPAID)
                && (currentUser.get().isAdmin()
                        || invoices.isInStaffUnit(invoice.getId(), currentUser.get().unitId()))) {
            actions = List.of("confirm-payment");
        }
        return new InvoiceDtos.Response(
                invoice.getId(),
                invoice.getCode(),
                invoice.getType(),
                invoice.getStudentId(),
                invoice.getSemesterId(),
                invoice.getRoomId(),
                invoice.getBillingMonth(),
                invoice.getTotalAmount(),
                invoice.getDueDate(),
                invoice.getStatus(),
                invoice.getPaidAt(),
                itemResponses,
                detail ? actions : null,
                detail ? historyService.list(EntityType.INVOICE, invoice.getId()) : null);
    }
}

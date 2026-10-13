package vn.edu.tlu.dms.billing.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.billing.api.InvoiceDtos;
import vn.edu.tlu.dms.billing.domain.Invoice;
import vn.edu.tlu.dms.billing.repository.InvoiceItemRepository;
import vn.edu.tlu.dms.billing.repository.InvoiceRepository;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/** Hóa đơn và dòng tiền không có API xóa/sửa tùy ý; thanh toán là một hành động có khóa. */
@Service
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository invoices;

    private final InvoiceItemRepository items;

    private final CurrentUser currentUser;

    private final ScopeGuard scopeGuard;

    private final RoomReferenceService rooms;

    private final AccountReferenceService accounts;

    private final JdbcTemplate jdbc;

    private final StatusHistoryService historyService;

    public InvoiceService(
            InvoiceRepository invoices,
            InvoiceItemRepository items,
            CurrentUser currentUser,
            ScopeGuard scopeGuard,
            RoomReferenceService rooms,
            AccountReferenceService accounts,
            JdbcTemplate jdbc,
            StatusHistoryService historyService) {
        this.invoices = invoices;
        this.items = items;
        this.currentUser = currentUser;
        this.scopeGuard = scopeGuard;
        this.rooms = rooms;
        this.accounts = accounts;
        this.jdbc = jdbc;
        this.historyService = historyService;
    }

    public PageResponse<InvoiceDtos.Response> searchInvoices(
            int page, int size, String type, String status, boolean mine) {
        return searchInvoices(page, size, type, status, mine, "");
    }

    public PageResponse<InvoiceDtos.Response> searchInvoices(
            int page, int size, String type, String status, boolean mine, String q) {
        scopeGuard.requireRole(mine ? new String[] {"STUDENT"} : new String[] {"STAFF"});
        var actor = currentUser.get();
        PageQuery.of(page, size, null, Set.of("id"), "id,desc");
        return PageResponse.from(
                invoices.searchAccessibleInvoices(
                                actor.role(),
                                actor.id(),
                                actor.unitId(),
                                actor.crossUnitAccess(),
                                type,
                                status,
                                "%" + q.trim() + "%",
                                PageRequest.of(page, size))
                        .map(invoice -> toInvoiceResponse(invoice, false)));
    }

    public InvoiceDtos.Response getInvoiceById(Long id) {
        scopeGuard.requireRole("STUDENT", "STAFF");
        return toInvoiceResponse(findAccessibleInvoice(id), true);
    }

    /**
     * Khóa hóa đơn trước khi kiểm tra UNPAID; hai request xác nhận cùng lúc chỉ một request thành
     * công. Không nhận totalAmount/status từ client vì số tiền thuộc dữ liệu đã phát hành.
     */
    @Transactional
    public InvoiceDtos.Response confirmInvoicePayment(Long id, InvoiceDtos.Payment input) {
        scopeGuard.requireRole("STAFF");
        jdbc.query(
                "SELECT id FROM invoice WHERE id = ? FOR UPDATE",
                (row, index) -> row.getLong(1),
                id);
        Invoice invoice = findAccessibleInvoice(id);
        if (invoice.getRoomId() != null) {
            scopeGuard.requireUnit(rooms.getRoomLocation(invoice.getRoomId()).unitId());
        } else {
            Boolean inOwnUnit =
                    jdbc.queryForObject(
                            """
                            SELECT EXISTS (
                                SELECT 1 FROM room_assignment_history a
                                JOIN bed b ON b.id = a.bed_id JOIN room r ON r.id = b.room_id
                                JOIN building bd ON bd.id = r.building_id
                                WHERE a.student_id = ? AND bd.management_unit_id = ?)
                            """,
                            Boolean.class,
                            invoice.getStudentId(),
                            currentUser.get().unitId());
            if (!Boolean.TRUE.equals(inOwnUnit)) {
                throw BusinessException.notFound();
            }
        }
        if (!invoice.getStatus().equals("UNPAID")) {
            throw BusinessException.conflict("INVOICE_ALREADY_PAID");
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
        invoice.setStatus("PAID");
        invoices.saveAndFlush(invoice);
        historyService.record("INVOICE", id, "UNPAID", "PAID", currentUser.get().id(), null);
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
        // Quyền tổng hợp chỉ mở rộng quyền đọc; quyền ghi cần đúng đơn vị của hóa đơn.
        List<String> actions = List.of();
        if (detail && currentUser.get().is("STAFF") && invoice.getStatus().equals("UNPAID")) {
            if (invoice.getRoomId() != null
                    && rooms.getRoomLocation(invoice.getRoomId())
                            .unitId()
                            .equals(currentUser.get().unitId())) {
                actions = List.of("confirm-payment");
            } else if (invoice.getStudentId() != null
                    && Boolean.TRUE.equals(
                            jdbc.queryForObject(
                                    """
                                    SELECT EXISTS (SELECT 1 FROM room_assignment_history a
                                    JOIN bed b ON b.id = a.bed_id JOIN room r ON r.id = b.room_id
                                    JOIN building bd ON bd.id = r.building_id
                                    WHERE a.student_id = ? AND bd.management_unit_id = ?)
                                    """,
                                    Boolean.class,
                                    invoice.getStudentId(),
                                    currentUser.get().unitId()))) {
                actions = List.of("confirm-payment");
            }
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
                detail ? historyService.list("INVOICE", invoice.getId()) : null);
    }
}

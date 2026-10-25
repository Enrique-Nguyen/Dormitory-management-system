package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.billing.api.InvoiceDtos;
import vn.edu.tlu.dms.billing.domain.Invoice;
import vn.edu.tlu.dms.billing.domain.InvoiceItem;
import vn.edu.tlu.dms.billing.repository.InvoiceItemRepository;
import vn.edu.tlu.dms.billing.repository.InvoiceRepository;
import vn.edu.tlu.dms.billing.service.InvoiceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ConfigKey;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.InvoiceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.InvoiceType;
import vn.edu.tlu.dms.common.constants.DmsConstants.MeterType;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.RoomStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.constants.DmsConstants.SummerChoice;
import vn.edu.tlu.dms.common.constants.DmsConstants.SummerStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.ConfigService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ConfigService config;
    private final AuditService audit;
    private final RoomReferenceService rooms;

    public InvoiceServiceImpl(
            InvoiceRepository invoices,
            InvoiceItemRepository items,
            CurrentUser currentUser,
            ScopeGuard scopeGuard,
            AccountReferenceService accounts,
            JdbcTemplate jdbc,
            StatusHistoryService historyService,
            ConfigService config,
            AuditService audit,
            RoomReferenceService rooms) {
        this.invoices = invoices;
        this.items = items;
        this.currentUser = currentUser;
        this.scopeGuard = scopeGuard;
        this.accounts = accounts;
        this.jdbc = jdbc;
        this.historyService = historyService;
        this.config = config;
        this.audit = audit;
        this.rooms = rooms;
    }

    /** Tìm hóa đơn trong phạm vi người dùng, lọc và phân trang tại CSDL. */
    @Override
    public PageResponse<InvoiceDtos.Response> searchByPage(SearchRequest input, boolean mine) {
        scopeGuard.requirePermission(Permission.INVOICE_READ);
        scopeGuard.requireRole(mine ? new String[] {Role.STUDENT} : new String[] {Role.STAFF});
        if (input.type() != null
                && !Set.of(InvoiceType.ROOM_FEE, InvoiceType.UTILITY).contains(input.type())) {
            throw BusinessException.invalid(ApiField.TYPE, ValidationMessage.INVOICE_TYPE_INVALID);
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
            throw BusinessException.invalid(
                    ApiField.STATUS, ValidationMessage.INVOICE_STATUS_INVALID);
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
                                                                .toLowerCase(Locale.ROOT)))
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

    /** Đọc chi tiết hóa đơn sau khi kiểm tra quyền sở hữu hoặc đơn vị. */
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
    public InvoiceDtos.Response confirmPayment(Long id, InvoiceDtos.Payment input) {
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
            throw BusinessException.invalid(ApiField.PAYER_NAME, ValidationMessage.PAYER_REQUIRED);
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

    /** Phát hành phí phòng theo học kỳ hiện tại, khóa sinh viên và ghi audit cùng transaction. */
    @Transactional
    @Override
    public InvoiceDtos.BatchIssueResult issueRoomFee(Long semesterId) {
        scopeGuard.requirePermission(Permission.INVOICE_CREATE);
        scopeGuard.requireRole(Role.STAFF, Role.ADMIN);

        var actor = currentUser.get();
        List<Map<String, Object>> semesterRows =
                jdbc.queryForList(
                        "SELECT id, name, type, start_date, end_date FROM semester WHERE id = ?",
                        semesterId);
        if (semesterRows.isEmpty()) {
            throw BusinessException.notFound();
        }
        LocalDate today = LocalDate.now();
        LocalDate start = ((Date) semesterRows.get(0).get("start_date")).toLocalDate();
        LocalDate end = ((Date) semesterRows.get(0).get("end_date")).toLocalDate();
        if (today.isBefore(start) || today.isAfter(end)) {
            throw BusinessException.invalid(
                    ApiField.SEMESTER_ID, ValidationMessage.INVOICE_CURRENT_SEMESTER);
        }
        String semesterType = (String) semesterRows.get(0).get("type");
        String semesterName = (String) semesterRows.get(0).get("name");

        String scopeCondition = "TRUE";
        List<Object> args = new ArrayList<>();
        if (!actor.isAdmin()) {
            if (actor.unitId() == null) {
                throw BusinessException.forbidden();
            }
            scopeCondition = "bd.management_unit_id = ?";
            args.add(actor.unitId());
        }

        String sql =
                """
                SELECT h.id AS assignment_id, h.student_id, h.price_main, h.price_summer
                FROM room_assignment_history h
                JOIN bed b ON b.id = h.bed_id
                JOIN room r ON r.id = b.room_id
                JOIN building bd ON bd.id = r.building_id
                JOIN dormitory_contract c ON c.id = h.contract_id
                WHERE h.ended_at IS NULL AND h.kind = ? AND c.status = ? AND %s
                  AND h.stay_started_at < CURRENT_DATE + 1
                  AND (? <> ? OR EXISTS (SELECT 1 FROM summer_choice sc
                      WHERE sc.contract_id = h.contract_id AND sc.semester_id = ?
                        AND sc.choice = ? AND sc.status = ?))
                ORDER BY h.student_id
                """
                        .formatted(scopeCondition);

        args.add(0, AssignmentKind.STAY);
        args.add(1, ContractStatus.ACTIVE);
        args.addAll(
                List.of(
                        semesterType,
                        SemesterType.SUMMER,
                        semesterId,
                        SummerChoice.STAY,
                        SummerStatus.APPROVED));
        List<Map<String, Object>> candidates = jdbc.queryForList(sql, args.toArray());
        // Khóa sinh viên theo thứ tự tăng dần trước khi đọc lại phân phòng và kiểm tra hóa đơn.
        for (var candidate : candidates) {
            rooms.lockStudentProfile(((Number) candidate.get("student_id")).longValue());
        }
        Set<Long> lockedStudents =
                candidates.stream()
                        .map(row -> ((Number) row.get("student_id")).longValue())
                        .collect(Collectors.toSet());
        List<Map<String, Object>> stays =
                jdbc.queryForList(sql, args.toArray()).stream()
                        .filter(
                                row ->
                                        lockedStudents.contains(
                                                ((Number) row.get("student_id")).longValue()))
                        .toList();

        int created = 0;
        int skipped = 0;
        long totalAmount = 0L;

        LocalDate now = LocalDate.now();
        OffsetDateTime issuedAt = OffsetDateTime.now();
        LocalDate dueDate = now.plusDays(config.positiveInt(ConfigKey.INVOICE_DUE_DAYS));

        for (var stay : stays) {
            Long studentId = ((Number) stay.get("student_id")).longValue();
            Long assignmentId = ((Number) stay.get("assignment_id")).longValue();

            Boolean exists =
                    jdbc.queryForObject(
                            "SELECT EXISTS(SELECT 1 FROM invoice WHERE student_id = ? AND"
                                    + " semester_id = ? AND type = ?)",
                            Boolean.class,
                            studentId,
                            semesterId,
                            InvoiceType.ROOM_FEE);
            if (Boolean.TRUE.equals(exists)) {
                skipped++;
                continue;
            }

            Number rawPrice =
                    SemesterType.SUMMER.equals(semesterType)
                            ? (Number) stay.get("price_summer")
                            : (Number) stay.get("price_main");
            if (rawPrice == null || rawPrice.longValue() < 0) {
                throw BusinessException.invalid(
                        ApiField.PRICE, ValidationMessage.ASSIGNMENT_PRICE_REQUIRED);
            }
            long price = rawPrice.longValue();

            String code =
                    jdbc.queryForObject(
                            "SELECT fn_next_code(?,'seq_invoice_code',true)",
                            String.class,
                            InvoiceType.ROOM_FEE_PREFIX);

            Invoice invoice = new Invoice();
            invoice.setCode(code);
            invoice.setType(InvoiceType.ROOM_FEE);
            invoice.setStudentId(studentId);
            invoice.setSemesterId(semesterId);
            invoice.setTotalAmount(price);
            invoice.setDueDate(dueDate);
            invoice.setIssuedAt(issuedAt);
            invoice.setIssuedBy(actor.id());
            invoice.setStatus(InvoiceStatus.UNPAID);
            invoice = invoices.saveAndFlush(invoice);

            InvoiceItem item = new InvoiceItem();
            item.setInvoiceId(invoice.getId());
            item.setItemType(InvoiceType.ROOM_FEE);
            item.setQuantity(BigDecimal.ONE);
            item.setUnitPrice(price);
            item.setAmount(price);
            item.setAssignmentId(assignmentId);
            item.setNote("Phí phòng " + semesterName);
            items.saveAndFlush(item);

            historyService.record(
                    EntityType.INVOICE,
                    invoice.getId(),
                    null,
                    InvoiceStatus.UNPAID,
                    actor.id(),
                    "Phát hành hóa đơn phí phòng học kỳ " + semesterName);

            created++;
            totalAmount += price;
        }

        var result = new InvoiceDtos.BatchIssueResult(created, skipped, totalAmount);
        audit.record(
                actor.id(),
                AuditAction.INVOICE_BATCH_ISSUE,
                EntityType.INVOICE,
                null,
                null,
                Map.of("type", InvoiceType.ROOM_FEE, "semesterId", semesterId, "result", result),
                null);
        return result;
    }

    /** Phát hành điện nước trong đơn vị ghi, khóa phòng/chỉ số trước kiểm tra hóa đơn trùng. */
    @Transactional
    @Override
    public InvoiceDtos.BatchIssueResult issueUtility(
            Long managementUnitId, LocalDate billingMonth) {
        scopeGuard.requirePermission(Permission.INVOICE_CREATE);
        scopeGuard.requireRole(Role.STAFF, Role.ADMIN);

        var actor = currentUser.get();
        Long effectiveUnitId = actor.isAdmin() ? managementUnitId : actor.unitId();
        if (!actor.isAdmin()
                && (effectiveUnitId == null
                        || (managementUnitId != null
                                && !managementUnitId.equals(effectiveUnitId)))) {
            throw BusinessException.notFound();
        }

        if (billingMonth == null || billingMonth.getDayOfMonth() != 1) {
            throw BusinessException.invalid(
                    ApiField.BILLING_MONTH, ValidationMessage.BILLING_MONTH_START);
        }

        Long electricRate =
                jdbc
                        .query(
                                """
                                SELECT unit_price FROM utility_rate
                                WHERE meter_type = ? AND effective_from <= ?
                                ORDER BY effective_from DESC, id DESC LIMIT 1
                                """,
                                (rs, rowNum) -> rs.getLong(1),
                                MeterType.ELECTRIC,
                                billingMonth)
                        .stream()
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        BusinessException.invalid(
                                                "rate", ValidationMessage.ELECTRIC_RATE_REQUIRED));

        Long waterRate =
                jdbc
                        .query(
                                """
                                SELECT unit_price FROM utility_rate
                                WHERE meter_type = ? AND effective_from <= ?
                                ORDER BY effective_from DESC, id DESC LIMIT 1
                                """,
                                (rs, rowNum) -> rs.getLong(1),
                                MeterType.WATER,
                                billingMonth)
                        .stream()
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        BusinessException.invalid(
                                                "rate", ValidationMessage.WATER_RATE_REQUIRED));

        String unitCondition = effectiveUnitId != null ? "bd.management_unit_id = ?" : "TRUE";
        List<Object> args = new ArrayList<>();
        if (effectiveUnitId != null) {
            args.add(effectiveUnitId);
        }

        String sql =
                """
                SELECT r.id, r.code, r.leader_student_id
                FROM room r
                JOIN building bd ON bd.id = r.building_id
                WHERE %s AND r.status = ?
                  AND EXISTS (SELECT 1 FROM room_assignment_history h JOIN bed b ON b.id=h.bed_id
                      WHERE b.room_id=r.id AND h.kind=? AND h.ended_at IS NULL)
                ORDER BY r.id FOR UPDATE OF r
                """
                        .formatted(unitCondition);

        args.add(RoomStatus.ACTIVE);
        args.add(AssignmentKind.STAY);
        List<Map<String, Object>> roomRows = jdbc.queryForList(sql, args.toArray());

        int created = 0;
        int skipped = 0;
        long totalAmount = 0L;

        LocalDate now = LocalDate.now();
        OffsetDateTime issuedAt = OffsetDateTime.now();
        LocalDate dueDate = now.plusDays(config.positiveInt(ConfigKey.INVOICE_DUE_DAYS));

        for (var roomRow : roomRows) {
            Long roomId = ((Number) roomRow.get("id")).longValue();

            Boolean exists =
                    jdbc.queryForObject(
                            "SELECT EXISTS(SELECT 1 FROM invoice WHERE room_id = ? AND"
                                    + " billing_month = ? AND type = ?)",
                            Boolean.class,
                            roomId,
                            billingMonth,
                            InvoiceType.UTILITY);
            if (Boolean.TRUE.equals(exists)) {
                skipped++;
                continue;
            }

            List<Map<String, Object>> readings =
                    jdbc.queryForList(
                            "SELECT id, meter_type, consumption FROM utility_reading WHERE room_id"
                                    + " = ? AND billing_month = ? ORDER BY id FOR UPDATE",
                            roomId,
                            billingMonth);

            Long electricConsumption = null;
            Long electricReadingId = null;
            Long waterConsumption = null;
            Long waterReadingId = null;

            for (var reading : readings) {
                String type = (String) reading.get("meter_type");
                Number c = (Number) reading.get("consumption");
                Long rId = ((Number) reading.get("id")).longValue();
                if (MeterType.ELECTRIC.equals(type)) {
                    electricConsumption = c != null ? c.longValue() : 0L;
                    electricReadingId = rId;
                } else if (MeterType.WATER.equals(type)) {
                    waterConsumption = c != null ? c.longValue() : 0L;
                    waterReadingId = rId;
                }
            }

            if (electricConsumption == null || waterConsumption == null) {
                skipped++;
                continue;
            }

            long electricCost = electricConsumption * electricRate;
            long waterCost = waterConsumption * waterRate;
            long total = electricCost + waterCost;

            String code =
                    jdbc.queryForObject(
                            "SELECT fn_next_code(?,'seq_invoice_code',true)",
                            String.class,
                            InvoiceType.UTILITY_PREFIX);

            Invoice invoice = new Invoice();
            invoice.setCode(code);
            invoice.setType(InvoiceType.UTILITY);
            invoice.setRoomId(roomId);
            invoice.setBillingMonth(billingMonth);
            invoice.setTotalAmount(total);
            invoice.setDueDate(dueDate);
            invoice.setIssuedAt(issuedAt);
            invoice.setIssuedBy(actor.id());
            invoice.setStatus(InvoiceStatus.UNPAID);
            invoice = invoices.saveAndFlush(invoice);

            InvoiceItem elItem = new InvoiceItem();
            elItem.setInvoiceId(invoice.getId());
            elItem.setItemType(MeterType.ELECTRIC);
            elItem.setQuantity(BigDecimal.valueOf(electricConsumption));
            elItem.setUnitPrice(electricRate);
            elItem.setAmount(electricCost);
            elItem.setUtilityReadingId(electricReadingId);
            elItem.setNote("Tiền điện tiêu thụ " + electricConsumption + " kWh");
            items.saveAndFlush(elItem);

            InvoiceItem wItem = new InvoiceItem();
            wItem.setInvoiceId(invoice.getId());
            wItem.setItemType(MeterType.WATER);
            wItem.setQuantity(BigDecimal.valueOf(waterConsumption));
            wItem.setUnitPrice(waterRate);
            wItem.setAmount(waterCost);
            wItem.setUtilityReadingId(waterReadingId);
            wItem.setNote("Tiền nước tiêu thụ " + waterConsumption + " m3");
            items.saveAndFlush(wItem);

            historyService.record(
                    EntityType.INVOICE,
                    invoice.getId(),
                    null,
                    InvoiceStatus.UNPAID,
                    actor.id(),
                    "Phát hành hóa đơn điện nước kỳ " + billingMonth);

            created++;
            totalAmount += total;
        }

        var result = new InvoiceDtos.BatchIssueResult(created, skipped, totalAmount);
        var auditValue = new java.util.LinkedHashMap<String, Object>();
        auditValue.put("type", InvoiceType.UTILITY);
        auditValue.put("managementUnitId", effectiveUnitId);
        auditValue.put("billingMonth", billingMonth);
        auditValue.put("result", result);
        audit.record(
                actor.id(),
                AuditAction.INVOICE_BATCH_ISSUE,
                EntityType.INVOICE,
                null,
                null,
                auditValue,
                null);
        return result;
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
            actions = List.of(BusinessAction.CONFIRM_PAYMENT);
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

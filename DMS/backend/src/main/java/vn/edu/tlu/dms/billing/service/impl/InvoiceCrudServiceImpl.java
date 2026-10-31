package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.api.InvoiceDtos;
import vn.edu.tlu.dms.billing.service.InvoiceCrudService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.InvoiceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.InvoiceType;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.impl.AbstractSqlCrudServiceImpl;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Hồ sơ hóa đơn UNPAID chưa có dòng tiền: khung nhập/sửa/xóa; phát hành tự tính tiền để làm sau.
 */
@Service
@Transactional(readOnly = true)
public class InvoiceCrudServiceImpl extends AbstractSqlCrudServiceImpl
        implements InvoiceCrudService {

    private final ScopeGuard guard;

    private final RoomReferenceService rooms;

    private static final Map<String, String> FIELDS =
            Map.of(
                    "type",
                    "type:text",
                    "studentId",
                    "student_id:long",
                    "semesterId",
                    "semester_id:long",
                    "roomId",
                    "room_id:long",
                    "billingMonth",
                    "billing_month:date",
                    "totalAmount",
                    "total_amount:long",
                    "dueDate",
                    "due_date:date");

    public InvoiceCrudServiceImpl(
            JdbcTemplate jdbc,
            SqlPage queries,
            CurrentUser current,
            AuditService audit,
            ScopeGuard guard,
            RoomReferenceService rooms) {
        super(jdbc, queries, current, audit);
        this.guard = guard;
        this.rooms = rooms;
    }

    @Transactional
    @Override
    public Object saveOrUpdate(InvoiceDtos.Save input) {
        if (input == null) {
            throw BusinessException.invalid(ApiField.INPUT, ValidationMessage.INVOICE_INPUT_REQUIRED);
        }
        if (input.id() == null) {
            guard.requirePermission(Permission.INVOICE_CREATE);
            Map<String, Object> map = new HashMap<>();
            if (input.type() != null) map.put("type", input.type());
            if (input.studentId() != null) map.put("studentId", input.studentId());
            if (input.semesterId() != null) map.put("semesterId", input.semesterId());
            if (input.roomId() != null) map.put("roomId", input.roomId());
            if (input.billingMonth() != null) map.put("billingMonth", input.billingMonth());
            if (input.totalAmount() != null) map.put("totalAmount", input.totalAmount());
            if (input.dueDate() != null) map.put("dueDate", input.dueDate());
            return createInvoiceRecord(map);
        } else {
            guard.requirePermission(Permission.INVOICE_UPDATE);
            if (input.id() <= 0) {
                throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE);
            }
            if (input.type() != null
                    || input.studentId() != null
                    || input.semesterId() != null
                    || input.roomId() != null
                    || input.billingMonth() != null) {
                throw BusinessException.invalid(
                        ApiField.FORM, ValidationMessage.INVOICE_EDIT_UNISSUED_ONLY);
            }
            Map<String, Object> map = new HashMap<>();
            if (input.totalAmount() != null) map.put("totalAmount", input.totalAmount());
            if (input.dueDate() != null) map.put("dueDate", input.dueDate());
            if (map.isEmpty()) {
                throw BusinessException.invalid(ApiField.FORM, ValidationMessage.UPDATE_INPUT_REQUIRED);
            }
            return updateUnpaidInvoiceRecord(input.id(), map);
        }
    }

    private Object createInvoiceRecord(Map<String, Object> input) {
        guard.requirePermission(Permission.INVOICE_CREATE);
        guard.requireRole(Role.STAFF);
        validate(input);
        String code =
                jdbc.queryForObject(
                        "SELECT fn_next_code(?,'seq_invoice_code',true)",
                        String.class,
                        InvoiceType.UTILITY.equals(input.get("type")) ? "DN" : "HP");
        return insertRecord(
                "invoice",
                FIELDS,
                Set.of("type", "totalAmount", "dueDate"),
                input,
                Map.of(
                        "code",
                        code,
                        "issued_by",
                        current.get().id(),
                        "status",
                        InvoiceStatus.UNPAID));
    }

    private Object updateUnpaidInvoiceRecord(Long id, Map<String, Object> input) {
        guard.requirePermission(Permission.INVOICE_UPDATE);
        guard.requireRole(Role.STAFF);
        var row = loadEditable(id);
        if (input.keySet().stream().anyMatch(k -> !Set.of("totalAmount", "dueDate").contains(k))) {
            throw BusinessException.invalid(
                    ApiField.FORM, ValidationMessage.INVOICE_EDIT_UNISSUED_ONLY);
        }
        var merged = new HashMap<>(row);
        merged.putAll(input);
        validate(merged);
        return updateRecord("invoice", id, FIELDS, input);
    }

    @Transactional
    @Override
    public void deleteUnusedInvoiceRecord(Long id) {
        guard.requirePermission(Permission.INVOICE_DELETE);
        guard.requireRole(Role.STAFF);
        loadEditable(id);
        deleteRecord("invoice", id);
    }

    private Map<String, Object> loadEditable(Long id) {
        var row = queries.one("SELECT * FROM invoice WHERE id=? FOR UPDATE", id);
        requireScope(row);
        if (!InvoiceStatus.UNPAID.equals(row.get("status"))
                || Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                "SELECT EXISTS(SELECT 1 FROM invoice_item WHERE invoice_id=?) OR"
                                        + " EXISTS(SELECT 1 FROM status_history WHERE"
                                        + " entity_type='INVOICE' AND entity_id=?)",
                                Boolean.class,
                                id,
                                id))) {
            throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
        }
        return row;
    }

    private void requireScope(Map<String, Object> input) {
        if (InvoiceType.UTILITY.equals(input.get("type"))) {
            guard.requireUnit(rooms.getRoomLocation(number(input, "roomId")).unitId());
        } else if (InvoiceType.ROOM_FEE.equals(input.get("type"))) {
            // ADMIN không bị giới hạn đơn vị, nhưng người nhận hóa đơn phải là sinh viên thật.
            Long studentId = number(input, "studentId");
            Long semesterId = number(input, "semesterId");
            rooms.getSemesterReference(semesterId);
            if (!Boolean.TRUE.equals(
                    jdbc.queryForObject(
                            "SELECT EXISTS(SELECT 1 FROM student_profile WHERE user_id=?)",
                            Boolean.class,
                            studentId))) {
                throw BusinessException.notFound();
            }
            // Cùng phạm vi học kỳ với InvoiceRepository.STAFF_UNIT_SCOPE; quyền tổng hợp chỉ để đọc.
            if (!current.get().isAdmin()
                    && !Boolean.TRUE.equals(
                            jdbc.queryForObject(
                                    """
                                    SELECT EXISTS (
                                        SELECT 1 FROM room_assignment_history a
                                        JOIN bed b ON b.id = a.bed_id
                                        JOIN room r ON r.id = b.room_id
                                        JOIN building bd ON bd.id = r.building_id
                                        JOIN semester s ON s.id = ?
                                        WHERE a.student_id = ? AND bd.management_unit_id = ?
                                            AND a.started_at < s.end_date + 1
                                            AND (a.ended_at IS NULL OR a.ended_at >= s.start_date))
                                    OR EXISTS (
                                        SELECT 1 FROM room_application ap
                                        JOIN registration_period rp ON rp.id = ap.period_id
                                        WHERE ap.student_id = ? AND ap.preferred_unit_id = ?
                                            AND rp.semester_id = ?)
                                    """,
                                    Boolean.class,
                                    semesterId,
                                    studentId,
                                    current.get().unitId(),
                                    studentId,
                                    current.get().unitId(),
                                    semesterId))) {
                throw BusinessException.notFound();
            }
        } else {
            throw BusinessException.invalid(ApiField.TYPE, ValidationMessage.INVOICE_TYPE_CHOICE);
        }
    }

    private void validate(Map<String, Object> input) {
        requireScope(input);
        if (InvoiceType.ROOM_FEE.equals(input.get("type"))) {
            if (input.get("roomId") != null || input.get("billingMonth") != null) {
                throw BusinessException.invalid(ApiField.TYPE, ValidationMessage.ROOM_FEE_TARGET);
            }
        } else {
            if (input.get("studentId") != null || input.get("semesterId") != null) {
                throw BusinessException.invalid(ApiField.TYPE, ValidationMessage.UTILITY_TARGET);
            }
            try {
                if (LocalDate.parse(input.get("billingMonth").toString()).getDayOfMonth() != 1) {
                    throw BusinessException.invalid(ApiField.BILLING_MONTH, ValidationMessage.FIRST_DAY_OF_MONTH);
                }
            } catch (RuntimeException e) {
                throw BusinessException.invalid(ApiField.BILLING_MONTH, ValidationMessage.FIRST_DAY_OF_MONTH_VALID);
            }
        }
        if (number(input, "totalAmount") < 0) {
            throw BusinessException.invalid(ApiField.TOTAL_AMOUNT, ValidationMessage.AMOUNT_NOT_NEGATIVE);
        }
        try {
            if (LocalDate.parse(input.get("dueDate").toString()).isBefore(LocalDate.now())) {
                throw BusinessException.invalid(ApiField.DUE_DATE, ValidationMessage.DUE_DATE_NOT_PAST);
            }
        } catch (RuntimeException e) {
            throw BusinessException.invalid(ApiField.DUE_DATE, ValidationMessage.DUE_DATE_FROM_TODAY);
        }
    }

    private static Long number(Map<String, Object> input, String field) {
        try {
            return Long.valueOf(input.get(field).toString());
        } catch (RuntimeException e) {
            throw BusinessException.invalid(field, ValidationMessage.NUMBER_REQUIRED);
        }
    }
}

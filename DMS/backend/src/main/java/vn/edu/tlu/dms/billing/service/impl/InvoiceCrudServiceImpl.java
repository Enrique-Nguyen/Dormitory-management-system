package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.service.InvoiceCrudService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.InvoiceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.InvoiceType;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
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
    public Object createInvoiceRecord(Map<String, Object> input) {
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

    @Transactional
    @Override
    public Object updateUnpaidInvoiceRecord(Long id, Map<String, Object> input) {
        guard.requirePermission(Permission.INVOICE_UPDATE);
        guard.requireRole(Role.STAFF);
        var row = loadEditable(id);
        if (input.keySet().stream().anyMatch(k -> !Set.of("totalAmount", "dueDate").contains(k))) {
            throw BusinessException.invalid(
                    "form", "Chỉ sửa tổng tiền/hạn đóng của hồ sơ chưa phát hành");
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
            if (!Boolean.TRUE.equals(
                    jdbc.queryForObject(
                            "SELECT EXISTS(SELECT 1 FROM student_profile WHERE user_id=?)",
                            Boolean.class,
                            studentId))) {
                throw BusinessException.notFound();
            }
            if (!current.get().isAdmin()
                    && !Boolean.TRUE.equals(
                            jdbc.queryForObject(
                                    "SELECT EXISTS(SELECT 1 FROM room_application WHERE"
                                            + " student_id=? AND preferred_unit_id=?)",
                                    Boolean.class,
                                    number(input, "studentId"),
                                    current.get().unitId()))) {
                throw BusinessException.notFound();
            }
        } else {
            throw BusinessException.invalid("type", "Chọn ROOM_FEE hoặc UTILITY");
        }
    }

    private void validate(Map<String, Object> input) {
        requireScope(input);
        if (InvoiceType.ROOM_FEE.equals(input.get("type"))) {
            rooms.getSemesterReference(number(input, "semesterId"));
            if (input.get("roomId") != null || input.get("billingMonth") != null) {
                throw BusinessException.invalid("type", "Phí phòng chỉ gắn sinh viên/học kỳ");
            }
        } else {
            if (input.get("studentId") != null || input.get("semesterId") != null) {
                throw BusinessException.invalid("type", "Điện nước chỉ gắn phòng/tháng");
            }
            try {
                if (LocalDate.parse(input.get("billingMonth").toString()).getDayOfMonth() != 1) {
                    throw BusinessException.invalid("billingMonth", "Dùng ngày đầu tháng");
                }
            } catch (RuntimeException e) {
                throw BusinessException.invalid("billingMonth", "Nhập ngày đầu tháng hợp lệ");
            }
        }
        if (number(input, "totalAmount") < 0) {
            throw BusinessException.invalid("totalAmount", "Số tiền không được âm");
        }
        try {
            if (LocalDate.parse(input.get("dueDate").toString()).isBefore(LocalDate.now())) {
                throw BusinessException.invalid("dueDate", "Hạn đóng không được trước hôm nay");
            }
        } catch (RuntimeException e) {
            throw BusinessException.invalid("dueDate", "Hạn đóng phải từ hôm nay");
        }
    }

    private static Long number(Map<String, Object> input, String field) {
        try {
            return Long.valueOf(input.get(field).toString());
        } catch (RuntimeException e) {
            throw BusinessException.invalid(field, "Giá trị số bắt buộc");
        }
    }
}

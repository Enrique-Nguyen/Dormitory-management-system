package vn.edu.tlu.dms.maintenance.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.maintenance.api.MaintenanceCrudController.Edit;
import vn.edu.tlu.dms.maintenance.service.MaintenanceCrudService;
import vn.edu.tlu.dms.maintenance.service.MaintenanceRequestService;

@Service
@Transactional
public class MaintenanceCrudServiceImpl implements MaintenanceCrudService {

    private final JdbcTemplate jdbc;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final MaintenanceRequestService requests;

    private final AuditService audit;

    private final StatusHistoryService history;

    public MaintenanceCrudServiceImpl(
            JdbcTemplate jdbc,
            CurrentUser current,
            ScopeGuard guard,
            MaintenanceRequestService requests,
            AuditService audit,
            StatusHistoryService history) {
        this.jdbc = jdbc;
        this.current = current;
        this.guard = guard;
        this.requests = requests;
        this.audit = audit;
        this.history = history;
    }

    private void requireOpenRequestOfCreator(Long id) {
        // Kiểm tra bằng JDBC để không nạp entity vào persistence context: các lệnh UPDATE bằng JDBC
        // bên dưới đi vòng Hibernate, nếu entity đã nằm trong cache cấp 1 thì phản hồi sẽ cũ.
        var rows =
                jdbc.query(
                        "SELECT requester_id,status FROM maintenance_request WHERE id=? FOR UPDATE",
                        (r, i) -> new Object[] {r.getLong(1), r.getString(2)},
                        id);
        // Ticket không tồn tại hoặc không thuộc người gọi đều trả 404 để không lộ sự tồn tại của id.
        if (rows.isEmpty()
                || (!current.get().isAdmin() && !current.get().id().equals(rows.get(0)[0]))) {
            throw BusinessException.notFound();
        }
        if (!MaintenanceStatus.OPEN.equals(rows.get(0)[1])) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
    }

    @Override()
    public Object updateOpenRequest(Long id, Edit input) {
        guard.requirePermission(Permission.MAINTENANCE_UPDATE);
        requireOpenRequestOfCreator(id);
        jdbc.update(
                "UPDATE maintenance_request SET category=?,severity=?,description=? WHERE id=?",
                input.category(),
                input.severity(),
                input.description().trim(),
                id);
        audit.record(
                current.get().id(),
                AuditAction.MAINTENANCE_UPDATE,
                "MAINTENANCE",
                id,
                null,
                input,
                null);
        return requests.getMaintenanceRequestById(id);
    }

    /** DELETE ở ticket là hủy logic: giữ ảnh và lịch sử để tra cứu, không xóa dữ liệu nghiệp vụ. */
    @Override()
    public void cancelOpenRequest(Long id) {
        guard.requirePermission(Permission.MAINTENANCE_DELETE);
        requireOpenRequestOfCreator(id);
        jdbc.update(
                "UPDATE maintenance_request SET status=? WHERE id=?",
                MaintenanceStatus.CANCELLED,
                id);
        history.record(
                "MAINTENANCE",
                id,
                MaintenanceStatus.OPEN,
                MaintenanceStatus.CANCELLED,
                current.get().id(),
                current.get().isAdmin() ? "Quản trị viên hủy yêu cầu" : "Người tạo hủy yêu cầu");
    }
}

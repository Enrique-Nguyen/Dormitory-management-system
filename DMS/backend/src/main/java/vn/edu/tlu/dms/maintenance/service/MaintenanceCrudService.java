package vn.edu.tlu.dms.maintenance.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.maintenance.api.MaintenanceCrudController.Edit;

@Service
@Transactional
public class MaintenanceCrudService {

    private final JdbcTemplate jdbc;

    private final CurrentUser current;

    private final MaintenanceRequestService requests;

    private final AuditService audit;

    private final StatusHistoryService history;

    public MaintenanceCrudService(
            JdbcTemplate jdbc,
            CurrentUser current,
            MaintenanceRequestService requests,
            AuditService audit,
            StatusHistoryService history) {
        this.jdbc = jdbc;
        this.current = current;
        this.requests = requests;
        this.audit = audit;
        this.history = history;
    }

    private void requireOpenRequestOfCreator(Long id) {
        jdbc.queryForObject(
                "SELECT id FROM maintenance_request WHERE id=? FOR UPDATE", Long.class, id);
        var request = requests.getMaintenanceRequestById(id);
        if (!current.get().id().equals(request.requesterId()) || !"OPEN".equals(request.status())) {
            throw BusinessException.conflict("RESOURCE_IN_USE");
        }
    }

    public Object updateOpenRequest(Long id, Edit input) {
        requireOpenRequestOfCreator(id);
        jdbc.update(
                "UPDATE maintenance_request SET category=?,severity=?,description=? WHERE id=?",
                input.category(),
                input.severity(),
                input.description().trim(),
                id);
        audit.record(
                current.get().id(), "MAINTENANCE_UPDATE", "MAINTENANCE", id, null, input, null);
        return requests.getMaintenanceRequestById(id);
    }

    /** DELETE ở ticket là hủy logic: giữ ảnh và lịch sử để tra cứu, không xóa dữ liệu nghiệp vụ. */
    public void cancelOpenRequest(Long id) {
        requireOpenRequestOfCreator(id);
        jdbc.update("UPDATE maintenance_request SET status='CANCELLED' WHERE id=?", id);
        history.record(
                "MAINTENANCE",
                id,
                "OPEN",
                "CANCELLED",
                current.get().id(),
                "Người tạo hủy yêu cầu");
    }
}

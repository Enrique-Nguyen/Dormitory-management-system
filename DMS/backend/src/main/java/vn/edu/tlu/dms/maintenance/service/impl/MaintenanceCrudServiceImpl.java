package vn.edu.tlu.dms.maintenance.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.maintenance.service.MaintenanceCrudService;
import vn.edu.tlu.dms.maintenance.service.MaintenanceRequestService;

@Service
@Transactional
public class MaintenanceCrudServiceImpl implements MaintenanceCrudService {

    private final JdbcTemplate jdbc;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final MaintenanceRequestService requests;

    private final StatusHistoryService history;

    public MaintenanceCrudServiceImpl(
            JdbcTemplate jdbc,
            CurrentUser current,
            ScopeGuard guard,
            MaintenanceRequestService requests,
            StatusHistoryService history) {
        this.jdbc = jdbc;
        this.current = current;
        this.guard = guard;
        this.requests = requests;
        this.history = history;
    }

    private void requireOpenRequestOfCreator(Long id) {
        var row =
                jdbc
                        .query(
                                "SELECT requester_id,status FROM maintenance_request WHERE id=? FOR"
                                        + " UPDATE",
                                (result, index) ->
                                        new Object[] {
                                            result.getLong("requester_id"),
                                            result.getString("status")
                                        },
                                id)
                        .stream()
                        .findFirst()
                        .orElseThrow(BusinessException::notFound);
        requests.getMaintenanceRequestById(id);
        if ((!current.get().isAdmin() && !current.get().id().equals(row[0]))
                || !MaintenanceStatus.OPEN.equals(row[1])) {
            throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
        }
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
                EntityType.MAINTENANCE,
                id,
                MaintenanceStatus.OPEN,
                MaintenanceStatus.CANCELLED,
                current.get().id(),
                current.get().isAdmin() ? "Quản trị viên hủy yêu cầu" : "Người tạo hủy yêu cầu");
    }
}

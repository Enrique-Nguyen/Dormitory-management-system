package vn.edu.tlu.dms.maintenance.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.maintenance.service.MaintenanceCrudService;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.util.Objects;

@Service
@Transactional
public class MaintenanceCrudServiceImpl implements MaintenanceCrudService {

    private final JdbcTemplate jdbc;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final RoomReferenceService rooms;

    private final StatusHistoryService history;

    public MaintenanceCrudServiceImpl(
            JdbcTemplate jdbc,
            CurrentUser current,
            ScopeGuard guard,
            RoomReferenceService rooms,
            StatusHistoryService history) {
        this.jdbc = jdbc;
        this.current = current;
        this.guard = guard;
        this.rooms = rooms;
        this.history = history;
    }

    private void requireOpenRequestOfCreator(Long id) {
        guard.requireRole(Role.STUDENT, Role.STAFF, Role.TECHNICIAN);
        // Đọc dữ liệu sau khóa bằng JDBC, tránh nạp entity rồi để UPDATE bên dưới làm cache cũ.
        var row =
                jdbc
                        .query(
                                "SELECT requester_id,status,room_id,building_id,assigned_to FROM"
                                        + " maintenance_request WHERE id=? FOR UPDATE",
                                (result, index) ->
                                        new OpenRequest(
                                                result.getLong("requester_id"),
                                                result.getString("status"),
                                                result.getObject("room_id", Long.class),
                                                result.getObject("building_id", Long.class),
                                                result.getObject("assigned_to", Long.class)),
                                id)
                        .stream()
                        .findFirst()
                        .orElseThrow(BusinessException::notFound);
        if (current.get().is(Role.STAFF)) {
            guard.requireUnit(
                    row.roomId() == null
                            ? rooms.getBuildingManagementUnitId(row.buildingId())
                            : rooms.getRoomLocation(row.roomId()).unitId());
        }
        if (current.get().is(Role.STUDENT)
                && !current.get().id().equals(row.requesterId())
                && !Objects.equals(
                        row.roomId(),
                        rooms.findCurrentStayingRoomId(current.get().id()).orElse(-1L))) {
            throw BusinessException.notFound();
        }
        if (current.get().is(Role.TECHNICIAN) && !current.get().id().equals(row.assignedTo())) {
            throw BusinessException.notFound();
        }
        // Ticket của người khác trả 404 để không lộ sự tồn tại của id; ticket đã rời OPEN trả 409.
        if (!current.get().isAdmin() && !current.get().id().equals(row.requesterId())) {
            throw BusinessException.notFound();
        }
        if (!MaintenanceStatus.OPEN.equals(row.status())) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
    }

    private record OpenRequest(
            Long requesterId, String status, Long roomId, Long buildingId, Long assignedTo) {}

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

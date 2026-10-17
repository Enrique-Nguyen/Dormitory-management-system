package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.StayRequestStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.contract.api.ContractDtos.TransferDetail;
import vn.edu.tlu.dms.contract.domain.RoomTransferPolicy;
import vn.edu.tlu.dms.contract.service.RoomTransferService;
import vn.edu.tlu.dms.room.service.BedOccupancyService;
import vn.edu.tlu.dms.room.service.OccupancyQuery.CurrentBed;
import vn.edu.tlu.dms.room.service.OccupancyQuery;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class RoomTransferServiceImpl implements RoomTransferService {

    private static final String SELECT_REQUEST =
            """
            SELECT t.id, t.contract_id, c.code AS contract_code, c.student_id,
                   sp.student_code, u.full_name, t.reason, t.status, t.target_bed_id,
                   t.decided_by, t.decided_at, t.decision_reason, t.created_at,
                   a.preferred_unit_id
            FROM room_transfer_request t
            JOIN dormitory_contract c ON c.id = t.contract_id
            JOIN room_application a ON a.id = c.application_id
            JOIN app_user u ON u.id = c.student_id
            JOIN student_profile sp ON sp.user_id = c.student_id
            WHERE t.id = ?
            """;

    /** Quyền cần có để gọi endpoint của từng hành động trong allowedActions. */
    private static final Map<String, String> ACTION_PERMISSIONS =
            Map.of(
                    BusinessAction.APPROVE, Permission.ROOM_TRANSFER_APPROVE,
                    BusinessAction.REJECT, Permission.ROOM_TRANSFER_APPROVE,
                    BusinessAction.CANCEL, Permission.ROOM_TRANSFER_DELETE);

    private final JdbcTemplate jdbc;

    private final BedOccupancyService occupancy;

    private final OccupancyQuery occupancyQuery;

    private final ScopeGuard guard;

    private final CurrentUser current;

    private final StatusHistoryService history;

    public RoomTransferServiceImpl(
            JdbcTemplate jdbc,
            BedOccupancyService occupancy,
            OccupancyQuery occupancyQuery,
            ScopeGuard guard,
            CurrentUser current,
            StatusHistoryService history) {
        this.jdbc = jdbc;
        this.occupancy = occupancy;
        this.occupancyQuery = occupancyQuery;
        this.guard = guard;
        this.current = current;
        this.history = history;
    }

    /** Dòng yêu cầu đọc từ CSDL cùng đơn vị dự phòng của đơn nguyện vọng. */
    private record Row(
            Long id,
            Long contractId,
            String contractCode,
            Long studentId,
            String studentCode,
            String studentName,
            String reason,
            String status,
            Long targetBedId,
            Long decidedBy,
            OffsetDateTime decidedAt,
            String decisionReason,
            OffsetDateTime createdAt,
            Long preferredUnitId) {}

    /** Ngữ cảnh phạm vi: dòng yêu cầu, giường hiện tại và đơn vị dùng để kiểm tra phạm vi. */
    private record Context(Row row, CurrentBed currentBed, Long unitId) {}

    @Override
    public TransferDetail getTransferRequest(Long id) {
        guard.requirePermission(Permission.ROOM_TRANSFER_READ);
        Context context = load(id, false);
        return toDetail(context);
    }

    @Transactional
    @Override
    public TransferDetail approve(Long id, Long targetBedId) {
        guard.requirePermission(Permission.ROOM_TRANSFER_APPROVE);
        Context context = load(id, true);
        requireAction(context, BusinessAction.APPROVE);
        // Khóa giường → sinh viên trong BedOccupancyService, sau đó mới cập nhật yêu cầu.
        occupancy.transfer(context.row().contractId(), targetBedId, context.row().reason());
        Actor actor = current.get();
        int updated =
                jdbc.update(
                        "UPDATE room_transfer_request SET status=?, target_bed_id=?,"
                                + " decided_by=?, decided_at=CURRENT_TIMESTAMP"
                                + " WHERE id=? AND status=?",
                        StayRequestStatus.APPROVED,
                        targetBedId,
                        actor.id(),
                        id,
                        StayRequestStatus.PENDING);
        requireUpdated(updated);
        history.record(
                EntityType.TRANSFER,
                id,
                StayRequestStatus.PENDING,
                StayRequestStatus.APPROVED,
                actor.id(),
                null);
        return toDetail(load(id, false));
    }

    @Transactional
    @Override
    public TransferDetail reject(Long id, String reason) {
        guard.requirePermission(Permission.ROOM_TRANSFER_APPROVE);
        if (reason == null || reason.isBlank()) {
            throw BusinessException.invalid(ApiField.REASON, ValidationMessage.REQUIRED);
        }
        Context context = load(id, true);
        requireAction(context, BusinessAction.REJECT);
        Actor actor = current.get();
        int updated =
                jdbc.update(
                        "UPDATE room_transfer_request SET status=?, decision_reason=?,"
                                + " decided_by=?, decided_at=CURRENT_TIMESTAMP"
                                + " WHERE id=? AND status=?",
                        StayRequestStatus.REJECTED,
                        reason.strip(),
                        actor.id(),
                        id,
                        StayRequestStatus.PENDING);
        requireUpdated(updated);
        history.record(
                EntityType.TRANSFER,
                id,
                StayRequestStatus.PENDING,
                StayRequestStatus.REJECTED,
                actor.id(),
                reason.strip());
        return toDetail(load(id, false));
    }

    @Transactional
    @Override
    public TransferDetail cancel(Long id) {
        guard.requirePermission(Permission.ROOM_TRANSFER_DELETE);
        guard.requireRole(Role.STUDENT);
        Context context = load(id, true);
        requireAction(context, BusinessAction.CANCEL);
        int updated =
                jdbc.update(
                        "UPDATE room_transfer_request SET status=? WHERE id=? AND status=?",
                        StayRequestStatus.CANCELLED,
                        id,
                        StayRequestStatus.PENDING);
        requireUpdated(updated);
        history.record(
                EntityType.TRANSFER,
                id,
                StayRequestStatus.PENDING,
                StayRequestStatus.CANCELLED,
                current.get().id(),
                null);
        return toDetail(load(id, false));
    }

    /**
     * Đọc yêu cầu và kiểm tra phạm vi: sinh viên chỉ thấy yêu cầu của mình, Staff theo đơn vị của
     * giường hiện tại (không còn giường thì theo đơn vị nguyện vọng). Ngoài phạm vi trả 404.
     */
    private Context load(Long id, boolean writing) {
        guard.requireRole(Role.STAFF, Role.STUDENT);
        Row row =
                jdbc.query(
                                SELECT_REQUEST,
                                (rs, i) ->
                                        new Row(
                                                rs.getLong("id"),
                                                rs.getLong("contract_id"),
                                                rs.getString("contract_code"),
                                                rs.getLong("student_id"),
                                                rs.getString("student_code"),
                                                rs.getString("full_name"),
                                                rs.getString("reason"),
                                                rs.getString("status"),
                                                rs.getObject("target_bed_id", Long.class),
                                                rs.getObject("decided_by", Long.class),
                                                rs.getObject("decided_at", OffsetDateTime.class),
                                                rs.getString("decision_reason"),
                                                rs.getObject("created_at", OffsetDateTime.class),
                                                rs.getLong("preferred_unit_id")),
                                id)
                        .stream()
                        .findFirst()
                        .orElseThrow(BusinessException::notFound);
        CurrentBed bed = occupancyQuery.findOpenAssignmentByContract(row.contractId()).orElse(null);
        Long unitId = bed == null ? row.preferredUnitId() : bed.managementUnitId();
        Actor actor = current.get();
        if (actor.is(Role.STUDENT)) {
            if (!actor.id().equals(row.studentId())) {
                throw BusinessException.notFound();
            }
        } else if (writing) {
            guard.requireUnit(unitId);
        } else {
            guard.requireReadableUnit(unitId);
        }
        return new Context(row, bed, unitId);
    }

    private void requireAction(Context context, String action) {
        RoomTransferPolicy.requireAction(
                context.row().status(),
                context.row().studentId(),
                context.unitId(),
                current.get(),
                action);
    }

    /** Yêu cầu đã rời PENDING giữa lúc đọc và lúc cập nhật: rollback toàn bộ thao tác. */
    private static void requireUpdated(int updated) {
        if (updated == 0) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
    }

    /** allowedActions lấy từ Policy rồi lọc theo quyền đang cấp, để UI chỉ hiện nút gọi được. */
    private TransferDetail toDetail(Context context) {
        Row row = context.row();
        List<String> actions =
                RoomTransferPolicy.allowedActions(
                                row.status(), row.studentId(), context.unitId(), current.get())
                        .stream()
                        .filter(action -> guard.hasPermission(ACTION_PERMISSIONS.get(action)))
                        .toList();
        return new TransferDetail(
                row.id(),
                row.contractId(),
                row.contractCode(),
                row.studentId(),
                row.studentCode(),
                row.studentName(),
                row.reason(),
                row.status(),
                row.targetBedId(),
                row.decidedBy(),
                row.decidedAt(),
                row.decisionReason(),
                row.createdAt(),
                context.currentBed(),
                actions,
                history.list(EntityType.TRANSFER, row.id()));
    }
}

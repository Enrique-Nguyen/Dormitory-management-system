package vn.edu.tlu.dms.room.service.impl;

import jakarta.persistence.EntityManager;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentEndReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.BedOperationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.RoomStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.room.domain.RoomAssignmentHistory;
import vn.edu.tlu.dms.room.repository.RoomAssignmentHistoryRepository;
import vn.edu.tlu.dms.room.repository.RoomRepository;
import vn.edu.tlu.dms.room.service.BedOccupancyService;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Cài đặt dịch vụ chiếm/trả/chuyển giường. Thứ tự khóa toàn cục (SAD 6.2): giường (bed_id tăng
 * dần) rồi hồ sơ sinh viên; mọi điều kiện được kiểm tra lại sau khi khóa. Hai partial unique index
 * ux_assignment_open_bed, ux_assignment_open_student là lưới an toàn cuối, được
 * GlobalExceptionHandler ánh xạ sang BED_TAKEN, STUDENT_ALREADY_ASSIGNED.
 */
@Service
public class BedOccupancyServiceImpl implements BedOccupancyService {

    private static final String LOCK_BEDS_SQL =
            """
            SELECT b.id, b.code, b.status AS bed_status, r.id AS room_id, r.code AS room_code,
                   r.status AS room_status, r.gender, bd.management_unit_id,
                   rt.price_main, rt.price_summer
            FROM bed b
            JOIN room r ON r.id = b.room_id
            JOIN building bd ON bd.id = r.building_id
            JOIN room_type rt ON rt.id = r.room_type_id
            WHERE b.id IN (%s)
            ORDER BY b.id
            FOR UPDATE OF b
            """;

    private final RoomAssignmentHistoryRepository assignments;

    private final RoomRepository rooms;

    private final JdbcTemplate jdbc;

    private final EntityManager entityManager;

    private final ScopeGuard guard;

    private final CurrentUser current;

    private final RoomReferenceService references;

    private final AuditService audit;

    public BedOccupancyServiceImpl(
            RoomAssignmentHistoryRepository assignments,
            RoomRepository rooms,
            JdbcTemplate jdbc,
            EntityManager entityManager,
            ScopeGuard guard,
            CurrentUser current,
            RoomReferenceService references,
            AuditService audit) {
        this.assignments = assignments;
        this.rooms = rooms;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
        this.guard = guard;
        this.current = current;
        this.references = references;
        this.audit = audit;
    }

    @Override
    @Transactional
    public Assignment occupyHold(Long bedId, Long studentId, Long contractId, String note) {
        return occupy(bedId, studentId, contractId, AssignmentKind.HOLD, note);
    }

    @Override
    @Transactional
    public Assignment occupyStay(Long bedId, Long studentId, Long contractId, String note) {
        return occupy(bedId, studentId, contractId, AssignmentKind.STAY, note);
    }

    @Override
    @Transactional
    public Assignment convertHoldToStay(Long contractId, Long actorId) {
        var open =
                findOpenRow(contractId)
                        .orElseThrow(
                                () -> BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION));
        var bed = lockBeds(List.of(open.bedId())).get(0);
        references.lockStudentProfile(open.studentId());
        var row = reloadOpenAssignment(contractId, open.bedId());
        if (!AssignmentKind.HOLD.equals(row.getKind())) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        row.setKind(AssignmentKind.STAY);
        row.setStayStartedAt(OffsetDateTime.now());
        assignments.saveAndFlush(row);
        audit.record(
                actorId,
                AuditAction.BED_STAY,
                EntityType.ASSIGNMENT,
                row.getId(),
                AssignmentKind.HOLD,
                AssignmentKind.STAY,
                null);
        return toAssignment(row, bed);
    }

    @Override
    @Transactional
    public Optional<Assignment> release(
            Long contractId, String endReason, Long actorId, String note) {
        if (!AssignmentEndReason.RELEASE.contains(endReason)) {
            throw BusinessException.invalid(
                    ApiField.END_REASON, "Lý do đóng phân phòng không hợp lệ");
        }
        var open = findOpenRow(contractId);
        if (open.isEmpty()) {
            return Optional.empty();
        }
        var bed = lockBeds(List.of(open.get().bedId())).get(0);
        references.lockStudentProfile(open.get().studentId());
        var row = findOpenAssignment(contractId).orElse(null);
        if (row == null) {
            return Optional.empty();
        }
        if (!row.getBedId().equals(bed.id())) {
            throw BusinessException.conflict(ErrorCode.CONCURRENT_UPDATE);
        }
        close(row, endReason, note);
        removeLeaderIfNeeded(bed.roomId(), row.getStudentId(), actorId);
        audit.record(
                actorId,
                AuditAction.BED_RELEASE,
                EntityType.ASSIGNMENT,
                row.getId(),
                null,
                Map.of(ApiField.BED_ID, row.getBedId(), ApiField.END_REASON, endReason),
                note);
        return Optional.of(toAssignment(row, bed));
    }

    @Override
    @Transactional
    public Assignment transfer(Long contractId, Long targetBedId, String note) {
        if (targetBedId == null) {
            throw BusinessException.invalid(ApiField.TARGET_BED_ID, ValidationMessage.REQUIRED);
        }
        var open =
                findOpenRow(contractId)
                        .orElseThrow(() -> BusinessException.conflict(ErrorCode.NOT_STAYING));
        if (open.bedId().equals(targetBedId)) {
            throw BusinessException.conflict(ErrorCode.SAME_BED);
        }
        // BR-05: khóa hai giường theo bed_id tăng dần trong một câu lệnh để tránh deadlock khi hai
        // yêu cầu chuyển chéo nhau.
        var locked = lockBeds(List.of(open.bedId(), targetBedId));
        if (locked.size() != 2) {
            throw BusinessException.notFound();
        }
        var source = locked.get(0).id().equals(open.bedId()) ? locked.get(0) : locked.get(1);
        var target = source == locked.get(0) ? locked.get(1) : locked.get(0);
        guard.requireUnit(source.unitId());
        if (!Objects.equals(source.unitId(), target.unitId())) {
            throw BusinessException.conflict(ErrorCode.TRANSFER_CROSS_UNIT);
        }
        references.lockStudentProfile(open.studentId());
        var row = reloadOpenAssignment(contractId, source.id());
        if (!AssignmentKind.STAY.equals(row.getKind())) {
            throw BusinessException.conflict(ErrorCode.NOT_STAYING);
        }
        requireBedAvailable(target);
        requireGender(target, row.getStudentId());

        Long actorId = current.get().id();
        var now = OffsetDateTime.now();
        close(row, AssignmentEndReason.TRANSFER, note);
        var next = newAssignment(target, row.getStudentId(), contractId, AssignmentKind.STAY, note);
        next.setStartedAt(now);
        next.setStayStartedAt(now);
        assignments.saveAndFlush(next);
        removeLeaderIfNeeded(source.roomId(), row.getStudentId(), actorId);
        var change = new LinkedHashMap<String, Object>();
        change.put("fromBedId", source.id());
        change.put("toBedId", target.id());
        change.put("closedAssignmentId", row.getId());
        audit.record(
                actorId,
                AuditAction.ROOM_TRANSFER,
                EntityType.ASSIGNMENT,
                next.getId(),
                Map.of(ApiField.BED_ID, source.id()),
                change,
                note);
        return toAssignment(next, target);
    }

    private Assignment occupy(
            Long bedId, Long studentId, Long contractId, String kind, String note) {
        if (bedId == null) {
            throw BusinessException.invalid(ApiField.BED_ID, ValidationMessage.REQUIRED);
        }
        if (studentId == null || contractId == null) {
            throw BusinessException.notFound();
        }
        var locked = lockBeds(List.of(bedId));
        if (locked.isEmpty()) {
            throw BusinessException.notFound();
        }
        var bed = locked.get(0);
        guard.requireUnit(bed.unitId());
        references.lockStudentProfile(studentId);
        requireBedAvailable(bed);
        if (exists(
                "SELECT EXISTS (SELECT 1 FROM room_assignment_history WHERE student_id=? AND"
                        + " ended_at IS NULL)",
                studentId)) {
            throw BusinessException.conflict(ErrorCode.STUDENT_ALREADY_ASSIGNED);
        }
        requireGender(bed, studentId);

        var row = newAssignment(bed, studentId, contractId, kind, note);
        var now = OffsetDateTime.now();
        row.setStartedAt(now);
        if (AssignmentKind.STAY.equals(kind)) {
            row.setStayStartedAt(now);
        }
        assignments.saveAndFlush(row);
        var change = new LinkedHashMap<String, Object>();
        change.put(ApiField.BED_ID, bedId);
        change.put("studentId", studentId);
        change.put("contractId", contractId);
        change.put("kind", kind);
        audit.record(
                row.getCreatedBy(),
                AuditAction.BED_OCCUPY,
                EntityType.ASSIGNMENT,
                row.getId(),
                null,
                change,
                note);
        return toAssignment(row, bed);
    }

    /** Giường và phòng phải ACTIVE, giường không có phân phòng mở (BR-01). Gọi sau khi khóa. */
    private void requireBedAvailable(LockedBed bed) {
        if (!BedOperationStatus.ACTIVE.equals(bed.bedStatus())
                || !RoomStatus.ACTIVE.equals(bed.roomStatus())) {
            var details = new LinkedHashMap<String, Object>();
            details.put(ApiField.BED_CODE, bed.fullCode());
            details.put("bedStatus", bed.bedStatus());
            details.put("roomStatus", bed.roomStatus());
            throw new BusinessException(ErrorCode.BED_NOT_AVAILABLE, HttpStatus.CONFLICT, details);
        }
        if (exists(
                "SELECT EXISTS (SELECT 1 FROM room_assignment_history WHERE bed_id=? AND"
                        + " ended_at IS NULL)",
                bed.id())) {
            throw new BusinessException(
                    ErrorCode.BED_TAKEN,
                    HttpStatus.CONFLICT,
                    Map.of(ApiField.BED_ID, bed.id(), ApiField.BED_CODE, bed.fullCode()));
        }
    }

    private void requireGender(LockedBed bed, Long studentId) {
        String gender =
                jdbc.queryForObject(
                        "SELECT gender FROM student_profile WHERE user_id=?",
                        String.class,
                        studentId);
        if (!bed.roomGender().equals(gender)) {
            throw new BusinessException(
                    ErrorCode.GENDER_MISMATCH,
                    HttpStatus.CONFLICT,
                    Map.of(
                            ApiField.ROOM_CODE,
                            bed.roomCode(),
                            ApiField.ROOM_GENDER,
                            bed.roomGender()));
        }
    }

    /** Khóa các giường theo ID tăng dần và đọc kèm phòng, đơn vị, đơn giá loại phòng. */
    private List<LockedBed> lockBeds(List<Long> bedIds) {
        String placeholders = String.join(",", bedIds.stream().map(id -> "?").toList());
        return jdbc.query(
                LOCK_BEDS_SQL.formatted(placeholders),
                (rs, i) ->
                        new LockedBed(
                                rs.getLong("id"),
                                rs.getString("code"),
                                rs.getString("bed_status"),
                                rs.getLong("room_id"),
                                rs.getString("room_code"),
                                rs.getString("room_status"),
                                rs.getString("gender"),
                                rs.getLong("management_unit_id"),
                                rs.getLong("price_main"),
                                rs.getLong("price_summer")),
                bedIds.toArray());
    }

    /** Đọc nhanh phân phòng mở của hợp đồng (chưa khóa) để biết giường cần khóa. */
    private Optional<OpenRow> findOpenRow(Long contractId) {
        return jdbc
                .query(
                        "SELECT bed_id, student_id FROM room_assignment_history WHERE"
                                + " contract_id=? AND ended_at IS NULL",
                        (rs, i) -> new OpenRow(rs.getLong("bed_id"), rs.getLong("student_id")),
                        contractId)
                .stream()
                .findFirst();
    }

    /** Đọc lại phân phòng mở sau khi khóa; bỏ qua bản đã nạp trước đó trong persistence context. */
    private Optional<RoomAssignmentHistory> findOpenAssignment(Long contractId) {
        var row =
                assignments.findOne(
                        (a, q, b) ->
                                b.and(
                                        b.equal(a.get("contractId"), contractId),
                                        b.isNull(a.get("endedAt"))));
        row.ifPresent(entityManager::refresh);
        return row.filter(r -> r.getEndedAt() == null);
    }

    private RoomAssignmentHistory reloadOpenAssignment(Long contractId, Long expectedBedId) {
        var row =
                findOpenAssignment(contractId)
                        .orElseThrow(() -> BusinessException.conflict(ErrorCode.NOT_STAYING));
        if (!row.getBedId().equals(expectedBedId)) {
            throw BusinessException.conflict(ErrorCode.CONCURRENT_UPDATE);
        }
        return row;
    }

    /** Đóng dòng cũ và flush ngay, để dòng mới không vấp ux_assignment_open_student. */
    private void close(RoomAssignmentHistory row, String endReason, String note) {
        row.setEndedAt(OffsetDateTime.now());
        row.setEndReason(endReason);
        if (note != null && !note.isBlank()) {
            row.setNote(note.trim());
        }
        assignments.saveAndFlush(row);
    }

    private RoomAssignmentHistory newAssignment(
            LockedBed bed, Long studentId, Long contractId, String kind, String note) {
        var row = new RoomAssignmentHistory();
        row.setStudentId(studentId);
        row.setContractId(contractId);
        row.setBedId(bed.id());
        row.setKind(kind);
        row.setPriceMain(bed.priceMain());
        row.setPriceSummer(bed.priceSummer());
        row.setNote(note == null || note.isBlank() ? null : note.trim());
        row.setCreatedBy(current.get().id());
        return row;
    }

    /** FR-ROOM-10, BR-11: trưởng phòng mất phân phòng trong phòng thì tự gỡ và ghi audit. */
    private void removeLeaderIfNeeded(Long roomId, Long studentId, Long actorId) {
        var room = rooms.findById(roomId).orElseThrow(BusinessException::notFound);
        entityManager.refresh(room);
        if (!studentId.equals(room.getLeaderStudentId())) {
            return;
        }
        room.setLeaderStudentId(null);
        rooms.saveAndFlush(room);
        audit.record(
                actorId,
                AuditAction.ROOM_LEADER_CHANGE,
                EntityType.ROOM,
                roomId,
                studentId,
                null,
                AuditReason.LEADER_AUTO_REMOVED);
    }

    private boolean exists(String sql, Object... args) {
        return Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, args));
    }

    private Assignment toAssignment(RoomAssignmentHistory row, LockedBed bed) {
        return new Assignment(
                row.getId(),
                row.getStudentId(),
                row.getContractId(),
                bed.id(),
                bed.fullCode(),
                bed.roomId(),
                bed.roomCode(),
                row.getKind(),
                row.getPriceMain(),
                row.getPriceSummer());
    }

    private record OpenRow(Long bedId, Long studentId) {}

    private record LockedBed(
            Long id,
            String code,
            String bedStatus,
            Long roomId,
            String roomCode,
            String roomStatus,
            String roomGender,
            Long unitId,
            Long priceMain,
            Long priceSummer) {

        /** Mã hiển thị phòng-giường, ví dụ A101-03, dùng trong thông báo lỗi. */
        String fullCode() {
            return roomCode + "-" + code;
        }
    }
}

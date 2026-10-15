package vn.edu.tlu.dms.room.service.impl;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditEntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.BedOperationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.PlacementSource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.RoomStatus;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.PlacementContextLookup;
import vn.edu.tlu.dms.common.service.PlacementContextLookup.PlacementContext;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.room.api.RoomDtos.AvailableRoom;
import vn.edu.tlu.dms.room.api.RoomDtos.BedInput;
import vn.edu.tlu.dms.room.api.RoomDtos.BedResponse;
import vn.edu.tlu.dms.room.api.RoomDtos.BedStatus;
import vn.edu.tlu.dms.room.api.RoomDtos.FreeBed;
import vn.edu.tlu.dms.room.api.RoomDtos.Input;
import vn.edu.tlu.dms.room.api.RoomDtos.LeaderChange;
import vn.edu.tlu.dms.room.api.RoomDtos.Occupant;
import vn.edu.tlu.dms.room.api.RoomDtos.Response;
import vn.edu.tlu.dms.room.api.RoomDtos.RoomRef;
import vn.edu.tlu.dms.room.api.RoomDtos.RoomTypeRef;
import vn.edu.tlu.dms.room.api.RoomDtos.Status;
import vn.edu.tlu.dms.room.api.RoomDtos.StudentRef;
import vn.edu.tlu.dms.room.domain.Bed;
import vn.edu.tlu.dms.room.domain.Building;
import vn.edu.tlu.dms.room.domain.Room;
import vn.edu.tlu.dms.room.domain.RoomAssignmentHistory;
import vn.edu.tlu.dms.room.repository.BedRepository;
import vn.edu.tlu.dms.room.repository.RoomAssignmentHistoryRepository;
import vn.edu.tlu.dms.room.repository.RoomRepository;
import vn.edu.tlu.dms.room.repository.RoomTypeRepository;
import vn.edu.tlu.dms.room.service.OccupancyQuery;
import vn.edu.tlu.dms.room.service.RoomReferenceService;
import vn.edu.tlu.dms.room.service.RoomService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final RoomRepository rooms;

    private final BedRepository beds;

    private final RoomTypeRepository types;

    private final RoomAssignmentHistoryRepository assignments;

    private final JdbcTemplate jdbc;

    private final ScopeGuard guard;

    private final CurrentUser current;

    private final RoomReferenceService references;

    private final AccountReferenceService accounts;

    private final AuditService audit;

    private final OccupancyQuery occupancy;

    private final List<PlacementContextLookup> placementLookups;

    public RoomServiceImpl(
            RoomRepository rooms,
            BedRepository beds,
            RoomTypeRepository types,
            RoomAssignmentHistoryRepository assignments,
            JdbcTemplate jdbc,
            ScopeGuard guard,
            CurrentUser current,
            RoomReferenceService references,
            AccountReferenceService accounts,
            AuditService audit,
            OccupancyQuery occupancy,
            List<PlacementContextLookup> placementLookups) {
        this.rooms = rooms;
        this.beds = beds;
        this.types = types;
        this.assignments = assignments;
        this.jdbc = jdbc;
        this.guard = guard;
        this.current = current;
        this.references = references;
        this.accounts = accounts;
        this.audit = audit;
        this.occupancy = occupancy;
        this.placementLookups = placementLookups;
    }

    @Override
    public PageResponse<Response> searchRooms(
            int page,
            int size,
            List<String> sort,
            Long buildingId,
            Short floor,
            Long roomTypeId,
            String gender,
            String status,
            Boolean hasFreeBed,
            Boolean noLeader,
            String keyword) {
        guard.requirePermission(Permission.ROOM_READ);
        guard.requireRole(Role.STAFF);
        var actor = current.get();
        var pageable =
                PageQuery.of(
                        page, size, sort, Set.of("id", "code", "floor", "buildingId"), "code,asc");
        Specification<Room> spec =
                (r, q, b) -> {
                    var filters = new ArrayList<Predicate>();
                    if (keyword != null && !keyword.isBlank()) {
                        filters.add(
                                b.like(
                                        b.lower(r.get("code")),
                                        LikePattern.contains(keyword.trim().toLowerCase(Locale.ROOT))));
                    }
                    var building = q.subquery(Long.class);
                    var bd = building.from(Building.class);
                    building.select(bd.get("id"))
                            .where(
                                    actor.canReadAllUnits()
                                            ? b.conjunction()
                                            : b.equal(bd.get("managementUnitId"), actor.unitId()));
                    filters.add(r.get("buildingId").in(building));
                    if (buildingId != null) {
                        filters.add(b.equal(r.get("buildingId"), buildingId));
                    }
                    if (floor != null) {
                        filters.add(b.equal(r.get("floor"), floor));
                    }
                    if (roomTypeId != null) {
                        filters.add(b.equal(r.get("roomTypeId"), roomTypeId));
                    }
                    if (gender != null) {
                        filters.add(b.equal(r.get("gender"), gender));
                    }
                    if (status != null) {
                        filters.add(b.equal(r.get("status"), status));
                    }
                    if (noLeader != null) {
                        filters.add(
                                noLeader
                                        ? b.isNull(r.get("leaderStudentId"))
                                        : b.isNotNull(r.get("leaderStudentId")));
                    }
                    if (hasFreeBed != null) {
                        var free = q.subquery(Long.class);
                        var bed = free.from(Bed.class);
                        var occupied = free.subquery(Long.class);
                        var a = occupied.from(RoomAssignmentHistory.class);
                        occupied.select(a.get("id"))
                                .where(
                                        b.equal(a.get("bedId"), bed.get("id")),
                                        b.isNull(a.get("endedAt")));
                        free.select(bed.get("id"))
                                .where(
                                        b.equal(bed.get("roomId"), r.get("id")),
                                        b.equal(bed.get("status"), BedOperationStatus.ACTIVE),
                                        b.not(b.exists(occupied)));
                        filters.add(hasFreeBed ? b.exists(free) : b.not(b.exists(free)));
                    }
                    return b.and(filters.toArray(Predicate[]::new));
                };
        var found = rooms.findAll(spec, pageable);
        // Nạp số liệu công suất của cả trang bằng một truy vấn thay vì mỗi phòng một truy vấn.
        var occupancy = loadOccupancy(found.getContent().stream().map(Room::getId).toList());
        return PageResponse.from(
                found.map(r -> toRoomResponse(r, false, occupancy.get(r.getId()))));
    }

    @Override
    public Response getRoomById(Long id) {
        guard.requirePermission(Permission.ROOM_READ);
        guard.requireRole(Role.STAFF);
        var r = findAccessibleRoom(id, false);
        return toRoomResponse(r, true, null);
    }

    @Override
    @Transactional
    public Response createRoom(Input input) {
        guard.requirePermission(Permission.ROOM_CREATE);
        requireValue(input.buildingId(), "buildingId");
        requireValue(input.roomTypeId(), "roomTypeId");
        guard.requireUnit(references.getBuildingManagementUnitId(input.buildingId()));
        references.requireActiveRoomType(input.roomTypeId());
        var type = types.findById(input.roomTypeId()).orElseThrow(BusinessException::notFound);
        var r = new Room();
        r.setBuildingId(input.buildingId());
        r.setRoomTypeId(input.roomTypeId());
        r.setFloor(requireValue(input.floor(), "floor"));
        r.setCode(trimRequiredText(input.code(), "code"));
        r.setGender(requireValue(input.gender(), "gender"));
        r.setStatus(RoomStatus.ACTIVE);
        rooms.saveAndFlush(r);
        syncBeds(r, type.getBedCount());
        audit.record(
                current.get().id(),
                AuditAction.ROOM_CREATE,
                AuditEntityType.ROOM,
                r.getId(),
                null,
                input,
                null);
        return toRoomResponse(r, true, null);
    }

    @Override
    @Transactional
    public Response updateRoom(Long id, Input input) {
        guard.requirePermission(Permission.ROOM_UPDATE);
        lockRoomBedsInAscendingOrder(id);
        var r = findAccessibleRoom(id, true);
        if (input.buildingId() != null && !input.buildingId().equals(r.getBuildingId())) {
            throw BusinessException.invalid("buildingId", "Không chuyển phòng sang tòa khác");
        }
        boolean sensitive =
                input.gender() != null && !input.gender().equals(r.getGender())
                        || input.roomTypeId() != null
                                && !input.roomTypeId().equals(r.getRoomTypeId());
        if (sensitive) {
            requireRoomWithoutOpenAssignments(r);
        }
        if (input.floor() != null) {
            r.setFloor(input.floor());
        }
        if (input.code() != null) {
            r.setCode(trimRequiredText(input.code(), "code"));
        }
        if (input.gender() != null) {
            r.setGender(input.gender());
        }
        boolean typeChanged =
                input.roomTypeId() != null && !input.roomTypeId().equals(r.getRoomTypeId());
        if (input.roomTypeId() != null) {
            references.requireActiveRoomType(input.roomTypeId());
            r.setRoomTypeId(input.roomTypeId());
        }
        rooms.saveAndFlush(r);
        if (typeChanged) {
            // Số giường phải khớp loại phòng mới, nếu không công suất hiển thị và giá sẽ lệch nhau.
            syncBeds(
                    r,
                    types.findById(r.getRoomTypeId())
                            .orElseThrow(BusinessException::notFound)
                            .getBedCount());
        }
        audit.record(
                current.get().id(),
                AuditAction.ROOM_UPDATE,
                AuditEntityType.ROOM,
                id,
                null,
                input,
                null);
        return toRoomResponse(r, true, null);
    }

    @Override
    @Transactional
    public Response changeRoomStatus(Long id, Status input) {
        guard.requirePermission(Permission.ROOM_UPDATE);
        lockRoomBedsInAscendingOrder(id);
        var r = findAccessibleRoom(id, true);
        if (!input.status().equals(RoomStatus.ACTIVE)) {
            requireRoomWithoutOpenAssignments(r);
        }
        String before = r.getStatus();
        r.setStatus(input.status());
        rooms.saveAndFlush(r);
        audit.record(
                current.get().id(),
                AuditAction.ROOM_STATUS,
                AuditEntityType.ROOM,
                id,
                before,
                input.status(),
                trimRequiredText(input.reason(), "reason"));
        return toRoomResponse(r, true, null);
    }

    @Override
    @Transactional
    public BedResponse addBedToRoom(Long roomId, BedInput input) {
        guard.requirePermission(Permission.BED_CREATE);
        var r = findAccessibleRoom(roomId, true);
        var bed = new Bed();
        bed.setRoomId(r.getId());
        bed.setCode(trimRequiredText(input.code(), "code"));
        bed.setStatus(BedOperationStatus.ACTIVE);
        beds.saveAndFlush(bed);
        return toBedResponse(bed);
    }

    @Override
    @Transactional
    public BedResponse updateBedCode(Long id, BedInput input) {
        guard.requirePermission(Permission.BED_UPDATE);
        var bed = lockAndFindBed(id);
        findAccessibleRoom(bed.getRoomId(), true);
        String previous = bed.getCode();
        bed.setCode(trimRequiredText(input.code(), "code"));
        beds.saveAndFlush(bed);
        audit.record(
                current.get().id(),
                AuditAction.BED_UPDATE,
                AuditEntityType.BED,
                id,
                previous,
                bed.getCode(),
                null);
        return toBedResponse(bed);
    }

    @Override
    @Transactional
    public BedResponse changeBedStatus(Long id, BedStatus input) {
        guard.requirePermission(Permission.BED_UPDATE);
        var bed = lockAndFindBed(id);
        findAccessibleRoom(bed.getRoomId(), true);
        if (!input.status().equals(BedOperationStatus.ACTIVE) && hasOpenBedAssignment(id)) {
            throw BusinessException.conflict(ErrorCode.BED_OCCUPIED);
        }
        String before = bed.getStatus();
        bed.setStatus(input.status());
        beds.saveAndFlush(bed);
        audit.record(
                current.get().id(),
                AuditAction.BED_STATUS,
                AuditEntityType.BED,
                id,
                before,
                input.status(),
                trimRequiredText(input.reason(), "reason"));
        return toBedResponse(bed);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        guard.requirePermission(Permission.ROOM_DELETE);
        lockRoomBedsInAscendingOrder(id);
        var r = findAccessibleRoom(id, true);
        if (jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM room_assignment_history a JOIN bed b ON b.id=a.bed_id"
                        + " WHERE b.room_id=?)",
                Boolean.class,
                id)) {
            throw BusinessException.conflict(ErrorCode.ROOM_OCCUPIED);
        }
        var rows = beds.findAll((b, q, cb) -> cb.equal(b.get("roomId"), id));
        beds.deleteAll(rows);
        beds.flush();
        rooms.delete(r);
        rooms.flush();
        audit.record(
                current.get().id(),
                AuditAction.ROOM_DELETE,
                AuditEntityType.ROOM,
                id,
                r.getCode(),
                null,
                null);
    }

    @Override
    @Transactional
    public void deleteBed(Long id) {
        guard.requirePermission(Permission.BED_DELETE);
        var bed = lockAndFindBed(id);
        findAccessibleRoom(bed.getRoomId(), true);
        if (assignments.exists((r, q, b) -> b.equal(r.get("bedId"), id))) {
            throw BusinessException.conflict(ErrorCode.BED_OCCUPIED);
        }
        beds.delete(bed);
        beds.flush();
        audit.record(
                current.get().id(),
                AuditAction.BED_DELETE,
                AuditEntityType.BED,
                id,
                bed.getCode(),
                null,
                null);
    }

    @Override
    @Transactional
    public Response assignRoomLeader(Long id, Long studentId) {
        guard.requirePermission(Permission.ROOM_UPDATE);
        lockRoomBedsInAscendingOrder(id);
        var r = findAccessibleRoom(id, true);
        boolean staying =
                jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM v_current_stay WHERE room_id=? AND"
                                + " student_id=? AND assignment_kind='STAY')",
                        Boolean.class,
                        id,
                        studentId);
        if (!staying) {
            throw new BusinessException(
                    ErrorCode.LEADER_NOT_STAYING,
                    HttpStatus.CONFLICT,
                    Map.of("roomCode", r.getCode()));
        }
        Long before = r.getLeaderStudentId();
        if (studentId.equals(before)) {
            return toRoomResponse(r, true, null);
        }
        // Báo rõ phòng đang do sinh viên làm trưởng; ux_room_leader vẫn chặn khi chạy đồng thời.
        var leading =
                jdbc.queryForList(
                        "SELECT code FROM room WHERE leader_student_id=? AND id<>?",
                        String.class,
                        studentId,
                        id);
        if (!leading.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.LEADER_ALREADY_ASSIGNED,
                    HttpStatus.CONFLICT,
                    Map.of("roomCode", leading.get(0)));
        }
        r.setLeaderStudentId(studentId);
        rooms.saveAndFlush(r);
        audit.record(
                current.get().id(),
                AuditAction.ROOM_LEADER_CHANGE,
                AuditEntityType.ROOM,
                id,
                before,
                studentId,
                null);
        return toRoomResponse(r, true, null);
    }

    @Override
    @Transactional
    public void removeRoomLeader(Long id, String reason) {
        guard.requirePermission(Permission.ROOM_UPDATE);
        lockRoomBedsInAscendingOrder(id);
        var r = findAccessibleRoom(id, true);
        String trimmedReason = trimRequiredText(reason, "reason");
        Long before = r.getLeaderStudentId();
        if (before == null) {
            // Đã không có trưởng phòng (gỡ trùng hoặc vừa tự gỡ): không ghi thêm lịch sử.
            return;
        }
        r.setLeaderStudentId(null);
        rooms.saveAndFlush(r);
        audit.record(
                current.get().id(),
                AuditAction.ROOM_LEADER_CHANGE,
                AuditEntityType.ROOM,
                id,
                before,
                null,
                trimmedReason);
    }

    @Override
    public List<AvailableRoom> findAvailableBeds(
            Long applicationId, Long transferRequestId, Long buildingId, Long roomTypeId) {
        guard.requirePermission(Permission.BED_READ);
        if ((applicationId == null) == (transferRequestId == null)) {
            throw BusinessException.invalid(
                    PlacementSource.APPLICATION,
                    "Truyền đúng một trong applicationId hoặc transferRequestId");
        }
        String source =
                applicationId != null
                        ? PlacementSource.APPLICATION
                        : PlacementSource.TRANSFER_REQUEST;
        Long sourceId = applicationId != null ? applicationId : transferRequestId;
        PlacementContext context =
                placementLookups.stream()
                        .filter(lookup -> lookup.source().equals(source))
                        .findFirst()
                        .flatMap(lookup -> lookup.find(sourceId))
                        .orElseThrow(BusinessException::notFound);
        Long unitId = context.preferredUnitId();
        if (unitId == null) {
            // BR-05: chỉ giường cùng đơn vị với giường hiện tại của hợp đồng.
            unitId =
                    occupancy
                            .findOpenAssignmentByContract(context.contractId())
                            .orElseThrow(() -> BusinessException.conflict(ErrorCode.NOT_STAYING))
                            .managementUnitId();
        }
        guard.requireUnit(unitId);
        if (!context.ready()) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        String gender =
                jdbc.queryForObject(
                        "SELECT gender FROM student_profile WHERE user_id=?",
                        String.class,
                        context.studentId());
        Long preferredType = context.preferredRoomTypeId();
        Map<Long, AvailableRoom> grouped = new LinkedHashMap<>();
        jdbc.query(
                """
                SELECT r.id AS room_id, r.code AS room_code, r.floor, r.gender,
                       bd.id AS building_id, bd.code AS building_code,
                       rt.id AS room_type_id, rt.name AS room_type_name, rt.price_main,
                       o.active_beds, o.occupied_beds, o.held_beds,
                       b.id AS bed_id, b.code AS bed_code
                FROM bed b
                JOIN room r ON r.id = b.room_id
                JOIN building bd ON bd.id = r.building_id
                JOIN room_type rt ON rt.id = r.room_type_id
                JOIN v_room_occupancy o ON o.room_id = r.id
                WHERE bd.management_unit_id = ? AND r.gender = ?
                  AND r.status = ? AND b.status = ?
                  AND NOT EXISTS (SELECT 1 FROM room_assignment_history a
                                  WHERE a.bed_id = b.id AND a.ended_at IS NULL)
                  AND (CAST(? AS BIGINT) IS NULL OR bd.id = ?)
                  AND (CAST(? AS BIGINT) IS NULL OR rt.id = ?)
                ORDER BY (rt.id = CAST(? AS BIGINT)) DESC NULLS LAST, bd.code, r.code, b.code
                """,
                rs -> {
                    Long roomId = rs.getLong("room_id");
                    var room = grouped.get(roomId);
                    if (room == null) {
                        Long typeId = rs.getLong("room_type_id");
                        room =
                                new AvailableRoom(
                                        new RoomRef(
                                                roomId,
                                                rs.getString("room_code"),
                                                rs.getLong("building_id"),
                                                rs.getString("building_code"),
                                                rs.getShort("floor"),
                                                new RoomTypeRef(
                                                        typeId, rs.getString("room_type_name")),
                                                rs.getString("gender"),
                                                rs.getLong("price_main")),
                                        Objects.equals(typeId, preferredType),
                                        new ArrayList<>(),
                                        rs.getLong("active_beds"),
                                        rs.getLong("occupied_beds"),
                                        rs.getLong("held_beds"));
                        grouped.put(roomId, room);
                    }
                    room.freeBeds()
                            .add(new FreeBed(rs.getLong("bed_id"), rs.getString("bed_code")));
                },
                unitId,
                gender,
                RoomStatus.ACTIVE,
                BedOperationStatus.ACTIVE,
                buildingId,
                buildingId,
                roomTypeId,
                roomTypeId,
                preferredType);
        return List.copyOf(grouped.values());
    }

    /**
     * Đưa số giường ACTIVE của phòng về đúng sức chứa của loại phòng. Thêm: kích hoạt lại giường
     * RETIRED trước, rồi mới tạo giường mang mã hai chữ số còn thiếu. Bớt: từ mã lớn nhất, giường
     * chưa từng có phân phòng thì xóa, giường đã có lịch sử chuyển RETIRED để giữ lịch sử. Giường
     * BROKEN không bị đụng tới. Gọi sau khi đã khóa các giường và xác nhận phòng không có phân phòng
     * mở.
     */
    private void syncBeds(Room r, int target) {
        var all = new ArrayList<>(beds.findAll((b, q, cb) -> cb.equal(b.get("roomId"), r.getId())));
        all.sort(Comparator.comparing(Bed::getCode));
        Set<String> used = new HashSet<>();
        all.forEach(b -> used.add(b.getCode()));
        var active = new ArrayList<Bed>();
        all.stream().filter(b -> BedOperationStatus.ACTIVE.equals(b.getStatus())).forEach(active::add);
        for (Bed bed : all) {
            if (active.size() >= target) {
                break;
            }
            if (BedOperationStatus.RETIRED.equals(bed.getStatus())) {
                bed.setStatus(BedOperationStatus.ACTIVE);
                active.add(beds.save(bed));
            }
        }
        for (int number = 1; active.size() < target; number++) {
            String code = String.format("%02d", number);
            if (used.add(code)) {
                var bed = new Bed();
                bed.setRoomId(r.getId());
                bed.setCode(code);
                bed.setStatus(BedOperationStatus.ACTIVE);
                active.add(beds.save(bed));
            }
        }
        active.sort(Comparator.comparing(Bed::getCode));
        for (int i = active.size() - 1; active.size() > target; i--) {
            var bed = active.get(i);
            Long bedId = bed.getId();
            if (assignments.exists((a, q, cb) -> cb.equal(a.get("bedId"), bedId))) {
                bed.setStatus(BedOperationStatus.RETIRED);
                beds.save(bed);
            } else {
                beds.delete(bed);
            }
            active.remove(i);
        }
        beds.flush();
    }

    private Room findAccessibleRoom(Long id, boolean writing) {
        var r = rooms.findById(id).orElseThrow(BusinessException::notFound);
        if (writing) {
            guard.requireUnit(references.getBuildingManagementUnitId(r.getBuildingId()));
        } else {
            guard.requireReadableUnit(references.getBuildingManagementUnitId(r.getBuildingId()));
        }
        return r;
    }

    /**
     * Mọi kiểm tra phòng trống phải diễn ra sau khi khóa giường theo ID tăng dần. Khi bổ sung
     * nghiệp vụ phân giường, service đó phải dùng cùng thứ tự khóa để tránh race/deadlock.
     */
    private void lockRoomBedsInAscendingOrder(Long id) {
        jdbc.query(
                "SELECT id FROM bed WHERE room_id=? ORDER BY id FOR UPDATE",
                (r, i) -> r.getLong(1),
                id);
    }

    private Bed lockAndFindBed(Long id) {
        jdbc.query("SELECT id FROM bed WHERE id=? FOR UPDATE", (r, i) -> r.getLong(1), id);
        return beds.findById(id).orElseThrow(BusinessException::notFound);
    }

    private boolean hasOpenBedAssignment(Long id) {
        return assignments.exists(
                (r, q, b) -> b.and(b.equal(r.get("bedId"), id), b.isNull(r.get("endedAt"))));
    }

    private void requireRoomWithoutOpenAssignments(Room r) {
        long count =
                jdbc.queryForObject(
                        "SELECT held_beds+occupied_beds FROM v_room_occupancy WHERE room_id=?",
                        Long.class,
                        r.getId());
        if (count > 0) {
            throw new BusinessException(
                    ErrorCode.ROOM_OCCUPIED,
                    HttpStatus.CONFLICT,
                    Map.of("roomCode", r.getCode(), "count", count));
        }
    }

    private Map<Long, Map<String, Object>> loadOccupancy(List<Long> roomIds) {
        Map<Long, Map<String, Object>> result = new HashMap<>();
        if (roomIds.isEmpty()) {
            return result;
        }
        jdbc.query(
                "SELECT room_id,active_beds,held_beds,occupied_beds,free_beds FROM"
                        + " v_room_occupancy WHERE room_id = ANY (?)",
                row -> {
                    result.put(
                            row.getLong("room_id"),
                            Map.of(
                                    "active_beds", row.getLong("active_beds"),
                                    "held_beds", row.getLong("held_beds"),
                                    "occupied_beds", row.getLong("occupied_beds"),
                                    "free_beds", row.getLong("free_beds")));
                },
                (Object) roomIds.toArray(new Long[0]));
        return result;
    }

    /** {@code counts} đã nạp theo lô cho danh sách; null thì tự truy vấn (API chi tiết). */
    private Response toRoomResponse(Room r, boolean detail, Map<String, Object> counts) {
        if (counts == null) {
            counts = loadOccupancy(List.of(r.getId())).get(r.getId());
        }
        List<BedResponse> children = detail ? toBedResponses(r.getId()) : null;
        long occupied = ((Number) counts.get("occupied_beds")).longValue();
        return new Response(
                r.getId(),
                r.getCode(),
                r.getFloor(),
                r.getGender(),
                r.getStatus(),
                r.getBuildingId(),
                r.getRoomTypeId(),
                r.getLeaderStudentId(),
                ((Number) counts.get("active_beds")).longValue(),
                ((Number) counts.get("held_beds")).longValue(),
                occupied,
                ((Number) counts.get("free_beds")).longValue(),
                r.getLeaderStudentId() == null && occupied > 0,
                detail ? studentRef(r.getLeaderStudentId()) : null,
                children,
                detail ? leaderHistory(r.getId()) : null);
    }

    /** Lịch sử đổi trưởng phòng từ audit_log, gồm cả lần hệ thống tự gỡ (FR-ROOM-09, 10). */
    private List<LeaderChange> leaderHistory(Long roomId) {
        Map<Long, StudentRef> names = new java.util.HashMap<>();
        return audit
                .listEntityLog(AuditEntityType.ROOM, roomId, AuditAction.ROOM_LEADER_CHANGE)
                .stream()
                .map(
                        e ->
                                new LeaderChange(
                                        cachedRef(names, parseId(e.oldValue())),
                                        cachedRef(names, parseId(e.newValue())),
                                        e.actorId(),
                                        e.actorName(),
                                        e.reason(),
                                        e.at()))
                .toList();
    }

    private StudentRef cachedRef(Map<Long, StudentRef> names, Long id) {
        return id == null ? null : names.computeIfAbsent(id, this::studentRef);
    }

    private StudentRef studentRef(Long id) {
        if (id == null) {
            return null;
        }
        var u = accounts.user(id);
        return new StudentRef(u.id(), u.fullName(), u.username());
    }

    /** Giá trị trước/sau của ROOM_LEADER_CHANGE được lưu là số JSON hoặc null. */
    private static Long parseId(String json) {
        if (json == null || json.isBlank() || "null".equals(json.trim())) {
            return null;
        }
        return Long.valueOf(json.trim());
    }

    /** Giường của phòng cùng phân phòng hiệu lực, nạp bằng một truy vấn cho cả phòng. */
    private List<BedResponse> toBedResponses(Long roomId) {
        var roomBeds =
                beds.findAll((b, q, cb) -> cb.equal(b.get("roomId"), roomId), Sort.by("code"));
        if (roomBeds.isEmpty()) {
            return List.of();
        }
        var bedIds = roomBeds.stream().map(Bed::getId).toList();
        Map<Long, RoomAssignmentHistory> openByBed = new HashMap<>();
        assignments
                .findAll(
                        (r, q, cb) ->
                                cb.and(r.get("bedId").in(bedIds), cb.isNull(r.get("endedAt"))))
                .forEach(a -> openByBed.put(a.getBedId(), a));
        return roomBeds.stream().map(b -> toBedResponse(b, openByBed.get(b.getId()))).toList();
    }

    private BedResponse toBedResponse(Bed b) {
        var a =
                assignments
                        .findAll(
                                (r, q, cb) ->
                                        cb.and(
                                                cb.equal(r.get("bedId"), b.getId()),
                                                cb.isNull(r.get("endedAt"))))
                        .stream()
                        .findFirst()
                        .orElse(null);
        return toBedResponse(b, a);
    }

    private BedResponse toBedResponse(Bed b, RoomAssignmentHistory a) {
        Occupant occupant = null;
        if (a != null) {
            var u = accounts.user(a.getStudentId());
            occupant = new Occupant(u.id(), u.fullName(), u.username(), a.getKind());
        }
        return new BedResponse(b.getId(), b.getCode(), b.getStatus(), occupant);
    }

    private <T> T requireValue(T value, String field) {
        if (value == null) {
            throw BusinessException.invalid(field, "Trường bắt buộc");
        }
        return value;
    }

    private String trimRequiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.invalid(field, "Trường bắt buộc");
        }
        return value.trim();
    }

    @Override()
    public PageResponse<Response> searchRooms(
            int page,
            int size,
            List<String> sort,
            Long buildingId,
            Short floor,
            Long roomTypeId,
            String gender,
            String status,
            Boolean hasFreeBed,
            Boolean noLeader) {
        return searchRooms(
                page,
                size,
                sort,
                buildingId,
                floor,
                roomTypeId,
                gender,
                status,
                hasFreeBed,
                noLeader,
                "");
    }
}

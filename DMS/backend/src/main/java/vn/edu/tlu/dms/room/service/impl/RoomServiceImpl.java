package vn.edu.tlu.dms.room.service.impl;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.room.api.RoomDtos.BedInput;
import vn.edu.tlu.dms.room.api.RoomDtos.BedResponse;
import vn.edu.tlu.dms.room.api.RoomDtos.BedStatus;
import vn.edu.tlu.dms.room.api.RoomDtos.Input;
import vn.edu.tlu.dms.room.api.RoomDtos.Occupant;
import vn.edu.tlu.dms.room.api.RoomDtos.Response;
import vn.edu.tlu.dms.room.api.RoomDtos.Status;
import vn.edu.tlu.dms.room.domain.Bed;
import vn.edu.tlu.dms.room.domain.Building;
import vn.edu.tlu.dms.room.domain.Room;
import vn.edu.tlu.dms.room.domain.RoomAssignmentHistory;
import vn.edu.tlu.dms.room.repository.BedRepository;
import vn.edu.tlu.dms.room.repository.RoomAssignmentHistoryRepository;
import vn.edu.tlu.dms.room.repository.RoomRepository;
import vn.edu.tlu.dms.room.repository.RoomTypeRepository;
import vn.edu.tlu.dms.room.service.RoomReferenceService;
import vn.edu.tlu.dms.room.service.RoomService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
            AuditService audit) {
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
        guard.requireRole("STAFF");
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
                                    actor.crossUnitAccess()
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
                                        b.equal(bed.get("status"), "ACTIVE"),
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
        guard.requireRole("STAFF");
        var r = findAccessibleRoom(id, false);
        return toRoomResponse(r, true, null);
    }

    @Override
    @Transactional
    public Response createRoom(Input input) {
        guard.requireRole("STAFF");
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
        r.setStatus("ACTIVE");
        rooms.saveAndFlush(r);
        syncBeds(r, type.getBedCount());
        audit.record(current.get().id(), "ROOM_CREATE", "ROOM", r.getId(), null, input, null);
        return toRoomResponse(r, true, null);
    }

    @Override
    @Transactional
    public Response updateRoom(Long id, Input input) {
        guard.requireRole("STAFF");
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
        audit.record(current.get().id(), "ROOM_UPDATE", "ROOM", id, null, input, null);
        return toRoomResponse(r, true, null);
    }

    @Override
    @Transactional
    public Response changeRoomStatus(Long id, Status input) {
        guard.requireRole("STAFF");
        lockRoomBedsInAscendingOrder(id);
        var r = findAccessibleRoom(id, true);
        if (!input.status().equals("ACTIVE")) {
            requireRoomWithoutOpenAssignments(r);
        }
        String before = r.getStatus();
        r.setStatus(input.status());
        rooms.saveAndFlush(r);
        audit.record(
                current.get().id(),
                "ROOM_STATUS",
                "ROOM",
                id,
                before,
                input.status(),
                trimRequiredText(input.reason(), "reason"));
        return toRoomResponse(r, true, null);
    }

    @Override
    @Transactional
    public BedResponse addBedToRoom(Long roomId, BedInput input) {
        guard.requireRole("STAFF");
        var r = findAccessibleRoom(roomId, true);
        var bed = new Bed();
        bed.setRoomId(r.getId());
        bed.setCode(trimRequiredText(input.code(), "code"));
        bed.setStatus("ACTIVE");
        beds.saveAndFlush(bed);
        return toBedResponse(bed);
    }

    @Override
    @Transactional
    public BedResponse updateBedCode(Long id, BedInput input) {
        guard.requireRole("STAFF");
        var bed = lockAndFindBed(id);
        findAccessibleRoom(bed.getRoomId(), true);
        String previous = bed.getCode();
        bed.setCode(trimRequiredText(input.code(), "code"));
        beds.saveAndFlush(bed);
        audit.record(current.get().id(), "BED_UPDATE", "BED", id, previous, bed.getCode(), null);
        return toBedResponse(bed);
    }

    @Override
    @Transactional
    public BedResponse changeBedStatus(Long id, BedStatus input) {
        guard.requireRole("STAFF");
        var bed = lockAndFindBed(id);
        findAccessibleRoom(bed.getRoomId(), true);
        if (!input.status().equals("ACTIVE") && hasOpenBedAssignment(id)) {
            throw BusinessException.conflict("BED_OCCUPIED");
        }
        String before = bed.getStatus();
        bed.setStatus(input.status());
        beds.saveAndFlush(bed);
        audit.record(
                current.get().id(),
                "BED_STATUS",
                "BED",
                id,
                before,
                input.status(),
                trimRequiredText(input.reason(), "reason"));
        return toBedResponse(bed);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        guard.requireRole("STAFF");
        lockRoomBedsInAscendingOrder(id);
        var r = findAccessibleRoom(id, true);
        if (jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM room_assignment_history a JOIN bed b ON b.id=a.bed_id"
                        + " WHERE b.room_id=?)",
                Boolean.class,
                id)) {
            throw BusinessException.conflict("ROOM_OCCUPIED");
        }
        var rows = beds.findAll((b, q, cb) -> cb.equal(b.get("roomId"), id));
        beds.deleteAll(rows);
        beds.flush();
        rooms.delete(r);
        rooms.flush();
        audit.record(current.get().id(), "ROOM_DELETE", "ROOM", id, r.getCode(), null, null);
    }

    @Override
    @Transactional
    public void deleteBed(Long id) {
        guard.requireRole("STAFF");
        var bed = lockAndFindBed(id);
        findAccessibleRoom(bed.getRoomId(), true);
        if (assignments.exists((r, q, b) -> b.equal(r.get("bedId"), id))) {
            throw BusinessException.conflict("BED_OCCUPIED");
        }
        beds.delete(bed);
        beds.flush();
        audit.record(current.get().id(), "BED_DELETE", "BED", id, bed.getCode(), null, null);
    }

    @Override
    @Transactional
    public Response assignRoomLeader(Long id, Long studentId) {
        guard.requireRole("STAFF");
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
            throw BusinessException.conflict("LEADER_NOT_STAYING");
        }
        Long before = r.getLeaderStudentId();
        r.setLeaderStudentId(studentId);
        rooms.saveAndFlush(r);
        audit.record(current.get().id(), "ROOM_LEADER_CHANGE", "ROOM", id, before, studentId, null);
        return toRoomResponse(r, true, null);
    }

    @Override
    @Transactional
    public void removeRoomLeader(Long id, String reason) {
        guard.requireRole("STAFF");
        lockRoomBedsInAscendingOrder(id);
        var r = findAccessibleRoom(id, true);
        Long before = r.getLeaderStudentId();
        r.setLeaderStudentId(null);
        rooms.saveAndFlush(r);
        audit.record(
                current.get().id(),
                "ROOM_LEADER_CHANGE",
                "ROOM",
                id,
                before,
                null,
                trimRequiredText(reason, "reason"));
    }

    /**
     * Đưa số giường của phòng về đúng sức chứa của loại phòng. Thêm giường mang mã hai chữ số còn
     * thiếu; bớt giường từ mã lớn nhất và chỉ bớt giường chưa từng có phân phòng (lịch sử không
     * bị xóa). Gọi sau khi đã khóa các giường của phòng.
     */
    private void syncBeds(Room r, int target) {
        var existing = new ArrayList<>(beds.findAll((b, q, cb) -> cb.equal(b.get("roomId"), r.getId())));
        existing.sort(java.util.Comparator.comparing(Bed::getCode));
        Set<String> used = new java.util.HashSet<>();
        existing.forEach(b -> used.add(b.getCode()));
        for (int number = 1; existing.size() < target; number++) {
            String code = String.format("%02d", number);
            if (used.add(code)) {
                var bed = new Bed();
                bed.setRoomId(r.getId());
                bed.setCode(code);
                bed.setStatus("ACTIVE");
                existing.add(beds.save(bed));
            }
        }
        for (int i = existing.size() - 1; i >= 0 && existing.size() > target; i--) {
            var bed = existing.get(i);
            Long bedId = bed.getId();
            if (!assignments.exists((a, q, cb) -> cb.equal(a.get("bedId"), bedId))) {
                beds.delete(bed);
                existing.remove(i);
            }
        }
        beds.flush();
        if (existing.size() > target) {
            throw BusinessException.conflict("BED_OCCUPIED");
        }
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
                    "ROOM_OCCUPIED", 409, Map.of("roomCode", r.getCode(), "count", count));
        }
    }

    private Map<Long, Map<String, Object>> loadOccupancy(List<Long> roomIds) {
        Map<Long, Map<String, Object>> result = new java.util.HashMap<>();
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
                ((Number) counts.get("occupied_beds")).longValue(),
                ((Number) counts.get("free_beds")).longValue(),
                children);
    }

    /** Giường của phòng cùng phân phòng hiệu lực, nạp bằng một truy vấn cho cả phòng. */
    private List<BedResponse> toBedResponses(Long roomId) {
        var roomBeds =
                beds.findAll((b, q, cb) -> cb.equal(b.get("roomId"), roomId), Sort.by("code"));
        if (roomBeds.isEmpty()) {
            return List.of();
        }
        var bedIds = roomBeds.stream().map(Bed::getId).toList();
        Map<Long, RoomAssignmentHistory> openByBed = new java.util.HashMap<>();
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

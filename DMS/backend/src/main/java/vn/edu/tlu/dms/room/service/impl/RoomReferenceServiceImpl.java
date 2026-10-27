package vn.edu.tlu.dms.room.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.BedOperationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.RoomStatus;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.room.repository.BuildingRepository;
import vn.edu.tlu.dms.room.repository.RoomRepository;
import vn.edu.tlu.dms.room.repository.RoomTypeRepository;
import vn.edu.tlu.dms.room.repository.SemesterRepository;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RoomReferenceServiceImpl implements RoomReferenceService {

    private final RoomRepository rooms;

    private final BuildingRepository buildings;

    private final RoomTypeRepository types;

    private final SemesterRepository semesters;

    private final JdbcTemplate jdbc;

    public RoomReferenceServiceImpl(
            RoomRepository rooms,
            BuildingRepository buildings,
            RoomTypeRepository types,
            SemesterRepository semesters,
            JdbcTemplate jdbc) {
        this.rooms = rooms;
        this.buildings = buildings;
        this.types = types;
        this.semesters = semesters;
        this.jdbc = jdbc;
    }

    @Override()
    public Location getRoomLocation(Long id) {
        var r = rooms.findById(id).orElseThrow(BusinessException::notFound);
        return new Location(
                id,
                r.getBuildingId(),
                getBuildingManagementUnitId(r.getBuildingId()),
                r.getGender(),
                r.getStatus());
    }

    @Override()
    public Long getBuildingManagementUnitId(Long id) {
        return buildings
                .findById(id)
                .orElseThrow(BusinessException::notFound)
                .getManagementUnitId();
    }

    @Override()
    public void requireActiveRoomType(Long id) {
        if (id == null
                || !types.exists(
                        (r, q, b) -> b.and(b.equal(r.get("id"), id), b.isTrue(r.get("active"))))) {
            throw BusinessException.notFound();
        }
    }

    @Override()
    public SemesterRef getSemesterReference(Long id) {
        var r = semesters.findById(id).orElseThrow(BusinessException::notFound);
        return new SemesterRef(r.getId(), r.getType(), r.getStartDate(), r.getEndDate());
    }

    @Override()
    public Optional<Long> findCurrentStayingRoomId(Long studentId) {
        return jdbc
                .query(
                        "SELECT room_id FROM v_current_stay WHERE student_id=? AND"
                                + " assignment_kind='STAY'",
                        (r, i) -> r.getLong(1),
                        studentId)
                .stream()
                .findFirst();
    }

    @Override()
    public boolean hasPendingOrActiveContract(Long studentId) {
        // Hợp đồng còn hiệu lực có thể tạm không có phân phòng khi sinh viên gửi đồ kỳ hè.
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM dormitory_contract WHERE student_id=? AND"
                                + " status IN ('PENDING','ACTIVE'))",
                        Boolean.class,
                        studentId));
    }

    @Override()
    public void lockStudentProfile(Long studentId) {
        var rows =
                jdbc.query(
                        "SELECT user_id FROM student_profile WHERE user_id=? FOR UPDATE",
                        (r, i) -> r.getLong(1),
                        studentId);
        if (rows.isEmpty()) {
            throw BusinessException.notFound();
        }
    }

    @Override
    public DashboardInfrastructure getDashboardInfrastructure(Long managementUnitId) {
        Long buildingCount =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM building WHERE (CAST(? AS bigint) IS NULL OR"
                            + " management_unit_id=?)",
                        Long.class,
                        managementUnitId,
                        managementUnitId);
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.query(
                """
                SELECT r.status,COUNT(*) AS cnt FROM room r
                JOIN building bd ON bd.id=r.building_id
                WHERE (CAST(? AS bigint) IS NULL OR bd.management_unit_id=?)
                GROUP BY r.status ORDER BY r.status
                """,
                rs -> {
                    counts.put(rs.getString("status"), rs.getLong("cnt"));
                },
                managementUnitId,
                managementUnitId);
        return new DashboardInfrastructure(
                buildingCount == null ? 0 : buildingCount, Map.copyOf(counts));
    }

    /** Tính sức chứa và lấp đầy trên cùng phạm vi giường hoạt động, tách giường bảo trì. */
    @Override
    public DashboardCapacity getDashboardCapacity(Long managementUnitId) {
        String unitName =
                managementUnitId == null
                        ? null
                        : jdbc
                                .query(
                                        "SELECT name FROM management_unit WHERE id=?",
                                        (rs, i) -> rs.getString(1),
                                        managementUnitId)
                                .stream()
                                .findFirst()
                                .orElseThrow(BusinessException::notFound);
        return jdbc.queryForObject(
                """
                SELECT COUNT(*) FILTER (WHERE b.status=? AND r.status=?) AS operational,
                       COUNT(*) FILTER (WHERE b.status=? AND r.status=? AND EXISTS (
                           SELECT 1 FROM room_assignment_history h WHERE h.bed_id=b.id
                               AND h.ended_at IS NULL AND h.kind=?)) AS occupied,
                       COUNT(*) FILTER (WHERE b.status=? AND r.status=? AND NOT EXISTS (
                           SELECT 1 FROM room_assignment_history h WHERE h.bed_id=b.id
                               AND h.ended_at IS NULL)) AS available,
                       COUNT(*) FILTER (WHERE r.status=?) AS maintenance,
                       COUNT(DISTINCT r.id) FILTER (WHERE r.status=? AND r.leader_student_id IS NULL
                           AND EXISTS (SELECT 1 FROM room_assignment_history h JOIN bed hb ON hb.id=h.bed_id
                               WHERE hb.room_id=r.id AND h.ended_at IS NULL AND h.kind=?)) AS missing_leader
                FROM bed b JOIN room r ON r.id=b.room_id JOIN building bd ON bd.id=r.building_id
                WHERE (CAST(? AS bigint) IS NULL OR bd.management_unit_id=?)
                """,
                (rs, i) ->
                        new DashboardCapacity(
                                unitName,
                                rs.getLong("operational"),
                                rs.getLong("occupied"),
                                rs.getLong("available"),
                                rs.getLong("maintenance"),
                                rs.getLong("missing_leader")),
                BedOperationStatus.ACTIVE,
                RoomStatus.ACTIVE,
                BedOperationStatus.ACTIVE,
                RoomStatus.ACTIVE,
                AssignmentKind.STAY,
                BedOperationStatus.ACTIVE,
                RoomStatus.ACTIVE,
                RoomStatus.MAINTENANCE,
                RoomStatus.ACTIVE,
                AssignmentKind.STAY,
                managementUnitId,
                managementUnitId);
    }
}

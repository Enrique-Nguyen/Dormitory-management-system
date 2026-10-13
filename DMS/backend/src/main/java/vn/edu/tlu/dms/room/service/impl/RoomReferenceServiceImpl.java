package vn.edu.tlu.dms.room.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.room.repository.BuildingRepository;
import vn.edu.tlu.dms.room.repository.RoomRepository;
import vn.edu.tlu.dms.room.repository.RoomTypeRepository;
import vn.edu.tlu.dms.room.repository.SemesterRepository;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

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
}

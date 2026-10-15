package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentEndReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.room.api.RoomDtos.LeaderChange;
import vn.edu.tlu.dms.room.api.RoomDtos.Response;
import vn.edu.tlu.dms.room.api.RoomDtos.StudentRef;
import vn.edu.tlu.dms.room.service.BedOccupancyService;
import vn.edu.tlu.dms.room.service.RoomService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Trưởng phòng trên PostgreSQL thật với AuditService thật (FR-ROOM-09, 10, BR-11, AT-21): điều
 * kiện chỉ định, một sinh viên chỉ làm trưởng một phòng, cảnh báo thiếu trưởng phòng và lịch sử
 * đổi trưởng phòng đọc từ audit_log. Mọi case chạy trong transaction được rollback.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RoomLeaderIntegrationTest {

    @Autowired RoomService rooms;

    @Autowired BedOccupancyService occupancy;

    @Autowired JdbcTemplate jdbc;

    @MockitoBean CurrentUser currentUser;

    private String suffix;

    private Long staffId;

    private Long periodId;

    private Long roomA;

    private Long roomB;

    private final List<Long> beds = new ArrayList<>();

    private int studentCount;

    @BeforeEach
    void createFixtureData() {
        suffix = UUID.randomUUID().toString().substring(0, 6);
        Long unitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code = 'K1'", Long.class);
        staffId = createUser(Role.STAFF, unitId, "Cán bộ " + suffix);
        when(currentUser.get()).thenReturn(new Actor(staffId, Role.STAFF, unitId, false, false));
        Long typeId =
                jdbc.queryForObject(
                        "INSERT INTO room_type(name,bed_count,price_main,price_summer)"
                                + " VALUES (?,2,1500000,700000) RETURNING id",
                        Long.class,
                        "Loại " + suffix);
        Long buildingId =
                jdbc.queryForObject(
                        "INSERT INTO building(management_unit_id,code,name,deposit_amount)"
                                + " VALUES(?,?,?,1000000) RETURNING id",
                        Long.class,
                        unitId,
                        "L" + suffix,
                        "Tòa " + suffix);
        roomA = createRoom(buildingId, typeId, "L101");
        roomB = createRoom(buildingId, typeId, "L102");
        for (Long room : List.of(roomA, roomA, roomB)) {
            beds.add(
                    jdbc.queryForObject(
                            "INSERT INTO bed(room_id,code) VALUES(?,?) RETURNING id",
                            Long.class,
                            room,
                            "0" + (beds.size() + 1)));
        }
        Long semesterId =
                jdbc.queryForObject(
                        "INSERT INTO semester(academic_year,type,name,start_date,end_date)"
                                + " VALUES('2060-2061','HK1','HK1','2060-09-01','2061-01-31')"
                                + " RETURNING id",
                        Long.class);
        periodId =
                jdbc.queryForObject(
                        "INSERT INTO registration_period(semester_id,kind,name,open_at,close_at,"
                                + "created_by) VALUES(?,'REGULAR','Đợt',now(),now()+interval '1"
                                + " day',?) RETURNING id",
                        Long.class,
                        semesterId,
                        staffId);
    }

    @Test
    void leaderMustStayInRoomAndLeadOnlyOneRoom() {
        Long holder = createStudent();
        occupancy.occupyHold(beds.get(0), holder, createContract(holder), null);
        Long first = createStudent();
        occupancy.occupyStay(beds.get(1), first, createContract(first), null);
        Long second = createStudent();
        occupancy.occupyStay(beds.get(2), second, createContract(second), null);

        assertConflict(() -> rooms.assignRoomLeader(roomA, holder), ErrorCode.LEADER_NOT_STAYING);
        assertConflict(() -> rooms.assignRoomLeader(roomA, second), ErrorCode.LEADER_NOT_STAYING);
        assertThat(rooms.getRoomById(roomA).missingLeader()).isTrue();

        // Dữ liệu lệch (sinh viên còn ghi là trưởng phòng B) bị chặn với mã lỗi và phòng cụ thể.
        jdbc.update("UPDATE room SET leader_student_id=? WHERE id=?", first, roomB);
        assertThatThrownBy(() -> rooms.assignRoomLeader(roomA, first))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode())
                                    .isEqualTo(ErrorCode.LEADER_ALREADY_ASSIGNED);
                            assertThat(e.getDetails()).containsEntry("roomCode", "L102");
                        });
        jdbc.update("UPDATE room SET leader_student_id=NULL WHERE id=?", roomB);

        Response assigned = rooms.assignRoomLeader(roomA, first);
        assertThat(assigned.leader().id()).isEqualTo(first);
        assertThat(assigned.missingLeader()).isFalse();
        // Chỉ định lại đúng người đang là trưởng phòng không ghi thêm lịch sử.
        rooms.assignRoomLeader(roomA, first);
        assertThat(rooms.getRoomById(roomA).leaderHistory()).hasSize(1);
    }

    @Test
    void leaderHistoryIncludesManualAndAutomaticRemoval() {
        Long first = createStudent();
        Long firstContract = createContract(first);
        occupancy.occupyStay(beds.get(0), first, firstContract, null);
        Long second = createStudent();
        occupancy.occupyStay(beds.get(1), second, createContract(second), null);

        rooms.assignRoomLeader(roomA, first);
        occupancy.release(firstContract, AssignmentEndReason.MOVE_OUT, staffId, null);
        // AT-21: phòng còn người ở mà mất trưởng phòng thì hiện cảnh báo.
        Response afterMoveOut = rooms.getRoomById(roomA);
        assertThat(afterMoveOut.leaderStudentId()).isNull();
        assertThat(afterMoveOut.missingLeader()).isTrue();

        rooms.assignRoomLeader(roomA, second);
        assertThatThrownBy(() -> rooms.removeRoomLeader(roomA, " "))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        rooms.removeRoomLeader(roomA, "Sinh viên xin thôi");
        rooms.removeRoomLeader(roomA, "Gỡ lặp");

        List<LeaderChange> history = rooms.getRoomById(roomA).leaderHistory();
        assertThat(history)
                .extracting(h -> id(h.from()) + ">" + id(h.to()) + ":" + h.reason())
                .containsExactly(
                        "null>" + first + ":null",
                        first + ">null:" + AuditReason.LEADER_AUTO_REMOVED,
                        "null>" + second + ":null",
                        second + ">null:Sinh viên xin thôi");
        assertThat(history.get(0).actorName()).isEqualTo("Cán bộ " + suffix);
        assertThat(history.get(0).to().studentCode()).startsWith("SV" + suffix);
    }

    private static Long id(StudentRef ref) {
        return ref == null ? null : ref.id();
    }

    private void assertConflict(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }

    private Long createStudent() {
        String code = "SV" + suffix + studentCount++;
        Long id =
                jdbc.queryForObject(
                        "INSERT INTO app_user(username,role,full_name,must_change_password,"
                                + "keycloak_user_id) VALUES(?,?,?,false,?) RETURNING id",
                        Long.class,
                        code,
                        Role.STUDENT,
                        "Sinh viên " + code,
                        UUID.randomUUID().toString());
        jdbc.update(
                "INSERT INTO student_profile(user_id,student_code,gender) VALUES (?,?,'M')",
                id,
                code);
        return id;
    }

    private Long createContract(Long student) {
        Long application =
                jdbc.queryForObject(
                        "INSERT INTO room_application(student_id,period_id,preferred_unit_id,"
                                + "status) SELECT ?,?,id,'ASSIGNED' FROM management_unit WHERE"
                                + " code='K1' RETURNING id",
                        Long.class,
                        student,
                        periodId);
        return jdbc.queryForObject(
                "INSERT INTO dormitory_contract(code,student_id,application_id,start_date,"
                        + "end_date,status,activation_deadline,deposit_required,activated_at)"
                        + " VALUES(?,?,?,'2060-09-01','2061-06-30','ACTIVE',now()+interval"
                        + " '7 days',1000000,now()) RETURNING id",
                Long.class,
                "HD" + suffix + application,
                student,
                application);
    }

    private Long createUser(String role, Long unitId, String name) {
        return jdbc.queryForObject(
                "INSERT INTO app_user(username,role,full_name,management_unit_id,"
                        + "must_change_password,keycloak_user_id) VALUES(?,?,?,?,false,?)"
                        + " RETURNING id",
                Long.class,
                role + "-" + UUID.randomUUID(),
                role,
                name,
                unitId,
                UUID.randomUUID().toString());
    }

    private Long createRoom(Long buildingId, Long typeId, String code) {
        return jdbc.queryForObject(
                "INSERT INTO room(building_id,room_type_id,floor,code,gender)"
                        + " VALUES(?,?,1,?,'M') RETURNING id",
                Long.class,
                buildingId,
                typeId,
                code);
    }
}

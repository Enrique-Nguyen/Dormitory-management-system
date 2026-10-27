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

import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentEndReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Response;
import vn.edu.tlu.dms.registration.service.RoomApplicationService;
import vn.edu.tlu.dms.room.service.BedOccupancyService;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Kiểm thử tích hợp phân giường, hủy giữ chỗ, hủy đơn ASSIGNED và xác nhận diện ưu tiên trên
 * PostgreSQL thật (FR-REG-06, FR-REG-08..11, API 4.3/4.4): tạo HOLD + hợp đồng PENDING trong một
 * transaction, chống phân lặp/giường đã có người, trả đơn về APPROVED, chặn hủy khi hợp đồng đã
 * ACTIVE, tính lại điểm ưu tiên, phạm vi đơn vị và nhật ký thao tác. Mọi case chạy trong
 * transaction được rollback.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ApplicationAssignmentIntegrationTest {

    private static final LocalDate SEMESTER_START = LocalDate.of(2070, 9, 1);

    private static final LocalDate SEMESTER_END = LocalDate.of(2071, 1, 31);

    @Autowired RoomApplicationService applications;

    @Autowired BedOccupancyService occupancy;

    @Autowired JdbcTemplate jdbc;

    @MockitoBean CurrentUser currentUser;

    private String suffix;

    private Long unitId;

    private Long staffId;

    private Long studentId;

    private Long semesterId;

    private Long periodId;

    private Long applicationId;

    private Long roomId;

    private Long firstBedId;

    private Long secondBedId;

    private Long categoryApprovedId;

    private Long categoryRejectedId;

    private Long categoryPendingId;

    @BeforeEach
    void createFixtureData() {
        suffix = UUID.randomUUID().toString().substring(0, 6);
        unitId = jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        staffId = createUser(Role.STAFF, unitId, "Cán bộ " + suffix);
        when(currentUser.get()).thenReturn(new Actor(staffId, Role.STAFF, unitId, false, false));
        studentId = createStudent("SV" + suffix);
        semesterId =
                jdbc.queryForObject(
                        "INSERT INTO semester(academic_year,type,name,start_date,end_date)"
                                + " VALUES('2070-2071','HK1','HK1 2070-2071',?,?) RETURNING id",
                        Long.class,
                        SEMESTER_START,
                        SEMESTER_END);
        periodId =
                jdbc.queryForObject(
                        "INSERT INTO registration_period(semester_id,kind,name,open_at,close_at,"
                                + " created_by) VALUES(?,'REGULAR','Đợt',now()-interval '1 hour',"
                                + " now()+interval '1 day',?) RETURNING id",
                        Long.class,
                        semesterId,
                        staffId);
        applicationId =
                jdbc.queryForObject(
                        "INSERT INTO room_application(student_id,period_id,preferred_unit_id,"
                                + "status,submitted_at) VALUES(?,?,?,'APPROVED',now()) RETURNING id",
                        Long.class,
                        studentId,
                        periodId,
                        unitId);
        categoryApprovedId = createPriorityCategory("A", 30);
        categoryRejectedId = createPriorityCategory("B", 20);
        categoryPendingId = createPriorityCategory("C", 10);
        addPriority(categoryApprovedId);
        addPriority(categoryRejectedId);
        addPriority(categoryPendingId);

        Long roomTypeId =
                jdbc.queryForObject(
                        "INSERT INTO room_type(name,bed_count,price_main,price_summer)"
                                + " VALUES(?,3,1500000,700000) RETURNING id",
                        Long.class,
                        "Loại " + suffix);
        Long buildingId =
                jdbc.queryForObject(
                        "INSERT INTO building(management_unit_id,code,name,deposit_amount)"
                                + " VALUES(?,?,?,1000000) RETURNING id",
                        Long.class,
                        unitId,
                        "B" + suffix,
                        "Tòa " + suffix);
        roomId =
                jdbc.queryForObject(
                        "INSERT INTO room(building_id,room_type_id,floor,code,gender)"
                                + " VALUES(?,?,1,?,'M') RETURNING id",
                        Long.class,
                        buildingId,
                        roomTypeId,
                        "P" + suffix);
        firstBedId = createBed(roomId, "01");
        secondBedId = createBed(roomId, "02");
    }

    /** Phân giường thành công: HOLD + hợp đồng PENDING theo học kỳ, đơn ASSIGNED, lịch sử/audit. */
    @Test
    void assignCreatesHoldContractAndMarksApplicationAssigned() {
        Response result = applications.assignBed(applicationId, firstBedId);

        assertThat(result.status()).isEqualTo(ApplicationStatus.ASSIGNED);
        assertThat(result.contractId()).isNotNull();
        assertThat(result.hold()).isNotNull();
        assertThat(result.hold().bedId()).isEqualTo(firstBedId);
        assertThat(result.hold().roomId()).isEqualTo(roomId);
        assertThat(result.hold().contractId()).isEqualTo(result.contractId());
        assertThat(result.hold().activationDeadline()).isNotNull();
        assertThat(result.allowedActions()).contains(BusinessAction.RELEASE_HOLD);

        var contract =
                jdbc.queryForMap(
                        "SELECT student_id,application_id,status,start_date,end_date,"
                                + "activation_deadline FROM dormitory_contract WHERE id=?",
                        result.contractId());
        assertThat(contract.get("student_id")).isEqualTo(studentId);
        assertThat(contract.get("application_id")).isEqualTo(applicationId);
        assertThat(contract.get("status")).isEqualTo(ContractStatus.PENDING);
        assertThat(((java.sql.Date) contract.get("start_date")).toLocalDate())
                .isEqualTo(SEMESTER_START);
        assertThat(((java.sql.Date) contract.get("end_date")).toLocalDate())
                .isEqualTo(SEMESTER_END);
        assertThat(contract.get("activation_deadline")).isNotNull();

        assertThat(
                        jdbc.queryForObject(
                                "SELECT kind FROM room_assignment_history WHERE contract_id=?",
                                String.class,
                                result.contractId()))
                .isEqualTo(AssignmentKind.HOLD);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT ended_at IS NULL FROM room_assignment_history"
                                        + " WHERE contract_id=?",
                                Boolean.class,
                                result.contractId()))
                .isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM room_application WHERE id=?",
                                String.class,
                                applicationId))
                .isEqualTo(ApplicationStatus.ASSIGNED);
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM status_history WHERE entity_type=?"
                                        + " AND entity_id=? AND from_status=? AND to_status=?",
                                EntityType.APPLICATION,
                                applicationId,
                                ApplicationStatus.APPROVED,
                                ApplicationStatus.ASSIGNED))
                .isEqualTo(1);
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM audit_log WHERE action=? AND entity_id=?",
                                AuditAction.APPLICATION_ASSIGN,
                                applicationId))
                .isEqualTo(1);
    }

    /** Gửi phân giường lần hai: đơn đã ASSIGNED nên báo sai trạng thái, không tạo phân phòng mới. */
    @Test
    void assignTwiceIsRejected() {
        applications.assignBed(applicationId, firstBedId);

        assertThatThrownBy(() -> applications.assignBed(applicationId, secondBedId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM dormitory_contract WHERE application_id=?",
                                applicationId))
                .isEqualTo(1);
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM room_assignment_history WHERE student_id=?"
                                        + " AND ended_at IS NULL",
                                studentId))
                .isEqualTo(1);
    }

    /** Giường đã có người giữ/ở: module room báo BED_TAKEN, toàn bộ transaction rollback. */
    @Test
    void assignTakenBedReportsBedTaken() {
        Long otherStudentId = createStudent("SVX" + suffix);
        Long otherApplicationId =
                jdbc.queryForObject(
                        "INSERT INTO room_application(student_id,period_id,preferred_unit_id,"
                                + "status) VALUES(?,?,?,'APPROVED') RETURNING id",
                        Long.class,
                        otherStudentId,
                        periodId,
                        unitId);
        Long otherContractId =
                jdbc.queryForObject(
                        "INSERT INTO dormitory_contract(code,student_id,application_id,start_date,"
                                + "end_date,status,activation_deadline)"
                                + " VALUES(?,?,?,?,?,'PENDING',now()+interval '7 days') RETURNING id",
                        Long.class,
                        "HD" + suffix + "-X",
                        otherStudentId,
                        otherApplicationId,
                        SEMESTER_START,
                        SEMESTER_END);
        occupancy.occupyHold(secondBedId, otherStudentId, otherContractId, null);

        assertThatThrownBy(() -> applications.assignBed(applicationId, secondBedId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BED_TAKEN));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM room_application WHERE id=?",
                                String.class,
                                applicationId))
                .isEqualTo(ApplicationStatus.APPROVED);
    }

    /** Hủy giữ chỗ: đóng HOLD, hủy hợp đồng PENDING, đơn về APPROVED và phân lại được. */
    @Test
    void releaseHoldReturnsApplicationToApprovedAndCancelsPendingContract() {
        Response assigned = applications.assignBed(applicationId, firstBedId);

        Response released =
                applications.releaseHold(applicationId, "Đổi ý, cần phân lại giường");

        assertThat(released.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(released.contractId()).isNull();
        assertThat(released.hold()).isNull();
        assertThat(released.allowedActions()).contains(BusinessAction.ASSIGN);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                assigned.contractId()))
                .isEqualTo(ContractStatus.CANCELLED);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT end_reason FROM room_assignment_history"
                                        + " WHERE contract_id=?",
                                String.class,
                                assigned.contractId()))
                .isEqualTo(AssignmentEndReason.HOLD_CANCELLED);
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM audit_log WHERE action=? AND entity_id=?",
                                AuditAction.APPLICATION_RELEASE_HOLD,
                                applicationId))
                .isEqualTo(1);

        Response reassigned = applications.assignBed(applicationId, secondBedId);
        assertThat(reassigned.status()).isEqualTo(ApplicationStatus.ASSIGNED);
        assertThat(reassigned.contractId()).isNotEqualTo(assigned.contractId());
        assertThat(reassigned.hold().bedId()).isEqualTo(secondBedId);
    }

    /** Hủy đơn ASSIGNED: đóng HOLD + hủy hợp đồng PENDING + đơn CANCELLED trong một transaction. */
    @Test
    void cancelAssignedApplicationReleasesHoldAndCancelsContract() {
        Response assigned = applications.assignBed(applicationId, firstBedId);
        when(currentUser.get()).thenReturn(new Actor(studentId, Role.STUDENT, null, false, false));

        Response cancelled =
                applications.performApplicationAction(
                        applicationId, BusinessAction.CANCEL, null);

        assertThat(cancelled.status()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                assigned.contractId()))
                .isEqualTo(ContractStatus.CANCELLED);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT end_reason FROM room_assignment_history"
                                        + " WHERE contract_id=?",
                                String.class,
                                assigned.contractId()))
                .isEqualTo(AssignmentEndReason.HOLD_CANCELLED);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT ended_at IS NOT NULL FROM room_assignment_history"
                                        + " WHERE contract_id=?",
                                Boolean.class,
                                assigned.contractId()))
                .isTrue();
    }

    /** Hợp đồng đã ACTIVE: không cho hủy đơn hoặc giải phóng giữ chỗ để bảo toàn chỗ ở. */
    @Test
    void cancelAndReleaseAreBlockedWhenContractActive() {
        Response assigned = applications.assignBed(applicationId, firstBedId);
        jdbc.update(
                "UPDATE dormitory_contract SET status=?,deposit_required=1000000,"
                        + "activated_at=now() WHERE id=?",
                ContractStatus.ACTIVE,
                assigned.contractId());
        occupancy.convertHoldToStay(assigned.contractId(), staffId);

        when(currentUser.get()).thenReturn(new Actor(studentId, Role.STUDENT, null, false, false));
        assertThatThrownBy(
                        () ->
                                applications.performApplicationAction(
                                        applicationId, BusinessAction.CANCEL, null))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.APPLICATION_HAS_CONTRACT));

        when(currentUser.get()).thenReturn(new Actor(staffId, Role.STAFF, unitId, false, false));
        assertThatThrownBy(() -> applications.releaseHold(applicationId, "Thử hủy giữ chỗ"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.APPLICATION_HAS_CONTRACT));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT ended_at IS NULL FROM room_assignment_history"
                                        + " WHERE contract_id=?",
                                Boolean.class,
                                assigned.contractId()))
                .isTrue();
    }

    /** Xác nhận đúng/không đúng từng diện ưu tiên, tính lại điểm và ghi nhật ký. */
    @Test
    void verifyPriorityRecalculatesScoreAndRecordsAudit() {
        jdbc.update(
                "UPDATE room_application SET status=? WHERE id=?",
                ApplicationStatus.SUBMITTED,
                applicationId);

        Response first =
                applications.verifyPriority(
                        applicationId, categoryApprovedId, true, "Đã kiểm tra giấy tờ");
        assertThat(first.priorityScore()).isEqualTo(60);

        Response second =
                applications.verifyPriority(applicationId, categoryRejectedId, false, null);
        assertThat(second.priorityScore()).isEqualTo(40);
        assertThat(second.priorities())
                .anySatisfy(
                        priority -> {
                            if (priority.categoryId().equals(categoryRejectedId)) {
                                assertThat(priority.verified()).isFalse();
                                assertThat(priority.verifiedBy()).isEqualTo(staffId);
                                assertThat(priority.verifiedByName()).isEqualTo("Cán bộ " + suffix);
                                assertThat(priority.verifiedAt()).isNotNull();
                            }
                        });

        var row =
                jdbc.queryForMap(
                        "SELECT verified,verified_by,verified_at FROM application_priority"
                                + " WHERE application_id=? AND category_id=?",
                        applicationId,
                        categoryRejectedId);
        assertThat(row.get("verified")).isEqualTo(false);
        assertThat(((Number) row.get("verified_by")).longValue()).isEqualTo(staffId);
        assertThat(row.get("verified_at")).isNotNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT priority_score FROM room_application WHERE id=?",
                                Integer.class,
                                applicationId))
                .isEqualTo(40);
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM audit_log WHERE action=? AND entity_id=?",
                                AuditAction.APPLICATION_PRIORITY_VERIFY,
                                applicationId))
                .isEqualTo(2);

        jdbc.update(
                "UPDATE room_application SET status=? WHERE id=?",
                ApplicationStatus.APPROVED,
                applicationId);
        assertThatThrownBy(
                        () ->
                                applications.verifyPriority(
                                        applicationId, categoryPendingId, true, null))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
    }

    /** Staff đơn vị khác không thấy đơn: phân giường trả NOT_FOUND, không lộ dữ liệu. */
    @Test
    void assignOutsideStaffUnitIsNotFound() {
        Long otherUnitId =
                jdbc.queryForObject(
                        "SELECT id FROM management_unit WHERE code<>'K1' ORDER BY id LIMIT 1",
                        Long.class);
        Long otherStaffId = createUser(Role.STAFF, otherUnitId, "Cán bộ " + suffix + "X");
        when(currentUser.get())
                .thenReturn(new Actor(otherStaffId, Role.STAFF, otherUnitId, false, false));

        assertThatThrownBy(() -> applications.assignBed(applicationId, firstBedId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM dormitory_contract WHERE application_id=?",
                                applicationId))
                .isZero();
    }

    private Long createUser(String role, Long unitId, String name) {
        return jdbc.queryForObject(
                "INSERT INTO app_user(username,password_hash,role,full_name,"
                        + "management_unit_id,must_change_password) VALUES(?,?,?,?,?,false)"
                        + " RETURNING id",
                Long.class,
                role + "-" + UUID.randomUUID(),
                "hash",
                role,
                name,
                unitId);
    }

    private Long createStudent(String code) {
        Long id =
                jdbc.queryForObject(
                        "INSERT INTO app_user(username,password_hash,role,full_name,"
                                + "must_change_password) VALUES(?,?,'STUDENT',?,false) RETURNING id",
                        Long.class,
                        code,
                        "hash",
                        "Sinh viên " + code);
        jdbc.update(
                "INSERT INTO student_profile(user_id,student_code,gender) VALUES(?,?,'M')",
                id,
                code);
        return id;
    }

    private Long createPriorityCategory(String code, int weight) {
        return jdbc.queryForObject(
                "INSERT INTO priority_category(code,name,weight,active)"
                        + " VALUES(?,?,?,true) RETURNING id",
                Long.class,
                code + "-" + suffix,
                "Diện " + code + " " + suffix,
                weight);
    }

    private void addPriority(Long categoryId) {
        jdbc.update(
                "INSERT INTO application_priority(application_id,category_id)"
                        + " VALUES(?,?)",
                applicationId,
                categoryId);
    }

    private Long createBed(Long room, String code) {
        return jdbc.queryForObject(
                "INSERT INTO bed(room_id,code) VALUES(?,?) RETURNING id",
                Long.class,
                room,
                code);
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }
}

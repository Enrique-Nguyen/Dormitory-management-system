package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.ExtensionSource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.constants.DmsConstants.StayRequestStatus;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.contract.api.ContractDtos;
import vn.edu.tlu.dms.contract.service.ContractCrudService;
import vn.edu.tlu.dms.room.service.BedOccupancyService;
import vn.edu.tlu.dms.support.FutureSemesterFixture;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Kiểm thử tích hợp duyệt/từ chối/hủy yêu cầu gia hạn trên PostgreSQL thật (FR-CON-06, API 4.9):
 * cập nhật endDate, chống chồng khoảng lưu trú của cùng sinh viên, lưu lý do từ chối, quyền hủy của
 * sinh viên sở hữu và timeline/audit. Mọi case chạy trong transaction được rollback.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ContractExtensionIntegrationTest {

    private static final LocalDate CONTRACT_START = LocalDate.of(2099, 9, 1);

    private static final LocalDate CONTRACT_END = LocalDate.of(2100, 1, 31);

    private static final LocalDate EXTENDED_END = CONTRACT_END.plusDays(30);

    @Autowired ContractCrudService extensions;

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

    private Long contractId;

    private Long extensionId;

    @BeforeEach
    void createFixture() {
        suffix = UUID.randomUUID().toString().substring(0, 6);
        unitId = jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        staffId = createUser(Role.STAFF, unitId, "Cán bộ " + suffix);
        when(currentUser.get()).thenReturn(new Actor(staffId, Role.STAFF, unitId, false, false));

        studentId = createStudent("SV" + suffix);
        // Học kỳ cấp phát động để không trùng UNIQUE(academic_year, type) và không chồng
        // daterange với học kỳ đã có trong CSDL.
        semesterId = FutureSemesterFixture.insert(jdbc, SemesterType.HK1, "HK1 " + suffix);
        periodId =
                jdbc.queryForObject(
                        "INSERT INTO registration_period(semester_id,kind,name,open_at,close_at,"
                                + " created_by) VALUES(?,'REGULAR','Đợt',now(),now()+interval '1"
                                + " day',?) RETURNING id",
                        Long.class,
                        semesterId,
                        staffId);
        applicationId =
                jdbc.queryForObject(
                        "INSERT INTO room_application(student_id,period_id,preferred_unit_id,"
                                + "status) VALUES(?,?,?,'ASSIGNED') RETURNING id",
                        Long.class,
                        studentId,
                        periodId,
                        unitId);
        contractId =
                jdbc.queryForObject(
                        "INSERT INTO dormitory_contract(code,student_id,application_id,start_date,"
                                + "end_date,status,activation_deadline,deposit_required,activated_at,"
                                + "activated_by) VALUES(?,?,?,?,?,'ACTIVE',now()+interval '7"
                                + " days',1000000,now(),?) RETURNING id",
                        Long.class,
                        "HD" + suffix,
                        studentId,
                        applicationId,
                        CONTRACT_START,
                        CONTRACT_END,
                        staffId);
        Long roomTypeId =
                jdbc.queryForObject(
                        "INSERT INTO room_type(name,bed_count,price_main,price_summer)"
                                + " VALUES(?,2,1500000,700000) RETURNING id",
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
        Long roomId =
                jdbc.queryForObject(
                        "INSERT INTO room(building_id,room_type_id,floor,code,gender,status)"
                                + " VALUES(?,?,1,?,'M','ACTIVE') RETURNING id",
                        Long.class,
                        buildingId,
                        roomTypeId,
                        "P" + suffix);
        Long bedId =
                jdbc.queryForObject(
                        "INSERT INTO bed(room_id,code) VALUES(?,'01') RETURNING id",
                        Long.class,
                        roomId);
        occupancy.occupyStay(bedId, studentId, contractId, null);

        extensionId =
                jdbc.queryForObject(
                        "INSERT INTO contract_extension_request(contract_id,source,old_end_date,"
                                + "new_end_date,reason,status)"
                                + " VALUES(?,?,?,?,?,?) RETURNING id",
                        Long.class,
                        contractId,
                        ExtensionSource.STUDENT,
                        CONTRACT_END,
                        EXTENDED_END,
                        "Ở thêm học kỳ hè",
                        StayRequestStatus.PENDING);
    }

    /** Staff đúng đơn vị duyệt: cập nhật endDate, trạng thái, lịch sử và audit. */
    @Test
    void approveUpdatesContractEndDate() {
        ContractDtos.ExtensionDetail before = extensions.getExtensionDetail(extensionId);
        assertThat(before.allowedActions())
                .contains(BusinessAction.APPROVE, BusinessAction.REJECT);

        ContractDtos.ExtensionDetail detail = extensions.approveExtension(extensionId);

        assertThat(detail.status()).isEqualTo(StayRequestStatus.APPROVED);
        assertThat(detail.decidedBy()).isEqualTo(staffId);
        assertThat(detail.decidedAt()).isNotNull();
        assertThat(detail.oldEndDate()).isEqualTo(CONTRACT_END);
        assertThat(detail.newEndDate()).isEqualTo(EXTENDED_END);
        assertThat(detail.allowedActions()).isEmpty();
        assertThat(detail.timeline())
                .anyMatch(
                        row ->
                                StayRequestStatus.PENDING.equals(row.fromStatus())
                                        && StayRequestStatus.APPROVED.equals(row.toStatus()));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT end_date FROM dormitory_contract WHERE id=?",
                                LocalDate.class,
                                contractId))
                .isEqualTo(EXTENDED_END);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE action=? AND entity_id=?",
                                Integer.class,
                                AuditAction.CONTRACT_EXTENSION_APPROVE,
                                extensionId))
                .isEqualTo(1);
    }

    /** Duyệt lần hai bị chặn vì yêu cầu không còn PENDING. */
    @Test
    void doubleApproveIsRejected() {
        extensions.approveExtension(extensionId);

        assertThatThrownBy(() -> extensions.approveExtension(extensionId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT end_date FROM dormitory_contract WHERE id=?",
                                LocalDate.class,
                                contractId))
                .isEqualTo(EXTENDED_END);
    }

    /** Từ chối bắt buộc lý do, lưu decision_reason và không đổi endDate của hợp đồng. */
    @Test
    void rejectRequiresReasonAndKeepsContractEndDate() {
        assertThatThrownBy(() -> extensions.rejectExtension(extensionId, " "))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                            assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                        });

        ContractDtos.ExtensionDetail detail =
                extensions.rejectExtension(extensionId, "Không đủ điều kiện ở thêm");

        assertThat(detail.status()).isEqualTo(StayRequestStatus.REJECTED);
        assertThat(detail.decisionReason()).isEqualTo("Không đủ điều kiện ở thêm");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT end_date FROM dormitory_contract WHERE id=?",
                                LocalDate.class,
                                contractId))
                .isEqualTo(CONTRACT_END);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE action=? AND entity_id=?",
                                Integer.class,
                                AuditAction.CONTRACT_EXTENSION_REJECT,
                                extensionId))
                .isEqualTo(1);
    }

    /** Sinh viên chủ sở hữu hủy yêu cầu PENDING; hợp đồng không đổi. */
    @Test
    void ownerCanCancelPendingExtension() {
        when(currentUser.get())
                .thenReturn(new Actor(studentId, Role.STUDENT, null, false, false));

        ContractDtos.ExtensionDetail detail = extensions.cancelExtension(extensionId);

        assertThat(detail.status()).isEqualTo(StayRequestStatus.CANCELLED);
        assertThat(detail.allowedActions()).isEmpty();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT end_date FROM dormitory_contract WHERE id=?",
                                LocalDate.class,
                                contractId))
                .isEqualTo(CONTRACT_END);
    }

    /**
     * Chồng khoảng lưu trú: DB chuẩn chỉ cho một hợp đồng PENDING/ACTIVE mỗi sinh viên, nên test
     * tạm gỡ unique index trong transaction để mô phỏng dữ liệu xung đột và kiểm tra lớp phòng vệ
     * của Service. Rollback của test khôi phục nguyên trạng index.
     */
    @Test
    void overlappingContractBlocksApproval() {
        jdbc.execute("DROP INDEX ux_contract_live_student");
        // Học kỳ thứ hai cũng cấp phát động, tiếp sau học kỳ của fixture nên không chồng daterange.
        Long secondSemester =
                FutureSemesterFixture.insert(jdbc, SemesterType.HK2, "HK2 " + suffix);
        Long secondPeriod =
                jdbc.queryForObject(
                        "INSERT INTO registration_period(semester_id,kind,name,open_at,close_at,"
                                + " created_by) VALUES(?,'REGULAR','Đợt 2',now(),now()+interval '1"
                                + " day',?) RETURNING id",
                        Long.class,
                        secondSemester,
                        staffId);
        Long secondApplication =
                jdbc.queryForObject(
                        "INSERT INTO room_application(student_id,period_id,preferred_unit_id,"
                                + "status) VALUES(?,?,?,'ASSIGNED') RETURNING id",
                        Long.class,
                        studentId,
                        secondPeriod,
                        unitId);
        jdbc.update(
                "INSERT INTO dormitory_contract(code,student_id,application_id,start_date,end_date,"
                        + "status,activation_deadline) VALUES(?,?,?,?,?,'PENDING',"
                        + "now()+interval '7 days')",
                "HD2" + suffix,
                studentId,
                secondApplication,
                CONTRACT_END.minusDays(10),
                EXTENDED_END.plusDays(60));

        assertThatThrownBy(() -> extensions.approveExtension(extensionId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode())
                                    .isEqualTo(ErrorCode.CONTRACT_PERIOD_OVERLAP);
                            assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                            assertThat(e.getDetails()).containsKey("contractCode");
                        });
        assertThat(
                        jdbc.queryForObject(
                                "SELECT end_date FROM dormitory_contract WHERE id=?",
                                LocalDate.class,
                                contractId))
                .isEqualTo(CONTRACT_END);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM contract_extension_request WHERE id=?",
                                String.class,
                                extensionId))
                .isEqualTo(StayRequestStatus.PENDING);
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
                                + "must_change_password) VALUES(?,?,'STUDENT',?,false)"
                                + " RETURNING id",
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
}

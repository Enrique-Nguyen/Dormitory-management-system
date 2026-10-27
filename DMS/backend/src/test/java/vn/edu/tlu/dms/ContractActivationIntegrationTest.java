package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.DepositType;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.PaymentMethod;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.contract.api.ContractDtos;
import vn.edu.tlu.dms.contract.service.ContractService;
import vn.edu.tlu.dms.room.service.BedOccupancyService;
import vn.edu.tlu.dms.support.FutureSemesterFixture;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Kiểm thử tích hợp kích hoạt hợp đồng trên PostgreSQL thật (FR-CON-01, AT-18, API 4.6): thu đúng
 * mức cọc của tòa, HOLD → STAY, hạn kích hoạt, hợp đồng đã hủy, gửi lặp, thiếu giấy và rollback khi
 * thu cọc lỗi. Lớp không bọc transaction của test để kiểm chứng rollback thật của Service; dữ liệu
 * được dọn trong {@link #cleanup()}. Cần Docker đang chạy.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ContractActivationIntegrationTest {

    private static final LocalDate CONTRACT_START = LocalDate.of(2099, 9, 1);

    private static final LocalDate CONTRACT_END = LocalDate.of(2100, 1, 31);

    private static final long DEPOSIT = 1_000_000L;

    @Autowired ContractService contracts;

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

    private Long roomTypeId;

    private Long buildingId;

    private Long roomId;

    private Long bedId;

    @BeforeEach
    void createFixture() {
        suffix = UUID.randomUUID().toString().substring(0, 6);
        unitId = jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        staffId = createUser(Role.STAFF, unitId, "Cán bộ " + suffix);
        when(currentUser.get()).thenReturn(new Actor(staffId, Role.STAFF, unitId, false, false));

        studentId = createStudent("SV" + suffix);
        FutureSemesterFixture.Created semester =
                FutureSemesterFixture.create(
                        jdbc, SemesterType.HK1, "HK1 " + suffix, 30);
        semesterId = semester.id();
        LocalDate contractStart = semester.startDate();
        LocalDate contractEnd = semester.endDate();
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
                                + "status) VALUES(?,?,?,'APPROVED') RETURNING id",
                        Long.class,
                        studentId,
                        periodId,
                        unitId);
        contractId =
                jdbc.queryForObject(
                        "INSERT INTO dormitory_contract(code,student_id,application_id,start_date,"
                                + "end_date,status,activation_deadline)"
                                + " VALUES(?,?,?,?,?,'PENDING',now()+interval '7 days')"
                                + " RETURNING id",
                        Long.class,
                        "HD" + suffix,
                        studentId,
                        applicationId,
                        contractStart,
                        contractEnd);

        roomTypeId =
                jdbc.queryForObject(
                        "INSERT INTO room_type(name,bed_count,price_main,price_summer)"
                                + " VALUES(?,2,1500000,700000) RETURNING id",
                        Long.class,
                        "Loại " + suffix);
        buildingId =
                jdbc.queryForObject(
                        "INSERT INTO building(management_unit_id,code,name,deposit_amount)"
                                + " VALUES(?,?,?,?) RETURNING id",
                        Long.class,
                        unitId,
                        "B" + suffix,
                        "Tòa " + suffix,
                        DEPOSIT);
        roomId =
                jdbc.queryForObject(
                        "INSERT INTO room(building_id,room_type_id,floor,code,gender,status)"
                                + " VALUES(?,?,1,?,'M','ACTIVE') RETURNING id",
                        Long.class,
                        buildingId,
                        roomTypeId,
                        "P" + suffix);
        bedId =
                jdbc.queryForObject(
                        "INSERT INTO bed(room_id,code) VALUES(?,'01') RETURNING id",
                        Long.class,
                        roomId);
        occupancy.occupyHold(bedId, studentId, contractId, null);
    }

    @AfterEach
    void cleanup() {
        if (contractId != null) {
            jdbc.update("DELETE FROM deposit_transaction WHERE contract_id=?", contractId);
            jdbc.update(
                    "DELETE FROM contract_extension_request WHERE contract_id=?", contractId);
            jdbc.update("DELETE FROM room_assignment_history WHERE contract_id=?", contractId);
            jdbc.update("DELETE FROM dormitory_contract WHERE id=?", contractId);
        }
        if (applicationId != null) {
            jdbc.update("DELETE FROM room_application WHERE id=?", applicationId);
        }
        if (periodId != null) {
            jdbc.update("DELETE FROM registration_period WHERE id=?", periodId);
        }
        if (semesterId != null) {
            jdbc.update("DELETE FROM semester WHERE id=?", semesterId);
        }
        if (roomId != null) {
            jdbc.update("DELETE FROM bed WHERE room_id=?", roomId);
            jdbc.update("DELETE FROM room WHERE id=?", roomId);
        }
        if (buildingId != null) {
            jdbc.update("DELETE FROM building WHERE id=?", buildingId);
        }
        if (roomTypeId != null) {
            jdbc.update("DELETE FROM room_type WHERE id=?", roomTypeId);
        }
    }

    /** AT-18: đúng mức cọc → ACTIVE, thu cọc một lần và chuyển HOLD sang STAY. */
    @Test
    void activationCollectsDepositAndConvertsHoldToStay() {
        ContractDtos.ActivationResult result =
                contracts.activate(
                        contractId,
                        new ContractDtos.ActivateRequest(
                                true, DEPOSIT, PaymentMethod.CASH, "Thu cọc kích hoạt"));

        assertThat(result.contract().status()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(result.contract().activatedAt()).isNotNull();
        assertThat(result.contract().depositRequired()).isEqualTo(DEPOSIT);
        assertThat(result.deposit().type()).isEqualTo(DepositType.DEPOSIT);
        assertThat(result.deposit().amount()).isEqualTo(DEPOSIT);
        assertThat(result.deposit().paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(result.currentAssignment()).isNotNull();
        assertThat(result.currentAssignment().kind()).isEqualTo(AssignmentKind.STAY);

        var contract =
                jdbc.queryForMap(
                        "SELECT status,deposit_required,activated_at,activated_by"
                                + " FROM dormitory_contract WHERE id=?",
                        contractId);
        assertThat(contract.get("status")).isEqualTo(ContractStatus.ACTIVE);
        assertThat(((Number) contract.get("deposit_required")).longValue()).isEqualTo(DEPOSIT);
        assertThat(contract.get("activated_at")).isNotNull();
        assertThat(((Number) contract.get("activated_by")).longValue()).isEqualTo(staffId);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT kind FROM room_assignment_history WHERE contract_id=?",
                                String.class,
                                contractId))
                .isEqualTo(AssignmentKind.STAY);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT stay_started_at IS NOT NULL FROM room_assignment_history"
                                        + " WHERE contract_id=?",
                                Boolean.class,
                                contractId))
                .isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT amount FROM deposit_transaction WHERE contract_id=?"
                                        + " AND type='DEPOSIT'",
                                Long.class,
                                contractId))
                .isEqualTo(DEPOSIT);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM status_history WHERE entity_type=?"
                                        + " AND entity_id=? AND from_status=? AND to_status=?",
                                Integer.class,
                                EntityType.CONTRACT,
                                contractId,
                                ContractStatus.PENDING,
                                ContractStatus.ACTIVE))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE action=? AND entity_id=?",
                                Integer.class,
                                AuditAction.CONTRACT_ACTIVATE,
                                contractId))
                .isEqualTo(1);
    }

    /** AT-18: sai mức cọc trả DEPOSIT_AMOUNT_MISMATCH kèm mức bắt buộc và không ghi gì. */
    @Test
    void wrongDepositAmountIsRejectedWithRequiredAmount() {
        assertThatThrownBy(
                        () ->
                                contracts.activate(
                                        contractId,
                                        new ContractDtos.ActivateRequest(
                                                true, DEPOSIT - 1, PaymentMethod.CASH, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode())
                                    .isEqualTo(ErrorCode.DEPOSIT_AMOUNT_MISMATCH);
                            assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                            assertThat(e.getDetails()).containsEntry("required", DEPOSIT);
                        });
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contractId))
                .isEqualTo(ContractStatus.PENDING);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT deposit_required FROM dormitory_contract WHERE id=?",
                                Long.class,
                                contractId))
                .isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT kind FROM room_assignment_history WHERE contract_id=?",
                                String.class,
                                contractId))
                .isEqualTo(AssignmentKind.HOLD);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM deposit_transaction WHERE contract_id=?",
                                Integer.class,
                                contractId))
                .isZero();
    }

    /** Quá hạn kích hoạt trả CONTRACT_ACTIVATION_EXPIRED dù job chưa kịp hủy. */
    @Test
    void expiredDeadlineIsRejected() {
        jdbc.update(
                "UPDATE dormitory_contract SET activation_deadline=now()-interval '1 day'"
                        + " WHERE id=?",
                contractId);

        assertThatThrownBy(
                        () ->
                                contracts.activate(
                                        contractId,
                                        new ContractDtos.ActivateRequest(
                                                true, DEPOSIT, PaymentMethod.TRANSFER, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode())
                                    .isEqualTo(ErrorCode.CONTRACT_ACTIVATION_EXPIRED);
                            assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                        });
        assertThat(
                        jdbc.queryForObject(
                                "SELECT kind FROM room_assignment_history WHERE contract_id=?",
                                String.class,
                                contractId))
                .isEqualTo(AssignmentKind.HOLD);
    }

    /** Hợp đồng đã bị job tự hủy trả CONTRACT_AUTO_CANCELLED. */
    @Test
    void cancelledContractReportsAutoCancelled() {
        jdbc.update(
                "UPDATE dormitory_contract SET status='CANCELLED',closed_at=now(),"
                        + "status_reason='Quá hạn' WHERE id=?",
                contractId);

        assertThatThrownBy(
                        () ->
                                contracts.activate(
                                        contractId,
                                        new ContractDtos.ActivateRequest(
                                                true, DEPOSIT, PaymentMethod.CASH, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode())
                                    .isEqualTo(ErrorCode.CONTRACT_AUTO_CANCELLED);
                            assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                        });
    }

    /** Gửi kích hoạt lần hai bị chặn, không thu cọc trùng. */
    @Test
    void secondActivationIsRejected() {
        contracts.activate(
                contractId,
                new ContractDtos.ActivateRequest(true, DEPOSIT, PaymentMethod.CASH, null));

        assertThatThrownBy(
                        () ->
                                contracts.activate(
                                        contractId,
                                        new ContractDtos.ActivateRequest(
                                                true, DEPOSIT, PaymentMethod.CASH, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM deposit_transaction WHERE contract_id=?",
                                Integer.class,
                                contractId))
                .isEqualTo(1);
    }

    /** Không xác nhận giấy hợp đồng trả VALIDATION_FAILED trước khi khóa/ghi dữ liệu. */
    @Test
    void unsignedPaperContractIsRejected() {
        assertThatThrownBy(
                        () ->
                                contracts.activate(
                                        contractId,
                                        new ContractDtos.ActivateRequest(
                                                false, DEPOSIT, PaymentMethod.CASH, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                            assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                        });
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contractId))
                .isEqualTo(ContractStatus.PENDING);
    }

    /** Không còn HOLD thì không thể kích hoạt. */
    @Test
    void missingHoldAssignmentIsRejected() {
        jdbc.update("DELETE FROM room_assignment_history WHERE contract_id=?", contractId);

        assertThatThrownBy(
                        () ->
                                contracts.activate(
                                        contractId,
                                        new ContractDtos.ActivateRequest(
                                                true, DEPOSIT, PaymentMethod.CASH, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NO_HOLD_ASSIGNMENT));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contractId))
                .isEqualTo(ContractStatus.PENDING);
    }

    /** Thu cọc lỗi (đã có giao dịch DEPOSIT) làm rollback cả trạng thái, cọc và HOLD → STAY. */
    @Test
    void depositFailureRollsBackWholeActivation() {
        jdbc.update(
                "INSERT INTO deposit_transaction(contract_id,type,amount,confirmed_by)"
                        + " VALUES(?,'DEPOSIT',?,?)",
                contractId,
                DEPOSIT,
                staffId);

        assertThatThrownBy(
                        () ->
                                contracts.activate(
                                        contractId,
                                        new ContractDtos.ActivateRequest(
                                                true, DEPOSIT, PaymentMethod.CASH, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_DEPOSIT));

        // Transaction của Service phải rollback toàn bộ, không chỉ dừng ở bước thu cọc.
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contractId))
                .isEqualTo(ContractStatus.PENDING);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT deposit_required FROM dormitory_contract WHERE id=?",
                                Long.class,
                                contractId))
                .isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT activated_at FROM dormitory_contract WHERE id=?",
                                Object.class,
                                contractId))
                .isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT kind FROM room_assignment_history WHERE contract_id=?",
                                String.class,
                                contractId))
                .isEqualTo(AssignmentKind.HOLD);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM status_history WHERE entity_type=?"
                                        + " AND entity_id=? AND to_status=?",
                                Integer.class,
                                EntityType.CONTRACT,
                                contractId,
                                ContractStatus.ACTIVE))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE action=? AND entity_id=?",
                                Integer.class,
                                AuditAction.CONTRACT_ACTIVATE,
                                contractId))
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

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

import vn.edu.tlu.dms.billing.service.DepositService;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ConfigKey;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.DepositType;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.ChargeableStudent;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Conclude;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Deduction;
import vn.edu.tlu.dms.maintenance.service.MaintenanceRequestService;
import vn.edu.tlu.dms.support.FutureSemesterFixture;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kiểm thử tích hợp luồng kết luận ticket và trừ cọc trên PostgreSQL thật (FR-MNT-09/10, AT-14,
 * AT-22). Cần Docker đang chạy; nếu môi trường không có Docker thì không chạy được lớp này.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class MaintenanceConcludeIntegrationTest {

    @Autowired MaintenanceRequestService maintenance;

    @Autowired DepositService depositService;

    @Autowired JdbcTemplate jdbc;

    @MockitoBean CurrentUser currentUser;

    private String suffix;

    private Long unitId;

    private Long staffId;

    private Long buildingId;

    private Long roomTypeId;

    private Long roomId;

    private Long ticketId;

    private final List<Long> studentIds = new ArrayList<>();

    private final List<Long> contractIds = new ArrayList<>();

    private int ticketSequence;

    private int contractSequence;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        unitId = jdbc.queryForObject("SELECT id FROM management_unit WHERE code = 'K1'", Long.class);
        staffId = insertUser("staff_" + suffix, Role.STAFF, unitId, "Staff Test");
        buildingId =
                jdbc.queryForObject(
                        "INSERT INTO building(code, name, management_unit_id, deposit_amount)"
                                + " VALUES(?, ?, ?, 1000000) RETURNING id",
                        Long.class,
                        "B" + suffix,
                        "Building " + suffix,
                        unitId);
        roomTypeId =
                jdbc.queryForObject(
                        "INSERT INTO room_type(name, bed_count, price_main, price_summer)"
                                + " VALUES(?, 4, 1500000, 500000) RETURNING id",
                        Long.class,
                        "T4-" + suffix);
        roomId = createRoom("101");

        Long semesterId =
                // Học kỳ cấp phát động: tránh UNIQUE(academic_year, type) và ràng buộc chồng
                // daterange của bảng semester khi CSDL đã có dữ liệu mẫu.
                FutureSemesterFixture.insert(jdbc, SemesterType.HK1, "Học kỳ 1 " + suffix);
        Long periodId =
                jdbc.queryForObject(
                        """
                        INSERT INTO registration_period(semester_id, kind, name, open_at, close_at, created_by)
                        VALUES(?, 'REGULAR', ?, now(), now() + interval '30 days', ?)
                        RETURNING id
                        """,
                        Long.class,
                        semesterId,
                        "Period " + suffix,
                        staffId);

        for (int index = 0; index < 3; index++) {
            Long studentId = createStudent(suffix + index, "Student " + index);
            Long contractId = createContract(studentId, periodId);
            Long bedId =
                    jdbc.queryForObject(
                            "INSERT INTO bed(room_id, code) VALUES(?, ?) RETURNING id",
                            Long.class,
                            roomId,
                            String.format("%02d", index + 1));
            jdbc.update(
                    """
                    INSERT INTO room_assignment_history(student_id, contract_id, bed_id, kind,
                        started_at, stay_started_at, price_main, price_summer, created_by)
                    VALUES(?, ?, ?, 'STAY', now() - interval '2 days', now() - interval '1 day',
                        1500000, 500000, ?)
                    """,
                    studentId,
                    contractId,
                    bedId,
                    staffId);
            studentIds.add(studentId);
            contractIds.add(contractId);
        }

        ticketId = createRoomTicket(roomId, MaintenanceStatus.CLOSED, null, null);

        // Cấu hình hạn nộp bù cọc khác mặc định để chứng minh Service đọc ConfigService.
        jdbc.update(
                "UPDATE system_config SET value = '9' WHERE key = ?",
                ConfigKey.DEPOSIT_TOPUP_DUE_DAYS);

        when(currentUser.get())
                .thenReturn(new Actor(staffId, Role.STAFF, unitId, false, false));
        for (Long contractId : contractIds) {
            depositService.collectInitial(contractId, 1_000_000L, "Thu cọc khi kích hoạt");
        }
    }

    @Test
    void at14SplitsRepairCostAcrossThreeOccupantsAndWritesLedger() {
        LocalDate expectedDueDate = LocalDate.now().plusDays(9);
        var response =
                maintenance.conclude(
                        ticketId, new Conclude("MISUSE", 100_000L, null, null));

        assertThat(response.cause()).isEqualTo("MISUSE");
        assertThat(response.repairCost()).isEqualTo(100_000L);
        assertThat(response.concluded()).isTrue();
        assertThat(response.deductions())
                .extracting(Deduction::contractId)
                .containsExactlyElementsOf(contractIds);
        assertThat(response.deductions())
                .extracting(Deduction::amount)
                .containsExactly(33_333L, 33_333L, 33_334L);
        assertThat(response.deductions())
                .extracting(Deduction::topUpDueDate)
                .containsOnly(expectedDueDate);

        // Chia tiền theo hợp đồng tăng dần; người cuối nhận phần dư.
        assertThat(deductionAmounts(ticketId))
                .containsExactly(-33_333L, -33_333L, -33_334L);

        Map<String, Object> ticket = ticketRow(ticketId);
        assertThat(ticket.get("cause")).isEqualTo("MISUSE");
        assertThat(((Number) ticket.get("repair_cost")).longValue()).isEqualTo(100_000L);
        assertThat(ticket.get("cost_note")).isNull();
        assertThat(((Number) ticket.get("cost_decided_by")).longValue()).isEqualTo(staffId);
        assertThat(ticket.get("status")).isEqualTo(MaintenanceStatus.CLOSED);

        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT COUNT(*) FROM status_history
                                WHERE entity_type = ? AND entity_id = ? AND from_status = ? AND to_status = ?
                                """,
                                Long.class,
                                EntityType.MAINTENANCE,
                                ticketId,
                                MaintenanceStatus.CLOSED,
                                MaintenanceStatus.CLOSED))
                .isEqualTo(1L);
        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT COUNT(*) FROM audit_log
                                WHERE action = ? AND entity_type = ? AND entity_id = ?
                                """,
                                Long.class,
                                AuditAction.MAINTENANCE_CONCLUDE,
                                EntityType.MAINTENANCE,
                                ticketId))
                .isEqualTo(1L);
        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT new_value ->> 'cause' FROM audit_log
                                WHERE action = ? AND entity_type = ? AND entity_id = ?
                                """,
                                String.class,
                                AuditAction.MAINTENANCE_CONCLUDE,
                                EntityType.MAINTENANCE,
                                ticketId))
                .isEqualTo("MISUSE");

        // Màn chi tiết đọc lại đúng các khoản trừ cọc đã ghi.
        var detail = maintenance.getMaintenanceRequestById(ticketId);
        assertThat(detail.deductions())
                .extracting(Deduction::contractId)
                .containsExactlyElementsOf(contractIds);
        assertThat(detail.deductions())
                .extracting(Deduction::amount)
                .containsExactly(33_333L, 33_333L, 33_334L);
    }

    @Test
    void emptyChargedStudentListSelectsEveryOccupant() {
        var response =
                maintenance.conclude(
                        ticketId, new Conclude("MISUSE", 99_999L, List.of(), null));
        assertThat(response.deductions())
                .extracting(Deduction::amount)
                .containsExactly(33_333L, 33_333L, 33_333L);
        assertThat(deductionAmounts(ticketId))
                .containsExactly(-33_333L, -33_333L, -33_333L);
    }

    @Test
    void at22WearDoesNotCreateDeductionOrRepairCost() {
        var response =
                maintenance.conclude(
                        ticketId,
                        new Conclude("WEAR", null, null, "Hao mòn tự nhiên theo thời gian"));

        assertThat(response.cause()).isEqualTo("WEAR");
        assertThat(response.repairCost()).isNull();
        assertThat(response.costNote()).isEqualTo("Hao mòn tự nhiên theo thời gian");
        assertThat(response.deductions()).isEmpty();
        assertThat(deductionCount(ticketId)).isZero();

        Map<String, Object> ticket = ticketRow(ticketId);
        assertThat(ticket.get("cause")).isEqualTo("WEAR");
        assertThat(ticket.get("repair_cost")).isNull();
        assertThat(ticket.get("cost_note")).isEqualTo("Hao mòn tự nhiên theo thời gian");
    }

    @Test
    void wearRequiresNote() {
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        ticketId, new Conclude("WEAR", null, null, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(ticketRow(ticketId).get("cause")).isNull();
        assertThat(deductionCount(ticketId)).isZero();
    }

    @Test
    void wearRejectsRepairCost() {
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        ticketId,
                                        new Conclude("WEAR", 10_000L, null, "Hao mòn tự nhiên")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.COST_INVALID));
        assertThat(ticketRow(ticketId).get("cause")).isNull();
        assertThat(deductionCount(ticketId)).isZero();
    }

    @Test
    void misuseRequiresRepairCost() {
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        ticketId, new Conclude("MISUSE", null, null, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.COST_INVALID));
    }

    @Test
    void misuseRejectsZeroRepairCost() {
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        ticketId, new Conclude("MISUSE", 0L, null, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.COST_INVALID));
    }

    @Test
    void invalidCauseIsRejected() {
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        ticketId, new Conclude("OTHER", 10_000L, null, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void commonAreaTicketCannotBeCharged() {
        Long commonTicket = createCommonTicket(MaintenanceStatus.CLOSED);
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        commonTicket,
                                        new Conclude("MISUSE", 100_000L, null, "Hỏng do sử dụng")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.COMMON_AREA_NO_CHARGE));
        assertThat(deductionCount(commonTicket)).isZero();
    }

    @Test
    void ticketMustBeClosedBeforeConclusion() {
        Long openTicket = createRoomTicket(roomId, MaintenanceStatus.OPEN, null, null);
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        openTicket,
                                        new Conclude("MISUSE", 100_000L, null, "Hỏng do sử dụng")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(ticketRow(openTicket).get("cause")).isNull();
    }

    @Test
    void ticketCannotBeConcludedTwice() {
        maintenance.conclude(
                ticketId, new Conclude("MISUSE", 100_000L, null, "Hỏng do sử dụng"));
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        ticketId,
                                        new Conclude("WEAR", null, null, "Hao mòn tự nhiên")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.TICKET_ALREADY_CONCLUDED));
        assertThat(deductionCount(ticketId)).isEqualTo(3L);
    }

    @Test
    void subsetRequiresNote() {
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        ticketId,
                                        new Conclude(
                                                "MISUSE",
                                                100_000L,
                                                List.of(studentIds.get(0), studentIds.get(1)),
                                                null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(deductionCount(ticketId)).isZero();
    }

    @Test
    void subsetChargesOnlySelectedStudents() {
        var response =
                maintenance.conclude(
                        ticketId,
                        new Conclude(
                                "MISUSE",
                                100_000L,
                                List.of(studentIds.get(0), studentIds.get(1)),
                                "Chỉ hai bạn làm hỏng thiết bị"));
        assertThat(response.deductions())
                .extracting(Deduction::contractId)
                .containsExactly(contractIds.get(0), contractIds.get(1));
        assertThat(response.deductions())
                .extracting(Deduction::amount)
                .containsExactly(50_000L, 50_000L);
        assertThat(deductionCountForContract(ticketId, contractIds.get(2))).isZero();
    }

    @Test
    void chargedStudentsMustBeOccupantsAtTicketCreation() {
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        ticketId,
                                        new Conclude(
                                                "MISUSE",
                                                100_000L,
                                                List.of(999_999L),
                                                "Cần ghi chú")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> {
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.CHARGED_STUDENTS_INVALID);
                            assertThat(exception.getDetails())
                                    .containsEntry("invalidIds", List.of(999_999L));
                        });
        assertThat(deductionCount(ticketId)).isZero();
    }

    @Test
    void emptyOccupancyAtTicketCreationCannotBeCharged() {
        Long emptyRoomId = createRoom("102");
        Long emptyTicket = createRoomTicket(emptyRoomId, MaintenanceStatus.CLOSED, null, null);
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        emptyTicket,
                                        new Conclude("MISUSE", 100_000L, null, "Hỏng do sử dụng")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.NO_OCCUPANT_TO_CHARGE));
    }

    @Test
    void chargeableStudentsComeFromOccupancyAtTicketCreation() {
        var chargeable = maintenance.findChargeableStudents(ticketId);
        assertThat(chargeable).hasSize(3);
        assertThat(chargeable)
                .extracting(ChargeableStudent::contractId)
                .containsExactlyElementsOf(contractIds);
        assertThat(chargeable)
                .extracting(item -> item.student().studentCode())
                .containsExactly(suffix + "0", suffix + "1", suffix + "2");
        assertThat(chargeable)
                .allSatisfy(
                        item -> {
                            assertThat(item.student().id()).isNotNull();
                            assertThat(item.student().fullName()).isNotBlank();
                            assertThat(item.student().gender()).isEqualTo("M");
                            assertThat(item.contractStatus()).isEqualTo(ContractStatus.ACTIVE);
                        });
    }

    @Test
    void staffOutsideUnitCannotReadChargeableStudents() {
        when(currentUser.get())
                .thenReturn(
                        new Actor(staffId, Role.STAFF, unitId + 1_000, false, false));
        assertThatThrownBy(() -> maintenance.findChargeableStudents(ticketId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void staffOutsideUnitCannotConclude() {
        when(currentUser.get())
                .thenReturn(
                        new Actor(staffId, Role.STAFF, unitId + 1_000, false, false));
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        ticketId,
                                        new Conclude("MISUSE", 100_000L, null, "Hỏng do sử dụng")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void technicianCannotConclude() {
        when(currentUser.get())
                .thenReturn(new Actor(99L, Role.TECHNICIAN, unitId, false, false));
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        ticketId,
                                        new Conclude("MISUSE", 100_000L, null, "Hỏng do sử dụng")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    private Long createRoom(String code) {
        return jdbc.queryForObject(
                "INSERT INTO room(building_id, room_type_id, code, floor, gender, status)"
                        + " VALUES(?, ?, ?, 1, 'M', 'ACTIVE') RETURNING id",
                Long.class,
                buildingId,
                roomTypeId,
                code);
    }

    private Long createStudent(String studentCode, String fullName) {
        Long studentId = insertUser(studentCode, Role.STUDENT, null, fullName);
        jdbc.update(
                "INSERT INTO student_profile(user_id, student_code, gender) VALUES(?, ?, 'M')",
                studentId,
                studentCode);
        return studentId;
    }

    private Long createContract(Long studentId, Long periodId) {
        Long applicationId =
                jdbc.queryForObject(
                        """
                        INSERT INTO room_application(student_id, period_id, preferred_unit_id, status)
                        VALUES(?, ?, ?, 'APPROVED')
                        RETURNING id
                        """,
                        Long.class,
                        studentId,
                        periodId,
                        unitId);
        return jdbc.queryForObject(
                """
                INSERT INTO dormitory_contract(code, student_id, application_id, start_date, end_date,
                    activation_deadline, deposit_required, activated_at, status)
                VALUES(?, ?, ?, CURRENT_DATE, CURRENT_DATE + 180, now() + interval '7 days',
                    1000000, now(), 'ACTIVE')
                RETURNING id
                """,
                Long.class,
                "HD-" + suffix + "-" + contractSequence++,
                studentId,
                applicationId);
    }

    private Long createRoomTicket(Long targetRoomId, String status, String cause, Long repairCost) {
        return jdbc.queryForObject(
                """
                INSERT INTO maintenance_request(code, requester_id, room_id, category, severity,
                    description, status, cause, repair_cost)
                VALUES(?, ?, ?, 'WATER', 'MEDIUM', 'Hỏng vòi nước', ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                nextTicketCode(),
                studentIds.get(0),
                targetRoomId,
                status,
                cause,
                repairCost);
    }

    private Long createCommonTicket(String status) {
        return jdbc.queryForObject(
                """
                INSERT INTO maintenance_request(code, requester_id, building_id, floor, category,
                    severity, description, status)
                VALUES(?, ?, ?, 2, 'OTHER', 'LOW', 'Hỏng đèn hành lang', ?)
                RETURNING id
                """,
                Long.class,
                nextTicketCode(),
                studentIds.get(0),
                buildingId,
                status);
    }

    private String nextTicketCode() {
        return "TK-" + suffix + "-" + ticketSequence++;
    }

    private Long insertUser(String username, String role, Long unit, String fullName) {
        return jdbc.queryForObject(
                """
                INSERT INTO app_user(username, role, management_unit_id, full_name, password_hash)
                VALUES(?, ?, ?, ?, 'hash')
                RETURNING id
                """,
                Long.class,
                username,
                role,
                unit,
                fullName);
    }

    private Map<String, Object> ticketRow(Long id) {
        return jdbc.queryForMap(
                """
                SELECT cause, repair_cost, cost_note, cost_decided_by, status
                FROM maintenance_request WHERE id = ?
                """,
                id);
    }

    private List<Long> deductionAmounts(Long id) {
        return jdbc.queryForList(
                """
                SELECT amount FROM deposit_transaction
                WHERE maintenance_request_id = ? AND type = ? ORDER BY contract_id
                """,
                Long.class,
                id,
                DepositType.DEDUCTION);
    }

    private Long deductionCount(Long id) {
        return jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM deposit_transaction
                WHERE maintenance_request_id = ? AND type = ?
                """,
                Long.class,
                id,
                DepositType.DEDUCTION);
    }

    private Long deductionCountForContract(Long id, Long contractId) {
        return jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM deposit_transaction
                WHERE maintenance_request_id = ? AND contract_id = ? AND type = ?
                """,
                Long.class,
                id,
                contractId,
                DepositType.DEDUCTION);
    }
}

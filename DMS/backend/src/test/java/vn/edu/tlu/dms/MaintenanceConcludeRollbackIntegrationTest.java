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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.DepositType;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Conclude;
import vn.edu.tlu.dms.maintenance.service.MaintenanceRequestService;
import vn.edu.tlu.dms.support.FutureSemesterFixture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kiểm thử rollback thật của luồng kết luận ticket: cố ý tạo một khoản trừ cọc trùng cho hợp đồng
 * thứ hai để lần deduct thứ hai lỗi, sau đó xác nhận khoản trừ thứ nhất, lịch sử và audit của
 * ticket đều bị rollback. Lớp này không bọc transaction của test vì cần giao dịch của Service thực
 * sự rollback; dữ liệu seed được dọn trong {@link #cleanup()}.
 *
 * <p>Cần Docker đang chạy; nếu môi trường không có Docker thì không chạy được lớp này.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MaintenanceConcludeRollbackIntegrationTest {

    @Autowired MaintenanceRequestService maintenance;

    @Autowired JdbcTemplate jdbc;

    @MockitoBean CurrentUser currentUser;

    private String suffix;

    private Long unitId;

    private Long staffId;

    private Long buildingId;

    private Long roomTypeId;

    private Long roomId;

    private Long semesterId;

    private Long periodId;

    private Long ticketId;

    private final List<Long> studentIds = new ArrayList<>();

    private final List<Long> applicationIds = new ArrayList<>();

    private final List<Long> contractIds = new ArrayList<>();

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
        roomId =
                jdbc.queryForObject(
                        "INSERT INTO room(building_id, room_type_id, code, floor, gender, status)"
                                + " VALUES(?, ?, '101', 1, 'M', 'ACTIVE') RETURNING id",
                        Long.class,
                        buildingId,
                        roomTypeId);
        semesterId =
                // Học kỳ cấp phát động (không trùng UNIQUE(academic_year, type), không chồng
                // daterange) và được dọn trong cleanup() vì lớp test không bọc transaction.
                FutureSemesterFixture.insert(jdbc, SemesterType.HK1, "Học kỳ 1 " + suffix);
        periodId =
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
            Long applicationId = createApplication(studentId);
            Long contractId = createContract(studentId, applicationId);
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
            applicationIds.add(applicationId);
            contractIds.add(contractId);
        }

        ticketId =
                jdbc.queryForObject(
                        """
                        INSERT INTO maintenance_request(code, requester_id, room_id, category,
                            severity, description, status)
                        VALUES(?, ?, ?, 'WATER', 'MEDIUM', 'Hỏng vòi nước', ?)
                        RETURNING id
                        """,
                        Long.class,
                        "TK-" + suffix,
                        studentIds.get(0),
                        roomId,
                        MaintenanceStatus.CLOSED);

        // Khoản trừ trùng cho hợp đồng thứ hai: deduct thứ nhất thành công, deduct thứ hai lỗi.
        jdbc.update(
                """
                INSERT INTO deposit_transaction(contract_id, type, amount, maintenance_request_id,
                    due_date, confirmed_by)
                VALUES(?, ?, -1, ?, CURRENT_DATE, ?)
                """,
                contractIds.get(1),
                DepositType.DEDUCTION,
                ticketId,
                staffId);

        when(currentUser.get())
                .thenReturn(new Actor(staffId, Role.STAFF, unitId, false, false));
    }

    @AfterEach
    void cleanup() {
        if (ticketId != null) {
            jdbc.update("DELETE FROM deposit_transaction WHERE maintenance_request_id = ?", ticketId);
            jdbc.update("DELETE FROM maintenance_request WHERE id = ?", ticketId);
        }
        if (!contractIds.isEmpty()) {
            for (Long contractId : contractIds) {
                jdbc.update(
                        "DELETE FROM room_assignment_history WHERE contract_id = ?", contractId);
            }
        }
        if (roomId != null) {
            jdbc.update("DELETE FROM bed WHERE room_id = ?", roomId);
        }
        for (Long contractId : contractIds) {
            jdbc.update("DELETE FROM dormitory_contract WHERE id = ?", contractId);
        }
        for (Long applicationId : applicationIds) {
            jdbc.update("DELETE FROM room_application WHERE id = ?", applicationId);
        }
        if (periodId != null) {
            jdbc.update("DELETE FROM registration_period WHERE id = ?", periodId);
        }
        if (semesterId != null) {
            jdbc.update("DELETE FROM semester WHERE id = ?", semesterId);
        }
        if (roomId != null) {
            jdbc.update("DELETE FROM room WHERE id = ?", roomId);
        }
        if (roomTypeId != null) {
            jdbc.update("DELETE FROM room_type WHERE id = ?", roomTypeId);
        }
        if (buildingId != null) {
            jdbc.update("DELETE FROM building WHERE id = ?", buildingId);
        }
        for (Long studentId : studentIds) {
            jdbc.update("DELETE FROM student_profile WHERE user_id = ?", studentId);
            jdbc.update("DELETE FROM app_user WHERE id = ?", studentId);
        }
        if (staffId != null) {
            jdbc.update("DELETE FROM app_user WHERE id = ?", staffId);
        }
    }

    @Test
    void failedDeductionRollsBackTicketAndEarlierDeductions() {
        assertThatThrownBy(
                        () ->
                                maintenance.conclude(
                                        ticketId,
                                        new Conclude("MISUSE", 100_000L, null, "Hỏng do sử dụng")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.DUPLICATE_DEDUCTION));

        // Chỉ còn khoản trừ trùng đã seed; khoản trừ thứ nhất và audit của nó đã rollback.
        assertThat(deductionAmounts(ticketId)).containsExactly(-1L);
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
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT COUNT(*) FROM status_history
                                WHERE entity_type = ? AND entity_id = ?
                                """,
                                Long.class,
                                EntityType.MAINTENANCE,
                                ticketId))
                .isZero();

        Map<String, Object> ticket =
                jdbc.queryForMap(
                        """
                        SELECT cause, repair_cost, cost_note, cost_decided_by, status
                        FROM maintenance_request WHERE id = ?
                        """,
                        ticketId);
        assertThat(ticket.get("cause")).isNull();
        assertThat(ticket.get("repair_cost")).isNull();
        assertThat(ticket.get("cost_decided_by")).isNull();
        assertThat(ticket.get("status")).isEqualTo(MaintenanceStatus.CLOSED);
    }

    private Long createStudent(String studentCode, String fullName) {
        Long studentId = insertUser(studentCode, Role.STUDENT, null, fullName);
        jdbc.update(
                "INSERT INTO student_profile(user_id, student_code, gender) VALUES(?, ?, 'M')",
                studentId,
                studentCode);
        return studentId;
    }

    private Long createApplication(Long studentId) {
        return jdbc.queryForObject(
                """
                INSERT INTO room_application(student_id, period_id, preferred_unit_id, status)
                VALUES(?, ?, ?, 'APPROVED')
                RETURNING id
                """,
                Long.class,
                studentId,
                periodId,
                unitId);
    }

    private Long createContract(Long studentId, Long applicationId) {
        return jdbc.queryForObject(
                """
                INSERT INTO dormitory_contract(code, student_id, application_id, start_date, end_date,
                    activation_deadline, deposit_required, activated_at, status)
                VALUES(?, ?, ?, CURRENT_DATE, CURRENT_DATE + 180, now() + interval '7 days',
                    1000000, now(), 'ACTIVE')
                RETURNING id
                """,
                Long.class,
                "HD-" + suffix + "-" + contractIds.size(),
                studentId,
                applicationId);
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
}

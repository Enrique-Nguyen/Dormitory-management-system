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

import vn.edu.tlu.dms.billing.service.DebtQuery;
import vn.edu.tlu.dms.billing.service.DepositService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Kiểm thử tích hợp cho sổ cọc (DepositService) và công nợ (DebtQuery) trên PostgreSQL thật.
 * Kiểm tra các kịch bản AT-14, AT-19, BR-10: thu cọc, trừ cọc, bù cọc, hoàn cọc và công nợ.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class DepositIntegrationTest {

    @Autowired DepositService depositService;

    @Autowired DebtQuery debtQuery;

    @Autowired JdbcTemplate jdbc;

    @MockitoBean CurrentUser currentUser;

    private Long staffId;
    private Long studentId;
    private Long unitId;
    private Long contractId;
    private Long ticketId;
    private Long semesterId;

    @BeforeEach
    void setup() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Lấy đơn vị quản lý K1 có sẵn
        unitId =
                jdbc.queryForObject(
                        "SELECT id FROM management_unit WHERE code = 'K1'", Long.class);

        // Tạo cán bộ BQL
        staffId =
                jdbc.queryForObject(
                        """
                        INSERT INTO app_user(username, role, management_unit_id, full_name, password_hash)
                        VALUES(?, 'STAFF', ?, 'Staff Test', 'hash')
                        RETURNING id
                        """,
                        Long.class,
                        "staff_" + suffix,
                        unitId);

        // Tạo sinh viên
        studentId =
                jdbc.queryForObject(
                        """
                        INSERT INTO app_user(username, role, full_name, password_hash)
                        VALUES(?, 'STUDENT', 'Student Test', 'hash')
                        RETURNING id
                        """,
                        Long.class,
                        "sv_" + suffix);

        jdbc.update(
                "INSERT INTO student_profile(user_id, student_code, gender) VALUES(?, ?, 'M')",
                studentId,
                suffix);

        // Tạo học kỳ và đợt đăng ký
        semesterId =
                jdbc.queryForObject(
                        """
                        INSERT INTO semester(academic_year, type, name, start_date, end_date)
                        VALUES('2099-2100', 'HK1', 'Học kỳ 1', '2099-09-01', '2099-12-31')
                        RETURNING id
                        """,
                        Long.class);

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

        Long appId =
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

        // Tạo hợp đồng
        contractId =
                jdbc.queryForObject(
                        """
                        INSERT INTO dormitory_contract(code, student_id, application_id, start_date, end_date, activation_deadline, deposit_required, activated_at, status)
                        VALUES(?, ?, ?, CURRENT_DATE, CURRENT_DATE + 180, now() + interval '7 days', 1000000, now(), 'ACTIVE')
                        RETURNING id
                        """,
                        Long.class,
                        "HD-" + suffix,
                        studentId,
                        appId);

        // Tạo tòa nhà và phòng
        Long buildingId =
                jdbc.queryForObject(
                        "INSERT INTO building(code, name, management_unit_id, deposit_amount) VALUES(?, ?, ?, 1000000) RETURNING id",
                        Long.class,
                        "B" + suffix,
                        "Building " + suffix,
                        unitId);

        Long roomTypeId =
                jdbc.queryForObject(
                        "INSERT INTO room_type(name, bed_count, price_main, price_summer) VALUES(?, 4, 1500000, 500000) RETURNING id",
                        Long.class,
                        "T4-" + suffix);

        Long roomId =
                jdbc.queryForObject(
                        "INSERT INTO room(building_id, room_type_id, code, floor, gender, status) VALUES(?, ?, '101', 1, 'M', 'ACTIVE') RETURNING id",
                        Long.class,
                        buildingId,
                        roomTypeId);

        // Tạo ticket sửa chữa
        ticketId =
                jdbc.queryForObject(
                        """
                        INSERT INTO maintenance_request(code, requester_id, room_id, category, severity, description, status, cause, repair_cost)
                        VALUES(?, ?, ?, 'WATER', 'MEDIUM', 'Hỏng vòi nước', 'CLOSED', 'MISUSE', 100000)
                        RETURNING id
                        """,
                        Long.class,
                        "TK-" + suffix,
                        studentId,
                        roomId);

        // Giả lập actor là Staff có quyền
        when(currentUser.get())
                .thenReturn(new Actor(staffId, Role.STAFF, unitId, false, false));
    }

    @Test
    void collectInitial_successAndDuplicateCheck() {
        // Thu cọc ban đầu 1.000.000 đ
        var tx = depositService.collectInitial(contractId, 1000000L, "Thu cọc khi ký hợp đồng");
        assertThat(tx).isNotNull();
        assertThat(tx.amount()).isEqualTo(1000000L);
        assertThat(tx.type()).isEqualTo("DEPOSIT");

        // Kiểm tra số dư cọc
        var balance = depositService.getBalance(contractId);
        assertThat(balance.balance()).isEqualTo(1000000L);
        assertThat(balance.topupNeeded()).isEqualTo(0L);

        // Thu cọc lần hai phải bị chặn (DUPLICATE_DEPOSIT)
        assertThatThrownBy(
                        () ->
                                depositService.collectInitial(
                                        contractId, 1000000L, "Thu lần 2"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deduct_successAndTopupNeeded() {
        // Thu cọc ban đầu
        depositService.collectInitial(contractId, 1000000L, "Thu ban đầu");

        // Trừ cọc 100.000 đ do làm hư vòi nước (AT-14)
        LocalDate dueDate = LocalDate.now().plusDays(7);
        var deductTx = depositService.deduct(contractId, ticketId, 1000000L / 10, dueDate, "Trừ cọc sửa vòi nước");
        assertThat(deductTx).isNotNull();
        assertThat(deductTx.amount()).isEqualTo(-100000L); // Lưu số âm
        assertThat(deductTx.type()).isEqualTo("DEDUCTION");

        // Số dư còn lại 900.000 đ, cần bù 100.000 đ
        var balance = depositService.getBalance(contractId);
        assertThat(balance.balance()).isEqualTo(900000L);
        assertThat(balance.topupNeeded()).isEqualTo(100000L);

        // Trừ lần 2 cùng ticketId trên hợp đồng này phải bị chặn
        assertThatThrownBy(
                        () ->
                                depositService.deduct(
                                        contractId, ticketId, 50000L, dueDate, "Trừ tiếp"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void topup_successRestoresBalance() {
        depositService.collectInitial(contractId, 1000000L, "Thu ban đầu");
        depositService.deduct(contractId, ticketId, 200000L, LocalDate.now().plusDays(7), "Trừ cọc");

        // Sinh viên nộp bù cọc 200.000 đ
        var topupTx = depositService.topup(contractId, 200000L, "Nộp bù cọc");
        assertThat(topupTx).isNotNull();
        assertThat(topupTx.amount()).isEqualTo(200000L);
        assertThat(topupTx.type()).isEqualTo("TOPUP");

        var balance = depositService.getBalance(contractId);
        assertThat(balance.balance()).isEqualTo(1000000L);
        assertThat(balance.topupNeeded()).isEqualTo(0L);
    }

    @Test
    void refund_cannotExceedBalance() {
        depositService.collectInitial(contractId, 1000000L, "Thu ban đầu");

        // Chuyển hợp đồng sang EXPIRED để hoàn cọc
        jdbc.update("UPDATE dormitory_contract SET status = 'EXPIRED' WHERE id = ?", contractId);

        // Hoàn cọc 1.500.000 đ (vượt quá 1.000.000 đ) phải bị từ chối
        assertThatThrownBy(() -> depositService.refund(contractId, 1500000L, "Hoàn quá mức"))
                .isInstanceOf(BusinessException.class);

        // Hoàn cọc đúng số dư 1.000.000 đ
        var refundTx = depositService.refund(contractId, 1000000L, "Hoàn cọc khi trả phòng");
        assertThat(refundTx).isNotNull();
        assertThat(refundTx.amount()).isEqualTo(-1000000L);
        assertThat(refundTx.type()).isEqualTo("REFUND");

        var balance = depositService.getBalance(contractId);
        assertThat(balance.balance()).isEqualTo(0L);
    }

    @Test
    void debtQuery_returnsAccurateDebts() {
        // Ban đầu chưa có hóa đơn UNPAID và chưa có cọc âm
        var debt = debtQuery.getStudentDebt(studentId);
        assertThat(debt.hasDebt()).isFalse();
        assertThat(debt.totalDebt()).isEqualTo(0L);

        // Tạo 1 hóa đơn UNPAID 1.500.000 đ
        jdbc.update(
                """
                INSERT INTO invoice(code, type, student_id, semester_id, total_amount, due_date, status, issued_by)
                VALUES('HP-TEST', 'ROOM_FEE', ?, ?, 1500000, CURRENT_DATE + 7, 'UNPAID', ?)
                """,
                studentId,
                semesterId,
                staffId);

        debt = debtQuery.getStudentDebt(studentId);
        assertThat(debt.hasDebt()).isTrue();
        assertThat(debt.unpaidRoomFee()).isEqualTo(1500000L);
        assertThat(debt.totalDebt()).isEqualTo(1500000L);
    }
}

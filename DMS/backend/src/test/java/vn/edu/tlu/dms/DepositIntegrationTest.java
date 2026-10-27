package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.service.DebtQuery;
import vn.edu.tlu.dms.billing.service.DepositService;
import vn.edu.tlu.dms.billing.service.InvoiceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.notification.service.DashboardService;
import vn.edu.tlu.dms.support.FutureSemesterFixture;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Kiểm thử tích hợp cho sổ cọc (DepositService) và công nợ (DebtQuery) trên PostgreSQL thật. Kiểm
 * tra các kịch bản AT-14, AT-19, BR-10: thu cọc, trừ cọc, bù cọc, hoàn cọc và công nợ.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class DepositIntegrationTest {

    @Autowired DepositService depositService;

    @Autowired DebtQuery debtQuery;
    @Autowired InvoiceService invoiceService;
    @Autowired DashboardService dashboardService;
    @Autowired ObjectMapper mapper;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;

    @Autowired JdbcTemplate jdbc;

    @MockitoBean CurrentUser currentUser;

    private Long staffId;
    private Long studentId;
    private Long unitId;
    private Long contractId;
    private Long ticketId;
    private Long semesterId;
    private Long roomId;
    private String suffix;

    @BeforeEach
    void setup() {
        suffix = UUID.randomUUID().toString().substring(0, 8);

        // Đơn vị riêng cho test: các phép đếm theo phạm vi đơn vị (phát hành hóa đơn, điện nước,
        // dashboard) chỉ thấy đúng dữ liệu do test tạo, kể cả khi CSDL local đã có dữ liệu mẫu.
        unitId =
                jdbc.queryForObject(
                        "INSERT INTO management_unit(code, name) VALUES(?, ?) RETURNING id",
                        Long.class,
                        "U" + suffix,
                        "Đơn vị test " + suffix);

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

        // Tạo học kỳ và đợt đăng ký. Học kỳ cấp phát động để không trùng
        // UNIQUE(academic_year, type) và không chồng daterange với học kỳ đã tồn tại.
        semesterId = FutureSemesterFixture.insert(jdbc, SemesterType.HK1, "Học kỳ 1 " + suffix);

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
                        "INSERT INTO building(code, name, management_unit_id, deposit_amount)"
                            + " VALUES(?, ?, ?, 1000000) RETURNING id",
                        Long.class,
                        "B" + suffix,
                        "Building " + suffix,
                        unitId);

        Long roomTypeId =
                jdbc.queryForObject(
                        "INSERT INTO room_type(name, bed_count, price_main, price_summer) VALUES(?,"
                            + " 4, 1500000, 500000) RETURNING id",
                        Long.class,
                        "T4-" + suffix);

        roomId =
                jdbc.queryForObject(
                        "INSERT INTO room(building_id, room_type_id, code, floor, gender, status)"
                            + " VALUES(?, ?, '101', 1, 'M', 'ACTIVE') RETURNING id",
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
        when(currentUser.get()).thenReturn(new Actor(staffId, Role.STAFF, unitId, false, false));
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
        assertThatThrownBy(() -> depositService.collectInitial(contractId, 1000000L, "Thu lần 2"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_DEPOSIT);
    }

    @Test
    void deduct_successAndTopupNeeded() {
        // Thu cọc ban đầu
        depositService.collectInitial(contractId, 1000000L, "Thu ban đầu");

        // Trừ cọc 100.000 đ do làm hư vòi nước (AT-14)
        LocalDate dueDate = LocalDate.now().plusDays(7);
        var deductTx =
                depositService.deduct(
                        contractId, ticketId, 1000000L / 10, dueDate, "Trừ cọc sửa vòi nước");
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
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_DEDUCTION);
    }

    @Test
    void topup_successRestoresBalance() {
        depositService.collectInitial(contractId, 1000000L, "Thu ban đầu");
        depositService.deduct(
                contractId, ticketId, 200000L, LocalDate.now().plusDays(7), "Trừ cọc");

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
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFUND_EXCEEDS_BALANCE);

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

    @Test
    void initialDepositRequiresCorrectAmountAndLiveContract() {
        assertThatThrownBy(() -> depositService.collectInitial(contractId, 500000L, null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DEPOSIT_INVALID);
        jdbc.update("UPDATE dormitory_contract SET status='CANCELLED' WHERE id=?", contractId);
        assertThatThrownBy(() -> depositService.collectInitial(contractId, 1000000L, null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DEPOSIT_INVALID);
    }

    @Test
    void pendingContractAppearsInPageAndCanCollectDeposit() throws Exception {
        jdbc.update(
                "UPDATE dormitory_contract SET status='PENDING',activated_at=NULL WHERE id=?",
                contractId);
        var input =
                mapper.readValue(
                        "{\"q\":\""
                                + jdbc.queryForObject(
                                        "SELECT code FROM dormitory_contract WHERE id=?",
                                        String.class,
                                        contractId)
                                + "\"}",
                        SearchRequest.class);
        var page = depositService.searchBalancesByPage(input, false);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content())
                .singleElement()
                .satisfies(
                        balance -> {
                            assertThat(balance.contractId()).isEqualTo(contractId);
                            assertThat(balance.status()).isEqualTo("PENDING");
                            assertThat(balance.contractCode()).isNotBlank();
                            assertThat(balance.studentName()).isEqualTo("Student Test");
                        });
        depositService.collectInitial(contractId, 1000000L, null);
        assertThat(depositService.getBalance(contractId).balance()).isEqualTo(1000000L);
    }

    @Test
    void studentCannotReadAnotherStudentsLedger() {
        when(currentUser.get())
                .thenReturn(new Actor(studentId + 100000, Role.STUDENT, null, false, false));
        assertThatThrownBy(() -> depositService.getBalance(contractId))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
        assertThatThrownBy(() -> depositService.listTransactions(contractId))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
        assertThat(depositService.searchBalancesByPage(new SearchRequest(), true).totalElements())
                .isZero();
    }

    @Test
    void aggregateStaffCanReadButCannotWriteAnotherUnitsLedger() {
        when(currentUser.get())
                .thenReturn(new Actor(staffId, Role.STAFF, unitId + 100000, true, false));
        assertThat(depositService.getBalance(contractId).contractId()).isEqualTo(contractId);
        assertThatThrownBy(() -> depositService.collectInitial(contractId, 1000000L, null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void staffWithoutAggregateCannotReadAnotherUnitsLedger() {
        when(currentUser.get())
                .thenReturn(new Actor(staffId, Role.STAFF, unitId + 100000, false, false));
        assertThatThrownBy(() -> depositService.getBalance(contractId))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
        assertThat(depositService.searchBalancesByPage(new SearchRequest(), false).totalElements())
                .isZero();
    }

    @Test
    void refundRejectsActiveContractAndAllowsTerminatedContract() {
        depositService.collectInitial(contractId, 1000000L, null);
        assertThatThrownBy(() -> depositService.refund(contractId, 1000000L, null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATE_TRANSITION);
        jdbc.update("UPDATE dormitory_contract SET status='TERMINATED' WHERE id=?", contractId);
        depositService.refund(contractId, 1000000L, null);
        assertThatThrownBy(() -> depositService.refund(contractId, 1L, null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_REFUND);
    }

    @Test
    void missingTicketKeepsIntegrityErrorInsteadOfReportingDuplicate() {
        assertThatThrownBy(
                        () ->
                                depositService.deduct(
                                        contractId, Long.MAX_VALUE, 1L, LocalDate.now(), null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void roomFeeUsesConfiguredDueDateAndWritesAuditWithoutDuplicating() {
        prepareStay();
        jdbc.update("UPDATE system_config SET value='11' WHERE key='invoice.due_days'");
        var first = invoiceService.issueRoomFee(semesterId);
        var second = invoiceService.issueRoomFee(semesterId);
        assertThat(first.totalCreated()).isEqualTo(1);
        assertThat(first.totalAmount()).isEqualTo(1500000L);
        assertThat(second.totalCreated()).isZero();
        assertThat(second.totalSkipped()).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT due_date FROM invoice WHERE student_id=?",
                                LocalDate.class,
                                studentId))
                .isEqualTo(LocalDate.now().plusDays(11));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE actor_id=? AND"
                                    + " action='INVOICE_BATCH_ISSUE'",
                                Long.class,
                                staffId))
                .isEqualTo(2);
    }

    @Test
    void aggregateReadDoesNotExpandInvoiceWriteScope() {
        prepareStay();
        when(currentUser.get())
                .thenReturn(new Actor(staffId, Role.STAFF, unitId + 100000, true, false));
        assertThat(invoiceService.issueRoomFee(semesterId).totalCreated()).isZero();
        assertThatThrownBy(
                        () ->
                                invoiceService.issueUtility(
                                        unitId, LocalDate.now().withDayOfMonth(1)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void futureSemesterCannotBeIssued() {
        assertThatThrownBy(() -> invoiceService.issueRoomFee(semesterId))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void summerFeeRequiresApprovedStayChoice() {
        prepareStay();
        // Học kỳ hè riêng cấp phát động (thay vì đổi loại của học kỳ fixture) để không đụng
        // UNIQUE(academic_year, type) với học kỳ hè đã có trong CSDL.
        Long summerId =
                FutureSemesterFixture.insert(jdbc, SemesterType.SUMMER, "Học kỳ hè " + suffix);
        jdbc.update(
                "UPDATE semester SET start_date=CURRENT_DATE-10,end_date=CURRENT_DATE+30 WHERE id=?",
                summerId);
        assertThat(invoiceService.issueRoomFee(summerId).totalCreated()).isZero();
        jdbc.update(
                "INSERT INTO summer_choice(contract_id,semester_id,choice,status)"
                    + " VALUES(?,?,'STAY','APPROVED')",
                contractId,
                summerId);
        assertThat(invoiceService.issueRoomFee(summerId).totalAmount()).isEqualTo(500000L);
    }

    @Test
    void utilitySkipsMissingReadingThenIssuesUsingSnapshotRates() {
        prepareStay();
        prepareReadings(false);
        var missing = invoiceService.issueUtility(unitId, billingMonth());
        assertThat(missing.totalCreated()).isZero();
        assertThat(missing.totalSkipped()).isEqualTo(1);
        addWaterReading();
        var issued = invoiceService.issueUtility(unitId, billingMonth());
        assertThat(issued.totalCreated()).isEqualTo(1);
        assertThat(issued.totalAmount()).isEqualTo(50000L);
        assertThat(invoiceService.issueUtility(unitId, billingMonth()).totalSkipped()).isEqualTo(1);
    }

    @Test
    void dashboardSeparatesHoldsAndNonOperationalBedsAndUsesConfig() {
        prepareStay();
        jdbc.update("UPDATE system_config SET value='3' WHERE key='contract.expiring_days'");
        jdbc.update("UPDATE dormitory_contract SET end_date=CURRENT_DATE+5 WHERE id=?", contractId);
        var first = dashboardService.getSummary(unitId);
        assertThat(first.capacity().operationalBeds()).isEqualTo(1);
        assertThat(first.capacity().occupiedBeds()).isEqualTo(1);
        assertThat(first.capacity().occupancyRate()).isEqualTo(100.0);
        assertThat(first.operations().contractsExpiringSoon()).isZero();
        jdbc.update("UPDATE system_config SET value='8' WHERE key='contract.expiring_days'");
        entityManager.clear();
        assertThat(dashboardService.getSummary(unitId).operations().contractsExpiringSoon())
                .isEqualTo(1);
        jdbc.update(
                "UPDATE room_assignment_history SET kind='HOLD',stay_started_at=NULL WHERE"
                    + " contract_id=?",
                contractId);
        assertThat(dashboardService.getSummary(unitId).capacity().occupiedBeds()).isZero();
        assertThat(dashboardService.getSummary(unitId).capacity().availableBeds()).isZero();
        jdbc.update("UPDATE room SET status='MAINTENANCE' WHERE id=?", roomId);
        var maintenance = dashboardService.getSummary(unitId).capacity();
        assertThat(maintenance.operationalBeds()).isZero();
        assertThat(maintenance.occupiedBeds()).isZero();
        assertThat(maintenance.maintenanceBeds()).isEqualTo(1);
    }

    @Test
    void dashboardRoomDebtStaysWithInvoiceSemesterAfterStudentLeaves() {
        prepareStay();
        invoiceService.issueRoomFee(semesterId);
        jdbc.update(
                "UPDATE room_assignment_history SET"
                    + " ended_at=CURRENT_TIMESTAMP,end_reason='MOVE_OUT' WHERE contract_id=?",
                contractId);
        assertThat(dashboardService.getSummary(unitId).finance().unpaidRoomFeeAmount())
                .isEqualTo(1500000L);
    }

    private LocalDate billingMonth() {
        // Tháng điện nước đặt ở tương lai để không đụng UNIQUE(meter_type, effective_from) của
        // utility_rate và các hóa đơn UTILITY đã lập theo tháng hiện tại khi CSDL có dữ liệu mẫu.
        // Đơn giá test chèn vẫn là đơn giá mới nhất áp dụng cho tháng này.
        return LocalDate.now().withDayOfMonth(1).plusMonths(24);
    }

    private void prepareStay() {
        // issueRoomFee chỉ chạy khi học kỳ bao trùm CURRENT_DATE. Trên CSDL đã có học kỳ đang chạy
        // (dữ liệu mẫu) thì kéo học kỳ của test về hôm nay sẽ phạm ràng buộc chồng daterange, nên
        // ràng buộc được gỡ trong transaction của test và tự phục hồi khi rollback.
        jdbc.execute("ALTER TABLE semester DROP CONSTRAINT semester_daterange_excl");
        jdbc.update(
                "UPDATE semester SET start_date=CURRENT_DATE-10,end_date=CURRENT_DATE+30 WHERE"
                    + " id=?",
                semesterId);
        Long bedId =
                jdbc.queryForObject(
                        "INSERT INTO bed(room_id,code) VALUES(?,'01') RETURNING id",
                        Long.class,
                        roomId);
        jdbc.update(
                """
                INSERT INTO room_assignment_history(student_id,contract_id,bed_id,kind,stay_started_at,price_main,price_summer,created_by)
                VALUES(?,?,?,'STAY',CURRENT_TIMESTAMP,1500000,500000,?)
                """,
                studentId,
                contractId,
                bedId,
                staffId);
    }

    private void prepareReadings(boolean water) {
        jdbc.update(
                "INSERT INTO utility_rate(meter_type,unit_price,effective_from,created_by)"
                    + " VALUES('ELECTRIC',3000,?,?),('WATER',10000,?,?)",
                billingMonth(),
                staffId,
                billingMonth(),
                staffId);
        jdbc.update(
                "INSERT INTO"
                    + " utility_reading(room_id,meter_type,billing_month,start_reading,end_reading,recorded_by)"
                    + " VALUES(?,'ELECTRIC',?,0,10,?)",
                roomId,
                billingMonth(),
                staffId);
        if (water) addWaterReading();
    }

    private void addWaterReading() {
        jdbc.update(
                "INSERT INTO"
                    + " utility_reading(room_id,meter_type,billing_month,start_reading,end_reading,recorded_by)"
                    + " VALUES(?,'WATER',?,0,2,?)",
                roomId,
                billingMonth(),
                staffId);
    }
}

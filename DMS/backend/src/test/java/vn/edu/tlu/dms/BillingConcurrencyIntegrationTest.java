package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import vn.edu.tlu.dms.billing.api.InvoiceDtos;
import vn.edu.tlu.dms.billing.service.InvoiceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** PostgreSQL riêng cho kiểm thử đồng thời; giữ nguyên audit chỉ ghi thêm đến khi hủy container. */
@SpringBootTest(properties = "dms.test.billing-concurrency=true")
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BillingConcurrencyIntegrationTest {
    @Autowired InvoiceService invoiceService;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean CurrentUser currentUser;
    private Long staffId, studentId, unitId, contractId, semesterId, roomId;

    @BeforeEach
    void setup() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Lấy đơn vị quản lý K1 có sẵn
        unitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code = 'K1'", Long.class);

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

        // Giả lập actor là Staff có quyền
        when(currentUser.get()).thenReturn(new Actor(staffId, Role.STAFF, unitId, false, false));
    }

    @Test
    void simultaneousBatchRequestsDoNotDuplicateOrRollbackOtherInvoices() throws Exception {
        prepareStay();
        prepareReadings(true);
        assertConcurrentIssue(() -> invoiceService.issueRoomFee(semesterId));
        assertConcurrentIssue(() -> invoiceService.issueUtility(unitId, billingMonth()));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM invoice WHERE issued_by=?",
                                Long.class,
                                staffId))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE actor_id=? AND"
                                    + " action='INVOICE_BATCH_ISSUE'",
                                Long.class,
                                staffId))
                .isEqualTo(4);
    }

    private LocalDate billingMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }

    private void prepareStay() {
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

    private void assertConcurrentIssue(Supplier<InvoiceDtos.BatchIssueResult> issue)
            throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var first =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();
                                return issue.get();
                            });
            var second =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();
                                return issue.get();
                            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var a = first.get(20, TimeUnit.SECONDS);
            var b = second.get(20, TimeUnit.SECONDS);
            assertThat(a.totalCreated() + b.totalCreated()).isEqualTo(1);
            assertThat(a.totalSkipped() + b.totalSkipped()).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(20, TimeUnit.SECONDS);
        }
    }
}

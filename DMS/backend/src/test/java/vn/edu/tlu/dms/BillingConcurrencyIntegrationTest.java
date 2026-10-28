package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
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
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.support.FutureSemesterFixture;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
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
    private Long createdSemesterId;
    private String suffix;

    @BeforeEach
    void setup() {
        this.suffix = UUID.randomUUID().toString().substring(0, 8);

        // Đơn vị riêng cho test: phát hành hàng loạt chỉ thấy dữ liệu do test tạo trên CSDL local có mẫu.
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

        // Tạo học kỳ và đợt đăng ký
        semesterId =
                FutureSemesterFixture.insert(
                        jdbc, SemesterType.HK1, "Học kỳ " + suffix, 121);
        createdSemesterId = semesterId;

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

    @AfterEach
    void cleanupTestData() {
        if (staffId == null || studentId == null || unitId == null) {
            return;
        }
        jdbc.update("DELETE FROM invoice_item WHERE invoice_id IN (SELECT id FROM invoice WHERE issued_by=?)", staffId);
        jdbc.update("DELETE FROM invoice WHERE issued_by=?", staffId);
        jdbc.update("DELETE FROM utility_reading WHERE recorded_by=?", staffId);
        jdbc.update("DELETE FROM utility_rate WHERE created_by=?", staffId);
        jdbc.update("DELETE FROM room_assignment_history WHERE contract_id=?", contractId);
        jdbc.update("DELETE FROM maintenance_request WHERE requester_id=?", studentId);
        jdbc.update("DELETE FROM dormitory_contract WHERE id=?", contractId);
        jdbc.update("DELETE FROM room_application WHERE student_id=?", studentId);
        jdbc.update("DELETE FROM registration_period WHERE created_by=?", staffId);
        jdbc.update("DELETE FROM semester WHERE id=?", createdSemesterId);
        if (roomId != null) {
            jdbc.update("DELETE FROM bed WHERE room_id=?", roomId);
            jdbc.update("DELETE FROM room WHERE id=?", roomId);
        }
        jdbc.update("DELETE FROM building WHERE management_unit_id=?", unitId);
        jdbc.update("DELETE FROM room_type WHERE name LIKE ?", "T4-" + suffix);
        jdbc.update("DELETE FROM notification_read WHERE user_id IN (?, ?)", staffId, studentId);
        jdbc.update("DELETE FROM notification WHERE user_id IN (?, ?)", staffId, studentId);
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
        return LocalDate.now().plusYears(10).withDayOfMonth(1);
    }

    private void prepareStay() {
        List<Long> currentSemesters =
                jdbc.queryForList(
                        "SELECT id FROM semester WHERE start_date <= CURRENT_DATE"
                                + " AND end_date >= CURRENT_DATE ORDER BY id",
                        Long.class);
        if (!currentSemesters.isEmpty()) {
            semesterId = currentSemesters.get(0);
        } else {
            jdbc.update(
                    "UPDATE semester SET start_date=CURRENT_DATE-10,end_date=CURRENT_DATE+30 WHERE"
                        + " id=?",
                    semesterId);
        }
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
                    + " VALUES('ELECTRIC',3000,?,?),('WATER',10000,?,?)"
                    + " ON CONFLICT (meter_type,effective_from) DO UPDATE SET unit_price=EXCLUDED.unit_price, created_by=EXCLUDED.created_by",
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

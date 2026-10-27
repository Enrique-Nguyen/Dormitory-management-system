package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.edu.tlu.dms.billing.service.DepositReminderLookup;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.constants.DmsConstants.SystemJob;
import vn.edu.tlu.dms.support.FutureSemesterFixture;
import vn.edu.tlu.dms.common.service.SystemJobService;
import vn.edu.tlu.dms.common.service.SystemJobService.Change;
import vn.edu.tlu.dms.contract.service.ContractLifecycleService;
import vn.edu.tlu.dms.maintenance.service.MaintenanceLifecycleService;
import vn.edu.tlu.dms.notification.service.SystemReminderService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Transaction commit thật trên container riêng, audit thật append-only, không gọi Keycloak. */
@SpringBootTest(properties = "dms.jobs.enabled=false")
@Import(TestcontainersConfiguration.class)
class SystemJobsIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired SystemJobService jobs;
    @Autowired ContractLifecycleService contracts;
    @Autowired MaintenanceLifecycleService maintenance;
    @Autowired SystemReminderService reminders;
    @Autowired DepositReminderLookup deposits;

    private static final AtomicInteger YEARS = new AtomicInteger(2900);
    private String suffix;
    private Long actor;
    private Long tech;
    private Long unit;
    private Long building;
    private Long room;
    private Long bed;
    private Long student;
    private Long application;
    private Long contract;

    @BeforeEach
    void fixture() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        unit = jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        actor =
                id(
                        "INSERT INTO app_user(username,full_name,role) VALUES(?,'Job"
                                + " admin','ADMIN') RETURNING id",
                        "j-admin-" + suffix);
        tech =
                id(
                        "INSERT INTO app_user(username,full_name,role,management_unit_id)"
                                + " VALUES(?,'Job tech','TECHNICIAN',?) RETURNING id",
                        "j-tech-" + suffix,
                        unit);
        student =
                id(
                        "INSERT INTO app_user(username,full_name,role) VALUES(?,'Job"
                                + " student','STUDENT') RETURNING id",
                        "j-student-" + suffix);
        jdbc.update(
                "INSERT INTO student_profile(user_id,student_code,gender) VALUES(?,?,'M')",
                student,
                "J-" + suffix);
        Long type =
                id(
                        "INSERT INTO room_type(name,bed_count,price_main,price_summer)"
                                + " VALUES(?,2,1000000,300000) RETURNING id",
                        "Job " + suffix);
        building =
                id(
                        "INSERT INTO building(management_unit_id,code,name,deposit_amount)"
                                + " VALUES(?,?,'Job building',1000000) RETURNING id",
                        unit,
                        "J-" + suffix);
        room =
                id(
                        "INSERT INTO room(building_id,room_type_id,floor,code,gender)"
                                + " VALUES(?,?,1,'101','M') RETURNING id",
                        building,
                        type);
        bed = id("INSERT INTO bed(room_id,code) VALUES(?,'1') RETURNING id", room);
        Long semester =
                FutureSemesterFixture.insert(
                        jdbc, SemesterType.HK1, "Job " + suffix, 500);
        Long period =
                id(
                        "INSERT INTO"
                            + " registration_period(name,kind,semester_id,open_at,close_at,created_by)"
                            + " VALUES(?,'REGULAR',?,CURRENT_TIMESTAMP-INTERVAL '30"
                            + " day',CURRENT_TIMESTAMP+INTERVAL '30 day',?) RETURNING id",
                        "Job " + suffix,
                        semester,
                        actor);
        application =
                id(
                        "INSERT INTO"
                                + " room_application(student_id,period_id,preferred_unit_id,status)"
                                + " VALUES(?,?,?,'ASSIGNED') RETURNING id",
                        student,
                        period,
                        unit);
        contract =
                id(
                        "INSERT INTO"
                            + " dormitory_contract(code,student_id,application_id,start_date,end_date,status,activation_deadline,deposit_required)"
                            + " VALUES(?,?,?,CURRENT_DATE,CURRENT_DATE+30,'PENDING',CURRENT_TIMESTAMP-INTERVAL"
                            + " '1 day',1000000) RETURNING id",
                        "J-" + suffix,
                        student,
                        application);
        jdbc.update(
                "INSERT INTO"
                    + " room_assignment_history(student_id,contract_id,bed_id,kind,price_main,price_summer,created_by)"
                    + " VALUES(?,?,?,'HOLD',1000000,300000,?)",
                student,
                contract,
                bed,
                actor);
    }

    private Long id(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private void activate() {
        jdbc.update(
                "UPDATE dormitory_contract SET"
                        + " status='ACTIVE',activated_at=CURRENT_TIMESTAMP,end_date=CURRENT_DATE+5"
                        + " WHERE id=?",
                contract);
        jdbc.update(
                "UPDATE room_assignment_history SET kind='STAY',stay_started_at=CURRENT_TIMESTAMP"
                        + " WHERE contract_id=?",
                contract);
    }

    @Test
    void pendingWithoutHoldStillLocksStudentUntilTransactionCommits() {
        jdbc.update("DELETE FROM room_assignment_history WHERE contract_id=?", contract);
        var pool = Executors.newSingleThreadExecutor();
        try {
            var result =
                    jobs.run(
                            SystemJob.CANCEL_PENDING,
                            () -> List.of(contract),
                            id -> {
                                var change = contracts.cancelExpiredPending(id);
                                var blocked = pool.submit(this::isStudentLockBlocked);
                                try {
                                    assertThat(blocked.get(10, TimeUnit.SECONDS)).isTrue();
                                } catch (Exception error) {
                                    throw new IllegalStateException(error);
                                }
                                return change;
                            });
            assertThat(result.processed()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean isStudentLockBlocked() throws java.sql.SQLException {
        try (var connection = jdbc.getDataSource().getConnection()) {
            connection.setReadOnly(false);
            connection.setAutoCommit(false);
            try (var statement =
                    connection.prepareStatement(
                            "SELECT user_id FROM student_profile WHERE user_id=? FOR UPDATE"
                                + " NOWAIT")) {
                statement.setLong(1, student);
                statement.executeQuery();
                return false;
            } catch (java.sql.SQLException error) {
                if ("55P03".equals(error.getSQLState())) {
                    return true;
                }
                throw error;
            } finally {
                connection.rollback();
            }
        }
    }

    @Test
    void cancelsPendingReleasesHoldCancelsApplicationAndAuditsOnce() {
        assertThat(contracts.findExpiredPendingIds()).contains(contract);
        var first =
                jobs.run(
                        SystemJob.CANCEL_PENDING,
                        () -> List.of(contract),
                        contracts::cancelExpiredPending);
        var again =
                jobs.run(
                        SystemJob.CANCEL_PENDING,
                        () -> List.of(contract),
                        contracts::cancelExpiredPending);
        assertThat(first).isEqualTo(new SystemJobService.RunResult(1, 0, 0));
        assertThat(again).isEqualTo(new SystemJobService.RunResult(0, 1, 0));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contract))
                .isEqualTo("CANCELLED");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM room_application WHERE id=?",
                                String.class,
                                application))
                .isEqualTo("CANCELLED");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT end_reason FROM room_assignment_history WHERE"
                                        + " contract_id=?",
                                String.class,
                                contract))
                .isEqualTo("HOLD_EXPIRED");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM status_history WHERE entity_type='CONTRACT'"
                                        + " AND entity_id=?",
                                Long.class,
                                contract))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM audit_log WHERE action='CONTRACT_AUTO_CANCEL'"
                                    + " AND entity_id=? AND actor_id IS NULL AND request_id IS NOT"
                                    + " NULL",
                                Long.class,
                                contract))
                .isEqualTo(1);
    }

    @Test
    void rechecksAfterSelectingAndPreservesAnActivatedContract() {
        var result =
                jobs.run(
                        SystemJob.CANCEL_PENDING,
                        () -> {
                            var candidates = List.of(contract);
                            activate();
                            return candidates;
                        },
                        contracts::cancelExpiredPending);
        assertThat(result).isEqualTo(new SystemJobService.RunResult(0, 1, 0));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM room_assignment_history WHERE contract_id=?"
                                        + " AND ended_at IS NULL",
                                Long.class,
                                contract))
                .isEqualTo(1);
    }

    @Test
    void listenerFailureRollsBackReleaseHistoryAndContractTogether() {
        jdbc.update("UPDATE room_application SET status='SUBMITTED' WHERE id=?", application);
        var result =
                jobs.run(
                        SystemJob.CANCEL_PENDING,
                        () -> List.of(contract),
                        contracts::cancelExpiredPending);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contract))
                .isEqualTo("PENDING");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM room_assignment_history WHERE contract_id=?"
                                        + " AND ended_at IS NULL",
                                Long.class,
                                contract))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM audit_log WHERE entity_type='ASSIGNMENT' AND"
                                    + " entity_id IN (SELECT id FROM room_assignment_history WHERE"
                                    + " contract_id=?)",
                                Long.class,
                                contract))
                .isZero();
    }

    @Test
    void failureInOneTransactionDoesNotBlockNextIdAndRestoresCallerContext() {
        String firstKey = "job.first." + suffix;
        String nextKey = "job.next." + suffix;
        var auth = new TestingAuthenticationToken("caller", "unused");
        SecurityContextHolder.getContext().setAuthentication(auth);
        MDC.put("requestId", "previous");
        try {
            var result =
                    jobs.run(
                            "test-isolation",
                            () -> List.of(1L, 2L),
                            number -> {
                                assertThat(SecurityContextHolder.getContext().getAuthentication())
                                        .isNull();
                                jdbc.update(
                                        "INSERT INTO system_config(key,value) VALUES(?,'1')",
                                        number == 1 ? firstKey : nextKey);
                                if (number == 1) {
                                    throw new IllegalStateException("Synthetic failure");
                                }
                                return new Change("JOB_TEST", "CONFIG", null, null, number, null);
                            });
            assertThat(result).isEqualTo(new SystemJobService.RunResult(1, 0, 1));
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM system_config WHERE key=?",
                                    Long.class,
                                    firstKey))
                    .isZero();
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM system_config WHERE key=?",
                                    Long.class,
                                    nextKey))
                    .isEqualTo(1);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(auth);
            assertThat(MDC.get("requestId")).isEqualTo("previous");
        } finally {
            MDC.remove("requestId");
            SecurityContextHolder.clearContext();
        }
    }

    private Long ticket(String state, int days) {
        return id(
                "INSERT INTO"
                    + " maintenance_request(code,requester_id,building_id,floor,category,severity,description,status,assigned_to,resolved_at)"
                    + " VALUES(?,?,?,1,'ELECTRIC','MEDIUM','Job ticket',?,?,CURRENT_TIMESTAMP-(? *"
                    + " INTERVAL '1 day')) RETURNING id",
                "JT-" + UUID.randomUUID().toString().substring(0, 8),
                actor,
                building,
                state,
                tech,
                days);
    }

    @Test
    void closesOnlyResolvedOlderThanConfiguredDeadlineAndIsRepeatable() {
        Long old = ticket("RESOLVED", 4);
        Long recent = ticket("RESOLVED", 2);
        Long reopened = ticket("IN_PROGRESS", 4);
        assertThat(maintenance.findAutoCloseIds()).contains(old).doesNotContain(recent, reopened);
        var first =
                jobs.run(
                        SystemJob.CLOSE_RESOLVED,
                        () -> List.of(old, recent, reopened),
                        maintenance::closeResolved);
        var again =
                jobs.run(SystemJob.CLOSE_RESOLVED, () -> List.of(old), maintenance::closeResolved);
        assertThat(first).isEqualTo(new SystemJobService.RunResult(1, 2, 0));
        assertThat(again.processed()).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM maintenance_request WHERE id=?",
                                String.class,
                                old))
                .isEqualTo("CLOSED");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM status_history WHERE"
                                    + " entity_type='MAINTENANCE' AND entity_id=? AND actor_id IS"
                                    + " NULL",
                                Long.class,
                                old))
                .isEqualTo(1);
    }

    @Test
    void contractRemindersArePerEndDateAndConcurrentRunsDoNotDuplicate() throws Exception {
        activate();
        assertThat(contracts.findExpiringIds()).contains(contract);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a =
                    pool.submit(
                            () ->
                                    jobs.run(
                                            SystemJob.REMIND_CONTRACT,
                                            () -> List.of(contract),
                                            reminders::remindExpiringContract));
            var b =
                    pool.submit(
                            () ->
                                    jobs.run(
                                            SystemJob.REMIND_CONTRACT,
                                            () -> List.of(contract),
                                            reminders::remindExpiringContract));
            assertThat(
                            a.get(15, TimeUnit.SECONDS).processed()
                                    + b.get(15, TimeUnit.SECONDS).processed())
                    .isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM notification WHERE"
                                        + " source_type='CONTRACT_EXPIRING' AND source_id=?",
                                Long.class,
                                contract))
                .isEqualTo(1);
        jdbc.update("UPDATE dormitory_contract SET end_date=CURRENT_DATE+7 WHERE id=?", contract);
        jobs.run(
                SystemJob.REMIND_CONTRACT,
                () -> List.of(contract),
                reminders::remindExpiringContract);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM notification WHERE"
                                        + " source_type='CONTRACT_EXPIRING' AND source_id=?",
                                Long.class,
                                contract))
                .isEqualTo(2);
    }

    @Test
    void depositReminderUsesFifoSkipsRepaidDeductionAndDoesNotDuplicate() {
        activate();
        jdbc.update(
                "INSERT INTO deposit_transaction(contract_id,type,amount,confirmed_by)"
                        + " VALUES(?,'DEPOSIT',1000000,?)",
                contract,
                actor);
        Long firstTicket = ticket("CLOSED", 4);
        Long secondTicket = ticket("CLOSED", 4);
        Long first =
                id(
                        "INSERT INTO"
                            + " deposit_transaction(contract_id,type,amount,maintenance_request_id,due_date)"
                            + " VALUES(?,'DEDUCTION',-100000,?,CURRENT_DATE-3) RETURNING id",
                        contract,
                        firstTicket);
        Long second =
                id(
                        "INSERT INTO"
                            + " deposit_transaction(contract_id,type,amount,maintenance_request_id,due_date)"
                            + " VALUES(?,'DEDUCTION',-100000,?,CURRENT_DATE-2) RETURNING id",
                        contract,
                        secondTicket);
        jdbc.update(
                "INSERT INTO deposit_transaction(contract_id,type,amount,confirmed_by)"
                        + " VALUES(?,'TOPUP',150000,?)",
                contract,
                actor);
        assertThat(deposits.findOverdueContractIds()).contains(contract);
        var initial =
                jobs.run(
                        SystemJob.REMIND_DEPOSIT,
                        () -> List.of(contract),
                        reminders::remindOverdueDeposit);
        var again =
                jobs.run(
                        SystemJob.REMIND_DEPOSIT,
                        () -> List.of(contract),
                        reminders::remindOverdueDeposit);
        assertThat(initial.processed()).isEqualTo(1);
        assertThat(again.skipped()).isEqualTo(1);
        assertThat(
                        jdbc.queryForList(
                                "SELECT source_id FROM notification WHERE user_id=? AND"
                                        + " source_type='DEPOSIT_OVERDUE'",
                                Long.class,
                                student))
                .containsExactly(second)
                .doesNotContain(first);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT body FROM notification WHERE user_id=? AND"
                                        + " source_type='DEPOSIT_OVERDUE'",
                                String.class,
                                student))
                .contains("50000");
        jdbc.update(
                "INSERT INTO deposit_transaction(contract_id,type,amount,confirmed_by)"
                        + " VALUES(?,'TOPUP',50000,?)",
                contract,
                actor);
        assertThat(deposits.findOverdueContractIds()).doesNotContain(contract);
    }
}

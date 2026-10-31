package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import vn.edu.tlu.dms.billing.service.DepositReminderLookup;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentEndReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.KeycloakClaim;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.constants.DmsConstants.SystemJob;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.service.SystemJobService;
import vn.edu.tlu.dms.common.service.SystemJobService.Change;
import vn.edu.tlu.dms.contract.service.ContractLifecycleService;
import vn.edu.tlu.dms.maintenance.service.MaintenanceLifecycleService;
import vn.edu.tlu.dms.notification.service.SystemReminderService;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Response;
import vn.edu.tlu.dms.registration.service.RoomApplicationService;
import vn.edu.tlu.dms.support.FutureSemesterFixture;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

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
    @Autowired RoomApplicationService applications;
    @Autowired PlatformTransactionManager transactionManager;

    private static final AtomicInteger YEARS = new AtomicInteger(2900);
    private static final String RELEASE_REASON = "Release hold for reassignment";
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
                                var studentBlocked =
                                        pool.submit(
                                                () ->
                                                        isLockBlocked(
                                                                "SELECT user_id FROM student_profile"
                                                                        + " WHERE user_id=? FOR UPDATE NOWAIT",
                                                                student));
                                var applicationBlocked =
                                        pool.submit(
                                                () ->
                                                        isLockBlocked(
                                                                "SELECT id FROM room_application WHERE"
                                                                        + " id=? FOR UPDATE NOWAIT",
                                                                application));
                                try {
                                    assertThat(studentBlocked.get(10, TimeUnit.SECONDS)).isTrue();
                                    assertThat(applicationBlocked.get(10, TimeUnit.SECONDS)).isTrue();
                                } catch (Exception error) {
                                    throw new IllegalStateException(error);
                                }
                                return change;
                            });
            assertThat(result).isEqualTo(new SystemJobService.RunResult(1, 0, 0));
            assertThat(contractStatus()).isEqualTo(ContractStatus.CANCELLED);
            assertThat(applicationStatus()).isEqualTo(ApplicationStatus.CANCELLED);
            assertThat(count("SELECT count(*) FROM room_assignment_history WHERE contract_id=?", contract))
                    .isZero();
            assertThat(historyCount(EntityType.CONTRACT, contract)).isEqualTo(1);
            assertThat(historyCount(EntityType.APPLICATION, application)).isEqualTo(1);
            assertThat(auditCount(AuditAction.CONTRACT_AUTO_CANCEL, EntityType.CONTRACT, contract, null))
                    .isEqualTo(1);
            assertThat(auditCount(AuditAction.APPLICATION_AUTO_CANCEL, EntityType.APPLICATION, application, null))
                    .isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean isLockBlocked(String sql, Long id) throws SQLException {
        try (var connection = jdbc.getDataSource().getConnection()) {
            connection.setReadOnly(false);
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                statement.executeQuery();
                return false;
            } catch (SQLException error) {
                if ("55P03".equals(error.getSQLState())) {
                    return true;
                }
                throw error;
            } finally {
                connection.rollback();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void interactiveCancellationWinsBeforeExpiryLocksResources(boolean cancelApplication)
            throws Exception {
        var jwt = adminJwt();
        var manualBackend = new CompletableFuture<Integer>();
        var jobBackend = new CompletableFuture<Integer>();
        var allowManual = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var manual =
                    pool.submit(
                            () ->
                                    asAdmin(
                                            jwt,
                                            () ->
                                                    new TransactionTemplate(transactionManager)
                                                            .execute(
                                                                    transaction -> {
                                                                        jdbc.queryForObject(
                                                                                "SELECT id FROM room_application"
                                                                                        + " WHERE id=? FOR UPDATE",
                                                                                Long.class,
                                                                                application);
                                                                        manualBackend.complete(backendPid());
                                                                        awaitLatch(allowManual);
                                                                        return cancelOrRelease(cancelApplication);
                                                                    })));
            int manualPid = manualBackend.get(10, TimeUnit.SECONDS);
            var job =
                    pool.submit(
                            () ->
                                    jobs.run(
                                            SystemJob.CANCEL_PENDING,
                                            () -> List.of(contract),
                                            id -> {
                                                jobBackend.complete(backendPid());
                                                return contracts.cancelExpiredPending(id);
                                            }));
            int jobPid = jobBackend.get(10, TimeUnit.SECONDS);
            awaitBlockedBy(jobPid, manualPid);
            assertWaitingOnApplication(jobPid);
            assertThat(
                            isLockBlocked(
                                    "SELECT id FROM room_application WHERE id=? FOR UPDATE NOWAIT",
                                    application))
                    .isTrue();
            assertThat(isLockBlocked("SELECT id FROM bed WHERE id=? FOR UPDATE NOWAIT", bed))
                    .isFalse();
            assertThat(
                            isLockBlocked(
                                    "SELECT user_id FROM student_profile WHERE user_id=? FOR UPDATE NOWAIT",
                                    student))
                    .isFalse();
            assertThat(
                            isLockBlocked(
                                    "SELECT id FROM dormitory_contract WHERE id=? FOR UPDATE NOWAIT",
                                    contract))
                    .isFalse();
            assertThat(historyCount(EntityType.APPLICATION, application)).isZero();
            assertThat(historyCount(EntityType.CONTRACT, contract)).isZero();
            allowManual.countDown();
            assertThat(manual.get(15, TimeUnit.SECONDS).status())
                    .isEqualTo(
                            cancelApplication ? ApplicationStatus.CANCELLED : ApplicationStatus.APPROVED);
            assertThat(job.get(15, TimeUnit.SECONDS))
                    .isEqualTo(new SystemJobService.RunResult(0, 1, 0));
            assertCancellationOutcome(false, cancelApplication);
        } finally {
            allowManual.countDown();
            pool.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void expiryWinsBeforeInteractiveCancellation(boolean cancelApplication) throws Exception {
        var jwt = adminJwt();
        var manualBackend = new CompletableFuture<Integer>();
        var jobBackend = new CompletableFuture<Integer>();
        var allowJobCommit = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var job =
                    pool.submit(
                            () ->
                                    jobs.run(
                                            SystemJob.CANCEL_PENDING,
                                            () -> List.of(contract),
                                            id -> {
                                                var change = contracts.cancelExpiredPending(id);
                                                jobBackend.complete(backendPid());
                                                awaitLatch(allowJobCommit);
                                                return change;
                                            }));
            int jobPid = jobBackend.get(10, TimeUnit.SECONDS);
            var manual =
                    pool.submit(
                            () ->
                                    asAdmin(
                                            jwt,
                                            () ->
                                                    new TransactionTemplate(transactionManager)
                                                            .execute(
                                                                    transaction -> {
                                                                        manualBackend.complete(backendPid());
                                                                        return cancelOrRelease(cancelApplication);
                                                                    })));
            int manualPid = manualBackend.get(10, TimeUnit.SECONDS);
            awaitBlockedBy(manualPid, jobPid);
            assertWaitingOnApplication(manualPid);
            allowJobCommit.countDown();
            assertThat(job.get(15, TimeUnit.SECONDS))
                    .isEqualTo(new SystemJobService.RunResult(1, 0, 0));
            assertThatThrownBy(() -> manual.get(15, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(BusinessException.class)
                    .satisfies(
                            error ->
                                    assertThat(((BusinessException) error.getCause()).getErrorCode())
                                            .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
            assertCancellationOutcome(true, cancelApplication);
        } finally {
            allowJobCommit.countDown();
            pool.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rechecksContractAfterWaitingForBed(boolean activateContract) throws Exception {
        var holderBackend = new CompletableFuture<Integer>();
        var jobBackend = new CompletableFuture<Integer>();
        var allowChange = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var holder =
                    pool.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    transaction -> {
                                                        jdbc.queryForObject(
                                                                "SELECT id FROM bed WHERE id=? FOR UPDATE",
                                                                Long.class,
                                                                bed);
                                                        holderBackend.complete(backendPid());
                                                        awaitLatch(allowChange);
                                                        if (activateContract) {
                                                            activate();
                                                        } else {
                                                            jdbc.update(
                                                                    "UPDATE dormitory_contract SET"
                                                                            + " activation_deadline=CURRENT_TIMESTAMP"
                                                                            + "+INTERVAL '1 day' WHERE id=?",
                                                                    contract);
                                                        }
                                                    }));
            int holderPid = holderBackend.get(10, TimeUnit.SECONDS);
            var job =
                    pool.submit(
                            () ->
                                    jobs.run(
                                            SystemJob.CANCEL_PENDING,
                                            () -> List.of(contract),
                                            id -> {
                                                jobBackend.complete(backendPid());
                                                return contracts.cancelExpiredPending(id);
                                            }));
            int jobPid = jobBackend.get(10, TimeUnit.SECONDS);
            awaitBlockedBy(jobPid, holderPid);
            assertThat(
                            isLockBlocked(
                                    "SELECT id FROM room_application WHERE id=? FOR UPDATE NOWAIT",
                                    application))
                    .isTrue();
            assertThat(
                            isLockBlocked(
                                    "SELECT user_id FROM student_profile WHERE user_id=? FOR UPDATE NOWAIT",
                                    student))
                    .isFalse();
            assertThat(
                            isLockBlocked(
                                    "SELECT id FROM dormitory_contract WHERE id=? FOR UPDATE NOWAIT",
                                    contract))
                    .isFalse();
            allowChange.countDown();
            holder.get(15, TimeUnit.SECONDS);
            assertThat(job.get(15, TimeUnit.SECONDS))
                    .isEqualTo(new SystemJobService.RunResult(0, 1, 0));
            assertThat(contractStatus())
                    .isEqualTo(activateContract ? ContractStatus.ACTIVE : ContractStatus.PENDING);
            assertThat(applicationStatus()).isEqualTo(ApplicationStatus.ASSIGNED);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT kind FROM room_assignment_history WHERE contract_id=?"
                                            + " AND ended_at IS NULL",
                                    String.class,
                                    contract))
                    .isEqualTo(activateContract ? AssignmentKind.STAY : AssignmentKind.HOLD);
            assertThat(historyCount(EntityType.APPLICATION, application)).isZero();
            assertThat(historyCount(EntityType.CONTRACT, contract)).isZero();
            assertThat(auditCount(AuditAction.CONTRACT_AUTO_CANCEL, EntityType.CONTRACT, contract, null))
                    .isZero();
            assertThat(auditCount(AuditAction.APPLICATION_AUTO_CANCEL, EntityType.APPLICATION, application, null))
                    .isZero();
        } finally {
            allowChange.countDown();
            pool.shutdownNow();
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
        assertThat(applicationStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(historyCount(EntityType.APPLICATION, application)).isZero();
        assertThat(historyCount(EntityType.CONTRACT, contract)).isZero();
        assertAudit(AuditAction.APPLICATION_AUTO_CANCEL, EntityType.APPLICATION, application, null, 0);
        assertAudit(AuditAction.CONTRACT_AUTO_CANCEL, EntityType.CONTRACT, contract, null, 0);
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

    private Jwt adminJwt() {
        String subject = "job-admin-" + suffix;
        jdbc.update("UPDATE app_user SET keycloak_user_id=? WHERE id=?", subject, actor);
        Long version =
                jdbc.queryForObject(
                        "SELECT security_version FROM app_user WHERE id=?", Long.class, actor);
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim(KeycloakClaim.REALM_ACCESS, Map.of(KeycloakClaim.ROLES, List.of(Role.ADMIN)))
                .claim(KeycloakClaim.SECURITY_VERSION, version)
                .build();
    }

    private <T> T asAdmin(Jwt jwt, Supplier<T> operation) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        try {
            return operation.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Response cancelOrRelease(boolean cancelApplication) {
        return cancelApplication
                ? applications.performApplicationAction(application, BusinessAction.CANCEL, null)
                : applications.releaseHold(application, RELEASE_REASON);
    }

    private int backendPid() {
        return jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
    }

    private void awaitBlockedBy(int waitingPid, int blockerPid) {
        await().atMost(10, TimeUnit.SECONDS)
                .until(
                        () ->
                                Boolean.TRUE.equals(
                                        jdbc.queryForObject(
                                                "SELECT ? = ANY(pg_blocking_pids(?))",
                                                Boolean.class,
                                                blockerPid,
                                                waitingPid)));
    }

    private void assertWaitingOnApplication(int pid) {
        assertThat(
                        jdbc.queryForObject(
                                "SELECT query FROM pg_stat_activity WHERE pid=?", String.class, pid))
                .contains("room_application")
                .contains("FOR UPDATE");
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent transaction");
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }

    private String applicationStatus() {
        return jdbc.queryForObject(
                "SELECT status FROM room_application WHERE id=?", String.class, application);
    }

    private String contractStatus() {
        return jdbc.queryForObject(
                "SELECT status FROM dormitory_contract WHERE id=?", String.class, contract);
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private long historyCount(String type, Long id) {
        return count(
                "SELECT count(*) FROM status_history WHERE entity_type=? AND entity_id=?", type, id);
    }

    private long auditCount(String action, String type, Long id, Long actorId) {
        return count(
                "SELECT count(*) FROM audit_log WHERE action=? AND entity_type=? AND entity_id=?"
                        + " AND actor_id IS NOT DISTINCT FROM ?",
                action,
                type,
                id,
                actorId);
    }

    private void assertAudit(String action, String type, Long id, Long actorId, int expected) {
        assertThat(
                        count(
                                "SELECT count(*) FROM audit_log WHERE action=? AND entity_type=?"
                                        + " AND entity_id=?",
                                action,
                                type,
                                id))
                .isEqualTo(expected);
        assertThat(auditCount(action, type, id, actorId)).isEqualTo(expected);
    }

    private void assertTransition(
            String type, Long id, String before, String after, Long actorId) {
        assertThat(historyCount(type, id)).isEqualTo(1);
        assertThat(
                        count(
                                "SELECT count(*) FROM status_history WHERE entity_type=? AND entity_id=?"
                                        + " AND from_status=? AND to_status=?"
                                        + " AND actor_id IS NOT DISTINCT FROM ?",
                                type,
                                id,
                                before,
                                after,
                                actorId))
                .isEqualTo(1);
    }

    private void assertCancellationOutcome(boolean automatic, boolean cancelApplication) {
        Long expectedActor = automatic ? null : actor;
        String expectedApplication =
                automatic || cancelApplication
                        ? ApplicationStatus.CANCELLED
                        : ApplicationStatus.APPROVED;
        assertThat(applicationStatus()).isEqualTo(expectedApplication);
        assertThat(contractStatus()).isEqualTo(ContractStatus.CANCELLED);
        assertThat(count("SELECT count(*) FROM room_assignment_history WHERE contract_id=?", contract))
                .isEqualTo(1);
        assertThat(
                        count(
                                "SELECT count(*) FROM room_assignment_history WHERE contract_id=?"
                                        + " AND ended_at IS NULL",
                                contract))
                .isZero();
        var assignment =
                jdbc.queryForMap(
                        "SELECT id,kind,end_reason,ended_at FROM room_assignment_history WHERE contract_id=?",
                        contract);
        Long assignmentId = ((Number) assignment.get("id")).longValue();
        assertThat(assignment.get("kind")).isEqualTo(AssignmentKind.HOLD);
        assertThat(assignment.get("ended_at")).isNotNull();
        assertThat(assignment.get("end_reason"))
                .isEqualTo(
                        automatic
                                ? AssignmentEndReason.HOLD_EXPIRED
                                : AssignmentEndReason.HOLD_CANCELLED);
        assertTransition(
                EntityType.APPLICATION,
                application,
                ApplicationStatus.ASSIGNED,
                expectedApplication,
                expectedActor);
        assertTransition(
                EntityType.CONTRACT,
                contract,
                ContractStatus.PENDING,
                ContractStatus.CANCELLED,
                expectedActor);
        assertAudit(AuditAction.BED_RELEASE, EntityType.ASSIGNMENT, assignmentId, expectedActor, 1);
        assertAudit(
                AuditAction.CONTRACT_AUTO_CANCEL, EntityType.CONTRACT, contract, null, automatic ? 1 : 0);
        assertAudit(
                AuditAction.APPLICATION_AUTO_CANCEL,
                EntityType.APPLICATION,
                application,
                null,
                automatic ? 1 : 0);
        assertAudit(AuditAction.CRUD_UPDATE, EntityType.CONTRACT, contract, actor, automatic ? 0 : 1);
        assertAudit(
                AuditAction.APPLICATION_RELEASE_HOLD,
                EntityType.APPLICATION,
                application,
                actor,
                !automatic && !cancelApplication ? 1 : 0);
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

package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import vn.edu.tlu.dms.account.api.AccountDtos.Preference;
import vn.edu.tlu.dms.account.api.AccountDtos.PreferenceInput;
import vn.edu.tlu.dms.account.repository.UserPreferenceRepository;
import vn.edu.tlu.dms.account.service.UserPreferenceService;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.service.RolePermissionService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Verifies the PostgreSQL primary-key race when both first saves observe no preference row. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserPreferenceConcurrencyIntegrationTest {

    private static final int WAIT_SECONDS = 30;

    @Autowired UserPreferenceService preferences;

    @Autowired JdbcTemplate jdbc;

    @Autowired PlatformTransactionManager transactionManager;

    @MockitoBean CurrentUser currentUser;

    @MockitoBean RolePermissionService rolePermissions;

    @MockitoSpyBean UserPreferenceRepository repository;

    @Test
    void concurrentFirstSavesCommitExactlyOneWinnerAndRejectTheOther() throws Exception {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactions.setTimeout(WAIT_SECONDS);

        String username = "pref_race_" + UUID.randomUUID();
        Long userId = transactions.execute(status -> jdbc.queryForObject(
                """
                INSERT INTO app_user(username, role, full_name, password_hash)
                VALUES (?, ?, ?, ?) RETURNING id
                """,
                Long.class, username, Role.STUDENT, "Preference race", "test-unused-hash"));
        assertThat(userId).isNotNull();

        CountDownLatch bothMissing = new CountDownLatch(2);
        Set<Long> transactionIds = ConcurrentHashMap.newKeySet();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            when(currentUser.get())
                    .thenReturn(new Actor(userId, Role.STUDENT, null, false, false));
            when(rolePermissions.getGrantedPermissions(anyString()))
                    .thenReturn(List.of(Permission.PROFILE_READ, Permission.PROFILE_UPDATE));
            assertThat(preferenceCount(userId)).isZero();

            doAnswer(invocation -> {
                // Spring Data query methods are abstract: do not use callRealMethod on the spy.
                // Execute the equivalent real locking lookup on this worker's transaction instead.
                // Only this read is intercepted; persist/flush and all constraints remain real.
                assertThat(jdbc.queryForList(
                        "SELECT user_id FROM user_preference WHERE user_id = ? FOR UPDATE",
                        Long.class, userId)).isEmpty();
                transactionIds.add(jdbc.queryForObject("SELECT txid_current()", Long.class));
                bothMissing.countDown();
                assertThat(bothMissing.await(WAIT_SECONDS, TimeUnit.SECONDS))
                        .as("both transactions finish their missing-row lookup before either insert")
                        .isTrue();
                return Optional.empty();
            }).when(repository).lockByUserId(userId);

            PreferenceInput first = new PreferenceInput(
                    DmsConstants.Preference.MODE_DARK,
                    DmsConstants.Preference.PRESET_TEAL,
                    true,
                    DmsConstants.Preference.INITIAL_VERSION);
            PreferenceInput second = new PreferenceInput(
                    DmsConstants.Preference.MODE_LIGHT,
                    DmsConstants.Preference.PRESET_AMBER,
                    false,
                    DmsConstants.Preference.INITIAL_VERSION);

            var firstFuture = workers.submit(() -> save(transactions, first));
            var secondFuture = workers.submit(() -> save(transactions, second));
            List<Outcome> outcomes = List.of(
                    firstFuture.get(WAIT_SECONDS * 2L, TimeUnit.SECONDS),
                    secondFuture.get(WAIT_SECONDS * 2L, TimeUnit.SECONDS));

            assertThat(transactionIds).hasSize(2);
            assertThat(bothMissing.getCount()).isZero();
            verify(repository, times(2)).lockByUserId(userId);
            List<Outcome> successes = outcomes.stream()
                    .filter(outcome -> outcome.result() != null).toList();
            List<Outcome> failures = outcomes.stream()
                    .filter(outcome -> outcome.failure() != null).toList();
            assertThat(successes).hasSize(1);
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).failure().getErrorCode())
                    .isEqualTo(ErrorCode.CONCURRENT_UPDATE);
            assertThat(failures.get(0).failure().getStatus())
                    .isEqualTo(HttpStatus.CONFLICT.value());

            Outcome winner = successes.get(0);
            Preference expected = new Preference(
                    winner.input().themeMode(), winner.input().themePreset(),
                    winner.input().tableDense(), 1L);
            assertThat(winner.result()).isEqualTo(expected);
            // Futures complete only after commit/rollback, so these reads see committed state.
            assertThat(preferenceCount(userId)).isEqualTo(1L);
            var stored = jdbc.queryForMap(
                    """
                    SELECT theme_mode, theme_preset, table_dense, version, updated_at
                    FROM user_preference WHERE user_id = ?
                    """, userId);
            assertThat(stored.get("theme_mode")).isEqualTo(expected.themeMode());
            assertThat(stored.get("theme_preset")).isEqualTo(expected.themePreset());
            assertThat(stored.get("table_dense")).isEqualTo(expected.tableDense());
            assertThat(stored.get("version")).isEqualTo(1L);
            assertThat(stored.get("updated_at")).isNotNull();
            assertThat(preferences.preferences()).isEqualTo(expected);
        } finally {
            // Release a lone worker even if setup/submission/assertions failed.
            while (bothMissing.getCount() > 0) {
                bothMissing.countDown();
            }
            workers.shutdownNow();
            assertThat(workers.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS))
                    .as("workers stop before fixture cleanup")
                    .isTrue();
            transactions.executeWithoutResult(status -> {
                jdbc.update("DELETE FROM user_preference WHERE user_id = ?", userId);
                jdbc.update("DELETE FROM app_user WHERE id = ?", userId);
            });
        }
    }

    private Outcome save(TransactionTemplate transactions, PreferenceInput input) {
        try {
            Preference result = transactions.execute(status -> {
                // Bound database waits as well as Java waits, including a blocked duplicate insert.
                jdbc.queryForObject(
                        "SELECT set_config('lock_timeout', ?, true)", String.class, "10s");
                jdbc.queryForObject(
                        "SELECT set_config('statement_timeout', ?, true)", String.class, "20s");
                return preferences.saveOrUpdate(input);
            });
            return new Outcome(input, result, null);
        } catch (BusinessException conflict) {
            // Catch outside the template: the losing transaction must have rolled back already.
            return new Outcome(input, null, conflict);
        }
    }

    private Long preferenceCount(Long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_preference WHERE user_id = ?", Long.class, userId);
    }

    private record Outcome(PreferenceInput input, Preference result, BusinessException failure) {}
}

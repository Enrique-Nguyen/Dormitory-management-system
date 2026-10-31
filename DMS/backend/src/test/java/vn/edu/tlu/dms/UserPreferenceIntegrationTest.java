package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.api.AccountDtos.PreferenceInput;
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
import java.util.Map;
import java.util.UUID;

/**
 * Kiểm thử tích hợp sở thích giao diện trên PostgreSQL thật: mặc định khi chưa có dòng, lưu và
 * tăng version, chặn version cũ, từ chối giá trị ngoài danh mục và tách dữ liệu giữa hai tài
 * khoản. Cần PostgreSQL của cấu hình test; không thay đổi dữ liệu của tài khoản khác.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserPreferenceIntegrationTest {

    @Autowired UserPreferenceService preferences;

    @Autowired JdbcTemplate jdbc;

    @MockitoBean CurrentUser currentUser;

    @MockitoBean RolePermissionService rolePermissions;

    private Long userA;

    private Long userB;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        userA = insertUser("pref_a_" + suffix);
        userB = insertUser("pref_b_" + suffix);
        // Quyền hồ sơ được cấp cho mọi vai trò trong ma trận thật; giả lập để test độc lập dữ liệu.
        when(rolePermissions.getGrantedPermissions(anyString()))
                .thenReturn(List.of(Permission.PROFILE_READ, Permission.PROFILE_UPDATE));
        as(userA);
    }

    @Test
    void returnsDefaultsWithoutInsertingRow() {
        var result = preferences.preferences();

        assertThat(result.themeMode()).isEqualTo(DmsConstants.Preference.DEFAULT_MODE);
        assertThat(result.themePreset()).isEqualTo(DmsConstants.Preference.DEFAULT_PRESET);
        assertThat(result.tableDense()).isFalse();
        assertThat(result.version()).isEqualTo(DmsConstants.Preference.INITIAL_VERSION);
        assertThat(preferenceCount(userA)).isZero();
    }

    @Test
    void firstSaveInsertsThenUpdatePersistsAndIncrementsVersion() {
        var created =
                preferences.saveOrUpdate(
                        new PreferenceInput(
                                DmsConstants.Preference.MODE_DARK,
                                DmsConstants.Preference.PRESET_TEAL,
                                true,
                                null));

        assertThat(created.version()).isEqualTo(1L);
        assertThat(preferenceCount(userA)).isEqualTo(1L);
        Map<String, Object> stored = storedPreference(userA);
        assertThat(stored.get("theme_mode")).isEqualTo(DmsConstants.Preference.MODE_DARK);
        assertThat(stored.get("theme_preset")).isEqualTo(DmsConstants.Preference.PRESET_TEAL);
        assertThat(stored.get("table_dense")).isEqualTo(true);
        assertThat(stored.get("version")).isEqualTo(1L);
        assertThat(stored.get("updated_at")).isNotNull();

        var updated =
                preferences.saveOrUpdate(
                        new PreferenceInput(
                                DmsConstants.Preference.MODE_LIGHT,
                                DmsConstants.Preference.PRESET_VIOLET,
                                false,
                                created.version()));

        assertThat(updated.version()).isEqualTo(2L);
        assertThat(preferences.preferences()).isEqualTo(updated);
        Map<String, Object> reread = storedPreference(userA);
        assertThat(reread.get("theme_mode")).isEqualTo(DmsConstants.Preference.MODE_LIGHT);
        assertThat(reread.get("theme_preset")).isEqualTo(DmsConstants.Preference.PRESET_VIOLET);
        assertThat(reread.get("table_dense")).isEqualTo(false);
        assertThat(reread.get("version")).isEqualTo(2L);
    }

    @Test
    void staleVersionIsRejectedAndKeepsStoredValues() {
        preferences.saveOrUpdate(
                new PreferenceInput(
                        DmsConstants.Preference.MODE_DARK,
                        DmsConstants.Preference.PRESET_TEAL,
                        true,
                        null));

        assertThatThrownBy(
                        () ->
                                preferences.saveOrUpdate(
                                        new PreferenceInput(
                                                DmsConstants.Preference.MODE_LIGHT,
                                                DmsConstants.Preference.PRESET_AMBER,
                                                false,
                                                7L)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> {
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.CONCURRENT_UPDATE);
                            assertThat(exception.getStatus())
                                    .isEqualTo(HttpStatus.CONFLICT.value());
                        });

        Map<String, Object> stored = storedPreference(userA);
        assertThat(stored.get("theme_mode")).isEqualTo(DmsConstants.Preference.MODE_DARK);
        assertThat(stored.get("theme_preset")).isEqualTo(DmsConstants.Preference.PRESET_TEAL);
        assertThat(stored.get("version")).isEqualTo(1L);
    }

    @Test
    void existingRowRequiresVersionOnUpdate() {
        preferences.saveOrUpdate(
                new PreferenceInput(
                        DmsConstants.Preference.MODE_DARK,
                        DmsConstants.Preference.PRESET_TEAL,
                        true,
                        null));

        assertThatThrownBy(
                        () ->
                                preferences.saveOrUpdate(
                                        new PreferenceInput(
                                                DmsConstants.Preference.MODE_LIGHT,
                                                DmsConstants.Preference.PRESET_AMBER,
                                                false,
                                                null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> {
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
                            assertThat(exception.getStatus())
                                    .isEqualTo(HttpStatus.BAD_REQUEST.value());
                        });

        Map<String, Object> stored = storedPreference(userA);
        assertThat(stored.get("theme_mode")).isEqualTo(DmsConstants.Preference.MODE_DARK);
        assertThat(stored.get("theme_preset")).isEqualTo(DmsConstants.Preference.PRESET_TEAL);
        assertThat(stored.get("version")).isEqualTo(1L);
    }

    @Test
    void rejectsModePresetOutsideWhitelist() {
        assertThatThrownBy(
                        () ->
                                preferences.saveOrUpdate(
                                        new PreferenceInput("neon", "blue", false, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> {
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
                            assertThat(exception.getStatus())
                                    .isEqualTo(HttpStatus.BAD_REQUEST.value());
                        });

        assertThatThrownBy(
                        () ->
                                preferences.saveOrUpdate(
                                        new PreferenceInput("dark", "pink", false, null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> {
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
                            assertThat(exception.getStatus())
                                    .isEqualTo(HttpStatus.BAD_REQUEST.value());
                        });

        assertThatThrownBy(
                        () ->
                                preferences.saveOrUpdate(
                                        new PreferenceInput(
                                                DmsConstants.Preference.MODE_LIGHT,
                                                DmsConstants.Preference.PRESET_BLUE,
                                                null,
                                                null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> {
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
                            assertThat(exception.getStatus())
                                    .isEqualTo(HttpStatus.BAD_REQUEST.value());
                        });

        assertThat(preferenceCount(userA)).isZero();
    }

    @Test
    void accountsKeepTheirOwnPreferences() {
        preferences.saveOrUpdate(
                new PreferenceInput(
                        DmsConstants.Preference.MODE_DARK,
                        DmsConstants.Preference.PRESET_EMERALD,
                        true,
                        null));

        as(userB);
        var untouched = preferences.preferences();
        assertThat(untouched.themeMode()).isEqualTo(DmsConstants.Preference.DEFAULT_MODE);
        assertThat(untouched.themePreset()).isEqualTo(DmsConstants.Preference.DEFAULT_PRESET);
        assertThat(untouched.version()).isEqualTo(DmsConstants.Preference.INITIAL_VERSION);
        assertThat(preferenceCount(userB)).isZero();

        preferences.saveOrUpdate(
                new PreferenceInput(
                        DmsConstants.Preference.MODE_LIGHT,
                        DmsConstants.Preference.PRESET_AMBER,
                        false,
                        null));

        as(userA);
        var mine = preferences.preferences();
        assertThat(mine.themeMode()).isEqualTo(DmsConstants.Preference.MODE_DARK);
        assertThat(mine.themePreset()).isEqualTo(DmsConstants.Preference.PRESET_EMERALD);
        assertThat(mine.tableDense()).isTrue();
        assertThat(mine.version()).isEqualTo(1L);
        assertThat(preferenceCount(userA)).isEqualTo(1L);
        assertThat(preferenceCount(userB)).isEqualTo(1L);
    }

    private void as(Long userId) {
        when(currentUser.get()).thenReturn(new Actor(userId, Role.STUDENT, null, false, false));
    }

    private Long preferenceCount(Long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_preference WHERE user_id = ?", Long.class, userId);
    }

    private Map<String, Object> storedPreference(Long userId) {
        return jdbc.queryForMap(
                """
                SELECT theme_mode, theme_preset, table_dense, version, updated_at
                FROM user_preference WHERE user_id = ?
                """,
                userId);
    }

    private Long insertUser(String username) {
        return jdbc.queryForObject(
                """
                INSERT INTO app_user(username, role, full_name, password_hash)
                VALUES(?, 'STUDENT', ?, 'hash')
                RETURNING id
                """,
                Long.class,
                username,
                "Preference " + username);
    }
}

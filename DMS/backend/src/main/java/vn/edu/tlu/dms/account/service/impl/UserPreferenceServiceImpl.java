package vn.edu.tlu.dms.account.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.api.AccountDtos.Preference;
import vn.edu.tlu.dms.account.api.AccountDtos.PreferenceInput;
import vn.edu.tlu.dms.account.domain.UserPreference;
import vn.edu.tlu.dms.account.repository.UserPreferenceRepository;
import vn.edu.tlu.dms.account.service.UserPreferenceService;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;

import java.time.OffsetDateTime;
import java.util.Set;

/** Sở thích giao diện thuộc đúng tài khoản đang đăng nhập, lấy ID từ {@link CurrentUser}. */
@Service
@Transactional(readOnly = true)
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceRepository preferences;

    private final CurrentUser current;

    private final ScopeGuard guard;

    public UserPreferenceServiceImpl(
            UserPreferenceRepository preferences, CurrentUser current, ScopeGuard guard) {
        this.preferences = preferences;
        this.current = current;
        this.guard = guard;
    }

    @Override
    public Preference preferences() {
        guard.requirePermission(Permission.PROFILE_READ);
        return toResponse(preferences.findById(current.get().id()).orElse(null));
    }

    @Override
    @Transactional
    public Preference saveOrUpdate(PreferenceInput input) {
        guard.requirePermission(Permission.PROFILE_UPDATE);
        String themeMode =
                requireAllowed(
                        input.themeMode(),
                        DmsConstants.Preference.MODES,
                        ApiField.THEME_MODE,
                        ValidationMessage.PREFERENCE_MODE_INVALID);
        String themePreset =
                requireAllowed(
                        input.themePreset(),
                        DmsConstants.Preference.PRESETS,
                        ApiField.THEME_PRESET,
                        ValidationMessage.PREFERENCE_PRESET_INVALID);
        if (input.tableDense() == null) {
            throw BusinessException.invalid(
                    ApiField.TABLE_DENSE, ValidationMessage.PREFERENCE_TABLE_DENSE_REQUIRED);
        }
        Long userId = current.get().id();
        // Khóa dòng trước, đọc lại version sau khi có khóa rồi mới ghi.
        var stored = preferences.lockByUserId(userId).orElse(null);
        long storedVersion =
                stored == null ? DmsConstants.Preference.INITIAL_VERSION : stored.getVersion();
        if (stored != null && input.version() == null) {
            throw BusinessException.invalid(
                    ApiField.VERSION, ValidationMessage.PREFERENCE_VERSION_REQUIRED);
        }
        if (input.version() != null && input.version().longValue() != storedVersion) {
            throw BusinessException.conflict(ErrorCode.CONCURRENT_UPDATE);
        }        if (stored == null) {
            var created = new UserPreference();
            created.setUserId(userId);
            created.setThemeMode(themeMode);
            created.setThemePreset(themePreset);
            created.setTableDense(input.tableDense());
            created.setVersion(Math.incrementExact(storedVersion));
            created.setUpdatedAt(OffsetDateTime.now());
            try {
                // Không có dòng để khóa khi lưu lần đầu: hai tab cùng chèn thì PK chặn một bên;
                // trả 409 để client tải lại thay vì lỗi hệ thống.
                preferences.saveAndFlush(created);
            } catch (DataIntegrityViolationException duplicate) {
                throw BusinessException.conflict(ErrorCode.CONCURRENT_UPDATE);
            }
            return toResponse(created);
        }
        stored.setThemeMode(themeMode);
        stored.setThemePreset(themePreset);
        stored.setTableDense(input.tableDense());
        stored.setVersion(Math.incrementExact(storedVersion));
        stored.setUpdatedAt(OffsetDateTime.now());
        preferences.saveAndFlush(stored);
        return toResponse(stored);
    }

    /** Từ chối mã ngoài danh mục đã duyệt kể cả khi client bỏ qua validation của DTO. */
    private String requireAllowed(
            String value, Set<String> allowed, String field, String message) {
        if (value == null || !allowed.contains(value)) {
            throw BusinessException.invalid(field, message);
        }
        return value;
    }

    private Preference toResponse(UserPreference stored) {
        if (stored == null) {
            return new Preference(
                    DmsConstants.Preference.DEFAULT_MODE,
                    DmsConstants.Preference.DEFAULT_PRESET,
                    DmsConstants.Preference.DEFAULT_TABLE_DENSE,
                    DmsConstants.Preference.INITIAL_VERSION);
        }
        return new Preference(
                stored.getThemeMode(),
                stored.getThemePreset(),
                Boolean.TRUE.equals(stored.getTableDense()),
                stored.getVersion());
    }
}

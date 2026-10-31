package vn.edu.tlu.dms.account.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.Gender;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;

import java.time.LocalDate;
import java.util.List;

public final class AccountDtos {

    private AccountDtos() {}

    public record Password(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 64) String newPassword) {
        @Override
        public String toString() {
            return "Password[REDACTED]";
        }
    }

    public record UnitRef(Long id, String code, String name) {}

    public record Student(
            String studentCode, String gender, boolean isRoomLeader, boolean hasActiveContract) {}

    public record Me(
            Long id,
            String username,
            String fullName,
            String role,
            UnitRef managementUnit,
            boolean crossUnitAccess,
            boolean mustChangePassword,
            Student student,
            List<String> permissions) {}

    /** ID trống để tạo; có ID chỉ cập nhật các trường được gửi, không đặt lại mật khẩu. */
    public record Save(
            @Positive Long id,
            @Size(max = 50) String username,
            @Size(max = 100) String fullName,
            @Size(max = 20) String phone,
            @Pattern(regexp = Role.MANAGEMENT_ROLE_PATTERN) String role,
            @Positive Long managementUnitId,
            @Size(max = 20) String studentCode,
            @Pattern(regexp = Gender.PATTERN) String gender,
            LocalDate dateOfBirth,
            @Size(max = 100) String faculty,
            @Size(max = 10) String cohort) {}

    public record Create(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 100) String fullName,
            @Size(max = 20) String phone,
            @NotBlank @Pattern(regexp = Role.MANAGEMENT_ROLE_PATTERN) String role,
            Long managementUnitId) {}

    public record Update(
            @Size(max = 100) String fullName,
            @Size(max = 20) String phone,
            @Pattern(regexp = Role.MANAGEMENT_ROLE_PATTERN) String role,
            Long managementUnitId,
            @Size(max = 20) String studentCode,
            @Pattern(regexp = Gender.PATTERN) String gender,
            LocalDate dateOfBirth,
            @Size(max = 100) String faculty,
            @Size(max = 10) String cohort) {}

    public record ProfileInput(
            @Size(max = 20) String phone,
            @Size(max = 200) String hometown,
            @Size(max = 200) String emergencyContact) {}

    public record Profile(
            Long id,
            String username,
            String fullName,
            String phone,
            String role,
            Long managementUnitId,
            boolean locked,
            boolean crossUnitAccess,
            boolean mustChangePassword,
            String studentCode,
            String gender,
            LocalDate dateOfBirth,
            String faculty,
            String cohort,
            String hometown,
            String emergencyContact) {}

    /** Sở thích giao diện của chính tài khoản đang đăng nhập; version 0 khi chưa có bản ghi. */
    public record Preference(
            String themeMode, String themePreset, boolean tableDense, long version) {}

    /** Chỉ nhận preset đã duyệt; version null khi lưu lần đầu, có version khi sửa. */
    public record PreferenceInput(
            @NotBlank @Pattern(regexp = DmsConstants.Preference.MODE_PATTERN) String themeMode,
            @NotBlank @Pattern(regexp = DmsConstants.Preference.PRESET_PATTERN) String themePreset,
            @NotNull Boolean tableDense,
            @PositiveOrZero Long version) {}

    public record TemporaryPassword(Long userId, String temporaryPassword) {
        @Override
        public String toString() {
            return "TemporaryPassword[userId=" + userId + ", temporaryPassword=REDACTED]";
        }
    }

    public record Reason(@NotBlank @Size(min = 5, max = 500) String reason) {}

    public record CrossUnit(@NotNull Boolean enabled) {}
}

package vn.edu.tlu.dms.account.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class AccountDtos {

    private AccountDtos() {}

    public record Login(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 200) String password) {}

    public record Password(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 64) String newPassword) {}

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
            Student student) {}

    public record Create(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 100) String fullName,
            @Size(max = 20) String phone,
            @NotBlank @Pattern(regexp = "STAFF|TECHNICIAN|ADMIN") String role,
            Long managementUnitId) {}

    public record Update(
            @Size(max = 100) String fullName,
            @Size(max = 20) String phone,
            Long managementUnitId,
            @Size(max = 20) String studentCode,
            @Pattern(regexp = "M|F") String gender,
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

    public record TemporaryPassword(Long userId, String temporaryPassword) {}

    public record Reason(@NotBlank @Size(min = 5, max = 500) String reason) {}

    public record CrossUnit(@NotNull Boolean enabled) {}
}

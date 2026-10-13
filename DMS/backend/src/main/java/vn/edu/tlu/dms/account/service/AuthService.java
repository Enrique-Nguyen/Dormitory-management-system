package vn.edu.tlu.dms.account.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.api.AccountDtos.Me;
import vn.edu.tlu.dms.account.api.AccountDtos.Student;
import vn.edu.tlu.dms.account.api.AccountDtos.UnitRef;
import vn.edu.tlu.dms.account.domain.AppUser;
import vn.edu.tlu.dms.account.repository.AppUserRepository;
import vn.edu.tlu.dms.account.repository.ManagementUnitRepository;
import vn.edu.tlu.dms.account.repository.StudentProfileRepository;
import vn.edu.tlu.dms.account.security.DmsPrincipal;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.service.ResidencyLookup;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class AuthService {

    private final AppUserRepository users;

    private final ManagementUnitRepository units;

    private final StudentProfileRepository students;

    private final PasswordEncoder passwords;

    private final CurrentUser currentUser;

    private final ResidencyLookup residency;

    public AuthService(
            AppUserRepository users,
            ManagementUnitRepository units,
            StudentProfileRepository students,
            PasswordEncoder passwords,
            CurrentUser currentUser,
            ResidencyLookup residency) {
        this.users = users;
        this.units = units;
        this.students = students;
        this.passwords = passwords;
        this.currentUser = currentUser;
        this.residency = residency;
    }

    /**
     * Khóa tài khoản khi kiểm tra mật khẩu để không mất số lần sai khi có request đồng thời. Không
     * rollback BusinessException: đăng nhập thất bại vẫn phải lưu bộ đếm và hạn khóa. Mật khẩu và
     * hash không được ghi log hoặc đưa vào response.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public DmsPrincipal authenticate(String username, String password) {
        var user =
                users.lockByUsername(username)
                        .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", 401));
        if (Boolean.TRUE.equals(user.getLocked())) {
            throw new BusinessException("ACCOUNT_LOCKED", 403);
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new BusinessException(
                    "ACCOUNT_TEMP_LOCKED", 403, Map.of("lockedUntil", user.getLockedUntil()));
        }
        if (user.getLockedUntil() != null) {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
        }
        if (!passwords.matches(password, user.getPasswordHash())) {
            int failures = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failures);
            if (failures >= 5) {
                user.setLockedUntil(now.plusMinutes(15));
            }
            users.saveAndFlush(user);
            if (failures >= 5) {
                throw new BusinessException(
                        "ACCOUNT_TEMP_LOCKED", 403, Map.of("lockedUntil", user.getLockedUntil()));
            }
            throw new BusinessException(
                    "INVALID_CREDENTIALS",
                    401,
                    failures >= 3 ? Map.of("remainingAttempts", 5 - failures) : Map.of());
        }
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        users.save(user);
        return new DmsPrincipal(user.getId(), user.getUsername());
    }

    @Transactional(readOnly = true)
    public Me getCurrentUser() {
        AppUser u = users.findById(currentUser.get().id()).orElseThrow(BusinessException::notFound);
        UnitRef unit =
                u.getManagementUnitId() == null
                        ? null
                        : units.findById(u.getManagementUnitId())
                                .map(r -> new UnitRef(r.getId(), r.getCode(), r.getName()))
                                .orElse(null);
        Student student =
                students.findById(u.getId())
                        .map(
                                p -> {
                                    var stay = residency.current(u.getId()).orElse(null);
                                    return new Student(
                                            p.getStudentCode(),
                                            p.getGender(),
                                            stay != null && stay.roomLeader(),
                                            stay != null && stay.activeContract());
                                })
                        .orElse(null);
        return new Me(
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getRole(),
                unit,
                u.getCrossUnitAccess(),
                u.getMustChangePassword(),
                student);
    }

    @Transactional
    public void changePassword(String current, String next) {
        var u = users.lockById(currentUser.get().id()).orElseThrow(BusinessException::notFound);
        if (!passwords.matches(current, u.getPasswordHash())) {
            throw new BusinessException("WRONG_CURRENT_PASSWORD", 400);
        }
        if (next.length() < 8
                || next.length() > 64
                || !next.matches(".*[A-Za-z].*")
                || !next.matches(".*[0-9].*")
                || current.equals(next)) {
            throw new BusinessException("PASSWORD_POLICY", 400);
        }
        u.setPasswordHash(passwords.encode(next));
        u.setMustChangePassword(false);
        users.saveAndFlush(u);
    }
}

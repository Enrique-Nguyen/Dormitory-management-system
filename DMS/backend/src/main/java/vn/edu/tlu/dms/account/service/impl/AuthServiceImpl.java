package vn.edu.tlu.dms.account.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.api.AccountDtos.Me;
import vn.edu.tlu.dms.account.api.AccountDtos.Student;
import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.api.AccountDtos.UnitRef;
import vn.edu.tlu.dms.account.domain.AppUser;
import vn.edu.tlu.dms.account.repository.AppUserRepository;
import vn.edu.tlu.dms.account.repository.ManagementUnitRepository;
import vn.edu.tlu.dms.account.repository.StudentProfileRepository;
import vn.edu.tlu.dms.account.service.AuthService;
import vn.edu.tlu.dms.account.service.IdentityAccountService;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.ResidencyLookup;

import java.security.SecureRandom;
import java.util.Base64;

/** Hồ sơ DMS và thu hồi token; mật khẩu đăng nhập do Keycloak quản lý. */
@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository users;

    private final ManagementUnitRepository units;

    private final StudentProfileRepository students;

    private final CurrentUser currentUser;

    private final ResidencyLookup residency;

    private final IdentityAccountService identities;

    private final AuditService audit;

    private final EntityManager entityManager;

    private final SecureRandom random = new SecureRandom();

    public AuthServiceImpl(
            AppUserRepository users,
            ManagementUnitRepository units,
            StudentProfileRepository students,
            CurrentUser currentUser,
            ResidencyLookup residency,
            IdentityAccountService identities,
            AuditService audit,
            EntityManager entityManager) {
        this.users = users;
        this.units = units;
        this.students = students;
        this.currentUser = currentUser;
        this.residency = residency;
        this.identities = identities;
        this.audit = audit;
        this.entityManager = entityManager;
    }

    @Transactional
    @Override()
    public Me getCurrentUser() {
        AppUser u = users.findById(currentUser.get().id()).orElseThrow(BusinessException::notFound);
        if (u.getKeycloakUserId() != null && Boolean.TRUE.equals(u.getMustChangePassword())) {
            // Token được cấp sau khi người dùng hoàn tất đổi mật khẩu tại Keycloak.
            u.setMustChangePassword(false);
            users.saveAndFlush(u);
        }
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
                student,
                ScopeGuard.permissionsForRole(u.getRole()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public AppUser lockAccount(Long id) {
        var user = users.lockById(id).orElseThrow(BusinessException::notFound);
        // Khóa rồi refresh để không dùng dữ liệu cũ trong persistence context.
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        return user;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeSessions(AppUser user) {
        user.setSecurityVersion(Math.incrementExact(user.getSecurityVersion()));
        identities.synchronizeAccount(user);
        identities.revokeIdentitySessions(user);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public TemporaryPassword resetPassword(AppUser user, Long actorId) {
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        String temporary = "Kt7" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        user.setPasswordHash(null);
        user.setMustChangePassword(true);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        revokeSessions(user);
        users.saveAndFlush(user);
        identities.resetAccountPassword(user, temporary);
        identities.clearTemporaryLoginFailures(user);
        audit.record(
                actorId, AuditAction.USER_RESET_PASSWORD, "USER", user.getId(), null, null, null);
        return new TemporaryPassword(user.getId(), temporary);
    }

    @Override
    @Transactional
    public void revokeCurrentIdentitySessions() {
        var user = lockAccount(currentUser.get().id());
        currentUser.get();
        revokeSessions(user);
        users.saveAndFlush(user);
    }
}

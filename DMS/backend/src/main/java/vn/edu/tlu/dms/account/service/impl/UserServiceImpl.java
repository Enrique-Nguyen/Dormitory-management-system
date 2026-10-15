package vn.edu.tlu.dms.account.service.impl;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.api.AccountDtos.Create;
import vn.edu.tlu.dms.account.api.AccountDtos.Profile;
import vn.edu.tlu.dms.account.api.AccountDtos.ProfileInput;
import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.api.AccountDtos.Update;
import vn.edu.tlu.dms.account.domain.AppUser;
import vn.edu.tlu.dms.account.repository.AppUserRepository;
import vn.edu.tlu.dms.account.repository.StudentProfileRepository;
import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.account.service.AuthService;
import vn.edu.tlu.dms.account.service.IdentityAccountService;
import vn.edu.tlu.dms.account.service.UserService;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.PageQuery;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final AppUserRepository users;

    private final StudentProfileRepository students;

    private final AccountReferenceService references;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final AuditService audit;

    private final AuthService authService;

    private final IdentityAccountService identities;

    private static final SecureRandom RANDOM = new SecureRandom();

    public UserServiceImpl(
            AppUserRepository users,
            StudentProfileRepository students,
            AccountReferenceService references,
            CurrentUser current,
            ScopeGuard guard,
            AuditService audit,
            AuthService authService,
            IdentityAccountService identities) {
        this.users = users;
        this.students = students;
        this.references = references;
        this.current = current;
        this.guard = guard;
        this.audit = audit;
        this.authService = authService;
        this.identities = identities;
    }

    @Override
    public PageResponse<Profile> list(
            int page, int size, List<String> sort, String q, String role) {
        return list(page, size, sort, q, role, null, null);
    }

    @Override
    public PageResponse<Profile> list(
            int page,
            int size,
            List<String> sort,
            String q,
            String role,
            Long managementUnitId,
            Boolean locked) {
        guard.requirePermission(Permission.USER_READ);
        if (managementUnitId != null && managementUnitId <= 0) {
            throw BusinessException.invalid("managementUnitId", "ID đơn vị phải lớn hơn 0");
        }
        var pageable =
                PageQuery.of(
                        page,
                        size,
                        sort,
                        Set.of("id", "username", "fullName", "createdAt"),
                        "createdAt,desc");
        Specification<AppUser> spec =
                (r, query, b) -> {
                    var filters = new ArrayList<Predicate>();
                    if (role != null) {
                        filters.add(b.equal(r.get("role"), role));
                    }
                    if (managementUnitId != null) {
                        filters.add(b.equal(r.get("managementUnitId"), managementUnitId));
                    }
                    if (locked != null) {
                        filters.add(b.equal(r.get("locked"), locked));
                    }
                    if (q != null && !q.isBlank()) {
                        String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                        filters.add(
                                b.or(
                                        b.like(b.lower(r.get("username")), pattern),
                                        b.like(b.lower(r.get("fullName")), pattern)));
                    }
                    return b.and(filters.toArray(Predicate[]::new));
                };
        return PageResponse.from(users.findAll(spec, pageable).map(this::toUserProfileResponse));
    }

    @Override
    public Profile get(Long id) {
        guard.requirePermission(Permission.USER_READ);
        return toUserProfileResponse(findUserById(id));
    }

    @Override
    @Transactional
    public TemporaryPassword create(Create input) {
        guard.requirePermission(Permission.USER_CREATE);
        boolean unitRole = input.role().equals(Role.STAFF) || input.role().equals(Role.TECHNICIAN);
        if (unitRole) {
            references.requireUnit(input.managementUnitId());
        } else if (input.managementUnitId() != null) {
            throw BusinessException.invalid("managementUnitId", "Admin không gắn đơn vị");
        }
        var u = new AppUser();
        String temporary = generateTemporaryPassword();
        u.setUsername(input.username().trim());
        u.setFullName(trimRequiredText(input.fullName(), "fullName"));
        u.setPhone(input.phone());
        u.setRole(input.role());
        u.setManagementUnitId(input.managementUnitId());
        u.setCrossUnitAccess(false);
        u.setMustChangePassword(true);
        u.setLocked(false);
        u.setFailedLoginCount(0);
        u.setCreatedAt(OffsetDateTime.now());
        u.setPasswordHash(null);
        users.saveAndFlush(u);
        identities.provisionAccount(u, temporary);
        users.saveAndFlush(u);
        audit.record(
                current.get().id(),
                AuditAction.USER_CREATE,
                "USER",
                u.getId(),
                null,
                toUserProfileResponse(u),
                null);
        return new TemporaryPassword(u.getId(), temporary);
    }

    @Override
    @Transactional
    public Profile update(Long id, Update input) {
        guard.requirePermission(Permission.USER_UPDATE);
        var u = authService.lockAccount(id);
        var before = toUserProfileResponse(u);
        String previousUsername = u.getUsername();
        String previousRole = u.getRole();
        Long previousUnitId = u.getManagementUnitId();
        if (input.role() != null && !input.role().equals(previousRole)) {
            if (Role.STUDENT.equals(previousRole)) {
                throw BusinessException.invalid("role", "Hồ sơ sinh viên giữ vai trò STUDENT");
            }
            if (id.equals(current.get().id())) {
                throw BusinessException.invalid("role", "Không đổi vai trò tài khoản đang sử dụng");
            }
            u.setRole(input.role());
            if (Role.ADMIN.equals(input.role())) {
                // Admin hoạt động toàn hệ thống; bỏ phạm vi đơn vị/cờ tổng hợp của vai trò cũ.
                u.setManagementUnitId(null);
                u.setCrossUnitAccess(false);
            } else if (u.getManagementUnitId() == null && input.managementUnitId() == null) {
                throw BusinessException.invalid(
                        "managementUnitId", "Staff/KTV phải có đơn vị quản lý");
            }
            if (!Role.STAFF.equals(input.role())) {
                u.setCrossUnitAccess(false);
            }
        }
        if (input.fullName() != null) {
            u.setFullName(trimRequiredText(input.fullName(), "fullName"));
        }
        if (input.phone() != null) {
            u.setPhone(input.phone().trim());
        }
        if (input.managementUnitId() != null && !Role.ADMIN.equals(input.role())) {
            if (!Set.of(Role.STAFF, Role.TECHNICIAN).contains(u.getRole())) {
                throw BusinessException.invalid("managementUnitId", "Vai trò không có đơn vị");
            }
            references.requireUnit(input.managementUnitId());
            u.setManagementUnitId(input.managementUnitId());
        }
        var p = students.findById(id).orElse(null);
        if (p != null) {
            if (input.studentCode() != null) {
                p.setStudentCode(trimRequiredText(input.studentCode(), "studentCode"));
                u.setUsername(p.getStudentCode());
            }
            if (input.gender() != null) {
                p.setGender(input.gender());
            }
            if (input.dateOfBirth() != null) {
                p.setDateOfBirth(input.dateOfBirth());
            }
            if (input.faculty() != null) {
                p.setFaculty(input.faculty());
            }
            if (input.cohort() != null) {
                p.setCohort(input.cohort());
            }
            students.saveAndFlush(p);
        } else if (input.studentCode() != null
                || input.gender() != null
                || input.dateOfBirth() != null
                || input.faculty() != null
                || input.cohort() != null) {
            throw BusinessException.invalid("studentCode", "Chỉ áp dụng cho sinh viên");
        }
        // Phiên đã mở giữ thông tin đăng nhập/phạm vi cũ, nên phải đăng nhập lại khi đổi chúng.
        if (!Objects.equals(previousUsername, u.getUsername())
                || !Objects.equals(previousRole, u.getRole())
                || !Objects.equals(previousUnitId, u.getManagementUnitId())) {
            authService.revokeSessions(u);
        }
        users.saveAndFlush(u);
        identities.synchronizeAccount(u);
        audit.record(
                current.get().id(),
                AuditAction.USER_UPDATE,
                "USER",
                id,
                before,
                toUserProfileResponse(u),
                null);
        return toUserProfileResponse(u);
    }

    @Override
    @Transactional
    public void lock(Long id, String reason) {
        guard.requirePermission(Permission.USER_LOCK);
        if (id.equals(current.get().id())) {
            throw BusinessException.invalid("id", "Không tự khóa tài khoản đang dùng");
        }
        var u = authService.lockAccount(id);
        u.setLocked(true);
        authService.revokeSessions(u);
        users.saveAndFlush(u);
        identities.synchronizeAccount(u);
        audit.record(
                current.get().id(),
                AuditAction.USER_LOCK,
                "USER",
                id,
                null,
                Map.of("locked", true),
                reason.trim());
    }

    @Override
    @Transactional
    public void unlock(Long id) {
        guard.requirePermission(Permission.USER_UNLOCK);
        var u = authService.lockAccount(id);
        // Không hạ phiên bản bảo mật: mở khóa chỉ cho phép đăng nhập mới, phiên cũ vẫn bị thu hồi.
        u.setLocked(false);
        u.setLockedUntil(null);
        u.setFailedLoginCount(0);
        identities.clearTemporaryLoginFailures(u);
        users.saveAndFlush(u);
        identities.synchronizeAccount(u);
        audit.record(
                current.get().id(),
                AuditAction.USER_UNLOCK,
                "USER",
                id,
                null,
                Map.of("locked", false),
                null);
    }

    @Override
    @Transactional
    public TemporaryPassword resetPassword(Long id) {
        guard.requirePermission(Permission.USER_RESET_PASSWORD);
        var u = authService.lockAccount(id);
        return authService.resetPassword(u, current.get().id());
    }

    @Override
    @Transactional
    public void crossUnitAccess(Long id, boolean enabled) {
        guard.requirePermission(Permission.USER_CROSS_UNIT_ACCESS);
        var u = authService.lockAccount(id);
        if (!u.getRole().equals(Role.STAFF)) {
            throw BusinessException.conflict(ErrorCode.CROSS_UNIT_ONLY_STAFF);
        }
        boolean before = u.getCrossUnitAccess();
        u.setCrossUnitAccess(enabled);
        if (before != enabled) {
            authService.revokeSessions(u);
        }
        users.saveAndFlush(u);
        identities.synchronizeAccount(u);
        audit.record(
                current.get().id(),
                AuditAction.USER_CROSS_UNIT_ACCESS,
                "USER",
                id,
                before,
                enabled,
                null);
    }

    @Override
    public Profile profile() {
        guard.requirePermission(Permission.PROFILE_READ);
        return toUserProfileResponse(findUserById(current.get().id()));
    }

    @Override
    @Transactional
    public Profile updateProfile(ProfileInput input) {
        guard.requirePermission(Permission.PROFILE_UPDATE);
        var u = authService.lockAccount(current.get().id());
        // kiểm tra lại phiên sau khi chờ khóa tài khoản
        current.get();
        if (input.phone() != null) {
            u.setPhone(input.phone().trim());
        }
        var p = students.findById(u.getId()).orElse(null);
        if (p != null) {
            if (input.hometown() != null) {
                p.setHometown(input.hometown().trim());
            }
            if (input.emergencyContact() != null) {
                p.setEmergencyContact(input.emergencyContact().trim());
            }
            students.saveAndFlush(p);
        } else if (input.hometown() != null || input.emergencyContact() != null) {
            throw BusinessException.invalid("hometown", "Chỉ áp dụng cho sinh viên");
        }
        users.saveAndFlush(u);
        return toUserProfileResponse(u);
    }

    private Profile toUserProfileResponse(AppUser u) {
        var p = students.findById(u.getId()).orElse(null);
        return new Profile(
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getPhone(),
                u.getRole(),
                u.getManagementUnitId(),
                u.getLocked(),
                u.getCrossUnitAccess(),
                u.getMustChangePassword(),
                p == null ? null : p.getStudentCode(),
                p == null ? null : p.getGender(),
                p == null ? null : p.getDateOfBirth(),
                p == null ? null : p.getFaculty(),
                p == null ? null : p.getCohort(),
                p == null ? null : p.getHometown(),
                p == null ? null : p.getEmergencyContact());
    }

    private AppUser findUserById(Long id) {
        return users.findById(id).orElseThrow(BusinessException::notFound);
    }

    private String trimRequiredText(String value, String field) {
        if (value.isBlank()) {
            throw BusinessException.invalid(field, "Trường bắt buộc");
        }
        return value.trim();
    }

    private String generateTemporaryPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder value = new StringBuilder("Kt7");
        for (int i = 0; i < 13; i++) {
            value.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return value.toString();
    }
}

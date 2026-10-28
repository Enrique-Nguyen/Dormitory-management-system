package vn.edu.tlu.dms.account.service.impl;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.api.AccountDtos.Create;
import vn.edu.tlu.dms.account.api.AccountDtos.Profile;
import vn.edu.tlu.dms.account.api.AccountDtos.ProfileInput;
import vn.edu.tlu.dms.account.api.AccountDtos.Save;
import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.api.AccountDtos.Update;
import vn.edu.tlu.dms.account.domain.AppUser;
import vn.edu.tlu.dms.account.repository.AppUserRepository;
import vn.edu.tlu.dms.account.repository.StudentProfileRepository;
import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.account.service.AuthService;
import vn.edu.tlu.dms.account.service.IdentityAccountService;
import vn.edu.tlu.dms.account.service.UserService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.LikePattern;
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
    public PageResponse<Profile> searchByPage(SearchRequest input) {
        int page = input.page();
        int size = input.size();
        List<String> sort = input.sort();
        String q = input.q();
        String role = input.role();
        Long managementUnitId = input.managementUnitId();
        Boolean locked = input.locked();
        guard.requirePermission(Permission.USER_READ);
        if (managementUnitId != null && managementUnitId <= 0) {
            throw BusinessException.invalid(ApiField.MANAGEMENT_UNIT_ID, ValidationMessage.UNIT_ID_POSITIVE);
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
                        String pattern = LikePattern.contains(q.trim().toLowerCase(Locale.ROOT));
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
    public Object saveOrUpdate(Save input) {
        // Phân quyền theo nhánh trước khi khóa/ghi dữ liệu hoặc gọi Keycloak.
        if (input.id() == null) {
            guard.requirePermission(Permission.USER_CREATE);
            // DTO gộp không cho dùng trường hồ sơ sinh viên để tạo tài khoản cán bộ.
            rejectStudentFieldOnCreate("studentCode", input.studentCode());
            rejectStudentFieldOnCreate("gender", input.gender());
            rejectStudentFieldOnCreate("dateOfBirth", input.dateOfBirth());
            rejectStudentFieldOnCreate("faculty", input.faculty());
            rejectStudentFieldOnCreate("cohort", input.cohort());
            String username = trimRequiredText(input.username(), "username");
            String fullName = trimRequiredText(input.fullName(), "fullName");
            String role = trimRequiredText(input.role(), "role");
            return create(
                    new Create(username, fullName, input.phone(), role, input.managementUnitId()));
        }
        guard.requirePermission(Permission.USER_UPDATE);
        if (input.id() <= 0) {
            throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE);
        }
        // Tên đăng nhập bất biến ở API tài khoản; mã sinh viên có luồng kiểm soát riêng.
        if (input.username() != null) {
            throw BusinessException.invalid(
                    ApiField.USERNAME, ValidationMessage.USERNAME_IMMUTABLE);
        }
        return update(
                input.id(),
                new Update(
                        input.fullName(),
                        input.phone(),
                        input.role(),
                        input.managementUnitId(),
                        input.studentCode(),
                        input.gender(),
                        input.dateOfBirth(),
                        input.faculty(),
                        input.cohort()));
    }

    private TemporaryPassword create(Create input) {
        guard.requirePermission(Permission.USER_CREATE);
        boolean unitRole = input.role().equals(Role.STAFF) || input.role().equals(Role.TECHNICIAN);
        if (unitRole) {
            references.requireUnit(input.managementUnitId());
        } else if (input.managementUnitId() != null) {
            throw BusinessException.invalid(ApiField.MANAGEMENT_UNIT_ID, ValidationMessage.ADMIN_NO_UNIT);
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
                EntityType.USER,
                u.getId(),
                null,
                toUserProfileResponse(u),
                null);
        return new TemporaryPassword(u.getId(), temporary);
    }

    private Profile update(Long id, Update input) {
        guard.requirePermission(Permission.USER_UPDATE);
        var u = authService.lockAccount(id);
        var before = toUserProfileResponse(u);
        String previousUsername = u.getUsername();
        String previousRole = u.getRole();
        Long previousUnitId = u.getManagementUnitId();
        if (input.role() != null && !input.role().equals(previousRole)) {
            if (Role.STUDENT.equals(previousRole)) {
                throw BusinessException.invalid(ApiField.ROLE, ValidationMessage.STUDENT_ROLE_FIXED);
            }
            if (id.equals(current.get().id())) {
                throw BusinessException.invalid(ApiField.ROLE, ValidationMessage.ROLE_IN_USE_IMMUTABLE);
            }
            u.setRole(input.role());
            if (Role.ADMIN.equals(input.role())) {
                // Admin hoạt động toàn hệ thống; bỏ phạm vi đơn vị/cờ tổng hợp của vai trò cũ.
                u.setManagementUnitId(null);
                u.setCrossUnitAccess(false);
            } else if (u.getManagementUnitId() == null && input.managementUnitId() == null) {
                throw BusinessException.invalid(
                        ApiField.MANAGEMENT_UNIT_ID, ValidationMessage.UNIT_REQUIRED_FOR_ROLE);
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
                throw BusinessException.invalid(ApiField.MANAGEMENT_UNIT_ID, ValidationMessage.ROLE_HAS_NO_UNIT);
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
            throw BusinessException.invalid(ApiField.STUDENT_CODE, ValidationMessage.STUDENT_ONLY);
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
                EntityType.USER,
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
            throw BusinessException.invalid(ApiField.ID, ValidationMessage.SELF_LOCK_FORBIDDEN);
        }
        var u = authService.lockAccount(id);
        u.setLocked(true);
        authService.revokeSessions(u);
        users.saveAndFlush(u);
        identities.synchronizeAccount(u);
        audit.record(
                current.get().id(),
                AuditAction.USER_LOCK,
                EntityType.USER,
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
                EntityType.USER,
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
                EntityType.USER,
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
    public Profile saveOrUpdate(ProfileInput input) {
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
            throw BusinessException.invalid(ApiField.HOMETOWN, ValidationMessage.STUDENT_ONLY);
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
        if (value == null || value.isBlank()) {
            throw BusinessException.invalid(field, ValidationMessage.REQUIRED);
        }
        return value.trim();
    }

    /** Trả lỗi đúng tên trường khi client gửi dữ liệu sinh viên vào nhánh tạo cán bộ. */
    private void rejectStudentFieldOnCreate(String field, Object value) {
        if (value != null) {
            throw BusinessException.invalid(
                    field, ValidationMessage.STUDENT_FIELD_NOT_FOR_STAFF);
        }
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

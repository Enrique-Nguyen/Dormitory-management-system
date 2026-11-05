package vn.edu.tlu.dms.account.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.api.StudentManagementController.Save;
import vn.edu.tlu.dms.account.repository.AppUserRepository;
import vn.edu.tlu.dms.account.service.AuthService;
import vn.edu.tlu.dms.account.service.CatalogService;
import vn.edu.tlu.dms.account.service.IdentityAccountService;
import vn.edu.tlu.dms.account.service.StudentManagementService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
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
import vn.edu.tlu.dms.common.support.SqlPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Quản lý hồ sơ thật. Staff chỉ được xem/sửa sinh viên có lịch sử hoặc nguyện vọng trong đơn vị.
 */
@Service
@Transactional(readOnly = true)
public class StudentManagementServiceImpl implements StudentManagementService {

    private final JdbcTemplate jdbc;

    private final SqlPage queries;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final AuditService audit;

    private final AppUserRepository users;

    private final AuthService authService;

    private final IdentityAccountService identities;

    private final CatalogService catalogs;

    public StudentManagementServiceImpl(
            JdbcTemplate jdbc,
            SqlPage queries,
            CurrentUser current,
            ScopeGuard guard,
            AuditService audit,
            AppUserRepository users,
            AuthService authService,
            IdentityAccountService identities,
            CatalogService catalogs) {
        this.jdbc = jdbc;
        this.queries = queries;
        this.current = current;
        this.guard = guard;
        this.audit = audit;
        this.users = users;
        this.authService = authService;
        this.identities = identities;
        this.catalogs = catalogs;
    }

    private static final String SELECT =
            "SELECT"
                + " u.id,u.username,u.full_name,u.phone,u.locked,p.student_code,p.gender,p.date_of_birth,"
                + " p.school_id,p.faculty_id,p.cohort_id,p.hometown_id,s.name AS school_name,"
                + " f.name AS faculty,c.name AS cohort,h.name AS hometown,p.emergency_contact"
                + " FROM app_user u JOIN student_profile p ON p.user_id=u.id"
                + " LEFT JOIN school s ON s.id=p.school_id"
                + " LEFT JOIN faculty f ON f.id=p.faculty_id"
                + " LEFT JOIN student_cohort c ON c.id=p.cohort_id"
                + " LEFT JOIN hometown h ON h.id=p.hometown_id";

    private String scope() {
        if (current.get().canReadAllUnits()) {
            return "TRUE";
        }
        return unitScope();
    }

    private String unitScope() {
        return "(EXISTS (SELECT 1 FROM room_assignment_history a JOIN bed b ON b.id=a.bed_id JOIN"
                + " room r ON r.id=b.room_id JOIN building bd ON bd.id=r.building_id WHERE"
                + " a.student_id=u.id AND bd.management_unit_id=?) OR EXISTS (SELECT 1 FROM"
                + " room_application a WHERE a.student_id=u.id AND a.preferred_unit_id=?))";
    }

    private List<Object> scopeParameters() {
        return current.get().canReadAllUnits()
                ? new ArrayList<>()
                : new ArrayList<>(List.of(current.get().unitId(), current.get().unitId()));
    }

    @Override()
    public PageResponse<Map<String, Object>> searchByPage(SearchRequest input) {
        String q = input.q();
        guard.requirePermission(Permission.STUDENT_READ);
        var args = scopeParameters();
        args.add(LikePattern.contains(q.trim()));
        args.add(LikePattern.contains(q.trim()));
        var sql =
                new StringBuilder(SELECT)
                        .append(" WHERE ")
                        .append(scope())
                        .append(" AND (u.full_name ILIKE ? OR p.student_code ILIKE ?)");
        if (input.gender() != null && !input.gender().isBlank()) {
            sql.append(" AND p.gender=?");
            args.add(input.gender().trim());
        }
        if (input.schoolId() != null) {
            sql.append(" AND p.school_id=?");
            args.add(input.schoolId());
        }
        if (input.facultyId() != null) {
            sql.append(" AND p.faculty_id=?");
            args.add(input.facultyId());
        }
        if (input.cohortId() != null) {
            sql.append(" AND p.cohort_id=?");
            args.add(input.cohortId());
        }
        if (input.hometownId() != null) {
            sql.append(" AND p.hometown_id=?");
            args.add(input.hometownId());
        }
        if (input.locked() != null) {
            sql.append(" AND u.locked=?");
            args.add(input.locked());
        }
        if (input.managementUnitId() != null) {
            sql.append(" AND ").append(unitScope());
            args.add(input.managementUnitId());
            args.add(input.managementUnitId());
        }
        return queries.search(
                sql.toString(),
                args,
                input,
                Set.of(
                        "id",
                        "username",
                        "fullName",
                        "phone",
                        "locked",
                        "studentCode",
                        "gender",
                        "dateOfBirth",
                        ApiField.SCHOOL_ID,
                        ApiField.FACULTY_ID,
                        ApiField.COHORT_ID,
                        ApiField.HOMETOWN_ID,
                        ApiField.SCHOOL_NAME,
                        "faculty",
                        "cohort",
                        "hometown",
                        "emergencyContact"),
                "id,desc");
    }

    @Override()
    public Map<String, Object> getStudentById(Long id) {
        guard.requirePermission(Permission.STUDENT_READ);
        var args = scopeParameters();
        args.add(id);
        return queries.one(SELECT + " WHERE " + scope() + " AND u.id=?", args.toArray());
    }

    @Transactional
    @Override
    public Object saveOrUpdate(Save input) {
        if (input.id() == null) {
            guard.requirePermission(Permission.STUDENT_CREATE);
            // Chỉ Admin được tạo; quyền sửa hồ sơ của Staff không mở nhánh tạo.
            if (!current.get().isAdmin()) {
                throw BusinessException.forbidden();
            }
            requiredText(input.studentCode(), "studentCode");
            requiredText(input.fullName(), "fullName");
            requiredText(input.gender(), "gender");
            return createStudent(input);
        }
        guard.requirePermission(Permission.STUDENT_UPDATE);
        if (input.id() <= 0) {
            throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE);
        }
        return updateStudent(input.id(), input);
    }

    private TemporaryPassword createStudent(Save in) {
        guard.requirePermission(Permission.STUDENT_CREATE);
        var selected =
                StudentCatalogReferences.merge(
                        catalogs, new StudentCatalogReferences(null, null, null, null),
                        in.schoolId(), in.facultyId(), in.cohortId(), in.hometownId());
        String temporary = "Kt7" + UUID.randomUUID().toString().replace("-", "").substring(0, 13);
        Long id =
                jdbc.queryForObject(
                        "INSERT INTO app_user(username,password_hash,role,full_name,phone) VALUES"
                                + " (?,?,?,?,?) RETURNING id",
                        Long.class,
                        in.studentCode().trim(),
                        null,
                        Role.STUDENT,
                        in.fullName().trim(),
                        in.phone());
        jdbc.update(
                "INSERT INTO"
                    + " student_profile(user_id,student_code,gender,date_of_birth,school_id,faculty_id,cohort_id,hometown_id,emergency_contact)"
                    + " VALUES (?,?,?,?,?,?,?,?,?)",
                id,
                in.studentCode().trim(),
                in.gender(),
                in.dateOfBirth(),
                selected.schoolId(),
                selected.facultyId(),
                selected.cohortId(),
                selected.hometownId(),
                in.emergencyContact());
        var newUser = users.findById(Objects.requireNonNull(id)).orElseThrow(BusinessException::notFound);
        identities.provisionAccount(newUser, temporary);
        users.saveAndFlush(Objects.requireNonNull(newUser));
        audit.record(
                current.get().id(),
                AuditAction.STUDENT_CREATE,
                EntityType.USER,
                id,
                null,
                getStudentById(id),
                null);
        return new TemporaryPassword(id, temporary);
    }

    private Map<String, Object> updateStudent(Long id, Save in) {
        guard.requirePermission(Permission.STUDENT_UPDATE);
        var user = authService.lockAccount(id);
        var before = getStudentById(id);
        // Quyền tổng hợp không cấp quyền ghi. Phải có ít nhất một bản ghi thuộc đúng đơn vị.
        if (current.get().is(Role.STAFF)) {
            requireOwnStudent(id);
        }
        String studentCode = merged(in.studentCode(), before.get("studentCode"), "studentCode");
        String fullName = merged(in.fullName(), before.get("fullName"), "fullName");
        String gender = merged(in.gender(), before.get("gender"), "gender");
        // FR-ACC-04: quyền sửa hồ sơ trong đơn vị không cho Staff đổi danh tính sinh viên.
        if (!current.get().isAdmin()
                && (!studentCode.equals(before.get("studentCode"))
                        || !fullName.equals(before.get("fullName"))
                        || !gender.equals(before.get("gender")))) {
            throw BusinessException.forbidden();
        }
        if (!gender.equals(before.get("gender"))
                && Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                "SELECT EXISTS(SELECT 1 FROM room_assignment_history WHERE"
                                        + " student_id=? AND ended_at IS NULL)",
                                Boolean.class,
                                id))) {
            throw BusinessException.conflict(ErrorCode.STUDENT_HAS_ACTIVE_CONTRACT);
        }
        var selected =
                StudentCatalogReferences.merge(
                        catalogs,
                        new StudentCatalogReferences(
                                (Long) before.get(ApiField.SCHOOL_ID),
                                (Long) before.get(ApiField.FACULTY_ID),
                                (Long) before.get(ApiField.COHORT_ID),
                                (Long) before.get(ApiField.HOMETOWN_ID)),
                        in.schoolId(), in.facultyId(), in.cohortId(), in.hometownId());
        // Đổi mã sinh viên cũng đổi tên đăng nhập; thu hồi phiên chứa tên cũ trong cùng
        // transaction.
        if (!Objects.equals(user.getUsername(), studentCode)) {
            authService.revokeSessions(user);
        }
        user.setUsername(studentCode);
        user.setFullName(fullName);
        user.setPhone((String) keep(in.phone(), before.get("phone")));
        users.saveAndFlush(user);
        identities.synchronizeAccount(user);
        jdbc.update(
                "UPDATE student_profile SET"
                    + " student_code=?,gender=?,date_of_birth=?,school_id=?,faculty_id=?,cohort_id=?,hometown_id=?,emergency_contact=?"
                    + " WHERE user_id=?",
                studentCode,
                gender,
                keep(in.dateOfBirth(), before.get("dateOfBirth")),
                selected.schoolId(),
                selected.facultyId(),
                selected.cohortId(),
                selected.hometownId(),
                keep(in.emergencyContact(), before.get("emergencyContact")),
                id);
        var after = getStudentById(id);
        audit.record(
                current.get().id(),
                AuditAction.STUDENT_UPDATE,
                EntityType.USER,
                id,
                before,
                after,
                null);
        return after;
    }

    /** Trường bắt buộc: không gửi thì giữ giá trị cũ, gửi chuỗi trống thì báo lỗi. */
    private static String merged(String requested, Object stored, String field) {
        if (requested == null) {
            return String.valueOf(stored);
        }
        if (requested.isBlank()) {
            throw BusinessException.invalid(field, ValidationMessage.NOT_BLANK);
        }
        return requested.trim();
    }

    private static Object keep(Object requested, Object stored) {
        return requested != null ? requested : stored;
    }

    /**
     * Ghi cần ít nhất một hồ sơ/nguyện vọng thuộc đúng đơn vị của Staff, không tính quyền tổng hợp.
     */
    private void requireOwnStudent(Long id) {
        Long unit = current.get().unitId();
        boolean own =
                Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                "SELECT EXISTS(SELECT 1 FROM room_application WHERE"
                                        + " student_id=? AND preferred_unit_id=?) OR EXISTS(SELECT"
                                        + " 1 FROM room_assignment_history a JOIN bed b ON"
                                        + " b.id=a.bed_id JOIN room r ON r.id=b.room_id JOIN"
                                        + " building bd ON bd.id=r.building_id WHERE a.student_id=?"
                                        + " AND bd.management_unit_id=?)",
                                Boolean.class,
                                id,
                                unit,
                                id,
                                unit));
        if (!own) {
            throw BusinessException.notFound();
        }
    }

    @Transactional
    @Override()
    public void deleteUnusedStudent(Long id) {
        guard.requirePermission(Permission.STUDENT_DELETE);
        var before = getStudentById(id);
        // Khóa và kiểm tra lịch sử trước khi xóa; FK tiếp tục chặn mọi tham chiếu còn sót.
        jdbc.queryForObject("SELECT id FROM app_user WHERE id=? FOR UPDATE", Long.class, id);
        if (Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM room_application WHERE student_id=?) OR"
                                + " EXISTS(SELECT 1 FROM dormitory_contract WHERE student_id=?)",
                        Boolean.class,
                        id,
                        id))) {
            throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
        }
        var deletedUser = users.findById(Objects.requireNonNull(id)).orElseThrow(BusinessException::notFound);
        // Khóa danh tính trước; nếu xóa sau commit thất bại thì tài khoản ngoài vẫn không đăng nhập
        // được.
        boolean previouslyLocked = deletedUser.getLocked();
        deletedUser.setLocked(true);
        try {
            identities.synchronizeAccount(deletedUser);
            identities.revokeIdentitySessions(deletedUser);
        } finally {
            // Hàng sẽ bị xóa bằng JDBC; không để JPA flush một UPDATE lên hàng đã xóa.
            deletedUser.setLocked(previouslyLocked);
        }
        identities.deleteIdentityAfterCommit(deletedUser);
        jdbc.update("DELETE FROM student_profile WHERE user_id=?", id);
        jdbc.update("DELETE FROM app_user WHERE id=?", id);
        audit.record(
                current.get().id(),
                AuditAction.STUDENT_DELETE,
                EntityType.USER,
                id,
                before,
                null,
                null);
    }

    @Transactional
    @Override()
    public TemporaryPassword resetStudentPassword(Long id) {
        guard.requirePermission(Permission.STUDENT_RESET_PASSWORD);
        var user = authService.lockAccount(id);
        if (!Role.STUDENT.equals(user.getRole())
                || !Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                "SELECT EXISTS(SELECT 1 FROM student_profile WHERE user_id=?)",
                                Boolean.class,
                                id))) {
            throw BusinessException.notFound();
        }
        // BR-07: lịch sử cũ không đủ quyền reset; chỉ xét đơn trong đợt đang mở hoặc phân phòng
        // hiệu lực.
        boolean own =
                Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                "SELECT EXISTS(SELECT 1 FROM room_application ra JOIN"
                                    + " registration_period rp ON rp.id=ra.period_id WHERE"
                                    + " ra.student_id=? AND ra.preferred_unit_id=? AND"
                                    + " rp.open_at<=CURRENT_TIMESTAMP AND"
                                    + " rp.close_at>=CURRENT_TIMESTAMP) OR EXISTS(SELECT 1 FROM"
                                    + " room_assignment_history a JOIN bed b ON b.id=a.bed_id JOIN"
                                    + " room r ON r.id=b.room_id JOIN building bd ON"
                                    + " bd.id=r.building_id WHERE a.student_id=? AND"
                                    + " bd.management_unit_id=? AND a.ended_at IS NULL)",
                                Boolean.class,
                                id,
                                current.get().unitId(),
                                id,
                                current.get().unitId()));
        if (!current.get().isAdmin() && !own) {
            throw BusinessException.notFound();
        }
        return authService.resetPassword(user, current.get().id());
    }

    @Override
    public PageResponse<Map<String, Object>> searchTechniciansByPage(SearchRequest input) {
        guard.requirePermission(Permission.MAINTENANCE_UPDATE);
        // STUDENT cũng có MAINTENANCE_UPDATE (gửi/sửa ticket) nên phải khóa vai trò ở tầng service
        // để không lộ danh sách kỹ thuật viên cho sinh viên.
        guard.requireRole(Role.STAFF, Role.ADMIN);
        String sql =
                "SELECT u.id,u.full_name,u.username, (SELECT count(*) FROM maintenance_request m"
                        + " WHERE m.assigned_to=u.id AND m.status IN (?,?)) AS"
                        + " task_count FROM app_user u WHERE u.role=? AND NOT u.locked";
        var args = new ArrayList<Object>();
        args.add(MaintenanceStatus.ASSIGNED);
        args.add(MaintenanceStatus.IN_PROGRESS);
        args.add(Role.TECHNICIAN);
        if (!current.get().isAdmin()) {
            sql += " AND u.management_unit_id=?";
            args.add(current.get().unitId());
        }
        sql += " AND (u.full_name ILIKE ? OR u.username ILIKE ?)";
        String keyword = LikePattern.contains(input.q().trim());
        args.add(keyword);
        args.add(keyword);
        return queries.search(
                sql, args, input, Set.of("id", "fullName", "username", "taskCount"), "id,desc");
    }

    private String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.invalid(field, ValidationMessage.REQUIRED);
        }
        return value.trim();
    }
}

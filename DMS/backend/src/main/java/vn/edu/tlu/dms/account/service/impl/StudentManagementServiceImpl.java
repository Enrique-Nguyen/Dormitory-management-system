package vn.edu.tlu.dms.account.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.api.StudentManagementController.StudentInput;
import vn.edu.tlu.dms.account.api.StudentManagementController.StudentPatch;
import vn.edu.tlu.dms.account.service.StudentManagementService;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.SqlPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private final PasswordEncoder passwords;

    private final AuditService audit;

    public StudentManagementServiceImpl(
            JdbcTemplate jdbc,
            SqlPage queries,
            CurrentUser current,
            ScopeGuard guard,
            PasswordEncoder passwords,
            AuditService audit) {
        this.jdbc = jdbc;
        this.queries = queries;
        this.current = current;
        this.guard = guard;
        this.passwords = passwords;
        this.audit = audit;
    }

    private static final String SELECT =
            "SELECT"
                + " u.id,u.username,u.full_name,u.phone,u.locked,p.student_code,p.gender,p.date_of_birth,p.faculty,p.cohort,p.hometown,p.emergency_contact"
                + " FROM app_user u JOIN student_profile p ON p.user_id=u.id";

    private boolean seesAllUnits() {
        var actor = current.get();
        return actor.is("ADMIN") || actor.crossUnitAccess();
    }

    private String scope() {
        if (seesAllUnits()) {
            return "TRUE";
        }
        return "(EXISTS (SELECT 1 FROM room_assignment_history a JOIN bed b ON b.id=a.bed_id JOIN"
                + " room r ON r.id=b.room_id JOIN building bd ON bd.id=r.building_id WHERE"
                + " a.student_id=u.id AND bd.management_unit_id=?) OR EXISTS (SELECT 1 FROM"
                + " room_application a WHERE a.student_id=u.id AND a.preferred_unit_id=?))";
    }

    private List<Object> scopeParameters() {
        return seesAllUnits()
                ? new ArrayList<>()
                : new ArrayList<>(List.of(current.get().unitId(), current.get().unitId()));
    }

    @Override()
    public PageResponse<Map<String, Object>> searchStudents(int page, int size, String q) {
        guard.requireRole("ADMIN", "STAFF");
        var args = scopeParameters();
        args.add("%" + q.trim() + "%");
        args.add("%" + q.trim() + "%");
        return queries.search(
                SELECT
                        + " WHERE "
                        + scope()
                        + " AND (u.full_name ILIKE ? OR p.student_code ILIKE ?)",
                args,
                page,
                size);
    }

    @Override()
    public Map<String, Object> getStudentById(Long id) {
        guard.requireRole("ADMIN", "STAFF");
        var args = scopeParameters();
        args.add(id);
        return queries.one(SELECT + " WHERE " + scope() + " AND u.id=?", args.toArray());
    }

    @Transactional
    @Override()
    public TemporaryPassword createStudent(StudentInput in) {
        guard.requireRole("ADMIN");
        String temporary = "Kt7" + UUID.randomUUID().toString().replace("-", "").substring(0, 13);
        Long id =
                jdbc.queryForObject(
                        "INSERT INTO app_user(username,password_hash,role,full_name,phone) VALUES"
                                + " (?,?,'STUDENT',?,?) RETURNING id",
                        Long.class,
                        in.studentCode().trim(),
                        passwords.encode(temporary),
                        in.fullName().trim(),
                        in.phone());
        jdbc.update(
                "INSERT INTO"
                    + " student_profile(user_id,student_code,gender,date_of_birth,faculty,cohort,hometown,emergency_contact)"
                    + " VALUES (?,?,?,?,?,?,?,?)",
                id,
                in.studentCode().trim(),
                in.gender(),
                in.dateOfBirth(),
                in.faculty(),
                in.cohort(),
                in.hometown(),
                in.emergencyContact());
        audit.record(
                current.get().id(),
                "STUDENT_CREATE",
                "USER",
                id,
                null,
                Map.of("studentCode", in.studentCode()),
                null);
        return new TemporaryPassword(id, temporary);
    }

    @Transactional
    @Override()
    public Map<String, Object> updateStudent(Long id, StudentPatch in) {
        var before = getStudentById(id);
        // Quyền tổng hợp không cấp quyền ghi. Phải có ít nhất một bản ghi thuộc đúng đơn vị.
        if (current.get().is("STAFF")) {
            requireOwnStudent(id);
        }
        jdbc.queryForObject("SELECT id FROM app_user WHERE id=? FOR UPDATE", Long.class, id);
        String studentCode = merged(in.studentCode(), before.get("studentCode"), "studentCode");
        String fullName = merged(in.fullName(), before.get("fullName"), "fullName");
        String gender = merged(in.gender(), before.get("gender"), "gender");
        if (!gender.equals(before.get("gender"))
                && Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                "SELECT EXISTS(SELECT 1 FROM room_assignment_history WHERE"
                                        + " student_id=? AND ended_at IS NULL)",
                                Boolean.class,
                                id))) {
            throw BusinessException.conflict("STUDENT_HAS_ACTIVE_CONTRACT");
        }
        jdbc.update(
                "UPDATE app_user SET username=?,full_name=?,phone=? WHERE id=?",
                studentCode,
                fullName,
                keep(in.phone(), before.get("phone")),
                id);
        jdbc.update(
                "UPDATE student_profile SET"
                    + " student_code=?,gender=?,date_of_birth=?,faculty=?,cohort=?,hometown=?,emergency_contact=?"
                    + " WHERE user_id=?",
                studentCode,
                gender,
                keep(in.dateOfBirth(), before.get("dateOfBirth")),
                keep(in.faculty(), before.get("faculty")),
                keep(in.cohort(), before.get("cohort")),
                keep(in.hometown(), before.get("hometown")),
                keep(in.emergencyContact(), before.get("emergencyContact")),
                id);
        var after = getStudentById(id);
        audit.record(current.get().id(), "STUDENT_UPDATE", "USER", id, before, after, null);
        return after;
    }

    /** Trường bắt buộc: không gửi thì giữ giá trị cũ, gửi chuỗi trống thì báo lỗi. */
    private static String merged(String requested, Object stored, String field) {
        if (requested == null) {
            return String.valueOf(stored);
        }
        if (requested.isBlank()) {
            throw BusinessException.invalid(field, "Không để trống");
        }
        return requested.trim();
    }

    private static Object keep(Object requested, Object stored) {
        return requested != null ? requested : stored;
    }

    /** Ghi cần ít nhất một hồ sơ/nguyện vọng thuộc đúng đơn vị của Staff, không tính quyền tổng hợp. */
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
        guard.requireRole("ADMIN");
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
            throw BusinessException.conflict("RESOURCE_IN_USE");
        }
        jdbc.update("DELETE FROM student_profile WHERE user_id=?", id);
        jdbc.update("DELETE FROM app_user WHERE id=?", id);
        audit.record(current.get().id(), "STUDENT_DELETE", "USER", id, before, null, null);
    }

    @Transactional
    @Override()
    public TemporaryPassword resetStudentPassword(Long id) {
        guard.requireRole("STAFF");
        getStudentById(id);
        requireOwnStudent(id);
        String password = "Kt7" + UUID.randomUUID().toString().replace("-", "").substring(0, 13);
        jdbc.update(
                "UPDATE app_user SET password_hash=?,must_change_password=TRUE WHERE id=?",
                passwords.encode(password),
                id);
        audit.record(current.get().id(), "USER_RESET_PASSWORD", "USER", id, null, null, null);
        return new TemporaryPassword(id, password);
    }

    @Override()
    public List<Map<String, Object>> getTechnicians() {
        guard.requireRole("STAFF");
        return queries.list(
                "SELECT u.id,u.full_name,u.username, (SELECT count(*) FROM maintenance_request m"
                    + " WHERE m.assigned_to=u.id AND m.status IN ('ASSIGNED','IN_PROGRESS')) AS"
                    + " task_count FROM app_user u WHERE u.role='TECHNICIAN' AND NOT u.locked AND"
                    + " u.management_unit_id=? ORDER BY u.full_name",
                current.get().unitId());
    }
}

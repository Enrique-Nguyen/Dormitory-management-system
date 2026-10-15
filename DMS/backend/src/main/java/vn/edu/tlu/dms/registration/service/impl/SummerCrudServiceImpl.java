package vn.edu.tlu.dms.registration.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.RegistrationKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SummerChoice;
import vn.edu.tlu.dms.common.constants.DmsConstants.SummerStatus;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.impl.AbstractSqlCrudServiceImpl;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.registration.service.SummerCrudService;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/** CRUD lựa chọn hè đang chờ; xét duyệt/gia hạn và gửi đồ chưa nằm trong khung này. */
@Service
@Transactional(readOnly = true)
public class SummerCrudServiceImpl extends AbstractSqlCrudServiceImpl implements SummerCrudService {

    private final ScopeGuard guard;

    private static final Map<String, String> FIELDS =
            Map.of(
                    "contractId",
                    "contract_id:long",
                    "semesterId",
                    "semester_id:long",
                    "choice",
                    "choice:text");

    public SummerCrudServiceImpl(
            JdbcTemplate jdbc,
            SqlPage queries,
            CurrentUser current,
            AuditService audit,
            ScopeGuard guard) {
        super(jdbc, queries, current, audit);
        this.guard = guard;
    }

    @Override()
    public Object searchChoices(int page, int size, String q) {
        guard.requirePermission(Permission.SUMMER_REGISTRATION_READ);
        guard.requireRole(Role.STUDENT, Role.STAFF);
        var args = new ArrayList<Object>();
        String scope;
        if (current.get().is(Role.STUDENT)) {
            scope = "c.student_id=?";
            args.add(current.get().id());
        } else if (current.get().canReadAllUnits()) {
            scope = "TRUE";
        } else {
            scope = "a.preferred_unit_id=?";
            args.add(current.get().unitId());
        }
        args.add(LikePattern.contains(q));
        return queries.search(
                "SELECT sc.*,c.code AS contract_code,u.full_name FROM summer_choice sc JOIN"
                        + " dormitory_contract c ON c.id=sc.contract_id JOIN room_application a ON"
                        + " a.id=c.application_id JOIN app_user u ON u.id=c.student_id WHERE "
                        + scope
                        + " AND u.full_name ILIKE ?",
                args,
                page,
                size);
    }

    @Override()
    public Object getChoice(Long id) {
        guard.requirePermission(Permission.SUMMER_REGISTRATION_READ);
        var row = queries.one("SELECT * FROM summer_choice WHERE id=?", id);
        requireContract(number(row, "contractId"));
        return row;
    }

    private Map<String, Object> requireContract(Long id) {
        guard.requireRole(Role.STUDENT, Role.STAFF);
        var row =
                queries.one(
                        "SELECT c.*,a.preferred_unit_id FROM dormitory_contract c JOIN"
                                + " room_application a ON a.id=c.application_id WHERE c.id=?",
                        id);
        if (current.get().is(Role.STUDENT)) {
            if (!current.get().id().equals(row.get("studentId"))) {
                throw BusinessException.notFound();
            }
        } else {
            guard.requireReadableUnit(((Number) row.get("preferredUnitId")).longValue());
        }
        return row;
    }

    @Transactional
    @Override()
    public Object createChoice(Map<String, Object> input) {
        guard.requirePermission(Permission.SUMMER_REGISTRATION_CREATE);
        guard.requireRole(Role.STUDENT);
        var c = requireContract(number(input, "contractId"));
        if (!ContractStatus.ACTIVE.equals(c.get("status"))) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        String type =
                jdbc.queryForObject(
                        "SELECT type FROM semester WHERE id=?",
                        String.class,
                        number(input, "semesterId"));
        if (!RegistrationKind.SUMMER.equals(type)) {
            throw BusinessException.invalid("semesterId", "Chọn học kỳ hè");
        }
        validateChoice(input);
        return insertRecord(
                "summer_choice",
                FIELDS,
                Set.of("contractId", "semesterId", "choice"),
                input,
                Map.of("status", SummerStatus.PENDING));
    }

    @Transactional
    @Override()
    public Object updateChoice(Long id, Map<String, Object> input) {
        guard.requirePermission(Permission.SUMMER_REGISTRATION_UPDATE);
        loadEditable(id);
        if (input.size() != 1 || !input.containsKey("choice")) {
            throw BusinessException.invalid("choice", "Chỉ sửa lựa chọn");
        }
        validateChoice(input);
        return updateRecord("summer_choice", id, FIELDS, input);
    }

    @Transactional
    @Override()
    public void deleteChoice(Long id) {
        guard.requirePermission(Permission.SUMMER_REGISTRATION_DELETE);
        loadEditable(id);
        deleteRecord("summer_choice", id);
    }

    private void loadEditable(Long id) {
        guard.requireRole(Role.STUDENT);
        var row = queries.one("SELECT * FROM summer_choice WHERE id=? FOR UPDATE", id);
        requireContract(number(row, "contractId"));
        if (!SummerStatus.PENDING.equals(row.get("status"))) {
            throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
        }
    }

    private void validateChoice(Map<String, Object> input) {
        if (!SummerChoice.ALL.contains(input.getOrDefault("choice", ""))) {
            throw BusinessException.invalid("choice", "Lựa chọn không hợp lệ");
        }
    }

    private static Long number(Map<String, Object> input, String field) {
        try {
            return Long.valueOf(input.get(field).toString());
        } catch (RuntimeException e) {
            throw BusinessException.invalid(field, "Chọn bản ghi hợp lệ");
        }
    }
}

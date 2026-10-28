package vn.edu.tlu.dms.registration.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.constants.DmsConstants.SummerChoice;
import vn.edu.tlu.dms.common.constants.DmsConstants.SummerStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.service.impl.AbstractSqlCrudServiceImpl;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.contract.service.ContractService;
import vn.edu.tlu.dms.registration.api.SummerCrudController.Input;
import vn.edu.tlu.dms.registration.service.SummerCrudService;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * CRUD và xét duyệt lựa chọn hè. Lựa chọn STAY được ghi thành gia hạn SUMMER_STAY và cập nhật ngày
 * kết thúc hợp đồng trong cùng transaction; TEMP_LEAVE/LEAVE chỉ đổi trạng thái quyết định.
 */
@Service
@Transactional(readOnly = true)
public class SummerCrudServiceImpl extends AbstractSqlCrudServiceImpl implements SummerCrudService {

    private final ScopeGuard guard;

    private final StatusHistoryService history;

    private final ContractService contracts;

    private final RoomReferenceService rooms;

    /** Lý do từ chối phải dài tối thiểu số ký tự này để đủ thông tin cho sinh viên. */
    private static final int MIN_REASON_LENGTH = 5;

    /** Cột nghiệp vụ của lựa chọn hè; khai tường minh thay cho SELECT * để hợp đồng phản hồi rõ. */
    private static final String CHOICE_COLUMNS =
            "id, contract_id, semester_id, choice, status, decided_by, decided_at, created_at";

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
            ScopeGuard guard,
            StatusHistoryService history,
            ContractService contracts,
            RoomReferenceService rooms) {
        super(jdbc, queries, current, audit);
        this.guard = guard;
        this.history = history;
        this.contracts = contracts;
        this.rooms = rooms;
    }

    @Override()
    public PageResponse<Map<String, Object>> searchByPage(SearchRequest search, boolean mine) {
        guard.requirePermission(Permission.SUMMER_REGISTRATION_READ);
        guard.requireRole(
                mine ? new String[] {Role.STUDENT} : new String[] {Role.STUDENT, Role.STAFF});
        var args = new ArrayList<Object>();
        String scope;
        if (mine || current.get().is(Role.STUDENT)) {
            scope = "c.student_id=?";
            args.add(current.get().id());
        } else if (current.get().canReadAllUnits()) {
            scope = "TRUE";
        } else {
            scope = "a.preferred_unit_id=?";
            args.add(current.get().unitId());
        }
        args.add(LikePattern.contains(search.q()));
        String statusFilter = "";
        if (search.statuses() != null && !search.statuses().isEmpty()) {
            statusFilter =
                    " AND sc.status IN ("
                            + String.join(
                                    ",",
                                    Collections.nCopies(search.statuses().size(), "?"))
                            + ")";
            args.addAll(search.statuses());
        }
        return queries.search(
                "SELECT sc.*,c.student_id AS student_id,c.code AS contract_code,u.full_name FROM"
                        + " summer_choice sc JOIN dormitory_contract c ON c.id=sc.contract_id JOIN"
                        + " room_application a ON a.id=c.application_id JOIN app_user u ON"
                        + " u.id=c.student_id WHERE "
                        + scope
                        + " AND u.full_name ILIKE ?"
                        + statusFilter,
                args,
                search,
                Set.of(
                        "id",
                        "contractId",
                        "semesterId",
                        "choice",
                        "status",
                        "createdAt",
                        "contractCode",
                        "fullName"),
                "id,desc");
    }

    @Override()
    public Map<String, Object> getChoice(Long id) {
        guard.requirePermission(Permission.SUMMER_REGISTRATION_READ);
        var row = loadChoice(id);
        return toDetail(row);
    }

    @Override()
    public Map<String, Object> getMyCurrentChoice() {
        guard.requirePermission(Permission.SUMMER_REGISTRATION_READ);
        guard.requireRole(Role.STUDENT);
        var ids =
                jdbc.query(
                        """
                        SELECT sc.id
                        FROM summer_choice sc
                        JOIN dormitory_contract c ON c.id = sc.contract_id
                        WHERE c.student_id = ?
                        ORDER BY sc.created_at DESC, sc.id DESC
                        LIMIT 1
                        """,
                        (row, index) -> row.getLong(1),
                        current.get().id());
        return ids.isEmpty() ? null : getChoice(ids.get(0));
    }

    /**
     * Đọc bản ghi lựa chọn hè cùng hợp đồng nguồn; phạm vi đơn vị và chủ sở hữu được kiểm tra
     * trong {@code requireContract} trước khi trả bất kỳ dữ liệu nào.
     */
    private Map<String, Object> loadChoice(Long id) {
        var row = queries.one("SELECT " + CHOICE_COLUMNS + " FROM summer_choice WHERE id=?", id);
        attachContract(row, requireContract(number(row, ApiField.CONTRACT_ID)));
        return row;
    }

    /** Khóa lựa chọn hè trước khi xét duyệt rồi đọc lại hợp đồng nguồn sau khi đã có khóa. */
    private Map<String, Object> lockChoice(Long id) {
        var row =
                queries.one(
                        "SELECT " + CHOICE_COLUMNS + " FROM summer_choice WHERE id=? FOR UPDATE",
                        id);
        attachContract(row, requireContract(number(row, ApiField.CONTRACT_ID)));
        return row;
    }

    /**
     * Gắn thông tin hợp đồng nguồn phục vụ nghiệp vụ gia hạn và màn chi tiết: sinh viên, đơn vị
     * nguyện vọng, trạng thái, mã và thời hạn hợp đồng.
     */
    private static void attachContract(Map<String, Object> row, Map<String, Object> contract) {
        row.put(ApiField.STUDENT_ID, number(contract, ApiField.STUDENT_ID));
        row.put("preferredUnitId", number(contract, "preferredUnitId"));
        row.put(ApiField.CONTRACT_STATUS, contract.get(ApiField.STATUS));
        row.put(ApiField.CONTRACT_CODE, contract.get("code"));
        row.put(ApiField.CONTRACT_START_DATE, contract.get(ApiField.START_DATE));
        row.put(ApiField.CONTRACT_END_DATE, contract.get(ApiField.END_DATE));
    }

    /**
     * Dựng phản hồi chi tiết: bản ghi lựa chọn, hợp đồng nguồn, mã và tên sinh viên, tên học kỳ hè,
     * lý do quyết định lấy từ lịch sử trạng thái, hành động được phép và timeline. Bảng
     * {@code summer_choice} không có cột ghi chú của sinh viên cũng như thời điểm nộp nên phản hồi
     * chỉ trả {@code createdAt} và {@code decisionReason}, không đặt tên trường cho cột không tồn tại.
     */
    private Map<String, Object> toDetail(Map<String, Object> row) {
        var detail = new LinkedHashMap<>(row);
        var entries = history.list(EntityType.SUMMER_CHOICE, number(row, ApiField.ID));
        attachStudentAndSemester(detail);
        detail.put(ApiField.DECISION_REASON, lastDecisionReason(entries));
        detail.put("allowedActions", allowedActions(row));
        detail.put("timeline", entries);
        return detail;
    }

    /**
     * Bổ sung thông tin sinh viên và học kỳ hè cho màn chi tiết; các giá trị đọc từ
     * {@code student_profile}, {@code app_user} và {@code semester} theo liên kết sẵn có của lựa
     * chọn, không suy diễn dữ liệu mới.
     */
    private void attachStudentAndSemester(Map<String, Object> detail) {
        var reference =
                queries.one(
                        """
                        SELECT COALESCE(sp.student_code, u.username) AS student_code,
                               u.full_name AS student_name,
                               s.name AS semester_name
                        FROM summer_choice sc
                        JOIN dormitory_contract c ON c.id = sc.contract_id
                        JOIN app_user u ON u.id = c.student_id
                        LEFT JOIN student_profile sp ON sp.user_id = u.id
                        JOIN semester s ON s.id = sc.semester_id
                        WHERE sc.id = ?
                        """,
                        number(detail, ApiField.ID));
        detail.put(ApiField.STUDENT_CODE, reference.get("studentCode"));
        detail.put(ApiField.STUDENT_NAME, reference.get("studentName"));
        detail.put(ApiField.FULL_NAME, reference.get("studentName"));
        detail.put(ApiField.SEMESTER_NAME, reference.get("semesterName"));
    }

    /**
     * Lý do quyết định gần nhất của lựa chọn hè. Nghiệp vụ lưu lý do duyệt/từ chối vào
     * {@code status_history} tại thời điểm đổi trạng thái nên phản hồi đọc lại từ lịch sử.
     */
    private static String lastDecisionReason(List<StatusHistoryService.Entry> entries) {
        for (int index = entries.size() - 1; index >= 0; index--) {
            String reason = entries.get(index).reason();
            if (reason != null && !reason.isBlank()) {
                return reason;
            }
        }
        return null;
    }

    /**
     * Hành động quảng bá theo trạng thái, vai trò, đơn vị và quyền đang cấp. Backend luôn kiểm tra
     * lại khi thực hiện, danh sách này chỉ phục vụ giao diện.
     */
    private List<String> allowedActions(Map<String, Object> row) {
        if (!SummerStatus.PENDING.equals(row.get("status"))) {
            return List.of();
        }
        var actor = current.get();
        var actions = new ArrayList<String>();
        if (actor.isAdmin() || actor.is(Role.STAFF)) {
            if (!actor.isAdmin()
                    && !Objects.equals(actor.unitId(), number(row, "preferredUnitId"))) {
                return List.of();
            }
            if (guard.hasPermission(Permission.SUMMER_REGISTRATION_UPDATE)) {
                actions.add(BusinessAction.APPROVE);
                actions.add(BusinessAction.REJECT);
            }
            return actions;
        }
        if (actor.is(Role.STUDENT) && actor.id().equals(number(row, "studentId"))) {
            if (guard.hasPermission(Permission.SUMMER_REGISTRATION_UPDATE)) {
                actions.add(BusinessAction.EDIT);
            }
            if (guard.hasPermission(Permission.SUMMER_REGISTRATION_DELETE)) {
                actions.add(BusinessAction.CANCEL);
            }
        }
        return actions;
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

    @Override
    @Transactional
    public Object saveOrUpdate(Input input) {
        Long id = input.id();
        if (id != null && id <= 0) {
            throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE);
        }
        var fields = new LinkedHashMap<String, Object>();
        if (input.contractId() != null) {
            fields.put("contractId", input.contractId());
        }
        if (input.semesterId() != null) {
            fields.put("semesterId", input.semesterId());
        }
        if (input.choice() != null) {
            fields.put("choice", input.choice());
        }
        return id == null ? createChoice(fields) : updateChoice(id, fields);
    }

    private Object createChoice(Map<String, Object> input) {
        guard.requirePermission(Permission.SUMMER_REGISTRATION_CREATE);
        guard.requireRole(Role.STUDENT);
        var c = requireContract(number(input, "contractId"));
        if (!ContractStatus.ACTIVE.equals(c.get("status"))) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        var semester =
                queries.one("SELECT type FROM semester WHERE id=?", number(input, "semesterId"));
        if (!SemesterType.SUMMER.equals(semester.get("type"))) {
            throw BusinessException.invalid(
                    ApiField.SEMESTER_ID, ValidationMessage.SUMMER_SEMESTER_REQUIRED);
        }
        validateChoice(input);
        var created =
                insertRecord(
                        "summer_choice",
                        FIELDS,
                        Set.of("contractId", "semesterId", "choice"),
                        input,
                        Map.of("status", SummerStatus.PENDING));
        history.record(
                EntityType.SUMMER_CHOICE,
                number(asRow(created), ApiField.ID),
                null,
                SummerStatus.PENDING,
                current.get().id(),
                null);
        return created;
    }

    /** Bản ghi do tầng CRUD trả về dưới dạng Object; ép kiểu một lần tại chỗ dùng. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asRow(Object record) {
        return (Map<String, Object>) record;
    }

    private Object updateChoice(Long id, Map<String, Object> input) {
        guard.requirePermission(Permission.SUMMER_REGISTRATION_UPDATE);
        loadEditable(id);
        if (input.size() != 1 || !input.containsKey(ApiField.CHOICE)) {
            throw BusinessException.invalid(
                    ApiField.CHOICE, ValidationMessage.SUMMER_CHOICE_EDIT);
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

    @Transactional
    @Override()
    public Object approve(Long id) {
        return decide(id, null, true);
    }

    @Transactional
    @Override()
    public Object reject(Long id, String reason) {
        if (reason == null || reason.trim().length() < MIN_REASON_LENGTH) {
            throw BusinessException.invalid(
                    ApiField.REASON, ValidationMessage.REJECT_REASON_REQUIRED);
        }
        return decide(id, reason.trim(), false);
    }

    /**
     * Khóa lựa chọn trước, kiểm tra trạng thái sau khóa rồi mới tác động hợp đồng (STAY) và ghi
     * trạng thái/audit trong cùng transaction.
     */
    private Object decide(Long id, String reason, boolean approve) {
        guard.requirePermission(Permission.SUMMER_REGISTRATION_UPDATE);
        guard.requireRole(Role.STAFF);
        var choice = lockChoice(id);
        guard.requireUnit(number(choice, "preferredUnitId"));
        String before = (String) choice.get("status");
        if (!SummerStatus.PENDING.equals(before)) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        String option = (String) choice.get("choice");
        if (approve) {
            if (SummerChoice.STAY.equals(option)) {
                extendContractForSummer(choice, reason);
            } else if (!SummerChoice.ALL.contains(option)) {
                throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
            }
        }
        String after = approve ? SummerStatus.APPROVED : SummerStatus.REJECTED;
        Long actorId = current.get().id();
        jdbc.update(
                "UPDATE summer_choice SET status=?,decided_by=?,decided_at=? WHERE id=?",
                after,
                actorId,
                OffsetDateTime.now(),
                id);
        history.record(EntityType.SUMMER_CHOICE, id, before, after, actorId, reason);
        audit.record(
                actorId,
                approve ? AuditAction.SUMMER_CHOICE_APPROVE : AuditAction.SUMMER_CHOICE_REJECT,
                EntityType.SUMMER_CHOICE,
                id,
                Map.of(ApiField.STATUS, before, ApiField.CHOICE, option),
                Map.of(ApiField.STATUS, after, ApiField.DECIDED_BY, actorId),
                reason);
        return getChoice(id);
    }

    /**
     * Ghi nhận gia hạn hè nguồn SUMMER_STAY trong cùng transaction. Cổng hợp đồng hiện thực hiện
     * luôn việc cập nhật end_date bên trong {@code recordSummerExtension}; gọi thêm
     * {@code extendEndDate} sẽ gia hạn hai lần và bị chặn vì ngày mới không còn lớn hơn ngày cũ.
     */
    private void extendContractForSummer(Map<String, Object> choice, String reason) {
        if (!ContractStatus.ACTIVE.equals(choice.get(ApiField.CONTRACT_STATUS))) {
            throw BusinessException.conflict(ErrorCode.CONTRACT_NOT_ACTIVE);
        }
        var semester = rooms.getSemesterReference(number(choice, "semesterId"));
        if (!SemesterType.SUMMER.equals(semester.type())) {
            throw BusinessException.invalid(
                    ApiField.SEMESTER_ID, ValidationMessage.SUMMER_SEMESTER_REQUIRED);
        }
        LocalDate contractEnd = (LocalDate) choice.get("contractEndDate");
        if (contractEnd == null || !semester.endDate().isAfter(contractEnd)) {
            throw BusinessException.invalid(
                    ApiField.SEMESTER_ID, ValidationMessage.SUMMER_SEMESTER_AFTER_CONTRACT);
        }
        Long contractId = number(choice, "contractId");
        Long actorId = current.get().id();
        contracts.recordSummerExtension(contractId, semester.endDate(), actorId, reason);
    }

    private void loadEditable(Long id) {
        guard.requireRole(Role.STUDENT);
        var row =
                queries.one(
                        "SELECT " + CHOICE_COLUMNS + " FROM summer_choice WHERE id=? FOR UPDATE",
                        id);
        requireContract(number(row, ApiField.CONTRACT_ID));
        if (!SummerStatus.PENDING.equals(row.get(ApiField.STATUS))) {
            throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
        }
    }

    private void validateChoice(Map<String, Object> input) {
        if (!SummerChoice.ALL.contains(input.getOrDefault(ApiField.CHOICE, ""))) {
            throw BusinessException.invalid(
                    ApiField.CHOICE, ValidationMessage.SUMMER_CHOICE_INVALID);
        }
    }

    private static Long number(Map<String, Object> input, String field) {
        try {
            return Long.valueOf(input.get(field).toString());
        } catch (RuntimeException e) {
            throw BusinessException.invalid(field, ValidationMessage.RECORD_INVALID);
        }
    }
}

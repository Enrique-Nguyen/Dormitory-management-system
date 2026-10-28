package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ConfigKey;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.StayRequestStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.ConfigService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.service.impl.AbstractSqlCrudServiceImpl;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.contract.api.ContractDtos;
import vn.edu.tlu.dms.contract.service.ContractCrudService;
import vn.edu.tlu.dms.contract.service.ContractService;
import vn.edu.tlu.dms.room.service.OccupancyQuery;
import vn.edu.tlu.dms.room.service.OccupancyQuery.CurrentBed;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** CRUD hồ sơ PENDING; kích hoạt, chuyển giường, thu/hoàn cọc sẽ được triển khai riêng về sau. */
@Service
@Transactional(readOnly = true)
public class ContractCrudServiceImpl extends AbstractSqlCrudServiceImpl
        implements ContractCrudService {

    private static final String SELECT_EXTENSION =
            """
            SELECT e.id, e.contract_id, c.code AS contract_code, c.student_id,
                   c.start_date, c.end_date, c.status AS contract_status,
                   sp.student_code, u.full_name, e.source, e.old_end_date, e.new_end_date,
                   e.reason, e.status, e.decided_by, e.decided_at, e.decision_reason,
                   e.created_at, a.preferred_unit_id
            FROM contract_extension_request e
            JOIN dormitory_contract c ON c.id = e.contract_id
            JOIN room_application a ON a.id = c.application_id
            JOIN app_user u ON u.id = c.student_id
            LEFT JOIN student_profile sp ON sp.user_id = c.student_id
            """;

    /** Khóa mềm hợp đồng kèm ngày/trạng thái để kiểm tra lại sau khi khóa. */
    private static final String SELECT_CONTRACT_FOR_UPDATE =
            """
            SELECT id, student_id, start_date, end_date, status
            FROM dormitory_contract WHERE id = ? FOR UPDATE
            """;

    /** Quyền cần có để gọi endpoint của từng hành động trong allowedActions. */
    private static final Map<String, String> EXTENSION_ACTION_PERMISSIONS =
            Map.of(
                    BusinessAction.APPROVE, Permission.CONTRACT_EXTENSION_UPDATE,
                    BusinessAction.REJECT, Permission.CONTRACT_EXTENSION_UPDATE,
                    BusinessAction.CANCEL, Permission.CONTRACT_EXTENSION_DELETE);

    private final ScopeGuard guard;

    private final ConfigService configs;

    private final OccupancyQuery occupancy;

    private final StatusHistoryService history;

    private final ContractService contracts;

    private static final Map<String, String> CONTRACT =
            Map.of(
                    "applicationId",
                    "application_id:long",
                    "startDate",
                    "start_date:date",
                    "endDate",
                    "end_date:date");

    private static final Map<String, String> EXTENSION =
            Map.of(
                    "contractId",
                    "contract_id:long",
                    "newEndDate",
                    "new_end_date:date",
                    "reason",
                    "reason:text");

    private static final Map<String, String> TRANSFER =
            Map.of("contractId", "contract_id:long", "reason", "reason:text");

    public ContractCrudServiceImpl(
            JdbcTemplate jdbc,
            SqlPage queries,
            CurrentUser current,
            AuditService audit,
            ScopeGuard guard,
            ConfigService configs,
            OccupancyQuery occupancy,
            StatusHistoryService history,
            ContractService contracts) {
        super(jdbc, queries, current, audit);
        this.guard = guard;
        this.configs = configs;
        this.occupancy = occupancy;
        this.history = history;
        this.contracts = contracts;
    }

    /** Phạm vi lấy từ đơn nguyện vọng; ADMIN quản lý mọi đơn vị nhưng vẫn giữ trạng thái hợp lệ. */
    private Map<String, Object> requireContract(Long id, boolean writing) {
        guard.requireRole(Role.STAFF, Role.STUDENT);
        var row =
                queries.one(
                        "SELECT c.*,a.preferred_unit_id FROM dormitory_contract c JOIN"
                                + " room_application a ON a.id=c.application_id WHERE c.id=?",
                        id);
        if (current.get().is(Role.STUDENT)) {
            if (!current.get().id().equals(row.get("studentId"))) {
                throw BusinessException.notFound();
            }
        } else if (writing) {
            guard.requireUnit(((Number) row.get("preferredUnitId")).longValue());
        } else {
            guard.requireReadableUnit(((Number) row.get("preferredUnitId")).longValue());
        }
        return row;
    }

    @Transactional
    @Override
    public Object saveOrUpdateContract(ContractDtos.Save input) {
        // Annotation OR chỉ mở cổng; quyền chính xác phải kiểm tra trước đọc/ghi và sinh mã.
        guard.requirePermission(
                input.id() == null ? Permission.CONTRACT_CREATE : Permission.CONTRACT_UPDATE);
        requirePositiveId(input.id());
        var values = new LinkedHashMap<String, Object>();
        putIfPresent(values, "applicationId", input.applicationId());
        putIfPresent(values, "startDate", input.startDate());
        putIfPresent(values, "endDate", input.endDate());
        return input.id() == null
                ? createPendingContract(values)
                : updatePendingContract(input.id(), values);
    }

    private Object createPendingContract(Map<String, Object> input) {
        guard.requirePermission(Permission.CONTRACT_CREATE);
        guard.requireRole(Role.STAFF);
        Long applicationId = number(input, "applicationId");
        var application =
                queries.one("SELECT * FROM room_application WHERE id=? FOR UPDATE", applicationId);
        guard.requireUnit(((Number) application.get("preferredUnitId")).longValue());
        if (!ApplicationStatus.APPROVED.equals(application.get("status"))) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        validateDates(input, null);
        String code =
                jdbc.queryForObject(
                        "SELECT fn_next_code('HD','seq_contract_code',true)", String.class);
        return insertRecord(
                "dormitory_contract",
                CONTRACT,
                Set.of("applicationId", "startDate", "endDate"),
                input,
                Map.of(
                        "student_id",
                        application.get("studentId"),
                        "code",
                        code,
                        "status",
                        ContractStatus.PENDING,
                        "activation_deadline",
                        OffsetDateTime.now()
                                .plusDays(
                                        configs.positiveInt(ConfigKey.CONTRACT_ACTIVATION_DAYS))));
    }

    private Object updatePendingContract(Long id, Map<String, Object> input) {
        guard.requirePermission(Permission.CONTRACT_UPDATE);
        guard.requireRole(Role.STAFF);
        queries.one("SELECT id FROM dormitory_contract WHERE id=? FOR UPDATE", id);
        var row = requireContract(id, true);
        requirePending(row, ContractStatus.PENDING);
        requireNoAssignment(id);
        validateDates(input, row);
        if (input.containsKey("applicationId")) {
            throw BusinessException.invalid(ApiField.APPLICATION_ID, ValidationMessage.CONTRACT_APPLICATION_IMMUTABLE);
        }
        return updateRecord("dormitory_contract", id, CONTRACT, input);
    }

    @Transactional
    @Override
    public void deletePendingContract(Long id) {
        guard.requirePermission(Permission.CONTRACT_DELETE);
        guard.requireRole(Role.STAFF);
        queries.one("SELECT id FROM dormitory_contract WHERE id=? FOR UPDATE", id);
        requirePending(requireContract(id, true), ContractStatus.PENDING);
        requireNoAssignment(id);
        if (Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM status_history WHERE entity_type='CONTRACT'"
                                + " AND entity_id=?)",
                        Boolean.class,
                        id))) {
            throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
        }
        deleteRecord("dormitory_contract", id);
    }

    private void requireNoAssignment(Long id) {
        if (Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM room_assignment_history WHERE contract_id=?)"
                            + " OR EXISTS(SELECT 1 FROM deposit_transaction WHERE contract_id=?)",
                        Boolean.class,
                        id,
                        id))) {
            throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
        }
    }

    private void validateDates(Map<String, Object> input, Map<String, Object> old) {
        try {
            LocalDate start =
                    LocalDate.parse(
                            input.getOrDefault("startDate", old == null ? "" : old.get("startDate"))
                                    .toString());
            LocalDate end =
                    LocalDate.parse(
                            input.getOrDefault("endDate", old == null ? "" : old.get("endDate"))
                                    .toString());
            if (!end.isAfter(start)) {
                throw BusinessException.invalid(ApiField.END_DATE, ValidationMessage.END_DATE_AFTER_START);
            }
        } catch (DateTimeParseException | NullPointerException e) {
            throw BusinessException.invalid(ApiField.START_DATE, ValidationMessage.DATE_FORMAT_ISO);
        }
    }

    @Override
    public PageResponse<Map<String, Object>> searchStayRequestsByPage(boolean extension, SearchRequest input, boolean mine) {
        guard.requirePermission(
                extension ? Permission.CONTRACT_EXTENSION_READ : Permission.ROOM_TRANSFER_READ);
        guard.requireRole(Role.STUDENT, Role.STAFF);
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
        args.add(LikePattern.contains(input.q()));
        args.add(LikePattern.contains(input.q()));
        String statusFilter = "";
        if (input.statuses() != null && !input.statuses().isEmpty()) {
            statusFilter =
                    " AND req.status IN ("
                            + String.join(",", Collections.nCopies(input.statuses().size(), "?"))
                            + ")";
            args.addAll(input.statuses());
        }
        return queries.search(
                "SELECT req.*,c.code AS contract_code,u.full_name FROM "
                        + table(extension)
                        + " req JOIN dormitory_contract c ON c.id=req.contract_id JOIN"
                        + " room_application a ON a.id=c.application_id JOIN app_user u ON"
                        + " u.id=c.student_id WHERE "
                        + scope
                        + " AND (u.full_name ILIKE ? OR c.code ILIKE ?)"
                        + statusFilter,
                args,
                input,
                extension
                        ? Set.of(
                                "id",
                                "contractCode",
                                "fullName",
                                "status",
                                "createdAt",
                                "newEndDate")
                        : Set.of("id", "contractCode", "fullName", "status", "createdAt"),
                "id,desc");
    }

    @Override
    public ContractDtos.ExtensionDetail getExtensionDetail(Long id) {
        guard.requirePermission(Permission.CONTRACT_EXTENSION_READ);
        return toExtensionDetail(loadExtension(id, false));
    }

    @Override
    @Transactional
    public ContractDtos.ExtensionDetail approveExtension(Long id) {
        guard.requirePermission(Permission.CONTRACT_EXTENSION_UPDATE);
        guard.requireRole(Role.STAFF);
        ExtensionContext context = loadExtension(id, true);
        ExtensionRow row = lockExtensionRequest(id);
        ContractLock contract = lockContract(row.contractId());
        if (!ContractStatus.ACTIVE.equals(contract.status())) {
            throw BusinessException.conflict(ErrorCode.CONTRACT_NOT_ACTIVE);
        }
        if (!row.newEndDate().isAfter(contract.endDate())) {
            throw BusinessException.invalid(
                    ApiField.NEW_END_DATE, ValidationMessage.EXTENSION_DATE_AFTER_CURRENT);
        }
        String overlapping =
                findOverlappingContract(
                        contract.studentId(),
                        contract.id(),
                        contract.startDate(),
                        row.newEndDate());
        if (overlapping != null) {
            throw new BusinessException(
                    ErrorCode.CONTRACT_PERIOD_OVERLAP,
                    HttpStatus.CONFLICT,
                    Map.of(ApiField.CONTRACT_CODE, overlapping));
        }

        contracts.extendEndDate(contract.id(), row.newEndDate(), row.reason());
        Actor actor = current.get();
        int updated =
                jdbc.update(
                        "UPDATE contract_extension_request SET status=?, decided_by=?,"
                                + " decided_at=CURRENT_TIMESTAMP WHERE id=? AND status=?",
                        StayRequestStatus.APPROVED,
                        actor.id(),
                        id,
                        StayRequestStatus.PENDING);
        requireUpdated(updated);
        history.record(
                EntityType.EXTENSION,
                id,
                StayRequestStatus.PENDING,
                StayRequestStatus.APPROVED,
                actor.id(),
                row.reason());
        audit.record(
                actor.id(),
                AuditAction.CONTRACT_EXTENSION_APPROVE,
                EntityType.EXTENSION,
                id,
                extensionAuditBefore(row, StayRequestStatus.PENDING),
                extensionAuditAfter(contract.endDate(), row.newEndDate(), StayRequestStatus.APPROVED),
                row.reason());
        return toExtensionDetail(loadExtension(id, false));
    }

    @Override
    @Transactional
    public ContractDtos.ExtensionDetail rejectExtension(Long id, String reason) {
        guard.requirePermission(Permission.CONTRACT_EXTENSION_UPDATE);
        guard.requireRole(Role.STAFF);
        if (reason == null || reason.isBlank()) {
            throw BusinessException.invalid(
                    ApiField.REASON, ValidationMessage.REJECT_REASON_REQUIRED);
        }
        String normalized = reason.strip();
        loadExtension(id, true);
        ExtensionRow row = lockExtensionRequest(id);
        Actor actor = current.get();
        int updated =
                jdbc.update(
                        "UPDATE contract_extension_request SET status=?, decision_reason=?,"
                                + " decided_by=?, decided_at=CURRENT_TIMESTAMP"
                                + " WHERE id=? AND status=?",
                        StayRequestStatus.REJECTED,
                        normalized,
                        actor.id(),
                        id,
                        StayRequestStatus.PENDING);
        requireUpdated(updated);
        history.record(
                EntityType.EXTENSION,
                id,
                StayRequestStatus.PENDING,
                StayRequestStatus.REJECTED,
                actor.id(),
                normalized);
        audit.record(
                actor.id(),
                AuditAction.CONTRACT_EXTENSION_REJECT,
                EntityType.EXTENSION,
                id,
                extensionAuditBefore(row, StayRequestStatus.PENDING),
                extensionAuditAfter(
                        row.oldEndDate(), row.newEndDate(), StayRequestStatus.REJECTED),
                normalized);
        return toExtensionDetail(loadExtension(id, false));
    }

    @Override
    @Transactional
    public ContractDtos.ExtensionDetail cancelExtension(Long id) {
        guard.requirePermission(Permission.CONTRACT_EXTENSION_DELETE);
        guard.requireRole(Role.STUDENT);
        loadExtension(id, true);
        lockExtensionRequest(id);
        Actor actor = current.get();
        int updated =
                jdbc.update(
                        "UPDATE contract_extension_request SET status=? WHERE id=? AND status=?",
                        StayRequestStatus.CANCELLED,
                        id,
                        StayRequestStatus.PENDING);
        requireUpdated(updated);
        history.record(
                EntityType.EXTENSION,
                id,
                StayRequestStatus.PENDING,
                StayRequestStatus.CANCELLED,
                actor.id(),
                null);
        return toExtensionDetail(loadExtension(id, false));
    }

    /** Đọc yêu cầu và kiểm tra phạm vi; ngoài phạm vi trả 404 theo quy ước RBAC. */
    private ExtensionContext loadExtension(Long id, boolean writing) {
        guard.requireRole(Role.STAFF, Role.STUDENT);
        ExtensionRow row = loadExtensionRow(id);
        CurrentBed bed = occupancy.findOpenAssignmentByContract(row.contractId()).orElse(null);
        Long unitId = bed == null ? row.preferredUnitId() : bed.managementUnitId();
        Actor actor = current.get();
        if (actor.is(Role.STUDENT)) {
            if (!actor.id().equals(row.studentId())) {
                throw BusinessException.notFound();
            }
        } else if (writing) {
            guard.requireUnit(unitId);
        } else {
            guard.requireReadableUnit(unitId);
        }
        return new ExtensionContext(row, unitId);
    }

    private ExtensionRow loadExtensionRow(Long id) {
        return jdbc.query(SELECT_EXTENSION + " WHERE e.id=?", this::mapExtension, id).stream()
                .findFirst()
                .orElseThrow(BusinessException::notFound);
    }

    /** Khóa yêu cầu FOR UPDATE và yêu cầu còn PENDING tại thời điểm khóa. */
    private ExtensionRow lockExtensionRequest(Long id) {
        ExtensionRow row =
                jdbc.query(
                                SELECT_EXTENSION + " WHERE e.id=? FOR UPDATE OF e",
                                this::mapExtension,
                                id)
                        .stream()
                        .findFirst()
                        .orElseThrow(BusinessException::notFound);
        if (!StayRequestStatus.PENDING.equals(row.status())) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        return row;
    }

    /** Khóa hợp đồng sau yêu cầu; đọc lại ngày/trạng thái để chống quyết định trên dữ liệu cũ. */
    private ContractLock lockContract(Long id) {
        return jdbc.query(
                        SELECT_CONTRACT_FOR_UPDATE,
                        (rs, i) ->
                                new ContractLock(
                                        rs.getLong("id"),
                                        rs.getLong("student_id"),
                                        rs.getObject("start_date", LocalDate.class),
                                        rs.getObject("end_date", LocalDate.class),
                                        rs.getString("status")),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(BusinessException::notFound);
    }

    /** Mã hợp đồng PENDING/ACTIVE khác của cùng sinh viên chồng khoảng lưu trú, nếu có. */
    private String findOverlappingContract(
            Long studentId, Long contractId, LocalDate startDate, LocalDate newEndDate) {
        List<String> codes =
                jdbc.queryForList(
                        """
                        SELECT code FROM dormitory_contract
                        WHERE student_id=? AND id<>? AND status IN (?,?)
                          AND start_date<=? AND end_date>=?
                        ORDER BY id LIMIT 1
                        """,
                        String.class,
                        studentId,
                        contractId,
                        ContractStatus.PENDING,
                        ContractStatus.ACTIVE,
                        newEndDate,
                        startDate);
        return codes.isEmpty() ? null : codes.get(0);
    }

    private ExtensionRow mapExtension(ResultSet rs, int rowNum) throws SQLException {
        return new ExtensionRow(
                rs.getLong("id"),
                rs.getLong("contract_id"),
                rs.getString("contract_code"),
                rs.getLong("student_id"),
                rs.getString("student_code"),
                rs.getString("full_name"),
                rs.getString("source"),
                rs.getObject("old_end_date", LocalDate.class),
                rs.getObject("new_end_date", LocalDate.class),
                rs.getString("reason"),
                rs.getString("status"),
                rs.getObject("decided_by", Long.class),
                rs.getObject("decided_at", OffsetDateTime.class),
                rs.getString("decision_reason"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getLong("preferred_unit_id"),
                rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class),
                rs.getString("contract_status"));
    }

    private ContractDtos.ExtensionDetail toExtensionDetail(ExtensionContext context) {
        ExtensionRow row = context.row();
        List<String> actions =
                extensionActions(row, context.unitId()).stream()
                        .filter(action -> guard.hasPermission(EXTENSION_ACTION_PERMISSIONS.get(action)))
                        .toList();
        return new ContractDtos.ExtensionDetail(
                row.id(),
                row.contractId(),
                row.contractCode(),
                row.studentId(),
                row.studentCode(),
                row.studentName(),
                row.source(),
                row.oldEndDate(),
                row.newEndDate(),
                row.reason(),
                row.status(),
                row.decidedBy(),
                row.decidedAt(),
                row.decisionReason(),
                row.createdAt(),
                actions,
                history.list(EntityType.EXTENSION, row.id()));
    }

    /** PENDING có approve/reject cho Staff/Admin đúng đơn vị và cancel cho sinh viên chủ sở hữu. */
    private List<String> extensionActions(ExtensionRow row, Long unitId) {
        if (!StayRequestStatus.PENDING.equals(row.status())) {
            return List.of();
        }
        Actor actor = current.get();
        List<String> actions = new ArrayList<>();
        if (actor.is(Role.STUDENT)) {
            if (actor.id().equals(row.studentId())) {
                actions.add(BusinessAction.CANCEL);
            }
            return actions;
        }
        if ((actor.isAdmin() || actor.is(Role.STAFF))
                && (actor.isAdmin() || Objects.equals(actor.unitId(), unitId))) {
            actions.add(BusinessAction.APPROVE);
            actions.add(BusinessAction.REJECT);
        }
        return actions;
    }

    private static Map<String, Object> extensionAuditBefore(ExtensionRow row, String status) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put(ApiField.STATUS, status);
        before.put(ApiField.OLD_END_DATE, String.valueOf(row.oldEndDate()));
        before.put(ApiField.NEW_END_DATE, String.valueOf(row.newEndDate()));
        return before;
    }

    private static Map<String, Object> extensionAuditAfter(
            LocalDate oldEndDate, LocalDate newEndDate, String status) {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put(ApiField.STATUS, status);
        after.put(ApiField.OLD_END_DATE, String.valueOf(oldEndDate));
        after.put(ApiField.NEW_END_DATE, String.valueOf(newEndDate));
        return after;
    }

    /** Yêu cầu đã rời PENDING giữa lúc đọc và lúc cập nhật: rollback toàn bộ thao tác. */
    private static void requireUpdated(int updated) {
        if (updated == 0) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
    }

    /** Dòng yêu cầu gia hạn kèm đơn vị nguyện vọng để kiểm tra phạm vi. */
    private record ExtensionRow(
            Long id,
            Long contractId,
            String contractCode,
            Long studentId,
            String studentCode,
            String studentName,
            String source,
            LocalDate oldEndDate,
            LocalDate newEndDate,
            String reason,
            String status,
            Long decidedBy,
            OffsetDateTime decidedAt,
            String decisionReason,
            OffsetDateTime createdAt,
            Long preferredUnitId,
            LocalDate contractStartDate,
            LocalDate contractEndDate,
            String contractStatus) {}

    private record ExtensionContext(ExtensionRow row, Long unitId) {}

    private record ContractLock(
            Long id, Long studentId, LocalDate startDate, LocalDate endDate, String status) {}

    @Transactional
    @Override
    public Object saveOrUpdateStayRequest(boolean extension, ContractDtos.StaySave input) {
        guard.requirePermission(
                input.id() == null
                        ? (extension
                                ? Permission.CONTRACT_EXTENSION_CREATE
                                : Permission.ROOM_TRANSFER_CREATE)
                        : (extension
                                ? Permission.CONTRACT_EXTENSION_UPDATE
                                : Permission.ROOM_TRANSFER_UPDATE));
        requirePositiveId(input.id());
        var values = new LinkedHashMap<String, Object>();
        putIfPresent(values, "contractId", input.contractId());
        putIfPresent(values, "newEndDate", input.newEndDate());
        putIfPresent(values, "reason", input.reason());
        if (!extension && input.newEndDate() != null) {
            throw BusinessException.invalid(
                    ApiField.NEW_END_DATE, ValidationMessage.TRANSFER_NO_END_DATE);
        }
        return input.id() == null
                ? createStayRequest(extension, values)
                : updateStayRequest(extension, input.id(), values);
    }

    private Object createStayRequest(boolean extension, Map<String, Object> input) {
        guard.requirePermission(
                extension ? Permission.CONTRACT_EXTENSION_CREATE : Permission.ROOM_TRANSFER_CREATE);
        guard.requireRole(Role.STUDENT);
        var c = requireContract(number(input, "contractId"), false);
        if (!ContractStatus.ACTIVE.equals(c.get("status"))) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        if (extension) {
            validateExtension(input, c.get("endDate"));
        } else {
            requireStaying(((Number) c.get("id")).longValue());
        }
        Object created =
                insertRecord(
                        table(extension),
                        extension ? EXTENSION : TRANSFER,
                        extension
                                ? Set.of("contractId", "newEndDate", "reason")
                                : Set.of("contractId", "reason"),
                        input,
                        extension
                                ? Map.of(
                                        "old_end_date",
                                        c.get("endDate"),
                                        "status",
                                        StayRequestStatus.PENDING)
                                : Map.of("status", StayRequestStatus.PENDING));
        if (extension) {
            Long requestId = ((Number) ((Map<?, ?>) created).get("id")).longValue();
            history.record(
                    EntityType.EXTENSION,
                    requestId,
                    null,
                    StayRequestStatus.PENDING,
                    current.get().id(),
                    input.get("reason") == null ? null : input.get("reason").toString());
        }
        return created;
    }

    private Object updateStayRequest(boolean extension, Long id, Map<String, Object> input) {
        guard.requirePermission(
                extension ? Permission.CONTRACT_EXTENSION_UPDATE : Permission.ROOM_TRANSFER_UPDATE);
        var row = loadPendingRequest(extension, id);
        if (input.containsKey("contractId")) {
            throw BusinessException.invalid(ApiField.CONTRACT_ID, ValidationMessage.CONTRACT_IMMUTABLE);
        }
        if (extension && input.containsKey("newEndDate")) {
            validateExtension(input, row.get("oldEndDate"));
        }
        return updateRecord(table(extension), id, extension ? EXTENSION : TRANSFER, input);
    }

    @Transactional
    @Override
    public void deletePendingStayRequest(boolean extension, Long id) {
        guard.requirePermission(
                extension ? Permission.CONTRACT_EXTENSION_DELETE : Permission.ROOM_TRANSFER_DELETE);
        loadPendingRequest(extension, id);
        deleteRecord(table(extension), id);
    }

    private Map<String, Object> loadPendingRequest(boolean extension, Long id) {
        guard.requireRole(Role.STUDENT, Role.STAFF);
        var row = queries.one("SELECT * FROM " + table(extension) + " WHERE id=? FOR UPDATE", id);
        requireContract(((Number) row.get("contractId")).longValue(), true);
        requirePending(row, StayRequestStatus.PENDING);
        return row;
    }

    /** Chuyển phòng chỉ áp dụng khi sinh viên đang ở (STAY), không áp dụng khi mới giữ chỗ. */
    private void requireStaying(Long contractId) {
        boolean staying =
                occupancy
                        .findOpenAssignmentByContract(contractId)
                        .map(bed -> AssignmentKind.STAY.equals(bed.kind()))
                        .orElse(false);
        if (!staying) {
            throw BusinessException.conflict(ErrorCode.NOT_STAYING);
        }
    }

    private void validateExtension(Map<String, Object> input, Object old) {
        try {
            if (!LocalDate.parse(input.get("newEndDate").toString()).isAfter((LocalDate) old)) {
                throw BusinessException.invalid(
                        ApiField.NEW_END_DATE, ValidationMessage.EXTENSION_DATE_AFTER_CURRENT);
            }
        } catch (DateTimeParseException | NullPointerException e) {
            throw BusinessException.invalid(ApiField.NEW_END_DATE, ValidationMessage.DATE_INVALID);
        }
    }

    private static String table(boolean extension) {
        return extension ? "contract_extension_request" : "room_transfer_request";
    }

    private static void requirePending(Map<String, Object> row, String pendingStatus) {
        if (!pendingStatus.equals(row.get("status"))) {
            throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
        }
    }

    private static Long number(Map<String, Object> input, String field) {
        try {
            long value = Long.parseLong(input.get(field).toString());
            if (value <= 0) {
                throw new IllegalArgumentException();
            }
            return value;
        } catch (RuntimeException e) {
            throw BusinessException.invalid(field, ValidationMessage.RECORD_INVALID);
        }
    }

    private static void putIfPresent(Map<String, Object> values, String name, Object value) {
        if (value != null) {
            values.put(name, value);
        }
    }

    private static void requirePositiveId(Long id) {
        if (id != null && id <= 0) {
            throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE_INTEGER);
        }
    }
}

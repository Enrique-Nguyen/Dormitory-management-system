package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ConfigKey;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.StayRequestStatus;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.ConfigService;
import vn.edu.tlu.dms.common.service.impl.AbstractSqlCrudServiceImpl;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.contract.service.ContractCrudService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/** CRUD hồ sơ PENDING; kích hoạt, chuyển giường, thu/hoàn cọc sẽ được triển khai riêng về sau. */
@Service
@Transactional(readOnly = true)
public class ContractCrudServiceImpl extends AbstractSqlCrudServiceImpl
        implements ContractCrudService {

    private final ScopeGuard guard;

    private final ConfigService configs;

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
            ConfigService configs) {
        super(jdbc, queries, current, audit);
        this.guard = guard;
        this.configs = configs;
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
    public Object createPendingContract(Map<String, Object> input) {
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

    @Transactional
    @Override
    public Object updatePendingContract(Long id, Map<String, Object> input) {
        guard.requirePermission(Permission.CONTRACT_UPDATE);
        guard.requireRole(Role.STAFF);
        jdbc.queryForObject(
                "SELECT id FROM dormitory_contract WHERE id=? FOR UPDATE", Long.class, id);
        var row = requireContract(id, true);
        requirePending(row, ContractStatus.PENDING);
        requireNoAssignment(id);
        validateDates(input, row);
        if (input.containsKey("applicationId")) {
            throw BusinessException.invalid("applicationId", "Không đổi nguồn đơn của hợp đồng");
        }
        return updateRecord("dormitory_contract", id, CONTRACT, input);
    }

    @Transactional
    @Override
    public void deletePendingContract(Long id) {
        guard.requirePermission(Permission.CONTRACT_DELETE);
        guard.requireRole(Role.STAFF);
        jdbc.queryForObject(
                "SELECT id FROM dormitory_contract WHERE id=? FOR UPDATE", Long.class, id);
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
                throw BusinessException.invalid("endDate", "Ngày kết thúc phải sau ngày bắt đầu");
            }
        } catch (DateTimeParseException | NullPointerException e) {
            throw BusinessException.invalid("startDate", "Ngày dùng yyyy-MM-dd");
        }
    }

    @Override
    public Object searchStayRequests(boolean extension, int page, int size, String q) {
        guard.requirePermission(
                extension ? Permission.CONTRACT_EXTENSION_READ : Permission.ROOM_TRANSFER_READ);
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
        args.add("%" + q + "%");
        args.add("%" + q + "%");
        return queries.search(
                "SELECT req.*,c.code AS contract_code,u.full_name FROM "
                        + table(extension)
                        + " req JOIN dormitory_contract c ON c.id=req.contract_id JOIN"
                        + " room_application a ON a.id=c.application_id JOIN app_user u ON"
                        + " u.id=c.student_id WHERE "
                        + scope
                        + " AND (u.full_name ILIKE ? OR c.code ILIKE ?)",
                args,
                page,
                size);
    }

    @Override
    public Object getStayRequest(boolean extension, Long id) {
        guard.requirePermission(
                extension ? Permission.CONTRACT_EXTENSION_READ : Permission.ROOM_TRANSFER_READ);
        var row = queries.one("SELECT * FROM " + table(extension) + " WHERE id=?", id);
        requireContract(((Number) row.get("contractId")).longValue(), false);
        return row;
    }

    @Transactional
    @Override
    public Object createStayRequest(boolean extension, Map<String, Object> input) {
        guard.requirePermission(
                extension ? Permission.CONTRACT_EXTENSION_CREATE : Permission.ROOM_TRANSFER_CREATE);
        guard.requireRole(Role.STUDENT);
        var c = requireContract(number(input, "contractId"), false);
        if (!ContractStatus.ACTIVE.equals(c.get("status"))) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        if (extension) {
            validateExtension(input, c.get("endDate"));
        }
        return insertRecord(
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
    }

    @Transactional
    @Override
    public Object updateStayRequest(boolean extension, Long id, Map<String, Object> input) {
        guard.requirePermission(
                extension ? Permission.CONTRACT_EXTENSION_UPDATE : Permission.ROOM_TRANSFER_UPDATE);
        var row = loadPendingRequest(extension, id);
        if (input.containsKey("contractId")) {
            throw BusinessException.invalid("contractId", "Không đổi hợp đồng");
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

    private void validateExtension(Map<String, Object> input, Object old) {
        try {
            if (!LocalDate.parse(input.get("newEndDate").toString()).isAfter((LocalDate) old)) {
                throw BusinessException.invalid(
                        "newEndDate", "Ngày gia hạn phải sau ngày kết thúc hiện tại");
            }
        } catch (DateTimeParseException | NullPointerException e) {
            throw BusinessException.invalid("newEndDate", "Ngày không hợp lệ");
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
            return Long.valueOf(input.get(field).toString());
        } catch (RuntimeException e) {
            throw BusinessException.invalid(field, "Chọn bản ghi hợp lệ");
        }
    }
}

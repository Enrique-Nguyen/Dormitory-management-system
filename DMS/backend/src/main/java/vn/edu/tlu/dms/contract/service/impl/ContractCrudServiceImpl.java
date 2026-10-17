package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.ConfigKey;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.StayRequestStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.ConfigService;
import vn.edu.tlu.dms.common.service.impl.AbstractSqlCrudServiceImpl;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.contract.api.ContractDtos;
import vn.edu.tlu.dms.contract.service.ContractCrudService;
import vn.edu.tlu.dms.room.service.OccupancyQuery;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** CRUD hồ sơ PENDING; kích hoạt, chuyển giường, thu/hoàn cọc sẽ được triển khai riêng về sau. */
@Service
@Transactional(readOnly = true)
public class ContractCrudServiceImpl extends AbstractSqlCrudServiceImpl
        implements ContractCrudService {

    private final ScopeGuard guard;

    private final ConfigService configs;

    private final OccupancyQuery occupancy;

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
            OccupancyQuery occupancy) {
        super(jdbc, queries, current, audit);
        this.guard = guard;
        this.configs = configs;
        this.occupancy = occupancy;
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
            throw BusinessException.invalid("applicationId", "Không đổi nguồn đơn của hợp đồng");
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
                throw BusinessException.invalid("endDate", "Ngày kết thúc phải sau ngày bắt đầu");
            }
        } catch (DateTimeParseException | NullPointerException e) {
            throw BusinessException.invalid("startDate", "Ngày dùng yyyy-MM-dd");
        }
    }

    @Override
    public Object searchStayRequestsByPage(boolean extension, SearchRequest input, boolean mine) {
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
    public Object getStayRequest(boolean extension, Long id) {
        guard.requirePermission(
                extension ? Permission.CONTRACT_EXTENSION_READ : Permission.ROOM_TRANSFER_READ);
        var row = queries.one("SELECT * FROM " + table(extension) + " WHERE id=?", id);
        requireContract(((Number) row.get("contractId")).longValue(), false);
        return row;
    }

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
                    "newEndDate", "Yêu cầu chuyển phòng không nhận ngày gia hạn");
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

    private Object updateStayRequest(boolean extension, Long id, Map<String, Object> input) {
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
            long value = Long.parseLong(input.get(field).toString());
            if (value <= 0) {
                throw new IllegalArgumentException();
            }
            return value;
        } catch (RuntimeException e) {
            throw BusinessException.invalid(field, "Chọn bản ghi hợp lệ");
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

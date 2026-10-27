package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.service.DepositService;
import vn.edu.tlu.dms.billing.service.DepositService.BalanceView;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ConfigKey;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.DepositType;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.PaymentMethod;
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
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.contract.api.ContractDtos;
import vn.edu.tlu.dms.contract.domain.DormitoryContract;
import vn.edu.tlu.dms.contract.repository.DormitoryContractRepository;
import vn.edu.tlu.dms.contract.service.ContractService;
import vn.edu.tlu.dms.contract.service.StorageService;
import vn.edu.tlu.dms.contract.service.SummerLeaveLookup;
import vn.edu.tlu.dms.room.service.BedOccupancyService;
import vn.edu.tlu.dms.room.service.OccupancyQuery;
import vn.edu.tlu.dms.room.service.OccupancyQuery.AssignmentRow;
import vn.edu.tlu.dms.room.service.OccupancyQuery.CurrentBed;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Truy vấn và nghiệp vụ vòng đời hợp đồng. Hợp đồng được sinh bởi nghiệp vụ phân giường, không được
 * tạo, sửa trạng thái hoặc xóa bằng CRUD tổng quát vì sẽ làm lệch lịch sử phân phòng và sổ cọc.
 * Kích hoạt là một transaction duy nhất: khóa giường → sinh viên → hợp đồng, lưu mức cọc, thu cọc,
 * chuyển HOLD sang STAY và ghi lịch sử/audit trước khi trả kết quả.
 */
@Service
@Transactional(readOnly = true)
public class ContractServiceImpl implements ContractService {

    /** Khóa mức cọc bắt buộc trong details của DEPOSIT_AMOUNT_MISMATCH (API 4.6). */
    private static final String REQUIRED_DEPOSIT_KEY = "required";

    private static final String SELECT_CONTRACT =
            """
            SELECT c.id, c.code, c.student_id, c.application_id, c.start_date, c.end_date,
                   c.status, c.activation_deadline, c.deposit_required, a.preferred_unit_id
            FROM dormitory_contract c
            JOIN room_application a ON a.id = c.application_id
            """;

    private static final String SELECT_DETAIL =
            """
            SELECT c.student_id, u.full_name, sp.student_code, sp.gender, a.preferred_unit_id
            FROM dormitory_contract c
            JOIN room_application a ON a.id = c.application_id
            JOIN app_user u ON u.id = c.student_id
            LEFT JOIN student_profile sp ON sp.user_id = c.student_id
            WHERE c.id = ?
            """;

    /** Người dùng được đọc theo ID để hiển thị người kích hoạt trong chi tiết hợp đồng. */
    private static final String SELECT_ACTOR =
            "SELECT id, full_name, username FROM app_user WHERE id=?";

    private static final String SELECT_MOVE_OUT =
            "SELECT id, moved_out_at, asset_condition FROM move_out_record WHERE contract_id=?";

    private static final String PENDING_EXTENSION_SQL =
            "SELECT id FROM contract_extension_request WHERE contract_id=? AND status=?"
                    + " ORDER BY id DESC";

    private static final String PENDING_TRANSFER_SQL =
            "SELECT id FROM room_transfer_request WHERE contract_id=? AND status=?"
                    + " ORDER BY id DESC";

    private static final String PENDING_SUMMER_SQL =
            "SELECT id FROM summer_choice WHERE contract_id=? AND status=? ORDER BY id DESC";

    private final DormitoryContractRepository repository;

    private final CurrentUser currentUser;

    private final ScopeGuard scopeGuard;

    private final StatusHistoryService historyService;

    private final JdbcTemplate jdbc;

    private final OccupancyQuery occupancyQuery;

    private final SummerLeaveLookup summerLeaves;

    private final BedOccupancyService bedOccupancy;

    private final RoomReferenceService roomReferences;

    private final DepositService deposits;

    private final StorageService storage;

    private final ConfigService configs;

    private final AuditService audit;

    public ContractServiceImpl(
            DormitoryContractRepository repository,
            CurrentUser currentUser,
            ScopeGuard scopeGuard,
            StatusHistoryService historyService,
            JdbcTemplate jdbc,
            OccupancyQuery occupancyQuery,
            SummerLeaveLookup summerLeaves,
            BedOccupancyService bedOccupancy,
            RoomReferenceService roomReferences,
            DepositService deposits,
            StorageService storage,
            ConfigService configs,
            AuditService audit) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.scopeGuard = scopeGuard;
        this.historyService = historyService;
        this.jdbc = jdbc;
        this.occupancyQuery = occupancyQuery;
        this.summerLeaves = summerLeaves;
        this.bedOccupancy = bedOccupancy;
        this.roomReferences = roomReferences;
        this.deposits = deposits;
        this.storage = storage;
        this.configs = configs;
        this.audit = audit;
    }

    @Override
    public PageResponse<ContractDtos.Response> searchByPage(SearchRequest input, boolean mine) {
        scopeGuard.requirePermission(Permission.CONTRACT_READ);
        scopeGuard.requireRole(mine ? new String[] {Role.STUDENT} : new String[] {Role.STAFF});
        var actor = currentUser.get();
        var pageable =
                PageQuery.of(
                        input.page(),
                        input.size(),
                        input.sort(),
                        Set.of("id", "code", "status", "startDate", "endDate", "createdAt"),
                        "createdAt,desc");
        // Native SQL dùng tên cột được ánh xạ từ whitelist, tuyệt đối không nhận tên SQL từ client.
        var orders =
                pageable.getSort().stream()
                        .map(
                                order ->
                                        new Sort.Order(
                                                order.getDirection(),
                                                switch (order.getProperty()) {
                                                    case "startDate" -> "start_date";
                                                    case "endDate" -> "end_date";
                                                    case "createdAt" -> "created_at";
                                                    default -> order.getProperty();
                                                }))
                        .toList();
        List<String> statuses = input.statuses();
        return PageResponse.from(
                repository
                        .searchAccessibleContracts(
                                actor.role(),
                                actor.id(),
                                actor.unitId(),
                                actor.crossUnitAccess(),
                                mine,
                                statuses == null || statuses.isEmpty(),
                                statuses == null || statuses.isEmpty() ? List.of("") : statuses,
                                LikePattern.contains(input.q()),
                                PageRequest.of(input.page(), input.size(), Sort.by(orders)))
                        .map(contract -> toContractResponse(contract, false)));
    }

    @Override
    public ContractDtos.Response getContractById(Long id) {
        scopeGuard.requirePermission(Permission.CONTRACT_READ);
        scopeGuard.requireRole(Role.STUDENT, Role.STAFF);
        var actor = currentUser.get();
        DormitoryContract contract =
                repository
                        .findAccessibleContract(
                                id,
                                actor.role(),
                                actor.id(),
                                actor.unitId(),
                                actor.crossUnitAccess())
                        .orElseThrow(BusinessException::notFound);
        return toContractResponse(contract, true);
    }

    @Override
    public ContractDtos.Response getMyCurrentContract() {
        scopeGuard.requirePermission(Permission.CONTRACT_READ);
        scopeGuard.requireRole(Role.STUDENT);
        return repository
                .findAll(
                        (root, query, builder) ->
                                builder.and(
                                        builder.equal(
                                                root.get("studentId"), currentUser.get().id()),
                                        root.get("status")
                                                .in(ContractStatus.PENDING, ContractStatus.ACTIVE)))
                .stream()
                .findFirst()
                .map(contract -> toContractResponse(contract, true))
                .orElse(null);
    }

    @Override
    public boolean hasLiveContractForApplication(Long applicationId) {
        return repository.exists(
                (root, query, builder) ->
                        builder.and(
                                builder.equal(root.get("applicationId"), applicationId),
                                root.get("status")
                                        .in(ContractStatus.PENDING, ContractStatus.ACTIVE)));
    }

    @Override
    @Transactional
    public ContractDtos.ActivationResult activate(Long id, ContractDtos.ActivateRequest input) {
        scopeGuard.requirePermission(Permission.CONTRACT_UPDATE);
        scopeGuard.requireRole(Role.STAFF);
        validateActivation(input);

        ContractRow preview = loadContract(id);
        var holdPreview = occupancyQuery.findOpenAssignmentByContract(id).orElse(null);
        scopeGuard.requireUnit(
                holdPreview == null ? preview.preferredUnitId() : holdPreview.managementUnitId());

        // Thứ tự khóa toàn cục: giường → sinh viên → hợp đồng; đọc lại sau khi đã giữ khóa.
        bedOccupancy.lockOpenAssignment(id, preview.studentId());
        ContractRow contract = lockContract(id);
        if (ContractStatus.CANCELLED.equals(contract.status())) {
            throw BusinessException.conflict(ErrorCode.CONTRACT_AUTO_CANCELLED);
        }
        if (!ContractStatus.PENDING.equals(contract.status())) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (contract.activationDeadline() != null
                && contract.activationDeadline().isBefore(now)) {
            throw BusinessException.conflict(ErrorCode.CONTRACT_ACTIVATION_EXPIRED);
        }
        CurrentBed hold =
                occupancyQuery
                        .findOpenAssignmentByContract(id)
                        .filter(bed -> AssignmentKind.HOLD.equals(bed.kind()))
                        .orElseThrow(() -> BusinessException.conflict(ErrorCode.NO_HOLD_ASSIGNMENT));

        Long required = roomReferences.getBuildingDepositAmount(hold.buildingId());
        if (required == null || !required.equals(input.depositAmount())) {
            throw new BusinessException(
                    ErrorCode.DEPOSIT_AMOUNT_MISMATCH,
                    HttpStatus.BAD_REQUEST,
                    Map.of(REQUIRED_DEPOSIT_KEY, required == null ? 0L : required));
        }

        Actor actor = currentUser.get();
        String note = normalizeNote(input.note());
        // deposit_required phải có trước khi DepositService kiểm tra và ghi giao dịch thu cọc.
        int updated =
                jdbc.update(
                        "UPDATE dormitory_contract SET deposit_required=?,status=?,activated_at=?,"
                                + "activated_by=?,status_reason=NULL WHERE id=? AND status=?",
                        required,
                        ContractStatus.ACTIVE,
                        now,
                        actor.id(),
                        id,
                        ContractStatus.PENDING);
        if (updated == 0) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }

        // Lỗi ở bước thu cọc hoặc chuyển HOLD sang STAY làm rollback toàn bộ kích hoạt.
        deposits.collectInitial(id, input.depositAmount(), note);
        bedOccupancy.convertHoldToStay(id, actor.id());

        historyService.record(
                EntityType.CONTRACT,
                id,
                ContractStatus.PENDING,
                ContractStatus.ACTIVE,
                actor.id(),
                note);
        audit.record(
                actor.id(),
                AuditAction.CONTRACT_ACTIVATE,
                EntityType.CONTRACT,
                id,
                activationAuditBefore(),
                activationAuditAfter(required, input.depositAmount(), input.paymentMethod(), now),
                note);

        DormitoryContract activated =
                repository.findById(id).orElseThrow(BusinessException::notFound);
        ContractDtos.Response response = toContractResponse(activated, true);
        ContractDtos.DepositSummary deposit =
                new ContractDtos.DepositSummary(
                        DepositType.DEPOSIT,
                        input.depositAmount(),
                        input.depositAmount(),
                        input.paymentMethod(),
                        now,
                        note);
        return new ContractDtos.ActivationResult(
                response,
                deposit,
                occupancyQuery.findOpenAssignmentByContract(id).orElse(null),
                response.allowedActions());
    }

    @Override
    @Transactional
    public Long createPendingForApplication(
            Long applicationId, Long studentId, LocalDate startDate, LocalDate endDate) {
        if (applicationId == null || studentId == null || startDate == null || endDate == null) {
            throw BusinessException.invalid(ApiField.APPLICATION_ID, ValidationMessage.REQUIRED);
        }
        if (!endDate.isAfter(startDate)) {
            throw BusinessException.invalid(
                    ApiField.END_DATE, ValidationMessage.END_DATE_AFTER_START);
        }
        Map<String, Object> application =
                jdbc.query(
                                "SELECT student_id,status FROM room_application"
                                        + " WHERE id=? FOR UPDATE",
                                (rs, i) ->
                                        Map.<String, Object>of(
                                                "student_id", rs.getLong("student_id"),
                                                "status", rs.getString("status")),
                                applicationId)
                        .stream()
                        .findFirst()
                        .orElseThrow(BusinessException::notFound);
        if (!Objects.equals(studentId, number(application.get("student_id")))) {
            throw BusinessException.invalid(
                    ApiField.STUDENT_ID, ValidationMessage.STUDENT_IMMUTABLE);
        }
        if (!ApplicationStatus.APPROVED.equals(application.get("status"))) {
            throw BusinessException.conflict(ErrorCode.APPLICATION_NOT_APPROVED);
        }
        if (findLiveContractIdForApplication(applicationId) != null
                || Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                "SELECT EXISTS(SELECT 1 FROM dormitory_contract WHERE student_id=?"
                                        + " AND status IN (?,?))",
                                Boolean.class,
                                studentId,
                                ContractStatus.PENDING,
                                ContractStatus.ACTIVE))) {
            throw BusinessException.conflict(ErrorCode.CONTRACT_EXISTS);
        }

        String code =
                jdbc.queryForObject(
                        "SELECT fn_next_code('HD','seq_contract_code',true)", String.class);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime deadline =
                now.plusDays(configs.positiveInt(ConfigKey.CONTRACT_ACTIVATION_DAYS));
        Long id =
                jdbc.queryForObject(
                        """
                        INSERT INTO dormitory_contract(code, student_id, application_id, start_date,
                            end_date, status, activation_deadline, created_at)
                        VALUES(?,?,?,?,?,?,?,?)
                        RETURNING id
                        """,
                        Long.class,
                        code,
                        studentId,
                        applicationId,
                        startDate,
                        endDate,
                        ContractStatus.PENDING,
                        deadline,
                        now);
        Actor actor = currentUser.get();
        historyService.record(
                EntityType.CONTRACT, id, null, ContractStatus.PENDING, actor.id(), null);
        Map<String, Object> value = new LinkedHashMap<>();
        value.put(ApiField.CONTRACT_ID, id);
        value.put(ApiField.STATUS, ContractStatus.PENDING);
        value.put(ApiField.START_DATE, startDate.toString());
        value.put(ApiField.END_DATE, endDate.toString());
        audit.record(
                actor.id(), AuditAction.CRUD_CREATE, EntityType.CONTRACT, id, null, value, null);
        return id;
    }

    @Override
    public Long findLiveContractIdForApplication(Long applicationId) {
        if (applicationId == null) {
            return null;
        }
        List<Long> ids =
                jdbc.queryForList(
                        "SELECT id FROM dormitory_contract WHERE application_id=?"
                                + " AND status IN (?,?) ORDER BY id DESC",
                        Long.class,
                        applicationId,
                        ContractStatus.PENDING,
                        ContractStatus.ACTIVE);
        return ids.isEmpty() ? null : ids.get(0);
    }

    @Override
    public ActivationInfo findActivationInfo(Long contractId) {
        if (contractId == null) {
            return null;
        }
        List<ActivationInfo> rows =
                jdbc.query(
                        "SELECT code, activation_deadline FROM dormitory_contract WHERE id=?",
                        (rs, rowNum) ->
                                new ActivationInfo(
                                        rs.getString("code"),
                                        rs.getObject("activation_deadline", OffsetDateTime.class)),
                        contractId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    @Transactional
    public void cancelPendingContract(Long contractId, String reason) {
        ContractRow contract = lockContract(contractId);
        if (!ContractStatus.PENDING.equals(contract.status())) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        String normalized = normalizeNote(reason);
        jdbc.update(
                "UPDATE dormitory_contract SET status=?,closed_at=CURRENT_TIMESTAMP,status_reason=?"
                        + " WHERE id=? AND status=?",
                ContractStatus.CANCELLED,
                normalized,
                contractId,
                ContractStatus.PENDING);
        Actor actor = currentUser.get();
        historyService.record(
                EntityType.CONTRACT,
                contractId,
                ContractStatus.PENDING,
                ContractStatus.CANCELLED,
                actor.id(),
                normalized);
        Map<String, Object> before = new LinkedHashMap<>();
        before.put(ApiField.STATUS, ContractStatus.PENDING);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put(ApiField.STATUS, ContractStatus.CANCELLED);
        after.put(ApiField.REASON, normalized);
        audit.record(
                actor.id(),
                AuditAction.CRUD_UPDATE,
                EntityType.CONTRACT,
                contractId,
                before,
                after,
                normalized);
    }

    @Override
    @Transactional
    public void extendEndDate(Long contractId, LocalDate newEndDate, String reason) {
        EndDateRow row = lockEndDate(contractId);
        if (!ContractStatus.ACTIVE.equals(row.status())) {
            throw BusinessException.conflict(ErrorCode.CONTRACT_NOT_ACTIVE);
        }
        requireExtendedEndDate(row.endDate(), newEndDate);
        jdbc.update(
                "UPDATE dormitory_contract SET end_date=? WHERE id=?", newEndDate, contractId);
        Actor actor = currentUser.get();
        Map<String, Object> before = new LinkedHashMap<>();
        before.put(ApiField.END_DATE, row.endDate().toString());
        Map<String, Object> after = new LinkedHashMap<>();
        after.put(ApiField.END_DATE, newEndDate.toString());
        audit.record(
                actor.id(),
                AuditAction.CRUD_UPDATE,
                EntityType.CONTRACT,
                contractId,
                before,
                after,
                normalizeNote(reason));
    }

    @Override
    @Transactional
    public void recordSummerExtension(
            Long contractId, LocalDate newEndDate, Long actorId, String reason) {
        EndDateRow row = lockEndDate(contractId);
        if (!ContractStatus.ACTIVE.equals(row.status())) {
            throw BusinessException.conflict(ErrorCode.CONTRACT_NOT_ACTIVE);
        }
        LocalDate oldEndDate = row.endDate();
        requireExtendedEndDate(oldEndDate, newEndDate);
        OffsetDateTime now = OffsetDateTime.now();
        Long requestId =
                jdbc.queryForObject(
                        """
                        INSERT INTO contract_extension_request(contract_id, source, old_end_date,
                            new_end_date, reason, status, decided_by, decided_at, created_at)
                        VALUES(?,?,?,?,?,?,?,?,?)
                        RETURNING id
                        """,
                        Long.class,
                        contractId,
                        ContractDtos.ExtensionSource.SUMMER_STAY,
                        oldEndDate,
                        newEndDate,
                        normalizeNote(reason),
                        StayRequestStatus.APPROVED,
                        actorId,
                        now,
                        now);
        extendEndDate(contractId, newEndDate, reason);
        historyService.record(
                ContractDtos.ExtensionSource.ENTITY_TYPE,
                requestId,
                null,
                StayRequestStatus.APPROVED,
                actorId,
                normalizeNote(reason));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put(ApiField.CONTRACT_ID, contractId);
        after.put(ApiField.STATUS, StayRequestStatus.APPROVED);
        after.put(ApiField.OLD_END_DATE, oldEndDate.toString());
        after.put(ApiField.NEW_END_DATE, newEndDate.toString());
        audit.record(
                actorId,
                AuditAction.CONTRACT_EXTENSION_APPROVE,
                ContractDtos.ExtensionSource.ENTITY_TYPE,
                requestId,
                null,
                after,
                normalizeNote(reason));
    }

    /** Tổng hợp theo đơn vị của hợp đồng; ngưỡng ngày do ConfigService cung cấp. */
    @Override
    public ExpirySummary getExpirySummary(Long managementUnitId, int expiringDays) {
        return jdbc.queryForObject(
                """
                SELECT COUNT(*) FILTER (WHERE c.end_date BETWEEN CURRENT_DATE AND CURRENT_DATE+?) AS expiring,
                       COUNT(*) FILTER (WHERE c.end_date<CURRENT_DATE) AS overdue
                FROM dormitory_contract c JOIN room_application ap ON ap.id=c.application_id
                WHERE c.status=? AND (CAST(? AS bigint) IS NULL OR ap.preferred_unit_id=?)
                """,
                (rs, i) -> new ExpirySummary(rs.getLong("expiring"), rs.getLong("overdue")),
                expiringDays,
                ContractStatus.ACTIVE,
                managementUnitId,
                managementUnitId);
    }

    /** Dựng response; bản danh sách bỏ các truy vấn chi tiết để không phát sinh N+1. */
    private ContractDtos.Response toContractResponse(DormitoryContract contract, boolean detail) {
        if (!detail) {
            return new ContractDtos.Response(
                    contract.getId(),
                    contract.getCode(),
                    contract.getStudentId(),
                    contract.getApplicationId(),
                    contract.getStartDate(),
                    contract.getEndDate(),
                    contract.getStatus(),
                    contract.getActivationDeadline(),
                    contract.getDepositRequired(),
                    contract.getActivatedAt(),
                    contract.getStatusReason(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        DetailRow row = loadDetail(contract.getId());
        CurrentBed bed = occupancyQuery.findOpenAssignmentByContract(contract.getId()).orElse(null);
        Long unitId = bed == null ? row.preferredUnitId() : bed.managementUnitId();
        Long pendingExtension =
                pendingId(PENDING_EXTENSION_SQL, contract.getId(), StayRequestStatus.PENDING);
        Long pendingTransfer =
                pendingId(PENDING_TRANSFER_SQL, contract.getId(), StayRequestStatus.PENDING);
        Long pendingSummer =
                pendingId(PENDING_SUMMER_SQL, contract.getId(), StayRequestStatus.PENDING);
        BalanceView depositView = depositView(contract.getId());
        List<String> actions =
                contractActions(
                        contract,
                        row,
                        bed,
                        unitId,
                        pendingExtension,
                        pendingTransfer,
                        depositView);
        Long balance = depositView == null ? null : depositView.balance();
        Long daysRemaining =
                ContractStatus.ACTIVE.equals(contract.getStatus())
                        ? ChronoUnit.DAYS.between(today(), contract.getEndDate())
                        : null;
        return new ContractDtos.Response(
                contract.getId(),
                contract.getCode(),
                contract.getStudentId(),
                contract.getApplicationId(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getStatus(),
                contract.getActivationDeadline(),
                contract.getDepositRequired(),
                contract.getActivatedAt(),
                contract.getStatusReason(),
                actions,
                historyService.list(EntityType.CONTRACT, contract.getId()),
                row.studentCode(),
                row.studentName(),
                labels(contract),
                daysRemaining,
                balance,
                bed,
                pendingExtension,
                pendingTransfer,
                pendingSummer,
                new ContractDtos.StudentInfo(
                        row.studentId(), row.studentCode(), row.studentName(), row.gender()),
                depositInfo(contract, depositView),
                actorInfo(contract.getActivatedBy()),
                assignmentHistory(contract.getId()),
                new ContractDtos.PendingRequestsInfo(
                        pendingExtension, pendingTransfer, pendingSummer),
                storage.findOpenRecordIdByContract(contract.getId()),
                moveOutInfo(contract.getId()));
    }

    /**
     * Hành động được phép theo trạng thái, vai trò, phạm vi đơn vị và quyền đang cấp. PENDING có
     * activate cho Staff/Admin đúng đơn vị; ACTIVE có yêu cầu gia hạn/chuyển phòng cho sinh viên chủ
     * sở hữu khi đang ở và không có yêu cầu chờ cùng loại, các action trả phòng/gửi đồ của Staff
     * cùng đơn vị và hành động bù cọc khi sổ cọc còn thiếu (API mục 6).
     */
    private List<String> contractActions(
            DormitoryContract contract,
            DetailRow row,
            CurrentBed bed,
            Long unitId,
            Long pendingExtension,
            Long pendingTransfer,
            BalanceView depositView) {
        Actor actor = currentUser.get();
        boolean staffSameUnit =
                actor.isAdmin()
                        || (actor.is(Role.STAFF) && Objects.equals(actor.unitId(), unitId));
        List<String> actions = new ArrayList<>();
        if (ContractStatus.PENDING.equals(contract.getStatus())) {
            if (staffSameUnit && scopeGuard.hasPermission(Permission.CONTRACT_UPDATE)) {
                actions.add(BusinessAction.ACTIVATE);
            }
            return actions;
        }
        if (!ContractStatus.ACTIVE.equals(contract.getStatus())) {
            return actions;
        }
        boolean staying = bed != null && AssignmentKind.STAY.equals(bed.kind());
        boolean owner = actor.is(Role.STUDENT) && actor.id().equals(row.studentId());
        if (owner
                && staying
                && pendingExtension == null
                && scopeGuard.hasPermission(Permission.CONTRACT_EXTENSION_CREATE)) {
            actions.add(BusinessAction.CREATE_EXTENSION_REQUEST);
        }
        if (owner
                && staying
                && pendingTransfer == null
                && scopeGuard.hasPermission(Permission.ROOM_TRANSFER_CREATE)) {
            actions.add(BusinessAction.CREATE_TRANSFER_REQUEST);
        }
        if (staffSameUnit && staying) {
            if (scopeGuard.hasPermission(Permission.MOVE_OUT_CREATE)) {
                actions.add(BusinessAction.MOVE_OUT);
            }
            if (scopeGuard.hasPermission(Permission.STORAGE_CREATE)
                    && hasApprovedSummerExtension(contract)) {
                actions.add(BusinessAction.CREATE_STORAGE_RECORD);
            }
            if (topupNeeded(depositView) > 0
                    && scopeGuard.hasPermission(Permission.DEPOSIT_CREATE)) {
                actions.add(BusinessAction.DEPOSIT_TOPUP);
            }
        }
        return actions;
    }

    /** Nhãn tính từ trạng thái, ngày kết thúc và lựa chọn hè đã duyệt; không lưu vào CSDL. */
    private List<String> labels(DormitoryContract contract) {
        if (!ContractStatus.ACTIVE.equals(contract.getStatus())) {
            return List.of();
        }
        List<String> labels = new ArrayList<>();
        LocalDate today = today();
        if (contract.getEndDate().isBefore(today)) {
            labels.add(ContractDtos.Label.OVERDUE_MOVE_OUT);
        } else if (!contract.getEndDate()
                .isAfter(today.plusDays(configs.positiveInt(ConfigKey.CONTRACT_EXPIRING_DAYS)))) {
            labels.add(ContractDtos.Label.EXPIRING);
        }
        if (summerLeaves.findApprovedTempLeave(contract.getId()).isPresent()) {
            labels.add(ContractDtos.Label.SUMMER_LEAVE);
        }
        return labels;
    }

    /** Điều kiện hiển thị tạo phiếu gửi đồ: TEMP_LEAVE đã duyệt và hợp đồng phủ qua kỳ hè. */
    private boolean hasApprovedSummerExtension(DormitoryContract contract) {
        var leave = summerLeaves.findApprovedTempLeave(contract.getId()).orElse(null);
        if (leave == null) {
            return false;
        }
        if (contract.getEndDate().isAfter(leave.semesterEnd())) {
            return true;
        }
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        """
                        SELECT EXISTS (
                            SELECT 1 FROM contract_extension_request
                            WHERE contract_id=? AND status=? AND new_end_date>?)
                        """,
                        Boolean.class,
                        contract.getId(),
                        StayRequestStatus.APPROVED,
                        leave.semesterEnd()));
    }

    private ContractRow loadContract(Long contractId) {
        return jdbc.query(
                        SELECT_CONTRACT + " WHERE c.id=?", this::mapContractRow, contractId)
                .stream()
                .findFirst()
                .orElseThrow(BusinessException::notFound);
    }

    /** Khóa hợp đồng FOR UPDATE; nơi gọi phải khóa giường/sinh viên trước theo thứ tự SAD. */
    private ContractRow lockContract(Long contractId) {
        return jdbc.query(
                        SELECT_CONTRACT + " WHERE c.id=? FOR UPDATE OF c",
                        this::mapContractRow,
                        contractId)
                .stream()
                .findFirst()
                .orElseThrow(BusinessException::notFound);
    }

    private ContractRow mapContractRow(ResultSet rs, int rowNum) throws SQLException {
        return new ContractRow(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getLong("student_id"),
                rs.getLong("application_id"),
                rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class),
                rs.getString("status"),
                rs.getObject("activation_deadline", OffsetDateTime.class),
                rs.getObject("deposit_required", Long.class),
                rs.getLong("preferred_unit_id"));
    }

    /** Khóa hợp đồng và đọc ngày kết thúc/trạng thái mới nhất cho nghiệp vụ gia hạn. */
    private EndDateRow lockEndDate(Long contractId) {
        return jdbc.query(
                        "SELECT end_date,status FROM dormitory_contract WHERE id=? FOR UPDATE",
                        (rs, i) ->
                                new EndDateRow(
                                        rs.getObject("end_date", LocalDate.class),
                                        rs.getString("status")),
                        contractId)
                .stream()
                .findFirst()
                .orElseThrow(BusinessException::notFound);
    }

    private DetailRow loadDetail(Long contractId) {
        return jdbc.query(
                        SELECT_DETAIL,
                        (rs, i) ->
                                new DetailRow(
                                        rs.getString("full_name"),
                                        rs.getString("student_code"),
                                        rs.getLong("preferred_unit_id"),
                                        rs.getLong("student_id"),
                                        rs.getString("gender")),
                        contractId)
                .stream()
                .findFirst()
                .orElseThrow(BusinessException::notFound);
    }

    private Long pendingId(String sql, Long contractId, String status) {
        List<Long> ids = jdbc.queryForList(sql, Long.class, contractId, status);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** Sổ cọc chỉ đọc khi người xem còn quyền DEPOSIT_READ; thiếu quyền trả null. */
    private BalanceView depositView(Long contractId) {
        if (!scopeGuard.hasPermission(Permission.DEPOSIT_READ)) {
            return null;
        }
        return deposits.getBalanceView(contractId);
    }

    /**
     * Khối `deposit` theo API 4.5. Mức cọc lấy từ sổ cọc; khi hợp đồng chưa kích hoạt (chưa có dòng
     * trong v_deposit_balance) dùng mức cọc đã chụp trên hợp đồng để tránh trả số 0 gây nhầm lẫn.
     */
    private static ContractDtos.DepositInfo depositInfo(
            DormitoryContract contract, BalanceView view) {
        if (view == null) {
            return null;
        }
        Long required = view.depositRequired();
        if (required == null || required <= 0) {
            required = contract.getDepositRequired();
        }
        Long topup = view.topupNeeded();
        return new ContractDtos.DepositInfo(
                required, view.balance(), topup == null ? 0L : topup, view.topupOverdue());
    }

    /** Khoản cần bù cọc theo v_deposit_balance; chỉ khác 0 khi hợp đồng ACTIVE. */
    private static long topupNeeded(BalanceView view) {
        return view == null || view.topupNeeded() == null ? 0L : view.topupNeeded();
    }

    /** Người kích hoạt đọc từ app_user theo ID; null khi hợp đồng chưa từng kích hoạt. */
    private ContractDtos.ActorInfo actorInfo(Long actorId) {
        if (actorId == null) {
            return null;
        }
        return jdbc.query(
                        SELECT_ACTOR,
                        (rs, i) ->
                                new ContractDtos.ActorInfo(
                                        rs.getLong("id"),
                                        rs.getString("full_name"),
                                        rs.getString("username")),
                        actorId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    /** Toàn bộ lịch sử phân phòng của hợp đồng, mới nhất trước, do module room cung cấp. */
    private List<ContractDtos.AssignmentInfo> assignmentHistory(Long contractId) {
        return occupancyQuery.findAssignmentsByContract(contractId).stream()
                .map(ContractServiceImpl::toAssignmentInfo)
                .toList();
    }

    private static ContractDtos.AssignmentInfo toAssignmentInfo(AssignmentRow row) {
        return new ContractDtos.AssignmentInfo(
                row.id(),
                row.kind(),
                new ContractDtos.BedInfo(
                        row.bedId(),
                        bedDisplayCode(row),
                        row.roomId(),
                        row.roomCode(),
                        row.buildingId(),
                        row.buildingCode(),
                        row.floor(),
                        row.managementUnitId()),
                row.startedAt(),
                row.stayStartedAt(),
                row.endedAt(),
                row.endReason(),
                row.priceMain(),
                row.priceSummer());
    }

    /** Mã giường hiển thị dạng TÒA-PHÒNG-GIƯỜNG (API 4.5); thiếu thông tin thì trả mã ngắn. */
    private static String bedDisplayCode(AssignmentRow row) {
        if (row.buildingCode() == null || row.roomCode() == null) {
            return row.bedCode();
        }
        return row.buildingCode() + "-" + row.roomCode() + "-" + row.bedCode();
    }

    /**
     * Biên bản trả phòng của hợp đồng khi có (API 4.5). Bảng move_out_record không có mốc tạo riêng
     * nên {@code createdAt} dùng lại thời điểm ghi nhận trả phòng trong cùng transaction.
     */
    private ContractDtos.MoveOutInfo moveOutInfo(Long contractId) {
        return jdbc.query(SELECT_MOVE_OUT, ContractServiceImpl::mapMoveOut, contractId).stream()
                .findFirst()
                .orElse(null);
    }

    private static ContractDtos.MoveOutInfo mapMoveOut(ResultSet rs, int rowNum)
            throws SQLException {
        OffsetDateTime movedOutAt = rs.getObject("moved_out_at", OffsetDateTime.class);
        return new ContractDtos.MoveOutInfo(
                rs.getLong("id"), movedOutAt, rs.getString("asset_condition"), movedOutAt);
    }

    private static void validateActivation(ContractDtos.ActivateRequest input) {
        if (input == null || !Boolean.TRUE.equals(input.paperContractSigned())) {
            throw BusinessException.invalid(
                    ApiField.PAPER_CONTRACT_SIGNED, ValidationMessage.PAPER_CONTRACT_REQUIRED);
        }
        if (input.depositAmount() == null || input.depositAmount() <= 0) {
            throw BusinessException.invalid(
                    ApiField.DEPOSIT_AMOUNT, ValidationMessage.DEPOSIT_AMOUNT_REQUIRED);
        }
        if (!PaymentMethod.CASH.equals(input.paymentMethod())
                && !PaymentMethod.TRANSFER.equals(input.paymentMethod())) {
            throw BusinessException.invalid(
                    ApiField.PAYMENT_METHOD, ValidationMessage.PAYMENT_METHOD_INVALID);
        }
    }

    private static void requireExtendedEndDate(LocalDate oldEndDate, LocalDate newEndDate) {
        if (newEndDate == null || !newEndDate.isAfter(oldEndDate)) {
            throw BusinessException.invalid(
                    ApiField.NEW_END_DATE, ValidationMessage.EXTENSION_DATE_AFTER_CURRENT);
        }
    }

    private static Map<String, Object> activationAuditBefore() {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put(ApiField.STATUS, ContractStatus.PENDING);
        return before;
    }

    private static Map<String, Object> activationAuditAfter(
            Long required, Long amount, String paymentMethod, OffsetDateTime at) {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put(ApiField.STATUS, ContractStatus.ACTIVE);
        after.put(ApiField.DEPOSIT_AMOUNT, amount);
        after.put(REQUIRED_DEPOSIT_KEY, required);
        after.put(ApiField.PAYMENT_METHOD, paymentMethod);
        after.put(ApiField.ACTIVATED_AT, at.toString());
        return after;
    }

    private static Long number(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.strip();
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of(DmsConstants.APPLICATION_TIME_ZONE));
    }

    /** Hợp đồng kèm đơn vị nguyện vọng để kiểm tra phạm vi trước khi khóa. */
    private record ContractRow(
            Long id,
            String code,
            Long studentId,
            Long applicationId,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            OffsetDateTime activationDeadline,
            Long depositRequired,
            Long preferredUnitId) {}

    /** Thông tin sinh viên và đơn vị nguyện vọng dùng dựng chi tiết. */
    private record DetailRow(
            String studentName,
            String studentCode,
            Long preferredUnitId,
            Long studentId,
            String gender) {}

    /** Ngày kết thúc và trạng thái hợp đồng đọc sau khi khóa cho nghiệp vụ gia hạn. */
    private record EndDateRow(LocalDate endDate, String status) {}
}

package vn.edu.tlu.dms.registration.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentEndReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ConfigKey;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.RegistrationKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.ConfigService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.contract.service.ContractService;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.BedRef;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Hold;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Input;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Priority;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Response;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.StudentRef;
import vn.edu.tlu.dms.registration.domain.ApplicationPriority;
import vn.edu.tlu.dms.registration.domain.ApplicationPriorityId;
import vn.edu.tlu.dms.registration.domain.RegistrationPeriodUnitId;
import vn.edu.tlu.dms.registration.domain.RoomApplication;
import vn.edu.tlu.dms.registration.domain.RoomApplicationPolicy;
import vn.edu.tlu.dms.registration.repository.ApplicationPriorityRepository;
import vn.edu.tlu.dms.registration.repository.PriorityCategoryRepository;
import vn.edu.tlu.dms.registration.repository.RegistrationPeriodRepository;
import vn.edu.tlu.dms.registration.repository.RegistrationPeriodUnitRepository;
import vn.edu.tlu.dms.registration.repository.RoomApplicationRepository;
import vn.edu.tlu.dms.registration.service.RoomApplicationService;
import vn.edu.tlu.dms.room.service.BedOccupancyService;
import vn.edu.tlu.dms.room.service.OccupancyQuery;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cài đặt nghiệp vụ đơn đăng ký phòng. Phân giường, hủy giữ chỗ và xác nhận diện ưu tiên đều khóa
 * đơn trước, gọi module room/contract qua hợp đồng công khai và ghi lịch sử/nhật ký trong cùng
 * transaction để không lệch trạng thái giữa đơn, hợp đồng và phân phòng.
 */
@Service
@Transactional(readOnly = true)
public class RoomApplicationServiceImpl implements RoomApplicationService {

    /** Lý do kỹ thuật khi hủy đơn ASSIGNED không kèm lý do từ người dùng; chỉ dùng nội bộ. */
    private static final String CANCEL_REASON = "Hủy đơn đăng ký";

    /** Lý do từ chối/hủy giữ chỗ phải đủ dài để sinh viên đọc được nguyên nhân. */
    private static final int MIN_REASON_LENGTH = 5;

    private static final LiveContract NO_CONTRACT = new LiveContract(null, false, false, null, null);

    private final RoomApplicationRepository applications;

    private final RegistrationPeriodRepository periods;

    private final RegistrationPeriodUnitRepository periodUnits;

    private final ApplicationPriorityRepository priorities;

    private final PriorityCategoryRepository categories;

    private final RoomReferenceService rooms;

    private final AccountReferenceService accounts;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final StatusHistoryService history;

    private final JdbcTemplate jdbc;

    private final ContractService contracts;

    private final BedOccupancyService occupancy;

    private final OccupancyQuery occupancyQuery;

    private final ConfigService configs;

    private final AuditService audit;

    private final EntityManager entityManager;

    public RoomApplicationServiceImpl(
            RoomApplicationRepository applications,
            RegistrationPeriodRepository periods,
            RegistrationPeriodUnitRepository periodUnits,
            ApplicationPriorityRepository priorities,
            PriorityCategoryRepository categories,
            RoomReferenceService rooms,
            AccountReferenceService accounts,
            CurrentUser current,
            ScopeGuard guard,
            StatusHistoryService history,
            JdbcTemplate jdbc,
            ContractService contracts,
            BedOccupancyService occupancy,
            OccupancyQuery occupancyQuery,
            ConfigService configs,
            AuditService audit,
            EntityManager entityManager) {
        this.applications = applications;
        this.periods = periods;
        this.periodUnits = periodUnits;
        this.priorities = priorities;
        this.categories = categories;
        this.rooms = rooms;
        this.accounts = accounts;
        this.current = current;
        this.guard = guard;
        this.history = history;
        this.jdbc = jdbc;
        this.contracts = contracts;
        this.occupancy = occupancy;
        this.occupancyQuery = occupancyQuery;
        this.configs = configs;
        this.audit = audit;
        this.entityManager = entityManager;
    }

    @Override
    public PageResponse<Response> searchByPage(SearchRequest search, boolean mine) {
        int page = search.page();
        int size = search.size();
        List<String> sort = search.sort();
        Long periodId = search.periodId();
        List<String> statuses = search.statuses();
        String keyword = search.q();
        guard.requirePermission(Permission.ROOM_APPLICATION_READ);
        guard.requireRole(mine ? new String[] {Role.STUDENT} : new String[] {Role.STAFF});
        var actor = current.get();
        var pageable =
                PageQuery.of(
                        page,
                        size,
                        sort,
                        Set.of("id", "priorityScore", "submittedAt", "createdAt"),
                        "createdAt,desc");
        return PageResponse.from(
                applications
                        .findAll(
                                (r, q, b) -> {
                                    var filters = new ArrayList<Predicate>();
                                    if (keyword != null && !keyword.isBlank()) {
                                        var ids =
                                                jdbc.query(
                                                        "SELECT id FROM app_user WHERE full_name"
                                                                + " ILIKE ? OR username ILIKE ?",
                                                        (row, index) -> row.getLong(1),
                                                        LikePattern.contains(keyword.trim()),
                                                        LikePattern.contains(keyword.trim()));
                                        filters.add(
                                                ids.isEmpty()
                                                        ? b.disjunction()
                                                        : r.get("studentId").in(ids));
                                    }
                                    if (mine) {
                                        filters.add(b.equal(r.get("studentId"), actor.id()));
                                    } else if (!actor.canReadAllUnits()) {
                                        filters.add(
                                                b.equal(r.get("preferredUnitId"), actor.unitId()));
                                    }
                                    if (periodId != null) {
                                        filters.add(b.equal(r.get("periodId"), periodId));
                                    }
                                    if (statuses != null && !statuses.isEmpty()) {
                                        filters.add(r.get("status").in(statuses));
                                    }
                                    return b.and(filters.toArray(Predicate[]::new));
                                },
                                pageable)
                        .map(a -> toApplicationResponse(a, false)));
    }

    @Override()
    public Response getApplicationById(Long id) {
        guard.requirePermission(Permission.ROOM_APPLICATION_READ);
        var a = loadAccessibleApplication(id, false);
        return toApplicationResponse(a, true);
    }

    @Override
    @Transactional
    public Response saveOrUpdate(Input input) {
        Long id = input.id();
        if (id != null && id <= 0) {
            throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE);
        }
        // Annotation là OR, Service chỉ cấp quyền đúng với nhánh tạo hoặc sửa.
        return id == null ? createApplication(input) : updateDraftApplication(id, input);
    }

    /**
     * Sinh viên tạo đơn cho chính mình; ADMIN phải chọn sinh viên rõ ràng. Trạng thái và điểm ưu
     * tiên luôn do hệ thống xác định, lịch sử ghi đúng tài khoản thao tác.
     */
    private Response createApplication(Input input) {
        guard.requirePermission(Permission.ROOM_APPLICATION_CREATE);
        guard.requireRole(Role.STUDENT);
        if (input.periodId() == null || input.preferredUnitId() == null) {
            throw BusinessException.invalid("periodId", "Thiếu đợt hoặc đơn vị nguyện vọng");
        }
        Long studentId = resolveApplicationStudent(input.studentId());
        rooms.lockStudentProfile(studentId);
        if (rooms.hasPendingOrActiveContract(studentId)) {
            throw BusinessException.conflict(ErrorCode.CONTRACT_EXISTS);
        }
        var a = new RoomApplication();
        a.setStudentId(studentId);
        a.setPeriodId(input.periodId());
        a.setPreferredUnitId(input.preferredUnitId());
        a.setPreferredRoomTypeId(input.preferredRoomTypeId());
        a.setNote(input.note());
        a.setStatus(ApplicationStatus.DRAFT);
        a.setPriorityScore(0);
        a.setCreatedAt(OffsetDateTime.now());
        validateApplicationPeriod(a);
        applications.saveAndFlush(a);
        replaceApplicationPriorities(a.getId(), input.priorityCategoryIds());
        history.record(
                EntityType.APPLICATION,
                a.getId(),
                null,
                ApplicationStatus.DRAFT,
                current.get().id(),
                null);
        return toApplicationResponse(a, true);
    }

    private Response updateDraftApplication(Long id, Input input) {
        guard.requirePermission(Permission.ROOM_APPLICATION_UPDATE);
        guard.requireRole(Role.STUDENT);
        var a = loadAccessibleApplication(id, true);
        RoomApplicationPolicy.requireAction(
                a, current.get(), BusinessAction.EDIT, false, false);
        if (input.studentId() != null && !input.studentId().equals(a.getStudentId())) {
            throw BusinessException.invalid(
                    ApiField.STUDENT_ID, ValidationMessage.STUDENT_IMMUTABLE);
        }
        if (input.periodId() != null && !input.periodId().equals(a.getPeriodId())) {
            throw BusinessException.invalid("periodId", "Không đổi đợt của đơn đã tạo");
        }
        if (input.preferredUnitId() != null) {
            a.setPreferredUnitId(input.preferredUnitId());
        }
        if (input.preferredRoomTypeId() != null) {
            a.setPreferredRoomTypeId(input.preferredRoomTypeId());
        }
        if (input.note() != null) {
            a.setNote(input.note().trim());
        }
        validateApplicationPeriod(a);
        if (input.priorityCategoryIds() != null) {
            replaceApplicationPriorities(id, input.priorityCategoryIds());
        }
        applications.saveAndFlush(a);
        return toApplicationResponse(a, true);
    }

    /** Khóa đơn, kiểm tra Policy và ghi trạng thái + timeline trong cùng transaction. */
    @Transactional
    @Override()
    public Response performApplicationAction(Long id, String action, String reason) {
        guard.requirePermission(
                BusinessAction.CANCEL.equals(action)
                        ? Permission.ROOM_APPLICATION_DELETE
                        : Permission.ROOM_APPLICATION_UPDATE);
        var a = loadAccessibleApplication(id, true);
        var contract = liveContract(a);
        RoomApplicationPolicy.requireAction(
                a, current.get(), action, contract.live(), contract.holdActive());
        if (action.equals(BusinessAction.SUBMIT)) {
            validateApplicationPeriod(a);
            if (rooms.hasPendingOrActiveContract(a.getStudentId())) {
                throw BusinessException.conflict(ErrorCode.CONTRACT_EXISTS);
            }
            a.setPriorityScore(recalculatePriorityScore(id));
            a.setSubmittedAt(OffsetDateTime.now());
        }
        if (action.equals(BusinessAction.REJECT)
                && (reason == null || reason.trim().length() < MIN_REASON_LENGTH)) {
            throw BusinessException.invalid(
                    ApiField.REASON, ValidationMessage.REJECT_REASON_REQUIRED);
        }
        String before = a.getStatus();
        if (BusinessAction.CANCEL.equals(action)
                && ApplicationStatus.ASSIGNED.equals(before)) {
            // Hủy đơn đang giữ chỗ phải đóng HOLD và hủy hợp đồng PENDING cùng transaction.
            releaseHoldResources(contract.contractId(), CANCEL_REASON);
        }
        a.setStatus(
                switch (action) {
                    case BusinessAction.SUBMIT -> ApplicationStatus.SUBMITTED;
                    case BusinessAction.APPROVE -> ApplicationStatus.APPROVED;
                    case BusinessAction.REJECT -> ApplicationStatus.REJECTED;
                    case BusinessAction.CANCEL -> ApplicationStatus.CANCELLED;
                    default -> throw BusinessException.invalid("action", "Hành động không hỗ trợ");
                });
        a.setStatusReason(reason);
        applications.saveAndFlush(a);
        history.record(
                EntityType.APPLICATION, id, before, a.getStatus(), current.get().id(), reason);
        return toApplicationResponse(a, true);
    }

    /**
     * Phân giường cho đơn APPROVED: khóa đơn, tạo hợp đồng PENDING theo học kỳ của đợt rồi giữ chỗ
     * HOLD qua module room. Thứ tự gọi service giữ nguyên thứ tự khóa bed → student → contract của
     * module room/contract; lỗi ở bất kỳ bước nào đều rollback cả hợp đồng và phân phòng.
     */
    @Override
    @Transactional
    public Response assignBed(Long id, Long bedId) {
        guard.requirePermission(Permission.ROOM_APPLICATION_UPDATE);
        guard.requireRole(Role.STAFF);
        if (bedId == null || bedId <= 0) {
            throw BusinessException.invalid(
                    ApiField.BED_ID, ValidationMessage.BED_REQUIRED_FOR_ASSIGN);
        }
        var a = loadAccessibleApplication(id, true);
        var existing = liveContract(a);
        // Policy chỉ cho ASSIGN khi APPROVED và chưa có hợp đồng PENDING/ACTIVE.
        RoomApplicationPolicy.requireAction(
                a, current.get(), BusinessAction.ASSIGN, existing.live(), existing.holdActive());
        var period = periods.findById(a.getPeriodId()).orElseThrow(BusinessException::notFound);
        var semester = rooms.getSemesterReference(period.getSemesterId());
        Long contractId =
                contracts.createPendingForApplication(
                        id, a.getStudentId(), semester.startDate(), semester.endDate());
        var assignment = occupancy.occupyHold(bedId, a.getStudentId(), contractId, null);
        var actor = current.get();
        String before = a.getStatus();
        a.setStatus(ApplicationStatus.ASSIGNED);
        a.setStatusReason(null);
        applications.saveAndFlush(a);
        history.record(
                EntityType.APPLICATION, id, before, a.getStatus(), actor.id(), null);
        audit.record(
                actor.id(),
                AuditAction.APPLICATION_ASSIGN,
                EntityType.APPLICATION,
                id,
                Map.of(ApiField.STATUS, before),
                assignmentValue(a, contractId, assignment),
                null);
        return toApplicationResponse(a, true, estimatedActivationDeadline());
    }

    /** Hủy giữ chỗ: đóng HOLD, hủy hợp đồng PENDING và trả đơn ASSIGNED về APPROVED. */
    @Override
    @Transactional
    public Response releaseHold(Long id, String reason) {
        guard.requirePermission(Permission.ROOM_APPLICATION_UPDATE);
        guard.requireRole(Role.STAFF);
        String note = requireReleaseReason(reason);
        var a = loadAccessibleApplication(id, true);
        var contract = liveContract(a);
        RoomApplicationPolicy.requireAction(
                a, current.get(), BusinessAction.RELEASE_HOLD, contract.live(), contract.holdActive());
        releaseHoldResources(contract.contractId(), note);
        var actor = current.get();
        String before = a.getStatus();
        a.setStatus(ApplicationStatus.APPROVED);
        a.setStatusReason(null);
        applications.saveAndFlush(a);
        history.record(EntityType.APPLICATION, id, before, a.getStatus(), actor.id(), note);
        var values = new LinkedHashMap<String, Object>();
        values.put(ApiField.CONTRACT_ID, contract.contractId());
        values.put(ApiField.STATUS, a.getStatus());
        audit.record(
                actor.id(),
                AuditAction.APPLICATION_RELEASE_HOLD,
                EntityType.APPLICATION,
                id,
                beforeValue(before, contract.contractId()),
                values,
                note);
        return toApplicationResponse(a, true);
    }

    /** Staff xác nhận một diện ưu tiên rồi tính lại điểm của cả đơn trong cùng transaction. */
    @Override
    @Transactional
    public Response verifyPriority(Long id, Long categoryId, Boolean verified, String note) {
        guard.requirePermission(Permission.ROOM_APPLICATION_UPDATE);
        guard.requireRole(Role.STAFF);
        if (verified == null) {
            throw BusinessException.invalid(
                    ApiField.VERIFIED, ValidationMessage.PRIORITY_VERIFIED_REQUIRED);
        }
        if (categoryId == null || categoryId <= 0) {
            throw BusinessException.notFound();
        }
        var a = loadAccessibleApplication(id, true);
        var contract = liveContract(a);
        RoomApplicationPolicy.requireAction(
                a, current.get(), BusinessAction.VERIFY_PRIORITY, contract.live(), contract.holdActive());
        var row =
                priorities
                        .findById(new ApplicationPriorityId(id, categoryId))
                        .orElseThrow(BusinessException::notFound);
        var actor = current.get();
        Boolean previous = row.getVerified();
        String trimmedNote = normalizeNote(note);
        row.setVerified(verified);
        row.setVerifiedBy(actor.id());
        row.setVerifiedAt(OffsetDateTime.now());
        row.setNote(trimmedNote);
        priorities.saveAndFlush(row);
        a.setPriorityScore(recalculatePriorityScore(id));
        applications.saveAndFlush(a);
        var before = new LinkedHashMap<String, Object>();
        before.put(ApiField.VERIFIED, previous);
        var after = new LinkedHashMap<String, Object>();
        after.put(ApiField.VERIFIED, verified);
        after.put(ApiField.ID, categoryId);
        after.put(ApiField.PRIORITY_SCORE, a.getPriorityScore());
        audit.record(
                actor.id(),
                AuditAction.APPLICATION_PRIORITY_VERIFY,
                EntityType.APPLICATION,
                id,
                before,
                after,
                trimmedNote);
        return toApplicationResponse(a, true);
    }

    /** Hủy HOLD và hợp đồng PENDING; Policy đã bảo đảm còn HOLD và hợp đồng chưa kích hoạt. */
    private void releaseHoldResources(Long contractId, String reason) {
        if (contractId == null) {
            throw BusinessException.conflict(ErrorCode.NO_HOLD_ASSIGNMENT);
        }
        var actor = current.get();
        occupancy.release(contractId, AssignmentEndReason.HOLD_CANCELLED, actor.id(), reason);
        contracts.cancelPendingContract(contractId, reason);
    }

    /**
     * Trạng thái hợp đồng còn sống của đơn và việc còn giữ chỗ HOLD hay không. Chỉ hỏi module
     * contract/room khi đơn đã APPROVED/ASSIGNED để danh sách và thao tác sớm không tốn truy vấn.
     */
    private LiveContract liveContract(RoomApplication a) {
        if (!ApplicationStatus.APPROVED.equals(a.getStatus())
                && !ApplicationStatus.ASSIGNED.equals(a.getStatus())) {
            return NO_CONTRACT;
        }
        Long contractId = contracts.findLiveContractIdForApplication(a.getId());
        if (contractId == null) {
            return NO_CONTRACT;
        }
        boolean holdActive =
                occupancyQuery
                        .findOpenAssignmentByContract(contractId)
                        .map(bed -> AssignmentKind.HOLD.equals(bed.kind()))
                        .orElse(false);
        var activationInfo = contracts.findActivationInfo(contractId);
        String code = activationInfo != null ? activationInfo.code() : null;
        OffsetDateTime deadline =
                activationInfo != null ? activationInfo.activationDeadline() : null;
        return new LiveContract(contractId, true, holdActive, code, deadline);
    }

    /** Hạn kích hoạt hiển thị ngay sau phân giường, tính theo cùng tham số cấu hình của contract. */
    private OffsetDateTime estimatedActivationDeadline() {
        return OffsetDateTime.now()
                .plusDays(configs.positiveInt(ConfigKey.CONTRACT_ACTIVATION_DAYS));
    }

    private static String requireReleaseReason(String reason) {
        if (reason == null || reason.trim().length() < MIN_REASON_LENGTH) {
            throw BusinessException.invalid(
                    ApiField.REASON, ValidationMessage.RELEASE_HOLD_REASON_REQUIRED);
        }
        return reason.trim();
    }

    private static String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.strip();
    }

    private static Map<String, Object> beforeValue(String status, Long contractId) {
        var value = new LinkedHashMap<String, Object>();
        value.put(ApiField.STATUS, status);
        value.put(ApiField.CONTRACT_ID, contractId);
        return value;
    }

    private static Map<String, Object> assignmentValue(
            RoomApplication application, Long contractId, BedOccupancyService.Assignment bed) {
        var value = new LinkedHashMap<String, Object>();
        value.put(ApiField.STATUS, application.getStatus());
        value.put(ApiField.CONTRACT_ID, contractId);
        value.put(ApiField.BED_ID, bed.bedId());
        value.put(ApiField.BED_CODE, bed.bedCode());
        value.put(ApiField.ROOM_CODE, bed.roomCode());
        return value;
    }

    /** Tính điểm ưu tiên: tổng trọng số các diện chưa bị xác nhận "không đúng". */
    private int recalculatePriorityScore(Long applicationId) {
        return priorities.findAll((r, q, b) -> b.equal(r.get("applicationId"), applicationId)).stream()
                .filter(p -> !Boolean.FALSE.equals(p.getVerified()))
                .mapToInt(
                        p ->
                                categories
                                        .findById(p.getCategoryId())
                                        .orElseThrow(BusinessException::notFound)
                                        .getWeight())
                .sum();
    }

    private RoomApplication loadAccessibleApplication(Long id, boolean writing) {
        guard.requireRole(Role.STUDENT, Role.STAFF);
        if (writing) {
            jdbc.query(
                    "SELECT id FROM room_application WHERE id=? FOR UPDATE",
                    (r, i) -> r.getLong(1),
                    id);
        }
        var a = applications.findById(id).orElseThrow(BusinessException::notFound);
        if (writing) {
            entityManager.refresh(a);
        }
        var actor = current.get();
        if (actor.is(Role.STUDENT) && !actor.id().equals(a.getStudentId())) {
            throw BusinessException.notFound();
        }
        if (actor.is(Role.STAFF)) {
            if (writing) {
                guard.requireUnit(a.getPreferredUnitId());
            } else {
                guard.requireReadableUnit(a.getPreferredUnitId());
            }
        }
        return a;
    }

    /** Không dùng ID ADMIN làm sinh viên và không cho sinh viên gửi ID của người khác. */
    private Long resolveApplicationStudent(Long requestedStudentId) {
        var actor = current.get();
        if (!actor.isAdmin()) {
            if (requestedStudentId != null && !requestedStudentId.equals(actor.id())) {
                throw BusinessException.forbidden();
            }
            return actor.id();
        }
        if (requestedStudentId == null) {
            throw BusinessException.invalid(
                    ApiField.STUDENT_ID, ValidationMessage.STUDENT_REQUIRED_FOR_ADMIN);
        }
        var student = accounts.user(requestedStudentId);
        if (!Role.STUDENT.equals(student.role())) {
            throw BusinessException.invalid(
                    ApiField.STUDENT_ID, ValidationMessage.STUDENT_ACCOUNT_REQUIRED);
        }
        return student.id();
    }

    private void validateApplicationPeriod(RoomApplication a) {
        var p = periods.findById(a.getPeriodId()).orElseThrow(BusinessException::notFound);
        var now = OffsetDateTime.now();
        if (!p.getKind().equals(RegistrationKind.REGULAR)
                || now.isBefore(p.getOpenAt())
                || !now.isBefore(p.getCloseAt())) {
            throw BusinessException.conflict(ErrorCode.PERIOD_NOT_OPEN);
        }
        if (!periodUnits.existsById(
                new RegistrationPeriodUnitId(p.getId(), a.getPreferredUnitId()))) {
            throw new BusinessException(ErrorCode.UNIT_NOT_IN_PERIOD, HttpStatus.BAD_REQUEST);
        }
        if (a.getPreferredRoomTypeId() != null) {
            rooms.requireActiveRoomType(a.getPreferredRoomTypeId());
        }
    }

    private void replaceApplicationPriorities(Long id, List<Long> ids) {
        List<Long> selected = ids == null ? List.of() : ids;
        if (new HashSet<>(selected).size() != selected.size()) {
            throw BusinessException.invalid("priorityCategoryIds", "Không khai trùng diện ưu tiên");
        }
        for (Long categoryId : selected) {
            if (!categories.exists(
                    (r, q, b) ->
                            b.and(b.equal(r.get("id"), categoryId), b.isTrue(r.get("active"))))) {
                throw BusinessException.notFound();
            }
        }
        priorities.deleteAll(priorities.findAll((r, q, b) -> b.equal(r.get("applicationId"), id)));
        priorities.flush();
        for (Long categoryId : selected) {
            var row = new ApplicationPriority();
            row.setApplicationId(id);
            row.setCategoryId(categoryId);
            priorities.save(row);
        }
        priorities.flush();
    }

    private Response toApplicationResponse(RoomApplication a, boolean detail) {
        return toApplicationResponse(a, detail, null);
    }

    /**
     * Dựng chi tiết đơn; danh sách bỏ qua dữ liệu hợp đồng/giường để không phát sinh truy vấn
     * chéo module. {@code activationDeadline} chỉ có ngay sau phân giường vì hạn kích hoạt do
     * module contract chụp lúc tạo hợp đồng.
     */
    private Response toApplicationResponse(
            RoomApplication a, boolean detail, OffsetDateTime activationDeadline) {
        var user = accounts.user(a.getStudentId());
        var contract = detail ? liveContract(a) : NO_CONTRACT;
        var priorityRows =
                detail
                        ? priorities
                                .findAll((r, q, b) -> b.equal(r.get("applicationId"), a.getId()))
                                .stream()
                                .map(
                                        p -> {
                                            var category =
                                                    categories
                                                            .findById(p.getCategoryId())
                                                            .orElseThrow(
                                                                    BusinessException::notFound);
                                            var verifier =
                                                    p.getVerifiedBy() == null
                                                            ? null
                                                            : accounts.user(p.getVerifiedBy());
                                            return new Priority(
                                                    p.getCategoryId(),
                                                    category.getName(),
                                                    category.getWeight(),
                                                    p.getVerified(),
                                                    p.getVerifiedBy(),
                                                    verifier == null
                                                            ? null
                                                            : verifier.fullName(),
                                                    p.getVerifiedAt(),
                                                    p.getNote());
                                        })
                                .toList()
                        : null;
        return new Response(
                a.getId(),
                a.getStatus(),
                new StudentRef(user.id(), user.username(), user.fullName(), user.gender()),
                a.getPeriodId(),
                a.getPreferredUnitId(),
                a.getPreferredRoomTypeId(),
                a.getPriorityScore(),
                a.getNote(),
                a.getStatusReason(),
                a.getSubmittedAt(),
                detail ? contract.contractId() : null,
                detail ? buildHold(a, contract, activationDeadline) : null,
                priorityRows,
                detail
                        ? RoomApplicationPolicy.allowedActions(
                                a, current.get(), contract.live(), contract.holdActive())
                        : null,
                detail ? history.list(EntityType.APPLICATION, a.getId()) : null);
    }

    /** Chỉ đơn ASSIGNED còn hợp đồng sống và phân phòng mở mới có thông tin giường đang giữ. */
    private Hold buildHold(
            RoomApplication a, LiveContract contract, OffsetDateTime activationDeadline) {
        if (!ApplicationStatus.ASSIGNED.equals(a.getStatus()) || contract.contractId() == null) {
            return null;
        }
        var bed =
                occupancyQuery
                        .findOpenAssignmentByContract(contract.contractId())
                        .orElse(null);
        if (bed == null) {
            return null;
        }
        OffsetDateTime deadline =
                activationDeadline != null ? activationDeadline : contract.activationDeadline();
        return new Hold(
                bed.assignmentId(),
                new BedRef(
                        bed.bedId(),
                        bed.bedCode(),
                        bed.roomId(),
                        bed.roomCode(),
                        bed.buildingId(),
                        bed.buildingCode(),
                        bed.managementUnitId()),
                bed.bedId(),
                bed.bedCode(),
                bed.roomId(),
                bed.roomCode(),
                bed.buildingId(),
                bed.buildingCode(),
                bed.managementUnitId(),
                contract.contractId(),
                contract.contractCode(),
                deadline);
    }

    /** Hợp đồng còn sống của đơn và tình trạng HOLD; giữ ID thay vì trả entity ra ngoài module. */
    private record LiveContract(
            Long contractId,
            boolean live,
            boolean holdActive,
            String contractCode,
            OffsetDateTime activationDeadline) {}
}

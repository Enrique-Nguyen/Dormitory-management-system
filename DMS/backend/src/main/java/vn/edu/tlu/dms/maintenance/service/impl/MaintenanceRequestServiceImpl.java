package vn.edu.tlu.dms.maintenance.service.impl;

import jakarta.persistence.EntityManager;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.billing.service.DepositService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ConfigKey;
import vn.edu.tlu.dms.common.constants.DmsConstants.DepositType;
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
import vn.edu.tlu.dms.common.service.ConfigService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.ChargeableStudent;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Conclude;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Deduction;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Photo;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Response;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Save;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.StudentRef;
import vn.edu.tlu.dms.maintenance.domain.MaintenanceRequest;
import vn.edu.tlu.dms.maintenance.domain.MaintenanceRequestPolicy;
import vn.edu.tlu.dms.maintenance.domain.MaintenanceRequestPolicy.Cause;
import vn.edu.tlu.dms.maintenance.domain.TicketConcludedEvent;
import vn.edu.tlu.dms.maintenance.domain.TicketStatusChangedEvent;
import vn.edu.tlu.dms.maintenance.repository.MaintenancePhotoRepository;
import vn.edu.tlu.dms.maintenance.repository.MaintenanceRequestRepository;
import vn.edu.tlu.dms.maintenance.service.MaintenancePhotoService;
import vn.edu.tlu.dms.maintenance.service.MaintenanceRequestService;
import vn.edu.tlu.dms.room.service.OccupancyQuery;
import vn.edu.tlu.dms.room.service.OccupancyQuery.Occupant;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class MaintenanceRequestServiceImpl implements MaintenanceRequestService {

    /** Giới hạn độ dài reason của status_history/audit_log để không cắt mất giao dịch. */
    private static final int REASON_MAX_LENGTH = 500;

    /** Ghi chú kết luận và lý do bỏ bớt người chịu trách nhiệm cần tối thiểu 5 ký tự. */
    private static final int MIN_NOTE_LENGTH = 5;

    private static final String NOTE_FIELD = "note";

    private static final String INVALID_IDS_FIELD = "invalidIds";

    private final MaintenanceRequestRepository requests;

    private final MaintenancePhotoRepository photoRows;

    private final MaintenancePhotoService photos;

    private final RoomReferenceService rooms;

    private final AccountReferenceService accounts;

    private final OccupancyQuery occupancy;

    private final DepositService deposits;

    private final ConfigService config;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final StatusHistoryService history;

    private final JdbcTemplate jdbc;

    private final AuditService audit;

    private final EntityManager entityManager;

    private final ApplicationEventPublisher events;

    public MaintenanceRequestServiceImpl(
            MaintenanceRequestRepository requests,
            MaintenancePhotoRepository photoRows,
            MaintenancePhotoService photos,
            RoomReferenceService rooms,
            AccountReferenceService accounts,
            OccupancyQuery occupancy,
            DepositService deposits,
            ConfigService config,
            CurrentUser current,
            ScopeGuard guard,
            StatusHistoryService history,
            JdbcTemplate jdbc,
            AuditService audit,
            EntityManager entityManager,
            ApplicationEventPublisher events) {
        this.requests = requests;
        this.photoRows = photoRows;
        this.photos = photos;
        this.rooms = rooms;
        this.accounts = accounts;
        this.occupancy = occupancy;
        this.deposits = deposits;
        this.config = config;
        this.current = current;
        this.guard = guard;
        this.history = history;
        this.jdbc = jdbc;
        this.audit = audit;
        this.entityManager = entityManager;
        this.events = events;
    }

    @Override
    public PageResponse<Response> searchByPage(SearchRequest request) {
        guard.requirePermission(Permission.MAINTENANCE_READ);
        guard.requireRole(Role.STUDENT, Role.STAFF, Role.TECHNICIAN);
        var actor = current.get();
        var pageable =
                PageQuery.of(
                        request.page(),
                        request.size(),
                        request.sort(),
                        Set.of("id", "code", "category", "severity", "status", "createdAt"),
                        "createdAt,desc");
        var sort =
                Sort.by(
                        pageable.getSort().stream()
                                .map(
                                        order ->
                                                new Sort.Order(
                                                        order.getDirection(),
                                                        "createdAt".equals(order.getProperty())
                                                                ? "created_at"
                                                                : order.getProperty()))
                                .toList());
        Long roomId =
                actor.is(Role.STUDENT)
                        ? rooms.findCurrentStayingRoomId(actor.id()).orElse(-1L)
                        : -1L;
        List<String> statuses = request.statuses();
        return PageResponse.from(
                requests.searchVisibleRequests(
                                actor.role(),
                                actor.id(),
                                actor.unitId(),
                                actor.crossUnitAccess(),
                                roomId,
                                statuses == null || statuses.isEmpty(),
                                statuses == null || statuses.isEmpty() ? List.of("") : statuses,
                                request.category(),
                                request.severity(),
                                LikePattern.contains(request.q()),
                                PageRequest.of(request.page(), request.size(), sort))
                        .map(row -> toMaintenanceResponse(row, false)));
    }

    @Transactional
    @Override
    public Response saveOrUpdate(Save input, List<MultipartFile> files) {
        guard.requirePermission(
                input.id() == null ? Permission.MAINTENANCE_CREATE : Permission.MAINTENANCE_UPDATE);
        if (input.id() != null && input.id() <= 0) {
            throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE_INTEGER);
        }
        if (input.id() == null) {
            return createMaintenanceRequest(input, files);
        }
        if (files != null && files.stream().anyMatch(f -> f != null && !f.isEmpty())) {
            throw BusinessException.invalid(ApiField.PHOTOS, ValidationMessage.PHOTO_UPDATE_UNSUPPORTED);
        }
        // Khóa trước, nạp lại dữ liệu thật rồi mới kiểm tra OPEN/chủ sở hữu, tránh ghi theo dữ liệu
        // cũ.
        var req = loadAccessibleRequest(input.id(), true);
        if (!current.get().isAdmin() && !current.get().id().equals(req.getRequesterId())) {
            throw BusinessException.notFound();
        }
        if (!MaintenanceStatus.OPEN.equals(req.getStatus())) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        if (input.roomId() != null || input.buildingId() != null || input.floor() != null) {
            throw BusinessException.invalid(ApiField.ROOM_ID, ValidationMessage.MAINTENANCE_LOCATION_IMMUTABLE);
        }
        if (input.category() == null && input.severity() == null && input.description() == null) {
            throw BusinessException.invalid(ApiField.FORM, ValidationMessage.NOTHING_TO_UPDATE);
        }
        if (input.category() != null) req.setCategory(input.category());
        if (input.severity() != null) req.setSeverity(input.severity());
        if (input.description() != null) {
            requireNonBlank(input.description(), "description");
            req.setDescription(input.description().trim());
        }
        requests.saveAndFlush(req);
        audit.record(
                current.get().id(),
                AuditAction.MAINTENANCE_UPDATE,
                EntityType.MAINTENANCE,
                input.id(),
                null,
                input,
                null);
        return toMaintenanceResponse(req, true);
    }

    @Override()
    public Response getMaintenanceRequestById(Long id) {
        guard.requirePermission(Permission.MAINTENANCE_READ);
        return toMaintenanceResponse(loadAccessibleRequest(id, false), true);
    }

    /** Ticket, ảnh và timeline là một thao tác ghi. Lỗi ảnh hoặc lỗi CSDL làm rollback toàn bộ. */
    private Response createMaintenanceRequest(Save input, List<MultipartFile> files) {
        guard.requirePermission(Permission.MAINTENANCE_CREATE);
        guard.requireRole(Role.STUDENT, Role.STAFF);
        requireNonBlank(input.category(), "category");
        requireNonBlank(input.severity(), "severity");
        requireNonBlank(input.description(), "description");
        var actor = current.get();
        var request = new MaintenanceRequest();
        if (actor.is(Role.STUDENT)) {
            if (input.roomId() != null || input.buildingId() != null || input.floor() != null) {
                throw new BusinessException(
                        ErrorCode.TICKET_LOCATION_INVALID, HttpStatus.BAD_REQUEST);
            }
            request.setRoomId(
                    rooms.findCurrentStayingRoomId(actor.id())
                            .orElseThrow(() -> BusinessException.conflict(ErrorCode.NOT_STAYING)));
        } else {
            boolean roomLocation =
                    input.roomId() != null && input.buildingId() == null && input.floor() == null;
            boolean commonLocation =
                    input.roomId() == null && input.buildingId() != null && input.floor() != null;
            if (!roomLocation && !commonLocation) {
                throw new BusinessException(
                        ErrorCode.TICKET_LOCATION_INVALID, HttpStatus.BAD_REQUEST);
            }
            if (roomLocation) {
                guard.requireUnit(rooms.getRoomLocation(input.roomId()).unitId());
                request.setRoomId(input.roomId());
            } else {
                guard.requireUnit(rooms.getBuildingManagementUnitId(input.buildingId()));
                request.setBuildingId(input.buildingId());
                request.setFloor(input.floor());
            }
        }
        request.setCode(
                jdbc.queryForObject(
                        "SELECT fn_next_code('TK','seq_maintenance_code',false)", String.class));
        request.setRequesterId(actor.id());
        request.setCategory(input.category());
        request.setSeverity(input.severity());
        request.setDescription(input.description().trim());
        request.setStatus(MaintenanceStatus.OPEN);
        request.setCreatedAt(OffsetDateTime.now());
        requests.saveAndFlush(request);
        photos.savePhotos(request.getId(), files);
        photoRows.flush();
        history.record(
                EntityType.MAINTENANCE,
                request.getId(),
                null,
                MaintenanceStatus.OPEN,
                actor.id(),
                null);
        return toMaintenanceResponse(request, true);
    }

    /** Kiểm tra kỹ thuật viên cùng đơn vị trước khi giao ticket, không chỉ kiểm tra role. */
    @Transactional
    @Override()
    public Response assignTechnician(Long id, Long technicianId) {
        guard.requirePermission(Permission.MAINTENANCE_UPDATE);
        guard.requireRole(Role.STAFF);
        var request = loadAccessibleRequest(id, true);
        MaintenanceRequestPolicy.requireAction(
                request, current.get(), false, BusinessAction.ASSIGN);
        var technician = accounts.user(technicianId);
        if (!technician.role().equals(Role.TECHNICIAN)
                || !Objects.equals(technician.unitId(), requestUnitId(request))) {
            throw BusinessException.conflict(ErrorCode.TECHNICIAN_OUT_OF_UNIT);
        }
        request.setAssignedTo(technicianId);
        changeStatusAndRecordHistory(request, MaintenanceStatus.ASSIGNED, null);
        return toMaintenanceResponse(request, true);
    }

    @Transactional
    @Override()
    public Response performMaintenanceAction(Long id, String action, String reason) {
        guard.requirePermission(
                action.equals(BusinessAction.CANCEL)
                        ? Permission.MAINTENANCE_DELETE
                        : Permission.MAINTENANCE_UPDATE);
        var request = loadAccessibleRequest(id, true);
        var actor = current.get();
        boolean staffCanAccept = staffCanAcceptRequest(request);
        MaintenanceRequestPolicy.requireAction(request, actor, staffCanAccept, action);
        if (Set.of(
                                BusinessAction.REJECT,
                                BusinessAction.REOPEN,
                                BusinessAction.ADD_NOTE,
                                BusinessAction.RESOLVE)
                        .contains(action)
                && (reason == null || reason.trim().length() < 5)) {
            throw BusinessException.invalid(ApiField.REASON, ValidationMessage.REASON_MIN_5);
        }
        if (action.equals(BusinessAction.ACCEPT)
                && (actor.is(Role.STAFF) || actor.isAdmin())
                && !actor.id().equals(request.getRequesterId())
                && (reason == null || reason.trim().length() < 5)) {
            throw BusinessException.invalid(ApiField.REASON, ValidationMessage.PROXY_ACCEPT_REASON_REQUIRED);
        }
        String next =
                switch (action) {
                    case BusinessAction.REJECT -> MaintenanceStatus.REJECTED;
                    case BusinessAction.CANCEL -> MaintenanceStatus.CANCELLED;
                    case BusinessAction.START, BusinessAction.REOPEN, BusinessAction.ADD_NOTE ->
                            MaintenanceStatus.IN_PROGRESS;
                    case BusinessAction.RESOLVE -> MaintenanceStatus.RESOLVED;
                    case BusinessAction.ACCEPT -> MaintenanceStatus.CLOSED;
                    default -> throw BusinessException.invalid(ApiField.ACTION, ValidationMessage.ACTION_UNSUPPORTED);
                };
        if (next.equals(MaintenanceStatus.RESOLVED)) {
            request.setResolvedAt(OffsetDateTime.now());
        }
        if (action.equals(BusinessAction.REOPEN)) {
            request.setResolvedAt(null);
        }
        if (next.equals(MaintenanceStatus.CLOSED)) {
            request.setClosedAt(OffsetDateTime.now());
        }
        changeStatusAndRecordHistory(request, next, reason);
        return toMaintenanceResponse(request, true);
    }

    @Override
    public List<ChargeableStudent> findChargeableStudents(Long id) {
        guard.requirePermission(Permission.MAINTENANCE_READ);
        guard.requireRole(Role.STAFF, Role.ADMIN);
        var request = loadAccessibleRequest(id, false);
        requireRoomTicket(request);
        // Danh sách dùng cho thao tác ghi nên phải đúng đơn vị ghi, không mở theo quyền đọc tổng
        // hợp.
        guard.requireUnit(requestUnitId(request));
        requireConclusiveState(request);
        return chargeableStudents(request);
    }

    /** Kết luận và trừ cọc là một transaction; một lần deduct lỗi sẽ rollback cả ticket/audit. */
    @Transactional
    @Override
    public Response conclude(Long id, Conclude input) {
        guard.requirePermission(Permission.MAINTENANCE_UPDATE);
        guard.requireRole(Role.STAFF, Role.ADMIN);
        var request = loadAccessibleRequest(id, true);
        requireRoomTicket(request);
        requireConclusiveState(request);
        Cause cause = Cause.from(input.cause());
        if (cause == null) {
            throw BusinessException.invalid(ApiField.CAUSE, ValidationMessage.CAUSE_INVALID);
        }
        String note = input.note();
        List<Deduction> deductions =
                cause == Cause.WEAR
                        ? concludeWear(input, note)
                        : concludeMisuse(request, input, note);
        request.setCause(cause.name());
        request.setRepairCost(input.repairCost());
        request.setCostNote(note);
        request.setCostDecidedBy(current.get().id());
        request.setCostDecidedAt(OffsetDateTime.now());
        requests.saveAndFlush(request);
        String reason = conclusionReason(cause, input.repairCost(), deductions, note);
        history.record(
                EntityType.MAINTENANCE,
                request.getId(),
                MaintenanceStatus.CLOSED,
                MaintenanceStatus.CLOSED,
                current.get().id(),
                reason);
        audit.record(
                current.get().id(),
                AuditAction.MAINTENANCE_CONCLUDE,
                EntityType.MAINTENANCE,
                request.getId(),
                null,
                conclusionAudit(cause, input, deductions, note),
                reason);
        events.publishEvent(
                new TicketConcludedEvent(
                        request.getId(),
                        request.getRequesterId(),
                        cause.name(),
                        deductions.stream().map(deduction -> deduction.student().id()).toList()));
        return toMaintenanceResponse(request, true, deductions);
    }

    /** WEAR không trừ cọc, không nhận chi phí và bắt buộc ghi chú theo BR-10/FR-MNT-09. */
    private List<Deduction> concludeWear(Conclude input, String note) {
        if (input.repairCost() != null) {
            throw new BusinessException(ErrorCode.COST_INVALID, HttpStatus.BAD_REQUEST);
        }
        if (input.chargedStudentIds() != null && !input.chargedStudentIds().isEmpty()) {
            throw new BusinessException(ErrorCode.CHARGED_STUDENTS_INVALID, HttpStatus.BAD_REQUEST);
        }
        if (note == null || note.length() < MIN_NOTE_LENGTH) {
            throw BusinessException.invalid(NOTE_FIELD, ValidationMessage.WEAR_NOTE_REQUIRED);
        }
        return List.of();
    }

    /** MISUSE thu đủ chi phí từ đúng các sinh viên đang ở tại thời điểm tạo ticket. */
    private List<Deduction> concludeMisuse(
            MaintenanceRequest request, Conclude input, String note) {
        Long cost = input.repairCost();
        if (cost == null || cost <= 0) {
            throw new BusinessException(ErrorCode.COST_INVALID, HttpStatus.BAD_REQUEST);
        }
        List<Occupant> candidates =
                occupancy.findOccupantsAt(request.getRoomId(), request.getCreatedAt()).stream()
                        .sorted(Comparator.comparing(Occupant::contractId))
                        .toList();
        if (candidates.isEmpty()) {
            throw BusinessException.conflict(ErrorCode.NO_OCCUPANT_TO_CHARGE);
        }
        LinkedHashSet<Long> candidateIds = new LinkedHashSet<>();
        candidates.forEach(candidate -> candidateIds.add(candidate.studentId()));
        LinkedHashSet<Long> selectedIds = new LinkedHashSet<>();
        if (input.chargedStudentIds() == null || input.chargedStudentIds().isEmpty()) {
            selectedIds.addAll(candidateIds);
        } else {
            List<Long> invalidIds = new ArrayList<>();
            for (Long studentId : input.chargedStudentIds()) {
                if (studentId == null || !candidateIds.contains(studentId)) {
                    invalidIds.add(studentId);
                } else {
                    selectedIds.add(studentId);
                }
            }
            if (!invalidIds.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.CHARGED_STUDENTS_INVALID,
                        HttpStatus.BAD_REQUEST,
                        Map.of(INVALID_IDS_FIELD, invalidIds));
            }
        }
        List<Occupant> selected =
                candidates.stream()
                        .filter(candidate -> selectedIds.contains(candidate.studentId()))
                        .toList();
        if (selected.size() < candidates.size()
                && (note == null || note.length() < MIN_NOTE_LENGTH)) {
            throw BusinessException.invalid(NOTE_FIELD, ValidationMessage.CHARGED_NOTE_REQUIRED);
        }
        // Mỗi sinh viên được chọn phải nhận khoản trừ dương; không thể chia đồng tiền nhỏ hơn số
        // người.
        if (cost < selected.size()) {
            throw new BusinessException(ErrorCode.COST_INVALID, HttpStatus.BAD_REQUEST);
        }
        LocalDate dueDate =
                LocalDate.now()
                        .plusDays(config.positiveInt(ConfigKey.DEPOSIT_TOPUP_DUE_DAYS));
        long base = cost / selected.size();
        long remainder = cost % selected.size();
        List<Deduction> deductions = new ArrayList<>(selected.size());
        for (int index = 0; index < selected.size(); index++) {
            Occupant occupant = selected.get(index);
            long amount = base + (index == selected.size() - 1 ? remainder : 0);
            deposits.deduct(occupant.contractId(), request.getId(), amount, dueDate, note);
            deductions.add(
                    new Deduction(
                            occupant.contractId(),
                            studentRef(occupant.studentId()),
                            amount,
                            dueDate));
        }
        return deductions;
    }

    private List<ChargeableStudent> chargeableStudents(MaintenanceRequest request) {
        return occupancy.findOccupantsAt(request.getRoomId(), request.getCreatedAt()).stream()
                .sorted(Comparator.comparing(Occupant::contractId))
                .map(
                        occupant ->
                                new ChargeableStudent(
                                        studentRef(occupant.studentId()),
                                        occupant.contractId(),
                                        contractStatus(occupant.contractId())))
                .toList();
    }

    private StudentRef studentRef(Long studentId) {
        var user = accounts.user(studentId);
        // Tên đăng nhập của sinh viên chính là mã sinh viên theo quy ước tài khoản hiện tại.
        return new StudentRef(user.id(), user.username(), user.fullName(), user.gender());
    }

    /**
     * Đọc trạng thái hợp đồng qua record của tầng Service billing, không phụ thuộc DTO. Trạng thái
     * chỉ dùng để hiển thị nên trả null thay vì làm hỏng cả danh sách khi thiếu quyền/ngoài phạm vi.
     */
    private String contractStatus(Long contractId) {
        if (!guard.hasPermission(Permission.DEPOSIT_READ)) {
            return null;
        }
        try {
            return deposits.getBalanceView(contractId).status();
        } catch (BusinessException exception) {
            return null;
        }
    }

    /**
     * Đọc lại các khoản trừ cọc đã ghi cho ticket để màn chi tiết không phải suy đoán. Chỉ gọi
     * dịch vụ công khai của billing; lỗi đọc sổ cọc của một hợp đồng lịch sử được bỏ qua vì lỗi
     * hiển thị không được biến thành lỗi 404/403 của ticket.
     */
    private List<Deduction> storedDeductions(MaintenanceRequest request) {
        if (request.getCause() == null
                || Cause.WEAR.name().equals(request.getCause())
                || request.getRoomId() == null
                || !guard.hasPermission(Permission.DEPOSIT_READ)) {
            return List.of();
        }
        List<Deduction> deductions = new ArrayList<>();
        for (Occupant occupant :
                occupancy.findOccupantsAt(request.getRoomId(), request.getCreatedAt())) {
            List<DepositService.DepositEntry> entries;
            try {
                entries = deposits.listEntries(occupant.contractId());
            } catch (BusinessException exception) {
                continue;
            }
            for (DepositService.DepositEntry entry : entries) {
                if (!Objects.equals(request.getId(), entry.maintenanceRequestId())
                        || !DepositType.DEDUCTION.equals(entry.type())) {
                    continue;
                }
                long amount = Math.abs(entry.amount());
                deductions.add(
                        new Deduction(
                                occupant.contractId(),
                                studentRef(occupant.studentId()),
                                amount,
                                entry.dueDate()));
            }
        }
        deductions.sort(Comparator.comparing(Deduction::contractId));
        return deductions;
    }

    private Map<String, Object> conclusionAudit(
            Cause cause, Conclude input, List<Deduction> deductions, String note) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(ApiField.CAUSE, cause.name());
        if (input.repairCost() != null) {
            details.put(ApiField.REPAIR_COST, input.repairCost());
        }
        details.put(
                ApiField.CHARGED_STUDENT_IDS,
                deductions.stream().map(deduction -> deduction.student().id()).toList());
        if (note != null) {
            details.put(NOTE_FIELD, note);
        }
        return details;
    }

    private String conclusionReason(
            Cause cause, Long repairCost, List<Deduction> deductions, String note) {
        String summary =
                cause == Cause.WEAR
                        ? "Kết luận hao mòn tự nhiên"
                        : "Kết luận hư hỏng do sử dụng; trừ cọc "
                                + repairCost
                                + " đ cho "
                                + deductions.size()
                                + " sinh viên";
        if (note != null) {
            summary += ": " + note;
        }
        return truncate(summary, REASON_MAX_LENGTH);
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void requireRoomTicket(MaintenanceRequest request) {
        if (request.getRoomId() == null) {
            throw BusinessException.conflict(ErrorCode.COMMON_AREA_NO_CHARGE);
        }
    }

    /** Đã kết luận được báo riêng để người dùng không thử lại thao tác không thể đảo ngược. */
    private void requireConclusiveState(MaintenanceRequest request) {
        if (!MaintenanceStatus.CLOSED.equals(request.getStatus())) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        if (request.getCause() != null) {
            throw BusinessException.conflict(ErrorCode.TICKET_ALREADY_CONCLUDED);
        }
    }

    @Override()
    public Path getAuthorizedPhotoFile(Long id, Long photoId) {
        guard.requirePermission(Permission.MAINTENANCE_READ);
        loadAccessibleRequest(id, false);
        return photos.getPhotoFile(id, photoId);
    }

    private MaintenanceRequest loadAccessibleRequest(Long id, boolean writing) {
        guard.requireRole(Role.STUDENT, Role.STAFF, Role.TECHNICIAN);
        if (writing) {
            jdbc.query(
                    "SELECT id FROM maintenance_request WHERE id=? FOR UPDATE",
                    (r, i) -> r.getLong(1),
                    id);
        }
        var request = requests.findById(id).orElseThrow(BusinessException::notFound);
        if (writing) {
            entityManager.refresh(request);
        }
        var actor = current.get();
        if (actor.is(Role.STUDENT)
                && !actor.id().equals(request.getRequesterId())
                && !Objects.equals(
                        request.getRoomId(),
                        rooms.findCurrentStayingRoomId(actor.id()).orElse(-1L))) {
            throw BusinessException.notFound();
        }
        if (actor.is(Role.TECHNICIAN) && !actor.id().equals(request.getAssignedTo())) {
            throw BusinessException.notFound();
        }
        if (actor.is(Role.STAFF)) {
            if (writing) {
                guard.requireUnit(requestUnitId(request));
            } else {
                guard.requireReadableUnit(requestUnitId(request));
            }
        }
        return request;
    }

    private Long requestUnitId(MaintenanceRequest r) {
        return r.getRoomId() != null
                ? rooms.getRoomLocation(r.getRoomId()).unitId()
                : rooms.getBuildingManagementUnitId(r.getBuildingId());
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.invalid(field, ValidationMessage.REQUIRED);
        }
    }

    private boolean staffCanAcceptRequest(MaintenanceRequest r) {
        var actor = current.get();
        return actor.is(Role.STAFF)
                && Objects.equals(actor.unitId(), requestUnitId(r))
                && (accounts.user(r.getRequesterId()).role().equals(Role.STAFF)
                        || !Objects.equals(
                                r.getRoomId(),
                                rooms.findCurrentStayingRoomId(r.getRequesterId()).orElse(-1L)));
    }

    private void changeStatusAndRecordHistory(MaintenanceRequest r, String next, String reason) {
        String previous = r.getStatus();
        r.setStatus(next);
        requests.saveAndFlush(r);
        history.record(
                EntityType.MAINTENANCE, r.getId(), previous, next, current.get().id(), reason);
        // Notification chỉ xử lý sau commit; sự kiện vẫn mang trạng thái mới và người được giao.
        events.publishEvent(
                new TicketStatusChangedEvent(
                        r.getId(), r.getRequesterId(), r.getAssignedTo(), next));
    }

    private Response toMaintenanceResponse(MaintenanceRequest r, boolean detail) {
        return toMaintenanceResponse(r, detail, detail ? storedDeductions(r) : null);
    }

    private Response toMaintenanceResponse(
            MaintenanceRequest r, boolean detail, List<Deduction> deductions) {
        return new Response(
                r.getId(),
                r.getCode(),
                r.getRequesterId(),
                r.getRoomId(),
                r.getBuildingId(),
                r.getFloor(),
                r.getCategory(),
                r.getSeverity(),
                r.getDescription(),
                r.getStatus(),
                r.getAssignedTo(),
                r.getCreatedAt(),
                detail
                        ? photoRows
                                .findAll((p, q, b) -> b.equal(p.get("requestId"), r.getId()))
                                .stream()
                                .map(
                                        p ->
                                                new Photo(
                                                        p.getId(),
                                                        "/api/maintenance-requests/"
                                                                + r.getId()
                                                                + "/photos/"
                                                                + p.getId()))
                                .toList()
                        : null,
                r.getCause(),
                r.getRepairCost(),
                r.getCostNote(),
                r.getCause() != null,
                deductions,
                detail
                        ? (current.get().is(Role.STAFF)
                                        && !Objects.equals(current.get().unitId(), requestUnitId(r))
                                ? List.of()
                                : MaintenanceRequestPolicy.allowedActions(
                                                r, current.get(), staffCanAcceptRequest(r))
                                        .stream()
                                        .filter(
                                                action ->
                                                        guard.hasPermission(
                                                                action.equals(BusinessAction.CANCEL)
                                                                        ? Permission
                                                                                .MAINTENANCE_DELETE
                                                                        : Permission
                                                                                .MAINTENANCE_UPDATE))
                                        .toList())
                        : null,
                detail ? history.list(EntityType.MAINTENANCE, r.getId()) : null);
    }

    /** Tổng hợp ticket của phòng hoặc khu vực chung trong đơn vị được yêu cầu. */
    @Override
    public Map<String, Long> countByStatus(Long managementUnitId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.query(
                """
                SELECT m.status, COUNT(*) AS cnt FROM maintenance_request m
                LEFT JOIN room r ON r.id=m.room_id
                LEFT JOIN building bd ON bd.id=COALESCE(r.building_id,m.building_id)
                WHERE (CAST(? AS bigint) IS NULL OR bd.management_unit_id=?) GROUP BY m.status
                ORDER BY m.status
                """,
                rs -> {
                    counts.put(rs.getString("status"), rs.getLong("cnt"));
                },
                managementUnitId,
                managementUnitId);
        return counts;
    }
}

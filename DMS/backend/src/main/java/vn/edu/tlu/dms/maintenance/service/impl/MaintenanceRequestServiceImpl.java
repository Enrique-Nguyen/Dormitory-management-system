package vn.edu.tlu.dms.maintenance.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Create;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Photo;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Response;
import vn.edu.tlu.dms.maintenance.domain.MaintenanceRequest;
import vn.edu.tlu.dms.maintenance.domain.MaintenanceRequestPolicy;
import vn.edu.tlu.dms.maintenance.repository.MaintenancePhotoRepository;
import vn.edu.tlu.dms.maintenance.repository.MaintenanceRequestRepository;
import vn.edu.tlu.dms.maintenance.service.MaintenancePhotoService;
import vn.edu.tlu.dms.maintenance.service.MaintenanceRequestService;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class MaintenanceRequestServiceImpl implements MaintenanceRequestService {

    private final MaintenanceRequestRepository requests;

    private final MaintenancePhotoRepository photoRows;

    private final MaintenancePhotoService photos;

    private final RoomReferenceService rooms;

    private final AccountReferenceService accounts;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final StatusHistoryService history;

    private final JdbcTemplate jdbc;

    public MaintenanceRequestServiceImpl(
            MaintenanceRequestRepository requests,
            MaintenancePhotoRepository photoRows,
            MaintenancePhotoService photos,
            RoomReferenceService rooms,
            AccountReferenceService accounts,
            CurrentUser current,
            ScopeGuard guard,
            StatusHistoryService history,
            JdbcTemplate jdbc) {
        this.requests = requests;
        this.photoRows = photoRows;
        this.photos = photos;
        this.rooms = rooms;
        this.accounts = accounts;
        this.current = current;
        this.guard = guard;
        this.history = history;
        this.jdbc = jdbc;
    }

    @Override()
    public PageResponse<Response> searchMaintenanceRequests(
            int page, int size, String status, String category, String severity) {
        return searchMaintenanceRequests(page, size, status, category, severity, "");
    }

    @Override()
    public PageResponse<Response> searchMaintenanceRequests(
            int page, int size, String status, String category, String severity, String q) {
        guard.requirePermission(Permission.MAINTENANCE_READ);
        guard.requireRole(Role.STUDENT, Role.STAFF, Role.TECHNICIAN);
        var actor = current.get();
        PageQuery.of(page, size, null, Set.of("id"), "id,desc");
        Long roomId =
                actor.is(Role.STUDENT)
                        ? rooms.findCurrentStayingRoomId(actor.id()).orElse(-1L)
                        : -1L;
        return PageResponse.from(
                requests.searchVisibleRequests(
                                actor.role(),
                                actor.id(),
                                actor.unitId(),
                                actor.crossUnitAccess(),
                                roomId,
                                status,
                                category,
                                severity,
                                "%" + q.trim() + "%",
                                PageRequest.of(page, size))
                        .map(r -> toMaintenanceResponse(r, false)));
    }

    @Override()
    public Response getMaintenanceRequestById(Long id) {
        guard.requirePermission(Permission.MAINTENANCE_READ);
        return toMaintenanceResponse(loadAccessibleRequest(id, false), true);
    }

    /** Ticket, ảnh và timeline là một thao tác ghi. Lỗi ảnh hoặc lỗi CSDL làm rollback toàn bộ. */
    @Transactional
    @Override()
    public Response createMaintenanceRequest(Create input, List<MultipartFile> files) {
        guard.requirePermission(Permission.MAINTENANCE_CREATE);
        guard.requireRole(Role.STUDENT, Role.STAFF);
        var actor = current.get();
        var request = new MaintenanceRequest();
        if (actor.is(Role.STUDENT)) {
            if (input.roomId() != null || input.buildingId() != null || input.floor() != null) {
                throw new BusinessException("TICKET_LOCATION_INVALID", 400);
            }
            request.setRoomId(
                    rooms.findCurrentStayingRoomId(actor.id())
                            .orElseThrow(() -> BusinessException.conflict("NOT_STAYING")));
        } else {
            boolean roomLocation =
                    input.roomId() != null && input.buildingId() == null && input.floor() == null;
            boolean commonLocation =
                    input.roomId() == null && input.buildingId() != null && input.floor() != null;
            if (!roomLocation && !commonLocation) {
                throw new BusinessException("TICKET_LOCATION_INVALID", 400);
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
                "MAINTENANCE", request.getId(), null, MaintenanceStatus.OPEN, actor.id(), null);
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
            throw BusinessException.conflict("TECHNICIAN_OUT_OF_UNIT");
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
            throw BusinessException.invalid("reason", "Cần lý do hoặc ghi chú tối thiểu 5 ký tự");
        }
        if (action.equals(BusinessAction.ACCEPT)
                && (actor.is(Role.STAFF) || actor.isAdmin())
                && !actor.id().equals(request.getRequesterId())
                && (reason == null || reason.trim().length() < 5)) {
            throw BusinessException.invalid("reason", "Nghiệm thu thay sinh viên cần lý do");
        }
        String next =
                switch (action) {
                    case BusinessAction.REJECT -> MaintenanceStatus.REJECTED;
                    case BusinessAction.CANCEL -> MaintenanceStatus.CANCELLED;
                    case BusinessAction.START, BusinessAction.REOPEN, BusinessAction.ADD_NOTE ->
                            MaintenanceStatus.IN_PROGRESS;
                    case BusinessAction.RESOLVE -> MaintenanceStatus.RESOLVED;
                    case BusinessAction.ACCEPT -> MaintenanceStatus.CLOSED;
                    default -> throw BusinessException.invalid("action", "Hành động không hỗ trợ");
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
        history.record("MAINTENANCE", r.getId(), previous, next, current.get().id(), reason);
    }

    private Response toMaintenanceResponse(MaintenanceRequest r, boolean detail) {
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
                detail ? history.list("MAINTENANCE", r.getId()) : null);
    }
}

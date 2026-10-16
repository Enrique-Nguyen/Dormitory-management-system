package vn.edu.tlu.dms.notification.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationScope;
import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationSource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.notification.api.NotificationDtos;
import vn.edu.tlu.dms.notification.domain.Notification;
import vn.edu.tlu.dms.notification.repository.NotificationRepository;
import vn.edu.tlu.dms.notification.service.NotificationCrudService;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** CRUD bài thủ công: STAFF tự đăng, ADMIN mọi bài. Không sửa/xóa thông báo hệ thống. */
@Service
@Transactional(readOnly = true)
public class NotificationCrudServiceImpl implements NotificationCrudService {

    private final JdbcTemplate jdbc;

    private final SqlPage queries;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final AuditService audit;

    private final NotificationRepository repository;

    private final RoomReferenceService rooms;

    public NotificationCrudServiceImpl(
            JdbcTemplate jdbc,
            SqlPage queries,
            CurrentUser current,
            ScopeGuard guard,
            AuditService audit,
            NotificationRepository repository,
            RoomReferenceService rooms) {
        this.jdbc = jdbc;
        this.queries = queries;
        this.current = current;
        this.guard = guard;
        this.audit = audit;
        this.repository = repository;
        this.rooms = rooms;
    }

    @Override
    public PageResponse<Map<String, Object>> searchByPage(SearchRequest request) {
        guard.requirePermission(Permission.NOTIFICATION_READ);
        guard.requireRole(Role.STAFF);
        var args = new ArrayList<Object>();
        String owner = current.get().isAdmin() ? "TRUE" : "n.created_by=?";
        if (!current.get().isAdmin()) args.add(current.get().id());
        args.add(LikePattern.contains(request.q().trim()));
        args.add(LikePattern.contains(request.q().trim()));
        return queries.search(
                "SELECT n.id,n.scope,n.title,n.body,n.created_at,n.created_by,n.management_unit_id"
                        + " AS unit_id,n.building_id,n.floor,n.room_id FROM notification n WHERE "
                        + owner
                        + " AND n.source_type IS NULL AND (n.title ILIKE ? OR n.body ILIKE ?)",
                args,
                request,
                Set.of("id", "title", "scope", "createdAt", "createdBy"),
                "createdAt,desc");
    }

    @Override
    @Transactional
    public Object saveOrUpdate(NotificationDtos.Create input) {
        guard.requirePermission(
                input.id() == null
                        ? Permission.NOTIFICATION_CREATE
                        : Permission.NOTIFICATION_UPDATE);
        if (input.id() == null) return createManualNotification(input);
        if (input.id() <= 0)
            throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE);
        return updatePublished(input.id(), input);
    }

    @Override()
    public Map<String, Object> getPublished(Long id) {
        guard.requirePermission(Permission.NOTIFICATION_READ);
        guard.requireRole(Role.STAFF);
        var args = new ArrayList<Object>();
        args.add(id);
        String owner = current.get().isAdmin() ? "TRUE" : "created_by=?";
        if (!current.get().isAdmin()) {
            args.add(current.get().id());
        }
        return queries.one(
                "SELECT id,scope,title,body,created_at,management_unit_id AS"
                        + " unit_id,building_id,floor,room_id FROM notification WHERE id=? AND "
                        + owner
                        + " AND source_type IS NULL",
                args.toArray());
    }

    /** Khóa trước khi đọc lại bản ghi; chỉ sửa nội dung, không đổi tập người nhận. */
    private Object updatePublished(Long id, NotificationDtos.Create input) {
        queries.one("SELECT id FROM notification WHERE id=? FOR UPDATE", id);
        var before = getPublished(id);
        requireSame("scope", input.scope(), before.get("scope"));
        requireSame("unitId", input.unitId(), before.get("unitId"));
        requireSame("buildingId", input.buildingId(), before.get("buildingId"));
        requireSame("roomId", input.roomId(), before.get("roomId"));
        requireSame("floor", input.floor(), before.get("floor"));
        if (input.title() == null && input.body() == null) {
            throw BusinessException.invalid(ApiField.FORM, ValidationMessage.NO_CONTENT_TO_UPDATE);
        }
        String title =
                input.title() == null
                        ? (String) before.get("title")
                        : requireText(input.title(), "title");
        String body =
                input.body() == null
                        ? (String) before.get("body")
                        : requireText(input.body(), "body");
        jdbc.update("UPDATE notification SET title=?,body=? WHERE id=?", title, body, id);
        var after = getPublished(id);
        audit.record(
                current.get().id(),
                AuditAction.NOTIFICATION_UPDATE,
                EntityType.NOTIFICATION,
                id,
                before,
                after,
                null);
        return after;
    }

    private static String requireText(String text, String field) {
        if (text == null || text.isBlank())
            throw BusinessException.invalid(field, ValidationMessage.REQUIRED);
        return text.trim();
    }

    private static void requireSame(String field, Object supplied, Object stored) {
        boolean equal =
                supplied instanceof Number left && stored instanceof Number right
                        ? left.longValue() == right.longValue()
                        : Objects.equals(supplied, stored);
        if (supplied != null && !equal) {
            throw BusinessException.invalid(field, "Không đổi phạm vi thông báo đã đăng");
        }
    }

    private NotificationDtos.Response createManualNotification(NotificationDtos.Create input) {
        guard.requirePermission(Permission.NOTIFICATION_CREATE);
        guard.requireRole(Role.STAFF);
        requireText(input.scope(), "scope");
        requireText(input.title(), "title");
        requireText(input.body(), "body");
        validateNotificationScope(input);
        Notification notification = new Notification();
        notification.setScope(input.scope());
        notification.setManagementUnitId(input.unitId());
        notification.setBuildingId(input.buildingId());
        notification.setFloor(input.floor());
        notification.setRoomId(input.roomId());
        notification.setTitle(input.title().trim());
        notification.setBody(input.body().trim());
        notification.setCreatedBy(current.get().id());
        notification.setCreatedAt(OffsetDateTime.now());
        repository.saveAndFlush(notification);
        audit.record(
                current.get().id(),
                AuditAction.NOTIFICATION_CREATE,
                EntityType.NOTIFICATION,
                notification.getId(),
                null,
                input,
                null);
        return new NotificationDtos.Response(
                notification.getId(),
                notification.getScope(),
                notification.getTitle(),
                notification.getBody(),
                NotificationSource.MANUAL,
                null,
                notification.getCreatedAt(),
                false);
    }

    private void validateNotificationScope(NotificationDtos.Create input) {
        int fields =
                (input.unitId() == null ? 0 : 1)
                        + (input.buildingId() == null ? 0 : 1)
                        + (input.floor() == null ? 0 : 1)
                        + (input.roomId() == null ? 0 : 1);
        boolean valid =
                switch (input.scope()) {
                    case NotificationScope.ALL -> fields == 0;
                    case NotificationScope.UNIT -> fields == 1 && input.unitId() != null;
                    case NotificationScope.BUILDING -> fields == 1 && input.buildingId() != null;
                    case NotificationScope.FLOOR ->
                            fields == 2 && input.buildingId() != null && input.floor() != null;
                    case NotificationScope.ROOM -> fields == 1 && input.roomId() != null;
                    default -> false;
                };
        if (!valid) {
            throw BusinessException.invalid(
                    "scope", "Các trường vị trí không khớp phạm vi thông báo");
        }
        switch (input.scope()) {
            case NotificationScope.ALL -> {
                if (!current.get().canReadAllUnits()) {
                    throw BusinessException.forbidden();
                }
            }
            case NotificationScope.UNIT -> {
                if (!Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                "SELECT EXISTS(SELECT 1 FROM management_unit WHERE id=?)",
                                Boolean.class,
                                input.unitId()))) {
                    throw BusinessException.notFound();
                }
                guard.requireUnit(input.unitId());
            }
            case NotificationScope.BUILDING, NotificationScope.FLOOR ->
                    guard.requireUnit(rooms.getBuildingManagementUnitId(input.buildingId()));
            case NotificationScope.ROOM ->
                    guard.requireUnit(rooms.getRoomLocation(input.roomId()).unitId());
            default -> throw BusinessException.invalid("scope", "Phạm vi không hỗ trợ");
        }
    }

    @Transactional
    @Override()
    public void deletePublished(Long id) {
        guard.requirePermission(Permission.NOTIFICATION_DELETE);
        queries.one("SELECT id FROM notification WHERE id=? FOR UPDATE", id);
        var before = getPublished(id);
        jdbc.update("DELETE FROM notification_read WHERE notification_id=?", id);
        jdbc.update("DELETE FROM notification WHERE id=?", id);
        audit.record(
                current.get().id(),
                AuditAction.NOTIFICATION_DELETE,
                EntityType.NOTIFICATION,
                id,
                before,
                null,
                null);
    }
}

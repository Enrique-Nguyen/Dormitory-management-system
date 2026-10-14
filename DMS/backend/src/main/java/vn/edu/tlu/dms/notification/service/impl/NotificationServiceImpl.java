package vn.edu.tlu.dms.notification.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationScope;
import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationSource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.notification.api.NotificationDtos;
import vn.edu.tlu.dms.notification.domain.Notification;
import vn.edu.tlu.dms.notification.repository.NotificationRepository;
import vn.edu.tlu.dms.notification.service.NotificationService;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;

    private final JdbcTemplate jdbc;

    private final CurrentUser currentUser;

    private final ScopeGuard scopeGuard;

    private final RoomReferenceService rooms;

    public NotificationServiceImpl(
            NotificationRepository repository,
            JdbcTemplate jdbc,
            CurrentUser currentUser,
            ScopeGuard scopeGuard,
            RoomReferenceService rooms) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.currentUser = currentUser;
        this.scopeGuard = scopeGuard;
        this.rooms = rooms;
    }

    /** View áp dụng cả quyền đơn vị lẫn khoảng thời gian sinh viên thực sự ở trong phòng. */
    @Override()
    public PageResponse<NotificationDtos.Response> getMyNotifications(
            int page, int size, Boolean unread) {
        return getMyNotifications(page, size, unread, "");
    }

    @Override()
    public PageResponse<NotificationDtos.Response> getMyNotifications(
            int page, int size, Boolean unread, String keyword) {
        scopeGuard.requirePermission(Permission.NOTIFICATION_READ);
        PageQuery.of(page, size, null, Set.of("id"), "id,desc");
        Long userId = currentUser.get().id();
        String filter = unread == null ? "" : unread ? " AND NOT is_read" : " AND is_read";
        filter += " AND (title ILIKE ? OR body ILIKE ?)";
        String pattern = "%" + keyword.trim() + "%";
        Long total =
                jdbc.queryForObject(
                        "SELECT count(*) FROM " + inboxSource() + filter,
                        Long.class,
                        userId,
                        pattern,
                        pattern);
        List<NotificationDtos.Response> notifications =
                jdbc.query(
                        "SELECT * FROM "
                                + inboxSource()
                                + filter
                                + " ORDER BY created_at DESC, notification_id DESC LIMIT ? OFFSET"
                                + " ?",
                        (row, index) ->
                                new NotificationDtos.Response(
                                        row.getLong("notification_id"),
                                        row.getString("scope"),
                                        row.getString("title"),
                                        row.getString("body"),
                                        row.getString("source_type") == null
                                                ? NotificationSource.MANUAL
                                                : row.getString("source_type"),
                                        notificationLink(
                                                row.getString("source_type"),
                                                row.getObject("source_id", Long.class)),
                                        row.getObject("created_at", OffsetDateTime.class),
                                        row.getBoolean("is_read")),
                        userId,
                        pattern,
                        pattern,
                        size,
                        (long) page * size);
        return new PageResponse<>(
                notifications, page, size, total, (int) ((total + size - 1) / size));
    }

    @Override()
    public NotificationDtos.UnreadCount getUnreadNotificationCount() {
        scopeGuard.requirePermission(Permission.NOTIFICATION_READ);
        Long count =
                jdbc.queryForObject(
                        "SELECT count(*) FROM " + inboxSource() + " AND NOT is_read",
                        Long.class,
                        currentUser.get().id());
        return new NotificationDtos.UnreadCount(count);
    }

    /**
     * Kiểm tra quyền qua inbox trước khi ghi; ON CONFLICT giúp đánh dấu đọc nhiều lần không lỗi.
     */
    @Transactional
    @Override()
    public void markNotificationAsRead(Long notificationId) {
        scopeGuard.requirePermission(Permission.NOTIFICATION_READ);
        Long userId = currentUser.get().id();
        Boolean visible =
                currentUser.get().isAdmin()
                        ? repository.existsById(notificationId)
                        : jdbc.queryForObject(
                                "SELECT EXISTS (SELECT 1 FROM v_user_notification WHERE user_id = ?"
                                        + " AND notification_id = ?)",
                                Boolean.class,
                                userId,
                                notificationId);
        if (!Boolean.TRUE.equals(visible)) {
            throw BusinessException.notFound();
        }
        jdbc.update(
                """
                INSERT INTO notification_read (notification_id, user_id)
                VALUES (?, ?) ON CONFLICT DO NOTHING
                """,
                notificationId,
                userId);
    }

    @Transactional
    @Override()
    public void markAllNotificationsAsRead() {
        scopeGuard.requirePermission(Permission.NOTIFICATION_READ);
        Long userId = currentUser.get().id();
        if (currentUser.get().isAdmin()) {
            // Đánh dấu cho chính Admin; không sửa trạng thái đã đọc của người nhận khác.
            jdbc.update(
                    "INSERT INTO notification_read(notification_id,user_id)"
                            + " SELECT id,? FROM notification ON CONFLICT DO NOTHING",
                    userId);
            return;
        }
        jdbc.update(
                """
                INSERT INTO notification_read (notification_id, user_id)
                SELECT notification_id, user_id FROM v_user_notification
                WHERE user_id = ? AND NOT is_read ON CONFLICT DO NOTHING
                """,
                userId);
    }

    @Transactional
    @Override()
    public NotificationDtos.Response createManualNotification(NotificationDtos.Create input) {
        scopeGuard.requirePermission(Permission.NOTIFICATION_CREATE);
        scopeGuard.requireRole(Role.STAFF);
        validateNotificationScope(input);
        Notification notification = new Notification();
        notification.setScope(input.scope());
        notification.setManagementUnitId(input.unitId());
        notification.setBuildingId(input.buildingId());
        notification.setFloor(input.floor());
        notification.setRoomId(input.roomId());
        notification.setTitle(input.title().trim());
        notification.setBody(input.body().trim());
        notification.setCreatedBy(currentUser.get().id());
        notification.setCreatedAt(OffsetDateTime.now());
        repository.saveAndFlush(notification);
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
                if (!currentUser.get().canReadAllUnits()) {
                    throw new BusinessException("FORBIDDEN", 403);
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
                scopeGuard.requireUnit(input.unitId());
            }
            case NotificationScope.BUILDING, NotificationScope.FLOOR ->
                    scopeGuard.requireUnit(rooms.getBuildingManagementUnitId(input.buildingId()));
            case NotificationScope.ROOM ->
                    scopeGuard.requireUnit(rooms.getRoomLocation(input.roomId()).unitId());
            default -> throw BusinessException.invalid("scope", "Phạm vi không hỗ trợ");
        }
    }

    /**
     * Thông báo tự động dùng transaction mới sau commit nghiệp vụ; lỗi gửi thông báo không làm mất
     * đợt đăng ký đã lưu. Listener chịu trách nhiệm bắt và ghi log lỗi.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override()
    public void notifyActiveStudentsOfSummerPeriod(
            Long periodId, String periodName, List<Long> unitIds, Long actorId) {
        for (Long unitId : unitIds) {
            List<Long> studentIds =
                    jdbc.query(
                            """
                            SELECT DISTINCT c.student_id FROM dormitory_contract c
                            JOIN room_assignment_history a ON a.contract_id = c.id
                            JOIN bed b ON b.id = a.bed_id JOIN room r ON r.id = b.room_id
                            JOIN building bd ON bd.id = r.building_id
                            WHERE c.status = 'ACTIVE' AND bd.management_unit_id = ?
                            """,
                            (row, index) -> row.getLong(1),
                            unitId);
            for (Long studentId : studentIds) {
                Notification notification = new Notification();
                notification.setScope(NotificationScope.USER);
                notification.setUserId(studentId);
                notification.setTitle("Mở đợt đăng ký hè");
                notification.setBody(periodName);
                notification.setSourceType(NotificationSource.SUMMER_PERIOD_OPENED);
                notification.setSourceId(periodId);
                notification.setCreatedBy(actorId);
                notification.setCreatedAt(OffsetDateTime.now());
                repository.save(notification);
            }
        }
        repository.flush();
    }

    private String notificationLink(String source, Long id) {
        if (source == null || id == null) {
            return null;
        }
        String portal =
                currentUser.get().is(Role.STUDENT)
                        ? "/student"
                        : currentUser.get().isAdmin() ? "/admin" : "/staff";
        return switch (source) {
            case NotificationSource.SUMMER_PERIOD_OPENED -> portal + "/summer";
            case NotificationSource.APPLICATION_APPROVED, NotificationSource.BED_HELD ->
                    portal + "/applications/" + id;
            case NotificationSource.INVOICE_ISSUED -> portal + "/invoices";
            case NotificationSource.TICKET_STATUS_CHANGED -> portal + "/maintenance/" + id;
            default -> null;
        };
    }

    /**
     * Admin đọc toàn bộ thông báo nhưng isRead vẫn là trạng thái của chính Admin. Các role khác
     * tiếp tục dùng view lọc đơn vị, chủ sở hữu và khoảng thời gian ở. Mỗi nguồn đều nhận đúng một
     * tham số userId để giữ phân trang/lọc nhất quán.
     */
    private String inboxSource() {
        if (!currentUser.get().isAdmin()) {
            return "v_user_notification WHERE user_id = ?";
        }
        return "(SELECT n.id AS notification_id,n.scope,n.title,n.body,n.source_type,n.source_id,"
                + " n.created_at,(nr.notification_id IS NOT NULL) AS is_read FROM notification n"
                + " LEFT JOIN notification_read nr ON nr.notification_id=n.id AND nr.user_id=?)"
                + " admin_inbox WHERE TRUE";
    }
}

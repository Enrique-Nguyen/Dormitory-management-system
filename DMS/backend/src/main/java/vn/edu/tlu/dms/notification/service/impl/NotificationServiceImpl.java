package vn.edu.tlu.dms.notification.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
        PageQuery.of(page, size, null, Set.of("id"), "id,desc");
        Long userId = currentUser.get().id();
        String filter = unread == null ? "" : unread ? " AND NOT is_read" : " AND is_read";
        filter += " AND (title ILIKE ? OR body ILIKE ?)";
        String pattern = "%" + keyword.trim() + "%";
        Long total =
                jdbc.queryForObject(
                        "SELECT count(*) FROM v_user_notification WHERE user_id = ?" + filter,
                        Long.class,
                        userId,
                        pattern,
                        pattern);
        List<NotificationDtos.Response> notifications =
                jdbc.query(
                        "SELECT * FROM v_user_notification WHERE user_id = ?"
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
                                                ? "MANUAL"
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
        Long count =
                jdbc.queryForObject(
                        "SELECT count(*) FROM v_user_notification WHERE user_id = ? AND NOT"
                                + " is_read",
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
        Long userId = currentUser.get().id();
        Boolean visible =
                jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM v_user_notification WHERE user_id = ? AND"
                                + " notification_id = ?)",
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
        Long userId = currentUser.get().id();
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
        scopeGuard.requireRole("STAFF");
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
                "MANUAL",
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
                    case "ALL" -> fields == 0;
                    case "UNIT" -> fields == 1 && input.unitId() != null;
                    case "BUILDING" -> fields == 1 && input.buildingId() != null;
                    case "FLOOR" ->
                            fields == 2 && input.buildingId() != null && input.floor() != null;
                    case "ROOM" -> fields == 1 && input.roomId() != null;
                    default -> false;
                };
        if (!valid) {
            throw BusinessException.invalid(
                    "scope", "Các trường vị trí không khớp phạm vi thông báo");
        }
        switch (input.scope()) {
            case "ALL" -> {
                if (!currentUser.get().crossUnitAccess()) {
                    throw new BusinessException("FORBIDDEN", 403);
                }
            }
            case "UNIT" -> scopeGuard.requireUnit(input.unitId());
            case "BUILDING", "FLOOR" ->
                    scopeGuard.requireUnit(rooms.getBuildingManagementUnitId(input.buildingId()));
            case "ROOM" -> scopeGuard.requireUnit(rooms.getRoomLocation(input.roomId()).unitId());
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
        if (unitIds == null || unitIds.isEmpty()) {
            return;
        }
        // Chỉ xét chỗ ở hiện hành (ended_at IS NULL) và gộp các đơn vị trong một truy vấn để mỗi
        // sinh viên chỉ nhận một thông báo.
        List<Long> studentIds =
                jdbc.query(
                        """
                        SELECT DISTINCT c.student_id FROM dormitory_contract c
                        JOIN room_assignment_history a ON a.contract_id = c.id
                        JOIN bed b ON b.id = a.bed_id JOIN room r ON r.id = b.room_id
                        JOIN building bd ON bd.id = r.building_id
                        WHERE c.status = 'ACTIVE' AND a.ended_at IS NULL
                          AND bd.management_unit_id = ANY (?)
                        """,
                        (row, index) -> row.getLong(1),
                        (Object) unitIds.toArray(new Long[0]));
        for (Long studentId : studentIds) {
            Notification notification = new Notification();
            notification.setScope("USER");
            notification.setUserId(studentId);
            notification.setTitle("Mở đợt đăng ký hè");
            notification.setBody(periodName);
            notification.setSourceType("SUMMER_PERIOD_OPENED");
            notification.setSourceId(periodId);
            notification.setCreatedBy(actorId);
            notification.setCreatedAt(OffsetDateTime.now());
            repository.save(notification);
        }
        repository.flush();
    }

    private String notificationLink(String source, Long id) {
        if (source == null || id == null) {
            return null;
        }
        String portal = currentUser.get().is("STUDENT") ? "/student" : "/staff";
        return switch (source) {
            case "SUMMER_PERIOD_OPENED" -> portal + "/summer";
            case "APPLICATION_APPROVED", "BED_HELD" -> portal + "/applications/" + id;
            case "INVOICE_ISSUED" -> portal + "/invoices";
            case "TICKET_STATUS_CHANGED" -> portal + "/maintenance/" + id;
            default -> null;
        };
    }
}

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
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.PageQuery;
import vn.edu.tlu.dms.notification.api.NotificationDtos;
import vn.edu.tlu.dms.notification.domain.Notification;
import vn.edu.tlu.dms.notification.repository.NotificationRepository;
import vn.edu.tlu.dms.notification.service.NotificationService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;

    private final JdbcTemplate jdbc;

    private final CurrentUser currentUser;

    private final ScopeGuard scopeGuard;

    public NotificationServiceImpl(
            NotificationRepository repository,
            JdbcTemplate jdbc,
            CurrentUser currentUser,
            ScopeGuard scopeGuard) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.currentUser = currentUser;
        this.scopeGuard = scopeGuard;
    }

    /** Phạm vi hộp thư và trạng thái đọc được lọc trước phân trang; chỉ sắp xếp cột đã cho phép. */
    @Override
    public PageResponse<NotificationDtos.Response> searchByPage(SearchRequest request) {
        scopeGuard.requirePermission(Permission.NOTIFICATION_READ);
        int page = request.page();
        int size = request.size();
        Boolean unread = request.unread();
        String keyword = request.q();
        var pageable =
                PageQuery.of(
                        page,
                        size,
                        request.sort(),
                        Set.of("id", "createdAt", "title", "scope", "isRead"),
                        "createdAt,desc");
        String ordering =
                pageable.getSort().stream()
                        .map(
                                order -> {
                                    String column =
                                            switch (order.getProperty()) {
                                                case "id" -> "notification_id";
                                                case "createdAt" -> "created_at";
                                                case "isRead" -> "is_read";
                                                default -> order.getProperty();
                                            };
                                    return column + " " + order.getDirection().name();
                                })
                        .collect(Collectors.joining(", "));
        Long userId = currentUser.get().id();
        var actor = currentUser.get();
        // KTV dùng cổng /technician; nếu gán nhầm /staff thì liên kết thông báo không mở được.
        String portal =
                actor.is(Role.STUDENT)
                        ? "/student"
                        : actor.is(Role.TECHNICIAN)
                                ? "/technician"
                                : actor.isAdmin() ? "/admin" : "/staff";
        String filter = unread == null ? "" : unread ? " AND NOT is_read" : " AND is_read";
        filter += " AND (title ILIKE ? OR body ILIKE ?)";
        String pattern = LikePattern.contains(keyword.trim());
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
                                + " ORDER BY "
                                + ordering
                                + " LIMIT ? OFFSET ?",
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
                                                portal,
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
        createUserNotifications(
                studentIds,
                "Mở đợt đăng ký hè",
                periodName,
                NotificationSource.SUMMER_PERIOD_OPENED,
                periodId,
                actorId);
    }

    /**
     * Tạo thông báo USER trong transaction mới để listener gọi sau commit; lỗi ở đây chỉ được
     * listener ghi log và không làm rollback nghiệp vụ đã lưu.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override()
    public void notifyUsers(
            List<Long> userIds, String title, String body, String source, Long sourceId) {
        createUserNotifications(userIds, title, body, source, sourceId, null);
    }

    /**
     * Lọc người nhận null/trùng và bỏ qua nội dung thiếu. Khi có sourceId, kiểm tra bản ghi cùng
     * người nhận, nguồn, mã nguồn và tiêu đề trước khi thêm để sự kiện phát lặp không tạo trùng.
     * Ticket đổi trạng thái khác nhau dùng tiêu đề khác nhau nên vẫn nhận đủ thông báo.
     */
    private void createUserNotifications(
            List<Long> userIds,
            String title,
            String body,
            String source,
            Long sourceId,
            Long createdBy) {
        if (userIds == null || userIds.isEmpty() || title == null || title.isBlank()) {
            return;
        }
        if (body == null || body.isBlank() || source == null || source.isBlank()) {
            return;
        }
        List<Long> recipients = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (recipients.isEmpty()) {
            return;
        }
        String normalizedTitle = title.trim();
        String normalizedBody = body.trim();
        for (Long userId : recipients) {
            if (sourceId != null && notificationExists(userId, source, sourceId, normalizedTitle)) {
                continue;
            }
            Notification notification = new Notification();
            notification.setScope(NotificationScope.USER);
            notification.setUserId(userId);
            notification.setTitle(normalizedTitle);
            notification.setBody(normalizedBody);
            notification.setSourceType(source);
            notification.setSourceId(sourceId);
            notification.setCreatedBy(createdBy);
            notification.setCreatedAt(OffsetDateTime.now());
            repository.save(notification);
        }
        repository.flush();
    }

    private boolean notificationExists(Long userId, String source, Long sourceId, String title) {
        Boolean exists =
                jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM notification WHERE user_id=?"
                                + " AND source_type=? AND source_id=? AND title=?)",
                        Boolean.class,
                        userId,
                        source,
                        sourceId,
                        title);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Dựng liên kết mở đúng màn nghiệp vụ từ mã nguồn, mã bản ghi và cổng của người đọc. Liên kết
     * không lưu trong CSDL; thông báo không gắn bản ghi hoặc nguồn thủ công trả null để giao diện
     * không hiện nút mở. Package-private để kiểm thử trực tiếp quy tắc dựng liên kết.
     *
     * @param portal tiền tố cổng theo vai trò người đọc, ví dụ /student hoặc /technician
     * @param source mã nguồn thông báo trong NotificationSource
     * @param id mã bản ghi nghiệp vụ; có thể null
     * @return đường dẫn frontend hoặc null nếu không xác định được màn đích
     */
    String notificationLink(String portal, String source, Long id) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case NotificationSource.SUMMER_PERIOD_OPENED -> portal + "/summer";
            case NotificationSource.APPLICATION_APPROVED, NotificationSource.BED_HELD ->
                    id == null ? null : portal + "/applications/" + id;
            case NotificationSource.INVOICE_ISSUED -> portal + "/invoices";
            case NotificationSource.TICKET_STATUS_CHANGED ->
                    id == null
                            ? null
                            : portal.equals("/technician")
                                    ? portal + "/tasks/" + id
                                    : portal + "/maintenance/" + id;
            case NotificationSource.DEPOSIT_DEDUCTED -> portal + "/deposit";
            case NotificationSource.CONTRACT_EXPIRING,
                    NotificationSource.CONTRACT_MOVED_OUT ->
                    id == null ? portal + "/contracts" : portal + "/contracts/" + id;
            case NotificationSource.STORAGE_RECORD_CHANGED -> portal + "/contracts";
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

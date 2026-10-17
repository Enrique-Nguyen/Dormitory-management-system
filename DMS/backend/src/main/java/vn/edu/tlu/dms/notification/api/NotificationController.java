package vn.edu.tlu.dms.notification.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.notification.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.NOTIFICATION_READ)
    @PostMapping("/search-by-page")
    public PageResponse<NotificationDtos.Response> searchByPage(
            @Valid @RequestBody SearchRequest request) {
        return service.searchByPage(request);
    }

    @RequirePrivilege(Permission.NOTIFICATION_READ)
    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCount getUnreadNotificationCount() {
        return service.getUnreadNotificationCount();
    }

    // Đánh dấu đã đọc là thao tác của người nhận, không cần quyền sửa thông báo phát hành.
    @RequirePrivilege(Permission.NOTIFICATION_READ)
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable Long id) {
        service.markNotificationAsRead(id);
        return ResponseEntity.noContent().build();
    }

    // Chỉ đánh dấu các thông báo của tài khoản hiện tại; không sửa nội dung thông báo.
    @RequirePrivilege(Permission.NOTIFICATION_READ)
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead() {
        service.markAllNotificationsAsRead();
        return ResponseEntity.noContent().build();
    }
}

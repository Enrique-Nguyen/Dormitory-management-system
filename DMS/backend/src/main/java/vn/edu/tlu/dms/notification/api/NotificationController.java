package vn.edu.tlu.dms.notification.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.notification.service.NotificationService;

import java.net.URI;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<NotificationDtos.Response> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unread,
            @RequestParam(defaultValue = "") String q) {
        return service.getMyNotifications(page, size, unread, q);
    }

    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCount getUnreadNotificationCount() {
        return service.getUnreadNotificationCount();
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable Long id) {
        service.markNotificationAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead() {
        service.markAllNotificationsAsRead();
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<NotificationDtos.Response> createManualNotification(
            @Valid @RequestBody NotificationDtos.Create input) {
        NotificationDtos.Response response = service.createManualNotification(input);
        return ResponseEntity.created(URI.create("/api/notifications/" + response.id()))
                .body(response);
    }
}

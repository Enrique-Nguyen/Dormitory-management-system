package vn.edu.tlu.dms.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "notification_read")
@IdClass(NotificationReadId.class)
public class NotificationRead {

    @Id
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_at", nullable = false)
    private OffsetDateTime readAt;

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long value) {
        this.notificationId = value;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long value) {
        this.userId = value;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(OffsetDateTime value) {
        this.readAt = value;
    }
}

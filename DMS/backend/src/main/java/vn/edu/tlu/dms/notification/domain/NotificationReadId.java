package vn.edu.tlu.dms.notification.domain;

import java.io.Serializable;
import java.util.Objects;

public class NotificationReadId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long notificationId;

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long value) {
        this.notificationId = value;
    }

    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long value) {
        this.userId = value;
    }

    public NotificationReadId() {}

    public NotificationReadId(Long notificationId, Long userId) {
        this.notificationId = notificationId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof NotificationReadId other
                && Objects.equals(notificationId, other.notificationId)
                && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId, userId);
    }
}

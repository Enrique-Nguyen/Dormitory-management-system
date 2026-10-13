package vn.edu.tlu.dms.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "status_history")
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String value) {
        this.entityType = value;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long value) {
        this.entityId = value;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String value) {
        this.fromStatus = value;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String value) {
        this.toStatus = value;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long value) {
        this.actorId = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        this.reason = value;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime value) {
        this.createdAt = value;
    }
}

package vn.edu.tlu.dms.notification.domain;

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
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "scope", nullable = false, length = 10)
    private String scope;

    @Column(name = "management_unit_id")
    private Long managementUnitId;

    @Column(name = "building_id")
    private Long buildingId;

    @Column(name = "floor")
    private Short floor;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String value) {
        this.scope = value;
    }

    public Long getManagementUnitId() {
        return managementUnitId;
    }

    public void setManagementUnitId(Long value) {
        this.managementUnitId = value;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long value) {
        this.buildingId = value;
    }

    public Short getFloor() {
        return floor;
    }

    public void setFloor(Short value) {
        this.floor = value;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long value) {
        this.roomId = value;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long value) {
        this.userId = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String value) {
        this.body = value;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String value) {
        this.sourceType = value;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long value) {
        this.sourceId = value;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long value) {
        this.createdBy = value;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime value) {
        this.createdAt = value;
    }
}

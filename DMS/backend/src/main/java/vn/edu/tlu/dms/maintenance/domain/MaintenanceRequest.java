package vn.edu.tlu.dms.maintenance.domain;

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
@Table(name = "maintenance_request")
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "building_id")
    private Long buildingId;

    @Column(name = "floor")
    private Short floor;

    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "severity", nullable = false, length = 10)
    private String severity;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "cause", length = 10)
    private String cause;

    @Column(name = "repair_cost")
    private Long repairCost;

    @Column(name = "cost_note", length = 500)
    private String costNote;

    @Column(name = "cost_decided_by")
    private Long costDecidedBy;

    @Column(name = "cost_decided_at")
    private OffsetDateTime costDecidedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        this.code = value;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long value) {
        this.requesterId = value;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long value) {
        this.roomId = value;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String value) {
        this.category = value;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String value) {
        this.severity = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long value) {
        this.assignedTo = value;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime value) {
        this.resolvedAt = value;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(OffsetDateTime value) {
        this.closedAt = value;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String value) {
        this.cause = value;
    }

    public Long getRepairCost() {
        return repairCost;
    }

    public void setRepairCost(Long value) {
        this.repairCost = value;
    }

    public String getCostNote() {
        return costNote;
    }

    public void setCostNote(String value) {
        this.costNote = value;
    }

    public Long getCostDecidedBy() {
        return costDecidedBy;
    }

    public void setCostDecidedBy(Long value) {
        this.costDecidedBy = value;
    }

    public OffsetDateTime getCostDecidedAt() {
        return costDecidedAt;
    }

    public void setCostDecidedAt(OffsetDateTime value) {
        this.costDecidedAt = value;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime value) {
        this.createdAt = value;
    }
}

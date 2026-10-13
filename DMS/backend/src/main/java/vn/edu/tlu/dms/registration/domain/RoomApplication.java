package vn.edu.tlu.dms.registration.domain;

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
@Table(name = "room_application")
public class RoomApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Column(name = "preferred_unit_id", nullable = false)
    private Long preferredUnitId;

    @Column(name = "preferred_room_type_id")
    private Long preferredRoomTypeId;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "priority_score", nullable = false)
    private Integer priorityScore;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long value) {
        this.studentId = value;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long value) {
        this.periodId = value;
    }

    public Long getPreferredUnitId() {
        return preferredUnitId;
    }

    public void setPreferredUnitId(Long value) {
        this.preferredUnitId = value;
    }

    public Long getPreferredRoomTypeId() {
        return preferredRoomTypeId;
    }

    public void setPreferredRoomTypeId(Long value) {
        this.preferredRoomTypeId = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String value) {
        this.note = value;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer value) {
        this.priorityScore = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String value) {
        this.statusReason = value;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime value) {
        this.submittedAt = value;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime value) {
        this.createdAt = value;
    }
}

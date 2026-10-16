package vn.edu.tlu.dms.contract.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "dormitory_contract")
public class DormitoryContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "activation_deadline", nullable = false)
    private OffsetDateTime activationDeadline;

    @Column(name = "deposit_required")
    private Long depositRequired;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "activated_by")
    private Long activatedBy;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

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

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long value) {
        this.studentId = value;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long value) {
        this.applicationId = value;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate value) {
        this.startDate = value;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate value) {
        this.endDate = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    public OffsetDateTime getActivationDeadline() {
        return activationDeadline;
    }

    public void setActivationDeadline(OffsetDateTime value) {
        this.activationDeadline = value;
    }

    public Long getDepositRequired() {
        return depositRequired;
    }

    public void setDepositRequired(Long value) {
        this.depositRequired = value;
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(OffsetDateTime value) {
        this.activatedAt = value;
    }

    public Long getActivatedBy() {
        return activatedBy;
    }

    public void setActivatedBy(Long value) {
        this.activatedBy = value;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(OffsetDateTime value) {
        this.closedAt = value;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String value) {
        this.statusReason = value;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime value) {
        this.createdAt = value;
    }
}

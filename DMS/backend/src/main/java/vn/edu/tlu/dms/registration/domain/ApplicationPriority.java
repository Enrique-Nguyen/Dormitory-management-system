package vn.edu.tlu.dms.registration.domain;

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
@Table(name = "application_priority")
@IdClass(ApplicationPriorityId.class)
public class ApplicationPriority {

    @Id
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Id
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "verified")
    private Boolean verified;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long value) {
        this.applicationId = value;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long value) {
        this.categoryId = value;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean value) {
        this.verified = value;
    }

    public Long getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Long value) {
        this.verifiedBy = value;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime value) {
        this.verifiedAt = value;
    }
}

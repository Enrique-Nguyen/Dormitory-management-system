package vn.edu.tlu.dms.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "registration_quota")
@IdClass(RegistrationQuotaId.class)
public class RegistrationQuota {

    @Id
    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Id
    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "quota", nullable = false)
    private Integer quota;

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long value) {
        this.periodId = value;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long value) {
        this.buildingId = value;
    }

    public Integer getQuota() {
        return quota;
    }

    public void setQuota(Integer value) {
        this.quota = value;
    }
}

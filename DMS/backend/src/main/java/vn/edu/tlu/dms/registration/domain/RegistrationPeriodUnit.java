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
@Table(name = "registration_period_unit")
@IdClass(RegistrationPeriodUnitId.class)
public class RegistrationPeriodUnit {

    @Id
    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Id
    @Column(name = "management_unit_id", nullable = false)
    private Long managementUnitId;

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long value) {
        this.periodId = value;
    }

    public Long getManagementUnitId() {
        return managementUnitId;
    }

    public void setManagementUnitId(Long value) {
        this.managementUnitId = value;
    }
}

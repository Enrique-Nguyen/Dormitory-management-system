package vn.edu.tlu.dms.registration.domain;

import java.io.Serializable;
import java.util.Objects;

public class RegistrationPeriodUnitId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long periodId;

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long value) {
        this.periodId = value;
    }

    private Long managementUnitId;

    public Long getManagementUnitId() {
        return managementUnitId;
    }

    public void setManagementUnitId(Long value) {
        this.managementUnitId = value;
    }

    public RegistrationPeriodUnitId() {}

    public RegistrationPeriodUnitId(Long periodId, Long managementUnitId) {
        this.periodId = periodId;
        this.managementUnitId = managementUnitId;
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof RegistrationPeriodUnitId other
                && Objects.equals(periodId, other.periodId)
                && Objects.equals(managementUnitId, other.managementUnitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periodId, managementUnitId);
    }
}

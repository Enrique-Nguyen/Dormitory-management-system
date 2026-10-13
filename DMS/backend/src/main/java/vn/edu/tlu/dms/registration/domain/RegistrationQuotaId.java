package vn.edu.tlu.dms.registration.domain;

import java.io.Serializable;
import java.util.Objects;

public class RegistrationQuotaId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long periodId;

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long value) {
        this.periodId = value;
    }

    private Long buildingId;

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long value) {
        this.buildingId = value;
    }

    public RegistrationQuotaId() {}

    public RegistrationQuotaId(Long periodId, Long buildingId) {
        this.periodId = periodId;
        this.buildingId = buildingId;
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof RegistrationQuotaId other
                && Objects.equals(periodId, other.periodId)
                && Objects.equals(buildingId, other.buildingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periodId, buildingId);
    }
}

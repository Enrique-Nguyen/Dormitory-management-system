package vn.edu.tlu.dms.registration.domain;

import java.io.Serializable;
import java.util.Objects;

public class ApplicationPriorityId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long applicationId;

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long value) {
        this.applicationId = value;
    }

    private Long categoryId;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long value) {
        this.categoryId = value;
    }

    public ApplicationPriorityId() {}

    public ApplicationPriorityId(Long applicationId, Long categoryId) {
        this.applicationId = applicationId;
        this.categoryId = categoryId;
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof ApplicationPriorityId other
                && Objects.equals(applicationId, other.applicationId)
                && Objects.equals(categoryId, other.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, categoryId);
    }
}

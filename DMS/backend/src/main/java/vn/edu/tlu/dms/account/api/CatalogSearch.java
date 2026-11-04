package vn.edu.tlu.dms.account.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;

import java.util.List;

public record CatalogSearch(
        @Min(value = 0, message = ValidationMessage.PAGE_RANGE) Integer page,
        @Min(value = 1, message = ValidationMessage.PAGE_RANGE)
                @Max(value = 100, message = ValidationMessage.PAGE_RANGE) Integer size,
        @Size(max = 200) String q,
        @Size(max = 4) List<@Size(max = 100) String> sort,
        Boolean active,
        @Positive(message = ValidationMessage.ID_POSITIVE) Long parentId,
        @Positive(message = ValidationMessage.ID_POSITIVE) Long schoolId,
        @Size(max = 30) String code,
        @Size(max = 200) String name) {
    public CatalogSearch(Integer page, Integer size, String q, List<String> sort, Boolean active,
            Long parentId, Long schoolId) {
        this(page, size, q, sort, active, parentId, schoolId, null, null);
    }
}

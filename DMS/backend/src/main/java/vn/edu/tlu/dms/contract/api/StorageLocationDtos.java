package vn.edu.tlu.dms.contract.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;

import java.util.List;

/** Hợp đồng danh mục vị trí lưu đồ theo đơn vị quản lý. */
public final class StorageLocationDtos {
    private StorageLocationDtos() {}

    public record Search(
            @Min(value = 0, message = ValidationMessage.PAGE_RANGE) Integer page,
            @Min(value = 1, message = ValidationMessage.PAGE_RANGE)
                    @Max(value = 100, message = ValidationMessage.PAGE_RANGE) Integer size,
            @Size(max = 200) String q,
            @Size(max = 4) List<@Size(max = 100) String> sort,
            @Positive(message = ValidationMessage.ID_POSITIVE) Long managementUnitId,
            Boolean active,
            @Size(max = 30) String code,
            @Size(max = 100) String name) {}

    public record SaveRequest(
            @Positive(message = ValidationMessage.ID_POSITIVE) Long id,
            @Positive(message = ValidationMessage.ID_POSITIVE) Long managementUnitId,
            @Size(max = 30) String code,
            @Size(max = 100) String name,
            Boolean active) {}

    public record Location(
            Long id,
            Long managementUnitId,
            String code,
            String name,
            boolean active,
            List<String> allowedActions) {}
}

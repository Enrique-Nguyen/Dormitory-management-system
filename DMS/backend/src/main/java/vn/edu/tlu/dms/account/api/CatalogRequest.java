package vn.edu.tlu.dms.account.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;

public record CatalogRequest(
        @Positive(message = ValidationMessage.ID_POSITIVE) Long id,
        @Size(max = 30) String code,
        @Size(max = 500) String name,
        Boolean active,
        @Positive(message = ValidationMessage.ID_POSITIVE) Long parentId,
        @Positive(message = ValidationMessage.ID_POSITIVE) Long schoolId) {}

package vn.edu.tlu.dms.billing.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import vn.edu.tlu.dms.common.constants.DmsConstants.MeterType;

import java.time.LocalDate;

public final class UtilityRateDtos {

    private UtilityRateDtos() {}

    public record Create(
            @NotNull @Pattern(regexp = MeterType.PATTERN) String meterType,
            @NotNull @Positive Long unitPrice,
            @NotNull LocalDate effectiveFrom) {}

    public record Response(Long id, String meterType, Long unitPrice, LocalDate effectiveFrom) {}
}

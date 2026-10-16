package vn.edu.tlu.dms.billing.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import vn.edu.tlu.dms.common.constants.DmsConstants.MeterType;

import java.time.LocalDate;

public final class UtilityRateDtos {

    private UtilityRateDtos() {}

    public record Save(
            @Positive Long id,
            @Pattern(regexp = MeterType.PATTERN) String meterType,
            @Positive Long unitPrice,
            LocalDate effectiveFrom) {

        public Save(String meterType, Long unitPrice, LocalDate effectiveFrom) {
            this(null, meterType, unitPrice, effectiveFrom);
        }
    }

    public record Response(Long id, String meterType, Long unitPrice, LocalDate effectiveFrom) {}
}

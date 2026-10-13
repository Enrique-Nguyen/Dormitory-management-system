package vn.edu.tlu.dms.registration.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public final class RegistrationPeriodDtos {

    private RegistrationPeriodDtos() {}

    public record Quota(
            @NotNull @Positive Long buildingId, @NotNull @PositiveOrZero Integer quota) {}

    public record Input(
            @Positive Long semesterId,
            @Pattern(regexp = "REGULAR|SUMMER") String kind,
            @Size(max = 100) String name,
            OffsetDateTime openAt,
            OffsetDateTime closeAt,
            List<@Positive Long> unitIds,
            List<@Valid Quota> quotas) {}

    public record Response(
            Long id,
            Long semesterId,
            String kind,
            String name,
            OffsetDateTime openAt,
            OffsetDateTime closeAt,
            List<Long> unitIds,
            List<Quota> quotas) {}
}

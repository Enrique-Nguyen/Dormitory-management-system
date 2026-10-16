package vn.edu.tlu.dms.registration.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.RegistrationKind;

import java.time.OffsetDateTime;
import java.util.List;

public final class RegistrationPeriodDtos {

    private RegistrationPeriodDtos() {}

    public record Quota(
            @NotNull @Positive Long buildingId, @NotNull @PositiveOrZero Integer quota) {}

    /** Body lưu phẳng: ID trống tạo mới, ID dương sửa bản ghi đã có. */
    public record Input(
            @Positive Long id,
            @Positive Long semesterId,
            @Pattern(regexp = RegistrationKind.PATTERN) String kind,
            @Size(max = 100) String name,
            OffsetDateTime openAt,
            OffsetDateTime closeAt,
            List<@NotNull @Positive Long> unitIds,
            List<@NotNull @Valid Quota> quotas) {

        /** Giữ tương thích các lời gọi tạo mới trong mã Java hiện có. */
        public Input(
                Long semesterId,
                String kind,
                String name,
                OffsetDateTime openAt,
                OffsetDateTime closeAt,
                List<Long> unitIds,
                List<Quota> quotas) {
            this(null, semesterId, kind, name, openAt, closeAt, unitIds, quotas);
        }
    }

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

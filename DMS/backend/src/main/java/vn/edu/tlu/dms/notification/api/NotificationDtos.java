package vn.edu.tlu.dms.notification.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationScope;

import java.time.OffsetDateTime;

public final class NotificationDtos {

    private NotificationDtos() {}

    public record Create(
            @NotBlank @Pattern(regexp = NotificationScope.MANUAL_PATTERN) String scope,
            @Positive Long unitId,
            @Positive Long buildingId,
            @Min(0) Short floor,
            @Positive Long roomId,
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 5000) String body) {}

    public record Response(
            Long id,
            String scope,
            String title,
            String body,
            String source,
            String link,
            OffsetDateTime createdAt,
            boolean isRead) {}

    public record UnreadCount(long count) {}
}

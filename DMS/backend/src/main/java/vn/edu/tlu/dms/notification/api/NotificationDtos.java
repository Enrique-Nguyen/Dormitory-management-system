package vn.edu.tlu.dms.notification.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationScope;

import java.time.OffsetDateTime;

public final class NotificationDtos {

    private NotificationDtos() {}

    public record Create(
            @Positive Long id,
            @Pattern(regexp = NotificationScope.MANUAL_PATTERN) String scope,
            @Positive Long unitId,
            @Positive Long buildingId,
            @Min(0) Short floor,
            @Positive Long roomId,
            @Size(max = 150) String title,
            @Size(max = 5000) String body) {

        public Create(
                String scope,
                Long unitId,
                Long buildingId,
                Short floor,
                Long roomId,
                String title,
                String body) {
            this(null, scope, unitId, buildingId, floor, roomId, title, body);
        }
    }

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

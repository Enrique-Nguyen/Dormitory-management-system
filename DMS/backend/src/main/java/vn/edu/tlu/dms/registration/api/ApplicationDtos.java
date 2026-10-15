package vn.edu.tlu.dms.registration.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.service.StatusHistoryService;

import java.time.OffsetDateTime;
import java.util.List;

public final class ApplicationDtos {

    private ApplicationDtos() {}

    public record Input(
            @Positive Long periodId,
            @Positive Long preferredUnitId,
            @Positive Long preferredRoomTypeId,
            List<@Positive Long> priorityCategoryIds,
            @Size(max = 500) String note,
            @Positive Long studentId) {}

    public record Priority(Long categoryId, String name, Integer weight, Boolean verified) {}

    public record StudentRef(Long id, String studentCode, String fullName, String gender) {}

    public record Response(
            Long id,
            String status,
            StudentRef student,
            Long periodId,
            Long preferredUnitId,
            Long preferredRoomTypeId,
            Integer priorityScore,
            String note,
            String rejectReason,
            OffsetDateTime submittedAt,
            List<Priority> priorities,
            List<String> allowedActions,
            List<StatusHistoryService.Entry> timeline) {}
}

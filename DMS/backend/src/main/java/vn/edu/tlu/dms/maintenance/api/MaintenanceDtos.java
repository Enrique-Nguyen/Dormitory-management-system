package vn.edu.tlu.dms.maintenance.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.service.StatusHistoryService;

import java.time.OffsetDateTime;
import java.util.List;

public final class MaintenanceDtos {

    private MaintenanceDtos() {}

    public record Create(
            @NotBlank @Pattern(regexp = "ELECTRIC|WATER|FURNITURE|DOOR_LOCK|OTHER") String category,
            @NotBlank @Pattern(regexp = "LOW|MEDIUM|HIGH") String severity,
            @NotBlank @Size(max = 1000) String description,
            @Positive Long roomId,
            @Positive Long buildingId,
            @Min(0) Short floor) {}

    public record Assign(@NotNull @Positive Long technicianId) {}

    public record Note(@NotBlank @Size(min = 5, max = 500) String note) {

        public Note {
            if (note != null) {
                note = note.trim();
            }
        }
    }

    public record Photo(Long id, String url) {}

    public record Response(
            Long id,
            String code,
            Long requesterId,
            Long roomId,
            Long buildingId,
            Short floor,
            String category,
            String severity,
            String description,
            String status,
            Long assignedTo,
            OffsetDateTime createdAt,
            List<Photo> photos,
            List<String> allowedActions,
            List<StatusHistoryService.Entry> timeline) {}
}

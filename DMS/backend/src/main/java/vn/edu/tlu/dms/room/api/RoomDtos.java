package vn.edu.tlu.dms.room.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class RoomDtos {

    private RoomDtos() {}

    public record Input(
            @Positive Long buildingId,
            @Min(0) Short floor,
            @Size(max = 20) String code,
            @Positive Long roomTypeId,
            @Pattern(regexp = "M|F") String gender) {}

    public record Status(
            @NotBlank @Pattern(regexp = "ACTIVE|MAINTENANCE|CLOSED") String status,
            @NotBlank @Size(min = 5, max = 500) String reason) {

        public Status {
            if (reason != null) {
                reason = reason.trim();
            }
        }
    }

    public record BedInput(@NotBlank @Size(max = 10) String code) {}

    public record BedStatus(
            @NotBlank @Pattern(regexp = "ACTIVE|BROKEN|RETIRED") String status,
            @NotBlank @Size(min = 5, max = 500) String reason) {

        public BedStatus {
            if (reason != null) {
                reason = reason.trim();
            }
        }
    }

    public record Leader(@NotNull @Positive Long studentId) {}

    public record Occupant(Long studentId, String fullName, String studentCode, String kind) {}

    public record BedResponse(Long id, String code, String status, Occupant occupant) {}

    public record Response(
            Long id,
            String code,
            Short floor,
            String gender,
            String status,
            Long buildingId,
            Long roomTypeId,
            Long leaderStudentId,
            long activeBeds,
            long heldBeds,
            long occupiedBeds,
            long freeBeds,
            List<BedResponse> beds) {}
}

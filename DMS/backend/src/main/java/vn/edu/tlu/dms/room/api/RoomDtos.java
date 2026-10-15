package vn.edu.tlu.dms.room.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.BedOperationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.Gender;
import vn.edu.tlu.dms.common.constants.DmsConstants.RoomStatus;

import java.util.List;

public final class RoomDtos {

    private RoomDtos() {}

    public record Input(
            @Positive Long id,
            @Positive Long buildingId,
            @Min(0) Short floor,
            @Size(max = 20) String code,
            @Positive Long roomTypeId,
            @Pattern(regexp = Gender.PATTERN) String gender) {

        public Input(Long buildingId, Short floor, String code, Long roomTypeId, String gender) {
            this(null, buildingId, floor, code, roomTypeId, gender);
        }
    }

    public record Status(
            @NotBlank @Pattern(regexp = RoomStatus.PATTERN) String status,
            @NotBlank @Size(min = 5, max = 500) String reason) {

        public Status {
            if (reason != null) {
                reason = reason.trim();
            }
        }
    }

    public record BedInput(@Positive Long id, @Positive Long roomId, @Size(max = 10) String code) {

        public BedInput(String code) {
            this(null, null, code);
        }

        public BedInput(Long roomId, String code) {
            this(null, roomId, code);
        }
    }

    public record BedStatus(
            @NotBlank @Pattern(regexp = BedOperationStatus.PATTERN) String status,
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

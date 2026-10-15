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

import java.time.OffsetDateTime;
import java.util.List;

public final class RoomDtos {

    private RoomDtos() {}

    public record Input(
            @Positive Long buildingId,
            @Min(0) Short floor,
            @Size(max = 20) String code,
            @Positive Long roomTypeId,
            @Pattern(regexp = Gender.PATTERN) String gender) {}

    public record Status(
            @NotBlank @Pattern(regexp = RoomStatus.PATTERN) String status,
            @NotBlank @Size(min = 5, max = 500) String reason) {

        public Status {
            if (reason != null) {
                reason = reason.trim();
            }
        }
    }

    public record BedInput(@NotBlank @Size(max = 10) String code) {}

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
            boolean missingLeader,
            StudentRef leader,
            List<BedResponse> beds,
            List<LeaderChange> leaderHistory) {}

    /**
     * Sinh viên tham chiếu trong thông tin phòng; studentCode là tên đăng nhập (mã sinh viên).
     */
    public record StudentRef(Long id, String fullName, String studentCode) {}

    /**
     * Một lần đổi trưởng phòng đọc từ audit_log (FR-ROOM-09); actor null là hệ thống tự gỡ khi
     * phân phòng của trưởng phòng đóng (FR-ROOM-10).
     */
    public record LeaderChange(
            StudentRef from,
            StudentRef to,
            Long actorId,
            String actorName,
            String reason,
            OffsetDateTime at) {}

    /** Phòng còn giường trống cho split view chọn giường (API 4.4); số liệu tính từ view. */
    public record AvailableRoom(
            RoomRef room,
            boolean matchesPreferredType,
            List<FreeBed> freeBeds,
            long activeBeds,
            long occupiedCount,
            long heldCount) {}

    public record RoomRef(
            Long id,
            String code,
            Long buildingId,
            String buildingCode,
            Short floor,
            RoomTypeRef roomType,
            String gender,
            Long priceMain) {}

    public record RoomTypeRef(Long id, String name) {}

    public record FreeBed(Long id, String code) {}
}

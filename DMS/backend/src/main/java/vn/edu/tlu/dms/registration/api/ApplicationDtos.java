package vn.edu.tlu.dms.registration.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.service.StatusHistoryService;

import java.time.OffsetDateTime;
import java.util.List;

public final class ApplicationDtos {

    private ApplicationDtos() {}

    /** Body lưu phẳng: ID trống tạo mới, ID dương sửa bản ghi đã có. */
    public record Input(
            @Positive Long id,
            @Positive Long periodId,
            @Positive Long preferredUnitId,
            @Positive Long preferredRoomTypeId,
            List<@NotNull @Positive Long> priorityCategoryIds,
            @Size(max = 500) String note,
            @Positive Long studentId) {

        /** Giữ tương thích các lời gọi tạo mới trong mã Java hiện có. */
        public Input(
                Long periodId,
                Long preferredUnitId,
                Long preferredRoomTypeId,
                List<Long> priorityCategoryIds,
                String note,
                Long studentId) {
            this(
                    null,
                    periodId,
                    preferredUnitId,
                    preferredRoomTypeId,
                    priorityCategoryIds,
                    note,
                    studentId);
        }
    }

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

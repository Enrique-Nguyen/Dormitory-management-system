package vn.edu.tlu.dms.registration.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
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

    /** Chọn giường cụ thể để phân cho đơn đã APPROVED; bắt buộc và phải là ID dương. */
    public record AssignRequest(
            @NotNull(message = ValidationMessage.BED_REQUIRED_FOR_ASSIGN)
                    @Positive(message = ValidationMessage.ID_POSITIVE)
                    Long bedId) {}

    /**
     * Xác nhận một diện ưu tiên của đơn. {@code verified} bắt buộc để phân biệt "chưa xác nhận";
     * ghi chú tùy chọn được lưu cùng nhật ký thao tác.
     */
    public record PriorityVerifyRequest(
            @NotNull(message = ValidationMessage.PRIORITY_VERIFIED_REQUIRED) Boolean verified,
            @Size(max = 500) String note) {}

    /** Diện ưu tiên kèm kết quả xác nhận của Staff để giao diện hiển thị và tính lại điểm. */
    public record Priority(
            Long categoryId,
            String name,
            Integer weight,
            Boolean verified,
            Long verifiedBy,
            String verifiedByName,
            OffsetDateTime verifiedAt,
            String note) {}

    public record StudentRef(Long id, String studentCode, String fullName, String gender) {}

    /**
     * Giường đang giữ theo dạng tham chiếu lồng ở API Spec mục 2.8; {@code code} là mã giường đầy
     * đủ do module room ghép (K1-A101-03). Số tầng thuộc module room và chưa có trong hợp đồng truy
     * vấn liên module nên tham chiếu này không trả {@code floor}.
     */
    public record BedRef(
            Long id,
            String code,
            Long roomId,
            String roomCode,
            Long buildingId,
            String buildingCode,
            Long unitId) {}

    /**
     * Giường đang giữ và hợp đồng PENDING gắn với đơn ASSIGNED. Hạn kích hoạt do module contract
     * chụp khi tạo hợp đồng; khi đọc lại chi tiết đơn, giá trị này chỉ có ở phản hồi phân giường.
     * Các trường phẳng {@code bedId}, {@code bedCode}, {@code roomId}... được giữ nguyên để màn
     * danh sách và kiểm thử cũ không đổi hợp đồng.
     */
    public record Hold(
            Long assignmentId,
            BedRef bed,
            Long bedId,
            String bedCode,
            Long roomId,
            String roomCode,
            Long buildingId,
            String buildingCode,
            Long unitId,
            Long contractId,
            String contractCode,
            OffsetDateTime activationDeadline) {}

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
            Long contractId,
            Hold hold,
            List<Priority> priorities,
            List<String> allowedActions,
            List<StatusHistoryService.Entry> timeline) {}
}

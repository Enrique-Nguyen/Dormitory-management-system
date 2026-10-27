package vn.edu.tlu.dms.maintenance.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceCategory;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceSeverity;
import vn.edu.tlu.dms.common.service.StatusHistoryService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class MaintenanceDtos {

    private MaintenanceDtos() {}

    /** Không có id thì tạo mới; có id chỉ sửa các trường nội dung được gửi và giữ vị trí/ảnh cũ. */
    public record Save(
            @Positive Long id,
            @Pattern(regexp = MaintenanceCategory.PATTERN) String category,
            @Pattern(regexp = MaintenanceSeverity.PATTERN) String severity,
            @Size(max = 1000) String description,
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

    /** Nội dung kết luận ticket; Service kiểm tra nghiệp vụ theo nguyên nhân WEAR hoặc MISUSE. */
    public record Conclude(
            String cause,
            Long repairCost,
            List<Long> chargedStudentIds,
            @Size(max = 500) String note) {

        public Conclude {
            if (cause != null) {
                cause = cause.trim();
            }
            if (note != null) {
                note = note.trim();
            }
        }
    }

    public record Photo(Long id, String url) {}

    /** Thông tin tối thiểu của sinh viên, không lộ entity của module account. */
    public record StudentRef(Long id, String studentCode, String fullName, String gender) {}

    /** Sinh viên đang ở trong phòng tại thời điểm tạo ticket và trạng thái hợp đồng tương ứng. */
    public record ChargeableStudent(StudentRef student, Long contractId, String contractStatus) {}

    /** Một khoản trừ cọc đã ghi cho sinh viên khi kết luận ticket hư hỏng do sử dụng. */
    public record Deduction(
            Long contractId, StudentRef student, Long amount, LocalDate topUpDueDate) {}

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
            String cause,
            Long repairCost,
            String costNote,
            boolean concluded,
            List<Deduction> deductions,
            List<String> allowedActions,
            List<StatusHistoryService.Entry> timeline) {}
}

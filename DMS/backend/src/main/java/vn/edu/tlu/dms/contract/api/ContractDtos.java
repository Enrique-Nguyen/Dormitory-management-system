package vn.edu.tlu.dms.contract.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.room.service.OccupancyQuery.CurrentBed;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ContractDtos {

    private ContractDtos() {}

    /**
     * Mã nhãn tính toán của hợp đồng (BR-08). Giá trị trùng registry nhãn phía giao diện; không
     * lưu vào CSDL vì có thể suy ra từ trạng thái, ngày kết thúc và lựa chọn hè.
     */
    public static final class Label {
        public static final String EXPIRING = "EXPIRING";
        public static final String OVERDUE_MOVE_OUT = "OVERDUE_MOVE_OUT";
        public static final String SUMMER_LEAVE = "SUMMER_LEAVE";

        private Label() {}
    }

    /** Không có id là tạo; có id là sửa đúng hợp đồng PENDING đã tồn tại. */
    public record Save(
            @Positive Long id,
            @Positive Long applicationId,
            LocalDate startDate,
            LocalDate endDate) {}

    /** Payload chung cho yêu cầu gia hạn/chuyển phòng; không nhận trạng thái hoặc chủ sở hữu. */
    public record StaySave(
            @Positive Long id,
            @Positive Long contractId,
            LocalDate newEndDate,
            @Size(max = 500) String reason) {}

    /** Xác nhận kích hoạt hợp đồng PENDING: giấy tờ, đúng mức cọc và hình thức thu. */
    public record ActivateRequest(
            @NotNull Boolean paperContractSigned,
            @NotNull @Positive Long depositAmount,
            @NotBlank String paymentMethod,
            @Size(max = 500) String note) {}

    /** Thân yêu cầu từ chối gia hạn: lý do bắt buộc. */
    public record ExtensionRejectRequest(String reason) {}

    public record Response(
            Long id,
            String code,
            Long studentId,
            Long applicationId,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            OffsetDateTime activationDeadline,
            Long depositRequired,
            OffsetDateTime activatedAt,
            String statusReason,
            List<String> allowedActions,
            List<StatusHistoryService.Entry> timeline,
            String studentCode,
            String studentName,
            List<String> labels,
            Long daysRemaining,
            Long depositBalance,
            CurrentBed currentAssignment,
            Long pendingExtensionRequestId,
            Long pendingTransferRequestId,
            Long pendingSummerChoiceId,
            StudentInfo student,
            DepositInfo deposit,
            ActorInfo activatedBy,
            List<AssignmentInfo> assignments,
            PendingRequestsInfo pendingRequests,
            Long openStorageRecordId,
            MoveOutInfo moveOut) {

        /** Bản danh sách/không chi tiết: giữ 22 trường phẳng, các trường bổ sung để trống. */
        public Response(
                Long id,
                String code,
                Long studentId,
                Long applicationId,
                LocalDate startDate,
                LocalDate endDate,
                String status,
                OffsetDateTime activationDeadline,
                Long depositRequired,
                OffsetDateTime activatedAt,
                String statusReason,
                List<String> allowedActions,
                List<StatusHistoryService.Entry> timeline,
                String studentCode,
                String studentName,
                List<String> labels,
                Long daysRemaining,
                Long depositBalance,
                CurrentBed currentAssignment,
                Long pendingExtensionRequestId,
                Long pendingTransferRequestId,
                Long pendingSummerChoiceId) {
            this(
                    id,
                    code,
                    studentId,
                    applicationId,
                    startDate,
                    endDate,
                    status,
                    activationDeadline,
                    depositRequired,
                    activatedAt,
                    statusReason,
                    allowedActions,
                    timeline,
                    studentCode,
                    studentName,
                    labels,
                    daysRemaining,
                    depositBalance,
                    currentAssignment,
                    pendingExtensionRequestId,
                    pendingTransferRequestId,
                    pendingSummerChoiceId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
    }

    /** Sinh viên của hợp đồng trong chi tiết (API 4.5); gender đọc từ student_profile. */
    public record StudentInfo(Long id, String studentCode, String fullName, String gender) {}

    /** Sổ cọc của hợp đồng lấy từ v_deposit_balance (API 4.5, BR-10). */
    public record DepositInfo(
            Long required, Long balance, Long topUpRequired, boolean topUpOverdue) {}

    /** Người thực hiện một hành động (kích hoạt hợp đồng) hiển thị trong chi tiết. */
    public record ActorInfo(Long id, String fullName, String username) {}

    /** Giường của một dòng lịch sử phân phòng; `code` là mã đầy đủ tòa-phòng-giường. */
    public record BedInfo(
            Long id,
            String code,
            Long roomId,
            String roomCode,
            Long buildingId,
            String buildingCode,
            Integer floor,
            Long unitId) {}

    /** Một dòng lịch sử phân phòng trong chi tiết hợp đồng (API 4.5, SAD: không ghi đè lịch sử). */
    public record AssignmentInfo(
            Long id,
            String kind,
            BedInfo bed,
            OffsetDateTime startedAt,
            OffsetDateTime stayStartedAt,
            OffsetDateTime endedAt,
            String endReason,
            Long priceMain,
            Long priceSummer) {}

    /** Ba yêu cầu còn chờ của hợp đồng; null khi không có yêu cầu nào ở trạng thái PENDING. */
    public record PendingRequestsInfo(
            Long extensionRequestId, Long transferRequestId, Long summerChoiceId) {}

    /** Biên bản trả phòng của hợp đồng; `moveOutDate` là thời điểm ghi nhận trả phòng. */
    public record MoveOutInfo(
            Long id,
            OffsetDateTime moveOutDate,
            String assetCondition,
            OffsetDateTime createdAt) {}

    /** Khoản cọc vừa thu khi kích hoạt; không lộ DTO của module billing ra ngoài contract. */
    public record DepositSummary(
            String type,
            Long amount,
            Long balance,
            String paymentMethod,
            OffsetDateTime collectedAt,
            String note) {}

    /** Kết quả kích hoạt: hợp đồng đã ACTIVE, khoản cọc và phân phòng hiện tại. */
    public record ActivationResult(
            Response contract,
            DepositSummary deposit,
            CurrentBed currentAssignment,
            List<String> allowedActions) {}

    /** Chi tiết yêu cầu chuyển phòng (API 3.4); currentBed rỗng khi không còn phân phòng mở. */
    public record TransferDetail(
            Long id,
            Long contractId,
            String contractCode,
            Long studentId,
            String studentCode,
            String studentName,
            String reason,
            String status,
            Long targetBedId,
            Long decidedBy,
            OffsetDateTime decidedAt,
            String decisionReason,
            OffsetDateTime createdAt,
            CurrentBed currentBed,
            List<String> allowedActions,
            List<StatusHistoryService.Entry> timeline) {}

    /** Thân yêu cầu duyệt chuyển phòng: giường đích bắt buộc. */
    public record TransferApproveRequest(Long targetBedId) {}

    /** Thân yêu cầu từ chối chuyển phòng: lý do bắt buộc. */
    public record TransferRejectRequest(String reason) {}

    /** Chi tiết yêu cầu gia hạn kèm ngày cũ/mới, trạng thái, hành động được phép và timeline. */
    public record ExtensionDetail(
            Long id,
            Long contractId,
            String contractCode,
            Long studentId,
            String studentCode,
            String studentName,
            String source,
            LocalDate oldEndDate,
            LocalDate newEndDate,
            String reason,
            String status,
            Long decidedBy,
            OffsetDateTime decidedAt,
            String decisionReason,
            OffsetDateTime createdAt,
            List<String> allowedActions,
            List<StatusHistoryService.Entry> timeline) {}
}

package vn.edu.tlu.dms.contract.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.room.service.OccupancyQuery.CurrentBed;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ContractDtos {

    private ContractDtos() {}

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
            List<StatusHistoryService.Entry> timeline) {}

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
}

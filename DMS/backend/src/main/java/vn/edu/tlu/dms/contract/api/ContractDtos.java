package vn.edu.tlu.dms.contract.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.service.StatusHistoryService;

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
}

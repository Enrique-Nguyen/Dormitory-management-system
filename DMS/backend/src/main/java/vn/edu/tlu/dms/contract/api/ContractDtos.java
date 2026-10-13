package vn.edu.tlu.dms.contract.api;

import vn.edu.tlu.dms.common.service.StatusHistoryService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ContractDtos {

    private ContractDtos() {}

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

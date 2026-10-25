package vn.edu.tlu.dms.billing.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.PaymentMethod;
import vn.edu.tlu.dms.common.service.StatusHistoryService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class InvoiceDtos {

    private InvoiceDtos() {}

    public record Payment(
            Long payerUserId,
            @Size(max = 100) String payerName,
            @NotNull @Pattern(regexp = PaymentMethod.PATTERN) String paymentMethod) {}

    public record Item(
            Long id,
            String itemType,
            BigDecimal quantity,
            Long unitPrice,
            Long amount,
            String note) {}

    public record Response(
            Long id,
            String code,
            String type,
            Long studentId,
            Long semesterId,
            Long roomId,
            LocalDate billingMonth,
            Long totalAmount,
            LocalDate dueDate,
            String status,
            OffsetDateTime paidAt,
            List<Item> items,
            List<String> allowedActions,
            List<StatusHistoryService.Entry> timeline) {}

    public record Save(
            @Positive Long id,
            String type,
            @Positive Long studentId,
            @Positive Long semesterId,
            @Positive Long roomId,
            LocalDate billingMonth,
            @Min(0) Long totalAmount,
            LocalDate dueDate) {}

    public record BatchIssueResult(int totalCreated, int totalSkipped, long totalAmount) {}

    public record RoomFeeBatchIssueRequest(@NotNull @Positive Long semesterId) {}

    public record UtilityBatchIssueRequest(
            @Positive Long managementUnitId, @NotNull LocalDate billingMonth) {}
}

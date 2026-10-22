package vn.edu.tlu.dms.billing.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** DTO dữ liệu sổ cọc và giao dịch tiền cọc theo thiết kế SAD và SRS. */
public final class DepositDtos {

    private DepositDtos() {}

    /** Chi tiết một giao dịch trong sổ cọc. */
    public record Transaction(
            Long id,
            Long contractId,
            String type,
            Long amount,
            Long maintenanceRequestId,
            LocalDate dueDate,
            Long confirmedBy,
            OffsetDateTime createdAt,
            String note) {}

    /** Số dư và tình trạng tiền cọc của một hợp đồng (đọc từ v_deposit_balance). */
    public record Balance(
            Long contractId,
            Long studentId,
            String status,
            Long depositRequired,
            Long balance,
            Long topupNeeded,
            LocalDate oldestUnpaidDue,
            Boolean topupOverdue,
            Long depositDebt) {}

    /** Yêu cầu thu tiền cọc ban đầu khi kích hoạt hợp đồng (FR-CON-01, BR-10). */
    public record InitialDepositRequest(
            @NotNull @Positive(message = "Số tiền cọc phải lớn hơn 0") Long amount,
            @Size(max = 500) String note) {}

    /** Yêu cầu trừ cọc khi ticket hư hỏng do sử dụng được nghiệm thu đóng (FR-MNT-10). */
    public record DeductRequest(
            @NotNull Long maintenanceRequestId,
            @NotNull @Positive(message = "Số tiền trừ cọc phải lớn hơn 0") Long amount,
            @NotNull LocalDate dueDate,
            @Size(max = 500) String note) {}

    /** Yêu cầu nộp bù cọc khi sinh viên nộp khoản bù (FR-BIL-09). */
    public record TopupRequest(
            @NotNull @Positive(message = "Số tiền bù cọc phải lớn hơn 0") Long amount,
            @Size(max = 500) String note) {}

    /** Yêu cầu hoàn cọc khi làm thủ tục trả phòng (FR-CON-09). */
    public record RefundRequest(
            @NotNull @Positive(message = "Số tiền hoàn cọc phải lớn hơn 0") Long amount,
            @Size(max = 500) String note) {}
}

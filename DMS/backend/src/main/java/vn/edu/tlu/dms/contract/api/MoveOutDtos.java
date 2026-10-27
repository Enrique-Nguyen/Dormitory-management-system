package vn.edu.tlu.dms.contract.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.billing.service.DebtQuery.UnpaidInvoice;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.contract.service.MoveOutWarningLookup.UnsettledTicket;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** DTO xem trước và thực hiện trả phòng (API 4.8). Không trả entity ra ngoài module. */
public final class MoveOutDtos {

    private MoveOutDtos() {}

    /**
     * Dữ liệu xem trước trả phòng: kiểu kết thúc, số dư cọc, tiền hoàn, công nợ và cảnh báo.
     * {@code warnings} bật khi còn hóa đơn chưa đóng hoặc ticket chưa kết luận chi phí; các danh
     * sách chi tiết đi kèm để giao diện hiển thị trước khi Staff xác nhận.
     */
    public record Preview(
            String endStatus,
            Long depositBalance,
            Long refundAmount,
            Long negativeBalanceDebt,
            List<UnpaidInvoice> unpaidRoomFeeInvoices,
            List<UnpaidInvoice> unpaidRoomUtilityInvoices,
            List<UnsettledTicket> unsettledTickets,
            boolean isRoomLeader,
            boolean warnings) {}

    /** Thân thao tác trả phòng; DAMAGED bắt buộc có ghi chú tài sản. */
    public record Request(
            @NotNull(message = ValidationMessage.MOVE_OUT_DATE_REQUIRED) LocalDate moveOutDate,
            String assetCondition,
            @Size(max = 500) String assetNote,
            Boolean acknowledgedWarnings) {}

    /**
     * Kết quả sau khi hợp đồng đã kết thúc: tóm tắt hợp đồng, ảnh chụp biên bản trả phòng, tiền
     * hoàn và cảnh báo đã ghi nhận. {@code contractStatus} và {@code endStatus} là trạng thái cuối
     * của hợp đồng; {@code moveOutRecordId}/{@code movedOutAt} là biên bản đã ghi.
     */
    public record Result(
            Long moveOutRecordId,
            Long contractId,
            String contractCode,
            String contractStatus,
            LocalDate moveOutDate,
            String endStatus,
            Long depositBalance,
            Long refundAmount,
            Long negativeBalanceDebt,
            Long unpaidRoomFee,
            String assetCondition,
            String assetNote,
            Long confirmedBy,
            OffsetDateTime movedOutAt,
            LocalDate endDate,
            boolean warnings) {}
}

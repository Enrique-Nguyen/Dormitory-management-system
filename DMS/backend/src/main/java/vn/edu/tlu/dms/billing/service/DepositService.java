package vn.edu.tlu.dms.billing.service;

import vn.edu.tlu.dms.billing.api.DepositDtos;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Dịch vụ quản lý sổ cọc và các giao dịch cọc (SAD 4.1, SRS 3.6, BR-10).
 * Khóa hợp đồng trước khi ghi nhận thu, trừ vi phạm, nộp bù hoặc hoàn cọc.
 */
public interface DepositService {

    /** Thu cọc ban đầu khi ký hoặc kích hoạt hợp đồng (FR-CON-01, BR-10). */
    DepositDtos.Transaction collectInitial(Long contractId, Long amount, String note);

    /** Khấu trừ cọc khi ticket hư hỏng do sử dụng được đóng (FR-MNT-10, BR-10). */
    DepositDtos.Transaction deduct(
            Long contractId,
            Long maintenanceRequestId,
            Long amount,
            LocalDate dueDate,
            String note);

    /** Nộp bù khoản cọc bị khấu trừ (FR-BIL-09, FR-BIL-13). */
    DepositDtos.Transaction topup(Long contractId, Long amount, String note);

    /** Hoàn cọc khi sinh viên trả phòng (FR-CON-09, BR-10). */
    DepositDtos.Transaction refund(Long contractId, Long amount, String note);

    /** Đọc số dư cọc hiện tại và mức cọc quy định của hợp đồng. */
    DepositDtos.Balance getBalance(Long contractId);

    /**
     * Đọc số dư, mức cọc và khoản cần bù cọc của hợp đồng dưới dạng record của tầng Service để
     * module khác dùng mà không phụ thuộc DTO trong {@code billing.api}.
     */
    BalanceView getBalanceView(Long contractId);

    /** Lịch sử các giao dịch biến động sổ cọc của hợp đồng. */
    List<DepositDtos.Transaction> listTransactions(Long contractId);

    /** Lịch sử giao dịch dưới dạng record của tầng Service cho module khác đọc. */
    List<DepositEntry> listEntries(Long contractId);

    /**
     * Số dư, mức cọc, khoản cần bù và trạng thái hợp đồng tại thời điểm đọc. {@code topupNeeded} và
     * {@code topupOverdue} chỉ khác 0 / true khi hợp đồng ACTIVE vì view v_deposit_balance chỉ có
     * nghĩa với hợp đồng ACTIVE, EXPIRED, TERMINATED (BR-10).
     */
    public record BalanceView(
            Long contractId,
            Long balance,
            Long depositRequired,
            String status,
            Long topupNeeded,
            boolean topupOverdue) {}

    /** Một dòng sổ cọc dùng cho các module nghiệp vụ khác. */
    public record DepositEntry(
            Long id,
            Long contractId,
            String type,
            Long amount,
            Long maintenanceRequestId,
            LocalDate dueDate,
            Long confirmedBy,
            OffsetDateTime createdAt,
            String note) {}

    /** Tra cứu sổ cọc theo phân trang SearchRequest; mine = true lọc theo người đăng nhập. */
    PageResponse<DepositDtos.Balance> searchBalancesByPage(SearchRequest request, boolean mine);
}

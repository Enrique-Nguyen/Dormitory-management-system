package vn.edu.tlu.dms.contract.service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Cổng đọc lựa chọn tạm rời hè đã duyệt do module contract sở hữu, được module registration triển
 * khai để tránh phụ thuộc ngược từ contract lên registration (SAD 4.2).
 */
public interface SummerLeaveLookup {

    /** Trả lựa chọn TEMP_LEAVE đã APPROVED của hợp đồng, nếu có. */
    Optional<SummerLeave> findApprovedTempLeave(Long contractId);

    /** Thông tin lựa chọn hè cần cho điều kiện tạo phiếu gửi đồ. */
    public record SummerLeave(Long summerChoiceId, Long semesterId, LocalDate semesterEnd) {}
}

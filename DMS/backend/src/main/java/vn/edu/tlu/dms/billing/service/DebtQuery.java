package vn.edu.tlu.dms.billing.service;

import java.time.LocalDate;
import java.util.List;

/**
 * Truy vấn chỉ đọc về công nợ của sinh viên và phòng do module billing cung cấp cho module khác
 * (SAD 4.1, 4.2); không khóa và không thay đổi dữ liệu.
 */
public interface DebtQuery {

    /** Tổng hợp nợ tiền phòng UNPAID và khoản thiếu bù cọc của sinh viên (BR-10). */
    StudentDebt getStudentDebt(Long studentId);

    /** Tổng hợp nợ điện nước UNPAID của phòng (BR-03, BR-04). */
    RoomDebt getRoomDebt(Long roomId);

    /** Danh sách hóa đơn phí phòng UNPAID của sinh viên, dùng cho preview trả phòng. */
    List<UnpaidInvoice> findUnpaidRoomFeeInvoices(Long studentId);

    /** Danh sách hóa đơn điện nước UNPAID của phòng, dùng cho preview trả phòng. */
    List<UnpaidInvoice> findUnpaidUtilityInvoices(Long roomId);

    /** Thông tin tối thiểu của một hóa đơn chưa thanh toán. */
    public record UnpaidInvoice(Long id, String code, Long amount, LocalDate dueDate) {}

    /** Dữ liệu công nợ của sinh viên; dùng kiểm tra điều kiện trả phòng và gia hạn. */
    public record StudentDebt(
            Long studentId,
            Long unpaidRoomFee,
            Long depositDebt,
            Long totalDebt,
            boolean hasDebt) {}

    /** Dữ liệu công nợ tiền điện nước của phòng. */
    public record RoomDebt(Long roomId, Long unpaidUtilityFee, boolean hasDebt) {}

    /** Tổng hợp nợ hóa đơn theo học kỳ/phòng và bù cọc trong đơn vị cho Dashboard. */
    FinanceSummary getFinanceSummary(Long managementUnitId);

    /** Chỉ số tiền chưa thanh toán; không suy ra phạm vi từ chỗ ở hiện tại của sinh viên. */
    record FinanceSummary(long unpaidRoomFee, long unpaidUtility, long topupNeeded) {}
}

package vn.edu.tlu.dms.billing.service;

/**
 * Truy vấn chỉ đọc về công nợ của sinh viên và phòng do module billing cung cấp cho module khác
 * (SAD 4.1, 4.2); không khóa và không thay đổi dữ liệu.
 */
public interface DebtQuery {

    /** Tổng hợp nợ tiền phòng UNPAID và khoản thiếu bù cọc của sinh viên (BR-10). */
    StudentDebt getStudentDebt(Long studentId);

    /** Tổng hợp nợ điện nước UNPAID của phòng (BR-03, BR-04). */
    RoomDebt getRoomDebt(Long roomId);

    /** Dữ liệu công nợ của sinh viên; dùng kiểm tra điều kiện trả phòng và gia hạn. */
    public record StudentDebt(
            Long studentId,
            Long unpaidRoomFee,
            Long depositDebt,
            Long totalDebt,
            boolean hasDebt) {}

    /** Dữ liệu công nợ tiền điện nước của phòng. */
    public record RoomDebt(
            Long roomId,
            Long unpaidUtilityFee,
            boolean hasDebt) {}
}

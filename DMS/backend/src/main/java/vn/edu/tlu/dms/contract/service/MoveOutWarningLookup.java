package vn.edu.tlu.dms.contract.service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Cổng đọc ticket chưa kết luận do module contract sở hữu, được module maintenance triển khai để
 * tránh phụ thuộc ngược từ contract lên maintenance (SAD 4.2).
 */
public interface MoveOutWarningLookup {

    /**
     * Tìm ticket của phòng mà sinh viên là người ở tại thời điểm tạo ticket và chưa kết luận chi
     * phí. Trả danh sách rỗng khi không có ticket.
     */
    List<UnsettledTicket> findUnsettledTickets(Long studentId, Long roomId, OffsetDateTime at);

    /** Thông tin tối thiểu của ticket cần cảnh báo khi trả phòng. */
    public record UnsettledTicket(Long id, String code, String status, boolean concluded) {}
}

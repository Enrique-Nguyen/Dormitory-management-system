package vn.edu.tlu.dms.common.service;

import java.time.OffsetDateTime;
import java.util.List;

public interface StatusHistoryService {

    /** Lưu lần chuyển trạng thái, người thực hiện và lý do trong transaction của thao tác gốc. */
    void record(String type, Long id, String from, String to, Long actorId, String reason);

    /**
     * Đọc lịch sử của đối tượng theo loại và ID, sắp xếp theo thời điểm; nơi gọi phải kiểm tra
     * quyền đọc đối tượng trước.
     */
    List<Entry> list(String type, Long id);

    public record Entry(
            String fromStatus, String toStatus, Long actorId, String reason, OffsetDateTime at) {}
}

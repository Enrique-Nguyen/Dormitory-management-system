package vn.edu.tlu.dms.common.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface AuditService {

    /**
     * Admin tìm nhật ký theo từ khóa trong mã thao tác/tên người thực hiện, người thực hiện, mã
     * thao tác, loại đối tượng và khoảng thời gian. Các bộ lọc được kết hợp bằng AND; from/to tính
     * cả hai mốc, có thể gửi riêng từng mốc và phải kèm múi giờ ISO-8601. Kết quả chỉ đọc, phân
     * trang theo ID giảm dần và giữ nguyên dữ liệu nhật ký đã lưu.
     */
    PageResponse<Map<String, Object>> searchByPage(SearchRequest input);

    /** Admin đọc dữ liệu trước/sau của một bản ghi; thiếu ID trả 404, không có API sửa/xóa. */
    Map<String, Object> getAuditById(Long id);

    /**
     * Ghi nhật ký thao tác gồm người thực hiện, dữ liệu trước/sau và lý do; dùng cùng transaction
     * với thao tác cần theo dõi.
     */
    void record(
            Long actorId,
            String action,
            String type,
            Long id,
            Object before,
            Object after,
            String reason);

    /**
     * Đọc nhật ký của một đối tượng theo mã thao tác, sắp xếp theo thời điểm tăng dần; dùng hiển
     * thị lịch sử nghiệp vụ (ví dụ đổi trưởng phòng). Không kiểm tra quyền AUDIT_READ: nơi gọi phải
     * kiểm tra quyền đọc và phạm vi của đối tượng trước.
     */
    List<EntityLogEntry> listEntityLog(String entityType, Long entityId, String action);

    /** Một dòng nhật ký; giá trị trước/sau giữ dạng JSON đã lưu, actorId null nghĩa là SYSTEM. */
    public record EntityLogEntry(
            Long actorId,
            String actorName,
            String oldValue,
            String newValue,
            String reason,
            OffsetDateTime at) {}
}

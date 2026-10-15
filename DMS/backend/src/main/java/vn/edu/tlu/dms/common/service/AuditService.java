package vn.edu.tlu.dms.common.service;

import vn.edu.tlu.dms.common.dto.PageResponse;

import java.time.OffsetDateTime;
import java.util.Map;

public interface AuditService {

    /**
     * Admin tìm nhật ký theo từ khóa trong mã thao tác/tên người thực hiện, người thực hiện, mã
     * thao tác, loại đối tượng và khoảng thời gian. Các bộ lọc được kết hợp bằng AND; from/to tính
     * cả hai mốc, có thể gửi riêng từng mốc và phải kèm múi giờ ISO-8601. Kết quả chỉ đọc, phân
     * trang theo ID giảm dần và giữ nguyên dữ liệu nhật ký đã lưu.
     */
    PageResponse<Map<String, Object>> searchAuditLogs(
            int page,
            int size,
            String q,
            Long actorId,
            String action,
            String entityType,
            OffsetDateTime from,
            OffsetDateTime to);

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
}

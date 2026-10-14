package vn.edu.tlu.dms.common.service;

public interface AuditService {

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

package vn.edu.tlu.dms.maintenance.service;

import vn.edu.tlu.dms.common.service.SystemJobService.Change;

import java.util.List;

public interface MaintenanceLifecycleService {

    /** Chọn ID ticket RESOLVED đã quá thời hạn nghiệm thu từ ConfigService. */
    List<Long> findAutoCloseIds();

    /**
     * Khóa, kiểm tra lại thời hạn/trạng thái rồi đóng ticket; không kết luận nguyên nhân hay trừ
     * cọc.
     */
    Change closeResolved(Long id);
}

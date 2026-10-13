package vn.edu.tlu.dms.maintenance.service;

import vn.edu.tlu.dms.maintenance.api.MaintenanceCrudController.Edit;

/** Khai báo sửa và hủy yêu cầu còn mở; phần xử lý nằm trong MaintenanceCrudServiceImpl. */
public interface MaintenanceCrudService {

    /** Chủ yêu cầu sửa loại, mức độ và nội dung khi yêu cầu còn ở trạng thái OPEN. */
    Object updateOpenRequest(Long id, Edit input);

    /**
     * Chủ yêu cầu hủy yêu cầu còn mở bằng cách chuyển sang CANCELLED. Giữ lại ảnh và lịch sử để tra
     * cứu, không xóa dữ liệu nghiệp vụ.
     */
    void cancelOpenRequest(Long id);
}

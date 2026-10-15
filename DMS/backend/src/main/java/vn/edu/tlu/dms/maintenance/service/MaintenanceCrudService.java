package vn.edu.tlu.dms.maintenance.service;

/** Khai báo hủy yêu cầu còn mở; thêm/sửa đi qua MaintenanceRequestService.saveOrUpdate. */
public interface MaintenanceCrudService {

    /**
     * Chủ yêu cầu hoặc ADMIN hủy yêu cầu còn mở bằng cách chuyển sang CANCELLED. Giữ lại ảnh và
     * lịch sử để tra cứu, không xóa dữ liệu nghiệp vụ.
     */
    void cancelOpenRequest(Long id);
}

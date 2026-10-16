package vn.edu.tlu.dms.account.service;

import vn.edu.tlu.dms.account.api.ManagementUnitRequest;
import vn.edu.tlu.dms.account.api.ManagementUnitResponse;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.service.CrudService;

import java.util.List;

/**
 * Hợp đồng CRUD đơn vị quản lý; logic kiểm tra quyền và dữ liệu nằm trong
 * ManagementUnitServiceImpl.
 */
public interface ManagementUnitService
        extends CrudService<ManagementUnitRequest, ManagementUnitResponse> {

    /**
     * Tìm kiếm, sắp xếp và phân trang đơn vị quản lý trong phạm vi được phép; trang bắt đầu từ 0.
     */
    @Override
    PageResponse<ManagementUnitResponse> searchByPage(SearchRequest search);

    /** Đọc danh mục cho lời gọi nội bộ; API danh sách sử dụng searchByPage để phân trang. */
    @Override
    List<ManagementUnitResponse> getAll();

    /** Đọc chi tiết theo ID; trả NOT_FOUND nếu không tồn tại hoặc nằm ngoài phạm vi truy cập. */
    @Override
    ManagementUnitResponse getById(Long id);

    /**
     * Lưu đơn vị quản lý: ID trống tạo mới, có ID cập nhật từng phần và giữ trường không gửi. Kiểm
     * tra đúng quyền CREATE/UPDATE và ghi nhật ký cùng transaction; ID không tồn tại không tạo mới.
     */
    @Override
    ManagementUnitResponse saveOrUpdate(ManagementUnitRequest input);

    /**
     * Xóa đơn vị quản lý chưa được sử dụng; ràng buộc tham chiếu tiếp tục bảo vệ dữ liệu lịch sử.
     */
    @Override
    void deleteById(Long id);
}

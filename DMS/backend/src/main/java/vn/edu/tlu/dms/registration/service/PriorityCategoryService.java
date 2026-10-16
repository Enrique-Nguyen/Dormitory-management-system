package vn.edu.tlu.dms.registration.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.service.CrudService;
import vn.edu.tlu.dms.registration.api.PriorityCategoryRequest;
import vn.edu.tlu.dms.registration.api.PriorityCategoryResponse;

import java.util.List;

/**
 * Hợp đồng CRUD diện ưu tiên; logic kiểm tra quyền và dữ liệu nằm trong
 * PriorityCategoryServiceImpl.
 */
public interface PriorityCategoryService
        extends CrudService<PriorityCategoryRequest, PriorityCategoryResponse> {

    /** Tìm kiếm, sắp xếp và phân trang diện ưu tiên trong phạm vi được phép; trang bắt đầu từ 0. */
    @Override
    PageResponse<PriorityCategoryResponse> searchByPage(SearchRequest search);

    /** Đọc danh mục cho lời gọi nội bộ; API danh sách sử dụng searchByPage để phân trang. */
    @Override
    List<PriorityCategoryResponse> getAll();

    /** Đọc chi tiết theo ID; trả NOT_FOUND nếu không tồn tại hoặc nằm ngoài phạm vi truy cập. */
    @Override
    PriorityCategoryResponse getById(Long id);

    /**
     * Lưu diện ưu tiên: ID trống tạo mới, có ID cập nhật từng phần và giữ trường không gửi. Kiểm
     * tra đúng quyền CREATE/UPDATE và ghi nhật ký cùng transaction; ID không tồn tại không tạo mới.
     */
    @Override
    PriorityCategoryResponse saveOrUpdate(PriorityCategoryRequest input);

    /** Xóa diện ưu tiên chưa được sử dụng; ràng buộc tham chiếu tiếp tục bảo vệ dữ liệu lịch sử. */
    @Override
    void deleteById(Long id);
}

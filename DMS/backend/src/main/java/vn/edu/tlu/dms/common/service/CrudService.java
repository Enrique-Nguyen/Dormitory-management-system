package vn.edu.tlu.dms.common.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;

import java.util.List;

public interface CrudService<I, R> {

    /** Tìm kiếm, sắp xếp và phân trang từ CSDL sau khi áp dụng quyền và phạm vi dữ liệu. */
    PageResponse<R> searchByPage(SearchRequest search);

    /** Đọc danh mục nhỏ; service cụ thể phải kiểm tra vai trò và phạm vi dữ liệu. */
    List<R> getAll();

    /** Tra cứu theo khóa chính; không tồn tại hoặc ngoài phạm vi thì trả NOT_FOUND. */
    R getById(Long id);

    /**
     * ID trống tạo mới, có ID cập nhật từng phần; kiểm tra đúng quyền của từng nhánh. Không tìm
     * thấy ID phải trả NOT_FOUND, không tự chuyển yêu cầu sửa thành tạo mới.
     */
    R saveOrUpdate(I input);

    /** Chỉ xóa danh mục chưa bị tham chiếu; ràng buộc FK bảo vệ dữ liệu đã được dùng. */
    void deleteById(Long id);
}

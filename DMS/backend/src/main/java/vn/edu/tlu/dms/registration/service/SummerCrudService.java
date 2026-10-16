package vn.edu.tlu.dms.registration.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.registration.api.SummerCrudController.Input;

import java.util.Map;

/** Hợp đồng CRUD lựa chọn hè chờ xử lý; logic nằm trong SummerCrudServiceImpl. */
public interface SummerCrudService {

    /** Tìm kiếm/phân trang trong phạm vi; mine do server chỉ định để ép chủ sở hữu hiện tại. */
    PageResponse<Map<String, Object>> searchByPage(SearchRequest search, boolean mine);

    /** Đọc chi tiết lựa chọn hè sau khi kiểm tra chủ sở hữu hoặc phạm vi cán bộ. */
    Object getChoice(Long id);

    /**
     * ID trống tạo hồ sơ chờ, có ID sửa lựa chọn đang chờ; kiểm CREATE/UPDATE riêng. Không đổi hợp
     * đồng/học kỳ nguồn, không tự xét duyệt hoặc thay đổi chỗ ở.
     */
    Object saveOrUpdate(Input input);

    /** Xóa lựa chọn hè đang chờ thuộc phạm vi; chặn dữ liệu đã được xử lý. */
    void deleteChoice(Long id);
}

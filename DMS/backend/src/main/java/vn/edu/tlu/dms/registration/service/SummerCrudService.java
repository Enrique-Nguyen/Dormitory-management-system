package vn.edu.tlu.dms.registration.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.registration.api.SummerCrudController.Input;

import java.util.Map;

/** Hợp đồng CRUD và xét duyệt lựa chọn hè; logic nằm trong SummerCrudServiceImpl. */
public interface SummerCrudService {

    /** Tìm kiếm/phân trang trong phạm vi; mine do server chỉ định để ép chủ sở hữu hiện tại. */
    PageResponse<Map<String, Object>> searchByPage(SearchRequest search, boolean mine);

    /**
     * Đọc chi tiết lựa chọn hè sau khi kiểm tra chủ sở hữu hoặc phạm vi cán bộ. Phản hồi gồm bản ghi
     * lựa chọn, tham chiếu hợp đồng nguồn (mã, trạng thái, thời hạn), sinh viên (mã, tên) và tên học
     * kỳ hè, lý do quyết định lấy từ lịch sử trạng thái; bảng summer_choice không lưu ghi chú của
     * sinh viên hay thời điểm nộp nên không có trường tương ứng.
     *
     * @param id mã định danh lựa chọn hè
     * @return dữ liệu lựa chọn kèm allowedActions và lịch sử trạng thái
     */
    Object getChoice(Long id);

    /**
     * Đọc lựa chọn hè mới nhất của sinh viên đang đăng nhập.
     *
     * @return chi tiết lựa chọn mới nhất; null nếu sinh viên chưa gửi lựa chọn nào
     */
    Object getMyCurrentChoice();

    /**
     * ID trống tạo hồ sơ chờ, có ID sửa lựa chọn đang chờ; kiểm CREATE/UPDATE riêng. Không đổi hợp
     * đồng/học kỳ nguồn, không tự xét duyệt hoặc thay đổi chỗ ở.
     */
    Object saveOrUpdate(Input input);

    /**
     * Duyệt lựa chọn hè đang chờ của đơn vị quản lý. Lựa chọn STAY gia hạn hợp đồng qua học kỳ hè;
     * TEMP_LEAVE/LEAVE chỉ đổi trạng thái, không thay hợp đồng hoặc chỗ ở.
     *
     * @param id mã định danh lựa chọn hè
     * @return chi tiết lựa chọn sau khi duyệt
     */
    Object approve(Long id);

    /**
     * Từ chối lựa chọn hè đang chờ; lý do bắt buộc và được lưu vào lịch sử/audit.
     *
     * @param id mã định danh lựa chọn hè
     * @param reason lý do từ chối do cán bộ nhập
     * @return chi tiết lựa chọn sau khi từ chối
     */
    Object reject(Long id, String reason);

    /** Xóa lựa chọn hè đang chờ thuộc phạm vi; chặn dữ liệu đã được xử lý. */
    void deleteChoice(Long id);
}

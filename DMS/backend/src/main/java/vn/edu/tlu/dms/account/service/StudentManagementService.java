package vn.edu.tlu.dms.account.service;

import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.api.StudentManagementController.Save;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;

import java.util.Map;

/**
 * Quản lý hồ sơ thật. Staff chỉ được xem/sửa sinh viên có lịch sử hoặc nguyện vọng trong đơn vị.
 */
public interface StudentManagementService {

    /**
     * Tìm kiếm theo mã sinh viên/họ tên và phân trang sau khi lọc phạm vi; Admin xem toàn bộ, Staff
     * xem phạm vi được cấp.
     */
    PageResponse<Map<String, Object>> searchByPage(SearchRequest input);

    /** Đọc hồ sơ sinh viên theo ID tài khoản; trả NOT_FOUND khi không có hoặc ngoài phạm vi. */
    Map<String, Object> getStudentById(Long id);

    /**
     * Không có ID: Admin tạo tài khoản/hồ sơ và nhận mật khẩu tạm một lần. Có ID: kiểm quyền sửa,
     * phạm vi và lịch sử; chỉ cập nhật trường được gửi. Sửa mã sinh viên thu hồi phiên chứa tên
     * đăng nhập cũ, không cấp lại mật khẩu.
     */
    Object saveOrUpdate(Save input);

    /**
     * Admin xóa sinh viên chưa có dữ liệu liên quan; từ chối nếu hồ sơ đã phát sinh lịch sử hoặc
     * đang được tham chiếu.
     */
    void deleteUnusedStudent(Long id);

    /**
     * Staff đặt lại mật khẩu cho sinh viên thuộc đơn vị mình theo BR-07: có đơn trong đợt đang mở
     * hoặc phân phòng hiệu lực tại đơn vị. Lịch sử đã kết thúc và quyền đọc liên đơn vị không đủ
     * quyền reset. Trả mật khẩu tạm một lần, thu hồi phiên cũ và bắt đổi mật khẩu khi đăng nhập
     * lại.
     */
    TemporaryPassword resetStudentPassword(Long id);

    /** Tìm và phân trang kỹ thuật viên đang hoạt động trong phạm vi để chọn trên form. */
    PageResponse<Map<String, Object>> searchTechniciansByPage(SearchRequest input);
}

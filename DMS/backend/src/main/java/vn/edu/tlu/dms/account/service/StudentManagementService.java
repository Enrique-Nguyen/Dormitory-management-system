package vn.edu.tlu.dms.account.service;

import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.api.StudentManagementController.StudentInput;
import vn.edu.tlu.dms.account.api.StudentManagementController.StudentPatch;
import vn.edu.tlu.dms.common.dto.PageResponse;

import java.util.List;
import java.util.Map;

/**
 * Quản lý hồ sơ thật. Staff chỉ được xem/sửa sinh viên có lịch sử hoặc nguyện vọng trong đơn vị.
 */
public interface StudentManagementService {

    /**
     * Tìm kiếm theo mã sinh viên/họ tên và phân trang sau khi lọc phạm vi; Admin xem toàn bộ, Staff
     * xem phạm vi được cấp.
     */
    PageResponse<Map<String, Object>> searchStudents(int page, int size, String q);

    /** Đọc hồ sơ sinh viên theo ID tài khoản; trả NOT_FOUND khi không có hoặc ngoài phạm vi. */
    Map<String, Object> getStudentById(Long id);

    /**
     * Admin tạo tài khoản và hồ sơ sinh viên trong một transaction; trả mật khẩu tạm một lần và bắt
     * đổi khi đăng nhập.
     */
    TemporaryPassword createStudent(StudentInput in);

    /**
     * Sửa các trường hồ sơ được phép, kiểm tra vai trò và phạm vi; dữ liệu bỏ trống không dùng để
     * xóa tùy tiện lịch sử.
     */
    Map<String, Object> updateStudent(Long id, StudentPatch in);

    /**
     * Admin xóa sinh viên chưa có dữ liệu liên quan; từ chối nếu hồ sơ đã phát sinh lịch sử hoặc
     * đang được tham chiếu.
     */
    void deleteUnusedStudent(Long id);

    /**
     * Đặt lại mật khẩu trong phạm vi được phép, trả mật khẩu tạm một lần và bật cờ bắt đổi mật
     * khẩu.
     */
    TemporaryPassword resetStudentPassword(Long id);

    /** Đọc danh sách kỹ thuật viên trong phạm vi được phép để dùng làm dữ liệu chọn trên form. */
    List<Map<String, Object>> getTechnicians();
}

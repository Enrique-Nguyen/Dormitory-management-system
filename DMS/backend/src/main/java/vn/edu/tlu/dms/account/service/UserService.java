package vn.edu.tlu.dms.account.service;

import vn.edu.tlu.dms.account.api.AccountDtos.Profile;
import vn.edu.tlu.dms.account.api.AccountDtos.ProfileInput;
import vn.edu.tlu.dms.account.api.AccountDtos.Save;
import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;

public interface UserService {

    /**
     * Tìm tài khoản bằng từ khóa, vai trò, đơn vị và trạng thái khóa; trang bắt đầu từ 0. Các bộ
     * lọc giao nhau và chỉ cho phép các trường sắp xếp đã đăng ký.
     */
    PageResponse<Profile> searchByPage(SearchRequest input);

    /** Admin đọc chi tiết tài khoản theo ID; không trả mật khẩu hoặc hash. */
    Profile get(Long id);

    /**
     * Không có ID: kiểm quyền tạo, trường bắt buộc và cấp mật khẩu tạm một lần. Có ID: kiểm quyền
     * sửa, cập nhật trường được gửi và thu hồi phiên khi đổi vai trò/đơn vị. Kết quả tạo là
     * TemporaryPassword; kết quả sửa là Profile, không chứa mật khẩu.
     */
    Object saveOrUpdate(Save input);

    /** Admin khóa tài khoản với lý do bắt buộc, không được tự khóa; thu hồi toàn bộ phiên đã mở. */
    void lock(Long id, String reason);

    /**
     * Admin mở khóa và xóa khóa tạm/bộ đếm đăng nhập sai; người dùng phải đăng nhập mới, các phiên
     * bị thu hồi trước đó không được khôi phục.
     */
    void unlock(Long id);

    /**
     * Admin đặt mật khẩu tạm mới cho tài khoản, trả một lần, xóa khóa tạm/bộ đếm sai và thu hồi các
     * phiên cũ; lần đăng nhập tiếp theo bắt buộc đổi mật khẩu.
     */
    TemporaryPassword resetPassword(Long id);

    /**
     * Admin bật/tắt quyền đọc tổng hợp liên đơn vị cho Staff; quyền này không mở rộng quyền ghi.
     * Thu hồi phiên khi giá trị quyền thực sự thay đổi, yêu cầu đăng nhập lại để nhận quyền mới.
     */
    void crossUnitAccess(Long id, boolean enabled);

    /** Đọc hồ sơ của tài khoản hiện tại từ phiên đăng nhập; không nhận ID người khác từ client. */
    Profile profile();

    /** Cập nhật các trường cá nhân được phép của chính người đăng nhập và ghi audit. */
    Profile saveOrUpdate(ProfileInput input);
}

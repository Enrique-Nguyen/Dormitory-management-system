package vn.edu.tlu.dms.account.service;

import vn.edu.tlu.dms.account.api.AccountDtos.Create;
import vn.edu.tlu.dms.account.api.AccountDtos.Profile;
import vn.edu.tlu.dms.account.api.AccountDtos.ProfileInput;
import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.api.AccountDtos.Update;
import vn.edu.tlu.dms.common.dto.PageResponse;

import java.util.List;

public interface UserService {

    /**
     * Admin tìm kiếm và phân trang tài khoản theo từ khóa/vai trò; chỉ cho phép các trường sắp xếp
     * được khai báo.
     */
    PageResponse<Profile> list(int page, int size, List<String> sort, String q, String role);

    /** Admin đọc chi tiết tài khoản theo ID; không trả mật khẩu hoặc hash. */
    Profile get(Long id);

    /**
     * Admin tạo tài khoản cán bộ/quản trị, kiểm tra vai trò và đơn vị; trả mật khẩu tạm một lần.
     */
    TemporaryPassword create(Create input);

    /**
     * Admin cập nhật các trường tài khoản được phép, kiểm tra vai trò và ghi audit; thu hồi toàn bộ
     * phiên nếu tên đăng nhập hoặc đơn vị quản lý thay đổi để áp dụng phạm vi mới khi đăng nhập
     * lại.
     */
    Profile update(Long id, Update input);

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
    Profile updateProfile(ProfileInput input);
}

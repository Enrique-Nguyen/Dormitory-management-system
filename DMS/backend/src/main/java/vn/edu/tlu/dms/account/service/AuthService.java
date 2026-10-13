package vn.edu.tlu.dms.account.service;

import vn.edu.tlu.dms.account.api.AccountDtos.Me;
import vn.edu.tlu.dms.account.security.DmsPrincipal;

public interface AuthService {

    /**
     * Khóa tài khoản khi kiểm tra mật khẩu để không mất số lần sai khi có request đồng thời. Không
     * rollback BusinessException: đăng nhập thất bại vẫn phải lưu bộ đếm và hạn khóa. Mật khẩu và
     * hash không được ghi log hoặc đưa vào response.
     */
    DmsPrincipal authenticate(String username, String password);

    /**
     * Trả thông tin người đăng nhập, vai trò, đơn vị và cờ bắt đổi mật khẩu; không trả mật khẩu
     * hoặc hash.
     */
    Me getCurrentUser();

    /**
     * Kiểm tra mật khẩu hiện tại và quy tắc mật khẩu mới, cập nhật BCrypt và gỡ cờ bắt đổi mật
     * khẩu.
     */
    void changePassword(String current, String next);
}

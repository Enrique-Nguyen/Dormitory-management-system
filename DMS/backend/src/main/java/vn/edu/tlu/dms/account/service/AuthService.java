package vn.edu.tlu.dms.account.service;

import vn.edu.tlu.dms.account.api.AccountDtos.Me;
import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.domain.AppUser;

public interface AuthService {

    /** Trả hồ sơ, vai trò và phạm vi nghiệp vụ của danh tính Keycloak đang đăng nhập. */
    Me getCurrentUser();

    /** Khóa và đọc lại tài khoản mới nhất; bắt buộc gọi bên trong transaction ghi. */
    AppUser lockAccount(Long id);

    /** Tăng phiên bản bảo mật, đồng bộ Keycloak và thu hồi phiên để chặn token cũ ngay. */
    void revokeSessions(AppUser user);

    /** Cấp mật khẩu tạm tại Keycloak, bắt đổi lần đầu, xóa khóa tạm và ghi audit. */
    TemporaryPassword resetPassword(AppUser user, Long actorId);

    /** Thu hồi token và mọi phiên Keycloak của tài khoản hiện tại khi đăng xuất từ DMS. */
    void revokeCurrentIdentitySessions();
}

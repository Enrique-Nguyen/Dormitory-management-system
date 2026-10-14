package vn.edu.tlu.dms.account.service;

import vn.edu.tlu.dms.account.domain.AppUser;

public interface IdentityAccountService {

    /**
     * Tạo danh tính đăng nhập, liên kết ID và đăng ký dọn tài khoản nếu transaction DMS rollback.
     */
    void provisionAccount(AppUser user, String temporaryPassword);

    /** Đồng bộ tên, trạng thái, vai trò và phiên bản bảo mật sang máy chủ xác thực. */
    void synchronizeAccount(AppUser user);

    /** Đặt mật khẩu tạm tại máy chủ xác thực, bắt đổi lần đầu và thu hồi phiên đăng nhập. */
    void resetAccountPassword(AppUser user, String temporaryPassword);

    /** Xóa bộ đếm đăng nhập sai và khóa tạm tại máy chủ xác thực khi reset hoặc mở khóa. */
    void clearTemporaryLoginFailures(AppUser user);

    /** Thu hồi các phiên của danh tính; phiên bản bảo mật ở DMS chặn token cũ ngay lập tức. */
    void revokeIdentitySessions(AppUser user);

    /** Xóa danh tính sau khi transaction xóa hồ sơ DMS đã commit thành công. */
    void deleteIdentityAfterCommit(AppUser user);

    /** Chuyển từng tài khoản chưa liên kết, giữ nguyên ID hồ sơ và quan hệ nghiệp vụ DMS. */
    void migrateUnlinkedAccounts(
            String adminUsername, String initialAdminPassword, String credentialFile);
}

package vn.edu.tlu.dms.account.service;

public interface AccountReferenceService {

    /** Kiểm tra đơn vị quản lý tồn tại; báo NOT_FOUND nếu ID không hợp lệ. */
    void requireUnit(Long id);

    /**
     * Lấy thông tin tham chiếu tối thiểu của tài khoản để module khác kiểm tra vai trò và đơn vị,
     * không trả entity.
     */
    UserRef user(Long id);

    public record UserRef(
            Long id, String fullName, String username, String role, Long unitId, String gender) {}
}

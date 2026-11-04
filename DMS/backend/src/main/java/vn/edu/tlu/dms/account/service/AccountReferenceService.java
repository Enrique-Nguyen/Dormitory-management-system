package vn.edu.tlu.dms.account.service;

import java.util.Collection;
import java.util.Map;

public interface AccountReferenceService {

    /** Kiểm tra đơn vị quản lý tồn tại; báo NOT_FOUND nếu ID không hợp lệ. */
    void requireUnit(Long id);

    /**
     * Lấy thông tin tham chiếu tối thiểu của tài khoản để module khác kiểm tra vai trò và đơn vị,
     * không trả entity.
     */
    UserRef user(Long id);

    /**
     * Lấy thông tin tham chiếu của nhiều tài khoản trong hai truy vấn cố định, dùng cho danh sách
     * để tránh gọi {@link #user(Long)} lặp theo từng dòng. ID null hoặc không tồn tại bị bỏ qua.
     *
     * @param ids danh sách ID tài khoản cần tra cứu, có thể trùng
     * @return bản đồ ID tài khoản sang thông tin tham chiếu
     */
    Map<Long, UserRef> users(Collection<Long> ids);

    public record UserRef(
            Long id, String fullName, String username, String role, Long unitId, String gender) {}
}

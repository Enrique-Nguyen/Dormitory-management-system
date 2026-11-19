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

    /**
     * Kiểm tra tài khoản được chọn làm người phụ trách (trưởng ban, quản lý tòa, thủ kho) là cán bộ
     * STAFF thuộc đúng đơn vị; ID null được bỏ qua để trường tùy chọn có thể để trống.
     *
     * @param userId ID tài khoản được chọn, có thể null
     * @param unitId ID đơn vị quản lý mà người phụ trách phải thuộc về
     */
    void requireStaffOfUnit(Long userId, Long unitId);

    /**
     * Lấy tên đơn vị quản lý theo lô ID để hiển thị cạnh khóa ngoại, tránh trả ID thô cho giao diện.
     *
     * @param ids danh sách ID đơn vị, có thể trùng hoặc chứa null
     * @return bản đồ ID đơn vị sang tên đơn vị
     */
    Map<Long, String> unitNames(Collection<Long> ids);

    public record UserRef(
            Long id, String fullName, String username, String role, Long unitId, String gender) {}
}

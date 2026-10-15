package vn.edu.tlu.dms.common.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Hợp đồng ma trận quyền; toàn bộ kiểm tra và lưu dữ liệu nằm trong Service Impl. */
public interface RolePermissionService {

    /**
     * Admin xem danh mục màn hình/thao tác, quyền đang cấp và quyền mặc định của một vai trò. ADMIN
     * luôn có toàn quyền, chỉ hiển thị để tham khảo và không được sửa.
     */
    Matrix getRolePermissionMatrix(String role);

    /**
     * Admin lưu toàn bộ tập quyền được tick cho vai trò. Phải gửi phiên bản đã đọc để tránh ghi đè
     * thay đổi của người khác; quyền và nhật ký được lưu trong cùng một transaction.
     */
    Matrix updateRolePermissions(String role, Update input);

    /**
     * Lấy quyền có hiệu lực trực tiếp từ CSDL mỗi lần backend kiểm tra hoặc trả /auth/me. Vai trò
     * lạ hoặc bản ghi thiếu/sai không có quyền; không tự quay về quyền mặc định. ADMIN luôn nhận
     * toàn bộ danh mục quyền đã đăng ký.
     */
    List<String> getGrantedPermissions(String role);

    record Matrix(
            String role, String roleLabel, long version, boolean readOnly, List<Screen> screens) {}

    record Screen(String code, String label, String group, List<Privilege> privileges) {}

    record Privilege(
            String code,
            String action,
            String label,
            boolean granted,
            boolean defaultGranted,
            boolean assignable,
            boolean required) {}

    /** Chỉ nhận mã quyền đã đăng ký; version là phiên bản hiển thị lúc mở màn hình. */
    record Update(
            @NotNull @PositiveOrZero Long version,
            @NotNull @Size(max = 512) List<@NotBlank String> permissions) {}
}

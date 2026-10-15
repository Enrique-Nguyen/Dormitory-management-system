package vn.edu.tlu.dms.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Khai quyền API giống V5. Một mã: bắt buộc có quyền đó; nhiều mã: chỉ cần có một quyền (OR).
 * Annotation trên hàm thay thế khai báo của lớp. Không bỏ qua kiểm tra phạm vi/trạng thái ở Service
 * Impl. API không khai quyền hoặc dùng mảng rỗng bị từ chối; Admin cũng chỉ nhận mã quyền đã đăng
 * ký.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequirePrivilege {

    /** Danh sách mã quyền đã có trong ma trận ScopeGuard, ví dụ ROOM_READ hoặc ROOM_CREATE. */
    String[] value();
}

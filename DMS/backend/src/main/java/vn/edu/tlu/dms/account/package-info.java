/**
 * Module {@code account}: tài khoản, RBAC, CurrentUser, ScopeGuard, import sinh viên.
 *
 * <p>Phụ trách: TV1. Sở hữu bảng (ghi): management_unit, app_user, student_profile.
 *
 * <p>Gói con: {@code api} (controller + DTO record), {@code service} (Service công khai, Policy),
 * {@code domain} (entity, enum, sự kiện), {@code repository}, {@code job}. Quy tắc phụ thuộc giữa
 * các module: SAD mục 4.2, được kiểm tra bởi {@code ModuleDependencyTest}.
 */
package vn.edu.tlu.dms.account;

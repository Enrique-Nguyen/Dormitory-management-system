/**
 * Module {@code notification}: thông báo, bộ lắng nghe sự kiện nghiệp vụ, truy vấn dashboard.
 *
 * <p>Phụ trách: TV4 (thông báo), TV5 (dashboard). Sở hữu bảng (ghi): notification,
 * notification_read.
 *
 * <p>Gói con: {@code api} (controller + DTO record), {@code service} (Service công khai, Policy),
 * {@code domain} (entity, enum, sự kiện), {@code repository}, {@code job}. Quy tắc phụ thuộc giữa
 * các module: SAD mục 4.2, được kiểm tra bởi {@code ModuleDependencyTest}.
 */
package vn.edu.tlu.dms.notification;

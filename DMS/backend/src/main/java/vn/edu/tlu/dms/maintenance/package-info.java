/**
 * Module {@code maintenance}: yêu cầu sửa chữa, ảnh sự cố, kết luận chi phí.
 *
 * <p>Phụ trách: TV4. Sở hữu bảng (ghi): maintenance_request, maintenance_photo.
 *
 * <p>Gói con: {@code api} (controller + DTO record), {@code service} (Service công khai, Policy),
 * {@code domain} (entity, enum, sự kiện), {@code repository}, {@code job}. Quy tắc phụ thuộc giữa
 * các module: SAD mục 4.2, được kiểm tra bởi {@code ModuleDependencyTest}.
 */
package vn.edu.tlu.dms.maintenance;

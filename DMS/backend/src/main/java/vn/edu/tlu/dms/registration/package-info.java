/**
 * Module {@code registration}: đợt đăng ký, đơn đăng ký, xét duyệt và phân phòng.
 *
 * <p>Phụ trách: TV3. Sở hữu bảng (ghi): registration_period*, registration_quota, priority_category, room_application, application_priority, summer_choice.
 *
 * <p>Gói con: {@code api} (controller + DTO record), {@code service} (Service công khai, Policy),
 * {@code domain} (entity, enum, sự kiện), {@code repository}, {@code job}. Quy tắc phụ thuộc giữa
 * các module: SAD mục 4.2, được kiểm tra bởi {@code ModuleDependencyTest}.
 */
package vn.edu.tlu.dms.registration;

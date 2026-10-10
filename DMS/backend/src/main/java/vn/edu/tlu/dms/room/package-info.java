/**
 * Module {@code room}: tòa, phòng, giường; BedOccupancyService là đường duy nhất để chiếm / trả / chuyển giường.
 *
 * <p>Phụ trách: TV2. Sở hữu bảng (ghi): building, room_type, room, bed, semester, room_assignment_history.
 *
 * <p>Gói con: {@code api} (controller + DTO record), {@code service} (Service công khai, Policy),
 * {@code domain} (entity, enum, sự kiện), {@code repository}, {@code job}. Quy tắc phụ thuộc giữa
 * các module: SAD mục 4.2, được kiểm tra bởi {@code ModuleDependencyTest}.
 */
package vn.edu.tlu.dms.room;

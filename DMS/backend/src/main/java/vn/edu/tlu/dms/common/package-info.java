/**
 * Module {@code common}: xử lý lỗi, cấu hình, lịch sử trạng thái, nhật ký, sinh mã, lớp cơ sở cho
 * job định kỳ.
 *
 * <p>Phụ trách: TV1. Sở hữu bảng (ghi): system_config, status_history, audit_log.
 *
 * <p>Gói con: {@code api} (controller + DTO record), {@code service} (Service công khai, Policy),
 * {@code domain} (entity, enum, sự kiện), {@code repository}, {@code job}. Quy tắc phụ thuộc giữa
 * các module: SAD mục 4.2, được kiểm tra bởi {@code ModuleDependencyTest}.
 */
package vn.edu.tlu.dms.common;

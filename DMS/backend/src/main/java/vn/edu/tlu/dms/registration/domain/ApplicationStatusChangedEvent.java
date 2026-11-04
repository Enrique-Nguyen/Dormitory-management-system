package vn.edu.tlu.dms.registration.domain;

/**
 * Sự kiện thông báo sau khi đơn đăng ký được duyệt, bị từ chối hoặc được giữ chỗ (FR-NOT-04). Chỉ
 * chứa ID và giá trị hiển thị; module notification xử lý sau commit, lỗi chỉ được ghi log.
 *
 * @param applicationId đơn đăng ký vừa đổi trạng thái
 * @param studentId sinh viên nhận thông báo
 * @param status trạng thái mới của đơn (APPROVED, REJECTED hoặc ASSIGNED)
 * @param reason lý do từ chối, null với các trạng thái khác
 * @param bedLabel mã phòng-giường đang giữ khi ASSIGNED, null với các trạng thái khác
 */
public record ApplicationStatusChangedEvent(
        Long applicationId, Long studentId, String status, String reason, String bedLabel) {}

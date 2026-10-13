package vn.edu.tlu.dms.notification.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.notification.api.NotificationDtos;

import java.util.List;

/**
 * Hợp đồng xử lý hộp thư và tạo thông báo. Phần kiểm tra quyền, truy vấn và ghi dữ liệu được triển
 * khai trong NotificationServiceImpl.
 */
public interface NotificationService {

    /**
     * Lấy thông báo của người đang đăng nhập, giới hạn theo phạm vi nhận và thời gian cư trú.
     *
     * @param page chỉ số trang, bắt đầu từ 0
     * @param size số thông báo trên mỗi trang
     * @param unread true để chỉ lấy chưa đọc, false để chỉ lấy đã đọc, null để lấy cả hai
     * @return trang thông báo mà người dùng được phép xem
     */
    PageResponse<NotificationDtos.Response> getMyNotifications(int page, int size, Boolean unread);

    /**
     * Tìm kiếm thông báo trong hộp thư theo tiêu đề hoặc nội dung trước khi phân trang.
     *
     * @param page chỉ số trang, bắt đầu từ 0
     * @param size số thông báo trên mỗi trang
     * @param unread true để chỉ lấy chưa đọc, false để chỉ lấy đã đọc, null để lấy cả hai
     * @param keyword từ khóa tìm kiếm; chuỗi rỗng để không giới hạn theo từ khóa
     * @return trang thông báo phù hợp với bộ lọc và quyền nhận của người dùng
     */
    PageResponse<NotificationDtos.Response> getMyNotifications(
            int page, int size, Boolean unread, String keyword);

    /**
     * Đếm thông báo chưa đọc trong hộp thư của người đang đăng nhập.
     *
     * @return tổng số thông báo chưa đọc mà người dùng được phép xem
     */
    NotificationDtos.UnreadCount getUnreadNotificationCount();

    /**
     * Đánh dấu một thông báo là đã đọc sau khi xác nhận thông báo thuộc hộp thư của người dùng. Gọi
     * lại với thông báo đã đọc không tạo bản ghi trùng.
     *
     * @param notificationId mã định danh thông báo cần đánh dấu
     */
    void markNotificationAsRead(Long notificationId);

    /** Đánh dấu toàn bộ thông báo chưa đọc trong hộp thư của người đang đăng nhập là đã đọc. */
    void markAllNotificationsAsRead();

    /**
     * Tạo thông báo thủ công của nhân viên sau khi kiểm tra phạm vi đơn vị, tòa nhà, tầng hoặc
     * phòng. Phạm vi toàn hệ thống yêu cầu quyền truy cập liên đơn vị.
     *
     * @param input phạm vi nhận, tiêu đề và nội dung thông báo
     * @return thông báo vừa được tạo
     */
    NotificationDtos.Response createManualNotification(NotificationDtos.Create input);

    /**
     * Tạo thông báo mở đợt hè cho sinh viên có hợp đồng đang hoạt động tại các đơn vị nhận tin.
     * Được gọi sau khi đợt đăng ký đã commit và dùng transaction riêng để lỗi thông báo không làm
     * mất dữ liệu đợt đăng ký.
     *
     * @param periodId mã định danh đợt đăng ký hè
     * @param periodName tên đợt đăng ký dùng làm nội dung thông báo
     * @param unitIds các đơn vị quản lý cần gửi thông báo
     * @param actorId mã định danh người tạo đợt đăng ký
     */
    void notifyActiveStudentsOfSummerPeriod(
            Long periodId, String periodName, List<Long> unitIds, Long actorId);
}

package vn.edu.tlu.dms.notification.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.notification.api.NotificationDtos;

import java.util.List;

/**
 * Hợp đồng xử lý hộp thư và tạo thông báo. Phần kiểm tra quyền, truy vấn và ghi dữ liệu được triển
 * khai trong NotificationServiceImpl.
 */
public interface NotificationService {

    /**
     * Tìm kiếm và phân trang thông báo trong hộp thư theo yêu cầu tìm kiếm chuẩn hóa.
     *
     * @param request yêu cầu tìm kiếm gồm trang, kích thước, từ khóa và bộ lọc trạng thái đọc
     * @return trang kết quả thông báo trong hộp thư
     */
    PageResponse<NotificationDtos.Response> searchByPage(SearchRequest request);

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

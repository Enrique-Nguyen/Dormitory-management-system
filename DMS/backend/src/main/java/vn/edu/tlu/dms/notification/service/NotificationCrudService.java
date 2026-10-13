package vn.edu.tlu.dms.notification.service;

import vn.edu.tlu.dms.notification.api.NotificationDtos;

/**
 * Hợp đồng tra cứu, sửa và xóa thông báo thủ công của người đăng. Việc tạo thông báo được cung cấp
 * bởi NotificationService; thông báo hệ thống không được sửa hoặc xóa tại đây.
 */
public interface NotificationCrudService {

    /**
     * Tìm kiếm thông báo thủ công do nhân viên đang đăng nhập tạo, theo tiêu đề hoặc nội dung.
     *
     * @param page chỉ số trang, bắt đầu từ 0
     * @param size số thông báo trên mỗi trang
     * @param q từ khóa tìm kiếm; chuỗi rỗng để lấy toàn bộ thông báo được phép quản lý
     * @return kết quả tìm kiếm đã phân trang
     */
    Object searchPublished(int page, int size, String q);

    /**
     * Lấy chi tiết thông báo thủ công thuộc quyền quản lý của người đang đăng nhập.
     *
     * @param id mã định danh thông báo cần xem
     * @return dữ liệu thông báo; báo lỗi nếu không tồn tại hoặc không thuộc người đăng
     */
    Object getPublished(Long id);

    /**
     * Sửa tiêu đề và nội dung thông báo thủ công của người đăng, đồng thời ghi nhật ký thay đổi.
     * Phạm vi nhận và các trường vị trí phải giữ nguyên để không thay đổi người nhận trước đó.
     *
     * @param id mã định danh thông báo cần sửa
     * @param input nội dung mới kèm phạm vi nhận hiện tại của thông báo
     * @return thông báo sau khi cập nhật
     */
    Object updatePublished(Long id, NotificationDtos.Create input);

    /**
     * Xóa thông báo thủ công của người đăng và các bản ghi đánh dấu đã đọc liên quan; thao tác được
     * ghi nhật ký trong cùng transaction.
     *
     * @param id mã định danh thông báo cần xóa
     */
    void deletePublished(Long id);
}

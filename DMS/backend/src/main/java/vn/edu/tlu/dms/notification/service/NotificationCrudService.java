package vn.edu.tlu.dms.notification.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.notification.api.NotificationDtos;

import java.util.Map;

/**
 * Hợp đồng tìm kiếm, lưu chung và xóa thông báo thủ công của người đăng. ADMIN được quản lý thông
 * báo thủ công của mọi người; thông báo hệ thống không được sửa hoặc xóa tại đây.
 */
public interface NotificationCrudService {

    /**
     * Tìm kiếm và phân trang thông báo đã xuất bản theo yêu cầu tìm kiếm chuẩn hóa.
     *
     * @param request yêu cầu tìm kiếm gồm trang, kích thước, từ khóa
     * @return trang kết quả thông báo đã xuất bản
     */
    PageResponse<Map<String, Object>> searchByPage(SearchRequest request);

    /**
     * Thêm mới hoặc cập nhật thông báo thủ công theo mã định danh (id).
     *
     * @param input thông tin thông báo cần tạo mới (id = null) hoặc cập nhật (id != null)
     * @return thông tin thông báo sau khi lưu
     */
    Object saveOrUpdate(NotificationDtos.Create input);

    /**
     * Lấy chi tiết thông báo thủ công thuộc quyền quản lý của người đang đăng nhập.
     *
     * @param id mã định danh thông báo cần xem
     * @return dữ liệu thông báo; STAFF chỉ truy cập bài tự đăng, ADMIN được truy cập mọi bài thủ
     *     công
     */
    Map<String, Object> getPublished(Long id);

    /**
     * Xóa thông báo thủ công của người đăng và các bản ghi đánh dấu đã đọc liên quan; thao tác được
     * ghi nhật ký trong cùng transaction.
     *
     * @param id mã định danh thông báo cần xóa
     */
    void deletePublished(Long id);
}

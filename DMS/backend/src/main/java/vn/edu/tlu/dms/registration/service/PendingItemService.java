package vn.edu.tlu.dms.registration.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.registration.api.PendingItemController.Item;

/**
 * Hộp yêu cầu chờ xử lý của cán bộ. Dữ liệu được tổng hợp từ các Service công khai của
 * registration và contract; không truy vấn chéo bảng của module khác.
 */
public interface PendingItemService {

    /**
     * Tìm kiếm/phân trang yêu cầu đang chờ trong phạm vi đơn vị của cán bộ: đơn SUBMITTED, yêu cầu
     * gia hạn/chuyển phòng PENDING, lựa chọn hè PENDING và hợp đồng PENDING chờ kích hoạt.
     *
     * @param input từ khóa, loại yêu cầu (type) và phân trang
     * @return trang yêu cầu chờ, mới nhất trước
     */
    PageResponse<Item> searchByPage(SearchRequest input);
}

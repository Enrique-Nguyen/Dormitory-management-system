package vn.edu.tlu.dms.billing.service;

import vn.edu.tlu.dms.billing.api.UtilityReadingController.Input;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;

import java.util.Map;

/** CRUD chỉ số điện nước theo phòng/tháng, luôn kiểm tra quyền và đơn vị tại Impl. */
public interface UtilityReadingService {
    /**
     * Tìm theo phòng, tháng, loại đồng hồ và phân trang/sắp xếp tại CSDL trong phạm vi được đọc.
     */
    PageResponse<Map<String, Object>> searchByPage(SearchRequest input);

    /**
     * ID trống để tạo chỉ số mới; có ID để cập nhật các trường được gửi. Khi sửa không đổi phòng,
     * loại đồng hồ hoặc tháng; từ chối dữ liệu đã lập hóa đơn/tháng sau. Dữ liệu chưa gửi giữ
     * nguyên, không mặc định chỉ số cuối về 0.
     */
    Map<String, Object> saveOrUpdate(Input input);

    /**
     * Đọc chi tiết và allowedActions sau khi kiểm quyền, không tồn tại/ngoài đơn vị trả NOT_FOUND.
     */
    Map<String, Object> getReading(Long id);

    /** Xóa chỉ số chưa lập hóa đơn và chưa có tháng tiếp theo; ghi audit trong cùng transaction. */
    void deleteUnusedReading(Long id);
}

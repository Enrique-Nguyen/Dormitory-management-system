package vn.edu.tlu.dms.room.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.room.api.FloorRequest;

import java.util.Map;

public interface FloorService {
    /** Tìm danh mục tầng theo tòa và phạm vi đơn vị trước khi phân trang. */
    PageResponse<Map<String, Object>> searchByPage(SearchRequest input);

    /** Đọc chi tiết tầng trong phạm vi được cấp quyền. */
    Map<String, Object> getById(Long id);

    /** Thêm hoặc cập nhật tầng, giữ nguyên tòa và số tầng đã khai báo. */
    Map<String, Object> saveOrUpdate(FloorRequest input);

    /** Xóa tầng chưa được sử dụng trong dữ liệu nghiệp vụ. */
    void delete(Long id);

    /** Kiểm tra và khóa chia sẻ tầng đang sử dụng trước khi gán vào nghiệp vụ. */
    void requireActiveFloor(Long buildingId, Short floor);
}

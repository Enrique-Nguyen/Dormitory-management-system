package vn.edu.tlu.dms.contract.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.contract.api.StorageLocationDtos;

public interface StorageLocationService {
    /** Tra cứu phân trang vị trí lưu đồ trong phạm vi đơn vị được đọc. */
    PageResponse<StorageLocationDtos.Location> searchByPage(StorageLocationDtos.Search input);

    /** Đọc vị trí, kể cả vị trí đã ngừng sử dụng để hiển thị lịch sử. */
    StorageLocationDtos.Location getById(Long id);

    /** Tạo hoặc cập nhật tên và hiệu lực; mã và đơn vị bất biến sau khi tạo. */
    StorageLocationDtos.Location saveOrUpdate(StorageLocationDtos.SaveRequest input);

    /** Xóa vị trí chưa được tham chiếu; không xóa dữ liệu đã có lịch sử gửi đồ. */
    void delete(Long id);

    /** Khóa chia sẻ và kiểm tra vị trí đang hoạt động thuộc đúng đơn vị gửi đồ. */
    StorageLocationDtos.Location requireActiveForUnit(Long id, Long managementUnitId);
}

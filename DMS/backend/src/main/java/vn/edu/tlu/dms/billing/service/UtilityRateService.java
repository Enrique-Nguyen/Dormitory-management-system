package vn.edu.tlu.dms.billing.service;

import vn.edu.tlu.dms.billing.api.UtilityRateDtos;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;

/** Tra cứu và lưu phiên bản đơn giá điện nước; logic nằm trong Service Impl. */
public interface UtilityRateService {
    /** Tìm theo từ khóa/loại đồng hồ và phân trang, sắp xếp tại CSDL theo cột được phép. */
    PageResponse<UtilityRateDtos.Response> searchByPage(SearchRequest input);

    /**
     * ID trống: kiểm đủ dữ liệu bắt buộc và tạo phiên bản giá. Có ID: kiểm quyền sửa, người tạo và
     * ngày hiệu lực, chỉ cập nhật trường được gửi; không sửa giá đã hiệu lực.
     */
    UtilityRateDtos.Response saveOrUpdate(UtilityRateDtos.Save input);
}

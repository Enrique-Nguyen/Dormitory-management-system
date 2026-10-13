package vn.edu.tlu.dms.billing.service;

import vn.edu.tlu.dms.billing.api.UtilityRateDtos;

/** Khai báo thao tác sửa, xóa đơn giá tương lai; đơn giá đã hiệu lực phải được giữ lại. */
public interface UtilityRateCrudService {

    /**
     * Sửa phiên bản đơn giá do cán bộ đang đăng nhập tạo và chưa đến ngày hiệu lực.
     *
     * @param id mã định danh phiên bản đơn giá
     * @param input loại đồng hồ, đơn giá và ngày hiệu lực mới
     * @return thông tin đơn giá sau khi cập nhật
     */
    Object updateRate(Long id, UtilityRateDtos.Create input);

    /**
     * Xóa phiên bản đơn giá do cán bộ đang đăng nhập tạo nếu phiên bản chưa có hiệu lực.
     *
     * @param id mã định danh phiên bản đơn giá
     */
    void deleteRate(Long id);
}

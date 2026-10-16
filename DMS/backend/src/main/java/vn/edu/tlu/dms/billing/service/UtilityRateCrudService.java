package vn.edu.tlu.dms.billing.service;

/** Khai báo thao tác xóa đơn giá tương lai; đơn giá đã hiệu lực phải được giữ lại. */
public interface UtilityRateCrudService {

    /**
     * Xóa phiên bản đơn giá do cán bộ đang đăng nhập tạo nếu phiên bản chưa có hiệu lực.
     *
     * @param id mã định danh phiên bản đơn giá
     */
    void deleteRate(Long id);
}

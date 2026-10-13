package vn.edu.tlu.dms.billing.service;

import vn.edu.tlu.dms.billing.api.UtilityRateDtos;

import java.util.List;

/** Khai báo thao tác tra cứu và thêm phiên bản đơn giá điện, nước theo ngày hiệu lực. */
public interface UtilityRateService {

    /**
     * Lấy các phiên bản đơn giá điện, nước, sắp xếp theo ngày hiệu lực từ mới đến cũ.
     *
     * @param meterType loại đồng hồ ELECTRIC hoặc WATER; null để lấy cả hai loại
     * @return danh sách phiên bản đơn giá phù hợp
     */
    List<UtilityRateDtos.Response> getUtilityRates(String meterType);

    /**
     * Thêm một phiên bản đơn giá mới và lưu người tạo; giữ nguyên các phiên bản đã có.
     *
     * @param input loại đồng hồ, đơn giá và ngày bắt đầu hiệu lực
     * @return phiên bản đơn giá vừa tạo
     */
    UtilityRateDtos.Response createUtilityRate(UtilityRateDtos.Create input);
}

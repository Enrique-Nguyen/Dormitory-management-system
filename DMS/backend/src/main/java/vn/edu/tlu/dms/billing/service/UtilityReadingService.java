package vn.edu.tlu.dms.billing.service;

import vn.edu.tlu.dms.billing.api.UtilityReadingController.Input;

import java.time.LocalDate;
import java.util.Map;

/** Khai báo CRUD chỉ số điện, nước theo phòng và tháng trong phạm vi đơn vị quản lý. */
public interface UtilityReadingService {

    /**
     * Tìm kiếm và phân trang chỉ số điện, nước của các phòng mà cán bộ được phép xem.
     *
     * @param page số trang, bắt đầu từ 0
     * @param size số bản ghi mỗi trang
     * @param q từ khóa theo mã phòng; chuỗi rỗng để không lọc theo từ khóa
     * @param month tháng cần lọc; null để lấy mọi tháng
     * @return trang chỉ số phù hợp, kèm thông tin đã được sử dụng trong hóa đơn hay chưa
     */
    Object searchReadings(int page, int size, String q, LocalDate month);

    /**
     * Lấy chi tiết một bản ghi chỉ số và các thao tác được phép sau khi kiểm tra quyền xem.
     *
     * @param id mã định danh bản ghi chỉ số
     * @return chi tiết chỉ số và thông tin quyền sửa, xóa
     */
    Map<String, Object> getReading(Long id);

    /**
     * Thêm hoặc sửa chỉ số trong đơn vị của cán bộ; cơ sở dữ liệu tính lượng tiêu thụ. Khi sửa, giữ
     * nguyên phòng, loại đồng hồ và tháng; từ chối dữ liệu đã lập hóa đơn hoặc có tháng sau.
     *
     * @param id mã định danh bản ghi cần sửa; null để tạo bản ghi mới
     * @param input thông tin phòng, tháng, loại đồng hồ và các chỉ số
     * @return bản ghi chỉ số sau khi lưu
     */
    Object saveReading(Long id, Input input);

    /**
     * Xóa chỉ số chưa lập hóa đơn và chưa có chỉ số tháng tiếp theo, đồng thời ghi nhật ký thao
     * tác.
     *
     * @param id mã định danh bản ghi chỉ số
     */
    void deleteUnusedReading(Long id);
}

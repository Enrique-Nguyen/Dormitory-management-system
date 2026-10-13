package vn.edu.tlu.dms.registration.service;

import vn.edu.tlu.dms.registration.api.RegistrationPeriodDtos.Input;
import vn.edu.tlu.dms.registration.api.RegistrationPeriodDtos.Response;

import java.util.List;

/**
 * Khai báo các thao tác quản lý đợt đăng ký; phần xử lý nằm trong RegistrationPeriodServiceImpl.
 */
public interface RegistrationPeriodService {

    /**
     * Lấy các đợt đăng ký theo học kỳ, loại đợt và điều kiện đang mở. Chỉ trả các đợt thuộc phạm vi
     * người dùng được phép xem.
     *
     * @param semesterId mã học kỳ; null để không lọc học kỳ
     * @param kind loại đợt; null để không lọc loại
     * @param open điều kiện đang mở; null để không lọc thời gian mở
     * @return danh sách đợt đăng ký phù hợp
     */
    List<Response> getRegistrationPeriods(Long semesterId, String kind, Boolean open);

    /** Lấy chi tiết đợt đăng ký theo mã, sau khi kiểm tra quyền và phạm vi dữ liệu. */
    Response getRegistrationPeriodById(Long id);

    /** Tạo đợt đăng ký, đơn vị áp dụng và chỉ tiêu trong cùng một giao dịch. */
    Response createRegistrationPeriod(Input input);

    /**
     * Cập nhật đợt đăng ký được phép quản lý. Đợt đã mở chỉ được gia hạn thời điểm đóng; khóa bản
     * ghi để tránh ghi đè đồng thời.
     */
    Response updateRegistrationPeriod(Long id, Input input);

    /** Xóa đợt chưa mở và chưa có đơn đăng ký, cùng các chỉ tiêu và đơn vị áp dụng của đợt. */
    void deleteUnusedPeriod(Long id);

    /** Lấy những đợt đang mở mà sinh viên đăng nhập có thể lựa chọn để lập đơn. */
    List<Response> getOpenPeriodsForStudent();
}

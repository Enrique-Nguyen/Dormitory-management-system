package vn.edu.tlu.dms.registration.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.registration.api.RegistrationPeriodDtos.Input;
import vn.edu.tlu.dms.registration.api.RegistrationPeriodDtos.Response;

/** Hợp đồng quản lý đợt đăng ký; logic nằm trong RegistrationPeriodServiceImpl. */
public interface RegistrationPeriodService {

    /**
     * Tìm kiếm/phân trang đợt theo từ khóa, học kỳ, loại, thời gian và phạm vi được phép.
     * openForStudent do server chỉ định: true luôn chỉ lấy đợt đang mở cho form sinh viên.
     */
    PageResponse<Response> searchByPage(SearchRequest search, boolean openForStudent);

    /** Đọc chi tiết đợt theo ID sau khi kiểm tra quyền và phạm vi. */
    Response getRegistrationPeriodById(Long id);

    /**
     * ID trống tạo đợt và các đơn vị/chỉ tiêu, có ID cập nhật bản ghi đã tồn tại. Kiểm tra
     * CREATE/UPDATE riêng; đợt đã mở chỉ được gia hạn ngày đóng, ghi cùng transaction.
     */
    Response saveOrUpdate(Input input);

    /** Xóa đợt chưa mở/chưa có đơn cùng các chỉ tiêu và đơn vị áp dụng. */
    void deleteUnusedPeriod(Long id);
}

package vn.edu.tlu.dms.registration.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Input;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Response;

import java.util.List;

/** Khai báo thao tác với đơn đăng ký phòng; phần xử lý nằm trong RoomApplicationServiceImpl. */
public interface RoomApplicationService {

    /**
     * Tìm và phân trang đơn theo đợt, trạng thái và cách sắp xếp, trong phạm vi được phép xem. mine
     * bằng true chỉ lấy đơn của sinh viên đăng nhập; phiên bản này không lọc từ khóa.
     */
    PageResponse<Response> searchApplications(
            int page,
            int size,
            List<String> sort,
            Long periodId,
            List<String> statuses,
            boolean mine);

    /**
     * Tìm và phân trang đơn theo từ khóa, đợt, trạng thái và cách sắp xếp. Từ khóa áp dụng cho
     * thông tin sinh viên; mine bằng true chỉ lấy đơn của người đăng nhập.
     */
    PageResponse<Response> searchApplications(
            int page,
            int size,
            List<String> sort,
            Long periodId,
            List<String> statuses,
            boolean mine,
            String keyword);

    /** Lấy chi tiết đơn theo mã; chỉ cho phép chủ đơn hoặc cán bộ có phạm vi dữ liệu phù hợp. */
    Response getApplicationById(Long id);

    /**
     * Tạo đơn nháp cho sinh viên đăng nhập và ghi lịch sử tạo đơn trong cùng giao dịch. Mã sinh
     * viên, trạng thái và điểm ưu tiên do hệ thống xác định để tránh giả mạo.
     */
    Response createApplication(Input input);

    /** Sửa thông tin đơn còn ở trạng thái nháp và thuộc sinh viên đăng nhập. */
    Response updateDraftApplication(Long id, Input input);

    /**
     * Thực hiện thao tác chuyển trạng thái đơn theo quyền và quy tắc hiện có. Khóa đơn rồi ghi
     * trạng thái và lịch sử trong cùng giao dịch.
     *
     * @param id mã đơn đăng ký
     * @param action tên thao tác, như submit, approve, reject hoặc cancel
     * @param reason lý do thao tác khi quy tắc yêu cầu
     * @return thông tin đơn sau khi thực hiện thao tác
     */
    Response performApplicationAction(Long id, String action, String reason);
}

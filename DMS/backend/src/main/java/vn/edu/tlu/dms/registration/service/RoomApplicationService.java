package vn.edu.tlu.dms.registration.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Input;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Response;

/** Hợp đồng đơn đăng ký phòng; logic nằm trong RoomApplicationServiceImpl. */
public interface RoomApplicationService {

    /**
     * Tìm kiếm/phân trang theo đợt, trạng thái, từ khóa và sắp xếp sau khi lọc phạm vi. mine do
     * Controller chỉ định, không nhận từ JSON; true chỉ lấy đơn của người đăng nhập.
     */
    PageResponse<Response> searchByPage(SearchRequest search, boolean mine);

    /** Đọc chi tiết đơn; ngoài chủ sở hữu/phạm vi được cấp trả NOT_FOUND. */
    Response getApplicationById(Long id);

    /**
     * Lưu đơn DRAFT: ID trống tạo, ID dương sửa; quyền CREATE/UPDATE kiểm tra riêng. ADMIN được
     * chọn sinh viên thật khi tạo hộ; sửa không đổi chủ đơn hoặc đợt nguồn.
     */
    Response saveOrUpdate(Input input);

    /** Khóa đơn, kiểm tra Policy, đổi trạng thái và ghi lịch sử trong cùng transaction. */
    Response performApplicationAction(Long id, String action, String reason);
}

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

    /**
     * Đọc chi tiết đơn; ngoài chủ sở hữu/phạm vi được cấp trả NOT_FOUND. Phản hồi gồm {@code hold}
     * khi đơn ASSIGNED (giường lồng {@code hold.bed} theo API Spec 2.8 và các trường phẳng
     * {@code hold.bedId}, {@code hold.bedCode} giữ nguyên cho màn danh sách) cùng diện ưu tiên có
     * {@code verifiedBy} là ID người xác nhận và {@code verifiedByName} là họ tên tương ứng.
     */
    Response getApplicationById(Long id);

    /**
     * Lưu đơn DRAFT: ID trống tạo, ID dương sửa; quyền CREATE/UPDATE kiểm tra riêng. ADMIN được
     * chọn sinh viên thật khi tạo hộ; sửa không đổi chủ đơn hoặc đợt nguồn.
     */
    Response saveOrUpdate(Input input);

    /** Khóa đơn, kiểm tra Policy, đổi trạng thái và ghi lịch sử trong cùng transaction. */
    Response performApplicationAction(Long id, String action, String reason);

    /**
     * Phân giường cho đơn APPROVED trong một transaction: giữ chỗ HOLD, tạo hợp đồng PENDING theo
     * học kỳ của đợt và chuyển đơn sang ASSIGNED. Khóa đơn trước, sau đó module room khóa giường
     * rồi hồ sơ sinh viên; lỗi bất kỳ bước nào đều rollback toàn bộ. Đơn ngoài phạm vi trả
     * NOT_FOUND; giường đã có người, sinh viên đã có chỗ hoặc sai giới tính do module room báo.
     *
     * @param id mã đơn đăng ký cần phân giường
     * @param bedId giường đích trong đơn vị của người thao tác
     * @return chi tiết đơn kèm giường đang giữ, hợp đồng và hạn kích hoạt
     */
    Response assignBed(Long id, Long bedId);

    /**
     * Hủy giữ chỗ để phân lại: đóng phân phòng HOLD, hủy hợp đồng PENDING và đưa đơn ASSIGNED về
     * APPROVED. Chỉ áp dụng khi hợp đồng còn PENDING và chưa kích hoạt.
     *
     * @param id mã đơn đăng ký đang ASSIGNED
     * @param reason lý do hủy giữ chỗ, bắt buộc
     * @return chi tiết đơn sau khi trả về APPROVED
     */
    Response releaseHold(Long id, String reason);

    /**
     * Staff xác nhận một diện ưu tiên của đơn SUBMITTED, ghi người/thời điểm xác nhận, tính lại
     * điểm ưu tiên (tổng trọng số các diện chưa bị xác nhận "không đúng") và ghi nhật ký thao tác.
     * Ghi chú của người xác nhận chỉ lưu trong audit_log vì application_priority không có cột ghi
     * chú, nên phản hồi chi tiết không trả lại note theo từng diện.
     *
     * @param id mã đơn đăng ký
     * @param categoryId diện ưu tiên đã khai trên đơn
     * @param verified true nếu diện đúng, false nếu không đúng
     * @param note ghi chú của người xác nhận, có thể null
     * @return chi tiết đơn với điểm ưu tiên mới
     */
    Response verifyPriority(Long id, Long categoryId, Boolean verified, String note);
}

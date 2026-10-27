package vn.edu.tlu.dms.maintenance.service;

import org.springframework.web.multipart.MultipartFile;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.ChargeableStudent;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Conclude;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Response;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Save;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Khai báo thao tác với yêu cầu sửa chữa; phần xử lý nằm trong MaintenanceRequestServiceImpl. */
public interface MaintenanceRequestService {

    /**
     * Tìm kiếm và phân trang yêu cầu sửa chữa theo yêu cầu tìm kiếm chuẩn hóa.
     *
     * @param request trang, kích thước, từ khóa, danh sách trạng thái, phân loại, mức độ và thứ tự
     * @return trang kết quả yêu cầu sửa chữa
     */
    PageResponse<Response> searchByPage(SearchRequest request);

    /**
     * Thêm mới hoặc cập nhật yêu cầu sửa chữa theo mã định danh (id).
     *
     * @param input khi không có id phải đủ loại/mức độ/mô tả/vị trí; có id chỉ sửa trường đã gửi
     * @param files ảnh khi tạo mới; khi sửa giữ ảnh hiện có và từ chối gửi thêm ảnh mới
     * @return thông tin yêu cầu sửa chữa sau khi lưu
     */
    Response saveOrUpdate(Save input, List<MultipartFile> files);

    /** Lấy chi tiết yêu cầu sửa chữa theo mã sau khi kiểm tra quyền xem. */
    Response getMaintenanceRequestById(Long id);

    /** Giao yêu cầu cho kỹ thuật viên hợp lệ cùng đơn vị và ghi lịch sử phân công. */
    Response assignTechnician(Long id, Long technicianId);

    /**
     * Thực hiện thao tác với yêu cầu theo quyền và quy tắc chuyển trạng thái hiện có.
     *
     * @param id mã yêu cầu sửa chữa
     * @param action tên thao tác, như reject, cancel, start, resolve, accept hoặc reopen
     * @param reason nội dung ghi chú hoặc lý do khi thao tác yêu cầu
     * @return thông tin yêu cầu sau thao tác
     */
    Response performMaintenanceAction(Long id, String action, String reason);

    /**
     * Lấy sinh viên đang ở trong phòng tại thời điểm tạo ticket để chọn người chịu trừ cọc.
     *
     * @param id mã yêu cầu sửa chữa của phòng, đã CLOSED và chưa kết luận
     * @return danh sách ứng viên kèm hợp đồng và trạng thái hợp đồng
     */
    List<ChargeableStudent> findChargeableStudents(Long id);

    /**
     * Kết luận nguyên nhân và chi phí cho ticket của phòng đã CLOSED; ticket hư hỏng do sử dụng
     * được trừ cọc của các sinh viên được chọn trong cùng transaction.
     *
     * @param id mã yêu cầu sửa chữa
     * @param input nguyên nhân, chi phí, danh sách sinh viên chịu trách nhiệm và ghi chú
     * @return chi tiết ticket kèm các khoản trừ cọc vừa ghi
     */
    Response conclude(Long id, Conclude input);

    /** Kiểm tra quyền xem yêu cầu rồi trả đường dẫn của ảnh thuộc đúng yêu cầu đó. */
    Path getAuthorizedPhotoFile(Long id, Long photoId);

    /** Đếm ticket theo trạng thái và đơn vị quản lý để cung cấp cho Dashboard. */
    Map<String, Long> countByStatus(Long managementUnitId);
}

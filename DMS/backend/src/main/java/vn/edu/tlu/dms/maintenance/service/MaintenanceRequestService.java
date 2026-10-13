package vn.edu.tlu.dms.maintenance.service;

import org.springframework.web.multipart.MultipartFile;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Create;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Response;

import java.nio.file.Path;
import java.util.List;

/** Khai báo thao tác với yêu cầu sửa chữa; phần xử lý nằm trong MaintenanceRequestServiceImpl. */
public interface MaintenanceRequestService {

    /**
     * Tìm và phân trang yêu cầu theo trạng thái, loại và mức độ trong phạm vi được phép xem. Phiên
     * bản này không lọc từ khóa.
     */
    PageResponse<Response> searchMaintenanceRequests(
            int page, int size, String status, String category, String severity);

    /**
     * Tìm và phân trang yêu cầu theo từ khóa, trạng thái, loại và mức độ. Quyền xem của sinh viên,
     * cán bộ và kỹ thuật viên được áp dụng trước khi phân trang.
     */
    PageResponse<Response> searchMaintenanceRequests(
            int page, int size, String status, String category, String severity, String q);

    /** Lấy chi tiết yêu cầu sửa chữa theo mã sau khi kiểm tra quyền xem. */
    Response getMaintenanceRequestById(Long id);

    /**
     * Tạo yêu cầu sửa chữa cùng ảnh và lịch sử trong một giao dịch. Lỗi kiểm tra ảnh hoặc lỗi lưu
     * dữ liệu làm hoàn tác toàn bộ thao tác.
     */
    Response createMaintenanceRequest(Create input, List<MultipartFile> files);

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

    /** Kiểm tra quyền xem yêu cầu rồi trả đường dẫn của ảnh thuộc đúng yêu cầu đó. */
    Path getAuthorizedPhotoFile(Long id, Long photoId);
}

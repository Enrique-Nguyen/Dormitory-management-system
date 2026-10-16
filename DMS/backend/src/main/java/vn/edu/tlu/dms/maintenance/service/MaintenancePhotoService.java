package vn.edu.tlu.dms.maintenance.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Khai báo thao tác lưu và đọc ảnh yêu cầu sửa chữa; xử lý nằm trong MaintenancePhotoServiceImpl.
 */
public interface MaintenancePhotoService {

    /**
     * Kiểm tra nội dung, dung lượng và số lượng ảnh rồi lưu ảnh cho yêu cầu sửa chữa. Tên tệp do
     * máy chủ sinh; tệp được dọn khi giao dịch bị hoàn tác.
     */
    void savePhotos(Long requestId, List<MultipartFile> photos);

    /**
     * Lấy đường dẫn tệp ảnh có mã photoId thuộc yêu cầu requestId. Bên gọi phải kiểm tra quyền xem
     * yêu cầu trước khi phục vụ tệp cho người dùng.
     */
    Path getPhotoFile(Long requestId, Long photoId);
}

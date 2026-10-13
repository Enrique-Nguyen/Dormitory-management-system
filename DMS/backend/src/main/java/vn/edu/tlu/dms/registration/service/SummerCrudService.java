package vn.edu.tlu.dms.registration.service;

import java.util.Map;

/** Khai báo CRUD lựa chọn hè đang chờ; phần xử lý nằm trong SummerCrudServiceImpl. */
public interface SummerCrudService {

    /** Tìm lựa chọn hè theo từ khóa và phân trang trong phạm vi dữ liệu được phép xem. */
    Object searchChoices(int page, int size, String q);

    /** Lấy chi tiết lựa chọn hè theo mã sau khi kiểm tra chủ sở hữu hoặc phạm vi của cán bộ. */
    Object getChoice(Long id);

    /** Tạo lựa chọn hè đang chờ cho sinh viên đăng nhập; chỉ nhận các trường được cho phép. */
    Object createChoice(Map<String, Object> input);

    /** Cập nhật lựa chọn hè còn đang chờ và thuộc sinh viên đăng nhập. */
    Object updateChoice(Long id, Map<String, Object> input);

    /** Xóa lựa chọn hè còn đang chờ của sinh viên đăng nhập; chặn dữ liệu đã được xử lý. */
    void deleteChoice(Long id);
}

package vn.edu.tlu.dms.registration.service;

import java.util.Map;

/** Khai báo CRUD lựa chọn hè đang chờ; phần xử lý nằm trong SummerCrudServiceImpl. */
public interface SummerCrudService {

    /** Tìm lựa chọn hè theo từ khóa và phân trang trong phạm vi dữ liệu được phép xem. */
    Object searchChoices(int page, int size, String q);

    /** Lấy chi tiết lựa chọn hè theo mã sau khi kiểm tra chủ sở hữu hoặc phạm vi của cán bộ. */
    Object getChoice(Long id);

    /** Tạo lựa chọn hè đang chờ cho hợp đồng của mình; ADMIN được tạo hộ ở mọi đơn vị. */
    Object createChoice(Map<String, Object> input);

    /** Sửa lựa chọn hè còn đang chờ của mình; ADMIN được sửa hộ ở mọi đơn vị. */
    Object updateChoice(Long id, Map<String, Object> input);

    /** Xóa lựa chọn hè đang chờ của mình hoặc bởi ADMIN; chặn dữ liệu đã được xử lý. */
    void deleteChoice(Long id);
}

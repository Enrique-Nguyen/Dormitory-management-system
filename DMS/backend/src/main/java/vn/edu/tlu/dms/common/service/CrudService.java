package vn.edu.tlu.dms.common.service;

import java.util.List;

public interface CrudService<I, R> {

    /** Đọc danh mục nhỏ; service cụ thể phải kiểm tra vai trò và phạm vi dữ liệu. */
    List<R> getAll();

    /** Tra cứu theo khóa chính; không tồn tại hoặc ngoài phạm vi thì trả NOT_FOUND. */
    R getById(Long id);

    /** Tạo mới; kiểm tra trường bắt buộc trong Service và lưu trong một transaction. */
    R create(I input);

    /** Cập nhật từng phần: trường không gửi giữ nguyên, không dùng để đổi trạng thái nghiệp vụ. */
    R update(Long id, I input);

    /** Chỉ xóa danh mục chưa bị tham chiếu; ràng buộc FK bảo vệ dữ liệu đã được dùng. */
    void deleteById(Long id);
}

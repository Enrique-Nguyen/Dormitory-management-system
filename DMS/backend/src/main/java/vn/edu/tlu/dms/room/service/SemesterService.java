package vn.edu.tlu.dms.room.service;

import vn.edu.tlu.dms.common.service.CrudService;
import vn.edu.tlu.dms.room.api.SemesterRequest;
import vn.edu.tlu.dms.room.api.SemesterResponse;

import java.util.List;

/** Khai báo thao tác CRUD học kỳ; logic xử lý nằm trong SemesterServiceImpl. */
public interface SemesterService extends CrudService<SemesterRequest, SemesterResponse> {

    /** Đọc danh sách học kỳ sau khi kiểm tra vai trò và phạm vi được cấp. */
    @Override
    List<SemesterResponse> getAll();

    /** Đọc chi tiết học kỳ theo ID; không tồn tại hoặc ngoài phạm vi trả NOT_FOUND. */
    @Override
    SemesterResponse getById(Long id);

    /**
     * Tạo học kỳ, kiểm tra trường bắt buộc và ràng buộc; ghi dữ liệu và audit trong một
     * transaction.
     */
    @Override
    SemesterResponse create(SemesterRequest input);

    /** Sửa từng phần học kỳ; trường không gửi hoặc null giữ nguyên giá trị hiện tại. */
    @Override
    SemesterResponse update(Long id, SemesterRequest input);

    /** Xóa học kỳ chưa được sử dụng; ràng buộc FK ngăn xóa dữ liệu đang bị tham chiếu. */
    @Override
    void deleteById(Long id);
}

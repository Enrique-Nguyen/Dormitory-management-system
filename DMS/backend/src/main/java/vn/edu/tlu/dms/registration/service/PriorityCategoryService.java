package vn.edu.tlu.dms.registration.service;

import vn.edu.tlu.dms.common.service.CrudService;
import vn.edu.tlu.dms.registration.api.PriorityCategoryRequest;
import vn.edu.tlu.dms.registration.api.PriorityCategoryResponse;

import java.util.List;

/** Khai báo CRUD danh mục ưu tiên; phần xử lý nằm trong PriorityCategoryServiceImpl. */
public interface PriorityCategoryService
        extends CrudService<PriorityCategoryRequest, PriorityCategoryResponse> {

    /**
     * Lấy danh mục ưu tiên; sinh viên và kỹ thuật viên chỉ thấy mục đang hoạt động, không thấy
     * trọng số.
     */
    @Override
    List<PriorityCategoryResponse> getAll();

    /** Lấy chi tiết diện ưu tiên theo mã sau khi kiểm tra quyền đọc danh mục. */
    @Override
    PriorityCategoryResponse getById(Long id);

    /** Quản trị viên tạo diện ưu tiên với mã, tên, trọng số và trạng thái hoạt động. */
    @Override
    PriorityCategoryResponse create(PriorityCategoryRequest input);

    /** Quản trị viên sửa các trường được gửi của diện ưu tiên; trường bỏ qua giữ nguyên. */
    @Override
    PriorityCategoryResponse update(Long id, PriorityCategoryRequest input);

    /** Quản trị viên xóa diện ưu tiên chưa bị dữ liệu khác tham chiếu. */
    @Override
    void deleteById(Long id);
}

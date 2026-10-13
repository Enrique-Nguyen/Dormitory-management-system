package vn.edu.tlu.dms.account.service;

import vn.edu.tlu.dms.account.api.ManagementUnitRequest;
import vn.edu.tlu.dms.account.api.ManagementUnitResponse;
import vn.edu.tlu.dms.common.service.CrudService;

import java.util.List;

/** Khai báo thao tác CRUD đơn vị quản lý; logic xử lý nằm trong ManagementUnitServiceImpl. */
public interface ManagementUnitService
        extends CrudService<ManagementUnitRequest, ManagementUnitResponse> {

    /** Đọc danh sách đơn vị quản lý sau khi kiểm tra vai trò và phạm vi được cấp. */
    @Override
    List<ManagementUnitResponse> getAll();

    /** Đọc chi tiết đơn vị quản lý theo ID; không tồn tại hoặc ngoài phạm vi trả NOT_FOUND. */
    @Override
    ManagementUnitResponse getById(Long id);

    /**
     * Tạo đơn vị quản lý, kiểm tra trường bắt buộc và ràng buộc; ghi dữ liệu và audit trong một
     * transaction.
     */
    @Override
    ManagementUnitResponse create(ManagementUnitRequest input);

    /** Sửa từng phần đơn vị quản lý; trường không gửi hoặc null giữ nguyên giá trị hiện tại. */
    @Override
    ManagementUnitResponse update(Long id, ManagementUnitRequest input);

    /** Xóa đơn vị quản lý chưa được sử dụng; ràng buộc FK ngăn xóa dữ liệu đang bị tham chiếu. */
    @Override
    void deleteById(Long id);
}

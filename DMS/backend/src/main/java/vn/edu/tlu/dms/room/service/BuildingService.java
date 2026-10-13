package vn.edu.tlu.dms.room.service;

import vn.edu.tlu.dms.common.service.CrudService;
import vn.edu.tlu.dms.room.api.BuildingRequest;
import vn.edu.tlu.dms.room.api.BuildingResponse;

import java.util.List;

/** Khai báo thao tác CRUD tòa nhà; logic xử lý nằm trong BuildingServiceImpl. */
public interface BuildingService extends CrudService<BuildingRequest, BuildingResponse> {

    /** Đọc danh sách tòa nhà sau khi kiểm tra vai trò và phạm vi được cấp. */
    @Override
    List<BuildingResponse> getAll();

    /** Đọc chi tiết tòa nhà theo ID; không tồn tại hoặc ngoài phạm vi trả NOT_FOUND. */
    @Override
    BuildingResponse getById(Long id);

    /**
     * Tạo tòa nhà, kiểm tra trường bắt buộc và ràng buộc; ghi dữ liệu và audit trong một
     * transaction.
     */
    @Override
    BuildingResponse create(BuildingRequest input);

    /** Sửa từng phần tòa nhà; trường không gửi hoặc null giữ nguyên giá trị hiện tại. */
    @Override
    BuildingResponse update(Long id, BuildingRequest input);

    /** Xóa tòa nhà chưa được sử dụng; ràng buộc FK ngăn xóa dữ liệu đang bị tham chiếu. */
    @Override
    void deleteById(Long id);
}

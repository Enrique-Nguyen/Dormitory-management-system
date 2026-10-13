package vn.edu.tlu.dms.room.service;

import vn.edu.tlu.dms.common.service.CrudService;
import vn.edu.tlu.dms.room.api.RoomTypeRequest;
import vn.edu.tlu.dms.room.api.RoomTypeResponse;

import java.util.List;

/** Khai báo thao tác CRUD loại phòng; logic xử lý nằm trong RoomTypeServiceImpl. */
public interface RoomTypeService extends CrudService<RoomTypeRequest, RoomTypeResponse> {

    /** Đọc danh sách loại phòng sau khi kiểm tra vai trò và phạm vi được cấp. */
    @Override
    List<RoomTypeResponse> getAll();

    /** Đọc chi tiết loại phòng theo ID; không tồn tại hoặc ngoài phạm vi trả NOT_FOUND. */
    @Override
    RoomTypeResponse getById(Long id);

    /**
     * Tạo loại phòng, kiểm tra trường bắt buộc và ràng buộc; ghi dữ liệu và audit trong một
     * transaction.
     */
    @Override
    RoomTypeResponse create(RoomTypeRequest input);

    /** Sửa từng phần loại phòng; trường không gửi hoặc null giữ nguyên giá trị hiện tại. */
    @Override
    RoomTypeResponse update(Long id, RoomTypeRequest input);

    /** Xóa loại phòng chưa được sử dụng; ràng buộc FK ngăn xóa dữ liệu đang bị tham chiếu. */
    @Override
    void deleteById(Long id);
}

package vn.edu.tlu.dms.room.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.service.CrudService;
import vn.edu.tlu.dms.room.api.RoomTypeRequest;
import vn.edu.tlu.dms.room.api.RoomTypeResponse;

import java.util.List;

/** Hợp đồng CRUD loại phòng; logic kiểm tra quyền và dữ liệu nằm trong RoomTypeServiceImpl. */
public interface RoomTypeService extends CrudService<RoomTypeRequest, RoomTypeResponse> {

    /** Tìm kiếm, sắp xếp và phân trang loại phòng trong phạm vi được phép; trang bắt đầu từ 0. */
    @Override
    PageResponse<RoomTypeResponse> searchByPage(SearchRequest search);

    /** Đọc danh mục cho lời gọi nội bộ; API danh sách sử dụng searchByPage để phân trang. */
    @Override
    List<RoomTypeResponse> getAll();

    /** Đọc chi tiết theo ID; trả NOT_FOUND nếu không tồn tại hoặc nằm ngoài phạm vi truy cập. */
    @Override
    RoomTypeResponse getById(Long id);

    /**
     * Lưu loại phòng: ID trống tạo mới, có ID cập nhật từng phần và giữ trường không gửi. Kiểm tra
     * đúng quyền CREATE/UPDATE và ghi nhật ký cùng transaction; ID không tồn tại không tạo mới.
     */
    @Override
    RoomTypeResponse saveOrUpdate(RoomTypeRequest input);

    /** Xóa loại phòng chưa được sử dụng; ràng buộc tham chiếu tiếp tục bảo vệ dữ liệu lịch sử. */
    @Override
    void deleteById(Long id);
}

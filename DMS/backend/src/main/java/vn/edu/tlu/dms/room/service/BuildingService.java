package vn.edu.tlu.dms.room.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.service.CrudService;
import vn.edu.tlu.dms.room.api.BuildingRequest;
import vn.edu.tlu.dms.room.api.BuildingResponse;

import java.util.List;

/** Hợp đồng CRUD tòa nhà; logic kiểm tra quyền và dữ liệu nằm trong BuildingServiceImpl. */
public interface BuildingService extends CrudService<BuildingRequest, BuildingResponse> {

    /** Tìm kiếm, sắp xếp và phân trang tòa nhà trong phạm vi được phép; trang bắt đầu từ 0. */
    @Override
    PageResponse<BuildingResponse> searchByPage(SearchRequest search);

    /** Đọc danh mục cho lời gọi nội bộ; API danh sách sử dụng searchByPage để phân trang. */
    @Override
    List<BuildingResponse> getAll();

    /** Đọc chi tiết theo ID; trả NOT_FOUND nếu không tồn tại hoặc nằm ngoài phạm vi truy cập. */
    @Override
    BuildingResponse getById(Long id);

    /**
     * Lưu tòa nhà: ID trống tạo mới, có ID cập nhật từng phần và giữ trường không gửi. Kiểm tra
     * đúng quyền CREATE/UPDATE và ghi nhật ký cùng transaction; ID không tồn tại không tạo mới.
     */
    @Override
    BuildingResponse saveOrUpdate(BuildingRequest input);

    /** Xóa tòa nhà chưa được sử dụng; ràng buộc tham chiếu tiếp tục bảo vệ dữ liệu lịch sử. */
    @Override
    void deleteById(Long id);
}

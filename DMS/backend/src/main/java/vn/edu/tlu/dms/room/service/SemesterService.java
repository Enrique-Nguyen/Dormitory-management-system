package vn.edu.tlu.dms.room.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.service.CrudService;
import vn.edu.tlu.dms.room.api.SemesterRequest;
import vn.edu.tlu.dms.room.api.SemesterResponse;

import java.util.List;

/** Hợp đồng CRUD học kỳ; logic kiểm tra quyền và dữ liệu nằm trong SemesterServiceImpl. */
public interface SemesterService extends CrudService<SemesterRequest, SemesterResponse> {

    /** Tìm kiếm, sắp xếp và phân trang học kỳ trong phạm vi được phép; trang bắt đầu từ 0. */
    @Override
    PageResponse<SemesterResponse> searchByPage(SearchRequest search);

    /** Đọc danh mục cho lời gọi nội bộ; API danh sách sử dụng searchByPage để phân trang. */
    @Override
    List<SemesterResponse> getAll();

    /** Đọc chi tiết theo ID; trả NOT_FOUND nếu không tồn tại hoặc nằm ngoài phạm vi truy cập. */
    @Override
    SemesterResponse getById(Long id);

    /**
     * Lưu học kỳ: ID trống tạo mới, có ID cập nhật từng phần và giữ trường không gửi. Kiểm tra đúng
     * quyền CREATE/UPDATE và ghi nhật ký cùng transaction; ID không tồn tại không tạo mới.
     */
    @Override
    SemesterResponse saveOrUpdate(SemesterRequest input);

    /** Xóa học kỳ chưa được sử dụng; ràng buộc tham chiếu tiếp tục bảo vệ dữ liệu lịch sử. */
    @Override
    void deleteById(Long id);
}

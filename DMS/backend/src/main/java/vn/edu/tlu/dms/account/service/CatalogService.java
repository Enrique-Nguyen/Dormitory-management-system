package vn.edu.tlu.dms.account.service;

import vn.edu.tlu.dms.account.api.CatalogRequest;
import vn.edu.tlu.dms.account.api.CatalogSearch;
import vn.edu.tlu.dms.common.dto.PageResponse;

import java.util.Map;

public interface CatalogService {
    /** Tìm danh mục theo bộ lọc, phân trang và quyền đọc hiện tại. */
    PageResponse<Map<String, Object>> searchByPage(String kind, CatalogSearch input);

    /** Đọc chi tiết kể cả danh mục ngừng hoạt động để hiển thị dữ liệu cũ. */
    Map<String, Object> getById(String kind, Long id);

    /** Đọc tham chiếu nội bộ kể cả mục ngừng dùng; nơi gọi phải kiểm quyền và phạm vi hồ sơ. */
    Map<String, Object> getReferenceById(String kind, Long id);

    /** Tạo hoặc cập nhật từng phần, kiểm tra đúng quyền và ghi nhật ký cùng giao dịch. */
    Map<String, Object> saveOrUpdate(String kind, CatalogRequest input);

    /** Xóa danh mục chưa được sử dụng; khóa ngoại bảo vệ dữ liệu tham chiếu. */
    void delete(String kind, Long id);

    /** Kiểm tra và khóa chia sẻ danh mục đang hoạt động; ID null trả null. */
    Map<String, Object> requireActive(String kind, Long id);

    /** Giải mã import theo mã hoặc tên chính xác, không tự tạo danh mục. */
    Long resolveImport(String kind, String value);

    /** Kiểm tra mã danh mục đang hoạt động cho các quan hệ khóa tự nhiên. */
    void requireActiveCode(String kind, String code);
}

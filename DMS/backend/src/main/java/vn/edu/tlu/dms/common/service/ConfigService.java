package vn.edu.tlu.dms.common.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;

public interface ConfigService {

    /** Tìm và phân trang các khóa cấu hình theo từ khóa; chỉ Admin được truy cập. */
    PageResponse<ConfigValue> searchByPage(SearchRequest input);

    /**
     * Đọc giá trị cấu hình dạng số nguyên dương; báo lỗi nếu thiếu khóa hoặc giá trị không hợp lệ.
     */
    int positiveInt(String key);

    /** Cập nhật giá trị của khóa cấu hình đã có và ghi audit; không tạo khóa tùy ý từ client. */
    ConfigValue saveOrUpdate(String key, String value);

    public record ConfigValue(String key, String value, String description) {}
}

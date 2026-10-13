package vn.edu.tlu.dms.common.service;

import java.util.List;

public interface ConfigService {

    /** Đọc danh sách các khóa cấu hình hệ thống; chỉ Admin được truy cập. */
    List<ConfigValue> list();

    /**
     * Đọc giá trị cấu hình dạng số nguyên dương; báo lỗi nếu thiếu khóa hoặc giá trị không hợp lệ.
     */
    int positiveInt(String key);

    /** Cập nhật giá trị của khóa cấu hình đã có và ghi audit; không tạo khóa tùy ý từ client. */
    ConfigValue update(String key, String value);

    public record ConfigValue(String key, String value, String description) {}
}

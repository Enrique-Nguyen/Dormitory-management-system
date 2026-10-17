package vn.edu.tlu.dms.registration.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** ID trống để tạo mới; có ID để sửa từng phần. Trường không gửi giữ nguyên khi sửa. */
public record PriorityCategoryRequest(
        @Positive Long id,
        @Size(max = 30) String code,
        @Size(max = 100) String name,
        @PositiveOrZero Integer weight,
        Boolean active) {

    /** Tương thích lời gọi tạo mới trong mã Java; API dùng body có trường ID tùy chọn. */
    public PriorityCategoryRequest(String code, String name, Integer weight, Boolean active) {
        this(null, code, name, weight, active);
    }
}

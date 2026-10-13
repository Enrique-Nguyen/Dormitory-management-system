package vn.edu.tlu.dms.registration.api;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** PATCH giữ nguyên trường không gửi; khi tạo mới, Service kiểm tra các trường bắt buộc. */
public record PriorityCategoryRequest(
        @Size(max = 30) String code,
        @Size(max = 100) String name,
        @PositiveOrZero Integer weight,
        Boolean active) {}

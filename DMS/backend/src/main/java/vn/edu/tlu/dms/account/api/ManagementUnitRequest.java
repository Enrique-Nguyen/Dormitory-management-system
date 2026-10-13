package vn.edu.tlu.dms.account.api;

import jakarta.validation.constraints.Size;

/** PATCH giữ nguyên trường không gửi; khi tạo mới, Service kiểm tra các trường bắt buộc. */
public record ManagementUnitRequest(@Size(max = 20) String code, @Size(max = 100) String name) {}

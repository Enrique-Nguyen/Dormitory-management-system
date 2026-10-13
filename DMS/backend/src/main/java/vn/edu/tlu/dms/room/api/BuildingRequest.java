package vn.edu.tlu.dms.room.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** PATCH giữ nguyên trường không gửi; khi tạo mới, Service kiểm tra các trường bắt buộc. */
public record BuildingRequest(
        @Positive Long managementUnitId,
        @Size(max = 10) String code,
        @Size(max = 100) String name,
        @Positive Long depositAmount) {}

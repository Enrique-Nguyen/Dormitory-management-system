package vn.edu.tlu.dms.room.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** ID trống để tạo mới; có ID để sửa từng phần. Trường không gửi giữ nguyên khi sửa. */
public record BuildingRequest(
        @Positive Long id,
        @Positive Long managementUnitId,
        @Size(max = 10) String code,
        @Size(max = 100) String name,
        @Positive Long depositAmount) {

    /** Tương thích lời gọi tạo mới trong mã Java; API dùng body có trường ID tùy chọn. */
    public BuildingRequest(Long managementUnitId, String code, String name, Long depositAmount) {
        this(null, managementUnitId, code, name, depositAmount);
    }
}

package vn.edu.tlu.dms.room.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.Gender;

/**
 * ID trống để tạo mới; có ID để sửa từng phần. Tòa và số tầng không đổi sau khi tạo; chuỗi rỗng xóa
 * giới tính tầng hoặc ghi chú.
 */
public record FloorRequest(
        @Positive Long id,
        @Positive Long buildingId,
        @Min(0) Short floor,
        @Size(max = 100) String name,
        Boolean active,
        @Pattern(regexp = "^$|" + Gender.PATTERN) String gender,
        Boolean sharedBathroom,
        Boolean commonRoom,
        @Size(max = 1000) String note) {}

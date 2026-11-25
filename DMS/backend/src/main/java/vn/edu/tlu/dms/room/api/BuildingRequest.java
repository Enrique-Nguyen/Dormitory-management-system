package vn.edu.tlu.dms.room.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.Facility;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * ID trống để tạo mới; có ID để sửa từng phần. Trường không gửi hoặc null giữ nguyên khi sửa; chuỗi
 * rỗng xóa trường văn bản tùy chọn.
 */
public record BuildingRequest(
        @Positive Long id,
        @Positive Long managementUnitId,
        @Size(max = 10) String code,
        @Size(max = 100) String name,
        @Positive Long depositAmount,
        @Min(1) @Max(Facility.MAX_FLOOR_COUNT) Short floorCount,
        Boolean hasGroundFloor,
        @Pattern(regexp = Facility.GENDER_POLICY_PATTERN) String genderPolicy,
        @Size(max = 300) String address,
        @Min(Facility.MIN_COMMISSIONED_YEAR) @Max(2100) Short commissionedYear,
        @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 8, fraction = 2)
                BigDecimal floorAreaM2,
        @Positive Long managerUserId,
        @Size(max = 15) @Pattern(regexp = Facility.PHONE_PATTERN) String hotline,
        LocalTime openTime,
        LocalTime closeTime,
        @Size(max = 1000) String note) {

    /** Tương thích lời gọi tạo mới trong mã Java; API dùng body có trường ID tùy chọn. */
    public BuildingRequest(Long managementUnitId, String code, String name, Long depositAmount) {
        this(null, managementUnitId, code, name, depositAmount, (short) 1, false,
                Facility.GENDER_MIXED, null, null, null, null, null, null, null, null);
    }
}

package vn.edu.tlu.dms.room.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.Facility;

import java.math.BigDecimal;
import java.util.List;

/**
 * ID trống để tạo mới; có ID để sửa từng phần. Trường không gửi hoặc null giữ nguyên khi sửa; chuỗi
 * rỗng xóa mô tả.
 */
public record RoomTypeRequest(
        @Positive Long id,
        @Size(max = 50) String name,
        @Min(1) @Max(12) Short bedCount,
        @PositiveOrZero Long priceMain,
        @PositiveOrZero Long priceSummer,
        @DecimalMin("0.0") @Digits(integer = 4, fraction = 1) BigDecimal areaM2,
        List<@NotNull @Positive Long> amenityIds,
        Boolean active,
        @Pattern(regexp = Facility.ROOM_TYPE_CODE_PATTERN) String code,
        Boolean privateBathroom,
        Boolean hasBalcony,
        @Pattern(regexp = Facility.BED_STYLE_PATTERN) String bedStyle,
        Boolean hasKitchen,
        @Size(max = 1000) String description) {

    /** Tương thích lời gọi sửa trong mã Java trước khi có trường mô tả loại phòng. */
    public RoomTypeRequest(
            Long id,
            String name,
            Short bedCount,
            Long priceMain,
            Long priceSummer,
            BigDecimal areaM2,
            List<Long> amenityIds,
            Boolean active) {
        this(id, name, bedCount, priceMain, priceSummer, areaM2, amenityIds, active,
                null, null, null, null, null, null);
    }

    /** Tương thích lời gọi tạo mới trong mã Java; API dùng body có trường ID tùy chọn. */
    public RoomTypeRequest(
            String code,
            String name,
            Short bedCount,
            Long priceMain,
            Long priceSummer,
            BigDecimal areaM2,
            List<Long> amenityIds,
            Boolean active) {
        this(null, name, bedCount, priceMain, priceSummer, areaM2, amenityIds, active,
                code, null, null, null, null, null);
    }
}

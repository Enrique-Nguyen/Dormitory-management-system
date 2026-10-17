package vn.edu.tlu.dms.room.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** ID trống để tạo mới; có ID để sửa từng phần. Trường không gửi giữ nguyên khi sửa. */
public record RoomTypeRequest(
        @Positive Long id,
        @Size(max = 50) String name,
        @Min(1) @Max(12) Short bedCount,
        @PositiveOrZero Long priceMain,
        @PositiveOrZero Long priceSummer,
        @DecimalMin("0.0") @Digits(integer = 4, fraction = 1) BigDecimal areaM2,
        @Size(max = 500) String amenities,
        Boolean active) {

    /** Tương thích lời gọi tạo mới trong mã Java; API dùng body có trường ID tùy chọn. */
    public RoomTypeRequest(
            String name,
            Short bedCount,
            Long priceMain,
            Long priceSummer,
            BigDecimal areaM2,
            String amenities,
            Boolean active) {
        this(null, name, bedCount, priceMain, priceSummer, areaM2, amenities, active);
    }
}

package vn.edu.tlu.dms.room.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** PATCH giữ nguyên trường không gửi; khi tạo mới, Service kiểm tra các trường bắt buộc. */
public record RoomTypeRequest(
        @Size(max = 50) String name,
        @Min(1) @Max(12) Short bedCount,
        @PositiveOrZero Long priceMain,
        @PositiveOrZero Long priceSummer,
        @DecimalMin("0.0") @Digits(integer = 4, fraction = 1) BigDecimal areaM2,
        @Size(max = 500) String amenities,
        Boolean active) {}

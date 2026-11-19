package vn.edu.tlu.dms.room.api;

import java.math.BigDecimal;
import java.util.List;

/** Thông tin loại phòng; roomCount là số phòng đang dùng loại này, tính khi đọc. */
public record RoomTypeResponse(
        Long id,
        String name,
        Short bedCount,
        Long priceMain,
        Long priceSummer,
        BigDecimal areaM2,
        List<Long> amenityIds,
        String amenities,
        Boolean active,
        String code,
        Boolean privateBathroom,
        Boolean hasBalcony,
        String bedStyle,
        Boolean hasKitchen,
        String description,
        long roomCount) {}

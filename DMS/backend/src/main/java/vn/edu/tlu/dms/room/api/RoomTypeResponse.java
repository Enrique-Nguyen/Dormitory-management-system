package vn.edu.tlu.dms.room.api;

import java.math.BigDecimal;

public record RoomTypeResponse(
        Long id,
        String name,
        Short bedCount,
        Long priceMain,
        Long priceSummer,
        BigDecimal areaM2,
        String amenities,
        Boolean active) {}

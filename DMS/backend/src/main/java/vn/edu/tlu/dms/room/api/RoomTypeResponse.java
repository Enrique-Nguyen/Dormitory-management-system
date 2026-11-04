package vn.edu.tlu.dms.room.api;

import java.math.BigDecimal;
import java.util.List;

public record RoomTypeResponse(
        Long id,
        String name,
        Short bedCount,
        Long priceMain,
        Long priceSummer,
        BigDecimal areaM2,
        List<Long> amenityIds,
        String amenities,
        Boolean active) {}

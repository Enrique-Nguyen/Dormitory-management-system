package vn.edu.tlu.dms.room.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FloorRequest(
        @Positive Long id,
        @Positive Long buildingId,
        @Min(0) Short floor,
        @Size(max = 100) String name,
        Boolean active) {}

package vn.edu.tlu.dms.room.api;

public record BuildingResponse(
        Long id, Long managementUnitId, String code, String name, Long depositAmount) {}

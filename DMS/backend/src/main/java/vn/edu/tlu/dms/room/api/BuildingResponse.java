package vn.edu.tlu.dms.room.api;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Thông tin tòa nhà; số phòng, sức chứa và số đang ở là giá trị suy ra từ v_room_occupancy, không
 * lưu trong bảng building.
 */
public record BuildingResponse(
        Long id,
        Long managementUnitId,
        String managementUnitName,
        String code,
        String name,
        Long depositAmount,
        Short floorCount,
        Boolean hasGroundFloor,
        String genderPolicy,
        String address,
        Short commissionedYear,
        BigDecimal floorAreaM2,
        Long managerUserId,
        String managerUserName,
        String hotline,
        LocalTime openTime,
        LocalTime closeTime,
        String note,
        long roomCount,
        long capacity,
        long occupied) {}

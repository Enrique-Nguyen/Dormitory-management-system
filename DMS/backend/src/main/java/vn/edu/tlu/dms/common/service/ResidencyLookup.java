package vn.edu.tlu.dms.common.service;

import java.util.Optional;

/**
 * room cung cấp dữ liệu cư trú qua interface, để account không phụ thuộc entity/repository của
 * room.
 */
public interface ResidencyLookup {

    record Residency(
            Long roomId,
            Long buildingId,
            Long unitId,
            boolean activeContract,
            boolean roomLeader) {}

    /**
     * Tra cứu chỗ ở hiệu lực của sinh viên để các module đọc thông tin cư trú qua interface; trả
     * Optional rỗng khi chưa có phân phòng đang ở hoặc giữ chỗ.
     */
    Optional<Residency> current(Long studentId);
}

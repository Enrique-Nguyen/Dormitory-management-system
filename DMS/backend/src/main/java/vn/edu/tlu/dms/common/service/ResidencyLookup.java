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

    Optional<Residency> current(Long studentId);
}

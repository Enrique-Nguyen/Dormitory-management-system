package vn.edu.tlu.dms.room.service;

import java.util.Optional;

/**
 * Truy vấn chỉ đọc về chỗ ở hiện tại do module room cung cấp cho module khác (SAD 4.2 mục 2);
 * không khóa và không thay đổi dữ liệu.
 */
public interface OccupancyQuery {

    /**
     * Tìm phân phòng đang mở của hợp đồng kèm giường, phòng, tòa và đơn vị quản lý; trả Optional
     * rỗng khi hợp đồng không còn phân phòng mở.
     */
    Optional<CurrentBed> findOpenAssignmentByContract(Long contractId);

    /** Giường hiện tại của một phân phòng đang mở; dùng hiển thị và kiểm tra phạm vi. */
    public record CurrentBed(
            Long assignmentId,
            Long studentId,
            Long contractId,
            String kind,
            Long bedId,
            String bedCode,
            Long roomId,
            String roomCode,
            Long buildingId,
            String buildingCode,
            Long managementUnitId) {}
}

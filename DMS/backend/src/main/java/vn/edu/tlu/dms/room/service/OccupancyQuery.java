package vn.edu.tlu.dms.room.service;

import java.time.OffsetDateTime;
import java.util.List;
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

    /**
     * Danh sách sinh viên đang ở trong phòng tại một thời điểm, dùng để chọn người chịu trách nhiệm
     * khi kết luận ticket. Chỉ trả ID sinh viên và hợp đồng, không lộ entity của module room.
     */
    List<Occupant> findOccupantsAt(Long roomId, OffsetDateTime at);

    /** Kiểm tra một sinh viên có phải trưởng phòng hiện tại của phòng hay không. */
    boolean isRoomLeader(Long roomId, Long studentId);

    /**
     * Toàn bộ lịch sử phân phòng của một hợp đồng, mới nhất trước, kèm vị trí giường và đơn giá đã
     * chụp; dùng cho chi tiết hợp đồng (API 4.5). Chỉ đọc, không lộ entity của module room.
     */
    List<AssignmentRow> findAssignmentsByContract(Long contractId);

    /** Đọc vị trí của một giường để chọn giường nhận lại đồ mà không lộ entity room. */
    Optional<BedLocation> findBedLocation(Long bedId);

    /** Vị trí và trạng thái tối thiểu của giường. */
    public record BedLocation(
            Long bedId,
            Long roomId,
            Long roomTypeId,
            Long managementUnitId,
            String gender,
            String roomStatus,
            String bedStatus) {}

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

    /** Người ở tại thời điểm xác định; contractId là hợp đồng gắn với phân phòng lịch sử. */
    public record Occupant(Long studentId, Long contractId) {}

    /**
     * Một dòng lịch sử phân phòng của hợp đồng: {@code bedCode} là mã giường trong phòng
     * (vd. {@code 03}), mã hiển thị đầy đủ do bên đọc ghép với {@code buildingCode} và
     * {@code roomCode}; {@code endedAt}/{@code endReason} rỗng khi phân phòng còn hiệu lực, đơn giá
     * là ảnh chụp tại thời điểm phân phòng.
     */
    public record AssignmentRow(
            Long id,
            String kind,
            Long bedId,
            String bedCode,
            Long roomId,
            String roomCode,
            Long buildingId,
            String buildingCode,
            Integer floor,
            Long managementUnitId,
            OffsetDateTime startedAt,
            OffsetDateTime stayStartedAt,
            OffsetDateTime endedAt,
            String endReason,
            Long priceMain,
            Long priceSummer) {}
}

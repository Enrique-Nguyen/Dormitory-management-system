package vn.edu.tlu.dms.room.service;

import java.util.Optional;

/**
 * Dịch vụ duy nhất chiếm, trả và chuyển giường (SAD AD-02, BR-01, BR-02, BR-05). Mọi module
 * phân phòng, hủy giữ chỗ, kích hoạt, chuyển phòng, tạm rời hè và trả phòng phải gọi qua đây để
 * cơ chế khóa và hai partial unique index chỉ được xử lý ở một nơi.
 *
 * <p>Các hàm tham gia transaction của nơi gọi; nơi gọi chịu trách nhiệm kiểm tra quyền thao tác
 * và trạng thái của hợp đồng/đơn trước khi gọi.
 */
public interface BedOccupancyService {

    /**
     * Phân giường giữ chỗ (HOLD) cho sinh viên theo hợp đồng PENDING. Khóa giường rồi khóa sinh
     * viên, kiểm tra lại giường/phòng ACTIVE, giường trống, sinh viên chưa có phân phòng hiệu lực
     * và giới tính; chụp đơn giá loại phòng vào phân phòng. Giường ngoài đơn vị của Staff trả 404.
     */
    Assignment occupyHold(Long bedId, Long studentId, Long contractId, String note);

    /**
     * Mở phân phòng đang ở (STAY) trực tiếp, dùng cho phân lại giường đầu học kỳ 1 cho sinh viên
     * tạm rời hè. Quy tắc khóa và kiểm tra giống {@link #occupyHold}.
     */
    Assignment occupyStay(Long bedId, Long studentId, Long contractId, String note);

    /**
     * Đổi phân phòng HOLD đang mở của hợp đồng sang STAY trên cùng dòng khi kích hoạt hợp đồng;
     * ghi thời điểm bắt đầu ở. Không có phân phòng HOLD mở thì trả 409 INVALID_STATE_TRANSITION.
     */
    Assignment convertHoldToStay(Long contractId, Long actorId);

    /**
     * Đóng phân phòng đang mở của hợp đồng với lý do hủy giữ chỗ, quá hạn kích hoạt, trả phòng
     * hoặc tạm rời hè; tự gỡ trưởng phòng nếu sinh viên đang là trưởng phòng (FR-ROOM-10). Trả
     * Optional rỗng khi hợp đồng không còn phân phòng mở. actorId null nghĩa là tác vụ SYSTEM.
     */
    Optional<Assignment> release(Long contractId, String endReason, Long actorId, String note);

    /**
     * Chuyển sinh viên đang ở sang giường đích cùng đơn vị (BR-05): khóa hai giường theo bed_id
     * tăng dần, kiểm tra lại, đóng phân phòng cũ (TRANSFER) và mở phân phòng STAY mới với đơn giá
     * của phòng đích trong cùng transaction; gỡ trưởng phòng ở phòng cũ nếu cần. Hợp đồng giữ
     * nguyên.
     */
    Assignment transfer(Long contractId, Long targetBedId, String note);

    /** Phân phòng sau thao tác; chỉ chứa ID và mã, không trả entity ra ngoài module room. */
    public record Assignment(
            Long id,
            Long studentId,
            Long contractId,
            Long bedId,
            String bedCode,
            Long roomId,
            String roomCode,
            String kind,
            Long priceMain,
            Long priceSummer) {}
}

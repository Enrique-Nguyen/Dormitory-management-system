package vn.edu.tlu.dms.room.service;

import java.time.LocalDate;
import java.util.Optional;

public interface RoomReferenceService {

    /**
     * Lấy ID phòng/tòa/đơn vị cùng giới tính và trạng thái; báo NOT_FOUND nếu phòng không tồn tại.
     */
    Location getRoomLocation(Long id);

    /** Lấy ID đơn vị sở hữu tòa để nơi gọi kiểm tra phạm vi quyền. */
    Long getBuildingManagementUnitId(Long id);

    /** Kiểm tra loại phòng tồn tại và đang ACTIVE trước khi tạo hoặc cập nhật hồ sơ liên quan. */
    void requireActiveRoomType(Long id);

    /**
     * Đọc thông tin học kỳ gồm loại, ngày bắt đầu và kết thúc; không trả entity ra ngoài module.
     */
    SemesterRef getSemesterReference(Long id);

    /**
     * Tìm phòng sinh viên đang ở dựa trên phân phòng hiệu lực; trả Optional rỗng khi chưa có phòng.
     */
    Optional<Long> findCurrentStayingRoomId(Long studentId);

    /** Kiểm tra sinh viên có hợp đồng PENDING hoặc ACTIVE để bảo vệ điều kiện tạo/sửa hồ sơ. */
    boolean hasPendingOrActiveContract(Long studentId);

    /**
     * Khóa hồ sơ sinh viên đến hết transaction; nơi gọi phải tuân thủ thứ tự khóa thống nhất để
     * tránh xung đột.
     */
    void lockStudentProfile(Long studentId);

    public record Location(
            Long roomId, Long buildingId, Long unitId, String gender, String status) {}

    public record SemesterRef(Long id, String type, LocalDate startDate, LocalDate endDate) {}
}

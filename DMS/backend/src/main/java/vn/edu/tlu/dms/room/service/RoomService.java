package vn.edu.tlu.dms.room.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.room.api.RoomDtos.AvailableRoom;
import vn.edu.tlu.dms.room.api.RoomDtos.BedInput;
import vn.edu.tlu.dms.room.api.RoomDtos.BedResponse;
import vn.edu.tlu.dms.room.api.RoomDtos.BedStatus;
import vn.edu.tlu.dms.room.api.RoomDtos.Input;
import vn.edu.tlu.dms.room.api.RoomDtos.Response;
import vn.edu.tlu.dms.room.api.RoomDtos.Status;

import java.util.List;

public interface RoomService {

    /** Tra cứu phòng theo trang và bộ lọc SearchRequest; SQL lọc đơn vị trước khi phân trang. */
    PageResponse<Response> searchByPage(SearchRequest request);

    /** Tra cứu phòng; SQL lọc đơn vị trước khi phân trang, số chỗ trống lấy từ view. */
    PageResponse<Response> searchRooms(
            int page,
            int size,
            List<String> sort,
            Long buildingId,
            Short floor,
            Long roomTypeId,
            String gender,
            String status,
            Boolean hasFreeBed,
            Boolean noLeader,
            String q);

    /** Đọc phòng kèm giường và người đang ở/giữ chỗ, kiểm tra quyền trên đối tượng. */
    Response getRoomById(Long id);

    /** Thêm mới khi chưa có id hoặc cập nhật từng phần phòng khi đã có id trong một transaction. */
    Response saveOrUpdate(Input input);

    /** Thêm mới giường hoặc cập nhật mã hiển thị giường trong một transaction. */
    BedResponse saveOrUpdateBed(BedInput input);

    /** Đổi trạng thái vận hành có lý do; khóa giường trước khi kiểm tra còn người ở. */
    Response changeRoomStatus(Long id, Status input);

    /** Không chuyển Hỏng/Ngừng dùng khi giường đang được ở hoặc giữ chỗ. */
    BedResponse changeBedStatus(Long id, BedStatus input);

    /** Phòng từng có phân phòng phải được lưu lịch sử; không được xóa. */
    void deleteRoom(Long id);

    /** Chỉ xóa giường chưa từng được phân cho sinh viên. */
    void deleteBed(Long id);

    /** Trưởng phòng phải có phân phòng STAY ngay trong phòng; ghi nhật ký thay đổi. */
    Response assignRoomLeader(Long id, Long studentId);

    /** Gỡ trưởng phòng với lý do; giữ bản ghi cũ trong nhật ký. */
    void removeRoomLeader(Long id, String reason);

    /**
     * Liệt kê giường trống gom theo phòng cho split view (API 4.4, UC-16, UC-18). Truyền đúng một
     * nguồn: applicationId (đơn APPROVED, lọc theo đơn vị nguyện vọng, phòng đúng loại mong muốn
     * xếp trước) hoặc transferRequestId (yêu cầu PENDING, chỉ giường cùng đơn vị với giường hiện
     * tại, BR-05). Chỉ gồm phòng/giường ACTIVE, đúng giới tính sinh viên, không có phân phòng mở;
     * buildingId, roomTypeId là bộ lọc tùy chọn. Nguồn ngoài đơn vị của Staff trả 404.
     */
    List<AvailableRoom> findAvailableBeds(
            Long applicationId, Long transferRequestId, Long buildingId, Long roomTypeId);
}

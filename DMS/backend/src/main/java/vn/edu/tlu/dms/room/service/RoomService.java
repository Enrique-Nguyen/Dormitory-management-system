package vn.edu.tlu.dms.room.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.room.api.RoomDtos.BedInput;
import vn.edu.tlu.dms.room.api.RoomDtos.BedResponse;
import vn.edu.tlu.dms.room.api.RoomDtos.BedStatus;
import vn.edu.tlu.dms.room.api.RoomDtos.Input;
import vn.edu.tlu.dms.room.api.RoomDtos.Response;
import vn.edu.tlu.dms.room.api.RoomDtos.Status;

import java.util.List;

public interface RoomService {

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

    default PageResponse<Response> searchRooms(
            int page,
            int size,
            List<String> sort,
            Long buildingId,
            Short floor,
            Long roomTypeId,
            String gender,
            String status,
            Boolean hasFreeBed,
            Boolean noLeader) {
        return searchRooms(
                page,
                size,
                sort,
                buildingId,
                floor,
                roomTypeId,
                gender,
                status,
                hasFreeBed,
                noLeader,
                "");
    }

    /** Đọc phòng kèm giường và người đang ở/giữ chỗ, kiểm tra quyền trên đối tượng. */
    Response getRoomById(Long id);

    /** Tạo phòng và toàn bộ giường theo loại phòng trong một transaction. */
    Response createRoom(Input input);

    /** Đổi giới tính/loại phòng chỉ khi không còn phân phòng hiệu lực; không đổi tòa. */
    Response updateRoom(Long id, Input input);

    /** Đổi trạng thái vận hành có lý do; khóa giường trước khi kiểm tra còn người ở. */
    Response changeRoomStatus(Long id, Status input);

    /** Thêm giường thuộc phòng của đơn vị đang quản lý. */
    BedResponse addBedToRoom(Long roomId, BedInput input);

    /** Sửa mã hiển thị của giường, giữ ID và lịch sử phân phòng. */
    BedResponse updateBedCode(Long id, BedInput input);

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
}

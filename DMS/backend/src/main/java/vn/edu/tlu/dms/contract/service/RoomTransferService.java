package vn.edu.tlu.dms.contract.service;

import vn.edu.tlu.dms.contract.api.ContractDtos.TransferDetail;

/**
 * Xét duyệt yêu cầu chuyển phòng (FR-CON-07/08, BR-05, API 4.7). Việc khóa giường và đổi phân
 * phòng do BedOccupancyService của module room thực hiện trong cùng transaction.
 */
public interface RoomTransferService {

    /**
     * Đọc chi tiết yêu cầu kèm giường hiện tại, allowedActions và lịch sử trạng thái; yêu cầu ngoài
     * phạm vi của người gọi trả 404.
     */
    TransferDetail getTransferRequest(Long id);

    /**
     * Duyệt yêu cầu PENDING: chuyển sinh viên sang giường đích (đóng phân phòng cũ, mở STAY mới),
     * rồi đặt yêu cầu sang APPROVED. Mọi lỗi làm rollback cả việc chuyển giường.
     */
    TransferDetail approve(Long id, Long targetBedId);

    /** Từ chối yêu cầu PENDING với lý do bắt buộc; không thay đổi phân phòng. */
    TransferDetail reject(Long id, String reason);

    /** Sinh viên chủ yêu cầu hủy yêu cầu còn PENDING; giữ lại bản ghi và lịch sử trạng thái. */
    TransferDetail cancel(Long id);
}

package vn.edu.tlu.dms.contract.service;

import vn.edu.tlu.dms.contract.api.MoveOutDtos;

import java.time.LocalDate;

/**
 * Nghiệp vụ trả phòng (FR-CON-09, FR-CON-10, API 4.8). Việc đóng phân phòng, gỡ trưởng phòng và
 * hoàn cọc được thực hiện qua service công khai của module room/billing trong cùng transaction.
 */
public interface MoveOutService {

    /**
     * Xem trước trả phòng của hợp đồng ACTIVE trong đơn vị của Staff: kiểu kết thúc theo ngày trả,
     * số dư cọc, tiền hoàn, công nợ và các cảnh báo cần xác nhận.
     *
     * @param contractId mã hợp đồng cần trả phòng
     * @param moveOutDate ngày trả phòng dự kiến
     * @return dữ liệu xem trước; hợp đồng ngoài phạm vi trả 404
     */
    MoveOutDtos.Preview preview(Long contractId, LocalDate moveOutDate);

    /**
     * Thực hiện trả phòng trong một transaction: khóa giường/sinh viên rồi hợp đồng, đóng phân
     * phòng, ghi biên bản, cập nhật trạng thái hợp đồng và hoàn cọc nếu còn số dư dương.
     *
     * @param contractId mã hợp đồng cần trả phòng
     * @param input ngày trả, tình trạng tài sản và xác nhận cảnh báo
     * @return kết quả sau khi hợp đồng đã kết thúc
     */
    MoveOutDtos.Result execute(Long contractId, MoveOutDtos.Request input);
}

package vn.edu.tlu.dms.contract.service;

import vn.edu.tlu.dms.contract.api.StorageDtos;

/**
 * Nghiệp vụ gửi đồ khi tạm rời hè và nhận lại đồ đầu năm học (FR-CON-11, FR-CON-12, API 4.9).
 * Hợp đồng vẫn ACTIVE trong suốt thời gian gửi đồ; phân phòng được đóng/mở qua module room.
 */
public interface StorageService {

    /**
     * Tạo phiếu gửi đồ cho hợp đồng ACTIVE có lựa chọn TEMP_LEAVE đã duyệt và đã gia hạn qua học
     * kỳ hè; lưu giường cũ rồi đóng phân phòng với lý do SUMMER_LEAVE.
     *
     * @param contractId mã hợp đồng đang ACTIVE
     * @param input số kiện, mô tả, vị trí kho và ngày gửi
     * @return phiếu gửi đồ vừa tạo
     */
    StorageDtos.Record create(Long contractId, StorageDtos.CreateRequest input);

    /**
     * Nhận lại đồ: phân giường STAY trực tiếp cho sinh viên qua BedOccupancyService rồi ghi ngày
     * và người nhận lại vào phiếu.
     *
     * @param storageRecordId mã phiếu gửi đồ chưa nhận lại
     * @param input giường nhận và ngày nhận lại đồ
     * @return phiếu gửi đồ sau khi đã nhận lại
     */
    StorageDtos.Record returnStorage(Long storageRecordId, StorageDtos.ReturnRequest input);

    /**
     * Tìm phiếu gửi đồ chưa nhận lại của một hợp đồng, dùng cho chi tiết hợp đồng (API 4.5) hiển thị
     * liên kết kho gửi đồ mà không cần gọi thêm danh sách phiếu.
     *
     * @param contractId mã hợp đồng cần tra cứu
     * @return mã phiếu gửi đồ mở gần nhất; null khi hợp đồng chưa có phiếu hoặc đã nhận lại hết
     */
    Long findOpenRecordIdByContract(Long contractId);
}

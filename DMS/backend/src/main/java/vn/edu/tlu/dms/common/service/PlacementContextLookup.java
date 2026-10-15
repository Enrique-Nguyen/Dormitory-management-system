package vn.edu.tlu.dms.common.service;

import java.util.Optional;

/**
 * Module sở hữu đơn đăng ký/yêu cầu chuyển phòng cung cấp ngữ cảnh chọn giường qua interface này,
 * để room tính danh sách giường trống mà không phụ thuộc lên module tầng cao hơn.
 */
public interface PlacementContextLookup {

    /** Mã nguồn được hỗ trợ, lấy từ DmsConstants.PlacementSource. */
    String source();

    /**
     * Đọc ngữ cảnh chọn giường theo ID nguồn; trả Optional rỗng khi không tồn tại. Nơi gọi kiểm
     * tra phạm vi đơn vị và trạng thái trước khi trả dữ liệu.
     */
    Optional<PlacementContext> find(Long id);

    /**
     * Ngữ cảnh chọn giường. preferredUnitId null nghĩa là đơn vị lấy theo giường hiện tại của hợp
     * đồng (chuyển phòng); ready cho biết nguồn đang ở trạng thái được chọn giường.
     */
    record PlacementContext(
            Long studentId,
            Long contractId,
            Long preferredUnitId,
            Long preferredRoomTypeId,
            boolean ready) {}
}

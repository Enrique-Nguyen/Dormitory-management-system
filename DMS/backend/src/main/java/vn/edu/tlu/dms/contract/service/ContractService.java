package vn.edu.tlu.dms.contract.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.contract.api.ContractDtos;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Khai báo thao tác tra cứu hợp đồng theo chủ sở hữu hoặc phạm vi đơn vị quản lý. */
public interface ContractService {

    /**
     * Tìm kiếm và phân trang hợp đồng theo trạng thái, mã hoặc tên sinh viên trong phạm vi được
     * phép xem.
     *
     * @param input điều kiện tìm kiếm, phân trang, danh sách trạng thái và thứ tự đã cho phép
     * @param mine true để chỉ lấy dữ liệu của sinh viên đang đăng nhập
     * @return trang hợp đồng phù hợp với bộ lọc và quyền truy cập
     */
    PageResponse<ContractDtos.Response> searchByPage(SearchRequest input, boolean mine);

    /**
     * Lấy chi tiết hợp đồng theo API 4.5 sau khi kiểm tra quyền và phạm vi truy cập: sinh viên (kèm
     * giới tính), sổ cọc từ v_deposit_balance, người kích hoạt, toàn bộ lịch sử phân phòng mới nhất
     * trước, ba yêu cầu đang chờ, phiếu gửi đồ chưa nhận lại, biên bản trả phòng và allowedActions
     * theo vai trò/quyền của người xem.
     *
     * @param id mã định danh hợp đồng
     * @return thông tin chi tiết hợp đồng được phép xem
     */
    ContractDtos.Response getContractById(Long id);

    /**
     * Lấy hợp đồng PENDING hoặc ACTIVE của sinh viên đang đăng nhập với đầy đủ khối chi tiết như
     * getContractById.
     *
     * @return chi tiết hợp đồng hiện tại; null nếu chưa có hợp đồng phù hợp
     */
    ContractDtos.Response getMyCurrentContract();

    /**
     * Kiểm tra đơn đăng ký đã sinh hợp đồng PENDING hoặc ACTIVE hay chưa.
     *
     * @param applicationId mã định danh đơn đăng ký
     * @return true nếu còn hợp đồng sống tham chiếu đơn này
     */
    boolean hasLiveContractForApplication(Long applicationId);

    /**
     * Kích hoạt hợp đồng PENDING trong một transaction: kiểm tra phạm vi đơn vị, hạn kích hoạt,
     * mức cọc của tòa và phân phòng HOLD; lưu mức cọc, thu cọc ban đầu, chuyển phân phòng HOLD
     * sang STAY, cập nhật ACTIVE và ghi lịch sử/audit.
     *
     * @param id mã định danh hợp đồng cần kích hoạt
     * @param input xác nhận giấy hợp đồng, số tiền cọc, hình thức thanh toán và ghi chú
     * @return hợp đồng đã kích hoạt kèm khoản cọc và phân phòng hiện tại
     */
    ContractDtos.ActivationResult activate(Long id, ContractDtos.ActivateRequest input);

    /**
     * Tạo hợp đồng PENDING cho đơn đã APPROVED sau khi registration đã khóa đơn/sinh viên. Contract
     * module sở hữu mã, hạn kích hoạt và ghi lịch sử; registration chịu trách nhiệm phân giường.
     */
    Long createPendingForApplication(
            Long applicationId, Long studentId, LocalDate startDate, LocalDate endDate);

    /** Tìm hợp đồng PENDING/ACTIVE còn sống của đơn để release/cancel đúng bản ghi. */
    Long findLiveContractIdForApplication(Long applicationId);

    /**
     * Đọc mã và hạn kích hoạt đã lưu của hợp đồng, phục vụ màn chi tiết đơn đăng ký sau khi phân
     * giường; hợp đồng đã kích hoạt vẫn trả mã còn hạn kích hoạt null.
     *
     * @param contractId mã định danh hợp đồng
     * @return mã hợp đồng kèm hạn kích hoạt; null khi hợp đồng không tồn tại
     */
    ActivationInfo findActivationInfo(Long contractId);

    /** Mã hiển thị và hạn kích hoạt của hợp đồng, ảnh chụp từ bảng dormitory_contract. */
    record ActivationInfo(String code, OffsetDateTime activationDeadline) {}

    /** Hủy hợp đồng PENDING khi release-hold/hủy đơn; giữ lịch sử, không xóa cứng. */
    void cancelPendingContract(Long contractId, String reason);

    /** Cập nhật ngày kết thúc hợp đồng sau khi nghiệp vụ gia hạn/hè đã kiểm tra và khóa. */
    void extendEndDate(Long contractId, LocalDate newEndDate, String reason);

    /** Ghi nhận gia hạn hè như một yêu cầu APPROVED nguồn SUMMER_STAY trong cùng transaction. */
    void recordSummerExtension(
            Long contractId, LocalDate newEndDate, Long actorId, String reason);

    /** Đếm hợp đồng ACTIVE sắp hết hạn và quá hạn trả phòng trong phạm vi đơn vị đã xác định. */
    ExpirySummary getExpirySummary(Long managementUnitId, int expiringDays);

    /** Dữ liệu vận hành hợp đồng chỉ đọc cung cấp cho Dashboard. */
    record ExpirySummary(long expiringSoon, long overdueMoveout) {}
}

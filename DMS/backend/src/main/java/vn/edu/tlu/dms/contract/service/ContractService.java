package vn.edu.tlu.dms.contract.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.contract.api.ContractDtos;

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
     * Lấy chi tiết hợp đồng và lịch sử trạng thái sau khi kiểm tra quyền truy cập.
     *
     * @param id mã định danh hợp đồng
     * @return thông tin chi tiết hợp đồng được phép xem
     */
    ContractDtos.Response getContractById(Long id);

    /**
     * Lấy hợp đồng PENDING hoặc ACTIVE của sinh viên đang đăng nhập.
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

    /** Đếm hợp đồng ACTIVE sắp hết hạn và quá hạn trả phòng trong phạm vi đơn vị đã xác định. */
    ExpirySummary getExpirySummary(Long managementUnitId, int expiringDays);

    /** Dữ liệu vận hành hợp đồng chỉ đọc cung cấp cho Dashboard. */
    record ExpirySummary(long expiringSoon, long overdueMoveout) {}
}

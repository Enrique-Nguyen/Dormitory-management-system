package vn.edu.tlu.dms.contract.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.contract.api.ContractDtos;

/** Khai báo thao tác tra cứu hợp đồng theo chủ sở hữu hoặc phạm vi đơn vị quản lý. */
public interface ContractService {

    /**
     * Tìm kiếm và phân trang hợp đồng theo trạng thái, mã hoặc tên sinh viên trong phạm vi được
     * phép xem.
     *
     * @param page số trang, bắt đầu từ 0
     * @param size số bản ghi mỗi trang
     * @param status trạng thái cần lọc; null để lấy mọi trạng thái
     * @param mine true để chỉ lấy dữ liệu của sinh viên đang đăng nhập
     * @return trang hợp đồng phù hợp với bộ lọc và quyền truy cập
     */
    PageResponse<ContractDtos.Response> searchContracts(
            int page, int size, String status, boolean mine);

    /**
     * Tìm kiếm và phân trang hợp đồng theo trạng thái, mã hoặc tên sinh viên trong phạm vi được
     * phép xem.
     *
     * @param page số trang, bắt đầu từ 0
     * @param size số bản ghi mỗi trang
     * @param status trạng thái cần lọc; null để lấy mọi trạng thái
     * @param mine true để chỉ lấy dữ liệu của sinh viên đang đăng nhập
     * @param q từ khóa theo mã hồ sơ hoặc tên sinh viên; chuỗi rỗng để không lọc theo từ khóa
     * @return trang hợp đồng phù hợp với bộ lọc và quyền truy cập
     */
    PageResponse<ContractDtos.Response> searchContracts(
            int page, int size, String status, boolean mine, String q);

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
}

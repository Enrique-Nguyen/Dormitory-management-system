package vn.edu.tlu.dms.contract.service;

import java.util.Map;

/**
 * Khai báo CRUD hồ sơ hợp đồng và yêu cầu lưu trú đang chờ xử lý. ADMIN có quyền trên mọi đơn vị;
 * vai trò khác vẫn bị giới hạn đơn vị hoặc chủ sở hữu. Quyền không bỏ qua kiểm tra trạng thái.
 */
public interface ContractCrudService {

    /**
     * Tạo hợp đồng PENDING từ đơn đã APPROVED: STAFF trong đơn vị được giao, ADMIN mọi đơn vị; chưa
     * phân giường và chưa phát sinh tiền cọc.
     *
     * @param input mã đơn đăng ký, ngày bắt đầu và ngày kết thúc hợp đồng
     * @return hồ sơ hợp đồng vừa tạo
     */
    Object createPendingContract(Map<String, Object> input);

    /**
     * Sửa ngày bắt đầu hoặc kết thúc của hợp đồng PENDING chưa có phân giường hay giao dịch cọc.
     *
     * @param id mã định danh hợp đồng
     * @param input ngày bắt đầu hoặc kết thúc cần sửa; không thay đổi đơn đăng ký nguồn
     * @return hồ sơ hợp đồng sau khi cập nhật
     */
    Object updatePendingContract(Long id, Map<String, Object> input);

    /**
     * Xóa hợp đồng PENDING chưa có phân giường, giao dịch cọc hoặc lịch sử trạng thái.
     *
     * @param id mã định danh hợp đồng
     */
    void deletePendingContract(Long id);

    /**
     * Tìm kiếm và phân trang yêu cầu gia hạn hoặc chuyển phòng theo phạm vi người dùng.
     *
     * @param extension true cho yêu cầu gia hạn; false cho yêu cầu chuyển phòng
     * @param page số trang, bắt đầu từ 0
     * @param size số bản ghi mỗi trang
     * @param q từ khóa theo mã hồ sơ hoặc tên sinh viên; chuỗi rỗng để không lọc theo từ khóa
     * @return trang yêu cầu lưu trú được phép xem
     */
    Object searchStayRequests(boolean extension, int page, int size, String q);

    /**
     * Lấy chi tiết yêu cầu gia hạn hoặc chuyển phòng sau khi kiểm tra quyền xem hợp đồng.
     *
     * @param extension true cho yêu cầu gia hạn; false cho yêu cầu chuyển phòng
     * @param id mã định danh yêu cầu gia hạn hoặc chuyển phòng
     * @return thông tin yêu cầu được phép truy cập
     */
    Object getStayRequest(boolean extension, Long id);

    /**
     * Tạo yêu cầu PENDING cho hợp đồng ACTIVE: sinh viên chọn hợp đồng của mình, ADMIN được tạo
     * thay chủ hợp đồng đã chọn qua contractId. Chưa duyệt và chưa đổi hợp đồng hoặc giường.
     *
     * @param extension true cho yêu cầu gia hạn; false cho yêu cầu chuyển phòng
     * @param input mã hợp đồng, lý do và ngày kết thúc mới nếu là yêu cầu gia hạn
     * @return yêu cầu vừa tạo
     */
    Object createStayRequest(boolean extension, Map<String, Object> input);

    /**
     * Sửa yêu cầu PENDING: STUDENT của mình, STAFF đúng đơn vị, ADMIN toàn hệ thống; giữ hợp đồng
     * nguồn.
     *
     * @param extension true cho yêu cầu gia hạn; false cho yêu cầu chuyển phòng
     * @param id mã định danh yêu cầu gia hạn hoặc chuyển phòng
     * @param input lý do cần sửa và ngày kết thúc mới nếu là yêu cầu gia hạn
     * @return yêu cầu sau khi cập nhật
     */
    Object updateStayRequest(boolean extension, Long id, Map<String, Object> input);

    /**
     * Xóa yêu cầu gia hạn hoặc chuyển phòng còn PENDING: STUDENT của mình hoặc ADMIN toàn hệ thống.
     *
     * @param extension true cho yêu cầu gia hạn; false cho yêu cầu chuyển phòng
     * @param id mã định danh yêu cầu gia hạn hoặc chuyển phòng
     */
    void deletePendingStayRequest(boolean extension, Long id);
}

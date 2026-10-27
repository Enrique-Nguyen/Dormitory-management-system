package vn.edu.tlu.dms.contract.service;

import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.contract.api.ContractDtos;

/**
 * Khai báo CRUD hồ sơ hợp đồng và yêu cầu lưu trú đang chờ xử lý. ADMIN có quyền trên mọi đơn vị;
 * vai trò khác vẫn bị giới hạn đơn vị hoặc chủ sở hữu. Quyền không bỏ qua kiểm tra trạng thái.
 */
public interface ContractCrudService {

    /**
     * Lưu hợp đồng bằng một hàm: không có id tạo PENDING từ đơn APPROVED; có id sửa từng phần ngày
     * của hợp đồng PENDING chưa có phân giường/cọc. Kiểm tra quyền CREATE/UPDATE tương ứng.
     *
     * @param input id tùy chọn, mã đơn đăng ký, ngày bắt đầu và ngày kết thúc hợp đồng
     * @return hồ sơ tạo hoặc cập nhật; id không tồn tại trả 404, không được tạo ngầm
     */
    Object saveOrUpdateContract(ContractDtos.Save input);

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
     * @param input từ khóa, bộ lọc trạng thái, phân trang và sắp xếp hợp lệ
     * @param mine true chỉ lấy tài khoản hiện tại; endpoint quyết định, không lấy từ client
     * @return trang yêu cầu lưu trú được phép xem
     */
    Object searchStayRequestsByPage(boolean extension, SearchRequest input, boolean mine);

    /**
     * Lấy chi tiết yêu cầu gia hạn sau khi kiểm tra quyền xem hợp đồng, kèm ngày cũ/mới, hành động
     * được phép và timeline. Chi tiết yêu cầu chuyển phòng do {@code RoomTransferService} trả vì
     * kèm giường hiện tại, lịch sử và allowedActions.
     *
     * @param id mã định danh yêu cầu gia hạn
     * @return thông tin yêu cầu được phép truy cập
     */
    ContractDtos.ExtensionDetail getExtensionDetail(Long id);

    /**
     * Duyệt yêu cầu gia hạn PENDING của Staff/Admin đúng đơn vị trong một transaction: khóa yêu
     * cầu và hợp đồng, kiểm tra ngày mới sau ngày hiện tại và không chồng hợp đồng PENDING/ACTIVE
     * khác của sinh viên, cập nhật endDate, trạng thái yêu cầu, lịch sử và audit.
     *
     * @param id mã định danh yêu cầu gia hạn
     * @return chi tiết yêu cầu sau khi duyệt
     */
    ContractDtos.ExtensionDetail approveExtension(Long id);

    /**
     * Từ chối yêu cầu gia hạn PENDING của Staff/Admin đúng đơn vị; lý do bắt buộc và được lưu cùng
     * trạng thái, lịch sử và audit.
     *
     * @param id mã định danh yêu cầu gia hạn
     * @param reason lý do từ chối
     * @return chi tiết yêu cầu sau khi từ chối
     */
    ContractDtos.ExtensionDetail rejectExtension(Long id, String reason);

    /**
     * Hủy yêu cầu gia hạn PENDING của sinh viên chủ sở hữu hoặc ADMIN; không xóa cứng và giữ
     * timeline của yêu cầu.
     *
     * @param id mã định danh yêu cầu gia hạn
     * @return chi tiết yêu cầu sau khi hủy
     */
    ContractDtos.ExtensionDetail cancelExtension(Long id);

    /**
     * Lưu chung yêu cầu: không có id tạo PENDING trên hợp đồng ACTIVE; có id sửa yêu cầu PENDING.
     * Kiểm tra đúng quyền tạo/sửa và giữ hợp đồng nguồn, đơn vị và chủ sở hữu.
     *
     * @param extension true cho yêu cầu gia hạn; false cho yêu cầu chuyển phòng
     * @param input id tùy chọn, mã hợp đồng, lý do và ngày kết thúc mới nếu là yêu cầu gia hạn
     * @return yêu cầu được tạo hoặc sửa; id không tồn tại không được tạo ngầm
     */
    Object saveOrUpdateStayRequest(boolean extension, ContractDtos.StaySave input);

    /**
     * Xóa yêu cầu gia hạn hoặc chuyển phòng còn PENDING: STUDENT của mình hoặc ADMIN toàn hệ thống.
     *
     * @param extension true cho yêu cầu gia hạn; false cho yêu cầu chuyển phòng
     * @param id mã định danh yêu cầu gia hạn hoặc chuyển phòng
     */
    void deletePendingStayRequest(boolean extension, Long id);
}

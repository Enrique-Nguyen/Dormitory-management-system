package vn.edu.tlu.dms.notification.service;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;

import java.util.Map;

/** Hợp đồng tra cứu tiền cọc, kho gửi đồ và chỗ ở hiện tại; không chứa logic triển khai. */
public interface OverviewService {

    /**
     * Tìm và phân trang tiền cọc theo tên sinh viên. Sinh viên chỉ xem hợp đồng của mình; Staff
     * theo đơn vị, quyền tổng hợp mở rộng phạm vi đọc và Admin được xem toàn hệ thống. Khi
     * mine=true, mọi vai trò đều chỉ được xem dữ liệu của chính tài khoản đang đăng nhập.
     */
    PageResponse<Map<String, Object>> searchDepositsByPage(SearchRequest input, boolean mine);

    /**
     * Đọc số dư và các giao dịch của một hợp đồng sau khi kiểm tra quyền/phạm vi. Hợp đồng không
     * tồn tại hoặc ngoài phạm vi đều trả NOT_FOUND.
     */
    Map<String, Object> getDeposit(Long id);

    /**
     * Tìm phiếu gửi đồ trong phạm vi hợp đồng được đọc; mine=true luôn giới hạn chủ sở hữu theo tài
     * khoản đang đăng nhập, kể cả ADMIN có quyền đọc toàn hệ thống ở API chung.
     */
    PageResponse<Map<String, Object>> searchStorageByPage(SearchRequest input, boolean mine);

    /**
     * Tìm và phân trang chỗ ở hiện tại của chính người đăng nhập theo mã phòng/giường/tòa. Định
     * danh chủ sở hữu luôn lấy từ phiên, không nhận ID sinh viên do client truyền.
     */
    PageResponse<Map<String, Object>> searchMyRoomByPage(SearchRequest input);
}

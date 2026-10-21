package vn.edu.tlu.dms.contract.service;

import vn.edu.tlu.dms.common.service.SystemJobService.Change;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Các thao tác SYSTEM nội bộ; nơi gọi phải chạy qua SystemJobService. */
public interface ContractLifecycleService {

    /** Chỉ lấy ID hợp đồng PENDING đã qua deadline được chụp khi phân phòng. */
    List<Long> findExpiredPendingIds();

    /** Khóa đúng thứ tự, kiểm tra lại rồi hủy hợp đồng, trả giường và phát sự kiện hủy đơn. */
    Change cancelExpiredPending(Long id);

    /** Lấy ID hợp đồng ACTIVE nằm trong ngưỡng sắp hết hạn của ConfigService. */
    List<Long> findExpiringIds();

    /** Khóa hợp đồng và đọc lại ACTIVE; dùng làm khóa cha của thông báo cọc/hợp đồng. */
    Optional<ActiveContract> lockActiveContract(Long id);

    /** Kiểm tra ngày kết thúc theo cấu hình mới nhất, không lưu cờ sắp hết hạn. */
    boolean isExpiring(ActiveContract contract);

    record ActiveContract(Long id, Long studentId, String code, LocalDate endDate) {}
}

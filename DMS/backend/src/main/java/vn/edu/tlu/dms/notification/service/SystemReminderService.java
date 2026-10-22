package vn.edu.tlu.dms.notification.service;

import vn.edu.tlu.dms.common.service.SystemJobService.Change;

public interface SystemReminderService {

    /** Khóa/kiểm tra lại hợp đồng, tạo tối đa một thông báo cho mỗi (hợp đồng, ngày hết hạn). */
    Change remindExpiringContract(Long contractId);

    /** Khóa hợp đồng ACTIVE, tính lại FIFO và nhắc mỗi khoản trừ quá hạn chưa từng thông báo. */
    Change remindOverdueDeposit(Long contractId);
}

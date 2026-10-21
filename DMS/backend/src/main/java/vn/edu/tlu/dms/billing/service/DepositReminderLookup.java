package vn.edu.tlu.dms.billing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DepositReminderLookup {

    /** Lấy ID hợp đồng đang có khoản bù cọc quá hạn, tính theo view FIFO hiện hành. */
    List<Long> findOverdueContractIds();

    /**
     * Đọc lại các khoản trừ còn thiếu sau khi nơi gọi khóa hợp đồng ACTIVE. Khoản đã được bù theo
     * FIFO không được nhắc lại; chỉ gọi trong transaction SYSTEM.
     */
    List<OverdueDeduction> findUnpaidDeductions(Long contractId);

    record OverdueDeduction(Long id, BigDecimal remaining, LocalDate dueDate) {}
}

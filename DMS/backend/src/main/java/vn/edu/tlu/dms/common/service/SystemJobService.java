package vn.edu.tlu.dms.common.service;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/** Khung job chung: SYSTEM, transaction riêng từng ID, audit và cô lập lỗi trong lô. */
public interface SystemJobService {

    /**
     * Lấy ID ứng viên, xử lý từng ID trong REQUIRES_NEW; callback khóa và kiểm tra lại điều kiện.
     * Callback trả null nếu không còn đủ điều kiện. Lỗi một ID rollback cả nghiệp vụ và audit, các
     * ID sau tiếp tục chạy; không mở endpoint cho phép client gọi job.
     */
    RunResult run(String name, Supplier<List<Long>> candidates, Function<Long, Change> processOne);

    record Change(
            String action,
            String entityType,
            Long entityId,
            Object before,
            Object after,
            String reason) {}

    record RunResult(int processed, int skipped, int failed) {}
}

package vn.edu.tlu.dms.billing.service;

import vn.edu.tlu.dms.billing.api.InvoiceDtos;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;

import java.time.LocalDate;

/** Khai báo thao tác tra cứu hóa đơn và phát hành, thanh toán theo quyền truy cập. */
public interface InvoiceService {

    /** Tra cứu hóa đơn theo phân trang và quyền truy cập; mine = true lọc theo sinh viên. */
    PageResponse<InvoiceDtos.Response> searchByPage(SearchRequest request, boolean mine);

    /** Đọc chi tiết hóa đơn kèm các dòng thu và lịch sử trạng thái. */
    InvoiceDtos.Response getInvoiceById(Long id);

    /** Xác nhận thanh toán hóa đơn UNPAID; khóa bản ghi và ghi lịch sử trạng thái. */
    InvoiceDtos.Response confirmPayment(Long id, InvoiceDtos.Payment input);

    /** Phát hành tiền phòng cả kỳ cho sinh viên đang ở (FR-BIL-05, BR-04, BR-07); chốt đơn giá snapshot. */
    InvoiceDtos.BatchIssueResult issueRoomFee(Long semesterId);

    /** Phát hành điện nước theo phòng trong tháng (FR-BIL-04, BR-03, BR-04); áp bảng giá hiệu lực. */
    InvoiceDtos.BatchIssueResult issueUtility(Long managementUnitId, LocalDate billingMonth);
}

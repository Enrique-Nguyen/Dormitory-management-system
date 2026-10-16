package vn.edu.tlu.dms.billing.service;

import vn.edu.tlu.dms.billing.api.InvoiceDtos;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;

/** Khai báo thao tác tra cứu hóa đơn và xác nhận thanh toán theo quyền truy cập. */
public interface InvoiceService {

    /**
     * Tìm kiếm và phân trang hóa đơn theo điều kiện SearchRequest dùng chung.
     *
     * @param request điều kiện phân trang, từ khóa, loại và trạng thái hóa đơn
     * @param mine true nếu chỉ lấy hóa đơn của sinh viên đang đăng nhập, false nếu lấy theo đơn vị
     *     cán bộ
     * @return trang kết quả hóa đơn phù hợp
     */
    PageResponse<InvoiceDtos.Response> searchByPage(SearchRequest request, boolean mine);

    /**
     * Lấy chi tiết hóa đơn, các dòng thu và lịch sử trạng thái sau khi kiểm tra quyền xem.
     *
     * @param id mã định danh hóa đơn
     * @return chi tiết hóa đơn được phép truy cập
     */
    InvoiceDtos.Response getInvoiceById(Long id);

    /**
     * Xác nhận thanh toán hóa đơn UNPAID trong đơn vị của cán bộ và ghi lịch sử trạng thái. Khóa
     * bản ghi để tránh hai yêu cầu cùng xác nhận thành công; số tiền lấy từ hóa đơn đã lưu.
     *
     * @param id mã định danh hóa đơn
     * @param input thông tin người nộp và phương thức thanh toán
     * @return chi tiết hóa đơn sau khi chuyển sang trạng thái PAID
     */
    InvoiceDtos.Response confirmInvoicePayment(Long id, InvoiceDtos.Payment input);
}

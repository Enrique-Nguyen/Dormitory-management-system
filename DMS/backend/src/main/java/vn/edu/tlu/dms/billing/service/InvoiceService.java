package vn.edu.tlu.dms.billing.service;

import vn.edu.tlu.dms.billing.api.InvoiceDtos;
import vn.edu.tlu.dms.common.dto.PageResponse;

/** Khai báo thao tác tra cứu hóa đơn và xác nhận thanh toán theo quyền truy cập. */
public interface InvoiceService {

    /**
     * Tìm kiếm, lọc loại và trạng thái, rồi phân trang hóa đơn trong phạm vi người dùng được phép
     * xem.
     *
     * @param page số trang, bắt đầu từ 0
     * @param size số bản ghi mỗi trang
     * @param type loại hóa đơn cần lọc; null để lấy mọi loại
     * @param status trạng thái cần lọc; null để lấy mọi trạng thái
     * @param mine true để chỉ lấy dữ liệu của sinh viên đang đăng nhập
     * @return trang hóa đơn phù hợp với bộ lọc và quyền truy cập
     */
    PageResponse<InvoiceDtos.Response> searchInvoices(
            int page, int size, String type, String status, boolean mine);

    /**
     * Tìm kiếm, lọc loại và trạng thái, rồi phân trang hóa đơn trong phạm vi người dùng được phép
     * xem.
     *
     * @param page số trang, bắt đầu từ 0
     * @param size số bản ghi mỗi trang
     * @param type loại hóa đơn cần lọc; null để lấy mọi loại
     * @param status trạng thái cần lọc; null để lấy mọi trạng thái
     * @param mine true để chỉ lấy dữ liệu của sinh viên đang đăng nhập
     * @param q từ khóa theo mã hồ sơ hoặc tên sinh viên; chuỗi rỗng để không lọc theo từ khóa
     * @return trang hóa đơn phù hợp với bộ lọc và quyền truy cập
     */
    PageResponse<InvoiceDtos.Response> searchInvoices(
            int page, int size, String type, String status, boolean mine, String q);

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

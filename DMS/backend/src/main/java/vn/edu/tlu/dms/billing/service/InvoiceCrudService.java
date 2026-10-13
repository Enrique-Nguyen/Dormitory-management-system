package vn.edu.tlu.dms.billing.service;

import java.util.Map;

/** Khai báo thao tác thêm, sửa và xóa hồ sơ hóa đơn chưa phát sinh dòng tiền. */
public interface InvoiceCrudService {

    /**
     * Tạo hồ sơ hóa đơn UNPAID trong đơn vị của cán bộ đang đăng nhập; chưa phát sinh dòng tiền.
     *
     * @param input loại hóa đơn, đối tượng thu, tổng tiền và hạn đóng
     * @return hồ sơ hóa đơn vừa tạo
     */
    Object createInvoiceRecord(Map<String, Object> input);

    /**
     * Sửa tổng tiền hoặc hạn đóng của hóa đơn UNPAID chưa có dòng chi tiết hay lịch sử trạng thái.
     *
     * @param id mã định danh hóa đơn
     * @param input các trường được phép thay đổi: totalAmount và dueDate
     * @return hồ sơ hóa đơn sau khi cập nhật
     */
    Object updateUnpaidInvoiceRecord(Long id, Map<String, Object> input);

    /**
     * Xóa hóa đơn UNPAID chưa được sử dụng; từ chối xóa hóa đơn đã có dữ liệu liên quan.
     *
     * @param id mã định danh hóa đơn
     */
    void deleteUnusedInvoiceRecord(Long id);
}

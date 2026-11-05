package vn.edu.tlu.dms.billing.service;

import vn.edu.tlu.dms.billing.api.InvoiceDtos;

/** Khai báo thao tác thêm, sửa và xóa hóa đơn tạo tay chưa thanh toán. */
public interface InvoiceCrudService {

    /**
     * Thêm hóa đơn có nguồn MANUAL hoặc sửa tổng tiền/hạn đóng của hóa đơn MANUAL đang UNPAID.
     * Nguồn do server quyết định; không cho chuyển đối tượng, kỳ hoặc sửa hóa đơn đã phát hành.
     *
     * @param input thông tin hóa đơn cần lưu
     * @return hồ sơ hóa đơn sau khi lưu
     */
    Object saveOrUpdate(InvoiceDtos.Save input);

    /**
     * Xóa hóa đơn MANUAL đang UNPAID theo quyền/phạm vi; giữ audit và lịch sử trạng thái,
     * không bỏ ràng buộc tham chiếu tài chính của CSDL.
     *
     * @param id mã định danh hóa đơn
     */
    void deleteUnusedInvoiceRecord(Long id);
}

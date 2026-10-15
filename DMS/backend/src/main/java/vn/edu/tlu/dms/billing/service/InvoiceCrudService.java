package vn.edu.tlu.dms.billing.service;

import vn.edu.tlu.dms.billing.api.InvoiceDtos;

/** Khai báo thao tác thêm, sửa và xóa hồ sơ hóa đơn chưa phát sinh dòng tiền. */
public interface InvoiceCrudService {

    /**
     * Thêm mới hoặc cập nhật hồ sơ hóa đơn UNPAID chưa có dòng tiền. Khi input.id() == null: tạo
     * mới hóa đơn; khi input.id() != null: cập nhật hóa đơn chưa thanh toán.
     *
     * @param input thông tin hóa đơn cần lưu
     * @return hồ sơ hóa đơn sau khi lưu
     */
    Object saveOrUpdate(InvoiceDtos.Save input);

    /**
     * Xóa hóa đơn UNPAID chưa được sử dụng; từ chối xóa hóa đơn đã có dữ liệu liên quan.
     *
     * @param id mã định danh hóa đơn
     */
    void deleteUnusedInvoiceRecord(Long id);
}

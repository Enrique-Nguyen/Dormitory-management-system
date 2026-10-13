/**
 * Module {@code billing}: điện nước, hóa đơn, sổ cọc (DepositService, DebtQuery).
 *
 * <p>Phụ trách: TV5. Sở hữu bảng (ghi): utility_rate, utility_reading, invoice, invoice_item,
 * deposit_transaction.
 *
 * <p>Gói con: {@code api} (controller + DTO record), {@code service} (Service công khai, Policy),
 * {@code domain} (entity, enum, sự kiện), {@code repository}, {@code job}. Quy tắc phụ thuộc giữa
 * các module: SAD mục 4.2, được kiểm tra bởi {@code ModuleDependencyTest}.
 */
package vn.edu.tlu.dms.billing;

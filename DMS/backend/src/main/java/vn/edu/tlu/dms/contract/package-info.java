/**
 * Module {@code contract}: hợp đồng, gia hạn, chuyển phòng, trả phòng.
 *
 * <p>Phụ trách: TV3 (hợp đồng, gia hạn), TV2 (chuyển phòng), TV4 (trả phòng, gửi đồ). Sở hữu bảng (ghi): dormitory_contract, contract_extension_request, room_transfer_request, move_out_record, storage_record.
 *
 * <p>Gói con: {@code api} (controller + DTO record), {@code service} (Service công khai, Policy),
 * {@code domain} (entity, enum, sự kiện), {@code repository}, {@code job}. Quy tắc phụ thuộc giữa
 * các module: SAD mục 4.2, được kiểm tra bởi {@code ModuleDependencyTest}.
 */
package vn.edu.tlu.dms.contract;

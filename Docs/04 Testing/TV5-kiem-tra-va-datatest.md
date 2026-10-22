# Kiểm tra TV5: Điện nước, Hóa đơn, Tiền cọc và Staff Dashboard

> Ngày: 22/10/2026. Thành viên: TV5 Nguyễn Tiến Lộc.
> Căn cứ: SRS v0.12 (mục 3.6, 3.7), SAD v1.5 (mục 4.1, 4.2), API Specification.
> Nhánh bàn giao: `dev-locnt`, nền `origin/develop`.

## 1. Đối chiếu phần việc phân hệ TV5

| Yêu cầu | Trước khi kiểm tra | Kết quả triển khai và bằng chứng |
|---|---|---|
| FR-BIL-04: phát hành hóa đơn điện nước | Chưa có phát hành tự tính tiền | Đã có `issueUtility` quét phòng chốt chỉ số trong tháng, áp bậc giá hiệu lực `utility_rate`, tạo chi tiết `invoice_item` và chống phát hành trùng (BR-03, BR-04) |
| FR-BIL-05: phát hành hóa đơn tiền phòng | Chưa có phát hành theo kỳ | Đã có `issueRoomFee` quét hợp đồng ACTIVE trong học kỳ, snapshot mức giá phòng, bỏ qua các sinh viên đã có hóa đơn tiền phòng trong kỳ (BR-04, BR-07) |
| FR-BIL-06: xác nhận thanh toán | Endpoint cơ bản chưa khóa dòng | Đã bổ sung `SELECT FOR UPDATE` khóa hóa đơn UNPAID, ghi nhận phương thức CASH/TRANSFER, người nộp và cập nhật trạng thái PAID trong một transaction |
| FR-BIL-09: thu cọc ban đầu | Sổ cọc chỉ đọc, chưa có API nghiệp vụ | Đã triển khai `collectInitial` khóa hợp đồng PENDING/ACTIVE, chụp mức cọc của tòa nhà và chặn thu cọc trùng lặp trên cùng hợp đồng (BR-10) |
| FR-BIL-10: khấu trừ cọc hư hỏng | Chưa gắn kết với ticket sửa chữa | Đã triển khai `deduct` nhận ticket CLOSED có nguyên nhân MISUSE, ghi nhận số tiền khấu trừ âm theo ràng buộc CSDL và đặt hạn nộp bù cọc (BR-10, AT-14) |
| FR-BIL-11: tra cứu công nợ | Chưa có service công nợ | Đã có `DebtQuery` (`getStudentDebt`, `getRoomDebt`) phục vụ chặn gia hạn và xét duyệt trả phòng theo SAD 4.1 và BR-10 |
| FR-BIL-12: nộp bù cọc | Chưa có API bù cọc | Đã triển khai `topup` khôi phục số dư cọc về mức quy định khi sinh viên nộp bổ sung; chuyển trạng thái từ TOPUP_NEEDED về NORMAL (AT-19) |
| FR-BIL-13: hoàn trả cọc | Chưa có API hoàn cọc | Đã triển khai `refund` kiểm tra hợp đồng kết thúc lưu trú, chặn hoàn vượt số dư hiện có và tất toán số dư cọc về 0 |
| FR-NOT-05, FR-NOT-06: Staff Dashboard | Chỉ có trang menu rỗng | Đã triển khai `DashboardService` tổng hợp công suất giường, nợ đọng, hợp đồng cần bù cọc và sự cố theo đơn vị quản lý của cán bộ |
| Frontend TV5 | Chỉ có route và registry cơ bản | Đã hoàn thiện `StaffHomePage`, modal phát hành hóa đơn hàng loạt, modal sổ cọc đối soát lịch sử và modal xác nhận thanh toán |

## 2. Kiểm thử tự động

- **Kiểm thử tích hợp (`DepositIntegrationTest`)**:
  - Chạy trên JDK 21 và PostgreSQL 16 Testcontainers: **5/5 test đạt 100%, 0 lỗi, 0 thất bại**.
  - Kiểm tra đầy đủ chu trình: `collectInitial` -> `deduct` -> `topup` -> `refund` và `debtQuery`.
  - Kiểm tra các ràng buộc CSDL: số tiền âm cho deduction/refund, số tiền dương cho deposit/topup, duy nhất một lần thu/hoàn cọc trên mỗi hợp đồng.
- **Kiểm thử kiến trúc (`ModuleDependencyTest`)**:
  - Chạy ArchUnit: **8/8 test đạt 100%**.
  - Đảm bảo `notification` (Tier 6) gọi xuống `billing` (Tier 5), không có chu trình phụ thuộc ngược hoặc vi phạm phân tầng.
- **Frontend Verification**:
  - `npm run lint` (`oxlint`): **0 lỗi, 0 cảnh báo**.
  - `npm run build` (`tsc -b && vite build`): **Biên dịch production thành công 100%** (31.67s).

## 3. Giao diện và trải nghiệm người dùng

1. **Staff Dashboard (`StaffHomePage.tsx`)**:
   - KPI cards: Tỷ lệ lấp đầy giường kèm Progress bar động, Hóa đơn chưa nộp, Cảnh báo nộp bù cọc, Báo hỏng chưa xử lý.
   - Hiện trạng giường chi tiết: Tổng giường, Đang ở (STAY), Đang giữ chỗ (HOLD), Còn trống.
   - Cảnh báo phòng chưa có trưởng phòng theo quy tắc BR-11 kèm nút thao tác gán nhanh.
   - Thống kê phân loại sự cố kỹ thuật theo từng hạng mục.
2. **Modal phát hành hóa đơn (`IssueInvoicesModal.tsx`)**:
   - Tùy chọn phát hành tiền phòng học kỳ hoặc điện nước tháng.
   - Tích hợp thông báo giải thích cơ chế snapshot và chống phát hành trùng (BR-04).
3. **Modal sổ cọc đối soát (`DepositLedgerModal.tsx`)**:
   - Hiển thị số dư hiện tại, mức cọc quy định và trạng thái (Đầy đủ / Cần nộp bù cọc / Đã tất toán).
   - Bảng lịch sử giao dịch biến động cọc kèm Tag màu nhận diện.
   - Thao tác nộp bù cọc và hoàn trả cọc trực tiếp.
4. **Modal xác nhận thanh toán (`ConfirmPaymentModal.tsx`)**:
   - Chọn phương thức thanh toán Tiền mặt hoặc Chuyển khoản ngân hàng.
   - Ghi nhận thông tin người nộp tiền.

## 4. Giới hạn và lưu ý

- Việc phát hành hóa đơn tiền phòng phụ thuộc vào danh mục học kỳ và phân phòng mở của sinh viên.
- Việc phát hành hóa đơn điện nước phụ thuộc vào việc cán bộ đã ghi nhận chỉ số công tơ đầu/cuối của tháng đó.
- Hoàn trả cọc chỉ được thực hiện khi hợp đồng đã ở trạng thái kết thúc (EXPIRED / TERMINATED) hoặc làm thủ tục trả phòng.

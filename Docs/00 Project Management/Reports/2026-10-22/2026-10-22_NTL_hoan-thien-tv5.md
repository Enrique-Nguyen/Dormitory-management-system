# Báo cáo: Hoàn thiện nhiệm vụ TV5 (Điện nước, Hóa đơn, Tiền cọc & Dashboard)

> Ngày hoàn thành: 22/10/2026
> Thực hiện: TV5 Nguyễn Tiến Lộc
> Phạm vi: billing/notification, sổ cọc, công nợ, phát hành hóa đơn tự động và Staff Dashboard
> Căn cứ: SRS FR-BIL-01..13, FR-NOT-05, FR-NOT-06; SAD mục 4.1, 4.2; BR-03, BR-04, BR-07, BR-10, BR-11
> Trạng thái: Hoàn thành

---

## 1. Mục tiêu

Hoàn thành trọn vẹn toàn bộ các chức năng được phân công cho TV5 (Phân hệ 6: Điện nước, Hóa đơn, Tiền cọc và Phân hệ 7: Staff Dashboard & Báo cáo), bám sát các quy chuẩn kiến trúc phân tầng, không hardcode, locking chống race condition và phong cách code/comment tinh gọn của Phú Nguyên (TV2). Bàn giao qua nhánh `dev-locnt`, chia commit theo chuẩn Conventional Commits tương tự Nguyễn Văn Séc (TV1).

## 2. Kết quả

| Hạng mục | File | Nội dung |
|---|---|---|
| Sổ cọc & Tiền cọc | DepositService, DepositServiceImpl, DepositController, DepositDtos | Khóa hợp đồng SELECT FOR UPDATE; thu cọc ban đầu (`collectInitial`), trừ cọc vi phạm từ ticket sửa chữa (`deduct`), nộp bù cọc (`topup`), hoàn trả cọc (`refund`) và đối soát lịch sử biến động sổ cọc |
| Công nợ | DebtQuery, DebtQueryImpl | Cung cấp truy vấn chỉ đọc công nợ tiền phòng, nợ bù cọc và nợ điện nước phòng phục vụ điều kiện chặn gia hạn/trả phòng theo BR-10 |
| Phát hành hóa đơn | InvoiceService, InvoiceServiceImpl, InvoiceController, InvoiceDtos | Tự động quét hợp đồng ACTIVE chốt đơn giá snapshot cho tiền phòng cả kỳ (`issueRoomFee`); tính tiền điện nước phòng theo chỉ số và bậc giá (`issueUtility`); khóa bản ghi khi xác nhận thanh toán (`confirmPayment`) |
| Staff Dashboard | DashboardService, DashboardServiceImpl, DashboardController, DashboardDtos | Đặt tại module notification (Tier 6) tuân thủ phân tầng SAD 4.2; tổng hợp công suất giường, nợ đọng, cảnh báo bù cọc, phòng chưa có trưởng phòng và phân loại ticket sự cố |
| Hằng số & Lỗi | DmsConstants | Bổ sung đầy đủ ErrorCode, AuditAction và EntityType; không hardcode mã chuỗi trong logic |
| Giao diện Frontend | StaffHomePage, IssueInvoicesModal, DepositLedgerModal, ConfirmPaymentModal | Xây dựng Staff Dashboard trực quan với KPI cards, modal phát hành hóa đơn hàng loạt, modal sổ cọc đối soát lịch sử và modal xác nhận thanh toán |
| Client & Router | billing.ts, features/billing/index.ts, resources.ts, studentRoutes.tsx | Khai báo API client, các hook TanStack Query chuyên sâu, đồng bộ route `/student/deposit` trỏ vào `myDeposits` |

## 3. Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| Kiểm thử tích hợp PostgreSQL Testcontainers | 5/5 test đạt 100% (`DepositIntegrationTest`), kiểm tra thu cọc, trừ cọc, bù cọc, hoàn cọc và công nợ |
| Kiểm thử kiến trúc ArchUnit | 8/8 test đạt 100% (`ModuleDependencyTest`), không có chu trình phụ thuộc ngược |
| Frontend ESLint (`oxlint`) | 0 lỗi, 0 cảnh báo |
| Frontend Build (`tsc -b && vite build`) | Biên dịch production thành công 100% (31.67s) |

**Chưa kiểm tra:** Tải đồng thời trên 100 người dùng thực tế và thanh toán trực tuyến qua cổng ngân hàng bên ngoài (chức năng phạm vi Could, hiện dùng tiền mặt và chuyển khoản đối soát).

## 4. Điểm lệch so với thiết kế và quyết định cần xác nhận

| Nội dung | Quyết định | Lý do |
|---|---|---|
| Vị trí triển khai Staff Dashboard | Đặt Service tại module `notification` (Tier 6) thay vì `billing` (Tier 5) | Module `billing` không được gọi sang `maintenance` hay `contract` theo ma trận phân tầng SAD 4.2; module `notification` ở tầng trên cùng được phép tổng hợp dữ liệu từ các tầng dưới |
| Tên phương thức nghiệp vụ | Dùng động từ ngắn gọn (`collectInitial`, `deduct`, `topup`, `refund`, `getBalance`, `issueRoomFee`, `issueUtility`, `confirmPayment`, `getSummary`) | Tuân thủ phong cách code và JavaDoc tinh gọn, dứt khoát của Phú Nguyên (TV2) |

## 5. Việc còn lại

| Việc | Phụ trách |
|---|---|
| Không còn chức năng Must nào của TV5 chưa triển khai | TV5 |
| Mở Pull Request từ `dev-locnt` vào `develop` và tiến hành cross-review | TV5 & TV4 (Nguyễn Văn Quyền) |

## 6. Tệp liên quan

- Sửa: `InvoiceController.java`, `InvoiceDtos.java`, `InvoiceService.java`, `InvoiceServiceImpl.java`, `DepositTransactionRepository.java`, `DmsConstants.java`, `studentRoutes.tsx`, `resources.ts`, `StaffHomePage.tsx`.
- Mới: `DepositController.java`, `DepositDtos.java`, `DepositService.java`, `DepositServiceImpl.java`, `DebtQuery.java`, `DebtQueryImpl.java`, `DashboardController.java`, `DashboardDtos.java`, `DashboardService.java`, `DashboardServiceImpl.java`, `DepositIntegrationTest.java`, `billing.ts`, `features/billing/index.ts`, `IssueInvoicesModal.tsx`, `DepositLedgerModal.tsx`, `ConfirmPaymentModal.tsx`, `Docs/04 Testing/TV5-kiem-tra-va-datatest.md`.
- Xóa hoặc chuyển: Không xóa mã nguồn hiện có của các thành viên khác.

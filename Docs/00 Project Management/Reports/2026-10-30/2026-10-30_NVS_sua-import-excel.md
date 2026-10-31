# Báo cáo: Sửa import Excel không nhận dữ liệu đúng cấu trúc

> Ngày hoàn thành: 30/10/2026
> Thực hiện: TV1 Nguyễn Văn Séc, cùng Codex
> Phạm vi: import sinh viên backend/frontend, kiểm thử và tài liệu
> Căn cứ: SRS FR-ACC-05; API 4.10, 7.1; UI mục Import sinh viên; phân công đề xuất 13.1
> Trạng thái: Đã sửa và kiểm thử; chờ review PR

## 1. Mục tiêu

Kiểm tra lỗi người dùng báo: file đúng cấu trúc nhưng không nhận dữ liệu. Làm trên `dev-secnv`, cập nhật fast-forward từ `origin/develop` tại `c5b409d`; giữ source của các thành viên khác và gửi PR vào develop theo yêu cầu. Không sửa migration hoặc mở rộng quyền import cho Staff.

## 2. Kết quả

| Hạng mục | File | Nội dung |
|---|---|---|
| Mã dạng số | StudentImportServiceImpl, DmsConstants | Đọc số nguyên từ giá trị thật khi Excel hiển thị số mũ/phân nhóm; giữ đệm 0, từ chối phần thập phân và ô số quá 15 chữ số để không cấp sai username |
| Unicode và dòng trống | StudentImportServiceImpl | Chuẩn hóa NFC/NBSP/BOM đầu tiêu đề, kiểm trùng sau chuẩn hóa; chỉ đếm dòng có dữ liệu, giữ số dòng Excel và bỏ ô tiêu đề trống có định dạng |
| Preview và thao tác ghi | StudentImportPanel, resources, errorMessages | Thông báo đã đọc thành công thay cho bảng rỗng; báo chính xác cột thiếu; lỗi ghi đóng xác nhận và bắt kiểm tra lại |
| Kết quả một lần | StudentImportPage, StudentImportPanel | Giữ preview/tài khoản khi đổi tab; khóa thay/xóa file khi chưa tải credential, cho chọn lô tiếp theo sau tải |
| Kiểm thử | StudentImportIntegrationTest | Thêm sáu test backend hồi quy; fixture và script chẩn đoán UI/OIDC được giữ ngoài source |

## 3. Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| Tái hiện trên mã cũ | 11 test cũ đạt; ba test mới thất bại: số mũ bị xem là mã sai, Unicode báo thiếu cột, dòng trống báo file không hợp lệ |
| Backend import sau sửa | 17/17 đạt; gồm 5.000 dòng thật, 5.001 dòng bị từ chối, giữ số 0, mã Text 20 ký tự, trùng tiêu đề chuẩn hóa và rollback khi định danh lỗi |
| Backend toàn bộ | `mvnw.cmd -B verify`: 328 test, không lỗi hoặc bỏ qua |
| Frontend | lint/build đạt; còn cảnh báo bundle lớn có từ trước |
| Trình duyệt/Keycloak thật | Hai nhóm kiểm tra đã đạt: 6 bước đăng nhập/preview/tab/import/CSV thật và 7 kiểm tra preview/lỗi ở lượt chỉ đọc; không có ngoại lệ JS trong các bước đạt. Có mô phỏng phản hồi 503 để kiểm tra UI không tự retry; không mô phỏng xác thực |

**Chưa kiểm tra:** file cụ thể của người dùng chưa được cung cấp; kết quả áp dụng cho các lỗi đã tái hiện bằng fixture. Runner trình duyệt trên máy local còn timeout khi ghép liên tục toàn bộ bước/khởi tạo đăng nhập, nên không coi một lượt end-to-end hoàn chỉnh là đã đạt hoặc đưa runner đó vào CI/source. Không kiểm thử tải, nhiều backend instance hoặc production.

## 4. Điểm lệch so với thiết kế và quyết định cần xác nhận

| Nội dung | Quyết định | Lý do |
|---|---|---|
| Tên nhánh | Dùng `dev-secnv` theo yêu cầu trước đó | Ngoại lệ quy ước tên nhánh, vẫn dùng PR và CI trước merge |
| Mã dài và điện thoại mất chữ số trong Excel | Không đoán hoặc thêm lại chữ số | Dùng Text để giữ nguyên; tránh tạo sai danh tính |
| Giới hạn dòng | Đếm 5.000 dòng có dữ liệu, không đếm vùng chỉ có định dạng | Giữ giới hạn thiết kế và đúng quy tắc bỏ qua dòng trống |

Không thay endpoint hoặc JSON, phân quyền, transaction, bù Keycloak và giới hạn dung lượng/giải nén. Local đã sao lưu cả hai database trước khi chạy; Flyway tự áp dụng V7 đã có sẵn trên develop, không có migration mới từ bản sửa này.

## 5. Việc còn lại

| Việc | Phụ trách |
|---|---|
| Đối chiếu thêm nếu người dùng cung cấp file gốc; kiểm tra lại các bước trình duyệt bị timeout trên máy local | TV1 |
| Review và GitHub CI trước merge develop | Nhóm |

## 6. Tệp liên quan

- Sửa: StudentImportServiceImpl, DmsConstants, StudentImportIntegrationTest; StudentImportPanel, StudentImportPage, resources, errorMessages; API Spec, UI Design và README DataTest.
- Mới: Báo cáo này.
- Xóa hoặc chuyển: Không có. Log, backup, ảnh và tài khoản kiểm thử giữ trong thư mục local ngoài repository.

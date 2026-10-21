# Đặc tả yêu cầu phần mềm (SRS)
# Hệ thống quản lý ký túc xá sinh viên (DMS)

| | |
|---|---|
| Phiên bản | **0.12** |
| Ngày | 21/10/2026 |
| Học phần | Chuyên đề CNTT, lớp 65CNTT, Nhóm 3 |
| Giảng viên | Hoàng Quốc Dũng |
| Thành viên | Nguyễn Văn Séc, Nguyễn Phú Nguyên, Lê Đức Nguyên, Nguyễn Văn Quyền, Nguyễn Tiến Lộc |
| Tài liệu nguồn | `he-thong-quan-ly-ky-tuc-xa-mini.md` (đề xuất đề tài), `DMS-thuyet-trinh.html` (bài thuyết trình) |
| Trạng thái | Đã chốt toàn bộ câu hỏi nghiệp vụ Q-01 đến Q-21 (mục 9); sẵn sàng làm căn cứ thiết kế. Các số liệu quy mô vẫn cần đối chiếu khi khảo sát BQL |

### Lịch sử thay đổi

| Phiên bản | Ngày | Người thực hiện | Nội dung |
|---|---|---|---|
| 0.12 | 21/10/2026 | TV1 Nguyễn Văn Séc | Đồng bộ phần tài khoản và khung chung: làm rõ lỗi JWT, giữ FR-ACC-05 hai bước và trả mật khẩu tạm một lần; khung job SYSTEM có transaction riêng từng ID, nhắc bù cọc ít nhất mỗi giờ theo NFR-REL-03. Không đổi luồng nghiệp vụ của các module khác |
| 0.1 | 07/10/2026 | Nhóm 3 | Bản đầu tiên, dựng từ tài liệu đề xuất đã chốt |
| 0.2 | 07/10/2026 | Nhóm 3 | Cập nhật theo trả lời Q-01 đến Q-06: phí phòng theo học kỳ; hạn đóng 7 ngày; điện nước tính theo phòng qua **trưởng phòng**; **sổ tiền cọc** (mức cọc theo tòa, trừ khi sửa chữa, bù cọc, hoàn cọc); ngưỡng sắp hết hạn 15 ngày |
| 0.3 | 07/10/2026 | Nhóm 3 | Chốt Q-07 đến Q-21: quy mô ~1.500 sinh viên; hao mòn tự nhiên không trừ cọc; quá hạn bù cọc thì nhắc nhở; quy trình **học kỳ hè** (đăng ký ở hè, tạm rời hè và gửi đồ vào kho) |
| 0.4 | 08/10/2026 | Nhóm 3 | Bù cọc chưa nộp được tất toán bằng số dư khi trả phòng, không ghi thành công nợ (tránh thu hai lần); công nợ sau trả phòng chỉ gồm phí phòng chưa đóng và số dư cọc âm. Mục 6.2 thêm quy ước `errorCode` và `allowedActions` |
| 0.5 | 10/10/2026 | Nhóm 3 | Mục 5 đồng bộ với thiết kế CSDL v0.2 (`DB-Design-DMS.md`): tách hồ sơ sinh viên; thêm diện ưu tiên, đơn vị áp dụng của đợt, cấu hình, ảnh sự cố; lịch sử trạng thái dùng chung; mã hiển thị HD / TK / HP / DN; thông báo xem theo khoảng thời gian ở thực tế |
| 0.6 | 10/10/2026 | Nhóm 3 | Đồng bộ với Đặc tả API v0.2: Staff tạo được ticket **của phòng** (ví dụ hư hỏng phát hiện khi trả phòng) và tự nghiệm thu ticket do mình tạo; phát hành điện nước **bỏ qua** phòng thiếu chỉ số thay vì khóa cả đợt (UC-21, 1a); trừ cọc chia đều cho **người Staff chọn** trong số người ở lúc tạo ticket (mặc định chọn cả phòng, FR-MNT-10, Q-15) |
| 0.7 | 13/10/2026 | Nhóm 3 | Đồng bộ luồng đăng nhập Spring Security thật: xác thực JSON, CSRF, thu hồi phiên khi thay đổi bảo mật, giới hạn BCrypt và quyền reset theo đợt/phân phòng hiệu lực; bỏ cơ chế demo/mock |
| 0.8 | 14/10/2026 | Nhóm 3 | Theo yêu cầu chuyển đăng nhập sang Keycloak: OIDC/PKCE, JWT Bearer, tài khoản tập trung, liên kết subject và giữ RBAC/phạm vi DMS; không tự tạo tài khoản demo |
| 0.9 | 14/10/2026 | Nhóm 3 | Cập nhật ADMIN làm mọi nghiệp vụ ở mọi đơn vị; ma trận role/quyền CRUD, /me trả permissions, đổi role thu hồi token cũ |
| 0.10 | 15/10/2026 | Nhóm 3 | ADMIN tick quyền động theo role/màn/thao tác, lưu CSDL và áp dụng ngay; giữ giới hạn nghiệp vụ của vai trò, quyền hồ sơ bắt buộc và chống ghi đè khi nhiều Admin cùng sửa |
| 0.11 | 15/10/2026 | Nhóm 3 | Cập nhật đồng bộ theo kết quả code review và quyết định kỹ thuật: (1) Mục 10.1, FR-REG-04 & AT-32: Đơn ở trạng thái APPROVED chỉ được hủy (`cancel`) khi chưa sinh hợp đồng PENDING hoặc ACTIVE liên kết (nếu đã có hợp đồng, chặn 409 `APPLICATION_HAS_CONTRACT` để tránh hợp đồng mồ côi, phải hủy giữ chỗ/hợp đồng trước); (2) Mục 3.1, 3.6 và BR-07: Quy định rõ phạm vi phân quyền (scope) của Staff đối với hóa đơn tiền phòng được xác định theo `semester_id` của hóa đơn (sinh viên có lịch sử ở tại đơn vị trong kỳ hoặc nộp đơn vào đơn vị cho đợt thuộc kỳ), tránh lộ dữ liệu khi sinh viên chuyển đơn vị giữa các kỳ; (3) Mục 3.2 (FR-ROOM-02), mục 5.1 & AT-33: Bổ sung ràng buộc không cho phép đổi số giường (`bed_count`) khi loại phòng đã được gán cho phòng đang tồn tại (trả 409 `RESOURCE_IN_USE`); đồng bộ tham chiếu `DB-Design-DMS.md` v0.7. |

---

## Mục lục

1. Giới thiệu
2. Mô tả tổng quan
3. Yêu cầu chức năng
4. Quy tắc nghiệp vụ
5. Yêu cầu dữ liệu
6. Yêu cầu giao diện ngoài
7. Yêu cầu phi chức năng
8. Tiêu chí nghiệm thu và truy vết
9. Vấn đề mở và giả định cần xác nhận
10. Phụ lục: sơ đồ trạng thái

---

## 1. Giới thiệu

### 1.1. Mục đích

Tài liệu này đặc tả đầy đủ yêu cầu của Hệ thống quản lý ký túc xá sinh viên (DMS) phiên bản MVP. Đây là căn cứ để nhóm thiết kế (ERD, API, giao diện), cài đặt, kiểm thử và nghiệm thu; đồng thời là căn cứ để giảng viên đánh giá phạm vi đồ án.

Người đọc: giảng viên hướng dẫn, các thành viên nhóm, đại diện BQL ký túc xá khi khảo sát.

### 1.2. Phạm vi sản phẩm

DMS là ứng dụng web tập trung quản lý **vòng đời lưu trú** của sinh viên tại ký túc xá Trường Đại học Thủy Lợi, gồm hai đơn vị quản lý: **K1** và **N2 + N3**.

```text
Đăng ký → Xét duyệt → Phân phòng → Ký hợp đồng → Lưu trú / Tính phí
        → Báo hỏng → Chuyển phòng / Gia hạn → Trả phòng
```

**Trong phạm vi MVP:** bảy phân hệ (Tài khoản & RBAC; Phòng, Giường & Tòa; Đăng ký & Phân phòng; Hợp đồng; Yêu cầu sửa chữa; Điện nước & Hóa đơn; Thông báo & Dashboard), bốn vai trò (Student, Staff, Technician, Admin).

**Ngoài phạm vi MVP:** cổng thanh toán trực tuyến, QR ra vào, quản lý khách thăm, đồng hồ điện nước IoT, AI chatbot, ghép bạn cùng phòng, gợi ý phân phòng tự động, email và thông báo thời gian thực (mức Could).

**Lợi ích mong đợi:** một nguồn dữ liệu chung cho hai đơn vị, mỗi đơn vị chỉ thao tác trên dữ liệu của mình; không phân phòng vượt chỗ; theo dõi được ai đã đóng tiền, ai còn nợ; báo hỏng có người chịu trách nhiệm và lịch sử đầy đủ.

### 1.3. Thuật ngữ và viết tắt

| Thuật ngữ | Giải thích |
|---|---|
| BQL | Ban quản lý ký túc xá, vai trò **Staff** trong hệ thống |
| KTV | Kỹ thuật viên, vai trò **Technician** |
| Đơn vị quản lý | Đơn vị vận hành một nhóm tòa (`ManagementUnit`): hiện có K1 và N2 + N3. Là cấp phạm vi dữ liệu của Staff |
| Đợt đăng ký | Khoảng thời gian sinh viên được nộp đơn xin ở (`RegistrationPeriod`) |
| Đơn vị nguyện vọng | Đơn vị quản lý sinh viên chọn khi nộp đơn; quyết định Staff nào xét đơn |
| Phân phòng | Bản ghi một sinh viên chiếm một giường trong một khoảng thời gian (`RoomAssignmentHistory`). Có hai loại: **giữ chỗ** và **đang ở** |
| Phân phòng hiệu lực | Phân phòng chưa đóng (`ended_at` trống) |
| Sức chứa khai thác | Số giường ở trạng thái Hoạt động thuộc các phòng đang Hoạt động |
| Hợp đồng | Một lần lưu trú của sinh viên (`DormitoryContract`): thời hạn, tiền cọc, trạng thái. Không lưu phòng và giá |
| Học kỳ | Kỳ thu **phí phòng**: Học kỳ 1, Học kỳ 2, Học kỳ hè (`Semester`) |
| Tạm rời hè | Sinh viên không ở học kỳ hè nhưng tiếp tục ở năm học sau: gửi đồ cá nhân vào kho KTX, trả giường trong hè, giữ hợp đồng và tiền cọc |
| Hao mòn tự nhiên | Hư hỏng do thiết bị cũ, hết tuổi thọ, không do sinh viên sử dụng sai; không bị trừ cọc |
| Kỳ điện nước | Kỳ chốt chỉ số và thu tiền điện nước của phòng, theo **tháng** |
| Trưởng phòng | Một sinh viên đang ở trong phòng được Staff chỉ định; nhận hóa đơn điện nước của phòng, thu của các thành viên và nộp cho BQL. Không phải vai trò riêng mà là một chỉ định trên phòng |
| Mức cọc | Số tiền cọc bắt buộc, quy định theo tòa: K1 là 1.000.000 đ, N2 / N3 là 2.000.000 đ |
| Sổ cọc | Các giao dịch tiền cọc của một hợp đồng: thu cọc, trừ cọc, bù cọc, hoàn cọc. **Số dư cọc** = tổng các giao dịch |
| Bù cọc | Khoản sinh viên phải nộp để số dư cọc trở lại bằng mức cọc sau khi bị trừ. **Số tiền cần bù** = mức cọc − số dư cọc, tính ra, chỉ có nghĩa khi hợp đồng còn ACTIVE; khi trả phòng, khoản cần bù được tất toán bằng số dư |
| Ticket | Yêu cầu sửa chữa (`MaintenanceRequest`) |
| Nhãn suy ra | Thông tin tính từ dữ liệu khác, không lưu thành trạng thái: "Sắp hết hạn", "Quá hạn", "Đầy", số chỗ trống, số dư cọc |
| MoSCoW | Must / Should / Could / Won't: mức ưu tiên yêu cầu |
| RBAC | Phân quyền theo vai trò |

### 1.4. Tài liệu tham khảo

1. Đề xuất đề tài DMS, Nhóm 3 (`he-thong-quan-ly-ky-tuc-xa-mini.md`), bản đã chỉnh sửa 07/10/2026.
2. Bài thuyết trình DMS (`DMS-thuyet-trinh.html`).
3. IEEE Std 830-1998, *Recommended Practice for Software Requirements Specifications* (bố cục tham khảo).

### 1.5. Cách đọc tài liệu

- Mỗi yêu cầu có mã duy nhất: `FR-<phân hệ>-<số>` (chức năng), `BR-<số>` (quy tắc nghiệp vụ), `NFR-<nhóm>-<số>` (phi chức năng).
- Cột **Ưu tiên** dùng MoSCoW. Các yêu cầu có ký hiệu **✂** thuộc "đường cắt": nếu cuối tuần 7 lõi chưa đạt nghiệm thu nội bộ, chúng hạ từ Must xuống Should.
- Mục 9 ghi lại các quyết định nghiệp vụ đã chốt và nguồn gốc của chúng. **[Giả định]** đánh dấu quyết định kỹ thuật do nhóm tự chọn hoặc số liệu cần đối chiếu thực tế.

---

## 2. Mô tả tổng quan

### 2.1. Bối cảnh sản phẩm

Hiện hai đơn vị quản lý bằng sổ sách, Excel riêng lẻ và nhóm chat (theo quan sát của nhóm, sẽ xác nhận qua khảo sát). DMS là hệ thống mới, độc lập, không thay thế hay tích hợp với hệ thống đào tạo của trường trong MVP. Dữ liệu sinh viên được đưa vào bằng file Excel.

```text
 Trình duyệt (React SPA)  ──HTTPS/JSON──>  Spring Boot (REST API)  ──JDBC──>  PostgreSQL
  Student / Staff /                         Controller → Service →
  Technician / Admin                        Repository; tác vụ định kỳ
```

### 2.2. Chức năng chính

| # | Phân hệ | Chức năng chính |
|---|---|---|
| 1 | Tài khoản & RBAC | Đăng nhập, đổi mật khẩu, hồ sơ, import tài khoản, phân quyền theo vai trò và đơn vị, nhật ký |
| 2 | Phòng, Giường & Tòa | Danh mục đơn vị, tòa, loại phòng, phòng, giường; trạng thái vận hành; tình trạng chỗ |
| 3 | Đăng ký & Phân phòng | Đợt đăng ký, nộp đơn, điểm ưu tiên, xét duyệt, phân giường, giữ chỗ |
| 4 | Hợp đồng | Kích hoạt, tự hủy khi quá hạn, gia hạn, chuyển phòng, trả phòng |
| 5 | Yêu cầu sửa chữa | Báo hỏng, giao việc, xử lý, nghiệm thu, tự đóng |
| 6 | Điện nước, Hóa đơn & Tiền cọc | Phí phòng theo học kỳ cho từng sinh viên; điện nước theo phòng qua trưởng phòng; sổ tiền cọc; xác nhận thanh toán; công nợ |
| 7 | Thông báo & Dashboard | Thông báo theo phạm vi, thống kê cơ bản |

### 2.3. Người dùng

| Vai trò | Mô tả | Số lượng ước tính [Giả định] | Đặc điểm |
|---|---|---|---|
| Student | Sinh viên đăng ký ở và đang ở KTX; một sinh viên mỗi phòng được chỉ định làm **trưởng phòng** | ~1.500 | Dùng điện thoại là chính; thao tác không thường xuyên |
| Staff | Cán bộ BQL của một đơn vị | 5–10 | Dùng máy tính; thao tác nghiệp vụ hằng ngày |
| Technician | Kỹ thuật viên của một đơn vị | 3–6 | Dùng điện thoại tại hiện trường |
| Admin | Quản trị hệ thống và nghiệp vụ | 1–2 | Quản lý tài khoản, danh mục và mọi nghiệp vụ ở mọi đơn vị |

### 2.4. Môi trường vận hành

- Trình duyệt: Chrome, Edge, Firefox, Safari bản phát hành trong 2 năm gần nhất; giao diện responsive từ 360 px.
- Máy chủ: Java 17+, Spring Boot 3, PostgreSQL 15+; triển khai bằng Docker Compose trên một máy chủ [Giả định].
- Kết nối HTTPS.

### 2.5. Ràng buộc thiết kế và cài đặt

- Thời gian thực hiện 12 tuần, 5 thành viên.
- Công nghệ: React, Spring Boot, PostgreSQL; kiến trúc **modular monolith** ba lớp.
- Quy tắc nghiệp vụ nằm ở tầng Service; các bất biến quan trọng được bảo vệ thêm bằng ràng buộc CSDL.
- Không lưu dữ liệu suy ra được (số chỗ trống, "Đầy", "Sắp hết hạn", "Quá hạn").
- Không ghi đè dữ liệu lịch sử: phòng, giường và giá áp dụng chỉ nằm trên phân phòng; thay đổi bằng cách đóng dòng cũ, mở dòng mới.

### 2.6. Giả định và phụ thuộc

- BQL cung cấp danh sách sinh viên dạng Excel (mã SV, họ tên, giới tính, ngày sinh, khoa, khóa, số điện thoại).
- Hợp đồng được ký bằng giấy; hệ thống chỉ ghi nhận việc kích hoạt.
- Tiền được thu ngoài hệ thống (tiền mặt hoặc chuyển khoản); hệ thống chỉ ghi nhận xác nhận thanh toán.
- Tiền điện nước do các thành viên trong phòng tự thống nhất chia; trưởng phòng nộp cho BQL. Hệ thống không chia tiền điện nước cho từng sinh viên.
- Tiền cọc nộp khi ký hợp đồng; mức cọc theo tòa; được hoàn khi sinh viên rời KTX.
- Mỗi phòng có một đồng hồ điện và một đồng hồ nước.
- Các tham số cấu hình được: N ngày kích hoạt (7), ngưỡng "Sắp hết hạn" (15 ngày), số ngày tự đóng ticket (3), hạn đóng tiền (7 ngày sau ngày thông báo), mức cọc theo tòa, trọng số ưu tiên, đơn giá.

---

## 3. Yêu cầu chức năng

Quy ước chung cho mọi yêu cầu:

- Mọi API kiểm tra **vai trò + phạm vi + trạng thái** của đối tượng trước khi thực hiện; không chỉ ẩn nút ở giao diện.
- Mọi thao tác thay đổi trạng thái lưu người thao tác, thời gian và lý do (nếu có) vào lịch sử của đối tượng; thao tác quản trị ghi `AuditLog`.
- Mọi chuyển trạng thái không có trong sơ đồ ở mục 10 đều bị từ chối với lỗi rõ ràng.

### 3.1. Tài khoản & RBAC (ACC)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-ACC-01 | Người dùng đăng nhập tại Keycloak bằng tên đăng nhập/mã SV và mật khẩu; React dùng OIDC Authorization Code + PKCE, backend kiểm tra JWT. Đăng xuất từ DMS thu hồi token/phiên của tài khoản rồi chuyển sang logout Keycloak | Tất cả | Must |
| FR-ACC-02 | Sau 5 lần đăng nhập sai liên tiếp, tài khoản bị khóa tạm 15 phút [Giả định] | Tất cả | Should |
| FR-ACC-03 | Đổi mật khẩu qua Keycloak, 8–64 ký tự, có chữ/số, không dùng lại mật khẩu hiện tại; mật khẩu tạm bắt hoàn tất UPDATE_PASSWORD trước khi được cấp token. JWT đã cấp trước thay đổi trực tiếp tại Keycloak có thể còn hiệu lực tối đa 5 phút | Tất cả | Must |
| FR-ACC-04 | Người dùng xem và cập nhật hồ sơ cá nhân (số điện thoại, quê quán, liên hệ khẩn cấp); mã SV, họ tên, giới tính chỉ Admin sửa | Tất cả | Must |
| FR-ACC-05 | Admin import danh sách sinh viên từ Excel: kiểm tra định dạng, báo lỗi theo từng dòng, bỏ qua mã SV đã tồn tại, tạo tài khoản với mật khẩu tạm ngẫu nhiên tại Keycloak và required action đổi mật khẩu | Admin | Must |
| FR-ACC-06 | Admin tạo, khóa, mở khóa tài khoản Staff, Technician, Admin; gán/thay đổi vai trò và đơn vị quản lý, thu hồi token cũ | Admin | Must |
| FR-ACC-07 | Đặt lại mật khẩu: Admin cho mọi tài khoản; Staff chỉ cho sinh viên có đơn trong đợt đang mở hoặc phân phòng còn hiệu lực tại đơn vị mình (BR-07). Trả mật khẩu tạm một lần, bắt đổi khi đăng nhập, xóa bộ đếm sai/khóa tạm và thu hồi mọi phiên cũ | Admin, Staff | Must |
| FR-ACC-08 | Admin cấp quyền tổng hợp liên đơn vị cho một Staff cụ thể | Admin | Must |
| FR-ACC-09 | Admin xem `AuditLog`, lọc theo người, loại thao tác, thời gian | Admin | Must |
| FR-ACC-10 | Admin chọn STAFF/STUDENT/TECHNICIAN và tick quyền theo màn/thao tác; tìm màn, chọn tất cả quyền phù hợp, khôi phục mặc định vào bản nháp, lưu/hủy. Quyền ADMIN bất biến; PROFILE_READ bắt buộc. Lưu có version chống ghi đè và audit trước/sau; thu hồi quyền áp dụng cho mọi tài khoản cùng role từ lần gọi API tiếp theo | Admin | Must |

**Quy tắc phiên tài khoản Keycloak (v0.8):** khóa tài khoản, đổi tên đăng nhập/vai trò/đơn vị quản lý hoặc thay đổi quyền đọc liên đơn vị làm thu hồi các phiên cũ. Mở khóa chỉ cho phép đăng nhập mới, không phục hồi phiên đã thu hồi. Khởi tạo Admin phải bật cấu hình riêng và cung cấp mật khẩu; ứng dụng không tự tạo tài khoản demo và không dùng vai trò mock để vượt qua đăng nhập. Quên mật khẩu được xử lý bằng reset của người quản lý có quyền; chưa có đăng ký tự do hoặc gửi liên kết reset qua email.

**Quy tắc phân quyền phạm vi (Scope) của Staff:**
- Staff chỉ có quyền xem và thao tác dữ liệu thuộc đơn vị quản lý của mình (K1 hoặc N2 + N3), trừ khi được Admin cấp quyền tổng hợp liên đơn vị (FR-ACC-08). Truy cập ngoài phạm vi trả về lỗi **404** (không làm lộ sự tồn tại của tài nguyên ngoài phạm vi).
- Đối với hóa đơn tiền phòng (`ROOM_FEE`): phạm vi phân quyền của Staff được xác định theo **học kỳ (`semester_id`) của hóa đơn**. Staff chỉ có quyền xem hoặc ghi nhận/xác nhận hóa đơn nếu sinh viên có lịch sử lưu trú trong tòa thuộc đơn vị của Staff trong kỳ đó (khoảng thời gian ở giao với khoảng thời gian học kỳ), hoặc đã nộp đơn đăng ký vào đơn vị đó cho đợt thuộc kỳ đó. Quy tắc này đảm bảo khi sinh viên chuyển đơn vị quản lý giữa các học kỳ, Staff đơn vị này không xem hoặc can thiệp vào hóa đơn của học kỳ thuộc đơn vị khác.

### 3.2. Phòng, Giường & Tòa (ROOM)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-ROOM-01 | Admin quản lý danh mục đơn vị quản lý và tòa (mỗi tòa thuộc một đơn vị, có **mức cọc** riêng: K1 1.000.000 đ, N2 / N3 2.000.000 đ). Đổi mức cọc chỉ áp dụng cho hợp đồng kích hoạt sau đó | Admin | Must |
| FR-ROOM-02 | Staff quản lý loại phòng: số giường, **giá gốc theo học kỳ** (có thể khác nhau giữa học kỳ chính và học kỳ hè), diện tích, tiện ích. Đổi giá không ảnh hưởng phân phòng đang hiệu lực; không cho phép cập nhật số giường (`bed_count`) khi loại phòng đã được gán cho ít nhất một phòng đang tồn tại (trả 409 `RESOURCE_IN_USE`) nhằm bảo toàn số giường vật lý và công suất phòng | Staff | Must |
| FR-ROOM-03 | Staff thêm, sửa phòng thuộc đơn vị mình: tòa, tầng, mã phòng, loại phòng, giới tính | Staff | Must |
| FR-ROOM-04 | Hệ thống tự tạo giường theo số giường của loại phòng; Staff có thể thêm, bớt giường | Staff | Must |
| FR-ROOM-05 | Staff đổi trạng thái vận hành phòng (Hoạt động / Bảo trì / Ngừng sử dụng) và giường (Hoạt động / Hỏng), có lý do. Không cho đặt Bảo trì, Ngừng, Hỏng khi phòng / giường còn phân phòng hiệu lực (BR-01) | Staff | Must |
| FR-ROOM-06 | Không xóa phòng, giường đã từng có phân phòng; chỉ chuyển "Ngừng sử dụng" | Staff | Must |
| FR-ROOM-07 | Tra cứu phòng theo tòa, tầng, loại, giới tính, còn chỗ; mỗi phòng hiển thị số giường trống / đang giữ / đang ở (tính ra, không lưu) | Staff | Must |
| FR-ROOM-08 | Sinh viên xem thông tin phòng, giường, bạn cùng phòng (họ tên) và trưởng phòng của mình | Student | Must |
| FR-ROOM-09 | Staff chỉ định **trưởng phòng** là một sinh viên đang ở (phân phòng loại đang ở) trong phòng; mỗi phòng tối đa một trưởng phòng; đổi trưởng phòng được lưu lịch sử | Staff | Must |
| FR-ROOM-10 | Khi trưởng phòng chuyển phòng hoặc trả phòng, chỉ định tự gỡ; phòng còn người ở mà chưa có trưởng phòng hiển thị cảnh báo cho Staff | Hệ thống | Must |
| FR-ROOM-11 | Staff quản lý học kỳ: tên, năm học, loại (HK1 / HK2 / Hè), ngày bắt đầu, ngày kết thúc | Staff | Must |

### 3.3. Đăng ký & Phân phòng (REG)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-REG-01 | Staff tạo đợt đăng ký: học kỳ, ngày mở, ngày đóng, chỉ tiêu theo tòa; đợt có thể áp dụng cho một hoặc cả hai đơn vị | Staff | Must |
| FR-REG-02 | Sinh viên soạn đơn khi đợt đang mở: **đơn vị nguyện vọng**, loại phòng mong muốn, diện ưu tiên tự khai, ghi chú. Mỗi sinh viên tối đa **một đơn mỗi đợt** | Student | Must |
| FR-REG-03 | Không cho nộp đơn khi sinh viên đang có hợp đồng PENDING hoặc ACTIVE (sinh viên đang ở dùng gia hạn, FR-CON-06) | Student | Must |
| FR-REG-04 | Sinh viên nộp đơn (DRAFT → SUBMITTED) hoặc hủy đơn (DRAFT, SUBMITTED, và APPROVED khi chưa sinh hợp đồng PENDING / ACTIVE; đơn APPROVED đã có hợp đồng bị chặn 409 `APPLICATION_HAS_CONTRACT`, xem mục 10.1) | Student | Must |
| FR-REG-05 | Hệ thống tính **điểm ưu tiên** khi nộp theo trọng số cấu hình; Staff xem danh sách đơn của đơn vị mình sắp theo điểm | Hệ thống, Staff | Must ✂ |
| FR-REG-06 | Staff xác nhận diện ưu tiên sinh viên khai (đúng / không đúng); điểm được tính lại theo kết quả xác nhận | Staff | Should |
| FR-REG-07 | Staff duyệt (→ APPROVED) hoặc từ chối (→ REJECTED, bắt buộc lý do) đơn thuộc đơn vị mình | Staff | Must |
| FR-REG-08 | Staff phân giường cho đơn APPROVED: chọn giường trống phù hợp giới tính, loại phòng, phòng Hoạt động thuộc đơn vị mình. Hệ thống thực hiện trong một transaction: tạo phân phòng loại **giữ chỗ** (chụp đơn giá từ loại phòng), tạo hợp đồng PENDING, chuyển đơn sang ASSIGNED | Staff | Must |
| FR-REG-09 | Hai yêu cầu đồng thời vào cùng một giường: chỉ một thành công, yêu cầu còn lại nhận lỗi "Giường vừa được phân, chọn giường khác"; không có bản ghi thừa | Hệ thống | Must |
| FR-REG-10 | Gửi lặp cùng một yêu cầu phân phòng không tạo phân phòng thứ hai (trả lỗi 409) | Hệ thống | Must |
| FR-REG-11 | Staff hủy giữ chỗ để phân lại (ASSIGNED → APPROVED, bắt buộc lý do); sinh viên tự hủy giữ chỗ (ASSIGNED → CANCELLED). Cả hai đóng phân phòng và chuyển hợp đồng sang CANCELLED trong cùng transaction | Staff, Student | Must |
| FR-REG-12 | Sinh viên xem trạng thái đơn, lý do từ chối, giường được giữ và hạn kích hoạt | Student | Must |
| FR-REG-13 | Staff mở **đợt đăng ký học kỳ hè**; khi mở, hệ thống tạo thông báo cho mọi sinh viên có hợp đồng ACTIVE thuộc đơn vị | Staff, Hệ thống | Must |
| FR-REG-14 | Sinh viên đang ở chọn một trong ba lựa chọn cho hè: **ở hè**, **tạm rời hè** (tiếp tục năm học sau), **rời KTX**. Staff duyệt lựa chọn | Student, Staff | Must |
| FR-REG-15 | Sinh viên **ở hè**: khi duyệt, ngày kết thúc hợp đồng được gia hạn qua học kỳ hè (ghi như một gia hạn, FR-CON-06) và sinh viên được phát hành hóa đơn phí phòng học kỳ hè như HK1, HK2. Staff có thể chuyển sinh viên sang giường khác cùng đơn vị để gom phòng ở hè (dùng FR-CON-08) | Staff, Hệ thống | Must |

### 3.4. Hợp đồng (CON)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-CON-01 | Staff kích hoạt hợp đồng PENDING sau khi sinh viên ký giấy và **nộp đủ tiền cọc** theo mức cọc của tòa (hệ thống điền sẵn số tiền). Cùng transaction: ghi giao dịch **thu cọc** vào sổ cọc (số tiền, người xác nhận, thời gian), hợp đồng → ACTIVE, phân phòng đổi loại **giữ chỗ → đang ở trên chính dòng đó** | Staff | Must |
| FR-CON-02 | Hợp đồng PENDING quá **N ngày** (mặc định 7) kể từ lúc phân phòng tự chuyển CANCELLED, đóng phân phòng, đơn chuyển CANCELLED, lý do "tự động hủy" | Hệ thống | Must |
| FR-CON-03 | Hiển thị nhãn **"Sắp hết hạn"** cho hợp đồng ACTIVE còn ≤ **15 ngày** (cấu hình được) | Hệ thống | Must |
| FR-CON-04 | Hiển thị nhãn **"Quá hạn trả phòng"** cho hợp đồng ACTIVE đã qua ngày kết thúc mà chưa làm trả phòng; hợp đồng không tự chuyển EXPIRED (xem BR-02) | Hệ thống | Must |
| FR-CON-05 | Sinh viên và Staff xem hợp đồng: thời hạn, tiền cọc, trạng thái, lịch sử phân phòng (giường, phòng, từ / đến, đơn giá) | Student, Staff | Must |
| FR-CON-06 | Sinh viên gửi yêu cầu gia hạn (ngày kết thúc mới, lý do); Staff duyệt hoặc từ chối. Khi duyệt, cập nhật ngày kết thúc và lưu mốc cũ / mới / người xử lý; từ chối nếu khoảng mới chồng lên hợp đồng khác | Student, Staff | Must ✂ |
| FR-CON-07 | Sinh viên gửi yêu cầu chuyển phòng kèm lý do; Staff chọn giường đích **cùng đơn vị** và duyệt, hoặc từ chối có lý do | Student, Staff | Must |
| FR-CON-08 | Chuyển phòng trong một transaction: khóa giường nguồn và đích theo thứ tự cố định, kiểm tra giường đích trống, đóng phân phòng cũ, mở phân phòng mới (đơn giá của phòng mới), ghi lý do, người duyệt, thời gian. Lỗi bất kỳ bước nào thì không thay đổi dữ liệu | Hệ thống | Must |
| FR-CON-09 | Staff làm thủ tục trả phòng (khi hết hạn, chấm dứt trước hạn hoặc sinh viên chuyển đi): ghi tình trạng tài sản (đạt / hư hỏng kèm ghi chú), xem cảnh báo công nợ, đóng phân phòng, hợp đồng → EXPIRED (đúng hoặc sau ngày kết thúc) hoặc TERMINATED (trước ngày kết thúc), ghi giao dịch **hoàn cọc** bằng số dư cọc hiện tại (nếu dương; số tiền, người xác nhận, thời gian). Số dư cọc âm được ghi thành công nợ | Staff | Must |
| FR-CON-10 | Sau trả phòng, công nợ còn lại (phí phòng chưa đóng, số dư cọc âm) vẫn được theo dõi và hiển thị cho cả Staff và sinh viên. Bù cọc chưa nộp **không** thành công nợ vì đã được tất toán bằng số dư cọc (BR-10) | Hệ thống | Must |
| FR-CON-11 | **Tạm rời hè** (FR-REG-14): điều kiện là hợp đồng đã được gia hạn sang năm học kế tiếp. Staff ghi **phiếu gửi đồ** (số kiện, mô tả, vị trí kho, ngày gửi), đóng phân phòng để giường dùng được trong hè. Hợp đồng vẫn ACTIVE với nhãn **"Tạm rời hè"**; không thu phí hè; tiền cọc giữ nguyên; chỉ định trưởng phòng (nếu có) tự gỡ | Staff | Must |
| FR-CON-12 | Đầu học kỳ 1, Staff phân lại giường cho sinh viên tạm rời hè **không cần đơn mới**, ưu tiên giường cũ nếu còn trống (dùng chung dịch vụ chiếm giường, BR-01), và ghi ngày nhận lại đồ trên phiếu gửi đồ | Staff | Must |
| FR-CON-13 | Sinh viên chọn **rời KTX** cuối học kỳ 2 được làm thủ tục trả phòng (FR-CON-09) | Staff | Must |

### 3.5. Yêu cầu sửa chữa (MNT)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-MNT-01 | Sinh viên đang ở tạo ticket cho **phòng hiện tại**: loại sự cố, mô tả, mức độ (Thấp / Trung bình / Cao), tối đa 3 ảnh [Giả định] | Student | Must |
| FR-MNT-02 | Staff tạo ticket khu vực chung gắn với tòa và tầng, hoặc ticket **của một phòng** thuộc đơn vị mình (ví dụ hư hỏng phát hiện khi kiểm tra trả phòng; trừ cọc vẫn chỉ đi qua ticket, BR-10) | Staff | Must |
| FR-MNT-03 | Staff từ chối (→ REJECTED, có lý do) hoặc giao ticket OPEN cho một KTV **cùng đơn vị** (→ ASSIGNED); sinh viên hủy ticket OPEN của mình (→ CANCELLED) | Staff, Student | Must |
| FR-MNT-04 | KTV xem danh sách việc được giao, chuyển ASSIGNED → IN_PROGRESS → RESOLVED, ghi chú tiến độ; chỉ KTV được giao mới được cập nhật | Technician | Must |
| FR-MNT-05 | Người yêu cầu nghiệm thu: chấp nhận (RESOLVED → CLOSED) hoặc không chấp nhận (→ IN_PROGRESS, bắt buộc lý do). Ticket khu vực chung và ticket do Staff tạo thì Staff nghiệm thu | Student, Staff | Must |
| FR-MNT-06 | Ticket RESOLVED quá **3 ngày** không được nghiệm thu tự chuyển CLOSED, lý do "tự động đóng" | Hệ thống | Must |
| FR-MNT-07 | Nếu người yêu cầu đã trả phòng, Staff nghiệm thu thay, bắt buộc lý do | Staff | Must |
| FR-MNT-08 | Mỗi ticket hiển thị toàn bộ lịch sử chuyển trạng thái: người, thời gian, lý do | Tất cả liên quan | Must |
| FR-MNT-09 | Khi ticket **của phòng** chuyển CLOSED, Staff xác định **nguyên nhân**: *hao mòn tự nhiên* (không trừ cọc, BQL chịu chi phí) hoặc *hư hỏng do sử dụng* (nhập **chi phí sửa chữa** > 0). Ticket khu vực chung không tính phí cho sinh viên | Staff | Must |
| FR-MNT-10 | Với hư hỏng do sử dụng, hệ thống tự ghi giao dịch **trừ cọc**, chia đều cho các sinh viên **chịu trách nhiệm** do Staff chọn trong số sinh viên có phân phòng đang ở trong phòng tại thời điểm tạo ticket (mặc định chọn tất cả; bỏ bớt người thì bắt buộc ghi lý do); phần lẻ làm tròn cộng vào người cuối để tổng khớp; mỗi sinh viên phát sinh một khoản **bù cọc** bằng số bị trừ, hạn nộp 7 ngày (xem FR-BIL-09) | Hệ thống | Must |

### 3.6. Điện nước, Hóa đơn & Tiền cọc (BIL)

Có hai loại hóa đơn và một sổ cọc:

| | Phí phòng | Điện nước | Tiền cọc |
|---|---|---|---|
| Đối tượng | Từng sinh viên | Từng phòng (trưởng phòng nộp) | Từng hợp đồng |
| Chu kỳ | Học kỳ (HK1, HK2, Hè) | Tháng | Thu khi ký, trừ khi sửa chữa, bù, hoàn khi rời KTX |
| Hạn đóng | 7 ngày sau ngày thông báo | 7 ngày sau ngày thông báo | Bù cọc: 7 ngày sau khi bị trừ; quá hạn thì BQL nhắc nhở |
| Phạm vi Staff (Scope) | Theo **học kỳ (`semester_id`) của hóa đơn**: sinh viên có lịch sử lưu trú tại đơn vị trong kỳ hoặc nộp đơn vào đơn vị cho đợt thuộc kỳ | Theo **đơn vị quản lý của phòng** | Theo **hợp đồng** (tòa / đơn vị của hợp đồng) |

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-BIL-01 | Staff cấu hình đơn giá điện (đ/kWh), nước (đ/m³), có ngày hiệu lực | Staff | Must |
| FR-BIL-02 | Staff nhập chỉ số cuối kỳ theo phòng và loại đồng hồ; chỉ số đầu kỳ tự lấy từ chỉ số cuối kỳ trước. Từ chối nếu cuối < đầu | Staff | Must |
| FR-BIL-03 | Staff ghi nhận thay / reset đồng hồ (chỉ số cũ chốt, chỉ số mới bắt đầu, lý do) | Staff | Should |
| FR-BIL-04 | Staff phát hành **hóa đơn điện nước theo phòng** cho kỳ điện nước: tiền điện = tiêu thụ × đơn giá điện, tiền nước = tiêu thụ × đơn giá nước; đơn giá chụp vào dòng hóa đơn. Hóa đơn gửi đến **trưởng phòng**; mọi thành viên đang ở trong phòng xem được. Hệ thống **không** chia cho từng sinh viên | Staff | Must |
| FR-BIL-05 | Staff phát hành **hóa đơn phí phòng theo học kỳ** cho từng sinh viên đang ở trong học kỳ đó (với học kỳ hè: chỉ sinh viên đã đăng ký ở hè): số tiền = đơn giá học kỳ trên phân phòng hiệu lực tại ngày phát hành, **đủ cả học kỳ** kể cả khi vào ở hoặc rời đi giữa kỳ; chuyển phòng giữa kỳ không thu thêm hay hoàn chênh lệch. Đơn giá chụp vào dòng hóa đơn. Phạm vi Staff xem và phát hành xác định theo `semester_id` của hóa đơn (sinh viên có lịch sử ở tại đơn vị trong kỳ hoặc nộp đơn vào đơn vị cho đợt thuộc kỳ) | Staff | Must |
| FR-BIL-06 | Không phát hành trùng: tối đa một hóa đơn phí phòng cho mỗi (sinh viên, học kỳ) và một hóa đơn điện nước cho mỗi (phòng, kỳ điện nước) | Hệ thống | Must |
| FR-BIL-07 | Mọi hóa đơn có hạn đóng = ngày thông báo (phát hành) + **7 ngày** (cấu hình được); khi phát hành, hệ thống tạo thông báo cho người phải nộp. Quá hạn chưa thanh toán hiển thị nhãn **"Quá hạn"** | Hệ thống | Must |
| FR-BIL-08 | Staff xác nhận thanh toán (Chưa thanh toán → Đã thanh toán), lưu người nộp (với hóa đơn điện nước thường là trưởng phòng), người xác nhận, thời gian, hình thức. Hóa đơn đã thanh toán bị khóa chỉnh sửa. Với hóa đơn phí phòng, Staff chỉ được xác nhận nếu hóa đơn thuộc phạm vi đơn vị mình theo học kỳ (`semester_id`) | Staff | Must |
| FR-BIL-09 | **Sổ cọc** cho mỗi hợp đồng gồm các giao dịch: thu cọc (FR-CON-01), trừ cọc (FR-MNT-10), bù cọc, hoàn cọc (FR-CON-09). Staff xác nhận khi sinh viên nộp bù cọc. Số dư cọc tính từ sổ, không lưu | Staff, Hệ thống | Must |
| FR-BIL-10 | Sinh viên xem hóa đơn phí phòng của mình, hóa đơn điện nước của phòng mình, sổ cọc và số tiền cần bù của mình | Student | Must |
| FR-BIL-11 | Staff xem công nợ theo sinh viên (phí phòng, bù cọc khi hợp đồng còn ACTIVE, số dư cọc âm sau trả phòng) và theo phòng (điện nước); xuất báo cáo theo kỳ | Staff | Should |
| FR-BIL-12 | Điều chỉnh sau khi đã thanh toán bằng dòng "Điều chỉnh" (âm hoặc dương) ở hóa đơn kỳ kế tiếp, có lý do | Staff | Should |
| FR-BIL-13 | Khoản bù cọc quá **7 ngày** chưa nộp hiển thị nhãn **"Quá hạn bù cọc"**; hệ thống tạo thông báo nhắc nhở cho sinh viên và đưa vào danh sách cần nhắc của Staff. MVP không áp dụng phạt | Hệ thống, Staff | Must |

### 3.7. Thông báo & Dashboard (NOT)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-NOT-01 | Staff đăng thông báo cho đơn vị mình hoặc theo tòa; Staff có quyền tổng hợp đăng thông báo toàn KTX | Staff | Must |
| FR-NOT-02 | Staff đăng thông báo theo phòng / tầng | Staff | Must ✂ |
| FR-NOT-03 | Người dùng xem thông báo liên quan (toàn KTX, đơn vị, tòa, phòng của mình), đánh dấu đã đọc | Tất cả | Must |
| FR-NOT-04 | Hệ thống tự tạo thông báo trong hệ thống khi: đơn được duyệt / từ chối, được giữ chỗ, hợp đồng sắp hết hạn, hóa đơn mới, bị trừ cọc, ticket đổi trạng thái | Hệ thống | Should |
| FR-NOT-05 | Dashboard của Staff (theo đơn vị; toàn KTX nếu có quyền tổng hợp): tỷ lệ giường đang dùng / sức chứa khai thác, số giường trong phòng bảo trì (riêng), tổng tiền chưa thanh toán, số ticket theo trạng thái, số hợp đồng sắp hết hạn / quá hạn trả phòng, số phòng chưa có trưởng phòng, tổng bù cọc chưa nộp | Staff | Must |
| FR-NOT-06 | Dashboard dạng biểu đồ | Staff | Must ✂ |

---

## 4. Quy tắc nghiệp vụ

| Mã | Quy tắc | Cơ chế bảo vệ |
|---|---|---|
| **BR-01 Sức chứa** | Mỗi giường có tối đa một phân phòng hiệu lực (giữ chỗ hoặc đang ở). Không phân vào phòng Bảo trì / Ngừng, giường Hỏng. Giới tính sinh viên phải khớp giới tính phòng | CSDL: `UNIQUE(bed_id) WHERE ended_at IS NULL`. Service: khóa giường (`SELECT … FOR UPDATE`), kiểm tra lại sau khi khóa |
| **BR-02 Hợp đồng** | Mỗi sinh viên có tối đa một phân phòng hiệu lực và tối đa một hợp đồng PENDING / ACTIVE. Khoảng lưu trú của một sinh viên không chồng nhau. Hợp đồng ACTIVE luôn có một phân phòng đang ở, **trừ khi** đang tạm rời hè (có phiếu gửi đồ chưa nhận lại). Hợp đồng chỉ chuyển EXPIRED / TERMINATED qua thủ tục trả phòng | CSDL: `UNIQUE(student_id) WHERE ended_at IS NULL` trên phân phòng. Service: khóa bản ghi sinh viên khi thay đổi |
| **BR-03 Điện nước** | Chỉ số cuối ≥ đầu; đầu kỳ nối tiếp cuối kỳ trước trừ khi có ghi nhận thay đồng hồ; một chỉ số cho mỗi (phòng, loại đồng hồ, kỳ); hóa đơn điện nước thuộc về phòng, không chia cho sinh viên | CSDL: `UNIQUE(room_id, meter_type, period)`, `CHECK(end_reading >= start_reading)` |
| **BR-04 Hóa đơn** | Một hóa đơn phí phòng cho mỗi (sinh viên, học kỳ); một hóa đơn điện nước cho mỗi (phòng, kỳ điện nước). Hóa đơn đã thanh toán không sửa | CSDL: hai partial unique index theo loại hóa đơn. Service: chặn sửa khi đã thanh toán |
| **BR-05 Chuyển phòng** | Giường đích trống và **cùng đơn vị** với giường nguồn; đóng phân phòng cũ và mở mới trong một transaction; hợp đồng không thay đổi | Service: khóa hai giường theo thứ tự `bed_id` tăng dần |
| **BR-06 Sửa chữa** | Sinh viên chỉ tạo ticket cho phòng đang ở; chỉ KTV được giao cập nhật xử lý; chỉ đóng sau RESOLVED; KTV không tự đóng | Service: kiểm tra chuyển trạng thái theo bảng ở mục 10 |
| **BR-07 Phạm vi** | Staff chỉ thấy và thao tác dữ liệu thuộc đơn vị mình. Đơn đăng ký thuộc đơn vị nguyện vọng. Sinh viên thuộc phạm vi đơn vị khi có đơn trong đợt hiện hành hoặc phân phòng hiệu lực tại đơn vị đó. Hóa đơn phí phòng thuộc phạm vi đơn vị theo học kỳ (`semester_id`) của hóa đơn (sinh viên có lịch sử ở tại đơn vị trong học kỳ hoặc nộp đơn vào đơn vị cho đợt thuộc học kỳ, tránh lộ hóa đơn khi chuyển đơn vị giữa các kỳ); quyền ghi (xác nhận thanh toán) chỉ tính phân phòng/phòng thực tế của đúng đơn vị. Admin được làm mọi nghiệp vụ ở mọi đơn vị, vẫn kiểm tra trạng thái và ràng buộc dữ liệu | Service: mọi truy vấn của Staff lọc theo `management_unit_id`; kiểm tra trên từng đối tượng |
| **BR-08 Dữ liệu suy ra** | Số chỗ trống, "Đầy", "Sắp hết hạn", "Quá hạn", "Quá hạn trả phòng" không lưu, luôn tính từ dữ liệu gốc | Thiết kế CSDL / view |
| **BR-09 Lịch sử** | Không ghi đè dữ liệu lịch sử; mọi chuyển trạng thái lưu người, thời gian, lý do | Bảng lịch sử cho từng đối tượng + `AuditLog` |
| **BR-10 Tiền cọc** | Hợp đồng chỉ ACTIVE khi đã thu đủ mức cọc của tòa. Số dư cọc = tổng giao dịch sổ cọc. Trừ cọc chỉ phát sinh từ ticket của phòng đã CLOSED, nguyên nhân hư hỏng do sử dụng, có chi phí > 0; hao mòn tự nhiên không trừ cọc; tổng trừ của các sinh viên = chi phí ticket. Hoàn cọc ≤ số dư; mỗi hợp đồng tối đa một giao dịch hoàn cọc. Khi trả phòng, khoản bù cọc chưa nộp được tất toán bằng số dư (hoàn = số dư nếu dương, số dư âm thành công nợ), không ghi thêm nợ bù cọc | Service trong transaction; CSDL: `CHECK` dấu số tiền theo loại giao dịch, `UNIQUE(contract_id) WHERE type = 'REFUND'` |
| **BR-11 Trưởng phòng** | Trưởng phòng phải là sinh viên có phân phòng đang ở trong chính phòng đó; mỗi phòng tối đa một trưởng phòng | Service kiểm tra; gỡ tự động khi phân phòng của trưởng phòng đóng |

---

## 5. Yêu cầu dữ liệu

### 5.1. Thực thể

Mục này mô tả ở mức nghiệp vụ. Tên bảng, kiểu dữ liệu và ràng buộc chi tiết: `DB-Design-DMS.md` v0.7 và `DB-DMS-schema.sql`.

| Thực thể | Thuộc tính chính | Ghi chú |
|---|---|---|
| `ManagementUnit` | id, tên | K1; N2 + N3 |
| `User` | id, username, password_hash, role, management_unit_id (Staff, Technician), họ tên, SĐT, cờ đổi mật khẩu, trạng thái khóa, quyền tổng hợp liên đơn vị (Staff), `security_version` | Sinh viên dùng mã SV làm username; phiên giữ phiên bản lúc đăng nhập để kiểm tra thu hồi |
| `StudentProfile` | user_id, mã SV, giới tính, ngày sinh, khoa, khóa, quê quán, liên hệ khẩn cấp | 1–1 với `User` có role Student; mọi thực thể nghiệp vụ tham chiếu sinh viên qua đây |
| `Building` | id, tên, management_unit_id, **mức cọc** | K1: 1.000.000 đ; N2, N3: 2.000.000 đ |
| `RoomType` | id, tên, số giường, **giá gốc / học kỳ chính**, **giá gốc / học kỳ hè**, diện tích, tiện ích | Nơi duy nhất lưu giá gốc. Không sửa số giường khi loại phòng đã được gán cho phòng đang tồn tại (409 `RESOURCE_IN_USE`) |
| `Semester` | id, tên, năm học, loại (HK1 / HK2 / Hè), ngày bắt đầu, ngày kết thúc | Kỳ thu phí phòng |
| `Room` | id, building_id, room_type_id, tầng, mã phòng, giới tính, trạng thái vận hành, **room_leader_id** | Không lưu giá, số người. Đổi trưởng phòng ghi `AuditLog` |
| `Bed` | id, room_id, mã giường, trạng thái (Hoạt động / Hỏng / Ngừng dùng) | Bớt giường bằng trạng thái Ngừng dùng, không xóa |
| `RegistrationPeriod` | id, học kỳ, loại (thường / hè), ngày mở, ngày đóng, các đơn vị áp dụng | Chỉ tiêu lưu ở bảng phụ (period, building, quota) |
| `PriorityCategory` | id, mã, tên diện, trọng số, đang dùng | Khởi tạo: chính sách 50, năm nhất 30, ở xa 20 |
| `RoomApplication` | id, student_id, period_id, **preferred_unit_id**, room_type_id mong muốn, điểm ưu tiên, trạng thái, lý do | Duy nhất (student_id, period_id). Diện ưu tiên kê khai và Staff xác nhận từng diện ở bảng phụ (application, category). Đơn APPROVED chỉ hủy khi chưa có hợp đồng liên kết (409 `APPLICATION_HAS_CONTRACT`) |
| `DormitoryContract` | id, **mã** (HD-2026-0012), student_id, application_id, ngày bắt đầu, ngày kết thúc, **hạn kích hoạt** (chụp khi phân giường), **mức cọc yêu cầu** (chụp từ tòa khi kích hoạt), activated_at, trạng thái | **Không** lưu phòng, giá, số dư cọc. Hủy giữ chỗ để phân lại tạo hợp đồng mới |
| `DepositTransaction` | id, contract_id, loại (thu cọc / trừ cọc / bù cọc / hoàn cọc), số tiền (có dấu), maintenance_request_id (khi trừ cọc), hạn nộp (khi trừ cọc), người xác nhận, thời gian, ghi chú | Sổ cọc; số dư = tổng |
| `RoomAssignmentHistory` | id, student_id, contract_id, bed_id, started_at, thời điểm bắt đầu ở (HOLD → STAY trên cùng bản ghi), ended_at, lý do kết thúc, loại (HOLD / STAY), **đơn giá / học kỳ** (chính và hè), người duyệt | Nguồn duy nhất cho "ở đâu, giá bao nhiêu" |
| `ContractExtensionRequest` | id, contract_id, nguồn (sinh viên / ở hè), ngày kết thúc cũ / mới, lý do, trạng thái, người xử lý | Ở hè được ghi như một gia hạn |
| `RoomTransferRequest` | id, contract_id, lý do, giường đích, trạng thái, người xử lý | |
| `MoveOutRecord` | id, contract_id, tình trạng tài sản, ghi chú, công nợ tại thời điểm trả, tiền hoàn cọc, người xác nhận, thời gian | 1–1 với hợp đồng |
| `MaintenanceRequest` | id, **mã** (TK-1024), requester_id, room_id hoặc (building_id, tầng), assigned_to, loại sự cố, mức độ, mô tả, trạng thái, **nguyên nhân** (hao mòn tự nhiên / hư hỏng do sử dụng), **chi phí sửa chữa** | Nguyên nhân, chi phí chỉ có ở ticket của phòng. Ảnh sự cố (≤ 3) ở bảng phụ |
| `SummerChoice` | id, contract_id, semester_id (hè), lựa chọn (ở hè / tạm rời hè / rời KTX), trạng thái, người duyệt | Một lựa chọn cho mỗi (hợp đồng, học kỳ hè) |
| `StorageRecord` | id, contract_id, số kiện, mô tả, vị trí kho, giường cũ, ngày gửi, người nhận gửi, ngày nhận lại, người trả | Phiếu gửi đồ khi tạm rời hè; giường cũ dùng để gợi ý khi phân lại đầu HK1 |
| `StatusHistory` | id, loại đối tượng (đơn / hợp đồng / ticket / yêu cầu…), id đối tượng, từ trạng thái, đến trạng thái, người, thời gian, lý do | Dùng chung cho mọi đối tượng có trạng thái; dữ liệu cho timeline |
| `UtilityRate` | id, loại (điện / nước), đơn giá, hiệu lực từ | |
| `UtilityReading` | id, room_id, loại đồng hồ, kỳ, chỉ số đầu, chỉ số cuối, khi thay đồng hồ: chỉ số chốt đồng hồ cũ, chỉ số đầu đồng hồ mới, lý do; **tiêu thụ** (CSDL tự tính) | Duy nhất (phòng, loại đồng hồ, kỳ) |
| `Invoice` | id, **mã** (HP-2026-0001 phí phòng / DN-2026-0002 điện nước), **loại**, student_id + semester_id (phí phòng) hoặc room_id + kỳ điện nước (điện nước), tổng tiền, ngày thông báo, hạn đóng, trạng thái, người nộp (ghi lúc xác nhận thanh toán), phương thức, người xác nhận, thời gian thanh toán | Duy nhất theo loại (BR-04). Tổng tiền = tổng các dòng, tính cùng lúc tạo hóa đơn |
| `InvoiceItem` | id, invoice_id, loại phí, số lượng, đơn giá, thành tiền, nguồn (phân phòng / chỉ số điện nước / hóa đơn được điều chỉnh), ghi chú | |
| `Notification` | id, phạm vi (toàn KTX / đơn vị / tòa / tầng / phòng / người dùng), đối tượng nhận theo phạm vi (đơn vị, tòa + tầng, phòng, người dùng), tiêu đề, nội dung, nguồn (thông báo tự động), người đăng, thời gian | Bảng phụ lưu trạng thái đã đọc. Sinh viên thấy thông báo theo phòng / tầng / tòa / đơn vị đăng trong lúc mình đang ở đó |
| `AuditLog` | id, user_id, hành động, loại đối tượng, đối tượng, giá trị cũ / mới, lý do, thời gian | Chỉ ghi thêm |
| `SystemConfig` | khóa, giá trị, mô tả | Tham số cấu hình (mục 2.6): số ngày kích hoạt, ngưỡng sắp hết hạn, hạn đóng… |

### 5.2. View hỗ trợ

- `v_current_stay`: hợp đồng PENDING / ACTIVE + phân phòng hiệu lực + giường + phòng + tòa + đơn vị. Dùng cho tra cứu "sinh viên đang ở đâu" và lọc phạm vi.
- `v_room_occupancy`: theo phòng, số giường Hoạt động / đang giữ / đang ở / trống, trưởng phòng.
- `v_deposit_balance`: theo hợp đồng, mức cọc yêu cầu, số dư cọc, số tiền cần bù, khoản bù quá hạn.
- `v_summer_leave`: sinh viên đang tạm rời hè, phiếu gửi đồ, giường cũ.
- `v_contract_label`: hợp đồng ACTIVE kèm số ngày còn lại và nhãn Sắp hết hạn / Quá hạn trả phòng / Tạm rời hè.
- `v_user_notification`: thông báo mỗi người dùng được xem, kèm trạng thái đã đọc (FR-NOT-03).

### 5.3. Lưu trữ và bảo vệ dữ liệu

- Dữ liệu cá nhân (ngày sinh, SĐT, diện ưu tiên) chỉ hiển thị cho chính sinh viên và Staff cùng đơn vị.
- Dữ liệu không bị xóa cứng khi đã phát sinh nghiệp vụ; dùng trạng thái ngừng / khóa.
- Sao lưu CSDL tự động hằng ngày, giữ 7 bản gần nhất [Giả định].

---

## 6. Yêu cầu giao diện ngoài

### 6.1. Giao diện người dùng

- Mỗi vai trò có cổng riêng sau đăng nhập: Student (Phòng của tôi, Đơn, Hợp đồng, Hóa đơn, Báo hỏng, Thông báo), Staff (Dashboard, Đơn, Phòng, Hợp đồng, Điện nước, Hóa đơn, Sửa chữa, Thông báo), Technician (Việc của tôi), Admin (Tài khoản, Danh mục, Nhật ký).
- Màn hình danh sách có phân trang, lọc theo trạng thái; trạng thái hiển thị bằng nhãn màu thống nhất.
- Thao tác chuyển trạng thái không thể hoàn tác (từ chối, hủy, trả phòng, xác nhận thanh toán) có hộp xác nhận; những thao tác cần lý do bắt buộc nhập lý do.
- Thông báo lỗi bằng tiếng Việt, nói rõ lý do và cách xử lý (ví dụ "Giường A101-4 vừa được phân cho sinh viên khác, vui lòng chọn giường khác").

### 6.2. Giao diện phần mềm (REST API)

- JSON qua HTTPS; tiền tố `/api`. Hành động chuyển trạng thái dùng `POST /api/<tài nguyên>/{id}/<hành động>` (ví dụ `/approve`, `/reject`, `/activate`, `/move-out`).
- Mã lỗi: 400 dữ liệu không hợp lệ, 401 chưa đăng nhập, 403 ngoài quyền / phạm vi, 404 không tồn tại **hoặc ngoài phạm vi đọc**, 409 xung đột trạng thái / ràng buộc (giường đã bị chiếm, gửi trùng).
- Xác thực Bearer JWT trả `SESSION_REVOKED` (401) khi token không còn khớp phiên bản bảo mật. API không xác thực bằng cookie phiên; state/nonce/PKCE thuộc luồng Keycloak. Mọi lỗi bảo mật có `requestId`. Giao diện yêu cầu đăng nhập lại khi hết hạn/thu hồi và không giữ dữ liệu của tài khoản trước.
- Lỗi 400 / 409 trả về `errorCode` ổn định (ví dụ `BED_TAKEN`, `DUPLICATE_INVOICE`, `CONTRACT_AUTO_CANCELLED`) kèm dữ liệu chi tiết; giao diện dựng thông báo tiếng Việt từ mã, không dựa vào chuỗi `message`.
- API chi tiết của đơn, hợp đồng, ticket, hóa đơn và yêu cầu trả về `allowedActions`: các hành động người dùng hiện tại được thực hiện ở trạng thái hiện tại. Giao diện chỉ hiển thị nút theo danh sách này; API vẫn kiểm tra lại khi nhận request.
- Danh sách endpoint chi tiết thuộc tài liệu đặc tả API (tuần 3–4); tối thiểu bao phủ mọi FR trong mục 3.

### 6.3. Giao diện phần cứng và truyền thông

Không có tích hợp phần cứng. Không tích hợp hệ thống bên ngoài trong MVP; dữ liệu sinh viên vào bằng file Excel (.xlsx).

---

## 7. Yêu cầu phi chức năng

| Mã | Yêu cầu | Cách kiểm chứng |
|---|---|---|
| NFR-SEC-01 | Mật khẩu do Keycloak quản lý và băm; DMS không lưu bản sao mật khẩu/hash của tài khoản đã liên kết. Không đưa secret vào frontend, log hoặc Git | Review code, kiểm tra CSDL |
| NFR-SEC-02 | Phân quyền kiểm tra ở API trên từng đối tượng theo vai trò + phạm vi + trạng thái | Integration test truy cập chéo đơn vị |
| NFR-SEC-03 | Validate mọi input phía server; chống SQL injection (truy vấn tham số hóa) và XSS (escape đầu ra) | Test với input độc hại |
| NFR-SEC-04 | Phiên SSO Keycloak hết hạn sau 30 phút không hoạt động; access token 5 phút, chỉ giữ trong bộ nhớ. API DMS nhận Bearer JWT, kiểm tra issuer/audience/chữ ký/thời hạn. Khóa/reset/quyền/logout từ DMS chặn token cũ qua security_version | Kiểm thử cookie và token thật, nhiều phiên và request đồng thời |
| NFR-SEC-05 | Thông tin bí mật (mật khẩu CSDL, khóa) cấu hình qua biến môi trường, không nằm trong mã nguồn | Review repo |
| NFR-PER-01 | 95% request phản hồi dưới 2 giây với 100 người dùng đồng thời, dữ liệu ~1.500 sinh viên | Kiểm thử tải (JMeter / k6) kịch bản nộp đơn + tra cứu |
| NFR-PER-02 | Phát hành hóa đơn phí phòng cho toàn KTX (~1.500 sinh viên) hoàn tất dưới 30 giây | Đo thời gian chạy |
| NFR-REL-01 | Không phân phòng vượt sức chứa kể cả khi có yêu cầu đồng thời | Test tranh chấp đồng thời + test chèn trực tiếp vào CSDL |
| NFR-REL-02 | Phân phòng, kích hoạt, chuyển phòng, trả phòng, phát hành hóa đơn là nguyên tử: lỗi giữa chừng không để lại dữ liệu dở dang | Test chèn lỗi giữa nghiệp vụ |
| NFR-REL-03 | Tác vụ định kỳ (tự hủy hợp đồng PENDING, tự đóng ticket, nhắc bù cọc quá hạn) chạy ít nhất mỗi giờ, chạy lại an toàn (idempotent), ghi `AuditLog` với người thao tác "SYSTEM" | Unit test + kiểm tra log |
| NFR-USA-01 | Giao diện dùng được trên màn hình từ 360 px (sinh viên, KTV) | Kiểm thử trên điện thoại |
| NFR-USA-02 | Sinh viên mới hoàn tất nộp đơn trong ≤ 5 phút không cần hướng dẫn | Thử với 3 sinh viên |
| NFR-MNT-01 | Backend chia module theo phân hệ; logic nghiệp vụ chỉ ở Service | Review kiến trúc |
| NFR-MNT-02 | Độ phủ unit test cho tầng Service ≥ 60% | Báo cáo JaCoCo |
| NFR-MNT-03 | Log có mã yêu cầu (request id) để truy vết lỗi | Kiểm tra log |

---

## 8. Tiêu chí nghiệm thu và truy vết

### 8.1. Kịch bản nghiệm thu bắt buộc

| Mã | Kịch bản | Kết quả mong đợi | Yêu cầu liên quan |
|---|---|---|---|
| AT-01 | Luồng đầy đủ: nộp đơn → duyệt → phân giường → kích hoạt → tra cứu | Hợp đồng ACTIVE, phân phòng loại đang ở, sinh viên thấy đúng phòng | FR-REG-02..08, FR-CON-01 |
| AT-02 | Phòng A101 4 giường, 3 đã có người; gửi đồng thời 2 yêu cầu phân vào giường còn lại | Một thành công, một nhận 409; 4/4 giường; không bản ghi thừa | FR-REG-09, BR-01, NFR-REL-01 |
| AT-03 | Chèn trực tiếp hai phân phòng hiệu lực vào cùng một giường bằng SQL | CSDL từ chối | BR-01 |
| AT-04 | Gửi lặp yêu cầu phân phòng cho cùng sinh viên | Lần hai nhận 409 | FR-REG-10, BR-02 |
| AT-05 | Hợp đồng PENDING quá N ngày | Tự CANCELLED, giường trống, đơn CANCELLED | FR-CON-02 |
| AT-06 | Staff K1 đọc / sửa đơn, phòng, hóa đơn của N2 | Nhận 403 / 404 | BR-07, NFR-SEC-02 |
| AT-07 | Chuyển phòng có chèn lỗi giữa chừng | Dữ liệu không đổi | FR-CON-08, NFR-REL-02 |
| AT-08 | Chuyển phòng sang giường thuộc đơn vị khác | Bị từ chối | BR-05 |
| AT-09 | Gia hạn với ngày kết thúc chồng lên hợp đồng khác | Bị từ chối | FR-CON-06, BR-02 |
| AT-10 | Trả phòng khi còn hóa đơn phí phòng chưa đóng | Hiển thị cảnh báo; công nợ phí phòng vẫn được theo dõi sau trả phòng | FR-CON-09, FR-CON-10 |
| AT-11 | Ticket: OPEN → CLOSED trực tiếp; KTV tự đóng ticket | Bị từ chối | BR-06 |
| AT-12 | Ticket RESOLVED quá 3 ngày | Tự CLOSED, lý do "tự động đóng" | FR-MNT-06 |
| AT-13 | Nhập chỉ số cuối nhỏ hơn chỉ số đầu | Bị từ chối | FR-BIL-02, BR-03 |
| AT-14 | Phòng A101 (K1), 3 sinh viên đang ở, ticket hỏng vòi nước chi phí 100.000 đ được CLOSED | Mỗi sinh viên bị trừ 33.333 / 33.333 / 33.334 đ (tổng 100.000); mỗi người có khoản bù cọc tương ứng, hạn 7 ngày | FR-MNT-09, FR-MNT-10, BR-10 |
| AT-15 | Phát hành hóa đơn phí phòng hai lần cho cùng học kỳ; hóa đơn điện nước hai lần cho cùng phòng, cùng kỳ | Lần hai không tạo hóa đơn mới | FR-BIL-06, BR-04 |
| AT-16 | Sửa hóa đơn đã thanh toán | Bị từ chối | FR-BIL-08 |
| AT-17 | Sinh viên đang có hợp đồng ACTIVE nộp đơn đợt mới | Bị từ chối, gợi ý dùng gia hạn | FR-REG-03 |
| AT-18 | Kích hoạt hợp đồng ở N2 khi sinh viên mới nộp 1.000.000 đ | Bị từ chối (mức cọc N2 là 2.000.000 đ) | FR-CON-01, BR-10 |
| AT-19 | Sinh viên K1 bị trừ 200.000 đ, chưa bù, làm trả phòng | Hoàn cọc 800.000 đ; không còn công nợ bù cọc | FR-CON-09, FR-CON-10, BR-10 |
| AT-20 | Phát hành hóa đơn điện nước cho phòng có trưởng phòng | Một hóa đơn gắn với phòng; trưởng phòng nhận thông báo; các thành viên xem được; hạn đóng = ngày thông báo + 7 ngày | FR-BIL-04, FR-BIL-07 |
| AT-21 | Trưởng phòng trả phòng | Phòng mất chỉ định trưởng phòng, Staff thấy cảnh báo | FR-ROOM-10, BR-11 |
| AT-22 | Ticket của phòng CLOSED, Staff chọn nguyên nhân hao mòn tự nhiên | Không phát sinh trừ cọc | FR-MNT-09, BR-10 |
| AT-23 | Khoản bù cọc quá 7 ngày chưa nộp | Nhãn "Quá hạn bù cọc", sinh viên nhận thông báo nhắc, có trong danh sách của Staff | FR-BIL-13 |
| AT-24 | Sinh viên chọn tạm rời hè | Có phiếu gửi đồ; giường trống trong hè; hợp đồng vẫn ACTIVE, nhãn "Tạm rời hè"; không có hóa đơn hè; cọc giữ nguyên | FR-CON-11, BR-02 |
| AT-25 | Đầu HK1, phân lại giường cho sinh viên tạm rời hè khi giường cũ đã có người | Phân được giường khác, không cần đơn mới; phiếu gửi đồ ghi ngày nhận lại | FR-CON-12, BR-01 |
| AT-26 | Sinh viên đăng ký ở hè được duyệt | Ngày kết thúc hợp đồng gia hạn qua hè; có hóa đơn phí phòng học kỳ hè | FR-REG-15, FR-BIL-05 |
| AT-27 | Đăng nhập đúng bằng từng vai trò; gửi vai trò giả từ client | Quyền lấy từ CSDL, trường lạ bị từ chối; không thể nâng quyền | FR-ACC-01, NFR-SEC-02 |
| AT-28 | Đổi/reset mật khẩu khi tài khoản có nhiều phiên; khóa rồi mở khóa tài khoản | Phiên cũ mất hiệu lực; chỉ đăng nhập mới được dùng; mật khẩu tạm bắt đổi | FR-ACC-03, 06, 07, NFR-SEC-04 |
| AT-29 | Staff reset sinh viên chỉ có đơn hết đợt hoặc phân phòng đã kết thúc, kể cả có quyền đọc liên đơn vị | Bị từ chối; chỉ đơn trong đợt đang mở/phân phòng hiệu lực đúng đơn vị cho phép reset | FR-ACC-07, BR-07 |
| AT-30 | Đăng nhập PKCE, token sai issuer/audience/chữ ký, logout DMS và gọi lại token cũ | Token sai bị từ chối; token cũ sau logout bị DMS chặn | FR-ACC-01, NFR-SEC-04 |
| AT-31 | Token có username giống tài khoản DMS nhưng subject chưa liên kết | Trả IDENTITY_NOT_LINKED, không tự nhận hồ sơ/quyền bằng username | FR-ACC-03, NFR-SEC-01 |
| AT-32 | Sinh viên hủy đơn APPROVED khi đã sinh hợp đồng PENDING / ACTIVE | Bị từ chối (409 `APPLICATION_HAS_CONTRACT`), ẩn thao tác `cancel` trong `allowedActions` | FR-REG-04, mục 10.1 |
| AT-33 | Sửa số giường (`bed_count`) của loại phòng đã được gán cho ít nhất một phòng đang tồn tại | Bị từ chối (409 `RESOURCE_IN_USE`) | FR-ROOM-02 |

### 8.2. Ma trận Use Case ↔ Yêu cầu

| Use Case | Yêu cầu chức năng |
|---|---|
| UC-01 Login | FR-ACC-01, 02, 03 |
| UC-02 Update Profile | FR-ACC-04 |
| UC-03 View Room | FR-ROOM-08 |
| UC-04 Submit Room Application | FR-REG-02, 03, 04, 05 |
| UC-05 View Application Status | FR-REG-12 |
| UC-06 View Contract | FR-CON-05 |
| UC-07 View Invoice & Deposit | FR-BIL-10 |
| UC-08 Submit Maintenance Request | FR-MNT-01 |
| UC-09 Track / Accept Maintenance Request | FR-MNT-05, 08 |
| UC-10 Submit Room Transfer Request | FR-CON-07 |
| UC-11 Submit Contract Extension Request | FR-CON-06 |
| UC-12 View Notification | FR-NOT-03 |
| UC-13 Manage Rooms | FR-ROOM-02..07, 09, 11 |
| UC-14 Manage Students | FR-ACC-07, BR-07 |
| UC-15 Review Room Application | FR-REG-05, 06, 07 |
| UC-16 Assign Room | FR-REG-08..11 |
| UC-17 Manage Contract | FR-CON-03, 04, 05 |
| UC-18 Review Room Transfer Request | FR-CON-07, 08 |
| UC-19 Review Contract Extension | FR-CON-06 |
| UC-20 Record Utility Reading | FR-BIL-01, 02, 03 |
| UC-21 Generate Invoice / Confirm Payment | FR-BIL-04..08, 11, 12 |
| UC-22 Manage Maintenance Request | FR-MNT-02, 03, 07, 09, 10 |
| UC-23 Publish Notification | FR-NOT-01, 02 |
| UC-24 View Dashboard | FR-NOT-05, 06 |
| UC-25..27 (Technician) | FR-MNT-04 |
| UC-28 Manage Accounts | FR-ACC-05, 06, 07 |
| UC-29 Manage Roles & Scopes | FR-ACC-06, 08, 10, FR-ROOM-01 |
| UC-30 View Audit Log | FR-ACC-09 |
| UC-31 Manage Registration Period | FR-REG-01 |
| UC-32 Activate Contract | FR-CON-01, 02 |
| UC-33 Process Move-out | FR-CON-09, 10 |
| UC-34 Cancel Room Hold *(mới)* | FR-REG-11 |
| UC-35 Assign Room Leader *(mới)* | FR-ROOM-09, 10 |
| UC-36 Manage Deposit *(mới)* | FR-BIL-09, 13, FR-CON-01, 09 |
| UC-37 Submit Summer Choice *(mới)* | FR-REG-13, 14 |
| UC-38 Process Summer Choice / Storage *(mới)* | FR-REG-15, FR-CON-11, 12, 13 |

### 8.3. Đặc tả chi tiết Use Case trọng tâm

#### UC-16 Assign Room

| Mục | Nội dung |
|---|---|
| Tác nhân | Staff |
| Tiền điều kiện | Đơn ở APPROVED, thuộc đơn vị của Staff; sinh viên chưa có phân phòng hiệu lực |
| Luồng chính | 1. Staff mở đơn, hệ thống gợi ý danh sách giường trống phù hợp (đơn vị, giới tính, loại phòng). 2. Staff chọn giường, xác nhận. 3. Hệ thống khóa sinh viên và giường, kiểm tra lại, tạo phân phòng giữ chỗ (chụp đơn giá), tạo hợp đồng PENDING, đơn → ASSIGNED, commit. 4. Hệ thống tạo thông báo cho sinh viên kèm hạn kích hoạt |
| Luồng thay thế | 3a. Giường vừa bị chiếm: rollback, báo lỗi, đơn giữ APPROVED, quay lại bước 1. 3b. Phòng chuyển Bảo trì / giường Hỏng: rollback, báo lỗi |
| Hậu điều kiện | Một phân phòng HOLD hiệu lực, một hợp đồng PENDING, đơn ASSIGNED |

#### UC-32 Activate Contract

| Mục | Nội dung |
|---|---|
| Tác nhân | Staff |
| Tiền điều kiện | Hợp đồng PENDING thuộc đơn vị của Staff, chưa quá hạn kích hoạt; sinh viên đã ký giấy |
| Luồng chính | 1. Staff mở hợp đồng; hệ thống hiển thị mức cọc của tòa. 2. Staff xác nhận đã thu đủ cọc. 3. Hệ thống trong một transaction: ghi giao dịch thu cọc, chụp mức cọc vào hợp đồng, hợp đồng → ACTIVE, ghi activated_at; phân phòng HOLD → STAY trên cùng dòng |
| Luồng thay thế | 2a. Sinh viên chưa nộp đủ cọc: không kích hoạt. 3a. Hợp đồng vừa bị tự hủy: báo lỗi, không thay đổi |
| Hậu điều kiện | Hợp đồng ACTIVE, phân phòng STAY |

#### UC-18 Review Room Transfer Request

| Mục | Nội dung |
|---|---|
| Tác nhân | Staff (khởi tạo bởi Student qua UC-10) |
| Tiền điều kiện | Yêu cầu chuyển phòng đang chờ; hợp đồng ACTIVE thuộc đơn vị của Staff |
| Luồng chính | 1. Staff xem lý do, chọn giường đích trống **cùng đơn vị**. 2. Hệ thống khóa hai giường theo thứ tự `bed_id`, kiểm tra giường đích, đóng phân phòng cũ, mở phân phòng STAY mới với đơn giá phòng mới, yêu cầu → đã duyệt, commit |
| Luồng thay thế | 1a. Staff từ chối, nhập lý do. 2a. Giường đích vừa bị chiếm: rollback, báo lỗi |
| Hậu điều kiện | Hai khoảng phân phòng liên tiếp; hợp đồng không đổi |

#### UC-33 Process Move-out

| Mục | Nội dung |
|---|---|
| Tác nhân | Staff |
| Tiền điều kiện | Hợp đồng ACTIVE thuộc đơn vị của Staff |
| Luồng chính | 1. Staff ghi tình trạng tài sản. 2. Hệ thống hiển thị công nợ phí phòng chưa đóng và số dư cọc; khoản bù cọc chưa nộp hiển thị kèm ghi chú "được tất toán bằng số dư". 3. Staff xác nhận hoàn cọc bằng số dư cọc. 4. Hệ thống trong một transaction: ghi giao dịch hoàn cọc, đóng phân phòng, gỡ chỉ định trưởng phòng nếu có, hợp đồng → EXPIRED (nếu ngày trả ≥ ngày kết thúc) hoặc TERMINATED, tạo `MoveOutRecord` |
| Hậu điều kiện | Giường trống; công nợ phí phòng và số dư cọc âm (nếu có) vẫn theo dõi; không còn khoản cần bù; ticket OPEN / RESOLVED của sinh viên chuyển cho Staff nghiệm thu thay |

#### UC-21 Generate Invoice

| Mục | Nội dung |
|---|---|
| Tác nhân | Staff |
| Tiền điều kiện | Phí phòng: học kỳ đã được tạo. Điện nước: đã nhập đủ chỉ số kỳ cho các phòng có người ở của đơn vị |
| Luồng chính (phí phòng) | 1. Staff chọn học kỳ, xem trước danh sách. 2. Hệ thống lập cho mỗi sinh viên đang ở một hóa đơn theo đơn giá học kỳ trên phân phòng hiệu lực. 3. Staff xác nhận phát hành; hệ thống tạo hóa đơn trong một transaction, bỏ qua sinh viên đã có hóa đơn học kỳ đó, đặt hạn đóng = hôm nay + 7 ngày, tạo thông báo |
| Luồng chính (điện nước) | 1. Staff chọn kỳ điện nước. 2. Hệ thống tính tiền điện, nước cho từng phòng. 3. Staff xác nhận phát hành; mỗi phòng một hóa đơn, thông báo gửi trưởng phòng và các thành viên |
| Luồng thay thế | 1a. Còn phòng thiếu chỉ số: liệt kê và cảnh báo; phòng thiếu chỉ số được bỏ qua, các phòng đủ chỉ số vẫn phát hành; phát hành lại sau khi nhập đủ (không tạo trùng, FR-BIL-06). 3a. Phòng chưa có trưởng phòng: vẫn phát hành, thông báo gửi mọi thành viên, cảnh báo Staff |
| Hậu điều kiện | Mỗi sinh viên đang ở có đúng một hóa đơn phí phòng / học kỳ; mỗi phòng có người ở có đúng một hóa đơn điện nước / kỳ |

#### UC-22 (mở rộng) Close Maintenance Request with Cost

| Mục | Nội dung |
|---|---|
| Tác nhân | Staff |
| Tiền điều kiện | Ticket của phòng ở CLOSED (sinh viên nghiệm thu hoặc tự đóng) |
| Luồng chính | 1. Staff chọn nguyên nhân; nếu là hư hỏng do sử dụng thì nhập chi phí sửa chữa. 2. Với hư hỏng do sử dụng, hệ thống trong một transaction chia đều chi phí cho sinh viên đang ở tại thời điểm tạo ticket, ghi giao dịch trừ cọc cho từng người kèm hạn bù 7 ngày, tạo thông báo |
| Luồng thay thế | 1a. Hao mòn tự nhiên: bắt buộc ghi chú, không trừ cọc |
| Hậu điều kiện | Tổng các giao dịch trừ cọc = chi phí ticket |

*Đặc tả chi tiết các UC còn lại sẽ bổ sung ở phiên bản 0.2.*

---

## 9. Vấn đề mở và giả định cần xác nhận

### 9.1. Đã xác nhận (07/10/2026)

| # | Vấn đề | Kết luận | Mục bị ảnh hưởng |
|---|---|---|---|
| Q-01 | Kỳ thu phí phòng | Theo học kỳ: HK1, HK2, Hè | FR-ROOM-02, 11, FR-BIL-05 |
| Q-02 | Hạn đóng tiền | 7 ngày sau ngày thông báo | FR-BIL-07 |
| Q-03 | Điện nước | Tính theo phòng; các thành viên tự chia; **trưởng phòng** thu và nộp cho BQL | FR-ROOM-09, 10, FR-BIL-04, BR-03, BR-11 |
| Q-04 | Tiền cọc | Nộp khi ký hợp đồng; K1 1.000.000 đ, N2 / N3 2.000.000 đ; bị trừ khi phòng hỏng hóc cần sửa chữa và được các thành viên bù lại; hoàn khi rời KTX | FR-ROOM-01, FR-CON-01, 09, FR-MNT-09, 10, FR-BIL-09, BR-10 |
| Q-05 | Hạn kích hoạt hợp đồng | 7 ngày | FR-CON-02 |
| Q-06 | Ngưỡng "Sắp hết hạn" | 15 ngày | FR-CON-03 |

### 9.2. Đã xác nhận (07/10/2026, đợt 2)

| # | Vấn đề | Kết luận | Mục bị ảnh hưởng |
|---|---|---|---|
| Q-07 | Diện ưu tiên và trọng số | Chính sách, năm nhất, ở xa; Staff xác nhận thủ công; trọng số cấu hình được | FR-REG-05, 06 |
| Q-08 | Kỹ thuật viên | Thuộc một đơn vị; Staff chỉ giao cho KTV cùng đơn vị | FR-MNT-03 |
| Q-09 | Quyền tổng hợp liên đơn vị | Admin cấp cho Staff cụ thể | FR-ACC-08 |
| Q-10 | Chuyển phòng liên đơn vị | Không hỗ trợ; trả phòng rồi đăng ký lại | FR-CON-07, BR-05 |
| Q-11 | Giới tính phòng | Mỗi phòng một giới tính | BR-01 |
| Q-12 | Quy mô | ~1.500 sinh viên, 100 người dùng đồng thời | NFR-PER-01, 02 |
| Q-13 | Phiên đăng nhập | Cookie phiên HttpOnly, hết hạn sau 30 phút không hoạt động | NFR-SEC-04 |
| Q-14 | Chu kỳ điện nước | Hằng tháng | FR-BIL-04 |
| Q-15 | Chia tiền trừ cọc | Chia đều cho sinh viên đang ở tại thời điểm báo hỏng. Cập nhật 10/10/2026: Staff được bỏ bớt người không liên quan (có lý do), chia đều cho người còn lại | FR-MNT-10 |
| Q-16 | Hỏng hóc nào bị trừ cọc | Chỉ hư hỏng do sử dụng; hao mòn tự nhiên không trừ cọc | FR-MNT-09, BR-10 |
| Q-17 | Hạn bù cọc | 7 ngày; quá hạn thì BQL nhắc nhở, không phạt | FR-BIL-13 |
| Q-18 | Vào ở hoặc rời giữa học kỳ | Đóng đủ phí cả học kỳ, không hoàn phần dư | FR-BIL-05 |
| Q-19 | Chuyển phòng khác giá giữa học kỳ | Không thu thêm / hoàn chênh lệch trong học kỳ hiện tại; học kỳ sau tính theo phòng mới | FR-BIL-05 |
| Q-20 | Học kỳ hè | Có thông báo đăng ký ở hè. Ở hè thì đóng phí như HK1, HK2. Không ở hè nhưng tiếp tục năm sau thì gửi đồ vào kho KTX, nhận lại đầu HK1 | FR-REG-13..15, FR-CON-11..13 |
| Q-21 | Hóa đơn điện nước khi một thành viên rời đi | Hóa đơn thuộc về phòng; không chặn trả phòng nhưng hiển thị cảnh báo | FR-CON-09 |

### 9.3. Hạn chế đã biết

- Phí phòng thu đủ học kỳ, không tính theo số ngày ở.
- Tiền điện nước không được chia cho từng sinh viên trong hệ thống.
- Trừ cọc chia đều, không xác định người làm hỏng.
- Không hỗ trợ chuyển phòng giữa hai đơn vị quản lý.
- Nhập chỉ số điện nước thủ công.

---

## 10. Phụ lục: sơ đồ trạng thái

### 10.1. Đơn đăng ký

| Từ | Sang | Ai | Điều kiện |
|---|---|---|---|
| DRAFT | SUBMITTED | Student | Đợt đang mở; chưa có đơn khác trong đợt; không có hợp đồng PENDING / ACTIVE |
| DRAFT, SUBMITTED | CANCELLED | Student | |
| APPROVED | CANCELLED | Student | Chỉ được phép hủy khi **chưa sinh hợp đồng** PENDING hoặc ACTIVE liên kết. Nếu đã có hợp đồng, sinh viên không được tự ý hủy đơn trực tiếp (trả 409 `APPLICATION_HAS_CONTRACT`, ẩn thao tác `cancel` trong `allowedActions`) mà phải thực hiện thủ tục hủy giữ chỗ / hợp đồng trước nhằm tránh hợp đồng mồ côi |
| SUBMITTED | APPROVED | Staff | Cùng đơn vị |
| SUBMITTED | REJECTED | Staff | Bắt buộc lý do |
| APPROVED | ASSIGNED | Staff + hệ thống | Phân giường thành công (UC-16) |
| ASSIGNED | APPROVED | Staff | Hủy giữ chỗ để phân lại, bắt buộc lý do |
| ASSIGNED | CANCELLED | Student / hệ thống | Sinh viên hủy giữ chỗ / quá hạn kích hoạt |

### 10.2. Hợp đồng

| Từ | Sang | Ai | Điều kiện |
|---|---|---|---|
| (tạo) | PENDING | Hệ thống | Khi phân giường |
| PENDING | ACTIVE | Staff | Đã ký, đã thu cọc |
| PENDING | CANCELLED | Hệ thống / Staff / Student | Quá hạn kích hoạt, hủy giữ chỗ |
| ACTIVE | EXPIRED | Staff | Trả phòng vào hoặc sau ngày kết thúc |
| ACTIVE | TERMINATED | Staff | Trả phòng trước ngày kết thúc |

Nhãn suy ra trên ACTIVE: "Sắp hết hạn", "Quá hạn trả phòng", "Tạm rời hè".

### 10.3. Phân phòng

| Từ | Sang | Khi |
|---|---|---|
| (tạo) | HOLD, mở | Phân giường |
| HOLD | STAY (cùng dòng) | Kích hoạt hợp đồng |
| HOLD | đóng | Hủy giữ chỗ, quá hạn kích hoạt |
| STAY | đóng | Chuyển phòng (mở dòng mới), trả phòng, tạm rời hè |
| (tạo) | STAY, mở | Phân lại giường đầu HK1 cho sinh viên tạm rời hè |

### 10.4. Yêu cầu sửa chữa

| Từ | Sang | Ai |
|---|---|---|
| OPEN | ASSIGNED | Staff |
| OPEN | REJECTED | Staff (lý do) |
| OPEN | CANCELLED | Người yêu cầu |
| ASSIGNED | IN_PROGRESS | KTV được giao |
| IN_PROGRESS | RESOLVED | KTV được giao |
| RESOLVED | CLOSED | Người yêu cầu / Staff thay / hệ thống sau 3 ngày |
| RESOLVED | IN_PROGRESS | Người yêu cầu (lý do) |

### 10.5. Hóa đơn (phí phòng và điện nước)

| Từ | Sang | Ai |
|---|---|---|
| (tạo) | UNPAID | Staff phát hành |
| UNPAID | PAID | Staff xác nhận |

### 10.6. Sổ cọc

| Giao dịch | Dấu | Phát sinh khi | Ai |
|---|---|---|---|
| Thu cọc | + | Kích hoạt hợp đồng | Staff |
| Trừ cọc | − | Ticket của phòng CLOSED có chi phí | Hệ thống |
| Bù cọc | + | Sinh viên nộp bù | Staff xác nhận |
| Hoàn cọc | − | Trả phòng, bằng số dư cọc | Staff |

Nhãn suy ra trên UNPAID: "Quá hạn" khi qua hạn đóng.


### Ghi chú chuyển xác thực ngày 14/10/2026

Keycloak là tích hợp xác thực được người dùng yêu cầu thêm, là ngoại lệ cho ràng buộc ban đầu không tích hợp bên ngoài. Các quy định session cookie/BCrypt thuộc lịch sử v0.7; mã ứng dụng của luồng cũ đã được bỏ. Mật khẩu/tài khoản không đồng bộ qua Git; nhóm phải dùng cùng Keycloak/realm. Mật khẩu cũ không chuyển; ID hồ sơ và dữ liệu nghiệp vụ được giữ nguyên. Keycloak và PostgreSQL DMS không có transaction phân tán: thao tác bù có thể cần đối soát nếu mất kết nối, chưa có outbox bền vững.


Ghi chú dọn cấu trúc 14/10/2026: chỉ giữ Keycloak; dùng lại AuthController, SecurityConfig, BootstrapAdmin và AuthService/Impl. Không tạo lớp legacy hay Service chuyển đổi riêng. Migration đã áp dụng và các kiểm thử JWT/CRUD cần thiết được giữ. Hướng dẫn chạy gom vào README gốc.

## Phân quyền Role → thao tác (cập nhật 14/10/2026)

Theo quyết định mới của người phụ trách, ADMIN có mọi quyền nghiệp vụ trên toàn hệ thống. ADMIN không cần gắn management_unit_id; bỏ giới hạn đơn vị/chủ sở hữu/người được giao khi xác định quyền, nhưng vẫn phải qua trạng thái, ràng buộc dữ liệu, đối tượng sinh viên thật và lịch sử/audit. Không đổi hoặc giả vai trò/người thao tác để vượt phạm vi.

Khung tham khảo RolePrivilege của HR-V5: `ScopeGuard` kiểm tra quyền hiện hành qua `RolePermissionService`; ma trận trong code chỉ xác định quyền mặc định và giới hạn nghiệp vụ của từng role, mỗi mã là `<RESOURCE>_<READ|CREATE|UPDATE|DELETE>`. READ dùng cho tìm kiếm và xem chi tiết; khóa/mở khóa/reset/gán phạm vi tài khoản có mã riêng. `/api/auth/me` trả thêm `permissions: string[]`; React dùng danh sách này cho menu, route và từng nút. API khai quyền bằng @RequirePrivilege giống V5; Spring Security Method Security kiểm tra trước thân Controller. Service Impl tiếp tục kiểm quyền nội bộ, phạm vi và trạng thái. Không chỉ dựa vào việc ẩn nút.

- ADMIN: mọi quyền quản trị và nghiệp vụ ở mọi đơn vị; tài khoản và người thao tác trong audit vẫn là ADMIN thực.
- STAFF: nghiệp vụ trong đơn vị được giao; crossUnitAccess chỉ mở rộng đọc. Đơn/yêu cầu do sinh viên tạo không mặc nhiên cho Staff tạo hoặc hủy thay sinh viên; quyền sửa/xét duyệt phụ thuộc endpoint và trạng thái.
- TECHNICIAN: đọc/cập nhật tiến độ ticket được giao, không CRUD dữ liệu phòng/hợp đồng/hóa đơn.
- STUDENT: dữ liệu cá nhân và phòng đang ở; đơn/yêu cầu của mình, không sửa role hoặc ID người sở hữu.

Đổi role của tài khoản Staff/KTV/Admin qua PATCH `/api/admin/users/{id}` hỗ trợ trường `role`, đồng bộ Keycloak và thu hồi token cũ. Staff/KTV phải có đơn vị; chuyển sang ADMIN xóa đơn vị và cờ tổng hợp; không chuyển hồ sơ STUDENT thành tài khoản cán bộ, không tự đổi role tài khoản đang sử dụng. ADMIN tạo đơn đăng ký hộ sinh viên bằng `studentId` rõ ràng; STUDENT không được gửi ID của người khác.

Cập nhật 15/10/2026: ADMIN quản lý ma trận động tại `/admin/role-permissions`, lưu quyền của STAFF/STUDENT/TECHNICIAN trong `role_permission_matrix` qua Flyway V5. ADMIN giữ mọi quyền đã đăng ký và không có bản ghi để thu hồi quyền ADMIN. Mã quyền vẫn cố định trong Constants; quyền đang được cấp đọc từ CSDL. Sổ cọc/audit vẫn giữ chỉ đọc, hồ sơ có lịch sử và trạng thái vẫn chịu ràng buộc xóa/hủy như trước.

### Khai quyền API bằng @RequirePrivilege — 14/10/2026

Mọi hàm REST mapped trong ứng dụng khai `@RequirePrivilege(Permission.ROOM_CREATE)` hoặc một danh sách như `@RequirePrivilege({Permission.ROOM_TYPE_READ, Permission.ROOM_CREATE, Permission.ROOM_UPDATE})`. Nhiều mã dùng OR: có ít nhất một quyền mới được chạy API. Danh sách OR phù hợp API tra cứu cho form; thao tác ghi phải khai riêng CREATE/UPDATE/DELETE, không ghép quyền thêm/sửa để mở cả hai.

`common/security/RequirePrivilege.java` chỉ khai annotation; `ScopeGuard.hasDeclaredPrivilege` đọc khai báo trên hàm thật, dùng khai báo lớp nếu hàm chưa khai, và kiểm role với dữ liệu tài khoản hiện tại. `SecurityConfig` bật Spring Security Method Security và đăng ký AuthorizationManagerBeforeMethodInterceptor: thiếu annotation, mảng rỗng hoặc không có quyền thì trả 403 trước thân Controller. Chưa đăng nhập/token không hợp lệ vẫn bị filter chain trả 401. Annotation ở hàm thay thế annotation lớp; Admin không vượt qua mã quyền chưa đăng ký.

Service Impl vẫn giữ kiểm tra quyền cho lời gọi nội bộ, phạm vi đơn vị/chủ sở hữu, trạng thái và ràng buộc dữ liệu; việc thêm annotation không biến dữ liệu đang dùng thành dữ liệu được phép xóa. ADMIN tiếp tục có mọi quyền nghiệp vụ đã đăng ký trên toàn hệ thống. Frontend và `/api/auth/me.permissions` giữ nguyên mã quyền hiện có.

Ở bước bổ sung annotation chỉ thêm một file source là `RequirePrivilege.java`; không thêm migration, Aspect/Service mới hoặc dependency V5. Bộ test kiểm OR, chặn trước thân Controller, lớp/hàm, thiếu/rỗng/sai annotation và quét mọi API để không bỏ sót quyền. Backend clean verify đạt **53/53 test**.

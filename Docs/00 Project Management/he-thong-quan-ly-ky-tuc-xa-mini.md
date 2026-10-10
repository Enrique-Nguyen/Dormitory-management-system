# Đề xuất đề tài: Hệ thống quản lý ký túc xá sinh viên (DMS)

> **Phân tích, thiết kế và xây dựng hệ thống web quản lý ký túc xá sinh viên**
> *Design and Development of a Web-based Dormitory Management System for University Students*

| | |
|---|---|
| Học phần | Chuyên đề CNTT |
| Giảng viên | Hoàng Quốc Dũng |
| Lớp | 65CNTT |
| Nhóm | 3 |
| Thành viên | Nguyễn Phú Nguyên, Nguyễn Văn Séc, Nguyễn Văn Quyền, Nguyễn Tiến Lộc, Lê Đức Nguyên |
| Thời gian thực hiện | 12 tuần |

Tài liệu này là **bản đề xuất** phân tích, thiết kế và xây dựng MVP, chưa phải báo cáo một hệ thống đã triển khai. Nội dung thống nhất với bài thuyết trình `DMS-thuyet-trinh.html` và đặc tả yêu cầu `SRS-DMS.md` (bản 0.3); khi có chi tiết khác nhau, SRS là căn cứ.

---

## 1. Bối cảnh và vấn đề

Ký túc xá Trường Đại học Thủy Lợi hiện do **hai đơn vị** vận hành:

- **Tòa K1**: một đơn vị quản lý.
- **Cụm tòa N2 + N3**: một đơn vị quản lý khác.

Theo quan sát của nhóm, cả hai đơn vị đều quản lý bằng sổ sách giấy tờ, file Excel riêng lẻ và nhóm chat. Hệ quả:

| Vấn đề | Hậu quả |
|---|---|
| Dữ liệu rời rạc giữa hai đơn vị | Khó tổng hợp số phòng trống, số sinh viên đang ở |
| Báo hỏng thiết bị qua tin nhắn | Dễ bỏ sót, không biết ai đang xử lý |
| Theo dõi điện nước, chi phí thủ công | Chậm tính phí, khó biết khoản nào chưa thu |
| Hợp đồng lưu trú phân tán | Dễ quên gia hạn, không có lịch sử ở phòng nào |

**Nhu cầu:** một nguồn dữ liệu chung, trong đó mỗi đơn vị chỉ thao tác trên dữ liệu thuộc phạm vi của mình.

> Hiện trạng trên là quan sát của nhóm. Nhóm sẽ khảo sát Ban quản lý (BQL) ở tuần 1–2 về quy trình xét duyệt, hợp đồng, đơn giá và quyền tổng hợp liên tòa trước khi chốt SRS.

---

## 2. Giải pháp: một cổng quản lý vòng đời lưu trú

DMS là hệ thống web tập trung, nơi sinh viên, BQL và kỹ thuật viên tương tác trên cùng một nền tảng. Hệ thống quản lý toàn bộ vòng đời lưu trú của sinh viên:

```text
Đăng ký → Xét duyệt → Phân phòng → Ký hợp đồng → Lưu trú / Tính phí
        → Báo hỏng → Chuyển phòng / Gia hạn → Kết thúc lưu trú
```

| Bước | Mô tả |
|---|---|
| 1. Đăng ký | Sinh viên nộp nguyện vọng |
| 2. Xét duyệt | BQL kiểm tra điều kiện |
| 3. Phân phòng | Giữ chỗ hợp lệ trong phòng còn chỗ |
| 4. Ký hợp đồng | Kích hoạt lưu trú |
| 5. Lưu trú / Tính phí | Phí phòng theo học kỳ, điện nước theo phòng, tiền cọc |
| 6. Báo hỏng | Xử lý có người phụ trách và nghiệm thu |
| 7. Chuyển phòng / Gia hạn | Lưu lịch sử thay đổi |
| 8. Kết thúc | Đối soát, trả chỗ |

Bước 5 và 6 có thể lặp lại nhiều lần trong giai đoạn lưu trú.

Giá trị của đề tài nằm ở **các quy trình có trạng thái, quy tắc nghiệp vụ nhất quán và lịch sử lưu trú đầy đủ**, chứ không chỉ ở các chức năng CRUD.

---

## 3. Người dùng và phân quyền

### 3.1. Bốn vai trò

| Vai trò | Mô tả |
|---|---|
| **Student** (Sinh viên) | Đăng ký, lưu trú, xem hợp đồng, hóa đơn và tiền cọc, báo hỏng, yêu cầu chuyển phòng hoặc gia hạn, đăng ký ở hè. Mỗi phòng có một sinh viên được Staff chỉ định làm **trưởng phòng** (thu và nộp tiền điện nước của phòng) |
| **Staff** (Ban quản lý) | Thực hiện phần lớn nghiệp vụ quản lý trong phạm vi **đơn vị quản lý** được phân công (K1, hoặc N2 + N3) |
| **Technician** (Kỹ thuật viên) | Xử lý các yêu cầu sửa chữa được giao |
| **Admin** (Quản trị viên) | Quản lý tài khoản, vai trò, phạm vi quyền và danh mục hệ thống |

### 3.2. Ma trận quyền

Hệ thống dùng RBAC kết hợp **phạm vi dữ liệu** (đơn vị quản lý, phòng, người sở hữu), theo nguyên tắc quyền tối thiểu. Đơn vị quản lý (`ManagementUnit`) là cấp phạm vi của Staff: mỗi tòa thuộc một đơn vị, hiện có hai đơn vị là K1 và N2 + N3.

| Nghiệp vụ | Student | Staff | Technician | Admin |
|---|---|---|---|---|
| Đơn / Hợp đồng | Của mình | Đơn vị phụ trách | — | — |
| Phòng / Hóa đơn | Xem liên quan | Đơn vị phụ trách | — | — |
| Yêu cầu sửa chữa | Tạo, nghiệm thu | Tiếp nhận, giao việc | Việc được giao | — |
| Thông báo / Báo cáo | Xem liên quan | Phạm vi được cấp | Xem liên quan | Tổng hợp* |
| Tài khoản / Quyền | Hồ sơ cá nhân | Hồ sơ cá nhân | Hồ sơ cá nhân | Quản trị |

\* Quyền tổng hợp liên tòa được cấp riêng theo phân công.

**Nguyên tắc:**

- API kiểm tra quyền trên **từng đối tượng** theo bộ ba *vai trò + phạm vi + trạng thái*, không chỉ ẩn nút ở giao diện.
- Staff phụ trách K1 không đọc hoặc sửa dữ liệu N2 + N3 nếu không được phân quyền.
- **Đơn đăng ký** thuộc phạm vi đơn vị theo trường *đơn vị nguyện vọng* do sinh viên chọn khi nộp đơn, vì lúc này sinh viên chưa có phòng.
- **Sinh viên** thuộc phạm vi một đơn vị khi có đơn đăng ký hoặc phân phòng hiệu lực tại đơn vị đó; Staff chỉ xem và thao tác hồ sơ sinh viên trong phạm vi này.
- Technician chỉ cập nhật yêu cầu được giao cho mình.
- Admin quản lý kỹ thuật hệ thống, không mặc nhiên được duyệt hợp đồng hay sửa hóa đơn. Người kiêm nhiệm sẽ được cấp thêm vai trò Staff với phạm vi đơn vị cụ thể.
- Thông báo toàn KTX và báo cáo liên tòa cần quyền riêng.
- Vai trò là tập cố định (Student, Staff, Technician, Admin) kèm phạm vi đơn vị quản lý; MVP **không** xây bảng `Permission` chi tiết.

### 3.3. Tài khoản và mật khẩu

- **Tạo tài khoản sinh viên:** Admin **import danh sách từ file Excel** (lúc import sinh viên chưa thuộc đơn vị nào nên việc này không giao cho Staff). Sinh viên đăng nhập bằng mã sinh viên và mật khẩu mặc định, bắt buộc đổi mật khẩu ở lần đăng nhập đầu tiên.
- **Quên mật khẩu:** Admin, hoặc Staff của đơn vị mà sinh viên đang thuộc phạm vi, đặt lại mật khẩu (không dùng email, vì email chỉ ở mức Could).
- Mọi thao tác quản trị tài khoản đều được ghi vào `AuditLog`.

---

## 4. Kiến trúc và công nghệ

### 4.1. Kiến trúc web ba lớp

```text
+---------------------------+
|  Lớp giao diện: React     |  Biểu mẫu, trạng thái, cổng theo vai trò
+-------------+-------------+
              | HTTPS + JSON (REST API)
              v
+---------------------------+
|  Lớp ứng dụng: Spring Boot|  Xác thực, phân quyền,
|  Controller → Service     |  quy tắc nghiệp vụ, transaction
|            → Repository   |
+-------------+-------------+
              | ORM / SQL
              v
+---------------------------+
|  Lớp dữ liệu: PostgreSQL  |  Ràng buộc, khóa ngoại,
|                           |  lịch sử và nhật ký
+---------------------------+
```

- Backend triển khai dạng **modular monolith**: một ứng dụng, chia module theo nghiệp vụ, phù hợp thời lượng một học kỳ.
- **Controller** tiếp nhận request. **Service** kiểm tra quyền, trạng thái rồi thực hiện nghiệp vụ. **Repository** truy cập PostgreSQL. Logic nghiệp vụ tập trung ở Service.
- Validation phía client chỉ để hỗ trợ người dùng; server là nơi kiểm tra bắt buộc.
- Mật khẩu được băm; thông tin bí mật cấu hình ngoài mã nguồn.

### 4.2. Lý do chọn công nghệ

| Thành phần | Công nghệ | Lý do |
|---|---|---|
| Frontend | React | Phổ biến, phù hợp giao diện nhiều biểu mẫu và theo vai trò |
| Backend | Spring Boot | Hỗ trợ tốt transaction, bảo mật (Spring Security) và phân lớp |
| Database | PostgreSQL | Ràng buộc mạnh, hỗ trợ khóa bản ghi cho nghiệp vụ phân phòng |

---

## 5. Bảy phân hệ của MVP

| # | Phân hệ | Nội dung chính |
|---|---|---|
| 1 | **Tài khoản & RBAC** | Đăng nhập, đăng xuất, đổi mật khẩu, hồ sơ cá nhân, phân quyền theo vai trò và phạm vi, kiểm soát API |
| 2 | **Phòng, Giường & Tòa nhà** | Tòa, tầng, loại phòng, **giường**, giá; trạng thái vận hành của phòng (Hoạt động / Bảo trì / Ngừng sử dụng). Số chỗ trống và "Đầy" được tính ra từ trạng thái giường |
| 3 | **Đăng ký & Phân phòng** | Đợt đăng ký, nộp đơn, xếp hạng ưu tiên, duyệt đơn, chọn giường, giữ chỗ |
| 4 | **Hợp đồng** | Lập và kích hoạt hợp đồng, chuyển phòng, gia hạn, trả phòng, lưu lịch sử thay đổi |
| 5 | **Yêu cầu sửa chữa** | Báo hỏng (phòng và khu vực chung), giao việc, xử lý, nghiệm thu |
| 6 | **Điện nước, Hóa đơn & Tiền cọc** | Phí phòng theo học kỳ cho từng sinh viên; nhập chỉ số và hóa đơn điện nước theo phòng (trưởng phòng nộp); sổ tiền cọc; xác nhận thanh toán thủ công |
| 7 | **Thông báo & Dashboard** | Thông báo toàn KTX / theo tòa / theo phòng; thống kê cơ bản |

Mỗi phân hệ có dữ liệu, quyền và tiêu chí nghiệm thu riêng.

**Dashboard cơ bản** gồm:

- Tỷ lệ chỗ đang sử dụng trên **sức chứa khai thác**. Phòng bảo trì được tách khỏi mẫu số và hiển thị riêng.
- Tổng số tiền chưa thanh toán và tổng bù cọc chưa nộp.
- Số ticket sửa chữa theo trạng thái.

**Thanh toán:** BQL xác nhận thanh toán thủ công, hệ thống lưu người xác nhận và thời gian. MVP không tích hợp cổng thanh toán.

---

## 6. Workflow trọng tâm

### 6.1. Đăng ký và phân phòng

**Đợt đăng ký (`RegistrationPeriod`):** mỗi đợt có học kỳ, ngày mở, ngày đóng và chỉ tiêu theo tòa. Sinh viên chỉ nộp đơn trong thời gian đợt đang mở, **mỗi đợt một đơn**, trong đó chọn **một đơn vị nguyện vọng** (K1 hoặc N2 + N3). Đơn được chuyển đến Staff của đơn vị đó.

**Điểm ưu tiên:** khi nộp đơn, hệ thống tính điểm ưu tiên theo quy tắc đơn giản, cấu hình được (ví dụ diện chính sách, sinh viên năm nhất, ở xa). Hệ thống chỉ **sắp xếp danh sách đơn** theo điểm; quyết định duyệt cuối cùng vẫn thuộc BQL và có ghi lý do. Trọng số cụ thể sẽ được chốt khi khảo sát BQL.

```text
DRAFT ──> SUBMITTED ──> APPROVED ──> ASSIGNED
 (SV soạn)  (SV nộp)  │  (BQL duyệt) ^  (BQL + hệ thống phân phòng)
                      │              │       │
                      │              └───────┤ BQL hủy giữ chỗ để phân lại (kèm lý do)
                      │                      │
                      └──> REJECTED          └──> CANCELLED (SV hủy giữ chỗ / quá hạn kích hoạt)
                          (từ chối, kèm lý do)

Từ DRAFT / SUBMITTED / APPROVED có thể chuyển sang CANCELLED.
```

- **Rời khỏi ASSIGNED:** khi BQL hủy giữ chỗ để phân lại (ASSIGNED → APPROVED) hoặc khi sinh viên hủy / hợp đồng PENDING quá hạn kích hoạt (ASSIGNED → CANCELLED), hệ thống đóng dòng phân phòng, chuyển hợp đồng sang CANCELLED và đổi trạng thái đơn **trong cùng một transaction**. Sinh viên bị hủy có thể nộp đơn mới nếu đợt còn mở.
- Đơn ở ASSIGNED chuyển thành hồ sơ lưu trú khi hợp đồng ACTIVE; đơn không còn thay đổi trạng thái sau đó.

- Chỉ đơn ở trạng thái **APPROVED** mới được phân phòng.
- Nếu phòng đã chọn hết chỗ: đơn giữ nguyên APPROVED, BQL chọn phòng khác.
- Phân phòng thực hiện trong **một transaction**:

```text
Khóa bản ghi sinh viên và giường được chọn
  → Đọc lại trạng thái đơn, trạng thái giường (trống / đang giữ / đang ở), tình trạng phòng, hợp đồng hiện hành
  → Tạo bản ghi phân phòng (giữ chỗ) gắn với giường, chụp đơn giá từ loại phòng
  → Tạo hợp đồng PENDING
  → Chuyển đơn sang ASSIGNED
  → Commit (lỗi ở bất kỳ bước nào thì rollback toàn bộ)
```

- Sức chứa được mô hình hóa bằng **giường**: một giường chỉ có tối đa một phân phòng hiệu lực (đang giữ hoặc đang ở) tại một thời điểm. Ràng buộc này được **CSDL bảo vệ** bằng partial unique index (xem BR-01), nên dù có lỗi ở tầng ứng dụng thì dữ liệu cũng không thể vượt chỗ.
- Mọi luồng chiếm giường (phân phòng, chuyển phòng) đều dùng chung cơ chế khóa và chung ràng buộc.
- Gửi lại cùng một request không được tạo phân phòng thứ hai: index `UNIQUE(student_id) WHERE ended_at IS NULL` khiến lần gửi thứ hai bị CSDL từ chối, API trả lỗi 409.
- Hủy hoặc hết hạn giữ chỗ phải trả lại giường.

### 6.2. Hợp đồng lưu trú

```text
PENDING ──> ACTIVE ──> EXPIRED
   │           │
   │           └──> TERMINATED (chấm dứt trước hạn)
   └──> CANCELLED (không ký / hủy giữ chỗ)
```

- **Kích hoạt hợp đồng:** sinh viên đến ký bản giấy, sau đó Staff bấm "Kích hoạt" để chuyển PENDING → ACTIVE. Điều kiện: sinh viên đã nộp đủ **tiền cọc theo mức của tòa** (K1: 1.000.000 đ; N2, N3: 2.000.000 đ); hệ thống ghi giao dịch thu cọc vào sổ cọc. Cùng transaction đó, dòng phân phòng **đổi loại từ giữ chỗ sang đang ở trên chính dòng đó** (không đóng/mở dòng mới), nên giường được chiếm liên tục và ràng buộc không bị ảnh hưởng. Nếu quá **N ngày** (cấu hình được, mặc định 7) kể từ lúc phân phòng mà chưa kích hoạt, hệ thống tự chuyển hợp đồng sang CANCELLED, đóng dòng phân phòng (trả giường) và chuyển đơn sang CANCELLED.
- **Expiring** ("sắp hết hạn") **không phải trạng thái riêng**. Đó là nhãn cảnh báo khi còn ≤ **15 ngày** đến ngày kết thúc; hợp đồng vẫn ở trạng thái ACTIVE.
- Hợp đồng chỉ chuyển EXPIRED / TERMINATED qua thủ tục trả phòng; quá ngày kết thúc mà chưa trả phòng thì hiện nhãn "Quá hạn trả phòng".
- **Gia hạn:** sinh viên gửi yêu cầu → BQL kiểm tra điều kiện → phê duyệt hoặc từ chối. Gia hạn chỉ có hiệu lực sau khi được phê duyệt. Hệ thống lưu mốc thời gian cũ, mốc mới và người xử lý.
- **Kết thúc lưu trú:** xem mục 6.2.1.

#### 6.2.1. Trả phòng

Do Staff thực hiện khi hợp đồng hết hạn, bị chấm dứt hoặc sinh viên chuyển đi:

1. Ghi nhận tình trạng tài sản trong phòng / giường (đạt, hư hỏng kèm ghi chú).
2. Kiểm tra công nợ: nếu còn phí phòng chưa đóng (hoặc hóa đơn điện nước của phòng còn nợ) thì hiển thị cảnh báo và ghi nhận. Khoản bù cọc chưa nộp không thành công nợ mà được tất toán bằng số dư cọc ở bước 4.
3. Đóng phân phòng, trả giường trong một transaction; hợp đồng chuyển sang EXPIRED hoặc TERMINATED.
4. Hoàn cọc bằng **số dư cọc** hiện tại (mức cọc trừ các khoản đã trừ chưa bù); nếu số dư âm thì phần âm ghi thành công nợ. Ghi số tiền, người xác nhận, thời gian. Hệ thống không thực hiện chuyển tiền. Nếu sinh viên là trưởng phòng, chỉ định tự gỡ.

Công nợ còn lại vẫn tiếp tục được theo dõi sau khi trả phòng.

### 6.3. Chuyển phòng

1. Sinh viên gửi yêu cầu chuyển phòng, kèm lý do.
2. BQL xem xét và chọn phòng mới trong **cùng đơn vị quản lý** (MVP chưa hỗ trợ chuyển sang đơn vị khác).
3. Hệ thống thực hiện trong một transaction:
   - Khóa giường nguồn và giường đích theo **thứ tự cố định** để tránh deadlock.
   - Kiểm tra giường đích còn trống.
   - Đóng khoảng ở cũ, mở khoảng ở mới.
   - Dòng phân phòng mới chụp đơn giá của phòng mới; hợp đồng giữ nguyên (hợp đồng không lưu phòng).
   - Ghi lịch sử: lý do, người duyệt, thời gian.
4. Lỗi ở bất kỳ bước nào thì rollback toàn bộ.

Hệ thống **không** ghi đè trực tiếp kiểu `student.room_id = newRoom`. Phòng cũ và phòng mới được suy ra từ hai khoảng phân phòng liên tiếp trong `RoomAssignmentHistory`. Hợp đồng cũng không lưu `room_id`, nên chuyển phòng không phải ghi đè dữ liệu nào.

### 6.4. Báo hỏng và nghiệm thu

```text
OPEN ──> ASSIGNED ──> IN_PROGRESS ──> RESOLVED ──> CLOSED
(SV báo)  (BQL giao)   (KTV xử lý)    (KTV xong)   (SV nghiệm thu)
                            ^              │
                            └──────────────┘
                     SV không chấp nhận: quay lại IN_PROGRESS, kèm lý do

Từ OPEN có thể chuyển sang REJECTED (BQL từ chối) hoặc CANCELLED (SV hủy).
```

- **Đúng phòng:** sinh viên đang lưu trú chỉ được tạo ticket cho phòng hiện tại của mình. Sự cố ở **khu vực chung** (hành lang, nhà vệ sinh chung…) do Staff tạo ticket gắn với tòa / tầng.
- **Tự đóng:** nếu ticket ở RESOLVED quá **3 ngày** mà người yêu cầu không nghiệm thu hoặc phản hồi, hệ thống tự chuyển sang CLOSED và ghi rõ lý do "tự động đóng".
- **Đúng người:** chỉ Technician được giao mới được xử lý kỹ thuật.
- **Đủ lịch sử:** mỗi lần chuyển trạng thái đều lưu người thao tác, thời gian và lý do.
- RESOLVED nghĩa là đã sửa xong; CLOSED nghĩa là người yêu cầu đã nghiệm thu.
- API chặn các chuyển trạng thái không hợp lệ: Technician không được tự đóng ticket, không được nhảy thẳng từ OPEN sang CLOSED.
- Trường hợp người yêu cầu đã rời KTX: Staff nghiệm thu thay, ghi rõ lý do.
- **Chi phí và tiền cọc:** khi ticket của phòng đóng, Staff xác định nguyên nhân. *Hao mòn tự nhiên* không trừ cọc. *Hư hỏng do sử dụng* thì Staff nhập chi phí; hệ thống trừ đều vào cọc của các sinh viên đang ở phòng tại thời điểm báo hỏng (làm tròn ở người cuối để tổng khớp) và tạo khoản **bù cọc** cho từng người, hạn 7 ngày. Quá hạn thì hệ thống gửi nhắc nhở và BQL nhắc trực tiếp.

### 6.5. Phí phòng, điện nước và tiền cọc

Hệ thống có hai loại hóa đơn và một sổ cọc:

| | Phí phòng | Điện nước | Tiền cọc |
|---|---|---|---|
| Thuộc về | Từng sinh viên | Từng phòng | Từng hợp đồng |
| Chu kỳ | Học kỳ (HK1, HK2, Hè) | Tháng | Thu khi ký; trừ khi hư hỏng do sử dụng; bù; hoàn khi rời KTX |
| Người nộp | Sinh viên | **Trưởng phòng** (các thành viên tự thống nhất chia) | Sinh viên |
| Hạn đóng | 7 ngày sau ngày thông báo | 7 ngày sau ngày thông báo | Bù cọc: 7 ngày, quá hạn thì nhắc nhở |

```text
Phí phòng:  Chọn học kỳ → Lập hóa đơn từng sinh viên (đơn giá trên phân phòng) → Thông báo → BQL xác nhận thanh toán
Điện nước:  Nhập chỉ số theo phòng → Tính tiêu thụ × đơn giá → Hóa đơn phòng → Trưởng phòng nộp → BQL xác nhận
```

- Chỉ số điện, nước được ghi **theo phòng**. Tiêu thụ = chỉ số cuối − chỉ số đầu. Điện tính theo kWh, nước theo m³. Ví dụ minh họa: điện từ 1.250 đến 1.400 kWh, đơn giá giả định 3.000 đ/kWh → 450.000 đ (không phải biểu giá của nhà trường).
- **Hóa đơn điện nước thuộc về phòng**, gửi đến trưởng phòng, mọi thành viên xem được. Hệ thống không chia tiền cho từng sinh viên.
- **Phí phòng thu đủ cả học kỳ** theo đơn giá trên phân phòng hiệu lực lúc phát hành, kể cả khi vào ở hoặc rời đi giữa kỳ; chuyển phòng giữa kỳ không thu thêm hay hoàn chênh lệch.
- Mọi hóa đơn gồm các dòng phí (`InvoiceItem`) với đơn giá được chụp lúc phát hành. Trạng thái: **Chưa thanh toán → Đã thanh toán**; nhãn **Quá hạn** tính từ hạn đóng.
- **Sổ cọc** (`DepositTransaction`) ghi bốn loại giao dịch: thu cọc, trừ cọc, bù cọc, hoàn cọc. Số dư cọc là tổng các giao dịch, không lưu riêng.

### 6.6. Học kỳ hè

Khi BQL mở đợt đăng ký hè, mọi sinh viên đang ở nhận thông báo và chọn một trong ba:

| Lựa chọn | Xử lý |
|---|---|
| **Ở hè** | Hợp đồng được gia hạn qua hè; sinh viên đóng phí phòng học kỳ hè như HK1, HK2. BQL có thể chuyển phòng để gom phòng ở hè |
| **Tạm rời hè** (tiếp tục năm học sau) | Điều kiện: hợp đồng đã gia hạn sang năm học kế tiếp. Sinh viên **gửi đồ cá nhân vào kho KTX** (phiếu gửi đồ), trả giường trong hè; hợp đồng vẫn ACTIVE với nhãn "Tạm rời hè", không thu phí hè, giữ nguyên cọc. Đầu HK1, BQL phân lại giường (ưu tiên giường cũ) không cần đơn mới và trả đồ |
| **Rời KTX** | Làm thủ tục trả phòng (mục 6.2.1) |

---

## 7. Quy tắc nghiệp vụ

Các quy tắc được kiểm tra ở **Service** và được bảo vệ thêm bằng **ràng buộc trong Database**.

### BR-01: Sức chứa (theo giường)

- Mỗi giường có tối đa **một phân phòng hiệu lực** (đang giữ hoặc đang ở) tại một thời điểm. Sức chứa phòng = số giường đang khai thác; số chỗ trống được tính ra, không lưu trùng.
- Ràng buộc trên được đặt ở **CSDL** bằng hai partial unique index, Service chỉ kiểm tra sớm để báo lỗi rõ ràng:

  ```sql
  CREATE UNIQUE INDEX ux_assignment_bed_active     ON room_assignment(bed_id)     WHERE ended_at IS NULL;
  CREATE UNIQUE INDEX ux_assignment_student_active ON room_assignment(student_id) WHERE ended_at IS NULL;
  ```

  Phân phòng hiệu lực là dòng có `ended_at` để trống, gồm cả giữ chỗ và đang ở. MVP không cho đặt giường trước cho tương lai nên không cần exclusion constraint theo khoảng thời gian.
- Khóa giường trước khi phân, rồi kiểm tra lại sau khi khóa để tránh hai yêu cầu cùng thấy một giường trống.
- Không phân sinh viên vào phòng đang bảo trì hoặc ngừng sử dụng.
- Phòng phải phù hợp loại phòng đăng ký và giới tính (nếu áp dụng).

### BR-02: Hợp đồng

- Mỗi sinh viên có tối đa **một hợp đồng hiệu lực** tại một thời điểm. Hợp đồng "Expiring" vẫn tính là đang hiệu lực.
- Khoảng lưu trú của cùng một sinh viên không được giao nhau. Khóa bản ghi sinh viên khi thay đổi.

### BR-03: Điện nước

- Chỉ số cuối ≥ chỉ số đầu.
- Chỉ số đầu kỳ nối tiếp chỉ số cuối kỳ trước, trừ trường hợp thay hoặc reset đồng hồ được ghi nhận riêng.
- Duy nhất một bản ghi chỉ số cho mỗi (phòng, loại đồng hồ, kỳ); duy nhất một hóa đơn phí phòng cho mỗi (sinh viên, học kỳ) và một hóa đơn điện nước cho mỗi (phòng, tháng).
- Hóa đơn điện nước thuộc về phòng, không chia cho sinh viên.
- Hóa đơn đã xác nhận thanh toán bị khóa chỉnh sửa; muốn sửa phải làm nghiệp vụ điều chỉnh.

### BR-04: Chuyển phòng

- Không duyệt chuyển phòng nếu phòng đích đã đầy.
- Phòng đích phải cùng đơn vị quản lý với phòng hiện tại.
- Đóng phân phòng cũ và mở phân phòng mới đồng bộ trong một transaction, ghi đủ lịch sử (xem mục 6.3).
- Không ghi phòng hay giá vào hợp đồng: phòng và đơn giá áp dụng chỉ nằm trên dòng phân phòng.

### BR-05: Yêu cầu sửa chữa

- Sinh viên đang lưu trú chỉ được tạo yêu cầu cho phòng hiện tại; sự cố khu vực chung do Staff tạo.
- Chỉ Technician được giao mới được cập nhật trạng thái xử lý.
- Chỉ được đóng yêu cầu sau khi đã RESOLVED.

### BR-06: Tiền cọc và trưởng phòng

- Hợp đồng chỉ ACTIVE khi đã thu đủ mức cọc của tòa. Trừ cọc chỉ phát sinh từ hư hỏng do sử dụng; tổng trừ của các sinh viên bằng chi phí sửa chữa. Hoàn cọc không vượt quá số dư cọc.
- Trưởng phòng phải là sinh viên đang ở trong chính phòng đó; mỗi phòng tối đa một trưởng phòng.

---

## 8. Mô hình dữ liệu sơ bộ

### 8.1. Các thực thể chính

| Thực thể | Thuộc tính tiêu biểu |
|---|---|
| `User` | id, role, management_unit_id (phạm vi của Staff), thông tin hồ sơ, cờ bắt buộc đổi mật khẩu |
| `ManagementUnit` | id, tên (K1; N2 + N3) |
| `Building` | id, tên, management_unit_id, **mức cọc** (K1: 1.000.000 đ; N2, N3: 2.000.000 đ) |
| `Room` | building_id, tầng, loại phòng, giới tính, **trưởng phòng** (room_leader_id), status vận hành (Hoạt động / Bảo trì / Ngừng sử dụng) |
| `Bed` | room_id, mã giường, status (Hoạt động / Hỏng) |
| `RoomType` | số giường, **giá gốc theo học kỳ** chính và hè (nơi duy nhất lưu giá), diện tích, tiện ích |
| `Semester` | tên, năm học, loại (HK1 / HK2 / Hè), ngày bắt đầu / kết thúc |
| `RegistrationPeriod` | học kỳ, ngày mở / đóng, chỉ tiêu theo tòa |
| `RoomApplication` | student_id, period_id, **đơn vị nguyện vọng** (management_unit_id), loại phòng mong muốn, nguyện vọng, điểm ưu tiên, status |
| `DormitoryContract` | student_id, ngày bắt đầu / kết thúc, mức cọc yêu cầu, status. Đại diện cho **một lần lưu trú**; không lưu phòng, giá |
| `RoomAssignmentHistory` | student_id, contract_id, **bed_id**, từ / đến (đến để trống = đang hiệu lực), loại (giữ chỗ / đang ở), **đơn giá áp dụng**, lý do, người duyệt |
| `MoveOutRecord` | contract_id, tình trạng tài sản, công nợ còn lại, hoàn cọc, người xác nhận |
| `DepositTransaction` | contract_id, loại (thu / trừ / bù / hoàn), số tiền, ticket liên quan, hạn nộp, người xác nhận, thời gian |
| `SummerChoice` | contract_id, học kỳ hè, lựa chọn (ở hè / tạm rời hè / rời KTX), status |
| `StorageRecord` | contract_id, số kiện, mô tả, vị trí kho, ngày gửi / nhận lại |
| `ContractExtensionRequest` | contract_id, ngày kết thúc cũ / mới, status, người xử lý |
| `RoomTransferRequest` | student_id, phòng hiện tại, lý do, phòng mới, status |
| `MaintenanceRequest` | requester_id, room_id hoặc (building_id, tầng) cho khu vực chung, assigned_to, loại sự cố, mức độ, status, nguyên nhân (hao mòn tự nhiên / hư hỏng do sử dụng), chi phí sửa chữa |
| `UtilityReading` | room_id, loại đồng hồ, kỳ, chỉ số đầu / cuối |
| `Invoice` | **loại** (phí phòng / điện nước), student_id + học kỳ hoặc room_id + tháng, tổng tiền, ngày thông báo, hạn đóng, status |
| `InvoiceItem` | invoice_id, loại phí, số lượng, đơn giá, thành tiền |
| `Notification` | phạm vi (toàn KTX / tòa / phòng), nội dung, người đăng |
| `AuditLog` | user_id, hành động, đối tượng, giá trị cũ / mới, lý do, thời gian |

Các giá trị suy ra được (số người hiện tại, số chỗ trống, trạng thái "Đầy") **không lưu** mà tính từ `Bed` và phân phòng hiệu lực.

**Một nguồn dữ liệu cho mỗi thông tin:** "sinh viên đang ở đâu, với giá nào" chỉ được trả lời bởi dòng phân phòng hiệu lực (giường → phòng → tòa). Giá gốc chỉ nằm ở `RoomType` và được chụp vào phân phòng lúc phân / chuyển phòng, nên đổi giá sau này không ảnh hưởng người đang ở. Để tra cứu nhanh, dùng view `v_current_stay` (hợp đồng + phân phòng hiệu lực + giường + phòng). Phòng ghi trên hợp đồng giấy lấy từ dòng phân phòng đầu tiên của hợp đồng.

### 8.2. Quan hệ chính

- `ManagementUnit` 1–N `Building`, `RoomApplication`, `User` (Staff).
- `Building` 1–N `Room`; `RoomType` 1–N `Room`; `Room` 1–N `Bed`.
- `RegistrationPeriod` 1–N `RoomApplication`.
- `User` (sinh viên) 1–N `RoomApplication`, `DormitoryContract`, `MaintenanceRequest`, `RoomAssignmentHistory`, `Invoice`.
- `DormitoryContract` 1–N `RoomAssignmentHistory`: mỗi dòng là một khoảng phân phòng gắn với một giường; không ghi đè mất quá khứ.
- `Bed` 1–N `RoomAssignmentHistory` (tại một thời điểm chỉ một dòng hiệu lực).
- `Room` 1–N `MaintenanceRequest`, `UtilityReading`. Hợp đồng liên hệ với phòng gián tiếp qua phân phòng → giường → phòng.
- `Invoice` 1–N `InvoiceItem`; `User` (sinh viên) 1–N `Invoice` phí phòng; `Room` 1–N `Invoice` điện nước.
- `DormitoryContract` 1–N `DepositTransaction`, `SummerChoice`, `StorageRecord`.
- `DormitoryContract` 1–1 `MoveOutRecord` (khi trả phòng).
- `User` (Technician) 1–N `MaintenanceRequest` qua `assigned_to`.

### 8.3. Ràng buộc duy nhất

- `UtilityReading (room_id, meter_type, period)`
- `Invoice (student_id, semester_id)` với hóa đơn phí phòng; `Invoice (room_id, month)` với hóa đơn điện nước
- `RoomAssignmentHistory (bed_id) WHERE ended_at IS NULL` và `RoomAssignmentHistory (student_id) WHERE ended_at IS NULL` (partial unique index, xem BR-01).
- `RoomApplication (student_id, period_id)`: mỗi đợt một đơn.

Sinh viên chỉ xem hóa đơn phí phòng của mình, hóa đơn điện nước của phòng mình và sổ cọc của mình. Chi tiết danh mục và các bảng phụ sẽ được bổ sung trong tài liệu thiết kế CSDL.

---

## 9. Use Case

| Vai trò | Use Case |
|---|---|
| **Student** | UC-01 Login · UC-02 Update Profile · UC-03 View Room · UC-04 Submit Room Application · UC-05 View Application Status · UC-06 View Contract · UC-07 View Invoice & Deposit · UC-08 Submit Maintenance Request · UC-09 Track / Accept Maintenance Request · UC-10 Submit Room Transfer Request · UC-11 Submit Contract Extension Request · UC-12 View Notification · UC-37 Submit Summer Choice |
| **Staff** | UC-13 Manage Rooms · UC-14 Manage Students · UC-15 Review Room Application · UC-16 Assign Room · UC-17 Manage Contract · UC-18 Review Room Transfer Request · UC-19 Review Contract Extension · UC-20 Record Utility Reading · UC-21 Generate Invoice / Confirm Payment · UC-22 Manage Maintenance Request (gồm tạo ticket khu vực chung) · UC-23 Publish Notification · UC-24 View Dashboard · UC-31 Manage Registration Period · UC-32 Activate Contract · UC-33 Process Move-out · UC-34 Cancel Room Hold · UC-35 Assign Room Leader · UC-36 Manage Deposit · UC-38 Process Summer Choice / Storage |
| **Technician** | UC-25 View Assigned Tasks · UC-26 Update Repair Status · UC-27 Resolve Maintenance Request |
| **Admin** | UC-28 Manage Accounts (gồm import Excel, reset mật khẩu) · UC-29 Manage Roles & Scopes · UC-30 View Audit Log |

---

## 10. API sơ bộ

```text
# Authentication
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me

# Accounts
POST /api/users/import          (Admin, file Excel)
PUT  /api/users/{id}/reset-password
PUT  /api/auth/change-password

# Room & Bed
GET    /api/rooms
GET    /api/rooms/{id}
GET    /api/rooms/{id}/beds
POST   /api/rooms
PUT    /api/rooms/{id}
DELETE /api/rooms/{id}

# Registration Period & Application
POST /api/registration-periods
GET  /api/registration-periods/current
POST /api/applications
GET  /api/applications/my
GET  /api/applications
POST /api/applications/{id}/approve
POST /api/applications/{id}/reject

# Room Assignment & Contract
POST /api/assignments
GET  /api/assignments/student/{studentId}
POST /api/contracts/{id}/activate
POST /api/contracts/{id}/move-out
POST /api/transfer-requests
POST /api/transfer-requests/{id}/approve
POST /api/extension-requests
POST /api/extension-requests/{id}/approve

# Maintenance
POST /api/maintenance-requests
GET  /api/maintenance-requests/my
GET  /api/maintenance-requests
POST /api/maintenance-requests/{id}/assign
POST /api/maintenance-requests/{id}/status
POST /api/maintenance-requests/{id}/cost      (nguyên nhân, chi phí)

# Utility & Invoice
POST /api/utility-readings
POST /api/invoices/generate            (loại: phí phòng / điện nước)
GET  /api/invoices/my
GET  /api/deposits/my
POST /api/deposits/{contractId}/top-up
PUT  /api/rooms/{id}/leader

# Summer
POST /api/summer-choices
POST /api/summer-choices/{id}/approve
POST /api/storage-records/{id}/return
POST /api/invoices/{id}/confirm-payment
```

Hành động chuyển trạng thái dùng `POST /{id}/<hành động>`. Mọi endpoint đều kiểm tra vai trò, phạm vi dữ liệu và trạng thái hợp lệ của đối tượng.

---

## 11. Yêu cầu phi chức năng

| Nhóm | Yêu cầu |
|---|---|
| **Bảo mật** | Mật khẩu băm; phân quyền ở API theo từng đối tượng; validate input phía server; không lộ thông tin nhạy cảm; bí mật cấu hình ngoài mã nguồn |
| **Hiệu năng** | 95% request phản hồi dưới 2 giây với 100 người dùng đồng thời, dữ liệu ~1.500 sinh viên |
| **Tin cậy** | Không phân phòng vượt sức chứa kể cả khi có yêu cầu đồng thời; transaction cho phân phòng và chuyển phòng; không tạo hóa đơn trùng kỳ; tổng trừ cọc bằng chi phí sửa chữa |
| **Khả dụng** | Giao diện responsive; form có validation; thông báo lỗi rõ ràng |
| **Bảo trì** | Kiến trúc phân lớp, module theo nghiệp vụ; code convention; logging; unit test |

---

## 12. Phạm vi theo MoSCoW

MoSCoW ở đây dùng để ưu tiên **độ sâu trong từng phân hệ**, không loại phân hệ nào khỏi bảy phân hệ MVP.

| Mức | Nội dung |
|---|---|
| **Must Have** (bắt buộc) | Đủ bảy phân hệ MVP và bốn vai trò (gồm Technician); workflow chính, gồm cả chuyển phòng và gia hạn có lưu vết; trả phòng; RBAC theo phạm vi; lịch sử thay đổi; phí phòng theo học kỳ, điện nước theo phòng, sổ tiền cọc, trưởng phòng; học kỳ hè; thông báo, Dashboard cơ bản. **Cam kết đủ bảy phân hệ, trừ các hạng mục thuộc "đường cắt" dưới đây nếu kích hoạt** |
| **Should Have** (trong kế hoạch kỳ) | Lọc, tìm kiếm nâng cao; xuất báo cáo theo kỳ; nhắc việc ngay trên hệ thống |
| **Could Have** (sau khi lõi ổn định) | Email nhắc hạn; thông báo thời gian thực (WebSocket / SSE) |
| **Won't Have** (ngoài MVP) | QR ra vào, quản lý khách thăm, IoT đồng hồ điện nước, cổng thanh toán (VNPay / MoMo / ZaloPay), AI chatbot, ghép bạn cùng phòng, gợi ý phân phòng tự động |

- **Should** dự kiến làm trong kỳ sau khi Must đạt nghiệm thu; có thể cắt khi thiếu thời gian mà không phá luồng chính.
- **Could** chỉ làm khi còn nguồn lực, không cam kết trong kỳ.
- **Won't** được trình bày trong phần *Hướng phát triển* của báo cáo.

### 12.1. Đường cắt phạm vi

Cuối **tuần 7**, nhóm đánh giá phiên bản lõi (RBAC, phòng / giường, đơn, phân phòng, hợp đồng). Nếu lõi chưa đạt nghiệm thu nội bộ thì kích hoạt đường cắt: các hạng mục dưới đây **hạ từ Must xuống Should**, theo thứ tự:

1. Dashboard dạng biểu đồ (giữ lại các số liệu tổng hợp dạng bảng).
2. Thông báo theo phòng / theo tầng (giữ thông báo toàn KTX và theo tòa).
3. Gia hạn hợp đồng (BQL tạm cập nhật ngày kết thúc thủ công, vẫn có lưu vết).
4. Điểm ưu tiên tự động (BQL xếp thứ tự thủ công).

Các luồng không bị cắt: đăng ký → duyệt → phân phòng → hợp đồng, chuyển phòng, báo hỏng, điện nước và hóa đơn, trả phòng, RBAC theo tòa.

---

## 13. Kế hoạch triển khai (12 tuần)

| Tuần | Giai đoạn | Công việc | Mốc bàn giao |
|---|---|---|---|
| 01–02 | Phân tích | Khảo sát BQL, SRS, Use Case, chốt phạm vi | SRS, Use Case |
| 03–04 | Thiết kế | ERD, workflow, API, giao diện, tiêu chí kiểm thử | ERD, đặc tả API |
| 05–07 | Xây dựng lõi | RBAC, phòng / giường, dịch vụ chiếm / trả giường (xong tuần 5), đợt đăng ký, đơn, hợp đồng, chuyển phòng / gia hạn. **Cuối tuần 7: đánh giá và quyết định đường cắt (mục 12.1)** | Phiên bản lõi |
| 08–09 | Hoàn thiện | Bảo trì, hóa đơn, trả phòng, thông báo, báo cáo, tích hợp các luồng | Bản tích hợp |
| 10–12 | Nghiệm thu | Kiểm thử hệ thống, sửa lỗi, tài liệu, triển khai, demo | Gói triển khai, hướng dẫn sử dụng |

Mỗi nghiệp vụ có người phụ trách, người review và tiêu chí hoàn thành.

### 13.1. Phân công dự kiến

| Thành viên | Phụ trách chính | Review chéo |
|---|---|---|
| TV1: Nguyễn Văn Séc | Tài khoản, RBAC theo đơn vị quản lý, import Excel, `AuditLog`. Khung chung của backend: xác thực, xử lý lỗi, tác vụ định kỳ (`@Scheduled`) | Bảo mật, phân quyền cho toàn nhóm |
| TV2: Nguyễn Phú Nguyên | Tòa, phòng, giường, loại phòng; **dịch vụ chiếm / trả giường** (khóa, hai partial unique index); **chuyển phòng** | TV3 |
| TV3: Lê Đức Nguyên | Đợt đăng ký, đơn, điểm ưu tiên, xét duyệt; hợp đồng (kích hoạt, tự hủy khi quá hạn, gia hạn) | TV2 |
| TV4: Nguyễn Văn Quyền | Yêu cầu sửa chữa, giao việc kỹ thuật, thông báo; **trả phòng** (biên bản tài sản, hoàn cọc) | TV5 |
| TV5: Nguyễn Tiến Lộc | Điện nước, hóa đơn, kiểm tra công nợ (dùng cho bước trả phòng), Dashboard, báo cáo | TV4 |

**Nguyên tắc chia việc:** mọi thao tác chiếm hoặc trả giường (phân phòng, chuyển phòng, hủy giữ chỗ, trả phòng) đều gọi chung một dịch vụ do TV2 phụ trách, nên cơ chế khóa và ràng buộc chỉ viết một lần. Vì TV3 phụ thuộc dịch vụ này khi phân phòng, TV2 hoàn thành dịch vụ chiếm / trả giường trong **tuần 5**.

Phân công trên chỉ là phụ trách chính. Cả nhóm cùng tham gia phân tích yêu cầu, thiết kế CSDL, thiết kế API, kiến trúc, review code và kiểm thử.

---

## 14. Kiểm thử

Test được thiết kế từ giai đoạn yêu cầu và viết cùng lúc với chức năng, không dồn đến tuần cuối.

| Mức | Phạm vi |
|---|---|
| **Unit test** | Quy tắc nghiệp vụ, chuyển trạng thái hợp lệ / không hợp lệ |
| **Integration test** | Transaction, phân quyền API |
| **Kiểm thử đầu cuối** | Các luồng chính theo bốn vai trò |

**Các tình huống kiểm thử bắt buộc:**

- Truy cập chéo tòa (Staff K1 đọc / sửa dữ liệu N2 + N3).
- Hai yêu cầu đồng thời tranh một giường cuối cùng; chèn trực tiếp hai phân phòng vào cùng một giường phải bị CSDL từ chối.
- Hợp đồng PENDING quá hạn kích hoạt tự hủy và trả giường.
- Trừ cọc khi hư hỏng do sử dụng: tổng phần trừ của các sinh viên bằng chi phí sửa chữa; hao mòn tự nhiên không trừ cọc.
- Kích hoạt hợp đồng khi chưa nộp đủ mức cọc của tòa phải bị từ chối.
- Tạm rời hè: giường được trả, hợp đồng và cọc giữ nguyên; đầu HK1 phân lại giường không cần đơn mới.
- Trả phòng khi còn công nợ phải hiển thị cảnh báo; hoàn cọc bằng số dư cọc.
- Ticket RESOLVED quá 3 ngày tự đóng.
- Gia hạn trùng khoảng lưu trú.
- Rollback khi chuyển phòng lỗi giữa chừng.
- Ticket chuyển trạng thái sai (ví dụ OPEN → CLOSED, Technician tự đóng).
- Nhập chỉ số cuối nhỏ hơn chỉ số đầu.

---

## 15. Kịch bản demo

Dữ liệu minh họa được chuẩn bị sẵn, gồm tài khoản Student, Staff, Technician, Admin và dữ liệu các tòa K1, N2, N3.

| # | Kịch bản | Các bước | Kết quả mong đợi |
|---|---|---|---|
| 1 | Đăng ký thành công | Nộp đơn → duyệt → phân phòng → ký hợp đồng → tra cứu thông tin lưu trú | Đúng phòng, đúng trạng thái; hợp đồng ACTIVE |
| 2 | Tranh giường cuối cùng | Phòng A101 có 4 giường, 3 giường đã có người (ở + giữ chỗ); gửi đồng thời hai API phân phòng vào giường còn lại | Một yêu cầu thành công, một bị từ chối; 4/4 giường, không có bản ghi rác |
| 3 | Báo hỏng đến nghiệm thu | SV tạo yêu cầu → BQL giao → KTV sửa → SV nghiệm thu | Ticket CLOSED, đủ lịch sử người thao tác |

**Demo bổ sung:** hóa đơn điện nước theo phòng và trừ cọc khi hư hỏng; nhập chỉ số sai bị từ chối; chèn lỗi giữa nghiệp vụ chuyển phòng và xác nhận dữ liệu không thay đổi.

---

## 16. Sản phẩm bàn giao

1. Đề xuất đề tài (Project Proposal)
2. Đặc tả yêu cầu phần mềm (SRS)
3. Use Case Diagram và đặc tả Use Case
4. Activity Diagram, Sequence Diagram, State Diagram
5. ERD và thiết kế CSDL
6. Kiến trúc hệ thống và Deployment Diagram
7. Đặc tả API
8. Thiết kế giao diện (UI prototype)
9. Kế hoạch kiểm thử và test case
10. Mã nguồn, gói triển khai
11. Hướng dẫn sử dụng

---

## 17. Hướng phát triển

Các tính năng sau nằm ngoài MVP và được trình bày trong phần *Future Work*:

- **QR check-in / điểm danh ra vào:** lưu `AccessLog` (sinh viên, thời gian, cổng, vào / ra).
- **Quản lý khách thăm:** sinh viên đăng ký → BQL duyệt → cấp QR cho khách.
- **Thanh toán trực tuyến:** VNPay, MoMo, ZaloPay.
- **Thông báo thời gian thực và email** (nếu chưa làm ở mức Could).
- **Gợi ý phân phòng tự động:** chấm điểm phòng theo giới tính, loại phòng, chỗ trống, nguyện vọng tòa.
- **Ghép bạn cùng phòng:** theo giờ ngủ, thói quen học tập, mức độ sạch sẽ.
- **AI chatbot:** trả lời về nội quy, giờ đóng cửa, cách đăng ký, cách báo hỏng.
- **IoT:** đồng hồ điện nước thông minh cập nhật chỉ số tự động.

---

## 18. Câu hỏi dự kiến khi bảo vệ

| Câu hỏi | Trả lời |
|---|---|
| Vì sao không chỉ dùng Excel? | Cần kiểm soát quyền đồng thời với trạng thái nghiệp vụ và transaction, điều Excel không đảm bảo |
| Làm sao tránh phân phòng vượt chỗ? | Mỗi giường chỉ một phân phòng hiệu lực, được CSDL bảo vệ bằng ràng buộc; kết hợp khóa trong transaction và test tranh chấp đồng thời |
| Hai đơn vị dùng chung dữ liệu thế nào? | Một CSDL chung nhưng quyền theo đơn vị quản lý; đơn đăng ký thuộc đơn vị nguyện vọng; quyền tổng hợp liên đơn vị được cấp riêng |
| Vì sao không làm thanh toán online? | MVP ưu tiên hoàn thiện luồng thu phí và đối soát thủ công; cổng thanh toán là hướng phát triển |
| Vì sao điện nước không chia cho từng sinh viên? | Theo quy trình thực tế của KTX: hóa đơn tính theo phòng, các thành viên tự chia và trưởng phòng nộp |

---

## 19. Các quyết định thiết kế và điểm cần xác nhận

### 19.1. Quyết định đã chốt

| Vấn đề | Quyết định |
|---|---|
| Đợt đăng ký, ưu tiên | Có `RegistrationPeriod`; điểm ưu tiên đơn giản chỉ dùng để sắp xếp, BQL duyệt cuối |
| Kích hoạt hợp đồng | Ký giấy, Staff kích hoạt; quá N ngày (mặc định 7) tự hủy và trả giường |
| Trả phòng | Staff thực hiện: kiểm tra tài sản, công nợ, đóng phân phòng, hoàn cọc bằng số dư cọc |
| Tạo tài khoản sinh viên | Admin import Excel, mật khẩu mặc định, bắt buộc đổi ở lần đăng nhập đầu |
| Phạm vi dữ liệu | Theo **đơn vị quản lý** (`ManagementUnit`); đơn đăng ký thuộc đơn vị nguyện vọng; chuyển phòng chỉ trong cùng đơn vị |
| Vòng đời phân phòng | Kích hoạt hợp đồng đổi loại giữ chỗ → đang ở trên cùng dòng; hai partial unique index chặn trùng giường và trùng sinh viên; đơn rời ASSIGNED sang APPROVED (phân lại) hoặc CANCELLED |
| Quên mật khẩu | Admin / Staff đặt lại, không dùng email |
| Ticket | Staff tạo ticket khu vực chung; RESOLVED quá 3 ngày tự đóng |
| Sức chứa | Mô hình hóa theo **giường** (`Bed`); CSDL chặn trùng giường |
| Phòng và giá | Chỉ lưu trên **phân phòng** (đơn giá chụp từ `RoomType`); hợp đồng không lưu `room_id`, giá |
| Hóa đơn | Phí phòng theo **sinh viên × học kỳ** (HK1, HK2, Hè), thu đủ kỳ; điện nước theo **phòng × tháng**, trưởng phòng nộp; hạn đóng 7 ngày sau thông báo |
| Tiền cọc | Thu khi ký hợp đồng (K1 1 triệu, N2 / N3 2 triệu); trừ đều khi hư hỏng do sử dụng, hao mòn tự nhiên không trừ; bù trong 7 ngày, quá hạn thì nhắc; hoàn bằng số dư khi rời KTX |
| Học kỳ hè | Ở hè đóng phí như HK1, HK2; tạm rời hè gửi đồ vào kho, giữ hợp đồng và cọc |
| Dữ liệu suy ra | Không lưu số người hiện tại / trạng thái Đầy |
| Nhật ký | Có `AuditLog`; bỏ `Permission` chi tiết |
| Phạm vi | Giữ đủ 7 phân hệ là Must, có **đường cắt** quyết định ở cuối tuần 7 (mục 12.1) |

### 19.2. Điểm cần xác nhận khi khảo sát BQL

Các câu hỏi nghiệp vụ đã được chốt (xem `SRS-DMS.md`, mục 9). Khảo sát BQL ở tuần 1–2 tập trung đối chiếu số liệu: số giường thực tế từng tòa, biểu giá điện nước, giá phòng từng loại, danh sách diện ưu tiên và trọng số.

### 19.3. Hạn chế đã biết

- Phí phòng thu đủ học kỳ, không tính theo số ngày ở.
- Tiền điện nước không chia cho từng sinh viên trong hệ thống; trừ cọc chia đều, không xác định người làm hỏng.
- Nhập chỉ số vẫn thủ công, chưa có đồng hồ thông minh.
- Chỉ chuyển phòng trong cùng đơn vị quản lý; chuyển sang đơn vị khác phải trả phòng rồi đăng ký lại.
- Mỗi đợt sinh viên chỉ chọn một đơn vị nguyện vọng.

---

## 20. Kết luận

DMS giải quyết bài toán dữ liệu phân tán giữa hai đơn vị quản lý ký túc xá bằng một hệ thống tập trung, phân quyền theo đơn vị quản lý. Giá trị của đề tài nằm ở **quy trình có trạng thái, quy tắc nghiệp vụ nhất quán và lịch sử lưu trú đầy đủ**, với phạm vi đủ để triển khai trong một học kỳ theo kế hoạch đề xuất, đồng thời thể hiện trọn quy trình:

> **Yêu cầu → Phân tích → Thiết kế → Cài đặt → Kiểm thử**

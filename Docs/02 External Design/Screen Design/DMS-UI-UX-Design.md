# DMS UI/UX Design Specification

> Thiết kế frontend cho hệ thống **Dormitory Management System (DMS)**  
> Cơ sở: `SRS-DMS.md` — MVP  
> Frontend: React SPA  
> Backend: Spring Boot REST API  
> Database: PostgreSQL  
> Responsive: từ 360px
> Phiên bản: 1.2, 30/10/2026

### Lịch sử thay đổi

| Phiên bản | Ngày | Nội dung |
|---|---|---|
| 1.2 | 30/10/2026 | TV1: làm rõ preview đã đọc được dữ liệu hợp lệ, chỉ hiện bảng khi có lỗi/mã trùng; báo tên cột thiếu, buộc kiểm tra lại sau lỗi ghi; giữ preview/tài khoản khi đổi tab và chặn thay/xóa file trước khi tải tài khoản |
| 1.1 | 22/10/2026 | Bổ sung mô tả triển khai frontend TV5: màn hình Quản lý sổ cọc (bảng số dư, chi tiết ledger giao dịch, dialog nộp/bù/hoàn cọc), Quản lý hóa đơn & công nợ (modal phát hành tiền phòng/điện nước hàng loạt, dialog xác nhận thanh toán), và Staff Dashboard (KPI thẻ số liệu phòng, giường, công nợ, ticket) |
| 1.0 | 21/10/2026 | Bắt đầu ghi phiên bản; giữ thiết kế hiện có, bổ sung luồng import và đọc chi tiết audit đã triển khai bởi TV1 |

---

## 1. Mục tiêu thiết kế

DMS phục vụ 4 nhóm người dùng:

| Vai trò | Thiết bị chính | Đặc điểm sử dụng | Ưu tiên UX |
|---|---|---|---|
| Student | Điện thoại | Không thường xuyên | Đơn giản, dễ hiểu, mobile-first |
| Staff | Máy tính | Nghiệp vụ hằng ngày | Nhanh, nhiều dữ liệu, thao tác rõ |
| Technician | Điện thoại | Tại hiện trường | Tối giản, thao tác nhanh |
| Admin | Máy tính | Quản trị hệ thống | Data-management, chính xác |

### Nguyên tắc UX cốt lõi

Mỗi màn hình nghiệp vụ phải trả lời được 3 câu hỏi:

1. **Tôi đang ở đâu?**
2. **Dữ liệu đang ở trạng thái nào?**
3. **Tôi có thể làm gì tiếp theo?**

Thiết kế ưu tiên **state + action** thay vì hiển thị tất cả thao tác cùng lúc.

---

## 2. Định hướng visual

> Cập nhật triển khai 15/10/2026: giao diện dùng nền trắng/xám, màu xanh trầm làm điểm nhấn, không dùng gradient trang trí. Bộ màu và shadow khai một lần tại `app/theme.ts`, dùng chung cho CSS và Ant Design. Các màn ở cả bốn cổng dùng component `FormControls`, `DataTable`, `Surface`, `FormField`, `PageHeader` và `ResponsiveTable`; không tự tạo biến thể input/table/button theo từng màn. Thanh tìm kiếm/bộ lọc tự xuống hàng; form hai cột ở desktop, một cột dưới 640px. Giữ nguyên tên trường, lỗi validation, quyền và luồng CRUD. Khi mở màn phân quyền, mặc định xem ma trận ADMIN đầy đủ; bộ chọn ghi rõ vai trò đang phân quyền.

Phong cách tổng thể:

**University Portal + Modern SaaS Dashboard + Mobile App**

### 2.1. Layout

Desktop:

```text
┌──────────────────────────────────────────────────────────────┐
│ Sidebar │ Topbar: Search · Notifications · User              │
├─────────┼────────────────────────────────────────────────────┤
│         │                                                    │
│ Menu    │ Page Header                                        │
│         │                                                    │
│         │ Content                                             │
│         │                                                    │
│         │                                                    │
└─────────┴────────────────────────────────────────────────────┘
```

Mobile:

```text
┌────────────────────────────┐
│ Header / Page title        │
├────────────────────────────┤
│                            │
│ Content                    │
│                            │
│                            │
├────────────────────────────┤
│ Home │ Phòng │ Hóa đơn │ Báo hỏng │
└────────────────────────────┘
```

### 2.1a. Design tokens

Định nghĩa một lần (CSS variables hoặc theme của thư viện UI), cả nhóm dùng chung:

| Token | Giá trị |
|---|---|
| Font | Be Vietnam Pro (hỗ trợ dấu tiếng Việt tốt); fallback Inter, system-ui |
| Cỡ chữ | 12 / 14 (body) / 16 / 20 / 24 / 30 px |
| Spacing | Bội số 4: 4 / 8 / 12 / 16 / 24 / 32 px |
| Bo góc | 6 px (input, button), 10 px (card) |
| Màu chính | Primary `#1D4ED8`, nền `#F5F7FA`, card `#FFFFFF`, chữ `#1F2937`, chữ phụ `#6B7280` |
| Màu trạng thái | Success `#15803D`, Warning `#B45309`, Danger `#B91C1C`, Info `#0369A1`, Neutral `#4B5563` |
| Tiền | `1.000.000 đ` (dấu chấm phân cách nghìn) |
| Ngày giờ | `dd/MM/yyyy`, `dd/MM/yyyy HH:mm` |

Các mã màu trạng thái chọn đậm để chữ trắng trên nền màu và chữ màu trên nền trắng đều đạt tương phản ≥ 4.5:1.

### 2.2. Màu sắc

Đề xuất dùng nền sáng, card trắng, màu chủ đạo xanh dương theo phong cách hệ thống đại học.

Hệ thống trạng thái gồm **ba lớp hiển thị khác nhau**. Không trộn chúng trong một bảng màu.

#### Lớp 1: Trạng thái lưu trong CSDL → **badge đặc**

Nhãn tiếng Việt **theo từng đối tượng**: cùng một enum có thể mang nghĩa khác nhau (ASSIGNED ở đơn là "Đã giữ chỗ", ở ticket là "Đã giao KTV").

| Đối tượng | Enum | Nhãn hiển thị | Màu |
|---|---|---|---|
| Đơn đăng ký | DRAFT | Nháp | Gray |
| | SUBMITTED | Đã gửi | Blue |
| | APPROVED | Đã duyệt | Cyan |
| | ASSIGNED | Đã giữ chỗ | Purple |
| | REJECTED | Bị từ chối | Red |
| | CANCELLED | Đã hủy | Gray |
| Hợp đồng | PENDING | Chờ kích hoạt | Orange |
| | ACTIVE | Đang hiệu lực | Green |
| | EXPIRED | Đã kết thúc | Gray |
| | TERMINATED | Chấm dứt trước hạn | Gray |
| | CANCELLED | Đã hủy | Gray |
| Ticket sửa chữa | OPEN | Chờ xử lý | Blue |
| | ASSIGNED | Đã giao KTV | Purple |
| | IN_PROGRESS | Đang sửa | Orange |
| | RESOLVED | Chờ nghiệm thu | Cyan |
| | CLOSED | Đã đóng | Green |
| | REJECTED | Bị từ chối | Red |
| | CANCELLED | Đã hủy | Gray |
| Hóa đơn | UNPAID | Chưa thanh toán | Orange |
| | PAID | Đã thanh toán | Green |
| Yêu cầu (gia hạn, chuyển phòng, lựa chọn hè) | PENDING / APPROVED / REJECTED | Chờ duyệt / Đã duyệt / Bị từ chối | Orange / Green / Red |

#### Lớp 2: Nhãn suy ra → **chip viền + icon ⚠**

Không lưu trong CSDL, tính từ ngày và dữ liệu gốc (BR-08). Luôn đặt **cạnh** badge trạng thái, không thay thế nó.

| Nhãn | Gắn với | Điều kiện | Kiểu |
|---|---|---|---|
| Sắp hết hạn | Hợp đồng Đang hiệu lực | Còn ≤ 15 ngày | Viền vàng |
| Quá hạn trả phòng | Hợp đồng Đang hiệu lực | Đã qua ngày kết thúc | Viền đỏ |
| Tạm rời hè | Hợp đồng Đang hiệu lực | Có phiếu gửi đồ chưa nhận lại | Viền xám |
| Quá hạn | Hóa đơn Chưa thanh toán | Đã qua hạn đóng | Viền đỏ |
| Quá hạn bù cọc | Khoản bù cọc | Quá 7 ngày chưa nộp | Viền đỏ |

Ví dụ: `[Đang hiệu lực]` `⚠ Sắp hết hạn`

#### Lớp 3: Phòng và giường → **hai thuộc tính tách biệt**

| Thuộc tính | Giá trị | Hiển thị |
|---|---|---|
| Vận hành phòng | Hoạt động / Bảo trì / Ngừng sử dụng | Badge ở tiêu đề phòng; Hoạt động thì không cần hiện |
| Vận hành giường | Hoạt động / Hỏng | Ô giường gạch chéo khi Hỏng |
| Chiếm dụng giường (tính ra) | Trống / Đang giữ / Đang ở | Màu nền ô giường: trắng / vàng nhạt / xanh |

Một giường Hỏng không có chiếm dụng; một giường chỉ "Đang giữ" hoặc "Đang ở" khi đang Hoạt động.

#### Quy tắc triển khai

- Người dùng **không bao giờ thấy enum tiếng Anh**. Enum chỉ dùng trong tài liệu và code.
- Mọi nhãn, màu, icon lấy từ **một file** `src/constants/statusConfig.ts`, khóa theo `(entity, status)`. Không hard-code nhãn trong từng component.

> Không chỉ dùng màu để biểu diễn trạng thái; luôn kết hợp text/badge/icon để dễ đọc.

---

# 3. Information Architecture

## 3.1. Student Portal

```text
Student
├── Trang chủ
├── Phòng của tôi
├── Đơn đăng ký
│   ├── Danh sách đơn
│   └── Chi tiết đơn
├── Hợp đồng
│   ├── Chi tiết hợp đồng (kèm lịch sử phân phòng)
│   └── Yêu cầu gia hạn / chuyển phòng
├── Hóa đơn (phí phòng, điện nước phòng, công nợ)
├── Tiền cọc
├── Báo hỏng
│   ├── Danh sách ticket
│   └── Chi tiết ticket
├── Lựa chọn kỳ hè
├── Thông báo
└── Hồ sơ / Đổi mật khẩu
```

## 3.2. Staff Portal

```text
Staff
├── Dashboard
├── Yêu cầu chờ xử lý
├── Đơn đăng ký
├── Phòng & Giường
├── Sinh viên
├── Hợp đồng
├── Kỳ hè & Kho gửi đồ
├── Điện nước (chỉ số, đơn giá)
├── Hóa đơn
├── Tiền cọc / Công nợ
├── Sửa chữa
├── Thông báo
├── Loại phòng
├── Kỳ học
└── Đợt đăng ký
```

## 3.3. Technician Portal

```text
Technician
├── Việc của tôi
└── Chi tiết công việc
```

## 3.4. Admin Portal

```text
Admin
├── Tài khoản
├── Import sinh viên
├── Đơn vị & Tòa nhà (mức cọc)
├── Quyền tổng hợp liên đơn vị
└── Nhật ký hệ thống
```

---

# 4. Navigation

## 4.1. Student

### Mobile

Dùng bottom navigation:

```text
┌────────┬────────┬────────┬────────┐
│  Home  │ Phòng  │ Hóa đơn│Báo hỏng│
└────────┴────────┴────────┴────────┘
```

- Header: icon bell (Thông báo, có số chưa đọc) và avatar (Hồ sơ, Hợp đồng, Tiền cọc, Kỳ hè, Đổi mật khẩu, Đăng xuất).
- Tab Báo hỏng chỉ bật khi sinh viên đang ở; nếu không, mở trang giải thích lý do.

### Desktop

Sidebar có thể chuyển thành navigation ngang/sidebar tùy breakpoint.

---

## 4.2. Staff

Sidebar cố định trên desktop:

```text
DMS
────────────────
Dashboard
Yêu cầu chờ xử lý   (12)

QUẢN LÝ
  Đơn đăng ký
  Phòng & Giường
  Sinh viên
  Hợp đồng
  Kỳ hè & Kho gửi đồ

TÀI CHÍNH
  Điện nước
  Hóa đơn
  Tiền cọc

VẬN HÀNH
  Sửa chữa
  Thông báo

HỆ THỐNG
  Loại phòng
  Kỳ học
  Đợt đăng ký
```

---

## 4.3. Technician

Không cần sidebar phức tạp.

Ưu tiên:

```text
Header
  └── Thông báo / Profile

Việc của tôi
  ├── Mới
  ├── Đang xử lý
  └── Hoàn thành
```

---

# 5. Shared Design System

## 5.1. UI components

```text
components/
├── ui/
│   ├── Button
│   ├── Input
│   ├── Select
│   ├── DatePicker
│   ├── Modal
│   ├── Drawer
│   ├── Badge
│   ├── Card
│   ├── Tabs
│   ├── Table
│   ├── Pagination
│   ├── Dropdown
│   ├── Toast
│   ├── ConfirmDialog
│   ├── EmptyState
│   ├── Skeleton
│   └── Timeline
│
└── business/
    ├── RoomCard
    ├── BedCard
    ├── ContractStatus
    ├── InvoiceStatus
    ├── MaintenanceTimeline
    ├── DepositLedger
    ├── OccupancyChart
    └── NotificationItem
```

## 5.2. Confirmation

Các thao tác không thể hoàn tác phải có Confirm Dialog:

- Từ chối đơn
- Hủy
- Trả phòng
- Xác nhận thanh toán
- Các thao tác thay đổi trạng thái quan trọng

Nếu nghiệp vụ yêu cầu lý do:

```text
┌──────────────────────────────────┐
│ Xác nhận từ chối đơn             │
├──────────────────────────────────┤
│ Bạn có chắc muốn từ chối đơn?    │
│                                  │
│ Lý do *                          │
│ ┌──────────────────────────────┐ │
│ │ Nhập lý do...                │ │
│ └──────────────────────────────┘ │
│                                  │
│       Hủy       Xác nhận         │
└──────────────────────────────────┘
```

## 5.3. Error message

Không dùng:

> Có lỗi xảy ra.

Nên dùng:

> **Giường A101-4 vừa được phân cho sinh viên khác. Vui lòng chọn giường khác.**

Error phải nói rõ:

**Nguyên nhân → Cách xử lý**

---

# 6. Student UX

## 6.1. Student Dashboard

Mục tiêu: sinh viên nhìn vào là biết ngay tình trạng hiện tại.

```text
┌──────────────────────────────┐
│ Xin chào, Nguyễn Văn A 👋    │
│ 🔔 2 thông báo               │
├──────────────────────────────┤
│ PHÒNG HIỆN TẠI               │
│                              │
│ K1 - A101                    │
│ Giường: A101-3               │
│ Trưởng phòng: Nguyễn B       │
│                              │
│ [Xem phòng]                  │
├──────────────────────────────┤
│ HỢP ĐỒNG                     │
│ Đang hiệu lực                │
│ Còn 92 ngày                  │
│ [Xem hợp đồng]               │
├──────────────────────────────┤
│ HÓA ĐƠN                      │
│ 1 khoản chưa thanh toán      │
│ 1.200.000 đ                  │
│ [Xem hóa đơn]                │
├──────────────────────────────┤
│ THAO TÁC NHANH               │
│ [Đăng ký] [Báo hỏng]         │
└──────────────────────────────┘
```

Không nên biến Student Dashboard thành dashboard thống kê kiểu Staff.

Wireframe trên là biến thể **Đang ở**. Dashboard có 6 biến thể; card chính đổi theo tình trạng sinh viên:

| Biến thể | Điều kiện | Card chính | CTA |
|---|---|---|---|
| Chưa có đơn | Không có đơn trong đợt, không có hợp đồng | Đợt đăng ký đang mở (hạn nộp) hoặc "Chưa có đợt đăng ký" | [Đăng ký ở KTX] |
| Đơn đang xét | Đơn Đã gửi / Đã duyệt | Timeline đơn rút gọn | [Xem đơn], [Hủy đơn] |
| Được giữ chỗ | Đơn Đã giữ chỗ, hợp đồng Chờ kích hoạt | Mục 6.1a | [Hủy giữ chỗ] |
| Đang ở | Hợp đồng Đang hiệu lực, có giường | Wireframe trên | [Báo hỏng] |
| Tạm rời hè | Hợp đồng Đang hiệu lực, nhãn Tạm rời hè | Phiếu gửi đồ (số kiện, vị trí kho), ngày nhận lại dự kiến | [Xem phiếu] |
| Đã trả phòng | Hợp đồng Đã kết thúc / Chấm dứt | Công nợ còn lại (nếu có), số tiền hoàn cọc đã nhận | [Xem công nợ] |

Card Hóa đơn và Thông báo giữ nguyên ở các biến thể có hợp đồng. Đơn bị từ chối hoặc bị hủy hiển thị một dòng thông báo kèm lý do trên biến thể "Chưa có đơn".

### 6.1a. Biến thể "Được giữ chỗ" (đơn ASSIGNED, hợp đồng PENDING)

Sau khi Staff phân giường, sinh viên có **7 ngày** để ký giấy và nộp cọc (FR-REG-12, FR-CON-02). Card này đứng đầu dashboard, thay cho card "Phòng hiện tại":

```text
┌──────────────────────────────┐
│ ⏳ BẠN ĐƯỢC GIỮ CHỖ          │
│                              │
│ Giường K1-A101-03            │
│ Phòng 6 người · 1.500.000 đ/HK│
│                              │
│ Hạn kích hoạt: 15/10/2026    │
│ Còn 5 ngày                   │
├──────────────────────────────┤
│ CẦN MANG ĐẾN BQL K1          │
│ ☐ Hợp đồng đã ký             │
│ ☐ Tiền cọc 1.000.000 đ       │
├──────────────────────────────┤
│ [Xem chi tiết]  [Hủy giữ chỗ]│
└──────────────────────────────┘
```

- Mức cọc lấy theo tòa (K1 1.000.000 đ; N2 / N3 2.000.000 đ).
- Còn ≤ 2 ngày: đổi card sang màu cảnh báo.
- [Hủy giữ chỗ] mở Confirm Dialog: "Giường sẽ được trả lại và đơn chuyển sang Đã hủy. Không thể hoàn tác."
- Quá hạn mà chưa kích hoạt: hệ thống tự hủy, card đổi thành "Giữ chỗ đã hết hạn" kèm lý do.

---

# 7. Student - Phòng của tôi

Đây là một trong những màn hình nên ưu tiên thiết kế đẹp.

```text
┌──────────────────────────────────┐
│ ← Phòng của tôi                  │
├──────────────────────────────────┤
│ K1 · A101                        │
│ Phòng 6 người · Nam              │
├──────────────────────────────────┤
│ GIƯỜNG                            │
│                                  │
│ ┌──────┐ ┌──────┐ ┌──────┐      │
│ │  01  │ │  02  │ │ ★ 03 │      │
│ │ Đang │ │ Đang │ │  Bạn  │      │
│ │ ở    │ │ ở    │ │      │      │
│ └──────┘ └──────┘ └──────┘      │
│                                  │
│ ┌──────┐ ┌──────┐ ┌──────┐      │
│ │  04  │ │  05  │ │  06  │      │
│ │ Trống│ │ Đang │ │ Đang │      │
│ │      │ │ ở    │ │ ở    │      │
│ └──────┘ └──────┘ └──────┘      │
├──────────────────────────────────┤
│ TRƯỞNG PHÒNG                     │
│ Nguyễn Văn B                     │
├──────────────────────────────────┤
│ THÀNH VIÊN                       │
│ Nguyễn Văn B                     │
│ Trần Văn C                       │
│ ...                              │
└──────────────────────────────────┘
```

Mục tiêu là giúp sinh viên **nhìn thấy phòng**, thay vì chỉ đọc một bảng dữ liệu.

---

# 8. Student - Đăng ký phòng

Dùng Stepper 3 bước nhập liệu, sau đó là màn kết quả:

```text
① Thông tin  →  ② Nguyện vọng  →  ③ Xác nhận   ⇒   Đã gửi
```

**Bước 1 kiểm tra điều kiện trước** (FR-REG-02, FR-REG-03). Nếu không đủ điều kiện thì không cho đi tiếp và nói rõ lý do:

- "Không có đợt đăng ký nào đang mở." 
- "Bạn đã có đơn trong đợt này." kèm link tới đơn.
- "Bạn đang có hợp đồng. Sinh viên đang ở dùng chức năng gia hạn." kèm link.

Thông tin cá nhân ở bước 1 lấy từ hồ sơ (họ tên, MSSV, giới tính chỉ đọc; số điện thoại, liên hệ khẩn cấp sửa được).

Ví dụ bước 2:

```text
Bước 2 / 3

NGUYỆN VỌNG PHÒNG

Đơn vị *
[ K1                         ▼ ]

Loại phòng mong muốn *
[ 6 người · 1.500.000 đ/HK   ▼ ]

Diện ưu tiên (tự khai)
[ ] Hộ nghèo / cận nghèo
[ ] Con thương binh, liệt sĩ
[ ] Ở xa (> 100 km)
Staff sẽ xác nhận diện ưu tiên khi xét đơn.

Ghi chú
[.............................]

────────────────────────────

              [Quay lại] [Tiếp tục]
```

Đơn được lưu nháp tự động; sinh viên có thể quay lại hoàn thành trước khi đợt đóng. Mục tiêu NFR-USA-02: sinh viên mới nộp xong trong ≤ 5 phút.

Sau khi gửi, không chỉ hiện một badge.

Nên có Application Timeline:

```text
● Đã gửi đơn
│  08/10/2026 09:32
│
● Đang xét duyệt
│
○ Được duyệt
│
○ Được giữ chỗ
│  Giường K1-A101-03 · hạn kích hoạt 15/10
│
○ Đã kích hoạt hợp đồng
```

Nhánh kết thúc khác hiển thị ngay trên timeline, có lý do:

- **Bị từ chối**: lý do của Staff.
- **Đã hủy**: do sinh viên tự hủy, hoặc "tự động hủy" vì quá hạn kích hoạt.

---

# 9. Student - Hợp đồng & Tiền cọc

Trang hợp đồng nên gom thông tin quan trọng:

```text
HỢP ĐỒNG

┌────────────────────────────┐
│ Đang hiệu lực              │
│ Hợp đồng #HD-2026-0012     │
├────────────────────────────┤
│ Phòng       K1 - A101      │
│ Giường      A101-03        │
│ Ngày bắt đầu 01/09/2026    │
│ Ngày kết thúc 30/06/2027  │
└────────────────────────────┘

TIỀN CỌC

Mức cọc        1.000.000 đ
Số dư          1.000.000 đ
Cần bù         0 đ

[Lịch sử giao dịch]
```

Sổ cọc nên thể hiện dạng ledger:

```text
+1.000.000 đ  Thu cọc
01/09/2026

-200.000 đ    Trừ cọc sửa chữa
12/10/2026

+200.000 đ    Bù cọc
15/10/2026

────────────────────
Số dư: 1.000.000 đ
```

---

# 10. Student - Hóa đơn

Phân biệt rõ:

- Phí phòng
- Điện nước phòng
- Khoản cần bù cọc

Ví dụ card:

```text
Phí phòng · HK1 2026-2027

1.500.000 đ

Hạn thanh toán: 15/09/2026

[Quá hạn]

[Xem chi tiết]
```

Hóa đơn điện nước hiển thị rõ đây là hóa đơn của **phòng**, không phải số tiền đã chia cho từng sinh viên (FR-BIL-04). Mọi thành viên đang ở trong phòng đều xem được.

```text
Điện nước phòng K1-A101 · 10/2026

Điện  150 kWh × 3.000 đ     450.000 đ
Nước   12 m³  × 15.000 đ    180.000 đ
───────────────────────────────────
Tổng                        630.000 đ

Người nộp: Nguyễn Văn B (trưởng phòng)
Hạn thanh toán: 22/10/2026
[Chưa thanh toán]
```

Nếu người xem **là trưởng phòng**, card có thêm banner:

```text
┌──────────────────────────────────────┐
│ ★ Bạn là trưởng phòng                │
│ Bạn thu tiền các bạn cùng phòng và   │
│ nộp hóa đơn này cho BQL K1.          │
└──────────────────────────────────────┘
```

Phòng chưa có trưởng phòng: dòng "Người nộp" hiện "Chưa có trưởng phòng, liên hệ BQL".

---

# 11. Student - Báo hỏng

Sinh viên chỉ báo hỏng cho **phòng đang ở** (BR-06). Phòng được điền sẵn, không có bước chọn khu vực. Khu vực chung (hành lang, nhà vệ sinh tầng…) do Staff tạo ticket.

Form một trang:

```text
BÁO HỎNG · Phòng K1-A101

Loại sự cố *
[ Điện | Nước | Đồ dùng | Cửa/khóa | Khác ▼ ]

Mô tả *
[..........................................]

Mức độ *
( ) Thấp   (●) Trung bình   ( ) Cao

Ảnh (tối đa 3)
[ + ] [ + ] [ + ]

                                  [Gửi]
```

- Sinh viên chưa có phân phòng "Đang ở" (đang giữ chỗ, tạm rời hè): nút Báo hỏng bị ẩn, kèm giải thích ở trang Báo hỏng.
- Ticket "Chờ xử lý" của chính mình có nút [Hủy yêu cầu].

Chi tiết ticket:

```text
#TK-1024

Vòi nước bị rò
Phòng K1-A101

● Chờ xử lý
│
● Đã giao KTV
│  KTV: Nguyễn C
│
● Đang sửa
│
○ Chờ nghiệm thu
│
○ Đã đóng
```

Khi ticket ở **Chờ nghiệm thu**, người báo hỏng thấy:

```text
┌──────────────────────────────────────┐
│ KTV đã báo sửa xong · 10/10 15:20    │
│ "Đã thay gioăng vòi nước"            │
│                                      │
│ Tự đóng sau 3 ngày nếu không phản hồi│
│                                      │
│ [Chưa đạt]            [Chấp nhận]    │
└──────────────────────────────────────┘
```

- [Chưa đạt] bắt buộc lý do; ticket quay lại "Đang sửa" (FR-MNT-05).
- Ticket đã đóng hiển thị kết luận chi phí nếu có: "Hư hỏng do sử dụng · 200.000 đ · bạn bị trừ 50.000 đ tiền cọc".

Timeline là component dùng chung với Staff.

---

# 12. Staff Dashboard

Dashboard Staff phải tập trung vào **việc cần xử lý**, đồng thời đáp ứng các chỉ số trong SRS.

```text
┌────────────────────────────────────────────────────────────┐
│ Dashboard                                  K1 ▼            │
├────────────────────────────────────────────────────────────┤
│                                                            │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────┐ │
│ │ 82%        │ │ 12         │ │ 34.5M      │ │ 18       │ │
│ │ Giường dùng│ │ Giường BT  │ │ Chưa thu   │ │ Ticket   │ │
│ └────────────┘ └────────────┘ └────────────┘ └──────────┘ │
│                                                            │
│ ┌──────────────────────────┐ ┌───────────────────────────┐ │
│ │ Tình trạng ticket        │ │ Sử dụng giường            │ │
│ │                          │ │                           │ │
│ │ Chờ xử lý   5            │ │      █████████ 82%        │ │
│ │ Đã giao KTV 4            │ │                           │ │
│ │ Đang sửa    6            │ │ Đang ở / Tổng sức chứa   │ │
│ │ Chờ nghiệm thu 3         │ │                           │ │
│ └──────────────────────────┘ └───────────────────────────┘ │
│                                                            │
│ ⚠ Hợp đồng sắp hết hạn                         12         │
│ ⚠ Quá hạn trả phòng                            4          │
│ ⚠ Phòng chưa có trưởng phòng                   7          │
│ ⚠ Bù cọc chưa nộp                              9          │
└────────────────────────────────────────────────────────────┘
```

Không nên nhồi quá nhiều biểu đồ. Mỗi chart phải giúp Staff đưa ra quyết định.

- Bộ chọn đơn vị (`K1 ▼`) **chỉ hiện với Staff có quyền tổng hợp liên đơn vị** (FR-ACC-08). Staff thường thấy tên đơn vị của mình, không có dropdown.
- Mỗi dòng ⚠ và mỗi số KPI là **link** tới danh sách đã lọc sẵn (ví dụ "Hợp đồng sắp hết hạn 12" mở `/staff/contracts?label=sap-het-han`).
- Phần biểu đồ là ✂ (FR-NOT-06): bản đầu chỉ cần số liệu dạng card và danh sách; biểu đồ làm khi còn thời gian.

---

# 13. Staff - Đơn đăng ký

List page:

```text
ĐƠN ĐĂNG KÝ

[ Tìm sinh viên... ] [Trạng thái ▼] [Đợt ▼]

┌──────────┬────────────┬───────────┬───────────┬─────────┐
│ Mã SV    │ Sinh viên  │ Điểm ưu tiên│ Trạng thái│ Action │
├──────────┼────────────┼───────────┼───────────┼─────────┤
│ 20230001 │ Nguyễn A   │ 85        │ Đã gửi    │ Xem →   │
│ 20230002 │ Trần B     │ 72        │ Đã duyệt  │ Xem →   │
└──────────┴────────────┴───────────┴───────────┴─────────┘

                    [1] [2] [3] ... [10]
```

Có filter theo status và pagination.

---

# 14. Staff - Xét duyệt → Phân phòng

Đây là một **backbone screen** quan trọng. Cùng một trang chi tiết đơn, nhưng **nội dung và nút thay đổi theo trạng thái đơn** (Rule 02).

## 14.1. Đơn SUBMITTED: chỉ xét duyệt

```text
┌──────────────────────────────────────────────────────────────┐
│ ĐƠN #DK-2026-0012                          Đã gửi            │
├──────────────────────────────────────────────────────────────┤
│ SINH VIÊN                    NGUYỆN VỌNG                     │
│ Nguyễn Văn A                 Đơn vị: K1                      │
│ MSSV: 20230001 · Nam         Loại phòng: 6 người             │
│ Khoa CNTT                    Diện ưu tiên: Hộ nghèo          │
│                              [Xác nhận đúng] [Không đúng]    │
│ Điểm ưu tiên: 85             Ghi chú: ...                    │
├──────────────────────────────────────────────────────────────┤
│                                       [Từ chối]  [Duyệt]     │
└──────────────────────────────────────────────────────────────┘
```

- [Từ chối] mở dialog bắt buộc lý do.
- Chưa hiện danh sách giường ở bước này.

## 14.2. Đơn APPROVED: chọn giường (split view)

Không dùng một dropdown khổng lồ để chọn giường. Danh sách gợi ý **gom theo phòng**, chỉ gồm phòng Hoạt động, cùng đơn vị, đúng giới tính, còn giường trống (UC-16).

```text
┌──────────────────────────────────────────────────────────────┐
│ ĐƠN #DK-2026-0012                          Đã duyệt          │
├───────────────────────┬──────────────────────────────────────┤
│ SINH VIÊN             │ CHỌN GIƯỜNG    [Tòa ▼] [Loại phòng ▼]│
│                       │                                      │
│ Nguyễn Văn A          │ K1-A101 · 6 người · Nam              │
│ MSSV: 20230001        │ 1.500.000 đ/HK · còn 2/6 giường      │
│ Nam · Khoa CNTT       │   (●) Giường 03   ( ) Giường 04      │
│                       │                                      │
│ Nguyện vọng:          │ K1-A102 · 6 người · Nam              │
│ K1 · 6 người          │ 1.500.000 đ/HK · còn 1/6 giường      │
│                       │   ( ) Giường 04                      │
│ Ưu tiên: 85           │                                      │
├───────────────────────┴──────────────────────────────────────┤
│                         [Phân giường K1-A101-03]             │
└──────────────────────────────────────────────────────────────┘
```

- Phòng khớp nguyện vọng loại phòng xếp lên đầu.
- Giường đang giữ chỗ hoặc Hỏng không xuất hiện trong danh sách.
- Bấm [Phân giường] mở Confirm Dialog: "Giữ giường K1-A101-03 cho Nguyễn Văn A. Sinh viên có 7 ngày để ký hợp đồng và nộp cọc 1.000.000 đ."
- Thất bại do tranh chấp (409): giữ nguyên trang, tải lại danh sách giường và báo

> Giường K1-A101-03 vừa được phân cho sinh viên khác. Vui lòng chọn giường khác.

## 14.3. Đơn ASSIGNED: đang giữ chỗ

```text
┌──────────────────────────────────────────────────────────────┐
│ ĐƠN #DK-2026-0012                          Đã giữ chỗ        │
├──────────────────────────────────────────────────────────────┤
│ Giường giữ: K1-A101-03 · 1.500.000 đ/HK                      │
│ Hợp đồng: HD-2026-0012 · Chờ kích hoạt                       │
│ Hạn kích hoạt: 15/10/2026 (còn 5 ngày)                       │
├──────────────────────────────────────────────────────────────┤
│ [Hủy giữ chỗ để phân lại]          [Mở hợp đồng để kích hoạt]│
└──────────────────────────────────────────────────────────────┘
```

- [Hủy giữ chỗ để phân lại] bắt buộc lý do. Đơn quay về Đã duyệt, giường được trả lại, hợp đồng chuyển Đã hủy (FR-REG-11).

---

# 15. Staff - Phòng & Giường

Có 2 chế độ:

```text
[▦ Card] [☷ Table]
```

### Card View

```text
┌─────────────────────────┐
│ K1-A101                 │
│ Phòng 6 người           │
├─────────────────────────┤
│ ● ● ● ◐ ○ ○             │
│ 3 đang ở · 1 đang giữ · 2 trống │
├─────────────────────────┤
│ Trưởng phòng: Nguyễn A  │
│                         │
│ [Chi tiết]              │
└─────────────────────────┘
```

### Room Detail

```text
K1-A101

Hoạt động · 6 người · Nam

GIƯỜNG

[01] Nguyễn A       Đang ở
[02] Nguyễn B       Đang ở
[03] Trần C         Đang ở
[04] Lê D           Đang giữ · hạn 15/10
[05] —              Trống
[06] —              Hỏng (vòi sen gãy)

TRƯỞNG PHÒNG
Nguyễn A

[Đổi trưởng phòng]
```

Dialog chỉ định trưởng phòng (FR-ROOM-09, BR-11):

```text
┌──────────────────────────────────────┐
│ Chỉ định trưởng phòng K1-A101        │
├──────────────────────────────────────┤
│ (●) Nguyễn A        Đang ở           │
│ ( ) Nguyễn B        Đang ở           │
│ ( ) Trần C          Đang ở           │
│ ( ) Lê D            Đang giữ  (mờ)   │
│     Chỉ sinh viên đang ở mới được    │
│     chỉ định                         │
├──────────────────────────────────────┤
│                  [Hủy]  [Xác nhận]   │
└──────────────────────────────────────┘
```

- Phòng còn người ở nhưng chưa có trưởng phòng: banner cảnh báo ở đầu chi tiết phòng và trên card phòng.
- Lịch sử đổi trưởng phòng xem ở tab "Lịch sử" của phòng.

---

# 16. Staff - Hợp đồng

Trang danh sách dùng Table.

Chi tiết dùng layout:

```text
┌─────────────────────────────────────────────┐
│ HD-2026-0012              Đang hiệu lực      │
├─────────────────────────────────────────────┤
│ SINH VIÊN                                   │
│ Nguyễn Văn A                                │
│                                             │
│ PHÒNG                                       │
│ K1-A101 · Giường 03                         │
│                                             │
│ THỜI HẠN                                    │
│ 01/09/2026 → 30/06/2027                    │
│                                             │
│ TIỀN CỌC                                    │
│ Mức: 1.000.000 đ                            │
│ Số dư: 800.000 đ                             │
│ Cần bù: 200.000 đ                            │
├─────────────────────────────────────────────┤
│ [Trả phòng]                                 │
└─────────────────────────────────────────────┘
```

Các action phụ thuộc trạng thái hợp đồng:

| Trạng thái | Action của Staff |
|---|---|
| PENDING (chờ kích hoạt) | [Kích hoạt], [Hủy] |
| ACTIVE | [Trả phòng]. Gia hạn và chuyển phòng do sinh viên gửi yêu cầu, Staff duyệt ở hộp yêu cầu |
| EXPIRED / TERMINATED / CANCELLED | Chỉ xem |

Chi tiết hợp đồng (cả Staff và sinh viên) có tab **Lịch sử phân phòng** (FR-CON-05):

```text
Giường        Từ          Đến         Đơn giá/HK    Loại
K1-A101-03    01/09/2026  15/01/2027  1.500.000 đ   Đang ở
K1-B204-01    15/01/2027  —           1.800.000 đ   Đang ở (hiện tại)
```

Dòng "Giữ chỗ" chuyển thành "Đang ở" trên cùng dòng khi kích hoạt, nên không xuất hiện thành dòng riêng.

## 16.1. Kích hoạt hợp đồng (UC-32)

Hợp đồng PENDING mở ra form kích hoạt. Mức cọc lấy theo tòa và **không sửa được**.

```text
┌─────────────────────────────────────────────┐
│ HD-2026-0012                 Chờ kích hoạt  │
│ Hạn kích hoạt: 15/10/2026 (còn 5 ngày)      │
├─────────────────────────────────────────────┤
│ Sinh viên   Nguyễn Văn A · 20230001         │
│ Giường giữ  K1-A101-03                      │
│ Đơn giá     1.500.000 đ / học kỳ            │
│ Thời hạn    01/09/2026 → 30/06/2027         │
├─────────────────────────────────────────────┤
│ THU CỌC                                     │
│ Mức cọc tòa K1         1.000.000 đ          │
│ ☐ Sinh viên đã ký hợp đồng giấy             │
│ ☐ Đã thu đủ 1.000.000 đ tiền cọc            │
├─────────────────────────────────────────────┤
│ [Hủy hợp đồng]   [Xác nhận thu cọc & kích hoạt]│
└─────────────────────────────────────────────┘
```

- Nút kích hoạt chỉ bật khi cả hai ô đã được tick.
- Confirm Dialog: "Ghi nhận thu cọc 1.000.000 đ và kích hoạt hợp đồng. Không thể hoàn tác."
- Sau khi kích hoạt: hợp đồng chuyển Đang hiệu lực, sổ cọc có dòng "+1.000.000 đ Thu cọc", giường chuyển từ Đang giữ sang Đang ở.
- Hợp đồng vừa bị tự hủy vì quá hạn (409): báo "Hợp đồng đã tự hủy do quá hạn kích hoạt ngày 15/10. Cần phân giường lại từ đơn."

---

# 17. Staff - Trả phòng

Dùng wizard 3 bước.

```text
① Kiểm tra tài sản
      ↓
② Công nợ / Tiền cọc
      ↓
③ Xác nhận trả phòng
```

Ví dụ:

```text
BƯỚC 2 / 3

CÔNG NỢ & TIỀN CỌC

Phí phòng còn nợ                 0 đ
Điện nước phòng còn nợ           0 đ   (nợ của phòng, chỉ cảnh báo)

TIỀN CỌC
Mức cọc                  1.000.000 đ
Đã trừ sửa chữa           −200.000 đ
Số dư cọc                  800.000 đ
Bù cọc chưa nộp            200.000 đ   → tất toán bằng số dư

Hoàn cọc                   800.000 đ
Công nợ sau trả phòng            0 đ

[Quay lại] [Tiếp tục]
```

Quy tắc hiển thị (SRS v0.4, BR-10):

- Hoàn cọc luôn bằng số dư cọc nếu dương. Khoản bù cọc chưa nộp **không** thành công nợ.
- Số dư âm (ví dụ bị trừ 1.200.000 đ trên cọc 1.000.000 đ): Hoàn cọc 0 đ, dòng "Công nợ sau trả phòng 200.000 đ" tô cảnh báo.
- Phí phòng chưa đóng không chặn trả phòng, chỉ hiện cảnh báo và tiếp tục được theo dõi.

---

# 18. Staff - Điện nước

Màn hình nhập chỉ số, tách tab điện và nước:

```text
KỲ ĐIỆN NƯỚC: 10/2026          [Điện] [Nước]       Tòa [K1 ▼] Tầng [1 ▼]

Phòng      Đầu kỳ   Cuối kỳ   Tiêu thụ
K1-A101    1200     [1350]    150 kWh   ✓
K1-A102    980      [ 950]    —         ✗ Chỉ số cuối nhỏ hơn đầu kỳ (980)
K1-A103    1105     [    ]    —         Chưa nhập

Đã nhập: 41/44 phòng có người ở
```

- Chỉ số đầu kỳ tự lấy từ cuối kỳ trước, chỉ đọc.
- Validate ngay khi rời ô: cuối < đầu thì ô đỏ kèm lời giải thích; không lưu được dòng đó.
- Enter / Tab chuyển xuống phòng kế tiếp để nhập liên tục; lưu tự động theo từng dòng.
- Nút [Ghi nhận thay đồng hồ] ở từng phòng (FR-BIL-03): chốt chỉ số cũ, nhập chỉ số mới bắt đầu, lý do.
- Còn phòng có người ở chưa nhập đủ điện **và** nước: bước xem trước cảnh báo "N phòng thiếu chỉ số sẽ **không** được phát hành đợt này" kèm danh sách phòng; các phòng đủ chỉ số vẫn phát hành. Nhập đủ rồi bấm phát hành lại, hệ thống chỉ tạo hóa đơn cho phòng còn thiếu (UC-21, 1a).

Sau đó Staff có thể phát hành hóa đơn theo phòng. Bước xem trước khi phát hành cảnh báo phòng chưa có trưởng phòng (UC-21, 3a):

```text
⚠ 3 phòng chưa có trưởng phòng: K1-A103, K1-A207, K1-B110
  Hóa đơn vẫn được phát hành; thông báo gửi tới mọi thành viên.
  [Chỉ định trưởng phòng]
```

---

# 19. Staff - Hóa đơn & Công nợ

Nên có Tabs:

```text
[Phí phòng] [Điện nước] [Công nợ]
```

Status:

```text
CHƯA THANH TOÁN
QUÁ HẠN
ĐÃ THANH TOÁN
```

Hóa đơn đã thanh toán không cho chỉnh sửa trực tiếp.

Nếu cần điều chỉnh, dùng dòng **Điều chỉnh** ở hóa đơn kỳ kế tiếp và bắt buộc có lý do.

## 19.1. Phát hành hóa đơn phí phòng theo học kỳ (UC-21)

```text
PHÁT HÀNH PHÍ PHÒNG

Học kỳ   [ HK1 2026-2027 ▼ ]      Đơn vị: K1

XEM TRƯỚC
Sinh viên đang ở trong học kỳ       412
Đã có hóa đơn học kỳ này (bỏ qua)    5
Sẽ phát hành                        407
Tổng tiền                   610.500.000 đ
Hạn đóng                     15/10/2026  (hôm nay + 7 ngày)

[Xem danh sách chi tiết]

                       [Hủy]  [Phát hành 407 hóa đơn]
```

- Học kỳ hè: chỉ gồm sinh viên đã được duyệt "Ở hè".
- Đơn giá lấy từ phân phòng hiệu lực tại ngày phát hành; tính đủ cả học kỳ.
- Phát hành chạy trong một transaction; có loading state vì có thể mất vài giây (NFR-PER-02 ≤ 30 giây).

## 19.2. Xác nhận thanh toán (FR-BIL-08)

```text
┌──────────────────────────────────────┐
│ Xác nhận thanh toán                  │
├──────────────────────────────────────┤
│ Điện nước K1-A101 · 10/2026          │
│ Số tiền          630.000 đ           │
│                                      │
│ Người nộp *  [ Nguyễn Văn B (TP) ▼ ] │
│ Hình thức *  ( ) Tiền mặt            │
│              (●) Chuyển khoản        │
│ Ghi chú      [...................]   │
│                                      │
│ Sau khi xác nhận, hóa đơn bị khóa.   │
├──────────────────────────────────────┤
│              [Hủy]   [Xác nhận]      │
└──────────────────────────────────────┘
```

Với hóa đơn điện nước, "Người nộp" mặc định là trưởng phòng.

## 19.3. Cấu hình đơn giá điện nước (FR-BIL-01)

Bảng đơn giá điện (đ/kWh) và nước (đ/m³) có **ngày hiệu lực**. Thêm đơn giá mới thay vì sửa đơn giá cũ; hóa đơn đã phát hành giữ đơn giá đã chụp.

## 19.4. Công nợ

Tab Công nợ gồm:

- Theo sinh viên: phí phòng chưa đóng; bù cọc chưa nộp (chỉ khi hợp đồng còn hiệu lực); số dư cọc âm sau trả phòng.
- Theo phòng: điện nước chưa đóng.
- Sinh viên **đã trả phòng** vẫn xuất hiện nếu còn công nợ, có nhãn "Đã trả phòng".

Phía sinh viên: người đã trả phòng vẫn đăng nhập được, trang chủ chỉ còn card "Công nợ còn lại" và lịch sử (FR-CON-10).

---

# 20. Staff - Tiền cọc

Nên dùng Ledger UI thay vì chỉ hiển thị một số dư.

```text
TIỀN CỌC

Sinh viên: Nguyễn Văn A
Hợp đồng: HD-2026-0012

Mức cọc       1.000.000 đ
Số dư           800.000 đ
Cần bù          200.000 đ

LỊCH SỬ

01/09  +1.000.000  Thu cọc
12/10    -200.000  Trừ cọc sửa chữa

────────────────────────
Số dư: 800.000 đ

⚠ Quá hạn bù cọc
Hạn: 19/10/2026

[Xác nhận nộp bù cọc]
```

[Xác nhận nộp bù cọc] mở dialog: số tiền điền sẵn bằng số cần bù (có thể nộp một phần), người xác nhận, thời gian. Sau khi xác nhận, sổ cọc thêm dòng "+200.000 đ Bù cọc".

Danh sách "Cần nhắc bù cọc" (FR-BIL-13) liệt kê các khoản quá 7 ngày, có nút [Gửi nhắc nhở].

---

# 21. Staff - Maintenance

Nên thiết kế giống hệ thống ticket.

```text
SỬA CHỮA

[Chờ xử lý] [Đã giao KTV] [Đang sửa] [Chờ nghiệm thu] [Đã đóng] [Bị từ chối] [Đã hủy]

┌─────────────────────────────────────────────┐
│ #TK-1024                                    │
│ Vòi nước bị rò                               │
│ K1-A101                                     │
│ Nguyễn Văn A                                │
│                                             │
│ Đang sửa                                    │
│ KTV: Trần C                                  │
│                                             │
│ [Xem chi tiết]                              │
└─────────────────────────────────────────────┘
```

Chi tiết có Timeline:

```text
● Chờ xử lý
  Nguyễn A · 08/10 09:20

● Đã giao KTV
  Staff B → KTV C · 08/10 10:00

● Đang sửa
  KTV C · 08/10 14:30

○ Chờ nghiệm thu
○ Đã đóng
```

Action của Staff theo trạng thái:

| Trạng thái | Action |
|---|---|
| Chờ xử lý | [Giao KTV] (chỉ KTV cùng đơn vị), [Từ chối] (bắt buộc lý do) |
| Chờ nghiệm thu, người báo đã trả phòng | [Nghiệm thu thay] (bắt buộc lý do, FR-MNT-07) |
| Ticket khu vực chung, Chờ nghiệm thu | [Chấp nhận] / [Chưa đạt] (Staff là người nghiệm thu) |
| Đã đóng, ticket của phòng, chưa kết luận | [Kết luận chi phí] |

### Kết luận chi phí (FR-MNT-09/10, UC-22)

Ticket **của phòng** vừa chuyển "Đã đóng" xuất hiện trong danh sách "Cần kết luận chi phí". Panel:

```text
┌─────────────────────────────────────────────┐
│ KẾT LUẬN CHI PHÍ · #TK-1024 · K1-A101       │
├─────────────────────────────────────────────┤
│ Nguyên nhân *                               │
│ ( ) Hao mòn tự nhiên — BQL chịu chi phí     │
│ (●) Hư hỏng do sử dụng — trừ cọc            │
│                                             │
│ Chi phí sửa chữa *   [   200.000 ] đ        │
│ Ghi chú              [ ...................] │
├─────────────────────────────────────────────┤
│ Người chịu trừ cọc (ở phòng lúc báo hỏng,   │
│ 08/10/2026), chia đều cho người được chọn:  │
│ [✓] Nguyễn A     −66.666 đ                  │
│ [✓] Nguyễn B     −66.666 đ                  │
│ [ ] Trần C        0 đ                       │
│ [✓] Lê D         −66.668 đ                  │
│ Tổng        −200.000 đ ✓                    │
│ Mỗi người phải bù cọc, hạn 7 ngày           │
├─────────────────────────────────────────────┤
│                      [Hủy]  [Xác nhận]      │
└─────────────────────────────────────────────┘
```

- Hao mòn tự nhiên: bắt buộc ghi chú, ẩn ô chi phí, không trừ cọc.
- Danh sách ứng viên là người **đang ở tại thời điểm tạo ticket**, không phải hiện tại (API `chargeable-students`). Mặc định chọn hết; Staff bỏ chọn người không liên quan thì ô Ghi chú thành bắt buộc (ghi lý do). Phải chọn ít nhất một người. Tiền chia đều cho người được chọn, phần lẻ cộng vào người cuối để tổng khớp.
- Xác nhận mở Confirm Dialog vì không thể hoàn tác; sau đó mỗi sinh viên nhận thông báo "Bị trừ cọc".
- Ticket khu vực chung không có panel này.

---

# 22. Technician - Việc của tôi

Thiết kế mobile-first.

```text
Việc của tôi

┌───────────────────────────┐
│ 🔧 Vòi nước bị rò         │
│ K1-A101 · Mức độ: Cao     │
│ Đã giao KTV               │
│                           │
│ [Bắt đầu sửa]             │
└───────────────────────────┘

┌───────────────────────────┐
│ 🔧 Bóng đèn hỏng          │
│ K1-A205                   │
│ Đang sửa                  │
│                           │
│ [Ghi tiến độ] [Đã sửa xong]│
└───────────────────────────┘
```

KTV **chỉ thấy ticket được giao cho mình** (FR-MNT-04). Tab:

```text
[Mới giao] [Đang sửa] [Đã xong]
```

- "Mới giao" = ticket Đã giao KTV; "Đã xong" = Chờ nghiệm thu và Đã đóng (chỉ xem).
- KTV không đóng ticket; [Đã sửa xong] chuyển sang Chờ nghiệm thu và yêu cầu ghi chú kết quả.
- Ticket bị người báo trả về "Chưa đạt" quay lại tab Đang sửa, hiện lý do ở đầu thẻ.

Ticket detail ưu tiên:

- Mô tả, mức độ
- Phòng / vị trí
- Hình ảnh
- Lịch sử
- Nút hành động lớn, full-width

---

# 22A. Yêu cầu gia hạn và chuyển phòng

Gia hạn và chuyển phòng là **yêu cầu do sinh viên gửi, Staff duyệt** (FR-CON-06, FR-CON-07), không phải nút Staff bấm trực tiếp.

## Phía sinh viên

Trong chi tiết hợp đồng Đang hiệu lực:

```text
[Yêu cầu gia hạn]   [Yêu cầu chuyển phòng]
```

- Gia hạn: chọn ngày kết thúc mới, lý do.
- Chuyển phòng: lý do, nguyện vọng (tùy chọn). Không chọn giường; Staff chọn.
- Mỗi loại tối đa một yêu cầu đang chờ; yêu cầu hiển thị trạng thái Chờ duyệt / Đã duyệt / Bị từ chối kèm lý do.

## Phía Staff: Hộp "Yêu cầu chờ xử lý"

Một danh sách gom mọi thứ Staff cần quyết định, lọc theo loại:

```text
YÊU CẦU CHỜ XỬ LÝ                 [Tất cả ▼]

Loại            Sinh viên       Gửi lúc     
Đơn đăng ký     Nguyễn A        08/10 09:32   Xem →
Gia hạn         Trần B          07/10 14:10   Xem →
Chuyển phòng    Lê C            07/10 08:00   Xem →
Lựa chọn hè     Phạm D          06/10 20:15   Xem →
Bù cọc          Hoàng E         (đã nộp)      Xác nhận →
```

Duyệt chuyển phòng dùng lại **split view** của màn phân giường (mục 14.2), chỉ hiện giường trống **cùng đơn vị** với giường hiện tại. Panel trái hiện giường hiện tại và lý do. Thất bại do tranh chấp: thông báo giống mục 14.2.

Duyệt gia hạn bị từ chối nếu khoảng mới chồng hợp đồng khác; thông báo nói rõ hợp đồng nào bị chồng.

---

# 22B. Kỳ hè (FR-REG-13..15, FR-CON-11..13)

## Sinh viên chọn lựa chọn cho hè

Khi Staff mở đợt đăng ký hè, sinh viên đang ở nhận thông báo và trang chủ có card "Chọn phương án kỳ hè".

```text
KỲ HÈ 2027 · Hạn chọn: 30/05/2027

┌──────────────────────────────┐
│ ☀ Ở hè                       │
│ Tiếp tục ở, đóng phí hè      │
│ 900.000 đ                    │
│                    [Chọn]    │
└──────────────────────────────┘
┌──────────────────────────────┐
│ 📦 Tạm rời hè                │
│ Gửi đồ vào kho, giữ cọc,     │
│ quay lại đầu HK1 không cần   │
│ nộp đơn mới                  │
│                    [Chọn]    │
└──────────────────────────────┘
┌──────────────────────────────┐
│ 🚪 Rời KTX                   │
│ Làm thủ tục trả phòng,       │
│ hoàn cọc theo số dư          │
│                    [Chọn]    │
└──────────────────────────────┘
```

- "Tạm rời hè" bị khóa nếu hợp đồng chưa được gia hạn sang năm học kế tiếp; card hiện lý do và nút [Yêu cầu gia hạn].
- Sau khi chọn: trạng thái Chờ duyệt, đổi được cho tới khi Staff duyệt.

## Staff duyệt và ghi phiếu gửi đồ

Danh sách lựa chọn hè có tab theo phương án. Với "Tạm rời hè", duyệt mở form **phiếu gửi đồ**:

```text
PHIẾU GỬI ĐỒ · Nguyễn Văn A · K1-A101-03

Số kiện *       [ 3 ]
Mô tả *         [ 2 vali, 1 thùng sách ]
Vị trí kho *    [ Kho K1 - Kệ B2 ]
Ngày gửi *      [ 20/06/2027 ]

Sau khi lưu: giường K1-A101-03 được trả để dùng trong hè,
hợp đồng giữ nguyên, nhãn "Tạm rời hè".

                       [Hủy]  [Lưu phiếu]
```

Duyệt "Ở hè": gia hạn hợp đồng qua kỳ hè; có thể dùng chuyển phòng để gom phòng ở hè. Duyệt "Rời KTX": chuyển sang luồng trả phòng (mục 17).

## Staff phân lại giường đầu HK1

```text
SINH VIÊN TẠM RỜI HÈ · HK1 2027-2028          (38)

Sinh viên    Giường cũ     Gợi ý          Kho
Nguyễn A     K1-A101-03    ✓ Còn trống    Kệ B2   [Phân lại]
Trần B       K1-A102-01    ✗ Đã có người  Kệ B3   [Chọn giường khác]
```

- [Phân lại] tạo phân phòng "Đang ở" ngay (không qua giữ chỗ), ghi ngày nhận lại đồ trên phiếu gửi đồ.
- Không cần đơn mới; nhãn "Tạm rời hè" tự mất khi phân lại.

---

# 22C. Đăng nhập và đổi mật khẩu

- Lỗi đăng nhập không nói rõ sai tên hay sai mật khẩu. Sau 5 lần sai: "Tài khoản tạm khóa 15 phút".
- Tài khoản có cờ "bắt buộc đổi mật khẩu" (tất cả sinh viên mới import) được chuyển thẳng tới màn **Đổi mật khẩu lần đầu**, không vào được trang khác cho tới khi đổi xong (FR-ACC-03).
- Phiên hết hạn sau 30 phút không hoạt động: về /login với thông báo "Phiên đã hết hạn" và quay lại đúng trang cũ sau khi đăng nhập.

---

# 23. Admin

Admin không cần dashboard phức tạp.

Ưu tiên:

**Data management + validation + auditability**

Phạm vi Admin theo SRS (Admin **không** mặc nhiên có quyền nghiệp vụ, BR-07):

| Màn hình | Nội dung |
|---|---|
| Tài khoản | Tạo tài khoản Staff, KTV, Admin; gán vai trò và đơn vị; khóa / mở khóa; đặt lại mật khẩu mọi tài khoản |
| Import sinh viên | Mục dưới đây |
| Đơn vị & Tòa nhà | Danh mục đơn vị quản lý, tòa thuộc đơn vị, **mức cọc theo tòa** (đổi mức chỉ áp dụng cho hợp đồng kích hoạt sau đó) |
| Quyền tổng hợp liên đơn vị | Cấp / thu hồi cho từng Staff (FR-ACC-08). Không có màn sửa vai trò, vì 4 vai trò là cố định |
| Nhật ký hệ thống | Lọc theo người, loại thao tác, thời gian; tác vụ tự động có người thao tác "SYSTEM" |

Loại phòng và Kỳ học do **Staff** quản lý (FR-ROOM-02, FR-ROOM-11), không nằm ở Admin.

## Import sinh viên

```text
IMPORT SINH VIÊN

┌─────────────────────────────────────┐
│                                     │
│        Kéo file Excel vào đây       │
│              hoặc                   │
│          [Chọn file]                │
│                                     │
└─────────────────────────────────────┘

File: students.xlsx

[Kiểm tra dữ liệu]
```

Kết quả:

```text
Tổng dòng: 1500
Hợp lệ: 1487
Lỗi: 10
Đã tồn tại: 3

Dòng 23
Mã SV: 20230023
❌ Số điện thoại không hợp lệ

[Chỉ import dòng hợp lệ]
```

Triển khai tại `/admin/import` gồm tab **Import Excel** và **Hồ sơ sinh viên**. Dùng FormControls/Surface/DataTable và ConfirmDialog chung; kiểm tra quyền `STUDENT_CREATE` cho chọn file, tải mẫu và ghi. Kiểm tra lại sau khi đổi file; chỉ hiện nút ghi khi preview có dòng hợp lệ. Lỗi dòng hiển thị số dòng Excel, mã SV, tên cột và lý do tiếng Việt; mã đã tồn tại được bỏ qua.

Preview có dòng hợp lệ hiển thị thông báo đã đọc được dữ liệu và hướng dẫn bước xác nhận. Bảng chỉ liệt kê lỗi/mã đã tồn tại theo API; không dựng bảng rỗng khiến người dùng tưởng file không có dữ liệu. File thiếu cột hiển thị tên cột từ `details.missingColumns`; file không có dòng hợp lệ hiện cảnh báo và không có nút ghi. Sau lỗi ghi, đóng xác nhận và xóa preview cũ; người dùng phải chủ động kiểm tra lại trước khi gửi lần tiếp theo.

Sau khi ghi thành công, hiển thị tổng số đã tạo và nút tải tài khoản vừa tạo. Không hiển thị mật khẩu trực tiếp trên bảng, không lưu vào query cache/localStorage/sessionStorage; người quản trị tải và bàn giao một lần. Tài khoản mới bắt đổi mật khẩu qua Keycloak. Mất bản tải được xử lý bằng reset có quyền.

Đổi qua lại giữa tab Import Excel và Hồ sơ sinh viên giữ trạng thái import trong bộ nhớ của màn. Khi còn tài khoản chưa tải, khóa chọn/xóa file để tránh làm mất kết quả; sau khi tải mới cho chọn lô tiếp theo. Rời màn hoặc tải lại trang vẫn kết thúc bộ nhớ tạm, nên phải tải và bàn giao tài khoản trước khi rời màn. Hướng dẫn nhắc dùng Text cho mã dài hơn 15 chữ số, mã có số 0 đầu và điện thoại.

Màn nhật ký `/admin/audit-logs` giữ bộ lọc hiện tại; mở chi tiết tải dữ liệu trước/sau, lý do và requestId. Người thực hiện trống trong CSDL hiển thị **SYSTEM**; chỉ có quyền xem, không có sửa/xóa. Staff sửa hồ sơ sinh viên không có trường mã SV/họ tên/giới tính; backend vẫn từ chối nếu tự gửi các trường này để thay đổi danh tính.

---

# 24. Thông báo

Notification Center dùng chung cho tất cả role.

Nguồn thông báo:

- Đơn được duyệt / từ chối
- Được giữ chỗ
- Hợp đồng sắp hết hạn
- Hóa đơn mới
- Bị trừ cọc
- Ticket thay đổi trạng thái
- Thông báo do Staff đăng

UI:

```text
THÔNG BÁO

● Hóa đơn mới
  Phí phòng HK1 đã được phát hành.
  10 phút trước

● Ticket đã được giao
  Ticket #TK-1024 đã được giao cho KTV.
  2 giờ trước

○ Thông báo KTX
  Lịch bảo trì hệ thống nước...
  Hôm qua
```

Có trạng thái:

```text
[Chưa đọc] [Đã đọc]
```

---

# 25. Responsive Strategy

SRS yêu cầu responsive từ **360px**.

## Breakpoints đề xuất

```text
360–639     Mobile
640–1023    Tablet
1024+       Desktop
```

### Student

Mobile-first.

### Technician

Mobile-first.

### Staff

Desktop-first nhưng vẫn responsive.

### Admin

Desktop-first.

Không chỉ thu nhỏ desktop UI trên mobile.

Ví dụ Table trên mobile nên chuyển thành Card:

Desktop:

```text
| MSSV | Họ tên | Status | Action |
```

Mobile:

```text
┌─────────────────────┐
│ 20230001            │
│ Nguyễn Văn A        │
│ Đã gửi              │
│                     │
│ [Xem chi tiết]      │
└─────────────────────┘
```

---

# 25A. Accessibility

Mục tiêu: dùng được bằng điện thoại 360 px, bằng bàn phím, và với người nhìn kém.

| Hạng mục | Quy tắc |
|---|---|
| Tương phản | Chữ thường ≥ 4.5:1, chữ lớn và icon ≥ 3:1. Kiểm tra badge cam và vàng trên nền trắng |
| Vùng chạm | ≥ 44 × 44 px trên mobile; nút hành động chính của KTV full-width |
| Bàn phím | Mọi thao tác làm được bằng Tab / Enter / Space; focus ring rõ (2 px, màu primary) |
| Dialog | Trap focus, đóng bằng Esc, focus trả về nút đã mở dialog |
| Form | Mỗi input có `<label>`; trường bắt buộc có dấu * và chữ "bắt buộc" cho trình đọc màn hình; lỗi gắn với ô bằng `aria-describedby` |
| Thông báo | Toast và lỗi submit dùng `aria-live="polite"`; lỗi chặn thao tác dùng `role="alert"` |
| Trạng thái | Không dùng màu đơn độc (Rule 03); icon có `aria-label` |
| Bảng | Có `<th scope>`; trên mobile chuyển thành card (mục 25) |
| Ảnh | Ảnh báo hỏng có alt mô tả ngắn hoặc "Ảnh sự cố 1" |
| Chữ dài | Mã phòng + badge + số tiền phải xuống dòng gọn ở 360 px, không tràn ngang |

Kiểm tra: Lighthouse Accessibility ≥ 90 cho các màn P0; thử điều hướng bằng bàn phím cho luồng đăng ký và phân giường.

---

# 26. Route Structure

```text
/login
/change-password

/student
/student/room
/student/applications
/student/applications/:id
/student/contracts
/student/contracts/:id
/student/requests
/student/invoices
/student/deposit
/student/maintenance
/student/maintenance/:id
/student/summer
/student/notifications
/student/profile

/staff                      (redirect → /staff/dashboard)
/staff/dashboard
/staff/requests
/staff/applications
/staff/applications/:id
/staff/rooms
/staff/rooms/:id
/staff/students
/staff/contracts
/staff/contracts/:id        (kích hoạt hợp đồng là modal trên trang này)
/staff/summer
/staff/storage
/staff/utilities
/staff/utilities/prices
/staff/invoices
/staff/invoices/issue
/staff/deposits
/staff/maintenance
/staff/notifications
/staff/room-types
/staff/semesters
/staff/registration-periods

/technician
/technician/tasks
/technician/tasks/:id

/admin
/admin/users
/admin/units
/admin/buildings
/admin/import
/admin/aggregate-permissions
/admin/audit-logs
```

---

# 27. Frontend Component Architecture

```text
src/
├── app/
│   ├── router/
│   ├── providers/
│   └── layouts/
│       ├── StudentLayout
│       ├── StaffLayout
│       ├── TechnicianLayout
│       └── AdminLayout
│
├── components/
│   ├── ui/
│   └── business/
│
├── features/
│   ├── auth/
│   ├── students/
│   ├── rooms/
│   ├── registrations/
│   ├── contracts/
│   ├── billing/
│   ├── maintenance/
│   ├── notifications/
│   └── admin/
│
├── pages/
│   ├── student/
│   ├── staff/
│   ├── technician/
│   └── admin/
│
├── services/
│   └── api/
│
├── hooks/
├── types/
├── constants/
│   └── statusConfig.ts      (nhãn, màu, icon theo entity + status)
├── utils/                   (format tiền, ngày; map errorCode → câu tiếng Việt)
└── styles/                  (theme từ design tokens mục 2.1a)
```

Business components nên được tái sử dụng giữa các role khi cùng biểu diễn một nghiệp vụ.

Ví dụ:

```text
RoomCard
BedCard
StatusBadge
ContractTimeline
MaintenanceTimeline
DepositLedger
InvoiceCard
NotificationItem
```

## 27.1. Thư viện và stack

| Hạng mục | Chọn | Lý do |
|---|---|---|
| UI library | **Ant Design** | Có sẵn Steps, Timeline, Table, Descriptions, Badge, Tag, Modal.confirm, Form validate; không phải tự viết các component ở mục 5.1. Theme lấy từ design tokens (ConfigProvider) |
| Routing | React Router | Guard theo role; route ngoài quyền chuyển về trang chủ của role |
| Gọi API, cache | TanStack Query | Loading / error state thống nhất; invalidate sau mỗi action; refetch khi gặp 409 |
| Form | React Hook Form + Zod | Validate phía client cùng schema; lỗi gắn đúng ô (mục 25A) |
| HTTP | Axios, `withCredentials` | Cookie phiên HttpOnly; interceptor 401 → /login, quay lại trang cũ |

Phần mobile của sinh viên và KTV dùng cùng Ant Design, chỉnh layout bằng Grid / Flex và bottom nav tự viết; không cần thêm thư viện mobile riêng.

## 27.2. Quy ước với backend

**`allowedActions`**: API chi tiết mỗi đối tượng (đơn, hợp đồng, ticket, hóa đơn, yêu cầu) trả về danh sách hành động người dùng hiện tại được làm ở trạng thái hiện tại:

```json
{
  "id": 12,
  "status": "SUBMITTED",
  "allowedActions": ["approve", "reject"]
}
```

Frontend chỉ hiện nút có trong danh sách (Rule 02). Frontend **không** tự viết lại bảng chuyển trạng thái; backend vẫn kiểm tra lại khi nhận request.

**`errorCode`**: lỗi 400 / 409 trả về mã ổn định kèm dữ liệu cần để dựng câu thông báo:

```json
{ "errorCode": "BED_TAKEN", "message": "...", "details": { "bedCode": "K1-A101-03" } }
```

`utils/errorMessages.ts` dịch mã sang câu tiếng Việt theo Rule 06, ví dụ `BED_TAKEN` → "Giường K1-A101-03 vừa được phân cho sinh viên khác. Vui lòng chọn giường khác." Không parse chuỗi `message`.

## 27.3. Chia màn theo thành viên

Khớp với cách chia module trong tài liệu đề xuất (mục 13.1):

| TV | Màn hình |
|---|---|
| TV1 Nguyễn Văn Séc | Login, đổi mật khẩu, layout + guard theo role, `statusConfig`, theme; Admin (tài khoản, import, đơn vị & tòa, quyền tổng hợp, nhật ký) |
| TV2 Nguyễn Phú Nguyên | Phòng & giường (card, chi tiết), chọn giường (split view, mục 14.2), trưởng phòng, duyệt chuyển phòng, phân lại giường đầu HK1 |
| TV3 Lê Đức Nguyên | Dashboard sinh viên, đăng ký (stepper, timeline), duyệt đơn, hợp đồng, kích hoạt, yêu cầu gia hạn, lựa chọn kỳ hè, hộp yêu cầu chờ xử lý |
| TV4 Nguyễn Văn Quyền | Báo hỏng, ticket (sinh viên, Staff, KTV), kết luận chi phí, trả phòng, phiếu gửi đồ, thông báo |
| TV5 Nguyễn Tiến Lộc | Điện nước, phát hành hóa đơn, xác nhận thanh toán, sổ cọc, công nợ, dashboard Staff |

**Tuần đầu code**, TV1 làm xong layout, guard, theme, `statusConfig` và `errorMessages` để các thành viên khác dùng chung. Component dùng chung (StatusBadge, Timeline, ConfirmDialog có lý do) do người dùng đầu tiên viết và đưa vào `components/`.

---

# 28. 11 Backbone Screens nên thiết kế trước

Không nên thiết kế toàn bộ hệ thống ngay từ đầu.

Ưu tiên 11 màn hình:

1. Login
2. Student Dashboard
3. Student Registration
4. Application Detail / Timeline
5. My Room
6. Contract + Deposit
7. Invoice
8. Maintenance Report
9. Staff Dashboard
10. Staff Review Application → Assign Bed
11. Staff Activate Contract (thu cọc + kích hoạt)

Sau khi 11 màn hình này ổn, mở rộng:

```text
11 Backbone Screens
        ↓
Contracts
        ↓
Billing
        ↓
Maintenance
        ↓
Room Transfer
        ↓
Contract Extension
        ↓
Move-out
        ↓
Summer
        ↓
Admin
```

---

# 29. Figma Structure

Mức độ chi tiết:

- **Wireframe low-fi** cho toàn bộ màn hình, có thể vẽ trực tiếp từ các khung text trong tài liệu này.
- **High-fi** chỉ cho khoảng 6 màn dùng để demo: Student Dashboard, Đăng ký, Staff duyệt và chọn giường, Kích hoạt hợp đồng, Hóa đơn, Kết luận chi phí sửa chữa.
- Prototype chỉ nối luồng demo chính, không cần nối mọi màn.

Đề xuất tổ chức file Figma:

```text
DMS UI/UX
│
├── 00 - Design System
│   ├── Colors
│   ├── Typography
│   ├── Buttons
│   ├── Inputs
│   ├── Cards
│   ├── Tables
│   ├── Badges
│   └── Modals
│
├── 01 - Authentication
│
├── 02 - Student
│   ├── Dashboard
│   ├── Room
│   ├── Registration
│   ├── Contract
│   ├── Invoice
│   ├── Maintenance
│   └── Notification
│
├── 03 - Staff
│   ├── Dashboard
│   ├── Applications
│   ├── Rooms
│   ├── Contracts
│   ├── Billing
│   └── Maintenance
│
├── 04 - Technician
│
├── 05 - Admin
│
└── 06 - Prototype
```

---

# 30. UX Rules cần giữ xuyên suốt

### Rule 01 — State trước Action

Hiển thị trạng thái trước, action phù hợp sau.

### Rule 02 — Action theo trạng thái

Không hiển thị:

```text
[Approve] [Reject] [Cancel] [Assign] [Move-out]
```

nếu không phải action hợp lệ ở trạng thái hiện tại.

### Rule 03 — Không dùng màu đơn độc

Status phải có:

```text
Badge + Text tiếng Việt
```

Nhãn suy ra (Sắp hết hạn, Quá hạn…) dùng chip viền có icon, đặt cạnh badge trạng thái (mục 2.2).

không chỉ:

```text
🟢
```

### Rule 04 — Destructive action phải confirm

Các action như từ chối, hủy, trả phòng, xác nhận thanh toán cần confirmation.

### Rule 05 — Reason bắt buộc nếu nghiệp vụ yêu cầu

Ví dụ từ chối:

```text
Lý do *
```

không được để trống.

### Rule 06 — Error phải actionable

Luôn trả lời:

> Vì sao lỗi? Tôi phải làm gì?

### Rule 07 — Không xóa lịch sử nghiệp vụ

UI phải ưu tiên:

```text
Deactivate / Lock / Close / Status
```

thay vì hard delete đối với dữ liệu đã phát sinh nghiệp vụ.

### Rule 08 — Personal data

Thông tin cá nhân chỉ hiển thị cho đúng phạm vi quyền.

---

# 31. Design Priorities

Nếu thời gian thiết kế có hạn, ưu tiên theo thứ tự:

```text
P0
├── Login
├── Student Dashboard
├── Student Registration
├── Application Review
├── Room / Bed
├── Contract Activation (thu cọc)
├── Contract
└── Invoice

P1
├── Change Password (lần đầu)
├── Maintenance + Kết luận chi phí
├── Deposit
├── Notification
├── Staff Dashboard
├── Requests (gia hạn, chuyển phòng)
├── Move-out
├── Summer + Kho gửi đồ
├── Excel Import
└── Audit Log

P2
├── Biểu đồ dashboard (✂ trong SRS)
└── Thông báo theo phòng / tầng (✂)
```

Mọi màn P0 và P1 đều là **Must** trong SRS. P2 là các mục được cắt (✂) nếu trễ tiến độ ở tuần 7.

---

# 32. Kết luận

DMS không nên được thiết kế như một dashboard duy nhất cho tất cả người dùng.

Nên có 4 trải nghiệm:

```text
Student
→ Mobile-first
→ Friendly
→ Status-oriented

Staff
→ Desktop-first
→ Data-dense
→ Action-oriented

Technician
→ Mobile-first
→ Task-oriented
→ Large touch targets

Admin
→ Desktop-first
→ Data-management
→ Auditability
```

Thiết kế frontend nên bám sát vòng đời nghiệp vụ:

```text
Đăng ký
   ↓
Xét duyệt
   ↓
Phân phòng
   ↓
Ký / Kích hoạt hợp đồng
   ↓
Lưu trú
   ├── Hóa đơn
   ├── Điện nước
   ├── Tiền cọc
   └── Báo hỏng
   ↓
Gia hạn / Chuyển phòng
   ↓
Trả phòng
```

Đây là trục chính để mở rộng wireframe, prototype Figma và sau đó triển khai React.

## Cập nhật cổng Admin và quyền CRUD — 14/10/2026

Admin được làm mọi nghiệp vụ, truy cập các màn quản trị và nghiệp vụ trong cổng `/admin`; dùng lại trang CRUD của Staff và danh sách quyền `permissions` từ `/api/auth/me`. Menu/route kiểm READ, nút thêm/sửa/xóa kiểm đúng mã thao tác, riêng nút khóa/mở khóa/reset có quyền riêng. Admin được sửa/hủy bản ghi của người khác khi trạng thái cho phép. Technician chỉ có nút tiến độ của ticket được giao; Student giữ phạm vi cá nhân. Không tạo trang mới cho từng vai trò nếu dùng chung form CRUD.

## Cập nhật luồng chung — 15/10/2026

Menu Admin ưu tiên nghiệp vụ: Tổng quan/Yêu cầu chờ xử lý → Quản lý (gồm Sinh viên) → Tài chính → Vận hành → Danh mục → **Quản trị hệ thống** ở cuối. Nhóm cuối gồm Tài khoản, Quyền tổng hợp, Phân quyền theo vai trò, Cấu hình và Nhật ký hệ thống. Danh mục gồm Đơn vị, Tòa nhà & mức cọc, Diện ưu tiên, Loại phòng, Kỳ học và Đợt đăng ký; cổng Staff chỉ hiển thị các danh mục được cấp quyền. Khai báo nhóm tập trung tại `constants/navigation.ts` và dùng chung trên sidebar/Drawer.

Menu tài khoản của cả bốn cổng có Hồ sơ của tôi và Đổi mật khẩu/Đăng xuất. Trang chủ Student dùng PROFILE_READ, Technician dùng MAINTENANCE_READ để không bị chặn bởi quyền dashboard quản lý. Hồ sơ Student cho sửa điện thoại, quê quán và liên hệ khẩn cấp; cán bộ chỉ sửa điện thoại.

Trang CRUD dùng Form.Item bọc control trực tiếp để truyền value/onChange. Tìm kiếm, bộ lọc và phân trang được giữ trên URL theo resource; bộ lọc tùy khai báo màn, lỗi 400 hiện tại trường, 409 cập nhật lại dữ liệu. Tài khoản có lọc role/đơn vị/khóa và xác nhận các thay đổi làm thu hồi phiên; quyền đọc tổng hợp chỉ áp dụng Staff. Nhật ký có lọc người thực hiện/mã thao tác/loại đối tượng/từ–đến và chỉ cho đọc, không có nút sửa/xóa. Không mở các luồng nghiệp vụ chi tiết khi mới hoàn thiện khung.


## Màn phân quyền theo vai trò — 15/10/2026

Cổng ADMIN có menu **Phân quyền theo vai trò**, đường dẫn `/admin/role-permissions`; tách với Quyền tổng hợp liên đơn vị của từng cán bộ. Chọn role, tìm màn và tick các cột Xem/Tìm kiếm, Thêm, Sửa, Xóa và thao tác riêng. Catalog/nhóm/nhãn/trạng thái ô do backend trả. ADMIN chỉ xem toàn quyền; ô không phù hợp vai trò và PROFILE_READ bắt buộc bị khóa.

Tick ghi tự tick READ của màn; bỏ READ bỏ các thao tác cùng màn. Chọn tất cả chỉ chọn quyền phù hợp; khôi phục mặc định chỉ đổi bản nháp. Lưu mới ghi CSDL; Hủy trả về dữ liệu đã tải. Đổi role khi có thay đổi cần xác nhận bỏ bản nháp. Trong lúc lưu khóa các nút; gặp version 409 yêu cầu tải lại, không ghi đè âm thầm. /me được làm mới định kỳ/khi focus, sau lưu và sau API bị từ chối; hồ sơ là điểm điều hướng dự phòng khi mất quyền trang chủ.

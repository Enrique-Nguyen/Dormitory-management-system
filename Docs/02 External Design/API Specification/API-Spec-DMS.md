# Đặc tả API — DMS

> **Phiên bản 0.2**, 10/10/2026, Nhóm 3 — Chuyên đề CNTT, 65CNTT
> Căn cứ: `SRS-DMS.md` v0.6 (mục 3, 6.2, 10), `SAD-DMS.md` v1.0, `DB-Design-DMS.md` v0.3, `DMS-UI-UX-Design.md` (mục 27.2)
> Trạng thái: **đã chốt Q-API-01..07** (mục 9) theo thẩm định của nhóm

### Lịch sử thay đổi

| Phiên bản | Ngày | Nội dung |
|---|---|---|
| 0.1 | 10/10/2026 | Bản đầu tiên: quy ước chung, danh mục endpoint theo 7 module, schema cho các nghiệp vụ chính, danh mục `errorCode`, `allowedActions`, định dạng file import |
| 0.2 | 10/10/2026 | Theo thẩm định: Staff tạo ticket của phòng (`roomId`); xem trước trả phòng cảnh báo ticket chưa kết luận chi phí; xem trước điện nước trả phòng thiếu trưởng phòng và thiếu chỉ số; một thông báo `ROOM` cho mỗi hóa đơn điện nước; tổng hóa đơn không âm sau điều chỉnh; quy ước tên hành động không đổi trạng thái; `link` của thông báo do backend suy ra; chốt Q-API-01..07; trừ cọc chia cho người Staff chọn (`chargedStudentIds`, endpoint `chargeable-students`) |

---

## 1. Phạm vi và cách đọc

Tài liệu đặc tả giao diện REST giữa frontend React và backend Spring Boot. Nó là hợp đồng giữa hai phía: frontend chỉ dựa vào những gì ghi ở đây, backend không đổi tên trường, mã lỗi hay hành vi mà không cập nhật tài liệu.

- Mục 2: quy ước áp dụng cho **mọi** endpoint. Mục 3 không nhắc lại.
- Mục 3: danh mục endpoint theo module của SAD mục 4.1. Mỗi dòng ghi vai trò được gọi và FR tương ứng.
- Mục 4: schema request / response chi tiết cho các nghiệp vụ khó hoặc dùng chung.
- Mục 5: danh mục `errorCode`. Mục 6: `allowedActions` theo đối tượng.
- Mục 7: định dạng file Excel import / export. Mục 8: truy vết FR → endpoint.

Ký hiệu vai trò: **S** Student, **St** Staff, **T** Technician, **A** Admin, **\*** mọi người đã đăng nhập.

Sau khi bản này được duyệt, backend sinh tài liệu OpenAPI tự động bằng springdoc (`/v3/api-docs`, chỉ bật ở profile `dev`). Khi hai nguồn lệch nhau, **tài liệu này là chuẩn** và phải sửa code hoặc sửa tài liệu cho khớp.

---

## 2. Quy ước chung

### 2.1. Giao thức, định dạng

| Hạng mục | Quy ước |
|---|---|
| Tiền tố | `/api`. Không đánh số phiên bản trong URL (một client duy nhất) |
| Định dạng | `application/json; charset=utf-8`; upload dùng `multipart/form-data`; file Excel trả `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| Tên trường JSON | `camelCase` (`studentCode`, `depositAmount`). Cột CSDL `snake_case` được map ở DTO |
| Tên tài nguyên trong URL | danh từ số nhiều, `kebab-case` (`/registration-periods`, `/maintenance-requests`) |
| Giá trị liệt kê | Chuỗi in hoa, **trùng giá trị trong CSDL** (`SUBMITTED`, `ROOM_FEE`, `MISUSE`). Nhãn tiếng Việt do frontend dịch (`statusConfig.ts`) |
| Thời điểm | ISO 8601 có múi giờ: `"2026-10-10T08:30:00+07:00"` |
| Ngày | `"2026-10-10"`. Tháng điện nước: `"2026-10"` (`billingMonth`) |
| Tiền | Số nguyên, đơn vị đồng: `1000000`. Không gửi số thập phân, không gửi chuỗi có dấu chấm |
| ID | `id` là số nguyên dùng trong URL. Mã hiển thị (`HD-2026-0012`, `TK-1024`, `HP-2026-0001`) là trường `code`, chỉ để hiển thị và tìm kiếm |
| Trường rỗng | Có trong response với giá trị `null`, không bỏ trường |
| Mã ký tự | UTF-8; tên tiếng Việt có dấu giữ nguyên |

### 2.2. Xác thực, phiên, CSRF

- Đăng nhập tạo phiên phía máy chủ (Spring Session JDBC). Trình duyệt giữ cookie `SESSION` (`HttpOnly`, `Secure`, `SameSite=Lax`). Frontend gọi Axios với `withCredentials: true`; không lưu gì vào `localStorage`.
- **CSRF:** mọi request `POST`, `PUT`, `PATCH`, `DELETE` phải có header `X-XSRF-TOKEN` bằng giá trị cookie `XSRF-TOKEN`. Axios tự làm việc này. Khi mở ứng dụng, frontend gọi `GET /api/auth/csrf` một lần để nhận cookie.
- Phiên hết hạn sau **30 phút** không hoạt động → `401 UNAUTHENTICATED`; interceptor chuyển về `/login` và nhớ trang cũ.
- Tài khoản có cờ `mustChangePassword` chỉ được gọi `GET /api/auth/me`, `POST /api/auth/change-password`, `POST /api/auth/logout`; mọi API khác trả `403 PASSWORD_CHANGE_REQUIRED`.

### 2.3. Mã HTTP

| Mã | Khi nào | Body |
|---|---|---|
| 200 | Đọc, hoặc hành động trả về đối tượng sau khi đổi | Đối tượng |
| 201 | Tạo mới | Đối tượng vừa tạo; header `Location` |
| 204 | Thành công, không có gì để trả | Rỗng |
| 400 | Dữ liệu không hợp lệ (thiếu trường, sai định dạng, vi phạm quy tắc đầu vào) | Lỗi (mục 2.4) |
| 401 | Chưa đăng nhập, phiên hết hạn, sai mật khẩu khi đăng nhập | Lỗi |
| 403 | Đúng là có đối tượng nhưng vai trò không được làm; tài khoản bị khóa; phải đổi mật khẩu | Lỗi |
| 404 | Không tồn tại **hoặc ngoài phạm vi đọc** (Staff đơn vị khác, sinh viên xem đồ của người khác) | Lỗi `NOT_FOUND` |
| 409 | Xung đột trạng thái hoặc ràng buộc (giường đã bị chiếm, hành động không hợp lệ ở trạng thái hiện tại, gửi trùng) | Lỗi |
| 413 | File vượt giới hạn | Lỗi `FILE_TOO_LARGE` |
| 500 | Lỗi không lường trước | Lỗi `INTERNAL_ERROR` |

Quy tắc chọn giữa 403 và 404: nếu người gọi **không được biết** đối tượng tồn tại (ngoài phạm vi) thì 404; nếu đối tượng nằm trong phạm vi nhưng vai trò không có quyền làm hành động đó thì 403 `FORBIDDEN`.

### 2.4. Định dạng lỗi

```json
{
  "errorCode": "BED_TAKEN",
  "message": "Bed 1532 already has an open assignment",
  "details": { "bedId": 1532, "bedCode": "K1-A101-03" },
  "fieldErrors": [],
  "requestId": "b7e1c0d2-5f1a-4c55-9a0e-1f2d7c3e9a10"
}
```

| Trường | Ý nghĩa |
|---|---|
| `errorCode` | Mã ổn định (mục 5). Frontend **chỉ** dựa vào trường này để dựng câu tiếng Việt (`errorMessages.ts`) |
| `message` | Mô tả kỹ thuật tiếng Anh cho lập trình viên. Frontend không hiển thị, không parse |
| `details` | Dữ liệu cần để dựng câu thông báo (mã giường, hợp đồng bị chồng…). Khóa trong `details` của từng mã ghi ở mục 5 |
| `fieldErrors` | Với `VALIDATION_FAILED`: `[{ "field": "phone", "code": "PATTERN" }]`. `field` dùng đường dẫn JSON (`quotas[1].quota`). `code` thuộc: `REQUIRED`, `PATTERN`, `MIN`, `MAX`, `SIZE`, `PAST`, `FUTURE`, `INVALID` |
| `requestId` | Trùng header `X-Request-Id` và log máy chủ; hiển thị nhỏ ở hộp lỗi 500 để báo lỗi |

### 2.5. Danh sách: phân trang, sắp xếp, lọc

Request: `GET /api/applications?page=0&size=20&sort=priorityScore,desc&status=SUBMITTED&q=nguyen`

| Tham số | Quy ước |
|---|---|
| `page` | Bắt đầu từ **0** (mặc định 0) |
| `size` | Mặc định 20, tối đa 100 |
| `sort` | `field,asc` hoặc `field,desc`; lặp tham số để sắp nhiều cột. Mỗi endpoint ghi rõ trường được sắp |
| `q` | Tìm kiếm tự do (mã SV, họ tên, mã đối tượng) nếu endpoint hỗ trợ |
| Lọc khác | Tên trường như trong response. Lọc nhiều giá trị: lặp tham số (`status=OPEN&status=ASSIGNED`) |

Response:

```json
{
  "content": [ { "id": 12, "code": null, "status": "SUBMITTED" } ],
  "page": 0,
  "size": 20,
  "totalElements": 134,
  "totalPages": 7
}
```

- Item trong danh sách là bản **tóm tắt**; không có `allowedActions` (chỉ API chi tiết có, mục 2.6).
- Phạm vi dữ liệu (đơn vị của Staff, của chính sinh viên) đã được lọc **trong câu truy vấn**, trước khi phân trang (SAD 7.2). Không có tham số nào cho phép mở rộng phạm vi.
- Danh sách nhỏ, cố định (đơn vị, tòa, loại phòng, học kỳ, diện ưu tiên) trả mảng thẳng, không phân trang.

### 2.6. Đối tượng chi tiết, `allowedActions`, timeline

API chi tiết của đơn, hợp đồng, yêu cầu gia hạn, yêu cầu chuyển phòng, lựa chọn hè, ticket, hóa đơn trả về:

```json
{
  "id": 12,
  "status": "SUBMITTED",
  "allowedActions": ["approve", "reject"],
  "timeline": [
    { "fromStatus": "DRAFT", "toStatus": "SUBMITTED", "actor": { "id": 501, "fullName": "Nguyễn Văn A" },
      "reason": null, "at": "2026-10-08T09:32:00+07:00" }
  ]
}
```

- `allowedActions` tính theo **người gọi hiện tại** và trạng thái hiện tại (mục 6). Frontend chỉ hiện nút có trong danh sách. Quy ước tên:
  - Hành động chuyển trạng thái: **trùng đoạn cuối URL** (`POST /api/applications/12/approve` ↔ `"approve"`).
  - Thao tác trên tài nguyên con hoặc không đổi trạng thái (tạo yêu cầu, thêm ghi chú, xác nhận diện ưu tiên, nộp bù cọc…): dùng **mã hành động quy ước ở mục 6** (`create-extension-request` ↔ `POST /api/contracts/{id}/extension-requests`). Mục 6 ghi endpoint tương ứng của từng mã.
- Backend vẫn kiểm tra lại khi nhận request; hành động không hợp lệ trả `409 INVALID_STATE_TRANSITION` kèm `details.currentStatus`.
- `timeline` lấy từ `status_history`, xếp theo thời gian tăng dần. Người thao tác tự động có `actor.fullName = "SYSTEM"`.

### 2.7. Hành động chuyển trạng thái

- Dạng `POST /api/<tài nguyên>/{id}/<hành động>`; không dùng `PUT` / `PATCH` để đổi `status`.
- Hành động cần lý do nhận body `{ "reason": "..." }`; `reason` bắt buộc, 5–500 ký tự sau khi bỏ khoảng trắng → nếu thiếu: `400 VALIDATION_FAILED` với `fieldErrors[0].field = "reason"`.
- Hành động trả `200` với đối tượng chi tiết **sau** khi đổi (có `allowedActions` mới), để frontend cập nhật ngay không cần gọi lại.
- Gửi lặp cùng hành động: lần sau gặp trạng thái mới nên trả `409 INVALID_STATE_TRANSITION` hoặc mã cụ thể hơn (`DUPLICATE_INVOICE`…). Không tạo dữ liệu thừa (SAD 6.3).

### 2.8. Tham chiếu lồng nhau

Đối tượng liên quan trả dạng tóm tắt để frontend hiển thị không cần gọi thêm:

| Kiểu | Trường |
|---|---|
| `UserRef` | `id`, `fullName`, `username` |
| `StudentRef` | `id`, `studentCode`, `fullName`, `gender` |
| `BedRef` | `id`, `code` (`K1-A101-03`), `roomId`, `roomCode`, `buildingId`, `buildingCode`, `floor`, `unitId` |
| `UnitRef` | `id`, `code`, `name` |

---

## 3. Danh mục endpoint

Mỗi bảng tương ứng một module của SAD. "Phạm vi" nhắc lại cách lọc nếu khác quy tắc chung (Staff: đơn vị mình; Student: của chính mình).

### 3.1. `account`: xác thực, tài khoản, hồ sơ, nhật ký

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| GET | `/api/auth/csrf` | công khai | Nhận cookie `XSRF-TOKEN`; 204 | NFR-SEC |
| POST | `/api/auth/login` | công khai | Đăng nhập (mục 4.1) | FR-ACC-01, 02 |
| POST | `/api/auth/logout` | \* | Hủy phiên; 204 | FR-ACC-01 |
| GET | `/api/auth/me` | \* | Người dùng hiện tại (mục 4.1) | FR-ACC-01 |
| POST | `/api/auth/change-password` | \* | `{ currentPassword, newPassword }`; 204. Mật khẩu mới 8–64 ký tự, có chữ và số, khác mật khẩu cũ | FR-ACC-03 |
| GET | `/api/me/profile` | \* | Hồ sơ của tôi (kèm `studentProfile` nếu là sinh viên) | FR-ACC-04 |
| PATCH | `/api/me/profile` | \* | Sửa `phone`, `hometown`, `emergencyContact`; trường khác bị bỏ qua → 400 nếu gửi | FR-ACC-04 |
| GET | `/api/students` | St | Sinh viên trong phạm vi đơn vị (BR-07). Lọc `q`, `cohort`, `faculty`, `hasActiveContract` | UC-14 |
| GET | `/api/students/{id}` | St | Hồ sơ + chỗ ở hiện tại + danh sách hợp đồng | UC-14 |
| POST | `/api/students/{id}/reset-password` | St | Đặt lại mật khẩu sinh viên trong phạm vi (mục 4.1) | FR-ACC-07 |
| GET | `/api/technicians` | St | KTV cùng đơn vị, kèm số ticket đang xử lý (để giao việc) | FR-MNT-03 |
| GET | `/api/admin/users` | A | Lọc `role`, `unitId`, `locked`, `q` | FR-ACC-06 |
| POST | `/api/admin/users` | A | Tạo Staff / Technician / Admin: `{ username, fullName, phone, role, managementUnitId }`; trả mật khẩu tạm một lần (mục 4.1) | FR-ACC-06 |
| GET | `/api/admin/users/{id}` | A | Chi tiết | FR-ACC-06 |
| PATCH | `/api/admin/users/{id}` | A | Sửa `fullName`, `phone`, `managementUnitId`; với sinh viên thêm `studentCode`, `gender`, `dateOfBirth`, `faculty`, `cohort` | FR-ACC-04, 06 |
| POST | `/api/admin/users/{id}/lock` | A | `{ reason }` | FR-ACC-06 |
| POST | `/api/admin/users/{id}/unlock` | A | Mở khóa, xóa đếm đăng nhập sai | FR-ACC-02, 06 |
| POST | `/api/admin/users/{id}/reset-password` | A | Như Staff nhưng cho mọi tài khoản | FR-ACC-07 |
| PUT | `/api/admin/users/{id}/cross-unit-access` | A | `{ enabled: true }`; chỉ cho Staff | FR-ACC-08 |
| POST | `/api/admin/student-imports` | A | Import Excel, hai bước (mục 4.10) | FR-ACC-05 |
| GET | `/api/admin/student-imports/template` | A | Tải file mẫu `.xlsx` | FR-ACC-05 |
| GET | `/api/admin/audit-logs` | A | Lọc `userId`, `action`, `entityType`, `from`, `to`; sắp `createdAt,desc` | FR-ACC-09 |
| GET | `/api/admin/configs` | A | Tham số cấu hình (`system_config`) | SRS 2.6 |
| PUT | `/api/admin/configs/{key}` | A | `{ value }`; kiểm tra kiểu theo khóa | SRS 2.6 |

### 3.2. `room`: đơn vị, tòa, loại phòng, phòng, giường, học kỳ

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| GET | `/api/management-units` | \* | Danh sách đơn vị (mảng) | FR-ROOM-01 |
| POST, PATCH | `/api/admin/management-units[/{id}]` | A | `{ code, name }` | FR-ROOM-01 |
| GET | `/api/buildings` | \* | Tòa (mảng); Staff chỉ thấy tòa đơn vị mình, trừ khi có quyền tổng hợp | FR-ROOM-01 |
| POST, PATCH | `/api/admin/buildings[/{id}]` | A | `{ code, name, managementUnitId, depositAmount }`. Đổi mức cọc chỉ áp dụng cho hợp đồng kích hoạt sau đó | FR-ROOM-01 |
| GET | `/api/room-types` | St | Loại phòng (mảng) | FR-ROOM-02 |
| POST, PATCH | `/api/room-types[/{id}]` | St | `{ name, bedCount, priceMain, priceSummer, areaM2, amenities, active }`. Đổi giá không ảnh hưởng phân phòng đang hiệu lực | FR-ROOM-02 |
| GET | `/api/semesters` | \* | Học kỳ (mảng), sắp theo `startDate` | FR-ROOM-11 |
| POST, PATCH | `/api/semesters[/{id}]` | St | `{ name, academicYear, type, startDate, endDate }`; chồng thời gian → `409 SEMESTER_OVERLAP` | FR-ROOM-11 |
| GET | `/api/rooms` | St | Lọc `buildingId`, `floor`, `roomTypeId`, `gender`, `status`, `hasFreeBed`, `noLeader`; mỗi phòng kèm số giường trống / đang giữ / đang ở (từ `v_room_occupancy`) | FR-ROOM-07, 10 |
| POST | `/api/rooms` | St | `{ buildingId, floor, code, roomTypeId, gender }`; tự tạo giường theo `bedCount` | FR-ROOM-03, 04 |
| GET | `/api/rooms/{id}` | St | Chi tiết: giường, người đang ở / đang giữ từng giường, trưởng phòng, lịch sử trưởng phòng | FR-ROOM-07, 09 |
| PATCH | `/api/rooms/{id}` | St | Sửa `floor`, `code`, `roomTypeId`, `gender`. Đổi `gender` hoặc `roomTypeId` khi còn phân phòng hiệu lực → `409 ROOM_OCCUPIED` | FR-ROOM-03 |
| POST | `/api/rooms/{id}/status` | St | `{ status: ACTIVE \| MAINTENANCE \| CLOSED, reason }`; còn phân phòng hiệu lực → `409 ROOM_OCCUPIED` | FR-ROOM-05, 06 |
| POST | `/api/rooms/{id}/beds` | St | Thêm một giường `{ code }` | FR-ROOM-04 |
| POST | `/api/beds/{id}/status` | St | `{ status: ACTIVE \| BROKEN \| RETIRED, reason }`; còn phân phòng hiệu lực → `409 BED_OCCUPIED` | FR-ROOM-04, 05, 06 |
| GET | `/api/beds/available` | St | Giường trống cho split view (mục 4.4). Tham số `applicationId` **hoặc** `transferRequestId` **hoặc** `storageRecordId` | FR-REG-08, FR-CON-07, 12 |
| PUT | `/api/rooms/{id}/leader` | St | `{ studentId }`; phải đang ở trong phòng (BR-11) | FR-ROOM-09 |
| DELETE | `/api/rooms/{id}/leader` | St | Gỡ trưởng phòng `{ reason }`; 204 | FR-ROOM-09 |
| GET | `/api/me/room` | S | Phòng, giường của tôi, bạn cùng phòng (họ tên), trưởng phòng; `null` nếu không ở | FR-ROOM-08 |

Không có `DELETE /api/rooms/{id}` hay `DELETE /api/beds/{id}`: phòng, giường đã từng có phân phòng chỉ chuyển `CLOSED` / `RETIRED` (FR-ROOM-06). Phòng, giường chưa từng dùng cũng dùng trạng thái cho thống nhất.

### 3.3. `registration`: đợt đăng ký, đơn, kỳ hè

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| GET | `/api/priority-categories` | \* | Diện ưu tiên đang dùng (mảng); Staff, Admin thấy cả trọng số | FR-REG-05 |
| POST, PATCH | `/api/admin/priority-categories[/{id}]` | A | `{ code, name, weight, active }` | FR-REG-05 |
| GET | `/api/registration-periods` | St | Đợt áp dụng cho đơn vị mình; lọc `semesterId`, `kind`, `open` | FR-REG-01 |
| POST | `/api/registration-periods` | St | Tạo đợt (mục 4.2). Đợt `SUMMER` tạo thông báo cho sinh viên ACTIVE của đơn vị | FR-REG-01, 13 |
| GET | `/api/registration-periods/{id}` | St | Chi tiết + chỉ tiêu + thống kê số đơn theo trạng thái | FR-REG-01 |
| PATCH | `/api/registration-periods/{id}` | St | Sửa khi chưa mở; đã mở chỉ được lùi `closeAt` | FR-REG-01 |
| GET | `/api/me/registration-periods/open` | S | Đợt đang mở mà tôi được nộp, kèm `myApplicationId` nếu đã có đơn | FR-REG-02 |
| POST | `/api/applications` | S | Tạo đơn DRAFT (mục 4.3) | FR-REG-02, 03 |
| PATCH | `/api/applications/{id}` | S | Sửa đơn DRAFT | FR-REG-02 |
| POST | `/api/applications/{id}/submit` | S | DRAFT → SUBMITTED; tính điểm ưu tiên | FR-REG-04, 05 |
| POST | `/api/applications/{id}/cancel` | S | DRAFT / SUBMITTED / APPROVED / ASSIGNED → CANCELLED. Đang ASSIGNED thì đóng giữ chỗ và hủy hợp đồng PENDING cùng lúc | FR-REG-04, 11 |
| GET | `/api/me/applications` | S | Đơn của tôi, mới nhất trước | FR-REG-12 |
| GET | `/api/applications` | St | Đơn thuộc đơn vị nguyện vọng = đơn vị mình. Lọc `periodId`, `status`, `preferredRoomTypeId`, `gender`, `q`; sắp `priorityScore`, `submittedAt` | FR-REG-05 |
| GET | `/api/applications/{id}` | S, St | Chi tiết (mục 4.3); sinh viên thấy lý do từ chối, giường đang giữ, hạn kích hoạt | FR-REG-12 |
| POST | `/api/applications/{id}/priorities/{categoryId}/verify` | St | `{ verified: true \| false, note }`; tính lại điểm | FR-REG-06 |
| POST | `/api/applications/{id}/approve` | St | SUBMITTED → APPROVED | FR-REG-07 |
| POST | `/api/applications/{id}/reject` | St | SUBMITTED → REJECTED, `{ reason }` | FR-REG-07 |
| POST | `/api/applications/{id}/assign` | St | APPROVED → ASSIGNED: phân giường (mục 4.4) | FR-REG-08, 09, 10 |
| POST | `/api/applications/{id}/release-hold` | St | ASSIGNED → APPROVED, `{ reason }`: hủy giữ chỗ để phân lại | FR-REG-11 |
| POST | `/api/summer-choices` | S | `{ periodId, choice: STAY \| TEMP_LEAVE \| LEAVE }`; cần hợp đồng ACTIVE và đợt `SUMMER` đang mở. `TEMP_LEAVE` cần hợp đồng đã gia hạn sang năm học sau → nếu chưa: `409 TEMP_LEAVE_NOT_EXTENDED` | FR-REG-14, FR-CON-11 |
| GET | `/api/me/summer-choice` | S | Lựa chọn hè hiện tại của tôi (`null` nếu chưa chọn) | FR-REG-14 |
| PATCH | `/api/summer-choices/{id}` | S | Đổi `choice` khi còn PENDING | FR-REG-14 |
| GET | `/api/summer-choices` | St | Lọc `semesterId`, `choice`, `status` | FR-REG-14 |
| GET | `/api/summer-choices/{id}` | S, St | Chi tiết | FR-REG-14 |
| POST | `/api/summer-choices/{id}/approve` | St | `STAY`: gia hạn hợp đồng qua hè (ghi như một gia hạn, `source = SUMMER_STAY`). `TEMP_LEAVE`: chỉ duyệt, việc gửi đồ làm ở `/storage-records`. `LEAVE`: chỉ duyệt, trả phòng làm ở `/move-out` | FR-REG-15, FR-CON-11, 13 |
| POST | `/api/summer-choices/{id}/reject` | St | `{ reason }` | FR-REG-14 |

### 3.4. `contract`: hợp đồng, gia hạn, chuyển phòng, trả phòng, gửi đồ

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| GET | `/api/contracts` | St | Lọc `status`, `label` (`EXPIRING`, `OVERDUE_MOVE_OUT`, `SUMMER_LEAVE`), `buildingId`, `semesterId`, `q`; sắp `endDate`, `activatedAt` | FR-CON-03, 04, 05 |
| GET | `/api/contracts/{id}` | S, St | Chi tiết (mục 4.5): thời hạn, cọc, nhãn, lịch sử phân phòng, yêu cầu đang chờ | FR-CON-05 |
| GET | `/api/me/contract` | S | Hợp đồng PENDING / ACTIVE hiện tại (`null` nếu không có) | FR-CON-05 |
| GET | `/api/me/contracts` | S | Mọi hợp đồng của tôi | FR-CON-05 |
| POST | `/api/contracts/{id}/activate` | St | PENDING → ACTIVE, thu cọc (mục 4.6) | FR-CON-01 |
| POST | `/api/contracts/{id}/extension-requests` | S | `{ newEndDate, reason }`; ≤ 1 yêu cầu PENDING mỗi hợp đồng | FR-CON-06 |
| GET | `/api/extension-requests` | St | Lọc `status` | FR-CON-06 |
| GET | `/api/extension-requests/{id}` | S, St | Chi tiết: ngày cũ / mới, hợp đồng bị chồng (nếu có) | FR-CON-06 |
| POST | `/api/extension-requests/{id}/approve` | St | Cập nhật `endDate`; chồng hợp đồng khác → `409 CONTRACT_PERIOD_OVERLAP` | FR-CON-06 |
| POST | `/api/extension-requests/{id}/reject` | St | `{ reason }` | FR-CON-06 |
| POST | `/api/extension-requests/{id}/cancel` | S | Hủy khi còn PENDING | FR-CON-06 |
| POST | `/api/contracts/{id}/transfer-requests` | S | `{ reason }`; cần đang ở (STAY) | FR-CON-07 |
| GET | `/api/transfer-requests` | St | Lọc `status` | FR-CON-07 |
| GET | `/api/transfer-requests/{id}` | S, St | Chi tiết: giường hiện tại, lý do | FR-CON-07 |
| POST | `/api/transfer-requests/{id}/approve` | St | `{ targetBedId }`: chuyển phòng (mục 4.7) | FR-CON-07, 08 |
| POST | `/api/transfer-requests/{id}/reject` | St | `{ reason }` | FR-CON-07 |
| POST | `/api/transfer-requests/{id}/cancel` | S | Hủy khi còn PENDING | FR-CON-07 |
| GET | `/api/contracts/{id}/move-out/preview` | St | Xem trước trả phòng: công nợ, số dư cọc, tiền hoàn, kiểu kết thúc (mục 4.8) | FR-CON-09 |
| POST | `/api/contracts/{id}/move-out` | St | Làm thủ tục trả phòng (mục 4.8) | FR-CON-09, 10, 13 |
| POST | `/api/contracts/{id}/storage-records` | St | Tạm rời hè: ghi phiếu gửi đồ, đóng phân phòng (mục 4.9) | FR-CON-11 |
| GET | `/api/storage-records` | St | Sinh viên đang tạm rời hè (`v_summer_leave`): phiếu, giường cũ, giường cũ còn trống không. Lọc `returned` | FR-CON-12 |
| POST | `/api/storage-records/{id}/return` | St | Đầu HK1: phân lại giường + ghi ngày nhận lại đồ (mục 4.9) | FR-CON-12 |

### 3.5. `maintenance`: yêu cầu sửa chữa

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| POST | `/api/maintenance-requests` | S, St | Tạo ticket, `multipart/form-data` (mục 4.11). Sinh viên: phòng đang ở; Staff: một phòng hoặc khu vực chung thuộc đơn vị mình | FR-MNT-01, 02 |
| GET | `/api/maintenance-requests` | S, St, T | Sinh viên: ticket do mình tạo **và** ticket của phòng mình đang ở. Staff: đơn vị mình. KTV: ticket được giao. Lọc `status`, `severity`, `category`, `buildingId`, `scope` (`ROOM`, `COMMON`) | FR-MNT-04, 08 |
| GET | `/api/maintenance-requests/{id}` | S, St, T | Chi tiết, ảnh, ghi chú tiến độ, timeline, `allowedActions` | FR-MNT-08 |
| GET | `/api/maintenance-requests/{id}/photos/{photoId}` | S, St, T | Ảnh (kiểm tra quyền như chi tiết) | FR-MNT-01 |
| POST | `/api/maintenance-requests/{id}/assign` | St | OPEN → ASSIGNED, `{ technicianId }`; KTV phải cùng đơn vị → nếu không: `409 TECHNICIAN_OUT_OF_UNIT` | FR-MNT-03 |
| POST | `/api/maintenance-requests/{id}/reject` | St | OPEN → REJECTED, `{ reason }` | FR-MNT-03 |
| POST | `/api/maintenance-requests/{id}/cancel` | S | OPEN → CANCELLED (người tạo) | FR-MNT-03 |
| POST | `/api/maintenance-requests/{id}/start` | T | ASSIGNED → IN_PROGRESS | FR-MNT-04 |
| POST | `/api/maintenance-requests/{id}/notes` | T | Ghi chú tiến độ `{ note }`; 201 | FR-MNT-04 |
| POST | `/api/maintenance-requests/{id}/resolve` | T | IN_PROGRESS → RESOLVED, `{ note }` | FR-MNT-04 |
| POST | `/api/maintenance-requests/{id}/accept` | S, St | RESOLVED → CLOSED. Sinh viên: người tạo. Staff: ticket do Staff tạo (khu vực chung hoặc phòng), hoặc nghiệm thu thay khi sinh viên tạo đã trả phòng (khi đó bắt buộc `{ reason }`) | FR-MNT-05, 07 |
| POST | `/api/maintenance-requests/{id}/reopen` | S, St | RESOLVED → IN_PROGRESS, `{ reason }` (không chấp nhận nghiệm thu) | FR-MNT-05 |
| GET | `/api/maintenance-requests/{id}/chargeable-students` | St | Sinh viên đang ở trong phòng lúc tạo ticket (ứng viên chịu trừ cọc), mục 4.12 | FR-MNT-10 |
| POST | `/api/maintenance-requests/{id}/conclude` | St | Ticket của phòng đã CLOSED: kết luận nguyên nhân, chi phí; `MISUSE` thì trừ cọc (mục 4.12) | FR-MNT-09, 10 |

### 3.6. `billing`: điện nước, hóa đơn, tiền cọc, công nợ

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| GET | `/api/utility-rates` | St | Đơn giá theo thời gian; lọc `meterType` | FR-BIL-01 |
| POST | `/api/utility-rates` | St | `{ meterType: ELECTRIC \| WATER, unitPrice, effectiveFrom }`; trùng ngày → `409 RATE_EXISTS` | FR-BIL-01 |
| GET | `/api/utility-readings` | St | Bảng nhập chỉ số một tháng: `billingMonth` (bắt buộc), `buildingId`; mỗi phòng hai dòng điện / nước, có `startReading` (tự lấy từ tháng trước), `endReading`, `invoiced` | FR-BIL-02 |
| PUT | `/api/utility-readings` | St | Lưu hàng loạt chỉ số một tháng (mục 4.13) | FR-BIL-02 |
| POST | `/api/utility-readings/{id}/meter-replacement` | St | `{ oldMeterFinalReading, newMeterStartReading, reason }` | FR-BIL-03 |
| GET | `/api/invoices/room-fee/preview` | St | `semesterId`, `buildingId?`: số sinh viên sẽ phát hành, số đã có hóa đơn, tổng tiền | FR-BIL-05, 06 |
| POST | `/api/invoices/room-fee/issue` | St | Phát hành phí phòng (mục 4.14) | FR-BIL-05, 06, 07 |
| GET | `/api/invoices/utility/preview` | St | `billingMonth`, `buildingId?`: số phòng sẽ phát hành, đã phát hành, tổng tiền; `roomsMissingReading` và `roomsWithoutLeader` dạng `[{ id, code }]` để hiện cảnh báo trước khi phát hành (mục 4.14) | FR-BIL-04, 06 |
| POST | `/api/invoices/utility/issue` | St | Phát hành điện nước theo phòng (mục 4.14) | FR-BIL-04, 06, 07 |
| GET | `/api/invoices` | St | Lọc `type`, `status`, `overdue`, `semesterId`, `billingMonth`, `buildingId`, `roomId`, `studentId`, `q` | FR-BIL-08, 11 |
| GET | `/api/invoices/{id}` | S, St | Chi tiết, các dòng, `allowedActions`. Sinh viên: hóa đơn phí phòng của mình, hóa đơn điện nước của phòng mình đang ở hoặc đã ở trong tháng đó | FR-BIL-04, 10 |
| GET | `/api/me/invoices` | S | Hóa đơn của tôi và của phòng tôi; lọc `type`, `status` | FR-BIL-10 |
| POST | `/api/invoices/{id}/confirm-payment` | St | UNPAID → PAID (mục 4.15) | FR-BIL-08 |
| POST | `/api/invoices/{id}/adjustments` | St | Thêm dòng điều chỉnh vào hóa đơn **UNPAID** này cho một hóa đơn đã PAID trước đó của cùng sinh viên / phòng: `{ adjustsInvoiceId, amount (âm / dương, ≠ 0), reason }`. Tổng hóa đơn sau điều chỉnh phải ≥ 0 → nếu âm: `409 ADJUSTMENT_INVALID`, `details: { totalAfter }` (CSDL cũng chặn bằng `CHECK (total_amount >= 0)`) | FR-BIL-12 |
| GET | `/api/contracts/{id}/deposit` | S, St | Sổ cọc: mức cọc, số dư, số cần bù, hạn bù, quá hạn, các giao dịch (mục 4.16) | FR-BIL-09, 10 |
| GET | `/api/me/deposit` | S | Sổ cọc của hợp đồng hiện tại | FR-BIL-10 |
| POST | `/api/contracts/{id}/deposit/top-ups` | St | Xác nhận sinh viên nộp bù: `{ amount, paymentMethod, note }`; `amount` ≤ số cần bù → nếu lớn hơn: `409 TOPUP_EXCEEDS_REQUIRED` | FR-BIL-09 |
| GET | `/api/deposits/overdue-top-ups` | St | Danh sách cần nhắc: hợp đồng có khoản bù quá hạn | FR-BIL-13 |
| GET | `/api/debts/students` | St | Công nợ theo sinh viên: phí phòng chưa đóng, bù cọc (khi ACTIVE), số dư cọc âm sau trả phòng. Lọc `semesterId`, `buildingId`, `contractStatus` | FR-BIL-11, FR-CON-10 |
| GET | `/api/debts/rooms` | St | Công nợ điện nước theo phòng; lọc `billingMonth`, `buildingId` | FR-BIL-11 |
| GET | `/api/debts/export` | St | Xuất `.xlsx` theo bộ lọc của hai API trên + `kind=STUDENT \| ROOM` | FR-BIL-11 |
| GET | `/api/me/debts` | S | Công nợ của tôi (kể cả sau khi đã trả phòng) | FR-CON-10 |

### 3.7. `notification`: thông báo, dashboard, hộp việc chờ

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| GET | `/api/notifications` | \* | Hộp thư của tôi (`v_user_notification`), mới nhất trước; lọc `unread` | FR-NOT-03 |
| GET | `/api/notifications/unread-count` | \* | `{ count }`; frontend gọi lại mỗi 60 giây và khi đổi trang | FR-NOT-03 |
| POST | `/api/notifications/{id}/read` | \* | Đánh dấu đã đọc; 204 (gọi lại không lỗi) | FR-NOT-03 |
| POST | `/api/notifications/read-all` | \* | 204 | FR-NOT-03 |
| POST | `/api/notifications` | St | Đăng thông báo (mục 4.17) | FR-NOT-01, 02 |
| GET | `/api/notifications/published` | St | Thông báo do Staff đơn vị mình đăng (cả loại `ROOM`) | FR-NOT-01 |
| GET | `/api/dashboard` | St | Số liệu dashboard (mục 4.18); `unitId` chỉ dùng khi có quyền tổng hợp, bỏ trống = toàn KTX | FR-NOT-05, 06 |
| GET | `/api/pending-items` | St | Hộp "Yêu cầu chờ xử lý" (UI mục 22A): đơn SUBMITTED, gia hạn, chuyển phòng, lựa chọn hè PENDING, hợp đồng PENDING chờ kích hoạt. Lọc `type`; trả `{ type, entityId, student, submittedAt }` | UI 22A |

---

## 4. Schema chi tiết

Chỉ ghi những endpoint có logic hoặc dữ liệu phức tạp. Các endpoint CRUD đơn giản dùng đúng trường của bảng tương ứng trong CSDL (đổi sang `camelCase`).

### 4.1. Đăng nhập, người dùng hiện tại, mật khẩu tạm

`POST /api/auth/login`

```json
{ "username": "2251060001", "password": "********" }
```

200 trả `Me` (giống `GET /api/auth/me`):

```json
{
  "id": 501,
  "username": "2251060001",
  "fullName": "Nguyễn Văn A",
  "role": "STUDENT",
  "managementUnit": null,
  "crossUnitAccess": false,
  "mustChangePassword": true,
  "student": { "studentCode": "2251060001", "gender": "M", "isRoomLeader": false, "hasActiveContract": false }
}
```

- Sai tên hoặc mật khẩu: `401 INVALID_CREDENTIALS` (không phân biệt sai tên hay sai mật khẩu). `details.remainingAttempts` khi còn ≤ 2 lần.
- Bị khóa tạm: `403 ACCOUNT_TEMP_LOCKED`, `details.lockedUntil`. Bị Admin khóa: `403 ACCOUNT_LOCKED`.
- Với Staff / KTV: `managementUnit` là `UnitRef`, `student` là `null`.

Đặt lại mật khẩu (`/api/students/{id}/reset-password`, `/api/admin/users/{id}/reset-password`) và tạo tài khoản (`POST /api/admin/users`) trả:

```json
{ "userId": 501, "temporaryPassword": "Kt7mQ2vx" }
```

Mật khẩu tạm sinh ngẫu nhiên, **chỉ trả một lần** trong response này, không lưu dạng rõ, không ghi log; tài khoản bật `mustChangePassword`. Người đặt lại đọc cho sinh viên. Riêng import Excel dùng mật khẩu mặc định theo cấu hình (FR-ACC-05).

### 4.2. Tạo đợt đăng ký

`POST /api/registration-periods`

```json
{
  "semesterId": 7,
  "kind": "REGULAR",
  "name": "Đăng ký ở KTX HK1 2026-2027",
  "openAt": "2026-08-01T08:00:00+07:00",
  "closeAt": "2026-08-15T17:00:00+07:00",
  "unitIds": [1, 2],
  "quotas": [ { "buildingId": 1, "quota": 120 }, { "buildingId": 2, "quota": 80 } ]
}
```

- `closeAt > openAt`; `unitIds` khác rỗng; Staff không có quyền tổng hợp chỉ được ghi đơn vị mình → nếu không: `403 FORBIDDEN`. `quotas[].buildingId` phải thuộc các đơn vị trong `unitIds`.
- `kind = SUMMER`: `semesterId` phải là học kỳ hè; không có `quotas`.

### 4.3. Đơn đăng ký

`POST /api/applications` (và `PATCH` khi DRAFT)

```json
{
  "periodId": 12,
  "preferredUnitId": 1,
  "preferredRoomTypeId": 3,
  "priorityCategoryIds": [2, 5],
  "note": "Ở xa, nhà ở Lào Cai"
}
```

Lỗi: `409 PERIOD_NOT_OPEN`, `409 APPLICATION_EXISTS` (đã có đơn trong đợt), `409 CONTRACT_EXISTS` (đang có hợp đồng PENDING / ACTIVE, FR-REG-03), `400 UNIT_NOT_IN_PERIOD`.

`GET /api/applications/{id}`:

```json
{
  "id": 88,
  "status": "ASSIGNED",
  "student": { "id": 501, "studentCode": "2251060001", "fullName": "Nguyễn Văn A", "gender": "M" },
  "period": { "id": 12, "name": "Đăng ký ở KTX HK1 2026-2027", "semesterId": 7 },
  "preferredUnit": { "id": 1, "code": "K1", "name": "Ký túc xá K1" },
  "preferredRoomType": { "id": 3, "name": "Phòng 6 giường" },
  "priorities": [
    { "categoryId": 2, "name": "Năm nhất", "weight": 30, "verified": true, "verifiedBy": { "id": 31, "fullName": "Trần Thị B" } },
    { "categoryId": 5, "name": "Ở xa", "weight": 20, "verified": null, "verifiedBy": null }
  ],
  "priorityScore": 50,
  "note": "Ở xa, nhà ở Lào Cai",
  "rejectReason": null,
  "submittedAt": "2026-08-02T10:15:00+07:00",
  "hold": {
    "assignmentId": 940,
    "bed": { "id": 1532, "code": "K1-A101-03", "roomId": 210, "roomCode": "A101", "buildingId": 1, "buildingCode": "K1", "floor": 1, "unitId": 1 },
    "contractId": 377,
    "contractCode": "HD-2026-0377",
    "activationDeadline": "2026-08-27T10:20:00+07:00"
  },
  "allowedActions": ["release-hold"],
  "timeline": [ ]
}
```

- `priorityScore`: tổng trọng số các diện chưa bị xác nhận "không đúng" (`verified != false`).
- `hold` chỉ có khi `ASSIGNED`.

### 4.4. Giường trống và phân giường

`GET /api/beds/available?applicationId=88` trả giường đủ điều kiện, gom theo phòng, đã lọc: đơn vị của Staff, giới tính sinh viên, phòng `ACTIVE`, giường `ACTIVE`, không có phân phòng hiệu lực. Phòng đúng loại mong muốn xếp trước.

```json
[
  {
    "room": { "id": 210, "code": "A101", "buildingCode": "K1", "floor": 1, "roomType": { "id": 3, "name": "Phòng 6 giường" }, "gender": "M" },
    "matchesPreferredType": true,
    "freeBeds": [ { "id": 1532, "code": "K1-A101-03" }, { "id": 1535, "code": "K1-A101-06" } ],
    "occupiedCount": 3,
    "heldCount": 1
  }
]
```

Với `transferRequestId`: chỉ giường cùng đơn vị với giường hiện tại (BR-05), bỏ giường hiện tại. Với `storageRecordId`: giường cũ (nếu còn trống) đứng đầu, đánh dấu `isPreviousBed: true`.

`POST /api/applications/{id}/assign`

```json
{ "bedId": 1532 }
```

Một transaction (SAD 6.2): khóa giường, khóa sinh viên, kiểm tra lại, tạo phân phòng `HOLD` (chụp đơn giá), tạo hợp đồng `PENDING` (`activationDeadline` = bây giờ + cấu hình `contract.activation_days`), đơn → `ASSIGNED`. 200 trả chi tiết đơn như 4.3.

| Lỗi | Khi |
|---|---|
| `409 BED_TAKEN` | Giường vừa bị chiếm (kể cả do request đồng thời, bắt từ `ux_assignment_open_bed`). `details: { bedId, bedCode }` |
| `409 BED_NOT_AVAILABLE` | Giường / phòng không `ACTIVE`. `details: { bedCode, bedStatus, roomStatus }` |
| `409 GENDER_MISMATCH` | Giới tính phòng khác sinh viên |
| `404 NOT_FOUND` | Giường thuộc đơn vị khác |
| `409 STUDENT_ALREADY_ASSIGNED` | Sinh viên đã có phân phòng hiệu lực (gửi lặp) |
| `409 INVALID_STATE_TRANSITION` | Đơn không còn `APPROVED` |

Frontend gặp `BED_TAKEN` thì refetch danh sách giường và giữ nguyên split view (UI 14.2).

### 4.5. Chi tiết hợp đồng

`GET /api/contracts/{id}`

```json
{
  "id": 377,
  "code": "HD-2026-0377",
  "status": "ACTIVE",
  "labels": ["EXPIRING"],
  "daysRemaining": 12,
  "student": { "id": 501, "studentCode": "2251060001", "fullName": "Nguyễn Văn A", "gender": "M" },
  "startDate": "2026-08-20",
  "endDate": "2027-06-30",
  "activationDeadline": "2026-08-27T10:20:00+07:00",
  "activatedAt": "2026-08-21T09:00:00+07:00",
  "activatedBy": { "id": 31, "fullName": "Trần Thị B", "username": "staff.k1.b" },
  "deposit": { "required": 1000000, "balance": 950000, "topUpRequired": 50000, "topUpOverdue": false },
  "currentAssignment": {
    "id": 940, "kind": "STAY",
    "bed": { "id": 1532, "code": "K1-A101-03", "roomId": 210, "roomCode": "A101", "buildingId": 1, "buildingCode": "K1", "floor": 1, "unitId": 1 },
    "startedAt": "2026-08-18T10:20:00+07:00", "stayStartedAt": "2026-08-21T09:00:00+07:00",
    "priceMain": 1800000, "priceSummer": 900000
  },
  "assignments": [ ],
  "pendingRequests": { "extensionRequestId": null, "transferRequestId": null, "summerChoiceId": null },
  "openStorageRecordId": null,
  "moveOut": null,
  "allowedActions": ["move-out", "create-extension-request", "create-transfer-request"],
  "timeline": [ ]
}
```

- `labels` (tính, không lưu, BR-08): `EXPIRING`, `OVERDUE_MOVE_OUT`, `SUMMER_LEAVE`.
- `deposit` lấy từ `v_deposit_balance`. `topUpRequired` chỉ khác 0 khi `ACTIVE`.
- `assignments`: toàn bộ lịch sử phân phòng (mới nhất trước), mỗi dòng có `bed`, `kind`, `startedAt`, `endedAt`, `endReason`, đơn giá.

### 4.6. Kích hoạt hợp đồng

`POST /api/contracts/{id}/activate`

```json
{ "paperContractSigned": true, "depositAmount": 1000000, "paymentMethod": "CASH", "note": null }
```

- `depositAmount` phải **bằng** mức cọc của tòa (frontend điền sẵn từ `deposit.required` của chi tiết hợp đồng PENDING) → nếu khác: `400 DEPOSIT_AMOUNT_MISMATCH`, `details: { required }`.
- `paperContractSigned` phải `true` → nếu không: `400 VALIDATION_FAILED`.
- Một transaction: ghi giao dịch `DEPOSIT`, hợp đồng → `ACTIVE`, phân phòng `HOLD → STAY` trên cùng dòng.
- Quá hạn kích hoạt (job chưa kịp hủy): `409 CONTRACT_ACTIVATION_EXPIRED`. Đã bị hủy: `409 CONTRACT_AUTO_CANCELLED`.

### 4.7. Chuyển phòng

`POST /api/transfer-requests/{id}/approve`

```json
{ "targetBedId": 1610 }
```

Một transaction: khóa giường nguồn và đích theo `bed_id` tăng dần, kiểm tra lại, đóng phân phòng cũ (`endReason = TRANSFER`), mở phân phòng `STAY` mới (đơn giá phòng mới), gỡ trưởng phòng nếu cần, yêu cầu → `APPROVED`. Lỗi: `BED_TAKEN`, `BED_NOT_AVAILABLE`, `GENDER_MISMATCH`, `409 TRANSFER_CROSS_UNIT` (giường đích khác đơn vị), `409 SAME_BED`. 200 trả chi tiết yêu cầu.

### 4.8. Trả phòng

`GET /api/contracts/{id}/move-out/preview?moveOutDate=2027-06-30`

```json
{
  "endStatus": "EXPIRED",
  "depositBalance": 950000,
  "refundAmount": 950000,
  "negativeBalanceDebt": 0,
  "unpaidRoomFeeInvoices": [ { "id": 4410, "code": "HP-2027-0410", "amount": 1800000, "dueDate": "2027-01-20" } ],
  "unpaidRoomUtilityInvoices": [ { "id": 5120, "code": "DN-2027-0120", "amount": 420000 } ],
  "unsettledTickets": [
    { "id": 1024, "code": "TK-1024", "status": "RESOLVED", "concluded": false },
    { "id": 1031, "code": "TK-1031", "status": "CLOSED", "concluded": false }
  ],
  "isRoomLeader": true
}
```

- `endStatus`: `EXPIRED` nếu `moveOutDate ≥ endDate`, ngược lại `TERMINATED`.
- `refundAmount = max(depositBalance, 0)`; `negativeBalanceDebt = max(−depositBalance, 0)`. Khoản bù cọc chưa nộp đã nằm trong số dư, không thành nợ riêng (BR-10).
- Hóa đơn điện nước của phòng chỉ là cảnh báo (thuộc phòng, không thuộc sinh viên).
- `unsettledTickets`: ticket **của phòng** sinh viên đang ở mà sinh viên có tên trong danh sách người ở lúc tạo ticket, chưa xử lý xong chi phí: trạng thái `OPEN` / `ASSIGNED` / `IN_PROGRESS` / `RESOLVED`, hoặc `CLOSED` nhưng chưa kết luận nguyên nhân. Mục đích: Staff kết luận trước khi chốt trả phòng để tiền hoàn đã tính khoản trừ.
- Nếu Staff vẫn trả phòng rồi mới kết luận `MISUSE`: khoản trừ ghi vào hợp đồng đã kết thúc làm số dư âm, hiện thành **nợ cọc** của sinh viên (`negativeBalanceDebt`, FR-CON-10). Biên bản trả phòng không bị sửa và không có "quá hạn bù cọc" (view chỉ tính với hợp đồng ACTIVE). Dữ liệu không lệch, chỉ là thu sau thay vì trừ trước.

`POST /api/contracts/{id}/move-out`

```json
{ "moveOutDate": "2027-06-30", "assetCondition": "DAMAGED", "assetNote": "Gãy chân ghế", "acknowledgedWarnings": true }
```

- `assetCondition = DAMAGED` bắt buộc `assetNote`. Hư hỏng tài sản phát hiện lúc trả phòng **không** tự trừ cọc; muốn trừ thì Staff tạo ticket của phòng (mục 4.11, `roomId`), xử lý và kết luận `MISUSE` trước khi trả phòng (Q-API-04).
- Có công nợ hoặc `unsettledTickets` mà `acknowledgedWarnings` khác `true` → `409 MOVE_OUT_WARNINGS_NOT_ACKNOWLEDGED` với `details` như preview. Cảnh báo không chặn trả phòng (FR-CON-10).
- Một transaction: ghi `move_out_record`, đóng phân phòng, gỡ trưởng phòng, hợp đồng → `EXPIRED` / `TERMINATED`, ghi `REFUND` nếu `refundAmount > 0`. 200 trả chi tiết hợp đồng (có `moveOut`).

### 4.9. Tạm rời hè và nhận lại giường

`POST /api/contracts/{id}/storage-records`

```json
{ "packageCount": 3, "description": "2 thùng sách, 1 vali", "location": "Kho K1 tầng 1, kệ B", "depositedAt": "2027-06-25" }
```

Điều kiện: lựa chọn hè `TEMP_LEAVE` đã `APPROVED`; hợp đồng `ACTIVE` đã gia hạn sang năm học sau. Một transaction: tạo phiếu (lưu giường cũ), đóng phân phòng (`endReason = SUMMER_LEAVE`), gỡ trưởng phòng. Lỗi: `409 SUMMER_CHOICE_NOT_APPROVED`, `409 TEMP_LEAVE_NOT_EXTENDED`, `409 STORAGE_RECORD_EXISTS`.

`POST /api/storage-records/{id}/return`

```json
{ "bedId": 1532, "returnedAt": "2027-08-20" }
```

Một transaction: chiếm giường qua `BedOccupancyService` (mở phân phòng `STAY` trực tiếp, không cần đơn mới), ghi ngày nhận lại đồ. Lỗi như 4.4.

### 4.10. Import sinh viên

`POST /api/admin/student-imports` (`multipart/form-data`): phần `file` (`.xlsx`, ≤ 5 MB) và tham số `dryRun=true|false`.

- Bước 1 `dryRun=true`: chỉ kiểm tra, không ghi gì. Bước 2 `dryRun=false` (gửi lại cùng file): tạo các dòng hợp lệ trong một transaction, bỏ qua dòng lỗi và dòng trùng. Hai bước dùng chung một kết quả:

```json
{
  "dryRun": true,
  "totalRows": 1500,
  "validRows": 1487,
  "createdRows": 0,
  "existingRows": 3,
  "errorRows": 10,
  "rows": [
    { "rowNumber": 23, "studentCode": "20230023", "result": "ERROR", "errors": [ { "column": "phone", "code": "PATTERN" } ] },
    { "rowNumber": 41, "studentCode": "20230041", "result": "EXISTS", "errors": [] }
  ]
}
```

- `rows` chỉ liệt kê dòng `ERROR` và `EXISTS` (dòng hợp lệ không liệt kê để response gọn).
- `result`: `CREATED` (bước 2), `VALID` (bước 1), `EXISTS`, `ERROR`. File sai cấu trúc (thiếu cột, không phải xlsx): `400 IMPORT_FILE_INVALID`, `details.missingColumns`.
- Mức Could (SAD 9): tham số `resultFile=true` trả lại file `.xlsx` có thêm cột "Kết quả" và "Lỗi". Định dạng cột ở mục 7.1.

### 4.11. Tạo ticket kèm ảnh

`POST /api/maintenance-requests` (`multipart/form-data`)

| Phần | Nội dung |
|---|---|
| `data` | JSON (`application/json`), bên dưới |
| `photos` | 0–3 file JPEG / PNG / WebP, mỗi file ≤ 5 MB (SAD 9) |

```json
{ "category": "ELECTRIC", "severity": "MEDIUM", "description": "Bóng đèn phòng tắm không sáng", "roomId": null, "buildingId": null, "floor": null }
```

- Sinh viên: không gửi `roomId` / `buildingId` / `floor`; ticket gắn với **phòng đang ở** (lấy từ phân phòng `STAY`). Không đang ở → `409 NOT_STAYING`.
- Staff: gửi **hoặc** `roomId` (ticket của phòng, có thể kết luận trừ cọc), **hoặc** `buildingId` + `floor` (khu vực chung; vị trí cụ thể ghi trong `description`). Gửi cả hai hoặc không gửi gì → `400 TICKET_LOCATION_INVALID`. Phòng / tòa phải thuộc đơn vị mình, nếu không: 404.
- Ticket do Staff tạo thì Staff nghiệm thu (FR-MNT-05). Với ticket phòng, "người ở lúc tạo ticket" (dùng để chia trừ cọc, mục 4.12) tính như ticket do sinh viên tạo.
- Lỗi ảnh: `400 PHOTO_INVALID` (`details: { reason: COUNT \| TYPE \| SIZE, fileName }`), `500 PHOTO_UPLOAD_FAILED`.
- 201 trả chi tiết ticket.

### 4.12. Kết luận nguyên nhân, trừ cọc

`POST /api/maintenance-requests/{id}/conclude`

```json
{ "cause": "MISUSE", "repairCost": 150000, "chargedStudentIds": [501, 502], "note": "Vỡ kính cửa sổ do va đập; 2 bạn còn lại không ở phòng lúc xảy ra" }
```

- Chỉ ticket **của phòng** đã `CLOSED` và chưa kết luận → nếu không: `409 INVALID_STATE_TRANSITION` / `409 TICKET_ALREADY_CONCLUDED`; ticket khu vực chung → `409 COMMON_AREA_NO_CHARGE`.
- `MISUSE`: `repairCost > 0`. `WEAR`: không gửi `repairCost`, bắt buộc `note` → sai: `400 COST_INVALID`.
- `GET .../chargeable-students` trả các sinh viên có phân phòng `STAY` trong phòng **tại thời điểm tạo ticket** (`[{ student, contractId, contractStatus }]`); màn kết luận hiện danh sách này dạng checkbox, mặc định chọn hết.
- `chargedStudentIds` (chỉ với `MISUSE`): bỏ trống = chọn tất cả ứng viên. Phải là tập con **khác rỗng** của ứng viên → nếu không: `400 CHARGED_STUDENTS_INVALID`, `details: { invalidIds }`. Bỏ bớt ứng viên thì `note` bắt buộc ghi lý do → thiếu: `400 VALIDATION_FAILED` (`field = note`).
- `MISUSE`: cùng transaction ghi `DEDUCTION` chia đều cho các sinh viên được chọn; phần lẻ cộng vào người cuối để tổng khớp `repairCost`; hạn bù = hôm nay + 7 ngày. Danh sách được chọn và lý do lưu vào `status_history` của ticket để truy vết. 200 trả chi tiết ticket kèm:

```json
{ "deductions": [ { "contractId": 377, "student": { "id": 501, "studentCode": "2251060001", "fullName": "Nguyễn Văn A", "gender": "M" }, "amount": 37500, "topUpDueDate": "2026-10-17" } ] }
```

- Không có sinh viên nào ở tại thời điểm tạo ticket → `409 NO_OCCUPANT_TO_CHARGE`.

### 4.13. Nhập chỉ số điện nước

`PUT /api/utility-readings`

```json
{
  "billingMonth": "2026-10",
  "readings": [
    { "roomId": 210, "meterType": "ELECTRIC", "endReading": 1532 },
    { "roomId": 210, "meterType": "WATER", "endReading": 248 }
  ]
}
```

- Tạo hoặc sửa từng dòng; `startReading` tự lấy từ `endReading` tháng trước (tháng đầu tiên: bắt buộc gửi `startReading`).
- Kiểm tra **từng dòng**, lưu các dòng hợp lệ (nhập một tòa nhiều phòng, không để một ô sai làm mất cả trang). 200 trả `{ saved: 58, errors: [ { roomId, meterType, errorCode: "READING_INVALID", details: { startReading } } ] }`.
- Dòng đã có hóa đơn → lỗi dòng `READING_LOCKED`.

### 4.14. Phát hành hóa đơn

`POST /api/invoices/room-fee/issue`

```json
{ "semesterId": 7, "buildingId": null, "noticeDate": "2026-09-05" }
```

`POST /api/invoices/utility/issue`

```json
{ "billingMonth": "2026-10", "buildingId": 1, "noticeDate": "2026-11-02" }
```

- `noticeDate` mặc định hôm nay; `dueDate = noticeDate + invoice.due_days` (7).
- Phí phòng: một hóa đơn cho mỗi sinh viên đang có phân phòng `STAY` trong học kỳ (hè: chỉ sinh viên có lựa chọn `STAY` đã duyệt), số tiền = đơn giá trên phân phòng hiệu lực tại ngày phát hành. Điện nước: một hóa đơn cho mỗi phòng có đủ hai chỉ số tháng đó, người nhận là trưởng phòng.
- **Bỏ qua** (không lỗi) đối tượng đã có hóa đơn, để chạy lại an toàn. Chạy trong một transaction (NFR-REL-02, NFR-PER-02).

```json
{ "created": 1462, "skippedExisting": 25, "skippedMissingReading": 0, "roomsMissingReading": [], "roomsWithoutLeader": [ { "id": 213, "code": "K1-A103" } ], "totalAmount": 2631600000 }
```

- `skippedMissingReading`: phòng thiếu chỉ số điện hoặc nước của tháng (chỉ với điện nước).
- Phòng thiếu chỉ số bị **bỏ qua**, không chặn cả đợt (SRS v0.6, UC-21 1a); nhập đủ rồi phát hành lại thì chỉ tạo hóa đơn cho phòng còn thiếu.
- Phòng chưa có trưởng phòng **vẫn được phát hành** (Q-API-05). Preview đã trả `roomsWithoutLeader` để Staff chỉ định trước; response phát hành trả lại danh sách này.
- Thông báo: mỗi hóa đơn điện nước tạo **một** thông báo `scope = ROOM` cho phòng đó; `v_user_notification` tự đưa đến mọi người đang ở trong phòng, kể cả trưởng phòng, không cần rẽ nhánh. Mỗi hóa đơn phí phòng tạo một thông báo `scope = USER` cho sinh viên.

### 4.15. Xác nhận thanh toán

`POST /api/invoices/{id}/confirm-payment`

```json
{ "payerUserId": 501, "payerName": null, "paymentMethod": "TRANSFER", "paidAt": "2026-10-03T15:20:00+07:00", "note": "CK Vietcombank" }
```

- Phí phòng: `payerUserId` mặc định là sinh viên của hóa đơn. Điện nước: mặc định trưởng phòng; có thể là thành viên khác trong phòng. Người ngoài: `payerUserId = null`, `payerName` bắt buộc.
- `paidAt` mặc định bây giờ, không được ở tương lai. Đã PAID → `409 INVOICE_ALREADY_PAID`.

### 4.16. Sổ cọc

`GET /api/contracts/{id}/deposit`

```json
{
  "contractId": 377,
  "required": 1000000,
  "balance": 950000,
  "topUpRequired": 50000,
  "oldestUnpaidTopUpDue": "2026-10-17",
  "topUpOverdue": false,
  "negativeBalanceDebt": 0,
  "transactions": [
    { "id": 1, "type": "DEPOSIT", "amount": 1000000, "createdAt": "2026-08-21T09:00:00+07:00", "confirmedBy": { "id": 31, "fullName": "Trần Thị B", "username": "staff.k1.b" }, "maintenanceRequest": null, "topUpDueDate": null, "note": null },
    { "id": 9, "type": "DEDUCTION", "amount": -50000, "createdAt": "2026-10-10T10:00:00+07:00", "confirmedBy": null, "maintenanceRequest": { "id": 1024, "code": "TK-1024" }, "topUpDueDate": "2026-10-17", "note": "Vỡ kính cửa sổ" }
  ]
}
```

Số tiền có dấu như trong CSDL (`DEDUCTION`, `REFUND` âm). Frontend hiển thị dấu và màu theo `type`.

### 4.17. Đăng thông báo

`POST /api/notifications`

```json
{ "scope": "FLOOR", "unitId": null, "buildingId": 1, "floor": 3, "roomId": null, "title": "Cắt nước tầng 3", "body": "Từ 8h đến 11h ngày 12/10 để sửa đường ống." }
```

| `scope` | Trường bắt buộc | Ai được đăng |
|---|---|---|
| `ALL` | — | Staff có quyền tổng hợp |
| `UNIT` | `unitId` | Staff của đơn vị |
| `BUILDING` | `buildingId` | Staff của đơn vị chứa tòa |
| `FLOOR` | `buildingId`, `floor` | như trên |
| `ROOM` | `roomId` | như trên |

`USER` chỉ do hệ thống tạo. `title` ≤ 150 ký tự, `body` ≤ 5.000 ký tự, văn bản thuần (không HTML).

Mỗi thông báo trong hộp thư (`GET /api/notifications`): `{ id, scope, title, body, source, link, createdAt, isRead }`. `source` là `MANUAL` hoặc loại sự kiện tự động (`APPLICATION_APPROVED`, `BED_HELD`, `INVOICE_ISSUED`, `DEPOSIT_DEDUCTED`, `TICKET_STATUS_CHANGED`, `CONTRACT_EXPIRING`, `TOPUP_OVERDUE`, `SUMMER_PERIOD_OPENED`). `link` là đường dẫn frontend (`/student/invoices/4410`) để bấm vào mở đúng màn. `link` **không lưu** trong CSDL: mapper ở backend suy ra từ `source_type`, `source_id` của bảng `notification` và vai trò người gọi (sinh viên và Staff mở hai màn khác nhau); thông báo do Staff đăng có `link = null`.

### 4.18. Dashboard

`GET /api/dashboard`

```json
{
  "scope": { "unitId": 1, "unitCode": "K1" },
  "beds": { "operationalCapacity": 600, "occupied": 512, "held": 18, "free": 70, "inMaintenanceRooms": 12 },
  "unpaid": { "roomFeeAmount": 54000000, "utilityAmount": 8400000, "overdueInvoiceCount": 31 },
  "deposits": { "topUpOutstandingAmount": 1250000, "overdueTopUpCount": 4 },
  "tickets": { "OPEN": 5, "ASSIGNED": 3, "IN_PROGRESS": 7, "RESOLVED": 2 },
  "contracts": { "expiring": 14, "overdueMoveOut": 2, "pendingActivation": 18 },
  "roomsWithoutLeader": 6,
  "charts": {
    "occupancyByBuilding": [ { "buildingCode": "K1", "occupied": 512, "capacity": 600 } ],
    "unpaidBySemester": [ { "semesterName": "HK1 2026-2027", "amount": 54000000 } ]
  }
}
```

`occupancy rate = occupied / operationalCapacity`; giường trong phòng bảo trì không nằm trong `operationalCapacity`, hiển thị riêng (FR-NOT-05). `charts` phục vụ FR-NOT-06 (đường cắt ✂: nếu cắt thì trả `charts: null`).

---

## 5. Danh mục `errorCode`

Câu tiếng Việt là gợi ý cho `errorMessages.ts`; `{x}` lấy từ `details`.

### 5.1. Chung

| Mã | HTTP | Gợi ý thông báo |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Lỗi gắn vào từng ô theo `fieldErrors` |
| `UNAUTHENTICATED` | 401 | Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại |
| `FORBIDDEN` | 403 | Bạn không có quyền thực hiện thao tác này |
| `NOT_FOUND` | 404 | Không tìm thấy dữ liệu hoặc bạn không có quyền xem |
| `INVALID_STATE_TRANSITION` | 409 | Thao tác không còn hợp lệ vì trạng thái đã thay đổi ({currentStatus}). Trang đã được tải lại |
| `CONCURRENT_UPDATE` | 409 | Dữ liệu vừa được người khác thay đổi. Vui lòng thử lại |
| `FILE_TOO_LARGE` | 413 | File vượt quá {maxMb} MB |
| `INTERNAL_ERROR` | 500 | Hệ thống gặp lỗi. Mã tra cứu: {requestId} |

### 5.2. Tài khoản

| Mã | HTTP | Gợi ý thông báo |
|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Tên đăng nhập hoặc mật khẩu không đúng (còn {remainingAttempts} lần thử) |
| `ACCOUNT_TEMP_LOCKED` | 403 | Tài khoản tạm khóa đến {lockedUntil} do nhập sai nhiều lần |
| `ACCOUNT_LOCKED` | 403 | Tài khoản đã bị khóa. Liên hệ quản trị viên |
| `PASSWORD_CHANGE_REQUIRED` | 403 | Bạn cần đổi mật khẩu trước khi tiếp tục |
| `WRONG_CURRENT_PASSWORD` | 400 | Mật khẩu hiện tại không đúng |
| `PASSWORD_POLICY` | 400 | Mật khẩu mới phải 8–64 ký tự, có chữ và số, khác mật khẩu cũ |
| `USERNAME_EXISTS` | 409 | Tên đăng nhập {username} đã tồn tại |
| `CROSS_UNIT_ONLY_STAFF` | 409 | Chỉ cấp quyền tổng hợp cho tài khoản Staff |
| `IMPORT_FILE_INVALID` | 400 | File không đúng mẫu. Thiếu cột: {missingColumns} |
| `CONFIG_VALUE_INVALID` | 400 | Giá trị không hợp lệ cho {key} |

### 5.3. Phòng, giường

| Mã | HTTP | Gợi ý thông báo |
|---|---|---|
| `BED_TAKEN` | 409 | Giường {bedCode} vừa được phân cho sinh viên khác. Vui lòng chọn giường khác |
| `BED_NOT_AVAILABLE` | 409 | Giường {bedCode} hoặc phòng đang không hoạt động |
| `GENDER_MISMATCH` | 409 | Phòng {roomCode} dành cho {roomGender}, không phù hợp với sinh viên |
| `ROOM_OCCUPIED` | 409 | Phòng {roomCode} còn {count} sinh viên đang ở hoặc giữ chỗ |
| `BED_OCCUPIED` | 409 | Giường {bedCode} đang có người ở hoặc giữ chỗ |
| `ROOM_CODE_EXISTS` | 409 | Mã phòng {code} đã có trong tòa này |
| `SEMESTER_OVERLAP` | 409 | Thời gian trùng với học kỳ {semesterName} |
| `LEADER_NOT_STAYING` | 409 | Sinh viên này không đang ở trong phòng {roomCode} |
| `STUDENT_ALREADY_ASSIGNED` | 409 | Sinh viên đã có chỗ ở hoặc đang giữ chỗ |

### 5.4. Đăng ký, kỳ hè

| Mã | HTTP | Gợi ý thông báo |
|---|---|---|
| `PERIOD_NOT_OPEN` | 409 | Đợt đăng ký chưa mở hoặc đã đóng |
| `UNIT_NOT_IN_PERIOD` | 400 | Đợt này không áp dụng cho đơn vị đã chọn |
| `APPLICATION_EXISTS` | 409 | Bạn đã có đơn trong đợt này |
| `CONTRACT_EXISTS` | 409 | Bạn đang có hợp đồng còn hiệu lực. Hãy dùng chức năng gia hạn |
| `SUMMER_CHOICE_EXISTS` | 409 | Bạn đã gửi lựa chọn cho kỳ hè này |
| `TEMP_LEAVE_NOT_EXTENDED` | 409 | Hợp đồng cần được gia hạn sang năm học sau trước khi tạm rời hè |
| `SUMMER_CHOICE_NOT_APPROVED` | 409 | Lựa chọn tạm rời hè chưa được duyệt |

### 5.5. Hợp đồng

| Mã | HTTP | Gợi ý thông báo |
|---|---|---|
| `DEPOSIT_AMOUNT_MISMATCH` | 400 | Tiền cọc phải đúng {required} đ theo mức cọc của tòa |
| `CONTRACT_ACTIVATION_EXPIRED` | 409 | Đã quá hạn kích hoạt. Giữ chỗ sẽ bị hủy |
| `CONTRACT_AUTO_CANCELLED` | 409 | Hợp đồng đã tự động hủy do quá hạn kích hoạt |
| `CONTRACT_PERIOD_OVERLAP` | 409 | Khoảng thời gian mới trùng với hợp đồng {contractCode} |
| `REQUEST_PENDING_EXISTS` | 409 | Đã có một yêu cầu đang chờ xử lý cho hợp đồng này |
| `NOT_STAYING` | 409 | Bạn hiện không ở trong ký túc xá |
| `TRANSFER_CROSS_UNIT` | 409 | Chỉ chuyển sang giường cùng đơn vị quản lý |
| `SAME_BED` | 409 | Giường đích trùng giường hiện tại |
| `MOVE_OUT_WARNINGS_NOT_ACKNOWLEDGED` | 409 | Sinh viên còn công nợ hoặc ticket chưa kết luận chi phí. Vui lòng xác nhận trước khi trả phòng |
| `STORAGE_RECORD_EXISTS` | 409 | Đã có phiếu gửi đồ chưa nhận lại |

### 5.6. Sửa chữa

| Mã | HTTP | Gợi ý thông báo |
|---|---|---|
| `PHOTO_INVALID` | 400 | Ảnh {fileName} không hợp lệ (tối đa 3 ảnh JPEG / PNG / WebP, mỗi ảnh ≤ 5 MB) |
| `TICKET_LOCATION_INVALID` | 400 | Chọn một phòng hoặc một khu vực chung (tòa và tầng) |
| `PHOTO_UPLOAD_FAILED` | 500 | Không lưu được ảnh. Vui lòng thử lại |
| `TECHNICIAN_OUT_OF_UNIT` | 409 | Kỹ thuật viên không thuộc đơn vị của ticket |
| `TICKET_ALREADY_CONCLUDED` | 409 | Ticket đã được kết luận nguyên nhân |
| `COMMON_AREA_NO_CHARGE` | 409 | Ticket khu vực chung không tính phí cho sinh viên |
| `COST_INVALID` | 400 | Hư hỏng do sử dụng cần chi phí > 0; hao mòn tự nhiên cần ghi chú, không có chi phí |
| `NO_OCCUPANT_TO_CHARGE` | 409 | Không có sinh viên nào ở trong phòng lúc tạo ticket |
| `CHARGED_STUDENTS_INVALID` | 400 | Chọn ít nhất một sinh viên, và chỉ trong số người ở phòng lúc báo hỏng |

### 5.7. Điện nước, hóa đơn, cọc

| Mã | HTTP | Gợi ý thông báo |
|---|---|---|
| `READING_INVALID` | 400 / dòng | Chỉ số cuối ({endReading}) nhỏ hơn chỉ số đầu ({startReading}) |
| `READING_LOCKED` | 409 / dòng | Tháng {billingMonth} đã phát hành hóa đơn, không sửa chỉ số |
| `RATE_EXISTS` | 409 | Đã có đơn giá áp dụng từ ngày {effectiveFrom} |
| `DUPLICATE_INVOICE` | 409 | Hóa đơn cho {subject} kỳ này đã tồn tại |
| `INVOICE_ALREADY_PAID` | 409 | Hóa đơn đã được xác nhận thanh toán |
| `ADJUSTMENT_INVALID` | 409 | Chỉ điều chỉnh hóa đơn đã thanh toán, ghi vào hóa đơn chưa thanh toán của cùng sinh viên / phòng; tổng sau điều chỉnh không được âm ({totalAfter} đ) |
| `TOPUP_EXCEEDS_REQUIRED` | 409 | Số tiền bù vượt quá số cần bù ({topUpRequired} đ) |
| `NO_TOPUP_REQUIRED` | 409 | Hợp đồng không có khoản cần bù cọc |
| `DEPOSIT_INVALID` | 409 | Giao dịch tiền cọc không hợp lệ (bắt từ ràng buộc CSDL) |

### 5.8. Ánh xạ ràng buộc CSDL → `errorCode`

Theo `DB-Design-DMS.md` mục 4; `GlobalExceptionHandler` đọc tên ràng buộc từ `DataIntegrityViolationException`:

| Ràng buộc | `errorCode` |
|---|---|
| `ux_assignment_open_bed` | `BED_TAKEN` |
| `ux_assignment_open_student` | `STUDENT_ALREADY_ASSIGNED` |
| `ux_contract_live_student` | `CONTRACT_EXISTS` |
| `UNIQUE(student_id, period_id)` của `room_application` | `APPLICATION_EXISTS` |
| `UNIQUE(room_id, meter_type, billing_month)`, CHECK chỉ số | `READING_INVALID` |
| Hai partial unique index của `invoice` | `DUPLICATE_INVOICE` |
| CHECK / UNIQUE của `deposit_transaction` | `DEPOSIT_INVALID` |
| CHECK nguyên nhân, chi phí của `maintenance_request` | `COST_INVALID` |
| `CHECK (total_amount >= 0)` của `invoice` | `ADJUSTMENT_INVALID` |
| `EXCLUDE` của `semester` | `SEMESTER_OVERLAP` |
| Ràng buộc khác | `CONCURRENT_UPDATE` (ghi log mức WARN kèm tên ràng buộc) |

---

## 6. `allowedActions` theo đối tượng

Tên hành động = đoạn cuối URL. Policy ở backend (SAD 4.3) cài đúng bảng SRS mục 10; bảng dưới chỉ ghi ai thấy hành động nào ở trạng thái nào.

| Đối tượng | Trạng thái | Student (chủ) | Staff (cùng đơn vị) | Technician (được giao) |
|---|---|---|---|---|
| Đơn | DRAFT | `submit`, `cancel` | | |
| | SUBMITTED | `cancel` | `approve`, `reject`, `verify-priority` | |
| | APPROVED | `cancel` | `assign` | |
| | ASSIGNED | `cancel` | `release-hold` | |
| Hợp đồng | PENDING | | `activate` | |
| | ACTIVE | `create-extension-request`, `create-transfer-request` (khi đang ở, không có yêu cầu chờ) | `move-out`, `create-storage-record` (khi lựa chọn hè TEMP_LEAVE đã duyệt), `top-up` (khi cần bù) | |
| Yêu cầu gia hạn / chuyển phòng | PENDING | `cancel` | `approve`, `reject` | |
| Lựa chọn hè | PENDING | `edit` | `approve`, `reject` | |
| Ticket | OPEN | `cancel` (người tạo) | `assign`, `reject` | |
| | ASSIGNED | | | `start` |
| | IN_PROGRESS | | | `add-note`, `resolve` |
| | RESOLVED | `accept`, `reopen` (người tạo) | `accept`, `reopen` (ticket do Staff tạo, hoặc sinh viên tạo đã trả phòng) | |
| | CLOSED | | `conclude` (ticket phòng, chưa kết luận) | |
| Hóa đơn | UNPAID | | `confirm-payment`, `add-adjustment` | |
| Phiếu gửi đồ | chưa nhận lại | | `return` | |

Mã hành động không đổi trạng thái (mục 2.6) và endpoint tương ứng:

| Mã | Endpoint |
|---|---|
| `verify-priority` | `POST /api/applications/{id}/priorities/{categoryId}/verify` |
| `create-extension-request` | `POST /api/contracts/{id}/extension-requests` |
| `create-transfer-request` | `POST /api/contracts/{id}/transfer-requests` |
| `create-storage-record` | `POST /api/contracts/{id}/storage-records` |
| `top-up` | `POST /api/contracts/{id}/deposit/top-ups` |
| `edit` | `PATCH /api/summer-choices/{id}` |
| `add-note` | `POST /api/maintenance-requests/{id}/notes` |
| `add-adjustment` | `POST /api/invoices/{id}/adjustments` |

---

## 7. Định dạng file

### 7.1. File import sinh viên

Sheet đầu tiên; dòng 1 là tiêu đề, đúng tên cột (không phân biệt hoa thường, bỏ khoảng trắng hai đầu); thứ tự cột tùy ý.

| Cột | Bắt buộc | Quy tắc | Trường |
|---|---|---|---|
| `Mã SV` | Có | 6–20 ký tự chữ số; trùng trong file → lỗi cả hai dòng; đã có trong hệ thống → `EXISTS` | `studentCode`, `username` |
| `Họ tên` | Có | 2–100 ký tự | `fullName` |
| `Giới tính` | Có | `Nam` / `Nữ` (hoặc `M` / `F`) | `gender` |
| `Ngày sinh` | Có | Ô kiểu ngày hoặc `dd/MM/yyyy`; tuổi 15–60 | `dateOfBirth` |
| `Khoa` | Có | ≤ 100 ký tự | `faculty` |
| `Khóa` | Có | Dạng `K` + số (`K65`) | `cohort` |
| `Số điện thoại` | Không | 10 số, bắt đầu bằng 0 | `phone` |

Mã lỗi theo ô: `REQUIRED`, `PATTERN`, `DUPLICATE_IN_FILE`, `INVALID`. Giới hạn 5.000 dòng mỗi file.

### 7.2. File xuất công nợ

`GET /api/debts/export?kind=STUDENT&semesterId=7`: một sheet, dòng tiêu đề tiếng Việt, tiền dạng số (định dạng `#,##0`), dòng cuối là tổng. Tên file: `cong-no-sinh-vien_<mã học kỳ>_<yyyyMMdd>.xlsx` qua header `Content-Disposition`.

---

## 8. Truy vết FR → endpoint

| Nhóm FR | Endpoint chính |
|---|---|
| FR-ACC-01..03 | `/api/auth/*` |
| FR-ACC-04 | `/api/me/profile`, `PATCH /api/admin/users/{id}` |
| FR-ACC-05 | `/api/admin/student-imports` |
| FR-ACC-06..08 | `/api/admin/users/*`, `/api/students/{id}/reset-password` |
| FR-ACC-09 | `/api/admin/audit-logs` |
| FR-ROOM-01..07, 11 | `/api/admin/management-units`, `/api/admin/buildings`, `/api/room-types`, `/api/rooms`, `/api/beds`, `/api/semesters` |
| FR-ROOM-08 | `/api/me/room` |
| FR-ROOM-09, 10 | `/api/rooms/{id}/leader`, lọc `noLeader`; tự gỡ trong các nghiệp vụ đóng phân phòng |
| FR-REG-01, 13 | `/api/registration-periods` |
| FR-REG-02..07, 12 | `/api/applications/*`, `/api/me/applications` |
| FR-REG-08..11 | `/api/beds/available`, `/api/applications/{id}/assign`, `/release-hold`, `/cancel` |
| FR-REG-14, 15 | `/api/summer-choices/*` |
| FR-CON-01, 02 | `/api/contracts/{id}/activate`; tự hủy bằng job (SAD 6.4) |
| FR-CON-03..05 | `/api/contracts`, `/api/contracts/{id}`, `/api/me/contract(s)` |
| FR-CON-06 | `/api/contracts/{id}/extension-requests`, `/api/extension-requests/*` |
| FR-CON-07, 08 | `/api/contracts/{id}/transfer-requests`, `/api/transfer-requests/*` |
| FR-CON-09, 10, 13 | `/api/contracts/{id}/move-out[/preview]`, `/api/debts/*`, `/api/me/debts` |
| FR-CON-11, 12 | `/api/contracts/{id}/storage-records`, `/api/storage-records/*` |
| FR-MNT-01..10 | `/api/maintenance-requests/*`; tự đóng bằng job |
| FR-BIL-01..03 | `/api/utility-rates`, `/api/utility-readings` |
| FR-BIL-04..08, 12 | `/api/invoices/*` |
| FR-BIL-09, 10, 13 | `/api/contracts/{id}/deposit*`, `/api/me/deposit`, `/api/deposits/overdue-top-ups`; nhắc bằng job |
| FR-BIL-11 | `/api/debts/*` |
| FR-NOT-01..03 | `/api/notifications/*` |
| FR-NOT-04 | Tạo tự động từ sự kiện (SAD 4.2), đọc qua `/api/notifications` |
| FR-NOT-05, 06 | `/api/dashboard` |

---

## 9. Các câu hỏi đã chốt

Chốt ngày 10/10/2026 theo thẩm định của nhóm.

| Mã | Câu hỏi | Kết quả |
|---|---|---|
| Q-API-01 | `page` bắt đầu từ 0 hay 1? | Từ 0 (Spring `Pageable`); frontend quy đổi trong một hook phân trang dùng chung |
| Q-API-02 | Ai quản lý tham số cấu hình và diện ưu tiên? | Admin, vì dùng chung cho hai đơn vị; Staff chỉ xem |
| Q-API-03 | Đặt lại mật khẩu | Mật khẩu tạm ngẫu nhiên, trả một lần, bật `mustChangePassword` |
| Q-API-04 | Hư hỏng ghi lúc trả phòng có trừ cọc trực tiếp không? | Không. Staff tạo ticket của phòng (`roomId`), kết luận `MISUSE` trước khi trả phòng; preview trả phòng cảnh báo ticket chưa kết luận |
| Q-API-05 | Phòng chưa có trưởng phòng có phát hành hóa đơn điện nước không? | Có; một thông báo `ROOM` cho cả phòng; `roomsWithoutLeader` có ở cả preview và kết quả phát hành |
| Q-API-06 | Lưu chỉ số điện nước khi có dòng lỗi | Lưu từng dòng hợp lệ, trả danh sách dòng lỗi |
| Q-API-07 | Số chưa đọc thông báo | Hỏi lại mỗi 60 giây và khi đổi trang |

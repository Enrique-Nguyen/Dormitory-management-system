# Đặc tả API — DMS

> **Phiên bản 0.10**, 15/10/2026, Nhóm 3 — Chuyên đề CNTT, 65CNTT
> Căn cứ: `SRS-DMS.md` v0.10 (mục 3, 6.2, 10), `SAD-DMS.md` v1.3, `DB-Design-DMS.md` v0.6, `DMS-UI-UX-Design.md` (mục 27.2)
> Trạng thái: **đã chốt Q-API-01..07** (mục 9) theo thẩm định của nhóm

### Lịch sử thay đổi

| Phiên bản | Ngày | Nội dung |
|---|---|---|
| 0.1 | 10/10/2026 | Bản đầu tiên: quy ước chung, danh mục endpoint theo 7 module, schema cho các nghiệp vụ chính, danh mục `errorCode`, `allowedActions`, định dạng file import |
| 0.2 | 10/10/2026 | Theo thẩm định: Staff tạo ticket của phòng (`roomId`); xem trước trả phòng cảnh báo ticket chưa kết luận chi phí; xem trước điện nước trả phòng thiếu trưởng phòng và thiếu chỉ số; một thông báo `ROOM` cho mỗi hóa đơn điện nước; tổng hóa đơn không âm sau điều chỉnh; quy ước tên hành động không đổi trạng thái; `link` của thông báo do backend suy ra; chốt Q-API-01..07; trừ cọc chia cho người Staff chọn (`chargedStudentIds`, endpoint `chargeable-students`) |
| 0.3 | 13/10/2026 | Bổ sung GET chi tiết/DELETE danh mục và DELETE phòng/giường chưa có lịch sử cho khung CRUD; ghi rõ giới hạn triển khai và dẫn README backend |
| 0.4 | 13/10/2026 | Chốt phạm vi khung CRUD BE + React; bổ sung hồ sơ sinh viên, hợp đồng/hóa đơn chờ, yêu cầu/lựa chọn chờ, chỉ số, sửa/xóa thông báo và các ngoại lệ chỉ đọc |
| 0.5 | 13/10/2026 | Đồng bộ login/logout Spring Security, CSRF và thu hồi phiên; đổi/reset mật khẩu yêu cầu đăng nhập lại, reset Staff theo BR-07, giới hạn BCrypt và bỏ demo/mock |
| 0.6 | 14/10/2026 | Keycloak OIDC/PKCE, Bearer JWT, /me và logout thu hồi version; tạo/reset/khóa tài khoản qua Admin REST |
| 0.7 | 14/10/2026 | ADMIN toàn quyền nghiệp vụ; permissions trong /me, quyền CRUD theo role, PATCH role tài khoản và tạo đơn hộ sinh viên |
| 0.8 | 15/10/2026 | Hoàn thiện luồng chung: hồ sơ bốn cổng, lọc tài khoản managementUnitId/locked, lọc audit actorId/action/entityType/from/to và kiểm tra khoảng thời gian; giữ phạm vi khung CRUD |
| 0.9 | 15/10/2026 | GET/PUT ma trận quyền theo role; catalog có 105 quyền, kiểm giới hạn vai trò/quyền hồ sơ/READ, version và audit |
| 0.10 | 15/10/2026 | Thống nhất POST search-by-page và save-or-update theo V5; body tìm kiếm có kiểu, phân trang từ 0, ID trong body ghi phẳng, HTTP 200 cho tạo/sửa; giữ phân quyền theo nhánh, dữ liệu cá nhân, multipart, chi tiết/xóa/hành động và ma trận quyền chuyên biệt |

---

## 1. Phạm vi và cách đọc

Bổ sung triển khai 13/10/2026: khung backend trên nhánh `develop-quyennnv` có thêm GET chi tiết và DELETE cho danh mục `management-units`, `buildings`, `room-types`, `semesters`, `priority-categories`; đọc dùng `/api/<resource>/{id}`, xóa dùng đường dẫn ghi gốc kèm `/{id}` (không có hậu tố `/save-or-update`). DELETE chỉ áp dụng dữ liệu chưa bị tham chiếu; FK từ chối thì trả 409 `CONCURRENT_UPDATE`.

Có thêm DELETE `/api/rooms/{id}` và `/api/beds/{id}` cho dữ liệu chưa từng có phân phòng. Phòng/giường đã có lịch sử không được xóa, trả 409 `ROOM_OCCUPIED` hoặc `BED_OCCUPIED`. Thay đổi này phục vụ khung CRUD. Hợp đồng/hóa đơn chỉ ghi/xóa hồ sơ chờ chưa phát sinh theo mục 1.1; sổ cọc luôn chỉ đọc.

Danh sách endpoint và mức hoàn thiện thực tế của khung: [README backend](../../../DMS/backend/README.md). Các hành động nghiệp vụ còn lại trong tài liệu là hợp đồng API đích của toàn bộ MVP, không đồng nghĩa đã triển khai. Mục 1.1 và 2.5/2.9 mô tả contract CRUD hiện hành; các endpoint phân giường, kích hoạt, xét duyệt hè, tài chính và báo cáo chưa được bổ sung chỉ vì đổi quy ước API.

Tài liệu đặc tả giao diện REST giữa frontend React và backend Spring Boot. Nó là hợp đồng giữa hai phía: frontend chỉ dựa vào những gì ghi ở đây, backend không đổi tên trường, mã lỗi hay hành vi mà không cập nhật tài liệu.

- Mục 2: quy ước áp dụng cho **mọi** endpoint. Mục 3 không nhắc lại.
- Mục 3: danh mục endpoint theo module của SAD mục 4.1. Mỗi dòng ghi vai trò được gọi và FR tương ứng.
- Mục 4: schema request / response chi tiết cho các nghiệp vụ khó hoặc dùng chung.
- Mục 5: danh mục `errorCode`. Mục 6: `allowedActions` theo đối tượng.
- Mục 7: định dạng file Excel import / export. Mục 8: truy vết FR → endpoint.

Ký hiệu vai trò: **S** Student, **St** Staff, **T** Technician, **A** Admin, **\*** mọi người đã đăng nhập.

Sau khi bản này được duyệt, backend sinh tài liệu OpenAPI tự động bằng springdoc (`/v3/api-docs`, chỉ bật ở profile `dev`). Khi hai nguồn lệch nhau, **tài liệu này là chuẩn** và phải sửa code hoặc sửa tài liệu cho khớp.

---


### 1.1. Khung CRUD và contract thống nhất (15/10/2026)

Phạm vi vẫn là khung CRUD và React đơn giản. Từ phiên bản 0.10, mọi API nạp danh sách của khung, kể cả danh mục và lookup cho ô chọn, dùng `POST <readBase>/search-by-page` với body tìm kiếm; trả `PageResponse`. API tạo và sửa dùng chung `POST <writeBase>/save-or-update`, hàm Controller/Service là `saveOrUpdate`. Body ghi phẳng: `id` trống để tạo, có `id` để sửa. Không còn dùng GET danh sách hoặc cặp POST/PATCH tạo/sửa cũ. Chi tiết, DELETE và các hành động chuyên biệt được giữ.

- Sinh viên: danh sách qua `/api/students/search-by-page` hoặc `/api/admin/students/search-by-page`; chi tiết GET `/{id}`. Admin dùng `/api/admin/students/save-or-update` để tạo/sửa; Staff dùng `/api/students/save-or-update` để sửa trong phạm vi, không được tạo. Nhánh tạo trả mật khẩu tạm một lần; sửa trả hồ sơ. DELETE `/api/admin/students/{id}` chỉ khi chưa có dữ liệu liên quan. Reset mật khẩu vẫn là POST `/api/students/{id}/reset-password`.
- Danh mục đơn vị, tòa nhà, loại phòng, học kỳ và diện ưu tiên đều phân trang ở CSDL. `readBase` và `writeBase` có thể khác nhau: ba danh mục đơn vị/tòa/diện ưu tiên đọc ở `/api/...`, ghi ở `/api/admin/...`; xem mục 3.
- Phòng: `/api/rooms/save-or-update`; tạo tự sinh giường theo loại phòng, sửa giữ ràng buộc khi có người ở. Giường dùng `/api/beds/save-or-update` với `{id, roomId, code}`: tạo cần `roomId`, sửa chỉ đổi mã giường, không chuyển phòng nguồn.
- Đợt đăng ký: `/api/registration-periods/save-or-update`; DELETE `/{id}` chỉ khi chưa mở/chưa có đơn. Lookup đợt mở cho sinh viên là `/api/me/registration-periods/open/search-by-page`.
- Đơn đăng ký: `/api/applications/save-or-update` chỉ tạo/sửa bản DRAFT. Nộp, duyệt, từ chối và hủy giữ endpoint hành động. `/api/me/applications/search-by-page` luôn lấy sinh viên hiện tại phía server.
- Hợp đồng: `/api/contracts/save-or-update`, tạo nhận `applicationId`, `startDate`, `endDate`; chỉ tạo PENDING từ đơn APPROVED, chưa phân giường hay thu cọc. Khi sửa chỉ đổi ngày; DELETE `/{id}` chỉ khi PENDING chưa có phân phòng, tiền cọc hoặc lịch sử. Không nhận trạng thái từ client.
- Gia hạn/chuyển phòng: `/api/extension-requests/save-or-update`, `/api/transfer-requests/save-or-update`; tạo nhận `contractId` và nội dung yêu cầu, sửa giữ hợp đồng nguồn. Sinh viên sở hữu chỉ CRUD yêu cầu PENDING; Staff chỉ đọc. Nhánh `/api/me/.../search-by-page` giữ scope cá nhân. Chưa tự xét duyệt hay chuyển giường.
- Lựa chọn hè: `/api/summer-choices/save-or-update`, body có `contractId`, `semesterId`, `choice` (STAY/TEMP_LEAVE/LEAVE). Chỉ sinh viên sở hữu CRUD PENDING; sửa không đổi hợp đồng/học kỳ nguồn. Chưa cập nhật chỗ ở/hợp đồng.
- Chỉ số: `/api/utility-readings/save-or-update`, lưu một bản ghi gồm `roomId`, `meterType`, `billingMonth`, `startReading`, `endReading`. `billingMonth` là ngày đầu tháng. Chỉ số đã dùng trong hóa đơn hoặc có kỳ sau bị khóa sửa/xóa; khi sửa không đổi phòng/loại/kỳ.
- Đơn giá: `/api/utility-rates/save-or-update`; chỉ sửa/xóa bản giá tương lai đủ điều kiện theo người tạo/phạm vi. Giá đã có hiệu lực được giữ nguyên, thay giá bằng bản mới.
- Hóa đơn: `/api/invoices/save-or-update`; tạo nhận `type`, `totalAmount`, `dueDate` và `studentId`/`semesterId` cho ROOM_FEE hoặc `roomId`/`billingMonth` cho UTILITY. Chỉ tạo UNPAID, chưa có dòng chi tiết/tự tính tiền. Sửa tổng tiền/hạn đóng; DELETE `/{id}` chỉ khi chưa có dòng/lịch sử và chưa thanh toán.
- Sửa chữa: `/api/maintenance-requests/save-or-update`, giữ upload `multipart/form-data` với phần JSON `data` và phần `photos`. Tạo xử lý vị trí/ảnh; sửa ticket OPEN chỉ `category`, `severity`, `description`, giữ vị trí và ảnh đã có. DELETE `/{id}` chuyển CANCELLED và giữ lịch sử; action xử lý có sẵn giữ nguyên.
- Thông báo: danh sách quản trị là `/api/notifications/published/search-by-page`, chi tiết GET `/api/notifications/published/{id}`. `/api/notifications/save-or-update` tạo/sửa thông báo thủ công của mình; sửa nội dung giữ phạm vi. DELETE `/api/notifications/{id}` giữ nguyên. Hộp thư là `/api/notifications/search-by-page`; đánh dấu đã đọc và đếm chưa đọc là endpoint chuyên biệt.
- Hồ sơ và cấu hình chỉ sửa: GET `/api/me/profile`, POST `/api/me/profile/save-or-update`; cấu hình POST `/api/admin/configs/search-by-page`, POST `/api/admin/configs/save-or-update` với `{key,value}`, không tạo khóa mới.
- Các trang audit, tiền cọc, kho gửi đồ và phòng của tôi chỉ đọc qua `search-by-page`; `/api/me/room/search-by-page` trả trang có `content: []` nếu chưa có chỗ ở. GET `/api/contracts/{id}/deposit` vẫn là chi tiết sổ cọc. Không thêm chức năng ghi/xóa sổ tài chính, audit hay lịch sử.

Tìm kiếm và tính tổng đều áp dụng phạm vi trước khi phân trang. Ghi giữ nguyên trường không gửi/null như trước; chuỗi rỗng chỉ xóa nội dung text tùy chọn nếu DTO hỗ trợ. Field ngoài danh sách cho phép trả 400. Điều kiện đang sử dụng/hết quyền sửa vẫn trả 409 như RESOURCE_IN_USE, INVALID_STATE_TRANSITION, READING_LOCKED, READING_HAS_FOLLOWING_MONTH, RATE_ALREADY_EFFECTIVE; không xóa lịch sử để vượt kiểm tra.

ADMIN vẫn có mọi quyền đã đăng ký, nhưng chịu ràng buộc trạng thái/lịch sử/dữ liệu. Tài khoản cán bộ dùng khóa, đơn đăng ký/ticket dùng hủy. Endpoint cá nhân luôn là hồ sơ của người đăng nhập; không cho gửi ID người khác để mở rộng quyền. Hướng dẫn chạy và mức hoàn thiện: [README backend](../../../DMS/backend/README.md), [README chung](../../../README.md).

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
| Ngày | `"2026-10-10"`. Kỳ điện nước dùng ngày đầu tháng: `"2026-10-01"` (`billingMonth`) |
| Tiền | Số nguyên, đơn vị đồng: `1000000`. Không gửi số thập phân, không gửi chuỗi có dấu chấm |
| ID | `id` là số nguyên dương, nằm trong URL chi tiết/xóa/hành động hoặc trong body `save-or-update`. Mã hiển thị (`HD-2026-0012`, `TK-1024`, `HP-2026-0001`) là trường `code`, chỉ để hiển thị và tìm kiếm |
| Trường rỗng | Có trong response với giá trị `null`, không bỏ trường |
| Mã ký tự | UTF-8; tên tiếng Việt có dấu giữ nguyên |

### 2.2. Xác thực Keycloak

Frontend đăng nhập qua Keycloak `dms-web`, PKCE S256; gửi `Authorization: Bearer <access_token>` khi gọi DMS. JWT chỉ giữ trong bộ nhớ. Backend kiểm tra chữ ký, issuer, audience `dms-api` và expiry; subject phải liên kết vào DMS, role/version phải khớp. API không dùng cookie session/CSRF trong chế độ Keycloak.

GET /api/auth/me trả hồ sơ DMS. POST /api/auth/logout cần Bearer hợp lệ, thu hồi version/phiên của tài khoản và trả 204; gọi bằng token cũ trả 401. Sau đó frontend gọi logout Keycloak. Đăng nhập và đổi mật khẩu dùng endpoint/trang Keycloak, không gửi mật khẩu vào API DMS.

Tạo/reset trả mật khẩu tạm một lần; mật khẩu/hash không được ghi audit. Required action UPDATE_PASSWORD bắt buộc hoàn tất trước khi được cấp token. Login/CSRF/change-password của session cũ đã được bỏ; chỉ dùng endpoint/trang Keycloak.


### 2.3. Mã HTTP

| Mã | Khi nào | Body |
|---|---|---|
| 200 | Đọc, search-by-page, tạo hoặc sửa qua save-or-update, hành động trả đối tượng sau đổi | Đối tượng hoặc PageResponse; save-or-update không bắt buộc header Location |
| 201 | Chỉ hành động chuyên biệt đã quy định, ví dụ thêm ghi chú tiến độ; không dùng cho save-or-update | Theo schema của hành động |
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

### 2.5. Danh sách: search-by-page, sắp xếp và lọc

Request: `POST /api/applications/search-by-page`, `Content-Type: application/json`:

```json
{
  "page": 0,
  "size": 20,
  "q": "nguyen",
  "sort": ["priorityScore,desc", "submittedAt,asc"],
  "periodId": 12,
  "status": ["SUBMITTED", "APPROVED"]
}
```

`page`, `size`, `q` và các bộ lọc nằm cùng cấp trong body; không bọc thêm đối tượng `filters`. Mỗi endpoint chỉ áp dụng các bộ lọc được mô tả ở mục 3 và hỗ trợ trong Service. Mặc định gửi `{}` tương đương trang 0, 20 bản ghi, không có từ khóa/bộ lọc.

| Trường | Quy ước |
|---|---|
| `page` | Số nguyên từ **0**, mặc định 0; không đổi thành chỉ số trang từ 1 của V5 |
| `size` | Số nguyên 1–100, mặc định 20 |
| `sort` | Mảng tối đa 10 chuỗi `field,asc`/`field,desc`; chỉ dùng thuộc tính được Service cho phép |
| `q` | Từ khóa tối đa 500 ký tự; server chuẩn hóa khoảng trắng và áp dụng trước phân trang |
| Bộ lọc | Giá trị có kiểu JSON: `managementUnitId: 1`, `locked: false`, `billingMonth: "2026-10-01"`, `from: "2026-10-01T00:00:00+07:00"`; không gửi số/boolean dưới dạng chuỗi |
| `status` | Mảng chuỗi nếu màn hỗ trợ nhiều trạng thái; một giá trị dạng chuỗi cũng được bind thành mảng. Màn chỉ hỗ trợ một trạng thái dùng một phần tử |

Response HTTP 200:

```json
{
  "content": [ { "id": 12, "code": null, "status": "SUBMITTED" } ],
  "page": 0,
  "size": 20,
  "totalElements": 134,
  "totalPages": 7
}
```

- Tất cả danh mục, danh sách nghiệp vụ và lookup nạp cho ô chọn dùng cùng `PageResponse`; không còn trả mảng cho React tự phân trang/tìm kiếm. Lookup có hơn một trang phải tìm kiếm hoặc nạp thêm trang theo API.
- Phạm vi đơn vị/chủ sở hữu/người được giao áp dụng trong truy vấn dữ liệu và truy vấn đếm. Client không được gửi `mine`, owner/actor đang đăng nhập để thay danh tính. Các alias `/api/me/.../search-by-page` vẫn do server ép scope cá nhân.
- Item danh sách là DTO của resource; muốn dữ liệu chi tiết đầy đủ/`allowedActions`/timeline dùng endpoint chi tiết sẵn có. Frontend không suy ra quyền ghi chỉ từ việc đọc được item.
- `{content: [], totalElements: 0, totalPages: 0}` là danh sách rỗng hợp lệ. `/api/me/room/search-by-page` cũng theo quy ước này.
- `/api/auth/me`, `/api/me/profile`, `/api/me/contract`, `/api/contracts/{id}/deposit`, `/api/notifications/unread-count` và GET ma trận role là singleton/chi tiết; giữ endpoint chuyên biệt.

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
  - Thao tác trên tài nguyên con hoặc không đổi trạng thái (tạo yêu cầu, thêm ghi chú, xác nhận diện ưu tiên, nộp bù cọc…): dùng **mã hành động quy ước ở mục 6** (`create-extension-request` ↔ `POST /api/extension-requests/save-or-update`). Mục 6 ghi endpoint tương ứng của từng mã.
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

### 2.9. Tạo và sửa chung: save-or-update

Ví dụ `POST /api/room-types/save-or-update`:

```json
{
  "id": null,
  "name": "Phòng 6 người",
  "bedCount": 6,
  "priceMain": 2500000,
  "priceSummer": 700000,
  "active": true
}
```

Body phẳng, không bọc `data`/`payload` (ngoại lệ JSON trong multipart sửa chữa được nêu ở mục 4.11). Muốn sửa gửi cùng endpoint với `{"id": 15, "name": "Phòng 6 người có ban công"}`; Service giữ các trường không gửi và kiểm tra ràng buộc theo trạng thái hiện tại.

- Bỏ `id` hoặc gửi `id: null`: nhánh tạo, bắt buộc quyền `<RESOURCE>_CREATE` và các trường tạo mới. Có `id`: nhánh sửa, bắt buộc `<RESOURCE>_UPDATE`; ID phải dương. `id <= 0` trả 400; ID không tồn tại hoặc ngoài phạm vi trả 404 `NOT_FOUND`, không tự tạo bản ghi mới.
- Controller có thể khai `@RequirePrivilege({CREATE, UPDATE})` theo OR để vào chung endpoint; Impl phải kiểm tra đúng quyền của nhánh. Có CREATE không cho phép sửa và ngược lại. Giao dịch ghi/audit, khóa, kiểm tra phiên và ràng buộc lịch sử vẫn giữ nguyên.
- Cả tạo và sửa thành công trả **HTTP 200**. Thường trả DTO sau khi lưu; tài khoản/sinh viên giữ schema khác nhau theo nhánh: tạo trả `{userId, temporaryPassword}` một lần, sửa trả Profile/StudentMap. Không lưu mật khẩu tạm vào cache hồ sơ hoặc nhật ký.
- `readBase` và `writeBase` độc lập: ví dụ đọc `/api/buildings/search-by-page`, ghi `/api/admin/buildings/save-or-update`; không tự thêm/bỏ `/admin` phía client. Chi tiết/xóa tiếp tục dùng đường dẫn gốc quy định ở mục 3.
- `saveOrUpdate` là hàm chung trong Controller/Service; logic ở Impl, interface chỉ chữ ký và JavaDoc. Không giữ API tạo/sửa cũ làm đường vòng vượt quyền.
- Hồ sơ cá nhân và cấu hình dùng hậu tố `save-or-update` để thống nhất lưu, nhưng vẫn **chỉ sửa**: hồ sơ lấy ID hiện tại phía server; cấu hình dùng `{key,value}`, khóa phải có sẵn. Ma trận quyền có version và vai trò đích giữ GET/PUT `/api/admin/role-permissions/{role}` chuyên biệt; ADMIN luôn đủ quyền và bất biến.
- Xóa/hủy, thay trạng thái, reset mật khẩu, khóa/mở khóa, giao KTV, xác nhận thanh toán, đánh dấu đã đọc không gộp vào save-or-update. Không nhận `status`, actor hoặc trường sở hữu tùy ý để thực hiện hành động ngầm.

---

## 3. Danh mục endpoint

Các endpoint CRUD/search bên dưới theo contract 0.10. Những dòng phân giường, kích hoạt/chuyển/trả phòng, xét duyệt hè, kết luận tài chính, import/export, dashboard/công nợ vẫn là kế hoạch MVP nếu chưa nêu trong mục 1.1. Mỗi bảng tương ứng một module của SAD. "Phạm vi" nhắc lại cách lọc nếu khác quy tắc chung (Staff: đơn vị mình; Student: của chính mình).

### 3.1. `account`: xác thực, tài khoản, hồ sơ, nhật ký

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| GET | `/api/auth/csrf` | legacy-session | Chỉ có ở chế độ session cũ; không dùng với Keycloak | NFR-SEC |
| POST | `/api/auth/login` | legacy-session | Không tồn tại ở chế độ Keycloak; đăng nhập bằng OIDC tại Keycloak | FR-ACC-01, 02 |
| POST | `/api/auth/logout` | Bearer hợp lệ | Thu hồi version và phiên của tài khoản; 204. Sau đó FE logout Keycloak | FR-ACC-01 |
| GET | `/api/auth/me` | \* | Người dùng hiện tại (mục 4.1) | FR-ACC-01 |
| POST | `/api/auth/change-password` | legacy-session | Không dùng với Keycloak; React gọi action UPDATE_PASSWORD tại Keycloak | FR-ACC-03 |
| GET | `/api/me/profile` | \* | Hồ sơ của tôi (kèm `studentProfile` nếu là sinh viên) | FR-ACC-04 |
| POST | `/api/me/profile/save-or-update` | \* | Sinh viên sửa `phone`, `hometown`, `emergencyContact`; cán bộ chỉ sửa `phone`; trường ngoài DTO trả 400; chỉ sửa hồ sơ người đăng nhập, không nhận ID người khác | FR-ACC-04 |
| POST | `/api/students/search-by-page` | St | Sinh viên trong phạm vi đơn vị (BR-07), khung hỗ trợ `q`. Bộ lọc cohort/faculty/hasActiveContract thuộc kế hoạch mở rộng | UC-14 |
| GET | `/api/students/{id}` | St | Hồ sơ + chỗ ở hiện tại + danh sách hợp đồng | UC-14 |
| POST | `/api/students/{id}/reset-password` | St | Sinh viên có đơn trong đợt đang mở hoặc phân phòng hiệu lực đúng đơn vị; trả mật khẩu tạm và thu hồi phiên (mục 4.1) | FR-ACC-07 |
| POST | `/api/technicians/search-by-page` | St | KTV cùng đơn vị, kèm số ticket đang xử lý (để giao việc) | FR-MNT-03 |
| POST | `/api/admin/users/search-by-page` | A | Lọc `role`, `managementUnitId`, `locked`, `q`; kết hợp bằng AND, phân trang sau lọc | FR-ACC-06 |
| POST | `/api/admin/users/save-or-update` | A | Tạo/sửa Staff, Technician hoặc Admin bằng body phẳng `{id, username, fullName, phone, role, managementUnitId}`; `username` bắt buộc khi tạo. Tạo trả mật khẩu tạm một lần; sửa trả Profile. Đổi role/đơn vị thu hồi token cũ; hồ sơ STUDENT không được đổi sang role cán bộ | FR-ACC-06 |
| GET | `/api/admin/users/{id}` | A | Chi tiết | FR-ACC-06 |
| POST | `/api/admin/users/{id}/lock` | A | `{ reason }`; thu hồi phiên, không tự khóa tài khoản đang dùng | FR-ACC-06 |
| POST | `/api/admin/users/{id}/unlock` | A | Mở khóa, xóa đếm đăng nhập sai/khóa tạm; không phục hồi phiên cũ | FR-ACC-02, 06 |
| POST | `/api/admin/users/{id}/reset-password` | A | Như Staff nhưng cho mọi tài khoản | FR-ACC-07 |
| PUT | `/api/admin/users/{id}/cross-unit-access` | A | `{ enabled: true }`; chỉ cho Staff; giá trị thay đổi thu hồi phiên để đăng nhập lại nhận quyền mới | FR-ACC-08 |
| POST | `/api/admin/student-imports` | A | Import Excel, hai bước (mục 4.10) | FR-ACC-05 |
| GET | `/api/admin/student-imports/template` | A | Tải file mẫu `.xlsx` | FR-ACC-05 |
| POST | `/api/admin/audit-logs/search-by-page` | A | Lọc `actorId`, `action`, `entityType`, `from`, `to`, `q`; thời gian ISO có múi giờ, hai biên inclusive, từ > đến trả 400. Khung phân trang theo `id,desc` | FR-ACC-09 |
| POST | `/api/admin/configs/search-by-page` | A | Tham số cấu hình (`system_config`) | SRS 2.6 |
| POST | `/api/admin/configs/save-or-update` | A | `{ key, value }`; kiểm tra kiểu theo khóa; chỉ sửa khóa hiện có, không tạo cấu hình mới | SRS 2.6 |

Các alias sinh viên và endpoint chỉ sửa vẫn kiểm tra quyền tại Service, không suy ra quyền từ tiền tố URL.

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| POST | `/api/admin/students/search-by-page` | A | Cùng schema phân trang sinh viên; không làm mất scope của alias thường | UC-14 |
| GET | `/api/admin/students/{id}` | A | Chi tiết sinh viên | UC-14 |
| POST | `/api/admin/students/save-or-update` | A | Tạo/sửa `{id, studentCode, fullName, gender, dateOfBirth, faculty, cohort, phone, hometown, emergencyContact}`; tạo trả TemporaryPassword, sửa trả hồ sơ | FR-ACC-05, UC-14 |
| POST | `/api/students/save-or-update` | St | Chỉ sửa sinh viên trong phạm vi, bắt buộc ID và STUDENT_UPDATE; có đường dẫn này không cấp quyền STUDENT_CREATE | UC-14 |
| DELETE | `/api/admin/students/{id}` | A | Chỉ xóa hồ sơ chưa bị tham chiếu; nếu có lịch sử trả xung đột, giữ danh tính/audit | UC-14 |

### 3.2. `room`: đơn vị, tòa, loại phòng, phòng, giường, học kỳ

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| POST | `/api/management-units/search-by-page` | \* | Danh sách đơn vị (PageResponse) | FR-ROOM-01 |
| POST | `/api/admin/management-units/save-or-update` | A | Tạo/sửa bằng ID trong body. `{ id, code, name }` | FR-ROOM-01 |
| POST | `/api/buildings/search-by-page` | \* | Tòa (PageResponse); Staff chỉ thấy tòa đơn vị mình, trừ khi có quyền tổng hợp | FR-ROOM-01 |
| POST | `/api/admin/buildings/save-or-update` | A | Tạo/sửa bằng ID trong body. `{ id, code, name, managementUnitId, depositAmount }`. Đổi mức cọc chỉ áp dụng cho hợp đồng kích hoạt sau đó | FR-ROOM-01 |
| POST | `/api/room-types/search-by-page` | St | Loại phòng (PageResponse) | FR-ROOM-02 |
| POST | `/api/room-types/save-or-update` | St | Tạo/sửa bằng ID trong body. `{ id, name, bedCount, priceMain, priceSummer, areaM2, amenities, active }`. Đổi giá không ảnh hưởng phân phòng đang hiệu lực | FR-ROOM-02 |
| POST | `/api/semesters/search-by-page` | \* | Học kỳ (PageResponse), hỗ trợ `q`, `type` và sort theo trường được phép như startDate | FR-ROOM-11 |
| POST | `/api/semesters/save-or-update` | St | Tạo/sửa bằng ID trong body. `{ id, name, academicYear, type, startDate, endDate }`; chồng thời gian → `409 SEMESTER_OVERLAP` | FR-ROOM-11 |
| POST | `/api/rooms/search-by-page` | St | Lọc `buildingId`, `floor`, `roomTypeId`, `gender`, `status`, `hasFreeBed`, `noLeader`; mỗi phòng kèm số giường trống / đang giữ / đang ở (từ `v_room_occupancy`) | FR-ROOM-07, 10 |
| POST | `/api/rooms/save-or-update` | St | Tạo/sửa phòng bằng `{id, buildingId, floor, code, roomTypeId, gender}`; tạo tự sinh giường theo bedCount. Sửa chỉ các trường được phép; đổi giới tính/loại phòng khi còn phân phòng hiệu lực trả 409 ROOM_OCCUPIED | FR-ROOM-03, 04 |
| GET | `/api/rooms/{id}` | St | Chi tiết: giường, người đang ở / đang giữ từng giường, trưởng phòng, lịch sử trưởng phòng | FR-ROOM-07, 09 |
| POST | `/api/rooms/{id}/status` | St | `{ status: ACTIVE \| MAINTENANCE \| CLOSED, reason }`; còn phân phòng hiệu lực → `409 ROOM_OCCUPIED` | FR-ROOM-05, 06 |
| POST | `/api/beds/save-or-update` | St | Tạo/sửa mã giường `{id, roomId, code}`; khi tạo cần roomId, khi sửa không đổi phòng nguồn | FR-ROOM-04 |
| POST | `/api/beds/{id}/status` | St | `{ status: ACTIVE \| BROKEN \| RETIRED, reason }`; còn phân phòng hiệu lực → `409 BED_OCCUPIED` | FR-ROOM-04, 05, 06 |
| GET | `/api/beds/available` | St | Giường trống cho split view (mục 4.4). Tham số `applicationId` **hoặc** `transferRequestId` **hoặc** `storageRecordId` | FR-REG-08, FR-CON-07, 12 |
| PUT | `/api/rooms/{id}/leader` | St | `{ studentId }`; phải đang ở trong phòng (BR-11) | FR-ROOM-09 |
| DELETE | `/api/rooms/{id}/leader` | St | Gỡ trưởng phòng `{ reason }`; 204 | FR-ROOM-09 |
| POST | `/api/me/room/search-by-page` | S | Phòng, giường của tôi, bạn cùng phòng (họ tên), trưởng phòng; trang rỗng nếu không ở | FR-ROOM-08 |

Khung có DELETE phòng/giường chưa từng sử dụng; khi đã có lịch sử chỉ chuyển trạng thái CLOSED/RETIRED theo Policy. Không xóa lịch sử để cho phép xóa dữ liệu.

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| GET | `/api/management-units/{id}` | * | Chi tiết đơn vị | FR-ROOM-01 |
| DELETE | `/api/admin/management-units/{id}` | A | Chỉ xóa đơn vị chưa bị tham chiếu | FR-ROOM-01 |
| GET | `/api/buildings/{id}` | * | Chi tiết tòa trong phạm vi đọc | FR-ROOM-01 |
| DELETE | `/api/admin/buildings/{id}` | A | Chỉ xóa tòa chưa được sử dụng | FR-ROOM-01 |
| GET | `/api/room-types/{id}` | St | Chi tiết loại phòng | FR-ROOM-02 |
| DELETE | `/api/room-types/{id}` | St | Chỉ xóa loại phòng chưa bị tham chiếu | FR-ROOM-02 |
| GET | `/api/semesters/{id}` | * | Chi tiết học kỳ | FR-ROOM-11 |
| DELETE | `/api/semesters/{id}` | St | Chỉ xóa học kỳ chưa bị tham chiếu | FR-ROOM-11 |
| DELETE | `/api/rooms/{id}` | St | Chỉ xóa phòng chưa có lịch sử phân phòng | FR-ROOM-06 |
| DELETE | `/api/beds/{id}` | St | Chỉ xóa giường chưa có lịch sử phân phòng | FR-ROOM-06 |

### 3.3. `registration`: đợt đăng ký, đơn, kỳ hè

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| POST | `/api/priority-categories/search-by-page` | \* | Diện ưu tiên đang dùng (PageResponse); Staff, Admin thấy cả trọng số | FR-REG-05 |
| POST | `/api/admin/priority-categories/save-or-update` | A | Tạo/sửa bằng ID trong body. `{ id, code, name, weight, active }` | FR-REG-05 |
| POST | `/api/registration-periods/search-by-page` | St | Đợt áp dụng cho đơn vị mình; lọc `semesterId`, `kind`, `open` | FR-REG-01 |
| POST | `/api/registration-periods/save-or-update` | St | Tạo/sửa đợt bằng ID trong body (mục 4.2); đã mở chỉ được lùi closeAt. Đợt SUMMER mới tạo thông báo sau commit | FR-REG-01, 13 |
| GET | `/api/registration-periods/{id}` | St | Chi tiết + chỉ tiêu + thống kê số đơn theo trạng thái | FR-REG-01 |
| POST | `/api/me/registration-periods/open/search-by-page` | S | Đợt đang mở mà tôi được nộp, kèm `myApplicationId` nếu đã có đơn | FR-REG-02 |
| POST | `/api/applications/save-or-update` | S | Tạo/sửa đơn DRAFT bằng ID trong body (mục 4.3). ADMIN có thể tạo hộ với studentId rõ ràng; STUDENT không được chọn người sở hữu khác | FR-REG-02, 03 |
| POST | `/api/applications/{id}/submit` | S | DRAFT → SUBMITTED; tính điểm ưu tiên | FR-REG-04, 05 |
| POST | `/api/applications/{id}/cancel` | S | DRAFT / SUBMITTED / APPROVED / ASSIGNED → CANCELLED. Đang ASSIGNED thì đóng giữ chỗ và hủy hợp đồng PENDING cùng lúc | FR-REG-04, 11 |
| POST | `/api/me/applications/search-by-page` | S | Đơn của tôi, mới nhất trước | FR-REG-12 |
| POST | `/api/applications/search-by-page` | St | Đơn thuộc đơn vị nguyện vọng = đơn vị mình. Khung lọc `periodId`, `status`, `q`; sắp priorityScore/submittedAt theo whitelist. Bộ lọc loại phòng/giới tính là kế hoạch mở rộng | FR-REG-05 |
| GET | `/api/applications/{id}` | S, St | Chi tiết (mục 4.3); sinh viên thấy lý do từ chối, giường đang giữ, hạn kích hoạt | FR-REG-12 |
| POST | `/api/applications/{id}/priorities/{categoryId}/verify` | St | `{ verified: true \| false, note }`; tính lại điểm | FR-REG-06 |
| POST | `/api/applications/{id}/approve` | St | SUBMITTED → APPROVED | FR-REG-07 |
| POST | `/api/applications/{id}/reject` | St | SUBMITTED → REJECTED, `{ reason }` | FR-REG-07 |
| POST | `/api/applications/{id}/assign` | St | APPROVED → ASSIGNED: phân giường (mục 4.4) | FR-REG-08, 09, 10 |
| POST | `/api/applications/{id}/release-hold` | St | ASSIGNED → APPROVED, `{ reason }`: hủy giữ chỗ để phân lại | FR-REG-11 |
| POST | `/api/summer-choices/save-or-update` | S | Tạo/sửa hồ sơ hè PENDING: `{id, contractId, semesterId, choice}`; sửa không đổi hợp đồng/học kỳ nguồn. Chưa tự gia hạn, trả phòng hay phân giường | FR-REG-14, FR-CON-11 |
| GET | `/api/me/summer-choice` | S | Lựa chọn hè hiện tại của tôi (`null` nếu chưa chọn) | FR-REG-14 |
| POST | `/api/summer-choices/search-by-page` | St | Khung tìm `q` trong phạm vi; lọc semesterId/choice/status là kế hoạch mở rộng | FR-REG-14 |
| GET | `/api/summer-choices/{id}` | S, St | Chi tiết | FR-REG-14 |
| POST | `/api/summer-choices/{id}/approve` | St | `STAY`: gia hạn hợp đồng qua hè (ghi như một gia hạn, `source = SUMMER_STAY`). `TEMP_LEAVE`: chỉ duyệt, việc gửi đồ làm ở `/storage-records`. `LEAVE`: chỉ duyệt, trả phòng làm ở `/move-out` | FR-REG-15, FR-CON-11, 13 |
| POST | `/api/summer-choices/{id}/reject` | St | `{ reason }` | FR-REG-14 |

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| GET | `/api/priority-categories/{id}` | * | Chi tiết diện ưu tiên theo quyền | FR-REG-05 |
| DELETE | `/api/admin/priority-categories/{id}` | A | Chỉ xóa diện chưa được dùng | FR-REG-05 |
| DELETE | `/api/registration-periods/{id}` | St | Chỉ xóa đợt chưa mở, chưa có đơn | FR-REG-01 |
| POST | `/api/me/summer-choices/search-by-page` | S | Lựa chọn hè của người đăng nhập, không nhận chủ sở hữu tùy ý | FR-REG-14 |
| DELETE | `/api/summer-choices/{id}` | S | Chỉ xóa hồ sơ PENDING đủ điều kiện | FR-REG-14 |

### 3.4. `contract`: hợp đồng, gia hạn, chuyển phòng, trả phòng, gửi đồ

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| POST | `/api/contracts/search-by-page` | St | Khung lọc `status`, `q` trong phạm vi; nhãn suy diễn/buildingId/semesterId và sort nâng cao là kế hoạch mở rộng | FR-CON-03, 04, 05 |
| GET | `/api/contracts/{id}` | S, St | Chi tiết (mục 4.5): thời hạn, cọc, nhãn, lịch sử phân phòng, yêu cầu đang chờ | FR-CON-05 |
| GET | `/api/me/contract` | S | Hợp đồng PENDING / ACTIVE hiện tại (`null` nếu không có) | FR-CON-05 |
| POST | `/api/me/contracts/search-by-page` | S | Mọi hợp đồng của tôi | FR-CON-05 |
| POST | `/api/contracts/{id}/activate` | St | PENDING → ACTIVE, thu cọc (mục 4.6) | FR-CON-01 |
| POST | `/api/extension-requests/save-or-update` | S | Tạo/sửa yêu cầu PENDING `{id, contractId, newEndDate, reason}`; ≤ 1 yêu cầu chờ mỗi hợp đồng; sửa giữ contractId nguồn | FR-CON-06 |
| POST | `/api/extension-requests/search-by-page` | St | Khung tìm `q` trong phạm vi; lọc trạng thái nâng cao thuộc kế hoạch MVP | FR-CON-06 |
| GET | `/api/extension-requests/{id}` | S, St | Chi tiết: ngày cũ / mới, hợp đồng bị chồng (nếu có) | FR-CON-06 |
| POST | `/api/extension-requests/{id}/approve` | St | Cập nhật `endDate`; chồng hợp đồng khác → `409 CONTRACT_PERIOD_OVERLAP` | FR-CON-06 |
| POST | `/api/extension-requests/{id}/reject` | St | `{ reason }` | FR-CON-06 |
| POST | `/api/extension-requests/{id}/cancel` | S | Hủy khi còn PENDING | FR-CON-06 |
| POST | `/api/transfer-requests/save-or-update` | S | Tạo/sửa yêu cầu PENDING `{id, contractId, reason}`; cần đang ở STAY; sửa giữ contractId nguồn | FR-CON-07 |
| POST | `/api/transfer-requests/search-by-page` | St | Khung tìm `q` trong phạm vi; lọc trạng thái nâng cao thuộc kế hoạch MVP | FR-CON-07 |
| GET | `/api/transfer-requests/{id}` | S, St | Chi tiết: giường hiện tại, lý do | FR-CON-07 |
| POST | `/api/transfer-requests/{id}/approve` | St | `{ targetBedId }`: chuyển phòng (mục 4.7) | FR-CON-07, 08 |
| POST | `/api/transfer-requests/{id}/reject` | St | `{ reason }` | FR-CON-07 |
| POST | `/api/transfer-requests/{id}/cancel` | S | Hủy khi còn PENDING | FR-CON-07 |
| GET | `/api/contracts/{id}/move-out/preview` | St | Xem trước trả phòng: công nợ, số dư cọc, tiền hoàn, kiểu kết thúc (mục 4.8) | FR-CON-09 |
| POST | `/api/contracts/{id}/move-out` | St | Làm thủ tục trả phòng (mục 4.8) | FR-CON-09, 10, 13 |
| POST | `/api/contracts/{id}/storage-records` | St | Tạm rời hè: ghi phiếu gửi đồ, đóng phân phòng (mục 4.9) | FR-CON-11 |
| POST | `/api/storage-records/search-by-page` | St | Tra cứu phiếu gửi đồ theo `q` và phạm vi. Tình trạng giường cũ/lọc returned thuộc kế hoạch nghiệp vụ hè | FR-CON-12 |
| POST | `/api/storage-records/{id}/return` | St | Đầu HK1: phân lại giường + ghi ngày nhận lại đồ (mục 4.9) | FR-CON-12 |

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| POST | `/api/contracts/save-or-update` | St | Hồ sơ khung PENDING; tạo `{applicationId,startDate,endDate}`, sửa `{id,startDate,endDate}` | FR-CON-01 |
| DELETE | `/api/contracts/{id}` | St | Chỉ xóa PENDING chưa có phân phòng/cọc/lịch sử | FR-CON-01 |
| POST | `/api/me/extension-requests/search-by-page` | S | Yêu cầu gia hạn của tôi | FR-CON-06 |
| POST | `/api/me/transfer-requests/search-by-page` | S | Yêu cầu chuyển phòng của tôi | FR-CON-07 |
| DELETE | `/api/extension-requests/{id}` | S | Chỉ xóa yêu cầu PENDING đủ điều kiện | FR-CON-06 |
| DELETE | `/api/transfer-requests/{id}` | S | Chỉ xóa yêu cầu PENDING đủ điều kiện | FR-CON-07 |
| POST | `/api/me/storage-records/search-by-page` | S | Phiếu gửi đồ của tôi, chỉ đọc | FR-CON-12 |

### 3.5. `maintenance`: yêu cầu sửa chữa

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| POST | `/api/maintenance-requests/save-or-update` | S, St | Tạo/sửa ticket qua multipart data/photos (mục 4.11); ID trong data. Sửa chỉ nội dung khi OPEN, giữ vị trí và ảnh | FR-MNT-01, 02 |
| POST | `/api/maintenance-requests/search-by-page` | S, St, T | Sinh viên: ticket được phép theo người tạo/phòng hiện tại. Staff: đơn vị mình. KTV: ticket được giao. Khung lọc `q`, `status`, `severity`, `category`; buildingId/scope nâng cao là kế hoạch MVP | FR-MNT-04, 08 |
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

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| DELETE | `/api/maintenance-requests/{id}` | S, St | Hủy ticket OPEN theo quyền/người tạo; giữ ảnh và lịch sử | FR-MNT-03 |

### 3.6. `billing`: điện nước, hóa đơn, tiền cọc, công nợ

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| POST | `/api/utility-rates/search-by-page` | St | Đơn giá theo thời gian; lọc `meterType` | FR-BIL-01 |
| POST | `/api/utility-rates/save-or-update` | St | Tạo/sửa đơn giá: `{id, meterType, unitPrice, effectiveFrom}`; trùng loại/ngày trả 409 RATE_EXISTS. Chỉ sửa bản tương lai đủ điều kiện; không ghi đè giá đã hiệu lực | FR-BIL-01 |
| POST | `/api/utility-readings/search-by-page` | St | Khung tra cứu `q`, `billingMonth` tùy chọn, có startReading/endReading và trạng thái đã lập hóa đơn; bảng nhập hàng loạt theo tòa thuộc kế hoạch MVP | FR-BIL-02 |
| POST | `/api/utility-readings/save-or-update` | St | Lưu một bản ghi bằng ID trong body (mục 4.13); không phải import hàng loạt. Khi sửa giữ phòng/loại/kỳ | FR-BIL-02 |
| POST | `/api/utility-readings/{id}/meter-replacement` | St | `{ oldMeterFinalReading, newMeterStartReading, reason }` | FR-BIL-03 |
| GET | `/api/invoices/room-fee/preview` | St | `semesterId`, `buildingId?`: số sinh viên sẽ phát hành, số đã có hóa đơn, tổng tiền | FR-BIL-05, 06 |
| POST | `/api/invoices/room-fee/issue` | St | Phát hành phí phòng (mục 4.14) | FR-BIL-05, 06, 07 |
| GET | `/api/invoices/utility/preview` | St | `billingMonth`, `buildingId?`: số phòng sẽ phát hành, đã phát hành, tổng tiền; `roomsMissingReading` và `roomsWithoutLeader` dạng `[{ id, code }]` để hiện cảnh báo trước khi phát hành (mục 4.14) | FR-BIL-04, 06 |
| POST | `/api/invoices/utility/issue` | St | Phát hành điện nước theo phòng (mục 4.14) | FR-BIL-04, 06, 07 |
| POST | `/api/invoices/search-by-page` | St | Khung lọc `type`, `status`, `q` trong phạm vi; bộ lọc công nợ/kỳ/đối tượng nâng cao là kế hoạch MVP | FR-BIL-08, 11 |
| GET | `/api/invoices/{id}` | S, St | Chi tiết, các dòng, `allowedActions`. Sinh viên: hóa đơn phí phòng của mình, hóa đơn điện nước của phòng mình đang ở hoặc đã ở trong tháng đó | FR-BIL-04, 10 |
| POST | `/api/me/invoices/search-by-page` | S | Hóa đơn của tôi và của phòng tôi; lọc `type`, `status` | FR-BIL-10 |
| POST | `/api/invoices/{id}/confirm-payment` | St | UNPAID → PAID (mục 4.15) | FR-BIL-08 |
| POST | `/api/invoices/{id}/adjustments` | St | Thêm dòng điều chỉnh vào hóa đơn **UNPAID** này cho một hóa đơn đã PAID trước đó của cùng sinh viên / phòng: `{ adjustsInvoiceId, amount (âm / dương, ≠ 0), reason }`. Tổng hóa đơn sau điều chỉnh phải ≥ 0 → nếu âm: `409 ADJUSTMENT_INVALID`, `details: { totalAfter }` (CSDL cũng chặn bằng `CHECK (total_amount >= 0)`) | FR-BIL-12 |
| GET | `/api/contracts/{id}/deposit` | S, St | Sổ cọc: mức cọc, số dư, số cần bù, hạn bù, quá hạn, các giao dịch (mục 4.16) | FR-BIL-09, 10 |
| GET | `/api/me/deposit` | S | Sổ cọc của hợp đồng hiện tại | FR-BIL-10 |
| POST | `/api/contracts/{id}/deposit/top-ups` | St | Xác nhận sinh viên nộp bù: `{ amount, paymentMethod, note }`; `amount` ≤ số cần bù → nếu lớn hơn: `409 TOPUP_EXCEEDS_REQUIRED` | FR-BIL-09 |
| POST | `/api/deposits/overdue-top-ups/search-by-page` | St | Danh sách cần nhắc: hợp đồng có khoản bù quá hạn | FR-BIL-13 |
| POST | `/api/debts/students/search-by-page` | St | Công nợ theo sinh viên: phí phòng chưa đóng, bù cọc (khi ACTIVE), số dư cọc âm sau trả phòng. Lọc `semesterId`, `buildingId`, `contractStatus` | FR-BIL-11, FR-CON-10 |
| POST | `/api/debts/rooms/search-by-page` | St | Công nợ điện nước theo phòng; lọc `billingMonth`, `buildingId` | FR-BIL-11 |
| GET | `/api/debts/export` | St | Xuất `.xlsx` theo bộ lọc của hai API trên + `kind=STUDENT \| ROOM` | FR-BIL-11 |
| POST | `/api/me/debts/search-by-page` | S | Công nợ của tôi (kể cả sau khi đã trả phòng) | FR-CON-10 |

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| GET | `/api/utility-readings/{id}` | St | Chi tiết chỉ số trong phạm vi | FR-BIL-02 |
| DELETE | `/api/utility-readings/{id}` | St | Không xóa chỉ số đã khóa hoặc có kỳ sau | FR-BIL-02 |
| DELETE | `/api/utility-rates/{id}` | St | Chỉ xóa bản giá tương lai đủ điều kiện | FR-BIL-01 |
| POST | `/api/invoices/save-or-update` | St | Hồ sơ khung UNPAID; tạo có loại/đối tượng/kỳ, sửa chỉ tổng tiền/hạn đóng | FR-BIL-08 |
| DELETE | `/api/invoices/{id}` | St | Chỉ xóa UNPAID chưa có dòng chi tiết/lịch sử | FR-BIL-08 |
| POST | `/api/deposits/search-by-page` | St | Tổng quan sổ cọc đã lọc phạm vi, chỉ đọc | FR-BIL-09 |
| POST | `/api/me/deposits/search-by-page` | S | Sổ cọc hợp đồng của tôi, chỉ đọc | FR-BIL-10 |

### 3.7. `notification`: thông báo, dashboard, hộp việc chờ

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| POST | `/api/notifications/search-by-page` | \* | Hộp thư của tôi (`v_user_notification`), mới nhất trước; lọc `unread` | FR-NOT-03 |
| GET | `/api/notifications/unread-count` | \* | `{ count }`; frontend gọi lại mỗi 60 giây và khi đổi trang | FR-NOT-03 |
| POST | `/api/notifications/{id}/read` | \* | Đánh dấu đã đọc; 204 (gọi lại không lỗi) | FR-NOT-03 |
| POST | `/api/notifications/read-all` | \* | 204 | FR-NOT-03 |
| POST | `/api/notifications/save-or-update` | St | Tạo/sửa thông báo thủ công: `{id, scope, unitId, buildingId, floor, roomId, title, body}`. Khi sửa giữ phạm vi và chỉ cho người tạo đủ quyền; mục 4.17 | FR-NOT-01, 02 |
| POST | `/api/notifications/published/search-by-page` | St | Thông báo do Staff đơn vị mình đăng (cả loại `ROOM`) | FR-NOT-01 |
| GET | `/api/dashboard` | St | Số liệu dashboard (mục 4.18); `unitId` chỉ dùng khi có quyền tổng hợp, bỏ trống = toàn KTX | FR-NOT-05, 06 |
| POST | `/api/pending-items/search-by-page` | St | Hộp "Yêu cầu chờ xử lý" (UI mục 22A): đơn SUBMITTED, gia hạn, chuyển phòng, lựa chọn hè PENDING, hợp đồng PENDING chờ kích hoạt. Lọc `type`; trả `{ type, entityId, student, submittedAt }` | UI 22A |

| Method | Path | Vai trò | Mô tả | FR |
|---|---|---|---|---|
| GET | `/api/notifications/published/{id}` | St | Chi tiết thông báo đã đăng trong phạm vi | FR-NOT-01 |
| DELETE | `/api/notifications/{id}` | St | Chỉ xóa thông báo thủ công được phép; giữ kiểm tra người tạo/phạm vi | FR-NOT-01 |

---


## 4. Schema chi tiết

Chỉ ghi những endpoint có logic hoặc dữ liệu phức tạp. Các endpoint CRUD đơn giản dùng đúng trường của bảng tương ứng trong CSDL (đổi sang `camelCase`).

### 4.1. Người dùng hiện tại và mật khẩu tạm

**Keycloak là chế độ mặc định:** `/me` giữ schema hồ sơ bên dưới; mật khẩu tạm giữ schema `{ userId, temporaryPassword }`. JSON username/password, CSRF và change-password bên dưới là hợp đồng lịch sử v0.5, không còn endpoint tương ứng trong ứng dụng hiện tại. Đổi mật khẩu ở Keycloak không bị giới hạn 72 byte của BCrypt cũ. JWT phát hành trước thay đổi trực tiếp ngoài DMS có thể còn hiệu lực tối đa 5 phút.

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
- Request JSON thiếu/sai trường hoặc gửi thêm `role`/trường lạ trả `400 VALIDATION_FAILED`. Filter Spring Security nhận login, chạy strategy đổi session ID/xoay CSRF rồi lưu context; không nhận quyền do client tự khai báo.
- `GET /api/auth/me` trả `401 UNAUTHENTICATED` khi không có/hết phiên, `401 SESSION_REVOKED` khi phiên bản bảo mật thay đổi, `403 ACCOUNT_LOCKED` nếu tài khoản bị khóa. Sau thu hồi vật lý, lỗi có thể là `UNAUTHENTICATED`; client xử lý cả hai bằng đăng nhập lại.

`POST /api/auth/change-password`:

```json
{ "currentPassword": "********", "newPassword": "********" }
```

Thành công trả 204, thu hồi **mọi phiên kể cả phiên hiện tại**, xóa context/cookie CSRF và chuyển về đăng nhập. Mật khẩu mới có 8–64 ký tự, ít nhất một chữ ASCII và một chữ số, khác mật khẩu cũ; tổng mã hóa UTF-8 không quá 72 byte theo BCrypt. Sai mật khẩu hiện tại trả `400 WRONG_CURRENT_PASSWORD`, sai chính sách trả `400 PASSWORD_POLICY`; các lỗi này không thay đổi mật khẩu hoặc version.

`POST /api/auth/logout` không cần phiên hợp lệ nhưng luôn cần token CSRF; dùng `GET /api/auth/csrf` để lấy lại token trước logout. Spring Security dọn phiên và trả 204; việc gọi lặp không khôi phục hoặc tạo phiên đăng nhập.

Đặt lại mật khẩu (`/api/students/{id}/reset-password`, `/api/admin/users/{id}/reset-password`) và tạo tài khoản (`POST /api/admin/users/save-or-update`) trả:

```json
{ "userId": 501, "temporaryPassword": "Kt7mQ2vx" }
```

Mật khẩu tạm sinh ngẫu nhiên, **chỉ trả một lần** trong response này, không lưu dạng rõ, không ghi log; tài khoản bật `mustChangePassword`. Reset xóa khóa tạm/bộ đếm sai, thu hồi mọi phiên cũ, không tự mở khóa do Admin. Staff chỉ reset sinh viên có hồ sơ thật và có đơn trong đợt `openAt <= hiện tại <= closeAt` hoặc phân phòng `endedAt = null` tại đơn vị mình; lịch sử hết hiệu lực/quyền đọc liên đơn vị không đủ quyền reset. Ngoài phạm vi trả 404. Admin reset được mọi tài khoản; người được reset phải đăng nhập bằng mật khẩu tạm rồi đổi.

Quên mật khẩu được xử lý qua người quản lý có quyền; chưa có endpoint tự đăng ký hoặc gửi email reset. Bootstrap Admin mặc định tắt và yêu cầu mật khẩu do người triển khai cung cấp, không có tài khoản/mật khẩu demo tự tạo. Import Excel vẫn là nghiệp vụ dự kiến của FR-ACC-05, chưa nằm trong phần khung CRUD đã triển khai.

### 4.2. Tạo đợt đăng ký

`POST /api/registration-periods/save-or-update`

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

`POST /api/applications/save-or-update` (ID trống tạo mới; có ID sửa DRAFT)

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

`POST /api/maintenance-requests/save-or-update` (`multipart/form-data`)

| Phần | Nội dung |
|---|---|
| `data` | JSON (`application/json`), có `id` tùy chọn và nội dung bên dưới; không dùng JSON body thường cho endpoint upload |
| `photos` | 0–3 file JPEG / PNG / WebP, mỗi file ≤ 5 MB (SAD 9) |

```json
{ "id": null, "category": "ELECTRIC", "severity": "MEDIUM", "description": "Bóng đèn phòng tắm không sáng", "roomId": null, "buildingId": null, "floor": null }
```

- Sinh viên: không gửi `roomId` / `buildingId` / `floor`; ticket gắn với **phòng đang ở** (lấy từ phân phòng `STAY`). Không đang ở → `409 NOT_STAYING`.
- Staff: gửi **hoặc** `roomId` (ticket của phòng, có thể kết luận trừ cọc), **hoặc** `buildingId` + `floor` (khu vực chung; vị trí cụ thể ghi trong `description`). Gửi cả hai hoặc không gửi gì → `400 TICKET_LOCATION_INVALID`. Phòng / tòa phải thuộc đơn vị mình, nếu không: 404.
- Ticket do Staff tạo thì Staff nghiệm thu (FR-MNT-05). Với ticket phòng, "người ở lúc tạo ticket" (dùng để chia trừ cọc, mục 4.12) tính như ticket do sinh viên tạo.
- Lỗi ảnh: `400 PHOTO_INVALID` (`details: { reason: COUNT \| TYPE \| SIZE, fileName }`), `500 PHOTO_UPLOAD_FAILED`.
- 200 trả chi tiết ticket ở cả nhánh tạo và sửa. Muốn sửa gửi `id` trong JSON `data`; vị trí và ảnh cũ được giữ. Upload mới thuộc luồng tạo; không thay bộ ảnh khi sửa nội dung.

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

### 4.13. Lưu chỉ số điện nước theo bản ghi

`POST /api/utility-readings/save-or-update`

```json
{
  "id": null,
  "roomId": 210,
  "meterType": "ELECTRIC",
  "billingMonth": "2026-10-01",
  "startReading": 1450,
  "endReading": 1532
}
```

- Một request lưu một bản ghi trong transaction. Bỏ ID để tạo, có ID để sửa; trả 200 với bản ghi sau khi lưu. Không nhận mảng `readings` theo hợp đồng nhập hàng loạt cũ.
- `startReading` lấy từ kỳ trước khi phù hợp; kỳ đầu cần giá trị đầu. CSDL tính tiêu thụ và kiểm tra chỉ số cuối không nhỏ hơn chỉ số đầu.
- Khi sửa không đổi phòng, loại đồng hồ hoặc tháng nguồn. Chỉ số đã có hóa đơn trả 409 READING_LOCKED; có kỳ sau hoặc sai thay đồng hồ tiếp tục chịu kiểm tra riêng.
- Đổi đồng hồ là hành động chuyên biệt theo thiết kế; không dùng save-or-update để xóa ràng buộc kỳ trước/kỳ sau. Luồng nhập hàng loạt là công việc bổ sung, chưa được mở chỉ vì refactor endpoint.

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

`POST /api/notifications/save-or-update`

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

Mỗi item trong `content` của hộp thư (`POST /api/notifications/search-by-page`): `{ id, scope, title, body, source, link, createdAt, isRead }`. `source` là `MANUAL` hoặc loại sự kiện tự động (`APPLICATION_APPROVED`, `BED_HELD`, `INVOICE_ISSUED`, `DEPOSIT_DEDUCTED`, `TICKET_STATUS_CHANGED`, `CONTRACT_EXPIRING`, `TOPUP_OVERDUE`, `SUMMER_PERIOD_OPENED`). `link` là đường dẫn frontend (`/student/invoices/4410`) để bấm vào mở đúng màn. `link` **không lưu** trong CSDL: mapper ở backend suy ra từ `source_type`, `source_id` của bảng `notification` và vai trò người gọi (sinh viên và Staff mở hai màn khác nhau); thông báo do Staff đăng có `link = null`.

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
| `SESSION_REVOKED` | 401 | Phiên đã bị thu hồi do thay đổi mật khẩu hoặc quyền. Vui lòng đăng nhập lại |
| `CSRF_TOKEN_INVALID` | 403 | Phiên chống CSRF không hợp lệ. Tải lại trang và thực hiện lại thao tác |
| `FORBIDDEN` | 403 | Bạn không có quyền thực hiện thao tác này |
| `NOT_FOUND` | 404 | Không tìm thấy dữ liệu hoặc bạn không có quyền xem |
| `INVALID_STATE_TRANSITION` | 409 | Thao tác không còn hợp lệ vì trạng thái đã thay đổi ({currentStatus}). Trang đã được tải lại |
| `CONCURRENT_UPDATE` | 409 | Dữ liệu vừa được người khác thay đổi. Vui lòng thử lại |
| `FILE_TOO_LARGE` | 413 | File vượt quá {maxMb} MB |
| `INTERNAL_ERROR` | 500 | Hệ thống gặp lỗi. Mã tra cứu: {requestId} |

### 5.2. Tài khoản

Mã bổ sung cho Keycloak:
- `IDENTITY_NOT_LINKED` (403 khi đăng nhập, 409 khi quản lý): danh tính chưa có liên kết DMS.
- `IDENTITY_UNAVAILABLE` (503): Keycloak/Admin REST chưa sẵn sàng; không tự lặp thao tác ghi.
- `IDENTITY_NOT_CONFIGURED` (503): thiếu secret service account backend.
- `DUPLICATE_USERNAME` (409): username đã tồn tại trên Keycloak.


| Mã | HTTP | Gợi ý thông báo |
|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Tên đăng nhập hoặc mật khẩu không đúng (còn {remainingAttempts} lần thử) |
| `ACCOUNT_TEMP_LOCKED` | 403 | Tài khoản tạm khóa đến {lockedUntil} do nhập sai nhiều lần |
| `ACCOUNT_LOCKED` | 403 | Tài khoản đã bị khóa. Liên hệ quản trị viên |
| `PASSWORD_CHANGE_REQUIRED` | 403 | Bạn cần đổi mật khẩu trước khi tiếp tục |
| `WRONG_CURRENT_PASSWORD` | 400 | Mật khẩu hiện tại không đúng |
| `PASSWORD_POLICY` | 400 | Mật khẩu mới phải 8–64 ký tự, có chữ và số, khác mật khẩu cũ và không quá 72 byte UTF-8 |
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
| `create-extension-request` | `POST /api/extension-requests/save-or-update` |
| `create-transfer-request` | `POST /api/transfer-requests/save-or-update` |
| `create-storage-record` | `POST /api/contracts/{id}/storage-records` |
| `top-up` | `POST /api/contracts/{id}/deposit/top-ups` |
| `edit` | `POST /api/summer-choices/save-or-update` |
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
| FR-ACC-04 | `/api/me/profile`, `POST /api/admin/users/save-or-update` |
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


Ghi chú dọn cấu trúc 14/10/2026: chỉ giữ Keycloak; dùng lại AuthController, SecurityConfig, BootstrapAdmin và AuthService/Impl. Không tạo lớp legacy hay Service chuyển đổi riêng. Migration đã áp dụng và các kiểm thử JWT/CRUD cần thiết được giữ. Hướng dẫn chạy gom vào README gốc.

## Phân quyền Role → thao tác (cập nhật 14/10/2026)

Theo quyết định mới của người phụ trách, ADMIN có mọi quyền nghiệp vụ trên toàn hệ thống. ADMIN không cần gắn management_unit_id; bỏ giới hạn đơn vị/chủ sở hữu/người được giao khi xác định quyền, nhưng vẫn phải qua trạng thái, ràng buộc dữ liệu, đối tượng sinh viên thật và lịch sử/audit. Không đổi hoặc giả vai trò/người thao tác để vượt phạm vi.

Khung tham khảo RolePrivilege của HR-V5: `ScopeGuard` kiểm tra quyền hiện hành qua `RolePermissionService`; ma trận trong code chỉ xác định quyền mặc định và giới hạn nghiệp vụ của từng role, mỗi mã là `<RESOURCE>_<READ|CREATE|UPDATE|DELETE>`. READ dùng cho tìm kiếm và xem chi tiết; khóa/mở khóa/reset/gán phạm vi tài khoản có mã riêng. `/api/auth/me` trả thêm `permissions: string[]`; React dùng danh sách này cho menu, route và từng nút. API khai quyền bằng @RequirePrivilege giống V5; Spring Security Method Security kiểm tra trước thân Controller. Service Impl tiếp tục kiểm quyền nội bộ, phạm vi và trạng thái. Không chỉ dựa vào việc ẩn nút.

- ADMIN: mọi quyền quản trị và nghiệp vụ ở mọi đơn vị; tài khoản và người thao tác trong audit vẫn là ADMIN thực.
- STAFF: nghiệp vụ trong đơn vị được giao; crossUnitAccess chỉ mở rộng đọc. Đơn/yêu cầu do sinh viên tạo không mặc nhiên cho Staff tạo hoặc hủy thay sinh viên; quyền sửa/xét duyệt phụ thuộc endpoint và trạng thái.
- TECHNICIAN: đọc/cập nhật tiến độ ticket được giao, không CRUD dữ liệu phòng/hợp đồng/hóa đơn.
- STUDENT: dữ liệu cá nhân và phòng đang ở; đơn/yêu cầu của mình, không sửa role hoặc ID người sở hữu.

Đổi role của tài khoản Staff/KTV/Admin qua POST `/api/admin/users/save-or-update` với `id` trong body và trường `role`, đồng bộ Keycloak và thu hồi token cũ. Staff/KTV phải có đơn vị; chuyển sang ADMIN xóa đơn vị và cờ tổng hợp; không chuyển hồ sơ STUDENT thành tài khoản cán bộ, không tự đổi role tài khoản đang sử dụng. ADMIN tạo đơn đăng ký hộ sinh viên bằng `studentId` rõ ràng; STUDENT không được gửi ID của người khác.

Cập nhật 15/10/2026: ADMIN quản lý ma trận động tại `/admin/role-permissions`, lưu quyền của STAFF/STUDENT/TECHNICIAN trong `role_permission_matrix` qua Flyway V5. ADMIN giữ mọi quyền đã đăng ký và không có bản ghi để thu hồi quyền ADMIN. Mã quyền vẫn cố định trong Constants; quyền đang được cấp đọc từ CSDL. Sổ cọc/audit vẫn giữ chỉ đọc, hồ sơ có lịch sử và trạng thái vẫn chịu ràng buộc xóa/hủy như trước.


Trong bảng endpoint, vai trò ghi ở cột là quyền của người dùng thông thường; ADMIN được dùng thêm mọi endpoint nghiệp vụ theo quy tắc mới. Endpoint cá nhân vẫn là dữ liệu của tài khoản hiện tại; thao tác hộ sinh viên dùng endpoint nghiệp vụ và ID sinh viên/hợp đồng đích rõ ràng. Các mẫu auth/session lịch sử ở mục 4.1 không được dùng cho luồng Keycloak hiện hành.

### Khai quyền API bằng @RequirePrivilege — 14/10/2026

Mọi hàm REST mapped trong ứng dụng khai `@RequirePrivilege(Permission.ROOM_CREATE)` hoặc một danh sách như `@RequirePrivilege({Permission.ROOM_TYPE_READ, Permission.ROOM_CREATE, Permission.ROOM_UPDATE})`. Nhiều mã dùng OR: có ít nhất một quyền mới được chạy API. Danh sách OR phù hợp API tra cứu cho form. Endpoint save-or-update có thể khai OR CREATE/UPDATE nhưng Impl phải chọn đúng quyền theo ID; DELETE/hành động riêng vẫn khai quyền riêng.

`common/security/RequirePrivilege.java` chỉ khai annotation; `ScopeGuard.hasDeclaredPrivilege` đọc khai báo trên hàm thật, dùng khai báo lớp nếu hàm chưa khai, và kiểm role với dữ liệu tài khoản hiện tại. `SecurityConfig` bật Spring Security Method Security và đăng ký AuthorizationManagerBeforeMethodInterceptor: thiếu annotation, mảng rỗng hoặc không có quyền thì trả 403 trước thân Controller. Chưa đăng nhập/token không hợp lệ vẫn bị filter chain trả 401. Annotation ở hàm thay thế annotation lớp; Admin không vượt qua mã quyền chưa đăng ký.

Service Impl vẫn giữ kiểm tra quyền cho lời gọi nội bộ, phạm vi đơn vị/chủ sở hữu, trạng thái và ràng buộc dữ liệu; việc thêm annotation không biến dữ liệu đang dùng thành dữ liệu được phép xóa. ADMIN tiếp tục có mọi quyền nghiệp vụ đã đăng ký trên toàn hệ thống. Frontend và `/api/auth/me.permissions` giữ nguyên mã quyền hiện có.

Ở bước bổ sung annotation chỉ thêm một file source là `RequirePrivilege.java`; không thêm migration, Aspect/Service mới hoặc dependency V5. Bộ test kiểm OR, chặn trước thân Controller, lớp/hàm, thiếu/rỗng/sai annotation và quét mọi API để không bỏ sót quyền. Backend clean verify đạt **53/53 test**.

### API ma trận quyền theo vai trò (FR-ACC-10)

- `GET /api/admin/role-permissions/{role}`: chỉ ADMIN, `ROLE_PERMISSION_READ`. Role là ADMIN/STAFF/STUDENT/TECHNICIAN. Trả `{role, roleLabel, version, readOnly, screens}`. Mỗi screen gồm `{code, label, group, privileges}`; mỗi privilege gồm `{code, action, label, granted, defaultGranted, assignable, required}`. ADMIN có readOnly=true và mọi mã quyền; quyền không phù hợp/không tồn tại không thể tick.
- `PUT /api/admin/role-permissions/{role}`: chỉ ADMIN, `ROLE_PERMISSION_UPDATE`. Body `{version: 0, permissions: ["PROFILE_READ", ...]}` là toàn bộ danh sách mới, không phải phần thay đổi. Thành công trả ma trận đã lưu và version mới. Cả việc ghi quyền và audit là một transaction; không gọi Keycloak hoặc thu hồi phiên để cập nhật quyền theo role.

`VALIDATION_FAILED` (400) kèm `details.fieldErrors` khi role/danh sách/version sai, trùng mã, mã không đăng ký, mã không phù hợp vai trò, thiếu PROFILE_READ hoặc chọn ghi mà thiếu READ của tài nguyên. `ADMIN_PERMISSIONS_IMMUTABLE` (403) khi cố sửa quyền ADMIN. `PERMISSION_MATRIX_STALE` (409) khi version cũ; tải lại rồi chọn lại trước khi lưu, không tự gửi lại bản cũ. `PERMISSION_MATRIX_UNAVAILABLE` (409) khi dòng cấu hình của role không tồn tại. Dòng thiếu/hỏng không cấp quyền mặc định khi kiểm quyền runtime.

`/api/auth/me.permissions` đọc quyền CSDL hiện hành, ADMIN luôn có toàn bộ danh mục 105 mã. Thay đổi ảnh hưởng mọi tài khoản cùng role trên cùng database DMS. Các máy dùng database riêng vẫn có ma trận riêng dù cùng dùng Keycloak; muốn đồng bộ quyền phải dùng chung backend/database.

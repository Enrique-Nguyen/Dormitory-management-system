# Tài liệu kiến trúc phần mềm (SAD) — DMS

> **Phiên bản 1.2**, 14/10/2026, Nhóm 3 — Chuyên đề CNTT, 65CNTT
> Căn cứ: `SRS-DMS.md` v0.8 (mục 2, 4, 6, 7, 10), `DB-Design-DMS.md` v0.5, `DMS-UI-UX-Design.md` (mục 27), đề xuất đề tài (mục 4, 10, 13)
> Trạng thái: bản 1.0 được duyệt ngày 10/10/2026; bản 1.1 đồng bộ cấu trúc Service/Impl và luồng xác thực đã triển khai ngày 13/10/2026.

### Lịch sử thay đổi

| Phiên bản | Ngày | Nội dung |
|---|---|---|
| 0.1 | 10/10/2026 | Bản đầu tiên: gom các quyết định kiến trúc rải rác ở SRS, đề xuất, UI và CSDL; bổ sung ranh giới module, đồng thời, bảo mật, triển khai |
| 0.2 | 10/10/2026 | Theo thẩm định: tách sự kiện trạng thái (cùng transaction) và sự kiện thông báo (sau commit); quan hệ chéo module chỉ lưu ID; job dùng `TransactionTemplate` và khóa lại từng bản ghi; lọc phạm vi danh sách ngay trong SQL; thứ tự ghi ảnh và dọn file khi rollback; chốt Q-ARC-01..06 |
| 1.0 | 10/10/2026 | Nhóm duyệt bản 0.2, không đổi nội dung |
| 1.1 | 13/10/2026 | Đồng bộ Service interface/Impl; xác thực bằng AuthenticationManager/Provider/filter Spring Security, thu hồi phiên bằng security_version và dọn sau commit; CSRF/logout, reset theo BR-07 và bỏ demo/mock |
| 1.2 | 14/10/2026 | Keycloak OIDC/PKCE và resource server JWT; subject/version/role liên kết DMS; service account quản lý tài khoản và migration V4 |

---

## 1. Giới thiệu

### 1.1. Mục đích

Tài liệu mô tả cấu trúc tổng thể của DMS: hệ thống gồm những thành phần nào, chia module ra sao, các module gọi nhau theo quy tắc nào, và các quyết định kỹ thuật dùng chung cho cả nhóm (transaction, khóa, phân quyền, xử lý lỗi, tác vụ định kỳ, triển khai). Mục tiêu là để 5 thành viên code song song mà không viết lại cùng một cơ chế hai lần và không tạo phụ thuộc vòng giữa các module.

### 1.2. Vị trí trong bộ tài liệu

| Tài liệu | Trả lời câu hỏi |
|---|---|
| SRS (01 Requirements) | Hệ thống phải làm **gì** |
| **SAD (tài liệu này)** | Hệ thống được **tổ chức** thế nào |
| Thiết kế màn hình (Screen Design) | Người dùng **thấy** gì |
| Thiết kế CSDL (Database Design) | Dữ liệu được **lưu** thế nào |
| Đặc tả API (API Specification) | Frontend và backend **trao đổi** gì |
| Thiết kế chi tiết (03 Internal Design) | Từng module được **cài đặt** thế nào (lớp, sequence) |

SAD không lặp lại nội dung của các tài liệu trên; khi cần chi tiết thì dẫn mục tương ứng.

---

## 2. Mục tiêu và ràng buộc kiến trúc

### 2.1. Ràng buộc (không được chọn khác)

| Ràng buộc | Nguồn |
|---|---|
| React SPA + Spring Boot 3 (Java 17+) + PostgreSQL 15+ | SRS 2.4, 2.5 |
| Modular monolith ba lớp, logic nghiệp vụ chỉ ở Service | SRS 2.5, NFR-MNT-01 |
| 12 tuần, 5 thành viên, mỗi người phụ trách một nhóm phân hệ | SRS 2.5, đề xuất 13.1 |
| Triển khai Docker Compose trên một máy chủ [Giả định] | SRS 2.4 |
| Keycloak là tích hợp xác thực được yêu cầu thêm; dữ liệu sinh viên vào bằng Excel | SRS 6.3 |

### 2.2. Thuộc tính chất lượng quyết định kiến trúc

Xếp theo mức ảnh hưởng tới thiết kế:

1. **Toàn vẹn dữ liệu khi đồng thời** (NFR-REL-01, 02): không phân vượt chỗ, không để dữ liệu dở dang. Quyết định ở mục 6.
2. **Phân quyền theo phạm vi** (NFR-SEC-02, BR-07): Staff chỉ thấy đơn vị mình, kiểm tra trên từng đối tượng. Quyết định ở mục 7.
3. **Dễ chia việc, dễ bảo trì** (NFR-MNT-01, 02): module độc lập, test được tầng Service. Quyết định ở mục 4, 5.
4. **Hiệu năng vừa phải** (NFR-PER-01, 02): ~1.500 sinh viên, 100 người dùng đồng thời. Một instance backend là đủ; không cần cache phân tán hay message broker.

---

## 3. Ngữ cảnh hệ thống

```text
        Sinh viên (điện thoại)    Staff (máy tính)    KTV (điện thoại)    Admin (máy tính)
                 \                     |                    |                  /
                  \____________________|____HTTPS___________|_________________/
                                       v
                       +-------------------------------+
                       |              DMS              |
                       |  (web app, một máy chủ)       |
                       +-------------------------------+
                          ^                         |
          File Excel (.xlsx) danh sách SV           |  Báo cáo công nợ (xuất file, FR-BIL-11)
          do BQL cung cấp, Admin tải lên            v
```

- Không có hệ thống bên ngoài nào gọi vào hoặc được gọi ra. Thanh toán thu ngoài hệ thống, Staff xác nhận thủ công.
- Thông báo chỉ hiển thị trong hệ thống; không gửi email, SMS, push (ngoài phạm vi MVP).

---

## 4. Góc nhìn logic: module backend

### 4.1. Danh sách module

Bảy phân hệ của SRS thành bảy module, cộng một module `common` dùng chung. Mỗi module là một package Java gốc dưới `vn.edu.tlu.dms` [Giả định tên package].

| Module (package) | Phân hệ SRS | Sở hữu bảng (ghi) | Dịch vụ cung cấp cho module khác | Người phụ trách |
|---|---|---|---|---|
| `common` | — | `system_config`, `status_history`, `audit_log` | Xử lý lỗi, `ConfigService`, `StatusHistoryService`, `AuditService`, `CodeGenerator` (`fn_next_code`), lớp cơ sở cho job định kỳ | TV1 |
| `account` | 1. Tài khoản & RBAC | `management_unit`, `app_user`, `student_profile` | `CurrentUser` (người dùng, vai trò, đơn vị, quyền tổng hợp), `ScopeGuard` | TV1 |
| `room` | 2. Phòng, Giường & Tòa | `building`, `room_type`, `room`, `bed`, `semester`, **`room_assignment_history`** | **`BedOccupancyService`** (chiếm / trả / chuyển giường), `OccupancyQuery` (ai đang ở đâu, chỗ trống), `RoomLeaderService` | TV2 |
| `registration` | 3. Đăng ký & Phân phòng | `registration_period*`, `registration_quota`, `priority_category`, `room_application`, `application_priority`, `summer_choice` | — | TV3 |
| `contract` | 4. Hợp đồng | `dormitory_contract`, `contract_extension_request`, `room_transfer_request`, `move_out_record`, `storage_record` | `ContractService` (tạo PENDING, gia hạn) | TV3 (hợp đồng, gia hạn), TV2 (chuyển phòng), TV4 (trả phòng, gửi đồ) |
| `maintenance` | 5. Yêu cầu sửa chữa | `maintenance_request`, `maintenance_photo` | — | TV4 |
| `billing` | 6. Điện nước, Hóa đơn & Tiền cọc | `utility_rate`, `utility_reading`, `invoice`, `invoice_item`, `deposit_transaction` | **`DepositService`** (thu / trừ / bù / hoàn), `DebtQuery` (công nợ của sinh viên) | TV5 |
| `notification` | 7. Thông báo & Dashboard | `notification`, `notification_read` | `NotificationService`; bộ lắng nghe sự kiện nghiệp vụ; truy vấn dashboard (chỉ đọc) | TV4 (thông báo), TV5 (dashboard) |

`room_assignment_history` thuộc `room` (không thuộc `contract`) vì mọi thao tác chiếm / trả giường đi qua một dịch vụ duy nhất của TV2 (đề xuất 13.1): khóa giường và hai partial unique index chỉ viết một lần.

### 4.2. Quy tắc phụ thuộc giữa các module

```text
                       notification (lắng nghe sự kiện, dashboard chỉ đọc)
                              ^  sự kiện từ mọi module
   registration ──> contract ──> billing ──> room ──> account ──> common
        |              |                      ^
        |              └──────────────────────┘
        └─────────────────────────────────────┘
   maintenance ──> billing, room
```

Mũi tên `A ──> B` nghĩa là A được gọi Service của B. Quy tắc:

1. **Không phụ thuộc vòng.** Thứ tự tầng từ thấp lên: `common` → `account` → `room` → `billing` → `contract` → `registration` / `maintenance` → `notification`. Module chỉ gọi module ở tầng thấp hơn.
2. **Gọi qua Service công khai, không gọi Repository của module khác** và không sửa bảng module khác sở hữu. Đọc chéo cho mục đích hiển thị được dùng view hoặc query chỉ đọc mà module sở hữu cung cấp (`OccupancyQuery`, `DebtQuery`).
3. **Chiều ngược lại dùng sự kiện** (Spring `ApplicationEvent`). Khi module tầng thấp cần báo cho tầng cao, nó phát sự kiện; module tầng cao lắng nghe. Có hai loại sự kiện:

   | Loại | Ví dụ | Cách lắng nghe | Khi listener lỗi |
   |---|---|---|---|
   | **Sự kiện trạng thái**: listener đổi trạng thái nghiệp vụ | `ContractCancelled` → hủy đơn | `@EventListener` đồng bộ, **cùng transaction** | Rollback cả nghiệp vụ (đúng ý: phải nguyên tử) |
   | **Sự kiện thông báo**: chỉ tạo hiệu ứng phụ | `ApplicationApproved`, `InvoiceIssued`, `DepositDeducted`… → tạo thông báo | `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Transactional(propagation = REQUIRES_NEW)` | Ghi log lỗi, **không** ảnh hưởng nghiệp vụ đã commit |

   Lưu ý cài đặt: ở pha `AFTER_COMMIT` transaction cũ đã kết thúc, nên listener phải mở transaction mới (`REQUIRES_NEW`), nếu không bản ghi thông báo sẽ không được lưu. Đánh đổi chấp nhận được: nếu máy chủ dừng đúng giữa commit và listener thì mất một thông báo (FR-NOT-04 là mức Should); dữ liệu nghiệp vụ không bị ảnh hưởng.
4. **Entity không đi ra khỏi module.** Service công khai nhận và trả ID hoặc DTO.
5. **Quan hệ chéo module chỉ lưu ID.** Trong một module dùng `@ManyToOne(fetch = LAZY)` bình thường; tham chiếu sang entity của module khác thì chỉ khai báo cột khóa ngoại (`private Long contractId;`), không khai báo `@ManyToOne`. Khi cần dữ liệu hiển thị ghép, Service gọi query chỉ đọc của module kia hoặc đọc view. Cách này tránh ràng buộc chặt giữa domain model của hai module và tránh `LazyInitializationException` khi map DTO. Khóa ngoại dưới CSDL vẫn giữ nguyên.

Các chỗ dễ tạo vòng và cách giải:

| Tình huống | Hướng gọi | Cách làm |
|---|---|---|
| Phân giường (FR-REG-08): tạo phân phòng giữ chỗ + hợp đồng PENDING + đơn ASSIGNED | `registration` → `contract.createPending()` → `room.occupy()` | Một `@Transactional` ở `registration` |
| Hợp đồng PENDING tự hủy (FR-CON-02) phải hủy đơn | `contract` → (sự kiện) `ContractCancelled` | `registration` lắng nghe, chuyển đơn CANCELLED |
| Kích hoạt (FR-CON-01): thu cọc | `contract` → `billing.DepositService.collect()` | |
| Trả phòng (FR-CON-09): xem công nợ, hoàn cọc | `contract` → `billing.DebtQuery`, `DepositService.refund()` | |
| Phát hành phí phòng (FR-BIL-05): cần ai đang ở, đơn giá | `billing` → `room.OccupancyQuery` | Không gọi `contract` |
| Ticket CLOSED do sử dụng (FR-MNT-10): trừ cọc | `maintenance` → `billing.DepositService.deduct()`, `room.OccupancyQuery` (ai ở lúc tạo ticket) | |
| Phân phòng của trưởng phòng đóng (FR-ROOM-10) | Nội bộ `room` | `BedOccupancyService.release()` tự gỡ trưởng phòng |
| Tạo thông báo tự động (FR-NOT-04) | Mọi module → (sự kiện thông báo, sau commit) | `notification` lắng nghe: `ApplicationApproved`, `BedHeld`, `InvoiceIssued`, `DepositDeducted`, `TicketStatusChanged`… |

### 4.3. Cấu trúc bên trong một module

```text
vn.edu.tlu.dms.<module>/
├── api/          Controller + DTO request/response (record), mapper
├── service/      Chỉ Service interface, JavaDoc tiếng Việt cho từng phương thức
│   └── impl/     *ServiceImpl implements Service; logic và transaction nằm tại đây
├── domain/       Entity JPA, enum trạng thái, sự kiện của module
├── repository/   Spring Data JPA repository, native query, projection view
└── job/          Tác vụ định kỳ của module (nếu có)
```

Controller gọi Service interface qua constructor injection. Mỗi Service có Impl tương ứng; interface không chứa logic hoặc phương thức `default` xử lý nghiệp vụ. `AuthService` / `AuthServiceImpl` quản lý hồ sơ hiện tại và thu hồi token. `IdentityAccountService` / `KeycloakIdentityAccountServiceImpl` tích hợp danh tính và mật khẩu trên Keycloak. Các luồng quản trị dùng chung hai Service này.

| Tầng | Được làm | Không được làm |
|---|---|---|
| Controller | Nhận request, validate cú pháp (`@Valid`), lấy `CurrentUser`, gọi một hàm Service, trả DTO | Có `if` nghiệp vụ; gọi Repository; mở transaction |
| Service | `@Transactional`; kiểm tra quyền + phạm vi + trạng thái; khóa; ghi lịch sử; phát sự kiện; tính `allowedActions` | Trả entity ra ngoài; phụ thuộc HTTP (`HttpServletRequest`) |
| Repository | Truy vấn, khóa (`@Lock(PESSIMISTIC_WRITE)` hoặc `FOR UPDATE` native) | Chứa quy tắc nghiệp vụ |

**Policy chuyển trạng thái:** mỗi đối tượng có trạng thái (đơn, hợp đồng, ticket, hóa đơn, yêu cầu gia hạn / chuyển phòng) có một lớp `<Entity>Policy` cài bảng chuyển trạng thái của SRS mục 10. Policy được dùng ở hai chỗ: kiểm tra khi thực hiện hành động, và tính `allowedActions` cho API chi tiết. Nhờ vậy bảng chuyển trạng thái chỉ viết một lần ở backend; frontend không viết lại (UI 27.2).

---

## 5. Góc nhìn logic: frontend

Chi tiết ở UI mục 27; phần dưới chỉ chốt cấu trúc.

```text
src/
├── app/            router, guard theo vai trò, QueryClient, ConfigProvider (theme từ design tokens)
├── services/api/   Axios instance (withCredentials, XSRF, interceptor 401), hàm gọi API theo module
├── features/<module>/   hook TanStack Query, form (React Hook Form + Zod), component riêng của nghiệp vụ
├── pages/<role>/   student / staff / technician / admin: ghép feature thành trang
├── components/     StatusBadge, Timeline, ConfirmDialog có lý do, … dùng chung
├── constants/statusConfig.ts   màu và nhãn tiếng Việt cho mọi trạng thái
└── utils/errorMessages.ts      errorCode → câu tiếng Việt
```

- Bốn cổng tách theo route gốc: `/student`, `/staff`, `/technician`, `/admin`. Guard đọc vai trò và permissions từ `GET /api/auth/me`; menu/route kiểm READ, từng nút kiểm CREATE/UPDATE/DELETE. ADMIN có toàn bộ màn nghiệp vụ trong cổng Admin.
- Mọi dữ liệu từ server qua TanStack Query; sau mỗi hành động thì invalidate query liên quan; gặp 409 thì refetch rồi báo lỗi theo `errorCode`.
- Nút hành động chỉ hiện theo `allowedActions`.

---

## 6. Góc nhìn tiến trình: transaction, khóa, đồng thời

### 6.1. Ranh giới transaction

- Mỗi request ghi = **một transaction**, mở ở hàm Service công khai được Controller gọi (`@Transactional`, propagation mặc định REQUIRED). Service của module khác được gọi bên trong tham gia cùng transaction.
- Các nghiệp vụ SRS yêu cầu nguyên tử (NFR-REL-02) đều nằm trọn trong một transaction: phân giường, hủy giữ chỗ, kích hoạt, chuyển phòng, trả phòng, tạm rời hè, phát hành hóa đơn, trừ cọc khi đóng ticket.
- Mức cô lập: **READ COMMITTED** (mặc định PostgreSQL) + khóa tường minh ở các điểm tranh chấp. Không dùng SERIALIZABLE để tránh phải viết vòng thử lại.
- Request đọc dùng `@Transactional(readOnly = true)`.

### 6.2. Điểm tranh chấp và cách khóa

| Nghiệp vụ | Khóa | Thứ tự | Lớp phòng thủ thứ hai (CSDL) |
|---|---|---|---|
| Chiếm giường (phân giường, phân lại đầu HK1) | `bed` FOR UPDATE, rồi `student_profile` FOR UPDATE | Giường trước, sinh viên sau | `ux_assignment_open_bed`, `ux_assignment_open_student` |
| Chuyển phòng | Hai `bed` FOR UPDATE | `bed_id` tăng dần (BR-05) | như trên |
| Kích hoạt, gia hạn, trả phòng | `dormitory_contract` FOR UPDATE | | `ux_contract_live_student`, `UNIQUE … REFUND` |
| Phát hành hóa đơn | Không khóa | | Hai partial unique index của `invoice` → `DUPLICATE_INVOICE` |
| Xác nhận thanh toán | `invoice` FOR UPDATE | | CHECK trạng thái PAID |
| Đổi trạng thái phòng / giường | `room` hoặc `bed` FOR UPDATE, kiểm tra không còn phân phòng hiệu lực | | |

Quy tắc chung: **khóa xong mới kiểm tra lại điều kiện**, vì điều kiện có thể đã đổi trong lúc chờ khóa. Mọi khóa lấy theo thứ tự toàn cục `bed` → `student_profile` → `dormitory_contract` → `invoice` để không gây deadlock.

### 6.3. Gửi trùng và idempotence

- Gửi lặp cùng một hành động (bấm hai lần, mạng chập chờn) không tạo dữ liệu thừa vì: (1) Policy từ chối hành động không hợp lệ ở trạng thái mới, trả 409; (2) unique index chặn bản ghi thứ hai (FR-REG-10, FR-BIL-06).
- Không dùng `Idempotency-Key` header trong MVP; hai cơ chế trên đủ cho các nghiệp vụ của SRS.

### 6.4. Tác vụ định kỳ

| Job | Module | Lịch | Việc làm | Vì sao chạy lại an toàn |
|---|---|---|---|---|
| Tự hủy hợp đồng PENDING quá hạn | `contract` | Mỗi giờ | FR-CON-02 | Chỉ chọn hợp đồng còn PENDING và quá N ngày; xử lý từng hợp đồng một transaction |
| Tự đóng ticket RESOLVED quá 3 ngày | `maintenance` | Mỗi giờ | FR-MNT-06 | Chỉ chọn ticket còn RESOLVED |
| Nhắc bù cọc quá hạn | `billing` | Mỗi ngày 08:00 | FR-BIL-13 | Không tạo lại thông báo đã có cho cùng (hợp đồng, khoản trừ) |
| Thông báo hợp đồng sắp hết hạn | `contract` | Mỗi ngày 08:00 | FR-NOT-04 | Như trên, theo (hợp đồng, mốc) |

- Dùng `@Scheduled` của Spring; một lớp cơ sở trong `common` lo việc: đăng nhập với người dùng `SYSTEM`, xử lý **từng bản ghi trong một transaction riêng** (một bản ghi lỗi không chặn cả lô), ghi `audit_log`.
- Cách cài đặt transaction riêng: dùng `TransactionTemplate` (`transactionTemplate.executeWithoutResult(st -> processOne(id))`). **Không** gọi hàm `@Transactional(REQUIRES_NEW)` nằm trong cùng lớp với hàm `@Scheduled`: Spring proxy bỏ qua lời gọi nội bộ (self-invocation) nên annotation không có tác dụng.
- Truy vấn đầu tiên chỉ lấy **danh sách ID** ứng viên. Trong transaction của từng bản ghi, khóa bản ghi (`FOR UPDATE`) rồi **kiểm tra lại điều kiện** (còn PENDING? còn quá hạn?), vì Staff có thể đã kích hoạt hoặc xử lý trong lúc job đang chạy. Không thỏa thì bỏ qua.
- Chỉ chạy **một instance** backend nên không cần khóa phân tán. Nếu sau này chạy nhiều instance thì thêm ShedLock.
- Nhãn suy ra ("Sắp hết hạn", "Quá hạn", "Quá hạn bù cọc") **không cần job**: luôn tính khi đọc từ view (BR-08).

---

## 7. Bảo mật

### 7.1. Xác thực và phiên

- Keycloak quản lý đăng nhập, mật khẩu, required action và SSO; React dùng public client `dms-web`, Authorization Code + PKCE S256. Token chỉ giữ trong bộ nhớ của keycloak-js.
- Spring Security Resource Server kiểm tra chữ ký, issuer `/realms/dms`, audience `dms-api`, hạn token. Bearer gửi trong Authorization header; API không xác thực bằng cookie và không dùng CSRF của session. Bảo vệ state/nonce/PKCE của OIDC do adapter thực hiện.
- `CurrentUserImpl` ánh xạ `sub` duy nhất vào `app_user.keycloak_user_id`; không tự ghép theo username. Role token phải khớp role DMS; đơn vị/quyền liên đơn vị đọc từ CSDL. Khóa, subject không liên kết hoặc version cũ bị chặn.
- V4 thêm keycloak_user_id và unique index; password_hash nullable, bỏ hash local khi chuyển. V1–V3 không sửa. Bảng session cũ chỉ giữ do migration đã áp dụng; ứng dụng không dùng Spring Session.
- IdentityAccountService/Impl gọi Admin REST bằng client credentials của `dms-admin`; secret chỉ thuộc backend. Thuộc tính version chỉ Admin được sửa, mapper đưa dms_security_version vào JWT.
- Khóa/reset/đổi thông tin phạm vi/logout từ DMS tăng version và logout Keycloak. Logout DMS thu hồi mọi phiên của tài khoản. Mở khóa/reset xóa trạng thái brute-force tại Keycloak. Required action UPDATE_PASSWORD hoàn tất trước khi cấp token.
- Access token sống 5 phút; SSO idle 30 phút. Thay đổi mật khẩu hoặc logout trực tiếp ngoài DMS có thể để token JWT còn hiệu lực đến hết TTL; chưa có introspection/listener. Không đánh đồng logout refresh session với thu hồi tức thời mọi JWT.
- Khi create/sync rollback thì có thao tác bù; xóa danh tính sau commit, khóa trước khi xóa. HTTP và DB không có atomic transaction chung; lỗi bù/xóa ghi log để đối soát, chưa có outbox retry bền vững. Reset không thể phục hồi mật khẩu cũ nếu commit DMS thất bại sau khi Keycloak đã đổi mật khẩu.
- Chỉ có một luồng Keycloak; các Config/Controller/bootstrap song song, session cũ và BCrypt đã được loại khỏi ứng dụng.


### 7.2. Phân quyền ba lớp

| Lớp | Kiểm tra | Ở đâu | Khi sai |
|---|---|---|---|
| 1. Vai trò theo URL | Nhóm endpoint của vai trò nào | `SecurityFilterChain` (ví dụ `/api/admin/**` chỉ ADMIN) và `@PreAuthorize` | 403 |
| 2. Phạm vi đối tượng | Đối tượng thuộc đơn vị của Staff / KTV; sinh viên chỉ thấy đối tượng của mình | Service, qua `ScopeGuard` của `account` | **404** (không lộ sự tồn tại, SRS 6.2) |
| 3. Trạng thái | Hành động hợp lệ ở trạng thái hiện tại | Policy của đối tượng | 409 + `errorCode` |

- `ScopeGuard` dùng cho **API chi tiết và hành động trên một đối tượng** (`GET /api/contracts/{id}`, `POST …/{id}/activate`): tải đối tượng, kiểm tra phạm vi, sai thì 404.
- **API danh sách và tìm kiếm lọc phạm vi ngay trong câu truy vấn** (điều kiện `management_unit_id = :unitId` trong JPA Specification / JPQL / native query), rồi mới phân trang. Không tải dữ liệu lên rồi lọc trong Java: làm sai `Page<T>` (tổng số, số trang) và tốn bộ nhớ khi dữ liệu tăng.
- Đơn vị lọc lấy từ `CurrentUser`, **không** nhận từ tham số request. Staff có quyền tổng hợp (FR-ACC-08) bỏ điều kiện này.
- ADMIN có mọi quyền nghiệp vụ ở mọi đơn vị theo quyết định cập nhật 14/10/2026. ADMIN không gắn đơn vị; nhánh truy vấn ADMIN bỏ giới hạn đơn vị/chủ sở hữu nhưng không bỏ kiểm tra trạng thái và ràng buộc dữ liệu.

### 7.3. Bảo vệ khác

- Validate đầu vào bằng Bean Validation ở DTO; quy tắc nghiệp vụ ở Service (NFR-SEC-03). Chỉ dùng truy vấn tham số hóa (JPA, `:param`).
- React escape đầu ra mặc định; không dùng `dangerouslySetInnerHTML`.
- Mật khẩu CSDL, khóa, mật khẩu mặc định khi import: biến môi trường, file `.env` không đưa lên git (NFR-SEC-05).
- Tải file: giới hạn kích thước và kiểu (mục 9).

---

## 8. Xử lý lỗi, nhật ký, cấu hình

### 8.1. Mô hình lỗi

- Service ném `BusinessException(errorCode, details)`; một `@RestControllerAdvice` chuyển thành JSON `{ errorCode, message, details }` với mã HTTP theo SRS 6.2 (400, 401, 403, 404, 409).
- Lỗi ràng buộc CSDL (`DataIntegrityViolationException`): đọc tên ràng buộc, tra bảng ánh xạ ở CSDL mục 4 (`ux_assignment_open_bed` → `BED_TAKEN`…) để trả cùng định dạng. Đây là trường hợp hai request đồng thời vượt qua kiểm tra của Service.
- Lỗi không lường trước: 500 với `errorCode = INTERNAL_ERROR` và `requestId`, không trả stack trace.
- Danh mục `errorCode` đầy đủ thuộc tài liệu Đặc tả API.

### 8.2. Lịch sử và nhật ký

- Mọi chuyển trạng thái gọi `StatusHistoryService.record(entityType, entityId, from, to, actor, reason)` trong cùng transaction (BR-09); timeline ở giao diện đọc từ `status_history`.
- Thao tác quản trị và job định kỳ ghi `audit_log` (chỉ ghi thêm).
- Log ứng dụng (SLF4J + Logback): một filter gán `requestId` (lấy từ header `X-Request-Id` hoặc sinh mới) vào MDC và header phản hồi (NFR-MNT-03). Không log mật khẩu, cookie, token.

### 8.3. Cấu hình

- Tham số nghiệp vụ (7 ngày, 15 ngày, 3 ngày, hạn đóng, trọng số…) ở bảng `system_config`, đọc qua `ConfigService` (cache trong bộ nhớ, xóa cache khi Admin sửa).
- Tham số kỹ thuật (URL CSDL, thư mục upload, cổng) ở `application.yml` + biến môi trường; hai profile `dev` và `prod`.
- Lược đồ CSDL do Flyway quản lý (`V1__init.sql` = `DB-DMS-schema.sql`); thay đổi sau đó là file migration mới, không sửa file cũ.

---

## 9. Tệp tải lên và xuất file

| Loại | Xử lý |
|---|---|
| Ảnh sự cố (FR-MNT-01) | Tối đa 3 ảnh, mỗi ảnh ≤ 5 MB, chỉ JPEG / PNG / WebP (kiểm tra magic bytes, không tin phần mở rộng). Frontend nén ảnh trước khi gửi (mục tiêu ≤ 1–2 MB). Lưu ở thư mục trên volume Docker `/data/uploads/maintenance/<ticket_id>/<uuid>.<ext>`; `maintenance_photo.file_path` lưu đường dẫn tương đối. Tải ảnh qua endpoint có kiểm tra quyền, không mở thư mục tĩnh. Thứ tự ghi: xem bên dưới bảng |
| Import Excel (FR-ACC-05) | Apache POI đọc `.xlsx` ≤ 5 MB. Kiểm tra toàn bộ trước, rồi tạo các dòng hợp lệ trong một transaction; trả báo cáo từng dòng (tạo mới / bỏ qua do trùng mã / lỗi kèm lý do). Mức Could: cho tải về file Excel kết quả có thêm cột trạng thái và lý do lỗi để BQL sửa rồi nộp lại. Định dạng cột thuộc Đặc tả API |
| Xuất báo cáo công nợ (FR-BIL-11) | Sinh `.xlsx` bằng Apache POI, trả trực tiếp trong response; không lưu trên máy chủ |

**Tạo ticket kèm ảnh.** Request dạng `multipart/form-data` gồm phần JSON (`data`) và tối đa 3 phần `photos`. Trong một transaction của Service:

1. Kiểm tra số lượng, kích thước, kiểu ảnh trước khi ghi gì (sai thì 400 `PHOTO_INVALID`).
2. Lưu ticket để có `ticket_id`.
3. Ghi file vào `/data/uploads/maintenance/<ticket_id>/`, lưu các dòng `maintenance_photo`. Ghi file lỗi thì ném `BusinessException` (`PHOTO_UPLOAD_FAILED`) để rollback toàn bộ.
4. Đăng ký `TransactionSynchronization.afterCompletion`: nếu transaction **rollback** (kể cả lỗi xảy ra sau bước 3, lúc commit) thì xóa các file vừa ghi, để không còn file mồ côi.

---

## 10. Góc nhìn triển khai

```text
                 Internet (HTTPS 443)
                        |
          +-------------v--------------+
          |  nginx                     |   TLS; phục vụ file tĩnh React (build)
          |                            |   /api/** → backend:8080
          +-------------+--------------+
                        |
          +-------------v--------------+        +---------------------------+
          |  backend (Spring Boot jar) |──JDBC──>  postgres (PostgreSQL 16)  |
          |  Java 17, 1 instance       |        |  volume: pgdata           |
          |  volume: uploads           |        +---------------------------+
          +----------------------------+
```

- Docker Compose gồm ba service: `nginx`, `backend`, `postgres`. Frontend được build thành file tĩnh và đóng vào image nginx.
- Frontend và API **cùng origin** (nginx chuyển `/api`), nên cookie phiên dùng được với `SameSite=Lax` và không cần CORS ở môi trường chạy thật. Khi phát triển, Vite dev server proxy `/api` sang `localhost:8080`.
- Hai volume cần sao lưu: `pgdata` (dữ liệu) và `uploads` (ảnh). Sao lưu hằng ngày bằng `pg_dump` + nén thư mục ảnh [Giả định].
- Health check: Spring Boot Actuator `/actuator/health` (chỉ mở trong mạng nội bộ Docker).
- Múi giờ: container và JVM đặt `Asia/Ho_Chi_Minh`; CSDL lưu `TIMESTAMPTZ`.

---

## 11. Chiến lược kiểm thử theo kiến trúc

| Mức | Công cụ | Phạm vi | Liên quan |
|---|---|---|---|
| Unit | JUnit 5, Mockito | Service và Policy của từng module; mục tiêu ≥ 60% dòng tầng Service | NFR-MNT-02 (JaCoCo) |
| Tích hợp | Spring Boot Test + **Testcontainers PostgreSQL** | Repository, ràng buộc CSDL, ánh xạ lỗi → `errorCode`, phân quyền chéo đơn vị | NFR-SEC-02 |
| Đồng thời | Testcontainers, nhiều thread gọi cùng lúc | Hai yêu cầu vào một giường; chuyển phòng chéo nhau (deadlock) | NFR-REL-01, FR-REG-09 |
| Tải | k6 hoặc JMeter | Nộp đơn + tra cứu, 100 người dùng đồng thời; phát hành phí phòng 1.500 SV | NFR-PER-01, 02 |

Dùng PostgreSQL thật (Testcontainers) thay cho H2 vì partial unique index, `EXCLUDE`, view và `FOR UPDATE` phải chạy đúng như môi trường thật.

---

## 12. Truy vết thuộc tính chất lượng → quyết định

| Yêu cầu | Quyết định | Mục |
|---|---|---|
| NFR-REL-01 Không vượt chỗ | Khóa giường + kiểm tra lại; partial unique index; ánh xạ lỗi ràng buộc | 6.2, 8.1 |
| NFR-REL-02 Nguyên tử | Một request ghi = một transaction; sự kiện đồng bộ trong transaction | 4.2, 6.1 |
| NFR-REL-03 Job an toàn | Lớp job cơ sở, mỗi bản ghi một transaction, điều kiện chọn theo trạng thái | 6.4 |
| NFR-SEC-01..05 | BCrypt, phân quyền ba lớp, cookie HttpOnly + CSRF, biến môi trường | 7 |
| NFR-PER-01, 02 | Một instance, index theo CSDL mục 6, phát hành hóa đơn bằng batch insert | 2.2, 10 |
| NFR-MNT-01 | Module theo phân hệ, quy tắc phụ thuộc, Service chỉ interface và logic ở Impl | 4 |
| NFR-MNT-03 | `requestId` trong MDC | 8.2 |
| BR-08 Không lưu dữ liệu suy ra | Nhãn tính từ view, không có job cập nhật nhãn | 6.4 |
| BR-09 Lịch sử | `StatusHistoryService` dùng chung | 8.2 |

---

## 13. Các quyết định kiến trúc (tóm tắt)

| # | Quyết định | Phương án bị loại | Lý do |
|---|---|---|---|
| AD-01 | Modular monolith, một instance | Microservice | 12 tuần, 5 người; nghiệp vụ cần transaction xuyên module |
| AD-02 | `room_assignment_history` thuộc module `room`, mọi chiếm / trả giường qua `BedOccupancyService` | Mỗi module tự ghi phân phòng | Cơ chế khóa và ràng buộc chỉ viết một lần (đề xuất 13.1) |
| AD-03 | Phụ thuộc một chiều; chiều ngược dùng sự kiện: trạng thái thì cùng transaction, thông báo thì sau commit | Gọi qua lại tự do; mọi sự kiện cùng transaction | Tránh vòng; nghiệp vụ nguyên tử nhưng lỗi thông báo không làm hỏng nghiệp vụ |
| AD-10 | Quan hệ chéo module chỉ lưu ID | `@ManyToOne` giữa entity của hai module | Giảm ràng buộc giữa module; tránh lỗi lazy loading khi map DTO |
| AD-04 | Policy chuyển trạng thái ở backend, trả `allowedActions` | Frontend tự suy nút từ trạng thái | Một nguồn sự thật (UI 27.2) |
| AD-05 | Khóa bi quan (`FOR UPDATE`) ở điểm tranh chấp, READ COMMITTED | Khóa lạc quan (`@Version`); SERIALIZABLE | Phân giường là tranh chấp thật; cần báo lỗi rõ ngay, không thử lại |
| AD-06 | Keycloak OIDC/PKCE + Bearer JWT trong bộ nhớ | Session cookie cũ đã bỏ khỏi code | Theo yêu cầu đồng bộ danh tính; kiểm tra scope DMS và giới hạn TTL khi thay đổi ngoài DMS |
| AD-07 | Ngoài phạm vi đọc trả 404 | 403 | SRS 6.2: không lộ sự tồn tại đối tượng của đơn vị khác |
| AD-08 | Ảnh lưu trên volume, đường dẫn trong CSDL | Lưu BLOB trong PostgreSQL; dịch vụ lưu trữ đám mây | Đơn giản; không tích hợp ngoài |
| AD-09 | Testcontainers PostgreSQL cho test tích hợp | H2 | H2 không có partial index, `EXCLUDE` như PostgreSQL |

---

## 14. Các câu hỏi đã chốt

Chốt ngày 10/10/2026 theo thẩm định của nhóm.

| Mã | Câu hỏi | Kết quả |
|---|---|---|
| Q-ARC-01 | Tên package gốc | `vn.edu.tlu.dms` |
| Q-ARC-02 | Dùng sự kiện cho chiều phụ thuộc ngược? | Có. Sự kiện trạng thái (hủy đơn khi hợp đồng tự hủy) chạy cùng transaction; sự kiện thông báo chạy sau commit (mục 4.2) |
| Q-ARC-03 | Giới hạn ảnh sự cố | 3 ảnh, mỗi ảnh ≤ 5 MB, JPEG / PNG / WebP; frontend nén trước khi gửi |
| Q-ARC-04 | nginx hay Spring Boot phục vụ file tĩnh? | nginx: TLS, file tĩnh React, chuyển `/api` sang backend |
| Q-ARC-05 | Lịch job nhắc bù cọc và thông báo sắp hết hạn | 08:00 hằng ngày |
| Q-ARC-06 | Import Excel khi có dòng lỗi | Tạo các dòng hợp lệ, báo cáo từng dòng lỗi; file kết quả tải về ở mức Could |


Bổ sung triển khai v1.2: môi trường dev có Keycloak và database Keycloak riêng. Compose chỉ chuẩn bị hạ tầng; Windows local có script native. Dùng chung qua mạng phải có HTTPS và cấu hình issuer/redirect/web origin theo server thực tế; xem README.md.


Ghi chú dọn cấu trúc 14/10/2026: chỉ giữ Keycloak; dùng lại AuthController, SecurityConfig, BootstrapAdmin và AuthService/Impl. Không tạo lớp legacy hay Service chuyển đổi riêng. Migration đã áp dụng và các kiểm thử JWT/CRUD cần thiết được giữ. Hướng dẫn chạy gom vào README gốc.

## Phân quyền Role → thao tác (cập nhật 14/10/2026)

Theo quyết định mới của người phụ trách, ADMIN có mọi quyền nghiệp vụ trên toàn hệ thống. ADMIN không cần gắn management_unit_id; bỏ giới hạn đơn vị/chủ sở hữu/người được giao khi xác định quyền, nhưng vẫn phải qua trạng thái, ràng buộc dữ liệu, đối tượng sinh viên thật và lịch sử/audit. Không đổi hoặc giả vai trò/người thao tác để vượt phạm vi.

Khung tham khảo RolePrivilege của HR-V5: `ScopeGuard` khai ma trận cố định cho bốn role, mỗi mã là `<RESOURCE>_<READ|CREATE|UPDATE|DELETE>`. READ dùng cho tìm kiếm và xem chi tiết; khóa/mở khóa/reset/gán phạm vi tài khoản có mã riêng. `/api/auth/me` trả thêm `permissions: string[]`; React dùng danh sách này cho menu, route và từng nút. API khai quyền bằng @RequirePrivilege giống V5; Spring Security Method Security kiểm tra trước thân Controller. Service Impl tiếp tục kiểm quyền nội bộ, phạm vi và trạng thái. Không chỉ dựa vào việc ẩn nút.

- ADMIN: mọi quyền quản trị và nghiệp vụ ở mọi đơn vị; tài khoản và người thao tác trong audit vẫn là ADMIN thực.
- STAFF: nghiệp vụ trong đơn vị được giao; crossUnitAccess chỉ mở rộng đọc. Đơn/yêu cầu do sinh viên tạo không mặc nhiên cho Staff tạo hoặc hủy thay sinh viên; quyền sửa/xét duyệt phụ thuộc endpoint và trạng thái.
- TECHNICIAN: đọc/cập nhật tiến độ ticket được giao, không CRUD dữ liệu phòng/hợp đồng/hóa đơn.
- STUDENT: dữ liệu cá nhân và phòng đang ở; đơn/yêu cầu của mình, không sửa role hoặc ID người sở hữu.

Đổi role của tài khoản Staff/KTV/Admin qua PATCH `/api/admin/users/{id}` hỗ trợ trường `role`, đồng bộ Keycloak và thu hồi token cũ. Staff/KTV phải có đơn vị; chuyển sang ADMIN xóa đơn vị và cờ tổng hợp; không chuyển hồ sơ STUDENT thành tài khoản cán bộ, không tự đổi role tài khoản đang sử dụng. ADMIN tạo đơn đăng ký hộ sinh viên bằng `studentId` rõ ràng; STUDENT không được gửi ID của người khác.

Ma trận hiện cố định trong code, chưa có bảng/màn quản trị tick quyền động như HR-V5. Không thêm migration hoặc file Service riêng cho cùng trách nhiệm. Sổ cọc/audit vẫn giữ chỉ đọc, hồ sơ có lịch sử và trạng thái vẫn chịu ràng buộc xóa/hủy như trước.

### Khai quyền API bằng @RequirePrivilege — 14/10/2026

Mọi hàm REST mapped trong ứng dụng khai `@RequirePrivilege(Permission.ROOM_CREATE)` hoặc một danh sách như `@RequirePrivilege({Permission.ROOM_TYPE_READ, Permission.ROOM_CREATE, Permission.ROOM_UPDATE})`. Nhiều mã dùng OR: có ít nhất một quyền mới được chạy API. Danh sách OR phù hợp API tra cứu cho form; thao tác ghi phải khai riêng CREATE/UPDATE/DELETE, không ghép quyền thêm/sửa để mở cả hai.

`common/security/RequirePrivilege.java` chỉ khai annotation; `ScopeGuard.hasDeclaredPrivilege` đọc khai báo trên hàm thật, dùng khai báo lớp nếu hàm chưa khai, và kiểm role với dữ liệu tài khoản hiện tại. `SecurityConfig` bật Spring Security Method Security và đăng ký AuthorizationManagerBeforeMethodInterceptor: thiếu annotation, mảng rỗng hoặc không có quyền thì trả 403 trước thân Controller. Chưa đăng nhập/token không hợp lệ vẫn bị filter chain trả 401. Annotation ở hàm thay thế annotation lớp; Admin không vượt qua mã quyền chưa đăng ký.

Service Impl vẫn giữ kiểm tra quyền cho lời gọi nội bộ, phạm vi đơn vị/chủ sở hữu, trạng thái và ràng buộc dữ liệu; việc thêm annotation không biến dữ liệu đang dùng thành dữ liệu được phép xóa. ADMIN tiếp tục có mọi quyền nghiệp vụ đã đăng ký trên toàn hệ thống. Frontend và `/api/auth/me.permissions` giữ nguyên mã quyền hiện có.

Ở bước bổ sung annotation chỉ thêm một file source là `RequirePrivilege.java`; không thêm migration, Aspect/Service mới hoặc dependency V5. Bộ test kiểm OR, chặn trước thân Controller, lớp/hàm, thiếu/rỗng/sai annotation và quét mọi API để không bỏ sót quyền. Backend clean verify đạt **53/53 test**.

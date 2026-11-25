# DMS — Manifest dữ liệu 36 bảng baseline V1–V7

> **Ticket:** DMS-DATA-01 (phần tài liệu)  
> **Baseline:** nhánh `develop-quyennnv`, SHA `266f588`, ngày 31/10/2026  
> **Trạng thái:** **DRAFT — chưa được module owner/reviewer xác nhận**  
> **Giới hạn:** tài liệu chỉ đọc từ source; không chạy ứng dụng, không kết nối DB, không kết luận runtime. Mọi dòng có trạng thái `NOT_VERIFIED` cho đến khi owner/reviewer kiểm chứng.

## 1. Nguồn và phạm vi

Nguồn đã đọc:

- Kế hoạch `Docs/00 Project Management/ke-hoach-tong-the-chuan-hoa-dms-theo-hr-v5.md`, mục 6 và 9.3 (chỉ đọc; không sửa).
- Flyway V1–V7 trong `DMS/backend/src/main/resources/db/migration/`.
- Entity, repository, service, controller trong `DMS/backend/src/main/java/vn/edu/tlu/dms/`.
- Registry giao diện `DMS/frontend/src/features/crud/resources.ts` và các trang/component được registry dẫn tới.
- `Docs/02 External Design/Screen Design/DMS-UI-UX-Design.md`.
- Test source trong `DMS/backend/src/test/java/vn/edu/tlu/dms/` (chỉ đọc tên/kịch bản, không chạy).
- `DmsApplicationTests`: kỳ vọng 38 bảng base gồm 36 bảng khai báo V1–V7, `user_preference` (V8) và bảng lịch sử Flyway.

Quy ước:

- `T01`–`T36` là mã ổn định của manifest, theo thứ tự migration V1 → V2 → V5.
- `NOT_VERIFIED`: chưa có test chạy trong lượt này và chưa được owner/reviewer xác nhận.
- `CẦN XÁC MINH`: thông tin chưa xác định được từ source hoặc source chưa đủ để kết luận.
- Cột “Test hiện có” chỉ nêu file có tham chiếu trực tiếp; **không** có nghĩa test đã chạy, đã pass hoặc phủ toàn bộ quy tắc.
- Tên bảng chỉ khai một lần tại tiêu đề mục tương ứng; các mục sau dùng mã `Txx`.

## 2. Kết quả inventory và đối chiếu với kế hoạch

### 2.1. Số lượng bảng

Kết quả đếm từ migration:

| Nguồn | Số bảng | Ghi chú |
|---|---:|---|
| V1 | 33 | Toàn bộ bảng nghiệp vụ |
| V2 | 2 | Hai bảng phiên của Spring Session JDBC |
| V5 | 1 | Ma trận quyền động |
| V3, V4, V6, V7 | 0 | Chỉ `ALTER TABLE` hoặc cập nhật dữ liệu, không tạo bảng |
| **Tổng** | **36** | Khớp tuyên bố 36 bảng của kế hoạch mục 6 |

Đối chiếu:

- Kế hoạch ghi V1: 33, V2: 2, V5: 1 — source migration cho đúng 33/2/1, tổng 36.
- `DmsApplicationTests` kỳ vọng 38 bảng base vì có thêm `user_preference` (V8, trong working tree) và bảng lịch sử migration. Đây là khác biệt có giải thích, không phải bảng nghiệp vụ thứ 37/38 của manifest V1–V7.
- V2 khai báo tên bảng bằng chữ hoa không quote; PostgreSQL chuẩn hóa về chữ thường. Manifest dùng tên vật lý chữ thường.
- 33/36 bảng baseline có entity JPA. Ba bảng không có entity: T34, T35 (platform) và T36 (đọc/ghi bằng `JdbcTemplate`).
- `resources.ts` có 29 resource definition, không tương ứng 1-1 với 36 bảng: nhiều bảng con/chứng từ không có màn CRUD riêng.

Lệnh đếm (chạy từ repo root):

```bash
grep -h -E '^CREATE TABLE' DMS/backend/src/main/resources/db/migration/V[1-7]__*.sql \
  | sed -E 's/^CREATE TABLE ([A-Za-z_]+).*/\1/' \
  | tr '[:upper:]' '[:lower:]' | sort | uniq -c
```

Kết quả: 36 dòng, mỗi dòng có count `1`; không có tên trùng. Lệnh đếm theo file:

```bash
for f in DMS/backend/src/main/resources/db/migration/V[1-7]__*.sql; do
  printf '%s %s\n' "$(basename "$f")" "$(grep -c -E '^CREATE TABLE' "$f")"
done
```

Kết quả: V1 33, V2 2, V5 1; V3/V4/V6/V7 đều 0.

### 2.2. View, sequence, function, trigger

| Loại | Số lượng | Tên | Ghi chú |
|---|---:|---|---|
| View | 6 | `v_current_stay`, `v_room_occupancy`, `v_deposit_balance`, `v_summer_leave`, `v_contract_label`, `v_user_notification` | Chỉ đọc; dữ liệu suy ra không lưu bảng |
| Sequence | 3 | `seq_contract_code`, `seq_maintenance_code`, `seq_invoice_code` | Sinh mã hiển thị qua `fn_next_code` |
| Function | 2 | `fn_audit_log_append_only`, `fn_next_code` | Trigger function và sinh mã |
| Trigger | 1 | `tg_audit_log_append_only` | Chặn UPDATE/DELETE trên T33 |
| Extension | 1 | `btree_gist` | Phục vụ exclusion constraint của T09 |

View và sequence không phải bảng nghiệp vụ, không có CRUD UI. Không kết luận view/trigger đã tồn tại trên DB môi trường nào.

### 2.3. Ma trận độ phủ entity/repository

- 33 entity baseline ứng với 33 bảng V1. Working tree hiện có thêm entity của bảng NEW ngoài baseline (mục 7), không tính vào 36.
- 33 repository JPA baseline ứng với 33 entity; T36 dùng SQL native trong service; T34/T35 do Spring Session quản lý (nếu được bật). Working tree có thêm repository của bảng NEW.
- Không có entity nào ánh xạ view; view được đọc bằng SQL native/JDBC.
- `resources.ts` phủ 29 resource; các bảng T11, T12, T15, T17, T20, T22, T25, T29, T31, T32, T34, T35, T36 không có resource CRUD độc lập.

## 3. Hồ sơ quy tắc theo từng bảng

Mỗi mục dưới đây là một dòng của manifest. Tiêu đề chứa mã `Txx` và tên bảng; các mục khác trong tài liệu chỉ dùng mã.

### T01 — `management_unit` — Đơn vị quản lý

- **Migration:** V1.
- **Module owner:** account.
- **Mục đích:** danh mục đơn vị quản lý (K1, N23), gốc phạm vi dữ liệu của Staff/KTV và đơn vị nguyện vọng của đơn đăng ký.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; UNIQUE `code`; không có cột trạng thái. Được tham chiếu bởi T02, T05, T11, T30.
- **Vòng đời:** hard delete nếu chưa được tham chiếu; service danh mục xóa trực tiếp, FK của các bảng liên quan chặn khi đang dùng. Không có khóa mềm.
- **Service/port ghi:** `ManagementUnitServiceImpl` (kế thừa `AbstractCrudServiceImpl`), audit danh mục cùng transaction.
- **UI resource/màn hình:** `units` → `/management-units`; ghi qua `/admin/management-units`; `CrudPage`.
- **CRUD UI được phép:** xem, tạo, sửa, xóa; chỉ ADMIN ghi.
- **Test hiện có:** `CrudApiIntegrationTest` (vòng đời danh mục, lọc quản trị). Chưa đối chiếu rule xóa.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T02 — `app_user` — Tài khoản

- **Migration:** V1; V3 bổ sung `security_version`; V4 bổ sung `keycloak_user_id`, cho phép `password_hash` null.
- **Module owner:** account.
- **Mục đích:** danh tính DMS, vai trò, đơn vị, trạng thái khóa, phiên bản bảo mật và liên kết subject Keycloak.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; UNIQUE `username`; FK `management_unit_id`; CHECK role thuộc bốn vai trò; `ck_user_unit` buộc Staff/KTV có đơn vị và Student/Admin không có; `ck_user_cross_unit`; `security_version >= 0`; UNIQUE partial `keycloak_user_id`.
- **Vòng đời:** khóa/mở khóa thay vì xóa; không có DELETE cho tài khoản Staff/Admin. Xóa sinh viên chưa dùng đi qua T03. Đổi role/đơn vị/khóa/reset phải thu hồi phiên theo quy tắc dự án.
- **Service/port ghi:** `UserServiceImpl` (save, lock, unlock, reset, profile), `StudentManagementServiceImpl`, `AuthServiceImpl`, `KeycloakIdentityAccountServiceImpl`.
- **UI resource/màn hình:** `users` → `/admin/users`; `profile` → `/me/profile`; import sinh viên.
- **CRUD UI được phép:** tạo tài khoản Staff/KTV/Admin, sửa hồ sơ/vai trò/đơn vị, khóa/mở khóa, reset mật khẩu; không xóa cứng; người dùng tự sửa hồ sơ giới hạn.
- **Test hiện có:** `KeycloakAccountIntegrationTest`, `CrudApiIntegrationTest`. Chưa chạy trong lượt này.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T03 — `student_profile` — Hồ sơ sinh viên

- **Migration:** V1.
- **Module owner:** account.
- **Mục đích:** hồ sơ 1-1 với tài khoản sinh viên; mọi bảng nghiệp vụ tham chiếu sinh viên qua khóa này.
- **PK/FK/UNIQUE/CHECK chính:** PK/FK `user_id` → T02; UNIQUE `student_code`; CHECK giới tính `M/F`; các cột còn lại nullable.
- **Vòng đời:** hard delete chỉ khi chưa có đơn đăng ký và hợp đồng; service khóa tài khoản, kiểm tra T14/T16, xóa hồ sơ và tài khoản, xóa danh tính Keycloak sau commit. Còn lịch sử thì giữ/khóa, không xóa.
- **Service/port ghi:** `StudentManagementServiceImpl` (save, delete unused), `StudentImportServiceImpl` (tạo hàng loạt), `UserServiceImpl` (hồ sơ tự cập nhật).
- **UI resource/màn hình:** `students`, `adminStudents`, `StudentImportPanel`, `profile`.
- **CRUD UI được phép:** ADMIN tạo/xóa hồ sơ chưa dùng; Staff sửa liên hệ trong đơn vị; sinh viên sửa điện thoại/quê quán/liên hệ khẩn cấp; import tạo tài khoản.
- **Test hiện có:** `StudentImportIntegrationTest`, `CrudApiIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T04 — `system_config` — Cấu hình nghiệp vụ

- **Migration:** V1.
- **Module owner:** common.
- **Mục đích:** tham số nghiệp vụ dạng khóa/giá trị, đọc qua `ConfigService`; seed 5 khóa thời hạn/ngưỡng.
- **PK/FK/UNIQUE/CHECK chính:** PK `key`; FK `updated_by` → T02; `value` và `updated_at` NOT NULL; không có bảng lịch sử giá trị.
- **Vòng đời:** chỉ sửa giá trị của khóa đã tồn tại; không có API/UI tạo hoặc xóa khóa. Audit cùng transaction.
- **Service/port ghi:** `ConfigServiceImpl.saveOrUpdate`; hiện parse giá trị thành số nguyên 1–3650.
- **UI resource/màn hình:** `configs` → `/admin/configs`; màn chỉ sửa, không tạo/xóa.
- **CRUD UI được phép:** xem và sửa giá trị; không thêm/xóa key.
- **Test hiện có:** `CrudApiIntegrationTest` (lưu cấu hình giữ định danh).
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T05 — `building` — Tòa nhà

- **Migration:** V1.
- **Module owner:** Room.
- **Mục đích:** tòa thuộc đơn vị, mức cọc của tòa; gốc gom phòng và chỉ tiêu đăng ký.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `management_unit_id` → T01; UNIQUE `code`; CHECK `deposit_amount > 0`.
- **Vòng đời:** hard delete nếu chưa được tham chiếu bởi T07, T12, T21, T30; không có cột trạng thái.
- **Service/port ghi:** `BuildingServiceImpl` (danh mục), chỉ ADMIN ghi.
- **UI resource/màn hình:** `buildings` → `/buildings`; ghi qua `/admin/buildings`.
- **CRUD UI được phép:** xem, tạo, sửa, xóa.
- **Test hiện có:** `CrudApiIntegrationTest` (vòng đời danh mục).
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T06 — `room_type` — Loại phòng

- **Migration:** V1.
- **Module owner:** Room.
- **Mục đích:** loại phòng, số giường, giá gốc học kỳ và hè, trạng thái sử dụng; giá được snapshot sang phân phòng/hóa đơn.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; UNIQUE `name`; CHECK `bed_count` 1–12; CHECK giá `>= 0`; `active` mặc định true. Được tham chiếu bởi T07 và T14.
- **Vòng đời:** ngừng dùng bằng `active = false`; hard delete nếu chưa tham chiếu. Giá đã dùng phải giữ lịch sử. Quy tắc đổi `bed_count` khi đã có phòng/phân phòng còn tranh chấp giữa kế hoạch và test hiện có — `CẦN XÁC MINH`.
- **Service/port ghi:** `RoomTypeServiceImpl` (danh mục; có đồng bộ giường theo số giường).
- **UI resource/màn hình:** `roomTypes`; màn danh mục loại phòng.
- **CRUD UI được phép:** xem, tạo, sửa, xóa, bật/tắt `active`.
- **Test hiện có:** `CrudApiIntegrationTest` (thu gọn/mở rộng loại phòng, đồng bộ giường).
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T07 — `room` — Phòng

- **Migration:** V1.
- **Module owner:** Room.
- **Mục đích:** phòng vật lý thuộc tòa/loại, tầng, giới tính, trạng thái vận hành và trưởng phòng.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `building_id` → T05, `room_type_id` → T06, `leader_student_id` → T03; UNIQUE `(building_id, code)`; UNIQUE partial trưởng phòng; CHECK giới tính; CHECK trạng thái `ACTIVE/MAINTENANCE/CLOSED`.
- **Vòng đời:** đổi trạng thái vận hành bằng action; hard delete chỉ khi chưa có lịch sử phân phòng (T17); bỏ trưởng phòng khi người đó rời. Không hard delete phòng đã có lịch sử.
- **Service/port ghi:** `RoomServiceImpl` (save, change status, assign leader, delete).
- **UI resource/màn hình:** `rooms` → `/rooms`; `RoomDetailPage` quản lý giường/trưởng phòng.
- **CRUD UI được phép:** xem, tạo, sửa, đổi trạng thái, gán/bỏ trưởng phòng, xóa khi chưa dùng.
- **Test hiện có:** `RoomLeaderIntegrationTest`, `BedOccupancyIntegrationTest`, `CrudApiIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T08 — `bed` — Giường

- **Migration:** V1.
- **Module owner:** Room.
- **Mục đích:** giường vật lý trong phòng, trạng thái khai thác; đơn vị chiếm chỗ của phân phòng.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `room_id` → T07; UNIQUE `(room_id, code)`; CHECK trạng thái `ACTIVE/BROKEN/RETIRED`. Được tham chiếu bởi T17, T19, T25.
- **Vòng đời:** đổi trạng thái `ACTIVE/BROKEN/RETIRED`; hard delete chỉ khi chưa có phân phòng; sửa không chuyển phòng nguồn.
- **Service/port ghi:** `RoomServiceImpl.saveOrUpdateBed`, `changeBedStatus`, `deleteBed`.
- **UI resource/màn hình:** `rooms` + `RoomDetailPage` + `BedCrudPanel`.
- **CRUD UI được phép:** tạo, sửa mã, đổi trạng thái, xóa khi chưa có lịch sử.
- **Test hiện có:** `CrudApiIntegrationTest`, `BedOccupancyIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T09 — `semester` — Học kỳ

- **Migration:** V1.
- **Module owner:** Room.
- **Mục đích:** học kỳ thường/hè, khoảng ngày không chồng; nguồn cho đợt đăng ký, lựa chọn hè và hóa đơn phí phòng.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; UNIQUE `(academic_year, type)`; CHECK loại `HK1/HK2/SUMMER`; CHECK `end_date > start_date`; EXCLUDE gist chống chồng khoảng ngày.
- **Vòng đời:** hard delete nếu chưa được tham chiếu bởi T10, T24, T28; không có cột trạng thái.
- **Service/port ghi:** `SemesterServiceImpl`.
- **UI resource/màn hình:** `semesters` → `/semesters`.
- **CRUD UI được phép:** xem, tạo, sửa, xóa khi chưa dùng.
- **Test hiện có:** `CrudApiIntegrationTest` (chồng học kỳ bị PostgreSQL chặn).
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T10 — `registration_period` — Đợt đăng ký

- **Migration:** V1.
- **Module owner:** registration.
- **Mục đích:** đợt đăng ký thường hoặc đợt chọn phương án hè, thời gian mở/đóng, người tạo.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `semester_id` → T09, `created_by` → T02; CHECK loại `REGULAR/SUMMER`; CHECK `close_at > open_at`. Con T11/T12.
- **Vòng đời:** hard delete chỉ khi chưa mở và chưa có đơn T14; đợt đã mở chỉ sửa trường được phép (ngày đóng); con T11/T12 ghi trong cùng transaction.
- **Service/port ghi:** `RegistrationPeriodServiceImpl` (save/delete aggregate).
- **UI resource/màn hình:** `periods` → `/registration-periods`.
- **CRUD UI được phép:** xem, tạo, sửa; xóa đợt chưa dùng; cấu hình đơn vị qua `unitIds`; chỉ tiêu chưa thấy trong registry — xem GAP-07.
- **Test hiện có:** `CrudApiIntegrationTest` (mapping composite, scope đơn vị, đợt dùng chung).
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T11 — `registration_period_unit` — Đơn vị áp dụng đợt

- **Migration:** V1.
- **Module owner:** registration.
- **Mục đích:** quan hệ nhiều-nhiều giữa đợt và đơn vị áp dụng.
- **PK/FK/UNIQUE/CHECK chính:** PK composite `(period_id, management_unit_id)`; FK về T10 và T01.
- **Vòng đời:** dòng con của đợt; thay theo danh sách khi lưu đợt, xóa cùng đợt; không có API/UI độc lập.
- **Service/port ghi:** `RegistrationPeriodServiceImpl`.
- **UI resource/màn hình:** `periods` — trường `unitIds` nhiều lựa chọn.
- **CRUD UI được phép:** không có màn riêng; cấu hình qua form đợt.
- **Test hiện có:** `CrudApiIntegrationTest` (mapping composite).
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T12 — `registration_quota` — Chỉ tiêu theo tòa

- **Migration:** V1.
- **Module owner:** registration.
- **Mục đích:** chỉ tiêu số chỗ theo tòa trong một đợt.
- **PK/FK/UNIQUE/CHECK chính:** PK composite `(period_id, building_id)`; FK về T10 và T05; CHECK `quota >= 0`.
- **Vòng đời:** dòng con của đợt; thay khi input có quotas, xóa cùng đợt; đợt hè không nhận quota; không có API/UI độc lập.
- **Service/port ghi:** `RegistrationPeriodServiceImpl` (ghi trong transaction đợt).
- **UI resource/màn hình:** `CẦN XÁC MINH` — DTO backend có `quotas`, nhưng `resources.periods` chưa khai báo trường này.
- **CRUD UI được phép:** không có màn riêng; hiện chưa thấy control quota trong registry.
- **Test hiện có:** `CẦN XÁC MINH` (không thấy test tham chiếu trực tiếp).
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T13 — `priority_category` — Diện ưu tiên

- **Migration:** V1.
- **Module owner:** registration.
- **Mục đích:** danh mục diện ưu tiên và trọng số cấu hình; nguồn tính điểm đơn.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; UNIQUE `code`; CHECK `weight >= 0`; `active` mặc định true. Được tham chiếu bởi T15.
- **Vòng đời:** ngừng dùng bằng `active = false`; hard delete nếu chưa có liên kết T15; không có lịch sử giá trị.
- **Service/port ghi:** `PriorityCategoryServiceImpl`; chỉ ADMIN ghi.
- **UI resource/màn hình:** `priorities` → `/priority-categories`; ghi qua `/admin/priority-categories`.
- **CRUD UI được phép:** xem, tạo, sửa, xóa, bật/tắt `active`.
- **Test hiện có:** `ApplicationAssignmentIntegrationTest` (xác minh ưu tiên, tính lại điểm).
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T14 — `room_application` — Đơn đăng ký

- **Migration:** V1.
- **Module owner:** registration.
- **Mục đích:** đơn của sinh viên theo đợt, nguyện vọng đơn vị/loại phòng, điểm ưu tiên và trạng thái xử lý.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `student_id` → T03, `period_id` → T10, `preferred_unit_id` → T01, `preferred_room_type_id` → T06; UNIQUE `(student_id, period_id)`; CHECK trạng thái `DRAFT/SUBMITTED/APPROVED/REJECTED/ASSIGNED/CANCELLED`; CHECK REJECTED phải có lý do.
- **Vòng đời:** chỉ DRAFT được sửa; nộp/duyệt/từ chối/phân giường/giải phóng giữ chỗ/hủy bằng action riêng; không hard delete; lịch sử ghi T32.
- **Service/port ghi:** `RoomApplicationServiceImpl` (save, submit, approve, reject, cancel, assign, release, verify priority).
- **UI resource/màn hình:** `applications`, `myApplications`; `ApplicationDetailView`.
- **CRUD UI được phép:** tạo/sửa đơn DRAFT, hủy; duyệt/phân giường/giải phóng ở màn chi tiết; không xóa cứng.
- **Test hiện có:** `ApplicationAssignmentIntegrationTest`, `CrudApiIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T15 — `application_priority` — Diện ưu tiên của đơn

- **Migration:** V1; V7 bổ sung `note`.
- **Module owner:** registration.
- **Mục đích:** liên kết đơn với diện ưu tiên, kết quả xác minh của Staff và ghi chú.
- **PK/FK/UNIQUE/CHECK chính:** PK composite `(application_id, category_id)`; FK về T14, T13, T02; `verified` nullable; `note` nullable. Không có CHECK buộc `verified_by/verified_at` khi `verified = true`.
- **Vòng đời:** dòng con theo đơn; xác minh ghi đè người/thời gian/ghi chú trên cùng dòng; không CRUD độc lập; điểm đơn được tính lại trong transaction.
- **Service/port ghi:** `RoomApplicationServiceImpl.saveOrUpdate` và `verifyPriority`.
- **UI resource/màn hình:** `applications` — chọn nhiều diện; `VerifyPriorityModal`.
- **CRUD UI được phép:** chọn diện khi tạo/sửa đơn; xác minh ở màn chi tiết; không sửa trực tiếp điểm/trạng thái xác minh từ form đơn.
- **Test hiện có:** `ApplicationAssignmentIntegrationTest` (xác minh, audit, tính lại điểm).
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T16 — `dormitory_contract` — Hợp đồng lưu trú

- **Migration:** V1.
- **Module owner:** contract.
- **Mục đích:** một lần lưu trú của sinh viên, sinh từ đơn đã duyệt; giữ hạn kích hoạt, mức cọc chụp, trạng thái và lý do kết thúc.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; UNIQUE `code`; FK `student_id` → T03, `application_id` → T14, `activated_by` → T02; CHECK trạng thái `PENDING/ACTIVE/EXPIRED/TERMINATED/CANCELLED`; CHECK ngày; CHECK trạng thái hoạt động phải có `activated_at` và `deposit_required`; UNIQUE partial mỗi sinh viên/đơn tối đa một hợp đồng sống; UNIQUE `(id, student_id)` cho FK kép của T17.
- **Vòng đời:** tạo PENDING từ đơn APPROVED; chỉ PENDING được sửa; hard delete PENDING chỉ khi không có phân phòng/cọc/lịch sử; kích hoạt/hết hạn/kết thúc/hủy bằng action; không xóa hợp đồng đã hoạt động.
- **Service/port ghi:** `ContractServiceImpl` (tạo PENDING, activate, cancel, extend), `ContractCrudServiceImpl` (save/delete PENDING), `ContractLifecycleService` (hết hạn), `MoveOutService` (kết thúc).
- **UI resource/màn hình:** `contracts`, `myContracts`; `ContractDetailView`, `ActivateContractModal`.
- **CRUD UI được phép:** xem, tạo/sửa PENDING, xóa PENDING chưa dùng; kích hoạt/trả phòng/gửi đồ ở màn chi tiết; không sửa trạng thái trực tiếp.
- **Test hiện có:** `ContractActivationIntegrationTest`, `ContractExtensionIntegrationTest`, `ApplicationAssignmentIntegrationTest`, `CrudApiIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T17 — `room_assignment_history` — Lịch sử phân phòng

- **Migration:** V1.
- **Module owner:** Room.
- **Mục đích:** nguồn duy nhất cho “ai ở giường nào, từ khi nào, giá bao nhiêu”; HOLD/STAY và lịch sử đóng/mở.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `bed_id` → T08; FK kép `(contract_id, student_id)` → T16; CHECK `kind` HOLD/STAY; CHECK `end_reason`; CHECK giá `>= 0`; CHECK `ended_at` đi cặp `end_reason`; CHECK STAY đi cặp `stay_started_at`; UNIQUE partial mỗi giường/mỗi sinh viên chỉ một dòng mở.
- **Vòng đời:** append-only; chỉ `BedOccupancyService` mở/đóng/chuyển dòng, cùng transaction và thứ tự khóa; không CRUD, không xóa lịch sử.
- **Service/port ghi:** `BedOccupancyServiceImpl` (occupy HOLD/STAY, convert, release, transfer).
- **UI resource/màn hình:** chỉ đọc trong chi tiết hợp đồng/phòng và `myRoom`.
- **CRUD UI được phép:** không có.
- **Test hiện có:** `BedOccupancyIntegrationTest`, `ApplicationAssignmentIntegrationTest`, `MoveOutIntegrationTest`, `StorageIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T18 — `contract_extension_request` — Yêu cầu gia hạn

- **Migration:** V1.
- **Module owner:** contract.
- **Mục đích:** yêu cầu kéo dài ngày kết thúc hợp đồng, gồm nguồn STUDENT hoặc SUMMER_STAY.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `contract_id` → T16, `decided_by` → T02; CHECK nguồn; CHECK trạng thái `PENDING/APPROVED/REJECTED/CANCELLED`; CHECK ngày mới lớn hơn ngày cũ; CHECK REJECTED phải có lý do; UNIQUE partial một yêu cầu PENDING mỗi hợp đồng.
- **Vòng đời:** PENDING được tạo/sửa; duyệt/từ chối/hủy bằng action; hard delete PENDING theo endpoint hiện có; sau quyết định giữ nguyên bản ghi. Cần chốt xóa PENDING so với cancel — xem GAP-01.
- **Service/port ghi:** `ContractCrudServiceImpl` (save/delete stay request, approve/reject/cancel extension).
- **UI resource/màn hình:** `extensions`; `ExtensionDetailPage`.
- **CRUD UI được phép:** tạo/sửa PENDING; hủy (UI gọi action cancel); duyệt/từ chối ở màn chi tiết; backend vẫn có DELETE PENDING.
- **Test hiện có:** `ContractExtensionIntegrationTest`, `ContractExtensionRuleTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T19 — `room_transfer_request` — Yêu cầu chuyển phòng

- **Migration:** V1.
- **Module owner:** contract.
- **Mục đích:** yêu cầu chuyển phòng của sinh viên đang ở; Staff chọn giường đích khi duyệt.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `contract_id` → T16, `target_bed_id` → T08, `decided_by` → T02; CHECK trạng thái; CHECK APPROVED phải có giường đích; CHECK REJECTED phải có lý do; UNIQUE partial một yêu cầu PENDING mỗi hợp đồng.
- **Vòng đời:** PENDING tạo/sửa; duyệt/từ chối/hủy bằng action; hard delete PENDING theo endpoint; sau quyết định giữ bản ghi. Cần chốt xóa PENDING so với cancel — xem GAP-01.
- **Service/port ghi:** `ContractCrudServiceImpl` (save/delete), `RoomTransferServiceImpl` (approve/reject/cancel).
- **UI resource/màn hình:** `transfers`; `TransferReviewPage`.
- **CRUD UI được phép:** tạo/sửa PENDING; hủy; duyệt/từ chối ở màn chi tiết; backend vẫn có DELETE PENDING.
- **Test hiện có:** `BedOccupancyIntegrationTest`, `CrudApiIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T20 — `move_out_record` — Biên bản trả phòng

- **Migration:** V1.
- **Module owner:** contract.
- **Mục đích:** chứng từ trả phòng: tình trạng tài sản, công nợ phí phòng, số dư cọc và tiền hoàn tại thời điểm trả.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; UNIQUE `contract_id` → T16 (mỗi hợp đồng một biên bản); FK `confirmed_by` → T02; CHECK tài sản `OK/DAMAGED`; CHECK DAMAGED phải có ghi chú; CHECK hoàn cọc bằng `GREATEST(deposit_balance, 0)`.
- **Vòng đời:** append-only; tạo một lần trong transaction trả phòng; không sửa/xóa; không có CRUD.
- **Service/port ghi:** `MoveOutServiceImpl.execute` (preview chỉ đọc).
- **UI resource/màn hình:** `MoveOutPage`, `MoveOutModal`, chi tiết hợp đồng.
- **CRUD UI được phép:** chỉ tạo qua action trả phòng; không sửa/xóa.
- **Test hiện có:** `MoveOutIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T21 — `maintenance_request` — Yêu cầu sửa chữa

- **Migration:** V1.
- **Module owner:** maintenance.
- **Mục đích:** ticket sự cố theo phòng hoặc khu vực chung; phân công KTV, xử lý, nghiệm thu, kết luận nguyên nhân/chi phí.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; UNIQUE `code`; FK `requester_id`, `room_id`, `building_id`, `assigned_to`, `cost_decided_by`; CHECK category/severity/status/cause; CHECK đúng một loại vị trí; CHECK trạng thái xử lý phải có người được giao; CHECK cause chỉ ở ticket phòng đã CLOSED; CHECK chi phí theo cause.
- **Vòng đời:** OPEN được tạo/sửa/hủy; assign/start/resolve/accept/reopen/conclude/close theo policy; tự đóng RESOLVED sau số ngày cấu hình; không hard delete, giữ lịch sử T32.
- **Service/port ghi:** `MaintenanceRequestServiceImpl` (save, assign, action, conclude), `MaintenanceCrudServiceImpl.cancelOpenRequest`, `MaintenanceLifecycleServiceImpl.closeResolved`, `MaintenancePhotoServiceImpl`.
- **UI resource/màn hình:** `maintenance`; `MaintenanceDetailView`, `CreateTicketModal`, `ConcludeTicketModal`.
- **CRUD UI được phép:** tạo/sửa ticket OPEN dạng multipart; hủy; các action còn lại ở màn chi tiết.
- **Test hiện có:** `MaintenanceConcludeIntegrationTest`, `MaintenanceConcludeRollbackIntegrationTest`, `CrudApiIntegrationTest`, `SystemJobsIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T22 — `maintenance_photo` — Ảnh sự cố

- **Migration:** V1.
- **Module owner:** maintenance.
- **Mục đích:** metadata ảnh của ticket; file thật nằm ngoài CSDL, chỉ phục vụ qua endpoint kiểm quyền.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `request_id` → T21; `file_path` NOT NULL; không unique, không cascade.
- **Vòng đời:** append khi tạo ticket; giữ ảnh cũ khi sửa; không có API xóa ảnh; file được dọn nếu transaction rollback. Không có lịch sử.
- **Service/port ghi:** `MaintenancePhotoServiceImpl.savePhotos`.
- **UI resource/màn hình:** `MaintenancePhotoGallery`; endpoint ảnh trong chi tiết ticket.
- **CRUD UI được phép:** chỉ thêm khi tạo ticket và xem; không sửa/xóa.
- **Test hiện có:** `CrudApiIntegrationTest` (rollback ảnh); chưa thấy test riêng cho quyền đọc ảnh — `CẦN XÁC MINH`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T23 — `deposit_transaction` — Sổ cọc

- **Migration:** V1.
- **Module owner:** billing.
- **Mục đích:** ledger thu/trừ/bù/hoàn cọc; số dư và khoản cần bù là dữ liệu suy ra từ view.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `contract_id` → T16, `maintenance_request_id` → T21, `confirmed_by` → T02; CHECK type `DEPOSIT/DEDUCTION/TOPUP/REFUND`; CHECK dấu tiền; CHECK DEDUCTION phải có ticket và hạn bù; CHECK ngoài DEDUCTION phải có người xác nhận; UNIQUE partial một DEPOSIT, một REFUND, một DEDUCTION/ticket mỗi hợp đồng.
- **Vòng đời:** append-only; thu/trừ/bù/hoàn qua `DepositService` trong transaction có khóa hợp đồng; không sửa/xóa giao dịch.
- **Service/port ghi:** `DepositServiceImpl` (collectInitial, deduct, topup, refund); được gọi từ kích hoạt hợp đồng, kết luận ticket và trả phòng.
- **UI resource/màn hình:** `deposits`, `myDeposits` chỉ đọc; `DepositLedgerModal`; action nộp bù/hoàn cọc trong chi tiết.
- **CRUD UI được phép:** chỉ xem sổ; thu/trừ/bù/hoàn là action nghiệp vụ, không sửa/xóa dòng.
- **Test hiện có:** `DepositIntegrationTest`, `DepositTopupGuardTest`, `MaintenanceConcludeIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T24 — `summer_choice` — Lựa chọn kỳ hè

- **Migration:** V1.
- **Module owner:** registration.
- **Mục đích:** lựa chọn STAY/TEMP_LEAVE/LEAVE của hợp đồng trong học kỳ hè; STAY tạo gia hạn hợp đồng.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `contract_id` → T16, `semester_id` → T09, `decided_by` → T02; CHECK choice; CHECK trạng thái `PENDING/APPROVED/REJECTED`; UNIQUE `(contract_id, semester_id)`. Không có trạng thái CANCELLED.
- **Vòng đời:** PENDING tạo/sửa/xóa; duyệt/từ chối; STAY cập nhật ngày kết thúc hợp đồng; không có cancel trong schema — xem GAP-09.
- **Service/port ghi:** `SummerCrudServiceImpl` (save, approve, reject, delete).
- **UI resource/màn hình:** `summer`; `SummerChoiceDetailView`.
- **CRUD UI được phép:** tạo/sửa PENDING; duyệt/từ chối; backend có DELETE nhưng registry chưa khai báo action hủy — `CẦN XÁC MINH`.
- **Test hiện có:** `SummerChoiceIntegrationTest`, `SummerChoiceApprovalTest`, `StorageIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T25 — `storage_record` — Phiếu gửi đồ

- **Migration:** V1.
- **Module owner:** contract.
- **Mục đích:** phiếu gửi đồ khi tạm rời hè; giữ giường cũ để gợi ý phân lại; theo dõi ngày/người nhận lại.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `contract_id` → T16, UNIQUE `summer_choice_id` → T24, `previous_bed_id` → T08, `received_by`/`returned_by` → T02; CHECK số kiện `> 0`; CHECK ngày nhận lại không trước ngày gửi; CHECK ngày/người nhận lại đi cặp; UNIQUE partial một phiếu mở mỗi hợp đồng.
- **Vòng đời:** tạo qua action gửi đồ; nhận lại qua action; không sửa/xóa; phiếu mở/đã nhận là chứng từ.
- **Service/port ghi:** `StorageServiceImpl.create`, `returnStorage`.
- **UI resource/màn hình:** `StoragePage`, `StorageModal`; chi tiết hợp đồng.
- **CRUD UI được phép:** chỉ tạo phiếu và nhận lại; không sửa/xóa.
- **Test hiện có:** `StorageIntegrationTest`, `CrudApiIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T26 — `utility_rate` — Đơn giá điện nước

- **Migration:** V1.
- **Module owner:** billing.
- **Mục đích:** lịch sử đơn giá theo loại đồng hồ và ngày hiệu lực; hóa đơn snapshot đơn giá.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `created_by` → T02; CHECK meter type; CHECK `unit_price > 0`; UNIQUE `(meter_type, effective_from)`.
- **Vòng đời:** giá tương lai được tạo/sửa/xóa; giá đã có hiệu lực bất biến, thay bằng bản ghi mới; không sửa lịch sử.
- **Service/port ghi:** `UtilityRateServiceImpl.saveOrUpdate`, `UtilityRateCrudServiceImpl.deleteRate`.
- **UI resource/màn hình:** `rates` → `/utility-rates`.
- **CRUD UI được phép:** xem, tạo, sửa/xóa giá tương lai; không sửa giá đã hiệu lực.
- **Test hiện có:** `CrudApiIntegrationTest` (lưu/tìm giá giữ bộ lọc và giá trị từng phần).
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T27 — `utility_reading` — Chỉ số điện nước

- **Migration:** V1.
- **Module owner:** billing.
- **Mục đích:** chỉ số đầu/cuối theo phòng, loại đồng hồ, tháng; hỗ trợ thay/reset đồng hồ; `consumption` là cột generated.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `room_id` → T07, `recorded_by` → T02; CHECK meter type; CHECK `billing_month` là ngày 1; CHECK chỉ số không âm; CHECK cặp chỉ số thay đồng hồ; UNIQUE `(room_id, meter_type, billing_month)`; CHECK đầu/cuối theo từng trường hợp.
- **Vòng đời:** tạo/sửa/xóa khi chưa lập hóa đơn và chưa có kỳ sau; khóa khi đã dùng; DEC-BIL-01 yêu cầu đủ kỳ liên tiếp trước khi nhập kỳ mới.
- **Service/port ghi:** `UtilityReadingServiceImpl.saveOrUpdate`, `deleteUnusedReading`.
- **UI resource/màn hình:** `readings` → `/utility-readings`.
- **CRUD UI được phép:** xem, ghi chỉ số, sửa/xóa khi còn khóa cho phép.
- **Test hiện có:** `CrudApiIntegrationTest` (nhiều kịch bản kỳ/thay đồng hồ), `BillingConcurrencyIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T28 — `invoice` — Hóa đơn

- **Migration:** V1.
- **Module owner:** billing.
- **Mục đích:** hóa đơn phí phòng theo sinh viên/học kỳ hoặc điện nước theo phòng/tháng; trạng thái thu tiền.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; UNIQUE `code`; FK `student_id`, `semester_id`, `room_id`, `payer_user_id`, `confirmed_by`, `issued_by`; CHECK type/status/payment method; CHECK cột theo loại hóa đơn; CHECK hạn đóng không trước ngày phát hành; CHECK PAID đi cặp ngày/người/hình thức; UNIQUE partial chống trùng phí phòng và điện nước.
- **Vòng đời:** tạo thủ công hoặc phát hành hàng loạt; chỉ UNPAID chưa có dòng chi tiết/lịch sử được sửa/xóa; xác nhận thanh toán chuyển PAID và khóa; không có cancel; điều chỉnh qua workflow.
- **Service/port ghi:** `InvoiceServiceImpl` (issue, confirm payment), `InvoiceCrudServiceImpl` (save/delete hồ sơ UNPAID).
- **UI resource/màn hình:** `invoices`, `myInvoices`; `ConfirmPaymentModal`, `IssueInvoicesModal`.
- **CRUD UI được phép:** xem, tạo/sửa/xóa hồ sơ UNPAID chưa có dòng; xác nhận thanh toán; phát hành hàng loạt.
- **Test hiện có:** `BillingConcurrencyIntegrationTest`, `CrudApiIntegrationTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T29 — `invoice_item` — Dòng hóa đơn

- **Migration:** V1.
- **Module owner:** billing.
- **Mục đích:** dòng tiền phòng/điện/nước/điều chỉnh; snapshot đơn giá và liên kết nguồn.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `invoice_id` → T28, `assignment_id` → T17, `utility_reading_id` → T27, `adjusts_invoice_id` → T28; CHECK item type; CHECK ADJUSTMENT phải có ghi chú và hóa đơn đích; CHECK ngoài ADJUSTMENT không âm; CHECK ROOM_FEE phải có assignment; CHECK điện/nước phải có reading.
- **Vòng đời:** append-only khi phát hành hóa đơn; không có API/UI độc lập; không sửa/xóa; xóa hóa đơn bị chặn khi còn dòng.
- **Service/port ghi:** `InvoiceServiceImpl` (phát hành ghi dòng).
- **UI resource/màn hình:** chỉ đọc trong chi tiết hóa đơn.
- **CRUD UI được phép:** không có màn riêng.
- **Test hiện có:** `BillingConcurrencyIntegrationTest` (dòng hóa đơn); chưa thấy test riêng cho toàn bộ CHECK — `CẦN XÁC MINH`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T30 — `notification` — Thông báo

- **Migration:** V1.
- **Module owner:** Notification.
- **Mục đích:** thông báo thủ công hoặc hệ thống theo phạm vi ALL/UNIT/BUILDING/FLOOR/ROOM/USER.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `management_unit_id`, `building_id`, `room_id`, `user_id`, `created_by`; CHECK mỗi scope chỉ đúng các cột tương ứng; `title`/`body` NOT NULL; `source_type/source_id` cho thông báo tự động.
- **Vòng đời:** thủ công được tạo, sửa nội dung, xóa; scope/người nhận bất biến khi sửa; thông báo hệ thống không sửa/xóa qua CRUD; đánh dấu đọc là thao tác riêng.
- **Service/port ghi:** `NotificationCrudServiceImpl` (manual save/delete), `NotificationServiceImpl` (notify users/events).
- **UI resource/màn hình:** `notifications` → `/notifications/published`; `NotificationInboxPage`.
- **CRUD UI được phép:** tạo thông báo thủ công, sửa tiêu đề/nội dung, xóa; không sửa scope/người nhận; không CRUD thông báo hệ thống.
- **Test hiện có:** `NotificationServiceImplTest`, `CrudApiIntegrationTest`, `ContractNotificationListenerTest`, `MaintenanceNotificationListenerTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T31 — `notification_read` — Trạng thái đã đọc

- **Migration:** V1.
- **Module owner:** Notification.
- **Mục đích:** đánh dấu từng người dùng đã đọc thông báo; nguồn tính badge chưa đọc.
- **PK/FK/UNIQUE/CHECK chính:** PK composite `(notification_id, user_id)`; FK về T30 và T02; `read_at` NOT NULL.
- **Vòng đời:** append/upsert idempotent khi người nhận đánh dấu đọc; không có API sửa/xóa; dòng bị xóa khi thông báo bị xóa qua service.
- **Service/port ghi:** `NotificationServiceImpl` (mark read/read-all); `NotificationCrudServiceImpl` dọn dòng khi xóa thông báo.
- **UI resource/màn hình:** `NotificationInboxPage`; badge chưa đọc.
- **CRUD UI được phép:** chỉ đánh dấu đọc/đọc tất cả; không CRUD.
- **Test hiện có:** `CrudApiIntegrationTest` (đọc lặp idempotent), `NotificationServiceImplTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T32 — `status_history` — Lịch sử trạng thái

- **Migration:** V1.
- **Module owner:** common.
- **Mục đích:** timeline chuyển trạng thái dùng chung cho đơn, hợp đồng, ticket, yêu cầu, hóa đơn.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; CHECK `entity_type` thuộc danh sách; `entity_id` không có FK; `from_status` nullable; `to_status` NOT NULL; `actor_id` FK T02, nullable cho SYSTEM; index theo entity/thời gian.
- **Vòng đời:** append-only theo quy trình/service; **không có trigger** chặn UPDATE/DELETE; không có API sửa/xóa.
- **Service/port ghi:** `StatusHistoryServiceImpl.record`, được gọi trong transaction nghiệp vụ.
- **UI resource/màn hình:** chỉ đọc qua `StatusTimeline`/`stepsFromFlow` trong chi tiết.
- **CRUD UI được phép:** không có.
- **Test hiện có:** nhiều integration test có ghi/đọc bảng này; chưa thấy test riêng bảo vệ append-only — `CẦN XÁC MINH`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T33 — `audit_log` — Nhật ký kiểm toán

- **Migration:** V1.
- **Module owner:** common.
- **Mục đích:** nhật ký thao tác quản trị/tác vụ tự động với actor, before/after, requestId.
- **PK/FK/UNIQUE/CHECK chính:** PK `id`; FK `actor_id` → T02, nullable cho SYSTEM; `action`/`entity_type` NOT NULL; `old_value/new_value` JSONB; `request_id`; index actor/entity/action.
- **Vòng đời:** append-only, được **trigger** chặn UPDATE/DELETE; không có API/UI ghi/sửa/xóa.
- **Service/port ghi:** `AuditServiceImpl.record`, được gọi từ service nghiệp vụ.
- **UI resource/màn hình:** `audit` → `/admin/audit-logs`; chỉ đọc.
- **CRUD UI được phép:** chỉ xem và lọc; không sửa/xóa.
- **Test hiện có:** `CrudApiIntegrationTest` (lọc/chi tiết audit), `SensitiveValuesTest`.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T34 — `spring_session` — Phiên Spring (legacy)

- **Migration:** V2.
- **Module owner:** platform.
- **Mục đích:** bảng phiên Spring Session JDBC theo schema chuẩn; di sản của giai đoạn trước Keycloak.
- **PK/FK/UNIQUE/CHECK chính:** PK `primary_id`; UNIQUE `session_id`; index `expiry_time`, `principal_name`; không có FK.
- **Vòng đời:** do Spring Session quản lý nếu được bật (tạo/expiry/purge); source hiện không thấy dependency/cấu hình session trong `pom.xml`/`application*.yml`; không tự drop. Không có CRUD.
- **Service/port ghi:** không có trong mã DMS; `CẦN XÁC MINH` runtime.
- **UI resource/màn hình:** không có.
- **CRUD UI được phép:** không có.
- **Test hiện có:** `DmsApplicationTests` chỉ kiểm tổng số bảng.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T35 — `spring_session_attributes` — Thuộc tính phiên (legacy)

- **Migration:** V2.
- **Module owner:** platform.
- **Mục đích:** lưu attribute của phiên Spring Session.
- **PK/FK/UNIQUE/CHECK chính:** PK composite `(session_primary_id, attribute_name)`; FK `session_primary_id` → T34 `ON DELETE CASCADE`.
- **Vòng đời:** xóa cascade theo phiên; do Spring Session quản lý nếu được bật; không CRUD.
- **Service/port ghi:** không có trong mã DMS; `CẦN XÁC MINH` runtime.
- **UI resource/màn hình:** không có.
- **CRUD UI được phép:** không có.
- **Test hiện có:** `DmsApplicationTests` chỉ kiểm tổng số bảng.
- **Trạng thái xác minh:** `NOT_VERIFIED`.

### T36 — `role_permission_matrix` — Ma trận quyền động

- **Migration:** V5; V6 cập nhật quyền mặc định cho STAFF.
- **Module owner:** common/security.
- **Mục đích:** quyền động theo vai trò dưới dạng JSONB, có version và người cập nhật; ADMIN không lưu dòng và luôn có toàn quyền đã đăng ký.
- **PK/FK/UNIQUE/CHECK chính:** PK `role`; CHECK role chỉ STAFF/STUDENT/TECHNICIAN; CHECK `permissions` là JSONB array; CHECK `version >= 0`; FK `updated_by` → T02 `ON DELETE SET NULL`.
- **Vòng đời:** chỉ update với khóa dòng và kiểm version; không có API tạo/xóa dòng; thiếu dòng thì fail closed; ADMIN bất biến; audit cùng transaction.
- **Service/port ghi:** `RolePermissionServiceImpl.updateRolePermissions` (JDBC native); seed bất biến từ V5/V6.
- **UI resource/màn hình:** `RolePermissionsPage` → `/admin/role-permissions`; không nằm trong `resources.ts`.
- **CRUD UI được phép:** xem ma trận, sửa tập quyền của ba vai trò; không tạo/xóa dòng, không sửa ADMIN.
- **Test hiện có:** `KeycloakAccountIntegrationTest` (version cũ, ADMIN bất biến, thiếu dòng), `CrudApiIntegrationTest` (cấp quyền cho test).
- **Trạng thái xác minh:** `NOT_VERIFIED`.

## 4. Ma trận vòng đời theo owner

Bảng dưới đây chuyển ma trận kế hoạch mục 6.1 sang mã `Txx`; đây là quy tắc **phải chốt**, không phải xác nhận code đã đáp ứng.

| Owner | Bảng (mã) | Quy tắc vòng đời bắt buộc |
|---|---|---|
| Account | T01 | Danh mục; kiểm tham chiếu trước xóa, không xóa đơn vị đang dùng. |
| Account | T02–T03 | Tài khoản có nghiệp vụ dùng khóa, không xóa lịch sử; đổi role/đơn vị/khóa phải thu hồi phiên; dữ liệu chưa dùng chỉ xóa nếu policy cho phép. |
| Common | T04 | Chỉ sửa key hiện có, giá trị có kiểu qua `ConfigService`; không thêm/xóa key tùy ý từ UI. |
| Common/security | T36 | Quyền động, version/khóa/audit; ADMIN bất biến, không tạo dòng để thu hồi quyền ADMIN. |
| Room | T05, T09 | FK/scope, điều kiện tham chiếu/ngày không chồng; chỉ xóa bản chưa dùng. |
| Room | T06 | Giữ `active` và snapshot giá; không đổi số giường khi đã gán phòng theo SRS (cần đối chiếu code — GAP-08). |
| Room | T07–T08 | Phân biệt trạng thái vận hành và chiếm chỗ; đã có lịch sử dùng action/ngừng phù hợp, không hard delete. |
| Room | T17 | Chỉ `BedOccupancyService` ghi/đóng/mở; cùng transaction, giữ khóa và unique index; không CRUD lịch sử. |
| registration | T10–T12 | Con thuộc transaction của đợt; phạm vi đơn vị/quota; đợt đã mở có field bị khóa; xóa có dependency check. |
| registration | T13, T15 | Danh mục `active`; kết quả xác minh có người/thời gian/note và điểm tính lại; quan hệ không CRUD độc lập. |
| registration | T14 | DRAFT mới sửa nội dung; submit/approve/reject/assign/release/cancel theo policy, giữ history. |
| registration | T24 | Giữ nguồn hợp đồng/học kỳ; sửa/xóa PENDING theo contract hiện hành, không tự thêm trạng thái CANCELLED. |
| contract | T16 | PENDING chỉ xét xóa khi không có assignment/cọc/history; trạng thái hoạt động/kết thúc đi qua action. |
| contract | T18–T19 | Chốt xóa PENDING so với cancel trước rollout; quyết định xong giữ nguồn và lịch sử. |
| contract | T20, T25 | Chứng từ nghiệp vụ; tạo/trả phòng/gửi/nhận đồ qua service riêng, không mở CRUD sửa/xóa tùy ý. |
| maintenance | T21–T22 | Ticket cancel/close theo policy; ảnh có quyền đọc/validation/rollback cùng nghiệp vụ, không xóa lịch sử để sửa lỗi. |
| billing | T26–T27 | Giá hiệu lực bất biến; chỉ số giữ phòng/loại/tháng, khóa khi đã có hóa đơn/kỳ sau; thay công tơ theo semantics đã chốt. |
| billing | T28–T29 | Chưa thanh toán không có nghĩa luôn được sửa; kiểm chi tiết/history/scope học kỳ; PAID khóa, điều chỉnh qua workflow. |
| billing | T23 | Ledger append-only, tiền VND; thu/trừ/bù/hoàn có transaction/khóa, không sửa/xóa trực tiếp qua UI. |
| Notification | T30–T31 | Manual chỉ sửa nội dung theo scope; system không CRUD; mark read/read-all theo người nhận và idempotent. |
| common | T32–T33 | Append-only theo quy trình, audit gắn actor/requestId và không secret; không có nút sửa/xóa nhật ký. Trigger audit không được coi là bảo vệ mọi bảng history. |
| platform | T34–T35 | Di sản schema V2; Keycloak không dùng session fallback; không có UI CRUD và không tự drop để dọn. |

## 5. Khoảng trống và quyết định cần chốt

| Mã | Nội dung | ID liên quan | Bằng chứng hiện có | Quyết định cần chốt | Owner đề xuất |
|---|---|---|---|---|---|
| GAP-01 | Xóa PENDING so với cancel cho yêu cầu gia hạn/chuyển phòng | T18, T19 | UI khai báo `deleteAction` là cancel; backend vẫn có DELETE PENDING | Chọn một semantics, cập nhật API/UI/tài liệu/test; không để hai đường ghi mâu thuẫn | Contract + QA |
| GAP-02 | Phạm vi sửa hóa đơn phí phòng theo học kỳ | T28 | CRUD hiện chỉ cho sửa tổng tiền/hạn khi UNPAID; kế hoạch yêu cầu kiểm scope học kỳ cho list/detail/write/confirm | Chốt rule scope và áp nhất quán; bổ sung test | Billing + Security |
| GAP-03 | Chứng từ trả phòng/gửi đồ không phải CRUD chung | T20, T25 | Không có resource trong `resources.ts`; chỉ có trang/modal chuyên biệt | Giữ service/action riêng; không thêm resource generic | Contract |
| GAP-04 | Tính append-only của lịch sử trạng thái và audit | T32, T33 | T33 có trigger chặn UPDATE/DELETE; T32 không có trigger | Chốt có thêm trigger/policy cho T32 hay chỉ dựa service; không xóa lịch sử | Common + DB owner |
| GAP-05 | Hai bảng phiên Spring là di sản | T34, T35 | V2 tạo bảng; source không thấy dependency/cấu hình session | Xác nhận runtime có dùng không; không tự drop; quyết định retention | Platform |
| GAP-06 | Bảng sở thích người dùng mới | NEW (mục 7) | V8 và entity/service đang ở working tree, chưa thuộc baseline V1–V7 | Merge/đánh số migration, cập nhật manifest và số bảng test | Account + PREF-01 |
| GAP-07 | UI chỉ tiêu đợt đăng ký | T12 | Backend DTO có `quotas`; `resources.periods` chưa khai báo | Bổ sung control/quota hoặc bỏ khỏi contract UI | FE-CRUD + Registration |
| GAP-08 | Đổi số giường của loại phòng khi đã có phòng | T06 | Kế hoạch nói không đổi khi đã gán; test hiện có thu gọn/mở rộng và đồng bộ giường | Chốt theo SRS; sửa code/test nếu lệch | Room + Business |
| GAP-09 | Hủy lựa chọn kỳ hè | T24 | Schema không có CANCELLED; backend có DELETE; registry chưa khai báo cancel | Chốt xóa PENDING hay bổ sung cancel; không tự thêm trạng thái | Registration + Contract |
| GAP-10 | Toàn vẹn tham chiếu của lịch sử trạng thái | T32 | `entity_id` không FK; không trigger; có thể mồ côi | Chốt retention/kiểm tra mồ côi; không thêm FK tùy tiện | Common + DB owner |
| GAP-11 | Cột mật khẩu cũ trên tài khoản | T02 | V4 cho `password_hash` null; không còn BCrypt fallback | Chốt giữ cột để tương thích hay migration loại bỏ sau khi xác nhận | Account + Security |
| GAP-12 | Kiểu giá trị cấu hình | T04 | `ConfigService` parse số nguyên 1–3650 cho mọi key | Chốt có cần typed config cho key không phải số hay không | Common |
| GAP-13 | Lưu giữ ảnh sự cố | T22 | Không có API xóa ảnh; không cascade khi ticket kết thúc | Chốt retention/xóa file theo policy | Maintenance |
| GAP-14 | Độ phủ test của manifest | T12, T22, T29, T32 | Chưa thấy test trực tiếp cho một số CHECK/rule | Lập test matrix theo bảng; giữ `NOT_VERIFIED` đến khi có bằng chứng | QA |

## 6. Truy vết bảng → entity → repository → DTO/controller → UI

Mỗi `Txx` tương ứng một mục ở phần 3; bảng dưới đây chỉ dùng mã để tránh lặp tên bảng.

| ID | Entity | Repository | DTO/controller | UI |
|---|---|---|---|---|
| T01 | `ManagementUnit` | `ManagementUnitRepository` | `ManagementUnitRequest/Response`, `ManagementUnitController` | `units` |
| T02 | `AppUser` | `AppUserRepository` | `AccountDtos`, `UserController`, `AuthController` | `users`, `profile` |
| T03 | `StudentProfile` | `StudentProfileRepository` | `AccountDtos`, `StudentManagementController` | `students`, `adminStudents`, `profile` |
| T04 | `SystemConfig` | `SystemConfigRepository` | `ConfigController` | `configs` |
| T05 | `Building` | `BuildingRepository` | `BuildingRequest/Response`, `BuildingController` | `buildings` |
| T06 | `RoomType` | `RoomTypeRepository` | `RoomTypeRequest/Response`, `RoomTypeController` | `roomTypes` |
| T07 | `Room` | `RoomRepository` | `RoomDtos`, `RoomController` | `rooms`, `RoomDetailPage` |
| T08 | `Bed` | `BedRepository` | `RoomDtos`, `BedController` | `rooms`, `BedCrudPanel` |
| T09 | `Semester` | `SemesterRepository` | `SemesterRequest/Response`, `SemesterController` | `semesters` |
| T10 | `RegistrationPeriod` | `RegistrationPeriodRepository` | `RegistrationPeriodDtos`, `RegistrationPeriodController` | `periods` |
| T11 | `RegistrationPeriodUnit` | `RegistrationPeriodUnitRepository` | `RegistrationPeriodDtos` (lồng trong input) | `periods` (`unitIds`) |
| T12 | `RegistrationQuota` | `RegistrationQuotaRepository` | `RegistrationPeriodDtos.Quota` | `CẦN XÁC MINH` |
| T13 | `PriorityCategory` | `PriorityCategoryRepository` | `PriorityCategoryRequest/Response`, `PriorityCategoryController` | `priorities` |
| T14 | `RoomApplication` | `RoomApplicationRepository` | `ApplicationDtos`, `RoomApplicationController` | `applications`, `myApplications` |
| T15 | `ApplicationPriority` | `ApplicationPriorityRepository` | `ApplicationDtos`, endpoint verify trong `RoomApplicationController` | `VerifyPriorityModal` |
| T16 | `DormitoryContract` | `DormitoryContractRepository` | `ContractDtos`, `ContractController`, `ContractCrudController` | `contracts`, `myContracts` |
| T17 | `RoomAssignmentHistory` | `RoomAssignmentHistoryRepository` | `ContractDtos.AssignmentInfo`, `OccupancyQuery` | chi tiết hợp đồng, `myRoom` |
| T18 | `ContractExtensionRequest` | `ContractExtensionRequestRepository` | `ContractDtos`, `ContractCrudController` | `extensions` |
| T19 | `RoomTransferRequest` | `RoomTransferRequestRepository` | `ContractDtos`, `ContractCrudController`, `ContractController` | `transfers` |
| T20 | `MoveOutRecord` | `MoveOutRecordRepository` | `MoveOutDtos`, `MoveOutService`, `ContractController` | `MoveOutPage`, `MoveOutModal` |
| T21 | `MaintenanceRequest` | `MaintenanceRequestRepository` | `MaintenanceDtos`, `MaintenanceRequestController` | `maintenance` |
| T22 | `MaintenancePhoto` | `MaintenancePhotoRepository` | `MaintenanceDtos.Photo`, endpoint ảnh | `MaintenancePhotoGallery` |
| T23 | `DepositTransaction` | `DepositTransactionRepository` | `DepositDtos`, `DepositController` | `deposits`, `myDeposits`, `DepositLedgerModal` |
| T24 | `SummerChoice` | `SummerChoiceRepository` | `SummerCrudController.Input`, `SummerCrudController` | `summer` |
| T25 | `StorageRecord` | `StorageRecordRepository` | `StorageDtos`, `ContractController` | `StoragePage`, `StorageModal` |
| T26 | `UtilityRate` | `UtilityRateRepository` | `UtilityRateDtos`, `UtilityRateController`, `UtilityRateCrudController` | `rates` |
| T27 | `UtilityReading` | `UtilityReadingRepository` | `UtilityReadingController.Input`, `UtilityReadingServiceImpl` | `readings` |
| T28 | `Invoice` | `InvoiceRepository` | `InvoiceDtos`, `InvoiceController`, `InvoiceCrudController` | `invoices`, `myInvoices` |
| T29 | `InvoiceItem` | `InvoiceItemRepository` | `InvoiceDtos`, `InvoiceServiceImpl` (không có controller riêng) | chi tiết hóa đơn |
| T30 | `Notification` | `NotificationRepository` | `NotificationDtos`, `NotificationController`, `NotificationCrudController` | `notifications`, `NotificationInboxPage` |
| T31 | `NotificationRead` | `NotificationReadRepository` | `NotificationDtos`, `NotificationController` | `NotificationInboxPage` |
| T32 | `StatusHistory` | `StatusHistoryRepository` | `StatusHistoryService` (không có controller riêng) | `StatusTimeline` |
| T33 | `AuditLog` | `AuditLogRepository` | `AuditController` | `audit` |
| T34 | Không có | Không có | Không có | Không có |
| T35 | Không có | Không có | Không có | Không có |
| T36 | Không có (JDBC native) | Không có | `RolePermissionController` | `RolePermissionsPage` |

Ghi chú truy vết:

- T12: UI chưa có trường quota; không kết luận màn đã hỗ trợ.
- T34/T35: không có entity/repository/controller; chỉ tồn tại ở schema.
- T36: không có entity; `RolePermissionServiceImpl` dùng `JdbcTemplate`, không dùng repository JPA.

## 7. Bảng mới/parallel ngoài baseline 36

### NEW — `user_preference` (DMS-PREF-01)

- **Trạng thái:** `NEW / PARALLEL` — không thuộc baseline V1–V7 và **không tính** trong 36 bảng.
- **Nguồn:** `V8__user_preference.sql` và các file `UserPreference*` đang ở working tree tại thời điểm lập tài liệu; chưa commit vào baseline `266f588`.
- **Owner:** account.
- **Mục đích:** sở thích giao diện 1-1 theo tài khoản (theme mode, preset, mật độ bảng), có version chống ghi đè.
- **Khóa/ràng buộc:** PK/FK `user_id` → T02; CHECK `theme_mode` light/dark; CHECK `theme_preset` thuộc 6 preset; `table_dense` mặc định false; CHECK `version >= 0`; không dùng `ON DELETE CASCADE`.
- **Vòng đời:** upsert theo người đang đăng nhập; khóa dòng, kiểm version, tăng version; không có API xóa; tài khoản còn thì không xóa cứng.
- **Service/UI:** `UserPreferenceServiceImpl`, `UserPreferenceController`; `themeStore`/`UserMenu` phía frontend; test `UserPreferenceIntegrationTest`.
- **Việc phải làm khi merge:** giữ số V8 nếu baseline chưa có migration mới hơn; manifest V1–V7 vẫn là 36 bảng, `user_preference` là bảng thứ 37 của working tree. `DmsApplicationTests` đã cập nhật kỳ vọng 38 bảng base (36 + `user_preference` + Flyway history). Không sửa V1–V7.

## 8. Kiểm chứng tài liệu (read-only)

Các lệnh dưới đây chỉ đọc source và file manifest; không chạy ứng dụng, không kết nối DB.

### 8.1. Đếm bảng từ migration

```bash
grep -h -E '^CREATE TABLE' DMS/backend/src/main/resources/db/migration/V[1-7]__*.sql \
  | sed -E 's/^CREATE TABLE ([A-Za-z_]+).*/\1/' \
  | tr '[:upper:]' '[:lower:]' | sort | uniq -c
```

Kết quả thực tế: **36 dòng**, mỗi dòng count `1`; tổng **36**. Theo file: V1 **33**, V2 **2**, V5 **1**, V3/V4/V6/V7 **0**.

### 8.2. Đếm mục inventory trong tài liệu

```bash
grep -E '^### T[0-9]{2} — `[a-z_]+`' "Docs/00 Project Management/dms-data-manifest-36-tables.md" \
  | sed -E 's/^### T[0-9]{2} — `([a-z_]+)`.*/\1/' | sort | uniq -c
```

Kết quả thực tế: **36 dòng**, mỗi dòng count `1`; không có tên trùng.

### 8.3. Đối chiếu danh sách migration với inventory

```bash
diff \
  <(grep -h -E '^CREATE TABLE' DMS/backend/src/main/resources/db/migration/V[1-7]__*.sql \
      | sed -E 's/^CREATE TABLE ([A-Za-z_]+).*/\1/' | tr '[:upper:]' '[:lower:]' | sort) \
  <(grep -E '^### T[0-9]{2} — `[a-z_]+`' "Docs/00 Project Management/dms-data-manifest-36-tables.md" \
      | sed -E 's/^### T[0-9]{2} — `([a-z_]+)`.*/\1/' | sort)
```

Kết quả thực tế: exit code `0`, không có output — hai danh sách trùng khớp 36/36.

### 8.4. Kiểm mỗi tên bảng xuất hiện đúng một lần

```bash
for t in $(grep -h -E '^CREATE TABLE' DMS/backend/src/main/resources/db/migration/V[1-7]__*.sql \
             | sed -E 's/^CREATE TABLE ([A-Za-z_]+).*/\1/' | tr '[:upper:]' '[:lower:]' | sort); do
  printf '%s %s\n' "$t" "$(grep -o -w "$t" "Docs/00 Project Management/dms-data-manifest-36-tables.md" | wc -l)"
done
```

Kết quả thực tế: **36 dòng**, tất cả count `1`; không có tên xuất hiện 0 hoặc >1 lần.

### 8.5. Đếm object phụ

```bash
grep -h -E '^CREATE (VIEW|SEQUENCE|TRIGGER)' DMS/backend/src/main/resources/db/migration/V[1-7]__*.sql \
  | awk '{print $2}' | sort | uniq -c
```

Kết quả thực tế: **6 view**, **3 sequence**, **1 trigger**. Hàm: **2** (`fn_audit_log_append_only`, `fn_next_code`).

### 8.6. Đếm entity và resource UI

```bash
grep -rl '@Table(name' DMS/backend/src/main/java/vn/edu/tlu/dms/*/domain/*.java | wc -l
grep -rl '@Table(name' DMS/backend/src/main/java/vn/edu/tlu/dms/*/domain/*.java \
  | grep -v 'UserPreference.java' | wc -l
awk '/^export const resources/{f=1;next} f&&/^  [a-zA-Z]+: \{/{c++} END{print c}' \
  DMS/frontend/src/features/crud/resources.ts
```

Kết quả thực tế: **34 entity trong working tree**, trong đó **33 entity baseline** (sau khi loại file của bảng NEW); **29 resource**. Không có nghĩa 33/36 bảng đã được kiểm chứng runtime; chỉ là độ phủ source.

### 8.7. Giới hạn kiểm chứng

- Không chạy Maven, không chạy test, không khởi động PostgreSQL/Testcontainers.
- Không kiểm tra migration đã áp dụng trên DB nào.
- Không đối chiếu `information_schema` thật; con số 38 bảng base chỉ là kỳ vọng trong test source (sau khi thêm `user_preference` V8).
- `NOT_VERIFIED` không có nghĩa chức năng hỏng; chỉ có nghĩa chưa có bằng chứng chạy/owner xác nhận trong phạm vi ticket này.

## 9. Việc tiếp theo để đóng DMS-DATA-01

1. Owner từng module xác nhận cột “Module owner”, “Service/port ghi”, “Vòng đời”, “CRUD UI được phép”.
2. Chốt GAP-01..GAP-14, đặc biệt GAP-01, GAP-04, GAP-05, GAP-06, GAP-08, GAP-09.
3. Bổ sung test trực tiếp cho các bảng chưa có bằng chứng; chạy test trên DB sạch riêng và ghi exit code/test summary.
4. Sau khi merge bảng mới, cập nhật số bảng, manifest và test đếm bảng cùng một thay đổi.
5. Chỉ chuyển trạng thái khỏi `DRAFT` khi có reviewer độc lập và bằng chứng test thực tế.

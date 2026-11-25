# Ma trận truy vết và baseline DMS (DMS-STD-01)

> **Trạng thái tài liệu:** `BASELINE_SNAPSHOT` — chỉ ghi nhận bằng chứng đọc mã tại một thời điểm, **không phải** chứng nhận nghiệm thu, không phải báo cáo hoàn thành.
>
> **Ngày lập:** 31/10/2026, 10:36 +07:00.
>
> **Người lập:** Qoder (chỉ đọc mã và tài liệu), ticket **DMS-STD-01** theo kế hoạch `ke-hoach-tong-the-chuan-hoa-dms-theo-hr-v5.md` mục 9.3 và 12.
>
> **Phạm vi:** route/portal, 29 resource frontend, FR trong `Docs/01 Requirements/SRS-DMS.md` v0.14 và AT-01–AT-33. Không chạy npm/Maven, không chạy Docker, không mở trình duyệt trong lượt này.

## 1. Baseline và giới hạn môi trường

### 1.1. Mốc Git

| Hạng mục | Giá trị tại thời điểm lập | Ghi chú |
|---|---|---|
| Repository | `D:\Dormitory-management-system` | Git root là thư mục repo |
| Branch | `develop-quyennnv` | Tracking `origin/develop-quyennnv`, ahead 16 commit |
| HEAD SHA | `266f5886fda26496ac44baf93b4137e673c81ea0` (`266f588`) | Commit `docs(account): ghi báo cáo sửa import Excel của Nguyễn Văn Séc`, 30/10/2026 17:29 +07 |
| Working tree | **Có thay đổi chưa commit** | Ảnh chụp 10:36: 22 file đã sửa + 10 file chưa theo dõi; cây làm việc tiếp tục thay đổi trong lúc lập do agent khác đang sửa frontend/backend |
| Stash | `stash@{0}: On develop: qoder-preserve-maintenance-before-quyennnv-20261031` | Giữ nguyên, không áp dụng/xóa trong lượt này |
| CI | Không chạy trên working tree | Không có kết quả CI cho nội dung chưa commit |

**Cảnh báo quan trọng:** SHA `266f588` là mốc của commit gần nhất, **không đại diện** cho nội dung working tree. Các kết quả test/build dưới đây là bằng chứng lịch sử trên một trạng thái working tree khác, không phải chứng nhận cho cây hiện tại.

### 1.2. Nhóm thay đổi chưa commit (chỉ mô tả, không coi là đã commit)

| Nhóm | File tiêu biểu | Trạng thái |
|---|---|---|
| Làm lại giao diện/theme | `DMS/frontend/src/app/App.tsx`, `app/theme.ts`, `app/themeStore.ts` (mới), `index.css`, `app/layouts/SidebarShell.tsx`, `StudentLayout.tsx`, `TechnicianLayout.tsx`, `components/layout/UserMenu.tsx`, `components/ui/DataTable.tsx`, `pages/CrudPage.tsx`, `features/crud/resources.ts`, `pages/staff/PendingItemsPage.tsx` | Chưa commit; lint/build frontend pass 31/10/2026; chưa có ảnh browser |
| Tùy chọn giao diện theo tài khoản | Backend mới: `account/api/UserPreferenceController.java`, `domain/UserPreference.java`, `repository/UserPreferenceRepository.java`, `service/UserPreferenceService.java`, `service/impl/UserPreferenceServiceImpl.java`, `db/migration/V8__user_preference.sql`, test `UserPreferenceIntegrationTest.java`; frontend: `services/api/preferences.ts`, `app/themeStore.ts`; sửa `AccountDtos.java`, `App.tsx`, `UserMenu.tsx`, `theme.ts` | Chưa commit; 6 test pass + full suite 342 pass; lint/build pass |
| Sửa nghiệp vụ đang làm | `billing/service/impl/UtilityReadingServiceImpl.java` (DEC-BIL-01), `contract/domain/PendingContractExpired.java`, `contract/service/impl/ContractLifecycleServiceImpl.java`, `registration/service/impl/PendingContractExpiredListener.java`, `common/constants/DmsConstants.java`; test `CrudApiIntegrationTest.java`, `SystemJobsIntegrationTest.java` | Chưa commit; đã chạy trong full suite 342 test pass, chưa review độc lập riêng |
| Tài liệu đang đồng bộ | `Docs/01 Requirements/SRS-DMS.md` (v0.14), `Docs/02 External Design/API Specification/API-Spec-DMS.md`, `Docs/02 External Design/Database Design/DB-Design-DMS.md` | Chưa commit |
| Kế hoạch nguồn | `Docs/00 Project Management/ke-hoach-tong-the-chuan-hoa-dms-theo-hr-v5.md` | Chưa theo dõi Git (`??`), chỉ đọc |

Không có commit/push/stash/reset nào được thực hiện. Số lượng file có thể tăng sau ảnh chụp vì agent khác đang ghi đồng thời.

### 1.3. Giới hạn truy cập trong lượt này

- **Không có Docker/PostgreSQL test riêng** trong phiên này; không kiểm tra schema thật, không chạy migration V8, không xác nhận 36 bảng.
- **Không có browser baseline** (360/768/1440), không kiểm tra console/network, bàn phím, zoom, contrast.
- **Không chạy `npm run lint`, `npm run build`, `mvnw test/verify`** vì agent khác đang sửa frontend và cây làm việc đang thay đổi. Chỉ đọc file.
- Không có frontend test runner; không có E2E trong repo.
- Log test backend là bằng chứng lịch sử, không phải kết quả chạy lại trên HEAD/working tree hiện tại.

## 2. Quy ước đọc ma trận

- **Route**: đếm mục route lá trong `DMS/frontend/src/app/router/index.tsx` và bốn file route vai trò; route Admin dùng lại route Staff được tính riêng.
- **Resource**: đếm khóa cấp một trong `resources` tại `DMS/frontend/src/features/crud/resources.ts:284`.
- **Permission**: route được `RequireRole` kiểm tra role và `permissionsForPath` (`app/guards/RequireRole.tsx:96-120`); thao tác trong màn do `CrudPage` kiểm tra `READ/CREATE/UPDATE/DELETE` (`pages/CrudPage.tsx:407-427`). Cột quyền ghi mã màn hình, không thay thế kiểm tra action của backend.
- **Trạng thái bằng chứng**:
  - `CÓ`: có code/test được dẫn `file:dòng` hoặc tên test cụ thể.
  - `MỘT PHẦN`: chỉ một phần FR/AT có bằng chứng, phần còn lại chưa thấy.
  - `CẦN ĐỐI CHIẾU`: chưa tìm thấy bằng chứng đủ chắc; không suy đoán.
  - `KHÔNG THẤY`: không thấy implementation trong phạm vi đã đọc.
  - `NOT_VERIFIED`: chưa kiểm chứng UI/browser hoặc chưa đo NFR.
- **UI verification mặc định = `NOT_VERIFIED`** cho mọi hàng, trừ khi có ảnh/log browser thật (hiện không có).

## 3. Inventory route

### 3.1. Tổng hợp đếm

| Nhóm | Số route | Ghi chú |
|---|---:|---|
| Xác thực/catch-all ngoài portal | 4 | `/login`, `/forgot-password`, `/change-password`, `*` |
| Student portal | 17 | Gồm index `/student` |
| Staff portal | 30 | Gồm index `/staff` |
| Technician portal | 5 | Gồm index `/technician` |
| Admin portal | 38 | 10 route riêng + 28 route Staff dùng lại (bỏ `students` và index) |
| **Tổng route trong 4 portal** | **90** | 17 + 30 + 5 + 38 |
| **Tổng route entry (không tính 4 node gốc portal)** | **94** | 90 + 4 xác thực/catch-all |
| Tổng nút route nếu tính cả 4 gốc portal | 98 | 94 + `/student`, `/staff`, `/technician`, `/admin` |

Đếm bằng Grep: `path: '` trong `app/router` = 66 chuỗi literal; `index: true` = 4; Admin spread thêm 28 route từ Staff. Admin có 28 route dùng lại vì `adminRoutes.tsx:36-38` spread `staffRoutes.children[0].children` và loại `index`, `students`.

### 3.2. Route xác thực và gốc portal

| Đường dẫn | Portal | Component | Gate |
|---|---|---|---|
| `/login` | Công khai | `LoginPage` | Không |
| `/forgot-password` | Công khai | `ForgotPasswordPage` | Không |
| `/change-password` | Mọi vai trò | `ChangePasswordPage` | `RequireRole allowMustChangePassword` (đã đăng nhập, cho phép tài khoản bắt buộc đổi) |
| `*` | Mọi vai trò | `HomeRedirect` | `RequireRole` (đã đăng nhập → trang chủ vai trò; chưa đăng nhập → `/login`) |
| `/student` | Student | `RequireRole STUDENT` → `StudentLayout` | `PROFILE_READ` |
| `/staff` | Staff | `RequireRole STAFF` → `StaffLayout` | `PROFILE_READ` |
| `/technician` | Technician | `RequireRole TECHNICIAN` → `TechnicianLayout` | `PROFILE_READ` |
| `/admin` | Admin | `RequireRole ADMIN` → `AdminLayout` | `USER_READ` |

`RequireRole.tsx:96-104` cho ADMIN đi vào route Staff/Technician (không cho Student); route Admin vẫn là bản sao dưới `/admin`. Các bảng dưới đây liệt kê route theo khai báo, không liệt kê truy cập chéo của Admin.

Bốn layout: `StudentLayout` dùng menu ngang từ 640 px và bottom nav dưới 640 px, lọc mục theo quyền (`StudentLayout.tsx:32-33`, `96-114`); `TechnicianLayout` chỉ có topbar, không sidebar; `StaffLayout` và `AdminLayout` cùng dùng `SidebarShell` (sidebar từ 1024 px, Drawer dưới 1024 px) với nhóm menu từ `constants/navigation.ts`. Menu chỉ là lớp hiển thị; route vẫn bị `RequireRole`/`permissionsForPath` chặn và API vẫn kiểm tra lại.

### 3.3. Student — 17 route

| # | Đường dẫn | Component | Gate (role + quyền màn) |
|---:|---|---|---|
| 1 | `/student` | `StudentHomePage` | `STUDENT` + `PROFILE_READ` |
| 2 | `/student/room` | `CrudPage resource="myRoom"` | `STUDENT` + `ROOM_READ` |
| 3 | `/student/applications` | `CrudPage resource="myApplications"` | `STUDENT` + `ROOM_APPLICATION_READ` |
| 4 | `/student/applications/:id` | `StudentApplicationDetailPage` | `STUDENT` + `ROOM_APPLICATION_READ` |
| 5 | `/student/contracts` | `CrudPage resource="myContracts"` | `STUDENT` + `CONTRACT_READ` |
| 6 | `/student/contracts/:id` | `StudentContractDetailPage` | `STUDENT` + `CONTRACT_READ` |
| 7 | `/student/requests` | `CrudTabsPage` (Gia hạn, Chuyển phòng) | `STUDENT` + `CONTRACT_EXTENSION_READ` hoặc `ROOM_TRANSFER_READ` |
| 8 | `/student/requests/transfers/:id` | `TransferRequestPage` | `STUDENT` + `ROOM_TRANSFER_READ` |
| 9 | `/student/requests/extensions/:id` | `StudentExtensionDetailPage` | `STUDENT` + `CONTRACT_EXTENSION_READ` |
| 10 | `/student/invoices` | `CrudPage resource="myInvoices"` | `STUDENT` + `INVOICE_READ` |
| 11 | `/student/deposit` | `CrudPage resource="myDeposits"` | `STUDENT` + `DEPOSIT_READ` |
| 12 | `/student/maintenance` | `CrudPage resource="maintenance"` | `STUDENT` + `MAINTENANCE_READ` |
| 13 | `/student/maintenance/:id` | `StudentMaintenanceDetailPage` | `STUDENT` + `MAINTENANCE_READ` |
| 14 | `/student/summer` | `CrudPage resource="summer"` | `STUDENT` + `SUMMER_REGISTRATION_READ` |
| 15 | `/student/summer/:id` | `StudentSummerChoiceDetailPage` | `STUDENT` + `SUMMER_REGISTRATION_READ` |
| 16 | `/student/notifications` | `NotificationInboxPage` | `STUDENT` + `NOTIFICATION_READ` (chuông chỉ hiện khi có quyền) |
| 17 | `/student/profile` | `CrudPage resource="profile"` | `STUDENT` + `PROFILE_READ` |

### 3.4. Staff — 30 route

| # | Đường dẫn | Component | Gate (role + quyền màn) |
|---:|---|---|---|
| 1 | `/staff` | `HomeRedirect` → dashboard | `STAFF` + `PROFILE_READ` |
| 2 | `/staff/dashboard` | `StaffHomePage` | `STAFF` + `DASHBOARD_READ` |
| 3 | `/staff/profile` | `CrudPage resource="profile"` | `STAFF` + `PROFILE_READ` |
| 4 | `/staff/requests` | `CrudTabsPage` (Gia hạn, Chuyển phòng) | `STAFF` + `CONTRACT_EXTENSION_READ` hoặc `ROOM_TRANSFER_READ` |
| 5 | `/staff/applications` | `CrudPage resource="applications"` | `STAFF` + `ROOM_APPLICATION_READ` |
| 6 | `/staff/applications/:id` | `StaffApplicationDetailPage` | `STAFF` + `ROOM_APPLICATION_READ` |
| 7 | `/staff/rooms` | `CrudPage resource="rooms"` | `STAFF` + `ROOM_READ` |
| 8 | `/staff/rooms/:id` | `RoomDetailPage` | `STAFF` + `ROOM_READ` |
| 9 | `/staff/transfers/:id` | `TransferReviewPage` | `STAFF` + `ROOM_TRANSFER_READ` |
| 10 | `/staff/students` | `CrudPage resource="students"` | `STAFF` + `STUDENT_READ` |
| 11 | `/staff/contracts` | `CrudPage resource="contracts"` | `STAFF` + `CONTRACT_READ` |
| 12 | `/staff/contracts/:id` | `StaffContractDetailPage` | `STAFF` + `CONTRACT_READ` |
| 13 | `/staff/contracts/:id/move-out` | `MoveOutPage` | `STAFF` + `CONTRACT_READ` (thao tác trả phòng kiểm thêm quyền `MOVE_OUT_*`) |
| 14 | `/staff/extensions/:id` | `StaffExtensionDetailPage` | `STAFF` + `CONTRACT_EXTENSION_READ` |
| 15 | `/staff/summer` | `CrudPage resource="summer"` | `STAFF` + `SUMMER_REGISTRATION_READ` |
| 16 | `/staff/summer/:id` | `StaffSummerChoiceDetailPage` | `STAFF` + `SUMMER_REGISTRATION_READ` |
| 17 | `/staff/storage` | `StoragePage` | `STAFF` + `STORAGE_READ` |
| 18 | `/staff/utilities` | `CrudPage resource="readings"` | `STAFF` + `UTILITY_READING_READ` |
| 19 | `/staff/utilities/prices` | `CrudPage resource="rates"` | `STAFF` + `UTILITY_RATE_READ` |
| 20 | `/staff/invoices` | `CrudPage resource="invoices"` | `STAFF` + `INVOICE_READ` |
| 21 | `/staff/invoices/issue` | `CrudPage resource="invoices"` (phát hành) | `STAFF` + `INVOICE_READ` (phát hành kiểm `INVOICE_CREATE`) |
| 22 | `/staff/deposits` | `CrudPage resource="deposits"` | `STAFF` + `DEPOSIT_READ` |
| 23 | `/staff/maintenance` | `CrudPage resource="maintenance"` | `STAFF` + `MAINTENANCE_READ` |
| 24 | `/staff/maintenance/:id` | `StaffMaintenanceDetailPage` | `STAFF` + `MAINTENANCE_READ` |
| 25 | `/staff/notifications` | `CrudPage resource="notifications"` | `STAFF` + `NOTIFICATION_READ` |
| 26 | `/staff/inbox` | `NotificationInboxPage` | `STAFF` + `NOTIFICATION_READ` |
| 27 | `/staff/pending-items` | `PendingItemsPage` | `STAFF` + `ROOM_APPLICATION_READ` hoặc `CONTRACT_READ` |
| 28 | `/staff/room-types` | `CrudPage resource="roomTypes"` | `STAFF` + `ROOM_TYPE_READ` |
| 29 | `/staff/semesters` | `CrudPage resource="semesters"` | `STAFF` + `SEMESTER_READ` |
| 30 | `/staff/registration-periods` | `CrudPage resource="periods"` | `STAFF` + `REGISTRATION_PERIOD_READ` |

### 3.5. Technician — 5 route

| # | Đường dẫn | Component | Gate (role + quyền màn) |
|---:|---|---|---|
| 1 | `/technician` | `HomeRedirect` → tasks | `TECHNICIAN` + `PROFILE_READ` |
| 2 | `/technician/tasks` | `CrudPage resource="maintenance"` | `TECHNICIAN` + `MAINTENANCE_READ` |
| 3 | `/technician/tasks/:id` | `TaskDetailPage` | `TECHNICIAN` + `MAINTENANCE_READ` |
| 4 | `/technician/profile` | `CrudPage resource="profile"` | `TECHNICIAN` + `PROFILE_READ` |
| 5 | `/technician/notifications` | `NotificationInboxPage` | `TECHNICIAN` + `NOTIFICATION_READ` |

### 3.6. Admin — 38 route (10 riêng + 28 dùng lại Staff)

**10 route riêng:**

| # | Đường dẫn | Component | Gate (role + quyền màn) |
|---:|---|---|---|
| 1 | `/admin` | `Navigate to="users"` | `ADMIN` + `USER_READ` |
| 2 | `/admin/users` | `CrudPage resource="users"` | `ADMIN` + `USER_READ` |
| 3 | `/admin/units` | `CrudPage resource="units"` | `ADMIN` + `MANAGEMENT_UNIT_READ` |
| 4 | `/admin/buildings` | `CrudPage resource="buildings"` | `ADMIN` + `BUILDING_READ` |
| 5 | `/admin/import` | `StudentImportPage` | `ADMIN` + `STUDENT_READ` |
| 6 | `/admin/aggregate-permissions` | `CrudPage resource="users"` | `ADMIN` + `USER_READ` |
| 7 | `/admin/role-permissions` | `RolePermissionsPage` | `ADMIN` + `ROLE_PERMISSION_READ` |
| 8 | `/admin/priority-categories` | `CrudPage resource="priorities"` | `ADMIN` + `PRIORITY_CATEGORY_READ` |
| 9 | `/admin/configs` | `CrudPage resource="configs"` | `ADMIN` + `CONFIG_READ` |
| 10 | `/admin/audit-logs` | `CrudPage resource="audit"` | `ADMIN` + `AUDIT_READ` |

**28 route dùng lại Staff (đường dẫn đổi tiền tố `/staff` → `/admin`):**

| # | Đường dẫn | Component | Gate (role + quyền màn) |
|---:|---|---|---|
| 11 | `/admin/dashboard` | `StaffHomePage` | `ADMIN` + `DASHBOARD_READ` |
| 12 | `/admin/profile` | `CrudPage resource="profile"` | `ADMIN` + `PROFILE_READ` |
| 13 | `/admin/requests` | `CrudTabsPage` | `ADMIN` + `CONTRACT_EXTENSION_READ` hoặc `ROOM_TRANSFER_READ` |
| 14 | `/admin/applications` | `CrudPage resource="applications"` | `ADMIN` + `ROOM_APPLICATION_READ` |
| 15 | `/admin/applications/:id` | `StaffApplicationDetailPage` | `ADMIN` + `ROOM_APPLICATION_READ` |
| 16 | `/admin/rooms` | `CrudPage resource="rooms"` | `ADMIN` + `ROOM_READ` |
| 17 | `/admin/rooms/:id` | `RoomDetailPage` | `ADMIN` + `ROOM_READ` |
| 18 | `/admin/transfers/:id` | `TransferReviewPage` | `ADMIN` + `ROOM_TRANSFER_READ` |
| 19 | `/admin/contracts` | `CrudPage resource="contracts"` | `ADMIN` + `CONTRACT_READ` |
| 20 | `/admin/contracts/:id` | `StaffContractDetailPage` | `ADMIN` + `CONTRACT_READ` |
| 21 | `/admin/contracts/:id/move-out` | `MoveOutPage` | `ADMIN` + `CONTRACT_READ` |
| 22 | `/admin/extensions/:id` | `StaffExtensionDetailPage` | `ADMIN` + `CONTRACT_EXTENSION_READ` |
| 23 | `/admin/summer` | `CrudPage resource="summer"` | `ADMIN` + `SUMMER_REGISTRATION_READ` |
| 24 | `/admin/summer/:id` | `StaffSummerChoiceDetailPage` | `ADMIN` + `SUMMER_REGISTRATION_READ` |
| 25 | `/admin/storage` | `StoragePage` | `ADMIN` + `STORAGE_READ` |
| 26 | `/admin/utilities` | `CrudPage resource="readings"` | `ADMIN` + `UTILITY_READING_READ` |
| 27 | `/admin/utilities/prices` | `CrudPage resource="rates"` | `ADMIN` + `UTILITY_RATE_READ` |
| 28 | `/admin/invoices` | `CrudPage resource="invoices"` | `ADMIN` + `INVOICE_READ` |
| 29 | `/admin/invoices/issue` | `CrudPage resource="invoices"` | `ADMIN` + `INVOICE_READ` |
| 30 | `/admin/deposits` | `CrudPage resource="deposits"` | `ADMIN` + `DEPOSIT_READ` |
| 31 | `/admin/maintenance` | `CrudPage resource="maintenance"` | `ADMIN` + `MAINTENANCE_READ` |
| 32 | `/admin/maintenance/:id` | `StaffMaintenanceDetailPage` | `ADMIN` + `MAINTENANCE_READ` |
| 33 | `/admin/notifications` | `CrudPage resource="notifications"` | `ADMIN` + `NOTIFICATION_READ` |
| 34 | `/admin/inbox` | `NotificationInboxPage` | `ADMIN` + `NOTIFICATION_READ` |
| 35 | `/admin/pending-items` | `PendingItemsPage` | `ADMIN` + `ROOM_APPLICATION_READ` hoặc `CONTRACT_READ` |
| 36 | `/admin/room-types` | `CrudPage resource="roomTypes"` | `ADMIN` + `ROOM_TYPE_READ` |
| 37 | `/admin/semesters` | `CrudPage resource="semesters"` | `ADMIN` + `SEMESTER_READ` |
| 38 | `/admin/registration-periods` | `CrudPage resource="periods"` | `ADMIN` + `REGISTRATION_PERIOD_READ` |

Admin không có route `/admin/students`; hồ sơ sinh viên nằm trong tab của `/admin/import` dùng resource `adminStudents`.

## 4. Inventory resource

`resources` có **29 khóa** tại `resources.ts:284` (Grep `^  [a-zA-Z][a-zA-Z0-9]*: \{$` → 29). Kế hoạch ghi 29, khớp thực tế.

**Ký hiệu mode:** `R` = readOnly; `-C` = noCreate; `-D` = noDelete; `S` = singleton; `L` = useListRecordForEdit; `I` = inlineDetail; `F` = finance; `M` = createFormat multipart; `E:<trạng thái>` = editStates; `X:<action>` = deleteAction; `DTR` = có `detailRoutes`; `id:<khóa>` = idKey. Mặc định không ghi là tạo/sửa/xóa theo quyền chuẩn.

| # | Key (dòng) | Permission | Endpoint đọc | Endpoint ghi | Mode | Detail | Ghi chú |
|---:|---|---|---|---|---|---|---|
| 1 | `units` (285) | `MANAGEMENT_UNIT` | `/management-units` | `/admin/management-units` | CRUD; L | Modal | Danh mục đơn vị; `writeEndpoint` riêng |
| 2 | `buildings` (294) | `BUILDING` | `/buildings` | `/admin/buildings` | CRUD; L | Modal | Có `depositAmount` theo tòa |
| 3 | `roomTypes` (313) | `ROOM_TYPE` | `/room-types` | = endpoint | CRUD; L; default `active=true`; `bedCount` 1–12 | Modal | Giá HK/hè; AT-33 ở backend |
| 4 | `semesters` (336) | `SEMESTER` | `/semesters` | = endpoint | CRUD; L | Modal | HK1/HK2/SUMMER |
| 5 | `priorities` (356) | `PRIORITY_CATEGORY` | `/priority-categories` | `/admin/priority-categories` | CRUD; L; default `active=true` | Modal | Trọng số ưu tiên |
| 6 | `users` (376) | `USER` | `/admin/users` | = endpoint | CRUD; `X:lock`; filter role/unit/locked; reset PW; cross-unit | Modal | Xóa = khóa; không xóa tài khoản có lịch sử; mật khẩu tạm một lần |
| 7 | `students` (432) | `STUDENT` | `/students` | = endpoint (Admin ghi `/admin/students`) | CRUD; `deleteEndpoint=/admin/students`; `editFields` bỏ mã/tên/giới tính | Modal | Staff sửa liên hệ trong đơn vị; Admin xóa hồ sơ chưa có lịch sử |
| 8 | `adminStudents` (450) | `STUDENT` | `/admin/students` | = endpoint | CRUD | Modal | Chỉ dùng trong tab hồ sơ của `StudentImportPage`; không có route riêng |
| 9 | `rooms` (464) | `ROOM` | `/rooms` | = endpoint | CRUD; filter tòa/loại/tầng/giới tính/trạng thái; DTR Staff/Admin | **Modal** (DTR khai báo nhưng chưa được `CrudPage` đọc) | Giường/trưởng phòng ở `RoomDetailPage` |
| 10 | `periods` (502) | `REGISTRATION_PERIOD` | `/registration-periods` | = endpoint | CRUD; L; `closeAtOnlyAfterOpen` | Modal | Đợt đã mở chỉ sửa `closeAt` |
| 11 | `applications` (527) | `ROOM_APPLICATION` | `/applications` | = endpoint | CRUD; `E:DRAFT`; `X:cancel`; DTR Staff/Admin | **Modal** (DTR chưa dùng) | Duyệt/phân giường ở trang detail |
| 12 | `myApplications` (585) | `ROOM_APPLICATION` | `/me/applications` (detail `/applications`) | `/applications` | CRUD; `E:DRAFT`; `X:cancel`; DTR Student | **Modal** (DTR chưa dùng) | Chỉ sửa đơn nháp; xem giường giữ/hạn |
| 13 | `contracts` (619) | `CONTRACT` | `/contracts` | = endpoint | CRUD; `E:PENDING`; `detailColumns` cọc; DTR Staff/Admin | **Modal** (DTR chưa dùng) | Kích hoạt/trả phòng/gửi đồ ở detail |
| 14 | `myContracts` (664) | `CONTRACT` | `/me/contracts` (detail `/contracts`) | — | **R**; DTR Student | **Modal** (DTR chưa dùng) | Chỉ xem |
| 15 | `extensions` (679) | `CONTRACT_EXTENSION` | `/extension-requests` | = endpoint | CRUD; `E:PENDING`; `X:cancel`; DTR Staff/Admin/Student | **Modal** (DTR chưa dùng) | Hủy bằng action, không xóa cứng |
| 16 | `transfers` (724) | `ROOM_TRANSFER` | `/transfer-requests` | = endpoint | CRUD; `E:PENDING`; `X:cancel`; DTR Staff/Admin/Student | **Modal** (DTR chưa dùng) | Duyệt chuyển phòng ở detail |
| 17 | `summer` (765) | `SUMMER_REGISTRATION` | `/summer-choices` | = endpoint | CRUD; `E:PENDING`; DTR Staff/Admin/Student | **Modal** (DTR chưa dùng) | STAY tự gia hạn hợp đồng |
| 18 | `readings` (809) | `UTILITY_READING` | `/utility-readings` | = endpoint | CRUD; F; `keepCreateOnlyOnEdit`; khóa sửa/xóa khi `invoiced`/`hasFollowingMonth` | Modal | Thay/reset đồng hồ có trường riêng; DEC-BIL-01 đang sửa |
| 19 | `rates` (882) | `UTILITY_RATE` | `/utility-rates` | = endpoint | CRUD; F; L | Modal | Giá đã hiệu lực giữ lịch sử |
| 20 | `invoices` (918) | `INVOICE` | `/invoices` | = endpoint | CRUD; F; `E:UNPAID`; `createOnly` loại/hồ sơ/kỳ; filter loại/trạng thái | Modal | Tab items + xác nhận thanh toán; PAID khóa |
| 21 | `myInvoices` (959) | `INVOICE` | `/me/invoices` (detail `/invoices`) | — | **R**; F; filter loại/trạng thái | Modal | Chỉ xem hóa đơn của mình/phòng |
| 22 | `deposits` (987) | `DEPOSIT` | `/deposit-balances` (detail `/contracts`) | — | **R**; F; `id:contractId` | **Ledger modal** | Sổ cọc chỉ đọc; thu/trừ/bù/hoàn là nghiệp vụ riêng |
| 23 | `myDeposits` (1010) | `DEPOSIT` | `/me/deposit-balances` (detail `/contracts`) | — | **R**; F; `id:contractId` | **Ledger modal** | Sổ cọc của sinh viên |
| 24 | `maintenance` (1030) | `MAINTENANCE` | `/maintenance-requests` | = endpoint | CRUD; M; `E:OPEN`; `rowOwned=requesterId`; `editFields` bỏ `roomId`; DTR cả 4 role | **Modal** (DTR chưa dùng) | Ảnh multipart, history, nghiệm thu/kết luận ở detail |
| 25 | `notifications` (1079) | `NOTIFICATION` | `/notifications/published` | `/notifications` | CRUD; `keepCreateOnlyOnEdit`; default `scope=UNIT` | Modal | UI chỉ có UNIT/BUILDING/ROOM/ALL; **thiếu FLOOR** dù backend hỗ trợ |
| 26 | `profile` (1116) | `PROFILE` | `/me/profile` | = endpoint | R+U; **S** | Modal singleton | Chỉ sửa hồ sơ chính mình; Staff/KTV/Admin chỉ phone |
| 27 | `audit` (1133) | `AUDIT` | `/admin/audit-logs` | — | **R**; filter actor/action/type/from/to; `detailColumns` before/after | Modal | Nhật ký chỉ đọc |
| 28 | `configs` (1162) | `CONFIG` | `/admin/configs` | = endpoint | R+U; `-C`; `-D`; I; L; `id:key` | **Inline** | Chỉ sửa khóa đã tồn tại |
| 29 | `myRoom` (1181) | `ROOM` | `/me/room` | — | **R**; I; L | **Inline** | Phòng/giường hiện tại của sinh viên |

**Tổng hợp mode:** 6 resource read-only (`myContracts`, `myInvoices`, `deposits`, `myDeposits`, `audit`, `myRoom`); 1 singleton (`profile`); 1 `noCreate`/`noDelete` (`configs`); 6 finance (`readings`, `rates`, `invoices`, `myInvoices`, `deposits`, `myDeposits`); 9 resource khai `detailRoutes`; 8 resource có `editStates`; 5 resource có `deleteAction` (`users`, `applications`, `myApplications`, `extensions`, `transfers`).

**Phát hiện cần chốt:** `detailRoutes` được khai ở 9 resource và định nghĩa trong `types.ts:98`, nhưng Grep toàn `frontend/src` **không thấy consumer**; `CrudPage` vẫn mở modal cho nút Xem (`CrudPage.tsx:934-951`) và chỉ `navigate` ở các action chuyên biệt (move-out, storage). Vì vậy cột Detail ghi “Modal (DTR chưa dùng)”. Các trang detail vẫn có route riêng và được `PendingItemsPage` liên kết, nhưng không mở từ danh sách CRUD. Cần ticket FE-CRUD/Integrator quyết định nối `detailRoutes` hoặc bỏ khai báo.

## 5. Ma trận FR → route/resource/test

> Trạng thái dưới đây là **mức bằng chứng code/test**, không phải nghiệm thu UI. Mọi hàng UI vẫn `NOT_VERIFIED`.

### 5.1. FR-ACC (Tài khoản & RBAC)

| FR | Nội dung ngắn | Route/resource | Test/evidence | Trạng thái |
|---|---|---|---|---|
| FR-ACC-01 | Đăng nhập Keycloak OIDC/PKCE, JWT, logout | `/login`; `RequireRole`; Keycloak | `KeycloakJwtDecoderTest` (5 test); `KeycloakAccountIntegrationTest#anonymousRequestIsRejected`, `#dmsLogoutImmediatelyRejectsPreviouslyIssuedToken` | CÓ (backend); UI NOT_VERIFIED |
| FR-ACC-02 | Khóa tạm sau 5 lần sai | Cấu hình Keycloak, không thấy code DMS | Không thấy test | CẦN ĐỐI CHIẾU |
| FR-ACC-03 | Đổi mật khẩu, bắt đổi lần đầu | `/change-password`; `RequireRole.tsx:94-95` | `KeycloakAccountIntegrationTest#lockUnlockAndPasswordResetKeepOldTokensRevokedWithoutStoringPassword` (reset, không phải change) | MỘT PHẦN |
| FR-ACC-04 | Hồ sơ cá nhân | `profile` resource; 4 route `/profile` | `CrudApiIntegrationTest#profileAndConfigurationSaveKeepTheirRestrictedIdentifiers` | CÓ |
| FR-ACC-05 | Import sinh viên Excel | `/admin/import`; `adminStudents` | `StudentImportIntegrationTest` (17 test) | CÓ |
| FR-ACC-06 | Tạo/khóa/mở/đổi role tài khoản | `/admin/users`; `users` | `KeycloakAccountIntegrationTest#changingStaffRole...`, `#promotingStaffToAdmin...`, `#lockUnlock...` | CÓ |
| FR-ACC-07 | Reset mật khẩu theo quyền | `users`, `students`, `adminStudents`; `CrudPage.tsx:662-672` | `CrudApiIntegrationTest#studentCrudReturnsTemporaryPasswordAndBlocksDeleteWhenHistoryExists` | MỘT PHẦN (AT-29 chưa có test trực tiếp) |
| FR-ACC-08 | Quyền tổng hợp liên đơn vị | `/admin/aggregate-permissions`; `users` | `CrudApiIntegrationTest#listsAndDetailsRespectUnitScopeAndAggregatePermissionDoesNotAllowWrites`; `DepositIntegrationTest#aggregateStaffCanReadButCannotWriteAnotherUnitsLedger` | CÓ |
| FR-ACC-09 | Audit log | `/admin/audit-logs`; `audit` | `CrudApiIntegrationTest#auditFiltersCombineActorActionTypeSearchAndInclusiveTimeBoundaries`, `#auditDetailsAreAdminOnlyAndShowSystemActorAndBeforeAfterValues` | CÓ |
| FR-ACC-10 | Ma trận quyền động | `/admin/role-permissions`; `RolePermissionsPage` | `KeycloakAccountIntegrationTest` nhóm matrix (version, stale, immutable, restore) | CÓ |

### 5.2. FR-ROOM (Phòng, giường, tòa)

| FR | Nội dung ngắn | Route/resource | Test/evidence | Trạng thái |
|---|---|---|---|---|
| FR-ROOM-01 | Đơn vị, tòa, mức cọc | `/admin/units`, `/admin/buildings`; `units`, `buildings` | `CrudApiIntegrationTest#catalogLifecycleUsesAdminPermissionsAndSaveKeepsUnsentFields` | MỘT PHẦN (hiệu lực mức cọc với hợp đồng sau chưa có test riêng) |
| FR-ROOM-02 | Loại phòng, giá, không đổi `bed_count` khi đã gán | `/staff/room-types`; `roomTypes` | `CrudApiIntegrationTest#changingRoomTypeSynchronizesBedsAndRoomTypeCapacityIsGuarded`; `#shrinkingRoomTypeRetiresBedsWithHistoryAndGrowingReactivatesThem` | CÓ |
| FR-ROOM-03 | Thêm/sửa phòng | `/staff/rooms`; `rooms` | `CrudApiIntegrationTest#roomCrudCreatesBedsAndSupportsPartialUpdates` | CÓ |
| FR-ROOM-04 | Tự tạo/bớt giường | `/staff/rooms/:id`; `RoomDetailPage` | `CrudApiIntegrationTest#roomCrudCreatesBeds...`, `#bedSaveCreatesThenUpdatesSameBedWithoutChangingItsRoom` | CÓ |
| FR-ROOM-05 | Đổi trạng thái phòng/giường | `RoomDetailPage`; `RoomController.java:55`; `BedController.java:54` | `BedOccupancyIntegrationTest#occupyRejectsInactiveBedOrRoomAndGenderMismatch` (chỉ kiểm trạng thái khi chiếm) | MỘT PHẦN |
| FR-ROOM-06 | Không xóa phòng/giường có lịch sử | `rooms`; backend delete | `CrudApiIntegrationTest#shrinkingRoomTypeRetiresBedsWithHistoryAndGrowingReactivatesThem` | MỘT PHẦN |
| FR-ROOM-07 | Tra cứu phòng, số giường trống/giữ/ở | `/staff/rooms`; filter trong `resources.ts:468-481` | `DepositIntegrationTest#dashboardSeparatesHoldsAndNonOperationalBedsAndUsesConfig` | CÓ |
| FR-ROOM-08 | Sinh viên xem phòng/giường/bạn ở | `/student/room`; `myRoom` | `OverviewController.java:64`; chưa thấy test riêng | CẦN ĐỐI CHIẾU (test) |
| FR-ROOM-09 | Chỉ định trưởng phòng | `/staff/rooms/:id`; `RoomDetailPage` | `RoomLeaderIntegrationTest#leaderMustStayInRoomAndLeadOnlyOneRoom` | CÓ |
| FR-ROOM-10 | Tự gỡ trưởng phòng, cảnh báo thiếu | `RoomDetailPage`; backend | `RoomLeaderIntegrationTest#leaderHistoryIncludesManualAndAutomaticRemoval`; `MoveOutIntegrationTest#moveOutRefundsBalanceAndReleasesBed` | CÓ |
| FR-ROOM-11 | Quản lý học kỳ | `/staff/semesters`; `semesters` | `CrudApiIntegrationTest#semesterOverlapIsRejectedByPostgresConstraint` | CÓ |

### 5.3. FR-REG (Đăng ký & phân phòng)

| FR | Nội dung ngắn | Route/resource | Test/evidence | Trạng thái |
|---|---|---|---|---|
| FR-REG-01 | Tạo đợt, chỉ tiêu, đơn vị áp dụng | `/staff/registration-periods`; `periods` | `CrudApiIntegrationTest#registrationPeriodUsesCompositeMappingsAndRejectsCrossUnitWrite`, `#sharedRegistrationPeriodCannotBeRewrittenBySingleUnitStaff` | CÓ |
| FR-REG-02 | Sinh viên soạn/nộp đơn, một đơn mỗi đợt | `/student/applications`; `myApplications` | `CrudApiIntegrationTest#applicationWorkflowRecordsHistoryAndRejectsRepeatedActions`; `#adminCreatesAndProcessesApplicationOnBehalfWithoutChangingAuditActor` | CÓ |
| FR-REG-03 | Chặn nộp khi đang có hợp đồng PENDING/ACTIVE | `myApplications`; backend application policy | Không thấy test trực tiếp | CẦN ĐỐI CHIẾU |
| FR-REG-04 | Nộp/hủy đơn, APPROVED có hợp đồng bị chặn 409 | `myApplications`, `applications` | `CrudApiIntegrationTest#applicationWorkflow...`, `#approvedApplicationWithLiveContractCannotBeCancelled`; `RoomApplicationPolicyTest#approvedApplicationWithLiveContractCannotBeAssignedOrCancelled` | CÓ |
| FR-REG-05 | Tính điểm ưu tiên | `applications`; `priorities` | `ApplicationAssignmentIntegrationTest#verifyPriorityRecalculatesScoreAndRecordsAudit` | CÓ |
| FR-REG-06 | Xác nhận diện ưu tiên | `/staff/applications/:id` | `RoomApplicationPolicyTest#submittedApplicationLetsStaffApproveRejectAndVerifyPriority` | CÓ |
| FR-REG-07 | Duyệt/từ chối đơn | `/staff/applications/:id` | `RoomApplicationPolicyTest`; `CrudApiIntegrationTest#applicationWorkflow...` | CÓ |
| FR-REG-08 | Phân giường, tạo HOLD + hợp đồng PENDING | `/staff/applications/:id` | `ApplicationAssignmentIntegrationTest#assignCreatesHoldContractAndMarksApplicationAssigned` | CÓ |
| FR-REG-09 | Hai yêu cầu cùng giường chỉ một thắng | `BedOccupancyIntegrationTest#concurrentRequestsForLastBedHaveExactlyOneWinner` | CÓ |
| FR-REG-10 | Gửi lặp không tạo phân phòng thứ hai | `ApplicationAssignmentIntegrationTest#assignTwiceIsRejected` | CÓ |
| FR-REG-11 | Hủy giữ chỗ, đóng phân phòng, hủy hợp đồng | `applications`, `myApplications` | `ApplicationAssignmentIntegrationTest#releaseHoldReturnsApplicationToApprovedAndCancelsPendingContract`, `#cancelAssignedApplicationReleasesHoldAndCancelsContract` | CÓ |
| FR-REG-12 | Sinh viên xem trạng thái/lý do/giường giữ/hạn | `/student/applications/:id` | Route có; chưa thấy test UI/detail riêng | MỘT PHẦN |
| FR-REG-13 | Mở đợt hè, thông báo sinh viên ACTIVE | `/staff/summer`; `summer` | `SummerChoiceIntegrationTest`; chưa thấy test tạo thông báo khi mở đợt | CẦN ĐỐI CHIẾU |
| FR-REG-14 | Ba lựa chọn hè, Staff duyệt | `/student/summer`, `/staff/summer`; `summer` | `SummerChoiceApprovalTest`; `SummerChoiceIntegrationTest` | CÓ |
| FR-REG-15 | Ở hè: gia hạn hợp đồng + hóa đơn hè | `/staff/summer/:id`; `summer` | `SummerChoiceIntegrationTest#approveStayExtendsContractAndRecordsSummerExtension`; `DepositIntegrationTest#summerFeeRequiresApprovedStayChoice` | CÓ |

### 5.4. FR-CON (Hợp đồng)

| FR | Nội dung ngắn | Route/resource | Test/evidence | Trạng thái |
|---|---|---|---|---|
| FR-CON-01 | Kích hoạt PENDING, thu cọc, HOLD → STAY | `/staff/contracts/:id`; `contracts` | `ContractActivationIntegrationTest#activationCollectsDepositAndConvertsHoldToStay` | CÓ |
| FR-CON-02 | PENDING quá N ngày tự hủy | Job hệ thống | `SystemJobsIntegrationTest#cancelsPendingReleasesHoldCancelsApplicationAndAuditsOnce` | CÓ |
| FR-CON-03 | Nhãn “Sắp hết hạn” | `contracts`; view/label backend | `SystemJobsIntegrationTest#contractRemindersArePerEndDateAndConcurrentRunsDoNotDuplicate` (nhắc hạn, không phải nhãn UI) | CẦN ĐỐI CHIẾU (UI) |
| FR-CON-04 | Nhãn “Quá hạn trả phòng” | `contracts`; view/label backend | Chưa thấy test nhãn | CẦN ĐỐI CHIẾU |
| FR-CON-05 | Xem hợp đồng, cọc, lịch sử phân phòng | `/student/contracts/:id`, `/staff/contracts/:id` | `ContractDetailResponseTest#detailResponseExposesEveryDocumentedBlock` | CÓ |
| FR-CON-06 | Gia hạn, duyệt/từ chối, chống chồng lấn | `/student/requests/extensions/:id`, `/staff/extensions/:id`; `extensions` | `ContractExtensionIntegrationTest#approveUpdatesContractEndDate`, `#overlappingContractBlocksApproval`; `ContractExtensionRuleTest#overlappingStay...` | CÓ |
| FR-CON-07 | Yêu cầu chuyển phòng | `/student/requests/transfers/:id`, `/staff/transfers/:id`; `transfers` | `BedOccupancyIntegrationTest#studentCreatesTransferRequestOnlyWhileStaying`, `#approveTransferMovesStudentAndRecordsDecision` | CÓ |
| FR-CON-08 | Chuyển phòng một transaction, khóa hai giường | `/staff/transfers/:id` | `BedOccupancyIntegrationTest#transferOpensConsecutiveStayWithTargetPriceAndRemovesLeader`, `#crossingTransfersDoNotDeadlock` | CÓ |
| FR-CON-09 | Trả phòng, hoàn cọc, EXPIRED/TERMINATED | `/staff/contracts/:id/move-out` | `MoveOutIntegrationTest#moveOutRefundsBalanceAndReleasesBed` | CÓ |
| FR-CON-10 | Công nợ sau trả phòng | `deposits`; dashboard | Test số dư âm khi trả phòng và test truy vấn nợ riêng chưa chứng minh truy vấn vẫn nhìn thấy nợ sau checkout | MỘT PHẦN; CẦN TEST XUYÊN LUỒNG |
| FR-CON-11 | Tạm rời hè, phiếu gửi đồ, đóng giường | `/staff/storage`; `StoragePage` | `StorageIntegrationTest#createClosesAssignmentButKeepsContractActive` | CÓ |
| FR-CON-12 | Phân lại giường đầu HK1, nhận lại đồ | `/staff/storage` | `StorageIntegrationTest#returnOccupiesAvailableBedAndRecordsDate` | CÓ |
| FR-CON-13 | Rời KTX cuối HK2 làm trả phòng | `/staff/contracts/:id/move-out` | `MoveOutIntegrationTest` | CÓ |

### 5.5. FR-MNT (Sửa chữa)

| FR | Nội dung ngắn | Route/resource | Test/evidence | Trạng thái |
|---|---|---|---|---|
| FR-MNT-01 | Sinh viên tạo ticket phòng hiện tại | `/student/maintenance`; `maintenance` | `CrudApiIntegrationTest#maintenanceWorkflowChecksAssignedTechnicianAndRecordsProgress` | CÓ |
| FR-MNT-02 | Staff tạo ticket khu vực chung/phòng | `/staff/maintenance`; `maintenance` | `CrudApiIntegrationTest` tạo ticket có `buildingId/floor` và `roomId` (dòng 431, 835) | CÓ |
| FR-MNT-03 | Từ chối/giao KTV, sinh viên hủy OPEN | `/staff/maintenance/:id`; `MaintenanceRequestPolicyTest` | `MaintenanceRequestPolicyTest#openTicketActionsRemainUnchanged` | CÓ |
| FR-MNT-04 | KTV nhận → IN_PROGRESS → RESOLVED | `/technician/tasks/:id`; `TaskDetailPage` | `CrudApiIntegrationTest#maintenanceWorkflowChecksAssignedTechnicianAndRecordsProgress` | CÓ |
| FR-MNT-05 | Nghiệm thu CLOSED hoặc trả IN_PROGRESS | `/student/maintenance/:id`, `/staff/maintenance/:id` | `MaintenanceRequestPolicyTest#resolvedTicketActionsRemainUnchanged` | CÓ |
| FR-MNT-06 | RESOLVED quá 3 ngày tự đóng | Job hệ thống | `SystemJobsIntegrationTest#closesOnlyResolvedOlderThanConfiguredDeadlineAndIsRepeatable` | CÓ |
| FR-MNT-07 | Staff nghiệm thu thay | `StaffMaintenanceDetailPage`; endpoint `/accept` | `CrudApiIntegrationTest` gọi `/accept` (dòng 475, 1443) | CÓ |
| FR-MNT-08 | Toàn bộ lịch sử trạng thái | Detail pages; backend history | `CrudApiIntegrationTest#maintenanceWorkflow...` (ghi progress); UI history chưa kiểm | MỘT PHẦN |
| FR-MNT-09 | Kết luận nguyên nhân, chi phí | `/staff/maintenance/:id`; `MaintenanceConcludeIntegrationTest` | `#at22WearDoesNotCreateDeductionOrRepairCost`, `#misuseRequiresRepairCost` | CÓ |
| FR-MNT-10 | Trừ cọc chia đều, bù cọc 7 ngày | `MaintenanceConcludeIntegrationTest`; `deposits` | `#at14SplitsRepairCostAcrossThreeOccupantsAndWritesLedger`; `#subsetChargesOnlySelectedStudents` | CÓ |

### 5.6. FR-BIL (Điện nước, hóa đơn, cọc)

| FR | Nội dung ngắn | Route/resource | Test/evidence | Trạng thái |
|---|---|---|---|---|
| FR-BIL-01 | Đơn giá điện/nước có hiệu lực | `/staff/utilities/prices`; `rates` | `CrudApiIntegrationTest#utilityRateSaveAndSearchRetainMeterTypeFilterAndPartialValues` | CÓ |
| FR-BIL-02 | Nhập chỉ số, đủ kỳ liên tiếp | `/staff/utilities`; `readings` | `CrudApiIntegrationTest#readingCrudCalculatesConsumptionAndInvoiceRecordCrudRejectsForeignScope`, `#readingCreationRequiresConsecutiveMonthsAndPreservesMeterReplacement`, `#readingCannotBeInsertedBeforeAnExistingLaterMonth` | CÓ |
| FR-BIL-03 | Thay/reset đồng hồ | `readings` | `CrudApiIntegrationTest#readingCreationRequiresConsecutiveMonthsAndPreservesMeterReplacement` | CÓ |
| FR-BIL-04 | Hóa đơn điện nước theo phòng | `/staff/invoices`; `invoices` | `DepositIntegrationTest#utilitySkipsMissingReadingThenIssuesUsingSnapshotRates` | CÓ |
| FR-BIL-05 | Hóa đơn phí phòng theo học kỳ | `/staff/invoices/issue`; `invoices` | `DepositIntegrationTest#roomFeeUsesConfiguredDueDateAndWritesAuditWithoutDuplicating`, `#summerFeeRequiresApprovedStayChoice`, `#futureSemesterCannotBeIssued` | CÓ |
| FR-BIL-06 | Không phát hành trùng | `invoices` | `BillingConcurrencyIntegrationTest#simultaneousBatchRequestsDoNotDuplicateOrRollbackOtherInvoices` | CÓ |
| FR-BIL-07 | Hạn đóng +7 ngày, thông báo | `invoices` | `DepositIntegrationTest#roomFeeUsesConfiguredDueDateAndWritesAuditWithoutDuplicating` | CÓ |
| FR-BIL-08 | Xác nhận thanh toán, PAID khóa | `invoices`; `CrudPage.tsx:674-681` | `DepositIntegrationTest#confirmPaymentWritesOneAuditAndRejectsRepeatedConfirmation`; `CrudApiIntegrationTest#contractAndInvoiceQueriesRespectOwnershipAndPaymentCannotBeRepeated` | CÓ |
| FR-BIL-09 | Sổ cọc thu/trừ/bù/hoàn | `/staff/deposits`; `deposits` | `DepositIntegrationTest` (23 test: collect, deduct, topup, refund, debt) | CÓ |
| FR-BIL-10 | Sinh viên xem hóa đơn/cọc | `/student/invoices`, `/student/deposit`; `myInvoices`, `myDeposits` | `DepositIntegrationTest#studentCannotReadAnotherStudentsLedger` | CÓ |
| FR-BIL-11 | Công nợ theo sinh viên/phòng, xuất báo cáo | `deposits`; dashboard | `DepositIntegrationTest#debtQuery_returnsAccurateDebts`, `#dashboardRoomDebtStaysWithInvoiceSemesterAfterStudentLeaves`; chưa thấy export báo cáo | MỘT PHẦN |
| FR-BIL-12 | Điều chỉnh hóa đơn kỳ sau | Không thấy UI/resource | Không thấy test điều chỉnh | CẦN ĐỐI CHIẾU |
| FR-BIL-13 | Nhãn quá hạn bù cọc, nhắc sinh viên | `deposits`; job nhắc | `SystemJobsIntegrationTest#depositReminderUsesFifoSkipsRepaidDeductionAndDoesNotDuplicate` | MỘT PHẦN (UI nhãn chưa kiểm) |

### 5.7. FR-NOT (Thông báo & Dashboard)

| FR | Nội dung ngắn | Route/resource | Test/evidence | Trạng thái |
|---|---|---|---|---|
| FR-NOT-01 | Staff đăng thông báo đơn vị/tòa/toàn KTX | `/staff/notifications`; `notifications` | `CrudApiIntegrationTest#notificationSaveRetainsOmittedContentAndRecipientScope` | CÓ |
| FR-NOT-02 | Thông báo theo phòng/tầng | `notifications` | Backend: `CrudApiIntegrationTest#floorNotificationCanBeRetitledWithoutChangingScope`; **UI không có option FLOOR** (`resources.ts:1088`) | MỘT PHẦN |
| FR-NOT-03 | Xem thông báo, đánh dấu đã đọc | `/student/notifications`, `/staff/inbox`, `/technician/notifications`, `/admin/inbox` | `CrudApiIntegrationTest#notificationReadsAreIdempotentAndStudentsCannotPublish` | CÓ |
| FR-NOT-04 | Tự tạo thông báo khi đổi trạng thái/nhắc hạn | Listener/service | `ContractNotificationListenerTest` (7 test); `MaintenanceNotificationListenerTest` (9 test); hóa đơn mới chưa thấy test | MỘT PHẦN |
| FR-NOT-05 | Dashboard Staff theo đơn vị | `/staff/dashboard`; `StaffHomePage` | `DepositIntegrationTest#dashboardSeparatesHoldsAndNonOperationalBedsAndUsesConfig`, `#dashboardRoomDebtStaysWithInvoiceSemesterAfterStudentLeaves` | CÓ |
| FR-NOT-06 | Dashboard dạng biểu đồ | `/staff/dashboard` | `StaffHomePage`; không có test UI | CÓ (code); UI NOT_VERIFIED |

## 6. Ma trận AT-01–AT-33

> Cột “Bằng chứng” chỉ ghi test đọc được trong mã; **không** khẳng định đã chạy trên cây hiện tại. Mọi hàng UI vẫn `NOT_VERIFIED`.

| AT | Kịch bản (tóm tắt) | Bằng chứng test | Kết quả nền |
|---|---|---|---|
| AT-01 | Nộp → duyệt → phân giường → kích hoạt → tra cứu | `ApplicationAssignmentIntegrationTest#assignCreatesHoldContractAndMarksApplicationAssigned`; `ContractActivationIntegrationTest#activationCollectsDepositAndConvertsHoldToStay` | TEST CÓ |
| AT-02 | Hai request đồng thời vào giường cuối | `BedOccupancyIntegrationTest#concurrentRequestsForLastBedHaveExactlyOneWinner` | TEST CÓ |
| AT-03 | Chèn SQL hai phân phòng cùng giường | `BedOccupancyIntegrationTest#databaseRejectsTwoOpenAssignmentsOnOneBed` | TEST CÓ |
| AT-04 | Gửi lặp phân phòng | `ApplicationAssignmentIntegrationTest#assignTwiceIsRejected` | TEST CÓ |
| AT-05 | Hợp đồng PENDING quá N ngày | `SystemJobsIntegrationTest#cancelsPendingReleasesHoldCancelsApplicationAndAuditsOnce` | TEST CÓ |
| AT-06 | Staff K1 đọc/sửa dữ liệu N2 | `BedOccupancyIntegrationTest#occupyRejectsBedOutsideStaffUnitAsNotFound`; `CrudApiIntegrationTest#listsAndDetailsRespectUnitScopeAndAggregatePermissionDoesNotAllowWrites` | TEST CÓ |
| AT-07 | Chuyển phòng chèn lỗi giữa chừng | `BedOccupancyIntegrationTest` transfer tests; không thấy test fault-injection giữa transaction | MỘT PHẦN |
| AT-08 | Chuyển sang giường đơn vị khác | `BedOccupancyIntegrationTest#transferValidatesSourceAndTarget`, `#transferOutsideStaffUnitIsNotFound` | TEST CÓ |
| AT-09 | Gia hạn chồng hợp đồng khác | `ContractExtensionRuleTest#overlappingStayOfTheSameStudentBlocksApproval`; `ContractExtensionIntegrationTest#overlappingContractBlocksApproval` | TEST CÓ |
| AT-10 | Trả phòng còn hóa đơn chưa đóng | `MoveOutIntegrationTest#warningsRequireAcknowledgement`, `#negativeBalanceBecomesDebtWithoutRefund` | TEST CÓ |
| AT-11 | Ticket OPEN → CLOSED trực tiếp; KTV tự đóng | `MaintenanceRequestPolicyTest#openTicketActionsRemainUnchanged`, `#resolvedTicketActionsRemainUnchanged` | TEST CÓ |
| AT-12 | Ticket RESOLVED quá 3 ngày | `SystemJobsIntegrationTest#closesOnlyResolvedOlderThanConfiguredDeadlineAndIsRepeatable` | TEST CÓ |
| AT-13 | Chỉ số cuối < đầu | SRS/DB Design quy định CHECK; chưa kiểm migration, không thấy test riêng | CẦN ĐỐI CHIẾU |
| AT-14 | 3 sinh viên, sửa 100.000 đ | `MaintenanceConcludeIntegrationTest#at14SplitsRepairCostAcrossThreeOccupantsAndWritesLedger` | TEST CÓ |
| AT-15 | Phát hành trùng hóa đơn | `BillingConcurrencyIntegrationTest#simultaneousBatchRequestsDoNotDuplicateOrRollbackOtherInvoices` | TEST CÓ |
| AT-16 | Sửa hóa đơn đã thanh toán | `InvoiceCrudScopeIntegrationTest#paidInvoiceCannotBeEditedOrDeletedEvenByAdmin`: Staff/ADMIN gọi trực tiếp save-or-update và DELETE trên hóa đơn PAID, 409 và dữ liệu/audit giữ nguyên; đã chạy trong `audit-clean-full-test.log` | BACKEND PASS; UI NOT_VERIFIED |
| AT-17 | Sinh viên ACTIVE nộp đơn mới | Không thấy test trực tiếp | CẦN ĐỐI CHIẾU |
| AT-18 | Kích hoạt N2 với cọc 1.000.000 đ | `ContractActivationValidationTest#activationRejectsDepositAmountBelowBuildingLevel`; `ContractActivationIntegrationTest#wrongDepositAmountIsRejectedWithRequiredAmount` | TEST CÓ |
| AT-19 | K1 bị trừ 200.000 đ rồi trả phòng | `MoveOutIntegrationTest#moveOutRefundsBalanceAndReleasesBed` | TEST CÓ |
| AT-20 | Hóa đơn điện nước gửi trưởng phòng | `DepositIntegrationTest#utilitySkipsMissingReadingThenIssuesUsingSnapshotRates`; chưa thấy test thông báo trưởng phòng | MỘT PHẦN |
| AT-21 | Trưởng phòng trả phòng | `MoveOutIntegrationTest#moveOutRefundsBalanceAndReleasesBed`; `RoomLeaderIntegrationTest#leaderHistoryIncludesManualAndAutomaticRemoval` | TEST CÓ |
| AT-22 | Ticket CLOSED, nguyên nhân hao mòn | `MaintenanceConcludeIntegrationTest#at22WearDoesNotCreateDeductionOrRepairCost` | TEST CÓ |
| AT-23 | Bù cọc quá 7 ngày | `SystemJobsIntegrationTest#depositReminderUsesFifoSkipsRepaidDeductionAndDoesNotDuplicate` | MỘT PHẦN (nhãn UI chưa kiểm) |
| AT-24 | Sinh viên tạm rời hè | `StorageIntegrationTest#createClosesAssignmentButKeepsContractActive` | TEST CÓ |
| AT-25 | Phân lại giường đầu HK1 | `StorageIntegrationTest#returnOccupiesAvailableBedAndRecordsDate` | TEST CÓ |
| AT-26 | Đăng ký ở hè được duyệt | `SummerChoiceIntegrationTest#approveStayExtendsContractAndRecordsSummerExtension`; `DepositIntegrationTest#summerFeeRequiresApprovedStayChoice` | TEST CÓ |
| AT-27 | Đăng nhập từng vai trò, chống nâng quyền | `KeycloakAccountIntegrationTest#roleMismatchDoesNotGrantStoredRole`, `#unknownPrivilegeFailsClosedEvenForAdmin` | TEST CÓ |
| AT-28 | Đổi/reset mật khẩu nhiều phiên, khóa/mở | `KeycloakAccountIntegrationTest#lockUnlockAndPasswordResetKeepOldTokensRevokedWithoutStoringPassword` | TEST CÓ |
| AT-29 | Staff reset sai phạm vi | Không thấy test trực tiếp | CẦN ĐỐI CHIẾU |
| AT-30 | PKCE, token sai, logout rồi dùng token cũ | `KeycloakJwtDecoderTest` (5 test); `KeycloakAccountIntegrationTest#dmsLogoutImmediatelyRejectsPreviouslyIssuedToken` | TEST CÓ |
| AT-31 | Username trùng nhưng subject chưa liên kết | `KeycloakAccountIntegrationTest#sameUsernameWithDifferentSubjectCannotClaimProfile` | TEST CÓ |
| AT-32 | Hủy đơn APPROVED đã có hợp đồng | `RoomApplicationPolicyTest#approvedApplicationWithLiveContractCannotBeAssignedOrCancelled`; `ApplicationAssignmentIntegrationTest#cancelAndReleaseAreBlockedWhenContractActive` | TEST CÓ |
| AT-33 | Sửa `bed_count` của loại phòng đã gán | `CrudApiIntegrationTest#changingRoomTypeSynchronizesBedsAndRoomTypeCapacityIsGuarded` | TEST CÓ |

**Tổng AT:** 33 hàng; **27 `TEST CÓ`**, **3 `MỘT PHẦN`** (AT-07, AT-20, AT-23) và **3 `CẦN ĐỐI CHIẾU`** (AT-13, AT-17, AT-29). Không có AT nào được đánh `PASS` UI.

## 7. Sổ kiểm chứng tổng hợp

| Hạng mục | Implementation | Test hiện có | UI verification | Trạng thái | Ghi chú |
|---|---|---|---|---|---|
| Route + resource registry | `router/index.tsx:13-30`; 4 file route; `resources.ts:284-1196` | Không có test frontend | NOT_VERIFIED | CÓ (inventory) | 90 route portal; 29 resource; `detailRoutes` chưa có consumer |
| FR-ACC | `index.tsx:14-19`; `adminRoutes.tsx:20-34`; `resources.ts:376-462`, `1116` | `KeycloakAccountIntegrationTest`; `StudentImportIntegrationTest`; `CrudApiIntegrationTest` | NOT_VERIFIED | MỘT PHẦN | FR-ACC-02/03 chưa đủ bằng chứng; AT-29 chưa có test |
| FR-ROOM | `staffRoutes.tsx:53-54,92-93`; `adminRoutes.tsx:21-22`; `resources.ts:285-501` | `CrudApiIntegrationTest`, `BedOccupancyIntegrationTest`, `RoomLeaderIntegrationTest` | NOT_VERIFIED | MỘT PHẦN | FR-ROOM-05/06/08 thiếu test trực tiếp |
| FR-REG | `studentRoutes.tsx:27-33`; `staffRoutes.tsx:48-52`; `resources.ts:502-618` | `ApplicationAssignmentIntegrationTest`, `RoomApplicationPolicyTest` | NOT_VERIFIED | MỘT PHẦN | FR-REG-03/12/13 chưa đủ bằng chứng |
| FR-CON | `studentRoutes.tsx:34-58`; `staffRoutes.tsx:57-69`; `resources.ts:619-808` | `ContractActivationIntegrationTest`, `ContractExtensionIntegrationTest`, `MoveOutIntegrationTest`, `StorageIntegrationTest` | NOT_VERIFIED | MỘT PHẦN | FR-CON-03/04 nhãn chưa có test UI |
| FR-MNT | `studentRoutes.tsx:61-65`; `staffRoutes.tsx:81-85`; `technicianRoutes.tsx:19-20`; `resources.ts:1030-1078` | `MaintenanceRequestPolicyTest`, `MaintenanceConcludeIntegrationTest`, `SystemJobsIntegrationTest` | NOT_VERIFIED | CÓ (backend) | FR-MNT-08 history UI chưa kiểm |
| FR-BIL | `staffRoutes.tsx:76-80`; `studentRoutes.tsx:59-60`; `resources.ts:809-1029` | `DepositIntegrationTest` (23), `BillingConcurrencyIntegrationTest` | NOT_VERIFIED | MỘT PHẦN | FR-BIL-11 thiếu export; FR-BIL-12 chưa thấy |
| FR-NOT | `staffRoutes.tsx:87-90`; `studentRoutes.tsx:71`; `resources.ts:1079-1115` | `NotificationServiceImplTest`, listener tests, `CrudApiIntegrationTest` | NOT_VERIFIED | MỘT PHẦN | UI thiếu scope FLOOR; hóa đơn mới chưa thấy test |
| Tùy chọn giao diện theo tài khoản | `UserPreferenceController.java:18-37`; `V8__user_preference.sql`; `services/api/preferences.ts`; `themeStore.ts`; `App.tsx` | `UserPreferenceIntegrationTest` (6 test, pass 31/10/2026); full suite 342 test pass | NOT_VERIFIED | MỘT PHẦN | Chưa kiểm 2 account/tab/device trên browser |
| UI core/CRUD | `theme.ts`; `App.tsx:17`; `CrudPage.tsx:407-427`, `934-951`; `DataTable.tsx` | Không có frontend test | NOT_VERIFIED | MỘT PHẦN | Working tree chưa commit; chưa build/lint lại; DTR chưa nối |
| Backend suite lịch sử | Log `DMS/backend/data/plan-20261031-test/full-test.log:853-859` | 330 test, 0 failure/error/skip; BUILD SUCCESS | NOT_VERIFIED | CÓ (lịch sử) | Không đại diện working tree hiện tại; không chạy lại |
| 36 bảng/migration | Manifest V1–V7 có 36 bảng, V8 thêm user_preference; tổng 38 base gồm Flyway history | DmsApplicationTests + Flyway V1–V8 trên DB sạch pass trong full355 | NOT_VERIFIED | DATA IN_PROGRESS/DRAFT | Owner/lifecycle/upgrade-restore vẫn mở; fresh migration pass không thay nghiệm thu toàn dữ liệu |
| NFR | Xem bảng 7.1 | Một phần test backend | NOT_VERIFIED | CẦN ĐỐI CHIẾU | Chưa đo tải/backup/a11y |

### 7.1. NFR snapshot

| NFR | Bằng chứng hiện có | Trạng thái |
|---|---|---|
| NFR-SEC-01 | `SensitiveValuesTest` (2 test); Keycloak không lưu mật khẩu DMS | CÓ (test) |
| NFR-SEC-02 | scope/aggregate tests | CÓ (test) |
| NFR-SEC-03 | validation/schema tests | MỘT PHẦN |
| NFR-SEC-04 | `KeycloakJwtDecoderTest`, `KeycloakAccountIntegrationTest` | CÓ (test) |
| NFR-SEC-05 | Review cấu hình/env chưa thực hiện | CẦN ĐỐI CHIẾU |
| NFR-PER-01 | Chưa đo tải | NOT_VERIFIED |
| NFR-PER-02 | Chưa đo phát hành 1.500 hóa đơn | NOT_VERIFIED |
| NFR-REL-01 | `BedOccupancyIntegrationTest#concurrentRequestsForLastBedHaveExactlyOneWinner` | CÓ (test) |
| NFR-REL-02 | `MaintenanceConcludeRollbackIntegrationTest`; `SystemJobsIntegrationTest`; AT-07 chưa có fault-injection chuyển phòng | MỘT PHẦN |
| NFR-REL-03 | `SystemJobsIntegrationTest` (idempotent, audit SYSTEM) | CÓ (test) |
| NFR-USA-01/02 | Chưa có browser/người dùng thử | NOT_VERIFIED |
| NFR-MNT-01 | `ModuleDependencyTest` (8 test) | CÓ (test) |
| NFR-MNT-02 | JaCoCo chưa chạy lại | NOT_VERIFIED |
| NFR-MNT-03 | `GlobalExceptionHandlerTest` kiểm error code; requestId chưa kiểm riêng | CẦN ĐỐI CHIẾU |

## 8. Bằng chứng và lệnh kiểm tra

### 8.1. Frontend baseline (đợt 31/10/2026)

Sau khi tích hợp PREF-02, `npm run lint` và `npm run build` được chạy lại: **exit 0**, lint 0 lỗi/0 cảnh báo; build có cảnh báo bundle >500 kB có sẵn. Đây là kết quả **working tree**, không phải CI.

### 8.2. Backend full suite

- Log lịch sử: `DMS/backend/data/plan-20261031-test/full-test.log` — 330 test, 0 lỗi, BUILD SUCCESS (trước V8).
- Log đợt PREF: `DMS/backend/data/plan-20261031-test/wave2-full-test.log` — **342 test, 0 failures/0 errors, BUILD SUCCESS**, gồm **5** preference test. Lần targeted sau đó ở `wave2-pref-retest2.log` có **6** preference test; hai log không cùng revision.
- Kiểm lại ngày 31/10/2026: `audit-full-test.log` có 353 test pass; sau bổ sung PAID guard, lần chạy lặp `audit-final-test.log` trên DB cũ lỗi 16 fixture do semester chồng ngày (không coi là PASS).
- Chạy lại schema sạch PostgreSQL 16 riêng cổng 55444, JDK 17: `DMS/backend/data/plan-20261031-test/audit-clean-full-test.log` — **355 test, 0 failures/errors/skipped, BUILD SUCCESS**, kết thúc 11:26:34 +07. Gồm 6 preference test và 12 invocations invoice scope/PAID.
- Targeted sau đó: `audit-pref-concurrency.log` — **1 test race first-save, 0 failures/errors/skipped, BUILD SUCCESS**, kết thúc 11:31:21; không cộng vào tổng full355. Spy đồng bộ lookup, persistence/flush/PK/transactions là PostgreSQL thật. Chưa kiểm HTTP auth và không phải CI/E2E. Các log bị `.gitignore` bỏ qua.
- Đây là bằng chứng **working tree**, không phải CI; chưa chạy lại sau mọi thay đổi tài liệu (tài liệu không ảnh hưởng test).

### 8.3. Browser/UI

- Ảnh 360 px: **NOT DONE**.
- Ảnh 768 px: **NOT DONE**.
- Ảnh 1440 px: **NOT DONE**.
- Không có kiểm tra console/network, bàn phím, zoom, contrast, reduced-motion.

### 8.4. Lệnh đếm và kiểm tra đã dùng

| Mục đích | Lệnh/pattern | Kết quả |
|---|---|---|
| Đếm resource | `Grep` pattern `^  [a-zA-Z][a-zA-Z0-9]*: \{$` trên `resources.ts` | 29 |
| Đếm path literal | `Grep` pattern `path: '` trên `app/router` | 66 (5 file) |
| Đếm index route | `Grep` pattern `index: true` trên `app/router` | 4 |
| Đếm route portal | Đọc 5 file route + tính spread Admin | 90 (17+30+5+38) |
| Kiểm tra `detailRoutes` | `Grep` pattern `detailRoutes` trên `frontend/src` | 9 resource + 1 type; 0 consumer trong `CrudPage` |
| Git baseline | `rtk proxy git status --short --branch` | Branch `develop-quyennnv`, ahead 16, dirty |
| Git SHA | `rtk proxy git log -1 --format="%H %h %ad %s"` | `266f5886...` |
| Log ignored | `rtk proxy git check-ignore -v DMS/backend/data/plan-20261031-test/full-test.log` | `.gitignore:7: DMS/backend/data/` |
| Stash | `rtk proxy git stash list` | `stash@{0}` hiện có |
| Dòng SRS/route/resource | `rtk proxy wc -l` | SRS 716; router 30; student 76; staff 101; technician 26; admin 42; resources 1196 |

## 9. Gate checklist theo kế hoạch mục 12.2

> **Số ticket không phải tỷ lệ hoàn thành.** Kế hoạch có 15 ticket cha; hiện không có ticket triển khai nào `DONE`. Các trạng thái dưới đây là gate của baseline, không phải điểm phần trăm.

| # | Gate | Trạng thái | Bằng chứng/ghi chú |
|---:|---|---|---|
| 1 | Mọi resource/màn có mẫu view/edit và trạng thái phản hồi được duyệt | **OPEN** | 29 resource/90 route đã inventory; chưa duyệt mẫu; `detailRoutes` chưa nối |
| 2 | 36 bảng có rule/owner/test mapping | **OPEN** | DMS-DATA-01 BACKLOG; plan ghi 36 bảng từ migration; chưa kiểm lại schema |
| 3 | Preference theo user: account isolation, logout/race/tab/đa thiết bị, contrast | **NOT_VERIFIED** | Backend 6 test + full suite 342 pass; frontend lint/build pass; chưa browser |
| 4 | Mọi module dùng shared token, không lệch palette sáng/tối/popup | **NOT_VERIFIED** | Theme/UI đang sửa chưa commit; chưa browser |
| 5 | FR Must và AT-01–AT-33 có bằng chứng hoặc defer được duyệt | **OPEN** | 27 AT có test; AT-07/20/23 một phần; AT-13/17/29 cần đối chiếu; chưa có defer |
| 6 | Unit/ArchUnit/integration/lint/build/E2E chạy pass, có coverage/exit code | **NOT_VERIFIED** | Log 330 test lịch sử; lint/build lịch sử; E2E không có; chưa chạy lại |
| 7 | Visual desktop/tablet/mobile, bàn phím, zoom, reduced-motion, screenshot thật | **NOT_VERIFIED** | Không có ảnh 360/768/1440; không browser |
| 8 | NFR hiệu năng/khả dụng/backup-restore đo và đạt | **NOT_VERIFIED** | NFR-PER-01/02, USA, backup chưa đo |
| 9 | Mọi ticket có reviewer độc lập, ghi chú dev, giới hạn, quyết định | **OPEN** | 15 ticket cha; phần lớn BACKLOG; chưa có review độc lập cho lượt này |
| 10 | Không secret/token/log test/artifact lớn trong Git; stash đúng quyết định | **OPEN** | Log ignored; stash hiện có; working tree có 10 file mới chưa commit cần rà trước commit |
| 11 | Chỉ release sau khi người dùng cho phép và CI đúng SHA xanh | **OPEN** | Chưa commit/push; chưa có CI cho working tree |

## 10. Việc tiếp theo

| # | Việc | Owner dự kiến | Blocker | Điều kiện hoàn thành |
|---:|---|---|---|---|
| 1 | Chốt ảnh chụp working tree/diff và SHA sau khi các agent dừng ghi | Lead + các owner | Agent khác đang sửa FE/BE; cây thay đổi trong lúc lập | Diff/status ổn định, ghi thời điểm và danh sách file |
| 2 | Chạy lại `npm run lint` + `npm run build` trên cây đã chốt | FE-Core | Chưa freeze frontend; không chạy trong lượt này | Exit code 0, log lưu ngoài Git |
| 3 | Chạy lại backend full suite trên PostgreSQL test riêng | Lead/QA + BE owner | Không Docker; cây đang sửa; V8 chưa kiểm | Log 330+ test, 0 failure, ghi SHA/working tree |
| 4 | Baseline browser 360/768/1440, 4 role, console/network | Browser QA | Không browser; cần DB + Keycloak | Ảnh thật + lỗi phân loại + keyboard/zoom/contrast |
| 5 | Bổ sung test cho AT-07, AT-13, AT-17, AT-20, AT-23, AT-29 | QA + module owner (TV2/TV3/TV5) | Cần test DB riêng; chưa có kịch bản fault-injection | Test tái hiện được, chạy pass, gắn AT |
| 6 | Quyết định nối `detailRoutes` vào `CrudPage` hoặc bỏ khai báo | FE-CRUD + Integrator | Hiện 0 consumer; ảnh hưởng 9 resource | Một hướng được chốt, caller/test cập nhật |
| 7 | Làm rõ FR-BIL-11/12, FR-NOT-02 (FLOOR), FR-REG-13, FR-CON-03/04 | Nghiệp vụ + Billing/Notification owner | SRS/UI chưa thống nhất; cần quyết định trước khi code | Quyết định ghi trong SRS/API; test/caller đồng bộ |
| 8 | Kiểm preference 2 account/tab/device, cache isolation, contrast | BE-Account + FE-Theme + QA | Backend test pass; browser chưa chạy | Kịch bản account isolation pass; ảnh/log |
| 9 | Hoàn tất DMS-DATA-01: 36 bảng + view/sequence/trigger, rule xóa | BE-Data + owner TV1–TV5 | BACKLOG; chưa có Docker/DB test | Manifest 36/36 + mapping service/quyền/lifecycle |
| 10 | Rà gate 12.2 và xin phép release | Lead + chủ dự án | Các gate trên còn OPEN/NOT_VERIFIED | Người dùng chấp thuận; CI xanh đúng SHA; rollback rõ |

## 11. Kết luận baseline

- Route/resource đã đếm được: **90 route trong 4 portal**, **29 resource**, không có sai lệch với con số 29 của kế hoạch.
- **Không có hạng mục nào được đánh `DONE`** chỉ vì có file. Các trạng thái `CÓ` chỉ là bằng chứng code/test, không thay nghiệm thu UI.
- Hai phát hiện cần theo dõi ngay: (1) `detailRoutes` khai báo nhưng chưa được `CrudPage` dùng; (2) UI thông báo thiếu scope `FLOOR` dù backend đã hỗ trợ.
- Baseline UI/browser và NFR vẫn `NOT_VERIFIED`; không được tính vào tỷ lệ hoàn thành.

# Báo cáo: Review nhánh dev/nguyennp, dọn code, tối ưu và đưa vào develop

> Ngày hoàn thành: 17/10/2026
> Thực hiện: TV2 Nguyễn Phú Nguyên (cùng Claude Code)
> Phạm vi: toàn bộ thay đổi của nhánh `dev/nguyennp` so với `develop` (backend phòng/giường/chuyển phòng, frontend duyệt chuyển phòng và chi tiết phòng, tài liệu, script Docker)
> Căn cứ: `CLAUDE.md` (quy tắc không hardcode, Service interface/Impl), `Docs/02 External Design/API Specification/API-Spec-DMS.md`
> Trạng thái: Hoàn thành một phần (xem mục 3: test tích hợp chưa chạy được và mục 5: các đề xuất chưa làm)

---

## 1. Mục tiêu

Rà nhánh xem có code cần loại bỏ hoặc tối ưu không, sửa phần rủi ro thấp, ghi lại phần còn lại thành đề xuất, rồi tạo PR vào `develop`.

Trong phạm vi: merge `develop` vào nhánh, sửa lỗi phát sinh, xóa code chết, gom hằng, sửa lỗi chức năng tìm thấy khi review.
Ngoài phạm vi: các tối ưu hiệu năng backend và tách `CrudPage` (xem mục 5); chỉ ghi đề xuất.

Phát hiện đầu tiên: bản local của nhánh còn ở `d1d0ffd`, cũ hơn remote 11 commit. Review làm trên `origin/dev/nguyennp`. `develop` đã đổi hợp đồng API sang `POST search-by-page` và `POST save-or-update`, nên nhánh xung đột nội dung ở 14 file và có thêm lỗi biên dịch không báo xung đột.

## 2. Kết quả

### 2.1 Merge `develop` vào nhánh

Nguyên tắc: hợp đồng API theo `develop`, nghiệp vụ mới theo nhánh.

| Hạng mục | File | Nội dung |
|---|---|---|
| Ngày sinh | `StudentManagementController`, `StudentManagementServiceImpl`, `CrudPage.tsx`, `types.ts`, `resources.ts`, `CrudApiIntegrationTest` | Dùng record `Save` của develop. Bỏ `StudentPatch`, `dateOfBirthPresent()`, cờ `clearable` và test xóa ngày sinh bằng `null`, vì khung save-or-update của develop coi `null` là giữ nguyên |
| Trùng mapping | `ContractCrudController` | Xóa `getTransfer`: trùng `GET /api/transfer-requests/{id}` ở `ContractController`, nếu giữ cả hai ứng dụng không khởi động |
| Ticket bảo trì | `MaintenanceCrudServiceImpl`, `MaintenanceRequestServiceImpl` | Giữ khung develop, áp lại finding review: ticket của người khác trả 404, ticket không còn OPEN trả 409 `INVALID_STATE_TRANSITION`, cho cả đường hủy lẫn đường sửa |
| Phòng/giường | `RoomServiceImpl`, `RoomService`, `BedController`, `BedCrudPanel.tsx` | Giữ `findAvailableBeds`, trưởng phòng, `syncBeds` (kích hoạt lại giường RETIRED) kèm `entityManager.refresh` của develop; panel giường gọi `saveOrUpdate('/beds', …)` |
| Hằng loại đối tượng | `DmsConstants`, `RoomServiceImpl`, `BedOccupancyServiceImpl`, `RoomTransferServiceImpl` | Gộp `AuditEntityType`, `StatusEntityType` vào `EntityType` (thêm `ASSIGNMENT`, `TRANSFER`); giá trị chuỗi không đổi |
| Tài liệu | `API-Spec-DMS.md`, `DMS/backend/README.md` | Đánh lại phiên bản: giữ 0.11/0.12 của develop, phần chiếm/trả giường là 0.13, phần review là 0.14. Bỏ số test và số mã lỗi cứng trong README |

### 2.2 Lỗi chức năng sửa thêm

| Hạng mục | File | Nội dung |
|---|---|---|
| Route duyệt chuyển phòng bị 403 | `constants/navigation.ts` | `permissionsForPath` thiếu khóa `transfers`, nên `/staff/transfers/:id` trả `[]` quyền và luôn bị chặn. Đã thêm `transfers: [ROOM_TRANSFER_READ]` |
| Admin không vào được chi tiết phòng và yêu cầu | `features/crud/resources.ts`, `navigation.ts`, `RoomDetailPage`, `TransferReviewPage` | Thêm `detailRoutes` cho Admin; thêm `portalPath` để breadcrumb của Admin không bị đưa sang `/staff/…` |
| Hộp thoại xác nhận chờ refetch | `features/contract/index.ts`, `features/room/index.ts` | Chỉ chờ invalidate chi tiết đang xem; danh sách và giường trống tải lại nền |

### 2.3 Code chết và trùng lặp đã dọn

| Hạng mục | File | Nội dung |
|---|---|---|
| Overload không ai gọi | `RoomService`, `RoomServiceImpl` | Xóa `searchRooms` 10 tham số |
| Nhánh chết | `ContractCrudService`, `ContractCrudServiceImpl`, `ContractCrudController` | `getStayRequest(boolean, id)` chỉ còn gọi với `true`, đổi thành `getExtensionRequest(id)` |
| File không dùng | `pages/PlaceholderPage.tsx`, `pages/admin/AdminHomePage.tsx`, `pages/technician/TechnicianHomePage.tsx`, `types/common.ts` | Đã xóa; tìm toàn `src` không còn tham chiếu |
| CSS chết | `index.css` | Bỏ 4 selector `*-focused` không có class nào dùng và 5 biến CSS không có tham chiếu; sửa `rgba(29,78,216,.12)` lệch màu chủ đạo thành `rgba(54,91,125,.12)` |
| Thuộc tính vô hiệu | `CrudPage.tsx` | Bỏ `minWidth` trên cột antd (antd không có thuộc tính này) |
| Lặp biểu thức và literal | `statusConfig.ts` và 6 file dùng | Thêm `assignmentLabel`, `AssignmentKind`, `BED_STATUS`, `TRANSFER_ACTION` thay biểu thức `bedOccupancy[assignmentOccupancy[x]]` lặp 3 lần và các literal `'BROKEN'`, `'approve'`, `'reject'`, `'cancel'` |
| Khóa JSON và thông báo | `DmsConstants.ApiField`, `BedOccupancyServiceImpl`, `RoomServiceImpl`, `RoomTransferServiceImpl`, 3 Policy, `AbstractSqlCrudServiceImpl` | Thêm hằng `REASON`, `END_REASON`, `BED_ID`, `ROOM_CODE`, `CURRENT_STATUS`… và dùng `ValidationMessage.REQUIRED`. Giá trị chuỗi giữ nguyên |
| Nhỏ | `RoomServiceImpl`, `BedOccupancyIntegrationTest` | Tính `typeChanged` một lần, đổi biến cục bộ `occupancy` che field, bỏ tên đầy đủ `java.util.HashMap`, `java.sql.Timestamp` |
| Test | `KeycloakAccountIntegrationTest`, `ModuleDependencyTest`, `BedOccupancyIntegrationTest`, `CrudApiIntegrationTest` | Dùng `Permission.ALL.size()` và `ScopeGuard.permissionsForRole` thay số quyền cứng; 11 khối `assertThatThrownBy` lặp gom vào `assertConflict`; test FK đổi tên thành `savingRoomWithUnknownRoomTypeIsNotFoundAndNotReportedAsInUse` và gọi đúng endpoint |
| Tài liệu và script | `DMS/.env.example`, `DB-Design-DMS.md`, `README.md` | `.env.example` thêm `DMS_KEYCLOAK_INITIAL_ADMIN_*` mà `start-docker.ps1` bắt buộc; mô tả V6 khớp SQL thật; ghi rõ `start-docker.ps1` đã nạp tài khoản mẫu và xung đột cổng 5432 với `start-local.ps1` |

## 3. Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| `./mvnw -q -DskipTests test-compile` (backend, sau merge và sau dọn) | Đạt |
| `./mvnw test -Dtest=RoomTransferPolicyTest,ModuleDependencyTest` | Đạt 11/11 (3 + 8), gồm ArchUnit về cấu trúc Service/Impl, phụ thuộc module, `@RequirePrivilege` và danh mục 106 quyền |
| `npx tsc -p tsconfig.app.json --noEmit` (frontend) | Đạt, không lỗi |
| `npm run lint` (oxlint) | Đạt |
| `npm run build` | Đạt (chỉ có cảnh báo chunk lớn hơn 500 kB đã có từ trước) |
| `git grep` các tên đã xóa (`AuditEntityType`, `StatusEntityType`, `StudentPatch`, `PlaceholderPage`, `getStayRequest`, `dateOfBirthPresent`, `clearable`, `patch(` trong test) | Không còn tham chiếu |
| Tìm xung đột sót (`<<<<<<<`, `>>>>>>>`) trong repo | Không còn |

**Chưa kiểm tra:** `BedOccupancyIntegrationTest`, `RoomLeaderIntegrationTest`, `CrudApiIntegrationTest`, `KeycloakAccountIntegrationTest`, `DmsApplicationTests` cần Testcontainers PostgreSQL; môi trường làm việc không có Docker daemon nên các test này mới được biên dịch, chưa chạy. Cần chạy `./mvnw verify` trên máy có Docker trước khi merge PR. Các test CRUD viết lại cho `save-or-update` (maintenance multipart, `rooms/save-or-update`) và quyết định trả 404 cho ticket của người khác trong đường sửa là phần cần chú ý khi chạy. Chưa chạy giao diện trên trình duyệt.

## 4. Điểm lệch so với thiết kế và quyết định cần xác nhận

| Nội dung | Quyết định | Lý do |
|---|---|---|
| Xóa ngày sinh bằng `null` khi sửa sinh viên | Bỏ tính năng của nhánh | `develop` thống nhất `save-or-update` với `null` là giữ nguyên; làm lại cần đổi `Save` từ record sang lớp. Khung hiện chưa hỗ trợ xóa giá trị tùy chọn bằng `null` |
| Phiên bản API-Spec | 0.11/0.12 giữ của develop; nội dung nhánh đánh lại 0.13/0.14 | Hai bên cùng dùng số 0.11 và 0.12 cho nội dung khác nhau |
| `BedOccupancyService`, `occupyHold/occupyStay/convertHoldToStay/release` | Giữ nguyên dù chưa có caller trong `src/main` | README ghi đây là API để TV3 nối vào luồng phân giường |
| `DELETE /api/transfer-requests/{id}` | Giữ | Giao diện dùng `POST …/cancel`; cần chốt có bỏ đường xóa cứng hay không |
| Component `ResponsiveTable`, `ContractStatus`, `DepositLedger`, `InvoiceCard`, `NotificationItem`, `RoomCard` chưa có nơi dùng | Giữ | Là scaffold UI cho thành viên khác làm sau |
| `SRS-DMS.md` mục 329 và `SAD-DMS.md` dòng 4 vẫn trỏ DB-Design v0.7 | Chưa sửa | Sửa phải nâng phiên bản và ghi change log của hai tài liệu này; để cùng đợt cập nhật tài liệu |

## 5. Việc còn lại

Các đề xuất dưới đây tìm được khi review nhưng chưa làm vì cần chạy lại test tích hợp hoặc đổi hợp đồng API. Sắp theo giá trị giảm dần.

### 5.1 Backend: đúng đắn và hiệu năng

| Việc | Vị trí | Ghi chú |
|---|---|---|
| Khóa `SELECT … FOR UPDATE` dòng `room_transfer_request` khi duyệt/từ chối | `RoomTransferServiceImpl.approve/reject/cancel` | Hiện hai lần duyệt đồng thời có thể làm lượt thua chuyển giường rồi rollback và trả `CONCURRENT_UPDATE` thay vì `INVALID_STATE_TRANSITION`; vi phạm quy ước "khóa trước, kiểm tra sau". Không tạo vòng khóa vì mọi đường khóa giường trước. Test `concurrentApprovalsMoveStudentOnlyOnce` cần siết lại |
| N+1 khi đọc chi tiết phòng | `RoomServiceImpl` (phần dựng giường, trưởng phòng, lịch sử) | Mỗi người ở tốn 2 truy vấn (`users.findById`, `students.findById`); phòng 6 giường đầy khoảng 25 truy vấn. Thêm `AccountReferenceService.users(Collection<Long>)` trả `Map` |
| Đọc ma trận quyền lặp nhiều lần trong một request | `ScopeGuard`, `RolePermissionServiceImpl` | Một `GET /api/transfer-requests/{id}` đọc 3 đến 4 lần. Có thể ghi nhớ trong phạm vi request như `CurrentUser`; vẫn đọc mới ở request sau nên không trái yêu cầu "không cache" |
| Truy vấn giới tính trùng; nhiều SELECT thừa | `BedOccupancyServiceImpl.findOpenRow/findOpenAssignment/reloadOpenAssignment/removeLeaderIfNeeded`, `RoomReferenceServiceImpl.lockStudentProfile` | Gộp `SELECT gender … FOR UPDATE`; thay `findById + refresh + saveAndFlush` bằng một `UPDATE` có điều kiện |
| `exists` trong vòng lặp | `RoomServiceImpl.syncBeds` | Thay bằng một truy vấn lấy tập `bed_id` đã có lịch sử |
| `hasLiveContract` luôn chạy truy vấn | `RoomApplicationServiceImpl` | Chỉ cần gọi khi đơn đã được duyệt; `ApplicationStatus` chưa có hằng `ASSIGNED` dù CSDL cho phép |
| Join `v_room_occupancy` | `RoomServiceImpl` (truy vấn danh sách phòng) | Cần `EXPLAIN` trước khi đổi |
| Khóa trước, kiểm tra phạm vi sau | `RoomServiceImpl` (các hàm gọi `lockRoomBedsInAscendingOrder` trước `findAccessibleRoom`) | Staff đơn vị khác giữ khóa ngắn trước khi nhận 404 |
| Hai đường hủy yêu cầu chuyển phòng | `ContractCrudController` (DELETE), `ContractController` (cancel) | Chọn một đường |
| Chuỗi khóa JSON còn lại | Các Impl khác ngoài phạm vi đã dọn | Tiếp tục gom vào `ApiField` |

### 5.2 Frontend

| Việc | Vị trí | Ghi chú |
|---|---|---|
| Tách `CrudPage` (hiện hơn 1300 dòng) | `pages/CrudPage.tsx` | Tách bảng nhãn/`display`, `LookupInput`, hành động theo dòng (6 khối `modal.confirm` giống nhau), modal form và cột; đưa điều kiện `resource === …` rải rác (khoảng 20 chỗ) vào cấu hình `CrudResource` |
| Bảng nhãn trạng thái trùng và mâu thuẫn `statusConfig` | `CrudPage.tsx` (`labels`), `statusConfig.ts` (`crudValueLabels`) | Ví dụ `PENDING` là "Chờ xử lý" ở danh sách nhưng "Chờ duyệt" ở trang duyệt chuyển phòng. Dùng `StatusBadge` theo cấu hình resource |
| `useApiErrorHandler` invalidate toàn bộ query khi gặp 409; `CrudPage.refresh()` invalidate quá rộng | `hooks/useApiErrorHandler.ts`, `CrudPage.tsx` | Chỉ làm mới theo khóa liên quan |
| `BedCrudPanel` ghi qua `writeCrudRecord` và `useState`, không dùng mutation hook | `features/room/BedCrudPanel.tsx` | Thêm giường không làm mới cột `freeBeds` của danh sách phòng và danh sách giường trống |
| `RoomDetailPage` tải toàn bộ tòa nhà và loại phòng chỉ để lấy mã tòa và tên loại phòng | `RoomDetailPage.tsx` | Bổ sung `buildingCode`, `roomTypeName` vào `RoomDetail`; hiện tiêu đề nhấp nháy và thiếu thông tin nếu vai trò không có quyền đọc hai danh mục |
| Tab của `CrudTabsPage` không đồng bộ URL | `CrudTabsPage.tsx` | Từ `/staff/transfers/5` quay về `/staff/requests` luôn vào tab đầu |
| Lookup tải tối đa 100 trang tuần tự | `features/crud/api.ts` (`getLookupRecords`) | Với khoảng 1.500 sinh viên là khoảng 15 request nối tiếp; chuyển sang `Select` tìm phía server |
| `placeholderData: keepPreviousData` cho danh sách CRUD và `BedPicker` | `CrudPage.tsx`, `features/room/index.ts` | Tránh nhấp nháy khi đổi trang hoặc bộ lọc |
| Lazy-load route | `app/router/*Routes.tsx` | Trang mới của nhánh và `RolePermissionsPage` nên tải theo route |
| Mã lỗi chết sau khi chuyển sang Keycloak | `utils/errorMessages.ts` | `CSRF_TOKEN_INVALID`, `INVALID_CREDENTIALS`, `ACCOUNT_TEMP_LOCKED`, `PASSWORD_CHANGE_REQUIRED`, `WRONG_CURRENT_PASSWORD`, `PASSWORD_POLICY` không còn được backend phát; cần sửa cùng `API-Spec` |
| Mã lỗi viết thẳng ngoài registry | `services/api/client.ts`, `RequireRole.tsx`, `RolePermissionsPage.tsx` | Xuất hằng `ErrorCode` từ `errorMessages.ts` |
| Màu ngoài `theme.ts` | `statusConfig.ts`, `index.css`, `BedPicker.tsx` | Đưa bảng màu trạng thái và màu slate vào theme |
| Import trực tiếp primitive antd trong file cũ | `CrudPage.tsx`, `LoginPage`, `ChangePasswordPage`, `RolePermissionsPage`, `RoomCard`, `CrudTabsPage`, `NotificationBell` | Chuyển sang `components/ui` theo `CLAUDE.md` |

### 5.3 Tài liệu, script, test

| Việc | Ghi chú |
|---|---|
| Rút gọn khoảng 24 dòng trùng giữa `README.md` gốc và `DMS/backend/README.md` (mục phân quyền, `@RequirePrivilege`, Constants) | Chỉ giữ một bản, bản kia trỏ liên kết |
| Đồng bộ tham chiếu `DB-Design v0.7` trong SRS và SAD | Kèm nâng phiên bản và change log |
| Gom bốn hàm PowerShell trùng giữa `start-docker.ps1` và `start-local.ps1` (`Test-PortListening`, `Get-ManagedProcess`, `Stop-ManagedProcess`, `Wait-HttpReady`) vào một file dùng chung | Thấp |
| Tách lớp cơ sở cho fixture test (`createUser/createBuilding/createRoom/createStudent/createContract`) dùng chung giữa `BedOccupancyIntegrationTest`, `RoomLeaderIntegrationTest`, `CrudApiIntegrationTest`; thống nhất cách mock `AuditService` để dùng chung một context Spring | Giảm khoảng 90 dòng lặp |
| Bổ sung MockMvc cho `/api/transfer-requests/{id}/approve|reject|cancel` và `/api/beds/available` | Hiện chỉ gọi Service trực tiếp, annotation `@RequirePrivilege` của endpoint mới chỉ được ArchUnit kiểm tra |
| Test SQLState 23503 thật cho nhánh `DELETE` và ghi tham chiếu | Test FK hiện chỉ kiểm tra tầng Service trả 404 cho `roomTypeId` không tồn tại |

## 6. Tệp liên quan

- Sửa: `DMS/backend` (`DmsConstants`, `RoomService/Impl`, `BedOccupancyServiceImpl`, `RoomTransferServiceImpl`, `ContractCrudService/Impl/Controller`, `Maintenance*ServiceImpl`, `StudentManagement*`, 3 Policy, `AbstractSqlCrudServiceImpl`, README và 4 file test); `DMS/frontend` (`navigation.ts`, `statusConfig.ts`, `resources.ts`, `types.ts`, `CrudPage.tsx`, `BedCrudPanel.tsx`, `RoomDetailPage`, `TransferReviewPage`, `TransferRequestPage`, `TransferSummary`, `LeaderDialog`, `features/contract|room/index.ts`, `index.css`); `DMS/.env.example`; `README.md`; `API-Spec-DMS.md`; `DB-Design-DMS.md`.
- Mới: báo cáo này.
- Xóa: `pages/PlaceholderPage.tsx`, `pages/admin/AdminHomePage.tsx`, `pages/technician/TechnicianHomePage.tsx`, `types/common.ts`.

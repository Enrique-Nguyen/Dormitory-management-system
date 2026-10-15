# Báo cáo: Chiếm/trả giường, xét duyệt chuyển phòng và quản lý trưởng phòng

> Ngày hoàn thành: 15/10/2026
> Thực hiện: TV2 Nguyễn Phú Nguyên cùng Claude Code
> Phạm vi: `room`, `contract`, frontend phòng/chuyển phòng
> Căn cứ: SRS FR-ROOM-07..10, FR-CON-07/08, BR-01/02/05/11, AT-02/03/04/08/21; SAD 6.2; API-Spec 4.4, 4.7; UI 14.2, 15, 22A
> Trạng thái: Hoàn thành

---

## 1. Mục tiêu

Triển khai trọn vẹn nghiệp vụ quản lý chỗ ở tập trung trong ký túc xá, xét duyệt yêu cầu chuyển phòng, quản lý trưởng phòng và giao diện người dùng tương ứng cho cả Staff và Student:
- **Phạm vi hoàn thành:**
  - `BedOccupancyService` / `OccupancyQuery`: dịch vụ quản lý trạng thái chỗ ở tập trung (`occupyHold`, `occupyStay`, `convertHoldToStay`, `release`, `transfer`), khóa giường theo `bed_id` tăng dần, khóa sinh viên, chống deadlock và race condition;
  - `GET /api/beds/available`: tìm kiếm giường trống gom theo phòng cho split view, hỗ trợ nguồn đơn đăng ký và yêu cầu chuyển phòng;
  - Luồng xét duyệt chuyển phòng: `approve`, `reject`, `cancel` kèm `TransferDetail`, timeline và `allowedActions`;
  - Quản lý trưởng phòng: chỉ định, gỡ bỏ, cảnh báo thiếu trưởng phòng (`missingLeader`) và truy vấn lịch sử từ `audit_log`;
  - Migration Flyway V6 bổ sung quyền `ROOM_TRANSFER_APPROVE` cho vai trò `STAFF`;
  - Frontend: split view chọn giường (`BedPicker`), màn chi tiết phòng (`RoomDetailPage`), màn xét duyệt chuyển phòng (`TransferReviewPage`), màn tạo yêu cầu chuyển phòng cho sinh viên (`TransferRequestPage`).
- **Ngoài phạm vi:** Phân lại giường đầu HK1 (`storageRecordId`, FR-CON-12); cập nhật bạn cùng phòng và trưởng phòng vào `GET /api/me/room` (thuộc `OverviewController`); phân giường ban đầu, kích hoạt và trả phòng (phần việc của TV3).

## 2. Kết quả

Thực hiện qua 5 commit tương ứng với các bước trong kế hoạch:

| Commit | Nội dung |
|---|---|
| `dd0fc58` | BedOccupancyService |
| `35ec712` | beds/available |
| `ca6bba1` | Duyệt chuyển phòng, V6 |
| `0bf1c0b` | Trưởng phòng |
| `2d311f2` | Frontend |

## 3. Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| `./mvnw verify` (profile dev/testcontainers) | 108/108 test đạt, gồm ArchUnit (`ModuleDependencyTest`), `DmsApplicationTests`, `CrudApiIntegrationTest`, các ca kiểm thử bảo mật/Keycloak và 3 bộ test mới: `BedOccupancyIntegrationTest`, `RoomLeaderIntegrationTest`, `RoomTransferPolicyTest`. Bao quát các ca đồng thời AT-02 (hai request tranh một giường), AT-03 (hai sinh viên chuyển chéo phòng nhau), hai lượt duyệt đồng thời trên cùng yêu cầu chuyển phòng |
| `npm run lint` | Sạch (0 errors, 0 warnings) |
| `npm run build` | Sạch, biên dịch Vite + TypeScript thành công |

**Chưa kiểm tra:** Chưa chạy thử giao diện qua Keycloak thật; chưa có test runner frontend.

## 4. Điểm lệch so với thiết kế và quyết định cần xác nhận

| Nội dung | Quyết định | Lý do |
|---|---|---|
| Kiểm thử tầng Service | Dùng integration test với PostgreSQL thật thay cho unit test Mockito ở Service | Service gắn chặt với transaction, locking `SELECT FOR UPDATE` và ràng buộc CSDL; `AuditService` bị mock trong `BedOccupancyIntegrationTest` vì `audit_log` chỉ cho ghi thêm (append-only) |
| Vị trí test duyệt chuyển phòng | Gộp vào `BedOccupancyIntegrationTest` | Nghiệp vụ chuyển phòng liên quan chặt chẽ đến khóa và di chuyển chỗ ở của `BedOccupancyService` |
| Schema `beds/available` | Response có thêm `buildingId`, `priceMain`, `activeBeds` và `freeBeds.code` là mã ngắn | Phục vụ split view hiển thị đầy đủ thông tin phòng và ghép mã hiển thị phía frontend `${buildingCode}-${roomCode}-${bedCode}` |
| Vị trí endpoint chi tiết chuyển phòng | `GET /api/transfer-requests/{id}` chuyển sang `ContractController` và trả kiểu `TransferDetail` mới | `ContractCrudController` của khung chỉ trả DTO phẳng đơn giản; cần `TransferDetail` chứa `currentBed`, `timeline` và `allowedActions` |
| Endpoint `DELETE` chuyển phòng | Vẫn giữ endpoint `DELETE /api/transfer-requests/{id}` | Giữ tương thích với khung CRUD có sẵn; giao diện chuyển sang dùng `POST .../cancel` |
| Endpoint tạo yêu cầu chuyển phòng | Hiện dùng `POST /api/transfer-requests` (có `contractId` trong body); chưa có `POST /api/contracts/{id}/transfer-requests` | Khung CRUD ban đầu đã dựng đường dẫn này và frontend đang gọi; tránh tạo endpoint trùng lặp |
| Migration V6 | Không tăng `version` của dòng cấu hình trong `role_permission_matrix` | Tránh xung đột khóa lạc quan với màn quản trị ma trận quyền của Admin đang mở |
| Lỗi `LEADER_ALREADY_ASSIGNED` | Bắt lỗi chủ động trước khi va phải ràng buộc duy nhất CSDL `ux_room_leader` | Trả về kèm `details.roomCode` để người dùng biết sinh viên đang làm trưởng phòng nào; chỉ xảy ra khi dữ liệu bị lệch hoặc chạy đồng thời |
| Chuỗi thông báo trong mã | Một số chuỗi vẫn viết trực tiếp: tên key trong JSON details và câu "Trường bắt buộc" | Tuân thủ theo phong cách và quy ước mà codebase hiện có đang áp dụng |

## 5. Việc còn lại

| Việc | Người làm |
|---|---|
| Phân lại giường đầu HK1 (FR-CON-12) | TV2 |
| `/api/me/room` trả bạn cùng phòng và trưởng phòng | người phụ trách `OverviewController` |
| Gọi BedOccupancyService trong phân giường, kích hoạt, trả phòng; dùng lại `BedPicker` | TV3 |
| Badge "Yêu cầu chờ xử lý" | TV3, theo TODO trong `StaffLayout` |

## 6. Tệp liên quan

Lấy từ `git diff --stat d1d0ffd..HEAD`:

- **Sửa (29 tệp):**
  - Backend (14):
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/common/constants/DmsConstants.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/common/security/ScopeGuard.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/common/service/AuditService.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/common/service/impl/AuditServiceImpl.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/contract/api/ContractController.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/contract/api/ContractCrudController.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/contract/api/ContractDtos.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/contract/service/impl/ContractCrudServiceImpl.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/room/api/BedController.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/room/api/RoomDtos.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/room/service/RoomService.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/room/service/impl/RoomServiceImpl.java`
    - `DMS/backend/src/test/java/vn/edu/tlu/dms/KeycloakAccountIntegrationTest.java`
    - `DMS/backend/src/test/java/vn/edu/tlu/dms/ModuleDependencyTest.java`
  - Frontend (15):
    - `DMS/frontend/src/app/router/staffRoutes.tsx`
    - `DMS/frontend/src/app/router/studentRoutes.tsx`
    - `DMS/frontend/src/components/index.ts`
    - `DMS/frontend/src/components/ui/ActionBar.tsx`
    - `DMS/frontend/src/constants/authorization.ts`
    - `DMS/frontend/src/constants/navigation.ts`
    - `DMS/frontend/src/constants/statusConfig.ts`
    - `DMS/frontend/src/features/contract/index.ts`
    - `DMS/frontend/src/features/crud/resources.ts`
    - `DMS/frontend/src/features/crud/types.ts`
    - `DMS/frontend/src/features/room/index.ts`
    - `DMS/frontend/src/index.css`
    - `DMS/frontend/src/pages/CrudPage.tsx`
    - `DMS/frontend/src/utils/errorMessages.ts`
    - `DMS/frontend/src/utils/format.ts`

- **Mới (23 tệp):**
  - Backend (14):
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/common/service/PlacementContextLookup.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/contract/domain/RoomTransferPolicy.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/contract/service/RoomTransferService.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/contract/service/impl/RoomTransferServiceImpl.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/contract/service/impl/TransferPlacementLookupImpl.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/registration/service/impl/ApplicationPlacementLookupImpl.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/room/service/BedOccupancyService.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/room/service/OccupancyQuery.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/room/service/impl/BedOccupancyServiceImpl.java`
    - `DMS/backend/src/main/java/vn/edu/tlu/dms/room/service/impl/OccupancyQueryImpl.java`
    - `DMS/backend/src/main/resources/db/migration/V6__room_transfer_approve_permission.sql`
    - `DMS/backend/src/test/java/vn/edu/tlu/dms/BedOccupancyIntegrationTest.java`
    - `DMS/backend/src/test/java/vn/edu/tlu/dms/RoomLeaderIntegrationTest.java`
    - `DMS/backend/src/test/java/vn/edu/tlu/dms/RoomTransferPolicyTest.java`
  - Frontend (9):
    - `DMS/frontend/src/components/business/BedPicker.tsx`
    - `DMS/frontend/src/components/business/LeaderDialog.tsx`
    - `DMS/frontend/src/components/business/TransferSummary.tsx`
    - `DMS/frontend/src/features/room/BedCrudPanel.tsx`
    - `DMS/frontend/src/pages/staff/RoomDetailPage.tsx`
    - `DMS/frontend/src/pages/staff/TransferReviewPage.tsx`
    - `DMS/frontend/src/pages/student/TransferRequestPage.tsx`
    - `DMS/frontend/src/services/api/contract.ts`
    - `DMS/frontend/src/services/api/room.ts`

- **Xóa hoặc chuyển:** Không có.

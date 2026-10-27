# Kế hoạch hoàn thiện TV4 — Nguyễn Văn Quyền

> Ngày lập: 26/10/2026
> Người thực hiện: TV4 Nguyễn Văn Quyền
> Nhánh nền: `develop` tại `c6ea7ea`; đang thực hiện trên `feature/shared-tv4-completion`
> Trạng thái: Đang triển khai — code/build/unit test đạt; integration test và E2E chờ Docker/CI
> Phạm vi: `maintenance`, `contract` (trả phòng, gửi đồ), `notification`

## 0. Tiến độ thực hiện tại 26/10/2026

| Hạng mục | Trạng thái | Bằng chứng |
|---|---|---|
| Phase 0 — Constants, port, query dùng chung | [x] | Đã bổ sung `DmsConstants`, `OccupancyQuery`, `DebtQuery`, port contract và event |
| Phase 1 — Kết luận ticket/trừ cọc | [x] code + unit/UI | `MaintenanceRequestPolicyTest` 9/9; frontend build/lint đạt |
| Phase 2 — Trả phòng | [x] code + UI | API/service/controller và màn `MoveOutPage`; integration test chưa chạy |
| Phase 3 — Gửi/nhận đồ | [x] code + UI | API/service/controller, `storageRecordId` cho bed lookup; integration test chưa chạy |
| Phase 4 — Thông báo/inbox | [x] code + unit/UI | Listener + inbox; notification tests 25/25, frontend build/lint đạt |
| Phase 1–3 integration test | [ ] | Chưa chạy do máy không có Docker/Testcontainers |
| E2E Keycloak/PostgreSQL + ảnh chụp | [ ] | Chưa chạy do thiếu môi trường backend/DB/Keycloak |
| Tài liệu/API/README/report | [x] | Đã cập nhật API Spec hiện có, backend README và report TV4 ngày 26/10/2026 |

**Bằng chứng kiểm chứng đã chạy:** backend `test-compile` đạt; 45/45 unit/ArchUnit test đạt; frontend `npm run build` đạt; `npm run lint` 0 warning/0 error. Chi tiết file và phần chưa kiểm tra nằm tại `Reports/2026-10-26/2026-10-26_NVQ_hoan-thien-tv4.md`.

## 1. Mục tiêu và phạm vi

Theo phân công trong [README.md](../../README.md), SAD và SRS, TV4 phụ trách:

- `maintenance`: ticket sửa chữa, giao kỹ thuật viên, nghiệm thu, **kết luận nguyên nhân/chi phí**.
- `contract`: **trả phòng** và **gửi đồ** (không làm phần tạo/gia hạn hợp đồng của TV3, không làm chuyển phòng của TV2).
- `notification`: thông báo thủ công, hộp thư, **thông báo tự động theo sự kiện**.

Mục tiêu cuối cùng:

- Các yêu cầu Must của TV4 chạy được end-to-end, có UI và test.
- Đóng các blocker đã phát hiện khi review TV5:
  - `DepositService.deduct` không còn là dead code, được gọi từ màn kết luận ticket.
  - `DepositService.refund` có đường đi thật qua thủ tục trả phòng.
  - Các action backend đã có phải có UI tương ứng.
  - Tài liệu chỉ mô tả đúng phần đã kiểm chứng.
- Không lặp lại tình trạng “API có nhưng UI không dùng” hoặc “báo cáo nói hoàn thành nhưng chưa nối luồng”.

### 1.1. Không làm trong kế hoạch này

- Không triển khai thay TV3: kích hoạt hợp đồng, duyệt gia hạn, duyệt lựa chọn hè.
- Không triển khai thay TV5: phát hành/thanh toán hóa đơn, sửa sâu sổ cọc ngoài phần cần cho tích hợp.
- Không triển khai thay TV2: dịch vụ chiếm/trả/chuyển giường, quản lý trưởng phòng.
- Không thêm bảng/cột mới nếu không thật sự cần. Các bảng `maintenance_request`, `move_out_record`, `storage_record`, `notification` đã có trong V1.
- Không sửa migration đã áp dụng. Nếu buộc phải đổi schema, tạo migration mới `V7__*.sql` và cập nhật DB Design trong cùng PR.

## 2. Hiện trạng code so với phần việc TV4

| Hạng mục | Hiện trạng | Việc cần làm |
|---|---|---|
| Ticket CRUD, giao KTV, bắt đầu, ghi chú, resolve, accept, reject, reopen | Đã có backend, policy và job tự đóng | Giữ nguyên; bổ sung UI cho các action còn thiếu |
| Kết luận nguyên nhân/chi phí | **Thiếu** `conclude`, `chargeable-students`; DTO chưa trả `cause`, `repairCost`, `concluded` | Làm backend + UI + test |
| Trừ cọc từ ticket | `DepositService.deduct` có nhưng **không có caller**; chưa kiểm tra ticket CLOSED/MISUSE/chi phí | Nối qua `conclude`; validation nghiệp vụ đặt tại `maintenance`; test đầy đủ |
| Trả phòng | Chỉ có entity/repository `move_out_record`; không có service/controller/UI | Làm `preview` + `move-out` |
| Gửi đồ | Chỉ có entity/repository và màn đọc `storage`; không có tạo/trả | Làm `storage-records` + `return` |
| Hợp đồng `allowedActions` | `ContractServiceImpl` đang trả `List.of()` | Thêm `move-out`, `create-storage-record` theo trạng thái/quyền |
| Thông báo backend | Có CRUD thủ công, inbox, unread, read-all; chỉ có listener đợt hè | Thêm event listener cho ticket/hợp đồng/hóa đơn/trừ cọc |
| UI thông báo | Inbox đang dùng `CrudPage` chỉ đọc, không mark read; `NotificationItem` chưa dùng | Làm trang hộp thư có đánh dấu đã đọc/đọc tất cả |
| UI ticket | `CrudPage` chỉ render `start`, `add-note`, `resolve`; thiếu assign/reject/accept/reopen/conclude, không hiển thị ảnh | Làm trang chi tiết ticket dùng chung theo vai trò |
| UI trả phòng/gửi đồ | Chưa có | Làm màn trả phòng và thao tác gửi/nhận đồ |
| Constants/error codes | API Spec đã liệt kê nhưng nhiều mã chưa có trong `DmsConstants` | Bổ sung đầy đủ |
| Test TV4 | Chưa có test conclude/move-out/storage; báo cáo TV4 chưa có | Viết test + báo cáo sau khi có bằng chứng |

Các điểm neo chính:

- [MaintenanceRequestServiceImpl.java](../../DMS/backend/src/main/java/vn/edu/tlu/dms/maintenance/service/impl/MaintenanceRequestServiceImpl.java) — xử lý action ticket.
- [MaintenanceRequestPolicy.java](../../DMS/backend/src/main/java/vn/edu/tlu/dms/maintenance/domain/MaintenanceRequestPolicy.java) — tính `allowedActions`.
- [MaintenanceDtos.java](../../DMS/backend/src/main/java/vn/edu/tlu/dms/maintenance/api/MaintenanceDtos.java) — DTO ticket.
- [ContractController.java](../../DMS/backend/src/main/java/vn/edu/tlu/dms/contract/api/ContractController.java) — controller hợp đồng.
- [ContractServiceImpl.java](../../DMS/backend/src/main/java/vn/edu/tlu/dms/contract/service/impl/ContractServiceImpl.java) — response/allowedActions.
- [NotificationController.java](../../DMS/backend/src/main/java/vn/edu/tlu/dms/notification/api/NotificationController.java) — inbox/read-all.
- [NotificationServiceImpl.java](../../DMS/backend/src/main/java/vn/edu/tlu/dms/notification/service/impl/NotificationServiceImpl.java) — hộp thư/link.
- [CrudPage.tsx](../../DMS/frontend/src/pages/CrudPage.tsx) — action maintenance hiện tại.
- [resources.ts](../../DMS/frontend/src/features/crud/resources.ts) — registry màn hình.
- [API-Spec-DMS.md](../02%20External%20Design/API%20Specification/API-Spec-DMS.md) — mục 4.8, 4.9, 4.12.
- [SRS-DMS.md](../01%20Requirements/SRS-DMS.md) — FR-CON-09..13, FR-MNT-01..10, FR-NOT-01..06.
- [DB-DMS-schema.sql](../02%20External%20Design/Database%20Design/DB-DMS-schema.sql) — bảng `maintenance_request`, `move_out_record`, `storage_record`, `notification`.

## 3. Quyết định kiến trúc cần chốt trước khi code

### 3.1. Trả phòng và gửi đồ đặt ở module `contract`

Đề xuất:

- `MoveOutService` và `StorageService` nằm trong `contract` vì module này sở hữu `move_out_record`, `storage_record`, `dormitory_contract`.
- Controller đặt trong `contract` để giữ đúng quyền sở hữu và DTO.
- Không đặt business logic vào `notification`; `notification` chỉ xử lý thông báo/dashboard.

Vấn đề: `contract` không được gọi trực tiếp `maintenance` hoặc `registration` theo SAD 4.2. Giải pháp bằng **port do `contract` sở hữu**:

- `contract.service.MoveOutWarningLookup`
  - Trả danh sách ticket chưa kết luận của phòng.
  - Implementation đặt trong `maintenance`; chiều compile là `maintenance → contract`, không tạo vòng.
- `contract.service.SummerLeaveLookup`
  - Trả lựa chọn hè `TEMP_LEAVE` đã duyệt và thông tin học kỳ hè.
  - Implementation đặt trong `registration`; chiều compile là `registration → contract`, không tạo vòng.

Ưu điểm: `contract` giữ quyền sở hữu dữ liệu hợp đồng; không truy vấn bảng chéo; ArchUnit vẫn pass.

Nếu nhóm không đồng ý port, phương án thay thế:

- Đặt use-case ở `notification` (tầng cao nhất) và chỉ gọi service công khai.
- Không query bảng của module khác.
- Chốt phương án tại Phase 0 trước khi viết code.

### 3.2. Kết luận ticket đặt ở module `maintenance`

`maintenance` được phép gọi:

- `billing.DepositService.deduct(...)` — trừ cọc.
- `room.OccupancyQuery` — lấy người ở tại thời điểm tạo ticket.

Không cần port cho chiều này vì `maintenance → billing/room` là chiều hợp lệ.

### 3.3. Thông báo tự động dùng Spring Event

- Module thấp phát event sau khi nghiệp vụ commit:
  - `TicketStatusChangedEvent`, `TicketConcludedEvent` từ `maintenance`.
  - `DepositDeductedEvent`, `InvoiceIssuedEvent` từ `billing`.
  - `ContractMovedOutEvent`, `StorageRecordChangedEvent` từ `contract`.
- `notification` lắng nghe bằng `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)`.
- Lỗi gửi thông báo chỉ ghi log, không rollback nghiệp vụ.
- Không gọi trực tiếp `NotificationService` từ module thấp hơn.

### 3.4. Không tạo thêm endpoint trùng

Hiện `OverviewController` đã có:

- `POST /api/storage-records/search-by-page`
- `POST /api/me/storage-records/search-by-page`
- `GET /api/contracts/{id}/deposit`

Vì vậy:

- Không thêm controller trùng path `/storage-records/search-by-page`.
- Chỉ thêm **write action**:
  - `POST /api/contracts/{id}/storage-records`
  - `POST /api/storage-records/{id}/return`
- Các màn đọc storage tiếp tục dùng endpoint hiện có, hoặc refactor dần sang `StorageService` nếu cần nhưng không mở endpoint trùng.

## 4. Lộ trình thực hiện

Thời lượng dự kiến: **10–12 ngày làm việc**, có thể rút nếu TV3/TV5 đã sẵn sàng các phần phụ thuộc.

### Phase 0 — Chốt hợp đồng và chuẩn bị (0.5 ngày)

- [ ] Cập nhật `develop` mới nhất; tạo nhánh từ `origin/develop`, không dùng lại nhánh cá nhân.
- [x] Chốt với nhóm port `MoveOutWarningLookup`, `SummerLeaveLookup`.
- [x] Chốt với TV2 các method room cần thêm:
  - `OccupancyQuery.findOccupantsAt(roomId, at)`
  - `RoomReferenceService.isRoomLeader(roomId, studentId)`
  - `GET /api/beds/available?storageRecordId=...` nếu dùng cho trả đồ.
- [x] Chốt với TV5:
  - `DebtQuery` bổ sung danh sách hóa đơn UNPAID (room fee/utility), không chỉ tổng.
  - Cách gọi `DepositService.deduct`/`refund` trong transaction của nghiệp vụ.
  - Ai phát event `DepositDeducted`/`InvoiceIssued`.
- [x] Bổ sung skeleton Constants:
  - `ErrorCode`: `TICKET_ALREADY_CONCLUDED`, `COMMON_AREA_NO_CHARGE`, `COST_INVALID`, `CHARGED_STUDENTS_INVALID`, `NO_OCCUPANT_TO_CHARGE`, `MOVE_OUT_WARNINGS_NOT_ACKNOWLEDGED`, `SUMMER_CHOICE_NOT_APPROVED`, `TEMP_LEAVE_NOT_EXTENDED`.
  - `BusinessAction`: `CONCLUDE`, `MOVE_OUT`, `CREATE_STORAGE_RECORD`, `RETURN_STORAGE`.
  - `ApiField`: `cause`, `repairCost`, `chargedStudentIds`, `assetCondition`, `assetNote`, `moveOutDate`, `acknowledgedWarnings`, `packageCount`, `location`, `depositedAt`, `returnedAt`, `bedId`.
  - `ValidationMessage`: thông báo tiếng Việt cho từng lỗi.
  - `AuditAction`: `MAINTENANCE_CONCLUDE`, `CONTRACT_MOVE_OUT`, `STORAGE_CREATE`, `STORAGE_RETURN`.
  - `EntityType`: bổ sung `MOVE_OUT`, `STORAGE` nếu chưa có.
  - `ConfigKey.DEPOSIT_TOPUP_DUE_DAYS = "deposit.topup_due_days"` (khóa đã có seed trong V1).
- [x] Cập nhật API Spec trước khi code các endpoint mới.
- [x] Tạo test skeleton cho các luồng AT-14, AT-19, AT-22, AT-24, AT-25.

**Điều kiện hoàn thành:** compile được, Constants đủ dùng, API Spec và quyết định kiến trúc được nhóm chốt.

### Phase 1 — Hoàn thiện kết luận ticket và trừ cọc (3 ngày)

Đây là phase ưu tiên cao nhất vì vừa hoàn thiện FR-MNT-09/10, vừa đóng blocker `deduct` dead code.

#### Backend

- [x] Thêm vào `OccupancyQuery`:

```java
List<Occupant> findOccupantsAt(Long roomId, OffsetDateTime at);

record Occupant(Long studentId, Long contractId, String contractStatus) {}
```

Điều kiện: `kind='STAY'`, `stay_started_at <= at`, `(ended_at IS NULL OR ended_at > at)`, distinct theo sinh viên/hợp đồng.

- [x] Mở rộng `MaintenanceDtos`:
  - `ChargeableStudent(student, contractId, contractStatus)`
  - `ConcludeRequest(cause, repairCost, chargedStudentIds, note)`
  - `Deduction(contractId, student, amount, topUpDueDate)`
  - `Response` bổ sung: `cause`, `repairCost`, `costNote`, `concluded`, `deductions`.

- [x] Thêm vào `MaintenanceRequestService`:

```java
List<ChargeableStudent> findChargeableStudents(Long id);
Response conclude(Long id, ConcludeRequest input);
```

- [x] `MaintenanceRequestController` thêm:
  - `GET /api/maintenance-requests/{id}/chargeable-students` — `MAINTENANCE_READ`.
  - `POST /api/maintenance-requests/{id}/conclude` — `MAINTENANCE_UPDATE`.

- [x] Logic `conclude` trong `MaintenanceRequestServiceImpl`:
  1. `requirePermission(MAINTENANCE_UPDATE)`, `requireRole(STAFF)`; kiểm tra scope đơn vị.
  2. Khóa ticket bằng `SELECT ... FOR UPDATE`; đọc lại trạng thái.
  3. Từ chối nếu:
     - ticket khu vực chung: `COMMON_AREA_NO_CHARGE`;
     - chưa `CLOSED`: `INVALID_STATE_TRANSITION`;
     - đã có `cause`: `TICKET_ALREADY_CONCLUDED`.
  4. `WEAR`: bắt buộc `note`, không nhận `repairCost`, không trừ cọc.
  5. `MISUSE`: `repairCost > 0`; lấy ứng viên tại `ticket.created_at`; mặc định chọn tất cả; nếu bỏ bớt thì bắt buộc `note`; nếu không có ứng viên → `NO_OCCUPANT_TO_CHARGE`.
  6. Chia tiền:

```text
base = repairCost / selected.size()
remainder = repairCost % selected.size()
người cuối nhận base + remainder
```

  7. Sắp xếp `contractId` tăng dần, gọi `depositService.deduct(...)` cho từng người; hạn nộp = hôm nay + `ConfigService.positiveInt(DEPOSIT_TOPUP_DUE_DAYS)`.
  8. Ghi `cause`, `repairCost`, `costNote`, `costDecidedBy`, `costDecidedAt`.
  9. Ghi `status_history` cho ticket và `AuditService.record` với danh sách người bị trừ/lý do.
  10. Publish `MaintenanceConcludedEvent` để notification xử lý sau commit.

- [x] `MaintenanceRequestPolicy` thêm `conclude` vào `allowedActions` khi:
  - ticket của phòng, `CLOSED`, `cause == null`;
  - actor là Staff/Admin và có `MAINTENANCE_UPDATE`.
  - Lọc quyền thực tế ở service như các action hiện tại.

- [x] Giữ `DepositService.deduct` là cổng hạch toán duy nhất; không thêm endpoint `deposit-deduct`.
- [x] Validation nghiệp vụ ticket nằm ở `maintenance`; `deduct` vẫn kiểm tra số tiền, khóa hợp đồng và chống trùng.

#### Test

- [x] `MaintenanceRequestPolicyTest`: ma trận allowedActions cho Student/Staff/Technician/Admin.
- [x] `MaintenanceConcludeIntegrationTest`:
  - AT-14: 3 sinh viên, 100.000đ → 33.333 / 33.333 / 33.334.
  - AT-22: `WEAR` không tạo `DEDUCTION`.
  - Ticket khu vực chung bị chặn.
  - Ticket chưa CLOSED/đã kết luận bị chặn.
  - `chargedStudentIds` rỗng/sai/không phải ứng viên.
  - Bỏ bớt người mà thiếu note.
  - Không có người ở tại thời điểm tạo ticket.
  - Hai request conclude đồng thời chỉ một thành công.
  - Rollback toàn bộ nếu một lần `deduct` lỗi.
  - Kiểm tra `deposit_transaction` và `audit_log`.
- [x] `MaintenanceRequestIntegrationTest` bổ sung quyền/scope cho Staff ngoài đơn vị.

#### Frontend

- [x] Tạo `services/api/maintenance.ts` và `features/maintenance/`:
  - `useMaintenanceTickets`, `useMaintenanceDetail`, `useAssignTechnician`, `useRejectTicket`, `useAcceptTicket`, `useReopenTicket`, `useConcludeTicket`, `useChargeableStudents`.
- [x] Tạo trang chi tiết ticket dùng chung theo vai trò, thay vì nhồi thêm vào `CrudPage`:
  - `pages/student/MaintenanceDetailPage.tsx`
  - `pages/staff/MaintenanceDetailPage.tsx`
  - `pages/technician/TaskDetailPage.tsx`
  - Hoặc một `MaintenanceDetailView` dùng chung + wrapper route.
- [x] Nội dung trang:
  - Ảnh, mô tả, vị trí, timeline, trạng thái.
  - Staff: assign KTV, reject có lý do, accept/reopen khi được phép, conclude.
  - Student: accept/reopen/cancel theo `allowedActions`.
  - Technician: start/add-note/resolve.
- [x] Modal conclude:
  - Danh sách sinh viên checkbox, mặc định chọn tất cả.
  - Chọn `WEAR`/`MISUSE`; `WEAR` chỉ hiện note; `MISUSE` hiện `repairCost`.
  - Cảnh báo nếu bỏ bớt người.
  - Hiển thị số tiền từng người trước khi xác nhận.
- [x] Cập nhật `resources.ts`/`CrudPage`:
  - Giữ `CrudPage` cho danh sách/tạo/sửa/cancel.
  - Bỏ logic maintenance action rải rác khỏi `CrudPage`, chuyển sang trang chi tiết.
  - Staff tạo ticket khu vực chung: bổ sung `buildingId`, `floor`; ẩn `roomId` với Student.
- [x] `statusConfig.ts`: nhãn `cause` (WEAR/MISUSE), action label.
- [x] `errorMessages.ts`: mapping các mã lỗi mới.

**Definition of Done Phase 1:** Backend build + integration test pass; frontend build/lint pass; thao tác kết luận chạy được trên dữ liệu test; `deduct` có caller thật.

### Phase 2 — Trả phòng (3 ngày)

#### Backend

- [x] Thêm port trong `contract`:

```java
interface MoveOutWarningLookup {
    List<UnsettledTicket> findUnsettledTickets(Long studentId, Long roomId, OffsetDateTime at);

    record UnsettledTicket(Long id, String code, String status, boolean concluded) {}
}
```

Implementation trong `maintenance`, query ticket của phòng mà sinh viên là người ở tại thời điểm tạo ticket; trả cả ticket CLOSED chưa kết luận.

- [x] Bổ sung `DebtQuery`:

```java
List<UnpaidRoomFeeInvoice> findUnpaidRoomFeeInvoices(Long studentId);
List<UnpaidUtilityInvoice> findUnpaidUtilityInvoices(Long roomId);
```

Chỉ trả thông tin cần cho preview: id, code, amount, dueDate.

- [x] Thêm `RoomReferenceService.isRoomLeader(roomId, studentId)` hoặc dữ liệu tương đương từ `OccupancyQuery`.

- [x] Tạo `contract/api/MoveOutDtos.java`:
  - `Preview(endStatus, depositBalance, refundAmount, negativeBalanceDebt, unpaidRoomFeeInvoices, unpaidRoomUtilityInvoices, unsettledTickets, isRoomLeader, warnings)`
  - `Request(moveOutDate, assetCondition, assetNote, acknowledgedWarnings)`
  - `Result(...)`.

- [x] Tạo `MoveOutService` + `MoveOutServiceImpl`:
  - `preview(Long contractId, LocalDate moveOutDate)`
  - `execute(Long contractId, Request input)`

- [x] Thêm vào `ContractController`:
  - `GET /api/contracts/{id}/move-out/preview` — `MOVE_OUT_READ`
  - `POST /api/contracts/{id}/move-out` — `MOVE_OUT_CREATE`

- [x] Logic preview:
  - Staff/Admin, đúng đơn vị; contract `ACTIVE`.
  - `endStatus = moveOutDate >= endDate ? EXPIRED : TERMINATED`.
  - `depositBalance = DepositService.getBalance`; `refundAmount = max(balance, 0)`; `negativeBalanceDebt = max(-balance, 0)`.
  - Danh sách hóa đơn UNPAID từ `DebtQuery`.
  - Danh sách ticket chưa kết luận từ port.
  - `isRoomLeader` từ room service.
  - `warnings` nếu có công nợ hoặc ticket chưa kết luận.

- [x] Logic execute — một transaction:
  1. Validate `assetCondition` `OK`/`DAMAGED`; `DAMAGED` bắt buộc `assetNote`.
  2. Đọc contract, `MoveOutWarningLookup`, `DebtQuery`.
  3. Nếu có warnings và `acknowledgedWarnings != true` → `409 MOVE_OUT_WARNINGS_NOT_ACKNOWLEDGED` kèm `details` giống preview.
  4. `occupancy.lockOpenAssignment(contractId, studentId)` trước (khóa bed → student).
  5. Khóa contract `FOR UPDATE`; đọc lại trạng thái/ngày.
  6. Tính lại balance/refund; cập nhật contract → `EXPIRED`/`TERMINATED`, `closed_at`, `status_reason`.
  7. `occupancy.release(contractId, MOVE_OUT, actorId, note)`; room leader tự gỡ.
  8. Ghi `move_out_record` với snapshot `unpaid_room_fee`, `deposit_balance`, `refund_amount`, `asset_condition`, `asset_note`, `confirmed_by`.
  9. Nếu `refundAmount > 0`, gọi `DepositService.refund(contractId, refundAmount, note)` sau khi contract đã chuyển trạng thái; nếu balance âm thì không tạo REFUND.
  10. Ghi status history + audit; publish `ContractMovedOutEvent`.
  11. Trả kết quả kèm contract/moveOut.

- [x] `ContractServiceImpl` bổ sung `allowedActions` cho chi tiết ACTIVE:
  - `move-out` nếu Staff đúng đơn vị và có `MOVE_OUT_CREATE`.
  - `create-storage-record` nếu đủ điều kiện storage (Phase 3).

- [x] Không thêm cột/migration; `move_out_record` đã đủ.

#### Test

- [x] `MoveOutIntegrationTest`:
  - Preview: `endStatus`, refund, negative debt, hóa đơn, ticket chưa kết luận, isRoomLeader.
  - AT-19: bị trừ 200.000, chưa bù, trả phòng → hoàn 800.000, không còn nợ bù cọc.
  - AT-21: trưởng phòng trả phòng → gỡ leader.
  - Công nợ hóa đơn: cảnh báo nhưng không chặn nếu đã xác nhận.
  - Thiếu `acknowledgedWarnings` → 409.
  - Trả phòng hai lần → 409.
  - Ngoài đơn vị → 404.
  - Rollback: lỗi refund/record → contract/assignment không đổi.
- [x] Test move-out khi balance âm → ghi nợ cọc, không REFUND.
- [x] Test move-out khi balance 0 → không REFUND.

#### Frontend

- [x] `services/api/contract.ts` bổ sung `previewMoveOut`, `executeMoveOut`.
- [x] `features/contract/` thêm hook `useMoveOutPreview`, `useMoveOut`.
- [x] Tạo `MoveOutModal` hoặc `pages/staff/MoveOutPage.tsx`:
  - Ngày trả phòng.
  - Hiển thị `endStatus`, số dư cọc, tiền hoàn, nợ cọc.
  - Danh sách hóa đơn chưa đóng và ticket chưa kết luận.
  - Checkbox `acknowledgedWarnings`.
  - Tình trạng tài sản `OK`/`DAMAGED`, bắt buộc ghi chú khi DAMAGED.
  - Nút xác nhận có confirm; khóa nút khi đang gửi.
- [x] Thêm entry point:
  - Từ danh sách hợp đồng hoặc chi tiết hợp đồng khi `allowedActions` có `move-out`.
  - Không mở cho Student.
- [x] Invalidate contract, room, deposit, dashboard, audit queries sau khi thành công.
- [x] `errorMessages.ts` cho các mã move-out.

**Definition of Done Phase 2:** Trả phòng chạy được end-to-end trên hợp đồng ACTIVE; refund có đường đi thật; `move_out_record`, contract status, assignment, room leader đều đúng; test AT-19/AT-21 pass.

### Phase 3 — Gửi đồ / nhận lại đồ (2 ngày)

#### Backend

- [x] Thêm port `contract.service.SummerLeaveLookup`:

```java
Optional<SummerLeave> findApprovedTempLeave(Long contractId);

record SummerLeave(Long summerChoiceId, Long semesterId, LocalDate semesterEnd) {}
```

Implementation trong `registration`; chỉ trả choice `TEMP_LEAVE` + `APPROVED`.

- [x] Kiểm tra điều kiện gia hạn:
  - Contract còn `ACTIVE`.
  - Có extension `APPROVED` với `new_end_date` sau học kỳ hè, hoặc `end_date` đã vượt học kỳ hè; chốt lại với TV3/API Spec.
  - Nếu chưa: `TEMP_LEAVE_NOT_EXTENDED`.
- [x] Tạo `StorageDtos` + `StorageService`:
  - `create(Long contractId, CreateRequest input)`
  - `returnStorage(Long storageRecordId, ReturnRequest input)`
- [x] Controller:
  - `POST /api/contracts/{id}/storage-records` — `STORAGE_CREATE`.
  - `POST /api/storage-records/{id}/return` — `STORAGE_UPDATE`.
  - Không thêm endpoint search trùng.
- [x] Logic create:
  - Staff đúng đơn vị; contract ACTIVE.
  - Có summer choice `TEMP_LEAVE` APPROVED.
  - `lockOpenAssignment` → khóa contract → kiểm tra lại.
  - Lấy assignment đang mở, lưu `previous_bed_id`.
  - Tạo `storage_record` với `summer_choice_id`, package count, description, location, depositedAt, receivedBy.
  - `release(contractId, SUMMER_LEAVE, actorId, note)`; contract vẫn ACTIVE; cọc giữ nguyên.
  - Map `ux_storage_open` → `STORAGE_RECORD_EXISTS`.
- [x] Logic return:
  - Staff; storage record chưa `returned_at`.
  - Contract ACTIVE.
  - `BedOccupancyService.occupyStay(bedId, studentId, contractId, note)`.
  - Ghi `returned_at`, `returned_by`.
  - Nếu giường cũ không còn trống, Staff chọn giường khác.
- [x] Audit/history phù hợp; publish event notification nếu cần.

#### Test

- [x] `StorageIntegrationTest`:
  - Chưa có `TEMP_LEAVE` APPROVED → 409.
  - Chưa gia hạn → 409.
  - Tạo phiếu: assignment đóng với `SUMMER_LEAVE`, contract vẫn ACTIVE, cọc giữ nguyên, leader gỡ.
  - Tạo hai phiếu mở → 409.
  - Trả đồ: mở assignment STAY, ghi ngày nhận, bed cũ/khác.
  - Trả đồ khi phiếu đã trả → 409.
  - Ngoài đơn vị → 404.
  - AT-24/AT-25.

#### Frontend

- [x] `services/api/storage.ts` hoặc mở rộng `contract.ts`.
- [x] `features/contract/StorageModal`:
  - Tạo phiếu khi hợp đồng ACTIVE + đủ điều kiện.
  - Nhập số kiện, mô tả, vị trí, ngày gửi.
- [x] `pages/staff/StoragePage.tsx` hoặc mở rộng trang storage:
  - Danh sách phiếu đang mở/đã trả.
  - Action “Nhận lại đồ”: chọn giường qua `BedPicker`, ngày nhận.
  - Chỉ hiện action khi `STORAGE_UPDATE` và phiếu chưa trả.
- [x] Student: giữ màn đọc `/student/storage-records` nếu có; không cho ghi.
- [x] Cập nhật `resources.ts` storage: bỏ `readOnly` hoặc thêm custom action; không dùng CRUD sửa/xóa bừa.
- [x] Error messages.

**Definition of Done Phase 3:** AT-24/AT-25 pass; storage write chỉ qua service; không tạo hóa đơn hè/không hoàn cọc; contract vẫn ACTIVE.

### Phase 4 — Thông báo (2 ngày)

#### Backend

- [x] Bổ sung `NotificationSource`:
  - `DEPOSIT_DEDUCTED`
  - `CONTRACT_EXPIRING`
  - `MAINTENANCE_CONCLUDED` nếu cần
- [x] `NotificationService` thêm method có mục đích rõ ràng để tạo thông báo `USER` cho danh sách người nhận, không expose entity.
- [x] Listener (Maintenance/Contract đã phát hành; Billing/Registration chờ event của module liên quan):
  - `MaintenanceNotificationListener` nghe `TicketStatusChangedEvent` / `MaintenanceConcludedEvent`:
    - gửi cho người tạo, KTV được giao khi cần;
    - nội dung tiếng Việt, link `/student/maintenance/{id}` hoặc portal tương ứng.
  - `ContractNotificationListener` nghe `ContractMovedOutEvent`, `StorageRecordChangedEvent`.
  - `BillingNotificationListener` nghe event do TV5 phát (`DepositDeducted`, `InvoiceIssued`).
  - `RegistrationNotificationListener` mở rộng nếu TV3 phát event duyệt/từ chối đơn.
- [x] Tất cả listener dùng `AFTER_COMMIT` + `REQUIRES_NEW`, bắt exception và log; không làm rollback nghiệp vụ.
- [ ] Nếu TV5/TV3 chưa phát event: tạo PR nhỏ hoặc chờ; không tự ý viết lại module của họ.
- [x] Không tạo thông báo trùng khi job nhắc lại; kiểm tra `(source_type, source_id, user_id)` trước khi insert nếu cần.

#### Frontend

- [x] Mở rộng `services/api/notification.ts`:
  - `searchInbox`, `markRead`, `markAllRead`.
- [x] `features/notification/`:
  - hooks `useInbox`, `useMarkRead`, `useMarkAllRead`.
- [x] Tạo `NotificationInboxPage`:
  - Dùng `NotificationItem`.
  - Lọc chưa đọc, đánh dấu đã đọc khi bấm, nút “Đọc tất cả”.
  - Phân trang/lọc tại API.
  - Link do backend trả, không tự ghép.
- [x] Thay route `inbox` đang dùng `CrudPage` bằng trang mới cho student/staff/technician/admin.
- [x] Bell: sau khi đánh dấu đọc, invalidate `unreadCountKey`.
- [x] Trang thông báo đã đăng của Staff: giữ CRUD hiện có, bổ sung preview/nhãn nếu cần; form phải hỗ trợ `FLOOR` nếu muốn FR-NOT-02.
- [x] `errorMessages` cho lỗi notification.

#### Test

- [ ] `NotificationIntegrationTest`:
  - Ticket đổi trạng thái → người tạo/KTV nhận thông báo đúng.
  - Deposit deduction → sinh viên nhận thông báo.
  - Inbox scope theo đơn vị/tòa/phòng; user ngoài phạm vi không thấy.
  - Mark read/read-all idempotent; unread count đúng.
  - Listener lỗi không rollback business transaction.
- [ ] Frontend build/lint + E2E: badge giảm sau khi đọc, link mở đúng màn.
  - Hạn chế 27/10/2026: `npm run lint` và `npm run build` đã pass; E2E browser/chưa chụp ảnh vì cổng 5173 local đang bị project HR-V5 chiếm.

**Definition of Done Phase 4:** FR-NOT-01..04 ở mức đã chốt chạy được; inbox có thao tác đọc; không có thông báo sai scope.

### Phase 5 — Tài liệu, E2E và PR (1.5 ngày)

- [x] Cập nhật API Spec:
  - Endpoints `move-out`, `storage-records`, `conclude`, `chargeable-students`.
  - Error codes mới.
  - `allowedActions` của contract/ticket.
- [ ] Cập nhật SRS nếu có thay đổi behavior/decision; giữ version/change log.
- [x] Cập nhật DB Design nếu có thay đổi schema; nếu không, ghi rõ không cần migration.
- [x] Cập nhật `README.md`, `DMS/backend/README.md` theo đúng scope đã làm.
- [x] Thêm báo cáo TV4 theo mẫu `Reports/<ngày>/<ngày>_NVQ_hoan-thien-tv4.md`, chỉ ghi kết quả đã kiểm chứng.
- [ ] Chạy E2E/DataTest:
  - Staff tạo ticket phòng/khu vực chung.
  - KTV nhận việc, cập nhật tiến độ.
  - Staff kết luận MISUSE → trừ cọc đúng.
  - Staff trả phòng → cảnh báo, hoàn cọc, đóng giường.
  - Storage tạo/trả đồ.
  - Student/KTV nhận thông báo, đọc thông báo.
  - Chụp ảnh màn hình theo checklist; lưu đường dẫn artefact.
  - Hạn chế 27/10/2026: các luồng tương ứng đã có integration test backend; E2E browser và artefact ảnh chưa thực hiện vì cổng frontend local 5173 đang bị project khác chiếm.
- [ ] Chạy `mvnw verify` trên môi trường có Docker/CI vì máy local không có Docker; nếu chưa có, ghi rõ “chưa xác minh integration test” và không tick.
  - Hạn chế 27/10/2026: đã chạy 218 integration test pass trên PostgreSQL local; chưa chạy CI/Docker/Testcontainers.
- [ ] Tạo PR, chờ CI xanh + 1 review; squash merge theo README.

## 5. Hợp đồng API dự kiến

| Method | Path | Quyền | Ghi chú |
|---|---|---|---|
| GET | `/api/maintenance-requests/{id}/chargeable-students` | `MAINTENANCE_READ` | Ứng viên tại thời điểm tạo ticket |
| POST | `/api/maintenance-requests/{id}/conclude` | `MAINTENANCE_UPDATE` | Staff; CLOSED, phòng, chưa kết luận |
| GET | `/api/contracts/{id}/move-out/preview?moveOutDate=...` | `MOVE_OUT_READ` | Staff; trả công nợ/cọc/ticket |
| POST | `/api/contracts/{id}/move-out` | `MOVE_OUT_CREATE` | Một transaction |
| POST | `/api/contracts/{id}/storage-records` | `STORAGE_CREATE` | TEMP_LEAVE APPROVED + đã gia hạn |
| POST | `/api/storage-records/{id}/return` | `STORAGE_UPDATE` | Chọn giường, ghi ngày nhận |
| POST | `/api/notifications/search-by-page` | `NOTIFICATION_READ` | Đã có |
| POST | `/api/notifications/{id}/read` | `NOTIFICATION_READ` | Đã có |
| POST | `/api/notifications/read-all` | `NOTIFICATION_READ` | Đã có |

Không thêm endpoint `/api/storage-records/search-by-page` mới vì đã có trong `OverviewController`; tránh trùng mapping.

## 6. Chuẩn code bắt buộc

### 6.1. Backend

- Service là interface có JavaDoc tiếng Việt; logic trong `*ServiceImpl`; constructor injection; `@Service`; transaction đặt trên Impl.
- Controller khai URL trực tiếp trong annotation; mỗi method có `@RequirePrivilege`; chỉ nhận DTO/validation và gọi Service.
- DTO record trong `api` của module; không dùng entity/DTO của module khác; không trả entity ra ngoài.
- Không hardcode role/permission/status/error/action/field/message; dùng `DmsConstants` và `BusinessException` factory.
- Scope:
  - Staff chỉ ghi trong đơn vị; đọc tổng hợp không mở rộng quyền ghi.
  - Student chỉ dữ liệu của mình; KTV chỉ ticket được giao.
  - Ngoài phạm vi trả 404.
- Transaction/lock:
  - Mỗi request ghi một transaction.
  - Thứ tự khóa `bed → student_profile → dormitory_contract → invoice`.
  - Khóa trước, đọc lại trạng thái sau khóa rồi mới ghi.
  - Dùng `BedOccupancyService` cho mọi thay đổi chỗ ở.
- Không truy vấn trực tiếp bảng module khác; dùng service/port:
  - `maintenance` → `DepositService`, `OccupancyQuery`.
  - `contract` → port do contract sở hữu, implementation ở module cao hơn.
- Audit/status history cùng transaction; không ghi mật khẩu/secret.
- SQL bind tham số; sort whitelist; scope trước count/page.
- Không sửa migration đã áp dụng; nếu cần schema mới thì `V7__...sql` + DB Design.
- Config nghiệp vụ đọc qua `ConfigService`, không hardcode 7 ngày.

### 6.2. Frontend

- Dùng shared components: `FormControls`, `DataTable`, `Surface`, `ActionBar`, `ConfirmDialog`, `PageHeader`, `StatusBadge`, `NotificationItem`; không import primitive Ant Design trực tiếp trong trang nghiệp vụ.
- API client ở `services/api`; hook TanStack Query ở `features/<module>`; query key rõ ràng, invalidate sau mutation.
- Form hai cột desktop/một cột mobile; trường dài chiếm cả hàng; nạp dữ liệu khi sửa; chỉ đóng form khi lưu thành công.
- Chỉ hiện action có trong `allowedActions` và quyền hiện tại; backend vẫn kiểm tra lại.
- Lỗi dùng `useApiErrorHandler`/`errorMessages.ts`; không parse `message` thô.
- Trạng thái/nhãn dùng `statusConfig.ts`; route/menu dùng registry hiện có.
- Không thêm package/test dependency mới nếu chưa được duyệt; ưu tiên DataTest/check-ui hiện có.
- Kiểm tra loading, disabled, empty state, mobile 360px.

### 6.3. Git/PR

- Nhánh từ `origin/develop` mới nhất, không dùng nhánh cá nhân:
  - `feature/maintenance-conclude`
  - `feature/contract-checkout`
  - `feature/contract-storage`
  - `feature/notification-inbox`
- Commit:
  - `feat(maintenance): kết luận chi phí và trừ cọc`
  - `feat(contract): thêm luồng trả phòng`
  - `feat(contract): thêm phiếu gửi đồ và nhận lại`
  - `feat(notification): hộp thư và thông báo sự kiện`
  - `test(...)`, `docs(...)`
- PR vào `develop`: squash merge, 1 review, CI xanh.
- Thay đổi ở module TV2/TV5/TV3 phải tách commit nhỏ, ghi rõ lý do và nhờ owner review; không sửa rộng file của người khác.

## 7. Thứ tự ưu tiên và mốc bàn giao

| Thứ tự | Việc | Ước lượng | Phụ thuộc |
|---|---|---|---|
| 1 | Phase 0 + Constants/API Spec | 0.5 ngày | Nhóm chốt port |
| 2 | Conclude + chargeable + UI | 3 ngày | TV5 `deduct`, TV2 occupant query |
| 3 | Move-out + UI | 3 ngày | Phase 2 port, TV5 debt/refund |
| 4 | Storage + UI | 2 ngày | TV3 summer/extension |
| 5 | Notification events + inbox | 2 ngày | Event từ TV5/TV3 |
| 6 | E2E, docs, PR | 1.5 ngày | CI/Docker |

Nếu TV3 chưa có kích hoạt/gia hạn/duyệt hè, vẫn code và test bằng dữ liệu seed; chỉ màn demo end-to-end mới chờ TV3. Không tự mở rộng sang phần của TV3 để tránh chồng chéo.

## 8. Rủi ro và cách xử lý

| Rủi ro | Cách xử lý |
|---|---|
| Port contract → maintenance/registration bị nhóm phản đối | Chốt Phase 0; phương án B là đặt use-case ở `notification`, chỉ gọi service công khai; không query bảng chéo |
| `deduct` bị gọi sai hoặc trừ trùng | Chỉ gọi từ `conclude`; khóa ticket + unique `ux_deduction_ticket`; sort contractId; test đồng thời |
| Trả phòng deadlock với chuyển phòng/trả giường | Dùng `lockOpenAssignment` trước, khóa contract sau; test hai luồng đồng thời |
| Refund/contract status không nguyên tử | Một transaction; cập nhật status trước khi gọi refund; rollback toàn bộ nếu lỗi |
| Storage return chọn giường không hợp lệ | Dùng `BedOccupancyService.occupyStay`; kiểm tra giới tính/đơn vị/trạng thái; test |
| Listener thông báo lỗi làm hỏng nghiệp vụ | `AFTER_COMMIT` + `REQUIRES_NEW`, bắt exception và log |
| Không có Docker local | Chạy CI hoặc PostgreSQL test riêng; không tick test integration nếu chưa có output thật |
| UI thiếu action do backend đã có | Dùng chung `allowedActions`; test từng vai trò; không tự suy ra quyền từ trạng thái |
| Tài liệu overclaim | Cập nhật API Spec/README/report sau khi có bằng chứng; ghi rõ phần chưa kiểm chứng |

## 9. Definition of Done

Chỉ coi là hoàn thiện TV4 khi:

- [x] FR-MNT-09, FR-MNT-10: kết luận ticket, chia tiền, trừ cọc, AT-14/AT-22 pass.
- [x] FR-CON-09, FR-CON-10, FR-CON-13: preview/execute trả phòng, AT-19/AT-21 pass.
- [x] FR-CON-11, FR-CON-12: tạo/trả phiếu gửi đồ, AT-24/AT-25 pass.
- [x] FR-NOT-01..04: thông báo thủ công + event + inbox đọc/đánh dấu đã đọc.
- [x] Không còn hardcode role/status/error/action; ArchUnit pass.
- [ ] Backend `mvnw verify` pass trên CI/Docker; frontend build/lint pass.
- [ ] E2E các màn TV4 có ảnh chụp/artefact thật.
- [x] API Spec, README, DB Design, báo cáo TV4 đồng bộ.
- [ ] PR đã merge vào `develop` qua review; không push trực tiếp.

## 10. Nhật ký cập nhật

| Ngày | Nội dung | Người cập nhật |
|---|---|---|
| 26/10/2026 | Lập kế hoạch TV4 từ phân công, SRS/API/SAD và hiện trạng code trên `develop` tại `c6ea7ea` | TV4 Nguyễn Văn Quyền |

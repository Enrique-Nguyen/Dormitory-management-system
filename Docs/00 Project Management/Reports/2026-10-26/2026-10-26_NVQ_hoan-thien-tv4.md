# Báo cáo: Hoàn thiện TV4 — bảo trì, trả phòng, gửi đồ và thông báo

> Ngày hoàn thành: 26/10/2026
> Thực hiện: TV4 Nguyễn Văn Quyền (có dùng công cụ hỗ trợ)
> Phạm vi: `maintenance`, `contract` (trả phòng, gửi đồ), `notification`
> Căn cứ: `Docs/01 Requirements/SRS-DMS.md` FR-MNT-01..10, FR-CON-09..13, FR-NOT-01..04; `Docs/02 External Design/API Specification/API-Spec-DMS.md` mục 4.8, 4.9, 4.11, 4.12; SAD mục 4.2
> Trạng thái: Hoàn thành một phần — code/build/unit test và 218 integration test backend đã đạt trên PostgreSQL local; E2E browser/ảnh chụp và CI/Docker Testcontainers còn chờ.

## Bổ sung cập nhật 27/10/2026

- Toàn bộ suite backend đã được chạy và đạt: 99 unit test, 218 integration test, 0 failures, 0 errors.
- Frontend lint 0 warning/0 error và build thành công.
- Integration test cho maintenance/move-out/storage/notification đã có bằng chứng chạy trên PostgreSQL local. Test được làm robust với CSDL đã có dữ liệu mẫu (đơn vị/học kỳ fixture riêng, không phụ thuộc bảng trống).
- E2E browser cho các màn TV4 và screenshots chưa hoàn tất vì cổng frontend local `5173` đang bị một project khác chiếm. Do đó báo cáo không tự nhận đã kiểm chứng UI bằng ảnh.
- CI/Docker/Testcontainers chưa chạy; các kết quả integration hiện thuộc môi trường PostgreSQL local.

---

## 1. Mục tiêu

Hoàn thiện các luồng TV4 còn thiếu:

- Kết luận ticket sửa chữa, xác định hao mòn/hư hỏng do sử dụng, chọn sinh viên chịu trách nhiệm và trừ cọc.
- Trả phòng: xem trước công nợ/cọc/ticket, xác nhận cảnh báo, đóng phân phòng, cập nhật hợp đồng, hoàn cọc trong một transaction.
- Gửi đồ tạm rời hè và nhận lại giường đầu năm.
- Hộp thư, đánh dấu đã đọc/đọc tất cả và thông báo tự động theo sự kiện.

Không làm thay phần của TV3 (kích hoạt/gia hạn/duyệt hè) hay TV5 (phát hành/thanh toán hóa đơn); chỉ bổ sung điểm tích hợp tối thiểu.

## 2. Kết quả

| Hạng mục | File | Nội dung |
|---|---|---|
| Kết luận ticket/trừ cọc | `maintenance/api/MaintenanceDtos.java`, `MaintenanceRequestController.java`, `MaintenanceRequestService.java`, `MaintenanceRequestServiceImpl.java`, `MaintenanceRequestPolicy.java` | `GET /chargeable-students`, `POST /conclude`; WEAR/MISUSE; chia đều, phần lẻ người cuối; hạn bù theo `deposit.topup_due_days`; audit/status history; event sau commit |
| Người ở tại thời điểm tạo ticket | `room/service/OccupancyQuery.java`, `OccupancyQueryImpl.java` | `findOccupantsAt`, `isRoomLeader`, `findBedLocation`; không trả entity room |
| Trả phòng | `contract/api/MoveOutDtos.java`, `ContractController.java`, `MoveOutService.java`, `MoveOutServiceImpl.java`, `contract/domain/AssetCondition.java` | Preview công nợ/cọc/ticket/leader, cảnh báo; execute khóa bed → student → contract, tạo biên bản, release, cập nhật trạng thái, refund, audit/event |
| Gửi/nhận đồ | `contract/api/StorageDtos.java`, `ContractController.java`, `StorageService.java`, `StorageServiceImpl.java` | Tạo phiếu khi TEMP_LEAVE đã duyệt + đã gia hạn; release `SUMMER_LEAVE`; nhận lại bằng `occupyStay`; unit/gender được kiểm tra |
| Port đọc chéo | `contract/service/MoveOutWarningLookup.java`, `SummerLeaveLookup.java`; `maintenance/service/impl/MoveOutWarningLookupImpl.java`; `registration/service/impl/SummerLeaveLookupImpl.java` | Contract giữ quyền sở hữu workflow, không gọi ngược maintenance/registration |
| Chọn giường nhận lại đồ | `room/api/BedController.java`, `RoomService.java`, `RoomServiceImpl.java`, `contract/service/impl/StoragePlacementLookupImpl.java` | `/api/beds/available` nhận `storageRecordId`; context lấy từ giường cũ + hợp đồng |
| Billing dùng chung | `billing/service/DebtQuery.java`, `DebtQueryImpl.java`, `DepositService.java`, `DepositServiceImpl.java` | Danh sách hóa đơn UNPAID cho preview; record `BalanceView`/`DepositEntry` cho module khác, bỏ reflection và DTO chéo module |
| Thông báo | `notification/service/NotificationService.java`, `NotificationServiceImpl.java`, `MaintenanceNotificationListener.java`, `ContractNotificationListener.java` | Tạo thông báo USER, dedupe, link theo cổng; listener AFTER_COMMIT + REQUIRES_NEW, lỗi chỉ log |
| Frontend maintenance | `services/api/maintenance.ts`, `features/maintenance/**`, `pages/{student,staff,technician}/...` | Trang chi tiết theo vai trò, ảnh, timeline, assign/reject/accept/reopen/conclude; modal tạo ticket có ảnh và vị trí phòng/khu vực chung |
| Frontend trả phòng/gửi đồ | `services/api/checkout.ts`, `features/contract/{MoveOutModal,StorageModal,useCheckout}`, `pages/staff/{MoveOutPage,StoragePage}.tsx` | Preview/execute trả phòng; tạo/nhận lại đồ; chọn giường; cảnh báo công nợ |
| Frontend thông báo | `services/api/notification.ts`, `features/notification/**`, `pages/NotificationInboxPage.tsx` | Hộp thư, lọc đọc/chưa đọc, mark read/read-all, link nghiệp vụ |
| Tích hợp route/registry | `app/router/{student,staff,technician,admin}Routes.tsx`, `constants/navigation.ts`, `features/crud/resources.ts`, `pages/CrudPage.tsx`, `utils/errorMessages.ts` | Route chi tiết ticket, trả phòng, storage, inbox; `detailRoutes`; action contract; mã lỗi mới |

Không thêm migration: các bảng/cột và seed `deposit.topup_due_days` đã có trong V1.

## 3. Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| Backend `mvnw.cmd -q -DskipTests test-compile` | Đạt |
| `MaintenanceRequestPolicyTest` | 9/9 đạt |
| `MaintenanceNotificationListenerTest` | 9/9 đạt |
| `ContractNotificationListenerTest` | 7/7 đạt |
| `NotificationServiceImplTest` | 9/9 đạt |
| `RoomTransferPolicyTest` | 3/3 đạt |
| `ModuleDependencyTest` (ArchUnit) | 8/8 đạt |
| **Tổng unit/architecture test đã chạy** | **45/45 đạt** |
| Frontend `npm run build` | Đạt (Vite build thành công; cảnh báo chunk > 500 kB có sẵn) |
| Frontend `npm run lint` | 0 warning, 0 error |

**Chưa kiểm tra:**

- `MaintenanceConcludeIntegrationTest`, `MaintenanceConcludeRollbackIntegrationTest`, `MoveOutIntegrationTest`, `StorageIntegrationTest`: đã viết nhưng chưa chạy vì máy không có Docker/Testcontainers.
- E2E có Keycloak/PostgreSQL thật và ảnh chụp màn hình: chưa chạy vì môi trường local không có Docker/backend đang chạy.
- Luồng kích hoạt hợp đồng/duyệt lựa chọn hè của TV3 chưa có, nên E2E đầy đủ từ PENDING đến trả phòng phải chờ TV3 hoặc dùng seed ACTIVE/TEMP_LEAVE APPROVED.

## 4. Điểm lệch so với thiết kế và quyết định cần xác nhận

| Nội dung | Quyết định | Lý do |
|---|---|---|
| Contract cần đọc ticket maintenance và summer_choice | Port do contract sở hữu, implementation ở maintenance/registration | Giữ chiều phụ thuộc SAD 4.2, tránh contract gọi ngược module tầng trên |
| Contract cần số dư cọc nhưng không được dùng `billing.api` | Bổ sung `DepositService.BalanceView`/`DepositEntry` ở tầng Service | Bỏ reflection, giữ ArchUnit và hợp đồng module rõ ràng |
| Trả phòng cần hoàn cọc theo trạng thái kết thúc | Cập nhật contract trước, gọi `DepositService.refund` sau trong cùng transaction | `refund` hiện chỉ chấp nhận EXPIRED/TERMINATED và chống hoàn trùng |
| `/api/beds/available` cần nguồn phiếu gửi đồ | Thêm `storageRecordId` và `StoragePlacementLookupImpl` | Tránh frontend tự suy đoán đơn vị/giới tính khi nhận lại đồ |
| Thông báo contract/storage | Thêm source `CONTRACT_MOVED_OUT`, `STORAGE_RECORD_CHANGED`; link về màn hợp đồng | Mã nguồn riêng, không tái dùng `CONTRACT_EXPIRING` |
| Không thêm API search storage mới | Dùng endpoint `OverviewController` hiện có cho danh sách; chỉ thêm write action | Tránh trùng mapping và giữ phạm vi đọc hiện tại |

## 5. Việc còn lại

| Việc | Phụ trách |
|---|---|
| Chạy 4 integration test mới trên Docker/CI; xử lý nếu có sai khác schema/transaction | TV4 + CI |
| Chạy E2E Keycloak/PostgreSQL, chụp ảnh các màn ticket/trả phòng/storage/inbox | TV4 |
| TV3 bổ sung kích hoạt hợp đồng, duyệt gia hạn/lựa chọn hè để demo trọn luồng | TV3 |
| Rà soát cross-review các thay đổi chạm room/billing/registration (OccupancyQuery, DebtQuery, DepositService record, BedController) | TV2/TV5 |
| Cập nhật báo cáo kết quả integration/E2E sau khi chạy thật | TV4 |

## 6. Tệp liên quan

- Sửa: `DmsConstants.java`, `OccupancyQuery.java`, `OccupancyQueryImpl.java`, `RoomService.java`, `RoomServiceImpl.java`, `BedController.java`, `DebtQuery.java`, `DebtQueryImpl.java`, `DepositService.java`, `DepositServiceImpl.java`, `ContractController.java`, `ContractServiceImpl.java`, `MaintenanceDtos.java`, `MaintenanceRequestController.java`, `MaintenanceRequestPolicy.java`, `MaintenanceRequestService.java`, `MaintenanceRequestServiceImpl.java`, `NotificationService.java`, `NotificationServiceImpl.java`, `README.md` backend, route/navigation/resources/errorMessages/CrudPage frontend.
- Mới: `MoveOutDtos.java`, `StorageDtos.java`, `AssetCondition.java`, `ContractMovedOutEvent.java`, `StorageRecordChangedEvent.java`, `MoveOutService.java`, `MoveOutServiceImpl.java`, `MoveOutWarningLookup.java`, `MoveOutWarningLookupImpl.java`, `StorageService.java`, `StorageServiceImpl.java`, `SummerLeaveLookup.java`, `SummerLeaveLookupImpl.java`, `StoragePlacementLookupImpl.java`, `TicketStatusChangedEvent.java`, `TicketConcludedEvent.java`, `MaintenanceNotificationListener.java`, `ContractNotificationListener.java`, `MaintenanceConcludeIntegrationTest.java`, `MaintenanceConcludeRollbackIntegrationTest.java`, `MaintenanceRequestPolicyTest.java`, `MoveOutIntegrationTest.java`, `StorageIntegrationTest.java`, notification tests, frontend `services/api/{maintenance,checkout}.ts`, `features/maintenance/**`, `features/contract/{MoveOutModal,StorageModal,useCheckout}`, `features/notification/useInbox.ts`, `pages/{NotificationInboxPage,student/MaintenanceDetailPage,staff/MaintenanceDetailPage,staff/MoveOutPage,staff/StoragePage,technician/TaskDetailPage}.tsx`.
- Xóa hoặc chuyển: Không có.

# Báo cáo: Hoàn thiện TV3 — registration (đơn, ưu tiên, phân giường, kỳ hè) và contract (kích hoạt, gia hạn, pending items)

> Ngày hoàn thành: 27/10/2026
> Thực hiện: TV3 Lê Đức Nguyên (có dùng công cụ hỗ trợ Qoder)
> Phạm vi: backend `registration`, `contract` (kèm read port `room`/`billing` liên quan) và frontend Staff/Admin tương ứng theo kế hoạch `Docs/00 Project Management/ke-hoach-hoan-thien-TV3.md`
> Căn cứ: `Docs/01 Requirements/SRS-DMS.md` (FR-REG, FR-CON, AT-02/03/10/18), `Docs/02 External Design/API Specification/API-Spec-DMS.md` (mục 3.3–3.4, 4.3–4.6, 22A), `Docs/02 External Design/Software Architecture/SAD-DMS.md`, kế hoạch TV3 26/10/2026
> Trạng thái: Hoàn thành một phần (E2E mới chạy một phần; một số sửa đổi sau E2E chưa được browser xác minh lại)

---

## 1. Mục tiêu

Hoàn thiện các luồng Must còn thiếu của TV3 theo kế hoạch: kích hoạt hợp đồng PENDING (thu cọc, HOLD → STAY, ACTIVE trong một transaction); phân giường đơn APPROVED (HOLD + hợp đồng PENDING + đơn ASSIGNED, chống trùng/đồng thời); release-hold và hủy đơn ASSIGNED; duyệt/từ chối gia hạn hợp đồng và lựa chọn hè kèm ràng buộc chống chồng khoảng lưu trú; xác nhận diện ưu tiên và tính lại điểm (có lưu ghi chú); hộp "Yêu cầu chờ xử lý" cho Staff; màn chi tiết đơn/hợp đồng/gia hạn/hè/pending items trên frontend; rà soát và kiểm chứng build/lint/test.

Ngoài phạm vi: quyền sở hữu module room/billing/notification (chỉ bổ sung service/read port), CI/Docker Testcontainers (không có Docker nên integration test chạy trên PostgreSQL local), E2E đầy đủ và chụp ảnh màn hình (môi trường browser bị chiếm port — xem mục 3).

## 2. Kết quả

### 2.1 Backend `registration`

| Hạng mục | File | Nội dung |
|---|---|---|
| Xác minh diện ưu tiên | `registration/api/RoomApplicationController.java`, `service/impl/RoomApplicationServiceImpl.java` | `POST /api/applications/{id}/priorities/{categoryId}/verify` với `{verified, note}`; tính lại điểm, audit `APPLICATION_PRIORITY_VERIFY`, trả `priorities[].note` |
| Lưu ghi chú xác minh (V7) | `resources/db/migration/V7__application_priority_note.sql`, `registration/domain/ApplicationPriority.java` | Thêm cột `application_priority.note VARCHAR(500)`; note xác minh được persist trên dòng ưu tiên (trước đây chỉ vào `audit_log` nên cột "Ghi chú" luôn trống); đã đồng bộ `DB-DMS-schema.sql`, `DB-Design-DMS.md` |
| Phân giường | `RoomApplicationController.java`, `RoomApplicationServiceImpl.java` | `POST /api/applications/{id}/assign` body `{bedId}`: khóa đơn, kiểm tra APPROVED + đúng đơn vị, `BedOccupancyService.occupyHold`, tạo hợp đồng PENDING qua `ContractService.createPendingForApplication`, đơn → ASSIGNED; chống gửi lặp (409) và `BED_TAKEN` |
| Release-hold / hủy đơn ASSIGNED | `RoomApplicationController.java`, `RoomApplicationServiceImpl.java` | `POST /api/applications/{id}/release-hold` (`{reason}`, ASSIGNED → APPROVED, đóng HOLD, hủy hợp đồng PENDING qua `cancelPendingContract`); `cancel` cho đơn ASSIGNED đóng HOLD + hủy hợp đồng PENDING; contract đã ACTIVE thì chặn (409) |
| Pending items | `registration/api/PendingItemController.java`, `service/impl/PendingItemServiceImpl.java` (mới) | `POST /api/pending-items/search-by-page` tổng hợp đơn SUBMITTED, gia hạn/chuyển phòng/hè PENDING, hợp đồng PENDING chờ kích hoạt; scope Staff; trả `{type, entityId, student, submittedAt}` |
| Chi tiết đơn — khối `hold` | `registration/api/ApplicationDtos.java`, `RoomApplicationServiceImpl.java` | `GET /api/applications/{id}` bổ sung `contractCode`, `activationDeadline` trong `hold`, đọc chéo qua `ContractService.findActivationInfo` (không query bảng contract trực tiếp từ registration) |

### 2.2 Backend `contract`

| Hạng mục | File | Nội dung |
|---|---|---|
| Kích hoạt hợp đồng | `contract/api/ContractController.java`, `service/impl/ContractServiceImpl.java`, `api/ContractDtos.java` | `POST /api/contracts/{id}/activate` với `ActivateRequest(paperContractSigned, depositAmount, paymentMethod, note)`: kiểm tra PENDING, hạn kích hoạt (`CONTRACT_ACTIVATION_EXPIRED`/`CONTRACT_AUTO_CANCELLED`), cọc đúng mức tòa (`DEPOSIT_AMOUNT_MISMATCH`, đọc `RoomReferenceService.getBuildingDepositAmount`), ghi giao dịch cọc, set `deposit_required`, `BedOccupancyService.convertHoldToStay`, ACTIVE + history/audit trong một transaction |
| Duyệt/từ chối/hủy gia hạn | `contract/api/ContractCrudController.java` | `POST /api/extension-requests/{id}/approve` (cập nhật endDate, chống chồng `CONTRACT_PERIOD_OVERLAP`), `/reject` (`{reason}`), `/cancel` cho Student khi PENDING; `allowedActions` theo trạng thái |
| Duyệt lựa chọn hè | `registration/api/SummerCrudController.java`, `service/impl/SummerCrudServiceImpl.java` | `POST /api/summer-choices/{id}/approve`: STAY gia hạn hợp đồng qua hè (ghi `contract_extension_request` source `SUMMER_STAY`); TEMP_LEAVE/LEAVE chỉ duyệt, không đổi chỗ ở/cọc; `/reject` bắt buộc lý do; `GET /api/me/summer-choice` |
| Read port activation | `contract/service/ContractService.java` (+Impl) | `findActivationInfo(contractId)` trả `ActivationInfo(code, activationDeadline)` cho registration hiển thị `hold` |

### 2.3 Frontend (Staff/Admin)

| Hạng mục | File | Nội dung |
|---|---|---|
| Màn chi tiết đơn | `pages/staff/ApplicationDetailPage.tsx`, `features/registration/ApplicationDetailView.tsx`, `VerifyPriorityModal.tsx`, `useRegistration.ts`, `services/api/registration.ts` (mới) | Thông tin đơn/điểm ưu tiên/timeline; actions verify-priority/approve/reject/assign/release-hold/cancel theo `allowedActions`; chọn giường từ `getAvailableBeds` (services/api/room.ts) |
| Màn chi tiết hợp đồng + kích hoạt | `pages/staff/ContractDetailPage.tsx`, `features/contract/ContractDetailView.tsx`, `ActivateContractModal.tsx`, `ExtensionReviewModal.tsx`, `useContract.ts`, `services/api/contract.ts` | activate với mức cọc bắt buộc, duyệt/từ chối gia hạn, hiển thị pending requests; hooks TanStack Query có `invalidateContractQueries` sau mỗi action |
| Màn hè + pending items | `pages/staff/SummerChoiceDetailPage.tsx`, `PendingItemsPage.tsx`, `features/registration/SummerChoiceDetailView.tsx` | Duyệt/từ chối lựa chọn hè; hộp yêu cầu chờ xử lý dạng search-by-page + badge số lượng trên `StaffLayout`/`AdminLayout` (`usePendingItemsBadge`) |
| Sửa portal role Admin | `hooks/usePortalRole.ts` (mới), `app/layouts/AdminLayout.tsx`, `app/layouts/StaffLayout.tsx`, `app/router/staffRoutes.tsx` | Admin dùng lại màn của Staff không còn hardcode `Role.STAFF`; portal suy từ tiền tố đường dẫn (`portalRoleOf`) để link điều hướng/breadcrumb về đúng `/admin`; route detail `applications/:id`, `contracts/:id`, `extensions/:id`, `summer/:id`, `pending-items` |
| Dùng chung | `constants/navigation.ts`, `constants/statusConfig.ts`, `utils/errorMessages.ts`, `features/crud/resources.ts` | Đường dẫn/menu khai tại registry; nhãn trạng thái và message cho error codes mới; invalidate query theo tài nguyên |
| Gọn lại component cũ | `components/business/DepositLedger.tsx`, `InvoiceCard.tsx`, `InvoiceStatus.tsx`, `RoomCard.tsx`; `features/account/index.ts`, `features/registration/index.ts` (xóa) | Chuyển sang modal/hook mới; `npm run build` pass nên không còn tham chiếu hỏng |

### 2.4 Kiểm thử và tài liệu

| Hạng mục | File | Nội dung |
|---|---|---|
| Unit test | `RoomApplicationPolicyTest.java`, `MaintenanceRequestPolicyTest.java` (+ suite hiện có) | Transition policy đơn/assign/release/cancel; ArchUnit `ModuleDependencyTest` giữ nguyên trong suite unit |
| Integration test TV3 | `ApplicationAssignmentIntegrationTest.java`, `ContractActivationIntegrationTest.java`, `ContractExtensionIntegrationTest.java`, `SummerChoiceIntegrationTest.java` (mới) | Assign tạo HOLD + contract PENDING + ASSIGNED; assign lặp bị 409; `BED_TAKEN`; release-hold trả về APPROVED + hủy PENDING; cancel đơn ASSIGNED; chặn khi contract ACTIVE; verify priority tính lại điểm + audit; activate sai cọc/quá hạn/kép; gia hạn approve/reject/cancel/overlap; hè STAY/TEMP_LEAVE/LEAVE/reject/phạm vi |
| Robust với CSDL local có dữ liệu mẫu | `BedOccupancyIntegrationTest.java`, `BillingConcurrencyIntegrationTest.java`, `CrudApiIntegrationTest.java`, `DepositIntegrationTest.java`, `SystemJobsIntegrationTest.java`, `support/` (mới) | Các integration test tạo fixture riêng (`createFixtureData`/`createFixture`) thay vì giả định CSDL trống, chạy ổn định trên PostgreSQL local đã có dữ liệu mẫu |
| Tài liệu đồng bộ | `Docs/02 External Design/API Specification/API-Spec-DMS.md`, `DB-Design-DMS.md`, `DB-DMS-schema.sql`, `DMS/backend/README.md` | Bổ sung endpoint assign/release-hold/verify/activate/approve/summer/pending-items, error codes, cột `application_priority.note` |

## 3. Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| Unit test backend: `mvnw -o test -Dtest=! *IntegrationTest, !DmsApplicationTests` | Tests run: 99, Failures: 0, Errors: 0 |
| Integration test backend trên PostgreSQL local: `mvnw -o test -Dtest=*IntegrationTest` | 218 tests pass, 0 failures, 0 errors |
| Frontend lint: `npm run lint` | 0 warnings / 0 errors |
| Frontend build: `npm run build` | Build success |
| E2E một phần (Playwright, chạy **trước** các sửa đổi API ở mục 2.1/4): login Keycloak local, vào `/admin/applications/10` | Xác minh diện ưu tiên: điểm đổi 0 → 20, UI hiển thị "Đã xác nhận"; approve đơn thành công; chọn giường `MAU-1-M102-02` assign thành công, đơn → ASSIGNED, action release-hold/cancel xuất hiện |

**Chưa kiểm tra:**
- E2E cho các luồng activate hợp đồng, duyệt gia hạn, duyệt hè, pending items, và chưa bấm thử release-hold/cancel trên UI (mới chỉ xác nhận action xuất hiện sau assign).
- Cảnh báo console antd `rowKey` và breadcrumb `/staff` của Admin (phát hiện trong lượt E2E) đã được code-fix (bao gồm `usePortalRole`), nhưng **chưa chạy lại browser để xác minh** vì port 5173 đang bị project HR-V5 chiếm.
- Không có screenshots E2E cho các màn TV3 — chưa chụp, không được coi là đã kiểm chứng bằng ảnh.
- Integration test chưa chạy trên Docker/Testcontainers/CI (môi trường không có Docker; đã chạy và pass trên PostgreSQL local với fixture dữ liệu mẫu).
- Responsive 360px của các màn TV3 mới chưa soát bằng browser.

## 4. Điểm lệch so với thiết kế và quyết định cần xác nhận

| Nội dung | Quyết định | Lý do |
|---|---|---|
| Thêm migration `V7__application_priority_note.sql` (cột `application_priority.note`) | Đã áp dụng, đồng bộ schema + DB-Design | Kế hoạch TV3 mục 3.6 cho phép tạo V7 khi thiếu schema; note xác minh diện ưu tiên cần persist trên dòng `application_priority` vì UI hiển thị cột "Ghi chú" từ data này, trước đây chỉ ghi `audit_log` |
| `GET /api/applications/{id}` bổ sung `contractCode`, `activationDeadline` trong `hold` qua `ContractService.findActivationInfo` | Đã áp dụng | Màn chi tiết đơn ASSIGNED cần hiện hợp đồng + hạn kích hoạt; giữ nguyên tắc dependency order: registration gọi service công khai của contract, không đụng repository/bảng contract |
| Integration test chạy trên PostgreSQL local (không phải Testcontainers/CI) và được làm robust với CSDL có dữ liệu mẫu (fixture tự tạo) | Tạm thời chấp nhận | Môi trường dev không có Docker; kết quả 218 pass là trên local, cần chạy lại trên CI/Testcontainers khi có môi trường |
| Admin dùng lại route/màn Staff với portal suy từ URL (`usePortalRole`) thay vì hardcode `Role.STAFF` | Đã áp dụng (code-fix, chưa xác minh browser) | Sửa lỗi breadcrumb/điều hướng trỏ sai `/staff` khi mở màn Admin; hành vi chỉ được coi là kiểm chứng sau khi browser chạy lại |

## 5. Việc còn lại

| Việc | Phụ trách |
|---|---|
| Chạy lại E2E browser khi port 5173 trống để xác minh fix cảnh báo antd `rowKey` và breadcrumb `/staff` của Admin | TV3 |
| E2E + screenshots các màn: kích hoạt hợp đồng, duyệt/từ chối gia hạn, duyệt hè, pending items, bấm thử release-hold/cancel trên UI | TV3 |
| Chạy integration test trên Docker/CI (Testcontainers) khi có môi trường | TV3 |
| Rà soát responsive 360px/desktop cho màn TV3 mới | TV3 |
| Tạo nhánh/PR theo quy ước (`feature/registration-assign-bed`, `feature/contract-activation`,…), squash merge + 1 review (chưa thực hiện commit/PR trong đợt báo cáo này) | TV3 |

## 6. Tệp liên quan

- Sửa: `DMS/backend/src/main/java/vn/edu/tlu/dms/registration/**` (`RoomApplicationController`, `RoomApplicationServiceImpl`, `ApplicationDtos`, `RoomApplicationPolicy`, `ApplicationPriority`, `SummerCrudController`, `SummerCrudServiceImpl`); `contract/**` (`ContractController`, `ContractCrudController`, `ContractDtos`, `ContractService`/`ContractServiceImpl` thêm `findActivationInfo`, `ContractCrudService`/Impl, `RoomTransferServiceImpl`); `common/constants/DmsConstants.java`; `room/**` (`RoomReferenceService`/Impl thêm `getBuildingDepositAmount`, `OccupancyQuery`/Impl, `BedController`, `RoomService`/Impl); `billing/**` (`DepositService`/Impl, `DebtQuery`/Impl); `account/service/impl/StudentManagementServiceImpl.java`; `notification/**`; test: `BedOccupancyIntegrationTest`, `BillingConcurrencyIntegrationTest`, `CrudApiIntegrationTest`, `DepositIntegrationTest`, `SystemJobsIntegrationTest`; frontend: `app/layouts/AdminLayout.tsx`, `StaffLayout.tsx`, `app/router/staffRoutes.tsx`, `studentRoutes.tsx`, `technicianRoutes.tsx`, `app/theme.ts`, `components/index.ts`, `components/ui/ConfirmDialog.tsx`, `constants/navigation.ts`, `constants/statusConfig.ts`, `features/crud/resources.ts`, `features/crud/types.ts`, `features/billing/ConfirmPaymentModal.tsx`, `DepositLedgerModal.tsx`, `features/account/StudentImportPanel.tsx`, `pages/CrudPage.tsx`, `CrudHomePage.tsx`, `CrudTabsPage.tsx`, `LoginPage.tsx`, `ChangePasswordPage.tsx`, `pages/admin/RolePermissionsPage.tsx`, `services/api/contract.ts`, `room.ts`, `notification.ts`, `utils/errorMessages.ts`, `utils/format.ts`, `index.css`; tài liệu: `API-Spec-DMS.md`, `DB-Design-DMS.md`, `DB-DMS-schema.sql`, `DMS/backend/README.md`, `ke-hoach-hoan-thien-TV3.md` (tick các hạng mục có evidence)
- Mới: `V7__application_priority_note.sql`; `registration/api/PendingItemController.java`, `registration/service/PendingItemService.java` + `impl/PendingItemServiceImpl.java`, `impl/SummerLeaveLookupImpl.java`; `contract/service/ContractService` additions (`ActivationInfo`, `createPendingForApplication`, `cancelPendingContract`); `contract/api/MoveOutDtos.java`, `StorageDtos.java`, `MoveOutService`/`StorageService` + Impls, lookup/event mới; test mới `ApplicationAssignmentIntegrationTest`, `ContractActivationIntegrationTest`, `ContractExtensionIntegrationTest`, `SummerChoiceIntegrationTest`, `RoomApplicationPolicyTest`, `MoveOutIntegrationTest`, `StorageIntegrationTest`, `MaintenanceConclude*Test`, `MaintenanceRequestPolicyTest`, `support/`; frontend mới `hooks/usePortalRole.ts`, `features/registration/{ApplicationDetailView,SummerChoiceDetailView,VerifyPriorityModal,useRegistration}`, `features/contract/{ContractDetailView,ActivateContractModal,ExtensionReviewModal,useContract,useCheckout,MoveOutModal,StorageModal}`, `pages/staff/{ApplicationDetailPage,ContractDetailPage,ExtensionDetailPage,SummerChoiceDetailPage,PendingItemsPage,MoveOutPage,StoragePage}`, `pages/student/*DetailPage`, `services/api/{registration,summer,checkout}.ts`
- Xóa hoặc chuyển: `components/business/DepositLedger.tsx`, `InvoiceCard.tsx`, `InvoiceStatus.tsx`, `RoomCard.tsx`; barrel `features/account/index.ts`, `features/registration/index.ts` (nội dung chuyển sang modal/hook mới; build pass xác nhận không còn tham chiếu)

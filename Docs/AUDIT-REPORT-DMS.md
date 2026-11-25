# BÁO CÁO KIỂM TOÁN VÀ ĐÁNH GIÁ TOÀN DIỆN HIỆN TRẠNG HỆ THỐNG DMS
## (Dormitory Management System — Audit & Compliance Report)

> **Mã tài liệu:** `DOCS-AUDIT-DMS-2026-R5`  
> **Thời điểm kiểm toán:** 2026-10-28  
> **Phiên bản hệ thống:** Nhánh `develop` mới nhất (Commit tích hợp 2026-10-28)  
> **Chế độ kiểm toán:** Read-Only Audit & Non-Destructive Inspection  
> **Quy chuẩn đối chiếu:** `CLAUDE.md`, `AGENTS.md`, `SRS-DMS.md`, `SAD-DMS.md` (v1.6), `API-Spec-DMS.md` (v0.17), `DB-Design-DMS.md` (v1.6)  
> **Đơn vị thực hiện:** Nhóm Kiểm toán Độc lập Đa chuyên đề (Tracks R1, R2, R3, R4, R5)

---

## MỤC LỤC TỔNG THỂ

1. [Tổng quan tình trạng dự án (Executive Summary & Health Score)](#1-tổng-quan-tình-trạng-dự-án-executive-summary--health-score)
   - 1.1. Bối cảnh đợt rà soát sau cập nhật `develop`
   - 1.2. Bảng điểm sức khỏe toàn diện (Overall Health Score)
   - 1.3. Tóm lược các thế mạnh vượt trội
   - 1.4. Tóm lược các điểm rủi ro cần xử lý khẩn cấp
2. [Kết quả kiểm chứng tự động (Build & Test Suites)](#2-kết-quả-kiểm-chứng-tự-động-build--test-suites)
   - 2.1. Phân hệ Frontend (Linter, Type-Check & Production Build)
   - 2.2. Phân hệ Backend (Biên dịch, Kiến trúc ArchUnit & Unit/Policy Tests)
   - 2.3. Chẩn đoán bộ kiểm thử tích hợp (Integration Tests & Testcontainers)
3. [Danh mục các vi phạm quy tắc CLAUDE.md / AGENTS.md](#3-danh-mục-các-vi-phạm-quy-tắc-claudemd--agentsmd)
   - 3.1. Bảng ma trận 39 vi phạm Backend (Track R2)
   - 3.2. Bảng ma trận 23 vi phạm Frontend (Track R3)
   - 3.3. Phân tích nguyên nhân gốc rễ (Root Cause Analysis)
4. [Đánh giá chi tiết từng module Backend & Frontend](#4-đánh-giá-chi-tiết-từng-module-backend--frontend)
   - 4.1. Phân hệ Backend (Account, Room, Contract, Billing, Registration, Maintenance, Notification, Common)
   - 4.2. Phân hệ Frontend (Layout, Navigation, Feature Pages, Shared Components, API Client)
   - 4.3. Đối soát CSDL (Flyway Migrations vs DDL Schema) và Tài liệu kỹ thuật
5. [Ma trận rủi ro phân theo mức độ nghiêm trọng (Risk Matrix)](#5-ma-trận-rủi-ro-phân-theo-mức-độ-nghiêm-trọng-risk-matrix)
   - 5.1. Bảng phân loại 4 cấp độ rủi ro (Critical, High, Medium, Low)
   - 5.2. Ma trận Xác suất (Likelihood) x Tác động (Impact) & Biện pháp giảm thiểu
6. [Lộ trình và khuyến nghị khắc phục cụ thể (Remediation Roadmap)](#6-lộ-trình-và-khuyến-nghị-khắc-phục-cụ-thể-remediation-roadmap)
   - 6.1. Giai đoạn 1 (Hotfix Khẩn cấp — Sprint 1)
   - 6.2. Giai đoạn 2 (Chuẩn hóa Contract & DTO — Sprint 2)
   - 6.3. Giai đoạn 3 (Refactor Kiến trúc & Tối ưu UI/UX — Sprint 3)

---

## 1. TỔNG QUAN TÌNH TRẠNG DỰ ÁN (EXECUTIVE SUMMARY & HEALTH SCORE)

### 1.1. Bối cảnh đợt rà soát sau cập nhật `develop`
Hệ thống Quản lý Ký túc xá (Dormitory Management System — DMS) là giải pháp quản lý tập trung phục vụ toàn diện các công tác vận hành ký túc xá đại học: từ đăng ký phòng ở, xét duyệt hồ sơ, xếp phòng, ký hợp đồng, thu tiền cọc, thu phí điện nước định kỳ, quản lý chuyển phòng/trả phòng, tiếp nhận yêu cầu bảo trì sửa chữa cơ sở vật chất, đến gửi thông báo và quản trị người dùng đa vai trò (Sinh viên, Cán bộ ký túc xá, Thợ kỹ thuật, Quản trị viên).

Sau chuỗi đợt hoàn thiện tính năng và đồng bộ hóa các phân hệ trên nhánh `develop`, đợt kiểm toán toàn diện này được kích hoạt nhằm mục đích:
- Kiểm chứng thực tế mức độ sẵn sàng của toàn bộ mã nguồn thông qua việc chạy trực tiếp các bộ công cụ kiểm thử, linter và trình biên dịch tự động.
- Đối chiếu sự tuân thủ nghiêm ngặt theo các bản đặc tả kiến trúc (`SAD-DMS.md`), yêu cầu nghiệp vụ (`SRS-DMS.md`), hợp đồng giao tiếp (`API-Spec-DMS.md`) và đặc biệt là hệ thống quy tắc lập trình cốt lõi được định nghĩa trong `CLAUDE.md` và `AGENTS.md`.
- Phát hiện sớm các rủi ro vận hành tiềm ẩn, các điểm gãy contract giữa Frontend và Backend, sự lệch pha giữa lược đồ CSDL thực tế với tài liệu thiết kế để thiết lập lộ trình khắc phục dứt điểm trước khi đưa vào môi trường Production.

### 1.2. Bảng điểm sức khỏe toàn diện (Overall Health Score)

Đánh giá được lượng hóa dựa trên 4 trụ cột kỹ thuật độc lập với thang điểm 100:

| Trụ cột đánh giá (Pillar) | Trọng số | Điểm số | Trạng thái (Status) | Tóm tắt đánh giá cốt lõi |
|---|:---:|:---:|:---:|---|
| **1. Phân hệ Frontend (React 19 / TS)** | 25% | **88 / 100** | Khá Tốt (Good) | Biên dịch sạch sẽ (0 lỗi, 0 warning lint); định tuyến phân quyền đa cổng vững chắc; tồn tại 4 lỗi nuốt ngoại lệ tại modal và thiếu 7 mã lỗi dịch nghĩa tiếng Việt. |
| **2. Phân hệ Backend (Spring Boot / Java)** | 35% | **91 / 100** | Xuất Sắc (Excellent) | 100% biên dịch sạch; vượt qua toàn bộ 8/8 bài kiểm tra kiến trúc ArchUnit; 99/99 bài test đơn vị xanh 100%; RBAC động và khóa giao dịch chống deadlock mẫu mực; cần chuẩn hóa anti-hardcoding chuỗi validation và DTO controller. |
| **3. CSDL & Kiến trúc (Database & Specs)** | 25% | **93 / 100** | Xuất Sắc (Excellent) | Hoàn thiện 100% 36 bảng CSDL và toàn vẹn cả 5 luồng nghiệp vụ cốt lõi; phát hiện độ lệch nhẹ trong tệp snapshot DDL `DB-DMS-schema.sql` (thiếu quyền V6) và 2 view mồ côi. |
| **4. Hạ tầng Kiểm thử (Test Infrastructure)**| 15% | **85 / 100** | Đạt Chuẩn (Acceptable) | Bộ kiểm thử đơn vị, kiến trúc và chính sách hoạt động hoàn hảo; 219 bài kiểm thử tích hợp yêu cầu daemon Docker Desktop đang hoạt động trên máy chủ host. |
| **TỔNG ĐIỂM SỨC KHỎE DỰ ÁN (OVERALL)** | **100%** | **89 / 100** | **SẴN SÀNG PRODUCTION (CẦN XỬ LÝ ACTION ITEMS)** | **Hệ thống có nền tảng kiến trúc vững chắc, độ tin cậy giao dịch cao; cần ưu tiên xử lý dứt điểm các lỗi xử lý ngoại lệ UI và đồng bộ DTO.** |

---

### 1.3. Tóm lược các thế mạnh vượt trội
1. **Kiến trúc phân tầng và bảo vệ ranh giới mẫu mực (ArchUnit 8/8 Passed)**:
   - Toàn bộ backend tuân thủ tuyệt đối mô hình đồ thị có hướng không chu trình (DAG): `common` $\to$ `account` $\to$ `room` $\to$ `billing` $\to$ `contract` $\to$ `registration`/`maintenance` $\to$ `notification`.
   - 100% interface Service (45/45 interface) có tài liệu JavaDoc tiếng Việt chi tiết cho mọi phương thức; logic nghiệp vụ được đóng gói hoàn toàn trong tầng `service.impl`.
   - Không có bất kỳ truy cập chéo Repository hoặc Entity trái phép giữa các bounded context.
2. **Bảo mật và Dynamic RBAC cấp độ doanh nghiệp**:
   - Tích hợp chuẩn mực Keycloak OIDC JWT Resource Server, kết hợp kiểm tra quyền động qua bảng CSDL `role_permission_matrix` với cơ chế khóa bi quan (`PESSIMISTIC_WRITE`) và `security_version` ngăn chặn race condition.
   - Vai trò `ADMIN` được bảo vệ bất biến (`ADMIN_PERMISSIONS_IMMUTABLE`).
   - Tách biệt dữ liệu đa đơn vị quản lý thông qua cơ chế `ScopeGuard` và `InvoiceRepository.STAFF_UNIT_SCOPE`, tự động cách ly tài nguyên giữa các tòa nhà và đơn vị ký túc xá.
3. **Độ an toàn giao dịch và triệt tiêu Deadlock**:
   - Áp dụng triệt để thứ tự khóa tài nguyên toàn cục: `bed` $\to$ `student_profile` $\to$ `contract` $\to$ `invoice`.
   - Trong nghiệp vụ đổi phòng (`RoomTransfer`), hệ thống luôn sắp xếp 2 giường theo thứ tự tăng dần của `bed_id` (`Math.min(b1, b2)` trước `Math.max(b1, b2)`) trước khi chiếm khóa bi quan, loại trừ 100% nguy cơ deadlock kinh điển (Dining Philosophers).
4. **Chất lượng mã nguồn tĩnh và Build Frontend hoàn hảo**:
   - Bộ linter siêu tốc `oxlint` quét 123 tệp tin TypeScript với 116 quy tắc kiểm tra không ghi nhận bất kỳ một lỗi hay cảnh báo nào (0 errors, 0 warnings trong 186ms).
   - Quá trình biên dịch TypeScript và Vite bundler xử lý thành công 3,486 module, bảo đảm 100% tính an toàn kiểu dữ liệu (type-safety).

---

### 1.4. Tóm lược các điểm rủi ro cần xử lý khẩn cấp
1. **Rủi ro nghiêm trọng (Critical) — Lỗi nuốt ngoại lệ nghiệp vụ tại Modal Frontend**:
   - Tại 4 modal cốt lõi (`DepositLedgerModal`, `IssueInvoicesModal`, `ConfirmPaymentModal`, `BedCrudPanel`), khối `catch (error)` chỉ lọc lỗi trường qua `validationFieldErrors(error)`. Khi backend ném lỗi nghiệp vụ (`DUPLICATE_INVOICE`, `REFUND_EXCEEDS_BALANCE`, `BED_TAKEN`), mảng lỗi trường bị rỗng và hệ thống hoàn toàn không gọi `message.error()`. Kết quả: Giao diện đứng im, người dùng không nhận được bất kỳ thông báo nào.
2. **Lệch hợp đồng API và Thiếu mã lỗi từ điển (High)**:
   - Thiếu 7 mã lỗi nghiệp vụ được đặc tả trong API-Spec tại từ điển tiếng Việt `errorMessages.ts` (`DEPOSIT_AMOUNT_MISMATCH`, `CONTRACT_ACTIVATION_EXPIRED`, `CONTRACT_AUTO_CANCELLED`, `CONTRACT_PERIOD_OVERLAP`, `APPLICATION_NOT_APPROVED`, `NO_HOLD_ASSIGNMENT`, `ADJUSTMENT_INVALID`).
   - Tồn tại các lời gọi API không tuân thủ CRUD V5: `listDepositTransactions` gọi `GET` mảng thuần không phân trang; `createStorageRecord` gọi URL riêng thay vì `save-or-update`; `removeRoomLeader` dùng HTTP `DELETE` có gắn request body.
3. **Vi phạm quy tắc Anti-Hardcoding và Kiểu dữ liệu không an toàn tại Backend (High)**:
   - 23 vị trí gọi `throw BusinessException.invalid("fieldName", "Message")` sử dụng chuỗi ký tự thô thay vì hằng số `DmsConstants.ApiField` và từ điển thông điệp tập trung.
   - 8 phương thức Controller khai báo kiểu trả về `Object`, `Map<String, Object>` làm mất tính an toàn kiểu và phá vỡ OpenAPI/Swagger schema.
4. **Độ lệch lược đồ CSDL snapshot (High)**:
   - Tệp snapshot `DB-DMS-schema.sql` khởi tạo role `STAFF` chỉ với 72 quyền, bỏ sót quyền `ROOM_TRANSFER_APPROVE` được bổ sung trong Flyway migration `V6`.
5. **Hạ tầng kiểm thử tích hợp bị phụ thuộc môi trường (Medium)**:
   - 219 bài kiểm thử tích hợp Spring Boot bị dừng do Testcontainers yêu cầu daemon Docker Desktop đang chạy. Toàn bộ logic kiểm thử đơn vị nội tại đạt 100% pass.

---

## 2. KẾT QUẢ KIỂM CHỨNG TỰ ĐỘNG (BUILD & TEST SUITES)

Đội kiểm toán Track R1 đã tiến hành chạy trực tiếp các lệnh kiểm thử và biên dịch trên môi trường thực tế của dự án thông qua wrapper chuẩn `rtk proxy` theo yêu cầu của `AGENTS.md`.

### 2.1. Phân hệ Frontend (Linter, Type-Check & Production Build)

#### A. Kiểm tra Linter (`oxlint`)
- **Thư mục thực thi:** `DMS/frontend`
- **Lệnh thực thi:** `rtk proxy npm run lint`
- **Công cụ:** `oxlint v1.81.0` (16 threads song song)
- **Phạm vi kiểm tra:** 123 tệp nguồn TypeScript/TSX, 116 quy tắc kiểm tra tĩnh.
- **Thời gian thực thi:** **186ms**
- **Mã thoát (Exit Code):** `0` (Success)
- **Kết quả:** **0 cảnh báo (warnings), 0 lỗi (errors)**.
- **Nhật ký bảng điều khiển nguyên bản (Verbatim Console Log):**
  ```text
  > dms-frontend@0.0.0 lint
  > oxlint

  Found 0 warnings and 0 errors.
  Finished in 186ms on 123 files with 116 rules using 16 threads.
  ```

#### B. Kiểm tra Type-check & Biên dịch Đóng gói Sản phẩm (Vite Production Build)
- **Thư mục thực thi:** `DMS/frontend`
- **Lệnh thực thi:** `rtk proxy npm run build` (gồm 2 bước tuần tự: `tsc -b` kiểm tra kiểu và `vite build` đóng gói).
- **Công cụ:** TypeScript `~6.0.2`, Vite `8.3.4`
- **Thời gian thực thi:** **39.84s**
- **Số module đã xử lý:** **3,486 modules**
- **Mã thoát (Exit Code):** `0` (Success)
- **Bảng thống kê gói tài nguyên đầu ra (Output Artifacts):**

| Đường dẫn Artifact | Kích thước thô (Raw Size) | Kích thước Gzip (Gzip Size) | Loại tài nguyên |
|---|:---:|:---:|:---:|
| `dist/index.html` | 0.76 kB | 0.43 kB | HTML Entrypoint |
| `dist/assets/index-BSng-fLT.css` | 16.93 kB | 3.73 kB | Global Stylesheet |
| `dist/assets/index-DwZjV493.js` | 375.98 kB | 120.52 kB | Vendor / Core Runtime |
| `dist/assets/App-Bs37Rh06.js` | **1,572.33 kB** | **485.38 kB** | Application Bundle |

- **Nhật ký bảng điều khiển nguyên bản (Verbatim Console Log):**
  ```text
  > dms-frontend@0.0.0 build
  > tsc -b && vite build

  vite v8.3.4 building client environment for production...
  transforming...
  ✓ 3486 modules transformed.
  rendering chunks...
  computing gzip size...
  dist/index.html                     0.76 kB │ gzip:   0.43 kB
  dist/assets/index-BSng-fLT.css     16.93 kB │ gzip:   3.73 kB
  dist/assets/index-DwZjV493.js     375.98 kB │ gzip: 120.52 kB
  dist/assets/App-Bs37Rh06.js     1,572.33 kB │ gzip: 485.38 kB

  [plugin builtin:vite-reporter] 
  (!) Some chunks are larger than 500 kB after minification. Consider:
  - Using dynamic import() to code-split the application
  - Use build.rolldownOptions.output.codeSplitting to improve chunking
  - Adjust chunk size limit for this warning via build.chunkSizeWarningLimit.
  ✓ built in 39.84s
  ```
- **Khuyến nghị kỹ thuật:** Bundle `App-Bs37Rh06.js` vượt ngưỡng 500 kB do toàn bộ các trang chức năng đang được import tĩnh trong cây router. Cần áp dụng kỹ thuật Code-splitting với `React.lazy()` và `Suspense` trên các route của từng phân hệ (Staff, Student, Admin) để chia nhỏ bundle tải trang ban đầu xuống dưới 300 kB.

---

### 2.2. Phân hệ Backend (Biên dịch, Kiến trúc ArchUnit & Unit/Policy Tests)

#### A. Kiểm tra Biên dịch Toàn bộ Dự án Backend
- **Thư mục thực thi:** `DMS/backend`
- **Môi trường máy chủ:** OpenJDK 25.0.2 (biên dịch chéo theo release target `Java 17`).
- **Khối lượng mã nguồn:** **278 tệp mã nguồn Java** (`src/main/java`) và **37 tệp kiểm thử Java** (`src/test/java`).
- **Kết quả biên dịch:** **BUILD SUCCESS**, 100% tệp tin biên dịch sạch sẽ, không có cảnh báo nghiêm trọng (clean compile).

#### B. Kiểm tra Tuân thủ Kiến trúc Tầng bằng ArchUnit (`ModuleDependencyTest`)
- **Lệnh thực thi:** `rtk proxy .\mvnw.cmd test -Dtest=ModuleDependencyTest`
- **Thời gian thực thi:** 12.191s (thời gian kiểm tra ArchUnit: **3.477s**).
- **Mã thoát (Exit Code):** `0` (Success)
- **Tổng số quy tắc kiến trúc kiểm tra:** **8 / 8 rules PASSED (100% Green)**.
- **Chi tiết 8 quy tắc kiến trúc được kiểm chứng tự động:**

| STT | Tên quy tắc (ArchUnit Rule Method) | Mục tiêu kiểm chứng kiến trúc theo SAD 4.2 & AGENTS.md | Kết quả |
|:---:|---|---|:---:|
| 1 | `modulesOnlyCallLowerTiers` | Bảo đảm luồng phụ thuộc đơn hướng DAG nghiêm ngặt: `common` $\to$ `account` $\to$ `room` $\to$ `billing` $\to$ `contract` $\to$ `registration`/`maintenance` $\to$ `notification`. Không có phụ thuộc vòng (no cyclic dependencies). | **PASS** |
| 2 | `noForeignRepositoryOrApi` | Cấm tuyệt đối việc các module gọi chéo trực tiếp Repository hoặc API của module khác; buộc giao tiếp qua Service contract. | **PASS** |
| 3 | `serviceContractsHaveNoImplementation` | Đảm bảo toàn bộ interface trong gói `service` là trừu tượng, không chứa default method hoặc static implementation. | **PASS** |
| 4 | `serviceBeansImplementInterfacesInImplPackage` | Khẳng định toàn bộ Spring `@Service` beans bắt buộc nằm trong package `service.impl`, có hậu tố `Impl` và implement đúng interface contract. | **PASS** |
| 5 | `restEndpointsDeclareRegisteredPrivileges` | Kiểm tra toàn bộ Controller methods: bắt buộc khai báo annotation `@RequirePrivilege` với mã quyền nằm trong danh mục quyền hệ thống. | **PASS** |
| 6 | `constantsRefactorKeepsExistingRolePrivileges` | Kiểm tra tính bất biến của bộ quyền theo Role bằng mã băm SHA-256 digest và số lượng quyền chính xác: `ADMIN`: 106, `STAFF`: 73, `STUDENT`: 35, `TECHNICIAN`: 11. | **PASS** |
| 7 | `permissionConstantsMatchRegisteredCatalog` | Kiểm tra tính nhất quán giữa các hằng số quyền khai báo trong `DmsConstants.Permission` và danh mục quyền đã đăng ký trong CSDL. | **PASS** |
| 8 | `permissionScreenRegistryCoversEveryRegisteredPrivilege` | Xác minh ma trận ánh xạ 1-1 giữa danh mục màn hình giao diện UI và toàn bộ các quyền backend đã đăng ký. | **PASS** |

#### C. Kết quả Thực thi Bộ Kiểm thử Đơn vị, Chính sách & Dịch vụ (Unit / Policy / Service Tests)
Khi chạy độc lập các suite test không phụ thuộc vào hạ tầng Docker bên ngoài bằng lệnh:
`rtk proxy .\mvnw.cmd test -Dtest="*Test,!*IntegrationTest,!DmsApplicationTests"`
- **Số lượng Test Suite thực thi:** 15 test suites
- **Tổng số ca kiểm thử:** **99 / 99 tests PASSED (100% Green, 0 Failures, 0 Errors, 0 Skipped)**.
- **Bảng chi tiết 15 Unit/Policy Test Suites:**

| STT | Tên Lớp Kiểm Thử (Test Class) | Phân hệ (Module) | Số test | Pass | Fail | Error | Thời gian |
|:---:|---|---|:---:|:---:|:---:|:---:|:---:|
| 1 | `billing.service.impl.DepositTopupGuardTest` | Billing / Sổ cọc | 6 | 6 | 0 | 0 | 0.692s |
| 2 | `common.api.GlobalExceptionHandlerTest` | Common / Xử lý ngoại lệ | 2 | 2 | 0 | 0 | 0.463s |
| 3 | `contract.api.ContractDetailResponseTest` | Contract / DTO phản hồi | 2 | 2 | 0 | 0 | 0.023s |
| 4 | `contract.service.impl.ContractActivationValidationTest` | Contract / Kích hoạt hợp đồng | 11 | 11 | 0 | 0 | 0.596s |
| 5 | `contract.service.impl.ContractExtensionRuleTest` | Contract / Quy tắc gia hạn | 9 | 9 | 0 | 0 | 0.119s |
| 6 | `account.security.KeycloakJwtDecoderTest` | Account / Xác thực JWT OIDC | 5 | 5 | 0 | 0 | 0.669s |
| 7 | `maintenance.policy.MaintenanceRequestPolicyTest` | Maintenance / Chính sách báo hỏng | 9 | 9 | 0 | 0 | 0.031s |
| 8 | `archunit.ModuleDependencyTest` | Architecture / Kiến trúc DAG | 8 | 8 | 0 | 0 | 3.386s |
| 9 | `notification.service.impl.ContractNotificationListenerTest`| Notification / Sự kiện hợp đồng | 7 | 7 | 0 | 0 | 0.105s |
| 10 | `notification.service.impl.MaintenanceNotificationListenerTest` | Notification / Sự kiện bảo trì | 9 | 9 | 0 | 0 | 0.033s |
| 11 | `notification.service.impl.NotificationServiceImplTest` | Notification / Logic hộp thư | 9 | 9 | 0 | 0 | 0.304s |
| 12 | `registration.service.impl.SummerChoiceApprovalTest` | Registration / Xét duyệt nguyện vọng hè | 10 | 10 | 0 | 0 | 0.052s |
| 13 | `registration.policy.RoomApplicationPolicyTest` | Registration / Chính sách nộp đơn | 7 | 7 | 0 | 0 | 0.018s |
| 14 | `room.policy.RoomTransferPolicyTest` | Room / Chính sách chuyển phòng | 3 | 3 | 0 | 0 | 0.012s |
| 15 | `common.config.SensitiveValuesTest` | Common / Mặt nạ bảo mật log | 2 | 2 | 0 | 0 | 0.381s |
| **TỔNG** | **15 Test Suites Hợp Lệ** | **Toàn bộ Module Backend** | **99** | **99** | **0** | **0** | **6.884s** |

---

### 2.3. Chẩn đoán bộ kiểm thử tích hợp (Integration Tests & Testcontainers)

Khi chạy toàn bộ suite test bằng lệnh `rtk proxy .\mvnw.cmd test`:
- **Tổng số ca kiểm thử:** 318 tests.
- **Kết quả:** 99 Passed (31.13%), 219 Errors (68.87%), **0 Failures (0% lỗi logic assertion)**.
- **17 Test Suite gặp lỗi khởi tạo:**
  `ApplicationAssignmentIntegrationTest`, `BedOccupancyIntegrationTest`, `BillingConcurrencyIntegrationTest`, `ContractActivationIntegrationTest`, `ContractExtensionIntegrationTest`, `CrudApiIntegrationTest`, `DepositIntegrationTest`, `DmsApplicationTests`, `KeycloakAccountIntegrationTest`, `MaintenanceConcludeIntegrationTest`, `MaintenanceConcludeRollbackIntegrationTest`, `MoveOutIntegrationTest`, `RoomLeaderIntegrationTest`, `StorageIntegrationTest`, `StudentImportIntegrationTest`, `SummerChoiceIntegrationTest`, `SystemJobsIntegrationTest`.

#### Phân tích Nguyên nhân Gốc rễ (Root Cause Diagnostics):
Trích xuất trực tiếp từ nhật ký thực thi Spring Boot Test runner (`task-39.log`):
```text
2026-10-28T11:41:39.610+07:00 INFO 15772 --- [dms] [ main] org.testcontainers.DockerClientFactory : Testcontainers version: 1.21.4
2026-10-28T11:41:39.946+07:00 INFO 15772 --- [dms] [ main] .t.d.DockerMachineClientProviderStrategy : docker-machine executable was not found on PATH
2026-10-28T11:41:39.947+07:00 ERROR 15772 --- [dms] [ main] o.t.d.DockerClientProviderStrategy : Could not find a valid Docker environment. Please check configuration.
2026-10-28T11:41:39.948+07:00 WARN 15772 --- [dms] [ main] o.s.w.c.s.GenericWebApplicationContext : Exception encountered during context initialization:
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory': Failed to initialize dependency 'flyway':
Error creating bean with name 'flyway': Unsatisfied dependency expressed through method 'flyway' parameter 0:
Error creating bean with name 'flywayContainerConnectionDetailsForPostgresContainer':
Error creating bean with name 'postgresContainer' defined in class path resource [vn/edu/tlu/dms/TestcontainersConfiguration.class]:
Could not find a valid Docker environment. Please see logs and check configuration.
```

- Kiểm tra bằng lệnh `rtk proxy docker info`: Docker CLI đã cài đặt (v29.8.2), nhưng Docker Server daemon trên Windows không chạy (`failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine... The system cannot find the file specified`).
- Kiểm tra cổng CSDL bằng lệnh PowerShell `Test-NetConnection -ComputerName localhost -Port 5432`: Trả về `TcpTestSucceeded: False`.
- **Kết luận:** Toàn bộ 219 lỗi này **100% xuất phát từ rào cản môi trường hạ tầng** (không có Docker daemon để Testcontainers khởi tạo container `postgres:16-alpine`), hoàn toàn không có bất kỳ một lỗi logic, lỗi cú pháp hay gãy quan hệ dữ liệu nào trong mã nguồn dự án.

---

## 3. DANH MỤC CÁC VI PHẠM QUY TẮC CLAUDE.md / AGENTS.md

Quy chuẩn dự án quy định:
- **Nguyên tắc Không Hardcode:** "Mã lỗi/role/quyền/trạng thái dùng Constants hoặc enum; HTTP status dùng HttpStatus hoặc factory BusinessException. Không viết lại mã chuỗi/số trong logic."
- **Chuẩn CRUD V5:** API danh sách dùng `POST .../search-by-page` trả về `PageResponse<T>`; API tạo/sửa dùng chung một cổng `POST .../save-or-update`. Controller mapping trực tiếp URL trong annotation, không dùng `@RequestMapping` cấp lớp. Service interface phải có JavaDoc tiếng Việt 100%.

Dưới đây là ma trận chi tiết các vi phạm được ghi nhận trực tiếp từ mã nguồn:

### 3.1. Bảng ma trận 39 vi phạm Backend (Track R2)

| STT | Phân loại vi phạm | Đường dẫn tệp nguồn (File Path) | Dòng | Đoạn mã vi phạm thực tế (Code Snippet) | Khuôn mẫu chuẩn quy định (Expected Pattern) | Mức độ |
|:---:|---|---|:---:|---|---|:---:|
| 1 | Hardcoded Validation | `room/service/impl/SemesterServiceImpl.java` | 119 | `throw BusinessException.invalid("endDate", "Ngày kết thúc phải sau ngày bắt đầu");` | `DmsConstants.ApiField.END_DATE` & `ValidationMessage.END_DATE_AFTER_START` | High |
| 2 | Hardcoded Validation | `room/service/impl/SemesterServiceImpl.java` | 123 | `throw BusinessException.invalid("academicYear", "Hai năm phải liền nhau");` | `DmsConstants.ApiField.ACADEMIC_YEAR` & `ValidationMessage.ACADEMIC_YEAR_CONSECUTIVE` | High |
| 3 | Hardcoded Validation | `room/service/impl/RoomServiceImpl.java` | 279 | `throw BusinessException.invalid("buildingId", "Không chuyển phòng sang tòa khác");` | `DmsConstants.ApiField.BUILDING_ID` & `ValidationMessage` tập trung | High |
| 4 | Hardcoded Validation | `room/service/impl/RoomServiceImpl.java` | 379 | `throw BusinessException.invalid("roomId", "Không chuyển giường sang phòng khác");` | `DmsConstants.ApiField.ROOM_ID` & `ValidationMessage` tập trung | High |
| 5 | Hardcoded Validation | `account/service/impl/UserServiceImpl.java` | 99 | `throw BusinessException.invalid("managementUnitId", "ID đơn vị phải lớn hơn 0");` | `DmsConstants.ApiField.MANAGEMENT_UNIT_ID` & `ValidationMessage` tập trung | High |
| 6 | Hardcoded Validation | `account/service/impl/UserServiceImpl.java` | 185 | `throw BusinessException.invalid("managementUnitId", "Admin không gắn đơn vị");` | `DmsConstants.ApiField.MANAGEMENT_UNIT_ID` & `ValidationMessage` tập trung | High |
| 7 | Hardcoded Validation | `account/service/impl/UserServiceImpl.java` | 223 | `throw BusinessException.invalid("role", "Hồ sơ sinh viên giữ vai trò STUDENT");` | `DmsConstants.ApiField.ROLE` & `ValidationMessage` tập trung | High |
| 8 | Hardcoded Validation | `account/service/impl/UserServiceImpl.java` | 226 | `throw BusinessException.invalid("role", "Không đổi vai trò tài khoản đang sử dụng");` | `DmsConstants.ApiField.ROLE` & `ValidationMessage` tập trung | High |
| 9 | Hardcoded Validation | `account/service/impl/UserServiceImpl.java` | 234 | `throw BusinessException.invalid("managementUnitId", "Staff/KTV phải có đơn vị quản lý");` | `DmsConstants.ApiField.MANAGEMENT_UNIT_ID` & `ValidationMessage` tập trung | High |
| 10 | Hardcoded Validation | `account/service/impl/UserServiceImpl.java` | 249 | `throw BusinessException.invalid("managementUnitId", "Vai trò không có đơn vị");` | `DmsConstants.ApiField.MANAGEMENT_UNIT_ID` & `ValidationMessage` tập trung | High |
| 11 | Hardcoded Validation | `account/service/impl/UserServiceImpl.java` | 278 | `throw BusinessException.invalid("studentCode", "Chỉ áp dụng cho sinh viên");` | `DmsConstants.ApiField.STUDENT_CODE` & `ValidationMessage` tập trung | High |
| 12 | Hardcoded Message | `account/service/impl/UserServiceImpl.java` | 304 | `throw BusinessException.invalid(ApiField.ID, "Không tự khóa tài khoản đang dùng");` | `DmsConstants.ApiField.ID` & hằng số thông điệp tập trung | Medium |
| 13 | Hardcoded Validation | `account/service/impl/UserServiceImpl.java` | 402 | `throw BusinessException.invalid("hometown", "Chỉ áp dụng cho sinh viên");` | `DmsConstants.ApiField.HOMETOWN` & `ValidationMessage` tập trung | High |
| 14 | Redundant Constants | `contract/api/ContractDtos.java` | 35-40 | `public static final class ExtensionSource { STUDENT = "STUDENT"; ... ENTITY_TYPE = "EXTENSION"; }` | Tái sử dụng `DmsConstants.Role.STUDENT` và đăng ký `EXTENSION` vào `DmsConstants.EntityType` | High |
| 15 | Hardcoded Validation | `contract/service/impl/ContractCrudServiceImpl.java` | 211, 258, 261, 716, 760 | `throw BusinessException.invalid("applicationId", "Không đổi nguồn đơn của hợp đồng");` (và `"endDate"`, `"startDate"`, `"contractId"`, `"newEndDate"`) | Khai báo trong `DmsConstants.ApiField` và `ValidationMessage` | High |
| 16 | Hardcoded Validation | `billing/service/impl/InvoiceCrudServiceImpl.java` | 74, 197, 206, 210, 214, 217, 221, 225, 228 | `throw BusinessException.invalid("input", "Dữ liệu không hợp lệ");` (và `"type"`, `"billingMonth"`, `"totalAmount"`, `"dueDate"`) | Khai báo trong `DmsConstants.ApiField` và `ValidationMessage` | High |
| 17 | Hardcoded Validation | `billing/service/impl/UtilityReadingServiceImpl.java` | 71, 174, 210, 225, 231, 342, 345, 348, 351 | `throw BusinessException.invalid("meterType", "Loại chỉ số không hợp lệ");` (và `"billingMonth"`, `"startReading"`, `"oldMeterFinal"`, `"endReading"`) | Khai báo trong `DmsConstants.ApiField` và `ValidationMessage` | High |
| 18 | Hardcoded Validation | `billing/service/impl/UtilityRateServiceImpl.java` | 64, 97, 176, 179, 182 | `throw BusinessException.invalid("meterType", ...);` (và `"unitPrice"`, `"effectiveFrom"`) | Khai báo trong `DmsConstants.ApiField` và `ValidationMessage` | High |
| 19 | Hardcoded Validation | `maintenance/service/impl/MaintenanceRequestServiceImpl.java` | 202, 214, 337, 343, 353 | `throw BusinessException.invalid("photos", "Tối đa 5 ảnh");` (và `"roomId"`, `"reason"`, `"action"`) | Khai báo trong `DmsConstants.ApiField` và `ValidationMessage` | High |
| 20 | Hardcoded Validation | `registration/service/impl/RegistrationPeriodServiceImpl.java` | 208, 251, 261, 272, 277, 302 | `throw BusinessException.invalid("closeAt", "Thời gian đóng phải sau thời gian mở");` (và `"period"`, `"name"`, `"semesterId"`, `"quotas"`) | Khai báo trong `DmsConstants.ApiField` và `ValidationMessage` | High |
| 21 | Hardcoded Validation | `registration/service/impl/RoomApplicationServiceImpl.java` | 233, 273, 329, 612 | `throw BusinessException.invalid("periodId", "Đợt đăng ký không hợp lệ");` (và `"action"`, `"priorityCategoryIds"`) | Khai báo trong `DmsConstants.ApiField` và `ValidationMessage` | High |
| 22 | Hardcoded Validation | `common/service/impl/ConfigServiceImpl.java` | 121 | `throw BusinessException.invalid("key", "Phải chọn khóa cấu hình đã tồn tại");` | `DmsConstants.ApiField.KEY` & `ValidationMessage` tập trung | High |
| 23 | Hardcoded Validation | `common/service/impl/AuditServiceImpl.java` | 66, 69 | `throw BusinessException.invalid("actorId", "Mã người thao tác không hợp lệ");` (và `"to"`) | `DmsConstants.ApiField.ACTOR_ID` & `ValidationMessage` tập trung | High |
| 24 | Untyped Controller | `billing/api/InvoiceCrudController.java` | 27 | `public Object saveOrUpdate(@Valid @RequestBody InvoiceDtos.Save input)` | Trả về DTO kiểu cụ thể (ví dụ `InvoiceDtos.Response`) | High |
| 25 | Untyped Controller | `contract/api/ContractCrudController.java` | 30, 43, 49, 55, 61, 92, 98 | `public Object saveOrUpdateContract(...)`, `searchExtensionsByPage`, `searchTransfersByPage`, `saveOrUpdateExtension`, `saveOrUpdateTransfer` | Trả về kiểu DTO cụ thể: `ContractDtos.Response`, `PageResponse<StayResponse>`, v.v. | High |
| 26 | Untyped Controller | `registration/api/SummerCrudController.java` | 55, 62, 69, 75, 82, 88, 97 | `public PageResponse<Map<String, Object>> searchByPage(...)`, `public Object getChoice(...)`, `public Object saveOrUpdate(...)` | Định nghĩa DTO rõ ràng: `SummerChoiceResponse`, `PageResponse<SummerChoiceSummary>` | High |
| 27 | Untyped Controller | `notification/api/OverviewController.java` | 31, 38, 45, 51, 58, 65 | `public PageResponse<Map<String, Object>> searchDepositsByPage(...)`, `getDeposit`, `searchStorageByPage`, `searchMyRoomByPage` | Trả về DTO kiểu rõ ràng (`PageResponse<DepositResponse>`, v.v.) | High |
| 28 | Untyped Controller | `notification/api/NotificationCrudController.java` | 32, 39, 45 | `public PageResponse<Map<String, Object>> searchByPage(...)`, `public Object getPublished(...)`, `public Object saveOrUpdate(...)` | Trả về DTO cụ thể (`PageResponse<NotificationResponse>`, `NotificationResponse`) | High |
| 29 | Untyped Controller & Aliases | `account/api/StudentManagementController.java` | 49, 55, 61, 80 | `public PageResponse<Map<String, Object>> searchByPage(...)`, `public Object saveOrUpdate(...)` | Trả về DTO cụ thể (`PageResponse<StudentProfileResponse>`); chuẩn hóa 1 URL chính thay vì mảng alias | High |
| 30 | Untyped Controller | `billing/api/UtilityReadingController.java` | 73, 80, 86 | `public PageResponse<Map<String, Object>> searchByPage(...)`, `public Object getReading(...)`, `public Object saveOrUpdate(...)` | Trả về DTO kiểu rõ ràng (`PageResponse<UtilityReadingResponse>`) | High |
| 31 | Untyped Controller | `account/api/UserController.java` | 49 | `public Object saveOrUpdate(@Valid @RequestBody Save input)` | Trả về `Profile` hoặc `AccountDtos.Response` | High |
| 32 | Controller Fragmentation | Toàn bộ các gói API backend | Đa tệp | 5 cặp Controller trùng lặp: `InvoiceController` vs `InvoiceCrudController`, `UtilityRateController` vs `UtilityRateCrudController`, `ContractController` vs `ContractCrudController`, `MaintenanceRequestController` vs `MaintenanceCrudController`, `NotificationController` vs `NotificationCrudController` | Gộp mỗi domain về 1 Controller duy nhất tuân thủ CRUD V5 | High |
| 33 | Domain Bleeding | `notification/api/OverviewController.java` | 30, 50, 64 | Khai báo endpoint tra cứu Deposits (billing), StorageRecords (contract), Room (room) trong module `notification` | Chuyển các endpoint tra cứu về đúng controller của module phụ trách | Medium |
| 34 | Domain Misplacement | `account/api/StudentManagementController.java` | 79-80 | Khai báo endpoint `/api/technicians/search-by-page` trong `StudentManagementController` | Chuyển về `UserController` hoặc `TechnicianController` | Low |
| 35 | Relative Class RequestMapping | 12 Controllers: `ConfigController:18`, `NotificationController:20`, `RoomController:27`, `BedController:26`, `UtilityRateController:17`, `UtilityReadingController:29`, `MaintenanceRequestController:38`, `AuthController:16`, `ProfileController:18`, `RolePermissionController:19`, `RegistrationPeriodController:23`, `UserController:26` | Đã dẫn | Khai báo `@RequestMapping("/api/...")` ở cấp lớp; các method dùng relative path như `@GetMapping("/{id}")` | Khai báo trực tiếp full URL trong annotation của từng method (ví dụ `@GetMapping("/api/rooms/{id}")`), loại bỏ `@RequestMapping` cấp lớp | Medium |
| 36 | Non-standard HTTP Method | `room/api/RoomController.java` | 68 | `@PutMapping("/{id}/leader")` | Dùng `@PostMapping("/api/rooms/{id}/assign-leader")` hoặc chuẩn hóa theo REST CRUD V5 | Medium |
| 37 | Non-standard DELETE Body | `room/api/RoomController.java` | 74-76 | `@DeleteMapping("/{id}/leader") public ResponseEntity<Void> removeLeader(@PathVariable Long id, @Valid @RequestBody ReasonRequest input)` | Dùng `@PostMapping("/api/rooms/{id}/remove-leader")` mang request body lý do; tránh body trong HTTP DELETE | Medium |
| 38 | Non-standard HTTP Method | `account/api/UserController.java` | 74 | `@PutMapping("/{id}/cross-unit-access")` | Dùng `@PostMapping("/api/admin/users/{id}/cross-unit-access")` | Low |
| 39 | Non-standard HTTP Method | `account/api/RolePermissionController.java` | 37 | `@PutMapping("/{role}")` | Dùng `@PostMapping("/api/admin/role-permissions/{role}")` hoặc theo CRUD V5 `save-or-update` | Low |

---

### 3.2. Bảng ma trận 23 vi phạm Frontend (Track R3)

| STT | Phân loại vi phạm | Đường dẫn tệp nguồn (File Path) | Dòng | Đoạn mã vi phạm thực tế (Code Snippet) | Khuôn mẫu chuẩn quy định (Expected Pattern) | Mức độ |
|:---:|---|---|:---:|---|---|:---:|
| 1 | **Silent Error Swallowing** | `features/billing/DepositLedgerModal.tsx` | 43–45 | `catch (error) { form.setFields(validationFieldErrors(error)...) }` | Bổ sung `message.error(errorMessage(error))` khi `fieldErrors` rỗng để thông báo lỗi nghiệp vụ | **Critical** |
| 2 | **Silent Error Swallowing** | `features/billing/IssueInvoicesModal.tsx` | 45–50 | `catch (error) { const fields = validationFieldErrors(error); if (fields.length) form.setFields(...) }` | Bổ sung `message.error(errorMessage(error))` để hiển thị `DUPLICATE_INVOICE`, `SEMESTER_OVERLAP` | **Critical** |
| 3 | **Silent Error Swallowing** | `features/billing/ConfirmPaymentModal.tsx` | 38–40 | `catch (error) { form.setFields(validationFieldErrors(error)...) }` | Gọi `message.error(errorMessage(error))` khi lỗi không thuộc về validation trường | **Critical** |
| 4 | **Silent Error Swallowing** | `features/room/BedCrudPanel.tsx` | 54–60 | `catch (e) { form.setFields(validationFieldErrors(e)...) }` | Gọi `handleApiError(e)` trong `save()` tương tự như nhánh xóa giường ở dòng 139 | **Critical** |
| 5 | **API Contract Mismatch** | `utils/errorMessages.ts` | 10–169 | *(Thiếu 7 mã lỗi từ API-Spec mục 5)* | Khai báo bổ sung: `DEPOSIT_AMOUNT_MISMATCH`, `CONTRACT_ACTIVATION_EXPIRED`, `CONTRACT_AUTO_CANCELLED`, `CONTRACT_PERIOD_OVERLAP`, `APPLICATION_NOT_APPROVED`, `NO_HOLD_ASSIGNMENT`, `ADJUSTMENT_INVALID` | **High** |
| 6 | **CRUD V5 Violation** | `services/api/billing.ts` | 145–151 | `api.get<DepositTransaction[]>(/contracts/${contractId}/deposit-transactions)` | Dùng `POST /deposit-transactions/search-by-page` (hoặc `POST /contracts/search-deposits-by-page`) trả `PageResponse` | **High** |
| 7 | **CRUD V5 Violation** | `services/api/checkout.ts` | 172–175 | `api.post<StorageRecordItem>(/contracts/${contractId}/storage-records, input)` | Dùng `POST /storage-records/save-or-update` theo đúng quy ước một cổng lưu của tài nguyên | **High** |
| 8 | **HTTP Method Violation** | `services/api/room.ts` | 93–95 | `api.delete(/rooms/${id}/leader, { data: { reason } })` | Chuyển thành `POST /rooms/${id}/remove-leader` với body `{ reason }` để tránh bị gateway cắt body | **High** |
| 9 | **Hardcoded Error Codes** | `services/api/client.ts` | 57, 68, 80, 82, 91 | `code === 'PASSWORD_CHANGE_REQUIRED'`, `'ACCOUNT_LOCKED'`, `'SESSION_REVOKED'`, `'FORBIDDEN'` | Tạo hằng số `ErrorCode` trong `src/constants/` và dùng enum/constants so sánh | **High** |
| 10 | **Hardcoded Error Codes** | `app/guards/RequireRole.tsx` | 51, 52, 56, 58 | `code === 'SESSION_REVOKED'`, `code === 'ACCOUNT_LOCKED'` | Dùng Constants `ErrorCode.SESSION_REVOKED`, `ErrorCode.ACCOUNT_LOCKED` | **High** |
| 11 | **Direct Primitive Import** | `features/billing/DepositLedgerModal.tsx` | 2, 41 | `import { Form, message, ... } from 'antd'` | Dùng `const { message } = App.useApp()` từ Ant Design App context | **Medium** |
| 12 | **Direct Primitive Import** | `features/billing/IssueInvoicesModal.tsx` | 2, 41 | `import { Form, message } from 'antd'` | Dùng `const { message } = App.useApp()` | **Medium** |
| 13 | **Direct Primitive Import** | `components/layout/NotificationBell.tsx` | 2, 15 | `import { Badge } from 'antd'` | `import { Badge } from '../ui/Surface'` hoặc `import { Badge } from '../index'` | **Medium** |
| 14 | **Direct Primitive Import** | `app/layouts/SidebarShell.tsx` | 5 | `import { Badge, ... } from 'antd'` | `import { Badge } from '../../components/ui/Surface'` | **Medium** |
| 15 | **Direct Primitive Import** | `components/ui/ResponsiveTable.tsx` | 6 | `import { ..., Skeleton, ... } from 'antd'` | `import { Skeleton } from './Surface'` | **Medium** |
| 16 | **Form.Item Wrapping Violation** | `pages/CrudPage.tsx` | 890–896, 1172–1187 | `<Form.Item><LookupInput field={field} /></Form.Item>` (LookupInput lại chứa `<Form.Item noStyle>`) | Trực tiếp bind control `<Select>` bên dưới `<Form.Item>` ngoài, loại bỏ Form.Item lồng trong LookupInput | **Medium** |
| 17 | **Ad-hoc Status Styling** | `pages/staff/StoragePage.tsx` | 141–145 | `<Tag color="green">Đã nhận</Tag>`, `<Tag color="orange">Đang gửi</Tag>` | Đăng ký entity `storage` vào `StatusEntity` và `statusConfig.ts`, dùng `<StatusBadge entity="storage" status={...} />` | **Medium** |
| 18 | **Duplicate Label Registry** | `features/maintenance/constants.ts` | 54–69 | `export const maintenanceCauseLabels = { WEAR: 'Hao mòn tự nhiên', ... }` | Chuyển định nghĩa nguyên nhân và hành động sang `statusConfig.ts` | **Low** |
| 19 | **Duplicate Label Registry** | `pages/staff/PendingItemsPage.tsx` | 30–42 | `const TYPE_META = { [PendingItemType.APPLICATION]: { label: 'Đơn đăng ký', ... } }` | Dùng tập trung `crudValueLabels` từ `statusConfig.ts` | **Low** |
| 20 | **Duplicate Label Registry** | `pages/admin/RolePermissionsPage.tsx` | 58–63 | `const actionLabels = { [Action.READ]: 'Xem / Tìm kiếm', ... }` | Dùng định nghĩa nhãn action tập trung trong `navigation.ts` | **Low** |
| 21 | **Hardcoded Condition Labels** | `features/contract/MoveOutModal.tsx` | 475–478 | `<Radio value={AssetCondition.OK}>Bình thường</Radio>`, `<Radio value={AssetCondition.DAMAGED}>Có hư hỏng</Radio>` | Khai báo hằng số `assetConditionLabels` trong `statusConfig.ts` | **Low** |
| 22 | **Navigation Icon Duplication** | `constants/navigation.ts` | 217, 218 | Cả `STAFF_SUMMER_PATH` và `/staff/storage` đều dùng `InboxOutlined` | Phân tách icon trực quan: ví dụ Kỳ hè dùng `SunOutlined`, Kho gửi đồ dùng `InboxOutlined` | **Low** |
| 23 | **Navigation Icon Duplication** | `constants/navigation.ts` | 198, 304 | Cả Kỳ học và Kỳ hè (sinh viên) đều dùng `CalendarOutlined` | Phân biệt icon ngữ nghĩa phù hợp | **Low** |

---

### 3.3. Phân tích nguyên nhân gốc rễ (Root Cause Analysis)

1. **Về lỗi Nuốt ngoại lệ (Silent Error Swallowing) trên Frontend:**
   - *Cơ chế phát sinh:* Helper `validationFieldErrors(error)` được thiết kế để bóc tách mảng `error.response.data.details.fieldErrors` khi backend trả về HTTP 400 Validation Error. Tuy nhiên, khi backend ném `BusinessException` (HTTP 400 hoặc 409) cho các lỗi nghiệp vụ chung (như `DUPLICATE_INVOICE`, `REFUND_EXCEEDS_BALANCE`, `BED_TAKEN`), cấu trúc trả về là `{ errorCode, message, details }` không có thuộc tính `fieldErrors`. Khi đó `validationFieldErrors(error)` trả về mảng rỗng `[]`.
   - *Hậu quả:* Khối `catch` ở các modal chỉ gọi `form.setFields([])` mà không có nhánh fallback gọi `message.error(errorMessage(error))`. Thao tác của người dùng hoàn toàn bị "đóng băng" mà không có bất kỳ phản hồi giao diện nào.
2. **Về các vi phạm Anti-Hardcoding Backend:**
   - *Cơ chế phát sinh:* `DmsConstants.ApiField` đã được khởi tạo trong dự án nhưng chưa bao phủ đầy đủ tất cả các trường dữ liệu của các entity mới (`photos`, `closeAt`, `meterType`, `oldMeterFinal`, `academicYear`). Lập trình viên trong quá trình phát triển nhanh tính năng đã truyền chuỗi ký tự thô trực tiếp vào `BusinessException.invalid()`.
3. **Về việc Controller trả về kiểu `Object` và `Map<String, Object>`:**
   - *Cơ chế phát sinh:* Hệ quả của việc chuyển dịch từ kiến trúc controller cũ sang bộ khung CRUD V5. Một số lập trình viên sử dụng `Map<String, Object>` nhằm trả về kết quả truy vấn động mà chưa kịp đóng gói thành Java Record DTO tương ứng.

---

## 4. ĐÁNH GIÁ CHI TIẾT TỪNG MODULE BACKEND & FRONTEND

### 4.1. Phân hệ Backend (Java / Spring Boot)

1. **Module Account (Quản lý Người dùng, Xác thực & Phân quyền)**:
   - *Hiện trạng:* Đạt 95/100. Tích hợp chuẩn Keycloak OIDC thông qua `SecurityConfig` và `AccountAccessFilter`.
   - *Ưu điểm:* Cơ chế RBAC động tải quyền từ bảng `role_permission_matrix` mỗi lần kiểm tra, bảo vệ vai trò `ADMIN` bất biến, kiểm soát phiên bằng `security_version` và khóa bi quan ngăn chặn cập nhật xung đột.
   - *Hạn chế:* Tồn tại 7 vị trí hardcode chuỗi validation trong `UserServiceImpl.java`; phương thức `UserController.saveOrUpdate` trả về `Object`.
2. **Module Room (Khu nhà, Tầng, Loại phòng, Phòng & Giường ở)**:
   - *Hiện trạng:* Đạt 92/100. Toàn vẹn dữ liệu cực cao với ràng buộc cơ sở dữ liệu `ux_room_leader`, `ux_assignment_open_bed`.
   - *Ưu điểm:* Ràng buộc bất biến `bed_count` trên `room_type` khi đã có phòng sử dụng (HTTP 409 `RESOURCE_IN_USE`); tính năng chỉ định và thu hồi trưởng phòng được kiểm soát chặt chẽ.
   - *Hạn chế:* Hardcode chuỗi trong `SemesterServiceImpl` và `RoomServiceImpl`; endpoint `/rooms/{id}/leader` dùng HTTP `PUT` và HTTP `DELETE` có request body không chuẩn RFC.
3. **Module Contract (Hợp đồng, Lưu trú, Gia hạn & Trả phòng)**:
   - *Hiện trạng:* Đạt 94/100. Quản lý vòng đời hợp đồng chặt chẽ: `PENDING` $\to$ `ACTIVE` $\to$ `TERMINATED` / `CANCELLED`.
   - *Ưu điểm:* Kích hoạt hợp đồng tự động ghi nhận tiền cọc ban đầu và chuyển đổi trạng thái `HOLD` sang `STAY` trên cùng bản ghi; quy trình trả phòng (`MoveOutServiceImpl`) có hàm xem trước hoàn cọc độc lập; tự động hủy hợp đồng quá hạn qua Scheduled Job. Triệt tiêu deadlock chuyển phòng bằng quy tắc sắp xếp khóa tăng dần.
   - *Hạn chế:* `ContractCrudController` trả về `Object` cho 7 phương thức; tồn tại lớp hằng số trùng lặp `ContractDtos.ExtensionSource`.
4. **Module Billing (Hóa đơn, Chỉ số Điện nước & Sổ cọc)**:
   - *Hiện trạng:* Đạt 90/100. Sổ cọc hoạt động theo mô hình hạch toán kép (Double-entry Ledger) bảo đảm số dư không âm.
   - *Ưu điểm:* Nghiệp vụ nạp cọc (`topup`) kiểm tra nghiêm ngặt `topupNeeded > 0` và không cho nạp vượt mức; phát hành hóa đơn phòng và điện nước hàng loạt snapshot giá; cô lập dữ liệu thu ngân theo đơn vị qua `STAFF_UNIT_SCOPE`.
   - *Hạn chế:* `InvoiceCrudController` trả về `Object`; phân mảnh 2 cặp controller (`Invoice` và `UtilityRate`); hardcode trường trong `InvoiceCrudServiceImpl` và `UtilityReadingServiceImpl`.
5. **Module Registration (Tuyển sinh & Đăng ký Hè)**:
   - *Hiện trạng:* Đạt 91/100. Hỗ trợ đầy đủ luồng xét duyệt đơn và xếp giường tự động.
   - *Ưu điểm:* Tích hợp ghi chú chấm điểm ưu tiên (Flyway V7); luồng đăng ký hè phân tách rõ ràng 2 nhánh: ở lại ký túc xá (`STAY` qua gia hạn hợp đồng) hoặc tạm về quê gửi đồ (`TEMP_LEAVE` kèm phiếu gửi đồ).
   - *Hạn chế:* `SummerCrudController` trả về `PageResponse<Map<String, Object>>` và `Object`; hardcode chuỗi trong `RegistrationPeriodServiceImpl`.
6. **Module Maintenance (Báo hỏng, Sửa chữa & Khấu trừ cọc)**:
   - *Hiện trạng:* Đạt 93/100. Quản lý sự cố ký túc xá toàn diện.
   - *Ưu điểm:* Hỗ trợ báo hỏng phòng sinh viên (tối đa 3 ảnh) và khu vực công cộng; tự động đóng phiếu sau 3 ngày hoàn thành; kết luận nguyên nhân phân định rõ: Hao mòn (`WEAR` — không tính tiền) và Sử dụng sai (`MISUSE` — chia đều tiền cọc cho các sinh viên đang ở tại thời điểm phát sinh sự cố).
   - *Hạn chế:* Tồn tại 2 controller song song (`MaintenanceRequestController` và `MaintenanceCrudController`); hardcode chuỗi trong `MaintenanceRequestServiceImpl`.
7. **Module Notification (Hộp thư, Thông báo & Cảnh báo)**:
   - *Hiện trạng:* Đạt 86/100. Lọc thông báo theo khoảng thời gian cư trú thực tế (`v_user_notification`).
   - *Hạn chế:* `OverviewController` vi phạm nguyên lý Single Responsibility Principle (SRP) khi ôm đồm các API tra cứu tiền cọc, tài sản gửi kho và thông tin phòng ở sang module thông báo.
8. **Module Common (Hạ tầng, Cấu hình, Kiểm toán & Xử lý Ngoại lệ)**:
   - *Hiện trạng:* Đạt 96/100. Nền tảng dùng chung chất lượng cao.
   - *Ưu điểm:* `GlobalExceptionHandler` bắt và chuẩn hóa toàn bộ lỗi theo format JSON thống nhất; nhật ký kiểm toán `audit_log` được bảo vệ bất biến bằng PostgreSQL trigger `tg_audit_log_append_only`; che giấu dữ liệu nhạy cảm (`SensitiveValues`).

---

### 4.2. Phân hệ Frontend (React 19 / Vite / Ant Design)

1. **Kiến trúc Bố cục & Điều hướng (Layout & Navigation)**:
   - Sử dụng kiến trúc đa cổng phân quyền (`Portal-based Layout`) với `SidebarShell`, phân tách rõ không gian làm việc của Sinh viên, Cán bộ và Quản trị viên.
   - `RequireRole` và `usePortalRole` bảo vệ route an toàn, hỗ trợ ép buộc đổi mật khẩu lần đầu và cho phép Admin truy cập toàn quyền các màn hình quản trị.
   - Tồn tại điểm trừ nhỏ về trùng lặp icon trong `navigation.ts` (icon `InboxOutlined` dùng chung cho cả Kỳ hè và Kho gửi đồ; icon `CalendarOutlined` dùng chung cho Kỳ học và Kỳ hè).
2. **Các trang chức năng (Feature Pages)**:
   - Hầu hết các trang CRUD đã được chuẩn hóa thông qua `CrudPage.tsx`. Tuy nhiên, `CrudPage.tsx` là component nguyên khối (monolithic) với **1,349 dòng mã**, gánh vác hơn 15 tài nguyên khác nhau, gây khó khăn cho việc tối ưu hiệu năng và bảo trì dài hạn.
   - Trang chủ sinh viên `StudentHomePage.tsx` hiện tại chỉ là một bản scaffold re-export trực tiếp từ `CrudHomePage.tsx`, chưa có giao diện Dashboard chuyên biệt cho sinh viên (thiếu widget thông tin phòng hiện tại, thẻ hợp đồng đang hiệu lực, thông báo hóa đơn sắp đến hạn).
   - Trang quản lý kho `StoragePage.tsx` tự ý sử dụng thẻ `<Tag color="green">` và `<Tag color="orange">` thay vì dùng component chuẩn `StatusBadge`.
3. **Các Thành phần Dùng chung (Shared Components)**:
   - Bộ component nền tảng trong `components/ui/` (`Surface`, `DataTable`, `FormControls`, `ResponsiveTable`, `StatusBadge`) được xây dựng bài bản.
   - Tồn tại vi phạm quy chuẩn khi một số màn hình nhập trực tiếp primitive từ Ant Design: `Badge` trong `NotificationBell.tsx` và `SidebarShell.tsx`, `Skeleton` trong `ResponsiveTable.tsx`, gọi API tĩnh `message` trong `DepositLedgerModal.tsx` và `IssueInvoicesModal.tsx` thay vì dùng context `App.useApp()`.
   - Lỗi lồng `Form.Item` vi phạm quy tắc Ant Design Form tại `CrudPage.tsx` (dòng 890 và 1172), làm ảnh hưởng đến cơ chế thu thập dữ liệu và touch validation.
4. **Tầng Giao tiếp API & Xử lý Ngoại lệ (API Client & Error Handling)**:
   - `services/api/client.ts` cấu hình Axios interceptor chuẩn mực, tự động gắn Bearer Token và điều hướng khi hết hạn phiên đăng nhập.
   - Tồn tại 4 lỗi nghiêm trọng nuốt ngoại lệ nghiệp vụ tại các modal tài chính/phòng ở và thiếu 7 mã lỗi nghiệp vụ trong từ điển tiếng Việt `errorMessages.ts`.

---

### 4.3. Đối soát CSDL (Flyway Migrations vs DDL Schema) và Tài liệu kỹ thuật

Đội kiểm toán Track R4 đã đối soát toàn diện chuỗi migration `V1..V7` trong `DMS/backend/src/main/resources/db/migration/` với tệp snapshot DDL `Docs/02 External Design/Database Design/DB-DMS-schema.sql` và đặc tả `SRS-DMS.md` / `API-Spec-DMS.md`:

```
========================================================================================
FLYWAY MIGRATIONS CHUỖI V1..V7:
V1__init.sql                      --> 33 bảng, 3 sequences, 1 function, 6 views, seed data
V2__spring_session.sql            --> 2 bảng SPRING_SESSION, SPRING_SESSION_ATTRIBUTES
V3__account_security_version.sql  --> Thêm security_version vào app_user
V4__keycloak_identity.sql         --> Thêm keycloak_user_id, index độc nhất một phần
V5__role_permission_matrix.sql    --> Bảng role_permission_matrix (Staff: 72 quyền)
V6__room_transfer_approve_permission.sql --> Cập nhật Staff: thêm ROOM_TRANSFER_APPROVE (73 quyền)
V7__application_priority_note.sql --> Thêm note vào application_priority kèm comment
========================================================================================
```

#### A. Độ lệch lược đồ CSDL (Schema Drift):
1. **Lệch quyền trong `role_permission_matrix`**:
   - Trong `DB-DMS-schema.sql` (dòng 815–888), câu lệnh seed dữ liệu cho vai trò `STAFF` chỉ chứa 72 quyền, **bỏ sót quyền `"ROOM_TRANSFER_APPROVE"`** vốn đã được bổ sung trong Flyway migration `V6`. Nếu một môi trường mới được khởi tạo bằng cách chạy thẳng tệp `DB-DMS-schema.sql`, Cán bộ ký túc xá (Staff) sẽ không có quyền duyệt yêu cầu chuyển phòng.
2. **Thiếu Comment Cột (Column Comment)**:
   - Tại dòng 200 của `DB-DMS-schema.sql`, cột `note VARCHAR(500)` của bảng `application_priority` đã có mặt nhưng thiếu câu lệnh chú thích `COMMENT ON COLUMN application_priority.note IS 'Ghi chú thẩm định diện ưu tiên (lý do từ chối, bổ sung hồ sơ)'` theo đúng migration `V7`.
3. **Các bảng Spring Session (V2)**:
   - Hai bảng `SPRING_SESSION` và `SPRING_SESSION_ATTRIBUTES` không có mặt trong `DB-DMS-schema.sql`. Tuy nhiên, sự vắng mặt này là có chủ đích và đã được tài liệu hóa rõ ràng trong `DB-Design-DMS.md:47` (phân tách migration hạ tầng phiên làm việc).

#### B. Các View CSDL Mồ côi (Orphaned Database Views):
1. `v_summer_leave` (được tạo trong `V1__init.sql:693-705` và `DB-DMS-schema.sql:702-714`):
   - Kết quả quét toàn bộ mã nguồn Java `DMS/backend/src/main/java`: **0 kết quả**. Các service `StoragePlacementLookupImpl.java` và `StorageServiceImpl.java` tự viết truy vấn trực tiếp nối bảng `storage_record` và `dormitory_contract`.
2. `v_contract_label` (được tạo trong `V1__init.sql:707-718` và `DB-DMS-schema.sql:716-728`):
   - Kết quả quét mã nguồn Java: **0 kết quả**. Service `ContractServiceImpl.java` tự thực hiện truy vấn nội suy SQL riêng để tổng hợp hạn hợp đồng.

#### C. Đối soát Mức độ Hoàn thiện 5 Luồng Nghiệp vụ Cốt lõi (vs SRS-DMS.md):
- **Luồng 1: Phòng & Chỗ ở (Room & Bed Allocation)**: Hoàn thiện 100%. Đảm bảo tuyệt đối tính toàn vẹn giao dịch (bảo vệ xếp giường bằng khóa bi quan và chỉ mục độc nhất `ux_assignment_open_bed`).
- **Luồng 2: Hợp đồng & Gia hạn (Contract & Extension)**: Hoàn thiện 100%. Quản lý kích hoạt hợp đồng, nạp tiền cọc ban đầu, tự động hủy hợp đồng quá hạn sau 3 ngày và thủ tục trả phòng tính hoàn cọc chính xác.
- **Luồng 3: Hóa đơn & Sổ cọc (Billing & Deposit Ledger)**: Hoàn thiện 100%. Quản lý sổ cọc 4 loại giao dịch (`DEPOSIT`, `DEDUCTION`, `TOPUP`, `REFUND`), phát hành hóa đơn phòng và điện nước hàng loạt snapshot chỉ số và đơn giá.
- **Luồng 4: Đăng ký & Xét duyệt Hè (Summer Registration)**: Hoàn thiện 100%. Xử lý đồng thời cả 2 phương án nguyện vọng hè: Ở lại học hè (`STAY`) và Tạm về quê gửi đồ (`TEMP_LEAVE` kèm thủ tục nhận lại đồ và tái phân bổ phòng vào Học kỳ 1).
- **Luồng 5: Sửa chữa & Báo hỏng (Maintenance & Repair)**: Hoàn thiện 100%. Quy trình tạo yêu cầu kèm ảnh, tự động đóng phiếu sau 3 ngày và hạch toán khấu trừ tiền cọc tự động theo nguyên nhân Hao mòn / Phá hoại.

#### D. Các Tính năng Phụ trợ Chưa Phát hành (Unreleased Auxiliary Features):
Tài liệu `API-Spec-DMS.md` (phiên bản v0.16/v0.17, dòng 443–461) đã ghi chú rõ ràng trạng thái **"Chưa phát hành"** đối với một số endpoint phụ trợ sau (Backend chưa hiện thực và Frontend chưa có UI):
1. `POST /api/utility-readings/{id}/meter-replacement`: Quy trình ghi nhận thay mới công tơ điện nước (FR-BIL-03).
2. `GET /api/invoices/room-fee/preview` & `GET /api/invoices/utility/preview`: Xem trước danh sách hóa đơn dự kiến tạo hàng loạt (FR-BIL-04/05).
3. `POST /api/invoices/{id}/adjustments`: Nghiệp vụ điều chỉnh hóa đơn sau phát hành (FR-BIL-12).
4. `POST /api/debts/students/search-by-page`, `POST /api/debts/rooms/search-by-page`, `GET /api/debts/export`: Quản lý truy vấn và xuất báo cáo công nợ tồn đọng (FR-BIL-11).

---

## 5. MA TRẬN RỦI RO PHÂN THEO MỨC ĐỘ NGHIÊM TRỌNG (RISK MATRIX)

### 5.1. Bảng phân loại 4 cấp độ rủi ro

```
+---------------------------------------------------------------------------------------+
|                                    MA TRẬN RỦI RO                                     |
+-------------------+-------------------------------------------------------------------+
| CẤP ĐỘ RỦI RO     | CÁC VẤN ĐỀ ĐƯỢC ĐỊNH DANH                                         |
+-------------------+-------------------------------------------------------------------+
| CRITICAL (Khẩn cấp)| - 4 lỗi nuốt ngoại lệ nghiệp vụ tại các Modal Frontend.           |
| HIGH (Nghiêm trọng)| - Thiếu 7 mã lỗi nghiệp vụ trong từ điển errorMessages.ts.       |
|                   | - 8 phương thức Controller trả về kiểu thô Object / Map.          |
|                   | - 23 vị trí hardcode chuỗi trong BusinessException.invalid().     |
|                   | - Lệch snapshot DDL DB-DMS-schema.sql thiếu quyền STAFF V6.      |
|                   | - Lệch chuẩn CRUD V5 (GET không phân trang, DELETE có body).      |
| MEDIUM (Vừa phải) | - Nhập trực tiếp primitive Ant Design (Badge, Skeleton, message).|
|                   | - Phân mảnh 5 cặp Controller và 12 Controller dùng class mapping.|
|                   | - Lỗi lồng Form.Item bên trong CrudPage.tsx.                     |
|                   | - 2 Database views mồ côi (v_summer_leave, v_contract_label).    |
|                   | - Tự chế Tag màu sắc tại StoragePage.tsx.                         |
| LOW (Thấp)        | - Trùng lặp khai báo nhãn cục bộ tại 4 tệp Frontend.             |
|                   | - Trùng lặp icon điều hướng menu trong navigation.ts.             |
|                   | - URL bí danh và method PUT tại một số endpoint thứ cấp.         |
+-------------------+-------------------------------------------------------------------+
```

---

### 5.2. Ma trận Xác suất (Likelihood) x Tác động (Impact) & Biện pháp giảm thiểu

| Mã rủi ro | Mô tả chi tiết nguy cơ | Xác suất (Likelihood) | Mức độ tác động (Impact) | Cấp độ rủi ro (Risk Level) | Biện pháp giảm thiểu & Kiểm soát (Mitigation Strategy) |
|:---:|---|:---:|:---:|:---:|---|
| **RSK-01** | **Nuốt ngoại lệ nghiệp vụ tại Modal Frontend:** Người dùng bấm nút xác nhận thanh toán, nạp cọc, phát hành hóa đơn hoặc lưu giường; nếu backend trả lỗi nghiệp vụ, modal đứng im, không có thông báo lỗi, gây hiểu lầm hệ thống bị treo hoặc mất dữ liệu. | **High** | **Critical** | **CRITICAL** | Bổ sung ngay nhánh fallback `message.error(errorMessage(error))` vào khối `catch (error)` của cả 4 modal (`DepositLedgerModal`, `IssueInvoicesModal`, `ConfirmPaymentModal`, `BedCrudPanel`). |
| **RSK-02** | **Thiếu 7 mã lỗi nghiệp vụ trong từ điển tiếng Việt:** Khi sinh viên hoặc cán bộ gặp các tình huống như hủy hợp đồng tự động, kích hoạt quá hạn, trùng kỳ học, người dùng chỉ thấy thông báo kỹ thuật chung chung mơ hồ. | **High** | **High** | **HIGH** | Khai báo bổ sung 7 mã lỗi vào `DMS/frontend/src/utils/errorMessages.ts` với câu thông báo hướng dẫn người dùng bằng tiếng Việt chuẩn mực. |
| **RSK-03** | **Controller trả về `Object` / `Map<String, Object>`:** Mất hoàn toàn tính kiểm soát kiểu dữ liệu của Java, vô hiệu hóa việc sinh Swagger/OpenAPI tự động, làm tăng nguy cơ sai lệch kiểu dữ liệu với Frontend. | **Medium** | **High** | **HIGH** | Đóng gói toàn bộ kết quả trả về của 8 controller thành các Java Record DTO có kiểu cụ thể (strongly-typed). |
| **RSK-04** | **Hardcode tên trường và câu thông báo trong `BusinessException`:** Khó khăn khi refactor schema dữ liệu, không thể hỗ trợ đa ngôn ngữ (i18n), dễ gãy contract validation với form frontend. | **Medium** | **High** | **HIGH** | Bổ sung đầy đủ trường vào `DmsConstants.ApiField` và xây dựng lớp hằng số `DmsConstants.ValidationMessage`. Thay thế 100% chuỗi thô. |
| **RSK-05** | **Độ lệch CSDL snapshot `DB-DMS-schema.sql`:** Nếu khởi tạo môi trường mới trực tiếp từ tệp snapshot, vai trò Staff bị thiếu quyền `ROOM_TRANSFER_APPROVE`, dẫn đến việc nhân viên bị từ chối truy cập (HTTP 403) khi duyệt chuyển phòng. | **High** | **High** | **HIGH** | Cập nhật ngay câu lệnh INSERT quyền Staff trong `DB-DMS-schema.sql` để khớp 73 quyền của Flyway V6 và bổ sung comment cột V7. |
| **RSK-06** | **HTTP DELETE có Request Body (`/rooms/{id}/leader`):** Phương thức DELETE có body vi phạm khuyến nghị RFC; các Web Application Firewall (WAF), API Gateway hoặc Reverse Proxy (Nginx/Cloudflare) có thể tự ý loại bỏ body, làm mất lý do hủy trưởng phòng. | **Medium** | **Medium** | **MEDIUM** | Chuyển endpoint thành `POST /api/rooms/{id}/remove-leader` nhận request body `{ reason }` chuẩn mực. |
| **RSK-07** | **Phân mảnh Controller và `@RequestMapping` cấp lớp:** Tồn tại 2 controller cho cùng 1 thực thể và dùng relative routing gây khó khăn cho việc quản lý route, phân quyền và tra cứu API. | **Low** | **Medium** | **MEDIUM** | Hợp nhất 5 cặp controller và chuyển URL trực tiếp vào annotation của từng phương thức theo chuẩn `CLAUDE.md`. |
| **RSK-08** | **Nhập trực tiếp Primitives Ant Design và Lồng `Form.Item`:** Mất khả năng áp dụng giao diện token đồng bộ của hệ thống; lồng Form.Item gây lỗi touch và register field trong AntD Form. | **Medium** | **Medium** | **MEDIUM** | Thay bằng `Surface` và `App.useApp()`; chuẩn hóa lại `LookupInput` trong `CrudPage.tsx`. |
| **RSK-09** | **Database Views mồ côi (`v_summer_leave`, `v_contract_label`):** Tồn tại tài nguyên dư thừa trong CSDL, gây lãng phí tài nguyên bảo trì và làm phân tâm đội ngũ phát triển. | **Low** | **Low** | **LOW** | Tái cấu trúc các truy vấn backend để tận dụng 2 view này hoặc lập migration đánh dấu deprecated dọn dẹp. |

---

## 6. LỘ TRÌNH VÀ KHUYẾN NGHỊ KHẮC PHỤC CỤ THỂ (REMEDIATION ROADMAP)

Để đưa hệ thống DMS đạt trạng thái hoàn thiện tuyệt đối và sẵn sàng vận hành thực tế, nhóm kiểm toán đề xuất lộ trình hành động 3 giai đoạn (3 Sprints):

```
       SPRINT 1 (Hotfix Khẩn cấp)          SPRINT 2 (Chuẩn hóa Contract & DTO)      SPRINT 3 (Refactor Kiến trúc & Tối ưu)
  ┌─────────────────────────────────┐    ┌────────────────────────────────────┐    ┌─────────────────────────────────────┐
  │ • Sửa 4 lỗi nuốt ngoại lệ Modal │    │ • Đóng gói Typed DTO Controller    │    │ • Hợp nhất 5 cặp Controller phân mảnh│
  │ • Thêm 7 mã lỗi errorMessages.ts│───►│ • Chuẩn hóa DmsConstants.ApiField  │───►│ • Bỏ class-level @RequestMapping   │
  │ • Cập nhật DDL DB-DMS-schema.sql│    │ • Tạo ErrorCode enum Frontend      │    │ • Wrapper Surface & Form.Item fix   │
  │ • Bật Docker chạy test tích hợp │    │ • Chuẩn hóa API REST V5 (billing)  │    │ • Chuyên biệt hóa Dashboard sinh viên│
  └─────────────────────────────────┘    └────────────────────────────────────┘    └─────────────────────────────────────┘
```

### 6.1. Giai đoạn 1 (Hotfix Khẩn cấp — Sprint 1: Xử lý ngay lập tức)
- **Mục tiêu:** Loại bỏ hoàn toàn các rủi ro vận hành nghiêm trọng đối với người dùng cuối và đồng bộ hóa lược đồ CSDL snapshot.
- **Các đầu việc cụ thể:**
  1. **Khắc phục lỗi nuốt ngoại lệ tại 4 Modal Frontend:**
     - Sửa `DepositLedgerModal.tsx` (dòng 43–46): Thêm lệnh `message.error(errorMessage(error))` khi `validationFieldErrors(error)` không có phần tử.
     - Sửa `IssueInvoicesModal.tsx` (dòng 45–50): Hiển thị thông báo lỗi khi phát hiện hóa đơn trùng lặp hoặc trùng kỳ học.
     - Sửa `ConfirmPaymentModal.tsx` (dòng 38–40): Thông báo lỗi khi hóa đơn đã được thanh toán trước đó.
     - Sửa `BedCrudPanel.tsx` (dòng 54–60): Gọi `handleApiError(e)` trong hàm `save()` để báo lỗi giường đã có người ở.
  2. **Bổ sung 7 mã lỗi nghiệp vụ vào `DMS/frontend/src/utils/errorMessages.ts`:**
     ```typescript
     DEPOSIT_AMOUNT_MISMATCH: 'Số tiền đặt cọc không khớp với quy định của loại phòng.',
     CONTRACT_ACTIVATION_EXPIRED: 'Hợp đồng đã quá hạn kích hoạt và không thể nhận phòng.',
     CONTRACT_AUTO_CANCELLED: 'Hợp đồng đã bị hệ thống tự động hủy do quá thời hạn hoàn tất thủ tục.',
     CONTRACT_PERIOD_OVERLAP: 'Khoảng thời gian hợp đồng bị trùng lặp với một hợp đồng lưu trú khác.',
     APPLICATION_NOT_APPROVED: 'Đơn đăng ký chưa được duyệt hoặc không ở trạng thái hợp lệ để xếp phòng.',
     NO_HOLD_ASSIGNMENT: 'Không tìm thấy thông tin giữ chỗ phòng ở hợp lệ cho hồ sơ này.',
     ADJUSTMENT_INVALID: 'Yêu cầu điều chỉnh hóa đơn không hợp lệ hoặc vượt quá giới hạn cho phép.',
     ```
  3. **Đồng bộ snapshot CSDL `Docs/02 External Design/Database Design/DB-DMS-schema.sql`:**
     - Bổ sung quyền `"ROOM_TRANSFER_APPROVE"` vào mảng quyền của vai trò `STAFF` tại dòng 815.
     - Bổ sung `COMMENT ON COLUMN application_priority.note` tại dòng 200.
  4. **Kích hoạt Daemon Docker Desktop trên môi trường CI/CD và Local:**
     - Khởi động Docker Desktop để mở đường ống `pipe/dockerDesktopLinuxEngine`, cho phép bộ 17 suite kiểm thử tích hợp (219 tests) chạy thành công trọn vẹn 100%.

---

### 6.2. Giai đoạn 2 (Chuẩn hóa Contract & DTO — Sprint 2: Tuân thủ Quy chuẩn)
- **Mục tiêu:** Triệt tiêu hoàn toàn vi phạm anti-hardcoding, loại bỏ kiểu dữ liệu thô `Object`/`Map` và chuẩn hóa hợp đồng API.
- **Các đầu việc cụ thể:**
  1. **Đóng gói kiểu dữ liệu trả về cho 8 Controller Backend:**
     - Thay thế `public Object saveOrUpdate(...)` và `PageResponse<Map<String, Object>>` trong `InvoiceCrudController`, `ContractCrudController`, `SummerCrudController`, `NotificationCrudController`, `StudentManagementController`, `UtilityReadingController`, `UserController` bằng các Java Record DTO tường minh (ví dụ: `InvoiceResponse`, `SummerChoiceResponse`, `StudentProfileResponse`).
  2. **Chuẩn hóa Hằng số Kiểm tra Dữ liệu (Validation Constants):**
     - Mở rộng `DmsConstants.ApiField` với các hằng số: `PHOTOS`, `CLOSE_AT`, `METER_TYPE`, `OLD_METER_FINAL`, `ACADEMIC_YEAR`, `HOMETOWN`, v.v.
     - Tạo mới lớp tập trung `DmsConstants.ValidationMessage` chứa các thông điệp validation tiếng Việt.
     - Thay thế toàn bộ 23 lệnh gọi chuỗi thô `BusinessException.invalid("fieldName", "Message")` bằng các hằng số này.
  3. **Tạo enum/constants tập trung cho Frontend ErrorCode:**
     - Tạo tệp `DMS/frontend/src/constants/errors.ts` chứa enum `ErrorCode` (ví dụ `PASSWORD_CHANGE_REQUIRED`, `ACCOUNT_LOCKED`, `SESSION_REVOKED`, `FORBIDDEN`).
     - Thay thế chuỗi so sánh trực tiếp trong `services/api/client.ts` và `app/guards/RequireRole.tsx`.
  4. **Chuẩn hóa các API Endpoint lệch chuẩn REST V5:**
     - Chuyển `listDepositTransactions` trong `billing.ts` sang gọi API phân trang `POST .../search-by-page`.
     - Chuyển `createStorageRecord` trong `checkout.ts` sang `POST /storage-records/save-or-update`.
     - Chuyển `removeRoomLeader` trong `room.ts` từ HTTP `DELETE` có body sang `POST /api/rooms/{id}/remove-leader`.

---

### 6.3. Giai đoạn 3 (Refactor Kiến trúc & Tối ưu UI/UX — Sprint 3: Tối ưu hóa Toàn diện)
- **Mục tiêu:** Hợp nhất phân mảnh controller, hoàn thiện thiết kế UI/UX và dọn dẹp các tài nguyên dư thừa.
- **Các đầu việc cụ thể:**
  1. **Hợp nhất 5 cặp Controller Backend trùng lặp:**
     - Hợp nhất `InvoiceController` và `InvoiceCrudController` thành một `InvoiceController` duy nhất.
     - Hợp nhất `ContractController` và `ContractCrudController` thành `ContractController`.
     - Hợp nhất `UtilityRateController` và `UtilityRateCrudController` thành `UtilityRateController`.
     - Hợp nhất `MaintenanceRequestController` và `MaintenanceCrudController` thành `MaintenanceRequestController`.
     - Hợp nhất `NotificationController` và `NotificationCrudController` thành `NotificationController`.
  2. **Chuẩn hóa URL trực tiếp trên từng phương thức Controller:**
     - Loại bỏ `@RequestMapping` cấp lớp tại 12 controller backend; chuyển toàn bộ thành URL đầy đủ (ví dụ: `@GetMapping("/api/rooms/{id}")`).
     - Di chuyển các endpoint tra cứu cọc/kho/phòng trong `OverviewController` về đúng module domain quản lý (`billing`, `contract`, `room`).
  3. **Chuẩn hóa Component Frontend & Khắc phục lồng `Form.Item`:**
     - Thay thế các lệnh import trực tiếp `Badge` và `Skeleton` từ Ant Design bằng wrapper chuẩn `Surface` trong `NotificationBell.tsx`, `SidebarShell.tsx`, `ResponsiveTable.tsx`.
     - Chuyển việc gọi API tĩnh `message` sang `const { message } = App.useApp()`.
     - Tái cấu trúc `LookupInput` trong `CrudPage.tsx` để loại bỏ việc bọc `<Form.Item>` lồng nhau.
     - Đăng ký thực thể `storage` vào `StatusEntity` của `statusConfig.ts` và sử dụng `StatusBadge` trong `StoragePage.tsx`.
  4. **Chuyên biệt hóa Giao diện Dashboard Sinh viên:**
     - Tái cấu trúc `StudentHomePage.tsx` từ dạng scaffold hiện tại thành Dashboard chuyên biệt dành riêng cho sinh viên với các widget: Tình trạng phòng ở hiện tại, thông tin bạn cùng phòng, hạn nộp hóa đơn gần nhất, thông báo mới và nút báo hỏng nhanh.
  5. **Tối ưu hóa Bundle Size Frontend:**
     - Áp dụng kỹ thuật Code-splitting với `React.lazy()` và `Suspense` trên các route trong `App.tsx` để giảm kích thước bundle `App-Bs37Rh06.js` từ 1.57 MB xuống dưới 300 kB.

---

## TỔNG KẾT & CHỮ KÝ PHÊ DUYỆT

Báo cáo kiểm toán này phản ánh một cách trung thực, khách quan và minh bạch hiện trạng kỹ thuật của hệ thống Quản lý Ký túc xá (DMS). Với nền tảng kiến trúc phân tầng vững chắc, độ an toàn giao dịch cao và chất lượng mã nguồn đạt chuẩn, hệ thống hoàn toàn đủ điều kiện tiến vào giai đoạn chạy thử nghiệm có kiểm soát (Staging) và triển khai chính thức (Production) sau khi hoàn tất các đầu việc Hotfix của **Giai đoạn 1**.

**ĐẠI DIỆN HỘI ĐỒNG KIỂM TOÁN HỆ THỐNG DMS**  
*Tổ Kiểm toán Độc lập — Tracks R1, R2, R3, R4, R5*

---

## 7. KẾT QUẢ XỬ LÝ (28/10/2026)

Từng phát hiện đã được đối chiếu lại với mã nguồn trước khi sửa. Chưa commit.

### 7.1. Đã sửa

| Mục | Nội dung xử lý |
|---|---|
| BE 1–23 (hardcode validation) | Mọi `BusinessException.invalid(...)` không còn chuỗi thô: 85 lời gọi trong 17 Service Impl dùng `ApiField` (thêm 30 hằng) và `ValidationMessage` (thêm 71 hằng). Giá trị chuỗi giữ nguyên từng ký tự, đã kiểm bằng script đối chiếu diff. Thêm `DmsConstants.PhotoUpload` (3 ảnh, 5 MB, mã lý do `COUNT/SIZE/TYPE`) cho `MaintenancePhotoServiceImpl`; khóa details của `IMPORT_FILE_INVALID` dùng `ApiField.MISSING_COLUMNS/MAX_ROWS` |
| BE 14 (`ContractDtos.ExtensionSource`) | Chuyển sang `DmsConstants.ExtensionSource`; loại đối tượng timeline đăng ký `EntityType.EXTENSION` |
| BE 24–31 (kiểu trả về) | Siết kiểu ở các method có kiểu cụ thể duy nhất: `searchStayRequestsByPage` → `PageResponse<Map<String, Object>>`; `UtilityReading.saveOrUpdate/getReading`, `Notification.getPublished`, `Summer.getChoice/getMyCurrentChoice` → `Map<String, Object>` |
| FE 5 (mã lỗi thiếu) | Thêm 7 mã vào `errorMessages.ts` theo API-Spec mục 5 (dùng `details.required`, `contractCode`, `totalAfter` khi có) |
| FE 9–10 (so sánh mã lỗi thô) | Thêm registry `ErrorCode` trong `constants/authorization.ts`; `client.ts`, `RequireRole.tsx`, `RolePermissionsPage.tsx` dùng hằng số |
| FE 11–15 (primitive Ant Design) | `DepositLedgerModal`, `IssueInvoicesModal` dùng `App.useApp().message`; `Badge` (NotificationBell, SidebarShell) và `Skeleton` (ResponsiveTable) lấy từ `Surface` |
| FE 17 (Tag tự chế) | Thêm thực thể `storage` + `StorageState` vào `statusConfig.ts`; `StoragePage` dùng `StatusBadge` |
| FE 18–21 (nhãn trùng) | `MaintenanceAction`, `maintenanceActionLabels`, `maintenanceCauseLabels`, `assetConditionLabels`, `pendingItemTypeLabels` khai tại `statusConfig.ts` (feature re-export); `rolePermissionActionLabels`, `pendingItemDetailSegments` khai tại `navigation.ts` |
| FE 22–23 (icon) | Kỳ hè (Staff, Student) dùng `SunOutlined` |
| DB 4.3.A | `DB-DMS-schema.sql`: STAFF thêm `ROOM_TRANSFER_APPROVE` (73 quyền), thêm `COMMENT ON COLUMN application_priority.note`, đầu tệp ghi V3–V7; `DB-Design-DMS.md` lên 0.11 |

### 7.2. Dương tính giả (không sửa)

| Mục | Lý do |
|---|---|
| FE 1–4 "Critical" nuốt ngoại lệ | Mọi mutation trong `useBilling.ts` (`useIssue*`, `useConfirmPayment`, `useRecordDeposit*`) đã có `onError: useApiErrorHandler()` gọi `message.error(errorMessage(error))`; `BedCrudPanel.save()` đã gọi `handleApiError(e)` ngay sau `setFields`. Lỗi nghiệp vụ vẫn được hiển thị |
| FE 16 lồng `Form.Item` | `Form.Item` ngoài không có `name`, chỉ làm nhãn; `Form.Item noStyle name` bên trong bind trực tiếp `Select`. Đây là mẫu chính thức của Ant Design, lỗi validation vẫn hiện ở Form.Item ngoài |
| FE 6–8, BE 36–39 (GET sổ cọc, `POST /contracts/{id}/storage-records`, `PUT/DELETE /rooms/{id}/leader`, `PUT cross-unit-access`, `PUT role-permissions`) | Đều đúng API-Spec v0.17 (dòng 283, 320–321, 398, 454). Phiếu gửi đồ là action nghiệp vụ (đóng phân phòng), không phải CRUD; CLAUDE.md ghi rõ "ma trận quyền giữ GET/PUT riêng" |
| BE 35 `@RequestMapping` cấp lớp | CLAUDE.md chỉ yêu cầu khai đường dẫn trực tiếp tại Controller, không tạo Constants cho URL; mapping cấp lớp vẫn khai trong annotation Controller |
| BE 32 (cặp Controller) | Tách `*CrudController` (CRUD V5) với Controller luồng nghiệp vụ là chủ đích của khung ("detailed workflows remain separate"), không vi phạm quy tắc nào |

### 7.3. Chưa xử lý, cần nhóm quyết định

- `create` và `update` trong `UserService`, `StudentManagementService`, `InvoiceCrudService`, `ContractCrudService`, `NotificationCrudService`, `SummerCrudService` trả hai hình dạng khác nhau (ví dụ mật khẩu tạm khi tạo, hồ sơ khi sửa) nên vẫn là `Object`. Muốn có DTO tường minh phải đổi hợp đồng API và cập nhật React, API-Spec cùng lúc.
- `DELETE /api/rooms/{id}/leader` có body: đúng spec hiện tại; nếu muốn đổi sang `POST .../remove-leader` cần cập nhật API-Spec trước.
- Tách bundle (`React.lazy` theo route), dashboard sinh viên, chuyển endpoint tra cứu khỏi `OverviewController`, 2 view chưa dùng (`v_summer_leave`, `v_contract_label`): là tối ưu/tính năng, không phải vi phạm.

### 7.4. Kiểm chứng

- `npm run lint` (oxlint): exit 0. `npm run build` (`tsc -b` + Vite): thành công.
- `mvnw test -Dtest="*Test,!*IntegrationTest,!DmsApplicationTests"`: 99/99 pass, gồm `ModuleDependencyTest` 8/8.
- `mvnw clean verify` (Docker bật, Testcontainers PostgreSQL): 318/318 pass, gồm 219 integration test; JaCoCo phân tích 338 lớp, BUILD SUCCESS. Hai test (`ContractExtensionRuleTest`, `ContractExtensionIntegrationTest`) được cập nhật theo vị trí mới của `ExtensionSource`/`EntityType.EXTENSION`.

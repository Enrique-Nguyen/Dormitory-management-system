# Báo cáo: Kiểm tra các nhánh Git và vấn đề còn lại

> Ngày kiểm tra: 17/11/2026
> Thực hiện: Qoder hỗ trợ TV4 Nguyễn Văn Quyền rà soát; không thay đổi tác giả các phần triển khai.
> Phạm vi: toàn bộ đầu nhánh local/remote; thay đổi mới của billing trên develop; đối chiếu registration/contract và maintenance.
> Căn cứ: CLAUDE.md; SRS-DMS.md FR-MNT-07, FR-NOT-04; lịch sử và mã nguồn Git sau fetch origin.
> Trạng thái: Đã có kết quả kiểm chứng cục bộ; chưa nghiệm thu tích hợp/E2E và chưa xác nhận CI.

---

## 1. Mục tiêu

Xác định nhánh nào đã/chưa hợp nhất, công việc còn nằm ngoài nhánh và các lỗi có bằng chứng. Chỉ đọc mã, fetch thông tin remote và chạy kiểm chứng an toàn; không checkout, merge, apply stash, commit, push, chạy migration hoặc ghi CSDL.

Điểm chốt để đối chiếu:

- `origin/develop`: `14e76d95970355e91f897422485a34c3bccc8462`.
- Nhánh hiện tại `develop-quyennnv`: `6f30f03e05765edf072255f488bb2d90a52e1602`.
- `origin/main`: `1cc640de40ef4f8d520f032b33484791d78f5c40`.
- Remote đã được xác nhận bằng cả `git fetch origin` và `git ls-remote --heads origin`.

## 2. Kết quả

### 2.1. Quan hệ các nhánh remote

Các số sau là commit Git theo quan hệ tổ tiên so với `origin/develop`, không phải số tính năng hay số lỗi.

| Nhánh remote | SHA | Chậm hơn develop | Commit riêng | Kết luận |
|---|---|---:|---:|---|
| `develop` | `14e76d9` | 0 | 0 | Mốc tích hợp mới nhất |
| `dev-locnt` | `14e76d9` | 0 | 0 | Trùng đầu nhánh develop; không còn mã riêng chờ nhập |
| `develop-quyennnv` | `6f30f03` | 12 | 0 | Đã nhập; chưa nhận 12 commit mới từ develop |
| `main` | `1cc640d` | 49 | 0 | Là tổ tiên develop; chưa nhận các thay đổi mới |
| `dev-secnv` | `266f588` | 41 | 0 | Đã nằm trong develop |
| `feature/shared-tv3-completion` | `4410d31` | 58 | 0 | Đã nhập; trùng SHA với nhánh TV4 |
| `feature/shared-tv4-completion` | `4410d31` | 58 | 0 | Đã nhập; trùng SHA với nhánh TV3 |
| `fix/registration-xu-ly-review` | `880aeec` | 17 | 0 | Đã nằm trong develop |
| `refactor/frontend-core-gop-xu-ly-nghiep-vu` | `ab65b4f` | 20 | 0 | Đã nằm trong develop |
| `refactor/shared-audit-remediation` | `a1cc63a` | 47 | 0 | Đã nằm trong develop |
| `docs/registration-deep-review` | `9efe598` | 0 | 2 | Nhánh duy nhất có commit chưa nằm trong develop; chỉ sửa tài liệu |

Hai commit chưa nhập: `1a71484` báo cáo review chuyên sâu đăng ký KTX và `9efe598` hướng dẫn sự cố checksum Flyway V10. Chúng chỉ bổ sung hai tệp, không có code fix mới.

`main` và `develop` chênh lệch nội dung 217 tệp, 23.545 dòng thêm và 2.946 dòng xóa. Đây là độ lệch phiên bản, không tự chứng minh main bị lỗi hoặc phải phát hành ngay. Không cần nhập lại các nhánh đã là tổ tiên develop; chỉ cân nhắc dọn sau khi nhóm xác nhận.

### 2.2. Trạng thái local và công việc chưa vào commit

- Local `develop` ở `619ea86`, chậm `origin/develop` 27 commit, không có commit riêng.
- Local `develop-quyennnv` khớp nhánh cùng tên trên origin, nhưng chậm develop 12 commit.
- Local `main` khớp `origin/main`; hai nhánh feature TV3/TV4 local khớp remote tương ứng.
- Một tệp có thay đổi chưa commit: [CreateTicketModal.tsx:266](file:///D:/Dormitory-management-system/DMS/frontend/src/features/maintenance/CreateTicketModal.tsx#L266), thêm 10 dòng thuộc cấu hình popup cho các ô chọn. Thay đổi được giữ nguyên, không coi là lỗi.
- Có `stash@{0}` ở `8aef7ed`, ngày 31/10/2026, tên `qoder-preserve-maintenance-before-quyennnv-20261031`. Stash gồm 4 tệp, 126 dòng thêm, 5 dòng xóa: hai service maintenance và hai integration test. Stash không được áp dụng hay xóa.
- Nội dung stash có đề xuất sửa đúng hai vấn đề bảo trì bên dưới, nhưng không thể coi bản vá đã được kiểm thử hoặc đã vào develop.

### 2.3. Lỗi đã xác minh trong mã bảo trì

#### MNT-01 — P2: Staff được nghiệm thu thay sau khi sinh viên chuyển phòng, dù chưa trả phòng

- Vị trí: [MaintenanceRequestServiceImpl.java:706–713](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/maintenance/service/impl/MaintenanceRequestServiceImpl.java#L706).
- Cơ chế: `staffCanAcceptRequest` coi phòng hiện tại khác phòng của ticket là đủ điều kiện Staff nghiệm thu thay. Nó không phân biệt chuyển phòng với đã kết thúc lưu trú.
- Tình huống: ticket của sinh viên đang RESOLVED ở phòng A; sinh viên chuyển sang phòng B; Staff cùng đơn vị lấy được cả `accept` và `reopen`. Theo SRS FR-MNT-07, điều kiện nghiệm thu thay là người yêu cầu đã trả phòng.
- Bằng chứng chạy trực tiếp bằng JShell + Mockito với các lớp đã biên dịch, không có DB: còn ở phòng A → `false`; chuyển sang B → `true`, actions `[accept, reopen]`; không còn phòng đang ở → `true`.
- Policy sử dụng kết quả này tại [MaintenanceRequestPolicy.java:79–83](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/maintenance/domain/MaintenanceRequestPolicy.java#L79). Service dùng cùng điều kiện trước khi thực hiện lệnh, nên không chỉ là nút hiển thị sai.
- Đã xác nhận các tệp liên quan không khác giữa HEAD và `origin/develop`, vì vậy lỗi còn trên develop mới nhất.
- Cần sửa: phân biệt người đã trả phòng với người chỉ chuyển phòng; thêm ca hồi quy ở tầng service/integration. Không áp dụng stash nguyên khối khi chưa đối chiếu.

#### MNT-02 — P2: Ticket tự đóng không tạo thông báo đổi trạng thái

- Vị trí: [MaintenanceLifecycleServiceImpl.java:47–75](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/maintenance/service/impl/MaintenanceLifecycleServiceImpl.java#L47).
- Cơ chế: job đổi RESOLVED → CLOSED bằng SQL, ghi lịch sử và trả `Change` cho audit, nhưng không phát `TicketStatusChangedEvent`. Luồng đổi thủ công có phát sự kiện tại [MaintenanceRequestServiceImpl.java:716–725](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/maintenance/service/impl/MaintenanceRequestServiceImpl.java#L716).
- Hậu quả: người tạo phiếu và kỹ thuật viên không nhận thông báo ticket được tự đóng theo FR-NOT-04. Đây là yêu cầu mức Should trong SRS, không phải bằng chứng job không đóng được ticket.
- Bằng chứng: đọc toàn bộ service, job runner và listener; JShell + Mockito cho thấy nhánh tự đóng thực hiện 2 lệnh JDBC và 1 lần ghi lịch sử, trả thay đổi CLOSED, không có dependency/sự kiện thông báo. Listener chỉ nhận qua [MaintenanceNotificationListener.java:41–66](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/notification/service/impl/MaintenanceNotificationListener.java#L41).
- Test hiện có tại [SystemJobsIntegrationTest.java:820–848](file:///D:/Dormitory-management-system/DMS/backend/src/test/java/vn/edu/tlu/dms/SystemJobsIntegrationTest.java#L820) kiểm trạng thái/lịch sử/idempotence, chưa kiểm thông báo của nhánh tự đóng.
- Đã xác nhận mã này không khác giữa HEAD và `origin/develop`.
- Cần sửa: phát sự kiện trong transaction đóng ticket và kiểm thông báo sau commit, đồng thời giữ tính idempotent.

### 2.4. Lỗi còn lại của đăng ký / yêu cầu lưu trú

Tổng cộng đợt kiểm tra xác định **8 vấn đề trong mã/giao diện: 1 P1 và 7 P2**, gồm 6 mục dưới đây và 2 mục bảo trì phía trên. Phân biệt rõ việc đã chạy harness trong bộ nhớ với kết luận đọc mã; chưa có tái hiện UI hoặc DB thật.

#### REG-01 — P1: STAFF không thể duyệt/từ chối lựa chọn hè và Admin không thể cấp lại quyền

- Vị trí: [ScopeGuard.java:164–174](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/common/security/ScopeGuard.java#L164) loại `SUMMER_REGISTRATION_UPDATE` khỏi tập quyền hợp lệ của STAFF; [SummerCrudController.java:80–89](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/registration/api/SummerCrudController.java#L80) bắt buộc đúng quyền đó cho approve/reject.
- [RolePermissionServiceImpl.java:191–206](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/common/service/impl/RolePermissionServiceImpl.java#L191) từ chối cấp quyền ngoài eligibility. Không thể giải quyết chỉ bằng thao tác cấp quyền trên giao diện quản trị.
- Bằng chứng chạy JShell với lớp đã biên dịch: `STAFF_SUMMER_UPDATE_ELIGIBLE=false`; reflection xác nhận cả hai controller method yêu cầu quyền này; gọi validator bằng reflection với quyền đọc hợp lệ và quyền UPDATE hè trả `BusinessException: VALIDATION_FAILED`.
- Hậu quả dự đoán từ authorization chain: STAFF đúng đơn vị vẫn bị 403 khi duyệt/từ chối. Đây là mâu thuẫn với vai trò Staff trong API/SRS, không chỉ thiếu seed. ADMIN không chịu hạn chế eligibility của STAFF.
- Test hè hiện có mock quyền nên không phát hiện được lỗi này; cần regression qua phân quyền thật, không mock guard/matrix.

#### CON-01 — P2: STAFF sửa được nội dung yêu cầu gia hạn/chuyển phòng của sinh viên

- Vị trí: [ContractCrudServiceImpl.java:712–739](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/contract/service/impl/ContractCrudServiceImpl.java#L712).
- Nhánh sửa dùng cùng quyền UPDATE dành cho xét duyệt. `loadPendingRequest` nhận STAFF; kiểm tra hợp đồng chỉ buộc đúng đơn vị, không buộc người sở hữu. Vì vậy STAFF có quyền xét duyệt có đường đi sửa `reason` hoặc `newEndDate` của yêu cầu PENDING.
- Đặc tả tại [API-Spec-DMS.md:94](file:///D:/Dormitory-management-system/Docs/02%20External%20Design/API%20Specification/API-Spec-DMS.md#L94) ghi sinh viên sở hữu CRUD yêu cầu PENDING, Staff chỉ đọc qua cổng CRUD; action xét duyệt là riêng.
- Xác minh bằng đọc đầy đủ nhánh save/update/load và helper kiểm tra hợp đồng; chưa gọi API/ghi DB. Cần tách quyền xử lý action khỏi quyền viết lại yêu cầu của sinh viên.

#### REG-02 — P2: Sửa/gửi lại lựa chọn hè không kiểm tra hợp đồng còn ACTIVE

- Vị trí: [SummerCrudServiceImpl.java:415–427](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/registration/service/impl/SummerCrudServiceImpl.java#L415) và [loadEditable:600–606](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/registration/service/impl/SummerCrudServiceImpl.java#L600).
- Nhánh tạo kiểm ACTIVE, còn nhánh sửa chỉ kiểm người sở hữu, PENDING/REJECTED và đợt mở. `lockChoice` đọc trạng thái hợp đồng để đính kèm nhưng không từ chối hợp đồng đã kết thúc.
- Tình huống đọc mã: có lựa chọn PENDING/REJECTED → hợp đồng thành TERMINATED/EXPIRED → đợt hè còn mở → gửi sửa bằng ID vẫn thành công, thậm chí REJECTED mở lại PENDING. Trái điều kiện ACTIVE ở API mục đăng ký hè.
- Chưa tái hiện bằng DB; cần kiểm trạng thái hợp đồng nguồn ở nhánh sửa/gửi lại.

#### REG-03 — P2: Hộp việc chờ mất bản ghi ở trang sâu nhưng vẫn báo tổng đầy đủ

- Vị trí: [PendingItemServiceImpl.java:59](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/registration/service/impl/PendingItemServiceImpl.java#L59), [104–120](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/registration/service/impl/PendingItemServiceImpl.java#L104).
- Service chỉ quét tối đa 500 bản ghi mỗi nguồn nhưng trả tổng thật và cho phân trang vượt cửa sổ đã quét.
- Đã chạy chính service bằng JShell/Mockito với nguồn 600 đơn SUBMITTED, `size=20`: page 24 trả 20 dòng; page 25 và page 29 trả 0 dòng, trong khi cả ba vẫn `totalElements=600`, `totalPages=30`. Đây là tái hiện tầng service, không phải UI/DB.
- Ngoài ra [dòng 219–225](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/registration/service/impl/PendingItemServiceImpl.java#L219) gán timestamp hợp đồng bằng null, làm thứ tự trộn không theo thời điểm tạo hợp đồng. Cần sửa cùng cơ chế phân trang/gộp, không chỉ thay số 500 tùy ý.

#### REG-04 — P2: UI chưa có cách nhập/sửa chỉ tiêu đăng ký theo tòa

- Vị trí HEAD: [resources.ts:751–761](file:///D:/Dormitory-management-system/DMS/frontend/src/features/crud/resources.ts#L751); trên `origin/develop@14e76d9` là dòng 780–790.
- Form chỉ có học kỳ, loại, tên, thời gian và đơn vị; không có `quotas`. Tìm toàn bộ frontend không có `quotas`, `quota` hoặc editor chỉ tiêu khác.
- Backend đã có dữ liệu chỉ tiêu, nhưng Staff không thiết lập được bằng UI theo FR-REG-01. Đợt đã mở còn không cho sửa quotas nên không thể để sau mới bổ sung bằng form hiện tại.
- Xác minh cấu hình form/mã frontend, chưa mở UI. Đây là phần chức năng thiếu, không khẳng định chỉ tiêu là trần giao dịch khi duyệt/phân giường.

#### REG-05 — P2: Xóa đợt dùng trạng thái đọc trước khóa

- Vị trí: [RegistrationPeriodServiceImpl.java:272–276](file:///D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/registration/service/impl/RegistrationPeriodServiceImpl.java#L272).
- Entity được nạp bằng `findById` trước `FOR UPDATE`; sau khi lấy khóa không refresh/đọc lại `openAt`.
- Interleaving từ mã: T1 khóa đợt tương lai, đổi `openAt` sang hiện tại; T2 đọc phiên bản cũ rồi chờ khóa; T1 commit; T2 lấy khóa nhưng kiểm entity cũ và có thể xóa đợt vừa mở nếu chưa có đơn. Nhánh sửa đợt cho phép đổi thời gian khi đợt còn chưa mở.
- Đây là lỗi kiểm tra lại sau khóa; chưa có test concurrency chạy trên DB trong phiên này, không kết luận đã quan sát deadlock.

### 2.5. Rủi ro V10 và các điểm chưa nên kết luận thành lỗi

- Git xác nhận [V10__shared_catalogs.sql](file:///D:/Dormitory-management-system/DMS/backend/src/main/resources/db/migration/V10__shared_catalogs.sql) được tạo ở `b1c58b8`, sau đó bị sửa ngay trong `1436022`: bỏ khối chuẩn hóa năm học và nới CHECK. Nếu một DB đã áp dụng bản V10 cũ, Flyway validate có nguy cơ chặn backend vì checksum thay đổi. Đây là **rủi ro triển khai có điều kiện**, không phải khẳng định DB local hiện đang mismatch.
- Hướng dẫn sự cố V10 đang ở hai commit docs chưa nhập. Không dùng `repair`, sửa `flyway_schema_history`, xóa DB hoặc sửa tiếp migration để làm mất triệu chứng. Cần đối chiếu bản đã áp dụng và backup trước khi chốt cách xử lý.
- Backend chưa dùng quota để chặn vượt chỉ tiêu, nhưng đặc tả chưa chốt đó là giới hạn giao dịch hay chỉ tiêu kế hoạch; tách thành quyết định nghiệp vụ, không gán lỗi phân quyền.
- Xóa `preferredRoomTypeId` bằng null mâu thuẫn với quy ước partial update giữ null; cần contract xóa giá trị rõ ràng nếu nhóm muốn hỗ trợ.
- Quyền sửa đợt dùng chung của Staff tổng hợp có mâu thuẫn giữa bảng endpoint và phần chi tiết API. Chưa kết luận thành bug authorization khi spec chưa thống nhất.
- Các phát hiện cũ về thông báo duyệt/phân giường, sort điểm ưu tiên, lộ trọng số cho sinh viên, phân giường sai đơn vị và một số luồng hè đã có code sửa. Không sao chép nguyên báo cáo review cũ thành danh sách lỗi còn tồn tại.

### 2.6. Thay đổi billing mới

Đã rà soát tĩnh các thay đổi giữa `6f30f03` và `14e76d9`: nguồn tạo hóa đơn MANUAL/ISSUED, quyền sửa/xóa, khóa trước kiểm tra, metadata học kỳ, DTO/caller, migration V15 và integration test liên quan. Chưa xác minh được lỗi P1/P2 mới trong phạm vi diff này.

V15 là migration mới; không thấy sửa migration đã áp dụng trong khoảng so sánh billing mới. Việc đọc mã và unit test không chứng minh migration đã chạy đúng trên CSDL thật; phát hiện lịch sử V10 phía trên là một vấn đề độc lập.

### 2.7. Cảnh báo chất lượng chưa phải lỗi chặn build

- Lint còn 3 cảnh báo: regex ký tự điều khiển ở [excel.ts:38](file:///D:/Dormitory-management-system/DMS/frontend/src/features/crud/excel.ts#L38); cập nhật state trong effect ở [NotificationInboxPage.tsx:61](file:///D:/Dormitory-management-system/DMS/frontend/src/pages/NotificationInboxPage.tsx#L61); memoization ở [CrudPage.tsx:1254](file:///D:/Dormitory-management-system/DMS/frontend/src/pages/CrudPage.tsx#L1254) trên HEAD, tương ứng dòng 1267 trên develop mới.
- Build develop cảnh báo bundle lớn: App khoảng 1.650,91 kB minified / 503,57 kB gzip; ExcelJS khoảng 929,48 kB / 256,40 kB gzip. Chưa đo thời gian tải thực tế; không kết luận đây là lỗi hiệu năng đã tái hiện.
- Chưa có frontend test runner trong package scripts. Đây là khoảng trống kiểm chứng, không thay cho việc xác định lỗi nghiệp vụ cụ thể.

## 3. Kiểm tra

| Phạm vi | Kiểm chứng | Kết quả |
|---|---|---|
| Toàn bộ refs | Fetch, ls-remote, for-each-ref, rev-list, diff | Đã xác nhận 11 nhánh remote và quan hệ ở bảng trên |
| HEAD + thay đổi local hiện có | `npm --prefix DMS/frontend run lint` | 0 error, 3 warning |
| HEAD + thay đổi local hiện có | `npm --prefix DMS/frontend run build` | PASS; có cảnh báo chunk lớn |
| HEAD `6f30f03` | `mvnw.cmd test -Dtest=*Test,!*IntegrationTest,!DmsApplicationTests` | 212 test, 0 failure, 0 error, 0 skipped; gồm 8 test kiến trúc |
| Snapshot `origin/develop` `14e76d9` | Biên dịch sạch từ snapshot và cùng bộ Maven test | 212 test, 0 failure, 0 error, 0 skipped |
| Snapshot `origin/develop` | TypeScript build + Vite production build | PASS |
| Snapshot `origin/develop` | `oxlint src vite.config.ts` | 0 error, 3 warning |
| Bảo trì | JShell/Mockito chạy điều kiện nghiệm thu và tự đóng trong bộ nhớ | Xác nhận hai đường đi ở MNT-01/MNT-02; không phải E2E |
| Đăng ký hè | JShell kiểm eligibility, annotation controller và validator cấp quyền | STAFF không đủ quyền, cả hai action yêu cầu UPDATE, validator từ chối cấp |
| Hộp việc chờ | JShell/Mockito nguồn 600 đơn | Page 25/29 rỗng dù tổng 600 và 30 trang; page 24 có 20 dòng |
| Đăng ký/yêu cầu lưu trú | Đọc code, so với API/SRS mới, diff HEAD với develop | Sáu phát hiện ở mục 2.4; phần static được ghi rõ riêng |
| Giữ nguyên công việc | `git status --short --branch` sau kiểm thử | Giữ thay đổi ứng dụng ban đầu, chỉ thêm tài liệu báo cáo; không sửa mã ứng dụng |

Snapshot nằm tại `C:/Users/Admin/AppData/Local/Temp/dms-branch-review-20261117-EjhQXA`, được tạo bằng `git archive origin/develop`, không checkout hoặc tạo nhánh. Frontend sử dụng junction tới node_modules đã có, không cài package và không chạy `npm ci`; vì vậy đây không phải kiểm chứng cài dependency sạch từ lockfile. Lần lint mặc định đầu tiên trong snapshot bị quét cả dependency do snapshot không có Git ignore; đã chạy lại có phạm vi `src vite.config.ts` và chỉ sử dụng kết quả đúng phạm vi ở trên. Lần gọi Maven đầu tiên trong snapshot bị lỗi cú pháp shell trước khi chạy; đã sửa cách gọi và có BUILD SUCCESS khi chạy lại.

Bằng chứng thực thi lưu trong thư mục tạm của phiên:

- [Log 212 test trên HEAD](file:///C:/Users/Admin/AppData/Local/Temp/qoder-cli/D--MaNguonMo/0f800fcd-b088-4820-997a-f471dd147114/tasks/b3h6qd0yw.output#L424).
- [Log biên dịch và 212 test trên snapshot develop](file:///C:/Users/Admin/AppData/Local/Temp/qoder-cli/D--MaNguonMo/0f800fcd-b088-4820-997a-f471dd147114/tasks/b1ue7o1bo.output#L428).
- [Log frontend build trên develop](file:///C:/Users/Admin/AppData/Local/Temp/qoder-cli/D--MaNguonMo/0f800fcd-b088-4820-997a-f471dd147114/tasks/bd93mo9v3.output).
- [Log lint đúng phạm vi trên develop](file:///C:/Users/Admin/AppData/Local/Temp/qoder-cli/D--MaNguonMo/0f800fcd-b088-4820-997a-f471dd147114/tasks/b1khtyun7.output#L43).

Các log và snapshot ở thư mục tạm có thể bị môi trường dọn; báo cáo này ghi lại số liệu đã quan sát để không phụ thuộc tuổi thọ tệp tạm.

**Chưa kiểm tra:**

- Integration test/Testcontainers và `mvn verify` đầy đủ: Docker CLI không có trong PATH; cả pipe `docker_engine` và `dockerDesktopLinuxEngine` không tồn tại, không có DOCKER_HOST tùy chỉnh. Không tự bật/cài Docker.
- CSDL thật, checksum trong flyway_schema_history, áp dụng V15 hoặc repair V10: không truy cập/ghi DB.
- UI/E2E và thử bằng các phiên vai trò thật: không khởi động server hoặc thao tác trình duyệt trong đợt kiểm tra mã này.
- Trạng thái PR, approval và CI của GitHub: `gh` không có trong PATH. Lịch sử có merge commit không chứng minh CI đang xanh.
- Các nhánh cũ đã nằm trong develop không được build/test riêng từng đầu nhánh; việc kiểm chứng runtime tập trung HEAD và develop mới nhất.
- Không rà soát lại toàn bộ 217 tệp chênh lệch với main; không suy rộng kết quả thành toàn hệ thống không còn lỗi hoặc sẵn sàng production.

## 4. Điểm lệch so với thiết kế và quyết định cần xác nhận

| Nội dung | Kết luận / quyết định cần xác nhận | Lý do |
|---|---|---|
| Nghiệm thu thay sau chuyển phòng | Sửa theo FR-MNT-07 hoặc chốt lại SRS trước nếu nhóm muốn quyền rộng hơn | Hiện điều kiện code rộng hơn đã trả phòng |
| Thông báo tự đóng ticket | Bổ sung theo FR-NOT-04 (Should) nếu giữ chức năng thông báo đổi trạng thái hiện tại | Nhánh job bỏ qua event listener |
| Đồng bộ main | Chỉ thực hiện sau khi nhóm cho phép và CI/integration đạt trên đúng SHA | Main là nhánh ổn định, không mặc định merge ngay |
| Nhánh cũ và stash | Giữ nguyên, chưa xóa hay restore | Có thể vẫn cần cho đối chiếu hoặc công việc chưa hoàn tất |

## 5. Việc còn lại

| Ưu tiên | Việc | Phụ trách đề xuất |
|---|---|---|
| Trước khi làm tiếp | Bảo toàn thay đổi local rồi cập nhật nhánh làm việc từ develop theo quy trình nhóm | Người sở hữu nhánh |
| P1 | Sửa quyền Staff approve/reject hè theo đúng eligibility và kiểm authorization thật | TV3 + người phụ trách quyền dùng chung |
| Rủi ro triển khai | Đối chiếu V10 đã áp dụng trên từng DB trước khi nâng cấp; không repair mù | Người quản lý DB / nhóm phát triển |
| P2 | Chặn Staff viết lại nội dung yêu cầu; kiểm ACTIVE khi sửa/gửi lại hè | TV3 |
| P2 | Sửa phân trang hộp việc chờ và kiểm timestamp hợp đồng | TV3 |
| P2 | Bổ sung UI chỉ tiêu theo tòa và regression xóa đợt sau khóa | TV3 + frontend |
| P2 | Sửa MNT-01 và thêm test sinh viên chuyển phòng vẫn không cho Staff nghiệm thu thay | TV4 |
| P2 / Should | Sửa MNT-02 và kiểm thông báo sau auto-close, chạy lại lần hai không tạo trùng | TV4 |
| Kiểm chứng | Chuẩn bị Docker, chạy toàn bộ integration/verify trên develop đúng SHA | Nhóm phát triển |
| Kiểm chứng | Kiểm UI/E2E theo vai trò và migration trên DB test độc lập | Nhóm phát triển |
| Đồng bộ | Xem hai commit docs còn riêng, cập nhật báo cáo theo lỗi đã xác minh trước khi nhập | Người sở hữu registration / tài liệu |
| Phát hành | Xác nhận PR/CI và chọn thời điểm cập nhật main | Người duyệt / quản lý nhánh |
| P3 | Xử lý 3 lint warning và cân nhắc tách bundle dựa trên đo tải thực tế | Frontend |

## 6. Tệp liên quan

- Sửa mã ứng dụng: không có.
- Mới: báo cáo này; [bản báo cáo Canvas](file:///C:/Users/Admin/.qoder/projects/D--Dormitory-management-system/canvases/DMS-branch-review-2026-11-17.canvas.tsx) nằm ngoài repository. Không có chẩn đoán host Canvas được trả về khi ghi file; kiểm tra TypeScript thay thế với SDK hiện có trả 0 lỗi, chưa xác nhận preview render.
- Xóa hoặc chuyển mã nguồn: không có.
- Thay đổi local có sẵn và `stash@{0}`: giữ nguyên; không commit/push trong phiên kiểm tra.

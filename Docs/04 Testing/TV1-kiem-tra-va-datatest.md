# Kiểm tra TV1 và dữ liệu thử toàn màn

> Ngày: 21/10/2026. Thành viên: TV1 Nguyễn Văn Séc, cùng Codex.
> Căn cứ: SRS v0.12, SAD v1.5, API v0.15; phân công mục 13.1 trong đề xuất.
> Nhánh bàn giao: `dev-secnv` theo yêu cầu Nguyễn Văn Séc, nền `origin/develop` tại `a292f08`; triển khai ban đầu trên `feature/shared-tv1-completion`.

## Đối chiếu phần việc

| Yêu cầu | Trước khi kiểm tra | Kết quả triển khai và bằng chứng |
|---|---|---|
| FR-ACC-01: đăng nhập/đăng xuất | Có Keycloak OIDC + PKCE, JWT resource server | Giữ nguyên. `KeycloakJwtDecoderTest`, `KeycloakAccountIntegrationTest` kiểm issuer/audience/chữ ký, subject/role/version và thu hồi token. Kiểm thêm OIDC trên trình duyệt thật |
| FR-ACC-02: khóa tạm | Realm có bruteForceProtected, failureFactor=5, waitIncrement/maxFailureWait=900 | Giữ Keycloak làm nguồn chính. Không cài lại bộ đếm đăng nhập trong DMS; chưa đo thử chờ đủ 15 phút |
| FR-ACC-03: đổi mật khẩu | Keycloak có policy và UPDATE_PASSWORD | Giữ luồng Keycloak. Bổ sung che mật khẩu/secret trong diagnostic `toString`; JSON trả mật khẩu tạm một lần vẫn giữ hợp đồng |
| FR-ACC-04: hồ sơ | Có CRUD hồ sơ; Staff có thể đổi cả mã/họ tên/giới tính | Đã chặn ba trường định danh ở Service, ẩn khỏi form Staff; kiểm thử không ghi trái phép và vẫn sửa được liên hệ trong đơn vị |
| FR-ACC-05: Excel | Chưa có API/màn import; chỉ nhập từng hồ sơ | Có tải mẫu, preview mặc định dryRun=true, báo lỗi từng dòng, bỏ qua mã có sẵn, một transaction cho các dòng hợp lệ. Tài khoản Keycloak thật với mật khẩu ngẫu nhiên và bắt đổi. File tối đa 5 MB/5.000 dòng; giới hạn giải nén, từ chối công thức và giá trị sai. Có test rollback khi identity lỗi |
| FR-ACC-06: tài khoản cán bộ | Có tạo/sửa/khóa/mở/đổi role/đơn vị | Giữ cơ chế security_version và đồng bộ Keycloak; kiểm thử token cũ, trạng thái khóa và giới hạn vai trò/đơn vị |
| FR-ACC-07: reset | Có reset và scope BR-07 | Giữ quyền Admin; Staff cần đơn trong đợt mở hoặc phân phòng hiệu lực trong đơn vị. Kiểm thử trên PostgreSQL và bổ sung chuỗi kiểm tra tài khoản Keycloak thật |
| FR-ACC-08: đọc liên đơn vị | Có cờ crossUnitAccess | Kiểm read/write tách biệt: Staff tổng hợp được đọc thêm, ghi ngoài đơn vị vẫn 404 |
| FR-ACC-09: audit | Có append-only và bộ lọc, chưa xem trước/sau qua UI | Thêm GET chi tiết, trường oldValue/newValue và SYSTEM; không thêm endpoint sửa/xóa. Lỗi bảo mật có requestId |
| FR-ACC-10: quyền động | Có ma trận V5 và bổ sung quyền chuyển phòng V6 của develop | Giữ runtime grants từ CSDL, khóa/version/audit, ADMIN bất biến. Không sửa migration cũ hoặc thêm quyền ngoài registry |
| Khung xử lý lỗi | Có GlobalExceptionHandler và ánh xạ constraint/SQLState | Giữ cơ chế chung; bổ sung requestId cho 401/403 ở security filter chain, mã lỗi import và thông báo frontend |
| Khung @Scheduled | Chỉ bật EnableScheduling, chưa có job thực tế | Có SystemJobService: ID ứng viên, REQUIRES_NEW từng ID, khóa/kiểm tra lại, SYSTEM/audit, cô lập lỗi. Có bốn scheduler cấu hình qua môi trường |

## Bốn tác vụ SYSTEM

| Job | Lịch mặc định | Kiểm chứng |
|---|---|---|
| Hủy PENDING quá deadline | Mỗi giờ | Trả giường qua BedOccupancyService; khóa giường/sinh viên trước hợp đồng, hủy đơn bằng sự kiện đồng bộ; lỗi listener rollback toàn bộ; chạy lại không thêm lịch sử |
| Đóng RESOLVED quá hạn | Mỗi giờ | Đọc ConfigService, khóa rồi đọc lại; không kết luận nguyên nhân hay trừ cọc ngầm |
| Nhắc bù cọc quá hạn | Mỗi giờ | Tính FIFO, bỏ khoản đã bù, giữ khóa hợp đồng ACTIVE; thông báo/audit cùng transaction và không trùng |
| Nhắc hợp đồng sắp hết hạn | 08:00 hằng ngày | Ngưỡng từ ConfigService; chống trùng theo hợp đồng/ngày kết thúc; gia hạn có mốc mới; test đồng thời hai job |

Không tạo tài khoản/role SYSTEM có quyền Admin. `actor_id=NULL` là SYSTEM; ngữ cảnh xác thực request được dọn và khôi phục trong finally. Không mở API gọi job tùy ý. Chỉ hỗ trợ một instance backend như SAD; nhiều instance cần cơ chế lịch/khóa phân tán riêng.

## Kiểm thử tự động

`mvnw.cmd -B verify` trên JDK 17 và PostgreSQL Testcontainers: **152 test, 0 lỗi, 0 thất bại, 0 bỏ qua**. Có kiểm tra giữ khóa sinh viên cả khi hợp đồng chưa có HOLD. Frontend `npm run lint` và `npm run build` đạt. Build còn cảnh báo bundle lớn đã có trong ứng dụng.

| Phần mới | Độ phủ dòng JaCoCo |
|---|---:|
| StudentImportServiceImpl | 91,91% |
| SystemJobServiceImpl | 91,67% |
| ContractLifecycleServiceImpl | 97,50% |
| MaintenanceLifecycleServiceImpl | 100% |
| DepositReminderLookupImpl | 100% |
| SystemReminderServiceImpl | 94,74% |
| Toàn bộ package service.impl | **75,26%** |

Import có kiểm tra file hỏng/sai tiêu đề, mã trùng, điện thoại, ngày lịch không tồn tại, ngày Excel thật, đảo thứ tự/hoa thường tiêu đề, cột điện thoại tùy chọn, quá 5 MB/5.000 dòng, công thức, ZIP giải nén lớn, dryRun không ghi, ghi lại không tạo trùng, lỗi identity rollback dữ liệu/audit. SensitiveValuesTest bảo vệ log và hợp đồng JSON trả mật khẩu một lần.

## DataTest và màn hình

Seed tại `DMS/scripts/datatest/seed.cjs`; hướng dẫn tại README cùng thư mục. Có tài khoản thật cho Admin, Staff hai đơn vị, Staff đọc tổng hợp, KTV hai đơn vị, sinh viên đang ở/giữ chỗ/tạm rời/đã trả/chưa có hợp đồng, khóa và bắt đổi mật khẩu.

`verify.sql` có **21 kiểm tra đạt**: đủ danh mục/trạng thái chính, không trùng phân phòng mở, không sai giới tính, không chồng lịch sử giường/sinh viên, không ở phòng/giường ngừng hoạt động, hợp đồng đóng không còn phân phòng mở và tổng hóa đơn khớp chi tiết. Không ghi số dư/công suất/quá hạn suy diễn vào bảng.

`check-ui.cjs` đăng nhập OIDC và mở các route khai báo của bốn cổng, gồm Admin dùng lại màn Staff, các trang chi tiết và màn 360px. Cờ `--exercise-import` thử thêm import/rollback/cấp tài khoản/reset/thu hồi token trên dữ liệu giả. Kết quả từng route, ảnh chụp và tài khoản nằm ở thư mục kết quả local; không đưa mật khẩu/token vào Git hoặc báo cáo. Bản trên máy này: `D:\prjCDCNTT\dms-local\datatest`.

Kết quả trên Chrome local: **81 lượt màn (gồm 5 mobile), 19 kiểm tra API/OIDC, 0 thất bại, 0 ngoại lệ JavaScript**. Bốn lỗi lookup ở chi tiết hợp đồng/đơn/ticket được phát hiện và chạy lại sau sửa: không còn đường dẫn lookup query cũ, không tải danh mục Admin cho Staff hay danh mục phòng khi KTV chỉ xem ticket. Luồng thật đã tải CSV tài khoản, đổi mật khẩu lần đầu, đổi role và kiểm token cũ; khóa/mở/reset/logout đều được thử. Job nhắc bù cọc cũng đã chạy theo cron local và ghi audit SYSTEM.

## Giới hạn và phần còn lại

- Chưa kiểm tải 100 người dùng, phục hồi backup toàn hệ thống, thời gian khóa tạm đủ 15 phút hoặc cấu hình TLS production. Unit/integration/UI không thay thế các phép đo này.
- Chưa có outbox/retry bền vững giữa PostgreSQL và Keycloak. Khi lỗi identity, rollback hiện có chạy bù; nếu chính Keycloak ngừng hoạt động trong bước bù thì cần đối soát tài khoản mồ côi trước khi nhập lại.
- Xuất file Excel kết quả import là mức Could trong SAD, chưa triển khai. UI có báo lỗi JSON và tải tài khoản vừa tạo một lần.
- Những luồng TV2–TV5 còn nêu trong backend README (kích hoạt/gia hạn/trả phòng, phát hành hóa đơn, dashboard/báo cáo...) không được coi là hoàn thành chỉ vì có dữ liệu trên màn. Phần TV1 không thay cơ chế chiếm/trả giường chung của TV2.
- Nhánh/PR theo `.github/pull_request_template.md`, base develop, cần CI và review trước merge. Chưa tự commit/push/merge.

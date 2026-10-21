# Báo cáo: Hoàn thiện TV1 và dữ liệu kiểm thử toàn màn

> Ngày hoàn thành: 21/10/2026
> Thực hiện: TV1 Nguyễn Văn Séc, cùng Codex
> Phạm vi: account/common, import Excel, tác vụ SYSTEM và DataTest; khung frontend liên quan
> Căn cứ: đề xuất mục 13.1; SRS FR-ACC-01..10, NFR-REL-03, NFR-SEC-02..05; SAD 6.4, 7, 9; API 4.10, 7.1
> Trạng thái: Hoàn thành phần TV1 trong phạm vi nêu dưới đây

## 1. Mục tiêu

Đối chiếu phần việc TV1 với source mới nhất trên develop, bổ sung chức năng còn thiếu và nạp dữ liệu thử các màn hiện có. Đọc Docs, AGENTS/CLAUDE và .github trước khi triển khai. Triển khai ban đầu trên `feature/shared-tv1-completion`, nền `a292f08`; bàn giao qua nhánh `dev-secnv` theo yêu cầu Nguyễn Văn Séc, chia commit theo chức năng và mở PR vào `develop`. Không push trực tiếp vào `main`/`develop` hoặc tự merge.

## 2. Kết quả

| Hạng mục | File | Nội dung |
|---|---|---|
| Tài khoản và RBAC | account/service/impl/StudentManagementServiceImpl, account/config/SecurityConfig | Giữ Keycloak/PKCE/JWT và ma trận quyền hiện có; chặn Staff đổi mã/họ tên/giới tính; lỗi 401/403 có requestId |
| Import | StudentImportController, StudentImportService/Impl | Tải mẫu, kiểm tra và tạo hàng loạt; lỗi theo dòng, mã trùng, giới hạn file/giải nén; một transaction và bù Keycloak khi rollback |
| Bảo mật log | AccountDtos, StudentImportService.CreatedAccount, KeycloakProperties | Che mật khẩu/secret trong toString; giữ JSON trả mật khẩu tạm một lần, no-store |
| Audit | AuditController, AuditService/Impl | Chi tiết trước/sau, SYSTEM, giữ lọc/phân trang và append-only |
| Job chung | SystemJobService/Impl, các job contract/maintenance/notification | Transaction riêng từng ID; khóa/kiểm tra lại; hủy hợp đồng và đơn cùng transaction, đóng ticket, nhắc cọc FIFO và hợp đồng sắp hết hạn |
| Khóa chỗ ở | BedOccupancyService/Impl | Helper giữ thứ tự khóa cho job, kể cả chưa có HOLD; mọi trả giường vẫn qua dịch vụ TV2 |
| Frontend | StudentImportPage, features/account/StudentImportPanel, CrudPage và registry | Upload/preview/xác nhận/tải tài khoản; giữ tab hồ sơ; sửa lookup theo V5 và quyền đọc, không tải thêm danh mục khi chỉ xem |
| DataTest | DMS/scripts/datatest | Seed có backup/marker/manifest, Keycloak thật, kiểm tra 21 bất biến và công cụ chạy UI/API local |

## 3. Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| Backend verify, JDK 17, PostgreSQL Testcontainers | 152/152 đạt; không bỏ qua |
| JaCoCo tầng service.impl | 75,26%; phần Service mới 91,67%–100% |
| Frontend lint/build | Đạt; còn cảnh báo chunk lớn |
| Dữ liệu local | 21/21 kiểm tra đạt; đủ trạng thái chính và quan hệ cho bốn cổng |
| UI route | 81 lượt mở màn, gồm 5 lượt ở 360px; bốn lỗi lookup tìm được đã sửa và chạy lại đạt |
| OIDC/scope thật | Admin 106 quyền; Staff đúng đơn vị/tổng hợp chỉ đọc; Student sở hữu; KTV được giao; ngoài phạm vi 404 |
| Import rollback thật | Gây trùng username Keycloak: DMS rollback toàn lô, danh tính vừa tạo được dọn |
| Tải tài khoản/đổi mật khẩu/reset thật | Đạt; tổng 81 lượt màn và 19 kiểm tra API/OIDC, 0 thất bại/ngoại lệ JS. Tài khoản kiểm tra quản trị được khóa khi kết thúc |

**Chưa kiểm tra:** tải 100 người dùng/1.500 sinh viên, chờ đủ 15 phút khóa tạm, TLS production và diễn tập phục hồi cả hai database. Không suy ra các kết quả này từ unit test.

## 4. Điểm lệch so với thiết kế và quyết định cần xác nhận

| Nội dung | Quyết định | Lý do |
|---|---|---|
| SAD cũ nhắc cọc 08:00 hằng ngày, SRS NFR-REL-03 yêu cầu ít nhất mỗi giờ | Dùng mỗi giờ, đồng bộ SAD | SRS có độ ưu tiên cao hơn |
| Báo cáo .xlsx kết quả import | Chưa triển khai, giữ mức Could | Đã có lỗi từng dòng JSON và tải tài khoản một lần |
| PostgreSQL và Keycloak không có transaction phân tán | Giữ bù rollback của IdentityAccountService | Chưa có outbox/retry bền vững; nếu Keycloak lỗi trong bước bù cần đối soát |

Không có thay đổi schema/migration từ phần TV1 này. Local chạy V6 có sẵn trên develop; V1–V6 giữ nguyên.

## 5. Việc còn lại

| Việc | Phụ trách |
|---|---|
| Không còn chức năng Must của TV1 chưa triển khai trong phạm vi đối chiếu | TV1 |
| Review/CI qua PR trước merge develop | Nhóm |
| Các luồng còn thiếu nêu trong backend README: kích hoạt/gia hạn/trả phòng, phát hành hóa đơn, dashboard/báo cáo… | TV2–TV5 theo phân công |

## 6. Tệp liên quan

- Sửa: DTO/bảo mật/hồ sơ/audit/cấu hình/khóa giường; frontend router, registry, CRUD lookup; tài liệu SRS/SAD/API/UI và backend README.
- Mới: import Service/Impl/Controller và UI; khung/handler/scheduler SYSTEM; sự kiện hủy đơn; StudentImportIntegrationTest, SystemJobsIntegrationTest, SensitiveValuesTest; scripts/datatest và tài liệu kiểm thử.
- Xóa hoặc chuyển: Không xóa source hiện có. File test, mật khẩu và backup local nằm trong `D:\prjCDCNTT\dms-local\datatest`, không đưa vào Git. `.codex/` có từ trước được giữ nguyên.

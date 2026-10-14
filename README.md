# DMS - Hệ thống quản lý ký túc xá mini

Khung CRUD backend: xem [hướng dẫn chạy, API đã có và phần cần làm tiếp](DMS/backend/README.md).

Đồ án môn Chuyên đề CNTT, lớp 65CNTT, nhóm 3. Tài liệu thiết kế ở `Docs/`, mã nguồn ở `DMS/`.

| Thư mục | Nội dung |
|---|---|
| `Docs/` | Tài liệu theo giai đoạn: yêu cầu (SRS), thiết kế (SAD, CSDL, API, giao diện), kiểm thử, nhận xét, thuyết trình |
| `DMS/backend` | Spring Boot 3.5, Java 17, Maven (`mvnw`), PostgreSQL, Flyway |
| `DMS/frontend` | React 19, Vite, TypeScript, Ant Design, TanStack Query |
| `DMS/docker-compose.yml` | PostgreSQL 16 cho môi trường phát triển |

## Cần cài

- JDK 17 trở lên
- Node.js 22 (LTS)
- Docker Desktop (chạy PostgreSQL và test tích hợp bằng Testcontainers)

Maven không cần cài: dùng `mvnw` (Windows: `mvnw.cmd`) trong `DMS/backend`.

## Chạy khi phát triển

```bash
# 1. CSDL
cd DMS
docker compose up -d

# 2. Backend: http://localhost:8080 (Flyway tự tạo lược đồ khi khởi động)
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# 3. Frontend: http://localhost:5173 (/api chuyển sang backend)
cd ../frontend
npm install
npm run dev
```

Kiểm tra backend: `http://localhost:8080/actuator/health` trả `{"status":"UP"}`.

## Kiểm thử

```bash
cd DMS/backend
./mvnw verify                                   # toàn bộ test + báo cáo độ phủ target/site/jacoco/index.html
./mvnw test -Dtest=ModuleDependencyTest         # một lớp test
./mvnw test -Dtest=DmsApplicationTests#contextLoadsAndFlywayCreatesSchema   # một hàm test

cd ../frontend
npm run lint
npm run build
```

`ModuleDependencyTest` kiểm tra quy tắc phụ thuộc giữa các module (SAD mục 4.2). Test này đỏ nghĩa là có module gọi ngược tầng hoặc dùng Repository của module khác.

## Cấu trúc mã

Backend chia theo module của SAD mục 4.1, mỗi module là một package dưới `vn.edu.tlu.dms`:

| Module | Phụ trách |
|---|---|
| `common`, `account` | TV1 Nguyễn Văn Séc |
| `room` | TV2 Nguyễn Phú Nguyên |
| `registration` | TV3 Lê Đức Nguyên |
| `contract` | TV3 (hợp đồng, gia hạn), TV2 (chuyển phòng), TV4 (trả phòng, gửi đồ) |
| `maintenance` | TV4 Nguyễn Văn Quyền |
| `billing` | TV5 Nguyễn Tiến Lộc |
| `notification` | TV4 (thông báo), TV5 (dashboard) |

Trong mỗi module: `api/` (controller + DTO), `service/`, `domain/`, `repository/`, `job/`. Frontend chia `src/features/<module>` cùng tên.

**Thứ tự làm:** tuần đầu TV1 hoàn thiện `common` và `account` phía backend (đăng nhập, `CurrentUser`, mô hình lỗi, lịch sử trạng thái) và layout, guard, theme, `statusConfig`, `errorMessages` phía frontend. Các module khác dựa vào phần này.

## CSDL và migration

- `DMS/backend/src/main/resources/db/migration/V1__init.sql` là bản chép của `Docs/02 External Design/Database Design/DB-DMS-schema.sql` tại thời điểm dựng khung. `V2__spring_session.sql` tạo bảng phiên đăng nhập.
- Từ nay lược đồ thật là các file migration. Muốn đổi CSDL: thêm file mới `V3__mo_ta_ngan.sql`, **không sửa file đã có**, và cập nhật `DB-Design-DMS.md` trong cùng PR.
- Hibernate chạy `ddl-auto: validate`: entity lệch với lược đồ thì ứng dụng không khởi động.

## Quy ước làm việc

- Nhánh `main` được bảo vệ: chỉ nhận thay đổi qua Pull Request, cần 1 người duyệt và CI xanh.
- Tên nhánh: `feature/<module>-<viec>`, `fix/<module>-<loi>`, `docs/<tai-lieu>`. Ví dụ `feature/room-bed-occupancy`.
- Commit ngắn, tiếng Việt hoặc tiếng Anh đều được, bắt đầu bằng phạm vi: `room: thêm BedOccupancyService.occupy`.
- Đổi API, CSDL hoặc quy tắc nghiệp vụ thì sửa tài liệu trong `Docs/` cùng PR. Thứ tự ưu tiên khi mâu thuẫn: SRS, rồi SAD / CSDL / API, rồi giao diện.
- Không commit `.env`, mật khẩu, file trong `node_modules/`, `target/`, `dist/`.


## Chạy và đăng nhập DMS bằng Keycloak

Keycloak 26.8.1 quản lý mật khẩu, đăng nhập, đổi mật khẩu và phiên SSO. React dùng Authorization Code + PKCE S256; Spring Security xác thực JWT và DMS tiếp tục quản lý hồ sơ, đơn vị, phạm vi dữ liệu.

### Chạy trên máy đã chuẩn bị PostgreSQL local

Máy này đã có PostgreSQL riêng trong `backend/data/local-postgres`. Keycloak native dùng JDK đang cài; bản đã kiểm tra là JDK 27. Backend vẫn biên dịch target Java 17. Không cần Docker để chạy trên máy này.

Build sau khi sửa code:

```powershell
cd D:\Dormitory-management-system
powershell -NoProfile -ExecutionPolicy Bypass -File .\DMS\scripts\stop-local.ps1
cd .\DMS\backend
.\mvnw.cmd -DskipTests clean package
cd ..\frontend
npm.cmd ci
npm.cmd run build
cd ..\..
powershell -NoProfile -ExecutionPolicy Bypass -File .\DMS\scripts\start-local.ps1
```

- React: http://localhost:5173/login
- Keycloak Admin Console: http://localhost:8180/admin/
- Backend: http://localhost:8080/actuator/health
- Log/PID: `backend/data/local-run`.
- CSDL nghiệp vụ: `dms`; CSDL danh tính: `keycloak`, cùng cluster PostgreSQL local nhưng khác database.

Lần chuyển tài khoản đầu tiên dùng `start-local.ps1 -ProvisionAccounts`. Script đọc/tạo cấu hình riêng ở `backend/data/local-run/keycloak.env.local`; không có mật khẩu mặc định trong code. Tài khoản quản trị Keycloak là `keycloak-admin`, còn Admin DMS là `admin`.

Mật khẩu khởi tạo DMS được ghi riêng vào `backend/data/local-run/keycloak-accounts.local`. Đăng nhập lần đầu phải đổi mật khẩu trên Keycloak. Sau khi đã cấp và đổi mật khẩu, xóa tệp cấp mật khẩu này bằng `Remove-Item -LiteralPath` tới đúng tệp; không xóa tệp cấu hình chứa service-account secret đang dùng.

Không dùng `-ProvisionAccounts` cho lần chạy thông thường. Cờ này chỉ chuyển các hồ sơ chưa có `keycloak_user_id`; không reset mật khẩu người đã liên kết. Nếu chuyển tài khoản mới ở đợt sau, chọn tên tệp khác qua `DMS_KEYCLOAK_CREDENTIAL_FILE` và giữ lại tệp cấp cũ đến khi đã bàn giao.

### Chạy bằng Docker trên máy khác

Sao chép `.env.example` thành `.env` trong `DMS`, tự đặt mật khẩu DB Keycloak, mật khẩu quản trị Keycloak và secret client quản trị. Sau đó:

```powershell
cd D:\Dormitory-management-system\DMS
docker compose up -d
```

Terminal backend phải đặt `DMS_KEYCLOAK_ADMIN_CLIENT_SECRET` bằng secret trong `.env`; Spring không tự đọc `.env` của Compose. Đặt `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` nếu khác mặc định. Lần cấp Admin đầu:

```powershell
$env:DMS_KEYCLOAK_BOOTSTRAP_ENABLED = 'true'
$env:DMS_KEYCLOAK_INITIAL_ADMIN_USERNAME = 'admin'
$env:DMS_KEYCLOAK_INITIAL_ADMIN_PASSWORD = '<mat-khau-rieng-co-chu-va-so>'
.\mvnw.cmd spring-boot:run
```

Sau khi cấp xong, bỏ cờ bootstrap. Terminal React chạy `npm.cmd ci`, `npm.cmd run dev`. Realm template chỉ có service account, không có người dùng demo. Realm đã tồn tại sẽ được giữ nguyên khi restart; sửa JSON không tự ghi đè cấu hình realm hiện có. Thay đổi client/mapper/policy của realm đã chạy cần áp dụng qua Admin Console hoặc Admin REST có kiểm soát.

### Cách đọc code

- `account/config/SecurityConfig`: JWT có chữ ký hợp lệ, đúng issuer, audience `dms-api` và chưa hết hạn mới được xác thực.
- `account/service/impl/CurrentUserImpl`: tra `sub` vào `app_user.keycloak_user_id`; không ghép tài khoản bằng username. Kiểm tra khóa, role và `dms_security_version` mới nhất tại DMS.
- `account/service/IdentityAccountService` và `impl/KeycloakIdentityAccountServiceImpl`: tạo/sửa danh tính, khóa/mở khóa, reset mật khẩu tạm, thu hồi phiên, xóa danh tính.
- `IdentityAccountService.migrateUnlinkedAccounts` và Impl: giữ nguyên ID hồ sơ và liên kết nghiệp vụ khi chuyển tài khoản cũ. Mật khẩu cũ không chuyển; cấp mật khẩu tạm mới, bắt đổi lần đầu.
- `frontend/src/services/api/auth.ts`: adapter giữ token trong bộ nhớ; không lưu token/mật khẩu vào localStorage. Public client không có secret.
- Flyway V4 thêm subject liên kết, unique index và cho `password_hash` nhận NULL. Tài khoản đã liên kết không lưu bản sao hash mật khẩu tại DMS. V1–V3 giữ nguyên.

Các Service vẫn là interface có JavaDoc tiếng Việt; logic chỉ nằm trong Impl. ScopeGuard và kiểm tra dữ liệu theo đơn vị được giữ nguyên.

### Luồng bảo mật và giới hạn thực tế

React chuyển sang trang Keycloak để nhập mật khẩu. Required action `UPDATE_PASSWORD` hoàn tất trước khi cấp token. Keycloak quản lý khóa tạm khi sai mật khẩu; DMS không tự kiểm tra BCrypt khi dùng chế độ này. Quên mật khẩu vẫn qua Admin/Staff có quyền xác minh và reset; chưa bật SMTP hoặc đăng ký tự do.

Mỗi token chứa version từ thuộc tính chỉ Admin được sửa. Khóa/reset/đổi username/đơn vị/quyền liên đơn vị và logout từ DMS tăng version, đồng bộ sang Keycloak và thu hồi phiên; token cũ bị DMS chặn ngay. Logout từ DMS kết thúc các phiên của tài khoản trên các thiết bị, sau đó frontend chuyển qua logout Keycloak.

Đổi mật khẩu bằng trang Keycloak hoặc thao tác trực tiếp trong Keycloak Admin Console không phát sinh transaction/version DMS. JWT đã cấp có thể còn hiệu lực đến hết thời hạn, tối đa 5 phút; refresh/login mới do Keycloak kiểm soát. Nếu cần chặn tức thời mọi thay đổi bên ngoài DMS, cần bổ sung introspection hoặc listener sự kiện Keycloak. Không tuyên bố cơ chế này đã có.

PostgreSQL và Keycloak không có transaction phân tán. Khi rollback, tạo/sửa danh tính có thao tác bù; xóa danh tính thực hiện sau commit và được khóa trước khi xóa. Nếu Keycloak mất kết nối lúc bù/xóa thì log yêu cầu đối soát Admin Console; chưa có outbox retry bền vững. Reset mật khẩu không thể khôi phục mật khẩu cũ nếu HTTP thành công nhưng transaction DMS thất bại. Không tự lặp request ghi.

DMS chỉ dùng Keycloak. Mã session/BCrypt cũ, các wrapper legacy và test CSRF của luồng đã bỏ được dọn để giảm số file. Bảng Spring Session và các migration đã áp dụng vẫn giữ nguyên trong CSDL; ứng dụng không còn sử dụng Spring Session JDBC.

### Dùng chung cho nhóm

Đặt Keycloak trên máy chủ chung, dùng HTTPS, sửa issuer/redirect/web origin cho đúng địa chỉ và giữ CSDL/secret ngoài Git. Mỗi thành viên có tài khoản riêng. Dùng chung Keycloak mới đồng bộ danh tính; muốn đồng bộ phòng/sinh viên/hợp đồng thì CSDL DMS cũng phải dùng chung. Cấu hình `start-dev` và địa chỉ localhost hiện tại phục vụ local, không phải bản triển khai production.

### Kiểm chứng

Trước khi dọn: 50 test backend đạt trên PostgreSQL kiểm thử riêng, gồm chữ ký/issuer/audience/expiry JWT và subject/role/version/locked của hồ sơ; React lint/build đạt. Đăng nhập qua giao diện Keycloak đã đưa tài khoản kiểm thử vào portal Admin. API với token Keycloak thật đã kiểm tra CRUD sinh viên, khóa/mở khóa, reset bắt đổi mật khẩu, thu hồi token và logout.

Danh tính Keycloak, client password-grant và mật khẩu của kiểm thử trực tiếp đã được dọn. Hai hồ sơ kiểm thử DMS đã khóa và giữ lại để không vi phạm lịch sử audit append-only; không có tài khoản demo đang hoạt động. Tài khoản Admin DMS thật đã xác minh qua giao diện và được chuyển tới UPDATE_PASSWORD; mật khẩu mới cần người dùng tự nhập và gửi.

Trước khi chuyển đã lưu bản nguồn local trong `backend/data/keycloak-backup/source-before-keycloak.zip` và snapshot dữ liệu bảng ở schema `backup_before_keycloak`. Snapshot bảng không thay thế bản backup đầy đủ bằng `pg_dump`; không chạy script xóa/khôi phục schema tùy tiện.

Sau khi dọn: xóa/gộp 31 file, giảm số file mới chưa commit từ 38 xuống 9. Cấu hình, controller và bootstrap Keycloak dùng các file hiện có; thu hồi token/reset mật khẩu nằm trong AuthServiceImpl; cấp/chuyển danh tính nằm trong KeycloakIdentityAccountServiceImpl. Adapter, điều hướng và sự kiện đăng nhập gom vào frontend/src/services/api/auth.ts; trang quên mật khẩu gom vào LoginPage. Hướng dẫn chạy gom tại README này, script khởi động Keycloak gom vào start-local.ps1.

Backend clean verify đạt 33/33 test trên PostgreSQL kiểm thử riêng, không bỏ các kiểm tra CRUD/JWT. Các test session/CSRF của luồng đã loại bỏ được dọn; bộ CRUD dùng JWT. Backend clean package và React lint/build đạt. Sau khi khởi động lại, token Keycloak thật đã kiểm tra thành công CRUD sinh viên, role, khóa/mở khóa, reset và thu hồi token/logout. Giao diện đăng nhập, quên mật khẩu và chuyển hướng Keycloak PKCE S256 hoạt động. Các migration V1–V4 giữ nguyên; tài khoản/mật khẩu người dùng thật không bị reset khi dọn file.

# DMS - Hệ thống quản lý ký túc xá mini

Khung CRUD backend: xem [hướng dẫn chạy, API đã có và phần cần làm tiếp](DMS/backend/README.md).

API CRUD hiện tại dùng POST `.../search-by-page` cho danh sách/lookup và POST `.../save-or-update` cho tạo/sửa (ID trong body). React đã gọi theo contract này. Quy tắc nghiệp vụ và không hardcode được ghi trong [CLAUDE.md](CLAUDE.md); xem contract/ví dụ và kiểm chứng Java 17 trong README backend.

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

### Nhánh

| Nhánh | Vai trò | Nhận thay đổi từ |
|---|---|---|
| `main` | Bản ổn định, dùng để nộp và demo | PR từ `develop` khi xong một mốc, hoặc PR từ `hotfix/...` |
| `develop` | Nhánh tích hợp hằng ngày, mọi nhánh việc tách từ đây | PR từ nhánh việc |
| `<loai>/<module>-<viec>` | Một việc của một người | Tách từ `develop` mới nhất |

- `main` và `develop` đều được bảo vệ: không push thẳng, chỉ nhận qua Pull Request, cần 1 người duyệt và CI xanh.
- Tên nhánh việc: `<loai>/<module>-<viec>`, chữ thường, không dấu, các từ nối bằng `-`, tối đa khoảng 40 ký tự.
  - `<loai>`: `feature` (chức năng mới), `fix` (sửa lỗi), `refactor` (sửa cấu trúc, không đổi hành vi), `test` (chỉ thêm hoặc sửa test), `docs` (tài liệu trong `Docs/`, README), `chore` (cấu hình, CI, thư viện), `hotfix` (sửa gấp trên `main`).
  - `<module>`: `common`, `account`, `room`, `registration`, `contract`, `maintenance`, `billing`, `notification`; `frontend-core` cho layout, router, theme dùng chung của frontend; `shared` cho việc chạm nhiều module.
  - Ví dụ: `feature/room-bed-occupancy` (TV2), `feature/contract-transfer-room` (TV2), `feature/contract-checkout` (TV4), `fix/billing-electric-rounding` (TV5), `docs/shared-git-conventions`.
  - Không đặt nhánh theo tên người (`develop-ten`): nhiều người cùng module thì phân biệt bằng phần `<viec>`.
- Mỗi nhánh một việc, sống ngắn (vài ngày). Trước khi mở PR, cập nhật từ `develop`: `git pull --rebase origin develop` nếu nhánh chỉ mình làm, `git merge origin/develop` nếu nhiều người cùng làm.
- PR vào `develop` dùng **Squash and merge**, xóa nhánh sau khi merge. PR `develop` vào `main` dùng **Create a merge commit** (không squash, để hai nhánh không lệch lịch sử).
- `hotfix/...` tách từ `main`, PR vào `main`, sau đó mở thêm PR `main` vào `develop` để `develop` có bản sửa.

### Commit

- Dạng `<loai>(<module>): <mô tả>`, mô tả ngắn, tiếng Việt hoặc tiếng Anh đều được, không chấm cuối câu.
- `<loai>`: `feat`, `fix`, `refactor`, `test`, `docs`, `chore` (giống loại nhánh, `feature` viết là `feat`).
- Ví dụ: `feat(room): thêm API chiếm giường`, `fix(billing): sửa làm tròn tiền điện`, `docs(shared): cập nhật quy ước nhánh`.
- Tiêu đề PR theo cùng dạng, vì khi squash nó thành commit trên `develop`.

### Khác

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

### Chạy backend bằng IntelliJ trên máy local

Trước khi Run `DmsApplication`, mở terminal tại thư mục project và chạy:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\DMS\scripts\start-local.ps1 -DependenciesOnly
```

Tùy chọn này chỉ khởi động PostgreSQL ở 5432 và Keycloak ở 8180, để cổng 8080 cho backend trong IDE. Chọn module `dms-backend`, JDK 17 trở lên và Working directory là `D:\Dormitory-management-system\DMS\backend`. Với chức năng quản trị tài khoản, đặt biến `DMS_KEYCLOAK_ADMIN_CLIENT_SECRET` trong Run Configuration bằng giá trị cùng tên trong `DMS/backend/data/local-run/keycloak.env.local`; terminal/script không tự truyền biến sang IntelliJ đã mở.

Lỗi `Connection to localhost:5432 refused` nghĩa là PostgreSQL chưa chạy. Nếu báo thiếu package OAuth2, reload/sync Maven từ `DMS/backend/pom.xml`. Ứng dụng đặt múi giờ JVM thành `Asia/Ho_Chi_Minh` trước khi Spring mở kết nối để tránh tên cũ `Asia/Saigon` từ một số JDK Windows. Sau khi Run, kiểm tra `http://localhost:8080/actuator/health` trả `{"status":"UP"}`.

Đã kiểm chứng ngày 15/10/2026 bằng Java 17.0.17, classpath và thư mục chạy giống log IntelliJ: backend khởi động thành công, kết nối PostgreSQL/Flyway và health UP. Sau kiểm chứng đã trả cổng 8080 cho IDE; PostgreSQL và Keycloak tiếp tục chạy.

### Chạy bằng Docker trên máy Windows (một lệnh)

Cần Docker Desktop đang chạy, JDK 17 trở lên và Node.js. Từ thư mục gốc repository:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\DMS\scripts\start-docker.ps1
```

Script tự làm theo thứ tự: tạo `DMS\.env` với secret ngẫu nhiên (lần đầu), `docker compose up -d`, chờ Keycloak, build JAR và `npm ci` nếu chưa có, để backend chạy Flyway, nạp `sample-data.sql` và `sample-accounts.sql`, cấp tài khoản Keycloak (admin và các tài khoản mẫu) bằng mật khẩu tạm ngẫu nhiên rồi khởi động backend và frontend. Mật khẩu tạm nằm trong `backend\data\local-run\keycloak-docker-accounts.local` (Git bỏ qua), mật khẩu quản trị Keycloak là `KC_BOOTSTRAP_ADMIN_PASSWORD` trong `DMS\.env`. Chạy lại script là an toàn. Dừng: `stop-local.ps1`, rồi `docker compose stop` trong `DMS`. Xóa sạch dữ liệu: `docker compose down -v` và xóa `DMS\.env`, tệp cấp mật khẩu.

### Chạy bằng Docker trên máy khác (thủ công)

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

## Phân quyền Role → thao tác (cập nhật 14/10/2026)

Theo quyết định mới của người phụ trách, ADMIN có mọi quyền nghiệp vụ trên toàn hệ thống. ADMIN không cần gắn management_unit_id; bỏ giới hạn đơn vị/chủ sở hữu/người được giao khi xác định quyền, nhưng vẫn phải qua trạng thái, ràng buộc dữ liệu, đối tượng sinh viên thật và lịch sử/audit. Không đổi hoặc giả vai trò/người thao tác để vượt phạm vi.

Khung tham khảo RolePrivilege của HR-V5: `ScopeGuard` kiểm tra quyền hiện hành qua `RolePermissionService`; ma trận trong code chỉ xác định quyền mặc định và giới hạn nghiệp vụ của từng role, mỗi mã là `<RESOURCE>_<READ|CREATE|UPDATE|DELETE>`. READ dùng cho tìm kiếm và xem chi tiết; khóa/mở khóa/reset/gán phạm vi tài khoản có mã riêng. `/api/auth/me` trả thêm `permissions: string[]`; React dùng danh sách này cho menu, route và từng nút. API khai quyền bằng @RequirePrivilege giống V5; Spring Security Method Security kiểm tra trước thân Controller. Service Impl tiếp tục kiểm quyền nội bộ, phạm vi và trạng thái. Không chỉ dựa vào việc ẩn nút.

- ADMIN: mọi quyền quản trị và nghiệp vụ ở mọi đơn vị; tài khoản và người thao tác trong audit vẫn là ADMIN thực.
- STAFF: nghiệp vụ trong đơn vị được giao; crossUnitAccess chỉ mở rộng đọc. Đơn/yêu cầu do sinh viên tạo không mặc nhiên cho Staff tạo hoặc hủy thay sinh viên; quyền sửa/xét duyệt phụ thuộc endpoint và trạng thái.
- TECHNICIAN: đọc/cập nhật tiến độ ticket được giao, không CRUD dữ liệu phòng/hợp đồng/hóa đơn.
- STUDENT: dữ liệu cá nhân và phòng đang ở; đơn/yêu cầu của mình, không sửa role hoặc ID người sở hữu.

Đổi role của tài khoản Staff/KTV/Admin qua POST `/api/admin/users/save-or-update` với `id` và `role` trong body, đồng bộ Keycloak và thu hồi token cũ. Staff/KTV phải có đơn vị; chuyển sang ADMIN xóa đơn vị và cờ tổng hợp; không chuyển hồ sơ STUDENT thành tài khoản cán bộ, không tự đổi role tài khoản đang sử dụng. ADMIN tạo đơn đăng ký hộ sinh viên bằng `studentId` rõ ràng; STUDENT không được gửi ID của người khác.

Cập nhật 15/10/2026: ADMIN quản lý ma trận động tại `/admin/role-permissions`, lưu quyền của STAFF/STUDENT/TECHNICIAN trong `role_permission_matrix` qua Flyway V5. ADMIN giữ mọi quyền đã đăng ký và không có bản ghi để thu hồi quyền ADMIN. Mã quyền vẫn cố định trong Constants; quyền đang được cấp đọc từ CSDL. Sổ cọc/audit vẫn giữ chỉ đọc, hồ sơ có lịch sử và trạng thái vẫn chịu ràng buộc xóa/hủy như trước.

Kiểm chứng cập nhật phân quyền: backend clean verify đạt **44/44 test** (11 test mới trong hai file test đã có), clean package đạt; React lint/build và kiểm tra quyền/điều hướng đạt. Đã kiểm tra bằng token Keycloak thật và đăng nhập tài khoản kiểm thử tạm để thấy đầy đủ menu nghiệp vụ, nút Thêm mới/form phòng trong cổng Admin. Đã dọn danh tính/mật khẩu kiểm thử và khóa lại hồ sơ audit. Không thêm file source hoặc migration; máy chủ local đã chạy lại với mã mới.

### Khai quyền API bằng @RequirePrivilege — 14/10/2026

Mọi hàm REST mapped khai `@RequirePrivilege(Permission.ROOM_CREATE)` hoặc một danh sách như `@RequirePrivilege({Permission.ROOM_TYPE_READ, Permission.ROOM_CREATE, Permission.ROOM_UPDATE})`. Nhiều mã dùng OR: có ít nhất một quyền mới được chạy API. Với API `save-or-update`, annotation cho phép CREATE hoặc UPDATE đi vào Controller; Service Impl bắt buộc kiểm tra chính xác CREATE khi không có ID, UPDATE khi có ID. Quyền Thêm không cấp quyền Sửa và ngược lại; DELETE vẫn kiểm tra riêng.

`common/security/RequirePrivilege.java` chỉ khai annotation; `ScopeGuard.hasDeclaredPrivilege` đọc khai báo trên hàm thật, dùng khai báo lớp nếu hàm chưa khai, và kiểm role với dữ liệu tài khoản hiện tại. `SecurityConfig` bật Spring Security Method Security và đăng ký AuthorizationManagerBeforeMethodInterceptor: thiếu annotation, mảng rỗng hoặc không có quyền thì trả 403 trước thân Controller. Chưa đăng nhập/token không hợp lệ vẫn bị filter chain trả 401. Annotation ở hàm thay thế annotation lớp; Admin không vượt qua mã quyền chưa đăng ký.

Service Impl vẫn giữ kiểm tra quyền cho lời gọi nội bộ, phạm vi đơn vị/chủ sở hữu, trạng thái và ràng buộc dữ liệu; việc thêm annotation không biến dữ liệu đang dùng thành dữ liệu được phép xóa. ADMIN tiếp tục có mọi quyền nghiệp vụ đã đăng ký trên toàn hệ thống. Frontend và `/api/auth/me.permissions` giữ nguyên mã quyền hiện có.

Ở bước bổ sung annotation chỉ thêm một file source là `RequirePrivilege.java`; không thêm migration, Aspect/Service mới hoặc dependency V5. Bộ test kiểm OR, chặn trước thân Controller, lớp/hàm, thiếu/rỗng/sai annotation và quét mọi API để không bỏ sót quyền. Backend clean verify đạt **53/53 test**.

### Constants dùng chung — 15/10/2026

Backend khai mã cố định tại `common/constants/DmsConstants.java`: `Role`, `Action`, `Resource`, `Permission` và các nhóm trạng thái theo nghiệp vụ. Controller dùng `@RequirePrivilege(Permission.ROOM_CREATE)`; Service Impl dùng cùng mã trong `requirePermission`. `RolePermissionService` đọc quyền được cấp từ CSDL; `ScopeGuard` giữ danh mục quyền mặc định/phù hợp theo role; khai một hằng số không tự cấp thêm quyền cho role. Các nhóm có constructor private và tập mã bất biến để tránh thay đổi cấu hình quyền lúc chạy.

React dùng `frontend/src/constants/authorization.ts` cho role, nghiệp vụ và quyền, kèm kiểu TypeScript suy ra từ hằng số. `permissionFor` chỉ nhận mã đã đăng ký; mã không hợp lệ không mở menu/nút CRUD. Quyền thực tế vẫn lấy từ `/api/auth/me.permissions`. Backend và frontend giữ nguyên mã `<RESOURCE>_<ACTION>`, không đổi dữ liệu đã lưu, format JWT hoặc các migration Flyway đã áp dụng.

Constants dành cho mã cố định của ứng dụng. URL, port, realm, client ID và secret tiếp tục cấu hình qua biến môi trường; các ngưỡng nghiệp vụ tiếp tục đọc `system_config`. Không đưa mật khẩu/secret vào Constants. Khi thêm quyền, cập nhật danh mục Constants, giới hạn mặc định của `ScopeGuard`, migration cấp quyền nếu cần, annotation API và khai báo frontend tương ứng, rồi chạy test phân quyền.

Kiểm chứng: **55/55 test backend** đạt, gồm baseline giữ nguyên toàn bộ danh sách quyền ADMIN/STAFF/STUDENT/TECHNICIAN và kiểm danh mục 103 mã quyền. Đã đối chiếu mã role/action/resource/permission của React với class backend đã biên dịch và giữ nguyên 12 regex DTO. Backend clean package và React lint/build đạt; API dùng token Keycloak thật kiểm tra CRUD, khóa/mở khóa, reset và thu hồi phiên thành công. Bản local đã chạy lại, danh tính/mật khẩu kiểm thử được dọn. Bước này chỉ thêm hai file Constants; không thêm migration.

### Hoàn thiện các luồng dùng chung — 15/10/2026

- Đăng nhập và truy cập URL trực tiếp: trang gốc Admin kiểm USER_READ, Staff kiểm DASHBOARD_READ, Student kiểm PROFILE_READ, Technician kiểm MAINTENANCE_READ. Chuẩn hóa dấu gạch chéo/query/hash; cổng/màn không có trong khai báo bị từ chối. Chuyển hướng sau đăng nhập dùng quyền thực tế từ /auth/me.
- Hồ sơ của tôi có ở cả bốn cổng, dùng lại CrudPage: GET `/api/me/profile` để đọc và POST `/api/me/profile/save-or-update` để sửa. Sinh viên sửa điện thoại/ quê quán/liên hệ khẩn cấp; cán bộ chỉ sửa điện thoại. Mã sinh viên, họ tên, role và đơn vị không được sửa từ hồ sơ cá nhân.
- CRUD dùng chung lưu từ khóa, bộ lọc, số trang và cỡ trang trên URL theo từng resource, ví dụ users.q/users.role/users.page. Refresh, quay lại hoặc các tab ghép không làm lẫn điều kiện; đổi bộ lọc/cỡ trang trở về trang đầu, xóa hết trang tự về trang hợp lệ. Cỡ trang 10/20/50/100, mặc định 20; chỉ gửi bộ lọc được khai báo trong CrudResource.filters.
- Control nhập liệu được đặt trực tiếp trong Form.Item để nhận value/onChange. Form hiển thị lỗi trường do API trả về; lỗi 409 tải lại dữ liệu qua handler chung. Chuỗi rỗng có thể xóa nội dung text tùy chọn, trường không gửi hoặc null được giữ nguyên khi sửa. Không đóng modal đang gửi request; thao tác xóa, khóa, reset, mở khóa và đổi phạm vi đọc có xác nhận.
- Tài khoản lọc role, managementUnitId và locked; UI xác nhận đổi vai trò/đơn vị và giải thích việc thu hồi phiên. Staff có thể được Admin cấp/thu hồi đọc liên đơn vị qua API cross-unit-access đã có; quyền ghi trong đơn vị vẫn giữ nguyên.
- Nhật ký lọc actorId, action, entityType, from và to, kết hợp q. Action/type là mã chính xác; khoảng thời gian ISO có múi giờ, hai biên đều bao gồm. Bộ lọc sai trả 400; chỉ Admin được đọc. Khung hiện phân trang theo id giảm dần; xem chi tiết từ bản ghi đã trả về, không gọi endpoint chi tiết chưa có. Truy vấn nằm trong AuditServiceImpl, Controller chỉ nhận tham số và gọi Service.

Kiểm chứng: 60/60 test backend đạt trên PostgreSQL kiểm thử riêng; thêm 5 test cho bộ lọc, phạm vi quản trị, đổi đơn vị và khóa/mở khóa/reset thu hồi token. 53 kiểm tra điều hướng/hồ sơ của bốn role và 48 kiểm tra query/bộ lọc/validation đạt. Backend clean package và React lint/build đạt. Token Keycloak thật kiểm CRUD sinh viên/khóa/mở khóa/reset/logout; giao diện Admin kiểm đăng nhập, giữ bộ lọc khi reload, mở/sửa/xóa nội dung hồ sơ và lọc nhật ký. Tài khoản/mật khẩu kiểm thử được dọn, audit append-only giữ nguyên. Đây là các luồng chung của khung; chưa bổ sung nghiệp vụ phân phòng, hợp đồng, phát hành hóa đơn hay tiền cọc.


### Component giao diện dùng chung — 15/10/2026

Các cổng Admin, Quản lý, Sinh viên và Kỹ thuật dùng cùng theme trắng/xám và xanh trầm. Màu/shadow khai tại `DMS/frontend/src/app/theme.ts`; CSS bố cục dùng các biến này trong `src/index.css`.

- `components/ui/FormControls.tsx`: input, tìm kiếm, số, Select, ô nhiều dòng, checkbox/switch và các control mở rộng. Chuyển nguyên `value`, `onChange`, `ref`, `aria-*` vào control để dùng được với Ant Form/React Hook Form.
- `components/ui/DataTable.tsx`: bảng chung, trạng thái trống, cuộn ngang và tùy chọn phân trang do màn gọi truyền vào. `ResponsiveTable` dùng lại bảng này ở desktop.
- `components/ui/Surface.tsx`: nút, khối nội dung, modal/drawer, thanh bộ lọc và lưới biểu mẫu. Form hai cột trên desktop, một cột trên mobile; trường nhiều dòng chiếm cả hàng.
- Trang CRUD và các màn riêng dùng các component trên; API, quyền và điều kiện nghiệp vụ vẫn được kiểm tra như trước.

### Dữ liệu mẫu để cả nhóm thử CRUD — 15/10/2026

File dùng chung: **`DMS/scripts/sample-data.sql`**. Trước tiên khởi động backend để Flyway tạo schema đến V5. Chạy toàn bộ file trên database phát triển của mình; không đưa file này vào thư mục migration.

Từ thư mục gốc repository, nếu máy có `psql` trong PATH:

```powershell
psql -h localhost -p 5432 -U dms -d dms -v ON_ERROR_STOP=1 -f "DMS/scripts/sample-data.sql"
```

Thay host/port/user/database theo môi trường và nhập mật khẩu PostgreSQL khi được hỏi. Có thể mở file UTF-8 bằng DataGrip hoặc pgAdmin và chạy toàn bộ script trên đúng database thay cho dòng lệnh.

Bộ mẫu gồm 12 sinh viên, 3 tòa, 2 loại phòng, 9 phòng, 42 giường, 12 đơn đăng ký, 9 hợp đồng, 14 hóa đơn có dòng chi tiết, 12 chỉ số điện/nước, 3 yêu cầu sửa chữa, 3 yêu cầu gia hạn, 2 yêu cầu chuyển phòng, 3 lựa chọn hè, 1 phiếu gửi đồ, 1 phiếu trả phòng, sổ cọc và thông báo/lịch sử. Học kỳ dùng khóa năm học/loại; nếu đã có cùng học kỳ thì giữ nguyên lịch của nhóm. Đơn vị, danh mục ưu tiên, cấu hình và quyền đã có được giữ nguyên.

Script chạy trong transaction, giữ constraint và trigger, không xóa dữ liệu. Marker `DMS-SAMPLE-V1` trong audit giúp lần chạy sau bỏ qua toàn bộ phần nạp: không tạo bản ghi trùng và không ghi đè những gì đã sửa/xóa trên màn CRUD. Nếu phát hiện mã `MAU-*`/`mau.*` trùng hoặc lịch học kỳ chồng nhau, script báo lỗi và rollback; không tự sửa dữ liệu của nhóm.

**Tài khoản `mau.*` do `sample-data.sql` tạo ra đều bị khóa và chưa liên kết Keycloak; không có mật khẩu demo.** Muốn đăng nhập thử đủ bốn vai trò, làm theo mục dưới. SQL không thay mật khẩu, subject, phiên đăng nhập hay ma trận quyền.

### Tài khoản mẫu đăng nhập thử — 15/10/2026

Nếu chạy bằng `DMS\scripts\start-docker.ps1`, script đã tự nạp `sample-accounts.sql` và cấp tài khoản Keycloak; Đường A/Đường B dưới đây chỉ dành cho luồng PostgreSQL local (`start-local.ps1`). Hai luồng cùng dùng cổng 5432 nên không chạy đồng thời.

File **`DMS/scripts/sample-accounts.sql`** (chạy sau `sample-data.sql`, idempotent, không đưa vào thư mục migration) mở khóa 10 tài khoản mẫu và thêm ticket `MAU-TK-04` đã giao cho `mau.kt.1`. Các tài khoản `mau.*` còn lại vẫn bị khóa. Cuối script in bảng `locked`/`linked` để biết nên đi đường A hay B.

| Vai trò | Username | Đơn vị | Dùng để thử |
|---|---|---|---|
| ADMIN | `admin` (đã có) | — | phân quyền động, tài khoản, audit, toàn quyền nghiệp vụ |
| STAFF | `mau.ql.1` | K1, đọc liên đơn vị | duyệt chuyển phòng của SV 02, giao ticket MAU-TK-01 cho `mau.kt.1`, duyệt đơn SV 10 |
| STAFF | `mau.ql.2` | N2+N3 | tạo hợp đồng từ đơn đã duyệt của SV 11; thử chặn cross-scope (404) với dữ liệu K1 |
| TECHNICIAN | `mau.kt.1` | K1 | xử lý MAU-TK-04 (ASSIGNED) |
| TECHNICIAN | `mau.kt.2` | N2+N3 | xử lý MAU-TK-02 (ASSIGNED) |
| STUDENT | `mau.sv.01` | K1 | đang ở, trưởng phòng, gia hạn chờ duyệt, ticket MAU-TK-01 |
| STUDENT | `mau.sv.02` | K1 | đang ở, có yêu cầu chuyển phòng chờ duyệt |
| STUDENT | `mau.sv.07` | K1 | hợp đồng PENDING + giữ giường: thu cọc/kích hoạt |
| STUDENT | `mau.sv.10` | K1 | đơn SUBMITTED chờ duyệt |
| STUDENT | `mau.sv.11` | N2+N3 | đơn APPROVED, chưa có hợp đồng |
| STUDENT | `mau.sv.12` | N2+N3 | đơn DRAFT: sửa/nộp/hủy |

Tên đăng nhập là `mau.sv.01`…; mã sinh viên trong hồ sơ là `MAU-SV-01`.

**Đường A — tài khoản mẫu chưa liên kết Keycloak** (cột `linked` = `chưa`). Từ thư mục gốc repository:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\DMS\scripts\stop-local.ps1
psql -h localhost -p 5432 -U dms -d dms -v ON_ERROR_STOP=1 -f "DMS/scripts/sample-accounts.sql"
$env:DMS_KEYCLOAK_CREDENTIAL_FILE = '.\data\local-run\keycloak-sample-accounts.local'
powershell -NoProfile -ExecutionPolicy Bypass -File .\DMS\scripts\start-local.ps1 -ProvisionAccounts
```

Bootstrap từ chối ghi đè tệp credential cũ nên phải dùng tên tệp mới (đường dẫn tính từ `DMS\backend`). Bước này cấp Keycloak cho **mọi** tài khoản chưa liên kết, kể cả `mau.*` còn khóa (bị disabled nên không đăng nhập được). Mật khẩu tạm ngẫu nhiên của từng username nằm trong `DMS/backend/data/local-run/keycloak-sample-accounts.local` (Git bỏ qua). Đăng nhập tại `http://localhost:5173/login`, Keycloak bắt đổi mật khẩu lần đầu. Sau đó khởi động lại bình thường, không có `-ProvisionAccounts`, và xóa tệp credential bằng `Remove-Item -LiteralPath` tới đúng tệp khi đã bàn giao.

**Đường B — tài khoản mẫu đã được cấp Keycloak từ trước** (cột `linked` = `rồi`, Keycloak đang disabled): đăng nhập `admin`, vào quản lý tài khoản, bấm **Mở khóa** rồi **Đặt lại mật khẩu** cho từng username trong bảng. Chỉ cần chạy `sample-accounts.sql` nếu muốn có thêm ticket `MAU-TK-04`; việc mở khóa trong DB khi đó trùng với thao tác trên UI.

### Màn tick quyền động theo role — 15/10/2026

Đăng nhập ADMIN, vào **Phân quyền theo vai trò** tại `http://localhost:5173/admin/role-permissions`. Chọn STAFF/STUDENT/TECHNICIAN, tìm màn rồi tick Xem/Tìm kiếm, Thêm, Sửa, Xóa hoặc quyền riêng; bấm **Lưu quyền** để áp dụng. Khôi phục mặc định chỉ đổi bản nháp, vẫn cần lưu. ADMIN hiển thị toàn quyền và không sửa được. Các ô không phù hợp vai trò bị khóa; PROFILE_READ bắt buộc.

Backend dùng RolePermissionService interface/Impl và Flyway V5, chỉ thêm một bảng quyền. Khi chạy bản mới, Flyway tự tạo bảng và giữ quyền mặc định hiện tại; không sửa V1–V4. Quyền được đọc từ CSDL mỗi lần kiểm tra API, kể cả token đã cấp trước khi bỏ tick. Frontend cập nhật /me sau lưu, khi focus, mỗi 30 giây hoặc khi API bị từ chối; mất quyền trang chủ sẽ chuyển về hồ sơ. Mỗi lần lưu có version chống ghi đè và audit trước/sau.

Quyền áp dụng cho mọi tài khoản cùng role trên cùng backend/database DMS. Dùng chung Keycloak nhưng mỗi máy có database DMS riêng thì quyền vẫn riêng; muốn cả nhóm đồng bộ cần dùng chung backend/database. Mã quyền vẫn nằm trong Constants; danh sách đang cấp nằm trong bảng, không phải sửa code để bật/tắt từng quyền.


Kiểm chứng phần ma trận: backend clean verify đạt **73/73 test**, gồm 13 test mới (thu hồi/khôi phục với token cũ, role không được quản trị, danh sách sai, dữ liệu thiếu/hỏng, version 409, no-op và audit). Backend clean package đạt; React build/lint đạt và 128 kiểm tra runtime helper (27 matrix/fallback, 53 auth/navigation/profile, 48 CRUD query/validation) đạt. Danh mục Role/Action/Resource/Permission của React khớp chính xác class backend đã biên dịch.

Bản local đã chạy migration V5 và health UP. JWT Keycloak thật xác minh /me và GET ma trận bốn role; trình duyệt kiểm tra tick/bỏ READ, tự chọn READ khi tick Sửa, Hủy, cảnh báo đổi role và ADMIN chỉ xem. Không lưu thay đổi quyền trên database local khi thử giao diện; toàn bộ test ghi ma trận dùng PostgreSQL kiểm thử riêng. Danh tính/mật khẩu kiểm thử đã dọn, hồ sơ có audit được khóa và giữ lịch sử. Phần này chỉ thêm 3 Java source, 1 migration và 1 trang React; Service interface/Impl có ghi chú tiếng Việt.

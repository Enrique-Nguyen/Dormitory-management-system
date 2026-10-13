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

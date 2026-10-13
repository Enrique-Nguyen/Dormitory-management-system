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

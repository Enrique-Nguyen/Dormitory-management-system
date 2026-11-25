# Kế hoạch kiểm thử E2E tự động và bộ testcase chi tiết

> Phiên bản 0.1 · 24/11/2026 · Công cụ: Playwright (`DMS/frontend/e2e`).
> Căn cứ: SRS-DMS (mục 3, 4, 8, 10 và phần phân quyền), API-Spec-DMS, DMS-UI-UX-Design, CLAUDE.md (quy tắc CRUD dùng chung và không hardcode).
> Tài liệu này là **kế hoạch và đặc tả testcase**, chưa phải kết quả chạy. Hiện chỉ có khung Playwright và 14 test mẫu (đăng nhập, chuyển cổng theo role); các testcase bên dưới **chưa được cài đặt** trừ khi cột "Trạng thái" ghi khác.

### Lịch sử thay đổi

| Phiên bản | Ngày | Nội dung |
|---|---|---|
| 0.1 | 24/11/2026 | Bản đầu: chiến lược, môi trường, dữ liệu, lộ trình, 220 testcase, ma trận truy vết AT/FR → testcase |

---

## 1. Mục tiêu và phạm vi

### 1.1. Mục tiêu

1. Tự động hóa 33 kịch bản nghiệm thu bắt buộc AT-01..AT-33 của SRS mục 8.1 ở mức có thể thực hiện qua trình duyệt và API thật.
2. Bảo vệ các luồng nghiệp vụ lõi (đăng ký → phân giường → kích hoạt → sửa chữa → hóa đơn → cọc → trả phòng) khỏi hồi quy khi các thành viên cùng sửa code.
3. Kiểm chứng những điều mà unit test và test tích hợp phía backend không chứng minh được: Keycloak thật, redirect OIDC/PKCE, guard theo role trên React, nút hiển thị theo `allowedActions`, form Sửa nạp đúng dữ liệu, giao diện 360 px.

### 1.2. Trong phạm vi

- Bốn cổng: Student, Staff, Technician, Admin; toàn bộ phân hệ ACC, ROOM, REG, CON, MNT, BIL, NOT (SRS mục 3).
- Phân quyền theo vai trò + phạm vi đơn vị + trạng thái; ma trận quyền động (FR-ACC-10).
- Các tác vụ định kỳ (hủy hợp đồng PENDING, đóng ticket, nhắc bù cọc) bằng cách đẩy lùi thời gian dữ liệu.
- Responsive tối thiểu: 360 px (Student, Technician) và desktop (Staff, Admin).

### 1.3. Ngoài phạm vi E2E (đã có hoặc phải kiểm bằng cách khác)

| Nội dung | Cách kiểm chứng | Lý do |
|---|---|---|
| AT-03 chèn trực tiếp hai phân phòng vào một giường bằng SQL | `DmsApplicationTests`/test tích hợp Testcontainers đã có hoặc cần có | Là kiểm chứng ràng buộc CSDL, không cần trình duyệt |
| AT-07 chèn lỗi giữa chừng khi chuyển phòng | Test tích hợp có chèn lỗi (fault injection) | Cần can thiệp vào code/transaction; E2E chỉ kiểm hệ quả hiển thị (CON-11) |
| Rollback import khi Keycloak lỗi (ACC-12) | Test tích hợp đã có (`KeycloakAccountIntegrationTest` và liên quan) | Cần giả lập lỗi identity |
| NFR-PER-01/02 tải 100 user, phát hành 1.500 hóa đơn | k6 hoặc JMeter | E2E UI không đo tải; BIL-18 chỉ là phép đo thời gian bằng API |
| Chờ đủ 15 phút của khóa tạm (FR-ACC-02) | Cấu hình Keycloak, chỉ kiểm việc bị khóa | Quá chậm cho CI; AUTH-04 gắn thẻ `@slow` |
| Kiểm kiến trúc (ArchUnit), JaCoCo ≥ 60% | `./mvnw verify` | Đã có |
| Cổng thanh toán, QR, email, realtime | Ngoài MVP (SRS 1.2) | Không có tính năng |

---

## 2. Chiến lược kiểm thử

### 2.1. Ba loại testcase

| Loại | Ký hiệu | Mô tả | Công cụ |
|---|---|---|---|
| **UI** | `UI` | Người dùng thao tác trên trình duyệt từ đầu đến cuối | Playwright page |
| **API** | `API` | Gọi REST thật bằng Bearer JWT, dùng khi cần kiểm quyền, đồng thời, trường lạ, hoặc dữ liệu mà UI không cho gửi | Playwright `request` |
| **Lai** | `UI+API` | Dựng dữ liệu bằng API (nhanh, ổn định), kiểm bằng UI; hoặc thao tác UI rồi đối chiếu bằng API | Cả hai |
| **Thủ công** | `TAY` | Không tự động hóa được hợp lý (chạm điện thoại thật, đo cảm nhận) | Danh sách kiểm tra |

Nguyên tắc: **dựng dữ liệu bằng API, chỉ dùng UI cho đúng điều đang kiểm**. Một luồng 10 bước không cần đi qua giao diện cả 10 bước; chỉ luồng xuyên suốt (nhóm XF) đi hết bằng UI để chứng minh các màn nối với nhau.

### 2.2. Mức ưu tiên

| Mức | Ý nghĩa | Khi nào chạy |
|---|---|---|
| **P0** | Luồng lõi, phân quyền, tiền, an toàn dữ liệu; hỏng là không demo/nghiệm thu được | Mọi PR vào `develop` (tập smoke) |
| **P1** | Quy tắc nghiệp vụ và biên quan trọng | Hàng đêm và trước khi merge `develop` → `main` |
| **P2** | Hoàn thiện, giao diện, mục Should, chậm | Hàng đêm hoặc thủ công |

### 2.3. Thẻ (tag) Playwright

`@smoke` (tập P0 ngắn, < 5 phút), `@critical` (P0), `@regression` (P1), `@slow` (> 1 phút hoặc cần chờ), `@mobile`, `@concurrency` (chạy tuần tự), `@job` (cần đẩy lùi thời gian), `@axe` (quét truy cập). Chạy: `npx playwright test --grep @smoke`.

### 2.4. Quy ước mã testcase

`E2E-<NHÓM>-<số>`. Nhóm: `AUTH`, `ACC`, `RBAC`, `ROOM`, `REG`, `CON`, `MNT`, `BIL`, `NOT`, `SUM` (hè), `JOB`, `UI`, `SEC`, `XF` (luồng xuyên suốt). Trong mã nguồn, tiêu đề test bắt đầu bằng mã này để truy vết: `test('E2E-REG-13 hai Staff phân cùng một giường: một thành công, một BED_TAKEN', ...)`.

---

## 3. Môi trường và dữ liệu

### 3.1. Thành phần cần chạy

| Thành phần | Cách chạy | Ghi chú |
|---|---|---|
| PostgreSQL DMS + PostgreSQL Keycloak + Keycloak 26.8.1 | `docker compose up -d` trong `DMS/` (cần `.env` có `KEYCLOAK_DB_PASSWORD`, `KC_BOOTSTRAP_ADMIN_PASSWORD`, `DMS_KEYCLOAK_ADMIN_CLIENT_SECRET`) hoặc `scripts/start-local.ps1` | Cổng Keycloak 8180 |
| Backend Spring Boot | `./mvnw spring-boot:run` với profile riêng `e2e` (đề xuất, xem 3.4) | Cổng 8080, Flyway tự migrate |
| Frontend | Playwright tự bật `npm run dev` (5173); trong CI dùng bản build + `vite preview` | Proxy `/api` → 8080 |

**Tuyệt đối không chạy E2E trên CSDL có dữ liệu thật hoặc dữ liệu đang demo.** Dùng CSDL/realm riêng cho E2E (database `dms_e2e`, realm `dms-e2e` hoặc cùng realm với tiền tố người dùng `e2e.`).

### 3.2. Tài khoản E2E

Tạo bằng script gọi Keycloak Admin API (client `dms-admin` có sẵn) và API tạo tài khoản của DMS; **mật khẩu lấy từ biến môi trường, không commit**. Tài khoản phải **đã đổi mật khẩu lần đầu** (trừ nhóm test cố ý kiểm bước đổi mật khẩu).

| Biến môi trường | Vai trò | Đơn vị | Ghi chú |
|---|---|---|---|
| `E2E_ADMIN_*` | ADMIN | — | |
| `E2E_STAFF_*` | STAFF | K1 | Staff chính |
| `E2E_STAFF_N23_*` | STAFF | N2+N3 | Kiểm chéo đơn vị |
| `E2E_STAFF_CROSS_*` | STAFF | K1 + quyền tổng hợp | Kiểm FR-ACC-08 |
| `E2E_TECHNICIAN_*` | TECHNICIAN | K1 | |
| `E2E_TECHNICIAN_N23_*` | TECHNICIAN | N2+N3 | Kiểm giao việc khác đơn vị |
| `E2E_STUDENT_*` | STUDENT | — | Sinh viên có hợp đồng ACTIVE sẵn |
| `E2E_STUDENT_FREE_*` | STUDENT | — | Sinh viên chưa có hợp đồng, dùng cho luồng đăng ký |

Lưu ý: `playwright.config.ts` hiện chỉ có 4 vai trò (`student`, `staff`, `technician`, `admin`). Các tài khoản bổ sung ở trên cần thêm vào `e2e/roles.ts` (thêm khóa vai trò hoặc "persona") trước khi viết nhóm RBAC và REG.

### 3.3. Bộ dữ liệu nền (golden dataset)

Dựng một lần bằng script/`globalSetup`, lưu thành snapshot CSDL (template database) để khôi phục nhanh trước mỗi lần chạy đầy đủ. Mã dùng tiền tố `E2E-` để không đụng dữ liệu mẫu `MAU-`/`mau.*` của `scripts/sample-data.sql`.

| Đối tượng | Dữ liệu |
|---|---|
| Đơn vị | K1; N2+N3 |
| Tòa | `E2E-K1` (cọc 1.000.000 đ, thuộc K1); `E2E-N2` (cọc 2.000.000 đ, thuộc N2+N3) |
| Loại phòng | `E2E-4G` (4 giường, giá HK 3.000.000, giá hè 1.500.000); `E2E-6G` (6 giường, giá khác để kiểm chênh lệch khi chuyển) |
| Phòng | `E2E-A101` (K1, nam, 4G, **3/4 giường có người**, có trưởng phòng); `E2E-A102` (K1, nam, 4G, trống hoàn toàn); `E2E-A103` (K1, nữ, 4G, trống); `E2E-A104` (K1, nam, Bảo trì); `E2E-B201` (N2, nam, 6G, trống) |
| Học kỳ | Học kỳ hiện tại đang diễn ra; học kỳ kế tiếp có đợt đăng ký **đang mở** tại K1; học kỳ hè có đợt chưa mở |
| Sinh viên | `e2e.sv.01..03` ở A101 (hợp đồng ACTIVE); `e2e.sv.04..12` chưa có hợp đồng, đủ nam/nữ; 1 sinh viên đã trả phòng có công nợ |
| Đơn giá | Điện 3.000 đ/kWh, nước 15.000 đ/m³, hiệu lực từ đầu kỳ |
| Cấu hình | `system_config`: hạn kích hoạt 7 ngày, ngưỡng sắp hết hạn 15, tự đóng ticket 3 ngày, hạn hóa đơn 7 ngày |

### 3.4. Điều kiện kỹ thuật cần quyết định trước khi viết test

| # | Vấn đề | Đề xuất | Cần ai quyết |
|---|---|---|---|
| D1 | Gọi API có Bearer: token chỉ nằm trong bộ nhớ trình duyệt (NFR-SEC-04), `storageState` không chứa token | Tạo client Keycloak `dms-e2e` **chỉ có trong realm E2E** bật Direct Access Grants để helper API lấy token theo từng persona; không bật ở production | Thành viên phụ trách Keycloak |
| D2 | Kiểm tác vụ định kỳ cần đẩy lùi thời gian | Profile `e2e` đặt cron mỗi phút qua biến môi trường (đã cấu hình được theo TV1) + helper SQL lùi `created_at`/`assigned_at` của bản ghi E2E | Backend |
| D3 | Cách đặt lại dữ liệu | Snapshot CSDL (template) khôi phục trước lần chạy đầy đủ; trong mỗi test tạo dữ liệu riêng bằng API với mã duy nhất `E2E-<runId>-<n>` | Nhóm |
| D4 | Selector ổn định cho Ant Design | Ưu tiên `getByRole`/`getByLabel`; khi không đủ, thêm `data-testid` từ một registry duy nhất (tuân quy tắc không hardcode, không tạo bản sao chuỗi trong component) | Frontend |
| D5 | Kiểm hóa đơn/ngày cần "hôm nay" cố định | Dùng `page.clock` của Playwright cho hiển thị nhãn; với logic server dùng D2 | Nhóm |

### 3.5. Cấu trúc mã đề xuất

```
DMS/frontend/e2e/
  roles.ts                 # persona + biến môi trường
  auth.setup.ts            # đăng nhập từng persona, lưu e2e/.auth/<persona>.json
  fixtures/
    api.ts                 # lấy token (D1), gọi API, factory dữ liệu
    db.ts                  # helper SQL: lùi thời gian, đọc số liệu đối chiếu
    data.ts                # mã/tiền tố dữ liệu E2E, hằng số số tiền
  pages/                   # page object nhỏ cho form/bảng dùng chung (CrudPage, ConfirmDialog)
  anonymous/  student/  staff/  technician/  admin/
  flows/                   # nhóm XF nhiều vai trò
  security/                # nhóm SEC
```

Test nhiều vai trò trong một luồng dùng nhiều `browser.newContext({ storageState })` chứ không đăng nhập lại.

---

## 4. Lộ trình triển khai

| Pha | Nội dung | Đầu ra | Điều kiện hoàn thành |
|---|---|---|---|
| **0. Hạ tầng** | Quyết định D1–D5; script tạo tài khoản + golden dataset; `fixtures/api.ts`, `fixtures/db.ts`; mở rộng `roles.ts`; `e2e/README.md` cập nhật | Chạy được `npm run e2e` trên môi trường E2E sạch | Smoke AUTH-01 chạy xanh với cả 4 vai trò |
| **1. Smoke + đăng nhập + RBAC** | Nhóm AUTH, RBAC, UI-01..05, SEC-03..06 | Tập `@smoke` < 5 phút | Chạy xanh 3 lần liên tiếp không flaky |
| **2. Luồng lõi** | Nhóm ROOM, REG, CON (kích hoạt, chuyển, gia hạn, trả phòng), XF-01 | AT-01, 02, 04, 06, 08, 09, 17, 18, 32, 33 | Các AT liệt kê ở cột truy vết (mục 6) xanh |
| **3. Sửa chữa và tài chính** | Nhóm MNT, BIL, cọc, XF-02 | AT-10..16, 19..23 | Số tiền khớp tuyệt đối (làm tròn 33.333/33.333/33.334) |
| **4. Hè, tác vụ định kỳ, thông báo, dashboard** | Nhóm SUM, JOB, NOT | AT-05, 12, 24..26 | Job idempotent chạy lại không đổi kết quả |
| **5. Hồi quy và CI** | UI mobile, `@axe`, job CI, báo cáo, ổn định hóa test flaky | Workflow CI `e2e` | PR vào `develop` chạy `@smoke`; đêm chạy toàn bộ |

Ước lượng thô theo tình trạng dự án hiện tại: pha 0 và 1 là điều kiện tiên quyết cho mọi pha sau; pha 2–4 có thể chia cho các thành viên theo module (mỗi người sở hữu một thư mục con để không xung đột, giống cách chia file route theo vai trò).

### 4.1. Tích hợp CI

- Job `e2e` trong `.github/workflows/ci.yml`: dựng PostgreSQL + Keycloak (service container hoặc compose), nạp realm + golden dataset, chạy backend nền (profile `e2e`), `npx playwright install --with-deps chromium`, `npx playwright test`.
- PR vào `develop`: `--grep @smoke`. Hàng đêm và PR `develop` → `main`: toàn bộ trừ `@slow`; `@slow` chạy tuần.
- Artifact: `playwright-report/`, `test-results/` (trace, video, ảnh) khi fail. `retries: 1` trong CI; test fail sau retry là lỗi thật, test chỉ xanh khi retry phải được ghi nhận là flaky và sửa trong tuần.
- Secrets E2E đặt ở GitHub Secrets, không in ra log.

### 4.2. Quy tắc viết test (bắt buộc)

1. Mỗi test độc lập: tự tạo dữ liệu bằng API với mã duy nhất, không phụ thuộc thứ tự chạy (trừ `@concurrency` đặt `describe.configure({ mode: 'serial' })`).
2. Không `waitForTimeout` cố định; dùng assertion có tự chờ và `expect.poll` cho kết quả bất đồng bộ (job, thông báo).
3. Đối chiếu kết quả ở **hai nơi** khi là tiền hoặc trạng thái: hiển thị UI **và** dữ liệu từ API/CSDL.
4. Số tiền kiểm bằng số nguyên đồng, không so chuỗi đã định dạng; kiểm định dạng ở test riêng (BIL-19).
5. Lỗi nghiệp vụ kiểm theo `errorCode` ở API và theo **thông báo tiếng Việt** ở UI (UI không được hiện `message` thô). Mã lỗi lấy từ API-Spec mục 5; mã chưa có trong catalogue ghi rõ "cần đối chiếu".
6. Không đặt mật khẩu, token, secret vào mã, log hay ảnh chụp có thể commit.
7. Tuân quy tắc không hardcode của dự án: số tiền/hạn mức lấy từ `fixtures/data.ts` hoặc đọc từ `system_config`, không rải số trong từng test.

---

## 5. Testcase chi tiết

Cách đọc: **TC** = mã; **Loại** = UI/API/UI+API/TAY; **Ưu** = P0/P1/P2; **Tham chiếu** = yêu cầu hoặc AT trong SRS. Nhãn nút/trường mô tả theo nghiệp vụ; khi viết mã phải đối chiếu với chữ thực tế trên giao diện. Cột "Bước" dùng `→` giữa các thao tác.

### 5.1. AUTH, Đăng nhập, phiên, đổi mật khẩu (FR-ACC-01..03, AT-27, 28, 30, 31)

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-AUTH-01 | Đăng nhập thành công từng vai trò | 4 tài khoản đã đổi mật khẩu | Mở `/login` → bấm Đăng nhập → nhập tài khoản/mật khẩu tại Keycloak → xác nhận | Chuyển về đúng trang chủ vai trò: Student `/student`, Staff `/staff/dashboard`, Technician `/technician/tasks`, Admin `/admin/users`; có tên người dùng trên giao diện; `GET /api/auth/me` trả đúng `role` và `permissions` | UI | P0 | FR-ACC-01, AT-27 |
| E2E-AUTH-02 | Sai mật khẩu | Tài khoản hợp lệ | Đăng nhập với mật khẩu sai | Ở lại trang Keycloak, báo sai thông tin; không vào được cổng; không có token | UI | P0 | FR-ACC-01 |
| E2E-AUTH-03 | Tài khoản không tồn tại | — | Đăng nhập với tên lạ | Báo sai thông tin, thông báo không phân biệt "sai tên" hay "sai mật khẩu" | UI | P1 | FR-ACC-01 |
| E2E-AUTH-04 | Khóa tạm sau 5 lần sai | Tài khoản riêng dùng một lần | Nhập sai liên tiếp 5 lần → nhập đúng mật khẩu | Lần thứ 6 (đúng mật khẩu) vẫn bị từ chối vì khóa tạm; không kiểm việc chờ 15 phút | UI | P2 `@slow` | FR-ACC-02 |
| E2E-AUTH-05 | Mật khẩu tạm buộc đổi lần đầu | Tài khoản vừa tạo/reset (mật khẩu tạm) | Đăng nhập bằng mật khẩu tạm | Bị đưa tới bước cập nhật mật khẩu của Keycloak; chưa vào được cổng cho tới khi hoàn tất | UI | P0 | FR-ACC-03 |
| E2E-AUTH-06 | Chính sách mật khẩu mới | Như AUTH-05 | Thử lần lượt: 7 ký tự; 8 ký tự chỉ chữ; 65 ký tự; trùng mật khẩu hiện tại; rồi mật khẩu hợp lệ (8–64, có chữ và số) | Bốn trường hợp đầu bị từ chối kèm thông báo; trường hợp hợp lệ vào được cổng đúng vai trò | UI | P1 | FR-ACC-03 |
| E2E-AUTH-07 | Giữ phiên khi tải lại trang | Đã đăng nhập | F5 ở trang bất kỳ | Vẫn đăng nhập (SSO), không bị đưa ra `/login`; dữ liệu trang tải lại | UI | P0 | NFR-SEC-04 |
| E2E-AUTH-08 | Đăng xuất | Đã đăng nhập | Bấm Đăng xuất | Về `/login` qua logout Keycloak; Back của trình duyệt vào `/admin/users` bị chuyển về đăng nhập; gọi API bằng token cũ nhận 401 | UI+API | P0 | FR-ACC-01, AT-30 |
| E2E-AUTH-09 | Truy cập khi chưa đăng nhập | Phiên trống | Mở trực tiếp từng cổng `/student`, `/staff/dashboard`, `/technician/tasks`, `/admin/users` | Cả bốn chuyển về đăng nhập; sau khi đăng nhập quay về đúng đích đã yêu cầu | UI | P0 | FR-ACC-01 |
| E2E-AUTH-10 | Sai cổng theo vai trò | Đăng nhập Student | Mở `/admin/users`, `/staff/dashboard`, `/technician/tasks`; lặp lại cho Staff, Technician, Admin với cổng khác | Chuyển về **trang chủ của chính vai trò**, không hiện trang 403 | UI | P0 | FR-ACC-01, UI 27 |
| E2E-AUTH-11 | Token sai issuer/audience/chữ ký/hết hạn | Có token hợp lệ làm mẫu | Gọi `GET /api/auth/me` với: token sửa chữ ký; token của realm khác; token hết hạn; không có header | 401 cho cả bốn; có `requestId` | API | P0 | AT-30, NFR-SEC-04 |
| E2E-AUTH-12 | Subject chưa liên kết tài khoản DMS | Người dùng Keycloak tồn tại nhưng không có `app_user` liên kết, trùng `username` với một tài khoản DMS | Đăng nhập | Báo `IDENTITY_NOT_LINKED`, không tự nhận hồ sơ/quyền theo `username` | UI+API | P0 | AT-31, NFR-SEC-01 |
| E2E-AUTH-13 | Giả vai trò/trường lạ từ client | Đăng nhập Student | Gọi API với body có `role: "ADMIN"`, `userId` của người khác, `mine=false`; gọi `/api/.../me` kèm owner khác | Trường lạ bị từ chối (400); quyền lấy từ CSDL; không nâng được quyền; `/me` luôn ép người hiện tại | API | P0 | AT-27 |
| E2E-AUTH-14 | Khóa tài khoản làm mất hiệu lực phiên cũ | Student đang đăng nhập ở context A | Admin khóa tài khoản đó → Student thao tác ở A → Admin mở khóa → Student thao tác tiếp ở A → Student đăng nhập mới | Sau khóa: lần gọi API kế tiếp bị chặn, UI đưa về đăng nhập; sau mở khóa: phiên cũ **vẫn** vô hiệu; chỉ đăng nhập mới dùng được | UI+API | P0 | AT-28, FR-ACC-06 |
| E2E-AUTH-15 | Reset mật khẩu thu hồi mọi phiên | Student có hai phiên (hai context) | Admin reset mật khẩu → cả hai phiên gọi API | Cả hai bị từ chối; đăng nhập bằng mật khẩu tạm buộc đổi (AUTH-05) | UI+API | P0 | AT-28, FR-ACC-07 |
| E2E-AUTH-16 | Phiên hết hạn hiển thị cảnh báo | Đăng nhập | Giả lập API trả 401 (route mock) hoặc thu hồi phiên → thao tác | Về `/login?reason=expired`, có cảnh báo "Phiên đã hết hạn..." | UI | P1 | UI 27.2 |
| E2E-AUTH-17 | Đăng xuất đồng bộ nhiều tab | Hai tab cùng đăng nhập | Đăng xuất ở tab 1 | Tab 2 tự chuyển về đăng nhập (tín hiệu nhiều tab) | UI | P1 | FR-ACC-01 |
| E2E-AUTH-18 | Trang Quên mật khẩu | — | Mở `/forgot-password` | Hiển thị hướng dẫn liên hệ người quản lý; **không có** ô nhập email gửi liên kết | UI | P2 | SRS 3.1 (quy tắc phiên) |

### 5.2. ACC, Tài khoản, hồ sơ, import, audit (FR-ACC-04..09, AT-29)

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-ACC-01 | Student xem và sửa hồ sơ | Student đăng nhập | Vào Hồ sơ → sửa số điện thoại, quê quán, liên hệ khẩn cấp → Lưu → tải lại | Lưu thành công, giá trị còn sau tải lại; **form Sửa nạp đúng dữ liệu cũ trước khi chỉnh** | UI | P0 | FR-ACC-04 |
| E2E-ACC-02 | Hồ sơ: lỗi validate hiển thị theo trường | Như trên | Nhập số điện thoại sai định dạng → Lưu | Lỗi hiện dưới đúng trường; form **không đóng**; nút Lưu có trạng thái đang lưu rồi bật lại | UI | P1 | UI 27, CLAUDE.md (form) |
| E2E-ACC-03 | Trường định danh không sửa được | Student, và Staff sửa hồ sơ sinh viên | Mở form hồ sơ → tìm ô mã SV, họ tên, giới tính; gọi API với `studentCode`/`fullName`/`gender` mới | UI: ba trường không có hoặc chỉ đọc; API: từ chối, dữ liệu không đổi | UI+API | P0 | FR-ACC-04 |
| E2E-ACC-04 | Chỉ Student có quê quán/liên hệ khẩn cấp | Lần lượt đăng nhập 4 vai trò | Mở Hồ sơ | Student có ô quê quán và liên hệ khẩn cấp; Staff, Technician, Admin chỉ sửa số điện thoại | UI | P1 | CLAUDE.md "Common flows" |
| E2E-ACC-05 | Admin tạo tài khoản Staff | Admin | Người dùng → Thêm → nhập tên đăng nhập, vai trò STAFF, đơn vị K1 → Lưu | Hộp thoại hiện mật khẩu tạm **một lần**; đóng rồi mở lại/tải lại không còn; mật khẩu tạm không có trong phản hồi GET sau đó, không có trong audit | UI+API | P0 | FR-ACC-06, SEC-05 |
| E2E-ACC-06 | Tạo Staff thiếu đơn vị | Admin | Tạo STAFF, bỏ trống đơn vị | Lỗi tại trường đơn vị, không tạo tài khoản | UI | P1 | FR-ACC-06 |
| E2E-ACC-07 | Đổi vai trò/đơn vị thu hồi token | Staff đang đăng nhập | Admin đổi đơn vị của Staff sang N2+N3 → Staff gọi API bằng phiên cũ → đăng nhập lại | Phiên cũ bị chặn; phiên mới có phạm vi N2+N3; Keycloak đồng bộ vai trò | UI+API | P0 | FR-ACC-06 |
| E2E-ACC-08 | Giới hạn đổi vai trò | Admin | Đổi vai trò chính tài khoản mình; chuyển hồ sơ STUDENT sang STAFF; chuyển Staff sang ADMIN | Hai trường hợp đầu bị từ chối; sang ADMIN thành công và xóa đơn vị + cờ tổng hợp | UI+API | P1 | SRS phân quyền |
| E2E-ACC-09 | Khóa và mở khóa tài khoản | Admin | Khóa Staff → thử đăng nhập mới → mở khóa → đăng nhập mới | Khi khóa không đăng nhập được; mở khóa cho phép đăng nhập mới; có bản ghi audit mỗi thao tác | UI | P0 | FR-ACC-06 |
| E2E-ACC-10 | Admin reset mật khẩu | Admin, Student | Reset → ghi nhận mật khẩu tạm → đăng nhập Student bằng mật khẩu tạm | Mật khẩu tạm trả một lần; buộc đổi; bộ đếm sai/khóa tạm được xóa (nếu Student đang bị khóa tạm thì đăng nhập được) | UI | P0 | FR-ACC-07 |
| E2E-ACC-11 | Staff reset: trong phạm vi | Sinh viên có đơn trong đợt đang mở tại K1 (hoặc phân phòng hiệu lực tại K1) | Staff K1 → Sinh viên → Reset mật khẩu | Thành công | UI | P0 | FR-ACC-07, BR-07 |
| E2E-ACC-12 | Staff reset: ngoài phạm vi | Sinh viên chỉ có đơn đợt đã đóng/phân phòng đã kết thúc; sinh viên của N2; kể cả Staff có quyền đọc liên đơn vị | Staff K1 (và Staff tổng hợp) thử reset | Bị từ chối (404/403 theo API-Spec); không đổi mật khẩu | UI+API | P0 | AT-29 |
| E2E-ACC-13 | Import Excel: xem trước (dry-run) | Admin; file mẫu tải từ hệ thống, thêm 5 dòng hợp lệ, 1 dòng sai định dạng, 1 mã trùng có sẵn | Người dùng/Import → tải mẫu → chọn file → Xem trước | Báo lỗi **theo từng dòng** (số dòng + lý do); mã đã tồn tại bị đánh dấu bỏ qua; chưa tạo tài khoản nào | UI | P0 | FR-ACC-05 |
| E2E-ACC-14 | Import Excel: thực hiện | Như ACC-13 | Xác nhận import | Chỉ 5 dòng hợp lệ được tạo; mỗi tài khoản có mật khẩu tạm ngẫu nhiên và required action đổi mật khẩu; có thể đăng nhập bằng mật khẩu tạm → buộc đổi | UI+API | P0 | FR-ACC-05 |
| E2E-ACC-15 | Import Excel: file không hợp lệ | Admin | Thử: file > 5 MB; đuôi không phải .xlsx; file có công thức; > 5.000 dòng; thiếu cột | Mỗi trường hợp bị từ chối bằng thông báo rõ; không tạo dữ liệu | UI+API | P1 | FR-ACC-05, SRS 5.3 |
| E2E-ACC-16 | Staff không tạo được sinh viên | Staff | Mở màn Sinh viên; gọi `POST /api/.../save-or-update` tạo sinh viên | Không có nút Thêm; API trả 403 | UI+API | P0 | CLAUDE.md "Tài khoản/hồ sơ" |
| E2E-ACC-17 | Cấp quyền tổng hợp liên đơn vị | Admin, Staff K1 | Admin bật quyền tổng hợp cho Staff K1 → Staff đăng nhập lại | Staff đọc được dữ liệu N2 (danh sách, dashboard toàn KTX); phiên cũ bị thu hồi; **ghi** vào dữ liệu N2 vẫn 404 | UI+API | P0 | FR-ACC-08, BR-07 |
| E2E-ACC-18 | Audit log: xem và lọc | Admin; đã có vài thao tác từ các test trước | Nhật ký → lọc theo người, loại thao tác, khoảng thời gian (bao gồm ngày cuối) | Kết quả đúng bộ lọc; phân trang/đếm ở server; chi tiết hiển thị `oldValue`/`newValue`; không có nút sửa/xóa; không có mật khẩu/secret | UI | P1 | FR-ACC-09 |
| E2E-ACC-19 | Audit ghi thao tác nhạy cảm | Admin | Tạo/khóa tài khoản, đổi ma trận quyền, đổi cấu hình | Mỗi thao tác sinh đúng một bản ghi audit với actor là Admin thật | UI+API | P1 | FR-ACC-09, BR-09 |

### 5.3. RBAC, Phân quyền, phạm vi, ma trận quyền động (FR-ACC-08, 10, BR-07, AT-06, AT-27)

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-RBAC-01 | Menu theo vai trò và quyền | Bốn vai trò | Liệt kê menu từng vai trò | Mỗi vai trò chỉ thấy mục có quyền; không có mục của vai trò khác | UI | P0 | SRS phân quyền |
| E2E-RBAC-02 | Vào URL không có quyền | Staff bị thu hồi quyền một màn (RBAC-07) | Mở thẳng URL màn đó | Chuyển về trang chủ vai trò, không thấy dữ liệu | UI | P0 | FR-ACC-10 |
| E2E-RBAC-03 | Staff K1 đọc dữ liệu N2 | Có đơn, phòng, hợp đồng, hóa đơn, ticket của N2 với ID đã biết | Staff K1 gọi `GET` chi tiết từng loại | **404** cho tất cả (không lộ sự tồn tại); danh sách và tổng số (`total`) không chứa dữ liệu N2 | API | P0 | AT-06, BR-07, NFR-SEC-02 |
| E2E-RBAC-04 | Staff K1 ghi dữ liệu N2 | Như trên | `save-or-update`, `approve`, `assign`, `activate`, `DELETE` lên dữ liệu N2 | 404 cho tất cả; dữ liệu không đổi | API | P0 | AT-06 |
| E2E-RBAC-05 | Technician không vào dữ liệu nghiệp vụ | Technician | Gọi API phòng, hợp đồng, hóa đơn, đơn đăng ký (ghi và đọc) | 403 | API | P0 | SRS phân quyền |
| E2E-RBAC-06 | Student không thấy dữ liệu người khác | Hai sinh viên A, B | A gọi chi tiết hợp đồng, hóa đơn phí phòng, ticket, đơn của B; gọi `/me` kèm ID của B | 404/403; `/me` luôn trả dữ liệu của A | API | P0 | NFR-SEC-02 |
| E2E-RBAC-07 | Thu hồi quyền có hiệu lực ngay | Admin, Staff đang đăng nhập có quyền `ROOM_UPDATE` | Admin bỏ `ROOM_UPDATE` của STAFF → Lưu → Staff gọi sửa phòng ở lần tiếp theo | 403 ngay ở lần gọi kế tiếp mà không cần đăng nhập lại; nút Sửa phòng biến mất sau khi `/me` làm mới; với **mọi** tài khoản STAFF | UI+API | P0 | FR-ACC-10 |
| E2E-RBAC-08 | Ma trận quyền: thao tác trên giao diện | Admin | `/admin/role-permissions` → chọn STAFF → tìm màn theo tên → bấm "chọn tất cả quyền phù hợp" → Khôi phục mặc định vào bản nháp → Hủy | Bản nháp thay đổi theo thao tác; Hủy bỏ thay đổi, dữ liệu CSDL không đổi; danh mục màn/nhóm/nhãn lấy từ backend | UI | P1 | FR-ACC-10 |
| E2E-RBAC-09 | ADMIN bất biến | Admin | Chọn ADMIN trong ma trận; gọi `PUT /api/admin/role-permissions/ADMIN` | UI: mọi ô bị khóa/không sửa được; API: 403 `ADMIN_PERMISSIONS_IMMUTABLE`; Admin vẫn có đủ 105 quyền đã đăng ký | UI+API | P0 | FR-ACC-10 |
| E2E-RBAC-10 | Ràng buộc quyền READ | Admin | Bỏ `PROFILE_READ` của STUDENT; thêm một quyền ghi (CREATE/UPDATE/DELETE) của một màn mà không có READ của màn đó | Cả hai bị từ chối (UI báo lỗi hoặc khóa, API 400/409) | UI+API | P0 | FR-ACC-10 |
| E2E-RBAC-11 | Xung đột phiên bản khi lưu | Hai context Admin cùng mở ma trận STAFF | A lưu thay đổi → B (bản cũ) lưu | B nhận 409 với thông báo bản đã cũ; **bản nháp của B được giữ**; không tự động gửi lại | UI | P0 | FR-ACC-10 |
| E2E-RBAC-12 | Lưu không thay đổi | Admin | Mở ma trận → không đổi → Lưu | Không tăng version, không ghi audit | UI+API | P1 | FR-ACC-10 |
| E2E-RBAC-13 | Audit trước/sau của ma trận | Admin | Đổi một quyền → Lưu → xem nhật ký | Có bản ghi cùng transaction với dữ liệu trước và sau | UI | P1 | FR-ACC-10 |
| E2E-RBAC-14 | Staff không đổi được ma trận | Staff | Mở `/admin/role-permissions`; gọi `PUT` | Chuyển về trang chủ; API 403 | UI+API | P0 | UC-29 |
| E2E-RBAC-15 | Quyền ghi tách khỏi quyền đọc tổng hợp | Staff có crossUnit | Gọi ghi lên dữ liệu đơn vị khác | 404; quyền đọc tổng hợp không mở ghi | API | P0 | BR-07 |
| E2E-RBAC-16 | Admin thao tác mọi đơn vị nhưng vẫn bị ràng buộc | Admin | Duyệt đơn N2; thử phân giường K1 cho đơn nguyện vọng N2; thử chuyển trạng thái sai sơ đồ | Duyệt thành công; phân sai đơn vị `BED_UNIT_MISMATCH`; chuyển sai sơ đồ bị từ chối; audit ghi đúng Admin | API | P0 | BR-07, FR-REG-08 |

### 5.4. ROOM, Tòa, phòng, giường, trưởng phòng, học kỳ (FR-ROOM-01..11, AT-21, 33)

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-ROOM-01 | Admin quản lý tòa và mức cọc | Admin | Tạo tòa thuộc đơn vị với mức cọc 1.500.000 | Lưu được; hiển thị đúng | UI | P1 | FR-ROOM-01 |
| E2E-ROOM-02 | Đổi mức cọc chỉ áp dụng cho kích hoạt sau đó | Hợp đồng ACTIVE đã thu cọc 1.000.000 (K1); hợp đồng PENDING khác cùng tòa | Admin đổi cọc tòa K1 lên 1.200.000 → kích hoạt hợp đồng PENDING | Hợp đồng ACTIVE cũ giữ 1.000.000; hợp đồng mới yêu cầu 1.200.000; sổ cọc cũ không đổi | UI+API | P0 | FR-ROOM-01, BR-10 |
| E2E-ROOM-03 | Tạo loại phòng với giá theo học kỳ | Staff K1 | Tạo loại phòng 4 giường, giá HK 3.000.000 và giá hè 1.500.000 | Lưu được; hai giá khác nhau hiển thị đúng | UI | P1 | FR-ROOM-02 |
| E2E-ROOM-04 | Đổi giá không ảnh hưởng phân phòng hiện hành | Hợp đồng ACTIVE với đơn giá 3.000.000 | Staff đổi giá loại phòng lên 3.500.000 | Lịch sử phân phòng của hợp đồng cũ vẫn 3.000.000; phân phòng mới lấy 3.500.000 | UI+API | P0 | FR-ROOM-02, BR-09 |
| E2E-ROOM-05 | Sửa số giường của loại phòng đang dùng | Loại phòng đã gán cho ít nhất một phòng | Sửa `bed_count` | 409 `RESOURCE_IN_USE`; số giường không đổi; loại phòng **chưa** gán cho phòng nào thì sửa được | UI+API | P0 | AT-33, FR-ROOM-02 |
| E2E-ROOM-06 | Tạo phòng tự sinh giường | Staff K1 | Tạo phòng với loại 4 giường | Có đúng 4 giường; mã phòng trùng bị từ chối; thiếu trường bắt buộc báo lỗi theo trường | UI | P0 | FR-ROOM-03, 04 |
| E2E-ROOM-07 | Sửa phòng không phá chỗ ở hiện có | Phòng A101 đang có người | Thử đổi tòa; thử đổi loại phòng sang loại ít giường hơn số người đang ở; thử đổi giới tính phòng | Bị từ chối nếu làm sai chỗ ở hiện có; form Sửa nạp đúng dữ liệu cũ | UI+API | P0 | CLAUDE.md "Phòng và đăng ký" |
| E2E-ROOM-08 | Thêm/bớt giường | Phòng trống | Thêm một giường; bớt giường chưa từng có người; bớt giường có lịch sử phân phòng; sửa giường đổi phòng nguồn | Thêm/bớt giường trống được; giường có lịch sử bị từ chối (409); giường chỉ sửa được mã, không đổi phòng | UI+API | P1 | FR-ROOM-04, 06 |
| E2E-ROOM-09 | Đặt phòng Bảo trì/Ngừng khi còn người ở | Phòng A101 đang có phân phòng hiệu lực | Đặt Bảo trì không lý do; đặt Bảo trì có lý do | Bị từ chối do còn phân phòng hiệu lực (BR-01); phòng trống: bắt buộc lý do, thành công thì giường tính vào nhóm "bảo trì" riêng | UI+API | P0 | FR-ROOM-05, BR-01 |
| E2E-ROOM-10 | Đặt giường Hỏng khi còn người | Giường đang ở | Đặt Hỏng | Bị từ chối; giường trống đặt được, không phân được cho sinh viên nữa | UI+API | P0 | FR-ROOM-05 |
| E2E-ROOM-11 | Không xóa phòng/giường đã từng có phân phòng | Phòng có lịch sử | Bấm Xóa; gọi `DELETE` | 409; chỉ chuyển "Ngừng sử dụng"; phòng chưa từng có dữ liệu xóa được | UI+API | P0 | FR-ROOM-06 |
| E2E-ROOM-12 | Tra cứu phòng: lọc, phân trang, đếm | Dữ liệu nền | Lọc theo tòa, tầng, loại, giới tính, "còn chỗ"; đổi `page`/`size`; sắp xếp theo cột; thử `size=101` | Số giường trống/đang giữ/đang ở **tính ra** đúng với dữ liệu; `total` khớp số dòng; lọc/đếm áp dụng ở server (kiểm bằng request gửi đi); `size=101` bị từ chối | UI+API | P0 | FR-ROOM-07, CLAUDE.md |
| E2E-ROOM-13 | Staff chỉ thấy phòng đơn vị mình | Staff K1 | Tìm kiếm không lọc | Chỉ có phòng K1; tổng số không tính phòng N2 | UI+API | P0 | BR-07 |
| E2E-ROOM-14 | Student xem phòng của mình | Student ở A101 | Mở "Phòng của tôi" | Thấy phòng, giường, danh sách bạn cùng phòng (chỉ họ tên), trưởng phòng; không thấy thông tin nhạy cảm của bạn cùng phòng | UI | P0 | FR-ROOM-08 |
| E2E-ROOM-15 | Chỉ định trưởng phòng | Phòng có 3 sinh viên đang ở | Staff chỉ định A → chỉ định B → thử chỉ định sinh viên ngoài phòng | Mỗi phòng tối đa một trưởng phòng (B thay A, lịch sử lưu cả hai); sinh viên không ở phòng bị từ chối (BR-11) | UI+API | P0 | FR-ROOM-09, BR-11 |
| E2E-ROOM-16 | Trưởng phòng chuyển/trả phòng thì tự gỡ | A101 có trưởng phòng | Chuyển trưởng phòng sang phòng khác; kịch bản khác: trả phòng | Phòng A101 mất chỉ định, Staff thấy cảnh báo "chưa có trưởng phòng" trên danh sách và dashboard | UI | P0 | AT-21, FR-ROOM-10 |
| E2E-ROOM-17 | Quản lý học kỳ | Staff | Tạo học kỳ HK1/HK2/Hè với ngày bắt đầu, kết thúc; thử ngày kết thúc trước ngày bắt đầu | Hợp lệ lưu được; ngày sai báo lỗi theo trường; sửa giữ đúng ngày đã lưu (không lệch múi giờ) | UI | P1 | FR-ROOM-11 |
| E2E-ROOM-18 | Danh mục dùng chung (CRUD smoke) | Admin/Staff | Với từng danh mục (khoa, khóa, trường, quê quán, diện ưu tiên, tiện ích, tầng, loại sự cố, vị trí kho...): tạo → tìm → sửa → xóa bản ghi chưa dùng → xóa bản ghi đã dùng | Tạo/sửa/xóa hoạt động; bản ghi đang được dùng báo 409 xung đột rõ ràng; phân trang ở server | UI | P2 | SRS bổ sung danh mục (04/11/2026) |

### 5.5. REG, Đợt đăng ký, đơn, phân giường (FR-REG-01..12, AT-01, 02, 04, 17, 32)

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-REG-01 | Tạo đợt đăng ký | Staff K1, học kỳ đã có | Tạo đợt: học kỳ, ngày mở/đóng, chỉ tiêu theo tòa, áp dụng K1 | Lưu được; ghi audit tạo; ngày đóng trước ngày mở bị từ chối | UI | P0 | FR-REG-01 |
| E2E-REG-02 | Sửa đợt đã mở | Đợt đang mở | Sửa ngày đóng; thử sửa học kỳ/đơn vị | Chỉ trường nghiệp vụ cho phép sửa được; ghi audit; trường bị cấm bị từ chối | UI+API | P1 | CLAUDE.md "Phòng và đăng ký" |
| E2E-REG-03 | Lookup đợt: chỉ lấy đợt đang mở | Một đợt mở, một đợt đã đóng, một đợt chưa mở | Student mở form soạn đơn → xem danh sách đợt | Chỉ thấy đợt đang mở | UI+API | P0 | FR-REG-02 |
| E2E-REG-04 | Technician xem đợt chỉ đọc | Technician K1 | Mở đợt của đơn vị mình; thử sửa | Xem được đợt đơn vị mình; không sửa được | UI+API | P2 | FR-REG-01 |
| E2E-REG-05 | Soạn đơn (DRAFT) | Student chưa có hợp đồng, đợt mở | Chọn đơn vị nguyện vọng, loại phòng, diện ưu tiên, ghi chú → Lưu nháp | Có đơn DRAFT; sửa được; mỗi sinh viên tối đa **một đơn mỗi đợt**: tạo đơn thứ hai trong cùng đợt bị từ chối | UI | P0 | FR-REG-02 |
| E2E-REG-06 | Chặn nộp khi đang có hợp đồng | Student có hợp đồng ACTIVE (và biến thể PENDING) | Soạn/nộp đơn đợt mới | Bị từ chối kèm gợi ý dùng gia hạn; không tạo đơn | UI+API | P0 | AT-17, FR-REG-03 |
| E2E-REG-07 | Nộp đơn và tính điểm ưu tiên | Đơn DRAFT có diện ưu tiên | Nộp | Trạng thái SUBMITTED; điểm tính theo trọng số cấu hình; Student thấy diện đã khai nhưng **không thấy trọng số/điểm tính**; sau SUBMITTED không sửa được | UI+API | P0 | FR-REG-04, 05 |
| E2E-REG-08 | Client không tự gửi điểm/trạng thái | Student | Gọi API soạn đơn với `status: "APPROVED"`, `priorityScore: 999` | Trường lạ bị từ chối; không đổi điểm/trạng thái | API | P0 | CLAUDE.md "Phòng và đăng ký" |
| E2E-REG-09 | Hủy đơn theo trạng thái | Các đơn DRAFT, SUBMITTED, APPROVED (chưa có hợp đồng) | Student hủy từng đơn | Cả ba hủy được → CANCELLED | UI | P0 | FR-REG-04 |
| E2E-REG-10 | Hủy đơn APPROVED đã có hợp đồng | Đơn đã sinh hợp đồng PENDING/ACTIVE | Mở đơn; gọi `cancel` | Nút Hủy **không hiện** (không có trong `allowedActions`); API 409 `APPLICATION_HAS_CONTRACT`; dữ liệu không đổi | UI+API | P0 | AT-32 |
| E2E-REG-11 | Staff xem danh sách đơn | Nhiều đơn khác điểm và thời điểm nộp | Mở danh sách mặc định; lọc; đổi trang | Mặc định sắp theo điểm giảm dần rồi thời điểm nộp tăng dần; chỉ đơn của đơn vị nguyện vọng K1; phân trang/đếm ở server | UI+API | P0 | FR-REG-05, BR-07 |
| E2E-REG-12 | Xác nhận diện ưu tiên | Đơn SUBMITTED có diện tự khai | Staff đánh dấu "không đúng" | Điểm tính lại theo xác nhận; thứ tự danh sách cập nhật | UI | P1 | FR-REG-06 |
| E2E-REG-13 | Duyệt đơn | Đơn SUBMITTED K1 | Staff K1 duyệt | APPROVED; Student nhận thông báo; chỉ duyệt được khi SUBMITTED (đơn DRAFT/đã duyệt gọi `approve` bị từ chối) | UI+API | P0 | FR-REG-07 |
| E2E-REG-14 | Từ chối đơn bắt buộc lý do | Đơn SUBMITTED | Từ chối không nhập lý do → nhập lý do | Không lý do: lỗi trường lý do, không đổi trạng thái; có lý do: REJECTED; Student thấy lý do | UI | P0 | FR-REG-07, 12 |
| E2E-REG-15 | Phân giường thành công | Đơn APPROVED nam K1; A102 trống | Mở đơn → danh sách gợi ý giường → chọn → xác nhận | Gợi ý chỉ gồm giường trống đúng đơn vị nguyện vọng, giới tính, loại phòng, phòng Hoạt động; thành công: phân phòng HOLD (đơn giá chụp từ loại phòng), hợp đồng PENDING, đơn ASSIGNED, Student nhận thông báo kèm hạn kích hoạt; Student thấy giường được giữ và hạn | UI+API | P0 | AT-01, FR-REG-08, UC-16 |
| E2E-REG-16 | Phân giường sai đơn vị | Đơn nguyện vọng N2; giường K1 | Gọi `assign` (cả bằng Admin) | `BED_UNIT_MISMATCH`; không có phân phòng nào được tạo | API | P0 | FR-REG-08 |
| E2E-REG-17 | Giường không hợp lệ | Giường phòng Bảo trì (A104), giường Hỏng, phòng nữ cho sinh viên nam | Gọi `assign` | Mỗi trường hợp bị từ chối, đơn giữ APPROVED, không thay đổi dữ liệu | API | P0 | BR-01 |
| E2E-REG-18 | Tranh chấp giường (đồng thời) | A101 còn đúng 1 giường; hai đơn APPROVED khác nhau | Hai Staff (hai context) gọi `assign` **cùng lúc** vào giường đó | Một thành công, một nhận 409 `BED_TAKEN` ("Giường vừa được phân, chọn giường khác"); phòng 4/4; không phân phòng thừa; đơn thất bại vẫn APPROVED | API `@concurrency` | P0 | AT-02, FR-REG-09, NFR-REL-01 |
| E2E-REG-19 | Gửi lặp yêu cầu phân phòng | Đơn đã ASSIGNED | Gọi `assign` lần hai cho cùng sinh viên/đơn | 409 (Policy hoặc ràng buộc); không có phân phòng/hợp đồng thứ hai | API | P0 | AT-04, FR-REG-10 |
| E2E-REG-20 | Staff hủy giữ chỗ | Đơn ASSIGNED, hợp đồng PENDING | Hủy giữ chỗ không lý do → có lý do | Không lý do: lỗi; có lý do: đơn → APPROVED, hợp đồng → CANCELLED, phân phòng đóng, giường trống lại, cùng một transaction; Student nhận thông báo | UI+API | P0 | FR-REG-11, UC-34 |
| E2E-REG-21 | Student tự hủy giữ chỗ | Đơn ASSIGNED | Student hủy giữ chỗ | Đơn → CANCELLED, hợp đồng → CANCELLED, giường trống | UI | P0 | FR-REG-11 |
| E2E-REG-22 | Student xem trạng thái đơn | Đơn ở từng trạng thái | Mở danh sách đơn | Hiển thị đúng nhãn trạng thái (`statusConfig`), lý do từ chối, giường giữ, hạn kích hoạt; chỉ thấy đơn của mình | UI | P1 | FR-REG-12 |
| E2E-REG-23 | Admin tạo đơn hộ sinh viên | Admin | Tạo đơn kèm `studentId`; Student gửi `studentId` của người khác | Admin thành công; Student bị từ chối/ID bị bỏ qua, đơn thuộc về chính Student | API | P1 | SRS phân quyền |
| E2E-REG-24 | Đơn của đơn vị khác | Đơn nguyện vọng N2 | Staff K1 mở/duyệt | 404 | API | P0 | AT-06 |

### 5.6. CON, Hợp đồng, kích hoạt, gia hạn, chuyển phòng, trả phòng (FR-CON-01..10, AT-01, 07..10, 18, 19)

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-CON-01 | Kích hoạt hợp đồng | Hợp đồng PENDING K1 | Staff mở hợp đồng → thấy mức cọc 1.000.000 điền sẵn → xác nhận thu đủ cọc | Hợp đồng ACTIVE, ghi `activated_at`; phân phòng HOLD → STAY **trên cùng dòng**; sổ cọc có giao dịch thu cọc +1.000.000 (người xác nhận, thời gian); Student thấy hợp đồng ACTIVE và đúng phòng | UI+API | P0 | AT-01, FR-CON-01, UC-32 |
| E2E-CON-02 | Cọc không đủ | Hợp đồng PENDING N2 (cọc 2.000.000) | Nhập 1.000.000 | Bị từ chối; hợp đồng vẫn PENDING; sổ cọc không có giao dịch | UI+API | P0 | AT-18, BR-10 |
| E2E-CON-03 | Kích hoạt hợp đồng đã bị tự hủy | Hợp đồng quá hạn đã CANCELLED (JOB-01) | Gọi/bấm kích hoạt | `CONTRACT_AUTO_CANCELLED`; không đổi dữ liệu | API | P0 | UC-32 (3a) |
| E2E-CON-04 | Bấm kích hoạt hai lần | Hợp đồng PENDING | Gửi hai yêu cầu kích hoạt gần như đồng thời | Chỉ một giao dịch thu cọc; yêu cầu còn lại 409; số dư cọc đúng bằng 1 lần | API `@concurrency` | P0 | NFR-REL-02, BR-10 |
| E2E-CON-05 | Xem hợp đồng | Student có hợp đồng ACTIVE; Staff cùng đơn vị | Mở chi tiết | Thời hạn, tiền cọc, trạng thái, lịch sử phân phòng (giường, phòng, từ/đến, đơn giá) hiển thị đủ; Staff thấy cùng dữ liệu | UI | P0 | FR-CON-05 |
| E2E-CON-06 | Nhãn "Sắp hết hạn" | Hợp đồng ACTIVE kết thúc sau 14, 15, 16 ngày | Mở danh sách/chi tiết | 14 và 15 ngày: có nhãn; 16 ngày: không nhãn; đổi ngưỡng trong `system_config` thì nhãn đổi theo mà **không có trường lưu sẵn** | UI+API | P1 | FR-CON-03, BR-08 |
| E2E-CON-07 | Nhãn "Quá hạn trả phòng" | Hợp đồng ACTIVE quá ngày kết thúc, chưa trả phòng | Mở danh sách | Nhãn hiển thị; hợp đồng **vẫn ACTIVE**, không tự thành EXPIRED | UI+API | P0 | FR-CON-04, BR-02 |
| E2E-CON-08 | Yêu cầu gia hạn và duyệt | Student có hợp đồng ACTIVE | Student gửi gia hạn: ngày kết thúc mới + lý do → Staff duyệt | Ngày kết thúc hợp đồng cập nhật; lưu mốc cũ/mới/người xử lý; Student nhận thông báo | UI | P0 | FR-CON-06, UC-19 |
| E2E-CON-09 | Gia hạn bị từ chối | Hai tình huống | Staff từ chối có lý do; ngày mới chồng lên hợp đồng khác | Từ chối thủ công ghi lý do; chồng ngày bị từ chối tự động | UI+API | P0 | AT-09, FR-CON-06 |
| E2E-CON-10 | Gia hạn không hợp lệ | Student | Ngày mới ≤ ngày hiện tại của hợp đồng; hợp đồng không ACTIVE; Student không có hợp đồng | Mỗi trường hợp báo lỗi rõ, không tạo yêu cầu | UI+API | P1 | FR-CON-06 |
| E2E-CON-11 | Yêu cầu chuyển phòng và duyệt | Student ở A101; giường trống A102 cùng K1 | Student gửi yêu cầu + lý do → Staff chọn giường đích A102 → duyệt | Phân phòng cũ đóng, mở phân phòng STAY mới với **đơn giá của phòng mới**; hợp đồng không đổi; ghi lý do, người duyệt, thời gian; hai khoảng phân phòng liên tiếp không chồng ngày | UI+API | P0 | AT-01, FR-CON-07, 08, UC-18 |
| E2E-CON-12 | Chuyển sang đơn vị khác | Giường trống ở B201 (N2) | Staff K1 duyệt với giường đích B201 | Bị từ chối (cùng đơn vị mới được chuyển); dữ liệu không đổi | UI+API | P0 | AT-08, BR-05 |
| E2E-CON-13 | Giường đích vừa bị chiếm | Hai thao tác trên cùng giường đích | Staff duyệt chuyển vào giường đích X trong khi thao tác phân giường khác đã chiếm X | Báo lỗi giường đã bị phân; **dữ liệu không đổi** (phân phòng cũ còn mở, không có dòng mới) | API `@concurrency` | P0 | FR-CON-08, UC-18 (2a), AT-07 |
| E2E-CON-14 | Hai yêu cầu chuyển chéo nhau | A ở giường 1, B ở giường 2 | Duyệt đồng thời A → giường 2 và B → giường 1 | Không deadlock; kết quả nhất quán (không giường nào có hai người, không sinh viên nào mất chỗ) | API `@concurrency` | P1 | FR-CON-08 (khóa theo `bed_id` tăng dần) |
| E2E-CON-15 | Từ chối chuyển phòng | Yêu cầu đang chờ | Từ chối không lý do → có lý do | Không lý do báo lỗi; có lý do: yêu cầu bị từ chối, phân phòng giữ nguyên | UI | P1 | FR-CON-07 |
| E2E-CON-16 | Trả phòng đúng hạn: hoàn cọc đủ | Hợp đồng ACTIVE đến/qua ngày kết thúc; cọc 1.000.000, không trừ | Staff → Trả phòng → ghi tình trạng tài sản "đạt" → xem cảnh báo công nợ → xác nhận hoàn cọc | Hợp đồng → EXPIRED; phân phòng đóng; giường trống; ghi giao dịch hoàn cọc 1.000.000; có `MoveOutRecord`; chỉ định trưởng phòng gỡ nếu có | UI+API | P0 | FR-CON-09, UC-33 |
| E2E-CON-17 | Trả phòng trước hạn | Hợp đồng ACTIVE còn hạn | Trả phòng | Hợp đồng → TERMINATED (không phải EXPIRED) | UI | P0 | FR-CON-09 |
| E2E-CON-18 | Trả phòng khi còn nợ phí phòng | Sinh viên có hóa đơn phí phòng UNPAID | Mở màn trả phòng | **Hiển thị cảnh báo** công nợ nhưng **không chặn**; sau trả phòng công nợ vẫn hiện cho Staff và Student | UI | P0 | AT-10, FR-CON-10 |
| E2E-CON-19 | Trả phòng khi bị trừ cọc 200.000 chưa bù | K1, cọc 1.000.000 → số dư 800.000 (có khoản bù chưa nộp) | Trả phòng | Hoàn cọc 800.000; ghi chú "được tất toán bằng số dư"; sau đó **không còn khoản cần bù**, không phát sinh nợ bù cọc | UI+API | P0 | AT-19, BR-10 |
| E2E-CON-20 | Trả phòng khi số dư cọc âm | Số dư cọc âm do trừ nhiều hơn cọc | Trả phòng | Không có giao dịch hoàn; số dư âm ghi thành công nợ, hiển thị cho Staff và Student | UI+API | P1 | FR-CON-09, 10 |
| E2E-CON-21 | Hoàn cọc tối đa một lần | Hợp đồng đã trả phòng | Gọi hoàn cọc lần hai | Bị từ chối (409), không có giao dịch `REFUND` thứ hai | API | P0 | BR-10 |
| E2E-CON-22 | Trả phòng khi còn ticket mở | Sinh viên có ticket OPEN/RESOLVED | Trả phòng | Ticket chuyển cho Staff nghiệm thu thay | UI+API | P1 | UC-33 |
| E2E-CON-23 | Cảnh báo hóa đơn điện nước khi trả phòng | Phòng còn hóa đơn điện nước chưa đóng | Trả phòng | Hiển thị cảnh báo nhưng không chặn | UI | P2 | Q-21 |
| E2E-CON-24 | Điều kiện gia hạn/chuyển phòng | Student chưa có hợp đồng ACTIVE | Gửi yêu cầu gia hạn/chuyển phòng | Bị từ chối | API | P1 | FR-CON-06, 07 |
| E2E-CON-25 | Hợp đồng ngoài đơn vị | Hợp đồng N2 | Staff K1 kích hoạt/trả phòng/duyệt gia hạn | 404 | API | P0 | AT-06 |

### 5.7. MNT, Sửa chữa, nghiệm thu, trừ cọc (FR-MNT-01..10, AT-11, 12, 14, 22)

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-MNT-01 | Student tạo ticket có ảnh | Student đang ở A101 | Nhập loại sự cố, mô tả, mức độ, đính 3 ảnh (JPEG/PNG/WebP ≤ 5 MB) → Gửi | Ticket OPEN gắn **phòng hiện tại do server xác định**; hiển thị 3 ảnh | UI | P0 | FR-MNT-01 |
| E2E-MNT-02 | Giới hạn ảnh | Như trên | Thử: 4 ảnh; ảnh > 5 MB; file `.txt` đổi đuôi `.jpg` (sai magic bytes); định dạng GIF | Mỗi trường hợp bị từ chối bằng thông báo rõ; **không tạo ticket và không còn file trên đĩa** (rollback cả dữ liệu và file) | UI+API | P0 | FR-MNT-01, SAD upload |
| E2E-MNT-03 | Student không có phòng | Student chưa có hợp đồng/đã trả phòng | Tạo ticket | Bị từ chối | UI+API | P0 | BR-06 |
| E2E-MNT-04 | Sửa ticket của mình | Ticket OPEN của Student | Sửa mô tả → Lưu; sau khi Staff giao việc thử sửa lại; thêm ảnh khi sửa | OPEN sửa được, giữ vị trí và ảnh cũ; sau ASSIGNED không sửa được; thêm ảnh khi sửa báo lỗi "chưa hỗ trợ" rõ ràng | UI+API | P1 | CLAUDE.md "Sửa chữa/thông báo" |
| E2E-MNT-05 | Staff tạo ticket khu vực chung và ticket phòng | Staff K1 | Tạo ticket chung (tòa, tầng); tạo ticket cho phòng thuộc đơn vị; tạo cho phòng N2 | Hai ticket đầu thành công; phòng N2: 404 | UI+API | P0 | FR-MNT-02 |
| E2E-MNT-06 | Giao việc | Ticket OPEN | Giao cho KTV cùng đơn vị; thử giao cho KTV N2 | ASSIGNED; KTV khác đơn vị bị từ chối | UI+API | P0 | FR-MNT-03, Q-08 |
| E2E-MNT-07 | Từ chối và hủy | Ticket OPEN | Staff từ chối không lý do → có lý do; Student hủy ticket OPEN của mình | Cần lý do khi từ chối; REJECTED/CANCELLED đúng; Student không hủy được ticket đã ASSIGNED | UI | P0 | FR-MNT-03 |
| E2E-MNT-08 | KTV xử lý đúng quy trình | KTV được giao ticket | Xem danh sách việc → ASSIGNED → IN_PROGRESS (ghi chú) → RESOLVED | Đúng trình tự; ghi chú tiến độ lưu; KTV khác mở ticket này nhận 404 và không cập nhật được | UI+API | P0 | FR-MNT-04 |
| E2E-MNT-09 | Chuyển trạng thái sai sơ đồ | Các ticket ở từng trạng thái | OPEN → CLOSED; ASSIGNED → RESOLVED; KTV gọi đóng ticket; RESOLVED → ASSIGNED | Tất cả bị từ chối; KTV **không tự đóng** ticket | API | P0 | AT-11, BR-06 |
| E2E-MNT-10 | Nghiệm thu | Ticket RESOLVED của Student | Chấp nhận → CLOSED; ở ticket khác: không chấp nhận không lý do rồi có lý do | CLOSED; không chấp nhận phải có lý do, quay IN_PROGRESS và ghi lịch sử | UI | P0 | FR-MNT-05 |
| E2E-MNT-11 | Nghiệm thu ticket chung/ticket Staff tạo | Ticket chung RESOLVED | Staff nghiệm thu | CLOSED; không phát sinh phí cho sinh viên | UI | P1 | FR-MNT-05, 09 |
| E2E-MNT-12 | Người yêu cầu đã trả phòng | Ticket RESOLVED, người yêu cầu đã rời | Staff nghiệm thu thay không lý do → có lý do | Bắt buộc lý do; CLOSED | UI | P1 | FR-MNT-07 |
| E2E-MNT-13 | Lịch sử ticket | Ticket đã qua nhiều trạng thái | Mở chi tiết (Student, Staff, KTV) | Timeline đủ mọi chuyển trạng thái với người, thời gian, lý do | UI | P0 | FR-MNT-08 |
| E2E-MNT-14 | Nguyên nhân hao mòn tự nhiên | Ticket phòng CLOSED | Staff chọn "hao mòn tự nhiên" + ghi chú | **Không** phát sinh giao dịch trừ cọc; số dư cọc không đổi | UI+API | P0 | AT-22, FR-MNT-09 |
| E2E-MNT-15 | Hư hỏng do sử dụng: chia cọc | A101 có 3 sinh viên đang ở; ticket CLOSED | Chọn "hư hỏng do sử dụng", chi phí 100.000, giữ cả 3 người | Ba khoản trừ **33.333 / 33.333 / 33.334**, tổng đúng 100.000; mỗi người có khoản bù cọc tương ứng, hạn nộp 7 ngày; mỗi người nhận thông báo | UI+API | P0 | AT-14, FR-MNT-10, UC-22 |
| E2E-MNT-16 | Chi phí không hợp lệ và bỏ bớt người | Như trên | Chi phí 0 hoặc âm; bỏ một sinh viên không lý do; bỏ một sinh viên có lý do | 0/âm bị từ chối; bỏ người không lý do bị từ chối; có lý do: chia đều cho 2 người còn lại, tổng khớp chi phí | UI+API | P0 | FR-MNT-10, Q-15 |
| E2E-MNT-17 | Chỉ tính sinh viên đang ở lúc tạo ticket | Sinh viên D vào phòng **sau** khi ticket được tạo | Mở danh sách chịu trách nhiệm | D không nằm trong danh sách; sinh viên đã chuyển đi sau đó vẫn nằm | UI+API | P1 | FR-MNT-10 |
| E2E-MNT-18 | Không trừ cọc hai lần | Ticket đã được xác định nguyên nhân | Gửi lại việc xác định nguyên nhân/trừ cọc | Bị từ chối hoặc idempotent; tổng trừ vẫn bằng chi phí | API | P0 | BR-10 |
| E2E-MNT-19 | Ảnh chỉ qua endpoint có kiểm quyền | Ticket có ảnh | Student khác, Staff đơn vị khác, người chưa đăng nhập mở URL ảnh | 404/403/401; chủ ticket, Staff cùng đơn vị, KTV được giao xem được | API | P0 | SAD upload |
| E2E-MNT-20 | Thông báo theo trạng thái ticket | Ticket đổi trạng thái | Kiểm hộp thư Student | Có thông báo cho mỗi lần đổi trạng thái liên quan | UI | P2 | FR-NOT-04 |

### 5.8. BIL, Điện nước, hóa đơn, sổ cọc (FR-BIL-01..13, AT-13, 15, 16, 20, 23)

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-BIL-01 | Đơn giá điện nước có hiệu lực | Staff K1 | Thêm giá điện/nước với ngày hiệu lực; sửa giá đã có hiệu lực; sửa giá tương lai | Thêm bản mới được; bản đã hiệu lực **không sửa/xóa**; bản tương lai sửa được | UI+API | P1 | FR-BIL-01 |
| E2E-BIL-02 | Chỉ số đầu kỳ tự lấy | Phòng đã có kỳ trước, cuối kỳ = 120 | Mở nhập chỉ số kỳ mới | Chỉ số đầu = 120 (chỉ đọc); nhập cuối = 150 → tiêu thụ 30 | UI | P0 | FR-BIL-02 |
| E2E-BIL-03 | Chỉ số cuối nhỏ hơn đầu | Như trên | Nhập cuối = 100 | Bị từ chối, không lưu | UI+API | P0 | AT-13, BR-03 |
| E2E-BIL-04 | Kỳ đầu và thiếu kỳ liền trước | Phòng chưa có lịch sử; phòng có lịch sử | Kỳ đầu: Staff nhập chỉ số đầu; phòng có lịch sử: nhập kỳ cách hai tháng | Kỳ đầu cho nhập đầu; nhảy kỳ bị từ chối (phải nhập đủ các kỳ liên tiếp) | UI+API | P1 | FR-BIL-02, BR-03 |
| E2E-BIL-05 | Một chỉ số cho mỗi (phòng, loại, kỳ) | Đã nhập điện kỳ T | Nhập lại điện kỳ T | Bị từ chối (hoặc chuyển thành sửa nếu chưa lập hóa đơn); không có hai bản ghi | API | P0 | BR-03 |
| E2E-BIL-06 | Khóa sửa chỉ số khi đã lập hóa đơn | Kỳ T đã có hóa đơn | Sửa chỉ số kỳ T; sửa chỉ số kỳ có kỳ sau | Bị từ chối (409) | UI+API | P1 | CLAUDE.md "Điện nước/hóa đơn" |
| E2E-BIL-07 | Thay/reset đồng hồ | Chỉ số mới nhỏ hơn cuối kỳ trước | Ghi nhận thay đồng hồ có lý do → nhập chỉ số | Cho phép khi có ghi nhận; không có ghi nhận thì bị từ chối | UI+API | P2 | FR-BIL-03 |
| E2E-BIL-08 | Phát hành hóa đơn điện nước | A101 và A102 có chỉ số; A103 thiếu chỉ số | Staff → chọn kỳ → xem trước → phát hành | A101, A102: đúng một hóa đơn/phòng, tiền điện = tiêu thụ × giá điện, tiền nước tương tự, **đơn giá chụp vào dòng hóa đơn**; A103 được liệt kê cảnh báo và bỏ qua; hạn đóng = ngày phát hành + 7; không chia theo sinh viên | UI+API | P0 | AT-20, FR-BIL-04, 07, UC-21 |
| E2E-BIL-09 | Thông báo và quyền xem hóa đơn điện nước | A101 có trưởng phòng và 3 thành viên | Sau phát hành, kiểm từng Student | Trưởng phòng nhận thông báo; mọi thành viên đang ở xem được hóa đơn; sinh viên phòng khác không xem được | UI+API | P0 | AT-20, FR-BIL-04 |
| E2E-BIL-10 | Phòng chưa có trưởng phòng | Phòng không có trưởng phòng | Phát hành | Vẫn phát hành; thông báo gửi mọi thành viên; Staff thấy cảnh báo | UI | P1 | UC-21 (3a) |
| E2E-BIL-11 | Không phát hành trùng điện nước | Đã phát hành kỳ T cho A101 | Phát hành lại cùng kỳ; phát hành lại sau khi nhập bổ sung phòng thiếu | Không tạo hóa đơn thứ hai cho A101 (lỗi `DUPLICATE_INVOICE` hoặc bỏ qua); phòng thiếu chỉ số nay được phát hành | UI+API | P0 | AT-15, FR-BIL-06 |
| E2E-BIL-12 | Phát hành phí phòng theo học kỳ | Sinh viên ở A101 (giá 3.000.000), sinh viên vào ở giữa kỳ, sinh viên chuyển phòng giữa kỳ sang phòng giá khác | Staff → học kỳ → xem trước → phát hành | Mỗi sinh viên đang ở đúng một hóa đơn; số tiền = đơn giá học kỳ của phân phòng hiệu lực ngày phát hành, **đủ kỳ** kể cả vào giữa kỳ; chuyển phòng giữa kỳ không thu thêm/hoàn chênh lệch; đơn giá chụp vào dòng hóa đơn | UI+API | P0 | FR-BIL-05, Q-18, Q-19 |
| E2E-BIL-13 | Phí phòng học kỳ hè | Sinh viên A chọn ở hè (đã duyệt), sinh viên B tạm rời hè | Phát hành phí phòng kỳ hè | Chỉ A có hóa đơn (giá hè); B không có | UI+API | P0 | FR-REG-15, AT-24, 26 |
| E2E-BIL-14 | Không phát hành trùng phí phòng | Đã phát hành kỳ X | Phát hành lại kỳ X | Không có hóa đơn thứ hai cho (sinh viên, học kỳ); sinh viên đã có hóa đơn bị bỏ qua | UI+API | P0 | AT-15, BR-04 |
| E2E-BIL-15 | Xác nhận thanh toán | Hóa đơn UNPAID | Staff xác nhận: người nộp, hình thức | PAID; lưu người nộp, người xác nhận, thời gian, hình thức; lịch sử trạng thái ghi lại | UI | P0 | FR-BIL-08 |
| E2E-BIL-16 | Hóa đơn PAID bị khóa | Hóa đơn PAID | Sửa số tiền/hạn; xóa; xác nhận lần hai | Cả ba bị từ chối (409); dữ liệu không đổi | UI+API | P0 | AT-16, BR-04 |
| E2E-BIL-17 | Phạm vi hóa đơn phí phòng theo học kỳ | Sinh viên S ở K1 kỳ 1, chuyển sang N2 kỳ 2 | Staff K1 và Staff N2 mở hóa đơn kỳ 1 và kỳ 2 của S | Staff K1 chỉ thấy/xác nhận hóa đơn kỳ 1; Staff N2 chỉ thấy kỳ 2; truy cập chéo là 404; quyền ghi chỉ tính phân phòng đúng đơn vị | UI+API | P0 | BR-07, FR-BIL-05, 08 |
| E2E-BIL-18 | Nhãn "Quá hạn" | Hóa đơn UNPAID đã qua hạn (đẩy lùi dữ liệu) | Mở danh sách Student và Staff | Nhãn "Quá hạn" tính ra, không có cột lưu sẵn; hóa đơn chưa tới hạn không có nhãn | UI+API | P1 | FR-BIL-07, BR-08 |
| E2E-BIL-19 | Hóa đơn thủ công (MANUAL) | Staff có quyền | Sửa tổng tiền/hạn khi chưa thanh toán; xóa hóa đơn MANUAL chưa thanh toán; sửa/xóa hóa đơn ISSUED | MANUAL chưa thanh toán sửa/xóa được và lịch sử trạng thái được giữ; ISSUED không sửa/xóa | API | P1 | BR-04 |
| E2E-BIL-20 | Điều chỉnh sau thanh toán | Hóa đơn kỳ X đã PAID | Thêm dòng "Điều chỉnh" (âm/dương) có lý do vào hóa đơn kỳ kế | Dòng điều chỉnh có trên hóa đơn kỳ sau; thiếu lý do bị từ chối | UI | P2 | FR-BIL-12 |
| E2E-BIL-21 | Sổ cọc | Hợp đồng đã thu cọc, bị trừ, được bù | Mở sổ cọc (Student và Staff) | Liệt kê thu/trừ/bù/hoàn với dấu đúng; **số dư tính từ sổ**; không có nút Thêm/Sửa/Xóa; gọi API ghi trực tiếp vào sổ bị từ chối | UI+API | P0 | FR-BIL-09, SRS "Sổ cọc chỉ đọc" |
| E2E-BIL-22 | Xác nhận nộp bù cọc | Khoản bù cọc chưa nộp | Staff xác nhận đã nộp | Ghi giao dịch bù +số tiền; số dư tăng đúng; khoản bù không còn "cần bù"; không xác nhận được hai lần | UI+API | P0 | FR-BIL-09 |
| E2E-BIL-23 | "Quá hạn bù cọc" | Khoản bù cọc quá 7 ngày (đẩy lùi dữ liệu), job nhắc đã chạy | Student và Staff kiểm tra | Nhãn "Quá hạn bù cọc"; Student nhận thông báo nhắc; khoản nằm trong danh sách cần nhắc của Staff; không phạt | UI+API `@job` | P0 | AT-23, FR-BIL-13 |
| E2E-BIL-24 | Student xem tài chính của mình | Student A (phòng A101) | Mở hóa đơn phí phòng, hóa đơn điện nước phòng, sổ cọc, số tiền cần bù | Thấy đủ của mình và của phòng mình; không thấy của sinh viên khác | UI | P0 | FR-BIL-10 |
| E2E-BIL-25 | Công nợ và báo cáo | Dữ liệu có hóa đơn UNPAID, bù cọc, số dư âm | Staff mở công nợ theo sinh viên và theo phòng; xuất báo cáo theo kỳ | Số liệu khớp dữ liệu nguồn; tệp xuất mở được và có đúng số dòng/tổng | UI+API | P1 | FR-BIL-11 |
| E2E-BIL-26 | Thời gian phát hành phí phòng quy mô lớn | ~1.500 sinh viên (dữ liệu sinh) | Phát hành phí phòng toàn KTX bằng API | Hoàn tất < 30 giây | API (đo) | P2 | NFR-PER-02 |
| E2E-BIL-27 | Định dạng tiền và ngày | Các màn hóa đơn, cọc | Xem số tiền, ngày | Tiền theo định dạng VND dùng chung (`utils/format`), ngày theo múi giờ `Asia/Ho_Chi_Minh`, không lệch ngày khi lưu/hiển thị | UI | P1 | UI 27 |

### 5.9. SUM, Học kỳ hè, tạm rời hè, phiếu gửi đồ (FR-REG-13..15, FR-CON-11..13, AT-24..26)

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-SUM-01 | Mở đợt đăng ký hè | Staff K1; có sinh viên ACTIVE | Mở đợt hè | Mọi sinh viên có hợp đồng ACTIVE của đơn vị nhận thông báo; sinh viên đơn vị khác không nhận | UI+API | P1 | FR-REG-13 |
| E2E-SUM-02 | Chọn phương án hè | Student ACTIVE | Chọn "ở hè" / "tạm rời hè" / "rời KTX" | Lưu được ở trạng thái chờ duyệt; Student chưa có hợp đồng ACTIVE hoặc đợt hè chưa mở cho đơn vị bị từ chối | UI+API | P0 | FR-REG-14 |
| E2E-SUM-03 | Sửa, rút, gửi lại lựa chọn | Lựa chọn đang chờ; lựa chọn bị từ chối | Sửa lựa chọn chờ; rút lựa chọn chờ; sửa lựa chọn bị từ chối; gửi lại sau khi rút | Rút → "Đã hủy" (giữ lịch sử); sửa lựa chọn bị từ chối = gửi lại, về chờ duyệt; gửi lại cùng học kỳ mở lại **chính bản ghi cũ** | UI+API | P1 | FR-REG-14 |
| E2E-SUM-04 | Duyệt ở hè | Lựa chọn "ở hè" chờ duyệt | Staff duyệt | Ngày kết thúc hợp đồng gia hạn qua hè (ghi như một gia hạn, có mốc cũ/mới); Student có hóa đơn phí phòng kỳ hè khi phát hành | UI+API | P0 | AT-26, FR-REG-15 |
| E2E-SUM-05 | Gom phòng ở hè | Sinh viên ở hè | Staff chuyển sang giường khác cùng đơn vị | Chuyển thành công theo luồng chuyển phòng (CON-11); khác đơn vị bị từ chối | UI+API | P1 | FR-REG-15 |
| E2E-SUM-06 | Tạm rời hè | Hợp đồng đã gia hạn sang năm học kế tiếp; lựa chọn "tạm rời" được duyệt | Staff ghi phiếu gửi đồ: số kiện, mô tả, vị trí kho, ngày gửi | Phân phòng đóng, giường trống dùng được trong hè; hợp đồng **vẫn ACTIVE** với nhãn "Tạm rời hè"; không có hóa đơn phí hè; cọc giữ nguyên; trưởng phòng (nếu có) tự gỡ | UI+API | P0 | AT-24, FR-CON-11, BR-02 |
| E2E-SUM-07 | Tạm rời khi chưa gia hạn năm sau | Hợp đồng chưa gia hạn sang năm kế | Ghi phiếu gửi đồ | Bị từ chối | UI+API | P1 | FR-CON-11 |
| E2E-SUM-08 | Phân lại giường đầu HK1 | Sinh viên tạm rời; giường cũ còn trống; biến thể giường cũ đã có người | Staff phân lại giường | **Không cần đơn mới**; ưu tiên giường cũ nếu còn trống, nếu đã có người thì phân giường khác; phiếu gửi đồ ghi ngày nhận lại | UI+API | P0 | AT-25, FR-CON-12 |
| E2E-SUM-09 | Chọn rời KTX | Lựa chọn "rời KTX" được duyệt | Staff làm thủ tục trả phòng | Đi theo CON-16/17 | UI | P1 | FR-CON-13 |

### 5.10. NOT, Thông báo và dashboard (FR-NOT-01..06)

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-NOT-01 | Đăng thông báo theo phạm vi | Staff K1 | Đăng cho đơn vị; cho tòa; cho tầng; cho phòng | Đúng đối tượng nhận; Staff thường không đăng được toàn KTX | UI | P1 | FR-NOT-01, 02 |
| E2E-NOT-02 | Thông báo toàn KTX | Staff có quyền tổng hợp (và Admin) | Đăng toàn KTX | Mọi người dùng liên quan nhận được | UI | P1 | FR-NOT-01 |
| E2E-NOT-03 | Phạm vi người nhận | Thông báo cho phòng A101 | Student A101 và Student A102 mở hộp thư | Chỉ Student A101 thấy | UI+API | P0 | FR-NOT-03 |
| E2E-NOT-04 | Hộp thư và đánh dấu đã đọc | Student có thông báo chưa đọc | Mở hộp thư → đọc → đánh dấu đã đọc | Bộ đếm chưa đọc giảm; mỗi người chỉ đánh dấu của chính mình (người khác bị 404/403) | UI+API | P0 | FR-NOT-03 |
| E2E-NOT-05 | Thông báo hệ thống tự sinh | Thực hiện: duyệt/từ chối đơn, giữ chỗ, hóa đơn mới, trừ cọc, ticket đổi trạng thái | Kiểm hộp thư người liên quan sau mỗi sự kiện | Có đúng một thông báo cho mỗi sự kiện; thông báo lỗi **không** làm hỏng nghiệp vụ gốc | UI | P1 | FR-NOT-04 |
| E2E-NOT-06 | Sửa/xóa thông báo | Thông báo thủ công và thông báo hệ thống | Sửa thông báo thủ công; sửa/xóa thông báo hệ thống | Thủ công chỉ sửa được nội dung, giữ phạm vi/người nhận; thông báo hệ thống không sửa/xóa qua CRUD | UI+API | P1 | CLAUDE.md |
| E2E-NOT-07 | Dashboard Staff K1 | Dữ liệu nền với số liệu đã biết | Mở `/staff/dashboard` | Các chỉ số khớp số tính từ dữ liệu nguồn: tỷ lệ giường đang dùng/sức chứa khai thác, số giường trong phòng bảo trì (**riêng**), tổng tiền chưa thanh toán, ticket theo trạng thái, hợp đồng sắp hết hạn/quá hạn trả phòng, phòng chưa có trưởng phòng, tổng bù cọc chưa nộp | UI+API | P0 | FR-NOT-05 |
| E2E-NOT-08 | Dashboard theo phạm vi | Staff K1, Staff N2+N3, Staff tổng hợp | So sánh số liệu | K1 chỉ số K1; N2+N3 chỉ số của mình; tổng hợp = toàn KTX | UI | P1 | FR-NOT-05, BR-07 |
| E2E-NOT-09 | Biểu đồ dashboard | Staff | Mở dashboard | Biểu đồ hiển thị, không lỗi console, dữ liệu biểu đồ khớp bảng số | UI | P2 | FR-NOT-06 |

### 5.11. JOB, Tác vụ định kỳ SYSTEM (FR-CON-02, FR-MNT-06, FR-BIL-13, NFR-REL-03, AT-05, 12, 23)

Điều kiện chung: profile `e2e` đặt lịch chạy mỗi phút (quyết định D2); dữ liệu được lùi thời gian bằng helper SQL; kết quả đợi bằng `expect.poll`.

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-JOB-01 | Tự hủy hợp đồng PENDING quá hạn | Hợp đồng PENDING có thời điểm phân phòng cách đây N+1 ngày (N=7) | Đợi job chạy | Hợp đồng CANCELLED, phân phòng đóng, giường trống, đơn CANCELLED, lý do "tự động hủy"; audit có người thao tác `SYSTEM` | API `@job` | P0 | AT-05, FR-CON-02 |
| E2E-JOB-02 | Biên của hạn kích hoạt | Hợp đồng PENDING đúng N ngày; N−1 ngày | Đợi job chạy | Hợp đồng chưa quá hạn **không** bị hủy; chỉ quá hạn mới bị hủy | API `@job` | P1 | FR-CON-02 |
| E2E-JOB-03 | Job chạy lại an toàn | Sau JOB-01 | Kích hoạt job thêm một lần nữa | Không tạo thêm lịch sử/audit; không đổi dữ liệu | API `@job` | P0 | NFR-REL-03 |
| E2E-JOB-04 | Tự đóng ticket RESOLVED quá 3 ngày | Ticket RESOLVED cách đây 3 ngày + 1 phút; ticket 2 ngày | Đợi job | Ticket quá hạn CLOSED với lý do "tự động đóng", SYSTEM; ticket 2 ngày giữ RESOLVED; **không** tự kết luận nguyên nhân hay trừ cọc | API `@job` | P0 | AT-12, FR-MNT-06 |
| E2E-JOB-05 | Nhắc bù cọc quá hạn | Khoản bù cọc quá 7 ngày | Đợi job hai lần | Sinh viên nhận **một** thông báo (không trùng); khoản đã được bù không còn bị nhắc; nằm trong danh sách cần nhắc của Staff | API `@job` | P0 | AT-23, FR-BIL-13 |
| E2E-JOB-06 | Tham số lấy từ cấu hình | Admin | Đổi hạn kích hoạt từ 7 xuống 3 ngày qua màn Cấu hình → tạo hợp đồng PENDING cách 4 ngày | Hợp đồng bị hủy theo giá trị mới mà không cần triển khai lại; thử thêm khóa cấu hình mới qua `save-or-update` bị từ chối | UI+API `@job` | P1 | CLAUDE.md "không hardcode" |
| E2E-JOB-07 | Cô lập lỗi từng bản ghi | Một bản ghi khiến xử lý lỗi, các bản ghi khác hợp lệ | Đợi job | Bản ghi lỗi không chặn các bản ghi còn lại | API `@job` | P2 | SAD (job từng ID, từng transaction) |

### 5.12. UI, Hành vi giao diện dùng chung (CLAUDE.md "Form và kiểm chứng", UI 27)

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-UI-01 | Bấm Sửa nạp dữ liệu hiện có | Bản ghi có ngày, khóa ngoại, giá trị `false`, giá trị `0` | Bấm Sửa trên các màn CRUD tiêu biểu (phòng, đợt đăng ký, học kỳ, đơn giá) → **không đổi gì** → Lưu | Form hiển thị đúng ngày, lựa chọn khóa ngoại, công tắc `false`, số `0`; sau Lưu dữ liệu không mất/đổi (sửa từng phần) | UI | P0 | CLAUDE.md |
| E2E-UI-02 | Lỗi trường và trạng thái đang lưu | Form bất kỳ | Gửi dữ liệu sai; gửi dữ liệu đúng | Lỗi hiển thị dưới trường (`aria-describedby`); form mở khi lưu lỗi; nút ở trạng thái đang lưu và chặn bấm đôi; đóng form chỉ khi lưu **thành công** | UI | P0 | CLAUDE.md |
| E2E-UI-03 | Tìm kiếm, sắp xếp, phân trang ở server | Danh sách > 1 trang | Đổi `page`, `size` (1, 20, 100), từ khóa `q`, cột sắp xếp; mở 2 tab CRUD | Mỗi thao tác gửi `POST .../search-by-page` với body `{page,size,q,sort,...}`; `total` khớp số trên giao diện; sắp xếp chỉ theo cột server cho phép (cột lạ bị từ chối); trạng thái URL của tab này không rò sang tab khác; **không** tải một trang rồi lọc lại ở trình duyệt | UI+API | P0 | CLAUDE.md "API theo V5" |
| E2E-UI-04 | Giới hạn tham số phân trang | API | `page=-1`, `size=0`, `size=101`, `page="a"` | 400 với `errorCode` ổn định, không 500 | API | P1 | CLAUDE.md |
| E2E-UI-05 | Xử lý 409 và mã lỗi | Tạo xung đột (REG-18) | Quan sát thông báo của bên thất bại | Dữ liệu được làm mới (refetch); hiển thị câu tiếng Việt tương ứng `errorCode`; **không** hiện `message` thô hay JSON | UI | P0 | CLAUDE.md |
| E2E-UI-06 | `allowedActions` quyết định nút | Đối tượng ở nhiều trạng thái (đơn, hợp đồng, ticket, hóa đơn) | Mở chi tiết ở từng trạng thái | Chỉ hiện các nút có trong `allowedActions`; nút hành động không có trong danh sách không xuất hiện; nhưng gọi API trái trạng thái vẫn bị từ chối | UI+API | P0 | SRS 3, API-Spec mục 6 |
| E2E-UI-07 | Hộp thoại xác nhận | Thao tác không đảo ngược (duyệt, từ chối, kích hoạt, trả phòng, hủy) | Bấm thao tác | Có hộp thoại xác nhận; hành động cần lý do thì lý do bắt buộc; Hủy hộp thoại không gọi API | UI | P0 | CLAUDE.md "API conventions" |
| E2E-UI-08 | Nhãn trạng thái thống nhất | Các màn có trạng thái | So nhãn/màu với `statusConfig` (ba lớp: trạng thái DB, nhãn suy ra, tình trạng giường) | Mọi nơi dùng cùng nhãn tiếng Việt; không có trạng thái hiện mã thô | UI | P1 | UI 27 |
| E2E-UI-09 | Mobile 360 px: Student | Viewport 360×740 | Duyệt các màn Student: trang chủ, đơn, hợp đồng, hóa đơn, sửa chữa, thông báo | Có điều hướng dưới đáy (< 640 px); bảng chuyển thành thẻ; không cuộn ngang; vùng bấm đủ lớn; nộp đơn hoàn tất được | UI `@mobile` | P0 | NFR-USA-01 |
| E2E-UI-10 | Mobile 360 px: Technician | Viewport 360×740 | Xem việc, cập nhật tiến độ, đính ghi chú | Dùng trọn vẹn không cuộn ngang | UI `@mobile` | P1 | NFR-USA-01 |
| E2E-UI-11 | Desktop: Staff, Admin | 1280×800 và 900×700 | Mở các màn | ≥ 1024 px: sidebar; dưới 1024 px: Drawer mở/đóng được; bảng dùng DataTable | UI | P1 | UI 27 |
| E2E-UI-12 | Truy cập (a11y) | Các màn đăng nhập, danh sách, form | Quét axe; điều hướng bàn phím trong hộp thoại | Không lỗi mức nghiêm trọng/cao; Tab đi đúng thứ tự; Esc đóng hộp thoại; focus quay về nút gọi | UI `@axe` | P2 | UI 27 |
| E2E-UI-13 | Không có lỗi console | Mọi luồng smoke | Theo dõi `console.error` và `pageerror` | Không có lỗi không mong đợi (có danh sách cho phép rõ ràng) | UI | P1 | — |
| E2E-UI-14 | Nhập/xuất Excel ở giao diện | Admin/Staff | Tải mẫu; xuất báo cáo | Tệp tải về mở được, có tiêu đề cột đúng; xuất không chứa công thức độc hại | UI | P2 | SRS 5.3 |

### 5.13. SEC, Bảo mật và đầu vào độc hại (NFR-SEC-01..05)

| TC | Tên | Tiền điều kiện | Bước | Kết quả mong đợi | Loại | Ưu | Tham chiếu |
|---|---|---|---|---|---|---|---|
| E2E-SEC-01 | XSS được escape khi hiển thị | Student/Staff | Nhập `<script>`, `<img onerror>` vào mô tả ticket, nội dung thông báo, ghi chú đơn, họ tên (nếu sửa được) → xem ở vai trò khác | Hiển thị dạng văn bản, không chạy script; không có hộp thoại bất ngờ | UI | P0 | NFR-SEC-03 |
| E2E-SEC-02 | SQL injection | API | Gửi `' OR 1=1 --`, `"; DROP TABLE ...` vào `q`, sort, các trường văn bản | Không lỗi 500, không lộ dữ liệu thừa, dữ liệu nguyên vẹn | API | P0 | NFR-SEC-03 |
| E2E-SEC-03 | DTO từ chối trường ngoài hợp đồng | API | Gửi trường lạ, ID kiểu chuỗi, `page` sai kiểu, phần tử `null` trong danh sách quan hệ | 400 với `errorCode` ổn định, không chạy logic | API | P0 | CLAUDE.md "Validation" |
| E2E-SEC-04 | Token không lưu bền | Đã đăng nhập | Kiểm `localStorage`, `sessionStorage`, cookie, IndexedDB | Không chứa access token/refresh token dạng đọc được theo thiết kế (token trong bộ nhớ); không có mật khẩu | UI | P0 | NFR-SEC-04, NFR-SEC-01 |
| E2E-SEC-05 | Mật khẩu tạm chỉ trả một lần | Vừa tạo/reset tài khoản | Gọi lại GET người dùng, GET audit, kiểm cache phản hồi | Không có `temporaryPassword` hay mật khẩu ở bất kỳ phản hồi sau đó, trong audit hoặc log | API | P0 | CLAUDE.md, NFR-SEC-01 |
| E2E-SEC-06 | `requestId` trên lỗi | API | Gây lỗi 401, 403, 404, 409, và (nếu có cách an toàn) 500 | Phản hồi theo dạng `{errorCode, message, details}`; 401/403/500 có `requestId`; 500 là `INTERNAL_ERROR` không lộ stack trace | API | P1 | SAD, NFR-MNT-03 |
| E2E-SEC-07 | Quyền khai ở mọi endpoint | API | Gọi một mẫu endpoint ghi bằng tài khoản không có quyền (từng vai trò) | 403 trước khi Controller chạy; chưa đăng nhập 401 | API | P0 | `@RequirePrivilege` |
| E2E-SEC-08 | Secret không lộ ở frontend | Bản build | Tìm chuỗi secret trong bundle và phản hồi mạng | Không có `DMS_KEYCLOAK_ADMIN_CLIENT_SECRET`, mật khẩu CSDL hay khóa | UI | P1 | NFR-SEC-05 |

### 5.14. XF, Luồng xuyên suốt nhiều vai trò (chỉ chạy bằng UI từ đầu đến cuối)

Mỗi luồng dùng nhiều context trình duyệt (một context cho một vai trò, dùng `storageState`), chạy tuần tự, có đối chiếu cuối luồng bằng API/CSDL.

| TC | Tên | Chuỗi bước | Kết quả mong đợi cuối luồng | Ưu | Tham chiếu |
|---|---|---|---|---|---|
| E2E-XF-01 | Từ đăng ký đến ở được (AT-01) | Staff mở đợt → Student soạn và nộp đơn → Staff duyệt → Staff phân giường → Student thấy giường được giữ và hạn → Staff kích hoạt thu cọc → Student xem hợp đồng/phòng/sổ cọc | Hợp đồng ACTIVE, phân phòng STAY, sổ cọc +cọc tòa, Student thấy đúng phòng, giường; giường trong danh sách phòng tính là "đang ở" | P0 `@smoke` | AT-01 |
| E2E-XF-02 | Vòng đời đầy đủ một sinh viên | XF-01 → Student báo hỏng vòi nước → Staff giao việc → KTV xử lý → Student nghiệm thu → Staff chọn hư hỏng do sử dụng 100.000 đ chia 3 → Staff nhập điện nước và phát hành → trưởng phòng nhận hóa đơn → Staff xác nhận thanh toán → sinh viên nộp bù cọc, Staff xác nhận → trả phòng hoàn cọc | Mỗi bước đúng trạng thái; số dư cọc cuối = cọc − khoản trừ + bù; hoàn cọc = số dư; công nợ còn lại đúng | P0 | AT-01, 14, 19, 20 |
| E2E-XF-03 | Chu trình hè | Staff mở đợt hè → sinh viên A chọn ở hè, B chọn tạm rời → Staff duyệt cả hai, ghi phiếu gửi đồ cho B → phát hành phí hè → đầu HK1 phân lại giường cho B | A có hóa đơn hè, B không; giường của B dùng được trong hè; B có giường lại không cần đơn mới, phiếu ghi ngày nhận lại | P1 | AT-24, 25, 26 |
| E2E-XF-04 | Chuyển phòng ảnh hưởng thế nào đến tiền | Sinh viên ở phòng giá 3.000.000 → chuyển giữa kỳ sang phòng giá khác → phát hành phí phòng | Hóa đơn kỳ hiện tại không đổi theo phòng mới; kỳ sau theo phòng mới; lịch sử phân phòng hai dòng liên tiếp | P1 | Q-19, FR-BIL-05 |
| E2E-XF-05 | Giữ chỗ hết hạn rồi phân lại | Phân giường → để quá hạn (đẩy lùi) → job tự hủy → Staff phân lại giường cho cùng đơn (nếu đơn được trả về trạng thái hợp lệ theo luồng) | Giường được trả; không có bản ghi thừa; Student nhận thông báo hủy; luồng phân lại không vi phạm ràng buộc | P1 | AT-05, FR-CON-02 |
| E2E-XF-06 | Khóa/mở khóa trong lúc đang làm việc | Student đang soạn đơn → Admin khóa → Student bấm Nộp → Admin mở khóa → Student đăng nhập lại | Nộp bị chặn; sau mở khóa phiên cũ vẫn mất hiệu lực; đăng nhập mới nộp được | P1 | AT-28 |

---

## 6. Ma trận truy vết

### 6.1. Kịch bản nghiệm thu SRS (AT) → testcase

| AT | Testcase E2E | Ghi chú |
|---|---|---|
| AT-01 | XF-01, REG-15, CON-01, CON-11 | |
| AT-02 | REG-18 | API đồng thời |
| AT-03 | — | Test tích hợp CSDL (ngoài E2E) |
| AT-04 | REG-19 | |
| AT-05 | JOB-01, JOB-02, JOB-03, XF-05 | |
| AT-06 | RBAC-03, RBAC-04, REG-24, CON-25 | |
| AT-07 | CON-13 | E2E chỉ kiểm hệ quả; chèn lỗi giữa chừng ở test tích hợp |
| AT-08 | CON-12 | |
| AT-09 | CON-09 | |
| AT-10 | CON-18 | |
| AT-11 | MNT-09 | |
| AT-12 | JOB-04 | |
| AT-13 | BIL-03 | |
| AT-14 | MNT-15, XF-02 | |
| AT-15 | BIL-11, BIL-14 | |
| AT-16 | BIL-16 | |
| AT-17 | REG-06 | |
| AT-18 | CON-02 | |
| AT-19 | CON-19, XF-02 | |
| AT-20 | BIL-08, BIL-09 | |
| AT-21 | ROOM-16 | |
| AT-22 | MNT-14 | |
| AT-23 | BIL-23, JOB-05 | |
| AT-24 | SUM-06, XF-03 | |
| AT-25 | SUM-08, XF-03 | |
| AT-26 | SUM-04, BIL-13, XF-03 | |
| AT-27 | AUTH-01, AUTH-13 | |
| AT-28 | AUTH-14, AUTH-15, XF-06 | |
| AT-29 | ACC-12 | |
| AT-30 | AUTH-08, AUTH-11 | |
| AT-31 | AUTH-12 | |
| AT-32 | REG-10 | |
| AT-33 | ROOM-05 | |

### 6.2. Yêu cầu chức năng (FR) → testcase

| FR | Testcase | FR | Testcase |
|---|---|---|---|
| ACC-01 | AUTH-01..03, 07..10 | REG-11 | REG-20, REG-21 |
| ACC-02 | AUTH-04 | REG-12 | REG-22 |
| ACC-03 | AUTH-05, 06 | REG-13 | SUM-01 |
| ACC-04 | ACC-01..04 | REG-14 | SUM-02, SUM-03 |
| ACC-05 | ACC-13..15 | REG-15 | SUM-04, SUM-05 |
| ACC-06 | ACC-05..09 | CON-01 | CON-01..04 |
| ACC-07 | ACC-10..12, AUTH-15 | CON-02 | JOB-01..03 |
| ACC-08 | ACC-17, RBAC-15 | CON-03 | CON-06 |
| ACC-09 | ACC-18, ACC-19 | CON-04 | CON-07 |
| ACC-10 | RBAC-07..13 | CON-05 | CON-05 |
| ROOM-01 | ROOM-01, 02 | CON-06 | CON-08..10, CON-24 |
| ROOM-02 | ROOM-03..05 | CON-07 | CON-11, 12, 15, 24 |
| ROOM-03, 04 | ROOM-06..08 | CON-08 | CON-11, 13, 14 |
| ROOM-05 | ROOM-09, 10 | CON-09 | CON-16..23 |
| ROOM-06 | ROOM-11 | CON-10 | CON-18, CON-20 |
| ROOM-07 | ROOM-12, 13 | CON-11 | SUM-06, SUM-07 |
| ROOM-08 | ROOM-14 | CON-12 | SUM-08 |
| ROOM-09, 10 | ROOM-15, 16 | CON-13 | SUM-09 |
| ROOM-11 | ROOM-17 | MNT-01 | MNT-01..04 |
| REG-01 | REG-01, 02, 04 | MNT-02 | MNT-05 |
| REG-02 | REG-03, 05 | MNT-03 | MNT-06, 07 |
| REG-03 | REG-06 | MNT-04 | MNT-08 |
| REG-04 | REG-07, 09, 10 | MNT-05 | MNT-10, 11 |
| REG-05 | REG-07, 11 | MNT-06 | JOB-04 |
| REG-06 | REG-12 | MNT-07 | MNT-12 |
| REG-07 | REG-13, 14 | MNT-08 | MNT-13 |
| REG-08 | REG-15..17 | MNT-09 | MNT-14, 16 |
| REG-09 | REG-18 | MNT-10 | MNT-15..18 |
| REG-10 | REG-19 | BIL-01 | BIL-01 |
| BIL-02 | BIL-02..06 | BIL-08 | BIL-15, 16 |
| BIL-03 | BIL-07 | BIL-09 | BIL-21, 22 |
| BIL-04 | BIL-08..10 | BIL-10 | BIL-24 |
| BIL-05 | BIL-12, 13 | BIL-11 | BIL-25 |
| BIL-06 | BIL-11, 14 | BIL-12 | BIL-20 |
| BIL-07 | BIL-18 | BIL-13 | BIL-23, JOB-05 |
| NOT-01, 02 | NOT-01, 02 | NOT-05 | NOT-07, 08 |
| NOT-03 | NOT-03, 04 | NOT-06 | NOT-09 |
| NOT-04 | NOT-05 | | |

Các FR mức Should/Could chưa có testcase riêng (FR-ACC-02 chờ 15 phút, FR-BIL-03 mức P2...) được ghi rõ ở cột Ưu P2 hoặc ở mục 1.3.

---

## 7. Thống kê và rủi ro

### 7.1. Thống kê (đếm từ các bảng testcase ở mục 5)

| Nhóm | Số TC | P0 | P1 | P2 |
|---|---:|---:|---:|---:|
| AUTH | 18 | 12 | 4 | 2 |
| ACC | 19 | 12 | 7 | 0 |
| RBAC | 16 | 13 | 3 | 0 |
| ROOM | 18 | 13 | 4 | 1 |
| REG | 24 | 19 | 4 | 1 |
| CON | 25 | 17 | 7 | 1 |
| MNT | 20 | 15 | 4 | 1 |
| BIL | 27 | 16 | 8 | 3 |
| SUM | 9 | 4 | 5 | 0 |
| NOT | 9 | 3 | 5 | 1 |
| JOB | 7 | 4 | 2 | 1 |
| UI | 14 | 7 | 5 | 2 |
| SEC | 8 | 6 | 2 | 0 |
| XF | 6 | 2 | 4 | 0 |
| **Tổng** | **220** | **143** | **64** | **13** |

Số P0 chiếm đa số vì phần lớn là phân quyền, tiền và an toàn dữ liệu; tập `@smoke` chỉ lấy một phần nhỏ trong số P0 (luồng XF-01, đăng nhập 4 vai trò, RBAC-03, REG-18, CON-01).

### 7.2. Rủi ro và cách giảm

| Rủi ro | Ảnh hưởng | Cách giảm |
|---|---|---|
| Test flaky do chờ Keycloak/tải trang | CI đỏ giả, nhóm mất niềm tin | Đăng nhập một lần bằng `storageState`; chờ bằng assertion; `retries: 1` + theo dõi test xanh nhờ retry |
| Dữ liệu dùng chung giữa các test | Test phụ thuộc thứ tự, chạy song song hỏng | Mã duy nhất theo `runId`; dữ liệu riêng mỗi test; chỉ `@concurrency` chạy tuần tự |
| Không có token cho helper API (D1) | Không viết được nhóm API | Chốt D1 ở pha 0 trước mọi việc khác |
| Tác vụ định kỳ khó kiểm thử | Nhóm JOB bị bỏ | D2: cron mỗi phút ở profile `e2e` + helper lùi thời gian |
| Selector gắn vào chữ tiếng Việt | Đổi nhãn là vỡ test | Dùng `getByRole` + `data-testid` từ một registry; nhãn tập trung ở `resources.ts`/`statusConfig.ts` |
| Hợp đồng API đổi mà test không đổi | Test sai lệch tài liệu | Theo quy tắc dự án: đổi contract thì cập nhật React, tài liệu và kiểm thử cùng thay đổi |
| E2E làm chậm quy trình PR | Nhóm né chạy | Chỉ `@smoke` (< 5 phút) trên PR; phần còn lại chạy đêm |
| Chạy nhầm lên CSDL dùng chung/demo | Mất dữ liệu | CSDL/realm E2E tách riêng; kiểm tra tên CSDL trước khi chạy (từ chối nếu không phải `dms_e2e`) |

### 7.3. Điều kiện hoàn thành (Definition of Done)

- Mỗi testcase có mã `E2E-<NHÓM>-<số>` trong tiêu đề test, thuộc đúng thư mục theo vai trò, gắn thẻ ưu tiên.
- Xanh 3 lần liên tiếp trên môi trường sạch trước khi merge vào `develop`.
- Không mật khẩu/secret trong mã, log, ảnh chụp; không phụ thuộc dữ liệu `MAU-`.
- Test thất bại để lại trace + ảnh + video; kèm thông báo lỗi chỉ rõ `errorCode` hoặc phần tử mong đợi.
- Cập nhật bảng "Trạng thái cài đặt" bên dưới khi hoàn thành từng nhóm.

### 7.4. Trạng thái cài đặt

| Nhóm | Đã cài | Ghi chú |
|---|---|---|
| Hạ tầng Playwright | Có | Cấu hình 4 vai trò, `auth.setup.ts`, README |
| AUTH-01, AUTH-09, AUTH-10 (một phần) | Có mẫu | Hiện là 2 test cổng/vai trò + 2 test ẩn danh; **chưa chạy được thật** trên Keycloak/backend (môi trường chưa bật khi thử) |
| Các nhóm còn lại | Chưa | Theo lộ trình mục 4 |

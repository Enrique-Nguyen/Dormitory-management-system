# Đánh giá tổng thể và đề xuất cải thiện DMS (kèm đặc tả giao diện)

- **Ngày rà soát:** 19/11/2026
- **Nhánh:** `docs/registration-deep-review` (đồng bộ với `origin/develop` tại `14e76d9`)
- **Thực hiện:** Claude Code (rà soát theo yêu cầu của nhóm)
- **Phạm vi:** toàn bộ `DMS/backend` (8 module, khoảng 32.000 dòng Java, 175 endpoint, 15 migration, 48 lớp test), `DMS/frontend` (khoảng 30.000 dòng TS/TSX/CSS), `Docs/`, `DMS/scripts`. Giao diện được chạy thật với bộ DataTest trên máy local.
- **Không thay thế:** SRS/SAD/API/DB vẫn là nguồn chuẩn. Báo cáo này chỉ nêu điểm cần cải thiện và hướng sửa; mọi thay đổi quy tắc nghiệp vụ phải sửa SRS trước. Đã có review riêng cho module đăng ký: [bao-cao-review-chuyen-sau-module-dang-ky-ktx.md](bao-cao-review-chuyen-sau-module-dang-ky-ktx.md), nên báo cáo này không lặp lại các điểm đó.

---

## 0. Tóm tắt

### 0.1. Điểm mạnh nên giữ

| Hạng mục | Nhận xét |
|---|---|
| Kiến trúc backend | Modular monolith đúng SAD, ArchUnit chặn gọi ngược tầng và dùng Repository chéo module. Service interface/Impl nhất quán. |
| Toàn vẹn dữ liệu | Khóa `FOR UPDATE` trước khi kiểm tra trạng thái ở các luồng ghi quan trọng (hóa đơn, hợp đồng, yêu cầu, lựa chọn hè); khóa giường theo `bed_id` tăng dần; partial unique index chống trùng phân phòng. |
| Phân quyền | Ma trận quyền động trong CSDL, `@RequirePrivilege` bắt buộc trên mọi endpoint, ngoài phạm vi trả 404, ADMIN có mọi quyền nhưng vẫn giữ ràng buộc. |
| Xác thực | Keycloak OIDC + PKCE, chống dò mật khẩu (`failureFactor: 5`), chính sách mật khẩu khớp FR-ACC-03, thu hồi token qua `security_version`. |
| Kiểm thử | JaCoCo 84% dòng, 67% nhánh (báo cáo 05/11/2026). Mọi gói `service.impl` đạt 74–90% dòng, vượt ngưỡng 60% của SAD. Có Testcontainers và kiểm thử đồng thời. |
| Job định kỳ | Mẫu "chọn ID → xử lý từng bản ghi trong transaction riêng" qua `SystemJobService` đúng hướng dẫn SAD. |
| Dữ liệu thử | DataTest nạp được đầy đủ trạng thái, có sao lưu trước khi ghi, `verify.sql` 21/21 kiểm tra đạt. |

### 0.2. Mười việc nên làm trước (xếp theo tác động / công sức)

| # | Việc | Mức | Công sức | Mục chi tiết |
|---|---|---|---|---|
| 1 | Cổng Sinh viên và KTV trên điện thoại: thay bảng cuộn ngang bằng danh sách thẻ (`ResponsiveTable`) cho mọi màn `CrudPage` | 🔴 Cao | 1–2 ngày | [B.1](#b1-bảng-dữ-liệu-không-responsive-trên-điện-thoại) |
| 2 | Trang chủ Sinh viên hiển thị trạng thái cá nhân (phòng, hóa đơn cần đóng, hợp đồng, cọc) thay vì lưới liên kết | 🔴 Cao | 2–3 ngày | [C.1](#c1-sinh-viên--trang-chủ-student) |
| 3 | Hiển thị tên/mã thay cho ID thô (`Sinh viên #28`) trong chi tiết và danh sách | 🔴 Cao | 2 ngày | [B.3](#b3-tham-chiếu-hiển-thị-id-thô) |
| 4 | Danh sách phòng thiếu cột Tòa → 6 dòng `M101` không phân biệt được; danh sách hóa đơn thiếu cột "Đối tượng" | 🔴 Cao | 0,5 ngày | [B.2](#b2-thiếu-cột-định-danh-trong-danh-sách) |
| 5 | Tách bundle theo route (hiện `App.js` 1,6 MB một khối) | 🟡 TB | 0,5 ngày | [D.1](#d1-bundle-16-mb-không-chia-theo-route) |
| 6 | Giữ "Nhập Excel" ở mọi màn nhưng chuyển sang kiểm tra → xác nhận phía server, mẫu Excel đủ trường nghiệp vụ | 🟡 TB | 2–3 ngày | [B.5](#b5-nhập-excel-ở-mọi-màn) |
| 7 | Hoàn thiện yêu cầu SRS còn thiếu: FR-ROOM-08 bạn cùng phòng, FR-BIL-12 điều chỉnh, FR-CON-12 phân lại giường sau hè, FR-NOT-06 biểu đồ, xuất báo cáo | 🔴 Cao | 1–2 tuần (chia TV) | [A](#a-nghiệp-vụ-còn-thiếu-so-với-srs) |
| 8 | Đóng gói triển khai: Dockerfile backend, nginx + TLS + header bảo mật, compose `prod` | 🔴 Cao | 1–2 ngày | [F.1](#f1-chưa-có-cấu-hình-triển-khai-production) |
| 9 | Thêm kiểm thử frontend (Vitest) và E2E (Playwright) cho 4 cổng, chạy trong CI | 🟡 TB | 2–3 ngày | [G.1](#g1-frontend-chưa-có-kiểm-thử) |
| 10 | Tách `CrudPage.tsx` (2.209 dòng) và `index.css` (4.241 dòng, 231 `!important`) | 🟡 TB | 3–4 ngày | [D.2](#d2-tệp-quá-lớn-khó-bảo-trì) |

### 0.3. Bảng điểm hiện trạng

| Hạng mục | Điểm /10 | Ghi chú ngắn |
|---|---|---|
| Kiến trúc & toàn vẹn dữ liệu | 8,5 | Đúng SAD, khóa và ràng buộc tốt |
| Bảo mật ứng dụng | 8 | Tốt ở tầng API; thiếu cấu hình production (header, TLS) |
| Độ phủ nghiệp vụ so với SRS | 7 | Phần lõi đã có; còn 6–7 yêu cầu Should/Must chưa làm |
| Giao diện quản lý (Staff/Admin, desktop) | 6,5 | Đồng nhất, có dashboard; thiếu định danh, tên thay ID, thao tác hàng loạt |
| Giao diện Sinh viên/KTV (mobile-first) | 4 | Bảng cuộn ngang, trang chủ không có thông tin cá nhân |
| Hiệu năng frontend | 5,5 | Bundle lớn, không lazy-load |
| Khả năng bảo trì frontend | 5 | Tệp khổng lồ, CSS ghi đè nhiều |
| Kiểm thử | 7 | Backend tốt; frontend 0 test |
| Sẵn sàng triển khai | 4 | Chưa có Dockerfile/nginx/prod compose |

---

## 1. Phương pháp và nguồn tham khảo

1. **Đọc mã:** cấu trúc module, `AbstractSqlCrudServiceImpl`, các `*CrudServiceImpl`, `ScopeGuard`, job định kỳ, `SecurityConfig`, migration, cấu hình Keycloak `DMS/keycloak/dms-realm.json`; frontend: router, `navigation.ts`, `CrudPage.tsx`, `ResponsiveTable.tsx`, `excel.ts`, `CrudExcelPanel.tsx`, bundle `dist/`.
2. **Chạy thật:** Docker Compose + backend + Vite, nạp DataTest, đăng nhập Admin (desktop 1366×800) và Sinh viên `9900260001` (mobile 375×812). Cổng KTV **chưa** đăng nhập thử do browser pane không render ổn định; nhận xét cổng KTV dựa trên mã (`technicianRoutes.tsx` dùng chung `CrudPage`).
3. **Đối chiếu SRS:** danh sách FR trong `SRS-DMS.md` so với 175 endpoint hiện có.
4. **Tham khảo hệ thống lớn:**
   - *StarRez* (phần mềm quản lý KTX phổ biến ở đại học Mỹ/Úc/Anh): cổng sinh viên "một cửa" gồm chọn phòng/bạn cùng phòng, ký hợp đồng, hẹn nhận/trả phòng, báo hỏng; nhân viên cấu hình quy tắc đủ điều kiện ([StarRez](https://starrez.com/), [Room & Roommate](https://www.starrez.com/features/roommate-matching), [Carleton](https://carleton.ca/its/2024/starrez-provides-one-stop-solution-for-housing-and-residence-life-services/), [Wake Forest guide](https://prod.wp.cdn.aws.wfu.edu/sites/303/2017/07/starrez-student-portal-instructions.pdf)).
   - *Shopify Polaris – Index table / Resource index*: trạng thái rỗng có hành động, thao tác hàng loạt chỉ hiện khi chọn dòng, nhãn "động từ + danh từ" ([Polaris Index table](https://polaris.shopify.com/components/index-table), [Resource index](https://shopify.dev/docs/api/app-home/patterns/templates/resource-index)).
   - *Ant Design Pro / ProTable*: một layout + ProTable + Form cho một trang CRUD, lưu trạng thái bảng vào URL, cài đặt cột, xuất dữ liệu ([ProTable](https://www.mintlify.com/ant-design/pro-components/components/pro-table), [Pro layout](https://pro.ant.design/docs/layout)).
   - *Mẫu bảng quản trị / saved views*: chỉ dùng "bộ lọc đã lưu" cho bảng phức tạp, phạm vi chọn hàng loạt phải rõ (trang này hay mọi kết quả lọc), lọc phía server ([Basis saved views](https://design.basis.com/patterns/saved-views), [Admin data table pattern](https://meta.koder.dev/specs/patterns/admin-data-table/)).
   - *Dashboard*: 4–5 thẻ KPI đầu trang, biểu đồ chẩn đoán ở giữa, bảng ngoại lệ ở cuối; chỉ số quan trọng nhất đặt trên-trái ([Idealogic](https://idealogic.io/blog/dashboard-design), [UX Pilot](https://uxpilot.ai/blogs/dashboard-design-principles)).

---

## A. Nghiệp vụ còn thiếu so với SRS

`DMS/backend/README.md` mục "Giới hạn của khung" đã **lạc hậu**: nhiều luồng ghi là "còn lại" thực tế đã có endpoint (`/applications/{id}/assign`, `/release-hold`, `/priorities/{id}/verify`, `/contracts/{id}/activate`, `/extension-requests/{id}/approve`, `/summer-choices/{id}/approve`, `/invoices/issue-room-fee`, `/invoices/issue-utility`). Cần cập nhật README để thành viên không làm trùng.

Các yêu cầu **chưa thấy endpoint/màn hình** (FR-BIL-03 thay đồng hồ đã có trong form chỉ số điện nước: `oldMeterFinal`, `newMeterStart`, `replacementReason`):

| FR | Nội dung | Hướng sửa | Module / TV gợi ý |
|---|---|---|---|
| FR-ROOM-08 | Sinh viên xem bạn cùng phòng và trưởng phòng | Mở rộng `GET /api/me/room` (OverviewController) trả `roommates[{fullName}]`, `leader{fullName, phone?}` từ `v_current_stay`; chỉ trả họ tên (không mã SV/SĐT bạn cùng phòng) | notification/room – TV2 |
| FR-BIL-12 | Điều chỉnh sau thanh toán | `invoice_item` loại `ADJUSTMENT` (âm/dương, lý do bắt buộc) gắn vào hóa đơn kỳ kế tiếp; audit cùng transaction; không sửa hóa đơn đã PAID | billing – TV5 |
| FR-CON-12 | Phân lại giường đầu HK1 cho SV tạm rời hè | Hoàn thiện `storageRecordId` trong `GET /api/beds/available`, ưu tiên giường cũ; gọi `BedOccupancyService.occupyStay` | contract/room – TV2/TV4 |
| FR-NOT-06 | Dashboard dạng biểu đồ | Dashboard đã có donut và thanh tỷ trọng; bổ sung **xu hướng theo thời gian** (thu tiền theo tháng, lấp đầy theo học kỳ, ticket mở/đóng theo tuần) | notification – TV5 |
| Báo cáo | Xuất báo cáo Excel phía server (Apache POI theo SAD) | Endpoint `GET /api/reports/{type}.xlsx` stream; hiện xuất Excel chạy phía trình duyệt bằng cách gọi lặp `search-by-page` (`features/crud/excel.ts:75`), giới hạn số dòng và tải `exceljs` 908 KB | notification – TV5 |

**Cần chốt nghiệp vụ (ghi vào SRS trước khi sửa):**
- Danh sách đơn của Staff đang có tab **"Nháp"** (đếm 2 đơn DRAFT). Đơn nháp là dữ liệu riêng của sinh viên chưa nộp; nên lọc `status <> 'DRAFT'` cho Staff/Admin, hoặc ghi rõ trong SRS lý do được xem.
- Sinh viên chỉ thấy phòng dưới dạng bảng 1 dòng có ô tìm kiếm (xem C.2) — SRS FR-ROOM-08 mô tả "xem thông tin", không phải "tra cứu".

---

## B. Giao diện chung (áp dụng cho mọi cổng)

### B.1. Bảng dữ liệu không responsive trên điện thoại

- **Hiện trạng:** `pages/CrudPage.tsx:25` import trực tiếp `DataTable`; `ResponsiveTable` (hiển thị thẻ dưới 640 px) chỉ được dùng ở 3 nơi. Kết quả chạy thật ở 375 px: màn Hóa đơn sinh viên chỉ thấy cột `#`, `Thao tác`, `Mã`, `Loại` — **số tiền, hạn đóng, trạng thái bị đẩy ra ngoài màn hình**. Màn "Việc của tôi" của KTV (`technicianRoutes.tsx` → `CrudPage resource="maintenance"`) cũng chịu ảnh hưởng tương tự.
- **Tác động:** vi phạm yêu cầu mobile-first từ 360 px cho Student/KTV (CLAUDE.md, UI-UX §27). Đây là màn sinh viên dùng nhiều nhất.
- **Hướng sửa:**
  1. Trong `CrudPage`, thay `Table` bằng `ResponsiveTable`, truyền thêm cấu hình thẻ từ `resources.ts`: `card: { title, subtitle, amount?, status, meta[] }`.
  2. Khai trong `CrudResource` một trường `mobileCard` (registry, không hardcode trong component). Ví dụ hóa đơn: tiêu đề = loại + kỳ thu, số lớn = tổng tiền, chip = trạng thái/quá hạn, meta = hạn đóng.
  3. Thẻ cả khối là vùng bấm mở chi tiết (vùng chạm ≥ 44 px), ẩn cột `#` và nhóm icon thao tác trên mobile.

### B.2. Thiếu cột định danh trong danh sách

- **Phòng & Giường** (`/staff/rooms`, `/admin/rooms`): cột `Phòng, Tầng, Giới tính, Trạng thái, Giường trống` — **không có Tòa**. Với DataTest, 6 dòng liền nhau đều là `M101`. Thêm cột **Tòa** (đặt trước Phòng), đổi "Giường trống" thành **"Đang ở / Sức chứa"** dạng `3/4` kèm thanh nhỏ; thêm cột **Loại phòng**.
- **Hóa đơn**: có `Mã, Loại, Kỳ thu, Tổng tiền, Hạn đóng, Trạng thái` nhưng **không có người/phòng phải trả**. Thêm cột "Đối tượng": `Phòng K1-M101` cho điện nước, `Họ tên (mã SV)` cho phí phòng.
- **Đơn đăng ký**: cột "Mã đơn" hiển thị ID số (`2, 4, 6…`). Dùng mã nghiệp vụ hoặc bỏ cột; thêm "Đơn vị nguyện vọng", "Loại phòng mong muốn".

### B.3. Tham chiếu hiển thị ID thô

- **Hiện trạng:** chi tiết hóa đơn hiển thị **"Sinh viên #28"**. README backend thừa nhận "một số tham chiếu dùng ID; timeline trả `actorId`".
- **Hướng sửa (backend):** trong DTO chi tiết/danh sách trả kèm `studentName`, `studentCode`, `roomLabel`, `actorName` bằng JOIN trong truy vấn đọc (vẫn đi qua Service sở hữu dữ liệu, không vi phạm ranh giới module: dùng view `v_current_stay`/truy vấn đọc trong module sở hữu, hoặc gọi Service `account` lấy tên theo lô ID).
- **Hướng sửa (frontend):** thêm kiểu trường `reference` trong `resources.ts` với `labelKey`, để `CrudPage` hiển thị nhãn thay vì ID; không gọi thêm API từng dòng (tránh N+1 phía client).

### B.4. Chế độ "xem chi tiết" dùng lại bố cục form

- Modal "Chi tiết hóa đơn" có câu hướng dẫn nhập liệu *"chọn tòa nhà trước rồi chọn phòng thuộc tòa"* dù đang ở chế độ chỉ đọc; các giá trị đặt trong ô xám giống input.
- **Hướng sửa:** chế độ xem dùng `Descriptions` (nhãn – giá trị, 2 cột desktop), ẩn mọi `help` của form; số tiền căn phải, in đậm; hiển thị **các dòng chi tiết hóa đơn** (`invoice_item`: điện, nước, phí phòng, điều chỉnh) dạng bảng nhỏ có tổng.

### B.5. Nhập Excel ở mọi màn

- Nút "Nhập Excel" có ở Đơn đăng ký, Hóa đơn, Phòng… (`CrudExcelPanel` gắn chung trong `CrudPage.tsx:1504`). Mỗi dòng Excel là một lần gọi `save-or-update` riêng từ trình duyệt → lô không nguyên tử, lỗi giữa chừng để lại dữ liệu dở dang.
- **Quyết định của nhóm (19/11/2026): giữ "Nhập Excel" ở mọi màn.** Hướng sửa vì vậy là làm cho nhập Excel an toàn, không bỏ nút:
  1. Mẫu Excel sinh từ registry phải chứa **đủ trường nghiệp vụ** của tài nguyên (xem [dac-ta-truong-nghiep-vu-day-du.md](dac-ta-truong-nghiep-vu-day-du.md)), cột bắt buộc đánh dấu `*`, cột tham chiếu nhập bằng **mã** (mã tòa, mã SV, mã phòng) thay vì ID.
  2. Thêm endpoint chung phía server `POST /api/<resource>/import/validate` và `/import/commit`: kiểm tra toàn bộ file, trả lỗi theo dòng, rồi ghi **trong một transaction** (hoặc từng dòng có báo cáo kết quả) — giống import sinh viên hiện có.
  3. Với tài nguyên có luồng trạng thái (đơn, hợp đồng, hóa đơn, ticket), nhập Excel chỉ tạo bản ghi ở **trạng thái đầu** (DRAFT/PENDING/UNPAID/OPEN) và vẫn qua Service/Policy, không cho file tự đặt trạng thái, điểm ưu tiên hay số tiền suy diễn.

### B.6. Kiến trúc thông tin và điều hướng

- Sidebar Staff/Admin có khoảng 30 mục, trong đó 12 mục danh mục (Trường, Khoa, Khóa, Quê quán, Năm học, Tiện nghi, Hạng mục sửa chữa, Tầng, Vị trí kho, Loại phòng, Kỳ học, Đợt đăng ký).
  - **Đề xuất:** gom danh mục ít dùng vào một trang **"Danh mục"** dạng lưới thẻ có ô tìm kiếm (giống trang Settings của Shopify/Stripe), giữ trên sidebar các mục vận hành hằng ngày: Dashboard, Việc chờ xử lý, Đơn, Phòng, Sinh viên, Hợp đồng, Hóa đơn, Cọc, Sửa chữa, Thông báo.
  - Thêm **ô tìm kiếm toàn cục / bảng lệnh (Ctrl+K)**: tìm sinh viên theo mã/họ tên, phòng theo mã, mở thẳng chi tiết. Đây là thao tác Staff dùng nhiều nhất tại quầy.
- Không có **tiêu đề trang và breadcrumb** trên màn danh sách (chỉ có ô tìm kiếm). Thêm `PageHeader` (đã có component) với tiêu đề, mô tả ngắn và breadcrumb `Quản lý › Phòng & Giường › K1-M101`.
- Trang mặc định của Admin là `/admin/users` (`navigation.ts:177`) trong khi Staff vào Dashboard. Admin có mọi quyền nghiệp vụ, nên vào `/admin/dashboard`.
- Nhãn trộn tiếng Anh: "Home" (`navigation.ts:324`), "Dashboard". Đổi thành "Trang chủ", "Tổng quan". Biểu tượng mục "Phòng" trùng biểu tượng "Home" ở thanh dưới của sinh viên.

### B.7. Thương hiệu và giao diện đăng nhập

- Trang `/login` của React dùng logo "DMS" xanh dương, nút xanh dương; ứng dụng Staff dùng "TLU" xanh lá; trang Keycloak nền tối, nút xanh dương. Ba bộ màu khác nhau trong một lần đăng nhập.
- Trang `/login` chỉ có một nút "Đăng nhập" dẫn sang Keycloak → thêm một bước bấm không cần thiết.
- **Hướng sửa:** tự chuyển sang Keycloak khi chưa đăng nhập (giữ trang `/login` cho thông báo lỗi/đăng xuất); tạo **theme Keycloak** (`themes/dms/login`) dùng chung token màu từ `app/theme.ts` và logo trường.

### B.8. Thao tác hàng loạt và trạng thái bảng

- Bảng có checkbox chọn dòng nhưng không thấy thanh thao tác hàng loạt khi chọn. Theo Polaris: chỉ hiện checkbox khi có thao tác hàng loạt; khi chọn, hiện thanh "Đã chọn N · [Xác nhận thu tiền] [Xuất Excel]" và nói rõ phạm vi (trang này hay toàn bộ kết quả lọc).
- Ứng viên thao tác hàng loạt có giá trị: *xác nhận thanh toán nhiều hóa đơn*, *giao nhiều ticket cho KTV*, *duyệt nhiều đơn đã đủ điều kiện*. Backend xử lý từng ID trong transaction riêng và trả kết quả từng dòng (giống mẫu job).
- **Bộ lọc nhanh/đã lưu:** với Hóa đơn và Đơn đăng ký, thêm view có tên rõ nghĩa: "Quá hạn chưa thu", "Đơn chờ duyệt của đơn vị tôi". Tab trạng thái hiện có là bước đầu tốt.

### B.9. Trạng thái rỗng, tải, lỗi

- Trạng thái rỗng nên có câu giải thích + hành động chính (ví dụ sinh viên chưa có phòng: "Bạn chưa được xếp phòng. [Xem đợt đăng ký đang mở]"). Hiện bảng rỗng chung.
- Dùng skeleton theo hình dạng thẻ/bảng (đã có `PageSkeleton`) thay vì spinner toàn trang.

### B.10. Khả năng tiếp cận

- Nhóm icon thao tác (👁 ✎ 🗑) cần `aria-label` + tooltip; kiểm tra tương phản chip trạng thái (vàng nhạt "Chưa thanh toán" trên nền trắng) đạt 4,5:1.
- Bảng giữ ngữ nghĩa `<table>`; thẻ trên mobile dùng `<ul>/<li>` và tiêu đề `<h3>`.

---

## C. Đặc tả giao diện đề xuất theo từng cổng

Các khung dưới đây là wireframe mức bố cục; màu và khoảng cách lấy từ `app/theme.ts`, component lấy từ `components/index.ts` (không import trực tiếp Ant Design trong trang nghiệp vụ).

### C.1. Sinh viên – Trang chủ (`/student`)

**Hiện trạng:** 8 thẻ điều hướng giống nhau với phụ đề "Mở màn hình quản lý" (lặp lại thanh điều hướng, không có thông tin). **Đề xuất:** trang "tình trạng của tôi", ưu tiên việc cần làm (giống trang chủ cổng sinh viên StarRez).

```
┌──────────────────────────────┐
│ TLU  Ký túc xá        🔔3  (A)│
├──────────────────────────────┤
│ Xin chào, Nguyễn Văn An      │
│ HK1 2026–2027                │
│ ┌──────────────────────────┐ │
│ │ ⚠ CẦN LÀM (2)            │ │  ← chỉ hiện khi có việc
│ │ • Hóa đơn phí phòng      │ │
│ │   2.500.000 đ · hạn 16/11│ │
│ │   [Xem hóa đơn]          │ │
│ │ • Nghiệm thu sửa chữa    │ │
│ │   Vòi nước P.M101        │ │
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │
│ │ 🏠 PHÒNG CỦA TÔI         │ │
│ │ K1 · M101 · Giường 01    │ │
│ │ Trưởng phòng: Trần B     │ │
│ │ Bạn cùng phòng: 3 người >│ │
│ └──────────────────────────┘ │
│ ┌────────────┐┌────────────┐ │
│ │ HỢP ĐỒNG   ││ TIỀN CỌC   │ │
│ │ Hiệu lực   ││ 1.000.000đ │ │
│ │ đến 30/06  ││ Đủ         │ │
│ └────────────┘└────────────┘ │
│ THÔNG BÁO MỚI                │
│ • Lịch cắt nước tòa K1  2 giờ│
│ • Mở đợt đăng ký hè   hôm qua│
├──────────────────────────────┤
│ Trang chủ  Phòng  Hóa đơn  Báo hỏng │
└──────────────────────────────┘
```

- **Dữ liệu:** một endpoint tổng hợp `GET /api/me/overview` (module notification, Tier 6) trả `room`, `contract`, `depositBalance`, `pendingInvoices[]`, `tasks[]` (nghiệm thu, bù cọc, hạn kích hoạt), `latestNotifications[3]`. Một request thay vì 5.
- **Theo trạng thái sinh viên:** chưa có phòng → thẻ "Đợt đăng ký đang mở" + nút "Đăng ký ở KTX"; đang giữ chỗ → đếm ngược hạn kích hoạt; hợp đồng sắp hết hạn → nút "Gửi yêu cầu gia hạn".

### C.2. Sinh viên – Phòng của tôi (`/student/room`)

**Hiện trạng:** bảng 1 dòng có ô tìm kiếm và cột `Tòa | Phòng | Giường` bị cắt ngang. **Đề xuất:**

```
┌──────────────────────────────┐
│ ← Phòng của tôi              │
│ ┌──────────────────────────┐ │
│ │ K1 · Phòng M101          │ │
│ │ Tầng 1 · Nam · 4 giường  │ │
│ │ Bạn ở giường 01          │ │
│ │ [▣01 bạn][▣02][▣03][□04] │ │  ← sơ đồ giường, □ trống
│ └──────────────────────────┘ │
│ TRƯỞNG PHÒNG                 │
│  (T) Trần Văn B              │
│ BẠN CÙNG PHÒNG               │
│  (L) Lê Văn C                │
│  (P) Phạm Văn D              │
│ TIỆN NGHI                    │
│  Điều hòa · Nóng lạnh · Wifi │
│ ┌──────────────────────────┐ │
│ │ [🔧 Báo hỏng phòng này]   │ │
│ │ [⇄ Yêu cầu chuyển phòng] │ │
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

Dùng `BedCard`/`RoomCard` đã có trong `components/business`. Không hiển thị mã SV/SĐT của bạn cùng phòng (chỉ họ tên theo FR-ROOM-08).

### C.3. Sinh viên – Hóa đơn (`/student/invoices`)

```
┌──────────────────────────────┐
│ Hóa đơn                      │
│ Cần thanh toán: 2.932.500 đ  │
│ [Tất cả][Chưa TT 2][Đã TT]   │
│ ┌──────────────────────────┐ │
│ │ Phí phòng · HK1 26–27    │ │
│ │ 2.500.000 đ   (Quá hạn)  │ │
│ │ Hạn 16/11/2026         > │ │
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │
│ │ Điện nước · T11/2026     │ │
│ │ Phòng M101               │ │
│ │ 432.500 đ  (Chưa TT)     │ │
│ │ Trưởng phòng nộp       > │ │
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

Chi tiết hóa đơn: dòng tiền (điện = tiêu thụ × đơn giá…), hướng dẫn nộp tiền tại quầy (SRS không có cổng thanh toán), lịch sử trạng thái. Hóa đơn điện nước ghi rõ "Hóa đơn của phòng – trưởng phòng nộp" theo mô hình tiền của SAD.

### C.4. Sinh viên – Báo hỏng (tạo ticket)

- Form một cột: Loại sự cố (chip chọn nhanh có icon), Mức độ (3 nút phân đoạn Thấp/TB/Cao), Mô tả, Ảnh (tối đa 3, chụp trực tiếp bằng camera `accept="image/*" capture`), vị trí mặc định = phòng hiện tại (chỉ đọc).
- Sau khi gửi: màn xác nhận với mã ticket + timeline `StatusTimeline`. Khi RESOLVED: thẻ "Cần nghiệm thu" ở trang chủ với 2 nút **Chấp nhận** / **Chưa đạt (nhập lý do)**.

### C.5. Kỹ thuật viên – Việc của tôi (`/technician/tasks`)

```
┌──────────────────────────────┐
│ Việc của tôi       🔔  (K)   │
│ [Đang làm 2][Được giao 3][Xong]│
│ ┌──────────────────────────┐ │
│ │ 🔴 CAO · Mất điện        │ │
│ │ K1 · Tầng 2 · P.M203     │ │
│ │ Giao 2 giờ trước         │ │
│ │ [▶ Bắt đầu]  [Chi tiết]  │ │
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │
│ │ 🟡 TB · Vòi nước rò      │ │
│ │ K1 · P.M101 · Đang làm   │ │
│ │ [+ Ghi chú] [✓ Hoàn thành]│ │
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

- Sắp xếp mặc định: mức độ giảm dần rồi thời gian giao tăng dần. Nút hành động chính ngay trên thẻ (theo `allowedActions`), không phải mở chi tiết mới làm được.
- "Hoàn thành" mở bottom sheet: ghi chú kết quả + ảnh sau sửa.

### C.6. Staff – Dashboard (`/staff/dashboard`)

Bố cục hiện tại (4 KPI → cơ cấu giường/nợ → việc cần theo dõi → lối tắt) đã đúng mẫu "KPI trên – chẩn đoán giữa – ngoại lệ dưới". Đề xuất:
- KPI có **so sánh kỳ trước** (▲/▼ so với tháng trước) và bấm vào để mở danh sách đã lọc (ví dụ "Hóa đơn chưa thu" → `/staff/invoices?status=UNPAID`).
- Thêm biểu đồ **xu hướng**: tiền thu theo tháng (cột), tỷ lệ lấp đầy theo tòa (thanh ngang theo tòa K1/N2/N3), ticket mở/đóng theo tuần (đường).
- Bỏ khối "Lối tắt nghiệp vụ" dạng carousel 1/5 (lặp sidebar); thay bằng **"Việc chờ xử lý"** có số đếm và nút đi thẳng.
- Bộ chọn phạm vi (đơn vị/tòa/học kỳ) đặt ở đầu trang, áp cho toàn bộ dashboard; Staff có quyền tổng hợp mới thấy "Toàn KTX".

### C.7. Staff – Danh sách (mẫu dùng chung cho mọi `CrudPage`)

```
Quản lý › Phòng & Giường
Phòng & Giường                                   [Xuất Excel] [+ Thêm phòng]
Quản lý phòng, giường và trưởng phòng của đơn vị K1
┌─────────────────────────────────────────────────────────────────────────┐
│ 🔍 Tìm mã phòng…   Tòa [K1 ▾]  Tầng [Tất cả ▾]  Giới tính [▾]  Còn chỗ ☐ │
│ [Tất cả 18] [Hoạt động 14] [Bảo trì 2] [Đã đóng 2]        Cột ⚙  Mật độ ≡ │
├────┬──────┬───────┬──────┬──────────┬──────────┬────────────┬───────────┤
│ ☐  │ Tòa  │ Phòng │ Tầng │ Loại     │ Giới tính│ Đang ở     │ Trạng thái│
│ ☐  │ K1   │ M101  │ 1    │ 4 giường │ Nam      │ ███░ 3/4   │ Hoạt động │
│ ☐  │ N2   │ M101  │ 1    │ 6 giường │ Nữ       │ ██████ 6/6 │ Hoạt động │
├────┴──────┴───────┴──────┴──────────┴──────────┴────────────┴───────────┤
│ Đã chọn 2  [Đổi trạng thái…] [Xuất]                       ‹ 1 2 3 ›     │
└─────────────────────────────────────────────────────────────────────────┘
```

- Bộ lọc phổ biến hiển thị sẵn trên thanh (Tòa, Tầng), bộ lọc ít dùng nằm trong "Bộ lọc". Trạng thái bộ lọc đồng bộ URL (đã có cho search/page/size; mở rộng cho mọi filter để chia sẻ link).
- Thao tác dòng: bấm cả dòng mở chi tiết; menu "⋯" cho Sửa/Xóa thay vì 3 icon liền nhau.
- Chi tiết mở **Drawer bên phải** (giữ ngữ cảnh danh sách) cho đối tượng đơn giản; trang riêng cho đối tượng có luồng (đơn, hợp đồng, ticket) — đúng như hiện có.

### C.8. Staff – Chi tiết đơn đăng ký và phân giường

- Bố cục 2 cột: trái = thông tin sinh viên, diện ưu tiên (nút xác nhận đúng/sai từng diện), điểm ưu tiên; phải = `StatusTimeline` + `ActionBar` (Duyệt / Từ chối có lý do / Phân giường).
- "Phân giường" mở **split view**: danh sách phòng phù hợp (lọc sẵn giới tính, loại phòng, đơn vị) bên trái, sơ đồ giường của phòng chọn bên phải (`BedPicker`). Xử lý 409 `BED_TAKEN`: tải lại sơ đồ, báo "Giường vừa được xếp cho người khác".

### C.9. Staff – Thu tiền tại quầy

Luồng thường gặp: sinh viên đến quầy → Staff tìm theo mã SV → thấy mọi khoản phải nộp → xác nhận.
- Trang **"Công nợ sinh viên"**: ô tìm mã SV (Ctrl+K), thẻ hồ sơ, bảng khoản chưa nộp (phí phòng, bù cọc, hóa đơn điện nước của phòng nếu là trưởng phòng), chọn nhiều khoản → "Xác nhận đã thu" (phương thức, người nộp) → in/tải **biên nhận PDF**.
- Backend đã có `DebtQuery` và `confirm-payment`; cần endpoint xác nhận nhiều hóa đơn (mỗi hóa đơn khóa riêng theo thứ tự ID).

### C.10. Admin

- Trang mặc định: Dashboard toàn KTX.
- **Phân quyền theo vai trò** (`RolePermissionsPage`, 886 dòng): giữ ma trận; thêm ô "So sánh với mặc định" và hiển thị số người dùng bị ảnh hưởng trước khi lưu.
- **Nhật ký hệ thống**: hiển thị diff trước/sau dạng 2 cột thay vì JSON thô; lọc theo đối tượng và mở được đối tượng liên quan.
- **Tài khoản**: tab Vai trò thay vì một danh sách 64 dòng lẫn sinh viên và cán bộ; sinh viên quản lý ở màn Sinh viên.

---

## D. Frontend – kỹ thuật

### D.1. Bundle 1,6 MB không chia theo route

- `dist/assets/App-*.js` 1,6 MB + `index-*.js` 424 KB; 0 lần dùng `lazy(` trong `src/`. Sinh viên trên 4G phải tải toàn bộ màn Admin.
- **Hướng sửa:** trong `app/router/*Routes.tsx` dùng `lazy: () => import('../../pages/...')` của React Router; tách theo cổng (`/student`, `/staff`, `/technician`, `/admin`). `exceljs` đã được import động – giữ nguyên. Đặt ngân sách trong `vite.config.ts` (`build.chunkSizeWarningLimit`) và kiểm tra trong CI.

### D.2. Tệp quá lớn, khó bảo trì

| Tệp | Dòng | Vấn đề | Hướng tách |
|---|---|---|---|
| `pages/CrudPage.tsx` | 2.209 | Danh sách, bộ lọc, form, chi tiết, Excel, xóa trong một component | `CrudListView`, `CrudFilterBar`, `CrudFormModal`, `CrudDetailView`, hook `useCrudQuery`/`useCrudMutations` |
| `features/crud/resources.ts` | 1.548 | Mọi tài nguyên trong một registry | Một tệp mỗi module: `features/<module>/resources.ts`, gộp tại `features/crud/index.ts` (giảm xung đột merge giữa 5 thành viên) |
| `index.css` | 4.241 | 231 `!important`, 84 mã màu hex | Ghi đè Ant Design qua `ConfigProvider` token/`components` trong `app/theme.ts`; CSS module cho bố cục từng layout; xóa `!important` dần |
| `pages/staff/StaffHomePage.tsx` | 924 | Dashboard nhiều khối | Tách mỗi khối thành component trong `features/notification/dashboard/` |

### D.3. Form chưa thống nhất

- CLAUDE.md quy định React Hook Form + Zod, nhưng chỉ 6 tệp dùng RHF, 3 tệp dùng Zod, 5 tệp dùng `Form.useForm` của Ant Design. Chọn một chuẩn (RHF + Zod cho form nghiệp vụ mới) và ghi vào UI-UX §27; sinh schema Zod từ registry để validation phía client khớp backend.

### D.4. Kiểu dữ liệu

- 11 chỗ `any`, 6 chỗ tắt lint/`ts-ignore`. Sinh kiểu TypeScript từ OpenAPI (thêm `springdoc-openapi` ở backend, `openapi-typescript` ở frontend) để contract API không lệch khi đổi DTO.

---

## E. Backend – kỹ thuật

### E.1. Khung CRUD động trả `SELECT *`

- `AbstractSqlCrudServiceImpl` (`insertRecord`/`updateRecord`) trả `SELECT * FROM <table>` dưới dạng `Map`; audit lưu nguyên dòng `before`. Hiện an toàn vì bảng dùng khung không có cột bí mật, nhưng nếu sau này áp cho bảng có cột nhạy cảm sẽ lộ dữ liệu và ghi vào audit.
- **Hướng sửa:** mỗi `*CrudServiceImpl` khai danh sách cột trả về (allow-list) và hàm `toResponse` sang record DTO; audit chỉ ghi các trường thay đổi (diff) thay vì cả dòng. Có 20 truy vấn `SELECT *` cần rà.

### E.2. Tìm kiếm chưa có chỉ mục phù hợp

- 52 điều kiện `ILIKE`/`strpos(lower(...))`; chưa có `pg_trgm`. Với 1.500 sinh viên thì chấp nhận được, nhưng màn Nhật ký và Thông báo sẽ tăng nhanh.
- **Hướng sửa:** migration mới `CREATE EXTENSION IF NOT EXISTS pg_trgm` + `GIN (lower(full_name) gin_trgm_ops)` cho `app_user`, `student_profile.student_code`, `audit_log(created_at)`; đo bằng `EXPLAIN ANALYZE` với dữ liệu 10× DataTest trước khi thêm.

### E.3. Tài liệu backend lệch mã

- README liệt kê nhiều luồng là "còn lại" trong khi đã có endpoint (mục A). Cập nhật README và bảng tiến độ trong `Docs/04 Testing` sau mỗi PR lớn.

### E.4. Tài liệu API tự động

- Chưa có OpenAPI. Thêm `springdoc-openapi-starter-webmvc-ui` (chỉ bật ở profile `dev`) để sinh `/v3/api-docs`, dùng cho frontend (D.4) và đối chiếu với `API-Spec-DMS.md`.

---

## F. Bảo mật và vận hành

### F.1. Chưa có cấu hình triển khai production

- SAD yêu cầu Compose 3 dịch vụ `nginx` (TLS, phục vụ React, proxy `/api`), `backend`, `postgres`. Repo chỉ có `docker-compose.yml` cho dev (PostgreSQL + Keycloak); không có `Dockerfile` backend, không có cấu hình nginx.
- **Hướng sửa:**
  1. `DMS/backend/Dockerfile` nhiều tầng (build bằng `mvnw -DskipTests package`, chạy bằng JRE 17 non-root, `TZ=Asia/Ho_Chi_Minh`).
  2. `DMS/nginx/nginx.conf`: TLS, gzip/brotli, cache dài cho `assets/*` có hash, `try_files` cho SPA, proxy `/api` và (tùy chọn) `/auth` tới Keycloak.
  3. Header bảo mật tại nginx: `Content-Security-Policy` (cho phép origin Keycloak), `Strict-Transport-Security`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`.
  4. `docker-compose.prod.yml` với healthcheck, `restart: unless-stopped`, volume `pgdata`, `uploads`, giới hạn bộ nhớ JVM.

### F.2. Sao lưu và giám sát

- Script DataTest đã có mẫu `pg_dump`; chuẩn hóa thành job sao lưu hằng ngày cho `pgdata`, `uploads` và DB Keycloak, giữ 7 bản, có hướng dẫn khôi phục được thử thật.
- Actuator chỉ mở `health` (tốt). Thêm `metrics`/`prometheus` nội bộ (không qua nginx) và log JSON có `requestId` để truy vết.

### F.3. Dữ liệu cục bộ chứa mật khẩu

- `backend/data/local-run/*.local`, `backend/data/datatest/accounts.local` chứa mật khẩu thử, được `.gitignore` bỏ qua (đã kiểm tra). Ghi nhớ không chia sẻ thư mục `backend/data` khi gửi bài; nhắc trong README.
- `KC_BOOTSTRAP_ADMIN_PASSWORD` trong `DMS/.env` không còn khớp với tài khoản quản trị Keycloak hiện tại (Keycloak chỉ dùng biến này khi tạo DB lần đầu). Bổ sung ghi chú trong README: đổi mật khẩu quản trị Keycloak phải cập nhật `.env` hoặc dùng service account `dms-admin`.

---

## G. Kiểm thử và quy trình

### G.1. Frontend chưa có kiểm thử

- Không có test runner; CI chỉ `lint` + `build`.
- **Hướng sửa:**
  - Vitest + Testing Library cho `statusConfig`, `errorMessages`, `format.ts`, `ActionBar` (chỉ hiện nút trong `allowedActions`), `RequireRole`.
  - Playwright E2E dùng DataTest: mỗi cổng 3–5 kịch bản chính (SV tạo ticket → KTV hoàn thành → SV nghiệm thu; Staff duyệt đơn → phân giường → kích hoạt; Staff xác nhận thu tiền). Chạy viewport 375 px cho Student/KTV, 1366 px cho Staff/Admin. `DMS/scripts/datatest/check-ui.cjs` có thể là điểm khởi đầu.
  - Thêm job CI chạy E2E trên Compose (có thể chỉ chạy khi PR vào `main`).

### G.2. Kiểm thử hiệu năng

- SRS yêu cầu 95% request < 2 s với 100 người dùng đồng thời. Chưa thấy kịch bản k6/JMeter. Đề xuất `DMS/scripts/load/k6-search.js` cho các `search-by-page` chính và `issue-utility`, chạy với dữ liệu nhân bản 10×.

### G.3. Độ phủ nhánh

- Độ phủ nhánh 60–71% theo gói. Ưu tiên test nhánh lỗi cho `account.service.impl` (74% dòng, thấp nhất) và `maintenance.service.impl` (60% nhánh).

---

## H. Lộ trình đề xuất

| Đợt | Thời lượng | Nội dung | Phụ trách gợi ý |
|---|---|---|---|
| 1 – Sửa nhanh hiển thị | 3–4 ngày | B.1 (mobile card), B.2 (cột Tòa/Đối tượng), B.5 (mẫu Excel đủ trường), B.6 (nhãn tiếng Việt, trang mặc định Admin), D.1 (lazy route) | TV1 (frontend-core) + TV2 (phòng) + TV5 (hóa đơn) |
| 2 – Trải nghiệm sinh viên/KTV | 1 tuần | C.1–C.5, `GET /api/me/overview`, FR-ROOM-08, B.3 (tên thay ID) | TV2, TV4 |
| 3 – Nghiệp vụ còn thiếu | 1–2 tuần | FR-BIL-12, FR-CON-12, xuất báo cáo server, biểu đồ xu hướng, thu tiền tại quầy (C.9) | TV5, TV2/TV4 |
| 4 – Ổn định & triển khai | 1 tuần | F.1 (Dockerfile/nginx/prod), F.2 (sao lưu), G.1 (Vitest + Playwright), G.2 (k6), cập nhật README | TV1 + cả nhóm |
| 5 – Dọn kỹ thuật | liên tục | D.2 tách tệp, D.3 chuẩn form, D.4/E.4 OpenAPI, E.1 allow-list cột, E.2 chỉ mục | Theo module |

Mỗi đợt làm trên nhánh `<loai>/<module>-<viec>` từ `develop`, PR kèm cập nhật tài liệu liên quan theo quy ước README.

---

## I. Danh sách kiểm tra sau khi sửa

- [ ] Ở 360 px, mọi màn của Sinh viên/KTV không có thanh cuộn ngang; số tiền, hạn, trạng thái thấy ngay.
- [ ] Không còn chuỗi dạng `#<số>` thay cho tên trong chi tiết/danh sách.
- [ ] Danh sách phòng có cột Tòa; danh sách hóa đơn có cột Đối tượng.
- [ ] Bundle tải lần đầu của cổng Sinh viên < 500 KB (gzip).
- [ ] `npm run test` và Playwright chạy trong CI.
- [ ] `docker compose -f docker-compose.prod.yml up` chạy được trên máy sạch, có HTTPS và header bảo mật (kiểm bằng `curl -I`).
- [ ] README backend phản ánh đúng các luồng đã làm/chưa làm.

---

## Nguồn tham khảo

- StarRez – trang chủ và tính năng chọn phòng/bạn cùng phòng: https://starrez.com/ , https://www.starrez.com/features/roommate-matching
- Carleton University – StarRez cổng một cửa: https://carleton.ca/its/2024/starrez-provides-one-stop-solution-for-housing-and-residence-life-services/
- Wake Forest – hướng dẫn cổng sinh viên StarRez: https://prod.wp.cdn.aws.wfu.edu/sites/303/2017/07/starrez-student-portal-instructions.pdf
- Shopify Polaris – Index table: https://polaris.shopify.com/components/index-table ; Resource index: https://shopify.dev/docs/api/app-home/patterns/templates/resource-index
- Ant Design Pro – ProTable: https://www.mintlify.com/ant-design/pro-components/components/pro-table ; Layout: https://pro.ant.design/docs/layout
- Basis Design System – Saved views: https://design.basis.com/patterns/saved-views
- Admin data table pattern: https://meta.koder.dev/specs/patterns/admin-data-table/
- Dashboard design (KPI trên – chẩn đoán giữa – ngoại lệ dưới): https://idealogic.io/blog/dashboard-design ; https://uxpilot.ai/blogs/dashboard-design-principles

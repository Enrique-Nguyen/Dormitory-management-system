# Báo cáo công việc ngày 10/10/2026

> Phạm vi: dựng template UI (khung giao diện dùng chung) cho `DMS/frontend`
> Căn cứ: `Docs/02 External Design/Screen Design/DMS-UI-UX-Design.md` (mục 2, 3, 4, 5, 22C, 25, 25A, 26, 27)
> Thực hiện: Claude Code, theo yêu cầu của người dùng
> Trạng thái: hoàn thành phần khung, đã review và sửa 10 phát hiện (mục 7)

---

## 1. Mục tiêu

Khung frontend đã có sẵn (Vite, React 19, Ant Design 6, React Router 8, TanStack Query, React Hook Form + Zod, Axios) nhưng mọi trang chỉ là placeholder một dòng, `statusConfig` và `components/` còn rỗng. Theo UI mục 27.3, phần layout, guard, theme, `statusConfig`, `errorMessages` phải xong sớm để năm thành viên dùng chung. Báo cáo này ghi lại phần đó.

Phạm vi đã thống nhất với người dùng:

- Có: shell (layout, guard, router), component dùng chung, toàn bộ route mục 26 với trang giữ chỗ, màn Login và Đổi mật khẩu.
- Không: nội dung từng màn nghiệp vụ, biểu đồ (✂ P2), thư viện mới, test runner.
- Chạy khi chưa có backend: mock vai trò bằng biến môi trường.

## 2. Kết quả

### 2.1. Nền tảng

| Hạng mục | File | Nội dung |
|---|---|---|
| Design tokens | `src/app/theme.ts`, `src/index.css` | Giữ token mục 2.1a; thêm biến spacing bội số 4, focus ring 2 px, chặn cuộn ngang, vùng chạm ≥ 44 px trên mobile, lớp `.dms-page`, `.dms-bottom-nav`, `.sr-only` |
| Định dạng | `src/utils/format.ts` | Tiền `1.000.000 đ`, ngày `dd/MM/yyyy`, ngày giờ, thời gian tương đối, số ngày còn lại |
| Breakpoint | `src/hooks/useViewport.ts` | `mobile` < 640, `tablet` 640–1023, `desktop` ≥ 1024 (mục 25) |
| Kiểu dữ liệu | `src/types/auth.ts`, `src/types/common.ts` | `Me` đúng API Spec 4.1, `WithActions`, `Page<T>` |

### 2.2. Xác thực, guard, mock

| File | Nội dung |
|---|---|
| `src/services/api/auth.ts` | `getMe`, `login`, `logout`, `changePassword` |
| `src/services/api/mock.ts` | Người dùng giả theo vai trò, chỉ có tác dụng ở `npm run dev` |
| `src/services/api/notification.ts`, `src/features/notification/useUnreadCount.ts` | Số thông báo chưa đọc, gọi lại mỗi 60 giây và khi đổi trang |
| `src/features/account/useMe.ts` | Query `['me']` |
| `src/app/guards/RequireRole.tsx` | Chưa đăng nhập → `/login?redirect=…`; bắt buộc đổi mật khẩu → `/change-password`; sai vai trò → trang chủ vai trò đó |
| `src/services/api/client.ts` (sửa) | Interceptor 401 thêm `reason=expired` ở các cuộc gọi sau lần tải đầu |
| `.env.example` | Hướng dẫn bật mock |

Mock: chép `.env.example` thành `.env.local` (đã được git bỏ qua). Đổi vai trò bằng `?mockRole=student | student-must-change | student-no-contract | staff | staff-cross | technician | admin`; `?mockRole=off` để xóa.

### 2.3. Layout bốn cổng (`src/app/layouts/`)

- `SidebarShell`: dùng chung cho Staff và Admin. Sidebar cố định từ 1024 px, Drawer ở màn nhỏ hơn, topbar có tìm kiếm, chuông, menu người dùng.
- `StaffLayout`: sidebar nhóm Quản lý / Tài chính / Vận hành / Hệ thống; ô tìm kiếm chuyển tới `/staff/students?q=`.
- `AdminLayout`: menu sáu mục, không chuông, không tìm kiếm.
- `StudentLayout`: dưới 640 px có header và bottom nav 4 tab; từ 640 px dùng menu ngang. Tab Báo hỏng vẫn mở được nhưng mờ khi sinh viên chưa có hợp đồng.
- `TechnicianLayout`: header gọn, không sidebar.
- Component layout: `PageHeader`, `NotificationBell`, `UserMenu` (đổi mật khẩu, đăng xuất) trong `src/components/layout/`.

### 2.4. Router (`src/app/router/`)

`app/router.tsx` được thay bằng thư mục, mỗi vai trò một file để các thành viên sửa song song, ít xung đột merge:

- `index.tsx`, `studentRoutes.tsx`, `staffRoutes.tsx`, `technicianRoutes.tsx`, `adminRoutes.tsx`.
- Đủ toàn bộ route mục 26, kể cả `/change-password`; `/staff`, `/technician`, `/admin` tự chuyển về trang chủ của cổng.
- `src/pages/PlaceholderPage.tsx` hiện tên màn, thành viên phụ trách (theo mục 27.3) và mục tham chiếu. Bốn `*HomePage` cũ giữ làm trang index của cổng.

### 2.5. Trạng thái (`src/constants/statusConfig.ts`)

Nguồn duy nhất cho nhãn, màu, icon, gồm:

- Lớp 1 (badge đặc): đơn, hợp đồng, ticket, hóa đơn, yêu cầu. Màu nền đậm để chữ trắng đạt tương phản ≥ 4.5:1.
- Lớp 2 (chip viền): Sắp hết hạn, Quá hạn trả phòng, Tạm rời hè, Quá hạn, Quá hạn bù cọc.
- Lớp 3: vận hành phòng, vận hành giường, chiếm dụng giường (Trống / Đang giữ / Đang ở).
- `getStatus` cảnh báo ở console (chỉ DEV) khi gặp trạng thái chưa khai báo.

Menu và đường dẫn trang chủ theo vai trò nằm riêng ở `src/constants/navigation.ts`.

### 2.6. Component dùng chung (`src/components/`)

| Nhóm | Component |
|---|---|
| `ui/` | `StatusBadge`, `DerivedChip`, `ConfirmDialog` + `useConfirm` (lý do bắt buộc, 5–500 ký tự), `ActionBar` (chỉ hiện nút có trong `allowedActions`), `FormField` (nhãn, dấu `*`, `aria-describedby`), `StatusTimeline` + `stepsFromFlow`, `ResponsiveTable` (thẻ dưới 640 px), `EmptyState`, `PageSkeleton` |
| `business/` (chỉ nhận props) | `BedCard`, `RoomCard`, `InvoiceCard`, `InvoiceStatus`, `ContractStatus`, `DepositLedger`, `NotificationItem` |

Cố ý không bọc các thành phần antd đã đủ dùng (Button, Input, Modal, Table…). `OccupancyChart` bỏ qua vì thuộc phần biểu đồ ✂ P2. Tất cả được export từ `components/index.ts`.

### 2.7. Login, Đổi mật khẩu, thông báo lỗi

- `LoginPage`: React Hook Form + Zod; lỗi không phân biệt sai tên hay sai mật khẩu; xử lý tạm khóa; đọc `redirect` (chỉ nhận đường dẫn nội bộ) và `reason=expired`; trong chế độ mock có link vào cổng.
- `ChangePasswordPage`: dùng cho lần đầu (bắt buộc) và từ menu người dùng; chính sách 8–64 ký tự, có chữ và số, khác mật khẩu cũ.
- `src/utils/errorMessages.ts`: thêm mã chung (mục 5.1), tài khoản (5.2) và hai mã giường; câu thông báo không chứa enum tiếng Anh. Thêm `errorCodeOf`.
- `src/hooks/useApiErrorHandler.ts`: dùng làm `onError` của mutation; gặp 409 thì tải lại dữ liệu đang hiển thị.

## 3. Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| `tsc -b` | Sạch |
| `npm run lint` (oxlint) | Sạch |
| `npm run build` | Thành công |
| Student ở 375 px | Bottom nav đủ 4 tab, không cuộn ngang |
| Staff ở 1280 px | Sidebar đủ nhóm, `/staff` chuyển sang `/staff/dashboard` |
| Staff ở 800 px | Sidebar ẩn, Drawer mở được, 15 mục menu |
| Technician, Admin | Đúng layout và menu |
| Truy cập sai vai trò | Admin vào `/staff/rooms` bị chuyển về `/admin/users` |
| `student-must-change` | Bị giữ ở `/change-password` |
| Hộp thoại lý do | Để trống bị chặn kèm thông báo; nhập lý do thì gửi được |
| `ActionBar` | Chỉ hiện nút nằm trong danh sách cho phép |
| Badge trạng thái | Mọi cặp (đối tượng, trạng thái) hiển thị đúng nhãn, màu, icon |

Các component dùng chung được thử bằng một trang tạm (`/dev-gallery`), đã xóa sau khi kiểm tra.

Sửa trong lúc kiểm tra: icon trong badge dính sát chữ (bỏ lớp bọc `span`, dùng `aria-hidden` trực tiếp); tiêu đề timeline bị đẩy sang phía đối diện mốc trong antd 6 (đưa vào cột nội dung); prop `message` của `Alert` đã bị antd 6 đánh dấu lỗi thời (đổi sang `title`).

**Chưa kiểm tra:** điều hướng bằng bàn phím (Tab, Esc, trả focus), Lighthouse Accessibility ≥ 90 (mục 25A), đăng nhập thật với backend. Chưa có test runner frontend nên không có test tự động.

## 4. Điểm lệch so với thiết kế và quyết định cần xác nhận

| Nội dung | Quyết định | Lý do |
|---|---|---|
| Route `/technician/notifications` | Thêm mới, không có ở mục 26 | Mục 4.3 đặt chuông thông báo ở header KTV |
| `?mockRole=` | Thêm ngoài kế hoạch | Đổi vai trò khi test không phải sửa env |
| Route `/staff/students` | Ghi "Chưa phân công" | Mục 27.3 không giao thành viên nào |
| Thư mục `features/` | Giữ tên scaffold (`room`, `account`…) | Mục 27 dùng tên số nhiều (`rooms`, `students`); chưa đồng bộ |
| Menu Admin | Tách "Đơn vị" và "Tòa nhà & mức cọc" thành hai mục | Mục 26 có hai route riêng |

## 5. Việc còn lại

| Việc | Phụ trách (UI 27.3) |
|---|---|
| Nối badge "Yêu cầu chờ xử lý" vào API khi có (`TODO(TV3)` trong `StaffLayout`) | TV3 |
| Dựng nội dung từng màn, thay `PlaceholderPage` trong file route của vai trò | TV1–TV5 |
| Bổ sung `errorMessages` cho mã lỗi của từng module, đồng thời với danh mục API Spec | Từng thành viên |
| Kiểm tra bàn phím và Lighthouse cho Login và layout | TV1 |
| Tách code (bundle hiện ~1,1 MB, `build` cảnh báo > 500 kB) | Khi có trang thật |
| Thống nhất tên thư mục `features/` giữa SAD/UI và scaffold | Cả nhóm |

## 6. Tệp liên quan

- Sửa: `src/app/theme.ts`, `src/index.css`, `src/constants/statusConfig.ts`, `src/services/api/client.ts`, `src/utils/errorMessages.ts`, `src/pages/LoginPage.tsx`, `src/pages/*/**HomePage.tsx` (bốn trang), `src/components/index.ts`, `CLAUDE.md`.
- Chuyển: `src/app/router.tsx` → `src/app/router/`.
- Mới: `src/app/layouts/`, `src/app/guards/`, `src/components/{ui,business,layout}/`, `src/constants/navigation.ts`, `src/services/api/{auth,mock,notification}.ts`, `src/features/account/useMe.ts`, `src/features/notification/useUnreadCount.ts`, `src/hooks/`, `src/types/`, `src/utils/format.ts`, `src/pages/{PlaceholderPage,ChangePasswordPage}.tsx`, `.env.example`.

## 7. Review và sửa lỗi

Review toàn bộ phần đã viết (không có diff vì thư mục chưa phải repo git) cho ra 10 phát hiện. Đã sửa cả 10, kèm một lỗi mới tìm thấy khi kiểm tra lại.

| # | Phát hiện | Cách sửa |
|---|---|---|
| 1 | `ContractStatus` hard-code ngưỡng "sắp hết hạn" 15 ngày, trái quy tắc tham số cấu hình trong `CLAUDE.md` | Component nhận `labels` do backend tính (API Spec mục 6: `EXPIRING`, `OVERDUE_MOVE_OUT`, `SUMMER_LEAVE`), không tự tính ngày. Khóa nhãn suy ra trong `statusConfig` đổi trùng mã API |
| 2 | Lỗi không có `errorCode` (CSRF 403, proxy 502) bị báo "Không kết nối được tới máy chủ" | Ba nhánh: có phản hồi nhưng sai khuôn (kèm mã HTTP và hướng xử lý), mất kết nối, lỗi ngoài dự kiến |
| 3 | `me` cache 5 phút làm `hasActiveContract` bị cũ | `staleTime` 60 giây, tải lại khi quay lại tab; vẫn bị invalidate khi gặp 409 |
| 4 | `/` và đường dẫn lạ đưa người đã đăng nhập về form Login | Thêm `HomeRedirect` dưới guard; `LoginPage` chuyển thẳng khi đã có phiên (giữ nguyên trang ở chế độ mock). Đăng nhập mới và phiên còn hiệu lực dùng chung một đường chuyển trang |
| 5 | `ResponsiveTable`: desktop tự phân trang 10 dòng, mobile hiện hết | Mặc định tắt phân trang ở cả hai chế độ |
| 6 | Nhãn "Hỏng", "đang ở/đang giữ/trống" lặp ngoài `statusConfig` | `BedCard` và `RoomCard` lấy nhãn từ `bedOperation`, `bedOccupancy` |
| 7 | `useUnreadCount` gọi API hai lần khi mount; tham số `enabled` thừa | Đặt `useQuery` trước effect, effect dùng `refetchQueries` với `cancelRefetch: false`; bỏ `enabled` |
| 8 | Esc hoặc nút X đóng `ConfirmDialog` khi đang gửi | Trạng thái `submitting` đưa lên `ConfirmDialog`, khóa Esc, nút X và mask khi đang gửi |
| 9 | `overflow-x: hidden` toàn cục che lỗi tràn ngang | Gỡ bỏ. Kiểm tra lại ở 360 px lộ hai lỗi thật (xem dưới) |
| 10 | `stepsFromFlow` làm mất các bước trước nhánh kết thúc khi thiếu chi tiết | Thêm tham số `reachedStatus` (lấy từ `fromStatus` của dòng timeline cuối) |

Hai lỗi tràn ngang lộ ra sau khi gỡ `overflow-x: hidden`:

- Trang Login rộng 389 px ở màn 360 px (ô lưới không co được). Sửa bằng lớp `.dms-auth-page` (`minmax(0, 400px)`), dùng cho cả Đổi mật khẩu.
- Header Staff và Admin trên mobile: logo bị xuống dòng, ô tìm kiếm bị ép nhỏ. Sửa: dưới 640 px chỉ hiện "DMS", ô tìm kiếm chuyển vào Drawer.

Việc nhỏ kèm theo: bỏ lặp bộ lọc menu trong `StudentLayout`.

Kiểm tra lại sau khi sửa: `tsc -b`, `oxlint` (exit 0) và `npm run build` đều sạch. Trên trình duyệt: `/` và đường dẫn lạ chuyển đúng về `/staff/dashboard`, `/login` ở chế độ mock vẫn hiện form, `scrollWidth` bằng chiều rộng màn ở 360 px trên các trang student, staff, login, đổi mật khẩu; Drawer của Staff mở được và có ô tìm kiếm.

Chưa kiểm tra bằng trình duyệt: chặn Esc khi `ConfirmDialog` đang gửi, và việc `useUnreadCount` chỉ còn một request (chế độ mock không gọi mạng).

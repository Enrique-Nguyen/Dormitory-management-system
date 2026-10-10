# Nhận xét thiết kế UI/UX — DMS

> File được nhận xét: `Plan/DMS-UI-UX-Design.md` (bản trên máy Enrique, 08/10/2026)
> Đối chiếu với: `Plan/SRS-DMS.md` v0.3
> Ký hiệu: 🔴 lệch SRS hoặc thiếu luồng Must · 🟡 nên sửa · 🟢 gợi ý

---

## 0. Đánh giá chung

Đây là một bản định hướng UI tốt. Có ba điểm mạnh:

- Hướng tiếp cận đúng: chia 4 trải nghiệm theo vai trò (Student và KTV mobile-first, Staff và Admin desktop-first).
- Bộ nguyên tắc ở mục 30 rất chắc, gồm: state trước action, action theo trạng thái, không dùng màu đơn độc, lỗi phải chỉ cách xử lý, không xóa cứng.
- Các ý tưởng sau đều đáng giữ: timeline cho đơn và ticket, ledger cho tiền cọc, split view khi phân giường, bảng chuyển thành card trên mobile, ưu tiên làm trước các backbone screen.

Vấn đề chính nằm ở chỗ bản thiết kế được viết cho **"sinh viên đang ở, mọi thứ bình thường"**. Nó còn thiếu phần lớn các trạng thái chuyển tiếp mà SRS v0.3 đã chốt:

- giữ chỗ và kích hoạt hợp đồng;
- trưởng phòng;
- trừ cọc theo nguyên nhân hỏng;
- kỳ hè;
- các yêu cầu gia hạn và chuyển phòng.

Đây cũng chính là những chỗ giảng viên dễ hỏi nhất khi demo.

---

## 1. 🔴 Thiếu luồng Must so với SRS

### 1.1. Kích hoạt hợp đồng (UC-32) không có màn hình nào

Phân giường xong thì hợp đồng ở trạng thái PENDING và có **hạn kích hoạt 7 ngày**. Sinh viên phải ký giấy và nộp cọc, rồi Staff mới kích hoạt.

**Hiện trạng trong thiết kế:**

- Timeline đơn chỉ có các bước "Phân phòng → Hoàn tất".
- Không có màn hình Staff nào để thu cọc và kích hoạt.

**Đề xuất:**

- **Phía sinh viên** (FR-REG-12): khi đơn ở ASSIGNED, dashboard hiện một card nổi bật:
  - "Bạn được giữ giường K1-A101-03";
  - "Hạn kích hoạt: 15/10 (còn 5 ngày)";
  - "Cần mang: giấy hợp đồng đã ký + 1.000.000 đ tiền cọc";
  - nút [Hủy giữ chỗ].
- **Phía Staff**: màn hình hợp đồng PENDING gồm:
  - mức cọc của tòa được điền sẵn;
  - checkbox "Đã ký giấy";
  - nút [Xác nhận thu cọc & kích hoạt] có confirm.
- **Timeline đơn** nên đổi thành: Đã gửi → Đang xét → Được duyệt → **Được giữ chỗ (hạn …)** → **Đã kích hoạt hợp đồng**.
- Đưa màn hình này vào **P0** và backbone screens, vì nó nằm ngay trên trục chính "phân phòng → ở".

### 1.2. Trưởng phòng và hóa đơn điện nước

SRS quy định hóa đơn điện nước **gửi trưởng phòng**, trưởng phòng nộp, và mọi thành viên trong phòng đều xem được (FR-BIL-04). Thiết kế mới chỉ ghi "đây là hóa đơn của phòng".

**Đề xuất:**

- Card hóa đơn điện nước hiển thị "Người nộp: Nguyễn Văn B (trưởng phòng)".
- Nếu người xem chính là trưởng phòng, card hiện banner "Bạn là trưởng phòng, bạn nộp hóa đơn này cho BQL".
- Màn chỉ định trưởng phòng chỉ cho chọn sinh viên **đang ở**. Sinh viên đang giữ chỗ thì hiện mờ và có giải thích.
- Màn phát hành hóa đơn điện nước cảnh báo những phòng chưa có trưởng phòng (UC-21, nhánh 3a).

### 1.3. Sửa chữa: thiếu bước Staff chọn nguyên nhân và chi phí (FR-MNT-09/10, UC-22)

Đây là nguồn phát sinh duy nhất của giao dịch trừ cọc, nhưng thiết kế chưa có bước này.

**Cần thêm cho Staff**, sau khi ticket của phòng chuyển CLOSED: một panel "Kết luận chi phí" gồm

- radio *Hao mòn tự nhiên* (bắt buộc ghi chú) / *Hư hỏng do sử dụng* (nhập chi phí);
- bản xem trước chia tiền cho N sinh viên, ví dụ "200.000 đ → 4 người × 50.000 đ".

**Các lệch khác ở phần sửa chữa:**

- **Sinh viên chọn "khu vực".** BR-06 chỉ cho sinh viên báo hỏng **phòng đang ở**; khu vực chung là việc của Staff. Form của sinh viên cần các trường: loại sự cố, mô tả, mức độ (Thấp / TB / Cao), tối đa 3 ảnh.
- **Thiếu nghiệm thu của sinh viên.** Khi ticket ở RESOLVED cần có hai nút:
  - [Chấp nhận];
  - [Chưa đạt] kèm lý do bắt buộc.
- **Thiếu bộ đếm tự đóng.** Ticket RESOLVED nên hiện "Tự đóng sau 3 ngày".
- **Thiếu trạng thái ở tab Staff.** Tab chưa có REJECTED và CANCELLED.
- **Màn KTV hiển thị ticket OPEN kèm nút "Bắt đầu xử lý".** KTV chỉ nhìn thấy ticket đã **ASSIGNED** cho mình. Tab "Mới" phải là ASSIGNED.

### 1.4. Kỳ hè (FR-REG-13..15, FR-CON-11/12) chỉ có một route

Hiện chỉ có `/student/summer` và mục này bị xếp vào P2, trong khi SRS ghi là Must. Cần ba màn hình:

1. **Sinh viên chọn lựa chọn cho hè**, gồm ba card:
   - Ở hè (có phí hè);
   - Tạm rời hè (gửi đồ, giữ cọc, quay lại HK1);
   - Rời KTX.

   Lựa chọn tạm rời hè bị khóa nếu hợp đồng chưa gia hạn sang năm sau, và phải giải thích lý do khóa.
2. **Staff duyệt lựa chọn** và ghi **phiếu gửi đồ**: số kiện, mô tả, vị trí kho, ngày gửi.
3. **Staff phân lại giường đầu HK1**:
   - danh sách sinh viên "Tạm rời hè";
   - gợi ý giường cũ nếu còn trống;
   - nút ghi ngày nhận lại đồ.

### 1.5. Thiếu các màn hình Must khác

| Màn hình | FR | Ghi chú |
|---|---|---|
| Bắt buộc đổi mật khẩu lần đầu | ACC-03 | 1.500 tài khoản import có mật khẩu mặc định, nên đây là màn đầu tiên mọi sinh viên gặp |
| Yêu cầu gia hạn: SV gửi, Staff duyệt | CON-06 | Staff hiện có nút [Gia hạn] trực tiếp, nhưng SRS là luồng *yêu cầu → duyệt* |
| Yêu cầu chuyển phòng: SV gửi, Staff chọn giường đích cùng đơn vị | CON-07, UC-18 | Nên tái dùng split view của màn phân giường |
| Hộp "Yêu cầu chờ xử lý" cho Staff | — | Gom đơn, gia hạn, chuyển phòng, lựa chọn hè, bù cọc vào một inbox |
| Xem trước và phát hành hóa đơn phí phòng theo học kỳ | BIL-05, UC-21 | Có bước preview số lượng và tổng tiền, bỏ qua người đã có hóa đơn |
| Xác nhận thanh toán | BIL-08 | Dialog nhập người nộp và hình thức nộp |
| Xác nhận bù cọc | BIL-09 | |
| Cấu hình đơn giá điện nước có ngày hiệu lực; ghi nhận thay đồng hồ | BIL-01, BIL-03 | |
| Lịch sử phân phòng trong hợp đồng (giường, từ / đến, đơn giá) | CON-05 | |
| Công nợ sau trả phòng | CON-10 | Sinh viên đã trả phòng vẫn đăng nhập để xem công nợ |

---

## 2. 🔴 Wireframe tự vi phạm quy tắc của chính tài liệu

1. **Màn duyệt và phân phòng (mục 14).**
   - Ba nút [Từ chối] [Duyệt] [Phân giường] cùng hiện khi đơn đang SUBMITTED, trái với Rule 02. Ở SUBMITTED chỉ nên có Duyệt và Từ chối. Sau khi APPROVED mới hiện panel chọn giường.
   - Mỗi giường ghi "1 chỗ trống" là sai đơn vị: giường thì không có "chỗ". Nên ghi ở cấp phòng, ví dụ "Phòng A101: còn 2/6 giường · Nam · 6 người · 1.500.000 đ/HK".
   - Thiếu nút **[Hủy giữ chỗ để phân lại]** có lý do, dùng khi đơn ở ASSIGNED (FR-REG-11).
2. **Chi tiết hợp đồng của Staff (mục 16).** Các nút [Gia hạn] [Chuyển phòng] [Trả phòng] luôn hiển thị. Cần ẩn hoặc vô hiệu theo trạng thái: hợp đồng PENDING thì chỉ có Kích hoạt và Hủy.
3. **Wizard trả phòng (mục 17).** Ví dụ có "Khoản bù cọc 200.000, Số dư cọc 800.000, Hoàn dự kiến 800.000". Theo SRS hiện tại, khoản bù cọc chưa nộp vẫn bị theo dõi như công nợ sau khi trả phòng, nên sinh viên sẽ **mất 200.000 đ hai lần**: một lần đã trừ vào số dư, một lần vẫn còn nợ bù. Đây là một **lỗ hổng trong SRS** mà wireframe làm lộ ra (xem mục 6).

---

## 3. 🟡 Hệ thống trạng thái và nhãn

Bảng màu ở mục 2.2 đang trộn ba thứ khác nhau:

- **Trạng thái lưu trong CSDL**, có màu đặc:
  - Đơn: DRAFT, SUBMITTED, APPROVED, REJECTED, ASSIGNED, CANCELLED.
  - Hợp đồng: PENDING, ACTIVE, EXPIRED, TERMINATED, CANCELLED.
  - Ticket: OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED, REJECTED, CANCELLED.
  - Hóa đơn: **UNPAID / PAID**. Bảng màu hiện không có hai trạng thái này.
- **Nhãn suy ra**, nên dùng chip viền kèm icon ⚠ và đặt cạnh badge trạng thái:
  - Sắp hết hạn
  - Quá hạn
  - Quá hạn trả phòng
  - Tạm rời hè
  - Quá hạn bù cọc

  OVERDUE trong bảng màu chính là nhãn "Quá hạn" chứ không phải trạng thái.
- **Trạng thái vận hành phòng và giường**: Hoạt động / Bảo trì / Ngừng; Hỏng. Ngoài ra còn **trạng thái chiếm giường** (trống / đang giữ / đang ở). Chi tiết phòng ở mục 15 đang trộn hai thứ này ("[04] Trống ACTIVE"), và thiếu hẳn trạng thái **đang giữ** mà FR-ROOM-07 yêu cầu.

**Đề xuất cách làm:**

- Viết một file `statusConfig.ts` duy nhất ánh xạ `(entity, status) → {label tiếng Việt, màu, icon}`.
- Người dùng **không bao giờ thấy enum tiếng Anh**. Sinh viên không cần biết `IN_PROGRESS`; hãy hiện "Đang sửa".
- Cùng một chữ ASSIGNED có nghĩa khác nhau ở đơn ("Đã giữ chỗ") và ở ticket ("Đã giao KTV"), nên label phải theo entity.

---

## 4. 🟡 Các điểm nhỏ hơn theo màn hình

- **Student Dashboard chỉ có một trạng thái.** Cần thiết kế đủ 6 biến thể:
  1. Chưa có đơn, kèm CTA "Đăng ký" nếu đợt đang mở.
  2. Đơn đang xét.
  3. Được giữ chỗ.
  4. Đang ở.
  5. Tạm rời hè.
  6. Đã trả phòng nhưng còn công nợ.

  Đây là màn quan trọng nhất của sinh viên.
- **Đăng ký phòng.**
  - Stepper có 4 bước nhưng ví dụ ghi "Bước 2 / 3".
  - Bước nguyện vọng thiếu trường **diện ưu tiên tự khai** (FR-REG-02).
  - Nếu sinh viên đã có đơn trong đợt hoặc đang có hợp đồng, phải chặn ngay ở bước 1 kèm giải thích (FR-REG-03).
- **Bottom nav của sinh viên.** "Báo hỏng" bị giấu trong mục "Khác". Có thể đổi thành Home / Phòng / Hóa đơn / Báo hỏng và chuyển Khác lên avatar. Tùy nhóm chọn.
- **Hóa đơn của sinh viên.** "Khoản cần bù cọc" không phải là hóa đơn mà là khoản trong sổ cọc. Có thể gộp vào một tab "Cần thanh toán", nhưng cần ghi rõ nguồn và link sang sổ cọc.
- **Điện nước.**
  - Ví dụ chỉ có kWh. Cần tab hoặc cột riêng cho **điện và nước**.
  - Cần danh sách "phòng còn thiếu chỉ số", vì thiếu chỉ số thì không được phát hành (UC-21, nhánh 1a).
  - Nên validate ngay khi nhập (cuối < đầu thì ô đỏ kèm lời giải thích).
  - Nên hỗ trợ phím Enter / Tab để nhập liên tục.
- **Dashboard Staff.** Bộ chọn "K1 ▼" chỉ hiện với Staff có quyền tổng hợp (ACC-08). Mỗi dòng ⚠ nên là link dẫn tới danh sách đã lọc sẵn. Biểu đồ là ✂ (FR-NOT-06), nên làm sau cùng.
- **IA của Admin.**
  - "Loại phòng" và "Kỳ học" trong SRS là việc của **Staff** (FR-ROOM-02, FR-ROOM-11). Thiết kế hiện để ở cả hai portal. Cần chốt một chỗ.
  - "Vai trò / Phân quyền" dễ bị hiểu là trình sửa role. SRS chỉ có 4 role cố định cộng với **cấp quyền tổng hợp liên đơn vị**, nên nên đổi tên và thu hẹp lại.
  - Admin còn thiếu: khóa / mở khóa tài khoản, đặt lại mật khẩu, mức cọc theo tòa.
- **Route.** `/staff` và `/staff/dashboard` bị trùng. Thiếu các route: `/change-password`, `/staff/requests`, `/staff/contracts/:id/activate` (hoặc modal), `/staff/storage` (phiếu gửi đồ), `/staff/utilities/prices`.
- **Phiên 30 phút** (NFR-SEC-04). Khi API trả 401, chuyển về /login kèm thông báo "Phiên đã hết hạn" và quay lại đúng trang cũ. Form đang nhập dở, ví dụ đơn đăng ký, nên lưu nháp.
- **Priorities.** Mục "Advanced Reports" không có trong SRS, nên bỏ. Import Excel và Audit Log là Must (ACC-05, ACC-09), không nên để P2.

---

## 5. 🟡 Accessibility và responsive

Tài liệu mới có một quy tắc "không dùng màu đơn độc". Nên bổ sung thêm:

- **Tương phản**: chữ đạt tối thiểu 4.5:1. Cẩn thận với badge cam và vàng trên nền trắng.
- **Vùng chạm**: tối thiểu 44×44 px trên mobile. Nút chính của KTV nên full-width.
- **Bàn phím**: focus ring rõ ràng; dialog phải trap focus và đóng được bằng Esc; bảng của Staff dùng được bằng bàn phím.
- **Form**: mỗi input có `<label>`; thông báo lỗi gắn với ô bằng `aria-describedby`; toast dùng `aria-live`.
- **Font**: chọn font hỗ trợ dấu tiếng Việt tốt, ví dụ *Be Vietnam Pro* hoặc *Inter*.
- **Định dạng**: thống nhất tiền `1.000.000 đ` và ngày `dd/MM/yyyy`.
- **Design tokens**: tài liệu mới có ý tưởng mà chưa có token (spacing 4/8, cỡ chữ, bo góc, mã màu cụ thể). Không có token thì 5 người sẽ làm ra 5 kiểu giao diện khác nhau.
- **Kiểm tra ở 360px**: tên phòng dạng "K1-A101-03" cộng badge cộng số tiền rất dễ tràn dòng.

---

## 6. ⚠ Vấn đề nghiệp vụ phát hiện nhờ wireframe (cần chốt lại trong SRS)

**Bù cọc chưa nộp khi trả phòng.** Theo FR-MNT-10, khi bị trừ cọc, số dư cọc giảm **và** phát sinh khoản bù cọc. Khi trả phòng, sinh viên được hoàn số dư (đã bị giảm) **và** khoản bù chưa nộp vẫn bị tính là công nợ (FR-CON-10, UC-33). Kết quả là sinh viên bị thu hai lần.

**Hướng sửa đề xuất:** khi trả phòng, các khoản bù cọc chưa nộp được **xóa nghĩa vụ**, vì phần tiền đó đã bị trừ trực tiếp vào số dư. Công nợ chỉ còn phí phòng chưa đóng và số dư cọc âm. Màn trả phòng hiển thị:

> Hoàn cọc = số dư hiện tại; bù cọc chưa nộp: được tất toán bằng số dư.

---

## 7. 🟢 Khả thi trong 12 tuần — gợi ý kỹ thuật

1. **Không tự viết 17 component UI.** Hãy dùng một thư viện có sẵn để dành thời gian cho nghiệp vụ.
   - **Ant Design** đã có Steps, Timeline, Table, Descriptions, Badge, Tag, Modal.confirm và Form validate, rất hợp với Staff và Admin. Phần mobile của sinh viên cần tự chỉnh thêm một chút.
   - Phương án thay thế: **MUI**, hoặc **shadcn/ui + Tailwind** (đẹp hơn nhưng tốn công hơn).
2. **Stack đi kèm**:
   - TanStack Query: cache dữ liệu, xử lý 409 và refetch.
   - React Hook Form + Zod: form và validate.
   - React Router: dùng loader hoặc guard theo role.
3. **Không viết lại state machine ở frontend.** API nên trả về `allowedActions` cho từng đối tượng, ví dụ `["approve","reject"]`. Frontend chỉ việc hiện nút theo danh sách này. Cách này đáp ứng Rule 02 mà không bị lệch với backend.
4. **Map lỗi 409** thành thông báo tiếng Việt cụ thể bằng `errorCode` do backend trả về, ví dụ `BED_TAKEN` hoặc `DUPLICATE_INVOICE`. Không nên parse chuỗi message.
5. **Figma**: vẽ wireframe low-fi cho toàn bộ, chỉ vẽ high-fi cho khoảng 6 màn demo. Không cần mục "06 - Prototype" đầy đủ.
6. **Gắn màn hình với người phụ trách**, khớp với cách chia module ở mục 13.1 của tài liệu đề xuất:

| TV | Màn hình |
|---|---|
| TV1 Séc | Login, đổi mật khẩu, Admin (tài khoản, import, audit log), layout và guard theo role |
| TV2 Phú Nguyên | Phòng & giường, phân giường (split view), trưởng phòng, chuyển phòng, phân lại giường sau hè |
| TV3 Đức Nguyên | Đăng ký (stepper, timeline), duyệt đơn, hợp đồng, kích hoạt, gia hạn, lựa chọn hè |
| TV4 Quyền | Ticket (SV, Staff, KTV), kết luận chi phí, trả phòng, phiếu gửi đồ |
| TV5 Lộc | Điện nước, hóa đơn, sổ cọc, dashboard, thông báo |

Ai đó (gợi ý TV1) nên làm **`components/` và `statusConfig` trong tuần đầu tiên code** để các thành viên khác dùng chung.

7. **Backbone screens đề xuất (thay cho mục 28):**
   1. Login + đổi mật khẩu
   2. Student Dashboard (đủ 6 trạng thái)
   3. Đăng ký + timeline đơn
   4. Staff duyệt đơn → phân giường
   5. **Staff kích hoạt hợp đồng + thu cọc**
   6. Phòng của tôi / Chi tiết phòng (Staff)
   7. Hóa đơn (phí phòng + điện nước có trưởng phòng)
   8. Báo hỏng → KTV → nghiệm thu → **kết luận chi phí → trừ cọc**
   9. Sổ cọc
   10. Staff Dashboard

   Mười màn này đi trọn trục demo trong 7 tuần đầu, khớp với mốc cut line ở tuần 7.

---

## 8. Đề xuất thứ tự thảo luận

1. Mục 6: lỗi nghiệp vụ bù cọc khi trả phòng. Cần chốt vì phải sửa SRS.
2. Mục 1.1 và 2.1: kích hoạt hợp đồng và màn phân giường.
3. Mục 3: hệ thống trạng thái và nhãn.
4. Mục 1.2 đến 1.5: bổ sung các màn còn thiếu.
5. Mục 7: chọn thư viện UI và chia màn hình theo thành viên.

---

## 9. Kết quả xử lý (08/10/2026)

Tất cả các mục trên đã được Enrique đồng ý. Kết quả:

- **Mục 6**: SRS lên v0.4. Bù cọc chưa nộp được tất toán bằng số dư khi trả phòng. SRS mục 6.2 thêm quy ước `errorCode` và `allowedActions`.
- **Mục 1 đến 5 và mục 7**: đã sửa trực tiếp vào `Plan/DMS-UI-UX-Design.md` và đồng bộ về máy.

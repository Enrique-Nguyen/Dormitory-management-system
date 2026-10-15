# Nhận xét thiết kế DMS (Nhóm 3, 65CNTT)

Phạm vi duyệt: `Plan/he-thong-quan-ly-ky-tuc-xa-mini.md` (gọi tắt **MD**) và `Plan/DMS-thuyet-trinh.html` (gọi tắt **Slide**, 14 trang). Chưa sửa file nào.

Mức ưu tiên: 🔴 nên sửa trước khi nộp/thuyết trình · 🟡 nên làm rõ · ⚪ tùy chọn.

---

## 1. Điểm mạnh

- **Trọng tâm đúng chỗ.** Đề tài không dừng ở CRUD mà nhấn vào quy trình có trạng thái, quy tắc nghiệp vụ và lịch sử. Đây là điểm ăn điểm nhất khi bảo vệ.
- **Mô hình sức chứa theo giường + ràng buộc ở CSDL** là quyết định thiết kế tốt, có lý do rõ (chống vượt chỗ ngay cả khi tầng ứng dụng lỗi) và có test/demo chứng minh (tranh giường A101).
- **Lịch sử phân phòng dạng khoảng thời gian** (`RoomAssignmentHistory`), không ghi đè `room_id`. Đúng tư duy dữ liệu có thời gian.
- **Không lưu dữ liệu suy ra** (số chỗ trống, "Đầy", "Expiring"). Nhất quán và giảm lỗi lệch dữ liệu.
- **RBAC + phạm vi tòa + kiểm tra trên từng đối tượng**, Quyết định cũ Admin không có quyền nghiệp vụ đã được thay thế ngày 14/10/2026: Admin có mọi quyền nghiệp vụ ở mọi đơn vị, vẫn giữ trạng thái và ràng buộc dữ liệu. Phản ánh đúng bài toán hai đơn vị.
- **Quản trị phạm vi tốt**: MoSCoW theo độ sâu, có "đường cắt" tuần 7 với thứ tự cắt cụ thể. Ít nhóm sinh viên làm được điều này.
- **Trung thực về hiện trạng**: ghi rõ là quan sát của nhóm, có kế hoạch khảo sát, ví dụ đơn giá ghi rõ là giả định.
- Test case bắt buộc bám sát quy tắc nghiệp vụ, kịch bản demo có kết quả mong đợi đo được.

---

## 2. Mâu thuẫn giữa MD và Slide ✅ (đã sửa ngày 07/10/2026; vấn đề 3.1–3.4 và phân công ở mục 5 cũng đã cập nhật)

| # | Chỗ | MD nói | Slide nói | Đề xuất |
|---|---|---|---|---|
| 1 | Slide 02, dòng cuối | Expiring **không phải** trạng thái, chỉ là nhãn | `Active → Expiring → Expired` vẽ như chuỗi trạng thái | Đổi thành `Pending → Active → Expired / Terminated`, thêm chú thích "Sắp hết hạn = nhãn cảnh báo". Thầy dễ bắt lỗi chỗ này vì nó nằm ngay slide giải pháp |
| 2 | Slide 05, phân hệ 02 | Trạng thái vận hành phòng: Hoạt động / Bảo trì / Ngừng; "Trống/Đầy" là suy ra | "Trống / Có người / Bảo trì" | Sửa slide thành "Hoạt động / Bảo trì / Ngừng · chỗ trống tính từ giường" |
| 3 | Slide 07, ghi chú | Người yêu cầu đã rời KTX → **Staff nghiệm thu thay**; RESOLVED quá 3 ngày **tự đóng** | "cần quy định người thay thế… **không mặc định tự đóng**" | Thống nhất theo MD (đã chốt ở 19.1). Slide 07 cũng nên ghi tự đóng 3 ngày và nhánh REJECTED/CANCELLED |
| 4 | Slide 08 | 5 nhóm quy tắc (BR-01…BR-05) | "Bốn nhóm quy tắc" | Hoặc thêm BR-05 vào slide, hoặc ghi "bốn nhóm tiêu biểu" |
| 5 | Slide 09, ghi chú | Đã có `ContractExtensionRequest` | "lịch sử gia hạn bổ sung trong thiết kế CSDL" | Bỏ câu đó; dòng "Lược giản" nên kể thêm RoomType, TransferRequest, ExtensionRequest, Notification |
| 6 | Slide 13, Q&A | Chống vượt chỗ = **ràng buộc CSDL** + khóa + test | Chỉ "khóa và kiểm tra trong transaction" | Thêm "và CSDL chặn trùng giường", vì đó là ý mạnh nhất |
| 7 | Slide 11, ghi chú | 10 test bắt buộc | Thiếu "ticket tự đóng 3 ngày" và "chỉ số cuối < đầu" | Bổ sung cho đủ |

Tổng thời lượng gợi ý trên slide là khoảng 10 phút 20 giây, vừa khung 12 phút. Ổn.

---

## 3. Lỗ hổng / mâu thuẫn trong thiết kế nghiệp vụ

### 3.1. 🔴 `DormitoryContract.room_id` mâu thuẫn với nguyên tắc "không ghi đè"
Mục 6.3 nói không ghi `student.room_id = newRoom`, nhưng hợp đồng lại có `room_id` và bước "Cập nhật liên kết hợp đồng". Như vậy chuyển phòng vẫn ghi đè `contract.room_id`, và dữ liệu phòng bị lưu hai nơi (hợp đồng và phân phòng).
- **Đề xuất:** bỏ `room_id` khỏi hợp đồng; phòng/giường hiện tại suy ra từ phân phòng hiệu lực. Hợp đồng chỉ đại diện cho "một lần lưu trú" (thời hạn, tiền cọc, trạng thái).
- Kéo theo: **giá**. Hợp đồng có `giá`, `Room` có `giá`, `RoomType` cũng có `giá` (3 chỗ). Chuyển sang phòng khác loại thì tính tiền theo giá nào? Đề xuất: giá gốc ở `RoomType`, giá áp dụng được **chụp lại trên dòng phân phòng** lúc phân/chuyển, hóa đơn lấy từ đó.

### 3.2. 🔴 Vòng đời phân phòng (giữ chỗ → đang ở) chưa được định nghĩa
`RoomAssignmentHistory.loại` có "giữ chỗ / đang ở", nhưng khi kích hoạt hợp đồng thì sửa dòng đó hay đóng dòng giữ chỗ và mở dòng mới? Cách nào cũng được, nhưng phải chọn, vì nó quyết định ràng buộc CSDL.
- **Đề xuất cụ thể cho ràng buộc** (PostgreSQL, dễ trình bày hơn exclusion):
  ```sql
  CREATE UNIQUE INDEX one_active_per_bed     ON room_assignment(bed_id)     WHERE ended_at IS NULL;
  CREATE UNIQUE INDEX one_active_per_student ON room_assignment(student_id) WHERE ended_at IS NULL;
  ```
  Kích hoạt hợp đồng = cập nhật `type` từ HOLD sang STAY trên cùng dòng (không đụng ràng buộc). Exclusion constraint chỉ cần nếu cho phép phân phòng đặt trước cho tương lai.
- Index thứ hai cũng chính là cơ chế cho yêu cầu "gửi lại cùng request không tạo phân phòng thứ hai" (idempotency). Nên nói rõ điều này thay vì để mơ hồ.

### 3.3. 🔴 Sơ đồ trạng thái đơn đăng ký thiếu nhánh sau ASSIGNED
Mục 6.2 nói hợp đồng PENDING quá 7 ngày bị hủy và "đưa đơn về trạng thái phù hợp", nhưng sơ đồ 6.1 không có cạnh nào ra khỏi ASSIGNED. Cũng không có ASSIGNED → CANCELLED khi SV tự hủy giữ chỗ.
- **Đề xuất:** thêm `ASSIGNED → CANCELLED` (SV hủy hoặc quá hạn kích hoạt). Nếu muốn cho BQL phân lại, thêm `ASSIGNED → APPROVED`. Chọn một và ghi vào state diagram.

### 3.4. 🔴 Phạm vi tòa của **đơn đăng ký** chưa xác định
Ma trận quyền nói Staff xem "Đơn: tòa phụ trách", nhưng lúc nộp đơn chưa có phòng nên đơn chưa thuộc tòa nào. `RoomApplication` chỉ có "loại phòng mong muốn".
- **Đề xuất:** thêm `preferred_building_id` (nguyện vọng tòa) vào đơn và dùng nó làm phạm vi. Hoặc cho mỗi đợt đăng ký thuộc về một đơn vị. Liên quan: "chỉ tiêu theo tòa" ở `RegistrationPeriod` hiện chưa có quy tắc nào kiểm tra; nên thêm vào BR-01 hoặc ghi là chỉ dùng để thống kê.
- Tương tự, **UC-14 Manage Students / import Excel / reset mật khẩu** của Staff: Staff K1 có reset được mật khẩu sinh viên đang ở N2 không? Sinh viên chưa có phòng thì thuộc phạm vi ai?

### 3.5. 🟡 "Kỳ" hóa đơn chưa rõ
Tiền phòng thường thu theo học kỳ, điện nước thường theo tháng. Ràng buộc `Invoice (student_id, period)` ngầm giả định chung một kỳ.
- **Đề xuất:** định nghĩa `BillingPeriod` (tháng) rõ ràng; tiền phòng hoặc thu một lần khi kích hoạt hợp đồng, hoặc chia theo tháng. Ghi vào 19.2 để hỏi BQL.
- Chia đều điện nước cho "sinh viên đang ở phòng trong kỳ": SV chuyển phòng giữa kỳ bị tính ở cả hai phòng hay phòng nào? Đề xuất quy tắc đơn giản: tính ở phòng có phân phòng hiệu lực tại ngày chốt chỉ số.
- "Quá hạn" cần `due_date` trên `Invoice`. Nên coi "Quá hạn" là nhãn suy ra (giống Expiring) cho nhất quán với nguyên tắc của chính đồ án.
- "Nghiệp vụ điều chỉnh" hóa đơn đã thanh toán được nhắc ở BR-03 nhưng không có thực thể, UC hay API. Hoặc bổ sung (ví dụ `InvoiceItem` loại ADJUSTMENT ở kỳ sau), hoặc ghi là ngoài MVP.

### 3.6. 🟡 Tiền cọc chỉ có chiều hoàn, không có chiều thu
`DormitoryContract` có `tiền cọc`, trả phòng có "ghi nhận hoàn cọc", nhưng không có bước xác nhận đã nhận cọc. Đề xuất: Staff xác nhận đã thu cọc ngay khi bấm "Kích hoạt" (cùng màn hình), lưu người và thời gian.

### 3.7. 🟡 Hợp đồng hết hạn tự động hay thủ công?
Không rõ ACTIVE → EXPIRED do hệ thống tự chuyển khi qua ngày kết thúc hay chỉ khi Staff làm trả phòng. Nếu tự chuyển mà SV chưa trả phòng thì giường vẫn bị chiếm bởi một hợp đồng đã hết hạn. Đề xuất: EXPIRED chỉ đặt khi trả phòng; quá hạn mà chưa trả phòng thì hiện nhãn "Quá hạn trả phòng" trên Dashboard.

### 3.8. 🟡 Gia hạn và đăng ký đợt mới chồng nhau
SV đang ở muốn ở tiếp học kỳ sau thì gửi gia hạn hay nộp đơn đợt mới? Hai luồng này cần ranh giới. Đề xuất: SV đang ở chỉ dùng gia hạn; SV mới hoặc đã trả phòng dùng đơn đăng ký. Nên chặn nộp đơn khi đang có hợp đồng hiệu lực.

### 3.9. 🟡 Bàn giao giữa phòng bảo trì, giường hỏng và ticket
`Bed.status = Hỏng` và `Room.status = Bảo trì` do ai cập nhật? Ticket sửa chữa có làm đổi trạng thái giường không? Phòng chuyển sang Bảo trì khi đang có SV ở thì sao? Đề xuất tối thiểu: chỉ Staff đổi trạng thái thủ công; không cho đặt giường Hỏng khi giường đang có phân phòng hiệu lực (hoặc buộc chuyển SV trước).

### 3.10. 🟡 Thiếu tác vụ định kỳ trong kiến trúc
Ít nhất 3 việc chạy theo thời gian: hủy hợp đồng PENDING quá N ngày, tự đóng ticket RESOLVED quá 3 ngày, (tùy chọn) đánh dấu quá hạn. Kiến trúc chưa nhắc. Đề xuất ghi một dòng: dùng `@Scheduled` của Spring, chạy định kỳ, mỗi job xử lý trong transaction và ghi `AuditLog` với người thao tác là "SYSTEM".

### 3.11. ⚪ Giới tính
BR-01 nói "phù hợp giới tính (nếu áp dụng)" và `Room` có giới tính, nhưng `User` không có trường giới tính. Thêm vào `User` hoặc hồ sơ sinh viên.

### 3.12. ⚪ Minh chứng diện ưu tiên
Điểm ưu tiên dựa trên diện chính sách, ở xa… thường cần minh chứng (giấy tờ). Thiết kế không có upload file. Đề xuất: MVP để BQL tick xác nhận diện ưu tiên khi duyệt (không upload), ghi rõ là hạn chế. Đây cũng là dữ liệu cá nhân nhạy cảm, nên nhắc trong phần bảo mật.

---

## 4. API sơ bộ

- 🟡 **Thiếu endpoint** cho nhiều UC đã liệt kê: từ chối chuyển phòng/gia hạn, hủy đơn, xem hợp đồng (`GET /contracts/my`, `/contracts`), xem hóa đơn (`GET /invoices/my`, `/invoices`), thông báo (UC-12, UC-23), dashboard (UC-24), audit log (UC-30), quản lý tòa / loại phòng / giường, SV nghiệm thu hoặc không chấp nhận ticket. Nếu chỉ là "sơ bộ" thì nên ghi rõ "liệt kê tiêu biểu", nếu không thầy sẽ đối chiếu với 33 UC.
- 🟡 **`DELETE /api/rooms/{id}`** mâu thuẫn với lịch sử lưu trú (phòng đã từng có người thì không xóa được). Đề xuất bỏ, dùng chuyển trạng thái "Ngừng sử dụng".
- ⚪ Không nhất quán động từ: `approve` dùng PUT nhưng `activate`, `move-out` dùng POST. Chọn một quy ước cho các hành động chuyển trạng thái (thường là `POST /{id}/approve`).
- ⚪ Chưa nêu cơ chế xác thực (JWT hay session). Có `logout` nên cần nói rõ, vì JWT thuần thì logout phía server cần thêm cơ chế.

---

## 5. Kế hoạch và phân công

- 🔴 **Thành viên 3 quá tải.** Người này giữ đơn, xét duyệt, phân phòng, hợp đồng, chuyển phòng, gia hạn, tức là toàn bộ phần khó nhất (transaction, khóa, ràng buộc, nhiều state machine). Thành viên 4 (ticket + thông báo) và 2 (phòng/giường) nhẹ hơn nhiều. Đề xuất: chuyển **chuyển phòng + trả phòng** sang Thành viên 2 (vốn sở hữu giường), giữ cho Thành viên 3 đơn → phân phòng → hợp đồng/gia hạn.
- 🟡 Bảng phân công dùng "Thành viên 1…5" trong khi đã có tên. Nên ghi tên thật, giảng viên thường hỏi ai làm phần nào.
- 🟡 Code thực tế chỉ có tuần 5–9 (5 tuần) cho 7 phân hệ, 33 UC. Rủi ro lớn nhất là tích hợp dồn tuần 8–9. Đề xuất: tuần 4 dựng sẵn khung dự án (auth, cấu trúc module, CI chạy test, seed dữ liệu) để tuần 5 mọi người code nghiệp vụ được ngay.
- 🟡 Mục 19.1 "Quyết định đã chốt" trước khi khảo sát BQL (tuần 1–2). Nên thêm một câu: nếu khảo sát khác, nhóm điều chỉnh trong SRS. Tránh câu hỏi "chốt rồi thì khảo sát để làm gì".
- ⚪ Thiếu bảng rủi ro ngắn (không gặp được BQL, thành viên chậm tiến độ, test đồng thời khó dựng). Một bảng 3–4 dòng là đủ.

---

## 6. Yêu cầu phi chức năng

- 🟡 "95% request dưới 2 giây với tải dự kiến" chưa đo được vì chưa có con số tải. Đề xuất ghi giả định, ví dụ "khoảng 3.000 SV, 100 người dùng đồng thời trong đợt đăng ký", và cách đo (JMeter/k6 trên kịch bản nộp đơn + phân phòng).
- ⚪ Chưa nói triển khai ở đâu (Docker Compose trên một máy? VPS?), trong khi sản phẩm bàn giao có "Deployment Diagram" và "gói triển khai". Một dòng là đủ.
- ⚪ Sao lưu dữ liệu: một dòng "backup CSDL hằng ngày" là đủ cho MVP.

---

## 7. Gợi ý thứ tự sửa

1. Sửa 7 mâu thuẫn MD ↔ Slide (mục 2), nhanh và giảm rủi ro bị bắt lỗi khi thuyết trình.
2. Chốt mô hình hợp đồng / phân phòng / giá (3.1, 3.2, 3.3), vì nó ảnh hưởng ERD, API và test.
3. Bổ sung phạm vi đơn đăng ký (3.4) và định nghĩa kỳ hóa đơn (3.5).
4. Cân lại phân công (mục 5).
5. Các điểm 🟡 / ⚪ còn lại tùy thời gian.

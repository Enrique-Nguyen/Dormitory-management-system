# Đặc tả chi tiết trường nghiệp vụ đầy đủ cho các màn DMS

| Mục | Nội dung |
|---|---|
| Ngày | 19/11/2026 |
| Trạng thái | **ĐỀ XUẤT – chờ nhóm chốt.** Sau khi chốt: cập nhật SRS (FR liên quan) → `DB-Design-DMS.md` + migration mới → `API-Spec-DMS.md` → `DMS-UI-UX-Design.md` → code, theo thứ tự ưu tiên tài liệu trong `CLAUDE.md`. Không sửa migration V1–V15 đã chạy. |
| Cơ sở hiện trạng | Đọc trực tiếp `information_schema` và ràng buộc của CSDL đang chạy (Flyway V15) ngày 19/11/2026; registry `DMS/frontend/src/features/crud/resources.ts`; `DmsConstants`. |
| Quyết định đã có | **Giữ Nhập Excel ở mọi màn.** Mẫu Excel sinh từ registry, nên mỗi trường trong tài liệu này đều có cột Excel tương ứng (trừ trường hệ thống tự ghi). |
| Liên quan | [danh-gia-tong-the-va-de-xuat-cai-thien-DMS.md](danh-gia-tong-the-va-de-xuat-cai-thien-DMS.md), [bao-cao-review-chuyen-sau-module-dang-ky-ktx.md](bao-cao-review-chuyen-sau-module-dang-ky-ktx.md) |

## Mục lục

- [0. Quy ước chung](#0-quy-ước-chung)
- [Phần I – Cơ sở vật chất](#phần-i--cơ-sở-vật-chất): 1 Đơn vị · 2 Tòa nhà · 3 Tầng · 4 Loại phòng · 5 Phòng · 6 Giường · 7 Tài sản và biên bản
- [Phần II – Danh mục dùng chung](#phần-ii--danh-mục-dùng-chung): 8 Trường · 9 Khoa · 10 Ngành · 11 Khóa · 12 Dân tộc · 13 Quê quán · 14 Năm học · 15 Học kỳ · 16 Tiện nghi · 17 Hạng mục sửa chữa · 18 Diện ưu tiên · 19 Vị trí kho
- [Phần III – Con người](#phần-iii--con-người): 20 Sinh viên · 21 Cán bộ/KTV/Admin
- [Phần IV – Đăng ký và lưu trú](#phần-iv--đăng-ký-và-lưu-trú): 22 Đợt đăng ký · 23 Đơn đăng ký · 24 Hợp đồng · 25 Gia hạn · 26 Chuyển phòng · 27 Lựa chọn hè · 28 Phiếu gửi đồ
- [Phần V – Tài chính, vận hành, thông báo](#phần-v--tài-chính-vận-hành-thông-báo): 29 Chỉ số điện nước · 30 Đơn giá · 31 Hóa đơn · 32 Giao dịch cọc · 33 Sửa chữa · 34 Thông báo
- [Phần VI – Nhập/Xuất Excel ở mọi màn](#phần-vi--nhậpxuất-excel-ở-mọi-màn)
- [Phần VII – Mã lỗi, hằng số, migration, kiểm thử](#phần-vii--mã-lỗi-hằng-số-migration-kiểm-thử)

---

## 0. Quy ước chung

### 0.1. Cách đọc bảng trường

Mỗi tài nguyên có một bảng với các cột:

| Cột | Ý nghĩa |
|---|---|
| **#** | Thứ tự hiển thị trong form và thứ tự cột trong mẫu Excel |
| **Trường** | Nhãn tiếng Việt trên giao diện và tiêu đề cột Excel. 🆕 = trường mới đề xuất; không có ký hiệu = đã có trong CSDL |
| **DB / JSON** | Tên cột PostgreSQL (snake_case) / khóa JSON trong API (camelCase). Ví dụ `citizen_id` / `citizenId` |
| **Kiểu** | Kiểu PostgreSQL. `V(n)` = `VARCHAR(n)`; tiền = `BIGINT` đồng (giữ như các cột tiền hiện có: `deposit_amount`, `total_amount`, `repair_cost`) |
| **BB** | **B** bắt buộc khi tạo · **T** tùy chọn · **H** hệ thống tự ghi (không có trong form, không có trong mẫu nhập, có trong file xuất) · **B\*** bắt buộc khi thực hiện một hành động (ghi rõ) |
| **Kiểm tra** | Ràng buộc phía Service + CSDL, kèm `errorCode` khi vi phạm. Không ghi mã lỗi thì dùng `VALIDATION_FAILED` với `details.field` |
| **Sửa** | Ai được sửa sau khi tạo: **A** Admin · **S** Staff đúng đơn vị · **SV** chính sinh viên · **K** KTV được giao · **—** bất biến sau khi tạo · **H** chỉ hệ thống |

### 0.2. Kiểu và định dạng

| Loại | Quy ước |
|---|---|
| Mã | `V(30)`, chữ hoa, số, `-`, `_`; `CHECK (code ~ '^[A-Z0-9_-]{1,30}$')`; unique trong phạm vi ghi rõ. Không đổi mã đã được tham chiếu bởi dữ liệu nghiệp vụ (`RESOURCE_IN_USE`). Mã hiện có ngắn hơn (tòa `V(10)`, đơn vị `V(20)`) giữ nguyên độ dài, không mở rộng ngầm. |
| Tên | `V(200)`, `btrim(name) <> ''`. |
| Ghi chú | `note V(1000)` tùy chọn, có ở mọi danh mục và hồ sơ. |
| Mô tả dài | `TEXT` có giới hạn ở Service (≤ 5.000 ký tự). |
| SĐT | `V(15)`, chuẩn hóa bỏ khoảng trắng, regex `^(\+84|0)[0-9]{9,10}$`. |
| Email | `V(200)`, kiểm định dạng; email trường kiểm hậu tố theo `system_config` (`student.email_domain`). |
| Ngày / thời điểm | `DATE` cho ngày nghiệp vụ; `TIMESTAMPTZ` cho thời điểm hệ thống; hiển thị `dd/MM/yyyy` (đã có `utils/format.ts`). |
| Enum | Mã tiếng Anh viết hoa như các enum hiện có (`ACTIVE`, `MAINTENANCE`, `PENDING`…); giới tính giữ `CHAR(1)` `'M'/'F'` như `room.gender`, `student_profile.gender`. Nhãn tiếng Việt khai một lần ở `constants/statusConfig.ts` (frontend) và nhóm hằng số tương ứng trong `DmsConstants` (backend). |
| Trạng thái hoạt động | Danh mục có `active BOOLEAN NOT NULL DEFAULT TRUE`; đã được tham chiếu thì ngừng dùng thay cho xóa (`RESOURCE_IN_USE` khi xóa). |
| Tệp | Ảnh JPEG/PNG/WebP ≤ 5 MB, kiểm magic bytes (dùng lại cơ chế ảnh ticket); PDF ≤ 5 MB kiểm `%PDF-`. Lưu ở volume `uploads`, tên file do server sinh, chỉ phục vụ qua endpoint kiểm quyền. File được dọn khi transaction rollback. |
| Dấu vết | Bảng mới có `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`, `created_by BIGINT`, `updated_at`, `updated_by`. Lịch sử chi tiết vẫn ở `audit_log` / `status_history`. |

### 0.3. Quy tắc dùng chung khi thêm trường

1. **Sửa từng phần:** trường không gửi hoặc gửi `null` giữ giá trị cũ (đúng quy tắc `save-or-update` hiện tại). Chuỗi rỗng `""` chỉ xóa được trường văn bản **tùy chọn**; với trường bắt buộc trả `VALIDATION_FAILED`.
2. **Không lưu giá trị suy diễn:** số chỗ trống, “đầy”, “sắp hết hạn”, “quá hạn”, số dư cọc, điểm ưu tiên… vẫn tính từ view/Service. Các trường **H** ghi rõ là suy diễn hay được chụp (snapshot) tại thời điểm hành động.
3. **Khóa ngoại nhập bằng mã ở Excel**, bằng ID ở API JSON; Service tra mã → ID trong phạm vi quyền (ngoài phạm vi trả lỗi dòng “không tìm thấy”, không lộ tồn tại).
4. **Dữ liệu cá nhân nhạy cảm** (đánh dấu 🔒): CCCD, ngày/nơi cấp, thông tin người giám hộ, sức khỏe, BHYT, diện chính sách, minh chứng. Quy tắc:
   - Chỉ chính sinh viên, Staff đúng đơn vị đang quản lý sinh viên (có đơn/hợp đồng thuộc đơn vị) và Admin được đọc.
   - Danh sách hiển thị che: CCCD `0012******56`, SĐT giám hộ `09******78`. Chi tiết hiện đầy đủ khi có quyền `STUDENT_SENSITIVE_READ` 🆕.
   - `audit_log.old_value/new_value` ghi `"***"` cho trường 🔒, chỉ giữ tên trường đã đổi.
   - Xuất Excel chỉ có cột 🔒 khi người xuất có `STUDENT_SENSITIVE_READ`; file xuất được audit (`EXCEL_EXPORT`).
   - Căn cứ: Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân (dữ liệu sức khỏe, dân tộc, tôn giáo là dữ liệu nhạy cảm).
5. **Quyền:** không thêm quyền runtime trong code; mọi mã quyền mới (🆕) khai ở `DmsConstants.Permission`, `PermissionCatalog`, `frontend/src/constants/authorization.ts` và seed vào `role_permission_matrix` bằng migration mới.

---

# Phần I – Cơ sở vật chất

## 1. Đơn vị quản lý (`management_unit`) – `/admin/units`

**Mục đích:** ban quản lý một cụm tòa (K1; N2+N3). Là gốc của phạm vi dữ liệu Staff/KTV.
**Hiện có:** `code V(20)`, `name V(100)`.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Mã đơn vị | `code` / `code` | V(20) | B | unique; regex mã | — (khi đã có tòa) |
| 2 | Tên đơn vị | `name` / `name` | V(100) | B | không rỗng | A |
| 3 | 🆕 Tên viết tắt | `short_name` / `shortName` | V(30) | T | | A |
| 4 | 🆕 Địa chỉ | `address` / `address` | V(300) | B | | A |
| 5 | 🆕 SĐT trực ban | `hotline` / `hotline` | V(15) | B | regex SĐT | A |
| 6 | 🆕 Email | `email` / `email` | V(200) | T | định dạng email | A |
| 7 | 🆕 Trưởng ban | `head_user_id` / `headUserId` | BIGINT FK `app_user` | T | phải là STAFF thuộc đơn vị này → `BED_UNIT_MISMATCH`-tương tự: dùng 🆕 `USER_UNIT_MISMATCH` | A |
| 8 | 🆕 Giờ làm việc | `working_hours` / `workingHours` | V(100) | T | văn bản tự do, ví dụ `7:30–11:30, 13:30–17:00 (T2–T7)` | A |
| 9 | 🆕 Hoạt động | `active` / `active` | BOOLEAN | H (mặc định TRUE) | không ngừng khi còn tòa ACTIVE (`RESOURCE_IN_USE`) | A |
| 10 | 🆕 Ghi chú | `note` / `note` | V(1000) | T | | A |
| — | Số tòa, số giường, đang ở | suy diễn | | H | từ `building`, `v_room_occupancy` | |

- **Danh sách:** Mã · Tên · Trưởng ban · Hotline · Số tòa · Sức chứa / Đang ở · Hoạt động. Lọc: hoạt động. Sắp xếp: mã, tên.
- **Form (2 cột):** Thông tin chung (1–3, 9) · Liên hệ (4–6, 8) · Phụ trách (7) · Ghi chú (10, cả hàng).
- **Excel – sheet `DonVi`:** `ID | MA_DON_VI* | TEN_DON_VI* | TEN_VIET_TAT | DIA_CHI* | HOTLINE* | EMAIL | TEN_DANG_NHAP_TRUONG_BAN | GIO_LAM_VIEC | HOAT_DONG(1/0) | GHI_CHU`.

## 2. Tòa nhà (`building`) – `/admin/buildings`

**Hiện có:** `management_unit_id`, `code V(10)`, `name V(100)`, `deposit_amount BIGINT > 0`.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Đơn vị quản lý | `management_unit_id` / `managementUnitId` | FK | B | — đổi đơn vị khi còn người ở (`ROOM_OCCUPIED`) | A |
| 2 | Mã tòa | `code` / `code` | V(10) | B | unique | — khi có phòng |
| 3 | Tên tòa | `name` / `name` | V(100) | B | | A |
| 4 | Mức cọc (đ) | `deposit_amount` / `depositAmount` | BIGINT | B | > 0; đổi mức chỉ áp cho hợp đồng tạo sau (đã chụp `deposit_required`) | A |
| 5 | 🆕 Số tầng | `floor_count` / `floorCount` | SMALLINT | B | 1–30; không nhỏ hơn tầng lớn nhất đang có phòng (🆕 `FLOOR_IN_USE`) | A |
| 6 | 🆕 Có tầng trệt (tầng 0) | `has_ground_floor` / `hasGroundFloor` | BOOLEAN | T (FALSE) | | A |
| 7 | 🆕 Đối tượng giới tính | `gender_policy` / `genderPolicy` | V(10) | B | `M`, `F`, `MIXED`; khác `MIXED` thì mọi phòng phải cùng giới (`GENDER_MISMATCH`) | A |
| 8 | 🆕 Vị trí / địa chỉ | `address` / `address` | V(300) | T | ví dụ “Khu A, cổng số 2” | A |
| 9 | 🆕 Năm đưa vào sử dụng | `commissioned_year` / `commissionedYear` | SMALLINT | T | 1950–năm hiện tại | A |
| 10 | 🆕 Tổng diện tích sàn (m²) | `floor_area_m2` / `floorAreaM2` | NUMERIC(10,2) | T | > 0 | A |
| 11 | 🆕 Cán bộ quản lý tòa | `manager_user_id` / `managerUserId` | FK `app_user` | T | STAFF cùng đơn vị (`USER_UNIT_MISMATCH`) | A, S |
| 12 | 🆕 SĐT trực tòa | `hotline` / `hotline` | V(15) | T | regex SĐT | A, S |
| 13 | 🆕 Giờ mở cửa | `open_time` / `openTime` | TIME | T | | A, S |
| 14 | 🆕 Giờ đóng cửa | `close_time` / `closeTime` | TIME | T | hiển thị nội quy cho SV | A, S |
| 15 | 🆕 Tiện ích chung | bảng `building_amenity(building_id, amenity_id)` / `amenityIds[]` | N–N | T | tiện nghi `applies_to` có `BUILDING`; phần tử null → `VALIDATION_FAILED` | A, S |
| 16 | 🆕 Trạng thái | `status` / `status` | V(20) | H (ACTIVE) | `ACTIVE`, `RENOVATION`, `CLOSED`; đổi qua action `POST /api/buildings/{id}/status` có lý do; `CLOSED` khi còn người ở → `ROOM_OCCUPIED` | A |
| 17 | 🆕 Ảnh tòa | `photo_path` / `photoUrl` | file | T | 1 ảnh | A |
| 18 | 🆕 Ghi chú | `note` / `note` | V(1000) | T | | A, S |
| — | Số phòng, sức chứa, đang ở, đang giữ, trống | suy diễn | | H | `v_room_occupancy` | |

- **Danh sách:** Mã · Tên · Đơn vị · Giới tính · Số tầng · Phòng · Đang ở/Sức chứa (thanh %) · Mức cọc · Trạng thái. Lọc: đơn vị, giới tính, trạng thái.
- **Form:** Thông tin chung (1–3, 7, 16) · Quy mô (5, 6, 9, 10) · Tài chính (4) · Vận hành (11–14) · Tiện ích (15) · Ảnh & ghi chú (17, 18).
- **Excel – sheet `ToaNha`:** `ID | MA_DON_VI* | MA_TOA* | TEN_TOA* | MUC_COC* | SO_TANG* | CO_TANG_TRET | GIOI_TINH*(M/F/MIXED) | DIA_CHI | NAM_SU_DUNG | DIEN_TICH_SAN | TEN_DANG_NHAP_QUAN_LY | HOTLINE | GIO_MO | GIO_DONG | MA_TIEN_ICH(phân tách ;) | GHI_CHU`. Không có cột trạng thái.
- **Quyền mới:** `BUILDING_STATUS_UPDATE` 🆕.

## 3. Tầng (`building_floor`) – `/staff/floors`

**Hiện có:** `building_id`, `floor SMALLINT ≥ 0`, `name`, `active`; unique `(building_id, floor)`.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Tòa | `building_id` / `buildingId` | FK | B | trong phạm vi Staff | — |
| 2 | Số tầng | `floor` / `floor` | SMALLINT | B | 0..`floor_count` của tòa (🆕 `FLOOR_OUT_OF_RANGE`) | — khi có phòng |
| 3 | Tên tầng | `name` / `name` | V(100) | B | | S |
| 4 | 🆕 Giới tính tầng | `gender` / `gender` | CHAR(1) | T | chỉ khi tòa `MIXED`; phòng trên tầng phải cùng giới | S |
| 5 | 🆕 Khu vệ sinh chung | `shared_bathroom` / `sharedBathroom` | BOOLEAN | T | | S |
| 6 | 🆕 Phòng sinh hoạt chung | `common_room` / `commonRoom` | BOOLEAN | T | | S |
| 7 | Hoạt động | `active` / `active` | BOOLEAN | H | | S |
| 8 | 🆕 Ghi chú | `note` / `note` | V(1000) | T | | S |

- **Excel – sheet `Tang`:** `ID | MA_TOA* | SO_TANG* | TEN_TANG* | GIOI_TINH | VS_CHUNG(1/0) | PHONG_SINH_HOAT(1/0) | HOAT_DONG | GHI_CHU`.

## 4. Loại phòng (`room_type`) – `/staff/room-types`

**Hiện có:** `name` (unique), `bed_count 1–12`, `price_main ≥ 0`, `price_summer ≥ 0`, `area_m2`, `active`, tiện nghi qua `room_type_amenity`. **Không có mã** → không tham chiếu được ổn định từ Excel.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | 🆕 Mã loại phòng | `code` / `code` | V(30) | B | unique; migration sinh mã từ tên cho dữ liệu cũ (`RT-<id>`) | — khi đã dùng |
| 2 | Tên loại phòng | `name` / `name` | V | B | unique | S |
| 3 | Số giường | `bed_count` / `bedCount` | SMALLINT | B | 1–12; đổi khi đang dùng → 409 (đã có) | S |
| 4 | Giá học kỳ chính (đ/SV/kỳ) | `price_main` / `priceMain` | BIGINT | B | ≥ 0 | S (xem 4.1) |
| 5 | Giá học kỳ hè (đ/SV/kỳ) | `price_summer` / `priceSummer` | BIGINT | B | ≥ 0 | S (xem 4.1) |
| 6 | Diện tích (m²) | `area_m2` / `areaM2` | NUMERIC | T | > 0 | S |
| 7 | 🆕 Khép kín (WC riêng) | `private_bathroom` / `privateBathroom` | BOOLEAN | B | | S |
| 8 | 🆕 Có ban công | `has_balcony` / `hasBalcony` | BOOLEAN | T | | S |
| 9 | 🆕 Kiểu giường | `bed_style` / `bedStyle` | V(10) | B | `SINGLE`, `BUNK` | S |
| 10 | 🆕 Có bếp/khu nấu | `has_kitchen` / `hasKitchen` | BOOLEAN | T | | S |
| 11 | Tiện nghi | `room_type_amenity` / `amenityIds[]` | N–N | T | tiện nghi `applies_to` có `ROOM` | S |
| 12 | 🆕 Mô tả | `description` / `description` | V(1000) | T | hiển thị cho SV khi chọn loại phòng | S |
| 13 | 🆕 Ảnh minh họa | bảng `room_type_photo` / `photoUrls[]` | file × 3 | T | | S |
| 14 | Hoạt động | `active` / `active` | BOOLEAN | H | | S |

**4.1. Giá theo năm học (cần chốt trong SRS FR-ROOM-02).** Hiện giá nằm trực tiếp trên `room_type`; giá áp dụng được chụp vào `room_assignment_history` nên lịch sử không sai, nhưng không xem được bảng giá các năm. Đề xuất bảng `room_type_price(id, room_type_id, academic_year_id, price_main, price_summer, decision_no, created_at, created_by)` unique `(room_type_id, academic_year_id)`; `price_main/price_summer` trên `room_type` thành giá năm hiện tại (đọc qua view). Đổi giá = thêm dòng cho năm học, không sửa dòng đã có hợp đồng dùng (`RATE_ALREADY_EFFECTIVE`).

- **Danh sách:** Mã · Tên · Số giường · Kiểu giường · Khép kín · Diện tích · Giá HK chính · Giá hè · Số phòng đang dùng · Hoạt động.
- **Excel – sheet `LoaiPhong`:** `ID | MA_LOAI* | TEN_LOAI* | SO_GIUONG* | GIA_HK_CHINH* | GIA_HE* | DIEN_TICH | KHEP_KIN*(1/0) | BAN_CONG | KIEU_GIUONG*(SINGLE/BUNK) | BEP | MA_TIEN_ICH | MO_TA | HOAT_DONG`.

## 5. Phòng (`room`) – `/staff/rooms`

**Hiện có:** `building_id`, `room_type_id`, `floor`, `code` (unique theo tòa), `gender 'M'/'F'`, `status ACTIVE/MAINTENANCE/CLOSED`, `leader_student_id`.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Tòa | `building_id` / `buildingId` | FK | B | trong phạm vi | — (không chuyển tòa) |
| 2 | Tầng | `floor` / `floor` | SMALLINT | B | phải có trong `building_floor` của tòa (🆕 `FLOOR_OUT_OF_RANGE`) | S khi phòng trống |
| 3 | Mã phòng | `code` / `code` | V | B | unique theo tòa (`ROOM_CODE_EXISTS`) | S |
| 4 | 🆕 Tên hiển thị | `display_name` / `displayName` | V(50) | T | mặc định “Phòng {code}” | S |
| 5 | Loại phòng | `room_type_id` / `roomTypeId` | FK | B | đổi khi trống thì đồng bộ giường (đã có) | S |
| 6 | Giới tính | `gender` / `gender` | CHAR(1) | B | khớp `gender_policy` tòa/tầng (`GENDER_MISMATCH`); đổi khi có người ở → `ROOM_OCCUPIED` | S |
| 7 | 🆕 Diện tích thực tế (m²) | `area_m2` / `areaM2` | NUMERIC(6,2) | T | mặc định từ loại phòng | S |
| 8 | 🆕 Hướng | `orientation` / `orientation` | V(20) | T | `N`,`S`,`E`,`W`,`NE`,`NW`,`SE`,`SW` | S |
| 9 | 🆕 Số đồng hồ điện | `electric_meter_no` / `electricMeterNo` | V(30) | T | unique khi có | S |
| 10 | 🆕 Số đồng hồ nước | `water_meter_no` / `waterMeterNo` | V(30) | T | unique khi có | S |
| 11 | 🆕 Ngày đưa vào sử dụng | `commissioned_on` / `commissionedOn` | DATE | T | | S |
| 12 | Trạng thái | `status` / `status` | V(20) | H | đổi qua action có lý do (đã có) | S |
| 13 | 🆕 Lý do trạng thái | `status_reason` / `statusReason` | V(500) | H | ghi khi đổi trạng thái (hiện chỉ ở audit) | H |
| 14 | Trưởng phòng | `leader_student_id` / `leader` | FK | H | qua action chỉ định/gỡ (đã có) | S |
| 15 | 🆕 Ghi chú | `note` / `note` | V(1000) | T | | S |
| — | Sức chứa, đang ở, giữ chỗ, giường hỏng, trống | suy diễn | | H | `v_room_occupancy` | |

- **Danh sách (sửa lỗi hiện tại thiếu cột Tòa):** Tòa · Mã phòng · Tầng · Loại phòng · Giới tính · Đang ở/Sức chứa (`3/4` + thanh) · Giữ chỗ · Trưởng phòng · Trạng thái. Lọc: tòa, tầng, loại phòng, giới tính, trạng thái, “còn chỗ”, “thiếu trưởng phòng”. Sắp xếp mặc định: tòa, tầng, mã.
- **Form:** Vị trí (1, 2, 3, 4) · Loại & giới tính (5, 6) · Thông số (7, 8, 11) · Đồng hồ (9, 10) · Ghi chú (15).
- **Excel – sheet `Phong`:** `ID | MA_TOA* | TANG* | MA_PHONG* | TEN_HIEN_THI | MA_LOAI_PHONG* | GIOI_TINH*(M/F) | DIEN_TICH | HUONG | SO_DONG_HO_DIEN | SO_DONG_HO_NUOC | NGAY_SU_DUNG | GHI_CHU`. Tạo phòng qua Excel tự sinh giường theo loại (đúng FR-ROOM-04).

## 6. Giường (`bed`) – trong chi tiết phòng

**Hiện có:** `room_id`, `code V(10)` unique theo phòng, `status ACTIVE/BROKEN/RETIRED`.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Phòng | `room_id` / `roomId` | FK | B | | — |
| 2 | Mã giường | `code` / `code` | V(10) | B | unique theo phòng (`BED_CODE_EXISTS`) | S |
| 3 | 🆕 Vị trí | `position` / `position` | V(10) | T | `UPPER`, `LOWER`, `SINGLE`; loại phòng `BUNK` thì bắt buộc | S |
| 4 | 🆕 Mã tài sản giường | `asset_code` / `assetCode` | V(30) | T | liên kết `room_asset` (mục 7) | S |
| 5 | Trạng thái | `status` / `status` | V(20) | H | action đổi trạng thái có lý do; `BROKEN/RETIRED` khi có người ở → `BED_OCCUPIED` | S |
| 6 | 🆕 Ghi chú / lý do | `note` / `note` | V(500) | T | | S |
| — | Người đang ở, từ ngày, loại (giữ chỗ/ở) | suy diễn | | H | `room_assignment_history` mở | |

- **Excel – sheet `Giuong`:** `ID | MA_TOA* | MA_PHONG* | MA_GIUONG* | VI_TRI | MA_TAI_SAN | GHI_CHU`.

## 7. Tài sản phòng và biên bản nhận/trả phòng (🆕 toàn bộ)

**Lý do:** KTX thực tế (và StarRez *Inventory & Inspections*, eRezLife *Room Condition Report*) đều quản lý tài sản theo phòng và lập biên bản tình trạng khi nhận/trả phòng; đây là căn cứ khách quan để phân biệt **hao mòn tự nhiên** và **hư hỏng do sử dụng** (BR-10, FR-MNT-09/10). Hiện `move_out_record.asset_condition` chỉ là một chuỗi văn bản.

### 7.1. Loại tài sản (`asset_type`) – `/staff/asset-types`

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | Mã | `code` | V(30) | B | unique |
| 2 | Tên | `name` | V(200) | B | ví dụ Giường tầng, Tủ cá nhân, Bàn học, Ghế, Điều hòa, Bình nóng lạnh, Quạt trần, Đèn, Khóa cửa, Rèm |
| 3 | Đơn vị tính | `unit` | V(20) | B | Cái, Bộ, Chiếc |
| 4 | Đơn giá bồi thường tham khảo (đ) | `reference_price` | BIGINT | T | ≥ 0; chỉ gợi ý khi kết luận ticket, Staff vẫn nhập `repair_cost` |
| 5 | Thời gian khấu hao (tháng) | `depreciation_months` | SMALLINT | T | dùng để gợi ý “hao mòn tự nhiên” khi quá hạn khấu hao |
| 6 | Hạng mục sửa chữa mặc định | `maintenance_category_id` | FK | T | |
| 7 | Hoạt động / Ghi chú | `active`, `note` | | | |

### 7.2. Tài sản trong phòng (`room_asset`) – tab “Tài sản” trong chi tiết phòng

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | Phòng | `room_id` | FK | B | |
| 2 | Loại tài sản | `asset_type_id` | FK | B | |
| 3 | Mã tài sản / serial | `asset_code` | V(30) | T | unique toàn hệ thống khi có |
| 4 | Số lượng | `quantity` | SMALLINT | B | ≥ 1; có `asset_code` thì = 1 |
| 5 | Giường gắn kèm | `bed_id` | FK | T | tài sản cá nhân (tủ, bàn) gắn giường |
| 6 | Ngày lắp đặt | `installed_on` | DATE | T | |
| 7 | Tình trạng hiện tại | `condition` | V(20) | H | `GOOD`, `MINOR_DAMAGE`, `BROKEN`, `LOST`, `RETIRED`; cập nhật từ biên bản/ticket |
| 8 | Ghi chú | `note` | V(500) | T | |

### 7.3. Biên bản tình trạng (`room_condition_report`, `room_condition_item`)

| Bảng | Trường | Ghi chú |
|---|---|---|
| `room_condition_report` | `id`, `code` (H, `BB-<năm>-<số>`), `contract_id`, `bed_id`, `type` (`CHECK_IN`/`CHECK_OUT`), `inspected_at`, `inspected_by`, `student_confirmed_at` (H), `student_comment` (V(1000)), `note` | unique `(contract_id, bed_id, type)` |
| `room_condition_item` | `report_id`, `room_asset_id`, `condition`, `note`, ảnh (`room_condition_photo`, tối đa 3/dòng) | |

**Luồng:**
1. Kích hoạt hợp đồng (`/contracts/{id}/activate`) tạo biên bản `CHECK_IN` nháp với tài sản của phòng + giường, tình trạng mặc định = tình trạng hiện tại.
2. Sinh viên xác nhận trong 72 giờ (`system_config`: `condition_report.confirm_hours` 🆕): đồng ý hoặc ghi ý kiến + ảnh. Quá hạn coi như đồng ý (job hiện có thêm một bước).
3. Trả phòng (`/contracts/{id}/move-out`) tạo biên bản `CHECK_OUT`; Service so sánh với `CHECK_IN` để **gợi ý** khoản trừ cọc theo `reference_price`; Staff quyết định, giữ nguyên luồng trừ cọc hiện có.
4. `move_out_record.asset_condition` giữ để tương thích, ghi tóm tắt từ biên bản.

- **Quyền mới:** `ASSET_READ/CREATE/UPDATE/DELETE`, `CONDITION_REPORT_READ/CREATE/CONFIRM` 🆕.
- **Excel:** sheet `LoaiTaiSan` (`ID | MA* | TEN* | DON_VI_TINH* | DON_GIA_BOI_THUONG | KHAU_HAO_THANG | MA_HANG_MUC | HOAT_DONG | GHI_CHU`); sheet `TaiSanPhong` (`ID | MA_TOA* | MA_PHONG* | MA_LOAI_TAI_SAN* | MA_TAI_SAN | SO_LUONG* | MA_GIUONG | NGAY_LAP_DAT | GHI_CHU`). Biên bản **không** nhập Excel (cần chữ ký/xác nhận), chỉ xuất.

---

# Phần II – Danh mục dùng chung

Mọi danh mục trong phần này dùng chung khung `catalogResource` trong `resources.ts`; trường chung **Mã · Tên · Hoạt động · Ghi chú** không nhắc lại trong từng bảng. Sheet Excel luôn bắt đầu bằng `ID | MA* | TEN*` và kết thúc bằng `HOAT_DONG(1/0) | GHI_CHU`.

## 8. Trường (`school`) – `/staff/schools`

**Hiện có:** mã, tên, active.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | 🆕 Tên viết tắt | `short_name` / `shortName` | V(30) | T | ví dụ `TLU` |
| 2 | 🆕 Mã trường (Bộ GD&ĐT) | `moet_code` / `moetCode` | V(10) | T | ví dụ `TLA` |
| 3 | 🆕 Địa chỉ | `address` / `address` | V(300) | T | |
| 4 | 🆕 Là trường chủ quản | `is_home_school` / `homeSchool` | BOOLEAN | T | tối đa một dòng TRUE (partial unique index) |

Excel `Truong`: `… | TEN_VIET_TAT | MA_BO_GD | DIA_CHI | TRUONG_CHU_QUAN | …`.

## 9. Khoa (`faculty`) – `/staff/faculties`

**Hiện có:** trường, mã, tên, active.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | Trường | `school_id` / `schoolId` | FK | B | |
| 2 | 🆕 Tên viết tắt | `short_name` / `shortName` | V(30) | T | ví dụ `CNTT` |
| 3 | 🆕 SĐT văn phòng khoa | `phone` / `phone` | V(15) | T | |
| 4 | 🆕 Email | `email` / `email` | V(200) | T | |

Excel `Khoa`: `… | MA_TRUONG* | TEN_VIET_TAT | SDT | EMAIL | …`.

## 10. Ngành (🆕 `major`) – `/staff/majors`

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | Khoa | `faculty_id` / `facultyId` | FK | B | |
| 2 | Mã ngành | `code` / `code` | V(10) | B | 7 chữ số theo danh mục ngành của Bộ GD&ĐT (ví dụ `7480201`); unique theo khoa |
| 3 | Tên ngành | `name` / `name` | V(200) | B | |
| 4 | Trình độ | `level` / `level` | V(20) | B | `BACHELOR`, `ENGINEER`, `MASTER`, `DOCTOR` |

Ô chọn Ngành trong hồ sơ sinh viên phụ thuộc Khoa (`lookupDependsOn: facultyId`, giống Khoa phụ thuộc Trường hiện có). Excel `Nganh`: `ID | MA_KHOA* | MA_NGANH* | TEN_NGANH* | TRINH_DO* | HOAT_DONG | GHI_CHU`.

## 11. Khóa (`student_cohort`) – `/staff/cohorts`

**Hiện có:** mã, tên, active.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | 🆕 Năm nhập học | `enrollment_year` / `enrollmentYear` | SMALLINT | B | 2000 đến năm hiện tại + 1 |
| 2 | 🆕 Năm dự kiến tốt nghiệp | `expected_graduation_year` / `expectedGraduationYear` | SMALLINT | T | lớn hơn năm nhập học |

Dùng cho điều kiện đối tượng của đợt đăng ký (mục 22) và cảnh báo hợp đồng kéo dài quá năm tốt nghiệp. Migration để trống cho dữ liệu cũ; Staff bổ sung qua Excel (không tự suy từ mã khóa `K65` khi nhóm chưa chốt công thức).

## 12. Dân tộc (🆕 `ethnicity`) – `/staff/ethnicities`

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | Mã | `code` | V(10) | B | theo danh mục 54 dân tộc của Tổng cục Thống kê (`01` Kinh … `54` Ơ Đu) và `99` Người nước ngoài |
| 2 | Tên | `name` | V(100) | B | |
| 3 | Dân tộc thiểu số | `minority` / `minority` | BOOLEAN | B | `FALSE` cho Kinh; dùng gợi ý diện ưu tiên |
| 4 | Dân tộc rất ít người | `very_small_minority` / `verySmallMinority` | BOOLEAN | T | theo Nghị định 57/2017/NĐ-CP |

Seed 54 dòng bằng migration (dữ liệu seed bất biến khai trong migration, không đưa vào Constants).

## 13. Quê quán (`hometown`) – `/staff/hometowns`

**Hiện có:** mã, tên, `parent_id` (cấp cha), active.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | 🆕 Cấp hành chính | `admin_level` / `adminLevel` | V(10) | B | `PROVINCE`, `COMMUNE` (mô hình 2 cấp từ 01/07/2025); `COMMUNE` phải có cha `PROVINCE` |
| 2 | 🆕 Mã đơn vị hành chính | `gso_code` / `gsoCode` | V(10) | T | mã Tổng cục Thống kê |
| 3 | 🆕 Khu vực ưu tiên | `priority_area` / `priorityArea` | V(10) | T | `KV1`, `KV2NT`, `KV2`, `KV3` – gợi ý diện ưu tiên theo vùng |
| 4 | 🆕 Vùng đặc biệt khó khăn | `hardship_area` / `hardshipArea` | BOOLEAN | T | |

Excel `QueQuan`: `… | MA_CAP_CHA | CAP*(PROVINCE/COMMUNE) | MA_TCTK | KHU_VUC | VUNG_KHO_KHAN | …`.

## 14. Năm học (`academic_year`) – `/staff/academic-years`

**Hiện có:** `code` dạng `YYYY-YYYY`, `name`, `active`.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | 🆕 Ngày bắt đầu | `start_date` / `startDate` | DATE | B | năm của ngày = phần đầu của mã |
| 2 | 🆕 Ngày kết thúc | `end_date` / `endDate` | DATE | B | sau ngày bắt đầu; không chồng năm học khác (🆕 `ACADEMIC_YEAR_OVERLAP`) |
| — | Năm học hiện tại | suy diễn | | H | hôm nay nằm trong khoảng ngày |

Ràng buộc chéo: học kỳ (mục 15) phải nằm trong khoảng ngày của năm học cùng mã.

## 15. Học kỳ (`semester`) – `/staff/semesters`

**Hiện có:** `academic_year` (chuỗi), `type HK1/HK2/SUMMER`, `name`, `start_date`, `end_date`; unique `(academic_year, type)`.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | 🆕 Mã học kỳ | `code` / `code` | V(20) | H | sinh `HK1-2026`, `HK2-2026`, `HE-2026` từ loại + năm đầu; dùng làm khóa trong Excel |
| 2 | 🆕 Năm học (khóa ngoại) | `academic_year_id` / `academicYearId` | FK | H | migration điền theo chuỗi `academic_year`; giữ cột chuỗi cũ |
| 3 | 🆕 Hạn đóng phí phòng | `room_fee_due_date` / `roomFeeDueDate` | DATE | T | nếu có, hóa đơn phí phòng dùng ngày này thay cho “phát hành + N ngày” (cần chốt trong SRS FR-BIL-07) |
| 4 | 🆕 Ghi chú | `note` | V(1000) | T | |

## 16. Tiện nghi (`amenity`) – `/staff/amenities`

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | 🆕 Áp dụng cho | `applies_to` / `appliesTo` | V(10) | B | `ROOM`, `BUILDING`, `BOTH` (dữ liệu cũ mặc định `ROOM`) |
| 2 | 🆕 Biểu tượng | `icon` / `icon` | V(50) | T | tên icon trong registry frontend (không lưu SVG) |
| 3 | 🆕 Mô tả | `description` | V(500) | T | |

## 17. Hạng mục sửa chữa (`maintenance_category`) – `/staff/maintenance-categories`

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | 🆕 Mức độ mặc định | `default_severity` / `defaultSeverity` | V(10) | T | `LOW`, `MEDIUM`, `HIGH` – gợi ý khi SV tạo ticket |
| 2 | 🆕 Thời hạn xử lý (giờ) | `sla_hours` / `slaHours` | SMALLINT | T | nếu trống dùng SLA theo mức độ trong `system_config` |
| 3 | 🆕 Chuyên môn KTV | `skill` / `skill` | V(20) | T | `ELECTRIC`, `PLUMBING`, `CARPENTRY`, `HVAC`, `NETWORK`, `GENERAL` – gợi ý giao việc (mục 21) |
| 4 | 🆕 Hướng dẫn cho SV | `description` | V(500) | T | ví dụ “Ghi rõ vị trí ổ cắm” |

## 18. Diện ưu tiên (`priority_category`) – `/admin/priority-categories`

**Hiện có:** mã, tên, `weight ≥ 0`, active.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | 🆕 Mô tả điều kiện | `description` | V(1000) | B | hiển thị cho SV khi tự khai |
| 2 | 🆕 Giấy tờ minh chứng | `evidence_required` / `evidenceRequired` | V(500) | T | ví dụ “Giấy chứng nhận hộ nghèo do UBND xã cấp” |
| 3 | 🆕 Bắt buộc tải minh chứng | `evidence_mandatory` / `evidenceMandatory` | BOOLEAN | B | TRUE mà nộp đơn thiếu tệp → 🆕 `PRIORITY_EVIDENCE_REQUIRED` |
| 4 | 🆕 Căn cứ quy định | `legal_basis` / `legalBasis` | V(300) | T | |
| 5 | 🆕 Thứ tự hiển thị | `sort_order` / `sortOrder` | SMALLINT | T | |
| 6 | 🆕 Gợi ý từ hồ sơ | `auto_rule` / `autoRule` | V(30) | T | `ETHNIC_MINORITY`, `HARDSHIP_AREA`, `NONE` – chỉ **gợi ý** khai, Staff vẫn xác nhận (FR-REG-06) |

## 19. Vị trí kho (`storage_location`) – `/staff/storage-locations`

**Hiện có:** đơn vị, mã, tên, active.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra |
|---|---|---|---|---|---|
| 1 | 🆕 Tòa | `building_id` / `buildingId` | FK | T | thuộc cùng đơn vị |
| 2 | 🆕 Tầng / phòng kho | `place` / `place` | V(100) | T | |
| 3 | 🆕 Sức chứa (kiện) | `capacity` / `capacity` | INT | T | lớn hơn 0; vượt sức chứa chỉ cảnh báo, không chặn |
| 4 | 🆕 Cán bộ phụ trách | `keeper_user_id` / `keeperUserId` | FK | T | STAFF cùng đơn vị |
| — | Số kiện đang giữ | suy diễn | | H | tổng `package_count` của phiếu chưa trả |

---

# Phần III – Con người

## 20. Hồ sơ sinh viên (`app_user` + `student_profile`) – `/staff/students`, `/admin/import`, `/student/profile`

**Hiện có:** `app_user`: `username V(50)`, `full_name V(100)`, `phone V(20)`, `locked`, `must_change_password`; `student_profile`: `student_code` (unique), `gender 'M'/'F'`, `date_of_birth`, `emergency_contact` (một chuỗi), `school_id`, `faculty_id`, `cohort_id`, `hometown_id`.

Các trường mới thêm vào `student_profile` (một-một với tài khoản), trừ người giám hộ có thể có nhiều dòng nên tách bảng con.

### 20.1. Nhóm Định danh

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Mã sinh viên | `student_code` / `studentCode` | V | B | unique (`STUDENT_CODE_EXISTS`); giữ đệm 0 đầu (đã có test import) | — |
| 2 | Họ và tên | `app_user.full_name` / `fullName` | V(100) | B | | A |
| 3 | Giới tính | `gender` / `gender` | CHAR(1) | B | `M`/`F`; đổi khi đang ở → `GENDER_MISMATCH` | A |
| 4 | Ngày sinh | `date_of_birth` / `dateOfBirth` | DATE | B | tuổi 15–60 | A |
| 5 | 🆕 Nơi sinh | `birth_place_id` / `birthPlaceId` | FK `hometown` (PROVINCE) | T | | A, S |
| 6 | 🆕🔒 Số CCCD / hộ chiếu | `citizen_id` / `citizenId` | V(20) | B | CCCD: đúng 12 chữ số; người nước ngoài: hộ chiếu 6–20 ký tự chữ/số; unique (🆕 `CITIZEN_ID_EXISTS`) | A |
| 7 | 🆕🔒 Ngày cấp | `citizen_id_issued_on` / `citizenIdIssuedOn` | DATE | T | ≤ hôm nay, ≥ ngày sinh | A, S |
| 8 | 🆕🔒 Nơi cấp | `citizen_id_issued_by` / `citizenIdIssuedBy` | V(200) | T | | A, S |
| 9 | 🆕🔒 Dân tộc | `ethnicity_id` / `ethnicityId` | FK `ethnicity` | B | | A, S |
| 10 | 🆕🔒 Tôn giáo | `religion` / `religion` | V(100) | T | mặc định “Không” | A, S, SV |
| 11 | 🆕 Quốc tịch | `nationality` / `nationality` | V(100) | B | mặc định “Việt Nam”; khác Việt Nam thì `citizen_id` theo luật hộ chiếu | A |
| 12 | 🆕 Ảnh thẻ | `photo_path` / `photoUrl` | file | T | JPEG/PNG ≤ 2 MB, tỉ lệ 3:4 | A, S, SV |

### 20.2. Nhóm Liên lạc

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 13 | Số điện thoại | `app_user.phone` / `phone` | V(20) | B | regex SĐT | A, S, SV |
| 14 | 🆕 Email cá nhân | `personal_email` / `personalEmail` | V(200) | T | định dạng email | A, S, SV |
| 15 | 🆕 Email trường | `school_email` / `schoolEmail` | V(200) | T | hậu tố theo `system_config` `student.email_domain` 🆕 | A |
| 16 | Quê quán (tỉnh) | `hometown_id` / `hometownId` | FK | B | cấp PROVINCE | A, S |
| 17 | 🆕 Xã/phường thường trú | `permanent_commune_id` / `permanentCommuneId` | FK `hometown` (COMMUNE) | T | cha phải là tỉnh ở #16 | A, S, SV |
| 18 | 🆕 Địa chỉ thường trú (số nhà, thôn/xóm) | `permanent_address` / `permanentAddress` | V(300) | B | | A, S, SV |
| 19 | 🆕 Địa chỉ liên hệ hiện tại | `contact_address` / `contactAddress` | V(300) | T | | A, S, SV |

### 20.3. Nhóm Học tập

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 20 | Trường | `school_id` / `schoolId` | FK | B | | A |
| 21 | Khoa | `faculty_id` / `facultyId` | FK | B | thuộc trường #20 | A |
| 22 | 🆕 Ngành | `major_id` / `majorId` | FK `major` | T | thuộc khoa #21 | A |
| 23 | 🆕 Lớp hành chính | `class_name` / `className` | V(30) | B | ví dụ `65CNTT1` | A, S |
| 24 | Khóa | `cohort_id` / `cohortId` | FK | B | | A |
| 25 | 🆕 Hệ đào tạo | `training_type` / `trainingType` | V(20) | B | `FULL_TIME`, `TRANSFER` (liên thông), `SECOND_DEGREE`, `POSTGRADUATE`, `EXCHANGE` | A |
| 26 | 🆕 Năm nhập học | `enrollment_year` / `enrollmentYear` | SMALLINT | B | mặc định lấy từ khóa | A |
| 27 | 🆕 Tình trạng học tập | `study_status` / `studyStatus` | V(20) | H (mặc định `STUDYING`) | `STUDYING`, `DEFERRED`, `SUSPENDED`, `WITHDRAWN`, `GRADUATED`; đổi qua action `POST /api/students/{id}/study-status` có lý do + ngày hiệu lực; khác `STUDYING` thì chặn nộp đơn/gia hạn (🆕 `STUDENT_NOT_STUDYING`) và hiện cảnh báo trên hợp đồng đang ở | A, S |

### 20.4. Nhóm Gia đình – bảng con 🆕 `student_guardian`

Thay chuỗi `emergency_contact`. Mỗi sinh viên 1–3 dòng, đúng một dòng `is_emergency_contact = TRUE`.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 28 | 🔒 Họ tên | `full_name` | V(100) | B | | A, S, SV |
| 29 | 🔒 Quan hệ | `relation` | V(20) | B | `FATHER`, `MOTHER`, `GUARDIAN`, `SIBLING`, `OTHER` | A, S, SV |
| 30 | 🔒 Năm sinh | `birth_year` | SMALLINT | T | | A, S, SV |
| 31 | 🔒 Nghề nghiệp | `occupation` | V(100) | T | phục vụ xét hoàn cảnh | A, S, SV |
| 32 | 🔒 Số điện thoại | `phone` | V(15) | B | regex SĐT | A, S, SV |
| 33 | 🔒 Địa chỉ | `address` | V(300) | T | | A, S, SV |
| 34 | Liên hệ khẩn cấp | `is_emergency_contact` | BOOLEAN | B | đúng một dòng TRUE (partial unique index `(student_id) WHERE is_emergency_contact`) | A, S, SV |

**Chuyển dữ liệu cũ:** migration tạo một dòng `student_guardian(relation='OTHER', full_name='(chưa rõ)', phone=<số tách được từ emergency_contact>, is_emergency_contact=TRUE)` khi tách được số; luôn chép nguyên chuỗi cũ vào `note` của hồ sơ. Giữ cột `emergency_contact` thêm một phiên bản rồi mới bỏ bằng migration sau.

### 20.5. Nhóm Chính sách và sức khỏe

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 35 | 🆕🔒 Diện chính sách đã xác nhận | bảng `student_priority(student_id, category_id, verified, verified_by, verified_at, valid_until, evidence_path)` / `priorities[]` | N–N | T | đơn đăng ký chụp lại khi nộp; Staff xác nhận một lần dùng cho các đợt sau đến `valid_until` | S (xác nhận), SV (khai) |
| 36 | 🆕🔒 Số thẻ BHYT | `health_insurance_no` / `healthInsuranceNo` | V(15) | T | 10 hoặc 15 ký tự | A, S, SV |
| 37 | 🆕🔒 Nhóm máu | `blood_type` / `bloodType` | V(5) | T | `A`, `B`, `AB`, `O`, `UNKNOWN` | SV |
| 38 | 🆕🔒 Ghi chú sức khỏe / dị ứng | `health_note` / `healthNote` | V(500) | T | chỉ Staff đơn vị đang quản lý SV đọc | SV |

### 20.6. Nhóm Khác

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 39 | 🆕 Biển số xe | `vehicle_plate` / `vehiclePlate` | V(15) | T | regex biển số VN, chuẩn hóa chữ hoa | A, S, SV |
| 40 | 🆕 Ghi chú | `note` / `note` | V(1000) | T | SV không xem | A, S |
| 41 | Tài khoản bị khóa | `app_user.locked` | BOOLEAN | H | action khóa/mở (đã có) | A |
| — | Phòng/giường hiện tại, hợp đồng, công nợ | suy diễn | | H | `v_current_stay`, `DebtQuery` | |

### 20.7. Giao diện

- **Danh sách Staff:** Mã SV · Họ tên (avatar ảnh thẻ) · Giới tính · Lớp · Khoa · Khóa · SĐT · Phòng hiện tại · Tình trạng học tập · Khóa TK. Lọc: khoa, khóa, lớp, giới tính, tình trạng học tập, đang ở/không ở, dân tộc thiểu số, tỉnh. Tìm `q` theo mã SV, họ tên, SĐT, **CCCD (khớp chính xác, không LIKE)**.
- **Chi tiết (trang riêng `/staff/students/:id`):** đầu trang thẻ hồ sơ (ảnh, mã, tên, lớp, phòng hiện tại, nút gọi); tab *Hồ sơ* (các nhóm 20.1–20.6) · *Lưu trú* (lịch sử phân phòng) · *Đơn & hợp đồng* · *Tài chính* (hóa đơn, sổ cọc, công nợ) · *Sửa chữa* · *Nhật ký*.
- **Form:** 5 bước/tab theo nhóm; desktop hai cột, mobile một cột; Gia đình là bảng con thêm/xóa dòng (Form.List bọc control thật).
- **Cổng Sinh viên `/student/profile`:** chỉ các trường có “SV” trong cột Sửa được nhập; trường khác chỉ đọc. Có thanh “Hồ sơ đã đủ x/y trường bắt buộc”; nộp đơn đăng ký yêu cầu hồ sơ đủ trường bắt buộc (🆕 `PROFILE_INCOMPLETE`, `details.missing[]`).

### 20.8. Excel sinh viên (mở rộng import 2 bước hiện có)

Sheet `SinhVien`, một dòng một SV; người giám hộ thứ nhất nằm trên cùng dòng, người thứ 2–3 dùng sheet `NguoiGiamHo`.

`ID | MA_SV* | HO_TEN* | GIOI_TINH*(M/F) | NGAY_SINH*(dd/MM/yyyy) | MA_TINH_NOI_SINH | SO_CCCD* | NGAY_CAP | NOI_CAP | MA_DAN_TOC* | TON_GIAO | QUOC_TICH* | SDT* | EMAIL_CA_NHAN | EMAIL_TRUONG | MA_TINH* | MA_XA | DIA_CHI_THUONG_TRU* | DIA_CHI_LIEN_HE | MA_TRUONG* | MA_KHOA* | MA_NGANH | LOP* | MA_KHOA_HOC* | HE_DAO_TAO* | NAM_NHAP_HOC* | GH_HO_TEN* | GH_QUAN_HE* | GH_SDT* | GH_NGHE_NGHIEP | SO_BHYT | BIEN_SO_XE | GHI_CHU`

Sheet `NguoiGiamHo`: `MA_SV* | HO_TEN* | QUAN_HE* | NAM_SINH | NGHE_NGHIEP | SDT* | DIA_CHI | LIEN_HE_KHAN_CAP(1/0)`.

- Ô số (mã SV, CCCD, SĐT, BHYT) phải là **Text** để giữ số 0 đầu; validate từ chối số bị Excel làm tròn/số mũ (giữ các ca test `StudentImportIntegrationTest` hiện có, thêm ca cho CCCD).
- Có `ID` → cập nhật (ô trống giữ nguyên); không có `ID` → tạo tài khoản + danh tính Keycloak + mật khẩu tạm (luồng hiện có).
- Không có cột tình trạng học tập, diện chính sách đã xác nhận, nhóm máu, ghi chú sức khỏe (chỉ SV tự khai/Staff xác nhận).

## 21. Tài khoản cán bộ, KTV, Admin (`app_user`) – `/admin/users`

**Hiện có:** username, họ tên, SĐT, vai trò, đơn vị, quyền đọc liên đơn vị, khóa, `must_change_password`, `security_version`.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Tên đăng nhập | `username` | V(50) | B | unique (`USERNAME_EXISTS`) | — |
| 2 | Họ tên | `full_name` | V(100) | B | | A |
| 3 | 🆕 Mã cán bộ | `staff_code` / `staffCode` | V(20) | B (trừ STUDENT) | unique khi có | A |
| 4 | Vai trò | `role` | V(20) | B | đổi vai trò thu hồi phiên (đã có) | A |
| 5 | Đơn vị | `management_unit_id` | FK | B với STAFF/TECHNICIAN | `ck_user_unit` (đã có) | A |
| 6 | 🆕 Chức vụ | `position` / `position` | V(30) | T | `UNIT_HEAD`, `BUILDING_MANAGER`, `ACCOUNTANT`, `OFFICER`, `ELECTRICIAN`, `PLUMBER`, `TECHNICIAN`, `SYSTEM_ADMIN` | A |
| 7 | 🆕 Tòa phụ trách | bảng `staff_building(user_id, building_id)` / `buildingIds[]` | N–N | T | tòa thuộc đơn vị #5; chỉ để lọc mặc định và gợi ý giao việc, **không** thu hẹp quyền (phạm vi vẫn theo đơn vị) | A |
| 8 | 🆕 Chuyên môn (KTV) | bảng `technician_skill(user_id, skill)` / `skills[]` | N–N | T (TECHNICIAN) | giá trị `skill` như mục 17 | A |
| 9 | Số điện thoại | `phone` | V(20) | B | regex SĐT | A, chính mình |
| 10 | 🆕 Email công vụ | `work_email` / `workEmail` | V(200) | B (trừ STUDENT) | unique khi có | A |
| 11 | 🆕 Ngày bắt đầu công tác | `started_on` / `startedOn` | DATE | T | | A |
| 12 | Đọc liên đơn vị | `cross_unit_access` | BOOLEAN | T | chỉ STAFF (`CROSS_UNIT_ONLY_STAFF`) | A (API riêng đã có) |
| 13 | Khóa tài khoản | `locked` | BOOLEAN | H | action riêng (đã có) | A |
| 14 | 🆕 Lần đăng nhập gần nhất | `last_login_at` / `lastLoginAt` | TIMESTAMPTZ | H | cập nhật ở lần gọi `/api/auth/me` đầu tiên trong ngày | H |
| 15 | 🆕 Ghi chú | `note` | V(1000) | T | | A |

- **Danh sách:** tab theo vai trò (Cán bộ · KTV · Admin; sinh viên quản lý ở màn Sinh viên). Cột: Mã cán bộ · Họ tên · Tên đăng nhập · Vai trò · Chức vụ · Đơn vị · Tòa phụ trách · SĐT · Email · Đăng nhập gần nhất · Khóa.
- **Excel – sheet `CanBo`:** `ID | TEN_DANG_NHAP* | HO_TEN* | MA_CAN_BO* | VAI_TRO*(STAFF/TECHNICIAN/ADMIN) | MA_DON_VI | CHUC_VU | MA_TOA_PHU_TRACH(;) | CHUYEN_MON(;) | SDT* | EMAIL_CONG_VU* | NGAY_BAT_DAU | GHI_CHU`. Tạo qua Excel đi đúng luồng tạo tài khoản + Keycloak, xuất file mật khẩu tạm một lần (giữ cơ chế hiện có); không có cột khóa/đọc liên đơn vị.

---

# Phần IV – Đăng ký và lưu trú

## 22. Đợt đăng ký (`registration_period` + `registration_period_unit` + `registration_quota`) – `/staff/registration-periods`

**Hiện có:** học kỳ, loại `REGULAR/SUMMER`, tên, `open_at`, `close_at` (close > open), người tạo; đơn vị áp dụng (N–N); chỉ tiêu theo tòa.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | 🆕 Mã đợt | `code` / `code` | V(30) | B | unique; ví dụ `DK-HK1-2026-D1` | — khi đã mở |
| 2 | Tên đợt | `name` | V | B | | S (trước khi mở) |
| 3 | Học kỳ | `semester_id` | FK | B | | — khi đã mở |
| 4 | Loại đợt | `kind` | V(10) | B | `REGULAR`, `SUMMER` | — khi đã mở |
| 5 | Mở lúc | `open_at` | TIMESTAMPTZ | B | | — khi đã mở |
| 6 | Đóng lúc | `close_at` | TIMESTAMPTZ | B | > mở; đợt đã mở chỉ được kéo dài (đã có) | S |
| 7 | Đơn vị áp dụng | `registration_period_unit` / `unitIds[]` | N–N | B | | S (trước khi mở) |
| 8 | Chỉ tiêu theo tòa | `registration_quota` / `quotas[{buildingId, quota}]` | bảng con | B | quota ≥ 0; tổng chỉ tiêu ≤ số giường trống dự kiến chỉ cảnh báo | S (trước khi mở) |
| 9 | 🆕 Đối tượng: khóa | bảng `registration_period_cohort` / `cohortIds[]` | N–N | T | trống = mọi khóa; SV ngoài danh sách nộp đơn → 🆕 `NOT_ELIGIBLE_FOR_PERIOD` | S (trước khi mở) |
| 10 | 🆕 Đối tượng: hệ đào tạo | `training_types` / `trainingTypes[]` | V(100) (danh sách mã, phân tách `,`) hoặc bảng con | T | như #9 | S (trước khi mở) |
| 11 | 🆕 Hạn kích hoạt riêng (ngày) | `activation_days` / `activationDays` | SMALLINT | T | 1–30; trống dùng `system_config` `contract.activation_days` | S (trước khi mở) |
| 12 | 🆕 Ngày dự kiến công bố kết quả | `result_announce_on` / `resultAnnounceOn` | DATE | T | ≥ ngày đóng | S |
| 13 | 🆕 Số quyết định / thông báo | `decision_no` / `decisionNo` | V(50) | T | | S |
| 14 | 🆕 Nội dung hướng dẫn cho SV | `instructions` / `instructions` | TEXT (≤ 5.000) | T | hiển thị ở trang đăng ký của SV | S |
| 15 | 🆕 Tệp đính kèm (thông báo gốc) | `attachment_path` / `attachmentUrl` | PDF | T | | S |
| 16 | 🆕 Ghi chú nội bộ | `note` | V(1000) | T | SV không xem | S |
| — | Trạng thái (Sắp mở / Đang mở / Đã đóng) | suy diễn | | H | theo thời gian | |
| — | Số đơn theo trạng thái, đã phân/chỉ tiêu | suy diễn | | H | | |

- **Excel – sheet `DotDangKy`:** `ID | MA_DOT* | TEN_DOT* | MA_HOC_KY* | LOAI*(REGULAR/SUMMER) | MO_LUC* | DONG_LUC* | MA_DON_VI*(;) | HAN_KICH_HOAT_NGAY | NGAY_CONG_BO | SO_QUYET_DINH | HUONG_DAN | GHI_CHU`; sheet `ChiTieu`: `MA_DOT* | MA_TOA* | CHI_TIEU*`; sheet `DoiTuong`: `MA_DOT* | MA_KHOA_HOC | HE_DAO_TAO`.

## 23. Đơn đăng ký (`room_application` + `application_priority`) – `/staff/applications`, `/student/applications`

**Hiện có:** sinh viên, đợt, đơn vị nguyện vọng, loại phòng mong muốn, ghi chú, `priority_score` (H), trạng thái `DRAFT/SUBMITTED/APPROVED/REJECTED/ASSIGNED/CANCELLED…`, `status_reason`, `submitted_at`; unique `(student_id, period_id)`; diện ưu tiên tự khai có xác nhận.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | 🆕 Mã đơn | `code` / `code` | V(30) | H | `DK-<năm>-<số 6 chữ số>`, sinh khi tạo; thay cột “Mã đơn” đang hiện ID số | H |
| 2 | Sinh viên | `student_id` | FK | H (`/me`) / B (Staff nhập hộ) | `/me` luôn ép người hiện tại | — |
| 3 | Đợt đăng ký | `period_id` | FK | B | đợt đang mở (`PERIOD_NOT_OPEN`); SV đủ điều kiện đối tượng (`NOT_ELIGIBLE_FOR_PERIOD`) | — |
| 4 | Đơn vị nguyện vọng 1 | `preferred_unit_id` | FK | B | thuộc đợt (`UNIT_NOT_IN_PERIOD`) | SV (DRAFT) |
| 5 | Loại phòng mong muốn 1 | `preferred_room_type_id` | FK | T | | SV (DRAFT) |
| 6 | 🆕 Đơn vị nguyện vọng 2 | `alt_unit_id` / `altUnitId` | FK | T | khác #4; thuộc đợt | SV (DRAFT) |
| 7 | 🆕 Loại phòng mong muốn 2 | `alt_room_type_id` / `altRoomTypeId` | FK | T | | SV (DRAFT) |
| 8 | 🆕 Muốn ở cùng (mã SV) | `roommate_request_code` / `roommateRequestCode` | V(20) | T | phải là mã SV tồn tại, cùng giới; **chỉ để Staff tham khảo**, không tự ghép (auto-matching ngoài MVP) | SV (DRAFT) |
| 9 | 🆕 Ngày dự kiến nhận phòng | `expected_move_in` / `expectedMoveIn` | DATE | T | trong học kỳ | SV (DRAFT) |
| 10 | Diện ưu tiên tự khai | `application_priority` / `priorities[{categoryId, evidence}]` | bảng con | T | phần tử null → `VALIDATION_FAILED`; diện `evidence_mandatory` thiếu tệp khi nộp → `PRIORITY_EVIDENCE_REQUIRED` | SV (DRAFT) |
| 11 | 🆕🔒 Minh chứng diện ưu tiên | `application_priority.evidence_path` | PDF/ảnh ≤ 5 MB, tối đa 3/diện | T | | SV (DRAFT) |
| 12 | 🆕 Hoàn cảnh gia đình | `family_circumstance` / `familyCircumstance` | V(1000) | T | | SV (DRAFT) |
| 13 | Ghi chú của SV | `note` | V | T | | SV (DRAFT) |
| 14 | 🆕 Cam kết nội quy | `rules_accepted_at` / `rulesAccepted` | TIMESTAMPTZ | B\* khi nộp | client gửi `rulesAccepted: true`, server ghi thời điểm; thiếu → 🆕 `RULES_NOT_ACCEPTED` | H |
| 15 | 🆕 SĐT liên hệ (chụp) | `contact_phone` / `contactPhone` | V(15) | H | chụp từ hồ sơ khi nộp | H |
| 16 | Điểm ưu tiên | `priority_score` | INT | H | tính khi nộp/xác nhận (đã có) | H |
| 17 | Trạng thái, lý do | `status`, `status_reason` | | H | action riêng (đã có) | H |
| 18 | 🆕 Kênh tạo | `channel` / `channel` | V(10) | H | `PORTAL` (SV tự nộp), `STAFF` (nhập hộ), `EXCEL` | H |
| 19 | 🆕 Ghi chú xét duyệt | `review_note` / `reviewNote` | V(1000) | T | chỉ Staff/Admin xem | S |

- **Danh sách Staff:** Mã đơn · SV (tên + mã) · Giới tính · Khoa/Lớp · Đơn vị NV1 · Loại phòng NV1 · Diện ưu tiên (chip, ✓ đã xác nhận) · Điểm · Ngày nộp · Trạng thái. Lọc: đợt, đơn vị, trạng thái, giới tính, có diện ưu tiên, chưa xác nhận diện, khoa, khóa. Sắp xếp mặc định: điểm giảm dần, ngày nộp tăng dần. **Mặc định ẩn DRAFT** với Staff (cần chốt SRS).
- **Form SV (mobile, từng bước):** 1 Chọn đợt & nguyện vọng (4–9) → 2 Diện ưu tiên & minh chứng (10–12) → 3 Xem lại hồ sơ cá nhân (đọc từ mục 20, báo thiếu) → 4 Cam kết & nộp (13, 14).
- **Excel – sheet `DonDangKy`** (Staff nhập hộ): `ID | MA_DOT* | MA_SV* | MA_DON_VI_NV1* | MA_LOAI_PHONG_NV1 | MA_DON_VI_NV2 | MA_LOAI_PHONG_NV2 | MA_SV_O_CUNG | NGAY_NHAN_PHONG | MA_DIEN_UU_TIEN(;) | HOAN_CANH | GHI_CHU | NOP_NGAY(1/0)`. `NOP_NGAY=1` gọi action nộp sau khi tạo (vẫn qua Policy, tính điểm ở server). Không có cột điểm, trạng thái, xác nhận diện. Minh chứng không nhập qua Excel.

## 24. Hợp đồng (`dormitory_contract`) – `/staff/contracts`, `/student/contracts`

**Hiện có:** `code V(20)`, sinh viên, đơn, `start_date`, `end_date` (end > start), trạng thái `PENDING/ACTIVE/EXPIRED/TERMINATED/CANCELLED`, `activation_deadline`, `deposit_required`, `activated_at/by`, `closed_at`, `status_reason`.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Số hợp đồng | `code` | V(20) | H | sinh khi tạo | — |
| 2 | Sinh viên, Đơn nguồn | `student_id`, `application_id` | FK | B | đơn APPROVED (đã có) | — |
| 3 | Ngày bắt đầu / kết thúc | `start_date`, `end_date` | DATE | B | trong học kỳ của đợt; không chồng hợp đồng khác (`CONTRACT_PERIOD_OVERLAP`) | S (PENDING) |
| 4 | Tiền cọc yêu cầu | `deposit_required` | BIGINT | H | chụp mức cọc tòa khi phân giường | H |
| 5 | 🆕 Số hợp đồng giấy | `paper_no` / `paperNo` | V(30) | T | unique khi có | S |
| 6 | 🆕 Ngày ký | `signed_on` / `signedOn` | DATE | B\* khi kích hoạt | ≤ hôm nay; thiếu khi kích hoạt → `VALIDATION_FAILED` | S (PENDING) |
| 7 | 🆕 Đại diện ký phía KTX | `signed_by_user_id` / `signedByUserId` | FK `app_user` | B\* khi kích hoạt | STAFF/ADMIN cùng đơn vị | S (PENDING) |
| 8 | 🆕 Phiên bản điều khoản | `terms_version` / `termsVersion` | V(20) | H | chụp `system_config` `contract.terms_version` 🆕 khi kích hoạt | H |
| 9 | 🆕 Bản scan hợp đồng đã ký | `signed_file_path` / `signedFileUrl` | PDF ≤ 5 MB | T | | S |
| 10 | 🆕 Phương thức thu cọc | ghi ở `deposit_transaction` (mục 32) | | | | |
| 11 | Kích hoạt lúc/bởi, đóng lúc, lý do | `activated_at/by`, `closed_at`, `status_reason` | | H | action riêng | H |
| 12 | 🆕 Ghi chú | `note` | V(1000) | T | | S |
| — | Phòng/giường hiện tại, đơn giá, số dư cọc, cờ “Sắp hết hạn”, “Quá hạn trả phòng” | suy diễn | | H | `room_assignment_history`, `v_deposit_balance`, cấu hình ngưỡng | |

- **Danh sách:** Số HĐ · SV · Tòa–Phòng–Giường · Từ ngày · Đến ngày · Cọc yêu cầu / đã thu · Trạng thái + chip suy diễn. Lọc: trạng thái, tòa, sắp hết hạn, quá hạn trả phòng, thiếu cọc, học kỳ.
- **Modal Kích hoạt (đã có `ActivateContractModal`):** thêm #5–#9 và bước thu cọc; xác nhận tạo biên bản nhận phòng (mục 7.3).
- **Excel – sheet `HopDong`:** `ID | MA_DON* | NGAY_BAT_DAU* | NGAY_KET_THUC* | SO_HD_GIAY | GHI_CHU`. Chỉ tạo hồ sơ `PENDING` từ đơn APPROVED và sửa hồ sơ PENDING (đúng phạm vi CRUD hợp đồng hiện tại); kích hoạt, thu cọc không làm qua Excel.

## 25. Yêu cầu gia hạn (`contract_extension_request`) – `/student/requests`, `/staff/extensions`

**Hiện có:** hợp đồng, nguồn, ngày kết thúc cũ/mới, lý do, trạng thái, người/lúc/lý do quyết định.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | 🆕 Mã yêu cầu | `code` | V(30) | H | `GH-<năm>-<số>` | H |
| 2 | Hợp đồng | `contract_id` | FK | H (`/me`) | ACTIVE (`CONTRACT_NOT_ACTIVE`) | — |
| 3 | Ngày kết thúc mới | `new_end_date` | DATE | B | > ngày kết thúc cũ; ≤ hết học kỳ kế tiếp | SV (PENDING) |
| 4 | 🆕 Học kỳ gia hạn đến | `target_semester_id` / `targetSemesterId` | FK | B | ngày kết thúc mới nằm trong học kỳ này | SV (PENDING) |
| 5 | 🆕 Nhóm lý do | `reason_type` / `reasonType` | V(20) | B | `STUDY`, `INTERNSHIP`, `THESIS`, `FAMILY`, `OTHER` | SV (PENDING) |
| 6 | Lý do chi tiết | `reason` | V | B nếu `OTHER` | | SV (PENDING) |
| 7 | 🆕 Giữ phòng/giường hiện tại | `keep_current_bed` / `keepCurrentBed` | BOOLEAN | B | mặc định TRUE | SV (PENDING) |
| 8 | 🆕 Minh chứng | `evidence_path` | PDF/ảnh | T | | SV (PENDING) |
| 9 | Quyết định | `status`, `decided_by/at`, `decision_reason` | | H | action duyệt/từ chối (đã có); từ chối bắt buộc lý do | H |

Excel `GiaHan` (Staff nhập hộ): `ID | SO_HD* | NGAY_KET_THUC_MOI* | MA_HOC_KY* | NHOM_LY_DO* | LY_DO | GIU_GIUONG(1/0)`.

## 26. Yêu cầu chuyển phòng (`room_transfer_request`) – `/student/transfer`, `/staff/transfers`

**Hiện có:** hợp đồng, lý do, trạng thái, giường đích (Staff chọn khi duyệt), người/lúc/lý do quyết định.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | 🆕 Mã yêu cầu | `code` | V(30) | H | `CP-<năm>-<số>` | H |
| 2 | Hợp đồng | `contract_id` | FK | H (`/me`) | ACTIVE, đang ở; một yêu cầu PENDING (`REQUEST_PENDING_EXISTS`) | — |
| 3 | 🆕 Nhóm lý do | `reason_type` / `reasonType` | V(20) | B | `ROOMMATE`, `HEALTH`, `ROOM_TYPE`, `FACILITY`, `OTHER` | SV (PENDING) |
| 4 | Lý do chi tiết | `reason` | V | B | | SV (PENDING) |
| 5 | 🆕 Tòa mong muốn | `preferred_building_id` / `preferredBuildingId` | FK | T | cùng đơn vị (`TRANSFER_CROSS_UNIT`) | SV (PENDING) |
| 6 | 🆕 Loại phòng mong muốn | `preferred_room_type_id` / `preferredRoomTypeId` | FK | T | | SV (PENDING) |
| 7 | 🆕 Phòng cụ thể mong muốn | `preferred_room_id` / `preferredRoomId` | FK | T | cùng giới | SV (PENDING) |
| 8 | 🆕 Ngày muốn chuyển | `desired_date` / `desiredDate` | DATE | T | ≥ hôm nay, < ngày kết thúc hợp đồng | SV (PENDING) |
| 9 | 🆕 Minh chứng (giấy khám…) | `evidence_path` | PDF/ảnh | T | | SV (PENDING) |
| 10 | Giường đích | `target_bed_id` | FK | B\* khi duyệt | cùng đơn vị, trống, cùng giới (đã có) | S |
| 11 | 🆕 Ngày chuyển thực tế | `transferred_at` | TIMESTAMPTZ | H | khi duyệt | H |
| 12 | 🆕 Chênh lệch giá | suy diễn | | H | giá loại phòng mới − cũ, hiển thị cho Staff trước khi duyệt; xử lý tiền theo FR-BIL-12 (điều chỉnh) | |

Excel `ChuyenPhong`: `ID | SO_HD* | NHOM_LY_DO* | LY_DO* | MA_TOA_MONG_MUON | MA_LOAI_PHONG | MA_PHONG_MONG_MUON | NGAY_MUON_CHUYEN`.

## 27. Lựa chọn hè (`summer_choice`) – `/student/summer`, `/staff/summer`

**Hiện có:** hợp đồng, học kỳ, lựa chọn (ở hè / tạm rời hè / rời KTX), trạng thái, người/lúc quyết định.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Lựa chọn | `choice` | V | B | 3 giá trị hiện có | SV (PENDING/REJECTED) |
| 2 | 🆕 Ngày dự kiến rời | `leave_on` / `leaveOn` | DATE | B nếu tạm rời/rời KTX | trong khoảng hết HK2 ± 14 ngày | SV |
| 3 | 🆕 Ngày dự kiến quay lại | `return_on` / `returnOn` | DATE | B nếu tạm rời | trước ngày bắt đầu HK1 kế tiếp | SV |
| 4 | 🆕 Gửi đồ tại kho | `needs_storage` / `needsStorage` | BOOLEAN | T (tạm rời) | | SV |
| 5 | 🆕 Ước tính số kiện | `estimated_packages` / `estimatedPackages` | SMALLINT | T | 1–10 | SV |
| 6 | 🆕 Lý do (rời KTX) | `reason` | V(500) | T | | SV |
| 7 | 🆕 Ghi chú duyệt / lý do từ chối | `decision_reason` | V(500) | B khi từ chối | | S |

Excel `LuaChonHe`: `ID | SO_HD* | MA_HOC_KY_HE* | LUA_CHON* | NGAY_ROI | NGAY_QUAY_LAI | GUI_DO(1/0) | SO_KIEN | LY_DO`.

## 28. Phiếu gửi đồ (`storage_record`) – `/staff/storage`

**Hiện có:** hợp đồng, lựa chọn hè (unique), giường cũ, `package_count > 0`, mô tả, vị trí, gửi lúc / người nhận, trả lúc / người trả.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | 🆕 Mã phiếu | `code` | V(30) | H | `GD-<năm>-<số>`; in trên nhãn dán kiện | H |
| 2 | Số kiện | `package_count` | SMALLINT | B | > 0; bằng tổng số lượng các dòng vật phẩm nếu có | S (chưa trả) |
| 3 | Vị trí kho | `location_id` | FK | B | cùng đơn vị | S (chưa trả) |
| 4 | Mô tả chung | `description` | V | T | | S |
| 5 | 🆕 Danh sách vật phẩm | bảng `storage_item(record_id, name, quantity, note)` / `items[]` | bảng con | T | phần tử null → `VALIDATION_FAILED` | S (chưa trả) |
| 6 | 🆕 Ảnh khi nhận gửi | `storage_photo` | ảnh × 3 | T | | S |
| 7 | 🆕 Hạn nhận lại | `due_on` / `dueOn` | DATE | H | ngày bắt đầu HK1 + `storage.pickup_days` 🆕 | H |
| 8 | 🆕 Người nhận hộ (khi trả) | `pickup_by_name` / `pickupByName` | V(100) | T | | S (khi trả) |
| 9 | 🆕🔒 CCCD người nhận hộ | `pickup_by_citizen_id` / `pickupByCitizenId` | V(20) | B nếu có #8 | | S (khi trả) |
| 10 | 🆕 Tình trạng khi trả | `return_condition` / `returnCondition` | V(500) | T | | S (khi trả) |
| 11 | 🆕 Ghi chú | `note` | V(1000) | T | | S |

Excel `PhieuGuiDo`: `ID | SO_HD* | MA_VI_TRI* | SO_KIEN* | MO_TA | GHI_CHU`; sheet `VatPham`: `MA_PHIEU* | TEN* | SO_LUONG* | GHI_CHU`. Trả đồ không qua Excel (cần xác nhận tại quầy).

---

# Phần V – Tài chính, vận hành, thông báo

## 29. Chỉ số điện nước (`utility_reading`) – `/staff/utility-readings`

**Hiện có:** phòng, loại đồng hồ (điện/nước), tháng, chỉ số đầu, chỉ số cuối (`CHECK(end_reading >= start_reading)`), `old_meter_final`, `new_meter_start`, `replacement_reason` (thay đồng hồ – FR-BIL-03), `consumption`, người ghi; unique `(room_id, meter_type, month)`.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Phòng | `room_id` | FK | B | trong phạm vi đơn vị | — |
| 2 | Loại đồng hồ | `meter_type` | V | B | `ELECTRIC`, `WATER` | — |
| 3 | Tháng | `month` | DATE (ngày 1) | B | không ở tương lai | — |
| 4 | Chỉ số đầu | `start_reading` | NUMERIC | H/B | tự lấy từ chỉ số cuối tháng trước; chỉ nhập tay ở kỳ đầu tiên của phòng | S (kỳ đầu) |
| 5 | Chỉ số cuối | `end_reading` | NUMERIC | B | ≥ chỉ số đầu (đã có) | S (chưa lập HĐ, chưa có kỳ sau) |
| 6 | Thay đồng hồ | `old_meter_final`, `new_meter_start`, `replacement_reason` | | T | đã có; bắt buộc đủ bộ ba nếu có một | S |
| 7 | 🆕 Số đồng hồ | `meter_no` / `meterNo` | V(30) | H | chụp `room.electric_meter_no`/`water_meter_no` (mục 5) khi ghi | H |
| 8 | 🆕 Ngày ghi thực tế | `read_on` / `readOn` | DATE | B | trong tháng ghi hoặc ≤ 5 ngày đầu tháng sau | S |
| 9 | 🆕 Ảnh đồng hồ | `reading_photo_path` / `photoUrl` | ảnh ≤ 5 MB | T | bắt buộc nếu `system_config` `utility.photo_required` 🆕 = true | S |
| 10 | 🆕 Bất thường | `anomaly` / `anomaly` | suy diễn | H | tiêu thụ > `utility.anomaly_ratio` × trung bình 3 tháng trước → chip cảnh báo, **không chặn** | |
| 11 | 🆕 Ghi chú | `note` | V(500) | T | | S |
| — | Tiêu thụ | `consumption` | | H | đã có | |

- **Màn nhập theo lô (khuyến nghị):** chọn tòa + tháng → bảng tất cả phòng, cột chỉ số đầu (đọc sẵn), ô nhập chỉ số cuối, cột tiêu thụ tính ngay, chip bất thường; lưu từng dòng qua `save-or-update` hoặc gói Excel.
- **Excel – sheet `ChiSoDienNuoc`:** `ID | MA_TOA* | MA_PHONG* | LOAI*(ELECTRIC/WATER) | THANG*(yyyy-MM) | CHI_SO_DAU | CHI_SO_CUOI* | NGAY_GHI* | CHI_SO_CUOI_DH_CU | CHI_SO_DAU_DH_MOI | LY_DO_THAY | GHI_CHU`. Mẫu tải về đã điền sẵn tòa/phòng/chỉ số đầu của tháng được chọn để Staff chỉ nhập chỉ số cuối.

## 30. Đơn giá điện nước (`utility_rate`) – `/admin/utility-rates`

**Hiện có:** loại đồng hồ, `unit_price > 0`, `effective_from`; unique `(meter_type, effective_from)`; giá đã có hiệu lực không sửa (`RATE_ALREADY_EFFECTIVE`).

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Loại | `meter_type` | V | B | | — |
| 2 | Hiệu lực từ | `effective_from` | DATE | B | > hôm nay khi tạo mới | S (tương lai) |
| 3 | Đơn giá | `unit_price` | BIGINT | B | > 0 | S (tương lai) |
| 4 | 🆕 Đơn vị tính | `unit` / `unit` | V(10) | H | `KWH` cho điện, `M3` cho nước – suy từ loại, chỉ hiển thị | H |
| 5 | 🆕 Bậc thang | bảng `utility_rate_tier(rate_id, from_qty, to_qty, unit_price)` / `tiers[]` | bảng con | T | bậc liên tục từ 0, không chồng, bậc cuối `to_qty` trống; nếu có bậc thì `unit_price` là giá tham chiếu. **Chỉ dùng nếu nhóm chốt tính bậc thang** (SRS hiện tính một giá) | S (tương lai) |
| 6 | 🆕 Thuế VAT (%) | `vat_percent` / `vatPercent` | NUMERIC(4,2) | T | 0–20; trống = giá đã gồm thuế | S (tương lai) |
| 7 | 🆕 Căn cứ (quyết định giá) | `decision_no` / `decisionNo` | V(50) | T | | S |
| 8 | 🆕 Ghi chú | `note` | V(500) | T | | S |

Excel `DonGia`: `ID | LOAI* | HIEU_LUC_TU* | DON_GIA* | VAT | SO_QUYET_DINH | GHI_CHU`; sheet `BacThang`: `LOAI* | HIEU_LUC_TU* | TU* | DEN | DON_GIA*`.

## 31. Hóa đơn (`invoice` + `invoice_item`) – `/staff/invoices`, `/student/invoices`

**Hiện có (18 cột):** mã, loại `ROOM_FEE/UTILITY`, sinh viên hoặc phòng, học kỳ hoặc tháng, tổng tiền, trạng thái `UNPAID/PAID`, hạn thanh toán, phát hành lúc/bởi, thanh toán lúc/bởi, phương thức `CASH/TRANSFER`, ghi chú…; unique một hóa đơn / (SV, học kỳ) hoặc (phòng, tháng). Dòng chi tiết: `item_type`, `quantity`, `unit_price`, `amount`, `assignment_id`, `utility_reading_id`, `adjusts_invoice_id`, `note`.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Mã hóa đơn | `code` | V | H | sinh khi phát hành | — |
| 2 | Loại, đối tượng, kỳ | đã có | | B | | — |
| 3 | Hạn thanh toán | `due_date` | DATE | B | ≥ ngày phát hành | S (UNPAID) |
| 4 | 🆕 Người nộp (phòng – trưởng phòng) | `payer_student_id` / `payerStudentId` | FK | H (UTILITY) | chụp trưởng phòng lúc phát hành (BR: trưởng phòng nộp, không chia) | H |
| 5 | 🆕 Kỳ tính từ / đến | `period_from`, `period_to` | DATE | H | từ học kỳ/tháng | H |
| 6 | 🆕 Số tiền đã giảm | `discount_amount` / `discountAmount` | BIGINT | T | ≥ 0, ≤ tổng; chỉ khi có diện miễn giảm được duyệt (chờ SRS) | S (UNPAID) |
| 7 | 🆕 Phải thu | suy diễn | | H | tổng − giảm | |
| 8 | Thanh toán: lúc, bởi, phương thức | `paid_at`, `paid_by`, `payment_method` | | H | action xác nhận (đã có) | H |
| 9 | 🆕 Mã giao dịch ngân hàng | `transaction_ref` / `transactionRef` | V(50) | B\* nếu `TRANSFER` | unique khi có (chống ghi trùng một giao dịch) → `DUPLICATE_VALUE` | H (nhập khi xác nhận) |
| 10 | 🆕 Số biên lai | `receipt_no` / `receiptNo` | V(30) | H | sinh khi xác nhận `BL-<năm>-<số>` | H |
| 11 | 🆕 Người nộp thực tế | `paid_by_name` / `paidByName` | V(100) | T | khi người khác nộp hộ | H (nhập khi xác nhận) |
| 12 | 🆕 Ảnh chứng từ chuyển khoản | `payment_proof_path` / `paymentProofUrl` | ảnh/PDF | T | SV có thể tải lên trước để Staff đối chiếu (không tự đổi trạng thái) | SV (UNPAID) |
| 13 | 🆕 Hủy hóa đơn | `voided_at/by`, `void_reason` | | H | action `/void` riêng khi UNPAID và chưa có điều chỉnh; trạng thái mới 🆕 `VOID` (cần SRS mục 10) | H |
| 14 | Ghi chú | `note` | V | T | | S (UNPAID) |
| — | Quá hạn, số ngày quá hạn | suy diễn | | H | | |

**Dòng chi tiết (`invoice_item`)** giữ cấu trúc hiện có; bổ sung 🆕 `description` V(200) (diễn giải in trên biên lai, ví dụ “Điện tháng 10/2026: 120 kWh × 3.000 đ”).

- **Danh sách Staff:** Mã · Loại · **Đối tượng** (SV: tên + mã; phòng: Tòa–Phòng + trưởng phòng) · Kỳ · Phải thu · Hạn · Trạng thái + chip quá hạn. Lọc: loại, trạng thái, tòa, kỳ, quá hạn, phương thức.
- **Xuất biên lai PDF** (in tại quầy) – phần trình bày, không thêm dữ liệu.
- **Excel:**
  - `HoaDon` (chỉ lập hóa đơn **nháp/điều chỉnh** thủ công nếu nghiệp vụ cho phép): `ID | LOAI* | MA_SV | MA_PHONG | MA_HOC_KY | THANG | HAN_THANH_TOAN* | GHI_CHU`; sheet `ChiTietHoaDon`: `MA_HD* | LOAI_DONG* | SO_LUONG* | DON_GIA* | DIEN_GIAI`. Phát hành hàng loạt vẫn là action riêng từ chỉ số/hợp đồng.
  - 🆕 `XacNhanThanhToan` (đối soát sao kê): `MA_HD* | SO_TIEN* | PHUONG_THUC* | MA_GIAO_DICH | NGAY_NOP* | NGUOI_NOP`. Server kiểm số tiền = phải thu (lệch → 🆕 `PAYMENT_AMOUNT_MISMATCH`), hóa đơn UNPAID, rồi gọi đúng action xác nhận cho từng dòng.

## 32. Giao dịch cọc (`deposit_transaction`) – `/staff/deposits`

**Hiện có:** hợp đồng, loại `DEPOSIT/DEDUCTION/TOPUP/REFUND`, số tiền, `maintenance_request_id` (khấu trừ do hư hỏng sử dụng), `due_date`, người xác nhận, ghi chú; một `REFUND` / hợp đồng. Sổ cọc chỉ ghi thêm, không sửa/xóa tùy ý.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | 🆕 Mã phiếu | `code` | V(30) | H | `PT-` (thu), `PC-` (chi/hoàn) + năm + số | H |
| 2 | Hợp đồng, loại, số tiền | đã có | | B | số tiền > 0; khấu trừ ≤ số dư (dùng mã hiện có `DEPOSIT_INVALID`) | — |
| 3 | Lý do khấu trừ | `maintenance_request_id` | FK | B\* (`DEDUCTION`) | ticket nguyên nhân `MISUSE` (BR: chỉ hư hỏng do sử dụng) | — |
| 4 | 🆕 Biên bản liên quan | `condition_report_id` / `conditionReportId` | FK | T | biên bản CHECK_OUT (mục 7) | — |
| 5 | 🆕 Phương thức | `payment_method` / `paymentMethod` | V(10) | B | `CASH`, `TRANSFER` | — |
| 6 | 🆕 Mã giao dịch | `transaction_ref` | V(50) | B\* nếu `TRANSFER` | unique khi có | — |
| 7 | 🆕🔒 Tài khoản nhận hoàn cọc | `refund_bank_name`, `refund_account_name`, `refund_account_no` | V | B\* (`REFUND` + `TRANSFER`) | che số tài khoản khi hiển thị; SV khai ở trang trả phòng; Staff không gõ hộ | — |
| 8 | 🆕 Người giao/nhận tiền | `counterparty_name` / `counterpartyName` | V(100) | T | | — |
| 9 | Ghi chú | `note` | V | T | | — |
| 10 | 🆕 Phiếu đảo | `reverses_id` / `reversesId` | FK | T | sửa sai bằng một giao dịch đảo dấu tham chiếu phiếu gốc, **không sửa phiếu gốc** (cần SRS) | — |
| — | Số dư cọc | `v_deposit_balance` | | H | đã có | |

Excel `GiaoDichCoc` (nhập đối soát thu cọc đầu kỳ): `HOP_DONG* | LOAI*(DEPOSIT/TOPUP) | SO_TIEN* | PHUONG_THUC* | MA_GIAO_DICH | NGUOI_NOP | GHI_CHU`. Không có cột `ID` (chỉ ghi thêm); `DEDUCTION`/`REFUND` không nhập qua Excel vì cần ticket/biên bản và xác nhận từng phiếu.

## 33. Yêu cầu sửa chữa (`maintenance_request`) – `/student/tickets`, `/staff/tickets`, `/technician/tasks`

**Hiện có (19 cột):** mã, người báo, phòng, hạng mục, mô tả, mức độ, trạng thái, KTV được giao, nguyên nhân `WEAR/MISUSE`, chi phí, thời điểm giao/bắt đầu/hoàn thành/đóng, đánh giá…; ảnh tối đa 3 (multipart).

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Mã ticket | `code` | V | H | | — |
| 2 | Phòng | `room_id` | FK | H (SV: phòng đang ở) / B (Staff) | | — |
| 3 | 🆕 Vị trí chi tiết | `location_detail` / `locationDetail` | V(100) | T | ví dụ “Nhà vệ sinh”, “Ổ cắm cạnh giường B” | SV (NEW) |
| 4 | 🆕 Tài sản liên quan | `room_asset_id` / `roomAssetId` | FK | T | tài sản của phòng (mục 7) | SV (NEW) |
| 5 | Hạng mục | `category_id` | FK | B | | SV (NEW) |
| 6 | Mô tả | `description` | V | B | 10–1.000 ký tự | SV (NEW) |
| 7 | Mức độ | `severity` | V | B | mặc định theo hạng mục; Staff được nâng/hạ | S |
| 8 | 🆕 Khung giờ có thể vào phòng | `available_slots` / `availableSlots` | V(200) | T | ví dụ “Sau 17h các ngày thường” | SV (NEW) |
| 9 | 🆕 Cho phép vào khi vắng mặt | `entry_permitted` / `entryPermitted` | BOOLEAN | B | mặc định FALSE | SV (NEW) |
| 10 | 🆕 SĐT liên hệ | `contact_phone` | V(15) | H | chụp từ hồ sơ | H |
| 11 | 🆕 Hạn xử lý | `due_at` / `dueAt` | TIMESTAMPTZ | H | lúc tạo + SLA (mục 17); chip “Quá hạn” suy diễn | H |
| 12 | KTV, giao lúc | đã có | | B\* khi giao | KTV đúng đơn vị; gợi ý theo chuyên môn | action |
| 13 | 🆕 Kết quả xử lý | `resolution` / `resolution` | V(1000) | B\* khi hoàn thành | | KTV (action) |
| 14 | 🆕 Vật tư sử dụng | bảng `maintenance_material(request_id, name, quantity, unit, unit_cost)` / `materials[]` | bảng con | T | | KTV (action) |
| 15 | Nguyên nhân, chi phí | `cause`, `cost` | | B\* khi hoàn thành | `MISUSE` + chi phí > 0 mới được khấu trừ cọc | KTV (action) |
| 16 | 🆕 Ảnh sau sửa | `maintenance_photo.phase = AFTER` | ảnh × 3 | T | cột `phase` 🆕 `BEFORE/AFTER`, ảnh cũ mặc định `BEFORE` | KTV (action) |
| 17 | Đánh giá của SV | đã có (`rating`, `feedback`) | | T | 1–5 | SV (sau hoàn thành) |
| 18 | 🆕 Mở lại | `reopened_count` / `reopenedCount` | SMALLINT | H | action `/reopen` trong thời gian tự đóng (cần SRS) | H |

- **Danh sách Staff:** Mã · Tòa–Phòng · Hạng mục · Mức độ · Trạng thái · KTV · Tạo lúc · Hạn xử lý (đỏ nếu quá). **Màn KTV (mobile):** thẻ việc theo hạn xử lý, nút gọi SV, mở bản đồ tầng/phòng, cập nhật kết quả + ảnh sau.
- **Excel `SuaChua`** (Staff ghi nhận hộ sự cố qua điện thoại / kiểm tra định kỳ): `ID | MA_TOA* | MA_PHONG* | MA_HANG_MUC* | MO_TA* | MUC_DO | VI_TRI | MA_TAI_SAN | MA_SV_BAO`. Ảnh, giao việc, hoàn thành không qua Excel.

## 34. Thông báo (`notification`) – `/staff/notifications`, mọi cổng

**Hiện có (13 cột):** tiêu đề, nội dung, phạm vi `ALL/UNIT/BUILDING/FLOOR/ROOM/USER` + đích, loại (thủ công/hệ thống), người tạo, thời điểm; bảng người nhận có trạng thái đã đọc.

| # | Trường | DB / JSON | Kiểu | BB | Kiểm tra | Sửa |
|---|---|---|---|---|---|---|
| 1 | Tiêu đề | `title` | V(200) | B | | S (thủ công) |
| 2 | Nội dung | `content` | TEXT | B | ≤ 10.000; định dạng Markdown giới hạn (đậm, nghiêng, danh sách, liên kết), render an toàn | S (thủ công) |
| 3 | Phạm vi + đích | đã có | | B | trong phạm vi đơn vị của Staff | — |
| 4 | 🆕 Chuyên mục | `category` / `category` | V(20) | B | `GENERAL`, `FEE`, `REGISTRATION`, `MAINTENANCE`, `DISCIPLINE`, `EVENT`, `EMERGENCY` | S |
| 5 | 🆕 Mức ưu tiên | `priority` / `priority` | V(10) | B | `NORMAL`, `IMPORTANT`, `URGENT`; `URGENT` hiện banner trên trang chủ | S |
| 6 | 🆕 Ghim đến | `pinned_until` / `pinnedUntil` | TIMESTAMPTZ | T | ≤ 30 ngày | S |
| 7 | 🆕 Hẹn giờ đăng | `publish_at` / `publishAt` | TIMESTAMPTZ | T | > hiện tại; trước giờ đăng người nhận không thấy; job phát hành | S (chưa đăng) |
| 8 | 🆕 Hết hiệu lực | `expires_at` / `expiresAt` | TIMESTAMPTZ | T | > `publish_at` | S |
| 9 | 🆕 Tệp đính kèm | bảng `notification_attachment` | PDF/ảnh ≤ 5 MB × 3 | T | | S (thủ công) |
| 10 | 🆕 Yêu cầu xác nhận đã đọc | `ack_required` / `ackRequired` | BOOLEAN | T | Staff xem tỉ lệ đã đọc theo phòng | — |
| — | Số người nhận, đã đọc | suy diễn | | H | | |

Excel `ThongBao` (soạn hàng loạt, ví dụ nhắc phí từng phòng): `ID | TIEU_DE* | NOI_DUNG* | PHAM_VI* | MA_DICH | CHUYEN_MUC* | UU_TIEN* | HEN_GIO | HET_HIEU_LUC`. `MA_DICH` là mã tòa / `MA_TOA-MA_PHONG` / mã SV / mã đơn vị tùy phạm vi. Thông báo hệ thống không nhập/sửa qua Excel.

---

# Phần VI – Nhập/Xuất Excel ở mọi màn

Quyết định của nhóm: **giữ nút Nhập Excel và Xuất Excel trên mọi màn danh sách**. Phần này chuẩn hóa để Excel không trở thành “cửa sau” vượt qua quyền, phạm vi và Policy.

## VI.1. Nguyên tắc

1. **Excel đi qua đúng một cổng nghiệp vụ.** Mỗi dòng hợp lệ được chuyển thành đúng DTO của `save-or-update` (hoặc action riêng đã nêu ở từng mục) và gọi chính hàm Service đó. Không viết SQL nhập riêng, không bỏ qua `ScopeGuard`, Policy, audit.
2. **Cột `ID` quyết định tạo hay sửa.** Ô trống → tạo; số nguyên dương → sửa; ID không tồn tại hoặc ngoài phạm vi → lỗi dòng `NOT_FOUND` (không tự tạo thay). Sửa từng phần: **ô trống = giữ giá trị cũ**; muốn xóa text tùy chọn thì ghi đúng ký hiệu `#XOA` (chỉ áp dụng cho trường cho phép xóa).
3. **Tham chiếu bằng mã, không bằng ID nội bộ.** Ví dụ `MA_TOA`, `MA_PHONG`, `MA_SV`, `MA_HOC_KY`. Server tra mã trong phạm vi của người nhập; mã ngoài phạm vi báo như không tồn tại.
4. **Tài nguyên có trạng thái chỉ được tạo ở trạng thái đầu** (đơn `DRAFT`, hợp đồng `PENDING`, ticket `NEW`, hóa đơn `UNPAID`). Không có cột trạng thái/điểm/người duyệt. Các action được phép qua Excel được nêu rõ ở từng mục (ví dụ `NOP_NGAY` ở mục 23, sheet `XacNhanThanhToan` ở mục 31) và vẫn gọi đúng action, đúng quyền.
5. **Không nhập qua Excel:** tệp/ảnh/minh chứng, mật khẩu, dữ liệu 🔒 ngoài quyền `STUDENT_SENSITIVE_READ`, sổ cọc khấu trừ/hoàn, trả đồ, kích hoạt hợp đồng, thông báo hệ thống, audit.
6. **Hai bước bắt buộc: kiểm tra → ghi.** Người dùng luôn xem kết quả kiểm tra trước khi ghi.
7. **Quyền:** nhập cần quyền CREATE (có dòng tạo) và/hoặc UPDATE (có dòng sửa) của tài nguyên; xuất cần READ. Không thêm quyền `*_IMPORT` riêng để tránh nhân ma trận quyền (cần nhóm chốt; nếu muốn tách thì thêm `<RESOURCE>_IMPORT` qua migration seed).

## VI.2. Hợp đồng API chung

Khai trực tiếp trong mỗi Controller (không tạo Constants cho URL), dùng một lớp xử lý chung `common/excel` (🆕) để mọi tài nguyên giống nhau.

| Endpoint | Body | Kết quả |
|---|---|---|
| `GET /api/<resource>/import/template` | – | tệp `.xlsx` mẫu sinh từ registry cột |
| `POST /api/<resource>/import/validate` | multipart `file` (.xlsx ≤ 5 MB, kiểm magic bytes) | `ImportValidationResponse` |
| `POST /api/<resource>/import/commit` | `{ "token": "…" }` | `ImportCommitResponse` |
| `POST /api/<resource>/export` | cùng body `search-by-page` (bỏ `page/size`) | tệp `.xlsx` sinh ở server bằng Apache POI, lọc phạm vi trước khi truy vấn |

**`ImportValidationResponse`:**

```json
{
  "token": "imp_7f3c…",
  "expiresAt": "2026-11-19T10:30:00+07:00",
  "totalRows": 120,
  "createCount": 100,
  "updateCount": 15,
  "errorCount": 5,
  "rows": [
    { "sheet": "Phong", "row": 8, "action": "CREATE", "key": "K1-101" },
    { "sheet": "Phong", "row": 9, "action": "ERROR",
      "errors": [ { "column": "MA_LOAI_PHONG", "errorCode": "NOT_FOUND", "value": "LP9" } ] }
  ]
}
```

- `token` lưu bản phân tích (không lưu tệp gốc) trong bảng 🆕 `import_session(token, resource, actor_id, payload JSONB, expires_at)`; hết hạn sau `import.token_minutes` 🆕 (mặc định 30). Chỉ người tạo được commit. Payload không chứa dữ liệu 🔒 ở dạng rõ nếu người nhập không có quyền xem.
- Lỗi dùng `errorCode` có sẵn (`VALIDATION_FAILED`, `NOT_FOUND`, `DUPLICATE_VALUE`, mã nghiệp vụ…) để React dịch bằng `errorMessages.ts`; trùng mã ngay trong tệp báo 🆕 `IMPORT_DUPLICATE_ROW`.
- Tối đa `import.max_rows` 🆕 (mặc định 2.000) dòng; vượt → `IMPORT_FILE_INVALID`.

**`ImportCommitResponse`:** `{ created, updated, failed, rows:[{row, id, action, errors?}] }`.

**Chế độ ghi** (tham số `mode` khi validate):

| `mode` | Ý nghĩa | Mặc định cho |
|---|---|---|
| `ALL_OR_NOTHING` | một transaction; có lỗi bất kỳ thì không ghi gì, `commit` bị từ chối khi `errorCount > 0` | danh mục, cơ sở vật chất, đơn giá |
| `VALID_ROWS` | mỗi dòng một transaction (TransactionTemplate, giống job); dòng lỗi bị bỏ qua và trả về | sinh viên, chỉ số điện nước, xác nhận thanh toán, đơn đăng ký |

Ở cả hai chế độ, commit **kiểm tra lại** mọi dòng (dữ liệu có thể đổi giữa hai bước) và khóa bản ghi theo thứ tự khóa toàn cục trước khi ghi.

## VI.3. Mẫu Excel

- **Sheet dữ liệu** tên tiếng Việt không dấu như từng mục (`Phong`, `SinhVien`, …). Dòng 1 tiêu đề mã cột (`MA_PHONG*`, dấu `*` = bắt buộc khi tạo), dòng 2 nhãn tiếng Việt + ghi chú định dạng, dữ liệu từ dòng 3. Cột `ID` luôn đầu tiên.
- **Sheet ẩn `DanhMuc`** chứa danh sách mã hợp lệ trong phạm vi người tải (tòa, loại phòng, khoa, ngành, diện ưu tiên, giá trị enum). Cột tham chiếu có **Data Validation** dạng danh sách trỏ tới `DanhMuc` để người nhập chọn thay vì gõ.
- **Sheet `HuongDan`**: mô tả từng cột (sinh từ cùng registry), quy tắc `ID` trống/có, ký hiệu `#XOA`, định dạng ngày `yyyy-MM-dd`, tiền là số nguyên không dấu phân cách.
- Cột ngày định dạng Date, cột mã/CCCD/SĐT định dạng Text (giữ số 0 đầu).
- Xuất Excel dùng **cùng bố cục** với mẫu nhập (có cột `ID`), để quy trình “xuất → sửa → nhập lại” hoạt động. Cột suy diễn (số giường trống, số dư cọc…) nằm ở cuối, tiêu đề xám, bị bỏ qua khi nhập.

## VI.4. Giao diện

`CrudExcelPanel` hiện có giữ nguyên vị trí trên mọi màn, đổi luồng thành 3 bước trong Drawer:

1. **Tải mẫu / chọn tệp** – nút “Tải mẫu”, vùng kéo thả, chọn chế độ ghi (nếu tài nguyên cho phép đổi).
2. **Kết quả kiểm tra** – thẻ tổng (tạo / sửa / lỗi), bảng lỗi lọc theo sheet/cột, nút “Tải tệp lỗi” (tệp gốc + cột `LOI` tô đỏ).
3. **Ghi dữ liệu** – nút “Ghi N dòng hợp lệ” (hoặc bị vô hiệu ở `ALL_OR_NOTHING` khi còn lỗi), hiển thị tiến độ, xong thì invalidate query danh sách.

Xuất Excel chuyển từ vòng lặp phía client (`exportFilteredRecords` trong `features/crud/excel.ts`) sang endpoint `/export` ở server.

## VI.5. Bảng tổng hợp sheet theo màn

| Mục | Màn | Sheet | Tạo | Sửa | Ghi chú |
|---|---|---|---|---|---|
| 1 | Đơn vị | `DonVi` | ✓ | ✓ | |
| 2 | Tòa nhà | `ToaNha`, `TienNghiToa` | ✓ | ✓ | trạng thái tòa qua action |
| 3 | Tầng | `Tang` | ✓ | ✓ | |
| 4 | Loại phòng | `LoaiPhong`, `TienNghiLoaiPhong` | ✓ | ✓ | |
| 5–6 | Phòng, giường | `Phong`, `Giuong` | ✓ | ✓ | tạo phòng sinh giường theo loại |
| 7 | Tài sản | `LoaiTaiSan`, `TaiSanPhong` | ✓ | ✓ | biên bản không qua Excel |
| 8–19 | Danh mục | một sheet / danh mục | ✓ | ✓ | |
| 20 | Sinh viên | `SinhVien`, `NguoiGiamHo` | ✓ (Admin) | ✓ | 🔒 theo quyền; tạo tài khoản Keycloak như luồng import hiện có |
| 21 | Cán bộ | `CanBo` | ✓ (Admin) | ✓ | mật khẩu tạm không ghi vào tệp kết quả |
| 22 | Đợt đăng ký | `DotDangKy`, `ChiTieu`, `DoiTuong` | ✓ | ✓ (trước mở) | |
| 23 | Đơn đăng ký | `DonDangKy` | ✓ (`DRAFT`) | ✓ (`DRAFT`) | `NOP_NGAY` gọi action nộp |
| 24 | Hợp đồng | `HopDong` | ✓ (`PENDING`) | ✓ (`PENDING`) | |
| 25–27 | Gia hạn, chuyển phòng, hè | `GiaHan`, `ChuyenPhong`, `LuaChonHe` | ✓ | ✓ (chờ duyệt) | duyệt qua action |
| 28 | Gửi đồ | `PhieuGuiDo`, `VatPham` | ✓ | ✓ (chưa trả) | |
| 29 | Chỉ số điện nước | `ChiSoDienNuoc` | ✓ | ✓ (chưa lập HĐ) | mẫu điền sẵn chỉ số đầu |
| 30 | Đơn giá | `DonGia`, `BacThang` | ✓ | ✓ (tương lai) | |
| 31 | Hóa đơn | `HoaDon`, `ChiTietHoaDon`, `XacNhanThanhToan` | ✓ | ✓ (`UNPAID`) | xác nhận = action |
| 32 | Cọc | `GiaoDichCoc` | ✓ (thu/bổ sung) | — | sổ chỉ ghi thêm |
| 33 | Sửa chữa | `SuaChua` | ✓ (`NEW`) | ✓ (`NEW`) | |
| 34 | Thông báo | `ThongBao` | ✓ | ✓ (chưa đăng) | |
| — | Audit, lịch sử chỗ ở, ma trận quyền | – | — | — | chỉ **Xuất**; nút Nhập hiển thị nhưng vô hiệu kèm chú thích “Dữ liệu chỉ đọc” để giữ bố cục thống nhất |

---

# Phần VII – Mã lỗi, hằng số, migration, kiểm thử

## VII.1. Mã lỗi mới

Thêm cùng lúc vào `DmsConstants.ErrorCode`, `API-Spec-DMS.md` mục 5 và `frontend/src/utils/errorMessages.ts`. Mã đã có (`VALIDATION_FAILED`, `NOT_FOUND`, `DUPLICATE_VALUE`, `RESOURCE_IN_USE`, `GENDER_MISMATCH`, `PERIOD_NOT_OPEN`, `UNIT_NOT_IN_PERIOD`, `CONTRACT_PERIOD_OVERLAP`, `CONTRACT_NOT_ACTIVE`, `REQUEST_PENDING_EXISTS`, `TRANSFER_CROSS_UNIT`, `DEPOSIT_INVALID`, `RATE_ALREADY_EFFECTIVE`, `IMPORT_FILE_INVALID`…) được dùng lại, không tạo bản sao.

| Mã | HTTP | Mục | Thông điệp gợi ý (tiếng Việt, ở frontend) |
|---|---|---|---|
| `USER_UNIT_MISMATCH` | 400 | 1, 2, 19, 21 | Cán bộ được chọn không thuộc đơn vị này. |
| `FLOOR_IN_USE` | 409 | 2, 3 | Tầng đã có phòng, không thể giảm số tầng/xóa. |
| `FLOOR_OUT_OF_RANGE` | 400 | 3, 5 | Tầng vượt quá số tầng của tòa. |
| `ACADEMIC_YEAR_OVERLAP` | 409 | 14 | Khoảng thời gian trùng năm học khác. |
| `PRIORITY_EVIDENCE_REQUIRED` | 400 | 18, 23 | Diện ưu tiên này bắt buộc tải minh chứng. |
| `CITIZEN_ID_EXISTS` | 409 | 20 | Số CCCD đã thuộc sinh viên khác. |
| `STUDENT_NOT_STUDYING` | 409 | 20, 23 | Sinh viên không ở trạng thái đang học. |
| `PROFILE_INCOMPLETE` | 409 | 20, 23 | Hồ sơ cá nhân còn thiếu thông tin bắt buộc (`details.fields`). |
| `NOT_ELIGIBLE_FOR_PERIOD` | 409 | 22, 23 | Sinh viên không thuộc đối tượng của đợt đăng ký. |
| `RULES_NOT_ACCEPTED` | 400 | 23 | Cần đồng ý nội quy trước khi nộp đơn. |
| `PAYMENT_AMOUNT_MISMATCH` | 409 | 31 | Số tiền nộp khác số phải thu. |
| `IMPORT_DUPLICATE_ROW` | 400 | VI | Mã bị lặp trong cùng tệp. |
| `IMPORT_TOKEN_EXPIRED` | 409 | VI | Phiên kiểm tra đã hết hạn, hãy kiểm tra lại tệp. |
| `INVOICE_NOT_VOIDABLE` | 409 | 31 | Hóa đơn đã thanh toán hoặc đã điều chỉnh, không thể hủy (chỉ khi chốt trạng thái `VOID`). |

HTTP dùng `HttpStatus`/factory của `BusinessException`, không viết số trực tiếp. Lỗi ràng buộc CSDL mới phải thêm vào bảng ánh xạ constraint → `errorCode` (ví dụ `ux_student_citizen_id` → `CITIZEN_ID_EXISTS`, `ux_invoice_transaction_ref` → `DUPLICATE_VALUE`).

## VII.2. Quyền mới

Thêm vào `DmsConstants.Permission`, `PermissionCatalog` (màn, nhóm, nhãn, thứ tự), `frontend/src/constants/authorization.ts` và migration seed `role_permission_matrix` (ADMIN tự có toàn bộ; STAFF/TECHNICIAN/STUDENT cấp mặc định như cột cuối, Admin đổi được trên `/admin/role-permissions`).

| Mã quyền | Ý nghĩa | Cấp mặc định đề xuất |
|---|---|---|
| `BUILDING_STATUS_UPDATE` | đổi trạng thái tòa (sửa chữa/đóng) | ADMIN |
| `ASSET_READ`, `ASSET_CREATE`, `ASSET_UPDATE`, `ASSET_DELETE` | loại tài sản + tài sản phòng | STAFF (đủ 4), TECHNICIAN (READ) |
| `CONDITION_REPORT_READ`, `CONDITION_REPORT_CREATE` | biên bản nhận/trả phòng | STAFF |
| `CONDITION_REPORT_CONFIRM` | SV xác nhận biên bản của mình | STUDENT |
| `STUDENT_SENSITIVE_READ` | xem CCCD, BHYT, nhóm máu, ghi chú sức khỏe không che | ADMIN; STAFF tùy nhóm chốt |
| `MAJOR_*`, `ETHNICITY_*` | danh mục mới (READ/CREATE/UPDATE/DELETE) | STAFF đọc; ADMIN ghi |

Không thêm danh sách quyền runtime trong code; quyền hiệu lực luôn đọc từ `role_permission_matrix`.

## VII.3. Tham số cấu hình mới (`system_config`)

Hiện có: `contract.activation_days`, `contract.expiring_days`, `deposit.topup_due_days`, `invoice.due_days`, `maintenance.autoclose_days`. Thêm bằng migration seed (ConfigService chỉ sửa key đã tồn tại):

| Key | Mặc định | Dùng ở |
|---|---|---|
| `condition_report.confirm_hours` | 48 | 7.3 – hạn SV xác nhận biên bản |
| `student.email_domain` | `e.tlu.edu.vn` (nhóm xác nhận) | 20.2 – kiểm email trường |
| `contract.terms_version` | `2026.1` | 24 – phiên bản điều khoản chụp vào hợp đồng |
| `storage.pickup_days` | 14 | 28 – hạn nhận lại đồ |
| `utility.photo_required` | `false` | 29 |
| `utility.anomaly_ratio` | `2.0` | 29 – ngưỡng cảnh báo tiêu thụ |
| `maintenance.sla_hours.LOW/MEDIUM/HIGH` | 72 / 24 / 4 | 17, 33 |
| `import.token_minutes` | 30 | VI.2 |
| `import.max_rows` | 2000 | VI.2 |

## VII.4. Kế hoạch migration

Không sửa V1–V15. Mỗi đợt là một PR, gồm migration + cập nhật `DB-Design-DMS.md` + Constants + DTO + `resources.ts` + mẫu Excel + kiểm thử.

| Migration | Nội dung | Mục |
|---|---|---|
| `V16__facility_full_fields.sql` | cột mới `management_unit`, `building`, `building_floor`, `room_type`, `room`, `bed`; bảng `building_amenity`, `room_type_photo`; mở rộng CHECK trạng thái tòa | 1–6 |
| `V17__room_asset_condition_report.sql` | `asset_type`, `room_asset`, `room_condition_report`, `room_condition_item`, `room_condition_photo`; quyền ASSET/CONDITION_REPORT | 7 |
| `V18__catalog_full_fields.sql` | cột mới các danh mục; bảng `major`, `ethnicity` + seed 54 dân tộc; `semester.code`, `semester.academic_year_id` + điền dữ liệu | 8–19 |
| `V19__student_staff_profile.sql` | cột hồ sơ SV/cán bộ; `student_guardian` + chuyển `emergency_contact`; `staff_building`, `technician_skill`; unique CCCD; quyền `STUDENT_SENSITIVE_READ` | 20–21 |
| `V20__registration_stay_fields.sql` | cột mới mục 22–28; `registration_period_cohort`, `storage_item`; sinh `code` cho bản ghi cũ | 22–28 |
| `V21__finance_ops_notification_fields.sql` | cột mới mục 29–34; `utility_rate_tier`, `maintenance_material`, `notification_attachment`; cột `phase` ảnh sửa chữa | 29–34 |
| `V22__import_session_config.sql` | `import_session`; các key `system_config` ở VII.3 | VI |

**Phác thảo SQL (minh họa V16, phần tòa nhà):**

```sql
ALTER TABLE building
    ADD COLUMN floor_count      SMALLINT,
    ADD COLUMN has_ground_floor BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN gender_policy    VARCHAR(10),
    ADD COLUMN address          VARCHAR(300),
    ADD COLUMN manager_user_id  BIGINT REFERENCES app_user (id),
    ADD COLUMN note             VARCHAR(1000);

UPDATE building b
SET floor_count = COALESCE((SELECT MAX(f.floor) FROM building_floor f WHERE f.building_id = b.id), 1);

ALTER TABLE building
    ALTER COLUMN floor_count SET NOT NULL,
    ADD CONSTRAINT ck_building_floor_count CHECK (floor_count BETWEEN 1 AND 50),
    ADD CONSTRAINT ck_building_gender_policy CHECK (gender_policy IN ('M', 'F', 'MIXED'));
```

Quy tắc chung cho migration: cột mới **bắt buộc** thì thêm nullable → điền dữ liệu cũ → `SET NOT NULL`; mã sinh tự động cho bản ghi cũ dùng tiền tố + `id` để không trùng; tên cột/bảng ghi đúng như `information_schema` hiện tại (ví dụ tầng là `building_floor.floor`, không phải `floor_no`; đối chiếu lại trước khi viết migration thật).

## VII.5. Danh sách kiểm thử

Theo CLAUDE.md (kiểm từng nhánh quyền, scope, ID mất, partial update, paging/count, validation) cho **mỗi** tài nguyên được thêm trường:

- [ ] **Tạo** thiếu trường B → 400 `VALIDATION_FAILED` với `details.field` đúng tên JSON.
- [ ] **Sửa từng phần**: gửi một trường, các trường mới khác giữ nguyên (đặc biệt BOOLEAN `false`, số `0`, ngày).
- [ ] Trường bất biến (cột “—”) gửi giá trị khác → 400, không đổi ngầm.
- [ ] Kiểu sai, field ngoài hợp đồng, phần tử null trong danh sách con → 400.
- [ ] Ràng buộc CSDL mới ánh xạ đúng `errorCode` (unique CCCD, unique mã giao dịch, CHECK enum).
- [ ] Scope: Staff đơn vị khác đọc/sửa → 404; STUDENT chỉ `/me`.
- [ ] 🔒 Không có `STUDENT_SENSITIVE_READ` → giá trị bị che trong chi tiết, danh sách và **tệp xuất Excel**; audit ghi `***`.
- [ ] `search-by-page` lọc theo trường mới, đếm tổng đúng với bộ lọc; sort chỉ cột cho phép.
- [ ] Excel: mẫu tải về có `DanhMuc` + data validation; validate báo đúng dòng/cột; commit `ALL_OR_NOTHING` rollback toàn bộ khi một dòng lỗi phát sinh lúc ghi; `VALID_ROWS` ghi đủ dòng hợp lệ; token hết hạn / người khác dùng token → từ chối; nhập lại tệp vừa xuất không sinh thay đổi (idempotent).
- [ ] Form React: bấm Sửa nạp đủ trường mới trước khi chỉnh; lỗi trường hiển thị đúng ô; mobile một cột.
- [ ] Testcontainers: migration V16–V22 chạy sạch trên CSDL đã có dữ liệu DataTest (cập nhật số bảng trong `DmsApplicationTests`).

## VII.6. Tài liệu phải cập nhật khi chốt

Theo thứ tự ưu tiên, mỗi tài liệu tăng phiên bản và ghi nhật ký thay đổi:

1. `Docs/01 Requirements/SRS-DMS.md` – FR bổ sung (tài sản & biên bản, hồ sơ SV đầy đủ, người giám hộ, Excel 2 bước), bảng trạng thái mục 10 (trạng thái tòa, `VOID` hóa đơn nếu chốt, mở lại ticket), dữ liệu nhạy cảm (NFR bảo mật theo Nghị định 13/2023/NĐ-CP).
2. `Docs/02 External Design/Database Design/DB-Design-DMS.md` – bảng/cột/ràng buộc mới, ánh xạ constraint → `errorCode`, ERD.
3. `Docs/02 External Design/API Specification/API-Spec-DMS.md` – DTO mới, endpoint import/export, mã lỗi mục 5, `allowedActions` mới.
4. `Docs/02 External Design/Screen Design/DMS-UI-UX-Design.md` – cột danh sách, nhóm form, tab chi tiết, luồng Excel 3 bước.
5. `DMS/backend/README.md` – phạm vi đã làm.

## VII.7. Nguồn tham khảo

- Hệ thống quản lý KTX/nhà ở sinh viên phổ biến (StarRez, RMS Mercury, Adirondack THDS) – cấu trúc hồ sơ cư trú, biên bản tình trạng phòng (room condition report), quản lý tài sản phòng, lịch sử chỗ ở.
- Danh mục ngành đào tạo của Bộ GD&ĐT (Thông tư 09/2022/TT-BGDĐT) – mã ngành 7 chữ số.
- Danh mục 54 dân tộc của Tổng cục Thống kê; Nghị định 57/2017/NĐ-CP về dân tộc rất ít người.
- Nghị quyết 202/2025/QH15 và mô hình chính quyền địa phương 2 cấp từ 01/07/2025 – cấp hành chính tỉnh/xã.
- Quy chế tuyển sinh – khu vực ưu tiên KV1, KV2-NT, KV2, KV3.
- Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân – dữ liệu nhạy cảm (sức khỏe, CCCD).
- Thông tư 27/2009/TT-BGDĐT (quy chế ngoại trú, nội trú HSSV) – thông tin quản lý sinh viên nội trú.

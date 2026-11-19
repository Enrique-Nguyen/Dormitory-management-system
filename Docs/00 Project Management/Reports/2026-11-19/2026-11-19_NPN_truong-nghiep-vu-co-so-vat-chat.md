# Báo cáo: Trường nghiệp vụ đầy đủ cho cơ sở vật chất (đợt 1) và cải thiện giao diện chung

> Ngày hoàn thành: 19/11/2026
> Thực hiện: TV2 Nguyễn Phú Nguyên (cùng Claude Code)
> Phạm vi: backend module `account`, `room`, `common`; frontend `features/crud`, `features/room`, `pages`, `app/router`, `components/ui`
> Căn cứ: `Docs/05 Reviews/dac-ta-truong-nghiep-vu-day-du.md` Phần II; `Docs/05 Reviews/danh-gia-tong-the-va-de-xuat-cai-thien-DMS.md` mục 1, 3, 4, 5; SRS FR-ROOM-01..06
> Trạng thái: Hoàn thành một phần

---

## 1. Mục tiêu

Bổ sung các trường nghiệp vụ thực tế cho đơn vị quản lý, tòa nhà, tầng, loại phòng, phòng và giường (trước đây chỉ có mã, tên và một vài trường), đồng thời làm các cải thiện giao diện chung có lợi cho mọi màn: hiện tên thay ID, thẻ trên mobile, tách chunk theo route. Các đợt sau (danh mục, hồ sơ sinh viên/nhân sự, đăng ký, tài chính, nhập Excel phía server) chưa nằm trong báo cáo này.

## 2. Kết quả

| Hạng mục | File | Nội dung |
|---|---|---|
| Migration | `V16__facility_full_fields.sql`, `V17__building_ground_floor_backfill.sql` | Thêm cột cho `management_unit`, `building`, `building_floor`, `room_type`, `room`, `bed`; CHECK, unique số công tơ/đồng hồ, sequence mã loại phòng; backfill số tầng, đối tượng tòa, mã loại phòng và cờ tầng trệt. |
| Hằng số, lỗi | `DmsConstants`, `GlobalExceptionHandler` | Nhóm `Facility` (pattern điện thoại/email/hướng/kiểu giường/vị trí giường), mã lỗi `FLOOR_IN_USE`, `FLOOR_OUT_OF_RANGE`, `USER_UNIT_MISMATCH`; ánh xạ ràng buộc mới sang `DUPLICATE_VALUE`. |
| Dịch vụ | `ManagementUnitServiceImpl`, `BuildingServiceImpl`, `FloorServiceImpl`, `RoomTypeServiceImpl`, `RoomServiceImpl`, `AccountReferenceServiceImpl` | Sửa từng phần (`mergeText`/`mergeValue`), kiểm tra trưởng đơn vị/cán bộ tòa là STAFF cùng đơn vị, giảm số tầng, đổi đơn vị khi còn người ở, giới tính tòa/tầng/phòng, phạm vi tầng; trả tên đơn vị, người phụ trách, số phòng/sức chứa/đang ở tính khi đọc. |
| Màn hình CRUD | `features/crud/resources.ts`, `types.ts`, `api.ts`, `pages/CrudPage.tsx` | Form chia nhóm (Thông tin chung, Quy mô, Tài chính, Vận hành, Liên hệ, Quy cách, Đồng hồ điện nước…), gợi ý trường, kiểu trường `time`; cột Tòa, Loại phòng, Đang ở/Sức chứa, Số phòng; cột khóa ngoại hiện tên tham chiếu thay ID thô. |
| Chi tiết phòng | `pages/staff/RoomDetailPage.tsx`, `features/room/BedCrudPanel.tsx`, `services/api/room.ts` | Thẻ "Thông tin phòng"; giường có vị trí, mã tài sản, ghi chú. |
| Nhãn, thông báo lỗi | `constants/statusConfig.ts`, `utils/errorMessages.ts` | Nhãn đối tượng tòa, kiểu/vị trí giường, hướng phòng; thông báo cho 3 mã lỗi mới; `GENDER_MISMATCH` có câu riêng khi không gắn với sinh viên. |
| Mobile | `components/ui/ResponsiveTable.tsx`, `pages/CrudPage.tsx` | Danh sách CRUD dùng `ResponsiveTable`: thẻ dưới 640 px, bảng có chọn nhiều dòng trên desktop. |
| Tách chunk | `app/router/lazyPage.tsx`, 4 file route | Trang tải lười theo route; chunk khởi động giảm từ ~1,66 MB còn ~115 KB, `exceljs` chỉ tải khi dùng Excel. |
| Tài liệu | `DB-Design-DMS.md` v0.16, `API-Spec-DMS.md` v0.27 | Mô tả cột V16–V17, body/response mới và 3 mã lỗi. |

## 3. Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| `./mvnw verify` (Testcontainers) | 530 test, 529 đạt; 1 lỗi ở `CatalogApiIntegrationTest.floorZeroIsSelectableAndUsedFloorsCannotBeDeleted` do quy tắc tầng trệt mới. Đã sửa fixture và thêm kiểm tra nhánh từ chối; chạy lại riêng lớp này: 9/9 đạt. |
| Flyway trên CSDL dev | V16 và V17 áp thành công, dữ liệu DataTest giữ nguyên. |
| `npm run lint` | 0 lỗi, 3 cảnh báo có từ trước (`excel.ts`, `NotificationInboxPage.tsx`, `CrudPage.tsx`). |
| `npm run build` | Thành công. |

**Chưa kiểm tra:** chưa chạy lại toàn bộ `./mvnw verify` sau khi sửa test; chưa xem giao diện trên trình duyệt vì phiên đăng nhập Keycloak hết hạn và cần người dùng đăng nhập lại.

## 4. Điểm lệch so với thiết kế và quyết định cần xác nhận

| Nội dung | Quyết định | Lý do |
|---|---|---|
| Kế hoạch đánh số migration trong đặc tả (V17 tài sản/biên bản) | V17 dùng cho backfill tầng trệt; các đợt sau lùi một số | Không sửa V16 đã áp dụng. |
| Trưởng đơn vị | Chỉ chọn khi sửa đơn vị | Lúc tạo đơn vị chưa có nhân viên thuộc đơn vị. |
| Chọn nhiều dòng trên mobile | Không có ở dạng thẻ | Thao tác từng thẻ vẫn đủ; xóa hàng loạt dùng trên desktop. |

## 5. Việc còn lại

| Việc | Phụ trách |
|---|---|
| Kiểm tra giao diện các màn đơn vị, tòa, tầng, loại phòng, phòng trên trình duyệt | TV2 |
| Đợt 2–4 theo đặc tả trường nghiệp vụ (danh mục, hồ sơ, đăng ký, tài chính, vận hành), bảng tài sản/biên bản | Nhóm |
| Nhập Excel kiểm tra/ghi phía server, Dockerfile/nginx/compose prod, Vitest/Playwright, tách `CrudPage.tsx` | Nhóm |
| Cập nhật SRS, UI-UX và README backend theo trường mới | TV2 |

## 6. Tệp liên quan

- Sửa: các file module `account`, `room`, `common` nêu ở mục 2; `CatalogApiIntegrationTest.java`, `RoomCatalogServiceTest.java`, `CrudSearchFilterTest.java`; frontend `resources.ts`, `types.ts`, `api.ts`, `CrudPage.tsx`, `ResponsiveTable.tsx`, `RoomDetailPage.tsx`, `BedCrudPanel.tsx`, `room.ts`, `statusConfig.ts`, `errorMessages.ts`, 4 file route; `DB-Design-DMS.md`, `API-Spec-DMS.md`, `dac-ta-truong-nghiep-vu-day-du.md`.
- Mới: `V16__facility_full_fields.sql`, `V17__building_ground_floor_backfill.sql`, `app/router/lazyPage.tsx`, báo cáo này.
- Xóa hoặc chuyển: Không có.

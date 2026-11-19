# Báo cáo: Trường nghiệp vụ đầy đủ cho danh mục (đợt 2)

> Ngày hoàn thành: 19/11/2026
> Thực hiện: TV2 Nguyễn Phú Nguyên (cùng Claude Code)
> Phạm vi: backend module `account`, `room`, `registration`, `contract`, `common`; frontend `features/crud`, `constants`, `utils`
> Căn cứ: `Docs/05 Reviews/dac-ta-truong-nghiep-vu-day-du.md` mục 8–19; SRS FR-ROOM-11, FR-REG-05
> Trạng thái: Hoàn thành một phần

---

## 1. Mục tiêu

Bổ sung các trường nghiệp vụ thực tế cho các danh mục trước đây chỉ có mã, tên và trạng thái: trường, khoa, khóa, quê quán, năm học, học kỳ, tiện nghi, hạng mục sửa chữa, diện ưu tiên và vị trí kho. Nhập Excel vẫn dùng chung khung CRUD nên tự nhận các cột mới.

## 2. Kết quả

| Hạng mục | File | Nội dung |
|---|---|---|
| Migration | `V18__catalog_full_fields.sql` | Thêm cột cho 10 bảng danh mục. Ràng buộc mới: tối đa một trường chủ quản; năm nhập học/tốt nghiệp; cấp hành chính hai cấp tỉnh–xã; khu vực ưu tiên; năm học không chồng lấn (EXCLUDE gist); mã học kỳ là cột sinh; phạm vi áp dụng tiện nghi; mức độ/SLA/kỹ năng hạng mục sửa chữa; thứ tự và quy tắc tự nhận diện của diện ưu tiên; sức chứa vị trí kho. Backfill cấp xã cho quê quán có cấp cha, ngày năm học theo khung 01/09–31/08, mô tả diện ưu tiên lấy từ tên. |
| Hằng số, lỗi | `DmsConstants`, `GlobalExceptionHandler` | Nhóm `CatalogValue` (pattern và giới hạn); mã lỗi `ACADEMIC_YEAR_OVERLAP`; thông điệp validation mới; ánh xạ `ux_school_home_school` → `DUPLICATE_VALUE`, `ex_academic_year_period` → `ACADEMIC_YEAR_OVERLAP`. |
| Danh mục chung | `CatalogRequest`, `CatalogService`, `CatalogServiceImpl` | Mỗi loại chỉ nhận nhóm trường của mình (trường thuộc loại khác bị từ chối); null giữ giá trị cũ, chuỗi rỗng xóa trường văn bản; kiểm tra năm khóa, cấp hành chính và cấp cha, năm bắt đầu của năm học khớp mã; thêm `requireWithinAcademicYear` cho học kỳ. |
| Học kỳ | `Semester`, `SemesterRequest`, `SemesterResponse`, `SemesterServiceImpl` | Trả mã học kỳ sinh tự động, thêm hạn nộp tiền phòng và ghi chú; ngày học kỳ phải nằm trong năm học khi năm học đã có ngày. |
| Diện ưu tiên | `PriorityCategory`, `PriorityCategoryRequest`, `PriorityCategoryResponse`, `PriorityCategoryServiceImpl` | Mô tả (bắt buộc khi tạo), minh chứng, căn cứ pháp lý, thứ tự hiển thị, quy tắc tự nhận diện, ghi chú. |
| Vị trí kho | `StorageLocationDtos`, `StorageLocationServiceImpl`, `RoomReferenceService(Impl)` | Tòa, vị trí cụ thể, sức chứa, cán bộ phụ trách, ghi chú; tòa và cán bộ phải cùng đơn vị; trả tên đơn vị, tòa, người phụ trách và số kiện đang giữ (tính khi đọc). Thêm `getBuildingNames` vào dịch vụ tham chiếu của module `room`. |
| Màn hình CRUD | `features/crud/resources.ts` | Form chia nhóm (Thông tin chung, Liên hệ, Thời gian đào tạo, Ưu tiên, Xử lý, Minh chứng, Vị trí, Phụ trách…), gợi ý trường; cột mới cho từng danh mục; quê quán chỉ hiện "Thuộc tỉnh/thành phố" khi chọn cấp xã; tòa và cán bộ của vị trí kho lọc theo đơn vị đã chọn. |
| Nhãn, thông báo lỗi | `constants/statusConfig.ts`, `utils/errorMessages.ts` | Nhãn loại học kỳ, cấp hành chính, khu vực ưu tiên, phạm vi tiện nghi, mức độ, kỹ năng sửa chữa, quy tắc tự nhận diện; thông báo cho `ACADEMIC_YEAR_OVERLAP`. |
| Kiểm thử | `CatalogServiceTest` | Cập nhật kỳ vọng câu UPDATE (giữ năm nhập học/ghi chú cũ khi sửa từng phần); thêm 3 test: từ chối trường của loại khác, năm tốt nghiệp không sau năm nhập học, năm bắt đầu lệch mã năm học. |
| Tài liệu | `DB-Design-DMS.md` v0.17, `API-Spec-DMS.md` v0.28 | Mục V18, body/response mới và mã lỗi `ACADEMIC_YEAR_OVERLAP`. |

## 3. Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| `./mvnw -o verify` (Testcontainers) | 533/533 test đạt. Bước `repackage` lỗi do backend dev đang chạy giữ file jar; sau khi dừng backend, `package` chạy thành công. Dự án không cấu hình ngưỡng JaCoCo nên không có bước kiểm tra độ phủ. |
| Lần chạy `verify` đầu | `CatalogMigrationIntegrationTest` báo backfill năm học ban đầu (nới theo học kỳ cũ) vi phạm `ck_academic_year_start_year`; đã đổi backfill sang khung cố định 01/09–31/08 trước khi V18 áp lên CSDL dev, chạy lại đạt. |
| Flyway trên CSDL dev | V18 áp thành công. Năm học 2025-2026, 2026-2027 có ngày; mã học kỳ sinh đúng (`HK1-2026`, `HK2-2026`, `HE-2026`, `HE-2025`). |
| `npm run lint` | 0 lỗi, 3 cảnh báo có từ trước (`excel.ts`, `NotificationInboxPage.tsx`, `CrudPage.tsx`). |
| `npm run build` (gồm `tsc -b`) | Thành công. |

**Chưa kiểm tra:** chưa xem các màn danh mục và màn đợt 1 trên trình duyệt vì cần người dùng đăng nhập Keycloak; chưa thử nhập Excel với các cột mới.

## 4. Điểm lệch so với thiết kế và quyết định cần xác nhận

| Nội dung | Quyết định | Lý do |
|---|---|---|
| Đánh số migration | Danh mục dùng V18 thay vì V17 trong đặc tả | V17 đã dùng cho backfill tầng trệt ở đợt 1. |
| `semester.academic_year_id` | Không thêm | Học kỳ đã có khóa ngoại theo mã tới `academic_year.code` từ V10; thêm cột ID sẽ trùng dữ liệu. |
| Ngày năm học, năm nhập học của khóa, mô tả diện ưu tiên | Nullable ở CSDL, bắt buộc khi tạo ở Service | Dữ liệu cũ không suy ra được đầy đủ; tránh chặn migration và fixture hiện có. |
| Backfill ngày năm học | Khung cố định 01/09–31/08, không nới theo học kỳ cũ | Nới theo học kỳ có thể làm lệch năm bắt đầu hoặc chồng lấn năm liền kề. Học kỳ cũ nằm ngoài khung chỉ bị kiểm tra khi sửa ngày hoặc đổi năm học. |
| Danh mục ngành, dân tộc (54 dân tộc) và quyền `MAJOR_*`/`ETHNICITY_*` | Chưa làm | Cần bảng và quyền mới; để đợt hồ sơ sinh viên. |
| Mã lỗi `PRIORITY_EVIDENCE_REQUIRED` | Chưa áp dụng | Cờ `evidence_mandatory` đã lưu; kiểm tra khi nộp đơn thuộc đợt đăng ký. |

## 5. Việc còn lại

| Việc | Phụ trách |
|---|---|
| Kiểm tra giao diện danh mục (đợt 2) và cơ sở vật chất (đợt 1) trên trình duyệt, cả dạng thẻ trên mobile | TV2 |
| Thử nhập Excel các danh mục với cột mới | TV2 |
| Danh mục ngành, dân tộc; kiểm tra minh chứng bắt buộc khi nộp đơn | Nhóm |
| Đợt 3–4 theo đặc tả (hồ sơ sinh viên/nhân sự, đăng ký, tài chính, vận hành), bảng tài sản/biên bản | Nhóm |
| Cập nhật SRS, UI-UX và README backend theo trường mới | TV2 |

## 6. Tệp liên quan

- Sửa: `DmsConstants.java`, `GlobalExceptionHandler.java`, `CatalogRequest.java`, `CatalogService.java`, `CatalogServiceImpl.java`, `Semester.java`, `SemesterRequest.java`, `SemesterResponse.java`, `SemesterServiceImpl.java`, `PriorityCategory.java`, `PriorityCategoryRequest.java`, `PriorityCategoryResponse.java`, `PriorityCategoryServiceImpl.java`, `StorageLocationDtos.java`, `StorageLocationServiceImpl.java`, `RoomReferenceService.java`, `RoomReferenceServiceImpl.java`, `CatalogServiceTest.java`; frontend `resources.ts`, `statusConfig.ts`, `errorMessages.ts`; `DB-Design-DMS.md`, `API-Spec-DMS.md`.
- Mới: `V18__catalog_full_fields.sql`, báo cáo này.
- Xóa hoặc chuyển: Không có.

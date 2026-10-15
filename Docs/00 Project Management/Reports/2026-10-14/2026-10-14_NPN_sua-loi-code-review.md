# Báo cáo: Sửa lỗi từ code review nhánh `develop-quyennnv`

> Ngày hoàn thành: 14/10/2026
> Thực hiện: TV2 Nguyễn Phú Nguyên (cùng Claude Code)
> Phạm vi: `DMS/backend`, `DMS/frontend` (CrudPage, client Axios, thông báo lỗi), `Docs/02 External Design` (API-Spec, DB-Design). Nhánh `improve/develop-quyennv` tạo từ `develop-quyennnv`.
> Căn cứ: kết quả `/code-review` nhánh `develop-quyennnv`; `CLAUDE.md` (quy tắc service/impl, mã lỗi, phạm vi theo đơn vị); `API-Spec-DMS.md` mục 5; `DB-Design-DMS.md` mục 4
> Trạng thái: Hoàn thành một phần

---

## 1. Mục tiêu

Khắc phục các lỗi chức năng, ánh xạ mã lỗi và hiệu năng mà code review tìm ra. Ngoài phạm vi (đã thống nhất): chuyển SQL thô xuyên module sang Service/Repository đúng module, tách nhỏ CrudPage, viết test Mockito cho Service/Policy, `@PreAuthorize` cho mọi role.

## 2. Kết quả

| Hạng mục | File chính | Nội dung |
|---|---|---|
| Thông báo FLOOR không sửa được | `NotificationCrudServiceImpl` | So sánh phạm vi theo giá trị số, không dùng `Objects.equals` giữa `Short` và `Integer` |
| PATCH ticket trả dữ liệu cũ | `MaintenanceCrudServiceImpl` | Kiểm tra chủ ticket/trạng thái bằng JDBC để không nạp entity vào cache cấp 1; người không phải chủ trả 404 |
| Hủy đơn APPROVED để lại hợp đồng mồ côi | `RoomApplicationPolicy`, `RoomApplicationServiceImpl`, `ContractService.hasLiveContractForApplication` | Chặn hủy khi còn hợp đồng PENDING/ACTIVE (409 `APPLICATION_HAS_CONTRACT`), ẩn `cancel` trong `allowedActions`; Policy dùng `Objects.equals` nên không NPE |
| Staff ghi đè đợt dùng chung | `RegistrationPeriodServiceImpl` | `requireWritablePeriodUnits` dùng chung cho sửa và xóa; đợt chung với đơn vị khác trả 404 |
| PATCH sinh viên xóa trường không gửi | `StudentManagementController.StudentPatch`, `StudentManagementServiceImpl` | DTO PATCH riêng, trường không gửi giữ giá trị cũ, khóa dòng `app_user`; gom kiểm tra phạm vi vào `requireOwnStudent` |
| Chỉ số điện nước chèn trước tháng sau | `UtilityReadingServiceImpl` | Kiểm tra `READING_HAS_FOLLOWING_MONTH` cả khi tạo mới |
| Thông báo mở đợt hè sai/trùng | `NotificationServiceImpl` | Chỉ xét chỗ ở hiện hành, một truy vấn cho mọi đơn vị, mỗi sinh viên một thông báo |
| Phạm vi hóa đơn phí phòng | `InvoiceRepository`, `InvoiceServiceImpl` | Phạm vi theo học kỳ của hóa đơn (lịch sử ở trong học kỳ hoặc đơn nguyện vọng của đợt thuộc học kỳ); quyền ghi qua `isInStaffUnit` |
| Số giường lệch loại phòng | `RoomServiceImpl`, `RoomTypeServiceImpl` | Đổi loại phòng thì đồng bộ giường (`syncBeds`); đổi `bedCount` của loại phòng đang dùng trả 409 `RESOURCE_IN_USE` |
| Lỗi framework thành 500 | `GlobalExceptionHandler` | 405, 415, 404, 400 cho lỗi MVC |
| Ràng buộc CSDL luôn là `CONCURRENT_UPDATE` | `GlobalExceptionHandler` | Ánh xạ theo tên ràng buộc rồi theo SQLState; thêm `LEADER_ALREADY_ASSIGNED`, `STUDENT_CODE_EXISTS`, `BED_CODE_EXISTS`, `DUPLICATE_VALUE` |
| Thiếu mã lỗi ở UI và tài liệu | `errorMessages.ts`, `API-Spec-DMS.md` v0.5, `DB-Design-DMS.md` v0.4 | Thêm câu tiếng Việt và bảng mã; mã lạ không hiện chuỗi mã thô |
| Truy vấn lặp | `CurrentUserImpl`, `RoomServiceImpl`, `NotificationServiceImpl` | Cache Actor theo request (không có `invalidate()`; ảnh chụp xác thực dùng đến hết request), nạp công suất phòng và giường theo lô |
| CSRF và CrudPage | `services/api/client.ts`, `features/crud/api.ts`, `CrudPage.tsx`, `resources.ts`, `types.ts` | Làm mới CSRF trong interceptor (thử lại một lần); ghi dùng `useApiErrorHandler`; cờ cấu hình thay nhánh theo tên resource; lookup có `staleTime` |
| Mục nhỏ | `LikePattern`, `AuthServiceImpl`, `AccountAccessFilter` | Escape `%`, `_`, `\` trong tìm kiếm; băm giả khi username không tồn tại; tài khoản bị khóa vẫn đăng xuất được |

## 3. Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| `mvn test` với PostgreSQL 16 cục bộ (`-Ddms.test.external-db=true`) | 34 test đạt: `CrudApiIntegrationTest` 28, `CsrfSessionIntegrationTest` 1, `DmsApplicationTests` 1, `ModuleDependencyTest` 4 |
| 10 test hồi quy mới chạy trên mã cũ (`7981779`) | Cả 10 đỏ, xanh sau khi sửa |
| `npm run lint`, `npm run build` | Sạch |

**Chưa kiểm tra:**
- Không chạy được Docker trong môi trường làm việc nên không dùng Testcontainers.
- Chưa có test riêng cho: thông báo đợt hè (listener chạy `REQUIRES_NEW` sau commit nên test có rollback không thấy dữ liệu), phạm vi hóa đơn theo học kỳ, cache Actor và số truy vấn của danh sách phòng.
- Chưa chạy giao diện với backend thật; luồng làm mới CSRF trong interceptor mới được kiểm tra qua kiểu và build.
- Chưa chạy `./mvnw verify`/JaCoCo.

## 4. Điểm lệch so với thiết kế và quyết định cần xác nhận

| Nội dung | Quyết định | Lý do |
|---|---|---|
| Hủy đơn APPROVED đã có hợp đồng | Chặn, 409 `APPLICATION_HAS_CONTRACT` (không tự hủy hợp đồng) | Đã chốt với người thực hiện; tránh thay đổi dây chuyền chưa có trong SRS |
| Xử lý ghi ở CrudPage | Dùng `useApiErrorHandler` trong khối try/catch hiện có, chưa chuyển sang `useMutation` | Giữ diff nhỏ; chưa tách CrudPage |
| Đổi `bedCount` loại phòng đang dùng | Từ chối 409, không đồng bộ hàng loạt | An toàn hơn; cần xác nhận nếu muốn đồng bộ tự động |
| Phạm vi hóa đơn phí phòng | Theo học kỳ của hóa đơn | Tránh lộ hóa đơn kỳ cũ cho đơn vị cũ; cần SRS xác nhận quy tắc này |
| Mã `STUDENT_HAS_ACTIVE_CONTRACT` | Bổ sung vào API-Spec mục 5.2 | Backend đã phát ra nhưng tài liệu thiếu |

## 5. Việc còn lại

| Việc | Phụ trách |
|---|---|
| Chuyển SQL thô xuyên module (`OverviewController`, các `*CrudServiceImpl`) sang Service/Repository đúng module; bỏ import kiểu DTO từ controller trong service | Chưa phân công |
| Tách `CrudPage` theo resource, chuyển ghi sang `useMutation`, dùng `allowedActions`/`ConfirmDialog`/`statusConfig.ts` | Chưa phân công |
| Test Mockito cho Service và Policy (JaCoCo ≥ 60% lớp Service); thêm test cho các mục ở phần "Chưa kiểm tra" | Chưa phân công |
| `@PreAuthorize` cho vai trò Staff/Student/Technician; từ khóa tìm đơn qua `IN` toàn bộ ID | Chưa phân công |
| Cấp quyền thực thi cho `DMS/backend/mvnw` (hiện không chạy trực tiếp được trên Linux) | Chưa phân công |

## 6. Tệp liên quan

- Sửa: các file backend ở bảng mục 2 và `DMS/backend/README.md`; frontend `CrudPage.tsx`, `features/crud/{api,resources,types}.ts`, `services/api/client.ts`, `utils/errorMessages.ts`; `API-Spec-DMS.md`, `DB-Design-DMS.md`.
- Mới: `common/support/LikePattern.java`; 10 test trong `CrudApiIntegrationTest`; báo cáo này.
- Xóa hoặc chuyển: Không có.

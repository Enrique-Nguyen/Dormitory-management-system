# Báo cáo: Xử lý 10 findings từ code review nhánh `dev/nguyennp`

> Ngày hoàn thành: 15/10/2026
> Thực hiện: TV2 Nguyễn Phú Nguyên (cùng Claude Code)
> Phạm vi: `DMS/backend` (maintenance, room, registration, contract, account, common), `DMS/frontend` (CrudPage, errorMessages), `Docs/` (SRS, API-Spec)
> Căn cứ: kết quả `/code-review` nhánh `dev/nguyennp`; `CLAUDE.md` (quy tắc không hardcode, import đầu file, scope trả 404)
> Trạng thái: Hoàn thành

---

## 1. Kết quả

| # | Nội dung | Xử lý |
|---|---|---|
| 1 | Sửa/hủy ticket của người khác trả 409 `RESOURCE_IN_USE` | `MaintenanceCrudServiceImpl`: ticket không tồn tại hoặc không thuộc người gọi trả 404; ticket không còn `OPEN` trả 409 `INVALID_STATE_TRANSITION` |
| 2 | Mọi lỗi khóa ngoại thành `RESOURCE_IN_USE` | `GlobalExceptionHandler`: `DELETE` giữ 409 `RESOURCE_IN_USE`; ghi khác trả 400 `VALIDATION_FAILED` |
| 3 | Đổi loại phòng ít giường hơn bị `BED_OCCUPIED` khi giường dư có lịch sử | `RoomServiceImpl.syncBeds`: giường có lịch sử chuyển `RETIRED`, giường chưa có lịch sử bị xóa; khi tăng lại thì kích hoạt lại giường `RETIRED` trước khi tạo mã mới |
| 4 | Cache Actor theo request | Giữ nguyên hành vi (có chủ ý: tự thu hồi phiên không làm rollback chính request). Bổ sung JavaDoc ở `CurrentUser.get()`; sửa báo cáo 14/10 bỏ claim `invalidate()` |
| 5 | PATCH sinh viên không xóa được ngày sinh | `StudentPatch` chuyển thành lớp có setter riêng cho `dateOfBirth`: vắng key = giữ, `null` = xóa. Frontend: cờ `clearable` ở trường ngày sinh |
| 6 | Hardcode trạng thái | `ContractStatus.PENDING/ACTIVE`, `BedOperationStatus.*`, `MaintenanceStatus.CANCELLED` |
| 7 | Import inline `java.util.*` | Đưa lên đầu `RoomServiceImpl` |
| 8 | Guard hủy đơn có hợp đồng | Race không có thật vì cả hủy đơn lẫn tạo hợp đồng đều khóa `room_application` `FOR UPDATE`; chỉ bỏ điều kiện `APPROVED` trong `hasLiveContract` và ghi chú cơ chế khóa |
| 9 | Thiếu thông báo tiếng Việt cho 9 mã lỗi | Thêm vào `errorMessages.ts`; đối chiếu bằng script toàn bộ `ErrorCode` với `errorMessages.ts`: không còn mã thiếu. Thêm `IDENTITY_ALREADY_LINKED`, `BOOTSTRAP_USERNAME_IN_USE` vào API-Spec mục 5.2 |
| 10 | Lệch phiên bản tài liệu | `SRS-DMS.md` mục 5.1 trỏ `DB-Design-DMS.md` v0.7. `API-Spec-DMS.md` lên v0.12 |

## 2. Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| `./mvnw test -Ddms.test.external-db=true` (PostgreSQL 16 cục bộ) | 88 test đạt: `CrudApiIntegrationTest` 41, `KeycloakAccountIntegrationTest` 33, `ModuleDependencyTest` 8, `KeycloakJwtDecoderTest` 5, `DmsApplicationTests` 1 |
| Test mới | `maintenanceEditHidesForeignTicketsAndRejectsNonOpenOnes`, `shrinkingRoomTypeRetiresBedsWithHistoryAndGrowingReactivatesThem`, `studentPatchClearsDateOfBirthOnlyWhenNullIsSentExplicitly`, `foreignKeyViolationIsValidationOnWriteAndInUseOnDelete` |
| `npm run lint`, `npm run build` | Sạch |

**Chưa kiểm tra:**
- Không có Docker nên không chạy Testcontainers; chưa chạy `./mvnw verify`/JaCoCo.
- Test khóa ngoại (`foreignKeyViolation...`) mới khẳng định mã lỗi khác `RESOURCE_IN_USE` ở nhánh ghi, vì Service chặn loại phòng không tồn tại trước khi tới ràng buộc CSDL; chưa có test chạm trực tiếp SQLState 23503 khi ghi.
- Chưa có test riêng cho cache Actor của `CurrentUserImpl`.
- Chưa chạy giao diện với backend thật (xóa ngày sinh trên form).

## 3. Điểm lệch so với thiết kế và quyết định

| Nội dung | Quyết định | Lý do |
|---|---|---|
| Giường dư có lịch sử | `RETIRED`, không báo lỗi | Giữ lịch sử, không tính vào sức chứa (`v_room_occupancy.active_beds` chỉ đếm `ACTIVE`) |
| `Optional` cho PATCH | Không dùng | Jackson trả `Optional.empty()` cho cả key vắng mặt lẫn `null`; `Nulls.AS_EMPTY` cũng áp dụng cho key vắng mặt. Dùng setter chỉ chạy khi key có mặt |
| Không thêm `invalidate()` cho `CurrentUser` | Giữ ảnh chụp xác thực theo request | Gọi lại `get()` sau khi tự thu hồi/khóa sẽ ném `SESSION_REVOKED`/`ACCOUNT_LOCKED` giữa transaction |
| Không nâng phiên bản SRS | Chỉ sửa tham chiếu | Nâng SRS sẽ làm SAD và DB-Design (đang trỏ SRS v0.11) lệch theo; sửa tham chiếu không đổi yêu cầu |
| Báo cáo 14/10 | Chỉ sửa claim `invalidate()` | Các mục khác (phiên bản API-Spec v0.5, DB-Design v0.4, CSRF…) phản ánh thời điểm và commit trước, không thuộc diff nhánh này |

## 4. Việc còn lại

| Việc | Phụ trách |
|---|---|
| Test Mockito cho `CurrentUserImpl` (một truy vấn mỗi request) và test chạm trực tiếp SQLState 23503 | Chưa phân công |
| Chạy `./mvnw verify` với Docker để xác nhận JaCoCo ≥ 60% lớp Service | Chưa phân công |

## 5. Tệp liên quan

- Sửa: các file backend/frontend nêu ở mục 1; `CrudApiIntegrationTest.java`; `SRS-DMS.md`; `API-Spec-DMS.md`; báo cáo 14/10.
- Mới: báo cáo này.

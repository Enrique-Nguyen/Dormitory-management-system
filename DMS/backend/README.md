# Khung CRUD và đăng nhập backend DMS

Nhánh triển khai: `develop-quyennnv`. Backend giữ Spring Boot 3.5, Java 17+, PostgreSQL 16, Flyway và `ddl-auto: validate`.

## Cấu trúc và cách đọc mã

Quy tắc không hardcode được ghi tại `../../AGENTS.md` và mục tương ứng trong `../../CLAUDE.md`. `DmsConstants.ErrorCode` là nguồn mã lỗi hiện dùng; HTTP status dùng `HttpStatus`/factory `BusinessException`. `PermissionCatalog`, `PermissionMessage` và `ApiField` giữ danh mục/nhãn/thông báo/tên trường của ma trận; Service Impl xử lý logic và đọc quyền động từ CSDL. Clean verify đạt **108/108 test**, gồm kiểm tra registry đủ 106 quyền và giữ nguyên hợp đồng lỗi/HTTP hiện có.

Mỗi module có `domain` (entity), `repository`, `service` và `api` (Controller, request/response DTO). Toàn bộ `service/<Tên>Service.java` là interface, chỉ khai báo chữ ký hàm và JavaDoc tiếng Việt. Logic nằm trong `service/impl/<Tên>ServiceImpl.java`, dùng `implements <Tên>Service`, constructor injection và `@Service`. Transaction đặt trên Impl; Controller và các module khác gọi qua interface. Các hàm overload cũng được triển khai trong Impl, không dùng `default` chứa thân hàm ở interface.

Các lớp nền dùng chung nằm trong `common/service/impl`: `AbstractCrudServiceImpl` triển khai `CrudService`, `AbstractSqlCrudServiceImpl` chứa helper SQL cho các Impl của hồ sơ khung. Các kiểu record dữ liệu tham chiếu có thể khai báo trong interface, nhưng không chứa logic xử lý. Kiểm tra ArchUnit bảo vệ quy tắc interface/Impl cùng quy tắc phụ thuộc module.

`common/dto` chứa kiểu trao đổi dùng chung. Không đặt DTO dùng chung trong `common/api`: kiểm tra ArchUnit không cho module khác sử dụng Controller/DTO của một module khác. `common/support/PageQuery` kiểm tra page, size và danh sách trường sắp xếp.

`domain` là tên package, không có ý nghĩa đặc biệt với Spring. Các annotation `@Entity`, `@Table`, `@Id` mới quyết định mapping. Có entity và Repository cho cả 33 bảng nghiệp vụ, gồm khóa kép bằng `@IdClass`, JSONB, CHAR(1), NUMERIC và cột tiêu thụ điện nước do CSDL sinh. Tham chiếu chéo module lưu bằng ID, không dựng quan hệ JPA xuyên module.

Tên hàm dùng tiếng Anh rõ nghĩa; JavaDoc và ghi chú quan trọng dùng tiếng Việt. Danh sách dùng `searchByPage`, tạo/sửa dùng một hàm `saveOrUpdate`; giữ tên hành động rõ nghĩa như `getRoomById`, `changeRoomStatus`, `assignRoomLeader`, `submitApplication`, `assignTechnician`, `confirmInvoicePayment`.

## API đã có

- Xác thực mặc định: Keycloak OIDC/PKCE, Spring Security JWT Resource Server; /me trả hồ sơ DMS, logout thu hồi version. Mật khẩu/khóa tạm/đổi mật khẩu thuộc Keycloak. Subject, role, version và scope vẫn kiểm tra tại DMS.
- Tài khoản: Admin quản lý tài khoản và sinh viên; sinh viên được thêm từng hồ sơ bằng form CRUD, trả mật khẩu tạm một lần. Staff tìm kiếm/đọc/sửa và reset mật khẩu sinh viên trong phạm vi mình. Import Excel chưa triển khai. Tài khoản cán bộ dùng khóa thay cho xóa; sinh viên chỉ xóa khi chưa có dữ liệu liên quan.
- Sinh viên: POST `/api/(admin/)students/save-or-update` với ID trong body giữ nguyên trường không gửi (chỉ cần gửi trường muốn đổi).
- Hồ sơ: đọc và sửa các trường được phép của chính mình. Cấu hình hệ thống: Admin đọc và cập nhật các khóa hiện có.
- Danh mục: đơn vị quản lý, tòa nhà, loại phòng, học kỳ, diện ưu tiên có tìm kiếm phân trang/chi tiết/lưu chung/xóa. Request sửa qua save-or-update giữ nguyên trường không gửi hoặc gửi null; khung chưa hỗ trợ dùng null để xóa giá trị tùy chọn.
- Phòng/giường: phân trang và lọc phòng; đọc chi tiết có giường/người ở, cờ cảnh báo thiếu trưởng phòng (`missingLeader`), thông tin trưởng phòng (`leader`) và lịch sử thay đổi trưởng phòng (`leaderHistory`) từ `audit_log`; tạo phòng tự sinh giường; sửa (đổi loại phòng trống thì số giường đồng bộ theo loại mới; đổi `bedCount` của loại phòng đang dùng trả 409), đổi trạng thái có lý do, thêm/sửa giường qua save-or-update, xóa phòng/giường chưa từng có phân phòng; chỉ định/gỡ trưởng phòng (idempotent, không ghi audit thừa); cung cấp API giường trống `GET /api/beds/available` cho split view phân giường và duyệt chuyển phòng.
- Dịch vụ chỗ ở (`BedOccupancyService` & `OccupancyQuery`): quản lý vòng đời phân phòng/chỗ ở tập trung trong module room: chiếm giữ chỗ (`occupyHold`), kích hoạt ở (`occupyStay`), chuyển giữ chỗ sang ở (`convertHoldToStay`), trả phòng (`release`), chuyển phòng (`transfer`); khóa giường theo `bed_id` tăng dần chống deadlock, khóa sinh viên và kiểm tra lại trạng thái sau khi khóa; chụp đơn giá loại phòng vào lịch sử.
- Đợt đăng ký: tạo, tìm kiếm, đọc, sửa, lưu đơn vị áp dụng và chỉ tiêu; xóa đợt chưa mở và chưa có đơn. Đợt đã mở chỉ được gia hạn ngày đóng. Staff chỉ ghi đợt mà đơn vị mình là đơn vị duy nhất; đợt dùng chung ngoài phạm vi ghi trả 404.
- Đơn đăng ký: tạo/sửa DRAFT, nộp, duyệt, từ chối có lý do, hủy khi còn DRAFT/SUBMITTED/APPROVED (APPROVED đã có hợp đồng PENDING/ACTIVE trả 409 APPLICATION_HAS_CONTRACT); đọc danh sách/chi tiết theo quyền và timeline.
- Sửa chữa: tạo ticket và ảnh, danh sách/chi tiết, giao kỹ thuật viên cùng đơn vị, bắt đầu, ghi chú tiến độ, hoàn thành, nghiệm thu, mở lại, từ chối và hủy theo Policy.
- Điện nước: CRUD chỉ số từng phòng/tháng, tiêu thụ do CSDL tính. Chỉ số đã được dùng trong hóa đơn không được sửa/xóa. Đơn giá đã có hiệu lực giữ lịch sử; chỉ sửa/xóa bản giá tương lai do mình tạo.
- Hóa đơn: tìm kiếm/chi tiết và CRUD hồ sơ UNPAID chưa có dòng chi tiết hoặc lịch sử. Form nhập tổng tiền và hạn đóng, chưa tự phát hành/tính tiền. Endpoint xác nhận thanh toán có sẵn được giữ lại.
- Hợp đồng: tìm kiếm/chi tiết và CRUD hồ sơ PENDING từ đơn đã duyệt, chưa có phân giường/tiền cọc/lịch sử. Form chỉ lưu ngày bắt đầu/kết thúc; không kích hoạt hợp đồng.
- Yêu cầu gia hạn, chuyển phòng và lựa chọn hè: sinh viên CRUD hồ sơ PENDING của mình; Staff tra cứu theo phạm vi. Riêng yêu cầu chuyển phòng đã có đầy đủ luồng duyệt (`approve`), từ chối (`reject`) và hủy (`cancel`) qua `RoomTransferService` và `BedOccupancyService`; kiểm tra quyền `ROOM_TRANSFER_APPROVE`, `ROOM_TRANSFER_DELETE` và trả `TransferDetail` kèm `currentBed`, timeline và `allowedActions`.
- Sửa chữa dùng save-or-update để lưu nội dung khi OPEN và DELETE chuyển sang CANCELLED cho người tạo; giữ ảnh và lịch sử.
- Thông báo: CRUD thông báo thủ công của người tạo; sửa tiêu đề/nội dung giữ nguyên phạm vi. Hộp thư có tìm kiếm. Những endpoint đọc/đánh dấu đã đọc và listener có sẵn được giữ lại.
- Tổng quan: tra cứu sổ cọc, giao dịch cọc, phiếu gửi đồ, phòng của sinh viên và audit log. Các trang này chỉ đọc.

Lỗi framework trả 400/404/405/415 đúng loại; ràng buộc CSDL ngoài danh sách tên được ánh xạ theo SQLState (`DUPLICATE_VALUE`, `RESOURCE_IN_USE`, `VALIDATION_FAILED`). Chỉ lỗi đồng thời trả `CONCURRENT_UPDATE`. Tìm kiếm escape `%`, `_`, `\` bằng `LikePattern`; `CurrentUser.get()` chỉ truy vấn tài khoản một lần mỗi request.

Tất cả danh sách, danh mục và lookup ô chọn dùng POST `.../search-by-page` nhận body `SearchRequest`, lọc phạm vi trước khi đếm/phân trang tại CSDL và trả `PageResponse`. Giường dùng POST `/api/beds/save-or-update` với ID trong body để tạo/sửa mã giường. Màn React dùng API thật, hướng dẫn chạy được gom vào [README chung](../../README.md).

API giữ tiền tố `/api`, dùng `/search-by-page` và `/save-or-update` theo cách tổ chức V5, giữ ID số nguyên và phân trang từ 0 của DMS. Path khai trực tiếp trong annotation Controller giống V5 theo yêu cầu người phụ trách; không tạo Constants cho URL API. Constants vẫn dùng cho role/quyền/trạng thái/mã lỗi.

### Contract CRUD thống nhất — 15/10/2026

- Danh sách: POST `<readBase>/search-by-page`, JSON body `{ "page": 0, "size": 20, "q": "nguyen", "sort": ["id,desc"], "managementUnitId": 1, "locked": false }`. Bộ lọc nằm cùng cấp, có kiểu JSON đúng và chỉ dùng cho resource hỗ trợ. Trang từ 0; size 1–100. Kết quả HTTP 200 gồm `{content,page,size,totalElements,totalPages}`; danh sách rỗng có content rỗng.
- Lưu: POST `<writeBase>/save-or-update`, body phẳng `{ "id": null, ...các trường tạo mới }` để thêm; `{ "id": 15, ...các trường cần sửa }` để cập nhật. Không dùng URL `/{id}` hay PATCH cho lưu CRUD. HTTP **200 cho cả tạo và sửa**; ID không dương trả 400, ID không tồn tại/ngoài phạm vi trả 404, không tự tạo thay cho bản ghi sửa bị mất.
- Các nhóm units/buildings/priority-categories đọc từ `/api/<resource>/search-by-page` nhưng ghi `/api/admin/<resource>/save-or-update`. Users đọc/ghi `/api/admin/users/...`. Sinh viên đọc `/api/students/...` hoặc `/api/admin/students/...`; Admin tạo/sửa tại `/api/admin/students/save-or-update`, Staff chỉ sửa tại `/api/students/save-or-update` trong phạm vi được cấp.
- `saveOrUpdate` là một hàm public trong Service interface/Impl cho mỗi luồng CRUD. Controller khai OR CREATE/UPDATE; Impl kiểm tra chính xác quyền CREATE nếu ID trống hoặc UPDATE nếu có ID, rồi giữ scope/trạng thái/transaction/audit. Giao diện vẫn kiểm quyền Thêm/Sửa riêng.
- Tạo tài khoản/sinh viên trả `{userId,temporaryPassword}` một lần như trước; sửa trả Profile/StudentMap. Không đưa mật khẩu tạm vào cache hồ sơ hay audit. Các resource khác trả dữ liệu sau khi lưu.
- Sửa chữa giữ multipart: POST `/api/maintenance-requests/save-or-update`, phần JSON `data` chứa `id` và nội dung, phần `photos` cho ảnh khi tạo. Khi sửa chỉ đổi nội dung ticket OPEN, giữ ảnh/vị trí; không làm mất ảnh cũ.
- Giường: POST `/api/beds/save-or-update` với `{id,roomId,code}`; tạo cần roomId, sửa không đổi phòng nguồn. DELETE và POST đổi trạng thái giữ endpoint riêng.
- Hồ sơ cá nhân vẫn GET `/api/me/profile`, lưu qua POST `/api/me/profile/save-or-update` và chỉ sửa người đăng nhập. Cấu hình dùng `/api/admin/configs/search-by-page` và `/api/admin/configs/save-or-update` với `{key,value}`, chỉ sửa khóa đã có. `/api/me/room/search-by-page` là danh sách phòng cá nhân, không có chỗ ở trả trang rỗng.
- GET chi tiết, DELETE, hành động trạng thái, reset/khóa tài khoản, hộp thư đã đọc/chưa đọc và GET/PUT ma trận role giữ chuyên biệt. API `/auth/me` và `/notifications/unread-count` không đổi thành search. Không gộp trạng thái/tài chính/phân phòng vào lệnh lưu dữ liệu.
- Đây là thay đổi contract cần cập nhật frontend/caller đồng thời. Không duy trì cặp API tạo/sửa cũ hoặc danh sách GET để làm đường vòng. Đặc tả đầy đủ và ví dụ ở [API Spec](../../Docs/02%20External%20Design/API%20Specification/API-Spec-DMS.md), mục 1.1, 2.5, 2.9 và 3.

### Kiểm chứng refactor API (cập nhật 16/10/2026)

- Backend `clean verify` trên Java 17.0.17 và PostgreSQL 16 kiểm thử riêng: **102/102 test đạt**, gồm quyền CREATE/UPDATE riêng, scope và alias `/me`, ID mất/không hợp lệ, partial update, multipart/ảnh, sort/count, đầu vào null và 10 test hồi quy từ bản sửa code review đã tích hợp. Các migration đã áp dụng giữ nguyên.
- React build và lint đạt; **37/37 kiểm tra hợp đồng frontend** chạy helper/hàm tìm kiếm/lưu thật với HTTP stub. Đây không thay cho kiểm thử trình duyệt với Keycloak đang chạy.
- Quy tắc nghiệp vụ chung và ngoại lệ URL API literal nằm tại [CLAUDE.md](../../CLAUDE.md), được [AGENTS.md](../../AGENTS.md) yêu cầu áp dụng. `ValidationMessage`, `ApiField` và `EntityType` gom message/field/mã audit dùng chung; thông số vận hành vẫn đọc cấu hình.
- Khởi động lại backend/frontend cùng phiên bản khi sử dụng contract mới; caller cũ dùng GET danh sách hoặc POST/PATCH tạo/sửa phải chuyển sang POST `search-by-page`/`save-or-update`.

## Giới hạn của khung

Phạm vi hiện tại được chốt là **khung CRUD BE và React đơn giản**, chưa phải toàn bộ MVP. Các chức năng chi tiết để làm sau: import/export, xác minh ưu tiên, phân giường/hủy giữ chỗ, kích hoạt/gia hạn/trả phòng, xét duyệt hè/gửi nhận đồ, tự phát hành/điều chỉnh hóa đơn, thu/trừ/bù/hoàn cọc, kết luận chi phí sửa chữa, dashboard/công nợ và job định kỳ.

Các giới hạn cụ thể liên quan đến chỗ ở và chuyển phòng:
- Chưa phân lại giường đầu HK1 (`storageRecordId` trong API giường trống chưa triển khai, FR-CON-12 ngoài phạm vi hiện tại).
- `GET /api/me/room` (nằm trong `OverviewController`, module notification) hiện chỉ trả phòng và giường cơ bản của sinh viên, chưa trả danh sách bạn cùng phòng và trưởng phòng theo FR-ROOM-08.
- TV3 cần gọi `occupyHold` / `convertHoldToStay` / `release` từ `BedOccupancyService` khi triển khai các luồng phân giường cho đơn đăng ký, kích hoạt hợp đồng và làm thủ tục trả phòng.

Hợp đồng và hóa đơn chỉ sửa/xóa hồ sơ còn đủ điều kiện nêu trên. Lịch sử, audit và sổ cọc chỉ đọc. Xóa đơn đăng ký/ticket trên giao diện là hủy để giữ lịch sử. React không đưa các luồng xử lý chi tiết vào màn CRUD. Những endpoint xử lý đã có từ bước dựng backend được giữ lại, không mở rộng thêm.

Response nghiệp vụ hiện là DTO phục vụ khung: một số tham chiếu dùng ID; timeline trả `actorId` thay vì `actor` lồng nhau. Các bộ lọc nâng cao và DTO chi tiết đầy đủ trong API Spec là công việc tiếp theo. Khi bổ sung cần giữ quyền, transaction và kiểu trả về dùng chung.

## Quy tắc quan trọng

1. Vai trò và trạng thái lấy từ phiên/CSDL. Hồ sơ CRUD có danh sách trường cho phép; các ID và số tiền nhập ở form được kiểm tra quyền/ràng buộc phía backend. Không cho client tự ghi trạng thái hay điểm ưu tiên. Trường request không được hỗ trợ trả 400.
2. Staff chỉ ghi trong đơn vị mình. Quyền tổng hợp mở rộng quyền đọc; ngoại lệ tạo/sửa đợt nhiều đơn vị theo API 4.2. Đối tượng ngoài phạm vi trả 404.
3. Phạm vi danh sách được lọc trong SQL trước phân trang và đếm tổng. Các truy vấn đọc cần scope dùng view/lịch sử; chỉ Service của module sở hữu dữ liệu thực hiện ghi.
4. Đổi trạng thái dùng POST hành động, không PATCH trạng thái. Policy dùng chung để tính quyền và kiểm tra lệnh. Trạng thái và timeline cùng transaction.
5. Khóa giường theo ID tăng dần trước khi kiểm tra trạng thái phòng. Không xóa hoặc ghi đè lịch sử phân phòng; giá và số chỗ trống không lưu lại ở phòng.
6. Đơn giá đã có hiệu lực giữ nguyên; thêm bản giá mới để thay đổi giá. Hóa đơn thanh toán khóa bản ghi trước khi kiểm tra trạng thái, để chống xác nhận trùng.
7. Ảnh tối đa 3 file, mỗi file 5 MB, kiểm tra magic bytes JPEG/PNG/WebP. Server sinh tên file; quyền xem kiểm tra như ticket. CSDL rollback thì file đã ghi cũng được dọn.
8. Lỗi ràng buộc PostgreSQL được ánh xạ theo tên constraint sang `errorCode`; lỗi không dự kiến không trả stack trace hoặc SQL cho client.
9. Flyway là nguồn schema. Muốn thay CSDL thì thêm migration mới, không sửa V1/V2 đã chạy. Hibernate `validate` kiểm tra mapping, không tự sửa bảng.

## Chạy thử

Xem [README chung](../../README.md). Windows local dùng start-local.ps1; máy khác dùng Compose và đặt secret backend từ môi trường. Không có tài khoản/mật khẩu mặc định trong Git.

## Luồng đăng nhập Keycloak

SecurityConfig xác thực JWT (issuer/audience/chữ ký/expiry), CurrentUserImpl ghép subject bất biến vào app_user.keycloak_user_id và kiểm tra role/version/locked. V4 cho phép bỏ password_hash local khi chuyển tài khoản; giữ V1–V3. IdentityAccountService/Impl gọi Admin REST, có thao tác bù khi rollback và xóa sau commit; chưa có transaction phân tán/outbox retry bền vững. Quyền Staff reset và scope nghiệp vụ giữ nguyên.

Luồng session/BCrypt cũ đã được bỏ; ứng dụng chỉ xác thực qua Keycloak. Keycloak đã cấp JWT có thể còn sống tối đa 5 phút sau thay đổi ngoài DMS; thao tác từ DMS thu hồi ngay bằng version.


## Kiểm tra

```powershell
.\mvnw.cmd verify
```

Test tích hợp mặc định dùng PostgreSQL Testcontainers và cần Docker. Nếu có một PostgreSQL **riêng cho kiểm thử**, có thể chạy:

```powershell
.\mvnw.cmd '-Ddms.test.external-db=true' '-Dspring.datasource.url=jdbc:postgresql://localhost:55432/postgres' '-Dspring.datasource.username=dms_test' '-Dspring.datasource.password=test' verify
```

Không trỏ test vào CSDL đang sử dụng: test khởi động chạy migration và tạo dữ liệu mẫu. Các case CRUD thông thường có transaction rollback. Kiểm tra xóa/khóa, constraint, quyền, trạng thái và session nằm trong `CrudApiIntegrationTest`; mapping/schema trong `DmsApplicationTests`; phụ thuộc module trong `ModuleDependencyTest`; kiểm tra chiếm/trả/chuyển giường và ca đồng thời (AT-02, AT-03, chuyển chéo, duyệt kép) trong `BedOccupancyIntegrationTest`; kiểm tra trưởng phòng và lịch sử audit trong `RoomLeaderIntegrationTest`; kiểm tra ma trận chuyển trạng thái chuyển phòng trong `RoomTransferPolicyTest`. Tổng số test backend hiện tại: **108/108 test** đạt.

Kiểm chứng đăng nhập ngày 13/10/2026: `clean verify` đạt 38/38 test trên PostgreSQL kiểm thử riêng, gồm 14 case `AuthenticationSecurityIntegrationTest` cho CSRF/session thật, khóa đồng thời, đổi/reset mật khẩu, thu hồi phiên sau commit và phạm vi Staff. Frontend lint/build đạt. Chưa xác nhận thao tác UI trực tiếp vì công cụ trình duyệt báo `Unable to load browser request-header policy`.

Kiểm chứng Keycloak ngày 14/10/2026: token thật đã gọi CRUD sinh viên và quản lý khóa/reset; UI đăng nhập được vào portal Admin. Kết quả test cuối được ghi trong README.md.


Dọn cấu trúc: dùng lại AuthController, SecurityConfig và BootstrapAdmin; khóa/version/reset nằm trong AuthService/Impl, chuyển tài khoản nằm trong IdentityAccountService/Impl. Không tạo Controller/Config/Service song song cho cùng chức năng.

## Phân quyền Role → thao tác (cập nhật 14/10/2026)

Theo quyết định mới của người phụ trách, ADMIN có mọi quyền nghiệp vụ trên toàn hệ thống. ADMIN không cần gắn management_unit_id; bỏ giới hạn đơn vị/chủ sở hữu/người được giao khi xác định quyền, nhưng vẫn phải qua trạng thái, ràng buộc dữ liệu, đối tượng sinh viên thật và lịch sử/audit. Không đổi hoặc giả vai trò/người thao tác để vượt phạm vi.

Khung tham khảo RolePrivilege của HR-V5: `ScopeGuard` kiểm tra quyền hiện hành qua `RolePermissionService`; ma trận trong code chỉ xác định quyền mặc định và giới hạn nghiệp vụ của từng role, mỗi mã là `<RESOURCE>_<READ|CREATE|UPDATE|DELETE>`. READ dùng cho tìm kiếm và xem chi tiết; khóa/mở khóa/reset/gán phạm vi tài khoản có mã riêng. `/api/auth/me` trả thêm `permissions: string[]`; React dùng danh sách này cho menu, route và từng nút. API khai quyền bằng @RequirePrivilege giống V5; Spring Security Method Security kiểm tra trước thân Controller. Service Impl tiếp tục kiểm quyền nội bộ, phạm vi và trạng thái. Không chỉ dựa vào việc ẩn nút.

- ADMIN: mọi quyền quản trị và nghiệp vụ ở mọi đơn vị; tài khoản và người thao tác trong audit vẫn là ADMIN thực.
- STAFF: nghiệp vụ trong đơn vị được giao; crossUnitAccess chỉ mở rộng đọc. Đơn/yêu cầu do sinh viên tạo không mặc nhiên cho Staff tạo hoặc hủy thay sinh viên; quyền sửa/xét duyệt phụ thuộc endpoint và trạng thái.
- TECHNICIAN: đọc/cập nhật tiến độ ticket được giao, không CRUD dữ liệu phòng/hợp đồng/hóa đơn.
- STUDENT: dữ liệu cá nhân và phòng đang ở; đơn/yêu cầu của mình, không sửa role hoặc ID người sở hữu.

Đổi role của tài khoản Staff/KTV/Admin qua POST `/api/admin/users/save-or-update` với `id` trong body hỗ trợ trường `role`, đồng bộ Keycloak và thu hồi token cũ. Staff/KTV phải có đơn vị; chuyển sang ADMIN xóa đơn vị và cờ tổng hợp; không chuyển hồ sơ STUDENT thành tài khoản cán bộ, không tự đổi role tài khoản đang sử dụng. ADMIN tạo đơn đăng ký hộ sinh viên bằng `studentId` rõ ràng; STUDENT không được gửi ID của người khác.

Cập nhật 15/10/2026: ADMIN quản lý ma trận động tại `/admin/role-permissions`, lưu quyền của STAFF/STUDENT/TECHNICIAN trong `role_permission_matrix` qua Flyway V5. ADMIN giữ mọi quyền đã đăng ký và không có bản ghi để thu hồi quyền ADMIN. Mã quyền vẫn cố định trong Constants; quyền đang được cấp đọc từ CSDL. Sổ cọc/audit vẫn giữ chỉ đọc, hồ sơ có lịch sử và trạng thái vẫn chịu ràng buộc xóa/hủy như trước.

Kiểm chứng: **44/44 test backend** đạt trên PostgreSQL kiểm thử riêng. Bộ test mới kiểm Admin toàn đơn vị, tạo đơn hộ đúng sinh viên/audit, ticket/hóa đơn/thông báo, quyền từng role và đổi role thu hồi token cũ; các API /me vẫn chỉ trả dữ liệu cá nhân. Build backend và React lint/build đạt; ứng dụng local đã chạy lại.

### Khai quyền API bằng @RequirePrivilege — 14/10/2026

Mọi hàm REST mapped trong ứng dụng khai `@RequirePrivilege(Permission.ROOM_CREATE)` hoặc một danh sách như `@RequirePrivilege({Permission.ROOM_TYPE_READ, Permission.ROOM_CREATE, Permission.ROOM_UPDATE})`. Nhiều mã dùng OR: có ít nhất một quyền mới được chạy API. Danh sách OR phù hợp API tra cứu cho form; save-or-update khai OR CREATE/UPDATE ở Controller, Impl phải kiểm tra đúng quyền theo ID. DELETE và hành động chuyên biệt dùng mã quyền riêng.

`common/security/RequirePrivilege.java` chỉ khai annotation; `ScopeGuard.hasDeclaredPrivilege` đọc khai báo trên hàm thật, dùng khai báo lớp nếu hàm chưa khai, và kiểm role với dữ liệu tài khoản hiện tại. `SecurityConfig` bật Spring Security Method Security và đăng ký AuthorizationManagerBeforeMethodInterceptor: thiếu annotation, mảng rỗng hoặc không có quyền thì trả 403 trước thân Controller. Chưa đăng nhập/token không hợp lệ vẫn bị filter chain trả 401. Annotation ở hàm thay thế annotation lớp; Admin không vượt qua mã quyền chưa đăng ký.

Service Impl vẫn giữ kiểm tra quyền cho lời gọi nội bộ, phạm vi đơn vị/chủ sở hữu, trạng thái và ràng buộc dữ liệu; việc thêm annotation không biến dữ liệu đang dùng thành dữ liệu được phép xóa. ADMIN tiếp tục có mọi quyền nghiệp vụ đã đăng ký trên toàn hệ thống. Frontend và `/api/auth/me.permissions` giữ nguyên mã quyền hiện có.

Ở bước bổ sung annotation chỉ thêm một file source là `RequirePrivilege.java`; không thêm migration, Aspect/Service mới hoặc dependency V5. Bộ test kiểm OR, chặn trước thân Controller, lớp/hàm, thiếu/rỗng/sai annotation và quét mọi API để không bỏ sót quyền. Backend clean verify đạt **53/53 test**.

### Constants dùng chung — 15/10/2026

Backend khai mã cố định tại `common/constants/DmsConstants.java`: `Role`, `Action`, `Resource`, `Permission` và các nhóm trạng thái theo nghiệp vụ. Controller dùng `@RequirePrivilege(Permission.ROOM_CREATE)`; Service Impl dùng cùng mã trong `requirePermission`. `RolePermissionService` đọc quyền được cấp từ CSDL; `ScopeGuard` giữ danh mục quyền mặc định/phù hợp theo role; khai một hằng số không tự cấp thêm quyền cho role. Các nhóm có constructor private và tập mã bất biến để tránh thay đổi cấu hình quyền lúc chạy.

React dùng `frontend/src/constants/authorization.ts` cho role, nghiệp vụ và quyền, kèm kiểu TypeScript suy ra từ hằng số. `permissionFor` chỉ nhận mã đã đăng ký; mã không hợp lệ không mở menu/nút CRUD. Quyền thực tế vẫn lấy từ `/api/auth/me.permissions`. Backend và frontend giữ nguyên mã `<RESOURCE>_<ACTION>`, không đổi dữ liệu đã lưu, format JWT hoặc các migration Flyway đã áp dụng.

Constants dành cho mã cố định của ứng dụng. URL, port, realm, client ID và secret tiếp tục cấu hình qua biến môi trường; các ngưỡng nghiệp vụ tiếp tục đọc `system_config`. Không đưa mật khẩu/secret vào Constants. Khi thêm quyền, cập nhật danh mục Constants, giới hạn mặc định của `ScopeGuard`, migration cấp quyền nếu cần, annotation API và khai báo frontend tương ứng, rồi chạy test phân quyền.

Kiểm chứng: **55/55 test backend** đạt, gồm baseline giữ nguyên toàn bộ danh sách quyền ADMIN/STAFF/STUDENT/TECHNICIAN và kiểm danh mục 103 mã quyền. Đã đối chiếu mã role/action/resource/permission của React với class backend đã biên dịch và giữ nguyên 12 regex DTO. Backend clean package và React lint/build đạt; API dùng token Keycloak thật kiểm tra CRUD, khóa/mở khóa, reset và thu hồi phiên thành công. Bản local đã chạy lại, danh tính/mật khẩu kiểm thử được dọn. Bước này chỉ thêm hai file Constants; không thêm migration.

### Hoàn thiện các luồng dùng chung — 15/10/2026

- Đăng nhập và truy cập URL trực tiếp: trang gốc Admin kiểm USER_READ, Staff kiểm DASHBOARD_READ, Student kiểm PROFILE_READ, Technician kiểm MAINTENANCE_READ. Chuẩn hóa dấu gạch chéo/query/hash; cổng/màn không có trong khai báo bị từ chối. Chuyển hướng sau đăng nhập dùng quyền thực tế từ /auth/me.
- Hồ sơ của tôi có ở cả bốn cổng, dùng lại CrudPage và GET /api/me/profile và POST /api/me/profile/save-or-update. Sinh viên sửa điện thoại/ quê quán/liên hệ khẩn cấp; cán bộ chỉ sửa điện thoại. Mã sinh viên, họ tên, role và đơn vị không được sửa từ hồ sơ cá nhân.
- CRUD dùng chung lưu từ khóa, bộ lọc, số trang và cỡ trang trên URL theo từng resource, ví dụ users.q/users.role/users.page. Refresh, quay lại hoặc các tab ghép không làm lẫn điều kiện; đổi bộ lọc/cỡ trang trở về trang đầu, xóa hết trang tự về trang hợp lệ. Cỡ trang 10/20/50/100, mặc định 20; chỉ gửi bộ lọc được khai báo trong CrudResource.filters.
- Control nhập liệu được đặt trực tiếp trong Form.Item để nhận value/onChange. Form hiển thị lỗi trường do API trả về; lỗi 409 tải lại dữ liệu qua handler chung. Chuỗi rỗng có thể xóa nội dung text tùy chọn, null vẫn giữ nghĩa cập nhật từng phần cũ. Không đóng modal đang gửi request; thao tác xóa, khóa, reset, mở khóa và đổi phạm vi đọc có xác nhận.
- Tài khoản lọc role, managementUnitId và locked; UI xác nhận đổi vai trò/đơn vị và giải thích việc thu hồi phiên. Staff có thể được Admin cấp/thu hồi đọc liên đơn vị qua API cross-unit-access đã có; quyền ghi trong đơn vị vẫn giữ nguyên.
- Nhật ký lọc actorId, action, entityType, from và to, kết hợp q. Action/type là mã chính xác; khoảng thời gian ISO có múi giờ, hai biên đều bao gồm. Bộ lọc sai trả 400; chỉ Admin được đọc. Khung hiện phân trang theo id giảm dần; xem chi tiết từ bản ghi đã trả về, không gọi endpoint chi tiết chưa có. Truy vấn nằm trong AuditServiceImpl, Controller chỉ nhận tham số và gọi Service.

Kiểm chứng: 60/60 test backend đạt trên PostgreSQL kiểm thử riêng; thêm 5 test cho bộ lọc, phạm vi quản trị, đổi đơn vị và khóa/mở khóa/reset thu hồi token. 53 kiểm tra điều hướng/hồ sơ của bốn role và 48 kiểm tra query/bộ lọc/validation đạt. Backend clean package và React lint/build đạt. Token Keycloak thật kiểm CRUD sinh viên/khóa/mở khóa/reset/logout; giao diện Admin kiểm đăng nhập, giữ bộ lọc khi reload, mở/sửa/xóa nội dung hồ sơ và lọc nhật ký. Tài khoản/mật khẩu kiểm thử được dọn, audit append-only giữ nguyên. Đây là các luồng chung của khung; chưa bổ sung nghiệp vụ phân phòng, hợp đồng, phát hành hóa đơn hay tiền cọc.


### Màn tick quyền động theo role — 15/10/2026

Đăng nhập ADMIN, vào **Phân quyền theo vai trò** tại `http://localhost:5173/admin/role-permissions`. Chọn STAFF/STUDENT/TECHNICIAN, tìm màn rồi tick Xem/Tìm kiếm, Thêm, Sửa, Xóa hoặc quyền riêng; bấm **Lưu quyền** để áp dụng. Khôi phục mặc định chỉ đổi bản nháp, vẫn cần lưu. ADMIN hiển thị toàn quyền và không sửa được. Các ô không phù hợp vai trò bị khóa; PROFILE_READ bắt buộc.

Backend dùng RolePermissionService interface/Impl và Flyway V5, chỉ thêm một bảng quyền. Khi chạy bản mới, Flyway tự tạo bảng và giữ quyền mặc định hiện tại; không sửa V1–V4. Quyền được đọc từ CSDL mỗi lần kiểm tra API, kể cả token đã cấp trước khi bỏ tick. Frontend cập nhật /me sau lưu, khi focus, mỗi 30 giây hoặc khi API bị từ chối; mất quyền trang chủ sẽ chuyển về hồ sơ. Mỗi lần lưu có version chống ghi đè và audit trước/sau.

Quyền áp dụng cho mọi tài khoản cùng role trên cùng backend/database DMS. Dùng chung Keycloak nhưng mỗi máy có database DMS riêng thì quyền vẫn riêng; muốn cả nhóm đồng bộ cần dùng chung backend/database. Mã quyền vẫn nằm trong Constants; danh sách đang cấp nằm trong bảng, không phải sửa code để bật/tắt từng quyền.


Kiểm chứng phần ma trận: backend clean verify đạt **73/73 test**, gồm 13 test mới (thu hồi/khôi phục với token cũ, role không được quản trị, danh sách sai, dữ liệu thiếu/hỏng, version 409, no-op và audit). Backend clean package đạt; React build/lint đạt và 128 kiểm tra runtime helper (27 matrix/fallback, 53 auth/navigation/profile, 48 CRUD query/validation) đạt. Danh mục Role/Action/Resource/Permission của React khớp chính xác class backend đã biên dịch.

Bản local đã chạy migration V5 và health UP. JWT Keycloak thật xác minh /me và GET ma trận bốn role; trình duyệt kiểm tra tick/bỏ READ, tự chọn READ khi tick Sửa, Hủy, cảnh báo đổi role và ADMIN chỉ xem. Không lưu thay đổi quyền trên database local khi thử giao diện; toàn bộ test ghi ma trận dùng PostgreSQL kiểm thử riêng. Danh tính/mật khẩu kiểm thử đã dọn, hồ sơ có audit được khóa và giữ lịch sử. Phần này chỉ thêm 3 Java source, 1 migration và 1 trang React; Service interface/Impl có ghi chú tiếng Việt.

### Phân quyền duyệt chuyển phòng & Migration V6 — 15/10/2026

Bổ sung hành động `APPROVE` và mã quyền `ROOM_TRANSFER_APPROVE` vào catalogue phân quyền trong `DmsConstants.Permission`, nâng tổng số mã quyền của hệ thống từ 105 lên **106** mã.

Flyway `V6__room_transfer_approve_permission.sql` bổ sung mã `ROOM_TRANSFER_APPROVE` vào mảng JSONB của vai trò `STAFF` trong bảng `role_permission_matrix` nếu chưa có:
- Migration V6 không tăng trường `version` trong bảng và không thay đổi cấu trúc bảng (schema giữ nguyên).
- Sau migration V6: vai trò `STAFF` mặc định có **73** quyền, `ADMIN` có **106** quyền (toàn bộ catalogue), `STUDENT` giữ 35 quyền và `TECHNICIAN` giữ 11 quyền.

Các test mới cho phần chiếm/trả giường, duyệt chuyển phòng và phân quyền gồm:
- `BedOccupancyIntegrationTest`: kiểm tra toàn bộ luồng chiếm giữ chỗ (`occupyHold`), kích hoạt (`occupyStay`), chuyển sang ở (`convertHoldToStay`), trả phòng (`release`), chuyển phòng (`transfer`), kiểm tra đơn giá chụp theo loại phòng, và các ca đồng thời AT-02, AT-03, chuyển phòng chéo nhau, hai lượt duyệt đồng thời.
- `RoomLeaderIntegrationTest`: kiểm tra chỉ định/gỡ trưởng phòng, các ràng buộc `LEADER_ALREADY_ASSIGNED` (kèm `roomCode`), `LEADER_NOT_STAYING`, tự động gỡ trưởng phòng khi phân phòng đóng và đọc lịch sử từ `audit_log`.
- `RoomTransferPolicyTest`: kiểm tra chuyển trạng thái và `allowedActions` cho yêu cầu chuyển phòng theo vai trò và quyền.

# Khung CRUD và đăng nhập backend DMS

Nhánh triển khai: `develop-quyennnv`. Backend giữ Spring Boot 3.5, Java 17+, PostgreSQL 16, Flyway và `ddl-auto: validate`.

## Cấu trúc và cách đọc mã

Mỗi module có `domain` (entity), `repository`, `service` và `api` (Controller, request/response DTO). Toàn bộ `service/<Tên>Service.java` là interface, chỉ khai báo chữ ký hàm và JavaDoc tiếng Việt. Logic nằm trong `service/impl/<Tên>ServiceImpl.java`, dùng `implements <Tên>Service`, constructor injection và `@Service`. Transaction đặt trên Impl; Controller và các module khác gọi qua interface. Các hàm overload cũng được triển khai trong Impl, không dùng `default` chứa thân hàm ở interface.

Các lớp nền dùng chung nằm trong `common/service/impl`: `AbstractCrudServiceImpl` triển khai `CrudService`, `AbstractSqlCrudServiceImpl` chứa helper SQL cho các Impl của hồ sơ khung. Các kiểu record dữ liệu tham chiếu có thể khai báo trong interface, nhưng không chứa logic xử lý. Kiểm tra ArchUnit bảo vệ quy tắc interface/Impl cùng quy tắc phụ thuộc module.

`common/dto` chứa kiểu trao đổi dùng chung. Không đặt DTO dùng chung trong `common/api`: kiểm tra ArchUnit không cho module khác sử dụng Controller/DTO của một module khác. `common/support/PageQuery` kiểm tra page, size và danh sách trường sắp xếp.

`domain` là tên package, không có ý nghĩa đặc biệt với Spring. Các annotation `@Entity`, `@Table`, `@Id` mới quyết định mapping. Có entity và Repository cho cả 33 bảng nghiệp vụ, gồm khóa kép bằng `@IdClass`, JSONB, CHAR(1), NUMERIC và cột tiêu thụ điện nước do CSDL sinh. Tham chiếu chéo module lưu bằng ID, không dựng quan hệ JPA xuyên module.

Tên hàm dùng tiếng Anh rõ nghĩa; JavaDoc và ghi chú quan trọng dùng tiếng Việt. Ví dụ: `searchRooms`, `getRoomById`, `createRoom`, `updateRoom`, `changeRoomStatus`, `assignRoomLeader`, `createApplication`, `submitApplication`, `assignTechnician`, `confirmInvoicePayment`.

## API đã có

- Xác thực mặc định: Keycloak OIDC/PKCE, Spring Security JWT Resource Server; /me trả hồ sơ DMS, logout thu hồi version. Mật khẩu/khóa tạm/đổi mật khẩu thuộc Keycloak. Subject, role, version và scope vẫn kiểm tra tại DMS.
- Tài khoản: Admin quản lý tài khoản và sinh viên; sinh viên được thêm từng hồ sơ bằng form CRUD, trả mật khẩu tạm một lần. Staff tìm kiếm/đọc/sửa và reset mật khẩu sinh viên trong phạm vi mình. Import Excel chưa triển khai. Tài khoản cán bộ dùng khóa thay cho xóa; sinh viên chỉ xóa khi chưa có dữ liệu liên quan.
- Hồ sơ: đọc và sửa các trường được phép của chính mình. Cấu hình hệ thống: Admin đọc và cập nhật các khóa hiện có.
- Danh mục: đơn vị quản lý, tòa nhà, loại phòng, học kỳ, diện ưu tiên có tạo/đọc/PATCH/xóa. Request PATCH giữ nguyên trường không gửi hoặc gửi null; khung chưa hỗ trợ dùng null để xóa giá trị tùy chọn.
- Phòng/giường: phân trang và lọc phòng; đọc chi tiết có giường/người ở; tạo phòng tự sinh giường; sửa, đổi trạng thái có lý do, thêm giường, xóa phòng/giường chưa từng có phân phòng; chỉ định/gỡ trưởng phòng và ghi nhật ký.
- Đợt đăng ký: tạo, tìm kiếm, đọc, sửa, lưu đơn vị áp dụng và chỉ tiêu; xóa đợt chưa mở và chưa có đơn. Đợt đã mở chỉ được gia hạn ngày đóng.
- Đơn đăng ký: tạo/sửa DRAFT, nộp, duyệt, từ chối có lý do, hủy khi còn DRAFT/SUBMITTED/APPROVED; đọc danh sách/chi tiết theo quyền và timeline.
- Sửa chữa: tạo ticket và ảnh, danh sách/chi tiết, giao kỹ thuật viên cùng đơn vị, bắt đầu, ghi chú tiến độ, hoàn thành, nghiệm thu, mở lại, từ chối và hủy theo Policy.
- Điện nước: CRUD chỉ số từng phòng/tháng, tiêu thụ do CSDL tính. Chỉ số đã được dùng trong hóa đơn không được sửa/xóa. Đơn giá đã có hiệu lực giữ lịch sử; chỉ sửa/xóa bản giá tương lai do mình tạo.
- Hóa đơn: tìm kiếm/chi tiết và CRUD hồ sơ UNPAID chưa có dòng chi tiết hoặc lịch sử. Form nhập tổng tiền và hạn đóng, chưa tự phát hành/tính tiền. Endpoint xác nhận thanh toán có sẵn được giữ lại.
- Hợp đồng: tìm kiếm/chi tiết và CRUD hồ sơ PENDING từ đơn đã duyệt, chưa có phân giường/tiền cọc/lịch sử. Form chỉ lưu ngày bắt đầu/kết thúc; không kích hoạt hợp đồng.
- Yêu cầu gia hạn, chuyển phòng và lựa chọn hè: sinh viên CRUD hồ sơ PENDING của mình; Staff tra cứu theo phạm vi. Chưa xét duyệt hay thay đổi hợp đồng/chỗ ở.
- Sửa chữa bổ sung PATCH nội dung khi OPEN và DELETE chuyển sang CANCELLED cho người tạo; giữ ảnh và lịch sử.
- Thông báo: CRUD thông báo thủ công của người tạo; sửa tiêu đề/nội dung giữ nguyên phạm vi. Hộp thư có tìm kiếm. Những endpoint đọc/đánh dấu đã đọc và listener có sẵn được giữ lại.
- Tổng quan: tra cứu sổ cọc, giao dịch cọc, phiếu gửi đồ, phòng của sinh viên và audit log. Các trang này chỉ đọc.

Các danh sách nghiệp vụ nhận `q`, `page`, `size` và lọc phạm vi trước khi phân trang. Danh mục nhỏ trả mảng; React tìm kiếm và phân trang trên mảng này. Giường có PATCH mã giường. Màn React dùng API thật, hướng dẫn chạy được gom vào [README chung](../../README.md).

API giữ tiền tố `/api` và đường dẫn của API Spec; không sao chép `/save`, `/page`, UUID hoặc Keycloak của HR-V5.

## Giới hạn của khung

Phạm vi hiện tại được chốt là **khung CRUD BE và React đơn giản**, chưa phải toàn bộ MVP. Các chức năng chi tiết để làm sau: import/export, xác minh ưu tiên, phân giường/hủy giữ chỗ, kích hoạt/gia hạn/chuyển/trả phòng, xét duyệt hè/gửi nhận đồ, tự phát hành/điều chỉnh hóa đơn, thu/trừ/bù/hoàn cọc, kết luận chi phí sửa chữa, dashboard/công nợ và job định kỳ.

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

Không trỏ test vào CSDL đang sử dụng: test khởi động chạy migration và tạo dữ liệu mẫu. Các case CRUD thông thường có transaction rollback. Kiểm tra xóa/khóa, constraint, quyền, trạng thái và session nằm trong `CrudApiIntegrationTest`; mapping/schema trong `DmsApplicationTests`; phụ thuộc module trong `ModuleDependencyTest`.

Kiểm chứng đăng nhập ngày 13/10/2026: `clean verify` đạt 38/38 test trên PostgreSQL kiểm thử riêng, gồm 14 case `AuthenticationSecurityIntegrationTest` cho CSRF/session thật, khóa đồng thời, đổi/reset mật khẩu, thu hồi phiên sau commit và phạm vi Staff. Frontend lint/build đạt. Chưa xác nhận thao tác UI trực tiếp vì công cụ trình duyệt báo `Unable to load browser request-header policy`.

Kiểm chứng Keycloak ngày 14/10/2026: token thật đã gọi CRUD sinh viên và quản lý khóa/reset; UI đăng nhập được vào portal Admin. Kết quả test cuối được ghi trong README.md.


Dọn cấu trúc: dùng lại AuthController, SecurityConfig và BootstrapAdmin; khóa/version/reset nằm trong AuthService/Impl, chuyển tài khoản nằm trong IdentityAccountService/Impl. Không tạo Controller/Config/Service song song cho cùng chức năng.

## Phân quyền Role → thao tác (cập nhật 14/10/2026)

Theo quyết định mới của người phụ trách, ADMIN có mọi quyền nghiệp vụ trên toàn hệ thống. ADMIN không cần gắn management_unit_id; bỏ giới hạn đơn vị/chủ sở hữu/người được giao khi xác định quyền, nhưng vẫn phải qua trạng thái, ràng buộc dữ liệu, đối tượng sinh viên thật và lịch sử/audit. Không đổi hoặc giả vai trò/người thao tác để vượt phạm vi.

Khung tham khảo RolePrivilege của HR-V5: `ScopeGuard` khai ma trận cố định cho bốn role, mỗi mã là `<RESOURCE>_<READ|CREATE|UPDATE|DELETE>`. READ dùng cho tìm kiếm và xem chi tiết; khóa/mở khóa/reset/gán phạm vi tài khoản có mã riêng. `/api/auth/me` trả thêm `permissions: string[]`; React dùng danh sách này cho menu, route và từng nút. API khai quyền bằng @RequirePrivilege giống V5; Spring Security Method Security kiểm tra trước thân Controller. Service Impl tiếp tục kiểm quyền nội bộ, phạm vi và trạng thái. Không chỉ dựa vào việc ẩn nút.

- ADMIN: mọi quyền quản trị và nghiệp vụ ở mọi đơn vị; tài khoản và người thao tác trong audit vẫn là ADMIN thực.
- STAFF: nghiệp vụ trong đơn vị được giao; crossUnitAccess chỉ mở rộng đọc. Đơn/yêu cầu do sinh viên tạo không mặc nhiên cho Staff tạo hoặc hủy thay sinh viên; quyền sửa/xét duyệt phụ thuộc endpoint và trạng thái.
- TECHNICIAN: đọc/cập nhật tiến độ ticket được giao, không CRUD dữ liệu phòng/hợp đồng/hóa đơn.
- STUDENT: dữ liệu cá nhân và phòng đang ở; đơn/yêu cầu của mình, không sửa role hoặc ID người sở hữu.

Đổi role của tài khoản Staff/KTV/Admin qua PATCH `/api/admin/users/{id}` hỗ trợ trường `role`, đồng bộ Keycloak và thu hồi token cũ. Staff/KTV phải có đơn vị; chuyển sang ADMIN xóa đơn vị và cờ tổng hợp; không chuyển hồ sơ STUDENT thành tài khoản cán bộ, không tự đổi role tài khoản đang sử dụng. ADMIN tạo đơn đăng ký hộ sinh viên bằng `studentId` rõ ràng; STUDENT không được gửi ID của người khác.

Ma trận hiện cố định trong code, chưa có bảng/màn quản trị tick quyền động như HR-V5. Không thêm migration hoặc file Service riêng cho cùng trách nhiệm. Sổ cọc/audit vẫn giữ chỉ đọc, hồ sơ có lịch sử và trạng thái vẫn chịu ràng buộc xóa/hủy như trước.

Kiểm chứng: **44/44 test backend** đạt trên PostgreSQL kiểm thử riêng. Bộ test mới kiểm Admin toàn đơn vị, tạo đơn hộ đúng sinh viên/audit, ticket/hóa đơn/thông báo, quyền từng role và đổi role thu hồi token cũ; các API /me vẫn chỉ trả dữ liệu cá nhân. Build backend và React lint/build đạt; ứng dụng local đã chạy lại.

### Khai quyền API bằng @RequirePrivilege — 14/10/2026

Mọi hàm REST mapped trong ứng dụng khai `@RequirePrivilege(Permission.ROOM_CREATE)` hoặc một danh sách như `@RequirePrivilege({Permission.ROOM_TYPE_READ, Permission.ROOM_CREATE, Permission.ROOM_UPDATE})`. Nhiều mã dùng OR: có ít nhất một quyền mới được chạy API. Danh sách OR phù hợp API tra cứu cho form; thao tác ghi phải khai riêng CREATE/UPDATE/DELETE, không ghép quyền thêm/sửa để mở cả hai.

`common/security/RequirePrivilege.java` chỉ khai annotation; `ScopeGuard.hasDeclaredPrivilege` đọc khai báo trên hàm thật, dùng khai báo lớp nếu hàm chưa khai, và kiểm role với dữ liệu tài khoản hiện tại. `SecurityConfig` bật Spring Security Method Security và đăng ký AuthorizationManagerBeforeMethodInterceptor: thiếu annotation, mảng rỗng hoặc không có quyền thì trả 403 trước thân Controller. Chưa đăng nhập/token không hợp lệ vẫn bị filter chain trả 401. Annotation ở hàm thay thế annotation lớp; Admin không vượt qua mã quyền chưa đăng ký.

Service Impl vẫn giữ kiểm tra quyền cho lời gọi nội bộ, phạm vi đơn vị/chủ sở hữu, trạng thái và ràng buộc dữ liệu; việc thêm annotation không biến dữ liệu đang dùng thành dữ liệu được phép xóa. ADMIN tiếp tục có mọi quyền nghiệp vụ đã đăng ký trên toàn hệ thống. Frontend và `/api/auth/me.permissions` giữ nguyên mã quyền hiện có.

Ở bước bổ sung annotation chỉ thêm một file source là `RequirePrivilege.java`; không thêm migration, Aspect/Service mới hoặc dependency V5. Bộ test kiểm OR, chặn trước thân Controller, lớp/hàm, thiếu/rỗng/sai annotation và quét mọi API để không bỏ sót quyền. Backend clean verify đạt **53/53 test**.

### Constants dùng chung — 15/10/2026

Backend khai mã cố định tại `common/constants/DmsConstants.java`: `Role`, `Action`, `Resource`, `Permission` và các nhóm trạng thái theo nghiệp vụ. Controller dùng `@RequirePrivilege(Permission.ROOM_CREATE)`; Service Impl dùng cùng mã trong `requirePermission`. `ScopeGuard` vẫn quyết định role nào được cấp quyền; khai một hằng số không tự cấp thêm quyền cho role. Các nhóm có constructor private và tập mã bất biến để tránh thay đổi cấu hình quyền lúc chạy.

React dùng `frontend/src/constants/authorization.ts` cho role, nghiệp vụ và quyền, kèm kiểu TypeScript suy ra từ hằng số. `permissionFor` chỉ nhận mã đã đăng ký; mã không hợp lệ không mở menu/nút CRUD. Quyền thực tế vẫn lấy từ `/api/auth/me.permissions`. Backend và frontend giữ nguyên mã `<RESOURCE>_<ACTION>`, không đổi dữ liệu đã lưu, format JWT hoặc các migration Flyway đã áp dụng.

Constants dành cho mã cố định của ứng dụng. URL, port, realm, client ID và secret tiếp tục cấu hình qua biến môi trường; các ngưỡng nghiệp vụ tiếp tục đọc `system_config`. Không đưa mật khẩu/secret vào Constants. Khi thêm quyền, cập nhật danh mục Constants, ma trận `ScopeGuard`, annotation API và khai báo frontend tương ứng, rồi chạy test phân quyền.

Kiểm chứng: **55/55 test backend** đạt, gồm baseline giữ nguyên toàn bộ danh sách quyền ADMIN/STAFF/STUDENT/TECHNICIAN và kiểm danh mục 103 mã quyền. Đã đối chiếu mã role/action/resource/permission của React với class backend đã biên dịch và giữ nguyên 12 regex DTO. Backend clean package và React lint/build đạt; API dùng token Keycloak thật kiểm tra CRUD, khóa/mở khóa, reset và thu hồi phiên thành công. Bản local đã chạy lại, danh tính/mật khẩu kiểm thử được dọn. Bước này chỉ thêm hai file Constants; không thêm migration.

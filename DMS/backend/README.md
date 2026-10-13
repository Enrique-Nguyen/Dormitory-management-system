# Khung CRUD backend DMS

Nhánh triển khai: `develop-quyennnv`. Backend giữ Spring Boot 3.5, Java 17+, PostgreSQL 16, Flyway và `ddl-auto: validate`.

## Cấu trúc và cách đọc mã

Mỗi module có `domain` (entity), `repository`, `service` và `api` (Controller, request/response DTO). Danh mục dùng Service interface và implementation trong `service/impl`, tham khảo cách tách tầng của HR-V5. Service nghiệp vụ có transaction và Policy riêng; Controller chỉ nhận request, gọi Service và trả response.

`common/dto` chứa kiểu trao đổi dùng chung. Không đặt DTO dùng chung trong `common/api`: kiểm tra ArchUnit không cho module khác sử dụng Controller/DTO của một module khác. `common/support/PageQuery` kiểm tra page, size và danh sách trường sắp xếp.

`domain` là tên package, không có ý nghĩa đặc biệt với Spring. Các annotation `@Entity`, `@Table`, `@Id` mới quyết định mapping. Có entity và Repository cho cả 33 bảng nghiệp vụ, gồm khóa kép bằng `@IdClass`, JSONB, CHAR(1), NUMERIC và cột tiêu thụ điện nước do CSDL sinh. Tham chiếu chéo module lưu bằng ID, không dựng quan hệ JPA xuyên module.

Tên hàm dùng tiếng Anh rõ nghĩa; JavaDoc và ghi chú quan trọng dùng tiếng Việt. Ví dụ: `searchRooms`, `getRoomById`, `createRoom`, `updateRoom`, `changeRoomStatus`, `assignRoomLeader`, `createApplication`, `submitApplication`, `assignTechnician`, `confirmInvoicePayment`.

## API đã có

- Xác thực: CSRF, đăng nhập JSON, đăng xuất, người dùng hiện tại, đổi mật khẩu. Phiên lưu trong Spring Session JDBC; mật khẩu BCrypt, khóa tạm sau 5 lần sai, bắt đổi mật khẩu và khóa tài khoản có hiệu lực với phiên đang mở.
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

Các danh sách nghiệp vụ nhận `q`, `page`, `size` và lọc phạm vi trước khi phân trang. Danh mục nhỏ trả mảng; React tìm kiếm và phân trang trên mảng này. Giường có PATCH mã giường. Màn React dùng API thật, hướng dẫn ở [README frontend](../frontend/README.md).

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

Khởi động PostgreSQL bằng `docker compose up -d` trong `DMS`. Trong terminal PowerShell chạy backend, có thể tạo Admin đầu tiên bằng biến môi trường:

```powershell
$env:DMS_BOOTSTRAP_ADMIN_ENABLED = 'true'
$env:DMS_BOOTSTRAP_ADMIN_USERNAME = 'admin'
$env:DMS_BOOTSTRAP_ADMIN_PASSWORD = '<mat-khau-co-chu-va-so-tu-8-64-ky-tu>'
.\mvnw.cmd spring-boot:run
```

Mật khẩu bootstrap không có giá trị mặc định và không ghi log. Admin đầu tiên phải đổi mật khẩu. Sau lần tạo đầu, bỏ các biến bootstrap khỏi môi trường.

Đăng nhập từ SPA: GET `/api/auth/csrf`, gửi cookie `XSRF-TOKEN` trong header `X-XSRF-TOKEN` khi POST login, giữ cookie `SESSION`, rồi GET CSRF một lần nữa vì Spring xoay token sau đăng nhập. Frontend auth client đã được nối quy trình này.

## Kiểm tra

```powershell
.\mvnw.cmd verify
```

Test tích hợp mặc định dùng PostgreSQL Testcontainers và cần Docker. Nếu có một PostgreSQL **riêng cho kiểm thử**, có thể chạy:

```powershell
.\mvnw.cmd '-Ddms.test.external-db=true' '-Dspring.datasource.url=jdbc:postgresql://localhost:55432/postgres' '-Dspring.datasource.username=dms_test' '-Dspring.datasource.password=test' verify
```

Không trỏ test vào CSDL đang sử dụng: test khởi động chạy migration và tạo dữ liệu mẫu. Các case CRUD thông thường có transaction rollback. Kiểm tra xóa/khóa, constraint, quyền, trạng thái và session nằm trong `CrudApiIntegrationTest`; mapping/schema trong `DmsApplicationTests`; phụ thuộc module trong `ModuleDependencyTest`.

# Thiết kế triển khai khung CRUD backend

Ngày: 13/10/2026. Nhánh: `develop-quyennnv`.

Căn cứ: SRS v0.6, SAD v1.0, thiết kế CSDL v0.3, API Spec v0.4. Tham khảo HR-V5 ở cách chia Controller/DTO/Service interface/implementation/Repository; giữ stack và các đường dẫn của DMS. Phạm vi người dùng chốt: khung CRUD, chưa triển khai các luồng nghiệp vụ chi tiết.

Tài liệu triển khai, danh sách API thực tế, hướng dẫn chạy và việc còn lại nằm trong [README backend](../../DMS/backend/README.md).

## Phân chia trách nhiệm

- `common`: CRUD danh mục, DTO dùng chung, phân trang, lỗi, request ID, CurrentUser interface, ScopeGuard, cấu hình hệ thống, lịch sử và audit.
- `account`: tài khoản, hồ sơ, xác thực, session, bảo mật, đơn vị quản lý. `ManagementUnit` đặt ở account vì tài khoản Staff/Technician cần tra đơn vị và account không được phụ thuộc room; Controller vẫn giữ URL của API Spec.
- `room`: tòa, loại phòng, học kỳ, phòng, giường và lịch sử phân phòng. `RoomReferenceService` trả projection/ID cho module phía trên.
- `registration`: đợt, chỉ tiêu, đơn, diện ưu tiên; Policy đơn đăng ký.
- `maintenance`: ticket, Policy trạng thái, ảnh, ghi chú trong timeline.
- `billing`: CRUD chỉ số/đơn giá và hồ sơ hóa đơn chưa phát sinh; sổ cọc chỉ đọc.
- `contract`: CRUD hồ sơ hợp đồng chờ và yêu cầu gia hạn/chuyển phòng chờ; phiếu gửi đồ chỉ đọc.
- `notification`: hộp thư theo view, thông báo thủ công, listener sự kiện sau commit.

DTO dùng chung đặt trong `common/dto`, không đặt trong `common/api`; các module khác không sử dụng Controller/DTO của nhau. Entity không được trả trực tiếp ra API. Interface `ResidencyLookup` ở common do room triển khai, để account lấy cờ cư trú mà không tạo phụ thuộc ngược.

Các file `service/*Service.java` chỉ là interface: mỗi hàm có chú thích tiếng Việt nêu mục đích, quyền và điều kiện quan trọng. Mọi thân hàm nằm trong `service/impl/*ServiceImpl.java` và triển khai interface tương ứng bằng `implements`. `@Service` và `@Transactional` đặt ở Impl, giữ nguyên transaction khi tách lớp. Controller/các Service khác được inject bằng interface; không inject trực tiếp Impl. Interface không có hàm `default` hoặc hàm static chứa logic.

## Cơ chế cần giữ khi mở rộng

CRUD danh mục gồm `getAll`, `getById`, `create`, `update`, `deleteById`. `common/service/impl/AbstractCrudServiceImpl` thực hiện transaction, kiểm tra quyền qua hook bắt buộc, ghi audit và flush lỗi ràng buộc. Service cụ thể ánh xạ DTO và bổ sung kiểm tra nghiệp vụ; không dùng base này cho dữ liệu trạng thái hoặc giao dịch tài chính.

Các endpoint thao tác trạng thái gọi Policy sau khi khóa bản ghi. Trả 409 nếu hành động không còn hợp lệ; ghi lịch sử cùng transaction. Quyền tổng hợp không cho phép Staff ghi ngoài đơn vị mình, ngoại trừ phạm vi đợt đăng ký được API đặc tả.

Khóa phòng/giường không thay thế khóa của nghiệp vụ phân giường tương lai. Khi thêm phân giường, kích hoạt, chuyển/trả phòng phải dùng cùng thứ tự khóa đã có trong SAD: giường → hồ sơ sinh viên → hợp đồng → hóa đơn. Các thao tác mở rộng phải recheck sau khi lấy khóa và ghi giá chụp trên lịch sử phân phòng.

`common/service/impl/AbstractSqlCrudServiceImpl` là phần dùng chung cho một số hồ sơ khung: danh sách cột do server định nghĩa, giá trị dùng tham số SQL, không nhận tên bảng/cột tùy ý từ client. Mỗi Service vẫn kiểm tra quyền và điều kiện riêng trước khi ghi. `SqlPage` đếm tổng và phân trang sau khi áp dụng phạm vi. Hợp đồng/hóa đơn chỉ CRUD hồ sơ chờ chưa có dữ liệu phát sinh; sổ cọc, audit và lịch sử chỉ đọc. Các luồng chưa triển khai không được xuất hiện trong `allowedActions`.

## Khung React

`features/crud/resources.ts` khai báo endpoint, trường form, cột bảng, quyền và điều kiện trạng thái của từng màn. `CrudPage` dùng chung tìm kiếm, phân trang, thêm/sửa, xác nhận xóa/hủy và xem chi tiết. `features/crud/api.ts` gọi API và xử lý CSRF. Phân quyền ở UI chỉ điều khiển nút; backend luôn kiểm tra lại. Cấu hình màn và hướng dẫn chạy nằm ở [README frontend](../../DMS/frontend/README.md).

## Trạng thái bàn giao

Đã có mapping/Repository 33 bảng, phần nền và các API khung nêu trong README backend. CSDL V1/V2 không thay đổi. Các response chi tiết phức tạp, hành động liên module, import/export và job vẫn cần tiếp tục triển khai theo SRS; khung không tuyên bố hoàn thành MVP.

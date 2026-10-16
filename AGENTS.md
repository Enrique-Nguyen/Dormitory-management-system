# Quy tắc cho agent trong project DMS

- Đọc [CLAUDE.md](CLAUDE.md) trước khi sửa mã; áp dụng mục **Quy tắc bắt buộc: không hardcode** cho cả backend và frontend.
- Mọi lệnh shell phải bắt đầu bằng `rtk`; dùng `rtk proxy` khi chưa có wrapper phù hợp.
- Mã lỗi/role/quyền/trạng thái dùng Constants hoặc enum; HTTP status dùng `HttpStatus` hoặc factory `BusinessException`. Không viết lại mã chuỗi/số trong logic.
- Quyền động đọc từ CSDL; thông tin môi trường/secret đọc từ cấu hình; tham số nghiệp vụ đọc qua ConfigService. Giữ nguyên mã dữ liệu và migration đã áp dụng; đổi contract API phải đồng bộ caller, tài liệu và kiểm thử.
- Service là interface có JavaDoc tiếng Việt; logic nằm trong Impl, import ở đầu và xuống dòng rõ ràng.
- Áp dụng mục **Quy tắc nghiệp vụ và CRUD dùng chung** trong [CLAUDE.md](CLAUDE.md) khi sửa API, form, phân quyền hoặc dữ liệu. URL API viết trực tiếp trong annotation Controller; danh sách dùng `search-by-page`, tạo/sửa dùng chung `save-or-update`.
- Ưu tiên sửa file đang có, giữ các thay đổi người dùng đang làm và chạy kiểm chứng phù hợp trước khi hoàn tất.

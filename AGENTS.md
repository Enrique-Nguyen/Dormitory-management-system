# Quy tắc cho agent trong project DMS

- Đọc [CLAUDE.md](CLAUDE.md) trước khi sửa mã; áp dụng mục **Quy tắc bắt buộc: không hardcode** cho cả backend và frontend.
- Mọi lệnh shell phải bắt đầu bằng `rtk`; dùng `rtk proxy` khi chưa có wrapper phù hợp.
- Mã lỗi/role/quyền/trạng thái dùng Constants hoặc enum; HTTP status dùng `HttpStatus` hoặc factory `BusinessException`. Không viết lại mã chuỗi/số trong logic.
- Quyền động đọc từ CSDL; thông tin môi trường/secret đọc từ cấu hình; tham số nghiệp vụ đọc qua ConfigService. Giữ nguyên mã API và migration đã áp dụng khi refactor.
- Service là interface có JavaDoc tiếng Việt; logic nằm trong Impl, import ở đầu và xuống dòng rõ ràng.
- Ưu tiên sửa file đang có, giữ các thay đổi người dùng đang làm và chạy kiểm chứng phù hợp trước khi hoàn tất.

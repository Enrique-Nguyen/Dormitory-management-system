# DataTest cho DMS

Bộ dữ liệu giả để thử mọi màn hiện có của bốn cổng. Không chứa mật khẩu cố định, subject giả hoặc nhánh bỏ qua xác thực. Tài khoản được cấp tại Keycloak local; mã `DT-`, username `dt.*` và mã SV `9900260001`–`9900260040` dành riêng cho bộ này.

## Nạp dữ liệu

Yêu cầu Docker đang chạy các service trong `DMS/docker-compose.yml`, database đã được Flyway tạo và `DMS/.env` có secret service account Keycloak. Từ gốc repository, dùng Node.js 22 trở lên:

```powershell
node DMS/scripts/datatest/seed.cjs --apply
```

Mặc định kết quả nằm trong `DMS/backend/data/datatest/` (Git bỏ qua). Có thể chỉ định `--output <thư mục local>`. Máy Nguyễn Văn Séc đang dùng `D:\prjCDCNTT\dms-local\datatest`.

Script sao lưu cả DMS và Keycloak trước khi ghi, chuyển bộ `sample-data.sql` sang namespace riêng, nạp `extra.sql` rồi cấp danh tính thật. Không sửa file mẫu gốc, không xóa dữ liệu cũ, không bỏ constraint/trigger, không reset mật khẩu tài khoản có sẵn. Hai marker audit giúp chạy lại an toàn và giữ những thao tác đã thử trên UI; manifest `accounts.local` giúp tiếp tục nếu bước Keycloak bị gián đoạn. Giữ manifest này cùng database, không xóa để chạy lại từ đầu.

## Đăng nhập

Mở `http://localhost:5173/login`. Mật khẩu ngẫu nhiên nằm trong `accounts.local` tại thư mục kết quả, không nằm trong repository hoặc báo cáo.

| Tài khoản | Dùng để thử |
|---|---|
| `dt.admin` | Toàn bộ cổng Admin, import, tài khoản, quyền, cấu hình, audit |
| `dt.ql.1` / `dt.ql.2` | Staff K1 / N2+N3, kiểm tra phạm vi đọc và ghi |
| `dt.ql.tong-hop` | Staff K1 đọc liên đơn vị; vẫn chỉ ghi K1 |
| `dt.ql.khoa` | Staff bị khóa, đăng nhập phải bị từ chối |
| `dt.kt.1` / `dt.kt.2` | Công việc được giao ở hai đơn vị |
| `9900260001` / `9900260002` | Sinh viên nam/nữ đang ở; hóa đơn, cọc, ticket, thông báo |
| `9900260007` | Giữ chỗ, hợp đồng PENDING |
| `9900260008` | Tạm rời hè, còn đồ trong kho |
| `9900260009` | Đã trả phòng, hợp đồng TERMINATED, hoàn cọc |
| `9900260010` / `11` / `12` | Đơn SUBMITTED / APPROVED / DRAFT |
| `9900260013` / `14` | Đơn REJECTED / CANCELLED |
| `9900260015` / `16` | Hợp đồng EXPIRED / CANCELLED |
| `9900260017` | Hợp đồng sắp hết hạn |
| `9900260019` | Cọc thiếu và quá hạn bù; có số dư âm |
| `9900260021` | Lịch sử hè: đã nhận lại đồ và kết thúc hợp đồng |
| `9900260035` | Bắt đổi mật khẩu lần đầu tại Keycloak |
| `9900260036` | Sinh viên bị khóa |
| `9900260022`–`40` | Hồ sơ bổ sung để thử phân trang/tìm kiếm và trường hợp chưa có hợp đồng |

Các tài khoản thử thông thường dùng mật khẩu ngẫu nhiên không bắt đổi để tiện xem màn; riêng SV35 và mọi tài khoản tạo qua import thật vẫn có required action đổi mật khẩu. Cơ chế xác thực/phân quyền của ứng dụng giữ nguyên.

## Phạm vi dữ liệu

40 sinh viên, 3 tòa, 9 phòng, 42 giường, 4 học kỳ, 3 đợt; đủ trạng thái chính của đơn/hợp đồng, gia hạn/chuyển phòng, sửa chữa; 14 hóa đơn có chi tiết, gồm đã trả/chưa trả/quá hạn; sổ cọc có thu/trừ/bù/hoàn; lựa chọn hè và kho đã/chưa nhận đồ; thông báo theo tất cả phạm vi và audit append-only. Thống kê chính xác sau nạp nằm trong `summary.json`.

Số dư/công suất/quá hạn đọc từ view, không ghi giá trị suy diễn. Job có thể cập nhật dữ liệu khi đến lịch, nên số trạng thái có thể thay đổi sau khi nạp. Seed lại không khôi phục những gì người dùng đã sửa.

File Excel kiểm thử và kết quả chạy UI/API trên máy này nằm trong thư mục kết quả local; xem `README.md` ở đó. File hợp lệ dùng một dải mã riêng; file hỗn hợp có dòng hợp lệ, mã đã có, trùng trong file, sai điện thoại/ngày sinh/giới tính. Mật khẩu xuất từ import chỉ lưu ở file `.local` khi kiểm thử, không đưa vào ảnh chụp hoặc tài liệu.

Các màn của TV2–TV5 được nạp dữ liệu theo phần code đã có. Có dữ liệu hiển thị không có nghĩa mọi luồng xét duyệt/kích hoạt/thu tiền/báo cáo của các thành viên đã hoàn thành; đối chiếu ma trận kiểm tra trong `Docs/04 Testing`.

## Kiểm tra hồi quy import

`StudentImportIntegrationTest` dùng workbook Apache POI và PostgreSQL thật, có sáu ca bổ sung cho lỗi không nhận dữ liệu: mã số General/số mũ, giữ đệm 0 và mã Text 20 ký tự, từ chối số bị làm tròn/quá 15 chữ số, tiêu đề Unicode, dòng định dạng trống và biên 5.000/5.001 dòng có dữ liệu. Chạy riêng từ `DMS/backend`:

```powershell
.\mvnw.cmd -Dtest=StudentImportIntegrationTest test
```

Kiểm tra giao diện: tải mẫu, điền dữ liệu, chọn file rồi bấm **Kiểm tra dữ liệu**. File hợp lệ hiển thị tổng số và thông báo đọc thành công; bảng chỉ có dòng lỗi/mã trùng nên không xuất hiện khi tất cả dòng hợp lệ. Đặt mã dài hơn 15 chữ số, mã có số 0 đầu và điện thoại ở dạng Text. Sau khi xác nhận ghi, tải tài khoản trước khi đổi file; đổi qua lại tab Hồ sơ sinh viên không làm mất kết quả. File thiếu cột phải báo rõ tên cột; sau lỗi ghi cần kiểm tra lại trước khi gửi lô mới.

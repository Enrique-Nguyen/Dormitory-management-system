# Nhận xét cấu trúc thư mục Docs (10/10/2026)

Đọc từ máy: `D:\Uni\4th year\Chuyên đề CNTT\Docs` (chỉ đọc, chưa đổi gì).

## 1. Hiện trạng

```
Docs/
├── External Design/
│   ├── API Specification/              (trống)
│   ├── Database Design Documents/      DB-Design-DMS.md, DB-DMS-schema.sql, DB-ERD-1.png, DB-ERD-2.png
│   └── Software Architecture Document/ (trống)
├── Internal Design/
│   └── Detailed Design Document/       (trống)
├── Presentation/                       DMS-thuyet-trinh.html, he-thong-quan-ly-ky-tuc-xa-mini.md
└── Requirements Documentation/
    ├── SRS/                            SRS-DMS.md
    └── UI UX design/                   DMS-UI-UX-Design.md
```

Ngoài Docs/ còn `DMS/` (trống, chắc để chứa mã nguồn) và `CLAUDE.md`.

## 2. Cái đã đúng

- Chia theo giai đoạn Yêu cầu → Thiết kế ngoài → Thiết kế trong là đúng mô hình tài liệu chuẩn (kiểu Basic Design / Detailed Design).
- Thiết kế CSDL nằm trong External Design là hợp lý: nó mô tả bảng, quan hệ, ràng buộc mà mọi phân hệ dùng chung.
- Đã chừa chỗ cho Kiến trúc phần mềm và Đặc tả API: đúng hai tài liệu External Design còn thiếu.
- Tách `Docs/` khỏi `DMS/` (mã nguồn) là đúng.

## 3. Cần chỉnh

1. **UI/UX design đặt sai giai đoạn.** `DMS-UI-UX-Design.md` mô tả từng màn hình, bố cục, thành phần, thông báo lỗi, luồng chuyển màn. Đó là *thiết kế màn hình*, thuộc External Design, không phải tài liệu yêu cầu. Requirements chỉ nên giữ SRS (phần yêu cầu giao diện ở SRS mục 6 là đủ).
   - Mục 27 của file đó (stack, cấu trúc component, chia màn theo thành viên) thực chất là Internal Design. Chưa cần tách ngay; khi viết Detailed Design thì chuyển sang.
2. **`he-thong-quan-ly-ky-tuc-xa-mini.md` không phải slide.** Đó là bản đề xuất đề tài (bối cảnh, kiến trúc, workflow, mô hình dữ liệu sơ bộ). Nên chuyển sang thư mục quản lý dự án. Presentation chỉ giữ slide.
3. **Thiếu thư mục Review.** Hai file nhận xét (`nhan-xet-thiet-ke-DMS.md`, `nhan-xet-UI-UX.md`) hiện chỉ có trên cloud, máy chưa có chỗ chứa.
4. **Thiếu thư mục cho tài liệu sắp có:** kiểm thử (kế hoạch test, test case, báo cáo test, kiểm thử tải), quản lý dự án (đề xuất, kế hoạch 12 tuần, phân công 5 thành viên, biên bản họp), và về sau hướng dẫn sử dụng.
5. **Tên thư mục chưa đồng nhất:** "Requirements Documentation", "Database Design Documents", "Software Architecture Document", "UI UX design" (viết thường). Đề xuất bỏ chữ Documents/Documentation thừa, viết hoa đồng nhất, và thêm số thứ tự để thư mục sắp đúng trình tự dự án.
6. **Thư mục con chỉ chứa một file** (`SRS/`) làm sâu thêm một tầng không cần. Với quy mô đồ án, SRS để thẳng trong `01 Requirements/` là gọn hơn.
7. **`CLAUDE.md` đã cũ:** bảng file còn ghi đường dẫn phẳng và SRS v0.4. Cập nhật sau khi chốt cấu trúc.

## 4. Cây đề xuất

```
Docs/
├── 00 Project Management/
│   ├── he-thong-quan-ly-ky-tuc-xa-mini.md     (đề xuất đề tài, chuyển từ Presentation)
│   └── (kế hoạch, phân công, biên bản họp: sau này)
├── 01 Requirements/
│   └── SRS-DMS.md
├── 02 External Design/
│   ├── Software Architecture/                 SAD (cần viết)
│   ├── Screen Design/                         DMS-UI-UX-Design.md (chuyển từ Requirements)
│   ├── Database Design/                       DB-Design-DMS.md, DB-DMS-schema.sql, DB-ERD-1.png, DB-ERD-2.png
│   └── API Specification/                     (cần viết)
├── 03 Internal Design/
│   └── Detailed Design/                       (sau External Design)
├── 04 Testing/                                (kế hoạch test, test case, báo cáo)
├── 05 Reviews/
│   ├── nhan-xet-thiet-ke-DMS.md
│   └── nhan-xet-UI-UX.md
└── 06 Presentation/
    └── DMS-thuyet-trinh.html
```

Nếu muốn giữ đúng tên bạn đã đặt thì chỉ cần làm ba việc bắt buộc: chuyển UI/UX sang External Design, chuyển file đề xuất ra khỏi Presentation, thêm thư mục Reviews.

## 5. External Design: đã có gì, còn thiếu gì

| Tài liệu | Trạng thái | Nguồn sẵn có |
|---|---|---|
| Thiết kế CSDL | **Xong** (v0.2: 33 bảng, 6 view, test PG16 đạt) | DB-Design-DMS.md + schema.sql + ERD |
| Thiết kế màn hình | **Gần xong** (đã xử lý hết nhận xét 08/10) | DMS-UI-UX-Design.md |
| Kiến trúc phần mềm (SAD) | **Thiếu** | Rải rác ở đề xuất mục 4, SRS mục 2 và 7, CLAUDE.md: modular monolith 7 module, Controller→Service→Repository, React SPA, PostgreSQL, Docker Compose, session cookie, job định kỳ |
| Đặc tả API | **Thiếu, lớn nhất** | Quy ước ở SRS 6.2 và UI 27.2 (`/api`, `POST /{id}/<action>`, `errorCode`, `allowedActions`); chưa có danh sách endpoint, request/response, bảng mã lỗi |

Nội dung đề xuất cho hai tài liệu còn thiếu:

- **SAD:** sơ đồ ngữ cảnh và thành phần; 7 module và ranh giới phụ thuộc giữa chúng; phân tầng backend; cấu trúc frontend; xác thực, phiên, RBAC theo phạm vi ManagementUnit; giao dịch và khóa (khóa giường, chuyển phòng theo thứ tự bed_id); job định kỳ idempotent; mô hình triển khai Docker Compose; ánh xạ NFR (hiệu năng, bảo mật) sang quyết định kiến trúc.
- **API Specification:** quy ước chung (prefix, phân trang, định dạng lỗi, `allowedActions`, mã HTTP 400/404/409); danh sách endpoint theo module kèm vai trò được gọi và FR tương ứng; schema request/response cho các nghiệp vụ chính; **danh mục `errorCode`** (để frontend dịch sang tiếng Việt); định dạng file Excel import. Có thể kèm `openapi.yaml` để sau sinh code/Swagger.

Thứ tự đề xuất: SAD trước (ngắn, chốt ranh giới module), rồi API Specification (dựa trên SRS FR + CSDL v0.2 + màn hình đã có).

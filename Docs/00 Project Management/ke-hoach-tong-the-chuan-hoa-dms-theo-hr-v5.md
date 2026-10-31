# Kế hoạch tổng thể chuẩn hóa DMS theo mẫu tham khảo HR-V5

> Ngày lập: 31/10/2026  
> Nền khảo sát: `develop-quyennnv` tại `266f588`  
> Trạng thái cập nhật 31/10/2026: **AUDIT_IN_PROGRESS — đang review/sửa theo owner; chưa nghiệm thu toàn hệ thống**. #37/#38 ở REVIEW; gate browser/chức năng NOT_VERIFIED.  
> Phạm vi: giao diện bốn vai trò, tùy chọn giao diện theo tài khoản, danh sách/xem/thêm/sửa/xóa, cấu hình, quy tắc dữ liệu, liên kết nghiệp vụ và quy trình bàn giao cho developer/agent.  
> Cách lập ban đầu: bốn agent khảo sát chỉ đọc HR-V5/DMS; đối chiếu SRS, SAD, API, migration và component đang có; không chạy ứng dụng/test trong lượt lập ban đầu. Các đợt test và audit browser về sau ghi riêng ở mục 13, không hồi tố thành bằng chứng baseline.  
> Giới hạn lượt chi tiết hóa này: chỉ sửa tại chỗ file kế hoạch; không sửa code, ma trận truy vết, manifest, cấu hình hay tạo tài liệu mới. Owner đang sửa code bàn giao kết quả cho agent chính bổ sung sau.  
> Git: chưa commit/push. Stash `8aef7edff66a79bfc8a827cf52f8344c032e71b7` giữ nguyên, không áp dụng lại hoặc xóa.  
> Phụ trách thiết kế đã được người dùng chốt ngày 31/10/2026: **Qoder + reviewer độc lập**, không chờ Anti. Anti chưa được cấu hình, không giả nhận đã dispatch; nếu bổ sung sau phải giao phạm vi riêng. Quyết định phân công không đồng nghĩa các ticket triển khai đã bắt đầu.

## 1. Yêu cầu và kết quả phải đạt

1. Tham khảo HR-V5 từ mã thực tế, không sao chép nguyên framework hoặc mặc định mọi cách làm ở HR-V5 đều đúng.
2. Làm lại trải nghiệm giao diện toàn DMS theo một ngôn ngữ thống nhất: bố cục, màu, bảng, bộ lọc, biểu mẫu, thông báo, trạng thái và điều hướng dễ đọc.
3. Xem/Sửa cùng một bản ghi chỉ mở popup, không điều hướng sang trang detail; cả hai mode giữ cùng nhóm, thứ tự, dữ liệu, field bất biến, lịch sử và bảng con. VIEW không có mutation; action nghiệp vụ ở mode xử lý riêng theo quyền/allowedActions (mục 4.2).
4. Có tùy chọn màu/giao diện theo **từng tài khoản**, không để tài khoản B nhận cấu hình của tài khoản A trên máy dùng chung.
5. Mỗi bảng dữ liệu có owner, quy tắc tạo/xem/sửa/xóa, quyền/phạm vi, trạng thái, ràng buộc, audit/lịch sử và cách kiểm thử.
6. Các module nối thành luồng nghiệp vụ hoàn chỉnh; không làm đẹp từng trang nhưng làm mất liên kết, quyền, dữ liệu hoặc trạng thái giữa các trang.
7. Tên hàm, biến, component và hook thể hiện rõ mục đích. Mỗi ticket có ghi chú dev đủ để người khác hiểu lý do, quy tắc và cách kiểm chứng.
8. Chia việc cho nhiều agent có phạm vi file riêng, có reviewer độc lập, có bàn giao và kiểm tra chéo; không để nhiều agent cùng sửa file chung.
9. Chỉ đánh dấu hoàn thành khi có kết quả thực tế. Có code, build pass, API pass và E2E pass là các trạng thái khác nhau.

### Ngoài phạm vi tự động của kế hoạch

- Không đổi hệ quản trị PostgreSQL, hệ xác thực Keycloak, framework React/Ant Design, hoặc kiến trúc modular monolith chỉ để giống HR-V5.
- Không tự thêm payment gateway, realtime/email, auto-assignment hoặc chức năng ngoài SRS.
- Không thêm `voided`, `deleted`, `version`, BaseEntity hay CRUD cho mọi bảng bằng một migration đại trà.
- Không đổi mã role/status/permission/error đã công bố hoặc migration V1–V7; V8 đã áp dụng ở môi trường test cũng phải giữ nguyên. Mọi migration đã chạy đều không sửa/xóa, không chỉ V1–V7.
- Không tự cài thư viện, sửa quyền môi trường, CI, AGENTS/CLAUDE hoặc phát hành thay đổi từ việc lập kế hoạch này.
- Không tự phục hồi phần code trong stash. Việc đưa từng thay đổi cũ vào phải có ticket, review diff và kiểm thử riêng.

## 2. Nguồn chuẩn và hiện trạng đã xác nhận

### 2.1. Thứ tự tài liệu

SRS → SAD/DB Design/API Spec → UI Design → kế hoạch triển khai → báo cáo kết quả. Kế hoạch không thay thế quy tắc nghiệp vụ trong SRS. Nếu tài liệu mâu thuẫn, tạo quyết định nghiệp vụ và cập nhật tài liệu liên quan trước khi đổi hành vi.

- [SRS](../01%20Requirements/SRS-DMS.md): FR, BR, trạng thái, NFR và AT-01–AT-33.
- [SAD](../02%20External%20Design/Software%20Architecture/SAD-DMS.md): module, port/event, transaction/lock và ownership.
- [API Spec](../02%20External%20Design/API%20Specification/API-Spec-DMS.md): endpoint, payload, error codes, allowedActions.
- [DB Design](../02%20External%20Design/Database%20Design/DB-Design-DMS.md) và migration: schema thực tế phải kiểm tra từ Flyway, không chỉ snapshot.
- [UI Design](../02%20External%20Design/Screen%20Design/DMS-UI-UX-Design.md): giao diện và shared component hiện có.
- [Quy tắc code](../../CLAUDE.md): constants, CRUD V5, phân quyền, giao dịch, form, kiểm thử.
- [Kế hoạch TV3](ke-hoach-hoan-thien-TV3.md), [kế hoạch TV4](ke-hoach-hoan-thien-TV4.md): giữ làm nguồn truy vết; không xóa hoặc tick lại theo cảm giác.
- [Audit hiện có](../AUDIT-REPORT-DMS.md): đọc cả phần đính chính/remediation, không lấy riêng điểm số hoặc phát hiện cũ làm kết luận hiện tại.

### 2.2. Nền DMS hiện có — từ source, không phải chứng nhận runtime

| Hạng mục | Bằng chứng | Ý nghĩa cho kế hoạch |
|---|---|---|
| Bốn portal | `DMS/frontend/src/app/router/index.tsx:13` và các route role | Rollout phải kiểm cả Student/Staff/Technician/Admin; Admin tái dùng màn Staff. |
| 29 resource definitions | `DMS/frontend/src/features/crud/resources.ts:284` | Kiểm từng resource và màn chuyên biệt; số resource không phải số màn đã nghiệm thu. |
| Xem và sửa đang dùng các renderer khác nhau | `DMS/frontend/src/pages/CrudPage.tsx:1300`: invoice summary riêng, tài nguyên khác là form disabled | Cần canonical field/group và renderer theo mode; không chỉ đổi màu CSS. |
| Audit có detailColumns nhưng không có fields | `DMS/frontend/src/features/crud/resources.ts:1055` | Kiểm tra detail phải hiện dữ liệu trước/sau; không để mode xem thành form trống. |
| Có nền shared controls/layout | `DMS/frontend/src/components/ui/Surface.tsx:16`, `FormControls.tsx:20` | Mở rộng nền sẵn có, không tạo bộ UI song song hoặc import primitive rải rác. |
| Theme tĩnh | `DMS/frontend/src/app/App.tsx:17`, `app/theme.ts:117` | Hiện chưa có state/persistence theme theo user trong nền này. |
| 36 bảng khai báo trong migration V1–V7 | V1: 33 bảng; V2: 2 bảng session; V5: 1 bảng quyền | Đây là baseline 36 bảng do ứng dụng khai báo, gồm cả bảng platform, không phải toàn bộ đều là bảng nghiệp vụ. |
| V8 bổ sung `user_preference` | Log PREF targeted ghi schema version 8 trên PostgreSQL test riêng, mục 13.2 | Tổng sau V8: 37 bảng ứng dụng; 38 base table nếu tính cả `flyway_schema_history`. Không suy ra V8 đã áp dụng ở mọi môi trường. |
| CI có backend/frontend gates | `.github/workflows/ci.yml:22`, `:36` | Phải dùng kết quả trên SHA triển khai; không dùng kết quả lịch sử thay nghiệm thu hiện tại. |

**Giới hạn bằng chứng:** bảng này giữ snapshot source lúc lập; số dòng nguồn có thể dịch khi owner sửa code. Lượt lập ban đầu chưa có browser; audit sau đó đã thấy lỗi thật tại Edge `/admin/summer` (mục 13.3), không chứng nhận toàn UI. Lượt chi tiết hóa tài liệu không chạy lại ứng dụng/test. Không công bố phần trăm “production-ready”; chỉ báo từng FR/AT/gate đã có bằng chứng trên đúng revision.

## 3. HR-V5: tham khảo điều gì, không sao chép điều gì

| Mẫu thực tế | Nguồn đã đọc | Áp dụng cho DMS | Điểm phải làm tốt hơn/không sao chép |
|---|---|---|---|
| Tạo/sửa/xem chung form | [ShiftWorkFormDialog.tsx:104](file:///D:/HR-V5/frontend/src/features/shift-work/components/ShiftWorkFormDialog.tsx#L104) | Dùng chung group/field/mapping; mode create/edit/view rõ ràng. | Không chép UI framework, cách tính ca hoặc nghiệp vụ nhân sự. |
| View/edit dùng cùng dialog, quyền action riêng | [ShiftWorkListPage.tsx:257](file:///D:/HR-V5/frontend/src/features/shift-work/pages/ShiftWorkListPage.tsx#L257) | Một entry point detail, action xem/sửa/hủy/xóa đúng quyền. | Giữ paging DMS từ 0 ở API; không mang quy ước API HR từ 1 sang. |
| Bảng và confirm dùng chung | [data-table.tsx:36](file:///D:/HR-V5/frontend/src/shared/ui/data-table.tsx#L36), [ConfirmDialog.tsx:103](file:///D:/HR-V5/frontend/src/shared/components/ConfirmDialog.tsx#L103) | Đồng nhất toolbar, bảng, loading, destructive confirm và khóa gửi lặp. | Component tốt không bảo đảm mọi caller dùng đúng; phải test caller. |
| Theme store + CSS variables | [themeStore.ts:27](file:///D:/HR-V5/frontend/src/shared/theme/themeStore.ts#L27) | Một state/theme adapter cho AntD và CSS variables. | Khóa `hr-v5-theme` chung tài khoản không được sao chép sang DMS. |
| Theme mode/accent/preset gửi backend | [themeStore.ts:96](file:///D:/HR-V5/frontend/src/shared/theme/themeStore.ts#L96), [UserSettingServiceImpl.java:32](file:///D:/HR-V5/backend/hr-v5-api/src/main/java/com/globits/hr/service/impl/UserSettingServiceImpl.java#L32) | API “của tôi”, chủ sở hữu từ server, hydrate sau xác thực. | HR chỉ gửi 3 field, màu custom/gridline vẫn local; không có whitelist DTO đầy đủ và một số lỗi lưu bị bỏ qua. |
| Bảng cấu hình user có FK unique | [UserSetting.java:7](file:///D:/HR-V5/backend/hr-v5-api/src/main/java/com/globits/hr/domain/UserSetting.java#L7) | Cân nhắc preference 1–1 với user nếu DMS cần đồng bộ đa thiết bị. | Entity HR không chứng minh migration/DB triển khai; DMS phải có migration mới, validation và upgrade test. |
| Cấu hình nghiệp vụ có key/codec | [SystemConfigServiceImpl.java:75](file:///D:/HR-V5/backend/hr-v5-api/src/main/java/com/globits/hr/service/impl/SystemConfigServiceImpl.java#L75) | Phân biệt config nghiệp vụ, config kỹ thuật và sở thích cá nhân. | Không gom tất cả vào một Map tùy ý hoặc cho user đổi giá/quyền qua settings. |
| Xóa mềm `voided` ở ca làm việc | [ShiftWorkServiceImpl.java:509](file:///D:/HR-V5/backend/hr-v5-api/src/main/java/com/globits/hr/service/impl/ShiftWorkServiceImpl.java#L509) | Chỉ học UX confirm, dependency và thông báo kết quả. | Không áp `voided` cho audit, ledger, history hoặc mọi bảng DMS; pattern HR cũng có góc cần kiểm tra phụ thuộc. |

Các nhận xét HR ở đây là kết quả đọc mã trong phạm vi tham khảo, không phải audit toàn HR hoặc kết luận lỗi đã tái hiện runtime. Không chỉnh bất kỳ file nào của HR-V5.

## 4. Quy chuẩn UX mục tiêu

### 4.1. Một ngôn ngữ hiển thị cho toàn ứng dụng

- Giao diện quản trị nghiệp vụ gọn, sáng rõ, ưu tiên dữ liệu; giữ nhận diện DMS/TLU, không sao chép dashboard HR nguyên xi.
- Sidebar nhóm theo công việc; header gồm vị trí hiện tại, breadcrumb, thông báo và tài khoản. Màn sâu phải có đường quay lại đúng portal và bộ lọc trước đó.
- Trang danh sách: tiêu đề/mô tả → thống kê ngắn nếu có ích → bộ lọc/tìm kiếm → bảng/cards → pagination. Không lặp nhiều tiêu đề/card viền lồng nhau.
- Trang detail: header nhận diện/trạng thái → thông tin chính → thông tin liên quan → history/audit → action đúng ngữ cảnh. Đưa điều quan trọng lên trước, không bắt người dùng tìm nút qua nhiều vùng rời rạc.
- Một primary action chính cho mỗi ngữ cảnh; action nguy hiểm tách khỏi lưu/đóng. Quá nhiều action dùng overflow có nhãn rõ.
- Dùng token spacing/type/radius/surface/border/shadow trong nguồn chung; không hardcode màu hoặc khoảng cách đặc biệt tại từng màn.
- Chốt thiết kế trước bằng ít nhất ba pilot: danh mục đơn giản, form có quan hệ/bảng con, luồng hợp đồng/tiền cọc. Qoder thiết kế và reviewer độc lập kiểm tra; phải có before/after cùng viewport, không chỉ ảnh mock không gắn với dữ liệu thật.
- Không coi việc đổi toàn bộ palette là hoàn thành UX nếu luồng xem/sửa, lỗi, điều hướng hoặc quyền vẫn chưa đúng.

### 4.2. Hợp đồng xem–thêm–sửa

**Quyết định UX bắt buộc:** nút Xem/Sửa của cùng bản ghi mở popup Modal/Drawer ngay tại danh sách hoặc ngữ cảnh gọi; không đổi URL sang trang detail. Overview inline của `profile`/`configs`/`myRoom` không thay thế popup chi tiết khi có thao tác Xem/Sửa. Không nối lại `detailRoutes` chỉ để làm thuộc tính hết unused. Route nghiệp vụ hiện hữu chỉ giữ cho tác vụ xử lý chuyên biệt/deep link hợp lệ, không dùng làm đường vòng thay yêu cầu popup.

| Thành phần | Thêm | VIEW trong popup | EDIT trong cùng pattern popup |
|---|---|---|---|
| Nhóm, nhãn, thứ tự, dữ liệu | Cấu trúc canonical; phần chưa phát sinh ghi rõ | Toàn bộ nhóm của bản ghi, gồm nguồn tham chiếu/hệ thống/history/bảng con | Chính các nhóm, thứ tự và dữ liệu của VIEW; không mất phần chỉ đọc vì dùng `editFields` |
| Dữ liệu nguồn | Default hợp lệ theo nghiệp vụ | Detail response của đúng ID/scope; dữ liệu danh sách chỉ dùng khi đủ contract đã kiểm | Nạp cùng detail/mapper trước mở nhập; không lấy form trống/default đè bản ghi |
| Hiển thị | Control nhập hợp lệ | Text/readonly có thể đọc/copy, không làm mờ như disabled | Control cho field được ghi; phần bất biến giữ readonly cùng vị trí |
| Field bất biến/hệ thống | Không cho client tự khai | Hiện mã/tên nguồn, trạng thái, thời điểm theo đúng semantics API | Hiện y nguyên; loại khỏi payload hoặc giữ định danh đúng contract, không mở quyền sửa |
| History/bảng con/ảnh | Theo nghiệp vụ khi tạo | Đủ phần được phép đọc; có loading/empty/error/retry riêng | Vẫn hiển thị đủ, cùng thứ tự; lịch sử/ledger/ảnh cũ không thành input sửa/xóa |
| Validation/mutation | Theo nhánh tạo | Không submit/validation lưu; không save/approve/payment/delete/cancel hay mutation khác | Validation theo nhánh sửa; thiếu/null giữ dữ liệu theo contract; chỉ gửi whitelist |
| Footer/action | Hủy / Lưu | Đóng; Chuyển sang sửa hoặc Mở xử lý nếu hợp lệ, chỉ đổi mode, không ghi | Hủy / Lưu; workflow không chạy ngầm cùng lưu |

Tập canonical là hợp của field hiển thị hợp lệ trong detail, fields/editFields, detailColumns và section module, không chỉ các input có thể sửa. Mapping phải chỉ ra nguồn API và kiểu định dạng cho từng field. Nếu API thiếu phần cần thiết, ghi blocker/đề nghị contract cho owner; không tạo ngày/nhãn/lịch sử giả từ UI. `createdAt` không thay `submittedAt`; giá trị null ghi “Chưa có” đúng nghĩa, không suy đoán thời gian gửi.

**Tách workflow:** dùng mode/hộp thoại xử lý riêng với confirm/reason theo nghiệp vụ; nút lưu/duyệt/phân giường/kích hoạt/thanh toán/xóa chỉ xuất hiện ngoài VIEW. Chuyển mode phải giữ đúng ID và đọc lại quyền/trạng thái cần thiết. Quyền hiện hành + ownership/scope + `allowedActions` hợp lệ vẫn được bảo toàn; backend tái kiểm sau khóa. Không ẩn vĩnh viễn action hợp lệ để làm VIEW có vẻ an toàn. Hợp đồng, phân giường, trả phòng, ảnh ticket và invoice items vẫn dùng section/component module, không dồn policy vào CrudPage.

**Điều kiện bắt buộc:** giữ `false`, `0`, tiền/ngày/lookup; ưu tiên nhãn từ detail hoặc lookup đúng scope kể cả inactive, không dùng số ID trần thay tên. Nếu nguồn đã mất/không đọc được, ghi rõ tình trạng và mã tham chiếu, không giả làm option còn hợp lệ. Mở edit lỗi có retry, không form trống; save lỗi giữ draft; chặn gửi kép; đang ghi không đóng bằng X/Escape/mask; cảnh báo draft chưa lưu; focus đúng field lỗi và trả về trigger khi đóng. Kịch bản chi tiết ở mục 12.3.

### 4.3. Danh sách, lọc, bảng và action

- Giữ tìm kiếm/phân trang ở server, thống nhất total/count/sort; không lọc một trang dữ liệu tại frontend rồi coi là toàn bộ.
- STT, action, tên/mã, trạng thái, tiền và ngày có vị trí/căn lề nhất quán; tiền/số căn phải, nhãn/trạng thái có text, không chỉ dựa màu.
- Bộ lọc có nhãn, mặc định và nút xóa rõ; giữ q/filter/sort/page/size theo resource/tab qua detail → back → refresh.
- Detail mở từ bảng, thông báo, dashboard và pending items phải đến cùng tài nguyên và đúng portal/scope.
- Mobile: không tràn toàn trang; bảng rộng chỉ cuộn trong vùng bảng hoặc dùng cards theo module. Không ép 15 cột vào 360 px.
- View/edit/delete hiển thị theo quyền API, ownership, trạng thái và allowedActions; frontend không thay policy của backend.
- Không có action “xóa” chung cho mọi tài nguyên. Confirm phải nêu đối tượng, hậu quả, lý do nếu bắt buộc và phương án thay thế như khóa/ngừng sử dụng/hủy.

### 4.4. Trạng thái và khả dụng

Mỗi màn có initial loading, background loading, empty có hướng dẫn, field/business error có recovery, mất quyền, 404, 409 stale state và lưu thành công. Không nuốt lỗi hoặc đóng modal trước khi API thành công. Giữ `requestId` cho hỗ trợ lỗi nhưng không hiện stack trace/secret.

- Desktop chuẩn: 1440×900; kiểm thêm 1280 px.
- Tablet: 768 px; drawer/menu/toolbar không che nội dung.
- Mobile: 360 px, kiểm thêm 390 px và landscape; touch target tối thiểu 44×44 px.
- Contrast WCAG AA: chữ thường ít nhất 4.5:1; nội dung không chỉ phân biệt bằng màu; focus nhìn thấy; label/aria đầy đủ.
- Tab/Shift-Tab/Enter/Escape; modal focus trap đúng, không kẹt bàn phím; tôn trọng reduced motion và zoom 200%.

## 5. Cấu hình giao diện theo người dùng

### 5.1. Phân loại cấu hình

| Loại | Nguồn chuẩn | Ai được sửa | Ví dụ |
|---|---|---|---|
| Kỹ thuật | Properties/env triển khai | Người vận hành theo quy trình | URL/port/realm/client/timeout; secret không vào frontend/Git |
| Nghiệp vụ | `system_config` qua ConfigService | Quyền quản trị hiện có | Hạn kích hoạt, hạn bù, thời gian nhắc; không hardcode trong UI |
| Danh mục có ngày hiệu lực | Bảng nghiệp vụ owner | Quyền module | Giá điện/nước, mức cọc theo tòa, học kỳ |
| Sở thích người dùng | Preference của chính tài khoản | Người đang đăng nhập | Theme mode, accent/preset, viền bảng nếu được chốt |
| Trạng thái theo thiết bị | Cache có namespace user + resource | Chính user trên thiết bị | Sidebar thu gọn, bộ lọc, độ rộng cột; không giả định đồng bộ đa thiết bị |

### 5.2. Phương án đề xuất

1. Backend là nguồn chuẩn cho sở thích cần theo tài khoản/đa thiết bị. API `me` lấy user từ `CurrentUser`, không nhận userId/role/permission trong body.
2. DTO typed và whitelist: nhóm `themeMode`, `themePreset`, `themeAccent`; nếu cho chọn custom color thì validate màu, lưu đủ field custom và kiểm contrast trước khi chấp nhận. Không nhận CSS/HTML/JS tùy ý.
3. AntD ConfigProvider và CSS variables dùng chung một nguồn theme. Áp lên root để modal/drawer/tooltip portal không lệch màu.
4. Có preset được duyệt, hoàn nguyên về mặc định, preview trước lưu và trạng thái đang lưu/đã lưu/lỗi. Màu semantic success/warning/danger không bị tùy chọn accent đổi nghĩa.
5. Dùng API lưu thống nhất convention DMS: endpoint đề xuất `GET /api/me/preferences`, `POST /api/me/preferences/save-or-update`; đây là **contract dự kiến**, phải kiểm endpoint hiện có và chốt trong ticket trước khi thêm.
6. Tái dùng PROFILE_READ/PROFILE_UPDATE nếu phù hợp ma trận đã duyệt; nếu cần quyền mới phải qua quyết định riêng, không tự sửa quyền runtime. Lưu màu/giao diện không được đi qua luồng đổi role/đơn vị/khóa hoặc tự tăng `securityVersion` và đăng xuất tài khoản.
7. Tái dùng entity/service hiện có nếu không trộn trách nhiệm; nếu cần bảng 1–1 preference thì unique/FK theo user, version chống ghi đè, audit theo chính sách được chốt, migration **Vn+1**. Tại nền khảo sát có V1–V7 nhưng phải kiểm lại n lúc triển khai, không đóng đinh tên V8.
8. Local cache chỉ chứa lựa chọn không nhạy cảm, namespace theo môi trường/realm và ID user bất biến + schema version. Không lưu token/mật khẩu. User chưa xác định dùng giao diện công khai mặc định, không đọc cache của user trước.
9. Đổi user/logout hủy debounce/request/subscription đang chờ, bỏ state user cũ và không gửi muộn bằng identity mới. Race response của user A không được hydrate user B.
10. Đồng bộ nhiều tab chỉ cho đúng account; xử lý version conflict và lỗi mạng rõ. Không silent-fail việc người dùng vừa bấm lưu, không báo “đã lưu” khi server thất bại.
11. Fresh install/upgrade phải có default an toàn cho user chưa có preference. Không dùng thiếu/corrupt preference để nới quyền hoặc làm app không thể đăng nhập.

### 5.3. Nghiệm thu bắt buộc

- A chọn màu → refresh → mở thiết bị khác vẫn đúng nếu preference thuộc loại đồng bộ.
- A logout, B login trên cùng browser: không nhận màu/sidebar/table cache riêng của A; API lỗi của B không được lấy cache A làm fallback.
- Hai tab cùng user, hai tài khoản khác nhau và hai cửa sổ cùng browser đều kiểm tra đúng isolation.
- Đổi preset/mode/custom giữ đủ dữ liệu sau round-trip; popup/table/chart/header không sót palette cũ.
- Phiên/token hết hạn trong lúc debounce không làm lưu nhầm tài khoản; request thất bại có retry có kiểm soát.
- Tất cả preset hợp lệ được đo contrast; màu không hợp lệ hoặc field lạ bị backend từ chối.

## 6. Quy tắc cho toàn bộ dữ liệu

Manifest `DMS-DATA-01` giữ baseline **36/36 bảng V1–V7**, kèm 6 view, 3 sequence sinh mã và trigger đã inventory; không đổi các mã T01–T36. V8 thêm `user_preference` là bảng ứng dụng thứ 37, mục NEW riêng; tổng base table là **38 khi tính `flyway_schema_history`**. Không nhầm 38 bảng với 38 bảng nghiệp vụ. Source inventory và migration đã áp dụng là hai loại bằng chứng khác nhau; đối chiếu schema thật từng môi trường được phép trước rollout. Agent chính sẽ đồng bộ manifest sau, lượt này không sửa manifest.

### 6.1. Ma trận vòng đời theo owner

| Owner | Bảng | Quy tắc kế hoạch bắt buộc |
|---|---|---|
| account | `management_unit` | Danh mục; kiểm tham chiếu trước xóa, không xóa đơn vị đang dùng. |
| account | `app_user`, `student_profile` | Tài khoản có nghiệp vụ dùng khóa, không xóa lịch sử; role/unit/lock thay đổi phải thu hồi phiên. Dữ liệu chưa dùng chỉ xóa nếu policy cho phép. |
| account (NEW/V8) | `user_preference` | 1–1 theo user server; typed whitelist/version/isolation; không API xóa tùy ý, không tự xóa bảng hoặc migration đã áp dụng. |
| common | `system_config` | Chỉ sửa key hiện có, typed value qua ConfigService; không thêm/xóa key tùy ý từ UI. |
| common/security | `role_permission_matrix` | Quyền động, version/lock/audit; ADMIN bất biến, không tự tạo dòng để thu hồi quyền ADMIN. |
| room | `building`, `semester` | FK/scope, điều kiện tham chiếu/ngày không chồng; chỉ xóa bản chưa dùng. |
| room | `room_type` | Giữ active và snapshot giá; không đổi bed_count khi đã gán phòng theo SRS. |
| room | `room`, `bed` | Phân biệt trạng thái vận hành và chiếm chỗ; đã có lịch sử dùng action/ngừng phù hợp, không hard delete. |
| room | `room_assignment_history` | Chỉ BedOccupancyService ghi/đóng/mở; cùng transaction, giữ khóa và unique index; không CRUD lịch sử. |
| registration | `registration_period`, `registration_period_unit`, `registration_quota` | Con thuộc transaction của đợt; phạm vi đơn vị/quota; đợt đã mở có field bị khóa; xóa có dependency check. |
| registration | `priority_category`, `application_priority` | Danh mục active; kết quả xác minh có người/thời gian/note và điểm tính lại; quan hệ không CRUD độc lập. |
| registration | `room_application` | DRAFT mới sửa nội dung; submit/approve/reject/assign/release/cancel theo policy, giữ history. |
| registration | `summer_choice` | Giữ nguồn contract/semester; sửa/xóa PENDING theo contract hiện hành, không tự thêm trạng thái CANCELLED vào schema. |
| contract | `dormitory_contract` | PENDING chỉ xét xóa khi không có assignment/cọc/history; trạng thái hoạt động/kết thúc đi qua action. |
| contract | `contract_extension_request`, `room_transfer_request` | Chốt xóa PENDING so với cancel trước rollout; quyết định xong giữ nguồn và lịch sử. |
| contract | `move_out_record`, `storage_record` | Chứng từ nghiệp vụ; tạo/trả phòng/gửi/nhận đồ qua service riêng, không mở CRUD sửa/xóa tùy ý. |
| maintenance | `maintenance_request`, `maintenance_photo` | Ticket cancel/close theo policy; file ảnh quyền đọc/validation/rollback cùng nghiệp vụ, không xóa lịch sử để sửa lỗi. |
| billing | `utility_rate`, `utility_reading` | Giá hiệu lực bất biến; chỉ số giữ phòng/loại/tháng, khóa khi đã có hóa đơn/kỳ sau; thay công tơ bằng semantics đã chốt. |
| billing | `invoice`, `invoice_item` | Chưa thanh toán không có nghĩa luôn được sửa; kiểm chi tiết/history/scope học kỳ; PAID khóa, điều chỉnh qua workflow. |
| billing | `deposit_transaction` | Ledger append-only, tiền VND; thu/trừ/bù/hoàn có transaction/khóa, không sửa/xóa trực tiếp qua UI. |
| notification | `notification`, `notification_read` | Manual chỉ sửa nội dung theo scope; system không CRUD; mark read/read-all theo người nhận và idempotent. |
| common | `status_history`, `audit_log` | Append-only theo quy trình, audit gắn actor/requestId và không secret; không có nút sửa/xóa nhật ký. Trigger audit không được coi là trigger bảo vệ mọi bảng history. |
| platform | `spring_session`, `spring_session_attributes` | Di sản schema V2; Keycloak hiện không dùng session fallback; không có UI CRUD và không tự drop để dọn. |

### 6.2. Mẫu quy tắc phải điền cho từng bảng

- Owner module, service ghi, port đọc và reviewer.
- PK/FK/unique/index/check/default/nullability và migration tạo/đổi; mapping entity–DTO–UI.
- Field bất biến, field user sửa theo trạng thái; cách xử lý missing/null/empty/false/0.
- READ/CREATE/UPDATE/DELETE và action chuyên biệt; ownership, management unit, aggregate read không mở write.
- Delete/hủy/khóa/ngừng/đóng: điều kiện, bản liên quan, thông báo lỗi và effect history.
- Audit/history/event: trước/sau, reason, actor, requestId, timing transaction; dữ liệu phải che/không ghi.
- Quy tắc tiền, ngày và timezone: VND số nguyên, DATE cho ngày nghiệp vụ, TIMESTAMPTZ cho thời điểm; session epoch là ngoại lệ kỹ thuật có giải thích.
- Transaction/lock/rollback/idempotent/concurrency; không bỏ FK/index/check để test hoặc CRUD dễ qua.
- Retention, backup/restore và quyền truy cập; không tự chạy purge trong rollout UI.
- Regression test và tình huống lỗi bắt buộc; fresh install/upgrade khi có migration mới.

## 7. Liên kết nghiệp vụ và vấn đề phải chốt

### 7.1. Các chuỗi nghiệp vụ phải nghiệm thu xuyên module

| Chuỗi | Liên kết dữ liệu và UI phải giữ |
|---|---|
| Import → tài khoản → hồ sơ → đơn | Import một lần/preview/result, Keycloak required action, danh tính bất biến, quyền cập nhật ngay; không lộ mật khẩu trong UI cache/audit. |
| Đơn → duyệt → phân giường → hợp đồng → cọc | ASSIGNED có HOLD/code/deadline, activate thu đúng mức tòa, HOLD→STAY cùng transaction; phòng/pending/dashboard được invalidate. |
| Gia hạn/hè → hợp đồng → storage/phí hè | STAY gia hạn và phí hè theo lựa chọn duyệt; TEMP_LEAVE có điều kiện gia hạn/gửi đồ; nhận lại không tạo đơn mới. |
| Chuyển phòng/trả phòng → giường → trưởng phòng | Khóa theo thứ tự, cùng đơn vị, snapshot giá mới, bỏ trưởng phòng khi rời; breadcrumb/back/link không trỏ portal sai. |
| Ticket → sửa → nghiệm thu → kết luận → cọc | Chỉ đúng KTV/owner/Staff có điều kiện; WEAR không trừ; MISUSE theo trách nhiệm lúc tạo ticket; sự kiện sau commit. |
| Chỉ số/đơn giá → hóa đơn → thanh toán → công nợ | Nối kỳ, snapshot giá, chống trùng, scope học kỳ/phòng, thông báo và due date; PAID bất biến. |
| Action → history/audit → thông báo/pending/dashboard | Đồng bộ query keys/link; không notification trùng; read all/badge đúng; lỗi sau commit không làm mất giao dịch đã thành công. |

Tích hợp qua Service/port theo chiều SAD và event đúng timing; không hiểu “liên kết mật thiết” là import repository chéo module hoặc gom toàn bộ logic vào CrudPage.

### 7.2. Các đầu việc ưu tiên tái hiện, không tự coi là sửa xong

Các phát hiện sau là đầu vào từ rà soát trước; mục đã xác nhận được cập nhật riêng, không coi patch đang viết là đã nghiệm thu. Mọi fix phải tái kiểm trên revision bàn giao:

1. Release/cancel giữ chỗ so với job hết hạn có thứ tự khóa application và bed/contract khác nhau; tái hiện cạnh tranh để chốt order không deadlock.
2. **P1 đã xác nhận trong audit phiên này:** nhánh ROOM_FEE tại `InvoiceCrudServiceImpl` kiểm quyền ghi theo bất kỳ đơn cũ trong đơn vị thay vì gắn học kỳ hóa đơn. BE-Billing đang xử lý, chưa VERIFIED; kiểm cả tạo/sửa/xóa và đối soát list/detail/confirm theo cùng scope học kỳ, quyền đọc tổng hợp không mở ghi.
3. **Đã chốt 31/10/2026:** điện nước phải nhập đủ kỳ. Nếu đã có tháng 1 mà thiếu tháng 2, chặn nhập tháng 3 và yêu cầu hoàn thiện tháng 2; không tự nối lần ghi xa hơn hoặc gộp tiêu thụ. Kỳ đầu chưa có lịch sử vẫn nhập mốc đầu theo SRS; thay công tơ không tự bỏ qua yêu cầu đủ kỳ.
4. `allowedActions` phải khớp quyền động thật và các quyền phụ của action; lỗi phải trả theo API có dữ liệu rollback.
5. Thiếu công nợ sau trả phòng, notification hóa đơn hoặc tự đóng ticket: đối chiếu FR-CON-10, FR-BIL-07, FR-NOT-04 và caller.
6. Loại phòng khi assign: lấy SRS hiện hành làm chuẩn phù hợp loại phòng và đơn vị; API mô tả ưu tiên xếp trước phải được đối soát/đồng bộ trước khi code. Không tự mở ngoại lệ cho ADMIN hoặc chuyển mandatory thành preferred.
7. Hoàn cọc theo số dư cọc của SRS hiện hành; không tự thêm hoàn từng phần. Xóa/cancel yêu cầu PENDING, payload thanh toán và meter replacement phải giữ semantics có thẩm quyền; chỉ hỏi lại nếu đối soát phát hiện mâu thuẫn thực sự ảnh hưởng nghiệp vụ, không lấy thói quen HR để quyết định.
8. Bốn file maintenance trong stash chỉ là ứng viên patch; phải kiểm diff với baseline mới và test lại trước khi đưa vào ticket DMS-BIZ-02.

### 7.3. Quyết định đã được người dùng xác nhận

| Mã | Quyết định ngày 31/10/2026 | Ảnh hưởng ticket | Giới hạn |
|---|---|---|---|
| DEC-UX-01 | Qoder thiết kế cùng reviewer độc lập nếu Anti chưa được cấu hình; không chờ Anti. | DMS-UX-01, UI-CORE-01, QA-02 | Reviewer không tự duyệt phần mình triển khai; vẫn cần duyệt pilot, chưa dispatch code. |
| DEC-BIL-01 | Điện nước phải nhập đủ kỳ; thiếu kỳ liền trước thì hoàn thiện kỳ đó trước. | DMS-BIZ-02, UI-BIL, QA-02 | Không tự nối kỳ xa/gộp tiền; kỳ đầu và thay công tơ vẫn tuân SRS. |

Các quy tắc khác ưu tiên SRS như mục 2.1. Không còn câu hỏi đang chặn việc bắt đầu baseline/thiết kế; lựa chọn pilot sẽ được duyệt trên bản thiết kế, và mâu thuẫn nghiệp vụ mới phát hiện phải báo trước khi đổi hành vi. Việc xác nhận ở đây chỉ cập nhật kế hoạch, không đánh dấu ticket đã triển khai.

## 8. Tên hàm, truy vấn và ghi chú cho developer

### 8.1. Naming và tổ chức

- Giữ Controller mỏng, DTO typed, interface Service có JavaDoc tiếng Việt, logic trong Impl và constructor injection.
- Public CRUD theo DMS: `searchByPage`, `getById` hoặc tên get hiện hữu của tài nguyên, `saveOrUpdate`; không đổi tên/URL đã công bố chỉ để đồng nhất hình thức.
- Nghiệp vụ dùng tên chủ đích: `assignBed`, `releaseHold`, `activate`, `approveExtension`, `previewMoveOut`, `executeMoveOut`, `returnStorage`; helper nêu rõ điều kiện/đối tượng.
- Component PascalCase, hook `use<Domain><Purpose>`, handler `handle<Action>`, boolean `is/has/can` khi thực sự mang ý nghĩa đó; không đổi wire field đã công bố chỉ để thêm tiền tố.
- Phân biệt entity/model, API DTO, form value, mapper và view model; tránh Object/Map tùy ý cho contract mới, không thêm abstraction nếu vài dòng trong file hiện tại đủ rõ.
- Không xóa/tái cấu trúc code cũ đang ổn chỉ để làm sạch. Diff tối thiểu, giữ line endings; thay đổi shared có impact analysis, opt-in/migration caller và review trước.

### 8.2. Tham khảo truy vấn HR-V5 có chọn lọc

CRUD dùng repository hiện có. Search động có thể dùng EntityManager + JPQL trong ServiceImpl nếu entity/DTO cùng module đủ mapping và có parity tests. Giữ native SQL cần thiết cho view/ledger/khóa PostgreSQL/code-generation/job; không chuyển JPA Specifications sẵn có sang chuỗi JPQL chỉ để “giống V5”.

Bất kỳ conversion nào phải chứng minh: permission/scope cho cả count và content; param binding; sort whitelist/tie-break; paging/JSON/null; literal `%`, `_`, `\\`; Unicode/collation và datetime; transaction/flush/lock; không N+1. Nếu chưa chứng minh được parity thì ghi blocker, không thay ngầm.

### 8.3. Ghi chú đủ chi tiết nhưng đúng chỗ

- JavaDoc interface diễn tả nghiệp vụ/đầu vào/đầu ra/ràng buộc thực sự. Comment trong code chỉ giải thích WHY, invariant, thứ tự khóa hoặc workaround khó hiểu; không chèn nhật ký dài vào ServiceImpl.
- Mỗi ticket cập nhật ghi chú bàn giao trong mục 13 của file này; chi tiết contract/schema/UI pattern bổ sung vào tài liệu chuẩn đang có để tránh tài liệu song song mâu thuẫn.
- Khi triển khai, báo cáo theo mẫu Reports của nhóm và link từ ticket: đã làm, chưa làm, rule liên quan, file thay đổi, kết quả lệnh thực tế, khác biệt so với thiết kế, rollback.
- Tên hàm/cấu trúc không đủ giải thích quyết định nghiệp vụ thì ghi lý do và ví dụ nhỏ; không dùng comment để che thiếu test hoặc contract chưa chốt.

## 9. Ticket backlog và phân công dự kiến

### 9.1. Trạng thái

`BACKLOG → READY → IN_PROGRESS → REVIEW → VERIFIED → DONE`; `BLOCKED` có lý do và người cần quyết định. Không có ticket triển khai nào DONE ở thời điểm lập file. `AUDIT_IN_PROGRESS` là trạng thái đợt kiểm tra, không thay vòng đời ticket.

Tách hai trục: **IMPLEMENTED** chỉ xác nhận có code; **NOT_VERIFIED/PASS/FAIL** ghi cho từng gate static, chức năng/API, browser, security, NFR. `[x] IMPLEMENTED` giữ lịch sử việc đã viết nhưng không có nghĩa ticket VERIFIED. Chỉ chuyển VERIFIED khi các gate bắt buộc cùng revision có bằng chứng và reviewer độc lập duyệt; lint/build chỉ đóng gate static. Checklist nghiệm thu chưa chạy vẫn `[ ]`.

Ticket ID trong file là mã bàn giao ổn định; task số `#...` là tracker nội bộ phiên làm việc, **không phải GitHub Issue/Qoder cloud Issue đã phát hành**. Không có project cloud/Issue đích được chỉ định nên chưa tạo issue công khai.

### 9.2. Danh sách 15 ticket cha

| ID / tracker | Ưu tiên | Owner dự kiến | Reviewer độc lập | Phụ thuộc | Trạng thái |
|---|---|---|---|---|---|
| DMS-STD-01 / #34 | P0 | Lead + QA | TV1–TV5 | — | IN_PROGRESS; đã tạo ma trận truy vết 29 resource/94 route/78 FR/33 AT; còn ảnh browser và CI đúng SHA |
| DMS-UX-01 / #35 | P0 | Qoder / agent thiết kế | Reviewer độc lập + FE-Core + người dùng | STD-01 | IN_PROGRESS; đã chốt thiết kế Slate & Accent, KPI/filter/bảng theo chuẩn V5 |
| DMS-PREF-01/02 / #36 | P1 | BE-Account rồi FE-Theme | Security + QA | STD-01, UX-01, DATA-01 | REVIEW giữ nguyên; V8/API IMPLEMENTED; full 342 gồm 5 PREF, targeted sau có 6; browser isolation NOT_VERIFIED |
| DMS-UI-CORE-01 / #37 | P1 | Shared Integrator (FE-Core) | Reviewer thiết kế độc lập + QA | STD-01, UX-01 | REVIEW; theme/UserMenu IMPLEMENTED; static lint/build lịch sử pass; browser/chức năng NOT_VERIFIED |
| DMS-CRUD-01 / #38 | P1 | Shared Integrator (FE-CRUD) | BE owner + QA | UI-CORE-01, DATA-01 | REVIEW; renderer IMPLEMENTED nhưng chưa đủ parity/popup-only; browser/chức năng NOT_VERIFIED, audit đã thấy lỗi |
| DMS-DATA-01 / #39 | P0 | BE-Data | Owner TV1–TV5 + Security | STD-01 | IN_PROGRESS; manifest 36/36 bảng + 6 view/3 sequence/1 trigger (DRAFT), chờ owner xác nhận |
| DMS-BIZ-01 / #40 | P0 | TV2 + TV3 | Concurrency QA + Lead | STD-01, DATA-01 | BACKLOG |
| DMS-BIZ-02 / #41 | P0 | TV5 + TV4 | TV1 + QA | STD-01, DATA-01 | IN_PROGRESS: DEC-BIL-01 đang được kiểm thử/review; các nhánh khác chưa làm |
| DMS-UI-MODULE-01 / #42 | P1 | FE-Account / FE-Room | TV1 / TV2 + QA | PREF, CRUD | BACKLOG |
| DMS-UI-MODULE-02 / #43 | P1 | FE-Registration / FE-Contract | TV3 / TV4 + QA | PREF, CRUD, BIZ-01 | BACKLOG |
| DMS-UI-MODULE-03 / #44 | P1 | FE-Billing / FE-Maintenance / FE-Portal | TV5 / TV4 + QA | PREF, CRUD, BIZ-02 | BACKLOG |
| DMS-ENG-01 / #45 | P1 | Mỗi owner; Lead tích hợp | Developer khác | STD-01, xuyên suốt | BACKLOG |
| DMS-QA-01 / #46 | P0 | Security/QA chỉ đọc | Lead + nghiệp vụ | STD-01; lặp mỗi wave | IN_PROGRESS; đã review độc lập đợt PREF (0 P0, 2 P1 đã sửa), còn HTTP/race test và browser |
| DMS-QA-02 / #47 | P1 | Browser QA + performance QA | Reviewer thiết kế độc lập + nghiệp vụ | MODULE-01..03, QA-01 | BACKLOG |
| DMS-REL-01 / #48 | P1 | Lead/Release | QA + chủ dự án | ENG-01, QA-02 | BACKLOG |

Các vai trò là kế hoạch nhân sự, không xác nhận một người/agent đã nhận task. Ticket cha chứa nhiều module phải tách child task và chỉ định chính xác file trước khi chạy song song; không cho hai agent ghi cùng file dựa vào bảng này.

### 9.3. Nội dung, tiêu chí đạt và kiểm thử theo ticket

#### DMS-STD-01 — Baseline và ma trận truy vết

- [x] Khóa SHA/branch, kiểm working tree/stash, ghi môi trường và giới hạn truy cập trong `dms-traceability-matrix.md`.
- [x] Liệt kê route + 29 resource + màn nghiệp vụ, 36 bảng và FR/AT/NFR tương ứng; mỗi hàng có owner/implementation/test/verification status.
- [ ] Chạy baseline trên DB test riêng, không trên dữ liệu dev dùng thật; ghi exit code và test summary, ảnh trước sửa cùng viewport.
  - Frontend lint/build lịch sử: exit 0, lint 0 lỗi/0 cảnh báo, build cảnh báo bundle trên 500 kB; không phải test component/E2E. Backend full trên PostgreSQL 16 test riêng cổng 55442: **342 test, gồm 5 PREF**, 0 thất bại/0 lỗi/0 skip, kết thúc 31/10/2026 10:46:54 +07:00, `wave2-full-test.log`. Lượt targeted PREF sau đó có 6 test, kết thúc 10:57:28, không phải full cùng revision (mục 13.2). Chưa đủ ảnh baseline bốn portal/cùng viewport; giữ checklist mở.
- [ ] Lập danh sách P0/P1 đã tái hiện và các quyết định còn chờ; không sử dụng điểm “production ready” từ báo cáo cũ.
- **AC:** không hạng mục nào gắn DONE chỉ vì có file; bảng truy vết đủ nguồn và SHA. Ước lượng: 1–2 ngày công.
  - Ma trận ghi 94 route entry (4 portal + auth/catch-all), 29 resource, 78 FR, đủ AT-01–AT-33; các mục chưa có bằng chứng để `NOT_VERIFIED`, không suy ra điểm hoàn thiện từ số ticket.

#### DMS-UX-01 — Qoder thiết kế toàn bộ UI với reviewer độc lập

Phân công đã chốt theo DEC-UX-01: Qoder thực hiện, reviewer độc lập kiểm tra; không còn bị chặn do thiếu Anti. Các checkbox dưới đây vẫn chưa triển khai.

- [ ] Chỉ định agent thiết kế và agent reviewer khác nhau, tách phạm vi file trước dispatch; không cài/đăng ký hoặc giả nhận Anti thay người dùng.
- [ ] Agent thiết kế chỉ nhận brief UI: reference HR-V5, DMS tokens/business constraints, 4 role, 3 pilot và before/after; không đổi API/schema/auth hay chỉnh giá/trạng thái.
- [ ] Chốt shell, type scale, spacing, field group, view/edit pattern, bảng/filter/action, dark/light/accent và feedback.
- [ ] FE-Core/QA kiểm khả thi shared, nghiệp vụ kiểm action, người dùng duyệt pilot trước áp toàn dự án.
- **AC:** blueprint có các mode và viewport, contrast/focus, mapping component thực; không “đẹp” chỉ qua mock. Ước lượng: 2–3 ngày công sau khi STD-01 đạt.

#### DMS-PREF-01/02 — Sở thích cá nhân backend và frontend

- [x] PREF-01: chốt contract typed/whitelist/default/ownership/concurrency; quyết định persistence (bảng 1-1 `user_preference`, Flyway V8) và cập nhật API Spec v0.20 + DB Design v0.12.
- [x] PREF-02: state/provider/cache/hydration/reset/logout/multitab, UI chọn preset hợp lệ, save/error/reset; cache localStorage namespace theo user id, bridge chống race và 409.
- [ ] Test hai tài khoản chung browser, đa thiết bị, field lạ, invalid color, response cũ, logout khi debounce, xung đột version.
- **AC:** toàn app và portal popup theo cùng token, không lộ dữ liệu user cũ, không silent-fail. Ước lượng: BE 1–2 + FE 1–2 ngày công; FE tích hợp sau contract/palette chốt.
  - Bằng chứng 31/10/2026 tách hai revision working tree: full `wave2-full-test.log` lúc 10:46:54 có 342 test, trong đó PREF 5; targeted `wave2-pref-retest2.log` lúc 10:57:28 có 6 PREF pass sau sửa thiếu version (400). Không cộng thành 348 hoặc coi full đã phủ test thứ 6. Frontend lint/build pass lịch sử; browser hai tài khoản/đa tab, HTTP/race và full retest sau tích hợp còn chờ, ticket giữ `REVIEW`.

#### DMS-UI-CORE-01 — Shared design foundation

- [x] IMPLEMENTED: có thay đổi theme/App/CSS/shared/layout; ownership tiếp theo quy về một Shared Integrator theo mục 10.4, không chia CSS cho module agents.
- [x] IMPLEMENTED: Slate & Accent, 6 preset blue/indigo/teal/emerald/violet/amber và dark mode; đây là mô tả code, chưa là nghiệm thu màu/contrast.
- [x] IMPLEMENTED: thay font tiêu đề/logo sang sans-serif; còn phải kiểm khả năng đọc, dấu tiếng Việt và zoom.
- [x] IMPLEMENTED: `themeStore.ts` dùng `useSyncExternalStore`, cache localStorage và CSS variables; account isolation thuộc gate PREF.
- [x] IMPLEMENTED: `UserMenu.tsx` chọn sáng/tối, preset và mật độ bảng; `DataTable.tsx` nhận mật độ.
- [ ] Kiểm popup/tooltip/drawer/table/sidebar bốn portal ở 360/768/1440, light/dark và các preset; focus/contrast/keyboard/zoom; lỗi tải/lưu preference không báo thành công giả.
- **AC:** không bộ UI song song, fallback an toàn, không rò theme giữa tài khoản; có bằng chứng browser/chức năng cùng revision. **REVIEW; browser/chức năng NOT_VERIFIED**. Lint/build lịch sử pass chỉ chứng minh static. Ước lượng ban đầu 2–4 ngày công, phải ước lượng lại sau audit.

#### DMS-CRUD-01 — Một pattern xem/thêm/sửa/xóa và danh sách

- [x] IMPLEMENTED: có renderer VIEW riêng thay input disabled tại `CrudPage.tsx`; không suy ra đã đồng nhất mọi dữ liệu.
- [x] IMPLEMENTED: `.dms-record-view`, `renderViewFieldValue` và `formSections`; parity đầy đủ immutable/history/children trong EDIT còn phải kiểm/sửa.
- [x] IMPLEMENTED: có formatter badge/boolean/số/tiền/ngày/textarea/copy; nhãn lookup và semantics thời gian chưa được nghiệm thu.
- [x] IMPLEMENTED: có nhóm `detailColumns` “Nhật ký & Hệ thống”; cần kiểm audit before/after và field chỉ có ở detail không bị mất ở EDIT.
- [x] IMPLEMENTED (lịch sử, cần hiệu chỉnh): popup từng có “Chuyển sang sửa” cùng “Xóa”/“Xác nhận đã thu tiền”. Hai action ghi trong VIEW **không đạt** mục 4.2; phải chuyển sang mode xử lý riêng, không bỏ quyền hợp lệ.
- [ ] Xem/Sửa chỉ popup, cùng ID/nhóm/thứ tự/nguồn dữ liệu; không nối `detailRoutes`; registry canonical bao gồm immutable/history/children trong cả VIEW và EDIT.
- [ ] Kiểm từng resource mục 9.4 và bộ ca mục 12.3; VIEW network không có request ghi, save lỗi giữ draft, conflict không mất input, đang ghi không đóng/gửi kép.
- **AC:** đủ map field và section module, không payload sửa immutable; quyền/allowedActions không đổi; browser/function có evidence. **REVIEW; browser/chức năng NOT_VERIFIED**. Audit `/admin/summer` đã thấy ID trần và lệch dữ liệu thời gian/parity, nên không thể xác nhận chỉ bằng lint/build. Ước lượng ban đầu 2–4 ngày công, điều chỉnh theo pilot.

#### DMS-DATA-01 — Quy tắc bảng, cấu hình và xóa

- [x] Điền manifest đủ 36/36 bảng V1–V7 + 6 view, 3 sequence, 1 trigger trong `dms-data-manifest-36-tables.md` (DRAFT, mỗi dòng còn `NOT_VERIFIED`).
- [ ] Chốt hard delete/lock/inactive/cancel/close/append-only và code lỗi cùng module owner; không có một generic DeleteService cho mọi bảng.
- [ ] Kiểm tiền/timezone/index/constraints/security/retention và kế hoạch backup/restore; migration mới chỉ cho thay đổi được duyệt.
- **AC:** UI delete/action map về đúng service/permission/lifecycle; có test dữ liệu đã dùng, reference missing và cạnh tranh. Ước lượng: 2–3 ngày công.
  - Manifest liệt kê 14 khoảng trống cần owner quyết định (PENDING delete vs cancel, invoice scope, storage/move-out, append-only, quota UI, room-type capacity…); chưa thay thế xác nhận của owner.

#### DMS-BIZ-01 — Khóa và lưu trú

- [ ] Viết test tái hiện cạnh tranh release/cancel/expiry và kiểm order bed→student→contract→invoice, application lock tương thích.
- [ ] Đối soát/đồng bộ API theo SRS: phân giường đúng loại phòng và đơn vị, kể cả ADMIN; kiểm assignment rollback không tạo bản rác. Không thêm ngoại lệ preferred nếu chưa được duyệt thay đổi nghiệp vụ.
- [ ] Cập nhật port/policy/allowedActions và spec liên quan khi có quyết định, không bypass FK hoặc bù dữ liệu ngoài transaction.
- **AC:** test đồng thời lặp có timeout rõ; không deadlock/oversubscribe; state/history/audit nguyên tử. Ước lượng: 2–3 ngày công sau chốt semantics.

#### DMS-BIZ-02 — Tài chính, maintenance và notification

- [ ] Tái hiện scope invoice CRUD theo semester, meter gap, debt-after-checkout, required payment fields và notification timing; chọn fix từ bằng chứng.
- [ ] Áp DEC-BIL-01: đã có tháng 1 mà thiếu tháng 2 thì chặn tạo tháng 3, hướng dẫn hoàn thiện tháng 2; test kỳ đầu nhập mốc đầu, đủ tháng liên tiếp, thiếu nhiều tháng, thay công tơ và khóa khi có kỳ sau/hóa đơn. Không tự gộp tiêu thụ; đồng bộ SRS/API/error message/caller khi triển khai.
  - Tiến độ 31/10/2026, ticket con #49: đã thêm kiểm tra tạo kỳ liên tiếp sau khóa phòng, giữ logic sửa/xóa cũ; đồng bộ SRS v0.14 và API Spec v0.19. `CrudApiIntegrationTest`: 62 test, 0 thất bại/0 lỗi trên PostgreSQL test riêng. Chờ review độc lập và bộ test tổng; chưa tick hoàn thành.
- [ ] Review từng patch trong stash; student chuyển phòng không tự coi đã trả phòng, job đổi ticket cần đúng event nếu FR yêu cầu.
- [ ] Mở child task độc quyền theo module/test file, không để billing và maintenance sửa chung CrudApiIntegrationTest đồng thời.
- **AC:** test quyền/scope/payment/ledger/rollback/notification không trùng; cập nhật API/caller nếu đổi contract. Ước lượng: 3–5 ngày công, chỉ sau business decision.

#### DMS-UI-MODULE-01 — Account và Room

- [ ] **UI-ACC:** login/profile/import/accounts/role matrix/config/audit: view/edit cùng pattern; form nhập không lộ secret, import preview/result không mất; audit chỉ đọc đủ before/after.
- [ ] **UI-ROOM:** danh mục đơn vị/tòa/loại/học kỳ/phòng/giường, sơ đồ giường, leader/history và entry chuyển phòng; state vận hành khác occupancy; xóa đúng lịch sử.
- [ ] Tách hai child task file không giao nhau; registry/route/CSS requests gửi integrator.
- **AC:** role matrix/cross-unit, data dùng rồi, inactive lookup, mobile room/cards và return-to-list đều qua browser. Ước lượng: mỗi nhóm 1–2 ngày công.

#### DMS-UI-MODULE-02 — Registration và Contract

- [ ] **UI-REG:** đợt/quota, đơn/ưu tiên/duyệt/BedPicker/hè, giường giữ và deadline, pending link; không tự đổi status/score từ form.
- [ ] **UI-CON:** contract/extension/transfer/activation/move-out/storage; detail section thống nhất, tiền cọc/history rõ, confirm/reason/cảnh báo bắt buộc.
- [ ] Tách contract đọc/kích hoạt/gia hạn và checkout/storage child ownership khi nhiều agent; khóa public interface trước khi chia.
- **AC:** luồng đăng ký→ACTIVE, gia hạn/hè/chuyển/trả/gửi-nhận đồ chạy xuyên role không đứt link; không ép workflow thành generic edit. Ước lượng: mỗi nhóm 2–3 ngày công.

#### DMS-UI-MODULE-03 — Billing, Maintenance và Portal

- [ ] **UI-BIL:** rate/reading/invoice/preview/payment/deposit/debt; số tiền và hạn đúng, ledger readonly, không cập nhật balance suy ra từ UI.
- [ ] **UI-MNT:** create/edit/view ticket dùng nhóm field nhất quán; vị trí/ảnh/assignment/progress/accept/reopen/conclude đúng role; ảnh không mất khi sửa.
- [ ] **UI-PORTAL:** dashboard/home/pending/inbox/published notifications/badge, biểu đồ có text/table thay thế và link drill-down đúng scope.
- **AC:** bốn role, desktop/mobile, notifications/read counts và query invalidation đúng; không broad CSS override module khác. Ước lượng: mỗi nhóm 1–3 ngày công.

#### DMS-ENG-01 — Naming, conventions và ghi chú dev

- [ ] Áp mục 8 theo từng diff, không rename đại trà; interface/DTO/hook/service rõ chức năng và contract.
- [ ] Mỗi ticket có nhật ký bàn giao, diagram ngắn cho transaction/event phức tạp và link tài liệu chuẩn.
- [ ] Diff kiểm không hardcode/duplicate/line-ending noise; mọi đoạn xóa hoặc đổi logic cũ có lý do và reviewer xác nhận.
- **AC:** developer khác có thể xác định entry point, dữ liệu, quyền, state, cấu hình và chạy test từ ghi chú. Thực hiện xuyên suốt, không dồn cuối.

#### DMS-QA-01 — Review chéo độc lập

- [ ] Reviewer không tự duyệt ticket mình implement; kiểm read-only diff/SRS/API/table matrix trước và sau wave.
- [ ] Security kiểm input/schema/owner/role/aggregate write; DB kiểm constraints/append-only/migration; business owner kiểm amounts/status/actions.
- [ ] Findings có severity, repro, file:line, expected/actual, đề nghị owner xử lý; reviewer không tự sửa vùng đang bị người khác giữ.
- **AC:** không còn P0/P1 chưa triage; mọi defer có người chấp thuận và ngày, không hạ severity để qua gate.

#### DMS-QA-02 — Browser, AT và NFR

- [ ] Chốt công cụ UI test có sẵn; nếu cần thêm runner/dependency phải xin chấp thuận riêng, không cài ngầm từ plan.
- [ ] Chạy AT-01–AT-33 và role/scope/state matrix; theme isolation; screenshot 360/768/1440 trước/sau; lỗi console/network phải được phân loại.
- [ ] Đo NFR-PER-01: p95 < 2s với 100 người dùng đồng thời và dữ liệu khoảng 1.500 sinh viên; NFR-PER-02: phát hành hóa đơn phí phòng cho toàn KTX khoảng 1.500 sinh viên hoàn tất < 30s. Ghi đúng workload, số liệu, thời gian và môi trường.
- [ ] JaCoCo Service >=60% theo SRS; thử nộp đơn <=5 phút với 3 sinh viên, keyboard/zoom/contrast.
- **AC:** evidence gắn SHA/test data/logs; build pass không thay E2E, mock success không thay auth/API thật. Ước lượng: 2–4 ngày công sau rollout, performance và thử người dùng cần điều phối riêng.

#### DMS-REL-01 — Tích hợp và bàn giao

- [ ] Fresh migration + upgrade + backup/restore DB/uploads/Keycloak trên môi trường riêng, không phá data thật.
- [ ] Đồng bộ docs và nhật ký tồn tại; checklist Must/Should/defer; ảnh/test evidence/known limitations.
- [ ] Commit/PR/push/merge chỉ khi được người dùng cho phép ở lượt triển khai; CI xanh trên đúng SHA, review theo convention nhóm; không force push/tag/release tự động.
- **AC:** working tree/branch/stash rõ, không secret/generated logs trong commit, có cách rollback và hướng dẫn vận hành. Ước lượng: 1–2 ngày công.

### 9.4. Backlog chi tiết 29 resource — không suy ra đã nghiệm thu từ registry

Nguồn khóa là `DMS/frontend/src/features/crud/resources.ts`: **29 khóa, mỗi khóa một dòng dưới đây**. Nhóm theo ticket cha đang có, không tạo Issue/FR mới. Owner module lập map field/API và gửi patch đề nghị cho Shared Integrator; chỉ Integrator sửa registry/CrudPage/types/routes/CSS. Các section dưới đây là đích nghiệm thu; nếu detail API thiếu thì ghi blocker cho BE owner, không giả tạo response để đủ UI.

Quy ước: SV = Student, QL = Staff, KT = Technician, AD = Admin. `CRUD(key)` tham chiếu đúng `permission`, `fields/editFields`, `readOnly/noCreate/noDelete`, `editStates`, `deleteAction`, ownership hiện có của key trong registry và kiểm tra CrudPage/backend; **không phải cấp mới quyền CRUD**. `WF` chỉ action đang có trong component module/API Spec mục 6 và `allowedActions` của response, chạy ở mode xử lý riêng ngoài VIEW. Mọi dòng áp mục 4.2 và ca chung mục 12.3; cột kiểm thử ghi H = happy path, E = edge, P = permission/scope. Bằng chứng từng dòng phải có ID bản ghi, revision, role, ảnh VIEW/EDIT hoặc lý do readonly, payload/network và kết quả H/E/P; hiện đều `NOT_VERIFIED`.

#### 9.4.1. DMS-UI-MODULE-01 / #42 — UI-ACC và UI-ROOM (12 resource)

| Resource / portal | Section canonical theo thứ tự, có ở cả VIEW/EDIT | Được sửa / bất biến | Action tham chiếu, ngoài VIEW | Kiểm thử H / E / P | Evidence / trạng thái |
|---|---|---|---|---|---|
| `units` / AD | Mã–tên → tham chiếu/hệ thống nếu API có | Mã/tên theo DTO; ID/tham chiếu readonly | `CRUD(units)`; ghi `/admin/management-units` | H lưu/đọc lại; E trùng mã, xóa đã dùng; P AD ghi, role khác không tự mở ghi | Chưa đủ / NOT_VERIFIED |
| `buildings` / AD | Nhận diện → đơn vị → mức cọc → hệ thống | Field DTO theo dependency; ID/lịch sử cọc không ghi đè | `CRUD(buildings)`; `BuildingServiceImpl` | H nhãn đơn vị/cọc round-trip; E mức cọc sai/tham chiếu; P chỉ AD ghi | Chưa đủ / NOT_VERIFIED |
| `roomTypes` / QL, AD | Tên–số giường → giá HK/hè → diện tích/tiện nghi → active/hệ thống | Giá/thuộc tính theo SRS; ID, snapshot đã dùng readonly; bedCount khóa theo rule đã chốt | `CRUD(roomTypes)`; `RoomTypeServiceImpl` | H giá 0/active=false; E loại đã dùng, bedCount (AT-33); P QL đúng đơn vị/quyền, AD vẫn giữ ràng buộc | Chưa đủ / NOT_VERIFIED |
| `semesters` / QL, AD | Tên–năm học–loại → khoảng ngày → tham chiếu/hệ thống | DTO hợp lệ; ID và dữ liệu đã dùng theo lifecycle | `CRUD(semesters)`; `SemesterServiceImpl` | H ngày không lệch timezone; E chồng ngày/đã tham chiếu; P READ/ghi riêng | Chưa đủ / NOT_VERIFIED |
| `users` / AD | Username–danh tính → liên hệ → role/đơn vị → khóa/đọc liên đơn vị → hệ thống | FullName/phone/role/unit theo DTO; username readonly khi sửa; không đưa mật khẩu/subject/version bảo mật vào form | `CRUD(users)` với `deleteAction=lock`; reset/unlock/quyền tổng hợp theo caller hiện có | H lưu role/unit, thu hồi phiên; E secret chỉ trả một lần, khóa tài khoản có lịch sử; P chỉ quyền AD hợp lệ, không nâng quyền từ body | Chưa đủ / NOT_VERIFIED |
| `students` / QL | Mã–tên–giới tính → học vụ/ngày sinh → liên hệ → hệ thống | Theo `editFields` và whitelist server; mã/tên/giới tính không biến mất ở EDIT | `CRUD(students)`; xóa dùng `/admin/students`, reset theo caller | H sửa liên hệ giữ mã; E null/ngày, hồ sơ có lịch sử; P QL không tạo SV, ngoài đơn vị 404, đọc tổng hợp không ghi | Chưa đủ / NOT_VERIFIED |
| `adminStudents` / AD trong import | Danh tính → học vụ → liên hệ → hệ thống, cùng nguồn hồ sơ | Trường được DTO AD cho phép; ID/owner/history giữ nguyên | `CRUD(adminStudents)`; `StudentManagementServiceImpl` | H CRUD hồ sơ chưa dùng; E trùng mã/đã có nghiệp vụ, mật khẩu không lưu cache; P chỉ AD quản lý | Chưa đủ / NOT_VERIFIED |
| `rooms` / QL, AD | Tòa–phòng–loại → tầng/giới tính/vận hành → giường/occupancy → trưởng phòng → lịch sử | Code/floor/type/gender theo rule; tòa nguồn, số liệu suy ra/history readonly; giường/leader qua WF riêng | `CRUD(rooms)`; WF `RoomDetailPage`/`BedCrudPanel` | H tạo đủ giường, popup đủ children; E lịch sử chặn xóa, giường đang ở; P đơn vị, AD không bypass occupancy | Chưa đủ / NOT_VERIFIED |
| `profile` / SV, QL, KT, AD | Danh tính/role → liên hệ → thông tin SV nếu có → hệ thống | SV: phone/hometown/emergencyContact; QL/KT/AD: phone; danh tính/role readonly | `CRUD(profile)` singleton, chỉ cập nhật `/me/profile` | H bốn role, Xem/Sửa popup cùng danh tính; E phone rỗng/null theo contract; P body owner/role không đổi danh tính | Chưa đủ / NOT_VERIFIED |
| `audit` / AD | Actor–action–đối tượng → thời gian/reason/requestId → before/after | Toàn bộ readonly; không EDIT/save/delete | `CRUD(audit)` readonly; `AuditServiceImpl` | H JSON before/after đầy đủ; E SYSTEM/null/JSON lớn, lọc biên thời gian; P AD đọc, role khác bị chặn, không secret | Chưa đủ / NOT_VERIFIED |
| `configs` / AD | Key–mô tả → value → người/thời điểm cập nhật nếu API có | Chỉ value của key có sẵn; key/mô tả/hệ thống readonly trong cả hai mode | `CRUD(configs)` noCreate/noDelete; `ConfigServiceImpl` | H sửa đúng key từ popup; E key mất/giá trị sai/0 ngoài miền; P không tạo/xóa hoặc ghi key khác | Chưa đủ / NOT_VERIFIED |
| `myRoom` / SV | Tòa–phòng–giường → loại chỗ/đặc điểm → bạn ở/trưởng phòng → lịch sử được API cho đọc | Toàn bộ readonly; không tạo form sửa cho đủ parity | `CRUD(myRoom)` readonly; `/me/room` | H chỗ hiện tại; E chưa có chỗ/HOLD/rời phòng, children lỗi không giả rỗng; P chỉ chính SV | Chưa đủ / NOT_VERIFIED |

#### 9.4.2. DMS-UI-MODULE-02 / #43 — UI-REG và UI-CON (9 resource)

| Resource / portal | Section canonical theo thứ tự, có ở cả VIEW/EDIT | Được sửa / bất biến | Action tham chiếu, ngoài VIEW | Kiểm thử H / E / P | Evidence / trạng thái |
|---|---|---|---|---|---|
| `priorities` / AD | Mã–tên → trọng số → active/hệ thống | Field DTO; ID và điểm đã ghi trên đơn không tự ghi đè từ UI | `CRUD(priorities)`; `PriorityCategoryServiceImpl` | H weight=0/active=false; E trùng mã/đã tham chiếu/inactive; P AD ghi, SV không tự tăng điểm | Chưa đủ / NOT_VERIFIED |
| `periods` / QL, AD | Học kỳ–loại–tên → mở/đóng → đơn vị áp dụng → quota theo tòa → hệ thống | Trước mở theo DTO; sau mở chỉ closeAt; ID/quan hệ không được sửa ngầm | `CRUD(periods)`; `RegistrationPeriodServiceImpl` aggregate | H unitIds/quota đọc lại cùng EDIT; E phần tử null, quota=0, đợt hè/đã mở; P đợt nhiều đơn vị không bị QL một đơn vị ghi đè | Chưa đủ / NOT_VERIFIED |
| `applications` / QL, AD | SV → đợt/nguyện vọng → diện ưu tiên/note → điểm/xác minh → giường giữ/hạn/hợp đồng → lịch sử | Chỉ DRAFT theo DTO; SV nguồn, điểm/status/xác minh/assignment/history readonly | `CRUD(applications)` cancel; WF `ApplicationDetailView`, `VerifyPriorityModal` | H tạo/sửa nháp, duyệt/phân ở mode xử lý; E lặp submit, 409 đổi trạng thái, inactive lookup; P đúng đơn vị/quyền phụ action | Chưa đủ / NOT_VERIFIED |
| `myApplications` / SV | Danh tính của tôi → đợt/nguyện vọng → diện ưu tiên/note → điểm/quyết định → HOLD/deadline → lịch sử | DRAFT nhập nguyện vọng/note; owner/điểm/status/hạn readonly | `CRUD(myApplications)` cancel; WF `ApplicationDetailView` theo role | H nộp nháp, refresh thấy trạng thái; E đợt đóng không còn trong lookup, đơn APPROVED có hợp đồng; P `/me` ép owner, không đọc/sửa đơn người khác | Chưa đủ / NOT_VERIFIED |
| `contracts` / QL, AD | Mã–SV–đơn → thời hạn/trạng thái → cọc/hạn kích hoạt → assignment/giá snapshot → yêu cầu/chứng từ → lịch sử | PENDING chỉ field cho phép; đơn/SV nguồn, cọc/assignment/status/history readonly | `CRUD(contracts)`; WF `ContractDetailView`/`ActivateContractModal` | H PENDING mở EDIT đủ lịch sử/cọc; E hết hạn giữa thao tác, có ledger/assignment chặn xóa; P scope và action activation riêng | Chưa đủ / NOT_VERIFIED |
| `myContracts` / SV | Cùng section hợp đồng được phép đọc: nguồn → hạn → cọc → assignment → yêu cầu/chứng từ → lịch sử | Toàn bộ readonly, không vì `fields=[]` mà popup rỗng | `CRUD(myContracts)` readonly; WF gửi yêu cầu theo caller hiện có, không trong VIEW | H detail đủ block; E chưa có assignment/cọc và block lỗi; P chỉ hợp đồng của tôi | Chưa đủ / NOT_VERIFIED |
| `extensions` / SV, QL, AD | Hợp đồng/SV → ngày cũ–mới/reason → nguồn/yêu cầu → quyết định/thời điểm → lịch sử | PENDING sửa newEndDate/reason theo owner; contract/source/oldDate/decision/history readonly | `CRUD(extensions)` cancel; WF `ExtensionReviewModal`/`ExtensionDetailPage` | H gia hạn và history còn ở EDIT; E chồng ngày, stale 409, nguồn SUMMER_STAY; P ownership + quyền duyệt tách quyền sửa | Chưa đủ / NOT_VERIFIED |
| `transfers` / SV, QL, AD | Hợp đồng/SV → phòng/giường nguồn → reason → giường đích/quyết định → lịch sử | PENDING sửa reason; hợp đồng/nguồn/đích quyết định/status/history readonly | `CRUD(transfers)` cancel; WF `TransferReviewPage`/`TransferRequestPage` | H chuyển đúng loại/đơn vị; E giường đích vừa bị chiếm, hai chuyển ngược; P SV không tự assign, QL không chuyển liên đơn vị | Chưa đủ / NOT_VERIFIED |
| `summer` / SV, QL, AD | Hợp đồng/học kỳ có nhãn → choice → thời điểm gửi (`createdAt` theo API) → quyết định/reason → lịch sử/liên kết hè | PENDING chỉ sửa choice; contract/semester/status/decision/history readonly nhưng vẫn hiện ở EDIT; không có field note trong DTO lưu | `CRUD(summer)`; WF `SummerChoiceDetailView`, không tự thêm CANCELLED | H cùng ID ở VIEW/EDIT không hiện trần 8/4; E đọc đúng createdAt, đã duyệt khóa sửa; P lookup đúng role/scope, QL không dùng nhầm `/me` làm mất nhãn | Có patch; chờ nghiệm thu / NOT_VERIFIED |

#### 9.4.3. DMS-UI-MODULE-03 / #44 — UI-BIL, UI-MNT và UI-PORTAL (8 resource)

| Resource / portal | Section canonical theo thứ tự, có ở cả VIEW/EDIT | Được sửa / bất biến | Action tham chiếu, ngoài VIEW | Kiểm thử H / E / P | Evidence / trạng thái |
|---|---|---|---|---|---|
| `readings` / QL, AD | Phòng–đồng hồ–tháng → chỉ số đầu/cuối → thay/reset → tiêu thụ/khóa/hệ thống | Chỉ số và thay/reset khi chưa invoiced/kỳ sau; room/type/month/consumption readonly khi sửa | `CRUD(readings)`; `UtilityReadingServiceImpl` | H đủ kỳ/0 và thay đồng hồ; E thiếu kỳ trước, end<start, có kỳ sau/hóa đơn; P đúng phòng/đơn vị | Chưa đủ / NOT_VERIFIED |
| `rates` / QL, AD | Loại/đơn giá → hiệu lực → hệ thống | Chỉ bản tương lai hợp lệ; bản đã hiệu lực/snapshot readonly | `CRUD(rates)`; `UtilityRateServiceImpl`/`UtilityRateCrudServiceImpl` | H bản giá mới; E qua mốc hiệu lực khi đang mở, trùng kỳ/giá sai; P quyền ghi tách đọc | Chưa đủ / NOT_VERIFIED |
| `invoices` / QL, AD | Mã–loại–chủ thể → học kỳ/tháng → tiền/hạn/status → items → thanh toán → lịch sử | UNPAID chưa có items/history cấm sửa: total/dueDate; type/student/semester/room/month/items/payment/history readonly | `CRUD(invoices)`; WF `ConfirmPaymentModal`/`IssueInvoicesModal` | H lưu hồ sơ và thu ở workflow; E PAID/items khóa, duplicate/409; P ROOM_FEE theo semester, aggregate read không ghi, cả create/update/delete/confirm | Chưa đủ; P1 đang sửa / NOT_VERIFIED |
| `myInvoices` / SV | Cùng section invoice được phép đọc: nguồn/kỳ → tiền/hạn → items → thanh toán/history | Toàn bộ readonly, không nút thu tiền | `CRUD(myInvoices)` readonly | H hóa đơn cá nhân/phòng; E chưa có items/overdue/đã thu; P không đọc hóa đơn SV/phòng khác | Chưa đủ / NOT_VERIFIED |
| `deposits` / QL, AD | SV/hợp đồng → required/balance/topup/debt → hạn bù → ledger → nguồn ticket/người xác nhận | Toàn bộ readonly trong VIEW; không sửa/xóa dòng ledger | `CRUD(deposits)` readonly; WF `DepositLedgerModal` và API cọc hiện hữu ở mode riêng | H ledger/số dư đúng; E 0/âm/bù quá số thiếu/hoàn lặp; P đọc tổng hợp không thu/bù/hoàn khác đơn vị | Chưa đủ / NOT_VERIFIED |
| `myDeposits` / SV | Cùng section cọc được phép đọc: hợp đồng → số dư/cần bù/nợ → hạn → ledger/nguồn | Toàn bộ readonly, không WF tài chính cấp cho SV | `CRUD(myDeposits)` readonly | H số dư sau giao dịch; E chưa có ledger/âm/0, block lỗi không báo 0 giả; P chỉ hợp đồng của tôi | Chưa đủ / NOT_VERIFIED |
| `maintenance` / SV, QL, KT, AD | Ticket/chủ thể → vị trí → hạng mục/mức độ/nội dung → ảnh → phân công/tiến độ → kết luận/chi phí → lịch sử | OPEN theo quyền/owner chỉ nội dung DTO; vị trí/ảnh cũ/assignment/cost/history readonly ở EDIT | `CRUD(maintenance)` multipart; WF `MaintenanceDetailView` và các modal module | H sửa giữ ảnh và vị trí; E ảnh sai/rollback, lỗi save giữ draft, OPEN đổi state; P chủ ticket/KTV được giao/QL scope, quyền đọc ảnh | Chưa đủ / NOT_VERIFIED |
| `notifications` / QL, AD | Tiêu đề/body → scope/người nhận → nguồn manual/system → người/thời điểm đăng | Manual chỉ title/body; scope/recipient/source/created readonly; system không CRUD | `CRUD(notifications)`; `NotificationCrudServiceImpl`; không mark-read ngầm trong VIEW | H sửa nội dung giữ scope; E FLOOR thiếu ở UI cần đối chiếu caller, system khóa; P QL đúng người tạo/đơn vị, AD vẫn giữ rule system | Chưa đủ / NOT_VERIFIED |

### 9.5. Màn chuyên biệt ngoài 29 resource

Không tăng số resource để tính các trang này; không tạo FR mapping mới. Route snapshot/FR/AT hiện có tiếp tục truy ở [ma trận](dms-traceability-matrix.md); các kết luận cũ về nối `detailRoutes` và số test trong ma trận phải được agent chính căn chỉnh sau, không áp trái quyết định mục 4.2. Với màn xử lý, tách **VIEW popup** và **WORKFLOW**; không chuyển toàn bộ action thành form CRUD.

| Ticket / màn / portal | File owner module (dưới `DMS/frontend/src/`) | Công việc hữu hạn / action nguồn | H / E / P cần nghiệm thu | Evidence / trạng thái |
|---|---|---|---|---|
| #42 UI-ACC / Import / AD | `pages/admin/StudentImportPage.tsx`, `features/account/StudentImportPanel.tsx` | Chọn file → preview → confirm apply → kết quả/credential; không mất preview khi lỗi; bảng hồ sơ dùng `adminStudents` | H valid/skip/error totals đúng; E file sai, mã đầu 0, apply lặp/lỗi; P AD và secret một lần, không cache/audit mật khẩu | Chưa đủ / NOT_VERIFIED |
| #42 UI-ACC / Role matrix / AD | `pages/admin/RolePermissionsPage.tsx` | Đọc catalog/version từ backend → sửa draft → PUT riêng → refresh quyền; không đổi Keycloak | H lưu/no-op; E stale 409 giữ draft, thiếu READ, chuyển role khi dirty; P ADMIN bất biến, mất quyền áp ở request kế tiếp | Chưa đủ / NOT_VERIFIED |
| #44 UI-PORTAL / Pending / QL, AD | `pages/staff/PendingItemsPage.tsx`, `features/contract/useContract.ts`, `services/api/contract.ts` | Dùng wire category uppercase từ registry; filter/count tại server; bỏ KPI giả suy từ một page hoặc ghi rõ “trang hiện tại”; link xử lý đúng ID/portal | H từng category; E total>size/đổi trang/q/rỗng/unknown type/thiếu ID; P scope/thu hồi quyền; không dùng key lowercase gửi API | Chưa đủ; lỗi xác nhận / NOT_VERIFIED |
| #44 UI-PORTAL / Dashboard & home / QL, AD, SV | `pages/staff/StaffHomePage.tsx`, `pages/student/StudentHomePage.tsx` | Số liệu API tổng hợp, thời điểm cập nhật/nguồn; drill-down đúng filter; KT kiểm entry tasks trong wave chung, không tự tạo dashboard KT | H aggregate so với list; E stale/lỗi từng block/0/mẫu số 0; P scope và lối tắt theo quyền, không page-derived tổng giả | Chưa đủ / NOT_VERIFIED |
| #44 UI-PORTAL / Inbox & badge / cả 4 role | `pages/NotificationInboxPage.tsx`, `features/notification/useInbox.ts`, `features/notification/useUnreadCount.ts` | Xem popup không ghi; mark-read/read-all là action riêng có chủ đích, invalidate badge/count; deep link chỉ nguồn được phép | H read/read-all; E lỗi mạng/lặp action/nguồn đã mất; P chỉ người nhận, đổi user không nhận cache cũ | Chưa đủ / NOT_VERIFIED |
| #43 UI-REG / Duyệt–ưu tiên–phân giường / SV, QL, AD | `features/registration/ApplicationDetailView.tsx`, `features/registration/VerifyPriorityModal.tsx`, `features/registration/useRegistration.ts` | Tách section xem khỏi action `allowedActions`; chọn giường/confirm/reason theo API, giữ HOLD/deadline/history | H approve→assign; E hết chỗ/submit lặp/release-expiry race/rollback; P quyền phụ và cùng loại/đơn vị, SV không duyệt | Chưa đủ / NOT_VERIFIED |
| #43 UI-CON / Activation–extension–transfer–summer / SV, QL, AD | `features/contract/ContractDetailView.tsx`, `ActivateContractModal.tsx`, `ExtensionReviewModal.tsx` trong cùng thư mục; `pages/staff/TransferReviewPage.tsx`, `pages/student/TransferRequestPage.tsx`, `features/registration/SummerChoiceDetailView.tsx` | Mở workflow rõ ràng từ ngữ cảnh; thu đúng cọc/kích hoạt, duyệt yêu cầu theo policy; chuyển mode không mutation trong VIEW | H HOLD→STAY, gia hạn/hè; E sai cọc/hết hạn/chồng ngày/giường bị chiếm; P owner/unit/quyền action, AD không bypass state | Chưa đủ / NOT_VERIFIED |
| #42 UI-ROOM / Giường–leader / QL, AD | `pages/staff/RoomDetailPage.tsx`, `features/room/BedCrudPanel.tsx` | VIEW popup có beds/leader/history; đổi trạng thái/sửa mã/gán leader ở workflow, không sửa occupancy như field | H đúng resident; E giường có history, leader rời phòng; P unit, không chuyển phòng nguồn của bed | Chưa đủ / NOT_VERIFIED |
| #44 UI-MNT / Phân công–xử lý–nghiệm thu–kết luận / cả 4 role | `features/maintenance/MaintenanceDetailView.tsx`, `AssignTechnicianModal.tsx`, `TicketNoteModal.tsx`, `ConcludeTicketModal.tsx`, `useMaintenance.ts` trong cùng thư mục; `pages/technician/TaskDetailPage.tsx` | Mode workflow theo `allowedActions`; ảnh/history cùng nguồn xem; phân biệt WEAR/MISUSE và ledger sau kết luận | H assign→start→resolve→accept→conclude; E reopen, job close race, lỗi ledger rollback; P KTV được giao/owner/QL, không sửa tiền trong VIEW | Chưa đủ / NOT_VERIFIED |
| #44 UI-BIL / Phát hành–thu tiền–cọc / QL, AD | `features/billing/IssueInvoicesModal.tsx`, `ConfirmPaymentModal.tsx`, `DepositLedgerModal.tsx`, `useBilling.ts` | Preview/confirm theo contract hiện hữu, khóa submit, refresh invoice/ledger/dashboard; không mutation khi chỉ xem ledger | H issue/confirm/topup; E trùng/PAID/lỗi giữa transaction/semester cũ; P quyền chính/phụ và scope từng nhánh | Chưa đủ / NOT_VERIFIED |
| #43 UI-CON / Move-out & storage / QL, AD | `pages/staff/MoveOutPage.tsx`, `pages/staff/StoragePage.tsx`, `features/contract/MoveOutModal.tsx`, `StorageModal.tsx`, `useCheckout.ts` trong cùng thư mục | Preview → cảnh báo/ack → execute; gửi/nhận đồ riêng; chứng từ readonly, không thêm CRUD sửa/xóa | H giải phóng giường/hoàn cọc/nhận đồ; E nợ/âm/0/đã trả/lặp/target taken; P scope + quyền action, snapshot/history bất biến | Chưa đủ / NOT_VERIFIED |
| #42 UI-ACC / Auth / cả 4 role | `pages/LoginPage.tsx`, `pages/ChangePasswordPage.tsx`, `services/api/auth.ts`; guard do Integrator giữ | OIDC/PKCE/đổi bắt buộc/logout/refresh và chuyển portal đúng permission; không auth mock/fallback | H login/logout từng role; E 401/token cũ/khóa/reset/required action/đa tab; P không nâng role từ body/cache, theme PREF tách identity | Chưa đủ / NOT_VERIFIED |

### 9.6. Hàng đợi sửa theo bằng chứng — không giao “sửa toàn bộ”

Các mã Q dưới đây chỉ là hàng đợi con của ticket hiện có, không thay #34–#49. Owner đang làm giữ vùng của mình; đề xuất đổi file chung qua Integrator. Reviewer chỉ đọc, không ghi chồng.

| Thứ tự / mức | Ticket / việc hữu hạn | Owner ghi / phụ thuộc | Bằng chứng đầu vào → điều kiện đóng | Trạng thái |
|---|---|---|---|---|
| Q0 / P0 nếu phát hiện | #46: chặn rò dữ liệu, mất lịch sử/ledger hoặc migration phá dữ liệu | Lead dừng wave; Security/DB triage trước mọi rollout | Chưa có P0 mới được xác nhận trong lượt tài liệu; nếu có phải có repro, cách cô lập và review độc lập trước mở lại | Theo dõi, không gán PASS |
| Q1 / P1 xác nhận | #41: ROOM_FEE write scope theo semester | BE-Billing giữ `InvoiceCrudServiceImpl.java`; QA-Test giữ regression; không đổi shared UI cùng lúc | Đã sửa predicate cùng quy tắc đọc/thu; `InvoiceCrudScopeIntegrationTest` 12 invocations gồm sai kỳ/direct ID/aggregate/ADMIN/PAID, chạy trong full355 pass | BACKEND_TEST_PASS; browser/HTTP scope vẫn mở |
| Q2 / P1 nghiệm thu UX | #38 + #43: popup parity, nhãn hợp đồng/học kỳ hè, semantics thời gian | Integrator + FE-REG/CON theo file tách; cần detail response/lookup map | Đã tách display/write whitelist, adapter tái dùng 7 detail view cho 10 resource/alias, VIEW readonly, workflow riêng. Edge summer View/Edit smoke pass; serializer immutable test pass, chưa lưu thật/các role | PATCHED/SMOKE_PASS; full resource acceptance NOT_VERIFIED |
| Q3 / P1 chức năng danh sách | #44: Pending category/count không khớp wire/data scope | FE-Portal giữ `PendingItemsPage.tsx` và API adapter đã giao; registry do Integrator | Đã bỏ KPI tổng giả/lowercase, giữ filter registry + total từ server; Xem mở popup readonly. Edge lọc Gia hạn trả 1 dòng, URL không đổi khi mở popup | PATCHED/SMOKE_PASS; phân trang lớn và đa role còn mở |
| Q4 / P1 gate quyền | #36/#37/#46: preference isolation và HTTP/race | BE-Account/Integrator; QA-Test riêng, chờ Q1–Q3 không ghi shared | Giữ PREF REVIEW; 6 targeted không thay full → test hai account/tab, stale version, race first insert, logout/debounce, 4 portal/themes | REVIEW; browser NOT_VERIFIED |
| Q5 / P0 ưu tiên kiểm tra | #40/#41/#49: khóa lưu trú, đủ kỳ, event/ledger | TV2–TV5 theo RACI; tách test owner, cần freeze API/lock order | Test cạnh tranh/rollback + DEC-BIL-01; chưa tái hiện không tự tuyên bố thêm lỗi P0; patch đang có cần review và full retest | Mở theo ticket cha |

## 10. Phối hợp nhiều agent và kiểm tra nhau

### 10.1. RACI và khóa file

| Vùng | Người ghi duy nhất trong một wave | Ai review | Quy tắc |
|---|---|---|---|
| UX blueprint/palette | Qoder / agent thiết kế | Người dùng + reviewer độc lập + FE-Core | Chốt pilot trước thay code chung; không chờ Anti. |
| theme/App/CSS/shared controls | Shared Integrator duy nhất (gộp FE-Core/FE-CRUD trong wave) | Reviewer thiết kế độc lập + QA | Module gửi yêu cầu thay token/prop, không tự sửa file chung. |
| CrudPage/resources/types/navigation/routes/exports | Cùng Shared Integrator | Module owner + QA | Một hàng đợi integration; không chia theo portal vì Admin dùng màn Staff; không nối `detailRoutes` cho Xem/Sửa. |
| Account/config/preferences | BE/FE-Account theo file tách biệt | TV1/Security | Preferences không mở rộng quyền người dùng. |
| Room/occupancy | TV2/agent room | TV3 + concurrency QA | Không ghi assignment từ module khác. |
| Registration/contract | TV3/agent contract | TV2/TV4 + QA | Public port/API frozen trước parallel work. |
| Billing | TV5/agent billing | TV4 + Security | Tiền, ledger, invoice scope có tests. |
| Maintenance/checkout/storage/notification | TV4/agent tương ứng | TV5/TV3 + QA | Tách event notification khỏi state transaction đúng SAD. |
| Test dùng chung | QA test owner được chốt | Implementer + reviewer khác | Không hai agent cùng sửa CrudApiIntegrationTest/SystemJobsIntegrationTest. |
| Báo cáo/plan chung | Lead | Các owner xác nhận | Agent gửi handoff thay vì tự tick toàn kế hoạch. |

### 10.2. Brief bắt buộc khi giao việc

Mỗi lần dispatch phải có: ticket ID, SHA nền, mục tiêu/WHY, file được ghi và file cấm, API/schema/permission được phép thay hay không, AC/test, dependency, ai chạy lệnh build, nơi ghi bàn giao, điều kiện dừng. Không gửi yêu cầu chung “xem rồi sửa toàn dự án”.

- Agent triển khai chỉ nhận file ownership cụ thể; reviewer chỉ đọc.
- Dùng isolated worktree khi có cấu hình phù hợp; mỗi worktree có test/build output riêng, không nhiều Maven process chung `target`.
- Baseline/smoke trước, implement nhỏ, owner tự kiểm, reviewer kiểm độc lập, Lead chạy lại gate và đọc diff thật.
- Thay đổi ngoài scope hoặc file bất ngờ: dừng vùng đó và hỏi Lead/người dùng, không reset/stash/restore toàn repo.
- Findings tới sau khi user dừng/hủy không tạo lý do tiếp tục sửa. Chỉ áp dụng khi có yêu cầu tiếp theo.

### 10.3. Tổ chức thiết kế sau khi chốt Qoder + reviewer

1. Theo DEC-UX-01, Qoder/agent thiết kế nhận DMS-UX-01, reviewer độc lập chỉ đọc/kiểm kết quả. Không cần chờ hoặc tự cài/cấu hình Anti.
2. Dispatch DMS-UX-01 trước: chỉ design/pilot. Sau khi người dùng duyệt, FE-Core giữ file chung; module agents triển khai theo ticket và được review riêng.
3. Designer không sửa quyền/DB/định giá hoặc merge branch ngoài scope UI. Reviewer không tự duyệt code mình viết; QA/Business kiểm độc lập.
4. Nếu Anti được cấu hình về sau và người dùng muốn sử dụng, tạo phân công giới hạn rõ; không đổi owner file giữa chừng hoặc giả nhận nhiệm vụ đã được Anti thực hiện.

### 10.4. Sổ ownership file cho ba wave

Đây là phân công dự kiến để Lead chốt với owner đang chạy, không tự cấp quyền sửa code từ lượt tài liệu. Giữ một owner cho mỗi file qua W1–W3; chuyển owner chỉ sau handoff/diff và đóng quyền ghi cũ. File chưa liệt kê là **chưa được giao**, phải bổ sung trước dispatch, không hiểu tên module là quyền ghi cả thư mục. Bảng 9.5 diễn tả phạm vi chức năng; bảng này quyết định quyền ghi khi hai mô tả giao nhau.

Gốc tuyệt đối: `F = D:/Dormitory-management-system/DMS/frontend/src/`; `B = D:/Dormitory-management-system/DMS/backend/src/main/java/vn/edu/tlu/dms/`; `T = D:/Dormitory-management-system/DMS/backend/src/test/java/vn/edu/tlu/dms/`. Mỗi tên trong danh sách dưới đây là file cụ thể dưới gốc tương ứng, không có wildcard.

| Chủ sở hữu duy nhất | File được giữ khi triển khai | Ranh giới / handoff |
|---|---|---|
| Shared Integrator — shared CRUD/registry | `F/pages/CrudPage.tsx`; `F/features/crud/resources.ts`; `F/features/crud/types.ts`; `F/features/crud/api.ts`; `F/features/crud/RecordDetailContent.tsx`; `F/pages/CrudTabsPage.tsx`; `F/pages/CrudHomePage.tsx` | Module gửi field/group/mapper/caller impact đề nghị; không tự patch các file này. |
| Cùng Shared Integrator — shell/theme/CSS | `F/app/App.tsx`; `F/app/theme.ts`; `F/app/themeStore.ts`; `F/index.css`; `F/components/ui/Surface.tsx`; `F/components/ui/FormControls.tsx`; `F/components/ui/DataTable.tsx`; `F/components/layout/UserMenu.tsx`; `F/app/layouts/SidebarShell.tsx`; `F/app/layouts/StudentLayout.tsx`; `F/app/layouts/TechnicianLayout.tsx`; `F/services/api/preferences.ts` | PREF FE tích hợp bởi cùng owner; module không thêm palette/CSS override hay sửa provider. |
| Cùng Shared Integrator — routes/constants/exports | `F/app/router/index.tsx`; `F/app/router/adminRoutes.tsx`; `F/app/router/staffRoutes.tsx`; `F/app/router/studentRoutes.tsx`; `F/app/router/technicianRoutes.tsx`; `F/app/guards/RequireRole.tsx`; `F/constants/navigation.ts`; `F/constants/authorization.ts`; `F/constants/statusConfig.ts`; `F/utils/errorMessages.ts`; `F/components/index.ts`; `F/features/billing/index.ts`; `F/features/contract/index.ts`; `F/features/maintenance/index.ts`; `F/features/notification/index.ts`; `F/features/room/index.ts` | Giữ wire code và permission; export/route mới cần caller review. Không phục hồi navigation detail cho nút Xem/Sửa. |
| FE-Account | `F/pages/admin/StudentImportPage.tsx`; `F/features/account/StudentImportPanel.tsx`; `F/pages/admin/RolePermissionsPage.tsx`; `F/pages/LoginPage.tsx`; `F/pages/ChangePasswordPage.tsx`; `F/features/account/useMe.ts`; `F/services/api/auth.ts` | Account/profile registry gửi Integrator; không sửa themeStore/App trong cùng wave. |
| FE-Room | `F/pages/staff/RoomDetailPage.tsx`; `F/features/room/BedCrudPanel.tsx` | Child giường/leader và adapter workflow; section room/myRoom gửi Integrator, không mở ghi history. |
| FE-Registration | `F/features/registration/ApplicationDetailView.tsx`; `F/features/registration/VerifyPriorityModal.tsx`; `F/features/registration/SummerChoiceDetailView.tsx`; `F/features/registration/useRegistration.ts`; `F/pages/staff/ApplicationDetailPage.tsx`; `F/pages/student/ApplicationDetailPage.tsx`; `F/pages/staff/SummerChoiceDetailPage.tsx`; `F/pages/student/SummerChoiceDetailPage.tsx` | Giữ summer adapter ở một owner; canonical registry và route requests qua Integrator. |
| FE-Contract | `F/features/contract/ContractDetailView.tsx`; `F/features/contract/ActivateContractModal.tsx`; `F/features/contract/ExtensionReviewModal.tsx`; `F/features/contract/MoveOutModal.tsx`; `F/features/contract/StorageModal.tsx`; `F/features/contract/useCheckout.ts`; `F/pages/staff/ContractDetailPage.tsx`; `F/pages/student/ContractDetailPage.tsx`; `F/pages/staff/ExtensionDetailPage.tsx`; `F/pages/student/ExtensionDetailPage.tsx`; `F/pages/staff/TransferReviewPage.tsx`; `F/pages/student/TransferRequestPage.tsx`; `F/pages/staff/MoveOutPage.tsx`; `F/pages/staff/StoragePage.tsx` | Không giữ `useContract.ts`/`services/api/contract.ts` cùng FE-Portal; gửi yêu cầu signature thay đổi qua handoff trước. |
| FE-Billing | `F/features/billing/ConfirmPaymentModal.tsx`; `F/features/billing/IssueInvoicesModal.tsx`; `F/features/billing/DepositLedgerModal.tsx`; `F/features/billing/useBilling.ts` | Mode tài chính riêng, registry/nhãn chung qua Integrator; không sửa BE/test file. |
| FE-Maintenance | `F/features/maintenance/MaintenanceDetailView.tsx`; `F/features/maintenance/MaintenancePhotoGallery.tsx`; `F/features/maintenance/CreateTicketModal.tsx`; `F/features/maintenance/AssignTechnicianModal.tsx`; `F/features/maintenance/TicketNoteModal.tsx`; `F/features/maintenance/ConcludeTicketModal.tsx`; `F/features/maintenance/useMaintenance.ts`; `F/features/maintenance/constants.ts`; `F/pages/staff/MaintenanceDetailPage.tsx`; `F/pages/student/MaintenanceDetailPage.tsx`; `F/pages/technician/TaskDetailPage.tsx` | Không thay chi phí/assignment/state trong generic edit; export/registry shared qua Integrator. |
| FE-Portal | `F/pages/staff/PendingItemsPage.tsx`; `F/pages/staff/StaffHomePage.tsx`; `F/pages/student/StudentHomePage.tsx`; `F/pages/NotificationInboxPage.tsx`; `F/features/notification/useInbox.ts`; `F/features/notification/useUnreadCount.ts`; `F/features/contract/useContract.ts`; `F/services/api/contract.ts` | Owner contract API adapter duy nhất; FE-Contract gửi change request, chốt contract trước tích hợp. Shared labels/nav vẫn của Integrator. |
| BE-Billing (fix Q1/DEC-BIL-01) | `B/billing/service/impl/InvoiceCrudServiceImpl.java`; `B/billing/service/impl/UtilityReadingServiceImpl.java` | Fix đúng phạm vi đã giao, không đổi Service/DTO/lock order ngoài review; test do QA-Test ghi. |
| BE-Account (PREF review) | `B/account/api/UserPreferenceController.java`; `B/account/domain/UserPreference.java`; `B/account/repository/UserPreferenceRepository.java`; `B/account/service/UserPreferenceService.java`; `B/account/service/impl/UserPreferenceServiceImpl.java`; `B/account/api/AccountDtos.java` | Nếu cần API/migration khác phải được Lead giao riêng; không sửa/xóa V8 đã áp dụng. Không mở lại file account đang bị owner khác giữ. |
| QA-Test — độc lập với owner code | `T/CrudApiIntegrationTest.java`; `T/InvoiceCrudScopeIntegrationTest.java`; `T/UserPreferenceIntegrationTest.java`; `T/UserPreferenceConcurrencyIntegrationTest.java`; `T/DmsApplicationTests.java`; `T/ApplicationAssignmentIntegrationTest.java`; `T/BedOccupancyIntegrationTest.java`; `T/ContractActivationIntegrationTest.java`; `T/ContractExtensionIntegrationTest.java`; `T/MoveOutIntegrationTest.java`; `T/StorageIntegrationTest.java`; `T/SummerChoiceIntegrationTest.java`; `T/DepositIntegrationTest.java`; `T/BillingConcurrencyIntegrationTest.java`; `T/MaintenanceConcludeIntegrationTest.java`; `T/SystemJobsIntegrationTest.java`; `T/KeycloakAccountIntegrationTest.java` | Một test owner ghi tuần tự theo queue, không cho hai implementer ghi CrudApi/SystemJobs. Browser QA chỉ đọc code/chụp evidence bằng công cụ sẵn có; chưa có FE unit/component runner nên chưa giao file test FE giả định. |
| Lead tài liệu / reviewer | Chỉ file kế hoạch này trong lượt chi tiết hóa; matrix/manifest/API/SRS/DB do agent chính điều phối riêng sau | Reviewer không ghi file implementer; docs contract thay đổi cần scope riêng và kết quả thật, không tự tick fix đang chạy. |

BE-Room/Registration/Contract/Maintenance/Notification của Q5 chưa có danh sách file chốt ở đây: **BLOCKED_DISPATCH** cho đến khi Lead ghi rõ file đang giữ, public port, test owner và thứ tự tích hợp. Không tận dụng bảng ownership để tự mở ticket sửa lan rộng.

## 11. Lộ trình theo phase và gate

| Phase | Nội dung | Có thể song song | Gate trước khi chuyển |
|---|---|---|---|
| 0 — Baseline/quyết định | STD-01, DATA-01, triage BIZ; ghi nhận DEC-UX-01/DEC-BIL-01 | Research tách vùng | SHA + inventory + decision log + test nền rõ |
| 1 — UX/contract | UX-01, preference contract, field modes, delete matrix | Design và BE-data sau baseline | Người dùng/QA duyệt pilot, API/data semantics rõ |
| 2 — Shared foundation | UI-CORE, PREF rồi CRUD | Backend prefs tách FE nếu contract đã chốt | Pilot view/edit/theme/permissions đạt browser |
| 3 — Module rollout | MODULE-01..03 và BIZ fixes | 3–4 track, không cùng file/test/output | Mỗi module evidence và review, không đứt chuỗi |
| 4 — Nghiệm thu | QA-01/02, AT/NFR/a11y/restore | QA visual/security/performance tách môi trường | Không P0/P1, requirements traced, evidence đúng SHA |
| 5 — Bàn giao | REL-01 và docs/hướng dẫn | Chỉ sau gate trước | Được phép publish + CI/review, rollback rõ |

Ước lượng sơ bộ khi có 3–4 track độc lập: khoảng 12–20 ngày làm việc; không phải cam kết lịch. Sau Phase 0 phải ước lượng lại theo lỗi tái hiện, số màn/mode thực tế, năng lực Qoder/reviewer được giao và mức đổi schema. Không triển khai toàn bộ UI bằng một commit khổng lồ hoặc lấy số ticket DONE thay mức hoàn thiện FR.

### 11.1. Ba wave rollout có điều kiện dừng

Phase là nhóm công việc; W1–W3 là đợt tích hợp hữu hạn trong Phase 2–4, không tự bỏ qua baseline/thiết kế hay cho phép release. Mỗi wave khóa revision working tree/diff sau khi owner ngừng ghi, nạp seed test riêng và chỉ chạy gate khi đã xác nhận không còn build/test khác dùng chung output. Handoff nhỏ theo section/resource; không chờ sửa đồng loạt 29 resource rồi mới review.

| Wave / phạm vi | Owner và dependency bắt buộc | Thứ tự tích hợp / đầu ra | Gate ra và stop criteria |
|---|---|---|---|
| W1 — Chặn lỗi đã xác nhận + 3 pilot | Shared Integrator giữ toàn bộ shared mục 10.4; FE-REG pilot `summer`, FE-CON pilot `contracts`, pilot danh mục `roomTypes` do Integrator; BE-Billing Q1; FE-Portal Q3; QA-Test riêng. Cần baseline/repro, DTO/label map, rule bất biến và permission đã đối soát. | Q1 test/fix scope trước; Q2 canonical popup và workflow boundary; Q3 filter/count pending. Chốt 3 pilot VIEW/EDIT cùng ID, history/children readonly, loading/error, token theme; giữ PREF REVIEW. | Dừng mở rộng nếu P0/P1 chưa xử lý, View còn mutation/ID trần, EDIT mất section, lookup vượt scope, count sai hoặc mất draft. Cần backend regression đúng revision + lint/build + browser pilot ở 360/768/1440 light/dark; chưa đủ thì W2 BLOCKED. |
| W2 — Rollout 29 resource + luồng module | Mỗi module chỉ giữ feature files mục 10.4; Integrator nhận queue registry/routes/CSS; QA-Test quản lý test chung. Phụ thuộc pilot W1 được reviewer/người dùng chấp nhận; public port/DTO frozen; Q5 có file ownership trước dispatch. | Theo nhóm 9.4: account/room (12) → registration/contract (9) → billing/maintenance/notification (8), có thể song song feature không giao nhau. Mỗi nhóm bàn giao map field, H/E/P, impact query và mode workflow; hoàn thiện trang 9.5 không gắn CRUD sai lifecycle. | Dừng nhóm bị lỗi quyền/state/history/ledger/409; không kéo patch shared chưa review sang nhóm khác. Gate mỗi nhóm: bảng 9.4 đủ evidence, chuỗi xuyên module 7.1 không đứt; không lấy pass nhóm trước bảo chứng nhóm sau. |
| W3 — Freeze, hồi quy bốn portal, NFR/bàn giao | Lead freeze diff; Integrator chỉ tích hợp fix đã review; QA-Test chạy full; Browser QA kiểm 4 portal; performance/restore owner dùng môi trường riêng. Phụ thuộc W2 hoàn tất, scope test/data/revision rõ. | Chạy lại full backend/lint/build rồi browser và AT; preference isolation/tab/device; performance/restore đo riêng; agent chính cập nhật matrix/manifest và mục 13 với output thật. | Mọi P0/P1 hoặc gate chức năng/browser chưa đạt chặn nghiệm thu. NFR chưa đo = NOT_VERIFIED; thiếu restore/load không PASS. Chỉ sang REL khi đủ gate hoặc ngoại lệ được phê duyệt rõ, không đổi nhãn mục chưa đạt. Không tự commit/publish. |

**Quy tắc tiến độ:** sau mỗi pilot/nhóm, owner báo số kịch bản đã kiểm và lỗi còn mở, không báo phần trăm hoàn thiện. Owner code → QA-Test test tái hiện/hồi quy → reviewer độc lập → Integrator tích hợp → Lead freeze/retest. Khi có fix sau freeze, cấp revision mới và chạy lại các gate bị tác động; không dùng log trước fix làm kết quả cho bản cuối.

## 12. Ma trận nghiệm thu và điều kiện hoàn thành

### 12.1. Ma trận tối thiểu cho mỗi resource/màn

`Role × READ/CREATE/UPDATE/DELETE/action × trạng thái × scope × mode × viewport × theme`.

- CRUD hợp lệ; ID không tồn tại/ngoài scope; field lạ; missing/null/empty; false/0; khóa ngoại sai hoặc đã inactive.
- Thao tác không hợp lệ theo state, dữ liệu có tham chiếu/history, phiên hết hạn, quyền thu hồi, conflict và submit kép.
- Count/content/filter/sort/page khớp, query không sang resource khác, back giữ state.
- Read-only không gửi mutation, không mở quyền ghi qua UI, history không sửa được.
- Kiểm fresh DB và DB đã có data, đường nâng cấp migration, concurrent requests với transaction timeout rõ.

### 12.2. Gate tổng

- [ ] Tất cả resource/màn nghiệp vụ có mẫu view/edit và trạng thái phản hồi được duyệt.
- [ ] Baseline 36 bảng V1–V7 và bảng NEW/V8 thứ 37 có rule/owner/test mapping; đối chiếu 38 base table khi tính Flyway history, không đổi mã manifest cũ.
- [ ] Preference theo user được kiểm account isolation, logout/race/tab/đa thiết bị và contrast.
- [ ] Mọi module theo shared tokens, không màn sáng/tối hoặc popup lệch palette.
- [ ] FR Must và AT-01–AT-33 có bằng chứng hợp lệ hoặc defer được chủ dự án chấp thuận; thiếu không được tính hoàn thiện.
- [ ] Backend unit/ArchUnit/integration và lint/build chạy trên revision cuối; browser/E2E có kết quả thật, coverage báo riêng. Frontend chưa có unit/component runner nên gate đó NOT_VERIFIED, không gọi lint/build là test; nếu cần thêm runner phải có phê duyệt riêng, không cài dependency từ kế hoạch. Không che package/CI thất bại sau test pass.
- [ ] Visual desktop/tablet/mobile, bàn phím, zoom và reduced-motion đạt; có screenshots thật, không chỉ mock.
- [ ] NFR hiệu năng/khả dụng/backup-restore bắt buộc đã được đo và đạt theo SRS. Hạng mục chưa đo phải giữ `BLOCKED/NOT_VERIFIED`, không tick hoặc tính là đạt; muốn bàn giao với ngoại lệ cần chủ dự án phê duyệt bằng văn bản, nêu phạm vi, ảnh hưởng, owner và hạn xử lý, nhưng vẫn không ghi NFR đó là PASS.
- [ ] Mọi ticket có reviewer độc lập, ghi chú dev, giới hạn còn lại và quyết định nghiệp vụ.
- [ ] Không secret, token, log test hoặc artifact lớn không cần thiết trong Git; stash giữ/áp dụng đúng quyết định.
- [ ] Chỉ release sau khi người dùng cho phép và CI đúng SHA xanh.

### 12.3. Bộ ca chức năng bắt buộc cho popup và workflow

Chạy trên bản ghi seed có định danh đối chiếu được; ghi input/expected/actual và revision, không chỉ chụp ảnh trạng thái thành công. Các ca G là mã nghiệm thu nội bộ của kế hoạch, không phải FR/AT mới. Khi resource readonly, ghi EDIT không áp dụng kèm căn cứ `readOnly`, không tạo quyền sửa để chạy đủ ca.

| Ca | Chuẩn bị / thao tác | Expected bắt buộc | Bằng chứng tối thiểu |
|---|---|---|---|
| G01 — Popup/parity | Cùng ID mở VIEW rồi EDIT từ list, dashboard, inbox hoặc pending khi có entry đọc; so từng section | Không đổi URL sang detail; cùng nhãn/thứ tự/dữ liệu, immutable/history/children giữ ở EDIT; đổi mode không tự ghi | Ảnh cặp VIEW/EDIT + ID/URL + map field/response; readonly ghi N/A EDIT có lý do |
| G02 — Null/missing/empty | Nạp detail có null, sửa một field, không gửi field khác; thử xóa text tùy chọn | Null/thiếu theo contract giữ cũ khi partial; chuỗi rỗng chỉ xóa field cho phép; thời điểm chưa có không mượn createdAt; UI không crash | Response trước, payload save, response sau; field errors |
| G03 — False/0 | Seed active=false, số/giá/chỉ số=0 hợp lệ; mở EDIT rồi lưu field khác | Không dùng truthy fallback đổi false thành true hoặc 0 thành trống/default; giá trị ngoài miền bị validation, không tự đổi rule | Ảnh/payload và đối chiếu DB/API, không secret |
| G04 — Lookup inactive/mất option | Chọn record có nguồn inactive hoặc ngoài trang đầu lookup, chuyển VIEW/EDIT | Giữ nhãn từ detail/lookup có quyền; readonly nguồn cũ vẫn hiện; không chỉ số ID trần, không ép thay nguồn mới để lưu | Network lookup/filter/page + nhãn nguồn trong hai mode |
| G05 — ID không thấy | ID bị xóa, ngoài scope, sai kiểu; lookup nguồn không đọc được | 404 cho mất/ngoài scope theo contract, sai kiểu bị từ chối; popup lỗi có retry/đóng, không hiện form tạo thay; không lộ tên nguồn khác scope | HTTP/errorCode + UI + không phát sinh record |
| G06 — Stale 409 | Hai phiên mở cùng record/version, phiên B đổi trước rồi A lưu/action | Báo xung đột, giữ draft A, cho đọc lại/đối chiếu có chủ đích; không tự retry ghi hoặc overwrite; quyền/action cập nhật theo server | Hai request có thứ tự + UI trước/sau; nếu API dùng state conflict thì ghi rõ thay vì giả có version |
| G07 — Save thất bại | API trả field/business error, 5xx hoặc mất mạng lúc lưu | Popup còn mở, draft/ảnh cũ/lookup còn nguyên; lỗi trường đúng chỗ; nút hết busy khi request kết thúc, retry không gửi kép | Request/error + ảnh draft trước/sau, xác nhận transaction không ghi nửa chừng |
| G08 — Đóng khi busy/dirty | Trong lúc save/action pending bấm X/Escape/mask/Lưu lần nữa; thử đóng draft chưa lưu; đóng khi đang tải detail | Không đóng/gửi trùng khi đang ghi; không hiện success sớm; dirty có confirm; detail load bị hủy/bỏ response cũ khi đóng hoặc đổi ID, không hydrate bản ghi mới | Video/chuỗi ảnh + request count, UI cuối và focus trigger |
| G09 — History/children immutable | Detail có audit/history/ledger/items/ảnh, mở EDIT và thử gửi field hệ thống ngoài whitelist | Hiện đủ như VIEW, readonly; backend từ chối field ngoài contract/ghi bất biến theo DTO; không xóa history/ledger hoặc ảnh để lưu được | Ảnh hai mode, payload loại field hệ thống, negative API test |
| G10 — VIEW không ghi | Theo dõi Network, mở/đóng VIEW, expand tab/children, đổi viewport/theme | Không save/approve/payment/delete/cancel/mark-read hay mutation nghiệp vụ; POST search-by-page là đọc hợp lệ nếu không ghi DB, không cấm mọi POST máy móc. Pref save do người dùng đổi theme ghi nhận riêng, không tính là mutation bản ghi xem | Network đã lọc theo resource + DB/audit trước/sau khi cần; không có nút ghi trong VIEW |
| G11 — Quyền/allowedActions | READ-only; thu hồi UPDATE sau mở; QL đọc tổng hợp; SV khác owner; KT khác assignment | VIEW còn theo quyền đọc, EDIT/WF chỉ hiện đúng quyền/action; gọi API trực tiếp vẫn bị chặn, không effect phụ; AD cũng giữ state/history | /me permissions, detail allowedActions, negative HTTP + rollback assertions |
| G12 — Responsive/theme/a11y | VIEW/EDIT/WF ở 360/768/1440, light/dark; các preset; keyboard/zoom 200% | Không tràn toàn trang, footer/nút không che input; history/children cuộn dùng được; focus/label/contrast rõ, popup portal cùng theme | Screenshot ghi width/height/role/mode/preset; keyboard/focus và phép đo contrast, không suy từ màu token |
| G13 — Workflow riêng | Từ VIEW chỉ chuyển mode, thực hiện action hợp lệ với confirm/reason rồi reload | Request ghi chỉ sau confirm tại WORKFLOW; đúng ID/scope/state, nguyên tử, history/audit/query invalidation đúng; lỗi action không giả success | Chuỗi VIEW→WF→request→refresh + kiểm list/detail/pending/dashboard liên quan |
| G14 — Count và category | Dataset nhiều hơn page size, ít nhất hai category; đổi filter/q/page/sort ở pending/list | Wire category đúng registry, count/content cùng scope/filter trên server; không lấy rows.length/filter một trang làm tổng category. Nếu chưa có aggregate API, bỏ KPI tổng hoặc ghi rõ số trên trang | Payload search/response totalElements và đối chiếu nhiều trang; không bịa số/endpoint |

### 12.4. Baseline và gate bằng chứng không thay thế nhau

| Gate | Trạng thái hiện được phép ghi | Điều kiện được ghi PASS |
|---|---|---|
| Snapshot source/revision | HEAD nền `266f588` + working tree chưa commit, đang có owner khác sửa | Lead freeze có thời gian/múi giờ, diff fingerprint/danh sách file và môi trường; SHA HEAD riêng không chứng minh working tree |
| Backend full suite | 342 PASS lịch sử tại WT-full 10:46:54, PREF=5; không chứng nhận WT-targeted/WT cuối | Full retest sau handoff trên PostgreSQL test riêng; lệnh thực chạy bắt đầu `rtk`, exit/summary/số skip và raw log; không suy tổng test dự kiến |
| PREF targeted | 6 PASS tại WT-targeted 10:57:28; ticket REVIEW | HTTP/permission/race và browser isolation còn phải kiểm; targeted không đóng full suite |
| Frontend static | Lint/build lịch sử pass, bundle warning đã ghi; bản cuối chờ agent chính retest | `rtk proxy npm run lint`, `rtk proxy npm run build` trên cùng WT freeze; ghi warning/exit riêng |
| Frontend unit/component | **NOT_VERIFIED — chưa có runner**, không có test result FE unit để trích | Chỉ ghi pass khi runner được chấp thuận, test tồn tại và thực chạy; không cài deps/config trong lượt tài liệu này |
| Browser chức năng/visual | Repro lỗi summer đã có, coverage 4 portal chưa đủ; #37/#38 NOT_VERIFIED | Đủ matrix portal/width bên dưới, G01–G14 theo resource, screenshot/console/network và reviewer; lint/build không thay gate này |
| Schema/migration | Baseline 36 + V8=37 ứng dụng; 38 base gồm Flyway history; log test thấy version 8 | Fresh/upgrade thật trên DB riêng và inventory schema/count/constraints đúng; không dùng table count thay kiểm dữ liệu/lifecycle |
| Load/performance | NOT_VERIFIED | Đo workload NFR-PER-01/02 mục 9.3, có dữ liệu/thời lượng/concurrency/p95/lỗi, cấu hình và raw result; build/test chức năng không là load test |
| Backup/restore | NOT_VERIFIED | Backup DB/uploads/Keycloak + restore phối hợp trên môi trường riêng được duyệt; đo thời gian, kiểm dữ liệu/login/file sau phục hồi; có backup chưa chứng minh restore |
| CI/release | Chưa có CI cho WT chưa commit; không release trong lượt này | Chỉ sau quyền commit/PR/release riêng và CI đúng SHA, không lấy log local làm CI |

**Browser bắt buộc, không chỉ Admin pilot:** mỗi ô là evidence cần thu, chưa phải kết quả. Mỗi ô kiểm light/dark; bộ preset kiểm đủ trên shared pilot, module kiểm tương phản và palette đang dùng. Với action role không được phép, kiểm bị chặn thay vì tạo quyền để test.

| Portal | 360 px | 768 px | 1440 px | Luồng đặc trưng / evidence cần gắn |
|---|---|---|---|---|
| Student | NOT_VERIFIED | NOT_VERIFIED | NOT_VERIFIED | Đơn/hè/hồ sơ popup, contract/invoice/deposit readonly, inbox; owner isolation và bottom nav |
| Staff | NOT_VERIFIED | NOT_VERIFIED | NOT_VERIFIED | Pending/filter/count, duyệt/phân/kích hoạt, invoice/ledger, move-out/storage; scope và readonly vs WF |
| Technician | NOT_VERIFIED | NOT_VERIFIED | NOT_VERIFIED | Task popup/ảnh/history, assigned workflow, profile/inbox; task không được giao bị chặn |
| Admin | NOT_VERIFIED | NOT_VERIFIED | NOT_VERIFIED | Summer repro, catalog/user/import/role matrix/audit/config và màn Staff tái dùng; AD không bypass state |

Evidence mỗi ô: timestamp + revision/WT freeze + role/ID seed + viewport thực + theme/mode + screenshot hoặc video + console/network đã bỏ token/PII + expected/actual + người kiểm/reviewer. Audit hiện chỉ cung cấp repro Edge `localhost:5173/admin/summer`, chưa có bộ ảnh/mode/width đầy đủ nên không tự đổi ô nào thành PASS. Agent chính bổ sung kết quả retest cuối phiên, không điền sẵn số test/ảnh hoặc kết luận fix đạt.

## 13. Mẫu ghi chú dev và bằng chứng theo ticket

Sao chép mẫu vào nhật ký triển khai của ticket/báo cáo nhóm khi bắt đầu thực hiện. Không điền PASS trước khi chạy.

```text
Ticket / child task:
Owner / reviewer:
SHA nền / SHA hoặc working tree đang kiểm:
Mục tiêu và lý do:
Quy tắc SRS/BR/API liên quan:
File và hàm thay đổi:
Hành vi trước / sau; field immutable và scope:
API/schema/config/prefs thay đổi hoặc không thay:
Tái dùng component/service/DTO nào; vì sao chưa tái dùng được phần khác:
Transaction/lock/audit/event/query invalidation:
Lệnh kiểm tra, ngày giờ, exit code, số test:
Ảnh/log artifact (viewport/theme/role/test data, không secret):
Review findings / cách xử lý:
Còn thiếu / blocker / quyết định được chấp thuận:
Rollback / cách kiểm lại sau rollback:
```

Đối với thay đổi shared: bổ sung caller impact list và before/after của cả bốn portal. Đối với bảng DB mới: migration số nào, fresh/upgrade/restore, rule xóa và dữ liệu nhạy cảm. Đối với code quan trọng: giải thích WHY/invariant trong code thật bằng comment ngắn, diễn giải dài để ở đây/tài liệu chuẩn.

### 13.1. Nhật ký bàn giao đợt 31/10/2026 (PREF + tài liệu nền)

**DMS-PREF-01 — Sở thích giao diện theo tài khoản (backend)**
- Owner/reviewer: agent BE-Account / reviewer độc lập DMS-QA-01.
- SHA nền: `266f588` + working tree (chưa commit).
- Thay đổi: Flyway `V8__user_preference.sql` (bảng 1-1 `app_user`, CHECK mode/preset/version, không cascade); entity `UserPreference` implement `Persistable` để lần lưu đầu dùng `INSERT`; repository khóa dòng; service kiểm whitelist/version (`CONCURRENT_UPDATE` khi lệch hoặc khi hai tab cùng chèn); controller `GET/POST /api/me/preferences`, quyền `PROFILE_READ/PROFILE_UPDATE`, chủ sở hữu từ `CurrentUser`; DTO typed, từ chối trường lạ.
- API/DB: `API-Spec-DMS.md` v0.20 (mục 3.1, 4.19), `DB-Design-DMS.md` v0.12 + snapshot `DB-DMS-schema.sql` cập nhật V8; `DmsApplicationTests` kỳ vọng 38 bảng base = 37 bảng ứng dụng + Flyway history, không phải 38 bảng nghiệp vụ.
- Kiểm chứng lịch sử 31/10/2026: full 342 test lúc 10:46:54 +07:00 **gồm 5 PREF**; targeted PREF sau sửa lúc 10:57:28 có **6 test**, thêm ca thiếu version trả 400 bên cạnh default/lưu/version cũ/whitelist/isolation. Hai trạng thái working tree khác nhau, chưa có full suite cùng bản sau cùng; xem log/revision ở mục 13.2.
- Review: P1 insert-merge và P1 thiếu version được ghi nhận đã sửa trong đợt PREF; không kéo kết luận đó sang P1 invoice mới. P2 HTTP/permission test và race hai transaction còn mở; chưa có audit preference (chính sách chưa chốt). Giữ ticket REVIEW.
- Rollback an toàn (đính chính khuyến nghị cũ “gỡ V8”): V8 đã áp dụng trên DB test theo log targeted; **giữ nguyên migration, checksum và dữ liệu**. Nếu cần lùi UI/API, owner phải chứng minh code cũ tương thích schema và không mất preference; nếu schema/data lỗi thì sửa bằng migration tiến phiên bản sau được duyệt. Phục hồi chỉ bằng backup/restore DB/uploads/Keycloak phối hợp, có phê duyệt, điểm phục hồi và kiểm thử trên môi trường riêng; không tự xóa file V8, xóa Flyway history, drop/truncate bảng hoặc chạy repair để che lỗi.

**DMS-PREF-02 — Sở thích giao diện theo tài khoản (frontend)**
- Thay đổi: `services/api/preferences.ts`; `themeStore.ts` namespace cache `dms_user_theme_preferences:<id>`, version, hydrate/reset, đăng ký lưu; `ThemePreferenceBridge` trong `App.tsx` xử lý logout/debounce/409/đa tab, máy chủ thắng cache, không lưu echo, giữ thao tác người dùng nếu GET về muộn.
- Kiểm chứng: `npm run lint` 0 lỗi/0 cảnh báo; `npm run build` exit 0 (cảnh báo bundle >500 kB có sẵn). Chưa có browser test hai tài khoản/đa tab; chưa có test runner frontend.
- Tồn tại có chủ đích: thay đổi trong <600 ms trước khi đóng tab có thể chỉ nằm ở cache; lần đăng nhập sau máy chủ thắng.

**DMS-DATA-01 — Manifest dữ liệu**
- Tạo `dms-data-manifest-36-tables.md`: 36/36 bảng V1–V7, 6 view, 3 sequence, 1 trigger; mỗi bảng có owner/vòng đời/UI/test và trạng thái `NOT_VERIFIED`; 14 khoảng trống cần owner quyết định.
- Kiểm tra read-only: danh sách bảng migration và tiêu đề manifest khớp 36/36, mỗi tên xuất hiện đúng một lần. Chưa đối chiếu `information_schema` thật.

**DMS-STD-01 — Ma trận truy vết**
- Tạo `dms-traceability-matrix.md`: 29 resource, 94 route entry trong 4 portal, 78 FR, đủ AT-01–AT-33, sổ kiểm chứng và gate 12.2 để `OPEN/NOT_VERIFIED`.
- Bằng chứng: log full test 342 test (working tree), lint/build frontend; ảnh 360/768/1440 và CI đúng SHA chưa có.

**DMS-QA-01 — Review độc lập đợt này**
- Verdict ban đầu `CHANGES_REQUIRED`: 0 P0, 2 P1 (insert/merge, thiếu version) và các P2/P3 tài liệu/whitespace. Đã sửa P1, P2 snapshot/manifest và P3 race GET muộn/EOF; còn P2 test HTTP-race là follow-up.
- Chưa review browser/visual; chưa có reviewer độc lập cho các module ngoài PREF trong đợt này.

### 13.2. Sổ evidence theo thời điểm/revision — đính chính số test

Các nhãn WT-full/WT-targeted dưới đây dùng để phân biệt snapshot, **không phải SHA đã commit**. Cả hai dựa HEAD `266f588` + thay đổi chưa commit; log không chứa fingerprint đầy đủ nên không thể khẳng định toàn bộ file trùng nhau. Không cộng test giữa các lượt hoặc áp kết quả cho code đang được sửa sau log. Mốc kết thúc lấy từ raw log, múi giờ +07:00.

| Ngày giờ / snapshot | Log dưới `DMS/backend/data/plan-20261031-test/` | Kết quả thực đọc | Phạm vi / giới hạn |
|---|---|---|---|
| 31/10/2026 10:46:54 / WT-full | `wave2-full-test.log`: dòng 861, 868, 871, 874 | PREF **5**; tổng **342**; failure/error/skip đều 0; BUILD SUCCESS | Full suite lịch sử, PostgreSQL 16 cổng 55442. Không có ca PREF thứ 6 của targeted sau; không phải CI/coverage/hiệu năng/restore. |
| 31/10/2026 10:57:28 / WT-targeted | `wave2-pref-retest2.log`: dòng 55–56, 74, 78, 81, 84 | PREF **6**; failure/error/skip đều 0; BUILD SUCCESS; schema đang version 8 | Chỉ `UserPreferenceIntegrationTest` sau sửa thiếu version; không phải full 342 cùng revision. Log ignored, không đưa credential/log test vào Git. |
| 31/10/2026 11:26:34 +07 / backend sau sửa invoice scope | `audit-clean-full-test.log`, JDK 17 + PostgreSQL 16 riêng cổng 55444, schema mới V1–V8 | **355 test, 0 failures/errors/skipped, BUILD SUCCESS** | Gồm 12 invocations invoice-scope/PAID và 6 preference test cũ. Không phải CI/E2E; không cộng gộp test targeted sau vào tổng full này. |
| 31/10/2026 11:31:21 +07 / targeted preference race mới | `audit-pref-concurrency.log`, `UserPreferenceConcurrencyIntegrationTest` | **1 test, 0 failures/errors/skipped, BUILD SUCCESS** | Hai transaction độc lập cùng lưu lần đầu: 1 winner/version1 và 1 conflict409. Spy chỉ đồng bộ lookup, insert/flush/constraint PostgreSQL thật. Chưa full retest gồm test mới này. |
| Lần chạy lặp cùng DB test cũ / hạn chế fixture | `audit-final-test.log` | **355 test, 16 errors vì fixture semester chồng ngày**, không tính PASS | Chạy lại trên schema mới cổng 55444 đạt 355/355. Không bỏ constraint/đổi assertion; cần ticket riêng để các fixture test lặp lại cùng DB an toàn. |
| Frontend sau tích hợp adapter/shared popup | `npm run lint` và `npm run build` chạy riêng từ DMS/frontend, exit0 | **Lint 0 lỗi/0 cảnh báo; build PASS**, còn cảnh báo bundle >500 kB | Smoke isolation/hydration của themeStore và serializer sản xuất pass tại bộ nhớ, không gọi API. Không thay test component/E2E hay kiểm đủ 29 resource. |
| Edge localhost Admin / smoke thật | `/admin/summer` bản ghi #2: View→Edit→Close; `/admin/pending-items` lọc Gia hạn→Xem | **PASS trong phạm vi smoke không ghi dữ liệu** | VIEW Save=0, approve=0, choice control=0; EDIT Save=1, choice control=1, giá trị Rời KTX nạp sẵn; giữ sinh viên/hợp đồng/học kỳ/tiến trình. Pending Gia hạn trả 1 dòng và popup không đổi URL. Ảnh inline trong hội thoại; chưa kiểm network-no-write, lưu thật, 4 role/3 viewport nên các gate đầy đủ vẫn NOT_VERIFIED. |

Ma trận truy vết/manifest vẫn là snapshot cũ; agent chính sẽ căn chỉnh bằng chứng và quyết định popup-only sau khi nhận kết quả. Lượt tài liệu này không sửa hai file đó, không hồi tố log 342 thành 6 PREF.

### 13.3. Audit mới nhất — 31/10/2026, AUDIT_IN_PROGRESS

Audit/fix agents đang hoạt động; các dòng sau ghi lỗi đã thấy và owner xử lý, **không xác nhận patch hiện tại đã đạt**. Repro browser được bàn giao trong phiên, không phải test mới do lượt sửa tài liệu thực hiện.

| Finding / ticket | Bằng chứng xác nhận / expected | Chủ sở hữu code và kiểm tra độc lập | Gate còn mở |
|---|---|---|---|
| Popup summer / #38, #43 | Edge `localhost:5173/admin/summer`, popup VIEW hiện hợp đồng/học kỳ bằng **8/4** thay nhãn; header có createdAt nhưng submittedAt trống; VIEW/EDIT chưa đủ parity. Expected đúng semantics thời gian, nhãn/scope và cùng section immutable/history/children ở hai mode | Shared Integrator: `CrudPage.tsx`, `resources.ts`, `types.ts`, CSS; FE-Registration: `SummerChoiceDetailView.tsx`; QA browser đọc network/so cùng ID | Fix NOT_VERIFIED; không suy “submittedAt phải có” khi API không có, không lấy createdAt gán thay; G01/G04/G09/G10 + ảnh đủ width |
| Pending / #44 | `PendingItemsPage.tsx` dùng key lowercase cho category trong khi wire uppercase; KPI category lấy `rows.filter(...).length` của trang hiện tại, không phải tổng toàn scope. Expected filter đúng code, count từ server hoặc bỏ/đổi nhãn KPI | FE-Portal giữ page + `useContract.ts`/API adapter; shared registry do Integrator, QA-Test/Browser QA kiểm dataset nhiều trang | Fix NOT_VERIFIED; cần test payload từng category/q/page/scope và total, không tạo endpoint/count giả |
| ROOM_FEE scope / #41, P1 | Audit xác nhận `InvoiceCrudServiceImpl` kiểm bất kỳ application cũ thay vì semester hóa đơn, có thể mở ghi sai đơn vị/kỳ. Expected scope theo học kỳ, aggregate read không write | BE-Billing đang sửa file service; QA-Test độc quyền regression trong `CrudApiIntegrationTest.java`; Security reviewer chỉ đọc | Fix NOT_VERIFIED; tạo/sửa/xóa, kỳ đúng/sai, list/detail/confirm và rollback; full retest trên revision cuối |
| Chất lượng trạng thái / #37, #38 | VERIFIED trước đây chỉ dựa lint/build không chứng minh chức năng/browser. Đã hiệu chỉnh kế hoạch về REVIEW, giữ tick code là IMPLEMENTED | Lead cập nhật plan; reviewer xác nhận gate; agent chính đồng bộ matrix sau | Browser/chức năng NOT_VERIFIED; PREF #36 giữ REVIEW, không lùi/xóa lịch sử test đã có |

**Handoff triển khai sau audit:** các owner đã bàn giao patch invoice scope + 12 invocations hồi quy/PAID, canonical display–write whitelist (không gửi immutable khi sửa), bỏ summer note ngoài DTO, adapter embedded readonly tái dùng 7 detail view, workflow riêng dùng allowedActions/permission (transfer dùng ROOM_TRANSFER_APPROVE + targetBedId), JSON audit giữ cấu trúc lồng, close/submit guard và Form mount ổn định. Pending bỏ KPI page-derived và mở popup tại chỗ; CSS khôi phục căn tiền, density và không tạo hàng đo giả; preference giữ draft khi storage/409 GET về muộn. Reviewer độc lập không tìm thêm P1 trong phạm vi cuối; P2 lookup phòng đã thêm gate theo quyền danh mục. Kết quả thực ở mục 13.2; không coi scope reviewer đó là đã audit hết project.

**Còn thiếu có chủ đích:** workflow move-out/storage hiện còn navigation riêng (VIEW/EDIT không điều hướng); save CRUD thật/HTTP permission và đầy đủ 29 resource × 4 role × 3 viewport chưa nghiệm thu; browser chỉ smoke Admin trên dữ liệu mẫu không ghi. Chưa có draft warning đầy đủ, full network-no-write trace, NFR tải/coverage/restore và CI đúng SHA. Quy tắc assign loại phòng/đơn vị ở BIZ-01 vẫn phải xử lý theo SRS, không bị full test xanh che mất. Hai cluster audit riêng đã dừng; không thay DB dev/Keycloak, không commit/push. Giữ REVIEW/NOT_VERIFIED cho gate tương ứng.

**Kiểm chứng riêng tài liệu:** script Node chỉ đọc, chạy qua `rtk proxy`, xác nhận 29/29 khóa bảng 9.4 khớp registry, không trùng/thiếu; đủ mục chính 1–15; số cột các bảng Markdown nhất quán; 111 đường dẫn ownership có file thật, không giao trùng; gate nghiệm thu vẫn mở. Đây không phải test runtime/frontend/backend hay bằng chứng fix đã đạt.

## 14. Rủi ro và biện pháp kiểm soát

| Rủi ro | Kiểm soát |
|---|---|
| Làm đẹp nhưng phá nghiệp vụ hoặc quyền | BIZ/DATA contract trước UI; reviewer nghiệp vụ + permission/state/scope test. |
| Nhiều agent sửa cùng file chung | Một shared owner/queue; module chỉ viết file được giao; test target riêng. |
| Copy HR cả bug hoặc API khác convention | Reference matrix mục 3; DMS SRS/API thắng; parity test và typed DTO. |
| Tài khoản dùng nhầm theme/cache của người trước | Backend owner self, cache namespace, cancel timer, race/multitab tests. |
| Màu custom không đọc được | Token derivation/validation/contrast gate, default preset an toàn, không tùy ý đổi semantic danger. |
| DB refactor hoặc xóa mềm đại trà phá ledger/history | Rule baseline 36 + bảng NEW/V8; giữ migration đã chạy/FK/check/append-only. V8 không “gỡ”; sửa tiến phiên bản sau hoặc restore phối hợp được duyệt. |
| Chuyển JPQL làm sai Unicode/count/scope | So sánh parity PostgreSQL; chưa chứng minh thì defer, không đổi hình thức. |
| Test “xanh” vì bỏ constraint/mock hoặc chỉnh assertion để qua | DB sạch riêng, không bypass rule, QA đọc test/diff và raw summary. |
| Dev không hiểu code sau khi agent bàn giao | Naming/JavaDoc + note theo ticket, link entry points và test, review bằng developer khác. |
| Cổng/app project khác bị chiếm | Kiểm PID/command trước khi start; không kill app khác, chốt cổng/redirect dev được phép. |
| Kế hoạch bị coi là kết quả hoàn thành | Báo đúng trạng thái từng ticket; IMPLEMENTED không phải VERIFIED; tách WT-full/targeted/final và browser/NFR, không dùng lint/build hoặc số test làm phần trăm production-ready. |

## 15. Nhật ký cập nhật kế hoạch

| Ngày | Nội dung | Trạng thái |
|---|---|---|
| 31/10/2026 | Lập kế hoạch tổng thể theo yêu cầu; bốn agent đọc HR theme/CRUD và DMS UI/data; tạo 15 ticket cha nội bộ #34–#48, có dependency và reviewer; ghi rõ Anti chưa configured. | PLAN_ONLY |
| 31/10/2026 | Review độc lập kế hoạch: sửa cách mô tả NFR-PER-02 cho đúng workload hóa đơn khoảng 1.500 sinh viên; siết gate NFR chưa kiểm chứng không được tính PASS. Kiểm tra cấu trúc Markdown, liên kết nguồn và ma trận đủ 36/36 bảng bằng script chỉ đọc. | PLAN_REVIEWED; không thay cho nghiệm thu triển khai |
| 31/10/2026 | Người dùng xác nhận Qoder + reviewer độc lập và điện nước phải nhập đủ kỳ; bổ sung DEC-UX-01/DEC-BIL-01, bỏ chặn cấu hình Anti, đồng bộ ticket/phân công. Các nội dung khác theo SRS; chưa bắt đầu triển khai. | DECISIONS_CONFIRMED; PLAN_ONLY |
| 31/10/2026 | Triển khai đợt PREF và tài liệu nền bằng bốn agent song song + reviewer độc lập: V8/API, theme bridge/cache/409, manifest 36 bảng, ma trận 29 resource/94 route/78 FR/33 AT. Đính chính evidence: full 342 **gồm 5 PREF** lúc 10:46:54; targeted sau có **6 PREF** lúc 10:57:28, không phải full cùng revision. Lint/build lịch sử pass. Reviewer đợt PREF: 0 P0, 2 P1 được sửa; browser isolation và HTTP/race còn mở. | PREF REVIEW; DATA/STD IN_PROGRESS; chưa commit |
| 31/10/2026 | Chi tiết hóa tại chỗ theo audit: chốt popup-only VIEW/EDIT và workflow riêng; sửa #37/#38 về REVIEW, tick code IMPLEMENTED; thêm bảng 29 resource + trang chuyên biệt, queue P0/P1, ownership file, 3 wave và ca G01–G14/gate 4 portal. Ghi lỗi Edge summer 8/4 + thời gian/parity, Pending lowercase/count theo page, P1 ROOM_FEE scope; owner code đang xử lý, chưa xác nhận fix. Tách full/targeted/final, đính chính 36/37/38 bảng và rollback V8. Chỉ sửa file kế hoạch, không sửa matrix/manifest/code/config hay cài dependency. | **AUDIT_IN_PROGRESS; fixes/browser NOT_VERIFIED; retest cuối phiên PENDING do agent chính bổ sung** |

**Bước thực hiện tiếp theo:** các owner đang review/fix bàn giao Q1–Q3 trước, QA-Test/reviewer độc lập kiểm; agent chính freeze revision, chạy retest phù hợp và điền kết quả thật ở mục 13.2–13.3, rồi căn chỉnh matrix/manifest. Chỉ mở rộng W2 sau gate pilot W1; PREF giữ REVIEW đến khi đủ isolation/race/browser. Tài liệu này không tự cấp phép sửa rộng code, schema, quyền hoặc phát hành.

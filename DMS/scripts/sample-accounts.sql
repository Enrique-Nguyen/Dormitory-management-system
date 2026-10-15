-- DMS - mở khóa tài khoản mẫu để đăng nhập thử qua UI (chạy SAU sample-data.sql).
-- Không chứa mật khẩu hay Keycloak subject. Việc cấp danh tính Keycloak và mật khẩu tạm ngẫu nhiên
-- dùng luồng có sẵn (start-local.ps1 -ProvisionAccounts) hoặc Admin UI; xem README, mục
-- "Tài khoản mẫu đăng nhập thử". Script idempotent, chạy trong một transaction, không xóa dữ liệu.
-- Không đưa file này vào thư mục migration.
BEGIN;

DO $$
DECLARE
    tech_unit_one BIGINT;
    staff_unit_one BIGINT;
    building_one  BIGINT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM audit_log WHERE request_id = 'DMS-SAMPLE-V1') THEN
        RAISE EXCEPTION 'Chưa có dữ liệu mẫu DMS-SAMPLE-V1. Hãy nạp DMS/scripts/sample-data.sql trước.';
    END IF;

    -- Chỉ mở khóa các tài khoản cần thử; các tài khoản mau.* khác giữ nguyên trạng thái khóa.
    UPDATE app_user
       SET locked = FALSE, failed_login_count = 0, locked_until = NULL
     WHERE username IN ('mau.ql.1', 'mau.ql.2', 'mau.kt.1', 'mau.kt.2',
                        'mau.sv.01', 'mau.sv.02', 'mau.sv.07', 'mau.sv.10', 'mau.sv.11', 'mau.sv.12');

    -- Kỹ thuật viên K1 cần có việc: MAU-TK-02 đã giao cho mau.kt.2, MAU-TK-03 đã đóng.
    SELECT id INTO tech_unit_one FROM app_user WHERE username = 'mau.kt.1';
    SELECT id INTO staff_unit_one FROM app_user WHERE username = 'mau.ql.1';
    SELECT id INTO building_one FROM building WHERE code = 'MAU-1';
    IF tech_unit_one IS NULL OR staff_unit_one IS NULL OR building_one IS NULL THEN
        RAISE EXCEPTION 'Thiếu mau.kt.1, mau.ql.1 hoặc tòa MAU-1; kiểm tra lại dữ liệu mẫu.';
    END IF;
    INSERT INTO maintenance_request(code, requester_id, building_id, floor, category, severity,
                                    description, status, assigned_to)
    SELECT 'MAU-TK-04', staff_unit_one, building_one, 1, 'ELECTRIC', 'MEDIUM',
           'Mẫu - Đèn hành lang tầng 1 chập chờn, đã giao kỹ thuật viên.', 'ASSIGNED', tech_unit_one
     WHERE NOT EXISTS (SELECT 1 FROM maintenance_request WHERE code = 'MAU-TK-04');
END $$;

COMMIT;

-- Kiểm tra: linked = 'chưa' nghĩa là cần cấp Keycloak (đường A trong README);
-- linked = 'rồi' mà đang bị khóa ở Keycloak thì dùng Admin UI Mở khóa / Đặt lại mật khẩu (đường B).
SELECT u.username, u.role, mu.code AS unit, u.locked,
       CASE WHEN u.keycloak_user_id IS NULL THEN 'chưa' ELSE 'rồi' END AS linked
  FROM app_user u LEFT JOIN management_unit mu ON mu.id = u.management_unit_id
 WHERE u.username IN ('admin', 'mau.ql.1', 'mau.ql.2', 'mau.kt.1', 'mau.kt.2',
                      'mau.sv.01', 'mau.sv.02', 'mau.sv.07', 'mau.sv.10', 'mau.sv.11', 'mau.sv.12')
 ORDER BY u.role, u.username;

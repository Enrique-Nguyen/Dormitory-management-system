-- Liên kết bằng subject bất biến của Keycloak; không tự ghép bằng username.
ALTER TABLE app_user ADD COLUMN keycloak_user_id VARCHAR(255);
CREATE UNIQUE INDEX ux_app_user_keycloak_user_id ON app_user(keycloak_user_id)
    WHERE keycloak_user_id IS NOT NULL;
-- Tài khoản Keycloak không lưu thêm bản sao mật khẩu tại DMS.
ALTER TABLE app_user ALTER COLUMN password_hash DROP NOT NULL;

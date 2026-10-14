-- Phiên giữ phiên bản bảo mật lúc đăng nhập; thay đổi mật khẩu/quyền làm phiên cũ hết hiệu lực.
ALTER TABLE app_user
    ADD COLUMN security_version BIGINT NOT NULL DEFAULT 0
    CONSTRAINT ck_user_security_version CHECK (security_version >= 0);

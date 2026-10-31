-- DMS-PREF-01: sở thích giao diện theo tài khoản (theme mode, accent preset, mật độ bảng).
-- Bảng 1-1 với app_user qua PK/FK; mỗi tài khoản chỉ có tối đa một dòng.
-- Tài khoản có lịch sử nghiệp vụ được khóa chứ không xóa cứng, nên không dùng ON DELETE CASCADE.
-- Không sửa V1-V7; migration tăng version theo thông lệ.
CREATE TABLE user_preference (
    user_id       BIGINT PRIMARY KEY REFERENCES app_user(id),
    theme_mode    VARCHAR(10) NOT NULL DEFAULT 'light'
                  CONSTRAINT ck_user_preference_theme_mode
                  CHECK (theme_mode IN ('light', 'dark')),
    theme_preset  VARCHAR(20) NOT NULL DEFAULT 'blue'
                  CONSTRAINT ck_user_preference_theme_preset
                  CHECK (theme_preset IN ('blue', 'indigo', 'teal', 'emerald', 'violet', 'amber')),
    table_dense   BOOLEAN NOT NULL DEFAULT FALSE,
    version       BIGINT NOT NULL DEFAULT 0
                  CONSTRAINT ck_user_preference_version CHECK (version >= 0),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE user_preference IS
    'Sở thích giao diện của từng tài khoản; module account ghi, không xóa cứng khi tài khoản còn';
COMMENT ON COLUMN user_preference.version IS
    'Phiên bản chống ghi đè; tăng 1 sau mỗi lần lưu, không phải security_version của tài khoản';

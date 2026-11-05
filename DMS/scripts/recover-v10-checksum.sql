-- Chỉ dùng sau khi sao lưu, dừng backend và xác nhận lỗi checksum V10.
-- Hai bản SQL tương ứng với commit b1c58b8 và 1436022; không chạy repair hàng loạt.
-- Sau bước này phải chạy backend từ source có V16 để đồng bộ ràng buộc năm học.
BEGIN;
LOCK TABLE flyway_schema_history IN EXCLUSIVE MODE;

DO $$
DECLARE
    applied_checksum INTEGER;
    applied_success BOOLEAN;
    year_constraint TEXT;
    expected_old_constraint TEXT :=
        'CHECK ((((code)::text ~ ''^[0-9]{4}-[0-9]{4}$''::text) AND (("substring"((code)::text, 6, 4))::integer = (("substring"((code)::text, 1, 4))::integer + 1))))';
    expected_current_constraint TEXT :=
        'CHECK (((code)::text ~ ''^[0-9]{4}-[0-9]{4}$''::text))';
BEGIN
    SELECT checksum, success INTO applied_checksum, applied_success
    FROM flyway_schema_history
    WHERE version = '10' AND script = 'V10__shared_catalogs.sql' AND type = 'SQL';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Không tìm thấy bản V10 đã áp dụng. Không thay đổi lịch sử Flyway.';
    END IF;
    IF NOT applied_success OR EXISTS (SELECT 1 FROM flyway_schema_history WHERE NOT success) THEN
        RAISE EXCEPTION 'Có migration thất bại. Phải kiểm tra nguyên nhân trước khi phục hồi V10.';
    END IF;
    IF applied_checksum NOT IN (-1224946798, 1020332633) OR applied_checksum IS NULL THEN
        RAISE EXCEPTION 'Checksum V10 không thuộc hai bản đã kiểm chứng: %. Không thay đổi lịch sử Flyway.', applied_checksum;
    END IF;

    IF EXISTS (
        SELECT 1 FROM (VALUES
            ('school'), ('faculty'), ('student_cohort'), ('hometown'),
            ('academic_year'), ('amenity'), ('maintenance_category')
        ) AS required(table_name)
        WHERE to_regclass(required.table_name) IS NULL
    ) OR NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'semester'::regclass AND conname = 'fk_semester_academic_year'
    ) OR NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'maintenance_request'::regclass AND conname = 'fk_maintenance_category'
    ) THEN
        RAISE EXCEPTION 'Cấu trúc danh mục V10 không đầy đủ. Không thay đổi lịch sử Flyway.';
    END IF;

    SELECT pg_get_constraintdef(oid) INTO year_constraint
    FROM pg_constraint
    WHERE conrelid = 'academic_year'::regclass AND conname = 'ck_academic_year_code';
    IF year_constraint IS NULL
            OR year_constraint NOT IN (expected_old_constraint, expected_current_constraint) THEN
        RAISE EXCEPTION 'Ràng buộc năm học khác hai bản đã kiểm chứng: %. Không thay đổi lịch sử Flyway.', year_constraint;
    END IF;
    IF applied_checksum = 1020332633 THEN
        RAISE NOTICE 'V10 đã đúng checksum hiện tại; không có thay đổi.';
        RETURN;
    END IF;

    UPDATE flyway_schema_history SET checksum = 1020332633
    WHERE version = '10' AND script = 'V10__shared_catalogs.sql'
        AND checksum = -1224946798 AND success;
    RAISE NOTICE 'Đã phục hồi riêng checksum V10. Chạy backend có V16 trước khi sử dụng ứng dụng.';
END $$;

COMMIT;

-- Đợt 2: trường nghiệp vụ đầy đủ cho danh mục (đặc tả Docs/05 Reviews/dac-ta-truong-nghiep-vu-day-du.md mục 8–19).
-- Chỉ thêm cột, ràng buộc và backfill; không đổi dữ liệu nghiệp vụ hiện có. Cột bắt buộc ở nghiệp vụ nhưng chưa
-- có dữ liệu cũ để suy ra được để nullable ở CSDL và kiểm tra bắt buộc khi tạo ở Service.

-- Trường (mục 8)
ALTER TABLE school
    ADD COLUMN short_name     VARCHAR(30),
    ADD COLUMN moet_code      VARCHAR(10),
    ADD COLUMN address        VARCHAR(300),
    ADD COLUMN is_home_school BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN note           VARCHAR(1000);
-- Tối đa một trường chủ quản.
CREATE UNIQUE INDEX ux_school_home_school ON school ((TRUE)) WHERE is_home_school;

-- Khoa (mục 9)
ALTER TABLE faculty
    ADD COLUMN short_name VARCHAR(30),
    ADD COLUMN phone      VARCHAR(15),
    ADD COLUMN email      VARCHAR(200),
    ADD COLUMN note       VARCHAR(1000);

-- Khóa (mục 11): dữ liệu cũ để trống năm, Staff bổ sung sau.
ALTER TABLE student_cohort
    ADD COLUMN enrollment_year          SMALLINT,
    ADD COLUMN expected_graduation_year SMALLINT,
    ADD COLUMN note                     VARCHAR(1000),
    ADD CONSTRAINT ck_cohort_enrollment_year CHECK (enrollment_year IS NULL OR enrollment_year BETWEEN 2000 AND 2100),
    ADD CONSTRAINT ck_cohort_graduation_year CHECK (
        expected_graduation_year IS NULL OR enrollment_year IS NULL OR expected_graduation_year > enrollment_year);

-- Quê quán (mục 13): mô hình hai cấp tỉnh/xã; bản có cấp cha được đánh dấu cấp xã.
ALTER TABLE hometown
    ADD COLUMN admin_level   VARCHAR(10) NOT NULL DEFAULT 'PROVINCE',
    ADD COLUMN gso_code      VARCHAR(10),
    ADD COLUMN priority_area VARCHAR(10),
    ADD COLUMN hardship_area BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN note          VARCHAR(1000);
UPDATE hometown SET admin_level = 'COMMUNE' WHERE parent_id IS NOT NULL;
ALTER TABLE hometown
    ADD CONSTRAINT ck_hometown_admin_level CHECK (admin_level IN ('PROVINCE', 'COMMUNE')),
    ADD CONSTRAINT ck_hometown_commune_parent CHECK (admin_level = 'PROVINCE' OR parent_id IS NOT NULL),
    ADD CONSTRAINT ck_hometown_priority_area CHECK (priority_area IS NULL OR priority_area IN ('KV1', 'KV2NT', 'KV2', 'KV3'));

-- Năm học (mục 14): backfill khung chuẩn 01/09 năm đầu đến 31/08 năm sau. Không nới theo học kỳ cũ để
-- không phá ràng buộc năm bắt đầu và không làm các năm liền kề chồng lấn; học kỳ cũ lệch khung chỉ bị kiểm
-- tra lại khi được sửa ngày hoặc đổi năm học.
ALTER TABLE academic_year
    ADD COLUMN start_date DATE,
    ADD COLUMN end_date   DATE,
    ADD COLUMN note       VARCHAR(1000);
UPDATE academic_year
SET start_date = make_date(substr(code, 1, 4)::int, 9, 1),
    end_date   = make_date(substr(code, 1, 4)::int + 1, 8, 31)
WHERE code ~ '^[0-9]{4}-[0-9]{4}$';
ALTER TABLE academic_year
    ADD CONSTRAINT ck_academic_year_dates CHECK (start_date IS NULL OR end_date IS NULL OR end_date > start_date),
    ADD CONSTRAINT ck_academic_year_start_year CHECK (
        start_date IS NULL OR EXTRACT(YEAR FROM start_date)::int = substr(code, 1, 4)::int),
    ADD CONSTRAINT ex_academic_year_period EXCLUDE USING gist (daterange(start_date, end_date, '[]') WITH &&)
        WHERE (start_date IS NOT NULL AND end_date IS NOT NULL);

-- Học kỳ (mục 15): mã sinh từ loại + năm đầu, không lưu trùng dữ liệu; năm học đã là khóa ngoại theo mã từ V10.
ALTER TABLE semester
    ADD COLUMN code VARCHAR(20) GENERATED ALWAYS AS (
        (CASE type WHEN 'SUMMER' THEN 'HE' ELSE type END) || '-' || substr(academic_year, 1, 4)) STORED,
    ADD COLUMN room_fee_due_date DATE,
    ADD COLUMN note              VARCHAR(1000);

-- Tiện nghi (mục 16)
ALTER TABLE amenity
    ADD COLUMN applies_to  VARCHAR(10) NOT NULL DEFAULT 'ROOM',
    ADD COLUMN icon        VARCHAR(50),
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN note        VARCHAR(1000),
    ADD CONSTRAINT ck_amenity_applies_to CHECK (applies_to IN ('ROOM', 'BUILDING', 'BOTH'));

-- Hạng mục sửa chữa (mục 17)
ALTER TABLE maintenance_category
    ADD COLUMN default_severity VARCHAR(10),
    ADD COLUMN sla_hours        SMALLINT,
    ADD COLUMN skill            VARCHAR(20),
    ADD COLUMN description      VARCHAR(500),
    ADD COLUMN note             VARCHAR(1000),
    ADD CONSTRAINT ck_maintenance_category_severity CHECK (
        default_severity IS NULL OR default_severity IN ('LOW', 'MEDIUM', 'HIGH')),
    ADD CONSTRAINT ck_maintenance_category_sla CHECK (sla_hours IS NULL OR sla_hours BETWEEN 1 AND 720),
    ADD CONSTRAINT ck_maintenance_category_skill CHECK (
        skill IS NULL OR skill IN ('ELECTRIC', 'PLUMBING', 'CARPENTRY', 'HVAC', 'NETWORK', 'GENERAL'));

-- Diện ưu tiên (mục 18): mô tả bắt buộc khi tạo; bản cũ lấy tên làm mô tả.
ALTER TABLE priority_category
    ADD COLUMN description        VARCHAR(1000),
    ADD COLUMN evidence_required  VARCHAR(500),
    ADD COLUMN evidence_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN legal_basis        VARCHAR(300),
    ADD COLUMN sort_order         SMALLINT,
    ADD COLUMN auto_rule          VARCHAR(30),
    ADD COLUMN note               VARCHAR(1000),
    ADD CONSTRAINT ck_priority_category_sort_order CHECK (sort_order IS NULL OR sort_order >= 0),
    ADD CONSTRAINT ck_priority_category_auto_rule CHECK (
        auto_rule IS NULL OR auto_rule IN ('ETHNIC_MINORITY', 'HARDSHIP_AREA', 'NONE'));
UPDATE priority_category SET description = name WHERE description IS NULL;

-- Vị trí kho (mục 19): tòa và cán bộ phụ trách thuộc cùng đơn vị (Service kiểm tra).
ALTER TABLE storage_location
    ADD COLUMN building_id    BIGINT REFERENCES building (id),
    ADD COLUMN place          VARCHAR(100),
    ADD COLUMN capacity       INTEGER,
    ADD COLUMN keeper_user_id BIGINT REFERENCES app_user (id),
    ADD COLUMN note           VARCHAR(1000),
    ADD CONSTRAINT ck_storage_location_capacity CHECK (capacity IS NULL OR capacity > 0);

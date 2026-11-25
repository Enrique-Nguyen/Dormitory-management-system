-- Đợt 1 đặc tả trường nghiệp vụ đầy đủ (Docs/05 Reviews/dac-ta-truong-nghiep-vu-day-du.md, Phần I).
-- Chỉ thêm cột/ràng buộc; dữ liệu cũ được điền giá trị suy ra trước khi đặt NOT NULL.

-- 1. Đơn vị quản lý
ALTER TABLE management_unit
    ADD COLUMN short_name    VARCHAR(30),
    ADD COLUMN address       VARCHAR(300),
    ADD COLUMN hotline       VARCHAR(15),
    ADD COLUMN email         VARCHAR(200),
    ADD COLUMN head_user_id  BIGINT REFERENCES app_user(id),
    ADD COLUMN working_hours VARCHAR(100),
    ADD COLUMN active        BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN note          VARCHAR(1000);

-- 2. Tòa nhà
ALTER TABLE building
    ADD COLUMN floor_count       SMALLINT,
    ADD COLUMN has_ground_floor  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN gender_policy     VARCHAR(10),
    ADD COLUMN address           VARCHAR(300),
    ADD COLUMN commissioned_year SMALLINT,
    ADD COLUMN floor_area_m2     NUMERIC(10,2),
    ADD COLUMN manager_user_id   BIGINT REFERENCES app_user(id),
    ADD COLUMN hotline           VARCHAR(15),
    ADD COLUMN open_time         TIME,
    ADD COLUMN close_time        TIME,
    ADD COLUMN note              VARCHAR(1000);

UPDATE building b
SET floor_count = GREATEST(1,
        COALESCE((SELECT MAX(f.floor) FROM building_floor f WHERE f.building_id = b.id), 0),
        COALESCE((SELECT MAX(r.floor) FROM room r WHERE r.building_id = b.id), 0)),
    has_ground_floor = EXISTS (SELECT 1 FROM building_floor f WHERE f.building_id = b.id AND f.floor = 0)
        OR EXISTS (SELECT 1 FROM room r WHERE r.building_id = b.id AND r.floor = 0),
    gender_policy = CASE
        WHEN (SELECT COUNT(DISTINCT r.gender) FROM room r WHERE r.building_id = b.id) = 1
            THEN (SELECT MIN(r.gender) FROM room r WHERE r.building_id = b.id)
        ELSE 'MIXED' END;

-- Mặc định chỉ để bản ghi tạo bằng SQL nội bộ không lỗi; API luôn yêu cầu nhập hai trường này.
ALTER TABLE building
    ALTER COLUMN floor_count SET DEFAULT 1,
    ALTER COLUMN floor_count SET NOT NULL,
    ALTER COLUMN gender_policy SET DEFAULT 'MIXED',
    ALTER COLUMN gender_policy SET NOT NULL,
    ADD CONSTRAINT ck_building_floor_count CHECK (floor_count BETWEEN 1 AND 30),
    ADD CONSTRAINT ck_building_gender_policy CHECK (gender_policy IN ('M', 'F', 'MIXED')),
    ADD CONSTRAINT ck_building_commissioned_year CHECK (commissioned_year IS NULL OR commissioned_year >= 1950),
    ADD CONSTRAINT ck_building_floor_area CHECK (floor_area_m2 IS NULL OR floor_area_m2 > 0);

-- 3. Tầng
ALTER TABLE building_floor
    ADD COLUMN gender          CHAR(1) CHECK (gender IN ('M', 'F')),
    ADD COLUMN shared_bathroom BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN common_room     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN note            VARCHAR(1000);

-- 4. Loại phòng: thêm mã ổn định để tham chiếu từ Excel
ALTER TABLE room_type
    ADD COLUMN code             VARCHAR(30),
    ADD COLUMN private_bathroom BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN has_balcony      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN bed_style        VARCHAR(10) NOT NULL DEFAULT 'BUNK',
    ADD COLUMN has_kitchen      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN description      VARCHAR(1000);

CREATE SEQUENCE room_type_code_seq;
UPDATE room_type SET code = 'LP' || lpad(nextval('room_type_code_seq')::text, 3, '0') WHERE code IS NULL;

-- Mã sinh tự động khi bản ghi được tạo không kèm mã (SQL nội bộ, dữ liệu cũ); API vẫn nhận mã do người dùng nhập.
ALTER TABLE room_type
    ALTER COLUMN code SET DEFAULT ('LP' || lpad(nextval('room_type_code_seq')::text, 3, '0')),
    ALTER COLUMN code SET NOT NULL,
    ADD CONSTRAINT ux_room_type_code UNIQUE (code),
    ADD CONSTRAINT ck_room_type_bed_style CHECK (bed_style IN ('SINGLE', 'BUNK'));

-- 5. Phòng
ALTER TABLE room
    ADD COLUMN display_name      VARCHAR(50),
    ADD COLUMN area_m2           NUMERIC(6,2) CHECK (area_m2 IS NULL OR area_m2 > 0),
    ADD COLUMN orientation       VARCHAR(2)
        CHECK (orientation IN ('N', 'S', 'E', 'W', 'NE', 'NW', 'SE', 'SW')),
    ADD COLUMN electric_meter_no VARCHAR(30),
    ADD COLUMN water_meter_no    VARCHAR(30),
    ADD COLUMN commissioned_on   DATE,
    ADD COLUMN status_reason     VARCHAR(500),
    ADD COLUMN note              VARCHAR(1000);

CREATE UNIQUE INDEX ux_room_electric_meter ON room(electric_meter_no) WHERE electric_meter_no IS NOT NULL;
CREATE UNIQUE INDEX ux_room_water_meter ON room(water_meter_no) WHERE water_meter_no IS NOT NULL;

-- 6. Giường
ALTER TABLE bed
    ADD COLUMN position   VARCHAR(10) CHECK (position IN ('UPPER', 'LOWER', 'SINGLE')),
    ADD COLUMN asset_code VARCHAR(30),
    ADD COLUMN note       VARCHAR(500);

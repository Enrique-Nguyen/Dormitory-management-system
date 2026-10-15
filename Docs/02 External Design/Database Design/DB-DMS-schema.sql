-- =====================================================================
-- DMS - Hệ thống quản lý ký túc xá sinh viên
-- Lược đồ nghiệp vụ, bản 0.6 (15/10/2026): Keycloak và ma trận quyền động
-- DBMS: PostgreSQL 16
-- Căn cứ: SRS-DMS.md v0.10. Giải thích chi tiết: DB-Design-DMS.md v0.6
-- Đây là snapshot bảng nghiệp vụ hiện tại, gồm bổ sung của migration V3–V5.
-- Ứng dụng nâng cấp bằng Flyway V1–V5; V2 tạo bảng Spring Session riêng.
--
-- Quy ước:
--   * Tên bảng, cột: snake_case, số ít.
--   * Khóa chính: BIGINT GENERATED ALWAYS AS IDENTITY.
--   * Tiền: BIGINT, đơn vị đồng (VND không có phần lẻ).
--   * Thời điểm: TIMESTAMPTZ; ngày: DATE.
--   * Giá trị liệt kê: VARCHAR + CHECK (dễ map với JPA @Enumerated(STRING)).
--   * Không lưu dữ liệu suy ra (số chỗ trống, số dư cọc, nhãn quá hạn...).
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------
-- 1. Tổ chức, tài khoản
-- ---------------------------------------------------------------------

CREATE TABLE management_unit (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,          -- K1, N23
    name        VARCHAR(100) NOT NULL
);

CREATE TABLE app_user (
    keycloak_user_id     VARCHAR(255), -- subject Keycloak, không ghép bằng username
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username              VARCHAR(50)  NOT NULL UNIQUE,  -- mã SV với sinh viên
    password_hash         VARCHAR(100),         -- NULL khi dùng Keycloak; BCrypt chỉ legacy-session
    role                  VARCHAR(20)  NOT NULL
                          CHECK (role IN ('STUDENT','STAFF','TECHNICIAN','ADMIN')),
    full_name             VARCHAR(100) NOT NULL,
    phone                 VARCHAR(20),
    management_unit_id    BIGINT REFERENCES management_unit(id),
    cross_unit_access     BOOLEAN NOT NULL DEFAULT FALSE, -- quyền tổng hợp liên đơn vị (FR-ACC-08)
    must_change_password  BOOLEAN NOT NULL DEFAULT TRUE,
    locked                BOOLEAN NOT NULL DEFAULT FALSE, -- khóa bởi Admin
    failed_login_count    INT     NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,                    -- khóa tạm 15 phút sau 5 lần sai
    security_version      BIGINT NOT NULL DEFAULT 0       -- tăng để thu hồi phiên đăng nhập cũ
                          CONSTRAINT ck_user_security_version CHECK (security_version >= 0),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Staff, KTV bắt buộc thuộc một đơn vị; Student, Admin không gắn đơn vị
    CONSTRAINT ck_user_unit CHECK (
        (role IN ('STAFF','TECHNICIAN') AND management_unit_id IS NOT NULL)
     OR (role IN ('STUDENT','ADMIN')    AND management_unit_id IS NULL)),
    CONSTRAINT ck_user_cross_unit CHECK (NOT cross_unit_access OR role = 'STAFF')
);

-- Hồ sơ sinh viên, 1-1 với app_user. Mọi bảng nghiệp vụ tham chiếu sinh viên
-- qua student_profile để CSDL đảm bảo "student_id thật sự là sinh viên".
CREATE UNIQUE INDEX ux_app_user_keycloak_user_id ON app_user(keycloak_user_id) WHERE keycloak_user_id IS NOT NULL;

CREATE TABLE student_profile (
    user_id            BIGINT PRIMARY KEY REFERENCES app_user(id),
    student_code       VARCHAR(20)  NOT NULL UNIQUE,
    gender             CHAR(1)      NOT NULL CHECK (gender IN ('M','F')),
    date_of_birth      DATE,
    faculty            VARCHAR(100),
    cohort             VARCHAR(10),                     -- khóa, ví dụ K65
    hometown           VARCHAR(200),
    emergency_contact  VARCHAR(200)
);

CREATE TABLE system_config (
    key          VARCHAR(50) PRIMARY KEY,
    value        VARCHAR(100) NOT NULL,
    description  VARCHAR(200),
    updated_by   BIGINT REFERENCES app_user(id),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- 2. Tòa, loại phòng, phòng, giường, học kỳ
-- ---------------------------------------------------------------------

CREATE TABLE building (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    management_unit_id  BIGINT NOT NULL REFERENCES management_unit(id),
    code                VARCHAR(10)  NOT NULL UNIQUE,    -- K1, N2, N3
    name                VARCHAR(100) NOT NULL,
    deposit_amount      BIGINT NOT NULL CHECK (deposit_amount > 0)  -- mức cọc của tòa
);

CREATE TABLE room_type (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,
    bed_count       SMALLINT NOT NULL CHECK (bed_count BETWEEN 1 AND 12),
    price_main      BIGINT   NOT NULL CHECK (price_main   >= 0),  -- giá gốc / HK1, HK2
    price_summer    BIGINT   NOT NULL CHECK (price_summer >= 0),  -- giá gốc / học kỳ hè
    area_m2         NUMERIC(5,1),
    amenities       VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE room (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    building_id        BIGINT   NOT NULL REFERENCES building(id),
    room_type_id       BIGINT   NOT NULL REFERENCES room_type(id),
    floor              SMALLINT NOT NULL,
    code               VARCHAR(20) NOT NULL,              -- A101
    gender             CHAR(1)  NOT NULL CHECK (gender IN ('M','F')),
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                       CHECK (status IN ('ACTIVE','MAINTENANCE','CLOSED')),
    leader_student_id  BIGINT REFERENCES student_profile(user_id),  -- trưởng phòng (BR-11)
    UNIQUE (building_id, code)
);
CREATE UNIQUE INDEX ux_room_leader ON room(leader_student_id) WHERE leader_student_id IS NOT NULL;

CREATE TABLE bed (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id  BIGINT NOT NULL REFERENCES room(id),
    code     VARCHAR(10) NOT NULL,                        -- 01..06
    status   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
             CHECK (status IN ('ACTIVE','BROKEN','RETIRED')),
    UNIQUE (room_id, code)
);

CREATE TABLE semester (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    academic_year  VARCHAR(9)  NOT NULL CHECK (academic_year ~ '^\d{4}-\d{4}$'),
    type           VARCHAR(10) NOT NULL CHECK (type IN ('HK1','HK2','SUMMER')),
    name           VARCHAR(50) NOT NULL,
    start_date     DATE NOT NULL,
    end_date       DATE NOT NULL,
    UNIQUE (academic_year, type),
    CHECK (end_date > start_date),
    -- các học kỳ không chồng thời gian
    EXCLUDE USING gist (daterange(start_date, end_date, '[]') WITH &&)
);

-- ---------------------------------------------------------------------
-- 3. Đợt đăng ký, đơn đăng ký, diện ưu tiên
-- ---------------------------------------------------------------------

CREATE TABLE registration_period (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    semester_id  BIGINT NOT NULL REFERENCES semester(id),
    kind         VARCHAR(10) NOT NULL CHECK (kind IN ('REGULAR','SUMMER')), -- SUMMER: đợt chọn phương án hè
    name         VARCHAR(100) NOT NULL,
    open_at      TIMESTAMPTZ NOT NULL,
    close_at     TIMESTAMPTZ NOT NULL,
    created_by   BIGINT NOT NULL REFERENCES app_user(id),
    CHECK (close_at > open_at)
);

-- Đợt áp dụng cho một hoặc cả hai đơn vị (FR-REG-01)
CREATE TABLE registration_period_unit (
    period_id           BIGINT NOT NULL REFERENCES registration_period(id),
    management_unit_id  BIGINT NOT NULL REFERENCES management_unit(id),
    PRIMARY KEY (period_id, management_unit_id)
);

-- Chỉ tiêu theo tòa (FR-REG-01)
CREATE TABLE registration_quota (
    period_id    BIGINT NOT NULL REFERENCES registration_period(id),
    building_id  BIGINT NOT NULL REFERENCES building(id),
    quota        INT    NOT NULL CHECK (quota >= 0),
    PRIMARY KEY (period_id, building_id)
);

-- Danh mục diện ưu tiên, trọng số cấu hình được (Q-07)
CREATE TABLE priority_category (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code    VARCHAR(30)  NOT NULL UNIQUE,
    name    VARCHAR(100) NOT NULL,
    weight  INT NOT NULL CHECK (weight >= 0),
    active  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE room_application (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id              BIGINT NOT NULL REFERENCES student_profile(user_id),
    period_id               BIGINT NOT NULL REFERENCES registration_period(id),
    preferred_unit_id       BIGINT NOT NULL REFERENCES management_unit(id),
    preferred_room_type_id  BIGINT REFERENCES room_type(id),
    note                    VARCHAR(500),
    priority_score          INT NOT NULL DEFAULT 0,  -- tính lại khi Staff xác nhận diện ưu tiên
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                            CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','REJECTED','ASSIGNED','CANCELLED')),
    status_reason           VARCHAR(500),            -- lý do từ chối / hủy
    submitted_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_id, period_id),
    CHECK (status <> 'REJECTED' OR status_reason IS NOT NULL)
);
CREATE INDEX ix_application_period_status ON room_application(period_id, preferred_unit_id, status, priority_score DESC);

CREATE TABLE application_priority (
    application_id  BIGINT NOT NULL REFERENCES room_application(id),
    category_id     BIGINT NOT NULL REFERENCES priority_category(id),
    verified        BOOLEAN,                 -- NULL: chưa xác nhận; TRUE / FALSE: Staff xác nhận
    verified_by     BIGINT REFERENCES app_user(id),
    verified_at     TIMESTAMPTZ,
    PRIMARY KEY (application_id, category_id)
);

-- ---------------------------------------------------------------------
-- 4. Hợp đồng, phân phòng, sổ cọc
-- ---------------------------------------------------------------------

CREATE TABLE dormitory_contract (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code                   VARCHAR(20) NOT NULL UNIQUE,     -- HD-2026-0012
    student_id             BIGINT NOT NULL REFERENCES student_profile(user_id),
    application_id         BIGINT NOT NULL REFERENCES room_application(id),
    start_date             DATE NOT NULL,
    end_date               DATE NOT NULL,
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                           CHECK (status IN ('PENDING','ACTIVE','EXPIRED','TERMINATED','CANCELLED')),
    activation_deadline    TIMESTAMPTZ NOT NULL,             -- thời điểm phân giường + N ngày (chụp lúc tạo)
    deposit_required       BIGINT CHECK (deposit_required > 0), -- chụp mức cọc của tòa khi kích hoạt
    activated_at           TIMESTAMPTZ,
    activated_by           BIGINT REFERENCES app_user(id),
    closed_at              TIMESTAMPTZ,                      -- thời điểm EXPIRED / TERMINATED / CANCELLED
    status_reason          VARCHAR(500),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (end_date > start_date),
    CHECK (status NOT IN ('ACTIVE','EXPIRED','TERMINATED')
           OR (activated_at IS NOT NULL AND deposit_required IS NOT NULL)),
    UNIQUE (id, student_id)                                   -- cho khóa ngoại kép từ phân phòng
);
-- BR-02: mỗi sinh viên tối đa một hợp đồng PENDING / ACTIVE
CREATE UNIQUE INDEX ux_contract_live_student ON dormitory_contract(student_id)
    WHERE status IN ('PENDING','ACTIVE');
-- Một đơn có thể sinh nhiều hợp đồng (hủy giữ chỗ rồi phân lại) nhưng tối đa một hợp đồng còn sống
CREATE UNIQUE INDEX ux_contract_live_application ON dormitory_contract(application_id)
    WHERE status IN ('PENDING','ACTIVE');
CREATE INDEX ix_contract_pending_deadline ON dormitory_contract(activation_deadline)
    WHERE status = 'PENDING';

-- Nguồn duy nhất cho "ai ở giường nào, từ khi nào, giá bao nhiêu" (BR-01, BR-02)
CREATE TABLE room_assignment_history (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id    BIGINT NOT NULL,
    contract_id   BIGINT NOT NULL,
    bed_id        BIGINT NOT NULL REFERENCES bed(id),
    kind          VARCHAR(10) NOT NULL CHECK (kind IN ('HOLD','STAY')),
    started_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    stay_started_at TIMESTAMPTZ,                        -- thời điểm HOLD -> STAY trên cùng dòng
    ended_at      TIMESTAMPTZ,
    end_reason    VARCHAR(20) CHECK (end_reason IN
                  ('HOLD_CANCELLED','HOLD_EXPIRED','TRANSFER','MOVE_OUT','SUMMER_LEAVE')),
    price_main    BIGINT NOT NULL CHECK (price_main   >= 0),  -- đơn giá chụp từ loại phòng
    price_summer  BIGINT NOT NULL CHECK (price_summer >= 0),
    note          VARCHAR(500),
    created_by    BIGINT NOT NULL REFERENCES app_user(id),
    -- student_id phải khớp với sinh viên của hợp đồng
    FOREIGN KEY (contract_id, student_id) REFERENCES dormitory_contract(id, student_id),
    CHECK (ended_at IS NULL OR ended_at >= started_at),
    CHECK ((ended_at IS NULL) = (end_reason IS NULL)),
    CHECK ((kind = 'STAY') = (stay_started_at IS NOT NULL))
);
-- BR-01: mỗi giường tối đa một phân phòng hiệu lực
CREATE UNIQUE INDEX ux_assignment_open_bed     ON room_assignment_history(bed_id)     WHERE ended_at IS NULL;
-- BR-02: mỗi sinh viên tối đa một phân phòng hiệu lực
CREATE UNIQUE INDEX ux_assignment_open_student ON room_assignment_history(student_id) WHERE ended_at IS NULL;
CREATE INDEX ix_assignment_contract ON room_assignment_history(contract_id);
CREATE INDEX ix_assignment_bed_time ON room_assignment_history(bed_id, started_at);
CREATE INDEX ix_assignment_student  ON room_assignment_history(student_id, stay_started_at);  -- v_user_notification

CREATE TABLE contract_extension_request (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_id      BIGINT NOT NULL REFERENCES dormitory_contract(id),
    source           VARCHAR(20) NOT NULL DEFAULT 'STUDENT'
                     CHECK (source IN ('STUDENT','SUMMER_STAY')),  -- ở hè được ghi như một gia hạn
    old_end_date     DATE NOT NULL,
    new_end_date     DATE NOT NULL,
    reason           VARCHAR(500),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    decided_by       BIGINT REFERENCES app_user(id),
    decided_at       TIMESTAMPTZ,
    decision_reason  VARCHAR(500),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (new_end_date > old_end_date),
    CHECK (status <> 'REJECTED' OR decision_reason IS NOT NULL)
);
CREATE UNIQUE INDEX ux_extension_pending ON contract_extension_request(contract_id) WHERE status = 'PENDING';

CREATE TABLE room_transfer_request (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_id      BIGINT NOT NULL REFERENCES dormitory_contract(id),
    reason           VARCHAR(500) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    target_bed_id    BIGINT REFERENCES bed(id),        -- Staff chọn khi duyệt
    decided_by       BIGINT REFERENCES app_user(id),
    decided_at       TIMESTAMPTZ,
    decision_reason  VARCHAR(500),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (status <> 'APPROVED' OR target_bed_id IS NOT NULL),
    CHECK (status <> 'REJECTED' OR decision_reason IS NOT NULL)
);
CREATE UNIQUE INDEX ux_transfer_pending ON room_transfer_request(contract_id) WHERE status = 'PENDING';

CREATE TABLE move_out_record (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_id            BIGINT NOT NULL UNIQUE REFERENCES dormitory_contract(id),
    moved_out_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    asset_condition        VARCHAR(10) NOT NULL CHECK (asset_condition IN ('OK','DAMAGED')),
    asset_note             VARCHAR(500),
    unpaid_room_fee        BIGINT NOT NULL DEFAULT 0,   -- ảnh chụp công nợ phí phòng lúc trả
    deposit_balance        BIGINT NOT NULL,             -- số dư cọc lúc trả (có thể âm)
    refund_amount          BIGINT NOT NULL CHECK (refund_amount >= 0),
    confirmed_by           BIGINT NOT NULL REFERENCES app_user(id),
    CHECK (asset_condition = 'OK' OR asset_note IS NOT NULL),
    CHECK (refund_amount = GREATEST(deposit_balance, 0))  -- BR-10: hoàn đúng số dư nếu dương
);

-- ---------------------------------------------------------------------
-- 5. Sửa chữa
-- ---------------------------------------------------------------------

CREATE TABLE maintenance_request (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code             VARCHAR(20) NOT NULL UNIQUE,         -- TK-1024
    requester_id     BIGINT NOT NULL REFERENCES app_user(id),
    room_id          BIGINT REFERENCES room(id),           -- ticket của phòng
    building_id      BIGINT REFERENCES building(id),       -- ticket khu vực chung
    floor            SMALLINT,
    category         VARCHAR(20) NOT NULL
                     CHECK (category IN ('ELECTRIC','WATER','FURNITURE','DOOR_LOCK','OTHER')),
    severity         VARCHAR(10) NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH')),
    description      VARCHAR(1000) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                     CHECK (status IN ('OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED','REJECTED','CANCELLED')),
    assigned_to      BIGINT REFERENCES app_user(id),
    resolved_at      TIMESTAMPTZ,                         -- dùng cho tự đóng sau 3 ngày
    closed_at        TIMESTAMPTZ,
    cause            VARCHAR(10) CHECK (cause IN ('WEAR','MISUSE')), -- hao mòn / hư hỏng do sử dụng
    repair_cost      BIGINT,
    cost_note        VARCHAR(500),
    cost_decided_by  BIGINT REFERENCES app_user(id),
    cost_decided_at  TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- đúng một loại vị trí
    CHECK ((room_id IS NOT NULL AND building_id IS NULL AND floor IS NULL)
        OR (room_id IS NULL AND building_id IS NOT NULL AND floor IS NOT NULL)),
    CHECK (status NOT IN ('ASSIGNED','IN_PROGRESS','RESOLVED') OR assigned_to IS NOT NULL),
    -- nguyên nhân, chi phí chỉ có ở ticket của phòng đã CLOSED (FR-MNT-09)
    CHECK (cause IS NULL OR (room_id IS NOT NULL AND status = 'CLOSED')),
    CHECK (cause IS DISTINCT FROM 'MISUSE' OR repair_cost > 0),
    CHECK (cause IS DISTINCT FROM 'WEAR'   OR (repair_cost IS NULL AND cost_note IS NOT NULL)),
    CHECK (repair_cost IS NULL OR cause = 'MISUSE')
);
CREATE INDEX ix_maintenance_status   ON maintenance_request(status);
CREATE INDEX ix_maintenance_assignee ON maintenance_request(assigned_to, status);
CREATE INDEX ix_maintenance_room     ON maintenance_request(room_id);

CREATE TABLE maintenance_photo (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_id   BIGINT NOT NULL REFERENCES maintenance_request(id),
    file_path    VARCHAR(300) NOT NULL,
    uploaded_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Sổ cọc (BR-10). Số dư = SUM(amount), không lưu.
CREATE TABLE deposit_transaction (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_id             BIGINT NOT NULL REFERENCES dormitory_contract(id),
    type                    VARCHAR(10) NOT NULL CHECK (type IN ('DEPOSIT','DEDUCTION','TOPUP','REFUND')),
    amount                  BIGINT NOT NULL,      -- có dấu: thu, bù > 0; trừ, hoàn < 0
    maintenance_request_id  BIGINT REFERENCES maintenance_request(id),
    due_date                DATE,                 -- hạn bù cọc, chỉ có ở DEDUCTION
    confirmed_by            BIGINT REFERENCES app_user(id),  -- NULL khi hệ thống tự ghi trừ cọc
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    note                    VARCHAR(500),
    CHECK ((type IN ('DEPOSIT','TOPUP') AND amount > 0)
        OR (type IN ('DEDUCTION','REFUND') AND amount < 0)),
    CHECK ((type = 'DEDUCTION') = (maintenance_request_id IS NOT NULL)),
    CHECK ((type = 'DEDUCTION') = (due_date IS NOT NULL)),
    CHECK (type = 'DEDUCTION' OR confirmed_by IS NOT NULL)
);
CREATE UNIQUE INDEX ux_deposit_once  ON deposit_transaction(contract_id) WHERE type = 'DEPOSIT';
CREATE UNIQUE INDEX ux_refund_once   ON deposit_transaction(contract_id) WHERE type = 'REFUND';
-- một ticket chỉ trừ cọc một lần cho mỗi hợp đồng
CREATE UNIQUE INDEX ux_deduction_ticket ON deposit_transaction(contract_id, maintenance_request_id)
    WHERE type = 'DEDUCTION';

-- ---------------------------------------------------------------------
-- 6. Kỳ hè
-- ---------------------------------------------------------------------

CREATE TABLE summer_choice (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_id  BIGINT NOT NULL REFERENCES dormitory_contract(id),
    semester_id  BIGINT NOT NULL REFERENCES semester(id),    -- học kỳ hè
    choice       VARCHAR(15) NOT NULL CHECK (choice IN ('STAY','TEMP_LEAVE','LEAVE')),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    decided_by   BIGINT REFERENCES app_user(id),
    decided_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (contract_id, semester_id)
);

-- Phiếu gửi đồ khi tạm rời hè (FR-CON-11, FR-CON-12)
CREATE TABLE storage_record (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_id       BIGINT NOT NULL REFERENCES dormitory_contract(id),
    summer_choice_id  BIGINT NOT NULL UNIQUE REFERENCES summer_choice(id),
    previous_bed_id   BIGINT NOT NULL REFERENCES bed(id),     -- gợi ý phân lại giường cũ
    package_count     SMALLINT NOT NULL CHECK (package_count > 0),
    description       VARCHAR(500) NOT NULL,
    location          VARCHAR(100) NOT NULL,
    deposited_at      DATE NOT NULL,
    received_by       BIGINT NOT NULL REFERENCES app_user(id),
    returned_at       DATE,
    returned_by       BIGINT REFERENCES app_user(id),
    CHECK (returned_at IS NULL OR returned_at >= deposited_at),
    CHECK ((returned_at IS NULL) = (returned_by IS NULL))
);
-- "Tạm rời hè" = có phiếu chưa nhận lại; tối đa một phiếu mở mỗi hợp đồng
CREATE UNIQUE INDEX ux_storage_open ON storage_record(contract_id) WHERE returned_at IS NULL;

-- ---------------------------------------------------------------------
-- 7. Điện nước, hóa đơn
-- ---------------------------------------------------------------------

CREATE TABLE utility_rate (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meter_type      VARCHAR(10) NOT NULL CHECK (meter_type IN ('ELECTRIC','WATER')),
    unit_price      BIGINT NOT NULL CHECK (unit_price > 0),  -- đ/kWh hoặc đ/m3
    effective_from  DATE NOT NULL,
    created_by      BIGINT NOT NULL REFERENCES app_user(id),
    UNIQUE (meter_type, effective_from)
);

CREATE TABLE utility_reading (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id             BIGINT NOT NULL REFERENCES room(id),
    meter_type          VARCHAR(10) NOT NULL CHECK (meter_type IN ('ELECTRIC','WATER')),
    billing_month       DATE NOT NULL CHECK (EXTRACT(DAY FROM billing_month) = 1), -- ngày 1 của tháng
    start_reading       INT NOT NULL CHECK (start_reading >= 0),
    end_reading         INT NOT NULL,
    -- thay đồng hồ trong kỳ (FR-BIL-03): chỉ số chốt của đồng hồ cũ, chỉ số bắt đầu của đồng hồ mới
    old_meter_final     INT,
    new_meter_start     INT,
    replacement_reason  VARCHAR(300),
    consumption         INT GENERATED ALWAYS AS (
                          CASE WHEN old_meter_final IS NULL THEN end_reading - start_reading
                               ELSE (old_meter_final - start_reading) + (end_reading - new_meter_start) END
                        ) STORED,
    recorded_by         BIGINT NOT NULL REFERENCES app_user(id),
    recorded_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (room_id, meter_type, billing_month),               -- BR-03
    CHECK ((old_meter_final IS NULL) = (new_meter_start IS NULL)),
    CHECK ((old_meter_final IS NULL) = (replacement_reason IS NULL)),
    CHECK (CASE WHEN old_meter_final IS NULL THEN end_reading >= start_reading
                ELSE old_meter_final >= start_reading AND end_reading >= new_meter_start END)
);

CREATE TABLE invoice (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code             VARCHAR(20) NOT NULL UNIQUE,
    type             VARCHAR(10) NOT NULL CHECK (type IN ('ROOM_FEE','UTILITY')),
    -- phí phòng: theo (sinh viên, học kỳ)
    student_id       BIGINT REFERENCES student_profile(user_id),
    semester_id      BIGINT REFERENCES semester(id),
    -- điện nước: theo (phòng, tháng)
    room_id          BIGINT REFERENCES room(id),
    billing_month    DATE,
    total_amount     BIGINT NOT NULL CHECK (total_amount >= 0),  -- kể cả sau dòng điều chỉnh âm (FR-BIL-12)
    issued_at        TIMESTAMPTZ NOT NULL DEFAULT now(),   -- ngày thông báo
    due_date         DATE NOT NULL,
    status           VARCHAR(10) NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID','PAID')),
    payer_user_id    BIGINT REFERENCES app_user(id),         -- người nộp (thường là trưởng phòng)
    payer_name       VARCHAR(100),                           -- khi người nộp không có tài khoản
    payment_method   VARCHAR(10) CHECK (payment_method IN ('CASH','TRANSFER')),
    paid_at          TIMESTAMPTZ,
    confirmed_by     BIGINT REFERENCES app_user(id),
    issued_by        BIGINT NOT NULL REFERENCES app_user(id),
    CHECK ((type = 'ROOM_FEE' AND student_id IS NOT NULL AND semester_id IS NOT NULL
                              AND room_id IS NULL AND billing_month IS NULL)
        OR (type = 'UTILITY'  AND room_id IS NOT NULL AND billing_month IS NOT NULL
                              AND student_id IS NULL AND semester_id IS NULL)),
    CHECK (due_date >= issued_at::date),
    CHECK ((status = 'PAID') = (paid_at IS NOT NULL AND confirmed_by IS NOT NULL AND payment_method IS NOT NULL))
);
-- BR-04: không phát hành trùng
CREATE UNIQUE INDEX ux_invoice_room_fee ON invoice(student_id, semester_id) WHERE type = 'ROOM_FEE';
CREATE UNIQUE INDEX ux_invoice_utility  ON invoice(room_id, billing_month)  WHERE type = 'UTILITY';
CREATE INDEX ix_invoice_unpaid ON invoice(due_date) WHERE status = 'UNPAID';

CREATE TABLE invoice_item (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id           BIGINT NOT NULL REFERENCES invoice(id),
    item_type            VARCHAR(15) NOT NULL
                         CHECK (item_type IN ('ROOM_FEE','ELECTRIC','WATER','ADJUSTMENT')),
    quantity             NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit_price           BIGINT NOT NULL,       -- đơn giá chụp lúc phát hành
    amount               BIGINT NOT NULL,
    assignment_id        BIGINT REFERENCES room_assignment_history(id), -- nguồn đơn giá phí phòng
    utility_reading_id   BIGINT REFERENCES utility_reading(id),
    adjusts_invoice_id   BIGINT REFERENCES invoice(id),                 -- FR-BIL-12
    note                 VARCHAR(500),
    CHECK (item_type <> 'ADJUSTMENT' OR (note IS NOT NULL AND adjusts_invoice_id IS NOT NULL)),
    CHECK (item_type =  'ADJUSTMENT' OR amount >= 0),
    CHECK (item_type <> 'ROOM_FEE' OR assignment_id IS NOT NULL),
    CHECK (item_type NOT IN ('ELECTRIC','WATER') OR utility_reading_id IS NOT NULL)
);
CREATE INDEX ix_invoice_item_invoice ON invoice_item(invoice_id);

-- ---------------------------------------------------------------------
-- 8. Thông báo, lịch sử trạng thái, nhật ký
-- ---------------------------------------------------------------------

CREATE TABLE notification (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scope               VARCHAR(10) NOT NULL
                        CHECK (scope IN ('ALL','UNIT','BUILDING','FLOOR','ROOM','USER')),
    management_unit_id  BIGINT REFERENCES management_unit(id),
    building_id         BIGINT REFERENCES building(id),
    floor               SMALLINT,
    room_id             BIGINT REFERENCES room(id),
    user_id             BIGINT REFERENCES app_user(id),
    title               VARCHAR(200) NOT NULL,
    body                TEXT NOT NULL,
    source_type         VARCHAR(30),       -- APPLICATION, INVOICE, MAINTENANCE... (thông báo tự động)
    source_id           BIGINT,
    created_by          BIGINT REFERENCES app_user(id),  -- NULL: hệ thống
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (CASE scope
        WHEN 'ALL'      THEN num_nonnulls(management_unit_id, building_id, floor, room_id, user_id) = 0
        WHEN 'UNIT'     THEN management_unit_id IS NOT NULL AND num_nonnulls(building_id, floor, room_id, user_id) = 0
        WHEN 'BUILDING' THEN building_id IS NOT NULL AND num_nonnulls(management_unit_id, floor, room_id, user_id) = 0
        WHEN 'FLOOR'    THEN building_id IS NOT NULL AND floor IS NOT NULL AND num_nonnulls(management_unit_id, room_id, user_id) = 0
        WHEN 'ROOM'     THEN room_id IS NOT NULL AND num_nonnulls(management_unit_id, building_id, floor, user_id) = 0
        WHEN 'USER'     THEN user_id IS NOT NULL AND num_nonnulls(management_unit_id, building_id, floor, room_id) = 0
    END)
);
CREATE INDEX ix_notification_user ON notification(user_id, created_at DESC) WHERE scope = 'USER';
CREATE INDEX ix_notification_created ON notification(created_at DESC);

CREATE TABLE notification_read (
    notification_id  BIGINT NOT NULL REFERENCES notification(id),
    user_id          BIGINT NOT NULL REFERENCES app_user(id),
    read_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (notification_id, user_id)
);

-- Lịch sử chuyển trạng thái dùng chung cho đơn, hợp đồng, ticket, yêu cầu (BR-09).
-- Thay cho MaintenanceStatusHistory trong SRS; cấp dữ liệu cho timeline trên giao diện.
CREATE TABLE status_history (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type  VARCHAR(30) NOT NULL CHECK (entity_type IN
                 ('APPLICATION','CONTRACT','MAINTENANCE','EXTENSION','TRANSFER','SUMMER_CHOICE','INVOICE')),
    entity_id    BIGINT NOT NULL,
    from_status  VARCHAR(20),          -- NULL khi tạo mới
    to_status    VARCHAR(20) NOT NULL,
    actor_id     BIGINT REFERENCES app_user(id),  -- NULL: SYSTEM
    reason       VARCHAR(500),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_status_history_entity ON status_history(entity_type, entity_id, created_at);

-- Nhật ký thao tác quản trị và tác vụ tự động; chỉ ghi thêm (FR-ACC-09, NFR-REL-03)
CREATE TABLE audit_log (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id     BIGINT REFERENCES app_user(id),  -- NULL: SYSTEM
    action       VARCHAR(50) NOT NULL,            -- USER_LOCK, ROOM_LEADER_CHANGE, CONFIG_UPDATE...
    entity_type  VARCHAR(30) NOT NULL,
    entity_id    BIGINT,
    old_value    JSONB,
    new_value    JSONB,
    reason       VARCHAR(500),
    request_id   VARCHAR(50),                     -- NFR-MNT-03
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_actor  ON audit_log(actor_id, created_at DESC);
CREATE INDEX ix_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX ix_audit_action ON audit_log(action, created_at DESC);

CREATE FUNCTION fn_audit_log_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_log chỉ cho phép ghi thêm';
END $$;
CREATE TRIGGER tg_audit_log_append_only BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION fn_audit_log_append_only();

-- ---------------------------------------------------------------------
-- 8a. Sinh mã hiển thị (Q-DB-10)
--   HD-2026-0012 (hợp đồng), TK-1024 (ticket), HP-2026-0001 / DN-2026-0002 (hóa đơn
--   phí phòng / điện nước, chung một dãy số). Sequence không reset theo năm: số chạy
--   liên tục, năm trong mã chỉ để dễ đọc. Rollback có thể làm hụt số, chấp nhận được.
--   Service gọi: SELECT fn_next_code('HD', 'seq_contract_code', TRUE);
-- ---------------------------------------------------------------------

CREATE SEQUENCE seq_contract_code    START WITH 1;
CREATE SEQUENCE seq_maintenance_code START WITH 1000;
CREATE SEQUENCE seq_invoice_code     START WITH 1;

-- Đệm tối thiểu 4 chữ số và tự dài ra khi vượt 9999. Không dùng lpad(v, 4) vì lpad
-- cắt chuỗi dài hơn (12345 -> '1234'), cũng không dùng to_char(v, 'FM0000') vì tràn
-- thành '####'. Năm lấy theo giờ Việt Nam, không phụ thuộc TimeZone của phiên.
CREATE FUNCTION fn_next_code(p_prefix TEXT, p_seq REGCLASS, p_with_year BOOLEAN)
RETURNS VARCHAR LANGUAGE sql VOLATILE AS $$
    SELECT p_prefix || '-'
        || CASE WHEN p_with_year
                THEN to_char(now() AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY') || '-' ELSE '' END
        || lpad(v::text, GREATEST(4, length(v::text)), '0')
    FROM (SELECT nextval(p_seq) AS v) s
$$;

-- =====================================================================
-- 9. View hỗ trợ (SRS mục 5.2). Dữ liệu suy ra chỉ nằm ở đây (BR-08).
-- =====================================================================

-- Sinh viên đang ở / đang giữ ở đâu, thuộc đơn vị nào
CREATE VIEW v_current_stay AS
SELECT c.id            AS contract_id,
       c.student_id,
       c.status        AS contract_status,
       a.id            AS assignment_id,
       a.kind          AS assignment_kind,
       b.id            AS bed_id,
       b.code          AS bed_code,
       r.id            AS room_id,
       r.code          AS room_code,
       bd.id           AS building_id,
       bd.code         AS building_code,
       bd.management_unit_id,
       a.price_main,
       a.price_summer
FROM dormitory_contract c
JOIN room_assignment_history a ON a.contract_id = c.id AND a.ended_at IS NULL
JOIN bed      b  ON b.id  = a.bed_id
JOIN room     r  ON r.id  = b.room_id
JOIN building bd ON bd.id = r.building_id
WHERE c.status IN ('PENDING','ACTIVE');

-- Theo phòng: số giường hoạt động / đang giữ / đang ở / trống
CREATE VIEW v_room_occupancy AS
SELECT r.id AS room_id,
       r.building_id,
       bd.management_unit_id,
       r.code,
       r.gender,
       r.status,
       r.room_type_id,
       r.leader_student_id,
       COUNT(b.id) FILTER (WHERE b.status = 'ACTIVE')                       AS active_beds,
       COUNT(a.id) FILTER (WHERE a.kind = 'HOLD')                           AS held_beds,
       COUNT(a.id) FILTER (WHERE a.kind = 'STAY')                           AS occupied_beds,
       COUNT(b.id) FILTER (WHERE b.status = 'ACTIVE' AND a.id IS NULL)      AS free_beds
FROM room r
JOIN building bd ON bd.id = r.building_id
LEFT JOIN bed b ON b.room_id = r.id
LEFT JOIN room_assignment_history a ON a.bed_id = b.id AND a.ended_at IS NULL
GROUP BY r.id, bd.management_unit_id;

-- Theo hợp đồng: số dư cọc, số cần bù, khoản bù quá hạn.
-- Bù cọc được tính trả cho các lần trừ theo thứ tự thời gian (FIFO):
-- lần trừ thứ i đã được bù khi tổng trừ lũy kế đến i <= tổng đã bù.
CREATE VIEW v_deposit_balance AS
WITH ledger AS (
    SELECT contract_id,
           COALESCE(SUM(amount), 0)                                AS balance,
           COALESCE(SUM(amount) FILTER (WHERE type = 'TOPUP'), 0)  AS total_topup
    FROM deposit_transaction
    GROUP BY contract_id
),
deductions AS (
    SELECT d.contract_id, d.due_date,
           SUM(-d.amount) OVER (PARTITION BY d.contract_id ORDER BY d.created_at, d.id) AS cum_deducted
    FROM deposit_transaction d
    WHERE d.type = 'DEDUCTION'
),
uncovered AS (
    SELECT de.contract_id, MIN(de.due_date) AS oldest_unpaid_due
    FROM deductions de
    JOIN ledger l ON l.contract_id = de.contract_id
    WHERE de.cum_deducted > l.total_topup
    GROUP BY de.contract_id
)
SELECT c.id AS contract_id,
       c.student_id,
       c.status,
       c.deposit_required,
       COALESCE(l.balance, 0) AS balance,
       -- chỉ có nghĩa khi hợp đồng ACTIVE (SRS v0.4, BR-10)
       CASE WHEN c.status = 'ACTIVE'
            THEN GREATEST(c.deposit_required - COALESCE(l.balance, 0), 0) ELSE 0 END AS topup_needed,
       CASE WHEN c.status = 'ACTIVE' THEN u.oldest_unpaid_due END                 AS oldest_unpaid_due,
       COALESCE(c.status = 'ACTIVE' AND u.oldest_unpaid_due < CURRENT_DATE, FALSE) AS topup_overdue,
       -- công nợ cọc sau trả phòng: số dư âm
       CASE WHEN c.status IN ('EXPIRED','TERMINATED')
            THEN GREATEST(-COALESCE(l.balance, 0), 0) ELSE 0 END                  AS deposit_debt
FROM dormitory_contract c
LEFT JOIN ledger   l ON l.contract_id = c.id
LEFT JOIN uncovered u ON u.contract_id = c.id
WHERE c.status IN ('ACTIVE','EXPIRED','TERMINATED');

-- Sinh viên đang tạm rời hè
CREATE VIEW v_summer_leave AS
SELECT s.contract_id,
       c.student_id,
       s.id AS storage_record_id,
       s.package_count,
       s.location,
       s.deposited_at,
       s.previous_bed_id,
       NOT EXISTS (SELECT 1 FROM room_assignment_history a
                   WHERE a.bed_id = s.previous_bed_id AND a.ended_at IS NULL) AS previous_bed_free
FROM storage_record s
JOIN dormitory_contract c ON c.id = s.contract_id
WHERE s.returned_at IS NULL AND c.status = 'ACTIVE';

-- Hợp đồng ACTIVE kèm nhãn suy ra (FR-CON-03, FR-CON-04, FR-CON-11)
CREATE VIEW v_contract_label AS
SELECT c.id AS contract_id,
       c.student_id,
       c.end_date,
       (c.end_date - CURRENT_DATE) AS days_left,
       (c.end_date >= CURRENT_DATE AND c.end_date - CURRENT_DATE <=
            (SELECT value::int FROM system_config WHERE key = 'contract.expiring_days')) AS expiring_soon,
       (c.end_date < CURRENT_DATE) AS overdue_move_out,
       EXISTS (SELECT 1 FROM storage_record s
               WHERE s.contract_id = c.id AND s.returned_at IS NULL)                     AS summer_leave
FROM dormitory_contract c
WHERE c.status = 'ACTIVE';

-- Hộp thư thông báo của từng người dùng (FR-NOT-03, Q-DB-08). Truy vấn: WHERE user_id = ?
--   * ALL, USER: theo đúng phạm vi.
--   * Sinh viên: thông báo UNIT / BUILDING / FLOOR / ROOM được đăng trong lúc sinh viên
--     thật sự ở (STAY) tại nơi đó, khoảng [stay_started_at, ended_at). Giữ chỗ (HOLD)
--     chưa tính là ở. Sinh viên vẫn xem lại được thông báo của những lần ở trước.
--   * Staff, KTV: thêm UNIT / BUILDING / FLOOR thuộc đơn vị mình. Thông báo theo phòng
--     dành cho người ở, Staff xem ở màn quản lý thông báo (đọc thẳng bảng notification).
--   * Admin: chỉ ALL và USER.
CREATE VIEW v_user_notification AS
SELECT u.id AS user_id,
       n.id AS notification_id,
       n.scope,
       n.title,
       n.body,
       n.source_type,
       n.source_id,
       n.created_by,
       n.created_at,
       nr.read_at,
       (nr.read_at IS NOT NULL) AS is_read
FROM app_user u
JOIN notification n ON
       n.scope = 'ALL'
    OR (n.scope = 'USER' AND n.user_id = u.id)
    OR (u.role = 'STUDENT' AND n.scope IN ('UNIT','BUILDING','FLOOR','ROOM') AND EXISTS (
            SELECT 1
            FROM room_assignment_history a
            JOIN bed      b  ON b.id  = a.bed_id
            JOIN room     r  ON r.id  = b.room_id
            JOIN building bd ON bd.id = r.building_id
            WHERE a.student_id = u.id
              AND a.kind = 'STAY'
              AND n.created_at >= a.stay_started_at
              AND (a.ended_at IS NULL OR n.created_at < a.ended_at)
              AND CASE n.scope
                    WHEN 'ROOM'     THEN r.id = n.room_id
                    WHEN 'FLOOR'    THEN r.building_id = n.building_id AND r.floor = n.floor
                    WHEN 'BUILDING' THEN r.building_id = n.building_id
                    WHEN 'UNIT'     THEN bd.management_unit_id = n.management_unit_id
                  END))
    OR (u.role IN ('STAFF','TECHNICIAN') AND (
            (n.scope = 'UNIT' AND n.management_unit_id = u.management_unit_id)
         OR (n.scope IN ('BUILDING','FLOOR') AND EXISTS (
                SELECT 1 FROM building bd
                WHERE bd.id = n.building_id AND bd.management_unit_id = u.management_unit_id))))
LEFT JOIN notification_read nr ON nr.notification_id = n.id AND nr.user_id = u.id;

-- =====================================================================
-- 10. Dữ liệu khởi tạo
-- =====================================================================

INSERT INTO management_unit (code, name) VALUES
    ('K1',  'Khu K1'),
    ('N23', 'Khu N2 + N3');

INSERT INTO system_config (key, value, description) VALUES
    ('contract.activation_days',   '7',  'Số ngày để kích hoạt hợp đồng sau khi phân giường (FR-CON-02)'),
    ('contract.expiring_days',     '15', 'Ngưỡng nhãn Sắp hết hạn (FR-CON-03)'),
    ('maintenance.autoclose_days', '3',  'Số ngày tự đóng ticket RESOLVED (FR-MNT-06)'),
    ('invoice.due_days',           '7',  'Hạn đóng = ngày thông báo + N ngày (FR-BIL-07)'),
    ('deposit.topup_due_days',     '7',  'Hạn bù cọc sau khi bị trừ (FR-MNT-10)');

INSERT INTO priority_category (code, name, weight) VALUES
    ('POLICY',     'Diện chính sách (hộ nghèo, cận nghèo, con thương binh liệt sĩ)', 50),
    ('FRESHMAN',   'Sinh viên năm nhất', 30),
    ('FAR_AWAY',   'Ở xa (> 100 km)',    20);


-- Bổ sung v0.6: cùng DDL/dữ liệu mặc định với Flyway V5.
-- Ma trận quyền động theo vai trò; không lưu ADMIN vì ADMIN luôn có toàn quyền.
-- Chỉ bổ sung migration mới, không sửa checksum của các migration đã chạy.
CREATE TABLE role_permission_matrix (
    role VARCHAR(30) PRIMARY KEY,
    permissions JSONB NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_role_permission_matrix_role CHECK (role IN ('STAFF', 'STUDENT', 'TECHNICIAN')),
    CONSTRAINT ck_role_permission_matrix_array CHECK (jsonb_typeof(permissions) = 'array'),
    CONSTRAINT ck_role_permission_matrix_version CHECK (version >= 0)
);

-- Giữ nguyên quyền trước khi chuyển sang ma trận động: Staff 72, Student 35, Technician 11.
-- Đây là dữ liệu khởi tạo bất biến của migration; runtime đọc quyền đang lưu trong bảng.
INSERT INTO role_permission_matrix(role, permissions) VALUES
    ('STAFF', '[
    "BED_CREATE",
    "BED_DELETE",
    "BED_READ",
    "BED_UPDATE",
    "BUILDING_READ",
    "CONTRACT_CREATE",
    "CONTRACT_DELETE",
    "CONTRACT_EXTENSION_READ",
    "CONTRACT_EXTENSION_UPDATE",
    "CONTRACT_READ",
    "CONTRACT_UPDATE",
    "DASHBOARD_READ",
    "DEPOSIT_CREATE",
    "DEPOSIT_DELETE",
    "DEPOSIT_READ",
    "DEPOSIT_UPDATE",
    "INVOICE_CREATE",
    "INVOICE_DELETE",
    "INVOICE_READ",
    "INVOICE_UPDATE",
    "MAINTENANCE_CREATE",
    "MAINTENANCE_DELETE",
    "MAINTENANCE_READ",
    "MAINTENANCE_UPDATE",
    "MANAGEMENT_UNIT_READ",
    "MOVE_OUT_CREATE",
    "MOVE_OUT_DELETE",
    "MOVE_OUT_READ",
    "MOVE_OUT_UPDATE",
    "NOTIFICATION_CREATE",
    "NOTIFICATION_DELETE",
    "NOTIFICATION_READ",
    "NOTIFICATION_UPDATE",
    "PRIORITY_CATEGORY_READ",
    "PROFILE_READ",
    "PROFILE_UPDATE",
    "REGISTRATION_PERIOD_CREATE",
    "REGISTRATION_PERIOD_DELETE",
    "REGISTRATION_PERIOD_READ",
    "REGISTRATION_PERIOD_UPDATE",
    "ROOM_APPLICATION_READ",
    "ROOM_APPLICATION_UPDATE",
    "ROOM_CREATE",
    "ROOM_DELETE",
    "ROOM_READ",
    "ROOM_TRANSFER_READ",
    "ROOM_TRANSFER_UPDATE",
    "ROOM_TYPE_CREATE",
    "ROOM_TYPE_DELETE",
    "ROOM_TYPE_READ",
    "ROOM_TYPE_UPDATE",
    "ROOM_UPDATE",
    "SEMESTER_CREATE",
    "SEMESTER_DELETE",
    "SEMESTER_READ",
    "SEMESTER_UPDATE",
    "STORAGE_CREATE",
    "STORAGE_DELETE",
    "STORAGE_READ",
    "STORAGE_UPDATE",
    "STUDENT_READ",
    "STUDENT_RESET_PASSWORD",
    "STUDENT_UPDATE",
    "SUMMER_REGISTRATION_READ",
    "UTILITY_RATE_CREATE",
    "UTILITY_RATE_DELETE",
    "UTILITY_RATE_READ",
    "UTILITY_RATE_UPDATE",
    "UTILITY_READING_CREATE",
    "UTILITY_READING_DELETE",
    "UTILITY_READING_READ",
    "UTILITY_READING_UPDATE"
]'::jsonb),
    ('STUDENT', '[
    "BED_READ",
    "BUILDING_READ",
    "CONTRACT_EXTENSION_CREATE",
    "CONTRACT_EXTENSION_DELETE",
    "CONTRACT_EXTENSION_READ",
    "CONTRACT_EXTENSION_UPDATE",
    "CONTRACT_READ",
    "DEPOSIT_READ",
    "INVOICE_READ",
    "MAINTENANCE_CREATE",
    "MAINTENANCE_DELETE",
    "MAINTENANCE_READ",
    "MAINTENANCE_UPDATE",
    "MANAGEMENT_UNIT_READ",
    "NOTIFICATION_READ",
    "PRIORITY_CATEGORY_READ",
    "PROFILE_READ",
    "PROFILE_UPDATE",
    "REGISTRATION_PERIOD_READ",
    "ROOM_APPLICATION_CREATE",
    "ROOM_APPLICATION_DELETE",
    "ROOM_APPLICATION_READ",
    "ROOM_APPLICATION_UPDATE",
    "ROOM_READ",
    "ROOM_TRANSFER_CREATE",
    "ROOM_TRANSFER_DELETE",
    "ROOM_TRANSFER_READ",
    "ROOM_TRANSFER_UPDATE",
    "ROOM_TYPE_READ",
    "SEMESTER_READ",
    "STORAGE_READ",
    "SUMMER_REGISTRATION_CREATE",
    "SUMMER_REGISTRATION_DELETE",
    "SUMMER_REGISTRATION_READ",
    "SUMMER_REGISTRATION_UPDATE"
]'::jsonb),
    ('TECHNICIAN', '[
    "BUILDING_READ",
    "MAINTENANCE_READ",
    "MAINTENANCE_UPDATE",
    "MANAGEMENT_UNIT_READ",
    "NOTIFICATION_READ",
    "PRIORITY_CATEGORY_READ",
    "PROFILE_READ",
    "PROFILE_UPDATE",
    "REGISTRATION_PERIOD_READ",
    "ROOM_TYPE_READ",
    "SEMESTER_READ"
]'::jsonb);

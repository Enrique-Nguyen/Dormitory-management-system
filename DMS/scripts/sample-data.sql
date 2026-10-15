-- DMS: dữ liệu mẫu cho toàn bộ khung CRUD, PostgreSQL 16+, schema đã chạy Flyway V1..V5.
-- Chạy trong database phát triển bằng psql/DataGrip/pgAdmin; chạy TOÀN BỘ file.
-- psql -h localhost -p 5432 -U dms -d dms -v ON_ERROR_STOP=1 -f sample-data.sql
-- Không sửa dữ liệu đang có, không TRUNCATE/DELETE, không vô hiệu hóa constraint/trigger.
-- Mã MAU- / username mau.* là dữ liệu giả; không chứa mật khẩu hoặc Keycloak subject giả.
-- Tài khoản mẫu bị khóa, KHÔNG dùng đăng nhập. Tạo tài khoản đăng nhập qua luồng Keycloak của ứng dụng.
-- Script chạy lại sẽ bỏ qua nếu đã có audit marker DMS-SAMPLE-V1, không khôi phục thay đổi CRUD.

BEGIN;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(hashtextextended('dms.sample-data.v1', 0));

DO $seed$
DECLARE
    unit_ids        BIGINT[];
    staff_ids       BIGINT[] := '{}';
    tech_ids        BIGINT[] := '{}';
    student_ids     BIGINT[] := '{}';
    building_ids    BIGINT[] := '{}';
    room_ids        BIGINT[] := '{}';
    semester_ids    BIGINT[] := '{}';
    application_ids BIGINT[] := '{}';
    contract_ids    BIGINT[] := '{}';
    assignment_ids  BIGINT[] := '{}';
    ticket_ids      BIGINT[] := '{}';
    names TEXT[] := ARRAY[
        'Mẫu - Nguyễn Văn An', 'Mẫu - Trần Thị Bình', 'Mẫu - Phạm Minh Châu',
        'Mẫu - Lê Thu Duyên', 'Mẫu - Đặng Quốc Em', 'Mẫu - Hoàng Ngọc Hà',
        'Mẫu - Đỗ Gia Huy', 'Mẫu - Vũ Khánh Linh', 'Mẫu - Lê Tiến Minh',
        'Mẫu - Trần Ngọc Mai', 'Mẫu - Bùi Quốc Nam', 'Mẫu - Đào Thu Phương'
    ];
    academic_start  INT := EXTRACT(YEAR FROM CURRENT_DATE)::INT
                         - CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) < 9 THEN 1 ELSE 0 END;
    year_label      TEXT;
    semester_type   TEXT;
    start_day       DATE;
    end_day         DATE;
    active_start    DATE;
    active_end      DATE;
    active_semester BIGINT;
    regular_period  BIGINT;
    summer_period   BIGINT;
    previous_period BIGINT;
    room_type_four  BIGINT;
    room_type_six   BIGINT;
    category_id     BIGINT;
    category_weight INT;
    actor_id        BIGINT;
    row_id          BIGINT;
    room_id_value   BIGINT;
    bed_id_value    BIGINT;
    summer_id       BIGINT;
    reading_id      BIGINT;
    electric_id     BIGINT;
    water_id        BIGINT;
    invoice_id      BIGINT;
    electric_price  BIGINT;
    water_price     BIGINT;
    deposit_value   BIGINT;
    fee_value       BIGINT;
    month_start     DATE := date_trunc('month', CURRENT_DATE)::DATE;
    state_value     TEXT;
    invoice_state   TEXT;
    contract_start  DATE;
    contract_end    DATE;
    i INT;
    j INT;
    k INT;
    unit_index INT;
    room_index INT;
BEGIN
    IF to_regclass('public.role_permission_matrix') IS NULL THEN
        RAISE EXCEPTION 'Hãy khởi động backend để Flyway chạy đến V5 trước khi nạp dữ liệu mẫu.';
    END IF;
    IF EXISTS (SELECT 1 FROM audit_log WHERE request_id = 'DMS-SAMPLE-V1') THEN
        RAISE NOTICE 'DMS-SAMPLE-V1 đã được nạp. Giữ nguyên dữ liệu hiện tại, không nạp trùng.';
        RETURN;
    END IF;
    IF EXISTS (SELECT 1 FROM app_user WHERE username LIKE 'mau.%')
       OR EXISTS (SELECT 1 FROM building WHERE code LIKE 'MAU-%')
       OR EXISTS (SELECT 1 FROM dormitory_contract WHERE code LIKE 'MAU-%') THEN
        RAISE EXCEPTION 'Mã mau.*/MAU-* đã được dùng nhưng thiếu marker; kiểm tra dữ liệu trước khi nạp.';
    END IF;

    -- 1. Danh mục: tận dụng mã đơn vị có sẵn, không thay tên/giá/cấu hình của thành viên khác.
    INSERT INTO management_unit(code, name) VALUES ('K1', 'Khu K1'), ('N23', 'Khu N2 + N3')
        ON CONFLICT (code) DO NOTHING;
    unit_ids := ARRAY[(SELECT id FROM management_unit WHERE code = 'K1'),
                      (SELECT id FROM management_unit WHERE code = 'N23')];
    INSERT INTO priority_category(code, name, weight)
        VALUES ('POLICY', 'Diện chính sách', 50), ('FRESHMAN', 'Sinh viên năm nhất', 30),
               ('FAR_AWAY', 'Ở xa', 20) ON CONFLICT (code) DO NOTHING;
    SELECT id, weight INTO category_id, category_weight FROM priority_category WHERE code = 'FAR_AWAY';
    INSERT INTO system_config(key, value, description) VALUES
        ('contract.activation_days', '7', 'Số ngày kích hoạt hợp đồng'),
        ('contract.expiring_days', '15', 'Ngưỡng sắp hết hạn hợp đồng'),
        ('maintenance.autoclose_days', '3', 'Số ngày tự đóng yêu cầu đã xử lý'),
        ('invoice.due_days', '7', 'Hạn thanh toán hóa đơn'),
        ('deposit.topup_due_days', '7', 'Hạn bù tiền cọc') ON CONFLICT (key) DO NOTHING;

    -- Tài khoản chỉ phục vụ quan hệ FK và danh sách CRUD; không giả mạo đăng nhập Keycloak.
    FOR i IN 1..2 LOOP
        INSERT INTO app_user(username, role, full_name, management_unit_id, locked,
                             must_change_password, cross_unit_access)
        VALUES ('mau.ql.' || i, 'STAFF', 'Mẫu - Cán bộ quản lý khu ' || i, unit_ids[i],
                TRUE, FALSE, i = 1) RETURNING id INTO row_id;
        staff_ids := array_append(staff_ids, row_id);
        INSERT INTO app_user(username, role, full_name, management_unit_id, locked, must_change_password)
        VALUES ('mau.kt.' || i, 'TECHNICIAN', 'Mẫu - Kỹ thuật viên khu ' || i,
                unit_ids[i], TRUE, FALSE) RETURNING id INTO row_id;
        tech_ids := array_append(tech_ids, row_id);
    END LOOP;
    actor_id := staff_ids[1];
    FOR i IN 1..12 LOOP
        INSERT INTO app_user(username, role, full_name, locked, must_change_password)
        VALUES ('mau.sv.' || lpad(i::TEXT, 2, '0'), 'STUDENT', names[i], TRUE, FALSE)
        RETURNING id INTO row_id;
        student_ids := array_append(student_ids, row_id);
        INSERT INTO student_profile(user_id, student_code, gender, date_of_birth, faculty,
                                    cohort, hometown, emergency_contact)
        VALUES (row_id, 'MAU-SV-' || lpad(i::TEXT, 2, '0'), CASE WHEN i % 2 = 1 THEN 'M' ELSE 'F' END,
                make_date(2005, 1, 1) + (i * 15),
                CASE WHEN i % 3 = 0 THEN 'Kinh tế và Quản lý' ELSE 'Công nghệ thông tin' END,
                'K65', 'Địa chỉ mẫu để kiểm thử', 'Liên hệ mẫu - không phải dữ liệu cá nhân thật');
    END LOOP;

    INSERT INTO room_type(name, bed_count, price_main, price_summer, area_m2, amenities)
    VALUES ('Mẫu - Phòng 4 người', 4, 2500000, 900000, 28, 'Giường, tủ, bàn học, quạt')
    RETURNING id INTO room_type_four;
    INSERT INTO room_type(name, bed_count, price_main, price_summer, area_m2, amenities)
    VALUES ('Mẫu - Phòng 6 người', 6, 1800000, 650000, 36, 'Giường tầng, tủ, bàn học')
    RETURNING id INTO room_type_six;
    FOR i IN 1..3 LOOP
        unit_index := CASE WHEN i = 1 THEN 1 ELSE 2 END;
        INSERT INTO building(management_unit_id, code, name, deposit_amount)
        VALUES (unit_ids[unit_index], 'MAU-' || i, 'Mẫu - Tòa nhà ' || i,
                CASE WHEN i = 1 THEN 1000000 ELSE 2000000 END) RETURNING id INTO row_id;
        building_ids := array_append(building_ids, row_id);
        FOR j IN 1..3 LOOP
            INSERT INTO room(building_id, room_type_id, floor, code, gender, status)
            VALUES (building_ids[i], CASE WHEN j = 3 THEN room_type_six ELSE room_type_four END,
                    1, 'M10' || j, CASE WHEN j = 2 THEN 'F' ELSE 'M' END,
                    CASE WHEN i = 2 AND j = 3 THEN 'MAINTENANCE'
                         WHEN i = 3 AND j = 3 THEN 'CLOSED' ELSE 'ACTIVE' END)
            RETURNING id INTO row_id;
            room_ids := array_append(room_ids, row_id);
            FOR k IN 1..CASE WHEN j = 3 THEN 6 ELSE 4 END LOOP
                INSERT INTO bed(room_id, code, status)
                VALUES (row_id, lpad(k::TEXT, 2, '0'),
                        CASE WHEN j = 3 AND k = 6 THEN 'BROKEN' ELSE 'ACTIVE' END);
            END LOOP;
        END LOOP;
    END LOOP;

    -- 2. Học kỳ: dùng khóa tự nhiên (năm học, loại); không chèn kỳ chồng khoảng đã tồn tại.
    -- Nếu nhóm đã có lịch học kỳ, dùng lại nguyên ngày của lịch đó.
    FOR i IN 1..4 LOOP
        semester_type := CASE WHEN i = 1 THEN 'HK1' WHEN i = 2 THEN 'HK2' ELSE 'SUMMER' END;
        year_label := CASE WHEN i = 4 THEN (academic_start - 1) || '-' || academic_start
                           ELSE academic_start || '-' || (academic_start + 1) END;
        start_day := CASE i WHEN 1 THEN make_date(academic_start, 9, 1)
                           WHEN 2 THEN make_date(academic_start + 1, 2, 1)
                           WHEN 3 THEN make_date(academic_start + 1, 7, 1)
                           ELSE make_date(academic_start, 7, 1) END;
        end_day := CASE i WHEN 1 THEN make_date(academic_start + 1, 1, 31)
                         WHEN 2 THEN make_date(academic_start + 1, 6, 30)
                         WHEN 3 THEN make_date(academic_start + 1, 8, 31)
                         ELSE make_date(academic_start, 8, 31) END;
        SELECT id INTO row_id FROM semester WHERE academic_year = year_label AND type = semester_type;
        IF row_id IS NULL THEN
            IF EXISTS (SELECT 1 FROM semester WHERE daterange(start_date, end_date, '[]')
                        && daterange(start_day, end_day, '[]')) THEN
                RAISE EXCEPTION 'Lịch % % bị chồng với học kỳ hiện có. Kiểm tra lịch trước khi nạp.',
                    year_label, semester_type;
            END IF;
            INSERT INTO semester(academic_year, type, name, start_date, end_date)
            VALUES (year_label, semester_type, 'Mẫu - ' || semester_type || ' ' || year_label, start_day, end_day)
            RETURNING id INTO row_id;
        END IF;
        semester_ids := array_append(semester_ids, row_id);
    END LOOP;
    SELECT id, start_date, end_date INTO active_semester, active_start, active_end
        FROM semester WHERE CURRENT_DATE BETWEEN start_date AND end_date LIMIT 1;
    IF active_semester IS NULL THEN
        RAISE EXCEPTION 'Lịch học kỳ hiện có không bao phủ ngày hiện tại; chưa nạp dữ liệu mẫu.';
    END IF;
    INSERT INTO registration_period(semester_id, kind, name, open_at, close_at, created_by)
    VALUES (active_semester, 'REGULAR', 'Mẫu - Đăng ký nội trú', CURRENT_DATE - INTERVAL '60 days',
            CURRENT_DATE + INTERVAL '30 days', actor_id) RETURNING id INTO regular_period;
    INSERT INTO registration_period(semester_id, kind, name, open_at, close_at, created_by)
    VALUES (semester_ids[3], 'SUMMER', 'Mẫu - Đăng ký phương án hè', CURRENT_DATE - INTERVAL '1 day',
            CURRENT_DATE + INTERVAL '20 days', actor_id) RETURNING id INTO summer_period;
    INSERT INTO registration_period(semester_id, kind, name, open_at, close_at, created_by)
    VALUES (semester_ids[4], 'SUMMER', 'Mẫu - Đợt hè đã kết thúc', make_date(academic_start, 5, 1),
            make_date(academic_start, 6, 20), actor_id) RETURNING id INTO previous_period;
    INSERT INTO registration_period_unit(period_id, management_unit_id)
        SELECT p, u FROM unnest(ARRAY[regular_period, summer_period, previous_period]) p
        CROSS JOIN unnest(unit_ids) u;
    INSERT INTO registration_quota(period_id, building_id, quota)
        SELECT p, b, 14 FROM unnest(ARRAY[regular_period, summer_period]) p
        CROSS JOIN unnest(building_ids) b;

    -- 3. Đơn và hợp đồng: 6 đang ở, 1 giữ chỗ, 1 tạm rời hè, 1 đã trả phòng, 3 đơn chờ/nháp.
    FOR i IN 1..12 LOOP
        unit_index := CASE WHEN i IN (1, 2, 7, 8, 10) THEN 1 ELSE 2 END;
        state_value := CASE WHEN i <= 9 THEN 'ASSIGNED' WHEN i = 10 THEN 'SUBMITTED'
                            WHEN i = 11 THEN 'APPROVED' ELSE 'DRAFT' END;
        INSERT INTO room_application(student_id, period_id, preferred_unit_id,
            preferred_room_type_id, note, priority_score, status, submitted_at)
        VALUES (student_ids[i], regular_period, unit_ids[unit_index], room_type_four,
                'Dữ liệu mẫu - hồ sơ nội trú ' || i,
                CASE WHEN i % 2 = 0 AND i <= 9 THEN category_weight ELSE 0 END,
                state_value, CASE WHEN i < 12 THEN CURRENT_TIMESTAMP - INTERVAL '25 days' END)
        RETURNING id INTO row_id;
        application_ids := array_append(application_ids, row_id);
        IF i % 2 = 0 THEN
            INSERT INTO application_priority(application_id, category_id, verified, verified_by, verified_at)
            VALUES (row_id, category_id, i <= 9, CASE WHEN i <= 9 THEN staff_ids[unit_index] END,
                    CASE WHEN i <= 9 THEN CURRENT_TIMESTAMP - INTERVAL '24 days' END);
        END IF;
        IF i > 9 THEN CONTINUE; END IF;
        room_index := CASE WHEN i <= 6 THEN ((i - 1) / 2) * 3 + CASE WHEN i % 2 = 1 THEN 1 ELSE 2 END
                           WHEN i = 7 THEN 1 WHEN i = 8 THEN 2 ELSE 4 END;
        room_id_value := room_ids[room_index];
        SELECT b.deposit_amount, rt.price_main INTO deposit_value, fee_value
            FROM room r JOIN building b ON b.id = r.building_id
            JOIN room_type rt ON rt.id = r.room_type_id WHERE r.id = room_id_value;
        SELECT id INTO bed_id_value FROM bed WHERE room_id = room_id_value
            AND code = CASE WHEN i <= 6 THEN '01' WHEN i = 7 THEN '02' ELSE '03' END;
        state_value := CASE WHEN i = 7 THEN 'PENDING' WHEN i = 9 THEN 'TERMINATED' ELSE 'ACTIVE' END;
        contract_start := CASE WHEN i = 8 THEN make_date(academic_start, 6, 1)
                               WHEN i = 9 THEN CURRENT_DATE - 60
                               ELSE LEAST(GREATEST(active_start, CURRENT_DATE - 21), active_end - 1) END;
        contract_end := CASE WHEN i = 9 THEN CURRENT_DATE - 5
                             WHEN i = 3 THEN active_end + 30 ELSE active_end END;
        INSERT INTO dormitory_contract(code, student_id, application_id, start_date, end_date, status,
            activation_deadline, deposit_required, activated_at, activated_by, closed_at, status_reason)
        VALUES ('MAU-HD-' || lpad(i::TEXT, 2, '0'), student_ids[i], application_ids[i],
            contract_start, contract_end, state_value,
            CASE WHEN i = 7 THEN CURRENT_TIMESTAMP + INTERVAL '7 days' ELSE contract_start + INTERVAL '7 days' END,
            CASE WHEN i <> 7 THEN deposit_value END, CASE WHEN i <> 7 THEN contract_start::TIMESTAMPTZ END,
            CASE WHEN i <> 7 THEN staff_ids[unit_index] END,
            CASE WHEN i = 9 THEN (CURRENT_DATE - 5)::TIMESTAMPTZ END,
            CASE WHEN i = 9 THEN 'Mẫu - Đã hoàn tất trả phòng' END)
        RETURNING id INTO row_id;
        contract_ids := array_append(contract_ids, row_id);
        INSERT INTO room_assignment_history(student_id, contract_id, bed_id, kind, started_at,
            stay_started_at, ended_at, end_reason, price_main, price_summer, note, created_by)
        VALUES (student_ids[i], row_id, bed_id_value, CASE WHEN i = 7 THEN 'HOLD' ELSE 'STAY' END,
            CASE WHEN i = 7 THEN CURRENT_TIMESTAMP ELSE contract_start::TIMESTAMPTZ END,
            CASE WHEN i <> 7 THEN contract_start::TIMESTAMPTZ END,
            CASE WHEN i = 8 THEN make_date(academic_start, 7, 1)::TIMESTAMPTZ
                 WHEN i = 9 THEN (CURRENT_DATE - 5)::TIMESTAMPTZ END,
            CASE WHEN i = 8 THEN 'SUMMER_LEAVE' WHEN i = 9 THEN 'MOVE_OUT' END,
            fee_value, 900000, 'Dữ liệu mẫu - phân giường', staff_ids[unit_index])
        RETURNING id INTO reading_id;
        assignment_ids := array_append(assignment_ids, reading_id);
        IF i <= 6 THEN
            UPDATE room SET leader_student_id = student_ids[i] WHERE id = room_id_value;
        END IF;
        IF i <> 7 THEN
            INSERT INTO deposit_transaction(contract_id, type, amount, confirmed_by, note)
            VALUES (row_id, 'DEPOSIT', deposit_value, staff_ids[unit_index], 'Mẫu - Thu cọc khi nhận phòng');
        END IF;
    END LOOP;

    FOR i IN 1..3 LOOP
        INSERT INTO contract_extension_request(contract_id, source, old_end_date, new_end_date,
            reason, status, decided_by, decided_at, decision_reason)
        VALUES (contract_ids[i], 'STUDENT', active_end, active_end + 30,
            'Mẫu - Đề nghị tiếp tục ở để hoàn thành học phần',
            CASE i WHEN 1 THEN 'PENDING' WHEN 2 THEN 'REJECTED' ELSE 'APPROVED' END,
            CASE WHEN i > 1 THEN actor_id END, CASE WHEN i > 1 THEN CURRENT_TIMESTAMP END,
            CASE WHEN i = 2 THEN 'Mẫu - Chưa đủ điều kiện gia hạn' END);
    END LOOP;
    INSERT INTO room_transfer_request(contract_id, reason, status) VALUES
        (contract_ids[2], 'Mẫu - Đề nghị chuyển sang phòng còn chỗ', 'PENDING');
    INSERT INTO room_transfer_request(contract_id, reason, status, decided_by, decided_at, decision_reason)
        VALUES (contract_ids[4], 'Mẫu - Đề nghị chuyển khu', 'REJECTED', staff_ids[2],
                CURRENT_TIMESTAMP, 'Mẫu - Chưa có giường phù hợp');
    INSERT INTO summer_choice(contract_id, semester_id, choice, status) VALUES
        (contract_ids[1], semester_ids[3], 'STAY', 'PENDING'),
        (contract_ids[2], semester_ids[3], 'LEAVE', 'PENDING');
    INSERT INTO summer_choice(contract_id, semester_id, choice, status, decided_by, decided_at)
        VALUES (contract_ids[8], semester_ids[4], 'TEMP_LEAVE', 'APPROVED', actor_id,
                make_date(academic_start, 7, 1)) RETURNING id INTO summer_id;
    SELECT bed_id INTO bed_id_value FROM room_assignment_history WHERE id = assignment_ids[8];
    INSERT INTO storage_record(contract_id, summer_choice_id, previous_bed_id, package_count,
        description, location, deposited_at, received_by)
    VALUES (contract_ids[8], summer_id, bed_id_value, 2, 'Mẫu - Hai thùng sách và đồ cá nhân',
            'Kho mẫu K1 - Kệ A1', make_date(academic_start, 7, 1), actor_id);

    SELECT deposit_required INTO deposit_value FROM dormitory_contract WHERE id = contract_ids[9];
    INSERT INTO move_out_record(contract_id, moved_out_at, asset_condition, unpaid_room_fee,
        deposit_balance, refund_amount, confirmed_by)
    VALUES (contract_ids[9], (CURRENT_DATE - 5)::TIMESTAMPTZ, 'OK', 0, deposit_value, deposit_value, staff_ids[2]);
    INSERT INTO deposit_transaction(contract_id, type, amount, confirmed_by, note)
    VALUES (contract_ids[9], 'REFUND', -deposit_value, staff_ids[2], 'Mẫu - Hoàn cọc khi trả phòng');

    -- 4. Sửa chữa: vị trí phòng/khu chung, phân công kỹ thuật và khấu trừ cọc có chứng từ.
    INSERT INTO maintenance_request(code, requester_id, room_id, category, severity, description, status)
    VALUES ('MAU-TK-01', student_ids[1], room_ids[1], 'ELECTRIC', 'MEDIUM',
            'Mẫu - Đèn bàn học không sáng, đề nghị kiểm tra.', 'OPEN') RETURNING id INTO row_id;
    ticket_ids := array_append(ticket_ids, row_id);
    INSERT INTO maintenance_request(code, requester_id, building_id, floor, category, severity,
        description, status, assigned_to)
    VALUES ('MAU-TK-02', staff_ids[2], building_ids[2], 1, 'WATER', 'HIGH',
            'Mẫu - Vòi nước khu vực chung bị rò.', 'ASSIGNED', tech_ids[2]) RETURNING id INTO row_id;
    ticket_ids := array_append(ticket_ids, row_id);
    INSERT INTO maintenance_request(code, requester_id, room_id, category, severity, description,
        status, assigned_to, resolved_at, closed_at, cause, repair_cost, cost_note, cost_decided_by, cost_decided_at)
    VALUES ('MAU-TK-03', student_ids[1], room_ids[1], 'FURNITURE', 'LOW',
        'Mẫu - Đã thay khóa tủ hỏng do sử dụng.', 'CLOSED', tech_ids[1], CURRENT_TIMESTAMP - INTERVAL '2 days',
        CURRENT_TIMESTAMP - INTERVAL '1 day', 'MISUSE', 200000,
        'Mẫu - Chi phí vật tư có xác nhận', actor_id, CURRENT_TIMESTAMP - INTERVAL '1 day')
    RETURNING id INTO row_id;
    ticket_ids := array_append(ticket_ids, row_id);
    INSERT INTO deposit_transaction(contract_id, type, amount, maintenance_request_id, due_date, note)
        VALUES (contract_ids[1], 'DEDUCTION', -200000, row_id, CURRENT_DATE + 7, 'Mẫu - Trừ cọc sửa khóa tủ');
    INSERT INTO deposit_transaction(contract_id, type, amount, confirmed_by, note)
        VALUES (contract_ids[1], 'TOPUP', 200000, actor_id, 'Mẫu - Đã bù đủ tiền cọc');

    -- 5. Điện nước và hóa đơn: dòng chi tiết khớp tổng tiền, đơn giá/chỉ số không giả số dư suy ra.
    INSERT INTO utility_rate(meter_type, unit_price, effective_from, created_by) VALUES
        ('ELECTRIC', 3500, month_start, actor_id), ('WATER', 15000, month_start, actor_id)
        ON CONFLICT (meter_type, effective_from) DO NOTHING;
    SELECT unit_price INTO electric_price FROM utility_rate WHERE meter_type = 'ELECTRIC'
        AND effective_from <= month_start ORDER BY effective_from DESC LIMIT 1;
    SELECT unit_price INTO water_price FROM utility_rate WHERE meter_type = 'WATER'
        AND effective_from <= month_start ORDER BY effective_from DESC LIMIT 1;
    FOR i IN 1..9 LOOP
        IF i = 7 THEN CONTINUE; END IF;
        SELECT price_main INTO fee_value FROM room_assignment_history WHERE id = assignment_ids[i];
        invoice_state := CASE WHEN i IN (1, 2, 9) THEN 'PAID' ELSE 'UNPAID' END;
        INSERT INTO invoice(code, type, student_id, semester_id, total_amount, issued_at, due_date,
            status, payer_user_id, payment_method, paid_at, confirmed_by, issued_by)
        VALUES ('MAU-HP-' || lpad(i::TEXT, 2, '0'), 'ROOM_FEE', student_ids[i], active_semester,
            fee_value, CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + 7, invoice_state,
            CASE WHEN invoice_state = 'PAID' THEN student_ids[i] END,
            CASE WHEN invoice_state = 'PAID' THEN 'TRANSFER' END,
            CASE WHEN invoice_state = 'PAID' THEN CURRENT_TIMESTAMP - INTERVAL '6 days' END,
            CASE WHEN invoice_state = 'PAID' THEN actor_id END, actor_id) RETURNING id INTO invoice_id;
        INSERT INTO invoice_item(invoice_id, item_type, quantity, unit_price, amount, assignment_id, note)
        VALUES (invoice_id, 'ROOM_FEE', 1, fee_value, fee_value, assignment_ids[i], 'Mẫu - Phí phòng theo học kỳ');
    END LOOP;
    FOR i IN 1..6 LOOP
        room_index := ((i - 1) / 2) * 3 + CASE WHEN i % 2 = 1 THEN 1 ELSE 2 END;
        room_id_value := room_ids[room_index];
        INSERT INTO utility_reading(room_id, meter_type, billing_month, start_reading, end_reading, recorded_by)
        VALUES (room_id_value, 'ELECTRIC', month_start, 1000, 1080 + i * 5, actor_id)
        RETURNING id INTO electric_id;
        INSERT INTO utility_reading(room_id, meter_type, billing_month, start_reading, end_reading, recorded_by)
        VALUES (room_id_value, 'WATER', month_start, 100, 108 + i, actor_id) RETURNING id INTO water_id;
        INSERT INTO invoice(code, type, room_id, billing_month, total_amount, issued_at, due_date, issued_by)
        VALUES ('MAU-DN-' || lpad(i::TEXT, 2, '0'), 'UTILITY', room_id_value, month_start,
            (80 + i * 5) * electric_price + (8 + i) * water_price,
            CURRENT_DATE - INTERVAL '1 day', CURRENT_DATE + 7, actor_id) RETURNING id INTO invoice_id;
        INSERT INTO invoice_item(invoice_id, item_type, quantity, unit_price, amount, utility_reading_id, note)
        VALUES (invoice_id, 'ELECTRIC', 80 + i * 5, electric_price, (80 + i * 5) * electric_price,
                electric_id, 'Mẫu - Tiền điện'),
               (invoice_id, 'WATER', 8 + i, water_price, (8 + i) * water_price, water_id, 'Mẫu - Tiền nước');
    END LOOP;

    -- 6. Thông báo không gửi ra dịch vụ bên ngoài. Chỉ lưu dữ liệu hiển thị trong DMS local.
    INSERT INTO notification(scope, title, body, created_by)
    VALUES ('ALL', '[Mẫu] Hướng dẫn sử dụng ký túc xá',
            'Dữ liệu mẫu để kiểm thử màn thông báo, tìm kiếm và phân trang.', actor_id);
    INSERT INTO notification(scope, management_unit_id, title, body, created_by)
    VALUES ('UNIT', unit_ids[1], '[Mẫu] Lịch vệ sinh khu K1',
            'Dữ liệu minh họa phạm vi đơn vị, không phải lịch vận hành thật.', actor_id);
    INSERT INTO notification(scope, room_id, title, body, created_by)
    VALUES ('ROOM', room_ids[1], '[Mẫu] Kiểm tra thiết bị phòng M101',
            'Mẫu thông báo theo phòng.', actor_id);
    INSERT INTO notification(scope, user_id, title, body, created_by)
    VALUES ('USER', student_ids[1], '[Mẫu] Hồ sơ đã được tiếp nhận',
            'Mẫu thông báo riêng của sinh viên.', actor_id) RETURNING id INTO row_id;
    INSERT INTO notification_read(notification_id, user_id) VALUES (row_id, student_ids[1]);

    -- Lịch sử khởi tạo dữ liệu mẫu: không gán thao tác nhập SQL cho tài khoản người thật.
    INSERT INTO status_history(entity_type, entity_id, from_status, to_status, reason)
        SELECT 'APPLICATION', id, NULL, status, 'Khởi tạo dữ liệu mẫu bằng sample-data.sql'
        FROM room_application WHERE id = ANY(application_ids);
    INSERT INTO status_history(entity_type, entity_id, from_status, to_status, reason)
        SELECT 'CONTRACT', id, NULL, status, 'Khởi tạo dữ liệu mẫu bằng sample-data.sql'
        FROM dormitory_contract WHERE id = ANY(contract_ids);
    INSERT INTO status_history(entity_type, entity_id, from_status, to_status, reason)
        SELECT 'MAINTENANCE', id, NULL, status, 'Khởi tạo dữ liệu mẫu bằng sample-data.sql'
        FROM maintenance_request WHERE id = ANY(ticket_ids);
    INSERT INTO status_history(entity_type, entity_id, from_status, to_status, reason)
        SELECT 'EXTENSION', id, NULL, status, 'Khởi tạo dữ liệu mẫu bằng sample-data.sql'
        FROM contract_extension_request WHERE contract_id = ANY(contract_ids);
    INSERT INTO status_history(entity_type, entity_id, from_status, to_status, reason)
        SELECT 'TRANSFER', id, NULL, status, 'Khởi tạo dữ liệu mẫu bằng sample-data.sql'
        FROM room_transfer_request WHERE contract_id = ANY(contract_ids);
    INSERT INTO status_history(entity_type, entity_id, from_status, to_status, reason)
        SELECT 'SUMMER_CHOICE', id, NULL, status, 'Khởi tạo dữ liệu mẫu bằng sample-data.sql'
        FROM summer_choice WHERE contract_id = ANY(contract_ids);
    INSERT INTO status_history(entity_type, entity_id, from_status, to_status, reason)
        SELECT 'INVOICE', id, NULL, status, 'Khởi tạo dữ liệu mẫu bằng sample-data.sql'
        FROM invoice WHERE code LIKE 'MAU-%';
    INSERT INTO audit_log(action, entity_type, new_value, reason, request_id)
    VALUES ('SAMPLE_DATA_IMPORT', 'SYSTEM',
        jsonb_build_object('students', 12, 'buildings', 3, 'rooms', 9, 'beds', 42,
                           'applications', 12, 'contracts', 9, 'invoices', 14, 'maintenanceRequests', 3),
        'Nạp dữ liệu giả phục vụ kiểm thử CRUD; tài khoản mẫu bị khóa và không liên kết Keycloak.',
        'DMS-SAMPLE-V1');
    RAISE NOTICE 'Đã nạp dữ liệu mẫu: 12 sinh viên, 9 phòng, 42 giường, 9 hợp đồng, 14 hóa đơn.';
END
$seed$;
COMMIT;

-- Kết quả kiểm tra sau khi chạy (chỉ đếm dữ liệu có mã riêng của bộ mẫu).
SELECT 'Tài khoản mẫu (đã khóa)' AS screen, COUNT(*) AS sample_count FROM app_user WHERE username LIKE 'mau.%'
UNION ALL SELECT 'Sinh viên', COUNT(*) FROM student_profile WHERE student_code LIKE 'MAU-SV-%'
UNION ALL SELECT 'Tòa nhà', COUNT(*) FROM building WHERE code LIKE 'MAU-%'
UNION ALL SELECT 'Phòng', COUNT(*) FROM room WHERE building_id IN (SELECT id FROM building WHERE code LIKE 'MAU-%')
UNION ALL SELECT 'Giường', COUNT(*) FROM bed WHERE room_id IN
    (SELECT r.id FROM room r JOIN building b ON b.id = r.building_id WHERE b.code LIKE 'MAU-%')
UNION ALL SELECT 'Đơn đăng ký', COUNT(*) FROM room_application WHERE student_id IN
    (SELECT user_id FROM student_profile WHERE student_code LIKE 'MAU-SV-%')
UNION ALL SELECT 'Hợp đồng', COUNT(*) FROM dormitory_contract WHERE code LIKE 'MAU-%'
UNION ALL SELECT 'Hóa đơn', COUNT(*) FROM invoice WHERE code LIKE 'MAU-%'
UNION ALL SELECT 'Sửa chữa', COUNT(*) FROM maintenance_request WHERE code LIKE 'MAU-%'
UNION ALL SELECT 'Thông báo', COUNT(*) FROM notification WHERE title LIKE '[Mẫu]%';

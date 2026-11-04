-- PostgreSQL 16, Flyway V13; chỉ chỉnh một phần bộ sample-data.sql nguyên trạng.
-- Sao lưu trước; chạy toàn bộ bằng psql -v ON_ERROR_STOP=1; không đưa vào Flyway.
-- Không đổi username, Keycloak, emergency_contact, quyền hay xóa lịch sử.
-- Audit, status_history và emergency_contact có thể vẫn chứa nhãn mẫu.
-- Danh mục và học kỳ có thể dùng chung, không khẳng định mọi bảng được làm mới.

BEGIN;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(hashtextextended('dms.sample-data.v1', 0));
SELECT pg_advisory_xact_lock(hashtextextended('dms.realistic-data.v1', 0));
LOCK TABLE audit_log, app_user, student_profile, building, room, bed, room_application,
    dormitory_contract, room_assignment_history, maintenance_request, invoice, invoice_item,
    utility_reading, notification, contract_extension_request, room_transfer_request,
    storage_record, deposit_transaction, registration_period IN SHARE ROW EXCLUSIVE MODE;

DO $data$
DECLARE
    students BIGINT[];
    buildings BIGINT[];
    contracts BIGINT[];
    names TEXT[] := ARRAY[
        'Nguyễn Minh An', 'Trần Ngọc Bình', 'Phạm Gia Bảo', 'Lê Thu Chi',
        'Đặng Quốc Duy', 'Hoàng Mai Giang', 'Đỗ Anh Huy', 'Vũ Khánh Linh',
        'Bùi Đức Minh', 'Trần Phương Nhi', 'Ngô Hoàng Phúc', 'Phan Ngọc Thảo'
    ];
    old_names TEXT[] := ARRAY[
        'Mẫu - Nguyễn Văn An', 'Mẫu - Trần Thị Bình', 'Mẫu - Phạm Minh Châu',
        'Mẫu - Lê Thu Duyên', 'Mẫu - Đặng Quốc Em', 'Mẫu - Hoàng Ngọc Hà',
        'Mẫu - Đỗ Gia Huy', 'Mẫu - Vũ Khánh Linh', 'Mẫu - Lê Tiến Minh',
        'Mẫu - Trần Ngọc Mai', 'Mẫu - Bùi Quốc Nam', 'Mẫu - Đào Thu Phương'
    ];
    i INT;
    affected INT;
    seed_at TIMESTAMPTZ;
BEGIN
    IF to_regclass('public.room_type_amenity') IS NULL
       OR to_regclass('public.storage_location') IS NULL
       OR NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '13' AND success) THEN
        RAISE EXCEPTION 'Cần schema Flyway V13 trước khi chuyển dữ liệu.';
    END IF;
    IF (SELECT count(*) FROM audit_log WHERE request_id = 'DMS-SAMPLE-V1') <> 1 OR
       NOT EXISTS (SELECT 1 FROM audit_log WHERE request_id = 'DMS-SAMPLE-V1'
                   AND action = 'SAMPLE_DATA_IMPORT' AND entity_type = 'SYSTEM') THEN
        RAISE EXCEPTION 'Dấu nhận diện nguồn không đúng hoặc không duy nhất.';
    END IF;
    SELECT created_at INTO STRICT seed_at FROM audit_log WHERE request_id = 'DMS-SAMPLE-V1';
    IF EXISTS (SELECT 1 FROM building WHERE code IN ('KT-K1', 'KT-N2', 'KT-N3')) THEN
        RAISE EXCEPTION 'Mã đích đã tồn tại hoặc script đã chạy; không ghi đè.';
    END IF;

    SELECT array_agg(u.id ORDER BY u.username) INTO students
    FROM app_user u JOIN student_profile s ON s.user_id = u.id
    WHERE u.role = 'STUDENT' AND u.locked AND u.created_at = seed_at AND u.username IN
        (SELECT 'mau.sv.' || lpad(n::text, 2, '0') FROM generate_series(1, 12) n)
      AND s.student_code = 'MAU-SV-' || right(u.username, 2);
    SELECT array_agg(id ORDER BY code) INTO buildings FROM building
    WHERE code IN ('MAU-1', 'MAU-2', 'MAU-3');
    SELECT array_agg(id ORDER BY code) INTO contracts FROM dormitory_contract
    WHERE code IN (SELECT 'MAU-HD-' || lpad(n::text, 2, '0') FROM generate_series(1, 9) n);
    IF coalesce(array_length(students, 1), 0) <> 12
       OR coalesce(array_length(buildings, 1), 0) <> 3
       OR coalesce(array_length(contracts, 1), 0) <> 9
       OR (SELECT count(*) FROM app_user WHERE
           (username IN ('mau.ql.1', 'mau.ql.2') AND role = 'STAFF') OR
           (username IN ('mau.kt.1', 'mau.kt.2') AND role = 'TECHNICIAN')) <> 4
       OR (SELECT count(*) FROM room WHERE building_id = ANY(buildings)) <> 9
       OR (SELECT count(*) FROM bed WHERE room_id IN
           (SELECT id FROM room WHERE building_id = ANY(buildings))) <> 42
       OR (SELECT count(*) FROM room_application WHERE student_id = ANY(students)) <> 12
       OR EXISTS (SELECT 1 FROM generate_series(1, 9) n
                  WHERE NOT EXISTS (SELECT 1 FROM dormitory_contract c
                      JOIN room_application a ON a.id = c.application_id
                      WHERE c.id = contracts[n] AND c.student_id = students[n]
                        AND a.student_id = students[n])) THEN
        RAISE EXCEPTION 'Dữ liệu nguồn khác bộ dự kiến; không thay đổi, cần kiểm tra thủ công.';
    END IF;

    -- Chỉ đổi họ tên hiển thị; giữ mọi thuộc tính xác thực và phân quyền.
    FOR i IN 1..12 LOOP
        UPDATE app_user SET full_name = names[i] WHERE id = students[i] AND full_name = old_names[i];
        GET DIAGNOSTICS affected = ROW_COUNT;
        IF affected <> 1 THEN RAISE EXCEPTION 'Sinh viên %: cập nhật %/1', i, affected; END IF;
        UPDATE student_profile SET student_code = 'SV26' || lpad(i::text, 4, '0')
        WHERE user_id = students[i] AND student_code = 'MAU-SV-' || lpad(i::text, 2, '0');
        GET DIAGNOSTICS affected = ROW_COUNT;
        IF affected <> 1 THEN RAISE EXCEPTION 'Hồ sơ %: cập nhật %/1', i, affected; END IF;
    END LOOP;
    UPDATE app_user SET full_name = CASE username
        WHEN 'mau.ql.1' THEN 'Nguyễn Thanh Hải'
        WHEN 'mau.ql.2' THEN 'Trần Thị Hương'
        WHEN 'mau.kt.1' THEN 'Phạm Đức Long'
        WHEN 'mau.kt.2' THEN 'Lê Thành Trung' END
    WHERE created_at = seed_at AND locked AND (
        (username = 'mau.ql.1' AND role = 'STAFF' AND full_name = 'Mẫu - Cán bộ quản lý khu 1') OR
        (username = 'mau.ql.2' AND role = 'STAFF' AND full_name = 'Mẫu - Cán bộ quản lý khu 2') OR
        (username = 'mau.kt.1' AND role = 'TECHNICIAN' AND full_name = 'Mẫu - Kỹ thuật viên khu 1') OR
        (username = 'mau.kt.2' AND role = 'TECHNICIAN' AND full_name = 'Mẫu - Kỹ thuật viên khu 2'));
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 4 THEN RAISE EXCEPTION 'Cán bộ: cập nhật %/4', affected; END IF;

    -- Danh mục có thể được seed dùng lại: không suy đoán quyền sở hữu để đổi tên.
    FOR i IN 1..3 LOOP
        UPDATE building SET code = (ARRAY['KT-K1', 'KT-N2', 'KT-N3'])[i],
            name = (ARRAY['Ký túc xá K1', 'Ký túc xá N2', 'Ký túc xá N3'])[i]
        WHERE id = buildings[i] AND code = 'MAU-' || i AND name = 'Mẫu - Tòa nhà ' || i;
        GET DIAGNOSTICS affected = ROW_COUNT;
        IF affected <> 1 THEN RAISE EXCEPTION 'Tòa %: cập nhật %/1', i, affected; END IF;
    END LOOP;
    UPDATE room SET code = 'P' || substring(code FROM 2)
    WHERE building_id = ANY(buildings) AND code IN ('M101', 'M102', 'M103');
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 9 THEN RAISE EXCEPTION 'Phòng: cập nhật %/9', affected; END IF;

    FOR i IN 1..12 LOOP
        UPDATE room_application SET note = 'Đề nghị đăng ký chỗ ở nội trú'
        WHERE student_id = students[i] AND created_at = seed_at
          AND note = 'Dữ liệu mẫu - hồ sơ nội trú ' || i;
        GET DIAGNOSTICS affected = ROW_COUNT;
        IF affected <> 1 THEN RAISE EXCEPTION 'Đơn %: cập nhật %/1', i, affected; END IF;
    END LOOP;
    FOR i IN 1..9 LOOP
        UPDATE dormitory_contract SET code = 'HD-SV-' || right(code, 2),
            status_reason = CASE WHEN status_reason = 'Mẫu - Đã hoàn tất trả phòng'
                                 THEN 'Đã hoàn tất trả phòng' ELSE status_reason END
        WHERE id = contracts[i] AND student_id = students[i] AND created_at = seed_at
          AND code = 'MAU-HD-' || lpad(i::text, 2, '0')
          AND (i <> 9 OR status_reason = 'Mẫu - Đã hoàn tất trả phòng');
        GET DIAGNOSTICS affected = ROW_COUNT;
        IF affected <> 1 THEN RAISE EXCEPTION 'Hợp đồng %: cập nhật %/1', i, affected; END IF;
    END LOOP;
    UPDATE room_assignment_history SET note = 'Phân giường nội trú'
    WHERE contract_id = ANY(contracts) AND note = 'Dữ liệu mẫu - phân giường';
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 9 THEN RAISE EXCEPTION 'Phân giường: cập nhật %/9', affected; END IF;

    UPDATE contract_extension_request SET reason = 'Đề nghị tiếp tục ở để hoàn thành học phần',
        decision_reason = CASE WHEN decision_reason = 'Mẫu - Chưa đủ điều kiện gia hạn'
                               THEN 'Chưa đủ điều kiện gia hạn' ELSE decision_reason END
    WHERE contract_id = ANY(contracts[1:3]) AND created_at = seed_at
      AND reason = 'Mẫu - Đề nghị tiếp tục ở để hoàn thành học phần'
      AND (decision_reason IS NULL OR decision_reason = 'Mẫu - Chưa đủ điều kiện gia hạn');
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 3 THEN RAISE EXCEPTION 'Gia hạn: cập nhật %/3', affected; END IF;
    UPDATE room_transfer_request SET reason = replace(reason, 'Mẫu - ', ''),
        decision_reason = replace(decision_reason, 'Mẫu - ', '')
    WHERE created_at = seed_at AND (
        (contract_id = contracts[2] AND reason = 'Mẫu - Đề nghị chuyển sang phòng còn chỗ'
         AND decision_reason IS NULL) OR
        (contract_id = contracts[4] AND reason = 'Mẫu - Đề nghị chuyển khu'
         AND decision_reason = 'Mẫu - Chưa có giường phù hợp'));
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 2 THEN RAISE EXCEPTION 'Chuyển phòng: cập nhật %/2', affected; END IF;
    UPDATE storage_record SET description = 'Hai thùng sách và đồ dùng cá nhân'
    WHERE contract_id = contracts[8] AND description = 'Mẫu - Hai thùng sách và đồ cá nhân'
      AND summer_choice_id IN (SELECT id FROM summer_choice WHERE contract_id = contracts[8]
                               AND choice = 'TEMP_LEAVE' AND created_at = seed_at);
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 1 THEN RAISE EXCEPTION 'Gửi đồ: cập nhật %/1', affected; END IF;
    UPDATE deposit_transaction SET note = replace(note, 'Mẫu - ', '')
    WHERE contract_id = ANY(contracts) AND created_at = seed_at AND (
        (type = 'DEPOSIT' AND note = 'Mẫu - Thu cọc khi nhận phòng') OR
        (contract_id = contracts[9] AND type = 'REFUND' AND note = 'Mẫu - Hoàn cọc khi trả phòng') OR
        (contract_id = contracts[1] AND type = 'DEDUCTION' AND note = 'Mẫu - Trừ cọc sửa khóa tủ') OR
        (contract_id = contracts[1] AND type = 'TOPUP' AND note = 'Mẫu - Đã bù đủ tiền cọc'));
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 11 THEN RAISE EXCEPTION 'Sổ cọc: cập nhật %/11', affected; END IF;

    -- Ba ticket gốc; ticket 03 có created_at sau resolved_at trong seed.
    UPDATE maintenance_request SET code = 'SC-SV-' || right(code, 2),
        description = replace(description, 'Mẫu - ', ''), cost_note = replace(cost_note, 'Mẫu - ', ''),
        created_at = CASE WHEN code = 'MAU-TK-03' THEN resolved_at - interval '1 day' ELSE created_at END
    WHERE created_at = seed_at AND (
        (code = 'MAU-TK-01' AND requester_id = students[1] AND status = 'OPEN'
         AND description = 'Mẫu - Đèn bàn học không sáng, đề nghị kiểm tra.') OR
        (code = 'MAU-TK-02' AND building_id = buildings[2] AND status = 'ASSIGNED'
         AND requester_id = (SELECT id FROM app_user WHERE username = 'mau.ql.2')
         AND description = 'Mẫu - Vòi nước khu vực chung bị rò.') OR
        (code = 'MAU-TK-03' AND requester_id = students[1] AND status = 'CLOSED'
         AND description = 'Mẫu - Đã thay khóa tủ hỏng do sử dụng.'
         AND cost_note = 'Mẫu - Chi phí vật tư có xác nhận'
         AND resolved_at = seed_at - interval '2 days' AND closed_at = seed_at - interval '1 day'
         AND cost_decided_at = seed_at - interval '1 day'));
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 3 THEN RAISE EXCEPTION 'Sửa chữa: cập nhật %/3', affected; END IF;

    -- Seed có 8 hóa đơn phòng (không có 07). Gắn thời điểm phát hành với nhận giường.
    FOR i IN 1..9 LOOP
        IF i = 7 THEN CONTINUE; END IF;
        UPDATE invoice v SET code = 'HP-SV-' || right(v.code, 2),
            issued_at = greatest(v.issued_at, h.started_at),
            paid_at = CASE WHEN v.paid_at IS NULL THEN NULL ELSE greatest(v.paid_at, h.started_at) END,
            due_date = greatest(v.due_date, greatest(v.issued_at, h.started_at)::date)
        FROM invoice_item it JOIN room_assignment_history h ON h.id = it.assignment_id
        WHERE v.code = 'MAU-HP-' || lpad(i::text, 2, '0') AND v.type = 'ROOM_FEE'
          AND v.student_id = students[i] AND h.contract_id = contracts[i] AND h.student_id = students[i]
          AND v.issued_by = (SELECT id FROM app_user WHERE username = 'mau.ql.1')
          AND v.issued_at = date_trunc('day', seed_at) - interval '10 days'
          AND it.invoice_id = v.id AND it.item_type = 'ROOM_FEE' AND it.note = 'Mẫu - Phí phòng theo học kỳ'
          AND (SELECT count(*) FROM invoice_item x WHERE x.invoice_id = v.id) = 1;
        GET DIAGNOSTICS affected = ROW_COUNT;
        IF affected <> 1 THEN RAISE EXCEPTION 'Hóa đơn phòng %: cập nhật %/1', i, affected; END IF;
    END LOOP;
    UPDATE invoice_item SET note = 'Phí phòng theo học kỳ'
    WHERE note = 'Mẫu - Phí phòng theo học kỳ' AND invoice_id IN
        (SELECT id FROM invoice WHERE code LIKE 'HP-SV-%' AND student_id = ANY(students));
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 8 THEN RAISE EXCEPTION 'Chi tiết phí phòng: cập nhật %/8', affected; END IF;

    -- Chỉ số có recorded_at = seed_at, không thể phát hành trước khi ghi chỉ số.
    FOR i IN 1..6 LOOP
        UPDATE invoice v SET code = 'DN-SV-' || right(v.code, 2),
            issued_at = seed_at, due_date = greatest(v.due_date, seed_at::date)
        WHERE v.code = 'MAU-DN-' || lpad(i::text, 2, '0') AND v.type = 'UTILITY'
          AND v.status = 'UNPAID' AND v.issued_at = date_trunc('day', seed_at) - interval '1 day'
          AND v.issued_by = (SELECT id FROM app_user WHERE username = 'mau.ql.1')
          AND v.room_id = (SELECT id FROM room WHERE building_id = buildings[((i - 1) / 2) + 1]
                            AND code = CASE WHEN i % 2 = 1 THEN 'P101' ELSE 'P102' END)
          AND (SELECT count(*) FROM invoice_item it JOIN utility_reading r ON r.id = it.utility_reading_id
               WHERE it.invoice_id = v.id AND r.room_id = v.room_id AND r.billing_month = v.billing_month
                 AND r.recorded_at = seed_at AND r.recorded_by = v.issued_by
                 AND ((it.item_type = 'ELECTRIC' AND r.meter_type = 'ELECTRIC' AND it.note = 'Mẫu - Tiền điện')
                   OR (it.item_type = 'WATER' AND r.meter_type = 'WATER' AND it.note = 'Mẫu - Tiền nước'))) = 2
          AND (SELECT count(*) FROM invoice_item it WHERE it.invoice_id = v.id) = 2;
        GET DIAGNOSTICS affected = ROW_COUNT;
        IF affected <> 1 THEN RAISE EXCEPTION 'Hóa đơn điện nước %: cập nhật %/1', i, affected; END IF;
    END LOOP;
    UPDATE invoice_item SET note = replace(note, 'Mẫu - ', '')
    WHERE note IN ('Mẫu - Tiền điện', 'Mẫu - Tiền nước') AND invoice_id IN
        (SELECT id FROM invoice WHERE code LIKE 'DN-SV-%' AND room_id IN
         (SELECT id FROM room WHERE building_id = ANY(buildings)));
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 12 THEN RAISE EXCEPTION 'Chi tiết điện nước: cập nhật %/12', affected; END IF;

    UPDATE notification SET title = 'Hướng dẫn sinh hoạt nội trú',
        body = 'Sinh viên vui lòng thực hiện nội quy và giữ vệ sinh khu ở.'
    WHERE title = '[Mẫu] Hướng dẫn sử dụng ký túc xá' AND scope = 'ALL' AND created_at = seed_at
      AND body = 'Dữ liệu mẫu để kiểm thử màn thông báo, tìm kiếm và phân trang.'
      AND created_by = (SELECT id FROM app_user WHERE username = 'mau.ql.1');
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 1 THEN RAISE EXCEPTION 'Thông báo ALL: cập nhật %/1', affected; END IF;
    UPDATE notification SET title = 'Lịch vệ sinh khu K1',
        body = 'Đề nghị sinh viên sắp xếp vệ sinh khu vực sinh hoạt chung hằng tuần.'
    WHERE title = '[Mẫu] Lịch vệ sinh khu K1' AND scope = 'UNIT' AND created_at = seed_at
      AND body = 'Dữ liệu minh họa phạm vi đơn vị, không phải lịch vận hành thật.'
      AND management_unit_id = (SELECT management_unit_id FROM building WHERE id = buildings[1])
      AND created_by = (SELECT id FROM app_user WHERE username = 'mau.ql.1');
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 1 THEN RAISE EXCEPTION 'Thông báo UNIT: cập nhật %/1', affected; END IF;
    UPDATE notification SET title = 'Kiểm tra thiết bị phòng P101',
        body = 'Ban quản lý sẽ kiểm tra thiết bị trong phòng; sinh viên vui lòng phối hợp.'
    WHERE title = '[Mẫu] Kiểm tra thiết bị phòng M101' AND scope = 'ROOM' AND created_at = seed_at
      AND body = 'Mẫu thông báo theo phòng.' AND created_by = (SELECT id FROM app_user WHERE username = 'mau.ql.1')
      AND room_id = (SELECT id FROM room WHERE building_id = buildings[1] AND code = 'P101');
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 1 THEN RAISE EXCEPTION 'Thông báo ROOM: cập nhật %/1', affected; END IF;
    UPDATE notification SET title = 'Hồ sơ nội trú đã được tiếp nhận',
        body = 'Hồ sơ của bạn đã được tiếp nhận; vui lòng theo dõi trạng thái trên hệ thống.'
    WHERE title = '[Mẫu] Hồ sơ đã được tiếp nhận' AND scope = 'USER' AND user_id = students[1]
      AND created_at = seed_at AND body = 'Mẫu thông báo riêng của sinh viên.'
      AND created_by = (SELECT id FROM app_user WHERE username = 'mau.ql.1');
    GET DIAGNOSTICS affected = ROW_COUNT;
    IF affected <> 1 THEN RAISE EXCEPTION 'Thông báo USER: cập nhật %/1', affected; END IF;

    -- status_history và audit_log được giữ nguyên, kể cả nội dung lịch sử cũ.
END
$data$;
COMMIT;

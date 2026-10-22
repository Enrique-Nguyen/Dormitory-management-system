-- DataTest V1 mở rộng sample-data.sql. Chỉ ghi vào dữ liệu mang mã DT-/990026/dt.*.
BEGIN;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(hashtextextended('dms.datatest.v1', 0));
DO $fixtures$
DECLARE
    student BIGINT;
    staff BIGINT;
    tech BIGINT;
    unit BIGINT;
    period BIGINT;
    summer BIGINT;
    chosen_bed BIGINT;
    chosen_room BIGINT;
    app BIGINT;
    contract BIGINT;
    ticket BIGINT;
    row_id BIGINT;
    target_bed BIGINT;
    first_day DATE;
    last_day DATE;
    state TEXT;
    i INT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM audit_log WHERE request_id='DMS-DATATEST-V1') THEN
        RAISE EXCEPTION 'Cần nạp phần cơ sở DataTest trước.';
    END IF;
    IF EXISTS (SELECT 1 FROM audit_log WHERE request_id='DMS-DATATEST-EXTRA-V1') THEN
        RETURN;
    END IF;
    SELECT id INTO unit FROM management_unit WHERE code='K1';
    INSERT INTO app_user(username,role,full_name,management_unit_id,cross_unit_access,locked,must_change_password)
    VALUES ('dt.ql.tong-hop','STAFF','DataTest - Quản lý đọc liên đơn vị',unit,TRUE,FALSE,FALSE),
           ('dt.ql.khoa','STAFF','DataTest - Quản lý bị khóa',unit,FALSE,TRUE,FALSE);
    INSERT INTO app_user(username,role,full_name,locked,must_change_password)
    VALUES ('dt.admin','ADMIN','DataTest - Quản trị viên',FALSE,FALSE);
    UPDATE app_user SET cross_unit_access=FALSE WHERE username='dt.ql.1';
    FOR i IN 13..40 LOOP
        INSERT INTO app_user(username,role,full_name,phone,locked,must_change_password)
        VALUES ('990026'||lpad(i::TEXT,4,'0'),'STUDENT','DataTest - Sinh viên '||lpad(i::TEXT,2,'0'),
                '09'||lpad(i::TEXT,8,'0'),i=36,i=35) RETURNING id INTO student;
        INSERT INTO student_profile(user_id,student_code,gender,date_of_birth,faculty,cohort,hometown,emergency_contact)
        VALUES(student,'990026'||lpad(i::TEXT,4,'0'),CASE WHEN i%2=1 THEN 'M' ELSE 'F' END,
               DATE '2005-01-01'+i,'Công nghệ thông tin','K65','Địa chỉ DataTest','Liên hệ DataTest');
    END LOOP;
    UPDATE app_user SET locked=FALSE,must_change_password=FALSE,phone='0900000000'
    WHERE username LIKE 'dt.%' OR username ~ '^990026[0-9]{4}$';
    UPDATE app_user SET locked=TRUE WHERE username IN ('dt.ql.khoa','9900260036');
    UPDATE app_user SET must_change_password=TRUE WHERE username='9900260035';
    SELECT id INTO period FROM registration_period WHERE name='DataTest - Đăng ký nội trú';
    SELECT id INTO summer FROM semester WHERE type='SUMMER' AND end_date<CURRENT_DATE ORDER BY end_date DESC LIMIT 1;
    SELECT id INTO staff FROM app_user WHERE username='dt.ql.1';
    SELECT id INTO tech FROM app_user WHERE username='dt.kt.1';
    FOR i IN 13..20 LOOP
        SELECT user_id INTO student FROM student_profile WHERE student_code='990026'||lpad(i::TEXT,4,'0');
        -- Giường trống đúng giới tính; lấy đơn vị thực từ tòa để nguyện vọng và hợp đồng khớp.
        SELECT b.id,r.id,bd.management_unit_id INTO chosen_bed,chosen_room,unit
        FROM bed b JOIN room r ON r.id=b.room_id JOIN building bd ON bd.id=r.building_id
        WHERE bd.code LIKE 'DT-%' AND r.status='ACTIVE' AND b.status='ACTIVE'
          AND r.gender=CASE WHEN i%2=1 THEN 'M' ELSE 'F' END
          AND NOT EXISTS(SELECT 1 FROM room_assignment_history a WHERE a.bed_id=b.id AND a.ended_at IS NULL)
        ORDER BY b.id LIMIT 1;
        IF chosen_bed IS NULL THEN RAISE EXCEPTION 'Thiếu giường phù hợp cho DataTest %',i; END IF;
        SELECT id INTO staff FROM app_user WHERE username='dt.ql.'||CASE WHEN unit=(SELECT id FROM management_unit WHERE code='K1') THEN '1' ELSE '2' END;
        state:=CASE WHEN i=13 THEN 'REJECTED' WHEN i IN(14,16) THEN 'CANCELLED' ELSE 'ASSIGNED' END;
        INSERT INTO room_application(student_id,period_id,preferred_unit_id,preferred_room_type_id,status,status_reason,submitted_at,note)
        SELECT student,period,unit,r.room_type_id,state,
               CASE WHEN i IN(13,14,16) THEN 'DataTest - Hồ sơ thiếu điều kiện hoặc sinh viên hủy' END,
               CURRENT_TIMESTAMP-INTERVAL '20 day','DataTest - Trạng thái mở rộng'
        FROM room r WHERE r.id=chosen_room RETURNING id INTO app;
        IF i IN(13,14) THEN CONTINUE; END IF;
        state:=CASE WHEN i=15 THEN 'EXPIRED' WHEN i=16 THEN 'CANCELLED' WHEN i=18 THEN 'PENDING' ELSE 'ACTIVE' END;
        INSERT INTO dormitory_contract(code,student_id,application_id,start_date,end_date,status,activation_deadline,
                                        deposit_required,activated_at,activated_by,closed_at,status_reason)
        SELECT 'DT-HD-'||lpad(i::TEXT,2,'0'),student,app,CASE WHEN i IN(15,16) THEN CURRENT_DATE-90 ELSE CURRENT_DATE-45 END,
               CASE WHEN i IN(15,16) THEN CURRENT_DATE-60 WHEN i=17 THEN CURRENT_DATE+10 ELSE CURRENT_DATE+90 END,
               state,CASE WHEN i IN(15,16) THEN CURRENT_TIMESTAMP-INTERVAL '83 day'
                          WHEN i=18 THEN CURRENT_TIMESTAMP+INTERVAL '7 day'
                          ELSE CURRENT_TIMESTAMP-INTERVAL '38 day' END,bd.deposit_amount,
               CASE WHEN i=15 THEN CURRENT_TIMESTAMP-INTERVAL '90 day'
                    WHEN i NOT IN(16,18) THEN CURRENT_TIMESTAMP-INTERVAL '45 day' END,
               CASE WHEN i NOT IN(16,18) THEN staff END,
               CASE WHEN i IN(15,16) THEN CURRENT_TIMESTAMP-INTERVAL '60 day' END,
               CASE WHEN i IN(15,16) THEN 'DataTest - Lịch sử kết thúc hợp đồng' END
        FROM room r JOIN building bd ON bd.id=r.building_id WHERE r.id=chosen_room RETURNING id INTO contract;
        INSERT INTO room_assignment_history(student_id,contract_id,bed_id,kind,started_at,stay_started_at,ended_at,end_reason,price_main,price_summer,created_by)
        SELECT student,contract,chosen_bed,CASE WHEN i IN(16,18) THEN 'HOLD' ELSE 'STAY' END,
               CASE WHEN i IN(15,16) THEN CURRENT_TIMESTAMP-INTERVAL '90 day' WHEN i=18 THEN CURRENT_TIMESTAMP ELSE CURRENT_TIMESTAMP-INTERVAL '45 day' END,
               CASE WHEN i=15 THEN CURRENT_TIMESTAMP-INTERVAL '90 day' WHEN i NOT IN(16,18) THEN CURRENT_TIMESTAMP-INTERVAL '45 day' END,
               CASE WHEN i IN(15,16) THEN CURRENT_TIMESTAMP-INTERVAL '60 day' END,
               CASE WHEN i=15 THEN 'MOVE_OUT' WHEN i=16 THEN 'HOLD_CANCELLED' END,
               rt.price_main,rt.price_summer,staff FROM room r JOIN room_type rt ON rt.id=r.room_type_id WHERE r.id=chosen_room;
        IF i NOT IN(16,18) THEN
            INSERT INTO deposit_transaction(contract_id,type,amount,confirmed_by,created_at,note)
            SELECT contract,'DEPOSIT',deposit_required,staff,activated_at,'DataTest - Thu cọc' FROM dormitory_contract WHERE id=contract;
        END IF;
        IF i=15 THEN
            INSERT INTO move_out_record(contract_id,moved_out_at,asset_condition,unpaid_room_fee,deposit_balance,refund_amount,confirmed_by)
            SELECT id,closed_at,'OK',0,deposit_required,deposit_required,staff FROM dormitory_contract WHERE id=contract;
            INSERT INTO deposit_transaction(contract_id,type,amount,confirmed_by,created_at,note)
            SELECT id,'REFUND',-deposit_required,staff,closed_at,'DataTest - Hoàn cọc lịch sử' FROM dormitory_contract WHERE id=contract;
        END IF;
        IF i=19 THEN
            INSERT INTO maintenance_request(code,requester_id,room_id,category,severity,description,status,assigned_to,cause,repair_cost,closed_at)
            VALUES('DT-TK-COC',student,chosen_room,'FURNITURE','HIGH','DataTest - Hư hỏng tài sản do sử dụng',
                   'CLOSED',NULL,'MISUSE',1500000,CURRENT_TIMESTAMP-INTERVAL '10 day') RETURNING id INTO ticket;
            INSERT INTO deposit_transaction(contract_id,type,amount,maintenance_request_id,due_date,note)
            VALUES(contract,'DEDUCTION',-1500000,ticket,CURRENT_DATE-3,'DataTest - Cần bù cọc, đã quá hạn');
        END IF;
    END LOOP;

    -- Đủ trạng thái sửa chữa, có việc trong cả hai đơn vị; RESOLVED mới chưa bị job đóng ngay.
    SELECT id INTO staff FROM app_user WHERE username='dt.ql.1';
    FOR i IN 4..8 LOOP
        state:=CASE i WHEN 4 THEN 'IN_PROGRESS' WHEN 5 THEN 'RESOLVED' WHEN 6 THEN 'REJECTED' WHEN 7 THEN 'CANCELLED' ELSE 'ASSIGNED' END;
        INSERT INTO maintenance_request(code,requester_id,building_id,floor,category,severity,description,status,assigned_to,resolved_at)
        SELECT 'DT-TK-'||lpad(i::TEXT,2,'0'),staff,id,1,'ELECTRIC','MEDIUM','DataTest - Yêu cầu sửa chữa trạng thái '||state,
               state,CASE WHEN i IN(4,5,8) THEN tech END,
               CASE WHEN i=5 THEN CURRENT_TIMESTAMP-INTERVAL '1 day' END
        FROM building WHERE code='DT-1';
    END LOOP;
    INSERT INTO contract_extension_request(contract_id,source,old_end_date,new_end_date,status,reason)
    SELECT id,'STUDENT',end_date,end_date+30,'CANCELLED','DataTest - Sinh viên hủy yêu cầu gia hạn'
    FROM dormitory_contract WHERE code='DT-HD-04';
    INSERT INTO room_transfer_request(contract_id,status,reason)
    SELECT id,'CANCELLED','DataTest - Sinh viên hủy yêu cầu chuyển phòng'
    FROM dormitory_contract WHERE code='DT-HD-06';

    -- Một hồ sơ chuyển phòng đã duyệt có cả phân phòng cũ và mới, cùng đơn vị.
    SELECT id,student_id INTO contract,student FROM dormitory_contract WHERE code='DT-HD-05';
    SELECT bed_id INTO target_bed FROM room_assignment_history WHERE contract_id=contract AND ended_at IS NULL;
    SELECT b.id INTO chosen_bed FROM bed b JOIN room r ON r.id=b.room_id JOIN building bd ON bd.id=r.building_id
    WHERE bd.code IN('DT-2','DT-3') AND r.gender='M' AND b.id<>target_bed AND b.status='ACTIVE' AND r.status='ACTIVE'
      AND r.id<>(SELECT room_id FROM bed WHERE id=target_bed)
      AND NOT EXISTS(SELECT 1 FROM room_assignment_history a WHERE a.bed_id=b.id AND a.ended_at IS NULL) ORDER BY b.id LIMIT 1;
    SELECT id INTO staff FROM app_user WHERE username='dt.ql.2';
    UPDATE room_assignment_history SET started_at=CURRENT_TIMESTAMP-INTERVAL '15 day',stay_started_at=CURRENT_TIMESTAMP-INTERVAL '15 day'
    WHERE contract_id=contract AND ended_at IS NULL;
    INSERT INTO room_assignment_history(student_id,contract_id,bed_id,kind,started_at,stay_started_at,ended_at,end_reason,price_main,price_summer,created_by)
    SELECT student,contract,chosen_bed,'STAY',CURRENT_TIMESTAMP-INTERVAL '21 day',CURRENT_TIMESTAMP-INTERVAL '21 day',
           CURRENT_TIMESTAMP-INTERVAL '15 day','TRANSFER',rt.price_main,rt.price_summer,staff
    FROM bed b JOIN room r ON r.id=b.room_id JOIN room_type rt ON rt.id=r.room_type_id WHERE b.id=chosen_bed;
    INSERT INTO room_transfer_request(contract_id,target_bed_id,status,reason,decided_by,decided_at)
    VALUES(contract,target_bed,'APPROVED','DataTest - Chuyển phòng cùng đơn vị đã hoàn tất',staff,CURRENT_TIMESTAMP-INTERVAL '15 day');

    -- Gửi đồ đã nhận lại: một hợp đồng lịch sử trước hè, không đụng phiếu đang gửi của SV08.
    SELECT user_id INTO student FROM student_profile WHERE student_code='9900260021';
    SELECT start_date,end_date INTO first_day,last_day FROM semester WHERE id=summer;
    SELECT id INTO period FROM registration_period WHERE name='DataTest - Đợt hè đã kết thúc';
    SELECT b.id,r.id,bd.management_unit_id INTO chosen_bed,chosen_room,unit
    FROM bed b JOIN room r ON r.id=b.room_id JOIN building bd ON bd.id=r.building_id
    WHERE bd.code='DT-2' AND r.gender='M' AND b.status='ACTIVE' ORDER BY b.id LIMIT 1;
    INSERT INTO room_application(student_id,period_id,preferred_unit_id,status,note)
    VALUES(student,period,unit,'ASSIGNED','DataTest - Hồ sơ lịch sử hè') RETURNING id INTO app;
    INSERT INTO dormitory_contract(code,student_id,application_id,start_date,end_date,status,activation_deadline,
                                    deposit_required,activated_at,activated_by,closed_at,status_reason)
    VALUES('DT-HD-21',student,app,first_day-30,last_day,'EXPIRED',first_day-20,2000000,first_day-30,staff,last_day,
           'DataTest - Đã tạm rời, nhận lại đồ và kết thúc hợp đồng') RETURNING id INTO contract;
    INSERT INTO room_assignment_history(student_id,contract_id,bed_id,kind,started_at,stay_started_at,ended_at,end_reason,price_main,price_summer,created_by)
    VALUES(student,contract,chosen_bed,'STAY',first_day-30,first_day-30,first_day,'SUMMER_LEAVE',2500000,900000,staff),
          (student,contract,chosen_bed,'STAY',first_day+10,first_day+10,last_day,'MOVE_OUT',2500000,900000,staff);
    INSERT INTO summer_choice(contract_id,semester_id,choice,status,decided_by,decided_at)
    VALUES(contract,summer,'TEMP_LEAVE','APPROVED',staff,first_day) RETURNING id INTO row_id;
    INSERT INTO storage_record(contract_id,summer_choice_id,previous_bed_id,package_count,description,location,
                               deposited_at,received_by,returned_at,returned_by)
    VALUES(contract,row_id,chosen_bed,3,'DataTest - Ba kiện đồ đã nhận','Kho DataTest - Kệ B2',first_day,staff,first_day+10,staff);
    INSERT INTO deposit_transaction(contract_id,type,amount,confirmed_by,created_at,note)
    VALUES(contract,'DEPOSIT',2000000,staff,first_day-30,'DataTest - Thu cọc lịch sử'),
          (contract,'REFUND',-2000000,staff,last_day,'DataTest - Hoàn cọc sau khi trả phòng');
    INSERT INTO move_out_record(contract_id,moved_out_at,asset_condition,unpaid_room_fee,deposit_balance,refund_amount,confirmed_by)
    VALUES(contract,last_day,'OK',0,2000000,2000000,staff);

    SELECT semester_id INTO summer FROM registration_period WHERE name='DataTest - Đăng ký phương án hè';
    INSERT INTO summer_choice(contract_id,semester_id,choice,status,decided_by,decided_at)
    SELECT id,summer,'TEMP_LEAVE','REJECTED',staff,CURRENT_TIMESTAMP FROM dormitory_contract WHERE code='DT-HD-04';
    INSERT INTO status_history(entity_type,entity_id,to_status,actor_id,reason)
    SELECT 'SUMMER_CHOICE',sc.id,sc.status,staff,'DataTest - Không đủ điều kiện tạm rời hè'
    FROM summer_choice sc JOIN dormitory_contract c ON c.id=sc.contract_id WHERE c.code='DT-HD-04';

    -- Hóa đơn chưa trả đã quá hạn; giữ đúng ngày phát hành trước hạn thanh toán.
    UPDATE invoice SET issued_at=CURRENT_TIMESTAMP-INTERVAL '10 day',due_date=CURRENT_DATE-3
    WHERE code='DT-HP-05' AND status='UNPAID';
    INSERT INTO utility_rate(meter_type,unit_price,effective_from,created_by)
    SELECT 'ELECTRIC',3800,CURRENT_DATE+30,staff
    WHERE NOT EXISTS(SELECT 1 FROM utility_rate WHERE meter_type='ELECTRIC' AND effective_from=CURRENT_DATE+30);

    INSERT INTO notification(scope,building_id,title,body,created_by)
    SELECT 'BUILDING',id,'DataTest - Thông báo theo tòa','Lịch vệ sinh khu sinh hoạt chung.',staff FROM building WHERE code='DT-3';
    INSERT INTO notification(scope,building_id,floor,title,body,created_by)
    SELECT 'FLOOR',id,1,'DataTest - Thông báo theo tầng','Lịch kiểm tra điện tầng 1.',staff FROM building WHERE code='DT-3';
    INSERT INTO notification(scope,user_id,title,body,created_by)
    SELECT 'USER',id,'DataTest - Thông báo cho kỹ thuật viên','Kiểm tra danh sách việc được giao.',staff FROM app_user WHERE username='dt.kt.1';

    -- Lịch sử mở rộng cho màn timeline; không sửa hoặc xóa audit cũ.
    INSERT INTO status_history(entity_type,entity_id,to_status,actor_id,reason)
    SELECT 'APPLICATION',id,status,staff,'DataTest - Trạng thái ban đầu'
    FROM room_application WHERE note='DataTest - Trạng thái mở rộng';
    INSERT INTO status_history(entity_type,entity_id,to_status,actor_id,reason)
    SELECT 'CONTRACT',id,status,staff,'DataTest - Trạng thái ban đầu'
    FROM dormitory_contract WHERE code IN('DT-HD-15','DT-HD-16','DT-HD-17','DT-HD-18','DT-HD-19','DT-HD-20');
    INSERT INTO status_history(entity_type,entity_id,to_status,actor_id,reason)
    SELECT 'MAINTENANCE',id,status,staff,'DataTest - Trạng thái ban đầu'
    FROM maintenance_request WHERE code IN('DT-TK-04','DT-TK-05','DT-TK-06','DT-TK-07','DT-TK-08','DT-TK-COC');
    INSERT INTO audit_log(actor_id,action,entity_type,new_value,reason,request_id)
    VALUES(NULL,'DATATEST_SEED','SYSTEM',jsonb_build_object('students',40,'prefix','DT-'),
           'DataTest cho bốn vai trò; dữ liệu giả phục vụ kiểm thử local','DMS-DATATEST-EXTRA-V1');
END $fixtures$;
COMMIT;

-- Chỉ đọc: mọi chỉ số lỗi phải bằng 0; bộ dữ liệu chuẩn có 40 SV, 9 phòng, 42 giường.
WITH scoped_students AS (
    SELECT p.*,u.username,u.keycloak_user_id FROM student_profile p JOIN app_user u ON u.id=p.user_id
    WHERE p.student_code ~ '^990026[0-9]{4}$'
), scoped_assignments AS (
    SELECT a.* FROM room_assignment_history a JOIN dormitory_contract c ON c.id=a.contract_id WHERE c.code LIKE 'DT-%'
), checks AS (
    SELECT 'students' AS name,count(*)::bigint AS actual,40::bigint AS expected FROM scoped_students
    UNION ALL SELECT 'unlinked_or_wrong_username',count(*),0 FROM scoped_students WHERE keycloak_user_id IS NULL OR username<>student_code
    UNION ALL SELECT 'rooms',count(*),9 FROM room r JOIN building b ON b.id=r.building_id WHERE b.code LIKE 'DT-%'
    UNION ALL SELECT 'beds',count(*),42 FROM bed bed JOIN room r ON r.id=bed.room_id JOIN building b ON b.id=r.building_id WHERE b.code LIKE 'DT-%'
    UNION ALL SELECT 'application_statuses',count(DISTINCT status),6 FROM room_application a JOIN scoped_students s ON s.user_id=a.student_id
    UNION ALL SELECT 'contract_statuses',count(DISTINCT status),5 FROM dormitory_contract WHERE code LIKE 'DT-%'
    UNION ALL SELECT 'maintenance_statuses',count(DISTINCT status),7 FROM maintenance_request WHERE code LIKE 'DT-%'
    UNION ALL SELECT 'transfer_statuses',count(DISTINCT t.status),4 FROM room_transfer_request t JOIN dormitory_contract c ON c.id=t.contract_id WHERE c.code LIKE 'DT-%'
    UNION ALL SELECT 'extension_statuses',count(DISTINCT t.status),4 FROM contract_extension_request t JOIN dormitory_contract c ON c.id=t.contract_id WHERE c.code LIKE 'DT-%'
    UNION ALL SELECT 'summer_choice_types',count(DISTINCT t.choice),3 FROM summer_choice t JOIN dormitory_contract c ON c.id=t.contract_id WHERE c.code LIKE 'DT-%'
    UNION ALL SELECT 'summer_choice_statuses',count(DISTINCT t.status),3 FROM summer_choice t JOIN dormitory_contract c ON c.id=t.contract_id WHERE c.code LIKE 'DT-%'
    UNION ALL SELECT 'storage_states',count(DISTINCT (s.returned_at IS NOT NULL)),2 FROM storage_record s JOIN dormitory_contract c ON c.id=s.contract_id WHERE c.code LIKE 'DT-%'
    UNION ALL SELECT 'duplicate_open_bed',count(*),0 FROM (SELECT bed_id FROM scoped_assignments WHERE ended_at IS NULL GROUP BY bed_id HAVING count(*)>1) d
    UNION ALL SELECT 'duplicate_open_student',count(*),0 FROM (SELECT student_id FROM scoped_assignments WHERE ended_at IS NULL GROUP BY student_id HAVING count(*)>1) d
    UNION ALL SELECT 'gender_mismatch',count(*),0 FROM scoped_assignments a JOIN bed b ON b.id=a.bed_id JOIN room r ON r.id=b.room_id JOIN student_profile p ON p.user_id=a.student_id WHERE r.gender<>p.gender
    UNION ALL SELECT 'historical_bed_overlap',count(*),0 FROM scoped_assignments a JOIN scoped_assignments b ON a.bed_id=b.bed_id AND a.id<b.id
        WHERE tstzrange(a.started_at,a.ended_at,'[)') && tstzrange(b.started_at,b.ended_at,'[)')
    UNION ALL SELECT 'historical_student_overlap',count(*),0 FROM scoped_assignments a JOIN scoped_assignments b ON a.student_id=b.student_id AND a.id<b.id
        WHERE tstzrange(a.started_at,a.ended_at,'[)') && tstzrange(b.started_at,b.ended_at,'[)')
    UNION ALL SELECT 'inactive_bed_or_room_still_occupied',count(*),0 FROM scoped_assignments a JOIN bed b ON b.id=a.bed_id JOIN room r ON r.id=b.room_id
        WHERE a.ended_at IS NULL AND (b.status<>'ACTIVE' OR r.status<>'ACTIVE')
    UNION ALL SELECT 'closed_contract_still_assigned',count(*),0 FROM scoped_assignments a JOIN dormitory_contract c ON c.id=a.contract_id
        WHERE a.ended_at IS NULL AND c.status IN('CANCELLED','EXPIRED','TERMINATED')
    UNION ALL SELECT 'invoice_total_mismatch',count(*),0 FROM invoice i
        WHERE i.code LIKE 'DT-%' AND i.total_amount<>(SELECT COALESCE(sum(amount),0) FROM invoice_item WHERE invoice_id=i.id)
    UNION ALL SELECT 'cancelled_contract_assigned_application',count(*),0 FROM dormitory_contract c JOIN room_application a ON a.id=c.application_id
        WHERE c.code LIKE 'DT-%' AND c.status='CANCELLED' AND a.status='ASSIGNED'
)
SELECT name,actual,expected,actual=expected AS passed FROM checks ORDER BY name;

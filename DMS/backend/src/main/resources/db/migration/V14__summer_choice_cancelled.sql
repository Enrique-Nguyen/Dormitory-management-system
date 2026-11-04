-- Sinh viên rút lựa chọn hè còn chờ duyệt bằng trạng thái CANCELLED thay cho xóa cứng, giữ bản ghi
-- và status_history. Bản ghi CANCELLED/REJECTED được mở lại về PENDING khi sinh viên gửi lại trong
-- đợt hè còn mở, nên vẫn giữ UNIQUE (contract_id, semester_id).
ALTER TABLE summer_choice DROP CONSTRAINT IF EXISTS summer_choice_status_check;
ALTER TABLE summer_choice
    ADD CONSTRAINT summer_choice_status_check
    CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED'));

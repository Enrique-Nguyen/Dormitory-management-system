-- V10 đã có hai bản được áp dụng: một bản còn kiểm tra hai năm liên tiếp,
-- một bản chỉ kiểm tra định dạng để giữ nguyên dữ liệu danh mục cũ.
-- Đồng bộ bằng migration mới; không sửa V10 hoặc đổi/xóa học kỳ đã có.
ALTER TABLE academic_year DROP CONSTRAINT ck_academic_year_code;
ALTER TABLE academic_year
    ADD CONSTRAINT ck_academic_year_code CHECK (code ~ '^[0-9]{4}-[0-9]{4}$');

-- FR-REG-05: lưu ghi chú của Staff khi xác nhận diện ưu tiên ngay trên dòng xác nhận.
-- Trước đây ghi chú chỉ vào audit_log nên cột "Ghi chú" ở chi tiết đơn luôn trống.
-- Không sửa V1-V6; migration tăng version theo thông lệ API V5.
ALTER TABLE application_priority
    ADD COLUMN note VARCHAR(500);

COMMENT ON COLUMN application_priority.note IS
    'Ghi chú xác minh của Staff khi duyệt diện ưu tiên (null: chưa xác nhận)';

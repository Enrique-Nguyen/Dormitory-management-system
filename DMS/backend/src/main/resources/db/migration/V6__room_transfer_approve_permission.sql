-- Quyền duyệt/từ chối yêu cầu chuyển phòng (FR-CON-07/08) cấp mặc định cho STAFF.
-- Không sửa V1–V5. Không tăng version: migration chạy trước khi ứng dụng nhận yêu cầu.
UPDATE role_permission_matrix
SET permissions = permissions || '["ROOM_TRANSFER_APPROVE"]'::jsonb
WHERE role = 'STAFF'
  AND NOT permissions ? 'ROOM_TRANSFER_APPROVE';

-- Staff duyệt/từ chối lựa chọn hè cần SUMMER_REGISTRATION_UPDATE (REG-01). Việc sửa nội dung lựa chọn
-- vẫn chỉ dành cho sinh viên sở hữu ở tầng Service. Không sửa V1–V18.
UPDATE role_permission_matrix
SET permissions = permissions || '["SUMMER_REGISTRATION_UPDATE"]'::jsonb
WHERE role = 'STAFF'
  AND NOT permissions ? 'SUMMER_REGISTRATION_UPDATE';

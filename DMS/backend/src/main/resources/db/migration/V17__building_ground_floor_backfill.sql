-- V16 mặc định has_ground_floor = FALSE cho mọi tòa. Tòa đã khai tầng 0 (building_floor) hoặc có phòng
-- ở tầng 0 trước V16 phải được đánh dấu có tầng trệt, để kiểm tra quy mô tầng không chặn dữ liệu đang dùng.
UPDATE building b
SET has_ground_floor = TRUE
WHERE has_ground_floor = FALSE
  AND (EXISTS (SELECT 1 FROM building_floor f WHERE f.building_id = b.id AND f.floor = 0)
       OR EXISTS (SELECT 1 FROM room r WHERE r.building_id = b.id AND r.floor = 0));

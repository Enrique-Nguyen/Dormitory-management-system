-- Nguồn tạo do server quyết định; bản không xác định được nguồn giữ khóa như đã phát hành.
ALTER TABLE invoice
    ADD COLUMN creation_source VARCHAR(10) NOT NULL DEFAULT 'ISSUED',
    ADD CONSTRAINT ck_invoice_creation_source CHECK (creation_source IN ('MANUAL', 'ISSUED'));

-- CRUD hiện có luôn ghi audit theo ID. Không suy nguồn chỉ từ việc có lịch sử trạng thái.
-- Không chuyển các hóa đơn đã có chi tiết tài chính thành hóa đơn tạo tay.
UPDATE invoice i
SET creation_source = 'MANUAL'
WHERE NOT EXISTS (SELECT 1 FROM invoice_item item WHERE item.invoice_id = i.id)
  AND EXISTS (SELECT 1 FROM audit_log a
      WHERE a.entity_type = 'INVOICE' AND a.entity_id = i.id AND a.action = 'CRUD_CREATE');

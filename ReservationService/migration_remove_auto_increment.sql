-- Migration: Xóa AUTO_INCREMENT từ vehicle_id để cho phép set giá trị thủ công
-- Để đảm bảo vehicle_id khớp với external_vehicle_id

USE co_ownership_booking;

-- Xóa AUTO_INCREMENT từ vehicle_id
ALTER TABLE vehicles MODIFY COLUMN vehicle_id BIGINT NOT NULL;


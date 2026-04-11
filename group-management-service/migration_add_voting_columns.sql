-- Migration script to add missing columns to Voting table
-- These columns are used by the Voting entity but missing from the database schema

USE Group_Management_DB;

-- Add deadline column (deadline for voting)
ALTER TABLE Voting 
ADD COLUMN deadline DATETIME NULL COMMENT 'Hạn chót bỏ phiếu';

-- Add status column (voting status: OPEN, CLOSED, CANCELLED)
ALTER TABLE Voting 
ADD COLUMN status VARCHAR(20) DEFAULT 'OPEN' COMMENT 'Trạng thái bỏ phiếu: OPEN, CLOSED, CANCELLED';

-- Add closedAt column (when voting was closed)
ALTER TABLE Voting 
ADD COLUMN closedAt DATETIME NULL COMMENT 'Thời điểm đóng bỏ phiếu';

-- Add createdBy column (user ID who created the vote)
ALTER TABLE Voting 
ADD COLUMN createdBy INT NULL COMMENT 'User ID tạo bỏ phiếu';

-- Update existing records to have default status
UPDATE Voting SET status = 'OPEN' WHERE status IS NULL;

SELECT '✅ Migration completed: Added deadline, status, closedAt, and createdBy columns to Voting table' as '';


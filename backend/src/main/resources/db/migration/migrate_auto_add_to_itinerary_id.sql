-- 迁移脚本：将 auto_add 字段改为 itinerary_id 字段
-- 这个脚本将 auto_add 布尔字段转换为 itinerary_id 长整型字段

-- 1. 添加新的 itinerary_id 字段
ALTER TABLE user_destinations 
ADD COLUMN itinerary_id BIGINT NOT NULL DEFAULT 0;

-- 2. 将现有的 auto_add 数据迁移到 itinerary_id
-- auto_add = false 的记录设置 itinerary_id = 0 (手动添加)
-- auto_add = true 的记录设置 itinerary_id = 0 (因为我们无法确定具体来自哪个行程，先设为手动)
UPDATE user_destinations 
SET itinerary_id = 0;

-- 3. 删除旧的 auto_add 字段
ALTER TABLE user_destinations 
DROP COLUMN auto_add;

-- 4. 为 itinerary_id 添加索引以提高查询性能
CREATE INDEX idx_user_destinations_itinerary_id ON user_destinations(itinerary_id);

-- 5. 确保 start_date 和 end_date 字段存在（如果不存在则添加）
-- 这些字段可能已经存在，所以使用 IF NOT EXISTS 语法
-- 注意：MySQL 8.0+ 支持这种语法，如果是较老版本可能需要调整

-- 检查并添加 start_date 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'user_destinations' 
               AND COLUMN_NAME = 'start_date');

SET @sqlstmt := IF(@exist = 0, 
                   'ALTER TABLE user_destinations ADD COLUMN start_date DATE',
                   'SELECT ''start_date column already exists''');

PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 end_date 字段  
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'user_destinations' 
               AND COLUMN_NAME = 'end_date');

SET @sqlstmt := IF(@exist = 0, 
                   'ALTER TABLE user_destinations ADD COLUMN end_date DATE',
                   'SELECT ''end_date column already exists''');

PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt; 
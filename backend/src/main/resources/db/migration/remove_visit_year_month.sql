-- 删除 visit_year_month 字段的迁移脚本
-- 这个脚本将从 user_destinations 表中删除 visit_year_month 字段

USE voyagemate;

-- 1. 删除 visit_year_month 字段
ALTER TABLE user_destinations 
DROP COLUMN visit_year_month;

-- 2. 对于期望目的地，确保它们的日期和天数设置正确
-- 将所有期望目的地的 start_date 和 end_date 设置为 1900-01-01，days 设置为 -1
UPDATE user_destinations 
SET start_date = '1900-01-01', 
    end_date = '1900-01-01', 
    days = -1 
WHERE type = '期望目的地';

-- 3. 显示更新结果
SELECT 
    type,
    COUNT(*) as count,
    MIN(start_date) as min_start_date,
    MAX(start_date) as max_start_date,
    MIN(days) as min_days,
    MAX(days) as max_days
FROM user_destinations 
GROUP BY type; 
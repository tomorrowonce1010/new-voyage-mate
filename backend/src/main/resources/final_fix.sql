-- 最终修复脚本：禁用外键检查后进行修改
USE voyagemate;

-- 1. 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 2. 删除现有的外键约束（使用常见的约束名）
ALTER TABLE itinerary_days DROP FOREIGN KEY fk_first_activity;
ALTER TABLE itinerary_days DROP FOREIGN KEY fk_last_activity;

-- 3. 修改字段定义，允许NULL值
ALTER TABLE itinerary_days 
MODIFY COLUMN first_activity_id BIGINT DEFAULT NULL COMMENT '起始活动id，NULL表示没有活动';

ALTER TABLE itinerary_days 
MODIFY COLUMN last_activity_id BIGINT DEFAULT NULL COMMENT '末尾活动id，NULL表示没有活动';

-- 4. 将现有的-1值更新为NULL
UPDATE itinerary_days SET first_activity_id = NULL WHERE first_activity_id = -1;
UPDATE itinerary_days SET last_activity_id = NULL WHERE last_activity_id = -1;

-- 5. 修复 itinerary_activities 表
ALTER TABLE itinerary_activities 
MODIFY COLUMN prev_id BIGINT DEFAULT NULL COMMENT '前序活动id，NULL表示没有前序活动';

ALTER TABLE itinerary_activities 
MODIFY COLUMN next_id BIGINT DEFAULT NULL COMMENT '后序活动id，NULL表示没有后序活动';

UPDATE itinerary_activities SET prev_id = NULL WHERE prev_id = -1;
UPDATE itinerary_activities SET next_id = NULL WHERE next_id = -1;

-- 6. 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 7. 重新添加外键约束（可选）
ALTER TABLE itinerary_days
ADD CONSTRAINT fk_first_activity
FOREIGN KEY (first_activity_id) REFERENCES itinerary_activities(id) ON DELETE SET NULL;

ALTER TABLE itinerary_days
ADD CONSTRAINT fk_last_activity
FOREIGN KEY (last_activity_id) REFERENCES itinerary_activities(id) ON DELETE SET NULL;
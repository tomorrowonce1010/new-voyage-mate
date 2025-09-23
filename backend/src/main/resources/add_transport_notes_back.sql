-- 重新添加transport_notes字段到itinerary_activities表
USE voyagemate;
 
-- 检查字段是否存在，如果不存在则添加
ALTER TABLE itinerary_activities 
ADD COLUMN IF NOT EXISTS transport_notes TEXT COMMENT '交通方式备注'; 
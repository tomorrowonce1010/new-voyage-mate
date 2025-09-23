-- 修复travel_group_applications表的唯一约束
-- 允许用户重新申请加入组团

USE voyagemate;

-- 1. 删除原有的唯一约束
ALTER TABLE travel_group_applications 
DROP INDEX unique_group_applicant;

-- 2. 添加新的唯一约束：只对待审核状态的申请进行唯一性约束
ALTER TABLE travel_group_applications 
ADD UNIQUE KEY unique_pending_application (group_id, applicant_id, status);

-- 3. 清理可能存在的重复数据（如果有的话）
-- 对于每个用户在每个组团的待审核申请，只保留最新的一条
DELETE t1 FROM travel_group_applications t1
INNER JOIN travel_group_applications t2 
WHERE t1.id < t2.id 
  AND t1.group_id = t2.group_id 
  AND t1.applicant_id = t2.applicant_id 
  AND t1.status = t2.status 
  AND t1.status = '待审核'; 
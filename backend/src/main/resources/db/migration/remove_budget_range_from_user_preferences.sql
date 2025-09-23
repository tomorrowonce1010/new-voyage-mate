-- 删除用户偏好表中的budget_range字段并添加special_requirements_description字段
-- 执行日期：2025-07-05

-- 添加特殊需求描述字段
ALTER TABLE user_preferences 
ADD COLUMN special_requirements_description TEXT 
COMMENT '特殊需求描述';

-- 删除预算范围字段
ALTER TABLE user_preferences 
DROP COLUMN budget_range;

-- 更新表结构说明
ALTER TABLE user_preferences 
COMMENT = '用户偏好表 - 更新于2025-07-05：移除budget_range字段，添加special_requirements_description字段'; 
-- Profile功能测试数据初始化脚本
-- 用于测试环境的数据库初始化

-- 清理测试数据（如果存在）
DELETE FROM user_destinations WHERE user_preferences_id IN (SELECT id FROM user_preferences WHERE user_id IN (SELECT id FROM user WHERE email LIKE '%test%'));
DELETE FROM user_preferences WHERE user_id IN (SELECT id FROM user WHERE email LIKE '%test%');
DELETE FROM user_profiles WHERE user_id IN (SELECT id FROM user WHERE email LIKE '%test%');
DELETE FROM user WHERE email LIKE '%test%';

-- 清理测试目的地和标签
DELETE FROM destinations WHERE name LIKE '%测试%';
DELETE FROM tags WHERE name LIKE '%测试%';

-- 插入测试标签
INSERT INTO tags (id, name, description, created_at) VALUES
(1001, '测试标签1', '用于测试的标签1', NOW()),
(1002, '测试标签2', '用于测试的标签2', NOW()),
(1003, '测试标签3', '用于测试的标签3', NOW());

-- 插入测试目的地
INSERT INTO destinations (id, name, description, image_url, country, city, latitude, longitude, created_at) VALUES
(1001, '测试目的地1', '这是一个测试目的地1', 'test-dest1.jpg', '中国', '北京', 39.9042, 116.4074, NOW()),
(1002, '测试目的地2', '这是一个测试目的地2', 'test-dest2.jpg', '中国', '上海', 31.2304, 121.4737, NOW()),
(1003, '测试目的地3', '这是一个测试目的地3', 'test-dest3.jpg', '中国', '广州', 23.1291, 113.2644, NOW());

-- 插入测试用户
INSERT INTO user (id, username, email, created_at, updated_at) VALUES
(1001, '测试用户1', 'testuser1@example.com', NOW(), NOW()),
(1002, '测试用户2', 'testuser2@example.com', NOW(), NOW()),
(1003, '测试用户3', 'testuser3@example.com', NOW(), NOW());

-- 插入测试用户档案
INSERT INTO user_profiles (id, user_id, avatar_url, birthday, signature, bio, created_at, updated_at) VALUES
(1001, 1001, 'test-avatar1.jpg', '1990-01-01', '测试签名1', '测试个人简介1', NOW(), NOW()),
(1002, 1002, 'test-avatar2.jpg', '1995-05-15', '测试签名2', '测试个人简介2', NOW(), NOW()),
(1003, 1003, NULL, NULL, NULL, NULL, NOW(), NOW());

-- 插入测试用户偏好
INSERT INTO user_preferences (id, user_id, special_requirements, travel_preferences, special_requirements_description) VALUES
(1001, 1001, '无特殊需求', '{"1": 1, "2": 0, "3": 1}', '测试特殊需求描述1'),
(1002, 1002, '无障碍设施', '{"1": 0, "2": 1, "3": 0}', '测试特殊需求描述2'),
(1003, 1003, NULL, NULL, NULL);

-- 插入测试用户目的地
INSERT INTO user_destinations (id, user_preferences_id, destination_id, type, start_date, end_date, days, notes, created_at) VALUES
(1001, 1001, 1001, '历史目的地', '2024-01-01', '2024-01-05', 5, '测试历史目的地笔记1', NOW()),
(1002, 1001, 1002, '期望目的地', '1900-01-01', '1900-01-01', -1, '测试期望目的地笔记1', NOW()),
(1003, 1002, 1003, '历史目的地', '2024-06-01', '2024-06-10', 10, '测试历史目的地笔记2', NOW());

-- 提交事务
COMMIT; 
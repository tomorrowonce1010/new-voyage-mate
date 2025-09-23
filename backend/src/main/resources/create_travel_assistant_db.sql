-- 智能伴游助手数据库建库脚本
-- 适用MySQL 8.0+

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS voyagemate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE voyagemate;

-- 1. 用户基础信息表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) COMMENT '用户名，可重复',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT '邮箱，唯一标识，登录用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_email (email) COMMENT '邮箱索引'
) COMMENT='用户基础信息';

-- 2. 用户认证信息表
CREATE TABLE IF NOT EXISTS user_auth (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '关联的用户ID',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt加密后的密码',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_login TIMESTAMP NULL COMMENT '最后登录时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='用户认证信息表';

-- 3. 用户档案表
CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '关联的用户ID',
    avatar_url VARCHAR(500) COMMENT '头像',
    birthday DATE COMMENT '生日',
    signature VARCHAR(255) COMMENT '个性签名',
    bio TEXT COMMENT '个人简介',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) COMMENT='用户档案';

-- 4. 用户偏好表
CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '关联的用户ID',
    travel_preferences JSON COMMENT '旅行偏好',
    special_requirements TEXT COMMENT '特殊需求'
) COMMENT='用户偏好';

-- 5. 标签表
CREATE TABLE IF NOT EXISTS tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag VARCHAR(50) UNIQUE NOT NULL
) COMMENT='标签表';

-- 6. 目的地表
CREATE TABLE IF NOT EXISTS destinations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '目的地名称',
    description TEXT COMMENT '目的地描述',
    image_url VARCHAR(500) COMMENT '主图片URL',
    latitude DECIMAL(10,6) COMMENT '纬度',
    longitude DECIMAL(10,6) COMMENT '经度',
    join_count INT DEFAULT 0 COMMENT '被加入行程次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    tag_scores JSON COMMENT '标签权值（长度为40，权值范围0-100）'
) COMMENT='目的地信息';

-- 7. 景点表
CREATE TABLE IF NOT EXISTS attractions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    destination_id BIGINT NOT NULL COMMENT '所属目的地id',
    name VARCHAR(200) NOT NULL COMMENT '景点名称',
    description TEXT COMMENT '景点描述',
    image_url VARCHAR(500) COMMENT '主图片URL',
    latitude DECIMAL(10,6) COMMENT '纬度',
    longitude DECIMAL(10,6) COMMENT '经度',
    category ENUM('旅游景点','交通站点','餐饮','住宿') NOT NULL COMMENT '景点类别',
    opening_hours JSON COMMENT '开放时间',
    join_count INT DEFAULT 0 COMMENT '被加入行程次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    tag_scores JSON COMMENT '标签权值（长度为40，权值范围0-100）',
    FOREIGN KEY (destination_id) REFERENCES destinations(id) ON DELETE CASCADE,
    INDEX idx_destination_id (destination_id)
) COMMENT='景点信息';

-- 8. 行程主表
CREATE TABLE IF NOT EXISTS itineraries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '行程归属者的用户id',
    title VARCHAR(200) NOT NULL COMMENT '行程名称',
    image_url VARCHAR(500) COMMENT '主图片URL',
    start_date DATE NOT NULL COMMENT '起始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    budget DECIMAL(10,2) COMMENT '预算',
    traveler_count INT COMMENT '出行人数',
    travel_status ENUM('待出行','已出行') DEFAULT '待出行' COMMENT '出行状态',
    edit_status ENUM('草稿','完成') DEFAULT '草稿' COMMENT '编辑状态',
    permission_status ENUM('所有人可见','仅获得链接者可见','私人') DEFAULT '私人' COMMENT '权限状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='行程主表';

-- 9. 行程日程表
CREATE TABLE IF NOT EXISTS itinerary_days (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    itinerary_id BIGINT NOT NULL,
    day_number INT NOT NULL COMMENT '第几天',
    date DATE NOT NULL COMMENT '日期',
    title VARCHAR(200) NOT NULL COMMENT '景点名称',
    first_activity_id BIGINT DEFAULT NULL COMMENT '起始活动id，NULL表示没有活动',
    last_activity_id BIGINT DEFAULT NULL COMMENT '末尾活动id，NULL表示没有活动',
    accommodation JSON COMMENT '住宿信息 {"name":"","address":"","price":0}',
    notes TEXT COMMENT '当日备注',
    weather_info JSON COMMENT '天气信息',
    actual_cost DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE,
    INDEX idx_itinerary_id (itinerary_id)
) COMMENT='行程日程，包含链表起始活动id和末尾活动id';

-- 10. 行程活动表（伪双向链表结构）
CREATE TABLE IF NOT EXISTS itinerary_activities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    itinerary_day_id BIGINT NOT NULL,
    prev_id BIGINT DEFAULT NULL COMMENT '前序活动id，NULL表示没有前序活动',
    next_id BIGINT DEFAULT NULL COMMENT '后序活动id，NULL表示没有后序活动',
    title VARCHAR(200) NOT NULL COMMENT '活动名称',
    transport_mode VARCHAR(50) NOT NULL DEFAULT '步行' COMMENT '到达景点的交通方式',
    attraction_id BIGINT NOT NULL COMMENT '景点',
    start_time TIME COMMENT '起始时间',
    end_time TIME COMMENT '结束时间',
    transport_notes TEXT COMMENT '交通方式备注',
    attraction_notes TEXT COMMENT '景点备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (itinerary_day_id) REFERENCES itinerary_days(id) ON DELETE CASCADE,
    FOREIGN KEY (attraction_id) REFERENCES attractions(id) ON DELETE CASCADE,
    FOREIGN KEY (prev_id) REFERENCES itinerary_activities(id) ON DELETE SET NULL,
    FOREIGN KEY (next_id) REFERENCES itinerary_activities(id) ON DELETE SET NULL,
    INDEX idx_day_id (itinerary_day_id),
    INDEX idx_attraction_id (attraction_id)
) COMMENT='日程活动（伪双向链表结构，prev_id/next_id指向前后活动）';

-- 11. 添加itinerary_days表的外键约束（在itinerary_activities表创建后）
-- 由于first_activity_id和last_activity_id默认为NULL，这些外键约束可以正常工作
ALTER TABLE itinerary_days 
ADD CONSTRAINT fk_first_activity 
FOREIGN KEY (first_activity_id) REFERENCES itinerary_activities(id) ON DELETE SET NULL;

ALTER TABLE itinerary_days 
ADD CONSTRAINT fk_last_activity 
FOREIGN KEY (last_activity_id) REFERENCES itinerary_activities(id) ON DELETE SET NULL;

-- 12. 社区条目表
CREATE TABLE IF NOT EXISTS community_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    itinerary_id BIGINT NOT NULL COMMENT '关联的行程id',
    share_code CHAR(16) UNIQUE COMMENT '分享码',
    description TEXT COMMENT '行程描述',
    view_count INT DEFAULT 0 COMMENT '点击次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE
) COMMENT='社区条目';

-- 13. 社区条目-标签关联表
CREATE TABLE IF NOT EXISTS community_entry_tags (
    share_entry_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (share_entry_id, tag_id) COMMENT '社区条目-标签',
    FOREIGN KEY (share_entry_id) REFERENCES community_entries(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) COMMENT='行程分享条目-标签关联';

-- 14. 用户偏好目的地表（历史/期望合并）
CREATE TABLE IF NOT EXISTS user_destinations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_preferences_id BIGINT NOT NULL COMMENT '所属的用户偏好的id',
    destination_id BIGINT NOT NULL COMMENT '目的地id',
    type ENUM('历史目的地','期望目的地') NOT NULL COMMENT '类型',
    visit_year_month CHAR(7) COMMENT '游玩年月，格式YYYY-MM，仅历史目的地用，期望目的地值为0000000',
    days INT COMMENT '游玩天数，仅历史目的地用，期望目的地值为-1',
    notes VARCHAR(255) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    auto_add BOOLEAN DEFAULT FALSE COMMENT '是否是根据用户的已出行行程自动添加的历史目的地',
    FOREIGN KEY (user_preferences_id) REFERENCES user_preferences(id) ON DELETE CASCADE,
    FOREIGN KEY (destination_id) REFERENCES destinations(id) ON DELETE CASCADE,
    INDEX idx_user_preferences (user_preferences_id),
    INDEX idx_destination (destination_id)
) COMMENT='用户偏好目的地';
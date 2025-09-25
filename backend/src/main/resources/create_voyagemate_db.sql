-- 智能伴游助手VoyageMate建库脚本
-- 适用 MySQL 8.0+

-- 创建数据库
CREATE DATABASE IF NOT EXISTS voyagemate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 选择数据库
USE voyagemate;

-- 一、基础信息（yzf）：

-- 1.用户基础信息表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) COMMENT '用户名，可重复',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT '邮箱，唯一标识，登录用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_email (email) COMMENT '邮箱索引'
) COMMENT='用户基础信息';

-- 2.用户认证信息表
CREATE TABLE IF NOT EXISTS user_auth (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '关联的用户ID',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt加密后的密码',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_login TIMESTAMP NULL COMMENT '最后登录时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='用户认证信息表';

-- 3.用户档案表
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

-- 4.用户偏好表
CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '关联的用户ID',
    travel_preferences JSON COMMENT '旅行偏好',
    special_requirements TEXT COMMENT '特殊需求',
    special_requirements_description TEXT COMMENT '特殊需求描述',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='用户偏好';

-- 5.标签表
CREATE TABLE IF NOT EXISTS tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag VARCHAR(50) UNIQUE NOT NULL
) COMMENT='标签表';

-- 6.目的地表
CREATE TABLE IF NOT EXISTS destinations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    amap_poi_id VARCHAR(50) COMMENT '高德地图POI ID',
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

-- 7.景点表
CREATE TABLE IF NOT EXISTS attractions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    amap_poi_id VARCHAR(50) COMMENT '高德地图POI ID',
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

-- 8.行程主表
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
    group_id BIGINT COMMENT '作为团队行程时的团队id（个人行程时为null）',
    creator_id BIGINT COMMENT '作为团队行程时的创建者id（个人行程时为null）',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='行程主表';

-- 9.行程日程表
CREATE TABLE IF NOT EXISTS itinerary_days (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    itinerary_id BIGINT NOT NULL COMMENT '行程id',
    day_number INT NOT NULL COMMENT '第几天',
    date DATE NOT NULL COMMENT '日期',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    first_activity_id BIGINT DEFAULT NULL COMMENT '起始活动id，NULL表示没有活动',
    last_activity_id BIGINT DEFAULT NULL COMMENT '末尾活动id，NULL表示没有活动',
    notes TEXT COMMENT '当日备注',
    actual_cost DECIMAL(10,2) COMMENT '实际花费',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE,
    INDEX idx_itinerary_id (itinerary_id)
) COMMENT='行程日程，包含链表起始活动id和末尾活动id';

-- 10.行程活动表（伪双向链表结构）
CREATE TABLE IF NOT EXISTS itinerary_activities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    itinerary_day_id BIGINT NOT NULL COMMENT '所属日程id',
    prev_id BIGINT DEFAULT NULL COMMENT '前序活动id，NULL表示没有前序活动',
    next_id BIGINT DEFAULT NULL COMMENT '后序活动id，NULL表示没有后序活动',
    title VARCHAR(200) NOT NULL COMMENT '活动名称',
    transport_mode VARCHAR(50) NOT NULL DEFAULT '步行' COMMENT '到达景点的交通方式',
    attraction_id BIGINT NOT NULL COMMENT '景点id',
    start_time TIME COMMENT '起始时间',
    end_time TIME COMMENT '结束时间',
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

-- 11.社区条目表
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

-- 12.社区条目-标签关联表
CREATE TABLE IF NOT EXISTS community_entry_tags (
    share_entry_id BIGINT NOT NULL COMMENT '社区条目id',
    tag_id BIGINT NOT NULL COMMENT '标签id',
    PRIMARY KEY (share_entry_id, tag_id),
    FOREIGN KEY (share_entry_id) REFERENCES community_entries(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) COMMENT='社区条目-标签关联';

-- 13.用户偏好目的地表
CREATE TABLE IF NOT EXISTS user_destinations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_preferences_id BIGINT NOT NULL COMMENT '所属的用户偏好的id',
    destination_id BIGINT NOT NULL COMMENT '目的地id',
    itinerary_id BIGINT NOT NULL DEFAULT 0 COMMENT '来源行程id，0表示手动添加',
    type ENUM('历史目的地','期望目的地') NOT NULL COMMENT '类型',
    start_date DATE COMMENT '游玩开始日期',
    end_date DATE COMMENT '游玩结束日期',
    days INT COMMENT '游玩天数，期望目的地为-1',
    notes VARCHAR(255) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_preferences_id) REFERENCES user_preferences(id) ON DELETE CASCADE,
    FOREIGN KEY (destination_id) REFERENCES destinations(id) ON DELETE CASCADE,
    INDEX idx_user_preferences (user_preferences_id),
    INDEX idx_destination (destination_id),
    INDEX idx_user_destinations_itinerary_id (itinerary_id)
) COMMENT='用户偏好目的地';

-- 添加行程日程与活动链表外键关联（解决循环依赖）
SET FOREIGN_KEY_CHECKS = 0;

-- 直接添加外键，如果已存在会报错但不会中断脚本执行
-- 注意：如果外键已存在，会报错但脚本会继续执行
ALTER TABLE itinerary_days
  ADD CONSTRAINT fk_first_activity
  FOREIGN KEY (first_activity_id) REFERENCES itinerary_activities(id) ON DELETE SET NULL;

ALTER TABLE itinerary_days
  ADD CONSTRAINT fk_last_activity
  FOREIGN KEY (last_activity_id) REFERENCES itinerary_activities(id) ON DELETE SET NULL;

SET FOREIGN_KEY_CHECKS = 1; 




-- 二、智能组团部分（zyq）：

-- 14.组团主表
CREATE TABLE IF NOT EXISTS travel_groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '组团标题',
    description TEXT COMMENT '组团描述',
    destination_id BIGINT NOT NULL COMMENT '目的地ID',
    creator_id BIGINT NOT NULL COMMENT '创建者用户ID',
    max_members INT NOT NULL DEFAULT 6 COMMENT '最大成员数量',
    current_members INT NOT NULL DEFAULT 1 COMMENT '当前成员数量',
    start_date DATE NOT NULL COMMENT '出发日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    estimated_budget DECIMAL(10,2) COMMENT '预估人均费用',
    group_type ENUM('自由行', '半自助', '深度游') NOT NULL DEFAULT '自由行' COMMENT '组团类型',
    status ENUM('招募中', '已满员', '已出行', '已取消') NOT NULL DEFAULT '招募中' COMMENT '组团状态',
    privacy_level ENUM('公开', '仅链接可见', '邀请制') NOT NULL DEFAULT '公开' COMMENT '隐私等级',
    auto_match_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用智能匹配',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    group_chat_id BIGINT COMMENT '群聊id',
    is_public BOOLEAN DEFAULT TRUE COMMENT '是否公开',
    FOREIGN KEY (destination_id) REFERENCES destinations(id) ON DELETE RESTRICT,
    FOREIGN KEY (creator_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_destination (destination_id),
    INDEX idx_creator (creator_id),
    INDEX idx_status (status),
    INDEX idx_dates (start_date, end_date),
    INDEX idx_auto_match (auto_match_enabled)
) COMMENT='组团信息主表';

-- 15.组团成员表
CREATE TABLE IF NOT EXISTS travel_group_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '组团ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role ENUM('创建者', '管理员', '成员') NOT NULL DEFAULT '成员' COMMENT '成员角色',
    join_status ENUM('已加入', '待审核', '已拒绝', '已退出') NOT NULL DEFAULT '已加入' COMMENT '加入状态',
    join_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    FOREIGN KEY (group_id) REFERENCES travel_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY unique_group_user (group_id, user_id),
    INDEX idx_group (group_id),
    INDEX idx_user (user_id),
    INDEX idx_status (join_status)
) COMMENT='组团成员表';

-- 16.组团偏好标签关联表
CREATE TABLE IF NOT EXISTS travel_group_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '组团ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    weight DECIMAL(3,2) DEFAULT 1.0 COMMENT '标签权重',
    FOREIGN KEY (group_id) REFERENCES travel_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    UNIQUE KEY unique_group_tag (group_id, tag_id),
    INDEX idx_group (group_id),
    INDEX idx_tag (tag_id)
) COMMENT='组团偏好标签关联表';

-- 17.组团申请表
CREATE TABLE IF NOT EXISTS travel_group_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '组团ID',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    application_message TEXT COMMENT '申请留言',
    status ENUM('待审核', '已同意', '已拒绝', '已撤回') NOT NULL DEFAULT '待审核' COMMENT '申请状态',
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    processed_at TIMESTAMP NULL COMMENT '处理时间',
    processed_by BIGINT NULL COMMENT '处理人ID',
    FOREIGN KEY (group_id) REFERENCES travel_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (applicant_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (processed_by) REFERENCES user(id) ON DELETE SET NULL,
    -- 修改唯一约束：只对待审核状态的申请进行唯一性约束
    UNIQUE KEY unique_pending_application (group_id, applicant_id, status),
    INDEX idx_group (group_id),
    INDEX idx_applicant (applicant_id),
    INDEX idx_status (status)
) COMMENT='组团申请表';

-- 18.组团行程关联表
CREATE TABLE IF NOT EXISTS group_itineraries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '组团ID',
    itinerary_id BIGINT NOT NULL COMMENT '行程ID',
    is_template BOOLEAN DEFAULT FALSE COMMENT '是否为模板行程',
    created_by BIGINT NOT NULL COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (group_id) REFERENCES travel_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY unique_group_itinerary (group_id, itinerary_id),
    INDEX idx_group (group_id),
    INDEX idx_itinerary (itinerary_id)
) COMMENT='组团行程关联表';

-- 创建触发器：自动更新组团成员数量
DELIMITER $$
CREATE TRIGGER update_group_members_count_insert
    AFTER INSERT ON travel_group_members
    FOR EACH ROW
BEGIN
    IF NEW.join_status = '已加入' THEN
        UPDATE travel_groups 
        SET current_members = (
            SELECT COUNT(*) 
            FROM travel_group_members 
            WHERE group_id = NEW.group_id AND join_status = '已加入'
        )
        WHERE id = NEW.group_id;
    END IF;
END$$

CREATE TRIGGER update_group_members_count_update
    AFTER UPDATE ON travel_group_members
    FOR EACH ROW
BEGIN
    UPDATE travel_groups 
    SET current_members = (
        SELECT COUNT(*) 
        FROM travel_group_members 
        WHERE group_id = NEW.group_id AND join_status = '已加入'
    )
    WHERE id = NEW.group_id;
END$$

CREATE TRIGGER update_group_members_count_delete
    AFTER DELETE ON travel_group_members
    FOR EACH ROW
BEGIN
    UPDATE travel_groups 
    SET current_members = (
        SELECT COUNT(*) 
        FROM travel_group_members 
        WHERE group_id = OLD.group_id AND join_status = '已加入'
    )
    WHERE id = OLD.group_id;
END$$

DELIMITER ; 

-- 三、好友及聊天功能部分（zzm）：

-- 19.私聊消息表 (根据文件内容新增)
CREATE TABLE user_chat_message (
                                   message_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
                                   from_user_id BIGINT NOT NULL COMMENT '发送方用户ID',
                                   to_user_id BIGINT NOT NULL COMMENT '接收方用户ID',
                                   message_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '消息时间',
                                   content VARCHAR(500) NOT NULL COMMENT '消息内容'
) COMMENT '用户私聊消息表';

-- 20.好友关系表 (保持原结构)
CREATE TABLE friend (
                        id BIGINT NOT NULL COMMENT '用户ID',
                        friend_id BIGINT NOT NULL COMMENT '好友用户ID',
                        PRIMARY KEY (id, friend_id)
) COMMENT '好友关系表';

-- 21.群聊信息表 (添加索引优化)
CREATE TABLE group_chat_information (
                                        group_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '群组ID',
                                        group_name VARCHAR(500) NOT NULL COMMENT '群组名称',
                                        KEY idx_group_name (group_name)  -- 新增名称索引
) COMMENT '群组基本信息';

-- 22.群成员表 (添加外键约束)
CREATE TABLE group_chat_member (
                                   group_id BIGINT NOT NULL COMMENT '群组ID',
                                   user_id BIGINT NOT NULL COMMENT '成员用户ID',
                                   PRIMARY KEY (group_id, user_id),
                                   FOREIGN KEY (group_id)
                                       REFERENCES group_chat_information(group_id)
                                       ON DELETE CASCADE
                                       ON UPDATE CASCADE
) COMMENT '群组成员表';

-- 23.群聊消息表 (添加发送者外键约束)
CREATE TABLE group_chat_message (
                                    message_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
                                    group_id BIGINT NOT NULL COMMENT '群组ID',
                                    user_id BIGINT NOT NULL COMMENT '发送者ID',
                                    content VARCHAR(500) NOT NULL COMMENT '消息内容',
                                    message_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP COMMENT '消息时间',
                                    FOREIGN KEY (group_id)
                                        REFERENCES group_chat_information(group_id)
                                        ON DELETE CASCADE
                                        ON UPDATE CASCADE,
                                    FOREIGN KEY (group_id, user_id)
                                        REFERENCES group_chat_member(group_id, user_id)
                                        ON DELETE CASCADE
                                        ON UPDATE CASCADE
) COMMENT '群组消息表';
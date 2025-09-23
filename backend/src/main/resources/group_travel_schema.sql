-- 智能组团功能数据库表设计
-- 适用于VoyageMate项目

USE voyagemate;

-- 1. 组团主表
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
    departure_city VARCHAR(100) COMMENT '出发城市',
    group_avatar_url VARCHAR(500) COMMENT '组团头像',
    contact_info JSON COMMENT '联系方式信息',
    travel_style JSON COMMENT '旅行风格偏好',
    FOREIGN KEY (destination_id) REFERENCES destinations(id) ON DELETE RESTRICT,
    FOREIGN KEY (creator_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_destination (destination_id),
    INDEX idx_creator (creator_id),
    INDEX idx_status (status),
    INDEX idx_dates (start_date, end_date),
    INDEX idx_auto_match (auto_match_enabled)
) COMMENT='组团信息主表';

-- 2. 组团成员表
CREATE TABLE IF NOT EXISTS travel_group_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '组团ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role ENUM('创建者', '管理员', '成员') NOT NULL DEFAULT '成员' COMMENT '成员角色',
    join_status ENUM('已加入', '待审核', '已拒绝', '已退出') NOT NULL DEFAULT '已加入' COMMENT '加入状态',
    join_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    introduction TEXT COMMENT '个人介绍',
    emergency_contact JSON COMMENT '紧急联系人信息',
    compatibility_score DECIMAL(5,2) COMMENT '匹配度分数(0-100)',
    notes TEXT COMMENT '备注信息',
    FOREIGN KEY (group_id) REFERENCES travel_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY unique_group_user (group_id, user_id),
    INDEX idx_group (group_id),
    INDEX idx_user (user_id),
    INDEX idx_status (join_status)
) COMMENT='组团成员表';

-- 3. 组团偏好标签关联表
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

-- 4. 组团申请表
CREATE TABLE IF NOT EXISTS travel_group_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '组团ID',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    application_message TEXT COMMENT '申请留言',
    status ENUM('待审核', '已同意', '已拒绝', '已撤回') NOT NULL DEFAULT '待审核' COMMENT '申请状态',
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    processed_at TIMESTAMP NULL COMMENT '处理时间',
    processed_by BIGINT NULL COMMENT '处理人ID',
    rejection_reason TEXT COMMENT '拒绝原因',
    compatibility_score DECIMAL(5,2) COMMENT '匹配度分数',
    FOREIGN KEY (group_id) REFERENCES travel_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (applicant_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (processed_by) REFERENCES user(id) ON DELETE SET NULL,
    -- 修改唯一约束：只对待审核状态的申请进行唯一性约束
    UNIQUE KEY unique_pending_application (group_id, applicant_id, status),
    INDEX idx_group (group_id),
    INDEX idx_applicant (applicant_id),
    INDEX idx_status (status)
) COMMENT='组团申请表';

-- 5. 智能推荐记录表
CREATE TABLE IF NOT EXISTS group_recommendations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    group_id BIGINT NOT NULL COMMENT '推荐的组团ID',
    recommendation_score DECIMAL(5,2) NOT NULL COMMENT '推荐分数(0-100)',
    recommendation_reason JSON COMMENT '推荐原因详情',
    is_clicked BOOLEAN DEFAULT FALSE COMMENT '是否点击查看',
    is_applied BOOLEAN DEFAULT FALSE COMMENT '是否申请加入',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '推荐时间',
    clicked_at TIMESTAMP NULL COMMENT '点击时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES travel_groups(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_group_rec (user_id, group_id),
    INDEX idx_user (user_id),
    INDEX idx_group (group_id),
    INDEX idx_score (recommendation_score)
) COMMENT='智能推荐记录表';

-- 6. 组团聊天消息表
CREATE TABLE IF NOT EXISTS group_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '组团ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    message_type ENUM('文本', '图片', '位置', '系统') NOT NULL DEFAULT '文本' COMMENT '消息类型',
    content TEXT NOT NULL COMMENT '消息内容',
    media_url VARCHAR(500) COMMENT '媒体文件URL',
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否已删除',
    FOREIGN KEY (group_id) REFERENCES travel_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_group_time (group_id, sent_at),
    INDEX idx_sender (sender_id)
) COMMENT='组团聊天消息表';

-- 7. 用户匹配偏好设置表
CREATE TABLE IF NOT EXISTS user_match_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    preferred_group_size_min INT DEFAULT 3 COMMENT '偏好最小组团人数',
    preferred_group_size_max INT DEFAULT 6 COMMENT '偏好最大组团人数',
    budget_range_min DECIMAL(10,2) COMMENT '预算范围最小值',
    budget_range_max DECIMAL(10,2) COMMENT '预算范围最大值',
    age_range_min INT COMMENT '年龄范围最小值',
    age_range_max INT COMMENT '年龄范围最大值',
    gender_preference ENUM('不限', '同性', '异性') DEFAULT '不限' COMMENT '性别偏好',
    smoking_tolerance BOOLEAN DEFAULT TRUE COMMENT '是否接受吸烟者',
    pet_tolerance BOOLEAN DEFAULT TRUE COMMENT '是否接受携带宠物',
    language_requirements JSON COMMENT '语言要求',
    personality_preferences JSON COMMENT '性格偏好',
    notification_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用匹配通知',
    auto_apply_enabled BOOLEAN DEFAULT FALSE COMMENT '是否启用自动申请',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) COMMENT='用户匹配偏好设置表';

-- 8. 组团行程关联表
CREATE TABLE IF NOT EXISTS group_itineraries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '组团ID',
    itinerary_id BIGINT NOT NULL COMMENT '行程ID',
    is_template BOOLEAN DEFAULT FALSE COMMENT '是否为模板行程',
    vote_count INT DEFAULT 0 COMMENT '投票数',
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
-- 创建社区条目表
CREATE TABLE IF NOT EXISTS community_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    itinerary_id BIGINT NOT NULL UNIQUE,
    share_code VARCHAR(16) NOT NULL UNIQUE,
    description TEXT,
    view_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE,
    INDEX idx_share_code (share_code),
    INDEX idx_itinerary_id (itinerary_id),
    INDEX idx_created_at (created_at)
);

-- 添加注释
ALTER TABLE community_entries 
    COMMENT = '社区条目表，存储可分享的行程信息';

ALTER TABLE community_entries 
    MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN itinerary_id BIGINT NOT NULL COMMENT '关联的行程ID',
    MODIFY COLUMN share_code VARCHAR(16) NOT NULL COMMENT '分享码，16位随机字符',
    MODIFY COLUMN description TEXT COMMENT '用户添加的行程描述',
    MODIFY COLUMN view_count INT NOT NULL DEFAULT 0 COMMENT '在社区中的点击量',
    MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'; 
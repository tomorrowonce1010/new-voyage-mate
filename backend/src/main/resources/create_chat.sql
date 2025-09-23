USE voyagemate;

-- 私聊消息表 (根据文件内容新增)
CREATE TABLE user_chat_message (
                                   message_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
                                   from_user_id BIGINT NOT NULL COMMENT '发送方用户ID',
                                   to_user_id BIGINT NOT NULL COMMENT '接收方用户ID',
                                   message_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '消息时间',
                                   content VARCHAR(500) NOT NULL COMMENT '消息内容'
) COMMENT '用户私聊消息表';

-- 好友关系表 (保持原结构)
CREATE TABLE friend (
                        id BIGINT NOT NULL COMMENT '用户ID',
                        friend_id BIGINT NOT NULL COMMENT '好友用户ID',
                        PRIMARY KEY (id, friend_id)
) COMMENT '好友关系表';

-- 群聊信息表 (添加索引优化)
CREATE TABLE group_chat_information (
                                        group_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '群组ID',
                                        group_name VARCHAR(30) NOT NULL COMMENT '群组名称',
                                        KEY idx_group_name (group_name)  -- 新增名称索引
) COMMENT '群组基本信息';

-- 群成员表 (添加外键约束)
CREATE TABLE group_chat_member (
                                   group_id BIGINT NOT NULL COMMENT '群组ID',
                                   user_id BIGINT NOT NULL COMMENT '成员用户ID',
                                   join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
                                   PRIMARY KEY (group_id, user_id),
                                   FOREIGN KEY (group_id)
                                       REFERENCES group_chat_information(group_id)
                                       ON DELETE CASCADE
                                       ON UPDATE CASCADE
) COMMENT '群组成员表';

-- 群聊消息表 (添加发送者外键约束)
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
                                    FOREIGN KEY (user_id)
                                        REFERENCES group_chat_member(user_id)
                                        ON DELETE CASCADE
                                        ON UPDATE CASCADE
) COMMENT '群组消息表';
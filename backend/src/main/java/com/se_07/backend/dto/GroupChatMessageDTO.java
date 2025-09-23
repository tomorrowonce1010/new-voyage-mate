package com.se_07.backend.dto;

import java.time.LocalDateTime;

public class GroupChatMessageDTO {
    private Long messageId;
    private Long groupId;
    private Long userId;
    private String content;
    private LocalDateTime messageTime;
    private String userName;

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getMessageTime() { return messageTime; }
    public void setMessageTime(LocalDateTime messageTime) { this.messageTime = messageTime; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
} 
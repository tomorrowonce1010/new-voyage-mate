package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;

@Entity
@Table(name = "group_chat_member")
@Data
@IdClass(GroupChatMember.GroupChatMemberId.class)
public class GroupChatMember {
    @Id
    @Column(name = "group_id")
    private Long groupId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Data
    public static class GroupChatMemberId implements Serializable {
        private Long groupId;
        private Long userId;
    }
} 
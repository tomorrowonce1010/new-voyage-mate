package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "group_chat_information")
@Data
public class GroupChatInformation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_name", length = 500, nullable = false)
    private String groupName;
} 
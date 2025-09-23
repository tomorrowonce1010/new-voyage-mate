package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity

@Table(name = "user_chat_message")
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "from_user_id", nullable = false)
    private Long fromId;

    @Column(name = "to_user_id", nullable = false)
    private Long toId;

    @Column(name = "message_time", nullable = false)
    private LocalDateTime messageTime;

    @Column(name = "content", length = 500, nullable = true)
    private String content;
}
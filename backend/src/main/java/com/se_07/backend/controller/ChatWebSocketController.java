package com.se_07.backend.controller;

import com.se_07.backend.dto.ChatMessageDTO;
import com.se_07.backend.entity.ChatMessage;
import com.se_07.backend.service.ChatMessageService;
import com.se_07.backend.service.impl.ChatMessageServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ChatMessageService chatMessageService;

    // 前端发送到 /app/chat，服务端处理后推送到 /topic/chat.{toId} 和 /topic/chat.{fromId}
    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessageDTO message) {
        // 保存到数据库
        ChatMessage saved = chatMessageService.sendMessageDTO(message);
        // 构造返回DTO，包含 userName
        ChatMessageDTO dto = chatMessageService.toDTO(saved);
        // 推送给目标用户
        messagingTemplate.convertAndSend("/topic/chat." + dto.getToId(), dto);
        // 推送给自己
        messagingTemplate.convertAndSend("/topic/chat." + dto.getFromId(), dto);
    }
} 
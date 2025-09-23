package com.se_07.backend.controller;

import com.se_07.backend.dto.GroupChatMessageDTO;
import com.se_07.backend.service.GroupChatService;
import com.se_07.backend.repository.GroupChatMessageRepository;
import com.se_07.backend.entity.GroupChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

@Controller
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class GroupChatWebSocketController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private GroupChatService groupChatService;
    @Autowired
    private GroupChatMessageRepository groupChatMessageRepository;

    /**
     * 处理群聊消息（WebSocket）
     * 消息映射：前端发送到 /app/groupchat，服务端推送到 /topic/group.{groupId}
     * 使用方法：
     *   - 前端通过STOMP/WebSocket发送GroupChatMessageDTO对象到 /app/groupchat
     *   - 服务端保存消息后，推送到所有群成员的 /topic/group.{groupId}
     *   - GroupChatMessageDTO结构示例：
     *     {"groupId":1, "fromUserId":2, "content":"hello", ...}
     *   - 推送消息内容为完整的GroupChatMessageDTO，包含userName等信息
     */
    @MessageMapping("/groupchat")
    public void processGroupMessage(@Payload GroupChatMessageDTO message) {
        // 保存到数据库
        GroupChatMessageDTO saved = groupChatService.sendGroupMessage(message);
        // 查出实体，补全userName
        GroupChatMessage entity = groupChatMessageRepository.findById(saved.getMessageId()).orElse(null);
        GroupChatMessageDTO dto = entity != null ? groupChatService.toDTO(entity) : saved;
        // 推送给群成员
        messagingTemplate.convertAndSend("/topic/group." + dto.getGroupId(), dto);
    }
} 
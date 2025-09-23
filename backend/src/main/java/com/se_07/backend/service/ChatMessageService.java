package com.se_07.backend.service;

import com.se_07.backend.dto.ChatMessageDTO;
import com.se_07.backend.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService {
    ChatMessageDTO sendMessage(ChatMessageDTO dto);
    List<ChatMessageDTO> getMessagesBetween(Long fromId, Long toId);
    List<ChatMessageDTO> getAllMessagesForUser(Long userId);
    List<ChatMessageDTO> getMessagesBetweenUsers(Long userId1, Long userId2);
    ChatMessage sendMessageDTO(ChatMessageDTO dto);
    ChatMessageDTO toDTO(ChatMessage msg);
}
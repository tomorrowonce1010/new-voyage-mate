package com.se_07.backend.service.impl;

import com.se_07.backend.dto.ChatMessageDTO;
import com.se_07.backend.entity.ChatMessage;
import com.se_07.backend.repository.ChatMessageRepository;
import com.se_07.backend.repository.UserRepository;
import com.se_07.backend.entity.User;
import com.se_07.backend.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

    @Override
    public ChatMessageDTO sendMessage(ChatMessageDTO dto) {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(dto.getMessageId());
        msg.setFromId(dto.getFromId());
        msg.setToId(dto.getToId());
        msg.setMessageTime(dto.getMessageTime());
        msg.setContent(dto.getContent());
        ChatMessage saved = chatMessageRepository.save(msg);
        dto.setMessageId(saved.getMessageId());
        return dto;
    }

    @Override
    public ChatMessage sendMessageDTO(ChatMessageDTO dto) {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(dto.getMessageId());
        msg.setFromId(dto.getFromId());
        msg.setToId(dto.getToId());
        msg.setMessageTime(dto.getMessageTime() != null ? dto.getMessageTime() : java.time.LocalDateTime.now());
        msg.setContent(dto.getContent());
        return chatMessageRepository.save(msg);
    }

    @Override
    public List<ChatMessageDTO> getMessagesBetween(Long fromId, Long toId) {
        return chatMessageRepository.findByFromIdAndToId(fromId, toId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageDTO> getAllMessagesForUser(Long userId) {
        return chatMessageRepository.findByFromIdOrToId(userId, userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // 查询两名用户之间所有聊天记录（双向，按时间升序）
    public List<ChatMessageDTO> getMessagesBetweenUsers(Long userId1, Long userId2) {
        return chatMessageRepository.findMessagesBetweenUsers(userId1, userId2)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ChatMessageDTO toDTO(ChatMessage msg) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setMessageId(msg.getMessageId());
        dto.setFromId(msg.getFromId());
        dto.setToId(msg.getToId());
        dto.setMessageTime(msg.getMessageTime());
        dto.setContent(msg.getContent());
        // 查询发送者用户名
        User user = userRepository.findById(msg.getFromId()).orElse(null);
        dto.setUserName(user != null ? user.getUsername() : null);
        // 日志输出
        logger.info("ChatMessageDTO: {}", dto);
        return dto;
    }
}

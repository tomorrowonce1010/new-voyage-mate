package com.se_07.backend.service.impl;

import com.se_07.backend.dto.GroupChatMessageDTO;
import com.se_07.backend.entity.GroupChatMessage;
import com.se_07.backend.repository.GroupChatMessageRepository;
import com.se_07.backend.service.GroupChatService;
import com.se_07.backend.repository.UserRepository;
import com.se_07.backend.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupChatServiceImpl implements GroupChatService {
    @Autowired
    private GroupChatMessageRepository groupChatMessageRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public GroupChatMessageDTO sendGroupMessage(GroupChatMessageDTO dto) {
        GroupChatMessage msg = new GroupChatMessage();
        msg.setGroupId(dto.getGroupId());
        msg.setUserId(dto.getUserId());
        msg.setContent(dto.getContent());
        msg.setMessageTime(dto.getMessageTime() != null ? dto.getMessageTime() : LocalDateTime.now());
        GroupChatMessage saved = groupChatMessageRepository.save(msg);
        dto.setMessageId(saved.getMessageId());
        dto.setMessageTime(saved.getMessageTime());
        return dto;
    }

    @Override
    public List<GroupChatMessageDTO> getGroupMessages(Long groupId) {
        return groupChatMessageRepository.findByGroupIdOrderByMessageTimeAsc(groupId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public GroupChatMessageDTO toDTO(GroupChatMessage msg) {
        GroupChatMessageDTO dto = new GroupChatMessageDTO();
        dto.setMessageId(msg.getMessageId());
        dto.setGroupId(msg.getGroupId());
        dto.setUserId(msg.getUserId());
        dto.setContent(msg.getContent());
        dto.setMessageTime(msg.getMessageTime());
        // 查询发送者用户名
        User user = userRepository.findById(msg.getUserId()).orElse(null);
        dto.setUserName(user != null ? user.getUsername() : null);
        return dto;
    }
} 
package com.se_07.backend.service;

import com.se_07.backend.dto.GroupChatMessageDTO;
import com.se_07.backend.entity.GroupChatInformation;
import com.se_07.backend.entity.GroupChatMessage;
import com.se_07.backend.repository.GroupChatInformationRepository;
import com.se_07.backend.repository.GroupChatMemberRepository;
import com.se_07.backend.repository.GroupChatMessageRepository;
import com.se_07.backend.repository.UserRepository;
import com.se_07.backend.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GroupChatQueryService {
    @Autowired
    private GroupChatMemberRepository groupChatMemberRepository;
    @Autowired
    private GroupChatInformationRepository groupChatInformationRepository;
    @Autowired
    private GroupChatMessageRepository groupChatMessageRepository;
    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(GroupChatQueryService.class);

    // 查询用户所在群聊
    public List<GroupChatInformation> getGroupsByUserId(Long userId) {
        logger.info("查询用户所在群聊，userId={}", userId);
        List<Long> groupIds = groupChatMemberRepository.findGroupIdsByUserId(userId);
        logger.info("查到的groupIds: {}", groupIds);
        if (groupIds.isEmpty()) return Collections.emptyList();
        List<GroupChatInformation> groups = groupChatInformationRepository.findByGroupIdIn(groupIds);
        logger.info("最终查到的群聊信息: {}", groups);
        return groups;
    }

    // 查询用户所有群聊的历史消息
    public Map<Long, List<GroupChatMessageDTO>> getGroupHistoriesByUserId(Long userId) {
        List<Long> groupIds = groupChatMemberRepository.findGroupIdsByUserId(userId);
        Map<Long, List<GroupChatMessageDTO>> result = new HashMap<>();
        for (Long groupId : groupIds) {
            List<GroupChatMessageDTO> messages = groupChatMessageRepository
                .findByGroupIdOrderByMessageTimeAsc(groupId)
                .stream()
                .map(msg -> {
                    GroupChatMessageDTO dto = new GroupChatMessageDTO();
                    dto.setMessageId(msg.getMessageId());
                    dto.setGroupId(msg.getGroupId());
                    dto.setUserId(msg.getUserId());
                    // 查询用户名
                    User user = userRepository.findById(msg.getUserId()).orElse(null);
                    dto.setUserName(user != null ? user.getUsername() : null);
                    dto.setContent(msg.getContent());
                    dto.setMessageTime(msg.getMessageTime());
                    return dto;
                })
                .collect(Collectors.toList());
            result.put(groupId, messages);
        }
        return result;
    }
} 
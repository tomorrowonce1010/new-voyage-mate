package com.se_07.backend.service;

import com.se_07.backend.entity.GroupChatMessage;
import com.se_07.backend.dto.GroupChatMessageDTO;
import java.util.List;

public interface GroupChatService {
    GroupChatMessageDTO sendGroupMessage(GroupChatMessageDTO dto);
    List<GroupChatMessageDTO> getGroupMessages(Long groupId);
    GroupChatMessageDTO toDTO(GroupChatMessage msg);
} 
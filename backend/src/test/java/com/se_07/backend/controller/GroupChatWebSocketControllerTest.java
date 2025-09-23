package com.se_07.backend.chat.Controller;

import com.se_07.backend.controller.GroupChatWebSocketController;
import com.se_07.backend.dto.GroupChatMessageDTO;
import com.se_07.backend.entity.GroupChatMessage;
import com.se_07.backend.repository.GroupChatMessageRepository;
import com.se_07.backend.service.GroupChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupChatWebSocketControllerTest {
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private GroupChatService groupChatService;
    @Mock
    private GroupChatMessageRepository groupChatMessageRepository;
    @InjectMocks
    private GroupChatWebSocketController controller;

    private GroupChatMessageDTO buildDTO() {
        GroupChatMessageDTO dto = new GroupChatMessageDTO();
        dto.setGroupId(1L);
        dto.setUserId(2L);
        dto.setContent("hello");
        return dto;
    }

    @Test
    void testProcessGroupMessage_entityFound() {
        GroupChatMessageDTO input = buildDTO();
        GroupChatMessageDTO saved = new GroupChatMessageDTO();
        saved.setMessageId(10L); saved.setGroupId(1L); saved.setUserId(2L); saved.setContent("hello");
        GroupChatMessage entity = new GroupChatMessage();
        entity.setMessageId(10L); entity.setGroupId(1L); entity.setUserId(2L); entity.setContent("hello");
        GroupChatMessageDTO dto = new GroupChatMessageDTO();
        dto.setMessageId(10L); dto.setGroupId(1L); dto.setUserId(2L); dto.setContent("hello"); dto.setUserName("张三");
        when(groupChatService.sendGroupMessage(input)).thenReturn(saved);
        when(groupChatMessageRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(groupChatService.toDTO(entity)).thenReturn(dto);
        controller.processGroupMessage(input);
        verify(groupChatService, times(1)).sendGroupMessage(input);
        verify(groupChatMessageRepository, times(1)).findById(10L);
        verify(groupChatService, times(1)).toDTO(entity);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group.1"), eq(dto));
    }

    @Test
    void testProcessGroupMessage_entityNotFound() {
        GroupChatMessageDTO input = buildDTO();
        GroupChatMessageDTO saved = new GroupChatMessageDTO();
        saved.setMessageId(11L); saved.setGroupId(1L); saved.setUserId(2L); saved.setContent("hello");
        when(groupChatService.sendGroupMessage(input)).thenReturn(saved);
        when(groupChatMessageRepository.findById(11L)).thenReturn(Optional.empty());
        controller.processGroupMessage(input);
        verify(groupChatService, times(1)).sendGroupMessage(input);
        verify(groupChatMessageRepository, times(1)).findById(11L);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group.1"), eq(saved));
    }
}
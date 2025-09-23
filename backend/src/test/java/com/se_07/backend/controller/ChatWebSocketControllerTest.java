package com.se_07.backend.chat.Controller;

import com.se_07.backend.controller.ChatWebSocketController;
import com.se_07.backend.dto.ChatMessageDTO;
import com.se_07.backend.entity.ChatMessage;
import com.se_07.backend.service.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatWebSocketControllerTest {
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ChatMessageService chatMessageService;
    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

    private ChatMessageDTO buildDTO() {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setFromId(1L);
        dto.setToId(2L);
        dto.setContent("hello");
        return dto;
    }

    @Test
    void testProcessMessage() {
        ChatMessageDTO input = buildDTO();
        ChatMessage saved = new ChatMessage();
        saved.setFromId(1L); saved.setToId(2L); saved.setContent("hello");
        ChatMessageDTO output = new ChatMessageDTO();
        output.setFromId(1L); output.setToId(2L); output.setContent("hello");
        when(chatMessageService.sendMessageDTO(any(ChatMessageDTO.class))).thenReturn(saved);
        when(chatMessageService.toDTO(saved)).thenReturn(output);
        chatWebSocketController.processMessage(input);
        verify(chatMessageService, times(1)).sendMessageDTO(input);
        verify(chatMessageService, times(1)).toDTO(saved);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat.2"), eq(output));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat.1"), eq(output));
    }
}
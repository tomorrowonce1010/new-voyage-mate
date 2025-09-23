package com.se_07.backend.chat.Service;

import com.se_07.backend.dto.ChatMessageDTO;
import com.se_07.backend.service.ChatMessageService;
import com.se_07.backend.service.impl.ChatMessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ChatMessageServiceIntegrationTest {
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private ChatMessageServiceImpl chatMessageServiceImpl;

//    @Test
//    public void testSendAndQueryFriendMessage() {
//        ChatMessageDTO dto = new ChatMessageDTO();
//        dto.setFromId(1L);
//        dto.setToId(2L);
//        dto.setContent("hello");
//        dto.setMessageTime(LocalDateTime.now());
//        ChatMessageDTO saved = chatMessageService.sendMessage(dto);
//        assertNotNull(saved.getMessageId());
//        List<ChatMessageDTO> messages = chatMessageService.getMessagesBetween(1L, 2L);
//        assertTrue(messages.stream().anyMatch(m -> "hello".equals(m.getContent())));
//    }
//
//    @Test
//    public void testSendAndQueryFriendMessageImpl() {
//        ChatMessageDTO dto = new ChatMessageDTO();
//        dto.setFromId(1L);
//        dto.setToId(2L);
//        dto.setContent("hello-impl");
//        dto.setMessageTime(LocalDateTime.now());
//        ChatMessageDTO saved = chatMessageServiceImpl.sendMessage(dto);
//        assertNotNull(saved.getMessageId());
//        List<ChatMessageDTO> messages = chatMessageServiceImpl.getMessagesBetween(1L, 2L);
//        assertTrue(messages.stream().anyMatch(m -> "hello-impl".equals(m.getContent())));
//    }
} 
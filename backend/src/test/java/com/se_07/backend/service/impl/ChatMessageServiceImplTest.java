package com.se_07.backend.service.impl;

import com.se_07.backend.dto.ChatMessageDTO;
import com.se_07.backend.entity.ChatMessage;
import com.se_07.backend.entity.User;
import com.se_07.backend.repository.ChatMessageRepository;
import com.se_07.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChatMessageServiceImplTest {
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ChatMessageServiceImpl chatMessageService;

    @BeforeEach
    public void setUp() {
        // MockitoExtension自动处理
    }

    @Test
    public void testSendMessage_normal() {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setFromId(1L);
        dto.setToId(2L);
        dto.setContent("hi");
        dto.setMessageTime(LocalDateTime.now());
        ChatMessage saved = new ChatMessage();
        saved.setMessageId(10L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);
        ChatMessageDTO result = chatMessageService.sendMessage(dto);
        assertEquals(10L, result.getMessageId());
    }

    @Test
    public void testSendMessage_messageIdNull() {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setFromId(1L);
        dto.setToId(2L);
        dto.setContent("hi");
        dto.setMessageTime(LocalDateTime.now());
        ChatMessage saved = new ChatMessage();
        saved.setMessageId(null);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);
        ChatMessageDTO result = chatMessageService.sendMessage(dto);
        assertNull(result.getMessageId());
    }

    @Test
    public void testSendMessageDTO_messageTimeNull() {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setFromId(1L);
        dto.setToId(2L);
        dto.setContent("hi");
        dto.setMessageTime(null);
        ChatMessage saved = new ChatMessage();
        saved.setMessageId(11L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);
        ChatMessage result = chatMessageService.sendMessageDTO(dto);
        assertEquals(11L, result.getMessageId());
    }

    @Test
    public void testGetMessagesBetween_found() {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(1L);
        msg.setFromId(1L);
        msg.setToId(2L);
        msg.setContent("hi");
        msg.setMessageTime(LocalDateTime.now());
        when(chatMessageRepository.findByFromIdAndToId(1L, 2L)).thenReturn(Arrays.asList(msg));
        User user = new User(); user.setId(1L); user.setUsername("张三");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        List<ChatMessageDTO> result = chatMessageService.getMessagesBetween(1L, 2L);
        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).getUserName());
    }

    @Test
    public void testGetMessagesBetween_notFound() {
        when(chatMessageRepository.findByFromIdAndToId(1L, 2L)).thenReturn(Collections.emptyList());
        List<ChatMessageDTO> result = chatMessageService.getMessagesBetween(1L, 2L);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetAllMessagesForUser_found() {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(2L);
        msg.setFromId(1L);
        msg.setToId(2L);
        msg.setContent("hi");
        msg.setMessageTime(LocalDateTime.now());
        when(chatMessageRepository.findByFromIdOrToId(1L, 1L)).thenReturn(Arrays.asList(msg));
        User user = new User(); user.setId(1L); user.setUsername("张三");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        List<ChatMessageDTO> result = chatMessageService.getAllMessagesForUser(1L);
        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).getUserName());
    }

    @Test
    public void testGetAllMessagesForUser_notFound() {
        when(chatMessageRepository.findByFromIdOrToId(1L, 1L)).thenReturn(Collections.emptyList());
        List<ChatMessageDTO> result = chatMessageService.getAllMessagesForUser(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetMessagesBetweenUsers_found() {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(3L);
        msg.setFromId(1L);
        msg.setToId(2L);
        msg.setContent("hi");
        msg.setMessageTime(LocalDateTime.now());
        when(chatMessageRepository.findMessagesBetweenUsers(1L, 2L)).thenReturn(Arrays.asList(msg));
        User user = new User(); user.setId(1L); user.setUsername("张三");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        List<ChatMessageDTO> result = chatMessageService.getMessagesBetweenUsers(1L, 2L);
        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).getUserName());
    }

    @Test
    public void testGetMessagesBetweenUsers_notFound() {
        when(chatMessageRepository.findMessagesBetweenUsers(1L, 2L)).thenReturn(Collections.emptyList());
        List<ChatMessageDTO> result = chatMessageService.getMessagesBetweenUsers(1L, 2L);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToDTO_userNotFound() {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(4L);
        msg.setFromId(999L);
        msg.setToId(2L);
        msg.setContent("hi");
        msg.setMessageTime(LocalDateTime.now());
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        ChatMessageDTO dto = chatMessageService.toDTO(msg);
        assertNull(dto.getUserName());
    }

    @Test
    public void testSendMessage_contentNull() {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setFromId(1L);
        dto.setToId(2L);
        dto.setContent(null);
        dto.setMessageTime(LocalDateTime.now());
        ChatMessage saved = new ChatMessage();
        saved.setMessageId(12L);
        saved.setContent(null);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);
        ChatMessageDTO result = chatMessageService.sendMessage(dto);
        assertNull(result.getContent());
    }

    @Test
    public void testSendMessage_contentEmpty() {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setFromId(1L);
        dto.setToId(2L);
        dto.setContent("");
        dto.setMessageTime(LocalDateTime.now());
        ChatMessage saved = new ChatMessage();
        saved.setMessageId(13L);
        saved.setContent("");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);
        ChatMessageDTO result = chatMessageService.sendMessage(dto);
        assertEquals("", result.getContent());
    }


    @Test
    public void testToDTO_msgNull() {
        assertThrows(NullPointerException.class, () -> chatMessageService.toDTO(null));
    }
} 
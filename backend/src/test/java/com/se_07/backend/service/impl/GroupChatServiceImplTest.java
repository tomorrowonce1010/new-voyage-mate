package com.se_07.backend.service.impl;

import com.se_07.backend.dto.GroupChatMessageDTO;
import com.se_07.backend.entity.GroupChatMessage;
import com.se_07.backend.entity.User;
import com.se_07.backend.repository.GroupChatMessageRepository;
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
public class GroupChatServiceImplTest {
    @Mock
    private GroupChatMessageRepository groupChatMessageRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private GroupChatServiceImpl groupChatService;

    @BeforeEach
    public void setUp() {
        // MockitoExtension自动处理
    }

    @Test
    public void testSendGroupMessage() {
        GroupChatMessageDTO dto = new GroupChatMessageDTO();
        dto.setGroupId(1L);
        dto.setUserId(2L);
        dto.setContent("hello");
        LocalDateTime now = LocalDateTime.now();
        dto.setMessageTime(now);
        GroupChatMessage saved = new GroupChatMessage();
        saved.setMessageId(10L);
        saved.setGroupId(1L);
        saved.setUserId(2L);
        saved.setContent("hello");
        saved.setMessageTime(now);
        when(groupChatMessageRepository.save(any(GroupChatMessage.class))).thenReturn(saved);
        GroupChatMessageDTO result = groupChatService.sendGroupMessage(dto);
        assertEquals(10L, result.getMessageId());
        assertEquals(now, result.getMessageTime());
    }

    @Test
    public void testGetGroupMessages() {
        GroupChatMessage msg1 = new GroupChatMessage();
        msg1.setMessageId(1L);
        msg1.setGroupId(1L);
        msg1.setUserId(2L);
        msg1.setContent("hi");
        msg1.setMessageTime(LocalDateTime.now());
        GroupChatMessage msg2 = new GroupChatMessage();
        msg2.setMessageId(2L);
        msg2.setGroupId(1L);
        msg2.setUserId(3L);
        msg2.setContent("hello");
        msg2.setMessageTime(LocalDateTime.now());
        when(groupChatMessageRepository.findByGroupIdOrderByMessageTimeAsc(1L)).thenReturn(Arrays.asList(msg1, msg2));
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("张三");
        User user3 = new User();
        user3.setId(3L);
        user3.setUsername("李四");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        List<GroupChatMessageDTO> list = groupChatService.getGroupMessages(1L);
        assertEquals(2, list.size());
        assertEquals("张三", list.get(0).getUserName());
        assertEquals("李四", list.get(1).getUserName());
    }

    @Test
    public void testToDTO() {
        GroupChatMessage msg = new GroupChatMessage();
        msg.setMessageId(5L);
        msg.setGroupId(1L);
        msg.setUserId(2L);
        msg.setContent("test");
        msg.setMessageTime(LocalDateTime.now());
        User user = new User();
        user.setId(2L);
        user.setUsername("王五");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        GroupChatMessageDTO dto = groupChatService.toDTO(msg);
        assertEquals(5L, dto.getMessageId());
        assertEquals("test", dto.getContent());
        assertEquals("王五", dto.getUserName());
    }

    @Test
    public void testSendGroupMessage_messageTimeNull() {
        GroupChatMessageDTO dto = new GroupChatMessageDTO();
        dto.setGroupId(1L);
        dto.setUserId(2L);
        dto.setContent("test");
        dto.setMessageTime(null);
        GroupChatMessage saved = new GroupChatMessage();
        saved.setMessageId(1L);
        saved.setMessageTime(LocalDateTime.now());
        when(groupChatMessageRepository.save(any(GroupChatMessage.class))).thenReturn(saved);
        GroupChatMessageDTO result = groupChatService.sendGroupMessage(dto);
        assertNotNull(result.getMessageTime());
    }

    @Test
    public void testSendGroupMessage_contentNull() {
        GroupChatMessageDTO dto = new GroupChatMessageDTO();
        dto.setGroupId(1L);
        dto.setUserId(2L);
        dto.setContent(null);
        dto.setMessageTime(LocalDateTime.now());
        GroupChatMessage saved = new GroupChatMessage();
        saved.setMessageId(2L);
        saved.setContent(null);
        when(groupChatMessageRepository.save(any(GroupChatMessage.class))).thenReturn(saved);
        GroupChatMessageDTO result = groupChatService.sendGroupMessage(dto);
        assertNull(result.getContent());
    }

    @Test
    public void testSendGroupMessage_savedMessageIdNull() {
        GroupChatMessageDTO dto = new GroupChatMessageDTO();
        dto.setGroupId(1L);
        dto.setUserId(2L);
        dto.setContent("test");
        dto.setMessageTime(LocalDateTime.now());
        GroupChatMessage saved = new GroupChatMessage();
        saved.setMessageId(null);
        when(groupChatMessageRepository.save(any(GroupChatMessage.class))).thenReturn(saved);
        GroupChatMessageDTO result = groupChatService.sendGroupMessage(dto);
        assertNull(result.getMessageId());
    }

    @Test
    public void testSendGroupMessage_savedMessageTimeNull() {
        GroupChatMessageDTO dto = new GroupChatMessageDTO();
        dto.setGroupId(1L);
        dto.setUserId(2L);
        dto.setContent("test");
        dto.setMessageTime(LocalDateTime.now());
        GroupChatMessage saved = new GroupChatMessage();
        saved.setMessageId(3L);
        saved.setMessageTime(null);
        when(groupChatMessageRepository.save(any(GroupChatMessage.class))).thenReturn(saved);
        GroupChatMessageDTO result = groupChatService.sendGroupMessage(dto);
        assertNull(result.getMessageTime());
    }

    @Test
    public void testGetGroupMessages_noMessages() {
        when(groupChatMessageRepository.findByGroupIdOrderByMessageTimeAsc(999L)).thenReturn(Collections.emptyList());
        List<GroupChatMessageDTO> result = groupChatService.getGroupMessages(999L);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetGroupMessages_userNotFound() {
        GroupChatMessage msg = new GroupChatMessage();
        msg.setMessageId(1L);
        msg.setGroupId(1L);
        msg.setUserId(999L);
        msg.setContent("test");
        msg.setMessageTime(LocalDateTime.now());
        when(groupChatMessageRepository.findByGroupIdOrderByMessageTimeAsc(1L)).thenReturn(Arrays.asList(msg));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        List<GroupChatMessageDTO> result = groupChatService.getGroupMessages(1L);
        assertEquals(1, result.size());
        assertNull(result.get(0).getUserName());
    }

    @Test
    public void testGetGroupMessages_returnNullList() {
        when(groupChatMessageRepository.findByGroupIdOrderByMessageTimeAsc(888L)).thenReturn(null);
        assertThrows(NullPointerException.class, () -> groupChatService.getGroupMessages(888L));
    }

    @Test
    public void testToDTO_msgNull() {
        assertThrows(NullPointerException.class, () -> groupChatService.toDTO(null));
    }

    @Test
    public void testToDTO_userNotFound() {
        GroupChatMessage msg = new GroupChatMessage();
        msg.setMessageId(2L);
        msg.setGroupId(1L);
        msg.setUserId(888L);
        msg.setContent("test");
        msg.setMessageTime(LocalDateTime.now());
        when(userRepository.findById(888L)).thenReturn(Optional.empty());
        GroupChatMessageDTO dto = groupChatService.toDTO(msg);
        assertNull(dto.getUserName());
    }

    @Test
    public void testToDTO_fieldsNull() {
        GroupChatMessage msg = new GroupChatMessage();
        when(userRepository.findById(null)).thenReturn(Optional.empty());
        GroupChatMessageDTO dto = groupChatService.toDTO(msg);
        assertNull(dto.getMessageId());
        assertNull(dto.getGroupId());
        assertNull(dto.getUserId());
        assertNull(dto.getContent());
        assertNull(dto.getMessageTime());
        assertNull(dto.getUserName());
    }
} 
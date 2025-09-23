package com.se_07.backend.chat.Service;

import com.se_07.backend.dto.GroupChatMessageDTO;
import com.se_07.backend.entity.GroupChatInformation;
import com.se_07.backend.entity.GroupChatMessage;
import com.se_07.backend.entity.User;
import com.se_07.backend.repository.GroupChatInformationRepository;
import com.se_07.backend.repository.GroupChatMemberRepository;
import com.se_07.backend.repository.GroupChatMessageRepository;
import com.se_07.backend.repository.UserRepository;
import com.se_07.backend.service.GroupChatQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupChatQueryServiceTest {
    @Mock
    private GroupChatMemberRepository groupChatMemberRepository;
    @Mock
    private GroupChatInformationRepository groupChatInformationRepository;
    @Mock
    private GroupChatMessageRepository groupChatMessageRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private GroupChatQueryService groupChatQueryService;

    @BeforeEach
    public void setUp() {
        // MockitoExtension自动处理
    }

    // getGroupsByUserId 测试
    @Test
    public void testGetGroupsByUserId_hasGroups() {
        Long userId = 1L;
        List<Long> groupIds = Arrays.asList(10L, 20L);
        List<GroupChatInformation> groups = Arrays.asList(
                new GroupChatInformation(), new GroupChatInformation()
        );
        when(groupChatMemberRepository.findGroupIdsByUserId(userId)).thenReturn(groupIds);
        when(groupChatInformationRepository.findByGroupIdIn(groupIds)).thenReturn(groups);
        List<GroupChatInformation> result = groupChatQueryService.getGroupsByUserId(userId);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetGroupsByUserId_noGroups() {
        Long userId = 2L;
        when(groupChatMemberRepository.findGroupIdsByUserId(userId)).thenReturn(Collections.emptyList());
        List<GroupChatInformation> result = groupChatQueryService.getGroupsByUserId(userId);
        assertTrue(result.isEmpty());
    }

    // getGroupHistoriesByUserId 测试
    @Test
    public void testGetGroupHistoriesByUserId_hasMessages() {
        Long userId = 1L;
        List<Long> groupIds = Arrays.asList(10L, 20L);
        when(groupChatMemberRepository.findGroupIdsByUserId(userId)).thenReturn(groupIds);
        // group 10 有两条消息
        GroupChatMessage msg1 = new GroupChatMessage();
        msg1.setMessageId(1L); msg1.setGroupId(10L); msg1.setUserId(100L); msg1.setContent("hi"); msg1.setMessageTime(LocalDateTime.now());
        GroupChatMessage msg2 = new GroupChatMessage();
        msg2.setMessageId(2L); msg2.setGroupId(10L); msg2.setUserId(101L); msg2.setContent("hello"); msg2.setMessageTime(LocalDateTime.now());
        // group 20 无消息
        when(groupChatMessageRepository.findByGroupIdOrderByMessageTimeAsc(10L)).thenReturn(Arrays.asList(msg1, msg2));
        when(groupChatMessageRepository.findByGroupIdOrderByMessageTimeAsc(20L)).thenReturn(Collections.emptyList());
        // 用户名查找
        User user100 = new User(); user100.setId(100L); user100.setUsername("张三");
        User user101 = new User(); user101.setId(101L); user101.setUsername("李四");
        when(userRepository.findById(100L)).thenReturn(Optional.of(user100));
        when(userRepository.findById(101L)).thenReturn(Optional.of(user101));
        Map<Long, List<GroupChatMessageDTO>> result = groupChatQueryService.getGroupHistoriesByUserId(userId);
        assertEquals(2, result.size());
        assertEquals(2, result.get(10L).size());
        assertEquals(0, result.get(20L).size());
        assertEquals("张三", result.get(10L).get(0).getUserName());
        assertEquals("李四", result.get(10L).get(1).getUserName());
    }

    @Test
    public void testGetGroupHistoriesByUserId_noGroups() {
        Long userId = 2L;
        when(groupChatMemberRepository.findGroupIdsByUserId(userId)).thenReturn(Collections.emptyList());
        Map<Long, List<GroupChatMessageDTO>> result = groupChatQueryService.getGroupHistoriesByUserId(userId);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetGroupHistoriesByUserId_messageUserNotFound() {
        Long userId = 3L;
        List<Long> groupIds = Arrays.asList(30L);
        when(groupChatMemberRepository.findGroupIdsByUserId(userId)).thenReturn(groupIds);
        GroupChatMessage msg = new GroupChatMessage();
        msg.setMessageId(3L); msg.setGroupId(30L); msg.setUserId(999L); msg.setContent("test"); msg.setMessageTime(LocalDateTime.now());
        when(groupChatMessageRepository.findByGroupIdOrderByMessageTimeAsc(30L)).thenReturn(Arrays.asList(msg));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        Map<Long, List<GroupChatMessageDTO>> result = groupChatQueryService.getGroupHistoriesByUserId(userId);
        assertEquals(1, result.size());
        assertEquals(1, result.get(30L).size());
        assertNull(result.get(30L).get(0).getUserName());
    }
} 
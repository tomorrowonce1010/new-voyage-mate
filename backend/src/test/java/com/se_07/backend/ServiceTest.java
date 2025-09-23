package com.se_07.backend;

import com.se_07.backend.dto.GroupChatMessageDTO;
import com.se_07.backend.dto.UserProfileResponse;
import com.se_07.backend.dto.ChatMessageDTO;
import com.se_07.backend.entity.GroupChatInformation;
import com.se_07.backend.entity.GroupChatMember;
import com.se_07.backend.entity.GroupChatMessage;
import com.se_07.backend.entity.User;
import com.se_07.backend.repository.GroupChatInformationRepository;
import com.se_07.backend.repository.GroupChatMemberRepository;
import com.se_07.backend.repository.GroupChatMessageRepository;
import com.se_07.backend.repository.UserRepository;
import com.se_07.backend.repository.ChatMessageRepository;
import com.se_07.backend.service.impl.GroupChatManageServiceImpl;
import com.se_07.backend.service.impl.GroupChatServiceImpl;
import com.se_07.backend.service.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
public class ServiceTest {
    // Mock依赖
    @Mock
    private GroupChatMessageRepository groupChatMessageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupChatInformationRepository groupChatInformationRepository;
    @Mock
    private GroupChatMemberRepository groupChatMemberRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @InjectMocks
    private GroupChatServiceImpl groupChatService;
    @InjectMocks
    private GroupChatManageServiceImpl groupChatManageService;
    @InjectMocks
    private com.se_07.backend.service.impl.ChatMessageServiceImpl chatMessageServiceMock;

    // 集成测试依赖
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private com.se_07.backend.service.impl.ChatMessageServiceImpl chatMessageServiceImpl;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // GroupChatServiceImpl Mock测试
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

    // GroupChatManageServiceImpl Mock测试
    @Test
    public void testCreateGroup() {
        GroupChatInformation group = new GroupChatInformation();
        group.setGroupId(100L);
        group.setGroupName("测试群");
        when(groupChatInformationRepository.save(any(GroupChatInformation.class))).thenReturn(group);
        GroupChatMember savedMember = new GroupChatMember();
        when(groupChatMemberRepository.save(any(GroupChatMember.class))).thenReturn(savedMember);
        Long groupId = groupChatManageService.createGroup("测试群", 1L);
        assertEquals(100L, groupId);
        verify(groupChatInformationRepository, times(1)).save(any(GroupChatInformation.class));
        verify(groupChatMemberRepository, times(1)).save(any(GroupChatMember.class));
    }

    @Test
    public void testGetGroupMembers() {
        GroupChatMember member1 = new GroupChatMember();
        member1.setUserId(1L);
        GroupChatMember member2 = new GroupChatMember();
        member2.setUserId(2L);
        when(groupChatMemberRepository.findByGroupId(100L)).thenReturn(Arrays.asList(member1, member2));
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("张三");
        user1.setAvatarUrl("avatar1.png");
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("李四");
        user2.setAvatarUrl("avatar2.png");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        List<UserProfileResponse> members = groupChatManageService.getGroupMembers(100L);
        assertEquals(2, members.size());
        assertEquals("张三", members.get(0).getUsername());
        assertEquals("李四", members.get(1).getUsername());
    }

    @Test
    public void testAddUserToGroup() {
        GroupChatMember savedMember = new GroupChatMember();
        when(groupChatMemberRepository.save(any(GroupChatMember.class))).thenReturn(savedMember);
        groupChatManageService.addUserToGroup(100L, 2L);
        verify(groupChatMemberRepository, times(1)).save(any(GroupChatMember.class));
    }

    @Test
    public void testUpdateGroupName() {
        GroupChatInformation group = new GroupChatInformation();
        group.setGroupId(100L);
        group.setGroupName("旧群名");
        when(groupChatInformationRepository.findById(100L)).thenReturn(Optional.of(group));
        when(groupChatInformationRepository.save(any(GroupChatInformation.class))).thenReturn(group);
        groupChatManageService.updateGroupName(100L, "新群名");
        assertEquals("新群名", group.getGroupName());
        verify(groupChatInformationRepository, times(1)).save(group);
    }

    @Test
    public void testRemoveUserFromGroup() {
        GroupChatMember member = new GroupChatMember();
        member.setGroupId(100L);
        member.setUserId(2L);
        when(groupChatMemberRepository.findByGroupIdAndUserId(100L, 2L)).thenReturn(member);
        doNothing().when(groupChatMemberRepository).delete(member);
        groupChatManageService.removeUserFromGroup(100L, 2L);
        verify(groupChatMemberRepository, times(1)).delete(member);
    }

    @Test
    public void testLeaveGroup() {
        GroupChatMember member = new GroupChatMember();
        member.setGroupId(100L);
        member.setUserId(2L);
        when(groupChatMemberRepository.findByGroupIdAndUserId(100L, 2L)).thenReturn(member);
        doNothing().when(groupChatMemberRepository).delete(member);
        groupChatManageService.leaveGroup(100L, 2L);
        verify(groupChatMemberRepository, times(1)).delete(member);
    }

    @Test
    public void testCreateGroupWithMembers() {
        GroupChatInformation group = new GroupChatInformation();
        group.setGroupId(200L);
        group.setGroupName("新群");
        when(groupChatInformationRepository.save(any(GroupChatInformation.class))).thenReturn(group);
        GroupChatMember savedMember = new GroupChatMember();
        when(groupChatMemberRepository.save(any(GroupChatMember.class))).thenReturn(savedMember);
        Long groupId = groupChatManageService.createGroupWithMembers("新群", 1L, Arrays.asList(1L, 2L, 3L));
        assertEquals(200L, groupId);
        verify(groupChatMemberRepository, times(3)).save(any(GroupChatMember.class));
    }


    @Test
    public void testSendAndQueryFriendMessage() {
        // 构造消息
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setFromId(1L);
        dto.setToId(2L);
        dto.setContent("hello");
        dto.setMessageTime(LocalDateTime.now());

        // 发送消息
        ChatMessageDTO saved = chatMessageService.sendMessage(dto);
        assertNotNull(saved.getMessageId());

        // 查询消息
        List<ChatMessageDTO> messages = chatMessageService.getMessagesBetween(1L, 2L);
        assertTrue(messages.stream().anyMatch(m -> "hello".equals(m.getContent())));
    }

    @Test
    public void testSendAndQueryFriendMessageImpl() {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setFromId(1L);
        dto.setToId(2L);
        dto.setContent("hello-impl");
        dto.setMessageTime(LocalDateTime.now());

        ChatMessageDTO saved = chatMessageServiceImpl.sendMessage(dto);
        assertNotNull(saved.getMessageId());

        List<ChatMessageDTO> messages = chatMessageServiceImpl.getMessagesBetween(1L, 2L);
        assertTrue(messages.stream().anyMatch(m -> "hello-impl".equals(m.getContent())));
    }
} 
package com.se_07.backend.service.impl;

import com.se_07.backend.dto.UserProfileResponse;
import com.se_07.backend.entity.GroupChatInformation;
import com.se_07.backend.entity.GroupChatMember;
import com.se_07.backend.entity.User;
import com.se_07.backend.repository.GroupChatInformationRepository;
import com.se_07.backend.repository.GroupChatMemberRepository;
import com.se_07.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupChatManageServiceImplTest {
    @Mock
    private GroupChatInformationRepository groupChatInformationRepository;
    @Mock
    private GroupChatMemberRepository groupChatMemberRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private GroupChatManageServiceImpl groupChatManageService;

    @BeforeEach
    public void setUp() {
        // MockitoExtension自动处理
    }

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
    public void testUpdateGroupName_groupNotFound() {
        when(groupChatInformationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> groupChatManageService.updateGroupName(999L, "新群名"));
    }

    @Test
    public void testGetGroupMembers_userNotFound() {
        GroupChatMember member = new GroupChatMember();
        member.setUserId(123L);
        when(groupChatMemberRepository.findByGroupId(100L)).thenReturn(Arrays.asList(member));
        when(userRepository.findById(123L)).thenReturn(Optional.empty());
        List<UserProfileResponse> members = groupChatManageService.getGroupMembers(100L);
        assertEquals(1, members.size());
        assertNull(members.get(0).getUsername());
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
    public void testRemoveUserFromGroup_memberNotFound() {
        when(groupChatMemberRepository.findByGroupIdAndUserId(100L, 2L)).thenReturn(null);
        groupChatManageService.removeUserFromGroup(100L, 2L);
        verify(groupChatMemberRepository, never()).delete(any());
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
    public void testLeaveGroup_memberNotFound() {
        when(groupChatMemberRepository.findByGroupIdAndUserId(100L, 2L)).thenReturn(null);
        groupChatManageService.leaveGroup(100L, 2L);
        verify(groupChatMemberRepository, never()).delete(any());
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
    public void testCreateGroupWithMembers_onlyCreator() {
        GroupChatInformation group = new GroupChatInformation();
        group.setGroupId(300L);
        group.setGroupName("单人群");
        when(groupChatInformationRepository.save(any(GroupChatInformation.class))).thenReturn(group);
        GroupChatMember savedMember = new GroupChatMember();
        when(groupChatMemberRepository.save(any(GroupChatMember.class))).thenReturn(savedMember);
        Long groupId = groupChatManageService.createGroupWithMembers("单人群", 1L, Arrays.asList(1L));
        assertEquals(300L, groupId);
        verify(groupChatMemberRepository, times(1)).save(any(GroupChatMember.class));
    }

} 
package com.se_07.backend.chat.Controller;

import com.se_07.backend.controller.GroupChatManageController;
import com.se_07.backend.dto.UserProfileResponse;
import com.se_07.backend.service.GroupChatManageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class GroupChatManageControllerTest {
    @Mock
    private GroupChatManageService groupChatManageService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @InjectMocks
    private GroupChatManageController controller;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testCreateGroup() throws Exception {
        when(groupChatManageService.createGroup(anyString(), anyLong())).thenReturn(123L);
        mockMvc.perform(post("/group/create")
                .param("groupName", "test群")
                .param("creatorUserId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.groupId").value(123L));
        verify(groupChatManageService, times(1)).createGroup("test群", 1L);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group.member.1"), any(Map.class));
    }

    @Test
    void testCreateGroupWithMembers() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("groupName", "多人群");
        req.put("creatorUserId", 1);
        req.put("memberIds", Arrays.asList(2, 3));
        when(groupChatManageService.createGroupWithMembers(anyString(), anyLong(), anyList())).thenReturn(456L);
        mockMvc.perform(post("/group/createWithMembers")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.groupId").value(456L));
        verify(groupChatManageService, times(1)).createGroupWithMembers(eq("多人群"), eq(1L), anyList());
        verify(messagingTemplate, atLeast(1)).convertAndSend(startsWith("/topic/group.member."), any(Map.class));
    }

    @Test
    void testCreateGroupWithMembers_emptyMemberIds() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("groupName", "空群");
        req.put("creatorUserId", 1);
        req.put("memberIds", Collections.emptyList());
        when(groupChatManageService.createGroupWithMembers(anyString(), anyLong(), anyList())).thenReturn(789L);
        mockMvc.perform(post("/group/createWithMembers")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.groupId").value(789L));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(startsWith("/topic/group.member."), any(Map.class));
    }

    @Test
    void testCreateGroupWithMembers_memberIdsContainCreator() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("groupName", "群");
        req.put("creatorUserId", 1);
        req.put("memberIds", Arrays.asList(1, 2, 3));
        when(groupChatManageService.createGroupWithMembers(anyString(), anyLong(), anyList())).thenReturn(101L);
        mockMvc.perform(post("/group/createWithMembers")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.groupId").value(101L));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(startsWith("/topic/group.member."), any(Map.class));
    }

    @Test
    void testAddUserToGroup() throws Exception {
        doNothing().when(groupChatManageService).addUserToGroup(anyLong(), anyLong());
        when(groupChatManageService.getGroupMembers(anyLong())).thenReturn(Collections.emptyList());
        mockMvc.perform(post("/group/addUser")
                .param("groupId", "10")
                .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(groupChatManageService, times(1)).addUserToGroup(10L, 2L);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group.member.2"), any(Map.class));
    }

    @Test
    void testAddUserToGroup_getGroupMembersThrows() throws Exception {
        doNothing().when(groupChatManageService).addUserToGroup(anyLong(), anyLong());
        when(groupChatManageService.getGroupMembers(anyLong())).thenThrow(new RuntimeException("error"));
        mockMvc.perform(post("/group/addUser")
                .param("groupId", "10")
                .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group.member.2"), any(Map.class));
    }

    @Test
    void testUpdateGroupName() throws Exception {
        doNothing().when(groupChatManageService).updateGroupName(anyLong(), anyString());
        mockMvc.perform(post("/group/updateName")
                .param("groupId", "10")
                .param("newName", "新群名"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(groupChatManageService, times(1)).updateGroupName(10L, "新群名");
    }

    @Test
    void testGetGroupMembers() throws Exception {
        UserProfileResponse user = new UserProfileResponse();
        user.setId(1L); user.setUsername("张三");
        when(groupChatManageService.getGroupMembers(10L)).thenReturn(Collections.singletonList(user));
        mockMvc.perform(get("/group/members")
                .param("groupId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("张三"));
        verify(groupChatManageService, times(1)).getGroupMembers(10L);
    }

    @Test
    void testRemoveUserFromGroup() throws Exception {
        doNothing().when(groupChatManageService).removeUserFromGroup(anyLong(), anyLong());
        mockMvc.perform(post("/group/removeUser")
                .param("groupId", "10")
                .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(groupChatManageService, times(1)).removeUserFromGroup(10L, 2L);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/group.member.2"), any(Map.class));
    }

    @Test
    void testLeaveGroup() throws Exception {
        doNothing().when(groupChatManageService).leaveGroup(anyLong(), anyLong());
        mockMvc.perform(post("/group/leave")
                .param("groupId", "10")
                .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(groupChatManageService, times(1)).leaveGroup(10L, 2L);
    }

    @Test
    void testCreateGroup_missingParam() throws Exception {
        mockMvc.perform(post("/group/create")
                .param("groupName", "test群"))
                .andExpect(status().isBadRequest());
    }
}
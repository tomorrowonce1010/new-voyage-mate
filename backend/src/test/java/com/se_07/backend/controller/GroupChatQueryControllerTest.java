package com.se_07.backend.chat.Controller;

import com.se_07.backend.controller.GroupChatQueryController;
import com.se_07.backend.dto.GroupChatMessageDTO;
import com.se_07.backend.entity.GroupChatInformation;
import com.se_07.backend.service.GroupChatQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class GroupChatQueryControllerTest {
    @Mock
    private GroupChatQueryService groupChatQueryService;
    @InjectMocks
    private GroupChatQueryController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testGetGroupsByUser() throws Exception {
        GroupChatInformation info = new GroupChatInformation();
        info.setGroupId(1L); info.setGroupName("群聊1");
        when(groupChatQueryService.getGroupsByUserId(1L)).thenReturn(Collections.singletonList(info));
        mockMvc.perform(post("/group/listByUser")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(1L))
                .andExpect(jsonPath("$[0].groupName").value("群聊1"));
        verify(groupChatQueryService, times(1)).getGroupsByUserId(1L);
    }

    @Test
    void testGetGroupHistoriesByUser() throws Exception {
        GroupChatMessageDTO msg = new GroupChatMessageDTO();
        msg.setMessageId(1L); msg.setContent("hi");
        Map<Long, List<GroupChatMessageDTO>> map = new HashMap<>();
        map.put(1L, Collections.singletonList(msg));
        when(groupChatQueryService.getGroupHistoriesByUserId(1L)).thenReturn(map);
        mockMvc.perform(get("/group/historyByUser")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['1'][0].messageId").value(1L))
                .andExpect(jsonPath("$['1'][0].content").value("hi"));
        verify(groupChatQueryService, times(1)).getGroupHistoriesByUserId(1L);
    }

    @Test
    void testGetGroupsByUser_empty() throws Exception {
        when(groupChatQueryService.getGroupsByUserId(anyLong())).thenReturn(Collections.emptyList());
        mockMvc.perform(post("/group/listByUser")
                .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void testGetGroupHistoriesByUser_empty() throws Exception {
        when(groupChatQueryService.getGroupHistoriesByUserId(anyLong())).thenReturn(Collections.emptyMap());
        mockMvc.perform(get("/group/historyByUser")
                .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @Test
    void testGetGroupsByUser_missingParam() throws Exception {
        mockMvc.perform(post("/group/listByUser"))
                .andExpect(status().isBadRequest());
    }
}
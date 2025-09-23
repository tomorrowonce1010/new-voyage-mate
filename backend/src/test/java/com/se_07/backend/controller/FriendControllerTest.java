package com.se_07.backend.chat.Controller;

import com.se_07.backend.controller.FriendController;
import com.se_07.backend.service.FriendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class FriendControllerTest {
    @Mock
    private FriendService friendService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @InjectMocks
    private FriendController friendController;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(friendController).build();
    }

    @Test
    void testAddFriend() throws Exception {
        mockMvc.perform(post("/friends/add")
                .param("userId", "1")
                .param("friendId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(friendService, times(1)).addFriend(1L, 2L);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/friend.1"), any(Map.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/friend.2"), any(Map.class));
    }

    @Test
    void testDeleteFriend() throws Exception {
        mockMvc.perform(post("/friends/delete")
                .param("userId", "1")
                .param("friendId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(friendService, times(1)).deleteFriend(1L, 2L);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/friend.1"), any(Map.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/friend.2"), any(Map.class));
    }
} 
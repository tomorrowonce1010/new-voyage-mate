package com.se_07.backend.chat.Controller;

import com.se_07.backend.controller.ChatController;
import com.se_07.backend.dto.ChatMessageDTO;
import com.se_07.backend.service.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class ChatControllerTest {
    @Mock
    private ChatMessageService chatMessageService;
    @InjectMocks
    private ChatController chatController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
    }

    @Test
    void testSendMessage() throws Exception {
        ChatMessageDTO req = new ChatMessageDTO();
        req.setFromId(1L); req.setToId(2L); req.setContent("hi");
        ChatMessageDTO resp = new ChatMessageDTO();
        resp.setMessageId(10L); resp.setFromId(1L); resp.setToId(2L); resp.setContent("hi");
        when(chatMessageService.sendMessage(any(ChatMessageDTO.class))).thenReturn(resp);
        mockMvc.perform(post("/chat/send")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(10L))
                .andExpect(jsonPath("$.content").value("hi"));
        verify(chatMessageService, times(1)).sendMessage(any(ChatMessageDTO.class));
    }

    @Test
    void testGetMessagesBetween() throws Exception {
        ChatMessageDTO msg = new ChatMessageDTO();
        msg.setMessageId(1L); msg.setFromId(1L); msg.setToId(2L); msg.setContent("hello");
        when(chatMessageService.getMessagesBetweenUsers(1L, 2L)).thenReturn(Arrays.asList(msg));
        mockMvc.perform(post("/chat/history")
                .param("userId1", "1")
                .param("userId2", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("hello"));
        verify(chatMessageService, times(1)).getMessagesBetweenUsers(1L, 2L);
    }

    @Test
    void testGetAllMessagesForUser() throws Exception {
        ChatMessageDTO msg = new ChatMessageDTO();
        msg.setMessageId(2L); msg.setFromId(1L); msg.setToId(3L); msg.setContent("all");
        when(chatMessageService.getAllMessagesForUser(1L)).thenReturn(Collections.singletonList(msg));
        mockMvc.perform(get("/chat/all")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("all"));
        verify(chatMessageService, times(1)).getAllMessagesForUser(1L);
    }
}
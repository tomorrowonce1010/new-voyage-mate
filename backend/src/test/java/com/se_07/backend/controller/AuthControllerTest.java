package com.se_07.backend.controller;

import com.se_07.backend.dto.AuthResponse;
import com.se_07.backend.dto.ChatMessageDTO;
import com.se_07.backend.dto.LoginRequest;
import com.se_07.backend.dto.RegisterRequest;
import com.se_07.backend.service.AuthService;
import com.se_07.backend.service.ChatMessageService;
import com.se_07.backend.service.FriendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthService authService;
    
    @Mock
    private FriendService friendService;
    
    @Mock
    private ChatMessageService chatMessageService;
    
    @InjectMocks
    private AuthController authController;
    
    private MockHttpSession mockSession;
    
    @BeforeEach
    void setUp() {
        mockSession = new MockHttpSession();
    }

    @Test
    void testRegister_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        
        AuthResponse expectedResponse = new AuthResponse();
        expectedResponse.setSuccess(true);
        expectedResponse.setMessage("注册成功");
        expectedResponse.setUserId(1L);
        expectedResponse.setUsername("testuser");
        expectedResponse.setEmail("test@example.com");
        
        when(authService.register(any(RegisterRequest.class), any(HttpSession.class)))
                .thenReturn(expectedResponse);
        
        // Act
        ResponseEntity<AuthResponse> response = authController.register(request, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("注册成功", response.getBody().getMessage());
        assertEquals(1L, response.getBody().getUserId());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals("test@example.com", response.getBody().getEmail());
        
        verify(authService, times(1)).register(request, mockSession);
    }

    @Test
    void testRegister_Failure() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        
        AuthResponse expectedResponse = new AuthResponse();
        expectedResponse.setSuccess(false);
        expectedResponse.setMessage("邮箱已存在");
        
        when(authService.register(any(RegisterRequest.class), any(HttpSession.class)))
                .thenReturn(expectedResponse);
        
        // Act
        ResponseEntity<AuthResponse> response = authController.register(request, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("邮箱已存在", response.getBody().getMessage());
        
        verify(authService, times(1)).register(request, mockSession);
    }

    @Test
    void testLogin_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        
        AuthResponse expectedResponse = new AuthResponse();
        expectedResponse.setSuccess(true);
        expectedResponse.setMessage("登录成功");
        expectedResponse.setUserId(1L);
        expectedResponse.setUsername("testuser");
        expectedResponse.setEmail("test@example.com");
        
        when(authService.login(any(LoginRequest.class), any(HttpSession.class)))
                .thenReturn(expectedResponse);
        
        // Act
        ResponseEntity<AuthResponse> response = authController.login(request, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("登录成功", response.getBody().getMessage());
        assertEquals(1L, response.getBody().getUserId());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals("test@example.com", response.getBody().getEmail());
        
        verify(authService, times(1)).login(request, mockSession);
    }

    @Test
    void testLogin_Failure() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");
        
        AuthResponse expectedResponse = new AuthResponse();
        expectedResponse.setSuccess(false);
        expectedResponse.setMessage("邮箱或密码错误");
        
        when(authService.login(any(LoginRequest.class), any(HttpSession.class)))
                .thenReturn(expectedResponse);
        
        // Act
        ResponseEntity<AuthResponse> response = authController.login(request, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("邮箱或密码错误", response.getBody().getMessage());
        
        verify(authService, times(1)).login(request, mockSession);
    }

    @Test
    void testLogout() {
        // Arrange
        doNothing().when(authService).logout(any(HttpSession.class));
        
        // Act
        ResponseEntity<Void> response = authController.logout(mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(authService, times(1)).logout(mockSession);
    }

    @Test
    void testCheckLoginStatus_LoggedIn() {
        // Arrange
        AuthResponse expectedResponse = new AuthResponse();
        expectedResponse.setSuccess(true);
        expectedResponse.setMessage("已登录");
        expectedResponse.setUserId(1L);
        expectedResponse.setUsername("testuser");
        expectedResponse.setEmail("test@example.com");
        
        when(authService.checkLoginStatus(any(HttpSession.class)))
                .thenReturn(expectedResponse);
        
        // Act
        ResponseEntity<AuthResponse> response = authController.checkLoginStatus(mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("已登录", response.getBody().getMessage());
        assertEquals(1L, response.getBody().getUserId());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals("test@example.com", response.getBody().getEmail());
        
        verify(authService, times(1)).checkLoginStatus(mockSession);
    }

    @Test
    void testCheckLoginStatus_NotLoggedIn() {
        // Arrange
        AuthResponse expectedResponse = new AuthResponse();
        expectedResponse.setSuccess(false);
        expectedResponse.setMessage("未登录");
        
        when(authService.checkLoginStatus(any(HttpSession.class)))
                .thenReturn(expectedResponse);
        
        // Act
        ResponseEntity<AuthResponse> response = authController.checkLoginStatus(mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("未登录", response.getBody().getMessage());
        
        verify(authService, times(1)).checkLoginStatus(mockSession);
    }

    @Test
    void testAddFriend_Success() {
        // Arrange
        Long userId = 1L;
        Long friendId = 2L;
        doNothing().when(friendService).addFriend(userId, friendId);
        
        // Act
        ResponseEntity<Map<String, Object>> response = authController.addFriend(userId, friendId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("添加好友成功", response.getBody().get("message"));
        
        verify(friendService, times(1)).addFriend(userId, friendId);
    }

    @Test
    void testAddFriend_NullUserId() {
        // Arrange
        Long userId = null;
        Long friendId = 2L;
        
        // Act
        ResponseEntity<Map<String, Object>> response = authController.addFriend(userId, friendId);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("userId和friendId不能为空", response.getBody().get("message"));
        
        verify(friendService, never()).addFriend(any(), any());
    }

    @Test
    void testAddFriend_NullFriendId() {
        // Arrange
        Long userId = 1L;
        Long friendId = null;
        
        // Act
        ResponseEntity<Map<String, Object>> response = authController.addFriend(userId, friendId);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("userId和friendId不能为空", response.getBody().get("message"));
        
        verify(friendService, never()).addFriend(any(), any());
    }

    @Test
    void testAddFriend_SameUser() {
        // Arrange
        Long userId = 1L;
        Long friendId = 1L;
        
        // Act
        ResponseEntity<Map<String, Object>> response = authController.addFriend(userId, friendId);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("不能添加自己为好友", response.getBody().get("message"));
        
        verify(friendService, never()).addFriend(any(), any());
    }

    @Test
    void testGetAllFriends_Success() {
        // Arrange
        Long userId = 1L;
        List<Long> expectedFriends = Arrays.asList(2L, 3L, 4L);
        when(friendService.getAllFriends(userId)).thenReturn(expectedFriends);
        
        // Act
        ResponseEntity<Map<String, Object>> response = authController.getAllFriends(userId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(expectedFriends, response.getBody().get("friends"));
        
        verify(friendService, times(1)).getAllFriends(userId);
    }

    @Test
    void testGetAllFriends_NullUserId() {
        // Arrange
        Long userId = null;
        
        // Act
        ResponseEntity<Map<String, Object>> response = authController.getAllFriends(userId);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("userId不能为空", response.getBody().get("message"));
        
        verify(friendService, never()).getAllFriends(any());
    }

    @Test
    void testGetAllFriends_EmptyList() {
        // Arrange
        Long userId = 1L;
        List<Long> expectedFriends = Arrays.asList();
        when(friendService.getAllFriends(userId)).thenReturn(expectedFriends);
        
        // Act
        ResponseEntity<Map<String, Object>> response = authController.getAllFriends(userId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(expectedFriends, response.getBody().get("friends"));
        
        verify(friendService, times(1)).getAllFriends(userId);
    }

    @Test
    void testDeleteFriend_Success() {
        // Arrange
        Long userId = 1L;
        Long friendId = 2L;
        doNothing().when(friendService).deleteFriend(userId, friendId);
        
        // Act
        ResponseEntity<Map<String, Object>> response = authController.deleteFriend(userId, friendId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("删除好友成功", response.getBody().get("message"));
        
        verify(friendService, times(1)).deleteFriend(userId, friendId);
    }

    @Test
    void testDeleteFriend_NullUserId() {
        // Arrange
        Long userId = null;
        Long friendId = 2L;
        
        // Act
        ResponseEntity<Map<String, Object>> response = authController.deleteFriend(userId, friendId);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("userId和friendId不能为空", response.getBody().get("message"));
        
        verify(friendService, never()).deleteFriend(any(), any());
    }

    @Test
    void testDeleteFriend_NullFriendId() {
        // Arrange
        Long userId = 1L;
        Long friendId = null;
        
        // Act
        ResponseEntity<Map<String, Object>> response = authController.deleteFriend(userId, friendId);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("userId和friendId不能为空", response.getBody().get("message"));
        
        verify(friendService, never()).deleteFriend(any(), any());
    }

    @Test
    void testDeleteFriend_SameUser() {
        // Arrange
        Long userId = 1L;
        Long friendId = 1L;
        
        // Act
        ResponseEntity<Map<String, Object>> response = authController.deleteFriend(userId, friendId);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("不能删除自己", response.getBody().get("message"));
        
        verify(friendService, never()).deleteFriend(any(), any());
    }

    @Test
    void testGetChatMessagesBetweenUsers_Success() {
        // Arrange
        Long userId1 = 1L;
        Long userId2 = 2L;
        List<ChatMessageDTO> expectedMessages = Arrays.asList(
            createChatMessage(1L, userId1, userId2, "Hello"),
            createChatMessage(2L, userId2, userId1, "Hi there")
        );
        when(chatMessageService.getMessagesBetweenUsers(userId1, userId2)).thenReturn(expectedMessages);
        
        // Act
        List<ChatMessageDTO> result = authController.getChatMessagesBetweenUsers(userId1, userId2);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Hello", result.get(0).getContent());
        assertEquals("Hi there", result.get(1).getContent());
        
        verify(chatMessageService, times(1)).getMessagesBetweenUsers(userId1, userId2);
    }

    @Test
    void testGetChatMessagesBetweenUsers_EmptyList() {
        // Arrange
        Long userId1 = 1L;
        Long userId2 = 2L;
        List<ChatMessageDTO> expectedMessages = Arrays.asList();
        when(chatMessageService.getMessagesBetweenUsers(userId1, userId2)).thenReturn(expectedMessages);
        
        // Act
        List<ChatMessageDTO> result = authController.getChatMessagesBetweenUsers(userId1, userId2);
        
        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        
        verify(chatMessageService, times(1)).getMessagesBetweenUsers(userId1, userId2);
    }

    @Test
    void testGetChatMessagesBetweenUsers_SingleMessage() {
        // Arrange
        Long userId1 = 1L;
        Long userId2 = 2L;
        List<ChatMessageDTO> expectedMessages = Arrays.asList(
            createChatMessage(1L, userId1, userId2, "Single message")
        );
        when(chatMessageService.getMessagesBetweenUsers(userId1, userId2)).thenReturn(expectedMessages);
        
        // Act
        List<ChatMessageDTO> result = authController.getChatMessagesBetweenUsers(userId1, userId2);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Single message", result.get(0).getContent());
        assertEquals(userId1, result.get(0).getFromId());
        assertEquals(userId2, result.get(0).getToId());
        
        verify(chatMessageService, times(1)).getMessagesBetweenUsers(userId1, userId2);
    }

    @Test
    void testGetChatMessagesBetweenUsers_ReverseOrder() {
        // Arrange
        Long userId1 = 1L;
        Long userId2 = 2L;
        List<ChatMessageDTO> expectedMessages = Arrays.asList(
            createChatMessage(1L, userId2, userId1, "From user2"),
            createChatMessage(2L, userId1, userId2, "From user1")
        );
        when(chatMessageService.getMessagesBetweenUsers(userId2, userId1)).thenReturn(expectedMessages);
        
        // Act
        List<ChatMessageDTO> result = authController.getChatMessagesBetweenUsers(userId2, userId1);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("From user2", result.get(0).getContent());
        assertEquals("From user1", result.get(1).getContent());
        
        verify(chatMessageService, times(1)).getMessagesBetweenUsers(userId2, userId1);
    }

    // Helper method to create ChatMessageDTO
    private ChatMessageDTO createChatMessage(Long messageId, Long fromId, Long toId, String content) {
        ChatMessageDTO message = new ChatMessageDTO();
        message.setMessageId(messageId);
        message.setFromId(fromId);
        message.setToId(toId);
        message.setContent(content);
        message.setMessageTime(LocalDateTime.now());
        message.setUserName("User" + fromId);
        return message;
    }
} 
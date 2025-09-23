package com.se_07.backend.service.impl;

import com.se_07.backend.dto.AuthResponse;
import com.se_07.backend.dto.LoginRequest;
import com.se_07.backend.dto.RegisterRequest;
import com.se_07.backend.entity.User;
import com.se_07.backend.entity.UserAuth;
import com.se_07.backend.repository.UserAuthRepository;
import com.se_07.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserAuthRepository userAuthRepository;
    
    @InjectMocks
    private AuthServiceImpl authService;
    
    private MockHttpSession mockSession;
    private User testUser;
    private UserAuth testUserAuth;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    
    @BeforeEach
    void setUp() {
        mockSession = new MockHttpSession();
        
        // 设置测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        
        // 设置测试用户认证信息
        testUserAuth = new UserAuth();
        testUserAuth.setId(1L);
        testUserAuth.setUser(testUser);
        testUserAuth.setPasswordHash("$2a$10$encodedPasswordHash");
        testUserAuth.setCreatedAt(LocalDateTime.now());
        testUserAuth.setLastLogin(LocalDateTime.now());
        
        // 设置注册请求
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password123");
        
        // 设置登录请求
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(testUserAuth);
        
        // Act
        AuthResponse response = authService.register(registerRequest, mockSession);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals("注册成功", response.getMessage());
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        
        // 验证session设置
        assertEquals(1L, mockSession.getAttribute("userId"));
        assertEquals("testuser", mockSession.getAttribute("username"));
        assertEquals("test@example.com", mockSession.getAttribute("email"));
        
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userAuthRepository, times(1)).save(any(UserAuth.class));
    }

    @Test
    void testRegister_PasswordMismatch() {
        // Arrange
        registerRequest.setConfirmPassword("differentpassword");
        
        // Act
        AuthResponse response = authService.register(registerRequest, mockSession);
        
        // Assert
        assertFalse(response.isSuccess());
        assertEquals("两次输入的密码不一致", response.getMessage());
        
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void testRegister_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);
        
        // Act
        AuthResponse response = authService.register(registerRequest, mockSession);
        
        // Assert
        assertFalse(response.isSuccess());
        assertEquals("该邮箱已被注册", response.getMessage());
        
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(userRepository, never()).save(any());
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void testRegister_ExceptionDuringSave() {
        // Arrange
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));
        
        // Act
        AuthResponse response = authService.register(registerRequest, mockSession);
        
        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("注册失败："));
        assertTrue(response.getMessage().contains("Database error"));
        
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void testRegister_ExceptionDuringUserAuthSave() {
        // Arrange
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userAuthRepository.save(any(UserAuth.class))).thenThrow(new RuntimeException("Auth save error"));
        
        // Act
        AuthResponse response = authService.register(registerRequest, mockSession);
        
        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("注册失败："));
        assertTrue(response.getMessage().contains("Auth save error"));
        
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userAuthRepository, times(1)).save(any(UserAuth.class));
    }

    @Test
    void testLogin_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userAuthRepository.findByUserId(1L)).thenReturn(Optional.of(testUserAuth));
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(testUserAuth);
        
        // Mock BCryptPasswordEncoder
        BCryptPasswordEncoder mockEncoder = mock(BCryptPasswordEncoder.class);
        when(mockEncoder.matches("password123", "$2a$10$encodedPasswordHash")).thenReturn(true);
        
        // 使用反射设置passwordEncoder
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "passwordEncoder", mockEncoder);
        
        // Act
        AuthResponse response = authService.login(loginRequest, mockSession);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals("登录成功", response.getMessage());
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        
        // 验证session设置
        assertEquals(1L, mockSession.getAttribute("userId"));
        assertEquals("testuser", mockSession.getAttribute("username"));
        assertEquals("test@example.com", mockSession.getAttribute("email"));
        
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userAuthRepository, times(1)).findByUserId(1L);
        verify(userAuthRepository, times(1)).save(any(UserAuth.class));
        verify(mockEncoder, times(1)).matches("password123", "$2a$10$encodedPasswordHash");
    }

    @Test
    void testLogin_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        
        // Act
        AuthResponse response = authService.login(loginRequest, mockSession);
        
        // Assert
        assertFalse(response.isSuccess());
        assertEquals("邮箱或密码错误", response.getMessage());
        
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userAuthRepository, never()).findByUserId(any());
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void testLogin_UserAuthNotFound() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userAuthRepository.findByUserId(1L)).thenReturn(Optional.empty());
        
        // Act
        AuthResponse response = authService.login(loginRequest, mockSession);
        
        // Assert
        assertFalse(response.isSuccess());
        assertEquals("用户认证信息不存在", response.getMessage());
        
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userAuthRepository, times(1)).findByUserId(1L);
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void testLogin_WrongPassword() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userAuthRepository.findByUserId(1L)).thenReturn(Optional.of(testUserAuth));
        
        // Mock BCryptPasswordEncoder
        BCryptPasswordEncoder mockEncoder = mock(BCryptPasswordEncoder.class);
        when(mockEncoder.matches("wrongpassword", "$2a$10$encodedPasswordHash")).thenReturn(false);
        
        // 使用反射设置passwordEncoder
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "passwordEncoder", mockEncoder);
        
        loginRequest.setPassword("wrongpassword");
        
        // Act
        AuthResponse response = authService.login(loginRequest, mockSession);
        
        // Assert
        assertFalse(response.isSuccess());
        assertEquals("邮箱或密码错误", response.getMessage());
        
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userAuthRepository, times(1)).findByUserId(1L);
        verify(userAuthRepository, never()).save(any());
        verify(mockEncoder, times(1)).matches("wrongpassword", "$2a$10$encodedPasswordHash");
    }

    @Test
    void testLogin_ExceptionDuringProcess() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenThrow(new RuntimeException("Database error"));
        
        // Act
        AuthResponse response = authService.login(loginRequest, mockSession);
        
        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("登录失败："));
        assertTrue(response.getMessage().contains("Database error"));
        
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userAuthRepository, never()).findByUserId(any());
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void testLogin_ExceptionDuringSave() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userAuthRepository.findByUserId(1L)).thenReturn(Optional.of(testUserAuth));
        when(userAuthRepository.save(any(UserAuth.class))).thenThrow(new RuntimeException("Save error"));
        
        // Mock BCryptPasswordEncoder
        BCryptPasswordEncoder mockEncoder = mock(BCryptPasswordEncoder.class);
        when(mockEncoder.matches("password123", "$2a$10$encodedPasswordHash")).thenReturn(true);
        
        // 使用反射设置passwordEncoder
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "passwordEncoder", mockEncoder);
        
        // Act
        AuthResponse response = authService.login(loginRequest, mockSession);
        
        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("登录失败："));
        assertTrue(response.getMessage().contains("Save error"));
        
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userAuthRepository, times(1)).findByUserId(1L);
        verify(userAuthRepository, times(1)).save(any(UserAuth.class));
        verify(mockEncoder, times(1)).matches("password123", "$2a$10$encodedPasswordHash");
    }

    @Test
    void testLogout() {
        // Arrange
        mockSession.setAttribute("userId", 1L);
        mockSession.setAttribute("username", "testuser");
        mockSession.setAttribute("email", "test@example.com");
        // Act & Assert
        assertDoesNotThrow(() -> authService.logout(mockSession));
    }

    @Test
    void testCheckLoginStatus_LoggedIn() {
        // Arrange
        mockSession.setAttribute("userId", 1L);
        mockSession.setAttribute("username", "testuser");
        mockSession.setAttribute("email", "test@example.com");
        
        // Act
        AuthResponse response = authService.checkLoginStatus(mockSession);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals("已登录", response.getMessage());
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void testCheckLoginStatus_NotLoggedIn() {
        // Arrange
        // 不设置任何session属性
        
        // Act
        AuthResponse response = authService.checkLoginStatus(mockSession);
        
        // Assert
        assertFalse(response.isSuccess());
        assertEquals("未登录", response.getMessage());
        assertNull(response.getUserId());
        assertNull(response.getUsername());
        assertNull(response.getEmail());
    }

    @Test
    void testCheckLoginStatus_PartialSessionData() {
        // Arrange
        mockSession.setAttribute("userId", 1L);
        // 不设置username和email
        
        // Act
        AuthResponse response = authService.checkLoginStatus(mockSession);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals("已登录", response.getMessage());
        assertEquals(1L, response.getUserId());
        assertNull(response.getUsername());
        assertNull(response.getEmail());
    }

    @Test
    void testRegister_WithEmptyFields() {
        // Arrange
        RegisterRequest emptyRequest = new RegisterRequest();
        emptyRequest.setUsername("");
        emptyRequest.setEmail("");
        emptyRequest.setPassword("");
        emptyRequest.setConfirmPassword("");
        
        when(userRepository.existsByEmail("")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(testUserAuth);
        
        // Act
        AuthResponse response = authService.register(emptyRequest, mockSession);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals("注册成功", response.getMessage());
        
        verify(userRepository, times(1)).existsByEmail("");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userAuthRepository, times(1)).save(any(UserAuth.class));
    }

    @Test
    void testLogin_WithEmptyFields() {
        // Arrange
        LoginRequest emptyRequest = new LoginRequest();
        emptyRequest.setEmail("");
        emptyRequest.setPassword("");
        
        when(userRepository.findByEmail("")).thenReturn(Optional.empty());
        
        // Act
        AuthResponse response = authService.login(emptyRequest, mockSession);
        
        // Assert
        assertFalse(response.isSuccess());
        assertEquals("邮箱或密码错误", response.getMessage());
        
        verify(userRepository, times(1)).findByEmail("");
        verify(userAuthRepository, never()).findByUserId(any());
    }
} 
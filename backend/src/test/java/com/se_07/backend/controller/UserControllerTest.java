package com.se_07.backend.controller;

import com.se_07.backend.dto.*;
import com.se_07.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private MockHttpSession mockSession;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockSession = new MockHttpSession();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // 用户档案相关测试
    @Test
    @DisplayName("测试获取用户档案 - 成功")
    public void testGetUserProfile_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备测试数据
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(1L);
        profile.setUsername("testuser");
        profile.setEmail("test@example.com");
        
        // Mock服务层
        when(userService.getUserProfile(1L)).thenReturn(profile);
        
        // 执行请求
        mockMvc.perform(get("/users/profile")
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
        
        // 验证服务层调用
        verify(userService, times(1)).getUserProfile(1L);
    }

    @Test
    @DisplayName("测试获取用户档案 - 未授权")
    public void testGetUserProfile_Unauthorized() throws Exception {
        // 不设置session
        
        // 执行请求
        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isUnauthorized());
        
        // 验证服务层未被调用
        verify(userService, never()).getUserProfile(any());
    }

    @Test
    @DisplayName("测试更新用户档案 - 成功")
    public void testUpdateUserProfile_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备请求数据
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setUsername("newusername");
        request.setAvatarUrl("new-avatar.jpg");
        
        // 准备响应数据
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(1L);
        profile.setUsername("newusername");
        profile.setEmail("new@example.com");
        
        // Mock服务层
        when(userService.updateUserProfile(eq(1L), any(UserProfileUpdateRequest.class)))
                .thenReturn(profile);
        
        // 执行请求
        mockMvc.perform(put("/users/profile")
                .session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newusername"))
                .andExpect(jsonPath("$.email").value("new@example.com"));
        
        // 验证服务层调用
        verify(userService, times(1)).updateUserProfile(eq(1L), any(UserProfileUpdateRequest.class));
    }

    @Test
    @DisplayName("测试更新用户档案 - 未授权")
    public void testUpdateUserProfile_Unauthorized() throws Exception {
        // 不设置session
        
        // 准备请求数据
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setUsername("newusername");
        
        // 执行请求
        mockMvc.perform(put("/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        
        // 验证服务层未被调用
        verify(userService, never()).updateUserProfile(any(), any());
    }

    // 用户偏好相关测试
    @Test
    @DisplayName("测试更新用户偏好 - 成功")
    public void testUpdateUserPreferences_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备请求数据
        UserPreferencesUpdateRequest request = new UserPreferencesUpdateRequest();
        request.setTravelPreferences("{\"1\": 1, \"2\": 0}");
        request.setSpecialRequirements("需求1,需求2");
        
        // 准备响应数据
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(1L);
        profile.setUsername("testuser");
        
        // Mock服务层
        when(userService.updateUserPreferences(eq(1L), any(UserPreferencesUpdateRequest.class)))
                .thenReturn(profile);
        
        // 执行请求
        mockMvc.perform(put("/users/preferences")
                .session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        
        // 验证服务层调用
        verify(userService, times(1)).updateUserPreferences(eq(1L), any(UserPreferencesUpdateRequest.class));
    }

    @Test
    @DisplayName("测试更新用户偏好 - 未授权")
    public void testUpdateUserPreferences_Unauthorized() throws Exception {
        // 不设置session
        
        // 准备请求数据
        UserPreferencesUpdateRequest request = new UserPreferencesUpdateRequest();
        request.setTravelPreferences("{\"1\": 1}");
        
        // 执行请求
        mockMvc.perform(put("/users/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        
        // 验证服务层未被调用
        verify(userService, never()).updateUserPreferences(any(), any());
    }

    // 历史目的地相关测试
    @Test
    @DisplayName("测试获取历史目的地 - 成功")
    public void testGetHistoryDestinations_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备测试数据
        UserProfileResponse.HistoryDestinationDto dest1 = new UserProfileResponse.HistoryDestinationDto();
        dest1.setDestinationId(1L);
        dest1.setName("北京");
        
        UserProfileResponse.HistoryDestinationDto dest2 = new UserProfileResponse.HistoryDestinationDto();
        dest2.setDestinationId(2L);
        dest2.setName("上海");
        
        List<UserProfileResponse.HistoryDestinationDto> destinations = Arrays.asList(dest1, dest2);
        
        // Mock服务层
        when(userService.getHistoryDestinations(1L)).thenReturn(destinations);
        
        // 执行请求
        mockMvc.perform(get("/users/destinations/history")
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].destinationId").value(1))
                .andExpect(jsonPath("$[0].name").value("北京"))
                .andExpect(jsonPath("$[1].destinationId").value(2))
                .andExpect(jsonPath("$[1].name").value("上海"));
        
        // 验证服务层调用
        verify(userService, times(1)).getHistoryDestinations(1L);
    }

    @Test
    @DisplayName("测试获取历史目的地 - 未授权")
    public void testGetHistoryDestinations_Unauthorized() throws Exception {
        // 不设置session
        
        // 执行请求
        mockMvc.perform(get("/users/destinations/history"))
                .andExpect(status().isUnauthorized());
        
        // 验证服务层未被调用
        verify(userService, never()).getHistoryDestinations(any());
    }

    @Test
    @DisplayName("测试添加历史目的地 - 成功")
    public void testAddHistoryDestination_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备请求数据
        AddHistoryDestinationRequest request = new AddHistoryDestinationRequest();
        request.setName("北京");
        request.setDescription("首都");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(3));
        
        // Mock服务层
        doNothing().when(userService).addHistoryDestination(eq(1L), any(AddHistoryDestinationRequest.class));
        
        // 执行请求
        mockMvc.perform(post("/users/destinations/history")
                .session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("历史目的地添加成功"));
        
        // 验证服务层调用
        verify(userService, times(1)).addHistoryDestination(eq(1L), any(AddHistoryDestinationRequest.class));
    }

    @Test
    @DisplayName("测试添加历史目的地 - 未授权")
    public void testAddHistoryDestination_Unauthorized() throws Exception {
        // 不设置session
        
        // 准备请求数据
        AddHistoryDestinationRequest request = new AddHistoryDestinationRequest();
        request.setName("北京");
        request.setDescription("首都");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(3));
        
        // 执行请求
        mockMvc.perform(post("/users/destinations/history")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        
        // 验证服务层未被调用
        verify(userService, never()).addHistoryDestination(any(), any());
    }

    @Test
    @DisplayName("测试添加历史目的地 - 服务异常")
    public void testAddHistoryDestination_ServiceException() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备请求数据
        AddHistoryDestinationRequest request = new AddHistoryDestinationRequest();
        request.setName("北京");
        request.setDescription("首都");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(3));
        
        // Mock服务层抛出异常
        doThrow(new RuntimeException("目的地不存在")).when(userService)
                .addHistoryDestination(eq(1L), any(AddHistoryDestinationRequest.class));
        
        // 执行请求
        mockMvc.perform(post("/users/destinations/history")
                .session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("目的地不存在"));
        
        // 验证服务层调用
        verify(userService, times(1)).addHistoryDestination(eq(1L), any(AddHistoryDestinationRequest.class));
    }

    // 期望目的地相关测试
    @Test
    @DisplayName("测试添加期望目的地 - 成功")
    public void testAddWishlistDestination_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备请求数据
        AddWishlistDestinationRequest request = new AddWishlistDestinationRequest();
        request.setName("北京");
        request.setDescription("首都");
        
        // Mock服务层
        doNothing().when(userService).addWishlistDestination(eq(1L), any(AddWishlistDestinationRequest.class));
        
        // 执行请求
        mockMvc.perform(post("/users/destinations/wishlist")
                .session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("期望目的地添加成功"));
        
        // 验证服务层调用
        verify(userService, times(1)).addWishlistDestination(eq(1L), any(AddWishlistDestinationRequest.class));
    }

    @Test
    @DisplayName("测试添加期望目的地 - 未授权")
    public void testAddWishlistDestination_Unauthorized() throws Exception {
        // 不设置session
        
        // 准备请求数据
        AddWishlistDestinationRequest request = new AddWishlistDestinationRequest();
        request.setName("北京");
        request.setDescription("首都");
        
        // 执行请求
        mockMvc.perform(post("/users/destinations/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        
        // 验证服务层未被调用
        verify(userService, never()).addWishlistDestination(any(), any());
    }

    @Test
    @DisplayName("测试添加期望目的地 - 服务异常")
    public void testAddWishlistDestination_ServiceException() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备请求数据
        AddWishlistDestinationRequest request = new AddWishlistDestinationRequest();
        request.setName("北京");
        request.setDescription("首都");
        
        // Mock服务层抛出异常
        doThrow(new RuntimeException("目的地不存在")).when(userService)
                .addWishlistDestination(eq(1L), any(AddWishlistDestinationRequest.class));
        
        // 执行请求
        mockMvc.perform(post("/users/destinations/wishlist")
                .session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("目的地不存在"));
        
        // 验证服务层调用
        verify(userService, times(1)).addWishlistDestination(eq(1L), any(AddWishlistDestinationRequest.class));
    }

    // 自动添加历史目的地测试
    @Test
    @DisplayName("测试自动添加历史目的地 - 成功")
    public void testAddHistoryDestinationsFromCompletedItineraries_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // Mock服务层
        when(userService.addHistoryDestinationsFromCompletedItineraries(1L)).thenReturn(3);
        
        // 执行请求
        mockMvc.perform(post("/users/destinations/history/auto-add")
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.addedCount").value(3))
                .andExpect(jsonPath("$.message").value("成功从已出行的行程中添加了 3 个历史目的地"));
        
        // 验证服务层调用
        verify(userService, times(1)).addHistoryDestinationsFromCompletedItineraries(1L);
    }

    @Test
    @DisplayName("测试自动添加历史目的地 - 未授权")
    public void testAddHistoryDestinationsFromCompletedItineraries_Unauthorized() throws Exception {
        // 不设置session
        
        // 执行请求
        mockMvc.perform(post("/users/destinations/history/auto-add"))
                .andExpect(status().isUnauthorized());
        
        // 验证服务层未被调用
        verify(userService, never()).addHistoryDestinationsFromCompletedItineraries(any());
    }

    @Test
    @DisplayName("测试自动添加历史目的地 - 服务异常")
    public void testAddHistoryDestinationsFromCompletedItineraries_ServiceException() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // Mock服务层抛出异常
        when(userService.addHistoryDestinationsFromCompletedItineraries(1L))
                .thenThrow(new RuntimeException("数据库连接失败"));
        
        // 执行请求
        mockMvc.perform(post("/users/destinations/history/auto-add")
                .session(mockSession))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.addedCount").value(0))
                .andExpect(jsonPath("$.message").value("添加历史目的地时发生错误: 数据库连接失败"));
        
        // 验证服务层调用
        verify(userService, times(1)).addHistoryDestinationsFromCompletedItineraries(1L);
    }

    // 删除期望目的地测试
    @Test
    @DisplayName("测试删除期望目的地 - 成功")
    public void testRemoveWishlistDestination_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // Mock服务层
        doNothing().when(userService).removeWishlistDestination(eq(1L), eq(2L));
        
        // 执行请求
        mockMvc.perform(delete("/users/destinations/wishlist/2")
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("期望目的地删除成功"));
        
        // 验证服务层调用
        verify(userService, times(1)).removeWishlistDestination(1L, 2L);
    }

    @Test
    @DisplayName("测试删除期望目的地 - 未授权")
    public void testRemoveWishlistDestination_Unauthorized() throws Exception {
        // 不设置session
        
        // 执行请求
        mockMvc.perform(delete("/users/destinations/wishlist/2"))
                .andExpect(status().isUnauthorized());
        
        // 验证服务层未被调用
        verify(userService, never()).removeWishlistDestination(any(), any());
    }

    @Test
    @DisplayName("测试删除期望目的地 - 服务异常")
    public void testRemoveWishlistDestination_ServiceException() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // Mock服务层抛出异常
        doThrow(new RuntimeException("目的地不存在")).when(userService).removeWishlistDestination(eq(1L), eq(2L));
        
        // 执行请求
        mockMvc.perform(delete("/users/destinations/wishlist/2")
                .session(mockSession))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("删除失败: 目的地不存在"));
        
        // 验证服务层调用
        verify(userService, times(1)).removeWishlistDestination(1L, 2L);
    }

    // 删除自动添加的历史目的地测试
    @Test
    @DisplayName("测试删除自动添加的历史目的地 - 成功")
    public void testRemoveAutoAddedHistoryDestinationsFromItinerary_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // Mock服务层
        when(userService.removeAutoAddedHistoryDestinationsFromItinerary(eq(1L), eq(3L))).thenReturn(2);
        
        // 执行请求
        mockMvc.perform(delete("/users/destinations/history/auto-remove/3")
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.removedCount").value(2))
                .andExpect(jsonPath("$.message").value("成功删除了 2 个自动添加的历史目的地"));
        
        // 验证服务层调用
        verify(userService, times(1)).removeAutoAddedHistoryDestinationsFromItinerary(1L, 3L);
    }

    @Test
    @DisplayName("测试删除自动添加的历史目的地 - 未授权")
    public void testRemoveAutoAddedHistoryDestinationsFromItinerary_Unauthorized() throws Exception {
        // 不设置session
        
        // 执行请求
        mockMvc.perform(delete("/users/destinations/history/auto-remove/3"))
                .andExpect(status().isUnauthorized());
        
        // 验证服务层未被调用
        verify(userService, never()).removeAutoAddedHistoryDestinationsFromItinerary(any(), any());
    }

    @Test
    @DisplayName("测试删除自动添加的历史目的地 - 服务异常")
    public void testRemoveAutoAddedHistoryDestinationsFromItinerary_ServiceException() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // Mock服务层抛出异常
        when(userService.removeAutoAddedHistoryDestinationsFromItinerary(eq(1L), eq(3L)))
                .thenThrow(new RuntimeException("行程不存在"));
        
        // 执行请求
        mockMvc.perform(delete("/users/destinations/history/auto-remove/3")
                .session(mockSession))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.removedCount").value(0))
                .andExpect(jsonPath("$.message").value("删除历史目的地时发生错误: 行程不存在"));
        
        // 验证服务层调用
        verify(userService, times(1)).removeAutoAddedHistoryDestinationsFromItinerary(1L, 3L);
    }

    // 头像上传测试
    @Test
    @DisplayName("测试上传头像 - 成功")
    public void testUploadAvatar_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备测试文件
        MockMultipartFile file = new MockMultipartFile(
            "avatar", 
            "test.jpg", 
            "image/jpeg", 
            "test image content".getBytes()
        );
        
        // Mock服务层
        when(userService.uploadAvatar(eq(1L), any(MockMultipartFile.class)))
                .thenReturn("/uploads/avatars/test.jpg");
        
        // 执行请求
        mockMvc.perform(multipart("/users/avatar")
                .file(file)
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("头像上传成功"))
                .andExpect(jsonPath("$.avatarUrl").value("/uploads/avatars/test.jpg"));
        
        // 验证服务层调用
        verify(userService, times(1)).uploadAvatar(eq(1L), any(MockMultipartFile.class));
    }

    @Test
    @DisplayName("测试上传头像 - 未授权")
    public void testUploadAvatar_Unauthorized() throws Exception {
        // 不设置session
        
        // 准备测试文件
        MockMultipartFile file = new MockMultipartFile(
            "avatar", 
            "test.jpg", 
            "image/jpeg", 
            "test image content".getBytes()
        );
        
        // 执行请求
        mockMvc.perform(multipart("/users/avatar")
                .file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户未登录"));
        
        // 验证服务层未被调用
        verify(userService, never()).uploadAvatar(any(), any());
    }

    @Test
    @DisplayName("测试上传头像 - 空文件")
    public void testUploadAvatar_EmptyFile() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备空文件
        MockMultipartFile file = new MockMultipartFile(
            "avatar", 
            "test.jpg", 
            "image/jpeg", 
            new byte[0]
        );
        
        // Mock服务层抛出异常
        when(userService.uploadAvatar(eq(1L), any(MockMultipartFile.class)))
                .thenThrow(new RuntimeException("文件不能为空"));
        
        // 执行请求
        mockMvc.perform(multipart("/users/avatar")
                .file(file)
                .session(mockSession))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("头像上传失败: 文件不能为空"));
        
        // 验证服务层调用
        verify(userService, times(1)).uploadAvatar(eq(1L), any(MockMultipartFile.class));
    }

    @Test
    @DisplayName("测试上传头像 - 服务异常")
    public void testUploadAvatar_ServiceException() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备测试文件
        MockMultipartFile file = new MockMultipartFile(
            "avatar", 
            "test.jpg", 
            "image/jpeg", 
            "test image content".getBytes()
        );
        
        // Mock服务层抛出异常
        when(userService.uploadAvatar(eq(1L), any(MockMultipartFile.class)))
                .thenThrow(new RuntimeException("文件格式不支持"));
        
        // 执行请求
        mockMvc.perform(multipart("/users/avatar")
                .file(file)
                .session(mockSession))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("头像上传失败: 文件格式不支持"));
        
        // 验证服务层调用
        verify(userService, times(1)).uploadAvatar(eq(1L), any(MockMultipartFile.class));
    }

    // 旅行统计测试
    @Test
    @DisplayName("测试获取旅行统计 - 成功")
    public void testGetTravelStats_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备测试数据
        TravelStatsResponse stats = new TravelStatsResponse();
        stats.setTotalItineraries(5);
        stats.setTotalDestinations(10);
        stats.setTotalDays(50);
        
        // Mock服务层
        when(userService.getTravelStats(1L)).thenReturn(stats);
        
        // 执行请求
        mockMvc.perform(get("/users/travel-stats")
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItineraries").value(5))
                .andExpect(jsonPath("$.totalDestinations").value(10))
                .andExpect(jsonPath("$.totalDays").value(50));
        
        // 验证服务层调用
        verify(userService, times(1)).getTravelStats(1L);
    }

    @Test
    @DisplayName("测试获取旅行统计 - 未授权")
    public void testGetTravelStats_Unauthorized() throws Exception {
        // 不设置session
        
        // 执行请求
        mockMvc.perform(get("/users/travel-stats"))
                .andExpect(status().isUnauthorized());
        
        // 验证服务层未被调用
        verify(userService, never()).getTravelStats(any());
    }

    @Test
    @DisplayName("测试获取旅行统计 - 服务异常")
    public void testGetTravelStats_ServiceException() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // Mock服务层抛出异常
        when(userService.getTravelStats(1L)).thenThrow(new RuntimeException("数据库连接失败"));
        
        // 执行请求
        mockMvc.perform(get("/users/travel-stats")
                .session(mockSession))
                .andExpect(status().isInternalServerError());
        
        // 验证服务层调用
        verify(userService, times(1)).getTravelStats(1L);
    }

    // 用户主页测试
    @Test
    @DisplayName("测试获取用户主页 - 成功")
    public void testGetUserHomepage_Success() throws Exception {
        // 准备测试数据
        UserHomepageResponse homepage = new UserHomepageResponse();
        homepage.setId(2L);
        homepage.setUsername("otheruser");
        homepage.setPublicItineraries(Arrays.asList());
        
        // Mock服务层
        when(userService.getUserHomepage(eq(2L), anyString())).thenReturn(homepage);
        
        // 执行请求
        mockMvc.perform(get("/users/homepage/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("otheruser"));
        
        // 验证服务层调用
        verify(userService, times(1)).getUserHomepage(eq(2L), anyString());
    }

    @Test
    @DisplayName("测试获取用户主页 - 服务异常")
    public void testGetUserHomepage_ServiceException() throws Exception {
        // Mock服务层抛出异常
        when(userService.getUserHomepage(eq(999L), anyString()))
                .thenThrow(new RuntimeException("用户不存在"));
        
        // 执行请求
        mockMvc.perform(get("/users/homepage/999"))
                .andExpect(status().isNotFound());
        
        // 验证服务层调用
        verify(userService, times(1)).getUserHomepage(eq(999L), anyString());
    }

    // 用户搜索测试
    @Test
    @DisplayName("测试搜索用户 - 成功")
    public void testSearchUsers_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备测试数据
        Map<String, Object> user1 = new HashMap<>();
        user1.put("id", 2L);
        user1.put("username", "张三");
        user1.put("avatar", "张");
        
        Map<String, Object> user2 = new HashMap<>();
        user2.put("id", 3L);
        user2.put("username", "李四");
        user2.put("avatar", "李");
        
        List<Map<String, Object>> searchResults = Arrays.asList(user1, user2);
        
        // Mock服务层
        when(userService.searchUsers("张", 1L)).thenReturn(searchResults);
        
        // 执行请求
        mockMvc.perform(get("/users/search")
                .param("username", "张")
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].username").value("张三"))
                .andExpect(jsonPath("$[0].avatar").value("张"))
                .andExpect(jsonPath("$[1].id").value(3))
                .andExpect(jsonPath("$[1].username").value("李四"))
                .andExpect(jsonPath("$[1].avatar").value("李"));
        
        // 验证服务层调用
        verify(userService, times(1)).searchUsers("张", 1L);
    }

    @Test
    @DisplayName("测试搜索用户 - 过滤自己")
    public void testSearchUsers_FilterSelf() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备测试数据
        Map<String, Object> user2 = new HashMap<>();
        user2.put("id", 2L);
        user2.put("username", "李四");
        user2.put("avatar", "李");
        
        List<Map<String, Object>> searchResults = Arrays.asList(user2);
        
        // Mock服务层
        when(userService.searchUsers("张", 1L)).thenReturn(searchResults);
        
        // 执行请求
        mockMvc.perform(get("/users/search")
                .param("username", "张")
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(2)) // 只返回李四，过滤掉自己
                .andExpect(jsonPath("$[0].username").value("李四"));
        
        // 验证服务层调用
        verify(userService, times(1)).searchUsers("张", 1L);
    }

    @Test
    @DisplayName("测试搜索用户 - 用户名为空")
    public void testSearchUsers_EmptyUsername() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // Mock服务层
        when(userService.searchUsers("", 1L)).thenReturn(Arrays.asList());
        
        // 执行请求
        mockMvc.perform(get("/users/search")
                .param("username", "")
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        
        // 验证服务层调用
        verify(userService, times(1)).searchUsers("", 1L);
    }

    // 边界条件测试
    @Test
    @DisplayName("测试空请求体")
    public void testEmptyRequestBody() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 执行请求
        mockMvc.perform(put("/users/profile")
                .session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("测试无效JSON")
    public void testInvalidJson() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 执行请求
        mockMvc.perform(put("/users/profile")
                .session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    // 性能测试
    @Test
    @DisplayName("测试获取用户档案性能")
    public void testGetUserProfilePerformance() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);
        
        // 准备测试数据
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(1L);
        profile.setUsername("testuser");
        
        // Mock服务层
        when(userService.getUserProfile(1L)).thenReturn(profile);
        
        // 执行多次请求测试性能
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/users/profile")
                    .session(mockSession))
                    .andExpect(status().isOk());
        }
        long endTime = System.currentTimeMillis();
        
        // 验证平均响应时间在合理范围内（1秒内）
        long averageTime = (endTime - startTime) / 10;
        assert averageTime < 1000 : "平均响应时间过长: " + averageTime + "ms";
        
        // 验证服务层调用次数
        verify(userService, times(10)).getUserProfile(1L);
    }
} 
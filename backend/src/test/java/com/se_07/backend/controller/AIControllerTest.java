package com.se_07.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.se_07.backend.dto.AIRecommendationRequest;
import com.se_07.backend.dto.AIRecommendationResponse;
import com.se_07.backend.service.AIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AIControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AIService aiService;

    private MockHttpSession mockSession;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockSession = new MockHttpSession();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // 生成个人档案AI推荐相关测试
    @Test
    @DisplayName("测试生成个人档案AI推荐 - 成功")
    public void testGenerateProfileRecommendation_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setTravelPreferences(Arrays.asList("文化", "美食"));
        request.setSpecialNeeds(Arrays.asList("无障碍设施"));
        request.setNaturalLanguageDescription("我喜欢体验当地文化");
        request.setHistoricalDestinations(Arrays.asList("北京", "上海"));
        request.setWishlistDestinations(Arrays.asList("西安", "成都"));

        // 准备响应数据
        AIRecommendationResponse response = AIRecommendationResponse.success(
                "基于您的偏好，推荐您前往西安体验古都文化...", "general"
        );

        // Mock服务层
        when(aiService.generateProfileRecommendation(any(AIRecommendationRequest.class)))
                .thenReturn(response);

        // 执行请求
        mockMvc.perform(post("/ai/profile-recommendation")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content").value("基于您的偏好，推荐您前往西安体验古都文化..."))
                .andExpect(jsonPath("$.requestType").value("general"));

        // 验证服务层调用
        verify(aiService, times(1)).generateProfileRecommendation(any(AIRecommendationRequest.class));

        // 验证请求类型被正确设置
        verify(aiService).generateProfileRecommendation(argThat(req ->
                "general".equals(req.getRequestType())
        ));
    }

    @Test
    @DisplayName("测试生成个人档案AI推荐 - 未授权")
    public void testGenerateProfileRecommendation_Unauthorized() throws Exception {
        // 不设置session

        // 准备请求数据
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setTravelPreferences(Arrays.asList("文化", "美食"));

        // 执行请求
        mockMvc.perform(post("/ai/profile-recommendation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("用户未登录"));

        // 验证服务层未被调用
        verify(aiService, never()).generateProfileRecommendation(any());
    }

    @Test
    @DisplayName("测试生成个人档案AI推荐 - 服务异常")
    public void testGenerateProfileRecommendation_ServiceException() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setTravelPreferences(Arrays.asList("文化", "美食"));

        // Mock服务层抛出异常
        when(aiService.generateProfileRecommendation(any(AIRecommendationRequest.class)))
                .thenThrow(new RuntimeException("AI服务不可用"));

        // 执行请求
        mockMvc.perform(post("/ai/profile-recommendation")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("生成推荐失败：AI服务不可用"));

        // 验证服务层调用
        verify(aiService, times(1)).generateProfileRecommendation(any(AIRecommendationRequest.class));
    }

    @Test
    @DisplayName("测试生成个人档案AI推荐 - 空请求")
    public void testGenerateProfileRecommendation_EmptyRequest() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备空请求数据
        AIRecommendationRequest request = new AIRecommendationRequest();

        // 准备响应数据
        AIRecommendationResponse response = AIRecommendationResponse.success(
                "基于您的档案信息，为您推荐以下目的地...", "general"
        );

        // Mock服务层
        when(aiService.generateProfileRecommendation(any(AIRecommendationRequest.class)))
                .thenReturn(response);

        // 执行请求
        mockMvc.perform(post("/ai/profile-recommendation")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证服务层调用
        verify(aiService, times(1)).generateProfileRecommendation(any(AIRecommendationRequest.class));
    }

    // 生成详细行程规划相关测试
    @Test
    @DisplayName("测试生成详细行程规划 - 成功")
    public void testGenerateItineraryPlan_Success() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("西安");
        request.setDays(3);
        request.setTravelers(2);
        request.setBudget(5000.0);
        request.setTravelPreferences(Arrays.asList("文化", "美食"));
        request.setSpecialNeeds(Arrays.asList("无障碍设施"));

        // 准备响应数据
        AIRecommendationResponse response = AIRecommendationResponse.success(
                "{\"title\":\"西安3日游\",\"days\":3,\"travelers\":2,\"budget\":5000,\"plan\":[]}", "itinerary"
        );

        // Mock服务层
        when(aiService.generateItineraryPlan(any(AIRecommendationRequest.class)))
                .thenReturn(response);

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestType").value("itinerary"));

        // 验证服务层调用
        verify(aiService, times(1)).generateItineraryPlan(any(AIRecommendationRequest.class));

        // 验证请求类型被正确设置
        verify(aiService).generateItineraryPlan(argThat(req ->
                "itinerary".equals(req.getRequestType())
        ));
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 未授权")
    public void testGenerateItineraryPlan_Unauthorized() throws Exception {
        // 不设置session

        // 准备请求数据
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("西安");
        request.setDays(3);
        request.setTravelers(2);

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("用户未登录"));

        // 验证服务层未被调用
        verify(aiService, never()).generateItineraryPlan(any());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 目的地为空")
    public void testGenerateItineraryPlan_EmptyDestination() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据 - 目的地为空
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("");
        request.setDays(3);
        request.setTravelers(2);

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("目的地不能为空"));

        // 验证服务层未被调用
        verify(aiService, never()).generateItineraryPlan(any());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 目的地为null")
    public void testGenerateItineraryPlan_NullDestination() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据 - 目的地为null
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination(null);
        request.setDays(3);
        request.setTravelers(2);

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("目的地不能为空"));

        // 验证服务层未被调用
        verify(aiService, never()).generateItineraryPlan(any());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 目的地为空白字符")
    public void testGenerateItineraryPlan_BlankDestination() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据 - 目的地为空白字符
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("   ");
        request.setDays(3);
        request.setTravelers(2);

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("目的地不能为空"));

        // 验证服务层未被调用
        verify(aiService, never()).generateItineraryPlan(any());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 天数为null")
    public void testGenerateItineraryPlan_NullDays() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据 - 天数为null
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("西安");
        request.setDays(null);
        request.setTravelers(2);

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("旅行天数必须大于0"));

        // 验证服务层未被调用
        verify(aiService, never()).generateItineraryPlan(any());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 天数为0")
    public void testGenerateItineraryPlan_ZeroDays() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据 - 天数为0
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("西安");
        request.setDays(0);
        request.setTravelers(2);

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("旅行天数必须大于0"));

        // 验证服务层未被调用
        verify(aiService, never()).generateItineraryPlan(any());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 天数为负数")
    public void testGenerateItineraryPlan_NegativeDays() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据 - 天数为负数
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("西安");
        request.setDays(-1);
        request.setTravelers(2);

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("旅行天数必须大于0"));

        // 验证服务层未被调用
        verify(aiService, never()).generateItineraryPlan(any());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 人数为null")
    public void testGenerateItineraryPlan_NullTravelers() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据 - 人数为null
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("西安");
        request.setDays(3);
        request.setTravelers(null);

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("旅行人数必须大于0"));

        // 验证服务层未被调用
        verify(aiService, never()).generateItineraryPlan(any());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 人数为0")
    public void testGenerateItineraryPlan_ZeroTravelers() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据 - 人数为0
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("西安");
        request.setDays(3);
        request.setTravelers(0);

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("旅行人数必须大于0"));

        // 验证服务层未被调用
        verify(aiService, never()).generateItineraryPlan(any());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 人数为负数")
    public void testGenerateItineraryPlan_NegativeTravelers() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据 - 人数为负数
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("西安");
        request.setDays(3);
        request.setTravelers(-1);

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("旅行人数必须大于0"));

        // 验证服务层未被调用
        verify(aiService, never()).generateItineraryPlan(any());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 多个参数验证失败")
    public void testGenerateItineraryPlan_MultipleValidationFailures() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据 - 多个参数无效
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("");  // 空目的地
        request.setDays(0);          // 无效天数
        request.setTravelers(-1);    // 无效人数

        // 执行请求 - 应该返回第一个验证失败的错误
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("目的地不能为空"));

        // 验证服务层未被调用
        verify(aiService, never()).generateItineraryPlan(any());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 服务异常")
    public void testGenerateItineraryPlan_ServiceException() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("西安");
        request.setDays(3);
        request.setTravelers(2);

        // Mock服务层抛出异常
        when(aiService.generateItineraryPlan(any(AIRecommendationRequest.class)))
                .thenThrow(new RuntimeException("AI服务不可用"));

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("生成行程失败：AI服务不可用"));

        // 验证服务层调用
        verify(aiService, times(1)).generateItineraryPlan(any(AIRecommendationRequest.class));
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 最小有效请求")
    public void testGenerateItineraryPlan_MinimalValidRequest() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备最小有效请求数据
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("西安");
        request.setDays(1);
        request.setTravelers(1);

        // 准备响应数据
        AIRecommendationResponse response = AIRecommendationResponse.success(
                "{\"title\":\"西安1日游\",\"days\":1,\"travelers\":1,\"budget\":1000,\"plan\":[]}", "itinerary"
        );

        // Mock服务层
        when(aiService.generateItineraryPlan(any(AIRecommendationRequest.class)))
                .thenReturn(response);

        // 执行请求
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestType").value("itinerary"));

        // 验证服务层调用
        verify(aiService, times(1)).generateItineraryPlan(any(AIRecommendationRequest.class));
    }

    // AI服务状态测试
    @Test
    @DisplayName("测试获取AI服务状态 - 成功")
    public void testGetStatus_Success() throws Exception {
        // 执行请求
        mockMvc.perform(get("/ai/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("AI服务运行正常"));
    }

    // 边界情况测试
    @Test
    @DisplayName("测试生成个人档案AI推荐 - 空请求体")
    public void testGenerateProfileRecommendation_EmptyRequestBody() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 执行请求 - 空请求体
        mockMvc.perform(post("/ai/profile-recommendation")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 空请求体")
    public void testGenerateItineraryPlan_EmptyRequestBody() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 执行请求 - 空请求体
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("测试生成个人档案AI推荐 - 无效JSON")
    public void testGenerateProfileRecommendation_InvalidJson() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 执行请求 - 无效JSON
        mockMvc.perform(post("/ai/profile-recommendation")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("测试生成详细行程规划 - 无效JSON")
    public void testGenerateItineraryPlan_InvalidJson() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 执行请求 - 无效JSON
        mockMvc.perform(post("/ai/itinerary-plan")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    // 性能测试
    @Test
    @DisplayName("测试生成个人档案AI推荐性能")
    public void testGenerateProfileRecommendationPerformance() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setTravelPreferences(Arrays.asList("文化", "美食"));

        // 准备响应数据
        AIRecommendationResponse response = AIRecommendationResponse.success(
                "基于您的偏好，推荐您前往西安体验古都文化...", "general"
        );

        // Mock服务层
        when(aiService.generateProfileRecommendation(any(AIRecommendationRequest.class)))
                .thenReturn(response);

        // 执行多次请求测试性能
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/ai/profile-recommendation")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 验证性能（10次请求应该在合理时间内完成）
        assert duration < 5000 : "性能测试失败，10次请求耗时过长: " + duration + "ms";

        // 验证服务层调用次数
        verify(aiService, times(10)).generateProfileRecommendation(any(AIRecommendationRequest.class));
    }

    @Test
    @DisplayName("测试生成详细行程规划性能")
    public void testGenerateItineraryPlanPerformance() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("西安");
        request.setDays(3);
        request.setTravelers(2);

        // 准备响应数据
        AIRecommendationResponse response = AIRecommendationResponse.success(
                "{\"title\":\"西安3日游\",\"days\":3,\"travelers\":2,\"budget\":5000,\"plan\":[]}", "itinerary"
        );

        // Mock服务层
        when(aiService.generateItineraryPlan(any(AIRecommendationRequest.class)))
                .thenReturn(response);

        // 执行多次请求测试性能
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/ai/itinerary-plan")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 验证性能（10次请求应该在合理时间内完成）
        assert duration < 5000 : "性能测试失败，10次请求耗时过长: " + duration + "ms";

        // 验证服务层调用次数
        verify(aiService, times(10)).generateItineraryPlan(any(AIRecommendationRequest.class));
    }

    // 并发测试
    @Test
    @DisplayName("测试生成个人档案AI推荐并发")
    public void testGenerateProfileRecommendationConcurrency() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setTravelPreferences(Arrays.asList("文化", "美食"));

        // 准备响应数据
        AIRecommendationResponse response = AIRecommendationResponse.success(
                "基于您的偏好，推荐您前往西安体验古都文化...", "general"
        );

        // Mock服务层
        when(aiService.generateProfileRecommendation(any(AIRecommendationRequest.class)))
                .thenReturn(response);

        // 创建多个线程并发执行请求
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(() -> {
                try {
                    mockMvc.perform(post("/ai/profile-recommendation")
                                    .session(mockSession)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证服务层调用次数
        verify(aiService, times(5)).generateProfileRecommendation(any(AIRecommendationRequest.class));
    }

    @Test
    @DisplayName("测试生成详细行程规划并发")
    public void testGenerateItineraryPlanConcurrency() throws Exception {
        // 设置session
        mockSession.setAttribute("userId", 1L);

        // 准备请求数据
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setDestination("西安");
        request.setDays(3);
        request.setTravelers(2);

        // 准备响应数据
        AIRecommendationResponse response = AIRecommendationResponse.success(
                "{\"title\":\"西安3日游\",\"days\":3,\"travelers\":2,\"budget\":5000,\"plan\":[]}", "itinerary"
        );

        // Mock服务层
        when(aiService.generateItineraryPlan(any(AIRecommendationRequest.class)))
                .thenReturn(response);

        // 创建多个线程并发执行请求
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(() -> {
                try {
                    mockMvc.perform(post("/ai/itinerary-plan")
                                    .session(mockSession)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证服务层调用次数
        verify(aiService, times(5)).generateItineraryPlan(any(AIRecommendationRequest.class));
    }
}
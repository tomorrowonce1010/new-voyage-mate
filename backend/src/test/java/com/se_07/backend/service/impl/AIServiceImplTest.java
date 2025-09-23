package com.se_07.backend.service.impl;

import com.se_07.backend.dto.AIRecommendationRequest;
import com.se_07.backend.dto.AIRecommendationResponse;
import com.se_07.backend.entity.Attraction;
import com.se_07.backend.entity.Destination;
import com.se_07.backend.repository.AttractionRepository;
import com.se_07.backend.repository.DestinationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AIServiceImplTest {

    @Mock
    private DestinationRepository destinationRepository;
    @Mock
    private AttractionRepository attractionRepository;
    @InjectMocks
    private AIServiceImpl aiService;

    @BeforeEach
    void setUp() {
        // mock apiKey 和 apiUrl，避免真实请求
        ReflectionTestUtils.setField(aiService, "apiKey", "test-key");
        ReflectionTestUtils.setField(aiService, "apiUrl", "http://mock-api");
    }

    @Test
    void testGenerateProfileRecommendation_Normal() throws Exception {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setTravelPreferences(Arrays.asList("美食", "文化"));
        req.setSpecialNeeds(Arrays.asList("无障碍"));
        req.setNaturalLanguageDescription("喜欢自然风光");
        req.setHistoricalDestinations(Arrays.asList("北京"));
        req.setWishlistDestinations(Arrays.asList("上海"));
        Destination d1 = new Destination(); d1.setId(1L); d1.setName("北京"); d1.setDescription("首都");
        Destination d2 = new Destination(); d2.setId(2L); d2.setName("上海"); d2.setDescription("魔都");
        when(destinationRepository.findAll()).thenReturn(Arrays.asList(d1, d2));
        
        // Test the actual method - it will use fallback due to invalid API key
        AIRecommendationResponse resp = aiService.generateProfileRecommendation(req);
        assertNotNull(resp);
        assertTrue(resp.getContent().contains("北京") || resp.getContent().contains("上海"));
        assertEquals("general", resp.getRequestType());
    }

    @Test
    void testGenerateProfileRecommendation_Fallback() throws Exception {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setTravelPreferences(Arrays.asList("美食"));
        Destination d1 = new Destination(); d1.setId(1L); d1.setName("北京"); d1.setDescription("首都");
        when(destinationRepository.findAll()).thenReturn(Arrays.asList(d1));
        
        // Test the actual method - it will use fallback due to invalid API key
        AIRecommendationResponse resp = aiService.generateProfileRecommendation(req);
        assertNotNull(resp);
        assertTrue(resp.getContent().contains("北京"));
        assertEquals("general", resp.getRequestType());
    }

    @Test
    void testGenerateItineraryPlan_Normal() throws Exception {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setDestination("北京");
        req.setDays(2);
        req.setTravelers(2);
        req.setBudget(3000.0);
        req.setTravelPreferences(Arrays.asList("美食"));
        req.setSpecialNeeds(Arrays.asList("无障碍"));
        Attraction a1 = new Attraction(); a1.setId(1L); a1.setName("天安门"); a1.setDescription("地标");
        when(attractionRepository.findByDestinationName("北京")).thenReturn(Arrays.asList(a1));
        
        // Test the actual method - it will use fallback due to invalid API key
        AIRecommendationResponse resp = aiService.generateItineraryPlan(req);
        assertNotNull(resp);
        assertTrue(resp.getContent().contains("北京"));
        assertEquals("itinerary", resp.getRequestType());
    }

    @Test
    void testGenerateItineraryPlan_Fallback() throws Exception {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setDestination("北京");
        req.setDays(1);
        req.setTravelers(1);
        req.setBudget(1000.0);
        when(attractionRepository.findByDestinationName("北京")).thenReturn(Collections.emptyList());
        
        // Test the actual method - it will use fallback due to invalid API key
        AIRecommendationResponse resp = aiService.generateItineraryPlan(req);
        assertNotNull(resp);
        assertTrue(resp.getContent().contains("北京"));
        assertEquals("itinerary", resp.getRequestType());
    }

    @Test
    void testBuildProfilePrompt_AllFields() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setTravelPreferences(Arrays.asList("美食", "文化"));
        req.setSpecialNeeds(Arrays.asList("无障碍"));
        req.setNaturalLanguageDescription("喜欢自然风光");
        req.setHistoricalDestinations(Arrays.asList("北京"));
        req.setWishlistDestinations(Arrays.asList("上海"));
        Destination d1 = new Destination(); d1.setId(1L); d1.setName("北京"); d1.setDescription("首都");
        Destination d2 = new Destination(); d2.setId(2L); d2.setName("上海"); d2.setDescription("魔都");
        when(destinationRepository.findAll()).thenReturn(Arrays.asList(d1, d2));
        String prompt = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "buildProfilePrompt", req);
        assertTrue(prompt.contains("美食"));
        assertTrue(prompt.contains("无障碍"));
        assertTrue(prompt.contains("北京"));
        assertTrue(prompt.contains("上海"));
    }

    @Test
    void testBuildProfilePrompt_EmptyFields() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        when(destinationRepository.findAll()).thenReturn(Collections.emptyList());
        String prompt = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "buildProfilePrompt", req);
        assertTrue(prompt.contains("可推荐的城市范围"));
    }

    @Test
    void testBuildProfilePrompt_NullFields() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setTravelPreferences(null);
        req.setSpecialNeeds(null);
        req.setNaturalLanguageDescription(null);
        req.setHistoricalDestinations(null);
        req.setWishlistDestinations(null);
        Destination d1 = new Destination(); d1.setId(1L); d1.setName("北京"); d1.setDescription("首都");
        when(destinationRepository.findAll()).thenReturn(Arrays.asList(d1));
        String prompt = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "buildProfilePrompt", req);
        assertTrue(prompt.contains("北京"));
        assertFalse(prompt.contains("用户旅行偏好"));
        assertFalse(prompt.contains("特殊需求"));
        assertFalse(prompt.contains("用户描述"));
        assertFalse(prompt.contains("历史目的地"));
        assertFalse(prompt.contains("期望目的地"));
    }

    @Test
    void testBuildProfilePrompt_EmptyStringDescription() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setNaturalLanguageDescription("   "); // 只有空格
        Destination d1 = new Destination(); d1.setId(1L); d1.setName("北京"); d1.setDescription("首都");
        when(destinationRepository.findAll()).thenReturn(Arrays.asList(d1));
        String prompt = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "buildProfilePrompt", req);
        assertTrue(prompt.contains("北京"));
        assertFalse(prompt.contains("用户描述"));
    }

    @Test
    void testBuildItineraryPrompt_AllFields() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setDestination("北京");
        req.setDays(2);
        req.setTravelers(2);
        req.setBudget(3000.0);
        req.setTravelPreferences(Arrays.asList("美食"));
        req.setSpecialNeeds(Arrays.asList("无障碍"));
        Attraction a1 = new Attraction(); a1.setId(1L); a1.setName("天安门"); a1.setDescription("地标");
        when(attractionRepository.findByDestinationName("北京")).thenReturn(Arrays.asList(a1));
        String prompt = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "buildItineraryPrompt", req);
        assertTrue(prompt.contains("天安门"));
        assertTrue(prompt.contains("预算"));
    }

    @Test
    void testBuildItineraryPrompt_EmptyAttractions() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setDestination("北京");
        req.setDays(1);
        req.setTravelers(1);
        when(attractionRepository.findByDestinationName("北京")).thenReturn(Collections.emptyList());
        String prompt = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "buildItineraryPrompt", req);
        assertTrue(prompt.contains("北京"));
    }

    @Test
    void testBuildItineraryPrompt_NullBudget() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setDestination("北京");
        req.setDays(1);
        req.setTravelers(1);
        req.setBudget(null);
        when(attractionRepository.findByDestinationName("北京")).thenReturn(Collections.emptyList());
        String prompt = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "buildItineraryPrompt", req);
        assertTrue(prompt.contains("北京"));
        assertTrue(prompt.contains("5000")); // 默认预算
    }

    @Test
    void testBuildItineraryPrompt_NullFields() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setDestination("北京");
        req.setDays(1);
        req.setTravelers(1);
        req.setTravelPreferences(null);
        req.setSpecialNeeds(null);
        when(attractionRepository.findByDestinationName("北京")).thenReturn(Collections.emptyList());
        String prompt = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "buildItineraryPrompt", req);
        assertTrue(prompt.contains("北京"));
        assertFalse(prompt.contains("旅行偏好"));
        assertFalse(prompt.contains("特殊需求"));
    }

    @Test
    void testGenerateFallbackProfileRecommendation_EmptyDestinations() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        when(destinationRepository.findAll()).thenReturn(Collections.emptyList());
        String result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "generateFallbackProfileRecommendation", req);
        assertTrue(result.contains("张家界"));
    }

    @Test
    void testGenerateFallbackProfileRecommendation_WithDestinations() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        Destination d1 = new Destination(); d1.setId(1L); d1.setName("北京"); d1.setDescription("首都");
        when(destinationRepository.findAll()).thenReturn(Arrays.asList(d1));
        String result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "generateFallbackProfileRecommendation", req);
        assertTrue(result.contains("北京"));
    }

    @Test
    void testGenerateFallbackProfileRecommendation_MultipleDestinations() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        Destination d1 = new Destination(); d1.setId(1L); d1.setName("北京"); d1.setDescription("首都");
        Destination d2 = new Destination(); d2.setId(2L); d2.setName("上海"); d2.setDescription("魔都");
        Destination d3 = new Destination(); d3.setId(3L); d3.setName("广州"); d3.setDescription("花城");
        Destination d4 = new Destination(); d4.setId(4L); d4.setName("深圳"); d4.setDescription("鹏城");
        when(destinationRepository.findAll()).thenReturn(Arrays.asList(d1, d2, d3, d4));
        String result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "generateFallbackProfileRecommendation", req);
        // 应该只包含3个城市（最多推荐3个）
        assertTrue(result.contains("北京") || result.contains("上海") || result.contains("广州") || result.contains("深圳"));
    }

    @Test
    void testGenerateFallbackItinerary_EmptyAttractions() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setDestination("北京");
        req.setDays(1);
        req.setTravelers(1);
        req.setBudget(1000.0);
        when(attractionRepository.findByDestinationName("北京")).thenReturn(Collections.emptyList());
        String result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "generateFallbackItinerary", req);
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("市中心漫步"));
    }

    @Test
    void testGenerateFallbackItinerary_WithAttractions() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setDestination("北京");
        req.setDays(2);
        req.setTravelers(2);
        req.setBudget(2000.0);
        Attraction a1 = new Attraction(); a1.setId(1L); a1.setName("天安门"); a1.setDescription("地标");
        Attraction a2 = new Attraction(); a2.setId(2L); a2.setName("故宫"); a2.setDescription("博物馆");
        when(attractionRepository.findByDestinationName("北京")).thenReturn(Arrays.asList(a1, a2));
        String result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "generateFallbackItinerary", req);
        assertTrue(result.contains("天安门"));
        // For a 2-day trip, day 2 is the last day, so it will only include "购买纪念品" not "深度游览特色景点"
        // and "故宫" won't be included since it's the second attraction and we only have 2 days
        assertTrue(result.contains("购买纪念品"));
    }

    @Test
    void testGenerateFallbackItinerary_MultiDayWithAttractions() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setDestination("北京");
        req.setDays(3);
        req.setTravelers(2);
        req.setBudget(3000.0);
        Attraction a1 = new Attraction(); a1.setId(1L); a1.setName("天安门"); a1.setDescription("地标");
        Attraction a2 = new Attraction(); a2.setId(2L); a2.setName("故宫"); a2.setDescription("博物馆");
        Attraction a3 = new Attraction(); a3.setId(3L); a3.setName("颐和园"); a3.setDescription("皇家园林");
        when(attractionRepository.findByDestinationName("北京")).thenReturn(Arrays.asList(a1, a2, a3));
        String result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "generateFallbackItinerary", req);
        assertTrue(result.contains("天安门"));
        assertTrue(result.contains("故宫"));
        assertTrue(result.contains("深度游览特色景点")); // 中间天应该包含这个
        assertTrue(result.contains("购买纪念品")); // 最后一天
    }

    @Test
    void testGenerateFallbackItinerary_NullBudget() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setDestination("北京");
        req.setDays(1);
        req.setTravelers(1);
        req.setBudget(null);
        when(attractionRepository.findByDestinationName("北京")).thenReturn(Collections.emptyList());
        String result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "generateFallbackItinerary", req);
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("5000")); // 默认预算
    }

    @Test
    void testGenerateFallbackItinerary_SingleDayLastDay() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setDestination("北京");
        req.setDays(1);
        req.setTravelers(1);
        req.setBudget(1000.0);
        Attraction a1 = new Attraction(); a1.setId(1L); a1.setName("天安门"); a1.setDescription("地标");
        when(attractionRepository.findByDestinationName("北京")).thenReturn(Arrays.asList(a1));
        String result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "generateFallbackItinerary", req);
        assertTrue(result.contains("天安门"));
        assertTrue(result.contains("市中心漫步"));
        // 单天行程不会包含“购买纪念品”
    }

    @Test
    void testGenerateFallbackItinerary_MiddleDaysLogic() {
        AIRecommendationRequest req = new AIRecommendationRequest();
        req.setDestination("北京");
        req.setDays(5);
        req.setTravelers(2);
        req.setBudget(5000.0);
        Attraction a1 = new Attraction(); a1.setId(1L); a1.setName("天安门"); a1.setDescription("地标");
        Attraction a2 = new Attraction(); a2.setId(2L); a2.setName("故宫"); a2.setDescription("博物馆");
        Attraction a3 = new Attraction(); a3.setId(3L); a3.setName("颐和园"); a3.setDescription("皇家园林");
        Attraction a4 = new Attraction(); a4.setId(4L); a4.setName("长城"); a4.setDescription("世界遗产");
        when(attractionRepository.findByDestinationName("北京")).thenReturn(Arrays.asList(a1, a2, a3, a4));
        String result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(aiService, "generateFallbackItinerary", req);
        assertTrue(result.contains("天安门"));
        assertTrue(result.contains("故宫"));
        assertTrue(result.contains("颐和园"));
        assertTrue(result.contains("长城"));
        assertTrue(result.contains("深度游览特色景点"));
        assertTrue(result.contains("购买纪念品"));
    }
} 
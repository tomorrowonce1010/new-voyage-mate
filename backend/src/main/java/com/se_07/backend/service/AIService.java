package com.se_07.backend.service;

import com.se_07.backend.dto.AIRecommendationRequest;
import com.se_07.backend.dto.AIRecommendationResponse;

public interface AIService {
    
    /**
     * 生成个人档案AI推荐
     */
    AIRecommendationResponse generateProfileRecommendation(AIRecommendationRequest request);
    
    /**
     * 生成详细行程规划
     */
    AIRecommendationResponse generateItineraryPlan(AIRecommendationRequest request);
} 
package com.se_07.backend.dto;

import lombok.Data;

@Data
public class AIRecommendationResponse {
    private String content;
    private String requestType;
    private boolean success;
    private String errorMessage;
    
    public static AIRecommendationResponse success(String content, String requestType) {
        AIRecommendationResponse response = new AIRecommendationResponse();
        response.setContent(content);
        response.setRequestType(requestType);
        response.setSuccess(true);
        return response;
    }
    
    public static AIRecommendationResponse error(String errorMessage) {
        AIRecommendationResponse response = new AIRecommendationResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
} 
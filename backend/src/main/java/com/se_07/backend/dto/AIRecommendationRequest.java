package com.se_07.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class AIRecommendationRequest {
    private List<String> travelPreferences;
    private List<String> specialNeeds;
    private String naturalLanguageDescription;
    private List<String> historicalDestinations;
    private List<String> wishlistDestinations;
    private String requestType; // "general" 或 "itinerary"
    
    // 行程规划专用字段
    private String destination;
    private Integer days;
    private Integer travelers;
    private Double budget;
} 
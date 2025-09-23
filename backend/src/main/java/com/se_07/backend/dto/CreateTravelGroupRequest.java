package com.se_07.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateTravelGroupRequest {
    private String title;
    private String description;
    private Integer maxMembers;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal estimatedBudget;
    private String groupType;
    private Boolean isPublic;
    private List<String> travelTags;
    private String introduction;
    private Long destinationId;  // 添加目的地ID字段
} 
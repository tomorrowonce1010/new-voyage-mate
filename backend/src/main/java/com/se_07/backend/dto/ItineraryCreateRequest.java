package com.se_07.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ItineraryCreateRequest {
    private String title;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private Integer travelerCount;
    private String travelStatus;
    private String permissionStatus;
} 
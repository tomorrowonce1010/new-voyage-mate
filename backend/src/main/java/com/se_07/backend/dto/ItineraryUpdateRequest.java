package com.se_07.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ItineraryUpdateRequest {
    private String title;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private Integer travelerCount;
    private String travelStatus;  // "待出行" 或 "已出行"
    private String editStatus;    // "草稿" 或 "完成"
    private String permissionStatus;  // "所有人可见" 或 "仅获得链接者可见" 或 "私人"
} 
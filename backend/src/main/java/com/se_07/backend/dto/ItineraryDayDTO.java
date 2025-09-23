package com.se_07.backend.dto;

import com.se_07.backend.entity.ItineraryActivity;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItineraryDayDTO {
    private Long id;
    private Long itineraryId;  // 只包含itinerary的ID
    private String itineraryTitle;  // 包含itinerary的标题
    private Integer dayNumber;
    private LocalDate date;
    private String title;
    private Long firstActivityId;
    private Long lastActivityId;
    private String notes;
    private BigDecimal actualCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ItineraryActivity> activities;

    // 可以添加一些简单的itinerary信息
    private LocalDate itineraryStartDate;
    private LocalDate itineraryEndDate;
    private String itineraryPermissionStatus;

    private java.util.List<String> destinationNames;
} 
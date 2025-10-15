package com.se_07.backend.dto;

import com.se_07.backend.entity.Attraction;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;

@Data
public class ItineraryActivityDTO {
    private Long id;
    private Long itineraryDayId;  // 只包含itineraryDay的ID
    private Integer dayNumber;     // 包含所属日程的天数
    private LocalDate date;        // 包含所属日程的日期
    private Long prevId;
    private Long nextId;
    private String title;
    private String transportMode;
    private String transportNotes;
    private Attraction attraction;
    private LocalTime startTime;
    private LocalTime endTime;
    private String attractionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 
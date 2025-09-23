package com.se_07.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class CommunityEntryDocument {
    private Long id;
    private String shareCode;
    private String description;
    private Integer viewCount;
    private String createdAt;
    private String updatedAt;
    private String itineraryTitle;
    private String startDate;
    private String endDate;
    private String destinations;
    private String authorUsername;
    private Long authorId;
    private String tags;
    private List<Float> vector;
} 
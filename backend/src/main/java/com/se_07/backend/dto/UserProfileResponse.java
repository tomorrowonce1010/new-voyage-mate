package com.se_07.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private LocalDate birthday;
    private String signature;
    private String bio;
    private List<TagPreferenceDto> travelPreferences;
    private List<String> specialRequirements;
    private String specialRequirementsDescription;
    private List<HistoryDestinationDto> historyDestinations;
    private List<WishlistDestinationDto> wishlistDestinations;
    
    @Data
    public static class HistoryDestinationDto {
        private Long destinationId;
        private String name;
        private String description;
        private String imageUrl;
        private String visitYearMonth;
        private Integer days;
        private String notes;
        private Long itineraryId;
        private LocalDate startDate;
        private LocalDate endDate;
        
        public boolean isManuallyAdded() {
            return itineraryId != null && itineraryId == 0L;
        }
        
        public boolean isAutoAdded() {
            return itineraryId != null && itineraryId > 0L;
        }
    }
    
    @Data
    public static class WishlistDestinationDto {
        private Long destinationId;
        private String name;
        private String description;
        private String imageUrl;
        private String notes;
    }
    
    @Data
    public static class TagPreferenceDto {
        private Long tagId;
        private String tagName;
        private Boolean selected;
        
        public TagPreferenceDto(Long tagId, String tagName, Boolean selected) {
            this.tagId = tagId;
            this.tagName = tagName;
            this.selected = selected;
        }
    }
} 
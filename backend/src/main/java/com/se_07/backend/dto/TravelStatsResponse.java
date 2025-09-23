package com.se_07.backend.dto;

import java.time.LocalDate;
import java.util.List;

public class TravelStatsResponse {
    private String username;
    private Integer companionDays;
    private Integer totalDestinations;
    private Integer totalDays;
    private Integer totalItineraries;
    private List<TimelineItem> timeline;
    private GeographyStats geography;
    private List<CityStats> topCities;

    // 时间轴项目
    public static class TimelineItem {
        private String name;
        private String visitYearMonth;
        private Integer days;
        private String notes;
        private Boolean hasItinerary;
        private Long itineraryId;
        private LocalDate startDate;
        private LocalDate endDate;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getVisitYearMonth() { return visitYearMonth; }
        public void setVisitYearMonth(String visitYearMonth) { this.visitYearMonth = visitYearMonth; }

        public Integer getDays() { return days; }
        public void setDays(Integer days) { this.days = days; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public Boolean getHasItinerary() { return hasItinerary; }
        public void setHasItinerary(Boolean hasItinerary) { this.hasItinerary = hasItinerary; }

        public Long getItineraryId() { return itineraryId; }
        public void setItineraryId(Long itineraryId) { this.itineraryId = itineraryId; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    // 地理统计
    public static class GeographyStats {
        private String easternmost;
        private String southernmost;
        private String westernmost;
        private String northernmost;
        private String favoriteMonth;
        private String mostTravelYear;

        // Getters and Setters
        public String getEasternmost() { return easternmost; }
        public void setEasternmost(String easternmost) { this.easternmost = easternmost; }

        public String getSouthernmost() { return southernmost; }
        public void setSouthernmost(String southernmost) { this.southernmost = southernmost; }

        public String getWesternmost() { return westernmost; }
        public void setWesternmost(String westernmost) { this.westernmost = westernmost; }

        public String getNorthernmost() { return northernmost; }
        public void setNorthernmost(String northernmost) { this.northernmost = northernmost; }

        public String getFavoriteMonth() { return favoriteMonth; }
        public void setFavoriteMonth(String favoriteMonth) { this.favoriteMonth = favoriteMonth; }

        public String getMostTravelYear() { return mostTravelYear; }
        public void setMostTravelYear(String mostTravelYear) { this.mostTravelYear = mostTravelYear; }
    }

    // 城市统计
    public static class CityStats {
        private String name;
        private Integer visitCount;
        private Integer totalDays;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getVisitCount() { return visitCount; }
        public void setVisitCount(Integer visitCount) { this.visitCount = visitCount; }

        public Integer getTotalDays() { return totalDays; }
        public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }
    }

    // Main class getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Integer getCompanionDays() { return companionDays; }
    public void setCompanionDays(Integer companionDays) { this.companionDays = companionDays; }

    public Integer getTotalDestinations() { return totalDestinations; }
    public void setTotalDestinations(Integer totalDestinations) { this.totalDestinations = totalDestinations; }

    public Integer getTotalDays() { return totalDays; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }

    public Integer getTotalItineraries() { return totalItineraries; }
    public void setTotalItineraries(Integer totalItineraries) { this.totalItineraries = totalItineraries; }

    public List<TimelineItem> getTimeline() { return timeline; }
    public void setTimeline(List<TimelineItem> timeline) { this.timeline = timeline; }

    public GeographyStats getGeography() { return geography; }
    public void setGeography(GeographyStats geography) { this.geography = geography; }

    public List<CityStats> getTopCities() { return topCities; }
    public void setTopCities(List<CityStats> topCities) { this.topCities = topCities; }
} 
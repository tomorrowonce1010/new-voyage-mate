package com.se_07.backend.dto;

import java.time.LocalDate;
import java.util.List;

public class UserHomepageResponse {
    private Long id;
    private String username;
    private LocalDate birthday;
    private String ip;
    private String signature;
    private String bio;
    private String avatarUrl;
    private List<PublicItineraryDto> publicItineraries;

    // 内部类：公开行程DTO
    public static class PublicItineraryDto {
        private Long id;
        private String title;
        private String imageUrl;
        private LocalDate startDate;
        private LocalDate endDate;
        private String duration;
        private String destination;
        private String description;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }

        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public List<PublicItineraryDto> getPublicItineraries() { return publicItineraries; }
    public void setPublicItineraries(List<PublicItineraryDto> publicItineraries) { this.publicItineraries = publicItineraries; }
} 
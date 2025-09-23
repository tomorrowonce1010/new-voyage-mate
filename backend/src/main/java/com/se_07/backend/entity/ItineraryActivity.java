package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "itinerary_activities")
@Data
public class ItineraryActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "itinerary_day_id", nullable = false)
    private ItineraryDay itineraryDay;
    
    @Column(name = "prev_id")
    private Long prevId = null;  // 默认为 null，表示没有前序活动
    
    @Column(name = "next_id")
    private Long nextId = null;  // 默认为 null，表示没有后序活动
    
    @Column(name = "title", length = 200, nullable = false)
    private String title;
    
    @Column(name = "transport_mode", length = 50, nullable = false)
    private String transportMode = "步行";
    
    @ManyToOne
    @JoinColumn(name = "attraction_id", nullable = false)
    private Attraction attraction;
    
    @Column(name = "start_time")
    private LocalTime startTime;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Column(name = "attraction_notes", columnDefinition = "TEXT")
    private String attractionNotes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 
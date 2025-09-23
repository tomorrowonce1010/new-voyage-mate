package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_destinations")
@Data
public class UserDestination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_preferences_id", nullable = false)
    private Long userPreferencesId;
    
    @Column(name = "destination_id", nullable = false)
    private Long destinationId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Type type;
    

    
    @Column(name = "days")
    private Integer days;
    
    @Column(name = "notes", length = 255)
    private String notes;
    
    @Column(name = "itinerary_id", nullable = false)
    private Long itineraryId = 0L; // 0表示手动添加，>0表示来自某个行程的自动添加
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", insertable = false, updatable = false)
    private Destination destination;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum Type {
        历史目的地, 期望目的地
    }
    
    // 便捷方法：判断是否为手动添加
    public boolean isManuallyAdded() {
        return itineraryId != null && itineraryId == 0L;
    }
    
    // 便捷方法：判断是否为自动添加
    public boolean isAutoAdded() {
        return itineraryId != null && itineraryId > 0L;
    }
} 
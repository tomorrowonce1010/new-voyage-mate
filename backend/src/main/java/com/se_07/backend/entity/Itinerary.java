package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "itineraries")
@Data
public class Itinerary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;
    
    @Column(name = "group_id")
    private Long groupId;
    
    @Column(name = "title", length = 200, nullable = false)
    private String title;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "budget", precision = 10, scale = 2)
    private BigDecimal budget;
    
    @Column(name = "traveler_count")
    private Integer travelerCount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "travel_status")
    private TravelStatus travelStatus = TravelStatus.待出行;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "edit_status")
    private EditStatus editStatus = EditStatus.草稿;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_status")
    private PermissionStatus permissionStatus = PermissionStatus.私人;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ItineraryDay> itineraryDays;
    
    public enum TravelStatus {
        待出行, 已出行
    }
    
    public enum EditStatus {
        草稿, 完成
    }
    
    public enum PermissionStatus {
        所有人可见, 仅获得链接者可见, 私人
    }
    
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
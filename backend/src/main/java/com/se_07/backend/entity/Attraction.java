package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "attractions")
@Data
public class Attraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "amap_poi_id", length = 50)
    private String amapPoiId;
    
    @ManyToOne
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;
    
    @Column(name = "name", length = 200, nullable = false)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @Column(name = "latitude", precision = 10, scale = 6)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 10, scale = 6)
    private BigDecimal longitude;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private AttractionCategory category;
    
    @Column(name = "opening_hours", columnDefinition = "JSON")
    private String openingHours;
    
    @Column(name = "join_count")
    private Integer joinCount = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "tag_scores", columnDefinition = "JSON")
    private String tagScores;
    
    public enum AttractionCategory {
        旅游景点, 交通站点, 餐饮, 住宿
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
package com.se_07.backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "destinations")
@Data
public class Destination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "amap_poi_id", length = 50)
    private String amapPoiId;
    
    @Column(name = "name", length = 200, nullable = false)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @Column(name = "latitude", precision = 10, scale = 6)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 10, scale = 6)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal longitude;
    
    @Column(name = "join_count")
    private Integer joinCount = 0;
    
    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Column(name = "tag_scores", columnDefinition = "JSON")
    private String tagScores;
    
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
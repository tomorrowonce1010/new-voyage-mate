package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_recommendations")
@Data
public class GroupRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private TravelGroup group;
    
    @Column(name = "recommendation_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal recommendationScore;
    
    @Column(name = "recommendation_reason", columnDefinition = "JSON")
    private String recommendationReason;
    
    @Column(name = "is_clicked")
    private Boolean isClicked = false;
    
    @Column(name = "is_applied")
    private Boolean isApplied = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
} 
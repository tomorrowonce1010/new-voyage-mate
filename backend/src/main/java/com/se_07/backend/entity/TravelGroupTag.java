package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "travel_group_tags")
@Data
public class TravelGroupTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private TravelGroup group;
    
    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
    
    @Column(name = "weight", precision = 3, scale = 2)
    private BigDecimal weight = BigDecimal.valueOf(1.0);
} 
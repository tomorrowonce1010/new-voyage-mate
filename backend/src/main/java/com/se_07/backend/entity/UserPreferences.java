package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_preferences")
@Data
public class UserPreferences {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(name = "travel_preferences", columnDefinition = "JSON")
    private String travelPreferences;
    
    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;
    
    @Column(name = "special_requirements_description", columnDefinition = "TEXT")
    private String specialRequirementsDescription;
} 
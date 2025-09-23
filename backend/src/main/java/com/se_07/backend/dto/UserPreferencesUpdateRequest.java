package com.se_07.backend.dto;

import lombok.Data;

@Data
public class UserPreferencesUpdateRequest {
    private String travelPreferences; // JSON格式: {"1": 1, "2": 0, ...}
    private String specialRequirements;
    private String specialRequirementsDescription; // 特殊需求描述
} 
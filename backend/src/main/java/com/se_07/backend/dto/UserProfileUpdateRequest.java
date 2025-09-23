package com.se_07.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserProfileUpdateRequest {
    private String username;
    private String avatarUrl;
    private LocalDate birthday;
    private String signature;
    private String bio;
} 
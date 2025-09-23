package com.se_07.backend.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private boolean success;
    private String message;
    private Long userId;
    private String username;
    private String email;
} 
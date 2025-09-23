package com.se_07.backend.dto;

import lombok.Data;

@Data
public class AddWishlistDestinationRequest {
    private String name;
    private String description;
    private String notes;
} 
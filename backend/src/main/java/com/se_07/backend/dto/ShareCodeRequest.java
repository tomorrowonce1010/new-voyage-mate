package com.se_07.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class ShareCodeRequest {
    private String description;
    private List<Long> tagIds;

    public ShareCodeRequest() {}

    public ShareCodeRequest(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
} 
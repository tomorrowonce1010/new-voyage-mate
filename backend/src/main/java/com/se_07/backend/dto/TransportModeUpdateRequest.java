package com.se_07.backend.dto;

public class TransportModeUpdateRequest {
    private String transportMode;

    // 默认构造函数
    public TransportModeUpdateRequest() {
    }

    // 带参数的构造函数
    public TransportModeUpdateRequest(String transportMode) {
        this.transportMode = transportMode;
    }

    // Getter和Setter
    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }
} 
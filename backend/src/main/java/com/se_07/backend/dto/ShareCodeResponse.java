package com.se_07.backend.dto;

public class ShareCodeResponse {
    private String shareCode;
    private String message;

    public ShareCodeResponse() {}

    public ShareCodeResponse(String shareCode) {
        this.shareCode = shareCode;
    }

    public ShareCodeResponse(String shareCode, String message) {
        this.shareCode = shareCode;
        this.message = message;
    }

    public String getShareCode() {
        return shareCode;
    }

    public void setShareCode(String shareCode) {
        this.shareCode = shareCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 
package com.se_07.backend.dto;

import lombok.Data;

@Data
public class PermissionStatusResponse {
    private boolean needsShareDialog;
    private String message;
    
    public PermissionStatusResponse() {}
    
    public PermissionStatusResponse(boolean needsShareDialog, String message) {
        this.needsShareDialog = needsShareDialog;
        this.message = message;
    }
    
    public static PermissionStatusResponse success() {
        return new PermissionStatusResponse(false, "权限状态更新成功");
    }
    
    public static PermissionStatusResponse needsShare() {
        return new PermissionStatusResponse(true, "请完成分享设置");
    }
} 
package com.se_07.backend.service;

import com.se_07.backend.dto.LoginRequest;
import com.se_07.backend.dto.RegisterRequest;
import com.se_07.backend.dto.AuthResponse;
import jakarta.servlet.http.HttpSession;

public interface AuthService {
    
    /**
     * 用户注册
     * @param request 注册请求
     * @param session HTTP会话
     * @return 注册结果
     */
    AuthResponse register(RegisterRequest request, HttpSession session);
    
    /**
     * 用户登录
     * @param request 登录请求
     * @param session HTTP会话
     * @return 登录结果
     */
    AuthResponse login(LoginRequest request, HttpSession session);
    
    /**
     * 用户登出
     * @param session HTTP会话
     */
    void logout(HttpSession session);
    
    /**
     * 检查登录状态
     * @param session HTTP会话
     * @return 登录状态信息
     */
    AuthResponse checkLoginStatus(HttpSession session);
} 
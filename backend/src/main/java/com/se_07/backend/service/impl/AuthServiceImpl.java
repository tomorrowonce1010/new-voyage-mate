package com.se_07.backend.service.impl;

import com.se_07.backend.dto.AuthResponse;
import com.se_07.backend.dto.LoginRequest;
import com.se_07.backend.dto.RegisterRequest;
import com.se_07.backend.entity.User;
import com.se_07.backend.entity.UserAuth;
import com.se_07.backend.repository.UserAuthRepository;
import com.se_07.backend.repository.UserRepository;
import com.se_07.backend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserAuthRepository userAuthRepository;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpSession session) {
        AuthResponse response = new AuthResponse();
        
        try {
            // 验证密码一致性
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                response.setSuccess(false);
                response.setMessage("两次输入的密码不一致");
                return response;
            }
            
            // 检查邮箱是否已存在
            if (userRepository.existsByEmail(request.getEmail())) {
                response.setSuccess(false);
                response.setMessage("该邮箱已被注册");
                return response;
            }
            
            // 创建用户
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user = userRepository.save(user);
            
            // 创建用户认证信息
            UserAuth userAuth = new UserAuth();
            userAuth.setUser(user);
            userAuth.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            userAuthRepository.save(userAuth);
            
            // 设置session
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("email", user.getEmail());
            
            response.setSuccess(true);
            response.setMessage("注册成功");
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("注册失败：" + e.getMessage());
        }
        
        return response;
    }

    @Override
    public AuthResponse login(LoginRequest request, HttpSession session) {
        AuthResponse response = new AuthResponse();
        
        try {
            // 根据邮箱查找用户
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            if (!userOptional.isPresent()) {
                response.setSuccess(false);
                response.setMessage("邮箱或密码错误");
                return response;
            }
            
            User user = userOptional.get();
            
            // 查找用户认证信息
            Optional<UserAuth> userAuthOptional = userAuthRepository.findByUserId(user.getId());
            if (!userAuthOptional.isPresent()) {
                response.setSuccess(false);
                response.setMessage("用户认证信息不存在");
                return response;
            }
            
            UserAuth userAuth = userAuthOptional.get();
            
            // 验证密码
            if (!passwordEncoder.matches(request.getPassword(), userAuth.getPasswordHash())) {
                response.setSuccess(false);
                response.setMessage("邮箱或密码错误");
                return response;
            }
            
            // 更新最后登录时间
            userAuth.setLastLogin(LocalDateTime.now());
            userAuthRepository.save(userAuth);
            
            // 设置session
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("email", user.getEmail());
            
            response.setSuccess(true);
            response.setMessage("登录成功");
            response.setUserId(user.getId());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("登录失败：" + e.getMessage());
        }
        
        return response;
    }

    @Override
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @Override
    public AuthResponse checkLoginStatus(HttpSession session) {
        AuthResponse response = new AuthResponse();
        
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            response.setSuccess(false);
            response.setMessage("未登录");
            return response;
        }
        
        String username = (String) session.getAttribute("username");
        String email = (String) session.getAttribute("email");
        
        response.setSuccess(true);
        response.setMessage("已登录");
        response.setUserId(userId);
        response.setUsername(username);
        response.setEmail(email);
        
        return response;
    }
}
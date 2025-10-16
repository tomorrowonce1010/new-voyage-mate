package com.se_07.backend.controller;

import com.se_07.backend.service.AuthService;
import com.se_07.backend.service.FriendService;
import com.se_07.backend.dto.LoginRequest;
import com.se_07.backend.dto.RegisterRequest;
import com.se_07.backend.dto.AuthResponse;
import com.se_07.backend.dto.ChatMessageDTO;
import com.se_07.backend.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/auth")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;
    @Autowired
    private FriendService friendService;
    @Autowired
    private ChatMessageService chatMessageService;

    /**
     * 用户注册
     * POST /auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request, HttpSession session) {
        AuthResponse response = authService.register(request, session);
        return ResponseEntity.ok(response);
    }

    /**
     * 用户登录
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpSession session) {
        AuthResponse response = authService.login(request, session);
        int error = 10/0; // 测试错误
        return ResponseEntity.ok(response);
    }

    /**
     * 用户登出
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.ok().build();
    }

    /**
     * 检查登录状态
     * GET /auth/status
     */
    @GetMapping("/status")
    public ResponseEntity<AuthResponse> checkLoginStatus(HttpSession session) {
        AuthResponse response = authService.checkLoginStatus(session);
        return ResponseEntity.ok(response);
    }

    /**
     * 添加好友（双向）
     * POST /auth/friends/add
     * 参数: userId, friendId
     * Content-Type: application/x-www-form-urlencoded
     */
    @PostMapping("/friends/add")
    public ResponseEntity<Map<String, Object>> addFriend(@RequestParam Long userId, @RequestParam Long friendId) {
        logger.info("添加好友: {} <-> {}", userId, friendId);
        Map<String, Object> response = new HashMap<>();
        if (userId == null || friendId == null) {
            response.put("success", false);
            response.put("message", "userId和friendId不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        if (userId.equals(friendId)) {
            response.put("success", false);
            response.put("message", "不能添加自己为好友");
            return ResponseEntity.badRequest().body(response);
        }
        friendService.addFriend(userId, friendId);
        response.put("success", true);
        response.put("message", "添加好友成功");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有好友
     * POST /auth/friends/list
     * 参数: userId
     * Content-Type: application/x-www-form-urlencoded
     */
    @PostMapping("/friends/list")
    public ResponseEntity<Map<String, Object>> getAllFriends(@RequestParam Long userId) {
        logger.info("获取好友列表: {}", userId);
        Map<String, Object> response = new HashMap<>();
        if (userId == null) {
            response.put("success", false);
            response.put("message", "userId不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        List<Long> friends = friendService.getAllFriends(userId);
        response.put("success", true);
        response.put("friends", friends);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除好友（双向）
     * POST /auth/friends/delete
     * 参数: userId, friendId
     * Content-Type: application/x-www-form-urlencoded
     */
    @PostMapping("/friends/delete")
    public ResponseEntity<Map<String, Object>> deleteFriend(@RequestParam Long userId, @RequestParam Long friendId) {
        logger.info("删除好友: {} <-> {}", userId, friendId);
        Map<String, Object> response = new HashMap<>();
        if (userId == null || friendId == null) {
            response.put("success", false);
            response.put("message", "userId和friendId不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        if (userId.equals(friendId)) {
            response.put("success", false);
            response.put("message", "不能删除自己");
            return ResponseEntity.badRequest().body(response);
        }
        friendService.deleteFriend(userId, friendId);
        response.put("success", true);
        response.put("message", "删除好友成功");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取两名用户之间所有聊天记录（双向）
     * POST /auth/chat/messages
     * 参数: userId1, userId2
     * Content-Type: application/x-www-form-urlencoded
     */
    @PostMapping("/chat/messages")
    public List<ChatMessageDTO> getChatMessagesBetweenUsers(@RequestParam Long userId1, @RequestParam Long userId2) {
        return chatMessageService.getMessagesBetweenUsers(userId1, userId2);
    }

} 
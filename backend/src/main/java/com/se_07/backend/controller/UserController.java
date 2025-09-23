package com.se_07.backend.controller;

import com.se_07.backend.dto.*;
import com.se_07.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取当前用户ID的辅助方法
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getSession().getAttribute("userId");
    }
    
    /**
     * 获取用户档案
     * GET /api/users/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * 更新用户档案
     * PUT /api/users/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @RequestBody UserProfileUpdateRequest request,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        UserProfileResponse profile = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * 设置旅行偏好
     * PUT /api/users/preferences
     */
    @PutMapping("/preferences")
    public ResponseEntity<UserProfileResponse> updateUserPreferences(
            @RequestBody UserPreferencesUpdateRequest request,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        UserProfileResponse profile = userService.updateUserPreferences(userId, request);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * 获取历史目的地
     * GET /api/users/destinations/history
     */
    @GetMapping("/destinations/history")
    public ResponseEntity<List<UserProfileResponse.HistoryDestinationDto>> getHistoryDestinations(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<UserProfileResponse.HistoryDestinationDto> destinations = userService.getHistoryDestinations(userId);
        return ResponseEntity.ok(destinations);
    }
    
    /**
     * 手动添加历史目的地
     * POST /api/users/destinations/history
     */
    @PostMapping("/destinations/history")
    public ResponseEntity<Map<String, Object>> addHistoryDestination(
            @RequestBody AddHistoryDestinationRequest request,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            userService.addHistoryDestination(userId, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "历史目的地添加成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }
    
    /**
     * 添加期望目的地
     * POST /api/users/destinations/wishlist
     */
    @PostMapping("/destinations/wishlist")
    public ResponseEntity<Map<String, Object>> addWishlistDestination(
            @RequestBody AddWishlistDestinationRequest request,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            userService.addWishlistDestination(userId, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "期望目的地添加成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    /**
     * 从已出行的行程中自动添加历史目的地
     * POST /api/users/destinations/history/auto-add
     */
    @PostMapping("/destinations/history/auto-add")
    public ResponseEntity<Map<String, Object>> addHistoryDestinationsFromCompletedItineraries(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            int addedCount = userService.addHistoryDestinationsFromCompletedItineraries(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("addedCount", addedCount);
            response.put("message", "成功从已出行的行程中添加了 " + addedCount + " 个历史目的地");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("addedCount", 0);
            response.put("message", "添加历史目的地时发生错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 删除期望目的地
     * DELETE /api/users/destinations/wishlist/{destinationId}
     */
    @DeleteMapping("/destinations/wishlist/{destinationId}")
    public ResponseEntity<Map<String, Object>> removeWishlistDestination(
            @PathVariable Long destinationId,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            userService.removeWishlistDestination(userId, destinationId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "期望目的地删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 删除指定行程中自动添加的历史目的地
     * DELETE /api/users/destinations/history/auto-remove/{itineraryId}
     */
    @DeleteMapping("/destinations/history/auto-remove/{itineraryId}")
    public ResponseEntity<Map<String, Object>> removeAutoAddedHistoryDestinationsFromItinerary(
            @PathVariable Long itineraryId,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            int removedCount = userService.removeAutoAddedHistoryDestinationsFromItinerary(userId, itineraryId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("removedCount", removedCount);
            response.put("message", "成功删除了 " + removedCount + " 个自动添加的历史目的地");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("removedCount", 0);
            response.put("message", "删除历史目的地时发生错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 上传用户头像
     */
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取当前用户ID
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "用户未登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // 上传头像
            String avatarUrl = userService.uploadAvatar(userId, file);
            
            response.put("success", true);
            response.put("message", "头像上传成功");
            response.put("avatarUrl", avatarUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "头像上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取用户旅行统计数据
     */
    @GetMapping("/travel-stats")
    public ResponseEntity<TravelStatsResponse> getTravelStats(HttpServletRequest request) {
        try {
            // 获取当前用户ID
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            TravelStatsResponse stats = userService.getTravelStats(userId);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取用户主页信息（包含公开行程）
     */
    @GetMapping("/homepage/{userId}")
    public ResponseEntity<UserHomepageResponse> getUserHomepage(
            @PathVariable Long userId, 
            HttpServletRequest request) {
        try {
            // 获取请求来源IP
            String requestIp = getClientIp(request);
            UserHomepageResponse homepage = userService.getUserHomepage(userId, requestIp);
            return ResponseEntity.ok(homepage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 用户搜索接口，模糊匹配用户名，返回基本信息列表（不含自己）
     * GET /users/search?username=xxx
     */
    @GetMapping("/search")
    public List<Map<String, Object>> searchUsers(@RequestParam String username, HttpSession session) {
        Long myId = (Long) session.getAttribute("userId");
        return userService.searchUsers(username, myId);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        return ip;
    }
} 
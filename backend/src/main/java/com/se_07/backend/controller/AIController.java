package com.se_07.backend.controller;

import com.se_07.backend.dto.AIRecommendationRequest;
import com.se_07.backend.dto.AIRecommendationResponse;
import com.se_07.backend.service.AIService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AIController {
    
    @Autowired
    private AIService aiService;
    
    /**
     * 生成个人档案AI推荐
     * POST /api/ai/profile-recommendation
     */
    @PostMapping("/profile-recommendation")
    public ResponseEntity<AIRecommendationResponse> generateProfileRecommendation(
            @RequestBody AIRecommendationRequest request,
            HttpSession session) {
        
        try {
            // 验证用户登录状态
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(
                    AIRecommendationResponse.error("用户未登录")
                );
            }

            // 设置请求类型
            request.setRequestType("general");
            
            // 调用AI服务生成推荐
            AIRecommendationResponse response = aiService.generateProfileRecommendation(request);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                AIRecommendationResponse.error("生成推荐失败：" + e.getMessage())
            );
        }
    }
    
    /**
     * 生成详细行程规划
     * POST /api/ai/itinerary-plan
     */
    @PostMapping("/itinerary-plan")
    public ResponseEntity<AIRecommendationResponse> generateItineraryPlan(
            @RequestBody AIRecommendationRequest request,
            HttpSession session) {
        
        try {
            // 验证用户登录状态
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(
                    AIRecommendationResponse.error("用户未登录")
                );
            }

            // 验证必要参数
            if (request.getDestination() == null || request.getDestination().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    AIRecommendationResponse.error("目的地不能为空")
                );
            }
            
            if (request.getDays() == null || request.getDays() <= 0) {
                return ResponseEntity.badRequest().body(
                    AIRecommendationResponse.error("旅行天数必须大于0")
                );
            }
            
            if (request.getTravelers() == null || request.getTravelers() <= 0) {
                return ResponseEntity.badRequest().body(
                    AIRecommendationResponse.error("旅行人数必须大于0")
                );
            }
            
            // 设置请求类型
            request.setRequestType("itinerary");
            
            // 调用AI服务生成行程
            AIRecommendationResponse response = aiService.generateItineraryPlan(request);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                AIRecommendationResponse.error("生成行程失败：" + e.getMessage())
            );
        }
    }
    
    /**
     * 测试AI服务状态
     * GET /api/ai/status
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("AI服务运行正常");
    }
}

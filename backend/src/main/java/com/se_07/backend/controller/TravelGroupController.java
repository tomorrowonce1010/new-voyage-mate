package com.se_07.backend.controller;

import com.se_07.backend.dto.CreateTravelGroupRequest;
import com.se_07.backend.dto.ItineraryCreateRequest;
import com.se_07.backend.dto.ItineraryDTO;
import com.se_07.backend.dto.TravelGroupDTO;
import com.se_07.backend.service.TravelGroupService;
import com.se_07.backend.service.ItineraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/group-travel")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class TravelGroupController {
    
    @Autowired
    private TravelGroupService travelGroupService;
    
    @Autowired
    private ItineraryService itineraryService;
    
    private Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
    
    /**
     * 获取所有公开的招募中组团
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> getPublicGroups(HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            List<TravelGroupDTO> groups = travelGroupService.getPublicRecruitingGroups(userId);
            return ResponseEntity.ok(createSuccessResponse(groups));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("获取公开组团失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取所有公开的招募中组团（带搜索功能）
     */
    @GetMapping("/public/search")
    public ResponseEntity<Map<String, Object>> searchPublicGroups(
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            List<TravelGroupDTO> groups = travelGroupService.getPublicRecruitingGroupsWithSearch(
                    userId, searchText, searchType, startDate, endDate);
            return ResponseEntity.ok(createSuccessResponse(groups));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("搜索公开组团失败：" + e.getMessage()));
        }
    }
    
    /**
     * 创建组团
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTravelGroup(
            @RequestBody CreateTravelGroupRequest request,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            TravelGroupDTO group = travelGroupService.createTravelGroup(request, userId);
            return ResponseEntity.ok(createSuccessResponse(group));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("创建组团失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取组团详情
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<Map<String, Object>> getGroupDetail(
            @PathVariable Long groupId,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            TravelGroupDTO group = travelGroupService.getGroupDetail(groupId, userId);
            return ResponseEntity.ok(createSuccessResponse(group));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("获取组团详情失败：" + e.getMessage()));
        }
    }
    
    /**
     * 申请加入组团
     * POST /api/group-travel/{groupId}/apply
     */
    @PostMapping("/{groupId}/apply")
    public ResponseEntity<Map<String, Object>> applyToJoinGroup(
            @PathVariable Long groupId,
            @RequestBody Map<String, String> request,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            String applicationMessage = request.get("applicationMessage");
            travelGroupService.applyToJoinGroup(groupId, userId, applicationMessage);
            return ResponseEntity.ok(createSuccessResponse("申请已提交"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("申请失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取用户创建的组团
     * GET /api/group-travel/my-created
     */
    @GetMapping("/my-created")
    public ResponseEntity<Map<String, Object>> getMyCreatedGroups(HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            List<TravelGroupDTO> groups = travelGroupService.getUserCreatedGroups(userId);
            return ResponseEntity.ok(createSuccessResponse(groups));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("获取我创建的组团失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取用户参与的组团
     * GET /api/group-travel/my-joined
     */
    @GetMapping("/my-joined")
    public ResponseEntity<Map<String, Object>> getMyJoinedGroups(HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            List<TravelGroupDTO> groups = travelGroupService.getUserJoinedGroups(userId);
            return ResponseEntity.ok(createSuccessResponse(groups));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("获取我参与的组团失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取组团的申请列表
     * GET /api/group-travel/{groupId}/applications
     */
    @GetMapping("/{groupId}/applications")
    public ResponseEntity<Map<String, Object>> getGroupApplications(
            @PathVariable Long groupId,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            List<Map<String, Object>> applications = travelGroupService.getGroupApplications(groupId, userId);
            return ResponseEntity.ok(createSuccessResponse(applications));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("获取申请列表失败：" + e.getMessage()));
        }
    }
    
    /**
     * 处理加入申请
     * POST /api/group-travel/{groupId}/applications/{applicationId}/process
     */
    @PostMapping("/{groupId}/applications/{applicationId}/process")
    public ResponseEntity<Map<String, Object>> processApplication(
            @PathVariable Long groupId,
            @PathVariable Long applicationId,
            @RequestParam String action,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            boolean approve = "approve".equals(action);
            travelGroupService.processApplication(groupId, applicationId, userId, approve);
            return ResponseEntity.ok(createSuccessResponse(approve ? "申请已通过" : "申请已拒绝"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("处理申请失败：" + e.getMessage()));
        }
    }
    
    /**
     * 创建团队行程
     * POST /api/group-travel/{groupId}/itinerary
     */
    @PostMapping("/{groupId}/itinerary")
    public ResponseEntity<Map<String, Object>> createGroupItinerary(
            @PathVariable Long groupId,
            @RequestBody ItineraryCreateRequest request,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            ItineraryDTO itinerary = itineraryService.createGroupItinerary(userId, groupId, request, false);
            return ResponseEntity.ok(createSuccessResponse(itinerary));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("创建团队行程失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取团队行程
     * GET /api/travel-groups/{groupId}/itinerary
     */
    @GetMapping("/{groupId}/itinerary")
    public ResponseEntity<Map<String, Object>> getGroupItinerary(
            @PathVariable Long groupId,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            List<ItineraryDTO> itineraries = itineraryService.getGroupItineraries(userId, groupId, false);
            // 返回第一个行程（通常一个组团只有一个行程）
            if (!itineraries.isEmpty()) {
                return ResponseEntity.ok(createSuccessResponse(itineraries.get(0)));
            } else {
                return ResponseEntity.ok(createSuccessResponse(null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("获取团队行程失败：" + e.getMessage()));
        }
    }

    /**
     * 获取推荐组团
     */
    @GetMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getRecommendedGroups(HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            List<TravelGroupDTO> groups = travelGroupService.getRecommendedGroups(userId);
            return ResponseEntity.ok(createSuccessResponse(groups));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("获取推荐组团失败：" + e.getMessage()));
        }
    }

    /**
     * 根据偏好获取推荐组团
     */
    @PostMapping("/recommendations-by-preferences")
    public ResponseEntity<Map<String, Object>> getRecommendationsByPreferences(
            @RequestBody Map<String, List<String>> request,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            List<String> preferences = request.get("preferences");
            List<TravelGroupDTO> groups = travelGroupService.getRecommendationsByPreferences(userId, preferences);
            return ResponseEntity.ok(createSuccessResponse(groups));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("获取推荐组团失败：" + e.getMessage()));
        }
    }

    /**
     * 获取用户在组团中的状态
     */
    @GetMapping("/{groupId}/user-status")
    public ResponseEntity<Map<String, Object>> getUserStatusInGroup(
            @PathVariable Long groupId,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            Map<String, Object> status = travelGroupService.getUserStatusInGroup(groupId, userId);
            return ResponseEntity.ok(createSuccessResponse(status));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("获取用户状态失败：" + e.getMessage()));
        }
    }

    /**
     * 更新组团状态
     */
    @PutMapping("/{groupId}/status")
    public ResponseEntity<Map<String, Object>> updateGroupStatus(
            @PathVariable Long groupId,
            @RequestBody Map<String, String> request,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            String status = request.get("status");
            TravelGroupDTO group = travelGroupService.updateGroupStatus(groupId, status, userId);
            return ResponseEntity.ok(createSuccessResponse(group));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("更新组团状态失败：" + e.getMessage()));
        }
    }

    /**
     * 撤回申请
     */
    @PostMapping("/{groupId}/applications/withdraw")
    public ResponseEntity<Map<String, Object>> withdrawApplication(
            @PathVariable Long groupId,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            travelGroupService.withdrawApplication(groupId, userId);
            return ResponseEntity.ok(createSuccessResponse("申请已撤回"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("撤回申请失败：" + e.getMessage()));
        }
    }

    /**
     * 退出组团
     */
    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Map<String, Object>> leaveGroup(
            @PathVariable Long groupId,
            HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("用户未登录"));
            }
            
            travelGroupService.leaveGroup(groupId, userId);
            return ResponseEntity.ok(createSuccessResponse("已退出组团"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("退出组团失败：" + e.getMessage()));
        }
    }
} 
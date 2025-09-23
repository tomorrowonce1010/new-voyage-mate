package com.se_07.backend.controller;

import com.se_07.backend.service.ItineraryService;
import com.se_07.backend.dto.ItineraryDTO;
import com.se_07.backend.entity.User;
import com.se_07.backend.dto.ItineraryCreateRequest;
import com.se_07.backend.dto.ItineraryUpdateRequest;
import com.se_07.backend.repository.UserRepository;
import com.se_07.backend.repository.ItineraryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.multipart.MultipartFile;
import com.se_07.backend.dto.ShareCodeRequest;
import com.se_07.backend.dto.ShareCodeResponse;
import com.se_07.backend.entity.Tag;
import com.se_07.backend.repository.TagRepository;
import java.util.ArrayList;
import java.util.Collections;
import com.se_07.backend.dto.PermissionStatusResponse;


@RestController
@RequestMapping("/itineraries")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ItineraryController {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryController.class);
    
    @Autowired
    private ItineraryService itineraryService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ItineraryRepository itineraryRepository;
    
    @Autowired
    private TagRepository tagRepository;
    

    
    /**
     * 创建新行程
     * POST /api/itineraries
     */
    @PostMapping
    public ResponseEntity<ItineraryDTO> createItinerary(
            @RequestBody ItineraryCreateRequest request,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        ItineraryDTO itinerary = itineraryService.createItinerary(userId, request);
        return ResponseEntity.ok(itinerary);
    }
    
    /**
     * 获取用户的行程列表
     * GET /api/itineraries/user
     */
    @GetMapping("/user")
    public ResponseEntity<List<ItineraryDTO>> getUserItineraries(HttpSession session,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            List<ItineraryDTO> itineraries = itineraryService.getUserItineraries(userId, pageable);
            return ResponseEntity.ok(itineraries);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 根据ID获取行程详情
     * GET /api/itineraries/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItineraryDTO> getItineraryById(
            @PathVariable Long id,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        ItineraryDTO itinerary = itineraryService.getItineraryById(id, userId);
        return ResponseEntity.ok(itinerary);
    }
    
    /**
     * 更新行程信息
     * PUT /api/itineraries/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ItineraryDTO> updateItinerary(
            @PathVariable Long id,
            @RequestBody ItineraryUpdateRequest request,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        ItineraryDTO itinerary = itineraryService.updateItinerary(id, userId, request);
        return ResponseEntity.ok(itinerary);
    }
    
    /**
     * 删除行程
     * DELETE /api/itineraries/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteItinerary(
            @PathVariable Long id,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        logger.debug("删除行程 - 用户ID: {}, 行程ID: {}", userId, id);
        
        if (userId == null) {
            logger.warn("删除行程失败 - 用户未登录");
            return ResponseEntity.status(401)
                .body(java.util.Collections.singletonMap("message", "请先登录"));
        }
        
        try {
            itineraryService.deleteItinerary(userId, id);
            logger.info("行程删除成功 - 用户ID: {}, 行程ID: {}", userId, id);
            return ResponseEntity.ok(java.util.Collections.singletonMap("message", "删除成功"));
        } catch (Exception e) {
            logger.error("删除行程时发生错误 - 用户ID: {}, 行程ID: {}, 错误: {}", userId, id, e.getMessage());
            return ResponseEntity.status(500)
                .body(java.util.Collections.singletonMap("message", e.getMessage()));
        }
    }
    
    /**
     * 锁定行程（设为已出行）
     * PUT /api/itineraries/{id}/lock
     */
    @PutMapping("/{id}/lock")
    public ResponseEntity<ItineraryDTO> lockItinerary(
            @PathVariable Long id,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        ItineraryDTO itinerary = itineraryService.lockItinerary(id, userId);
        return ResponseEntity.ok(itinerary);
    }
    
    /**
     * 获取用户的待出行行程
     * GET /api/itineraries/user/pending
     */
    @GetMapping("/user/pending")
    public ResponseEntity<List<ItineraryDTO>> getPendingItineraries(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<ItineraryDTO> itineraries = itineraryService.getPendingItineraries(userId);
        return ResponseEntity.ok(itineraries);
    }
    
    /**
     * 获取用户的已出行行程
     * GET /api/itineraries/user/completed
     */
    @GetMapping("/user/completed")
    public ResponseEntity<List<ItineraryDTO>> getCompletedItineraries(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<ItineraryDTO> itineraries = itineraryService.getCompletedItineraries(userId);
        return ResponseEntity.ok(itineraries);
    }
    
    /**
     * 更新行程权限状态
     * PUT /api/itineraries/{id}/permission
     */
    @PutMapping("/{id}/permission")
    public ResponseEntity<?> updatePermissionStatus(@PathVariable Long id, 
                                                   @RequestParam String permissionStatus,
                                                   HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            PermissionStatusResponse response = itineraryService.updatePermissionStatus(id, userId, permissionStatus);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    /**
     * 测试端点 - 检查用户状态和数据库连接
     * GET /api/itineraries/test
     */
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint(HttpSession session) {
        try {
            logger.info("测试端点被调用");
            
            Long userId = (Long) session.getAttribute("userId");
            logger.info("Session中的userId: {}", userId);
            
            // 检查所有session属性
            java.util.Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String name = attributeNames.nextElement();
                logger.info("Session属性: {} = {}", name, session.getAttribute(name));
            }
            
            // 测试数据库连接
            if (userId != null) {
                try {
                    User user = userRepository.findById(userId).orElse(null);
                    logger.info("数据库中找到用户: {}", user != null ? user.getEmail() : "null");
                } catch (Exception dbError) {
                    logger.error("数据库查询用户失败", dbError);
                }
            }
            
            return ResponseEntity.ok().body("{\n" +
                    "  \"status\": \"success\",\n" +
                    "  \"userId\": " + userId + ",\n" +
                    "  \"sessionId\": \"" + session.getId() + "\",\n" +
                    "  \"message\": \"测试端点工作正常\"\n" +
                    "}");
        } catch (Exception e) {
            logger.error("测试端点异常", e);
            return ResponseEntity.status(500).body("{\n" +
                    "  \"status\": \"error\",\n" +
                    "  \"message\": \"" + e.getMessage() + "\"\n" +
                    "}");
        }
    }
    
    /**
     * 测试创建行程（用于调试）
     * POST /api/itineraries/test
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testCreateItinerary(
            @RequestBody Map<String, Object> testData,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "测试接口");
        response.put("userId", userId);
        response.put("testData", testData);
        response.put("travelerCount", testData.get("travelerCount"));
        response.put("travelerCountType", testData.get("travelerCount") != null ? testData.get("travelerCount").getClass().getName() : "null");

        return ResponseEntity.ok(response);
    }

    /**
     * 健康检查端点 - 不需要登录
     * GET /api/itineraries/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            logger.info("健康检查端点被调用");
            
            // 测试数据库连接
            long userCount = userRepository.count();
            logger.info("数据库中的用户总数: {}", userCount);
            
            // 测试行程表
            long itineraryCount = -1;
            String itineraryError = "none";
            try {
                itineraryCount = itineraryRepository.count();
                logger.info("数据库中的行程总数: {}", itineraryCount);
            } catch (Exception e) {
                itineraryError = e.getMessage();
                logger.error("查询行程表失败", e);
            }
            
            return ResponseEntity.ok().body("{\n" +
                    "  \"status\": \"healthy\",\n" +
                    "  \"database\": \"connected\",\n" +
                    "  \"userCount\": " + userCount + ",\n" +
                    "  \"itineraryCount\": " + itineraryCount + ",\n" +
                    "  \"itineraryError\": \"" + itineraryError + "\",\n" +
                    "  \"message\": \"服务运行正常\"\n" +
                    "}");
        } catch (Exception e) {
            logger.error("健康检查异常", e);
            return ResponseEntity.status(500).body("{\n" +
                    "  \"status\": \"error\",\n" +
                    "  \"database\": \"disconnected\",\n" +
                    "  \"message\": \"" + e.getMessage() + "\"\n" +
                    "}");
        }
    }
    
    /**
     * 更新行程基本信息
     * PUT /api/itineraries/{id}/basic
     */
    @PutMapping("/{id}/basic")
    public ResponseEntity<ItineraryDTO> updateItineraryBasic(@PathVariable Long id,
                                                            @RequestBody Map<String, Object> updates,
                                                            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ItineraryDTO updated = itineraryService.updateItineraryBasic(userId, id, updates);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 更新行程状态
     * PUT /api/itineraries/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ItineraryDTO> updateItineraryStatus(@PathVariable Long id,
                                                             @RequestParam String status,
                                                             HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ItineraryDTO updated = itineraryService.updateItineraryStatus(userId, id, status);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 上传行程封面图片
     * POST /api/itineraries/{id}/cover
     */
    @PostMapping("/{id}/cover")
    public ResponseEntity<Map<String, String>> uploadCoverImage(@PathVariable Long id,
                                                               @RequestParam("file") MultipartFile file,
                                                               HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            String imageUrl = itineraryService.uploadCoverImage(userId, id, file);
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 更新日程标题
     * PUT /api/itineraries/{itineraryId}/days/{dayId}/title
     */
    @PutMapping("/{itineraryId}/days/{dayId}/title")
    public ResponseEntity<Map<String, String>> updateDayTitle(@PathVariable Long itineraryId,
                                                             @PathVariable Long dayId,
                                                             @RequestBody Map<String, String> request,
                                                             HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            String newTitle = request.get("title");
            itineraryService.updateDayTitle(userId, itineraryId, dayId, newTitle);
            Map<String, String> response = new HashMap<>();
            response.put("title", newTitle);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 设置行程编辑状态为完成
     * PUT /api/itineraries/{id}/edit-complete
     */
    @PutMapping("/{id}/edit-complete")
    public ResponseEntity<ItineraryDTO> setEditComplete(@PathVariable Long id,
                                                       HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ItineraryDTO updated = itineraryService.setEditComplete(userId, id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 更新行程日期
     * PUT /api/itineraries/{id}/shift
     */
    @PutMapping("/{id}/shift")
    public ResponseEntity<ItineraryDTO> shiftItineraryDates(@PathVariable Long id,
                                                            @RequestBody Map<String, String> request,
                                                            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            String dateStr = request.get("newStartDate");
            java.time.LocalDate newStartDate = java.time.LocalDate.parse(dateStr);
            ItineraryDTO updated = itineraryService.shiftItineraryDates(userId, id, newStartDate);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 生成行程分享码
     * POST /api/itineraries/{id}/share
     */
    @PostMapping("/{id}/share")
    public ResponseEntity<ShareCodeResponse> generateShareCode(@PathVariable Long id, 
                                             @RequestBody ShareCodeRequest request,
                                             HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            logger.error("用户未登录，无法生成分享码");
            return ResponseEntity.status(401).build();
        }
        
        try {
            logger.info("开始生成分享码 - 用户ID: {}, 行程ID: {}, 描述: {}", userId, id, request.getDescription());
            String shareCode = itineraryService.generateShareCode(id, userId, request);
            logger.info("分享码生成成功 - 用户ID: {}, 行程ID: {}, 分享码: {}", userId, id, shareCode);
            return ResponseEntity.ok(new ShareCodeResponse(shareCode));
        } catch (Exception e) {
            logger.error("生成分享码失败 - 用户ID: {}, 行程ID: {}, 错误: {}", userId, id, e.getMessage());
            return ResponseEntity.badRequest().body(new ShareCodeResponse(null, e.getMessage()));
        }
    }
    
    /**
     * 调试：检查所有社区条目的索引状态
     * GET /api/itineraries/debug/check-index-status
     */
    @GetMapping("/debug/check-index-status")
    public ResponseEntity<String> checkIndexStatus(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            itineraryService.checkAllCommunityEntriesIndexStatus();
            return ResponseEntity.ok("检查完成，请查看日志");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 调试：检查Elasticsearch中的文档数量
     * GET /api/itineraries/debug/check-es-count
     */
    @GetMapping("/debug/check-es-count")
    public ResponseEntity<String> checkElasticsearchCount(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            itineraryService.checkElasticsearchDocumentCount();
            return ResponseEntity.ok("检查完成，请查看日志");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试端点（不需要登录）
     * GET /api/itineraries/debug/test
     */
    @GetMapping("/debug/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("调试端点工作正常");
    }
    
    /**
     * 调试：检查Elasticsearch中的文档数量（不需要登录）
     * GET /api/itineraries/debug/check-es-count-public
     */
    @GetMapping("/debug/check-es-count-public")
    public ResponseEntity<String> checkElasticsearchCountPublic() {
        try {
            itineraryService.checkElasticsearchDocumentCount();
            return ResponseEntity.ok("检查完成，请查看日志");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("检查失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> getAvailableTags(HttpSession session) {
        try {
            // 获取前30个标签
            List<Tag> tags = tagRepository.findTop30ByOrderByIdAsc();
            if (tags == null) {
                tags = new ArrayList<>();
            }
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            logger.error("获取标签失败: ", e);
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * 导入AI生成的行程
     * POST /api/itineraries/import-ai
     */
    @PostMapping("/import-ai")
    public ResponseEntity<?> importAIItinerary(
            @RequestBody com.se_07.backend.dto.AIItineraryImportRequest importRequest,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "请先登录"));
        }

        try {
            logger.info("导入AI行程请求 - 用户ID: {}, 行程标题: {}", userId, importRequest.getTitle());

            ItineraryDTO itinerary = itineraryService.importAIItinerary(userId, importRequest);

            logger.info("AI行程导入成功 - 用户ID: {}, 行程ID: {}", userId, itinerary.getId());

            return ResponseEntity.ok(itinerary);
        } catch (Exception e) {
            logger.error("导入AI行程失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "导入失败: " + e.getMessage()));
        }
    }

    /**
     * 创建团队行程
     * POST /api/itineraries/group/{groupId}
     */
    @PostMapping("/group/{groupId}")
    public ResponseEntity<ItineraryDTO> createGroupItinerary(
            @PathVariable Long groupId,
            @RequestBody ItineraryCreateRequest request,
            @RequestParam(required = false) Boolean isTemplate,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        ItineraryDTO itinerary = itineraryService.createGroupItinerary(userId, groupId, request, isTemplate != null && isTemplate);
        return ResponseEntity.ok(itinerary);
    }

    /**
     * 获取团队行程列表
     * GET /api/itineraries/group/{groupId}
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ItineraryDTO>> getGroupItineraries(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "false") Boolean templatesOnly,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<ItineraryDTO> itineraries = itineraryService.getGroupItineraries(userId, groupId, templatesOnly);
        return ResponseEntity.ok(itineraries);
    }

    /**
     * 将行程设置为团队模板
     * PUT /api/itineraries/group/{groupId}/{itineraryId}/template
     */
    @PutMapping("/group/{groupId}/{itineraryId}/template")
    public ResponseEntity<ItineraryDTO> setAsTemplate(
            @PathVariable Long groupId,
            @PathVariable Long itineraryId,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        ItineraryDTO itinerary = itineraryService.setAsGroupTemplate(userId, groupId, itineraryId);
        return ResponseEntity.ok(itinerary);
    }

    /**
     * 获取用户的个人行程列表
     * GET /api/itineraries/user/personal
     */
    @GetMapping("/user/personal")
    public ResponseEntity<List<ItineraryDTO>> getPersonalItineraries(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            List<ItineraryDTO> itineraries = itineraryService.getPersonalItineraries(userId, pageable);
            return ResponseEntity.ok(itineraries);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 获取用户的团队行程列表
     * GET /api/itineraries/user/team
     */
    @GetMapping("/user/team")
    public ResponseEntity<List<ItineraryDTO>> getTeamItineraries(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            List<ItineraryDTO> itineraries = itineraryService.getTeamItineraries(userId, pageable);
            return ResponseEntity.ok(itineraries);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

}
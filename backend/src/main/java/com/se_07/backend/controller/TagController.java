package com.se_07.backend.controller;

import com.se_07.backend.entity.Tag;
import com.se_07.backend.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tags")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class TagController {
    
    @Autowired
    private TagRepository tagRepository;
    
    /**
     * 获取所有标签
     * GET /api/tags
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTags() {
        try {
            List<Tag> tags = tagRepository.findAll();
            return ResponseEntity.ok(createSuccessResponse(tags));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("获取标签失败：" + e.getMessage()));
        }
    }
    
    // 工具方法
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
} 
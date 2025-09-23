package com.se_07.backend.controller;

import com.se_07.backend.service.CommunityService;
import com.se_07.backend.service.SemanticSearchService;
import com.se_07.backend.repository.CommunityEntryRepository;
import com.se_07.backend.entity.CommunityEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@RestController
@RequestMapping("/community")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class CommunityController {
    
    private static final Logger logger = LoggerFactory.getLogger(CommunityController.class);
    
    @Autowired
    private CommunityService communityService;
    
    @Autowired
    private SemanticSearchService semanticSearchService;
    
    @Autowired
    private CommunityEntryRepository communityEntryRepository;
    
    /**
     * 获取公共社区条目
     * GET /api/community/public
     */
    @GetMapping("/public")
    public ResponseEntity<List<Map<String, Object>>> getPublicCommunityEntries() {
        try {
            List<Map<String, Object>> entries = communityService.getPublicCommunityEntries();
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            logger.error("获取公共社区条目失败: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 根据分享码获取社区条目
     * GET /api/community/share/{shareCode}
     */
    @GetMapping("/share/{shareCode}")
    public ResponseEntity<Map<String, Object>> getCommunityEntryByShareCode(@PathVariable String shareCode) {
        try {
            Map<String, Object> entry = communityService.getCommunityEntryByShareCode(shareCode);
            if (entry == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(entry);
        } catch (Exception e) {
            logger.error("根据分享码获取社区条目失败: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 搜索社区条目 - 按行程名称、描述、目的地搜索
     * GET /api/community/search?q={searchTerm}
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchCommunityEntries(@RequestParam String q) {
        try {
            List<Map<String, Object>> entries = communityService.searchCommunityEntries(q);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            logger.error("搜索社区条目失败: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 增加社区条目的查看次数
     * POST /api/community/{id}/view
     */
    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long id) {
        try {
            communityService.incrementViewCount(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("增加查看次数失败: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 根据行程ID获取社区条目（用于前端检查是否已分享）
     * GET /api/community/itinerary/{itineraryId}
     */
    @GetMapping("/itinerary/{itineraryId}")
    public ResponseEntity<Map<String, Object>> getCommunityEntryByItineraryId(@PathVariable Long itineraryId) {
        try {
            logger.info("查询行程ID {} 的社区条目", itineraryId);
            Optional<CommunityEntry> entryOpt = communityEntryRepository.findByItineraryId(itineraryId);
            if (!entryOpt.isPresent()) {
                logger.info("行程ID {} 没有找到对应的社区条目", itineraryId);
                return ResponseEntity.notFound().build();
            }
            CommunityEntry entry = entryOpt.get();
            Map<String, Object> result = new HashMap<>();
            result.put("id", entry.getId());
            result.put("shareCode", entry.getShareCode());
            logger.info("找到行程ID {} 的社区条目，分享码: {}", itineraryId, entry.getShareCode());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("根据行程ID获取社区条目失败 - 行程ID: {}, 错误: {}", itineraryId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 获取热门标签
     * GET /api/community/popular/tags?limit=10
     */
    @GetMapping("/popular/tags")
    public ResponseEntity<List<Map<String,Object>>> getPopularTags(@RequestParam(defaultValue="10") int limit) {
        try {
            List<Map<String,Object>> tags = communityService.getPopularTags(limit);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            logger.error("获取热门标签失败: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 获取热门作者
     * GET /api/community/popular/authors?limit=5
     */
    @GetMapping("/popular/authors")
    public ResponseEntity<List<Map<String,Object>>> getPopularAuthors(@RequestParam(defaultValue="5") int limit) {
        try {
            List<Map<String,Object>> authors = communityService.getPopularAuthors(limit);
            return ResponseEntity.ok(authors);
        } catch (Exception e) {
            logger.error("获取热门作者失败: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 社区条目语义搜索
     * GET /api/community/semantic/search?q={query}&page={page}&size={size}
     */
    @GetMapping("/semantic/search")
    public ResponseEntity<Map<String, Object>> semanticSearchCommunityEntries(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            var result = semanticSearchService.semanticSearchCommunityEntries(q, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("content", result.getContent());
            response.put("totalElements", result.getTotalElements());
            response.put("totalPages", result.getTotalPages());
            response.put("currentPage", result.getNumber());
            response.put("size", result.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("社区条目语义搜索失败: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 社区条目语义搜索+标签组合搜索
     * GET /api/community/semantic/search/tags?q={query}&tags={tags}&page={page}&size={size}
     */
    @GetMapping("/semantic/search/tags")
    public ResponseEntity<Map<String, Object>> semanticSearchCommunityEntriesByTags(
            @RequestParam String q,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            var result = semanticSearchService.semanticSearchCommunityEntriesByTags(q, tags != null ? tags : List.of(), page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("content", result.getContent());
            response.put("totalElements", result.getTotalElements());
            response.put("totalPages", result.getTotalPages());
            response.put("currentPage", result.getNumber());
            response.put("size", result.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("社区条目语义搜索+标签组合搜索失败: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 按目的地关键词搜索社区条目
     * GET /api/community/search/destination?destination={destination}&page={page}&size={size}
     */
    @GetMapping("/search/destination")
    public ResponseEntity<Map<String, Object>> searchCommunityEntriesByDestination(
            @RequestParam String destination,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Map<String, Object> result = communityService.searchCommunityEntriesByDestination(destination, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("按目的地搜索社区条目失败: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 作者语义搜索
     * GET /api/community/semantic/search/authors?q={query}&page={page}&size={size}
     */
    @GetMapping("/semantic/search/authors")
    public ResponseEntity<Map<String, Object>> semanticSearchAuthors(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            var result = semanticSearchService.semanticSearchAuthors(q, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("content", result.getContent());
            response.put("totalElements", result.getTotalElements());
            response.put("totalPages", result.getTotalPages());
            response.put("currentPage", result.getNumber());
            response.put("size", result.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("作者语义搜索失败: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 按标签筛选社区条目
     * GET /api/community/search/tags?tags={tag1}&tags={tag2}&page={page}&size={size}
     */
    @GetMapping("/search/tags")
    public ResponseEntity<Map<String, Object>> filterCommunityEntriesByTags(
            @RequestParam List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            var result = semanticSearchService.filterCommunityEntriesByTags(tags, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("content", result.getContent());
            response.put("totalElements", result.getTotalElements());
            response.put("totalPages", result.getTotalPages());
            response.put("currentPage", result.getNumber());
            response.put("size", result.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("按标签筛选社区条目失败: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 获取公共社区条目（支持排序）
     * GET /api/community/public/sorted?sortBy={sortBy}&page={page}&size={size}
     */
    @GetMapping("/public/sorted")
    public ResponseEntity<Map<String, Object>> getPublicCommunityEntriesWithSort(
            @RequestParam(defaultValue = "time") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Map<String, Object> result = communityService.getPublicCommunityEntriesWithSort(sortBy, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取排序社区条目失败: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
} 
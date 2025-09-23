package com.se_07.backend.controller;

import com.se_07.backend.entity.Attraction;
import com.se_07.backend.service.AttractionService;
import com.se_07.backend.service.SemanticSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/attractions")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AttractionController {
    
    @Autowired
    private AttractionService attractionService;
    
    @Autowired
    private SemanticSearchService semanticSearchService;
    
    /**
     * 获取热门景点列表
     * GET /api/attractions/hot
     */
    @GetMapping("/hot")
    public ResponseEntity<Page<Attraction>> getHotAttractions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Attraction> attractions = attractionService.getHotAttractions(pageable);
        return ResponseEntity.ok(attractions);
    }
    
    /**
     * 根据ID获取景点详情
     * GET /api/attractions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Attraction> getAttractionById(@PathVariable Long id) {
        Optional<Attraction> attraction = attractionService.getAttractionById(id);
        return attraction.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据目的地ID获取景点列表
     * GET /api/attractions/destination/{destinationId}
     */
    @GetMapping("/destination/{destinationId}")
    public ResponseEntity<Page<Attraction>> getAttractionsByDestinationId(
            @PathVariable Long destinationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Attraction> attractions = attractionService.getAttractionsByDestinationWithFilters(destinationId, tag, keyword, pageable);
        return ResponseEntity.ok(attractions);
    }
    
    /**
     * 根据分类获取景点列表
     * GET /api/attractions/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<Attraction>> getAttractionsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Attraction.AttractionCategory attractionCategory = Attraction.AttractionCategory.valueOf(category);
            Pageable pageable = PageRequest.of(page, size);
            Page<Attraction> attractions = attractionService.getAttractionsByCategory(attractionCategory, pageable);
            return ResponseEntity.ok(attractions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 搜索景点（按名称模糊匹配）
     * GET /api/attractions/search?keyword=xxx&limit=10
     */
    @GetMapping("/search")
    public ResponseEntity<java.util.List<Attraction>> searchAttractions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") Integer limit) {
        java.util.List<Attraction> attractions = attractionService.searchAttractionsByName(keyword, limit);
        return ResponseEntity.ok(attractions);
    }
    
    /**
     * 获取景点的热门标签
     * GET /api/attractions/{id}/top-tags
     */
    @GetMapping("/{id}/top-tags")
    public ResponseEntity<List<String>> getAttractionTopTags(
            @PathVariable Long id,
            @RequestParam(defaultValue = "3") int count) {
        Optional<Attraction> attractionOpt = attractionService.getAttractionById(id);
        if (!attractionOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        List<String> topTags = attractionService.getTopNTags(attractionOpt.get(), count);
        return ResponseEntity.ok(topTags);
    }
    
    /**
     * 增加景点被加入行程的次数
     * POST /api/attractions/{id}/increment-join-count
     */
    @PostMapping("/{id}/increment-join-count")
    public ResponseEntity<Void> incrementJoinCount(@PathVariable Long id) {
        try {
            attractionService.incrementJoinCount(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 景点语义搜索
     * GET /api/attractions/semantic-search/{destinationId}
     */
    @GetMapping("/semantic-search/{destinationId}")
    public ResponseEntity<org.springframework.data.domain.Page<Attraction>> semanticSearch(
            @PathVariable Long destinationId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        org.springframework.data.domain.Page<Attraction> results = semanticSearchService.semanticSearchAttractions(destinationId, keyword, page, size);
        return ResponseEntity.ok(results);
    }
    
    /**
     * 景点语义搜索+标签组合
     * GET /api/attractions/semantic-search-by-tags/{destinationId}
     */
    @GetMapping("/semantic-search-by-tags/{destinationId}")
    public ResponseEntity<org.springframework.data.domain.Page<Attraction>> semanticSearchByTags(
            @PathVariable Long destinationId,
            @RequestParam String keyword,
            @RequestParam List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        org.springframework.data.domain.Page<Attraction> results = semanticSearchService.semanticSearchAttractionsByTags(destinationId, keyword, tags, page, size);
        return ResponseEntity.ok(results);
    }
} 
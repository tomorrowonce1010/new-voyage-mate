package com.se_07.backend.controller;

import com.se_07.backend.entity.Destination;
import com.se_07.backend.service.DestinationService;
import com.se_07.backend.service.SemanticSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/destinations")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class DestinationController {

    @Autowired
    private DestinationService destinationService;

    @Autowired
    private SemanticSearchService semanticSearchService;

    /**
     * 获取热门目的地列表
     * GET /api/destinations/hot
     */
    @GetMapping("/hot")
    public ResponseEntity<Page<Destination>> getHotDestinations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Destination> destinations = destinationService.getHotDestinations(pageable);
        return ResponseEntity.ok(destinations);
    }

    /**
     * 根据ID获取目的地详情
     * GET /api/destinations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Destination> getDestinationById(@PathVariable Long id) {
        Destination destination = destinationService.getDestinationById(id);
        return ResponseEntity.ok(destination);
    }

    /**
     * 搜索目的地
     * GET /api/destinations/search
     */
    @GetMapping("/search")
    public ResponseEntity<Page<Destination>> searchDestinations(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Destination> destinations = destinationService.searchDestinations(keyword, pageable);
        return ResponseEntity.ok(destinations);
    }



    @GetMapping("/filter-by-tags")
    public ResponseEntity<Page<Destination>> getDestinationsByTags(
            @RequestParam List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Destination> destinations = destinationService.getDestinationsByTags(tags, pageable);
        return ResponseEntity.ok(destinations);
    }

    /**
     * 组合搜索（标签+关键词）
     * GET /api/destinations/search-by-tags-and-keyword
     */
    @GetMapping("/search-by-tags-and-keyword")
    public ResponseEntity<Page<Destination>> searchDestinationsByTagsAndKeyword(
            @RequestParam List<String> tags,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Destination> destinations = destinationService.searchDestinationsByTagsAndKeyword(tags, keyword, pageable);
        return ResponseEntity.ok(destinations);
    }

    /**
     * 模糊搜索目的地名称
     * GET /api/destinations/name
     */
    @GetMapping("/name")
    public ResponseEntity<List<Destination>> searchDestinationsByName(@RequestParam String name) {
        List<Destination> destinations = destinationService.searchDestinationsByName(name);
        return ResponseEntity.ok(destinations);
    }

    /**
     * 语义搜索目的地
     * GET /api/destinations/semantic-search
     */
    @GetMapping("/semantic-search")
    public ResponseEntity<List<Destination>> semanticSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int size) {
        List<Destination> results = semanticSearchService.semanticSearch(keyword, size);
        return ResponseEntity.ok(results);
    }

    /**
     * 语义搜索+标签组合
     * GET /api/destinations/semantic-search-by-tags
     */
    @GetMapping("/semantic-search-by-tags")
    public ResponseEntity<List<Destination>> semanticSearchByTags(
            @RequestParam String keyword,
            @RequestParam List<String> tags,
            @RequestParam(defaultValue = "10") int size) {
        List<Destination> results = semanticSearchService.semanticSearchByTags(keyword, tags, size);
        return ResponseEntity.ok(results);
    }

    /**
     * 获取所有标签
     * GET /api/destinations/tags
     */
    @GetMapping("/tags")
    public ResponseEntity<List<String>> getAllTags() {
        List<String> tags = destinationService.getAllTags();
        return ResponseEntity.ok(tags);
    }



    /**
     * 获取目的地的热门标签
     * GET /api/destinations/{id}/top-tags
     */
    @GetMapping("/{id}/top-tags")
    public ResponseEntity<List<String>> getTopTags(
            @PathVariable Long id,
            @RequestParam(defaultValue = "6") int count) {
        Destination destination = destinationService.getDestinationById(id);
        if (destination == null) {
            return ResponseEntity.notFound().build();
        }
        List<String> topTags = destinationService.getTopNTags(destination, count);
        return ResponseEntity.ok(topTags);
    }


} 
package com.se_07.backend.service.impl;

import com.se_07.backend.entity.Attraction;
import com.se_07.backend.repository.AttractionRepository;
import com.se_07.backend.repository.TagRepository;
import com.se_07.backend.entity.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.se_07.backend.service.AttractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttractionServiceImpl implements AttractionService {

    @Autowired
    private AttractionRepository attractionRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TagRepository tagRepository;

    @Override
    public Optional<Attraction> getAttractionById(Long id) {
        return attractionRepository.findById(id);
    }

    @Override
    public Page<Attraction> getAttractionsByDestinationId(Long destinationId, Pageable pageable) {
        return attractionRepository.findByDestinationIdOrderByJoinCountDesc(destinationId, pageable);
    }

    @Override
    public List<String> getTopNTags(Attraction attraction, int n) {
        if (attraction == null || attraction.getTagScores() == null || attraction.getTagScores().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(attraction.getTagScores());
            Map<String, Double> idToScore = new java.util.HashMap<>();

            if (root.isObject()) {
                // {"7":37, "12":10, ...}
                root.fields().forEachRemaining(entry -> {
                    if (entry.getValue().isNumber()) {
                        idToScore.put(entry.getKey(), entry.getValue().asDouble());
                    }
                });
            } else if (root.isArray()) {
                // [0,0,0,37,...]  index+1 == tagId
                int idx = 0;
                for (com.fasterxml.jackson.databind.JsonNode node : root) {
                    if (node.isNumber()) {
                        double score = node.asDouble();
                        if (score > 0) {
                            idToScore.put(String.valueOf(idx + 1), score); // tag IDs start at 1
                        }
                    }
                    idx++;
                }
            } else {
                return java.util.Collections.emptyList();
            }

            // 获取权重最高的N个标签ID
            List<Long> topTagIds = idToScore.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(n)
                    .map(e -> Long.valueOf(e.getKey()))
                    .collect(Collectors.toList());

            if (topTagIds.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            Map<Long, String> idToTag = tagRepository.findAllById(topTagIds)
                    .stream()
                    .collect(Collectors.toMap(Tag::getId, Tag::getTag));

            return topTagIds.stream()
                    .map(id -> idToTag.getOrDefault(id, ""))
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("解析景点标签权重JSON失败: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public Page<Attraction> getAttractionsByCategory(Attraction.AttractionCategory category, Pageable pageable) {
        return attractionRepository.findByCategoryOrderByJoinCountDesc(category, pageable);
    }

    @Override
    public List<Attraction> searchAttractions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        try {
            // 优先使用全文搜索，如果失败则使用LIKE搜索
            return attractionRepository.searchByKeyword(keyword.trim());
        } catch (Exception e) {
            // 全文搜索失败，使用LIKE搜索作为备选
            System.err.println("全文搜索失败，使用LIKE搜索: " + e.getMessage());
            return attractionRepository.findByNameContainingOrDescriptionContaining(keyword.trim());
        }
    }

    @Override
    public void incrementJoinCount(Long attractionId) {
        Optional<Attraction> attractionOpt = attractionRepository.findById(attractionId);
        if (attractionOpt.isPresent()) {
            Attraction attraction = attractionOpt.get();
            attraction.setJoinCount((attraction.getJoinCount() == null ? 0 : attraction.getJoinCount()) + 1);
            attractionRepository.save(attraction);
        }
    }

    @Override
    public Page<Attraction> getHotAttractions(Pageable pageable) {
        return attractionRepository.findAllByOrderByJoinCountDesc(pageable);
    }

    @Override
    public java.util.List<Attraction> searchAttractionsByName(String keyword, Integer limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        if (limit == null || limit <= 0) {
            limit = 10; // 默认最多返回10个结果
        }
        
        return attractionRepository.findByNameContainingIgnoreCaseOrderByJoinCountDesc(
            keyword.trim(), 
            org.springframework.data.domain.PageRequest.of(0, limit)
        ).getContent();
    }

    @Override
    public Page<Attraction> getAttractionsByDestinationWithFilters(Long destinationId, String tagParam, String keyword, Pageable pageable) {
        List<Attraction> attractions = attractionRepository.findByDestinationIdOrderByJoinCountDesc(destinationId);

        // 标签过滤
        if (tagParam != null && !tagParam.trim().isEmpty()) {
            java.util.List<String> tagList = java.util.Arrays.stream(tagParam.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!tagList.isEmpty()) {
                attractions = attractions.stream()
                        .filter(a -> {
                            List<String> topTags = getTopNTags(a, 3);
                            // 需要包含 tagList 中所有标签才通过
                            return tagList.stream().allMatch(topTags::contains);
                        })
                        .collect(Collectors.toList());
            }
        }

        // 关键词过滤
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            attractions = attractions.stream()
                    .filter(a -> {
                        String name = a.getName() != null ? a.getName().toLowerCase() : "";
                        String desc = a.getDescription() != null ? a.getDescription().toLowerCase() : "";
                        return name.contains(kw) || desc.contains(kw);
                    })
                    .collect(Collectors.toList());
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), attractions.size());
        List<Attraction> pageContent = start >= attractions.size() ? java.util.Collections.emptyList() : attractions.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, attractions.size());
    }
} 
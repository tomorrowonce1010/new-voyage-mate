package com.se_07.backend.service.impl;

import com.se_07.backend.entity.Destination;
import com.se_07.backend.repository.DestinationRepository;
import com.se_07.backend.service.DestinationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.se_07.backend.entity.Tag;
import com.se_07.backend.repository.TagRepository;

@Service
public class DestinationServiceImpl implements DestinationService {

    @Autowired
    private DestinationRepository destinationRepository;
    
    @Autowired
    private TagRepository tagRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Page<Destination> getHotDestinations(Pageable pageable) {
        return destinationRepository.findAllByOrderByJoinCountDesc(pageable);
    }

    @Override
    public Destination getDestinationById(Long id) {
        return destinationRepository.findById(id).orElse(null);
    }

    @Override
    public List<Destination> searchDestinations(String keyword) {
        // 使用LIKE查询进行关键词搜索，支持中文和英文
        // 搜索范围包括目的地和景点的名称、描述
        System.out.println("searchDestinations: " + keyword);
        List<Destination> destinations = destinationRepository.searchByKeyword(keyword);
        System.out.println("destinations size: " + destinations.size());
        return destinations;
    }

    @Override
    public Page<Destination> searchDestinations(String keyword, Pageable pageable) {
        // 使用LIKE查询进行关键词搜索，支持中文和英文，支持分页
        // 搜索范围包括目的地和景点的名称、描述
        System.out.println("searchDestinations with pagination: " + keyword);
        Page<Destination> destinations = destinationRepository.searchByKeyword(keyword, pageable);
        System.out.println("destinations page size: " + destinations.getContent().size());
        return destinations;
    }

    @Override
    public List<Destination> searchDestinationsByName(String name) {
        return destinationRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public void incrementJoinCount(Long destinationId) {
        Destination destination = destinationRepository.findById(destinationId).orElse(null);
        if (destination != null) {
            destination.setJoinCount(destination.getJoinCount() + 1);
            destinationRepository.save(destination);
        }
    }
    
    @Override
    public List<String> getAllTags() {
        return tagRepository.findAllByOrderById()
                .stream()
                .map(Tag::getTag)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<String> getTopNTags(Destination destination, int n) {
        if (destination == null || destination.getTagScores() == null || destination.getTagScores().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        try {
            // 解析JSON字符串为Map<String, Object>，其中key是标签ID
            Map<String, Object> tagScores = objectMapper.readValue(
                destination.getTagScores(), 
                new TypeReference<Map<String, Object>>() {}
            );
            
            // 获取权重最高的N个标签ID
            List<String> topTagIds = tagScores.entrySet().stream()
                .filter(entry -> {
                    // 过滤出数值类型的权重，并且权重大于0
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue() >= 0;
                    }
                    return false;
                })
                .sorted((entry1, entry2) -> {
                    // 按权重降序排序
                    double score1 = ((Number) entry1.getValue()).doubleValue();
                    double score2 = ((Number) entry2.getValue()).doubleValue();
                    return Double.compare(score2, score1);
                })
                .limit(n)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            // 根据标签ID查询标签名称并保持顺序
            if (topTagIds.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            List<Long> idList = topTagIds.stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            Map<Long, String> idToTag = tagRepository.findAllById(idList)
                    .stream()
                    .collect(Collectors.toMap(Tag::getId, Tag::getTag));

            return idList.stream()
                    .map(id -> idToTag.getOrDefault(id, ""))
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());
                
        } catch (Exception e) {
            // JSON解析失败时，返回空列表
            System.err.println("解析标签权重JSON失败: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
    

    
    @Override
    public List<Destination> getDestinationsByTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        // 获取所有目的地
        List<Destination> allDestinations = destinationRepository.findAll();
        
        // 计算每个目的地的标签权重总和并排序
        return allDestinations.stream()
            .map(destination -> {
                double totalScore = calculateTagsScore(destination, tags);
                return new DestinationWithScore(destination, totalScore);
            })
            .filter(destWithScore -> destWithScore.getTotalScore() > 0) // 只保留有相关标签的目的地
            .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore())) // 按权重降序排序
            .map(DestinationWithScore::getDestination)
            .collect(Collectors.toList());
    }
    
    @Override
    public Page<Destination> getDestinationsByTags(List<String> tags, Pageable pageable) {
        if (tags == null || tags.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // 获取所有符合条件的目的地并排序
        List<Destination> allFilteredDestinations = getDestinationsByTags(tags);
        
        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allFilteredDestinations.size());
        
        List<Destination> pageContent = allFilteredDestinations.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, allFilteredDestinations.size());
    }
    
    @Override
    public Page<Destination> searchDestinationsByTagsAndKeyword(List<String> tags, String keyword, Pageable pageable) {
        if (tags == null || tags.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // 先按标签过滤获取所有相关目的地
        List<Destination> tagFilteredDestinations = getDestinationsByTags(tags);
        
        // 在标签过滤结果中进行关键词搜索
        List<Destination> combinedResults = tagFilteredDestinations;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchKeyword = keyword.trim().toLowerCase();
            combinedResults = tagFilteredDestinations.stream()
                .filter(dest -> 
                    dest.getName().toLowerCase().contains(searchKeyword) ||
                    (dest.getDescription() != null && dest.getDescription().toLowerCase().contains(searchKeyword))
                )
                .collect(Collectors.toList());
        }
        
        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combinedResults.size());
        
        List<Destination> pageContent = combinedResults.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, combinedResults.size());
    }
    
    /**
     * 计算目的地在指定标签列表上的权重总和
     */
    private double calculateTagsScore(Destination destination, List<String> tags) {
        if (destination == null || destination.getTagScores() == null || destination.getTagScores().isEmpty()) {
            return 0.0;
        }
        
        try {
            // 首先将标签名称转换为标签ID
            if (tags.isEmpty()) {
                return 0.0;
            }
            
            List<Long> tagIds = tagRepository.findByTagIn(tags)
                    .stream()
                    .map(Tag::getId)
                    .collect(Collectors.toList());
            
            if (tagIds.isEmpty()) {
                return 0.0;
            }
            
            // 解析JSON字符串为Map，key是标签ID字符串
            Map<String, Object> tagScores = objectMapper.readValue(
                destination.getTagScores(), 
                new TypeReference<Map<String, Object>>() {}
            );
            
            // 计算选中标签的权重总和
            return tagIds.stream()
                .mapToDouble(tagId -> {
                    Object scoreObj = tagScores.get(String.valueOf(tagId));
                    if (scoreObj instanceof Number) {
                        double score = ((Number) scoreObj).doubleValue();
                        return Math.max(0, score); // 确保非负
                    }
                    return 0.0;
                })
                .sum();
                
        } catch (Exception e) {
            System.err.println("解析标签权重JSON失败: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * 内部类：带权重的目的地
     */
    private static class DestinationWithScore {
        private final Destination destination;
        private final double totalScore;
        
        public DestinationWithScore(Destination destination, double totalScore) {
            this.destination = destination;
            this.totalScore = totalScore;
        }
        
        public Destination getDestination() {
            return destination;
        }
        
        public double getTotalScore() {
            return totalScore;
        }
    }
} 
package com.se_07.backend.service;

import com.se_07.backend.entity.Destination;
import com.se_07.backend.entity.Attraction;
import java.util.List;
import java.util.Map;

public interface SemanticSearchService {
    List<Destination> semanticSearch(String query, int size);
    
    List<Destination> semanticSearchByTags(String query, List<String> tags, int size);
    
    /**
     * 对指定目的地的景点进行语义搜索
     * @param destinationId 目的地ID
     * @param query 搜索查询
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 景点分页结果
     */
    org.springframework.data.domain.Page<Attraction> semanticSearchAttractions(Long destinationId, String query, int page, int size);
    
    /**
     * 对指定目的地的景点进行语义搜索+标签组合搜索
     * @param destinationId 目的地ID
     * @param query 搜索查询
     * @param tags 标签列表
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 景点分页结果
     */
    org.springframework.data.domain.Page<Attraction> semanticSearchAttractionsByTags(Long destinationId, String query, List<String> tags, int page, int size);
    
    /**
     * 对社区条目进行语义搜索
     * @param query 搜索查询
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 社区条目分页结果
     */
    org.springframework.data.domain.Page<Map<String, Object>> semanticSearchCommunityEntries(String query, int page, int size);
    
    /**
     * 对社区条目进行语义搜索+标签组合搜索
     * @param query 搜索查询
     * @param tags 标签列表
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 社区条目分页结果
     */
    org.springframework.data.domain.Page<Map<String, Object>> semanticSearchCommunityEntriesByTags(String query, List<String> tags, int page, int size);
    
    /**
     * 对作者进行语义搜索
     * @param query 搜索查询
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 作者分页结果
     */
    org.springframework.data.domain.Page<Map<String, Object>> semanticSearchAuthors(String query, int page, int size);
    
    /**
     * 仅用标签筛选社区条目，按view_count降序
     * @param tags 标签列表
     * @param page 页码
     * @param size 每页大小
     * @return 社区条目分页结果
     */
    org.springframework.data.domain.Page<Map<String, Object>> filterCommunityEntriesByTags(List<String> tags, int page, int size);
} 
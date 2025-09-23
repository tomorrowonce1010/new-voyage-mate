package com.se_07.backend.service;

import java.util.List;
import java.util.Map;

public interface CommunityService {
    
    /**
     * 获取所有公共社区条目
     * @return 公共社区条目列表
     */
    List<Map<String, Object>> getPublicCommunityEntries();
    
    /**
     * 根据分享码获取社区条目
     * @param shareCode 分享码
     * @return 社区条目信息，如果不存在则返回null
     */
    Map<String, Object> getCommunityEntryByShareCode(String shareCode);
    
    /**
     * 搜索社区条目 - 按行程名称、描述、目的地搜索
     * @param searchTerm 搜索关键词
     * @return 匹配的社区条目列表
     */
    List<Map<String, Object>> searchCommunityEntries(String searchTerm);
    
    /**
     * 增加社区条目的查看次数
     * @param entryId 社区条目ID
     */
    void incrementViewCount(Long entryId);
    
    /**
     * 获取按使用次数排序的热门标签
     */
    List<Map<String, Object>> getPopularTags(int limit);
    
    /**
     * 获取热门作者（按其所有社区条目的总浏览量降序）
     */
    List<Map<String, Object>> getPopularAuthors(int limit);
    
    /**
     * 按目的地关键词搜索社区条目
     * @param destination 目的地关键词
     * @param page 页码
     * @param size 每页大小
     * @return 匹配的社区条目列表
     */
    Map<String, Object> searchCommunityEntriesByDestination(String destination, int page, int size);
    
    /**
     * 获取所有公共社区条目（支持排序）
     * @param sortBy 排序方式："popularity" 按热度排序，"time" 按时间排序
     * @param page 页码
     * @param size 每页大小
     * @return 公共社区条目列表
     */
    Map<String, Object> getPublicCommunityEntriesWithSort(String sortBy, int page, int size);
} 
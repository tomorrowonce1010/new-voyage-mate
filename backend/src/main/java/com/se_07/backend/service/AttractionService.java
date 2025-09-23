package com.se_07.backend.service;

import com.se_07.backend.entity.Attraction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface AttractionService {
    
    /**
     * 根据ID获取景点详情
     * @param id 景点ID
     * @return 景点详情
     */
    Optional<Attraction> getAttractionById(Long id);
    
    /**
     * 根据目的地ID获取景点列表（分页）
     * @param destinationId 目的地ID
     * @param pageable 分页参数
     * @return 景点分页结果
     */
    Page<Attraction> getAttractionsByDestinationId(Long destinationId, Pageable pageable);
    
    /**
     * 获取景点tag_scores字段中权重最高的N个标签
     * @param attraction 景点对象
     * @param n 返回标签数量
     * @return 标签列表
     */
    List<String> getTopNTags(Attraction attraction, int n);
    
    /**
     * 根据分类获取景点列表
     * @param category 景点分类
     * @param pageable 分页参数
     * @return 景点分页结果
     */
    Page<Attraction> getAttractionsByCategory(Attraction.AttractionCategory category, Pageable pageable);
    
    /**
     * 搜索景点（按名称或描述）
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    List<Attraction> searchAttractions(String keyword);
    
    /**
     * 增加景点被加入行程的次数
     * @param attractionId 景点ID
     */
    void incrementJoinCount(Long attractionId);
    
    /**
     * 获取热门景点列表
     * @param pageable 分页参数
     * @return 热门景点分页结果
     */
    Page<Attraction> getHotAttractions(Pageable pageable);
    
    /**
     * 根据名称搜索景点（支持模糊搜索）
     * @param keyword 搜索关键词
     * @param limit 限制返回数量，默认10
     * @return 景点列表
     */
    java.util.List<Attraction> searchAttractionsByName(String keyword, Integer limit);

    /**
     * 根据目的地ID、标签（可多个逗号分隔）及关键词获取景点列表（分页）
     * @param destinationId 目的地ID
     * @param tag 标签（可选，多个用逗号分隔）
     * @param keyword 搜索关键词（可选）
     * @param pageable 分页参数
     * @return 景点分页结果
     */
    Page<Attraction> getAttractionsByDestinationWithFilters(Long destinationId, String tag, String keyword, Pageable pageable);
} 
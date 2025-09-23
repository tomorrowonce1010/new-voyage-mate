package com.se_07.backend.service;

import com.se_07.backend.entity.Destination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface DestinationService {
    
    /**
     * 获取热门目的地列表
     * @param pageable 分页参数
     * @return 热门目的地分页结果
     */
    Page<Destination> getHotDestinations(Pageable pageable);
    
    /**
     * 根据目的地ID获取目的地详情
     * @param id 目的地ID
     * @return 目的地详情
     */
    Destination getDestinationById(Long id);
    
    /**
     * 根据关键词搜索目的地（目的地名称、标签、景点、描述）
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    List<Destination> searchDestinations(String keyword);
    
    /**
     * 根据关键词搜索目的地（支持分页）
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 搜索结果分页数据
     */
    Page<Destination> searchDestinations(String keyword, Pageable pageable);
    
    /**
     * 按多个标签过滤目的地（按权值之和降序排序）
     * @param tags 标签名称列表
     * @return 过滤结果列表
     */
    List<Destination> getDestinationsByTags(List<String> tags);
    
    /**
     * 按多个标签过滤目的地（按权值之和降序排序，支持分页）
     * @param tags 标签名称列表
     * @param pageable 分页参数
     * @return 过滤结果分页数据
     */
    Page<Destination> getDestinationsByTags(List<String> tags, Pageable pageable);
    
    /**
     * 组合搜索：按标签和关键词同时搜索目的地（支持分页）
     * @param tags 标签名称列表
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 搜索结果分页数据
     */
    Page<Destination> searchDestinationsByTagsAndKeyword(List<String> tags, String keyword, Pageable pageable);
    
    /**
     * 模糊搜索目的地名称（语义搜索）
     * @param name 目的地名称
     * @return 搜索结果列表
     */
    List<Destination> searchDestinationsByName(String name);

    /**
     * 获取所有标签
     */
    List<String> getAllTags();

    /**
     * 获取目的地tag_scores字段中权重最高的N个标签
     */
    List<String> getTopNTags(Destination destination, int n);


    
    /**
     * 增加目的地被加入行程的次数
     * @param destinationId 目的地ID
     */
    void incrementJoinCount(Long destinationId);
} 
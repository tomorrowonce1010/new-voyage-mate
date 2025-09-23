package com.se_07.backend.repository;

import com.se_07.backend.entity.Attraction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttractionRepository extends JpaRepository<Attraction, Long> {
    
    /**
     * 根据目的地ID分页查询景点，按join_count降序排序
     */
    Page<Attraction> findByDestinationIdOrderByJoinCountDesc(Long destinationId, Pageable pageable);
    
    /**
     * 根据目的地ID查询所有景点，按join_count降序排序
     */
    List<Attraction> findByDestinationIdOrderByJoinCountDesc(Long destinationId);

    /**
     * 根据目的地名称查询所有景点
     */
    @Query("SELECT a FROM Attraction a WHERE a.destination.name = :destinationName")
    List<Attraction> findByDestinationName(@Param("destinationName") String destinationName);
    
    /**
     * 根据分类查询景点，按join_count降序排序
     */
    Page<Attraction> findByCategoryOrderByJoinCountDesc(Attraction.AttractionCategory category, Pageable pageable);
    
    /**
     * 按join_count降序排序获取所有景点（热门景点）
     */
    Page<Attraction> findAllByOrderByJoinCountDesc(Pageable pageable);
    
    /**
     * 搜索景点（名称或描述包含关键词）
     */
    @Query("SELECT a FROM Attraction a WHERE a.name LIKE %:keyword% OR a.description LIKE %:keyword%")
    List<Attraction> findByNameContainingOrDescriptionContaining(@Param("keyword") String keyword);
    
    /**
     * 全文搜索景点（使用MySQL FULLTEXT搜索）
     */
    @Query(value = "SELECT * FROM attractions WHERE MATCH(name, description) AGAINST(?1 IN NATURAL LANGUAGE MODE)", nativeQuery = true)
    List<Attraction> searchByKeyword(String keyword);
    
    /**
     * 根据名称模糊搜索景点，按热度排序
     */
    Page<Attraction> findByNameContainingIgnoreCaseOrderByJoinCountDesc(String name, Pageable pageable);
    
    /**
     * 根据高德地图POI ID查找景点
     */
    java.util.Optional<Attraction> findByAmapPoiId(String amapPoiId);
} 
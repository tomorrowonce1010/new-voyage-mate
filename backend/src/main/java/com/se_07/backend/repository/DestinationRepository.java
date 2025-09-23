package com.se_07.backend.repository;

import com.se_07.backend.entity.Destination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Long> {
    
    // 获取热门目的地（按join_count排序）
    Page<Destination> findAllByOrderByJoinCountDesc(Pageable pageable);
    
    // 搜索目的地（包括目的地名称和描述、景点名称和描述）
    // 使用LIKE查询支持中文搜索，避免FULLTEXT对中文的支持问题
    @Query(value = "SELECT DISTINCT d.* FROM destinations d " +
                   "LEFT JOIN attractions a ON a.destination_id = d.id " +
                   "WHERE d.name LIKE CONCAT('%', :keyword, '%') " +
                   "OR d.description LIKE CONCAT('%', :keyword, '%') " +
                   "OR a.name LIKE CONCAT('%', :keyword, '%') " +
                   "OR a.description LIKE CONCAT('%', :keyword, '%') " +
                   "ORDER BY d.join_count DESC",
           nativeQuery = true)
    List<Destination> searchByKeyword(@Param("keyword") String keyword);
    
    // 搜索目的地（支持分页）
    @Query("SELECT DISTINCT d FROM Destination d " +
           "LEFT JOIN Attraction a ON a.destination.id = d.id " +
           "WHERE d.name LIKE CONCAT('%', :keyword, '%') " +
           "OR d.description LIKE CONCAT('%', :keyword, '%') " +
           "OR a.name LIKE CONCAT('%', :keyword, '%') " +
           "OR a.description LIKE CONCAT('%', :keyword, '%') " +
           "ORDER BY d.joinCount DESC")
    Page<Destination> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 按标签过滤目的地
    @Query(value = "SELECT * FROM destinations d WHERE JSON_EXTRACT(d.tag_scores, :tagPath) > 0 ORDER BY JSON_EXTRACT(d.tag_scores, :tagPath) DESC", 
           nativeQuery = true)
    List<Destination> findByTagScore(@Param("tagPath") String tagPath);
    
    // 模糊搜索目的地名称
    List<Destination> findByNameContainingIgnoreCase(String name);

    // 精确查找目的地名称
    Optional<Destination> findByName(String name);

    List<Destination> findByNameIn(List<String> names);
} 
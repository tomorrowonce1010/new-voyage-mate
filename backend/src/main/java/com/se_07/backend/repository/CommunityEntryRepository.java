package com.se_07.backend.repository;

import com.se_07.backend.entity.CommunityEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

public interface CommunityEntryRepository extends JpaRepository<CommunityEntry, Long> {
    
    // 根据行程id检索
    Optional<CommunityEntry> findByItineraryId(Long itineraryId);

    // 根据分享码检索
    Optional<CommunityEntry> findByShareCode(String shareCode);

    // 根据行程id删除
    @Transactional
    void deleteByItineraryId(Long itineraryId);
    
    // 检索所有人可见的行程
    @Query("SELECT ce FROM CommunityEntry ce JOIN ce.itinerary i WHERE i.permissionStatus = '所有人可见' ORDER BY ce.createdAt DESC")
    List<CommunityEntry> findPublicEntries();

    // 查找热门作者
    @Query("SELECT i.user.id, i.user.username, SUM(ce.viewCount) FROM CommunityEntry ce JOIN ce.itinerary i GROUP BY i.user.id, i.user.username ORDER BY SUM(ce.viewCount) DESC")
    List<Object[]> findAuthorPopularity();
    
    // 搜索社区条目 - 按行程名称、描述、目的地、作者用户名搜索
    @Query("SELECT DISTINCT ce FROM CommunityEntry ce " +
           "JOIN ce.itinerary i " +
           "LEFT JOIN i.user u " +
           "LEFT JOIN i.itineraryDays id " +
           "LEFT JOIN id.activities act " +
           "LEFT JOIN act.attraction attr " +
           "LEFT JOIN attr.destination d " +
           "WHERE i.permissionStatus = '所有人可见' " +
           "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(ce.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(attr.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY ce.createdAt DESC")
    List<CommunityEntry> searchPublicEntries(@Param("searchTerm") String searchTerm);
    
    // 按目的地搜索社区条目（分页）
    @Query(value = "SELECT DISTINCT ce.* FROM community_entries ce " +
           "JOIN itineraries i ON ce.itinerary_id = i.id " +
           "LEFT JOIN itinerary_days id ON i.id = id.itinerary_id " +
           "LEFT JOIN itinerary_activities act ON id.id = act.itinerary_day_id " +
           "LEFT JOIN attractions attr ON act.attraction_id = attr.id " +
           "LEFT JOIN destinations d ON attr.destination_id = d.id " +
           "WHERE i.permission_status = '所有人可见' " +
           "AND LOWER(d.name) LIKE LOWER(CONCAT('%', :destination, '%')) " +
           "ORDER BY ce.created_at DESC " +
           "LIMIT :size OFFSET :offset", nativeQuery = true)
    List<CommunityEntry> findByDestination(@Param("destination") String destination, 
                                          @Param("offset") int offset, 
                                          @Param("size") int size);
    
    // 按目的地搜索社区条目总数
    @Query("SELECT COUNT(DISTINCT ce) FROM CommunityEntry ce " +
           "JOIN ce.itinerary i " +
           "LEFT JOIN i.itineraryDays id " +
           "LEFT JOIN id.activities act " +
           "LEFT JOIN act.attraction attr " +
           "LEFT JOIN attr.destination d " +
           "WHERE i.permissionStatus = '所有人可见' " +
           "AND LOWER(d.name) LIKE LOWER(CONCAT('%', :destination, '%'))")
    long countByDestination(@Param("destination") String destination);
    
    // 按用户ID查询公开的社区条目
    @Query("SELECT ce FROM CommunityEntry ce JOIN ce.itinerary i " +
           "WHERE i.permissionStatus = '所有人可见' AND i.user.id = :userId " +
           "ORDER BY ce.createdAt DESC")
    List<CommunityEntry> findPublicEntriesByUserId(@Param("userId") Long userId);

    // 查询所有公开社区条目，按view_count降序
    @Query("SELECT ce FROM CommunityEntry ce JOIN ce.itinerary i WHERE i.permissionStatus = '所有人可见' ORDER BY ce.viewCount DESC")
    List<CommunityEntry> findAllPublic();
    
    // 查询所有公开社区条目，按热度降序排序（分页）
    @Query("SELECT ce FROM CommunityEntry ce JOIN ce.itinerary i WHERE i.permissionStatus = '所有人可见' ORDER BY ce.viewCount DESC")
    org.springframework.data.domain.Page<CommunityEntry> findAllPublicByPopularity(org.springframework.data.domain.Pageable pageable);
    
    // 查询所有公开社区条目，按时间降序排序（分页）
    @Query("SELECT ce FROM CommunityEntry ce JOIN ce.itinerary i WHERE i.permissionStatus = '所有人可见' ORDER BY ce.createdAt DESC")
    org.springframework.data.domain.Page<CommunityEntry> findAllPublicByTime(org.springframework.data.domain.Pageable pageable);
} 
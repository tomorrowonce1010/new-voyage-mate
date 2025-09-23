package com.se_07.backend.repository;

import com.se_07.backend.entity.Itinerary;
import com.se_07.backend.entity.Itinerary.TravelStatus;
import com.se_07.backend.entity.Itinerary.EditStatus;
import com.se_07.backend.entity.Itinerary.PermissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    
    // 根据用户ID获取行程列表
    List<Itinerary> findByUserId(Long userId);
    
    // 根据用户ID和出行状态获取行程
    List<Itinerary> findByUserIdAndTravelStatus(Long userId, TravelStatus travelStatus);
    
    // 根据用户ID和编辑状态获取行程
    List<Itinerary> findByUserIdAndEditStatus(Long userId, EditStatus editStatus);
    
    // 根据权限状态获取公开行程
    List<Itinerary> findByPermissionStatus(PermissionStatus permissionStatus);
    
    // 分页获取用户的行程
    Page<Itinerary> findByUserId(Long userId, Pageable pageable);
    
    // 分页获取用户的行程，按创建时间降序排列
    Page<Itinerary> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // 根据用户ID和日期范围获取行程
    List<Itinerary> findByUserIdAndStartDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    // 根据用户ID和权限状态获取行程
    List<Itinerary> findByUserIdAndPermissionStatus(Long userId, PermissionStatus permissionStatus);

    /**
     * 根据团队ID查询行程
     */
    Optional<Itinerary> findByGroupId(Long groupId);
    boolean existsByGroupId(Long groupId);

    /**
     * 获取用户的个人行程（group_id为null）
     */
    @Query("SELECT i FROM Itinerary i WHERE i.user.id = ?1 AND i.groupId IS NULL ORDER BY i.createdAt DESC")
    Page<Itinerary> findPersonalItineraries(Long userId, Pageable pageable);

    /**
     * 获取用户的团队行程（group_id不为null）
     */
    @Query("SELECT i FROM Itinerary i WHERE i.user.id = ?1 AND i.groupId IS NOT NULL ORDER BY i.createdAt DESC")
    Page<Itinerary> findTeamItineraries(Long userId, Pageable pageable);
}
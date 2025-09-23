package com.se_07.backend.repository;

import com.se_07.backend.entity.GroupItinerary;
import com.se_07.backend.entity.TravelGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupItineraryRepository extends JpaRepository<GroupItinerary, Long> {
    Optional<GroupItinerary> findByGroup(TravelGroup group);
    
    List<GroupItinerary> findByGroupId(Long groupId);
    
    List<GroupItinerary> findByGroupIdAndIsTemplate(Long groupId, boolean isTemplate);
    
    Optional<GroupItinerary> findByGroupIdAndItineraryId(Long groupId, Long itineraryId);
    
    /**
     * 获取团队行程，使用JOIN FETCH避免懒加载问题
     */
    @Query("SELECT gi FROM GroupItinerary gi " +
           "JOIN FETCH gi.itinerary i " +
           "JOIN FETCH i.user " +
           "WHERE gi.group.id = :groupId")
    List<GroupItinerary> findByGroupIdWithItinerary(@Param("groupId") Long groupId);
} 
package com.se_07.backend.repository;

import com.se_07.backend.entity.ItineraryActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryActivityRepository extends JpaRepository<ItineraryActivity, Long> {
    
    /**
     * 根据日程ID查找所有活动
     */
    List<ItineraryActivity> findByItineraryDayId(Long itineraryDayId);
    
    /**
     * 根据日程ID删除所有活动
     */
    void deleteByItineraryDayId(Long itineraryDayId);
} 
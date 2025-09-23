package com.se_07.backend.repository;

import com.se_07.backend.entity.ItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {
    
    /**
     * 根据行程ID查找所有日程，按日期编号排序
     */
    List<ItineraryDay> findByItineraryIdOrderByDayNumber(Long itineraryId);
    
    /**
     * 根据行程ID删除所有日程
     */
    void deleteByItineraryId(Long itineraryId);
} 
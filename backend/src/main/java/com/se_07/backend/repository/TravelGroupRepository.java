package com.se_07.backend.repository;

import com.se_07.backend.entity.TravelGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelGroupRepository extends JpaRepository<TravelGroup, Long> {
    
    List<TravelGroup> findByCreatorId(Long creatorId);
    
    List<TravelGroup> findByIsPublicTrueAndStatus(TravelGroup.GroupStatus status);

    List<TravelGroup> findByStatusOrderByCreatedAtDesc(TravelGroup.GroupStatus status);
} 
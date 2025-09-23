package com.se_07.backend.repository;

import com.se_07.backend.entity.TravelGroup;
import com.se_07.backend.entity.TravelGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelGroupMemberRepository extends JpaRepository<TravelGroupMember, Long> {
    
    Optional<TravelGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    
    List<TravelGroupMember> findByGroupId(Long groupId);
    
    List<TravelGroupMember> findByUserId(Long userId);
    
    Long countByGroupId(Long groupId);

    /**
     * 计算组团成员数量
     */
    long countByGroupAndJoinStatus(TravelGroup group, TravelGroupMember.JoinStatus joinStatus);
} 
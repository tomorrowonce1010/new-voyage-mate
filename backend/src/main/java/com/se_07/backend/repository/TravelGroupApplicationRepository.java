package com.se_07.backend.repository;

import com.se_07.backend.entity.TravelGroupApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelGroupApplicationRepository extends JpaRepository<TravelGroupApplication, Long> {
    
    // 查找组团的所有申请
    List<TravelGroupApplication> findByGroupIdAndStatusOrderByApplyDateDesc(Long groupId, TravelGroupApplication.ApplicationStatus status);
    
    // 查找用户的所有申请
    List<TravelGroupApplication> findByApplicantIdOrderByApplyDateDesc(Long applicantId);
    
    // 检查用户是否已申请该组团
    Optional<TravelGroupApplication> findByGroupIdAndApplicantId(Long groupId, Long applicantId);
    
    // 检查用户是否有待审核的申请
    boolean existsByGroupIdAndApplicantIdAndStatus(Long groupId, Long applicantId, TravelGroupApplication.ApplicationStatus status);
    
    // 查找待审核的申请数量
    @Query("SELECT COUNT(tga) FROM TravelGroupApplication tga WHERE tga.group.id = :groupId AND tga.status = '待审核'")
    Long countPendingApplicationsByGroupId(@Param("groupId") Long groupId);
    
    // 查找用户创建的组团的所有待审核申请
    @Query("SELECT tga FROM TravelGroupApplication tga WHERE tga.group.creator.id = :creatorId AND tga.status = '待审核' ORDER BY tga.applyDate DESC")
    List<TravelGroupApplication> findPendingApplicationsByCreator(@Param("creatorId") Long creatorId);
    
    // 查找特定用户在特定组团的特定状态申请
    List<TravelGroupApplication> findByGroupIdAndApplicantIdAndStatusOrderByApplyDateDesc(Long groupId, Long applicantId, TravelGroupApplication.ApplicationStatus status);
    
    // 新增：查找指定组团的所有申请，按申请时间倒序
    List<TravelGroupApplication> findByGroupIdOrderByApplyDateDesc(Long groupId);
    
    // 添加缺失的方法
    Optional<TravelGroupApplication> findByGroupIdAndApplicantIdAndStatus(Long groupId, Long applicantId, TravelGroupApplication.ApplicationStatus status);
    
    // 查找指定组团的所有申请
    List<TravelGroupApplication> findByGroupId(Long groupId);
} 
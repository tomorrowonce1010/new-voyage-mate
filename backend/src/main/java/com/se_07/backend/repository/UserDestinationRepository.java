package com.se_07.backend.repository;

import com.se_07.backend.entity.UserDestination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserDestinationRepository extends JpaRepository<UserDestination, Long> {
    
    @Query("SELECT ud FROM UserDestination ud " +
           "JOIN FETCH ud.destination d " +
           "WHERE ud.userPreferencesId = :userPreferencesId AND ud.type = :type " +
           "ORDER BY ud.createdAt DESC")
    List<UserDestination> findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(
            @Param("userPreferencesId") Long userPreferencesId, 
            @Param("type") UserDestination.Type type);
            
    @Query("SELECT COUNT(ud) FROM UserDestination ud " +
           "WHERE ud.userPreferencesId = :userPreferencesId AND ud.destinationId = :destinationId")
    long countByUserPreferencesIdAndDestinationId(
            @Param("userPreferencesId") Long userPreferencesId, 
            @Param("destinationId") Long destinationId);

    @Query("SELECT COUNT(ud) FROM UserDestination ud " +
           "WHERE ud.userPreferencesId = :userPreferencesId AND ud.destinationId = :destinationId AND ud.type = :type")
    long countByUserPreferencesIdAndDestinationIdAndType(
            @Param("userPreferencesId") Long userPreferencesId, 
            @Param("destinationId") Long destinationId,
            @Param("type") UserDestination.Type type);

    // 查找指定行程ID的自动添加目的地
    @Query("SELECT ud FROM UserDestination ud " +
           "WHERE ud.userPreferencesId = :userPreferencesId AND ud.type = :type AND ud.itineraryId = :itineraryId AND ud.destinationId IN :destinationIds")
    List<UserDestination> findByUserPreferencesIdAndTypeAndItineraryIdAndDestinationIdIn(
            @Param("userPreferencesId") Long userPreferencesId,
            @Param("type") UserDestination.Type type,
            @Param("itineraryId") Long itineraryId,
            @Param("destinationIds") List<Long> destinationIds);
            
    // 查找指定行程ID的所有目的地
    @Query("SELECT ud FROM UserDestination ud " +
           "WHERE ud.userPreferencesId = :userPreferencesId AND ud.itineraryId = :itineraryId")
    List<UserDestination> findByUserPreferencesIdAndItineraryId(
            @Param("userPreferencesId") Long userPreferencesId,
            @Param("itineraryId") Long itineraryId);
            
    // 查找手动添加的目的地（itineraryId = 0）
    @Query("SELECT ud FROM UserDestination ud " +
           "JOIN FETCH ud.destination d " +
           "WHERE ud.userPreferencesId = :userPreferencesId AND ud.type = :type AND ud.itineraryId = 0 " +
           "ORDER BY ud.createdAt DESC")
    List<UserDestination> findManuallyAddedByUserPreferencesIdAndType(
            @Param("userPreferencesId") Long userPreferencesId,
            @Param("type") UserDestination.Type type);
            
    // 查找自动添加的目的地（itineraryId > 0）
    @Query("SELECT ud FROM UserDestination ud " +
           "JOIN FETCH ud.destination d " +
           "WHERE ud.userPreferencesId = :userPreferencesId AND ud.type = :type AND ud.itineraryId > 0 " +
           "ORDER BY ud.startDate DESC, ud.createdAt DESC")
    List<UserDestination> findAutoAddedByUserPreferencesIdAndType(
            @Param("userPreferencesId") Long userPreferencesId,
            @Param("type") UserDestination.Type type);
} 
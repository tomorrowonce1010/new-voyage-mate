package com.se_07.backend.repository;

import com.se_07.backend.entity.CommunityEntryTag;
import com.se_07.backend.entity.CommunityEntry;
import com.se_07.backend.entity.CommunityEntryTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CommunityEntryTagRepository extends JpaRepository<CommunityEntryTag, CommunityEntryTagId> {
    List<CommunityEntryTag> findByCommunityEntry(CommunityEntry communityEntry);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CommunityEntryTag cet WHERE cet.communityEntry = :communityEntry")
    void deleteByCommunityEntry(@Param("communityEntry") CommunityEntry communityEntry);

    @Query("SELECT cet.tag.tag AS tag, COUNT(cet) AS cnt FROM CommunityEntryTag cet GROUP BY cet.tag.tag ORDER BY cnt DESC")
    List<Object[]> findTagPopularity();
} 
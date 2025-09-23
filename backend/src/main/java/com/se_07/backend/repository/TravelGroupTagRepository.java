package com.se_07.backend.repository;

import com.se_07.backend.entity.TravelGroupTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelGroupTagRepository extends JpaRepository<TravelGroupTag, Long> {
    
    // 查找组团的所有标签
    List<TravelGroupTag> findByGroupId(Long groupId);
    
    // 查找使用某个标签的所有组团
    List<TravelGroupTag> findByTagId(Long tagId);
    
    // 删除组团的所有标签
    void deleteByGroupId(Long groupId);
} 
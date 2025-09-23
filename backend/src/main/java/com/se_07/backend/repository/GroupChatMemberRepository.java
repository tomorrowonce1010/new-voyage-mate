package com.se_07.backend.repository;

import com.se_07.backend.entity.GroupChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupChatMemberRepository extends JpaRepository<GroupChatMember, GroupChatMember.GroupChatMemberId> {

    @Query("SELECT m.groupId FROM GroupChatMember m WHERE m.userId = :userId")
    List<Long> findGroupIdsByUserId(@Param("userId") Long userId);

    List<GroupChatMember> findByGroupId(Long groupId);

    GroupChatMember findByGroupIdAndUserId(Long groupId, Long userId);
} 
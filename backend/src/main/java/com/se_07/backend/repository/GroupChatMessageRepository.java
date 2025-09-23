package com.se_07.backend.repository;

import com.se_07.backend.entity.GroupChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface GroupChatMessageRepository extends JpaRepository<GroupChatMessage, Long> {
    @Query("SELECT m FROM GroupChatMessage m WHERE m.groupId = :groupId ORDER BY m.messageTime ASC")
    List<GroupChatMessage> findByGroupIdOrderByMessageTimeAsc(@Param("groupId") Long groupId);
} 
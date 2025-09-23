package com.se_07.backend.repository;

import com.se_07.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByFromIdOrToId(Long fromId, Long toId);
    List<ChatMessage> findByFromIdAndToId(Long fromId, Long toId);
    @Query("SELECT m FROM ChatMessage m WHERE (m.fromId = :userId1 AND m.toId = :userId2) OR (m.fromId = :userId2 AND m.toId = :userId1) ORDER BY m.messageTime ASC")
    List<ChatMessage> findMessagesBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
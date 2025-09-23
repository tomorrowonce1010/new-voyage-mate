package com.se_07.backend.repository;

import com.se_07.backend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    List<Friend> findAllById(Long id);
    void deleteByIdAndFriendId(Long id, Long friendId);
    boolean existsByIdAndFriendId(Long id, Long friendId);
} 
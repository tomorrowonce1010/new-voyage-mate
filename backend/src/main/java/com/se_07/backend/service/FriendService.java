package com.se_07.backend.service;

import java.util.List;

public interface FriendService {
    void addFriend(Long userId, Long friendId);
    void deleteFriend(Long userId, Long friendId);
    List<Long> getAllFriends(Long userId);
} 
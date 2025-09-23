package com.se_07.backend.service.impl;

import com.se_07.backend.entity.Friend;
import com.se_07.backend.repository.FriendRepository;
import com.se_07.backend.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {
    @Autowired
    private FriendRepository friendRepository;

    @Override
    @Transactional
    public void addFriend(Long userId, Long friendId) {
        if (!friendRepository.existsByIdAndFriendId(userId, friendId)) {
            Friend f1 = new Friend();
            f1.setId(userId);
            f1.setFriendId(friendId);
            friendRepository.save(f1);
        }
        if (!friendRepository.existsByIdAndFriendId(friendId, userId)) {
            Friend f2 = new Friend();
            f2.setId(friendId);
            f2.setFriendId(userId);
            friendRepository.save(f2);
        }
    }

    @Override
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        friendRepository.deleteByIdAndFriendId(userId, friendId);
        friendRepository.deleteByIdAndFriendId(friendId, userId);
    }

    @Override
    public List<Long> getAllFriends(Long userId) {
        return friendRepository.findAllById(userId)
                .stream()
                .map(Friend::getFriendId)
                .collect(Collectors.toList());
    }
} 
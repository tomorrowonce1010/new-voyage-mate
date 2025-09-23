package com.se_07.backend.service.impl;

import com.se_07.backend.entity.Friend;
import com.se_07.backend.repository.FriendRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceImplTest {

    @Mock
    private FriendRepository friendRepository;
    @InjectMocks
    private FriendServiceImpl friendService;

    @BeforeEach
    void setUp() {}

    @Test
    void testAddFriend_bothNotExist() {
        when(friendRepository.existsByIdAndFriendId(1L, 2L)).thenReturn(false);
        when(friendRepository.existsByIdAndFriendId(2L, 1L)).thenReturn(false);
        friendService.addFriend(1L, 2L);
        verify(friendRepository, times(1)).save(argThat(f -> f.getId().equals(1L) && f.getFriendId().equals(2L)));
        verify(friendRepository, times(1)).save(argThat(f -> f.getId().equals(2L) && f.getFriendId().equals(1L)));
    }

    @Test
    void testAddFriend_oneExists() {
        when(friendRepository.existsByIdAndFriendId(1L, 2L)).thenReturn(true);
        when(friendRepository.existsByIdAndFriendId(2L, 1L)).thenReturn(false);
        friendService.addFriend(1L, 2L);
        verify(friendRepository, never()).save(argThat(f -> f.getId().equals(1L)));
        verify(friendRepository, times(1)).save(argThat(f -> f.getId().equals(2L) && f.getFriendId().equals(1L)));
    }

    @Test
    void testAddFriend_bothExist() {
        when(friendRepository.existsByIdAndFriendId(1L, 2L)).thenReturn(true);
        when(friendRepository.existsByIdAndFriendId(2L, 1L)).thenReturn(true);
        friendService.addFriend(1L, 2L);
        verify(friendRepository, never()).save(any());
    }

    @Test
    void testDeleteFriend() {
        doNothing().when(friendRepository).deleteByIdAndFriendId(1L, 2L);
        doNothing().when(friendRepository).deleteByIdAndFriendId(2L, 1L);
        friendService.deleteFriend(1L, 2L);
        verify(friendRepository, times(1)).deleteByIdAndFriendId(1L, 2L);
        verify(friendRepository, times(1)).deleteByIdAndFriendId(2L, 1L);
    }

    @Test
    void testGetAllFriends_hasFriends() {
        Friend f1 = new Friend(); f1.setId(1L); f1.setFriendId(2L);
        Friend f2 = new Friend(); f2.setId(1L); f2.setFriendId(3L);
        when(friendRepository.findAllById(1L)).thenReturn(Arrays.asList(f1, f2));
        List<Long> friends = friendService.getAllFriends(1L);
        assertEquals(2, friends.size());
        assertTrue(friends.contains(2L));
        assertTrue(friends.contains(3L));
    }

    @Test
    void testGetAllFriends_noFriends() {
        when(friendRepository.findAllById(1L)).thenReturn(Collections.emptyList());
        List<Long> friends = friendService.getAllFriends(1L);
        assertTrue(friends.isEmpty());
    }
}
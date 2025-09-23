package com.se_07.backend.service;

import com.se_07.backend.dto.UserProfileResponse;
import java.util.List;

public interface GroupChatManageService {
    Long createGroup(String groupName, Long creatorUserId);
    void addUserToGroup(Long groupId, Long userId);
    void updateGroupName(Long groupId, String newName);
    List<UserProfileResponse> getGroupMembers(Long groupId);
    void removeUserFromGroup(Long groupId, Long userId);
    void leaveGroup(Long groupId, Long userId);
    Long createGroupWithMembers(String groupName, Long creatorUserId, List<Long> memberIds);
} 
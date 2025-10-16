package com.se_07.backend.service.impl;

import com.se_07.backend.dto.UserProfileResponse;
import com.se_07.backend.entity.GroupChatInformation;
import com.se_07.backend.entity.GroupChatMember;
import com.se_07.backend.entity.User;
import com.se_07.backend.repository.GroupChatInformationRepository;
import com.se_07.backend.repository.GroupChatMemberRepository;
import com.se_07.backend.repository.UserRepository;
import com.se_07.backend.service.GroupChatManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupChatManageServiceImpl implements GroupChatManageService {
    @Autowired
    private GroupChatInformationRepository groupChatInformationRepository;
    @Autowired
    private GroupChatMemberRepository groupChatMemberRepository;
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private com.se_07.backend.repository.UserProfileRepository userProfileRepository;

    @Override
    public Long createGroup(String groupName, Long creatorUserId) {
        GroupChatInformation group = new GroupChatInformation();
        group.setGroupName(groupName);
        group = groupChatInformationRepository.save(group);
        // 创建者自动入群
        GroupChatMember member = new GroupChatMember();
        member.setGroupId(group.getGroupId());
        member.setUserId(creatorUserId);
        groupChatMemberRepository.save(member);
        return group.getGroupId();
    }

    @Override
    public void addUserToGroup(Long groupId, Long userId) {
        // 检查用户是否已经在群组中，避免主键冲突
        GroupChatMember existingMember = groupChatMemberRepository.findByGroupIdAndUserId(groupId, userId);
        if (existingMember != null) {
            // 用户已经在群组中，无需重复添加
            return;
        }
        
        GroupChatMember member = new GroupChatMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        groupChatMemberRepository.save(member);
    }

    @Override
    public void updateGroupName(Long groupId, String newName) {
        GroupChatInformation group = groupChatInformationRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("群聊不存在"));
        group.setGroupName(newName);
        groupChatInformationRepository.save(group);
    }

    @Override
    public List<UserProfileResponse> getGroupMembers(Long groupId) {
        List<GroupChatMember> members = groupChatMemberRepository.findByGroupId(groupId);
        return members.stream()
            .map(member -> {
                User user = userRepository.findById(member.getUserId()).orElse(null);
                UserProfileResponse resp = new UserProfileResponse();
                if (user != null) {
                    resp.setId(user.getId());
                    resp.setUsername(user.getUsername());
                    // 从UserProfile中获取头像URL
                    userProfileRepository.findByUserId(user.getId())
                        .ifPresent(profile -> resp.setAvatarUrl(profile.getAvatarUrl()));
                }
                return resp;
            })
            .collect(Collectors.toList());
    }

    @Override
    public void removeUserFromGroup(Long groupId, Long userId) {
        GroupChatMember member = groupChatMemberRepository.findByGroupIdAndUserId(groupId, userId);
        if (member != null) {
            groupChatMemberRepository.delete(member);
        }
    }

    @Override
    public void leaveGroup(Long groupId, Long userId) {
        GroupChatMember member = groupChatMemberRepository.findByGroupIdAndUserId(groupId, userId);
        if (member != null) {
            groupChatMemberRepository.delete(member);
        }
    }

    @Override
    public Long createGroupWithMembers(String groupName, Long creatorUserId, List<Long> memberIds) {
        GroupChatInformation group = new GroupChatInformation();
        group.setGroupName(groupName);
        group = groupChatInformationRepository.save(group);
        // 创建者自动入群
        GroupChatMember creator = new GroupChatMember();
        creator.setGroupId(group.getGroupId());
        creator.setUserId(creatorUserId);
        groupChatMemberRepository.save(creator);
        // 其他成员入群（去重，排除创建者）
        // 使用Set去重，避免memberIds中有重复值导致主键冲突
        java.util.Set<Long> uniqueMemberIds = new java.util.HashSet<>(memberIds);
        for (Long userId : uniqueMemberIds) {
            if (!userId.equals(creatorUserId)) {
                GroupChatMember member = new GroupChatMember();
                member.setGroupId(group.getGroupId());
                member.setUserId(userId);
                groupChatMemberRepository.save(member);
            }
        }
        return group.getGroupId();
    }
} 
package com.se_07.backend.service.impl;

import com.se_07.backend.dto.CreateTravelGroupRequest;
import com.se_07.backend.dto.TravelGroupDTO;
import com.se_07.backend.entity.*;
import com.se_07.backend.repository.*;
import com.se_07.backend.service.GroupChatManageService;
import com.se_07.backend.service.TravelGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TravelGroupServiceImpl implements TravelGroupService {

    @Autowired
    private TravelGroupRepository travelGroupRepository;

    @Autowired
    private TravelGroupMemberRepository memberRepository;

    @Autowired
    private TravelGroupApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private GroupChatManageService groupChatManageService;

    @Override
    @Transactional
    public TravelGroupDTO createTravelGroup(CreateTravelGroupRequest request, Long userId) {
        try {
            // 参数验证
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("标题不能为空");
            }
            if (request.getMaxMembers() == null || request.getMaxMembers() < 2) {
                throw new IllegalArgumentException("成员数量必须大于等于2");
            }
            if (request.getStartDate() == null || request.getEndDate() == null) {
                throw new IllegalArgumentException("开始日期和结束日期不能为空");
            }
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new IllegalArgumentException("开始日期不能晚于结束日期");
            }
            if (request.getDestinationId() == null) {
                throw new IllegalArgumentException("目的地不能为空");
            }

            // 获取创建者用户信息
            User creator = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 获取目的地信息
            Destination destination = destinationRepository.findById(request.getDestinationId())
                    .orElseThrow(() -> new RuntimeException("目的地不存在"));

            TravelGroup group = new TravelGroup();
            group.setTitle(request.getTitle());
            group.setDescription(request.getDescription());
            group.setStartDate(request.getStartDate());
            group.setEndDate(request.getEndDate());
            group.setMaxMembers(request.getMaxMembers());
            group.setEstimatedBudget(request.getEstimatedBudget());
            group.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : true);
            group.setGroupType(request.getGroupType() != null ? TravelGroup.GroupType.valueOf(request.getGroupType()) : TravelGroup.GroupType.自由行);
            group.setCreator(creator);
            group.setDestination(destination);  // 设置目的地
            group.setCurrentMembers(1); // 创建时默认只有创建者一个成员
            group.setStatus(TravelGroup.GroupStatus.招募中); // 设置初始状态
            
            // 保存组团
            group = travelGroupRepository.save(group);

            // 1. 创建群聊并关联
            Long groupChatId = groupChatManageService.createGroup(group.getTitle(), userId);
            group.setGroupChatId(groupChatId);
            group = travelGroupRepository.save(group);
            
            // 创建成员记录
            TravelGroupMember member = new TravelGroupMember();
            member.setGroup(group);
            member.setUser(creator);
            member.setRole(TravelGroupMember.MemberRole.创建者);
            member.setJoinStatus(TravelGroupMember.JoinStatus.已加入);
            member.setJoinDate(LocalDateTime.now());
            memberRepository.save(member);

            // 设置旅行标签
            if (request.getTravelTags() != null && !request.getTravelTags().isEmpty()) {
                List<Tag> tags = tagRepository.findByTagIn(request.getTravelTags());
                List<TravelGroupTag> groupTags = new ArrayList<>();
                for (Tag tag : tags) {
                    TravelGroupTag groupTag = new TravelGroupTag();
                    groupTag.setGroup(group);
                    groupTag.setTag(tag);
                    groupTag.setWeight(BigDecimal.valueOf(1.0)); // 默认权重为1.0
                    groupTags.add(groupTag);
                }
                group.setTravelTags(groupTags);
                group = travelGroupRepository.save(group);
            }
            
            // 返回DTO
            List<TravelGroupMember> members = memberRepository.findByGroupId(group.getId());
            return TravelGroupDTO.fromEntity(group, members);
        } catch (Exception e) {
            throw new RuntimeException("创建组团失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TravelGroupDTO> getPublicRecruitingGroups(Long currentUserId) {
        List<TravelGroup> groups = travelGroupRepository.findAll();
        return groups.stream()
                .filter(group -> {
                    // 检查是否为公开组团且状态为招募中
                    if (!Boolean.TRUE.equals(group.getIsPublic()) || !TravelGroup.GroupStatus.招募中.equals(group.getStatus())) {
                        return false;
                    }
                    
                    // 检查是否已满员（即使状态还是招募中）
                    long memberCount = memberRepository.countByGroupAndJoinStatus(group, TravelGroupMember.JoinStatus.已加入);
                    return memberCount < group.getMaxMembers();
                })
                .map(group -> {
                    List<TravelGroupMember> members = memberRepository.findByGroupId(group.getId());
                    return TravelGroupDTO.fromEntity(group, members);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TravelGroupDTO> getPublicRecruitingGroupsWithSearch(Long currentUserId, String searchText, String searchType, String startDate, String endDate) {
        List<TravelGroup> groups = travelGroupRepository.findAll();
        return groups.stream()
                .filter(group -> {
                    // 检查是否为公开组团且状态为招募中
                    if (!Boolean.TRUE.equals(group.getIsPublic()) || !TravelGroup.GroupStatus.招募中.equals(group.getStatus())) {
                        return false;
                    }
                    
                    // 检查是否已满员（即使状态还是招募中）
                    long memberCount = memberRepository.countByGroupAndJoinStatus(group, TravelGroupMember.JoinStatus.已加入);
                    if (memberCount >= group.getMaxMembers()) {
                        return false;
                    }
                    
                    // 搜索过滤
                    if (searchText != null && !searchText.trim().isEmpty()) {
                        String searchLower = searchText.toLowerCase().trim();
                        
                        switch (searchType) {
                            case "groupName":
                                if (!group.getTitle().toLowerCase().contains(searchLower)) {
                                    return false;
                                }
                                break;
                            case "creator":
                                if (group.getCreator() == null || !group.getCreator().getUsername().toLowerCase().contains(searchLower)) {
                                    return false;
                                }
                                break;
                            case "destination":
                                if (group.getDestination() == null || !group.getDestination().getName().toLowerCase().contains(searchLower)) {
                                    return false;
                                }
                                break;
                            default:
                                // 默认搜索团名
                                if (!group.getTitle().toLowerCase().contains(searchLower)) {
                                    return false;
                                }
                                break;
                        }
                    }
                    
                    // 日期范围过滤
                    if (startDate != null && !startDate.trim().isEmpty()) {
                        LocalDate start = LocalDate.parse(startDate);
                        if (group.getStartDate().isBefore(start)) {
                            return false;
                        }
                    }
                    
                    if (endDate != null && !endDate.trim().isEmpty()) {
                        LocalDate end = LocalDate.parse(endDate);
                        if (group.getEndDate().isAfter(end)) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .map(group -> {
                    List<TravelGroupMember> members = memberRepository.findByGroupId(group.getId());
                    return TravelGroupDTO.fromEntity(group, members);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TravelGroupDTO> getGroupsByDestination(Long destinationId, Long currentUserId) {
        List<TravelGroup> groups = travelGroupRepository.findByStatusOrderByCreatedAtDesc(TravelGroup.GroupStatus.招募中);
        
        return groups.stream()
                .filter(group -> {
                    // 检查是否为公开组团
                    if (!Boolean.TRUE.equals(group.getIsPublic())) {
                        return false;
                    }
                    
                    // 检查是否已满员（即使状态还是招募中）
                    long memberCount = memberRepository.countByGroupAndJoinStatus(group, TravelGroupMember.JoinStatus.已加入);
                    return memberCount < group.getMaxMembers();
                })
                .map(group -> TravelGroupDTO.fromEntity(group, userRepository.findById(currentUserId).orElse(null)))
                .collect(Collectors.toList());
    }

    @Override
    public List<TravelGroupDTO> getUserCreatedGroups(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        List<TravelGroup> groups = travelGroupRepository.findByCreatorId(userId);
        return groups.stream()
                .map(group -> {
                    List<TravelGroupMember> members = memberRepository.findByGroupId(group.getId());
                    return TravelGroupDTO.fromEntity(group, members);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TravelGroupDTO> getUserJoinedGroups(Long userId) {
        List<TravelGroupMember> memberships = memberRepository.findByUserId(userId);
        return memberships.stream()
                .map(member -> {
                    List<TravelGroupMember> members = memberRepository.findByGroupId(member.getGroup().getId());
                    return TravelGroupDTO.fromEntity(member.getGroup(), members);
                })
                .collect(Collectors.toList());
    }

    @Override
    public TravelGroupDTO getGroupDetail(Long groupId, Long userId) {
        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("组团不存在"));
        List<TravelGroupMember> members = memberRepository.findByGroupId(groupId);
        return TravelGroupDTO.fromEntity(group, members);
    }

    @Override
    public void processApplication(Long groupId, Long applicationId, Long processerId, boolean approve) {
        // 检查组团是否存在
        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("组团不存在"));
        
        // 检查是否为组团创建者
        if (!group.getCreator().getId().equals(processerId)) {
            throw new RuntimeException("只有组团创建者可以处理申请");
        }
        
        // 检查申请是否存在
        TravelGroupApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));
        
        // 检查申请是否属于该组团
        if (!application.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("申请不属于该组团");
        }
        
        // 检查申请状态是否为待处理
        if (application.getStatus() != TravelGroupApplication.ApplicationStatus.待审核) {
            throw new RuntimeException("该申请已被处理");
        }
        
        // 如果同意申请
        if (approve) {
            // 检查组团是否已满
            if (group.getCurrentMembers() >= group.getMaxMembers()) {
                throw new RuntimeException("组团已满员");
            }
            
            // 创建成员记录
            TravelGroupMember member = new TravelGroupMember();
            member.setGroup(group);
            member.setUser(application.getApplicant());
            member.setRole(TravelGroupMember.MemberRole.成员);
            member.setJoinStatus(TravelGroupMember.JoinStatus.已加入);
            member.setJoinDate(LocalDateTime.now());
            memberRepository.save(member);
            
            // 更新组团当前成员数
            group.setCurrentMembers(group.getCurrentMembers() + 1);
            
            // 如果组团已满，更新状态
            if (group.getCurrentMembers().equals(group.getMaxMembers())) {
                group.setStatus(TravelGroup.GroupStatus.已满员);
            }
            
            travelGroupRepository.save(group);

            // 2. 通过审核后加入群聊
            if (group.getGroupChatId() != null) {
                groupChatManageService.addUserToGroup(group.getGroupChatId(), application.getApplicant().getId());
            }
        }
        
        // 更新申请状态
        application.setStatus(approve ? 
            TravelGroupApplication.ApplicationStatus.已同意 : 
            TravelGroupApplication.ApplicationStatus.已拒绝);
        application.setProcessor(userRepository.findById(processerId)
                .orElseThrow(() -> new RuntimeException("处理人不存在")));
        application.setProcessDate(LocalDateTime.now());
        applicationRepository.save(application);
    }

    @Override
    public void withdrawApplication(Long groupId, Long userId) {
        // 检查申请是否存在
        Optional<TravelGroupApplication> applicationOpt = applicationRepository
                .findByGroupIdAndApplicantIdAndStatus(groupId, userId, TravelGroupApplication.ApplicationStatus.待审核);
        TravelGroupApplication application = applicationOpt
                .orElseThrow(() -> new RuntimeException("申请不存在"));
        
        // 检查申请状态
        if (application.getStatus() != TravelGroupApplication.ApplicationStatus.待审核) {
            throw new RuntimeException("该申请已被处理,无法撤回");
        }
        
        // 直接删除申请记录，而不是标记为已拒绝
        applicationRepository.delete(application);
    }





    @Override
    public void leaveGroup(Long groupId, Long userId) {
        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("组团不存在"));

        TravelGroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("您不是该组团的成员"));

        if (member.getRole() == TravelGroupMember.MemberRole.创建者) {
            throw new RuntimeException("创建者不能退出组团");
        }

        memberRepository.delete(member);
        group.setCurrentMembers(group.getCurrentMembers() - 1);

        // 如果之前是满员状态，现在变回招募中
        if (TravelGroup.GroupStatus.已满员.equals(group.getStatus())) {
            group.setStatus(TravelGroup.GroupStatus.招募中);
        }

        travelGroupRepository.save(group);
    }

    @Override
    public void cancelGroup(Long groupId, Long userId) {
        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("组团不存在"));

        if (!group.getCreator().getId().equals(userId)) {
            throw new RuntimeException("只有创建者可以取消组团");
        }

        if (!TravelGroup.GroupStatus.招募中.equals(group.getStatus())) {
            throw new RuntimeException("只能取消招募中的组团");
        }

        group.setStatus(TravelGroup.GroupStatus.已取消);
        travelGroupRepository.save(group);
    }

    @Override
    public TravelGroupDTO updateGroupStatus(Long groupId, String status, Long userId) {
        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("组团不存在"));

        if (!group.getCreator().getId().equals(userId)) {
            throw new RuntimeException("只有创建者可以更新组团状态");
        }

        // 验证状态转换的合法性
        if (TravelGroup.GroupStatus.已满员.equals(group.getStatus()) && "已结束".equals(status)) {
            group.setStatus(TravelGroup.GroupStatus.已结束);
        } else if (TravelGroup.GroupStatus.招募中.equals(group.getStatus()) && "已取消".equals(status)) {
            group.setStatus(TravelGroup.GroupStatus.已取消);
        } else {
            throw new RuntimeException("不允许的状态转换");
        }

        group = travelGroupRepository.save(group);
        return TravelGroupDTO.fromEntity(group, userRepository.findById(userId).orElse(null));
    }

    @Override
    public void applyToJoinGroup(Long groupId, Long userId, String message) {
        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("组团不存在"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 检查是否已经是成员
        if (memberRepository.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            throw new RuntimeException("您已经是该组团成员");
        }
        
        // 检查是否已经有待审核的申请
        Optional<TravelGroupApplication> existingPendingApplication = applicationRepository
                .findByGroupIdAndApplicantIdAndStatus(groupId, userId, TravelGroupApplication.ApplicationStatus.待审核);
        if (existingPendingApplication.isPresent()) {
            throw new RuntimeException("您已经申请过该组团，请等待审核结果");
        }
        
        // 检查是否有已同意的申请（用户可能已经是成员但记录不一致）
        Optional<TravelGroupApplication> approvedApplication = applicationRepository
                .findByGroupIdAndApplicantIdAndStatus(groupId, userId, TravelGroupApplication.ApplicationStatus.已同意);
        if (approvedApplication.isPresent()) {
            throw new RuntimeException("您的申请已被同意，请检查是否已经是成员");
        }
        
        // 创建新申请
        TravelGroupApplication application = new TravelGroupApplication();
        application.setGroup(group);
        application.setApplicant(user);
        application.setMessage(message);
        application.setStatus(TravelGroupApplication.ApplicationStatus.待审核);
        application.setApplyDate(LocalDateTime.now());
        
        try {
            applicationRepository.save(application);
        } catch (Exception e) {
            // 如果出现约束冲突，提供更友好的错误信息
            if (e.getMessage().contains("unique_pending_application")) {
                throw new RuntimeException("您已经申请过该组团，请等待审核结果");
            }
            throw new RuntimeException("申请失败，请稍后重试");
        }
    }

    @Override
    public void handleApplication(Long groupId, Long applicationId, Long processerId, boolean approve) {
        // 检查组团是否存在
        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("组团不存在"));
        
        // 检查是否为组团创建者
        if (!group.getCreator().getId().equals(processerId)) {
            throw new RuntimeException("只有组团创建者可以处理申请");
        }
        
        // 检查申请是否存在
        TravelGroupApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));
        
        // 检查申请是否属于该组团
        if (!application.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("申请不属于该组团");
        }
        
        // 检查申请状态是否为待审核
        if (application.getStatus() != TravelGroupApplication.ApplicationStatus.待审核) {
            throw new RuntimeException("该申请已被处理");
        }
        
        // 如果同意申请
        if (approve) {
            // 检查组团是否已满
            if (group.getCurrentMembers() >= group.getMaxMembers()) {
                throw new RuntimeException("组团已满员");
            }
            
            // 创建成员记录
            TravelGroupMember member = new TravelGroupMember();
            member.setGroup(group);
            member.setUser(application.getApplicant());
            member.setRole(TravelGroupMember.MemberRole.成员);
            member.setJoinDate(LocalDateTime.now());
            memberRepository.save(member);
            
            // 更新组团当前成员数
            group.setCurrentMembers(group.getCurrentMembers() + 1);
            
            // 如果组团已满，更新状态
            if (group.getCurrentMembers().equals(group.getMaxMembers())) {
                group.setStatus(TravelGroup.GroupStatus.已满员);
            }
            
            travelGroupRepository.save(group);
            
            // 更新申请状态为已通过
            application.setStatus(TravelGroupApplication.ApplicationStatus.已同意);
            application.setProcessDate(LocalDateTime.now());
            applicationRepository.save(application);
        } else {
            // 更新申请状态为已拒绝
            application.setStatus(TravelGroupApplication.ApplicationStatus.已拒绝);
            application.setProcessDate(LocalDateTime.now());
            applicationRepository.save(application);
        }
    }

    @Override
    public List<Map<String, Object>> getGroupApplications(Long groupId, Long userId) {
        // 验证用户权限
        TravelGroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("无权查看"));
        
        if (member.getRole() != TravelGroupMember.MemberRole.创建者 &&
            member.getRole() != TravelGroupMember.MemberRole.管理员) {
            throw new RuntimeException("无权查看");
        }
        
        List<TravelGroupApplication> applications = applicationRepository.findByGroupId(groupId);
        return applications.stream()
                .map(app -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", app.getId());
                    result.put("applicantId", app.getApplicant().getId());
                    result.put("applicantName", app.getApplicant().getUsername());
                    result.put("message", app.getMessage());
                    result.put("status", app.getStatus());
                    result.put("applyDate", app.getApplyDate());
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TravelGroupDTO> getRecommendedGroups(Long userId) {
        // 获取用户偏好
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 获取所有公开招募中的组团
        List<TravelGroup> allGroups = travelGroupRepository.findAll();
        
        // 根据用户偏好过滤和排序
        return allGroups.stream()
                .filter(group -> Boolean.TRUE.equals(group.getIsPublic()) && 
                               TravelGroup.GroupStatus.招募中.equals(group.getStatus()))
                .map(group -> {
                    List<TravelGroupMember> members = memberRepository.findByGroupId(group.getId());
                    return TravelGroupDTO.fromEntity(group, members);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TravelGroupDTO> getRecommendationsByPreferences(Long userId, List<String> preferences) {
        // 获取所有公开招募中的组团
        List<TravelGroup> allGroups = travelGroupRepository.findAll();
        
        // 根据偏好过滤和排序
        return allGroups.stream()
                .filter(group -> Boolean.TRUE.equals(group.getIsPublic()) && 
                               TravelGroup.GroupStatus.招募中.equals(group.getStatus()))
                .map(group -> {
                    List<TravelGroupMember> members = memberRepository.findByGroupId(group.getId());
                    return TravelGroupDTO.fromEntity(group, members);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getUserStatusInGroup(Long groupId, Long userId) {
        Map<String, Object> status = new HashMap<>();
        
        // 检查是否为创建者
        TravelGroup group = travelGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("组团不存在"));
        boolean isCreator = group.getCreator().getId().equals(userId);
        status.put("isCreator", isCreator);
        
        // 检查是否为成员
        boolean isMember = memberRepository.findByGroupIdAndUserId(groupId, userId).isPresent();
        status.put("isMember", isMember);
        
        // 检查是否有待处理的申请
        Optional<TravelGroupApplication> pendingApplication = applicationRepository
                .findByGroupIdAndApplicantIdAndStatus(groupId, userId, TravelGroupApplication.ApplicationStatus.待审核);
        boolean hasPendingApplication = pendingApplication.isPresent();
        status.put("hasPendingApplication", hasPendingApplication);
        
        return status;
    }

    @Override
    public void removeUserFromGroup(Long groupId, Long userId) {
        TravelGroup group = travelGroupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("组团不存在"));
        if (group.getGroupChatId() != null) {
            groupChatManageService.removeUserFromGroup(group.getGroupChatId(), userId);
        }
    }
}

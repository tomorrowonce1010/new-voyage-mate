package com.se_07.backend.service;

import com.se_07.backend.dto.CreateTravelGroupRequest;
import com.se_07.backend.dto.ItineraryCreateRequest;
import com.se_07.backend.dto.ItineraryDTO;
import com.se_07.backend.dto.TravelGroupDTO;
import com.se_07.backend.entity.TravelGroup;

import java.util.List;
import java.util.Map;

public interface TravelGroupService {
    
    /**
     * 创建组团
     */
    TravelGroupDTO createTravelGroup(CreateTravelGroupRequest request, Long userId);
    
    /**
     * 获取组团详情
     */
    TravelGroupDTO getGroupDetail(Long groupId, Long userId);
    
    /**
     * 获取所有公开的招募中组团
     */
    List<TravelGroupDTO> getPublicRecruitingGroups(Long currentUserId);
    
    /**
     * 获取所有公开的招募中组团（带搜索功能）
     */
    List<TravelGroupDTO> getPublicRecruitingGroupsWithSearch(Long currentUserId, String searchText, String searchType, String startDate, String endDate);
    
    /**
     * 根据目的地获取组团
     */
    List<TravelGroupDTO> getGroupsByDestination(Long destinationId, Long currentUserId);
    
    /**
     * 获取用户创建的组团
     */
    List<TravelGroupDTO> getUserCreatedGroups(Long userId);
    
    /**
     * 获取用户参与的组团
     */
    List<TravelGroupDTO> getUserJoinedGroups(Long userId);

    /**
     * 申请加入组团
     */
    void applyToJoinGroup(Long groupId, Long userId, String message);

    /**
     * 获取推荐的组团
     */
    List<TravelGroupDTO> getRecommendedGroups(Long userId);

    /**
     * 获取组团的申请列表
     */
    List<Map<String, Object>> getGroupApplications(Long groupId, Long userId);

    /**
     * 处理加入申请
     */
    void handleApplication(Long groupId, Long applicationId, Long processerId, boolean approve);

    /**
     * 根据用户偏好获取推荐组团
     */
    List<TravelGroupDTO> getRecommendationsByPreferences(Long userId, List<String> preferences);

    /**
     * 获取用户在组团中的状态
     */
    Map<String, Object> getUserStatusInGroup(Long groupId, Long userId);

    /**
     * 撤回申请
     */
    void withdrawApplication(Long groupId, Long userId);

    /**
     * 处理组团申请
     * @param groupId 组团ID
     * @param applicationId 申请ID
     * @param processerId 处理人ID
     * @param approve 是否同意
     */
    void processApplication(Long groupId, Long applicationId, Long processerId, boolean approve);
    
    /**
     * 更新组团状态
     */
    TravelGroupDTO updateGroupStatus(Long groupId, String status, Long userId);
    
    /**
     * 取消组团
     */
    void cancelGroup(Long groupId, Long userId);
    
    /**
     * 退出组团
     */
    void leaveGroup(Long groupId, Long userId);

    /**
     * 从组团中移除成员（并同步移除群聊成员）
     */
    void removeUserFromGroup(Long groupId, Long userId);
} 
package com.se_07.backend.service;

import com.se_07.backend.dto.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface UserService {

    /**
     * 获取完整用户档案
     * @param userId 用户ID
     * @return 用户档案响应
     */
    UserProfileResponse getUserProfile(Long userId);

    /**
     * 更新用户档案
     * @param userId 用户ID
     * @param request 更新请求
     * @return 更新后的用户档案响应
     */
    UserProfileResponse updateUserProfile(Long userId, UserProfileUpdateRequest request);

    /**
     * 更新用户偏好
     * @param userId 用户ID
     * @param request 更新请求
     * @return 更新后的用户档案响应
     */
    UserProfileResponse updateUserPreferences(Long userId, UserPreferencesUpdateRequest request);

    /**
     * 获取历史目的地
     * @param userId 用户ID
     * @return 历史目的地列表
     */
    List<UserProfileResponse.HistoryDestinationDto> getHistoryDestinations(Long userId);

    /**
     * 获取期望目的地
     * @param userId 用户ID
     * @return 期望目的地列表
     */
    List<UserProfileResponse.WishlistDestinationDto> getWishlistDestinations(Long userId);

    /**
     * 添加历史目的地
     * @param userId 用户ID
     * @param request 历史目的地请求
     */
    void addHistoryDestination(Long userId, AddHistoryDestinationRequest request);

    /**
     * 添加期望目的地
     * @param userId 用户ID
     * @param request 期望目的地请求
     */
    void addWishlistDestination(Long userId, AddWishlistDestinationRequest request);

    /**
     * 删除历史目的地
     * @param userId 用户ID
     * @param destinationId 目的地ID
     */
    void removeHistoryDestination(Long userId, Long destinationId);

    /**
     * 删除期望目的地
     * @param userId 用户ID
     * @param destinationId 目的地ID
     */
    void removeWishlistDestination(Long userId, Long destinationId);

    /**
     * 从用户的已出行行程中自动添加历史目的地
     * @param userId 用户ID
     * @return 添加的历史目的地数量
     */
    int addHistoryDestinationsFromCompletedItineraries(Long userId);

    /**
     * 删除指定行程中自动添加的历史目的地
     * @param userId 用户ID
     * @param itineraryId 行程ID
     * @return 删除的历史目的地数量
     */
    int removeAutoAddedHistoryDestinationsFromItinerary(Long userId, Long itineraryId);

    /**
     * 上传用户头像
     * @param userId 用户ID
     * @param file 头像文件
     * @return 头像URL
     */
    String uploadAvatar(Long userId, MultipartFile file);

    /**
     * 获取用户旅行统计数据
     * @param userId 用户ID
     * @return 旅行统计响应
     */
    TravelStatsResponse getTravelStats(Long userId);

    /**
     * 获取用户主页信息（包含公开行程）
     * @param userId 用户ID
     * @param requestIp 请求来源IP
     * @return 用户主页响应
     */
    UserHomepageResponse getUserHomepage(Long userId, String requestIp);

    /**
     * 搜索用户
     * @param username 用户名关键词
     * @param currentUserId 当前用户ID（用于过滤自己）
     * @return 用户列表
     */
    List<Map<String, Object>> searchUsers(String username, Long currentUserId);
}
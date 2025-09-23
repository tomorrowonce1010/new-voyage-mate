package com.se_07.backend.service;

import com.se_07.backend.dto.ActivityCreateRequest;
import com.se_07.backend.dto.AmapActivityCreateRequest;
import com.se_07.backend.dto.ItineraryActivityDTO;
import java.util.List;

public interface ItineraryActivityService {

    /**
     * 创建新的行程活动
     * @param userId 当前登录用户ID
     * @param request 创建活动的请求体
     * @return 创建好的活动 DTO
     */
    ItineraryActivityDTO createActivity(Long userId, ActivityCreateRequest request);

    /**
     * 根据日程ID获取该日程下的所有活动
     * @param userId 当前登录用户ID（用于鉴权）
     * @param itineraryDayId 日程ID
     * @return 活动DTO列表
     */
    java.util.List<ItineraryActivityDTO> getActivitiesByDay(Long userId, Long itineraryDayId);

    /**
     * 更新活动运输模式
     * @param userId 当前登录用户ID
     * @param activityId 活动ID
     * @param transportMode 新的运输模式
     * @return 更新后的活动DTO
     */
    ItineraryActivityDTO updateTransportMode(Long userId, Long activityId, String transportMode);

    /**
     * 更新活动的景点
     * @param userId 当前登录用户ID
     * @param activityId 活动ID
     * @param attractionId 新的景点ID
     * @return 更新后的活动DTO
     */
    ItineraryActivityDTO updateActivityAttraction(Long userId, Long activityId, Long attractionId);

    /**
     * 更新活动的备注
     * @param userId 当前登录用户ID
     * @param activityId 活动ID
     * @param attractionNotes 新的备注内容
     * @return 更新后的活动DTO
     */
    ItineraryActivityDTO updateActivityNotes(Long userId, Long activityId, String attractionNotes);

    /**
     * 更新活动的时间
     * @param userId 当前登录用户ID
     * @param activityId 活动ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 更新后的活动DTO
     */
    ItineraryActivityDTO updateActivityTime(Long userId, Long activityId, String startTime, String endTime);

    /**
     * 更新活动标题
     */
    ItineraryActivityDTO updateActivityTitle(Long userId, Long activityId, String title);

    /**
     * 删除活动
     * @param userId 当前登录用户ID
     * @param activityId 活动ID
     */
    void deleteActivity(Long userId, Long activityId);

    /**
     * 通过高德地图API创建行程活动
     * @param userId 当前登录用户ID
     * @param request 创建活动的请求体（包含高德地图景点信息）
     * @return 创建好的活动 DTO
     */
    ItineraryActivityDTO createActivityFromAmap(Long userId, AmapActivityCreateRequest request);

    /**
     * 通过高德地图API更新活动景点
     * @param userId 当前登录用户ID
     * @param activityId 活动ID
     * @param attractionInfo 新的景点信息
     * @return 更新后的活动DTO
     */
    ItineraryActivityDTO updateActivityAmapAttraction(Long userId, Long activityId, com.se_07.backend.dto.AmapActivityCreateRequest.AmapAttractionInfo attractionInfo);
}
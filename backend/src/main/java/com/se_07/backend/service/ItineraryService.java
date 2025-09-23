package com.se_07.backend.service;

import com.se_07.backend.dto.ItineraryDTO;
import com.se_07.backend.dto.ItineraryCreateRequest;
import com.se_07.backend.dto.ItineraryUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import com.se_07.backend.entity.Itinerary;
import org.springframework.web.multipart.MultipartFile;
import com.se_07.backend.dto.ShareCodeRequest;
import java.time.LocalDate;
import com.se_07.backend.dto.PermissionStatusResponse;

public interface ItineraryService {
    
    /**
     * 创建新行程
     * @param userId 用户ID
     * @param request 创建行程请求
     * @return 创建的行程DTO
     */
    ItineraryDTO createItinerary(Long userId, ItineraryCreateRequest request);
    
    /**
     * 获取用户的行程列表
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 行程DTO分页结果
     */
    List<ItineraryDTO> getUserItineraries(Long userId, Pageable pageable);
    
    /**
     * 根据ID获取行程详情
     * @param userId 用户ID
     * @param itineraryId 行程ID
     * @return 行程DTO详情
     */
    ItineraryDTO getItineraryById(Long userId, Long itineraryId);
    
    /**
     * 更新行程信息
     * @param userId 用户ID
     * @param itineraryId 行程ID
     * @param request 更新请求
     * @return 更新后的行程DTO
     */
    ItineraryDTO updateItinerary(Long userId, Long itineraryId, ItineraryUpdateRequest request);
    
    /**
     * 删除行程
     * @param userId 用户ID
     * @param itineraryId 行程ID
     */
    void deleteItinerary(Long userId, Long itineraryId);
    
    /**
     * 锁定行程（设为已出行）
     * @param itineraryId 行程ID
     * @param userId 用户ID
     * @return 更新后的行程DTO
     */
    ItineraryDTO lockItinerary(Long itineraryId, Long userId);
    
    /**
     * 获取用户的待出行行程
     * @param userId 用户ID
     * @return 待出行行程DTO列表
     */
    List<ItineraryDTO> getPendingItineraries(Long userId);
    
    /**
     * 获取用户的已出行行程
     * @param userId 用户ID
     * @return 已出行行程DTO列表
     */
    List<ItineraryDTO> getCompletedItineraries(Long userId);
    
    /**
     * 设置行程权限
     * @param itineraryId 行程ID
     * @param userId 用户ID
     * @param permissionStatus 权限状态
     * @return 更新后的行程DTO
     */
    ItineraryDTO setItineraryPermission(Long itineraryId, Long userId, String permissionStatus);
    
    /**
     * 更新行程基本信息
     * @param userId 用户ID
     * @param itineraryId 行程ID
     * @param updates 更新内容
     * @return 更新后的行程DTO
     */
    ItineraryDTO updateItineraryBasic(Long userId, Long itineraryId, Map<String, Object> updates);
    
    /**
     * 更新行程状态
     * @param userId 用户ID
     * @param itineraryId 行程ID
     * @param status 状态
     * @return 更新后的行程DTO
     */
    ItineraryDTO updateItineraryStatus(Long userId, Long itineraryId, String status);
    
    /**
     * 上传行程封面图片
     * @param userId 用户ID
     * @param itineraryId 行程ID
     * @param file 图片文件
     * @return 图片URL
     */
    String uploadCoverImage(Long userId, Long itineraryId, MultipartFile file);
    
    /**
     * 更新日程标题
     * @param userId 用户ID
     * @param itineraryId 行程ID
     * @param dayId 日程ID
     * @param newTitle 新标题
     */
    void updateDayTitle(Long userId, Long itineraryId, Long dayId, String newTitle);
    
    /**
     * 设置行程编辑状态为完成
     * @param userId 用户ID
     * @param itineraryId 行程ID
     * @return 更新后的行程DTO
     */
    ItineraryDTO setEditComplete(Long userId, Long itineraryId);
    
    /**
     * 平移行程开始日期，同时保持天数不变并同步更新所有日程日期
     * @param userId 用户ID
     * @param itineraryId 行程ID
     * @param newStartDate 新的开始日期 (yyyy-MM-dd)
     * @return 更新后的行程DTO
     */
    ItineraryDTO shiftItineraryDates(Long userId, Long itineraryId, LocalDate newStartDate);
    
    /**
     * 更新行程权限状态
     * @param itineraryId 行程ID
     * @param userId 用户ID
     * @param permissionStatus 权限状态
     * @return 权限状态更新响应
     */
    PermissionStatusResponse updatePermissionStatus(Long itineraryId, Long userId, String permissionStatus);
    
    /**
     * 生成行程分享码
     * @param itineraryId 行程ID
     * @param userId 用户ID
     * @param request 分享请求
     * @return 分享码
     */
    String generateShareCode(Long itineraryId, Long userId, ShareCodeRequest request);
    
    /**
     * 检查所有社区条目的索引状态（用于调试）
     */
    void checkAllCommunityEntriesIndexStatus();
    
    /**
     * 检查Elasticsearch中的文档数量（用于调试）
     */
    void checkElasticsearchDocumentCount();

    /**
     * 创建团队行程
     */
    ItineraryDTO createGroupItinerary(Long userId, Long groupId, ItineraryCreateRequest request, boolean isTemplate);

    /**
     * 获取团队行程列表
     */
    List<ItineraryDTO> getGroupItineraries(Long userId, Long groupId, Boolean templatesOnly);

    /**
     * 将行程设置为团队模板
     */
    ItineraryDTO setAsGroupTemplate(Long userId, Long groupId, Long itineraryId);

    /**
     * 获取用户的个人行程列表（group_id为null）
     */
    List<ItineraryDTO> getPersonalItineraries(Long userId, Pageable pageable);

    /**
     * 获取用户的团队行程列表（group_id不为null）
     */
    List<ItineraryDTO> getTeamItineraries(Long userId, Pageable pageable);

    /**
     * 导入AI生成的行程数据
     * @param userId 用户ID
     * @param importRequest AI生成的行程数据
     * @return 导入后的行程DTO
     */
    ItineraryDTO importAIItinerary(Long userId, com.se_07.backend.dto.AIItineraryImportRequest importRequest);
}
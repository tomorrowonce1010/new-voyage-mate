package com.se_07.backend.dto;

import com.se_07.backend.entity.Itinerary.TravelStatus;
import com.se_07.backend.entity.Itinerary.EditStatus;
import com.se_07.backend.entity.Itinerary.PermissionStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItineraryDTO {
    private Long id;
    private Long userId;
    private String username;  // 添加用户名，方便前端显示
    private String title;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private Integer travelerCount;
    private TravelStatus travelStatus;
    private EditStatus editStatus;
    private PermissionStatus permissionStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ItineraryDayDTO> itineraryDays;  // 使用DTO而不是实体
    private java.util.List<String> destinationNames;
    
    // 团队相关字段
    private Long groupId;  // 团队ID
    private String groupTitle;  // 团队标题
    private Boolean isGroupCreator;  // 是否为团队发起人
    private Boolean isTeamItinerary;  // 是否为团队行程
    private String userRole;  // 用户在团队中的角色（创建者/管理员/成员）
} 
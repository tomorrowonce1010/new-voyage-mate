package com.se_07.backend.dto;

import com.se_07.backend.entity.TravelGroup;
import com.se_07.backend.entity.TravelGroupMember;
import com.se_07.backend.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class TravelGroupDTO {
    private Long id;
    private String title;
    private String description;
    private String status;
    private Boolean isPublic;
    private LocalDate startDate;
    private LocalDate endDate;
    private int maxMembers;
    private int currentMembers;
    private BigDecimal estimatedBudget;
    private String groupType;
    private List<String> travelTags;
    private Map<String, Object> creator;
    private List<Map<String, Object>> members;
    private int availableSlots;
    private boolean hasGroupItinerary;
    private Long groupItineraryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long creatorId;
    private String creatorName;
    private Map<String, Object> destination; // 添加目的地信息

    public static TravelGroupDTO fromEntity(TravelGroup group, List<TravelGroupMember> members) {
        TravelGroupDTO dto = new TravelGroupDTO();
        dto.setId(group.getId());
        dto.setTitle(group.getTitle());
        dto.setDescription(group.getDescription());
        dto.setStatus(group.getStatus() != null ? group.getStatus().toString() : null);
        dto.setIsPublic(group.getIsPublic());
        dto.setStartDate(group.getStartDate());
        dto.setEndDate(group.getEndDate());
        dto.setMaxMembers(group.getMaxMembers());
        dto.setCurrentMembers(group.getCurrentMembers());
        dto.setEstimatedBudget(group.getEstimatedBudget());
        dto.setGroupType(group.getGroupType() != null ? group.getGroupType().toString() : null);
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());

        // 设置标签
        dto.setTravelTags(group.getTravelTags().stream()
                .map(groupTag -> groupTag.getTag().getTag())
                .collect(Collectors.toList()));

        // 设置创建者信息
        User creator = group.getCreator();
        if (creator != null) {
            dto.setCreatorId(creator.getId());
            dto.setCreatorName(creator.getUsername());

            Map<String, Object> creatorMap = new HashMap<>();
            creatorMap.put("id", creator.getId());
            creatorMap.put("username", creator.getUsername());
            dto.setCreator(creatorMap);
        }

        // 设置目的地信息
        if (group.getDestination() != null) {
            Map<String, Object> destinationMap = new HashMap<>();
            destinationMap.put("id", group.getDestination().getId());
            destinationMap.put("name", group.getDestination().getName());
            destinationMap.put("description", group.getDestination().getDescription());
            destinationMap.put("imageUrl", group.getDestination().getImageUrl());
            dto.setDestination(destinationMap);
        }

        // 设置成员信息
        if (members != null) {
            List<Map<String, Object>> memberList = new ArrayList<>();
            for (TravelGroupMember member : members) {
                Map<String, Object> memberMap = new HashMap<>();
                memberMap.put("id", member.getUser().getId());
                memberMap.put("username", member.getUser().getUsername());
                memberMap.put("role", member.getRole());
                memberMap.put("joinDate", member.getJoinDate());
                memberList.add(memberMap);
            }
            dto.setMembers(memberList);
        }

        // 计算可用名额
        dto.setAvailableSlots(group.getMaxMembers() - group.getCurrentMembers());

        // 设置行程信息 - 由于已删除itinerary字段，暂时设为false
        dto.setHasGroupItinerary(false);

        return dto;
    }

    public static TravelGroupDTO fromEntity(TravelGroup group, User currentUser) {
        TravelGroupDTO dto = new TravelGroupDTO();
        dto.setId(group.getId());
        dto.setTitle(group.getTitle());
        dto.setDescription(group.getDescription());
        dto.setStartDate(group.getStartDate());
        dto.setEndDate(group.getEndDate());
        dto.setMaxMembers(group.getMaxMembers());
        dto.setEstimatedBudget(group.getEstimatedBudget());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());
        
        User creator = group.getCreator();
        if (creator != null) {
            dto.setCreatorId(creator.getId());
            dto.setCreatorName(creator.getUsername());
        }
        
        return dto;
    }
} 
package com.se_07.backend.dto.converter;

import com.se_07.backend.dto.ItineraryDTO;
import com.se_07.backend.entity.Itinerary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

@Component
public class ItineraryConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(ItineraryConverter.class);
    
    @Autowired
    private ItineraryDayConverter itineraryDayConverter;
    
    public ItineraryDTO toDTO(Itinerary entity) {
        if (entity == null) {
            return null;
        }
        
        logger.debug("开始转换Itinerary实体到DTO - ID: {}, 出行人数: {}", entity.getId(), entity.getTravelerCount());
        
        ItineraryDTO dto = new ItineraryDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUser().getId());
        dto.setUsername(entity.getUser().getUsername());
        dto.setTitle(entity.getTitle());
        dto.setImageUrl(entity.getImageUrl());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setBudget(entity.getBudget());
        dto.setTravelerCount(entity.getTravelerCount());
        dto.setTravelStatus(entity.getTravelStatus());
        dto.setEditStatus(entity.getEditStatus());
        dto.setPermissionStatus(entity.getPermissionStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        // 设置团队相关字段
        dto.setGroupId(entity.getGroupId());
        dto.setIsTeamItinerary(entity.getGroupId() != null);
        
        logger.debug("DTO转换完成 - 出行人数: {}", dto.getTravelerCount());
        
        if (entity.getItineraryDays() != null) {
            dto.setItineraryDays(entity.getItineraryDays().stream()
                    .map(itineraryDayConverter::toDTO)
                    .collect(Collectors.toList()));
        }
        
        // 计算整条行程的目的地列表
        java.util.Set<String> itineraryDestSet = new java.util.HashSet<>();
        if (entity.getItineraryDays() != null) {
            entity.getItineraryDays().forEach(day -> {
                if (day.getActivities() != null) {
                    day.getActivities().forEach(act -> {
                        if (act.getAttraction() != null && act.getAttraction().getDestination() != null) {
                            itineraryDestSet.add(act.getAttraction().getDestination().getName());
                        }
                    });
                }
            });
        }
        dto.setDestinationNames(new java.util.ArrayList<>(itineraryDestSet));
        
        return dto;
    }
    
    public Itinerary toEntity(ItineraryDTO dto) {
        if (dto == null) {
            return null;
        }
        
        logger.debug("开始转换ItineraryDTO到实体 - ID: {}, 出行人数: {}", dto.getId(), dto.getTravelerCount());
        
        Itinerary entity = new Itinerary();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setImageUrl(dto.getImageUrl());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setBudget(dto.getBudget());
        entity.setTravelerCount(dto.getTravelerCount());
        entity.setTravelStatus(dto.getTravelStatus());
        entity.setEditStatus(dto.getEditStatus());
        entity.setPermissionStatus(dto.getPermissionStatus());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        
        logger.debug("实体转换完成 - 出行人数: {}", entity.getTravelerCount());
        
        return entity;
        // 注意：不设置user和itineraryDays，这些关系需要在service层处理
    }
} 
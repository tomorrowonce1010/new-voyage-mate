package com.se_07.backend.dto.converter;

import com.se_07.backend.dto.ItineraryDayDTO;
import com.se_07.backend.entity.ItineraryDay;
import org.springframework.stereotype.Component;

@Component
public class ItineraryDayConverter {
    
    public ItineraryDayDTO toDTO(ItineraryDay entity) {
        if (entity == null) {
            return null;
        }
        
        ItineraryDayDTO dto = new ItineraryDayDTO();
        dto.setId(entity.getId());
        dto.setDayNumber(entity.getDayNumber());
        dto.setDate(entity.getDate());
        dto.setTitle(entity.getTitle());
        dto.setFirstActivityId(entity.getFirstActivityId());
        dto.setLastActivityId(entity.getLastActivityId());
        dto.setNotes(entity.getNotes());
        dto.setActualCost(entity.getActualCost());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        // dto.setActivities(entity.getActivities()); // 避免懒加载问题，活动列表通过专门的接口获取
        
        // 设置itinerary相关信息
        if (entity.getItinerary() != null) {
            dto.setItineraryId(entity.getItinerary().getId());
            dto.setItineraryTitle(entity.getItinerary().getTitle());
            dto.setItineraryStartDate(entity.getItinerary().getStartDate());
            dto.setItineraryEndDate(entity.getItinerary().getEndDate());
            dto.setItineraryPermissionStatus(entity.getItinerary().getPermissionStatus().toString());
        }
        
        // 计算目的地列表
        java.util.Set<String> destSet = new java.util.HashSet<>();
        if (entity.getActivities() != null) {
            entity.getActivities().forEach(act -> {
                if (act.getAttraction() != null && act.getAttraction().getDestination() != null) {
                    destSet.add(act.getAttraction().getDestination().getName());
                }
            });
        }
        dto.setDestinationNames(new java.util.ArrayList<>(destSet));
        
        return dto;
    }
    
    public ItineraryDay toEntity(ItineraryDayDTO dto) {
        if (dto == null) {
            return null;
        }
        
        ItineraryDay entity = new ItineraryDay();
        entity.setId(dto.getId());
        entity.setDayNumber(dto.getDayNumber());
        entity.setDate(dto.getDate());
        entity.setTitle(dto.getTitle());
        entity.setFirstActivityId(dto.getFirstActivityId());
        entity.setLastActivityId(dto.getLastActivityId());
        entity.setNotes(dto.getNotes());
        entity.setActualCost(dto.getActualCost());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        // entity.setActivities(dto.getActivities()); // 避免懒加载问题，活动列表通过专门的接口获取
        
        return entity;
        // 注意：这里不设置itinerary，因为这需要在service层处理
    }
} 
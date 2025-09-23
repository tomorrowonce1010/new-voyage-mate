package com.se_07.backend.dto.converter;

import com.se_07.backend.dto.ItineraryActivityDTO;
import com.se_07.backend.entity.ItineraryActivity;
import org.springframework.stereotype.Component;

@Component
public class ItineraryActivityConverter {
    
    public ItineraryActivityDTO toDTO(ItineraryActivity entity) {
        if (entity == null) {
            return null;
        }
        
        ItineraryActivityDTO dto = new ItineraryActivityDTO();
        dto.setId(entity.getId());
        dto.setPrevId(entity.getPrevId());
        dto.setNextId(entity.getNextId());
        dto.setTitle(entity.getTitle());
        dto.setTransportMode(entity.getTransportMode());
        dto.setAttraction(entity.getAttraction());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setAttractionNotes(entity.getAttractionNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        // 设置itineraryDay相关信息
        if (entity.getItineraryDay() != null) {
            dto.setItineraryDayId(entity.getItineraryDay().getId());
            dto.setDayNumber(entity.getItineraryDay().getDayNumber());
            dto.setDate(entity.getItineraryDay().getDate());
        }
        
        return dto;
    }
    
    public ItineraryActivity toEntity(ItineraryActivityDTO dto) {
        if (dto == null) {
            return null;
        }
        
        ItineraryActivity entity = new ItineraryActivity();
        entity.setId(dto.getId());
        entity.setPrevId(dto.getPrevId());
        entity.setNextId(dto.getNextId());
        entity.setTitle(dto.getTitle());
        entity.setTransportMode(dto.getTransportMode());
        entity.setAttraction(dto.getAttraction());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setAttractionNotes(dto.getAttractionNotes());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        
        return entity;
        // 注意：这里不设置itineraryDay，因为这需要在service层处理
    }
} 
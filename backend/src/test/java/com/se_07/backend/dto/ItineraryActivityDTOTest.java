package com.se_07.backend.dto;

import com.se_07.backend.entity.Attraction;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class ItineraryActivityDTOTest {
    @Test
    void testGetterSetterAndEqualsHashCodeToString() {
        ItineraryActivityDTO dto1 = new ItineraryActivityDTO();
        ItineraryActivityDTO dto2 = new ItineraryActivityDTO();

        Long id = 1L;
        Long itineraryDayId = 2L;
        Integer dayNumber = 3;
        LocalDate date = LocalDate.of(2024, 6, 1);
        Long prevId = 4L;
        Long nextId = 5L;
        String title = "测试标题";
        String transportMode = "bus";
        Attraction attraction = new Attraction();
        attraction.setId(10L);
        attraction.setName("西湖");
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(10, 0);
        String transportNotes = "交通备注";
        String attractionNotes = "景点备注";
        LocalDateTime createdAt = LocalDateTime.of(2024, 6, 1, 8, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 9, 0);

        // set
        dto1.setId(id);
        dto1.setItineraryDayId(itineraryDayId);
        dto1.setDayNumber(dayNumber);
        dto1.setDate(date);
        dto1.setPrevId(prevId);
        dto1.setNextId(nextId);
        dto1.setTitle(title);
        dto1.setTransportMode(transportMode);
        dto1.setAttraction(attraction);
        dto1.setStartTime(startTime);
        dto1.setEndTime(endTime);
        dto1.setTransportNotes(transportNotes);
        dto1.setAttractionNotes(attractionNotes);
        dto1.setCreatedAt(createdAt);
        dto1.setUpdatedAt(updatedAt);

        // get
        assertEquals(id, dto1.getId());
        assertEquals(itineraryDayId, dto1.getItineraryDayId());
        assertEquals(dayNumber, dto1.getDayNumber());
        assertEquals(date, dto1.getDate());
        assertEquals(prevId, dto1.getPrevId());
        assertEquals(nextId, dto1.getNextId());
        assertEquals(title, dto1.getTitle());
        assertEquals(transportMode, dto1.getTransportMode());
        assertEquals(attraction, dto1.getAttraction());
        assertEquals(startTime, dto1.getStartTime());
        assertEquals(endTime, dto1.getEndTime());
        assertEquals(transportNotes, dto1.getTransportNotes());
        assertEquals(attractionNotes, dto1.getAttractionNotes());
        assertEquals(createdAt, dto1.getCreatedAt());
        assertEquals(updatedAt, dto1.getUpdatedAt());

        // equals & hashCode
        dto2.setId(id);
        dto2.setItineraryDayId(itineraryDayId);
        dto2.setDayNumber(dayNumber);
        dto2.setDate(date);
        dto2.setPrevId(prevId);
        dto2.setNextId(nextId);
        dto2.setTitle(title);
        dto2.setTransportMode(transportMode);
        dto2.setAttraction(attraction);
        dto2.setStartTime(startTime);
        dto2.setEndTime(endTime);
        dto2.setTransportNotes(transportNotes);
        dto2.setAttractionNotes(attractionNotes);
        dto2.setCreatedAt(createdAt);
        dto2.setUpdatedAt(updatedAt);
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());

        // toString
        String str = dto1.toString();
        assertTrue(str.contains("测试标题"));
        assertTrue(str.contains("bus"));
        assertTrue(str.contains("西湖"));
        assertTrue(str.contains("交通备注"));
        assertTrue(str.contains("景点备注"));
    }

    @Test
    void testNotEqualsAndNull() {
        ItineraryActivityDTO dto1 = new ItineraryActivityDTO();
        ItineraryActivityDTO dto2 = new ItineraryActivityDTO();
        dto1.setId(1L);
        dto2.setId(2L);
        assertNotEquals(dto1, dto2);
        assertNotEquals(dto1, null);
        assertNotEquals(dto1, new Object());
    }

    @Test
    void testEqualsAttractionNullCases() {
        ItineraryActivityDTO dto1 = new ItineraryActivityDTO();
        ItineraryActivityDTO dto2 = new ItineraryActivityDTO();
        dto1.setId(1L);
        dto2.setId(1L);
        // attraction都为null
        assertEquals(dto1, dto2);

        // 一个为null，一个不为null
        Attraction attraction = new Attraction();
        attraction.setId(10L);
        dto1.setAttraction(attraction);
        assertNotEquals(dto1, dto2);
        dto2.setAttraction(attraction);
        assertEquals(dto1, dto2);
    }

    @Test
    void testEqualsPartialFields() {
        ItineraryActivityDTO dto1 = new ItineraryActivityDTO();
        ItineraryActivityDTO dto2 = new ItineraryActivityDTO();
        dto1.setId(1L);
        dto1.setTitle("A");
        dto2.setId(1L);
        dto2.setTitle("B");
        // 只要有字段不同就不相等
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithSelf() {
        ItineraryActivityDTO dto = new ItineraryActivityDTO();
        dto.setId(1L);
        assertEquals(dto, dto);
    }

    @Test
    void testHashCodeConsistency() {
        ItineraryActivityDTO dto1 = new ItineraryActivityDTO();
        ItineraryActivityDTO dto2 = new ItineraryActivityDTO();
        dto1.setId(1L);
        dto2.setId(1L);
        assertEquals(dto1.hashCode(), dto2.hashCode());

        dto2.setId(2L);
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testToStringNullFields() {
        ItineraryActivityDTO dto = new ItineraryActivityDTO();
        String str = dto.toString();
        assertNotNull(str);
        // 字段为null时toString也能正常工作
    }
}
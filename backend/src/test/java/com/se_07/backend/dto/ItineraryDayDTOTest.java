package com.se_07.backend.dto;

import com.se_07.backend.entity.ItineraryActivity;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ItineraryDayDTOTest {
    @Test
    void testGetterSetterAndEqualsHashCodeToString() {
        ItineraryDayDTO dto1 = new ItineraryDayDTO();
        ItineraryDayDTO dto2 = new ItineraryDayDTO();

        Long id = 1L;
        Long itineraryId = 2L;
        String itineraryTitle = "行程标题";
        Integer dayNumber = 3;
        LocalDate date = LocalDate.of(2024, 6, 1);
        String title = "日程标题";
        Long firstActivityId = 4L;
        Long lastActivityId = 5L;
        String accommodation = "酒店";
        String notes = "备注";
        String weatherInfo = "晴";
        BigDecimal actualCost = new BigDecimal("888.88");
        LocalDateTime createdAt = LocalDateTime.of(2024, 6, 1, 8, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 9, 0);
        ItineraryActivity activity = new ItineraryActivity();
        activity.setId(100L);
        List<ItineraryActivity> activities = Collections.singletonList(activity);
        LocalDate itineraryStartDate = LocalDate.of(2024, 6, 1);
        LocalDate itineraryEndDate = LocalDate.of(2024, 6, 10);
        String itineraryPermissionStatus = "private";
        List<String> destinationNames = Arrays.asList("杭州", "上海");

        // set
        dto1.setId(id);
        dto1.setItineraryId(itineraryId);
        dto1.setItineraryTitle(itineraryTitle);
        dto1.setDayNumber(dayNumber);
        dto1.setDate(date);
        dto1.setTitle(title);
        dto1.setFirstActivityId(firstActivityId);
        dto1.setLastActivityId(lastActivityId);
        dto1.setAccommodation(accommodation);
        dto1.setNotes(notes);
        dto1.setWeatherInfo(weatherInfo);
        dto1.setActualCost(actualCost);
        dto1.setCreatedAt(createdAt);
        dto1.setUpdatedAt(updatedAt);
        dto1.setActivities(activities);
        dto1.setItineraryStartDate(itineraryStartDate);
        dto1.setItineraryEndDate(itineraryEndDate);
        dto1.setItineraryPermissionStatus(itineraryPermissionStatus);
        dto1.setDestinationNames(destinationNames);

        // get
        assertEquals(id, dto1.getId());
        assertEquals(itineraryId, dto1.getItineraryId());
        assertEquals(itineraryTitle, dto1.getItineraryTitle());
        assertEquals(dayNumber, dto1.getDayNumber());
        assertEquals(date, dto1.getDate());
        assertEquals(title, dto1.getTitle());
        assertEquals(firstActivityId, dto1.getFirstActivityId());
        assertEquals(lastActivityId, dto1.getLastActivityId());
        assertEquals(accommodation, dto1.getAccommodation());
        assertEquals(notes, dto1.getNotes());
        assertEquals(weatherInfo, dto1.getWeatherInfo());
        assertEquals(actualCost, dto1.getActualCost());
        assertEquals(createdAt, dto1.getCreatedAt());
        assertEquals(updatedAt, dto1.getUpdatedAt());
        assertEquals(activities, dto1.getActivities());
        assertEquals(itineraryStartDate, dto1.getItineraryStartDate());
        assertEquals(itineraryEndDate, dto1.getItineraryEndDate());
        assertEquals(itineraryPermissionStatus, dto1.getItineraryPermissionStatus());
        assertEquals(destinationNames, dto1.getDestinationNames());

        // equals & hashCode
        dto2.setId(id);
        dto2.setItineraryId(itineraryId);
        dto2.setItineraryTitle(itineraryTitle);
        dto2.setDayNumber(dayNumber);
        dto2.setDate(date);
        dto2.setTitle(title);
        dto2.setFirstActivityId(firstActivityId);
        dto2.setLastActivityId(lastActivityId);
        dto2.setAccommodation(accommodation);
        dto2.setNotes(notes);
        dto2.setWeatherInfo(weatherInfo);
        dto2.setActualCost(actualCost);
        dto2.setCreatedAt(createdAt);
        dto2.setUpdatedAt(updatedAt);
        dto2.setActivities(activities);
        dto2.setItineraryStartDate(itineraryStartDate);
        dto2.setItineraryEndDate(itineraryEndDate);
        dto2.setItineraryPermissionStatus(itineraryPermissionStatus);
        dto2.setDestinationNames(destinationNames);
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());

        // toString
        String str = dto1.toString();
        assertTrue(str.contains("行程标题"));
        assertTrue(str.contains("日程标题"));
        assertTrue(str.contains("酒店"));
        assertTrue(str.contains("杭州"));
        assertTrue(str.contains("private"));
    }

    @Test
    void testEqualsWithNullFields() {
        ItineraryDayDTO dto1 = new ItineraryDayDTO();
        ItineraryDayDTO dto2 = new ItineraryDayDTO();
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testEqualsWithSelf() {
        ItineraryDayDTO dto = new ItineraryDayDTO();
        dto.setId(1L);
        assertEquals(dto, dto);
    }

    @Test
    void testEqualsWithNullAndOtherType() {
        ItineraryDayDTO dto = new ItineraryDayDTO();
        assertNotEquals(dto, null);
        assertNotEquals(dto, new Object());
    }

    @Test
    void testEqualsPartialFields() {
        ItineraryDayDTO dto1 = new ItineraryDayDTO();
        ItineraryDayDTO dto2 = new ItineraryDayDTO();
        dto1.setId(1L);
        dto2.setId(2L);
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testHashCodeConsistency() {
        ItineraryDayDTO dto1 = new ItineraryDayDTO();
        ItineraryDayDTO dto2 = new ItineraryDayDTO();
        dto1.setTitle("A");
        dto2.setTitle("A");
        assertEquals(dto1.hashCode(), dto2.hashCode());
        dto2.setTitle("B");
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testToStringNullFields() {
        ItineraryDayDTO dto = new ItineraryDayDTO();
        String str = dto.toString();
        assertNotNull(str);
    }

    @Test
    void testEqualsActivitiesNullCases() {
        ItineraryDayDTO dto1 = new ItineraryDayDTO();
        ItineraryDayDTO dto2 = new ItineraryDayDTO();
        dto1.setTitle("A");
        dto2.setTitle("A");
        // activities都为null
        assertEquals(dto1, dto2);
        // 一个为null，一个不为null
        ItineraryActivity activity = new ItineraryActivity();
        activity.setId(1L);
        dto1.setActivities(Collections.singletonList(activity));
        assertNotEquals(dto1, dto2);
        dto2.setActivities(Collections.singletonList(activity));
        assertEquals(dto1, dto2);
    }

    @Test
    void testEqualsDestinationNamesNullCases() {
        ItineraryDayDTO dto1 = new ItineraryDayDTO();
        ItineraryDayDTO dto2 = new ItineraryDayDTO();
        dto1.setTitle("A");
        dto2.setTitle("A");
        // destinationNames都为null
        assertEquals(dto1, dto2);
        dto1.setDestinationNames(Arrays.asList("A"));
        assertNotEquals(dto1, dto2);
        dto2.setDestinationNames(Arrays.asList("A"));
        assertEquals(dto1, dto2);
    }

    @Test
    void testEqualsActivitiesNullAndEmpty() {
        ItineraryDayDTO dto1 = new ItineraryDayDTO();
        ItineraryDayDTO dto2 = new ItineraryDayDTO();
        dto1.setTitle("A");
        dto2.setTitle("A");
        // activities都为null
        assertEquals(dto1, dto2);
        // 一个为null，一个为空
        dto1.setActivities(null);
        dto2.setActivities(Collections.emptyList());
        assertNotEquals(dto1, dto2);
        // 都为空
        dto1.setActivities(Collections.emptyList());
        assertEquals(dto1, dto2);
    }

    @Test
    void testEqualsDestinationNamesNullAndEmpty() {
        ItineraryDayDTO dto1 = new ItineraryDayDTO();
        ItineraryDayDTO dto2 = new ItineraryDayDTO();
        dto1.setTitle("A");
        dto2.setTitle("A");
        // destinationNames都为null
        assertEquals(dto1, dto2);
        // 一个为null，一个为空
        dto1.setDestinationNames(null);
        dto2.setDestinationNames(Collections.emptyList());
        assertNotEquals(dto1, dto2);
        // 都为空
        dto1.setDestinationNames(Collections.emptyList());
        assertEquals(dto1, dto2);
    }

    @Test
    void testAllFieldsNull() {
        ItineraryDayDTO dto1 = new ItineraryDayDTO();
        ItineraryDayDTO dto2 = new ItineraryDayDTO();
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotNull(dto1.toString());
    }

    @Test
    void testAllFieldsDifferent() {
        ItineraryDayDTO dto1 = new ItineraryDayDTO();
        ItineraryDayDTO dto2 = new ItineraryDayDTO();
        dto1.setId(1L);
        dto2.setId(2L);
        dto1.setTitle("A");
        dto2.setTitle("B");
        dto1.setDayNumber(1);
        dto2.setDayNumber(2);
        dto1.setAccommodation("X");
        dto2.setAccommodation("Y");
        dto1.setDestinationNames(Arrays.asList("A"));
        dto2.setDestinationNames(Arrays.asList("B"));
        assertNotEquals(dto1, dto2);
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testPartialNullFields() {
        ItineraryDayDTO dto1 = new ItineraryDayDTO();
        ItineraryDayDTO dto2 = new ItineraryDayDTO();
        dto1.setTitle("A");
        // dto2.title为null
        assertNotEquals(dto1, dto2);
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testListOrderAffectsEquals() {
        ItineraryDayDTO dto1 = new ItineraryDayDTO();
        ItineraryDayDTO dto2 = new ItineraryDayDTO();
        dto1.setDestinationNames(Arrays.asList("A", "B"));
        dto2.setDestinationNames(Arrays.asList("B", "A"));
        assertNotEquals(dto1, dto2);
    }
}
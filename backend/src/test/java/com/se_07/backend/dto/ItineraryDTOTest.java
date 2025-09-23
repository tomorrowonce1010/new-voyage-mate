package com.se_07.backend.dto;

import com.se_07.backend.entity.Itinerary;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ItineraryDTOTest {
    @Test
    void testGetterSetterAndEqualsHashCodeToString() {
        ItineraryDTO dto1 = new ItineraryDTO();
        ItineraryDTO dto2 = new ItineraryDTO();

        Long id = 1L;
        Long userId = 2L;
        String username = "user";
        String title = "行程标题";
        String imageUrl = "img.jpg";
        LocalDate startDate = LocalDate.of(2024, 6, 1);
        LocalDate endDate = LocalDate.of(2024, 6, 10);
        BigDecimal budget = new BigDecimal("1234.56");
        Integer travelerCount = 5;
        Itinerary.TravelStatus travelStatus = Itinerary.TravelStatus.待出行;
        Itinerary.EditStatus editStatus = Itinerary.EditStatus.草稿;
        Itinerary.PermissionStatus permissionStatus = Itinerary.PermissionStatus.私人;
        LocalDateTime createdAt = LocalDateTime.of(2024, 6, 1, 8, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 9, 0);
        ItineraryDayDTO day = new ItineraryDayDTO();
        day.setId(100L);
        List<ItineraryDayDTO> itineraryDays = Collections.singletonList(day);
        List<String> destinationNames = Arrays.asList("杭州", "上海");
        Long groupId = 10L;
        String groupTitle = "团队";
        Boolean isGroupCreator = true;
        Boolean isTeamItinerary = true;
        String userRole = "管理员";

        // set
        dto1.setId(id);
        dto1.setUserId(userId);
        dto1.setUsername(username);
        dto1.setTitle(title);
        dto1.setImageUrl(imageUrl);
        dto1.setStartDate(startDate);
        dto1.setEndDate(endDate);
        dto1.setBudget(budget);
        dto1.setTravelerCount(travelerCount);
        dto1.setTravelStatus(travelStatus);
        dto1.setEditStatus(editStatus);
        dto1.setPermissionStatus(permissionStatus);
        dto1.setCreatedAt(createdAt);
        dto1.setUpdatedAt(updatedAt);
        dto1.setItineraryDays(itineraryDays);
        dto1.setDestinationNames(destinationNames);
        dto1.setGroupId(groupId);
        dto1.setGroupTitle(groupTitle);
        dto1.setIsGroupCreator(isGroupCreator);
        dto1.setIsTeamItinerary(isTeamItinerary);
        dto1.setUserRole(userRole);

        // get
        assertEquals(id, dto1.getId());
        assertEquals(userId, dto1.getUserId());
        assertEquals(username, dto1.getUsername());
        assertEquals(title, dto1.getTitle());
        assertEquals(imageUrl, dto1.getImageUrl());
        assertEquals(startDate, dto1.getStartDate());
        assertEquals(endDate, dto1.getEndDate());
        assertEquals(budget, dto1.getBudget());
        assertEquals(travelerCount, dto1.getTravelerCount());
        assertEquals(travelStatus, dto1.getTravelStatus());
        assertEquals(editStatus, dto1.getEditStatus());
        assertEquals(permissionStatus, dto1.getPermissionStatus());
        assertEquals(createdAt, dto1.getCreatedAt());
        assertEquals(updatedAt, dto1.getUpdatedAt());
        assertEquals(itineraryDays, dto1.getItineraryDays());
        assertEquals(destinationNames, dto1.getDestinationNames());
        assertEquals(groupId, dto1.getGroupId());
        assertEquals(groupTitle, dto1.getGroupTitle());
        assertEquals(isGroupCreator, dto1.getIsGroupCreator());
        assertEquals(isTeamItinerary, dto1.getIsTeamItinerary());
        assertEquals(userRole, dto1.getUserRole());

        // equals & hashCode
        dto2.setId(id);
        dto2.setUserId(userId);
        dto2.setUsername(username);
        dto2.setTitle(title);
        dto2.setImageUrl(imageUrl);
        dto2.setStartDate(startDate);
        dto2.setEndDate(endDate);
        dto2.setBudget(budget);
        dto2.setTravelerCount(travelerCount);
        dto2.setTravelStatus(travelStatus);
        dto2.setEditStatus(editStatus);
        dto2.setPermissionStatus(permissionStatus);
        dto2.setCreatedAt(createdAt);
        dto2.setUpdatedAt(updatedAt);
        dto2.setItineraryDays(itineraryDays);
        dto2.setDestinationNames(destinationNames);
        dto2.setGroupId(groupId);
        dto2.setGroupTitle(groupTitle);
        dto2.setIsGroupCreator(isGroupCreator);
        dto2.setIsTeamItinerary(isTeamItinerary);
        dto2.setUserRole(userRole);
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());

        // toString
        String str = dto1.toString();
        assertTrue(str.contains("行程标题"));
        assertTrue(str.contains("团队"));
        assertTrue(str.contains("杭州"));
        assertTrue(str.contains("管理员"));
    }

    @Test
    void testEqualsWithNullFields() {
        ItineraryDTO dto1 = new ItineraryDTO();
        ItineraryDTO dto2 = new ItineraryDTO();
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testEqualsWithSelf() {
        ItineraryDTO dto = new ItineraryDTO();
        dto.setId(1L);
        assertEquals(dto, dto);
    }

    @Test
    void testEqualsWithNullAndOtherType() {
        ItineraryDTO dto = new ItineraryDTO();
        assertNotEquals(dto, null);
        assertNotEquals(dto, new Object());
    }

    @Test
    void testEqualsPartialFields() {
        ItineraryDTO dto1 = new ItineraryDTO();
        ItineraryDTO dto2 = new ItineraryDTO();
        dto1.setId(1L);
        dto2.setId(2L);
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testHashCodeConsistency() {
        ItineraryDTO dto1 = new ItineraryDTO();
        ItineraryDTO dto2 = new ItineraryDTO();
        dto1.setTitle("A");
        dto2.setTitle("A");
        assertEquals(dto1.hashCode(), dto2.hashCode());
        dto2.setTitle("B");
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testToStringNullFields() {
        ItineraryDTO dto = new ItineraryDTO();
        String str = dto.toString();
        assertNotNull(str);
    }

    @Test
    void testEqualsItineraryDaysNullCases() {
        ItineraryDTO dto1 = new ItineraryDTO();
        ItineraryDTO dto2 = new ItineraryDTO();
        dto1.setTitle("A");
        dto2.setTitle("A");
        // itineraryDays都为null
        assertEquals(dto1, dto2);
        // 一个为null，一个不为null
        ItineraryDayDTO day = new ItineraryDayDTO();
        day.setId(1L);
        dto1.setItineraryDays(Collections.singletonList(day));
        assertNotEquals(dto1, dto2);
        dto2.setItineraryDays(Collections.singletonList(day));
        assertEquals(dto1, dto2);
    }

    @Test
    void testEqualsDestinationNamesNullCases() {
        ItineraryDTO dto1 = new ItineraryDTO();
        ItineraryDTO dto2 = new ItineraryDTO();
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
    void testListOrderAffectsEquals() {
        ItineraryDTO dto1 = new ItineraryDTO();
        ItineraryDTO dto2 = new ItineraryDTO();
        dto1.setDestinationNames(Arrays.asList("A", "B"));
        dto2.setDestinationNames(Arrays.asList("B", "A"));
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithDifferentIds() {
        ItineraryDTO dto1 = new ItineraryDTO();
        dto1.setId(1L);

        ItineraryDTO dto2 = new ItineraryDTO();
        dto2.setId(2L);

        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithNullUsername() {
        ItineraryDTO dto1 = new ItineraryDTO();
        dto1.setUsername(null);

        ItineraryDTO dto2 = new ItineraryDTO();
        dto2.setUsername("user");

        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithDifferentBudget() {
        ItineraryDTO dto1 = new ItineraryDTO();
        dto1.setBudget(new BigDecimal("100.00"));

        ItineraryDTO dto2 = new ItineraryDTO();
        dto2.setBudget(new BigDecimal("200.00"));

        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithNullBudget() {
        ItineraryDTO dto1 = new ItineraryDTO();
        dto1.setBudget(null);

        ItineraryDTO dto2 = new ItineraryDTO();
        dto2.setBudget(new BigDecimal("100.00"));

        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithDifferentTravelStatus() {
        ItineraryDTO dto1 = new ItineraryDTO();
        dto1.setTravelStatus(Itinerary.TravelStatus.待出行);

        ItineraryDTO dto2 = new ItineraryDTO();
        dto2.setTravelStatus(Itinerary.TravelStatus.已出行);

        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithNullItineraryDays() {
        ItineraryDTO dto1 = new ItineraryDTO();
        dto1.setItineraryDays(null);

        ItineraryDTO dto2 = new ItineraryDTO();
        dto2.setItineraryDays(Collections.emptyList());

        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithDifferentGroupId() {
        ItineraryDTO dto1 = new ItineraryDTO();
        dto1.setGroupId(1L);

        ItineraryDTO dto2 = new ItineraryDTO();
        dto2.setGroupId(2L);

        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithNullGroupTitle() {
        ItineraryDTO dto1 = new ItineraryDTO();
        dto1.setGroupTitle(null);

        ItineraryDTO dto2 = new ItineraryDTO();
        dto2.setGroupTitle("Team A");

        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithDifferentIsGroupCreator() {
        ItineraryDTO dto1 = new ItineraryDTO();
        dto1.setIsGroupCreator(true);

        ItineraryDTO dto2 = new ItineraryDTO();
        dto2.setIsGroupCreator(false);

        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithNullUserRole() {
        ItineraryDTO dto1 = new ItineraryDTO();
        dto1.setUserRole(null);

        ItineraryDTO dto2 = new ItineraryDTO();
        dto2.setUserRole("ADMIN");

        assertNotEquals(dto1, dto2);
    }

    @Test
    void testHashCodeWithNullFields() {
        ItineraryDTO dto = new ItineraryDTO();
        dto.setTitle(null);
        dto.setImageUrl(null);
        dto.setBudget(null);

        // 验证不会抛出NPE
        assertDoesNotThrow(dto::hashCode);
    }

    @Test
    void testToStringWithNullFields() {
        ItineraryDTO dto = new ItineraryDTO();
        dto.setTitle(null);
        dto.setImageUrl(null);
        dto.setBudget(null);

        String result = dto.toString();
        assertNotNull(result);
        assertTrue(result.contains("null"));
    }
}
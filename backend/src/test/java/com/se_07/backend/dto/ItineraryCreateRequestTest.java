package com.se_07.backend.dto;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ItineraryCreateRequestTest {
    @Test
    void testGetterSetterAndEqualsHashCodeToString() {
        ItineraryCreateRequest req1 = new ItineraryCreateRequest();
        ItineraryCreateRequest req2 = new ItineraryCreateRequest();

        String title = "测试行程";
        String imageUrl = "http://img.com/1.jpg";
        LocalDate startDate = LocalDate.of(2024, 6, 1);
        LocalDate endDate = LocalDate.of(2024, 6, 10);
        BigDecimal budget = new BigDecimal("1234.56");
        Integer travelerCount = 5;
        String travelStatus = "未出行";
        String permissionStatus = "private";

        // set
        req1.setTitle(title);
        req1.setImageUrl(imageUrl);
        req1.setStartDate(startDate);
        req1.setEndDate(endDate);
        req1.setBudget(budget);
        req1.setTravelerCount(travelerCount);
        req1.setTravelStatus(travelStatus);
        req1.setPermissionStatus(permissionStatus);

        // get
        assertEquals(title, req1.getTitle());
        assertEquals(imageUrl, req1.getImageUrl());
        assertEquals(startDate, req1.getStartDate());
        assertEquals(endDate, req1.getEndDate());
        assertEquals(budget, req1.getBudget());
        assertEquals(travelerCount, req1.getTravelerCount());
        assertEquals(travelStatus, req1.getTravelStatus());
        assertEquals(permissionStatus, req1.getPermissionStatus());

        // equals & hashCode
        req2.setTitle(title);
        req2.setImageUrl(imageUrl);
        req2.setStartDate(startDate);
        req2.setEndDate(endDate);
        req2.setBudget(budget);
        req2.setTravelerCount(travelerCount);
        req2.setTravelStatus(travelStatus);
        req2.setPermissionStatus(permissionStatus);
        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());

        // toString
        String str = req1.toString();
        assertTrue(str.contains("测试行程"));
        assertTrue(str.contains("1234.56"));
        assertTrue(str.contains("private"));
    }

    @Test
    void testNotEqualsAndNull() {
        ItineraryCreateRequest req1 = new ItineraryCreateRequest();
        ItineraryCreateRequest req2 = new ItineraryCreateRequest();
        req1.setTitle("A");
        req2.setTitle("B");
        assertNotEquals(req1, req2);
        assertNotEquals(req1, null);
        assertNotEquals(req1, new Object());
    }

    @Test
    void testEqualsWithSelf() {
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        req.setTitle("A");
        assertEquals(req, req);
    }

    @Test
    void testHashCodeConsistency() {
        ItineraryCreateRequest req1 = new ItineraryCreateRequest();
        ItineraryCreateRequest req2 = new ItineraryCreateRequest();
        req1.setTitle("A");
        req2.setTitle("A");
        assertEquals(req1.hashCode(), req2.hashCode());
        req2.setTitle("B");
        assertNotEquals(req1.hashCode(), req2.hashCode());
    }

    @Test
    void testEqualsBudgetNullCases() {
        ItineraryCreateRequest req1 = new ItineraryCreateRequest();
        ItineraryCreateRequest req2 = new ItineraryCreateRequest();
        req1.setTitle("A");
        req2.setTitle("A");
        // budget都为null
        assertEquals(req1, req2);
        // 一个为null，一个不为null
        req1.setBudget(new BigDecimal("1.23"));
        assertNotEquals(req1, req2);
        req2.setBudget(new BigDecimal("1.23"));
        assertEquals(req1, req2);
    }

    @Test
    void testEqualsStartDateNullCases() {
        ItineraryCreateRequest req1 = new ItineraryCreateRequest();
        ItineraryCreateRequest req2 = new ItineraryCreateRequest();
        req1.setTitle("A");
        req2.setTitle("A");
        // startDate都为null
        assertEquals(req1, req2);
        req1.setStartDate(LocalDate.of(2024, 6, 1));
        assertNotEquals(req1, req2);
        req2.setStartDate(LocalDate.of(2024, 6, 1));
        assertEquals(req1, req2);
    }

    @Test
    void testEqualsEndDateNullCases() {
        ItineraryCreateRequest req1 = new ItineraryCreateRequest();
        ItineraryCreateRequest req2 = new ItineraryCreateRequest();
        req1.setTitle("A");
        req2.setTitle("A");
        // endDate都为null
        assertEquals(req1, req2);
        req1.setEndDate(LocalDate.of(2024, 6, 2));
        assertNotEquals(req1, req2);
        req2.setEndDate(LocalDate.of(2024, 6, 2));
        assertEquals(req1, req2);
    }

    @Test
    void testEqualsTravelerCountNullCases() {
        ItineraryCreateRequest req1 = new ItineraryCreateRequest();
        ItineraryCreateRequest req2 = new ItineraryCreateRequest();
        req1.setTitle("A");
        req2.setTitle("A");
        // travelerCount都为null
        assertEquals(req1, req2);
        req1.setTravelerCount(1);
        assertNotEquals(req1, req2);
        req2.setTravelerCount(1);
        assertEquals(req1, req2);
    }

    @Test
    void testEqualsPermissionStatusNullCases() {
        ItineraryCreateRequest req1 = new ItineraryCreateRequest();
        ItineraryCreateRequest req2 = new ItineraryCreateRequest();
        req1.setTitle("A");
        req2.setTitle("A");
        // permissionStatus都为null
        assertEquals(req1, req2);
        req1.setPermissionStatus("private");
        assertNotEquals(req1, req2);
        req2.setPermissionStatus("private");
        assertEquals(req1, req2);
    }

    @Test
    void testEqualsWithDifferentTypeAndNull() {
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        assertNotEquals(req, null);
        assertNotEquals(req, new Object());
        assertEquals(req, req);
    }

    @Test
    void testToStringNullFields() {
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        String str = req.toString();
        assertNotNull(str);
    }

    @Test
    void testEqualsPartialFields() {
        ItineraryCreateRequest req1 = new ItineraryCreateRequest();
        ItineraryCreateRequest req2 = new ItineraryCreateRequest();
        req1.setTitle("A");
        req2.setTitle("B");
        assertNotEquals(req1, req2);
    }
}
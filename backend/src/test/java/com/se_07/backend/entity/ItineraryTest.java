package com.se_07.backend.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class ItineraryTest {
    @Test void testGetterSetterEqualsHashCodeToString() {
        Itinerary i1 = new Itinerary();
        i1.setId(1L);
        i1.setTitle("t");
        i1.setUser(new User());
        i1.setStartDate(LocalDate.now());
        i1.setEndDate(LocalDate.now().plusDays(1));
        i1.setBudget(BigDecimal.TEN);
        i1.setTravelerCount(2);
        i1.setImageUrl("img");
        i1.setGroupId(3L);
        i1.setTravelStatus(Itinerary.TravelStatus.待出行);
        i1.setEditStatus(Itinerary.EditStatus.草稿);
        i1.setPermissionStatus(Itinerary.PermissionStatus.私人);
        i1.setItineraryDays(Collections.emptyList());
        i1.setCreatedAt(LocalDate.now().atStartOfDay());
        i1.setUpdatedAt(LocalDate.now().atStartOfDay());
        assertEquals(1L, i1.getId());
        assertEquals("t", i1.getTitle());
        assertNotNull(i1.getUser());
        assertEquals(LocalDate.now(), i1.getStartDate());
        assertEquals(LocalDate.now().plusDays(1), i1.getEndDate());
        assertEquals(BigDecimal.TEN, i1.getBudget());
        assertEquals(2, i1.getTravelerCount());
        assertEquals("img", i1.getImageUrl());
        assertEquals(3L, i1.getGroupId());
        assertEquals(Itinerary.TravelStatus.待出行, i1.getTravelStatus());
        assertEquals(Itinerary.EditStatus.草稿, i1.getEditStatus());
        assertEquals(Itinerary.PermissionStatus.私人, i1.getPermissionStatus());
        assertEquals(Collections.emptyList(), i1.getItineraryDays());
        assertNotNull(i1.getCreatedAt());
        assertNotNull(i1.getUpdatedAt());
        // equals/hashCode
        Itinerary i2 = new Itinerary();
        i2.setId(1L);
        assertNotEquals(i1, i2);
        assertNotEquals(i1.hashCode(), i2.hashCode());
        i2.setId(2L);
        assertNotEquals(i1, i2);
        // toString
        assertTrue(i1.toString().contains("t"));
    }
    @Test void testExtremeEqualsHashCode() {
        Itinerary i1 = new Itinerary();
        Itinerary i2 = new Itinerary();
        assertEquals(i1, i2);
        assertEquals(i1.hashCode(), i2.hashCode());
        i1.setId(1L);
        assertNotEquals(i1, i2);
    }
    @Test void testEqualsHashCodeExtremeCases() {
        Itinerary i1 = new Itinerary();
        Itinerary i2 = new Itinerary();
        // id都为null，内容全相同
        assertEquals(i1, i2);
        assertEquals(i1.hashCode(), i2.hashCode());
        // id不同
        i1.setId(1L); i2.setId(2L);
        assertNotEquals(i1, i2);
        // id相同但其它字段不同
        i2.setId(1L); i2.setTitle("diff");
        assertNotEquals(i1, i2);
        // List顺序不同
        i1.setItineraryDays(Arrays.asList());
        i2.setItineraryDays(Arrays.asList(new com.se_07.backend.entity.ItineraryDay()));
        assertNotEquals(i1, i2);
        // null与非null
        assertNotEquals(i1, null);
        // 同对象
        assertEquals(i1, i1);
        // 不同类型
        assertNotEquals(i1, "str");
    }
    @Test void testToStringAllFields() {
        Itinerary i = new Itinerary();
        i.setId(1L); i.setTitle("t"); i.setUser(new User());
        i.setStartDate(LocalDate.now()); i.setEndDate(LocalDate.now().plusDays(1));
        i.setBudget(BigDecimal.ONE); i.setTravelerCount(1); i.setImageUrl("img");
        i.setGroupId(2L); i.setTravelStatus(Itinerary.TravelStatus.待出行);
        i.setEditStatus(Itinerary.EditStatus.草稿); i.setPermissionStatus(Itinerary.PermissionStatus.私人);
        i.setItineraryDays(Arrays.asList(new com.se_07.backend.entity.ItineraryDay()));
        i.setCreatedAt(LocalDate.now().atStartOfDay()); i.setUpdatedAt(LocalDate.now().atStartOfDay());
        String s = i.toString();
        assertTrue(s.contains("t"));
        assertTrue(s.contains("img"));
        assertTrue(s.contains("待出行") || s.contains("草稿") || s.contains("私人"));
    }
} 
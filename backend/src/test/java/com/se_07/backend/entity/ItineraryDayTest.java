package com.se_07.backend.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ItineraryDayTest {
    @Test void testGetterSetterEqualsHashCodeToString() {
        ItineraryDay d1 = new ItineraryDay();
        d1.setId(1L);
        d1.setItinerary(new Itinerary());
        d1.setDayNumber(2);
        d1.setDate(LocalDate.now());
        d1.setTitle("day");
        d1.setFirstActivityId(10L);
        d1.setLastActivityId(20L);
        assertEquals(1L, d1.getId());
        assertNotNull(d1.getItinerary());
        assertEquals(2, d1.getDayNumber());
        assertEquals(LocalDate.now(), d1.getDate());
        assertEquals("day", d1.getTitle());
        assertEquals(10L, d1.getFirstActivityId());
        assertEquals(20L, d1.getLastActivityId());
        // equals/hashCode
        ItineraryDay d2 = new ItineraryDay();
        d2.setId(1L);
        assertNotEquals(d1, d2);
        assertNotEquals(d1.hashCode(), d2.hashCode());
        d2.setId(2L);
        assertNotEquals(d1, d2);
        // toString
        assertTrue(d1.toString().contains("day"));
    }
    @Test void testExtremeEqualsHashCode() {
        ItineraryDay d1 = new ItineraryDay();
        ItineraryDay d2 = new ItineraryDay();
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
        d1.setId(1L);
        assertNotEquals(d1, d2);
    }
    @Test void testEqualsHashCodeExtremeCases() {
        ItineraryDay d1 = new ItineraryDay();
        ItineraryDay d2 = new ItineraryDay();
        // id都为null
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
        // id不同
        d1.setId(1L); d2.setId(2L);
        assertNotEquals(d1, d2);
        // id相同但其它字段不同
        d2.setId(1L); d2.setTitle("diff");
        assertNotEquals(d1, d2);
        // null与非null
        assertNotEquals(d1, null);
        // 同对象
        assertEquals(d1, d1);
        // 不同类型
        assertNotEquals(d1, "str");
    }
    @Test void testToStringAllFields() {
        ItineraryDay d = new ItineraryDay();
        d.setId(1L); d.setItinerary(new Itinerary()); d.setDayNumber(2);
        d.setDate(LocalDate.now()); d.setTitle("day"); d.setFirstActivityId(10L); d.setLastActivityId(20L);
        String s = d.toString();
        assertTrue(s.contains("day"));
        assertTrue(s.contains("10") || s.contains("20"));
    }
} 
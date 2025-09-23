package com.se_07.backend.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class ItineraryActivityTest {
    @Test void testGetterSetterEqualsHashCodeToString() {
        ItineraryActivity a1 = new ItineraryActivity();
        a1.setId(1L);
        a1.setItineraryDay(new ItineraryDay());
        a1.setTitle("a");
        a1.setAttraction(new Attraction());
        a1.setStartTime(LocalTime.of(8,0));
        a1.setEndTime(LocalTime.of(9,0));
        a1.setTransportMode("bus");
        a1.setAttractionNotes("note");
        a1.setPrevId(2L);
        a1.setNextId(3L);
        assertEquals(1L, a1.getId());
        assertNotNull(a1.getItineraryDay());
        assertEquals("a", a1.getTitle());
        assertNotNull(a1.getAttraction());
        assertEquals(LocalTime.of(8,0), a1.getStartTime());
        assertEquals(LocalTime.of(9,0), a1.getEndTime());
        assertEquals("bus", a1.getTransportMode());
        assertEquals("note", a1.getAttractionNotes());
        assertEquals(2L, a1.getPrevId());
        assertEquals(3L, a1.getNextId());
        // equals/hashCode
        ItineraryActivity a2 = new ItineraryActivity();
        a2.setId(1L);
        assertNotEquals(a1, a2);
        assertNotEquals(a1.hashCode(), a2.hashCode());
        a2.setId(2L);
        assertNotEquals(a1, a2);
        // toString
        assertTrue(a1.toString().contains("a"));
    }
    @Test void testExtremeEqualsHashCode() {
        ItineraryActivity a1 = new ItineraryActivity();
        ItineraryActivity a2 = new ItineraryActivity();
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        a1.setId(1L);
        assertNotEquals(a1, a2);
    }
    @Test void testEqualsHashCodeExtremeCases() {
        ItineraryActivity a1 = new ItineraryActivity();
        ItineraryActivity a2 = new ItineraryActivity();
        // id都为null
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        // id不同
        a1.setId(1L); a2.setId(2L);
        assertNotEquals(a1, a2);
        // id相同但其它字段不同
        a2.setId(1L); a2.setTitle("diff");
        assertNotEquals(a1, a2);
        // null与非null
        assertNotEquals(a1, null);
        // 同对象
        assertEquals(a1, a1);
        // 不同类型
        assertNotEquals(a1, "str");
    }
    @Test void testToStringAllFields() {
        ItineraryActivity a = new ItineraryActivity();
        a.setId(1L); a.setItineraryDay(new ItineraryDay()); a.setTitle("a");
        a.setAttraction(new Attraction()); a.setStartTime(LocalTime.of(8,0)); a.setEndTime(LocalTime.of(9,0));
        a.setTransportMode("bus"); a.setAttractionNotes("note"); a.setPrevId(2L); a.setNextId(3L);
        String s = a.toString();
        assertTrue(s.contains("a"));
        assertTrue(s.contains("bus") || s.contains("note"));
    }
} 
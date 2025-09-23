package com.se_07.backend.chat.Entity;

import com.se_07.backend.entity.FriendId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FriendIdTest {
    @Test
    void testEqualsAndHashCode() {
        FriendId id1 = new FriendId(1L, 2L);
        FriendId id2 = new FriendId(1L, 2L);
        FriendId id3 = new FriendId(2L, 1L);
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1, id3);
    }

    @Test
    void testEqualsWithNullAndOtherType() {
        FriendId id1 = new FriendId(1L, 2L);
        assertNotEquals(id1, null);
        assertNotEquals(id1, "string");
    }

    @Test
    void testEqualsSelf() {
        FriendId id1 = new FriendId(1L, 2L);
        assertEquals(id1, id1);
    }

    @Test
    void testEmptyConstructor() {
        FriendId id = new FriendId();
        assertNull(getField(id, "id"));
        assertNull(getField(id, "friendId"));
    }

    @Test
    void testToString() {
        FriendId id = new FriendId(1L, 2L);
        String str = id.toString();
        assertTrue(str.contains("2"));
    }

    // 反射获取私有字段值
    private Object getField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }
} 
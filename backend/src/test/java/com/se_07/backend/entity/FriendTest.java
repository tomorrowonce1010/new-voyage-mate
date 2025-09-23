package com.se_07.backend.chat.Entity;

import com.se_07.backend.entity.Friend;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class FriendTest {

    @Test
    void testGetterSetter() {
        Friend friend = new Friend();
        friend.setId(1L);
        friend.setFriendId(2L);
        assertEquals(1L, friend.getId());
        assertEquals(2L, friend.getFriendId());
    }

    @Test
    void testEqualsAndHashCode_sameValues() {
        Friend f1 = new Friend();
        f1.setId(1L);
        f1.setFriendId(2L);

        Friend f2 = new Friend();
        f2.setId(1L);
        f2.setFriendId(2L);

        // Test equality and hashCode
        assertEquals(f1, f2); // same values should be equal
        assertEquals(f1.hashCode(), f2.hashCode()); // hash codes should be equal
    }

    @Test
    void testEquals_differentValues() {
        Friend f1 = new Friend();
        f1.setId(1L);
        f1.setFriendId(2L);

        Friend f2 = new Friend();
        f2.setId(2L);
        f2.setFriendId(3L);

        assertNotEquals(f1, f2); // different values should not be equal
        assertNotEquals(f1.hashCode(), f2.hashCode()); // hash codes should differ
    }

    @Test
    void testEquals_nullAndDifferentType() {
        Friend f1 = new Friend();
        f1.setId(1L);
        f1.setFriendId(2L);

        assertNotEquals(f1, null); // should not be equal to null
        assertNotEquals(f1, "not a Friend"); // should not be equal to different type
    }

    @Test
    void testEquals_reflexivity() {
        Friend f1 = new Friend();
        f1.setId(1L);
        f1.setFriendId(2L);
        assertEquals(f1, f1); // reflexivity check
    }

    @Test
    void testCanEqual() throws Exception {
        Friend f1 = new Friend();

        // Using reflection to call protected canEqual method
        Method canEqualMethod = Friend.class.getDeclaredMethod("canEqual", Object.class);
        canEqualMethod.setAccessible(true); // Allow access to protected method

        assertTrue((Boolean) canEqualMethod.invoke(f1, new Friend())); // test valid equality
        assertFalse((Boolean) canEqualMethod.invoke(f1, "not a friend")); // test invalid equality
    }

    @Test
    void testToString() {
        Friend f = new Friend();
        f.setId(1L);
        f.setFriendId(2L);
        String s = f.toString();
        assertTrue(s.contains("1"));
        assertTrue(s.contains("2"));
    }

    @Test
    void testConstructor() {
        Friend friend = new Friend();
        assertNotNull(friend);
    }

    @Test
    void testHashCodeConsistency() {
        Friend f1 = new Friend();
        f1.setId(1L);
        f1.setFriendId(2L);

        // hashCode should be consistent between multiple calls
        int hash1 = f1.hashCode();
        int hash2 = f1.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    void testHashCode_diffValues() {
        Friend f1 = new Friend();
        f1.setId(1L);
        f1.setFriendId(2L);

        Friend f2 = new Friend();
        f2.setId(2L);
        f2.setFriendId(3L);

        // hashCode should be different for different values
        assertNotEquals(f1.hashCode(), f2.hashCode());
    }

    @Test
    void testEqualityWithDifferentInstances() {
        Friend f1 = new Friend();
        f1.setId(1L);
        f1.setFriendId(2L);

        Friend f2 = new Friend();
        f2.setId(1L);
        f2.setFriendId(2L);

        // Check equality between two instances with same values
        assertTrue(f1.equals(f2));

        // Check with a completely different instance type
        assertFalse(f1.equals(new Object()));
    }

    @Test
    void testEmptyConstructor() {
        // Make sure we can create a Friend object without setting properties
        Friend friend = new Friend();
        assertNotNull(friend);
    }

    @Test
    void testEqualsWithNullFields() {
        Friend f1 = new Friend();
        f1.setId(null);
        f1.setFriendId(2L);

        Friend f2 = new Friend();
        f2.setId(null);
        f2.setFriendId(2L);

        assertEquals(f1, f2); // 相等字段为null也认为相等

        Friend f3 = new Friend();
        f3.setId(1L);
        f3.setFriendId(null);

        Friend f4 = new Friend();
        f4.setId(1L);
        f4.setFriendId(3L);

        assertNotEquals(f3, f4); // 一个字段为null，一个为非null
    }

    @Test
    void testHashCodeWithNullFields() {
        Friend f1 = new Friend();
        f1.setId(null);
        f1.setFriendId(null);

        Friend f2 = new Friend();
        f2.setId(null);
        f2.setFriendId(null);

        assertEquals(f1.hashCode(), f2.hashCode()); // hashCode一致

        // 与非null字段比较
        Friend f3 = new Friend();
        f3.setId(1L);
        f3.setFriendId(null);

        assertNotEquals(f1.hashCode(), f3.hashCode());
    }

    @Test
    void testEqualsSymmetryAndTransitivity() {
        Friend f1 = new Friend(); f1.setId(1L); f1.setFriendId(2L);
        Friend f2 = new Friend(); f2.setId(1L); f2.setFriendId(2L);
        Friend f3 = new Friend(); f3.setId(1L); f3.setFriendId(2L);

        // 对称性
        assertTrue(f1.equals(f2));
        assertTrue(f2.equals(f1));

        // 传递性
        assertTrue(f1.equals(f2));
        assertTrue(f2.equals(f3));
        assertTrue(f1.equals(f3));
    }

    @Test
    void testToStringWithNullFields() {
        Friend f = new Friend();
        f.setId(null);
        f.setFriendId(null);

        String str = f.toString();
        assertTrue(str.contains("null")); // 应包含 null
    }

    @Test
    void testCanEqualWithNullAndDifferentType() throws Exception {
        Friend f = new Friend();

        Method canEqualMethod = Friend.class.getDeclaredMethod("canEqual", Object.class);
        canEqualMethod.setAccessible(true);
        assertFalse((Boolean) canEqualMethod.invoke(f, "string"));
    }

    @Test
    void testEqualsSelfWithNullFields() {
        Friend f = new Friend();
        f.setId(null);
        f.setFriendId(null);

        assertEquals(f, f); // 自反性必须成立
    }

}

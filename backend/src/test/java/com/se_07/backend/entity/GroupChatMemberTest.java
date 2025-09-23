package com.se_07.backend.chat.Entity;

import com.se_07.backend.entity.GroupChatMember;
import com.se_07.backend.entity.GroupChatMember.GroupChatMemberId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupChatMemberTest {

    // ====================== GroupChatMemberId 测试 ======================

    @Test
    void testIdEquals_SameInstance() {
        GroupChatMemberId id = new GroupChatMemberId();
        id.setGroupId(1L);
        id.setUserId(2L);
        assertEquals(id, id);
    }

    @Test
    void testIdEquals_NullComparison() {
        GroupChatMemberId id = new GroupChatMemberId();
        assertNotEquals(id, null);
    }

    @Test
    void testIdEquals_DifferentClass() {
        GroupChatMemberId id = new GroupChatMemberId();
        assertNotEquals(id, "string");
    }

    @Test
    void testIdEquals_AllFieldsNull() {
        GroupChatMemberId id1 = new GroupChatMemberId();
        GroupChatMemberId id2 = new GroupChatMemberId();
        assertEquals(id1, id2);
    }

    @Test
    void testIdEquals_OneFieldNull() {
        GroupChatMemberId id1 = new GroupChatMemberId();
        id1.setGroupId(1L);

        GroupChatMemberId id2 = new GroupChatMemberId();
        assertNotEquals(id1, id2);
    }

    @Test
    void testIdEquals_DifferentValues() {
        GroupChatMemberId id1 = new GroupChatMemberId();
        id1.setGroupId(1L);
        id1.setUserId(2L);

        GroupChatMemberId id2 = new GroupChatMemberId();
        id2.setGroupId(3L);
        id2.setUserId(4L);

        assertNotEquals(id1, id2);
    }

    @Test
    void testIdHashCode_Consistency() {
        GroupChatMemberId id = new GroupChatMemberId();
        id.setGroupId(1L);
        id.setUserId(2L);

        int hashCode1 = id.hashCode();
        int hashCode2 = id.hashCode();
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    void testIdHashCode_SameValues() {
        GroupChatMemberId id1 = new GroupChatMemberId();
        id1.setGroupId(1L);
        id1.setUserId(2L);

        GroupChatMemberId id2 = new GroupChatMemberId();
        id2.setGroupId(1L);
        id2.setUserId(2L);

        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testIdHashCode_DifferentValues() {
        GroupChatMemberId id1 = new GroupChatMemberId();
        id1.setGroupId(1L);

        GroupChatMemberId id2 = new GroupChatMemberId();
        id2.setUserId(2L);

        assertNotEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testIdHashCode_AllFieldsNull() {
        GroupChatMemberId id1 = new GroupChatMemberId();
        GroupChatMemberId id2 = new GroupChatMemberId();
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testIdToString_WithValues() {
        GroupChatMemberId id = new GroupChatMemberId();
        id.setGroupId(1L);
        id.setUserId(2L);

        String str = id.toString();
        assertTrue(str.contains("groupId=1"));
        assertTrue(str.contains("userId=2"));
    }

    @Test
    void testIdToString_WithNullValues() {
        GroupChatMemberId id = new GroupChatMemberId();
        String str = id.toString();
        assertTrue(str.contains("groupId=null"));
        assertTrue(str.contains("userId=null"));
    }

    @Test
    void testIdCanEqual_Reflection() {
        GroupChatMemberId id = new GroupChatMemberId();
        // 实际测试中可能需要使用反射调用canEqual方法
        // 这里通过equals行为间接测试
        GroupChatMemberId sameId = new GroupChatMemberId();
        assertEquals(id, sameId);

        assertNotEquals(id, new Object());
    }

    @Test
    void testIdEquals_Subclass() {
        GroupChatMemberId id = new GroupChatMemberId();
        id.setGroupId(1L);
        id.setUserId(2L);

        class SubId extends GroupChatMemberId {}
        SubId subId = new SubId();
        subId.setGroupId(1L);
        subId.setUserId(2L);

        assertEquals(id, subId);
    }

    // ====================== GroupChatMember 测试 ======================

    @Test
    void testMemberEquals_SameInstance() {
        GroupChatMember member = new GroupChatMember();
        member.setGroupId(1L);
        member.setUserId(2L);
        assertEquals(member, member);
    }

    @Test
    void testMemberEquals_NullComparison() {
        GroupChatMember member = new GroupChatMember();
        assertNotEquals(member, null);
    }

    @Test
    void testMemberEquals_DifferentClass() {
        GroupChatMember member = new GroupChatMember();
        assertNotEquals(member, "string");
    }

    @Test
    void testMemberEquals_AllFieldsNull() {
        GroupChatMember m1 = new GroupChatMember();
        GroupChatMember m2 = new GroupChatMember();
        assertEquals(m1, m2);
    }

    @Test
    void testMemberEquals_OneFieldNull() {
        GroupChatMember m1 = new GroupChatMember();
        m1.setGroupId(1L);

        GroupChatMember m2 = new GroupChatMember();
        assertNotEquals(m1, m2);
    }

    @Test
    void testMemberEquals_DifferentValues() {
        GroupChatMember m1 = new GroupChatMember();
        m1.setGroupId(1L);
        m1.setUserId(2L);

        GroupChatMember m2 = new GroupChatMember();
        m2.setGroupId(3L);
        m2.setUserId(4L);

        assertNotEquals(m1, m2);
    }

    @Test
    void testMemberHashCode_Consistency() {
        GroupChatMember member = new GroupChatMember();
        member.setGroupId(1L);
        member.setUserId(2L);

        int hashCode1 = member.hashCode();
        int hashCode2 = member.hashCode();
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    void testMemberHashCode_SameValues() {
        GroupChatMember m1 = new GroupChatMember();
        m1.setGroupId(1L);
        m1.setUserId(2L);

        GroupChatMember m2 = new GroupChatMember();
        m2.setGroupId(1L);
        m2.setUserId(2L);

        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void testMemberHashCode_DifferentValues() {
        GroupChatMember m1 = new GroupChatMember();
        m1.setGroupId(1L);

        GroupChatMember m2 = new GroupChatMember();
        m2.setUserId(2L);

        assertNotEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void testMemberHashCode_AllFieldsNull() {
        GroupChatMember m1 = new GroupChatMember();
        GroupChatMember m2 = new GroupChatMember();
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void testMemberToString_WithValues() {
        GroupChatMember member = new GroupChatMember();
        member.setGroupId(1L);
        member.setUserId(2L);

        String str = member.toString();
        assertTrue(str.contains("groupId=1"));
        assertTrue(str.contains("userId=2"));
    }

    @Test
    void testMemberToString_WithNullValues() {
        GroupChatMember member = new GroupChatMember();
        String str = member.toString();
        assertTrue(str.contains("groupId=null"));
        assertTrue(str.contains("userId=null"));
    }

    @Test
    void testMemberGetterSetter() {
        GroupChatMember member = new GroupChatMember();

        // 测试正常值
        member.setGroupId(1L);
        member.setUserId(2L);
        assertEquals(1L, member.getGroupId());
        assertEquals(2L, member.getUserId());

        // 测试边界值
        member.setGroupId(0L);
        member.setUserId(Long.MAX_VALUE);
        assertEquals(0L, member.getGroupId());
        assertEquals(Long.MAX_VALUE, member.getUserId());

        // 测试null值
        member.setGroupId(null);
        member.setUserId(null);
        assertNull(member.getGroupId());
        assertNull(member.getUserId());
    }

    @Test
    void testMemberEquals_Subclass() {
        GroupChatMember member = new GroupChatMember();
        member.setGroupId(1L);
        member.setUserId(2L);

        class SubMember extends GroupChatMember {}
        SubMember subMember = new SubMember();
        subMember.setGroupId(1L);
        subMember.setUserId(2L);

        assertEquals(member, subMember);
    }

    @Test
    void testMemberEquals_WithMixedNulls() {
        GroupChatMember m1 = new GroupChatMember();
        m1.setGroupId(1L);
        m1.setUserId(null);

        GroupChatMember m2 = new GroupChatMember();
        m2.setGroupId(1L);
        m2.setUserId(null);

        GroupChatMember m3 = new GroupChatMember();
        m3.setGroupId(1L);
        m3.setUserId(2L);

        assertEquals(m1, m2);
        assertNotEquals(m1, m3);
    }

    @Test
    void testMemberEquals_Transitivity() {
        GroupChatMember m1 = new GroupChatMember();
        m1.setGroupId(1L);
        m1.setUserId(2L);

        GroupChatMember m2 = new GroupChatMember();
        m2.setGroupId(1L);
        m2.setUserId(2L);

        GroupChatMember m3 = new GroupChatMember();
        m3.setGroupId(1L);
        m3.setUserId(2L);

        assertEquals(m1, m2);
        assertEquals(m2, m3);
        assertEquals(m1, m3);
    }

    @Test
    void testMemberEquals_Symmetry() {
        GroupChatMember m1 = new GroupChatMember();
        m1.setGroupId(1L);
        m1.setUserId(2L);

        GroupChatMember m2 = new GroupChatMember();
        m2.setGroupId(1L);
        m2.setUserId(2L);

        assertEquals(m1, m2);
        assertEquals(m2, m1);
    }

    @Test
    void testMemberHashCode_NullFields() {
        GroupChatMember m1 = new GroupChatMember();
        m1.setGroupId(null);
        m1.setUserId(2L);

        GroupChatMember m2 = new GroupChatMember();
        m2.setGroupId(null);
        m2.setUserId(2L);

        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void testMemberHashCode_AfterModification() {
        GroupChatMember member = new GroupChatMember();
        member.setGroupId(1L);
        member.setUserId(2L);
        int initialHashCode = member.hashCode();

        member.setGroupId(3L);
        assertNotEquals(initialHashCode, member.hashCode());
    }

    @Test
    void testMemberToString_AfterModification() {
        GroupChatMember member = new GroupChatMember();
        member.setGroupId(1L);
        String initialString = member.toString();

        member.setUserId(2L);
        String modifiedString = member.toString();

        assertNotEquals(initialString, modifiedString);
        assertTrue(modifiedString.contains("userId=2"));
    }
}
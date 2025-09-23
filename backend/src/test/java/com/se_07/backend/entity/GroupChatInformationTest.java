package com.se_07.backend.chat.Entity;

import com.se_07.backend.entity.GroupChatInformation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupChatInformationTest {

    @Test
    void testGetterSetter() {
        GroupChatInformation group = new GroupChatInformation();

        // 测试正常值
        group.setGroupId(1L);
        group.setGroupName("Developers");

        assertEquals(1L, group.getGroupId());
        assertEquals("Developers", group.getGroupName());

        // 测试 null 值
        group.setGroupId(null);
        group.setGroupName(null);

        assertNull(group.getGroupId());
        assertNull(group.getGroupName());
    }

    @Test
    void testEquals() {
        GroupChatInformation group1 = createGroup(1L, "Developers");
        GroupChatInformation group2 = createGroup(1L, "Developers");
        GroupChatInformation group3 = createGroup(2L, "Designers");

        // 相等性测试
        assertEquals(group1, group2, "相同ID和名称的对象应相等");

        // 不等性测试
        assertNotEquals(group1, group3, "不同ID的对象应不相等");

        // ID不同但名称相同
        GroupChatInformation group4 = createGroup(2L, "Developers");
        assertNotEquals(group1, group4, "不同ID但相同名称的对象应不相等");

        // 名称不同但ID相同
        GroupChatInformation group5 = createGroup(1L, "Designers");
        assertNotEquals(group1, group5, "相同ID但不同名称的对象应不相等");

        // null 处理
        assertNotEquals(group1, null, "与null比较应不相等");

        // 不同类型
        assertNotEquals(group1, "string", "与不同类型比较应不相等");

        // 自反性
        assertEquals(group1, group1, "对象应与自身相等");

        // 对称性
        assertEquals(group2, group1, "相等关系应是对称的");

        // null ID 测试
        GroupChatInformation nullId1 = createGroup(null, "Test");
        GroupChatInformation nullId2 = createGroup(null, "Test");
        GroupChatInformation notNullId = createGroup(3L, "Test");

        assertEquals(nullId1, nullId2, "两者ID均为null且名称相同应相等");
        assertNotEquals(nullId1, notNullId, "ID null vs 非null应不相等");

        // null 名称测试
        GroupChatInformation nullName1 = createGroup(4L, null);
        GroupChatInformation nullName2 = createGroup(4L, null);
        GroupChatInformation notNullName = createGroup(4L, "Exists");

        assertEquals(nullName1, nullName2, "两者名称均为null且ID相同应相等");
        assertNotEquals(nullName1, notNullName, "名称 null vs 非null应不相等");

        // 所有字段为null
        GroupChatInformation allNull1 = new GroupChatInformation();
        GroupChatInformation allNull2 = new GroupChatInformation();

        assertEquals(allNull1, allNull2, "所有字段为null的对象应相等");
        assertNotEquals(allNull1, group1, "所有字段为null vs 非null应不相等");
    }

    @Test
    void testHashCode() {
        GroupChatInformation group1 = createGroup(1L, "Developers");
        GroupChatInformation group2 = createGroup(1L, "Developers");
        GroupChatInformation group3 = createGroup(2L, "Designers");

        // 相同对象应有相同hashCode
        assertEquals(group1.hashCode(), group2.hashCode());

        // 不同对象通常应有不同hashCode（非强制但常见）
        assertNotEquals(group1.hashCode(), group3.hashCode());

        // null ID 测试
        GroupChatInformation nullId1 = createGroup(null, "Test");
        GroupChatInformation nullId2 = createGroup(null, "Test");
        GroupChatInformation notNullId = createGroup(3L, "Test");

        assertEquals(nullId1.hashCode(), nullId2.hashCode());
        assertNotEquals(nullId1.hashCode(), notNullId.hashCode());

        // null 名称测试
        GroupChatInformation nullName1 = createGroup(4L, null);
        GroupChatInformation nullName2 = createGroup(4L, null);
        GroupChatInformation notNullName = createGroup(4L, "Exists");

        assertEquals(nullName1.hashCode(), nullName2.hashCode());
        assertNotEquals(nullName1.hashCode(), notNullName.hashCode());

        // 所有字段为null
        GroupChatInformation allNull1 = new GroupChatInformation();
        GroupChatInformation allNull2 = new GroupChatInformation();

        assertEquals(allNull1.hashCode(), allNull2.hashCode());
    }

    @Test
    void testToString() {
        GroupChatInformation group = createGroup(1L, "Developers");
        String str = group.toString();

        // 验证包含关键字段
        assertTrue(str.contains("groupId=1"), "应包含groupId");
        assertTrue(str.contains("groupName=Developers"), "应包含groupName");

        // null ID 测试
        GroupChatInformation nullId = createGroup(null, "Test");
        String nullIdStr = nullId.toString();
        assertTrue(nullIdStr.contains("groupId=null"), "应处理null groupId");
        assertTrue(nullIdStr.contains("groupName=Test"), "应包含非null groupName");

        // null 名称测试
        GroupChatInformation nullName = createGroup(2L, null);
        String nullNameStr = nullName.toString();
        assertTrue(nullNameStr.contains("groupId=2"), "应包含groupId");
        assertTrue(nullNameStr.contains("groupName=null"), "应处理null groupName");

        // 所有字段为null
        GroupChatInformation allNull = new GroupChatInformation();
        String allNullStr = allNull.toString();
        assertTrue(allNullStr.contains("groupId=null"), "应处理null groupId");
        assertTrue(allNullStr.contains("groupName=null"), "应处理null groupName");
    }

    // 辅助方法创建群组对象
    private GroupChatInformation createGroup(Long id, String name) {
        GroupChatInformation group = new GroupChatInformation();
        group.setGroupId(id);
        group.setGroupName(name);
        return group;
    }
}
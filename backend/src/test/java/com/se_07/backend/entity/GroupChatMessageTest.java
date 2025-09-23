package com.se_07.backend.chat.Entity;

import com.se_07.backend.entity.GroupChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class GroupChatMessageTest {

    private GroupChatMessage message1;
    private GroupChatMessage message2;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();

        message1 = new GroupChatMessage();
        message1.setMessageId(1L);
        message1.setGroupId(100L);
        message1.setUserId(200L);
        message1.setContent("Hello World");
        message1.setMessageTime(testTime);

        message2 = new GroupChatMessage();
        message2.setMessageId(1L);
        message2.setGroupId(100L);
        message2.setUserId(200L);
        message2.setContent("Hello World");
        message2.setMessageTime(testTime);
    }

    // 测试 getter 方法
    @Test
    void getMessageId() {
        assertEquals(1L, message1.getMessageId());
    }

    @Test
    void getGroupId() {
        assertEquals(100L, message1.getGroupId());
    }

    @Test
    void getUserId() {
        assertEquals(200L, message1.getUserId());
    }

    @Test
    void getContent() {
        assertEquals("Hello World", message1.getContent());
    }

    @Test
    void getMessageTime() {
        assertEquals(testTime, message1.getMessageTime());
    }

    // 测试 setter 方法
    @Test
    void setMessageId() {
        message1.setMessageId(999L);
        assertEquals(999L, message1.getMessageId());

        // 测试 null 值
        message1.setMessageId(null);
        assertNull(message1.getMessageId());
    }

    @Test
    void setGroupId() {
        message1.setGroupId(555L);
        assertEquals(555L, message1.getGroupId());

        // 测试 null 值
        message1.setGroupId(null);
        assertNull(message1.getGroupId());
    }

    @Test
    void setUserId() {
        message1.setUserId(333L);
        assertEquals(333L, message1.getUserId());

        // 测试 null 值
        message1.setUserId(null);
        assertNull(message1.getUserId());
    }

    @Test
    void setContent() {
        message1.setContent("New Content");
        assertEquals("New Content", message1.getContent());

        // 测试空字符串
        message1.setContent("");
        assertEquals("", message1.getContent());

        // 测试 null 值
        message1.setContent(null);
        assertNull(message1.getContent());

        // 测试长内容（边界值）
        String longContent = new String(new char[500]).replace('\0', 'A');
        message1.setContent(longContent);
        assertEquals(500, message1.getContent().length());
    }

    @Test
    void setMessageTime() {
        LocalDateTime newTime = LocalDateTime.now().plusDays(1);
        message1.setMessageTime(newTime);
        assertEquals(newTime, message1.getMessageTime());

        // 测试 null 值
        message1.setMessageTime(null);
        assertNull(message1.getMessageTime());
    }

    // 测试 equals 方法的各种场景
    @Test
    void testEquals() {
        // 相同对象
        assertEquals(message1, message1);

        // 相等对象
        assertEquals(message1, message2);

        // 与 null 比较
        assertNotEquals(null, message1);

        // 与不同类型比较
        assertNotEquals(message1, "string");

        // ID 不同
        GroupChatMessage differentId = new GroupChatMessage();
        differentId.setMessageId(2L);
        differentId.setGroupId(100L);
        differentId.setUserId(200L);
        differentId.setContent("Hello World");
        differentId.setMessageTime(testTime);
        assertNotEquals(message1, differentId);

        // Group ID 不同
        GroupChatMessage differentGroupId = new GroupChatMessage();
        differentGroupId.setMessageId(1L);
        differentGroupId.setGroupId(101L);
        differentGroupId.setUserId(200L);
        differentGroupId.setContent("Hello World");
        differentGroupId.setMessageTime(testTime);
        assertNotEquals(message1, differentGroupId);

        // User ID 不同
        GroupChatMessage differentUserId = new GroupChatMessage();
        differentUserId.setMessageId(1L);
        differentUserId.setGroupId(100L);
        differentUserId.setUserId(201L);
        differentUserId.setContent("Hello World");
        differentUserId.setMessageTime(testTime);
        assertNotEquals(message1, differentUserId);

        // Content 不同
        GroupChatMessage differentContent = new GroupChatMessage();
        differentContent.setMessageId(1L);
        differentContent.setGroupId(100L);
        differentContent.setUserId(200L);
        differentContent.setContent("Different Content");
        differentContent.setMessageTime(testTime);
        assertNotEquals(message1, differentContent);

        // Message Time 不同
        GroupChatMessage differentTime = new GroupChatMessage();
        differentTime.setMessageId(1L);
        differentTime.setGroupId(100L);
        differentTime.setUserId(200L);
        differentTime.setContent("Hello World");
        differentTime.setMessageTime(testTime.plusSeconds(1));
        assertNotEquals(message1, differentTime);

        // 部分字段为 null
        GroupChatMessage partialNull1 = new GroupChatMessage();
        partialNull1.setMessageId(1L);
        partialNull1.setGroupId(null);
        partialNull1.setUserId(200L);
        partialNull1.setContent("Hello World");
        partialNull1.setMessageTime(testTime);
        assertNotEquals(message1, partialNull1);

        // 所有字段为 null
        GroupChatMessage allNull1 = new GroupChatMessage();
        GroupChatMessage allNull2 = new GroupChatMessage();
        assertEquals(allNull1, allNull2);
    }

    // 测试 hashCode 方法
    @Test
    void testHashCode() {
        // 相等对象应有相同哈希码
        assertEquals(message1.hashCode(), message2.hashCode());

        // 不同对象应有不同哈希码（尽量）
        GroupChatMessage different = new GroupChatMessage();
        different.setMessageId(2L);
        different.setGroupId(100L);
        different.setUserId(200L);
        different.setContent("Hello World");
        different.setMessageTime(testTime);
        assertNotEquals(message1.hashCode(), different.hashCode());

        // 所有字段为 null 的对象应有相同哈希码
        GroupChatMessage nullMessage1 = new GroupChatMessage();
        GroupChatMessage nullMessage2 = new GroupChatMessage();
        assertEquals(nullMessage1.hashCode(), nullMessage2.hashCode());

        // 部分字段为 null 的哈希码一致性
        GroupChatMessage partialNull1 = new GroupChatMessage();
        partialNull1.setMessageId(1L);
        partialNull1.setGroupId(null);
        partialNull1.setUserId(200L);
        partialNull1.setContent("Hello World");
        partialNull1.setMessageTime(testTime);

        GroupChatMessage partialNull2 = new GroupChatMessage();
        partialNull2.setMessageId(1L);
        partialNull2.setGroupId(null);
        partialNull2.setUserId(200L);
        partialNull2.setContent("Hello World");
        partialNull2.setMessageTime(testTime);
        assertEquals(partialNull1.hashCode(), partialNull2.hashCode());
    }

    // 测试 toString 方法
    @Test
    void testToString() {
        String str = message1.toString();

        // 检查所有字段是否在字符串中
        assertTrue(str.contains("messageId=1"));
        assertTrue(str.contains("groupId=100"));
        assertTrue(str.contains("userId=200"));
        assertTrue(str.contains("content=Hello World"));
        assertTrue(str.contains("messageTime=" + testTime));

        // 测试空值情况
        GroupChatMessage nullMessage = new GroupChatMessage();
        String nullStr = nullMessage.toString();
        assertTrue(nullStr.contains("messageId=null"));
        assertTrue(nullStr.contains("groupId=null"));
        assertTrue(nullStr.contains("userId=null"));
        assertTrue(nullStr.contains("content=null"));
        assertTrue(nullStr.contains("messageTime=null"));

        // 测试长内容截断
        String longContent = new String(new char[500]).replace('\0', 'A');
        message1.setContent(longContent);
        str = message1.toString();
        assertTrue(str.contains("content=" + longContent));
    }

    // 测试 canEqual 方法（Lombok 生成）
//    @Test
//    void canEqual() {
//        // 同类型对象应可以相等比较
//        assertTrue(message1.canEqual(message2));
//
//        // 不同类型对象不应相等
//        assertFalse(message1.canEqual("string"));
//
//        // 子类对象（假设有子类）
//        class SubMessage extends GroupChatMessage {}
//        assertFalse(message1.canEqual(new SubMessage()));
//    }

    // 测试时间精度问题
    @Test
    void testTimePrecision() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime truncated = now.truncatedTo(ChronoUnit.MILLIS);

        message1.setMessageTime(now);
        message2.setMessageTime(truncated);

        // 如果时间精度不同但实际时间相同，应视为相等
        if (now.equals(truncated)) {
            assertEquals(message1, message2);
        } else {
            assertNotEquals(message1, message2);
        }
    }

    // 测试内容边界值
    @Test
    void testContentBoundary() {
        // 测试最大长度内容
        String maxContent = new String(new char[500]).replace('\0', 'A');
        message1.setContent(maxContent);
        assertEquals(500, message1.getContent().length());

        // 测试超长内容（数据库层会处理，这里测试setter）
        String overMaxContent = new String(new char[501]).replace('\0', 'B');
        message1.setContent(overMaxContent);
        assertEquals(501, message1.getContent().length());
    }
}
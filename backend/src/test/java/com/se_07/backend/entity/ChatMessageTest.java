package com.se_07.backend.chat.Entity;

import com.se_07.backend.entity.ChatMessage;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class ChatMessageTest {

    @Test
    void testGetterSetter() {
        ChatMessage msg = new ChatMessage();

        // 测试所有字段
        msg.setMessageId(1L);
        msg.setFromId(2L);
        msg.setToId(3L);
        LocalDateTime now = LocalDateTime.now();
        msg.setMessageTime(now);
        msg.setContent("hello");

        assertEquals(1L, msg.getMessageId());
        assertEquals(2L, msg.getFromId());
        assertEquals(3L, msg.getToId());
        assertEquals(now, msg.getMessageTime());
        assertEquals("hello", msg.getContent());

        // 测试 null 值
        msg.setMessageId(null);
        msg.setFromId(null);
        msg.setToId(null);
        msg.setMessageTime(null);
        msg.setContent(null);

        assertNull(msg.getMessageId());
        assertNull(msg.getFromId());
        assertNull(msg.getToId());
        assertNull(msg.getMessageTime());
        assertNull(msg.getContent());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime time = LocalDateTime.of(2023, 1, 1, 12, 0);

        // 基础相等对象
        ChatMessage base = createMessage(1L, 2L, 3L, time, "hi");

        // 1. 完全相同的对象
        ChatMessage same = createMessage(1L, 2L, 3L, time, "hi");
        assertEquals(base, same, "相同对象应相等");
        assertEquals(base.hashCode(), same.hashCode(), "相同对象应有相同哈希值");

        // 2. 不同类型对象
        assertNotEquals(base, "string", "不同类型对象应不相等");
        assertNotEquals(base, null, "与null比较应不相等");

        // 3. 自反性
        assertEquals(base, base, "对象应与自身相等");

        // 4. 对称性
        assertEquals(same, base, "相等关系应是对称的");

        // 5. 传递性
        ChatMessage same2 = createMessage(1L, 2L, 3L, time, "hi");
        assertEquals(same, same2, "相等关系应是传递的");
        assertEquals(base, same2, "相等关系应是传递的");

        // 6. 字段差异测试
        // 不同ID
        assertNotEquals(base, createMessage(99L, 2L, 3L, time, "hi"), "不同ID应不相等");
        // 不同发送者
        assertNotEquals(base, createMessage(1L, 99L, 3L, time, "hi"), "不同发送者应不相等");
        // 不同接收者
        assertNotEquals(base, createMessage(1L, 2L, 99L, time, "hi"), "不同接收者应不相等");
        // 不同时间
        assertNotEquals(base, createMessage(1L, 2L, 3L, time.plusHours(1), "hi"), "不同时间应不相等");
        // 不同内容
        assertNotEquals(base, createMessage(1L, 2L, 3L, time, "different"), "不同内容应不相等");

        // 7. null 内容处理
        ChatMessage nullContent = createMessage(1L, 2L, 3L, time, null);
        ChatMessage nullContent2 = createMessage(1L, 2L, 3L, time, null);
        ChatMessage notNullContent = createMessage(1L, 2L, 3L, time, "hi");

        assertEquals(nullContent, nullContent2, "两者内容均为null应相等");
        assertNotEquals(base, nullContent, "内容null vs 非null应不相等");
        assertNotEquals(nullContent, notNullContent, "内容null vs 非null应不相等");
        assertEquals(nullContent.hashCode(), nullContent2.hashCode(), "相同null内容应有相同哈希值");

        // 8. 部分字段为null
        // ID为null
        ChatMessage nullId1 = createMessage(null, 2L, 3L, time, "hi");
        ChatMessage nullId2 = createMessage(null, 2L, 3L, time, "hi");
        ChatMessage notNullId = createMessage(1L, 2L, 3L, time, "hi");

        assertEquals(nullId1, nullId2, "两者ID均为null应相等");
        assertNotEquals(nullId1, notNullId, "ID null vs 非null应不相等");
        assertEquals(nullId1.hashCode(), nullId2.hashCode(), "相同null ID应有相同哈希值");

        // 发送者为null
        ChatMessage nullFrom1 = createMessage(1L, null, 3L, time, "hi");
        ChatMessage nullFrom2 = createMessage(1L, null, 3L, time, "hi");
        assertNotEquals(nullFrom1, base, "发送者null vs 非null应不相等");
        assertEquals(nullFrom1, nullFrom2, "两者发送者均为null应相等");

        // 接收者为null
        ChatMessage nullTo1 = createMessage(1L, 2L, null, time, "hi");
        ChatMessage nullTo2 = createMessage(1L, 2L, null, time, "hi");
        assertNotEquals(nullTo1, base, "接收者null vs 非null应不相等");
        assertEquals(nullTo1, nullTo2, "两者接收者均为null应相等");

        // 时间为null
        ChatMessage nullTime1 = createMessage(1L, 2L, 3L, null, "hi");
        ChatMessage nullTime2 = createMessage(1L, 2L, 3L, null, "hi");
        assertNotEquals(nullTime1, base, "时间null vs 非null应不相等");
        assertEquals(nullTime1, nullTime2, "两者时间均为null应相等");

        // 9. 所有字段都为null
        ChatMessage allNull1 = new ChatMessage();
        ChatMessage allNull2 = new ChatMessage();
        assertEquals(allNull1, allNull2, "所有字段为null应相等");
        assertEquals(allNull1.hashCode(), allNull2.hashCode(), "所有字段为null应有相同哈希值");
        assertNotEquals(allNull1, base, "所有字段为null vs 非null应不相等");
    }

    @Test
    void testToString() {
        // 1. 所有字段都有值
        ChatMessage fullMsg = createMessage(1L, 2L, 3L, LocalDateTime.of(2023, 1, 1, 12, 0), "hi");
        String fullStr = fullMsg.toString();
        assertTrue(fullStr.contains("messageId=1"), "应包含messageId");
        assertTrue(fullStr.contains("fromId=2"), "应包含fromId");
        assertTrue(fullStr.contains("toId=3"), "应包含toId");
        assertTrue(fullStr.contains("messageTime=2023-01-01T12:00"), "应包含messageTime");
        assertTrue(fullStr.contains("content=hi"), "应包含内容");

        // 2. 内容为null
        ChatMessage nullContent = createMessage(4L, 5L, 6L, LocalDateTime.now(), null);
        String nullContentStr = nullContent.toString();
        assertTrue(nullContentStr.contains("content=null"), "应显示null内容");

        // 3. ID为null
        ChatMessage nullId = createMessage(null, 7L, 8L, LocalDateTime.now(), "test");
        String nullIdStr = nullId.toString();
        assertTrue(nullIdStr.contains("messageId=null"), "应显示null ID");

        // 4. 发送者为null
        ChatMessage nullFrom = createMessage(9L, null, 10L, LocalDateTime.now(), "test");
        String nullFromStr = nullFrom.toString();
        assertTrue(nullFromStr.contains("fromId=null"), "应显示null发送者");

        // 5. 接收者为null
        ChatMessage nullTo = createMessage(11L, 12L, null, LocalDateTime.now(), "test");
        String nullToStr = nullTo.toString();
        assertTrue(nullToStr.contains("toId=null"), "应显示null接收者");

        // 6. 时间为null
        ChatMessage nullTime = createMessage(13L, 14L, 15L, null, "test");
        String nullTimeStr = nullTime.toString();
        assertTrue(nullTimeStr.contains("messageTime=null"), "应显示null时间");

        // 7. 所有字段为null
        ChatMessage allNull = new ChatMessage();
        String allNullStr = allNull.toString();
        assertTrue(allNullStr.contains("messageId=null"), "应显示null ID");
        assertTrue(allNullStr.contains("fromId=null"), "应显示null发送者");
        assertTrue(allNullStr.contains("toId=null"), "应显示null接收者");
        assertTrue(allNullStr.contains("messageTime=null"), "应显示null时间");
        assertTrue(allNullStr.contains("content=null"), "应显示null内容");
    }

    // 辅助方法创建消息对象
    private ChatMessage createMessage(Long id, Long from, Long to, LocalDateTime time, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(id);
        msg.setFromId(from);
        msg.setToId(to);
        msg.setMessageTime(time);
        msg.setContent(content);
        return msg;
    }
}
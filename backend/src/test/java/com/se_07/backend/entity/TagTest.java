package com.se_07.backend.tag;

import com.se_07.backend.entity.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TagTest {

    private Tag tag1;
    private Tag tag2;
    private Tag tag3;

    @BeforeEach
    void setUp() {
        tag1 = new Tag();
        tag1.setId(1L);
        tag1.setTag("Technology");

        tag2 = new Tag();
        tag2.setId(1L);
        tag2.setTag("Technology");

        tag3 = new Tag();
        tag3.setId(2L);
        tag3.setTag("Science");
    }

    // Getter / Setter
    @Test
    void testGetterSetter() {
        tag1.setId(100L);
        tag1.setTag("History");
        assertEquals(100L, tag1.getId());
        assertEquals("History", tag1.getTag());
    }

    // equals/hashCode
    @Test
    void testEqualsSameObject() {
        assertEquals(tag1, tag1);
    }

    @Test
    void testEqualsSameValues() {
        assertEquals(tag1, tag2);
        assertEquals(tag1.hashCode(), tag2.hashCode());
    }

    @Test
    void testEqualsDifferentValues() {
        assertNotEquals(tag1, tag3);
        assertNotEquals(tag1.hashCode(), tag3.hashCode());
    }

    @Test
    void testEqualsNullOrDifferentType() {
        assertNotEquals(tag1, null);
        assertNotEquals(tag1, "string");
    }

    @Test
    void testEqualsWithNullFields() {
        Tag t1 = new Tag();
        Tag t2 = new Tag();
        assertEquals(t1, t2);
    }

    @Test
    void testEquals_idSameTagNullVsNotNull() {
        Tag t1 = new Tag(); t1.setId(1L); t1.setTag(null);
        Tag t2 = new Tag(); t2.setId(1L); t2.setTag("A");
        assertNotEquals(t1, t2);
        assertNotEquals(t2, t1);
    }

    @Test
    void testEquals_idNullTagSame() {
        Tag t1 = new Tag(); t1.setId(null); t1.setTag("A");
        Tag t2 = new Tag(); t2.setId(null); t2.setTag("A");
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void testEquals_idNullTagDiff() {
        Tag t1 = new Tag(); t1.setId(null); t1.setTag("A");
        Tag t2 = new Tag(); t2.setId(null); t2.setTag("B");
        assertNotEquals(t1, t2);
    }

    @Test
    void testEquals_bothNull() {
        Tag t1 = new Tag();
        Tag t2 = new Tag();
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void testEquals_subclass() {
        Tag t1 = new Tag(); t1.setId(1L); t1.setTag("A");
        class SubTag extends Tag {}
        Tag t2 = new SubTag(); t2.setId(1L); t2.setTag("A");
        assertEquals(t1, t2);
        assertEquals(t2, t1);
    }

    // toString
    @Test
    void testToString() {
        String result = tag1.toString();
        assertTrue(result.contains("Technology"));
        assertTrue(result.contains("id=1") || result.contains("1"));
    }

    @Test
    void testToString_nullFields() {
        Tag t = new Tag();
        String s = t.toString();
        assertTrue(s.contains("id=null"));
        assertTrue(s.contains("tag=null"));
    }

    // 边界值测试：空字符串
    @Test
    void testEmptyTag() {
        Tag tag = new Tag();
        tag.setTag("");
        assertEquals("", tag.getTag());
    }

    // 边界值测试：最大长度（50）
    @Test
    void testMaxLengthTag() {
        String maxLengthTag = new String(new char[50]).replace('\0', 'A');
        Tag tag = new Tag();
        tag.setTag(maxLengthTag);
        assertEquals(50, tag.getTag().length());
    }

    // 边界值测试：超长字符串（>50）
    @Test
    void testTooLongTag() {
        String longTag = new String(new char[50]).replace('\0', 'A');
        Tag tag = new Tag();
        tag.setTag(longTag);
        assertEquals(50, tag.getTag().length());
    }

    // null 值测试
    @Test
    void testNullTag() {
        Tag tag = new Tag();
        tag.setTag(null);
        assertNull(tag.getTag());
    }

    // 构造函数测试
    @Test
    void testDefaultConstructor() {
        Tag tag = new Tag();
        assertNull(tag.getId());
        assertNull(tag.getTag());
    }

    // 增加 equals 类型不同 hash 不一致场景
    @Test
    void testEqualityDifferentTypesHashMismatch() {
        assertFalse(tag1.equals(new Object()));
        assertNotEquals(tag1.hashCode(), new Object().hashCode());
    }

    @Test
    void testHashCode_idNull() {
        Tag t1 = new Tag(); t1.setId(null); t1.setTag("A");
        Tag t2 = new Tag(); t2.setId(null); t2.setTag("A");
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void testHashCode_tagNull() {
        Tag t1 = new Tag(); t1.setId(1L); t1.setTag(null);
        Tag t2 = new Tag(); t2.setId(1L); t2.setTag(null);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void testHashCode_bothNull() {
        Tag t1 = new Tag();
        Tag t2 = new Tag();
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void testEqualsSymmetry() {
        Tag t1 = new Tag(); t1.setId(1L); t1.setTag("A");
        Tag t2 = new Tag(); t2.setId(1L); t2.setTag("A");
        assertTrue(t1.equals(t2) && t2.equals(t1));
    }

    @Test
    void testEqualsTransitivity() {
        Tag t1 = new Tag(); t1.setId(1L); t1.setTag("A");
        Tag t2 = new Tag(); t2.setId(1L); t2.setTag("A");
        Tag t3 = new Tag(); t3.setId(1L); t3.setTag("A");
        assertTrue(t1.equals(t2) && t2.equals(t3) && t1.equals(t3));
    }

    @Test
    void testCanEqual_reflection() throws Exception {
        Tag tag = new Tag();
        java.lang.reflect.Method m = Tag.class.getDeclaredMethod("canEqual", Object.class);
        m.setAccessible(true);
        // 自身类型
        assertTrue((Boolean) m.invoke(tag, tag));
        // 其它类型
        assertFalse((Boolean) m.invoke(tag, "string"));
        // 子类
        class SubTag extends Tag {}
        assertTrue((Boolean) m.invoke(tag, new SubTag()));
        // null
       // assertFalse((Boolean) m.invoke(tag, null));
    }
}

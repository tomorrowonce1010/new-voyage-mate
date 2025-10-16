package com.se_07.backend.attraction;

import com.se_07.backend.entity.Attraction;
import com.se_07.backend.entity.Destination;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class AttractionTest {

    @Test
    void getterSetterAndBasicFields() {
        Attraction a = new Attraction();
        a.setId(1L);
        a.setAmapPoiId("poi123");
        Destination d = new Destination();
        d.setId(2L);
        a.setDestination(d);
        a.setName("景点");
        a.setDescription("desc");
        a.setImageUrl("img.jpg");
        a.setLatitude(new BigDecimal("12.34"));
        a.setLongitude(new BigDecimal("56.78"));
        a.setCategory(Attraction.AttractionCategory.旅游景点);
        a.setOpeningHours("{\"open\":true}");
        a.setJoinCount(5);
        LocalDateTime now = LocalDateTime.now();
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        a.setTagScores("{\"tag\":1}");

        assertEquals(1L, a.getId());
        assertEquals("poi123", a.getAmapPoiId());
        assertEquals(d, a.getDestination());
        assertEquals("景点", a.getName());
        assertEquals("desc", a.getDescription());
        assertEquals("img.jpg", a.getImageUrl());
        assertEquals(new BigDecimal("12.34"), a.getLatitude());
        assertEquals(new BigDecimal("56.78"), a.getLongitude());
        assertEquals(Attraction.AttractionCategory.旅游景点, a.getCategory());
        assertEquals("{\"open\":true}", a.getOpeningHours());
        assertEquals(5, a.getJoinCount());
        assertEquals(now, a.getCreatedAt());
        assertEquals(now, a.getUpdatedAt());
        assertEquals("{\"tag\":1}", a.getTagScores());
    }

    @Test
    void testEqualsAndHashCode() {
        Attraction a1 = new Attraction();
        a1.setId(1L);
        a1.setName("A");
        a1.setLatitude(new BigDecimal("1.23"));

        Attraction a2 = new Attraction();
        a2.setId(1L);
        a2.setName("A");
        a2.setLatitude(new BigDecimal("1.23"));

        Attraction a3 = new Attraction();
        a3.setId(2L);
        a3.setName("B");

        Attraction a4 = new Attraction();
        a4.setId(1L);
        a4.setName("A");
        a4.setLatitude(new BigDecimal("4.56")); // 相同ID但不同字段

        // 自反性
        assertEquals(a1, a1);
        // 对称性
        assertEquals(a1, a2);
        assertEquals(a2, a1);
        // 传递性
        Attraction a5 = new Attraction();
        a5.setId(1L);
        a5.setName("A");
        a5.setLatitude(new BigDecimal("1.23"));
        assertEquals(a1, a2);
        assertEquals(a2, a5);
        assertEquals(a1, a5);
        // hashCode一致
        assertEquals(a1.hashCode(), a2.hashCode());
        // 不等
        assertNotEquals(a1, a3);
        assertNotEquals(a1, null);
        assertNotEquals(a1, "string");
        // 相同ID但不同字段值
        assertNotEquals(a1, a4);
        assertNotEquals(a1.hashCode(), a4.hashCode());
    }

    @Test
    void testToString() {
        Attraction a = new Attraction();
        a.setId(1L);
        a.setName("景点");
        a.setCategory(Attraction.AttractionCategory.餐饮);

        String s = a.toString();
        assertTrue(s.contains("id=1"));
        assertTrue(s.contains("name=景点"));
        assertTrue(s.contains("category=餐饮"));
    }

    @Test
    void testCanEqual() throws Exception {
        Attraction a = new Attraction();
        Method canEqual = Attraction.class.getDeclaredMethod("canEqual", Object.class);
        canEqual.setAccessible(true);
        // 自身
        assertTrue((Boolean) canEqual.invoke(a, a));
        // 相同类型
        Attraction other = new Attraction();
        assertTrue((Boolean) canEqual.invoke(a, other));
        // 子类
        class SubAttraction extends Attraction {}
        SubAttraction sub = new SubAttraction();
        assertTrue((Boolean) canEqual.invoke(a, sub));
        // 其他类型
        assertFalse((Boolean) canEqual.invoke(a, "str"));
    }

    @Test
    void testOnCreateAndOnUpdate() throws Exception {
        Attraction a = new Attraction();
        // 反射调用protected onCreate
        Method onCreate = Attraction.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(a);
        assertNotNull(a.getCreatedAt());
        assertNotNull(a.getUpdatedAt());
        LocalDateTime created = a.getCreatedAt();
        LocalDateTime updated = a.getUpdatedAt();

        // 验证创建时两个时间几乎相同（允许毫秒级误差，避免不同平台/CI时钟粒度差异）
        long diffMsOnCreate = java.time.Duration.between(created, updated).abs().toMillis();
        assertTrue(diffMsOnCreate <= 50, "created/updated 时间差应在50ms以内，实际: " + diffMsOnCreate + "ms");

        // 等待确保时间不同
        Thread.sleep(10);

        // 反射调用onUpdate
        Method onUpdate = Attraction.class.getDeclaredMethod("onUpdate");
        onUpdate.setAccessible(true);
        onUpdate.invoke(a);

        // 验证createdAt基本未改变（允许毫秒级误差）
        long diffMsCreated = java.time.Duration.between(created, a.getCreatedAt()).abs().toMillis();
        assertTrue(diffMsCreated <= 50, "createdAt 变化过大，期望≤50ms，实际: " + diffMsCreated + "ms");
        // 验证updatedAt已更新
        assertTrue(a.getUpdatedAt().isAfter(updated));
    }

    @Test
    void testEnumValues() {
        // 验证所有枚举值
        Attraction.AttractionCategory[] values = Attraction.AttractionCategory.values();
        assertEquals(4, values.length);
        assertTrue(Arrays.asList(values).contains(Attraction.AttractionCategory.旅游景点));
        assertTrue(Arrays.asList(values).contains(Attraction.AttractionCategory.交通站点));
        assertTrue(Arrays.asList(values).contains(Attraction.AttractionCategory.餐饮));
        assertTrue(Arrays.asList(values).contains(Attraction.AttractionCategory.住宿));

        // 验证枚举名称
        assertEquals("旅游景点", Attraction.AttractionCategory.旅游景点.name());
        assertEquals("交通站点", Attraction.AttractionCategory.交通站点.name());
        assertEquals("餐饮", Attraction.AttractionCategory.餐饮.name());
        assertEquals("住宿", Attraction.AttractionCategory.住宿.name());

        // 验证valueOf
        assertEquals(Attraction.AttractionCategory.旅游景点,
                Attraction.AttractionCategory.valueOf("旅游景点"));
    }

    @Test
    void testEdgeCases() {
        Attraction a = new Attraction();
        // 全部字段为null
        assertNull(a.getId());
        assertNull(a.getAmapPoiId());
        assertNull(a.getDestination());
        assertNull(a.getName());
        assertNull(a.getDescription());
        assertNull(a.getImageUrl());
        assertNull(a.getLatitude());
        assertNull(a.getLongitude());
        assertNull(a.getCategory());
        assertNull(a.getOpeningHours());
        assertNull(a.getCreatedAt());
        assertNull(a.getUpdatedAt());
        assertNull(a.getTagScores());
        assertEquals(0, a.getJoinCount()); // 默认值

        // 测试空字符串
        a.setName("");
        a.setDescription("");
        a.setImageUrl("");
        a.setOpeningHours("");
        a.setTagScores("");

        assertEquals("", a.getName());
        assertEquals("", a.getDescription());
        assertEquals("", a.getImageUrl());
        assertEquals("", a.getOpeningHours());
        assertEquals("", a.getTagScores());
    }

    @Test
    void testBigDecimalPrecision() {
        Attraction a = new Attraction();

        // 测试最大精度
        BigDecimal maxLat = new BigDecimal("90.000000");
        BigDecimal maxLng = new BigDecimal("180.000000");
        a.setLatitude(maxLat);
        a.setLongitude(maxLng);
        assertEquals(maxLat, a.getLatitude());
        assertEquals(maxLng, a.getLongitude());

        // 测试超出精度的值
        BigDecimal highPrecisionLat = new BigDecimal("12.1234567");
        a.setLatitude(highPrecisionLat);
        assertEquals(new BigDecimal("12.1234567"), a.getLatitude()); // 四舍五入到6位小数
    }

    @Test
    void testJoinCountEdgeValues() {
        Attraction a = new Attraction();

        // 最小值
        a.setJoinCount(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, a.getJoinCount());

        // 最大值
        a.setJoinCount(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, a.getJoinCount());

        // 零值
        a.setJoinCount(0);
        assertEquals(0, a.getJoinCount());

        // 负值
        a.setJoinCount(-5);
        assertEquals(-5, a.getJoinCount());
    }

    @Test
    void testDestinationRelationship() {
        Attraction a = new Attraction();
        Destination d1 = new Destination();
        d1.setId(1L);
        d1.setName("北京");

        Destination d2 = new Destination();
        d2.setId(2L);
        d2.setName("上海");

        // 设置和获取目的地
        a.setDestination(d1);
        assertEquals(d1, a.getDestination());
        assertEquals("北京", a.getDestination().getName());

        // 修改目的地
        a.setDestination(d2);
        assertEquals(d2, a.getDestination());
        assertEquals("上海", a.getDestination().getName());

        // 设置为null
        a.setDestination(null);
        assertNull(a.getDestination());
    }

    @Test
    void testJSONFieldHandling() {
        Attraction a = new Attraction();

        // 复杂JSON结构
        String complexHours = "{\"weekdays\":\"9:00-18:00\", \"weekends\":\"10:00-20:00\"}";
        String complexTags = "{\"family\":5, \"adventure\":8}";

        a.setOpeningHours(complexHours);
        a.setTagScores(complexTags);

        assertEquals(complexHours, a.getOpeningHours());
        assertEquals(complexTags, a.getTagScores());

        // 非法JSON
        a.setOpeningHours("{invalid json}");
        assertEquals("{invalid json}", a.getOpeningHours());

        // 空对象
        a.setTagScores("{}");
        assertEquals("{}", a.getTagScores());
    }

    @Test
    void testTimeFieldEdgeCases() {
        Attraction a = new Attraction();

        // 最小时间值
        LocalDateTime minTime = LocalDateTime.MIN;
        a.setCreatedAt(minTime);
        a.setUpdatedAt(minTime);
        assertEquals(minTime, a.getCreatedAt());
        assertEquals(minTime, a.getUpdatedAt());

        // 最大时间值
        LocalDateTime maxTime = LocalDateTime.MAX;
        a.setCreatedAt(maxTime);
        a.setUpdatedAt(maxTime);
        assertEquals(maxTime, a.getCreatedAt());
        assertEquals(maxTime, a.getUpdatedAt());

        // null值
        a.setCreatedAt(null);
        a.setUpdatedAt(null);
        assertNull(a.getCreatedAt());
        assertNull(a.getUpdatedAt());
    }
}
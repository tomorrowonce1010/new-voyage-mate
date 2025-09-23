package com.se_07.backend.destination.Entity;

import com.se_07.backend.entity.Destination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class DestinationTest {

    private Destination destination;
    private Destination sameDestination;
    private Destination differentDestination;

    @BeforeEach
    void setUp() {
        destination = new Destination();
        destination.setId(1L);
        destination.setAmapPoiId("POI123");
        destination.setName("西湖景区");
        destination.setDescription("杭州著名景点");
        destination.setImageUrl("https://example.com/image.jpg");
        destination.setLatitude(new BigDecimal("30.246580"));
        destination.setLongitude(new BigDecimal("120.108620"));
        destination.setJoinCount(100);
        destination.setCreatedAt(LocalDateTime.now().minusDays(1));
        destination.setUpdatedAt(LocalDateTime.now());
        destination.setTagScores("{\"scenery\":9,\"culture\":8}");

        // 创建相同内容的对象
        sameDestination = new Destination();
        sameDestination.setId(1L);
        sameDestination.setAmapPoiId("POI123");
        sameDestination.setName("西湖景区");
        sameDestination.setDescription("杭州著名景点");
        sameDestination.setImageUrl("https://example.com/image.jpg");
        sameDestination.setLatitude(new BigDecimal("30.246580"));
        sameDestination.setLongitude(new BigDecimal("120.108620"));
        sameDestination.setJoinCount(100);
        sameDestination.setCreatedAt(destination.getCreatedAt());
        sameDestination.setUpdatedAt(destination.getUpdatedAt());
        sameDestination.setTagScores("{\"scenery\":9,\"culture\":8}");

        // 创建不同内容的对象
        differentDestination = new Destination();
        differentDestination.setId(2L);
        differentDestination.setAmapPoiId("POI456");
        differentDestination.setName("灵隐寺");
        differentDestination.setDescription("杭州著名寺庙");
        differentDestination.setImageUrl("https://example.com/image2.jpg");
        differentDestination.setLatitude(new BigDecimal("30.255000"));
        differentDestination.setLongitude(new BigDecimal("120.095000"));
        differentDestination.setJoinCount(50);
        differentDestination.setCreatedAt(LocalDateTime.now().minusDays(2));
        differentDestination.setUpdatedAt(LocalDateTime.now().minusHours(1));
        differentDestination.setTagScores("{\"religion\":9,\"history\":8}");
    }

    @Test
    void testGettersAndSetters() {
        // ID
        assertEquals(1L, destination.getId());
        destination.setId(2L);
        assertEquals(2L, destination.getId());
        destination.setId(null);
        assertNull(destination.getId());

        // Amap POI ID
        assertEquals("POI123", destination.getAmapPoiId());
        destination.setAmapPoiId("POI456");
        assertEquals("POI456", destination.getAmapPoiId());
        destination.setAmapPoiId(null);
        assertNull(destination.getAmapPoiId());

        // 测试边界值
        destination.setAmapPoiId(new String(new char[50]).replace('\0', 'A'));
        assertEquals(50, destination.getAmapPoiId().length());
        destination.setAmapPoiId(new String(new char[51]).replace('\0', 'B'));
        assertEquals(51, destination.getAmapPoiId().length());

        // Name
        assertEquals("西湖景区", destination.getName());
        destination.setName("雷峰塔");
        assertEquals("雷峰塔", destination.getName());
        destination.setName(null);
        assertNull(destination.getName());

        // 测试边界值
        destination.setName(new String(new char[200]).replace('\0', 'C'));
        assertEquals(200, destination.getName().length());
        destination.setName(new String(new char[201]).replace('\0', 'D'));
        assertEquals(201, destination.getName().length());

        // Description
        assertEquals("杭州著名景点", destination.getDescription());
        destination.setDescription("新描述");
        assertEquals("新描述", destination.getDescription());
        destination.setDescription(null);
        assertNull(destination.getDescription());

        // Image URL
        assertEquals("https://example.com/image.jpg", destination.getImageUrl());
        destination.setImageUrl("https://example.com/new.jpg");
        assertEquals("https://example.com/new.jpg", destination.getImageUrl());
        destination.setImageUrl(null);
        assertNull(destination.getImageUrl());

        // 测试边界值
        destination.setImageUrl(new String(new char[500]).replace('\0', 'E'));
        assertEquals(500, destination.getImageUrl().length());
        destination.setImageUrl(new String(new char[501]).replace('\0', 'F'));
        assertEquals(501, destination.getImageUrl().length());

        // Latitude
        assertEquals(new BigDecimal("30.246580"), destination.getLatitude());
        destination.setLatitude(new BigDecimal("35.000000"));
        assertEquals(new BigDecimal("35.000000"), destination.getLatitude());
        destination.setLatitude(null);
        assertNull(destination.getLatitude());

        // 测试精度
        destination.setLatitude(new BigDecimal("123.123456"));
        assertEquals(6, destination.getLatitude().scale());

        // Longitude
        assertEquals(new BigDecimal("120.108620"), destination.getLongitude());
        destination.setLongitude(new BigDecimal("125.000000"));
        assertEquals(new BigDecimal("125.000000"), destination.getLongitude());
        destination.setLongitude(null);
        assertNull(destination.getLongitude());

        // 测试精度
        destination.setLongitude(new BigDecimal("123.123456"));
        assertEquals(6, destination.getLongitude().scale());

        // Join Count
        assertEquals(100, destination.getJoinCount());
        destination.setJoinCount(200);
        assertEquals(200, destination.getJoinCount());
        destination.setJoinCount(null);
        assertNull(destination.getJoinCount());

        // 测试边界值
        destination.setJoinCount(0);
        assertEquals(0, destination.getJoinCount());
        destination.setJoinCount(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, destination.getJoinCount());
        destination.setJoinCount(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, destination.getJoinCount());

        // Created At
        LocalDateTime now = LocalDateTime.now();
        destination.setCreatedAt(now);
        assertEquals(now, destination.getCreatedAt());
        destination.setCreatedAt(null);
        assertNull(destination.getCreatedAt());

        // Updated At
        destination.setUpdatedAt(now);
        assertEquals(now, destination.getUpdatedAt());
        destination.setUpdatedAt(null);
        assertNull(destination.getUpdatedAt());

        // Tag Scores
        assertEquals("{\"scenery\":9,\"culture\":8}", destination.getTagScores());
        destination.setTagScores("{\"new\":5}");
        assertEquals("{\"new\":5}", destination.getTagScores());
        destination.setTagScores(null);
        assertNull(destination.getTagScores());
    }

    @Test
    void testEqualsAndHashCode() {
        // 相同对象
        assertEquals(destination, sameDestination);
        assertEquals(destination.hashCode(), sameDestination.hashCode());

        // 不同对象
        assertNotEquals(destination, differentDestination);
        assertNotEquals(destination.hashCode(), differentDestination.hashCode());

        // 自反性
        assertEquals(destination, destination);

        // 对称性
        assertEquals(destination, sameDestination);
        assertEquals(sameDestination, destination);

        // 传递性
        Destination anotherSame = new Destination();
        anotherSame.setId(1L);
        anotherSame.setName("西湖景区");
        assertEquals(destination, sameDestination);
        assertNotEquals(sameDestination, anotherSame);
        assertNotEquals(destination, anotherSame);

        // null比较
        assertNotEquals(null, destination);

        // 不同类型比较
        assertNotEquals(destination, "字符串");

        // 测试每个字段不同时的相等性
        testFieldInequality("id", 99L);
        testFieldInequality("amapPoiId", "DIFFERENT");
        testFieldInequality("name", "不同的名称");
        testFieldInequality("description", "不同的描述");
        testFieldInequality("imageUrl", "不同的URL");
        testFieldInequality("latitude", new BigDecimal("99.999999"));
        testFieldInequality("longitude", new BigDecimal("99.999999"));
        testFieldInequality("joinCount", 999);
        testFieldInequality("createdAt", LocalDateTime.now().plusDays(10));
        testFieldInequality("updatedAt", LocalDateTime.now().plusDays(10));
        testFieldInequality("tagScores", "{\"different\":true}");
    }

    private void testFieldInequality(String fieldName, Object differentValue) {
        Destination modified = new Destination();

        // 复制所有字段
        modified.setId(destination.getId());
        modified.setAmapPoiId(destination.getAmapPoiId());
        modified.setName(destination.getName());
        modified.setDescription(destination.getDescription());
        modified.setImageUrl(destination.getImageUrl());
        modified.setLatitude(destination.getLatitude());
        modified.setLongitude(destination.getLongitude());
        modified.setJoinCount(destination.getJoinCount());
        modified.setCreatedAt(destination.getCreatedAt());
        modified.setUpdatedAt(destination.getUpdatedAt());
        modified.setTagScores(destination.getTagScores());

        // 修改指定字段
        switch (fieldName) {
            case "id": modified.setId((Long) differentValue); break;
            case "amapPoiId": modified.setAmapPoiId((String) differentValue); break;
            case "name": modified.setName((String) differentValue); break;
            case "description": modified.setDescription((String) differentValue); break;
            case "imageUrl": modified.setImageUrl((String) differentValue); break;
            case "latitude": modified.setLatitude((BigDecimal) differentValue); break;
            case "longitude": modified.setLongitude((BigDecimal) differentValue); break;
            case "joinCount": modified.setJoinCount((Integer) differentValue); break;
            case "createdAt": modified.setCreatedAt((LocalDateTime) differentValue); break;
            case "updatedAt": modified.setUpdatedAt((LocalDateTime) differentValue); break;
            case "tagScores": modified.setTagScores((String) differentValue); break;
        }

        assertNotEquals(destination, modified, "字段 " + fieldName + " 不同时应不相等");
        assertNotEquals(destination.hashCode(), modified.hashCode(), "字段 " + fieldName + " 不同时hashCode应不同");
    }

    @Test
    void testNullFieldEquality() {
        // 全null对象
        Destination nullDestination1 = new Destination();
        Destination nullDestination2 = new Destination();
        assertEquals(nullDestination1, nullDestination2);
        assertEquals(nullDestination1.hashCode(), nullDestination2.hashCode());

        // 部分null vs 全null
        Destination partialNull = new Destination();
        partialNull.setId(1L);
        assertNotEquals(nullDestination1, partialNull);

        // 相同部分null
        Destination partialNull1 = new Destination();
        partialNull1.setId(1L);
        partialNull1.setName(null);

        Destination partialNull2 = new Destination();
        partialNull2.setId(1L);
        partialNull2.setName(null);

        assertEquals(partialNull1, partialNull2);
    }

    @Test
    void testToString() {
        String str = destination.toString();

        // 验证包含所有字段
        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("amapPoiId=POI123"));
        assertTrue(str.contains("name=西湖景区"));
        assertTrue(str.contains("description=杭州著名景点"));
        assertTrue(str.contains("imageUrl=https://example.com/image.jpg"));
        assertTrue(str.contains("latitude=30.246580"));
        assertTrue(str.contains("longitude=120.108620"));
        assertTrue(str.contains("joinCount=100"));
        assertTrue(str.contains("createdAt="));
        assertTrue(str.contains("updatedAt="));
        assertTrue(str.contains("tagScores={\"scenery\":9,\"culture\":8}"));

        // 测试null值
        destination.setId(null);
        destination.setName(null);
        String nullStr = destination.toString();
        assertTrue(nullStr.contains("id=null"));
        assertTrue(nullStr.contains("name=null"));
    }

    @Test
    void testLifecycleCallbacks() throws Exception {
        Destination d = new Destination();
        // 反射调用onCreate
        java.lang.reflect.Method onCreate = Destination.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(d);
        assertNotNull(d.getCreatedAt());
        assertNotNull(d.getUpdatedAt());
        LocalDateTime created = d.getCreatedAt();
        Thread.sleep(10);
        // 反射调用onUpdate
        java.lang.reflect.Method onUpdate = Destination.class.getDeclaredMethod("onUpdate");
        onUpdate.setAccessible(true);
        onUpdate.invoke(d);
        assertTrue(d.getUpdatedAt().isAfter(created) || d.getUpdatedAt().isEqual(created));
    }

    @Test
    void testPrecisionAndScale() {
        // 测试纬度的精度和小数位
        BigDecimal highPrecisionLat = new BigDecimal("123.1234567890");
        destination.setLatitude(highPrecisionLat);
        assertEquals(1, new BigDecimal("123.123457").compareTo(destination.getLatitude()));

        // 测试经度的精度和小数位
        BigDecimal highPrecisionLon = new BigDecimal("-45.9876543210");
        destination.setLongitude(highPrecisionLon);
        assertEquals(1, new BigDecimal("-45.987654").compareTo(destination.getLongitude()));
    }

    @Test
    void testJoinCountEdgeCases() {
        // 测试边界值
        destination.setJoinCount(0);
        assertEquals(0, destination.getJoinCount());

        destination.setJoinCount(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, destination.getJoinCount());

        destination.setJoinCount(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, destination.getJoinCount());

        // 测试负数
        destination.setJoinCount(-5);
        assertEquals(-5, destination.getJoinCount());
    }

    @Test
    void testDateTimeHandling() {
        // 测试日期时间精度
        LocalDateTime preciseTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        destination.setCreatedAt(preciseTime);
        assertEquals(preciseTime, destination.getCreatedAt());

        // 测试null值处理
        destination.setUpdatedAt(null);
        assertNull(destination.getUpdatedAt());

        // 测试不同时区（LocalDateTime不包含时区信息）
        LocalDateTime utcTime = LocalDateTime.now();
        destination.setCreatedAt(utcTime);
        assertEquals(utcTime, destination.getCreatedAt());
    }

    @Test
    void testTagScoresHandling() {
        // 测试空JSON
        destination.setTagScores("{}");
        assertEquals("{}", destination.getTagScores());

        // 测试大JSON
        String baseJson = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(baseJson);
        String largeJson = sb.toString();
        destination.setTagScores(largeJson);
        assertEquals(largeJson, destination.getTagScores());

        // 测试无效JSON
        destination.setTagScores("{invalid}");
        assertEquals("{invalid}", destination.getTagScores());
    }

    @Test
    void testCanEqual() throws Exception {
        Destination d = new Destination();
        java.lang.reflect.Method m = Destination.class.getDeclaredMethod("canEqual", Object.class);
        m.setAccessible(true);
        // 自身类型
        assertTrue((Boolean) m.invoke(d, d));
        // 其它类型
        assertFalse((Boolean) m.invoke(d, "string"));
        // 子类
        class SubDestination extends Destination {}
        assertTrue((Boolean) m.invoke(d, new SubDestination()));
    }

    @Test
    void testHashCodeConsistency() {
        int initialHashCode = destination.hashCode();
        assertEquals(initialHashCode, destination.hashCode());
        assertEquals(initialHashCode, destination.hashCode());

        // 修改后hashCode应变化
        destination.setName("新名称");
        assertNotEquals(initialHashCode, destination.hashCode());
    }

    @Test
    void testEqualsWithSubclass() {
        class SubDestination extends Destination {
            private String extraField = "extra";
        }

        SubDestination sub = new SubDestination();
        sub.setId(1L);
        sub.setName("西湖景区");

        // 即使字段相同，不同类型也不应相等
        assertNotEquals(destination, sub);
        assertNotEquals(sub, destination);
    }

    @Test
    void testEqualsWithDifferentNullCombinations() {
        Destination d1 = new Destination();
        d1.setId(1L);
        d1.setName(null);

        Destination d2 = new Destination();
        d2.setId(1L);
        d2.setName("非空");

        assertNotEquals(d1, d2);
    }

    @Test
    void testBigDecimalEquality() {
        Destination d1 = new Destination();
        d1.setLatitude(new BigDecimal("30.000000"));

        Destination d2 = new Destination();
        d2.setLatitude(new BigDecimal("30.000000"));

        Destination d3 = new Destination();
        d3.setLatitude(new BigDecimal("30.000001"));

        assertEquals(d1, d2);
        assertNotEquals(d1, d3);

        // 测试不同精度
        Destination d4 = new Destination();
        d4.setLatitude(new BigDecimal("30.0"));
        assertNotEquals(d1, d4);
    }
}
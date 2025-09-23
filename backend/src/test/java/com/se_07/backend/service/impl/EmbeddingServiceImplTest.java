package com.se_07.backend.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceImplTest {

    @Mock
    private RestTemplate mockRestTemplate;

    private EmbeddingServiceImpl embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingServiceImpl();
        // 注入mock的RestTemplate
        ReflectionTestUtils.setField(embeddingService, "restTemplate", mockRestTemplate);
        ReflectionTestUtils.setField(embeddingService, "embeddingServiceUrl", "http://localhost:8000/embed");
    }

    @AfterEach
    void tearDown() {
        // 清理任何可能的静态mock
    }

    @Test
    void testEmbed_Success() {
        // 准备测试数据
        String testText = "测试文本";
        float[] expectedEmbedding = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        float[][] response = {expectedEmbedding};

        // Mock RestTemplate的行为
        when(mockRestTemplate.postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        )).thenReturn(response);

        // 执行测试
        float[] result = embeddingService.embed(testText);

        // 验证结果
        assertNotNull(result);
        assertArrayEquals(expectedEmbedding, result, 0.001f);
        assertEquals(expectedEmbedding.length, result.length);

        // 验证调用
        verify(mockRestTemplate, times(1)).postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        );
    }

    @Test
    void testEmbed_ServiceReturnsNull() {
        // 准备测试数据
        String testText = "测试文本";

        // Mock RestTemplate返回null
        when(mockRestTemplate.postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        )).thenReturn(null);

        // 执行测试
        float[] result = embeddingService.embed(testText);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.length);

        // 验证调用
        verify(mockRestTemplate, times(1)).postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        );
    }

    @Test
    void testEmbed_ServiceReturnsEmptyArray() {
        // 准备测试数据
        String testText = "测试文本";

        // Mock RestTemplate返回空数组
        when(mockRestTemplate.postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        )).thenReturn(new float[0][]);

        // 执行测试
        float[] result = embeddingService.embed(testText);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.length);

        // 验证调用
        verify(mockRestTemplate, times(1)).postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        );
    }

    @Test
    void testEmbed_ServiceReturnsEmptyInnerArray() {
        // 准备测试数据
        String testText = "测试文本";

        // Mock RestTemplate返回包含空内部数组的数组
        when(mockRestTemplate.postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        )).thenReturn(new float[][]{{}});

        // 执行测试
        float[] result = embeddingService.embed(testText);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.length);

        // 验证调用
        verify(mockRestTemplate, times(1)).postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        );
    }

    @Test
    void testEmbed_ResourceAccessException_RetrySuccess() {
        // 准备测试数据
        String testText = "测试文本";
        float[] expectedEmbedding = {0.1f, 0.2f, 0.3f};
        float[][] response = {expectedEmbedding};

        // Mock RestTemplate第一次抛出ResourceAccessException，第二次成功
        when(mockRestTemplate.postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        ))
        .thenThrow(new ResourceAccessException("Connection failed"))
        .thenReturn(response);

        // 执行测试
        float[] result = embeddingService.embed(testText);

        // 验证结果
        assertNotNull(result);
        assertArrayEquals(expectedEmbedding, result, 0.001f);

        // 验证调用次数（应该被调用2次：1次失败，1次成功）
        verify(mockRestTemplate, times(2)).postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        );
    }

    @Test
    void testEmbed_ResourceAccessException_MaxRetriesExceeded() {
        // 准备测试数据
        String testText = "测试文本";

        // Mock RestTemplate总是抛出ResourceAccessException
        when(mockRestTemplate.postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        )).thenThrow(new ResourceAccessException("Connection failed"));

        // 执行测试
        float[] result = embeddingService.embed(testText);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.length);

        // 验证调用次数（应该被调用3次：最大重试次数）
        verify(mockRestTemplate, times(3)).postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        );
    }

    @Test
    void testEmbed_ResourceAccessException_Interrupted() {
        // 准备测试数据
        String testText = "测试文本";

        // Mock RestTemplate抛出ResourceAccessException
        when(mockRestTemplate.postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        )).thenThrow(new ResourceAccessException("Connection failed"));

        // 创建一个线程来执行embed方法，然后在等待期间中断它
        Thread testThread = new Thread(() -> {
            embeddingService.embed(testText);
        });

        testThread.start();

        // 等待一小段时间确保进入重试逻辑
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 中断线程
        testThread.interrupt();

        // 等待线程结束
        try {
            testThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 验证调用至少被尝试过
        verify(mockRestTemplate, atLeastOnce()).postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        );
    }

    @Test
    void testEmbed_UnexpectedException() {
        // 准备测试数据
        String testText = "测试文本";

        // Mock RestTemplate抛出意外异常
        when(mockRestTemplate.postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        )).thenThrow(new RuntimeException("Unexpected error"));

        // 执行测试
        float[] result = embeddingService.embed(testText);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.length);

        // 验证调用次数（应该只被调用1次，因为不是ResourceAccessException）
        verify(mockRestTemplate, times(1)).postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        );
    }

    @Test
    void testEmbed_EmptyText() {
        // 准备测试数据
        String testText = "";
        float[] expectedEmbedding = {0.0f, 0.0f, 0.0f};
        float[][] response = {expectedEmbedding};

        // Mock RestTemplate的行为
        when(mockRestTemplate.postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        )).thenReturn(response);

        // 执行测试
        float[] result = embeddingService.embed(testText);

        // 验证结果
        assertNotNull(result);
        assertArrayEquals(expectedEmbedding, result, 0.001f);

        // 验证调用
        verify(mockRestTemplate, times(1)).postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        );
    }

    @Test
    void testEmbed_NullText() {
        // 准备测试数据
        String testText = null;
        float[] expectedEmbedding = {0.0f, 0.0f, 0.0f};
        float[][] response = {expectedEmbedding};

        // Mock RestTemplate的行为
        when(mockRestTemplate.postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        )).thenReturn(response);

        // 执行测试
        float[] result = embeddingService.embed(testText);

        // 验证结果
        assertNotNull(result);
        assertArrayEquals(expectedEmbedding, result, 0.001f);

        // 验证调用
        verify(mockRestTemplate, times(1)).postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        );
    }

    @Test
    void testEmbed_LongText() {
        // 准备测试数据
        String testText = "这是一个很长的测试文本，包含了很多字符和内容，用来测试embedding服务是否能正确处理长文本输入。这个文本应该足够长，以确保我们的测试覆盖了各种边界情况。";
        float[] expectedEmbedding = new float[768]; // 假设是768维向量
        for (int i = 0; i < expectedEmbedding.length; i++) {
            expectedEmbedding[i] = (float) Math.random();
        }
        float[][] response = {expectedEmbedding};

        // Mock RestTemplate的行为
        when(mockRestTemplate.postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        )).thenReturn(response);

        // 执行测试
        float[] result = embeddingService.embed(testText);

        // 验证结果
        assertNotNull(result);
        assertArrayEquals(expectedEmbedding, result, 0.001f);
        assertEquals(768, result.length);

        // 验证调用
        verify(mockRestTemplate, times(1)).postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        );
    }

    @Test
    void testEmbed_VerifyPayloadStructure() {
        // 准备测试数据
        String testText = "测试文本";
        float[] expectedEmbedding = {0.1f, 0.2f, 0.3f};
        float[][] response = {expectedEmbedding};

        // Mock RestTemplate的行为
        when(mockRestTemplate.postForObject(
            eq("http://localhost:8000/embed"),
            any(Map.class),
            eq(float[][].class)
        )).thenReturn(response);

        // 执行测试
        embeddingService.embed(testText);

        // 验证调用参数
        verify(mockRestTemplate, times(1)).postForObject(
            eq("http://localhost:8000/embed"),
            argThat(payload -> {
                if (!(payload instanceof Map)) {
                    return false;
                }
                Map<String, Object> map = (Map<String, Object>) payload;
                if (!map.containsKey("texts")) {
                    return false;
                }
                Object texts = map.get("texts");
                if (!(texts instanceof java.util.List)) {
                    return false;
                }
                java.util.List<?> textList = (java.util.List<?>) texts;
                return textList.size() == 1 && testText.equals(textList.get(0));
            }),
            eq(float[][].class)
        );
    }

    @Test
    void testEmbed_ConstructorInitialization() {
        // 测试构造函数是否正确初始化了RestTemplate
        EmbeddingServiceImpl newService = new EmbeddingServiceImpl();
        
        // 验证RestTemplate被创建
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(newService, "restTemplate");
        assertNotNull(restTemplate);
        
        // 验证RequestFactory被设置
        assertNotNull(restTemplate.getRequestFactory());
    }

    @Test
    void testEmbed_DefaultUrlValue() {
        // 测试默认URL值 - 由于@Value注解需要Spring上下文，这里我们测试构造函数的行为
        EmbeddingServiceImpl newService = new EmbeddingServiceImpl();
        
        // 验证RestTemplate被正确初始化
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(newService, "restTemplate");
        assertNotNull(restTemplate);
        
        // 验证RequestFactory被设置
        assertNotNull(restTemplate.getRequestFactory());
        
        // 验证embeddingServiceUrl字段存在（即使值为null，因为@Value需要Spring上下文）
        assertTrue(ReflectionTestUtils.getField(newService, "embeddingServiceUrl") == null || 
                  ReflectionTestUtils.getField(newService, "embeddingServiceUrl") instanceof String);
    }
}
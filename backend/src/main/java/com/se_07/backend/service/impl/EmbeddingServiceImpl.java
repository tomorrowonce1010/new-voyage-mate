package com.se_07.backend.service.impl;

import com.se_07.backend.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

    private final RestTemplate restTemplate;

    @Value("${embedding.service.url:http://localhost:8000/embed}")
    private String embeddingServiceUrl;

    public EmbeddingServiceImpl() {
        // 配置RestTemplate以处理连接问题
        this.restTemplate = new RestTemplate();
        
        // 设置连接超时和读取超时
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10秒连接超时
        factory.setReadTimeout(30000);    // 30秒读取超时
        this.restTemplate.setRequestFactory(factory);
    }

    @Override
    public float[] embed(String text) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("texts", Collections.singletonList(text));
                
                log.debug("Calling embedding service for text: {}", text);
                float[][] res = restTemplate.postForObject(embeddingServiceUrl, payload, float[][].class);
                
                if (res != null && res.length > 0) {
                    log.debug("Successfully got embedding vector of length: {}", res[0].length);
                    return res[0];
                } else {
                    log.warn("Embedding service returned null or empty result");
                    return new float[0];
                }
            } catch (org.springframework.web.client.ResourceAccessException e) {
                retryCount++;
                log.warn("Connection error on attempt {}: {}", retryCount, e.getMessage());
                
                if (retryCount >= maxRetries) {
                    log.error("Failed to call embedding service after {} attempts", maxRetries, e);
                    return new float[0];
                }
                
                // 等待一段时间后重试
                try {
                    Thread.sleep(1000 * retryCount); // 递增等待时间
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new float[0];
                }
            } catch (Exception e) {
                log.error("Unexpected error calling embedding service", e);
                return new float[0];
            }
        }
        
        return new float[0];
    }
} 
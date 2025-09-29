package com.se_07.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.HashMap;
import java.util.Map;

@Service
public class SimpleRAGService {
    
    @Value("${rag.service.url:http://localhost:8000}")
    private String ragServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public SimpleRAGService() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * 检查RAG服务健康状态
     */
    public boolean isRAGServiceHealthy() {
        try {
            String url = ragServiceUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 搜索相关文本
     */
    public String searchText(String query, int topK) {
        try {
            String url = ragServiceUrl + "/search?query=" + java.net.URLEncoder.encode(query, "UTF-8") + "&top_k=" + topK;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                return (String) body.get("context");
            }
            return "未找到相关信息";
        } catch (Exception e) {
            return "搜索服务暂时不可用: " + e.getMessage();
        }
    }
    
    /**
     * 智能问答
     */
    public Map<String, Object> askQuestion(String question, int topK) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String url = ragServiceUrl + "/ask?question=" + java.net.URLEncoder.encode(question, "UTF-8") + "&top_k=" + topK;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                result.put("success", true);
                result.put("answer", body.get("answer"));
                result.put("context", body.get("context"));
                result.put("chunksUsed", body.get("chunks_used"));
            } else {
                result.put("success", false);
                result.put("answer", "RAG服务暂时不可用");
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            result.put("success", false);
            result.put("answer", "RAG服务错误: " + e.getStatusCode());
        } catch (Exception e) {
            result.put("success", false);
            result.put("answer", "RAG服务连接失败: " + e.getMessage());
        }
        
        return result;
    }
}

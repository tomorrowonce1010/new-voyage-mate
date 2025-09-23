package com.se_07.backend.service;

public interface EmbeddingService {
    /**
     * 获取单条文本的向量表示
     * @param text 输入文本
     * @return 768 维浮点数组
     */
    float[] embed(String text);
} 
package com.se_07.backend.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se_07.backend.entity.User;
import com.se_07.backend.service.EmbeddingService;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ElasticsearchIndexService {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchIndexService.class);

    @Value("${spring.elasticsearch.host:localhost}")
    private String elasticsearchHost;

    @Value("${spring.elasticsearch.port:9200}")
    private int elasticsearchPort;

    @Autowired
    private EmbeddingService embeddingService;

    private static final String AUTHORS_INDEX = "authors";
    private static final int VECTOR_DIMENSIONS = 768;

    /**
     * 创建Elasticsearch客户端
     * @return ElasticsearchClient
     */
    private ElasticsearchClient createElasticsearchClient() {
        RestClient restClient = RestClient.builder(
                new HttpHost(elasticsearchHost, elasticsearchPort, "http")
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }

    /**
     * 确保authors索引存在
     * @param client Elasticsearch客户端
     * @return 是否成功创建或已存在
     */
    private boolean ensureAuthorsIndexExists(ElasticsearchClient client) {
        try {
            // 检查索引是否存在
            boolean indexExists = client.indices().exists(r -> r.index(AUTHORS_INDEX)).value();
            if (indexExists) {
                logger.info("索引 '{}' 已存在", AUTHORS_INDEX);
                return true;
            }

            logger.info("索引 '{}' 不存在，开始创建...", AUTHORS_INDEX);

            // 使用简化的方式创建索引
            String mappingJson = "{\n" +
                    "  \"properties\": {\n" +
                    "    \"user_id\": { \"type\": \"keyword\" },\n" +
                    "    \"username\": { \n" +
                    "      \"type\": \"text\",\n" +
                    "      \"analyzer\": \"standard\",\n" +
                    "      \"fields\": { \"keyword\": { \"type\": \"keyword\" } }\n" +
                    "    },\n" +
                    "    \"email\": { \"type\": \"keyword\" },\n" +
                    "    \"bio\": { \"type\": \"text\", \"analyzer\": \"standard\" },\n" +
                    "    \"signature\": { \"type\": \"text\", \"analyzer\": \"standard\" },\n" +
                    "    \"vector\": { \n" +
                    "      \"type\": \"dense_vector\",\n" +
                    "      \"dims\": " + VECTOR_DIMENSIONS + ",\n" +
                    "      \"index\": true,\n" +
                    "      \"similarity\": \"cosine\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            // 创建索引
            CreateIndexResponse createIndexResponse = client.indices().create(c -> c
                    .index(AUTHORS_INDEX)
                    .settings(s -> s
                            .numberOfShards("1")
                            .numberOfReplicas("1"))
                    .mappings(m -> m.withJson(new java.io.StringReader(mappingJson)))
            );

            logger.info("索引 '{}' 创建结果: {}", AUTHORS_INDEX, createIndexResponse.acknowledged());
            
            return createIndexResponse.acknowledged();
        } catch (Exception e) {
            logger.error("创建索引 '{}' 失败: {}", AUTHORS_INDEX, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 异步索引用户到Elasticsearch
     * @param user 用户对象
     */
    public void indexUserToElasticsearch(User user) {
        // 使用异步线程池执行索引操作
        CompletableFuture.runAsync(() -> {
            ElasticsearchClient client = null;
            try {
                logger.info("开始索引用户 {} 到 Elasticsearch", user.getId());

                // 创建Elasticsearch客户端
                client = createElasticsearchClient();

                // 检查Elasticsearch连接
                logger.info("检查Elasticsearch连接...");
                try {
                    boolean isConnected = client.ping().value();
                    logger.info("Elasticsearch连接状态: {}", isConnected ? "成功" : "失败");
                    if (!isConnected) {
                        logger.error("无法连接到Elasticsearch");
                        return;
                    }
                } catch (Exception e) {
                    logger.error("Elasticsearch连接检查失败: {}", e.getMessage());
                    return;
                }

                // 确保索引存在
                if (!ensureAuthorsIndexExists(client)) {
                    logger.error("无法确保索引 '{}' 存在，索引操作取消", AUTHORS_INDEX);
                    return;
                }

                // 准备用户文本以生成向量
                String userText = user.getUsername() + " " + (user.getEmail() != null ? user.getEmail() : "");
                
                // 获取用户向量表示
                float[] vector = embeddingService.embed(userText);
                if (vector == null || vector.length == 0) {
                    logger.error("无法为用户 {} 生成向量表示", user.getId());
                    return;
                }

                // 构建文档
                Map<String, Object> document = new HashMap<>();
                document.put("user_id", user.getId());
                document.put("username", user.getUsername());
                document.put("email", user.getEmail());
                
                // 将float[]转换为List<Float>
                List<Float> vectorList = new ArrayList<>();
                for (float f : vector) {
                    vectorList.add(f);
                }
                document.put("vector", vectorList);

                logger.info("准备索引用户文档: {}", user.getUsername());

                // 索引到Elasticsearch
                IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                        .index(AUTHORS_INDEX)
                        .id(String.valueOf(user.getId()))
                        .document(document)
                );

                logger.info("准备发送索引请求到索引 '{}', 文档ID: {}", AUTHORS_INDEX, user.getId());

                IndexResponse response = client.index(request);

                logger.info("索引响应 - 结果: {}, 索引: {}, ID: {}, 版本: {}", 
                        response.result(), response.index(), response.id(), response.version());

                // 验证文档是否真的存在
                try {
                    Thread.sleep(1000); // 等待1秒确保索引完成
                    boolean docExists = client.exists(e -> e.index(AUTHORS_INDEX).id(String.valueOf(user.getId()))).value();
                    logger.info("验证文档存在性: {}", docExists ? "文档存在" : "文档不存在");
                } catch (Exception e) {
                    logger.warn("验证文档存在性失败: {}", e.getMessage());
                }

            } catch (Exception e) {
                logger.error("索引用户 {} 到 Elasticsearch 时发生错误: {}", user.getId(), e.getMessage(), e);
            } finally {
                // 关闭客户端
                if (client != null) {
                    try {
                        client._transport().close();
                    } catch (Exception e) {
                        logger.warn("关闭Elasticsearch客户端时发生错误: {}", e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 更新用户在Elasticsearch中的信息
     * @param user 用户对象
     */
    public void updateUserInElasticsearch(User user) {
        // 直接调用索引方法，它会覆盖现有文档
        indexUserToElasticsearch(user);
    }

    /**
     * 从Elasticsearch中删除用户
     * @param userId 用户ID
     */
    public void deleteUserFromElasticsearch(Long userId) {
        CompletableFuture.runAsync(() -> {
            ElasticsearchClient client = null;
            try {
                logger.info("开始从Elasticsearch删除用户 {}", userId);

                // 创建Elasticsearch客户端
                client = createElasticsearchClient();

                // 检查Elasticsearch连接
                boolean isConnected = client.ping().value();
                if (!isConnected) {
                    logger.error("无法连接到Elasticsearch");
                    return;
                }

                // 检查索引是否存在
                boolean indexExists = client.indices().exists(r -> r.index(AUTHORS_INDEX)).value();
                if (!indexExists) {
                    logger.warn("索引 '{}' 不存在，无需删除", AUTHORS_INDEX);
                    return;
                }

                // 删除文档
                client.delete(d -> d
                        .index(AUTHORS_INDEX)
                        .id(String.valueOf(userId))
                );

                logger.info("用户 {} 已从Elasticsearch中删除", userId);

            } catch (Exception e) {
                logger.error("从Elasticsearch删除用户 {} 时发生错误: {}", userId, e.getMessage(), e);
            } finally {
                // 关闭客户端
                if (client != null) {
                    try {
                        client._transport().close();
                    } catch (Exception e) {
                        logger.warn("关闭Elasticsearch客户端时发生错误: {}", e.getMessage());
                    }
                }
            }
        });
    }
}

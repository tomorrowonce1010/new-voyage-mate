package com.se_07.backend.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se_07.backend.entity.*;
import com.se_07.backend.repository.*;
import com.se_07.backend.service.AttractionService;
import com.se_07.backend.service.CommunityService;
import com.se_07.backend.service.DestinationService;
import com.se_07.backend.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SemanticSearchServiceImplTest {

    @Mock
    private ElasticsearchClient esClient;
    
    @Mock
    private DestinationRepository destinationRepository;
    
    @Mock
    private AttractionRepository attractionRepository;
    
    @Mock
    private TagRepository tagRepository;
    
    @Mock
    private CommunityEntryRepository communityEntryRepository;
    
    @Mock
    private CommunityEntryTagRepository communityEntryTagRepository;
    
    @Mock
    private EmbeddingService embeddingService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private DestinationService destinationService;
    
    @Mock
    private AttractionService attractionService;
    
    @Mock
    private CommunityService communityService;

    private SemanticSearchServiceImpl semanticSearchService;

    @BeforeEach
    void setUp() {
        semanticSearchService = new SemanticSearchServiceImpl(
                esClient, destinationRepository, attractionRepository, tagRepository,
                communityEntryRepository, communityEntryTagRepository, embeddingService,
                objectMapper, destinationService, attractionService, communityService
        );
        
        // 设置默认的索引名称
        ReflectionTestUtils.setField(semanticSearchService, "indexName", "destinations");
        ReflectionTestUtils.setField(semanticSearchService, "attractionIndexPrefix", "attractions_destination_");
        ReflectionTestUtils.setField(semanticSearchService, "communityIndexName", "community_entries");
    }

    @Test
    void testSemanticSearch_EmbeddingReturnsNull() {
        // 准备测试数据
        String query = "test query";
        int size = 5;
        
        // 模拟embedding服务返回null
        when(embeddingService.embed(query)).thenReturn(null);
        
        // 执行测试
        List<Destination> result = semanticSearchService.semanticSearch(query, size);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        // 验证方法调用
        verify(embeddingService).embed(query);
        verifyNoInteractions(esClient, destinationRepository);
    }

    @Test
    void testSemanticSearch_EmbeddingReturnsEmptyArray() {
        // 准备测试数据
        String query = "test query";
        int size = 5;
        
        // 模拟embedding服务返回空数组
        when(embeddingService.embed(query)).thenReturn(new float[0]);
        
        // 执行测试
        List<Destination> result = semanticSearchService.semanticSearch(query, size);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        // 验证方法调用
        verify(embeddingService).embed(query);
        verifyNoInteractions(esClient, destinationRepository);
    }

    @Test
    void testSemanticSearchByTags_NoSemanticResults() {
        // 准备测试数据
        String query = "nature";
        List<String> tags = Arrays.asList("beach", "mountain");
        int size = 5;
        
        // 模拟embedding服务返回null
        when(embeddingService.embed(query)).thenReturn(null);
        
        // 执行测试
        List<Destination> result = semanticSearchService.semanticSearchByTags(query, tags, size);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        // 验证方法调用
        verify(embeddingService).embed(query);
        verifyNoInteractions(esClient, destinationRepository, destinationService);
    }

    @Test
    void testSemanticSearchAttractions_EmbeddingReturnsNull() {
        // 准备测试数据
        Long destinationId = 1L;
        String query = "amusement park";
        int page = 0;
        int size = 10;
        
        // 模拟embedding服务返回null
        when(embeddingService.embed(query)).thenReturn(null);
        
        // 执行测试
        Page<Attraction> result = semanticSearchService.semanticSearchAttractions(destinationId, query, page, size);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        
        // 验证方法调用
        verify(embeddingService).embed(query);
        verifyNoInteractions(esClient, attractionRepository);
    }

    @Test
    void testFilterCommunityEntriesByTags_Success() {
        // 准备测试数据
        List<String> tags = Arrays.asList("travel", "adventure");
        int page = 0;
        int size = 10;
        
        // 创建测试条目
        CommunityEntry entry1 = createCommunityEntry(1L, "Amazing Trip");
        CommunityEntry entry2 = createCommunityEntry(2L, "Great Experience");
        
        // Mock repository返回所有条目
        when(communityEntryRepository.findAllPublic()).thenReturn(Arrays.asList(entry1, entry2));
        
        // Mock标签查询 - 第一个条目包含所有搜索标签（allMatch要求）
        when(communityEntryTagRepository.findByCommunityEntry(entry1)).thenReturn(Arrays.asList(
                createCommunityEntryTag(entry1, "travel"),
                createCommunityEntryTag(entry1, "adventure"),
                createCommunityEntryTag(entry1, "culture")
        ));
        // 第二个条目只包含部分搜索标签，不满足allMatch要求
        when(communityEntryTagRepository.findByCommunityEntry(entry2)).thenReturn(Arrays.asList(
                createCommunityEntryTag(entry2, "travel"),
                createCommunityEntryTag(entry2, "food")
        ));
        
        // Mock communityService返回条目详情
        Map<String, Object> entry1Map = new HashMap<>();
        entry1Map.put("id", 1L);
        entry1Map.put("description", "Amazing Trip");
        when(communityService.getCommunityEntryByShareCode(entry1.getShareCode())).thenReturn(entry1Map);
        
        // 执行测试
        Page<Map<String, Object>> result = semanticSearchService.filterCommunityEntriesByTags(tags, page, size);
        
        // 验证结果 - 只有第一个条目匹配所有标签
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        
        // 验证方法调用
        verify(communityEntryRepository).findAllPublic();
        verify(communityEntryTagRepository).findByCommunityEntry(entry1);
        verify(communityEntryTagRepository).findByCommunityEntry(entry2);
        verify(communityService).getCommunityEntryByShareCode(entry1.getShareCode());
    }

    @Test
    void testFilterCommunityEntriesByTags_EmptyTags() {
        // 准备测试数据
        List<String> tags = new ArrayList<>();
        int page = 0;
        int size = 10;
        
        // 执行测试
        Page<Map<String, Object>> result = semanticSearchService.filterCommunityEntriesByTags(tags, page, size);
        
        // 验证结果 - 空标签时直接返回空页，不调用repository
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        
        // 验证方法调用 - 空标签时不会调用repository
        verifyNoInteractions(communityEntryRepository);
    }

    @Test
    void testCalculatePopularityScore_WithHighJoinCount() {
        // 测试热度评分计算 - 高参与人数
        double score = (Double) ReflectionTestUtils.invokeMethod(semanticSearchService, 
                "calculatePopularityScore", 1000);
        
        // 验证结果 - 根据实现，使用log(1 + joinCount) / log(107)
        // log(1001) / log(107) ≈ 6.908 / 4.673 ≈ 1.478
        assertTrue(score > 0.0); // 只检查大于0，因为高参与人数可能超过1
        // 计算实际期望值：log(1001) / log(107) ≈ 1.478
        double expectedScore = Math.log(1001) / Math.log(107);
        assertEquals(expectedScore, score, 0.001); // 允许0.001的误差
        
        // 打印实际值用于调试
        System.out.println("Expected score: " + expectedScore);
        System.out.println("Actual score: " + score);
    }

    // --- 补充异常分支和边界测试 ---
    @Test
    void testSemanticSearch_ExceptionInRepository() throws Exception {
        String query = "test";
        int size = 5;
        float[] vector = {0.1f, 0.2f};
        when(embeddingService.embed(query)).thenReturn(vector);
        
        // Mock Elasticsearch response
        co.elastic.clients.elasticsearch.core.SearchResponse<Void> mockResponse = mock(co.elastic.clients.elasticsearch.core.SearchResponse.class);
        co.elastic.clients.elasticsearch.core.search.Hit<Void> mockHit = mock(co.elastic.clients.elasticsearch.core.search.Hit.class);
        when(mockHit.id()).thenReturn("1");
        when(mockHit.score()).thenReturn(0.8);
        when(mockResponse.hits()).thenReturn(mock(co.elastic.clients.elasticsearch.core.search.HitsMetadata.class));
        when(mockResponse.hits().hits()).thenReturn(Arrays.asList(mockHit));
        when(esClient.search(any(java.util.function.Function.class), eq(Void.class))).thenReturn(mockResponse);
        
        // repository抛异常
        when(destinationRepository.findAllById(any())).thenThrow(new RuntimeException("db error"));
        List<Destination> result = semanticSearchService.semanticSearch(query, size);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSemanticSearchByTags_TagsNull() throws Exception {
        String query = "test";
        float[] vector = {0.1f, 0.2f};
        when(embeddingService.embed(query)).thenReturn(vector);
        
        // Mock Elasticsearch response
        co.elastic.clients.elasticsearch.core.SearchResponse<Void> mockResponse = mock(co.elastic.clients.elasticsearch.core.SearchResponse.class);
        co.elastic.clients.elasticsearch.core.search.Hit<Void> mockHit = mock(co.elastic.clients.elasticsearch.core.search.Hit.class);
        when(mockHit.id()).thenReturn("1");
        when(mockHit.score()).thenReturn(0.8);
        when(mockResponse.hits()).thenReturn(mock(co.elastic.clients.elasticsearch.core.search.HitsMetadata.class));
        when(mockResponse.hits().hits()).thenReturn(Arrays.asList(mockHit));
        when(esClient.search(any(java.util.function.Function.class), eq(Void.class))).thenReturn(mockResponse);
        
        // repository返回空
        when(destinationRepository.findAllById(any())).thenReturn(Collections.emptyList());
        List<Destination> result = semanticSearchService.semanticSearchByTags(query, null, 5);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSemanticSearchAttractions_ValidPageSize() throws Exception {
        Long destinationId = 1L;
        String query = "test";
        int page = 0;
        int size = 1; // 测试有效页面大小
        float[] vector = {0.1f, 0.2f};
        when(embeddingService.embed(query)).thenReturn(vector);
        
        // Mock Elasticsearch response
        co.elastic.clients.elasticsearch.core.SearchResponse<Void> mockResponse = mock(co.elastic.clients.elasticsearch.core.SearchResponse.class);
        co.elastic.clients.elasticsearch.core.search.Hit<Void> mockHit = mock(co.elastic.clients.elasticsearch.core.search.Hit.class);
        when(mockHit.id()).thenReturn("1");
        when(mockHit.score()).thenReturn(0.8);
        when(mockResponse.hits()).thenReturn(mock(co.elastic.clients.elasticsearch.core.search.HitsMetadata.class));
        when(mockResponse.hits().hits()).thenReturn(Arrays.asList(mockHit));
        when(esClient.search(any(java.util.function.Function.class), eq(Void.class))).thenReturn(mockResponse);
        
        when(attractionRepository.findAllById(any())).thenReturn(Collections.emptyList());
        
        Page<Attraction> result = semanticSearchService.semanticSearchAttractions(destinationId, query, page, size);
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void testCalculateHybridScore_ZeroTotalResults() {
        double semanticScore = 0.5;
        double popularityScore = 0.3;
        int totalResults = 0;
        double hybridScore = (Double) ReflectionTestUtils.invokeMethod(semanticSearchService, 
                "calculateHybridScore", semanticScore, popularityScore, totalResults);
        assertTrue(hybridScore >= 0.0);
    }

    @Test
    void testCalculatePopularityScore_WithNullJoinCount() {
        // 测试热度评分计算 - null参与人数
        double score = (Double) ReflectionTestUtils.invokeMethod(semanticSearchService, 
                "calculatePopularityScore", (Integer) null);
        
        // 验证结果
        assertEquals(0.0, score, 0.001);
    }

    @Test
    void testCalculatePopularityScore_WithZeroJoinCount() {
        // 测试热度评分计算 - 零参与人数
        double score = (Double) ReflectionTestUtils.invokeMethod(semanticSearchService, 
                "calculatePopularityScore", 0);
        
        // 验证结果
        assertEquals(0.0, score, 0.001);
    }

    @Test
    void testCalculateHybridScore() {
        // 测试混合评分计算
        double semanticScore = 0.8;
        double popularityScore = 0.6;
        int totalResults = 100;
        
        double hybridScore = (Double) ReflectionTestUtils.invokeMethod(semanticSearchService, 
                "calculateHybridScore", semanticScore, popularityScore, totalResults);
        
        // 验证结果
        assertTrue(hybridScore > 0.0);
        assertTrue(hybridScore <= 2.0); // 语义分数 + 热度分数
    }

    @Test
    void testCalculateHybridScore_WithHighResults() {
        // 测试混合评分计算 - 高结果数
        double semanticScore = 0.9;
        double popularityScore = 0.7;
        int totalResults = 1000;
        
        double hybridScore = (Double) ReflectionTestUtils.invokeMethod(semanticSearchService, 
                "calculateHybridScore", semanticScore, popularityScore, totalResults);
        
        // 验证结果
        assertTrue(hybridScore > 0.0);
        assertTrue(hybridScore <= 2.0);
    }

    @Test
    void testCalculateHybridScore_WithLowResults() {
        // 测试混合评分计算 - 低结果数
        double semanticScore = 0.5;
        double popularityScore = 0.3;
        int totalResults = 5;
        
        double hybridScore = (Double) ReflectionTestUtils.invokeMethod(semanticSearchService, 
                "calculateHybridScore", semanticScore, popularityScore, totalResults);
        
        // 验证结果
        assertTrue(hybridScore > 0.0);
        assertTrue(hybridScore <= 2.0);
    }

    @Test
    void testGetCommunityEntryTags() {
        // 测试获取社区条目标签
        CommunityEntry entry = createCommunityEntry(1L, "Test Entry");
        List<CommunityEntryTag> tags = Arrays.asList(
                createCommunityEntryTag(entry, "travel"),
                createCommunityEntryTag(entry, "adventure"),
                createCommunityEntryTag(entry, "culture")
        );
        
        when(communityEntryTagRepository.findByCommunityEntry(entry)).thenReturn(tags);
        
        // 使用反射调用私有方法
        List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(semanticSearchService, 
                "getCommunityEntryTags", entry);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("travel"));
        assertTrue(result.contains("adventure"));
        assertTrue(result.contains("culture"));
        
        // 验证方法调用
        verify(communityEntryTagRepository).findByCommunityEntry(entry);
    }

    @Test
    void testGetCommunityEntryTags_EmptyTags() {
        // 测试获取社区条目标签 - 空标签
        CommunityEntry entry = createCommunityEntry(1L, "Test Entry");
        
        when(communityEntryTagRepository.findByCommunityEntry(entry)).thenReturn(new ArrayList<>());
        
        // 使用反射调用私有方法
        List<String> result = (List<String>) ReflectionTestUtils.invokeMethod(semanticSearchService, 
                "getCommunityEntryTags", entry);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        // 验证方法调用
        verify(communityEntryTagRepository).findByCommunityEntry(entry);
    }

    // 辅助方法：创建Destination对象
    private Destination createDestination(Long id, String name) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setName(name);
        return destination;
    }

    // 辅助方法：创建Attraction对象
    private Attraction createAttraction(Long id, String name, Integer joinCount) {
        Attraction attraction = new Attraction();
        attraction.setId(id);
        attraction.setName(name);
        attraction.setJoinCount(joinCount);
        return attraction;
    }

    // 辅助方法：创建CommunityEntry对象
    private CommunityEntry createCommunityEntry(Long id, String description) {
        CommunityEntry entry = new CommunityEntry();
        entry.setId(id);
        entry.setDescription(description);
        return entry;
    }

    // 辅助方法：创建CommunityEntryTag对象
    private CommunityEntryTag createCommunityEntryTag(CommunityEntry entry, String tagName) {
        CommunityEntryTag tag = new CommunityEntryTag();
        tag.setCommunityEntry(entry);
        Tag tagEntity = new Tag();
        tagEntity.setTag(tagName);
        tag.setTag(tagEntity);
        return tag;
    }
}
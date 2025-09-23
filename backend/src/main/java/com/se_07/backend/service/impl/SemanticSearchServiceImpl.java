package com.se_07.backend.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se_07.backend.entity.Attraction;
import com.se_07.backend.entity.CommunityEntry;
import com.se_07.backend.entity.CommunityEntryTag;
import com.se_07.backend.entity.Destination;
import com.se_07.backend.repository.*;
import com.se_07.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemanticSearchServiceImpl implements SemanticSearchService {

    private final ElasticsearchClient esClient;
    private final DestinationRepository destinationRepository;
    private final AttractionRepository attractionRepository;
    private final TagRepository tagRepository;
    private final CommunityEntryRepository communityEntryRepository;
    private final CommunityEntryTagRepository communityEntryTagRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final DestinationService destinationService;
    private final AttractionService attractionService;
    private final CommunityService communityService;

    @Value("${semantic.index.name:destinations}")
    private String indexName;
    
    @Value("${semantic.attraction.index.prefix:attractions_destination_}")
    private String attractionIndexPrefix;
    
    @Value("${semantic.community.index.name:community_entries}")
    private String communityIndexName;

    @Override
    public List<Destination> semanticSearch(String query, int size) {
        // 1. 调用嵌入服务获取向量
        float[] vector = embeddingService.embed(query);
        if (vector == null || vector.length == 0) {
            return Collections.emptyList();
        }

        // 2. 构造 KNN 查询
        List<Double> vecList = new ArrayList<>(vector.length);
        for (float v : vector) vecList.add((double) v);

        Script script = Script.of(s -> s.inline(i -> i
                .lang("painless")
                .source("cosineSimilarity(params.q,'vector')+1.0")
                .params("q", JsonData.of(vecList))
        ));

        try {
            SearchResponse<Void> response = esClient.search(s -> s
                    .index(indexName)
                    .size(size)
                    .query(q -> q.scriptScore(ss -> ss
                            .script(script)
                            .query(m -> m.matchAll(ma -> ma))
                    )) , Void.class);

            List<Long> ids = response.hits().hits().stream()
                    .map(Hit::id)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            // 按顺序取回对象
            List<Destination> destinations = destinationRepository.findAllById(ids);
            // 保持原排序
            Map<Long, Destination> id2Dest = destinations.stream().collect(Collectors.toMap(Destination::getId, d -> d));
            return ids.stream().map(id2Dest::get).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public List<Destination> semanticSearchByTags(String query, List<String> tags, int size) {
        // 1. 先获取所有目的地的语义搜索结果
        List<Destination> allSemanticResults = semanticSearch(query, 1000); // 获取大量结果用于标签过滤
        
        if (allSemanticResults.isEmpty()) {
            return Collections.emptyList();
        }

        
        // 2. 按标签过滤结果
        List<Destination> filteredResults = allSemanticResults.stream()
                .filter(destination -> {
                    // 检查目的地是否包含至少一个搜索标签
                    if (tags.isEmpty()) {
                        System.out.println("--------没有标签要求，返回所有结果----------");
                        return true; // 如果没有标签要求，返回所有结果
                    }
                    
                    // 获取目的地的前6个标签
                    List<String> destinationTopTags = destinationService.getTopNTags(destination, 6);
                    System.out.println("目的地: " + destination.getName() + ", 前6个标签: " + destinationTopTags);
                    
                    // 检查前6个标签中是否包含至少一个搜索标签
                    boolean hasMatchingTag = destinationTopTags.stream()
                            .anyMatch(destinationTag -> tags.contains(destinationTag));
                    
                    System.out.println("搜索标签: " + tags + ", 是否匹配: " + hasMatchingTag);
                    
                    return hasMatchingTag;
                })
                .limit(size)
                .collect(Collectors.toList());

        return filteredResults;
    }

    @Override
    public org.springframework.data.domain.Page<Attraction> semanticSearchAttractions(Long destinationId, String query, int page, int size) {
        // 1. 调用嵌入服务获取向量
        float[] vector = embeddingService.embed(query);
        if (vector == null || vector.length == 0) {
            return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }

        // 2. 构造 KNN 查询
        List<Double> vecList = new ArrayList<>(vector.length);
        for (float v : vector) vecList.add((double) v);

        Script script = Script.of(s -> s.inline(i -> i
                .lang("painless")
                .source("cosineSimilarity(params.q,'vector')+1.0")
                .params("q", JsonData.of(vecList))
        ));

        String attractionIndexName = attractionIndexPrefix + String.valueOf(destinationId);
        System.out.println("---------景点索引名称: " + attractionIndexName + "---------");

        try {
            // 获取所有语义搜索结果用于混合排序
            SearchResponse<Void> allResultsResponse = esClient.search(s -> s
                    .index(attractionIndexName)
                    .size(400) // 获取足够多的结果用于混合排序
                    .query(q -> q.scriptScore(ss -> ss
                            .script(script)
                            .query(m -> m.matchAll(ma -> ma))
                    )) , Void.class);

            // 提取ID和相似度分数
            List<SearchResult> searchResults = allResultsResponse.hits().hits().stream()
                    .map(hit -> new SearchResult(
                            Long.parseLong(hit.id()),
                            hit.score() != null ? hit.score() : 0.0
                    ))
                    .collect(Collectors.toList());

            if (searchResults.isEmpty()) {
                return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
            }

            // 获取所有景点数据
            List<Long> allIds = searchResults.stream()
                    .map(SearchResult::getId)
                    .collect(Collectors.toList());
            
            List<Attraction> allAttractions = attractionRepository.findAllById(allIds);
            Map<Long, Attraction> id2Attraction = allAttractions.stream()
                    .collect(Collectors.toMap(Attraction::getId, a -> a));

            // 计算混合评分并排序
            List<AttractionWithScore> attractionsWithScores = searchResults.stream()
                    .map(result -> {
                        Attraction attraction = id2Attraction.get(result.getId());
                        if (attraction != null) {
                            double semanticScore = result.getScore();
                            double popularityScore = calculatePopularityScore(attraction.getJoinCount());
                            double hybridScore = calculateHybridScore(semanticScore, popularityScore, searchResults.size());
                            
                            return new AttractionWithScore(attraction, hybridScore, semanticScore, popularityScore);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> Double.compare(b.getHybridScore(), a.getHybridScore())) // 降序排序
                    .collect(Collectors.toList());

            // 手动分页
            int totalElements = attractionsWithScores.size();
            int start = page * size;
            int end = Math.min(start + size, totalElements);
            
            List<Attraction> pageContent = start >= totalElements ? 
                    Collections.emptyList() : 
                    attractionsWithScores.subList(start, end).stream()
                            .map(AttractionWithScore::getAttraction)
                            .collect(Collectors.toList());

            // 创建分页结果
            org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page, size);
            return new org.springframework.data.domain.PageImpl<>(pageContent, pageRequest, totalElements);
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }
    }

    /**
     * 计算热度评分
     * 使用对数函数平滑热度差异，避免热门景点过度主导排序
     */
    private double calculatePopularityScore(Integer joinCount) {
        if (joinCount == null || joinCount <= 0) {
            return 0.0;
        }
        // 使用对数函数：log(1 + joinCount) / log(1 + maxJoinCount)
        // 当前最大热度为106，根据实际数据调整
        return Math.log(1 + joinCount) / Math.log(107);
    }

    /**
     * 计算混合评分
     * 根据不同的搜索场景调整语义相似度和热度的权重
     * @param semanticScore 语义相似度评分
     * @param popularityScore 热度评分
     * @param totalResults 总结果数
     * @param searchContext 搜索上下文："community" 表示社区搜索，"attraction" 表示景点搜索
     * @return 混合评分
     */
    private double calculateHybridScore(double semanticScore, double popularityScore, 
        int totalResults, String searchContext) {
        double semanticWeight;
        double popularityWeight;
        
        if ("community".equals(searchContext)) {
            // 社区搜索：更重视语义相似度
            semanticWeight = 0.9;
            popularityWeight = 0.1;
        } else {
            // 目的地与景点搜索（explore和destinationdetail页面）：更重视热度
            semanticWeight = 0.3;
            popularityWeight = 0.7;
        }
        return semanticWeight * semanticScore + popularityWeight * popularityScore;
    }
    
    /**
     * 计算混合评分（兼容旧版本）
     * 默认使用景点搜索的权重
     */
    private double calculateHybridScore(double semanticScore, double popularityScore, int totalResults) {
        return calculateHybridScore(semanticScore, popularityScore, totalResults, "attraction");
    }

    /**
     * 搜索结果内部类
     */
    private static class SearchResult {
        private final Long id;
        private final double score;

        public SearchResult(Long id, double score) {
            this.id = id;
            this.score = score;
        }

        public Long getId() { return id; }
        public double getScore() { return score; }
    }

    /**
     * 带评分的景点内部类
     */
    private static class AttractionWithScore {
        private final Attraction attraction;
        private final double hybridScore;
        private final double semanticScore;
        private final double popularityScore;

        public AttractionWithScore(Attraction attraction, double hybridScore, double semanticScore, double popularityScore) {
            this.attraction = attraction;
            this.hybridScore = hybridScore;
            this.semanticScore = semanticScore;
            this.popularityScore = popularityScore;
        }

        public Attraction getAttraction() { return attraction; }
        public double getHybridScore() { return hybridScore; }
        public double getSemanticScore() { return semanticScore; }
        public double getPopularityScore() { return popularityScore; }
    }

    @Override
    public org.springframework.data.domain.Page<Attraction> semanticSearchAttractionsByTags(Long destinationId, String query, List<String> tags, int page, int size) {
        // 1. 先获取所有景点的语义搜索结果（不分页，用于标签过滤）
        // 直接调用嵌入服务和Elasticsearch，获取所有结果用于标签过滤

        float[] vector = embeddingService.embed(query);
        if (vector == null || vector.length == 0) {
            return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }

        // 构造 KNN 查询
        List<Double> vecList = new ArrayList<>(vector.length);
        for (float v : vector) vecList.add((double) v);

        Script script = Script.of(s -> s.inline(i -> i
                .lang("painless")
                .source("cosineSimilarity(params.q,'vector')+1.0")
                .params("q", JsonData.of(vecList))
        ));

        String attractionIndexName = attractionIndexPrefix + String.valueOf(destinationId);
        System.out.println("---------景点索引名称: " + attractionIndexName + "---------");

        try {
            // 获取所有语义搜索结果用于标签过滤
            SearchResponse<Void> allResultsResponse = esClient.search(s -> s
                    .index(attractionIndexName)
                    .size(10000) // 获取大量结果用于标签过滤
                    .query(q -> q.scriptScore(ss -> ss
                            .script(script)
                            .query(m -> m.matchAll(ma -> ma))
                    )) , Void.class);

            // 提取ID和相似度分数
            List<SearchResult> searchResults = allResultsResponse.hits().hits().stream()
                    .map(hit -> new SearchResult(
                            Long.parseLong(hit.id()),
                            hit.score() != null ? hit.score() : 0.0
                    ))
                    .collect(Collectors.toList());

            if (searchResults.isEmpty()) {
                return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
            }

            // 获取所有景点数据
            List<Long> allIds = searchResults.stream()
                    .map(SearchResult::getId)
                    .collect(Collectors.toList());
            
            List<Attraction> allAttractions = attractionRepository.findAllById(allIds);
            Map<Long, Attraction> id2Attraction = allAttractions.stream()
                    .collect(Collectors.toMap(Attraction::getId, a -> a));

            // 2. 按标签过滤结果并计算混合评分
            List<AttractionWithScore> filteredResultsWithScores = searchResults.stream()
                    .map(result -> {
                        Attraction attraction = id2Attraction.get(result.getId());
                        if (attraction != null) {
                            // 检查景点是否包含至少一个搜索标签
                            if (tags.isEmpty()) {
                                System.out.println("--------没有标签要求，返回所有结果----------");
                                // 计算混合评分
                                double semanticScore = result.getScore();
                                double popularityScore = calculatePopularityScore(attraction.getJoinCount());
                                double hybridScore = calculateHybridScore(semanticScore, popularityScore, searchResults.size());
                                
                                return new AttractionWithScore(attraction, hybridScore, semanticScore, popularityScore);
                            }
                            
                            // 获取景点的前6个标签
                            List<String> attractionTopTags = attractionService.getTopNTags(attraction, 6);
                            System.out.println("景点: " + attraction.getName() + ", 前6个标签: " + attractionTopTags);
                            
                            // 检查前6个标签中是否包含至少一个搜索标签
                            boolean hasMatchingTag = attractionTopTags.stream()
                                    .anyMatch(attractionTag -> tags.contains(attractionTag));
                            
                            System.out.println("搜索标签: " + tags + ", 是否匹配: " + hasMatchingTag);
                            
                            if (hasMatchingTag) {
                                // 计算混合评分
                                double semanticScore = result.getScore();
                                double popularityScore = calculatePopularityScore(attraction.getJoinCount());
                                double hybridScore = calculateHybridScore(semanticScore, popularityScore, searchResults.size());
                                
                                return new AttractionWithScore(attraction, hybridScore, semanticScore, popularityScore);
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> Double.compare(b.getHybridScore(), a.getHybridScore())) // 降序排序
                    .collect(Collectors.toList());
            
            // 3. 手动分页
            int totalElements = filteredResultsWithScores.size();
            int start = page * size;
            int end = Math.min(start + size, totalElements);
            
            List<Attraction> pageContent = start >= totalElements ? 
                    Collections.emptyList() : 
                    filteredResultsWithScores.subList(start, end).stream()
                            .map(AttractionWithScore::getAttraction)
                            .collect(Collectors.toList());
            
            // 4. 创建分页结果
            org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page, size);
            return new org.springframework.data.domain.PageImpl<>(pageContent, pageRequest, totalElements);
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }
    }

    @Override
    public org.springframework.data.domain.Page<Map<String, Object>> semanticSearchCommunityEntries(String query, int page, int size) {
        // 1. 调用嵌入服务获取向量
        /*
        float[] vector = embeddingService.embed(query);
        if (vector == null || vector.length == 0) {
            return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }

        // 2. 构造 KNN 查询
        List<Double> vecList = new ArrayList<>(vector.length);
        for (float v : vector) vecList.add((double) v);

        Script script = Script.of(s -> s.inline(i -> i
                .lang("painless")
                .source("cosineSimilarity(params.q,'vector')+1.0")
                .params("q", JsonData.of(vecList))
        ));

        try {
            // 获取所有语义搜索结果用于混合排序
            SearchResponse<Void> allResultsResponse = esClient.search(s -> s
                    .index(communityIndexName)
                    .size(400) // 获取足够多的结果用于混合排序
                    .query(q -> q.scriptScore(ss -> ss
                            .script(script)
                            .query(m -> m.matchAll(ma -> ma))
                    )) , Void.class);

            // 提取ID和相似度分数
            List<SearchResult> searchResults = allResultsResponse.hits().hits().stream()
                    .map(hit -> new SearchResult(
                            Long.parseLong(hit.id()),
                            hit.score() != null ? hit.score() : 0.0
                    ))
                    .collect(Collectors.toList());

            if (searchResults.isEmpty()) {
                return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
            }

            // 获取所有社区条目数据
            List<Long> allIds = searchResults.stream()
                    .map(SearchResult::getId)
                    .collect(Collectors.toList());
            
            List<CommunityEntry> allEntries = communityEntryRepository.findAllById(allIds);
            Map<Long, CommunityEntry> id2Entry = allEntries.stream()
                    .collect(Collectors.toMap(CommunityEntry::getId, e -> e));

            // 计算混合评分并排序
            List<CommunityEntryWithScore> entriesWithScores = searchResults.stream()
                    .map(result -> {
                        CommunityEntry entry = id2Entry.get(result.getId());
                        if (entry != null) {
                            // 计算混合评分：语义相似度 + 热度（浏览量）
                            double semanticScore = result.getScore();
                            double popularityScore = calculatePopularityScore(entry.getViewCount());
                            double hybridScore = calculateHybridScore(semanticScore, popularityScore, searchResults.size(), "community");
                            
                            return new CommunityEntryWithScore(entry, hybridScore, semanticScore, popularityScore);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> Double.compare(b.getHybridScore(), a.getHybridScore())) // 降序排序
                    .collect(Collectors.toList());

            // 手动分页
            int totalElements = entriesWithScores.size();
            int start = page * size;
            int end = Math.min(start + size, totalElements);
            
            List<CommunityEntry> pageContent = start >= totalElements ? 
                    Collections.emptyList() : 
                    entriesWithScores.subList(start, end).stream()
                            .map(CommunityEntryWithScore::getEntry)
                            .collect(Collectors.toList());

            // 转换为Map格式
            List<Map<String, Object>> resultMaps = new ArrayList<>();
            for (CommunityEntry entry : pageContent) {
                try {
                    Map<String, Object> entryMap = communityService.getCommunityEntryByShareCode(entry.getShareCode());
                    if (entryMap != null) {
                        resultMaps.add(entryMap);
                    }
                } catch (Exception e) {
                    System.err.println("转换社区条目失败: " + e.getMessage());
                }
            }

            // 创建分页结果
            org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page, size);
            return new org.springframework.data.domain.PageImpl<>(resultMaps, pageRequest, totalElements);
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }
        */
        return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
    }

    @Override
    public org.springframework.data.domain.Page<Map<String, Object>> semanticSearchCommunityEntriesByTags(String query, List<String> tags, int page, int size) {
        // 1. 先获取所有社区条目的语义搜索结果（不分页，用于标签过滤）

        float[] vector = embeddingService.embed(query);
        if (vector == null || vector.length == 0) {
            return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }

        // 构造 KNN 查询
        List<Double> vecList = new ArrayList<>(vector.length);
        for (float v : vector) vecList.add((double) v);

        Script script = Script.of(s -> s.inline(i -> i
                .lang("painless")
                .source("cosineSimilarity(params.q,'vector')+1.0")
                .params("q", JsonData.of(vecList))
        ));

        try {
            // 获取所有语义搜索结果用于标签过滤
            SearchResponse<Void> allResultsResponse = esClient.search(s -> s
                    .index(communityIndexName)
                    .size(10000) // 获取大量结果用于标签过滤
                    .query(q -> q.scriptScore(ss -> ss
                            .script(script)
                            .query(m -> m.matchAll(ma -> ma))
                    )) , Void.class);

            // 提取ID和相似度分数，保持语义搜索的原始顺序
            List<SearchResult> searchResults = allResultsResponse.hits().hits().stream()
                    .map(hit -> new SearchResult(
                            Long.parseLong(hit.id()),
                            hit.score() != null ? hit.score() : 0.0
                    ))
                    .collect(Collectors.toList());

            if (searchResults.isEmpty()) {
                return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
            }

            // 获取所有社区条目数据
            List<Long> allIds = searchResults.stream()
                    .map(SearchResult::getId)
                    .collect(Collectors.toList());
            
            List<CommunityEntry> allEntries = communityEntryRepository.findAllById(allIds);
            Map<Long, CommunityEntry> id2Entry = allEntries.stream()
                    .collect(Collectors.toMap(CommunityEntry::getId, e -> e));

            // 2. 按标签过滤结果，保持语义搜索的原始顺序
            List<CommunityEntryWithScore> filteredResultsWithScores = searchResults.stream()
                    .map(result -> {
                        CommunityEntry entry = id2Entry.get(result.getId());
                        if (entry != null) {
                            // 检查社区条目是否包含所有搜索标签
                            if (tags.isEmpty()) {
                                System.out.println("--------没有标签要求，返回所有结果----------");
                                // 计算混合评分
                                double semanticScore = result.getScore();
                                double popularityScore = calculatePopularityScore(entry.getViewCount());
                                double hybridScore = calculateHybridScore(semanticScore, popularityScore, searchResults.size(), "community");
                                
                                return new CommunityEntryWithScore(entry, hybridScore, semanticScore, popularityScore);
                            }
                            
                            // 获取社区条目的标签
                            List<String> entryTags = getCommunityEntryTags(entry);
                            System.out.println("社区条目: " + entry.getShareCode() + ", 标签: " + entryTags);
                            
                            // 检查标签中是否包含所有搜索标签（而不是至少一个）
                            boolean hasAllMatchingTags = tags.stream()
                                    .allMatch(searchTag -> entryTags.contains(searchTag));
                            
                            System.out.println("搜索标签: " + tags + ", 是否包含所有标签: " + hasAllMatchingTags);
                            
                            if (hasAllMatchingTags) {
                                // 计算混合评分
                                double semanticScore = result.getScore();
                                double popularityScore = calculatePopularityScore(entry.getViewCount());
                                double hybridScore = calculateHybridScore(semanticScore, popularityScore, searchResults.size(), "community");
                                
                                return new CommunityEntryWithScore(entry, hybridScore, semanticScore, popularityScore);
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()); // 保持语义搜索的原始顺序，不重新排序
            
            // 3. 手动分页
            int totalElements = filteredResultsWithScores.size();
            int start = page * size;
            int end = Math.min(start + size, totalElements);
            
            List<CommunityEntry> pageContent = start >= totalElements ? 
                    Collections.emptyList() : 
                    filteredResultsWithScores.subList(start, end).stream()
                            .map(CommunityEntryWithScore::getEntry)
                            .collect(Collectors.toList());
            
            // 4. 转换为Map格式
            List<Map<String, Object>> resultMaps = new ArrayList<>();
            for (CommunityEntry entry : pageContent) {
                try {
                    Map<String, Object> entryMap = communityService.getCommunityEntryByShareCode(entry.getShareCode());
                    if (entryMap != null) {
                        resultMaps.add(entryMap);
                    }
                } catch (Exception e) {
                    System.err.println("转换社区条目失败: " + e.getMessage());
                }
            }
            
            // 5. 创建分页结果
            org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page, size);
            return new org.springframework.data.domain.PageImpl<>(resultMaps, pageRequest, totalElements);
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }


    }

    /**
     * 获取社区条目的标签
     */
    private List<String> getCommunityEntryTags(CommunityEntry entry) {
        try {
            List<CommunityEntryTag> tagLinks = communityEntryTagRepository.findByCommunityEntry(entry);
            return tagLinks.stream()
                    .map(link -> link.getTag().getTag())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("获取社区条目标签失败: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 带评分的社区条目内部类
     */
    private static class CommunityEntryWithScore {

        private final CommunityEntry entry;
        private final double hybridScore;
        private final double semanticScore;
        private final double popularityScore;

        public CommunityEntryWithScore(CommunityEntry entry, double hybridScore, double semanticScore, double popularityScore) {
            this.entry = entry;
            this.hybridScore = hybridScore;
            this.semanticScore = semanticScore;
            this.popularityScore = popularityScore;
        }

        public CommunityEntry getEntry() { return entry; }
        public double getHybridScore() { return hybridScore; }
        public double getSemanticScore() { return semanticScore; }
        public double getPopularityScore() { return popularityScore; }


    }



    @Override
    public org.springframework.data.domain.Page<Map<String, Object>> semanticSearchAuthors(String query, int page, int size) {

        // 1. 调用嵌入服务获取向量
        float[] vector = embeddingService.embed(query);
        if (vector == null || vector.length == 0) {
            return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }

        // 2. 构造 KNN 查询
        List<Double> vecList = new ArrayList<>(vector.length);
        for (float v : vector) vecList.add((double) v);

        Script script = Script.of(s -> s.inline(i -> i
                .lang("painless")
                .source("cosineSimilarity(params.q,'vector')+1.0")
                .params("q", JsonData.of(vecList))
        ));

        try {
            // 获取所有语义搜索结果用于混合排序
            SearchResponse<Map> allResultsResponse = esClient.search(s -> s
                    .index("authors")
                    .size(400) // 获取足够多的结果用于混合排序
                    .query(q -> q.scriptScore(ss -> ss
                            .script(script)
                            .query(m -> m.matchAll(ma -> ma))
                    )) , Map.class);

            // 提取作者ID和相似度分数，按搜索顺序排序
            List<AuthorSearchResult> authorSearchResults = new ArrayList<>();
            for (var hit : allResultsResponse.hits().hits()) {
                try {
                    var source = hit.source();
                    if (source != null) {
                        Long authorId = Long.parseLong(hit.id());
                        double semanticScore = hit.score() != null ? hit.score() : 0.0;
                        authorSearchResults.add(new AuthorSearchResult(authorId, semanticScore));
                    }
                } catch (Exception e) {
                    System.err.println("处理作者信息失败: " + e.getMessage());
                }
            }

            if (authorSearchResults.isEmpty()) {
                return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
            }

            // 3. 为每个作者获取其公开的社区条目
            List<CommunityEntry> allAuthorEntries = new ArrayList<>();
            for (AuthorSearchResult authorResult : authorSearchResults) {
                try {
                    List<CommunityEntry> authorEntries = communityEntryRepository.findPublicEntriesByUserId(authorResult.getAuthorId());
                    // 为每个条目添加作者搜索分数，用于后续排序
                    for (CommunityEntry entry : authorEntries) {
                        allAuthorEntries.add(entry);
                    }
                } catch (Exception e) {
                    System.err.println("获取作者 " + authorResult.getAuthorId() + " 的社区条目失败: " + e.getMessage());
                }
            }

            if (allAuthorEntries.isEmpty()) {
                return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
            }

            // 4. 转换为Map格式并保持作者搜索顺序
            List<Map<String, Object>> resultMaps = new ArrayList<>();
            for (CommunityEntry entry : allAuthorEntries) {
                try {
                    Map<String, Object> entryMap = communityService.getCommunityEntryByShareCode(entry.getShareCode());
                    if (entryMap != null) {
                        // 添加作者搜索相关信息
                        Long userId = entry.getItinerary().getUser().getId();
                        double authorScore = getAuthorSearchScore(userId, authorSearchResults);
                        entryMap.put("author_search_score", authorScore);
                        resultMaps.add(entryMap);
                    }
                } catch (Exception e) {
                    System.err.println("转换社区条目失败: " + e.getMessage());
                }
            }

            // 5. 按作者搜索分数和条目创建时间排序
            resultMaps.sort((a, b) -> {
                Double scoreA = (Double) a.get("author_search_score");
                Double scoreB = (Double) b.get("author_search_score");
                
                if (scoreA != null && scoreB != null) {
                    int scoreCompare = Double.compare(scoreB, scoreA); // 降序
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                }
                
                // 如果作者搜索分数相同，按条目创建时间排序
                Object createdAtA = a.get("createdAt");
                Object createdAtB = b.get("createdAt");
                if (createdAtA != null && createdAtB != null) {
                    // 安全地比较时间，支持不同的时间格式
                    try {
                        if (createdAtA instanceof String && createdAtB instanceof String) {
                            return ((String) createdAtB).compareTo((String) createdAtA); // 降序
                        } else if (createdAtA instanceof java.time.LocalDateTime && createdAtB instanceof java.time.LocalDateTime) {
                            return ((java.time.LocalDateTime) createdAtB).compareTo((java.time.LocalDateTime) createdAtA); // 降序
                        } else {
                            // 如果类型不匹配，转换为字符串比较
                            String timeA = createdAtA.toString();
                            String timeB = createdAtB.toString();
                            return timeB.compareTo(timeA); // 降序
                        }
                    } catch (Exception e) {
                        System.err.println("时间比较失败: " + e.getMessage());
                        return 0;
                    }
                }
                
                return 0;
            });

            // 6. 手动分页
            int totalElements = resultMaps.size();
            int start = page * size;
            int end = Math.min(start + size, totalElements);
            
            List<Map<String, Object>> pageContent = start >= totalElements ? 
                    Collections.emptyList() : 
                    resultMaps.subList(start, end);

            // 7. 创建分页结果
            org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page, size);
            return new org.springframework.data.domain.PageImpl<>(pageContent, pageRequest, totalElements);
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }
    }


    
    /**
     * 获取作者在搜索结果中的分数
     */
    private double getAuthorSearchScore(Long authorId, List<AuthorSearchResult> authorSearchResults) {

        return authorSearchResults.stream()
                .filter(result -> result.getAuthorId().equals(authorId))
                .findFirst()
                .map(AuthorSearchResult::getSemanticScore)
                .orElse(0.0);


    }
    
    /**
     * 作者搜索结果内部类
     */
    private static class AuthorSearchResult {

        private final Long authorId;
        private final double semanticScore;

        public AuthorSearchResult(Long authorId, double semanticScore) {
            this.authorId = authorId;
            this.semanticScore = semanticScore;
        }

        public Long getAuthorId() { return authorId; }
        public double getSemanticScore() { return semanticScore; }


    }

    @Override
    public org.springframework.data.domain.Page<Map<String, Object>> filterCommunityEntriesByTags(List<String> tags, int page, int size) {
        if (tags == null || tags.isEmpty()) {
            return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }
        
        try {
            // 查询所有公开社区条目
            List<CommunityEntry> allEntries = communityEntryRepository.findAllPublic();
            
            if (allEntries.isEmpty()) {
                return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
            }
            
            // 批量获取所有条目的标签，减少数据库查询次数
            Map<Long, List<String>> entryTagsMap = new HashMap<>();
            for (CommunityEntry entry : allEntries) {
                List<String> entryTags = getCommunityEntryTags(entry);
                entryTagsMap.put(entry.getId(), entryTags);
            }
            
            // 过滤包含所有标签的条目
            List<CommunityEntry> filtered = allEntries.stream()
                .filter(entry -> {
                    List<String> entryTags = entryTagsMap.get(entry.getId());
                    if (entryTags == null || entryTags.isEmpty()) {
                        return false;
                    }
                    return tags.stream().allMatch(entryTags::contains);
                })
                .sorted((a, b) -> Integer.compare(
                    b.getViewCount() != null ? b.getViewCount() : 0,
                    a.getViewCount() != null ? a.getViewCount() : 0
                ))
                .collect(Collectors.toList());
            
            // 分页
            int total = filtered.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            
            List<Map<String, Object>> resultMaps = new ArrayList<>();
            if (start < end) {
                for (CommunityEntry entry : filtered.subList(start, end)) {
                    try {
                        Map<String, Object> entryMap = communityService.getCommunityEntryByShareCode(entry.getShareCode());
                        if (entryMap != null) {
                            resultMaps.add(entryMap);
                        }
                    } catch (Exception e) {
                        //System.err.println("获取社区条目详情失败: " + e.getMessage());
                        // 继续处理其他条目，不因为单个条目失败而中断
                    }
                }
            }
            
            return new org.springframework.data.domain.PageImpl<>(resultMaps, org.springframework.data.domain.PageRequest.of(page, size), total);
        } catch (Exception e) {

            System.err.println("按标签筛选社区条目失败: " + e.getMessage());
            e.printStackTrace();
            return org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(page, size));
        }
    }
} 
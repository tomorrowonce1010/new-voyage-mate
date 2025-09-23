package com.se_07.backend.service.impl;

import com.se_07.backend.entity.CommunityEntry;
import com.se_07.backend.entity.CommunityEntryTag;
import com.se_07.backend.repository.CommunityEntryRepository;
import com.se_07.backend.repository.CommunityEntryTagRepository;
import com.se_07.backend.service.CommunityService;
import com.se_07.backend.dto.converter.ItineraryConverter;
import com.se_07.backend.dto.ItineraryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CommunityServiceImpl implements CommunityService {
    
    private static final Logger logger = LoggerFactory.getLogger(CommunityServiceImpl.class);
    
    @Autowired
    private CommunityEntryRepository communityEntryRepository;
    
    @Autowired
    private CommunityEntryTagRepository communityEntryTagRepository;
    
    @Autowired
    private ItineraryConverter itineraryConverter;
    
    @Override
    public List<Map<String, Object>> getPublicCommunityEntries() {
        try {
            List<CommunityEntry> entries = communityEntryRepository.findPublicEntries();
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (CommunityEntry entry : entries) {
                Map<String, Object> entryMap = new HashMap<>();
                entryMap.put("id", entry.getId());
                entryMap.put("shareCode", entry.getShareCode());
                entryMap.put("description", entry.getDescription());
                entryMap.put("viewCount", entry.getViewCount());
                entryMap.put("createdAt", entry.getCreatedAt());
                
                // 获取标签
                List<CommunityEntryTag> tagLinks = communityEntryTagRepository.findByCommunityEntry(entry);
                List<String> tagNames = new ArrayList<>();
                for (CommunityEntryTag link : tagLinks) {
                    if (link.getTag() != null) {
                        tagNames.add(link.getTag().getTag());
                    }
                }
                entryMap.put("tags", tagNames);
                
                // 添加行程信息
                if (entry.getItinerary() != null) {
                    // 使用ItineraryConverter转换为DTO，然后提取需要的信息
                    ItineraryDTO itineraryDTO = itineraryConverter.toDTO(entry.getItinerary());
                    
                    Map<String, Object> itineraryMap = new HashMap<>();
                    itineraryMap.put("id", itineraryDTO.getId());
                    itineraryMap.put("title", itineraryDTO.getTitle());
                    itineraryMap.put("startDate", itineraryDTO.getStartDate());
                    itineraryMap.put("endDate", itineraryDTO.getEndDate());
                    itineraryMap.put("coverImageUrl", itineraryDTO.getImageUrl());
                    
                    // 处理目的地信息
                    if (itineraryDTO.getDestinationNames() != null && !itineraryDTO.getDestinationNames().isEmpty()) {
                        itineraryMap.put("destination", String.join("、", itineraryDTO.getDestinationNames()));
                    } else {
                        itineraryMap.put("destination", "待规划目的地");
                    }
                    
                    // 添加用户信息
                    if (entry.getItinerary().getUser() != null) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", entry.getItinerary().getUser().getId());
                        userMap.put("username", entry.getItinerary().getUser().getUsername());
                        itineraryMap.put("user", userMap);
                    }
                    
                    entryMap.put("itinerary", itineraryMap);
                }
                
                result.add(entryMap);
            }
            
            return result;
        } catch (Exception e) {
            logger.error("获取公共社区条目失败: {}", e.getMessage());
            throw new RuntimeException("获取公共社区条目失败", e);
        }
    }
    
    @Override
    public Map<String, Object> getCommunityEntryByShareCode(String shareCode) {
        try {
            Optional<CommunityEntry> entryOpt = communityEntryRepository.findByShareCode(shareCode);
            if (!entryOpt.isPresent()) {
                return null;
            }
            
            CommunityEntry entry = entryOpt.get();
            
            // 自动增加查看次数
            entry.setViewCount(entry.getViewCount() + 1);
            communityEntryRepository.save(entry);
            
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("id", entry.getId());
            entryMap.put("shareCode", entry.getShareCode());
            entryMap.put("description", entry.getDescription());
            entryMap.put("viewCount", entry.getViewCount());
            entryMap.put("createdAt", entry.getCreatedAt());
            
            // 获取标签
            List<CommunityEntryTag> tagLinks = communityEntryTagRepository.findByCommunityEntry(entry);
            List<String> tagNames = new ArrayList<>();
            for (CommunityEntryTag link : tagLinks) {
                if (link.getTag() != null) {
                    tagNames.add(link.getTag().getTag());
                }
            }
            entryMap.put("tags", tagNames);
            
            // 添加行程信息
            if (entry.getItinerary() != null) {
                // 使用ItineraryConverter转换为DTO，然后提取需要的信息
                ItineraryDTO itineraryDTO = itineraryConverter.toDTO(entry.getItinerary());
                
                Map<String, Object> itineraryMap = new HashMap<>();
                itineraryMap.put("id", itineraryDTO.getId());
                itineraryMap.put("title", itineraryDTO.getTitle());
                itineraryMap.put("startDate", itineraryDTO.getStartDate());
                itineraryMap.put("endDate", itineraryDTO.getEndDate());
                itineraryMap.put("coverImageUrl", itineraryDTO.getImageUrl());
                itineraryMap.put("permissionStatus", itineraryDTO.getPermissionStatus());
                
                // 处理目的地信息
                if (itineraryDTO.getDestinationNames() != null && !itineraryDTO.getDestinationNames().isEmpty()) {
                    itineraryMap.put("destination", String.join("、", itineraryDTO.getDestinationNames()));
                } else {
                    itineraryMap.put("destination", "待规划目的地");
                }
                
                // 添加用户信息
                if (entry.getItinerary().getUser() != null) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", entry.getItinerary().getUser().getId());
                    userMap.put("username", entry.getItinerary().getUser().getUsername());
                    itineraryMap.put("user", userMap);
                }
                
                entryMap.put("itinerary", itineraryMap);
            }
            
            return entryMap;
        } catch (Exception e) {
            logger.error("根据分享码获取社区条目失败: {}", e.getMessage());
            throw new RuntimeException("根据分享码获取社区条目失败", e);
        }
    }
    
    @Override
    public List<Map<String, Object>> searchCommunityEntries(String searchTerm) {
        try {
            List<CommunityEntry> entries = communityEntryRepository.searchPublicEntries(searchTerm);
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (CommunityEntry entry : entries) {
                Map<String, Object> entryMap = new HashMap<>();
                entryMap.put("id", entry.getId());
                entryMap.put("shareCode", entry.getShareCode());
                entryMap.put("description", entry.getDescription());
                entryMap.put("viewCount", entry.getViewCount());
                entryMap.put("createdAt", entry.getCreatedAt());
                
                // 获取标签
                List<CommunityEntryTag> tagLinks = communityEntryTagRepository.findByCommunityEntry(entry);
                List<String> tagNames = new ArrayList<>();
                for (CommunityEntryTag link : tagLinks) {
                    if (link.getTag() != null) {
                        tagNames.add(link.getTag().getTag());
                    }
                }
                entryMap.put("tags", tagNames);
                
                // 添加行程信息
                if (entry.getItinerary() != null) {
                    // 使用ItineraryConverter转换为DTO，然后提取需要的信息
                    ItineraryDTO itineraryDTO = itineraryConverter.toDTO(entry.getItinerary());
                    
                    Map<String, Object> itineraryMap = new HashMap<>();
                    itineraryMap.put("id", itineraryDTO.getId());
                    itineraryMap.put("title", itineraryDTO.getTitle());
                    itineraryMap.put("startDate", itineraryDTO.getStartDate());
                    itineraryMap.put("endDate", itineraryDTO.getEndDate());
                    itineraryMap.put("coverImageUrl", itineraryDTO.getImageUrl());
                    
                    // 处理目的地信息
                    if (itineraryDTO.getDestinationNames() != null && !itineraryDTO.getDestinationNames().isEmpty()) {
                        itineraryMap.put("destination", String.join("、", itineraryDTO.getDestinationNames()));
                    } else {
                        itineraryMap.put("destination", "待规划目的地");
                    }
                    
                    // 添加用户信息
                    if (entry.getItinerary().getUser() != null) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", entry.getItinerary().getUser().getId());
                        userMap.put("username", entry.getItinerary().getUser().getUsername());
                        itineraryMap.put("user", userMap);
                    }
                    
                    entryMap.put("itinerary", itineraryMap);
                }
                
                result.add(entryMap);
            }
            
            return result;
        } catch (Exception e) {
            logger.error("搜索社区条目失败: {}", e.getMessage());
            throw new RuntimeException("搜索社区条目失败", e);
        }
    }
    
    @Override
    public void incrementViewCount(Long entryId) {
        try {
            Optional<CommunityEntry> entryOpt = communityEntryRepository.findById(entryId);
            if (entryOpt.isPresent()) {
                CommunityEntry entry = entryOpt.get();
                entry.setViewCount(entry.getViewCount() + 1);
                communityEntryRepository.save(entry);
                logger.info("社区条目 {} 的查看次数已增加", entryId);
            } else {
                logger.warn("社区条目 {} 不存在", entryId);
            }
        } catch (Exception e) {
            logger.error("增加查看次数失败: {}", e.getMessage());
            throw new RuntimeException("增加查看次数失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> getPopularTags(int limit) {
        List<Object[]> rows = communityEntryTagRepository.findTagPopularity();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            if (result.size() >= limit) break;
            Map<String, Object> map = new HashMap<>();
            map.put("tag", row[0]);
            map.put("count", ((Number) row[1]).longValue());
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getPopularAuthors(int limit) {
        List<Object[]> rows = communityEntryRepository.findAuthorPopularity();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            if (result.size() >= limit) break;
            Map<String, Object> map = new HashMap<>();
            map.put("userId", row[0]);
            map.put("username", row[1]);
            map.put("totalViews", ((Number) row[2]).longValue());
            result.add(map);
        }
        return result;
    }
    
    @Override
    public Map<String, Object> searchCommunityEntriesByDestination(String destination, int page, int size) {
        try {
            // 计算偏移量
            int offset = page * size;
            
            // 获取总数
            long total = communityEntryRepository.countByDestination(destination);
            
            // 获取分页数据
            List<CommunityEntry> entries = communityEntryRepository.findByDestination(destination, offset, size);
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (CommunityEntry entry : entries) {
                Map<String, Object> entryMap = new HashMap<>();
                entryMap.put("id", entry.getId());
                entryMap.put("shareCode", entry.getShareCode());
                entryMap.put("description", entry.getDescription());
                entryMap.put("viewCount", entry.getViewCount());
                entryMap.put("createdAt", entry.getCreatedAt());
                
                // 获取标签
                List<CommunityEntryTag> tagLinks = communityEntryTagRepository.findByCommunityEntry(entry);
                List<String> tagNames = new ArrayList<>();
                for (CommunityEntryTag link : tagLinks) {
                    if (link.getTag() != null) {
                        tagNames.add(link.getTag().getTag());
                    }
                }
                entryMap.put("tags", tagNames);
                
                // 添加行程信息
                if (entry.getItinerary() != null) {
                    ItineraryDTO itineraryDTO = itineraryConverter.toDTO(entry.getItinerary());
                    
                    Map<String, Object> itineraryMap = new HashMap<>();
                    itineraryMap.put("id", itineraryDTO.getId());
                    itineraryMap.put("title", itineraryDTO.getTitle());
                    itineraryMap.put("startDate", itineraryDTO.getStartDate());
                    itineraryMap.put("endDate", itineraryDTO.getEndDate());
                    itineraryMap.put("coverImageUrl", itineraryDTO.getImageUrl());
                    
                    // 处理目的地信息
                    if (itineraryDTO.getDestinationNames() != null && !itineraryDTO.getDestinationNames().isEmpty()) {
                        itineraryMap.put("destination", String.join("、", itineraryDTO.getDestinationNames()));
                    } else {
                        itineraryMap.put("destination", "待规划目的地");
                    }
                    
                    // 添加用户信息
                    if (entry.getItinerary().getUser() != null) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", entry.getItinerary().getUser().getId());
                        userMap.put("username", entry.getItinerary().getUser().getUsername());
                        itineraryMap.put("user", userMap);
                    }
                    
                    entryMap.put("itinerary", itineraryMap);
                }
                
                result.add(entryMap);
            }
            
            // 构建分页响应
            Map<String, Object> response = new HashMap<>();
            response.put("content", result);
            response.put("totalElements", total);
            response.put("totalPages", (int) Math.ceil((double) total / size));
            response.put("currentPage", page);
            response.put("size", size);
            
            return response;
        } catch (Exception e) {
            logger.error("按目的地搜索社区条目失败: {}", e.getMessage());
            throw new RuntimeException("按目的地搜索社区条目失败", e);
        }
    }
    
    @Override
    public Map<String, Object> getPublicCommunityEntriesWithSort(String sortBy, int page, int size) {
        try {
            org.springframework.data.domain.PageRequest pageRequest = 
                org.springframework.data.domain.PageRequest.of(page, size);
            
            org.springframework.data.domain.Page<CommunityEntry> entriesPage;
            
            if ("popularity".equals(sortBy)) {
                // 按热度排序
                entriesPage = communityEntryRepository.findAllPublicByPopularity(pageRequest);
            } else {
                // 默认按时间排序
                entriesPage = communityEntryRepository.findAllPublicByTime(pageRequest);
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (CommunityEntry entry : entriesPage.getContent()) {
                Map<String, Object> entryMap = new HashMap<>();
                entryMap.put("id", entry.getId());
                entryMap.put("shareCode", entry.getShareCode());
                entryMap.put("description", entry.getDescription());
                entryMap.put("viewCount", entry.getViewCount());
                entryMap.put("createdAt", entry.getCreatedAt());
                
                // 获取标签
                List<CommunityEntryTag> tagLinks = communityEntryTagRepository.findByCommunityEntry(entry);
                List<String> tagNames = new ArrayList<>();
                for (CommunityEntryTag link : tagLinks) {
                    if (link.getTag() != null) {
                        tagNames.add(link.getTag().getTag());
                    }
                }
                entryMap.put("tags", tagNames);
                
                // 添加行程信息
                if (entry.getItinerary() != null) {
                    // 使用ItineraryConverter转换为DTO，然后提取需要的信息
                    ItineraryDTO itineraryDTO = itineraryConverter.toDTO(entry.getItinerary());
                    
                    Map<String, Object> itineraryMap = new HashMap<>();
                    itineraryMap.put("id", itineraryDTO.getId());
                    itineraryMap.put("title", itineraryDTO.getTitle());
                    itineraryMap.put("startDate", itineraryDTO.getStartDate());
                    itineraryMap.put("endDate", itineraryDTO.getEndDate());
                    itineraryMap.put("coverImageUrl", itineraryDTO.getImageUrl());
                    
                    // 处理目的地信息
                    if (itineraryDTO.getDestinationNames() != null && !itineraryDTO.getDestinationNames().isEmpty()) {
                        itineraryMap.put("destination", String.join("、", itineraryDTO.getDestinationNames()));
                    } else {
                        itineraryMap.put("destination", "待规划目的地");
                    }
                    
                    // 添加用户信息
                    if (entry.getItinerary().getUser() != null) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", entry.getItinerary().getUser().getId());
                        userMap.put("username", entry.getItinerary().getUser().getUsername());
                        itineraryMap.put("user", userMap);
                    }
                    
                    entryMap.put("itinerary", itineraryMap);
                }
                
                result.add(entryMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", result);
            response.put("totalElements", entriesPage.getTotalElements());
            response.put("totalPages", entriesPage.getTotalPages());
            response.put("currentPage", entriesPage.getNumber());
            response.put("size", entriesPage.getSize());
            response.put("sortBy", sortBy);
            
            return response;
        } catch (Exception e) {
            logger.error("获取排序社区条目失败: {}", e.getMessage());
            throw new RuntimeException("获取排序社区条目失败", e);
        }
    }
} 
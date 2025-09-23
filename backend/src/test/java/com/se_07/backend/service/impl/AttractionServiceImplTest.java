package com.se_07.backend.service.impl;

import com.se_07.backend.entity.Attraction;
import com.se_07.backend.entity.Tag;
import com.se_07.backend.repository.AttractionRepository;
import com.se_07.backend.repository.TagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AttractionServiceImplTest {
    @Mock AttractionRepository attractionRepository;
    @Mock TagRepository tagRepository;
    ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks AttractionServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        java.lang.reflect.Field field = AttractionServiceImpl.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(service, objectMapper);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getAttractionById_found() {
        Attraction a = new Attraction(); a.setId(1L);
        when(attractionRepository.findById(1L)).thenReturn(Optional.of(a));
        Optional<Attraction> result = service.getAttractionById(1L);
        assertTrue(result.isPresent());
    }
    @Test
    void getAttractionById_notFound() {
        when(attractionRepository.findById(2L)).thenReturn(Optional.empty());
        assertFalse(service.getAttractionById(2L).isPresent());
    }
    @Test
    void getAttractionsByDestinationId() {
        Page<Attraction> page = new PageImpl<>(List.of(new Attraction()));
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L, PageRequest.of(0, 10))).thenReturn(page);
        assertEquals(1, service.getAttractionsByDestinationId(1L, PageRequest.of(0, 10)).getTotalElements());
    }
    @Test
    void getTopNTags_nullOrEmpty() {
        assertTrue(service.getTopNTags(null, 3).isEmpty());
        Attraction a = new Attraction();
        a.setTagScores(null);
        assertTrue(service.getTopNTags(a, 3).isEmpty());
        a.setTagScores("");
        assertTrue(service.getTopNTags(a, 3).isEmpty());
    }
    @Test
    void getTopNTags_jsonObject() throws Exception {
        Attraction a = new Attraction();
        a.setTagScores("{\"7\":37,\"12\":10}");
        Tag t7 = new Tag(); t7.setId(7L); t7.setTag("美食");
        Tag t12 = new Tag(); t12.setId(12L); t12.setTag("自然");
        when(tagRepository.findAllById(Arrays.asList(7L,12L))).thenReturn(Arrays.asList(t7, t12));
        List<String> tags = service.getTopNTags(a, 2);
        assertTrue(tags.contains("美食") && tags.contains("自然"));
    }
    @Test
    void getTopNTags_jsonArray() throws Exception {
        Attraction a = new Attraction();
        a.setTagScores("[0,0,0,37,0,0,10]");
        Tag t4 = new Tag(); t4.setId(4L); t4.setTag("历史");
        Tag t7 = new Tag(); t7.setId(7L); t7.setTag("自然");
        when(tagRepository.findAllById(Arrays.asList(4L,7L))).thenReturn(Arrays.asList(t4, t7));
        List<String> tags = service.getTopNTags(a, 2);
        assertTrue(tags.contains("历史") && tags.contains("自然"));
    }
    @Test
    void getTopNTags_jsonParseError() throws Exception {
        Attraction a = new Attraction();
        a.setTagScores("bad json");
        assertTrue(service.getTopNTags(a, 2).isEmpty());
    }
    @Test
    void getAttractionsByCategory() {
        Page<Attraction> page = new PageImpl<>(List.of(new Attraction()));
        when(attractionRepository.findByCategoryOrderByJoinCountDesc(Attraction.AttractionCategory.旅游景点, PageRequest.of(0, 5))).thenReturn(page);
        assertEquals(1, service.getAttractionsByCategory(Attraction.AttractionCategory.旅游景点, PageRequest.of(0, 5)).getTotalElements());
    }
    @Test
    void searchAttractions_keywordNullOrEmpty() {
        assertTrue(service.searchAttractions(null).isEmpty());
        assertTrue(service.searchAttractions("").isEmpty());
    }
    @Test
    void searchAttractions_fulltextSuccess() {
        Attraction a = new Attraction();
        when(attractionRepository.searchByKeyword("美食")).thenReturn(List.of(a));
        assertEquals(1, service.searchAttractions("美食").size());
    }
    @Test
    void searchAttractions_fulltextFailFallback() {
        when(attractionRepository.searchByKeyword("test")).thenThrow(new RuntimeException("fail"));
        when(attractionRepository.findByNameContainingOrDescriptionContaining("test")).thenReturn(List.of(new Attraction()));
        assertEquals(1, service.searchAttractions("test").size());
    }
    @Test
    void incrementJoinCount_found() {
        Attraction a = new Attraction(); a.setId(1L); a.setJoinCount(2);
        when(attractionRepository.findById(1L)).thenReturn(Optional.of(a));
        when(attractionRepository.save(any())).thenReturn(a);
        service.incrementJoinCount(1L);
        assertEquals(3, a.getJoinCount());
    }
    @Test
    void incrementJoinCount_notFound() {
        when(attractionRepository.findById(2L)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.incrementJoinCount(2L));
    }
    @Test
    void getHotAttractions() {
        Page<Attraction> page = new PageImpl<>(List.of(new Attraction()));
        when(attractionRepository.findAllByOrderByJoinCountDesc(PageRequest.of(0, 3))).thenReturn(page);
        assertEquals(1, service.getHotAttractions(PageRequest.of(0, 3)).getTotalElements());
    }
    @Test
    void searchAttractionsByName_nullOrEmpty() {
        assertTrue(service.searchAttractionsByName(null, 5).isEmpty());
        assertTrue(service.searchAttractionsByName("", 5).isEmpty());
    }
    @Test
    void searchAttractionsByName_limitNullOrZero() {
        Page<Attraction> page = new PageImpl<>(List.of(new Attraction()));
        when(attractionRepository.findByNameContainingIgnoreCaseOrderByJoinCountDesc(eq("test"), any(Pageable.class))).thenReturn(page);
        assertEquals(1, service.searchAttractionsByName("test", null).size());
        assertEquals(1, service.searchAttractionsByName("test", 0).size());
    }
    @Test
    void searchAttractionsByName_normal() {
        Page<Attraction> page = new PageImpl<>(List.of(new Attraction()));
        when(attractionRepository.findByNameContainingIgnoreCaseOrderByJoinCountDesc(eq("test"), any(Pageable.class))).thenReturn(page);
        assertEquals(1, service.searchAttractionsByName("test", 2).size());
    }
    @Test
    void getAttractionsByDestinationWithFilters_noTagKeyword() {
        Attraction a1 = new Attraction(); a1.setName("美食天堂");
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Arrays.asList(a1));
        Pageable pageable = PageRequest.of(0, 1);
        assertEquals(1, service.getAttractionsByDestinationWithFilters(1L, null, null, pageable).getTotalElements());
    }
    @Test
    void getAttractionsByDestinationWithFilters_pagination() {
        List<Attraction> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) { Attraction a = new Attraction(); a.setName("A"+i); list.add(a); }
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(list);
        Pageable pageable = PageRequest.of(1, 2); // 第二页
        assertEquals(2, service.getAttractionsByDestinationWithFilters(1L, null, null, pageable).getContent().size());
    }
    @Test
    void getAttractionsByDestinationWithFilters_tagOnly() throws Exception {
        Attraction a1 = new Attraction(); a1.setName("美食天堂");
        Attraction a2 = new Attraction(); a2.setName("自然风光");
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Arrays.asList(a1, a2));
        AttractionServiceImpl testService = new AttractionServiceImpl() {
            @Override
            public List<String> getTopNTags(Attraction a, int n) {
                if ("美食天堂".equals(a.getName())) return List.of("美食");
                if ("自然风光".equals(a.getName())) return List.of("自然");
                return List.of();
            }
        };
        // 注入依赖
        java.lang.reflect.Field field = AttractionServiceImpl.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(testService, objectMapper);
        java.lang.reflect.Field repoField = AttractionServiceImpl.class.getDeclaredField("attractionRepository");
        repoField.setAccessible(true);
        repoField.set(testService, attractionRepository);
        Pageable pageable = PageRequest.of(0, 2);
        assertEquals(1, testService.getAttractionsByDestinationWithFilters(1L, "美食", null, pageable).getTotalElements());
    }
    @Test
    void getAttractionsByDestinationWithFilters_keywordOnly() {
        Attraction a1 = new Attraction(); a1.setName("美食天堂"); a1.setDescription("好吃");
        Attraction a2 = new Attraction(); a2.setName("自然风光"); a2.setDescription("风景");
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Arrays.asList(a1, a2));
        Pageable pageable = PageRequest.of(0, 2);
        assertEquals(1, service.getAttractionsByDestinationWithFilters(1L, null, "天堂", pageable).getTotalElements());
    }
    @Test
    void getAttractionsByDestinationWithFilters_tagAndKeyword() throws Exception {
        Attraction a1 = new Attraction(); a1.setName("美食天堂"); a1.setDescription("好吃");
        Attraction a2 = new Attraction(); a2.setName("自然风光"); a2.setDescription("风景");
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Arrays.asList(a1, a2));
        AttractionServiceImpl testService = new AttractionServiceImpl() {
            @Override
            public List<String> getTopNTags(Attraction a, int n) {
                if ("美食天堂".equals(a.getName())) return List.of("美食");
                if ("自然风光".equals(a.getName())) return List.of("自然");
                return List.of();
            }
        };
        java.lang.reflect.Field field = AttractionServiceImpl.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(testService, objectMapper);
        java.lang.reflect.Field repoField = AttractionServiceImpl.class.getDeclaredField("attractionRepository");
        repoField.setAccessible(true);
        repoField.set(testService, attractionRepository);
        Pageable pageable = PageRequest.of(0, 1);
        assertEquals(1, testService.getAttractionsByDestinationWithFilters(1L, "美食", "天堂", pageable).getTotalElements());
    }
    @Test
    void getAttractionsByDestinationWithFilters_noResult() {
        Attraction a1 = new Attraction(); a1.setName("美食天堂");
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Arrays.asList(a1));
        Pageable pageable = PageRequest.of(0, 1);
        assertEquals(0, service.getAttractionsByDestinationWithFilters(1L, "不存在", null, pageable).getTotalElements());
    }
    @Test
    void getAttractionsByDestinationWithFilters_paginationEdge() {
        List<Attraction> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) { Attraction a = new Attraction(); a.setName("A"+i); list.add(a); }
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(list);
        Pageable pageable = PageRequest.of(2, 2); // 超出范围
        assertEquals(0, service.getAttractionsByDestinationWithFilters(1L, null, null, pageable).getContent().size());
    }
}
package com.se_07.backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se_07.backend.entity.Destination;
import com.se_07.backend.entity.Tag;
import com.se_07.backend.repository.DestinationRepository;
import com.se_07.backend.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DestinationServiceImplTest {
    @Mock
    private DestinationRepository destinationRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private DestinationServiceImpl destinationService;

    private Destination destination;
    private Tag tag;
    private List<Destination> destinationList;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        destination = new Destination();
        destination.setId(1L);
        destination.setName("Shanghai");
        destination.setDescription("A city in China");
        destination.setJoinCount(10);
        destination.setTagScores("{\"1\": 2.5, \"2\": 1.0}");
        tag = new Tag();
        tag.setId(1L);
        tag.setTag("美食");
        destinationList = Arrays.asList(destination);
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void testGetHotDestinations() {
        Page<Destination> page = new PageImpl<>(destinationList);
        when(destinationRepository.findAllByOrderByJoinCountDesc(pageable)).thenReturn(page);
        Page<Destination> result = destinationService.getHotDestinations(pageable);
        assertEquals(1, result.getContent().size());
        verify(destinationRepository).findAllByOrderByJoinCountDesc(pageable);
    }

    @Test
    void testGetDestinationById_Found() {
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        Destination result = destinationService.getDestinationById(1L);
        assertNotNull(result);
        assertEquals("Shanghai", result.getName());
    }

    @Test
    void testGetDestinationById_NotFound() {
        when(destinationRepository.findById(2L)).thenReturn(Optional.empty());
        Destination result = destinationService.getDestinationById(2L);
        assertNull(result);
    }

    @Test
    void testSearchDestinations() {
        when(destinationRepository.searchByKeyword("shanghai")).thenReturn(destinationList);
        List<Destination> result = destinationService.searchDestinations("shanghai");
        assertEquals(1, result.size());
        verify(destinationRepository).searchByKeyword("shanghai");
    }

    @Test
    void testSearchDestinationsWithPagination() {
        Page<Destination> page = new PageImpl<>(destinationList);
        when(destinationRepository.searchByKeyword(eq("shanghai"), any(Pageable.class))).thenReturn(page);
        Page<Destination> result = destinationService.searchDestinations("shanghai", pageable);
        assertEquals(1, result.getContent().size());
        verify(destinationRepository).searchByKeyword(eq("shanghai"), any(Pageable.class));
    }

    @Test
    void testSearchDestinationsByName() {
        when(destinationRepository.findByNameContainingIgnoreCase("shanghai")).thenReturn(destinationList);
        List<Destination> result = destinationService.searchDestinationsByName("shanghai");
        assertEquals(1, result.size());
        verify(destinationRepository).findByNameContainingIgnoreCase("shanghai");
    }

    @Test
    void testIncrementJoinCount_Found() {
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(destination));
        when(destinationRepository.save(any(Destination.class))).thenReturn(destination);
        destinationService.incrementJoinCount(1L);
        assertEquals(11, destination.getJoinCount());
        verify(destinationRepository).save(destination);
    }

    @Test
    void testIncrementJoinCount_NotFound() {
        when(destinationRepository.findById(2L)).thenReturn(Optional.empty());
        destinationService.incrementJoinCount(2L);
        verify(destinationRepository, never()).save(any());
    }

    @Test
    void testGetAllTags() {
        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setTag("文化");
        when(tagRepository.findAllByOrderById()).thenReturn(Arrays.asList(tag, tag2));
        List<String> tags = destinationService.getAllTags();
        assertEquals(Arrays.asList("美食", "文化"), tags);
    }

    @Test
    void testGetTopNTags_Normal() throws Exception {
        destination.setTagScores("{\"1\": 2.5, \"2\": 1.0}");
        Map<String, Object> tagScores = new HashMap<>();
        tagScores.put("1", 2.5);
        tagScores.put("2", 1.0);
        when(objectMapper.readValue(eq(destination.getTagScores()), any(TypeReference.class))).thenReturn(tagScores);
        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setTag("文化");
        when(tagRepository.findAllById(Arrays.asList(1L, 2L))).thenReturn(Arrays.asList(tag, tag2));
        List<String> result = destinationService.getTopNTags(destination, 2);
        assertEquals(Arrays.asList("美食", "文化"), result);
    }

    @Test
    void testGetTopNTags_EmptyOrNull() throws Exception {
        destination.setTagScores(null);
        List<String> result = destinationService.getTopNTags(destination, 2);
        assertTrue(result.isEmpty());
        destination.setTagScores("");
        result = destinationService.getTopNTags(destination, 2);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTopNTags_JsonParseException() throws Exception {
        destination.setTagScores("invalid json");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenThrow(new RuntimeException("parse error"));
        List<String> result = destinationService.getTopNTags(destination, 2);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetDestinationsByTags_Normal() throws Exception {
        List<String> tags = Arrays.asList("美食", "文化");
        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setTag("文化");
        when(destinationRepository.findAll()).thenReturn(destinationList);
        when(tagRepository.findByTagIn(tags)).thenReturn(Arrays.asList(tag, tag2));
        Map<String, Object> tagScores = new HashMap<>();
        tagScores.put("1", 2.5);
        tagScores.put("2", 1.0);
        when(objectMapper.readValue(eq(destination.getTagScores()), any(TypeReference.class))).thenReturn(tagScores);
        List<Destination> result = destinationService.getDestinationsByTags(tags);
        assertEquals(1, result.size());
    }

    @Test
    void testGetDestinationsByTags_EmptyTags() {
        List<Destination> result = destinationService.getDestinationsByTags(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetDestinationsByTags_JsonParseException() throws Exception {
        List<String> tags = Arrays.asList("美食");
        when(destinationRepository.findAll()).thenReturn(destinationList);
        when(tagRepository.findByTagIn(tags)).thenReturn(Collections.singletonList(tag));
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenThrow(new RuntimeException("parse error"));
        List<Destination> result = destinationService.getDestinationsByTags(tags);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetDestinationsByTagsWithPageable() throws Exception {
        List<String> tags = Arrays.asList("美食");
        when(destinationRepository.findAll()).thenReturn(destinationList);
        when(tagRepository.findByTagIn(tags)).thenReturn(Collections.singletonList(tag));
        Map<String, Object> tagScores = new HashMap<>();
        tagScores.put("1", 2.5);
        when(objectMapper.readValue(eq(destination.getTagScores()), any(TypeReference.class))).thenReturn(tagScores);
        Page<Destination> result = destinationService.getDestinationsByTags(tags, pageable);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testGetDestinationsByTagsWithPageable_EmptyTags() {
        Page<Destination> result = destinationService.getDestinationsByTags(Collections.emptyList(), pageable);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void testSearchDestinationsByTagsAndKeyword_Normal() throws Exception {
        List<String> tags = Arrays.asList("美食");
        when(destinationRepository.findAll()).thenReturn(destinationList);
        when(tagRepository.findByTagIn(tags)).thenReturn(Collections.singletonList(tag));
        Map<String, Object> tagScores = new HashMap<>();
        tagScores.put("1", 2.5);
        when(objectMapper.readValue(eq(destination.getTagScores()), any(TypeReference.class))).thenReturn(tagScores);
        Page<Destination> result = destinationService.searchDestinationsByTagsAndKeyword(tags, "shanghai", pageable);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testSearchDestinationsByTagsAndKeyword_EmptyTags() {
        Page<Destination> result = destinationService.searchDestinationsByTagsAndKeyword(Collections.emptyList(), "shanghai", pageable);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void testSearchDestinationsByTagsAndKeyword_KeywordFilter() throws Exception {
        List<String> tags = Arrays.asList("美食");
        when(destinationRepository.findAll()).thenReturn(destinationList);
        when(tagRepository.findByTagIn(tags)).thenReturn(Collections.singletonList(tag));
        Map<String, Object> tagScores = new HashMap<>();
        tagScores.put("1", 2.5);
        when(objectMapper.readValue(eq(destination.getTagScores()), any(TypeReference.class))).thenReturn(tagScores);
        // 目的地名称不包含keyword
        destination.setName("Beijing");
        Page<Destination> result = destinationService.searchDestinationsByTagsAndKeyword(tags, "shanghai", pageable);
        assertEquals(0, result.getContent().size());
    }
} 
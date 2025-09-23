package com.se_07.backend.controller;

import com.se_07.backend.entity.Destination;
import com.se_07.backend.service.DestinationService;
import com.se_07.backend.service.SemanticSearchService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DestinationControllerTest {

    @Mock
    private DestinationService destinationService;

    @Mock
    private SemanticSearchService semanticSearchService;

    @InjectMocks
    private DestinationController destinationController;

    private Destination testDestination;
    private List<Destination> testDestinations;
    private Page<Destination> testDestinationPage;

    @BeforeEach
    void setUp() {
        // 创建测试目的地
        testDestination = new Destination();
        testDestination.setId(1L);
        testDestination.setName("北京");
        testDestination.setDescription("中国的首都");
        testDestination.setImageUrl("http://example.com/beijing.jpg");
        testDestination.setLatitude(new BigDecimal("39.9042"));
        testDestination.setLongitude(new BigDecimal("116.4074"));
        testDestination.setJoinCount(100);
        testDestination.setCreatedAt(LocalDateTime.now());
        testDestination.setUpdatedAt(LocalDateTime.now());
        testDestination.setTagScores("{\"文化\":0.8,\"历史\":0.9,\"美食\":0.7}");

        testDestinations = Arrays.asList(testDestination);
        testDestinationPage = new PageImpl<>(testDestinations, PageRequest.of(0, 10), 1);
    }

    @AfterEach
    void tearDown() {
        // 清理资源
    }

    @Test
    void getHotDestinations_ShouldReturnHotDestinations() {
        // Given
        when(destinationService.getHotDestinations(any(Pageable.class)))
                .thenReturn(testDestinationPage);

        // When
        ResponseEntity<Page<Destination>> response = destinationController.getHotDestinations(0, 10);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        assertEquals("北京", response.getBody().getContent().get(0).getName());
        
        verify(destinationService).getHotDestinations(any(Pageable.class));
    }

    @Test
    void getHotDestinations_WithCustomPageAndSize_ShouldReturnCorrectPage() {
        // Given
        when(destinationService.getHotDestinations(any(Pageable.class)))
                .thenReturn(testDestinationPage);

        // When
        ResponseEntity<Page<Destination>> response = destinationController.getHotDestinations(2, 5);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(destinationService).getHotDestinations(PageRequest.of(2, 5));
    }

    @Test
    void getDestinationById_ShouldReturnDestination() {
        // Given
        when(destinationService.getDestinationById(1L))
                .thenReturn(testDestination);

        // When
        ResponseEntity<Destination> response = destinationController.getDestinationById(1L);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("北京", response.getBody().getName());
        assertEquals(1L, response.getBody().getId());
        
        verify(destinationService).getDestinationById(1L);
    }

    @Test
    void getDestinationById_WithNonExistentId_ShouldReturnDestination() {
        // Given
        when(destinationService.getDestinationById(999L))
                .thenReturn(null);

        // When
        ResponseEntity<Destination> response = destinationController.getDestinationById(999L);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(destinationService).getDestinationById(999L);
    }

    @Test
    void searchDestinations_ShouldReturnSearchResults() {
        // Given
        when(destinationService.searchDestinations(eq("北京"), any(Pageable.class)))
                .thenReturn(testDestinationPage);

        // When
        ResponseEntity<Page<Destination>> response = destinationController.searchDestinations("北京", 0, 8);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        
        verify(destinationService).searchDestinations("北京", PageRequest.of(0, 8));
    }

    @Test
    void searchDestinations_WithCustomPageAndSize_ShouldReturnCorrectPage() {
        // Given
        when(destinationService.searchDestinations(eq("上海"), any(Pageable.class)))
                .thenReturn(testDestinationPage);

        // When
        ResponseEntity<Page<Destination>> response = destinationController.searchDestinations("上海", 1, 5);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(destinationService).searchDestinations("上海", PageRequest.of(1, 5));
    }

    @Test
    void getDestinationsByTags_ShouldReturnFilteredDestinations() {
        // Given
        List<String> tags = Arrays.asList("文化", "历史");
        when(destinationService.getDestinationsByTags(eq(tags), any(Pageable.class)))
                .thenReturn(testDestinationPage);

        // When
        ResponseEntity<Page<Destination>> response = destinationController.getDestinationsByTags(tags, 0, 8);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        
        verify(destinationService).getDestinationsByTags(tags, PageRequest.of(0, 8));
    }

    @Test
    void getDestinationsByTags_WithEmptyTags_ShouldReturnEmptyPage() {
        // Given
        List<String> tags = Collections.emptyList();
        Page<Destination> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 8), 0);
        when(destinationService.getDestinationsByTags(eq(tags), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        ResponseEntity<Page<Destination>> response = destinationController.getDestinationsByTags(tags, 0, 8);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getContent().size());
        
        verify(destinationService).getDestinationsByTags(tags, PageRequest.of(0, 8));
    }

    @Test
    void searchDestinationsByTagsAndKeyword_ShouldReturnCombinedSearchResults() {
        // Given
        List<String> tags = Arrays.asList("文化", "历史");
        when(destinationService.searchDestinationsByTagsAndKeyword(eq(tags), eq("北京"), any(Pageable.class)))
                .thenReturn(testDestinationPage);

        // When
        ResponseEntity<Page<Destination>> response = destinationController.searchDestinationsByTagsAndKeyword(tags, "北京", 0, 8);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        
        verify(destinationService).searchDestinationsByTagsAndKeyword(tags, "北京", PageRequest.of(0, 8));
    }

    @Test
    void searchDestinationsByTagsAndKeyword_WithEmptyTags_ShouldReturnEmptyPage() {
        // Given
        List<String> tags = Collections.emptyList();
        Page<Destination> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 8), 0);
        when(destinationService.searchDestinationsByTagsAndKeyword(eq(tags), eq("北京"), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        ResponseEntity<Page<Destination>> response = destinationController.searchDestinationsByTagsAndKeyword(tags, "北京", 0, 8);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getContent().size());
        
        verify(destinationService).searchDestinationsByTagsAndKeyword(tags, "北京", PageRequest.of(0, 8));
    }

    @Test
    void searchDestinationsByName_ShouldReturnMatchingDestinations() {
        // Given
        when(destinationService.searchDestinationsByName("北京"))
                .thenReturn(testDestinations);

        // When
        ResponseEntity<List<Destination>> response = destinationController.searchDestinationsByName("北京");

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("北京", response.getBody().get(0).getName());
        
        verify(destinationService).searchDestinationsByName("北京");
    }

    @Test
    void searchDestinationsByName_WithNoMatches_ShouldReturnEmptyList() {
        // Given
        when(destinationService.searchDestinationsByName("不存在的城市"))
                .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<Destination>> response = destinationController.searchDestinationsByName("不存在的城市");

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        
        verify(destinationService).searchDestinationsByName("不存在的城市");
    }

    @Test
    void semanticSearch_ShouldReturnSemanticSearchResults() {
        // Given
        when(semanticSearchService.semanticSearch("北京", 10))
                .thenReturn(testDestinations);

        // When
        ResponseEntity<List<Destination>> response = destinationController.semanticSearch("北京", 10);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("北京", response.getBody().get(0).getName());
        
        verify(semanticSearchService).semanticSearch("北京", 10);
    }

    @Test
    void semanticSearch_WithCustomSize_ShouldReturnCorrectNumberOfResults() {
        // Given
        when(semanticSearchService.semanticSearch("上海", 5))
                .thenReturn(testDestinations);

        // When
        ResponseEntity<List<Destination>> response = destinationController.semanticSearch("上海", 5);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(semanticSearchService).semanticSearch("上海", 5);
    }

    @Test
    void semanticSearch_WithNoResults_ShouldReturnEmptyList() {
        // Given
        when(semanticSearchService.semanticSearch("不存在的城市", 10))
                .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<Destination>> response = destinationController.semanticSearch("不存在的城市", 10);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        
        verify(semanticSearchService).semanticSearch("不存在的城市", 10);
    }

    @Test
    void semanticSearchByTags_ShouldReturnSemanticSearchResultsWithTags() {
        // Given
        List<String> tags = Arrays.asList("文化", "历史");
        when(semanticSearchService.semanticSearchByTags("北京", tags, 10))
                .thenReturn(testDestinations);

        // When
        ResponseEntity<List<Destination>> response = destinationController.semanticSearchByTags("北京", tags, 10);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("北京", response.getBody().get(0).getName());
        
        verify(semanticSearchService).semanticSearchByTags("北京", tags, 10);
    }

    @Test
    void semanticSearchByTags_WithEmptyTags_ShouldReturnResults() {
        // Given
        List<String> tags = Collections.emptyList();
        when(semanticSearchService.semanticSearchByTags("北京", tags, 10))
                .thenReturn(testDestinations);

        // When
        ResponseEntity<List<Destination>> response = destinationController.semanticSearchByTags("北京", tags, 10);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        
        verify(semanticSearchService).semanticSearchByTags("北京", tags, 10);
    }

    @Test
    void semanticSearchByTags_WithCustomSize_ShouldReturnCorrectNumberOfResults() {
        // Given
        List<String> tags = Arrays.asList("美食", "购物");
        when(semanticSearchService.semanticSearchByTags("上海", tags, 5))
                .thenReturn(testDestinations);

        // When
        ResponseEntity<List<Destination>> response = destinationController.semanticSearchByTags("上海", tags, 5);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(semanticSearchService).semanticSearchByTags("上海", tags, 5);
    }

    @Test
    void getAllTags_ShouldReturnAllTags() {
        // Given
        List<String> tags = Arrays.asList("文化", "历史", "美食", "购物", "自然", "娱乐");
        when(destinationService.getAllTags())
                .thenReturn(tags);

        // When
        ResponseEntity<List<String>> response = destinationController.getAllTags();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(6, response.getBody().size());
        assertTrue(response.getBody().contains("文化"));
        assertTrue(response.getBody().contains("历史"));
        
        verify(destinationService).getAllTags();
    }

    @Test
    void getAllTags_WithEmptyTags_ShouldReturnEmptyList() {
        // Given
        when(destinationService.getAllTags())
                .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<String>> response = destinationController.getAllTags();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        
        verify(destinationService).getAllTags();
    }

    @Test
    void getTopTags_ShouldReturnTopTags() {
        // Given
        List<String> topTags = Arrays.asList("文化", "历史", "美食");
        when(destinationService.getDestinationById(1L))
                .thenReturn(testDestination);
        when(destinationService.getTopNTags(testDestination, 6))
                .thenReturn(topTags);

        // When
        ResponseEntity<List<String>> response = destinationController.getTopTags(1L, 6);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        assertTrue(response.getBody().contains("文化"));
        assertTrue(response.getBody().contains("历史"));
        assertTrue(response.getBody().contains("美食"));
        
        verify(destinationService).getDestinationById(1L);
        verify(destinationService).getTopNTags(testDestination, 6);
    }

    @Test
    void getTopTags_WithNonExistentDestination_ShouldReturnNotFound() {
        // Given
        when(destinationService.getDestinationById(999L))
                .thenReturn(null);

        // When
        ResponseEntity<List<String>> response = destinationController.getTopTags(999L, 6);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(destinationService).getDestinationById(999L);
        verify(destinationService, never()).getTopNTags(any(), anyInt());
    }

    @Test
    void getTopTags_WithCustomCount_ShouldReturnCorrectNumberOfTags() {
        // Given
        List<String> topTags = Arrays.asList("文化", "历史");
        when(destinationService.getDestinationById(1L))
                .thenReturn(testDestination);
        when(destinationService.getTopNTags(testDestination, 3))
                .thenReturn(topTags);

        // When
        ResponseEntity<List<String>> response = destinationController.getTopTags(1L, 3);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        
        verify(destinationService).getDestinationById(1L);
        verify(destinationService).getTopNTags(testDestination, 3);
    }

    @Test
    void getTopTags_WithDefaultCount_ShouldReturnDefaultNumberOfTags() {
        // Given
        List<String> topTags = Arrays.asList("文化", "历史", "美食", "购物", "自然", "娱乐");
        when(destinationService.getDestinationById(1L))
                .thenReturn(testDestination);
        when(destinationService.getTopNTags(testDestination, 6))
                .thenReturn(topTags);

        // When
        ResponseEntity<List<String>> response = destinationController.getTopTags(1L, 6);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(6, response.getBody().size());
        
        verify(destinationService).getDestinationById(1L);
        verify(destinationService).getTopNTags(testDestination, 6);
    }
}
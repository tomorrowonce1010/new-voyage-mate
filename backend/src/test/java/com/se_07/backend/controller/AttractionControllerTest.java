package com.se_07.backend.controller;

import com.se_07.backend.entity.Attraction;
import com.se_07.backend.service.AttractionService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttractionControllerTest {

    @Mock
    private AttractionService attractionService;

    @InjectMocks
    private AttractionController attractionController;

    private Attraction attraction;
    private Page<Attraction> attractionPage;

    @BeforeEach
    void setUp() {
        attraction = new Attraction();
        attraction.setId(1L);
        attraction.setName("西湖景区");
        attraction.setDescription("杭州著名景点");

        List<Attraction> attractions = Collections.singletonList(attraction);
        attractionPage = new PageImpl<>(attractions);
    }

    @Test
    void getHotAttractions_ShouldReturnPageOfAttractions() {
        // 准备测试数据
        Pageable pageable = PageRequest.of(0, 10);
        when(attractionService.getHotAttractions(pageable)).thenReturn(attractionPage);

        // 执行方法
        ResponseEntity<Page<Attraction>> response = attractionController.getHotAttractions(0, 10);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        assertEquals("西湖景区", response.getBody().getContent().get(0).getName());

        // 验证服务调用
        verify(attractionService, times(1)).getHotAttractions(pageable);
    }


    @Test
    void getAttractionById_WhenExists_ShouldReturnAttraction() {
        // 准备测试数据
        when(attractionService.getAttractionById(1L)).thenReturn(Optional.of(attraction));

        // 执行方法
        ResponseEntity<Attraction> response = attractionController.getAttractionById(1L);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("西湖景区", response.getBody().getName());

        // 验证服务调用
        verify(attractionService, times(1)).getAttractionById(1L);
    }

    @Test
    void getAttractionById_WhenNotExists_ShouldReturnNotFound() {
        // 准备测试数据
        when(attractionService.getAttractionById(1L)).thenReturn(Optional.empty());

        // 执行方法
        ResponseEntity<Attraction> response = attractionController.getAttractionById(1L);

        // 验证结果
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        // 验证服务调用
        verify(attractionService, times(1)).getAttractionById(1L);
    }

    @Test
    void getAttractionsByDestinationId_WithoutFilters_ShouldReturnAttractions() {
        // 准备测试数据
        Pageable pageable = PageRequest.of(0, 8);
        when(attractionService.getAttractionsByDestinationWithFilters(eq(1L), isNull(), isNull(), eq(pageable)))
                .thenReturn(attractionPage);

        // 执行方法
        ResponseEntity<Page<Attraction>> response = attractionController.getAttractionsByDestinationId(
                1L, 0, 8, null, null);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());

        // 验证服务调用
        verify(attractionService, times(1))
                .getAttractionsByDestinationWithFilters(eq(1L), isNull(), isNull(), eq(pageable));
    }

    @Test
    void getAttractionsByDestinationId_WithTagFilter_ShouldReturnFilteredAttractions() {
        // 准备测试数据
        Pageable pageable = PageRequest.of(0, 8);
        when(attractionService.getAttractionsByDestinationWithFilters(eq(1L), eq("park"), isNull(), eq(pageable)))
                .thenReturn(attractionPage);

        // 执行方法
        ResponseEntity<Page<Attraction>> response = attractionController.getAttractionsByDestinationId(
                1L, 0, 8, "park", null);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());

        // 验证服务调用
        verify(attractionService, times(1))
                .getAttractionsByDestinationWithFilters(eq(1L), eq("park"), isNull(), eq(pageable));
    }

    @Test
    void getAttractionsByDestinationId_WithKeywordFilter_ShouldReturnFilteredAttractions() {
        // 准备测试数据
        Pageable pageable = PageRequest.of(0, 8);
        when(attractionService.getAttractionsByDestinationWithFilters(eq(1L), isNull(), eq("西湖"), eq(pageable)))
                .thenReturn(attractionPage);

        // 执行方法
        ResponseEntity<Page<Attraction>> response = attractionController.getAttractionsByDestinationId(
                1L, 0, 8, null, "西湖");

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());

        // 验证服务调用
        verify(attractionService, times(1))
                .getAttractionsByDestinationWithFilters(eq(1L), isNull(), eq("西湖"), eq(pageable));
    }


    @Test
    void getAttractionsByCategory_WithInvalidCategory_ShouldReturnBadRequest() {
        // 执行方法（无效分类）
        ResponseEntity<Page<Attraction>> response = attractionController.getAttractionsByCategory(
                "INVALID_CATEGORY", 0, 10);

        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void searchAttractions_ShouldReturnMatchingAttractions() {
        // 准备测试数据
        List<Attraction> attractions = Arrays.asList(attraction, attraction);
        when(attractionService.searchAttractionsByName(eq("西湖"), eq(10)))
                .thenReturn(attractions);

        // 执行方法
        ResponseEntity<List<Attraction>> response = attractionController.searchAttractions("西湖", 10);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("西湖景区", response.getBody().get(0).getName());

        // 验证服务调用
        verify(attractionService, times(1))
                .searchAttractionsByName(eq("西湖"), eq(10));
    }

    @Test
    void searchAttractions_WithEmptyResult_ShouldReturnEmptyList() {
        // 准备测试数据
        when(attractionService.searchAttractionsByName(eq("不存在"), eq(10)))
                .thenReturn(Collections.emptyList());

        // 执行方法
        ResponseEntity<List<Attraction>> response = attractionController.searchAttractions("不存在", 10);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        // 验证服务调用
        verify(attractionService, times(1))
                .searchAttractionsByName(eq("不存在"), eq(10));
    }

    @Test
    void getAttractionTopTags_WhenAttractionExists_ShouldReturnTags() {
        // 准备测试数据
        when(attractionService.getAttractionById(1L)).thenReturn(Optional.of(attraction));
        when(attractionService.getTopNTags(eq(attraction), eq(3)))
                .thenReturn(Arrays.asList("公园", "自然", "休闲"));

        // 执行方法
        ResponseEntity<List<String>> response = attractionController.getAttractionTopTags(1L, 3);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        assertEquals("公园", response.getBody().get(0));

        // 验证服务调用
        verify(attractionService, times(1)).getAttractionById(1L);
        verify(attractionService, times(1)).getTopNTags(eq(attraction), eq(3));
    }

    @Test
    void getAttractionTopTags_WhenAttractionNotExists_ShouldReturnNotFound() {
        // 准备测试数据
        when(attractionService.getAttractionById(1L)).thenReturn(Optional.empty());

        // 执行方法
        ResponseEntity<List<String>> response = attractionController.getAttractionTopTags(1L, 3);

        // 验证结果
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        // 验证服务调用
        verify(attractionService, times(1)).getAttractionById(1L);
        verify(attractionService, never()).getTopNTags(any(), anyInt());
    }

    @Test
    void incrementJoinCount_WhenAttractionExists_ShouldReturnOk() {
        // 准备测试数据
        doNothing().when(attractionService).incrementJoinCount(1L);

        // 执行方法
        ResponseEntity<Void> response = attractionController.incrementJoinCount(1L);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // 验证服务调用
        verify(attractionService, times(1)).incrementJoinCount(1L);
    }

    @Test
    void incrementJoinCount_WhenAttractionNotExists_ShouldReturnNotFound() {
        // 准备测试数据
        doThrow(new RuntimeException("Attraction not found")).when(attractionService).incrementJoinCount(1L);

        // 执行方法
        ResponseEntity<Void> response = attractionController.incrementJoinCount(1L);

        // 验证结果
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        // 验证服务调用
        verify(attractionService, times(1)).incrementJoinCount(1L);
    }

    @Test
    void incrementJoinCount_WithServiceException_ShouldReturnInternalServerError() {
        // 准备测试数据
        doThrow(new RuntimeException("Unexpected error")).when(attractionService).incrementJoinCount(1L);

        // 执行方法
        ResponseEntity<Void> response = attractionController.incrementJoinCount(1L);

        // 验证结果
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        // 验证服务调用
        verify(attractionService, times(1)).incrementJoinCount(1L);
    }
}
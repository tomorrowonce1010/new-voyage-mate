package com.se_07.backend.controller;

import com.se_07.backend.dto.CreateTravelGroupRequest;
import com.se_07.backend.dto.ItineraryCreateRequest;
import com.se_07.backend.dto.ItineraryDTO;
import com.se_07.backend.dto.TravelGroupDTO;
import com.se_07.backend.service.ItineraryService;
import com.se_07.backend.service.TravelGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TravelGroupControllerTest {

    @Mock
    private TravelGroupService travelGroupService;
    
    @Mock
    private ItineraryService itineraryService;
    
    @InjectMocks
    private TravelGroupController travelGroupController;
    
    private MockHttpSession mockSession;
    private CreateTravelGroupRequest createRequest;
    private TravelGroupDTO travelGroupDTO;
    private ItineraryDTO itineraryDTO;
    
    @BeforeEach
    void setUp() {
        mockSession = new MockHttpSession();
        mockSession.setAttribute("userId", 1L);
        
        // 设置创建组团请求
        createRequest = new CreateTravelGroupRequest();
        createRequest.setTitle("测试组团");
        createRequest.setDescription("这是一个测试组团");
        createRequest.setMaxMembers(10);
        createRequest.setStartDate(LocalDate.now().plusDays(30));
        createRequest.setEndDate(LocalDate.now().plusDays(37));
        createRequest.setEstimatedBudget(new BigDecimal("5000"));
        createRequest.setGroupType("休闲");
        createRequest.setIsPublic(true);
        createRequest.setTravelTags(Arrays.asList("文化", "美食"));
        createRequest.setIntroduction("组团介绍");
        createRequest.setDestinationId(1L);
        
        // 设置组团DTO
        travelGroupDTO = new TravelGroupDTO();
        travelGroupDTO.setId(1L);
        travelGroupDTO.setTitle("测试组团");
        travelGroupDTO.setDescription("这是一个测试组团");
        travelGroupDTO.setStatus("RECRUITING");
        travelGroupDTO.setIsPublic(true);
        travelGroupDTO.setStartDate(LocalDate.now().plusDays(30));
        travelGroupDTO.setEndDate(LocalDate.now().plusDays(37));
        travelGroupDTO.setMaxMembers(10);
        travelGroupDTO.setCurrentMembers(1);
        travelGroupDTO.setEstimatedBudget(new BigDecimal("5000"));
        travelGroupDTO.setGroupType("休闲");
        travelGroupDTO.setTravelTags(Arrays.asList("文化", "美食"));
        travelGroupDTO.setCreatorId(1L);
        travelGroupDTO.setCreatorName("测试用户");
        
        // 设置行程DTO
        itineraryDTO = new ItineraryDTO();
        itineraryDTO.setId(1L);
        itineraryDTO.setTitle("测试行程");
        itineraryDTO.setImageUrl("test-image.jpg");
        itineraryDTO.setStartDate(LocalDate.now().plusDays(30));
        itineraryDTO.setEndDate(LocalDate.now().plusDays(37));
        itineraryDTO.setBudget(new BigDecimal("5000"));
        itineraryDTO.setTravelerCount(2);
    }

    @Test
    void testGetPublicGroups_Success() {
        // Arrange
        List<TravelGroupDTO> groups = Arrays.asList(travelGroupDTO);
        when(travelGroupService.getPublicRecruitingGroups(1L)).thenReturn(groups);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getPublicGroups(mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(groups, response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).getPublicRecruitingGroups(1L);
    }

    @Test
    void testGetPublicGroups_Exception() {
        // Arrange
        when(travelGroupService.getPublicRecruitingGroups(1L)).thenThrow(new RuntimeException("Database error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getPublicGroups(mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("获取公开组团失败："));
        
        verify(travelGroupService, times(1)).getPublicRecruitingGroups(1L);
    }

    @Test
    void testSearchPublicGroups_Success() {
        // Arrange
        List<TravelGroupDTO> groups = Arrays.asList(travelGroupDTO);
        when(travelGroupService.getPublicRecruitingGroupsWithSearch(1L, "北京", "title", "2024-01-01", "2024-12-31"))
                .thenReturn(groups);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.searchPublicGroups(
                "北京", "title", "2024-01-01", "2024-12-31", mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(groups, response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).getPublicRecruitingGroupsWithSearch(1L, "北京", "title", "2024-01-01", "2024-12-31");
    }

    @Test
    void testSearchPublicGroups_WithNullParameters() {
        // Arrange
        List<TravelGroupDTO> groups = Arrays.asList(travelGroupDTO);
        when(travelGroupService.getPublicRecruitingGroupsWithSearch(1L, null, null, null, null))
                .thenReturn(groups);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.searchPublicGroups(
                null, null, null, null, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(groups, response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).getPublicRecruitingGroupsWithSearch(1L, null, null, null, null);
    }

    @Test
    void testSearchPublicGroups_Exception() {
        // Arrange
        when(travelGroupService.getPublicRecruitingGroupsWithSearch(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Search error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.searchPublicGroups(
                "北京", "title", "2024-01-01", "2024-12-31", mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("搜索公开组团失败："));
        
        verify(travelGroupService, times(1)).getPublicRecruitingGroupsWithSearch(1L, "北京", "title", "2024-01-01", "2024-12-31");
    }

    @Test
    void testCreateTravelGroup_Success() {
        // Arrange
        when(travelGroupService.createTravelGroup(createRequest, 1L)).thenReturn(travelGroupDTO);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.createTravelGroup(createRequest, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(travelGroupDTO, response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).createTravelGroup(createRequest, 1L);
    }

    @Test
    void testCreateTravelGroup_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.createTravelGroup(createRequest, sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).createTravelGroup(any(), any());
    }

    @Test
    void testCreateTravelGroup_Exception() {
        // Arrange
        when(travelGroupService.createTravelGroup(createRequest, 1L)).thenThrow(new RuntimeException("Creation error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.createTravelGroup(createRequest, mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("创建组团失败："));
        
        verify(travelGroupService, times(1)).createTravelGroup(createRequest, 1L);
    }

    @Test
    void testGetGroupDetail_Success() {
        // Arrange
        when(travelGroupService.getGroupDetail(1L, 1L)).thenReturn(travelGroupDTO);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getGroupDetail(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(travelGroupDTO, response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).getGroupDetail(1L, 1L);
    }

    @Test
    void testGetGroupDetail_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getGroupDetail(1L, sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).getGroupDetail(anyLong(), anyLong());
    }

    @Test
    void testGetGroupDetail_Exception() {
        // Arrange
        when(travelGroupService.getGroupDetail(1L, 1L)).thenThrow(new RuntimeException("Detail error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getGroupDetail(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("获取组团详情失败："));
        
        verify(travelGroupService, times(1)).getGroupDetail(1L, 1L);
    }

    @Test
    void testApplyToJoinGroup_Success() {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("applicationMessage", "我想加入这个组团");
        doNothing().when(travelGroupService).applyToJoinGroup(1L, 1L, "我想加入这个组团");
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.applyToJoinGroup(1L, request, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("申请已提交", response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).applyToJoinGroup(1L, 1L, "我想加入这个组团");
    }

    @Test
    void testApplyToJoinGroup_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        Map<String, String> request = new HashMap<>();
        request.put("applicationMessage", "我想加入这个组团");
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.applyToJoinGroup(1L, request, sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).applyToJoinGroup(anyLong(), anyLong(), any());
    }

    @Test
    void testApplyToJoinGroup_Exception() {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("applicationMessage", "我想加入这个组团");
        doThrow(new RuntimeException("Apply error")).when(travelGroupService).applyToJoinGroup(1L, 1L, "我想加入这个组团");
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.applyToJoinGroup(1L, request, mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("申请失败："));
        
        verify(travelGroupService, times(1)).applyToJoinGroup(1L, 1L, "我想加入这个组团");
    }

    @Test
    void testGetMyCreatedGroups_Success() {
        // Arrange
        List<TravelGroupDTO> groups = Arrays.asList(travelGroupDTO);
        when(travelGroupService.getUserCreatedGroups(1L)).thenReturn(groups);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getMyCreatedGroups(mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(groups, response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).getUserCreatedGroups(1L);
    }

    @Test
    void testGetMyCreatedGroups_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getMyCreatedGroups(sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).getUserCreatedGroups(anyLong());
    }

    @Test
    void testGetMyCreatedGroups_Exception() {
        // Arrange
        when(travelGroupService.getUserCreatedGroups(1L)).thenThrow(new RuntimeException("Created groups error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getMyCreatedGroups(mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("获取我创建的组团失败："));
        
        verify(travelGroupService, times(1)).getUserCreatedGroups(1L);
    }

    @Test
    void testGetMyJoinedGroups_Success() {
        // Arrange
        List<TravelGroupDTO> groups = Arrays.asList(travelGroupDTO);
        when(travelGroupService.getUserJoinedGroups(1L)).thenReturn(groups);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getMyJoinedGroups(mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(groups, response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).getUserJoinedGroups(1L);
    }

    @Test
    void testGetMyJoinedGroups_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getMyJoinedGroups(sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).getUserJoinedGroups(anyLong());
    }

    @Test
    void testGetMyJoinedGroups_Exception() {
        // Arrange
        when(travelGroupService.getUserJoinedGroups(1L)).thenThrow(new RuntimeException("Joined groups error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getMyJoinedGroups(mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("获取我参与的组团失败："));
        
        verify(travelGroupService, times(1)).getUserJoinedGroups(1L);
    }

    @Test
    void testGetGroupApplications_Success() {
        // Arrange
        List<Map<String, Object>> applications = Arrays.asList(
            Map.of("id", 1L, "userId", 2L, "message", "申请加入")
        );
        when(travelGroupService.getGroupApplications(1L, 1L)).thenReturn(applications);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getGroupApplications(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(applications, response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).getGroupApplications(1L, 1L);
    }

    @Test
    void testGetGroupApplications_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getGroupApplications(1L, sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).getGroupApplications(anyLong(), anyLong());
    }

    @Test
    void testGetGroupApplications_Exception() {
        // Arrange
        when(travelGroupService.getGroupApplications(1L, 1L)).thenThrow(new RuntimeException("Applications error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getGroupApplications(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("获取申请列表失败："));
        
        verify(travelGroupService, times(1)).getGroupApplications(1L, 1L);
    }

    @Test
    void testProcessApplication_Approve() {
        // Arrange
        doNothing().when(travelGroupService).processApplication(1L, 1L, 1L, true);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.processApplication(1L, 1L, "approve", mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("申请已通过", response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).processApplication(1L, 1L, 1L, true);
    }

    @Test
    void testProcessApplication_Reject() {
        // Arrange
        doNothing().when(travelGroupService).processApplication(1L, 1L, 1L, false);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.processApplication(1L, 1L, "reject", mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("申请已拒绝", response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).processApplication(1L, 1L, 1L, false);
    }

    @Test
    void testProcessApplication_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.processApplication(1L, 1L, "approve", sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).processApplication(anyLong(), anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void testProcessApplication_Exception() {
        // Arrange
        doThrow(new RuntimeException("Process error")).when(travelGroupService).processApplication(1L, 1L, 1L, true);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.processApplication(1L, 1L, "approve", mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("处理申请失败："));
        
        verify(travelGroupService, times(1)).processApplication(1L, 1L, 1L, true);
    }

    @Test
    void testCreateGroupItinerary_Success() {
        // Arrange
        ItineraryCreateRequest itineraryRequest = new ItineraryCreateRequest();
        itineraryRequest.setTitle("团队行程");
        itineraryRequest.setImageUrl("team-image.jpg");
        itineraryRequest.setStartDate(LocalDate.now().plusDays(30));
        itineraryRequest.setEndDate(LocalDate.now().plusDays(37));
        itineraryRequest.setBudget(new BigDecimal("5000"));
        itineraryRequest.setTravelerCount(5);
        
        when(itineraryService.createGroupItinerary(1L, 1L, itineraryRequest, false)).thenReturn(itineraryDTO);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.createGroupItinerary(1L, itineraryRequest, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(itineraryDTO, response.getBody().get("data"));
        
        verify(itineraryService, times(1)).createGroupItinerary(1L, 1L, itineraryRequest, false);
    }

    @Test
    void testCreateGroupItinerary_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        ItineraryCreateRequest itineraryRequest = new ItineraryCreateRequest();
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.createGroupItinerary(1L, itineraryRequest, sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(itineraryService, never()).createGroupItinerary(anyLong(), anyLong(), any(), anyBoolean());
    }

    @Test
    void testCreateGroupItinerary_Exception() {
        // Arrange
        ItineraryCreateRequest itineraryRequest = new ItineraryCreateRequest();
        when(itineraryService.createGroupItinerary(1L, 1L, itineraryRequest, false))
                .thenThrow(new RuntimeException("Create itinerary error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.createGroupItinerary(1L, itineraryRequest, mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("创建团队行程失败："));
        
        verify(itineraryService, times(1)).createGroupItinerary(1L, 1L, itineraryRequest, false);
    }

    @Test
    void testGetGroupItinerary_Success_WithItinerary() {
        // Arrange
        List<ItineraryDTO> itineraries = Arrays.asList(itineraryDTO);
        when(itineraryService.getGroupItineraries(1L, 1L, false)).thenReturn(itineraries);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getGroupItinerary(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(itineraryDTO, response.getBody().get("data"));
        
        verify(itineraryService, times(1)).getGroupItineraries(1L, 1L, false);
    }

    @Test
    void testGetGroupItinerary_Success_EmptyList() {
        // Arrange
        List<ItineraryDTO> itineraries = new ArrayList<>();
        when(itineraryService.getGroupItineraries(1L, 1L, false)).thenReturn(itineraries);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getGroupItinerary(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertNull(response.getBody().get("data"));
        
        verify(itineraryService, times(1)).getGroupItineraries(1L, 1L, false);
    }

    @Test
    void testGetGroupItinerary_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getGroupItinerary(1L, sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(itineraryService, never()).getGroupItineraries(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void testGetGroupItinerary_Exception() {
        // Arrange
        when(itineraryService.getGroupItineraries(1L, 1L, false)).thenThrow(new RuntimeException("Get itinerary error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getGroupItinerary(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("获取团队行程失败："));
        
        verify(itineraryService, times(1)).getGroupItineraries(1L, 1L, false);
    }

    @Test
    void testGetRecommendedGroups_Success() {
        // Arrange
        List<TravelGroupDTO> groups = Arrays.asList(travelGroupDTO);
        when(travelGroupService.getRecommendedGroups(1L)).thenReturn(groups);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getRecommendedGroups(mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(groups, response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).getRecommendedGroups(1L);
    }

    @Test
    void testGetRecommendedGroups_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getRecommendedGroups(sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).getRecommendedGroups(anyLong());
    }

    @Test
    void testGetRecommendedGroups_Exception() {
        // Arrange
        when(travelGroupService.getRecommendedGroups(1L)).thenThrow(new RuntimeException("Recommendations error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getRecommendedGroups(mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("获取推荐组团失败："));
        
        verify(travelGroupService, times(1)).getRecommendedGroups(1L);
    }

    @Test
    void testGetRecommendationsByPreferences_Success() {
        // Arrange
        List<TravelGroupDTO> groups = Arrays.asList(travelGroupDTO);
        Map<String, List<String>> request = new HashMap<>();
        request.put("preferences", Arrays.asList("文化", "美食"));
        
        when(travelGroupService.getRecommendationsByPreferences(1L, Arrays.asList("文化", "美食"))).thenReturn(groups);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getRecommendationsByPreferences(request, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(groups, response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).getRecommendationsByPreferences(1L, Arrays.asList("文化", "美食"));
    }

    @Test
    void testGetRecommendationsByPreferences_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        Map<String, List<String>> request = new HashMap<>();
        request.put("preferences", Arrays.asList("文化", "美食"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getRecommendationsByPreferences(request, sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).getRecommendationsByPreferences(anyLong(), any());
    }

    @Test
    void testGetRecommendationsByPreferences_Exception() {
        // Arrange
        Map<String, List<String>> request = new HashMap<>();
        request.put("preferences", Arrays.asList("文化", "美食"));
        
        when(travelGroupService.getRecommendationsByPreferences(1L, Arrays.asList("文化", "美食")))
                .thenThrow(new RuntimeException("Preferences error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getRecommendationsByPreferences(request, mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("获取推荐组团失败："));
        
        verify(travelGroupService, times(1)).getRecommendationsByPreferences(1L, Arrays.asList("文化", "美食"));
    }

    @Test
    void testGetUserStatusInGroup_Success() {
        // Arrange
        Map<String, Object> status = Map.of("role", "MEMBER", "joinDate", "2024-01-01");
        when(travelGroupService.getUserStatusInGroup(1L, 1L)).thenReturn(status);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getUserStatusInGroup(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(status, response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).getUserStatusInGroup(1L, 1L);
    }

    @Test
    void testGetUserStatusInGroup_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getUserStatusInGroup(1L, sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).getUserStatusInGroup(anyLong(), anyLong());
    }

    @Test
    void testGetUserStatusInGroup_Exception() {
        // Arrange
        when(travelGroupService.getUserStatusInGroup(1L, 1L)).thenThrow(new RuntimeException("Status error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.getUserStatusInGroup(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("获取用户状态失败："));
        
        verify(travelGroupService, times(1)).getUserStatusInGroup(1L, 1L);
    }

    @Test
    void testUpdateGroupStatus_Success() {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("status", "RECRUITING");
        
        when(travelGroupService.updateGroupStatus(1L, "RECRUITING", 1L)).thenReturn(travelGroupDTO);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.updateGroupStatus(1L, request, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(travelGroupDTO, response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).updateGroupStatus(1L, "RECRUITING", 1L);
    }

    @Test
    void testUpdateGroupStatus_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        Map<String, String> request = new HashMap<>();
        request.put("status", "RECRUITING");
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.updateGroupStatus(1L, request, sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).updateGroupStatus(anyLong(), any(), anyLong());
    }

    @Test
    void testUpdateGroupStatus_Exception() {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("status", "RECRUITING");
        
        when(travelGroupService.updateGroupStatus(1L, "RECRUITING", 1L)).thenThrow(new RuntimeException("Update status error"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.updateGroupStatus(1L, request, mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("更新组团状态失败："));
        
        verify(travelGroupService, times(1)).updateGroupStatus(1L, "RECRUITING", 1L);
    }

    @Test
    void testWithdrawApplication_Success() {
        // Arrange
        doNothing().when(travelGroupService).withdrawApplication(1L, 1L);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.withdrawApplication(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("申请已撤回", response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).withdrawApplication(1L, 1L);
    }

    @Test
    void testWithdrawApplication_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.withdrawApplication(1L, sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).withdrawApplication(anyLong(), anyLong());
    }

    @Test
    void testWithdrawApplication_Exception() {
        // Arrange
        doThrow(new RuntimeException("Withdraw error")).when(travelGroupService).withdrawApplication(1L, 1L);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.withdrawApplication(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("撤回申请失败："));
        
        verify(travelGroupService, times(1)).withdrawApplication(1L, 1L);
    }

    @Test
    void testLeaveGroup_Success() {
        // Arrange
        doNothing().when(travelGroupService).leaveGroup(1L, 1L);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.leaveGroup(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("已退出组团", response.getBody().get("data"));
        
        verify(travelGroupService, times(1)).leaveGroup(1L, 1L);
    }

    @Test
    void testLeaveGroup_UserNotLoggedIn() {
        // Arrange
        MockHttpSession sessionWithoutUser = new MockHttpSession();
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.leaveGroup(1L, sessionWithoutUser);
        
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("用户未登录", response.getBody().get("message"));
        
        verify(travelGroupService, never()).leaveGroup(anyLong(), anyLong());
    }

    @Test
    void testLeaveGroup_Exception() {
        // Arrange
        doThrow(new RuntimeException("Leave error")).when(travelGroupService).leaveGroup(1L, 1L);
        
        // Act
        ResponseEntity<Map<String, Object>> response = travelGroupController.leaveGroup(1L, mockSession);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("退出组团失败："));
        
        verify(travelGroupService, times(1)).leaveGroup(1L, 1L);
    }
} 
package com.se_07.backend.controller;

import com.se_07.backend.dto.*;
import com.se_07.backend.entity.Tag;
import com.se_07.backend.repository.ItineraryRepository;
import com.se_07.backend.repository.TagRepository;
import com.se_07.backend.repository.UserRepository;
import com.se_07.backend.service.ItineraryService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItineraryControllerTest {
    @Mock
    private ItineraryService itineraryService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItineraryRepository itineraryRepository;
    @Mock
    private TagRepository tagRepository;
    @InjectMocks
    private ItineraryController controller;
    @Mock
    private HttpSession session;
    private final Long userId = 1L;
    private final Long itineraryId = 2L;
    private final Long dayId = 3L;
    private final Long groupId = 4L;

    @BeforeEach
    void setUp() {
        lenient().when(session.getAttribute("userId")).thenReturn(userId);
    }

    @Test
    void createItinerary_success() {
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        ItineraryDTO dto = new ItineraryDTO();
        when(itineraryService.createItinerary(eq(userId), any())).thenReturn(dto);
        ResponseEntity<ItineraryDTO> resp = controller.createItinerary(req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void createItinerary_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<ItineraryDTO> resp = controller.createItinerary(new ItineraryCreateRequest(), session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void getUserItineraries_success() {
        List<ItineraryDTO> list = Collections.singletonList(new ItineraryDTO());
        when(itineraryService.getUserItineraries(eq(userId), any(Pageable.class))).thenReturn(list);
        ResponseEntity<List<ItineraryDTO>> resp = controller.getUserItineraries(session, 0, 10);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(list, resp.getBody());
    }

    @Test
    void getUserItineraries_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<List<ItineraryDTO>> resp = controller.getUserItineraries(session, 0, 10);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void getUserItineraries_error() {
        when(itineraryService.getUserItineraries(eq(userId), any(Pageable.class))).thenThrow(new RuntimeException("fail"));
        ResponseEntity<List<ItineraryDTO>> resp = controller.getUserItineraries(session, 0, 10);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void getItineraryById_success() {
        ItineraryDTO dto = new ItineraryDTO();
        when(itineraryService.getItineraryById(eq(itineraryId), eq(userId))).thenReturn(dto);
        ResponseEntity<ItineraryDTO> resp = controller.getItineraryById(itineraryId, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void getItineraryById_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<ItineraryDTO> resp = controller.getItineraryById(itineraryId, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void updateItinerary_success() {
        ItineraryUpdateRequest req = new ItineraryUpdateRequest();
        ItineraryDTO dto = new ItineraryDTO();
        when(itineraryService.updateItinerary(eq(itineraryId), eq(userId), any())).thenReturn(dto);
        ResponseEntity<ItineraryDTO> resp = controller.updateItinerary(itineraryId, req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void updateItinerary_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<ItineraryDTO> resp = controller.updateItinerary(itineraryId, new ItineraryUpdateRequest(), session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void deleteItinerary_success() {
        doNothing().when(itineraryService).deleteItinerary(eq(userId), eq(itineraryId));
        ResponseEntity<Map<String, String>> resp = controller.deleteItinerary(itineraryId, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("删除成功", resp.getBody().get("message"));
        verify(itineraryService, times(1)).deleteItinerary(eq(userId), eq(itineraryId));
    }

    @Test
    void deleteItinerary_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<Map<String, String>> resp = controller.deleteItinerary(itineraryId, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("请先登录", resp.getBody().get("message"));
    }

    @Test
    void lockItinerary_success() {
        ItineraryDTO dto = new ItineraryDTO();
        when(itineraryService.lockItinerary(eq(itineraryId), eq(userId))).thenReturn(dto);
        ResponseEntity<ItineraryDTO> resp = controller.lockItinerary(itineraryId, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void lockItinerary_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<ItineraryDTO> resp = controller.lockItinerary(itineraryId, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void getPendingItineraries_success() {
        List<ItineraryDTO> list = Collections.singletonList(new ItineraryDTO());
        when(itineraryService.getPendingItineraries(eq(userId))).thenReturn(list);
        ResponseEntity<List<ItineraryDTO>> resp = controller.getPendingItineraries(session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(list, resp.getBody());
    }

    @Test
    void getPendingItineraries_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<List<ItineraryDTO>> resp = controller.getPendingItineraries(session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void getCompletedItineraries_success() {
        List<ItineraryDTO> list = Collections.singletonList(new ItineraryDTO());
        when(itineraryService.getCompletedItineraries(eq(userId))).thenReturn(list);
        ResponseEntity<List<ItineraryDTO>> resp = controller.getCompletedItineraries(session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(list, resp.getBody());
    }

    @Test
    void getCompletedItineraries_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<List<ItineraryDTO>> resp = controller.getCompletedItineraries(session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void updatePermissionStatus_success() {
        PermissionStatusResponse response = PermissionStatusResponse.success();
        when(itineraryService.updatePermissionStatus(eq(itineraryId), eq(userId), eq("public"))).thenReturn(response);
        ResponseEntity<?> resp = controller.updatePermissionStatus(itineraryId, "public", session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("权限状态更新成功", ((PermissionStatusResponse)resp.getBody()).getMessage());
    }

    @Test
    void updatePermissionStatus_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<?> resp = controller.updatePermissionStatus(itineraryId, "public", session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void testEndpoint_success() {
        when(session.getAttributeNames()).thenReturn(Collections.enumeration(Arrays.asList("userId")));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        ResponseEntity<?> resp = controller.testEndpoint(session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().toString().contains("status"));
    }

    @Test
    void testCreateItinerary_success() {
        Map<String, Object> testData = new HashMap<>();
        testData.put("travelerCount", 5);
        ResponseEntity<Map<String, Object>> resp = controller.testCreateItinerary(testData, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(userId, resp.getBody().get("userId"));
        assertEquals(5, resp.getBody().get("travelerCount"));
    }

    @Test
    void testCreateItinerary_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        Map<String, Object> testData = new HashMap<>();
        ResponseEntity<Map<String, Object>> resp = controller.testCreateItinerary(testData, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }


    @Test
    void updateItineraryBasic_success() {
        Map<String, Object> updates = new HashMap<>();
        ItineraryDTO dto = new ItineraryDTO();
        when(itineraryService.updateItineraryBasic(eq(userId), eq(itineraryId), eq(updates))).thenReturn(dto);
        ResponseEntity<ItineraryDTO> resp = controller.updateItineraryBasic(itineraryId, updates, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void updateItineraryBasic_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        Map<String, Object> updates = new HashMap<>();
        ResponseEntity<ItineraryDTO> resp = controller.updateItineraryBasic(itineraryId, updates, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void updateItineraryBasic_error() {
        Map<String, Object> updates = new HashMap<>();
        when(itineraryService.updateItineraryBasic(eq(userId), eq(itineraryId), eq(updates))).thenThrow(new RuntimeException("fail"));
        ResponseEntity<ItineraryDTO> resp = controller.updateItineraryBasic(itineraryId, updates, session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void updateItineraryStatus_success() {
        ItineraryDTO dto = new ItineraryDTO();
        when(itineraryService.updateItineraryStatus(eq(userId), eq(itineraryId), eq("completed"))).thenReturn(dto);
        ResponseEntity<ItineraryDTO> resp = controller.updateItineraryStatus(itineraryId, "completed", session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void updateItineraryStatus_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<ItineraryDTO> resp = controller.updateItineraryStatus(itineraryId, "completed", session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void updateItineraryStatus_error() {
        when(itineraryService.updateItineraryStatus(eq(userId), eq(itineraryId), eq("completed"))).thenThrow(new RuntimeException("fail"));
        ResponseEntity<ItineraryDTO> resp = controller.updateItineraryStatus(itineraryId, "completed", session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void uploadCoverImage_success() {
        MultipartFile file = mock(MultipartFile.class);
        when(itineraryService.uploadCoverImage(eq(userId), eq(itineraryId), eq(file))).thenReturn("url");
        ResponseEntity<Map<String, String>> resp = controller.uploadCoverImage(itineraryId, file, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("url", resp.getBody().get("imageUrl"));
    }

    @Test
    void uploadCoverImage_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        MultipartFile file = mock(MultipartFile.class);
        ResponseEntity<Map<String, String>> resp = controller.uploadCoverImage(itineraryId, file, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void uploadCoverImage_error() {
        MultipartFile file = mock(MultipartFile.class);
        when(itineraryService.uploadCoverImage(eq(userId), eq(itineraryId), eq(file))).thenThrow(new RuntimeException("fail"));
        ResponseEntity<Map<String, String>> resp = controller.uploadCoverImage(itineraryId, file, session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void updateDayTitle_success() {
        Map<String, String> req = new HashMap<>();
        req.put("title", "新标题");
        doNothing().when(itineraryService).updateDayTitle(eq(userId), eq(itineraryId), eq(dayId), eq("新标题"));
        ResponseEntity<Map<String, String>> resp = controller.updateDayTitle(itineraryId, dayId, req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("新标题", resp.getBody().get("title"));
    }

    @Test
    void updateDayTitle_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        Map<String, String> req = new HashMap<>();
        req.put("title", "新标题");
        ResponseEntity<Map<String, String>> resp = controller.updateDayTitle(itineraryId, dayId, req, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void updateDayTitle_error() {
        Map<String, String> req = new HashMap<>();
        req.put("title", "新标题");
        doThrow(new RuntimeException("fail")).when(itineraryService).updateDayTitle(eq(userId), eq(itineraryId), eq(dayId), eq("新标题"));
        ResponseEntity<Map<String, String>> resp = controller.updateDayTitle(itineraryId, dayId, req, session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void setEditComplete_success() {
        ItineraryDTO dto = new ItineraryDTO();
        when(itineraryService.setEditComplete(eq(userId), eq(itineraryId))).thenReturn(dto);
        ResponseEntity<ItineraryDTO> resp = controller.setEditComplete(itineraryId, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void setEditComplete_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<ItineraryDTO> resp = controller.setEditComplete(itineraryId, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void setEditComplete_error() {
        when(itineraryService.setEditComplete(eq(userId), eq(itineraryId))).thenThrow(new RuntimeException("fail"));
        ResponseEntity<ItineraryDTO> resp = controller.setEditComplete(itineraryId, session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void shiftItineraryDates_success() {
        Map<String, String> req = new HashMap<>();
        req.put("newStartDate", "2024-01-01");
        ItineraryDTO dto = new ItineraryDTO();
        when(itineraryService.shiftItineraryDates(eq(userId), eq(itineraryId), eq(LocalDate.parse("2024-01-01")))).thenReturn(dto);
        ResponseEntity<ItineraryDTO> resp = controller.shiftItineraryDates(itineraryId, req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void shiftItineraryDates_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        Map<String, String> req = new HashMap<>();
        req.put("newStartDate", "2024-01-01");
        ResponseEntity<ItineraryDTO> resp = controller.shiftItineraryDates(itineraryId, req, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void shiftItineraryDates_error() {
        Map<String, String> req = new HashMap<>();
        req.put("newStartDate", "2024-01-01");
        when(itineraryService.shiftItineraryDates(eq(userId), eq(itineraryId), eq(LocalDate.parse("2024-01-01")))).thenThrow(new RuntimeException("fail"));
        ResponseEntity<ItineraryDTO> resp = controller.shiftItineraryDates(itineraryId, req, session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void generateShareCode_success() {
        ShareCodeRequest req = new ShareCodeRequest();
        req.setDescription("desc");
        when(itineraryService.generateShareCode(eq(itineraryId), eq(userId), eq(req))).thenReturn("code");
        ResponseEntity<ShareCodeResponse> resp = controller.generateShareCode(itineraryId, req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("code", resp.getBody().getShareCode());
    }

    @Test
    void generateShareCode_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ShareCodeRequest req = new ShareCodeRequest();
        req.setDescription("desc");
        ResponseEntity<ShareCodeResponse> resp = controller.generateShareCode(itineraryId, req, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void generateShareCode_error() {
        ShareCodeRequest req = new ShareCodeRequest();
        req.setDescription("desc");
        when(itineraryService.generateShareCode(eq(itineraryId), eq(userId), eq(req))).thenThrow(new RuntimeException("fail"));
        ResponseEntity<ShareCodeResponse> resp = controller.generateShareCode(itineraryId, req, session);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }


    @Test
    void importAIItinerary_success() {
        com.se_07.backend.dto.AIItineraryImportRequest req = new com.se_07.backend.dto.AIItineraryImportRequest();
        ItineraryDTO dto = new ItineraryDTO();
        when(itineraryService.importAIItinerary(eq(userId), eq(req))).thenReturn(dto);
        ResponseEntity<?> resp = controller.importAIItinerary(req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void importAIItinerary_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        com.se_07.backend.dto.AIItineraryImportRequest req = new com.se_07.backend.dto.AIItineraryImportRequest();
        ResponseEntity<?> resp = controller.importAIItinerary(req, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void importAIItinerary_error() {
        com.se_07.backend.dto.AIItineraryImportRequest req = new com.se_07.backend.dto.AIItineraryImportRequest();
        when(itineraryService.importAIItinerary(eq(userId), eq(req))).thenThrow(new RuntimeException("fail"));
        ResponseEntity<?> resp = controller.importAIItinerary(req, session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertTrue(resp.getBody().toString().contains("导入失败"));
    }

    @Test
    void createGroupItinerary_success() {
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        ItineraryDTO dto = new ItineraryDTO();
        when(itineraryService.createGroupItinerary(eq(userId), eq(groupId), eq(req), eq(false))).thenReturn(dto);
        ResponseEntity<ItineraryDTO> resp = controller.createGroupItinerary(groupId, req, false, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void getGroupItineraries_success() {
        List<ItineraryDTO> list = Arrays.asList(new ItineraryDTO(), new ItineraryDTO());
        when(itineraryService.getGroupItineraries(eq(userId), eq(groupId), eq(false))).thenReturn(list);
        ResponseEntity<List<ItineraryDTO>> resp = controller.getGroupItineraries(groupId, false, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(list, resp.getBody());
    }

    @Test
    void setAsTemplate_success() {
        ItineraryDTO dto = new ItineraryDTO();
        when(itineraryService.setAsGroupTemplate(eq(userId), eq(groupId), eq(itineraryId))).thenReturn(dto);
        ResponseEntity<ItineraryDTO> resp = controller.setAsTemplate(groupId, itineraryId, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void getPersonalItineraries_success() {
        List<ItineraryDTO> list = Arrays.asList(new ItineraryDTO(), new ItineraryDTO());
        when(itineraryService.getPersonalItineraries(eq(userId), any(Pageable.class))).thenReturn(list);
        ResponseEntity<List<ItineraryDTO>> resp = controller.getPersonalItineraries(session, 0, 10);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(list, resp.getBody());
    }

    @Test
    void getPersonalItineraries_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<List<ItineraryDTO>> resp = controller.getPersonalItineraries(session, 0, 10);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void getPersonalItineraries_error() {
        when(itineraryService.getPersonalItineraries(eq(userId), any(Pageable.class))).thenThrow(new RuntimeException("fail"));
        ResponseEntity<List<ItineraryDTO>> resp = controller.getPersonalItineraries(session, 0, 10);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void getTeamItineraries_success() {
        List<ItineraryDTO> list = Arrays.asList(new ItineraryDTO(), new ItineraryDTO());
        when(itineraryService.getTeamItineraries(eq(userId), any(Pageable.class))).thenReturn(list);
        ResponseEntity<List<ItineraryDTO>> resp = controller.getTeamItineraries(session, 0, 10);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(list, resp.getBody());
    }

    @Test
    void getTeamItineraries_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<List<ItineraryDTO>> resp = controller.getTeamItineraries(session, 0, 10);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void getTeamItineraries_error() {
        when(itineraryService.getTeamItineraries(eq(userId), any(Pageable.class))).thenThrow(new RuntimeException("fail"));
        ResponseEntity<List<ItineraryDTO>> resp = controller.getTeamItineraries(session, 0, 10);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void healthCheck_success() {
        when(userRepository.count()).thenReturn(10L);
        when(itineraryRepository.count()).thenReturn(5L);
        ResponseEntity<?> resp = controller.healthCheck();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().toString().contains("healthy"));
    }

    @Test
    void healthCheck_error() {
        when(userRepository.count()).thenThrow(new RuntimeException("db error"));
        ResponseEntity<?> resp = controller.healthCheck();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertTrue(resp.getBody().toString().contains("error"));
    }

    @Test
    void getAvailableTags_success() {
        List<Tag> tags = Arrays.asList(new Tag(), new Tag());
        when(tagRepository.findTop30ByOrderByIdAsc()).thenReturn(tags);
        ResponseEntity<List<Tag>> resp = controller.getAvailableTags(session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(tags, resp.getBody());
    }

    @Test
    void getAvailableTags_error() {
        when(tagRepository.findTop30ByOrderByIdAsc()).thenThrow(new RuntimeException("fail"));
        ResponseEntity<List<Tag>> resp = controller.getAvailableTags(session);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
    }
}
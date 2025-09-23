package com.se_07.backend.controller;

import com.se_07.backend.dto.ActivityCreateRequest;
import com.se_07.backend.dto.AmapActivityCreateRequest;
import com.se_07.backend.dto.ItineraryActivityDTO;
import com.se_07.backend.dto.TransportModeUpdateRequest;
import com.se_07.backend.service.ItineraryActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpSession;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItineraryActivityControllerTest {
    @Mock
    private ItineraryActivityService activityService;
    @InjectMocks
    private ItineraryActivityController controller;
    @Mock
    private HttpSession session;
    private final Long userId = 1L;
    private final Long activityId = 10L;
    private final Long dayId = 20L;

    @BeforeEach
    void setUp() {
        when(session.getAttribute("userId")).thenReturn(userId);
    }

    @Test
    void createActivity_success() {
        ActivityCreateRequest req = new ActivityCreateRequest();
        ItineraryActivityDTO dto = new ItineraryActivityDTO();
        when(activityService.createActivity(eq(userId), any())).thenReturn(dto);
        ResponseEntity<ItineraryActivityDTO> resp = controller.createActivity(req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void createActivity_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<ItineraryActivityDTO> resp = controller.createActivity(new ActivityCreateRequest(), session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void getActivitiesByDay_success() {
        List<ItineraryActivityDTO> list = Collections.singletonList(new ItineraryActivityDTO());
        when(activityService.getActivitiesByDay(eq(userId), eq(dayId))).thenReturn(list);
        ResponseEntity<List<ItineraryActivityDTO>> resp = controller.getActivitiesByDay(dayId, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(list, resp.getBody());
    }

    @Test
    void getActivitiesByDay_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<List<ItineraryActivityDTO>> resp = controller.getActivitiesByDay(dayId, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void updateTransportMode_success() {
        TransportModeUpdateRequest req = new TransportModeUpdateRequest();
        req.setTransportMode("bus");
        ItineraryActivityDTO dto = new ItineraryActivityDTO();
        when(activityService.updateTransportMode(eq(userId), eq(activityId), eq("bus"))).thenReturn(dto);
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateTransportMode(activityId, req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void updateTransportMode_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateTransportMode(activityId, new TransportModeUpdateRequest(), session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void updateActivityAttraction_success() {
        Map<String, Object> req = new HashMap<>();
        req.put("attractionId", 123L);
        ItineraryActivityDTO dto = new ItineraryActivityDTO();
        when(activityService.updateActivityAttraction(eq(userId), eq(activityId), eq(123L))).thenReturn(dto);
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateActivityAttraction(activityId, req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void updateActivityAttraction_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        Map<String, Object> req = new HashMap<>();
        req.put("attractionId", 123L);
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateActivityAttraction(activityId, req, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void updateActivityNotes_success() {
        Map<String, String> req = new HashMap<>();
        req.put("attractionNotes", "note");
        ItineraryActivityDTO dto = new ItineraryActivityDTO();
        when(activityService.updateActivityNotes(eq(userId), eq(activityId), eq("note"))).thenReturn(dto);
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateActivityNotes(activityId, req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void updateActivityNotes_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        Map<String, String> req = new HashMap<>();
        req.put("attractionNotes", "note");
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateActivityNotes(activityId, req, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void updateActivityTime_success() {
        Map<String, String> req = new HashMap<>();
        req.put("startTime", "08:00");
        req.put("endTime", "10:00");
        ItineraryActivityDTO dto = new ItineraryActivityDTO();
        when(activityService.updateActivityTime(eq(userId), eq(activityId), eq("08:00"), eq("10:00"))).thenReturn(dto);
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateActivityTime(activityId, req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void updateActivityTime_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        Map<String, String> req = new HashMap<>();
        req.put("startTime", "08:00");
        req.put("endTime", "10:00");
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateActivityTime(activityId, req, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void updateActivityTitle_success() {
        Map<String, String> req = new HashMap<>();
        req.put("title", "新标题");
        ItineraryActivityDTO dto = new ItineraryActivityDTO();
        when(activityService.updateActivityTitle(eq(userId), eq(activityId), eq("新标题"))).thenReturn(dto);
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateActivityTitle(activityId, req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void updateActivityTitle_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        Map<String, String> req = new HashMap<>();
        req.put("title", "新标题");
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateActivityTitle(activityId, req, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void deleteActivity_success() {
        doNothing().when(activityService).deleteActivity(eq(userId), eq(activityId));
        ResponseEntity<Void> resp = controller.deleteActivity(activityId, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(activityService, times(1)).deleteActivity(eq(userId), eq(activityId));
    }

    @Test
    void deleteActivity_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<Void> resp = controller.deleteActivity(activityId, session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void createActivityFromAmap_success() {
        AmapActivityCreateRequest req = new AmapActivityCreateRequest();
        ItineraryActivityDTO dto = new ItineraryActivityDTO();
        when(activityService.createActivityFromAmap(eq(userId), any())).thenReturn(dto);
        ResponseEntity<ItineraryActivityDTO> resp = controller.createActivityFromAmap(req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void createActivityFromAmap_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<ItineraryActivityDTO> resp = controller.createActivityFromAmap(new AmapActivityCreateRequest(), session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void updateActivityAmapAttraction_success() {
        AmapActivityCreateRequest req = new AmapActivityCreateRequest();
        AmapActivityCreateRequest.AmapAttractionInfo info = new AmapActivityCreateRequest.AmapAttractionInfo();
        req.setAttractionInfo(info);
        ItineraryActivityDTO dto = new ItineraryActivityDTO();
        when(activityService.updateActivityAmapAttraction(eq(userId), eq(activityId), eq(info))).thenReturn(dto);
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateActivityAmapAttraction(activityId, req, session);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void updateActivityAmapAttraction_unauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateActivityAmapAttraction(activityId, new AmapActivityCreateRequest(), session);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void updateActivityAmapAttraction_badRequest() {
        AmapActivityCreateRequest req = new AmapActivityCreateRequest();
        req.setAttractionInfo(null);
        ResponseEntity<ItineraryActivityDTO> resp = controller.updateActivityAmapAttraction(activityId, req, session);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }
}
package com.se_07.backend.service.impl;

import com.se_07.backend.dto.ActivityCreateRequest;
import com.se_07.backend.dto.AmapActivityCreateRequest;
import com.se_07.backend.dto.ItineraryActivityDTO;
import com.se_07.backend.dto.converter.ItineraryActivityConverter;
import com.se_07.backend.entity.*;
import com.se_07.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItineraryActivityServiceImplTest {
    @Mock
    private ItineraryActivityRepository activityRepository;
    @Mock
    private ItineraryDayRepository dayRepository;
    @Mock
    private AttractionRepository attractionRepository;
    @Mock
    private DestinationRepository destinationRepository;
    @Mock
    private ItineraryRepository itineraryRepository;
    @Mock
    private ItineraryActivityConverter activityConverter;
    @Mock
    private TravelGroupMemberRepository travelGroupMemberRepository;
    @InjectMocks
    private ItineraryActivityServiceImpl service;

    private User user;
    private Itinerary itinerary;
    private ItineraryDay day;
    private Attraction attraction;
    private ItineraryActivity activity;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        itinerary = new Itinerary();
        itinerary.setId(10L);
        itinerary.setUser(user);
        itinerary.setTravelStatus(Itinerary.TravelStatus.待出行);
        itinerary.setPermissionStatus(Itinerary.PermissionStatus.所有人可见);
        day = new ItineraryDay();
        day.setId(100L);
        day.setItinerary(itinerary);
        day.setFirstActivityId(null);
        day.setLastActivityId(null);
        attraction = new Attraction();
        attraction.setId(1000L);
        attraction.setName("景点A");
        activity = new ItineraryActivity();
        activity.setId(10000L);
        activity.setItineraryDay(day);
        activity.setAttraction(attraction);
        activity.setTitle("活动A");
        activity.setTransportMode("步行");
    }

    @Test
    void createActivity_success() {
        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setItineraryDayId(day.getId());
        req.setAttractionId(attraction.getId());
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
        when(activityRepository.save(any())).thenReturn(activity);
        when(activityConverter.toDTO(any())).thenReturn(new ItineraryActivityDTO());
        ItineraryActivityDTO dto = service.createActivity(user.getId(), req);
        assertNotNull(dto);
    }

    @Test
    void createActivity_missingDayOrAttraction() {
        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setItineraryDayId(null);
        req.setAttractionId(null);
        assertThrows(RuntimeException.class, () -> service.createActivity(user.getId(), req));
    }

    @Test
    void createActivity_dayNull() {
        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setItineraryDayId(999L);
        when(dayRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.createActivity(1L, req));
    }
    @Test
    void createActivity_attractionNull() {
        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setItineraryDayId(day.getId());
        req.setAttractionId(999L);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(attractionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.createActivity(1L, req));
    }
    @Test
    void createActivity_noPermission_team() {
        itinerary.setGroupId(2L);
        day.setItinerary(itinerary);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.createActivity(1L, new ActivityCreateRequest() {{ setItineraryDayId(day.getId()); setAttractionId(attraction.getId()); }}));
    }

    @Test
    void createActivity_dayNotFound() {
        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setItineraryDayId(999L);
        req.setAttractionId(attraction.getId());
        when(dayRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.createActivity(user.getId(), req));
    }

    @Test
    void createActivity_itineraryNull() {
        ActivityCreateRequest req = new ActivityCreateRequest();
        ItineraryDay badDay = new ItineraryDay();
        badDay.setId(101L);
        badDay.setItinerary(null);
        req.setItineraryDayId(101L);
        req.setAttractionId(attraction.getId());
        when(dayRepository.findById(101L)).thenReturn(Optional.of(badDay));
        assertThrows(RuntimeException.class, () -> service.createActivity(user.getId(), req));
    }

    @Test
    void createActivity_noPermission() {
        ActivityCreateRequest req = new ActivityCreateRequest();
        Itinerary otherItinerary = new Itinerary();
        otherItinerary.setId(11L);
        otherItinerary.setUser(new User());
        otherItinerary.setTravelStatus(Itinerary.TravelStatus.待出行);
        ItineraryDay otherDay = new ItineraryDay();
        otherDay.setId(102L);
        otherDay.setItinerary(otherItinerary);
        req.setItineraryDayId(102L);
        req.setAttractionId(attraction.getId());
        when(dayRepository.findById(102L)).thenReturn(Optional.of(otherDay));
        assertThrows(RuntimeException.class, () -> service.createActivity(user.getId(), req));
    }

    @Test
    void createActivity_attractionNotFound() {
        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setItineraryDayId(day.getId());
        req.setAttractionId(9999L);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(attractionRepository.findById(9999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.createActivity(user.getId(), req));
    }

    @Test
    void createActivity_prevAndNextIdBothSet() {
        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setItineraryDayId(day.getId());
        req.setAttractionId(attraction.getId());
        req.setPrevId(1L);
        req.setNextId(2L);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
        assertThrows(RuntimeException.class, () -> service.createActivity(user.getId(), req));
    }

    @Test
    void getActivitiesByDay_success_empty() {
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(activityRepository.findByItineraryDayId(day.getId())).thenReturn(Collections.emptyList());
        List<ItineraryActivityDTO> result = service.getActivitiesByDay(user.getId(), day.getId());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getActivitiesByDay_dayNull() {
        when(dayRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getActivitiesByDay(1L, 999L));
    }
    @Test
    void getActivitiesByDay_itineraryNull() {
        ItineraryDay d = new ItineraryDay(); d.setId(2L); d.setItinerary(null);
        when(dayRepository.findById(2L)).thenReturn(Optional.of(d));
        assertThrows(RuntimeException.class, () -> service.getActivitiesByDay(1L, 2L));
    }
    @Test
    void getActivitiesByDay_teamNoMemberPrivate() {
        itinerary.setGroupId(2L); itinerary.setPermissionStatus(Itinerary.PermissionStatus.私人);
        day.setItinerary(itinerary);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getActivitiesByDay(1L, day.getId()));
    }
    @Test
    void getActivitiesByDay_personalNotOwnerPrivate() {
        User other = new User(); other.setId(99L);
        itinerary.setUser(other); itinerary.setPermissionStatus(Itinerary.PermissionStatus.私人);
        day.setItinerary(itinerary);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        assertThrows(RuntimeException.class, () -> service.getActivitiesByDay(1L, day.getId()));
    }
    @Test
    void getActivitiesByDay_empty() {
        itinerary.setUser(user); itinerary.setPermissionStatus(Itinerary.PermissionStatus.所有人可见);
        day.setItinerary(itinerary);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(activityRepository.findByItineraryDayId(day.getId())).thenReturn(Collections.emptyList());
        assertTrue(service.getActivitiesByDay(1L, day.getId()).isEmpty());
    }

    @Test
    void updateTransportMode_success() {
        activity.getItineraryDay().setItinerary(itinerary);
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(activityRepository.save(any())).thenReturn(activity);
        when(activityConverter.toDTO(any())).thenReturn(new ItineraryActivityDTO());
        ItineraryActivityDTO dto = service.updateTransportMode(user.getId(), activity.getId(), "公交");
        assertNotNull(dto);
    }

    @Test
    void updateTransportMode_activityNotFound() {
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateTransportMode(1L, 999L, "公交"));
    }
    @Test
    void updateTransportMode_itineraryNull() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay(); d.setItinerary(null); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateTransportMode(1L, 1L, "公交"));
    }
    @Test
    void updateTransportMode_noPermission_personal() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay();
        Itinerary iti = new Itinerary(); iti.setUser(new User()); d.setItinerary(iti); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateTransportMode(1L, 1L, "公交"));
    }

    @Test
    void updateActivityAttraction_success() {
        activity.getItineraryDay().setItinerary(itinerary);
        Attraction newAttraction = new Attraction();
        newAttraction.setId(2000L);
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(attractionRepository.findById(newAttraction.getId())).thenReturn(Optional.of(newAttraction));
        when(activityRepository.save(any())).thenReturn(activity);
        when(activityConverter.toDTO(any())).thenReturn(new ItineraryActivityDTO());
        ItineraryActivityDTO dto = service.updateActivityAttraction(user.getId(), activity.getId(), newAttraction.getId());
        assertNotNull(dto);
    }

    @Test
    void updateActivityAttraction_activityNotFound() {
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateActivityAttraction(1L, 999L, 1L));
    }
    @Test
    void updateActivityAttraction_itineraryNull() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay(); d.setItinerary(null); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateActivityAttraction(1L, 1L, 1L));
    }
    @Test
    void updateActivityAttraction_noPermission_personal() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay();
        Itinerary iti = new Itinerary(); iti.setUser(new User()); d.setItinerary(iti); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateActivityAttraction(1L, 1L, 1L));
    }
    @Test
    void updateActivityAttraction_attractionNotFound() {
        activity.getItineraryDay().setItinerary(itinerary);
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(attractionRepository.findById(9999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateActivityAttraction(user.getId(), activity.getId(), 9999L));
    }

    @Test
    void updateActivityNotes_success() {
        activity.getItineraryDay().setItinerary(itinerary);
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(activityRepository.save(any())).thenReturn(activity);
        when(activityConverter.toDTO(any())).thenReturn(new ItineraryActivityDTO());
        ItineraryActivityDTO dto = service.updateActivityNotes(user.getId(), activity.getId(), "新备注");
        assertNotNull(dto);
    }

    @Test
    void updateActivityNotes_activityNotFound() {
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateActivityNotes(1L, 999L, "note"));
    }
    @Test
    void updateActivityNotes_itineraryNull() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay(); d.setItinerary(null); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateActivityNotes(1L, 1L, "note"));
    }
    @Test
    void updateActivityNotes_noPermission_personal() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay();
        Itinerary iti = new Itinerary(); iti.setUser(new User()); d.setItinerary(iti); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateActivityNotes(1L, 1L, "note"));
    }

    @Test
    void updateActivityTime_success() {
        activity.getItineraryDay().setItinerary(itinerary);
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(activityRepository.save(any())).thenReturn(activity);
        when(activityConverter.toDTO(any())).thenReturn(new ItineraryActivityDTO());
        ItineraryActivityDTO dto = service.updateActivityTime(user.getId(), activity.getId(), "08:00", "10:00");
        assertNotNull(dto);
    }

    @Test
    void updateActivityTime_activityNotFound() {
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateActivityTime(1L, 999L, "08:00", "10:00"));
    }
    @Test
    void updateActivityTime_itineraryNull() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay(); d.setItinerary(null); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateActivityTime(1L, 1L, "08:00", "10:00"));
    }
    @Test
    void updateActivityTime_noPermission_personal() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay();
        Itinerary iti = new Itinerary(); iti.setUser(new User()); d.setItinerary(iti); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateActivityTime(1L, 1L, "08:00", "10:00"));
    }

    @Test
    void deleteActivity_success() {
        activity.getItineraryDay().setItinerary(itinerary);
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        doNothing().when(activityRepository).delete(any());
        service.deleteActivity(user.getId(), activity.getId());
        verify(activityRepository, times(1)).delete(any());
    }

    @Test
    void deleteActivity_activityNotFound() {
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.deleteActivity(1L, 999L));
    }
    @Test
    void deleteActivity_itineraryNull() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay(); d.setItinerary(null); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.deleteActivity(1L, 1L));
    }
    @Test
    void deleteActivity_noPermission_personal() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay();
        Itinerary iti = new Itinerary(); iti.setUser(new User()); d.setItinerary(iti); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.deleteActivity(1L, 1L));
    }

    @Test
    void updateActivityTitle_success() {
        activity.getItineraryDay().setItinerary(itinerary);
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(activityRepository.save(any())).thenReturn(activity);
        when(activityConverter.toDTO(any())).thenReturn(new ItineraryActivityDTO());
        ItineraryActivityDTO dto = service.updateActivityTitle(user.getId(), activity.getId(), "新标题");
        assertNotNull(dto);
    }

    @Test
    void updateActivityTitle_activityNotFound() {
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateActivityTitle(1L, 999L, "title"));
    }
    @Test
    void updateActivityTitle_itineraryNull() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay(); d.setItinerary(null); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateActivityTitle(1L, 1L, "title"));
    }
    @Test
    void updateActivityTitle_noPermission_personal() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay();
        Itinerary iti = new Itinerary(); iti.setUser(new User()); d.setItinerary(iti); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateActivityTitle(1L, 1L, "title"));
    }

    @Test
    void createActivityFromAmap_success() {
        AmapActivityCreateRequest req = new AmapActivityCreateRequest();
        req.setItineraryDayId(day.getId());
        req.setAttractionId(attraction.getId());
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
        when(activityRepository.save(any())).thenReturn(activity);
        when(activityConverter.toDTO(any())).thenReturn(new ItineraryActivityDTO());
        ItineraryActivityDTO dto = service.createActivityFromAmap(user.getId(), req);
        assertNotNull(dto);
    }

    @Test
    void createActivityFromAmap_dayNotFound() {
        AmapActivityCreateRequest req = new AmapActivityCreateRequest();
        req.setItineraryDayId(999L);
        when(dayRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.createActivityFromAmap(1L, req));
    }
    @Test
    void createActivityFromAmap_attractionNotFound() {
        AmapActivityCreateRequest req = new AmapActivityCreateRequest();
        req.setItineraryDayId(day.getId());
        req.setAttractionId(999L);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(attractionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.createActivityFromAmap(1L, req));
    }

    @Test
    void updateActivityAmapAttraction_activityIdNull() {
        assertThrows(RuntimeException.class, () -> service.updateActivityAmapAttraction(1L, null, new AmapActivityCreateRequest.AmapAttractionInfo()));
    }
    @Test
    void updateActivityAmapAttraction_attractionInfoNull() {
        assertThrows(RuntimeException.class, () -> service.updateActivityAmapAttraction(1L, 1L, null));
    }
    @Test
    void updateActivityAmapAttraction_activityNotFound() {
        when(activityRepository.findById(99999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateActivityAmapAttraction(1L, 99999L, new AmapActivityCreateRequest.AmapAttractionInfo()));
    }
    @Test
    void updateActivityAmapAttraction_dayNull() {
        ItineraryActivity act = new ItineraryActivity();
        act.setId(123L);
        act.setItineraryDay(null);
        when(activityRepository.findById(123L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateActivityAmapAttraction(1L, 123L, new AmapActivityCreateRequest.AmapAttractionInfo()));
    }
    @Test
    void updateActivityAmapAttraction_itineraryNull() {
        ItineraryActivity act = new ItineraryActivity();
        ItineraryDay d = new ItineraryDay(); d.setItinerary(null); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateActivityAmapAttraction(1L, 1L, new AmapActivityCreateRequest.AmapAttractionInfo()));
    }
    @Test
    void updateActivityAmapAttraction_teamNoMember() {
        itinerary.setGroupId(99L);
        day.setItinerary(itinerary);
        activity.setItineraryDay(day);
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(eq(99L), anyLong())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateActivityAmapAttraction(user.getId(), activity.getId(), new AmapActivityCreateRequest.AmapAttractionInfo()));
    }
    @Test
    void updateActivityAmapAttraction_personalNoPermission() {
        Itinerary iti = new Itinerary(); iti.setUser(new User());
        ItineraryDay d = new ItineraryDay(); d.setItinerary(iti);
        ItineraryActivity act = new ItineraryActivity(); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        assertThrows(RuntimeException.class, () -> service.updateActivityAmapAttraction(1L, 1L, new AmapActivityCreateRequest.AmapAttractionInfo()));
    }
    @Test
    void updateActivityAmapAttraction_success() {
        Itinerary iti = new Itinerary(); iti.setUser(user);
        ItineraryDay d = new ItineraryDay(); d.setItinerary(iti);
        ItineraryActivity act = new ItineraryActivity(); act.setItineraryDay(d); act.setId(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));
        Attraction amapAttraction = new Attraction();
        // mock findOrCreateAttractionFromAmap
        ItineraryActivityServiceImpl spyService = org.mockito.Mockito.spy(service);

    }


    @Test
    void findOrCreateDestination_createNew() throws Exception {
        when(destinationRepository.findByNameContainingIgnoreCase(anyString())).thenReturn(Collections.emptyList());
        when(destinationRepository.save(any())).thenReturn(new Destination());
        var m = ItineraryActivityServiceImpl.class.getDeclaredMethod("findOrCreateDestination", String.class);
        m.setAccessible(true);
        Object dest = m.invoke(service, "新城市");
        assertNotNull(dest);
    }

    @Test
    void findOrCreateDestination_nullOrEmpty() throws Exception {
        var m = ItineraryActivityServiceImpl.class.getDeclaredMethod("findOrCreateDestination", String.class);
        m.setAccessible(true);
        assertThrows(Exception.class, () -> m.invoke(service, (String) null));
        assertThrows(Exception.class, () -> m.invoke(service, ""));
    }

    @Test
    void findOrCreateAttraction_createNew() throws Exception {
        Destination dest = new Destination();
        dest.setId(1L);
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(anyLong())).thenReturn(Collections.emptyList());
        when(attractionRepository.save(any())).thenReturn(new Attraction());
        var m = ItineraryActivityServiceImpl.class.getDeclaredMethod("findOrCreateAttraction", AmapActivityCreateRequest.AmapAttractionInfo.class, Destination.class);
        m.setAccessible(true);
        Object attr = m.invoke(service, new AmapActivityCreateRequest.AmapAttractionInfo(), dest);
        assertNotNull(attr);
    }

    @Test
    void findOrCreateAttraction_null() throws Exception {
        var m = ItineraryActivityServiceImpl.class.getDeclaredMethod("findOrCreateAttraction", AmapActivityCreateRequest.AmapAttractionInfo.class, Destination.class);
        m.setAccessible(true);
        assertThrows(Exception.class, () -> m.invoke(service, null, null));
    }

    // 工具方法：反射调用private findOrCreateAttractionFromAmap
    private Object invokeFindOrCreateAttractionFromAmap(AmapActivityCreateRequest.AmapAttractionInfo info) throws Exception {
        var m = ItineraryActivityServiceImpl.class.getDeclaredMethod("findOrCreateAttractionFromAmap", AmapActivityCreateRequest.AmapAttractionInfo.class);
        m.setAccessible(true);
        return m.invoke(service, info);
    }

    @Test
    void findOrCreateAttractionFromAmap_nullInfo() throws Exception {
        assertThrows(Exception.class, () -> invokeFindOrCreateAttractionFromAmap(null));
    }
    @Test
    void findOrCreateAttractionFromAmap_amapPoiIdExists() throws Exception {
        AmapActivityCreateRequest.AmapAttractionInfo info = new AmapActivityCreateRequest.AmapAttractionInfo();
        info.setId("poi123");
        Attraction exist = new Attraction();
        when(attractionRepository.findByAmapPoiId("poi123")).thenReturn(Optional.of(exist));
        Object result = invokeFindOrCreateAttractionFromAmap(info);
        assertEquals(exist, result);
    }
    @Test
    void findOrCreateAttractionFromAmap_cityNameEmpty_cityFallback() throws Exception {
        AmapActivityCreateRequest.AmapAttractionInfo info = new AmapActivityCreateRequest.AmapAttractionInfo();
        info.setId("");
        info.setCityname("");
        info.setCity("testcity");
        info.setName("景点");
        Destination dest = new Destination(); dest.setId(1L);
        when(destinationRepository.findByNameContainingIgnoreCase("testcity")).thenReturn(Collections.singletonList(dest));
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Collections.emptyList());
        when(attractionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Object result = invokeFindOrCreateAttractionFromAmap(info);
        assertNotNull(result);
    }
    @Test
    void findOrCreateAttractionFromAmap_createNewDestinationAndAttraction() throws Exception {
        AmapActivityCreateRequest.AmapAttractionInfo info = new AmapActivityCreateRequest.AmapAttractionInfo();
        info.setId("");
        info.setCityname("新城市");
        info.setName("新景点");
        Destination dest = new Destination(); dest.setId(2L);
        when(destinationRepository.findByNameContainingIgnoreCase("新城市")).thenReturn(Collections.emptyList());
        when(destinationRepository.save(any())).thenReturn(dest);
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(2L)).thenReturn(Collections.emptyList());
        when(attractionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Object result = invokeFindOrCreateAttractionFromAmap(info);
        assertNotNull(result);
    }
    @Test
    void findOrCreateAttractionFromAmap_attractionExistsInDestination() throws Exception {
        AmapActivityCreateRequest.AmapAttractionInfo info = new AmapActivityCreateRequest.AmapAttractionInfo();
        info.setId("");
        info.setCityname("城市");
        info.setName("景点");
        Destination dest = new Destination(); dest.setId(3L);
        Attraction exist = new Attraction(); exist.setName("景点");
        when(destinationRepository.findByNameContainingIgnoreCase("城市")).thenReturn(Collections.singletonList(dest));
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(3L)).thenReturn(Collections.singletonList(exist));
        Object result = invokeFindOrCreateAttractionFromAmap(info);
        assertEquals(exist, result);
    }

    // findOrCreateAttraction类型分支测试
    private Object invokeFindOrCreateAttraction(AmapActivityCreateRequest.AmapAttractionInfo info, Destination dest) throws Exception {
        var m = ItineraryActivityServiceImpl.class.getDeclaredMethod("findOrCreateAttraction", AmapActivityCreateRequest.AmapAttractionInfo.class, Destination.class);
        m.setAccessible(true);
        return m.invoke(service, info, dest);
    }
    @Test
    void findOrCreateAttraction_category_scenic() throws Exception {
        AmapActivityCreateRequest.AmapAttractionInfo info = new AmapActivityCreateRequest.AmapAttractionInfo();
        info.setName("景点"); info.setType("风景名胜");
        Destination dest = new Destination(); dest.setId(1L);
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Collections.emptyList());
        when(attractionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Attraction result = (Attraction) invokeFindOrCreateAttraction(info, dest);
        assertEquals(Attraction.AttractionCategory.旅游景点, result.getCategory());
    }
    @Test
    void findOrCreateAttraction_category_park() throws Exception {
        AmapActivityCreateRequest.AmapAttractionInfo info = new AmapActivityCreateRequest.AmapAttractionInfo();
        info.setName("景点"); info.setType("公园");
        Destination dest = new Destination(); dest.setId(1L);
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Collections.emptyList());
        when(attractionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Attraction result = (Attraction) invokeFindOrCreateAttraction(info, dest);
        assertEquals(Attraction.AttractionCategory.旅游景点, result.getCategory());
    }
    @Test
    void findOrCreateAttraction_category_food() throws Exception {
        AmapActivityCreateRequest.AmapAttractionInfo info = new AmapActivityCreateRequest.AmapAttractionInfo();
        info.setName("餐厅"); info.setType("餐饮");
        Destination dest = new Destination(); dest.setId(1L);
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Collections.emptyList());
        when(attractionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Attraction result = (Attraction) invokeFindOrCreateAttraction(info, dest);
        assertEquals(Attraction.AttractionCategory.餐饮, result.getCategory());
    }
    @Test
    void findOrCreateAttraction_category_hotel() throws Exception {
        AmapActivityCreateRequest.AmapAttractionInfo info = new AmapActivityCreateRequest.AmapAttractionInfo();
        info.setName("酒店"); info.setType("住宿");
        Destination dest = new Destination(); dest.setId(1L);
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Collections.emptyList());
        when(attractionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Attraction result = (Attraction) invokeFindOrCreateAttraction(info, dest);
        assertEquals(Attraction.AttractionCategory.住宿, result.getCategory());
    }
    @Test
    void findOrCreateAttraction_category_transport() throws Exception {
        AmapActivityCreateRequest.AmapAttractionInfo info = new AmapActivityCreateRequest.AmapAttractionInfo();
        info.setName("车站"); info.setType("交通");
        Destination dest = new Destination(); dest.setId(1L);
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Collections.emptyList());
        when(attractionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Attraction result = (Attraction) invokeFindOrCreateAttraction(info, dest);
        assertEquals(Attraction.AttractionCategory.交通站点, result.getCategory());
    }
    @Test
    void findOrCreateAttraction_category_other() throws Exception {
        AmapActivityCreateRequest.AmapAttractionInfo info = new AmapActivityCreateRequest.AmapAttractionInfo();
        info.setName("其他"); info.setType("其他类型");
        Destination dest = new Destination(); dest.setId(1L);
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Collections.emptyList());
        when(attractionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Attraction result = (Attraction) invokeFindOrCreateAttraction(info, dest);
        assertEquals(Attraction.AttractionCategory.旅游景点, result.getCategory());
    }
    @Test
    void findOrCreateAttraction_category_nullType() throws Exception {
        AmapActivityCreateRequest.AmapAttractionInfo info = new AmapActivityCreateRequest.AmapAttractionInfo();
        info.setName("未知"); info.setType(null);
        Destination dest = new Destination(); dest.setId(1L);
        when(attractionRepository.findByDestinationIdOrderByJoinCountDesc(1L)).thenReturn(Collections.emptyList());
        when(attractionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Attraction result = (Attraction) invokeFindOrCreateAttraction(info, dest);
        assertEquals(Attraction.AttractionCategory.旅游景点, result.getCategory());
    }

    // insertActivityIntoLinkedList分支全覆盖
    private void invokeInsertActivityIntoLinkedList(ItineraryDay day, ItineraryActivity newActivity, Long nextId, Long prevId) throws Exception {
        var m = ItineraryActivityServiceImpl.class.getDeclaredMethod(
            "insertActivityIntoLinkedList",
            ItineraryDay.class, ItineraryActivity.class, Long.class, Long.class
        );
        m.setAccessible(true);
        m.invoke(service, day, newActivity, nextId, prevId);
    }

    @Test
    void insertActivity_firstActivity() throws Exception {
        ItineraryDay day = new ItineraryDay(); day.setId(1L); day.setFirstActivityId(null); day.setLastActivityId(null);
        ItineraryActivity newActivity = new ItineraryActivity(); newActivity.setId(10L);
        when(activityRepository.save(any())).thenReturn(newActivity);
        when(dayRepository.save(any())).thenReturn(day);
        assertDoesNotThrow(() -> invokeInsertActivityIntoLinkedList(day, newActivity, null, null));
    }
    @Test
    void insertActivity_nextId_head() throws Exception {
        ItineraryDay day = new ItineraryDay(); day.setId(1L); day.setFirstActivityId(20L);
        ItineraryActivity newActivity = new ItineraryActivity(); newActivity.setId(10L);
        ItineraryActivity next = new ItineraryActivity(); next.setId(20L); next.setPrevId(null);
        when(activityRepository.findById(20L)).thenReturn(Optional.of(next));
        when(activityRepository.save(any())).thenReturn(newActivity);
        when(dayRepository.save(any())).thenReturn(day);
        assertDoesNotThrow(() -> invokeInsertActivityIntoLinkedList(day, newActivity, 20L, null));
    }
    @Test
    void insertActivity_nextId_middle() throws Exception {
        ItineraryDay day = new ItineraryDay(); day.setId(1L); day.setFirstActivityId(20L);
        ItineraryActivity newActivity = new ItineraryActivity(); newActivity.setId(10L);
        ItineraryActivity next = new ItineraryActivity(); next.setId(20L); next.setPrevId(30L);
        ItineraryActivity prev = new ItineraryActivity(); prev.setId(30L);
        when(activityRepository.findById(20L)).thenReturn(Optional.of(next));
        when(activityRepository.findById(30L)).thenReturn(Optional.of(prev));
        when(activityRepository.save(any())).thenReturn(newActivity);
        when(dayRepository.save(any())).thenReturn(day);
        assertDoesNotThrow(() -> invokeInsertActivityIntoLinkedList(day, newActivity, 20L, null));
    }
    @Test
    void insertActivity_prevId_tail() throws Exception {
        ItineraryDay day = new ItineraryDay(); day.setId(1L); day.setLastActivityId(30L);
        ItineraryActivity newActivity = new ItineraryActivity(); newActivity.setId(10L);
        ItineraryActivity prev = new ItineraryActivity(); prev.setId(30L); prev.setNextId(null);
        when(activityRepository.findById(30L)).thenReturn(Optional.of(prev));
        when(activityRepository.save(any())).thenReturn(newActivity);
        when(dayRepository.save(any())).thenReturn(day);
        assertDoesNotThrow(() -> invokeInsertActivityIntoLinkedList(day, newActivity, null, 30L));
    }
    @Test
    void insertActivity_prevId_middle() throws Exception {
        ItineraryDay day = new ItineraryDay(); day.setId(1L); day.setLastActivityId(30L);
        ItineraryActivity newActivity = new ItineraryActivity(); newActivity.setId(10L);
        ItineraryActivity prev = new ItineraryActivity(); prev.setId(30L); prev.setNextId(40L);
        ItineraryActivity next = new ItineraryActivity(); next.setId(40L);
        when(activityRepository.findById(30L)).thenReturn(Optional.of(prev));
        when(activityRepository.findById(40L)).thenReturn(Optional.of(next));
        when(activityRepository.save(any())).thenReturn(newActivity);
        when(dayRepository.save(any())).thenReturn(day);
        assertDoesNotThrow(() -> invokeInsertActivityIntoLinkedList(day, newActivity, null, 30L));
    }
    @Test
    void insertActivity_prevId_notExist() {
        ItineraryDay day = new ItineraryDay(); day.setId(1L);
        ItineraryActivity newActivity = new ItineraryActivity(); newActivity.setId(10L);
        when(activityRepository.findById(888L)).thenReturn(Optional.empty());
    }
    @Test
    void insertActivity_appendToEnd_empty() throws Exception {
        ItineraryDay day = new ItineraryDay(); day.setId(1L); day.setLastActivityId(null);
        ItineraryActivity newActivity = new ItineraryActivity(); newActivity.setId(10L);
        when(activityRepository.save(any())).thenReturn(newActivity);
        when(dayRepository.save(any())).thenReturn(day);
        assertDoesNotThrow(() -> invokeInsertActivityIntoLinkedList(day, newActivity, null, null));
    }
    @Test
    void insertActivity_appendToEnd_nonEmpty() throws Exception {
        ItineraryDay day = new ItineraryDay(); day.setId(1L); day.setLastActivityId(50L);
        ItineraryActivity newActivity = new ItineraryActivity(); newActivity.setId(10L);
        ItineraryActivity last = new ItineraryActivity(); last.setId(50L);
        when(activityRepository.findById(50L)).thenReturn(Optional.of(last));
        when(activityRepository.save(any())).thenReturn(newActivity);
        when(dayRepository.save(any())).thenReturn(day);
        assertDoesNotThrow(() -> invokeInsertActivityIntoLinkedList(day, newActivity, null, null));
    }

    @Test
    void insertActivity_AfterExistingNode() {
        // 准备现有活动
        ItineraryActivity existing = new ItineraryActivity();
        existing.setId(888L);
        existing.setPrevId(null);
        existing.setNextId(null);

        day.setFirstActivityId(existing.getId());
        day.setLastActivityId(existing.getId());

        // 创建请求（指定prevId）
        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setItineraryDayId(day.getId());
        req.setAttractionId(attraction.getId());
        req.setPrevId(existing.getId()); // 插入到现有节点后

        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
        when(activityRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(activityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 执行
        service.createActivity(user.getId(), req);

    }

    @Test
    void insertActivity_AppendToEnd() {
        // 准备现有活动
        ItineraryActivity existing = new ItineraryActivity();
        existing.setId(888L);
        existing.setPrevId(null);
        existing.setNextId(null);

        day.setFirstActivityId(existing.getId());
        day.setLastActivityId(existing.getId());

        // 创建请求（不指定位置）
        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setItineraryDayId(day.getId());
        req.setAttractionId(attraction.getId());

        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
        when(activityRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(activityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 执行
        service.createActivity(user.getId(), req);

    }

    @Test
    void getActivitiesByDay_linkedListBroken() {
        itinerary.setUser(user); itinerary.setPermissionStatus(Itinerary.PermissionStatus.所有人可见);
        day.setItinerary(itinerary);
        ItineraryActivity a1 = new ItineraryActivity(); a1.setId(1L); a1.setNextId(2L);
        // a2缺失，链表断裂
        day.setFirstActivityId(1L);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(activityRepository.findByItineraryDayId(day.getId())).thenReturn(Arrays.asList(a1));
        when(activityConverter.toDTO(any())).thenReturn(new ItineraryActivityDTO());
        assertDoesNotThrow(() -> service.getActivitiesByDay(1L, day.getId()));
    }
    @Test
    void createActivityFromAmap_nextIdNotExist() {
        AmapActivityCreateRequest req = new AmapActivityCreateRequest();
        req.setItineraryDayId(day.getId());
        req.setAttractionId(attraction.getId());
        req.setNextId(999L);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.createActivityFromAmap(1L, req));
    }
    @Test
    void createActivityFromAmap_nextIdNotSameDay() {
        AmapActivityCreateRequest req = new AmapActivityCreateRequest();
        req.setItineraryDayId(day.getId());
        req.setAttractionId(attraction.getId());
        req.setNextId(888L);
        ItineraryActivity next = new ItineraryActivity();
        ItineraryDay otherDay = new ItineraryDay(); otherDay.setId(999L);
        next.setItineraryDay(otherDay);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(attractionRepository.findById(attraction.getId())).thenReturn(Optional.of(attraction));
        when(activityRepository.findById(888L)).thenReturn(Optional.of(next));
        assertThrows(RuntimeException.class, () -> service.createActivityFromAmap(1L, req));
    }
    @Test
    void createActivityFromAmap_attractionInfoNull() {
        AmapActivityCreateRequest req = new AmapActivityCreateRequest();
        req.setItineraryDayId(day.getId());
        req.setAttractionId(null);
        req.setAttractionInfo(null);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        // attractionRepository.findById不会被调用
        assertThrows(RuntimeException.class, () -> service.createActivityFromAmap(1L, req));
    }

}
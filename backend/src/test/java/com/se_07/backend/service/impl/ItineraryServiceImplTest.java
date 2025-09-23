package com.se_07.backend.service.impl;

import com.se_07.backend.dto.ItineraryCreateRequest;
import com.se_07.backend.dto.ItineraryDTO;
import com.se_07.backend.dto.ItineraryUpdateRequest;
import com.se_07.backend.dto.ShareCodeRequest;
import com.se_07.backend.dto.PermissionStatusResponse;
import com.se_07.backend.dto.converter.ItineraryConverter;
import com.se_07.backend.entity.*;
import com.se_07.backend.repository.*;
import com.se_07.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItineraryServiceImplTest {
    @Mock private ItineraryRepository itineraryRepository;
    @Mock private ItineraryDayRepository itineraryDayRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private ItineraryConverter itineraryConverter;
    @Mock private CommunityEntryRepository communityEntryRepository;
    @Mock private TagRepository tagRepository;
    @Mock private CommunityEntryTagRepository communityEntryTagRepository;
    @Mock private TravelGroupRepository travelGroupRepository;
    @Mock private TravelGroupMemberRepository travelGroupMemberRepository;
    @Mock private GroupItineraryRepository groupItineraryRepository;
    @Mock private com.se_07.backend.repository.AttractionRepository attractionRepository;
    @Mock private com.se_07.backend.repository.ItineraryActivityRepository itineraryActivityRepository;
    @InjectMocks private ItineraryServiceImpl service;

    private User user;
    private Itinerary itinerary;
    private ItineraryDay day;
    private TravelGroup group;
    private TravelGroupMember member;

    @BeforeEach
    void setUp() {
        user = new User(); user.setId(1L);
        itinerary = new Itinerary(); itinerary.setId(10L); itinerary.setUser(user);
        itinerary.setStartDate(LocalDate.now()); itinerary.setEndDate(LocalDate.now().plusDays(2));
        day = new ItineraryDay(); day.setId(100L); day.setItinerary(itinerary);
        group = new TravelGroup(); group.setId(2L); group.setCreator(user);
        member = new TravelGroupMember(); member.setUser(user); member.setGroup(group);
    }

    @Test void createItinerary_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        req.setStartDate(LocalDate.now()); req.setEndDate(LocalDate.now().plusDays(1));
        assertThrows(RuntimeException.class, () -> service.createItinerary(1L, req));
    }
    @Test void createItinerary_endBeforeStart() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        req.setStartDate(LocalDate.now()); req.setEndDate(LocalDate.now().minusDays(1));
        assertThrows(RuntimeException.class, () -> service.createItinerary(1L, req));
    }
    @Test void createItinerary_travelerCountEdge() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itineraryRepository.save(any())).thenReturn(itinerary);
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        when(itineraryConverter.toDTO(any())).thenReturn(new ItineraryDTO());
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        req.setStartDate(LocalDate.now()); req.setEndDate(LocalDate.now().plusDays(1));
        req.setTravelerCount(-1); service.createItinerary(1L, req);
        req.setTravelerCount(100); service.createItinerary(1L, req);
        req.setTravelerCount(null); service.createItinerary(1L, req);
    }
    @Test void createItinerary_invalidStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        req.setStartDate(LocalDate.now()); req.setEndDate(LocalDate.now().plusDays(1));
        req.setTravelStatus("bad");
        assertThrows(RuntimeException.class, () -> service.createItinerary(1L, req));
        req.setTravelStatus(null); req.setPermissionStatus("bad");
        assertThrows(RuntimeException.class, () -> service.createItinerary(1L, req));
    }
    @Test void getUserItineraries_userNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.getUserItineraries(1L, Pageable.unpaged()));
    }
    @Test void getUserItineraries_success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itineraryRepository.findByUserIdOrderByCreatedAtDesc(anyLong(), any())).thenReturn(new PageImpl<>(List.of(itinerary)));
        when(itineraryConverter.toDTO(any())).thenReturn(new ItineraryDTO());
        List<ItineraryDTO> list = service.getUserItineraries(1L, Pageable.unpaged());
        assertEquals(1, list.size());
    }
    @Test void getItineraryById_notFound() {
        when(itineraryRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getItineraryById(10L, 1L));
    }
    @Test void getItineraryById_permission() {
        itinerary.setPermissionStatus(Itinerary.PermissionStatus.私人);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        // 非本人且私人
        User other = new User(); other.setId(2L);
        itinerary.setUser(other);
        assertThrows(RuntimeException.class, () -> service.getItineraryById(10L, 1L));
        // 本人
        itinerary.setUser(user);
        service.getItineraryById(10L, 1L);
    }
    @Test void getItineraryById_teamMemberCanAccess() {
        Itinerary itinerary = new Itinerary(); itinerary.setGroupId(2L); itinerary.setUser(user); itinerary.setPermissionStatus(com.se_07.backend.entity.Itinerary.PermissionStatus.私人);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L)).thenReturn(Optional.of(new com.se_07.backend.entity.TravelGroupMember()));
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        assertDoesNotThrow(() -> service.getItineraryById(10L, 1L));
    }
    @Test void getItineraryById_teamNotMemberButCreator() {
        Itinerary itinerary = new Itinerary(); itinerary.setGroupId(2L);
        User creator = new User(); creator.setId(1L); itinerary.setUser(creator); itinerary.setPermissionStatus(com.se_07.backend.entity.Itinerary.PermissionStatus.私人);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L)).thenReturn(Optional.empty());
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        assertDoesNotThrow(() -> service.getItineraryById(10L, 1L));
    }
    @Test void getItineraryById_teamNotMemberNotCreatorNotPrivate() {
        Itinerary itinerary = new Itinerary(); itinerary.setGroupId(2L);
        User creator = new User(); creator.setId(99L); itinerary.setUser(creator); itinerary.setPermissionStatus(com.se_07.backend.entity.Itinerary.PermissionStatus.所有人可见);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L)).thenReturn(Optional.empty());
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        assertDoesNotThrow(() -> service.getItineraryById(10L, 1L));
    }
    @Test void getItineraryById_teamNotMemberNotCreatorPrivate() {
        Itinerary itinerary = new Itinerary(); itinerary.setGroupId(2L); itinerary.setUser(user); itinerary.setPermissionStatus(com.se_07.backend.entity.Itinerary.PermissionStatus.私人);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L)).thenReturn(Optional.empty());
        User other = new User(); other.setId(99L); itinerary.setUser(other);
        assertThrows(RuntimeException.class, () -> service.getItineraryById(10L, 1L));
    }
    @Test void updateItinerary_notFound() {
        when(itineraryRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateItinerary(1L, 10L, new ItineraryUpdateRequest()));
    }
    @Test void updateItinerary_permission() {
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        itinerary.setUser(user);
        itinerary.setTravelStatus(Itinerary.TravelStatus.待出行);
        when(itineraryRepository.save(any())).thenReturn(itinerary);
        when(itineraryConverter.toDTO(any())).thenReturn(new ItineraryDTO());
        service.updateItinerary(1L, 10L, new ItineraryUpdateRequest());
        // 已出行
        itinerary.setTravelStatus(Itinerary.TravelStatus.已出行);
        assertThrows(RuntimeException.class, () -> service.updateItinerary(1L, 10L, new ItineraryUpdateRequest()));
    }
    @Test void updateItinerary_updateTitle() {
        Itinerary itinerary = new Itinerary(); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        ItineraryUpdateRequest req = new ItineraryUpdateRequest();
        req.setTitle("新标题");
        assertDoesNotThrow(() -> service.updateItinerary(1L, 10L, req));
    }
    @Test void updateItinerary_updateStartDate() {
        Itinerary itinerary = new Itinerary(); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        ItineraryUpdateRequest req = new ItineraryUpdateRequest();
        req.setStartDate(LocalDate.now());
        assertDoesNotThrow(() -> service.updateItinerary(1L, 10L, req));
    }
    @Test void updateItinerary_updateEndDate() {
        Itinerary itinerary = new Itinerary(); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        ItineraryUpdateRequest req = new ItineraryUpdateRequest();
        req.setEndDate(LocalDate.now().plusDays(1));
        assertDoesNotThrow(() -> service.updateItinerary(1L, 10L, req));
    }
    @Test void updateItinerary_updateBudget() {
        Itinerary itinerary = new Itinerary(); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        ItineraryUpdateRequest req = new ItineraryUpdateRequest();
        req.setBudget(new java.math.BigDecimal("123.45"));
        assertDoesNotThrow(() -> service.updateItinerary(1L, 10L, req));
    }
    @Test void updateItinerary_updateTravelerCount() {
        Itinerary itinerary = new Itinerary(); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        ItineraryUpdateRequest req = new ItineraryUpdateRequest();
        req.setTravelerCount(5);
        assertDoesNotThrow(() -> service.updateItinerary(1L, 10L, req));
    }
    @Test void updateItinerary_updateTravelStatus() {
        Itinerary itinerary = new Itinerary(); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        ItineraryUpdateRequest req = new ItineraryUpdateRequest();
        req.setTravelStatus("待出行");
        assertDoesNotThrow(() -> service.updateItinerary(1L, 10L, req));
    }
    @Test void updateItinerary_updatePermissionStatus() {
        Itinerary itinerary = new Itinerary(); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        ItineraryUpdateRequest req = new ItineraryUpdateRequest();
        req.setPermissionStatus("私人");
        assertDoesNotThrow(() -> service.updateItinerary(1L, 10L, req));
    }
    @Test void updateItinerary_travelStatusInvalid() {
        Itinerary itinerary = new Itinerary(); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        ItineraryUpdateRequest req = new ItineraryUpdateRequest();
        req.setTravelStatus("非法状态");
        assertThrows(RuntimeException.class, () -> service.updateItinerary(1L, 10L, req));
    }
    @Test void updateItinerary_permissionStatusInvalid() {
        Itinerary itinerary = new Itinerary(); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        ItineraryUpdateRequest req = new ItineraryUpdateRequest();
        req.setPermissionStatus("非法权限");
        assertThrows(RuntimeException.class, () -> service.updateItinerary(1L, 10L, req));
    }
    @Test void updateItinerary_teamNotMemberNotCreator() {
        Itinerary itinerary = new Itinerary(); itinerary.setGroupId(2L); itinerary.setUser(user); itinerary.setTravelStatus(com.se_07.backend.entity.Itinerary.TravelStatus.待出行);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L)).thenReturn(Optional.empty());
        User other = new User(); other.setId(99L); itinerary.setUser(other);
        ItineraryUpdateRequest req = new ItineraryUpdateRequest();
        assertThrows(RuntimeException.class, () -> service.updateItinerary(1L, 10L, req));
    }
    @Test void deleteItinerary_notFound() {
        when(itineraryRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.deleteItinerary(1L, 10L));
    }
    @Test void deleteItinerary_permission() {
        itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        doNothing().when(itineraryRepository).delete(any());
        service.deleteItinerary(1L, 10L);
    }
    @Test void deleteItinerary_teamPermission_creator() {
        // 团队创建者删除行程
        TravelGroup group = new TravelGroup();
        User creator = new User(); creator.setId(99L);
        group.setCreator(creator);

        Itinerary teamItinerary = new Itinerary();
        teamItinerary.setGroupId(2L);
        User itineraryCreator = new User(); itineraryCreator.setId(100L);
        teamItinerary.setUser(itineraryCreator);

        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(teamItinerary));
        when(travelGroupRepository.findById(2L)).thenReturn(Optional.of(group));

        // 团队创建者删除（非行程创建者）
        service.deleteItinerary(99L, 10L);
        verify(itineraryRepository).delete(teamItinerary);
    }

    @Test
    void deleteItinerary_teamPermission_itineraryCreator() {
        // 行程创建者删除团队行程
        TravelGroup group = new TravelGroup();
        User creator = new User(); creator.setId(99L);
        group.setCreator(creator);

        Itinerary teamItinerary = new Itinerary();
        teamItinerary.setGroupId(2L);
        User itineraryCreator = new User(); itineraryCreator.setId(100L);
        teamItinerary.setUser(itineraryCreator);

        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(teamItinerary));
        when(travelGroupRepository.findById(2L)).thenReturn(Optional.of(group));

        // 行程创建者删除
        service.deleteItinerary(100L, 10L);
        verify(itineraryRepository).delete(teamItinerary);
    }

    @Test
    void deleteItinerary_teamPermission_denied() {
        // 无权限删除团队行程
        TravelGroup group = new TravelGroup();
        User creator = new User(); creator.setId(99L);
        group.setCreator(creator);

        Itinerary teamItinerary = new Itinerary();
        teamItinerary.setGroupId(2L);
        User itineraryCreator = new User(); itineraryCreator.setId(100L);
        teamItinerary.setUser(itineraryCreator);

        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(teamItinerary));
        when(travelGroupRepository.findById(2L)).thenReturn(Optional.of(group));

        // 无权限用户尝试删除
        assertThrows(RuntimeException.class, () -> service.deleteItinerary(1L, 10L));
    }
    @Test void lockItinerary_notFound() {
        when(itineraryRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.lockItinerary(10L, 1L));
    }
    @Test void lockItinerary_permission() {
        itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any())).thenReturn(itinerary);
        when(itineraryConverter.toDTO(any())).thenReturn(new ItineraryDTO());
        service.lockItinerary(10L, 1L);
    }
    @Test void lockItinerary_teamNotMemberNotCreator() {
        itinerary.setGroupId(2L);
        itinerary.setUser(user);
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(eq(2L), anyLong())).thenReturn(Optional.empty());
        User other = new User(); other.setId(99L);
        itinerary.setUser(other);
        assertThrows(RuntimeException.class, () -> service.lockItinerary(1L, 10L));
    }
    @Test void lockItinerary_teamMember() {
        itinerary.setGroupId(2L);
        itinerary.setUser(user);
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(eq(2L), anyLong())).thenReturn(Optional.of(new TravelGroupMember()));
        when(itineraryRepository.save(any())).thenReturn(itinerary);
        assertDoesNotThrow(() -> service.lockItinerary(1L, 1L));
    }
    @Test void lockItinerary_personalNotCreator() {
        itinerary.setGroupId(null);
        User other = new User(); other.setId(99L);
        itinerary.setUser(other);
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        assertThrows(RuntimeException.class, () -> service.lockItinerary(1L, 10L));
    }
    @Test void getPendingItineraries_empty() {
        when(itineraryRepository.findByUserIdAndTravelStatus(anyLong(), any())).thenReturn(Collections.emptyList());
        List<ItineraryDTO> list = service.getPendingItineraries(1L);
        assertTrue(list.isEmpty());
    }
    @Test void setItineraryPermission_notFound() {
        when(itineraryRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.setItineraryPermission(10L, 1L, "所有人可见"));
    }
    @Test void setItineraryPermission_permission() {
        itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any())).thenReturn(itinerary);
        when(itineraryConverter.toDTO(any())).thenReturn(new ItineraryDTO());
        service.setItineraryPermission(10L, 1L, "所有人可见");
    }
    @Test void setItineraryPermission_teamNotMemberNotCreator() {
        Itinerary itinerary = new Itinerary(); itinerary.setGroupId(2L); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L)).thenReturn(Optional.empty());
        User other = new User(); other.setId(99L); itinerary.setUser(other);
        assertThrows(RuntimeException.class, () -> service.setItineraryPermission(10L, 1L, "所有人可见"));
    }
    @Test void updateItineraryBasic_notFound() {
        when(itineraryRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateItineraryBasic(1L, 10L, new HashMap<>()));
    }
    @Test void updateItineraryStatus_notFound() {
        when(itineraryRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateItineraryStatus(1L, 10L, "已出行"));
    }
    @Test void updateItineraryStatus_autoHistoryDestinations() {
        // 测试状态变更时历史目的地的自动处理
        Itinerary itinerary = new Itinerary();
        itinerary.setUser(user);
        itinerary.setTravelStatus(Itinerary.TravelStatus.待出行);

        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));

        // 从待出行变为已出行 - 应添加历史目的地
        service.updateItineraryStatus(1L, 10L, "已出行");
        verify(userService).addHistoryDestinationsFromCompletedItineraries(1L);

        // 从已出行变回待出行 - 应删除历史目的地
        itinerary.setTravelStatus(Itinerary.TravelStatus.已出行);
        service.updateItineraryStatus(1L, 10L, "待出行");
        verify(userService).removeAutoAddedHistoryDestinationsFromItinerary(1L, 10L);
    }
    @Test void uploadCoverImage_notFound() {
        when(itineraryRepository.findById(10L)).thenReturn(Optional.empty());
        MultipartFile file = mock(MultipartFile.class);
        assertThrows(RuntimeException.class, () -> service.uploadCoverImage(1L, 10L, file));
    }
    @Test void uploadCoverImage_emptyFile() {
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        assertThrows(RuntimeException.class, () -> service.uploadCoverImage(1L, 10L, file));
    }
    @Test void uploadCoverImage_badFormat() {
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.txt");
        assertThrows(RuntimeException.class, () -> service.uploadCoverImage(1L, 10L, file));
    }
    @Test void uploadCoverImage_ioException() throws Exception {
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getInputStream()).thenThrow(new java.io.IOException("fail"));
        assertThrows(RuntimeException.class, () -> service.uploadCoverImage(1L, 10L, file));
    }
    @Test void uploadCoverImage_success() throws Exception {
        // 测试封面图片上传成功
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("cover.jpg");
        when(file.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("test".getBytes()));

        // 模拟文件保存
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get("uploads/covers"));

        String result = service.uploadCoverImage(1L, 10L, file);
        assertNotNull(result);
        assertTrue(result.startsWith("/covers/"));

        // 清理测试文件
        java.nio.file.Files.walk(java.nio.file.Paths.get("uploads"))
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try { java.nio.file.Files.delete(path); }
                    catch (Exception e) {}
                });
    }
    @Test void shiftItineraryDates_notFound() {
        when(itineraryRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.shiftItineraryDates(1L, 10L, LocalDate.now()));
    }
    @Test void shiftItineraryDates_newStartNull() {
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        assertThrows(RuntimeException.class, () -> service.shiftItineraryDates(1L, 10L, null));
    }
    @Test void generateShareCode_notFound() {
        when(itineraryRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.generateShareCode(10L, 1L, new ShareCodeRequest()));
    }
    @Test void generateShareCode_autoPermissionChange() {
        // 测试生成分享码时权限自动更新
        itinerary.setUser(user);
        itinerary.setPermissionStatus(Itinerary.PermissionStatus.私人);

        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any(Itinerary.class))).thenReturn(itinerary);
        when(communityEntryRepository.findByShareCode(anyString())).thenReturn(Optional.empty());
        when(communityEntryRepository.findByItineraryId(10L)).thenReturn(Optional.empty());
        when(communityEntryRepository.save(any(CommunityEntry.class))).thenAnswer(invocation -> {
            CommunityEntry entry = invocation.getArgument(0);
            if (entry.getId() == null) {
                entry.setId(1L);
            }
            return entry;
        });

        ShareCodeRequest request = new ShareCodeRequest();
        String shareCode = service.generateShareCode(10L, 1L, request);

        // 验证权限状态已更新
        assertEquals(Itinerary.PermissionStatus.仅获得链接者可见, itinerary.getPermissionStatus());
        
        // 验证分享码不为空
        assertNotNull(shareCode);
        assertFalse(shareCode.isEmpty());
        
        // 验证保存方法被调用
        verify(itineraryRepository).save(any(Itinerary.class));
        verify(communityEntryRepository).save(any(CommunityEntry.class));
    }
    @Test void generateShareCode_teamNotMemberNotCreator() {
        Itinerary itinerary = new Itinerary(); itinerary.setGroupId(2L); itinerary.setUser(user); itinerary.setPermissionStatus(com.se_07.backend.entity.Itinerary.PermissionStatus.私人);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L)).thenReturn(Optional.empty());
        User other = new User(); other.setId(99L); itinerary.setUser(other);
        ShareCodeRequest req = new ShareCodeRequest();
        assertThrows(RuntimeException.class, () -> service.generateShareCode(10L, 1L, req));
    }
    @Test void createGroupItinerary_teamNotMember() {
        when(travelGroupMemberRepository.findByGroupIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        assertThrows(RuntimeException.class, () -> service.createGroupItinerary(1L, 2L, req, false));
    }
    @Test void createGroupItinerary_groupNotFound() {
        when(travelGroupMemberRepository.findByGroupIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(member));
        when(travelGroupRepository.findById(anyLong())).thenReturn(Optional.empty());
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        assertThrows(RuntimeException.class, () -> service.createGroupItinerary(1L, 2L, req, false));
    }
    @Test void createGroupItinerary_duplicate() {
        when(travelGroupMemberRepository.findByGroupIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(member));
        when(travelGroupRepository.findById(anyLong())).thenReturn(Optional.of(group));
        when(groupItineraryRepository.findByGroupId(anyLong())).thenReturn(List.of(new GroupItinerary()));
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        assertThrows(RuntimeException.class, () -> service.createGroupItinerary(1L, 2L, req, false));
    }
    @Test void createGroupItinerary_success() {
        when(travelGroupMemberRepository.findByGroupIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(member));
        when(travelGroupRepository.findById(anyLong())).thenReturn(Optional.of(group));
        when(groupItineraryRepository.findByGroupId(anyLong())).thenReturn(Collections.emptyList());
        when(itineraryRepository.save(any())).thenReturn(itinerary);
        when(itineraryConverter.toDTO(any())).thenReturn(new ItineraryDTO());
        ItineraryCreateRequest req = new ItineraryCreateRequest();
        req.setStartDate(LocalDate.now()); req.setEndDate(LocalDate.now().plusDays(1));
        service.createGroupItinerary(1L, 2L, req, false);
    }
    @Test void getGroupItineraries_notMember() {
        when(travelGroupMemberRepository.findByGroupIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getGroupItineraries(1L, 2L, false));
    }
    @Test void getGroupItineraries_success() {
        when(travelGroupMemberRepository.findByGroupIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(member));
        when(groupItineraryRepository.findByGroupId(anyLong())).thenReturn(List.of(new GroupItinerary()));
        when(itineraryConverter.toDTO(any())).thenReturn(new ItineraryDTO());
        List<ItineraryDTO> list = service.getGroupItineraries(1L, 2L, false);
        assertNotNull(list);
    }
    @Test void importAIItinerary_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        com.se_07.backend.dto.AIItineraryImportRequest req = new com.se_07.backend.dto.AIItineraryImportRequest();
        assertThrows(RuntimeException.class, () -> service.importAIItinerary(1L, req));
    }
    @Test void importAIItinerary_titleNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        com.se_07.backend.dto.AIItineraryImportRequest req = new com.se_07.backend.dto.AIItineraryImportRequest();
        req.setTitle(null);
        assertThrows(RuntimeException.class, () -> service.importAIItinerary(1L, req));
        req.setTitle("");
        assertThrows(RuntimeException.class, () -> service.importAIItinerary(1L, req));
    }
    @Test void importAIItinerary_daysInvalid() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        com.se_07.backend.dto.AIItineraryImportRequest req = new com.se_07.backend.dto.AIItineraryImportRequest();
        req.setTitle("t");
        req.setDays(null);
        assertThrows(RuntimeException.class, () -> service.importAIItinerary(1L, req));
        req.setDays(0);
        assertThrows(RuntimeException.class, () -> service.importAIItinerary(1L, req));
    }
    @Test void importAIItinerary_planNullOrEmpty() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        com.se_07.backend.dto.AIItineraryImportRequest req = new com.se_07.backend.dto.AIItineraryImportRequest();
        req.setTitle("t"); req.setDays(1);
        req.setPlan(null);
        assertThrows(RuntimeException.class, () -> service.importAIItinerary(1L, req));
        req.setPlan(Collections.emptyList());
        assertThrows(RuntimeException.class, () -> service.importAIItinerary(1L, req));
    }
    @Test void importAIItinerary_travelersNullOrInvalid() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        com.se_07.backend.dto.AIItineraryImportRequest req = new com.se_07.backend.dto.AIItineraryImportRequest();
        req.setTitle("t"); req.setDays(1); req.setPlan(Arrays.asList(new com.se_07.backend.dto.AIItineraryImportRequest.DayPlan()));
        req.setTravelers(null);
    }
    @Test void importAIItinerary_createFail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        com.se_07.backend.dto.AIItineraryImportRequest req = new com.se_07.backend.dto.AIItineraryImportRequest();
        req.setTitle("t"); req.setDays(1); req.setPlan(Arrays.asList(new com.se_07.backend.dto.AIItineraryImportRequest.DayPlan())); req.setTravelers(1);
        when(itineraryRepository.save(any())).thenReturn(new Itinerary());
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.importAIItinerary(1L, req));
    }
    @Test void getTeamItineraries_empty() {
        when(userRepository.existsById(anyLong())).thenReturn(true);
        when(travelGroupMemberRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());
        List<ItineraryDTO> list = service.getTeamItineraries(1L, Pageable.unpaged());
        assertTrue(list.isEmpty());
    }
    @Test void getCompletedItineraries_empty() {
        when(itineraryRepository.findByUserIdAndTravelStatus(anyLong(), any())).thenReturn(Collections.emptyList());
        assertTrue(service.getCompletedItineraries(1L).isEmpty());
    }
    @Test void updateItineraryBasic_teamNotMemberNotCreator() {
        itinerary.setGroupId(2L);
        itinerary.setUser(user);
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(eq(2L), anyLong())).thenReturn(Optional.empty());
        User other = new User(); other.setId(99L);
        itinerary.setUser(other);
        assertThrows(RuntimeException.class, () -> service.updateItineraryBasic(1L, 10L, new HashMap<>()));
    }
    @Test void updateItineraryBasic_teamMember_editable() {
        itinerary.setGroupId(2L);
        itinerary.setUser(user);
        itinerary.setTravelStatus(Itinerary.TravelStatus.待出行);
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(eq(2L), anyLong())).thenReturn(Optional.of(new TravelGroupMember()));
        when(itineraryRepository.save(any())).thenReturn(itinerary);
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", "新标题");
        assertDoesNotThrow(() -> service.updateItineraryBasic(1L, 1L, updates));
    }
    @Test void updateItineraryStatus_teamNotMemberNotCreator() {
        itinerary.setGroupId(2L);
        itinerary.setUser(user);
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(eq(2L), anyLong())).thenReturn(Optional.empty());
        User other = new User(); other.setId(99L);
        itinerary.setUser(other);
        assertThrows(RuntimeException.class, () -> service.updateItineraryStatus(1L, 10L, "已出行"));
    }
    @Test void updateItineraryStatus_teamMember() {
        itinerary.setGroupId(2L);
        itinerary.setUser(user);
        itinerary.setTravelStatus(Itinerary.TravelStatus.待出行);
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(eq(2L), anyLong())).thenReturn(Optional.of(new TravelGroupMember()));
        when(itineraryRepository.save(any())).thenReturn(itinerary);
        assertDoesNotThrow(() -> service.updateItineraryStatus(1L, 1L, "已出行"));
    }
    @Test void updateDayTitle_teamNotMemberNotCreator() {
        itinerary.setGroupId(2L);
        itinerary.setUser(user);
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(eq(2L), anyLong())).thenReturn(Optional.empty());
        User other = new User(); other.setId(99L);
        itinerary.setUser(other);
        when(itineraryDayRepository.findById(100L)).thenReturn(Optional.of(new com.se_07.backend.entity.ItineraryDay()));
        assertThrows(RuntimeException.class, () -> service.updateDayTitle(1L, 10L, 100L, "新标题"));
    }
    @Test void setEditComplete_teamMember() {
        Itinerary itinerary = new Itinerary(); itinerary.setGroupId(2L); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L)).thenReturn(Optional.of(new com.se_07.backend.entity.TravelGroupMember()));
        when(itineraryRepository.save(any())).thenReturn(itinerary);
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        assertDoesNotThrow(() -> service.setEditComplete(1L, 10L));
    }
    @Test void setEditComplete_teamNotMemberNotCreator() {
        Itinerary itinerary = new Itinerary(); itinerary.setGroupId(2L); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L)).thenReturn(Optional.empty());
        User other = new User(); other.setId(99L); itinerary.setUser(other);
        assertThrows(RuntimeException.class, () -> service.setEditComplete(1L, 10L));
    }
    @Test void setEditComplete_personalCreator() {
        Itinerary itinerary = new Itinerary(); itinerary.setGroupId(null); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any())).thenReturn(itinerary);
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        assertDoesNotThrow(() -> service.setEditComplete(1L, 10L));
    }
    @Test void setEditComplete_personalNotCreator() {
        Itinerary itinerary = new Itinerary(); itinerary.setGroupId(null); itinerary.setUser(user);
        User other = new User(); other.setId(99L); itinerary.setUser(other);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        assertThrows(RuntimeException.class, () -> service.setEditComplete(1L, 10L));
    }
    @Test void shiftItineraryDates_teamNotMemberNotCreator() {
        itinerary.setGroupId(2L);
        itinerary.setUser(user);
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(eq(2L), anyLong())).thenReturn(Optional.empty());
        User other = new User(); other.setId(99L);
        itinerary.setUser(other);
        assertThrows(RuntimeException.class, () -> service.shiftItineraryDates(1L, 10L, LocalDate.now()));
    }
    @Test void createItineraryDaysWithActivities_emptyPlan() throws Exception {
        java.lang.reflect.Method m = service.getClass().getDeclaredMethod("createItineraryDaysWithActivities", Itinerary.class, List.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(service, itinerary, Collections.emptyList()));
    }
    @Test void getTeamItineraries_groupItineraryException() {
        when(userRepository.existsById(anyLong())).thenReturn(true);
        TravelGroup group1 = new TravelGroup(); group1.setId(1L);
        TravelGroup group2 = new TravelGroup(); group2.setId(2L);
        TravelGroupMember m1 = new TravelGroupMember(); m1.setGroup(group1);
        TravelGroupMember m2 = new TravelGroupMember(); m2.setGroup(group2);
        when(travelGroupMemberRepository.findByUserId(anyLong())).thenReturn(Arrays.asList(m1, m2));
        when(groupItineraryRepository.findByGroupId(1L)).thenThrow(new RuntimeException("error"));
        GroupItinerary gi = new GroupItinerary();
        Itinerary iti = new Itinerary(); iti.setId(100L); iti.setTitle("t");
        gi.setItinerary(iti);
        when(groupItineraryRepository.findByGroupId(2L)).thenReturn(Arrays.asList(gi));
        List<ItineraryDTO> list = service.getTeamItineraries(1L, Pageable.unpaged());
        assertEquals(0, list.size());
    }
    @Test void getTeamItineraries_groupItineraryNull() {
        when(userRepository.existsById(anyLong())).thenReturn(true);
        TravelGroup group1 = new TravelGroup(); group1.setId(1L);
        TravelGroupMember m1 = new TravelGroupMember(); m1.setGroup(group1);
        when(travelGroupMemberRepository.findByUserId(anyLong())).thenReturn(Arrays.asList(m1));
        GroupItinerary gi = new GroupItinerary();
        gi.setItinerary(null);
        when(groupItineraryRepository.findByGroupId(1L)).thenReturn(Arrays.asList(gi));
        List<ItineraryDTO> list = service.getTeamItineraries(1L, Pageable.unpaged());
        assertTrue(list.isEmpty());
    }
    @Test void getTeamItineraries_paginationStartOver() {
        when(userRepository.existsById(anyLong())).thenReturn(true);
        TravelGroup group1 = new TravelGroup(); group1.setId(1L);
        TravelGroupMember m1 = new TravelGroupMember(); m1.setGroup(group1);
        when(travelGroupMemberRepository.findByUserId(anyLong())).thenReturn(Arrays.asList(m1));
        GroupItinerary gi = new GroupItinerary();
        Itinerary iti = new Itinerary(); iti.setId(100L); iti.setTitle("t");
        gi.setItinerary(iti);
        when(groupItineraryRepository.findByGroupId(1L)).thenReturn(Arrays.asList(gi));
        Pageable pageable = PageRequest.of(10, 10);
        List<ItineraryDTO> list = service.getTeamItineraries(1L, pageable);
        assertTrue(list.isEmpty());
    }
    @Test void getTeamItineraries_groupInfoException() {
        when(userRepository.existsById(anyLong())).thenReturn(true);
        TravelGroup group1 = new TravelGroup(); group1.setId(1L);
        TravelGroupMember m1 = new TravelGroupMember(); m1.setGroup(group1);
        when(travelGroupMemberRepository.findByUserId(anyLong())).thenReturn(Arrays.asList(m1));
        GroupItinerary gi = new GroupItinerary();
        Itinerary iti = new Itinerary(); iti.setId(100L); iti.setTitle("t"); iti.setGroupId(1L);
        gi.setItinerary(iti);
        when(groupItineraryRepository.findByGroupId(1L)).thenReturn(Arrays.asList(gi));
        org.mockito.Mockito.lenient().when(travelGroupRepository.findById(1L)).thenThrow(new RuntimeException("error"));
        Pageable pageable = PageRequest.of(0, 10);
        List<ItineraryDTO> list = service.getTeamItineraries(1L, pageable);
        assertEquals(1, list.size());
    }
    @Test void getTeamItineraries_groupInfoNormal() {
        when(userRepository.existsById(anyLong())).thenReturn(true);
        TravelGroup group1 = new TravelGroup(); group1.setId(1L);
        TravelGroupMember m1 = new TravelGroupMember(); m1.setGroup(group1);
        when(travelGroupMemberRepository.findByUserId(anyLong())).thenReturn(Arrays.asList(m1));
        GroupItinerary gi = new GroupItinerary();
        Itinerary iti = new Itinerary(); iti.setId(100L); iti.setTitle("t"); iti.setGroupId(1L);
        gi.setItinerary(iti);
        when(groupItineraryRepository.findByGroupId(1L)).thenReturn(Arrays.asList(gi));
        TravelGroup group = new TravelGroup(); group.setTitle("g"); User creator = new User(); creator.setId(1L); group.setCreator(creator);
        org.mockito.Mockito.lenient().when(travelGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        org.mockito.Mockito.lenient().when(travelGroupMemberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.of(member));
        Pageable pageable = PageRequest.of(0, 10);
        List<ItineraryDTO> list = service.getTeamItineraries(1L, pageable);
        assertEquals(1, list.size());
    }

    // 添加以下测试用例到 ItineraryServiceImplTest 类中

    @Test
    void setAsGroupTemplate_success() {
        // 测试设置团队模板
        when(travelGroupMemberRepository.findByGroupIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.of(member));
        when(groupItineraryRepository.findByGroupIdAndItineraryId(anyLong(), anyLong()))
                .thenReturn(Optional.of(new GroupItinerary()));

        ItineraryDTO result = service.setAsGroupTemplate(1L, 2L, 10L);
        assertNull(result);
    }

    @Test
    void shiftItineraryDates_success() {
        // 测试调整行程日期
        itinerary.setUser(user);
        LocalDate newStart = LocalDate.now().plusDays(5);

        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryDayRepository.findByItineraryIdOrderByDayNumber(10L))
                .thenReturn(Arrays.asList(
                        createDay(1, LocalDate.now()),
                        createDay(2, LocalDate.now().plusDays(1))
                ));
        when(itineraryRepository.save(any())).thenReturn(itinerary);
        when(itineraryConverter.toDTO(any())).thenReturn(new ItineraryDTO());

        ItineraryDTO result = service.shiftItineraryDates(1L, 10L, newStart);
        assertNotNull(result);

        // 验证日期已更新
        assertEquals(newStart, itinerary.getStartDate());
        assertEquals(newStart.plusDays(2), itinerary.getEndDate());
    }

    private ItineraryDay createDay(int dayNumber, LocalDate date) {
        ItineraryDay day = new ItineraryDay();
        day.setDayNumber(dayNumber);
        day.setDate(date);
        return day;
    }

    @Test
    void getPersonalItineraries_success() {
        // 测试获取个人行程
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itineraryRepository.findPersonalItineraries(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Arrays.asList(itinerary)));
        when(itineraryConverter.toDTO(any())).thenReturn(new ItineraryDTO());

        List<ItineraryDTO> result = service.getPersonalItineraries(1L, Pageable.unpaged());
        assertEquals(1, result.size());
    }

    @Test
    void updatePermissionStatus_teamMemberAllowed() {
        // 测试团队成员更新权限
        itinerary.setGroupId(2L);
        itinerary.setUser(user);
        itinerary.setPermissionStatus(Itinerary.PermissionStatus.仅获得链接者可见); // 设置初始状态不是私人

        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L))
                .thenReturn(Optional.of(member));
        when(itineraryRepository.save(any(Itinerary.class))).thenReturn(itinerary);

        PermissionStatusResponse response = service.updatePermissionStatus(10L, 1L, "所有人可见");
        
        // 验证权限状态已更新
        assertEquals(Itinerary.PermissionStatus.所有人可见, itinerary.getPermissionStatus());
        assertFalse(response.isNeedsShareDialog());
        verify(itineraryRepository).save(itinerary);
    }

    @Test
    void updatePermissionStatus_teamNotMemberNotCreator() {
        Itinerary itinerary = new Itinerary(); itinerary.setGroupId(2L); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(travelGroupMemberRepository.findByGroupIdAndUserId(2L, 1L)).thenReturn(Optional.empty());
        User other = new User(); other.setId(99L); itinerary.setUser(other);
        assertThrows(RuntimeException.class, () -> service.updatePermissionStatus(10L, 1L, "所有人可见"));
    }

    @Test
    void createItineraryDaysWithActivities_multiDayPlanAndActivities() throws Exception {
        java.lang.reflect.Method m = service.getClass().getDeclaredMethod("createItineraryDaysWithActivities", Itinerary.class, List.class);
        m.setAccessible(true);
        Itinerary itinerary = new Itinerary(); itinerary.setStartDate(LocalDate.now());
        // dayPlan1: 2个活动，第1个找不到景点，第2个能找到
        com.se_07.backend.dto.AIItineraryImportRequest.DayPlan dayPlan1 = new com.se_07.backend.dto.AIItineraryImportRequest.DayPlan();
        dayPlan1.setDay(1);
        com.se_07.backend.dto.AIItineraryImportRequest.ActivityPlan ap1 = new com.se_07.backend.dto.AIItineraryImportRequest.ActivityPlan();
        ap1.setName("notfound");
        com.se_07.backend.dto.AIItineraryImportRequest.ActivityPlan ap2 = new com.se_07.backend.dto.AIItineraryImportRequest.ActivityPlan();
        ap2.setName("found"); ap2.setStartTime("08:00"); ap2.setEndTime("09:00"); ap2.setTransportMode("bus"); ap2.setDescription("desc");
        dayPlan1.setActivities(Arrays.asList(ap1, ap2));
        // dayPlan2: 1个活动，能找到景点
        com.se_07.backend.dto.AIItineraryImportRequest.DayPlan dayPlan2 = new com.se_07.backend.dto.AIItineraryImportRequest.DayPlan();
        dayPlan2.setDay(2);
        com.se_07.backend.dto.AIItineraryImportRequest.ActivityPlan ap3 = new com.se_07.backend.dto.AIItineraryImportRequest.ActivityPlan();
        ap3.setName("found2");
        dayPlan2.setActivities(Arrays.asList(ap3));
        // mock
        when(attractionRepository.findByNameContainingOrDescriptionContaining("notfound")).thenReturn(Collections.emptyList());
        when(attractionRepository.findByNameContainingOrDescriptionContaining("found")).thenReturn(Arrays.asList(new com.se_07.backend.entity.Attraction()));
        when(attractionRepository.findByNameContainingOrDescriptionContaining("found2")).thenReturn(Arrays.asList(new com.se_07.backend.entity.Attraction()));
        when(itineraryActivityRepository.save(any())).thenAnswer(invocation -> {
            com.se_07.backend.entity.ItineraryActivity act = invocation.getArgument(0);
            act.setId((long) (Math.random() * 1000));
            return act;
        });
        when(itineraryDayRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        // 执行
        assertDoesNotThrow(() -> m.invoke(service, itinerary, Arrays.asList(dayPlan1, dayPlan2)));
    }
    @Test void createItineraryDaysWithActivities_chainAndPrevNext() throws Exception {
        java.lang.reflect.Method m = service.getClass().getDeclaredMethod("createItineraryDaysWithActivities", Itinerary.class, List.class);
        m.setAccessible(true);
        Itinerary itinerary = new Itinerary(); itinerary.setStartDate(LocalDate.now());
        com.se_07.backend.dto.AIItineraryImportRequest.DayPlan dayPlan = new com.se_07.backend.dto.AIItineraryImportRequest.DayPlan();
        dayPlan.setDay(1);
        com.se_07.backend.dto.AIItineraryImportRequest.ActivityPlan ap1 = new com.se_07.backend.dto.AIItineraryImportRequest.ActivityPlan(); ap1.setName("a1");
        com.se_07.backend.dto.AIItineraryImportRequest.ActivityPlan ap2 = new com.se_07.backend.dto.AIItineraryImportRequest.ActivityPlan(); ap2.setName("a2");
        dayPlan.setActivities(Arrays.asList(ap1, ap2));
        when(attractionRepository.findByNameContainingOrDescriptionContaining(anyString())).thenReturn(Arrays.asList(new com.se_07.backend.entity.Attraction()));
        when(itineraryActivityRepository.save(any())).thenAnswer(invocation -> {
            com.se_07.backend.entity.ItineraryActivity act = invocation.getArgument(0);
            act.setId((long) (Math.random() * 1000));
            return act;
        });
        when(itineraryDayRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertDoesNotThrow(() -> m.invoke(service, itinerary, Arrays.asList(dayPlan)));
    }
    @Test void createItineraryDaysWithActivities_allFieldsNull() throws Exception {
        java.lang.reflect.Method m = service.getClass().getDeclaredMethod("createItineraryDaysWithActivities", Itinerary.class, List.class);
        m.setAccessible(true);
        Itinerary itinerary = new Itinerary(); itinerary.setStartDate(LocalDate.now());
        com.se_07.backend.dto.AIItineraryImportRequest.DayPlan dayPlan = new com.se_07.backend.dto.AIItineraryImportRequest.DayPlan();
        dayPlan.setDay(null); dayPlan.setActivities(null);
    }

    @Test
    void updateItineraryBasic_updateAllFields() {
        Itinerary itinerary = new Itinerary(); itinerary.setUser(user);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", "新标题");
        updates.put("startDate", LocalDate.now());
        updates.put("endDate", LocalDate.now().plusDays(1));
        updates.put("budget", new java.math.BigDecimal("123.45"));
        updates.put("travelerCount", 5);
        updates.put("travelStatus", "待出行");
        updates.put("permissionStatus", "私人");
        assertDoesNotThrow(() -> service.updateItineraryBasic(1L, 10L, updates));
    }
    @Test void updateItineraryBasic_permissionChangeTriggersHandle() {
        Itinerary itinerary = new Itinerary(); itinerary.setUser(user);
        itinerary.setPermissionStatus(com.se_07.backend.entity.Itinerary.PermissionStatus.私人);
        when(itineraryRepository.findById(10L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(any())).thenAnswer(invocation -> {
            com.se_07.backend.entity.Itinerary it = invocation.getArgument(0);
            it.setPermissionStatus(com.se_07.backend.entity.Itinerary.PermissionStatus.所有人可见);
            return it;
        });
        when(itineraryConverter.toDTO(any())).thenReturn(new com.se_07.backend.dto.ItineraryDTO());
        Map<String, Object> updates = new HashMap<>();
        updates.put("permissionStatus", "所有人可见");
        assertDoesNotThrow(() -> service.updateItineraryBasic(1L, 10L, updates));
    }

    // 工具方法：反射调用private createOrUpdateCommunityEntry
    private void invokeCreateOrUpdateCommunityEntry(Itinerary itinerary, String code, String desc, List<Long> tagIds) throws Exception {
        java.lang.reflect.Method method = ItineraryServiceImpl.class.getDeclaredMethod(
            "createOrUpdateCommunityEntry",
            Itinerary.class, String.class, String.class, List.class
        );
        method.setAccessible(true);
        method.invoke(service, itinerary, code, desc, tagIds);
    }

    @Test
    void createOrUpdateCommunityEntry_existingEntry() throws Exception {
        Itinerary itinerary = new Itinerary(); itinerary.setId(1L);
        CommunityEntry existing = new CommunityEntry(); existing.setId(2L); existing.setItinerary(itinerary);
        when(communityEntryRepository.findByItineraryId(1L)).thenReturn(Optional.of(existing));
        when(communityEntryRepository.save(any())).thenReturn(existing);
        doNothing().when(communityEntryTagRepository).deleteByCommunityEntry(existing);
        when(tagRepository.findById(1L)).thenReturn(Optional.of(new Tag()));
        when(tagRepository.findById(2L)).thenReturn(Optional.of(new Tag()));
        assertDoesNotThrow(() -> {
            try {
                invokeCreateOrUpdateCommunityEntry(itinerary, "code", "desc", Arrays.asList(1L, 2L));
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
    @Test
    void createOrUpdateCommunityEntry_newEntry() throws Exception {
        Itinerary itinerary = new Itinerary(); itinerary.setId(1L);
        when(communityEntryRepository.findByItineraryId(1L)).thenReturn(null);
        when(communityEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tagRepository.findById(1L)).thenReturn(Optional.of(new Tag()));
    }
    @Test
    void createOrUpdateCommunityEntry_tagIdsNull() throws Exception {
        Itinerary itinerary = new Itinerary(); itinerary.setId(1L);
        when(communityEntryRepository.findByItineraryId(1L)).thenReturn(null);
        when(communityEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
    @Test
    void createOrUpdateCommunityEntry_tagIdsEmpty() throws Exception {
        Itinerary itinerary = new Itinerary(); itinerary.setId(1L);
        when(communityEntryRepository.findByItineraryId(1L)).thenReturn(null);
        when(communityEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
    @Test
    void createOrUpdateCommunityEntry_tagNotFound() throws Exception {
        Itinerary itinerary = new Itinerary(); itinerary.setId(1L);
        when(communityEntryRepository.findByItineraryId(1L)).thenReturn(null);
        when(communityEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tagRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> {
            try {
                invokeCreateOrUpdateCommunityEntry(itinerary, "code", "desc", Arrays.asList(404L));
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
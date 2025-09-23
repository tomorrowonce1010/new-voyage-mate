package com.se_07.backend.service;

import com.se_07.backend.dto.*;
import com.se_07.backend.entity.*;
import com.se_07.backend.repository.*;
import com.se_07.backend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private UserDestinationRepository userDestinationRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private ItineraryRepository itineraryRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserProfile testProfile;
    private UserPreferences testPreferences;
    private Destination testDestination;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setCreatedAt(LocalDateTime.now());

        // 创建测试用户档案
        testProfile = new UserProfile();
        testProfile.setId(1L);
        testProfile.setUser(testUser);
        testProfile.setAvatarUrl("avatar.jpg");
        testProfile.setBirthday(LocalDate.of(1990, 1, 1));
        testProfile.setSignature("测试签名");
        testProfile.setBio("测试简介");

        // 创建测试用户偏好
        testPreferences = new UserPreferences();
        testPreferences.setId(1L);
        testPreferences.setUser(testUser);
        testPreferences.setTravelPreferences("[\"美食\",\"文化\"]");
        testPreferences.setSpecialRequirements("[\"无障碍设施\"]");
        testPreferences.setSpecialRequirementsDescription("需要无障碍设施");

        // 创建测试目的地
        testDestination = new Destination();
        testDestination.setId(1L);
        testDestination.setName("北京");
        testDestination.setDescription("首都");
        testDestination.setImageUrl("beijing.jpg");
    }

    @Test
    void testGetUserProfile_UserExists_WithProfileAndPreferences() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), any()))
                .thenReturn(new ArrayList<>());
        when(tagRepository.findAllByOrderById()).thenReturn(new ArrayList<>());

        // Act
        UserProfileResponse response = userService.getUserProfile(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("avatar.jpg", response.getAvatarUrl());
        assertEquals(LocalDate.of(1990, 1, 1), response.getBirthday());
        assertEquals("测试签名", response.getSignature());
        assertEquals("测试简介", response.getBio());
        assertNotNull(response.getTravelPreferences());
        assertNotNull(response.getSpecialRequirements());
        assertEquals("需要无障碍设施", response.getSpecialRequirementsDescription());
    }

    @Test
    void testGetUserProfile_UserExists_WithoutProfileAndPreferences() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(tagRepository.findAllByOrderById()).thenReturn(new ArrayList<>());

        // Act
        UserProfileResponse response = userService.getUserProfile(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
        assertNull(response.getAvatarUrl());
        assertNull(response.getBirthday());
        assertNull(response.getSignature());
        assertNull(response.getBio());
        assertNotNull(response.getTravelPreferences());
        assertTrue(response.getTravelPreferences().isEmpty());
        assertNotNull(response.getSpecialRequirements());
        assertTrue(response.getSpecialRequirements().isEmpty());
        assertNull(response.getSpecialRequirementsDescription());
    }

    @Test
    void testGetUserProfile_UserNotExists() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.getUserProfile(999L));
    }

    @Test
    void testUpdateUserProfile_NewProfile() {
        // Arrange
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setUsername("newusername");
        request.setAvatarUrl("newavatar.jpg");
        request.setBirthday(LocalDate.of(1995, 5, 5));
        request.setSignature("新签名");
        request.setBio("新简介");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(tagRepository.findAllByOrderById()).thenReturn(new ArrayList<>());

        // Act
        UserProfileResponse response = userService.updateUserProfile(1L, request);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(any(User.class));
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void testUpdateUserProfile_ExistingProfile() {
        // Arrange
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setUsername("newusername");
        request.setAvatarUrl("newavatar.jpg");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(tagRepository.findAllByOrderById()).thenReturn(new ArrayList<>());

        // Act
        UserProfileResponse response = userService.updateUserProfile(1L, request);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(any(User.class));
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void testUpdateUserProfile_UsernameEmpty() {
        // Arrange
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setUsername("   ");
        request.setAvatarUrl("newavatar.jpg");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(tagRepository.findAllByOrderById()).thenReturn(new ArrayList<>());

        // Act
        UserProfileResponse response = userService.updateUserProfile(1L, request);

        // Assert
        assertNotNull(response);
        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void testUpdateUserProfile_UsernameNull() {
        // Arrange
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setUsername(null);
        request.setAvatarUrl("newavatar.jpg");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(tagRepository.findAllByOrderById()).thenReturn(new ArrayList<>());

        // Act
        UserProfileResponse response = userService.updateUserProfile(1L, request);

        // Assert
        assertNotNull(response);
        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void testUpdateUserPreferences_NewPreferences() {
        // Arrange
        UserPreferencesUpdateRequest request = new UserPreferencesUpdateRequest();
        request.setTravelPreferences("[\"美食\",\"购物\"]");
        request.setSpecialRequirements("[\"无障碍设施\",\"电梯\"]");
        request.setSpecialRequirementsDescription("需要无障碍设施和电梯");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(tagRepository.findAllByOrderById()).thenReturn(new ArrayList<>());

        // Act
        UserProfileResponse response = userService.updateUserPreferences(1L, request);

        // Assert
        assertNotNull(response);
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }

    @Test
    void testUpdateUserPreferences_ExistingPreferences() {
        // Arrange
        UserPreferencesUpdateRequest request = new UserPreferencesUpdateRequest();
        request.setTravelPreferences("[\"美食\",\"购物\"]");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(tagRepository.findAllByOrderById()).thenReturn(new ArrayList<>());

        // Act
        UserProfileResponse response = userService.updateUserPreferences(1L, request);

        // Assert
        assertNotNull(response);
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }

    @Test
    void testGetHistoryDestinations() {
        // Arrange
        UserDestination historyDest = new UserDestination();
        historyDest.setId(1L);
        historyDest.setDestination(testDestination);
        historyDest.setDestinationId(1L);
        historyDest.setStartDate(LocalDate.of(2023, 1, 1));
        historyDest.setEndDate(LocalDate.of(2023, 1, 3));
        historyDest.setDays(3);
        historyDest.setNotes("很好的旅行");
        historyDest.setItineraryId(1L);

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));

        // Act
        List<UserProfileResponse.HistoryDestinationDto> result = userService.getHistoryDestinations(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getDestinationId());
        assertEquals("2023-01", result.get(0).getVisitYearMonth());
        assertEquals(3, result.get(0).getDays());
        assertEquals("很好的旅行", result.get(0).getNotes());
        assertEquals(1L, result.get(0).getItineraryId());
        assertEquals("北京", result.get(0).getName());
    }

    @Test
    void testGetHistoryDestinations_StartDateNull() {
        // Arrange
        UserDestination historyDest = new UserDestination();
        historyDest.setId(1L);
        historyDest.setDestination(testDestination);
        historyDest.setStartDate(null);
        historyDest.setEndDate(LocalDate.of(2023, 1, 3));
        historyDest.setDays(3);
        historyDest.setNotes("很好的旅行");
        historyDest.setItineraryId(1L);

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));

        // Act
        List<UserProfileResponse.HistoryDestinationDto> result = userService.getHistoryDestinations(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("未知", result.get(0).getVisitYearMonth());
    }

    @Test
    void testGetWishlistDestinations() {
        // Arrange
        UserDestination wishlistDest = new UserDestination();
        wishlistDest.setId(2L);
        wishlistDest.setDestination(testDestination);
        wishlistDest.setDestinationId(1L);
        wishlistDest.setNotes("想去的地方");

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.期望目的地)))
                .thenReturn(Arrays.asList(wishlistDest));

        // Act
        List<UserProfileResponse.WishlistDestinationDto> result = userService.getWishlistDestinations(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getDestinationId());
        assertEquals("想去的地方", result.get(0).getNotes());
        assertEquals("北京", result.get(0).getName());
    }

    @Test
    void testAddHistoryDestination() {
        // Arrange
        AddHistoryDestinationRequest request = new AddHistoryDestinationRequest();
        request.setName("上海");
        request.setDescription("魔都");
        request.setStartDate(LocalDate.of(2023, 2, 1));
        request.setEndDate(LocalDate.of(2023, 2, 3));
        request.setDays(3);
        request.setNotes("上海之旅");

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(destinationRepository.findByName("上海"))
                .thenReturn(Optional.of(testDestination));
        when(userDestinationRepository.save(any(UserDestination.class))).thenReturn(new UserDestination());

        // Act
        userService.addHistoryDestination(1L, request);

        // Assert
        verify(userDestinationRepository).save(any(UserDestination.class));
    }

    @Test
    void testAddHistoryDestination_NewDestination() {
        // Arrange
        AddHistoryDestinationRequest request = new AddHistoryDestinationRequest();
        request.setName("新城市");
        request.setDescription("新城市描述");
        request.setStartDate(LocalDate.of(2023, 2, 1));
        request.setEndDate(LocalDate.of(2023, 2, 3));
        request.setDays(3);
        request.setNotes("新城市之旅");

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(destinationRepository.findByName("新城市"))
                .thenReturn(Optional.empty());
        when(destinationRepository.save(any(Destination.class))).thenReturn(testDestination);
        when(userDestinationRepository.save(any(UserDestination.class))).thenReturn(new UserDestination());

        // Act
        userService.addHistoryDestination(1L, request);

        // Assert
        verify(destinationRepository).save(any(Destination.class));
        verify(userDestinationRepository).save(any(UserDestination.class));
    }

    @Test
    void testAddWishlistDestination() {
        // Arrange
        AddWishlistDestinationRequest request = new AddWishlistDestinationRequest();
        request.setName("巴黎");
        request.setDescription("浪漫之都");
        request.setNotes("想去巴黎");

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(destinationRepository.findByName("巴黎"))
                .thenReturn(Optional.of(testDestination));
        when(userDestinationRepository.save(any(UserDestination.class))).thenReturn(new UserDestination());

        // Act
        userService.addWishlistDestination(1L, request);

        // Assert
        verify(userDestinationRepository).save(any(UserDestination.class));
    }

    @Test
    void testAddWishlistDestination_NewDestination() {
        // Arrange
        AddWishlistDestinationRequest request = new AddWishlistDestinationRequest();
        request.setName("新城市");
        request.setDescription("新城市描述");
        request.setNotes("想去新城市");

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(destinationRepository.findByName("新城市"))
                .thenReturn(Optional.empty());
        when(destinationRepository.save(any(Destination.class))).thenReturn(testDestination);
        when(userDestinationRepository.save(any(UserDestination.class))).thenReturn(new UserDestination());

        // Act
        userService.addWishlistDestination(1L, request);

        // Assert
        verify(destinationRepository).save(any(Destination.class));
        verify(userDestinationRepository).save(any(UserDestination.class));
    }

    @Test
    void testRemoveHistoryDestination() {
        // Arrange
        UserDestination historyDest = new UserDestination();
        historyDest.setId(1L);
        historyDest.setDestination(testDestination);
        historyDest.setDestinationId(1L);

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));

        // Act
        userService.removeHistoryDestination(1L, 1L);

        // Assert
        verify(userDestinationRepository).delete(historyDest);
    }

    @Test
    void testRemoveWishlistDestination() {
        // Arrange
        UserDestination wishlistDest = new UserDestination();
        wishlistDest.setId(2L);
        wishlistDest.setDestination(testDestination);
        wishlistDest.setDestinationId(1L);

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.期望目的地)))
                .thenReturn(Arrays.asList(wishlistDest));

        // Act
        userService.removeWishlistDestination(1L, 1L);

        // Assert
        verify(userDestinationRepository).delete(wishlistDest);
    }

    @Test
    void testAddHistoryDestinationsFromCompletedItineraries() {
        // Arrange
        Itinerary completedItinerary = new Itinerary();
        completedItinerary.setId(1L);
        completedItinerary.setTitle("北京之旅");
        completedItinerary.setTravelStatus(Itinerary.TravelStatus.已出行);
        completedItinerary.setStartDate(LocalDate.of(2023, 1, 1));
        completedItinerary.setEndDate(LocalDate.of(2023, 1, 3));
        completedItinerary.setItineraryDays(new ArrayList<>()); // 空列表，所以不会添加任何目的地

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(Arrays.asList(completedItinerary));

        // Act
        int result = userService.addHistoryDestinationsFromCompletedItineraries(1L);

        // Assert
        assertEquals(0, result); // 没有行程天数，所以返回0
        verify(userDestinationRepository, never()).save(any(UserDestination.class));
    }



    @Test
    void testRemoveAutoAddedHistoryDestinationsFromItinerary() {
        // Arrange
        UserDestination autoAddedDest = new UserDestination();
        autoAddedDest.setId(1L);
        autoAddedDest.setItineraryId(1L);
        autoAddedDest.setType(UserDestination.Type.历史目的地);

        Itinerary itinerary = new Itinerary();
        itinerary.setId(1L);
        itinerary.setUser(testUser);

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(userDestinationRepository.findByUserPreferencesIdAndItineraryId(any(), eq(1L)))
                .thenReturn(Arrays.asList(autoAddedDest));

        // Act
        int result = userService.removeAutoAddedHistoryDestinationsFromItinerary(1L, 1L);

        // Assert
        assertEquals(1, result);
        verify(userDestinationRepository).deleteAll(Arrays.asList(autoAddedDest));
    }

    @Test
    void testUploadAvatar() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "avatar.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        // Act
        String result = userService.uploadAvatar(1L, file);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("avatar"));
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void testUploadAvatar_EmptyFile() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "avatar",
                "avatar.jpg",
                "image/jpeg",
                new byte[0]
        );

        // Act & Assert - 当文件为空时应该抛出异常
        assertThrows(RuntimeException.class, () -> userService.uploadAvatar(1L, emptyFile));
    }

    @Test
    void testGetTravelStats() {
        // Arrange
        UserDestination historyDest = new UserDestination();
        historyDest.setDestination(testDestination);
        historyDest.setStartDate(LocalDate.of(2023, 1, 1));
        historyDest.setEndDate(LocalDate.of(2023, 1, 3));
        historyDest.setDays(3);

        Itinerary completedItinerary = new Itinerary();
        completedItinerary.setId(1L);
        completedItinerary.setTitle("北京之旅");
        completedItinerary.setTravelStatus(Itinerary.TravelStatus.已出行);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(Arrays.asList(completedItinerary));

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(1, result.getTotalDestinations());
        assertEquals(3, result.getTotalDays());
        assertEquals(1, result.getTotalItineraries());
        assertNotNull(result.getTimeline());
        assertNotNull(result.getGeography());
    }

    @Test
    void testGetTravelStats_EmptyData() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(new ArrayList<>());
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(new ArrayList<>());

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(0, result.getTotalDestinations());
        assertEquals(0, result.getTotalDays());
        assertEquals(0, result.getTotalItineraries());
        assertNotNull(result.getTimeline());
        assertTrue(result.getTimeline().isEmpty());
        assertNotNull(result.getGeography());
    }

    @Test
    void testGetUserHomepage() {
        // Arrange
        Itinerary publicItinerary = new Itinerary();
        publicItinerary.setId(1L);
        publicItinerary.setTitle("公开行程");
        publicItinerary.setPermissionStatus(Itinerary.PermissionStatus.所有人可见);
        publicItinerary.setStartDate(LocalDate.of(2023, 1, 1));
        publicItinerary.setEndDate(LocalDate.of(2023, 1, 3));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(itineraryRepository.findByUserIdAndPermissionStatus(1L, Itinerary.PermissionStatus.所有人可见))
                .thenReturn(Arrays.asList(publicItinerary));

        // Act
        UserHomepageResponse result = userService.getUserHomepage(1L, "127.0.0.1");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("avatar.jpg", result.getAvatarUrl());
        assertNotNull(result.getPublicItineraries());
        assertEquals(1, result.getPublicItineraries().size());
    }

    @Test
    void testGetUserHomepage_NoPublicItineraries() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(itineraryRepository.findByUserIdAndPermissionStatus(1L, Itinerary.PermissionStatus.所有人可见))
                .thenReturn(new ArrayList<>());

        // Act
        UserHomepageResponse result = userService.getUserHomepage(1L, "127.0.0.1");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertNotNull(result.getPublicItineraries());
        assertTrue(result.getPublicItineraries().isEmpty());
    }

    @Test
    void testSearchUsers() {
        // Arrange
        User user1 = new User();
        user1.setId(2L);
        user1.setUsername("user1");

        when(userRepository.findByUsernameContaining("user"))
                .thenReturn(Arrays.asList(user1));

        // Act
        List<Map<String, Object>> result = userService.searchUsers("user", 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).get("id"));
        assertEquals("user1", result.get(0).get("username"));
    }

    @Test
    void testSearchUsers_ExcludeCurrentUser() {
        // Arrange
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("currentuser");

        when(userRepository.findByUsernameContaining("user"))
                .thenReturn(Arrays.asList(currentUser));

        // Act
        List<Map<String, Object>> result = userService.searchUsers("user", 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchUsers_EmptyResult() {
        // Arrange
        when(userRepository.findByUsernameContaining("nonexistent"))
                .thenReturn(new ArrayList<>());

        // Act
        List<Map<String, Object>> result = userService.searchUsers("nonexistent", 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testExceptionHandling_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.getUserProfile(999L));
    }

    @Test
    void testEdgeCases_EmptyLists() {
        // Arrange
        UserPreferences emptyPreferences = new UserPreferences();
        emptyPreferences.setId(1L);
        emptyPreferences.setUser(testUser);
        emptyPreferences.setTravelPreferences(null);
        emptyPreferences.setSpecialRequirements(null);
        emptyPreferences.setSpecialRequirementsDescription(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(emptyPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), any()))
                .thenReturn(new ArrayList<>());
        when(tagRepository.findAllByOrderById()).thenReturn(new ArrayList<>());

        // Act
        UserProfileResponse response = userService.getUserProfile(1L);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getTravelPreferences());
        assertTrue(response.getTravelPreferences().isEmpty());
        assertNotNull(response.getSpecialRequirements());
        assertTrue(response.getSpecialRequirements().isEmpty());
    }

    @Test
    void testGetTravelStats_WithGeographyData() {
        // Arrange - 创建包含地理位置信息的目的地
        Destination easternDest = new Destination();
        easternDest.setId(2L);
        easternDest.setName("上海");
        easternDest.setDescription("华东地区重要城市");

        Destination southernDest = new Destination();
        southernDest.setId(3L);
        southernDest.setName("三亚");
        southernDest.setDescription("华南地区海滨城市");

        Destination westernDest = new Destination();
        westernDest.setId(4L);
        westernDest.setName("成都");
        westernDest.setDescription("西南地区重要城市");

        Destination northernDest = new Destination();
        northernDest.setId(5L);
        northernDest.setName("哈尔滨");
        northernDest.setDescription("东北地区重要城市");

        // 创建历史目的地记录
        UserDestination historyDest1 = new UserDestination();
        historyDest1.setDestination(easternDest);
        historyDest1.setStartDate(LocalDate.of(2023, 3, 15)); // 3月
        historyDest1.setEndDate(LocalDate.of(2023, 3, 17));
        historyDest1.setDays(3);

        UserDestination historyDest2 = new UserDestination();
        historyDest2.setDestination(southernDest);
        historyDest2.setStartDate(LocalDate.of(2023, 7, 10)); // 7月
        historyDest2.setEndDate(LocalDate.of(2023, 7, 12));
        historyDest2.setDays(3);

        UserDestination historyDest3 = new UserDestination();
        historyDest3.setDestination(westernDest);
        historyDest3.setStartDate(LocalDate.of(2023, 10, 5)); // 10月
        historyDest3.setEndDate(LocalDate.of(2023, 10, 7));
        historyDest3.setDays(3);

        UserDestination historyDest4 = new UserDestination();
        historyDest4.setDestination(northernDest);
        historyDest4.setStartDate(LocalDate.of(2023, 12, 20)); // 12月
        historyDest4.setEndDate(LocalDate.of(2023, 12, 22));
        historyDest4.setDays(3);

        List<UserDestination> historyDests = Arrays.asList(historyDest1, historyDest2, historyDest3, historyDest4);
        List<Destination> destinations = Arrays.asList(easternDest, southernDest, westernDest, northernDest);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(historyDests);
        when(destinationRepository.findByNameIn(any())).thenReturn(destinations);
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(new ArrayList<>());

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getGeography());
        assertEquals("上海", result.getGeography().getEasternmost());
        assertEquals("三亚", result.getGeography().getSouthernmost());
        assertEquals("成都", result.getGeography().getWesternmost());
        assertEquals("哈尔滨", result.getGeography().getNorthernmost());
        // 由于有多个月份，应该返回访问次数最多的月份
        assertNotNull(result.getGeography().getFavoriteMonth());
        assertEquals("2023", result.getGeography().getMostTravelYear());
    }

    @Test
    void testGetTravelStats_WithVisitYearMonth() {
        // Arrange - 测试使用startDate的情况
        UserDestination historyDest = new UserDestination();
        historyDest.setDestination(testDestination);
        historyDest.setStartDate(LocalDate.of(2023, 5, 15)); // 5月
        historyDest.setEndDate(LocalDate.of(2023, 5, 17));
        historyDest.setDays(3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));
        when(destinationRepository.findByNameIn(any())).thenReturn(Arrays.asList(testDestination));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(new ArrayList<>());

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getGeography());
        assertEquals("5月", result.getGeography().getFavoriteMonth());
        assertEquals("2023", result.getGeography().getMostTravelYear());
    }

    @Test
    void testGetTravelStats_WithNullStartDate() {
        // Arrange - 测试startDate为null的情况
        UserDestination historyDest = new UserDestination();
        historyDest.setDestination(testDestination);
        historyDest.setStartDate(null);
        historyDest.setEndDate(null);
        historyDest.setDays(3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));
        when(destinationRepository.findByNameIn(any())).thenReturn(Arrays.asList(testDestination));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(new ArrayList<>());

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getGeography());
        assertNull(result.getGeography().getFavoriteMonth());
        assertNull(result.getGeography().getMostTravelYear());
    }

    @Test
    void testGetTravelStats_WithMultipleCities() {
        // Arrange - 测试多个城市的情况，验证城市统计
        Destination city1 = new Destination();
        city1.setId(2L);
        city1.setName("北京");

        Destination city2 = new Destination();
        city2.setId(3L);
        city2.setName("上海");

        UserDestination historyDest1 = new UserDestination();
        historyDest1.setDestination(city1);
        historyDest1.setStartDate(LocalDate.of(2023, 1, 1));
        historyDest1.setEndDate(LocalDate.of(2023, 1, 3));
        historyDest1.setDays(3);

        UserDestination historyDest2 = new UserDestination();
        historyDest2.setDestination(city1); // 再次访问北京
        historyDest2.setStartDate(LocalDate.of(2023, 6, 1));
        historyDest2.setEndDate(LocalDate.of(2023, 6, 5));
        historyDest2.setDays(5);

        UserDestination historyDest3 = new UserDestination();
        historyDest3.setDestination(city2);
        historyDest3.setStartDate(LocalDate.of(2023, 9, 1));
        historyDest3.setEndDate(LocalDate.of(2023, 9, 2));
        historyDest3.setDays(2);

        List<UserDestination> historyDests = Arrays.asList(historyDest1, historyDest2, historyDest3);
        List<Destination> destinations = Arrays.asList(city1, city2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(historyDests);
        when(destinationRepository.findByNameIn(any())).thenReturn(destinations);
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(new ArrayList<>());

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalDestinations());
        assertEquals(10, result.getTotalDays()); // 3 + 5 + 2
        assertNotNull(result.getTopCities());
        assertEquals(2, result.getTopCities().size());
        // 北京应该排在第一位（访问2次，共8天）
        assertEquals("北京", result.getTopCities().get(0).getName());
        assertEquals(2, result.getTopCities().get(0).getVisitCount());
        assertEquals(8, result.getTopCities().get(0).getTotalDays());
    }

    @Test
    void testGetTravelStats_WithCompletedItineraries() {
        // Arrange - 测试包含已完成行程的情况
        UserDestination historyDest = new UserDestination();
        historyDest.setDestination(testDestination);
        historyDest.setStartDate(LocalDate.of(2023, 1, 1));
        historyDest.setEndDate(LocalDate.of(2023, 1, 3));
        historyDest.setDays(3);

        Itinerary completedItinerary = new Itinerary();
        completedItinerary.setId(1L);
        completedItinerary.setTitle("北京之旅");
        completedItinerary.setTravelStatus(Itinerary.TravelStatus.已出行);
        completedItinerary.setStartDate(LocalDate.of(2023, 1, 1));
        completedItinerary.setEndDate(LocalDate.of(2023, 1, 3));

        // 创建行程天数和活动
        ItineraryDay day1 = new ItineraryDay();
        day1.setId(1L);
        day1.setItinerary(completedItinerary);
        day1.setDayNumber(1);

        ItineraryActivity activity1 = new ItineraryActivity();
        activity1.setId(1L);
        activity1.setItineraryDay(day1);

        Attraction attraction1 = new Attraction();
        attraction1.setId(1L);
        attraction1.setDestination(testDestination);
        activity1.setAttraction(attraction1);

        day1.setActivities(Arrays.asList(activity1));
        completedItinerary.setItineraryDays(Arrays.asList(day1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));
        when(destinationRepository.findByNameIn(any())).thenReturn(Arrays.asList(testDestination));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(Arrays.asList(completedItinerary));

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalItineraries());
        assertNotNull(result.getTimeline());
        assertFalse(result.getTimeline().isEmpty());
    }

    @Test
    void testGetTravelStats_WithNullDays() {
        // Arrange - 测试days为null的情况
        UserDestination historyDest = new UserDestination();
        historyDest.setDestination(testDestination);
        historyDest.setStartDate(LocalDate.of(2023, 1, 1));
        historyDest.setEndDate(LocalDate.of(2023, 1, 3));
        historyDest.setDays(null); // days为null

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));
        when(destinationRepository.findByNameIn(any())).thenReturn(Arrays.asList(testDestination));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(new ArrayList<>());

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalDestinations());
        assertEquals(0, result.getTotalDays()); // days为null时应该计算为0
    }

    @Test
    void testGetTravelStats_WithShortStartDate() {
        // Arrange - 测试startDate为null的情况
        UserDestination historyDest = new UserDestination();
        historyDest.setDestination(testDestination);
        historyDest.setStartDate(null);
        historyDest.setEndDate(null);
        historyDest.setDays(3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));
        when(destinationRepository.findByNameIn(any())).thenReturn(Arrays.asList(testDestination));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(new ArrayList<>());

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getGeography());
        assertNull(result.getGeography().getFavoriteMonth());
        assertNull(result.getGeography().getMostTravelYear());
    }

    @Test
    void testGetTravelStats_WithDescriptionBasedGeography() {
        // Arrange - 测试基于描述的地理位置判断
        Destination easternDest = new Destination();
        easternDest.setId(2L);
        easternDest.setName("某个城市");
        easternDest.setDescription("华东地区重要城市"); // 通过描述判断

        Destination southernDest = new Destination();
        southernDest.setId(3L);
        southernDest.setName("另一个城市");
        southernDest.setDescription("华南地区海滨城市"); // 通过描述判断

        UserDestination historyDest1 = new UserDestination();
        historyDest1.setDestination(easternDest);
        historyDest1.setStartDate(LocalDate.of(2023, 1, 1));
        historyDest1.setEndDate(LocalDate.of(2023, 1, 3));
        historyDest1.setDays(3);

        UserDestination historyDest2 = new UserDestination();
        historyDest2.setDestination(southernDest);
        historyDest2.setStartDate(LocalDate.of(2023, 6, 1));
        historyDest2.setEndDate(LocalDate.of(2023, 6, 3));
        historyDest2.setDays(3);

        List<UserDestination> historyDests = Arrays.asList(historyDest1, historyDest2);
        List<Destination> destinations = Arrays.asList(easternDest, southernDest);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(historyDests);
        when(destinationRepository.findByNameIn(any())).thenReturn(destinations);
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(new ArrayList<>());

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getGeography());
        assertEquals("某个城市", result.getGeography().getEasternmost());
        assertEquals("另一个城市", result.getGeography().getSouthernmost());
        assertNull(result.getGeography().getWesternmost());
        assertNull(result.getGeography().getNorthernmost());
    }

    @Test
    void testGetTravelStats_WithNullDestinationDescription() {
        // Arrange - 测试目的地描述为null的情况
        Destination dest = new Destination();
        dest.setId(2L);
        dest.setName("上海");
        dest.setDescription(null); // 描述为null

        UserDestination historyDest = new UserDestination();
        historyDest.setDestination(dest);
        historyDest.setStartDate(LocalDate.of(2023, 1, 1));
        historyDest.setEndDate(LocalDate.of(2023, 1, 3));
        historyDest.setDays(3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));
        when(destinationRepository.findByNameIn(any())).thenReturn(Arrays.asList(dest));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(new ArrayList<>());

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getGeography());
        assertEquals("上海", result.getGeography().getEasternmost()); // 通过名称判断
    }

    @Test
    void testUpdateUserProfile_WithNullUser() {
        // Arrange - 测试profile.getUser()为null的情况
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setUser(null); // user为null

        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setUsername("newusername");

        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);

        // Act
        UserProfileResponse result = userService.updateUserProfile(1L, request);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(2)).findById(1L); // 调用两次：一次在updateUserProfile，一次在getUserProfile
    }

    @Test
    void testUpdateUserProfile_WithAvatarUrl() {
        // Arrange - 测试更新头像URL
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setAvatarUrl("new-avatar.jpg");

        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        UserProfileResponse result = userService.updateUserProfile(1L, request);

        // Assert
        assertNotNull(result);
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void testUpdateUserPreferences_WithTravelPreferences() {
        // Arrange - 测试更新旅行偏好
        UserPreferencesUpdateRequest request = new UserPreferencesUpdateRequest();
        request.setTravelPreferences("{\"1\": 1, \"2\": 0}");

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        UserProfileResponse result = userService.updateUserPreferences(1L, request);

        // Assert
        assertNotNull(result);
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }

    @Test
    void testGetHistoryDestinations_WithNullDestination() {
        // Arrange - 测试目的地为null的情况
        UserDestination historyDest = new UserDestination();
        historyDest.setDestination(null); // destination为null
        historyDest.setStartDate(LocalDate.of(2023, 1, 1));
        historyDest.setEndDate(LocalDate.of(2023, 1, 3));
        historyDest.setDays(3);

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));

        // Act
        List<UserProfileResponse.HistoryDestinationDto> result = userService.getHistoryDestinations(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getName()); // 目的地为null时，name应该为null
    }

    @Test
    void testGetWishlistDestinations_WithNullDestination() {
        // Arrange - 测试目的地为null的情况
        UserDestination wishlistDest = new UserDestination();
        wishlistDest.setDestination(null); // destination为null

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.期望目的地)))
                .thenReturn(Arrays.asList(wishlistDest));

        // Act
        List<UserProfileResponse.WishlistDestinationDto> result = userService.getWishlistDestinations(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getName()); // 目的地为null时，name应该为null
    }

    @Test
    void testAddWishlistDestination_AlreadyExists() {
        // Arrange - 测试添加已存在的期望目的地
        AddWishlistDestinationRequest request = new AddWishlistDestinationRequest();
        request.setName("北京");
        request.setDescription("首都");
        request.setNotes("想去看看");

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(destinationRepository.findByName("北京")).thenReturn(Optional.of(testDestination));
        when(userDestinationRepository.countByUserPreferencesIdAndDestinationIdAndType(any(), eq(1L), eq(UserDestination.Type.期望目的地)))
                .thenReturn(1L); // 已存在

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.addWishlistDestination(1L, request));
    }

    @Test
    void testParseJsonToStringList_WithInvalidJson() {
        // Arrange - 测试解析无效JSON的情况
        String invalidJson = "invalid json";

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), any()))
                .thenReturn(new ArrayList<>());
        when(tagRepository.findAllByOrderById()).thenReturn(new ArrayList<>());

        // 修改testPreferences的specialRequirements为无效JSON
        testPreferences.setSpecialRequirements(invalidJson);

        // Act
        UserProfileResponse result = userService.getUserProfile(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getSpecialRequirements());
        assertTrue(result.getSpecialRequirements().isEmpty()); // 解析失败时应该返回空列表
    }

    @Test
    void testParseTravelPreferences_WithInvalidJson() {
        // Arrange - 测试解析无效旅行偏好JSON的情况
        String invalidJson = "invalid json";

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), any()))
                .thenReturn(new ArrayList<>());
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setTag("测试标签");
        when(tagRepository.findAllByOrderById()).thenReturn(Arrays.asList(tag));

        // 修改testPreferences的travelPreferences为无效JSON
        testPreferences.setTravelPreferences(invalidJson);

        // Act
        UserProfileResponse result = userService.getUserProfile(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTravelPreferences());
        // 解析失败时应该返回所有标签，但selected为false
    }

    @Test
    void testParseTravelPreferences_WithValidJson() {
        // Arrange - 测试解析有效旅行偏好JSON的情况
        String validJson = "{\"1\": 1, \"2\": 0}";
        Tag tag1 = new Tag();
        tag1.setId(1L);
        tag1.setTag("美食");
        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setTag("文化");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), any()))
                .thenReturn(new ArrayList<>());
        when(tagRepository.findAllByOrderById()).thenReturn(Arrays.asList(tag1, tag2));

        // 修改testPreferences的travelPreferences为有效JSON
        testPreferences.setTravelPreferences(validJson);

        // Act
        UserProfileResponse result = userService.getUserProfile(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTravelPreferences());
        assertEquals(2, result.getTravelPreferences().size());
        // 第一个标签应该被选中，第二个不应该
        assertTrue(result.getTravelPreferences().get(0).getSelected());
        assertFalse(result.getTravelPreferences().get(1).getSelected());
    }

    @Test
    void testAddHistoryDestinationsFromCompletedItineraries_EmptyItineraries() {
        // Arrange - 测试没有已完成行程的情况
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(new ArrayList<>());

        // Act
        int result = userService.addHistoryDestinationsFromCompletedItineraries(1L);

        // Assert
        assertEquals(0, result);
    }

    @Test
    void testAddHistoryDestinationsFromCompletedItineraries_WithItineraryDays() {
        // Arrange - 测试有行程天数的情况
        Itinerary completedItinerary = new Itinerary();
        completedItinerary.setId(1L);
        completedItinerary.setTitle("北京之旅");
        completedItinerary.setTravelStatus(Itinerary.TravelStatus.已出行);
        completedItinerary.setStartDate(LocalDate.of(2023, 1, 1));
        completedItinerary.setEndDate(LocalDate.of(2023, 1, 3));

        ItineraryDay day1 = new ItineraryDay();
        day1.setId(1L);
        day1.setItinerary(completedItinerary);
        day1.setDayNumber(1);
        day1.setDate(LocalDate.of(2023, 1, 1));

        ItineraryActivity activity1 = new ItineraryActivity();
        activity1.setId(1L);
        activity1.setItineraryDay(day1);

        Attraction attraction1 = new Attraction();
        attraction1.setId(1L);
        attraction1.setDestination(testDestination);
        activity1.setAttraction(attraction1);

        day1.setActivities(Arrays.asList(activity1));
        completedItinerary.setItineraryDays(Arrays.asList(day1));

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(Arrays.asList(completedItinerary));
        when(userDestinationRepository.findByUserPreferencesIdAndItineraryId(any(), eq(1L)))
                .thenReturn(new ArrayList<>());
        when(userDestinationRepository.save(any(UserDestination.class))).thenReturn(new UserDestination());

        // Act
        int result = userService.addHistoryDestinationsFromCompletedItineraries(1L);

        // Assert
        assertEquals(1, result);
        verify(userDestinationRepository).save(any(UserDestination.class));
    }

    @Test
    void testRemoveAutoAddedHistoryDestinationsFromItinerary_WithHistoryDestinations() {
        // Arrange - 测试删除历史目的地的情况
        UserDestination historyDest = new UserDestination();
        historyDest.setId(1L);
        historyDest.setType(UserDestination.Type.历史目的地);

        Itinerary itinerary = new Itinerary();
        itinerary.setId(1L);
        itinerary.setUser(testUser);

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(userDestinationRepository.findByUserPreferencesIdAndItineraryId(any(), eq(1L)))
                .thenReturn(Arrays.asList(historyDest));

        // Act
        int result = userService.removeAutoAddedHistoryDestinationsFromItinerary(1L, 1L);

        // Assert
        assertEquals(1, result);
        verify(userDestinationRepository).deleteAll(Arrays.asList(historyDest));
    }

    @Test
    void testRemoveAutoAddedHistoryDestinationsFromItinerary_ItineraryNotFound() {
        // Arrange - 测试行程不存在的情况
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(itineraryRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        int result = userService.removeAutoAddedHistoryDestinationsFromItinerary(1L, 1L);

        // Assert
        assertEquals(0, result);
    }

    @Test
    void testRemoveAutoAddedHistoryDestinationsFromItinerary_WrongUser() {
        // Arrange - 测试行程不属于该用户的情况
        User otherUser = new User();
        otherUser.setId(2L);

        Itinerary itinerary = new Itinerary();
        itinerary.setId(1L);
        itinerary.setUser(otherUser);

        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));

        // Act
        int result = userService.removeAutoAddedHistoryDestinationsFromItinerary(1L, 1L);

        // Assert
        assertEquals(0, result);
    }

    @Test
    void testUploadAvatar_WithDirectoryCreation() throws Exception {
        // Arrange - 测试需要创建目录的情况
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "avatar.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);

        // 模拟目录不存在的情况
        Path uploadPath = Paths.get("uploads/avatars/");
        if (Files.exists(uploadPath)) {
            // 如果目录存在，先删除
            Files.walk(uploadPath)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
        }

        // Act
        String result = userService.uploadAvatar(1L, file);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("avatar"));
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void testUploadAvatar_WithNullOriginalFilename() throws Exception {
        // Arrange - 测试原始文件名为null的情况
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                null, // 原始文件名为null
                "image/jpeg",
                "test image content".getBytes()
        );

        // Act & Assert - 当文件名为null时应该抛出异常
        assertThrows(RuntimeException.class, () -> userService.uploadAvatar(1L, file));
    }

    @Test
    void testUploadAvatar_WithIOException() throws Exception {
        // Arrange - 测试IO异常的情况
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("avatar.jpg");
        doThrow(new IOException("模拟IO异常")).when(mockFile).getInputStream();

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.uploadAvatar(1L, mockFile));
    }

    @Test
    void testGetTravelStats_WithNullCreatedAt() {
        // Arrange - 测试用户创建时间为null的情况
        testUser.setCreatedAt(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(new ArrayList<>());
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(new ArrayList<>());

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertNotNull(result.getCompanionDays());
        assertTrue(result.getCompanionDays() >= 1);
    }

    @Test
    void testBuildTimeline_WithNullActivities() {
        // Arrange - 测试活动为null的情况
        Itinerary completedItinerary = new Itinerary();
        completedItinerary.setId(1L);
        completedItinerary.setTitle("北京之旅");
        completedItinerary.setTravelStatus(Itinerary.TravelStatus.已出行);
        completedItinerary.setStartDate(LocalDate.of(2023, 1, 1));
        completedItinerary.setEndDate(LocalDate.of(2023, 1, 3));

        ItineraryDay day1 = new ItineraryDay();
        day1.setId(1L);
        day1.setItinerary(completedItinerary);
        day1.setDayNumber(1);
        day1.setDate(LocalDate.of(2023, 1, 1));
        day1.setActivities(null); // 活动为null

        completedItinerary.setItineraryDays(Arrays.asList(day1));

        UserDestination historyDest = new UserDestination();
        historyDest.setDestination(testDestination);
        historyDest.setStartDate(LocalDate.of(2023, 1, 1));
        historyDest.setEndDate(LocalDate.of(2023, 1, 3));
        historyDest.setDays(3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));
        when(destinationRepository.findByNameIn(any())).thenReturn(Arrays.asList(testDestination));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(Arrays.asList(completedItinerary));

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTimeline());
    }

    @Test
    void testBuildTimeline_WithNullAttraction() {
        // Arrange - 测试景点为null的情况
        Itinerary completedItinerary = new Itinerary();
        completedItinerary.setId(1L);
        completedItinerary.setTitle("北京之旅");
        completedItinerary.setTravelStatus(Itinerary.TravelStatus.已出行);
        completedItinerary.setStartDate(LocalDate.of(2023, 1, 1));
        completedItinerary.setEndDate(LocalDate.of(2023, 1, 3));

        ItineraryDay day1 = new ItineraryDay();
        day1.setId(1L);
        day1.setItinerary(completedItinerary);
        day1.setDayNumber(1);
        day1.setDate(LocalDate.of(2023, 1, 1));

        ItineraryActivity activity1 = new ItineraryActivity();
        activity1.setId(1L);
        activity1.setItineraryDay(day1);
        activity1.setAttraction(null); // 景点为null

        day1.setActivities(Arrays.asList(activity1));
        completedItinerary.setItineraryDays(Arrays.asList(day1));

        UserDestination historyDest = new UserDestination();
        historyDest.setDestination(testDestination);
        historyDest.setStartDate(LocalDate.of(2023, 1, 1));
        historyDest.setEndDate(LocalDate.of(2023, 1, 3));
        historyDest.setDays(3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest));
        when(destinationRepository.findByNameIn(any())).thenReturn(Arrays.asList(testDestination));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(Arrays.asList(completedItinerary));

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTimeline());
    }

    @Test
    void testGetMonthName_WithAllMonths() {
        // Arrange - 测试所有月份的情况
        UserDestination historyDest1 = new UserDestination();
        historyDest1.setDestination(testDestination);
        historyDest1.setStartDate(LocalDate.of(2023, 1, 1)); // 1月
        historyDest1.setEndDate(LocalDate.of(2023, 1, 3));
        historyDest1.setDays(3);

        UserDestination historyDest2 = new UserDestination();
        historyDest2.setDestination(testDestination);
        historyDest2.setStartDate(LocalDate.of(2023, 12, 1)); // 12月
        historyDest2.setEndDate(LocalDate.of(2023, 12, 3));
        historyDest2.setDays(3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), eq(UserDestination.Type.历史目的地)))
                .thenReturn(Arrays.asList(historyDest1, historyDest2));
        when(destinationRepository.findByNameIn(any())).thenReturn(Arrays.asList(testDestination));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行))
                .thenReturn(new ArrayList<>());

        // Act
        TravelStatsResponse result = userService.getTravelStats(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getGeography());
        assertNotNull(result.getGeography().getFavoriteMonth());
    }

    @Test
    void testUpdateUserPreferences_TravelPreferencesNull() {
        // Arrange
        UserPreferencesUpdateRequest request = new UserPreferencesUpdateRequest();
        request.setTravelPreferences(null); // 传null
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(testPreferences);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        // Act
        UserProfileResponse result = userService.updateUserPreferences(1L, request);
        // Assert
        assertNotNull(result);
    }

    @Test
    void testParseJsonToStringList_EmptyString() {
        // Arrange
        String emptyJson = "   ";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), any())).thenReturn(new ArrayList<>());
        when(tagRepository.findAllByOrderById()).thenReturn(new ArrayList<>());
        testPreferences.setSpecialRequirements(emptyJson);
        // Act
        UserProfileResponse result = userService.getUserProfile(1L);
        // Assert
        assertNotNull(result);
        assertTrue(result.getSpecialRequirements().isEmpty());
    }

    @Test
    void testParseTravelPreferences_EmptyString() {
        // Arrange
        String emptyJson = "   ";
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setTag("标签");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), any())).thenReturn(new ArrayList<>());
        when(tagRepository.findAllByOrderById()).thenReturn(Arrays.asList(tag));
        testPreferences.setTravelPreferences(emptyJson);
        // Act
        UserProfileResponse result = userService.getUserProfile(1L);
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTravelPreferences().size());
        assertFalse(result.getTravelPreferences().get(0).getSelected());
    }

    @Test
    void testParseTravelPreferences_PreferenceValueNull() {
        // Arrange
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setTag("标签");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(userDestinationRepository.findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(any(), any())).thenReturn(new ArrayList<>());
        when(tagRepository.findAllByOrderById()).thenReturn(Arrays.asList(tag));
        testPreferences.setTravelPreferences("{}" ); // preferenceValue为null
        // Act
        UserProfileResponse result = userService.getUserProfile(1L);
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTravelPreferences().size());
        assertFalse(result.getTravelPreferences().get(0).getSelected());
    }



    @Test
    void testRemoveAutoAddedHistoryDestinationsFromItinerary_NoHistoryType() {
        // Arrange
        UserDestination notHistoryDest = new UserDestination();
        notHistoryDest.setId(1L);
        notHistoryDest.setType(UserDestination.Type.期望目的地);
        Itinerary itinerary = new Itinerary();
        itinerary.setId(1L);
        itinerary.setUser(testUser);
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(userDestinationRepository.findByUserPreferencesIdAndItineraryId(any(), eq(1L))).thenReturn(Arrays.asList(notHistoryDest));
        // Act
        int result = userService.removeAutoAddedHistoryDestinationsFromItinerary(1L, 1L);
        // Assert
        assertEquals(0, result);
    }

    @Test
    void testRemoveAutoAddedHistoryDestinationsFromItinerary_EmptyList() {
        // Arrange
        Itinerary itinerary = new Itinerary();
        itinerary.setId(1L);
        itinerary.setUser(testUser);
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(userDestinationRepository.findByUserPreferencesIdAndItineraryId(any(), eq(1L))).thenReturn(new ArrayList<>());
        // Act
        int result = userService.removeAutoAddedHistoryDestinationsFromItinerary(1L, 1L);
        // Assert
        assertEquals(0, result);
    }

    @Test
    void testUploadAvatar_NoExtension() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "avatar", // 没有扩展名
                "image/jpeg",
                "test image content".getBytes()
        );
        // Act & Assert - 当文件名没有扩展名时应该抛出异常
        assertThrows(RuntimeException.class, () -> userService.uploadAvatar(1L, file));
    }

    @Test
    void testAddHistoryDestinationsFromCompletedItineraries_AlreadyExists_Branch() {
        // Arrange
        Itinerary completedItinerary = new Itinerary();
        completedItinerary.setId(1L);
        completedItinerary.setTitle("北京之旅");
        completedItinerary.setTravelStatus(Itinerary.TravelStatus.已出行);
        completedItinerary.setStartDate(LocalDate.of(2023, 1, 1));
        completedItinerary.setEndDate(LocalDate.of(2023, 1, 3));
        ItineraryDay day1 = new ItineraryDay();
        day1.setId(1L);
        day1.setItinerary(completedItinerary);
        day1.setDayNumber(1);
        day1.setDate(LocalDate.of(2023, 1, 1));
        ItineraryActivity activity1 = new ItineraryActivity();
        activity1.setId(1L);
        activity1.setItineraryDay(day1);
        Attraction attraction1 = new Attraction();
        attraction1.setId(1L);
        attraction1.setDestination(testDestination);
        activity1.setAttraction(attraction1);
        day1.setActivities(Arrays.asList(activity1));
        completedItinerary.setItineraryDays(Arrays.asList(day1));
        UserDestination existingDest = new UserDestination();
        existingDest.setDestinationId(1L);
        existingDest.setType(UserDestination.Type.历史目的地);
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPreferences));
        when(itineraryRepository.findByUserIdAndTravelStatus(1L, Itinerary.TravelStatus.已出行)).thenReturn(Arrays.asList(completedItinerary));
        when(userDestinationRepository.findByUserPreferencesIdAndItineraryId(any(), eq(1L))).thenReturn(Arrays.asList(existingDest));
        // Act
        int result = userService.addHistoryDestinationsFromCompletedItineraries(1L);
        // Assert
        assertEquals(0, result);
    }

} 
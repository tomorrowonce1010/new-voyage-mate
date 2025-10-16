package com.se_07.backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se_07.backend.dto.*;
import com.se_07.backend.entity.*;
import com.se_07.backend.repository.*;
import com.se_07.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private UserDestinationRepository userDestinationRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ItineraryRepository itineraryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public UserProfileResponse getUserProfile(Long userId) {
        // 获取用户基础信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 获取用户档案
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        // 获取用户偏好
        UserPreferences preferences = userPreferencesRepository.findByUserId(userId).orElse(null);

        // 构建响应对象
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());

        if (profile != null) {
            response.setAvatarUrl(profile.getAvatarUrl());
            response.setBirthday(profile.getBirthday());
            response.setSignature(profile.getSignature());
            response.setBio(profile.getBio());
        }

        if (preferences != null) {
            response.setTravelPreferences(parseTravelPreferences(preferences.getTravelPreferences()));
            response.setSpecialRequirements(parseJsonToStringList(preferences.getSpecialRequirements()));
            response.setSpecialRequirementsDescription(preferences.getSpecialRequirementsDescription());

            // 获取历史目的地和期望目的地
            response.setHistoryDestinations(getHistoryDestinations(userId));
            response.setWishlistDestinations(getWishlistDestinations(userId));
        } else {
            response.setTravelPreferences(parseTravelPreferences(null));
            response.setSpecialRequirements(new ArrayList<>());
            response.setSpecialRequirementsDescription(null);
            response.setHistoryDestinations(new ArrayList<>());
            response.setWishlistDestinations(new ArrayList<>());
        }

        return response;
    }

    @Override
    public UserProfileResponse updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        // 获取或创建用户档案
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(new UserProfile());

        if (profile.getId() == null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            profile.setUser(user);
        }

        // 更新用户名（如果提供）
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            User user = profile.getUser();
            if (user == null) {
                user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("用户不存在"));
            }
            user.setUsername(request.getUsername().trim());
            userRepository.save(user);
        }

        // 更新档案信息
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBirthday() != null) {
            profile.setBirthday(request.getBirthday());
        }
        if (request.getSignature() != null) {
            profile.setSignature(request.getSignature());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }

        userProfileRepository.save(profile);

        return getUserProfile(userId);
    }

    @Override
    public UserProfileResponse updateUserPreferences(Long userId, UserPreferencesUpdateRequest request) {
        // 获取或创建用户偏好
        UserPreferences preferences = userPreferencesRepository.findByUserId(userId).orElse(new UserPreferences());

        if (preferences.getId() == null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            preferences.setUser(user);
        }

        // 更新偏好信息
        if (request.getTravelPreferences() != null) {
            preferences.setTravelPreferences(request.getTravelPreferences());
        }
        if (request.getSpecialRequirements() != null) {
            preferences.setSpecialRequirements(request.getSpecialRequirements());
        }
        if (request.getSpecialRequirementsDescription() != null) {
            preferences.setSpecialRequirementsDescription(request.getSpecialRequirementsDescription());
        }

        userPreferencesRepository.save(preferences);

        return getUserProfile(userId);
    }

    @Override
    public List<UserProfileResponse.HistoryDestinationDto> getHistoryDestinations(Long userId) {
        Long preferencesId = getOrCreateUserPreferencesId(userId);

        List<UserDestination> destinations = userDestinationRepository
                .findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(preferencesId, UserDestination.Type.历史目的地);

        return destinations.stream().map(dest -> {
            UserProfileResponse.HistoryDestinationDto dto = new UserProfileResponse.HistoryDestinationDto();
            dto.setDestinationId(dest.getDestinationId());
            // 从startDate生成visitYearMonth用于兼容性
            if (dest.getStartDate() != null) {
                String visitYearMonth = dest.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                dto.setVisitYearMonth(visitYearMonth);
            } else {
                dto.setVisitYearMonth("未知");
            }
            dto.setDays(dest.getDays());
            dto.setNotes(dest.getNotes());
            dto.setItineraryId(dest.getItineraryId()); // 使用itineraryId替代autoAdd
            dto.setStartDate(dest.getStartDate());
            dto.setEndDate(dest.getEndDate());

            // 获取目的地详细信息
            if (dest.getDestination() != null) {
                dto.setName(dest.getDestination().getName());
                dto.setDescription(dest.getDestination().getDescription());
                dto.setImageUrl(dest.getDestination().getImageUrl());
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserProfileResponse.WishlistDestinationDto> getWishlistDestinations(Long userId) {
        Long preferencesId = getOrCreateUserPreferencesId(userId);

        List<UserDestination> destinations = userDestinationRepository
                .findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(preferencesId, UserDestination.Type.期望目的地);

        return destinations.stream().map(dest -> {
            UserProfileResponse.WishlistDestinationDto dto = new UserProfileResponse.WishlistDestinationDto();
            dto.setDestinationId(dest.getDestinationId());
            dto.setNotes(dest.getNotes());

            // 获取目的地详细信息
            if (dest.getDestination() != null) {
                dto.setName(dest.getDestination().getName());
                dto.setDescription(dest.getDestination().getDescription());
                dto.setImageUrl(dest.getDestination().getImageUrl());
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public void addHistoryDestination(Long userId, AddHistoryDestinationRequest request) {
        Long preferencesId = getOrCreateUserPreferencesId(userId);

        // 创建或查找目的地
        Destination destination = findOrCreateDestination(request.getName(), request.getDescription());

        // 创建用户目的地记录
        UserDestination userDestination = new UserDestination();
        userDestination.setUserPreferencesId(preferencesId);
        userDestination.setDestinationId(destination.getId());
        userDestination.setType(UserDestination.Type.历史目的地);

        userDestination.setDays(request.getDays());
        userDestination.setNotes(request.getNotes());
        userDestination.setItineraryId(0L); // 设置为0表示手动添加
        userDestination.setStartDate(request.getStartDate());
        userDestination.setEndDate(request.getEndDate());

        userDestinationRepository.save(userDestination);
    }

    @Override
    public void addWishlistDestination(Long userId, AddWishlistDestinationRequest request) {
        Long preferencesId = getOrCreateUserPreferencesId(userId);

        // 创建或查找目的地
        Destination destination = findOrCreateDestination(request.getName(), request.getDescription());

        // 检查是否已存在期望目的地
        if (userDestinationRepository.countByUserPreferencesIdAndDestinationIdAndType(preferencesId, destination.getId(), UserDestination.Type.期望目的地) > 0) {
            throw new RuntimeException("该目的地已在您的期望目的地列表中");
        }

        // 创建用户目的地记录
        UserDestination userDestination = new UserDestination();
        userDestination.setUserPreferencesId(preferencesId);
        userDestination.setDestinationId(destination.getId());
        userDestination.setType(UserDestination.Type.期望目的地);
        // 为期望目的地设置特殊的日期和天数值
        userDestination.setStartDate(LocalDate.of(1900, 1, 1)); // 设置为1900-01-01表示期望目的地
        userDestination.setEndDate(LocalDate.of(1900, 1, 1));
        userDestination.setDays(-1);
        userDestination.setNotes(request.getNotes());

        userDestinationRepository.save(userDestination);
    }

    @Override
    public void removeHistoryDestination(Long userId, Long destinationId) {
        Long preferencesId = getOrCreateUserPreferencesId(userId);

        List<UserDestination> destinations = userDestinationRepository
                .findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(preferencesId, UserDestination.Type.历史目的地);

        destinations.stream()
                .filter(dest -> dest.getDestinationId().equals(destinationId))
                .forEach(userDestinationRepository::delete);
    }

    @Override
    public void removeWishlistDestination(Long userId, Long destinationId) {
        Long preferencesId = getOrCreateUserPreferencesId(userId);

        List<UserDestination> destinations = userDestinationRepository
                .findByUserPreferencesIdAndTypeOrderByCreatedAtDesc(preferencesId, UserDestination.Type.期望目的地);

        destinations.stream()
                .filter(dest -> dest.getDestinationId().equals(destinationId))
                .forEach(userDestinationRepository::delete);
    }

    // 辅助方法
    private Long getOrCreateUserPreferencesId(Long userId) {
        UserPreferences preferences = userPreferencesRepository.findByUserId(userId).orElse(null);

        if (preferences == null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            preferences = new UserPreferences();
            preferences.setUser(user);
            preferences = userPreferencesRepository.save(preferences);
        }

        return preferences.getId();
    }

    private Destination findOrCreateDestination(String name, String description) {
        Optional<Destination> existing = destinationRepository.findByName(name);

        if (existing.isPresent()) {
            return existing.get();
        }

        // 创建新目的地
        Destination destination = new Destination();
        destination.setName(name);
        destination.setDescription(description);
        destination.setJoinCount(0);

        return destinationRepository.save(destination);
    }

    private List<String> parseJsonToStringList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<UserProfileResponse.TagPreferenceDto> parseTravelPreferences(String travelPreferencesJson) {
        List<UserProfileResponse.TagPreferenceDto> result = new ArrayList<>();

        // 获取所有标签
        List<Tag> allTags = tagRepository.findAllByOrderById();

        // 解析用户偏好JSON
        Map<String, Integer> preferences = new HashMap<>();
        if (travelPreferencesJson != null && !travelPreferencesJson.trim().isEmpty()) {
            try {
                preferences = objectMapper.readValue(travelPreferencesJson, new TypeReference<Map<String, Integer>>() {});
            } catch (Exception e) {
                // 解析失败，使用空的偏好
                preferences = new HashMap<>();
            }
        }

        // 构建响应数据
        for (Tag tag : allTags) {
            String tagIdStr = tag.getId().toString();
            Integer preferenceValue = preferences.getOrDefault(tagIdStr, 0);
            boolean selected = preferenceValue != null && preferenceValue == 1;

            result.add(new UserProfileResponse.TagPreferenceDto(tag.getId(), tag.getTag(), selected));
        }

        return result;
    }

    @Override
    public int addHistoryDestinationsFromCompletedItineraries(Long userId) {
        Long preferencesId = getOrCreateUserPreferencesId(userId);
        
        // 获取用户所有已出行的行程
        List<Itinerary> completedItineraries = itineraryRepository
                .findByUserIdAndTravelStatus(userId, Itinerary.TravelStatus.已出行);
        
        if (completedItineraries.isEmpty()) {
            return 0;
        }
        
        int addedCount = 0;
        
        // 对每个已出行的行程单独处理
        for (Itinerary itinerary : completedItineraries) {
            // 收集该行程中的所有目的地
            Set<Long> itineraryDestinationIds = new HashSet<>();
            if (itinerary.getItineraryDays() != null) {
                for (ItineraryDay day : itinerary.getItineraryDays()) {
                    if (day.getActivities() != null) {
                        for (ItineraryActivity activity : day.getActivities()) {
                            if (activity.getAttraction() != null && 
                                activity.getAttraction().getDestination() != null) {
                                itineraryDestinationIds.add(activity.getAttraction().getDestination().getId());
                            }
                        }
                    }
                }
            }
            
            if (itineraryDestinationIds.isEmpty()) {
                continue; // 跳过没有目的地的行程
            }
            
            // 为该行程的每个目的地创建历史记录
            for (Long destinationId : itineraryDestinationIds) {
                // 检查是否已存在该目的地和行程的历史记录
                List<UserDestination> existing = userDestinationRepository
                        .findByUserPreferencesIdAndItineraryId(preferencesId, itinerary.getId());
                
                boolean alreadyExists = existing.stream()
                        .anyMatch(ud -> ud.getDestinationId().equals(destinationId) && 
                                      ud.getType() == UserDestination.Type.历史目的地);
                
                if (!alreadyExists) {
                    // 计算该目的地在此行程中的天数
                    Set<LocalDate> visitedDates = new HashSet<>();
                    if (itinerary.getItineraryDays() != null) {
                        for (ItineraryDay day : itinerary.getItineraryDays()) {
                            if (day.getActivities() != null) {
                                for (ItineraryActivity activity : day.getActivities()) {
                                    if (activity.getAttraction() != null && 
                                        activity.getAttraction().getDestination() != null &&
                                        activity.getAttraction().getDestination().getId().equals(destinationId)) {
                                        visitedDates.add(day.getDate());
                                    }
                                }
                            }
                        }
                    }
                    
                    // 创建新的历史目的地记录
                    UserDestination userDestination = new UserDestination();
                    userDestination.setUserPreferencesId(preferencesId);
                    userDestination.setDestinationId(destinationId);
                    userDestination.setType(UserDestination.Type.历史目的地);
                    userDestination.setItineraryId(itinerary.getId()); // 关联到具体行程
                    userDestination.setStartDate(itinerary.getStartDate());
                    userDestination.setEndDate(itinerary.getEndDate());
                    userDestination.setDays(visitedDates.size());
                    userDestination.setNotes("在行程\"" + itinerary.getTitle() + "\"中游玩");
                    
                    userDestinationRepository.save(userDestination);
                    addedCount++;
                }
            }
        }
        
        return addedCount;
    }
    
    @Override
    public int removeAutoAddedHistoryDestinationsFromItinerary(Long userId, Long itineraryId) {
        Long preferencesId = getOrCreateUserPreferencesId(userId);
        
        // 获取指定的行程
        Itinerary itinerary = itineraryRepository.findById(itineraryId).orElse(null);
        if (itinerary == null || !itinerary.getUser().getId().equals(userId)) {
            return 0; // 行程不存在或不属于该用户
        }
        
        // 直接查找该行程对应的所有目的地记录
        List<UserDestination> itineraryDestinations = userDestinationRepository
                .findByUserPreferencesIdAndItineraryId(preferencesId, itineraryId);
        
        // 过滤出历史目的地类型的记录
        List<UserDestination> historyDestinationsToRemove = itineraryDestinations.stream()
                .filter(ud -> ud.getType() == UserDestination.Type.历史目的地)
                .collect(Collectors.toList());
        
        int removedCount = historyDestinationsToRemove.size();
        
        // 删除这些记录
        if (!historyDestinationsToRemove.isEmpty()) {
            userDestinationRepository.deleteAll(historyDestinationsToRemove);
        }
        
        return removedCount;
    }
    


    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                throw new RuntimeException("文件不能为空");
            }

            // 验证文件类型
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || !isValidImageFile(originalFileName)) {
                throw new RuntimeException("文件格式不支持，请上传 JPG、PNG 或 GIF 格式的图片");
            }

            // 验证文件大小（限制为2MB）
            long fileSize = file.getSize();
            long maxSize = 2 * 1024 * 1024; // 2MB
            
            if (fileSize > maxSize) {
                throw new RuntimeException("头像文件大小不能超过2MB");
            }

            // 创建上传目录
            String uploadDir = "../uploads/avatars/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String fileExtension = getFileExtension(originalFileName);
            String fileName = UUID.randomUUID().toString() + "." + fileExtension;

            // 保存文件
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            // 构建访问URL - 确保与StaticResourceConfig一致
            String avatarUrl = "/avatars/" + fileName;

            // 更新用户头像URL
            UserProfile profile = userProfileRepository.findByUserId(userId).orElse(new UserProfile());
            if (profile.getId() == null) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("用户不存在"));
                profile.setUser(user);
            }
            profile.setAvatarUrl(avatarUrl);
            userProfileRepository.save(profile);

            return avatarUrl;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    private boolean isValidImageFile(String filename) {
        String lowerCase = filename.toLowerCase();
        return lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") || 
               lowerCase.endsWith(".png") || lowerCase.endsWith(".gif");
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    @Override
    public TravelStatsResponse getTravelStats(Long userId) {
        TravelStatsResponse response = new TravelStatsResponse();
        
        // 获取用户基本信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        response.setUsername(user.getUsername());
        
        // 计算陪伴天数（从注册到现在）
        LocalDate registrationDate = user.getCreatedAt() != null ? 
            user.getCreatedAt().toLocalDate() : LocalDate.now();
        long companionDays = ChronoUnit.DAYS.between(registrationDate, LocalDate.now()) + 1;
        response.setCompanionDays((int) companionDays);
        
        // 获取历史目的地数据
        List<UserProfileResponse.HistoryDestinationDto> historyDestinations = getHistoryDestinations(userId);
        response.setTotalDestinations(historyDestinations.size());
        
        // 计算总旅行天数
        int totalDays = historyDestinations.stream()
                .mapToInt(dest -> dest.getDays() != null ? dest.getDays() : 0)
                .sum();
        response.setTotalDays(totalDays);
        
        // 获取已完成行程数量
        List<Itinerary> completedItineraries = itineraryRepository
                .findByUserIdAndTravelStatus(userId, Itinerary.TravelStatus.已出行);
        response.setTotalItineraries(completedItineraries.size());
        
        // 构建时间轴数据
        List<TravelStatsResponse.TimelineItem> timeline = buildTimeline(userId, historyDestinations, completedItineraries);
        response.setTimeline(timeline);
        
        // 构建地理统计（改进版本）
        TravelStatsResponse.GeographyStats geography = buildGeographyStats(historyDestinations);
        response.setGeography(geography);
        
        // 构建城市统计
        List<TravelStatsResponse.CityStats> topCities = buildCityStats(historyDestinations);
        response.setTopCities(topCities);
        
        return response;
    }
    
    private List<TravelStatsResponse.TimelineItem> buildTimeline(Long userId, 
            List<UserProfileResponse.HistoryDestinationDto> historyDestinations,
            List<Itinerary> completedItineraries) {
        
        List<TravelStatsResponse.TimelineItem> timeline = new ArrayList<>();
        
        // 创建行程到目的地列表的映射，以及行程的基本信息
        Map<Long, ItineraryInfo> itineraryInfoMap = new HashMap<>();
        Map<String, Long> destinationToItineraryMap = new HashMap<>();
        
        for (Itinerary itinerary : completedItineraries) {
            Set<String> destinationsInItinerary = new HashSet<>();
            LocalDate startDate = null;
            LocalDate endDate = null;
            int totalDays = 0;
            
            if (itinerary.getItineraryDays() != null) {
                for (ItineraryDay day : itinerary.getItineraryDays()) {
                    if (day.getActivities() != null) {
                        for (ItineraryActivity activity : day.getActivities()) {
                            if (activity.getAttraction() != null && 
                                activity.getAttraction().getDestination() != null) {
                                String destName = activity.getAttraction().getDestination().getName();
                                destinationsInItinerary.add(destName);
                                destinationToItineraryMap.put(destName, itinerary.getId());
                            }
                        }
                    }
                    
                    // 计算开始和结束日期
                    if (startDate == null || day.getDate().isBefore(startDate)) {
                        startDate = day.getDate();
                    }
                    if (endDate == null || day.getDate().isAfter(endDate)) {
                        endDate = day.getDate();
                    }
                    totalDays++;
                }
            }
            
            ItineraryInfo info = new ItineraryInfo();
            info.id = itinerary.getId();
            info.title = itinerary.getTitle();
            info.destinations = new ArrayList<>(destinationsInItinerary);
            info.startDate = startDate;
            info.endDate = endDate;
            info.totalDays = totalDays;
            itineraryInfoMap.put(itinerary.getId(), info);
        }
        
        // 分离自动添加和手动添加的目的地
        List<UserProfileResponse.HistoryDestinationDto> autoAddedDestinations = new ArrayList<>();
        List<UserProfileResponse.HistoryDestinationDto> manualDestinations = new ArrayList<>();
        
        for (UserProfileResponse.HistoryDestinationDto dest : historyDestinations) {
            if (dest.isAutoAdded()) { // 使用新的方法判断是否为自动添加
                autoAddedDestinations.add(dest);
            } else {
                manualDestinations.add(dest);
            }
        }
        
        // 处理自动添加的目的地（按行程合并）
        Map<Long, List<UserProfileResponse.HistoryDestinationDto>> itineraryDestinationsMap = new HashMap<>();
        for (UserProfileResponse.HistoryDestinationDto dest : autoAddedDestinations) {
            Long itineraryId = dest.getItineraryId(); // 直接使用itineraryId字段
            if (itineraryId != null && itineraryId > 0) {
                itineraryDestinationsMap.computeIfAbsent(itineraryId, k -> new ArrayList<>()).add(dest);
            }
        }
        
        // 为每个行程创建合并的时间轴项目
        for (Map.Entry<Long, List<UserProfileResponse.HistoryDestinationDto>> entry : itineraryDestinationsMap.entrySet()) {
            Long itineraryId = entry.getKey();
            List<UserProfileResponse.HistoryDestinationDto> destinations = entry.getValue();
            ItineraryInfo itineraryInfo = itineraryInfoMap.get(itineraryId);
            
            if (itineraryInfo != null && !destinations.isEmpty()) {
                TravelStatsResponse.TimelineItem item = new TravelStatsResponse.TimelineItem();
                
                // 合并目的地名称（用逗号分隔）
                String combinedName = destinations.stream()
                    .map(UserProfileResponse.HistoryDestinationDto::getName)
                    .collect(Collectors.joining("、"));
                item.setName(combinedName);
                
                // 使用行程的时间信息
                UserProfileResponse.HistoryDestinationDto firstDest = destinations.get(0);
                item.setVisitYearMonth(firstDest.getVisitYearMonth());
                item.setStartDate(itineraryInfo.startDate);
                item.setEndDate(itineraryInfo.endDate);
                item.setDays(itineraryInfo.totalDays);
                
                // 使用行程标题作为备注
                item.setNotes("行程：" + itineraryInfo.title);
                
                item.setHasItinerary(true);
                item.setItineraryId(itineraryId);
                
                timeline.add(item);
            }
        }
        
        // 处理手动添加的目的地（保持单独显示）
        for (UserProfileResponse.HistoryDestinationDto dest : manualDestinations) {
            TravelStatsResponse.TimelineItem item = new TravelStatsResponse.TimelineItem();
            item.setName(dest.getName());
            item.setVisitYearMonth(dest.getVisitYearMonth());
            item.setDays(dest.getDays());
            item.setNotes(dest.getNotes());
            item.setStartDate(dest.getStartDate());
            item.setEndDate(dest.getEndDate());
            item.setHasItinerary(false);
            item.setItineraryId(null);
            
            timeline.add(item);
        }
        
        // 按时间排序（按出行日期先后顺序）- 优先使用startDate，如果没有则使用visitYearMonth
        timeline.sort((a, b) -> {
            // 如果都有startDate，使用startDate排序（正序，早的在前）
            if (a.getStartDate() != null && b.getStartDate() != null) {
                return a.getStartDate().compareTo(b.getStartDate());
            }
            // 如果只有一个有startDate，有startDate的排在前面
            if (a.getStartDate() != null && b.getStartDate() == null) {
                return -1;
            }
            if (a.getStartDate() == null && b.getStartDate() != null) {
                return 1;
            }
            // 如果都没有startDate，使用visitYearMonth排序（正序，早的在前）
            return a.getVisitYearMonth().compareTo(b.getVisitYearMonth());
        });
        
        return timeline;
    }
    
    // 辅助类：存储行程信息
    private static class ItineraryInfo {
        Long id;
        String title;
        List<String> destinations;
        LocalDate startDate;
        LocalDate endDate;
        int totalDays;
    }
    
    private TravelStatsResponse.GeographyStats buildGeographyStats(
            List<UserProfileResponse.HistoryDestinationDto> historyDestinations) {
        
        TravelStatsResponse.GeographyStats geography = new TravelStatsResponse.GeographyStats();
        
        if (historyDestinations.isEmpty()) {
            return geography;
        }
        
        // 从数据库获取目的地的真实信息
        List<String> destinationNames = historyDestinations.stream()
                .map(UserProfileResponse.HistoryDestinationDto::getName)
                .collect(Collectors.toList());
        
        List<Destination> destinations = destinationRepository.findByNameIn(destinationNames);
        
        // 基于目的地名称进行地理位置判断（改进版本）
        // 使用列表来收集所有匹配的城市，可以让一个城市出现在多个方向
        List<String> easternCities = new ArrayList<>();
        List<String> southernCities = new ArrayList<>();
        List<String> westernCities = new ArrayList<>();
        List<String> northernCities = new ArrayList<>();
        
        for (Destination dest : destinations) {
            String name = dest.getName();
            String description = dest.getDescription();
            
            // 更全面的地理位置判断逻辑
            if (isEasternCity(name, description)) {
                easternCities.add(name);
            }
            if (isSouthernCity(name, description)) {
                southernCities.add(name);
            }
            if (isWesternCity(name, description)) {
                westernCities.add(name);
            }
            if (isNorthernCity(name, description)) {
                northernCities.add(name);
            }
        }
        
        // 选择每个方向的代表城市（优先选择第一个，以后可以改为选择访问次数最多的）
        geography.setEasternmost(easternCities.isEmpty() ? null : easternCities.get(0));
        geography.setSouthernmost(southernCities.isEmpty() ? null : southernCities.get(0));
        geography.setWesternmost(westernCities.isEmpty() ? null : westernCities.get(0));
        geography.setNorthernmost(northernCities.isEmpty() ? null : northernCities.get(0));
        
        // 统计最爱出行月份
        Map<String, Integer> monthCount = new HashMap<>();
        for (UserProfileResponse.HistoryDestinationDto dest : historyDestinations) {
            // 优先使用startDate，如果没有则使用visitYearMonth（兼容性）
            if (dest.getStartDate() != null) {
                String month = String.format("%02d", dest.getStartDate().getMonthValue());
                monthCount.put(month, monthCount.getOrDefault(month, 0) + 1);
            } else if (dest.getVisitYearMonth() != null && dest.getVisitYearMonth().length() >= 7 && !dest.getVisitYearMonth().equals("未知")) {
                String month = dest.getVisitYearMonth().substring(5, 7); // 提取月份
                monthCount.put(month, monthCount.getOrDefault(month, 0) + 1);
            }
        }
        
        String favoriteMonth = monthCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> getMonthName(entry.getKey()))
                .orElse(null);
        geography.setFavoriteMonth(favoriteMonth);
        
        // 统计旅行次数最多的年份
        Map<String, Integer> yearCount = new HashMap<>();
        for (UserProfileResponse.HistoryDestinationDto dest : historyDestinations) {
            // 优先使用startDate，如果没有则使用visitYearMonth（兼容性）
            if (dest.getStartDate() != null) {
                String year = String.valueOf(dest.getStartDate().getYear());
                yearCount.put(year, yearCount.getOrDefault(year, 0) + 1);
            } else if (dest.getVisitYearMonth() != null && dest.getVisitYearMonth().length() >= 4 && !dest.getVisitYearMonth().equals("未知")) {
                String year = dest.getVisitYearMonth().substring(0, 4); // 提取年份
                yearCount.put(year, yearCount.getOrDefault(year, 0) + 1);
            }
        }
        
        String mostTravelYear = yearCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        geography.setMostTravelYear(mostTravelYear);
        
        return geography;
    }
    
    // 地理位置判断辅助方法
    private boolean isEasternCity(String name, String description) {
        return name.contains("上海") || name.contains("杭州") || name.contains("南京") || 
               name.contains("苏州") || name.contains("福州") || name.contains("厦门") ||
               name.contains("青岛") || name.contains("大连") || name.contains("宁波") ||
               (description != null && (description.contains("华东") || description.contains("东部")));
    }
    
    private boolean isSouthernCity(String name, String description) {
        return name.contains("三亚") || name.contains("海南") || name.contains("广州") ||
               name.contains("深圳") || name.contains("珠海") || name.contains("桂林") ||
               name.contains("昆明") || name.contains("南宁") || name.contains("湛江") ||
               (description != null && (description.contains("华南") || description.contains("南部")));
    }
    
    private boolean isWesternCity(String name, String description) {
        return name.contains("西藏") || name.contains("新疆") || name.contains("拉萨") ||
               name.contains("乌鲁木齐") || name.contains("成都") || name.contains("重庆") ||
               name.contains("西安") || name.contains("兰州") || name.contains("银川") ||
               (description != null && (description.contains("西部") || description.contains("西北") || description.contains("西南")));
    }
    
    private boolean isNorthernCity(String name, String description) {
        return name.contains("哈尔滨") || name.contains("长春") || name.contains("沈阳") ||
               name.contains("北京") || name.contains("天津") || name.contains("太原") ||
               name.contains("呼和浩特") || name.contains("大同") || name.contains("包头") ||
               (description != null && (description.contains("华北") || description.contains("东北") || description.contains("北部")));
    }
    
    // 月份名称转换
    private String getMonthName(String month) {
        switch (month) {
            case "01": return "1月";
            case "02": return "2月";
            case "03": return "3月";
            case "04": return "4月";
            case "05": return "5月";
            case "06": return "6月";
            case "07": return "7月";
            case "08": return "8月";
            case "09": return "9月";
            case "10": return "10月";
            case "11": return "11月";
            case "12": return "12月";
            default: return month + "月";
        }
    }
    
    private List<TravelStatsResponse.CityStats> buildCityStats(
            List<UserProfileResponse.HistoryDestinationDto> historyDestinations) {
        
        Map<String, TravelStatsResponse.CityStats> cityStatsMap = new HashMap<>();
        
        for (UserProfileResponse.HistoryDestinationDto dest : historyDestinations) {
            String cityName = dest.getName();
            TravelStatsResponse.CityStats stats = cityStatsMap.get(cityName);
            
            if (stats == null) {
                stats = new TravelStatsResponse.CityStats();
                stats.setName(cityName);
                stats.setVisitCount(0);
                stats.setTotalDays(0);
                cityStatsMap.put(cityName, stats);
            }
            
            stats.setVisitCount(stats.getVisitCount() + 1);
            stats.setTotalDays(stats.getTotalDays() + (dest.getDays() != null ? dest.getDays() : 0));
        }
        
        // 按访问次数排序，取前5名
        return cityStatsMap.values().stream()
                .sorted((a, b) -> b.getVisitCount().compareTo(a.getVisitCount()))
                .limit(5)
                .collect(Collectors.toList());
    }

    @Override
    public UserHomepageResponse getUserHomepage(Long userId, String requestIp) {
        // 获取用户基本信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 获取用户档案
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        // 构建响应对象
        UserHomepageResponse response = new UserHomepageResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setIp(requestIp);

        if (profile != null) {
            response.setBirthday(profile.getBirthday());
            response.setSignature(profile.getSignature());
            response.setBio(profile.getBio());
            response.setAvatarUrl(profile.getAvatarUrl());
        }

        // 获取该用户的所有公开行程
        List<Itinerary> publicItineraries = itineraryRepository
                .findByUserIdAndPermissionStatus(userId, Itinerary.PermissionStatus.所有人可见);

        // 转换为DTO格式
        List<UserHomepageResponse.PublicItineraryDto> publicItineraryDtos = publicItineraries.stream()
                .map(this::convertToPublicItineraryDto)
                .collect(Collectors.toList());

        response.setPublicItineraries(publicItineraryDtos);

        return response;
    }

    private UserHomepageResponse.PublicItineraryDto convertToPublicItineraryDto(Itinerary itinerary) {
        UserHomepageResponse.PublicItineraryDto dto = new UserHomepageResponse.PublicItineraryDto();
        dto.setId(itinerary.getId());
        dto.setTitle(itinerary.getTitle());
        dto.setImageUrl(itinerary.getImageUrl());
        dto.setStartDate(itinerary.getStartDate());
        dto.setEndDate(itinerary.getEndDate());

        // 计算行程天数
        if (itinerary.getStartDate() != null && itinerary.getEndDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(itinerary.getStartDate(), itinerary.getEndDate()) + 1;
            dto.setDuration(days + "天");
        } else {
            dto.setDuration("未知");
        }

        // 提取目的地信息
        Set<String> destinations = new HashSet<>();
        if (itinerary.getItineraryDays() != null) {
            for (ItineraryDay day : itinerary.getItineraryDays()) {
                if (day.getActivities() != null) {
                    for (ItineraryActivity activity : day.getActivities()) {
                        if (activity.getAttraction() != null && 
                            activity.getAttraction().getDestination() != null) {
                            destinations.add(activity.getAttraction().getDestination().getName());
                        }
                    }
                }
            }
        }

        if (!destinations.isEmpty()) {
            dto.setDestination(destinations.stream().collect(Collectors.joining("、")));
        } else {
            dto.setDestination("待规划目的地");
        }

        dto.setDescription("精彩的旅行体验，快来一起探索吧！");

        return dto;
    }

    @Override
    public List<Map<String, Object>> searchUsers(String username, Long currentUserId) {
        return userRepository.findByUsernameContaining(username).stream()
            .filter(u -> !u.getId().equals(currentUserId))
            .map(u -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("username", u.getUsername());
                m.put("avatar", u.getUsername() != null ? u.getUsername().substring(0, 1) : "U");
                return m;
            })
            .collect(Collectors.toList());
    }
} 
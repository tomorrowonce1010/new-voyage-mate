package com.se_07.backend.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.se_07.backend.dto.*;
import com.se_07.backend.dto.converter.ItineraryConverter;
import com.se_07.backend.entity.*;
import com.se_07.backend.repository.*;
import com.se_07.backend.service.EmbeddingService;
import com.se_07.backend.service.ItineraryService;
import com.se_07.backend.service.UserService;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ItineraryServiceImpl implements ItineraryService {

    private static final Logger logger = LoggerFactory.getLogger(ItineraryServiceImpl.class);

    @Autowired
    private ItineraryRepository itineraryRepository;
    
    @Autowired
    private ItineraryDayRepository itineraryDayRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;

    @Autowired
    private ItineraryConverter itineraryConverter;

    @Autowired
    private CommunityEntryRepository communityEntryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private CommunityEntryTagRepository communityEntryTagRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ItineraryActivityRepository itineraryActivityRepository;

    @Value("${elasticsearch.host:localhost}")
    private String elasticsearchHost;

    @Value("${elasticsearch.port:9200}")
    private int elasticsearchPort;

    @Value("${elasticsearch.index.community:community_entries}")
    private String communityEntriesIndex;

    @Autowired
    private TravelGroupRepository travelGroupRepository;

    @Autowired
    private TravelGroupMemberRepository travelGroupMemberRepository;

    @Autowired
    private GroupItineraryRepository groupItineraryRepository;

    @Autowired
    private com.se_07.backend.repository.AttractionRepository attractionRepository;

    // 将封面图片保存到 uploads/covers 目录（外部可写），由资源映射提供访问
    private static final String UPLOAD_DIR = "uploads/covers/";

    // 用于生成分享码的字符集
    private static final String SHARE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHARE_CODE_LENGTH = 16;

    @Override
    @Transactional
    public ItineraryDTO createItinerary(Long userId, ItineraryCreateRequest request) {
        logger.info("开始创建行程 - 用户ID: {}, 请求数据: {}", userId, request);

        // 查找用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 验证日期
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("结束日期不能早于开始日期");
        }
        
        // 验证出行人数
        Integer travelerCount = request.getTravelerCount();
        logger.info("请求中的出行人数: {}", travelerCount);

        if (travelerCount != null && (travelerCount <= 0 || travelerCount > 50)) {
            logger.warn("出行人数超出合理范围: {}, 使用默认值2", travelerCount);
            travelerCount = 2;
        } else if (travelerCount == null) {
            logger.info("出行人数为空，使用默认值2");
            travelerCount = 2;
        }
        
        // 创建行程实体
        Itinerary itinerary = new Itinerary();
        itinerary.setUser(user);
        itinerary.setTitle(request.getTitle());
        itinerary.setImageUrl(request.getImageUrl());
        itinerary.setStartDate(request.getStartDate());
        itinerary.setEndDate(request.getEndDate());
        itinerary.setBudget(request.getBudget());
        itinerary.setTravelerCount(travelerCount);

        logger.info("设置出行人数: {}", itinerary.getTravelerCount());
        
        // 设置旅行状态，如果未指定则默认为"待出行"
        if (request.getTravelStatus() != null && !request.getTravelStatus().trim().isEmpty()) {
            try {
                itinerary.setTravelStatus(Itinerary.TravelStatus.valueOf(request.getTravelStatus().trim()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("无效的旅行状态: " + request.getTravelStatus());
            }
        } else {
            itinerary.setTravelStatus(Itinerary.TravelStatus.待出行);
        }
        
        itinerary.setEditStatus(Itinerary.EditStatus.草稿); // 新建行程默认为草稿状态
        
        // 设置权限状态，如果未指定则默认为"私人"
        if (request.getPermissionStatus() != null && !request.getPermissionStatus().trim().isEmpty()) {
            try {
                itinerary.setPermissionStatus(Itinerary.PermissionStatus.valueOf(request.getPermissionStatus().trim()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("无效的权限状态: " + request.getPermissionStatus());
            }
        } else {
            itinerary.setPermissionStatus(Itinerary.PermissionStatus.私人);
        }
        
        logger.info("保存行程前 - 出行人数: {}", itinerary.getTravelerCount());
        
        // 保存行程
        Itinerary savedItinerary = itineraryRepository.save(itinerary);
        
        logger.info("行程保存成功 - ID: {}, 出行人数: {}", savedItinerary.getId(), savedItinerary.getTravelerCount());
        
        // 根据起始和结束日期创建日程
        createItineraryDays(savedItinerary);

        // 重新获取包含完整itineraryDays信息的itinerary对象
        Itinerary completeItinerary = itineraryRepository.findById(savedItinerary.getId())
                .orElseThrow(() -> new RuntimeException("行程创建失败"));
        
        logger.info("行程创建完成 - ID: {}, 出行人数: {}", completeItinerary.getId(), completeItinerary.getTravelerCount());

        ItineraryDTO dto = itineraryConverter.toDTO(completeItinerary);
        logger.info("转换为DTO - 出行人数: {}", dto.getTravelerCount());

        return dto;
    }
    
    /**
     * 根据行程的起始和结束日期创建空的日程
     */
    private void createItineraryDays(Itinerary itinerary) {
        LocalDate startDate = itinerary.getStartDate();
        LocalDate endDate = itinerary.getEndDate();
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        for (int i = 0; i < daysBetween; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            
            ItineraryDay day = new ItineraryDay();
            day.setItinerary(itinerary);
            day.setDayNumber(i + 1);
            day.setDate(currentDate);
            day.setTitle("待规划");
            day.setFirstActivityId(null);  // 使用 null 而不是 -1
            day.setLastActivityId(null);   // 使用 null 而不是 -1
            
            // 这里可以根据需要设置其他默认值
            // day.setAccommodation("{}");
            // day.setWeatherInfo("{}");
            
            // 保存日程
            itineraryDayRepository.save(day);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItineraryDTO> getUserItineraries(Long userId, Pageable pageable) {
        // 检查用户是否存在
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("用户不存在");
        }

        // 获取用户的行程列表
        Page<Itinerary> itinerariesPage = itineraryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // 转换为DTO
        return itinerariesPage.getContent().stream()
                .map(itineraryConverter::toDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ItineraryDTO getItineraryById(Long itineraryId, Long userId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));
        
        // 检查用户权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 检查是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                // 如果不是团队成员，检查是否是行程创建者或权限状态
        if (!itinerary.getUser().getId().equals(userId) && 
            itinerary.getPermissionStatus() == Itinerary.PermissionStatus.私人) {
            throw new RuntimeException("无权访问此行程");
                }
            }
        } else {
            // 个人行程 - 验证是否是创建者或具有查看权限
            if (!itinerary.getUser().getId().equals(userId) &&
                itinerary.getPermissionStatus() == Itinerary.PermissionStatus.私人) {
                throw new RuntimeException("无权访问此行程");
            }
        }
        
        // 触发懒加载，确保日程数据被加载
        if (itinerary.getItineraryDays() != null) {
            itinerary.getItineraryDays().size(); // 触发懒加载
        }
        
        return itineraryConverter.toDTO(itinerary);
    }

    @Override
    @Transactional
    public ItineraryDTO updateItinerary(Long userId, Long itineraryId, ItineraryUpdateRequest request) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));

        // 检查用户权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 检查是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                // 如果不是团队成员，检查是否是行程创建者
        if (!itinerary.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限修改此行程");
                }
            }

            // 检查团队行程状态，已出行的行程不能编辑
            if (itinerary.getTravelStatus() == Itinerary.TravelStatus.已出行) {
                throw new RuntimeException("已出行的行程不能编辑，只能查看");
            }
        } else {
            // 个人行程 - 只有创建者可以修改
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权限修改此行程");
            }

            // 检查个人行程状态，已出行的行程不能编辑
            if (itinerary.getTravelStatus() == Itinerary.TravelStatus.已出行) {
                throw new RuntimeException("已出行的行程不能编辑，只能查看");
            }
        }

        // 保存旧的权限状态
        Itinerary.PermissionStatus oldPermissionStatus = itinerary.getPermissionStatus();

        // 更新行程信息
        if (request.getTitle() != null) {
            itinerary.setTitle(request.getTitle());
        }
        if (request.getStartDate() != null) {
            itinerary.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            itinerary.setEndDate(request.getEndDate());
        }
        if (request.getBudget() != null) {
            itinerary.setBudget(request.getBudget());
        }
        if (request.getTravelerCount() != null) {
            itinerary.setTravelerCount(request.getTravelerCount());
        }
        if (request.getTravelStatus() != null && !request.getTravelStatus().trim().isEmpty()) {
            try {
                itinerary.setTravelStatus(Itinerary.TravelStatus.valueOf(request.getTravelStatus().trim()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("无效的旅行状态: " + request.getTravelStatus());
            }
        }
        if (request.getPermissionStatus() != null && !request.getPermissionStatus().trim().isEmpty()) {
            try {
                itinerary.setPermissionStatus(Itinerary.PermissionStatus.valueOf(request.getPermissionStatus().trim()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("无效的权限状态: " + request.getPermissionStatus());
            }
        }

        // 保存更新后的行程
        Itinerary updatedItinerary = itineraryRepository.save(itinerary);

        // 如果权限状态发生变化，处理相关逻辑
        if (oldPermissionStatus != updatedItinerary.getPermissionStatus()) {
            handlePermissionStatusChange(updatedItinerary, oldPermissionStatus);
        }

        return itineraryConverter.toDTO(updatedItinerary);
    }

    private void handlePermissionStatusChange(Itinerary itinerary, Itinerary.PermissionStatus oldStatus) {
        Itinerary.PermissionStatus newStatus = itinerary.getPermissionStatus();
        
        if (newStatus == Itinerary.PermissionStatus.私人) {
            // 变为私人：删除社区条目
            communityEntryRepository.deleteByItineraryId(itinerary.getId());
        } else if (oldStatus == Itinerary.PermissionStatus.私人 && newStatus == Itinerary.PermissionStatus.所有人可见) {
            // 从私人变为公开：如果有社区条目，保持；如果没有，不自动创建
            // 用户需要手动点击分享按钮来创建社区条目
        }
    }

    @Override
    @Transactional
    public void deleteItinerary(Long userId, Long itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));
        
        // 检查用户权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 团队发起人或行程创建者都可以删除
            TravelGroup group = travelGroupRepository.findById(itinerary.getGroupId())
                    .orElseThrow(() -> new RuntimeException("团队不存在"));
            boolean isGroupCreator = group.getCreator().getId().equals(userId);
            boolean isItineraryCreator = (itinerary.getCreator() != null && itinerary.getCreator().getId().equals(userId)) ||
                                        itinerary.getUser().getId().equals(userId);
            if (!isGroupCreator && !isItineraryCreator) {
                throw new RuntimeException("只有团队发起人或行程创建者可以删除团队行程");
            }
        } else {
            // 个人行程 - 只有创建者可以删除
        if (!itinerary.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权删除该行程");
            }
        }

        // 删除行程时，若行程状态为已出行，删除历史目的地中的该行程自动添加的目的地
        if (itinerary.getTravelStatus() == Itinerary.TravelStatus.已出行) {
            userService.removeAutoAddedHistoryDestinationsFromItinerary(userId, itineraryId);
        }
        
        // 删除相关的日程和活动（由于设置了CASCADE，会自动删除）
        itineraryRepository.delete(itinerary);
    }

    @Override
    public ItineraryDTO lockItinerary(Long itineraryId, Long userId) {
        // TODO: 实现锁定行程逻辑
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));
        
        // 检查用户权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 检查是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                // 如果不是团队成员，检查是否是行程创建者
        if (!itinerary.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权锁定该行程");
                }
            }
        } else {
            // 个人行程 - 只有创建者可以锁定
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权锁定该行程");
            }
        }
        
        itinerary.setTravelStatus(Itinerary.TravelStatus.已出行);
        Itinerary savedItinerary = itineraryRepository.save(itinerary);
        return itineraryConverter.toDTO(savedItinerary);
    }

    @Override
    public List<ItineraryDTO> getPendingItineraries(Long userId) {
        // 实现获取待出行行程逻辑
        System.out.println("--------------------------------start getPendingItineraries--------------------------------");
        List<Itinerary> itineraries = itineraryRepository.findByUserIdAndTravelStatus(userId, Itinerary.TravelStatus.待出行);
        return itineraries.stream()
                .map(itineraryConverter::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItineraryDTO> getCompletedItineraries(Long userId) {
        // 实现获取已出行行程逻辑
        System.out.println("--------------------------------start getCompletedItineraries--------------------------------");
        List<Itinerary> itineraries = itineraryRepository.findByUserIdAndTravelStatus(userId, Itinerary.TravelStatus.已出行);
        return itineraries.stream()
                .map(itineraryConverter::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ItineraryDTO setItineraryPermission(Long itineraryId, Long userId, String permissionStatus) {
        // 实现设置行程权限逻辑
        System.out.println("--------------------------------start setItineraryPermission--------------------------------");
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));

        // 检查用户权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 检查是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                // 如果不是团队成员，检查是否是行程创建者
                if (!itinerary.getUser().getId().equals(userId)) {
                    throw new RuntimeException("无权设置该行程权限");
                }
            }
        } else {
            // 个人行程 - 只有创建者可以设置权限
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权设置该行程权限");
            }
        }

        itinerary.setPermissionStatus(Itinerary.PermissionStatus.valueOf(permissionStatus));
        Itinerary savedItinerary = itineraryRepository.save(itinerary);
        return itineraryConverter.toDTO(savedItinerary);
    }

    @Override
    @Transactional
    public ItineraryDTO updateItineraryBasic(Long userId, Long itineraryId, Map<String, Object> updates) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));

        // 检查用户权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 检查是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                // 如果不是团队成员，检查是否是行程创建者
        if (!itinerary.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权修改该行程");
                }
            }

            // 检查团队行程状态，已出行的行程不能编辑
            if (itinerary.getTravelStatus() == Itinerary.TravelStatus.已出行) {
                throw new RuntimeException("已出行的行程不能编辑，只能查看");
            }
        } else {
            // 个人行程 - 只有创建者可以修改
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权修改该行程");
            }

            // 检查个人行程状态，已出行的行程不能编辑
            if (itinerary.getTravelStatus() == Itinerary.TravelStatus.已出行) {
                throw new RuntimeException("已出行的行程不能编辑，只能查看");
            }
        }

        // 更新基本信息
        if (updates.containsKey("title")) {
            itinerary.setTitle((String) updates.get("title"));
        }
        if (updates.containsKey("budget")) {
            Object budgetObj = updates.get("budget");
            if (budgetObj instanceof Number) {
                itinerary.setBudget(new BigDecimal(budgetObj.toString()));
            }
        }
        if (updates.containsKey("travelerCount")) {
            Object countObj = updates.get("travelerCount");
            if (countObj instanceof Number) {
                itinerary.setTravelerCount(((Number) countObj).intValue());
            }
        }

        Itinerary savedItinerary = itineraryRepository.save(itinerary);
        return itineraryConverter.toDTO(savedItinerary);
    }

    @Override
    @Transactional
    public ItineraryDTO updateItineraryStatus(Long userId, Long itineraryId, String status) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));

        // 检查用户权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 检查是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                // 如果不是团队成员，检查是否是行程创建者
        if (!itinerary.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权修改该行程");
                }
            }
        } else {
            // 个人行程 - 只有创建者可以修改
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权修改该行程");
            }
        }

        // 获取原始状态
        Itinerary.TravelStatus originalStatus = itinerary.getTravelStatus();
        
        try {
            Itinerary.TravelStatus newStatus = Itinerary.TravelStatus.valueOf(status);
            itinerary.setTravelStatus(newStatus);
            
            // 保存行程状态
            Itinerary savedItinerary = itineraryRepository.save(itinerary);
            
            // 处理目的地同步
            if (originalStatus != newStatus) {
                if (newStatus == Itinerary.TravelStatus.已出行 && originalStatus == Itinerary.TravelStatus.待出行) {
                    // 从待出行变为已出行：自动添加历史目的地
                    userService.addHistoryDestinationsFromCompletedItineraries(userId);
                } else if (newStatus == Itinerary.TravelStatus.待出行 && originalStatus == Itinerary.TravelStatus.已出行) {
                    // 从已出行变为待出行：删除该行程的自动添加的历史目的地
                    userService.removeAutoAddedHistoryDestinationsFromItinerary(userId, itineraryId);
                }
            }
            
            return itineraryConverter.toDTO(savedItinerary);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的状态值: " + status);
        }
    }

    @Override
    @Transactional
    public String uploadCoverImage(Long userId, Long itineraryId, MultipartFile file) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));

        // 检查用户权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 检查是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                // 如果不是团队成员，检查是否是行程创建者
        if (!itinerary.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权修改该行程");
                }
            }
        } else {
            // 个人行程 - 只有创建者可以修改
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权修改该行程");
            }
        }

        // 验证文件
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidImageFile(originalFilename)) {
            throw new RuntimeException("文件格式不支持，请上传 JPG、PNG 或 GIF 格式的图片");
        }

        try {
            // 创建上传目录
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String fileExtension = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID().toString() + "." + fileExtension;
            Path filePath = uploadPath.resolve(fileName);

            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 构建访问URL
            String imageUrl = "/covers/" + fileName;

            // 更新行程封面
            itinerary.setImageUrl(imageUrl);
            itineraryRepository.save(itinerary);

            return imageUrl;
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage());
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
    @Transactional
    public void updateDayTitle(Long userId, Long itineraryId, Long dayId, String newTitle) {
        // 验证行程权限
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));
        
        // 检查用户权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 检查是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                // 如果不是团队成员，检查是否是行程创建者
        if (!itinerary.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权修改该行程");
                }
            }

            // 检查团队行程状态，已出行的行程不能编辑
            if (itinerary.getTravelStatus() == Itinerary.TravelStatus.已出行) {
                throw new RuntimeException("已出行的行程不能编辑，只能查看");
            }
        } else {
            // 个人行程 - 只有创建者可以修改
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权修改该行程");
            }

            // 检查个人行程状态，已出行的行程不能编辑
            if (itinerary.getTravelStatus() == Itinerary.TravelStatus.已出行) {
                throw new RuntimeException("已出行的行程不能编辑，只能查看");
            }
        }
        
        // 验证日程存在且属于该行程
        ItineraryDay day = itineraryDayRepository.findById(dayId)
                .orElseThrow(() -> new RuntimeException("日程不存在"));
        
        if (!day.getItinerary().getId().equals(itineraryId)) {
            throw new RuntimeException("日程不属于该行程");
        }
        
        // 更新标题
        day.setTitle(newTitle);
        itineraryDayRepository.save(day);
    }

    @Override
    @Transactional
    public ItineraryDTO setEditComplete(Long userId, Long itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));
        
        // 检查用户权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 检查是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                // 如果不是团队成员，检查是否是行程创建者
        if (!itinerary.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权修改该行程");
                }
            }
        } else {
            // 个人行程 - 只有创建者可以修改
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权修改该行程");
            }
        }
        
        // 设置编辑状态为完成
        itinerary.setEditStatus(Itinerary.EditStatus.完成);
        Itinerary savedItinerary = itineraryRepository.save(itinerary);
        
        return itineraryConverter.toDTO(savedItinerary);
    }

    @Override
    @Transactional
    public ItineraryDTO shiftItineraryDates(Long userId, Long itineraryId, LocalDate newStartDate) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));

        // 检查用户权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 检查是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                // 如果不是团队成员，检查是否是行程创建者
        if (!itinerary.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权修改该行程");
                }
            }

            // 检查团队行程状态，已出行的行程不能编辑
            if (itinerary.getTravelStatus() == Itinerary.TravelStatus.已出行) {
                throw new RuntimeException("已出行的行程不能编辑，只能查看");
            }
        } else {
            // 个人行程 - 只有创建者可以修改
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权修改该行程");
            }

            // 检查个人行程状态，已出行的行程不能编辑
            if (itinerary.getTravelStatus() == Itinerary.TravelStatus.已出行) {
                throw new RuntimeException("已出行的行程不能编辑，只能查看");
            }
        }

        LocalDate oldStart = itinerary.getStartDate();
        LocalDate oldEnd = itinerary.getEndDate();

        if (newStartDate == null) {
            throw new RuntimeException("新的开始日期不能为空");
        }

        long durationDays = java.time.temporal.ChronoUnit.DAYS.between(oldStart, oldEnd);
        LocalDate newEndDate = newStartDate.plusDays(durationDays);

        itinerary.setStartDate(newStartDate);
        itinerary.setEndDate(newEndDate);

        // 更新各日程日期
        List<ItineraryDay> days = itineraryDayRepository.findByItineraryIdOrderByDayNumber(itineraryId);
        for (ItineraryDay day : days) {
            int index = day.getDayNumber() - 1; // dayNumber 从1开始
            LocalDate newDate = newStartDate.plusDays(index);
            day.setDate(newDate);
            itineraryDayRepository.save(day);
        }

        Itinerary saved = itineraryRepository.save(itinerary);
        return itineraryConverter.toDTO(saved);
    }

    @Override
    public PermissionStatusResponse updatePermissionStatus(Long itineraryId, Long userId, String permissionStatus) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));
        
        // 检查权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 检查是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                // 如果不是团队成员，检查是否是行程创建者
                if (!itinerary.getUser().getId().equals(userId)) {
                    throw new RuntimeException("无权修改此行程权限");
                }
            }
        } else {
            // 个人行程 - 只有创建者可以修改
        if (!itinerary.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限修改此行程");
            }
        }

        // 保存旧的权限状态
        Itinerary.PermissionStatus oldPermissionStatus = itinerary.getPermissionStatus();
        
        // 转换并验证权限状态
        Itinerary.PermissionStatus newPermissionStatus;
        try {
            newPermissionStatus = Itinerary.PermissionStatus.valueOf(permissionStatus);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的权限状态: " + permissionStatus);
        }
        
        // 检查是否从"私人"转为"所有人可见"
        boolean needsShareDialog = (oldPermissionStatus == Itinerary.PermissionStatus.私人 &&
                                   newPermissionStatus == Itinerary.PermissionStatus.所有人可见);

        if (needsShareDialog) {
            // 如果需要弹出分享窗口，先不更新权限状态
            logger.info("从私人转为所有人可见，需要弹出分享窗口");
            return PermissionStatusResponse.needsShare();
        }
        
        // 更新权限状态
        itinerary.setPermissionStatus(newPermissionStatus);
        
        // 处理权限状态变化
        handlePermissionStatusChange(itinerary, oldPermissionStatus);
        
        // 保存更改
        itineraryRepository.save(itinerary);

        return PermissionStatusResponse.success();
    }

    @Override
    public String generateShareCode(Long itineraryId, Long userId, ShareCodeRequest request) {
        logger.debug("生成分享码 - 用户ID: {}, 行程ID: {}, 请求: {}", userId, itineraryId, request);
        
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("行程不存在"));

        // 检查权限
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 检查是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                // 如果不是团队成员，检查是否是行程创建者
        if (!itinerary.getUser().getId().equals(userId)) {
                    throw new RuntimeException("无权分享此行程");
                }
            }
        } else {
            // 个人行程 - 只有创建者可以分享
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权分享此行程");
            }
        }

        // 保存旧的权限状态
        Itinerary.PermissionStatus oldPermissionStatus = itinerary.getPermissionStatus();

        // 检查行程权限状态
        // 只有当行程状态为"私人"时，才自动更新为"仅获得链接者可见"
        // 如果用户已经将状态设置为"所有人可见"，则保持该状态
        // 特殊情况：如果描述不为空且不是默认描述，说明是从"私人"直接分享为"所有人可见"
        if (itinerary.getPermissionStatus() == Itinerary.PermissionStatus.私人) {
            // 检查是否是公开分享（有描述内容）
            if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
                // 如果有描述，说明是公开分享，直接设置为"所有人可见"
                itinerary.setPermissionStatus(Itinerary.PermissionStatus.所有人可见);
                logger.info("行程 {} 权限状态从私人直接设置为所有人可见", itineraryId);
            } else {
                // 如果没有描述，设置为"仅获得链接者可见"
            itinerary.setPermissionStatus(Itinerary.PermissionStatus.仅获得链接者可见);
            logger.info("行程 {} 权限状态从私人自动更新为仅获得链接者可见", itineraryId);
        }
        }

        // 保存权限状态变更
        itinerary = itineraryRepository.save(itinerary);

        // 不在这里调用handlePermissionStatusChange，而是在createOrUpdateCommunityEntry中处理
        // 因为我们需要在创建社区条目时知道完整的上下文信息

        try {
            // 生成分享码
            String shareCode;
            do {
                shareCode = generateRandomShareCode();
            } while (communityEntryRepository.findByShareCode(shareCode).isPresent());

            // 创建或更新社区条目，同时处理权限状态变更的逻辑
            createOrUpdateCommunityEntry(itinerary, shareCode, request.getDescription(), request.getTagIds());

            logger.info("分享码生成成功 - 用户ID: {}, 行程ID: {}, 分享码: {}", userId, itineraryId, shareCode);
            return shareCode;
        } catch (Exception e) {
            logger.error("生成分享码失败 - 用户ID: {}, 行程ID: {}, 错误: {}", userId, itineraryId, e.getMessage());
            throw new RuntimeException("生成分享码失败: " + e.getMessage());
        }
    }

    private void createOrUpdateCommunityEntry(Itinerary itinerary, String shareCode, String description, List<Long> tagIds) {
        logger.info("开始创建或更新社区条目 - 行程ID: {}, 分享码: {}, 描述: {}", itinerary.getId(), shareCode, description);

        // 检查是否已存在社区条目
        CommunityEntry existingEntry = communityEntryRepository.findByItineraryId(itinerary.getId())
                .orElse(null);
        CommunityEntry communityEntry;

        if (existingEntry != null) {
            logger.info("找到现有社区条目 ID: {}, 进行更新", existingEntry.getId());
            // 更新现有条目
            existingEntry.setShareCode(shareCode);
            existingEntry.setDescription(description);
            communityEntry = communityEntryRepository.save(existingEntry);
            
            // 删除现有的标签关联
            communityEntryTagRepository.deleteByCommunityEntry(existingEntry);
        } else {
            logger.info("未找到现有社区条目，创建新条目");
            // 创建新条目
            communityEntry = new CommunityEntry();
            communityEntry.setItinerary(itinerary);
            communityEntry.setShareCode(shareCode);
            communityEntry.setDescription(description);
            communityEntry.setViewCount(0);
            communityEntry = communityEntryRepository.save(communityEntry);
            logger.info("新社区条目创建成功，ID: {}", communityEntry.getId());
        }

        // 添加新的标签关联
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new RuntimeException("标签不存在: " + tagId));
                
                CommunityEntryTag entryTag = new CommunityEntryTag();
                entryTag.setCommunityEntry(communityEntry);
                entryTag.setTag(tag);
                communityEntryTagRepository.save(entryTag);
            }
        }

        // 确保所有数据库操作完成后再进行Elasticsearch操作
        // 异步索引到 Elasticsearch（仅当权限为"所有人可见"时）
        final Long entryId = communityEntry.getId();
        final Itinerary.PermissionStatus currentStatus = itinerary.getPermissionStatus();
        
        logger.info("社区条目 {} 创建完成，当前权限状态: {}", entryId, currentStatus);
        
        if (currentStatus == Itinerary.PermissionStatus.所有人可见) {
            // 延迟一点时间确保数据库事务完全提交
            logger.info("权限为所有人可见，准备索引社区条目 {} 到 Elasticsearch", entryId);
            CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> {
                indexCommunityEntryToElasticsearch(entryId);
            });
        } else if (currentStatus == Itinerary.PermissionStatus.仅获得链接者可见) {
            // 如果权限是"仅获得链接者可见"，确保从Elasticsearch中删除
            logger.info("权限为仅获得链接者可见，准备从 Elasticsearch 删除社区条目 {}", entryId);
            deleteCommunityEntryFromElasticsearch(entryId);
        }
    }

    /**
     * 异步索引社区条目到 Elasticsearch
     * @param communityEntryId 社区条目ID
     */
    private void indexCommunityEntryToElasticsearch(Long communityEntryId) {
        // 使用异步线程池执行索引操作
        CompletableFuture.runAsync(() -> {
            ElasticsearchClient client = null;
            try {
                logger.info("开始索引社区条目 {} 到 Elasticsearch", communityEntryId);

                // 3. 创建Elasticsearch客户端
                client = createElasticsearchClient();

                // 检查Elasticsearch连接
                logger.info("检查Elasticsearch连接...");
                try {
                    boolean isConnected = client.ping().value();
                    logger.info("Elasticsearch连接状态: {}", isConnected ? "成功" : "失败");
                    if (!isConnected) {
                        logger.error("无法连接到Elasticsearch");
                        return;
                    }
                } catch (Exception e) {
                    logger.error("Elasticsearch连接检查失败: {}", e.getMessage());
                    return;
                }

                // 检查索引是否存在
                logger.info("检查索引 '{}' 是否存在...", communityEntriesIndex);
                try {
                    boolean indexExists = client.indices().exists(r -> r.index(communityEntriesIndex)).value();
                    logger.info("索引 '{}' 存在状态: {}", communityEntriesIndex, indexExists ? "存在" : "不存在");
                    if (!indexExists) {
                        logger.warn("索引 '{}' 不存在，索引操作可能失败", communityEntriesIndex);
                    }
                } catch (Exception e) {
                    logger.error("检查索引存在性失败: {}", e.getMessage());
                }

                // 1. 获取社区条目详细信息
                Map<String, Object> entryDetails = getCommunityEntryDetails(communityEntryId);
                if (entryDetails == null) {
                    logger.error("未找到社区条目 ID: {}", communityEntryId);
                    return;
                }

                // 2. 准备文本并生成向量
                String text = prepareTextForEmbedding(entryDetails);
                if (text.trim().isEmpty()) {
                    logger.error("社区条目 {} 的文本为空，无法生成向量", communityEntryId);
                    return;
                }

                logger.info("准备向量化文本: {}", text.length() > 100 ? text.substring(0, 100) + "..." : text);

                float[] vector = embeddingService.embed(text);
                if (vector.length == 0) {
                    logger.error("社区条目 {} 向量生成失败", communityEntryId);
                    return;
                }

                logger.info("成功生成向量，维度: {}", vector.length);

                // 输出向量的前10个元素用于调试
                if (vector.length > 0) {
                    StringBuilder vectorSample = new StringBuilder();
                    int sampleSize = Math.min(10, vector.length);
                    for (int i = 0; i < sampleSize; i++) {
                        if (i > 0) vectorSample.append(", ");
                        vectorSample.append(String.format("%.6f", vector[i]));
                    }
                    logger.info("向量样本（前{}个元素）: [{}]", sampleSize, vectorSample.toString());
                }

                // 4. 准备索引文档
                CommunityEntryDocument document = new CommunityEntryDocument();
                document.setId((Long) entryDetails.get("id"));
                document.setShareCode((String) entryDetails.get("share_code"));
                document.setDescription((String) entryDetails.get("description"));
                document.setViewCount((Integer) entryDetails.get("view_count"));

                // 处理日期字段
                Object createdAt = entryDetails.get("created_at");
                if (createdAt != null) {
                    document.setCreatedAt(createdAt.toString());
                }

                Object updatedAt = entryDetails.get("updated_at");
                if (updatedAt != null) {
                    document.setUpdatedAt(updatedAt.toString());
                }

                document.setItineraryTitle((String) entryDetails.get("itinerary_title"));

                Object startDate = entryDetails.get("start_date");
                if (startDate != null) {
                    document.setStartDate(startDate.toString());
                }

                Object endDate = entryDetails.get("end_date");
                if (endDate != null) {
                    document.setEndDate(endDate.toString());
                }

                document.setDestinations((String) entryDetails.get("destinations"));
                document.setAuthorUsername((String) entryDetails.get("author_username"));
                document.setAuthorId((Long) entryDetails.get("author_id"));
                document.setTags((String) entryDetails.get("tags"));

                // 将float[]转换为List<Float>
                List<Float> vectorList = new ArrayList<>();
                for (float f : vector) {
                    vectorList.add(f);
                }
                document.setVector(vectorList);

                logger.info("准备索引文档: {}", document.getItineraryTitle());

                // 5. 索引到Elasticsearch
                IndexRequest<CommunityEntryDocument> request = IndexRequest.of(i -> i
                        .index(communityEntriesIndex)
                        .id(String.valueOf(communityEntryId))
                        .document(document)
                );

                logger.info("准备发送索引请求到索引 '{}', 文档ID: {}", communityEntriesIndex, communityEntryId);

                IndexResponse response = client.index(request);

                logger.info("索引响应 - 结果: {}, 索引: {}, ID: {}, 版本: {}",
                        response.result(), response.index(), response.id(), response.version());

                if (response.result() == Result.Created ||
                    response.result() == Result.Updated) {
                    logger.info("社区条目 {} 成功索引到 Elasticsearch", communityEntryId);

                    // 验证文档是否真的存在
                    try {
                        Thread.sleep(1000); // 等待1秒确保索引完成
                        boolean docExists = client.exists(e -> e.index(communityEntriesIndex).id(String.valueOf(communityEntryId))).value();
                        logger.info("验证文档存在性: {}", docExists ? "文档存在" : "文档不存在");
                    } catch (Exception e) {
                        logger.warn("验证文档存在性失败: {}", e.getMessage());
                    }
                } else {
                    logger.error("社区条目 {} 索引失败，响应: {}", communityEntryId, response.result());
                }
                
            } catch (Exception e) {
                logger.error("索引社区条目 {} 到 Elasticsearch 时发生错误: {}", communityEntryId, e.getMessage(), e);
            } finally {
                // 关闭客户端
                if (client != null) {
                    try {
                        client._transport().close();
                    } catch (Exception e) {
                        logger.warn("关闭Elasticsearch客户端时发生错误: {}", e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 异步删除社区条目从 Elasticsearch
     * @param communityEntryId 社区条目ID
     */
    private void deleteCommunityEntryFromElasticsearch(Long communityEntryId) {
        // 使用异步线程池执行删除操作
        CompletableFuture.runAsync(() -> {
            ElasticsearchClient client = null;
            try {
                logger.info("开始从 Elasticsearch 删除社区条目 {}", communityEntryId);

                // 创建Elasticsearch客户端
                client = createElasticsearchClient();

                // 删除文档
                DeleteRequest request = DeleteRequest.of(d -> d
                        .index(communityEntriesIndex)
                        .id(String.valueOf(communityEntryId))
                );

                DeleteResponse response = client.delete(request);

                if (response.result() == Result.Deleted) {
                    logger.info("社区条目 {} 成功从 Elasticsearch 删除", communityEntryId);
                } else if (response.result() == Result.NotFound) {
                    logger.info("社区条目 {} 在 Elasticsearch 中不存在", communityEntryId);
                } else {
                    logger.error("社区条目 {} 从 Elasticsearch 删除失败，响应: {}", communityEntryId, response.result());
                }

            } catch (Exception e) {
                logger.error("删除社区条目 {} 从 Elasticsearch 时发生错误: {}", communityEntryId, e.getMessage(), e);
            } finally {
                // 关闭客户端
                if (client != null) {
                    try {
                        client._transport().close();
                    } catch (Exception e) {
                        logger.warn("关闭Elasticsearch客户端时发生错误: {}", e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 获取社区条目的详细信息用于索引
     * @param communityEntryId 社区条目ID
     * @return 社区条目详细信息的Map
     */
    private Map<String, Object> getCommunityEntryDetails(Long communityEntryId) {
        // 使用原生SQL查询获取完整的社区条目信息
        String sql = """
            SELECT 
                ce.id,
                ce.share_code,
                ce.description,
                ce.view_count,
                ce.created_at,
                ce.updated_at,
                i.title as itinerary_title,
                i.start_date,
                i.end_date,
                GROUP_CONCAT(DISTINCT d.name SEPARATOR ', ') as destinations,
                u.username as author_username,
                u.id as author_id,
                GROUP_CONCAT(DISTINCT t.tag SEPARATOR ', ') as tags
            FROM community_entries ce
            JOIN itineraries i ON ce.itinerary_id = i.id
            JOIN user u ON i.user_id = u.id
            LEFT JOIN itinerary_days iday ON i.id = iday.itinerary_id
            LEFT JOIN itinerary_activities ia ON iday.id = ia.itinerary_day_id
            LEFT JOIN attractions a ON ia.attraction_id = a.id
            LEFT JOIN destinations d ON a.destination_id = d.id
            LEFT JOIN community_entry_tags cet ON ce.id = cet.share_entry_id
            LEFT JOIN tags t ON cet.tag_id = t.id
            WHERE ce.id = ? AND i.permission_status = '所有人可见'
            GROUP BY ce.id, ce.share_code, ce.description, ce.view_count, ce.created_at, ce.updated_at,
                     i.title, i.start_date, i.end_date, u.username, u.id
            """;

        try {
            // 这里需要使用JdbcTemplate来执行原生SQL
            // 暂时返回基本信息，后续可以优化
            Optional<CommunityEntry> entry = communityEntryRepository.findById(communityEntryId);
            if (entry.isEmpty()) {
                return null;
            }

            CommunityEntry communityEntry = entry.get();
            Itinerary itinerary = communityEntry.getItinerary();

            // 检查权限状态
            logger.info("社区条目 {} 对应的行程权限状态: {}", communityEntryId, itinerary.getPermissionStatus());

            // 如果权限状态不是"所有人可见"，不应该索引
            if (itinerary.getPermissionStatus() != Itinerary.PermissionStatus.所有人可见) {
                logger.warn("社区条目 {} 对应的行程权限状态不是'所有人可见'，跳过索引", communityEntryId);
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", communityEntry.getId());
            result.put("share_code", communityEntry.getShareCode());
            result.put("description", communityEntry.getDescription() != null ? communityEntry.getDescription() : "");
            result.put("view_count", communityEntry.getViewCount() != null ? communityEntry.getViewCount() : 0);
            result.put("created_at", communityEntry.getCreatedAt());
            result.put("updated_at", communityEntry.getUpdatedAt());
            result.put("itinerary_title", itinerary.getTitle());
            result.put("start_date", itinerary.getStartDate());
            result.put("end_date", itinerary.getEndDate());
            result.put("author_username", itinerary.getUser().getUsername());
            result.put("author_id", itinerary.getUser().getId());

            // 获取标签信息
            List<CommunityEntryTag> entryTags = communityEntryTagRepository.findByCommunityEntry(communityEntry);
            String tags = entryTags.stream()
                    .map(tag -> tag.getTag().getTag())
                    .collect(Collectors.joining(", "));
            result.put("tags", tags);

            // 获取目的地信息 - 通过行程-日程-活动-景点-目的地链获取
            String destinations = getDestinationsForItinerary(itinerary.getId());
            result.put("destinations", destinations);

            return result;
            } catch (Exception e) {
            logger.error("获取社区条目详细信息失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 准备用于向量化的文本
     * @param entryDetails 社区条目详细信息
     * @return 组合后的文本
     */
    private String prepareTextForEmbedding(Map<String, Object> entryDetails) {
        List<String> textParts = new ArrayList<>();

        // 行程名称
        String title = (String) entryDetails.get("itinerary_title");
        if (title != null && !title.trim().isEmpty()) {
            textParts.add(title);
        }

        // 描述
        String description = (String) entryDetails.get("description");
        if (description != null && !description.trim().isEmpty()) {
            textParts.add(description);
        }

        // 作者名称
        String authorUsername = (String) entryDetails.get("author_username");
        if (authorUsername != null && !authorUsername.trim().isEmpty()) {
            textParts.add("作者: " + authorUsername);
        }

        // 目的地
        String destinations = (String) entryDetails.get("destinations");
        if (destinations != null && !destinations.trim().isEmpty()) {
            textParts.add("目的地: " + destinations);
        }

        // 标签
        String tags = (String) entryDetails.get("tags");
        if (tags != null && !tags.trim().isEmpty()) {
            textParts.add("标签: " + tags);
        }

        return String.join(" ", textParts);
            }

    /**
     * 获取行程的目的地信息
     * @param itineraryId 行程ID
     * @return 目的地名称列表，用逗号分隔
     */
    private String getDestinationsForItinerary(Long itineraryId) {
        try {
            // 通过行程-日程-活动-景点-目的地链获取目的地
            List<ItineraryDay> days = itineraryDayRepository.findByItineraryIdOrderByDayNumber(itineraryId);
            Set<String> destinationNames = new HashSet<>();

            for (ItineraryDay day : days) {
                // 获取每天的活动
                List<ItineraryActivity> activities = itineraryActivityRepository.findByItineraryDayId(day.getId());

                for (ItineraryActivity activity : activities) {
                    // 获取活动的景点，然后获取目的地
                    if (activity.getAttraction() != null &&
                        activity.getAttraction().getDestination() != null) {
                        destinationNames.add(activity.getAttraction().getDestination().getName());
                    }
                }
            }

            return String.join(", ", destinationNames);
        } catch (Exception e) {
            logger.error("获取行程目的地信息失败: {}", e.getMessage(), e);
            return "";
            }
        }

    /**
     * 创建Elasticsearch客户端
     * @return ElasticsearchClient
     */
    private ElasticsearchClient createElasticsearchClient() {
        RestClient restClient = RestClient.builder(
                new HttpHost(elasticsearchHost, elasticsearchPort, "http")
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }

    // 生成随机分享码
    private String generateRandomShareCode() {
        StringBuilder sb = new StringBuilder(SHARE_CODE_LENGTH);
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < SHARE_CODE_LENGTH; i++) {
            int randomIndex = random.nextInt(SHARE_CODE_CHARS.length());
            sb.append(SHARE_CODE_CHARS.charAt(randomIndex));
        }
        
        return sb.toString();
    }

    /**
     * 检查所有社区条目的索引状态（用于调试）
     */
    public void checkAllCommunityEntriesIndexStatus() {
        logger.info("=== 开始检查所有社区条目的索引状态 ===");

        List<CommunityEntry> allEntries = communityEntryRepository.findAll();
        logger.info("数据库中总共有 {} 个社区条目", allEntries.size());

        int publicCount = 0;
        int linkOnlyCount = 0;
        int privateCount = 0;

        for (CommunityEntry entry : allEntries) {
            Itinerary itinerary = entry.getItinerary();
            Itinerary.PermissionStatus status = itinerary.getPermissionStatus();

            logger.info("社区条目 {} (分享码: {}) - 行程: '{}' - 权限状态: {}",
                    entry.getId(), entry.getShareCode(), itinerary.getTitle(), status);

            switch (status) {
                case 所有人可见:
                    publicCount++;
                    break;
                case 仅获得链接者可见:
                    linkOnlyCount++;
                    break;
                case 私人:
                    privateCount++;
                    break;
            }
        }

        logger.info("权限状态统计: 所有人可见={}, 仅获得链接者可见={}, 私人={}",
                publicCount, linkOnlyCount, privateCount);
        logger.info("应该在Elasticsearch中索引的社区条目数量: {}", publicCount);
        logger.info("=== 社区条目索引状态检查完成 ===");
    }

    /**
     * 检查Elasticsearch中的文档数量
     */
    public void checkElasticsearchDocumentCount() {
        ElasticsearchClient client = null;
        try {
            logger.info("=== 开始检查Elasticsearch中的文档数量 ===");

            client = createElasticsearchClient();

            // 检查连接
            boolean isConnected = client.ping().value();
            logger.info("Elasticsearch连接状态: {}", isConnected ? "成功" : "失败");

            if (!isConnected) {
                logger.error("无法连接到Elasticsearch");
                return;
            }

            // 检查索引是否存在
            boolean indexExists = client.indices().exists(r -> r.index(communityEntriesIndex)).value();
            logger.info("索引 '{}' 存在状态: {}", communityEntriesIndex, indexExists ? "存在" : "不存在");

            if (!indexExists) {
                logger.warn("索引 '{}' 不存在", communityEntriesIndex);
                return;
            }

            // 查询文档数量
            var countResponse = client.count(c -> c.index(communityEntriesIndex));
            logger.info("Elasticsearch中索引 '{}' 的文档数量: {}", communityEntriesIndex, countResponse.count());

            // 获取索引的基本信息
            var statsResponse = client.indices().stats(s -> s.index(communityEntriesIndex));
            logger.info("索引统计信息: {}", statsResponse.toString());

        } catch (Exception e) {
            logger.error("检查Elasticsearch文档数量失败: {}", e.getMessage(), e);
        } finally {
            if (client != null) {
                try {
                    client._transport().close();
                } catch (Exception e) {
                    logger.warn("关闭Elasticsearch客户端失败: {}", e.getMessage());
                }
            }
        }

        logger.info("=== Elasticsearch文档数量检查完成 ===");
    }

    @Override
    @Transactional
    public ItineraryDTO createGroupItinerary(Long userId, Long groupId, ItineraryCreateRequest request, boolean isTemplate) {
        // 验证用户是否是团队成员
        TravelGroupMember member = travelGroupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new RuntimeException("您不是该团队的成员"));

        // 验证团队是否存在
        TravelGroup group = travelGroupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("团队不存在"));

        // 检查是否已有团队行程存在（一个团队只能有一个行程）
        List<GroupItinerary> existingItineraries = groupItineraryRepository.findByGroupId(groupId);
        if (!existingItineraries.isEmpty()) {
            throw new RuntimeException("该团队已存在行程，无法重复创建");
        }

        // 创建行程
        Itinerary itinerary = new Itinerary();
        itinerary.setUser(member.getUser());
        itinerary.setCreator(member.getUser()); // 设置创建者
        itinerary.setTitle(request.getTitle());
        itinerary.setImageUrl(request.getImageUrl());
        itinerary.setStartDate(request.getStartDate());
        itinerary.setEndDate(request.getEndDate());
        itinerary.setBudget(request.getBudget());
        itinerary.setTravelerCount(request.getTravelerCount());
        itinerary.setTravelStatus(Itinerary.TravelStatus.待出行);
        itinerary.setEditStatus(Itinerary.EditStatus.草稿);
        itinerary.setPermissionStatus(Itinerary.PermissionStatus.私人); // 团队行程默认私人
        itinerary.setGroupId(groupId); // 设置团队ID

        // 保存行程
        Itinerary savedItinerary = itineraryRepository.save(itinerary);

        // 创建行程天数
        createItineraryDays(savedItinerary);

        // 创建团队行程关联
        GroupItinerary groupItinerary = new GroupItinerary();
        groupItinerary.setGroup(group);
        groupItinerary.setItinerary(savedItinerary);
        groupItinerary.setTemplate(isTemplate);
        groupItinerary.setCreatedBy(member.getUser());
        groupItineraryRepository.save(groupItinerary);

        return itineraryConverter.toDTO(savedItinerary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItineraryDTO> getGroupItineraries(Long userId, Long groupId, Boolean templatesOnly) {
        // 验证用户是否是团队成员
        TravelGroupMember member = travelGroupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new RuntimeException("您不是该团队的成员"));

        // 获取团队行程列表
        List<GroupItinerary> groupItineraries;
        if (templatesOnly) {
            groupItineraries = groupItineraryRepository.findByGroupIdAndIsTemplate(groupId, true);
        } else {
            groupItineraries = groupItineraryRepository.findByGroupId(groupId);
        }

        // 转换为DTO
        return groupItineraries.stream()
            .map(gi -> itineraryConverter.toDTO(gi.getItinerary()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ItineraryDTO setAsGroupTemplate(Long userId, Long groupId, Long itineraryId) {
        // 验证用户是否是团队成员
        TravelGroupMember member = travelGroupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new RuntimeException("您不是该团队的成员"));

        // 验证行程是否属于该团队
        GroupItinerary groupItinerary = groupItineraryRepository.findByGroupIdAndItineraryId(groupId, itineraryId)
            .orElseThrow(() -> new RuntimeException("该行程不属于此团队"));

        // 设置为模板
        groupItinerary.setTemplate(true);
        groupItineraryRepository.save(groupItinerary);

        return itineraryConverter.toDTO(groupItinerary.getItinerary());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItineraryDTO> getPersonalItineraries(Long userId, Pageable pageable) {
        // 检查用户是否存在
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("用户不存在");
        }

        // 获取用户的个人行程列表
        Page<Itinerary> itinerariesPage = itineraryRepository.findPersonalItineraries(userId, pageable);

        // 转换为DTO
        return itinerariesPage.getContent().stream()
                .map(itineraryConverter::toDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItineraryDTO> getTeamItineraries(Long userId, Pageable pageable) {
        try {
            logger.info("开始获取团队行程 - 用户ID: {}", userId);

            // 检查用户是否存在
            if (!userRepository.existsById(userId)) {
                throw new RuntimeException("用户不存在");
            }

            // 获取用户参与的所有团队成员记录
            List<TravelGroupMember> memberships = travelGroupMemberRepository.findByUserId(userId);
            logger.info("用户 {} 参与的团队成员记录数: {}", userId, memberships.size());

            // 获取所有团队ID（只包括已加入的团队）
            List<Long> groupIds = memberships.stream()
                    .filter(member -> member.getJoinStatus() == TravelGroupMember.JoinStatus.已加入)
                    .map(member -> member.getGroup().getId())
                    .collect(java.util.stream.Collectors.toList());

            logger.info("用户 {} 已加入的团队ID列表: {}", userId, groupIds);

            if (groupIds.isEmpty()) {
                logger.info("用户 {} 没有加入任何团队", userId);
                return new java.util.ArrayList<>();
            }

            // 查询这些团队的所有行程
            List<Itinerary> teamItineraries = new java.util.ArrayList<>();
            for (Long groupId : groupIds) {
                try {
                    logger.debug("查询团队 {} 的行程", groupId);
                    // 查询该团队的行程
                    List<GroupItinerary> groupItineraries = groupItineraryRepository.findByGroupId(groupId);
                    logger.debug("团队 {} 的行程数量: {}", groupId, groupItineraries.size());

                    for (GroupItinerary groupItinerary : groupItineraries) {
                        Itinerary itinerary = groupItinerary.getItinerary();
                        if (itinerary != null) {
                            teamItineraries.add(itinerary);
                            logger.debug("添加行程: ID={}, 标题={}", itinerary.getId(), itinerary.getTitle());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("获取团队 {} 的行程时出错: {}", groupId, e.getMessage());
                    // 继续处理其他团队
                }
            }

            logger.info("总共找到 {} 个团队行程", teamItineraries.size());

            // 按创建时间倒序排序
            teamItineraries.sort((a, b) -> {
                LocalDateTime aTime = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                LocalDateTime bTime = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                return bTime.compareTo(aTime);
            });

            // 应用分页逻辑
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), teamItineraries.size());

            if (start >= teamItineraries.size()) {
                return new java.util.ArrayList<>();
            }

            List<Itinerary> pagedItineraries = teamItineraries.subList(start, end);

            // 转换为DTO并添加团队信息
            return pagedItineraries.stream()
                .map(itinerary -> {
                    ItineraryDTO dto = itineraryConverter.toDTO(itinerary);

                    // 获取团队信息
                    if (itinerary.getGroupId() != null) {
                        try {
                            TravelGroup group = travelGroupRepository.findById(itinerary.getGroupId()).orElse(null);
                            if (group != null) {
                                dto.setGroupTitle(group.getTitle());

                                // 检查当前用户是否为团队创建者
                                boolean isCreator = group.getCreator().getId().equals(userId);
                                dto.setIsGroupCreator(isCreator);

                                // 获取用户在团队中的角色
                                TravelGroupMember member = travelGroupMemberRepository
                                    .findByGroupIdAndUserId(itinerary.getGroupId(), userId)
                                    .orElse(null);
                                if (member != null) {
                                    dto.setUserRole(member.getRole().toString());
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("获取团队信息失败: {}", e.getMessage());
                        }
                    }

                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            logger.error("获取团队行程失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            // 返回空列表而不是抛出异常，避免前端错误
            return new java.util.ArrayList<>();
        }
    }

    @Override
    @Transactional
    public ItineraryDTO importAIItinerary(Long userId, com.se_07.backend.dto.AIItineraryImportRequest importRequest) {
        logger.info("开始导入AI生成的行程 - 用户ID: {}, 行程标题: {}", userId, importRequest.getTitle());

        // 查找用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证请求数据
        if (importRequest.getTitle() == null || importRequest.getTitle().trim().isEmpty()) {
            throw new RuntimeException("行程标题不能为空");
        }

        if (importRequest.getDays() == null || importRequest.getDays() <= 0) {
            throw new RuntimeException("行程天数必须大于0");
        }

        if (importRequest.getPlan() == null || importRequest.getPlan().isEmpty()) {
            throw new RuntimeException("行程计划不能为空");
        }

        // 验证出行人数
        Integer travelerCount = importRequest.getTravelers();
        if (travelerCount == null || travelerCount <= 0) {
            logger.warn("出行人数无效: {}, 使用默认值2", travelerCount);
            travelerCount = 2;
        }

        // 创建行程实体
        Itinerary itinerary = new Itinerary();
        itinerary.setUser(user);
        itinerary.setTitle(importRequest.getTitle());
        itinerary.setBudget(importRequest.getBudget());
        itinerary.setTravelerCount(travelerCount);
        itinerary.setTravelStatus(Itinerary.TravelStatus.待出行);
        itinerary.setEditStatus(Itinerary.EditStatus.草稿);
        itinerary.setPermissionStatus(Itinerary.PermissionStatus.私人);

        // 计算开始和结束日期 - 默认从今天开始
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(importRequest.getDays() - 1);
        itinerary.setStartDate(startDate);
        itinerary.setEndDate(endDate);

        // 保存行程
        Itinerary savedItinerary = itineraryRepository.save(itinerary);
        logger.info("行程创建成功 - ID: {}", savedItinerary.getId());

        // 创建日程和活动
        createItineraryDaysWithActivities(savedItinerary, importRequest.getPlan());

        // 重新获取完整的行程数据
        Itinerary completeItinerary = itineraryRepository.findById(savedItinerary.getId())
                .orElseThrow(() -> new RuntimeException("行程创建失败"));

        logger.info("AI行程导入完成 - 行程ID: {}", completeItinerary.getId());

        return itineraryConverter.toDTO(completeItinerary);
    }

    /**
     * 为导入的行程创建日程和活动
     */
    private void createItineraryDaysWithActivities(Itinerary itinerary, List<com.se_07.backend.dto.AIItineraryImportRequest.DayPlan> planList) {
        LocalDate startDate = itinerary.getStartDate();

        for (com.se_07.backend.dto.AIItineraryImportRequest.DayPlan dayPlan : planList) {
            // 创建日程
            ItineraryDay day = new ItineraryDay();
            day.setItinerary(itinerary);
            day.setDayNumber(dayPlan.getDay());
            day.setDate(startDate.plusDays(dayPlan.getDay() - 1));
            day.setTitle("第" + dayPlan.getDay() + "天");
            day.setFirstActivityId(null);
            day.setLastActivityId(null);

            // 保存日程
            ItineraryDay savedDay = itineraryDayRepository.save(day);
            logger.info("创建日程 - 日程ID: {}, 第{}天", savedDay.getId(), dayPlan.getDay());

            // 创建活动
            List<ItineraryActivity> dayActivities = new ArrayList<>();
            ItineraryActivity prevActivity = null;

            for (com.se_07.backend.dto.AIItineraryImportRequest.ActivityPlan activityPlan : dayPlan.getActivities()) {
                // 搜索景点
                List<com.se_07.backend.entity.Attraction> attractions = attractionRepository.findByNameContainingOrDescriptionContaining(activityPlan.getName());

                if (attractions.isEmpty()) {
                    logger.warn("未找到景点: {}, 跳过该活动", activityPlan.getName());
                    continue;
                }

                // 使用第一个搜索结果
                com.se_07.backend.entity.Attraction attraction = attractions.get(0);
                logger.info("找到景点: {} -> {}", activityPlan.getName(), attraction.getName());

                // 创建活动
                ItineraryActivity activity = new ItineraryActivity();
                activity.setItineraryDay(savedDay);
                activity.setTitle(activityPlan.getName());
                activity.setAttraction(attraction);
                activity.setTransportMode(activityPlan.getTransportMode() != null ? activityPlan.getTransportMode() : "步行");
                activity.setAttractionNotes(activityPlan.getDescription());

                // 解析时间
                try {
                    if (activityPlan.getStartTime() != null) {
                        activity.setStartTime(java.time.LocalTime.parse(activityPlan.getStartTime()));
                    }
                    if (activityPlan.getEndTime() != null) {
                        activity.setEndTime(java.time.LocalTime.parse(activityPlan.getEndTime()));
                    }
                } catch (Exception e) {
                    logger.warn("解析时间失败: {} - {}, 跳过时间设置", activityPlan.getStartTime(), activityPlan.getEndTime());
                }

                // 设置链表关系
                if (prevActivity != null) {
                    prevActivity.setNextId(null); // 先设置为null，保存后再更新
                    activity.setPrevId(null);
                }

                // 保存活动
                ItineraryActivity savedActivity = itineraryActivityRepository.save(activity);
                logger.info("创建活动 - 活动ID: {}, 景点: {}", savedActivity.getId(), attraction.getName());

                // 更新链表关系
                if (prevActivity != null) {
                    prevActivity.setNextId(savedActivity.getId());
                    itineraryActivityRepository.save(prevActivity);

                    savedActivity.setPrevId(prevActivity.getId());
                    itineraryActivityRepository.save(savedActivity);
                }

                dayActivities.add(savedActivity);
                prevActivity = savedActivity;
            }

            // 更新日程的首末活动ID
            if (!dayActivities.isEmpty()) {
                savedDay.setFirstActivityId(dayActivities.get(0).getId());
                savedDay.setLastActivityId(dayActivities.get(dayActivities.size() - 1).getId());
                itineraryDayRepository.save(savedDay);
                logger.info("更新日程首末活动 - 日程ID: {}, 首活动ID: {}, 末活动ID: {}",
                    savedDay.getId(), savedDay.getFirstActivityId(), savedDay.getLastActivityId());
            }
        }
    }
}
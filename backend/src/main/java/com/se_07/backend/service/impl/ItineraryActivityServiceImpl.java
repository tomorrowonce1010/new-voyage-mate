package com.se_07.backend.service.impl;

import com.se_07.backend.dto.ActivityCreateRequest;
import com.se_07.backend.dto.AmapActivityCreateRequest;
import com.se_07.backend.dto.ItineraryActivityDTO;
import com.se_07.backend.dto.converter.ItineraryActivityConverter;
import com.se_07.backend.entity.*;
import com.se_07.backend.repository.*;
import com.se_07.backend.service.ItineraryActivityService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ItineraryActivityServiceImpl implements ItineraryActivityService {

    @Autowired
    private ItineraryActivityRepository activityRepository;
    @Autowired
    private ItineraryDayRepository dayRepository;
    @Autowired
    private AttractionRepository attractionRepository;
    @Autowired
    private DestinationRepository destinationRepository;
    @Autowired
    private ItineraryRepository itineraryRepository;
    @Autowired
    private ItineraryActivityConverter activityConverter;
    @Autowired
    private TravelGroupMemberRepository travelGroupMemberRepository;

    /**
     * 检查用户是否可以编辑指定行程
     * @param itinerary 行程对象
     * @param userId 用户ID
     * @throws RuntimeException 如果用户无权编辑
     */
    private void checkEditPermission(Itinerary itinerary, Long userId) {
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 验证是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                throw new RuntimeException("您不是该团队的成员,无权编辑此行程");
            }
            
            // 检查团队行程状态，已出行的行程不能编辑
            if (itinerary.getTravelStatus() == Itinerary.TravelStatus.已出行) {
                throw new RuntimeException("已出行的行程不能编辑，只能查看");
            }
        } else {
            // 个人行程 - 验证是否是创建者
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权编辑此行程");
            }
            
            // 检查个人行程状态，已出行的行程不能编辑
            if (itinerary.getTravelStatus() == Itinerary.TravelStatus.已出行) {
                throw new RuntimeException("已出行的行程不能编辑，只能查看");
            }
        }
    }

    @Override
    @Transactional
    public ItineraryActivityDTO createActivity(Long userId, ActivityCreateRequest req) {
        System.out.println("-----------start create activity------------");
        if (req.getItineraryDayId() == null || req.getAttractionId() == null) {
            System.out.println("itineraryDayId 和 attractionId 为必填");
            throw new RuntimeException("itineraryDayId 和 attractionId 为必填");
        }

        // 获取日程并校验所有权
        ItineraryDay day = dayRepository.findById(req.getItineraryDayId())
                .orElseThrow(() -> new RuntimeException("日程不存在"));
        Itinerary itinerary = day.getItinerary();
        if (itinerary == null) {
            throw new RuntimeException("日程数据异常，缺少行程");
        }

        // 检查编辑权限
        checkEditPermission(itinerary, userId);

        // 获取景点
        Attraction attraction = attractionRepository.findById(req.getAttractionId())
                .orElseThrow(() -> new RuntimeException("景点不存在"));

        // 创建实体
        ItineraryActivity activity = new ItineraryActivity();
        activity.setItineraryDay(day);
        activity.setAttraction(attraction);
        activity.setTitle(req.getTitle() != null ? req.getTitle() : attraction.getName());
        activity.setTransportMode(req.getTransportMode() != null ? req.getTransportMode() : "步行");
        activity.setStartTime(req.getStartTime());
        activity.setEndTime(req.getEndTime());
        //activity.setTransportNotes(req.getTransportNotes());
        activity.setAttractionNotes(req.getAttractionNotes());

        // 根据传参决定插入方式
        Long effectivePrevId = req.getPrevId();

        // 如果指定了 nextId，则计算 prevId
        if (req.getNextId() != null) {
            if (req.getPrevId() != null) {
                throw new RuntimeException("prevId 和 nextId 只能传一个");
            }
            Long nextId = req.getNextId();
            ItineraryActivity nextActivity = activityRepository.findById(nextId)
                    .orElseThrow(() -> new RuntimeException("nextId 不存在"));
            if (!nextActivity.getItineraryDay().getId().equals(day.getId())) {
                throw new RuntimeException("nextId 不属于同一天程");
            }
            effectivePrevId = nextActivity.getPrevId(); // 可能为 null
        }

        // 先保存活动以获得ID
        ItineraryActivity saved = activityRepository.save(activity);
        
        // 然后处理链表逻辑
        insertActivityIntoLinkedList(day, saved, req.getNextId(), effectivePrevId);
        
        return activityConverter.toDTO(saved);
    }

    /**
     * 从链表中移除活动并维护链表结构
     */
    private void removeActivityFromLinkedList(ItineraryDay day, ItineraryActivity activity) {
        Long activityId = activity.getId();
        Long prevId = activity.getPrevId();
        Long nextId = activity.getNextId();
        
        // 更新前一个活动的nextId
        if (prevId != null) {
            ItineraryActivity prevActivity = activityRepository.findById(prevId)
                    .orElseThrow(() -> new RuntimeException("前一个活动不存在"));
            prevActivity.setNextId(nextId);
            activityRepository.save(prevActivity);
        } else {
            // 如果被删除的活动是第一个活动，更新日程的firstActivityId
            day.setFirstActivityId(nextId);
            dayRepository.save(day);
        }
        
        // 更新后一个活动的prevId
        if (nextId != null) {
            ItineraryActivity nextActivity = activityRepository.findById(nextId)
                    .orElseThrow(() -> new RuntimeException("后一个活动不存在"));
            nextActivity.setPrevId(prevId);
            activityRepository.save(nextActivity);
        } else {
            // 如果被删除的活动是最后一个活动，更新日程的lastActivityId
            day.setLastActivityId(prevId);
            dayRepository.save(day);
        }
    }

    /**
     * 将新活动插入到链表中的正确位置
     */
    private void insertActivityIntoLinkedList(ItineraryDay day, ItineraryActivity newActivity, Long nextId, Long prevId) {
        Long newId = newActivity.getId();
        
        if (day.getFirstActivityId() == null) {
            // 情况1：这是第一个活动  
            newActivity.setPrevId(null);
            newActivity.setNextId(null);
            activityRepository.save(newActivity);
            
            day.setFirstActivityId(newId);
            day.setLastActivityId(newId);
            dayRepository.save(day);
            
        } else if (nextId != null) {
            // 情况2：插入到指定活动之前
            ItineraryActivity nextActivity = activityRepository.findById(nextId)
                    .orElseThrow(() -> new RuntimeException("指定的后续活动不存在"));
            
            Long nextPrevId = nextActivity.getPrevId();
            
            newActivity.setPrevId(nextPrevId);
            newActivity.setNextId(nextId);
            activityRepository.save(newActivity);
            
            nextActivity.setPrevId(newId);
            activityRepository.save(nextActivity);
            
            if (nextPrevId != null) {
                activityRepository.findById(nextPrevId).ifPresent(prev -> {
                    prev.setNextId(newId);
                    activityRepository.save(prev);
                });
            } else {
                // 插入到头部
                day.setFirstActivityId(newId);
                dayRepository.save(day);
            }
            
        } else if (prevId != null) {
            // 情况3：插入到指定活动之后
            ItineraryActivity prevActivity = activityRepository.findById(prevId)
                    .orElseThrow(() -> new RuntimeException("指定的前序活动不存在"));
            
            Long prevNextId = prevActivity.getNextId();
            
            newActivity.setPrevId(prevId);
            newActivity.setNextId(prevNextId);
            activityRepository.save(newActivity);
            
            prevActivity.setNextId(newId);
            activityRepository.save(prevActivity);
            
            if (prevNextId != null) {
                activityRepository.findById(prevNextId).ifPresent(next -> {
                    next.setPrevId(newId);
                    activityRepository.save(next);
                });
            } else {
                // 插入到尾部
                day.setLastActivityId(newId);
                dayRepository.save(day);
            }
            
        } else {
            // 情况4：追加到末尾
            Long lastId = day.getLastActivityId();
            
            newActivity.setPrevId(lastId);
            newActivity.setNextId(null);
            activityRepository.save(newActivity);
            
            if (lastId != null) {
                activityRepository.findById(lastId).ifPresent(last -> {
                    last.setNextId(newId);
                    activityRepository.save(last);
                });
            }
            
            day.setLastActivityId(newId);
            dayRepository.save(day);
        }
    }

    @Override
    @Transactional
    public java.util.List<ItineraryActivityDTO> getActivitiesByDay(Long userId, Long itineraryDayId) {
        ItineraryDay day = dayRepository.findById(itineraryDayId)
                .orElseThrow(() -> new RuntimeException("日程不存在"));

        // 鉴权：检查权限
        Itinerary itinerary = day.getItinerary();
        if (itinerary == null) {
            throw new RuntimeException("日程数据异常，缺少行程");
        }

        // 如果是团队行程,验证是否是团队成员
        if (itinerary.getGroupId() != null) {
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember && itinerary.getPermissionStatus() == Itinerary.PermissionStatus.私人) {
                throw new RuntimeException("您不是该团队的成员,无权查看此行程");
            }
        } else {
            // 个人行程 - 只能查看自己的或公开的
            if (!itinerary.getUser().getId().equals(userId) && itinerary.getPermissionStatus() == Itinerary.PermissionStatus.私人) {
                throw new RuntimeException("无权查看该日程");
            }
        }

        // 获取所有活动并按链表顺序排序
        java.util.List<ItineraryActivity> allActivities = activityRepository.findByItineraryDayId(itineraryDayId);
        
        // 如果没有活动，直接返回空列表
        if (allActivities.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // 创建活动映射
        java.util.Map<Long, ItineraryActivity> activityMap = new java.util.HashMap<>();
        for (ItineraryActivity activity : allActivities) {
            activityMap.put(activity.getId(), activity);
        }
        
        // 按链表顺序构建有序列表
        java.util.List<ItineraryActivity> orderedActivities = new java.util.ArrayList<>();
        Long currentId = day.getFirstActivityId();
        
        while (currentId != null && activityMap.containsKey(currentId)) {
            ItineraryActivity currentActivity = activityMap.get(currentId);
            orderedActivities.add(currentActivity);
            currentId = currentActivity.getNextId();
        }

        return orderedActivities.stream().map(activityConverter::toDTO).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public ItineraryActivityDTO updateTransportMode(Long userId, Long activityId, String transportMode) {
        // 查找活动
        ItineraryActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        
        // 鉴权：只能修改自己的活动
        Itinerary itinerary = activity.getItineraryDay().getItinerary();
        if (itinerary == null) {
            throw new RuntimeException("日程数据异常，缺少行程");
        }
        if (itinerary.getGroupId() != null) {
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember && itinerary.getPermissionStatus() == Itinerary.PermissionStatus.私人) {
                throw new RuntimeException("您不是该团队的成员,无权修改此活动");
            }
        } else {
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权修改此活动");
            }
        }
        
        // 更新交通方式
        activity.setTransportMode(transportMode);
        ItineraryActivity savedActivity = activityRepository.save(activity);
        
        return activityConverter.toDTO(savedActivity);
    }

    @Override
    @Transactional
    public ItineraryActivityDTO updateActivityAttraction(Long userId, Long activityId, Long attractionId) {
        // 查找活动
        ItineraryActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        
        // 鉴权：只能修改自己的活动
        Itinerary itinerary = activity.getItineraryDay().getItinerary();
        if (itinerary == null) {
            throw new RuntimeException("日程数据异常，缺少行程");
        }
        if (itinerary.getGroupId() != null) {
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember && itinerary.getPermissionStatus() == Itinerary.PermissionStatus.私人) {
                throw new RuntimeException("您不是该团队的成员,无权修改此活动");
            }
        } else {
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权修改此活动");
            }
        }
        
        // 查找新景点
        Attraction newAttraction = attractionRepository.findById(attractionId)
                .orElseThrow(() -> new RuntimeException("景点不存在"));
        
        // 更新活动的景点和标题
        activity.setAttraction(newAttraction);
        ItineraryActivity savedActivity = activityRepository.save(activity);
        
        return activityConverter.toDTO(savedActivity);
    }

    @Override
    @Transactional
    public ItineraryActivityDTO updateActivityNotes(Long userId, Long activityId, String attractionNotes) {
        // 查找活动
        ItineraryActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        
        // 鉴权：只能修改自己的活动
        Itinerary itinerary = activity.getItineraryDay().getItinerary();
        if (itinerary == null) {
            throw new RuntimeException("日程数据异常，缺少行程");
        }
        if (itinerary.getGroupId() != null) {
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember && itinerary.getPermissionStatus() == Itinerary.PermissionStatus.私人) {
                throw new RuntimeException("您不是该团队的成员,无权修改此活动");
            }
        } else {
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权修改此活动");
            }
        }
        
        // 更新备注
        activity.setAttractionNotes(attractionNotes);
        ItineraryActivity savedActivity = activityRepository.save(activity);
        
        return activityConverter.toDTO(savedActivity);
    }

    @Override
    @Transactional
    public ItineraryActivityDTO updateActivityTime(Long userId, Long activityId, String startTime, String endTime) {
        // 查找活动
        ItineraryActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        
        // 鉴权：只能修改自己的活动
        Itinerary itinerary = activity.getItineraryDay().getItinerary();
        if (itinerary == null) {
            throw new RuntimeException("日程数据异常，缺少行程");
        }
        if (itinerary.getGroupId() != null) {
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember && itinerary.getPermissionStatus() == Itinerary.PermissionStatus.私人) {
                throw new RuntimeException("您不是该团队的成员,无权修改此活动");
            }
        } else {
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权修改此活动");
            }
        }
        
        // 更新时间
        if (startTime != null && !startTime.trim().isEmpty()) {
            activity.setStartTime(java.time.LocalTime.parse(startTime));
        } else {
            activity.setStartTime(null);
        }
        
        if (endTime != null && !endTime.trim().isEmpty()) {
            activity.setEndTime(java.time.LocalTime.parse(endTime));
        } else {
            activity.setEndTime(null);
        }
        
        ItineraryActivity savedActivity = activityRepository.save(activity);
        
        return activityConverter.toDTO(savedActivity);
    }

    @Override
    @Transactional
    public void deleteActivity(Long userId, Long activityId) {
        // 查找活动
        ItineraryActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        
        // 鉴权：只能删除自己的活动
        Itinerary itinerary = activity.getItineraryDay().getItinerary();
        if (itinerary == null) {
            throw new RuntimeException("日程数据异常，缺少行程");
        }
        if (itinerary.getGroupId() != null) {
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember && itinerary.getPermissionStatus() == Itinerary.PermissionStatus.私人) {
                throw new RuntimeException("您不是该团队的成员,无权删除此活动");
            }
        } else {
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权删除此活动");
            }
        }
        
        // 获取日程信息
        ItineraryDay day = activity.getItineraryDay();
        
        // 维护链表结构
        removeActivityFromLinkedList(day, activity);
        
        // 删除活动
        activityRepository.delete(activity);
    }

    @Override
    @Transactional
    public ItineraryActivityDTO updateActivityTitle(Long userId, Long activityId, String title) {
        ItineraryActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        Itinerary itinerary = activity.getItineraryDay().getItinerary();
        if (itinerary == null) {
            throw new RuntimeException("日程数据异常，缺少行程");
        }
        if (itinerary.getGroupId() != null) {
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember && itinerary.getPermissionStatus() == Itinerary.PermissionStatus.私人) {
                throw new RuntimeException("您不是该团队的成员,无权修改此活动");
            }
        } else {
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权修改此活动");
            }
        }
        activity.setTitle(title);
        ItineraryActivity saved = activityRepository.save(activity);
        return activityConverter.toDTO(saved);
    }

    @Override
    @Transactional
    public ItineraryActivityDTO createActivityFromAmap(Long userId, AmapActivityCreateRequest req) {
        System.out.println("-----------start create activity from amap------------");
        if (req.getItineraryDayId() == null) {
            throw new RuntimeException("itineraryDayId 为必填");
        }

        // 获取日程并校验所有权
        ItineraryDay day = dayRepository.findById(req.getItineraryDayId())
                .orElseThrow(() -> new RuntimeException("日程不存在"));
        Itinerary itinerary = day.getItinerary();
        if (itinerary == null) {
            throw new RuntimeException("日程数据异常，缺少行程");
        }

        // 检查权限: 如果是个人行程,必须是创建者; 如果是团队行程,必须是团队成员
        if (itinerary.getGroupId() != null) {
            // 团队行程 - 验证是否是团队成员
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                throw new RuntimeException("您不是该团队的成员,无权编辑此行程");
            }
        } else {
            // 个人行程 - 验证是否是创建者
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权向此行程添加活动");
            }
        }

        // 处理景点
        Attraction attraction;
        if (req.getAttractionId() != null) {
            // 如果提供了景点ID，直接使用
            attraction = attractionRepository.findById(req.getAttractionId())
                    .orElseThrow(() -> new RuntimeException("景点不存在"));
        } else {
            // 根据高德地图信息创建或查找景点
            attraction = findOrCreateAttractionFromAmap(req.getAttractionInfo());
        }

        // 创建实体
        ItineraryActivity activity = new ItineraryActivity();
        activity.setItineraryDay(day);
        activity.setAttraction(attraction);
        activity.setTitle(req.getTitle() != null ? req.getTitle() : attraction.getName());
        activity.setTransportMode(req.getTransportMode() != null ? req.getTransportMode() : "步行");
        activity.setStartTime(req.getStartTime());
        activity.setEndTime(req.getEndTime());

        // 根据传参决定插入方式
        Long effectivePrevId = null;

        // 如果指定了 nextId，则计算 prevId
        if (req.getNextId() != null) {
            Long nextId = req.getNextId();
            ItineraryActivity nextActivity = activityRepository.findById(nextId)
                    .orElseThrow(() -> new RuntimeException("nextId 不存在"));
            if (!nextActivity.getItineraryDay().getId().equals(day.getId())) {
                throw new RuntimeException("nextId 不属于同一天程");
            }
            effectivePrevId = nextActivity.getPrevId(); // 可能为 null
        }

        // 先保存活动以获得ID
        ItineraryActivity saved = activityRepository.save(activity);
        
        // 然后处理链表逻辑
        insertActivityIntoLinkedList(day, saved, req.getNextId(), effectivePrevId);
        
        return activityConverter.toDTO(saved);
    }

    /**
     * 根据高德地图信息查找或创建景点
     */
    private Attraction findOrCreateAttractionFromAmap(AmapActivityCreateRequest.AmapAttractionInfo amapInfo) {
        if (amapInfo == null) {
            throw new RuntimeException("景点信息不能为空");
        }

        // 1. 首先尝试根据高德地图POI ID查找景点
        if (amapInfo.getId() != null && !amapInfo.getId().trim().isEmpty()) {
            java.util.Optional<Attraction> existingAttraction = attractionRepository.findByAmapPoiId(amapInfo.getId());
            if (existingAttraction.isPresent()) {
                return existingAttraction.get();
            }
        }
        
        // 2. 查找或创建目的地（城市）- 优先使用cityname字段
        String cityName = amapInfo.getCityname();
        if (cityName == null || cityName.trim().isEmpty()) {
            cityName = amapInfo.getCity(); // 如果cityname为空，使用city字段作为fallback
        }
        Destination destination = findOrCreateDestination(cityName);
        
        // 3. 查找或创建景点
        Attraction attraction = findOrCreateAttraction(amapInfo, destination);
        
        return attraction;
    }

    /**
     * 查找或创建目的地
     */
    private Destination findOrCreateDestination(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            throw new RuntimeException("城市名称不能为空");
        }

        // 尝试根据城市名称查找目的地
        java.util.List<Destination> destinations = destinationRepository.findByNameContainingIgnoreCase(cityName);
        
        // 如果找到匹配的目的地，返回第一个
        if (!destinations.isEmpty()) {
            return destinations.get(0);
        }

        // 如果没有找到创建新的目的地
        Destination newDestination = new Destination();
        newDestination.setName(cityName);
        newDestination.setDescription("通过高德地图API自动创建的目的地");
        
        return destinationRepository.save(newDestination);
    }

    /**
     * 查找或创建景点
     */
    private Attraction findOrCreateAttraction(AmapActivityCreateRequest.AmapAttractionInfo amapInfo, Destination destination) {
        // 尝试根据名称和目的地查找景点
        java.util.List<Attraction> attractions = attractionRepository.findByDestinationIdOrderByJoinCountDesc(destination.getId());
        
        for (Attraction attraction : attractions) {
            if (attraction.getName().equals(amapInfo.getName())) {
                return attraction;
            }
        }

        // 如果没有找到，创建新的景点
        Attraction newAttraction = new Attraction();
        newAttraction.setDestination(destination);
        newAttraction.setName(amapInfo.getName());
        newAttraction.setDescription(amapInfo.getDescription());
        newAttraction.setAmapPoiId(amapInfo.getId());
        
        // 设置坐标
        if (amapInfo.getLatitude() != null && amapInfo.getLongitude() != null) {
            newAttraction.setLatitude(java.math.BigDecimal.valueOf(amapInfo.getLatitude()));
            newAttraction.setLongitude(java.math.BigDecimal.valueOf(amapInfo.getLongitude()));
        }
        
        // 根据类型设置分类
        if (amapInfo.getType() != null) {
            if (amapInfo.getType().contains("风景名胜") || amapInfo.getType().contains("公园")) {
                newAttraction.setCategory(Attraction.AttractionCategory.旅游景点);
            } else if (amapInfo.getType().contains("餐饮")) {
                newAttraction.setCategory(Attraction.AttractionCategory.餐饮);
            } else if (amapInfo.getType().contains("住宿")) {
                newAttraction.setCategory(Attraction.AttractionCategory.住宿);
            } else if (amapInfo.getType().contains("交通")) {
                newAttraction.setCategory(Attraction.AttractionCategory.交通站点);
            } else {
                newAttraction.setCategory(Attraction.AttractionCategory.旅游景点);
            }
        } else {
            newAttraction.setCategory(Attraction.AttractionCategory.旅游景点);
        }
        
        return attractionRepository.save(newAttraction);
    }

    @Override
    @Transactional
    public ItineraryActivityDTO updateActivityAmapAttraction(Long userId, Long activityId, com.se_07.backend.dto.AmapActivityCreateRequest.AmapAttractionInfo attractionInfo) {
        // 校验参数
        if (activityId == null) {
            throw new RuntimeException("activityId 为必填");
        }
        if (attractionInfo == null) {
            throw new RuntimeException("景点信息不能为空");
        }
        // 查找活动
        ItineraryActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        // 获取日程和行程
        ItineraryDay day = activity.getItineraryDay();
        if (day == null) {
            throw new RuntimeException("活动未关联日程");
        }
        Itinerary itinerary = day.getItinerary();
        if (itinerary == null) {
            throw new RuntimeException("日程数据异常，缺少行程");
        }
        // 权限校验
        if (itinerary.getGroupId() != null) {
            boolean isMember = travelGroupMemberRepository.findByGroupIdAndUserId(itinerary.getGroupId(), userId).isPresent();
            if (!isMember) {
                throw new RuntimeException("您不是该团队的成员,无权编辑此活动");
            }
        } else {
            if (!itinerary.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权修改此活动");
            }
        }
        // 查找或创建景点
        Attraction attraction = findOrCreateAttractionFromAmap(attractionInfo);
        // 更新活动景点
        activity.setAttraction(attraction);
        ItineraryActivity savedActivity = activityRepository.save(activity);
        return activityConverter.toDTO(savedActivity);
    }
} 
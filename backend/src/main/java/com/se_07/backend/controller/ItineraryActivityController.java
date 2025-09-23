package com.se_07.backend.controller;

import com.se_07.backend.dto.ActivityCreateRequest;
import com.se_07.backend.dto.AmapActivityCreateRequest;
import com.se_07.backend.dto.ItineraryActivityDTO;
import com.se_07.backend.dto.TransportModeUpdateRequest;
import com.se_07.backend.service.ItineraryActivityService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activities")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ItineraryActivityController {

    @Autowired
    private ItineraryActivityService activityService;

    /**
     * 为指定日程创建活动
     * POST /api/activities
     */
    @PostMapping
    public ResponseEntity<ItineraryActivityDTO> createActivity(@RequestBody ActivityCreateRequest request,
                                                               HttpSession session) {
        System.out.println("-----------start create activity controller------------");
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        ItineraryActivityDTO dto = activityService.createActivity(userId, request);
        return ResponseEntity.ok(dto);
    }

    /**
     * 获取指定日程的全部活动
     * GET /api/activities/day/{dayId}
     */
    @GetMapping("/day/{dayId}")
    public ResponseEntity<java.util.List<ItineraryActivityDTO>> getActivitiesByDay(@PathVariable Long dayId,
                                                                                   HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        java.util.List<ItineraryActivityDTO> list = activityService.getActivitiesByDay(userId, dayId);
        return ResponseEntity.ok(list);
    }

    /**
     * 更新活动的交通方式
     * PUT /api/activities/{activityId}/transport
     */
    @PutMapping("/{activityId}/transport")
    public ResponseEntity<ItineraryActivityDTO> updateTransportMode(@PathVariable Long activityId,
                                                                    @RequestBody TransportModeUpdateRequest request,
                                                                    HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        ItineraryActivityDTO updatedActivity = activityService.updateTransportMode(userId, activityId, request.getTransportMode());
        return ResponseEntity.ok(updatedActivity);
    }

    /**
     * 更新活动的景点
     * PUT /api/activities/{activityId}/attraction
     */
    @PutMapping("/{activityId}/attraction")
    public ResponseEntity<ItineraryActivityDTO> updateActivityAttraction(@PathVariable Long activityId,
                                                                         @RequestBody java.util.Map<String, Object> request,
                                                                         HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        Long attractionId = Long.valueOf(request.get("attractionId").toString());
        ItineraryActivityDTO updatedActivity = activityService.updateActivityAttraction(userId, activityId, attractionId);
        return ResponseEntity.ok(updatedActivity);
    }

    /**
     * 更新活动的备注
     * PUT /api/activities/{activityId}/notes
     */
    @PutMapping("/{activityId}/notes")
    public ResponseEntity<ItineraryActivityDTO> updateActivityNotes(@PathVariable Long activityId,
                                                                    @RequestBody java.util.Map<String, String> request,
                                                                    HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        String attractionNotes = request.get("attractionNotes");
        ItineraryActivityDTO updatedActivity = activityService.updateActivityNotes(userId, activityId, attractionNotes);
        return ResponseEntity.ok(updatedActivity);
    }

    /**
     * 更新活动的时间
     * PUT /api/activities/{activityId}/time
     */
    @PutMapping("/{activityId}/time")
    public ResponseEntity<ItineraryActivityDTO> updateActivityTime(@PathVariable Long activityId,
                                                                   @RequestBody java.util.Map<String, String> request,
                                                                   HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        String startTime = request.get("startTime");
        String endTime = request.get("endTime");
        ItineraryActivityDTO updatedActivity = activityService.updateActivityTime(userId, activityId, startTime, endTime);
        return ResponseEntity.ok(updatedActivity);
    }

    /**
     * 更新活动标题
     * PUT /api/activities/{activityId}/title
     */
    @PutMapping("/{activityId}/title")
    public ResponseEntity<ItineraryActivityDTO> updateActivityTitle(@PathVariable Long activityId,
                                                                    @RequestBody java.util.Map<String, String> request,
                                                                    HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        String title = request.get("title");
        ItineraryActivityDTO updated = activityService.updateActivityTitle(userId, activityId, title);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除活动
     * DELETE /api/activities/{activityId}
     */
    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long activityId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        activityService.deleteActivity(userId, activityId);
        return ResponseEntity.ok().build();
    }

    /**
     * 通过高德地图API创建行程活动
     * POST /api/activities/amap
     */
    @PostMapping("/amap")
    public ResponseEntity<ItineraryActivityDTO> createActivityFromAmap(@RequestBody AmapActivityCreateRequest request,
                                                                       HttpSession session) {
        System.out.println("-----------start create activity from amap controller------------");
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        ItineraryActivityDTO dto = activityService.createActivityFromAmap(userId, request);
        return ResponseEntity.ok(dto);
    }

    /**
     * 通过高德地图API更新活动景点
     * PUT /api/activities/{activityId}/amap-attraction
     * 请求体：AmapActivityCreateRequest，仅使用attractionInfo字段
     * 示例postBody：
     * {
     *   "itineraryDayId": 29,
     *   "attractionInfo": {
     *     "id": "BV10820968",
     *     "name": "永德路(地铁站)",
     *     "address": "15号线",
     *     "city": "上海",
     *     "description": "15号线",
     *     "longitude": 121.443203,
     *     "latitude": 31.039147,
     *     "tel": "",
     *     "type": "交通设施服务;地铁站;地铁站"
     *   }
     * }
     */
    @PutMapping("/{activityId}/amap-attraction")
    public ResponseEntity<ItineraryActivityDTO> updateActivityAmapAttraction(
            @PathVariable Long activityId,
            @RequestBody com.se_07.backend.dto.AmapActivityCreateRequest request,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        if (request.getAttractionInfo() == null) {
            return ResponseEntity.badRequest().build();
        }
        ItineraryActivityDTO updatedActivity = activityService.updateActivityAmapAttraction(userId, activityId, request.getAttractionInfo());
        return ResponseEntity.ok(updatedActivity);
    }
}
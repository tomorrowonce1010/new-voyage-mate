package com.se_07.backend.controller;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import com.se_07.backend.service.FriendService;

@RestController
@RequestMapping("/friends")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class FriendController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private FriendService friendService;

    /**
     * 删除好友（双向）
     * 接口映射：POST /friends/delete
     * 使用方法：
     *   - 参数：userId（操作用户ID，Long），friendId（被删除好友ID，Long）
     *   - Content-Type: application/x-www-form-urlencoded
     *   - 返回：
     *     {
     *       "success": true
     *     }
     *   - 删除后，双方会收到WebSocket推送：/topic/friend.{userId} 和 /topic/friend.{friendId}
     *     推送消息内容：
     *     {
     *       "type": "friend_removed",
     *       "userId": 1,      // 操作用户ID
     *       "friendId": 2     // 被删除好友ID
     *     }
     */
    @PostMapping("/delete")
    public Map<String, Object> deleteFriend(@RequestParam Long userId, @RequestParam Long friendId) {
        friendService.deleteFriend(userId, friendId);
        // 通知双方
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "friend_removed");
        msg.put("userId", userId);
        msg.put("friendId", friendId);
        messagingTemplate.convertAndSend("/topic/friend." + userId, msg);
        messagingTemplate.convertAndSend("/topic/friend." + friendId, msg);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        return resp;
    }

    /**
     * 添加好友（双向）
     * 接口映射：POST /friends/add
     * 使用方法：
     *   - 参数：userId（操作用户ID，Long），friendId（被添加好友ID，Long）
     *   - Content-Type: application/x-www-form-urlencoded
     *   - 返回：
     *     {
     *       "success": true
     *     }
     *   - 添加后，双方会收到WebSocket推送：/topic/friend.{userId} 和 /topic/friend.{friendId}
     *     推送消息内容：
     *     {
     *       "type": "friend_added",
     *       "userId": 1,      // 操作用户ID
     *       "friendId": 2     // 被添加好友ID
     *     }
     */
    @PostMapping("/add")
    public Map<String, Object> addFriend(@RequestParam Long userId, @RequestParam Long friendId) {
        friendService.addFriend(userId, friendId);
        // 通知双方
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "friend_added");
        msg.put("userId", userId);
        msg.put("friendId", friendId);
        messagingTemplate.convertAndSend("/topic/friend." + userId, msg);
        messagingTemplate.convertAndSend("/topic/friend." + friendId, msg);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        return resp;
    }
} 
package com.se_07.backend.controller;

import com.se_07.backend.dto.UserProfileResponse;
import com.se_07.backend.service.GroupChatManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/group")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class GroupChatManageController {
    @Autowired
    private GroupChatManageService groupChatManageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 创建群聊
     * 接口映射：POST /group/create
     * 使用方法：
     *   - 参数：groupName（群聊名称，String），creatorUserId（创建者用户ID，Long）
     *   - Content-Type: application/x-www-form-urlencoded
     *   - 返回：
     *     {
     *       "success": true,
     *       "groupId": 123
     *     }
     *   - 创建成功后，创建者会收到WebSocket推送：/topic/group.member.{creatorUserId}
     *     推送消息内容：
     *     {
     *       "type": "group_member_added",
     *       "groupId": 123,
     *       "userId": 1, // 创建者ID
     *       "groupName": "群聊名称"
     *     }
     */
    @PostMapping("/create")
    public Map<String, Object> createGroup(@RequestParam String groupName, @RequestParam Long creatorUserId) {
        Long groupId = groupChatManageService.createGroup(groupName, creatorUserId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("groupId", groupId);
        // --- 新增：推送消息给创建者 ---
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "group_member_added");
        msg.put("groupId", groupId);
        msg.put("userId", creatorUserId);
        msg.put("groupName", groupName);
        messagingTemplate.convertAndSend("/topic/group.member." + creatorUserId, msg);
        // ---
        return resp;
    }

    /**
     * 多人创建群聊
     * 接口映射：POST /group/createWithMembers
     * 使用方法：
     *   - 请求体JSON：
     *     {
     *       "groupName": "群名",
     *       "creatorUserId": 1,
     *       "memberIds": [2, 3, 4]
     *     }
     *   - 返回：
     *     {
     *       "success": true,
     *       "groupId": 123
     *     }
     *   - 创建成功后，所有初始成员（包括创建者）会收到WebSocket推送：/topic/group.member.{userId}
     *     推送消息内容：
     *     {
     *       "type": "group_member_added",
     *       "groupId": 123,
     *       "userId": 2, // 成员ID
     *       "groupName": "群聊名称"
     *     }
     */
    @PostMapping("/createWithMembers")
    public Map<String, Object> createGroupWithMembers(@RequestBody Map<String, Object> req) {
        String groupName = (String) req.get("groupName");
        Long creatorUserId = Long.valueOf(req.get("creatorUserId").toString());
        List<Integer> memberIdsInt = (List<Integer>) req.get("memberIds");
        List<Long> memberIds = memberIdsInt.stream().map(Long::valueOf).collect(Collectors.toList());
        Long groupId = groupChatManageService.createGroupWithMembers(groupName, creatorUserId, memberIds);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("groupId", groupId);
        // --- 新增：推送消息给所有初始成员 ---
        List<Long> allMemberIds = new java.util.ArrayList<>(memberIds);
        if (!allMemberIds.contains(creatorUserId)) allMemberIds.add(creatorUserId);
        for (Long userId : allMemberIds) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "group_member_added");
            msg.put("groupId", groupId);
            msg.put("userId", userId);
            msg.put("groupName", groupName);
            messagingTemplate.convertAndSend("/topic/group.member." + userId, msg);
        }
        // ---
        return resp;
    }

    /**
     * 拉人入群
     * 接口映射：POST /group/addUser
     * 使用方法：
     *   - 参数：groupId（群聊ID，Long），userId（被拉用户ID，Long）
     *   - Content-Type: application/x-www-form-urlencoded
     *   - 返回：
     *     {
     *       "success": true
     *     }
     *   - 拉人成功后，被拉用户会收到WebSocket推送：/topic/group.member.{userId}
     *     推送消息内容：
     *     {
     *       "type": "group_member_added",
     *       "groupId": 123,
     *       "userId": 2, // 被拉用户ID
     *       "groupName": "群聊名称"
     *     }
     */
    @PostMapping("/addUser")
    public Map<String, Object> addUserToGroup(@RequestParam Long groupId, @RequestParam Long userId) {
        groupChatManageService.addUserToGroup(groupId, userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        // --- 新增：推送消息给被拉用户 ---
        // 查询群聊名称（可选优化：如无service方法可用可省略）
        String groupName = "新群聊";
        try {
            Object info = groupChatManageService.getGroupMembers(groupId);
            if (info instanceof java.util.List && !((java.util.List<?>)info).isEmpty()) {
                // 这里假设有UserProfileResponse，实际可根据业务调整
                groupName = "群聊" + groupId;
            }
        } catch (Exception e) {}
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "group_member_added");
        msg.put("groupId", groupId);
        msg.put("userId", userId);
        msg.put("groupName", groupName);
        messagingTemplate.convertAndSend("/topic/group.member." + userId, msg);
        // ---
        return resp;
    }

    /**
     * 修改群聊名称
     * 接口映射：POST /group/updateName
     * 使用方法：
     *   - 参数：groupId（群聊ID，Long），newName（新群名，String）
     *   - Content-Type: application/x-www-form-urlencoded
     *   - 返回：
     *     {
     *       "success": true
     *     }
     */
    @PostMapping("/updateName")
    public Map<String, Object> updateGroupName(@RequestParam Long groupId, @RequestParam String newName) {
        groupChatManageService.updateGroupName(groupId, newName);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        return resp;
    }

    /**
     * 获取群聊所有成员
     * 接口映射：GET /group/members
     * 使用方法：
     *   - 参数：groupId（群聊ID，Long）
     *   - 返回：用户信息数组（UserProfileResponse列表）
     *   - 响应示例：
     *     [
     *       {
     *         "id": 1,
     *         "username": "张三",
     *         "email": "zhangsan@example.com",
     *         "avatarUrl": "http://...",
     *         "birthday": "2000-01-01",
     *         "signature": "个性签名",
     *         ...
     *       },
     *       ...
     *     ]
     */
    @GetMapping("/members")
    public List<UserProfileResponse> getGroupMembers(@RequestParam Long groupId) {
        return groupChatManageService.getGroupMembers(groupId);
    }

    /**
     * 移除群成员
     * 接口映射：POST /group/removeUser
     * 使用方法：
     *   - 参数：groupId（群聊ID，Long），userId（被移除用户ID，Long）
     *   - Content-Type: application/x-www-form-urlencoded
     *   - 返回：
     *     {
     *       "success": true
     *     }
     *   - 被移除用户会收到WebSocket推送：/topic/group.member.{userId}
     *     消息内容：
     *     {
     *       "type": "group_member_removed",
     *       "groupId": 123,
     *       "userId": 2
     *     }
     */
    @PostMapping("/removeUser")
    public Map<String, Object> removeUserFromGroup(@RequestParam Long groupId, @RequestParam Long userId) {
        groupChatManageService.removeUserFromGroup(groupId, userId);
        // 推送被踢出消息
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "group_member_removed");
        msg.put("groupId", groupId);
        msg.put("userId", userId);
        messagingTemplate.convertAndSend("/topic/group.member." + userId, msg);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        return resp;
    }

    /**
     * 退出群聊
     * 接口映射：POST /group/leave
     * 使用方法：
     *   - 参数：groupId（群聊ID，Long），userId（操作用户ID，Long）
     *   - Content-Type: application/x-www-form-urlencoded
     *   - 返回：
     *     {
     *       "success": true
     *     }
     */
    @PostMapping("/leave")
    public Map<String, Object> leaveGroup(@RequestParam Long groupId, @RequestParam Long userId) {
        groupChatManageService.leaveGroup(groupId, userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        return resp;
    }
} 
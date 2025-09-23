package com.se_07.backend.controller;

import com.se_07.backend.dto.GroupChatMessageDTO;
import com.se_07.backend.entity.GroupChatInformation;
import com.se_07.backend.service.GroupChatQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/group")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class GroupChatQueryController {
    @Autowired
    private GroupChatQueryService groupChatQueryService;

    /**
     * 获取用户所在所有群聊
     * 接口映射：POST /group/listByUser
     * 使用方法：
     *   - 参数：userId（用户ID，Long）
     *   - Content-Type: application/x-www-form-urlencoded
     *   - 返回：群聊信息数组（GroupChatInformation列表）
     *   - 响应示例：[ {"groupId":1, "groupName":"群聊1", ...}, ... ]
     */
    @PostMapping("/listByUser")
    public List<GroupChatInformation> getGroupsByUser(@RequestParam Long userId) {
        return groupChatQueryService.getGroupsByUserId(userId);
    }

    /**
     * 获取用户所有群聊的历史消息
     * 接口映射：GET /group/historyByUser
     * 使用方法：
     *   - 参数：userId（用户ID，Long）
     *   - 返回：Map<群聊ID, 消息列表>，如 { "1": [消息DTO, ...], "2": [消息DTO, ...] }
     *   - 响应示例：{ "1": [ {"messageId":1, "content":"hi", ...}, ... ] }
     */
    @GetMapping("/historyByUser")
    public Map<Long, List<GroupChatMessageDTO>> getGroupHistoriesByUser(@RequestParam Long userId) {
        return groupChatQueryService.getGroupHistoriesByUserId(userId);
    }
} 
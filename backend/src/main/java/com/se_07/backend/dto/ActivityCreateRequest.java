package com.se_07.backend.dto;

import lombok.Data;
import java.time.LocalTime;

/**
 * 请求体：创建行程活动
 */
@Data
public class ActivityCreateRequest {
    private Long itineraryDayId; // 必填：所属日程ID
    private Long attractionId;   // 必填：景点ID

    private String title;        // 活动标题
    private String transportMode; // 交通方式，如 步行 / 公交
    private LocalTime startTime;  // 开始时间
    private LocalTime endTime;    // 结束时间
    private String attractionNotes;

    /**
     * 可选：指定插入链表位置，prevId == null 表示插入到链表开头
     * 如果都为 null，则默认追加到链表末尾。
     */
    private Long prevId;
    // 后序活动ID：若要插入到某活动之前可填写该ID
    private Long nextId;
} 
package com.se_07.backend.dto;

import lombok.Data;
import java.time.LocalTime;

/**
 * 请求体：通过高德地图API创建行程活动
 */
@Data
public class AmapActivityCreateRequest {
    private Long itineraryDayId; // 必填：所属日程ID
    private Long attractionId;   // 可选：景点ID，如果为null则根据attractionInfo创建新景点
    private String title;        // 活动标题
    private String transportMode; // 交通方式，如 步行 / 公交
    private LocalTime startTime;  // 开始时间
    private LocalTime endTime;    // 结束时间
    private Long nextId;         // 后序活动ID：若要插入到某活动之前可填写该ID
    
    // 高德地图景点信息
    private AmapAttractionInfo attractionInfo;
    
    @Data
    public static class AmapAttractionInfo {
        private String id;           // 高德地图POI ID
        private String name;         // 景点名称
        private String address;      // 地址
        private String city;         // 城市
        private String cityname;     // 高德地图返回的城市名称
        private String description;  // 描述
        private Double longitude;    // 经度
        private Double latitude;     // 纬度
        private String tel;          // 电话
        private String type;         // 类型
    }
} 
package com.se_07.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * AI生成的行程导入请求DTO
 */
@Data
public class AIItineraryImportRequest {
    
    /**
     * 行程标题
     */
    private String title;
    
    /**
     * 行程天数
     */
    private Integer days;
    
    /**
     * 出行人数
     */
    private Integer travelers;
    
    /**
     * 预算
     */
    private BigDecimal budget;
    
    /**
     * 行程计划
     */
    private List<DayPlan> plan;
    
    /**
     * 单日行程计划
     */
    @Data
    public static class DayPlan {
        
        /**
         * 第几天
         */
        private Integer day;
        
        /**
         * 当天的活动列表
         */
        private List<ActivityPlan> activities;
    }
    
    /**
     * 活动计划
     */
    @Data
    public static class ActivityPlan {
        
        /**
         * 活动名称（景点名称）
         */
        private String name;
        
        /**
         * 开始时间
         */
        private String startTime;
        
        /**
         * 结束时间
         */
        private String endTime;
        
        /**
         * 交通方式
         */
        private String transportMode;
        
        /**
         * 活动描述
         */
        private String description;
    }
} 
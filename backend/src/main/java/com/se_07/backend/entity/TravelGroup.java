package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "travel_groups")
@Data
public class TravelGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "title", length = 200, nullable = false)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;
    
    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    
    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 6;
    
    @Column(name = "current_members", nullable = false)
    private Integer currentMembers = 1;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "estimated_budget", precision = 10, scale = 2)
    private BigDecimal estimatedBudget;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false)
    private GroupType groupType = GroupType.自由行;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GroupStatus status = GroupStatus.招募中;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_level", nullable = false)
    private PrivacyLevel privacyLevel = PrivacyLevel.公开;
    
    @Column(name = "auto_match_enabled")
    private Boolean autoMatchEnabled = true;
    
    @Column(name = "is_public")
    private Boolean isPublic = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 关联的群聊ID，组团创建时自动生成群聊并关联
     */
    @Column(name = "group_chat_id")
    private Long groupChatId;
    
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TravelGroupMember> members;
    
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TravelGroupApplication> applications;
    
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TravelGroupTag> groupTags;
    
    @OneToOne
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;
    
    public enum GroupType {
        自由行, 半自助, 深度游
    }
    
    public enum GroupStatus {
        招募中, 已满员, 已出行, 已结束, 已取消
    }
    
    public enum PrivacyLevel {
        公开, 仅链接可见, 邀请制
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // 业务方法
    public boolean isFull() {
        return currentMembers >= maxMembers;
    }
    
    public boolean canJoin() {
        return status == GroupStatus.招募中 && !isFull();
    }
    
    public boolean isCreator(Long userId) {
        return creator != null && creator.getId().equals(userId);
    }
    
    public int getAvailableSlots() {
        return Math.max(0, maxMembers - currentMembers);
    }
    
    // 自动更新状态
    public void updateStatusIfNeeded() {
        if (status == GroupStatus.招募中 && isFull()) {
            status = GroupStatus.已满员;
        }
    }
    
    // 检查是否可以手动更改状态
    public boolean canUpdateStatus(GroupStatus newStatus) {
        // 创建者可以将已满员状态改为已结束
        if (status == GroupStatus.已满员 && newStatus == GroupStatus.已结束) {
            return true;
        }
        // 创建者可以将招募中状态改为已取消
        if (status == GroupStatus.招募中 && newStatus == GroupStatus.已取消) {
            return true;
        }
        return false;
    }
    
    // 获取旅行标签
    public List<TravelGroupTag> getTravelTags() {
        return groupTags;
    }
    
    // 设置旅行标签
    public void setTravelTags(List<TravelGroupTag> travelTags) {
        this.groupTags = travelTags;
    }
} 
package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_group_members")
@Data
public class TravelGroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private TravelGroup group;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MemberRole role = MemberRole.成员;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "join_status", nullable = false)
    private JoinStatus joinStatus = JoinStatus.已加入;
    
    @Column(name = "join_date")
    private LocalDateTime joinDate;
    
    public enum MemberRole {
        创建者, 管理员, 成员
    }
    
    public enum JoinStatus {
        已加入, 待审核, 已拒绝, 已退出
    }
    
    @PrePersist
    protected void onCreate() {
        if (joinDate == null) {
            joinDate = LocalDateTime.now();
        }
    }
} 
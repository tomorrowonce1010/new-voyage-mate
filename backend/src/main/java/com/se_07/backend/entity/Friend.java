package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "friend")
@IdClass(FriendId.class)
@Data
public class Friend {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Id
    @Column(name = "friend_id", nullable = false)
    private Long friendId;
} 
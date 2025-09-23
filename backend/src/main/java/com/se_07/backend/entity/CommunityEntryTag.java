package com.se_07.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "community_entry_tags")
@Data
@IdClass(CommunityEntryTagId.class)
public class CommunityEntryTag {
    
    @Id
    @ManyToOne
    @JoinColumn(name = "share_entry_id", nullable = false)
    private CommunityEntry communityEntry;
    
    @Id
    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
} 
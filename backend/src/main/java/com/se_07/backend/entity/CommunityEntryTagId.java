package com.se_07.backend.entity;

import java.io.Serializable;
import java.util.Objects;

public class CommunityEntryTagId implements Serializable {
    private Long communityEntry;
    private Long tag;

    public CommunityEntryTagId() {}

    public CommunityEntryTagId(Long communityEntry, Long tag) {
        this.communityEntry = communityEntry;
        this.tag = tag;
    }

    public Long getCommunityEntry() {
        return communityEntry;
    }

    public void setCommunityEntry(Long communityEntry) {
        this.communityEntry = communityEntry;
    }

    public Long getTag() {
        return tag;
    }

    public void setTag(Long tag) {
        this.tag = tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommunityEntryTagId that = (CommunityEntryTagId) o;
        return Objects.equals(communityEntry, that.communityEntry) && Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(communityEntry, tag);
    }
} 
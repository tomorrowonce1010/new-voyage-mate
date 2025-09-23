package com.se_07.backend.entity;

import java.io.Serializable;
import java.util.Objects;

public class FriendId implements Serializable {
    private Long id;
    private Long friendId;

    public FriendId() {}
    public FriendId(Long id, Long friendId) {
        this.id = id;
        this.friendId = friendId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FriendId)) return false;
        FriendId that = (FriendId) o;
        return Objects.equals(id, that.id) && Objects.equals(friendId, that.friendId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, friendId);
    }
} 
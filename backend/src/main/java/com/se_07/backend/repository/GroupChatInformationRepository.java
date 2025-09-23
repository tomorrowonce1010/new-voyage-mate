package com.se_07.backend.repository;

import com.se_07.backend.entity.GroupChatInformation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupChatInformationRepository extends JpaRepository<GroupChatInformation, Long> {
    List<GroupChatInformation> findByGroupIdIn(List<Long> groupIds);
} 
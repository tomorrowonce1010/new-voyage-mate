package com.se_07.backend.repository;

import com.se_07.backend.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByTag(String tag);
    List<Tag> findAllByOrderById();
    List<Tag> findByTagIn(List<String> tags);
    List<Tag> findTop30ByOrderByIdAsc();
} 
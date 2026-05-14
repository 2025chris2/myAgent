package com.tzl.llongagent.repository;

import com.tzl.llongagent.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<ConversationEntity, String> {
    List<ConversationEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByIdAndUserId(String id, String userId);
}

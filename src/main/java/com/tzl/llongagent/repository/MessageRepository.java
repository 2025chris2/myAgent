package com.tzl.llongagent.repository;

import com.tzl.llongagent.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    void deleteByConversationId(String conversationId);
    @Query(value = "SELECT DISTINCT conversation_id FROM message", nativeQuery = true)
    List<String> findAllConversationIds();
}

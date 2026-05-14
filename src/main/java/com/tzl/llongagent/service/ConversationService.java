package com.tzl.llongagent.service;

import cn.hutool.core.util.RandomUtil;
import com.tzl.llongagent.entity.ConversationEntity;
import com.tzl.llongagent.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public String create(String userId) {
        ConversationEntity conv = new ConversationEntity();
        conv.setId(RandomUtil.randomString(6));
        conv.setUserId(userId);
        return conversationRepository.save(conv).getId();
    }

    public String create(String userId, String title) {
        ConversationEntity conv = new ConversationEntity();
        conv.setId(RandomUtil.randomString(6));
        conv.setUserId(userId);
        conv.setTitle(title);
        return conversationRepository.save(conv).getId();
    }

    public List<ConversationEntity> listByUser(String userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public boolean belongsToUser(String conversationId, String userId) {
        return conversationRepository.existsByIdAndUserId(conversationId, userId);
    }

    public ConversationEntity findByIdOrThrow(String conversationId, String userId) {
        ConversationEntity conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        if (!conv.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此会话");
        }
        return conv;
    }

    public void delete(String conversationId, String userId) {
        if (!belongsToUser(conversationId, userId)) {
            throw new RuntimeException("无权删除此会话");
        }
        conversationRepository.deleteById(conversationId);
    }
}

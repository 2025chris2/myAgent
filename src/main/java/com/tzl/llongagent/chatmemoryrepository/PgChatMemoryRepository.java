package com.tzl.llongagent.chatmemoryrepository;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.tzl.llongagent.entity.MessageEntity;
import com.tzl.llongagent.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgChatMemoryRepository implements ChatMemoryRepository {

    private final MessageRepository messageRepository;

    @Override
    public List<String> findConversationIds() {
        return List.of();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<MessageEntity> entities = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<Message> messages = new ArrayList<>();
        for (MessageEntity entity : entities) {
            messages.add(deserializeMessage(entity.getContent(), entity.getMessageType()));
        }
        return messages;
    }

    @Override
    @Transactional
    public void saveAll(String conversationId, List<Message> messages) {
        messageRepository.deleteByConversationId(conversationId);

        List<MessageEntity> entities = messages.stream()
                .map(msg -> {
                    MessageEntity entity = new MessageEntity();
                    entity.setConversationId(conversationId);
                    entity.setMessageType(msg.getMessageType().name());
                    entity.setContent(JSON.toJSONString(msg));
                    return entity;
                })
                .toList();

        messageRepository.saveAll(entities);
    }

    @Override
    @Transactional
    public void deleteByConversationId(String conversationId) {
        messageRepository.deleteByConversationId(conversationId);
    }

    private Message deserializeMessage(String json, String messageType) {
        JSONObject obj = JSON.parseObject(json);
        String text = obj.getString("text");
        if (text == null) {
            text = "";
        }

        MessageType type = MessageType.valueOf(messageType);
        return switch (type) {
            case USER -> new UserMessage(text);
            case ASSISTANT -> new AssistantMessage(text);
            case SYSTEM -> new SystemMessage(text);
            case TOOL -> new AssistantMessage(text);
        };
    }
}

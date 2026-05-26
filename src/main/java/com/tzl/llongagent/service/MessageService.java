package com.tzl.llongagent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.tzl.llongagent.entity.MessageEntity;
import com.tzl.llongagent.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageService {

    // 通过注解进行注入
    private final MessageRepository messageRepository;

    @Transactional
    public void saveMessages(String conversationId, List<Message> messages) {
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

    @Transactional
    public void replaceMessages(String conversationId, List<Message> messages) {
        messageRepository.deleteByConversationId(conversationId);
        saveMessages(conversationId, messages);
    }

    public List<Map<String, Object>> loadMessagesForFrontend(String conversationId) {
        List<MessageEntity> entities = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        return entities.stream()
                .map(entity -> {
                    String role = entity.getMessageType().equals("USER") ? "user" : "assistant";
                    String content = "";
                    try {
                        JSONObject obj = JSON.parseObject(entity.getContent());
                        String text = obj.getString("text");
                        if (text != null) content = text;
                    } catch (Exception ignored) {
                        content = entity.getContent();
                    }
                    return Map.<String, Object>of(
                            "id", String.valueOf(entity.getId()),
                            "role", role,
                            "content", content,
                            "time", entity.getCreatedAt() != null
                                    ? entity.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                    : ""
                    );
                })
                .toList();
    }

    public List<Message> loadMessages(String conversationId) {
        List<MessageEntity> entities = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<Message> messages = new ArrayList<>();
        for (MessageEntity entity : entities) {
            messages.add(deserializeMessage(entity.getContent(), entity.getMessageType()));
        }
        return messages;
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

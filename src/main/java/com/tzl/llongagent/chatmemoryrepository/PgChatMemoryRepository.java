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

    // 由于打了 @RequiredArgsConstructor 注解，使 MessageRepository 可以自动注入
    private final MessageRepository messageRepository;

    @Override
    public List<String> findConversationIds() {
        return messageRepository.findAllConversationIds();
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

    // 先删后查，保持数据库与前端展示的数据一致性
    // @Transactional 保证 原子性与安全性
    @Override
    @Transactional
    public void saveAll(String conversationId, List<Message> messages) {

        // 先删除原来的信息
        messageRepository.deleteByConversationId(conversationId);

        // 处理传过来的 Message，对其进行处理再存储
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

    // 配合上面的方法，保证数据的安全
    @Override
    @Transactional
    public void deleteByConversationId(String conversationId) {
        messageRepository.deleteByConversationId(conversationId);
    }

    // 反序列化，读取数据库中存储的信息
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

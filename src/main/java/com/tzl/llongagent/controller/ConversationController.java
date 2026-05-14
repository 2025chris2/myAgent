package com.tzl.llongagent.controller;

import com.tzl.llongagent.service.ConversationService;
import com.tzl.llongagent.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        String conversationId = conversationService.create(userId);
        return ResponseEntity.ok(Map.of("conversationId", conversationId));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listConversations(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        var conversations = conversationService.listByUser(userId);
        var result = conversations.stream()
                .map(conv -> Map.<String, Object>of(
                        "id", conv.getId(),
                        "title", conv.getTitle() != null ? conv.getTitle() : "",
                        "createdAt", conv.getCreatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @PathVariable String conversationId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        if (!conversationService.belongsToUser(conversationId, userId)) {
            return ResponseEntity.status(403).build();
        }
        List<Map<String, Object>> messages = messageService.loadMessagesForFrontend(conversationId);
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> delete(@PathVariable String conversationId,
                                       Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        conversationService.delete(conversationId, userId);
        return ResponseEntity.noContent().build();
    }
}

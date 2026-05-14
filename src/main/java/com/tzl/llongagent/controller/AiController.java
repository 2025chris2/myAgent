package com.tzl.llongagent.controller;

import com.tzl.llongagent.agent.llongManus;
import com.tzl.llongagent.app.PlanApp;
import com.tzl.llongagent.service.ConversationService;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/plan_app/ai")
public class AiController {

    @Resource
    private PlanApp planApp;

    @Resource
    private llongManus llongManus;

    @Resource
    private ConversationService conversationService;

    private String resolveConversationId(Authentication auth, String conversationId) {
        String userId = auth != null ? (String) auth.getPrincipal() : "anonymous";
        if (conversationId == null || conversationId.isBlank()) {
            return conversationService.create(userId);
        }
        if (auth != null) {
            conversationService.findByIdOrThrow(conversationId, userId);
        }
        return conversationId;
    }

    // ========== PlanApp 接口 ==========

    @GetMapping("/chat/sync")
    public String doChatWithPlanAppSync(
            @RequestParam String userMessage,
            @RequestParam(required = false) String conversationId,
            Authentication authentication) {
        String convId = resolveConversationId(authentication, conversationId);
        return planApp.doChat(userMessage, convId);
    }

    @GetMapping("/chat/async")
    public SseEmitter doChatWithPlanAppServerSseEmitter(
            @RequestParam String userMessage,
            @RequestParam(required = false) String conversationId,
            Authentication authentication) {
        String convId = resolveConversationId(authentication, conversationId);
        SseEmitter sseEmitter = new SseEmitter(180000L);

        planApp.doChatByStream(userMessage, convId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.complete();
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        return sseEmitter;
    }

    // ========== llongManus Agent 接口 ==========

    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(
            @RequestParam String userMessage,
            @RequestParam(required = false) String conversationId,
            Authentication authentication) {
        String convId = resolveConversationId(authentication, conversationId);
        return llongManus.runStream(userMessage, convId);
    }
}

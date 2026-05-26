package com.tzl.llongagent.agent;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.tzl.llongagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/***
 * 抽象的基础代理类，用于管理代理状态和执行流程
 * 提供状态转换，内存管理和基于步骤的执行循环的基础功能
 * 子类必须实现 step 方法
 * 
 * 这里的父类只做声明和定义，具体由子类来实现和填充
 */
@Slf4j
@Data
public abstract class BaseAgent {

    // Agent 的名字
    private String name;

    // 推送流式信息的工具
    // 这里不写为 private ,原因：子类用会更便捷，不用先 getSseEmitter() 再 send()
    protected SseEmitter sseEmitter;

    // 系统提示词
    private String SYSTEM_PROMPT;

    // 引导智能体下一步的提示词
    private String NEXT_STEP_PROMPT;

    // agent 的状态,默认是空闲
    private volatile AgentState state = AgentState.IDLE;

    // 当前步骤
    private int currentStep = 0;

    // 最大的执行次数
    private int maxStep = 20;

    // 防止 cleanUp() 被多次调用
    private volatile boolean cleaned = false;

    // AI 最终的回答内容
    // 这里用 protected 修饰是为了让子类直接访问
    protected String finalAnswer;

    // LLM 大模型
    // 这里只做 声明，由子类传入大模型，解耦
    private ChatClient deepseekChatClient;

    // 大模型的上下文 memory 记忆(需自己维护)
    // 此 messageList　是给大模型看的
    // results 是自定义的用来给用户看的
    // 由于服务对象不同，存储的时机也不同!
    private List<Message> messageList = new ArrayList<>();

    /***
     * 运行代理
     * @param userMessage 用户的提示词
     * @param conversationId 用户的对话ID
     * @return 执行结果
     */
    public String run(String userMessage, String conversationId) {

        // 1.基础校验
        // 对 Agent 状态和提示词合法性 进行判断
        if (this.state != AgentState.IDLE)
            throw new RuntimeException("Agent cannot run agent form this state :" + state);
        if (StrUtil.isBlank(userMessage))
            throw new RuntimeException("Agent cannot run with empty userMessage!");
        if (StrUtil.isBlank(conversationId) || StrUtil.length(conversationId) != 6)
            throw new RuntimeException("conversationId must be 6 non-blank characters!");

        // 2.修改状态,避免冲突
        this.state = AgentState.RUNNING;
        this.cleaned = false;

        // 3.记录上下文信息
        messageList.add(new UserMessage(userMessage));

        // 4.添加下一步引导提示词（仅一次）
        if (StrUtil.isNotBlank(getNEXT_STEP_PROMPT())) {
            messageList.add(new UserMessage(getNEXT_STEP_PROMPT()));
        }

        // 5.保存结果列表(大模型返回的是 String)
        // 此　results 是给用户看的，不是给大模型看的
        // messageList 是给大模型看的，不是给用户看的
        // 所以存储的时机不同
        List<String> results = new ArrayList<>();

        try {
            // 在 step()中，子类实现时，有可能把 state 设为 AgentState.FINISHED
            // 退出循环的条件是： 
            // 一： AgentState.FINISHED
            // 二： maxStep 
            // 一般是 AgentState.FINISHED 正常退出循环，如果是 maxStep 则需要 if 语句来判断
            for (int i = 0; i < maxStep && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxStep);
                // 单步执行结果
                String stepResult = this.step();
                String result = "Step " + stepNumber + ":" + stepResult;

                // 把单步的执行结果，传给 String 列表，存储起来
                results.add(result);
            }

            // 如果当步骤达到最大步骤且 state 并不为 FINISHED,则说明在执行过程中被打断，并不是正常的退出流程
            // 如果为 FINISHED 则说明，在执行的最后一步，完成了任务

            // 这里有两个条件,一： !AgentState.FINISHED,二： maxStep
            // 一般退出的是条件一，如果是条件二，那么需要后续的 if 来判断
            // 如果是条件一：意味着 消息都结束了，存在 messageList 中，可以进行后续处理

            // 判断: 第20步的状态是否是 AgentState.FINISHED
            // 一： 是，正常退出    二： 不是，未执行完退出，不正常的退出，需要打日志和添加异常消息
            if (currentStep == maxStep && state != AgentState.FINISHED) {
                this.state = AgentState.FINISHED;
                results.add("Terminated: Agent has reached max step: (" + maxStep + ")");
                log.warn("Reached max step " + maxStep);
            }

            // 追加最终答案,这里在 step() 中会给 finalAnswer 设置值的
            // 流式与非流式的 前端渲染 的核心！
            if (StrUtil.isNotBlank(getFinalAnswer())) {
                results.add("Final Answer: " + getFinalAnswer());
            }

            // 此 results 是返回给用户，所以在这要拼接
            // 把 String 列表，拼接为一个 String 字符串
            return String.join("\n", results);
            
        }catch(Exception e) {
            this.state = AgentState.ERROR;
            log.error("Agent executing error",e);
            results.add("执行错误: " + e.getMessage());
            // 此 results 是返回给用户，所以在这要拼接
            return String.join("\n", results);
        } finally {
            // 持久化消息，子类实现
            persistMessages(conversationId);
            cleanUp();
        }
    }

    /***
     * 运行代理
     * @param userMessage 用户的提示词
     * @param conversationId 对话ID
     * @return 执行结果
     */
    public SseEmitter runStream(String userMessage, String conversationId) {

        // 创建一个连接时间较长的 SseEmitter
        SseEmitter emitter = new SseEmitter(300000L);
        this.sseEmitter = emitter;

        // 使用线程异步处理,避免阻塞主线程
        CompletableFuture.runAsync(() -> {

            // 判断 Agent 的状态
            if(this.state != AgentState.IDLE) {
                sendSseEvent("error", "无法运行非空闲状态的 Agent, 此Agent 的状态: " + this.state);
                emitter.complete();
                return;
            }

            // 判断提示词
            if(StrUtil.isBlank(userMessage)) {
                sendSseEvent("error", "无法运行空提示词!");
                emitter.complete();
                return;
            }

            // 判断 Id
            if(StrUtil.isBlank(conversationId) || StrUtil.length(conversationId) != 6) {
                sendSseEvent("error", "无法运行 Agent With 不到6个Id");
                emitter.complete();
                return;
            }

            // 修改 Agent 的状态, 防止多线程调用同一行 Agent
            this.state = AgentState.RUNNING;
            this.cleaned = false;

            // 记录上下文消息
            messageList.add(new UserMessage(userMessage));

            // 添加下一步引导提示词（仅一次）
            if (StrUtil.isNotBlank(getNEXT_STEP_PROMPT())) {
                messageList.add(new UserMessage(getNEXT_STEP_PROMPT()));
            }

            // 创建一个列表，用于保存结果列表
            List<String> results = new ArrayList<>();

            // 使用 try - catch, 防止意外报错
            // 退出循环的条件： 
            // 一： AgentState.FINISHED
            // 二： maxStep
            try{
                for(int i = 0 ; i < maxStep && state != AgentState.FINISHED ; i++) {
                    
                    int stepNumber = i + 1;
                    // 记录当前的步骤
                    currentStep = stepNumber;

                    log.info("Executing Step {}/{}", stepNumber, maxStep);

                    // 调用 stepStream 进行单步推理，内部已发送 typed SSE 事件
                    String stepResult = stepStream();
                    String result = "Step " + stepNumber + ":" + stepResult;
                    results.add(result);
                }

                if(currentStep == maxStep && state != AgentState.FINISHED) {
                    state = AgentState.FINISHED;
                    results.add("Terminated: Reached max step: " + maxStep);
                    log.warn("Reached maxStep " + maxStep);
                    sendSseEvent("error", "Reached max step: " + maxStep);
                }

                // 流式与非流式的核心！
                // 发送完成事件（携带 finalAnswer），然后关闭 SSE
                String answer = getFinalAnswer();

                sendSseEvent("done", StrUtil.isNotBlank(answer)
                        ? answer
                        : "");

                // 结束
                emitter.complete();

            } catch(Exception e) {
                state = AgentState.ERROR;
                log.error("Error executing agent", e);
                sendSseEvent("error", "执行错误: " + e.getMessage());
                emitter.complete();
            }finally {
                persistMessages(conversationId);
                cleanUp();
            }
        });

        // 设置超时回调
        emitter.onTimeout(() -> {
            state = AgentState.ERROR;
            log.error("SseEmitter connect timeout!");
            try {
                // 因为是超时，所以要 completeWithError
                emitter.completeWithError(new AsyncRequestTimeoutException());
            } catch (Exception ignored) {
                // 可能已被其他逻辑关闭，静默忽略
            }
        });

        // 设置完成回调
        emitter.onCompletion(() -> {
            if(state == AgentState.RUNNING)
                state = AgentState.ERROR;
            log.info("SSE connect completed!");
        });

        // 一创建完 SseEmitter 就立刻返回
        return emitter;
    }


    // sendSseEvent() —— SSE 事件推送
    // 权限是 protected 子类能直接访问 ，而外部类无法访问
    // 子类可以重写
    // sendSseEvent 是给前端推送消息，这里不能关闭，而是在 step() 中进行关闭
    protected void sendSseEvent(String type, String data) {
        if(sseEmitter != null) {
            try{
                Map<String, String> event = new HashMap<>();
                event.put("type", type);
                event.put("data", data);
                String json = JSONUtil.toJsonStr(event);
                sseEmitter.send(SseEmitter.event().name("message").data(json, MediaType.APPLICATION_JSON));
                log.info("BaseAgent 的推送成功!");
            } catch(IOException e) {
                sseEmitter.completeWithError(e);
                log.error("BaseAgent 的推送失败: {}",e.getMessage());
            } catch(IllegalStateException e) {
                log.warn("SSE emitter 已完成，忽略后续推送: {}", e.getMessage());
            }
        }
    }

    // 由 abstract 修饰的方法，子类必须重写
    public abstract String step();

    // 有 abstract 修饰的方法，子类必须重写
    public abstract String stepStream();

    // 这里用 protected 是因为有的类可以重写，有的不用重写，如果用 abstract 修饰，代表必须重写
    protected void persistMessages(String conversationId) {
        // default no-op, override in subclasses to persist messages
    }

    protected void cleanUp(){
        if (cleaned) return;
        cleaned = true;
        state = AgentState.IDLE;
        messageList.clear();
        currentStep = 0;
        finalAnswer = null;
        sseEmitter = null;
    }



}
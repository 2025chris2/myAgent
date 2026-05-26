package com.tzl.llongagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.tzl.llongagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolCallAgent extends ReActAgent {

    // 注入所有的工具
    // final ToolCallback[] availableTools 只能保证引用不可变（不能让 availableTools 指向另一个新数组）
    // 但数组内部的元素仍然可以被修改
    private final ToolCallback[] availableTools;

    // 我们要在不同的函数里面使用，所以这里把作用域提升到提升为类的成员变量（实例字段）
    // 获取聊天响应，此聊天响应有要调用的工具
    private ChatResponse toolCallChatResponse;

    // 我们要在不同的函数里面使用，所以这里把作用域提示到全局了
    // 禁用 SpringAI 内置的工具调用机制，手动控制调用工具
    private ChatOptions chatOptions;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    public ToolCallAgent(ToolCallback[] toolCallbacks) {

        // 调用父类的构造函数，进行一些初始化
        super();

        this.availableTools = toolCallbacks;
        this.toolCallingManager = ToolCallingManager.builder().build();

        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和上下文
        this.chatOptions = ToolCallingChatOptions.builder()
                // 这里是禁止
                .internalToolExecutionEnabled(false)
                .toolCallbacks(availableTools)
                .build();
    }

    @Override
    public boolean think() {
        return thinkInternal(false);
    }

    @Override
    public boolean thinkStream() {
        return thinkInternal(true);
    }

    // thinkInter: 就是和 AI LLM进行对话，获取结果，围绕结果进行展开的
    /***
     * 思考阶段：调用大模型，决定是否需要调用工具
     * @param stream 是否为流式模式
     * @return 是否需要执行行动
     */
    // 权限是 private , 内部函数
    private boolean thinkInternal(boolean stream) {

    // ========== 1. 准备上下文和请求 ==========
    List<Message> messageList = getMessageList();
    Prompt prompt = new Prompt(messageList, chatOptions);

    // ========== 2. 调用大模型 & 处理响应 ==========
    try {
        ChatResponse chatResponse = getDeepseekChatClient()
                .prompt(prompt)
                .system(getSYSTEM_PROMPT())
                // 如果工具为空，则取消下面的注释
                // .toolCallbacks(availableTools)
                .call()
                .chatResponse();

        this.toolCallChatResponse = chatResponse;

        if (toolCallChatResponse == null) {
            throw new IllegalStateException("ChatClient returned null response");
        }

        // ========== 3. 提取 AI 回复并保存到历史上下文 ==========
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
        messageList.add(assistantMessage);

        // ========== 4. 解析回复内容 ==========
        String result = assistantMessage.getText();
        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();

        log.info("{}的思考: {}", getName(), result);
        log.info("{}选择了: {}个工具来使用", getName(), toolCalls.size());

        // ========== 5. 决策：是否需要调用工具 ==========
        if (toolCalls.isEmpty()) {
            // 5.1 无需工具：直接结束，给出最终答案
            // 对于非流式来说，最终答案是在 BaseAgent 中进行提取了
            this.finalAnswer = result;
            setState(AgentState.FINISHED);

            if (stream) {
                // 对于流式来说，答案必须由 sendSseEvent 来推送
                sendSseEvent("final_answer", finalAnswer);
            }
            return false;
        }

        // 5.2 需要工具：进入行动阶段，通知前端准备调用
        if (stream && StrUtil.isNotBlank(result)) {
            sendSseEvent("thinking", result);
        }

        String toolCallInfo = toolCalls.stream()
                .map(toolCall -> String.format("工具名称: %s, 参数: %s", toolCall.name(), toolCall.arguments()))
                .collect(Collectors.joining("\n"));
        log.info(toolCallInfo);

        if (stream) {
            sendSseEvent("tool_call", "准备调用工具\n" + toolCallInfo);
        }
        return true;

    // ========== 6. 异常处理 ==========
    } catch (Exception e) {
        log.error("{}的思考过程遇到了问题", getName(), e);

        this.finalAnswer = "AI 处理时遇到了问题: " + e.getMessage();
        messageList.add(new AssistantMessage("AI 处理时遇到了问题"));

        setState(AgentState.FINISHED);
        if (stream) {
            sendSseEvent("error", "思考过程出错");
        }
        return false;
    }
}

    @Override
    public String act() {
        return actInternal(false);
    }

    @Override
    public String actStream() {
        return actInternal(true);
    }

    // 根据 thinkInternal 的指令，进行操作，行动，并把结果给返回了
    /***
     * 执行阶段：调用工具并处理结果
     * @param stream 是否为流式模式
     * @return 执行结果
     */
    // 权限是 private ,内部函数
    private String actInternal(boolean stream) {

    // ========== 1. 执行工具调用 ==========
    Prompt prompt = new Prompt(getMessageList(), chatOptions);
    ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);

    // ========== 2. 校验 conversationHistory 合法性 ==========
    List<Message> conversationHistory = toolExecutionResult.conversationHistory();
    Object lastMessage = CollUtil.getLast(conversationHistory);

    // instanceof 这里不仅是判断了 toolResponseMessage　的类型，也把 toolResponseMessage　的引用指向了 lastMessage
    if (!(lastMessage instanceof ToolResponseMessage toolResponseMessage)) {
        log.error("conversationHistory 最后一条消息不是 ToolResponseMessage: {}", lastMessage);

        setState(AgentState.FINISHED);
        this.finalAnswer = "工具执行结果解析失败";

        if (stream) {
            sendSseEvent("error", "工具执行结果解析失败");
        }
        return this.finalAnswer;
    }

    // ========== 3. 更新消息上下文 ==========
    setMessageList(conversationHistory);

    // ========== 4. 提取核心数据（只做读取，不产生副作用） ==========
    boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
            .anyMatch(response -> response.name().equals("doTerminate"));

    String finalText = null;
    if (terminateToolCalled) {
        finalText = Optional.ofNullable(toolCallChatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(AssistantMessage::getText)
                .orElse(null);
    }

    // 在 instanceof 的判断中，不仅判断了类型还把 toolResponseMessage　指向了 lastMessage
    String results = toolResponseMessage.getResponses().stream()
            .map(response -> "工具 " + response.name() + " 返回的结果：" + response.responseData())
            .collect(Collectors.joining("\n"));

    // ========== 5. 更新内部状态（finalAnswer、State） ==========
    if (terminateToolCalled) {
        if (StrUtil.isNotBlank(finalText)) {
            this.finalAnswer = finalText;
        }
        setState(AgentState.FINISHED);
    }

    // ========== 6. 统一输出（SSE → 日志 → 返回） ==========
    if (stream) {
        if (terminateToolCalled && StrUtil.isNotBlank(this.finalAnswer)) {
            sendSseEvent("final_answer", this.finalAnswer);
        }
        sendSseEvent("tool_result", results);
    }

    log.info(results);
    return results;
}

    protected void cleanUp() {
        super.cleanUp();
        toolCallChatResponse = null;
    }
}

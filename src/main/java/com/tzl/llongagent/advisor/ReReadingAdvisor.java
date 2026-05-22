package com.tzl.llongagent.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

/***
 * 自定义 Re2 Advisor
 * 可提高LLM的推理能力
 */
public class ReReadingAdvisor implements BaseAdvisor {

    private static final String DEFAULT_RE2_ADVISE_TEMPLATE = """
            {re2_input_query}
			Read the question again: {re2_input_query}
            """;

    // 本类核心自己维护一个模板类，对于用户的输入进行增强
    private final String re2AdvisorTemplate;

    private int order = 0;

    public ReReadingAdvisor(){
        this(DEFAULT_RE2_ADVISE_TEMPLATE);
    }

    public ReReadingAdvisor(String re2AdvisorTemplate) {
        this.re2AdvisorTemplate = re2AdvisorTemplate;
    }

    @Override
    public int getOrder() {
        return order;
    }

    /**
     *执行请求前改写Prompt
     * @param chatClientRequest
     * @param advisorChain
     * @return
     */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String augmentedUserText = PromptTemplate.builder()
                .template(re2AdvisorTemplate)
                .variables(Map.of("re2_input_query", chatClientRequest.prompt().getUserMessage().getText()))
                // 生成 Template 实例
                .build()
                // 渲染模板，把占位符替换为实际值，得到最终字符串
                .render();

        // mutate是创建副本,进行对request操作,但不影响原来的request
        // 实际上创建了副本，也是一个 ChatClientReqeust类，有各种方法进行操作
        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
                .build();
    }

    /***
     * 因为这里只是对于　用户传入的信息　进行重读增强，所以 ChatClientResponse　类并没有作用
     */
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

}

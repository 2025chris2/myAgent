package com.tzl.llongagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/***
 * 自定义日志Advisor
 * 打印 info 级别日志、只输出单次用户提示词和 AI 回复的文本
 */
@Slf4j
public class llongLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    private final int order;

    public llongLoggerAdvisor() {
        this(0);
    }

    public llongLoggerAdvisor(int order) {
        this.order = order;
    }

    // 发送请求时打印
    protected void logRequest(ChatClientRequest request) {
        log.info("llong的觅途的request日志打印: {}", request.context());
    }

    // 接收响应时打印
    protected void logResponse(ChatClientResponse response) {
        log.info("llong的觅途的response日志打印: {}", response.chatResponse().getResult().getOutput().getText());
    }

    // ChatClientResponse,ChatClientRequest是Advisor链中流转的请求对象和响应对象
    // ChatClient 我们平时调用的时候，屏蔽了底层的调用

    // 非流式的核心方法
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // 打印流转在 Advisor 中的 ChatClientRequest
        logRequest(chatClientRequest);

        // 将请求交给责任链的下一环，指定了 ChatClientRequest 的流向
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        // 打印流出的响应
        logResponse(chatClientResponse);

        // 返回响应，给上层调用者返回，就是用户看到的结果
        return chatClientResponse;
    }

    // 流式的核心方法
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        // 打印流转在　Advisor　中的 ChatClientRequest
        logRequest(chatClientRequest);
        
        //　把请求交给责任链的下一环，制定了这个　ChatClientResponse　的去向
        Flux<ChatClientResponse> chatClientResponseFlux = streamAdvisorChain.nextStream(chatClientRequest);

        // 把结果进行聚集，然后打印，再返回,这里返回是给上层调用者，也就是我们用户看得结果
        return (new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponseFlux, this::logResponse));
    }

    @Override
    public String getName() {

        // 这里的 getName 是完整的名字，包括了包名加类名,例如最上面的包的导入
        // return this.getClass().getName();

        // 这个只返回纯类名
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return order;
    }

    public String toString() {
        return llongLoggerAdvisor.class.getSimpleName();
    }
}

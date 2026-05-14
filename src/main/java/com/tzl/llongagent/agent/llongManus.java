package com.tzl.llongagent.agent;

import com.tzl.llongagent.advisor.llongLoggerAdvisor;
import com.tzl.llongagent.service.MessageService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;

/***
 * 龙的 AI 超级智能体（拥有自主规划能力，可以直接使用）
 */
@Component
public class llongManus extends ToolCallAgent{

    private final MessageService messageService;

    public llongManus(ToolCallback[] allTools, DeepSeekChatModel deepseekChatModel,
                      MessageService messageService) {
        super(allTools);
        this.messageService = messageService;
        this.setName("llongManus");
        String SYSTEM_PROMPT = """
                You are EternalChristmas, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                When you generate files (PDF, Markdown, downloads, etc.), you MUST include a clickable download link in your final response.
                The download base URL is: http://8.147.64.213
                Format: [download filename](http://8.147.64.213/api/files/download?file=FILE_PATH)
                Use the exact FILE_PATH from the tool result (e.g. pdf/output.pdf, file/hello.md).
                Never use localhost or any other domain.
                CRITICAL: The download URL MUST include the file extension! If the file is a Markdown file, the URL must end with .md. If the file is a PDF, the URL must end with .pdf. For example: file/hello.md NOT file/hello, pdf/output.pdf NOT pdf/output.
                CRITICAL: The download URL MUST include the file extension! If the file is a Markdown file, the URL must end with .md. If the file is a PDF, the URL must end with .pdf. For example: file/hello.md NOT file/hello, pdf/output.pdf NOT pdf/output.
                """;
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If any file was generated, always output the download link using http://8.147.64.213/api/files/download?file= + the file path from the tool result.
                Format: [download filename](http://8.147.64.213/api/files/download?file=FILE_PATH)
                Never use localhost or any other domain. Use exactly http://8.147.64.213.
                CRITICAL: The download URL MUST include the file extension! If the file is a Markdown file, the URL must end with .md. If the file is a PDF, the URL must end with .pdf. For example: file/hello.md NOT file/hello, pdf/output.pdf NOT pdf/output.
                CRITICAL: The download URL MUST include the file extension! If the file is a Markdown file, the URL must end with .md. If the file is a PDF, the URL must end with .pdf. For example: file/hello.md NOT file/hello, pdf/output.pdf NOT pdf/output.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setSYSTEM_PROMPT(SYSTEM_PROMPT);
        this.setNEXT_STEP_PROMPT(NEXT_STEP_PROMPT);

        // 初始化聊天客户端
        // 这里工具的传入在ToolCallAgent里面实现的,这里不用再次传入allTools
        ChatClient deepseekChatClient = ChatClient.builder(deepseekChatModel)
                .defaultAdvisors(new llongLoggerAdvisor())
                .build();

        // 在上上层中，我们定义了 ChatClient，在这里我们传入
        this.setDeepseekChatClient(deepseekChatClient);

    }

    @Override
    protected void persistMessages(String conversationId) {
        List<Message> messages = getMessageList();
        if (!messages.isEmpty()) {
            messageService.replaceMessages(conversationId, messages);
        }
    }

}

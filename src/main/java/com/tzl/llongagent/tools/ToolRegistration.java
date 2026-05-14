package com.tzl.llongagent.tools;


import com.tzl.llongagent.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 * 集中的工具注册类
 */
@Slf4j
@Configuration
public class ToolRegistration {

    @Value("${tavily.apikey}")
    private String api_key;

    @Autowired
    private FileConstant fileConstant;

    @Autowired(required = false)
    private List<ToolCallbackProvider> toolCallbackProviders;

    @Bean
    @Lazy
    public ToolCallback[] allTools(){
        log.info("ToolRegistration 开始注册工具了!");

        String baseDir = fileConstant.getFileSaveDir();

        // 建立工具的对象实例
        FileOperationTool fileOperationTool = new FileOperationTool(baseDir);
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool(baseDir);
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool(baseDir);
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        WebSearchTool webSearchTool = new WebSearchTool(api_key);
        TerminateTool terminateTool = new TerminateTool();

        // ToolCallbacks: 把一系列的普通的对象，转化成工具，但是对象必须要有@Tool注解
        ToolCallback[] localTools = ToolCallbacks.from(
                fileOperationTool,
                pdfGenerationTool,
                resourceDownloadTool,
                terminateTool,
                webSearchTool,
                webScrapingTool,
                terminalOperationTool
        );

        // 合并 MCP 等外部工具提供者注册的工具
        List<ToolCallback> all = new ArrayList<>(Arrays.asList(localTools));
        if (toolCallbackProviders != null) {
            for (ToolCallbackProvider provider : toolCallbackProviders) {
                ToolCallback[] providerTools = provider.getToolCallbacks();
                if (providerTools != null && providerTools.length > 0) {
                    all.addAll(Arrays.asList(providerTools));
                    log.info("加载了 {} 个外部工具", providerTools.length);
                }
            }
        }
        log.info("总共注册了 {} 个工具", all.size());
        return all.toArray(new ToolCallback[0]);
    }

}

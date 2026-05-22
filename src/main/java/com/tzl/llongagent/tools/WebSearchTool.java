package com.tzl.llongagent.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import java.util.List;
import java.util.stream.Collectors;

/***
 * 使用 Tavily 进行搜索，对 AI 友好
 * 本类围绕 Tavily 进行构建的
 */
@Slf4j
public class WebSearchTool {

    private final String TAVILY_API_URL;

    private final String API_KEY;

    public WebSearchTool(String key) {
        TAVILY_API_URL = "https://api.tavily.com/search";
        API_KEY = key;
    }


    @Tool(description = "Search for information from Tavily")
    public String doWebSearch(
            @ToolParam(description = "Search query keyword") String query
    ) {

        log.info("WebSearchTool 开始全网搜索了!");

        // 1. 构造 POST 请求体（Tavily 要求 JSON body）
        JSONObject Obody = JSONUtil.createObj()
                .set("api_key", API_KEY)
                .set("query", query)
                .set("search_depth", "basic")
                .set("max_results", 5)
                .set("include_answer", true)
                .set("include_raw_content", false)
                .set("include_images", false);

            // 这里网络请求和响应都是 Hutool 中的
        try(HttpResponse resp = HttpRequest.post(TAVILY_API_URL)
                .header("Content-Type", "application/json")
                // .header("Authorization", "Bearer " + API_KEY)
                .body(Obody.toString())
                .timeout(30000)
                .execute();) {
            if(!resp.isOk())
                throw new RuntimeException("Tavily HTTP" + resp.getStatus());

            // 这里的 resp 类型是一个 JSON 被""包裹的 String 类型
            // 使用 parseObj ,可以转换与解析格式，这里是转换，把 String 转换为 JSON ，就是去掉""
            JSONObject root = JSONUtil.parseObj(resp.body());
            JSONArray results = root.getJSONArray("results");

            // 截取前5条
            int limit = Math.min(results.size(), 5);

            // 所有类的父类
            List<Object> topResults = results.subList(0, limit);

            // 格式化为 AI 友好的文本
            return topResults.stream().map(obj -> {
                // 强转
                JSONObject item = (JSONObject) obj;
                return String.format(
                        "标题: %s\n链接:%s\n摘要: %s\n",
                        item.getStr("title"),
                        item.getStr("url"),
                        item.getStr("content")
                );
            }).collect(Collectors.joining("\n---\n"));
        } catch(Exception e) {
            // 抛出异常让上层处理，而不是返回错误字符串
            throw new RuntimeException("Tavily 搜索失败: " + e.getMessage());
        }
    }
}

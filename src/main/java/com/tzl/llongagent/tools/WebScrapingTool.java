package com.tzl.llongagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

@Slf4j
public class WebScrapingTool {

    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(
        @ToolParam(description = "URL of the web page to scrape") String url
    ) {
        try{
            log.info("WebScrapingTool 开始抓取页面了!");

            // 抓取并解析 html
            Document document = Jsoup.connect(url)
            //      伪装浏览器，防止被拒绝
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();

            // 记录文本的长度
            String text = document.text();

            // 限制我们抓取的大小，最大不要超过8000
            int maxLength = 8000;
            if(text.length() > 8000) {
                text = text.substring(0, maxLength) + "\n...[Content truncated]";
            }
            return text;
        } catch(Exception e) {
            return "Error occurred while scraping the web page " + e.getMessage();
        }
    }

}

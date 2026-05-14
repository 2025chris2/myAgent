package com.tzl.llongimagesearchmcpserver.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImageSearchTool {

    @Value("${pexels.api-key}")
    private String apiKey;

    private static final String API_URL = "https://api.pexels.com/v1/search";

    @Tool(description = "search image from web")
    public String searchImage(
            @ToolParam(description = "search query keyword") String query) {
        try {
            List<String> imageUrls = searchMediumImage(query);
            if (imageUrls.isEmpty()) {
                return "No images found.";
            }
            return String.join(",", imageUrls);
        } catch (Exception e) {
            log.error("Image search failed for query: {}", query, e);
            return "Image search failed.";
        }
    }

    private List<String> searchMediumImage(String query) {
        Map<String, String> headers = Map.of("Authorization", apiKey);
        HttpResponse httpResponse = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form("query", query)
                .execute();

        if (httpResponse.getStatus() != 200) {
            throw new RuntimeException("HTTP error status: " + httpResponse.getStatus());
        }

        JSONObject json = JSONUtil.parseObj(httpResponse.body());
        JSONArray photos = json.getJSONArray("photos");
        if (photos == null || photos.isEmpty()) {
            return Collections.emptyList();
        }

        return photos.stream()
                .map(obj -> ((JSONObject) obj).getByPath("src.medium", String.class))
                .map(Object::toString)
                .filter(src -> src != null && !src.isEmpty())
                .collect(Collectors.toList());
    }
}

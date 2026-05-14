package com.tzl.llongagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;


/***
 * 资源下载工具
 */
@Slf4j
public class ResourceDownloadTool {

    private final String FILE_DIR;

    public ResourceDownloadTool(String baseDir) {
        this.FILE_DIR = Paths.get(baseDir, "download").toString();
    }

    @Tool(description = "Download a resource from a given URL")
    public String downloadResource(
            @ToolParam(description = "URL of the resource to download") String url,
            @ToolParam(description = "Name of file to save the download resource") String fileName
    ) {
        if (!validFileName(fileName))
            return "Invalid filename!";

        String filePath = Paths.get(FILE_DIR, fileName).toString();
        try{
            log.info("ResourceDownloadTool 开始下载源码了！");

            FileUtil.mkdir(FILE_DIR);
            HttpUtil.downloadFile(url, new File(filePath));
            String relativePath = "download/" + fileName;
            String encodedPath = URLEncoder.encode(relativePath, StandardCharsets.UTF_8);
            String downloadUrl = "/api/files/download?file=" + encodedPath;
            return "Resource downloaded successfully! Download: " + downloadUrl
                    + " (saved to: " + filePath + ")";
        } catch (Exception e) {
            return "Error downloading resource " +e.getMessage();
        }
    }

    private boolean validFileName(String fileName) {
        return fileName != null && !fileName.isEmpty()
                && !fileName.contains("..")
                && !fileName.contains("/")
                && !fileName.contains("\\");
    }

}

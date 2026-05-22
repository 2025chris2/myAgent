package com.tzl.llongagent.tools;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/***
 * 文件操作工具类(提供文件读写功能)
 */
@Slf4j
public class FileOperationTool {

    // 自己维护一个 FILE_DIR ，文件操作类的核心
    private final String FILE_DIR;

    // 进行拼接，此次指定了路径
    public FileOperationTool(String baseDir) {

        // 这里是 FileOperationTool 类，所以我们指定为 "file" 路径即可
        this.FILE_DIR = Paths.get(baseDir, "file").toString();
    }

    @Tool(description = "Read content from file")
    public String readFile(@ToolParam(description = "Name of the file to read") String fileName) {
        // 判断文件名是否合法
        if(!invalidFileName(fileName))
            return "Invalid filename!";

        // 拼接要读取的路径，具体到文件名
        String filePath = Paths.get(FILE_DIR, fileName).toString();

        try{
            log.info("FileOperationTool 开始读取文件了!");

           // 通过 hutool 工具类读取内容
           // 方法的解释： 按 Utf8 的编码进行读文件，按 String 的格式返回文件
           // 这里返回的是 String，所以我们能直接 return
           return FileUtil.readUtf8String(filePath);
        } catch(Exception e) {
            return "Error read file" + e.getMessage();
        }
    }

    @Tool(description = "Write content to a file")
    public String writeFile(@ToolParam(description = "Name of the file to write") String fileName,
                            @ToolParam(description = "Content to write to the file")String content) {
        // 判断文件名是否合法
        if(!invalidFileName(fileName))
            return "Invalid filename!";

        String filePath = Paths.get(FILE_DIR, fileName).toString();

        try{
            log.info("FileOperationTool 开始写文件了");

            // 如果文件夹存在,则没有什么影响,如果文件夹不存在,则创建新文件夹
            FileUtil.mkdir(FILE_DIR);
            FileUtil.writeUtf8String(content, filePath);

            // 拼接 相对路径
            String relativePath = "file/" + fileName;
            
            // 对拼接的路径进行解码 UTF-8 的格式
            String encodedPath = URLEncoder.encode(relativePath, StandardCharsets.UTF_8);

            // 下载路径
            String downloadUrl = "/api/files/download?file=" + encodedPath;
            return "File written successfully! Download: " + downloadUrl
                    + " (saved to: " + filePath + ")";

        } catch(Exception e) {
            return "Error writing to file " + e.getMessage();
        }

    }

    private boolean invalidFileName(String fileName){
        return fileName != null && !fileName.isEmpty()
                && !fileName.contains("..")
                && !fileName.contains("/")
                && !fileName.contains("\\");
    }

}

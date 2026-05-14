package com.tzl.llongagent.controller;

import com.tzl.llongagent.constant.FileConstant;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileDownloadController {

    private final FileConstant fileConstant;

    public FileDownloadController(FileConstant fileConstant) {
        this.fileConstant = fileConstant;
    }

    private static final Map<String, MediaType> MIME_MAP = new HashMap<>();
    static {
        MIME_MAP.put("pdf", MediaType.APPLICATION_PDF);
        MIME_MAP.put("md", MediaType.parseMediaType("text/markdown"));
        MIME_MAP.put("txt", MediaType.TEXT_PLAIN);
        MIME_MAP.put("html", MediaType.TEXT_HTML);
        MIME_MAP.put("htm", MediaType.TEXT_HTML);
        MIME_MAP.put("css", MediaType.parseMediaType("text/css"));
        MIME_MAP.put("js", MediaType.parseMediaType("application/javascript"));
        MIME_MAP.put("json", MediaType.APPLICATION_JSON);
        MIME_MAP.put("xml", MediaType.APPLICATION_XML);
        MIME_MAP.put("png", MediaType.IMAGE_PNG);
        MIME_MAP.put("jpg", MediaType.IMAGE_JPEG);
        MIME_MAP.put("jpeg", MediaType.IMAGE_JPEG);
        MIME_MAP.put("gif", MediaType.IMAGE_GIF);
        MIME_MAP.put("svg", MediaType.parseMediaType("image/svg+xml"));
        MIME_MAP.put("zip", MediaType.parseMediaType("application/zip"));
        MIME_MAP.put("docx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        MIME_MAP.put("xlsx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/download")
    public void download(@RequestParam("file") String file,
                         @RequestHeader(value = "X-Auto-Download", required = false) String autoDownload,
                         HttpServletResponse response) throws IOException {
        // 去除前导 / 或 \，确保 file 参数始终为相对路径，防止 Paths.get 将绝对路径当作
        // 根路径而忽略 baseDir（例如 /home/tzl/my-project/34 会被解析为绝对路径而非 baseDir 下的子路径）
        String normalizedFile = file;
        while (normalizedFile.startsWith("/") || normalizedFile.startsWith("\\")) {
            normalizedFile = normalizedFile.substring(1);
        }
        if (!normalizedFile.equals(file)) {
            log.warn("文件路径包含前导斜杠，已规范化: {} -> {}", file, normalizedFile);
        }

        String baseDir = fileConstant.getFileSaveDir();
        File target = Paths.get(baseDir, normalizedFile).toFile();

        if (!target.exists() || !target.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("File not found: " + normalizedFile);
            return;
        }

        String canonical;
        try {
            canonical = target.getCanonicalPath();
            String base = new File(baseDir).getCanonicalPath();
            if (!canonical.startsWith(base)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                response.getWriter().write("Invalid file path");
                return;
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("Invalid file path");
            return;
        }

        String fileName = target.getName();

        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            ext = fileName.substring(dot + 1).toLowerCase();
        }
        MediaType mediaType = MIME_MAP.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM);

        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String asciiName = fileName.replaceAll("[^\\x00-\\x7F]", "_");

        response.setContentType(mediaType.toString());
        response.setContentLengthLong(target.length());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''" + encodedName);

        try (FileInputStream fis = new FileInputStream(target);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }

        if ("true".equals(autoDownload)) {
            if (target.delete()) {
                log.info("文件已自动清理: {}", target.getAbsolutePath());
            } else {
                log.warn("文件清理失败: {}", target.getAbsolutePath());
            }
        }
    }
}

package com.tzl.llongagent.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileConstant {

    @Value("${file.save-dir}")
    private String fileSaveDir;

    public String getFileSaveDir() {
        return fileSaveDir;
    }

    public String getConversationDir(String conversationId) {
        return fileSaveDir + "/" + conversationId;
    }
    
}

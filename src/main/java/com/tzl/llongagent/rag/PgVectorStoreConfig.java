package com.tzl.llongagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Configuration
public class PgVectorStoreConfig {

    @Resource
    private PlanAppDocumentLoader documentLoader;

    @Resource
    private PlanAppTextSplitter textSplitter;

    @Bean
    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {

        // 返回的 VectorStore 的实现类
        PgVectorStore pgVectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .dimensions(1024)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("vector_store")
                .maxDocumentBatchSize(10)
                .build();

        // 检查向量表是否已有数据，避免每次启动重复向量化
        Integer rowCount = 0;
        try {
            rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class);
        } catch (Exception e) {
            log.info("向量表尚未创建，将在首次添加文档时自动创建");
        }
        if(rowCount > 0){
            log.info("向量表已经有 {} 条数据, 跳过文档加载和向量化", rowCount);
            return pgVectorStore;
        }

        List<Document> documents = documentLoader.loadMarkDown();
        log.info("文档加载完成，块数: {}", documents.size());

        // 过滤空内容文档，避免向量化失败
        List<Document> validDocuments = documents.stream()
                .filter(doc -> doc.getText() != null && !doc.getText().trim().isEmpty())
                .toList();
        int emptyCount = documents.size() - validDocuments.size();
        if (emptyCount > 0) {
            log.warn("过滤掉 {} 个空内容文档块", emptyCount);
        }

        List<Document> splitDocuments = textSplitter.splitDocuments(validDocuments);
        log.info("文档分割完成，总块数: {}", splitDocuments.size());

        // 每次进行向量化的文档大小
        int batchSize = 10;
        // 最多的重试次数
        int maxRetries = 3;
        // 成功向量化的个数
        int successCount = 0;

        // 开始向量化
        for (int i = 0; i < splitDocuments.size(); i += batchSize) {
            // 记录 当前批次 的结束位置
            int end = Math.min(i + batchSize, splitDocuments.size());
            // 取出 当前批次 要进行向量化的文档
            List<Document> batchDocuments = splitDocuments.subList(i, end);
            // 记录第几次进行向量化
            int batchIndex = i / batchSize + 1;

            // 进行带有重试的向量化
            if (embedBatchWithRetry(pgVectorStore, batchDocuments, maxRetries, batchIndex)) {
                // 记录成功向量化的文档个数
                successCount += batchDocuments.size();
            }

            // 批次间延迟，避免触发 embedding API 限流
            if (end < splitDocuments.size()) {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    // 标记当前线程被打断，方便抓报错
                    Thread.currentThread().interrupt();
                    log.warn("批次间等待被中断，停止向量化");
                    break;
                }
            }
        }

        log.info("向量化完成: {}/{} 个文档块成功嵌入", successCount, splitDocuments.size());
        return pgVectorStore;

    }

    private boolean embedBatchWithRetry(PgVectorStore store, List<Document> batchDocuments, int maxRetries, int batchIndex) {
        // 开始向量化，从第一次到第 maxRetries 次进行尝试
        for (int attempt = 1; attempt <= maxRetries; attempt++) {

            // store.add() 可能因网络/API/数据库问题抛异常，需要捕获并重试
            try {
                store.add(batchDocuments);
                log.info("批次 {} 嵌入成功 ({} 个文档)", batchIndex, batchDocuments.size());
                return true;
            } catch (Exception e) {
                log.error("批次 {} 嵌入失败 (第 {} 次尝试): {}", batchIndex, attempt, e.getMessage());
                if (attempt < maxRetries) {

                    // 指数退避：等待 2^attempt 秒，乘以 1000 转成毫秒
                    long waitMs = (long) Math.pow(2, attempt) * 1000L;
                    log.info("等待 {}ms 后重试...", waitMs);
                    try {
                        Thread.sleep(waitMs);
                    } catch (InterruptedException ie) {
                        // 重新设置中断标志，不丢失中断状态
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        log.error("批次 {} 重试 {} 次后仍然失败，跳过该批次", batchIndex, maxRetries);
        return false;
    }

}

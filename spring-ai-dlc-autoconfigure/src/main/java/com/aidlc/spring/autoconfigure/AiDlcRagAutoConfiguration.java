package com.aidlc.spring.autoconfigure;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.aidlc.spring.core.rag.ingest.IngestionPipeline;
import com.aidlc.spring.core.rag.query.RagPipeline;

/**
 * Autoconfiguration for the AI-DLC RAG facades ({@link IngestionPipeline}, {@link RagPipeline}),
 * activated when a {@link VectorStore} and {@link ChatClient.Builder} are available.
 */
@AutoConfiguration(after = AiDlcChatAutoConfiguration.class)
@ConditionalOnBean({VectorStore.class, ChatClient.Builder.class})
@EnableConfigurationProperties(AiDlcProperties.class)
public class AiDlcRagAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IngestionPipeline ingestionPipeline(VectorStore vectorStore) {
        return new IngestionPipeline(vectorStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public RagPipeline ragPipeline(
            ChatClient.Builder chatClientBuilder, VectorStore vectorStore, AiDlcProperties properties) {
        AiDlcProperties.Rag rag = properties.rag();
        RagPipeline.Builder builder = RagPipeline.builder(chatClientBuilder, vectorStore)
                .topK(rag.topK())
                .similarityThreshold(rag.similarityThreshold());
        if (rag.queryRewrite()) {
            builder.enableQueryRewrite();
        }
        return builder.build();
    }
}

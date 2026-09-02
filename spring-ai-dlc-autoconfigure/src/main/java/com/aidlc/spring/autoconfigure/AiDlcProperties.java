package com.aidlc.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Top-level {@code ai.dlc.*} configuration properties for the AI-DLC starter.
 */
@ConfigurationProperties("ai.dlc")
public record AiDlcProperties(
        @NestedConfigurationProperty @DefaultValue Rag rag,
        @NestedConfigurationProperty @DefaultValue Agent agent) {

    /**
     * RAG ingestion and query tuning.
     *
     * <p>Note: {@code chunkOverlap} is currently a no-op. Spring AI 2.0's
     * {@code TokenTextSplitter} does not expose an overlap knob, so the value is accepted
     * for forward compatibility but not applied during chunking (see {@code ChunkingSpec}).
     */
    public record Rag(
            @DefaultValue("800") int chunkSize,
            @DefaultValue("100") int chunkOverlap,
            @DefaultValue("4") int topK,
            @DefaultValue("0.0") double similarityThreshold,
            @DefaultValue("false") boolean queryRewrite,
            @NestedConfigurationProperty @DefaultValue IngestOnStartup ingestOnStartup) {

        /** Startup ingestion of a classpath document set. */
        public record IngestOnStartup(
                @DefaultValue("false") boolean enabled,
                @DefaultValue("rag-corpus/**/*.md") String locationPattern) {
        }
    }

    /** Agent execution defaults. */
    public record Agent(@DefaultValue("5") int maxToolIterations) {
    }
}

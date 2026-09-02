package com.aidlc.spring.core.rag.ingest;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;

/**
 * Chunking parameters used to build a {@link TokenTextSplitter}.
 *
 * <p>Spring AI 2.0's {@code TokenTextSplitter} does not expose a direct "overlap"
 * knob; {@code chunkOverlap} is retained here for API stability with the plan and
 * future splitter implementations, but is not currently passed to the splitter.
 */
public record ChunkingSpec(int chunkSize, int chunkOverlap) {

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_CHUNK_OVERLAP = 100;

    public static ChunkingSpec defaults() {
        return new ChunkingSpec(DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }

    public TokenTextSplitter toSplitter() {
        return TokenTextSplitter.builder().withChunkSize(chunkSize).build();
    }
}

package com.aidlc.spring.core.rag.query;

/**
 * A single retrieved-document reference backing a {@link RagAnswer}.
 */
public record Citation(String source, String snippet, Double score) {
}

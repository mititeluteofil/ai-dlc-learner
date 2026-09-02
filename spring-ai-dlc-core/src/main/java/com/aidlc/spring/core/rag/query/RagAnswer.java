package com.aidlc.spring.core.rag.query;

import java.util.List;

/**
 * Result of {@link RagPipeline#ask(String)}: the model's answer plus the documents
 * that were retrieved to ground it.
 */
public record RagAnswer(String answer, List<Citation> citations) {
}

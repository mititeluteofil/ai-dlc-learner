package com.aidlc.spring.core.rag.ingest;

import java.time.Duration;
import java.util.List;

/**
 * Outcome of a single {@link IngestionPipeline} run.
 */
public record IngestionReport(int documentsRead, int chunksWritten, Duration elapsed, List<String> errors) {

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}

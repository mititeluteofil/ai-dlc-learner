package com.aidlc.spring.core.rag.ingest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChunkingSpecTest {

    @Test
    void defaultsAre800And100() {
        ChunkingSpec defaults = ChunkingSpec.defaults();

        assertEquals(800, defaults.chunkSize());
        assertEquals(100, defaults.chunkOverlap());
    }

    @Test
    void toSplitterBuildsNonNullSplitter() {
        ChunkingSpec spec = new ChunkingSpec(400, 50);

        assertNotNull(spec.toSplitter());
    }
}

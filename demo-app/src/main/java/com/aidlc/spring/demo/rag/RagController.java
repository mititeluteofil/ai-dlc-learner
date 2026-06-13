package com.aidlc.spring.demo.rag;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.aidlc.spring.core.rag.ingest.ChunkingSpec;
import com.aidlc.spring.core.rag.ingest.DocumentSource;
import com.aidlc.spring.core.rag.ingest.IngestionPipeline;
import com.aidlc.spring.core.rag.ingest.IngestionReport;
import com.aidlc.spring.core.rag.query.RagAnswer;
import com.aidlc.spring.core.rag.query.RagPipeline;

/**
 * Docs-RAG endpoint backed by the {@link RagPipeline} and {@link IngestionPipeline} beans
 * provided by {@code spring-ai-dlc-starter}'s autoconfiguration.
 */
@RestController
public class RagController {

    private final RagPipeline ragPipeline;
    private final IngestionPipeline ingestionPipeline;

    public RagController(RagPipeline ragPipeline, IngestionPipeline ingestionPipeline) {
        this.ragPipeline = ragPipeline;
        this.ingestionPipeline = ingestionPipeline;
    }

    @PostMapping("/api/rag/ask")
    public RagAnswer ask(@RequestBody AskRequest request) {
        return ragPipeline.ask(request.question());
    }

    @PostMapping("/api/rag/ingest")
    public IngestionReport ingest() {
        return ingestionPipeline.from(DocumentSource.classpath("rag-corpus/**/*.md"))
                .withChunking(ChunkingSpec.defaults())
                .run();
    }

    public record AskRequest(String question) {
    }
}

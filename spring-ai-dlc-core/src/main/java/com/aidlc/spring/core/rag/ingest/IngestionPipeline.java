package com.aidlc.spring.core.rag.ingest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Collapses Spring AI's reader -&gt; splitter -&gt; vector store flow into a fluent
 * {@code from(source).withChunking(spec).run()} call.
 *
 * <p>Chunks are written with a deterministic id (SHA-256 of {@code source identifier
 * + chunk index}) so re-running ingestion against the same source upserts rather than
 * duplicating entries in the vector store.
 */
public class IngestionPipeline {

    private final VectorStore vectorStore;

    public IngestionPipeline(VectorStore vectorStore) {
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
    }

    public Run from(DocumentSource source) {
        return new Run(Objects.requireNonNull(source, "source must not be null"));
    }

    /** Mutable builder for a single ingestion run. */
    public final class Run {

        private final DocumentSource source;
        private ChunkingSpec chunkingSpec = ChunkingSpec.defaults();

        private Run(DocumentSource source) {
            this.source = source;
        }

        public Run withChunking(ChunkingSpec chunkingSpec) {
            this.chunkingSpec = Objects.requireNonNull(chunkingSpec, "chunkingSpec must not be null");
            return this;
        }

        public IngestionReport run() {
            Instant start = Instant.now();
            List<String> errors = new ArrayList<>();
            int documentsRead = 0;
            int chunksWritten = 0;

            try {
                List<Document> documents = source.read();
                documentsRead = documents.size();

                TokenTextSplitter splitter = chunkingSpec.toSplitter();
                List<Document> chunks = splitter.apply(documents);

                MessageDigest digest = sha256();
                List<Document> identified = new ArrayList<>(chunks.size());
                for (int i = 0; i < chunks.size(); i++) {
                    identified.add(withDeterministicId(chunks.get(i), digest, source.identifier(), i));
                }

                if (!identified.isEmpty()) {
                    vectorStore.add(identified);
                }
                chunksWritten = identified.size();
            }
            catch (Exception e) {
                errors.add(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }

            Duration elapsed = Duration.between(start, Instant.now());
            return new IngestionReport(documentsRead, chunksWritten, elapsed, List.copyOf(errors));
        }
    }

    static Document withDeterministicId(Document document, MessageDigest digest, String sourceIdentifier, int chunkIndex) {
        String id = deterministicId(digest, sourceIdentifier, chunkIndex);
        return document.mutate().id(id).build();
    }

    static String deterministicId(String sourceIdentifier, int chunkIndex) {
        return deterministicId(sha256(), sourceIdentifier, chunkIndex);
    }

    private static String deterministicId(MessageDigest digest, String sourceIdentifier, int chunkIndex) {
        // MessageDigest.digest() resets the instance, so a single digest can be reused across chunks.
        byte[] hash = digest.digest((sourceIdentifier + "#" + chunkIndex).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

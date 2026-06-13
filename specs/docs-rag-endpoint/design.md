# docs-rag-endpoint — Design

## Approach

`demo-app` depends on `spring-ai-dlc-starter`, which pulls in `spring-ai-dlc-core` (the
`RagPipeline`/`IngestionPipeline` facades) and `spring-ai-dlc-autoconfigure`
(`AiDlcRagAutoConfiguration`, which wires those facades around the autoconfigured Spring AI
`VectorStore`/`ChatClient.Builder`, plus `StartupIngestionRunner`). `demo-app` only adds a
thin `RagController` and a `rag-corpus/` resource directory containing the docs to index.
Embeddings use Ollama `nomic-embed-text` (768 dims) into pgvector, configured via
`application.yml`.

## Components

| Component | Module | Package | Responsibility |
|---|---|---|---|
| `RagController` | demo-app | `com.aidlc.spring.demo.rag` | REST endpoints for ask/ingest |
| `RagPipeline` | spring-ai-dlc-core | `com.aidlc.spring.core.rag.query` | `ask(question)` -> `RagAnswer` (answer + citations) |
| `IngestionPipeline` | spring-ai-dlc-core | `com.aidlc.spring.core.rag.ingest` | `from(source).withChunking(spec).run()` -> `IngestionReport` |
| `DocumentSource` | spring-ai-dlc-core | `com.aidlc.spring.core.rag.ingest` | `.classpath("rag-corpus/**/*.md")` factory |
| `ChunkingSpec` | spring-ai-dlc-core | `com.aidlc.spring.core.rag.ingest` | `.defaults()` = 800/100 -> `TokenTextSplitter` |
| `AiDlcRagAutoConfiguration` | spring-ai-dlc-autoconfigure | `com.aidlc.spring.autoconfigure` | Provides `RagPipeline`/`IngestionPipeline` beans, conditional on `VectorStore` + `ChatClient.Builder` |
| `StartupIngestionRunner` | spring-ai-dlc-autoconfigure | `com.aidlc.spring.autoconfigure` | `ApplicationRunner`, conditional on `ai.dlc.rag.ingest-on-startup.enabled` |
| `AiDlcProperties` | spring-ai-dlc-autoconfigure | `com.aidlc.spring.autoconfigure` | `ai.dlc.rag.*` (chunk-size, chunk-overlap, top-k, similarity-threshold, query-rewrite, ingest-on-startup.*) |

## API & Data

- `POST /api/rag/ask`
  - request: `record AskRequest(String question)`
  - response: `RagAnswer(String answer, List<Citation> citations)` where
    `Citation(String source, String snippet, Double score)`
- `POST /api/rag/ingest`
  - request: none (body ignored)
  - response: `IngestionReport(int documentsRead, int chunksWritten, Duration elapsed, List<String> errors)`
- Config (already defined in `AiDlcProperties`, used as-is, no new properties):
  - `ai.dlc.rag.chunk-size` (default 800), `ai.dlc.rag.chunk-overlap` (default 100)
  - `ai.dlc.rag.top-k` (default 4), `ai.dlc.rag.similarity-threshold` (default 0.0)
  - `ai.dlc.rag.ingest-on-startup.enabled` (demo-app sets `true`),
    `ai.dlc.rag.ingest-on-startup.location-pattern` (demo-app sets `classpath:rag-corpus/**/*.md`)
- Embedding/vector store config (`application.yml`, raw Spring AI properties):
  - `spring.ai.ollama.embedding.model=nomic-embed-text`
  - `spring.ai.vectorstore.pgvector.dimensions=768`, `initialize-schema=true`

## Files to Create/Modify

- `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/ingest/IngestionPipeline.java` (new)
- `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/ingest/DocumentSource.java` (new)
- `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/ingest/ChunkingSpec.java` (new)
- `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/ingest/IngestionReport.java` (new)
- `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/query/RagPipeline.java` (new)
- `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/query/RagAnswer.java` (new)
- `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/query/Citation.java` (new)
- `spring-ai-dlc-autoconfigure/src/main/java/com/aidlc/spring/autoconfigure/AiDlcProperties.java` (new)
- `spring-ai-dlc-autoconfigure/src/main/java/com/aidlc/spring/autoconfigure/AiDlcRagAutoConfiguration.java` (new)
- `spring-ai-dlc-autoconfigure/src/main/java/com/aidlc/spring/autoconfigure/StartupIngestionRunner.java` (new)
- `demo-app/src/main/java/com/aidlc/spring/demo/rag/RagController.java` (new)
- `demo-app/src/main/resources/rag-corpus/implementation-plan.md` (new)
- `demo-app/src/main/resources/rag-corpus/readme.md` (new)
- `demo-app/src/main/resources/application.yml` (modify — added `ai.dlc.rag.ingest-on-startup`, pgvector + ollama embedding config)

## Risks

- Embedding dimension (768, `nomic-embed-text`) must stay consistent with the pgvector table
  schema; switching the `openai`/`anthropic` profiles to different embedding models requires
  a distinct table name or `dimensions` value to avoid a dimension-mismatch error on insert.
- First-run ingestion depends on Ollama having pulled `nomic-embed-text` and `qwen3:4b`
  (`pull-model-strategy=when_missing`), which can make the first `/api/rag/ask` slow.
- Re-running `/api/rag/ingest` relies on deterministic document IDs (hash of source + chunk
  index) in `IngestionPipeline` to upsert rather than duplicate — if that hashing changes,
  re-ingestion could create duplicate chunks.

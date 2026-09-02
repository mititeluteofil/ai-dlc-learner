# docs-rag-endpoint — Tasks

Each task is <= 1 hour. Format:

`- [x] T<n>: <verb phrase> — files: <comma-separated paths> — verify: <command>`

All tasks below are marked done — this feature was built during Phase 6 of the
implementation plan, before the spec-driven scaffold existed. This file documents it
retroactively as a worked example for the spec-driven workflow.

## Core: ingestion facades

- [x] T1: Add `DocumentSource` classpath/path/url/text factories and `ChunkingSpec` (defaults 800/100) — files: `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/ingest/DocumentSource.java`, `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/ingest/ChunkingSpec.java` — verify: `./gradlew :spring-ai-dlc-core:test`
- [x] T2: Add `IngestionReport` record and `IngestionPipeline` fluent `from(source).withChunking(spec).run()` with deterministic chunk IDs — files: `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/ingest/IngestionReport.java`, `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/ingest/IngestionPipeline.java` — verify: `./gradlew :spring-ai-dlc-core:test`

## Core: query facade

- [x] T3: Add `Citation` and `RagAnswer` records and `RagPipeline.ask(question)` with topK/similarityThreshold/promptTemplate/filterExpression builder options — files: `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/query/Citation.java`, `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/query/RagAnswer.java`, `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/rag/query/RagPipeline.java` — verify: `./gradlew :spring-ai-dlc-core:test`

## Autoconfigure

- [x] T4: Add `AiDlcProperties` (`ai.dlc.rag.*`, `ai.dlc.agent.*`) — files: `spring-ai-dlc-autoconfigure/src/main/java/com/aidlc/spring/autoconfigure/AiDlcProperties.java` — verify: `./gradlew :spring-ai-dlc-autoconfigure:test`
- [x] T5: Add `AiDlcRagAutoConfiguration` providing `RagPipeline`/`IngestionPipeline` beans, conditional on `VectorStore` + `ChatClient.Builder`, `@ConditionalOnMissingBean` — files: `spring-ai-dlc-autoconfigure/src/main/java/com/aidlc/spring/autoconfigure/AiDlcRagAutoConfiguration.java` — verify: `./gradlew :spring-ai-dlc-autoconfigure:test`
- [x] T6: Add `StartupIngestionRunner` `ApplicationRunner`, conditional on `ai.dlc.rag.ingest-on-startup.enabled` — files: `spring-ai-dlc-autoconfigure/src/main/java/com/aidlc/spring/autoconfigure/StartupIngestionRunner.java` — verify: `./gradlew :spring-ai-dlc-autoconfigure:test`

## Demo wiring

- [x] T7: Add `RagController` with `POST /api/rag/ask` and `POST /api/rag/ingest` — files: `demo-app/src/main/java/com/aidlc/spring/demo/rag/RagController.java` — verify: `./gradlew :demo-app:build`
- [x] T8: Add `rag-corpus/` markdown docs and configure `application.yml` (pgvector dims=768, ollama embedding model, `ai.dlc.rag.ingest-on-startup.*`) — files: `demo-app/src/main/resources/rag-corpus/implementation-plan.md`, `demo-app/src/main/resources/rag-corpus/readme.md`, `demo-app/src/main/resources/application.yml` — verify: `./gradlew :demo-app:build`

## Tests

- [x] T9: Unit tests for ingestion/query facades with mocked `VectorStore`/`ChatClient` — files: `spring-ai-dlc-core/src/test/java/com/aidlc/spring/core/rag/**` — verify: `./gradlew :spring-ai-dlc-core:test`
- [x] T10: `ApplicationContextRunner` tests for `AiDlcRagAutoConfiguration` (beans conditional, overrides respected) — files: `spring-ai-dlc-autoconfigure/src/test/java/com/aidlc/spring/autoconfigure/**` — verify: `./gradlew :spring-ai-dlc-autoconfigure:test`

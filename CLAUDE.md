# CLAUDE.md

`ai-dlc-learner` is a Spring Boot AI starter template (RAG + agentic workflows, Spring AI 2.0,
Boot 4, Java 21) paired with an AI-DLC spec-driven dev workflow.

## Module map

- `spring-ai-dlc-core` — provider-neutral facades: `rag.ingest.*` (IngestionPipeline,
  DocumentSource, ChunkingSpec, IngestionReport), `rag.query.*` (RagPipeline, RagAnswer,
  Citation), `agent.*` (AgentDefinition, Agent, AgentResult, AgentFactory, ToolInvocation),
  `agent.workflow.*` (Workflow, ChainWorkflow, ParallelWorkflow, RoutingWorkflow,
  EvaluatorOptimizerWorkflow, Workflows).
- `spring-ai-dlc-autoconfigure` — `AiDlcProperties` (`ai.dlc.*`), `AiDlcChatAutoConfiguration`,
  `AiDlcRagAutoConfiguration`, `AiDlcAgentAutoConfiguration`, `StartupIngestionRunner`.
- `spring-ai-dlc-starter` — aggregator (depends on core + autoconfigure).
- `demo-app` — runnable playground: `RagController` (`/api/rag/ask`, `/api/rag/ingest`),
  `AgentController` (`/api/agent/ask`, `/api/agent/summarize`), `rag-corpus/` docs.

## Spec-first rule

**Never write code without a corresponding `specs/<feature>/tasks.md` entry.** New features go
through `/spec-init` -> `/spec-design` -> `/spec-tasks` -> `/implement` (see `specs/README.md`
and `specs/docs-rag-endpoint/` for a worked example). Each phase has a human review gate.

## Build commands

- `./gradlew build` — full build, all modules.
- `./gradlew :spring-ai-dlc-core:test` (or any module) — scoped tests.
- `docker compose up -d` — Ollama + pgvector for local dev.
- `./gradlew :demo-app:bootRun` — run the demo app.

## Conventions

- Java 21, records for data carriers, constructor injection, no Lombok.
- Group `com.aidlc`, base package `com.aidlc.spring`; `ai.dlc.*` config properties,
  `com.aidlc.spring.{core,autoconfigure,demo}.*` Java packages.
- `@ConditionalOnMissingBean` for all autoconfigure beans; Boot 4 testing uses `@MockitoBean`.

## Token rules

- Read only the files named in the relevant spec (`requirements.md`/`design.md`/`tasks.md`'s
  `files:` field) — subagents have explicit allow-lists, stop and report if something's missing.
- Prefer Grep/Glob over broad exploration.
- Don't re-read files you just wrote/edited.

## See also

- `docs/architecture.md` — module map + diagram (added in Phase 8).
- `specs/templates/` — exact structure for requirements/design/tasks files.
- `specs/README.md` — full spec-driven workflow explanation.

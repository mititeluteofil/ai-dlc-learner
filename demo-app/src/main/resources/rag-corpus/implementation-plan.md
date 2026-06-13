# Plan: Spring Boot AI Starter Template + AI-DLC Spec-Driven Scaffold

## Context

`ai-dlc-learner` is an empty green-field repo. The goal is to turn it into a **template repository** with two halves that reinforce each other:

1. **A Spring Boot AI base library** — a provider-neutral, low-boilerplate foundation for RAG, agentic workflows, and vector-DB access, built on **Spring AI 2.0 GA** (released May 28, 2026; Spring Boot 4, Framework 7, Java 21, Jackson 3). Structured so it could later be published as an OSS Spring Boot starter wrapping Spring AI.
2. **An AI-DLC / spec-driven development scaffold** — Claude Code subagents, slash commands, spec templates, and a token-efficient `CLAUDE.md`, modeled on AWS AI-DLC (Inception → Construction → Operations) and Kiro-style specs (requirements / design / tasks per feature).

The "why": Python owns fast-paced AI development. This template exists to shrink time-to-AI-tool in Java — clone, `docker compose up`, and both the app stack *and* the agentic dev workflow are ready. A `docs/java-vs-python.md` comparison doc states this thesis explicitly with side-by-side code.

**User decisions (locked):** Spring AI only (no LangChain4j adapter yet, keep the seam clean) · Boot 4 + Spring AI 2.0 GA · Gradle Kotlin DSL, multi-module · Zero-config local dev via Docker Compose (Ollama + pgvector); OpenAI/Anthropic via profiles + env vars.

## Repository layout

```
ai-dlc-learner/
├── README.md                    # rewrite: what this template is, 3-command quickstart
├── CLAUDE.md                    # root agent rules, < 60 lines
├── compose.yaml                 # ollama + pgvector/pgvector:pg17, healthchecks, model volume
├── .env.example                 # OPENAI_API_KEY=, ANTHROPIC_API_KEY=
├── settings.gradle.kts / build.gradle.kts / gradlew*
├── gradle/libs.versions.toml    # ALL versions pinned here (Boot 4.0.x, Spring AI BOM 2.0.x)
├── spring-ai-dlc-core/          # provider-neutral facades; depends on Spring AI interfaces only
├── spring-ai-dlc-autoconfigure/ # @AutoConfiguration + @ConfigurationProperties("ai.dlc")
├── spring-ai-dlc-starter/       # aggregator module (api(core) + api(autoconfigure))
├── demo-app/                    # runnable playground; profiles: default(ollama), openai, anthropic
├── .claude/
│   ├── settings.json            # allow gradle, docker compose, curl localhost
│   ├── agents/                  # 6 subagents (below)
│   └── commands/                # 6 slash commands (below)
├── specs/
│   ├── README.md, templates/{requirements,design,tasks}.template.md
│   └── docs-rag-endpoint/       # worked example spec describing the actual demo feature
├── docs/
│   ├── how-to-use.md            # detailed guide (deliverable)
│   ├── java-vs-python.md        # comparison doc (deliverable)
│   ├── architecture.md          # module map + mermaid, short
│   └── ai-dlc-workflow.md       # AI-DLC phases → commands mapping
└── .github/workflows/ci.yml     # gradle build + unit tests (no Ollama in CI)
```

Group `com.aidlc`, base package `com.aidlc.spring`. Specs live in `specs/` (short paths = fewer tokens in every prompt).

## Core library design

### `spring-ai-dlc-core` — `com.aidlc.spring.core.rag`
Collapse Spring AI's reader→splitter→vectorstore and ChatClient+RetrievalAugmentationAdvisor chains into two entry points:

- `rag.ingest.IngestionPipeline` — fluent: `ingestion.from(source).withChunking(spec).run()` → `IngestionReport`
- `rag.ingest.DocumentSource` — sealed factories: `.classpath("rag-corpus/**/*.md")`, `.path()`, `.url()`, `.text()`; picks the right DocumentReader by extension
- `rag.ingest.ChunkingSpec` — record (chunkSize, overlap) → `TokenTextSplitter`; `.defaults()` = 800/100
- `rag.ingest.IngestionReport` — record: documentsRead, chunksWritten, elapsed, errors
- `rag.query.RagPipeline` — `rag.ask("question")` → `RagAnswer`; builder exposes topK, similarityThreshold, promptTemplate, filterExpression, `enableQueryRewrite()`
- `rag.query.RagAnswer` — record: answer + `List<Citation>` (source, snippet, score) from advisor context

```java
IngestionReport r = ingestion.from(DocumentSource.classpath("rag-corpus/**/*.md")).run();
RagAnswer a = rag.ask("How do I switch to OpenAI?");
```

Idempotency: deterministic document IDs (hash of source + chunk index) so re-ingestion upserts instead of duplicating.

### `com.aidlc.spring.core.agent` — Anthropic "Building Effective Agents" patterns over ChatClient
- `AgentDefinition` (record: name, systemPrompt, tool objects, options) · `Agent` (executor with max-iterations guard) · `AgentResult` (finalText, toolInvocations, usage) · `AgentFactory` (only place provider config touches agents)
- `workflow.Workflow` (functional interface) + `ChainWorkflow`, `ParallelWorkflow` (virtual threads), `RoutingWorkflow` (structured-output classifier → route map), `EvaluatorOptimizerWorkflow`, and `Workflows` static DSL

Deliberately NOT built: memory abstraction (use Spring AI `ChatMemory`), multi-agent bus, streaming workflows. Scope = facades and defaults, not a framework rewrite. Future-LangChain4j seam: core depends only on Spring AI interfaces (`VectorStore`, `ChatClient`, `Document`) — documented in architecture.md.

### `spring-ai-dlc-autoconfigure`
- `AiDlcProperties` (`ai.dlc.*`): `rag.{chunk-size, chunk-overlap, top-k, similarity-threshold, query-rewrite, ingest-on-startup.*}`, `agent.max-tool-iterations`
- `AiDlcRagAutoConfiguration` — `@ConditionalOnBean({VectorStore, ChatClient.Builder})`, all beans `@ConditionalOnMissingBean`
- `AiDlcAgentAutoConfiguration`, optional `AiDlcChatAutoConfiguration` (default ChatClient + `SimpleLoggerAdvisor`)
- `StartupIngestionRunner` — `@ConditionalOnProperty` ApplicationRunner
- `AutoConfiguration.imports` + configuration-processor for IDE property autocomplete. Vector-store layer stays thin: provider selection is already property-driven in Spring AI.

## Agentic scaffold (.claude/ + specs/)

**Subagents** (each prompt enumerates exactly which files it may read — the token-efficiency contract; rule: "if a file isn't in your allowed list, stop and report, don't explore"):

| Agent | Role | Writes |
|---|---|---|
| `requirements-analyst` | idea → user stories + EARS acceptance criteria | `specs/<f>/requirements.md` |
| `solution-architect` | requirements → components, API, explicit files-to-touch | `specs/<f>/design.md` |
| `task-planner` | design → ordered checkbox tasks, each with files + verify command | `specs/<f>/tasks.md` |
| `implementer` | execute ONE task; reads only files named in the task | source files, ticks box |
| `test-engineer` | tests for a task; Bash limited to gradle test | test files |
| `spec-auditor` | diff code vs spec via `git diff --name-only`; report only | nothing |

**Commands:** `/spec-init <feature> "<idea>"` → `/spec-design` → `/spec-tasks` → `/implement <feature> [task-N]` (runs implementer + test-engineer, ticks checkbox only on green tests) · `/spec-review` (auditor) · `/spec-status` (grep checkbox counts, near-zero tokens). Human review gates between each phase = AI-DLC checkpoints.

**Spec templates** (strict structure): requirements = Intent (2 sentences) / User Stories / EARS criteria / Out of Scope; design = Approach / Components table / API & Data / **Files to Create-Modify (explicit paths)** / Risks; tasks = `- [ ] T1: <verb> — files: … — verify: <command>`, ≤ 1 h each.

**`specs/docs-rag-endpoint/`** ships filled-in for the real demo feature — a worked example agents can pattern-match.

**`CLAUDE.md`** (< 60 lines): purpose + module map · "features go spec-first, never code without a tasks.md entry" · build commands · conventions (Java 21, records, constructor injection, no Lombok, `ai.dlc.*` prefix) · token rules ("read only files named in specs; prefer Grep; don't re-read files you wrote") · pointers to architecture.md and templates.

## Demo app

1. **Docs-RAG endpoint** — startup-ingests `classpath:rag-corpus/**/*.md` (this repo's own docs, copied in); `POST /api/rag/ask` → answer + citations; `POST /api/rag/ingest`.
2. **Agent workflow endpoint** — `ProjectTools` (`@Tool listGradleModules()`, stubbed `getBuildStatus()` — tool calling with no external APIs), a `RoutingWorkflow` (docs question → RAG, project question → tool agent), a `ChainWorkflow` example; `POST /api/agent/ask`.

Defaults: Ollama `qwen3:4b` chat + `nomic-embed-text` embeddings, `pull-model-strategy=when_missing`, pgvector `initialize-schema=true`, `dimensions=768`. Profiles `openai`/`anthropic` switch `spring.ai.model.*` providers, set matching dimensions, and use distinct pgvector table names per profile.

## Docs deliverables

- `docs/how-to-use.md`: clone as template → compose up → bootRun → curl; full AI-DLC walkthrough (idea → /spec-init → … → /implement); extending the library; publishing the starter.
- `docs/java-vs-python.md`: thesis "Python owns AI velocity — this closes the gap in Java"; side-by-side LangChain RAG (~15 lines Python) vs `IngestionPipeline`/`RagPipeline` (~8 lines Java); ecosystem, typing/refactorability, ops (GraalVM, virtual threads, single jar), performance, team fit.

## Implementation order (verification gate per phase)

1. **Skeleton** — wrapper, 4 modules, `libs.versions.toml`; verify exact Spring AI 2.0 artifact IDs against the published BOM POM. Gate: `./gradlew build` green.
2. **Infra + raw boot** — compose.yaml + demo-app on raw Spring AI starters (de-risk stack first). Gate: compose healthy, `/actuator/health` UP, throwaway ChatClient endpoint answers via Ollama.
3. **Core RAG** — facades + unit tests with mocked VectorStore/ChatModel. Gate: `:spring-ai-dlc-core:test` green.
4. **Core agents** — Agent + 4 workflow classes; deterministic tests with stubbed ChatClient.
5. **Autoconfigure + starter** — `ApplicationContextRunner` tests (beans conditional, overrides respected).
6. **Demo wired to starter** — replace throwaway code. Gate: end-to-end `curl /api/rag/ask` returns citations; routing works; `openai` profile boots with key.
7. **Agentic scaffold** — CLAUDE.md, agents, commands, templates, worked-example spec (written truthfully against what Phase 6 built). Gate: `/spec-init test-feature` produces valid requirements.md in a live session, then delete.
8. **Docs + CI** — how-to-use, java-vs-python, architecture, workflow docs; GitHub Actions (unit tests only; LLM-touching integration tests behind a flag with Testcontainers).

Commit per phase on branch `claude/java-springboot-ai-starter-d7rhv3`, push at the end (`git push -u origin`, retry w/ backoff on network failure). No PR unless requested.

## Risks / gotchas (mitigations baked into phases)

- **Jackson 3** (`tools.jackson.*`) — 1.x-era snippets import the wrong namespace; never declare Jackson versions directly.
- **Spring AI 2.0 API drift** — immutable builder-only options, per-model autoconfigure artifacts; verify against Maven Central in Phase 1, budget slack in Phase 2.
- **Boot 4 testing** — `@MockBean` → `@MockitoBean`.
- **Ollama first boot** — ~2.8 GB model pulls; named volume, documented warm-up `ollama pull`, generous init timeout.
- **Embedding dimension mismatch** across profiles — per-profile `dimensions` + distinct table names; called out loudly in docs.
- **CI** — no GPU/Ollama: fakes only in default `test` task.
- **Small-model flakiness** for routing/structured output — low temperature, enum-constrained outputs, docs note that agent demos shine on cloud profiles.

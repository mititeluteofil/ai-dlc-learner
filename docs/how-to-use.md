# How to use this template

## 1. Quickstart

```bash
git clone <this-repo-url> my-ai-app && cd my-ai-app
docker compose up -d                 # Ollama + pgvector, with healthchecks
./gradlew :demo-app:bootRun
```

First boot pulls `qwen3:4b` (chat) and `nomic-embed-text` (embeddings) into the Ollama
container (`pull-model-strategy: when_missing`) — this can take a few minutes and ~2.8 GB on a
cold cache. The app waits on Postgres/pgvector via the datasource; pgvector's schema is
created automatically (`spring.ai.vectorstore.pgvector.initialize-schema=true`).

On startup, `ai.dlc.rag.ingest-on-startup.enabled=true` (set in
`demo-app/src/main/resources/application.yml`) ingests every markdown file under
`classpath:rag-corpus/**/*.md` (this repo's own `implementation-plan.md` and `readme.md`,
copied into `demo-app/src/main/resources/rag-corpus/`) into pgvector.

### Try the endpoints

**RAG: ask a question grounded in the docs corpus**

```bash
curl -s localhost:8080/api/rag/ask \
  -H 'Content-Type: application/json' \
  -d '{"question": "What modules make up this project?"}' | jq
```

Returns a `RagAnswer`: `{"answer": "...", "citations": [{"source": "...", "snippet": "...", "score": 0.81}, ...]}`.

**RAG: re-ingest the docs corpus on demand**

```bash
curl -s -X POST localhost:8080/api/rag/ingest | jq
```

Returns an `IngestionReport`: `{"documentsRead": 2, "chunksWritten": N, "elapsed": "...", "errors": []}`.
Re-running this upserts (deterministic chunk IDs), it never duplicates entries.

**Agent: routed question (docs vs. project)**

```bash
curl -s localhost:8080/api/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{"question": "What Gradle modules does this repo have?"}' | jq

curl -s localhost:8080/api/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{"question": "How do I add a new RAG feature?"}' | jq
```

`AgentController` uses a `RoutingWorkflow`: a structured-output classifier call decides whether
the question is a "project" question (routed to a tool-calling `Agent` over `ProjectTools` —
`listGradleModules()`, stubbed `getBuildStatus()`) or a "docs" question (routed to
`RagPipeline.ask(...)`).

**Agent: chain workflow (summarize -> bullet points)**

```bash
curl -s localhost:8080/api/agent/summarize \
  -H 'Content-Type: application/json' \
  -d '{"question": "<a long paragraph of text to summarize>"}' | jq
```

`AgentController`'s `summaryChain` is a two-step `ChainWorkflow` (`Workflows.chain(...)`):
summarize in two sentences, then rewrite as bullet points.

> Small local models (e.g. `qwen3:4b`) can be flaky at the structured-output classification
> step used by `RoutingWorkflow`. If routing seems inconsistent, this is expected with small
> models — it's more reliable on cloud profiles (see "OpenAI profile" below).

## 2. Full AI-DLC walkthrough

This repo's `.claude/` scaffold implements a spec-first workflow: every feature gets a
`specs/<feature>/{requirements,design,tasks}.md`, written in order with a human review gate
between each step. Full mechanics: [`specs/README.md`](../specs/README.md); phase mapping:
[`docs/ai-dlc-workflow.md`](ai-dlc-workflow.md).

A worked example already exists at [`specs/docs-rag-endpoint/`](../specs/docs-rag-endpoint/) —
it documents the real `RagController`/`IngestionPipeline`/`RagPipeline` feature built in
Phase 6. Use it as a pattern reference when writing new specs.

To add a new feature end-to-end:

```bash
# 1. Inception: idea -> user stories + EARS acceptance criteria
/spec-init add-streaming-endpoint "Stream RAG answers token-by-token over SSE"
# -> review specs/add-streaming-endpoint/requirements.md

# 2. Inception -> Construction: requirements -> components, API, files to touch
/spec-design add-streaming-endpoint
# -> review specs/add-streaming-endpoint/design.md, especially "Files to Create/Modify"

# 3. Construction planning: design -> ordered checkbox tasks with verify commands
/spec-tasks add-streaming-endpoint
# -> review task order and verify: commands in tasks.md

# 4. Construction execution: implement + test one task at a time
/implement add-streaming-endpoint task-1
# implementer writes the code and ticks the checkbox; test-engineer runs the verify
# command and reverts the checkbox if tests fail. Repeat for each remaining task.

# 5. Operations: diff the code against the spec before merging
/spec-review add-streaming-endpoint

# Any time: near-zero-token progress check across all specs/
/spec-status
```

Each of these commands lives in `.claude/commands/*.md` and delegates to a subagent in
`.claude/agents/*.md`. Every subagent's prompt enumerates exactly which files it may read —
if a needed file isn't on that list, it stops and reports instead of exploring the repo. See
`CLAUDE.md` for the repo-wide rules ("never write code without a `tasks.md` entry").

## 3. Extending the library

`spring-ai-dlc-starter` is an aggregator: it pulls in `spring-ai-dlc-core` (the
`IngestionPipeline`/`RagPipeline`/`Agent`/`Workflow` facades) and
`spring-ai-dlc-autoconfigure` (the Spring Boot wiring). To add a new RAG or agent feature in an
application built on this starter:

- **Use the existing facades directly.** `IngestionPipeline`, `RagPipeline`, `AgentFactory`,
  and the `Workflow` implementations (`ChainWorkflow`, `ParallelWorkflow`, `RoutingWorkflow`,
  `EvaluatorOptimizerWorkflow`, or the `Workflows` DSL) are autoconfigured beans — inject them
  like `RagController` and `AgentController` do.

- **Tune behavior via `ai.dlc.*` properties** (see `AiDlcProperties`):
  - `ai.dlc.rag.chunk-size` / `chunk-overlap` (default 800/100)
  - `ai.dlc.rag.top-k` / `similarity-threshold` (default 4 / 0.0)
  - `ai.dlc.rag.query-rewrite` (default false — enables `RetrievalAugmentationAdvisor` +
    `RewriteQueryTransformer` instead of a plain `QuestionAnswerAdvisor`)
  - `ai.dlc.rag.ingest-on-startup.enabled` / `location-pattern`
  - `ai.dlc.agent.max-tool-iterations` (default 5)

- **Override any bean** by declaring your own bean of the same type in your application —
  every bean in `AiDlcChatAutoConfiguration`, `AiDlcRagAutoConfiguration`, and
  `AiDlcAgentAutoConfiguration` is `@ConditionalOnMissingBean`, so your definition wins and the
  default is skipped. For example, to customize the `RagPipeline` (e.g. a custom
  `promptTemplate` or `filterExpression`), define your own `RagPipeline` bean using
  `RagPipeline.builder(chatClientBuilder, vectorStore)...build()`.

- **New document sources**: `DocumentSource` is a sealed interface with `classpath()`,
  `path()`, `url()`, and `text()` factories. To support a new source type, add a new record
  implementing `DocumentSource` (`identifier()` + `read()`); `IngestionPipeline.from(...)`
  works with any implementation.

- **New workflow types**: `Workflow` is a single-method functional interface
  (`String execute(String input)`). Compose existing implementations via `Workflows` (e.g.
  `Workflows.chain(...)`), or implement `Workflow` directly for custom control flow — see
  `ParallelWorkflow` (virtual threads), `RoutingWorkflow` (structured-output classifier), and
  `EvaluatorOptimizerWorkflow` for patterns to follow.

- **New tools for agents**: annotate methods with `@Tool` (Spring AI's tool-calling
  annotation) on a `@Component`, as `ProjectTools` does, then pass the component to
  `AgentDefinition.of(...).withTools(List.of(yourTools))`.

## 4. OpenAI profile (documented gap / future work)

`demo-app/src/main/resources/application-openai.yml` exists as a **template** for an `openai`
Spring profile: it sets `spring.ai.openai.api-key`, a `gpt-4o-mini` chat model,
`text-embedding-3-small` embeddings (`dimensions=1536`), and a distinct pgvector table
(`vector_store_openai`) so Ollama (768-dim) and OpenAI (1536-dim) embeddings can coexist in the
same Postgres instance.

**This profile is not yet wired up** — `demo-app/build.gradle.kts` currently depends only on
`spring-ai-starter-model-ollama`. To actually use it:

1. Add `org.springframework.ai:spring-ai-starter-model-openai` (version resolved via
   `spring-ai-bom`, already imported) to `demo-app/build.gradle.kts`.
2. Set `OPENAI_API_KEY` (see `.env.example`).
3. Run with `OPENAI_API_KEY=sk-... ./gradlew :demo-app:bootRun --args='--spring.profiles.active=openai'`.

An `anthropic` profile would follow the same pattern with
`org.springframework.ai:spring-ai-starter-model-anthropic` and `spring.ai.anthropic.*`
properties (chat only — Anthropic does not offer an embeddings API, so an `anthropic` profile
would still need an embedding model from another provider for the vector store).

## 5. Publishing the starter (future work)

`spring-ai-dlc-core`, `spring-ai-dlc-autoconfigure`, and `spring-ai-dlc-starter` are not
currently published anywhere — they're consumed via Gradle project dependencies
(`project(":spring-ai-dlc-core")`, etc.) within this repo. To publish them as Maven artifacts
under group `com.aidlc`:

- Apply the `maven-publish` plugin (and `signing` for Maven Central) to the three library
  modules (not `demo-app`).
- Replace the shared `0.1.0-SNAPSHOT` version in the root `build.gradle.kts` with a real
  release versioning scheme (e.g. semantic versioning, or a CI-driven tag-based version).
- Configure a `publishing { publications { ... } }` block per module with group `com.aidlc`,
  the module's existing artifact name, and POM metadata (license, SCM URL, developers).
- Decide a publishing target: a local/company Maven repo for internal use, or Maven
  Central / Sonatype OSSRH for OSS distribution (requires GPG signing and a Sonatype account).
- `spring-ai-dlc-starter` would be the primary consumer-facing artifact (`api(core)` +
  `api(autoconfigure)`), matching the "single starter dependency" pattern.

None of this is configured yet — it's listed here so the path is clear if/when this template
graduates into a standalone published starter.

# ai-dlc-learner

A **Spring Boot AI starter template** (RAG + agentic workflows on Spring AI 2.0, Boot 4,
Java 21) paired with an **AI-DLC spec-driven development scaffold** for Claude Code. Clone it,
and both the application stack *and* the agentic dev workflow are ready.

Two halves that reinforce each other:

1. **A provider-neutral Spring Boot AI library** (`spring-ai-dlc-core` /
   `-autoconfigure` / `-starter`) that collapses Spring AI's reader -> splitter -> vector store
   and `ChatClient` + retrieval-advisor chains into two entry points — `IngestionPipeline` and
   `RagPipeline` — plus agent/workflow facades (`Agent`, `AgentFactory`, `ChainWorkflow`,
   `ParallelWorkflow`, `RoutingWorkflow`, `EvaluatorOptimizerWorkflow`).
2. **An AI-DLC spec-driven scaffold** (`.claude/agents/`, `.claude/commands/`, `specs/`) modeled
   on AWS AI-DLC (Inception -> Construction -> Operations): `/spec-init` -> `/spec-design` ->
   `/spec-tasks` -> `/implement` -> `/spec-review`, each with a human review gate.

## Quickstart

```bash
git clone <this-repo-url> my-ai-app && cd my-ai-app
docker compose up -d                 # Ollama + pgvector (zero-config local dev)
./gradlew :demo-app:bootRun
```

That's it — no API keys needed. First boot pulls the Ollama models
(`qwen3:4b` chat, `nomic-embed-text` embeddings, ~2.8 GB) and ingests this repo's own docs into
pgvector (`ai.dlc.rag.ingest-on-startup.enabled=true`).

Try it:

```bash
# Ask a question grounded in this repo's docs (RAG)
curl -s localhost:8080/api/rag/ask \
  -H 'Content-Type: application/json' \
  -d '{"question": "What modules make up this project?"}' | jq

# Routed agent: docs question vs. project/tool question
curl -s localhost:8080/api/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{"question": "What Gradle modules does this repo have?"}' | jq

# Two-step chain workflow: summarize -> bullet points
curl -s localhost:8080/api/agent/summarize \
  -H 'Content-Type: application/json' \
  -d '{"question": "<text to summarize>"}' | jq
```

## Project structure

```
spring-ai-dlc-core/          # provider-neutral RAG + agent facades over Spring AI
spring-ai-dlc-autoconfigure/ # ai.dlc.* Spring Boot autoconfiguration
spring-ai-dlc-starter/       # aggregator: api(core) + api(autoconfigure)
demo-app/                    # runnable playground (RagController, AgentController, ...)
.claude/                     # agentic scaffold: subagents + slash commands
specs/                        # spec-driven workflow templates + worked example
docs/                         # detailed guides (below)
compose.yaml                  # ollama + pgvector for local dev
```

## Docs

- [`docs/how-to-use.md`](docs/how-to-use.md) — full quickstart, all endpoints, the AI-DLC
  walkthrough, extending the library, and notes on publishing/profiles.
- [`docs/architecture.md`](docs/architecture.md) — module map, dependency diagram, and the
  "future LangChain4j seam".
- [`docs/ai-dlc-workflow.md`](docs/ai-dlc-workflow.md) — AI-DLC phases mapped to the
  `.claude/` subagents and slash commands.
- [`docs/java-vs-python.md`](docs/java-vs-python.md) — why this template exists: closing the
  Java/Python AI velocity gap, with real code side-by-side.
- [`CLAUDE.md`](CLAUDE.md) — repo rules for agentic coding (spec-first, conventions, token
  rules).
- [`specs/README.md`](specs/README.md) — the spec-driven workflow in detail, plus
  [`specs/docs-rag-endpoint/`](specs/docs-rag-endpoint/), a worked example.

## Build

```bash
./gradlew build                    # full build, all modules, unit tests (no Docker needed)
./gradlew :spring-ai-dlc-core:test # scoped tests for any module
```

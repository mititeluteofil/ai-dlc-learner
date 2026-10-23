# ai-dlc-learner

A **Spring Boot AI starter template** (RAG + agentic workflows on Spring AI 2.0, Boot 4,
Java 21) paired with an **AI-DLC spec-driven development scaffold** for Claude Code. Clone it,
and both the application stack *and* the agentic dev workflow are ready.

Two halves that reinforce each other:

1. **A provider-neutral Spring Boot AI library** (`spring-ai-dlc-core` /
   `-autoconfigure` / `-starter`) that collapses Spring AI's reader -> splitter -> vector store
   and `ChatClient` + retrieval-advisor chains into two entry points — `IngestionPipeline` and
   `RagPipeline` — plus agent/workflow/memory facades (`Agent`, `AgentFactory`,
   `ConversationMemory`, `ChainWorkflow`, `ParallelWorkflow`, `RoutingWorkflow`,
   `EvaluatorOptimizerWorkflow`).
2. **An AI-DLC spec-driven scaffold** (`.claude/agents/`, `.claude/commands/`, `specs/`) modeled
   on AWS AI-DLC (Inception -> Construction -> Operations): `/spec-init` -> `/spec-design` ->
   `/spec-tasks` -> `/implement` -> `/spec-review`, each with a human review gate.

## Quickstart (Zero API_KEY Required)

```bash
git clone <this-repo-url> my-ai-app && cd my-ai-app
./gradlew :demo-app:bootRun          # Runs immediately in Zero-Config Standalone mode!
```

No `OPENAI_API_KEY` or Docker required to try it out:
- **Zero-Config Standalone Mode**: Automatically uses an in-memory `SimpleVectorStore`, ingests `rag-corpus/*.md` at startup, and routes LLM calls to a keyless OpenAI-compatible inference endpoint (or local Ollama if running on `localhost:11434`).
- **Full Local Stack (`docker compose up -d`)**: Starts local **Ollama** (`qwen3:4b` chat + `nomic-embed-text` embeddings) and PostgreSQL **`pgvector`** (`localhost:5432`).
- **Interactive Web Playground**: Open **`http://localhost:8080`** in your browser, or see [`demo-app/GUIDE.md`](demo-app/GUIDE.md) for the complete walkthrough.

Try it via CLI:

```bash
# Ask a question grounded in this repo's docs (RAG)
curl -s localhost:8080/api/rag/ask \
  -H 'Content-Type: application/json' \
  -d '{"question": "What modules make up this project?"}' | jq

# Stateful routed agent with multi-turn ConversationMemory (keyed by conversationId)
curl -s localhost:8080/api/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{"question": "Hi, my name is Teofil. What Gradle modules does this repo have?", "conversationId": "session-1"}' | jq

curl -s localhost:8080/api/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{"question": "What is my name and what is the build status?", "conversationId": "session-1"}' | jq

# Two-step chain workflow: summarize -> bullet points
curl -s localhost:8080/api/agent/summarize \
  -H 'Content-Type: application/json' \
  -d '{"question": "<text to summarize>"}' | jq
```

## Project structure

```
spring-ai-dlc-core/          # provider-neutral RAG, agent, memory & workflow facades
spring-ai-dlc-autoconfigure/ # ai.dlc.* Spring Boot autoconfiguration
spring-ai-dlc-starter/       # aggregator: api(core) + api(autoconfigure)
demo-app/                    # runnable playground + web UI (see demo-app/GUIDE.md)
.claude/                     # agentic scaffold: subagents + slash commands
specs/                       # spec-driven workflow templates + worked examples
docs/                        # detailed guides (below)
compose.yaml                 # ollama + pgvector for local dev
```

## Docs

- [`demo-app/GUIDE.md`](demo-app/GUIDE.md) — complete guide to running and testing the zero-API-key demo app (Web UI + CLI examples).
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
  [`specs/docs-rag-endpoint/`](specs/docs-rag-endpoint/) and [`specs/conversation-memory/`](specs/conversation-memory/).

## Build

```bash
./gradlew build                    # full build, all modules, unit tests (no Docker needed)
./gradlew :spring-ai-dlc-core:test # scoped tests for any module
```

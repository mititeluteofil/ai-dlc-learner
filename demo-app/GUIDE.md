# AI-DLC Demo App — Complete User Guide (Zero API_KEY Required)

## 1. What Is This Project About?

`demo-app` is a runnable Spring Boot 4 + Spring AI 2.0 reference application that showcases **`spring-ai-dlc-starter`** — a provider-neutral Java AI library designed to make building production AI features in Spring Boot as fast and ergonomic as Python, while keeping enterprise type safety and testability.

It demonstrates four core capabilities in one cohesive mini-project:

1. **RAG Pipeline (`IngestionPipeline` + `RagPipeline`)**: Automatically ingests project Markdown documentation (`rag-corpus/*.md`) on startup, splits it into token chunks, embeds it into a vector store, and answers questions with source citations (`/api/rag/ask`).
2. **Tool-Calling Agent (`AgentFactory` + `ProjectTools`)**: An AI agent equipped with `@Tool` methods (`listGradleModules()`, `getBuildStatus()`) that inspects project metadata to answer repository questions (`/api/agent/ask`).
3. **Pluggable Conversation Memory (`ConversationMemory`)**: Multi-turn stateful memory keyed by `conversationId` with configurable backends (`in-memory`, `jdbc`, `pgvector`), bounded sliding window (`max-messages`), and automatic graceful degradation if a backing store fails.
4. **Multi-Step Agentic Workflows (`RoutingWorkflow` + `ChainWorkflow`)**:
   - `RoutingWorkflow`: Classifies incoming questions into either the `docs` route (RAG) or `project` route (stateful Agent + Tools).
   - `ChainWorkflow`: A two-step pipeline (`/api/agent/summarize`) that summarizes text in two sentences and reformats the result as concise bullet points.

---

## 2. How It Runs Without an `API_KEY`

You do **not** need an OpenAI/Anthropic API key (or even Docker) to run `demo-app` and execute real LLM calls.

At startup, `DemoApplication` and `ZeroKeyAiConfig` automatically detect your local environment:

| Component | Mode A: Zero-Config Standalone (Default if Docker is off) | Mode B: Local Docker / Ollama (`docker compose up -d`) |
|---|---|---|
| **Chat LLM** | Calls a keyless OpenAI-compatible LLM endpoint (`https://text.pollinations.ai/openai`) with full message history & tool context | Uses local **Ollama** (`http://localhost:11434`, model `qwen3:4b`) |
| **Embeddings** | Built-in deterministic 768-dim local semantic n-gram embedding model | Local **Ollama** (`nomic-embed-text`, 768-dim) |
| **Vector Store** | Spring AI in-memory `SimpleVectorStore` (ingests `rag-corpus/*.md` at boot in <1s) | PostgreSQL **`pgvector`** (`localhost:5432/aidlc`) |
| **Chat Memory** | `InMemoryChatMemoryRepository` (`ai.dlc.agent.memory.backend=in-memory`) | Configurable: `in-memory`, `jdbc`, or `pgvector` |

---

## 3. Running the Demo App

### Option 1: Instant Zero-Config Run (No Docker, No API_KEY)

From the repository root:

```bash
./gradlew :demo-app:bootRun
```

The application starts on **`http://localhost:8080`**, ingests the Markdown corpus into memory, and is immediately ready to make live LLM calls.

### Option 2: Full Local Stack with Ollama + pgvector (No API_KEY)

If you have Docker running and want 100% local model inference + persistent PostgreSQL vector storage:

```bash
docker compose up -d
./gradlew :demo-app:bootRun
```

---

## 4. Using the Interactive Web Playground

Once the app is running, open your browser to:

👉 **`http://localhost:8080`**

The interactive dashboard displays your active runtime status (`LLM Provider`, `Memory Backend`, `VectorStore`) and provides 3 live test panels:
1. **Routed Agent + Conversation Memory (`/api/agent/ask`)** — test multi-turn conversations by keeping the same `conversationId` across turns.
2. **Docs RAG Pipeline (`/api/rag/ask`)** — ask questions about the repo's architecture and inspect the returned source citations.
3. **Two-Step Chain Workflow (`/api/agent/summarize`)** — paste any paragraph to run the `summarize -> bullet-points` chain.

---

## 5. Testing via CLI (`curl` / PowerShell)

### Check Runtime Status

```bash
curl -s http://localhost:8080/api/demo/status
```

### A. Multi-Turn Stateful Conversation Memory (`/api/agent/ask`)

Supply a non-blank `conversationId` to enable `ConversationMemory` on the `project` route:

**Turn 1 — Introduce yourself and ask a project question:**
```bash
curl -s http://localhost:8080/api/agent/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Hi, my name is Teofil. What Gradle modules exist in this repo?", "conversationId": "session-42"}'
```

**Turn 2 — Ask a follow-up using the same `conversationId`:**
```bash
curl -s http://localhost:8080/api/agent/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is my name, and what is the current build status of those modules?", "conversationId": "session-42"}'
```

**Stateless Call — Omit `conversationId` (no memory read or written):**
```bash
curl -s http://localhost:8080/api/agent/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What Gradle modules exist in this repo?"}'
```

### B. Grounded Documentation RAG (`/api/rag/ask`)

```bash
curl -s http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the two halves of ai-dlc-learner?"}'
```

### C. Two-Step Summarization Chain (`/api/agent/summarize`)

```bash
curl -s http://localhost:8080/api/agent/summarize \
  -H "Content-Type: application/json" \
  -d '{"question": "Spring AI-DLC Learner provides a provider-neutral Spring Boot starter for RAG and agentic workflows alongside a spec-driven development scaffold. It supports in-memory, JDBC, and pgvector conversation memory backends."}'
```

---

## 6. Configuring Conversation Memory (`application.yml`)

Conversation memory is configured under `ai.dlc.agent.memory.*` in [`demo-app/src/main/resources/application.yml`](src/main/resources/application.yml):

```yaml
ai:
  dlc:
    agent:
      memory:
        enabled: true          # Set to false to disable ChatMemory autoconfiguration completely
        max-messages: 20       # Sliding window size per conversationId (oldest evicted first)
        backend: in-memory     # Options: in-memory | jdbc | pgvector
        initialize-schema: true
```

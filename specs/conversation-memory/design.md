# conversation-memory — Design

## Approach

Conversation memory is added as a first-class, autoconfigured library capability, mirroring how
RAG and agents are wired today. Spring AI 2.0 ships the abstractions we build on:
`ChatMemory` (the conversation-scoped store interface), `MessageWindowChatMemory` (the bounded,
message-count window implementation), `ChatMemoryRepository` (the pluggable persistence SPI,
with `InMemoryChatMemoryRepository` and `JdbcChatMemoryRepository` implementations), and
`MessageChatMemoryAdvisor` (the `ChatClient` advisor that reads/writes history keyed by a
`conversationId`). The starter's job is to select and wire these from `ai.dlc.agent.memory.*`
config, not to reimplement them.

A new `AiDlcMemoryAutoConfiguration` (in `spring-ai-dlc-autoconfigure`) provides, gated on
`ai.dlc.agent.memory.enabled=true`: a `ChatMemoryRepository` chosen by the `backend` selector
(`in-memory` / `jdbc` / `pgvector`) and a `MessageWindowChatMemory` bounded by `max-messages`,
both `@ConditionalOnMissingBean`. A thin core facade, `ConversationMemory`
(`spring-ai-dlc-core`, package `com.aidlc.spring.core.agent.memory`), wraps the `ChatMemory`
and exposes a single method that decorates a `ChatClient` request with a
`MessageChatMemoryAdvisor` for a given conversation id, plus graceful-degradation handling so a
store failure logs and falls back to a stateless call. demo-app is the first consumer: its
`AgentController` injects the optional `ConversationMemory` bean and threads an optional
`conversationId` from the request through the `project` route only; the `docs` (RAG) route and
`/api/agent/summarize` stay stateless. When the memory bean is absent (`enabled=false` or no
backend infra), `AgentController` behaves exactly as today.

Schema initialization for persistent backends reuses Spring AI's
`JdbcChatMemoryRepository` schema initializer (the Spring AI JDBC chat-memory artifact ships
`schema-@@platform@@.sql` scripts and a `JdbcChatMemoryRepositorySchemaInitializer` gated on a
property). We do **not** introduce Flyway/Liquibase. Both `jdbc` and `pgvector` backends use the
same `JdbcChatMemoryRepository` over the application's existing `DataSource` (the pgvector
backend is the same Postgres instance RAG already uses via `docker compose`, so the chat-memory
tables live alongside the vector store in one database); `pgvector` therefore differs from
`jdbc` only in that it is documented/intended for the shared RAG Postgres and selects the
PostgreSQL dialect explicitly. Schema creation is controlled by a new property
`ai.dlc.agent.memory.initialize-schema` (default `true`), wired to the Spring AI initializer.

## Components

| Component | Module | Package | Responsibility |
|---|---|---|---|
| `ConversationMemory` | spring-ai-dlc-core | `com.aidlc.spring.core.agent.memory` | Facade over Spring AI `ChatMemory`; `advise(ChatClient.ChatClientRequestSpec, conversationId)` attaches a `MessageChatMemoryAdvisor`; `isEnabled()`; graceful-degradation wrapper around store reads/writes |
| `MemoryBackend` | spring-ai-dlc-core | `com.aidlc.spring.core.agent.memory` | Enum `IN_MEMORY`, `JDBC`, `PGVECTOR` — the typed `backend` selector |
| `AiDlcMemoryAutoConfiguration` | spring-ai-dlc-autoconfigure | `com.aidlc.spring.autoconfigure` | `@ConditionalOnProperty(ai.dlc.agent.memory.enabled=true)`; provides `ChatMemoryRepository`, `ChatMemory`, and `ConversationMemory` beans, all `@ConditionalOnMissingBean` |
| `AiDlcProperties.Agent.Memory` | spring-ai-dlc-autoconfigure | `com.aidlc.spring.autoconfigure` | New nested record: `enabled`, `maxMessages`, `backend`, `initializeSchema` under `ai.dlc.agent.memory.*` |
| `AgentController` (modify) | demo-app | `com.aidlc.spring.demo.agent` | Adds optional `conversationId` to request; applies `ConversationMemory` to the `project` route only |

### Backend selection strategy

`AiDlcMemoryAutoConfiguration` declares one `ChatMemoryRepository` bean whose body switches on
`properties.agent().memory().backend()`:

- `IN_MEMORY` (default) -> `new InMemoryChatMemoryRepository()`. No external infra.
- `JDBC` -> `JdbcChatMemoryRepository.builder().jdbcTemplate(jdbcTemplate).build()` using the
  application `DataSource`/`JdbcTemplate`. The repository bean method takes
  `ObjectProvider<JdbcTemplate>`; if absent, it throws an `IllegalStateException` with an
  actionable message ("backend=jdbc requires a DataSource; add a JDBC driver + spring.datasource.*"),
  satisfying the fail-fast requirement.
- `PGVECTOR` -> same `JdbcChatMemoryRepository` over the existing (RAG) Postgres `DataSource`,
  with `dialect(new PostgresChatMemoryRepositoryDialect())` set explicitly. Fails fast the same
  way if no `DataSource` is present.

The `ChatMemory` bean is always `MessageWindowChatMemory.builder().chatMemoryRepository(repo)
.maxMessages(properties.agent().memory().maxMessages()).build()`, enforcing the bound and
oldest-first eviction. The repository bean methods are `@ConditionalOnMissingBean`, so an app
can supply its own `ChatMemoryRepository` and the starter backs off; likewise for `ChatMemory`
and `ConversationMemory`.

### Schema-initialization decision

Persistent backends rely on Spring AI's `JdbcChatMemoryRepositorySchemaInitializer`
(packaged with `spring-ai-starter-model-chat-memory-repository-jdbc`), which runs the bundled
`org/springframework/ai/chat/memory/repository/jdbc/schema-postgresql.sql` script at startup
when enabled. We surface this through `ai.dlc.agent.memory.initialize-schema` (default `true`)
and let the Spring AI starter's own autoconfiguration create the initializer bean; the AI-DLC
autoconfigure module does not hand-roll DDL. This matches the existing
`spring.ai.vectorstore.pgvector.initialize-schema: true` pattern already used for RAG. No
Flyway/Liquibase is introduced (explicitly out of scope).

## API & Data

- `POST /api/agent/ask`
  - request (modify): `record AskRequest(String question, String conversationId)` —
    `conversationId` optional; blank/absent => stateless.
  - response (unchanged): `record AskResponse(String answer)`
  - Memory applies only to the `project` route; the `docs` route and `/api/agent/summarize`
    remain stateless.
- `ConversationMemory` facade (core) shape:
  - `boolean isEnabled()`
  - `ChatClient.ChatClientRequestSpec advise(ChatClient.ChatClientRequestSpec request, String conversationId)`
    — returns the request with a `MessageChatMemoryAdvisor` bound to `conversationId` when the id
    is non-blank, otherwise the request unchanged. Catches store exceptions, logs, and returns
    the un-advised request (graceful degradation).
- New `ai.dlc.*` config (nested under existing `Agent` record):
  - `ai.dlc.agent.memory.enabled` — `boolean`, default `false`, gates the whole capability.
  - `ai.dlc.agent.memory.max-messages` — `int`, default `20`, the `MessageWindowChatMemory`
    retention bound (oldest messages evicted beyond this).
  - `ai.dlc.agent.memory.backend` — enum `in-memory` | `jdbc` | `pgvector`, default `in-memory`.
  - `ai.dlc.agent.memory.initialize-schema` — `boolean`, default `true`, whether the JDBC
    chat-memory schema initializer runs (no effect for `in-memory`).

## Files to Create/Modify

- `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/agent/memory/ConversationMemory.java` (new)
- `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/agent/memory/MemoryBackend.java` (new)
- `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/agent/memory/package-info.java` (new)
- `spring-ai-dlc-autoconfigure/src/main/java/com/aidlc/spring/autoconfigure/AiDlcMemoryAutoConfiguration.java` (new)
- `spring-ai-dlc-autoconfigure/src/main/java/com/aidlc/spring/autoconfigure/AiDlcProperties.java` (modify — add nested `Memory` record under `Agent`)
- `spring-ai-dlc-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (modify — register `AiDlcMemoryAutoConfiguration`)
- `spring-ai-dlc-autoconfigure/build.gradle.kts` (modify — add Spring AI chat-memory + JDBC chat-memory-repository dependencies)
- `demo-app/src/main/java/com/aidlc/spring/demo/agent/AgentController.java` (modify — `conversationId` field; apply `ConversationMemory` to project route)
- `demo-app/src/main/resources/application.yml` (modify — add `ai.dlc.agent.memory.*` defaults / example)
- `gradle/libs.versions.toml` (modify — add `spring-ai-chat-memory` / `spring-ai-starter-model-chat-memory-repository-jdbc` library aliases)
- `spring-ai-dlc-core/build.gradle.kts` (modify — add `spring-ai-client-chat` already present; add chat-memory api dependency if `ChatMemory`/`MessageChatMemoryAdvisor` types are not transitively on the core classpath)
- `spring-ai-dlc-autoconfigure/src/test/java/com/aidlc/spring/autoconfigure/AiDlcMemoryAutoConfigurationTest.java` (new — backend selection, conditional-on-missing-bean, fail-fast on missing DataSource)
- `spring-ai-dlc-autoconfigure/src/test/java/com/aidlc/spring/autoconfigure/AiDlcPropertiesTest.java` (modify — assert `ai.dlc.agent.memory.*` defaults/binding)
- `spring-ai-dlc-core/src/test/java/com/aidlc/spring/core/agent/memory/ConversationMemoryTest.java` (new — advise attaches advisor for non-blank id, no-ops for blank, degrades on store error)
- `demo-app/src/test/java/com/aidlc/spring/demo/agent/AgentControllerTest.java` (modify — stateful vs stateless ask, summarize unaffected)

## Risks

- **Spring AI 2.0 API/coordinate exactness.** The exact artifact id
  (`spring-ai-starter-model-chat-memory-repository-jdbc`) and builder/dialect class names
  (`JdbcChatMemoryRepository.builder()`, `PostgresChatMemoryRepositoryDialect`,
  `MessageWindowChatMemory.builder()`) must be confirmed against the resolved 2.0.0 BOM during
  `/spec-tasks`/implementation; if names differ, the autoconfigure wiring must adjust. Do not
  guess coordinates without checking the BOM.
- **jdbc vs pgvector overlap.** As designed, both use `JdbcChatMemoryRepository` over the same
  `DataSource`; `pgvector` is essentially `jdbc` pointed at the RAG Postgres. This keeps scope
  small but means the `pgvector` selector adds little beyond intent/dialect — reviewers may
  prefer collapsing them, or a true pgvector-native store may be expected later.
- **Schema initializer ownership.** Relying on Spring AI's
  `JdbcChatMemoryRepositorySchemaInitializer` means schema creation is governed by the Spring AI
  starter's own property/condition; our `ai.dlc.agent.memory.initialize-schema` must be mapped
  onto whatever property the starter exposes (e.g.
  `spring.ai.chat.memory.repository.jdbc.initialize-schema`) rather than re-implementing DDL.
- **Fail-fast vs lazy DataSource.** Throwing in the `ChatMemoryRepository` bean method requires
  the `DataSource` absence to be detectable at context refresh; with `ObjectProvider` this is
  fine, but ordering relative to Spring Boot's `DataSourceAutoConfiguration` should be asserted in
  the autoconfigure test.
- **Advisor and tool-calling interaction.** The `project` route runs through `AgentFactory`/`Agent`
  (its own `ChatClient` build with a tool-calling loop). Threading a per-request
  `MessageChatMemoryAdvisor` may require the controller to build the project agent's request via
  `ConversationMemory.advise(...)` rather than relying on `Agent.run`, since `Agent` currently
  takes only a `userMessage`. The controller wiring (not `Agent`) is the integration point;
  whether `Agent`/`AgentFactory` need a memory-aware overload is an open question for `/spec-tasks`.

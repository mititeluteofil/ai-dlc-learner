# conversation-memory — Tasks

Each task is <= 1 hour. Format:

`- [ ] T<n>: <verb phrase> — files: <comma-separated paths> — verify: <command>`

## Build/dependencies

- [ ] T1: Add `spring-ai-chat-memory` and `spring-ai-starter-model-chat-memory-repository-jdbc` library aliases (confirm exact coordinates/class names against the resolved Spring AI 2.0.0 BOM) — files: `gradle/libs.versions.toml` — verify: `./gradlew :spring-ai-dlc-core:dependencies`
- [ ] T2: Wire the new chat-memory dependencies into `spring-ai-dlc-core` (api dependency for `ChatMemory`/`MessageChatMemoryAdvisor` types) and `spring-ai-dlc-autoconfigure` (chat-memory + JDBC chat-memory-repository starter) — files: `spring-ai-dlc-core/build.gradle.kts`, `spring-ai-dlc-autoconfigure/build.gradle.kts` — verify: `./gradlew :spring-ai-dlc-autoconfigure:build`

## Core

- [ ] T3: Add `MemoryBackend` enum (`IN_MEMORY`, `JDBC`, `PGVECTOR`) and package-info for the new memory package — files: `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/agent/memory/MemoryBackend.java`, `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/agent/memory/package-info.java` — verify: `./gradlew :spring-ai-dlc-core:test`
- [ ] T4: Add `ConversationMemory` facade wrapping `ChatMemory`: `isEnabled()`, `advise(ChatClient.ChatClientRequestSpec, conversationId)` that attaches `MessageChatMemoryAdvisor` for non-blank ids, no-ops for blank ids, and degrades gracefully (log + unadvised request) on store errors — files: `spring-ai-dlc-core/src/main/java/com/aidlc/spring/core/agent/memory/ConversationMemory.java` — verify: `./gradlew :spring-ai-dlc-core:test`
- [ ] T5: Unit tests for `ConversationMemory`: advisor attached for non-blank conversation id, request unchanged for blank id, graceful degradation when the underlying store throws — files: `spring-ai-dlc-core/src/test/java/com/aidlc/spring/core/agent/memory/ConversationMemoryTest.java` — verify: `./gradlew :spring-ai-dlc-core:test`

## Autoconfigure

- [ ] T6: Add nested `Memory` record (`enabled` default `false`, `maxMessages` default `20`, `backend` default `in-memory`, `initializeSchema` default `true`) under `AiDlcProperties.Agent` for `ai.dlc.agent.memory.*` — files: `spring-ai-dlc-autoconfigure/src/main/java/com/aidlc/spring/autoconfigure/AiDlcProperties.java` — verify: `./gradlew :spring-ai-dlc-autoconfigure:test`
- [ ] T7: Extend `AiDlcPropertiesTest` to assert `ai.dlc.agent.memory.*` defaults and custom binding — files: `spring-ai-dlc-autoconfigure/src/test/java/com/aidlc/spring/autoconfigure/AiDlcPropertiesTest.java` — verify: `./gradlew :spring-ai-dlc-autoconfigure:test`
- [ ] T8: Add `AiDlcMemoryAutoConfiguration`, `@ConditionalOnProperty(ai.dlc.agent.memory.enabled=true)`, providing `@ConditionalOnMissingBean` beans: `ChatMemoryRepository` (switch on `backend` — `IN_MEMORY` -> `InMemoryChatMemoryRepository`, `JDBC`/`PGVECTOR` -> `JdbcChatMemoryRepository` over `ObjectProvider<JdbcTemplate>` with fail-fast `IllegalStateException` if absent, `PGVECTOR` sets `PostgresChatMemoryRepositoryDialect`), `ChatMemory` (`MessageWindowChatMemory` bounded by `maxMessages`), and `ConversationMemory` — files: `spring-ai-dlc-autoconfigure/src/main/java/com/aidlc/spring/autoconfigure/AiDlcMemoryAutoConfiguration.java` — verify: `./gradlew :spring-ai-dlc-autoconfigure:test`
- [ ] T9: Register `AiDlcMemoryAutoConfiguration` in the autoconfiguration imports file — files: `spring-ai-dlc-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — verify: `./gradlew :spring-ai-dlc-autoconfigure:test`
- [ ] T10: `ApplicationContextRunner` tests for `AiDlcMemoryAutoConfiguration`: disabled by default (no beans), `in-memory` backend wires `InMemoryChatMemoryRepository`, `jdbc`/`pgvector` wire `JdbcChatMemoryRepository` when a `DataSource`/`JdbcTemplate` is present, fail-fast `IllegalStateException` when `jdbc`/`pgvector` configured without a `DataSource`, and user-supplied `ChatMemoryRepository`/`ChatMemory` beans are respected (`@ConditionalOnMissingBean` back-off) — files: `spring-ai-dlc-autoconfigure/src/test/java/com/aidlc/spring/autoconfigure/AiDlcMemoryAutoConfigurationTest.java` — verify: `./gradlew :spring-ai-dlc-autoconfigure:test`

## Demo wiring

- [ ] T11: Add optional `conversationId` to `AskRequest`; in the `project` route build the request via `ConversationMemory.advise(...)` when the optional `ConversationMemory` bean is present and `conversationId` is non-blank; `docs` route and `/api/agent/summarize` remain unchanged/stateless — files: `demo-app/src/main/java/com/aidlc/spring/demo/agent/AgentController.java` — verify: `./gradlew :demo-app:build`
- [ ] T12: Add `ai.dlc.agent.memory.*` example/defaults to demo `application.yml` (e.g. `enabled: true`, `max-messages: 20`, `backend: in-memory`, `initialize-schema: true`, with comments on `jdbc`/`pgvector` options) — files: `demo-app/src/main/resources/application.yml` — verify: `./gradlew :demo-app:build`
- [ ] T13: Extend `AgentControllerTest` to cover: stateful `ask` with `conversationId` advises memory, stateless `ask` without `conversationId` unchanged, `summarize` unaffected by memory bean presence — files: `demo-app/src/test/java/com/aidlc/spring/demo/agent/AgentControllerTest.java` — verify: `./gradlew :demo-app:test`

## Full verification

- [ ] T14: Run the full build to confirm all modules compile and tests pass together — files: (none) — verify: `./gradlew build`

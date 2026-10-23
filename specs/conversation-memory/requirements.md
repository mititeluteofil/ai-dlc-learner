# conversation-memory — Requirements

## Intent

The `/api/agent/ask` endpoint currently treats every request as a stand-alone
question with no memory of prior turns. More fundamentally, the AI-DLC starter
ships no conversation-memory capability at all, so every application built on it
must wire `ChatMemory` by hand — which is exactly the kind of production-grade
plumbing this template exists to provide.

This feature adds conversation memory as a **first-class, autoconfigured library
capability** in `spring-ai-dlc-core` / `spring-ai-dlc-autoconfigure` (not just
demo-app glue), exposed through Spring AI's `ChatMemory` / `ChatMemoryRepository`
abstractions. Memory is keyed by a client-supplied conversation/session id and
backed by a **pluggable, configuration-selected store**: an in-memory default for
local dev and tests, and a persistent backend (JDBC, and the pgvector-backed
datastore already used for RAG) for production deployments. The `/api/agent/ask`
endpoint is the first consumer that demonstrates the capability end-to-end.

## User Stories

- As a client application, I want to pass a `conversationId` when calling
  `/api/agent/ask`, so that follow-up questions are answered with awareness of
  earlier turns in the same conversation.
- As a client application, I want to omit the `conversationId` for one-off questions,
  so that I don't have to manage session state for stateless use cases.
- As an application developer using the starter, I want a `ChatMemory` bean
  autoconfigured from `ai.dlc.*` properties, so that I get production-grade
  conversation memory by adding the starter and a few config lines — without
  writing my own repository/advisor wiring.
- As an operator running in production, I want to select a persistent memory
  backend (JDBC or pgvector) via configuration, so that conversation history
  survives restarts and is shared across instances, rather than being lost with an
  in-process map.
- As an operator, I want conversation history to be bounded (e.g. a max number of
  messages retained per conversation), so that memory growth doesn't cause
  unbounded resource usage or runaway prompt sizes.
- As a developer extending this template, I want to override the autoconfigured
  memory store with my own `ChatMemoryRepository`/`ChatMemory` bean, so that I can
  plug in a custom backend without forking the library.

## EARS Acceptance Criteria

### Library capability (core + autoconfigure)

- The system shall provide an autoconfigured `ChatMemory` bean in
  `spring-ai-dlc-autoconfigure`, conditional on memory being enabled and consistent
  with the existing `@ConditionalOnMissingBean` convention so applications can
  override it.
- The system shall expose memory configuration under the `ai.dlc.agent.memory.*`
  namespace, including at minimum an `enabled` flag, a `max-messages` bound, and a
  `backend` selector, consistent with existing `ai.dlc.*` property conventions.
- The system shall support a `backend` of at least `in-memory` (default), `jdbc`,
  and `pgvector`, selecting the corresponding `ChatMemoryRepository` implementation.
- When `backend=in-memory`, the system shall use an in-process repository requiring
  no external datastore, suitable for local dev and tests.
- When `backend=jdbc`, the system shall persist conversation history through a
  JDBC-backed `ChatMemoryRepository` using the application's configured `DataSource`,
  so that history survives application restarts.
- When `backend=pgvector`, the system shall persist conversation history in the same
  PostgreSQL datastore used by the RAG pipeline (per `docker compose` local setup),
  reusing the existing database connection configuration.
- If the configured backend's required infrastructure is absent (e.g. no
  `DataSource` for `jdbc`), then the system shall fail fast at startup with a clear,
  actionable error message rather than silently falling back.
- Where an application already defines its own `ChatMemory` or `ChatMemoryRepository`
  bean, the system shall back off and use the application-provided bean.

### `/api/agent/ask` behavior

- The system shall accept an optional `conversationId` field in the
  `POST /api/agent/ask` request body (in addition to the existing `question` field).
- When a request to `/api/agent/ask` includes a non-blank `conversationId`, the
  system shall retrieve prior messages for that conversation id and include them as
  context for the chat model before sending the current question.
- When a request to `/api/agent/ask` includes a non-blank `conversationId`, the
  system shall persist the user's question and the assistant's response under that
  conversation id after the response is produced.
- When a request to `/api/agent/ask` omits `conversationId` (or it is blank), the
  system shall process the request without reading or writing any conversation
  memory, preserving current stateless behavior.
- The system shall scope stored conversation history per `conversationId`, such that
  messages from one conversation id are never used as context for a different
  conversation id.
- While the number of stored messages for a conversation exceeds
  `ai.dlc.agent.memory.max-messages`, the system shall evict the oldest messages so
  that only the most recent messages within the limit are retained as context.
- If `ai.dlc.agent.memory.enabled=false`, then the system shall not autoconfigure a
  `ChatMemory` bean and `/api/agent/ask` shall ignore any supplied `conversationId`,
  behaving exactly as it does today.
- If the underlying memory store throws an error while retrieving or persisting
  history at request time, then the system shall log the error and still return a
  best-effort answer for the current question without memory context, rather than
  failing the request.
- Where the `/api/agent/summarize` endpoint or other non-conversational workflows are
  invoked, the system shall not apply conversation memory to them.

## Out of Scope

- Long-term/cross-session memory summarization or compaction strategies beyond a
  simple message-count eviction limit (semantic summarization may be a later feature).
- Authentication, authorization, or per-user access control on conversation history,
  and tenant isolation beyond the `conversationId` key.
- Exposing an HTTP API to list, retrieve, or delete conversation history directly.
- Conversation memory for the `RoutingWorkflow`'s `docs` route (RAG pipeline) or the
  `summarize` chain workflow.
- Client-side conversation id generation/validation rules (e.g. UUID format
  enforcement) beyond basic blank/non-blank checks.
- Database schema migration tooling (e.g. Flyway/Liquibase) for the persistent
  backends; the design phase decides how the chat-memory table is created/initialized.
- Memory backends beyond `in-memory`, `jdbc`, and `pgvector` (e.g. Redis, Cassandra).

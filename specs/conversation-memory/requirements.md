# conversation-memory — Requirements

## Intent

The `/api/agent/ask` endpoint currently treats every request as a stand-alone
question with no memory of prior turns. This feature adds optional, per-conversation
chat memory (via Spring AI's `ChatMemory` abstraction) so a client can carry on a
multi-turn conversation by supplying a conversation/session id.

## User Stories

- As a client application, I want to pass a `conversationId` when calling
  `/api/agent/ask`, so that follow-up questions are answered with awareness of
  earlier turns in the same conversation.
- As a client application, I want to omit the `conversationId` for one-off questions,
  so that I don't have to manage session state for stateless use cases.
- As an operator, I want conversation history to be bounded (e.g. a max number of
  messages/tokens retained per conversation), so that memory growth doesn't cause
  unbounded resource usage or runaway prompt sizes.
- As a developer extending this template, I want the memory mechanism to plug into
  the existing `Agent`/`AgentFactory`/`RoutingWorkflow` abstractions via Spring AI's
  `ChatMemory`/advisor support, so that other agents and workflows can reuse it
  without bespoke wiring.

## EARS Acceptance Criteria

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
- While the number of stored messages for a conversation exceeds a configured limit
  (`ai.dlc.agent.memory.max-messages`), the system shall evict the oldest messages so
  that only the most recent messages within the limit are retained as context.
- The system shall expose memory configuration (enabled flag, max messages, and
  storage backend selection) under the `ai.dlc.agent.memory.*` configuration
  namespace, consistent with existing `ai.dlc.*` property conventions.
- If `ai.dlc.agent.memory.enabled=false`, then the system shall ignore any
  `conversationId` supplied in the request and behave as if memory were not
  configured.
- If the underlying `ChatMemory` store is unavailable or throws an error while
  retrieving or persisting history, then the system shall log the error and still
  return a best-effort answer for the current question without memory context,
  rather than failing the request.
- Where the `/api/agent/summarize` endpoint or other non-conversational workflows are
  invoked, the system shall not apply conversation memory to them.

## Out of Scope

- Long-term/cross-session memory summarization or compaction strategies beyond a
  simple message-count eviction limit.
- Authentication, authorization, or per-user access control on conversation history.
- A persistent (e.g. JDBC/pgvector-backed) `ChatMemory` implementation — initial
  scope covers in-memory storage with configuration hooks for swapping backends
  later; choice of default backend is decided in the design phase.
- Exposing an API to list, retrieve, or delete conversation history directly.
- Conversation memory for the `RoutingWorkflow`'s `docs` route (RAG pipeline) or the
  `summarize` chain workflow.
- Client-side conversation id generation/validation rules (e.g. UUID format
  enforcement) beyond basic blank/non-blank checks.

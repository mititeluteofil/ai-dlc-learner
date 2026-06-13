# docs-rag-endpoint — Requirements

## Intent

The demo app should let users ask natural-language questions about this repo's own
documentation (implementation plan, READMEs) and get back an answer grounded in those
docs with citations. This proves out the `IngestionPipeline`/`RagPipeline` facades end-to-end.

## User Stories

- As a developer exploring this template, I want to ask "How do I switch to OpenAI?" and get
  an answer sourced from the repo's docs, so that I don't have to read every file manually.
- As a developer, I want to trigger re-ingestion of the docs corpus on demand, so that I can
  pick up doc changes without restarting the app.
- As an operator, I want the docs corpus to be ingested automatically on startup, so that the
  RAG endpoint works immediately after `docker compose up` + `bootRun`.

## EARS Acceptance Criteria

- The system shall expose `POST /api/rag/ask` accepting `{"question": "<text>"}` and returning
  an answer plus a list of citations (source, snippet, score).
- The system shall expose `POST /api/rag/ingest` that (re-)ingests all markdown files under
  `classpath:rag-corpus/**/*.md` and returns an `IngestionReport`.
- When `ai.dlc.rag.ingest-on-startup.enabled=true`, the system shall ingest the configured
  classpath location pattern automatically at application startup.
- While the configured embedding model and pgvector table are consistent with
  `ai.dlc.rag.*` defaults (chunk size 800, overlap 100, top-K 4), the system shall retrieve
  relevant chunks for a query without additional configuration.
- If re-ingestion is run on documents already ingested, then the system shall upsert
  (not duplicate) chunks via deterministic document IDs.

## Out of Scope

- Authentication/authorization on the RAG endpoints.
- Non-markdown document sources (PDF, URL, etc.) for this corpus — `DocumentSource` supports
  them, but the demo corpus is markdown-only.
- Streaming responses.

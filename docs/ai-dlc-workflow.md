# AI-DLC workflow in this repo

This repo's `.claude/` scaffold implements **AI-DLC** (AWS's AI Development Lifecycle:
**Inception -> Construction -> Operations**) as a set of slash commands, each backed by a
subagent with a tight file allow-list. For the full mechanics (file structure, templates,
review gates) see [`specs/README.md`](../specs/README.md); this page maps the AI-DLC phases
to what was actually built in Phase 7.

## Phase mapping

| AI-DLC phase | What happens | Command | Subagent | Output |
|---|---|---|---|---|
| **Inception** | Idea -> user stories + EARS acceptance criteria | `/spec-init <feature> "<idea>"` | `requirements-analyst` | `specs/<feature>/requirements.md` |
| **Inception -> Construction** | Requirements -> components, API shape, explicit files to touch | `/spec-design [feature]` | `solution-architect` | `specs/<feature>/design.md` |
| **Construction (planning)** | Design -> ordered, checkbox tasks with verify commands | `/spec-tasks [feature]` | `task-planner` | `specs/<feature>/tasks.md` |
| **Construction (execution)** | Implement one task, then test it; checkbox ticked only on green tests | `/implement <feature> [task-N]` | `implementer` + `test-engineer` | source + test files, updated `tasks.md` |
| **Operations** | Diff the code against the spec before merging | `/spec-review <feature>` | `spec-auditor` | report only (read-only) |
| (any phase) | Near-zero-token progress check | `/spec-status` | none (greps checkboxes) | progress table |

Every transition between phases is a **human review gate** - the command prints the path it
wrote and a one-line reminder of what to review before moving on.

## A full walkthrough

```
/spec-init add-streaming-endpoint "Stream RAG answers token-by-token over SSE"
# -> review specs/add-streaming-endpoint/requirements.md

/spec-design add-streaming-endpoint
# -> review specs/add-streaming-endpoint/design.md, especially "Files to Create/Modify"

/spec-tasks add-streaming-endpoint
# -> review the task order and verify: commands in tasks.md

/implement add-streaming-endpoint task-1
# -> implementer writes code + ticks the box, test-engineer runs the verify command
# repeat /implement for each remaining task

/spec-review add-streaming-endpoint
# -> spec-auditor diffs git status/diff against design.md + tasks.md

/spec-status
# -> one-line-per-feature progress table across all of specs/
```

## Worked example

[`specs/docs-rag-endpoint/`](../specs/docs-rag-endpoint/) documents the docs-RAG feature
(`RagController`, `IngestionPipeline`/`RagPipeline` wiring, `rag-corpus/`) that was actually
built in Phase 6, written retroactively as a worked example. Use its `requirements.md`,
`design.md`, and `tasks.md` as pattern references - especially the "Files to Create/Modify"
list in `design.md` and the `- [x] T<n>: ... — files: … — verify: …` task format - when
writing specs for new features.

## Subagents at a glance

Each subagent's prompt (in `.claude/agents/`) enumerates exactly which files it may read; if a
needed file isn't on that list, the subagent stops and reports rather than exploring the repo.
This keeps every phase token-efficient and keeps `spec-auditor`'s diff meaningful (it compares
real changes against the explicit file lists in `design.md`/`tasks.md`).

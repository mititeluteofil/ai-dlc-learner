# specs/ — spec-driven development workflow

This repo follows a spec-first workflow modeled on **AWS AI-DLC** (Inception -> Construction ->
Operations) and Kiro-style specs. Every non-trivial feature gets a `specs/<feature>/` directory
with three files, written in order, each with a **human review gate** before the next step:

```
specs/<feature>/
├── requirements.md   # Inception: idea -> user stories + EARS acceptance criteria
├── design.md         # Inception -> Construction: components, API, files to touch
└── tasks.md          # Construction planning: ordered checkbox tasks with verify commands
```

## The flow

| Step | Command | Subagent | Output | AI-DLC phase |
|---|---|---|---|---|
| 1 | `/spec-init <feature> "<idea>"` | `requirements-analyst` | `requirements.md` | Inception |
| **gate** | *human reviews requirements.md* | | | |
| 2 | `/spec-design [feature]` | `solution-architect` | `design.md` | Inception -> Construction |
| **gate** | *human reviews design.md, esp. "Files to Create/Modify"* | | | |
| 3 | `/spec-tasks [feature]` | `task-planner` | `tasks.md` | Construction planning |
| **gate** | *human reviews task order & verify commands* | | | |
| 4 | `/implement <feature> [task-N]` | `implementer` + `test-engineer` | source + tests, checkbox ticked on green | Construction execution |
| 5 | `/spec-review <feature>` | `spec-auditor` | report only | Operations |
| any time | `/spec-status` | (no agent — grep) | progress table | — |

Repeat step 4 for each task. Step 5 can run after each task or once at the end, before merging.

## Why this exists

- **Token efficiency**: each subagent has a tight, explicit allow-list of files it may read
  (see each agent's prompt in `.claude/agents/`). If a subagent thinks it needs a file outside
  its list, it stops and reports instead of exploring the whole repo.
- **Traceability**: `design.md`'s "Files to Create/Modify" and `tasks.md`'s `files:`/`verify:`
  fields let `spec-auditor` mechanically check that what was built matches what was planned.
- **Small, verifiable steps**: every task is <= 1 hour and has a concrete `verify:` command
  (usually a scoped `./gradlew :<module>:test`).

## See also

- `specs/templates/` — the exact structure each file must follow.
- `specs/docs-rag-endpoint/` — a worked example written truthfully against the real
  docs-RAG feature already built in this repo (Phase 6). Use it as a pattern reference.
- `CLAUDE.md` — repo-wide rules, including "never write code without a `tasks.md` entry".

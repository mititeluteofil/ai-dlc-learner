---
description: Invoke spec-auditor to diff code vs spec for a feature
argument-hint: <feature>
---

Invoke the **spec-auditor** subagent for feature `$1`. It reads `specs/$1/design.md` and `specs/$1/tasks.md`, runs `git status` / `git diff --name-only`, and reports (without writing anything) which files match, are missing, are undocumented, or have out-of-sync checkboxes.

Print the report verbatim. This is the **Operations** review gate before merging/closing the feature.

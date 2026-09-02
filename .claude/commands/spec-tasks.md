---
description: Invoke task-planner to turn design.md into tasks.md for a feature
argument-hint: [feature]
---

Determine the target feature: if `$1` is given, use it; otherwise use the most recently modified feature directory under `specs/` (excluding `templates/`) that has a `design.md` but no `tasks.md` yet, or the most recently modified feature directory overall.

Invoke the **task-planner** subagent for that feature. It should read `specs/<feature>/design.md` and `specs/templates/tasks.template.md`, and write `specs/<feature>/tasks.md` with ordered, unchecked `- [ ] T<n>: ... — files: ... — verify: ...` items.

After it finishes, print the path written and the task count, and remind the user this is the **Construction planning** checkpoint: review the task order and verify commands before running `/implement <feature> task-1`.

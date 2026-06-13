---
description: Invoke solution-architect to turn requirements.md into design.md for a feature
argument-hint: [feature]
---

Determine the target feature: if `$1` is given, use it; otherwise use the most recently modified directory under `specs/` (excluding `templates/`) that has a `requirements.md` but no `design.md` yet, or the most recently modified feature directory overall.

Invoke the **solution-architect** subagent for that feature. It should read `specs/<feature>/requirements.md` and `specs/templates/design.template.md`, and write `specs/<feature>/design.md`.

After it finishes, print the path written and remind the user this is the **Inception -> Construction** checkpoint: review `specs/<feature>/design.md` (especially "Files to Create/Modify") before running `/spec-tasks`.

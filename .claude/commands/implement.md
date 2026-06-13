---
description: Implement and test one task from specs/<feature>/tasks.md; ticks the checkbox only if tests pass
argument-hint: <feature> [task-N]
---

Feature: `$1`. Task: `$2` (if omitted, use the first unchecked `- [ ]` task in `specs/$1/tasks.md`, in file order).

Steps:
1. Invoke the **implementer** subagent for feature `$1`, task `$2`. It implements the change and ticks the checkbox in `specs/$1/tasks.md` for that task.
2. Invoke the **test-engineer** subagent for the same feature/task. It writes/updates tests and runs the task's `verify:` command.
3. If test-engineer reports **PASS**: the checkbox (already ticked by implementer) stays ticked — confirm this to the user.
   If test-engineer reports **FAIL**: revert the checkbox for that task back to `- [ ]` in `specs/$1/tasks.md` (the task is not done until tests are green), and report the failure summary so the user can re-run `/implement $1 $2` after a fix.

Report a one-line result: task ID, PASS/FAIL, and final checkbox state.

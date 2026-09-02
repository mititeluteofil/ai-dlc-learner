---
description: Start a new feature spec — invokes requirements-analyst to write specs/<feature>/requirements.md
argument-hint: <feature> "<idea>"
---

Invoke the **requirements-analyst** subagent for feature `$1` with idea/prompt: $2

The subagent should write `specs/$1/requirements.md` following `specs/templates/requirements.template.md`, using the worked example at `specs/docs-rag-endpoint/requirements.md` as a pattern reference for tone and structure.

After it finishes, print the path written and remind the user this is an AI-DLC **Inception** checkpoint: review `specs/$1/requirements.md` before running `/spec-design`.

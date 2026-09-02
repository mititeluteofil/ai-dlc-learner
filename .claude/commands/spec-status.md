---
description: Near-zero-token summary of checkbox progress across all specs/*/tasks.md
---

Run this directly (no subagent needed):

```bash
for f in specs/*/tasks.md; do
  [ -f "$f" ] || continue
  feature=$(basename "$(dirname "$f")")
  total=$(grep -cE '^\s*-\s\[[ xX]\]' "$f")
  done=$(grep -cE '^\s*-\s\[[xX]\]' "$f")
  printf '%-24s %2d/%2d done\n' "$feature" "$done" "$total"
done
```

Print the output as a simple table: feature name, done/total tasks. If no `specs/*/tasks.md` files exist, say so.

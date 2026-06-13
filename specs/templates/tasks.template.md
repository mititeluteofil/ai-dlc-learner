# <Feature Name> — Tasks

Each task is <= 1 hour. Format:

`- [ ] T<n>: <verb phrase> — files: <comma-separated paths> — verify: <command>`

## <Group 1, e.g. Core>

- [ ] T1: <description> — files: <paths> — verify: `./gradlew :<module>:test`

## <Group 2, e.g. Autoconfigure>

- [ ] T2: <description> — files: <paths> — verify: `./gradlew :<module>:test`

## <Group 3, e.g. Demo wiring>

- [ ] T3: <description> — files: <paths> — verify: `./gradlew :demo-app:build`

## <Group 4, e.g. Tests>

- [ ] T4: <description> — files: <paths> — verify: `./gradlew test`

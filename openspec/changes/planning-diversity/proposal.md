## Why

`generateTopSchedules` is supposed to return several *distinct* planning options so a human can choose between real alternatives. In practice it returns ~10 plannings that differ by only one or two slots — they look identical. The search converges into a single high-score basin and the post-hoc diversity step has nothing diverse to pick from, so the feature provides no usable choice.

## What Changes

- Add a **score-tolerance band** so the candidate pool keeps complete plannings that are slightly below the best score instead of only near-optimal clones (driven by aggressive optimistic-score pruning today).
- Make restarts **genuinely stochastic**: randomize driver selection among near-equal candidates (currently random is only the last tie-break key, so reseeding barely changes the explored tree).
- Enforce a **minimum pairwise distance** when admitting candidates to the pool, using the existing `planningDistance`, so the pool is diverse *before* final selection rather than relying solely on post-hoc max-min selection.
- Make diversity **tunable** via planner parameters (tolerance, min-distance, restarts) wired through the CLI.
- **Variety is prioritized over marginal score**: the band may be generous and the minimum-distance rule may promote a more distant, slightly-lower-scored planning into ranks 2..N. The top-1 candidate remains the best planning actually found (never worse than what the search computed), but ranks 2..N optimize for distinctness rather than the next-best score.

## Capabilities

### New Capabilities
- `planning-diversity`: rules governing how the schedule planner produces a set of *distinct* candidate plannings — score-tolerance acceptance, stochastic restart behavior, minimum-distance pool admission, and tunable diversity parameters.

### Modified Capabilities
<!-- No existing specs under openspec/specs/; nothing to modify. -->

## Impact

- `carpool-back/.../schedule/calculator/BruteForceSchedulePlanner.java`: pruning relaxation, stochastic ordering, pool admission, new parameters.
- `carpool-back/.../workbook/normalization/WorkbookStatsCli.java`: expose diversity parameters.
- Tests: `BruteForceSchedulePlannerTest`, `ScheduleServiceTest`.
- No API/data-model changes; output count unchanged. Top-1 stays best-found; ranks 2..N favor diversity.

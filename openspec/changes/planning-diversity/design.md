## Context

`BruteForceSchedulePlanner.generateTopSchedules` runs a depth-first search over slots, scoring complete plannings and keeping a pool of candidates, then calls `selectDiverseCandidates` (max-min distance) to pick the final `topCount`. Today it returns ~10 near-identical plannings.

Three coupled causes funnel the search into a single basin, so the post-hoc diversity step has no diverse pool to choose from:

1. **Pruning** — `shouldPruneByOptimisticScore` cuts any branch that cannot beat the incumbent; with strictly lexicographic `compareScores`, only plannings at the exact score apex survive.
2. **Greedy driver order** — in `exploreSlot`, `sortedDrivers` uses random only as the last tie-break key, so the DFS always walks the same greedy order.
3. **Weak restarts** — restarts reseed only `orderedSlots` and the tie-break, so the explored tree barely moves.

Decision from the user: implement all three mechanisms (A score-tolerance band, B stochastic restarts, C minimum-distance pool admission) and **prioritize variety over marginal score** for ranks 2..N, while keeping top-1 equal to the best planning found.

## Goals / Non-Goals

**Goals:**
- Return `topCount` plannings that are pairwise distinct (≥ configured min distance) when the input allows.
- Keep top-1 = best complete planning found.
- Make tolerance, min-distance, and restarts tunable via the planner API and the CLI, with safe defaults.
- Deterministic output for a fixed seed + parameters.

**Non-Goals:**
- No change to scoring semantics (`PlanningScorer`, `compareScores`) or the planning data model.
- No new external dependency; no API/HTTP surface change.
- Not switching to a different search paradigm (e.g. metaheuristic/ILP); stays the existing DFS-with-restarts.

## Decisions

### A. Score-tolerance band relaxes pruning and pool admission
- Introduce `scoreTolerance` (integer, expressed on the same scale as `PlanningScore.totalScore` / the comparison). Define a candidate as **in-band** when it is complete and its score is within `tolerance` of the current best by `compareScores`.
- `shouldPruneByOptimisticScore`: prune only when the optimistic completion cannot reach **best − tolerance**, instead of "cannot beat best". Concretely, widen the existing `optimisticTotalScore < incumbent.totalScore()` cut by `tolerance` and gate it behind the same completeness checks already present.
- `addTopCandidate` / `addMergedCandidate`: keep admitting in-band complete plannings (dedup by `planningSignature` as today) rather than discarding anything below the incumbent.
- *Why over alternatives:* a band is the minimal change that lets structurally-different-but-slightly-worse plannings reach the pool. Alternative (remove pruning entirely) explodes the state count and breaks the time budget.

### B. Stochastic restarts via randomized driver selection
- Add `randomness` weighting so that within a restart the driver chosen among near-equal candidates is sampled, not fixed. Concretely: keep the existing sort keys (preference group, trip ratio, preference rank) as a coarse filter, but break ties / near-ties using the per-seed `random` already on `SearchState` with higher influence (e.g. group candidates whose primary keys are within an epsilon and shuffle within the group using `randomOrderFor`).
- Each restart already gets `seed + restartIndex`; this makes that seed actually matter.
- *Why over alternatives:* reuses the existing seeded `Random` and `randomOrderFor` cache, preserving per-seed determinism. Alternative (softmax over partial score) is more invasive and harder to keep deterministic.

### C. Minimum-distance pool admission + variety-first final ranking
- Add `minDistance` (0.0–1.0, reusing `planningDistance`). When merging restart candidates into the pool, prefer keeping in-band candidates that are ≥ `minDistance` from those already kept; when the pool is full, evict the closest-pair member rather than the lowest-score one, so the retained pool stays spread out.
- `selectDiverseCandidates` stays as the final greedy max-min selection but now (a) always emits the best-score candidate first (top-1 guarantee) and (b) for ranks 2..N picks by distance first, score second — already its behavior in `compareDiversityAware`; the change is that the pool feeding it is now genuinely diverse.
- *Why:* moves diversity upstream into pool admission (root cause 4) instead of relying on a homogeneous pool.

### Parameter plumbing
- Extend the `generateTopSchedules(...)` overload with `scoreTolerance`, `minDistance` (keep `restarts`, `seed` already present). Provide defaults: `restarts` ≥ a few, `scoreTolerance` modest but non-zero, `minDistance` small but non-zero — chosen so top-1 is unchanged and lower ranks diversify.
- `WorkbookStatsCli`: add flags (e.g. `--score-tolerance`, `--min-distance`, `--restarts`) passed through to the planner.

## Risks / Trade-offs

- **Wider band + more restarts increases runtime** → bounded by the existing `maxSeconds` / `maxExploredStates` deadline; the global deadline already short-circuits restarts.
- **Variety-first ranking can surface a clearly worse rank-2 planning** → the band caps how bad in-band candidates can be; tolerance is tunable and defaults conservative.
- **Min-distance may return fewer than `topCount`** when input genuinely has few alternatives → spec allows returning fewer; do not pad with near-duplicates.
- **Stochastic selection could perturb top-1 determinism** → top-1 is taken as the best-by-score from the merged pool, independent of selection randomness; per-seed determinism preserved via seeded `Random`.

## Migration Plan

Pure internal algorithm change, no data migration. New CLI flags are additive with defaults. Rollback = revert the planner/CLI changes; no persisted state affected.

## Open Questions

- Default numeric values for `scoreTolerance` and `minDistance` — calibrate against the real `covoiturage.xlsx` workbook during implementation (verify top-1 unchanged, ranks 2..N distinct).

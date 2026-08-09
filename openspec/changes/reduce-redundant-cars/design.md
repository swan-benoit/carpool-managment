# Design — Reduce Redundant Cars

## Context

`BruteForceSchedulePlanner` explores slot-by-slot driver assignments, scores results via `PlanningScorer`, then diversifies candidates through driver-swap perturbation. `PlanningScorer.computeRedundantDrivers` already detects capacity-based redundancy per slot (sorted capacities vs. children count), and `REDUNDANT_DRIVER_PENALTY = -10` folds it into `totalScore`.

Two mechanisms let redundant cars survive today:

1. **Ranking order.** `compareScores` (BruteForceSchedulePlanner.java:554) ranks: complete → assignedRequiredTransportSlots → justice.min → justice.avg → redundantDrivers → totalScore → … Justice dominates, and justice rewards each family reaching its `perfectMeanTripPerWeek` — which pushes toward *more* trips, i.e. more cars.
2. **No merge move.** The only neighbourhood move is driver swap (same car structure). Nothing ever removes a car and redistributes its children, so a redundant-car planning found by the greedy exploration can never be repaired.

Observed symptom: slot with 12 children served by 4 cars (loads 3/4/2/3) where 3 cars suffice.

## Goals / Non-Goals

**Goals:**
- Complete plannings with fewer redundant drivers rank above those with more, ahead of justice.
- A "car merge" neighbourhood move removes redundant cars when spare seats exist on the slot.
- Top-N diversification never returns a planning that is a strictly-worse redundant variant of another candidate.

**Non-Goals:**
- No change to workbook format, MCP tools, or front-end.
- No global trip-count minimization across the week — only per-slot car redundancy.
- No change to completeness semantics: completeness and assigned-slot count stay the top criteria.
- Justice model itself (`perfectMeanTripPerWeek`, deviation penalty) unchanged.

## Decisions

### D1 — Promote `redundantDrivers` in `compareScores`, above justice

New order: complete → assignedRequiredTransportSlots → **redundantDrivers (fewer wins)** → justice.min → justice.avg → totalScore → impossible → avoid → preferred.

*Why:* one comparator line move; deterministic; matches user intent ("never use a car we can drop"). Alternative considered: raise `REDUNDANT_DRIVER_PENALTY` weight — rejected because `totalScore` sits below justice anyway, so any finite penalty can be outvoted by justice.

*Consequence:* justice becomes tie-breaker among equally-tight plannings, per spec.

### D2 — Add a car-merge neighbourhood move in perturbation

New `carMergeNeighbours(ScheduleResult)`: for each trip, for each assignment whose children (excluding none — all children considered) fit entirely into the spare seats of the other assignments on that same trip, produce a neighbour where that car is removed and its children distributed into the spare seats. Driver's own children of *remaining* cars never move; the removed driver's own children go wherever seats remain (they are passengers now).

Wire it into `expandByPerturbation` alongside `driverSwapNeighbours`. Merged neighbours are scored normally; with D1 they dominate their redundant parents, so `selectStatDiverseCandidates` (which keeps best-scored first) surfaces them.

*Why:* the exploration order can lock in a small car early (greedy child-fill in `Schedule.childrenCandidates` limits by `leftCapacity`); a repair move is cheaper than restructuring the exhaustive search. Alternative considered: teach `exploreSlot` to only branch on car-minimal partitions — rejected: large blast radius on search completeness and pruning correctness.

### D2bis — Cascade repair (added during implementation)

One merge neighbour removes a single car, and `PERTURB_ROUNDS = 2` bounds how many merges the perturbation can chain — plannings with 3+ redundant cars did not converge (observed: best candidate kept 2-3 redundant cars). Added `repairRedundantCars`: greedily apply the best-scoring merge until no merge improves the score, applied via `repairAll` (signature-deduplicated) to the merged pool before perturbation and to the final selected candidates. Validated on the real workbook: 0 redundant cars on all slots, justice improved as a side effect (min 0.81 → 0.95).

### D3 — Keep `computeRedundantDrivers` as the single source of truth

The existing capacity-sort lower bound is exactly the criterion the spec uses. No new metric. If D2's merge produces a planning where `redundantDrivers` drops, ranking follows automatically.

### D4 — Pruning guard update

`shouldPruneByOptimisticScore` already refuses to prune when `currentScore.redundantDrivers() < incumbent.redundantDrivers()`. Verify this stays consistent with the new comparator order (redundantDrivers now ranks higher, so partial schedules that are tighter must not be pruned by justice-based bounds). Adjust the guard order to mirror `compareScores`.

## Risks / Trade-offs

- [Justice degradation] Ranking tightness above justice can lower min-justice on some datasets. → Mitigation: justice remains immediately next criterion; stat-diverse top-N still exposes alternative profiles; measure on `covoit-requirement.csv` before/after.
- [Perturbation blow-up] Car-merge neighbours multiply the pool (bounded by `PERTURB_CAP = 4000`). → Mitigation: merge move generates at most one neighbour per (trip, removable car) pair; cap already enforced.
- [Test churn] Existing planner tests asserting score ordering will need updated expectations. → Mitigation: update fixtures alongside comparator change; keep `computeRedundantDrivers` tests untouched.
- [Distribution ambiguity] Several ways to distribute a removed car's children into spare seats produce equivalent scores. → Accept first-fit (deterministic order by driver name) to keep signatures stable.

## Migration Plan

Pure algorithm change, no data or API migration. Rollback = revert commit. Validate by regenerating `result.xlsx` from `covoit-requirement.csv` and checking the Monday-morning odd-week slot drops to 3 cars.

## Open Questions

- None blocking. If datasets appear where justice collapses under strict car-minimality, consider a tolerance (allow +1 redundant driver when it raises min-justice above a threshold) — out of scope for now.

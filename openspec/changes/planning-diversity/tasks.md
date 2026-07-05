## 1. Planner parameters

- [ ] 1.1 Add `scoreTolerance` (int) and `minDistance` (double) constants/defaults to `BruteForceSchedulePlanner` alongside `DEFAULT_RESTARTS`
- [ ] 1.2 Add a `generateTopSchedules(...)` overload carrying `scoreTolerance` and `minDistance`, and thread them through to `SearchState`
- [ ] 1.3 Store `scoreTolerance` and `minDistance` as fields on `SearchState`

## 2. Score-tolerance band (mechanism A)

- [ ] 2.1 Add a helper `isWithinBand(candidateScore, bestScore, tolerance)` using `compareScores` semantics
- [ ] 2.2 Relax `shouldPruneByOptimisticScore` so it prunes only branches that cannot reach `best − tolerance` (widen the optimistic cut by `tolerance`, keep completeness gates)
- [ ] 2.3 Update `evaluate` / `addTopCandidate` to admit in-band complete plannings (not only strictly-better ones), keeping signature dedup

## 3. Stochastic restarts (mechanism B)

- [ ] 3.1 In `exploreSlot`, group `sortedDrivers` whose primary keys are within an epsilon and randomize within the group using `SearchState.randomOrderFor` (seeded)
- [ ] 3.2 Verify per-seed determinism is preserved (same seed → same exploration order)

## 4. Minimum-distance pool admission + ranking (mechanism C)

- [ ] 4.1 Update `addMergedCandidate` to prefer in-band candidates ≥ `minDistance` from kept members; when full, evict the closest-pair member instead of the lowest-score one
- [ ] 4.2 Ensure `selectDiverseCandidates` emits the best-score candidate first (top-1 guarantee) then ranks 2..N by distance-first via `compareDiversityAware`
- [ ] 4.3 Allow returning fewer than `topCount` when fewer distinct plannings exist (no near-duplicate padding)

## 5. CLI wiring

- [ ] 5.1 Add `--score-tolerance`, `--min-distance`, `--restarts` flags to `WorkbookStatsCli` (with defaults)
- [ ] 5.2 Pass the parsed flags into the new `generateTopSchedules` overload

## 6. Tests & calibration

- [ ] 6.1 Add `BruteForceSchedulePlannerTest` cases: returned candidates pairwise distance ≥ `minDistance`; top-1 equals best found; in-band suboptimal-but-distinct planning is retained
- [ ] 6.2 Add a determinism test: same seed + params → identical candidate set/order
- [ ] 6.3 Add a "few alternatives" test: input with < topCount distinct plannings returns all distinct, no duplicate signatures
- [ ] 6.4 Verify `generateBestSchedule` / `ScheduleServiceTest` unchanged
- [ ] 6.5 Calibrate default `scoreTolerance` and `minDistance` against `covoiturage.xlsx`: confirm top-1 unchanged and ranks 2..N visibly distinct
- [ ] 6.6 Run `./mvnw test` and confirm green

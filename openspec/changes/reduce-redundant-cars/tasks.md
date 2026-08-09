# Tasks — Reduce Redundant Cars

## 1. Ranking

- [x] 1.1 Reorder `compareScores` in `BruteForceSchedulePlanner`: move `redundantDrivers` comparison directly after `assignedRequiredTransportSlots`, before justice min/avg (D1)
- [x] 1.2 Align `shouldPruneByOptimisticScore` guard order with the new comparator so tighter partial schedules are never pruned by justice bounds (D4)
- [x] 1.3 Add unit test: complete planning with 0 redundant drivers ranks above complete planning with 1 redundant driver and better min-justice
- [x] 1.4 Add unit test: equal redundant drivers → justice min breaks the tie; incomplete planning still loses to complete planning with redundant drivers

## 2. Car-merge move

- [x] 2.1 Implement `carMergeNeighbours(ScheduleResult)` in `BruteForceSchedulePlanner`: for each trip, for each removable car whose children fit in the other cars' spare seats, build the merged neighbour (first-fit by driver name, remaining drivers keep their own children, capacities respected)
- [x] 2.2 Wire `carMergeNeighbours` into `expandByPerturbation` alongside `driverSwapNeighbours`, under the existing `PERTURB_CAP` / signature-dedup guards
- [x] 2.3 Add unit test: slot with loads 3/4/2/3 and spare seats produces a 3-car neighbour with all 12 children still assigned
- [x] 2.4 Add unit test: slot with zero spare seats produces no merge neighbour; merged cars never exceed capacity and never lose a driver's own child

## 3. Top-candidate filtering

- [x] 3.1 Verify `selectStatDiverseCandidates` no longer surfaces redundant variants once ranking is fixed; if a redundant variant of an existing candidate still appears in top-N, filter it by planning signature + redundantDrivers comparison
- [x] 3.2 Add test covering the spec scenario: pool containing A and A-plus-redundant-car returns A only

## 4. Validation

- [x] 4.1 Run existing planner test suite (`./mvnw test` in carpool-back); update fixtures whose expectations depended on the old comparator order — 35/35 unit tests pass, no fixture change needed. Note: the two `@QuarkusTest` classes (FamilyResourceTest, FullScheduleResourceTest) cannot run locally (Docker devservices incompatible: "client version 1.32 is too old"), unrelated to this change.
- [x] 4.2 Regenerate planning from `covoit-requirement.csv` and confirm the odd-week Monday morning slot uses 3 cars instead of 4, with no completeness regression and justice stats reported before/after — regenerated from covoiturage.xlsx (seed 0, top 3): odd Monday morning now 3 cars (4+3+5 children), 0 redundant cars on all slots for all 3 candidates; before: 3 redundant cars, minJustice 0.81, avgJustice 0.949 → after: 0 redundant, minJustice 0.95, avgJustice 0.995.

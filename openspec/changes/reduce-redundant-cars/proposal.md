# Reduce Redundant Cars

## Why

Generated plannings sometimes use more cars than necessary on a slot. Example observed (odd week, Monday morning): 4 cars for 12 children, including one car carrying only 2 children ("Orion (Orion, Marin)") while spare seats existed in the other cars — 3 cars would have sufficed. Extra cars mean wasted parent trips, which is exactly what a carpool planner should avoid.

Root cause: the scorer already computes a `redundantDrivers` metric, but it ranks below justice (min/avg) in `compareScores`, and the search's justice objective actively rewards spreading trips across more families. A schedule with an unnecessary extra car can therefore beat a tighter one.

## What Changes

- The planner treats car-count minimality per slot as a stronger objective: a complete planning that uses fewer cars on a slot wins over one using redundant cars, before justice tie-breaking.
- Redundant-car detection accounts for real seat availability (capacities of the cars actually present on the slot), matching the existing `computeRedundantDrivers` logic.
- Justice remains the tie-breaker among plannings with equal car usage — fairness is preserved, but not at the cost of extra vehicles.
- Search neighbourhood gains a "car merge" move: children from an under-filled car are redistributed into spare seats of other cars on the same slot, removing the redundant car (complements the existing driver-swap perturbation).
- Candidate diversification must not resurface redundant-car plannings in the top results when a car-minimal equivalent exists.

## Capabilities

### New Capabilities

- `schedule-car-minimization`: the schedule planner produces plannings that never use a redundant car on any slot when a feasible redistribution exists (respecting car capacities, child presence, and driver constraints), and ranks car-minimal plannings above redundant ones.

### Modified Capabilities

<!-- none: no existing specs in openspec/specs/ -->

## Impact

- `carpool-back/src/main/java/com/carpool/schedule/calculator/BruteForceSchedulePlanner.java`: `compareScores` ordering, perturbation moves (`driverSwapNeighbours` + new merge move), pruning logic (`shouldPruneByOptimisticScore` uses `redundantDrivers`).
- `carpool-back/src/main/java/com/carpool/schedule/PlanningScorer.java`: `computeRedundantDrivers` (possibly refined), `REDUNDANT_DRIVER_PENALTY` weight.
- Existing planner tests in `carpool-back/src/test` may need updated expectations (score ordering changes).
- Workbook output (`result.xlsx`) unchanged in format; only the selected plannings change.

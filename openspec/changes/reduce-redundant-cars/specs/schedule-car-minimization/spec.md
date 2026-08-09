# Schedule Car Minimization

## ADDED Requirements

### Requirement: Planner avoids redundant cars on a slot
The schedule planner SHALL NOT retain a car on a slot when its children can all be redistributed into the spare seats of the other cars already assigned to that slot, respecting each car's capacity.

A car is *redundant* on a slot when the total number of children assigned to the slot can be covered by fewer cars among those present, given their capacities (the existing `redundantDrivers` computation).

#### Scenario: Under-filled car merged into spare seats
- **WHEN** a slot has 12 children assigned to 4 cars with capacities allowing 3 cars to carry all 12 (e.g. loads 3/4/2/3 with spare seats in the 3-child cars)
- **THEN** the selected planning uses at most 3 cars for that slot and every child remains assigned

#### Scenario: No feasible merge keeps all cars
- **WHEN** a slot has children whose total count equals the summed capacity of the assigned cars (no spare seats)
- **THEN** the planning keeps all cars and no redistribution is attempted

### Requirement: Car-minimal plannings rank above redundant ones
When comparing two complete plannings that assign the same required transport slots, the planner SHALL rank the planning with fewer redundant drivers above the one with more, before applying justice tie-breaking.

#### Scenario: Fewer cars beats better justice
- **WHEN** planning A is complete with 0 redundant drivers and planning B is complete with 1 redundant driver but a higher minimum justice score
- **THEN** planning A ranks above planning B

#### Scenario: Justice breaks ties at equal car usage
- **WHEN** plannings A and B are complete with the same number of redundant drivers
- **THEN** the planning with the higher minimum justice score ranks first

### Requirement: Completeness still dominates
Car minimization SHALL NOT cause the planner to prefer an incomplete planning: a complete planning with redundant drivers still ranks above any incomplete planning.

#### Scenario: Complete with extra car beats incomplete
- **WHEN** planning A is complete with 2 redundant drivers and planning B is incomplete with 0 redundant drivers
- **THEN** planning A ranks above planning B

### Requirement: Driver constraints respected during redistribution
When removing a redundant car, the redistribution SHALL keep every remaining driver's assignment valid: a driver's own present children stay in that driver's car, and no car exceeds its capacity.

#### Scenario: Driver's own children never moved out
- **WHEN** a redundant car is removed and its children redistributed
- **THEN** each remaining car still contains all of its driver's own present children and its total children count does not exceed its capacity

### Requirement: Top candidates are car-minimal
The candidate diversification (top-N plannings returned to the user) SHALL NOT include a planning with redundant drivers when a planning identical except for the redundant car(s) exists in the pool.

#### Scenario: Redundant variant filtered from top results
- **WHEN** the candidate pool contains planning A and planning B where B equals A plus one redundant car on a slot
- **THEN** the returned top plannings include A and not B

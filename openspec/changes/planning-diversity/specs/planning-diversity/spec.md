## ADDED Requirements

### Requirement: Distinct candidate plannings

When asked for `topCount` plannings, the planner SHALL return a set of plannings that are meaningfully distinct from one another, not minor variations of a single planning.

#### Scenario: Multiple requested plannings are distinct
- **WHEN** `generateTopSchedules` is called with `topCount` greater than 1 and the input admits several structurally different complete plannings
- **THEN** each returned candidate SHALL differ from every other returned candidate by at least the configured minimum planning distance

#### Scenario: Few alternatives exist
- **WHEN** the input admits fewer distinct plannings than `topCount`
- **THEN** the planner SHALL return all distinct plannings it found without duplicating any planning signature

### Requirement: Top-ranked planning is the best found

Prioritizing variety SHALL NOT cause the planner to return a top-ranked planning worse than the best complete planning it actually discovered.

#### Scenario: Top-1 is best found
- **WHEN** `generateTopSchedules` returns its candidates
- **THEN** the first returned candidate SHALL be the highest-scoring complete planning found, ranked by the existing score comparison

#### Scenario: generateBestSchedule unaffected
- **WHEN** `generateBestSchedule` is called
- **THEN** it SHALL return the same best planning it would return without the diversity changes

### Requirement: Variety prioritized for lower ranks

For ranks 2 through N, the planner SHALL favor distinctness over marginal score: a more distant candidate MAY be ranked ahead of a closer candidate that has a slightly higher score, provided both are within the score-tolerance band.

#### Scenario: Distant candidate promoted over closer higher-score one
- **WHEN** two candidates are both within the tolerance band, one closer to already-selected plannings with a higher score and one farther with a slightly lower score
- **THEN** the planner SHALL prefer the farther candidate for the next rank

### Requirement: Score-tolerance acceptance band

The candidate pool SHALL retain complete plannings whose score is within a configurable tolerance below the best score, so that structurally different but slightly-suboptimal plannings remain available for diverse selection.

#### Scenario: Slightly-suboptimal distinct planning kept
- **WHEN** a complete planning is found whose score is below the best score but within the configured tolerance band, and it differs from existing pool members
- **THEN** the planner SHALL admit it to the candidate pool rather than discarding it as worse than the incumbent

#### Scenario: Far-below planning rejected
- **WHEN** a complete planning is found whose score is below the configured tolerance band
- **THEN** the planner SHALL NOT include it among the returned candidates

#### Scenario: Pruning respects the band
- **WHEN** the search evaluates whether to prune a branch by optimistic score
- **THEN** it SHALL only prune branches that cannot produce a complete planning within the tolerance band of the current best, not merely branches that cannot beat the current best

### Requirement: Stochastic restarts

Each restart SHALL explore the search space with genuinely randomized driver selection among near-equal choices, so that different seeds produce different candidate plannings.

#### Scenario: Different seeds yield different candidates
- **WHEN** the planner runs multiple restarts with different seeds on input that admits several plannings
- **THEN** the restarts SHALL collectively contribute distinct plannings to the merged candidate pool rather than re-discovering the same planning

#### Scenario: Determinism for a fixed seed
- **WHEN** the planner runs twice with the same seed, parameters, and input
- **THEN** it SHALL produce the same set of candidates in the same order

### Requirement: Tunable diversity parameters

Diversity behavior SHALL be configurable through planner parameters and the workbook planning CLI.

#### Scenario: Parameters exposed through the CLI
- **WHEN** a user runs the workbook planning CLI
- **THEN** they SHALL be able to set the score tolerance, the minimum planning distance, and the restart count

#### Scenario: Sensible defaults
- **WHEN** the diversity parameters are not specified
- **THEN** the planner SHALL apply defaults that keep the top-ranked planning equal to the best found and still produce distinct lower-ranked plannings when the input allows

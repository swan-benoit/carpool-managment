# Capability: metrics-computation

## Requirement: Metrics use family as the driving unit
Initial metrics computation SHALL treat each family as a single driving unit with one car capacity.

### Scenario: Family metrics do not split on parent identities
- **GIVEN** a family includes separated parents
- **WHEN** metrics are computed
- **THEN** the family is still treated as one computation unit

## Requirement: Slot-level child absences
Child absences used by metrics SHALL be represented at week type, week day, and time slot granularity.

### Scenario: Child absent only on one time slot
- **GIVEN** a child is unavailable for transport on a morning slot but not on the evening slot of the same day
- **WHEN** metrics input is represented
- **THEN** that distinction is preserved in the structured model

## Requirement: Initial metrics ignore family preferences
Initial metrics computation SHALL ignore family preference values.

### Scenario: Preference capture does not affect first metrics
- **GIVEN** family preferences are present in the workbook
- **WHEN** initial metrics are computed
- **THEN** those preferences do not alter the metrics result

## Requirement: Preference data is preserved
Family preference data SHALL be preserved for future planning generation even when it is ignored by initial metrics computation.

### Scenario: Workbook reused later for planning generation
- **GIVEN** a workbook has already been used for metrics computation
- **WHEN** planning generation is introduced later
- **THEN** previously entered family preference data remains available for reuse

## Requirement: Java computes business metrics from workbook input
The system SHALL compute business metrics in Java from normalized workbook input rather than using Excel formulas as the source of truth.

### Scenario: Workbook drives Java metrics computation
- **GIVEN** a completed workbook exists
- **WHEN** metrics are generated
- **THEN** Java reads workbook-derived structured data and computes the metrics
- **AND** Excel is not treated as the authoritative implementation of business formulas

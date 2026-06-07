# Capability: normalization-pipeline

## Requirement: Human-friendly workbook normalization
The system SHALL normalize the human-friendly workbook into structured Java domain data.

### Scenario: Workbook transformed into structured metrics input
- **GIVEN** a completed family workbook exists
- **WHEN** normalization runs
- **THEN** structured domain data is produced from the authoritative slot grids

## Requirement: Grid as the authoritative source
The slot grid SHALL be the authoritative source for normalization.

### Scenario: Final slot values are normalized
- **GIVEN** a completed family workbook exists
- **WHEN** normalization runs
- **THEN** the final slot grid values are used as the authoritative input

## Requirement: MCP-mediated workbook prefill
LLM-assisted workbook prefill SHALL be performed through domain-oriented MCP operations rather than arbitrary cell editing.

### Scenario: Prefill writes structured workbook content
- **GIVEN** semi-structured source input is interpreted by an LLM
- **WHEN** workbook data is written
- **THEN** the LLM uses constrained MCP operations aligned with workbook domain concepts

## Requirement: Java-owned workbook structure
Workbook structure SHALL be generated and owned by Java rather than by the LLM.

### Scenario: Workbook layout remains deterministic
- **GIVEN** a workbook must be created or regenerated
- **WHEN** the template is produced
- **THEN** Java generates the workbook structure deterministically

## Requirement: Ambiguity remains reviewable
Ambiguous source information SHALL remain visible for human review instead of being silently converted into hard structured values.

### Scenario: Source text is unclear
- **GIVEN** the original human input contains ambiguous wording
- **WHEN** the workbook is prefilled
- **THEN** uncertainty is preserved in a reviewable form rather than hidden by forced interpretation

# Design: Excel-first carpool input

## Overview

The target system replaces Angular-based data entry with a workbook-centric flow.
The workbook is the main editable artifact for families.
The Java backend becomes the source of template generation, normalization, and metrics computation.

The workbook is an input and output artifact, not the place where business metrics formulas are authored as the source of truth.

The design separates four concerns:

1. Human-readable source input.
2. Workbook template generation.
3. LLM-assisted prefill under MCP control.
4. Deterministic normalization and metrics computation.

## Target Flow

```text
human questionnaire / shared spreadsheet / CSV
                    |
                    v
             LLM interpretation
                    |
                    v
      MCP domain tools for workbook filling
                    |
                    v
        Java-generated Excel workbook template
                    |
                    v
           family review and correction
                    |
                    v
     normalization to Java domain structures
                    |
                    v
             metrics computation
                    |
                    v
          metrics written back to workbook
```

## Core Domain Decisions

### Family as the unit of input

- A family represents one group of children.
- Separated parents do not create multiple family entities.
- For computation, one family is treated as one driving unit and one car capacity.

### Preserve the existing Java model as a base

The existing Java concepts remain the main starting point:

- `Family`
- `Child`
- `WeekType`
- `WeekDay`
- `TimeSlot`

The required model change is limited and focused:

- child absences must become slot-level rather than day-level

Conceptually:

```text
AbsenceDays
  -> still represents absence-like input
  -> must include timeSlot
```

### Preferences remain family-level

- Driving preferences are captured at family level.
- They are not modeled per parent.
- They are not used by the initial metrics computation.
- They are preserved for future planning generation and scoring.

## Workbook Design

### Workbook shape

The system uses a single workbook.

Suggested structure:

```text
requirements.xlsx
├── README
├── Index
├── Family - <name 1>
├── Family - <name 2>
└── ...
```

### Family tab contract

Every family tab uses the same structure.

Sections:

1. Family identity and car capacity.
2. Family preference slot grid.
3. Child absence slot grid.
4. Informational free-text fields.

## Java-Generated Template

### Decision

The workbook template must be generated in Java.

This is important because:

- the template structure becomes code-defined and reproducible
- workbook evolution stays close to the Java domain model
- future validation and normalization can reuse the same slot definitions
- the workbook can be regenerated deterministically

### Responsibilities of the Java template generator

The Java template generator should:

- create the workbook skeleton
- create the README and Index tabs
- create one family tab per family when data is known
- apply the standard layout for each family tab
- generate the fixed slot grid structure
- encode validation-friendly cell values where applicable

This generator is responsible for workbook structure, not for free-form interpretation.

## MCP-Guided Prefill

### Decision

The LLM must not freely manipulate workbook cells.
Instead, workbook prefill should be mediated by an MCP with domain-level operations.

### Why MCP

Using an MCP constrains generation and makes it auditable.

Benefits:

- avoids uncontrolled direct spreadsheet editing
- ensures only valid domain values are written
- keeps workbook structure authoritative in Java
- improves replayability and validation

### MCP level

The MCP should expose domain-oriented workbook actions rather than raw cell editing.

Good examples:

- create workbook from Java template
- ensure family sheet exists
- set family metadata
- set family preference slot value
- set child absence slot value
- set informational note
- validate workbook completeness
- save workbook

Bad examples:

- set arbitrary cell by coordinate
- merge arbitrary ranges
- rewrite workbook layout from the LLM side

## Normalization

Normalization converts workbook content into Java domain data used by metrics.

### Used now for metrics

- family identity
- car capacity
- children
- child absence slots

These workbook values are read by Java and converted into domain data before metrics are computed.

### Captured now for future planning generation

- family preference slots

### Informational only

- meeting point
- WhatsApp participation
- security notes
- free-form guard arrangement notes
- remarks

## Metrics Boundary

Initial metrics computation uses only:

- family car capacity
- child presence/absence inferred from slot-level absences

Initial metrics computation does not use family preferences.

Those preferences are still collected now to avoid asking families for a second input cycle later.

Business metrics formulas remain implemented in Java.
Excel may later contain simple display or helper formulas, but it is not the source of truth for metrics computation.

## Risks and Constraints

### Ambiguous free-form source data

Human input may remain partially ambiguous.
The system should preserve ambiguity for review rather than silently turning it into hard facts.

### Workbook usability

The workbook must remain easy enough to fill even with 16 slot columns.
The helper sections are required to reduce repetitive manual input.

### Model drift

If the workbook and Java model drift apart, normalization becomes fragile.
Generating the workbook in Java reduces this risk.

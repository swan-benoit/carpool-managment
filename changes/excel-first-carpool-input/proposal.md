# Proposal: Excel-first carpool input

## Problem

The current Angular + database input flow does not match how the carpool group actually shares information.
Requirements are currently gathered in a human-friendly shared spreadsheet/questionnaire format, with free-form wording and family-specific context.

The existing Java codebase already contains useful domain concepts and metrics logic, but the current product shape is too heavy for the real workflow:

- Angular is not the natural place for data entry.
- A database is not needed as the primary source of truth.
- The current child absence model lacks time-slot granularity.
- Families should not be asked to re-enter data in multiple formats.

## Goals

- Replace Angular-first input with a single Excel workbook used as the primary editable input.
- Keep the Excel workbook as input only, not as the source of business formula execution.
- Use one workbook tab per family.
- Keep the current core Java domain as the basis for metrics.
- Extend child absences to slot-level granularity: week type, week day, and time slot.
- Capture family-level driving preferences during the first data entry pass.
- Generate the workbook template in Java.
- Support an LLM-assisted prefill flow constrained through an MCP rather than free-form workbook editing.
- Reuse the same workbook later for planning generation.
- Compute metrics from workbook data in Java and write the results back to workbook outputs.

## Non-Goals

- Do not implement full planning generation in this change.
- Do not model separate parent-level driving schedules.
- Do not require database-backed data entry as the primary workflow.
- Do not attempt to formalize all informational notes into hard structured inputs.
- Do not ask families for a second round of input dedicated only to planning preferences.

## Scope

This change formalizes the target workflow and data shape for the Excel-first input system:

1. A Java-generated workbook template.
2. One family tab per family group.
3. Slot-level child absence capture.
4. Family-level preference capture.
5. An MCP-mediated LLM prefill path that writes structured workbook content using domain operations.

## Outcome

The system will move to a simpler and more portable workflow:

- Human-friendly source data remains shareable as Excel.
- The workbook structure is stable and machine-readable.
- Metrics can be computed from the workbook without Angular or a database-first workflow.
- Preferences are captured immediately and preserved for future planning generation.

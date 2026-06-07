# Tasks: Excel-first carpool input

## 1. Define workbook contract

- [ ] Define the workbook-level tabs: README, Index, and family tabs.
- [ ] Define the standard structure of a family tab.
- [ ] Define the 16-slot grid layout shared by all family tabs.
- [ ] Define allowed values for family preferences.
- [ ] Define allowed values for child absences.

## 2. Define Java template generation

- [ ] Define the Java component responsible for workbook template generation.
- [ ] Define how workbook structure is produced deterministically from Java.

## 3. Define slot-level absence model

- [ ] Define the conceptual evolution of child absences to include `timeSlot`.
- [ ] Define how slot-level absences map to the current Java domain.
- [ ] Define what remains unchanged in the existing `Family` and `Child` model.

## 4. Define MCP-mediated prefill

- [ ] Define the MCP responsibilities for workbook prefill.
- [ ] Define domain-oriented MCP operations.
- [ ] Define which values the MCP may write and which structure remains Java-owned.
- [ ] Define how ambiguous source input is surfaced for human review.

## 5. Define normalization inputs

- [ ] Define which workbook fields are consumed by metrics computation.
- [ ] Define which workbook fields are preserved for future planning generation.
- [ ] Define which workbook fields remain informational only.

## 6. Define metrics scope

- [ ] Confirm that initial metrics ignore family preferences.
- [ ] Confirm that preferences are still captured during the first input pass.
- [ ] Define how slot-level child absences affect target metrics.
- [ ] Define workbook-to-Java metrics flow and confirm Excel is not the source of truth for business formulas.

## 7. Prepare future planning generation

- [ ] Define the preserved preference data contract for later planning generation.
- [ ] Define the boundary between current metrics work and future planning optimization.

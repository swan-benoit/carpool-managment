# Capability: input-workbook

## Requirement: Single workbook input
The system SHALL use a single Excel workbook as the primary editable input artifact.

### Scenario: Workbook used as the primary family-editable format
- **GIVEN** the carpool group needs to provide structured requirements
- **WHEN** the input artifact is created
- **THEN** it is represented as a single Excel workbook

## Requirement: One tab per family
The workbook SHALL contain one tab per family.

### Scenario: Separated parents remain one family entity
- **GIVEN** a group of children belongs to one family entity
- **AND** the parents are separated
- **WHEN** the workbook is generated
- **THEN** the workbook contains a single family tab for that child group

## Requirement: Stable family tab structure
Each family tab SHALL use the same layout and slot grid structure.

### Scenario: Normalization relies on a stable template
- **GIVEN** multiple family tabs exist in the workbook
- **WHEN** the workbook is normalized into Java domain data
- **THEN** each family tab exposes the same structural contract

## Requirement: Preference capture in the first input pass
The workbook SHALL capture family preference data during the first family input pass.

### Scenario: Family enters data only once
- **GIVEN** the family is asked to complete the workbook
- **WHEN** they fill their family tab
- **THEN** both current metrics inputs and future planning preference inputs are captured in the same pass

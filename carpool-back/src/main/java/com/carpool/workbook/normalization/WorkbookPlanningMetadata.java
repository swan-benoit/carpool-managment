package com.carpool.workbook.normalization;

public record WorkbookPlanningMetadata(
        String planner,
        Long exploredStates,
        Boolean searchCompleted,
        Double maxSeconds
) {
}

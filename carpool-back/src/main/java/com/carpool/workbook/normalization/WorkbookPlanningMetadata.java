package com.carpool.workbook.normalization;

import java.util.Map;

public record WorkbookPlanningMetadata(
        String planner,
        Long exploredStates,
        Boolean searchCompleted,
        Double maxSeconds,
        Long seed,
        Integer restarts,
        Map<String, Double> maxFamilyTrips
) {
}

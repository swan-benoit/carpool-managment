package com.carpool.workbook.normalization;

import java.util.List;

import com.carpool.schedule.PlanningScore;

public record WorkbookPerfectStatsResponse(
        Double totalRequiredTripsPerWeek,
        Double totalPerfectMeanTripPerWeek,
        List<WorkbookPerfectStat> families,
        PlanningScore planningScore,
        WorkbookPlanningMetadata planningMetadata,
        WorkbookPlanningView planning,
        List<WorkbookPlanCandidateView> planCandidates
) {
}

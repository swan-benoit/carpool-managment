package com.carpool.workbook.normalization;

import com.carpool.schedule.PlanningScore;

public record WorkbookPlanCandidateView(
        int rank,
        WorkbookPlanningMetadata planningMetadata,
        PlanningScore planningScore,
        WorkbookPlanningView planning,
        Double minimumDistanceToHigherRanked
) {
}

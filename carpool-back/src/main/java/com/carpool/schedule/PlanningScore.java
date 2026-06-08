package com.carpool.schedule;

import java.util.List;

public record PlanningScore(
        int totalScore,
        int impossibleAssignments,
        int avoidAssignments,
        int preferredAssignments,
        int okAssignments,
        List<FamilyPlanningScore> families
) {
}
